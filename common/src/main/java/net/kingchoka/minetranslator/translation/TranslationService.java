package net.kingchoka.minetranslator.translation;

import net.kingchoka.minetranslator.cache.TranslationCache;
import net.kingchoka.minetranslator.config.ModConfig;
import net.kingchoka.minetranslator.debug.TranslationDebugLogger;
import net.kingchoka.minetranslator.translation.provider.*;
import net.minecraft.client.Minecraft;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class TranslationService {
    private static final TranslationService INSTANCE = new TranslationService();

    public static TranslationService getInstance() {
        return INSTANCE;
    }

    private final ExecutorService translationExecutor = Executors.newFixedThreadPool(3, new ThreadFactory() {
        private int count = 0;
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "MineTranslator-Worker-" + (++count));
            thread.setDaemon(true);
            return thread;
        }
    });

    private final Map<String, TranslationProvider> providers = Collections.synchronizedMap(new HashMap<>());

    private TranslationService() {
        providers.put("Google", new GoogleTranslationProvider());
        providers.put("Fake", new FakeTranslationProvider());
        providers.put("DeepL", new DeepLProvider());
        providers.put("Gemini", new GeminiProvider());
        providers.put("Claude", new ClaudeProvider());
        providers.put("OpenAI", new OpenAIProvider());
    }

    public void registerProvider(String id, TranslationProvider provider) {
        providers.put(id, provider);
    }

    public int activeRequests() {
        return translationExecutor instanceof ThreadPoolExecutor pool ? pool.getActiveCount() : 0;
    }

    public int queuedRequests() {
        return translationExecutor instanceof ThreadPoolExecutor pool ? pool.getQueue().size() : 0;
    }

    public CompletableFuture<TranslationResult> translate(TranslationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            String sourceText = request.sourceText();
            if (sourceText == null || sourceText.isBlank()) {
                return TranslationResult.failure(request, "Empty source text");
            }

            ModConfig config = ModConfig.getInstance();
            String providerId = request.providerId() != null ? request.providerId() : config.provider;

            int contextHash = request.context() != null ? request.context().hashCode() : 0;

            // 1. Check Cache first
            String cached = TranslationCache.getInstance().get(
                providerId,
                request.mode(),
                sourceText,
                request.sourceLanguage(),
                request.targetLanguage(),
                contextHash
            );

            if (cached != null) {
                return TranslationResult.success(request, cached);
            }

            // 2. Fetch provider
            TranslationProvider provider = providers.get(providerId);
            if (provider == null) {
                provider = providers.get("Google");
            }

            if (provider == null) {
                return TranslationResult.failure(request, "No translation provider available");
            }

            try {
                String translated = provider.translate(sourceText, request.sourceLanguage(), request.targetLanguage());
                if (translated != null && !translated.isEmpty()) {
                    // 3. Put into cache
                    TranslationCache.getInstance().put(
                        providerId,
                        request.mode(),
                        sourceText,
                        request.sourceLanguage(),
                        request.targetLanguage(),
                        contextHash,
                        translated
                    );
                    return TranslationResult.success(request, translated);
                } else {
                    return TranslationResult.failure(request, "Empty response from provider");
                }
            } catch (Exception e) {
                TranslationDebugLogger.error("Translation request failed for: {}. Error: {}", sourceText, e.toString());
                return TranslationResult.failure(request, e.toString());
            }
        }, translationExecutor);
    }
}
