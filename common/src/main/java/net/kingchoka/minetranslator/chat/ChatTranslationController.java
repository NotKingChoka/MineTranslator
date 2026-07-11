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

        boolean isNPC = plainText.contains("[NPC]");
        boolean shouldTranslate = force;
        if (!shouldTranslate) {
            if (config.translateOnlyNPC) {
                shouldTranslate = isNPC;
            } else {
                shouldTranslate = config.autoTranslateEveryMessage || (isPlayer && config.autoTranslatePlayerMessages);
            }
        }
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
                SplitComponent split = null;
                if (finalIsPlayer) {
                    split = splitComponentAtIndex(content, parseResult.username());
                    Component translatedBody = colorizeTranslatedText(split.body, translatedText);
                    finalComponent = Component.empty().append(split.prefix).append(translatedBody);
                } else {
                    finalComponent = colorizeTranslatedText(content, translatedText);
                }

                if (config.showOriginal) {
                    String origText = (finalIsPlayer && split != null) ? split.body.getString() : content.getString();
                    finalComponent = Component.empty()
                        .append(finalComponent)
                        .append(Component.literal(" (" + origText + ")")
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

    public static class TextSegment {
        public final String text;
        public final Style style;
        public TextSegment(String text, Style style) {
            this.text = text;
            this.style = style;
        }
    }

    private Component colorizeTranslatedText(Component original, String translatedText) {
        List<TextSegment> segments = new ArrayList<>();
        original.visit((style, text) -> {
            if (!text.isEmpty()) {
                segments.add(new TextSegment(text, style));
            }
            return Optional.empty();
        }, Style.EMPTY);

        List<TextSegment> merged = new ArrayList<>();
        for (TextSegment seg : segments) {
            if (merged.isEmpty()) {
                merged.add(seg);
            } else {
                TextSegment last = merged.get(merged.size() - 1);
                if (last.style.equals(seg.style)) {
                    merged.set(merged.size() - 1, new TextSegment(last.text + seg.text, last.style));
                } else {
                    merged.add(seg);
                }
            }
        }

        int translatedIdx = 0;
        var resultComponent = Component.empty().withStyle(original.getStyle());

        for (int i = 0; i < merged.size(); i++) {
            TextSegment seg = merged.get(i);
            String segText = seg.text;
            Style segStyle = seg.style;

            String trimmed = segText.trim();
            if (trimmed.length() < 2) {
                continue;
            }

            if (trimmed.matches("^[A-Za-z0-9_\\-\\+\\(\\)\\[\\]\\{\\}⛃\\s,\\.!]+$")) {
                int foundIdx = translatedText.indexOf(trimmed, translatedIdx);
                if (foundIdx != -1) {
                    if (foundIdx > translatedIdx) {
                        String middleText = translatedText.substring(translatedIdx, foundIdx);
                        Style middleStyle = (i > 0) ? merged.get(i - 1).style : segStyle;
                        resultComponent.append(Component.literal(middleText).withStyle(middleStyle));
                    }

                    resultComponent.append(Component.literal(trimmed).withStyle(segStyle));
                    translatedIdx = foundIdx + trimmed.length();
                }
            }
        }

        if (translatedIdx < translatedText.length()) {
            String tailText = translatedText.substring(translatedIdx);
            Style tailStyle = merged.isEmpty() ? Style.EMPTY : merged.get(merged.size() - 1).style;
            resultComponent.append(Component.literal(tailText).withStyle(tailStyle));
        }

        return resultComponent;
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
