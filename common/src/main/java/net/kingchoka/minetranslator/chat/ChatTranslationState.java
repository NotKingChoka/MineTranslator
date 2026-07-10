package net.kingchoka.minetranslator.chat;

import net.minecraft.network.chat.Component;

public class ChatTranslationState {
    public enum TranslationStatus {
        ORIGINAL,
        QUEUED,
        TRANSLATING,
        TRANSLATED,
        FAILED
    }

    public final long entryId;
    public final Component originalText;
    public Component translatedText;
    public TranslationStatus status = TranslationStatus.ORIGINAL;

    public ChatTranslationState(long entryId, Component originalText) {
        this.entryId = entryId;
        this.originalText = originalText;
    }
}
