package net.kingchoka.minetranslator.translation.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.kingchoka.minetranslator.translation.TranslationProvider;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GoogleTranslationProvider implements TranslationProvider {

    @Override
    public String translate(String text, String sourceLang, String targetLang) throws Exception {
        String urlStr = "https://translate.googleapis.com/translate_a/single?dt=t&client=gtx&q=" +
                URLEncoder.encode(text, StandardCharsets.UTF_8) +
                "&sl=" + sourceLang +
                "&tl=" + targetLang;

        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setDoInput(true);
            conn.setDoOutput(false); // Do NOT change to POST for GET request!
            conn.setUseCaches(false);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("User-Agent", "MineTranslator");
            conn.connect();

            int statusCode = conn.getResponseCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new Exception("Google translation API returned status: " + statusCode);
            }

            StringBuilder responseBuilder = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    responseBuilder.append(line);
                }
            }

            return parseGoogleJson(responseBuilder.toString());
        } finally {
            conn.disconnect();
        }
    }

    private String parseGoogleJson(String jsonResponse) throws Exception {
        JsonElement element = JsonParser.parseString(jsonResponse);
        if (element.isJsonArray()) {
            JsonArray rootArray = element.getAsJsonArray();
            JsonArray translationArray = rootArray.get(0).getAsJsonArray();
            StringBuilder sb = new StringBuilder();
            for (JsonElement translationEntry : translationArray) {
                JsonArray entry = translationEntry.getAsJsonArray();
                sb.append(entry.get(0).getAsString());
            }
            return sb.toString();
        }
        throw new Exception("Unexpected json format from Google Translate: " + jsonResponse);
    }
}
