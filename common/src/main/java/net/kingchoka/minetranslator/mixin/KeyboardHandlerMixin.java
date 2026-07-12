package net.kingchoka.minetranslator.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.platform.InputConstants;
import net.kingchoka.minetranslator.keybind.MTKeyMappings;
import net.kingchoka.minetranslator.tooltip.TooltipTranslationController;
import net.kingchoka.minetranslator.chat.ChatTranslationController;
import net.kingchoka.minetranslator.api.ChatComponentMixinAccessor;
import net.kingchoka.minetranslator.debug.TranslationDebugLogger;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void onKeyPress(long windowPointer, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (isTranslateKey(key, scancode)) {
            TranslationDebugLogger.info("[KeyboardHandlerMixin] Translate key matched: key={}, scancode={}, action={}", key, scancode, action);
            if (action == 1) { // GLFW_PRESS
                TooltipTranslationController.setTranslateKeyPressed(true);
                
                // Handle chat translation under cursor if chat is open
                Minecraft client = Minecraft.getInstance();
                boolean chatTranslated = false;
                if (client.gui != null && client.gui.getChat() != null) {
                    double mouseX = client.mouseHandler.xpos();
                    double mouseY = client.mouseHandler.ypos();
                    try {
                        GuiMessage msg = ((ChatComponentMixinAccessor) client.gui.getChat()).MineTranslator$getMessageAt(mouseX, mouseY);
                        if (msg != null) {
                            ChatTranslationController.getInstance().translateMessage(msg, client.gui.getChat(), true);
                            chatTranslated = true;
                        }
                    } catch (Exception ignored) {}
                }

                // If chat message was successfully translated or if we are not typing in a text field,
                // cancel the event to prevent typing the grave accent/Ё character.
                boolean isFieldFocused = isTextFieldFocused(client.screen);
                if (!isFieldFocused || chatTranslated) {
                    ci.cancel();
                }
            } else if (action == 0) { // GLFW_RELEASE
                TooltipTranslationController.setTranslateKeyPressed(false);
            }
        }
    }

    private boolean isTranslateKey(int key, int scancode) {
        var mapping = MTKeyMappings.TRANSLATE_KEY;
        if (mapping == null) return false;

        var keyObj = ((KeyMappingAccessor) mapping).MineTranslator$getKey();
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

    private boolean isTextFieldFocused(Screen screen) {
        if (screen == null) return false;
        var focused = screen.getFocused();
        if (focused == null) return false;
        String className = focused.getClass().getName().toLowerCase();
        return className.contains("editbox") 
            || className.contains("textfield") 
            || className.contains("search") 
            || className.contains("input");
    }
}
