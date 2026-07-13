package net.kingchoka.minetranslator.config.gui;

import net.kingchoka.minetranslator.config.gui.ui.MineTranslatorConfigScreen;
import net.minecraft.client.gui.screens.Screen;

public final class MTConfigScreen {
    private MTConfigScreen() {}

    public static Screen create(Screen parent) {
        return new MineTranslatorConfigScreen(parent);
    }
}
