package net.kingchoka.minetranslator.chat;

import net.minecraft.client.GuiMessage;
import net.minecraft.network.chat.Component;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ChatEntryStore {
    private static final ChatEntryStore INSTANCE = new ChatEntryStore();

    public static ChatEntryStore getInstance() {
        return INSTANCE;
    }

    private final AtomicLong idSequence = new AtomicLong(0);
    private final Map<GuiMessage, ChatTranslationState> store = Collections.synchronizedMap(new WeakHashMap<>());

    private ChatEntryStore() {}

    public ChatTranslationState getOrCreateState(GuiMessage message) {
        return store.computeIfAbsent(message, msg -> new ChatTranslationState(idSequence.incrementAndGet(), msg.content()));
    }

    public ChatTranslationState getState(GuiMessage message) {
        return store.get(message);
    }
}
