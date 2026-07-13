package net.kingchoka.minetranslator.config.gui.ui.component;

import net.kingchoka.minetranslator.config.gui.ui.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public final class ValuePill implements UiControl {
    private final Supplier<String> value;
    private int x;
    private int y;
    private int width;
    private int height;

    public ValuePill(Supplier<String> value) {
        this.value = value;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        this.width = Math.min(150, Math.max(76, width));
        this.height = Math.min(20, Math.max(16, height));
        this.x = x + width - this.width;
        this.y = y + (height - this.height) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        UiTheme.roundedFill(graphics, x, y, width, height, UiTheme.PANEL);
        UiTheme.border(graphics, x, y, width, height, UiTheme.BORDER);
        UiTheme.drawEllipsizedText(graphics, font, Component.literal(value.get()), x + 6, y + 6, width - 12, UiTheme.SECONDARY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
}
