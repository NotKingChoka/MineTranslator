package net.kingchoka.mtlib.mixin;

import net.kingchoka.mtlib.api.MTLibrary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.minecraft.class_310", remap = false)
public abstract class ClientTickMixin {
    @Inject(method = "method_1574", at = @At("TAIL"), remap = false)
    private void minetranslatorlib$endTick(CallbackInfo ci) {
        MTLibrary.fireTick(this);
    }
}
