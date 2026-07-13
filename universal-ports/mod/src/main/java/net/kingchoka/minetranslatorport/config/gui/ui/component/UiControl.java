package net.kingchoka.minetranslatorport.config.gui.ui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public interface UiControl {
    void setBounds(int x, int y, int width, int height);
    void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick);
    boolean mouseClicked(double mouseX, double mouseY, int button);
    default void tick() {}
    default boolean isOpen() { return false; }
    default void close() {}
    default boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
    default boolean charTyped(char codePoint, int modifiers) { return false; }
    default boolean mouseReleased(double mouseX, double mouseY, int button) { return false; }
    default boolean isFocused() { return false; }
    default void setFocused(boolean focused) {}
}
