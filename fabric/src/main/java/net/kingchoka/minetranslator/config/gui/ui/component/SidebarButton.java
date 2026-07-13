package net.kingchoka.minetranslator.config.gui.ui.component;

import net.kingchoka.minetranslator.config.gui.ui.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class SidebarButton {
    private final String icon;
    private final Component label;
    private int x;
    private int y;
    private int width;
    private int height;
    private float hover;

    public SidebarButton(String icon, Component label) {
        this.icon = icon;
        this.label = label;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, boolean selected, float partialTick) {
        boolean hovered = contains(mouseX, mouseY);
        float target = hovered || selected ? 1.0F : 0.0F;
        hover += (target - hover) * Math.min(1.0F, 0.28F + partialTick * 0.12F);
        int color = UiTheme.mix(UiTheme.PANEL, UiTheme.CARD_HOVER, hover);
        if (selected) color = UiTheme.mix(color, UiTheme.PRIMARY, 0.12F);
        UiTheme.roundedFill(graphics, x, y, width, height, color);
        if (selected) graphics.fill(x, y + 3, x + 2, y + height - 3, UiTheme.PRIMARY);
        graphics.drawString(font, icon, x + 8, y + (height - font.lineHeight) / 2, selected ? UiTheme.PRIMARY : UiTheme.MUTED, false);
        UiTheme.drawEllipsizedText(graphics, font, label, x + 24, y + (height - font.lineHeight) / 2,
            width - 30, selected ? UiTheme.TEXT : UiTheme.MUTED);
        if (hovered && font.width(label) > width - 30) graphics.setTooltipForNextFrame(font, label, mouseX, mouseY);
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
