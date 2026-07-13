package net.kingchoka.mtlib.mixin;

import net.kingchoka.mtlib.api.MTLibrary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.minecraft.client.Minecraft", remap = false)
public abstract class ClientTick26Mixin {
    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void minetranslatorlib$endTick26(CallbackInfo ci) {
        MTLibrary.fireTick(this);
    }
}
