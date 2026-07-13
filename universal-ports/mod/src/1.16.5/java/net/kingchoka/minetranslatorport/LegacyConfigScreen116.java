package net.kingchoka.minetranslatorport;

import net.minecraft.class_2588;
import net.minecraft.class_2585;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_4587;

/** Dark MineTranslator settings screen implemented against the stable 1.16.5 intermediary API. */
public final class LegacyConfigScreen116 extends class_437 {
    private static final int BACKGROUND = 0xF00F1115;
    private static final int PANEL = 0xF2171A20;
    private static final int CARD = 0xF21D2129;
    private static final int BORDER = 0xFF343A46;
    private static final int ACCENT = 0xFFF5A623;
    private static final int TEXT = 0xFFF2F2F2;
    private static final int MUTED = 0xFFA7AFBD;
    private static final int SECONDARY = 0xFF36CFE5;

    private final class_437 parent;
    private final PortController controller = PortController.get();
    private int selected;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int sidebarWidth;
    private int controlWidth;
    private class_4185 languageButton;
    private class_4185 autoPlayersButton;
    private class_4185 controlsButton;

    public LegacyConfigScreen116(class_437 parent) {
        super(new class_2585("MineTranslator"));
        this.parent = parent;
    }

    @Override
    protected void method_25426() {
        panelWidth = Math.min(720, Math.max(300, this.field_22789 - 16));
        panelHeight = Math.min(410, Math.max(210, this.field_22790 - 12));
        panelX = (this.field_22789 - panelWidth) / 2;
        panelY = (this.field_22790 - panelHeight) / 2;
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
            this.method_25411(new class_4185(
                panelX + 8, panelY + 49 + i * 23, sidebarWidth - 16, 19,
                tr(categories[i]),
                button -> {
                    selected = category;
                    updateVisibility();
                }
            ));
        }

        languageButton = this.method_25411(new class_4185(
            panelX + panelWidth - controlWidth - 12, panelY + 105, controlWidth, 22,
            languageLabel(),
            button -> {
                controller.cycleTargetLanguage();
                button.method_25355(languageLabel());
            }
        ));
        autoPlayersButton = this.method_25411(new class_4185(
            panelX + panelWidth - controlWidth - 12, panelY + 151, controlWidth, 22,
            autoPlayersLabel(),
            button -> {
                controller.toggleAutoTranslatePlayerMessages();
                button.method_25355(autoPlayersLabel());
            }
        ));
        controlsButton = this.method_25411(new class_4185(
            panelX + panelWidth - controlWidth - 12, panelY + 105, controlWidth, 22,
            tr("MineTranslator.config.controls"),
            button -> ReflectionAccess.openControls(this.field_22787)
        ));

