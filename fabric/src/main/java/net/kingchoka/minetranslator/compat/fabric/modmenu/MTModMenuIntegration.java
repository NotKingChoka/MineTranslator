package net.kingchoka.minetranslator.compat.fabric.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.kingchoka.minetranslator.MineTranslator;
import net.minecraft.client.gui.screens.Screen;

public final class MTModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            var compat = MineTranslator.getCompat();
            return compat != null ? (Screen) compat.createConfigScreen(parent) : null;
        };
    }
}
