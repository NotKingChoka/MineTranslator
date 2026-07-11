package net.kingchoka.minetranslator.tooltip;

import net.kingchoka.minetranslator.cache.TranslationCache;
import net.kingchoka.minetranslator.config.ModConfig;
import net.kingchoka.minetranslator.debug.TranslationDebugLogger;
import net.kingchoka.minetranslator.keybind.MTKeyMappings;
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

        boolean forceTranslate = isMappingDown(MTKeyMappings.TRANSLATE_ITEM_KEY) || isMappingDown(MTKeyMappings.TRANSLATE_KEY);
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

    private static boolean isMappingDown(net.minecraft.client.KeyMapping mapping) {
        if (mapping == null) return false;
        try {
            net.kingchoka.minetranslator.mixin.KeyMappingAccessor accessor = (net.kingchoka.minetranslator.mixin.KeyMappingAccessor) mapping;
            com.mojang.blaze3d.platform.InputConstants.Key key = accessor.MineTranslator$getKey();
            if (key != null && key.getValue() != com.mojang.blaze3d.platform.InputConstants.UNKNOWN.getValue()) {
                long window = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
                if (window != 0) {
                    if (key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM) {
                        return org.lwjgl.glfw.GLFW.glfwGetKey(window, key.getValue()) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
                    } else if (key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE) {
                        return org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, key.getValue()) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
                    }
                }
            }
        } catch (Exception e) {
            // Fallback
        }
        return mapping.isDown();
    }
}
