package net.kingchoka.minetranslator.core;

import net.minecraft.network.chat.Component;
import net.kingchoka.minetranslator.api.ComponentizableEnum;
import net.kingchoka.minetranslator.api.IServiceProvider;
import org.jetbrains.annotations.NotNull;

/**
 * The enum of translation services.
 * Involves a name and a provider instance.
 */
public enum TranslationService implements ComponentizableEnum {
    GoogleTranslation("Google Translation", GoogleTranslationProvider.INSTANCE),
    OpenAIClient("OpenAI Client", OpenAIClientProvider.INSTANCE),
    DeepLTranslation("DeepL Translation", DeepLTranslationProvider.INSTANCE),
    Gemini("Gemini", GeminiProvider.INSTANCE),
    Claude("Claude", ClaudeProvider.INSTANCE);

    public final String displayName;
    public final IServiceProvider provider;

    TranslationService(String displayName, IServiceProvider provider) {
        this.displayName = displayName;
        this.provider = provider;
    }

    @Override
    public @NotNull Component toComponent() {
        return Component.literal(this.displayName);
    }
}
