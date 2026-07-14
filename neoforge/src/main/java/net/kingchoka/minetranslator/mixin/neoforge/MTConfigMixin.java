package net.kingchoka.minetranslator.mixin.neoforge;

import net.kingchoka.minetranslator.MineTranslator;
import net.kingchoka.minetranslator.config.MTConfig;
import net.kingchoka.minetranslator.config.neoforge.MTConfigImplNeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MTConfig.class)
public interface MTConfigMixin {

    @Overwrite
    static void init() {
        MineTranslator.LOGGER.debug("NeoForge is loaded, using neoforge for MineTranslator Config.");
        MTConfig.Dummy.INSTANCE = MTConfigImplNeoForge.INSTANCE;
        // init is completed in mod constructor, no need to call here
//        MTConfigImplNeoForge.init(container);
    }
}
