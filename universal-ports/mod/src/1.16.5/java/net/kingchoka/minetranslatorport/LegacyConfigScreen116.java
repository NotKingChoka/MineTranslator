package net.kingchoka.minetranslatorport;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class LegacyConfigScreen116 extends Screen {
    private static final int BACKGROUND = 0xF00F1115;
    private static final int PANEL = 0xF2171A20;
    private static final int CARD = 0xF21D2129;
    private static final int BORDER = 0xFF343A46;
    private static final int ACCENT = 0xFFF5A623;
    private static final int TEXT = 0xFFF2F2F2;
    private static final int MUTED = 0xFFA7AFBD;
    private static final int SECONDARY = 0xFF36CFE5;

    private final Screen parent;
    private final PortController controller = PortController.get();
    private int selected;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int sidebarWidth;
    private int controlWidth;
    private Button languageButton;
    private Button autoPlayersButton;
    private Button controlsButton;

    public LegacyConfigScreen116(Screen parent) {
        super(literal("MineTranslator"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(720, Math.max(300, this.width - 16));
        panelHeight = Math.min(410, Math.max(210, this.height - 12));
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;
        sidebarWidth = panelWidth < 520 ? 112 : 165;
        controlWidth = panelWidth < 520 ? 112 : 170;

        String[] categories = {
            "MineTranslator.category.general",
            "MineTranslator.category.providers",
            "MineTranslator.category.chat",
            "MineTranslator.category.items",
            "MineTranslator.category.keys",
            "MineTranslator.category.debug"
        };
        for (int i = 0; i < categories.length; i++) {
            final int category = i;
            this.addRenderableWidget(new Button(
                panelX + 8, panelY + 49 + i * 23, sidebarWidth - 16, 19,
                tr(categories[i]),
                button -> {
                    selected = category;
                    updateVisibility();
                }
            ));
        }

        languageButton = this.addRenderableWidget(new Button(
            panelX + panelWidth - controlWidth - 12, panelY + 105, controlWidth, 22,
            languageLabel(),
            button -> {
                controller.cycleTargetLanguage();
                button.setMessage(languageLabel());
            }
        ));
        autoPlayersButton = this.addRenderableWidget(new Button(
            panelX + panelWidth - controlWidth - 12, panelY + 151, controlWidth, 22,
            autoPlayersLabel(),
            button -> {
                controller.toggleAutoTranslatePlayerMessages();
                button.setMessage(autoPlayersLabel());
            }
        ));
        controlsButton = this.addRenderableWidget(new Button(
            panelX + panelWidth - controlWidth - 12, panelY + 105, controlWidth, 22,
            tr("MineTranslator.config.controls"),
            button -> ReflectionAccess.openControls(this.minecraft)
        ));

        this.addRenderableWidget(new Button(
            panelX + panelWidth - 224, panelY + panelHeight - 34, 100, 22,
            tr("MineTranslator.config.cancel"),
            button -> this.minecraft.setScreen(parent)
        ));
        this.addRenderableWidget(new Button(
            panelX + panelWidth - 116, panelY + panelHeight - 34, 104, 22,
            tr("MineTranslator.config.save"),
            button -> {
                controller.saveConfig();
                this.minecraft.setScreen(parent);
            }
        ));
        updateVisibility();
    }

    private void updateVisibility() {
        languageButton.visible = selected == 0 || selected == 1;
        autoPlayersButton.visible = selected == 0 || selected == 2;
        controlsButton.visible = selected == 4;
    }

    private Component languageLabel() {
        return tr("MineTranslator.config.target_language", controller.targetLanguage().toUpperCase());
    }

    private Component autoPlayersLabel() {
        return tr(controller.autoTranslatePlayerMessages()
            ? "MineTranslator.config.enabled" : "MineTranslator.config.disabled");
    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        fill(matrices, 0, 0, this.width, this.height, BACKGROUND);
        fill(matrices, panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL);
        fill(matrices, panelX, panelY, panelX + panelWidth, panelY + 43, CARD);
        fill(matrices, panelX, panelY + 42, panelX + panelWidth, panelY + 43, BORDER);
        fill(matrices, panelX + sidebarWidth - 1, panelY + 43, panelX + sidebarWidth, panelY + panelHeight - 44, BORDER);
        fill(matrices, panelX + sidebarWidth, panelY + panelHeight - 44,
            panelX + panelWidth, panelY + panelHeight - 43, BORDER);
        fill(matrices, panelX + 12, panelY + 11, panelX + 34, panelY + 33, ACCENT);

        drawLeft(matrices, "M", panelX + 19, panelY + 18, 0xFF171A20, 10);
        drawLeft(matrices, "MineTranslator", panelX + 43, panelY + 11, TEXT, panelWidth - 55);
        drawLeft(matrices, "v3.0.0-alpha.1 • Fabric Legacy", panelX + 43, panelY + 24, MUTED, panelWidth - 55);
        drawLeft(matrices, titleForSelected(), panelX + sidebarWidth + 12, panelY + 57, TEXT,
            panelWidth - sidebarWidth - 24);
        if (panelWidth >= 430) {
            drawLeft(matrices, descriptionForSelected(), panelX + sidebarWidth + 12, panelY + 73, MUTED,
                panelWidth - sidebarWidth - 24);
        }

        if (selected == 0 || selected == 1) {
            drawCard(matrices, panelY + 94, "MineTranslator.config.language_title", "MineTranslator.config.language_desc");
        }
        if (selected == 0 || selected == 2) {
            drawCard(matrices, panelY + 140, "MineTranslator.config.players_title", "MineTranslator.config.players_desc");
        }
        if (selected == 3) {
            drawInfo(matrices, panelY + 94, "MineTranslator.config.items_info", SECONDARY);
        } else if (selected == 4) {
            drawCard(matrices, panelY + 94, "MineTranslator.config.keys_title", "MineTranslator.config.keys_desc");
        } else if (selected == 5) {
            drawInfo(matrices, panelY + 94, "MineTranslator.config.debug_info", SECONDARY);
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void drawCard(PoseStack matrices, int y, String title, String description) {
        int x = panelX + sidebarWidth + 6;
        int right = panelX + panelWidth - 12;
        fill(matrices, x, y, right, y + 40, CARD);
        fill(matrices, x, y, x + 2, y + 40, ACCENT);
        int textWidth = Math.max(48, panelX + panelWidth - controlWidth - 24 - (x + 10));
        drawLeft(matrices, tr(title).getString(), x + 10, y + 8, TEXT, textWidth);
        drawLeft(matrices, tr(description).getString(), x + 10, y + 23, MUTED, textWidth);
    }

    private void drawInfo(PoseStack matrices, int y, String key, int color) {
        int x = panelX + sidebarWidth + 6;
        fill(matrices, x, y, panelX + panelWidth - 12, y + 50, CARD);
        drawLeft(matrices, tr(key).getString(), x + 12, y + 18, color,
            panelX + panelWidth - x - 24);
    }

    private void drawLeft(PoseStack matrices, String text, int x, int y, int color, int maxWidth) {
        drawString(matrices, this.font, ellipsize(text, maxWidth), x, y, color);
    }

    private String ellipsize(String text, int maxWidth) {
        if (text == null) return "";
        if (maxWidth <= 0 || this.font.width(text) <= maxWidth) return text;
        String suffix = "...";
        int suffixWidth = this.font.width(suffix);
        int end = text.length();
        while (end > 0 && this.font.width(text.substring(0, end)) + suffixWidth > maxWidth) end--;
        return end == 0 ? suffix : text.substring(0, end) + suffix;
    }

    private String titleForSelected() {
        String[] keys = {
            "MineTranslator.category.general", "MineTranslator.category.providers",
            "MineTranslator.category.chat", "MineTranslator.category.items",
            "MineTranslator.category.keys", "MineTranslator.category.debug"
        };
        return tr(keys[selected]).getString();
    }

    private String descriptionForSelected() {
        return tr("MineTranslator.config.section_desc").getString();
    }

    private Component tr(String key, Object... args) {
        return translatable(key, args);
    }

    private static Component literal(String text) {
        try {
            return (Component) Component.class.getMethod("literal", String.class).invoke(null, text);
        } catch (Exception e) {
            try {
                Class<?> textCompClass = Class.forName("net.minecraft.network.chat.TextComponent");
                return (Component) textCompClass.getConstructor(String.class).newInstance(text);
            } catch (Exception ex) {
                try {
                    Class<?> literalClass = Class.forName("net.minecraft.class_2585");
                    return (Component) literalClass.getConstructor(String.class).newInstance(text);
                } catch (Exception ex2) {
                    throw new RuntimeException(ex2);
                }
            }
        }
    }

    private static Component translatable(String key, Object... args) {
        try {
            return (Component) Component.class.getMethod("translatable", String.class, Object[].class).invoke(null, key, args);
        } catch (Exception e) {
            try {
                Class<?> transCompClass = Class.forName("net.minecraft.network.chat.TranslatableComponent");
                return (Component) transCompClass.getConstructor(String.class, Object[].class).newInstance(key, args);
            } catch (Exception ex) {
                try {
                    Class<?> transClass = Class.forName("net.minecraft.class_2588");
                    return (Component) transClass.getConstructor(String.class, Object[].class).newInstance(key, args);
                } catch (Exception ex2) {
                    throw new RuntimeException(ex2);
                }
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
