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

        try {
            Class<?> keyEventClass = Class.forName("net.minecraft.client.input.KeyEvent");
            TranslationDebugLogger.info("KeyEvent class inspection:");
            for (var method : keyEventClass.getDeclaredMethods()) {
                TranslationDebugLogger.info("  method: {}", method.toString());
            }
            for (var field : keyEventClass.getDeclaredFields()) {
                TranslationDebugLogger.info("  field: {}", field.toString());
            }
        } catch (Exception e) {
            TranslationDebugLogger.error("Failed to inspect KeyEvent class: {}", e.toString());
        }

        ItemTooltipCallbacks.EVENT.register((stack, context, flag, lines) -> {
            TranslationDebugLogger.info("[MineTranslator v3] ItemTooltipCallback triggered for: {}", stack.getHoverName().getString());
            TooltipTranslationController.getInstance().onGetTooltip(stack, lines);
        });

        net.kingchoka.minetranslator.event.ScreenCallbacks.KEY_PRESSED_POST.register((screen, context) -> {
            TranslationDebugLogger.info("[MineTranslator v3] KEY_PRESSED_POST triggered: {}", context);
            boolean matchTranslate = MTKeyMappings.TRANSLATE_KEY.matches(context);
            TranslationDebugLogger.info("[MineTranslator v3] Match state: TRANSLATE_KEY={}", matchTranslate);
            if (matchTranslate) {
                net.kingchoka.minetranslator.tooltip.TooltipTranslationController.setTranslateKeyPressed(true);
                Minecraft client = Minecraft.getInstance();
                double mouseX = client.mouseHandler.xpos();
                double mouseY = client.mouseHandler.ypos();
                try {
                    GuiMessage msg = ((ChatComponentMixinAccessor) client.gui.getChat()).MineTranslator$getMessageAt(mouseX, mouseY);
                    if (msg != null) {
                        TranslationDebugLogger.chat("Manual translation triggered via screen keypress for message: {}", msg.content().getString());
                        ChatTranslationController.getInstance().translateMessage(msg, client.gui.getChat(), true);
                    } else {
                        TranslationDebugLogger.chat("KEY_PRESSED_POST matched, but no message under mouse at X={}, Y={}", mouseX, mouseY);
                    }
                } catch (Exception e) {
                    TranslationDebugLogger.chat("Failed to get chat message at X={}, Y={} in KEY_PRESSED_POST: {}", mouseX, mouseY, e.toString());
                }
            }
        });

        net.kingchoka.minetranslator.event.ScreenCallbacks.KEY_RELEASED_POST.register((screen, context) -> {
            boolean matchTranslate = MTKeyMappings.TRANSLATE_KEY.matches(context);
            if (matchTranslate) {
                net.kingchoka.minetranslator.tooltip.TooltipTranslationController.setTranslateKeyPressed(false);
            }
        });

        net.kingchoka.minetranslator.event.ScreenCallbacks.REMOVED.register((screen) -> {
            net.kingchoka.minetranslator.tooltip.TooltipTranslationController.setTranslateKeyPressed(false);
        });

        ClientTickCallbacks.POST.register(client -> {
            while (MTKeyMappings.CONFIG_KEY.consumeClick()) {
                try {
                    Class<?> screenClass = Class.forName("net.kingchoka.minetranslator.config.gui.MTConfigScreen");
                    var method = screenClass.getMethod("create", Screen.class);
                    Screen screen = (Screen) method.invoke(null, client.screen);
                    client.setScreen(screen);
                } catch (Exception e) {
                    TranslationDebugLogger.error("Failed to open config screen: {}", e.toString());
                }
            }

            while (MTKeyMappings.TRANSLATE_KEY.consumeClick()) {
                if (client.player == null) continue;
                double mouseX = client.mouseHandler.xpos();
                double mouseY = client.mouseHandler.ypos();
                GuiMessage msg = ((ChatComponentMixinAccessor) client.gui.getChat()).MineTranslator$getMessageAt(mouseX, mouseY);
                if (msg != null) {
                    TranslationDebugLogger.chat("Manual translation triggered via client tick for message: {}", msg.content().getString());
                    ChatTranslationController.getInstance().translateMessage(msg, client.gui.getChat(), true);
                }
            }
        });
    }
}
