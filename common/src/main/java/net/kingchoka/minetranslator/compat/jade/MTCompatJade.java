package net.kingchoka.minetranslator.compat.jade;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.kingchoka.minetranslator.core.TranslationKit;
import net.kingchoka.minetranslator.event.ClientTickCallbacks;
import net.kingchoka.minetranslator.keybind.MTKeyMappings;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class MTCompatJade implements IWailaPlugin {

    public static void init() {
        ClientTickCallbacks.POST.register(client -> {
            if (Minecraft.getInstance().screen == null) {
                if (MTKeyMappings.TRANSLATE_KEY.isDown()) {
                    TranslationKit.getInstance().start(client);
                } else {
                    TranslationKit.getInstance().stop();
                }
            }
        });
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        IWailaPlugin.super.registerClient(registration);

        registration.registerBlockComponent(MTJadeExtension.BLOCK, Block.class);
        registration.registerEntityComponent(MTJadeExtension.ENTITY, Entity.class);
    }
}