        this.method_25411(new class_4185(
            panelX + panelWidth - 224, panelY + panelHeight - 34, 100, 22,
            tr("MineTranslator.config.cancel"),
            button -> this.field_22787.method_1507(parent)
        ));
        this.method_25411(new class_4185(
            panelX + panelWidth - 116, panelY + panelHeight - 34, 104, 22,
            tr("MineTranslator.config.save"),
            button -> {
                controller.saveConfig();
                this.field_22787.method_1507(parent);
            }
        ));
        updateVisibility();
    }

    private void updateVisibility() {
        languageButton.field_22764 = selected == 0 || selected == 1;
        autoPlayersButton.field_22764 = selected == 0 || selected == 2;
        controlsButton.field_22764 = selected == 4;
    }

    private class_2561 languageLabel() {
        return tr("MineTranslator.config.target_language", controller.targetLanguage().toUpperCase());
    }

    private class_2561 autoPlayersLabel() {
        return tr(controller.autoTranslatePlayerMessages()
            ? "MineTranslator.config.enabled" : "MineTranslator.config.disabled");
    }

    @Override
    public void method_25394(class_4587 matrices, int mouseX, int mouseY, float delta) {
        class_332.method_25294(matrices, 0, 0, this.field_22789, this.field_22790, BACKGROUND);
        class_332.method_25294(matrices, panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL);
        class_332.method_25294(matrices, panelX, panelY, panelX + panelWidth, panelY + 43, CARD);
        class_332.method_25294(matrices, panelX, panelY + 42, panelX + panelWidth, panelY + 43, BORDER);
        class_332.method_25294(matrices, panelX + sidebarWidth - 1, panelY + 43, panelX + sidebarWidth, panelY + panelHeight - 44, BORDER);
        class_332.method_25294(matrices, panelX + sidebarWidth, panelY + panelHeight - 44,
            panelX + panelWidth, panelY + panelHeight - 43, BORDER);
        class_332.method_25294(matrices, panelX + 12, panelY + 11, panelX + 34, panelY + 33, ACCENT);

        drawLeft(matrices, "M", panelX + 19, panelY + 18, 0xFF171A20, 10);
        drawLeft(matrices, "MineTranslator", panelX + 43, panelY + 11, TEXT, panelWidth - 55);
        drawLeft(matrices, "v3.0.0-alpha.1 • Fabric 1.16.5", panelX + 43, panelY + 24, MUTED, panelWidth - 55);
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

        super.method_25394(matrices, mouseX, mouseY, delta);
    }

    private void drawCard(class_4587 matrices, int y, String title, String description) {
        int x = panelX + sidebarWidth + 6;
        int right = panelX + panelWidth - 12;
        class_332.method_25294(matrices, x, y, right, y + 40, CARD);
        class_332.method_25294(matrices, x, y, x + 2, y + 40, ACCENT);
        int textWidth = Math.max(48, panelX + panelWidth - controlWidth - 24 - (x + 10));
        drawLeft(matrices, tr(title).getString(), x + 10, y + 8, TEXT, textWidth);
        drawLeft(matrices, tr(description).getString(), x + 10, y + 23, MUTED, textWidth);
    }

    private void drawInfo(class_4587 matrices, int y, String key, int color) {
        int x = panelX + sidebarWidth + 6;
        class_332.method_25294(matrices, x, y, panelX + panelWidth - 12, y + 50, CARD);
        drawLeft(matrices, tr(key).getString(), x + 12, y + 18, color,
            panelX + panelWidth - x - 24);
    }

    private void drawLeft(class_4587 matrices, String text, int x, int y, int color, int maxWidth) {
        class_332.method_25303(matrices, this.field_22793, ellipsize(text, maxWidth), x, y, color);
    }

    private String ellipsize(String text, int maxWidth) {
        if (text == null) return "";
        if (maxWidth <= 0 || this.field_22793.method_1727(text) <= maxWidth) return text;
        String suffix = "...";
        int suffixWidth = this.field_22793.method_1727(suffix);
        int end = text.length();
        while (end > 0 && this.field_22793.method_1727(text.substring(0, end)) + suffixWidth > maxWidth) end--;
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

    private class_2561 tr(String key, Object... args) {
        class_2588 translated = new class_2588(key, args);
        String resolved = translated.getString();
        if (!resolved.equals(key)) return translated;
        String fallback = fallback(key);
        if (args.length > 0) {
            try { fallback = String.format(fallback, args); } catch (Exception ignored) {}
        }
        return new class_2585(fallback);
    }

    private String fallback(String key) {
        if ("MineTranslator.category.general".equals(key)) return "Общие";
        if ("MineTranslator.category.providers".equals(key)) return "Переводчики";
        if ("MineTranslator.category.chat".equals(key)) return "Чат";
        if ("MineTranslator.category.items".equals(key)) return "Предметы";
        if ("MineTranslator.category.keys".equals(key)) return "Клавиши";
        if ("MineTranslator.category.debug".equals(key)) return "Отладка";
        if ("MineTranslator.config.section_desc".equals(key)) return "Настройки MineTranslator для Minecraft 1.16.5";
        if ("MineTranslator.config.language_title".equals(key)) return "Целевой язык";
        if ("MineTranslator.config.language_desc".equals(key)) return "Язык перевода чата, предметов и ввода";
        if ("MineTranslator.config.target_language".equals(key)) return "Язык: %s";
        if ("MineTranslator.config.players_title".equals(key)) return "Сообщения игроков";
        if ("MineTranslator.config.players_desc".equals(key)) return "Автоперевод сообщений с ником в [скобках]";
        if ("MineTranslator.config.enabled".equals(key)) return "Включено";
        if ("MineTranslator.config.disabled".equals(key)) return "Выключено";
        if ("MineTranslator.config.items_info".equals(key)) return "Наведи на предмет и нажми Ё: перевод / оригинал";
        if ("MineTranslator.config.keys_title".equals(key)) return "Клавиши MineTranslator";
        if ("MineTranslator.config.keys_desc".equals(key)) return "Переназначение обеих клавиш в стандартном меню";
        if ("MineTranslator.config.controls".equals(key)) return "Открыть управление";
        if ("MineTranslator.config.debug_info".equals(key)) return "Безопасные логи включены; содержимое tooltip не записывается";
        if ("MineTranslator.config.cancel".equals(key)) return "Отмена";
        if ("MineTranslator.config.save".equals(key)) return "Сохранить";
        return key;
    }

    @Override
    public boolean method_25404(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.field_22787.method_1507(parent);
            return true;
        }
        return super.method_25404(keyCode, scanCode, modifiers);
    }
}
