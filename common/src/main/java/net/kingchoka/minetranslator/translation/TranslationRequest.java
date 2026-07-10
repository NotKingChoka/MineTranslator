package net.kingchoka.minetranslator.translation;

import java.util.List;

public record TranslationRequest(
    String sourceText,
    String sourceLanguage,
    String targetLanguage,
    TranslationMode mode,
    List<String> context,
    String providerId
) {}
