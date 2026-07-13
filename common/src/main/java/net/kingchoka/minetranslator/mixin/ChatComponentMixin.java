package net.kingchoka.minetranslator.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.kingchoka.minetranslator.api.ChatComponentMixinAccessor;
import net.kingchoka.minetranslator.chat.ChatTranslationController;
import net.kingchoka.minetranslator.chat.ChatEntryStore;
import net.kingchoka.minetranslator.chat.ChatTranslationState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin implements ChatComponentMixinAccessor {
    @Unique
    private final int[] MineTranslator$messageIndexTrimmedToAll = Util.make(new int[100], arr -> Arrays.fill(arr, -1));

    @Shadow
    @Final
    private List<GuiMessage> allMessages;

    @Shadow
    protected abstract int getWidth();

    @Shadow
    protected abstract double getScale();

    @Shadow
    @Final
    Minecraft minecraft;

    @Shadow
    public abstract boolean isChatFocused();

    @Shadow
    protected abstract boolean isChatHidden();

    @Shadow
    public abstract int getLinesPerPage();

    @Shadow
    @Final
    private List<GuiMessage.Line> trimmedMessages;

    @Shadow
    private int chatScrollbarPos;

    @Shadow
    protected abstract int getLineHeight();

    @Shadow
    public abstract void addMessage(Component message, @Nullable MessageSignature signature, @Nullable GuiMessageTag tag);

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At("RETURN"))
    private void MineTranslator$onAddMessageTail(Component message, @Nullable MessageSignature signature, @Nullable GuiMessageTag tag, CallbackInfo ci) {
        if (!this.allMessages.isEmpty()) {
            GuiMessage guiMessage = this.allMessages.get(0);
            ChatTranslationController.getInstance().onChatMessageReceived(guiMessage, (ChatComponent) (Object) this);
        }
    }

    @Inject(method = "clearMessages(Z)V", at = @At("TAIL"))
    private void MineTranslator$afterClearMessages(boolean bl, CallbackInfo ci) {
        Arrays.fill(this.MineTranslator$messageIndexTrimmedToAll, -1);
    }

    @Inject(method = "refreshTrimmedMessages()V", at = @At("HEAD"))
    private void MineTranslator$beforeRefreshTrimmedMessages(CallbackInfo ci) {
        Arrays.fill(this.MineTranslator$messageIndexTrimmedToAll, -1);
    }

    @WrapOperation(method = "addMessageToDisplayQueue(Lnet/minecraft/client/GuiMessage;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/GuiMessage;splitLines(Lnet/minecraft/client/gui/Font;I)Ljava/util/List;"))
    private List<FormattedCharSequence> MineTranslator$wrapSplitLines(GuiMessage instance, Font font, int i, Operation<List<FormattedCharSequence>> original) {
        // Some client mods add chat messages from worker threads. Font layout
        // must only run on the render thread; rebuild the chat there instead.
        if (!this.minecraft.isSameThread()) {
            this.minecraft.execute(this::refreshTrimmedMessages);
            return List.of();
        }

        var state = ChatEntryStore.getInstance().getState(instance);
        List<FormattedCharSequence> toReturn;
        if (state != null && state.status == ChatTranslationState.TranslationStatus.TRANSLATED && state.translatedText != null) {
            toReturn = font.split(state.translatedText, i);
        } else {
            toReturn = original.call(instance, font, i);
        }
        
        int s = toReturn.size();
        for (int x = 99; x >= s; x--) {
            this.MineTranslator$messageIndexTrimmedToAll[x] = this.MineTranslator$messageIndexTrimmedToAll[x - s] + 1;
        }
        Arrays.fill(this.MineTranslator$messageIndexTrimmedToAll, 0, s, 0);
        return toReturn;
    }

    @Unique
    @Override
    @Nullable
    public String MineTranslator$getMessageContentAt(double globalMouseX, double globalMouseY) {
        double mouseX = this.MineTranslator$screenToChatX(globalMouseX);
        double mouseY = this.MineTranslator$screenToChatY(globalMouseY);
        int i = this.MineTranslator$getMessageLineIndexAt(mouseX, mouseY);
        if (i >= 0 && i < this.MineTranslator$messageIndexTrimmedToAll.length) {
            int idx = this.MineTranslator$messageIndexTrimmedToAll[i];
            if (idx >= 0 && idx < this.allMessages.size()) {
                return this.allMessages.get(idx).content().getString();
            }
        }
        return null;
    }

    @Unique
    @Override
    public int[] MineTranslator$getMessageIndexTrimmedToAll() {
        return MineTranslator$messageIndexTrimmedToAll;
    }

    @Unique
    @Override
    public double MineTranslator$screenToChatX(double x) {
        double guiX = x * this.minecraft.getWindow().getGuiScaledWidth()
            / (double) this.minecraft.getWindow().getScreenWidth();
        return guiX / this.getScale() - (double) 4.0F;
    }

    @Unique
    @Override
    public double MineTranslator$screenToChatY(double y) {
        double guiY = y * this.minecraft.getWindow().getGuiScaledHeight()
            / (double) this.minecraft.getWindow().getScreenHeight();
        double d = (double) this.minecraft.getWindow().getGuiScaledHeight() - guiY - (double) 40.0F;
        return d / (this.getScale() * (double) this.getLineHeight());
    }

    @Unique
    @Override
    public int MineTranslator$getMessageLineIndexAt(double mouseX, double mouseY) {
        if (this.isChatFocused() && !this.isChatHidden()) {
            if (!(mouseX < (double) -4.0F) && !(mouseX > (double) Mth.floor((double) this.getWidth() / this.getScale()))) {
                int i = Math.min(this.getLinesPerPage(), this.trimmedMessages.size());
                if (mouseY >= (double) 0.0F && mouseY < (double) i) {
                    int j = Mth.floor(mouseY + (double) this.chatScrollbarPos);
                    if (j >= 0 && j < this.trimmedMessages.size()) {
                        return j;
                    }
                }
            }
        }
        return -1;
    }

    @Shadow
    public abstract void refreshTrimmedMessages();

    @Unique
    @Override
    @Nullable
    public GuiMessage MineTranslator$getMessageAt(double globalMouseX, double globalMouseY) {
        double mouseX = this.MineTranslator$screenToChatX(globalMouseX);
        double mouseY = this.MineTranslator$screenToChatY(globalMouseY);
        int i = this.MineTranslator$getMessageLineIndexAt(mouseX, mouseY);
        if (i >= 0 && i < this.MineTranslator$messageIndexTrimmedToAll.length) {
            int idx = this.MineTranslator$messageIndexTrimmedToAll[i];
            if (idx >= 0 && idx < this.allMessages.size()) {
                return this.allMessages.get(idx);
            }
        }
        return null;
    }

    @Unique
    @Override
    public int MineTranslator$getMessageIndexAt(double globalMouseX, double globalMouseY) {
        double mouseX = this.MineTranslator$screenToChatX(globalMouseX);
        double mouseY = this.MineTranslator$screenToChatY(globalMouseY);
        int i = this.MineTranslator$getMessageLineIndexAt(mouseX, mouseY);
        if (i >= 0 && i < this.MineTranslator$messageIndexTrimmedToAll.length) {
            return this.MineTranslator$messageIndexTrimmedToAll[i];
        }
        return -1;
    }

    @Unique
    @Override
    public void MineTranslator$addMessageDirect(Component message, @Nullable MessageSignature signature, @Nullable GuiMessageTag tag) {
        this.addMessage(message, signature, tag);
    }

    @Unique
    @Override
    public void MineTranslator$refreshTrimmedMessages() {
        this.refreshTrimmedMessages();
    }
}
