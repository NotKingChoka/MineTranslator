package net.kingchoka.minetranslator.compat;

public interface IMTCompat {
    /**
     * Opens the mod's configuration screen.
     * @param parentScreen The parent screen to return to, as a raw Object to avoid compile-time Minecraft dependency.
     */
    void openConfigScreen(Object parentScreen);

    /**
     * Creates and returns the mod's configuration screen instance.
     * @param parentScreen The parent screen.
     * @return The screen instance as an Object.
     */
    Object createConfigScreen(Object parentScreen);

    /**
     * Gets the game's current language code (e.g., "en_us").
     */
    String getGameLanguage();
}
