package net.kingchoka.minetranslator.translation;

import net.kingchoka.minetranslator.translation.provider.*;

import java.util.Locale;

public final class ProviderConnectionController {
    private ProviderConnectionController() {}

    public static Result test(String providerId, String apiKey) {
        try {
            TranslationProvider provider = switch (providerId) {
                case "Google" -> new GoogleTranslationProvider();
                case "Fake" -> new FakeTranslationProvider();
                case "DeepL" -> new DeepLProvider(() -> apiKey);
                case "Gemini" -> new GeminiProvider(() -> apiKey);
                case "Claude" -> new ClaudeProvider(() -> apiKey);
                case "OpenAI" -> new OpenAIProvider(() -> apiKey);
                default -> null;
            };
            if (provider == null) return Result.MODEL_NOT_FOUND;
            String translated = provider.translate("Hello", "en", "ru");
            return translated == null || translated.isBlank() ? Result.NETWORK_ERROR : Result.SUCCESS;
        } catch (IllegalStateException e) {
            return Result.INVALID_KEY;
        } catch (Exception e) {
            String message = String.valueOf(e.getMessage()).toLowerCase(Locale.ROOT);
            if (message.contains("401") || message.contains("403") || message.contains("api key")) return Result.INVALID_KEY;
            if (message.contains("404") || message.contains("model")) return Result.MODEL_NOT_FOUND;
            if (message.contains("429") || message.contains("limit")) return Result.RATE_LIMIT;
            return Result.NETWORK_ERROR;
        }
    }

    public enum Result { SUCCESS, INVALID_KEY, MODEL_NOT_FOUND, RATE_LIMIT, NETWORK_ERROR }
}
