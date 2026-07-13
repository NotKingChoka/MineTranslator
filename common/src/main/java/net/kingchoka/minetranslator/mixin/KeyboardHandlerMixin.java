package net.kingchoka.minetranslator.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GuiMessage;
import com.mojang.blaze3d.platform.InputConstants;
import net.kingchoka.minetranslator.keybind.MTKeyMappings;
import net.kingchoka.minetranslator.tooltip.TooltipTranslationController;
import net.kingchoka.minetranslator.chat.ChatTranslationController;
import net.kingchoka.minetranslator.chat.ChatInputTranslationController;
import net.kingchoka.minetranslator.api.ChatComponentMixinAccessor;
import net.kingchoka.minetranslator.debug.TranslationDebugLogger;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "method_22676", at = @At("HEAD"), cancellable = true, remap = false)
    private void onKey(long windowPointer, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (isTranslateKey(key, scancode)) {
            if (action == 1) { // GLFW_PRESS
                Minecraft client = Minecraft.getInstance();
                boolean handled = false;

                // 1. A visible chat line under the cursor has the highest priority.
                if (client.gui != null && client.gui.getChat() != null) {
                    double mouseX = client.mouseHandler.xpos();
                    double mouseY = client.mouseHandler.ypos();
                    try {
                        GuiMessage msg = ((ChatComponentMixinAccessor) client.gui.getChat()).MineTranslator$getMessageAt(mouseX, mouseY);
                        if (msg != null) {
                            ChatTranslationController.getInstance().translateMessage(msg, client.gui.getChat(), true);
                            handled = true;
                        } else if (client.screen instanceof net.minecraft.client.gui.screens.ChatScreen) {
                            TranslationDebugLogger.chat("No chat line under cursor at X={}, Y={}", mouseX, mouseY);
                        }
                    } catch (Exception e) {
                        TranslationDebugLogger.warn("Failed to select chat line: {}", e.toString());
                    }
                }

                // 2. With no hovered line, translate the current chat input.
                if (!handled && client.screen instanceof net.minecraft.client.gui.screens.ChatScreen chatScreen) {
                    handled = ChatInputTranslationController.getInstance().translateOrRestore(chatScreen);
                }

                // 3. Otherwise the same key toggles the currently hovered item tooltip.
                if (!handled) TooltipTranslationController.getInstance().requestManualTranslation();

                // Never type the grave accent/Ё used as the translation shortcut.
                ci.cancel();
            }
        }
    }

    private boolean isTranslateKey(int key, int scancode) {
        var mapping = MTKeyMappings.TRANSLATE_KEY;
        if (mapping == null) return false;

        var keyObj = mapping.key;
        if (keyObj == null) return false;

        int targetKey = keyObj.getValue();
        // Check by key code
        if (key != -1 && key == targetKey) {
            return true;
        }

        // Default grave accent key scancode check (41 is backtick/tilde/Ё on most Windows keyboards)
        if (targetKey == GLFW.GLFW_KEY_GRAVE_ACCENT && scancode == 41) {
            return true;
        }

        // Try to resolve scancode of target key
        try {
            int targetScancode = -1;
            if (keyObj.getType() == InputConstants.Type.KEYSYM) {
                targetScancode = GLFW.glfwGetKeyScancode(keyObj.getValue());
            } else if (keyObj.getType() == InputConstants.Type.SCANCODE) {
                targetScancode = keyObj.getValue();
            }
            if (targetScancode != -1 && scancode == targetScancode) {
                return true;
            }
        } catch (Exception ignored) {}

        return false;
    }

}
