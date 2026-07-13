package net.kingchoka.minetranslator.config.gui.ui.component;

import net.kingchoka.minetranslator.config.gui.ui.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class SettingsCard {
    private final Component title;
    private final List<SettingRow> rows = new ArrayList<>();
    private int x;
    private int y;
    private int width;
    private int height;

    public SettingsCard(Component title) { this.title = title; }

    public SettingsCard add(SettingRow row) {
        rows.add(row);
        return this;
    }

    public int preferredHeight(boolean compact) {
        return 28 + rows.size() * (compact ? 34 : 42) + 8;
    }

    public void setBounds(int x, int y, int width, boolean compact) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = preferredHeight(compact);
        int rowHeight = compact ? 34 : 42;
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setBounds(x + 6, y + 26 + i * rowHeight, width - 12, rowHeight - 2, compact);
        }
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        UiTheme.panel(graphics, x, y, width, height, UiTheme.CARD);
        graphics.drawString(font, title, x + 10, y + 9, UiTheme.PRIMARY, false);
        graphics.fill(x + 8, y + 23, x + width - 8, y + 24, UiTheme.BORDER);
        for (SettingRow row : rows) row.render(graphics, font, mouseX, mouseY, partialTick);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (SettingRow row : rows) {
            if (row.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    public List<SettingRow> rows() { return rows; }
    public int height() { return height; }
}
