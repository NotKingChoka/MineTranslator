package net.kingchoka.minetranslator.api;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jetbrains.annotations.Nullable;

/**
 * To access mixin methods in ChatComponent.
 */
public interface ChatComponentMixinAccessor {

    @Nullable String MineTranslator$getMessageContentAt(double globalMouseX, double globalMouseY);
    int[] MineTranslator$getMessageIndexTrimmedToAll();
    double MineTranslator$screenToChatX(double x);
    double MineTranslator$screenToChatY(double y);
    int MineTranslator$getMessageLineIndexAt(double mouseX, double mouseY);

    @Nullable GuiMessage MineTranslator$getMessageAt(double globalMouseX, double globalMouseY);
    int MineTranslator$getMessageIndexAt(double globalMouseX, double globalMouseY);
    void MineTranslator$addMessageDirect(Component message, @Nullable MessageSignature signature, @Nullable GuiMessageTag tag);
    void MineTranslator$refreshTrimmedMessages();
}
