package net.kingchoka.minetranslatorlib.mixin;

import net.kingchoka.minetranslatorlib.api.MineTranslatorLibrary;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void minetranslatorlib$afterTick(CallbackInfo ci) {
        MineTranslatorLibrary.dispatchEndClientTick((Minecraft) (Object) this);
    }
}
