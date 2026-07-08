package net.kingchoka.minetranslator.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kingchoka.minetranslator.MineTranslator;
import net.kingchoka.minetranslator.api.IServiceProvider;
import net.kingchoka.minetranslator.config.MTConfig;
import net.kingchoka.minetranslator.exception.ServiceException;
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

public class ClaudeProvider implements IServiceProvider {
    public static final ClaudeProvider INSTANCE = new ClaudeProvider();

    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com/";
    private static final String DEFAULT_MODEL = "claude-3-5-sonnet-20241022";
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;

    @NotNull
    private String apiKey = "";
    @NotNull
    private String baseUrl = "";
    @NotNull
    private String model = "";

    public static ClaudeProvider getInstance() {
        return INSTANCE;
    }

    private ClaudeProvider() {}

    public void refresh() {
        MTConfig config = MTConfig.getInstance();
        String key = config.getClaudeApiKey();
        this.apiKey = key == null ? "" : key.trim();

        String url = config.getClaudeBaseUrl();
        if (url == null || url.trim().isEmpty()) {
            this.baseUrl = DEFAULT_BASE_URL;
        } else {
            this.baseUrl = url.trim();
            if (!this.baseUrl.endsWith("/")) {
                this.baseUrl += "/";
            }
        }

        String mdl = config.getClaudeModel();
        this.model = (mdl == null || mdl.trim().isEmpty()) ? DEFAULT_MODEL : mdl.trim();
    }

    @Override
    public String translate(String q, String sl, String tl, List<String> context) throws Exception {
        return translate(q, sl, tl, context, "GENERAL");
    }

    @Override
    public String translate(String q, String sl, String tl, List<String> context, String type) throws Exception {
        if (this.apiKey.isEmpty()) {
            throw new IllegalStateException("Claude API key is not configured.");
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
        payload.addProperty("model", this.model);
        payload.addProperty("max_tokens", 1024);
        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);
        messages.add(userMsg);
        payload.add("messages", messages);

        String jsonPayload = TranslationKit.GSON.toJson(payload);

        String fullUrl = this.baseUrl;
        if (!fullUrl.contains("/v1/messages")) {
            fullUrl = fullUrl + "v1/messages";
        }

        HttpURLConnection con = null;
        try {
            URL urlObj = URI.create(fullUrl).toURL();
            con = (urlObj.openConnection() instanceof HttpURLConnection huc) ? huc : (HttpURLConnection) urlObj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("x-api-key", this.apiKey);
            con.setRequestProperty("anthropic-version", "2023-06-01");
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            con.setRequestProperty("Accept", "application/json");
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
                    throw new ServiceException.Claude(errMsg, statusCode);
                }
                throw new ServiceException.Claude(rawResponse, statusCode);
            }

            JsonArray contentArr = responseJson.getAsJsonArray("content");
            if (contentArr == null || contentArr.isEmpty()) {
                throw new IOException("Invalid Claude response: 'content' array not found or empty. Response: " + rawResponse);
            }
            JsonObject textObj = contentArr.get(0).getAsJsonObject();
            if (textObj == null || !textObj.has("text")) {
                throw new IOException("Invalid Claude response: 'text' field not found in content. Response: " + rawResponse);
            }
            return textObj.get("text").getAsString().trim();

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
