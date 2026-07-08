package net.psunset.translatorpp.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.psunset.translatorpp.api.ChatComponentMixinAccessor;
import net.psunset.translatorpp.api.ChatScreenMixinAccessor;
import net.psunset.translatorpp.core.TranslationKit;
import net.psunset.translatorpp.keybind.TPPKeyMappings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen implements ChatScreenMixinAccessor {
    @Shadow
    protected EditBox input;

    @Unique
    private boolean translatorpp$sendingDirectly = false;

    protected ChatScreenMixin(Component component) {
        super(component);
    }

    @Override
    @Unique
    public void translatorpp$sendChatDirectly(String text, boolean addToHistory) {
        this.translatorpp$sendingDirectly = true;
        try {
            ((ChatScreen) (Object) this).handleChatInput(text, addToHistory);
        } finally {
            this.translatorpp$sendingDirectly = false;
        }
    }

    @Override
    @Unique
    public EditBox translatorpp$getInput() {
        return this.input;
    }

    @Inject(method = "handleChatInput(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    private void translatorpp$onHandleChatInput(String string, boolean addToHistory, CallbackInfo ci) {
        if (this.translatorpp$sendingDirectly) {
            return;
        }

        if (TranslationKit.getInstance().onSendChatMessage(string, (ChatScreen) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z", at = @At("HEAD"), cancellable = true)
    private void translatorpp$onKeyPressed(net.minecraft.client.input.KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (TPPKeyMappings.TRANSLATE_INPUT_KEY.matches(keyEvent)) {
            TranslationKit.getInstance().translateInputWithContext(this.input);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void translatorpp$onRender(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ChatComponentMixinAccessor accessor = (ChatComponentMixinAccessor) this.minecraft.gui.getChat();
        String text = accessor.translatorpp$getMessageContentAt(i, j);
        TranslationKit.getInstance().setHoveredText(text);

        if (!TranslationKit.getInstance().isKeyDown()) {
            GuiMessage msg = accessor.translatorpp$getMessageAt(i, j);
            int idx = accessor.translatorpp$getMessageIndexAt(i, j);
            TranslationKit.getInstance().updateHoveredChatMessage(msg, idx);
        }
    }
}
