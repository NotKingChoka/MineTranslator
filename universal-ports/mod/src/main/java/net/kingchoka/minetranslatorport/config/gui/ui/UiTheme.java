package net.kingchoka.minetranslatorport.config.gui.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class UiTheme {
    public static final int BACKGROUND = 0xE60F1115;
    public static final int PANEL = 0xF2171A20;
    public static final int CARD = 0xF21D2129;
    public static final int CARD_HOVER = 0xF2242A34;
    public static final int BORDER = 0xFF343A46;
    public static final int PRIMARY = 0xFFF5A623;
    public static final int SECONDARY = 0xFF36CFE5;
    public static final int TEXT = 0xFFF2F2F2;
    public static final int MUTED = 0xFFA7AFBD;
    public static final int SUCCESS = 0xFF4CD97B;
    public static final int WARNING = 0xFFFFBE45;
    public static final int ERROR = 0xFFFF5D68;
    public static final int SHADOW = 0x70000000;

    private UiTheme() {}

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x + 3, y + 4, x + width + 3, y + height + 4, SHADOW);
        roundedFill(graphics, x, y, width, height, BORDER);
        roundedFill(graphics, x + 1, y + 1, width - 2, height - 2, color);
    }

    public static void roundedFill(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (width <= 2 || height <= 2) return;
        graphics.fill(x + 1, y, x + width - 1, y + height, color);
        graphics.fill(x, y + 1, x + width, y + height - 1, color);
    }

    public static void border(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x + 1, y, x + width - 1, y + 1, color);
        graphics.fill(x + 1, y + height - 1, x + width - 1, y + height, color);
        graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    public static void drawEllipsizedText(
        GuiGraphics graphics, Font font, Component text, int x, int y, int maxWidth, int color
    ) {
        String value = text.getString();
        if (font.width(value) <= maxWidth) {
            graphics.drawString(font, value, x, y, color, false);
            return;
        }
        String ellipsis = "...";
        String clipped = font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width(ellipsis)));
        graphics.drawString(font, clipped + ellipsis, x, y, color, false);
    }

    public static int mix(int from, int to, float amount) {
        amount = Math.max(0.0F, Math.min(1.0F, amount));
        int a = lerp((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, amount);
        int r = lerp((from >>> 16) & 0xFF, (to >>> 16) & 0xFF, amount);
        int g = lerp((from >>> 8) & 0xFF, (to >>> 8) & 0xFF, amount);
        int b = lerp(from & 0xFF, to & 0xFF, amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerp(int from, int to, float amount) {
        return Math.round(from + (to - from) * amount);
    }
}
