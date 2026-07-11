package net.kingchoka.minetranslator.chat;

import net.kingchoka.minetranslator.debug.TranslationDebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerMessageParser {

    private static final Pattern HYPIXEL_MESSAGE = Pattern.compile(
        "(?:^|.*\\s)\\[(?<head>[A-Za-z0-9_]{1,16}) head\\](?<username>[A-Za-z0-9_]{1,16}):\\s*(?<body>.*)$"
    );

    public record PlayerParseResult(
        boolean success,
        String username,
        String body,
        int bodyStartIndex
    ) {
        public static PlayerParseResult failure() {
            return new PlayerParseResult(false, null, null, -1);
        }
    }

    public static PlayerParseResult parse(String fullString) {
        if (fullString == null || fullString.isBlank()) {
            return PlayerParseResult.failure();
        }

        // 1. Hypixel head-marker parse
        Matcher matcher = HYPIXEL_MESSAGE.matcher(fullString);
        if (matcher.matches()) {
            String head = matcher.group("head");
            String username = matcher.group("username");
            String body = matcher.group("body");
            if (head.equals(username)) {
                int bodyStartIndex = fullString.indexOf(body);
                return new PlayerParseResult(true, username, body, bodyStartIndex);
            }
        }

        // 2. Fallback parser: Find the first colon/arrow separator
        int sepIdx = -1;
        for (int i = 0; i < fullString.length(); i++) {
            char c = fullString.charAt(i);
            if (c == ':' || c == '»' || c == '▶') {
                sepIdx = i;
                break;
            }
        }

        if (sepIdx != -1) {
            String header = fullString.substring(0, sepIdx).trim();
            String[] words = header.split("\\s+");
            if (words.length > 0) {
                String rawCandidate = words[words.length - 1].trim();
                String candidate = rawCandidate.replaceAll("[\\[\\]\\(\\)\\{\\}]", "").trim();
                
                if (candidate.matches("^[a-zA-Z0-9_]{3,16}$")) {
                    boolean isLocalPlayer = isPlayerInTabList(candidate);
                    boolean isPrivateMessage = header.toLowerCase(Locale.ROOT).contains("from") || 
                                               header.toLowerCase(Locale.ROOT).contains("to") || 
                                               header.contains("сообщение");
                    
                    if (isLocalPlayer || isPrivateMessage) {
                        int candidateIdx = fullString.lastIndexOf(rawCandidate, sepIdx);
                        if (candidateIdx != -1) {
                            int restIdx = sepIdx + 1;
                            while (restIdx < fullString.length() && Character.isWhitespace(fullString.charAt(restIdx))) {
                                restIdx++;
                            }
                            String body = fullString.substring(restIdx);
                            return new PlayerParseResult(true, candidate, body, restIdx);
                        }
                    }
                }
            }
        }

        return PlayerParseResult.failure();
    }

    private static boolean isPlayerInTabList(String username) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            java.util.Collection<net.minecraft.client.multiplayer.PlayerInfo> players = connection.getOnlinePlayers();
            for (net.minecraft.client.multiplayer.PlayerInfo player : players) {
                if (player.getProfile().name().equalsIgnoreCase(username)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void runParserTests() {
        TranslationDebugLogger.info("Running PlayerMessageParser unit tests...");
        String[] testStrings = {
            "[3] [VIP] [inkobink head]inkobink: как получить эти цветные монеты над своим именем?",
            "[421] ⛃ [MVP++] [Frank_lol_ head]Frank_lol_: selling for lbin - tax :D",
            "[5oulKeeper head]5oulKeeper: you feel sad",
            "[VIP] [Endersalt head]Endersalt: eating blues at map frfr",
            "[PP4L head]PP4L: where ru kvon"
        };
        String[] expectedUsers = {
            "inkobink", "Frank_lol_", "5oulKeeper", "Endersalt", "PP4L"
        };
        String[] expectedBodies = {
            "как получить эти цветные монеты над своим именем?",
            "selling for lbin - tax :D",
            "you feel sad",
            "eating blues at map frfr",
            "where ru kvon"
        };
        
        int passed = 0;
        for (int i = 0; i < testStrings.length; i++) {
            PlayerParseResult res = parse(testStrings[i]);
            boolean matches = res.success() && res.username().equals(expectedUsers[i]) && res.body().equals(expectedBodies[i]);
            if (matches) {
                passed++;
                TranslationDebugLogger.info("Test {} PASSED: User: '{}', Body: '{}'", i + 1, res.username(), res.body());
            } else {
                TranslationDebugLogger.error("Test {} FAILED!", i + 1);
                TranslationDebugLogger.error("  Input:    {}", testStrings[i]);
                TranslationDebugLogger.error("  Expected: User: '{}', Body: '{}'", expectedUsers[i], expectedBodies[i]);
                TranslationDebugLogger.error("  Actual:   Success: {}, User: '{}', Body: '{}'", res.success(), res.username(), res.body());
            }
        }
        TranslationDebugLogger.info("Parser tests completed. Passed: {}/{}", passed, testStrings.length);
    }
}
