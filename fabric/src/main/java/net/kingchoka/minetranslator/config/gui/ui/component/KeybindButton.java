package net.kingchoka.minetranslator.config.gui.ui.component;

import com.mojang.blaze3d.platform.InputConstants;
import net.kingchoka.minetranslator.config.gui.ui.UiTheme;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class KeybindButton implements UiControl {
    private final KeyMapping mapping;
    private final Runnable changed;
    private int x,y,width,height;
    private boolean listening;

    public KeybindButton(KeyMapping mapping, Runnable changed) { this.mapping=mapping; this.changed=changed; }
    @Override public void setBounds(int x,int y,int w,int h){this.x=x;this.y=y;this.width=w;this.height=h;}
    @Override public void render(GuiGraphics g, Font f, int mx, int my, float pt){
        boolean conflict=conflict();
        UiTheme.roundedFill(g,x,y,width,height,listening?UiTheme.CARD_HOVER:UiTheme.BACKGROUND);
        UiTheme.border(g,x,y,width,height,listening?UiTheme.PRIMARY:(conflict?UiTheme.ERROR:UiTheme.BORDER));
        Component label=listening?Component.translatable("MineTranslator.keybind.listening"):mapping.getTranslatedKeyMessage();
        UiTheme.drawEllipsizedText(g,f,label,x+7,y+(height-f.lineHeight)/2,width-14,conflict?UiTheme.ERROR:UiTheme.SECONDARY);
        if(conflict&&mx>=x&&mx<x+width&&my>=y&&my<y+height)
            g.setTooltipForNextFrame(f,Component.translatable("MineTranslator.keybind.conflict"),mx,my);
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(mx<x||mx>=x+width||my<y||my>=y+height)return false;
        if(button==0){listening=true;return true;}
        if(button==1){mapping.setKey(mapping.getDefaultKey());KeyMapping.resetMapping();changed.run();return true;}
        if(button==2){mapping.setKey(InputConstants.UNKNOWN);KeyMapping.resetMapping();changed.run();return true;}
        return false;
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(!listening)return false;
        listening=false;
        if(event.key()==GLFW.GLFW_KEY_ESCAPE)return true;
        mapping.setKey(event.key()==GLFW.GLFW_KEY_DELETE||event.key()==GLFW.GLFW_KEY_BACKSPACE
            ?InputConstants.UNKNOWN:InputConstants.getKey(event));
        KeyMapping.resetMapping(); changed.run(); return true;
    }
    @Override public boolean isFocused(){return listening;}
    @Override public void setFocused(boolean focused){if(!focused)listening=false;}
    private boolean conflict(){
        Minecraft client=Minecraft.getInstance();
        if(client.options==null||mapping.isUnbound())return false;
        for(KeyMapping other:client.options.keyMappings)if(other!=mapping&&mapping.same(other))return true;
        return false;
    }
}
