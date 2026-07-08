package net.kingchoka.minetranslator.compat.fabric;

import net.kingchoka.minetranslator.compat.IMTCompat;
import net.kingchoka.minetranslator.config.gui.MTConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class MTCompatImpl implements IMTCompat {
    @Override
    public void openConfigScreen(Object parentScreen) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            mc.setScreen(new MTConfigScreen((Screen) parentScreen));
        });
    }

    @Override
    public Object createConfigScreen(Object parentScreen) {
        return new MTConfigScreen((Screen) parentScreen);
    }

    @Override
    public String getGameLanguage() {
        return Minecraft.getInstance().getLanguageManager().getSelected();
    }
}
