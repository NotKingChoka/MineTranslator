package net.kingchoka.minetranslator.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig INSTANCE;

    public String provider = "Google";
    public String apiKey = "";
    public String sourceLanguage = "auto";
    public String targetLanguage = "ru";
    public boolean autoTranslateEveryMessage = false;
    public boolean autoTranslatePlayerMessages = false;
    public boolean autoTranslateItemNames = false;
    public boolean autoTranslateItemTooltips = false;
    public boolean showOriginal = false;
    public boolean debugLogging = true;
    public boolean translateMyMessages = false;
    public boolean translateOnlyNPC = false;

    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        File configFile = getConfigFile();
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
                INSTANCE = GSON.fromJson(reader, ModConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new ModConfig();
                }
            } catch (Exception e) {
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
            INSTANCE.save();
        }
    }

    public void save() {
        File configFile = getConfigFile();
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileWriter writer = new FileWriter(configFile, StandardCharsets.UTF_8)) {
            GSON.toJson(this, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static File getConfigFile() {
        return new File(Minecraft.getInstance().gameDirectory, "config/minetranslator_v3.json");
    }
}
