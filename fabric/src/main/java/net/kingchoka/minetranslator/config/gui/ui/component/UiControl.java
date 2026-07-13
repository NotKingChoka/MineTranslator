package net.kingchoka.minetranslator.config.gui.ui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

public interface UiControl {
    void setBounds(int x, int y, int width, int height);
    void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick);
    boolean mouseClicked(double mouseX, double mouseY, int button);
    default void tick() {}
    default boolean isOpen() { return false; }
    default void close() {}
    default boolean keyPressed(KeyEvent event) { return false; }
    default boolean charTyped(CharacterEvent event) { return false; }
    default boolean isFocused() { return false; }
    default void setFocused(boolean focused) {}
}
