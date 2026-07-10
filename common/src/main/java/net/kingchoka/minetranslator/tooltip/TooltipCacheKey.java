package net.kingchoka.minetranslator.tooltip;

public record TooltipCacheKey(
    String provider,
    String itemId,
    String originalText,
    String targetLanguage
) {}
