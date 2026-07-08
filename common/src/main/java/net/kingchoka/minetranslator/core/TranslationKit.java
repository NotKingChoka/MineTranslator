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

    static final TranslationKit INSTANCE = new TranslationKit();
    static final Gson GSON = new GsonBuilder().create();

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

    private TranslationKit() {
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

        // Check cache first
        String cachedResult = translationCache.get(translatedText);
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
                                MTConfig.getInstance().getTargetLanguage(),
                                new java.util.ArrayList<>()
                        );
                    } catch (Exception e) {
                        throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
                    }
                }, translationExecutor)
                .thenAcceptAsync(it -> {
                    // Update the result and cache it
                    translatedResult = it + SUCCESS;
                    translationCache.put(translatedText, it); // Add to cache
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
        
        // Find last word
        int lastSpace = clean.lastIndexOf(' ');
        if (lastSpace != -1) {
            clean = clean.substring(lastSpace + 1);
        }
        // Remove remaining brackets if any
        clean = clean.replaceAll("[\\[\\]\\(\\)\\{\\}]", "").trim();
        return clean;
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
        int colonIdx = findSeparatorColonIndex(fullString);
        if (colonIdx == -1) {
            return null;
        }
        String prefixText = fullString.substring(0, colonIdx + 1);
        String sender = getSenderName(prefixText);
        if (!isValidUsername(sender)) {
            return null;
        }
        return splitComponentAtIndex(original, colonIdx);
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
        int totalLetters = 0;
        
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (Character.isLetter(c)) {
                totalLetters++;
                if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC) {
                    cyrillicLetters++;
                } else if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z') {
                    latinLetters++;
                }
            }
        }
        
        if (totalLetters == 0) {
            return true;
        }
        
        if ("ru".equalsIgnoreCase(targetLang) || "uk".equalsIgnoreCase(targetLang) || "kk".equalsIgnoreCase(targetLang)) {
            if (cyrillicLetters > 0 && ((double) cyrillicLetters / totalLetters) > 0.6) {
                return true;
            }
        }
        
        if ("en".equalsIgnoreCase(targetLang)) {
            if (latinLetters > 0 && ((double) latinLetters / totalLetters) > 0.6) {
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

    public void onNewChatMessageAdded(GuiMessage guiMessage, ChatComponent chat) {
        MTConfig config = MTConfig.getInstance();
        boolean autoChat = config.getAutoTranslateChat();
        boolean autoNpc = config.getAutoTranslateNpc();
        boolean autoPlayer = config.getTranslatePlayerMessages();
        boolean preload = config.getPreloadChatTranslations();

        Component content = guiMessage.content();
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

        boolean shouldTranslateForDisplay = (isNpc && autoNpc) || (isPlayer && autoPlayer) || (isSystem && autoChat);
        boolean shouldTranslateForPreload = preload;

        if (!shouldTranslateForDisplay && !shouldTranslateForPreload) {
            return;
        }

        String origBodyText = split.body.getString();
        String textToTranslate = origBodyText.trim();
        if (textToTranslate.isEmpty()) {
            return;
        }

        String targetLang = config.getTargetLanguage();
        if (shouldSkipTranslation(textToTranslate, targetLang)) {
            return;
        }

        boolean npcContextOnly = config.getUseNpcContextOnly();
        int contextSize = config.getTranslationContextSize();
        List<String> contextList = getTranslationContext(chat, npcContextOnly, contextSize);

        String cacheKey = getCacheKey(textToTranslate, targetLang, contextList);

        if (this.translationCache.containsKey(cacheKey)) {
            if (shouldTranslateForDisplay) {
                String translated = this.translationCache.get(cacheKey);
                Component finalComp = assembleTranslatedMessage(split, origBodyText, translated, isNpc, isPlayer, config);
                updateMessageInChat(chat, guiMessage, finalComp);
            }
            return;
        }

        final SplitComponent finalSplit = split;
        final boolean finalIsNpc = isNpc;
        final boolean finalIsPlayer = isPlayer;

        translationExecutor.submit(() -> {
            try {
                if (finalIsPlayer) {
                    MineTranslator.LOGGER.info("[Translator++] Player chat detected");
                    MineTranslator.LOGGER.info("Prefix: {}", finalSplit.prefix.getString());
                    String senderName = getSenderName(finalSplit.prefix.getString());
                    MineTranslator.LOGGER.info("Username: {}", senderName);
                    MineTranslator.LOGGER.info("Original content: {}", textToTranslate);
                    MineTranslator.LOGGER.info("Provider: {}", config.getService().name());
                    MineTranslator.LOGGER.info("Target language: {}", targetLang);
                }

                String sourceLang = config.getSourceLanguage();
                String type = finalIsPlayer ? "PLAYER_CHAT" : "GENERAL";
                String translated = config.getService().provider.translate(textToTranslate, sourceLang, targetLang, contextList, type);
                
                if (translated != null && !translated.isEmpty()) {
                    if (finalIsPlayer) {
                        MineTranslator.LOGGER.info("Translated content: {}", translated);
                    }
                    this.translationCache.put(cacheKey, translated);
                    if (shouldTranslateForDisplay) {
                        Component finalComp = assembleTranslatedMessage(finalSplit, origBodyText, translated, finalIsNpc, finalIsPlayer, config);
                        Minecraft.getInstance().execute(() -> {
                            updateMessageInChat(chat, guiMessage, finalComp);
                        });
                    }
                }
            } catch (Exception e) {
                MineTranslator.LOGGER.error("Auto-translation/Preload failed for message: {}. Error: {}", textToTranslate, e.toString());
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
        int index = chat.allMessages.indexOf(original);
        if (index >= 0) {
            GuiMessage updatedMessage = new GuiMessage(
                original.addedTime(),
                newContent,
                original.signature(),
                original.tag()
            );
            chat.allMessages.set(index, updatedMessage);
            ((ChatComponentMixinAccessor) chat).MineTranslator$refreshTrimmedMessages();
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