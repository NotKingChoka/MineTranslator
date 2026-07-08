package net.kingchoka.minetranslator.config.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.kingchoka.minetranslator.api.ComponentizableEnum;
import net.kingchoka.minetranslator.api.IServiceProvider;
import net.kingchoka.minetranslator.config.MTConfig;
import net.kingchoka.minetranslator.core.*;
import net.kingchoka.minetranslator.keybind.MTKeyMappings;
import net.kingchoka.minetranslator.mixin.KeyMappingAccessor;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MTConfigScreen extends Screen {
    private final Screen parent;
    private final MTConfig config;

    // Cache of configuration variables to edit before saving
    private TranslationMode mode;
    private String sourceLanguage;
    private String targetLanguage;
    private TranslationService service;
    private boolean autoTranslateChat;
    private boolean autoTranslateNpc;
    private boolean useNpcContextOnly;
    private int translationContextSize;
    private boolean showOriginalWithTranslation;
    private boolean preserveFormatting;
    
    private boolean translatePlayerMessages;
    private boolean preserveGameTermsInPlayerChat;
    private boolean showPlayerOriginalWithTranslation;
    private boolean translateMyMessagesBeforeSending;
    private String myMessageTargetLanguage;
    private boolean showTranslationPreviewBeforeSending;
    private boolean sendOriginalIfTranslationFails;
    private boolean useContextForMyMessages;
    private int playerContextSize;
    private boolean playerContextOnly;
    private boolean replaceInputWithTranslation;
    private boolean autoSendAfterTranslation;
    
    private boolean autoTranslateItemNames;
    private boolean autoTranslateItemTooltips;
    private boolean preserveItemFormatting;
    private boolean translateStats;
    private boolean translateLore;
    private boolean translateEnchantments;
    private boolean translateAbilities;
    
    private boolean fastTranslationMode;
    private boolean preloadChatTranslations;
    private boolean preloadTooltipTranslations;
    private int maxCacheSize;

    private String openaiApiKey;
    private OpenAIClientProvider.Api openaiBaseUrl;
    private String openaiCustomBaseUrl;
    private String openaiModel;
    private String deeplApiKey;
    private String geminiApiKey;
    private String geminiBaseUrl;
    private String geminiModel;
    private String claudeApiKey;
    private String claudeBaseUrl;
    private String claudeModel;

    // Layout parameters
    private int leftPanelX = 0;
    private int leftPanelWidth = 110;
    private int contentX = 120;
    private int contentWidth = 0;
    private int rowHeight = 20;
    private int rowGap = 4;
    private int labelX = 125;
    private int controlWidth = 100;
    private int controlX = 0;

    // GUI UI state
    private Category currentCategory = Category.GENERAL;
    private int scrollY = 0;
    private int maxScrollY = 0;
    
    // Masking state for API keys
    private boolean maskOpenaiKey = true;
    private boolean maskDeeplKey = true;
    private boolean maskGeminiKey = true;
    private boolean maskClaudeKey = true;

    // Key mapping mapper state
    private KeyMapping mappingToBind = null;
    private String conflictWarning = "";

    // Test Provider state
    private String testStatus = "";
    private int testStatusColor = 0xFFCCCCCC;
    private boolean testingInProgress = false;

    // Row layout system
    private static class OptionRow {
        public final String labelKey;
        public final Component labelComponent;
        public final boolean isHeader;
        public final List<net.minecraft.client.gui.components.AbstractWidget> widgets = new ArrayList<>();
        public int y;

        public OptionRow(String labelKey, boolean isHeader) {
            this.labelKey = labelKey;
            this.labelComponent = Component.translatable(labelKey);
            this.isHeader = isHeader;
        }
    }
    
    private final List<OptionRow> optionRows = new ArrayList<>();
    private Component tooltipToShow = null;

    public enum Category {
        GENERAL("MineTranslator.category.general"),
        PROVIDERS("MineTranslator.category.providers"),
        CHAT("MineTranslator.category.chat"),
        ITEMS("MineTranslator.category.items"),
        MY_MESSAGES("MineTranslator.category.mymessages"),
        KEYBINDS("MineTranslator.category.keybinds"),
        ADVANCED("MineTranslator.category.advanced");

        public final String langKey;
        Category(String langKey) {
            this.langKey = langKey;
        }
    }

    public MTConfigScreen(Screen parent) {
        super(Component.translatable("MineTranslator.screen.title"));
        this.parent = parent;
        this.config = MTConfig.getInstance();
        this.loadSettings();
    }

    private void loadSettings() {
        this.mode = config.getMode();
        this.sourceLanguage = config.getSourceLanguage();
        this.targetLanguage = config.getTargetLanguage();
        this.service = config.getService();
        this.autoTranslateChat = config.getAutoTranslateChat();
        this.autoTranslateNpc = config.getAutoTranslateNpc();
        this.useNpcContextOnly = config.getUseNpcContextOnly();
        this.translationContextSize = config.getTranslationContextSize();
        this.showOriginalWithTranslation = config.getShowOriginalWithTranslation();
        this.preserveFormatting = config.getPreserveFormatting();
        
        this.translatePlayerMessages = config.getTranslatePlayerMessages();
        this.preserveGameTermsInPlayerChat = config.getPreserveGameTermsInPlayerChat();
        this.showPlayerOriginalWithTranslation = config.getShowPlayerOriginalWithTranslation();
        this.translateMyMessagesBeforeSending = config.getTranslateMyMessagesBeforeSending();
        this.myMessageTargetLanguage = config.getMyMessageTargetLanguage();
        this.showTranslationPreviewBeforeSending = config.getShowTranslationPreviewBeforeSending();
        this.sendOriginalIfTranslationFails = config.getSendOriginalIfTranslationFails();
        this.useContextForMyMessages = config.getUseContextForMyMessages();
        this.playerContextSize = config.getPlayerContextSize();
        this.playerContextOnly = config.getPlayerContextOnly();
        this.replaceInputWithTranslation = config.getReplaceInputWithTranslation();
        this.autoSendAfterTranslation = config.getAutoSendAfterTranslation();
        
        this.autoTranslateItemNames = config.getAutoTranslateItemNames();
        this.autoTranslateItemTooltips = config.getAutoTranslateItemTooltips();
        this.preserveItemFormatting = config.getPreserveItemFormatting();
        this.translateStats = config.getTranslateStats();
        this.translateLore = config.getTranslateLore();
        this.translateEnchantments = config.getTranslateEnchantments();
        this.translateAbilities = config.getTranslateAbilities();
        
        this.fastTranslationMode = config.getFastTranslationMode();
        this.preloadChatTranslations = config.getPreloadChatTranslations();
        this.preloadTooltipTranslations = config.getPreloadTooltipTranslations();
        this.maxCacheSize = config.getMaxCacheSize();

        this.openaiApiKey = config.getOpenaiApiKey();
        this.openaiBaseUrl = config.getOpenaiBaseUrl();
        this.openaiCustomBaseUrl = config.getOpenaiCustomBaseUrl();
        this.openaiModel = config.getOpenaiModel();
        this.deeplApiKey = config.getDeepLApiKey();
        this.geminiApiKey = config.getGeminiApiKey();
        this.geminiBaseUrl = config.getGeminiBaseUrl();
        this.geminiModel = config.getGeminiModel();
        this.claudeApiKey = config.getClaudeApiKey();
        this.claudeBaseUrl = config.getClaudeBaseUrl();
        this.claudeModel = config.getClaudeModel();
    }

    private void saveSettings() {
        config.set("mode", this.mode);
        config.set("sourceLanguage", this.sourceLanguage);
        config.set("targetLanguage", this.targetLanguage);
        config.set("service", this.service);
        config.set("autoTranslateChat", this.autoTranslateChat);
        config.set("autoTranslateNpc", this.autoTranslateNpc);
        config.set("useNpcContextOnly", this.useNpcContextOnly);
        config.set("translationContextSize", this.translationContextSize);
        config.set("showOriginalWithTranslation", this.showOriginalWithTranslation);
        config.set("preserveFormatting", this.preserveFormatting);
        
        config.set("translatePlayerMessages", this.translatePlayerMessages);
        config.set("preserveGameTermsInPlayerChat", this.preserveGameTermsInPlayerChat);
        config.set("showPlayerOriginalWithTranslation", this.showPlayerOriginalWithTranslation);
        config.set("translateMyMessagesBeforeSending", this.translateMyMessagesBeforeSending);
        config.set("myMessageTargetLanguage", this.myMessageTargetLanguage);
        config.set("showTranslationPreviewBeforeSending", this.showTranslationPreviewBeforeSending);
        config.set("sendOriginalIfTranslationFails", this.sendOriginalIfTranslationFails);
        config.set("useContextForMyMessages", this.useContextForMyMessages);
        config.set("playerContextSize", this.playerContextSize);
        config.set("playerContextOnly", this.playerContextOnly);
        config.set("replaceInputWithTranslation", this.replaceInputWithTranslation);
        config.set("autoSendAfterTranslation", this.autoSendAfterTranslation);
        
        config.set("autoTranslateItemNames", this.autoTranslateItemNames);
        config.set("autoTranslateItemTooltips", this.autoTranslateItemTooltips);
        config.set("preserveItemFormatting", this.preserveItemFormatting);
        config.set("translateStats", this.translateStats);
        config.set("translateLore", this.translateLore);
        config.set("translateEnchantments", this.translateEnchantments);
        config.set("translateAbilities", this.translateAbilities);
        
        config.set("fastTranslationMode", this.fastTranslationMode);
        config.set("preloadChatTranslations", this.preloadChatTranslations);
        config.set("preloadTooltipTranslations", this.preloadTooltipTranslations);
        config.set("maxCacheSize", this.maxCacheSize);

        config.set("openaiApiKey", this.openaiApiKey);
        config.set("openaiBaseUrl", this.openaiBaseUrl);
        config.set("openaiCustomBaseUrl", this.openaiCustomBaseUrl);
        config.set("openaiModel", this.openaiModel);
        config.set("deeplApiKey", this.deeplApiKey);
        config.set("geminiApiKey", this.geminiApiKey);
        config.set("geminiBaseUrl", this.geminiBaseUrl);
        config.set("geminiModel", this.geminiModel);
        config.set("claudeApiKey", this.claudeApiKey);
        config.set("claudeBaseUrl", this.claudeBaseUrl);
        config.set("claudeModel", this.claudeModel);
        
        config.save();
        Minecraft.getInstance().options.save();
    }

    @Override
    protected void init() {
        this.contentX = this.leftPanelWidth + 10;
        this.contentWidth = this.width - this.contentX - 15;
        this.labelX = this.contentX + 5;
        this.controlWidth = 100;
        this.controlX = this.width - this.controlWidth - 20;

        this.rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        this.clearWidgets();
        this.optionRows.clear();
        
        // Add sidebar category buttons
        int sidebarY = 35;
        int buttonW = this.leftPanelWidth - 10;
        for (Category cat : Category.values()) {
            this.addRenderableWidget(new MarqueeButton(5, sidebarY, buttonW, 18, Component.translatable(cat.langKey), b -> {
                this.currentCategory = cat;
                this.scrollY = 0;
                this.rebuildWidgets();
            }, false, this.currentCategory == cat));
            sidebarY += 22;
        }

        // Add save and cancel buttons at footer
        int footerY = this.height - 30;
        this.addRenderableWidget(new TPPButton(this.width - 155, footerY, 70, 20, Component.translatable("MineTranslator.button.save"), b -> {
            this.saveSettings();
            this.onClose();
        }, true));

        this.addRenderableWidget(new TPPButton(this.width - 80, footerY, 70, 20, Component.translatable("MineTranslator.button.cancel"), b -> {
            this.onClose();
        }, false));

        // Populate options rows
        switch (this.currentCategory) {
            case GENERAL:
                addHeader("MineTranslator.category.general");
                addEnum("MineTranslator.option.mode", TranslationMode.class, this.mode, v -> this.mode = v);
                addDropdown("MineTranslator.option.source_language", getLanguageTags(true), this.sourceLanguage, v -> this.sourceLanguage = v);
                addDropdown("MineTranslator.option.target_language", getLanguageTags(false), this.targetLanguage, v -> this.targetLanguage = v);
                addToggle("MineTranslator.option.show_original", this.showOriginalWithTranslation, v -> this.showOriginalWithTranslation = v);
                addToggle("MineTranslator.option.preserve_formatting", this.preserveFormatting, v -> this.preserveFormatting = v);
                break;

            case PROVIDERS:
                addHeader("MineTranslator.category.providers");
                addEnum("MineTranslator.option.service", TranslationService.class, this.service, v -> {
                    this.service = v;
                    this.rebuildWidgets();
                });

                if (this.service == TranslationService.OpenAIClient) {
                    addMaskableTextField("MineTranslator.option.api_key", this.openaiApiKey, v -> this.openaiApiKey = v, this.maskOpenaiKey, b -> {
                        this.maskOpenaiKey = !this.maskOpenaiKey;
                        this.rebuildWidgets();
                    });
                    addEnum("MineTranslator.option.base_url", OpenAIClientProvider.Api.class, this.openaiBaseUrl, v -> this.openaiBaseUrl = v);
                    if (this.openaiBaseUrl == OpenAIClientProvider.Api.Custom) {
                        addTextField("MineTranslator.option.base_url", this.openaiCustomBaseUrl, v -> this.openaiCustomBaseUrl = v);
                    }
                    addTextField("MineTranslator.option.model", this.openaiModel, v -> this.openaiModel = v);
                } else if (this.service == TranslationService.DeepLTranslation) {
                    addMaskableTextField("MineTranslator.option.api_key", this.deeplApiKey, v -> this.deeplApiKey = v, this.maskDeeplKey, b -> {
                        this.maskDeeplKey = !this.maskDeeplKey;
                        this.rebuildWidgets();
                    });
                } else if (this.service == TranslationService.Gemini) {
                    addMaskableTextField("MineTranslator.option.api_key", this.geminiApiKey, v -> this.geminiApiKey = v, this.maskGeminiKey, b -> {
                        this.maskGeminiKey = !this.maskGeminiKey;
                        this.rebuildWidgets();
                    });
                    addTextField("MineTranslator.option.base_url", this.geminiBaseUrl, v -> this.geminiBaseUrl = v);
                    addTextField("MineTranslator.option.model", this.geminiModel, v -> this.geminiModel = v);
                } else if (this.service == TranslationService.Claude) {
                    addMaskableTextField("MineTranslator.option.api_key", this.claudeApiKey, v -> this.claudeApiKey = v, this.maskClaudeKey, b -> {
                        this.maskClaudeKey = !this.maskClaudeKey;
                        this.rebuildWidgets();
                    });
                    addTextField("MineTranslator.option.base_url", this.claudeBaseUrl, v -> this.claudeBaseUrl = v);
                    addTextField("MineTranslator.option.model", this.claudeModel, v -> this.claudeModel = v);
                }

                addTestProviderRow();
                break;

            case CHAT:
                addHeader("MineTranslator.category.chat");
                addToggle("MineTranslator.option.auto_chat", this.autoTranslateChat, v -> this.autoTranslateChat = v);
                addToggle("MineTranslator.option.auto_npc", this.autoTranslateNpc, v -> this.autoTranslateNpc = v);
                addToggle("MineTranslator.option.auto_player", this.translatePlayerMessages, v -> this.translatePlayerMessages = v);
                addToggle("MineTranslator.option.preserve_game_terms", this.preserveGameTermsInPlayerChat, v -> this.preserveGameTermsInPlayerChat = v);
                addToggle("MineTranslator.option.use_npc_context", this.useNpcContextOnly, v -> this.useNpcContextOnly = v);
                addNumeric("MineTranslator.option.context_size", this.translationContextSize, 0, 50, v -> this.translationContextSize = v);
                break;

            case ITEMS:
                addHeader("MineTranslator.category.items");
                addToggle("MineTranslator.option.auto_item_names", this.autoTranslateItemNames, v -> this.autoTranslateItemNames = v);
                addToggle("MineTranslator.option.auto_item_tooltips", this.autoTranslateItemTooltips, v -> this.autoTranslateItemTooltips = v);
                addToggle("MineTranslator.option.preserve_item_formatting", this.preserveItemFormatting, v -> this.preserveItemFormatting = v);
                addToggle("MineTranslator.option.translate_stats", this.translateStats, v -> this.translateStats = v);
                addToggle("MineTranslator.option.translate_lore", this.translateLore, v -> this.translateLore = v);
                addToggle("MineTranslator.option.translate_enchantments", this.translateEnchantments, v -> this.translateEnchantments = v);
                addToggle("MineTranslator.option.translate_abilities", this.translateAbilities, v -> this.translateAbilities = v);
                break;

            case MY_MESSAGES:
                addHeader("MineTranslator.category.mymessages");
                addToggle("MineTranslator.option.translate_my_messages", this.translateMyMessagesBeforeSending, v -> this.translateMyMessagesBeforeSending = v);
                addDropdown("MineTranslator.option.my_target_language", getLanguageTags(false), this.myMessageTargetLanguage, v -> this.myMessageTargetLanguage = v);
                addToggle("MineTranslator.option.show_preview", this.showTranslationPreviewBeforeSending, v -> this.showTranslationPreviewBeforeSending = v);
                addToggle("MineTranslator.option.send_original_on_fail", this.sendOriginalIfTranslationFails, v -> this.sendOriginalIfTranslationFails = v);
                addToggle("MineTranslator.option.my_use_context", this.useContextForMyMessages, v -> this.useContextForMyMessages = v);
                addNumeric("MineTranslator.option.player_context_size", this.playerContextSize, 0, 100, v -> this.playerContextSize = v);
                addToggle("MineTranslator.option.player_context_only", this.playerContextOnly, v -> this.playerContextOnly = v);
                addToggle("MineTranslator.option.replace_input", this.replaceInputWithTranslation, v -> this.replaceInputWithTranslation = v);
                addToggle("MineTranslator.option.auto_send", this.autoSendAfterTranslation, v -> this.autoSendAfterTranslation = v);
                break;

            case KEYBINDS:
                addHeader("MineTranslator.category.keybinds");
                addKeybind("MineTranslator.keybind.translate_hovered_chat", MTKeyMappings.TRANSLATE_KEY);
                addKeybind("MineTranslator.keybind.translate_selected_input", MTKeyMappings.TRANSLATE_INPUT_KEY);
                addKeybind("key.MineTranslator.config", MTKeyMappings.CONFIG_KEY);
                addKeybind("MineTranslator.keybind.translate_hovered_item", MTKeyMappings.TRANSLATE_ITEM_KEY);
                addKeybind("MineTranslator.keybind.fast_translate", MTKeyMappings.FAST_TRANSLATE_KEY);
                addKeybind("MineTranslator.keybind.toggle_auto_chat", MTKeyMappings.TOGGLE_AUTO_CHAT_KEY);
                addKeybind("MineTranslator.keybind.toggle_auto_npc", MTKeyMappings.TOGGLE_AUTO_NPC_KEY);
                addKeybind("MineTranslator.keybind.toggle_auto_items", MTKeyMappings.TOGGLE_AUTO_ITEMS_KEY);

                OptionRow warningRow = new OptionRow("MineTranslator.error.conflict", false);
                this.optionRows.add(warningRow);
                break;

            case ADVANCED:
                addHeader("MineTranslator.category.advanced");
                addToggle("MineTranslator.option.fast_translation_mode", this.fastTranslationMode, v -> this.fastTranslationMode = v);
                addToggle("MineTranslator.option.preload_chat_translations", this.preloadChatTranslations, v -> this.preloadChatTranslations = v);
                addToggle("MineTranslator.option.preload_tooltip_translations", this.preloadTooltipTranslations, v -> this.preloadTooltipTranslations = v);
                addNumeric("MineTranslator.option.max_cache_size", this.maxCacheSize, 100, 100000, v -> this.maxCacheSize = v);
                break;
        }

        this.updateWidgetPositions();
    }

    private void updateWidgetPositions() {
        int contentStartY = 35;
        int currentY = contentStartY - this.scrollY;
        
        for (OptionRow row : this.optionRows) {
            row.y = currentY;
            
            // Adjust widgets to the currentY coordinate of the row
            for (var widget : row.widgets) {
                widget.setY(currentY);
            }
            
            currentY += this.rowHeight + this.rowGap;
        }
        
        // Calculate max scroll Y
        int totalContentHeight = this.optionRows.size() * (this.rowHeight + this.rowGap);
        int viewportHeight = this.height - 75; // from y=30 to y=height-45
        this.maxScrollY = Math.max(0, totalContentHeight - viewportHeight);
        
        // Clamp scroll
        if (this.scrollY > this.maxScrollY) {
            this.scrollY = this.maxScrollY;
            // update positions again with clamped scroll
            updateWidgetPositions();
            return;
        }
        
        // Update visibility
        this.updateWidgetVisibility();
    }

    private void updateWidgetVisibility() {
        int viewportMinY = 30;
        int viewportMaxY = this.height - 40;
        
        // Set everything to invisible by default (except sidebar / footer)
        for (var widget : this.children()) {
            if (widget instanceof net.minecraft.client.gui.components.AbstractWidget w) {
                if (w.getX() < this.leftPanelWidth || w.getY() > this.height - 35) {
                    w.visible = true;
                    w.active = true;
                    continue;
                }
                w.visible = false;
                w.active = false;
            }
        }
        
        // Set visible rows' widgets to active/visible
        for (OptionRow row : this.optionRows) {
            boolean inViewport = row.y >= viewportMinY && (row.y + this.rowHeight) <= viewportMaxY;
            for (var widget : row.widgets) {
                widget.visible = inViewport;
                widget.active = inViewport;
                if (widget instanceof EditBox box) {
                    box.setEditable(inViewport);
                }
            }
        }
    }

    private LabelWidget createLabelWidget(String labelKey) {
        int labelW = this.controlX - this.labelX - 10;
        return new LabelWidget(this.labelX, 0, labelW, 18, Component.translatable(labelKey), false);
    }

    private void addHeader(String labelKey) {
        OptionRow row = new OptionRow(labelKey, true);
        int labelW = this.width - this.labelX - 25;
        LabelWidget label = new LabelWidget(this.labelX, 0, labelW, 18, Component.translatable(labelKey), true);
        row.widgets.add(label);
        this.addRenderableWidget(label);
        this.optionRows.add(row);
    }

    private void addToggle(String labelKey, boolean value, java.util.function.Consumer<Boolean> consumer) {
        OptionRow row = new OptionRow(labelKey, false);
        
        LabelWidget label = createLabelWidget(labelKey);
        row.widgets.add(label);
        this.addRenderableWidget(label);
        
        int toggleWidth = 36;
        int toggleX = this.controlX + this.controlWidth - toggleWidth;
        
        TPPToggleButton btn = new TPPToggleButton(toggleX, 0, toggleWidth, 18, value, b -> {
            TPPToggleButton toggle = (TPPToggleButton) b;
            toggle.toggle();
            consumer.accept(toggle.getState());
        });
        
        row.widgets.add(btn);
        this.addRenderableWidget(btn);
        this.optionRows.add(row);
    }

    private <E extends Enum<E>> void addEnum(String labelKey, Class<E> enumClass, E currentVal, java.util.function.Consumer<E> consumer) {
        OptionRow row = new OptionRow(labelKey, false);
        
        LabelWidget label = createLabelWidget(labelKey);
        row.widgets.add(label);
        this.addRenderableWidget(label);
        
        E[] values = enumClass.getEnumConstants();
        Component message = Component.literal(currentVal.toString());
        if (currentVal instanceof ComponentizableEnum ce) {
            message = ce.toComponent();
        }
        
        TPPButton btn = new TPPButton(this.controlX, 0, this.controlWidth, 18, message, b -> {
            int nextIdx = (currentVal.ordinal() + 1) % values.length;
            consumer.accept(values[nextIdx]);
            this.rebuildWidgets();
        }, false);
        
        row.widgets.add(btn);
        this.addRenderableWidget(btn);
        this.optionRows.add(row);
    }

    private void addDropdown(String labelKey, List<String> selections, String currentVal, java.util.function.Consumer<String> consumer) {
        OptionRow row = new OptionRow(labelKey, false);
        
        LabelWidget label = createLabelWidget(labelKey);
        row.widgets.add(label);
        this.addRenderableWidget(label);
        
        TPPButton btn = new TPPButton(this.controlX, 0, this.controlWidth, 18, Component.literal(currentVal), b -> {
            int currentIdx = selections.indexOf(currentVal);
            int nextIdx = (currentIdx + 1) % selections.size();
            consumer.accept(selections.get(nextIdx));
            this.rebuildWidgets();
        }, false);
        
        row.widgets.add(btn);
        this.addRenderableWidget(btn);
        this.optionRows.add(row);
    }

    private void addTextField(String labelKey, String initialVal, java.util.function.Consumer<String> consumer) {
        OptionRow row = new OptionRow(labelKey, false);
        
        LabelWidget label = createLabelWidget(labelKey);
        row.widgets.add(label);
        this.addRenderableWidget(label);
        
        EditBox box = new EditBox(this.font, this.controlX, 0, this.controlWidth, 18, Component.translatable(labelKey));
        box.setMaxLength(512);
        box.setValue(initialVal);
        box.setResponder(consumer);
        
        row.widgets.add(box);
        this.addRenderableWidget(box);
        this.optionRows.add(row);
    }

    private void addMaskableTextField(String labelKey, String initialVal, java.util.function.Consumer<String> consumer, boolean masked, java.util.function.Consumer<Button> maskTogglePress) {
        OptionRow row = new OptionRow(labelKey, false);
        
        LabelWidget label = createLabelWidget(labelKey);
        row.widgets.add(label);
        this.addRenderableWidget(label);
        
        int fieldW = this.controlWidth - 24;
        EditBox box = new EditBox(this.font, this.controlX, 0, fieldW, 18, Component.translatable(labelKey));
        box.setMaxLength(512);
        box.setValue(initialVal);
        box.setResponder(consumer);
        if (masked) {
            box.addFormatter((str, pos) -> FormattedCharSequence.forward("*".repeat(str.length()), net.minecraft.network.chat.Style.EMPTY));
        }
        
        Component eyeMessage = Component.translatable(masked ? "MineTranslator.button.show" : "MineTranslator.button.hide");
        TPPButton eyeBtn = new TPPButton(this.controlX + fieldW + 2, 0, 22, 18, eyeMessage, b -> {
            maskTogglePress.accept(b);
        }, false);
        
        row.widgets.add(box);
        row.widgets.add(eyeBtn);
        this.addRenderableWidget(box);
        this.addRenderableWidget(eyeBtn);
        this.optionRows.add(row);
    }

    private void addNumeric(String labelKey, int currentVal, int min, int max, java.util.function.Consumer<Integer> consumer) {
        OptionRow row = new OptionRow(labelKey, false);
        
        LabelWidget label = createLabelWidget(labelKey);
        row.widgets.add(label);
        this.addRenderableWidget(label);
        
        int btnW = 20;
        int valW = this.controlWidth - btnW * 2 - 4;
        
        TPPButton minusBtn = new TPPButton(this.controlX, 0, btnW, 18, Component.literal("-"), b -> {
            int step = labelKey.contains("max_cache_size") ? 100 : 1;
            int newVal = Math.max(min, currentVal - step);
            consumer.accept(newVal);
            this.rebuildWidgets();
        }, false);
        
        TPPButton valDisplay = new TPPButton(this.controlX + btnW + 2, 0, valW, 18, Component.literal(String.valueOf(currentVal)), b -> {}, false);
        valDisplay.active = false;
        
        TPPButton plusBtn = new TPPButton(this.controlX + btnW + 2 + valW + 2, 0, btnW, 18, Component.literal("+"), b -> {
            int step = labelKey.contains("max_cache_size") ? 100 : 1;
            int newVal = Math.min(max, currentVal + step);
            consumer.accept(newVal);
            this.rebuildWidgets();
        }, false);
        
        row.widgets.add(minusBtn);
        row.widgets.add(valDisplay);
        row.widgets.add(plusBtn);
        this.addRenderableWidget(minusBtn);
        this.addRenderableWidget(valDisplay);
        this.addRenderableWidget(plusBtn);
        this.optionRows.add(row);
    }

    private void addKeybind(String labelKey, KeyMapping key) {
        OptionRow row = new OptionRow(labelKey, false);
        
        LabelWidget label = createLabelWidget(labelKey);
        row.widgets.add(label);
        this.addRenderableWidget(label);
        
        int actionsW = 64;
        Component display = (this.mappingToBind == key) ? 
                Component.translatable("MineTranslator.button.press_key") : 
                key.getTranslatedKeyMessage();

        TPPButton keyBtn = new TPPButton(this.controlX, 0, actionsW, 18, display, b -> {
            this.mappingToBind = key;
            this.conflictWarning = "";
            this.rebuildWidgets();
        }, this.mappingToBind == key);

        TPPButton clearBtn = new TPPButton(this.controlX + actionsW + 2, 0, 16, 18, Component.literal("✖"), b -> {
            key.setKey(InputConstants.UNKNOWN);
            this.rebuildWidgets();
        }, false);

        TPPButton resetBtn = new TPPButton(this.controlX + actionsW + 2 + 16 + 2, 0, 16, 18, Component.literal("⟲"), b -> {
            key.setKey(key.getDefaultKey());
            this.rebuildWidgets();
        }, false);

        row.widgets.add(keyBtn);
        row.widgets.add(clearBtn);
        row.widgets.add(resetBtn);
        this.addRenderableWidget(keyBtn);
        this.addRenderableWidget(clearBtn);
        this.addRenderableWidget(resetBtn);
        this.optionRows.add(row);
    }

    private void addTestProviderRow() {
        OptionRow row = new OptionRow("MineTranslator.button.test_provider", false);
        
        LabelWidget label = createLabelWidget("MineTranslator.tooltip.test_provider");
        row.widgets.add(label);
        this.addRenderableWidget(label);
        
        TPPButton testBtn = new TPPButton(this.controlX, 0, this.controlWidth, 18, Component.translatable("MineTranslator.button.test_provider"), b -> {
            this.runProviderTest();
        }, false);
        
        row.widgets.add(testBtn);
        this.addRenderableWidget(testBtn);
        this.optionRows.add(row);
        
        OptionRow statusRow = new OptionRow("MineTranslator.status.title", false);
        int labelW = this.width - this.labelX - 25;
        LabelWidget statusLabel = new LabelWidget(this.labelX, 0, labelW, 18, Component.empty(), false) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                if (!testStatus.isEmpty()) {
                    int textY = this.getY() + (this.height - 8) / 2;
                    graphics.drawString(Minecraft.getInstance().font, Component.translatable("MineTranslator.status.title").append(Component.translatable(testStatus)), this.getX(), textY, testStatusColor, false);
                }
            }
        };
        statusRow.widgets.add(statusLabel);
        this.addRenderableWidget(statusLabel);
        this.optionRows.add(statusRow);
    }

    private void runProviderTest() {
        this.testingInProgress = true;
        this.testStatus = "MineTranslator.status.checking";
        this.testStatusColor = 0xFFFFFF00;
        
        CompletableFuture.runAsync(() -> {
            // Backup original global config values
            TranslationService origService = config.getService();
            String origOpenaiApiKey = config.getOpenaiApiKey();
            OpenAIClientProvider.Api origOpenaiBaseUrl = config.getOpenaiBaseUrl();
            String origOpenaiCustomBaseUrl = config.getOpenaiCustomBaseUrl();
            String origOpenaiModel = config.getOpenaiModel();
            String origDeeplApiKey = config.getDeepLApiKey();
            String origGeminiApiKey = config.getGeminiApiKey();
            String origGeminiBaseUrl = config.getGeminiBaseUrl();
            String origGeminiModel = config.getGeminiModel();
            String origClaudeApiKey = config.getClaudeApiKey();
            String origClaudeBaseUrl = config.getClaudeBaseUrl();
            String origClaudeModel = config.getClaudeModel();

            try {
                // Apply temporary screen values to global config instance for testing
                config.set("service", this.service);
                config.set("openaiApiKey", this.openaiApiKey);
                config.set("openaiBaseUrl", this.openaiBaseUrl);
                config.set("openaiCustomBaseUrl", this.openaiCustomBaseUrl);
                config.set("openaiModel", this.openaiModel);
                config.set("deeplApiKey", this.deeplApiKey);
                config.set("geminiApiKey", this.geminiApiKey);
                config.set("geminiBaseUrl", this.geminiBaseUrl);
                config.set("geminiModel", this.geminiModel);
                config.set("claudeApiKey", this.claudeApiKey);
                config.set("claudeBaseUrl", this.claudeBaseUrl);
                config.set("claudeModel", this.claudeModel);

                // Refresh test providers with mock settings
                OpenAIClientProvider.getInstance().refresh();
                DeepLTranslationProvider.getInstance().refresh();
                GeminiProvider.getInstance().refresh();
                ClaudeProvider.getInstance().refresh();

                IServiceProvider testProvider = null;
                if (this.service == TranslationService.GoogleTranslation) {
                    testProvider = GoogleTranslationProvider.getInstance();
                } else if (this.service == TranslationService.OpenAIClient) {
                    testProvider = OpenAIClientProvider.getInstance();
                } else if (this.service == TranslationService.DeepLTranslation) {
                    testProvider = DeepLTranslationProvider.getInstance();
                } else if (this.service == TranslationService.Gemini) {
                    testProvider = GeminiProvider.getInstance();
                } else if (this.service == TranslationService.Claude) {
                    testProvider = ClaudeProvider.getInstance();
                }

                if (testProvider == null) {
                    throw new Exception("No provider configured.");
                }

                String res = testProvider.translate("Hi", "en", "es", null);
                if (res != null && !res.isEmpty()) {
                    this.testStatus = "MineTranslator.status.connected";
                    this.testStatusColor = 0xFF55FF55; // Vibrant Green
                } else {
                    throw new Exception("Response was empty.");
                }

            } catch (net.kingchoka.minetranslator.exception.ServiceException se) {
                int code = se.statusCode;
                if (code == 401) {
                    this.testStatus = "MineTranslator.status.invalid_key";
                } else if (code == 404) {
                    this.testStatus = "MineTranslator.status.wrong_model";
                } else if (code == 429) {
                    this.testStatus = "MineTranslator.status.rate_limited";
                } else {
                    this.testStatus = "MineTranslator.status.failed";
                }
                this.testStatusColor = 0xFFFF5555; // Red
            } catch (Exception e) {
                String msg = e.toString().toLowerCase();
                if (msg.contains("api key") || msg.contains("401") || msg.contains("unauthorized")) {
                    this.testStatus = "MineTranslator.status.invalid_key";
                } else if (msg.contains("timeout") || msg.contains("connect") || msg.contains("host")) {
                    this.testStatus = "MineTranslator.status.network_error";
                } else {
                    this.testStatus = "MineTranslator.status.failed";
                }
                this.testStatusColor = 0xFFFF5555;
            } finally {
                this.testingInProgress = false;
                
                // Restore original config values back
                config.set("service", origService);
                config.set("openaiApiKey", origOpenaiApiKey);
                config.set("openaiBaseUrl", origOpenaiBaseUrl);
                config.set("openaiCustomBaseUrl", origOpenaiCustomBaseUrl);
                config.set("openaiModel", origOpenaiModel);
                config.set("deeplApiKey", origDeeplApiKey);
                config.set("geminiApiKey", origGeminiApiKey);
                config.set("geminiBaseUrl", origGeminiBaseUrl);
                config.set("geminiModel", origGeminiModel);
                config.set("claudeApiKey", origClaudeApiKey);
                config.set("claudeBaseUrl", origClaudeBaseUrl);
                config.set("claudeModel", origClaudeModel);

                // Restore active provider settings
                OpenAIClientProvider.getInstance().refresh();
                DeepLTranslationProvider.getInstance().refresh();
                GeminiProvider.getInstance().refresh();
                ClaudeProvider.getInstance().refresh();
            }
        });
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX > this.leftPanelWidth + 5 && this.maxScrollY > 0) {
            this.scrollY = (int) Mth.clamp(this.scrollY - scrollY * 12, 0, this.maxScrollY);
            this.updateWidgetPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent keyEvent) {
        if (this.mappingToBind != null) {
            int keyCode = keyEvent.key();
            int scanCode = keyEvent.scancode();
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                // Clear active mapping click
                this.mappingToBind = null;
            } else {
                InputConstants.Key newKey = InputConstants.Type.KEYSYM.getOrCreate(keyCode);
                
                // Conflict resolver check
                boolean hasConflict = false;
                String conflictName = "";
                for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
                    InputConstants.Key bound = ((KeyMappingAccessor) mapping).MineTranslator$getKey();
                    if (mapping != this.mappingToBind && bound.equals(newKey) && !bound.equals(InputConstants.UNKNOWN)) {
                        hasConflict = true;
                        conflictName = mapping.getName();
                        break;
                    }
                }
                
                this.mappingToBind.setKey(newKey);
                if (hasConflict) {
                    this.conflictWarning = Component.translatable("MineTranslator.error.conflict").getString() + " (" + conflictName + ")";
                } else {
                    this.conflictWarning = "";
                }
                this.mappingToBind = null;
            }
            this.rebuildWidgets();
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Do nothing to prevent vanilla background from drawing over our custom screen layout.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Sidebar Background
        graphics.fill(0, 0, this.leftPanelWidth, this.height, 0xFF121212);
        
        // Content Area Background
        graphics.fill(this.leftPanelWidth, 0, this.width, this.height, 0xFF181818);

        // Render vertical indicator line next to active category
        int catIdx = this.currentCategory.ordinal();
        int lineY = 35 + catIdx * 22;
        graphics.fill(this.leftPanelWidth, lineY, this.leftPanelWidth + 2, lineY + 18, 0xFFFFD700);

        // Draw headers and footers panels overlay to clip scrolled labels/widgets
        graphics.fill(this.leftPanelWidth, 0, this.width, 30, 0xFF181818);
        graphics.fill(this.leftPanelWidth, this.height - 35, this.width, this.height, 0xFF181818);
        
        // Re-draw title over panel overlay
        graphics.drawString(this.font, this.title, this.contentX, 10, 0xFFE0A000);
        graphics.fill(this.leftPanelWidth, 24, this.width, 25, 0xFF3D3D3D);
        graphics.fill(this.leftPanelWidth, this.height - 35, this.width, this.height - 34, 0xFF3D3D3D);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private List<String> getLanguageTags(boolean includeAuto) {
        List<String> tags = new ArrayList<>();
        if (includeAuto) {
            tags.add("auto");
        }
        // Basic list of common languages supported by translation engines
        tags.addAll(Arrays.asList("en", "ru", "kk", "uk", "de", "fr", "es", "pt", "zh", "ja", "ko"));
        return tags;
    }

    private static class TPPButton extends Button {
        private final boolean primary;

        public TPPButton(int x, int y, int width, int height, Component message, OnPress onPress, boolean primary) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.primary = primary;
        }

        @Override
        protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isHoveredOrFocused();
            
            int bg = hovered ? 0xFF3A3A3A : 0xFF222222;
            if (this.primary) {
                bg = hovered ? 0xFFFFA726 : 0xFFFB8C00; // Premium Gold/Orange Accent
            }
            int border = hovered ? 0xFF666666 : 0xFF3D3D3D;
            if (this.primary) {
                border = hovered ? 0xFFFFCC80 : 0xFFFFA726;
            }
            
            // Draw filled box
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);
            
            // Draw outline borders
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, border);
            graphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, border);
            graphics.fill(this.getX(), this.getY() + 1, this.getX() + 1, this.getY() + this.height - 1, border);
            graphics.fill(this.getX() + this.width - 1, this.getY() + 1, this.getX() + this.width, this.getY() + this.height - 1, border);
            
            int textCol = primary ? 0xFF000000 : (hovered ? 0xFFFFFFFF : 0xFFD0D0D0);
            graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textCol);
        }
    }

    private static class TPPToggleButton extends Button {
        private boolean state;

        public TPPToggleButton(int x, int y, int width, int height, boolean initialState, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.state = initialState;
        }

        public void toggle() {
            this.state = !this.state;
        }

        public boolean getState() {
            return this.state;
        }

        @Override
        protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isHoveredOrFocused();

            int slotBg = this.state ? 0xFFFFA726 : 0xFF2A2A2A;
            int border = hovered ? 0xFF555555 : 0xFF3D3D3D;

            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, slotBg);
            
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, border);
            graphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, border);
            graphics.fill(this.getX(), this.getY() + 1, this.getX() + 1, this.getY() + this.height - 1, border);
            graphics.fill(this.getX() + this.width - 1, this.getY() + 1, this.getX() + this.width, this.getY() + this.height - 1, border);

            int knobWidth = this.height - 4;
            int knobX = this.state ? (this.getX() + this.width - knobWidth - 2) : (this.getX() + 2);
            int knobY = this.getY() + 2;
            int knobBg = hovered ? 0xFFFFFFFF : 0xFFE0E0E0;
            
            graphics.fill(knobX, knobY, knobX + knobWidth, knobY + knobWidth, knobBg);
        }
    }

    private static class MarqueeButton extends Button {
        private final boolean primary;
        private final boolean selected;
        private final Component fullText;
        private long hoverStartTime = -1;

        public MarqueeButton(int x, int y, int width, int height, Component message, OnPress onPress, boolean primary, boolean selected) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.primary = primary;
            this.selected = selected;
            this.fullText = message;
            
            var font = Minecraft.getInstance().font;
            if (font != null && font.width(message.getString()) > width - 10) {
                this.setTooltip(net.minecraft.client.gui.components.Tooltip.create(message));
            }
        }

        @Override
        protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isHoveredOrFocused();
            
            // Draw backgrounds and borders
            int bg = hovered ? 0xFF3A3A3A : 0xFF222222;
            if (this.selected) {
                bg = 0xFFFB8C00; // Accent gold/orange when selected
            } else if (this.primary) {
                bg = hovered ? 0xFFFFA726 : 0xFFFB8C00;
            }
            
            int border = hovered ? 0xFF666666 : 0xFF3D3D3D;
            if (this.selected) {
                border = 0xFFFFCC80;
            } else if (this.primary) {
                border = hovered ? 0xFFFFCC80 : 0xFFFFA726;
            }
            
            // Draw filled box
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);
            
            // Draw borders
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, border);
            graphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, border);
            graphics.fill(this.getX(), this.getY() + 1, this.getX() + 1, this.getY() + this.height - 1, border);
            graphics.fill(this.getX() + this.width - 1, this.getY() + 1, this.getX() + this.width, this.getY() + this.height - 1, border);
            
            var font = Minecraft.getInstance().font;
            String text = this.fullText.getString();
            int textWidth = font.width(text);
            int padding = 10;
            int maxTextWidth = this.width - padding;
            
            int textCol = selected ? 0xFF000000 : (primary ? 0xFF000000 : (hovered ? 0xFFFFFFFF : 0xFFD0D0D0));
            int textY = this.getY() + (this.height - 8) / 2;
            
            if (textWidth <= maxTextWidth) {
                graphics.drawCenteredString(font, this.fullText, this.getX() + this.width / 2, textY, textCol);
            } else {
                if (hovered || selected) {
                    if (hoverStartTime == -1) {
                        hoverStartTime = System.currentTimeMillis();
                    }
                } else {
                    hoverStartTime = -1;
                }
                
                int scrollRange = textWidth - maxTextWidth;
                double speed = (hovered || selected) ? 45.0 : 15.0; // speed in pixels per second
                double speedMs = speed / 1000.0;
                
                long startDelay = 1000;
                long endDelay = 1000;
                long scrollTime = (long) (scrollRange / speedMs);
                long totalCycle = startDelay + scrollTime + endDelay + scrollTime;
                
                long elapsed = 0;
                if (hoverStartTime != -1) {
                    elapsed = (System.currentTimeMillis() - hoverStartTime) % totalCycle;
                } else {
                    long slowScrollTime = (long) (scrollRange / (15.0 / 1000.0));
                    long slowTotalCycle = startDelay + slowScrollTime + endDelay + slowScrollTime;
                    elapsed = (System.currentTimeMillis()) % slowTotalCycle;
                    scrollTime = slowScrollTime;
                    totalCycle = slowTotalCycle;
                    speedMs = 15.0 / 1000.0;
                }
                
                double offset = 0;
                if (elapsed < startDelay) {
                    offset = 0;
                } else if (elapsed < startDelay + scrollTime) {
                    offset = (elapsed - startDelay) * speedMs;
                } else if (elapsed < startDelay + scrollTime + endDelay) {
                    offset = scrollRange;
                } else {
                    offset = scrollRange - (elapsed - startDelay - scrollTime - endDelay) * speedMs;
                }
                
                int scissorX = this.getX() + padding / 2;
                int scissorY = this.getY();
                int scissorW = maxTextWidth;
                int scissorH = this.height;
                
                graphics.enableScissor(scissorX, scissorY, scissorX + scissorW, scissorY + scissorH);
                graphics.drawString(font, text, (int) (this.getX() + padding / 2 - offset), textY, textCol, false);
                graphics.disableScissor();
            }
        }
    }

    private static class LabelWidget extends net.minecraft.client.gui.components.AbstractWidget {
        private final Component fullText;
        private final boolean isHeader;

        public LabelWidget(int x, int y, int width, int height, Component message, boolean isHeader) {
            super(x, y, width, height, message);
            this.fullText = message;
            this.isHeader = isHeader;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (this.isHeader) {
                int textY = this.getY() + 4;
                graphics.drawString(Minecraft.getInstance().font, this.fullText.getString().toUpperCase(Locale.ROOT), this.getX(), textY, 0xFFC0C0C0, false);
                graphics.fill(this.getX(), textY + 12, this.getX() + this.width, textY + 13, 0xFF3A3A3A);
            } else {
                int textY = this.getY() + (this.height - 8) / 2;
                var font = Minecraft.getInstance().font;
                String labelStr = this.fullText.getString();
                int labelWidth = font.width(labelStr);
                
                if (labelWidth <= this.width) {
                    graphics.drawString(font, this.fullText, this.getX(), textY, 0xFFEEEEEE, false);
                    this.setTooltip(null);
                } else {
                    graphics.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);
                    graphics.drawString(font, labelStr, this.getX(), textY, 0xFFEEEEEE, false);
                    graphics.disableScissor();
                    
                    this.setTooltip(net.minecraft.client.gui.components.Tooltip.create(this.fullText));
                }
            }
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
        }
    }
}
