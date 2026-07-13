package net.kingchoka.mtlib.mixin;

import net.kingchoka.mtlib.api.MTLibrary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.minecraft.class_338", remap = false)
public abstract class ChatHudMixin {
    @Inject(
        method = "method_1812(Lnet/minecraft/class_2561;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0,
        remap = false
    )
    private void minetranslatorlib$chat(@Coerce Object message, CallbackInfo ci) {
        if (MTLibrary.fireChat(this, message)) ci.cancel();
    }
}
