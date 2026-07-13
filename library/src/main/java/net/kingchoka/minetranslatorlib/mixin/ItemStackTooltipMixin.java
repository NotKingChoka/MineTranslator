package net.kingchoka.minetranslatorlib.mixin;

import net.kingchoka.minetranslatorlib.api.MineTranslatorLibrary;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {
    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void minetranslatorlib$tooltip(Item.TooltipContext context, Player player, TooltipFlag flag,
                                          CallbackInfoReturnable<List<Component>> cir) {
        MineTranslatorLibrary.dispatchItemTooltip((ItemStack) (Object) this, context, flag, cir.getReturnValue());
    }
}
