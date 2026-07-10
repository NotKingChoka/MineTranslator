package net.kingchoka.minetranslator.compat.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.kingchoka.minetranslator.MineTranslator;

public class MTCompatFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MineTranslator.setCompat(new MTCompatImpl());
    }
}
