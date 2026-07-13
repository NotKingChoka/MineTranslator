package net.kingchoka.mtlib.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MTLibrary {
    private static final List<TickListener> TICKS = new CopyOnWriteArrayList<TickListener>();
    private static final List<KeyListener> KEYS = new CopyOnWriteArrayList<KeyListener>();
    private static final List<ChatListener> CHAT = new CopyOnWriteArrayList<ChatListener>();
    private static final List<TooltipListener> TOOLTIPS = new CopyOnWriteArrayList<TooltipListener>();

    private MTLibrary() {}

    public static void onTick(TickListener listener) { TICKS.add(listener); }
    public static void onKey(KeyListener listener) { KEYS.add(listener); }
    public static void onChat(ChatListener listener) { CHAT.add(listener); }
    public static void onTooltip(TooltipListener listener) { TOOLTIPS.add(listener); }

    public static void fireTick(Object client) {
        for (TickListener listener : TICKS) listener.onTick(client);
    }

    public static boolean fireKey(Object keyboard, long window, int key, int scanCode, int action, int modifiers) {
        boolean cancel = false;
        for (KeyListener listener : KEYS) {
            cancel |= listener.onKey(keyboard, window, key, scanCode, action, modifiers);
        }
        return cancel;
    }

    public static boolean fireChat(Object chatHud, Object message) {
        boolean cancel = false;
        for (ChatListener listener : CHAT) cancel |= listener.onChat(chatHud, message);
        return cancel;
    }

    public static void fireTooltip(Object stack, List<Object> lines) {
        for (TooltipListener listener : TOOLTIPS) listener.onTooltip(stack, lines);
    }

    public interface TickListener { void onTick(Object client); }
    public interface KeyListener {
        boolean onKey(Object keyboard, long window, int key, int scanCode, int action, int modifiers);
    }
    public interface ChatListener { boolean onChat(Object chatHud, Object message); }
    public interface TooltipListener { void onTooltip(Object stack, List<Object> lines); }
}
