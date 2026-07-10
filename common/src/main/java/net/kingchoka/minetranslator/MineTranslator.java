package net.kingchoka.minetranslator;

import net.kingchoka.minetranslator.compat.jade.MTCompatJade;
import net.kingchoka.minetranslator.config.MTConfig;
import net.kingchoka.minetranslator.core.TranslationKit;
import net.kingchoka.minetranslator.tool.CompatUtl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MineTranslator {
    public static final String ID = "minetranslator";
    public static final String NAME = "MineTranslator";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static net.kingchoka.minetranslator.compat.IMTCompat compat;

    private MineTranslator() {
        throw new AssertionError("MineTranslator should not be instantiated");
    }

    public static void setCompat(net.kingchoka.minetranslator.compat.IMTCompat value) {
        compat = value;
    }

    public static net.kingchoka.minetranslator.compat.IMTCompat getCompat() {
        return compat;
    }

    public static void init() {
        TranslationKit.init();
        MTConfig.init();
        if (CompatUtl.Jade.isLoaded()) {
            MTCompatJade.init();
        }
    }
}
