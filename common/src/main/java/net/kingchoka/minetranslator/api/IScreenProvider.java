package net.kingchoka.minetranslator.api;

import net.minecraft.client.gui.screens.Screen;

/**
 * Obviously, a {@link Screen} provider.
 * Provides a MineTranslator screen without depending on an external config library.
 */
public interface IScreenProvider {
    /**
     * Create a {@link Screen}.
     * @param parent Often used to go back to the original screen.
     */
    Screen createScreen(Screen parent);
}
