package net.kingchoka.minetranslatorport.config.gui.ui.component;

import net.kingchoka.minetranslatorport.config.gui.ui.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class SettingRow {
    private final Component label;
    private final Component description;
    private final UiControl control;
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean compact;

    public SettingRow(Component label, Component description, UiControl control) {
        this.label = label;
        this.description = description;
        this.control = control;
    }

    public void setBounds(int x, int y, int width, int height, boolean compact) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.compact = compact;
        int controlWidth = compact ? Math.min(112, width / 3) : Math.min(164, width / 3);
        control.setBounds(x + width - controlWidth - 10, y + 6, controlWidth, height - 12);
    }

    public void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, float partialTick) {
        boolean hovered = contains(mouseX, mouseY);
        if (hovered) UiTheme.roundedFill(graphics, x, y, width, height, UiTheme.CARD_HOVER);
        int controlWidth = compact ? Math.min(112, width / 3) : Math.min(164, width / 3);
        int labelWidth = width - controlWidth - 34;
        int labelY = compact ? y + 8 : y + 7;
        UiTheme.drawEllipsizedText(graphics, font, label, x + 10, labelY, labelWidth, UiTheme.TEXT);
        if (!compact && !description.getString().isEmpty()) {
            UiTheme.drawEllipsizedText(graphics, font, description, x + 10, y + 21, labelWidth, UiTheme.MUTED);
        }
        control.render(graphics, font, mouseX, mouseY, partialTick);

        if (hovered && (font.width(label) > labelWidth || (!compact && font.width(description) > labelWidth))) {
            TooltipRenderer.show(graphics, font, Component.empty().append(label).append("\n").append(description), mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return control.mouseClicked(mouseX, mouseY, button);
    }

    public UiControl control() { return control; }

    private boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
