package net.kingchoka.minetranslator.chat;

import net.kingchoka.minetranslator.config.ModConfig;
import net.kingchoka.minetranslator.debug.TranslationDebugLogger;
import net.kingchoka.minetranslator.translation.TranslationMode;
import net.kingchoka.minetranslator.translation.TranslationRequest;
import net.kingchoka.minetranslator.translation.TranslationService;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.kingchoka.minetranslator.api.ChatComponentMixinAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Locale;

public class ChatTranslationController {
    private static final ChatTranslationController INSTANCE = new ChatTranslationController();

    public static ChatTranslationController getInstance() {
        return INSTANCE;
    }

    private ChatTranslationController() {}

    public void onChatMessageReceived(GuiMessage guiMessage, ChatComponent chat) {
        translateMessage(guiMessage, chat, false);
    }

    public void translateMessage(GuiMessage guiMessage, ChatComponent chat, boolean force) {
        ModConfig config = ModConfig.getInstance();
        Component content = guiMessage.content();
        String plainText = content.getString();

        ChatTranslationState state = ChatEntryStore.getInstance().getOrCreateState(guiMessage);

        if (force) {
            // Reset to allow re-translating with a different language or provider
            state.status = ChatTranslationState.TranslationStatus.ORIGINAL;
        }

        if (state.status != ChatTranslationState.TranslationStatus.ORIGINAL) {
            return;
        }

        boolean isPlayer = false;
        String textToTranslate = plainText;
        TranslationMode mode = TranslationMode.CHAT;
        int bodyStartIndex = 0;

        PlayerMessageParser.PlayerParseResult parseResult = PlayerMessageParser.parse(plainText);
        if (parseResult.success()) {
            isPlayer = true;
            textToTranslate = parseResult.body();
            bodyStartIndex = parseResult.bodyStartIndex();
            mode = TranslationMode.PLAYER_CHAT;
        }

        boolean shouldTranslate = force || config.autoTranslateEveryMessage || (isPlayer && config.autoTranslatePlayerMessages);
        if (!shouldTranslate) {
            return;
        }

        state.status = ChatTranslationState.TranslationStatus.QUEUED;

        TranslationRequest request = new TranslationRequest(
            textToTranslate,
            config.sourceLanguage,
            config.targetLanguage,
            mode,
            Collections.emptyList(),
            config.provider
        );

        final boolean finalIsPlayer = isPlayer;
        final int finalBodyStartIndex = bodyStartIndex;

        state.status = ChatTranslationState.TranslationStatus.TRANSLATING;
        TranslationDebugLogger.chat("Requesting translation. ID: {}, Plain: {}, Force: {}", state.entryId, plainText, force);

        TranslationService.getInstance().translate(request).thenAccept(result -> {
            if (result.success()) {
                String translatedText = result.translatedText();
                TranslationDebugLogger.chat("Result received. ID: {}, Translated: {}", state.entryId, translatedText);

                Component finalComponent;
                if (finalIsPlayer) {
                    SplitComponent split = splitComponentAtIndex(content, parseResult.username());
                    Style bodyStyle = getDominantStyle(split.body);
                    Component translatedBody = Component.literal(translatedText).withStyle(bodyStyle);
                    finalComponent = Component.empty().append(split.prefix).append(translatedBody);
                } else {
                    Style msgStyle = getDominantStyle(content);
                    finalComponent = Component.literal(translatedText).withStyle(msgStyle);
                }

                if (config.showOriginal) {
                    finalComponent = Component.empty()
                        .append(finalComponent)
                        .append(Component.literal(" (")
                            .append(content)
                            .append(Component.literal(")"))
                            .withStyle(Style.EMPTY.withColor(0x888888)));
                }

                state.translatedText = finalComponent;
                state.status = ChatTranslationState.TranslationStatus.TRANSLATED;

                Minecraft.getInstance().execute(() -> {
                    ((ChatComponentMixinAccessor) chat).MineTranslator$refreshTrimmedMessages();
                });
            } else {
                state.status = ChatTranslationState.TranslationStatus.FAILED;
                TranslationDebugLogger.warn("Translation failed for ID: {}. Error: {}", state.entryId, result.errorMessage());
            }
        });
    }

    public static class SplitComponent {
        public final Component prefix;
        public final Component body;

        public SplitComponent(Component prefix, Component body) {
            this.prefix = prefix;
            this.body = body;
        }
    }

    private Style getDominantStyle(Component component) {
        class StyleHolder {
            Style style = Style.EMPTY;
        }
        final StyleHolder holder = new StyleHolder();
        component.visit((style, text) -> {
            if (style != null && style.getColor() != null) {
                holder.style = style;
                return Optional.of(style);
            }
            return Optional.empty();
        }, Style.EMPTY);

        if (holder.style == Style.EMPTY) {
            return component.getStyle();
        }
        return holder.style;
    }

    public static SplitComponent splitComponentAtIndex(Component original, String username) {
        List<Component> leaves = new ArrayList<>();
        original.visit((style, text) -> {
            if (!text.isEmpty()) {
                leaves.add(Component.literal(text).withStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);

        StringBuilder sb = new StringBuilder();
        for (Component leaf : leaves) {
            sb.append(leaf.getString());
        }
        String leavesText = sb.toString();

        int sepIdx = -1;
        if (username != null) {
            String lowerText = leavesText.toLowerCase(Locale.ROOT);
            String lowerUser = username.toLowerCase(Locale.ROOT);
            int userIdx = lowerText.indexOf(lowerUser);
            if (userIdx != -1) {
                int searchStart = userIdx + lowerUser.length();
                for (int i = searchStart; i < leavesText.length(); i++) {
                    char c = leavesText.charAt(i);
                    if (c == ':' || c == '»' || c == '▶') {
                        sepIdx = i;
                        break;
                    }
                }
            }
        }

        // Fallback: find first separator in leavesText
        if (sepIdx == -1) {
            for (int i = 0; i < leavesText.length(); i++) {
                char c = leavesText.charAt(i);
                if (c == ':' || c == '»' || c == '▶') {
                    sepIdx = i;
                    break;
                }
            }
        }

        int boundary;
        if (sepIdx != -1) {
            boundary = sepIdx + 1;
        } else {
            boundary = leavesText.length();
        }

        var prefixComponent = Component.empty().withStyle(original.getStyle());
        var bodyComponent = Component.empty().withStyle(original.getStyle());

        int currentOffset = 0;

        for (Component leaf : leaves) {
            String leafText = leaf.getString();
            Style leafStyle = leaf.getStyle();
            int L = leafText.length();

            if (currentOffset + L <= boundary) {
                prefixComponent.append(leaf);
            } else if (currentOffset >= boundary) {
                bodyComponent.append(leaf);
            } else {
                int prefixLen = boundary - currentOffset;
                String prefixPart = leafText.substring(0, prefixLen);
                String bodyPart = leafText.substring(prefixLen);

                if (!prefixPart.isEmpty()) {
                    prefixComponent.append(Component.literal(prefixPart).withStyle(leafStyle));
                }
                if (!bodyPart.isEmpty()) {
                    bodyComponent.append(Component.literal(bodyPart).withStyle(leafStyle));
                }
            }
            currentOffset += L;
        }

        return new SplitComponent(prefixComponent, bodyComponent);
    }
}
