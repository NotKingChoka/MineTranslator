package net.kingchoka.minetranslator.config.gui.ui.component;

import net.kingchoka.minetranslator.config.gui.ui.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Dropdown implements UiControl {
    private final List<String> options;
    private final Supplier<String> getter;
    private final Consumer<String> setter;
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean open;

    public Dropdown(List<String> options, Supplier<String> getter, Consumer<String> setter) {
        this.options = List.copyOf(options);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        this.width = Math.min(150, Math.max(86, width));
        this.height = Math.min(20, Math.max(16, height));
        this.x = x + width - this.width;
        this.y = y + (height - this.height) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        UiTheme.roundedFill(graphics, x, y, width, height, contains(mouseX, mouseY) ? UiTheme.CARD_HOVER : UiTheme.PANEL);
        UiTheme.border(graphics, x, y, width, height, open ? UiTheme.PRIMARY : UiTheme.BORDER);
        UiTheme.drawEllipsizedText(graphics, font, Component.literal(getter.get()), x + 6, y + 6, width - 22, UiTheme.TEXT);
        graphics.drawString(font, open ? "^" : "v", x + width - 12, y + 6, UiTheme.MUTED, false);
        if (open) {
            int optionHeight = 18;
            int listHeight = options.size() * optionHeight + 4;
            UiTheme.panel(graphics, x, y + height + 2, width, listHeight, UiTheme.PANEL);
            for (int i = 0; i < options.size(); i++) {
                int optionY = y + height + 4 + i * optionHeight;
                boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= optionY && mouseY < optionY + optionHeight;
                if (hovered) graphics.fill(x + 2, optionY, x + width - 2, optionY + optionHeight, UiTheme.CARD_HOVER);
                int color = options.get(i).equals(getter.get()) ? UiTheme.PRIMARY : UiTheme.TEXT;
                graphics.drawString(font, options.get(i), x + 6, optionY + 5, color, false);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (contains(mouseX, mouseY)) {
            open = !open;
            return true;
        }
        if (open) {
            int optionHeight = 18;
            for (int i = 0; i < options.size(); i++) {
                int optionY = y + height + 4 + i * optionHeight;
                if (mouseX >= x && mouseX < x + width && mouseY >= optionY && mouseY < optionY + optionHeight) {
                    setter.accept(options.get(i));
                    open = false;
                    return true;
                }
            }
            open = false;
        }
        return false;
    }

    @Override
    public boolean isOpen() { return open; }

    @Override
    public void close() { open = false; }

    private boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
