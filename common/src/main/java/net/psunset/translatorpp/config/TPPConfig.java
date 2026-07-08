package net.psunset.translatorpp.config;

import net.minecraft.network.chat.Component;
import net.psunset.translatorpp.TranslatorPP;
import net.psunset.translatorpp.annotations.ExpectMixin;
import net.psunset.translatorpp.compat.clothconfig.TPPConfigImplCloth;
import net.psunset.translatorpp.core.OpenAIClientProvider;
import net.psunset.translatorpp.core.TranslationMode;
import net.psunset.translatorpp.core.TranslationService;
import net.psunset.translatorpp.event.ClientTickCallbacks;
import net.psunset.translatorpp.keybind.TPPKeyMappings;
import net.psunset.translatorpp.platform.Platform;
import net.psunset.translatorpp.tool.ClientUtl;
import net.psunset.translatorpp.tool.CompatUtl;
import org.jetbrains.annotations.ApiStatus;

/**
 * The main config interface for Translator++.
 * To get config values, use {@link TPPConfig#getInstance()}.
 */
public interface TPPConfig {

    TranslationMode getMode();

    boolean getAutoTranslateChat();

    boolean getAutoTranslateNpc();

    boolean getUseNpcContextOnly();

    int getTranslationContextSize();

    boolean getShowOriginalWithTranslation();

    boolean getPreserveFormatting();

    String getSourceLanguage();

    String getTargetLanguage();

    TranslationService getService();

    String getOpenaiApiKey();

    OpenAIClientProvider.Api getOpenaiBaseUrl();

    String getOpenaiCustomBaseUrl();

    String getOpenaiModel();

    String getDeepLApiKey();

    String getGeminiApiKey();
    String getGeminiBaseUrl();
    String getGeminiModel();
    String getClaudeApiKey();
    String getClaudeBaseUrl();
    String getClaudeModel();

    boolean getTranslatePlayerMessages();

    boolean getPreserveGameTermsInPlayerChat();

    boolean getShowPlayerOriginalWithTranslation();

    boolean getTranslateMyMessagesBeforeSending();

    String getMyMessageTargetLanguage();

    boolean getShowTranslationPreviewBeforeSending();

    boolean getSendOriginalIfTranslationFails();

    boolean getUseContextForMyMessages();

    int getPlayerContextSize();

    boolean getPlayerContextOnly();

    boolean getReplaceInputWithTranslation();

    boolean getAutoSendAfterTranslation();

    boolean getAutoTranslateItemNames();

    boolean getAutoTranslateItemTooltips();

    boolean getPreserveItemFormatting();

    boolean getTranslateStats();

    boolean getTranslateLore();

    boolean getTranslateEnchantments();

    boolean getTranslateAbilities();

    boolean getFastTranslationMode();

    boolean getPreloadChatTranslations();

    boolean getPreloadTooltipTranslations();

    int getMaxCacheSize();

    void set(String optionName, Object value);
    void save();

    @ExpectMixin(value = ExpectMixin.Expected.NEOFORGE, method = ExpectMixin.Method.OVERWRITE)
    static void init() {
        if (Platform.isNeoForge()) {
            throw new AssertionError("Mixin missing!");
        } else if (CompatUtl.ClothConfig.isLoaded()) {
            TranslatorPP.LOGGER.debug("Cloth Config is loaded, using cloth config for Translator++ Config.");
            Dummy.INSTANCE = new TPPConfigImplCloth();
            TPPConfigImplCloth.init();
        } else {
            TranslatorPP.LOGGER.debug("No config API is loaded, using default values for Translator++ Config.");
            Dummy.INSTANCE = new Dummy();
            Dummy.init();
        }
    }

    static TPPConfig getInstance() {
        return Dummy.INSTANCE;
    }

    /**
     * The dummy implementation of TPPConfig, which uses default values.
     * Only used when no config API is available.
     */
    @ApiStatus.Internal
    class Dummy implements TPPConfig {

        /**
         * The instance of the TPPConfig, not only works for the dummy one.
         * Modify this field is not allowed.
         * To get this instance, use {@link TPPConfig#getInstance()}.
         */
        public static TPPConfig INSTANCE;

        @Override
        public TranslationMode getMode() {
            return Default.mode;
        }

        @Override
        public boolean getAutoTranslateChat() {
            return Default.autoTranslateChat;
        }

        @Override
        public boolean getAutoTranslateNpc() {
            return Default.autoTranslateNpc;
        }

        @Override
        public boolean getUseNpcContextOnly() {
            return Default.useNpcContextOnly;
        }

        @Override
        public int getTranslationContextSize() {
            return Default.translationContextSize;
        }

        @Override
        public boolean getShowOriginalWithTranslation() {
            return Default.showOriginalWithTranslation;
        }

        @Override
        public boolean getPreserveFormatting() {
            return Default.preserveFormatting;
        }

        @Override
        public String getSourceLanguage() {
            return Default.sourceLanguage;
        }

        @Override
        public String getTargetLanguage() {
            return Default.targetLanguage;
        }

        @Override
        public TranslationService getService() {
            return Default.service;
        }

        @Override
        public String getOpenaiApiKey() {
            return Default.openaiApiKey;
        }

        @Override
        public OpenAIClientProvider.Api getOpenaiBaseUrl() {
            return Default.openaiBaseUrl;
        }

        @Override
        public String getOpenaiCustomBaseUrl() {
            return Default.openaiCustomBaseUrl;
        }

        @Override
        public String getOpenaiModel() {
            return Default.openaiModel;
        }

        @Override
        public String getDeepLApiKey() {
            return Default.deeplApiKey;
        }

