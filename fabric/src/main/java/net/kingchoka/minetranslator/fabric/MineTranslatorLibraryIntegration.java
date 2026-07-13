package net.kingchoka.minetranslator.fabric;

import net.kingchoka.minetranslator.event.ClientTickCallbacks;
import net.kingchoka.minetranslator.event.ItemTooltipCallbacks;
import net.kingchoka.minetranslator.event.ScreenCallbacks;
import net.kingchoka.minetranslator.keybind.MTKeyMappings;
import net.kingchoka.minetranslatorlib.api.MineTranslatorLibrary;
import net.kingchoka.minetranslatorlib.api.MineTranslatorLibraryEntrypoint;

/** Connects MineTranslator's platform-neutral events to the separate library mod. */
public final class MineTranslatorLibraryIntegration implements MineTranslatorLibraryEntrypoint {
    @Override
    public void registerLibraryHooks() {
        MineTranslatorLibrary.onEndClientTick(
            client -> ClientTickCallbacks.POST.getInvoker().afterTick(client)
        );
        MineTranslatorLibrary.onItemTooltip(
            (stack, context, flag, lines) ->
                ItemTooltipCallbacks.EVENT.getInvoker().getTooltip(stack, context, flag, lines)
        );
        MineTranslatorLibrary.onScreenKeyPressed(
            (screen, event) -> ScreenCallbacks.KEY_PRESSED_POST.getInvoker().afterKeyPress(screen, event)
        );
        MineTranslatorLibrary.onScreenRemoved(
            screen -> ScreenCallbacks.REMOVED.getInvoker().onRemove(screen)
        );
        MineTranslatorLibrary.registerKeyMappings(MTKeyMappings.getEntries());
        MineTranslatorLibrary.registerResourcePack(
            "minetranslator",
            "assets/minetranslator/lang/en_us.json"
        );
    }
}
