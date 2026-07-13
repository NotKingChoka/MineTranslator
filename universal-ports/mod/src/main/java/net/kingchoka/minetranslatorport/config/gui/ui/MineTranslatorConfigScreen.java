package net.kingchoka.minetranslatorport.config.gui.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.kingchoka.minetranslatorport.ModConfig;
import net.kingchoka.minetranslatorport.PortController;
import net.kingchoka.minetranslatorport.ProviderConnectionController;
import net.kingchoka.minetranslatorport.ReflectionAccess;
import net.kingchoka.minetranslatorport.config.gui.ui.component.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public final class MineTranslatorConfigScreen extends Screen {
    private static final String VERSION = "3.0.0-alpha.1";
    private static final List<String> PROVIDERS = List.of("Google", "DeepL", "Gemini", "Claude", "OpenAI", "Fake");
    private static final List<String> SOURCE_LANGUAGES = List.of("auto", "en", "ru", "de", "fr", "es", "pt", "zh", "ja", "ko");
    private static final List<String> TARGET_LANGUAGES = List.of("ru", "en", "kk", "uk", "de", "fr", "es", "pt", "zh", "ja", "ko");

    private final Screen parent;
    private final ModConfig config;
    private final ConfigDraft draft;
    private final EnumMap<Category, Double> scrollPositions = new EnumMap<>(Category.class);
    private final List<SidebarButton> sidebarButtons = new ArrayList<>();
    private final List<SettingsCard> cards = new ArrayList<>();
    private final List<ProviderCard> providerCards = new ArrayList<>();
    private final ScrollContainer scroll = new ScrollContainer();
    private final StatusBadge statusBadge = new StatusBadge();
    private SettingsCard providerSettings;

    private UiLayout layout;
    private Category selected = Category.GENERAL;
    private boolean dirty;
    private boolean confirmClose;
    private boolean confirmClearCache;
    private boolean uiAnimations = true;
    private long savedUntil;
    private int focusedSidebarIndex;
    private boolean providerTesting;
    private String providerTestResultKey = "MineTranslator.status.not_tested";

    public MineTranslatorConfigScreen(Screen parent) {
        super(Component.translatable("MineTranslator.screen.title"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
        this.draft = new ConfigDraft(config);
        for (Category category : Category.values()) {
            scrollPositions.put(category, 0.0);
            sidebarButtons.add(new SidebarButton(category.icon, Component.translatable(category.titleKey)));
        }
        buildProviderCards();
        buildProviderSettings();
        buildCards();
    }

    @Override
    protected void init() {
        layout = UiLayout.calculate(width, height);
        positionSidebar();
        buildCards();
        buildProviderSettings();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (layout == null) layout = UiLayout.calculate(width, height);
        graphics.fillGradient(0, 0, width, height, 0xE80B0D11, 0xF012151B);
        graphics.fill(0, 0, width, height, 0x33000000);
        UiTheme.panel(graphics, layout.panelX(), layout.panelY(), layout.panelWidth(), layout.panelHeight(), UiTheme.PANEL);
        renderHeader(graphics, mouseX, mouseY);
        renderSidebar(graphics, mouseX, mouseY, partialTick);
        renderContent(graphics, mouseX, mouseY, partialTick);
        renderFooter(graphics, mouseX, mouseY);
        if (confirmClearCache) renderClearCacheDialog(graphics, mouseX, mouseY);
        if (confirmClose) renderConfirmDialog(graphics, mouseX, mouseY);
    }

    private void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = layout.panelX();
        int y = layout.panelY();
        int h = layout.headerHeight();
        graphics.fill(x + 1, y + 1, x + layout.panelWidth() - 1, y + h, 0xF21A1E25);
        graphics.fill(x + 1, y + h - 1, x + layout.panelWidth() - 1, y + h, UiTheme.BORDER);
        UiTheme.roundedFill(graphics, x + 12, y + 10, 22, 22, UiTheme.PRIMARY);
        graphics.drawCenteredString(font, "M", x + 23, y + 17, 0xFF171A20);
        graphics.drawString(font, "MineTranslator", x + 42, y + 11, UiTheme.TEXT, false);
        graphics.drawString(font, VERSION, x + 42, y + 24, UiTheme.MUTED, false);

        Component provider = Component.literal(draft.provider);
        Component state = Component.translatable(providerNeedsKey(draft.provider) && draft.apiKey.isBlank()
            ? "MineTranslator.status.api_key_missing" : "MineTranslator.status.connected");
        int statusWidth = font.width(state) + 22;
        int statusX = x + layout.panelWidth() - statusWidth - 42;
        statusBadge.render(graphics, font, state, statusX, y + (h - 18) / 2,
            !providerNeedsKey(draft.provider) || !draft.apiKey.isBlank());
        int providerX = statusX - font.width(provider) - 10;
        if (providerX > x + 210) graphics.drawString(font, provider, providerX, y + 17, UiTheme.SECONDARY, false);

        int closeX = x + layout.panelWidth() - 28;
        boolean closeHover = contains(mouseX, mouseY, closeX, y + 8, 20, 20);
        if (closeHover) UiTheme.roundedFill(graphics, closeX, y + 8, 20, 20, UiTheme.CARD_HOVER);
        graphics.drawCenteredString(font, "x", closeX + 10, y + 14, closeHover ? UiTheme.ERROR : UiTheme.MUTED);
    }

    private void renderSidebar(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = layout.panelX() + 1;
        int y = layout.panelY() + layout.headerHeight();
        int bottom = layout.footerY();
        graphics.fill(x, y, x + layout.sidebarWidth() - 1, bottom, 0xD914171C);
        graphics.fill(x + layout.sidebarWidth() - 1, y, x + layout.sidebarWidth(), bottom, UiTheme.BORDER);
        for (int i = 0; i < sidebarButtons.size(); i++) {
            sidebarButtons.get(i).render(graphics, font, mouseX, mouseY, selected.ordinal() == i, partialTick);
        }
    }

    private void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = layout.contentX();
        int y = layout.contentY();
        int w = layout.contentWidth();
        int h = layout.contentHeight();
        graphics.fill(x, y, x + w, y + h, UiTheme.BACKGROUND);
        int padding = layout.compact() ? 12 : 18;
        graphics.drawString(font, Component.translatable(selected.titleKey), x + padding, y + 12, UiTheme.TEXT, false);
        UiTheme.drawEllipsizedText(graphics, font, Component.translatable(selected.descriptionKey),
            x + padding, y + 27, w - padding * 2, UiTheme.MUTED);

        int viewportY = y + 46;
        int viewportHeight = h - 52;
        scroll.setBounds(x + padding, viewportY, w - padding * 2, viewportHeight);
        int contentHeight = selected == Category.PROVIDERS
            ? providerContentHeight(w - padding * 2) : cardsContentHeight(layout.compact());
        scroll.setContentHeight(contentHeight);
        scroll.begin(graphics);
        int contentY = viewportY - scroll.offset();
        if (selected == Category.PROVIDERS) {
            renderProviders(graphics, mouseX, mouseY, x + padding, contentY, w - padding * 2);
        } else {
            renderCards(graphics, mouseX, mouseY, partialTick, x + padding, contentY, w - padding * 2);
        }
        scroll.end(graphics);
        scroll.renderScrollbar(graphics);
    }

    private void renderCards(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int x, int y, int width) {
        int currentY = y;
        UiControl openControl = null;
        for (SettingsCard card : cards) {
            card.setBounds(x, currentY, width - 5, layout.compact());
            card.render(graphics, font, mouseX, mouseY, partialTick);
            for (SettingRow row : card.rows()) if (row.control().isOpen()) openControl = row.control();
            currentY += card.height() + 10;
        }
        if (openControl != null) openControl.render(graphics, font, mouseX, mouseY, partialTick);
    }

    private void renderProviders(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width) {
        int columns = width >= 440 ? 2 : 1;
        int gap = 10;
        int cardWidth = (width - (columns - 1) * gap - 5) / columns;
        int cardHeight = 68;
        for (int i = 0; i < providerCards.size(); i++) {
            int column = i % columns;
            int row = i / columns;
            ProviderCard card = providerCards.get(i);
            card.setBounds(x + column * (cardWidth + gap), y + row * (cardHeight + gap), cardWidth, cardHeight);
            card.render(graphics, font, mouseX, mouseY, card.provider().equals(draft.provider), !draft.apiKey.isBlank());
        }
        int rows = (providerCards.size() + columns - 1) / columns;
        int settingsY = y + rows * (cardHeight + gap) + 4;
        providerSettings.setBounds(x, settingsY, width - 5, layout.compact());
        providerSettings.render(graphics, font, mouseX, mouseY, 0.0F);
        UiControl open = null;
        for (SettingRow row : providerSettings.rows()) if (row.control().isOpen()) open = row.control();
        if (open != null) open.render(graphics, font, mouseX, mouseY, 0.0F);
    }

    private void renderFooter(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = layout.panelX();
        int y = layout.footerY();
        int w = layout.panelWidth();
        int h = layout.footerHeight();
        graphics.fill(x + 1, y, x + w - 1, y + h - 1, 0xF21A1E25);
        graphics.fill(x + 1, y, x + w - 1, y + 1, UiTheme.BORDER);
        int buttonY = y + (h - 22) / 2;
        int saveW = layout.compact() ? 104 : 150;
        int cancelW = layout.compact() ? 70 : 88;
        int resetW = layout.compact() ? 78 : 108;
        int saveX = x + w - saveW - 12;
        int cancelX = saveX - cancelW - 8;
        drawButton(graphics, Component.translatable("MineTranslator.button.reset_section"), x + 12, buttonY, resetW, 22, false,
            contains(mouseX, mouseY, x + 12, buttonY, resetW, 22));
        drawButton(graphics, Component.translatable("MineTranslator.button.cancel"), cancelX, buttonY, cancelW, 22, false,
            contains(mouseX, mouseY, cancelX, buttonY, cancelW, 22));
        Component saveLabel = System.currentTimeMillis() < savedUntil
            ? Component.translatable("MineTranslator.status.saved")
            : Component.translatable("MineTranslator.button.save_changes");
        drawButton(graphics, saveLabel, saveX, buttonY, saveW, 22, dirty,
            contains(mouseX, mouseY, saveX, buttonY, saveW, 22));
    }

    private void renderConfirmDialog(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, width, height, 0x99000000);
        int dialogW = Math.min(380, width - 30);
        int dialogH = 116;
        int x = (width - dialogW) / 2;
        int y = (height - dialogH) / 2;
        UiTheme.panel(graphics, x, y, dialogW, dialogH, UiTheme.PANEL);
        graphics.drawString(font, Component.translatable("MineTranslator.dialog.unsaved.title"), x + 14, y + 14, UiTheme.TEXT, false);
        UiTheme.drawEllipsizedText(graphics, font, Component.translatable("MineTranslator.dialog.unsaved.description"),
            x + 14, y + 34, dialogW - 28, UiTheme.MUTED);
        int buttonY = y + dialogH - 34;
        int bw = (dialogW - 44) / 3;
        drawButton(graphics, Component.translatable("MineTranslator.button.save"), x + 10, buttonY, bw, 22, true,
            contains(mouseX, mouseY, x + 10, buttonY, bw, 22));
        drawButton(graphics, Component.translatable("MineTranslator.button.discard"), x + 17 + bw, buttonY, bw, 22, false,
            contains(mouseX, mouseY, x + 17 + bw, buttonY, bw, 22));
        drawButton(graphics, Component.translatable("MineTranslator.button.cancel"), x + 24 + bw * 2, buttonY, bw, 22, false,
            contains(mouseX, mouseY, x + 24 + bw * 2, buttonY, bw, 22));
    }

    private void renderClearCacheDialog(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, width, height, 0x99000000);
        int dialogW = Math.min(360, width - 30), dialogH = 100;
        int x = (width - dialogW) / 2, y = (height - dialogH) / 2;
        UiTheme.panel(graphics, x, y, dialogW, dialogH, UiTheme.PANEL);
        graphics.drawString(font, Component.translatable("MineTranslator.dialog.cache.title"), x + 14, y + 14, UiTheme.TEXT, false);
        UiTheme.drawEllipsizedText(graphics, font, Component.translatable("MineTranslator.dialog.cache.description"),
            x + 14, y + 34, dialogW - 28, UiTheme.MUTED);
        int buttonY = y + dialogH - 32;
        drawButton(graphics, Component.translatable("MineTranslator.button.clear_cache"), x + dialogW - 188, buttonY, 100, 22, true,
            contains(mouseX, mouseY, x + dialogW - 188, buttonY, 100, 22));
        drawButton(graphics, Component.translatable("MineTranslator.button.cancel"), x + dialogW - 80, buttonY, 68, 22, false,
            contains(mouseX, mouseY, x + dialogW - 80, buttonY, 68, 22));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (confirmClearCache) return handleClearCacheClick(mouseX, mouseY, button);
        if (confirmClose) return handleConfirmClick(mouseX, mouseY, button);
        if (scroll.mouseClicked(mouseX, mouseY, button)) return true;
        int closeX = layout.panelX() + layout.panelWidth() - 28;
        if (button == 0 && contains(mouseX, mouseY, closeX, layout.panelY() + 8, 20, 20)) {
            onClose();
            return true;
        }
        for (int i = 0; i < sidebarButtons.size(); i++) {
            if (button == 0 && sidebarButtons.get(i).contains(mouseX, mouseY)) {
                selectCategory(Category.values()[i]);
                return true;
            }
        }
        int buttonY = layout.footerY() + (layout.footerHeight() - 22) / 2;
        int saveW = layout.compact() ? 104 : 150;
        int cancelW = layout.compact() ? 70 : 88;
        int resetW = layout.compact() ? 78 : 108;
        int saveX = layout.panelX() + layout.panelWidth() - saveW - 12;
        int cancelX = saveX - cancelW - 8;
        if (button == 0 && contains(mouseX, mouseY, layout.panelX() + 12, buttonY, resetW, 22)) {
            resetSection(); return true;
        }
        if (button == 0 && contains(mouseX, mouseY, cancelX, buttonY, cancelW, 22)) {
            onClose(); return true;
        }
        if (button == 0 && dirty && contains(mouseX, mouseY, saveX, buttonY, saveW, 22)) {
            save(); return true;
        }
        if (selected == Category.PROVIDERS) {
            for (ProviderCard card : providerCards) {
                if (button == 0 && card.contains(mouseX, mouseY)) {
                    draft.provider = card.provider(); providerTestResultKey = "MineTranslator.status.not_tested";
                    buildProviderSettings(); markDirty(); return true;
                }
            }
            if (providerSettings.mouseClicked(mouseX, mouseY, button)) return true;
        } else {
            for (SettingsCard card : cards) if (card.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scroll.mouseDragged(mouseY)) {
            scrollPositions.put(selected, scroll.scrollValue());
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scroll.mouseReleased()) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (scroll.scroll(mouseX, mouseY, vertical)) {
            scrollPositions.put(selected, scroll.scrollValue());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (UiControl control : visibleControls()) if (control.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            focusedSidebarIndex = (focusedSidebarIndex + 1) % Category.values().length;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            focusedSidebarIndex = (focusedSidebarIndex + Category.values().length - 1) % Category.values().length;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            selectCategory(Category.values()[focusedSidebarIndex]);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (UiControl control : visibleControls()) if (control.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        for (UiControl control : visibleControls()) control.tick();
    }

    @Override
    public void onClose() {
        if (dirty && !confirmClose) {
            confirmClose = true;
            return;
        }
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    public void selectCategoryForVisualTest(int index) {
        if (index >= 0 && index < Category.values().length) {
            selectCategory(Category.values()[index]);
        }
    }

    private void buildCards() {
        cards.clear();
        switch (selected) {
            case GENERAL -> buildGeneral();
            case CHAT -> buildChat();
            case ITEMS -> buildItems();
            case MY_MESSAGES -> buildMyMessages();
            case KEYS -> buildKeys();
            case CACHE -> buildCache();
            case DEBUG -> buildDebug();
            case PROVIDERS -> { }
        }
    }

    private void buildGeneral() {
        cards.add(new SettingsCard(Component.translatable("MineTranslator.ui.card.translation"))
            .add(row("MineTranslator.option.service", "MineTranslator.ui.desc.provider",
                new Dropdown(PROVIDERS, () -> draft.provider, value -> { draft.provider = value; markDirty(); })))
            .add(row("MineTranslator.option.source_language", "MineTranslator.ui.desc.source_language",
                new Dropdown(SOURCE_LANGUAGES, () -> draft.sourceLanguage, value -> { draft.sourceLanguage = value; markDirty(); })))
            .add(row("MineTranslator.option.target_language", "MineTranslator.ui.desc.target_language",
                new Dropdown(TARGET_LANGUAGES, () -> draft.targetLanguage, value -> { draft.targetLanguage = value; markDirty(); }))));
        cards.add(new SettingsCard(Component.translatable("MineTranslator.ui.card.appearance"))
            .add(row("MineTranslator.option.show_original", "MineTranslator.ui.desc.show_original",
                toggle(() -> draft.showOriginal, value -> draft.showOriginal = value)))
            .add(row("MineTranslator.option.ui_animations", "MineTranslator.ui.desc.ui_animations",
                toggle(() -> uiAnimations, value -> uiAnimations = value))));
    }

    private void buildChat() {
        cards.add(new SettingsCard(Component.translatable("MineTranslator.ui.card.automatic_translation"))
            .add(row("MineTranslator.option.auto_chat", "MineTranslator.ui.desc.auto_chat",
                toggle(() -> draft.autoTranslateEveryMessage, value -> draft.autoTranslateEveryMessage = value)))
            .add(row("MineTranslator.option.auto_player", "MineTranslator.ui.desc.auto_player",
                toggle(() -> draft.autoTranslatePlayerMessages, value -> draft.autoTranslatePlayerMessages = value)))
            .add(row("MineTranslator.option.auto_npc", "MineTranslator.ui.desc.auto_npc",
                toggle(() -> draft.translateOnlyNPC, value -> draft.translateOnlyNPC = value)))
            .add(row("MineTranslator.option.show_original", "MineTranslator.ui.desc.show_original",
                toggle(() -> draft.showOriginal, value -> draft.showOriginal = value)))
            .add(row("MineTranslator.option.preserve_formatting", "MineTranslator.ui.desc.preserve_formatting",
                toggle(() -> draft.preserveFormatting, value -> draft.preserveFormatting = value))));
        cards.add(new SettingsCard(Component.translatable("MineTranslator.ui.card.languages"))
            .add(row("MineTranslator.option.source_language", "MineTranslator.ui.desc.source_language",
                new Dropdown(SOURCE_LANGUAGES, () -> draft.sourceLanguage, value -> { draft.sourceLanguage=value; markDirty(); })))
            .add(row("MineTranslator.option.target_language", "MineTranslator.ui.desc.target_language",
                new Dropdown(TARGET_LANGUAGES, () -> draft.targetLanguage, value -> { draft.targetLanguage=value; markDirty(); }))));
        cards.add(new SettingsCard(Component.translatable("MineTranslator.ui.card.context"))
            .add(row("MineTranslator.option.use_context", "MineTranslator.ui.desc.context_ai_only",
                new ToggleSwitch(() -> draft.useContext, value -> { draft.useContext=value; markDirty(); }, this::providerSupportsContext)))
            .add(row("MineTranslator.option.context_size", "MineTranslator.ui.desc.context_size",
                new NumberStepper(() -> draft.contextSize, value -> { draft.contextSize=value; markDirty(); }, 1, 30, 1))));
    }

    private void buildItems() {
        cards.add(new SettingsCard(Component.translatable("MineTranslator.category.items"))
            .add(row("MineTranslator.option.auto_item_names", "MineTranslator.ui.desc.item_names",
                toggle(() -> draft.autoTranslateItemNames, value -> draft.autoTranslateItemNames = value)))
            .add(row("MineTranslator.option.auto_item_tooltips", "MineTranslator.ui.desc.item_tooltips",
                toggle(() -> draft.autoTranslateItemTooltips, value -> draft.autoTranslateItemTooltips = value)))
            .add(row("MineTranslator.option.translate_lore", "MineTranslator.ui.desc.translate_lore",
                toggle(() -> draft.translateItemLore, value -> draft.translateItemLore=value)))
            .add(row("MineTranslator.option.translate_abilities", "MineTranslator.ui.desc.translate_abilities",
                toggle(() -> draft.translateItemAbilities, value -> draft.translateItemAbilities=value)))
            .add(row("MineTranslator.option.translate_stats", "MineTranslator.ui.desc.translate_stats",
                toggle(() -> draft.translateItemStats, value -> draft.translateItemStats=value)))
            .add(row("MineTranslator.option.preserve_colors", "MineTranslator.ui.desc.item_styles",
                toggle(() -> draft.preserveItemColors, value -> draft.preserveItemColors=value)))
            .add(row("MineTranslator.option.translate_on_hold", "MineTranslator.ui.desc.translate_on_hold",
                toggle(() -> draft.translateItemsOnHold, value -> draft.translateItemsOnHold=value))));
        cards.add(new SettingsCard(Component.translatable("MineTranslator.ui.card.tooltip_preview"))
            .add(row("MineTranslator.ui.preview.original", "MineTranslator.ui.preview.item_original",
                new ValuePill(() -> Component.translatable("MineTranslator.ui.preview.item_translated").getString()))));
    }

    private void buildMyMessages() {
        cards.add(new SettingsCard(Component.translatable("MineTranslator.category.mymessages"))
            .add(row("MineTranslator.option.translate_my_messages", "MineTranslator.ui.desc.my_messages",
                toggle(() -> draft.translateMyMessages, value -> draft.translateMyMessages = value)))
            .add(row("MineTranslator.option.my_target_language", "MineTranslator.ui.desc.target_language",
                new Dropdown(TARGET_LANGUAGES, () -> draft.targetLanguage, value -> { draft.targetLanguage = value; markDirty(); })))
            .add(row("MineTranslator.option.outgoing_source", "MineTranslator.ui.desc.outgoing_source",
                new Dropdown(TARGET_LANGUAGES, () -> draft.outgoingSourceLanguage, value -> { draft.outgoingSourceLanguage=value; markDirty(); })))
            .add(row("MineTranslator.option.outgoing_preview", "MineTranslator.ui.desc.outgoing_preview",
                toggle(() -> draft.outgoingPreview, value -> draft.outgoingPreview=value)))
            .add(row("MineTranslator.option.replace_input", "MineTranslator.ui.desc.replace_input",
                toggle(() -> draft.replaceInputText, value -> draft.replaceInputText=value)))
            .add(row("MineTranslator.option.send_automatically", "MineTranslator.ui.desc.send_automatically",
                toggle(() -> draft.sendAutomatically, value -> draft.sendAutomatically=value)))
            .add(row("MineTranslator.option.send_original_error", "MineTranslator.ui.desc.send_original_error",
                toggle(() -> draft.sendOriginalOnError, value -> draft.sendOriginalOnError=value)))
            .add(row("MineTranslator.option.outgoing_context", "MineTranslator.ui.desc.outgoing_context",
                toggle(() -> draft.outgoingPlayerContext, value -> draft.outgoingPlayerContext=value))));
        cards.add(new SettingsCard(Component.translatable("MineTranslator.ui.card.message_preview"))
            .add(row("MineTranslator.ui.preview.source", "MineTranslator.ui.preview.outgoing_source",
                new ValuePill(() -> Component.translatable("MineTranslator.ui.preview.outgoing_result").getString()))));
    }

    private void buildKeys() {
        cards.add(new SettingsCard(Component.translatable("MineTranslator.category.keys"))
            .add(row("MineTranslator.config.keys_title", "MineTranslator.config.keys_desc",
                new ActionButton(Component.translatable("MineTranslator.config.controls"), () -> {
                    ReflectionAccess.openControls(minecraft);
                }, false))));
    }

    private void buildCache() {
        cards.add(new SettingsCard(Component.translatable("MineTranslator.category.advanced"))
            .add(row("MineTranslator.ui.cache.entries", "MineTranslator.ui.desc.cache_entries",
                new ValuePill(() -> "0 / 0")))
            .add(row("MineTranslator.ui.cache.hit_rate", "MineTranslator.ui.desc.cache_hit_rate",
                new ValuePill(() -> "0%")))
            .add(row("MineTranslator.ui.cache.active", "MineTranslator.ui.desc.cache_active",
                new ValuePill(() -> "0")))
            .add(row("MineTranslator.ui.cache.queued", "MineTranslator.ui.desc.cache_queued",
                new ValuePill(() -> "0")))
            .add(row("MineTranslator.option.fast_translation", "MineTranslator.ui.desc.fast_translation",
                toggle(() -> draft.fastTranslation, value -> draft.fastTranslation=value)))
            .add(row("MineTranslator.option.cache_enabled", "MineTranslator.ui.desc.cache_enabled",
                toggle(() -> draft.cacheEnabled, value -> draft.cacheEnabled=value)))
            .add(row("MineTranslator.option.cache_max", "MineTranslator.ui.desc.cache_max",
                new NumberStepper(() -> draft.cacheMaxSize, value -> { draft.cacheMaxSize=value; markDirty(); }, 100, 5000, 100)))
            .add(row("MineTranslator.option.parallel_requests", "MineTranslator.ui.desc.parallel_requests",
                new NumberStepper(() -> draft.parallelRequests, value -> { draft.parallelRequests=value; markDirty(); }, 1, 8, 1)))
            .add(row("MineTranslator.button.clear_cache", "MineTranslator.ui.desc.clear_cache",
                new ActionButton(Component.translatable("MineTranslator.button.clear_cache"),
                    () -> confirmClearCache = true, false))));
    }

    private void buildDebug() {
        cards.add(new SettingsCard(Component.translatable("MineTranslator.category.debug"))
            .add(row("MineTranslator.option.debug_logging", "MineTranslator.ui.desc.debug_logging",
                toggle(() -> draft.debugLogging, value -> draft.debugLogging = value)))
            .add(row("MineTranslator.option.show_message_type", "MineTranslator.ui.desc.show_message_type",
                toggle(() -> draft.showMessageType, value -> draft.showMessageType=value)))
            .add(row("MineTranslator.option.show_parser", "MineTranslator.ui.desc.show_parser",
                toggle(() -> draft.showParserStrategy, value -> draft.showParserStrategy=value)))
            .add(row("MineTranslator.option.fake_provider", "MineTranslator.ui.desc.fake_provider",
                toggle(() -> draft.fakeProviderDebug, value -> draft.fakeProviderDebug=value)))
            .add(row("MineTranslator.option.translate_every_incoming", "MineTranslator.ui.desc.translate_every_incoming",
                toggle(() -> draft.translateEveryIncomingMessageDebug, value -> draft.translateEveryIncomingMessageDebug=value)))
            .add(row("MineTranslator.ui.status.version", "MineTranslator.ui.desc.version", new ValuePill(() -> VERSION)))
            .add(row("MineTranslator.ui.status.provider", "MineTranslator.ui.desc.provider", new ValuePill(() -> draft.provider)))
            .add(row("MineTranslator.ui.status.hooks", "MineTranslator.ui.desc.hooks",
                new ValuePill(() -> Component.translatable("MineTranslator.status.active").getString())))
            .add(row("MineTranslator.ui.status.queue", "MineTranslator.ui.desc.queue",
                new ValuePill(() -> "0")))
            .add(row("MineTranslator.button.copy_diagnostics", "MineTranslator.ui.desc.copy_diagnostics",
                new ActionButton(Component.translatable("MineTranslator.button.copy_diagnostics"), this::copyDiagnostics, false))));
    }

    private void buildProviderCards() {
        providerCards.clear();
        providerCards.add(new ProviderCard("Google", Component.translatable("MineTranslator.ui.provider.google"), false));
        providerCards.add(new ProviderCard("DeepL", Component.translatable("MineTranslator.ui.provider.deepl"), true));
        providerCards.add(new ProviderCard("Gemini", Component.translatable("MineTranslator.ui.provider.gemini"), true));
        providerCards.add(new ProviderCard("Claude", Component.translatable("MineTranslator.ui.provider.claude"), true));
        providerCards.add(new ProviderCard("OpenAI", Component.translatable("MineTranslator.ui.provider.openai"), true));
        providerCards.add(new ProviderCard("Fake", Component.translatable("MineTranslator.ui.provider.fake"), false));
    }

    private void buildProviderSettings() {
        providerSettings = new SettingsCard(Component.translatable("MineTranslator.ui.card.provider_settings"))
            .add(row("MineTranslator.option.api_key", "MineTranslator.ui.desc.api_key",
                new SecretInput(() -> draft.apiKey, value -> { draft.apiKey = value; markDirty(); })))
            .add(row("MineTranslator.option.model", "MineTranslator.ui.desc.model",
                new TextInput(() -> draft.model, value -> { draft.model = value; markDirty(); })))
            .add(row("MineTranslator.option.base_url", "MineTranslator.ui.desc.base_url",
                new TextInput(() -> draft.baseUrl, value -> { draft.baseUrl = value; markDirty(); })))
            .add(row("MineTranslator.option.timeout", "MineTranslator.ui.desc.timeout",
                new NumberStepper(() -> draft.requestTimeoutSeconds,
                    value -> { draft.requestTimeoutSeconds = value; markDirty(); }, 1, 60, 1)))
            .add(row("MineTranslator.button.test_connection", "MineTranslator.ui.desc.test_connection",
                new ActionButton(Component.translatable(providerTesting
                    ? "MineTranslator.status.testing" : "MineTranslator.button.test_connection"), this::testProvider, true)))
            .add(row("MineTranslator.ui.status.connection", "MineTranslator.ui.desc.connection_result",
                new ValuePill(() -> Component.translatable(providerTestResultKey).getString())));
    }

    private void testProvider() {
        if (providerTesting) return;
        providerTesting = true;
        providerTestResultKey = "MineTranslator.status.testing";
        buildProviderSettings();
        String provider = draft.provider;
        String key = draft.apiKey;
        CompletableFuture.supplyAsync(() -> ProviderConnectionController.test(provider, key)).thenAccept(result ->
            minecraft.execute(() -> {
                providerTesting = false;
                providerTestResultKey = switch (result) {
                    case SUCCESS -> "MineTranslator.status.test_success";
                    case INVALID_KEY -> "MineTranslator.status.invalid_key";
                    case MODEL_NOT_FOUND -> "MineTranslator.status.model_not_found";
                    case RATE_LIMIT -> "MineTranslator.status.rate_limit";
                    case NETWORK_ERROR -> "MineTranslator.status.network_error";
                };
                buildProviderSettings();
            }));
    }

    private SettingRow row(String labelKey, String descriptionKey, UiControl control) {
        return new SettingRow(Component.translatable(labelKey), Component.translatable(descriptionKey), control);
    }

    private ToggleSwitch toggle(java.util.function.BooleanSupplier getter, java.util.function.Consumer<Boolean> setter) {
        return new ToggleSwitch(getter, value -> { setter.accept(value); markDirty(); });
    }

    private void positionSidebar() {
        int x = layout.panelX() + 8;
        int y = layout.panelY() + layout.headerHeight() + 10;
        int available = layout.panelHeight() - layout.headerHeight() - layout.footerHeight() - 20;
        int gap = available < 230 ? 1 : 4;
        int desiredHeight = layout.compact() ? 29 : 33;
        int itemHeight = Math.max(16, Math.min(desiredHeight,
            (available - gap * (sidebarButtons.size() - 1)) / sidebarButtons.size()));
        for (int i = 0; i < sidebarButtons.size(); i++) {
            sidebarButtons.get(i).setBounds(x, y + i * (itemHeight + gap), layout.sidebarWidth() - 16, itemHeight);
        }
    }

    private void selectCategory(Category category) {
        scrollPositions.put(selected, scroll.scrollValue());
        selected = category;
        focusedSidebarIndex = category.ordinal();
        buildCards();
        scroll.setScrollValue(scrollPositions.getOrDefault(category, 0.0));
    }

    private int cardsContentHeight(boolean compact) {
        int result = 0;
        for (SettingsCard card : cards) result += card.preferredHeight(compact) + 10;
        return Math.max(0, result - 10);
    }

    private int providerContentHeight(int width) {
        int columns = width >= 440 ? 2 : 1;
        int cardsHeight = ((providerCards.size() + columns - 1) / columns) * 78 - 10;
        return cardsHeight + 14 + (providerSettings == null ? 0 : providerSettings.preferredHeight(layout != null && layout.compact()));
    }

    private void markDirty() { dirty = true; }

    private void save() {
        draft.applyTo(config);
        config.save();
        if (minecraft != null && minecraft.options != null) minecraft.options.save();
        dirty = false;
        savedUntil = System.currentTimeMillis() + 1400L;
    }

    private void resetSection() {
        ModConfig defaults = new ModConfig();
        switch (selected) {
            case GENERAL -> {
                draft.provider = defaults.provider;
                draft.sourceLanguage = defaults.sourceLanguage;
                draft.targetLanguage = defaults.targetLanguage;
                draft.showOriginal = defaults.showOriginal;
            }
            case CHAT -> {
                draft.autoTranslateEveryMessage = defaults.autoTranslateEveryMessage;
                draft.autoTranslatePlayerMessages = defaults.autoTranslatePlayerMessages;
                draft.translateOnlyNPC = defaults.translateOnlyNPC;
            }
            case ITEMS -> {
                draft.autoTranslateItemNames = defaults.autoTranslateItemNames;
                draft.autoTranslateItemTooltips = defaults.autoTranslateItemTooltips;
            }
            case MY_MESSAGES -> draft.translateMyMessages = defaults.translateMyMessages;
            case DEBUG -> draft.debugLogging = defaults.debugLogging;
            case PROVIDERS -> draft.provider = defaults.provider;
            case KEYS -> {
                net.minecraft.client.KeyMapping.resetMapping();
            }
            default -> { }
        }
        markDirty();
        buildCards();
    }

    private boolean handleConfirmClick(double mouseX, double mouseY, int button) {
        if (button != 0) return true;
        int dialogW = Math.min(380, width - 30);
        int dialogH = 116;
        int x = (width - dialogW) / 2;
        int y = (height - dialogH) / 2;
        int buttonY = y + dialogH - 34;
        int bw = (dialogW - 44) / 3;
        if (contains(mouseX, mouseY, x + 10, buttonY, bw, 22)) {
            save(); confirmClose = false; minecraft.setScreen(parent); return true;
        }
        if (contains(mouseX, mouseY, x + 17 + bw, buttonY, bw, 22)) {
            dirty = false; confirmClose = false; minecraft.setScreen(parent); return true;
        }
        if (contains(mouseX, mouseY, x + 24 + bw * 2, buttonY, bw, 22)) {
            confirmClose = false; return true;
        }
        return true;
    }

    private boolean handleClearCacheClick(double mouseX, double mouseY, int button) {
        if (button != 0) return true;
        int dialogW = Math.min(360, width - 30), dialogH = 100;
        int x = (width - dialogW) / 2, y = (height - dialogH) / 2;
        int buttonY = y + dialogH - 32;
        if (contains(mouseX, mouseY, x + dialogW - 188, buttonY, 100, 22)) {
            PortController.get().clearCache(); confirmClearCache = false; return true;
        }
        if (contains(mouseX, mouseY, x + dialogW - 80, buttonY, 68, 22)) {
            confirmClearCache = false; return true;
        }
        return true;
    }

    private List<UiControl> visibleControls() {
        List<UiControl> result = new ArrayList<>();
        if (selected == Category.PROVIDERS && providerSettings != null) {
            for (SettingRow row : providerSettings.rows()) result.add(row.control());
        } else {
            for (SettingsCard card : cards) for (SettingRow row : card.rows()) result.add(row.control());
        }
        return result;
    }

    private void copyDiagnostics() {
        if (minecraft == null) return;
        String report = "MineTranslator " + VERSION + "\n"
            + "Provider: " + draft.provider + "\n"
            + "Chat interceptors: 1\n"
            + "Tooltip hook: active\n"
            + "Queue: 0\n"
            + "Cache: 0/0\n"
            + "Last error: none";
        minecraft.keyboardHandler.setClipboard(report);
    }

    private void drawButton(GuiGraphics graphics, Component label, int x, int y, int w, int h,
                            boolean primary, boolean hovered) {
        int base = primary ? UiTheme.PRIMARY : UiTheme.CARD;
        if (hovered) base = UiTheme.mix(base, UiTheme.TEXT, 0.12F);
        UiTheme.roundedFill(graphics, x, y, w, h, base);
        UiTheme.border(graphics, x, y, w, h, primary ? UiTheme.PRIMARY : UiTheme.BORDER);
        int color = primary ? 0xFF171A20 : UiTheme.TEXT;
        String value = label.getString();
        if (font.width(value) <= w - 12) graphics.drawCenteredString(font, value, x + w / 2, y + 7, color);
        else UiTheme.drawEllipsizedText(graphics, font, label, x + 6, y + 7, w - 12, color);
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static boolean providerNeedsKey(String provider) {
        return !provider.equals("Google") && !provider.equals("Fake");
    }

    private boolean providerSupportsContext() {
        return draft.provider.equals("Gemini") || draft.provider.equals("Claude") || draft.provider.equals("OpenAI");
    }

    private enum Category {
        GENERAL("G", "MineTranslator.category.general", "MineTranslator.ui.category.general.desc"),
        PROVIDERS("P", "MineTranslator.category.providers", "MineTranslator.ui.category.providers.desc"),
        CHAT("C", "MineTranslator.category.chat", "MineTranslator.ui.category.chat.desc"),
        ITEMS("I", "MineTranslator.category.items", "MineTranslator.ui.category.items.desc"),
        MY_MESSAGES("M", "MineTranslator.category.mymessages", "MineTranslator.ui.category.mymessages.desc"),
        KEYS("K", "MineTranslator.category.keys", "MineTranslator.ui.category.keys.desc"),
        CACHE("F", "MineTranslator.category.advanced", "MineTranslator.ui.category.cache.desc"),
        DEBUG("D", "MineTranslator.category.debug", "MineTranslator.ui.category.debug.desc");

        final String icon;
        final String titleKey;
        final String descriptionKey;

        Category(String icon, String titleKey, String descriptionKey) {
            this.icon = icon;
            this.titleKey = titleKey;
            this.descriptionKey = descriptionKey;
        }
    }
}
