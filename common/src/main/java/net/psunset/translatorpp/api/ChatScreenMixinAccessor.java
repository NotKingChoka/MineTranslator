package net.psunset.translatorpp.api;

import net.minecraft.client.gui.components.EditBox;

public interface ChatScreenMixinAccessor {
    void translatorpp$sendChatDirectly(String text, boolean addToHistory);
    EditBox translatorpp$getInput();
}

