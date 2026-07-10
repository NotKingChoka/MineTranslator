package net.kingchoka.minetranslator.cache;

import net.kingchoka.minetranslator.debug.TranslationDebugLogger;
import net.kingchoka.minetranslator.translation.TranslationMode;
import java.util.LinkedHashMap;
import java.util.Map;

public class TranslationCache {
    private static final TranslationCache INSTANCE = new TranslationCache(500);

    public static TranslationCache getInstance() {
        return INSTANCE;
    }

    public record CacheKey(
        String provider,
        TranslationMode mode,
        String sourceText,
        String sourceLanguage,
        String targetLanguage,
        int contextHash
    ) {}

    private final int maxSize;
    private final Map<CacheKey, String> cacheMap;

    public TranslationCache(int maxSize) {
        this.maxSize = maxSize;
        this.cacheMap = new LinkedHashMap<>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<CacheKey, String> eldest) {
                return size() > TranslationCache.this.maxSize;
            }
        };
    }

    public synchronized String get(String provider, TranslationMode mode, String sourceText, String sourceLanguage, String targetLanguage, int contextHash) {
        CacheKey key = new CacheKey(provider, mode, sourceText, sourceLanguage, targetLanguage, contextHash);
        if (cacheMap.containsKey(key)) {
            String val = cacheMap.get(key);
            TranslationDebugLogger.info("[CACHE HIT] Key: {} -> {}", key, val);
            return val;
        }
        TranslationDebugLogger.info("[CACHE MISS] Key: {}", key);
        return null;
    }

    public synchronized void put(String provider, TranslationMode mode, String sourceText, String sourceLanguage, String targetLanguage, int contextHash, String translatedText) {
        if (translatedText == null) return;
        CacheKey key = new CacheKey(provider, mode, sourceText, sourceLanguage, targetLanguage, contextHash);
        cacheMap.put(key, translatedText);
        TranslationDebugLogger.info("[CACHE STORE] Key: {} -> {}", key, translatedText);
    }

    public synchronized void clear() {
        cacheMap.clear();
        TranslationDebugLogger.info("[CACHE CLEAR] Cache was cleared.");
    }
}
