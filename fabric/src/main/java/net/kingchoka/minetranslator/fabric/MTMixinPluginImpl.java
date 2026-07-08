package net.kingchoka.minetranslator.fabric;

import net.kingchoka.minetranslator.TPPMixinPlugin;
import net.kingchoka.minetranslator.platform.fabric.PlatformImpl;

public class MTMixinPluginImpl extends TPPMixinPlugin {
    static {
        PlatformImpl.init();
    }
}
