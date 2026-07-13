package net.kingchoka.minetranslator.translation.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kingchoka.minetranslator.config.ModConfig;
import net.kingchoka.minetranslator.translation.TranslationProvider;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class GeminiProvider implements TranslationProvider {
    private final Supplier<String> apiKeySupplier;
    public GeminiProvider() { this(() -> ModConfig.getInstance().apiKey); }
    public GeminiProvider(Supplier<String> apiKeySupplier) { this.apiKeySupplier = apiKeySupplier; }
    @Override
    public String translate(String text, String sourceLang, String targetLang) throws Exception {
        String apiKey = apiKeySupplier.get();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is not configured.");
        }

        String prompt = String.format("Translate the following text from %s to %s. Respond ONLY with the translation, no conversational filler, no markdown formatting, no quotes.\n\nText: %s",
            sourceLang, targetLang, text);

        JsonObject payload = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject contentObj = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject partObj = new JsonObject();
        partObj.addProperty("text", prompt);
        parts.add(partObj);
        contentObj.add("parts", parts);
        contents.add(contentObj);
        payload.add("contents", contents);

        String fullUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        URL url = URI.create(fullUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);

            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new Exception("Gemini API returned HTTP status: " + code);
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }

            JsonObject resp = JsonParser.parseString(sb.toString()).getAsJsonObject();
            return resp.getAsJsonArray("candidates").get(0).getAsJsonObject()
                .getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject()
                .get("text").getAsString().trim();
        } finally {
            conn.disconnect();
        }
    }
}
