package net.kingchoka.minetranslator.keybind.fabric;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.kingchoka.minetranslator.keybind.MTKeyMappings;

public final class MTKeyMappingsImpl {

    public static void init() {
        for (var key : MTKeyMappings.getEntries()) {
            KeyBindingHelper.registerKeyBinding(key);
        }
    }
}
