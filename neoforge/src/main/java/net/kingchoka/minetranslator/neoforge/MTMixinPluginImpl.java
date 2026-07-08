package net.kingchoka.minetranslator.neoforge;

import net.kingchoka.minetranslator.TPPMixinPlugin;
import net.kingchoka.minetranslator.platform.neoforge.PlatformImpl;

public class MTMixinPluginImpl extends TPPMixinPlugin {
    static {
        PlatformImpl.init();
    }
}
