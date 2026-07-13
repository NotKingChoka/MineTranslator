package net.kingchoka.minetranslator.translation.provider;

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

public class DeepLProvider implements TranslationProvider {
    private final Supplier<String> apiKeySupplier;
    public DeepLProvider() { this(() -> ModConfig.getInstance().apiKey); }
    public DeepLProvider(Supplier<String> apiKeySupplier) { this.apiKeySupplier = apiKeySupplier; }
    @Override
    public String translate(String text, String sourceLang, String targetLang) throws Exception {
        String apiKey = apiKeySupplier.get();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("DeepL API key is not configured.");
        }

        String host = apiKey.endsWith(":fx") ? "api-free.deepl.com" : "api.deepl.com";
        String urlStr = "https://" + host + "/v2/translate";

        JsonObject json = new JsonObject();
        json.addProperty("text", text);
        json.addProperty("target_lang", targetLang.toUpperCase());
        if (sourceLang != null && !sourceLang.equalsIgnoreCase("auto")) {
            json.addProperty("source_lang", sourceLang.toUpperCase());
        }

        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "DeepL-Auth-Key " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);

            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.write(json.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new Exception("DeepL API returned HTTP status: " + code);
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }

            JsonObject resp = JsonParser.parseString(sb.toString()).getAsJsonObject();
            return resp.getAsJsonArray("translations").get(0).getAsJsonObject().get("text").getAsString();
        } finally {
            conn.disconnect();
        }
    }
}
