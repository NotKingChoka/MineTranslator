package net.kingchoka.minetranslator.keybind.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.kingchoka.minetranslator.MineTranslator;
import net.kingchoka.minetranslator.keybind.MTKeyMappings;

@EventBusSubscriber(modid = MineTranslator.ID, value = Dist.CLIENT)
public final class MTKeyMappingsImpl {

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (var key : MTKeyMappings.getEntries()) {
            event.register(key);
        }
    }
}
