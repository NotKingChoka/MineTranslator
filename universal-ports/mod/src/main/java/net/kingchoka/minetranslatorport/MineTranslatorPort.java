package net.kingchoka.minetranslatorport;

import net.fabricmc.api.ClientModInitializer;

public final class MineTranslatorPort implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PortController.get().initialize();
    }
}
