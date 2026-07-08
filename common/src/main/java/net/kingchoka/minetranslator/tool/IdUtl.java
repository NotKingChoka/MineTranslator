package net.kingchoka.minetranslator.tool;

import net.minecraft.resources.Identifier;
import net.kingchoka.minetranslator.MineTranslator;

public final class IdUtl {
    /**
     * Create a Identifier with MineTranslator's namespace.
     */
    public static Identifier of(String name) {
        return Identifier.fromNamespaceAndPath(MineTranslator.ID, name);
    }

    /**
     * Create a Identifier with the vanilla, {@code Minecraft}, namespace.
     */
    public static Identifier ofVanilla(String name) {
        return Identifier.withDefaultNamespace(name);
    }
}
