package net.kingchoka.minetranslatorport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

final class GoogleTranslator {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3, runnable -> {
        Thread thread = new Thread(runnable, "MineTranslator-Port-Worker");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicBoolean SUCCESS_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean ERROR_LOGGED = new AtomicBoolean();

    private GoogleTranslator() {}

    static CompletableFuture<String> translate(String text, String source, String target) {
        return CompletableFuture.supplyAsync(() -> {
            if (text == null || text.trim().isEmpty()) return text;
            HttpURLConnection connection = null;
            try {
                String endpoint = "https://translate.googleapis.com/translate_a/single?dt=t&client=gtx&q="
                    + URLEncoder.encode(text, "UTF-8") + "&sl=" + source + "&tl=" + target;
                connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(6000);
                connection.setReadTimeout(6000);
                connection.setRequestProperty("User-Agent", "MineTranslator-MultiVersion");
                int status = connection.getResponseCode();
                if (status < 200 || status >= 300) {
                    logErrorOnce("HTTP " + status);
                    return text;
                }

                StringBuilder json = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                try {
                    String line;
                    while ((line = reader.readLine()) != null) json.append(line);
                } finally {
                    reader.close();
                }

                JsonArray root = new JsonParser().parse(json.toString()).getAsJsonArray();
                JsonArray segments = root.get(0).getAsJsonArray();
                StringBuilder result = new StringBuilder();
                for (JsonElement segment : segments) {
                    JsonArray values = segment.getAsJsonArray();
                    if (values.size() > 0 && !values.get(0).isJsonNull()) result.append(values.get(0).getAsString());
                }
                if (result.length() == 0) {
                    logErrorOnce("empty response");
                    return text;
                }
                if (SUCCESS_LOGGED.compareAndSet(false, true)) {
                    System.out.println("[MineTranslator] Google translation connection is ready");
                }
                return result.toString();
            } catch (Exception exception) {
                logErrorOnce(exception.getClass().getSimpleName());
                return text;
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, EXECUTOR);
    }

    private static void logErrorOnce(String reason) {
        if (ERROR_LOGGED.compareAndSet(false, true)) {
            System.out.println("[MineTranslator] Translation request failed safely: " + reason);
        }
    }
}
