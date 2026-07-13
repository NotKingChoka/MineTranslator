package net.kingchoka.mtlib.mixin;

import net.kingchoka.mtlib.api.MTLibrary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(targets = "net.minecraft.class_1799", remap = false)
public abstract class ItemTooltipMixin {
    @SuppressWarnings("unchecked")
    @Inject(method = "method_7950", at = @At("RETURN"), require = 0, remap = false)
    private void minetranslatorlib$tooltip(CallbackInfoReturnable<List<Object>> cir) {
        List<Object> lines = cir.getReturnValue();
        if (lines != null) MTLibrary.fireTooltip(this, lines);
    }
}
