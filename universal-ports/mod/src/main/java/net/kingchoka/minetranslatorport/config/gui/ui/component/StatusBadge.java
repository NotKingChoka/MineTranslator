package net.kingchoka.minetranslatorport.config.gui.ui.component;

import net.kingchoka.minetranslatorport.config.gui.ui.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class StatusBadge {
    public void render(GuiGraphics graphics, Font font, Component text, int x, int y, boolean ok) {
        int color = ok ? UiTheme.SUCCESS : UiTheme.WARNING;
        int width = font.width(text) + 22;
        UiTheme.roundedFill(graphics, x, y, width, 18, UiTheme.CARD);
        UiTheme.border(graphics, x, y, width, 18, UiTheme.BORDER);
        UiTheme.roundedFill(graphics, x + 6, y + 6, 6, 6, color);
        graphics.drawString(font, text, x + 16, y + 5, UiTheme.TEXT, false);
    }
}
