package net.kingchoka.mtlib.mixin;

import net.kingchoka.mtlib.api.MTLibrary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.minecraft.class_338", remap = false)
public abstract class ChatHudMixin {
    @Unique
    private Object minetranslatorlib$simpleMessage;

    @Inject(
        method = "method_1812(Lnet/minecraft/class_2561;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0,
        remap = false
    )
    private void minetranslatorlib$chat(@Coerce Object message, CallbackInfo ci) {
        // Since 1.19.2 this convenience method forwards to method_44811.
        // Remember the component so the second injector does not emit it twice.
        minetranslatorlib$simpleMessage = message;
        if (MTLibrary.fireChat(this, message)) ci.cancel();
    }

    @Inject(
        method = "method_44811(Lnet/minecraft/class_2561;Lnet/minecraft/class_7469;Lnet/minecraft/class_7591;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0,
        remap = false
    )
    private void minetranslatorlib$signedChat(
        @Coerce Object message,
        @Coerce Object signature,
        @Coerce Object indicator,
        CallbackInfo ci
    ) {
        if (message == minetranslatorlib$simpleMessage) {
            minetranslatorlib$simpleMessage = null;
            return;
        }
        minetranslatorlib$simpleMessage = null;
        if (MTLibrary.fireChat(this, message)) ci.cancel();
    }
}
