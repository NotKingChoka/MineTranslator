package net.psunset.translatorpp.compat.clothconfig;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.psunset.translatorpp.TranslatorPP;
import net.psunset.translatorpp.api.ComponentizableEnum;
import net.psunset.translatorpp.api.ITPPClothConfigData;
import net.psunset.translatorpp.config.TPPConfig;
import net.psunset.translatorpp.core.*;
import net.psunset.translatorpp.event.ClientTickCallbacks;
import net.psunset.translatorpp.keybind.TPPKeyMappings;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The ClothConfig-compatible implementation of {@link TPPConfig}.
 * To get config values, use {@link TPPConfig#getInstance()}.
 */
@ApiStatus.Internal
public class TPPConfigImplCloth implements TPPConfig {
    private static ConfigHolder<TPPConfigData> holder;

    public TPPConfigImplCloth() {
    }

    @Override
    public TranslationMode getMode() {
        return config().mode;
    }

    @Override
    public boolean getAutoTranslateChat() {
        return config().auto_translate_chat;
    }

    @Override
    public boolean getAutoTranslateNpc() {
        return config().auto_translate_npc;
    }

    @Override
    public boolean getUseNpcContextOnly() {
        return config().use_npc_context_only;
    }

    @Override
    public int getTranslationContextSize() {
        return config().translation_context_size;
    }

    @Override
    public boolean getShowOriginalWithTranslation() {
        return config().show_original_with_translation;
    }

    @Override
    public boolean getPreserveFormatting() {
        return config().preserve_formatting;
    }

    @Override
    public String getSourceLanguage() {
        return config().source_language;
    }

    @Override
    public String getTargetLanguage() {
        return config().target_language;
    }

    @Override
    public TranslationService getService() {
        return config().service;
    }

    @Override
    public String getOpenaiApiKey() {
        return config().openai_apikey;
    }

    @Override
    public OpenAIClientProvider.Api getOpenaiBaseUrl() {
        return config().openai_baseurl;
    }

    @Override
    public String getOpenaiCustomBaseUrl() {
        return config().openai_custom_baseurl;
    }

    @Override
    public String getOpenaiModel() {
        return config().openai_model;
    }

    @Override
    public String getDeepLApiKey() {
        return config().deepl_apikey;
    }

    @Override
    public String getGeminiApiKey() {
        return config().gemini_apikey;
    }

    @Override
    public String getGeminiBaseUrl() {
        return config().gemini_baseurl;
    }

    @Override
    public String getGeminiModel() {
        return config().gemini_model;
    }

    @Override
    public String getClaudeApiKey() {
        return config().claude_apikey;
    }

    @Override
    public String getClaudeBaseUrl() {
        return config().claude_baseurl;
    }

    @Override
    public String getClaudeModel() {
        return config().claude_model;
    }

    @Override
    public boolean getTranslatePlayerMessages() {
        return config().translate_player_messages;
    }

    @Override
    public boolean getPreserveGameTermsInPlayerChat() {
        return config().preserve_game_terms_in_player_chat;
    }

    @Override
    public boolean getShowPlayerOriginalWithTranslation() {
        return config().show_player_original_with_translation;
    }

    @Override
    public boolean getTranslateMyMessagesBeforeSending() {
        return config().translate_my_messages_before_sending;
    }

    @Override
    public String getMyMessageTargetLanguage() {
        return config().my_message_target_language;
    }

    @Override
    public boolean getShowTranslationPreviewBeforeSending() {
        return config().show_translation_preview_before_sending;
    }

    @Override
    public boolean getSendOriginalIfTranslationFails() {
        return config().send_original_if_translation_fails;
    }

    @Override
    public boolean getUseContextForMyMessages() {
        return config().use_context_for_my_messages;
    }

    @Override
    public int getPlayerContextSize() {
        return config().player_context_size;
    }

    @Override
    public boolean getPlayerContextOnly() {
        return config().player_context_only;
    }

    @Override
    public boolean getReplaceInputWithTranslation() {
        return config().replace_input_with_translation;
    }

    @Override
    public boolean getAutoSendAfterTranslation() {
        return config().auto_send_after_translation;
    }

    @Override
    public boolean getAutoTranslateItemNames() {
        return config().auto_translate_item_names;
    }

    @Override
    public boolean getAutoTranslateItemTooltips() {
        return config().auto_translate_item_tooltips;
    }

    @Override
    public boolean getPreserveItemFormatting() {
        return config().preserve_item_formatting;
    }

    @Override
    public boolean getTranslateStats() {
        return config().translate_stats;
    }

    @Override
    public boolean getTranslateLore() {
        return config().translate_lore;
    }

    @Override
    public boolean getTranslateEnchantments() {
        return config().translate_enchantments;
    }

    @Override
    public boolean getTranslateAbilities() {
        return config().translate_abilities;
    }

    @Override
    public boolean getFastTranslationMode() {
        return config().fast_translation_mode;
    }

    @Override
    public boolean getPreloadChatTranslations() {
        return config().preload_chat_translations;
    }

    @Override
    public boolean getPreloadTooltipTranslations() {
        return config().preload_tooltip_translations;
    }

    @Override
    public int getMaxCacheSize() {
        return config().max_cache_size;
    }

    @Override
    public void set(String option, Object value) {
        switch (option) {
            case "mode" -> config().mode = (TranslationMode) value;
            case "autoTranslateChat" -> config().auto_translate_chat = (Boolean) value;
            case "autoTranslateNpc" -> config().auto_translate_npc = (Boolean) value;
            case "useNpcContextOnly" -> config().use_npc_context_only = (Boolean) value;
            case "translationContextSize" -> config().translation_context_size = (Integer) value;
            case "showOriginalWithTranslation" -> config().show_original_with_translation = (Boolean) value;
            case "preserveFormatting" -> config().preserve_formatting = (Boolean) value;
            case "sourceLanguage" -> config().source_language = (String) value;
            case "targetLanguage" -> config().target_language = (String) value;
            case "service" -> config().service = (TranslationService) value;
            case "openaiApiKey" -> config().openai_apikey = (String) value;
            case "openaiBaseUrl" -> config().openai_baseurl = (OpenAIClientProvider.Api) value;
            case "openaiCustomBaseUrl" -> config().openai_custom_baseurl = (String) value;
            case "openaiModel" -> config().openai_model = (String) value;
            case "deeplApiKey" -> config().deepl_apikey = (String) value;
            case "geminiApiKey" -> config().gemini_apikey = (String) value;
            case "geminiBaseUrl" -> config().gemini_baseurl = (String) value;
            case "geminiModel" -> config().gemini_model = (String) value;
            case "claudeApiKey" -> config().claude_apikey = (String) value;
            case "claudeBaseUrl" -> config().claude_baseurl = (String) value;
            case "claudeModel" -> config().claude_model = (String) value;
            case "translatePlayerMessages" -> config().translate_player_messages = (Boolean) value;
            case "preserveGameTermsInPlayerChat" -> config().preserve_game_terms_in_player_chat = (Boolean) value;
            case "showPlayerOriginalWithTranslation" -> config().show_player_original_with_translation = (Boolean) value;
            case "translateMyMessagesBeforeSending" -> config().translate_my_messages_before_sending = (Boolean) value;
            case "myMessageTargetLanguage" -> config().my_message_target_language = (String) value;
            case "showTranslationPreviewBeforeSending" -> config().show_translation_preview_before_sending = (Boolean) value;
            case "sendOriginalIfTranslationFails" -> config().send_original_if_translation_fails = (Boolean) value;
            case "useContextForMyMessages" -> config().use_context_for_my_messages = (Boolean) value;
            case "playerContextSize" -> config().player_context_size = (Integer) value;
            case "playerContextOnly" -> config().player_context_only = (Boolean) value;
            case "replaceInputWithTranslation" -> config().replace_input_with_translation = (Boolean) value;
            case "autoSendAfterTranslation" -> config().auto_send_after_translation = (Boolean) value;
            case "autoTranslateItemNames" -> config().auto_translate_item_names = (Boolean) value;
            case "autoTranslateItemTooltips" -> config().auto_translate_item_tooltips = (Boolean) value;
            case "preserveItemFormatting" -> config().preserve_item_formatting = (Boolean) value;
            case "translateStats" -> config().translate_stats = (Boolean) value;
            case "translateLore" -> config().translate_lore = (Boolean) value;
            case "translateEnchantments" -> config().translate_enchantments = (Boolean) value;
            case "translateAbilities" -> config().translate_abilities = (Boolean) value;
            case "fastTranslationMode" -> config().fast_translation_mode = (Boolean) value;
            case "preloadChatTranslations" -> config().preload_chat_translations = (Boolean) value;
            case "preloadTooltipTranslations" -> config().preload_tooltip_translations = (Boolean) value;
            case "maxCacheSize" -> config().max_cache_size = (Integer) value;
        }
    }

    @Override
    public void save() {
        holder.save();
        OpenAIClientProvider.getInstance().refresh();
        OpenAIClientProvider.refreshCacheModels();
        DeepLTranslationProvider.getInstance().refresh();
        GeminiProvider.getInstance().refresh();
        ClaudeProvider.getInstance().refresh();
    }

    public static TPPConfigData config() {
        return holder.getConfig();
    }

    @Deprecated
    public static ITPPClothConfigData[] configs() {
        return new ITPPClothConfigData[]{config()};
    }

    public static void init() {
        // Migration logic:
        try {
            java.nio.file.Path configPath = java.nio.file.Paths.get("config").resolve("translatorpp.toml");
            if (java.nio.file.Files.exists(configPath)) {
                String content = java.nio.file.Files.readString(configPath);
                boolean migrated = false;
                if (content.contains("LibreTranslate")) {
                    content = content.replace("LibreTranslate", "DeepLTranslation");
                    migrated = true;
                }
                if (content.contains("OllamaClient")) {
                    content = content.replace("OllamaClient", "DeepLTranslation");
                    migrated = true;
                }
                if (migrated) {
                    java.nio.file.Files.writeString(configPath, content);
                }
            }
        } catch (Exception e) {
            TranslatorPP.LOGGER.error("Failed to migrate config: {}", e.toString());
        }

        holder = AutoConfig.register(TPPConfigData.class, Toml4jConfigSerializer::new);

        ClientTickCallbacks.POST.register(client -> {
            if (TPPKeyMappings.CONFIG_KEY.isDown()) {
                client.setScreen(new net.psunset.translatorpp.config.gui.TPPConfigScreen(client.screen));
            }
        });

        OpenAIClientProvider.getInstance().refresh();
        OpenAIClientProvider.refreshCacheModels();
        DeepLTranslationProvider.getInstance().refresh();
        GeminiProvider.getInstance().refresh();
        ClaudeProvider.getInstance().refresh();
    }

    @Config(name = TranslatorPP.ID)
    public static final class TPPConfigData implements ITPPClothConfigData {

        /* General */
        private TranslationMode mode = Default.mode;
        private String source_language = Default.sourceLanguage;
        private String target_language = Default.targetLanguage;
        private TranslationService service = Default.service;
        private boolean auto_translate_chat = Default.autoTranslateChat;
        private boolean auto_translate_npc = Default.autoTranslateNpc;
        private boolean use_npc_context_only = Default.useNpcContextOnly;
        private int translation_context_size = Default.translationContextSize;
        private boolean show_original_with_translation = Default.showOriginalWithTranslation;
        private boolean preserve_formatting = Default.preserveFormatting;

        private boolean translate_player_messages = Default.translatePlayerMessages;
        private boolean preserve_game_terms_in_player_chat = Default.preserveGameTermsInPlayerChat;
        private boolean show_player_original_with_translation = Default.showPlayerOriginalWithTranslation;
        private boolean translate_my_messages_before_sending = Default.translateMyMessagesBeforeSending;
        private String my_message_target_language = Default.myMessageTargetLanguage;
        private boolean show_translation_preview_before_sending = Default.showTranslationPreviewBeforeSending;
        private boolean send_original_if_translation_fails = Default.sendOriginalIfTranslationFails;
        private boolean use_context_for_my_messages = Default.useContextForMyMessages;
        private int player_context_size = Default.playerContextSize;
        private boolean player_context_only = Default.playerContextOnly;
        private boolean replace_input_with_translation = Default.replaceInputWithTranslation;
        private boolean auto_send_after_translation = Default.autoSendAfterTranslation;
        private boolean auto_translate_item_names = Default.autoTranslateItemNames;
        private boolean auto_translate_item_tooltips = Default.autoTranslateItemTooltips;
        private boolean preserve_item_formatting = Default.preserveItemFormatting;
        private boolean translate_stats = Default.translateStats;
        private boolean translate_lore = Default.translateLore;
        private boolean translate_enchantments = Default.translateEnchantments;
        private boolean translate_abilities = Default.translateAbilities;
        private boolean fast_translation_mode = Default.fastTranslationMode;
        private boolean preload_chat_translations = Default.preloadChatTranslations;
        private boolean preload_tooltip_translations = Default.preloadTooltipTranslations;
        private int max_cache_size = Default.maxCacheSize;

        /* OpenAI */
        private String openai_apikey = Default.openaiApiKey;
        private OpenAIClientProvider.Api openai_baseurl = Default.openaiBaseUrl;
        private String openai_custom_baseurl = Default.openaiCustomBaseUrl;
        private String openai_model = Default.openaiModel;

        /* DeepL */
        private String deepl_apikey = Default.deeplApiKey;

        /* Gemini */
        private String gemini_apikey = Default.geminiApiKey;
        private String gemini_baseurl = Default.geminiBaseUrl;
        private String gemini_model = Default.geminiModel;

        /* Claude */
        private String claude_apikey = Default.claudeApiKey;
        private String claude_baseurl = Default.claudeBaseUrl;
        private String claude_model = Default.claudeModel;

        @Override
        public Screen createScreen(Screen parent) {
            return new net.psunset.translatorpp.config.gui.TPPConfigScreen(parent);
        }
    }
}