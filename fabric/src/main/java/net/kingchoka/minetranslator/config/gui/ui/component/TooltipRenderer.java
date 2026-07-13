package net.kingchoka.minetranslator.config.gui.ui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class TooltipRenderer {
    private TooltipRenderer() {}
    public static void show(GuiGraphics graphics, Font font, Component text, int mouseX, int mouseY) {
        graphics.setTooltipForNextFrame(font, text, mouseX, mouseY);
    }
}
