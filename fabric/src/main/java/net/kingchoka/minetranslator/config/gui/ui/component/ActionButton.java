package net.kingchoka.minetranslator.config.gui.ui.component;

import net.kingchoka.minetranslator.config.gui.ui.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class ActionButton implements UiControl {
    private final Component label;
    private final Runnable action;
    private final boolean primary;
    private int x;
    private int y;
    private int width;
    private int height;

    public ActionButton(Component label, Runnable action, boolean primary) {
        this.label = label;
        this.action = action;
        this.primary = primary;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        this.width = Math.min(150, Math.max(72, width));
        this.height = Math.min(22, Math.max(18, height));
        this.x = x + width - this.width;
        this.y = y + (height - this.height) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        boolean hovered = contains(mouseX, mouseY);
        int base = primary ? UiTheme.PRIMARY : UiTheme.PANEL;
        int color = hovered ? UiTheme.mix(base, UiTheme.TEXT, 0.12F) : base;
        UiTheme.roundedFill(graphics, x, y, width, height, color);
        UiTheme.border(graphics, x, y, width, height, primary ? UiTheme.PRIMARY : UiTheme.BORDER);
        int textColor = primary ? 0xFF171A20 : UiTheme.TEXT;
        UiTheme.drawEllipsizedText(graphics, font, label, x + 7, y + (height - font.lineHeight) / 2, width - 14, textColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && contains(mouseX, mouseY)) {
            action.run();
            return true;
        }
        return false;
    }

    private boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
