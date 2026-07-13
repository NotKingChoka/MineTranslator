package net.kingchoka.minetranslator.tooltip;

import net.kingchoka.minetranslator.cache.TranslationCache;
import net.kingchoka.minetranslator.config.ModConfig;
import net.kingchoka.minetranslator.debug.TranslationDebugLogger;
import net.kingchoka.minetranslator.translation.TranslationMode;
import net.kingchoka.minetranslator.translation.TranslationRequest;
import net.kingchoka.minetranslator.translation.TranslationService;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TooltipTranslationController {
    private static final TooltipTranslationController INSTANCE = new TooltipTranslationController();

    public static TooltipTranslationController getInstance() {
        return INSTANCE;
    }

    private final Set<String> pendingRequests = ConcurrentHashMap.newKeySet();
    private volatile boolean manualTranslationRequested;
    private volatile String lastTooltipSignature;
    private volatile String manuallyTranslatedTooltip;
    private volatile String manuallyHiddenTooltip;

    private TooltipTranslationController() {}

    public void onGetTooltip(ItemStack stack, List<Component> lines) {
        ModConfig config = ModConfig.getInstance();
        if (lines == null || lines.isEmpty()) return;

        String tooltipSignature = createTooltipSignature(stack, lines);
        lastTooltipSignature = tooltipSignature;

        if (manualTranslationRequested) {
            manuallyTranslatedTooltip = tooltipSignature;
            manuallyHiddenTooltip = null;
            manualTranslationRequested = false;
            TranslationDebugLogger.tooltip("Manual tooltip translation selected: {}", stack.getHoverName().getString());
        }

        // A second press explicitly restores the original tooltip, including
        // when automatic item translation is enabled in the config.
        if (tooltipSignature.equals(manuallyHiddenTooltip)) {
            return;
        }

        boolean forceTranslate = tooltipSignature.equals(manuallyTranslatedTooltip);
        boolean translateNames = config.autoTranslateItemNames || forceTranslate;
        boolean translateTooltips = config.autoTranslateItemTooltips || forceTranslate;

        if (!translateNames && !translateTooltips) return;

        for (int i = 0; i < lines.size(); i++) {
            boolean isName = (i == 0);
            if (isName && !translateNames) continue;
            if (!isName && !translateTooltips) continue;

            Component originalComp = lines.get(i);
            String plainText = originalComp.getString();
            if (plainText.isBlank() || isOnlySpecialChars(plainText)) continue;

            TranslationMode mode = isName ? TranslationMode.ITEM_NAME : TranslationMode.ITEM_TOOLTIP;
            Component translated = translatePreservingStyles(originalComp, mode, config);
            if (translated != null) {
                lines.set(i, translated);
            }
        }
    }

    /**
     * Tooltip mods commonly build one visual line from several differently
     * coloured components. Translating the flattened string loses those child
     * styles, so translate each textual run and assemble the line again.
     */
    private Component translatePreservingStyles(Component original, TranslationMode mode, ModConfig config) {
        List<Component> segments = original.toFlatList();
        if (segments.isEmpty()) {
            segments = List.of(original);
        }

        MutableComponent translatedLine = Component.empty();
        boolean waitingForTranslation = false;

        for (Component segment : segments) {
            String text = segment.getString();

            // Numbers, spaces and separators must stay byte-for-byte intact.
            if (!containsLetter(text)) {
                translatedLine.append(Component.literal(text).withStyle(segment.getStyle()));
                continue;
            }

            String cached = TranslationCache.getInstance().get(
                config.provider,
                mode,
                text,
                config.sourceLanguage,
                config.targetLanguage,
                0
            );

            if (cached == null) {
                triggerAsyncTranslation(text, mode, config);
                waitingForTranslation = true;
            } else {
                translatedLine.append(Component.literal(cached).withStyle(segment.getStyle()));
            }
        }

        // Avoid a tooltip that changes colour/text one fragment at a time.
        return waitingForTranslation ? null : translatedLine;
    }

    private void triggerAsyncTranslation(String text, TranslationMode mode, ModConfig config) {
        String cacheKeyStr = config.provider + "|" + mode.name() + "|" + text;
        if (pendingRequests.add(cacheKeyStr)) {
            TranslationRequest request = new TranslationRequest(
                text,
                config.sourceLanguage,
                config.targetLanguage,
                mode,
                null,
                config.provider
            );

            TranslationService.getInstance().translate(request).thenAccept(result -> {
                pendingRequests.remove(cacheKeyStr);
                if (!result.success()) {
                    TranslationDebugLogger.tooltip("Tooltip request failed: {}", result.errorMessage());
                }
            });
        }
    }

    private boolean isOnlySpecialChars(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                return false;
            }
        }
        return true;
    }

    private boolean containsLetter(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetter(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Selects the tooltip currently under the cursor for manual translation.
     * The selection is kept after the key is released so an asynchronous
     * translation can be displayed when it arrives.
     */
    public void requestManualTranslation() {
        if (lastTooltipSignature != null) {
            if (lastTooltipSignature.equals(manuallyTranslatedTooltip)) {
                manuallyTranslatedTooltip = null;
                manuallyHiddenTooltip = lastTooltipSignature;
                manualTranslationRequested = false;
                return;
            }

            manuallyTranslatedTooltip = lastTooltipSignature;
            manuallyHiddenTooltip = null;
            manualTranslationRequested = false;
            return;
        }
        manualTranslationRequested = true;
    }

    public void clearManualTranslation() {
        manualTranslationRequested = false;
        lastTooltipSignature = null;
        manuallyTranslatedTooltip = null;
        manuallyHiddenTooltip = null;
    }

    private String createTooltipSignature(ItemStack stack, List<Component> lines) {
        String text = lines.stream()
            .map(Component::getString)
            .collect(Collectors.joining("\u001f"));
        return stack.getItem().toString() + "|" + text;
    }
}
