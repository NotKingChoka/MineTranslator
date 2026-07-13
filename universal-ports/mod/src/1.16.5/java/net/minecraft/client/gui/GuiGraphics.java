package net.minecraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GuiGraphics {
    private final PoseStack poseStack;

    public GuiGraphics(PoseStack poseStack) {
        this.poseStack = poseStack;
    }

    public PoseStack pose() {
        return this.poseStack;
    }

    public void fill(int minX, int minY, int maxX, int maxY, int color) {
        Screen.fill(this.poseStack, minX, minY, maxX, maxY, color);
    }

    public void fillGradient(int x1, int y1, int x2, int y2, int colorFrom, int colorTo) {
        // Рефлексивный вызов fillGradient в Screen, так как сигнатуры могут немного отличаться
        try {
            Screen.class.getDeclaredMethod("fillGradient", PoseStack.class, int.class, int.class, int.class, int.class, int.class, int.class)
                .invoke(null, this.poseStack, x1, y1, x2, y2, colorFrom, colorTo);
        } catch (Exception e) {
            try {
                // Альтернативная сигнатура (или метод в GuiComponent на старых версиях)
                Class<?> guiComponentClass = Class.forName("net.minecraft.client.gui.GuiComponent");
                guiComponentClass.getDeclaredMethod("fillGradient", PoseStack.class, int.class, int.class, int.class, int.class, int.class, int.class)
                    .invoke(null, this.poseStack, x1, y1, x2, y2, colorFrom, colorTo);
            } catch (Exception ex) {
                // Если не удалось, рисуем обычным fill
                fill(x1, y1, x2, y2, colorFrom);
            }
        }
    }

    public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawShadow(this.poseStack, text, x, y, color);
        } else {
            font.draw(this.poseStack, text, x, y, color);
        }
    }

    public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        drawString(font, text.getString(), x, y, color, shadow);
    }

    public void drawCenteredString(Font font, String text, int x, int y, int color) {
        try {
            Screen.class.getDeclaredMethod("drawCenteredString", PoseStack.class, Font.class, String.class, int.class, int.class, int.class)
                .invoke(null, this.poseStack, font, text, x, y, color);
        } catch (Exception e) {
            try {
                Class<?> guiComponentClass = Class.forName("net.minecraft.client.gui.GuiComponent");
                guiComponentClass.getDeclaredMethod("drawCenteredString", PoseStack.class, Font.class, String.class, int.class, int.class, int.class)
                    .invoke(null, this.poseStack, font, text, x, y, color);
            } catch (Exception ex) {
                drawString(font, text, x - font.width(text) / 2, y, color, true);
            }
        }
    }

    public void drawCenteredString(Font font, Component text, int x, int y, int color) {
        drawCenteredString(font, text.getString(), x, y, color);
    }

    public void enableScissor(int x1, int y1, int x2, int y2) {
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        int x = (int) (x1 * scale);
        int y = (int) (Minecraft.getInstance().getWindow().getScreenHeight() - y2 * scale);
        int w = (int) ((x2 - x1) * scale);
        int h = (int) ((y2 - y1) * scale);
        RenderSystem.enableScissor(x, y, w, h);
    }

    public void disableScissor() {
        RenderSystem.disableScissor();
    }
}
