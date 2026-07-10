package net.kingchoka.minetranslator.tooltip;

import net.minecraft.network.chat.Component;

public record TooltipEntry(
    String itemId,
    Component originalComponent,
    Component translatedComponent
) {}
