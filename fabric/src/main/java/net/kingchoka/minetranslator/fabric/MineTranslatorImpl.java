package net.kingchoka.minetranslator.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.kingchoka.minetranslator.MineTranslator;
import net.kingchoka.minetranslator.platform.fabric.PlatformImpl;
import net.kingchoka.minetranslator.config.gui.test.UiVisualTest;

public final class MineTranslatorImpl implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        PlatformImpl.init();

        /* Earlier */

        MineTranslator.init();
        UiVisualTest.init();
    }
}
