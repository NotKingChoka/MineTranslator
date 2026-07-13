package net.kingchoka.minetranslator.config.gui.test;

import net.kingchoka.minetranslator.config.gui.ui.MineTranslatorConfigScreen;
import net.kingchoka.minetranslator.event.ClientTickCallbacks;
import net.minecraft.client.Screenshot;

public final class UiVisualTest {
    private static int startupTicks;
    private static int settleTicks;
    private static int category;
    private static int postCaptureTicks;
    private static boolean captured;
    private static int capturesForCategory;

    private UiVisualTest() {}

    public static void init() {
        if (!Boolean.getBoolean("minetranslator.uiTest")) return;

        ClientTickCallbacks.POST.register(client -> {
            if (!(client.screen instanceof MineTranslatorConfigScreen screen)) {
                if (++startupTicks >= 160) {
                    client.setScreen(new MineTranslatorConfigScreen(client.screen));
                    startupTicks = Integer.MIN_VALUE;
                }
                return;
            }

            if (category >= 8) return;
            if (captured) {
                if (++postCaptureTicks < 20) return;
                postCaptureTicks = 0;
                captured = false;
                capturesForCategory++;
                if (capturesForCategory >= 2) {
                    capturesForCategory = 0;
                    category++;
                    if (category < 8) {
                        screen.selectCategoryForVisualTest(category);
                    }
                }
                return;
            }

            if (++settleTicks < 30) return;
            settleTicks = 0;
            Screenshot.grab(client.gameDirectory, client.getMainRenderTarget(), message -> {});
            captured = true;
        });
    }
}
