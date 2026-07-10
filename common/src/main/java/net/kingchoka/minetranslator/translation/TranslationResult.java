package net.kingchoka.minetranslator.translation;

public record TranslationResult(
    TranslationRequest request,
    String translatedText,
    boolean success,
    String errorMessage
) {
    public static TranslationResult success(TranslationRequest request, String translatedText) {
        return new TranslationResult(request, translatedText, true, null);
    }

    public static TranslationResult failure(TranslationRequest request, String errorMessage) {
        return new TranslationResult(request, null, false, errorMessage);
    }
}
