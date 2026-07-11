package net.kingchoka.minetranslator.mixin;

import net.kingchoka.minetranslator.keybind.MTKeyMappings;
import net.kingchoka.minetranslator.tooltip.TooltipTranslationController;
import net.kingchoka.minetranslator.chat.ChatTranslationController;
import net.kingchoka.minetranslator.api.ChatComponentMixinAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        KeyEvent context = new KeyEvent(keyCode, scanCode, modifiers);
        boolean matchTranslate = MTKeyMappings.TRANSLATE_KEY.matches(context);
        boolean matchTranslateItem = MTKeyMappings.TRANSLATE_ITEM_KEY.matches(context);
        if (matchTranslate || matchTranslateItem) {
            // Check if player is currently typing in a text field
            if (isTextFieldFocused((Screen) (Object) this)) {
                return;
            }

            TooltipTranslationController.setTranslateKeyPressed(true);
            
            // Handle chat message translation under cursor
            Minecraft client = Minecraft.getInstance();
            if (client.gui != null && client.gui.getChat() != null) {
                double mouseX = client.mouseHandler.xpos();
                double mouseY = client.mouseHandler.ypos();
                try {
                    GuiMessage msg = ((ChatComponentMixinAccessor) client.gui.getChat()).MineTranslator$getMessageAt(mouseX, mouseY);
                    if (msg != null) {
                        ChatTranslationController.getInstance().translateMessage(msg, client.gui.getChat(), true);
                    }
                } catch (Exception ignored) {}
            }
            
            cir.setReturnValue(true);
        }
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
