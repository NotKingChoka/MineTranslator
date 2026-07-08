package net.kingchoka.minetranslator.keybind;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.kingchoka.minetranslator.tool.IdUtl;
import org.lwjgl.glfw.GLFW;

public final class MTKeyMappings {

    private static final ImmutableSet.Builder<KeyMapping> ENTRIES = ImmutableSet.builder();

    public static final KeyMapping TRANSLATE_KEY = register(
            "key.MineTranslator.translate",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_T,
            Categories.GENERAL
    );

    public static final KeyMapping TRANSLATE_INPUT_KEY = register(
            "key.MineTranslator.translate_input",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            Categories.GENERAL
    );

    public static final KeyMapping CONFIG_KEY = register(
            "key.MineTranslator.config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            Categories.GENERAL
    );

    public static final KeyMapping TRANSLATE_ITEM_KEY = register(
            "key.MineTranslator.translate_item",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            Categories.GENERAL
    );

    public static final KeyMapping FAST_TRANSLATE_KEY = register(
            "key.MineTranslator.fast_translate",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            Categories.GENERAL
    );

    public static final KeyMapping TOGGLE_AUTO_CHAT_KEY = register(
            "key.MineTranslator.toggle_auto_chat",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            Categories.GENERAL
    );

    public static final KeyMapping TOGGLE_AUTO_NPC_KEY = register(
            "key.MineTranslator.toggle_auto_npc",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            Categories.GENERAL
    );

    public static final KeyMapping TOGGLE_AUTO_ITEMS_KEY = register(
            "key.MineTranslator.toggle_auto_items",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            Categories.GENERAL
    );

    private static KeyMapping register(String translation, InputConstants.Type type, int keyCode, KeyMapping.Category category) {
        var key = new KeyMapping(translation, type, keyCode, category);
        ENTRIES.add(key);
        return key;
    }

    private static KeyMapping register(String translation, int keyCode, KeyMapping.Category category) {
        var key = new KeyMapping(translation, keyCode, category);
        ENTRIES.add(key);
        return key;
    }

    public static ImmutableSet<KeyMapping> getEntries() {
        return ENTRIES.build();
    }

    interface Categories {
        KeyMapping.Category GENERAL = KeyMapping.Category.register(IdUtl.of("general"));
    }
}
