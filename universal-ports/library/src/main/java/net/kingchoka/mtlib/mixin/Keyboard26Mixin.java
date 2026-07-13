package net.kingchoka.mtlib.mixin;

import net.kingchoka.mtlib.api.MTLibrary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Pseudo
@Mixin(targets = "net.minecraft.client.KeyboardHandler", remap = false)
public abstract class Keyboard26Mixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true, remap = false)
    private void minetranslatorlib$key26(long window, int action, @Coerce Object event,
                                         CallbackInfo ci) {
        int key = readInt(event, "key");
        int scanCode = readInt(event, "scancode");
        int modifiers = readInt(event, "modifiers");
        if (MTLibrary.fireKey(this, window, key, scanCode, action, modifiers)) ci.cancel();
    }

    private static int readInt(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            return result instanceof Number ? ((Number) result).intValue() : 0;
        } catch (ReflectiveOperationException ignored) {
            return 0;
        }
    }
}
