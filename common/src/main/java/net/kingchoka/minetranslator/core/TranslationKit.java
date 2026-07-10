package net.kingchoka.minetranslator.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.kingchoka.minetranslator.MineTranslator;
import net.kingchoka.minetranslator.api.ChatComponentMixinAccessor;
import net.kingchoka.minetranslator.api.ChatScreenMixinAccessor;
import net.kingchoka.minetranslator.config.MTConfig;
import net.kingchoka.minetranslator.event.ItemTooltipCallbacks;
import net.kingchoka.minetranslator.event.ScreenCallbacks;
import net.kingchoka.minetranslator.exception.ServiceException;
import net.kingchoka.minetranslator.keybind.MTKeyMappings;
import net.kingchoka.minetranslator.tool.ClientUtl;
import net.kingchoka.minetranslator.tool.TooltipUtl;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * The main class to manage all translation process.
 */
public final class TranslationKit {

    private static final Pattern HYPIXEL_PLAYER_MESSAGE = Pattern.compile(
        "^(?<prefix>.*?\\[(?<headName>[A-Za-z0-9_]{3,16}) head\\])(?<username>[A-Za-z0-9_]{3,16})(?<separator>:\\s*)(?<body>.*)$"
    );

    static final TranslationKit INSTANCE = new TranslationKit();
    static final Gson GSON = new GsonBuilder().create();
    
    public static boolean debugForceTranslateAll = false;

    public static final String SUCCESS = "<O>";
    public static final String PROCESSING = "<?>";
    public static final String ERROR = "<X>";

