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
            .setTitle(Component.literal("MineTranslator v3 Settings"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));

        general.addEntry(entryBuilder.startStringDropdownMenu(Component.literal("Translation Provider"), config.provider)
            .setSelections(List.of("Google", "Fake", "DeepL", "Gemini", "Claude", "OpenAI"))
            .setDefaultValue("Google")
            .setSaveConsumer(val -> config.provider = val)
            .build());

        general.addEntry(entryBuilder.startTextField(Component.literal("API Key"), config.apiKey)
            .setDefaultValue("")
            .setSaveConsumer(val -> config.apiKey = val)
            .build());

        general.addEntry(entryBuilder.startTextField(Component.literal("Source Language"), config.sourceLanguage)
            .setDefaultValue("auto")
            .setSaveConsumer(val -> config.sourceLanguage = val)
            .build());

        general.addEntry(entryBuilder.startTextField(Component.literal("Target Language"), config.targetLanguage)
            .setDefaultValue("ru")
            .setSaveConsumer(val -> config.targetLanguage = val)
            .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Auto Translate Every Chat Message"), config.autoTranslateEveryMessage)
            .setDefaultValue(false)
            .setSaveConsumer(val -> config.autoTranslateEveryMessage = val)
            .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Auto Translate Player Messages"), config.autoTranslatePlayerMessages)
            .setDefaultValue(false)
            .setSaveConsumer(val -> config.autoTranslatePlayerMessages = val)
            .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Auto Translate Item Names"), config.autoTranslateItemNames)
            .setDefaultValue(false)
            .setSaveConsumer(val -> config.autoTranslateItemNames = val)
            .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Auto Translate Item Tooltips"), config.autoTranslateItemTooltips)
            .setDefaultValue(false)
            .setSaveConsumer(val -> config.autoTranslateItemTooltips = val)
            .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Show Original Text with Translation"), config.showOriginal)
            .setDefaultValue(false)
            .setSaveConsumer(val -> config.showOriginal = val)
            .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Debug Logging"), config.debugLogging)
            .setDefaultValue(true)
            .setSaveConsumer(val -> config.debugLogging = val)
            .build());

        general.addEntry(entryBuilder.startTextDescription(Component.literal("To clear translation cache, click below"))
            .build());

        builder.setSavingRunnable(() -> {
            config.save();
            TranslationCache.getInstance().clear();
        });

        return builder.build();
    }
}
