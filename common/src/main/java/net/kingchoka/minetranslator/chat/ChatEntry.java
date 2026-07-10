package net.kingchoka.minetranslator.chat;

import net.minecraft.network.chat.Component;

public record ChatEntry(
    long id,
    Component originalText,
    String originalPlainText,
    long receivedAt
) {}
