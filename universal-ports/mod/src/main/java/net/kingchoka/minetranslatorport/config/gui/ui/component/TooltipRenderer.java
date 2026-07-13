package net.kingchoka.minetranslatorport.config.gui.ui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.lang.reflect.Method;

public final class TooltipRenderer {
    private static Method setTooltipMethod;
    private static Method renderTooltipMethod;
    private static boolean initialized;

    private TooltipRenderer() {}

    private static void initialize() {
        if (initialized) return;
        initialized = true;
        try {
            setTooltipMethod = GuiGraphics.class.getMethod("setTooltipForNextFrame", Font.class, Component.class, int.class, int.class);
        } catch (NoSuchMethodException ignored) {
            try {
                renderTooltipMethod = GuiGraphics.class.getMethod("renderTooltip", Font.class, Component.class, int.class, int.class);
            } catch (NoSuchMethodException ignored2) {}
        }
    }

    public static void show(GuiGraphics graphics, Font font, Component text, int mouseX, int mouseY) {
        initialize();
        if (setTooltipMethod != null) {
            try {
                setTooltipMethod.invoke(graphics, font, text, mouseX, mouseY);
                return;
            } catch (Exception ignored) {}
        }
        if (renderTooltipMethod != null) {
            try {
                renderTooltipMethod.invoke(graphics, font, text, mouseX, mouseY);
                return;
            } catch (Exception ignored) {}
        }
    }
}
