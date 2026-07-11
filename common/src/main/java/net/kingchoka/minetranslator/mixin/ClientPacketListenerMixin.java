package net.kingchoka.minetranslator.mixin;

import net.kingchoka.minetranslator.config.ModConfig;
import net.kingchoka.minetranslator.translation.TranslationService;
import net.kingchoka.minetranslator.translation.TranslationRequest;
import net.kingchoka.minetranslator.translation.TranslationMode;
import net.kingchoka.minetranslator.debug.TranslationDebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Collections;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Unique
    private boolean MineTranslator$processingOutgoing = false;

    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void onSendChat(String message, CallbackInfo ci) {
        ModConfig config = ModConfig.getInstance();
        if (!config.translateMyMessages) {
            return;
        }

        if (message.startsWith("/")) {
            return;
        }

        if (MineTranslator$processingOutgoing) {
            return;
        }

        ci.cancel();

        String targetLang = config.sourceLanguage;
        if ("auto".equalsIgnoreCase(targetLang) || targetLang == null || targetLang.trim().isEmpty()) {
            targetLang = "en";
        }

        TranslationRequest request = new TranslationRequest(
            message,
            config.targetLanguage,
            targetLang,
            TranslationMode.CHAT,
            Collections.emptyList(),
            config.provider
        );

        TranslationDebugLogger.chat("Translating outgoing message: {}", message);

        ClientPacketListener listener = (ClientPacketListener) (Object) this;

        TranslationService.getInstance().translate(request).thenAccept(result -> {
            Minecraft.getInstance().execute(() -> {
                String textToSend;
                if (result.success()) {
                    textToSend = result.translatedText();
                    TranslationDebugLogger.chat("Outgoing message translated: {} -> {}", message, textToSend);
                } else {
                    textToSend = message;
                    TranslationDebugLogger.warn("Failed to translate outgoing message: {}. Sending original.", result.errorMessage());
                }

                MineTranslator$processingOutgoing = true;
                try {
                    listener.sendChat(textToSend);
                } finally {
                    MineTranslator$processingOutgoing = false;
                }
            });
        });
    }
}
