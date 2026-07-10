package net.kingchoka.minetranslator.translation.provider;

import net.kingchoka.minetranslator.translation.TranslationProvider;

public class FakeTranslationProvider implements TranslationProvider {
    @Override
    public String translate(String text, String sourceLang, String targetLang) throws Exception {
        return "[RU] " + text;
    }
}
