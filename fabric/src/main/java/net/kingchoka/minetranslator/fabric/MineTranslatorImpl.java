package net.kingchoka.minetranslator.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.kingchoka.minetranslator.MineTranslator;
import net.kingchoka.minetranslator.event.fabric.MTEventsImpl;
import net.kingchoka.minetranslator.keybind.fabric.MTKeyMappingsImpl;
import net.kingchoka.minetranslator.platform.fabric.PlatformImpl;

public final class MineTranslatorImpl implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        PlatformImpl.init();

        /* Earlier */

        MineTranslator.init();
        MTKeyMappingsImpl.init();

        /* Later */

        MTEventsImpl.init();
    }
}
