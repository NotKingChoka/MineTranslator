package net.kingchoka.minetranslatorlib.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.kingchoka.minetranslatorlib.api.MineTranslatorLibraryEntrypoint;

/** Loads integrations from mods which use MineTranslator Library. */
public final class MineTranslatorLibraryMod implements ClientModInitializer {
    public static final String ENTRYPOINT_KEY = "minetranslator-library";

    @Override
    public void onInitializeClient() {
        for (MineTranslatorLibraryEntrypoint entrypoint : FabricLoader.getInstance()
            .getEntrypoints(ENTRYPOINT_KEY, MineTranslatorLibraryEntrypoint.class)) {
            entrypoint.registerLibraryHooks();
        }
    }
}
