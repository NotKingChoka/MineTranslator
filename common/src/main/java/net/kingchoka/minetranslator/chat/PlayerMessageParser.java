package net.kingchoka.minetranslator.chat;

import net.minecraft.client.Minecraft;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlayerMessageParser {

    private static final Pattern HYPIXEL_MESSAGE = Pattern.compile(
        "(?:^|.*\\s)\\[(?<head>[A-Za-z0-9_]{1,16}) head\\](?<username>[A-Za-z0-9_]{1,16}):\\s*(?<body>.*)$"
    );

    private static final Pattern ANGLE_MESSAGE = Pattern.compile(
        "^<(?<username>[A-Za-z0-9_]{1,16})>\\s*(?<body>.+)$"
    );

    private static final Pattern SIMPLE_MESSAGE = Pattern.compile(
        "^(?<username>[A-Za-z0-9_]{1,16})\\s*[:\\u00BB\\u25B6]\\s*(?<body>.+)$"
    );

    private PlayerMessageParser() {}

    public record PlayerParseResult(
        boolean success,
        String username,
        String body,
        int prefixEndIndex,
        int bodyStartIndex
    ) {
        public static PlayerParseResult failure() {
            return new PlayerParseResult(false, null, null, -1, -1);
        }
    }

    public static PlayerParseResult parse(String fullString) {
        if (fullString == null || fullString.isBlank()) {
            return PlayerParseResult.failure();
        }

        Matcher matcher = HYPIXEL_MESSAGE.matcher(fullString);
        if (matcher.matches() && matcher.group("head").equals(matcher.group("username"))) {
            // Keep levels, ranks and the player-head component before the nick.
            return fromMatcher(matcher, matcher.start("username"));
        }

        matcher = ANGLE_MESSAGE.matcher(fullString);
        if (matcher.matches()) {
            return fromMatcher(matcher, 0);
        }

        matcher = SIMPLE_MESSAGE.matcher(fullString);
        if (matcher.matches()) {
            return fromMatcher(matcher, 0);
        }

        int separator = findSeparator(fullString);
        if (separator < 0) {
            return PlayerParseResult.failure();
        }

        String header = fullString.substring(0, separator).trim();
        String[] words = header.split("\\s+");
        if (words.length == 0) {
            return PlayerParseResult.failure();
        }

        String candidate = words[words.length - 1]
            .replaceAll("^[^A-Za-z0-9_]+|[^A-Za-z0-9_]+$", "");
        if (!candidate.matches("^[A-Za-z0-9_]{1,16}$")) {
            return PlayerParseResult.failure();
        }

        String lowerHeader = header.toLowerCase(Locale.ROOT);
        boolean privateMessage = lowerHeader.contains("from")
            || lowerHeader.contains("to")
            || lowerHeader.contains("от")
            || lowerHeader.contains("для");
        if (!isPlayerInTabList(candidate) && !privateMessage) {
            return PlayerParseResult.failure();
        }

        int usernameStart = fullString.lastIndexOf(candidate, separator);
        int bodyStart = separator + 1;
        while (bodyStart < fullString.length() && Character.isWhitespace(fullString.charAt(bodyStart))) {
            bodyStart++;
        }
        if (usernameStart < 0 || bodyStart >= fullString.length()) {
            return PlayerParseResult.failure();
        }

        return new PlayerParseResult(
            true,
            candidate,
            fullString.substring(bodyStart),
            usernameStart,
            bodyStart
        );
    }

    private static PlayerParseResult fromMatcher(Matcher matcher, int prefixEndIndex) {
        return new PlayerParseResult(
            true,
            matcher.group("username"),
            matcher.group("body"),
            prefixEndIndex,
            matcher.start("body")
        );
    }

    private static int findSeparator(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ':' || c == '\u00BB' || c == '\u25B6') {
                return i;
            }
        }
        return -1;
    }

    private static boolean isPlayerInTabList(String username) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return false;
        }
        return connection.getOnlinePlayers().stream()
            .anyMatch(player -> player.getProfile().name().equalsIgnoreCase(username));
    }
}
