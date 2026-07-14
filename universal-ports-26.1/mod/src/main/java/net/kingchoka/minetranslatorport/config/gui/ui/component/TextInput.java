package net.kingchoka.minetranslatorport.config.gui.ui.component;

import net.kingchoka.minetranslatorport.config.gui.ui.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class TextInput implements UiControl {
    protected final Supplier<String> getter;
    protected final Consumer<String> setter;
    protected int x, y, width, height, cursor;
    protected boolean focused;

    public TextInput(Supplier<String> getter, Consumer<String> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    @Override public void setBounds(int x, int y, int width, int height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
        cursor = Math.min(cursor, value().length());
    }

    @Override public void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, float partialTick) {
        UiTheme.roundedFill(graphics, x, y, width, height, UiTheme.BACKGROUND);
        UiTheme.border(graphics, x, y, width, height, focused ? UiTheme.SECONDARY : UiTheme.BORDER);
        String shown = displayValue();
        int available = width - 12;
        while (font.width(shown) > available && shown.length() > 1) shown = shown.substring(1);
        graphics.text(font, shown, x + 6, y + (height - font.lineHeight) / 2, UiTheme.TEXT, false);
        if (focused && (System.currentTimeMillis() / 450L) % 2L == 0L) {
            int caretX = Math.min(x + width - 5, x + 6 + font.width(displayPrefix()));
            graphics.fill(caretX, y + 4, caretX + 1, y + height - 4, UiTheme.SECONDARY);
        }
    }

    protected String displayValue() { return value(); }
    protected String displayPrefix() {
        String text = value();
        return text.substring(0, Math.min(cursor, text.length()));
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        focused = button == 0 && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        if (focused) cursor = value().length();
        return focused;
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;
        String text = value();
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> focused = false;
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (cursor > 0) update(text.substring(0, cursor - 1) + text.substring(cursor), cursor - 1);
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (cursor < text.length()) update(text.substring(0, cursor) + text.substring(cursor + 1), cursor);
            }
            case GLFW.GLFW_KEY_LEFT -> cursor = Math.max(0, cursor - 1);
            case GLFW.GLFW_KEY_RIGHT -> cursor = Math.min(text.length(), cursor + 1);
            case GLFW.GLFW_KEY_HOME -> cursor = 0;
            case GLFW.GLFW_KEY_END -> cursor = text.length();
            default -> { return false; }
        }
        return true;
    }

    @Override public boolean charTyped(char codePoint, int modifiers) {
        if (!focused || codePoint < ' ' || codePoint == 127) return false;
        String text = value();
        String added = String.valueOf(codePoint);
        if (text.length() + added.length() > 512) return true;
        update(text.substring(0, cursor) + added + text.substring(cursor), cursor + added.length());
        return true;
    }

    @Override public boolean isFocused() { return focused; }
    @Override public void setFocused(boolean focused) { this.focused = focused; }
    protected String value() { return getter.get() == null ? "" : getter.get(); }
    protected void update(String value, int newCursor) { setter.accept(value); cursor = newCursor; }
}
