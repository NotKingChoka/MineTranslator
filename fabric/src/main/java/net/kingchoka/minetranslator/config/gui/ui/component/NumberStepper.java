package net.kingchoka.minetranslator.config.gui.ui.component;

import net.kingchoka.minetranslator.config.gui.ui.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class NumberStepper implements UiControl {
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final int min, max, step;
    private int x, y, width, height;

    public NumberStepper(IntSupplier getter, IntConsumer setter, int min, int max, int step) {
        this.getter = getter; this.setter = setter; this.min = min; this.max = max; this.step = step;
    }
    @Override public void setBounds(int x, int y, int width, int height) { this.x=x; this.y=y; this.width=width; this.height=height; }
    @Override public void render(GuiGraphics g, Font f, int mx, int my, float pt) {
        UiTheme.roundedFill(g,x,y,width,height,UiTheme.BACKGROUND); UiTheme.border(g,x,y,width,height,UiTheme.BORDER);
        g.drawCenteredString(f,"−",x+11,y+(height-f.lineHeight)/2,UiTheme.MUTED);
        g.drawCenteredString(f,Integer.toString(getter.getAsInt()),x+width/2,y+(height-f.lineHeight)/2,UiTheme.SECONDARY);
        g.drawCenteredString(f,"+",x+width-11,y+(height-f.lineHeight)/2,UiTheme.MUTED);
    }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        if(button!=0||mx<x||mx>=x+width||my<y||my>=y+height)return false;
        int value=getter.getAsInt(); setter.accept(Math.max(min,Math.min(max,value+(mx<x+width/2?-step:step)))); return true;
    }
}
