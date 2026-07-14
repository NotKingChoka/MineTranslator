package net.kingchoka.minetranslatorport.config.gui.ui.component;

import net.kingchoka.minetranslatorport.config.gui.ui.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class ToggleSwitch implements UiControl {
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private final BooleanSupplier enabled;
    private int x;
    private int y;
    private int width = 34;
    private int height = 16;
    private float animation;

    public ToggleSwitch(BooleanSupplier getter, Consumer<Boolean> setter) {
        this(getter, setter, () -> true);
    }

    public ToggleSwitch(BooleanSupplier getter, Consumer<Boolean> setter, BooleanSupplier enabled) {
        this.getter = getter;
        this.setter = setter;
        this.enabled = enabled;
        this.animation = getter.getAsBoolean() ? 1.0F : 0.0F;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        this.width = Math.min(38, Math.max(30, width));
        this.height = Math.min(18, Math.max(14, height));
        this.x = x + width - this.width;
        this.y = y + (height - this.height) / 2;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, float partialTick) {
        boolean hovered = contains(mouseX, mouseY);
        float target = getter.getAsBoolean() ? 1.0F : 0.0F;
        animation += (target - animation) * Math.min(1.0F, 0.30F + partialTick * 0.15F);
        int off = hovered ? UiTheme.CARD_HOVER : UiTheme.BORDER;
        int track = enabled.getAsBoolean() ? UiTheme.mix(off, UiTheme.PRIMARY, animation) : UiTheme.PANEL;
        UiTheme.roundedFill(graphics, x, y, width, height, track);
        int knob = Math.max(10, height - 4);
        int knobX = x + 2 + Math.round((width - knob - 4) * animation);
        UiTheme.roundedFill(graphics, knobX, y + 2, knob, knob, UiTheme.TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && enabled.getAsBoolean() && contains(mouseX, mouseY)) {
            setter.accept(!getter.getAsBoolean());
            return true;
        }
        return false;
    }

    private boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
