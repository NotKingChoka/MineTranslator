package net.kingchoka.minetranslator.debug;

import net.kingchoka.minetranslator.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslationDebugLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("MineTranslator-v3");

    public static void chat(String message, Object... args) {
        if (ModConfig.getInstance().debugLogging) {
            LOGGER.info("[MineTranslator v3][CHAT] " + message, args);
        }
    }

    public static void tooltip(String message, Object... args) {
        if (ModConfig.getInstance().debugLogging) {
            LOGGER.info("[MineTranslator v3][TOOLTIP] " + message, args);
        }
    }

    public static void info(String message, Object... args) {
        LOGGER.info("[MineTranslator v3] " + message, args);
    }

    public static void warn(String message, Object... args) {
        LOGGER.warn("[MineTranslator v3] " + message, args);
    }

    public static void error(String message, Object... args) {
        LOGGER.error("[MineTranslator v3] " + message, args);
    }
}
