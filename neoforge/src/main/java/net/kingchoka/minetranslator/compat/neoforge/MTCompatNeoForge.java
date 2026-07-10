package net.kingchoka.minetranslator.compat.neoforge;

import net.neoforged.fml.common.Mod;
import net.kingchoka.minetranslator.MineTranslator;

@Mod("minetranslator-compat")
public class MTCompatNeoForge {
    public MTCompatNeoForge() {
        MineTranslator.setCompat(new MTCompatImpl());
    }
}
