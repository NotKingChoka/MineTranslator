package net.kingchoka.minetranslator.config.gui;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.kingchoka.minetranslator.cache.TranslationCache;
import net.kingchoka.minetranslator.config.ModConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

public class MTConfigScreen {
    public static Screen create(Screen parent) {
        ModConfig config = ModConfig.getInstance();
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("MineTranslator.screen.title"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("MineTranslator.category.general"));

        general.addEntry(entryBuilder.startStringDropdownMenu(Component.translatable("MineTranslator.option.service"), config.provider)
            .setSelections(List.of("Google", "Fake", "DeepL", "Gemini", "Claude", "OpenAI"))
            .setDefaultValue("Google")
            .setSaveConsumer(val -> config.provider = val)
            .build());

        general.addEntry(entryBuilder.startTextField(Component.translatable("MineTranslator.option.api_key"), config.apiKey)
            .setDefaultValue("")
            .setSaveConsumer(val -> config.apiKey = val)
            .build());

        general.addEntry(entryBuilder.startTextField(Component.translatable("MineTranslator.option.source_language"), config.sourceLanguage)
            .setDefaultValue("auto")
            .setSaveConsumer(val -> config.sourceLanguage = val)
            .build());

        general.addEntry(entryBuilder.startTextField(Component.translatable("MineTranslator.option.target_language"), config.targetLanguage)
            .setDefaultValue("ru")
            .setSaveConsumer(val -> config.targetLanguage = val)
            .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("MineTranslator.option.auto_chat"), config.autoTranslateEveryMessage)
            .setDefaultValue(false)
            .setSaveConsumer(val -> config.autoTranslateEveryMessage = val)
            .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("MineTranslator.option.auto_npc"), config.translateOnlyNPC)
            .setDefaultValue(false)
            .setSaveConsumer(val -> config.translateOnlyNPC = val)
            .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("MineTranslator.option.auto_player"), config.autoTranslatePlayerMessages)
            .setDefaultValue(false)
            .setSaveConsumer(val -> config.autoTranslatePlayerMessages = val)
            .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("MineTranslator.option.translate_my_messages"), config.translateMyMessages)
            .setDefaultValue(false)
            .setSaveConsumer(val -> config.translateMyMessages = val)
            .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("MineTranslator.option.auto_item_names"), config.autoTranslateItemNames)
            .setDefaultValue(false)
            .setSaveConsumer(val -> config.autoTranslateItemNames = val)
            .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("MineTranslator.option.auto_item_tooltips"), config.autoTranslateItemTooltips)
            .setDefaultValue(false)
            .setSaveConsumer(val -> config.autoTranslateItemTooltips = val)
            .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("MineTranslator.option.show_original"), config.showOriginal)
            .setDefaultValue(false)
            .setSaveConsumer(val -> config.showOriginal = val)
            .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("MineTranslator.option.debug_logging"), config.debugLogging)
            .setDefaultValue(true)
            .setSaveConsumer(val -> config.debugLogging = val)
            .build());

        general.addEntry(entryBuilder.startTextDescription(Component.translatable("MineTranslator.desc.clear_cache"))
            .build());

        builder.setSavingRunnable(() -> {
            config.save();
            TranslationCache.getInstance().clear();
        });

        return builder.build();
    }
}
