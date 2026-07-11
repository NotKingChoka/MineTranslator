package net.kingchoka.minetranslator.mixin;

import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Screen.class)
public class ScreenMixin {
    // Empty mixin, keyboard logic moved to KeyboardHandlerMixin for scancode support
}
