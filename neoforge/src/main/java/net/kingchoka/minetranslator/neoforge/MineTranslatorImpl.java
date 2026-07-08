package net.kingchoka.minetranslator.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.kingchoka.minetranslator.MineTranslator;
import net.kingchoka.minetranslator.config.neoforge.MTConfigImplNeoForge;
import net.kingchoka.minetranslator.event.neoforge.MTEventsImpl;
import net.kingchoka.minetranslator.platform.neoforge.PlatformImpl;

@Mod(value = MineTranslator.ID, dist = Dist.CLIENT)
public final class MineTranslatorImpl {

    public MineTranslatorImpl(ModContainer container, IEventBus modBus, Dist dist) {
        IEventBus gameBus = NeoForge.EVENT_BUS;

        PlatformImpl.init();

        /* Earlier */

        MineTranslator.init();
        MTConfigImplNeoForge.init(container);

        /* Later */

        MTEventsImpl.init(gameBus, modBus);
    }
}