        @Override
        public String getGeminiApiKey() {
            return Default.geminiApiKey;
        }

        @Override
        public String getGeminiBaseUrl() {
            return Default.geminiBaseUrl;
        }

        @Override
        public String getGeminiModel() {
            return Default.geminiModel;
        }

        @Override
        public String getClaudeApiKey() {
            return Default.claudeApiKey;
        }

        @Override
        public String getClaudeBaseUrl() {
            return Default.claudeBaseUrl;
        }

        @Override
        public String getClaudeModel() {
            return Default.claudeModel;
        }

        @Override
        public boolean getTranslatePlayerMessages() {
            return Default.translatePlayerMessages;
        }

        @Override
        public boolean getPreserveGameTermsInPlayerChat() {
            return Default.preserveGameTermsInPlayerChat;
        }

        @Override
        public boolean getShowPlayerOriginalWithTranslation() {
            return Default.showPlayerOriginalWithTranslation;
        }

        @Override
        public boolean getTranslateMyMessagesBeforeSending() {
            return Default.translateMyMessagesBeforeSending;
        }

        @Override
        public String getMyMessageTargetLanguage() {
            return Default.myMessageTargetLanguage;
        }

        @Override
        public boolean getShowTranslationPreviewBeforeSending() {
            return Default.showTranslationPreviewBeforeSending;
        }

        @Override
        public boolean getSendOriginalIfTranslationFails() {
            return Default.sendOriginalIfTranslationFails;
        }

        @Override
        public boolean getUseContextForMyMessages() {
            return Default.useContextForMyMessages;
        }

        @Override
        public int getPlayerContextSize() {
            return Default.playerContextSize;
        }

        @Override
        public boolean getPlayerContextOnly() {
            return Default.playerContextOnly;
        }

        @Override
        public boolean getReplaceInputWithTranslation() {
            return Default.replaceInputWithTranslation;
        }

        @Override
        public boolean getAutoSendAfterTranslation() {
            return Default.autoSendAfterTranslation;
        }

        @Override
        public boolean getAutoTranslateItemNames() {
            return Default.autoTranslateItemNames;
        }

        @Override
        public boolean getAutoTranslateItemTooltips() {
            return Default.autoTranslateItemTooltips;
        }

        @Override
        public boolean getPreserveItemFormatting() {
            return Default.preserveItemFormatting;
        }

        @Override
        public boolean getTranslateStats() {
            return Default.translateStats;
        }

        @Override
        public boolean getTranslateLore() {
            return Default.translateLore;
        }

        @Override
        public boolean getTranslateEnchantments() {
            return Default.translateEnchantments;
        }

        @Override
        public boolean getTranslateAbilities() {
            return Default.translateAbilities;
        }

        @Override
        public boolean getFastTranslationMode() {
            return Default.fastTranslationMode;
        }

        @Override
        public boolean getPreloadChatTranslations() {
            return Default.preloadChatTranslations;
        }

        @Override
        public boolean getPreloadTooltipTranslations() {
            return Default.preloadTooltipTranslations;
        }

        @Override
        public int getMaxCacheSize() {
            return Default.maxCacheSize;
        }

        @Override
        public void set(String optionName, Object value) {}

        @Override
        public void save() {}

        public static void init() {
            ClientTickCallbacks.POST.register(client -> {
                while (TPPKeyMappings.CONFIG_KEY.consumeClick()) {
                    ClientUtl.message(client, Component.translatable("misc.translatorpp.missing.clothconfig"));
                }
            });
        }
    }

    /**
     * To store default values.
     */
    interface Default {
        TranslationMode mode = TranslationMode.NAME_TOP;
        boolean autoTranslateChat = false;
        boolean autoTranslateNpc = false;
        boolean useNpcContextOnly = true;
        int translationContextSize = 5;
        boolean showOriginalWithTranslation = false;
        boolean preserveFormatting = true;
        String sourceLanguage = "auto";
        String targetLanguage = "zh-CN";
        TranslationService service = TranslationService.GoogleTranslation;
        String openaiApiKey = "";
        OpenAIClientProvider.Api openaiBaseUrl = OpenAIClientProvider.Api.OpenAI;
        String openaiCustomBaseUrl = "https://custom.api.url/";
        String openaiModel = "";
        String deeplApiKey = "";
        String geminiApiKey = "";
        String geminiBaseUrl = "https://generativelanguage.googleapis.com/";
        String geminiModel = "gemini-1.5-flash";
        String claudeApiKey = "";
        String claudeBaseUrl = "https://api.anthropic.com/";
        String claudeModel = "claude-3-5-sonnet-20241022";

        boolean translatePlayerMessages = false;
        boolean preserveGameTermsInPlayerChat = true;
        boolean showPlayerOriginalWithTranslation = false;
        boolean translateMyMessagesBeforeSending = false;
        String myMessageTargetLanguage = "en";
        boolean showTranslationPreviewBeforeSending = true;
        boolean sendOriginalIfTranslationFails = true;
        boolean useContextForMyMessages = true;
        int playerContextSize = 50;
        boolean playerContextOnly = true;
        boolean replaceInputWithTranslation = true;
        boolean autoSendAfterTranslation = false;
        boolean autoTranslateItemNames = false;
        boolean autoTranslateItemTooltips = false;
        boolean preserveItemFormatting = true;
        boolean translateStats = true;
        boolean translateLore = true;
        boolean translateEnchantments = true;
        boolean translateAbilities = true;
        boolean fastTranslationMode = true;
        boolean preloadChatTranslations = false;
        boolean preloadTooltipTranslations = false;
        int maxCacheSize = 1000;
    }
}