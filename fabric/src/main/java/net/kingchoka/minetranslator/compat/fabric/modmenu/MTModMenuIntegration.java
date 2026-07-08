package net.kingchoka.minetranslator.compat.fabric.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.kingchoka.minetranslator.config.gui.MTConfigScreen;

public final class MTModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return MTConfigScreen::new;
    }
}
