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
                if (finalIsPlayer && finalBodyStartIndex > 0) {
                    SplitComponent split = splitComponentAtIndex(content, finalBodyStartIndex - 1);
                    Component translatedBody = Component.literal(translatedText).withStyle(split.body.getStyle());
                    finalComponent = Component.empty().append(split.prefix).append(translatedBody);
                } else {
                    finalComponent = Component.literal(translatedText).withStyle(content.getStyle());
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

    public static SplitComponent splitComponentAtIndex(Component original, int index) {
        List<Component> leaves = new ArrayList<>();
        original.visit((style, text) -> {
            if (!text.isEmpty()) {
                leaves.add(Component.literal(text).withStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);

        var prefixComponent = Component.empty().withStyle(original.getStyle());
        var bodyComponent = Component.empty().withStyle(original.getStyle());

        int currentOffset = 0;
        int boundary = index + 1;

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
