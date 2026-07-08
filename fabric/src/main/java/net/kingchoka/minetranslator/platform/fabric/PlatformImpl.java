package net.kingchoka.minetranslator.platform.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.kingchoka.minetranslator.platform.IPlatform;
import net.kingchoka.minetranslator.platform.Platform;

public final class PlatformImpl implements IPlatform {

    static {
        Platform._innerImpl = new PlatformImpl();
    }

    private PlatformImpl() {
    }

    @Override
    public boolean isNeoForge() {
        return false;
    }

    @Override
    public boolean isFabric() {
        return true;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    /**
     * Does nothing but simply run the code in static field
     */
    public static void init() {
    }
}
