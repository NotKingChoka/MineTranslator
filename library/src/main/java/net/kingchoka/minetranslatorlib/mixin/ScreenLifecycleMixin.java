package net.kingchoka.minetranslatorlib.mixin;

import net.kingchoka.minetranslatorlib.api.MineTranslatorLibrary;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenLifecycleMixin {
    @Inject(method = "keyPressed", at = @At("HEAD"))
    private void minetranslatorlib$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        MineTranslatorLibrary.dispatchScreenKeyPressed((Screen) (Object) this, event);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void minetranslatorlib$removed(CallbackInfo ci) {
        MineTranslatorLibrary.dispatchScreenRemoved((Screen) (Object) this);
    }
}
