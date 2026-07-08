package net.kingchoka.minetranslator.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.kingchoka.minetranslator.api.ChatComponentMixinAccessor;
import net.kingchoka.minetranslator.api.ChatScreenMixinAccessor;
import net.kingchoka.minetranslator.core.TranslationKit;
import net.kingchoka.minetranslator.keybind.MTKeyMappings;
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
    private boolean MineTranslator$sendingDirectly = false;

    protected ChatScreenMixin(Component component) {
        super(component);
    }

    @Override
    @Unique
    public void MineTranslator$sendChatDirectly(String text, boolean addToHistory) {
        this.MineTranslator$sendingDirectly = true;
        try {
            ((ChatScreen) (Object) this).handleChatInput(text, addToHistory);
        } finally {
            this.MineTranslator$sendingDirectly = false;
        }
    }

    @Override
    @Unique
    public EditBox MineTranslator$getInput() {
        return this.input;
    }

    @Inject(method = "handleChatInput(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    private void MineTranslator$onHandleChatInput(String string, boolean addToHistory, CallbackInfo ci) {
        if (this.MineTranslator$sendingDirectly) {
            return;
        }

        if (TranslationKit.getInstance().onSendChatMessage(string, (ChatScreen) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z", at = @At("HEAD"), cancellable = true)
    private void MineTranslator$onKeyPressed(net.minecraft.client.input.KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (MTKeyMappings.TRANSLATE_INPUT_KEY.matches(keyEvent)) {
            TranslationKit.getInstance().translateInputWithContext(this.input);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void MineTranslator$onRender(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ChatComponentMixinAccessor accessor = (ChatComponentMixinAccessor) this.minecraft.gui.getChat();
        String text = accessor.MineTranslator$getMessageContentAt(i, j);
        TranslationKit.getInstance().setHoveredText(text);

        if (!TranslationKit.getInstance().isKeyDown()) {
            GuiMessage msg = accessor.MineTranslator$getMessageAt(i, j);
            int idx = accessor.MineTranslator$getMessageIndexAt(i, j);
            TranslationKit.getInstance().updateHoveredChatMessage(msg, idx);
        }
    }
}
