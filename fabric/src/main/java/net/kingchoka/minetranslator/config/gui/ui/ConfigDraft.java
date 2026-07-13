package net.kingchoka.minetranslator.config.gui.ui;

import net.kingchoka.minetranslator.config.ModConfig;

public final class ConfigDraft {
    public String provider;
    public String apiKey;
    public String model;
    public String baseUrl;
    public int requestTimeoutSeconds;
    public String sourceLanguage;
    public String targetLanguage;
    public boolean autoTranslateEveryMessage;
    public boolean autoTranslatePlayerMessages;
    public boolean autoTranslateItemNames;
    public boolean autoTranslateItemTooltips;
    public boolean showOriginal;
    public boolean debugLogging;
    public boolean translateMyMessages;
    public boolean translateOnlyNPC;
    public boolean preserveFormatting;
    public boolean useContext;
    public int contextSize;
    public boolean translateItemLore;
    public boolean translateItemAbilities;
    public boolean translateItemStats;
    public boolean preserveItemColors;
    public boolean translateItemsOnHold;
    public String outgoingSourceLanguage;
    public boolean outgoingPreview;
    public boolean replaceInputText;
    public boolean sendAutomatically;
    public boolean sendOriginalOnError;
    public boolean outgoingPlayerContext;
    public boolean fastTranslation;
    public boolean cacheEnabled;
    public int cacheMaxSize;
    public int parallelRequests;
    public boolean showMessageType;
    public boolean showParserStrategy;
    public boolean fakeProviderDebug;
    public boolean translateEveryIncomingMessageDebug;

    public ConfigDraft(ModConfig config) {
        copyFrom(config);
    }

    public void copyFrom(ModConfig config) {
        provider = config.provider;
        apiKey = config.apiKey;
        model = config.model;
        baseUrl = config.baseUrl;
        requestTimeoutSeconds = config.requestTimeoutSeconds;
        sourceLanguage = config.sourceLanguage;
        targetLanguage = config.targetLanguage;
        autoTranslateEveryMessage = config.autoTranslateEveryMessage;
        autoTranslatePlayerMessages = config.autoTranslatePlayerMessages;
        autoTranslateItemNames = config.autoTranslateItemNames;
        autoTranslateItemTooltips = config.autoTranslateItemTooltips;
        showOriginal = config.showOriginal;
        debugLogging = config.debugLogging;
        translateMyMessages = config.translateMyMessages;
        translateOnlyNPC = config.translateOnlyNPC;
        preserveFormatting = config.preserveFormatting;
        useContext = config.useContext;
        contextSize = config.contextSize;
        translateItemLore = config.translateItemLore;
        translateItemAbilities = config.translateItemAbilities;
        translateItemStats = config.translateItemStats;
        preserveItemColors = config.preserveItemColors;
        translateItemsOnHold = config.translateItemsOnHold;
        outgoingSourceLanguage = config.outgoingSourceLanguage;
        outgoingPreview = config.outgoingPreview;
        replaceInputText = config.replaceInputText;
        sendAutomatically = config.sendAutomatically;
        sendOriginalOnError = config.sendOriginalOnError;
        outgoingPlayerContext = config.outgoingPlayerContext;
        fastTranslation = config.fastTranslation;
        cacheEnabled = config.cacheEnabled;
        cacheMaxSize = config.cacheMaxSize;
        parallelRequests = config.parallelRequests;
        showMessageType = config.showMessageType;
        showParserStrategy = config.showParserStrategy;
        fakeProviderDebug = config.fakeProviderDebug;
        translateEveryIncomingMessageDebug = config.translateEveryIncomingMessageDebug;
    }

    public void applyTo(ModConfig config) {
        config.provider = provider;
        config.apiKey = apiKey;
        config.model = model;
        config.baseUrl = baseUrl;
        config.requestTimeoutSeconds = requestTimeoutSeconds;
        config.sourceLanguage = sourceLanguage;
        config.targetLanguage = targetLanguage;
        config.autoTranslateEveryMessage = autoTranslateEveryMessage;
        config.autoTranslatePlayerMessages = autoTranslatePlayerMessages;
        config.autoTranslateItemNames = autoTranslateItemNames;
        config.autoTranslateItemTooltips = autoTranslateItemTooltips;
        config.showOriginal = showOriginal;
        config.debugLogging = debugLogging;
        config.translateMyMessages = translateMyMessages;
        config.translateOnlyNPC = translateOnlyNPC;
        config.preserveFormatting = preserveFormatting;
        config.useContext = useContext;
        config.contextSize = contextSize;
        config.translateItemLore = translateItemLore;
        config.translateItemAbilities = translateItemAbilities;
        config.translateItemStats = translateItemStats;
        config.preserveItemColors = preserveItemColors;
        config.translateItemsOnHold = translateItemsOnHold;
        config.outgoingSourceLanguage = outgoingSourceLanguage;
        config.outgoingPreview = outgoingPreview;
        config.replaceInputText = replaceInputText;
        config.sendAutomatically = sendAutomatically;
        config.sendOriginalOnError = sendOriginalOnError;
        config.outgoingPlayerContext = outgoingPlayerContext;
        config.fastTranslation = fastTranslation;
        config.cacheEnabled = cacheEnabled;
        config.cacheMaxSize = cacheMaxSize;
        config.parallelRequests = parallelRequests;
        config.showMessageType = showMessageType;
        config.showParserStrategy = showParserStrategy;
        config.fakeProviderDebug = fakeProviderDebug;
        config.translateEveryIncomingMessageDebug = translateEveryIncomingMessageDebug;
    }
}
