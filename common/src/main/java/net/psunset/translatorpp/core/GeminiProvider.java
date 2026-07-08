package net.psunset.translatorpp.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.psunset.translatorpp.TranslatorPP;
import net.psunset.translatorpp.api.IServiceProvider;
import net.psunset.translatorpp.config.TPPConfig;
import net.psunset.translatorpp.exception.ServiceException;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class GeminiProvider implements IServiceProvider {
    public static final GeminiProvider INSTANCE = new GeminiProvider();

    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/";
    private static final String DEFAULT_MODEL = "gemini-1.5-flash";
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;

    @NotNull
    private String apiKey = "";
    @NotNull
    private String baseUrl = "";
    @NotNull
    private String model = "";

    public static GeminiProvider getInstance() {
        return INSTANCE;
    }

    private GeminiProvider() {}

    public void refresh() {
        TPPConfig config = TPPConfig.getInstance();
        String key = config.getGeminiApiKey();
        this.apiKey = key == null ? "" : key.trim();

        String url = config.getGeminiBaseUrl();
        if (url == null || url.trim().isEmpty()) {
            this.baseUrl = DEFAULT_BASE_URL;
        } else {
            this.baseUrl = url.trim();
            if (!this.baseUrl.endsWith("/")) {
                this.baseUrl += "/";
            }
        }

        String mdl = config.getGeminiModel();
        this.model = (mdl == null || mdl.trim().isEmpty()) ? DEFAULT_MODEL : mdl.trim();
    }

    @Override
    public String translate(String q, String sl, String tl, List<String> context) throws Exception {
        return translate(q, sl, tl, context, "GENERAL");
    }

    @Override
    public String translate(String q, String sl, String tl, List<String> context, String type) throws Exception {
        if (this.apiKey.isEmpty()) {
            throw new IllegalStateException("Gemini API key is not configured.");
        }

        String prompt;
        if ("PLAYER_CHAT".equals(type)) {
            String langName = OpenAIClientProvider.getLanguageName(tl);
            if (context != null && !context.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String ctx : context) {
                    sb.append("- ").append(ctx).append("\n");
                }
                prompt = OpenAIClientProvider.PLAYER_CHAT_PROMPT_WITH_CONTEXT.formatted(langName, this.separator(), sb.toString(), q);
            } else {
                prompt = OpenAIClientProvider.PLAYER_CHAT_PROMPT.formatted(langName, this.separator(), q);
            }
        } else {
            if (context != null && !context.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String ctx : context) {
                    sb.append("- ").append(ctx).append("\n");
                }
                prompt = OpenAIClientProvider.PROMPT_WITH_CONTEXT.formatted(sl, tl, this.separator(), sb.toString(), q);
            } else {
                prompt = OpenAIClientProvider.PROMPT.formatted(sl, tl, this.separator(), q);
            }
        }

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

        String jsonPayload = TranslationKit.GSON.toJson(payload);

        String cleanBaseUrl = this.baseUrl;
        String apiEndpoint = "v1beta/models/" + this.model + ":generateContent?key=" + this.apiKey;
        if (cleanBaseUrl.contains("v1beta/")) {
            cleanBaseUrl = cleanBaseUrl.replace("v1beta/", "");
        }
        if (cleanBaseUrl.contains("v1/")) {
            cleanBaseUrl = cleanBaseUrl.replace("v1/", "");
        }
        
        String fullUrl = cleanBaseUrl + apiEndpoint;

        HttpURLConnection con = null;
        try {
            URL urlObj = URI.create(fullUrl).toURL();
            con = (HttpURLConnection) urlObj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            con.setConnectTimeout(CONNECT_TIMEOUT);
            con.setReadTimeout(READ_TIMEOUT);
            con.setDoOutput(true);

            try (DataOutputStream dos = new DataOutputStream(con.getOutputStream())) {
                dos.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            int statusCode = con.getResponseCode();
            boolean isError = statusCode < 200 || statusCode >= 300;

            StringBuilder responseBuilder = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    isError ? con.getErrorStream() : con.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    responseBuilder.append(line);
                }
            }
            String rawResponse = responseBuilder.toString();
            JsonObject responseJson = TranslationKit.GSON.fromJson(rawResponse, JsonObject.class);

            if (isError) {
                if (responseJson != null && responseJson.has("error") && responseJson.get("error").isJsonObject()) {
                    JsonObject errorDetails = responseJson.getAsJsonObject("error");
                    String errMsg = errorDetails.has("message") ? errorDetails.get("message").getAsString() : rawResponse;
                    throw new ServiceException.Gemini(errMsg, statusCode);
                }
                throw new ServiceException.Gemini(rawResponse, statusCode);
            }

            JsonArray candidates = responseJson.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new IOException("Invalid Gemini response: 'candidates' array not found or empty. Response: " + rawResponse);
            }
            JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
            JsonObject content = firstCandidate.getAsJsonObject("content");
            if (content == null) {
                throw new IOException("Invalid Gemini response: 'content' not found in candidate. Response: " + rawResponse);
            }
            JsonArray resParts = content.getAsJsonArray("parts");
            if (resParts == null || resParts.isEmpty()) {
                throw new IOException("Invalid Gemini response: 'parts' not found or empty. Response: " + rawResponse);
            }
            return resParts.get(0).getAsJsonObject().get("text").getAsString().trim();

        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    public boolean isPresent() {
        return !this.apiKey.isEmpty() && !this.model.isEmpty();
    }
}
