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

public class OpenAIProvider implements TranslationProvider {
    private final Supplier<String> apiKeySupplier;
    public OpenAIProvider() { this(() -> ModConfig.getInstance().apiKey); }
    public OpenAIProvider(Supplier<String> apiKeySupplier) { this.apiKeySupplier = apiKeySupplier; }
    @Override
    public String translate(String text, String sourceLang, String targetLang) throws Exception {
        String apiKey = apiKeySupplier.get();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured.");
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("model", "gpt-4o-mini");
        
        JsonArray messages = new JsonArray();
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "user");
        msg.addProperty("content", String.format("Translate the following text from %s to %s. Respond ONLY with the translation, no conversational filler, no markdown formatting, no quotes.\n\nText: %s",
            sourceLang, targetLang, text));
        messages.add(msg);
        payload.add("messages", messages);

        URL url = URI.create("https://api.openai.com/v1/chat/completions").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);

            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new Exception("OpenAI API returned HTTP status: " + code);
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }

            JsonObject resp = JsonParser.parseString(sb.toString()).getAsJsonObject();
            return resp.getAsJsonArray("choices").get(0).getAsJsonObject()
                .getAsJsonObject("message").get("content").getAsString().trim();
        } finally {
            conn.disconnect();
        }
    }
}
