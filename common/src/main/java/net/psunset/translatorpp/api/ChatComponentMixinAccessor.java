package net.psunset.translatorpp.api;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jetbrains.annotations.Nullable;

/**
 * To access mixin methods in ChatComponent.
 */
public interface ChatComponentMixinAccessor {

    @Nullable String translatorpp$getMessageContentAt(double globalMouseX, double globalMouseY);
    int[] translatorpp$getMessageIndexTrimmedToAll();
    double translatorpp$screenToChatX(double x);
    double translatorpp$screenToChatY(double y);
    int translatorpp$getMessageLineIndexAt(double mouseX, double mouseY);

    @Nullable GuiMessage translatorpp$getMessageAt(double globalMouseX, double globalMouseY);
    int translatorpp$getMessageIndexAt(double globalMouseX, double globalMouseY);
    void translatorpp$addMessageDirect(Component message, @Nullable MessageSignature signature, @Nullable GuiMessageTag tag);
    void translatorpp$refreshTrimmedMessages();
}