    private static final AtomicInteger taskCounter = new AtomicInteger(0);
    private static final ExecutorService translationExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread translationThread = new Thread(r, "Translation-Worker-" + taskCounter.incrementAndGet());
        translationThread.setDaemon(true); // Allow JVM to exit even if this thread is running
        return translationThread;
    });

    public static TranslationKit getInstance() {
        return INSTANCE;
    }

    /**
     * A cache with LRU eviction policy to store recent translations.
     */
    public final Map<String, String> translationCache = Collections.synchronizedMap(
            new LinkedHashMap<>(100, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > MTConfig.getInstance().getMaxCacheSize();
                }
            }
    );

    private final Set<String> activeTooltipRequests = Collections.synchronizedSet(new HashSet<>());

    /**
     * The text currently being hovered over, null if none.
     */
    @Nullable
    private String hoveredText = null;

    /**
     * The text that is being translated, null if not translating yet.
     */
    @Nullable
    private String translatedText = null;

    /**
     * The result of the translation, null if not translated yet.
     */
    @Nullable
    private volatile String translatedResult = null;

    /**
     * Whether a translation is in progress or completed.
     */
    private volatile boolean translated = false;

    /**
     * Used when a screen open, key.isDown() won't work.
     * When there is no screen open, set to false.
     */
    private boolean translateKeyDown = false;

    private CompletableFuture<Void> translationFuture = null;

    // Chat freeze & buffering states
    private GuiMessage hoveredChatMessage = null;
    private int hoveredChatMessageIndex = -1;

    private GuiMessage lockedChatMessage = null;
    private int lockedChatMessageIndex = -1;
    private GuiMessage originalChatMessage = null;
    private GuiMessage translatedChatMessage = null;
    private boolean chatFrozen = false;

    public static class BufferedMessage {
        public final Component message;
        public final MessageSignature signature;
        public final GuiMessageTag tag;

        public BufferedMessage(Component message, MessageSignature signature, GuiMessageTag tag) {
            this.message = message;
            this.signature = signature;
            this.tag = tag;
        }
    }

    private final List<BufferedMessage> chatBuffer = new ArrayList<>();

    private static final java.util.concurrent.atomic.AtomicLong messageSequence = new java.util.concurrent.atomic.AtomicLong(0);

    public static class TranslationState {
        public final long id;
        public final GuiMessage original;
        public GuiMessage current;
        public Component translatedContent = null;
        public boolean isTranslating = false;
        
        public TranslationState(GuiMessage original) {
            this.id = messageSequence.incrementAndGet();
            this.original = original;
            this.current = original;
        }
    }

    private final List<TranslationState> activeTranslations = Collections.synchronizedList(new ArrayList<>());

    public TranslationState registerTranslationState(GuiMessage original) {
        synchronized (activeTranslations) {
            for (var s : activeTranslations) {
                if (s.original == original || s.current == original) {
                    return s;
                }
            }
            TranslationState state = new TranslationState(original);
            activeTranslations.add(state);
            if (activeTranslations.size() > 200) {
                activeTranslations.remove(0); // Remove oldest
            }
            return state;
        }
    }

    public TranslationState getTranslationState(GuiMessage message) {
        synchronized (activeTranslations) {
            for (var state : activeTranslations) {
                if (state.original == message || state.current == message) {
                    return state;
                }
            }
        }
        return null;
    }

    public TranslationState getTranslationStateById(long id) {
        synchronized (activeTranslations) {
            for (var state : activeTranslations) {
                if (state.id == id) {
                    return state;
                }
            }
        }
        return null;
    }

    public static class PlayerChatParseResult {
        public final String prefix;
        public final String username;
        public final String separator;
        public final String body;
        public final int bodyStartIndex;
        public final boolean valid;
        
        public PlayerChatParseResult(String prefix, String username, String separator, String body, int bodyStartIndex, boolean valid) {
            this.prefix = prefix;
            this.username = username;
            this.separator = separator;
            this.body = body;
            this.bodyStartIndex = bodyStartIndex;
            this.valid = valid;
        }
        
        public static PlayerChatParseResult invalid() {
            return new PlayerChatParseResult("", "", "", "", -1, false);
        }
    }

    public static PlayerChatParseResult parsePlayerMessage(String fullString) {
        if (fullString == null || fullString.isBlank()) {
            return PlayerChatParseResult.invalid();
        }

        // 1. Try Hypixel structural format first
        Matcher matcher = HYPIXEL_PLAYER_MESSAGE.matcher(fullString);
        if (matcher.matches()) {
            String headName = matcher.group("headName");
            String username = matcher.group("username");
            String prefix = matcher.group("prefix");
            String separator = matcher.group("separator");
            String body = matcher.group("body");
            
            if (headName.equals(username)) {
                int bodyStartIndex = prefix.length() + username.length() + separator.length();
                return new PlayerChatParseResult(prefix, username, separator, body, bodyStartIndex, true);
            }
        }

        // 2. Fallback: Parse messages without head marker
        int sepIdx = -1;
        
        for (int i = 0; i < fullString.length(); i++) {
            char c = fullString.charAt(i);
            if (c == ':' || c == '»' || c == '▶') {
                sepIdx = i;
                break;
            }
        }
        
        if (sepIdx != -1) {
            String header = fullString.substring(0, sepIdx).trim();
            String[] words = header.split("\\s+");
            if (words.length > 0) {
                String rawCandidate = words[words.length - 1].trim();
                String candidate = rawCandidate.replaceAll("[\\[\\]\\(\\)\\{\\}]", "").trim();
                
                if (candidate.matches("^[a-zA-Z0-9_]{3,16}$")) {
                    boolean isLocalPlayer = isPlayerInTabList(candidate);
                    boolean isPrivateMessage = header.toLowerCase(Locale.ROOT).contains("from") || 
                                               header.toLowerCase(Locale.ROOT).contains("to") || 
                                               header.contains("сообщение");
                    
                    if (isLocalPlayer || isPrivateMessage) {
                        int candidateIdx = fullString.lastIndexOf(rawCandidate, sepIdx);
                        if (candidateIdx != -1) {
                            String prefix = fullString.substring(0, candidateIdx);
                            int restIdx = sepIdx + 1;
                            while (restIdx < fullString.length() && Character.isWhitespace(fullString.charAt(restIdx))) {
                                restIdx++;
                            }
                            String finalSeparator = fullString.substring(candidateIdx + rawCandidate.length(), restIdx);
                            String body = fullString.substring(restIdx);
                            
                            return new PlayerChatParseResult(
                                prefix,
                                candidate,
                                finalSeparator,
                                body,
                                restIdx,
                                true
                            );
                        }
                    }
                }
            }
        }

        return PlayerChatParseResult.invalid();
    }

    private static boolean isPlayerInTabList(String username) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            for (var info : connection.getOnlinePlayers()) {
                if (info.getProfile() != null && username.equalsIgnoreCase(info.getProfile().name())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void runParserTests() {
        MineTranslator.LOGGER.info("[MineTranslator] Running PlayerMessageParser unit tests...");
        
        String[] testStrings = {
            "[421] ⛃ [MVP++] [Frank_lol_ head]Frank_lol_: selling for lbin - tax :D",
            "[110] ⛃ [VIP] [DOTDOTDOTDOT500 head]DOTDOTDOTDOT500: feed me fellas in bank",
            "[163] ⛃ [MVP+] [Blue_Non head]Blue_Non: can someone apply ancient on my tara cp?",
            "[548] ⛃ [MVP+] [Donivan_White head]Donivan_White: eating yellow at bank please",
            "[343] ⛃ [VIP] [Walking_Pepper head]Walking_Pepper: Lowballing 2b",
            "[397] ⛃ [MVP+] [KyleLikesCoffee head]KyleLikesCoffee: am i full",
            "[357] ⛃ [VIP] [pisztrang head]pisztrang: ty"
        };
        
        String[] expectedUsers = {
            "Frank_lol_", "DOTDOTDOTDOT500", "Blue_Non", "Donivan_White", "Walking_Pepper", "KyleLikesCoffee", "pisztrang"
        };
        
        String[] expectedBodies = {
            "selling for lbin - tax :D",
            "feed me fellas in bank",
            "can someone apply ancient on my tara cp?",
            "eating yellow at bank please",
            "Lowballing 2b",
            "am i full",
            "ty"
        };
        
        int passed = 0;
        for (int i = 0; i < testStrings.length; i++) {
            PlayerChatParseResult res = parsePlayerMessage(testStrings[i]);
            boolean matches = res.valid && res.username.equals(expectedUsers[i]) && res.body.equals(expectedBodies[i]);
            if (matches) {
                passed++;
                MineTranslator.LOGGER.info("[MineTranslator] Test {} PASSED: User: '{}', Body: '{}'", i + 1, res.username, res.body);
            } else {
                MineTranslator.LOGGER.error("[MineTranslator] Test {} FAILED!", i + 1);
                MineTranslator.LOGGER.error("  Input:    {}", testStrings[i]);
                MineTranslator.LOGGER.error("  Expected: User: '{}', Body: '{}'", expectedUsers[i], expectedBodies[i]);
                MineTranslator.LOGGER.error("  Actual:   Valid: {}, User: '{}', Body: '{}'", res.valid, res.username, res.body);
            }
        }
        
        MineTranslator.LOGGER.info("[MineTranslator] Parser tests completed. Passed: {}/{}", passed, testStrings.length);
    }

    private TranslationKit() {
        runParserTests();
    }

    public @Nullable String getHoveredText() {
        return hoveredText;
    }

    public void setHoveredText(@Nullable ItemStack stack, Minecraft client) {
        if (this.translateKeyDown) {
            return;
        }
        if (stack == null) {
            this.hoveredText = null;
            return;
        }
        this.hoveredText = TooltipUtl.getCombinedTooltipText(stack, client);
    }

    public void setHoveredText(List<Component> tooltip) {
        if (this.translateKeyDown) {
            return;
        }
        if (tooltip.isEmpty()) {
            this.hoveredText = null;
            return;
        }
        this.hoveredText = TooltipUtl.getCombinedTooltipText(tooltip);
    }

    public void setHoveredText(@Nullable String text) {
        if (this.translateKeyDown) {
            return;
        }
        this.hoveredText = text;
    }

    public void updateHoveredChatMessage(@Nullable GuiMessage message, int index) {
        if (this.chatFrozen) {
            return;
        }
        this.hoveredChatMessage = message;
        this.hoveredChatMessageIndex = index;
    }

    public boolean isChatFrozen() {
        return this.chatFrozen;
    }

    public void resetChatFreeze() {
        this.chatFrozen = false;
        this.lockedChatMessage = null;
        this.lockedChatMessageIndex = -1;
        this.originalChatMessage = null;
        this.translatedChatMessage = null;
        this.chatBuffer.clear();
    }

    public void bufferChatMessage(Component message, @Nullable MessageSignature signature, @Nullable GuiMessageTag tag) {
        this.chatBuffer.add(new BufferedMessage(message, signature, tag));
    }

    public @Nullable String getTranslatedText() {
        return translatedText;
    }

    public @Nullable String getTranslatedResult() {
        return translatedResult;
    }

    public boolean isTranslated() {
        return translated;
    }

    public boolean isKeyDown() {
        return translateKeyDown;
    }

    private void setKeyDown(boolean isKeyDown) {
        translateKeyDown = isKeyDown;
    }

    /**
     * Start translating the currently hovered text.
     */
    public void start(Minecraft client) {
        if (hoveredText == null || hoveredText.equals(translatedText)) {
            // Already translating or translated this exact stack instance
            return;
        }

        // Cancel any previous ongoing translation
        this.stop();

        translatedText = hoveredText;

        // Lock chat message if we are hovering over one
        if (this.hoveredChatMessage != null && this.hoveredChatMessageIndex != -1) {
            this.lockedChatMessage = this.hoveredChatMessage;
            this.lockedChatMessageIndex = this.hoveredChatMessageIndex;
            this.originalChatMessage = this.hoveredChatMessage;
            this.chatFrozen = true;
        }

        MTConfig config = MTConfig.getInstance();
        String targetLang = config.getTargetLanguage();

        if (this.originalChatMessage != null) {
            TranslationState state = registerTranslationState(this.originalChatMessage);
            long messageId = state.id;

            SplitComponent playerSplit = splitPlayerMessage(this.originalChatMessage.content());
            boolean isPlayer = playerSplit != null;
            SplitComponent split = isPlayer ? playerSplit : splitAtFirstColon(this.originalChatMessage.content());
            boolean isNpc = isNpcMessage(this.originalChatMessage.content());

            String origBodyText = split.body.getString();
            String textToTranslate = origBodyText.trim();
            
            // Set initial state
            translatedText = textToTranslate;
            translatedResult = I18n.get("misc.MineTranslator.translation.processing") + PROCESSING;
            if (this.chatFrozen && this.lockedChatMessage != null) {
                this.applyChatTranslation(client);
            }

            List<String> contextList = getTranslationContext(
                client.gui.getChat(),
                config.getUseNpcContextOnly(),
                config.getTranslationContextSize()
            );

            TranslationRequest request = new TranslationRequest(
                messageId,
                this.originalChatMessage,
                this.originalChatMessage.content(),
                split,
                targetLang,
                contextList,
                isPlayer,
                isNpc
            );

            MineTranslator.LOGGER.info("[MineTranslator][CHAT TRACE] Manual Hover triggers request for ID: {}", messageId);

            this.translationFuture = translateChatMessage(request, finalComp -> {
                translatedResult = finalComp.getString() + SUCCESS;
                translated = true;
                client.execute(() -> {
                    if (this.chatFrozen && this.lockedChatMessage != null) {
                        this.translatedChatMessage = new GuiMessage(
                            this.originalChatMessage.addedTime(),
                            finalComp,
                            this.originalChatMessage.signature(),
                            this.originalChatMessage.tag()
                        );
                        client.gui.getChat().allMessages.set(this.lockedChatMessageIndex, this.translatedChatMessage);
                        ((ChatComponentMixinAccessor) client.gui.getChat()).MineTranslator$refreshTrimmedMessages();
                    }
                });
            }).exceptionally(err -> {
                MineTranslator.LOGGER.error("Player manual translation failed for: {}. Cause: {}", textToTranslate, err.getCause());
                translatedResult = I18n.get("misc.MineTranslator.translation.failed") + ERROR;
                client.execute(() -> {
                    if (this.chatFrozen && this.lockedChatMessage != null) {
                        this.applyChatTranslation(client);
                    }
                });
                this.clientExecuteSendingError(client, err.getCause());
                return null;
            });
            return;
        }

        // Fallback for non-player messages (NPC, items, tooltips)
        // Check cache first
        String cacheKey = translatedText + "|" + targetLang;
        String cachedResult = translationCache.get(cacheKey);
        if (cachedResult != null) {
            translatedResult = I18n.get("misc.MineTranslator.translation", cachedResult) + SUCCESS;
            translated = true;
            translationFuture = CompletableFuture.completedFuture(null); // Create a completed future
            if (this.chatFrozen && this.lockedChatMessage != null) {
                this.applyChatTranslation(client);
            }
            return; // Skip API call
        }

        translatedResult = I18n.get("misc.MineTranslator.translation.processing") + PROCESSING; // Initial placeholder
        translated = true; // Set translated flag immediately
        if (this.chatFrozen && this.lockedChatMessage != null) {
            this.applyChatTranslation(client);
        }

        translationFuture = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return MTConfig.getInstance().getService().provider.translate(
                                translatedText,
                                MTConfig.getInstance().getSourceLanguage(),
                                targetLang,
                                new java.util.ArrayList<>()
                        );
                    } catch (Exception e) {
                        throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
                    }
                }, translationExecutor)
                .thenAcceptAsync(it -> {
                    // Update the result and cache it
                    translatedResult = it + SUCCESS;
                    translationCache.put(cacheKey, it); // Add to cache
                    client.execute(() -> {
                        if (this.chatFrozen && this.lockedChatMessage != null) {
                            this.applyChatTranslation(client);
                        }
                    });
                }, translationExecutor)
                .exceptionally(err -> {
                    MineTranslator.LOGGER.error("Translation failed for: {}. Cause: {}", translatedText, err.getCause());
                    translatedResult = I18n.get("misc.MineTranslator.translation.failed") + ERROR;
                    client.execute(() -> {
                        if (this.chatFrozen && this.lockedChatMessage != null) {
                            this.applyChatTranslation(client);
                        }
                    });
                    this.clientExecuteSendingError(client, err.getCause());
                    return null; // Indicate exception was handled
                });
    }

    private void clientExecuteSendingError(Minecraft client, Throwable throwable) {
        client.execute(() -> sendErrorToClient(client, throwable));
    }

    private void sendErrorToClient(Minecraft client, Throwable err) {
        if (err instanceof ServiceException se) {
            ClientUtl.message(client, Component.translatable("misc.MineTranslator.translation.failed.chat.status_code",
                    se.statusCode, se.getMessage()).withStyle(ChatFormatting.RED));
        } else {
            ClientUtl.message(client, Component.translatable("misc.MineTranslator.translation.failed.chat", err.toString()).withStyle(ChatFormatting.RED));
        }
    }

    /**
     * Stop any ongoing translation and clear the translated state.
     */
    public void stop() {
        if (this.translated) {
            if (translationFuture != null && !translationFuture.isDone()) {
                translationFuture.cancel(true);
            }
            translated = false;
            translatedText = null;
            translatedResult = null;
            translationFuture = null;
        }
        if (this.chatFrozen) {
            this.releaseChatFreeze(Minecraft.getInstance());
        }
    }

    /**
     * Clear the translation cache.
     */
    public void clearCache() {
        this.translationCache.clear();
    }

    /**
     * Get the default style and split result lines.
     */
    public Pair<Style, String[]> getStyledResultLines() {
        Style appliedStyle = Style.EMPTY;
        String resultText = this.translatedResult;

        switch (resultText.substring(resultText.length() - 3)) {
            case PROCESSING -> appliedStyle = appliedStyle.withColor(ChatFormatting.DARK_GRAY);
            case ERROR -> appliedStyle = appliedStyle.withColor(ChatFormatting.RED);
            default -> appliedStyle = appliedStyle.withColor(ChatFormatting.GRAY); // SUCCESS
        }

        String[] texts = resultText.substring(0, resultText.length() - 3).split(literalSeparator());
        return Pair.of(appliedStyle, texts);
    }

    /**
     * Replace the original tooltip with translation.
     */
    public void replaceTooltipWithTranslation(List<Component> lines) {
        var styledResult = getStyledResultLines();
        String[] texts = styledResult.getRight();

        for (int i = 0; i < lines.size(); i++) {
            if (i < texts.length) {
                Component originalLine = lines.get(i);
                Component translatedLine = applyStyleHierarchy(originalLine, texts[i]);
                lines.set(i, translatedLine);
            }
        }
    }

    public void applyChatTranslation(Minecraft client) {
        ChatComponent chat = client.gui.getChat();
        if (this.lockedChatMessageIndex >= 0 && this.lockedChatMessageIndex < chat.allMessages.size()) {
            GuiMessage current = chat.allMessages.get(this.lockedChatMessageIndex);
            if (current == this.originalChatMessage || current == this.translatedChatMessage) {
                var styledResult = getStyledResultLines();
                String[] texts = styledResult.getRight();
                String resultText = String.join("", texts);
                Component translatedComp = applyStyleHierarchy(this.originalChatMessage.content(), resultText);

                this.translatedChatMessage = new GuiMessage(
                    this.originalChatMessage.addedTime(),
                    translatedComp,
                    this.originalChatMessage.signature(),
                    this.originalChatMessage.tag()
                );
                chat.allMessages.set(this.lockedChatMessageIndex, this.translatedChatMessage);
                ((ChatComponentMixinAccessor) chat).MineTranslator$refreshTrimmedMessages();
            }
        }
    }

    public void releaseChatFreeze(Minecraft client) {
        this.chatFrozen = false;

        // Restore original message
        if (this.lockedChatMessage != null && this.originalChatMessage != null) {
            ChatComponent chat = client.gui.getChat();
            if (this.lockedChatMessageIndex >= 0 && this.lockedChatMessageIndex < chat.allMessages.size()) {
                GuiMessage current = chat.allMessages.get(this.lockedChatMessageIndex);
                if (current == this.translatedChatMessage || current == this.lockedChatMessage) {
                    chat.allMessages.set(this.lockedChatMessageIndex, this.originalChatMessage);
                }
            }
        }

        // Play back buffered messages
        if (!this.chatBuffer.isEmpty()) {
            ChatComponent chat = client.gui.getChat();
            ChatComponentMixinAccessor accessor = (ChatComponentMixinAccessor) chat;
            for (var msg : this.chatBuffer) {
                accessor.MineTranslator$addMessageDirect(msg.message, msg.signature, msg.tag);
            }
            this.chatBuffer.clear();
        }

        // Refresh display
        ((ChatComponentMixinAccessor) client.gui.getChat()).MineTranslator$refreshTrimmedMessages();

        // Reset states
        this.lockedChatMessage = null;
        this.lockedChatMessageIndex = -1;
        this.originalChatMessage = null;
        this.translatedChatMessage = null;
    }

    public static Component applyStyleHierarchy(Component original, String translatedText) {
        if (original.getSiblings().isEmpty()) {
            return Component.literal(translatedText).withStyle(original.getStyle());
        }

        List<Component> leaves = new ArrayList<>();
        original.visit((style, text) -> {
            if (!text.isEmpty()) {
                leaves.add(Component.literal(text).withStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);

        if (leaves.isEmpty()) {
            return Component.literal(translatedText).withStyle(original.getStyle());
        }

        if (leaves.size() == 1) {
            return Component.literal(translatedText).withStyle(leaves.get(0).getStyle());
        }

        var rootComponent = Component.empty().withStyle(original.getStyle());
        int transIdx = 0;
        int origTotalLen = 0;
        for (Component leaf : leaves) {
            origTotalLen += leaf.getString().length();
        }

        for (int i = 0; i < leaves.size(); i++) {
            Component leaf = leaves.get(i);
            String leafText = leaf.getString();
            Style leafStyle = leaf.getStyle();

            if (transIdx >= translatedText.length()) {
                break;
            }

            if (i == leaves.size() - 1) {
                String part = translatedText.substring(transIdx);
                rootComponent.append(Component.literal(part).withStyle(leafStyle));
                break;
            }

            int nextTransIdx = -1;
            boolean currentIsNumeric = leafText.matches("^[0-9\\+\\-\\%\\s\\:\\(\\)\\[\\]\\.\\,]+$");
            if (currentIsNumeric) {
                int matchPos = translatedText.indexOf(leafText, transIdx);
                if (matchPos == transIdx) {
                    nextTransIdx = transIdx + leafText.length();
                }
            }

            if (nextTransIdx == -1) {
                Component nextLeaf = leaves.get(i + 1);
                String nextLeafText = nextLeaf.getString();
                boolean nextIsNumeric = nextLeafText.matches("^[0-9\\+\\-\\%\\s\\:\\(\\)\\[\\]\\.\\,]+$");
                if (nextIsNumeric) {
                    int matchPos = translatedText.indexOf(nextLeafText, transIdx);
                    if (matchPos != -1) {
                        nextTransIdx = matchPos;
                    }
                }
            }

            if (nextTransIdx == -1 || nextTransIdx < transIdx) {
                double ratio = (double) leafText.length() / origTotalLen;
                int length = (int) Math.round(ratio * translatedText.length());
                length = Math.max(1, length);
                nextTransIdx = Math.min(translatedText.length(), transIdx + length);

                // Snapping color transitions to word boundaries to avoid splitting words
                if (nextTransIdx > transIdx && nextTransIdx < translatedText.length()) {
                    if (Character.isLetterOrDigit(translatedText.charAt(nextTransIdx - 1)) &&
                        Character.isLetterOrDigit(translatedText.charAt(nextTransIdx))) {
                        
                        int leftBound = nextTransIdx;
                        while (leftBound > transIdx && Character.isLetterOrDigit(translatedText.charAt(leftBound - 1))) {
                            leftBound--;
                        }
                        int rightBound = nextTransIdx;
                        while (rightBound < translatedText.length() && Character.isLetterOrDigit(translatedText.charAt(rightBound))) {
                            rightBound++;
                        }

                        if (leftBound > transIdx && (nextTransIdx - leftBound <= rightBound - nextTransIdx || rightBound == translatedText.length())) {
                            nextTransIdx = leftBound;
                        } else if (rightBound < translatedText.length()) {
                            nextTransIdx = rightBound;
                        }
                    }
                }
            }

            String part = translatedText.substring(transIdx, nextTransIdx);
            rootComponent.append(Component.literal(part).withStyle(leafStyle));
            transIdx = nextTransIdx;
        }

        return rootComponent;
    }

    /**
     * Create a component with translation result for chat.
     */
    public Component createResultForChat() {
        var styledResult = getStyledResultLines();
        Style appliedStyle = styledResult.getLeft();
        String[] texts = styledResult.getRight();

        var component = Component.literal("").withStyle(appliedStyle);
        for (String text : texts) {
            component.append(text);
        }
        return component;
    }

    public enum TooltipLineType {
        NAME,
        ENCHANTMENT,
        STAT,
        LORE,
        ABILITY,
        OTHER
    }

    public TooltipLineType classifyTooltipLine(Component component, int index) {
        if (index == 0) {
            return TooltipLineType.NAME;
        }
        String text = component.getString().trim();
        String lowerText = text.toLowerCase();

        if (text.startsWith("+") || text.startsWith("-") || 
            lowerText.contains("damage") || lowerText.contains("speed") || 
            lowerText.contains("armor") || lowerText.contains("toughness") ||
            lowerText.contains("when in") || lowerText.contains("при использовании") ||
            lowerText.contains("урон") || lowerText.contains("скорость") || lowerText.contains("броня") ||
            text.matches("^(\\+|-)?\\d+(\\.\\d+)?%?\\s+\\w+.*")) {
            return TooltipLineType.STAT;
        }

        if (text.matches("^.*\\s+[IVXLCDM]+$") || 
            lowerText.startsWith("sharpness") || lowerText.startsWith("protection") ||
            lowerText.startsWith("efficiency") || lowerText.startsWith("unbreaking") ||
            lowerText.startsWith("острота") || lowerText.startsWith("защита") ||
            lowerText.startsWith("эффективность") || lowerText.startsWith("прочность")) {
            return TooltipLineType.ENCHANTMENT;
        }

        if (lowerText.contains("ability") || lowerText.contains("active") || 
            lowerText.contains("cooldown") || lowerText.contains("способность") ||
            lowerText.contains("активно") || lowerText.contains("перезарядка") ||
            text.contains("[") && text.contains("]")) {
            return TooltipLineType.ABILITY;
        }

        Style style = component.getStyle();
        if (style.isItalic() || (style.getColor() != null && 
            (style.getColor().equals(TextColor.fromLegacyFormat(ChatFormatting.DARK_PURPLE)) || 
             style.getColor().equals(TextColor.fromLegacyFormat(ChatFormatting.GRAY)) ||
             style.getColor().equals(TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY))))) {
            return TooltipLineType.LORE;
        }

        return TooltipLineType.OTHER;
    }

    public boolean shouldTranslateTooltipLine(TooltipLineType type, MTConfig config) {
        switch (type) {
            case NAME:
                return config.getAutoTranslateItemNames();
            case ENCHANTMENT:
                return config.getAutoTranslateItemTooltips() && config.getTranslateEnchantments();
            case STAT:
                return config.getAutoTranslateItemTooltips() && config.getTranslateStats();
            case LORE:
                return config.getAutoTranslateItemTooltips() && config.getTranslateLore();
            case ABILITY:
                return config.getAutoTranslateItemTooltips() && config.getTranslateAbilities();
            default:
                return config.getAutoTranslateItemTooltips();
        }
    }

    public void requestTooltipTranslation(String cleanText, String cacheKey, MTConfig config, String targetLang) {
        if (activeTooltipRequests.contains(cacheKey)) {
            return;
        }
        activeTooltipRequests.add(cacheKey);
        translationExecutor.submit(() -> {
            try {
                String translated = config.getService().provider.translate(cleanText, config.getSourceLanguage(), targetLang, new ArrayList<>());
                if (translated != null && !translated.isEmpty()) {
                    translationCache.put(cacheKey, translated);
                }
            } catch (Exception e) {
                // silent
            } finally {
                activeTooltipRequests.remove(cacheKey);
            }
        });
    }

    public static void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(translationExecutor::shutdownNow));

        ItemTooltipCallbacks.EVENT.register((stack, context, flag, lines) -> {
            TranslationKit.getInstance().setHoveredText(lines);

            MTConfig config = MTConfig.getInstance();
            String targetLang = config.getTargetLanguage();

            boolean autoNames = config.getAutoTranslateItemNames();
            boolean autoTooltips = config.getAutoTranslateItemTooltips();
            boolean preload = config.getPreloadTooltipTranslations();

            if (autoNames || autoTooltips || preload) {
                for (int i = 0; i < lines.size(); i++) {
                    Component line = lines.get(i);
                    String cleanText = line.getString().trim();
                    if (cleanText.isEmpty()) continue;

                    TranslationKit.TooltipLineType type = TranslationKit.getInstance().classifyTooltipLine(line, i);
                    boolean shouldTranslate = TranslationKit.getInstance().shouldTranslateTooltipLine(type, config);

                    if (shouldTranslate || preload) {
                        String cacheKey = cleanText + "|" + targetLang;
                        if (TranslationKit.getInstance().translationCache.containsKey(cacheKey)) {
                            if (shouldTranslate) {
                                String translated = TranslationKit.getInstance().translationCache.get(cacheKey);
                                Component replaced;
                                if (config.getPreserveItemFormatting()) {
                                    replaced = TranslationKit.applyStyleHierarchy(line, translated);
                                } else {
                                    replaced = Component.literal(translated).withStyle(line.getStyle());
                                }
                                lines.set(i, replaced);
                            }
                        } else {
                            TranslationKit.getInstance().requestTooltipTranslation(cleanText, cacheKey, config, targetLang);
                        }
                    }
                }
            }

            if (TranslationKit.getInstance().isTranslated() &&
                    TranslationKit.getInstance().getTranslatedResult() != null &&
                    TooltipUtl.getCombinedTooltipText(lines).equals(TranslationKit.getInstance().translatedText)) {
                TranslationKit.getInstance().replaceTooltipWithTranslation(lines);
            }
        });

        ScreenCallbacks.KEY_PRESSED_POST.register((screen, context) -> {
            if (MTKeyMappings.TRANSLATE_KEY.matches(context)) {
                TranslationKit.getInstance().start(Minecraft.getInstance());
                TranslationKit.getInstance().setKeyDown(true);
            }
        });

        ScreenCallbacks.KEY_RELEASED_POST.register(((screen, context) -> {
            if (MTKeyMappings.TRANSLATE_KEY.matches(context)) {
                TranslationKit.getInstance().stop();
                TranslationKit.getInstance().setKeyDown(false);
            }
        }));

        ScreenCallbacks.REMOVED.register(screen -> {
            TranslationKit.getInstance().stop();
            TranslationKit.getInstance().setKeyDown(false);
        });
    }

    public static String separator() {
        return MTConfig.getInstance().getService().provider.separator();
    }

    public static String literalSeparator() {
        return Pattern.quote(separator());
    }

    public static boolean isNpcMessage(Component message) {
        String clean = message.getString().trim();
        int npcIdx = clean.indexOf("[NPC]");
        return npcIdx >= 0 && npcIdx < 15;
    }

    private static final Set<String> IGNORED_SENDERS = Set.of(
        "system", "server", "error", "warning", "info", "broadcast", "auction", "auctionhouse", "ah", "ad", "news", "announcement"
    );

    public static int findSeparatorColonIndex(String text) {
        int squareBrackets = 0;
        int roundBrackets = 0;
        int curlyBrackets = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '[') squareBrackets++;
            else if (c == ']') { if (squareBrackets > 0) squareBrackets--; }
            else if (c == '(') roundBrackets++;
            else if (c == ')') { if (roundBrackets > 0) roundBrackets--; }
            else if (c == '{') curlyBrackets++;
            else if (c == '}') { if (curlyBrackets > 0) curlyBrackets--; }
            else if (c == ':') {
                if (squareBrackets == 0 && roundBrackets == 0 && curlyBrackets == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static String getSenderName(String prefixText) {
        String clean = prefixText.trim();
        if (clean.endsWith(":")) {
            clean = clean.substring(0, clean.length() - 1).trim();
        }
        // Remove color codes
        clean = clean.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
        
        // Split by whitespace
        String[] words = clean.split("\\s+");
        if (words.length == 0) return "";

        // Iterate backwards from the last word to find the actual sender name
        for (int i = words.length - 1; i >= 0; i--) {
            String word = words[i].trim();
            if (word.isEmpty()) continue;

            // Strip brackets for validation
            String stripped = word.replaceAll("[\\[\\]\\(\\)\\{\\}]", "").trim();

            // Skip control characters and channel prefixes
            if (stripped.equals(">") || 
                stripped.equalsIgnoreCase("guild") || 
                stripped.equalsIgnoreCase("party") || 
                stripped.equalsIgnoreCase("to") || 
                stripped.equalsIgnoreCase("from") || 
                stripped.equalsIgnoreCase("co-op") || 
                stripped.equalsIgnoreCase("officer")) {
                continue;
            }

            // Skip numeric levels in brackets, e.g., [297]
            if (word.startsWith("[") && word.endsWith("]") && stripped.matches("^\\d+$")) {
                continue;
            }

            // Skip ranks in brackets, e.g., [MVP+]
            if (word.startsWith("[") && word.endsWith("]") && 
                (stripped.contains("MVP") || stripped.contains("VIP") || 
                 stripped.equalsIgnoreCase("helper") || stripped.equalsIgnoreCase("admin") || 
                 stripped.equalsIgnoreCase("youtube") || stripped.equalsIgnoreCase("owner") ||
                 stripped.equalsIgnoreCase("mod") || stripped.equalsIgnoreCase("gm"))) {
                continue;
            }

            // Skip trailing guild tags like PlayerName [TAG]
            if (i == words.length - 1 && word.startsWith("[") && word.endsWith("]")) {
                if (i > 0) {
                    continue;
                }
            }

            if (isValidUsername(stripped)) {
                return stripped;
            }
        }
        return "";
    }

    public static boolean isValidUsername(String name) {
        if (name == null || name.length() < 3 || name.length() > 20) {
            return false;
        }
        if (IGNORED_SENDERS.contains(name.toLowerCase(Locale.ROOT))) {
            return false;
        }
        return name.matches("^[a-zA-Z0-9_\\*\\+\\-\\.\\!]+$");
    }

    public static SplitComponent splitPlayerMessage(Component original) {
        String fullString = original.getString();
        PlayerChatParseResult res = parsePlayerMessage(fullString);
        if (!res.valid) {
            return null;
        }
        return splitComponentAtIndex(original, res.bodyStartIndex - 1);
    }

    public static SplitComponent splitComponentAtIndex(Component original, int index) {
        List<Component> leaves = new ArrayList<>();
        original.visit((style, text) -> {
            if (!text.isEmpty()) {
                leaves.add(Component.literal(text).withStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);

        var prefixComponent = Component.empty().withStyle(original.getStyle());
        var bodyComponent = Component.empty().withStyle(original.getStyle());

        int currentOffset = 0;
        int boundary = index + 1;

        for (Component leaf : leaves) {
            String leafText = leaf.getString();
            Style leafStyle = leaf.getStyle();
            int L = leafText.length();

            if (currentOffset + L <= boundary) {
                prefixComponent.append(leaf);
            } else if (currentOffset >= boundary) {
                bodyComponent.append(leaf);
            } else {
                int prefixLen = boundary - currentOffset;
                String prefixPart = leafText.substring(0, prefixLen);
                String bodyPart = leafText.substring(prefixLen);

                if (!prefixPart.isEmpty()) {
                    prefixComponent.append(Component.literal(prefixPart).withStyle(leafStyle));
                }
                if (!bodyPart.isEmpty()) {
                    bodyComponent.append(Component.literal(bodyPart).withStyle(leafStyle));
                }
            }
            currentOffset += L;
        }

        return new SplitComponent(prefixComponent, bodyComponent);
    }

    public static boolean shouldSkipTranslation(String text, String targetLang) {
        if (text == null || text.isBlank()) {
            return true;
        }
        String clean = text.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
        clean = clean.replaceAll("https?://\\S+\\s?", "");
        
        int cyrillicLetters = 0;
        int latinLetters = 0;
        
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (Character.isLetter(c)) {
                if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC) {
                    cyrillicLetters++;
                } else if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z') {
                    latinLetters++;
                }
            }
        }
        
        // If there are no letters at all, skip
        if (cyrillicLetters == 0 && latinLetters == 0) {
            return true;
        }
        
        if ("ru".equalsIgnoreCase(targetLang) || "uk".equalsIgnoreCase(targetLang) || "kk".equalsIgnoreCase(targetLang)) {
            // Skip only if there are no Latin letters to translate
            if (latinLetters == 0) {
                return true;
            }
        } else if ("en".equalsIgnoreCase(targetLang)) {
            // Skip only if there is no Cyrillic to translate
            if (cyrillicLetters == 0) {
                return true;
            }
        }
        
        return false;
    }

    public static class SplitComponent {
        public final Component prefix;
        public final Component body;

        public SplitComponent(Component prefix, Component body) {
            this.prefix = prefix;
            this.body = body;
        }
    }

    public static SplitComponent splitAtFirstColon(Component original) {
        String fullString = original.getString();
        int colonIdx = fullString.indexOf(":");
        if (colonIdx == -1) {
            return new SplitComponent(Component.empty(), original);
        }

        List<Component> leaves = new ArrayList<>();
        original.visit((style, text) -> {
            if (!text.isEmpty()) {
                leaves.add(Component.literal(text).withStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);

        var prefixComponent = Component.empty().withStyle(original.getStyle());
        var bodyComponent = Component.empty().withStyle(original.getStyle());

        int currentOffset = 0;
        int boundary = colonIdx + 1;

        for (Component leaf : leaves) {
            String leafText = leaf.getString();
            Style leafStyle = leaf.getStyle();
            int L = leafText.length();

            if (currentOffset + L <= boundary) {
                prefixComponent.append(leaf);
            } else if (currentOffset >= boundary) {
                bodyComponent.append(leaf);
            } else {
                int prefixLen = boundary - currentOffset;
                String prefixPart = leafText.substring(0, prefixLen);
                String bodyPart = leafText.substring(prefixLen);

                if (!prefixPart.isEmpty()) {
                    prefixComponent.append(Component.literal(prefixPart).withStyle(leafStyle));
                }
                if (!bodyPart.isEmpty()) {
                    bodyComponent.append(Component.literal(bodyPart).withStyle(leafStyle));
                }
            }
            currentOffset += L;
        }

        return new SplitComponent(prefixComponent, bodyComponent);
    }

    private String getCacheKey(String text, String targetLang, List<String> contextList) {
        if (contextList == null || contextList.isEmpty()) {
            return text + "|" + targetLang;
        }
        String contextStr = String.join("\n", contextList);
        int contextHash = contextStr.hashCode();
        return text + "|" + targetLang + "|" + contextHash;
    }

    public List<String> getTranslationContext(ChatComponent chat, boolean npcOnly, int size) {
        List<String> context = new ArrayList<>();
        if (size <= 0) {
            return context;
        }
        List<GuiMessage> messages = chat.allMessages;
        for (int i = 1; i < messages.size(); i++) {
            if (context.size() >= size) {
                break;
            }
            GuiMessage msg = messages.get(i);
            Component content = msg.content();
            if (npcOnly) {
                if (isNpcMessage(content)) {
                    SplitComponent split = splitAtFirstColon(content);
                    context.add(0, split.body.getString().trim());
                }
            } else {
                SplitComponent split = splitAtFirstColon(content);
                context.add(0, split.body.getString().trim());
            }
        }
        return context;
    }

    public static class PlaceholderProtector {
        private final List<String> placeholders = new ArrayList<>();
        
        public String protect(String text) {
            if (text == null) return null;
            
            // 1. Protect URLs
            Pattern urlPattern = Pattern.compile("https?://\\S+");
            Matcher urlMatcher = urlPattern.matcher(text);
            StringBuilder sb = new StringBuilder();
            while (urlMatcher.find()) {
                String url = urlMatcher.group();
                placeholders.add(url);
                urlMatcher.appendReplacement(sb, " __PROTECTED_VAL_" + (placeholders.size() - 1) + "__ ");
            }
            urlMatcher.appendTail(sb);
            text = sb.toString();
            
            // 2. Protect alphanumeric values like 50m, 2b, x6, x6BNTT, 10k, 100lvl, etc.
            Pattern alphaNumPattern = Pattern.compile("\\b(\\d+[a-zA-Z]+|[a-zA-Z]+\\d+[a-zA-Z0-9]*)\\b");
            Matcher alphaNumMatcher = alphaNumPattern.matcher(text);
            sb = new StringBuilder();
            while (alphaNumMatcher.find()) {
                String val = alphaNumMatcher.group();
                placeholders.add(val);
                alphaNumMatcher.appendReplacement(sb, " __PROTECTED_VAL_" + (placeholders.size() - 1) + "__ ");
            }
            alphaNumMatcher.appendTail(sb);
            text = sb.toString();
            
            return text;
        }
        
        public String restore(String translated) {
            if (translated == null) return null;
            String result = translated;
            for (int i = placeholders.size() - 1; i >= 0; i--) {
                String placeholder = "__PROTECTED_VAL_" + i + "__";
                result = replaceIgnoreCaseAndSpacing(result, placeholder, placeholders.get(i));
            }
            return result;
        }

        private String replaceIgnoreCaseAndSpacing(String text, String placeholder, String replacement) {
            String cleanPlaceholder = placeholder.toLowerCase(Locale.ROOT);
            String lowerText = text.toLowerCase(Locale.ROOT);
            
            int idx = lowerText.indexOf(cleanPlaceholder);
            if (idx != -1) {
                return text.substring(0, idx) + replacement + text.substring(idx + placeholder.length());
            }
            
            String regex = "__\\s*protected_val_\\s*" + placeholder.replaceAll("[^0-9]", "") + "\\s*__";
            return text.replaceAll("(?i)" + regex, Matcher.quoteReplacement(replacement));
        }
    }

    public static class TranslationRequest {
        public final long messageId;
        public final GuiMessage guiMessage;
        public final Component originalContent;
        public final SplitComponent split;
        public final String targetLang;
        public final List<String> contextList;
        public final boolean isPlayer;
        public final boolean isNpc;
        
        public TranslationRequest(long messageId, GuiMessage guiMessage, Component originalContent, SplitComponent split, String targetLang, List<String> contextList, boolean isPlayer, boolean isNpc) {
            this.messageId = messageId;
            this.guiMessage = guiMessage;
            this.originalContent = originalContent;
            this.split = split;
            this.targetLang = targetLang;
            this.contextList = contextList;
            this.isPlayer = isPlayer;
            this.isNpc = isNpc;
        }
    }

    public CompletableFuture<Void> translateChatMessage(
        TranslationRequest request,
        java.util.function.Consumer<Component> onComplete
    ) {
        String origBodyText = request.split.body.getString();
        String textToTranslate = origBodyText.trim();
        if (textToTranslate.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        // In force debug translation, skip filter is bypassed!
        if (!debugForceTranslateAll && shouldSkipTranslation(textToTranslate, request.targetLang)) {
            MineTranslator.LOGGER.info("[MineTranslator][CHAT TRACE] ID: {} Skipped translation by skip filters.", request.messageId);
            return CompletableFuture.completedFuture(null);
        }

        String cacheKey = getCacheKey(textToTranslate, request.targetLang, request.contextList);

        // In force debug translation, cache is bypassed!
        if (!debugForceTranslateAll && this.translationCache.containsKey(cacheKey)) {
            String translated = this.translationCache.get(cacheKey);
            Component finalComp = assembleTranslatedMessage(request.split, origBodyText, translated, request.isNpc, request.isPlayer, MTConfig.getInstance());
            onComplete.accept(finalComp);
            MineTranslator.LOGGER.info("[MineTranslator][CHAT TRACE] ID: {} Cache Hit. Translation applied.", request.messageId);
            return CompletableFuture.completedFuture(null);
        }

        PlaceholderProtector protector = new PlaceholderProtector();
        String protectedText = debugForceTranslateAll ? textToTranslate : protector.protect(textToTranslate);

        TranslationState state = getTranslationStateById(request.messageId);
        if (state != null) {
            state.isTranslating = true;
        }

        MineTranslator.LOGGER.info("[MineTranslator][CHAT TRACE] ID: {} Provider request started.", request.messageId);

        return CompletableFuture.runAsync(() -> {
            try {
                MTConfig config = MTConfig.getInstance();
                String sourceLang = config.getSourceLanguage();

                String type = request.isPlayer ? "PLAYER_CHAT" : "GENERAL";
                
                // In force debug translation, context size is forced to 0
                List<String> activeContext = debugForceTranslateAll ? Collections.emptyList() : request.contextList;

                String translatedRaw = config.getService().provider.translate(
                    protectedText,
                    sourceLang,
                    request.targetLang,
                    activeContext,
                    type
                );

                if (translatedRaw != null && !translatedRaw.isEmpty()) {
                    String translated = debugForceTranslateAll ? translatedRaw : protector.restore(translatedRaw);
                    MineTranslator.LOGGER.info("[MineTranslator][CHAT TRACE] ID: {} Provider result received: {}", request.messageId, translated);
                    
                    if (!debugForceTranslateAll) {
                        this.translationCache.put(cacheKey, translated);
                    }

                    if (state != null) {
                        state.translatedContent = Component.literal(translated);
                        state.isTranslating = false;
                    }

                    Component finalComp = assembleTranslatedMessage(request.split, origBodyText, translated, request.isNpc, request.isPlayer, config);
                    Minecraft.getInstance().execute(() -> {
                        onComplete.accept(finalComp);
                        MineTranslator.LOGGER.info("[MineTranslator][CHAT TRACE] ID: {} Applied on client thread.", request.messageId);
                    });
                } else {
                    MineTranslator.LOGGER.warn("[MineTranslator][CHAT TRACE] ID: {} Provider result is empty", request.messageId);
                }
            } catch (Exception e) {
                MineTranslator.LOGGER.error("[MineTranslator][CHAT TRACE] ID: {} Translation failed. Error: {}", request.messageId, e.toString());
            }
        }, translationExecutor);
    }

    public void onNewChatMessageAdded(GuiMessage guiMessage, ChatComponent chat) {
        MTConfig config = MTConfig.getInstance();
        boolean autoChat = config.getAutoTranslateChat();
        boolean autoNpc = config.getAutoTranslateNpc();
        boolean autoPlayer = config.getTranslatePlayerMessages();
        boolean preload = config.getPreloadChatTranslations();

        Component content = guiMessage.content();
        
        // 1. Register or retrieve stable state for this incoming message
        TranslationState state = registerTranslationState(guiMessage);
        long messageId = state.id;
        
        // Prevent recursive triggers
        if (state.translatedContent != null || state.isTranslating) {
            return;
        }

        boolean isNpc = isNpcMessage(content);
        SplitComponent split = null;
        boolean isPlayer = false;
        boolean isSystem = false;

        if (isNpc) {
            split = splitAtFirstColon(content);
        } else {
            split = splitPlayerMessage(content);
            if (split != null) {
                isPlayer = true;
            } else {
                split = splitAtFirstColon(content);
                isSystem = true;
            }
        }

        String cleanText = content.getString().replaceAll("§[0-9a-fk-orA-FK-OR]", "");
        MineTranslator.LOGGER.info("[MineTranslator][CHAT TRACE]");
        MineTranslator.LOGGER.info("ID: {}", messageId);
        MineTranslator.LOGGER.info("ChatHud addMessage intercepted: true");
        MineTranslator.LOGGER.info("Original plain text: {}", cleanText);
        MineTranslator.LOGGER.info("Force translate mode: {}", debugForceTranslateAll);
        MineTranslator.LOGGER.info("Player detected: {}", isPlayer);
        String senderName = isPlayer ? getSenderName(split.prefix.getString()) : "N/A";
        MineTranslator.LOGGER.info("Detected username: {}", senderName);
        MineTranslator.LOGGER.info("Extracted content: {}", split != null ? split.body.getString().trim() : "N/A");

        boolean shouldTranslateForDisplay = debugForceTranslateAll || (isNpc && autoNpc) || (isPlayer && autoPlayer) || (isSystem && autoChat);
        boolean shouldTranslateForPreload = preload;

        if (!shouldTranslateForDisplay && !shouldTranslateForPreload) {
            MineTranslator.LOGGER.info("[MineTranslator][CHAT TRACE] ID: {} Skipped: Ignored by configuration settings.", messageId);
            return;
        }

        String origBodyText = split.body.getString();
        String textToTranslate = origBodyText.trim();
        if (textToTranslate.isEmpty()) {
            MineTranslator.LOGGER.info("[MineTranslator][CHAT TRACE] ID: {} Skipped: Body content is empty.", messageId);
            return;
        }

        String targetLang = config.getTargetLanguage();
        boolean npcContextOnly = config.getUseNpcContextOnly();
        int contextSize = config.getTranslationContextSize();
        List<String> contextList = debugForceTranslateAll ? Collections.emptyList() : getTranslationContext(chat, npcContextOnly, contextSize);

        TranslationRequest request = new TranslationRequest(
            messageId,
            guiMessage,
            content,
            split,
            targetLang,
            contextList,
            isPlayer,
            isNpc
        );

        MineTranslator.LOGGER.info("[MineTranslator][CHAT TRACE] ID: {} Translation request created.", messageId);

        translateChatMessage(request, finalComp -> {
            if (shouldTranslateForDisplay) {
                updateMessageInChat(chat, guiMessage, finalComp);
            }
        });
    }

    private Component assembleTranslatedMessage(SplitComponent split, String origBodyText, String translatedText, boolean isNpc, boolean isPlayer, MTConfig config) {
        String leadingSpaces = "";
        int i = 0;
        while (i < origBodyText.length() && Character.isWhitespace(origBodyText.charAt(i))) {
            leadingSpaces += origBodyText.charAt(i);
            i++;
        }
        String trailingSpaces = "";
        int j = origBodyText.length() - 1;
        while (j >= i && Character.isWhitespace(origBodyText.charAt(j))) {
            trailingSpaces = origBodyText.charAt(j) + trailingSpaces;
            j--;
        }

        Component translatedBody;
        if (config.getPreserveFormatting()) {
            translatedBody = applyStyleHierarchy(split.body, leadingSpaces + translatedText + trailingSpaces);
        } else {
            translatedBody = Component.literal(leadingSpaces + translatedText + trailingSpaces).withStyle(split.body.getStyle());
        }

        boolean showOriginal = false;
        if (isNpc) {
            showOriginal = config.getShowOriginalWithTranslation();
        } else if (isPlayer) {
            showOriginal = config.getShowPlayerOriginalWithTranslation();
        } else {
            showOriginal = config.getShowOriginalWithTranslation();
        }

        Component finalBody;
        if (showOriginal) {
            Component origPart = Component.literal(" (" + origBodyText.trim() + ")").withStyle(ChatFormatting.GRAY);
            finalBody = Component.empty().append(translatedBody).append(origPart);
        } else {
            finalBody = translatedBody;
        }

        return Component.empty().append(split.prefix).append(finalBody);
    }

    private void updateMessageInChat(ChatComponent chat, GuiMessage original, Component newContent) {
        TranslationState state = getTranslationState(original);
        GuiMessage target = (state != null) ? state.current : original;
        
        int index = chat.allMessages.indexOf(target);
        if (index >= 0) {
            GuiMessage updatedMessage = new GuiMessage(
                original.addedTime(),
                newContent,
                original.signature(),
                original.tag()
            );
            chat.allMessages.set(index, updatedMessage);
            if (state != null) {
                state.current = updatedMessage;
            }
            ((ChatComponentMixinAccessor) chat).MineTranslator$refreshTrimmedMessages();
            MineTranslator.LOGGER.info("[MineTranslator] Replacement applied successfully to message");
        } else {
            MineTranslator.LOGGER.warn("[MineTranslator] Failed to apply replacement: message not found in chat view list");
        }
    }

    public boolean onSendChatMessage(String message, ChatScreen screen) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        if (message.startsWith("/tppsend ")) {
            String payload = message.substring("/tppsend ".length());
            ((ChatScreenMixinAccessor) screen).MineTranslator$sendChatDirectly(payload, true);
            return true;
        }
        if (message.startsWith("/tppedit ")) {
            String payload = message.substring("/tppedit ".length());
            ((ChatScreenMixinAccessor) screen).MineTranslator$getInput().setValue(payload);
            return true;
        }
        if (message.startsWith("/tppcancel")) {
            ClientUtl.message(Minecraft.getInstance(), Component.literal("§cOutgoing translation cancelled."));
            return true;
        }

        if (message.startsWith("/")) {
            return false;
        }

        MTConfig config = MTConfig.getInstance();
        if (!config.getTranslateMyMessagesBeforeSending()) {
            return false;
        }

        String targetLang = config.getMyMessageTargetLanguage();
        if (targetLang == null || targetLang.isEmpty() || targetLang.equalsIgnoreCase("auto")) {
            targetLang = config.getTargetLanguage();
        }

        String finalTargetLang = targetLang;
        final List<String> contextList = config.getUseContextForMyMessages()
            ? getTranslationContext(Minecraft.getInstance().gui.getChat(), config.getPlayerContextOnly(), config.getPlayerContextSize())
            : new java.util.ArrayList<>();

        if (config.getShowTranslationPreviewBeforeSending()) {
            ClientUtl.message(Minecraft.getInstance(), Component.literal("§b[TPP] Translating outgoing message...").withStyle(ChatFormatting.ITALIC));
        }

        translationExecutor.submit(() -> {
            try {
                String translated = config.getService().provider.translate(message, "auto", finalTargetLang, contextList);
                if (translated != null && !translated.isEmpty()) {
                    Minecraft.getInstance().execute(() -> {
                        if (config.getShowTranslationPreviewBeforeSending()) {
                            showOutboundPreview(message, translated);
                        } else {
                            ((ChatScreenMixinAccessor) screen).MineTranslator$sendChatDirectly(translated, true);
                        }
                    });
                } else {
                    handleOutboundFailure(message, screen, new RuntimeException("Empty result"));
                }
            } catch (Exception e) {
                handleOutboundFailure(message, screen, e);
            }
        });

        return true;
    }

    private void handleOutboundFailure(String original, ChatScreen screen, Exception e) {
        Minecraft.getInstance().execute(() -> {
            MineTranslator.LOGGER.error("Outbound translation failed. Error: {}", e.toString());
            MTConfig config = MTConfig.getInstance();
            if (config.getSendOriginalIfTranslationFails()) {
                ClientUtl.message(Minecraft.getInstance(), Component.literal("§cOutbound translation failed, sending original message."));
                ((ChatScreenMixinAccessor) screen).MineTranslator$sendChatDirectly(original, true);
            } else {
                ClientUtl.message(Minecraft.getInstance(), Component.literal("§cOutbound translation failed. Message not sent. Click [Edit] to restore."));
                showOutboundPreview(original, null);
            }
        });
    }

    private void showOutboundPreview(String original, @Nullable String translated) {
        Minecraft client = Minecraft.getInstance();
        ClientUtl.message(client, Component.literal("§8========================================"));
        ClientUtl.message(client, Component.literal("§bTranslator++ Outbound Preview:"));
        ClientUtl.message(client, Component.literal("§7Original: §f" + original));
        if (translated != null) {
            ClientUtl.message(client, Component.literal("§7Translation: §a" + translated));
        } else {
            ClientUtl.message(client, Component.literal("§7Translation: §c[Failed]"));
        }

        var actions = Component.empty();
        if (translated != null) {
            Component sendBtn = Component.literal("[ Send ]")
                .withStyle(style -> style
                    .withColor(ChatFormatting.GOLD)
                    .withBold(true)
                    .withClickEvent(new ClickEvent.RunCommand("/tppsend " + translated))
                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to send translated message"))));
            actions.append(sendBtn).append("   ");
        }

        Component editBtn = Component.literal("[ Edit ]")
            .withStyle(style -> style
                .withColor(ChatFormatting.YELLOW)
                .withBold(true)
                .withClickEvent(new ClickEvent.RunCommand("/tppedit " + original))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to edit original message"))));

        Component cancelBtn = Component.literal("[ Cancel ]")
            .withStyle(style -> style
                .withColor(ChatFormatting.RED)
                .withBold(true)
                .withClickEvent(new ClickEvent.RunCommand("/tppcancel"))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to cancel sending"))));

        actions.append(editBtn).append("   ").append(cancelBtn);
        ClientUtl.message(client, actions);
        ClientUtl.message(client, Component.literal("§8========================================"));
    }

    public void translateInputWithContext(EditBox input) {
        String text = input.getValue().trim();
        if (text.isEmpty() || text.startsWith("/")) {
            return;
        }

        MTConfig config = MTConfig.getInstance();
        String targetLang = config.getMyMessageTargetLanguage();
        if (targetLang == null || targetLang.isEmpty() || targetLang.equalsIgnoreCase("auto")) {
            targetLang = config.getTargetLanguage();
        }

        String finalTargetLang = targetLang;
        List<String> contextList = getTranslationContext(Minecraft.getInstance().gui.getChat(), config.getPlayerContextOnly(), 50);

        ClientUtl.message(Minecraft.getInstance(), Component.literal("§b[TPP] Translating input with context...").withStyle(ChatFormatting.ITALIC));

        translationExecutor.submit(() -> {
            try {
                String translated = config.getService().provider.translate(text, "auto", finalTargetLang, contextList);
                if (translated != null && !translated.isEmpty()) {
                    Minecraft.getInstance().execute(() -> {
                        if (config.getReplaceInputWithTranslation()) {
                            input.setValue(translated);
                        }
                        if (config.getAutoSendAfterTranslation()) {
                            Minecraft client = Minecraft.getInstance();
                            if (client.screen instanceof ChatScreen chatScreen) {
                                ((ChatScreenMixinAccessor) chatScreen).MineTranslator$sendChatDirectly(translated, true);
                            }
                        }
                        if (!config.getReplaceInputWithTranslation() && !config.getAutoSendAfterTranslation()) {
                            ClientUtl.message(Minecraft.getInstance(), Component.literal("§b[TPP] Input translation: §f" + translated));
                        }
                    });
                }
            } catch (Exception e) {
                Minecraft.getInstance().execute(() -> {
                    ClientUtl.message(Minecraft.getInstance(), Component.literal("§c[TPP] Input translation failed: " + e.toString()));
                });
            }
        });
    }
}