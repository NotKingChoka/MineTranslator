package net.kingchoka.minetranslatorport;

import net.kingchoka.mtlib.api.MTLibrary;
import net.kingchoka.mtlib.api.MTLibraryEntrypoint;

public final class LibraryIntegration implements MTLibraryEntrypoint {
    @Override
    public void registerHooks() {
        final PortController controller = PortController.get();
        controller.initialize();
        MTLibrary.onTick(controller::onTick);
        MTLibrary.onKey(controller::onKey);
        MTLibrary.onChat(controller::onChat);
        MTLibrary.onTooltip(controller::onTooltip);
    }
}
