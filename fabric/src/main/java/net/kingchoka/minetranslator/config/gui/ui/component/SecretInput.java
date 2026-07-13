package net.kingchoka.minetranslator.config.gui.ui.component;

import net.kingchoka.minetranslator.config.gui.ui.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SecretInput extends TextInput {
    private boolean revealed;

    public SecretInput(Supplier<String> getter, Consumer<String> setter) { super(getter, setter); }

    @Override protected String displayValue() {
        if (revealed) return value();
        return "•".repeat(Math.min(18, value().length()));
    }

    @Override protected String displayPrefix() {
        if (revealed) return super.displayPrefix();
        return "•".repeat(Math.min(cursor, 18));
    }

    @Override public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, font, mouseX, mouseY, partialTick);
        graphics.drawString(font, revealed ? "×" : "○", x + width - 13, y + (height - font.lineHeight) / 2,
            UiTheme.MUTED, false);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= x + width - 18 && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            revealed = !revealed;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
