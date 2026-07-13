package net.kingchoka.mtlib.mixin;

import net.kingchoka.mtlib.api.MTLibrary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.minecraft.class_309", remap = false)
public abstract class KeyboardMixin {
    @Inject(method = "method_1466", at = @At("HEAD"), cancellable = true, remap = false)
    private void minetranslatorlib$key(long window, int key, int scanCode, int action,
                                       int modifiers, CallbackInfo ci) {
        if (MTLibrary.fireKey(this, window, key, scanCode, action, modifiers)) ci.cancel();
    }
}
