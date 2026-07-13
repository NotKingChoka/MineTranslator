package net.kingchoka.mtlib.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.kingchoka.mtlib.api.MTLibraryEntrypoint;

public final class MTLibraryMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        for (MTLibraryEntrypoint entrypoint : FabricLoader.getInstance()
                .getEntrypoints("minetranslator-library", MTLibraryEntrypoint.class)) {
            entrypoint.registerHooks();
        }
    }
}
