package net.psunset.translatorpp.compat.fabric.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.psunset.translatorpp.config.gui.TPPConfigScreen;

public final class TPPModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return TPPConfigScreen::new;
    }
}
