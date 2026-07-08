package net.psunset.translatorpp.config.neoforge;

import com.electronwill.nightconfig.core.EnumGetMethod;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.psunset.translatorpp.TranslatorPP;
import net.psunset.translatorpp.config.TPPConfig;
import net.psunset.translatorpp.core.*;
import net.psunset.translatorpp.keybind.TPPKeyMappings;
import net.psunset.translatorpp.tool.CompatUtl;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The NeoForge-sided implementation of {@link TPPConfig}.
 * To get config values, use {@link TPPConfig#getInstance()}.
 */
@ApiStatus.Internal
@EventBusSubscriber(modid = TranslatorPP.ID)
public class TPPConfigImplNeoForge implements TPPConfig {

    public static final TPPConfigImplNeoForge INSTANCE;
    public static final ModConfigSpec SPEC;
    public static final Map<String, String> FIRST_CHILD_TO_CATEGORY = new HashMap<>();

    static {
        final Pair<TPPConfigImplNeoForge, ModConfigSpec> configPair = new ModConfigSpec.Builder().configure(TPPConfigImplNeoForge::new);
        INSTANCE = configPair.getLeft();
        SPEC = configPair.getRight();
    }

    /* General */
    public final ModConfigSpec.EnumValue<TranslationMode> mode;
    public final ModConfigSpec.ConfigValue<String> sourceLanguage;
    public final ModConfigSpec.ConfigValue<String> targetLanguage;
    public final ModConfigSpec.EnumValue<TranslationService> service;
    public final ModConfigSpec.BooleanValue autoTranslateChat;
    public final ModConfigSpec.BooleanValue autoTranslateNpc;
    public final ModConfigSpec.BooleanValue useNpcContextOnly;
    public final ModConfigSpec.IntValue translationContextSize;
    public final ModConfigSpec.BooleanValue showOriginalWithTranslation;
    public final ModConfigSpec.BooleanValue preserveFormatting;

    public final ModConfigSpec.BooleanValue translatePlayerMessages;
    public final ModConfigSpec.BooleanValue preserveGameTermsInPlayerChat;
    public final ModConfigSpec.BooleanValue showPlayerOriginalWithTranslation;
    public final ModConfigSpec.BooleanValue translateMyMessagesBeforeSending;
    public final ModConfigSpec.ConfigValue<String> myMessageTargetLanguage;
    public final ModConfigSpec.BooleanValue showTranslationPreviewBeforeSending;
    public final ModConfigSpec.BooleanValue sendOriginalIfTranslationFails;
    public final ModConfigSpec.BooleanValue useContextForMyMessages;
    public final ModConfigSpec.IntValue playerContextSize;
    public final ModConfigSpec.BooleanValue playerContextOnly;
    public final ModConfigSpec.BooleanValue replaceInputWithTranslation;
    public final ModConfigSpec.BooleanValue autoSendAfterTranslation;
    public final ModConfigSpec.BooleanValue autoTranslateItemNames;
    public final ModConfigSpec.BooleanValue autoTranslateItemTooltips;
    public final ModConfigSpec.BooleanValue preserveItemFormatting;
    public final ModConfigSpec.BooleanValue translateStats;
    public final ModConfigSpec.BooleanValue translateLore;
    public final ModConfigSpec.BooleanValue translateEnchantments;
    public final ModConfigSpec.BooleanValue translateAbilities;
    public final ModConfigSpec.BooleanValue fastTranslationMode;
    public final ModConfigSpec.BooleanValue preloadChatTranslations;
    public final ModConfigSpec.BooleanValue preloadTooltipTranslations;
    public final ModConfigSpec.IntValue maxCacheSize;

    /* OpenAI */
    public final ModConfigSpec.ConfigValue<String> openaiApiKey;
    public final ModConfigSpec.EnumValue<OpenAIClientProvider.Api> openaiBaseUrl;
    public final ModConfigSpec.ConfigValue<String> openaiCustomBaseUrl;
    public final ModConfigSpec.ConfigValue<String> openaiModel;

    /* DeepL */
    public final ModConfigSpec.ConfigValue<String> deeplApiKey;

    /* Gemini */
    public final ModConfigSpec.ConfigValue<String> geminiApiKey;
    public final ModConfigSpec.ConfigValue<String> geminiBaseUrl;
    public final ModConfigSpec.ConfigValue<String> geminiModel;

    /* Claude */
    public final ModConfigSpec.ConfigValue<String> claudeApiKey;
    public final ModConfigSpec.ConfigValue<String> claudeBaseUrl;
    public final ModConfigSpec.ConfigValue<String> claudeModel;

    private TPPConfigImplNeoForge(ModConfigSpec.Builder builder) {
        Set<String> tlList = Arrays.stream(Locale.getAvailableLocales())
                .map(Locale::toLanguageTag)
                .collect(Collectors.toSet());

        Set<String> slList = new HashSet<>(tlList.size() + 1);
        slList.add("auto");
        slList.addAll(tlList);

        /* ---------------------------------------- */
        FIRST_CHILD_TO_CATEGORY.put("mode", "general");

        this.mode = builder
                .translation("config.translatorpp.mode")
                .defineEnum("mode", Default.mode, EnumGetMethod.NAME_IGNORECASE);

        this.sourceLanguage = builder
                .translation("config.translatorpp.source_language")
                .defineInList("source_language", Default.sourceLanguage, slList);

        this.targetLanguage = builder
                .translation("config.translatorpp.target_language")
                .defineInList("target_language", Default.targetLanguage, tlList);

        this.service = builder
                .translation("config.translatorpp.service")
                .defineEnum("service", Default.service, EnumGetMethod.NAME_IGNORECASE);

        FIRST_CHILD_TO_CATEGORY.put("auto_translate_chat", "general");
        this.autoTranslateChat = builder
                .translation("config.translatorpp.auto_translate_chat")
                .define("auto_translate_chat", Default.autoTranslateChat);

        FIRST_CHILD_TO_CATEGORY.put("auto_translate_npc", "general");
        this.autoTranslateNpc = builder
                .translation("config.translatorpp.auto_translate_npc")
                .define("auto_translate_npc", Default.autoTranslateNpc);

        FIRST_CHILD_TO_CATEGORY.put("use_npc_context_only", "general");
        this.useNpcContextOnly = builder
                .translation("config.translatorpp.use_npc_context_only")
                .define("use_npc_context_only", Default.useNpcContextOnly);

        FIRST_CHILD_TO_CATEGORY.put("translation_context_size", "general");
        this.translationContextSize = builder
                .translation("config.translatorpp.translation_context_size")
                .defineInRange("translation_context_size", Default.translationContextSize, 0, 50);

        FIRST_CHILD_TO_CATEGORY.put("show_original_with_translation", "general");
        this.showOriginalWithTranslation = builder
                .translation("config.translatorpp.show_original_with_translation")
                .define("show_original_with_translation", Default.showOriginalWithTranslation);

        FIRST_CHILD_TO_CATEGORY.put("preserve_formatting", "general");
        this.preserveFormatting = builder
                .translation("config.translatorpp.preserve_formatting")
                .define("preserve_formatting", Default.preserveFormatting);

        FIRST_CHILD_TO_CATEGORY.put("translate_player_messages", "general");
        this.translatePlayerMessages = builder
                .translation("config.translatorpp.translate_player_messages")
                .define("translate_player_messages", Default.translatePlayerMessages);

        FIRST_CHILD_TO_CATEGORY.put("preserve_game_terms_in_player_chat", "general");
        this.preserveGameTermsInPlayerChat = builder
                .translation("config.translatorpp.preserve_game_terms_in_player_chat")
                .define("preserve_game_terms_in_player_chat", Default.preserveGameTermsInPlayerChat);

        FIRST_CHILD_TO_CATEGORY.put("show_player_original_with_translation", "general");
        this.showPlayerOriginalWithTranslation = builder
                .translation("config.translatorpp.show_player_original_with_translation")
                .define("show_player_original_with_translation", Default.showPlayerOriginalWithTranslation);

        FIRST_CHILD_TO_CATEGORY.put("fast_translation_mode", "general");
        this.fastTranslationMode = builder
                .translation("config.translatorpp.fast_translation_mode")
                .define("fast_translation_mode", Default.fastTranslationMode);

        FIRST_CHILD_TO_CATEGORY.put("preload_chat_translations", "general");
        this.preloadChatTranslations = builder
                .translation("config.translatorpp.preload_chat_translations")
                .define("preload_chat_translations", Default.preloadChatTranslations);

        FIRST_CHILD_TO_CATEGORY.put("preload_tooltip_translations", "general");
        this.preloadTooltipTranslations = builder
                .translation("config.translatorpp.preload_tooltip_translations")
                .define("preload_tooltip_translations", Default.preloadTooltipTranslations);

        FIRST_CHILD_TO_CATEGORY.put("max_cache_size", "general");
        this.maxCacheSize = builder
                .translation("config.translatorpp.max_cache_size")
                .defineInRange("max_cache_size", Default.maxCacheSize, 100, 5000);

        /* Outgoing */
        FIRST_CHILD_TO_CATEGORY.put("translate_my_messages_before_sending", "outgoing");
        this.translateMyMessagesBeforeSending = builder
                .translation("config.translatorpp.translate_my_messages_before_sending")
                .define("translate_my_messages_before_sending", Default.translateMyMessagesBeforeSending);

        this.myMessageTargetLanguage = builder
                .translation("config.translatorpp.my_message_target_language")
                .defineInList("my_message_target_language", Default.myMessageTargetLanguage, tlList);

        this.showTranslationPreviewBeforeSending = builder
                .translation("config.translatorpp.show_translation_preview_before_sending")
                .define("show_translation_preview_before_sending", Default.showTranslationPreviewBeforeSending);

        this.sendOriginalIfTranslationFails = builder
                .translation("config.translatorpp.send_original_if_translation_fails")
                .define("send_original_if_translation_fails", Default.sendOriginalIfTranslationFails);

        this.useContextForMyMessages = builder
                .translation("config.translatorpp.use_context_for_my_messages")
                .define("use_context_for_my_messages", Default.useContextForMyMessages);

        this.playerContextSize = builder
                .translation("config.translatorpp.player_context_size")
                .defineInRange("player_context_size", Default.playerContextSize, 0, 100);

        this.playerContextOnly = builder
                .translation("config.translatorpp.player_context_only")
                .define("player_context_only", Default.playerContextOnly);

        this.replaceInputWithTranslation = builder
                .translation("config.translatorpp.replace_input_with_translation")
                .define("replace_input_with_translation", Default.replaceInputWithTranslation);

        this.autoSendAfterTranslation = builder
                .translation("config.translatorpp.auto_send_after_translation")
                .define("auto_send_after_translation", Default.autoSendAfterTranslation);

        /* Tooltips */
        FIRST_CHILD_TO_CATEGORY.put("auto_translate_item_names", "tooltips");
        this.autoTranslateItemNames = builder
                .translation("config.translatorpp.auto_translate_item_names")
                .define("auto_translate_item_names", Default.autoTranslateItemNames);

        this.autoTranslateItemTooltips = builder
                .translation("config.translatorpp.auto_translate_item_tooltips")
                .define("auto_translate_item_tooltips", Default.autoTranslateItemTooltips);

        this.preserveItemFormatting = builder
                .translation("config.translatorpp.preserve_item_formatting")
                .define("preserve_item_formatting", Default.preserveItemFormatting);

        this.translateStats = builder
                .translation("config.translatorpp.translate_stats")
                .define("translate_stats", Default.translateStats);

        this.translateLore = builder
                .translation("config.translatorpp.translate_lore")
                .define("translate_lore", Default.translateLore);

        this.translateEnchantments = builder
                .translation("config.translatorpp.translate_enchantments")
                .define("translate_enchantments", Default.translateEnchantments);

        this.translateAbilities = builder
                .translation("config.translatorpp.translate_abilities")
                .define("translate_abilities", Default.translateAbilities);

        /* ---------------------------------------- */
        FIRST_CHILD_TO_CATEGORY.put("openai_apikey", "openai");

        this.openaiApiKey = builder
                .translation("config.translatorpp.openai_apikey")
                .define("openai_apikey", Default.openaiApiKey);

        this.openaiBaseUrl = builder
                .translation("config.translatorpp.openai_baseurl")
                .defineEnum("openai_baseurl", Default.openaiBaseUrl, EnumGetMethod.NAME_IGNORECASE);

        this.openaiCustomBaseUrl = builder
                .translation("config.translatorpp.openai_custom_baseurl")
                .define("openai_custom_baseurl", Default.openaiCustomBaseUrl);

        this.openaiModel = builder
                .translation("config.translatorpp.openai_model")
                .define("openai_model", Default.openaiModel, it ->
                        it == null ||  // DO NOT DELETE THIS LINE
                                it.toString().isEmpty() ||
                                !OpenAIClientProvider.getInstance().isPresent() ||
                                (OpenAIClientProvider.getInstance().isPresent() && OpenAIClientProvider.getCacheModels().contains(it.toString())));

        /* ---------------------------------------- */
        FIRST_CHILD_TO_CATEGORY.put("deepl_apikey", "deepl");

        this.deeplApiKey = builder
                .translation("config.translatorpp.deepl_apikey")
                .define("deepl_apikey", Default.deeplApiKey);

        /* ---------------------------------------- */
        FIRST_CHILD_TO_CATEGORY.put("gemini_apikey", "gemini");

        this.geminiApiKey = builder
                .translation("config.translatorpp.gemini_apikey")
                .define("gemini_apikey", Default.geminiApiKey);

        this.geminiBaseUrl = builder
                .translation("config.translatorpp.gemini_baseurl")
                .define("gemini_baseurl", Default.geminiBaseUrl);

        this.geminiModel = builder
                .translation("config.translatorpp.gemini_model")
                .define("gemini_model", Default.geminiModel);

        /* ---------------------------------------- */
        FIRST_CHILD_TO_CATEGORY.put("claude_apikey", "claude");

        this.claudeApiKey = builder
                .translation("config.translatorpp.claude_apikey")
                .define("claude_apikey", Default.claudeApiKey);

        this.claudeBaseUrl = builder
                .translation("config.translatorpp.claude_baseurl")
                .define("claude_baseurl", Default.claudeBaseUrl);

        this.claudeModel = builder
                .translation("config.translatorpp.claude_model")
                .define("claude_model", Default.claudeModel);
    }

    @Override
    public TranslationMode getMode() {
        return this.mode.get();
    }

    @Override
    public String getSourceLanguage() {
        return this.sourceLanguage.get();
    }

    @Override
    public String getTargetLanguage() {
        return this.targetLanguage.get();
    }

    @Override
    public TranslationService getService() {
        return this.service.get();
    }

    @Override
    public String getOpenaiApiKey() {
        return this.openaiApiKey.get();
    }

    @Override
    public OpenAIClientProvider.Api getOpenaiBaseUrl() {
        return this.openaiBaseUrl.get();
    }

    @Override
    public String getOpenaiCustomBaseUrl() {
        return this.openaiCustomBaseUrl.get();
    }

    @Override
    public String getOpenaiModel() {
        return this.openaiModel.get();
    }

    @Override
    public String getDeepLApiKey() {
        return this.deeplApiKey.get();
    }

    @Override
    public boolean getAutoTranslateChat() {
        return this.autoTranslateChat.get();
    }

    @Override
    public boolean getAutoTranslateNpc() {
        return this.autoTranslateNpc.get();
    }

    @Override
    public boolean getUseNpcContextOnly() {
        return this.useNpcContextOnly.get();
    }

    @Override
    public int getTranslationContextSize() {
        return this.translationContextSize.get();
    }

    @Override
    public boolean getShowOriginalWithTranslation() {
        return this.showOriginalWithTranslation.get();
    }

    @Override
    public boolean getPreserveFormatting() {
        return this.preserveFormatting.get();
    }

    @Override
    public String getGeminiApiKey() {
        return this.geminiApiKey.get();
    }

    @Override
    public String getGeminiBaseUrl() {
        return this.geminiBaseUrl.get();
    }

    @Override
    public String getGeminiModel() {
        return this.geminiModel.get();
    }

    @Override
    public String getClaudeApiKey() {
        return this.claudeApiKey.get();
    }

    @Override
    public String getClaudeBaseUrl() {
        return this.claudeBaseUrl.get();
    }

    @Override
    public String getClaudeModel() {
        return this.claudeModel.get();
    }

    @Override
    public boolean getTranslatePlayerMessages() {
        return this.translatePlayerMessages.get();
    }

    @Override
    public boolean getPreserveGameTermsInPlayerChat() {
        return this.preserveGameTermsInPlayerChat.get();
    }

    @Override
    public boolean getShowPlayerOriginalWithTranslation() {
        return this.showPlayerOriginalWithTranslation.get();
    }

    @Override
    public boolean getTranslateMyMessagesBeforeSending() {
        return this.translateMyMessagesBeforeSending.get();
    }

    @Override
    public String getMyMessageTargetLanguage() {
        return this.myMessageTargetLanguage.get();
    }

    @Override
    public boolean getShowTranslationPreviewBeforeSending() {
        return this.showTranslationPreviewBeforeSending.get();
    }

    @Override
    public boolean getSendOriginalIfTranslationFails() {
        return this.sendOriginalIfTranslationFails.get();
    }

    @Override
    public boolean getUseContextForMyMessages() {
        return this.useContextForMyMessages.get();
    }

    @Override
    public int getPlayerContextSize() {
        return this.playerContextSize.get();
    }

    @Override
    public boolean getPlayerContextOnly() {
        return this.playerContextOnly.get();
    }

    @Override
    public boolean getReplaceInputWithTranslation() {
        return this.replaceInputWithTranslation.get();
    }

    @Override
    public boolean getAutoSendAfterTranslation() {
        return this.autoSendAfterTranslation.get();
    }

    @Override
    public boolean getAutoTranslateItemNames() {
        return this.autoTranslateItemNames.get();
    }

    @Override
    public boolean getAutoTranslateItemTooltips() {
        return this.autoTranslateItemTooltips.get();
    }

    @Override
    public boolean getPreserveItemFormatting() {
        return this.preserveItemFormatting.get();
    }

    @Override
    public boolean getTranslateStats() {
        return this.translateStats.get();
    }

    @Override
    public boolean getTranslateLore() {
        return this.translateLore.get();
    }

    @Override
    public boolean getTranslateEnchantments() {
        return this.translateEnchantments.get();
    }

    @Override
    public boolean getTranslateAbilities() {
        return this.translateAbilities.get();
    }

    @Override
    public boolean getFastTranslationMode() {
        return this.fastTranslationMode.get();
    }

    @Override
    public boolean getPreloadChatTranslations() {
        return this.preloadChatTranslations.get();
    }

    @Override
    public boolean getPreloadTooltipTranslations() {
        return this.preloadTooltipTranslations.get();
    }

    @Override
    public int getMaxCacheSize() {
        return this.maxCacheSize.get();
    }

    @Override
    public void set(String option, Object value) {
        switch (option) {
            case "mode" -> this.mode.set((TranslationMode) value);
            case "autoTranslateChat" -> this.autoTranslateChat.set((Boolean) value);
            case "autoTranslateNpc" -> this.autoTranslateNpc.set((Boolean) value);
            case "useNpcContextOnly" -> this.useNpcContextOnly.set((Boolean) value);
            case "translationContextSize" -> this.translationContextSize.set((Integer) value);
            case "showOriginalWithTranslation" -> this.showOriginalWithTranslation.set((Boolean) value);
            case "preserveFormatting" -> this.preserveFormatting.set((Boolean) value);
            case "sourceLanguage" -> this.sourceLanguage.set((String) value);
            case "targetLanguage" -> this.targetLanguage.set((String) value);
            case "service" -> this.service.set((TranslationService) value);
            case "openaiApiKey" -> this.openaiApiKey.set((String) value);
            case "openaiBaseUrl" -> this.openaiBaseUrl.set((OpenAIClientProvider.Api) value);
            case "openaiCustomBaseUrl" -> this.openaiCustomBaseUrl.set((String) value);
            case "openaiModel" -> this.openaiModel.set((String) value);
            case "deeplApiKey" -> this.deeplApiKey.set((String) value);
            case "geminiApiKey" -> this.geminiApiKey.set((String) value);
            case "geminiBaseUrl" -> this.geminiBaseUrl.set((String) value);
            case "geminiModel" -> this.geminiModel.set((String) value);
            case "claudeApiKey" -> this.claudeApiKey.set((String) value);
            case "claudeBaseUrl" -> this.claudeBaseUrl.set((String) value);
            case "claudeModel" -> this.claudeModel.set((String) value);
            case "translatePlayerMessages" -> this.translatePlayerMessages.set((Boolean) value);
            case "preserveGameTermsInPlayerChat" -> this.preserveGameTermsInPlayerChat.set((Boolean) value);
            case "showPlayerOriginalWithTranslation" -> this.showPlayerOriginalWithTranslation.set((Boolean) value);
            case "translateMyMessagesBeforeSending" -> this.translateMyMessagesBeforeSending.set((Boolean) value);
            case "myMessageTargetLanguage" -> this.myMessageTargetLanguage.set((String) value);
            case "showTranslationPreviewBeforeSending" -> this.showTranslationPreviewBeforeSending.set((Boolean) value);
            case "sendOriginalIfTranslationFails" -> this.sendOriginalIfTranslationFails.set((Boolean) value);
            case "useContextForMyMessages" -> this.useContextForMyMessages.set((Boolean) value);
            case "playerContextSize" -> this.playerContextSize.set((Integer) value);
            case "playerContextOnly" -> this.playerContextOnly.set((Boolean) value);
            case "replaceInputWithTranslation" -> this.replaceInputWithTranslation.set((Boolean) value);
            case "autoSendAfterTranslation" -> this.autoSendAfterTranslation.set((Boolean) value);
            case "autoTranslateItemNames" -> this.autoTranslateItemNames.set((Boolean) value);
            case "autoTranslateItemTooltips" -> this.autoTranslateItemTooltips.set((Boolean) value);
            case "preserveItemFormatting" -> this.preserveItemFormatting.set((Boolean) value);
            case "translateStats" -> this.translateStats.set((Boolean) value);
            case "translateLore" -> this.translateLore.set((Boolean) value);
            case "translateEnchantments" -> this.translateEnchantments.set((Boolean) value);
            case "translateAbilities" -> this.translateAbilities.set((Boolean) value);
            case "fastTranslationMode" -> this.fastTranslationMode.set((Boolean) value);
            case "preloadChatTranslations" -> this.preloadChatTranslations.set((Boolean) value);
            case "preloadTooltipTranslations" -> this.preloadTooltipTranslations.set((Boolean) value);
            case "maxCacheSize" -> this.maxCacheSize.set((Integer) value);
        }
    }

    @Override
    public void save() {
        SPEC.save();
        OpenAIClientProvider.getInstance().refresh();
        OpenAIClientProvider.refreshCacheModels();
        DeepLTranslationProvider.getInstance().refresh();
        GeminiProvider.getInstance().refresh();
        ClaudeProvider.getInstance().refresh();
    }

    public static void init(ModContainer container) {
        moveOldConfigToNew();

        // Migration logic:
        try {
            java.nio.file.Path configPath = FMLPaths.CONFIGDIR.get().resolve("translatorpp.toml");
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

        container.registerConfig(ModConfig.Type.CLIENT, SPEC, TranslatorPP.ID + ".toml");

        container.registerExtensionPoint(IConfigScreenFactory.class,
                (c, p) -> new net.psunset.translatorpp.config.gui.TPPConfigScreen(p));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, TPPConfigImplNeoForge::afterClientTick);
    }

    public static void afterClientTick(ClientTickEvent.Post event) {
        if (TPPKeyMappings.CONFIG_KEY.isDown()) {
            Minecraft.getInstance().setScreen(new net.psunset.translatorpp.config.gui.TPPConfigScreen(Minecraft.getInstance().screen));
        }
    }

    @SubscribeEvent
    public static void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec().equals(SPEC)) {
            OpenAIClientProvider.getInstance().refresh();
            OpenAIClientProvider.refreshCacheModels();
            DeepLTranslationProvider.getInstance().refresh();
            GeminiProvider.getInstance().refresh();
            ClaudeProvider.getInstance().refresh();
        }
    }

    @SubscribeEvent
    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec().equals(SPEC)) {
            TranslationKit.getInstance().clearCache();
            OpenAIClientProvider.getInstance().refresh();
            OpenAIClientProvider.refreshCacheModels();
            DeepLTranslationProvider.getInstance().refresh();
            GeminiProvider.getInstance().refresh();
            ClaudeProvider.getInstance().refresh();
        }
    }

    private static void moveOldConfigToNew() {
        var configDir = FMLPaths.CONFIGDIR.get();
        var oConfigFile = configDir.resolve("translatorpp-client.toml");
        var configFile = configDir.resolve("translatorpp.toml");
        try {
            if (Files.exists(oConfigFile)) {
                if (Files.notExists(configFile)) {
                    byte[] oContent = Files.readAllBytes(oConfigFile);
                    Files.write(configFile, oContent, StandardOpenOption.CREATE);
                }
                Files.delete(oConfigFile);
            }
        } catch (IOException ignored) {
        }
    }
}
