package net.kingchoka.minetranslator.config.gui.ui.component;

import net.kingchoka.minetranslator.config.gui.ui.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public final class Slider implements UiControl {
    private final DoubleSupplier getter; private final DoubleConsumer setter; private final double min,max;
    private int x,y,width,height;
    public Slider(DoubleSupplier getter,DoubleConsumer setter,double min,double max){this.getter=getter;this.setter=setter;this.min=min;this.max=max;}
    @Override public void setBounds(int x,int y,int w,int h){this.x=x;this.y=y;this.width=w;this.height=h;}
    @Override public void render(GuiGraphics g,Font f,int mx,int my,float pt){
        int cy=y+height/2; g.fill(x+6,cy-1,x+width-6,cy+1,UiTheme.BORDER);
        double n=Math.max(0,Math.min(1,(getter.getAsDouble()-min)/(max-min))); int px=x+6+(int)((width-12)*n);
        g.fill(x+6,cy-1,px,cy+1,UiTheme.PRIMARY); UiTheme.roundedFill(g,px-4,cy-4,8,8,UiTheme.PRIMARY);
    }
    @Override public boolean mouseClicked(double mx,double my,int button){if(button!=0||mx<x||mx>=x+width||my<y||my>=y+height)return false;setter.accept(min+(max-min)*((mx-x)/(double)width));return true;}
}
