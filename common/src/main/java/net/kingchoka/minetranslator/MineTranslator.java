package net.kingchoka.minetranslator;

import net.kingchoka.minetranslator.chat.ChatTranslationController;
import net.kingchoka.minetranslator.chat.PlayerMessageParser;
import net.kingchoka.minetranslator.config.ModConfig;
import net.kingchoka.minetranslator.debug.TranslationDebugLogger;
import net.kingchoka.minetranslator.event.ClientTickCallbacks;
import net.kingchoka.minetranslator.event.ItemTooltipCallbacks;
import net.kingchoka.minetranslator.keybind.MTKeyMappings;
import net.kingchoka.minetranslator.tooltip.TooltipTranslationController;
import net.kingchoka.minetranslator.api.ChatComponentMixinAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.screens.Screen;

public final class MineTranslator {
    public static final String ID = "minetranslator";
    public static final String NAME = "MineTranslator";

    private MineTranslator() {
        throw new AssertionError("MineTranslator should not be instantiated");
    }

    public static void init() {
        ModConfig.load();
        ModConfig config = ModConfig.getInstance();

        TranslationDebugLogger.info("[MineTranslator v3] Version: 3.0.0-alpha.1");
        TranslationDebugLogger.info("[MineTranslator v3] Build timestamp: " + new java.util.Date().toString());
        TranslationDebugLogger.info("[MineTranslator v3] Active ChatHud hook: ChatComponentMixin");
        TranslationDebugLogger.info("[MineTranslator v3] Number of registered chat interceptors: 1");
        TranslationDebugLogger.info("[MineTranslator v3] Tooltip hook: registered");
        TranslationDebugLogger.info("[MineTranslator v3] Provider: " + config.provider);
        TranslationDebugLogger.info("[MineTranslator v3] Target language: " + config.targetLanguage);

        PlayerMessageParser.runParserTests();

        ItemTooltipCallbacks.EVENT.register((stack, context, flag, lines) -> {
            TooltipTranslationController.getInstance().onGetTooltip(stack, lines);
        });

        ClientTickCallbacks.POST.register(client -> {
            if (client.player == null) return;

            if (MTKeyMappings.CONFIG_KEY.consumeClick()) {
                try {
                    Class<?> screenClass = Class.forName("net.kingchoka.minetranslator.config.gui.MTConfigScreen");
                    var method = screenClass.getMethod("create", Screen.class);
                    Screen screen = (Screen) method.invoke(null, client.screen);
                    client.setScreen(screen);
                } catch (Exception e) {
                    TranslationDebugLogger.error("Failed to open config screen: {}", e.toString());
                }
            }

            if (MTKeyMappings.TRANSLATE_KEY.consumeClick()) {
                double mouseX = client.mouseHandler.xpos();
                double mouseY = client.mouseHandler.ypos();
                GuiMessage msg = ((ChatComponentMixinAccessor) client.gui.getChat()).MineTranslator$getMessageAt(mouseX, mouseY);
                if (msg != null) {
                    TranslationDebugLogger.chat("Manual translation triggered for message: {}", msg.content().getString());
                    ChatTranslationController.getInstance().translateMessage(msg, client.gui.getChat(), true);
                }
            }
        });
    }
}
