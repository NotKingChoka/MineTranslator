package net.kingchoka.minetranslator.mixin;

import net.kingchoka.minetranslator.api.ChatScreenMixinAccessor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin implements ChatScreenMixinAccessor {
    @Shadow protected EditBox input;

    @Override
    public EditBox MineTranslator$getInput() {
        return input;
    }

    @Override
    public void MineTranslator$sendChatDirectly(String text, boolean addToHistory) {
        ((ChatScreen) (Object) this).handleChatInput(text, addToHistory);
    }
}
