package net.kingchoka.minetranslatorport;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PortController {
    private static final PortController INSTANCE = new PortController();
    private static final long TOOLTIP_KEY_WINDOW_MS = 3000L;
    public static class ParseResult {
        public final boolean success;
        public final String prefix;
        public final String body;
        
        public ParseResult(boolean success, String prefix, String body) {
            this.success = success;
            this.prefix = prefix;
            this.body = body;
        }
    }

    private static final Pattern HYPIXEL_MSG = Pattern.compile(
        "^(.*?\\[([A-Za-z0-9_]{1,16}) head\\][A-Za-z0-9_]{1,16}:\\s*)(.*)$", Pattern.DOTALL
    );
    private static final Pattern ANGLE_MSG = Pattern.compile(
        "^(<([A-Za-z0-9_]{1,16})>\\s*)(.+)$", Pattern.DOTALL
    );
    private static final Pattern SIMPLE_MSG = Pattern.compile(
        "^(([A-Za-z0-9_]{1,16})\\s*[:\\u00BB\\u25B6]\\s*)(.+)$", Pattern.DOTALL
    );

    public static ParseResult parsePlayerMessage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ParseResult(false, null, null);
        }
        Matcher matcher = HYPIXEL_MSG.matcher(text);
        if (matcher.matches()) {
            return new ParseResult(true, matcher.group(1), matcher.group(3));
        }
        matcher = ANGLE_MSG.matcher(text);
        if (matcher.matches()) {
            return new ParseResult(true, matcher.group(1), matcher.group(3));
        }
        matcher = SIMPLE_MSG.matcher(text);
        if (matcher.matches()) {
            return new ParseResult(true, matcher.group(1), matcher.group(3));
        }
        return new ParseResult(false, null, null);
    }
    private static final Pattern TARGET_LANGUAGE = Pattern.compile("\\\"targetLanguage\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern AUTO_PLAYERS = Pattern.compile("\\\"autoTranslatePlayerMessages\\\"\\s*:\\s*(true|false)");

    private final Queue<Runnable> renderTasks = new ConcurrentLinkedQueue<Runnable>();
    private final List<ChatRecord> chat = Collections.synchronizedList(new ArrayList<ChatRecord>());
    private final Map<String, TooltipState> tooltips = Collections.synchronizedMap(new HashMap<String, TooltipState>());
    private final Map<Object, InputState> inputs = Collections.synchronizedMap(new WeakHashMap<Object, InputState>());

    private volatile TooltipFrame lastTooltip;
    private volatile boolean suppressChat;
    private volatile boolean initialized;
    public static PortController get() { return INSTANCE; }

    public void clearCache() {
        chat.clear();
        tooltips.clear();
        inputs.clear();
    }

    synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        ModConfig.load();
        System.out.println("[MineTranslator] Cross-version port initialized; target=" + targetLanguage()
            + ", autoPlayers=" + autoTranslatePlayerMessages());
    }

    void onTick(Object client) {
        ReflectionAccess.ensureKeyMappings(
            client,
            FabricLoader.getInstance().getGameDir().toFile()
        );
        Runnable task;
        int limit = 100;
        while (limit-- > 0 && (task = renderTasks.poll()) != null) {
            try { task.run(); } catch (Exception ignored) {}
        }
    }

    boolean onKey(Object keyboard, long window, int key, int scanCode, int action, int modifiers) {
        if (action != 1) return false;
        Object client = ReflectionAccess.minecraft();
        ReflectionAccess.ensureKeyMappings(
            client,
            FabricLoader.getInstance().getGameDir().toFile()
        );

        if (key == ReflectionAccess.configKeyCode()) {
            Object screen = ReflectionAccess.currentScreen(client);
            if (screen == null) {
                ReflectionAccess.openMineTranslatorConfig(client);
                return true;
            }
        }

        int translateKey = ReflectionAccess.translateKeyCode();
        if (key != translateKey && !(translateKey == 96 && scanCode == 41)) return false;
        Object screen = ReflectionAccess.currentScreen(client);
        boolean chatScreen = ReflectionAccess.isChatScreen(screen);

        // Inventory screens (especially the creative search tab) also own text fields.
        // Treating those as chat input translated the search query instead of the
        // hovered item and made the item/tooltip disappear from the filtered results.
        if (!chatScreen) {
            TooltipFrame tooltip = lastTooltip;
            if (tooltip != null && System.currentTimeMillis() - tooltip.seenAt < TOOLTIP_KEY_WINDOW_MS) {
                toggleTooltip(tooltip);
                return true;
            }
        }

        if (chatScreen) {
            Object textField = ReflectionAccess.textField(screen);
            if (textField != null) {
                String current = ReflectionAccess.inputText(textField);
                if (current != null && !current.trim().isEmpty()) {
                    toggleInput(textField, current);
                    return true;
                }
            }
            toggleHoveredChat(client);
            return true;
        }
        return false;
    }

    boolean onChat(Object chatHud, Object message) {
        if (message == null) {
            System.out.println("[MineTranslator Debug] onChat called with null message");
            return false;
        }
        String source = ReflectionAccess.text(message);
        System.out.println("[MineTranslator Debug] onChat text: " + source);
        if (source == null || source.trim().isEmpty()) return false;

        final ChatRecord record = new ChatRecord(chatHud, message, source);
        synchronized (chat) {
            chat.add(record);
            while (chat.size() > 250) chat.remove(0);
        }

        ParseResult parse = parsePlayerMessage(source);
        boolean isPlayer = parse.success;
        boolean isNPC = source.contains("[NPC]");

        boolean shouldTranslate = ModConfig.getInstance().autoTranslateEveryMessage
            || (isPlayer && ModConfig.getInstance().autoTranslatePlayerMessages)
            || (isNPC && ModConfig.getInstance().translateOnlyNPC);

        System.out.println("[MineTranslator Debug] isPlayer: " + isPlayer + ", isNPC: " + isNPC + ", shouldTranslate: " + shouldTranslate);

        if (!shouldTranslate) return false;

        String textToTranslate = isPlayer ? parse.body : source;
        System.out.println("[MineTranslator Debug] Translating: " + textToTranslate);

        GoogleTranslator.translate(textToTranslate, "auto", targetLanguage()).thenAccept(translated -> {
            System.out.println("[MineTranslator Debug] Translated result: " + translated);
            if (isPlayer) {
                record.translated = parse.prefix + translated;
            } else {
                record.translated = translated;
            }
            record.active = true;
            Object translatedComponent = ReflectionAccess.styledLiteral(record.translated, message);
            if (translatedComponent != null) {
                System.out.println("[MineTranslator Debug] StyledLiteral created, queueing replacement");
                renderTasks.add(() -> ReflectionAccess.replaceMessageInChat(chatHud, source, translatedComponent));
            } else {
                System.out.println("[MineTranslator Debug] StyledLiteral was null!");
            }
        });
        return false;
    }

    void onTooltip(Object stack, List<Object> lines) {
        if (lines == null || lines.isEmpty()) return;
        List<String> originals = new ArrayList<String>(lines.size());
        for (Object line : lines) originals.add(ReflectionAccess.text(line));
        String key = buildTooltipKey(stack, originals);
        TooltipState state = tooltips.get(key);
        lastTooltip = new TooltipFrame(key, new ArrayList<Object>(lines), originals, System.currentTimeMillis());
        if (state == null || !state.active || state.translated == null) return;
        int count = Math.min(lines.size(), state.translated.size());
        for (int i = 0; i < count; i++) {
            String sourceLine = originals.get(i);
            // Keep separator/unknown components intact. Replacing an unresolved
            // component with LiteralText("") is what produced the tiny empty box.
            if (!usableTooltipText(sourceLine)) continue;
            String translatedLine = safeTranslation(sourceLine, state.translated.get(i));
            Object translated = ReflectionAccess.styledLiteral(translatedLine, lines.get(i));
            String rendered = ReflectionAccess.text(translated);
            if (translated != null && rendered != null && !rendered.trim().isEmpty()) {
                try { lines.set(i, translated); } catch (Exception ignored) {}
            }
        }
    }

    private void toggleInput(final Object textField, String current) {
        InputState previous = inputs.get(textField);
        if (previous != null && previous.translated != null && current.equals(previous.translated)) {
            ReflectionAccess.setInputText(textField, previous.original);
            inputs.remove(textField);
            return;
        }
        final InputState state = new InputState(current);
        inputs.put(textField, state);
        GoogleTranslator.translate(current, "auto", targetLanguage()).thenAccept(translated -> {
            String safe = safeTranslation(current, translated);
            state.translated = safe;
            renderTasks.add(() -> ReflectionAccess.setInputText(textField, safe));
        });
    }

    private void toggleTooltip(TooltipFrame frame) {
        TooltipState state = tooltips.get(frame.key);
        if (state != null && state.active) {
            state.active = false;
            return;
        }
        if (state == null) {
            state = new TooltipState();
            tooltips.put(frame.key, state);
        }
        state.active = true;
        if (state.translated != null || state.loading) return;
        state.loading = true;
        System.out.println("[MineTranslator] Tooltip translation requested: lines=" + frame.originals.size());
        final TooltipState target = state;
        List<CompletableFuture<String>> futures = new ArrayList<CompletableFuture<String>>();
        for (String line : frame.originals) {
            if (!usableTooltipText(line)) {
                futures.add(CompletableFuture.completedFuture(line));
            } else {
                futures.add(GoogleTranslator.translate(line, "auto", targetLanguage()));
            }
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[futures.size()])).thenRun(() -> {
            List<String> translated = new ArrayList<String>(futures.size());
            for (int i = 0; i < futures.size(); i++) {
                translated.add(safeTranslation(frame.originals.get(i), futures.get(i).join()));
            }
            target.translated = translated;
            target.loading = false;
            System.out.println("[MineTranslator] Tooltip translation ready: lines=" + translated.size());
        });
    }

    private void toggleHoveredChat(Object client) {
        final ChatRecord record;
        synchronized (chat) {
            if (chat.isEmpty()) return;
            int newestOffset = ReflectionAccess.hoveredChatIndex(client, chat.size());
            int index = chat.size() - 1 - Math.max(0, newestOffset);
            if (index < 0 || index >= chat.size()) return;
            record = chat.get(index);
        }
        if (record.translated != null) {
            record.active = !record.active;
            Object targetComponent = record.active 
                ? ReflectionAccess.styledLiteral(record.translated, record.originalComponent)
                : record.originalComponent;
            String searchText = record.active ? record.originalText : record.translated;
            ReflectionAccess.replaceMessageInChat(record.chatHud, searchText, targetComponent);
            return;
        }
        ParseResult parse = parsePlayerMessage(record.originalText);
        final boolean isPlayer = parse.success;
        String textToTranslate = isPlayer ? parse.body : record.originalText;
        GoogleTranslator.translate(textToTranslate, "auto", targetLanguage()).thenAccept(translated -> {
            if (isPlayer) {
                record.translated = parse.prefix + translated;
            } else {
                record.translated = translated;
            }
            record.active = true;
            Object targetComponent = ReflectionAccess.styledLiteral(record.translated, record.originalComponent);
            if (targetComponent != null) {
                renderTasks.add(() -> ReflectionAccess.replaceMessageInChat(record.chatHud, record.originalText, targetComponent));
            }
        });
    }

    private void rebuildChat(Object chatHud) {}

    private String buildTooltipKey(Object stack, List<String> lines) {
        StringBuilder key = new StringBuilder(stack == null ? "null" : stack.getClass().getName());
        for (String line : lines) key.append('\u001f').append(line);
        return key.toString();
    }

    private void loadExistingConfig() {
        ModConfig.load();
    }

    public String targetLanguage() { return ModConfig.getInstance().targetLanguage; }

    public boolean autoTranslatePlayerMessages() { return ModConfig.getInstance().autoTranslatePlayerMessages; }

    public void cycleTargetLanguage() {
        String[] languages = {"ru", "en", "kk", "uk", "de", "fr", "es", "pt", "zh", "ja", "ko"};
        int index = 0;
        String current = targetLanguage();
        for (int i = 0; i < languages.length; i++) {
            if (languages[i].equals(current)) { index = i; break; }
        }
        ModConfig.getInstance().targetLanguage = languages[(index + 1) % languages.length];
        saveConfig();
    }

    public void toggleAutoTranslatePlayerMessages() {
        ModConfig.getInstance().autoTranslatePlayerMessages = !ModConfig.getInstance().autoTranslatePlayerMessages;
        saveConfig();
    }

    public void saveConfig() {
        ModConfig.getInstance().save();
    }

    private static String insertJsonProperty(String json, String property) {
        int close = json.lastIndexOf('}');
        if (close < 0) return "{\n" + property + "\n}\n";
        String before = json.substring(0, close).trim();
        boolean hasProperty = before.length() > 1 && !before.endsWith("{");
        return json.substring(0, close) + (hasProperty ? ",\n" : "\n") + property + "\n" + json.substring(close);
    }

    private static String safeTranslation(String original, String translated) {
        if (original == null) return translated == null ? "" : translated;
        if (translated == null || translated.trim().isEmpty()) return original;
        return translated;
    }

    private static boolean usableTooltipText(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String lower = text.toLowerCase();
        return !lower.contains("clickevent=")
            && !lower.contains("hoverevent=")
            && !lower.contains("insertion=")
            && !lower.contains("font=minecraft:")
            && !lower.contains("siblings=")
            && !lower.contains("style=style{");
    }

    private static final class ChatRecord {
        final Object chatHud;
        final Object originalComponent;
        final String originalText;
        volatile String translated;
        volatile boolean active;

        ChatRecord(Object chatHud, Object originalComponent, String originalText) {
            this.chatHud = chatHud;
            this.originalComponent = originalComponent;
            this.originalText = originalText;
        }
    }

    private static final class TooltipFrame {
        final String key;
        final List<Object> components;
        final List<String> originals;
        final long seenAt;

        TooltipFrame(String key, List<Object> components, List<String> originals, long seenAt) {
            this.key = key;
            this.components = components;
            this.originals = originals;
            this.seenAt = seenAt;
        }
    }

    private static final class TooltipState {
        volatile boolean active;
        volatile boolean loading;
        volatile List<String> translated;
    }

    private static final class InputState {
        final String original;
        volatile String translated;
        InputState(String original) { this.original = original; }
    }
}
