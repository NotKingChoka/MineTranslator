package net.kingchoka.minetranslator.api;

import net.minecraft.client.gui.components.EditBox;

public interface ChatScreenMixinAccessor {
    void MineTranslator$sendChatDirectly(String text, boolean addToHistory);
    EditBox MineTranslator$getInput();
}

