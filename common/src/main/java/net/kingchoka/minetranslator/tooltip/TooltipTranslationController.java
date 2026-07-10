package net.kingchoka.minetranslator.tooltip;

import net.kingchoka.minetranslator.cache.TranslationCache;
import net.kingchoka.minetranslator.config.ModConfig;
import net.kingchoka.minetranslator.debug.TranslationDebugLogger;
import net.kingchoka.minetranslator.translation.TranslationMode;
import net.kingchoka.minetranslator.translation.TranslationRequest;
import net.kingchoka.minetranslator.translation.TranslationService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TooltipTranslationController {
    private static final TooltipTranslationController INSTANCE = new TooltipTranslationController();

    public static TooltipTranslationController getInstance() {
        return INSTANCE;
    }

    private final Set<String> pendingRequests = ConcurrentHashMap.newKeySet();

    private TooltipTranslationController() {}

    public void onGetTooltip(ItemStack stack, List<Component> lines) {
        ModConfig config = ModConfig.getInstance();
        if (lines == null || lines.isEmpty()) return;

        boolean translateNames = config.autoTranslateItemNames;
        boolean translateTooltips = config.autoTranslateItemTooltips;

        if (!translateNames && !translateTooltips) return;

        for (int i = 0; i < lines.size(); i++) {
            boolean isName = (i == 0);
            if (isName && !translateNames) continue;
            if (!isName && !translateTooltips) continue;

            Component originalComp = lines.get(i);
            String plainText = originalComp.getString();
            if (plainText.isBlank() || isOnlySpecialChars(plainText)) continue;

            TranslationMode mode = isName ? TranslationMode.ITEM_NAME : TranslationMode.ITEM_TOOLTIP;

            String cached = TranslationCache.getInstance().get(
                config.provider,
                mode,
                plainText,
                config.sourceLanguage,
                config.targetLanguage,
                0
            );

            if (cached != null) {
                lines.set(i, Component.literal(cached).withStyle(originalComp.getStyle()));
            } else {
                triggerAsyncTranslation(plainText, mode, config);
            }
        }
    }

    private void triggerAsyncTranslation(String text, TranslationMode mode, ModConfig config) {
        String cacheKeyStr = config.provider + "|" + mode.name() + "|" + text;
        if (pendingRequests.add(cacheKeyStr)) {
            TranslationDebugLogger.tooltip("Tooltip request started for: {}", text);

            TranslationRequest request = new TranslationRequest(
                text,
                config.sourceLanguage,
                config.targetLanguage,
                mode,
                Collections.emptyList(),
                config.provider
            );

            TranslationService.getInstance().translate(request).thenAccept(result -> {
                pendingRequests.remove(cacheKeyStr);
                if (result.success()) {
                    TranslationDebugLogger.tooltip("Tooltip result stored: {}", result.translatedText());
                } else {
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
}
