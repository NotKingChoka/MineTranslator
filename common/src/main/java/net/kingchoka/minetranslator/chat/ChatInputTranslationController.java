package net.kingchoka.minetranslator.chat;

import net.kingchoka.minetranslator.api.ChatScreenMixinAccessor;
import net.kingchoka.minetranslator.config.ModConfig;
import net.kingchoka.minetranslator.debug.TranslationDebugLogger;
import net.kingchoka.minetranslator.translation.TranslationMode;
import net.kingchoka.minetranslator.translation.TranslationRequest;
import net.kingchoka.minetranslator.translation.TranslationService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class ChatInputTranslationController {
    private static final ChatInputTranslationController INSTANCE = new ChatInputTranslationController();
    private final Map<EditBox, InputState> states = Collections.synchronizedMap(new WeakHashMap<>());

    public static ChatInputTranslationController getInstance() {
        return INSTANCE;
    }

    private ChatInputTranslationController() {}

    public boolean translateOrRestore(ChatScreen screen) {
        if (!(screen instanceof ChatScreenMixinAccessor accessor)) return false;
        EditBox input = accessor.MineTranslator$getInput();
        if (input == null) return false;

        String current = input.getValue();
        if (current == null || current.isBlank() || current.startsWith("/")) return false;

        InputState previous = states.get(input);
        if (previous != null && current.equals(previous.translated())) {
            input.setValue(previous.original());
            states.remove(input);
            return true;
        }

        ModConfig config = ModConfig.getInstance();
        String targetLanguage = config.sourceLanguage;
        if (targetLanguage == null || targetLanguage.isBlank() || "auto".equalsIgnoreCase(targetLanguage)) {
            targetLanguage = "en";
        }

        TranslationRequest request = new TranslationRequest(
            current,
            config.targetLanguage,
            targetLanguage,
            TranslationMode.CHAT,
            Collections.emptyList(),
            config.provider
        );

        String sourceAtRequest = current;
        TranslationService.getInstance().translate(request).thenAccept(result ->
            Minecraft.getInstance().execute(() -> {
                if (Minecraft.getInstance().screen != screen || !input.getValue().equals(sourceAtRequest)) return;
                if (result.success()) {
                    input.setValue(result.translatedText());
                    states.put(input, new InputState(sourceAtRequest, result.translatedText()));
                } else {
                    TranslationDebugLogger.warn("Failed to translate chat input: {}", result.errorMessage());
                }
            })
        );
        return true;
    }

    private record InputState(String original, String translated) {}
}
