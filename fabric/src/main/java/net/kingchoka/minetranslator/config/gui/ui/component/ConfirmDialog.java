package net.kingchoka.minetranslator.config.gui.ui.component;

import net.kingchoka.minetranslator.config.gui.ui.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class ConfirmDialog {
    public void render(GuiGraphics g, Font f, int screenW, int screenH, Component title, Component description) {
        g.fill(0,0,screenW,screenH,0x99000000);
        int w=Math.min(380,screenW-30),h=116,x=(screenW-w)/2,y=(screenH-h)/2;
        UiTheme.panel(g,x,y,w,h,UiTheme.PANEL);
        g.drawString(f,title,x+14,y+14,UiTheme.TEXT,false);
        UiTheme.drawEllipsizedText(g,f,description,x+14,y+34,w-28,UiTheme.MUTED);
    }
}
