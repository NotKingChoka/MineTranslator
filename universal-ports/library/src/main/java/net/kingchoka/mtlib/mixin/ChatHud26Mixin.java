package net.kingchoka.mtlib.mixin;

import net.kingchoka.mtlib.api.MTLibrary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent", remap = false)
public abstract class ChatHud26Mixin {
    @Inject(
        method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
        at = @At("HEAD"), cancellable = true, require = 0, remap = false
    )
    private void minetranslatorlib$chat26(@Coerce Object message, CallbackInfo ci) {
        if (MTLibrary.fireChat(this, message)) ci.cancel();
    }
}
