package net.kingchoka.minetranslatorport.config.gui.ui.component;

import net.kingchoka.minetranslatorport.config.gui.ui.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class ProviderCard {
    private final String provider;
    private final Component description;
    private final boolean needsKey;
    private int x;
    private int y;
    private int width;
    private int height;

    public ProviderCard(String provider, Component description, boolean needsKey) {
        this.provider = provider;
        this.description = description;
        this.needsKey = needsKey;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    public void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, boolean selected, boolean hasKey) {
        boolean hovered = contains(mouseX, mouseY);
        int cardColor = hovered ? UiTheme.CARD_HOVER : UiTheme.CARD;
        UiTheme.panel(graphics, x, y, width, height, cardColor);
        if (selected) UiTheme.border(graphics, x, y, width, height, UiTheme.PRIMARY);
        graphics.text(font, provider, x + 10, y + 10, selected ? UiTheme.PRIMARY : UiTheme.TEXT, false);
        int stateColor = !needsKey || hasKey ? UiTheme.SUCCESS : UiTheme.WARNING;
        UiTheme.roundedFill(graphics, x + width - 16, y + 10, 6, 6, stateColor);
        UiTheme.drawEllipsizedText(graphics, font, description, x + 10, y + 26, width - 20, UiTheme.MUTED);
        Component status = Component.translatable(!needsKey || hasKey
            ? "MineTranslator.status.connected" : "MineTranslator.status.api_key_missing");
        UiTheme.drawEllipsizedText(graphics, font, status, x + 10, y + height - 18, width - 20, stateColor);
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public String provider() { return provider; }
}
