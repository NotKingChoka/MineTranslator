package net.kingchoka.minetranslatorport.config.gui.ui.component;

import net.kingchoka.minetranslatorport.config.gui.ui.UiTheme;
import net.minecraft.client.gui.GuiGraphics;

public final class ScrollContainer {
    private int x;
    private int y;
    private int width;
    private int height;
    private int contentHeight;
    private double scroll;
    private boolean dragging;

    public void setBounds(int x, int y, int width, int height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
        clamp();
    }

    public void setContentHeight(int contentHeight) {
        this.contentHeight = Math.max(0, contentHeight);
        clamp();
    }

    public boolean scroll(double mouseX, double mouseY, double amount) {
        if (!contains(mouseX, mouseY) || contentHeight <= height) return false;
        scroll -= amount * 24.0;
        clamp();
        return true;
    }

    public void begin(GuiGraphics graphics) {
        graphics.enableScissor(x, y, x + width, y + height);
    }

    public void end(GuiGraphics graphics) {
        graphics.disableScissor();
    }

    public void renderScrollbar(GuiGraphics graphics) {
        if (contentHeight <= height) return;
        int trackX = x + width - 4;
        int thumbHeight = Math.max(18, height * height / contentHeight);
        int travel = height - thumbHeight;
        int max = Math.max(1, contentHeight - height);
        int thumbY = y + (int) Math.round(travel * (scroll / max));
        graphics.fill(trackX, y, trackX + 2, y + height, 0x50242A34);
        graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, UiTheme.PRIMARY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || contentHeight <= height || mouseX < x + width - 8 || mouseX >= x + width
            || mouseY < y || mouseY >= y + height) return false;
        dragging = true;
        updateFromMouse(mouseY);
        return true;
    }

    public boolean mouseDragged(double mouseY) {
        if (!dragging) return false;
        updateFromMouse(mouseY);
        return true;
    }

    public boolean mouseReleased() {
        boolean wasDragging = dragging;
        dragging = false;
        return wasDragging;
    }

    public int offset() { return (int) Math.round(scroll); }
    public double scrollValue() { return scroll; }
    public void setScrollValue(double value) { scroll = value; clamp(); }

    private void clamp() {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - height)));
    }

    private void updateFromMouse(double mouseY) {
        int thumbHeight = Math.max(18, height * height / Math.max(1, contentHeight));
        int travel = Math.max(1, height - thumbHeight);
        double normalized = (mouseY - y - thumbHeight / 2.0) / travel;
        scroll = Math.max(0, Math.min(1, normalized)) * Math.max(0, contentHeight - height);
    }

    private boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
