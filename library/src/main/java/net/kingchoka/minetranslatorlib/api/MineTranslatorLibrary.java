package net.kingchoka.minetranslatorlib.api;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Public API of the standalone MineTranslator Library mod. */
public final class MineTranslatorLibrary {
    private static final List<Consumer<Minecraft>> END_TICK = new CopyOnWriteArrayList<>();
    private static final List<ItemTooltipListener> ITEM_TOOLTIP = new CopyOnWriteArrayList<>();
    private static final List<ScreenKeyListener> SCREEN_KEY_PRESSED = new CopyOnWriteArrayList<>();
    private static final List<Consumer<Screen>> SCREEN_REMOVED = new CopyOnWriteArrayList<>();
    private static final List<KeyMapping> KEY_MAPPINGS = new CopyOnWriteArrayList<>();
    private static final List<ResourcePackRegistration> RESOURCE_PACKS = new CopyOnWriteArrayList<>();

    private MineTranslatorLibrary() {}

    public static void onEndClientTick(Consumer<Minecraft> listener) {
        END_TICK.add(Objects.requireNonNull(listener, "listener"));
    }

    public static void onItemTooltip(ItemTooltipListener listener) {
        ITEM_TOOLTIP.add(Objects.requireNonNull(listener, "listener"));
    }

    public static void onScreenKeyPressed(ScreenKeyListener listener) {
        SCREEN_KEY_PRESSED.add(Objects.requireNonNull(listener, "listener"));
    }

    public static void onScreenRemoved(Consumer<Screen> listener) {
        SCREEN_REMOVED.add(Objects.requireNonNull(listener, "listener"));
    }

    public static void registerKeyMappings(Collection<KeyMapping> mappings) {
        for (KeyMapping mapping : mappings) {
            if (!KEY_MAPPINGS.contains(mapping)) {
                KEY_MAPPINGS.add(mapping);
            }
        }
    }

    public static void registerResourcePack(String modId, String markerResource) {
        ResourcePackRegistration registration = new ResourcePackRegistration(modId, markerResource);
        if (!RESOURCE_PACKS.contains(registration)) {
            RESOURCE_PACKS.add(registration);
        }
    }

    public static List<ResourcePackRegistration> resourcePacks() {
        return List.copyOf(RESOURCE_PACKS);
    }

    public static void dispatchEndClientTick(Minecraft client) {
        END_TICK.forEach(listener -> listener.accept(client));
    }

    public static void dispatchItemTooltip(ItemStack stack, Item.TooltipContext context,
                                           TooltipFlag flag, List<Component> lines) {
        ITEM_TOOLTIP.forEach(listener -> listener.onTooltip(stack, context, flag, lines));
    }

    public static void dispatchScreenKeyPressed(Screen screen, KeyEvent event) {
        SCREEN_KEY_PRESSED.forEach(listener -> listener.onKeyPressed(screen, event));
    }

    public static void dispatchScreenRemoved(Screen screen) {
        SCREEN_REMOVED.forEach(listener -> listener.accept(screen));
    }

    public static KeyMapping[] appendKeyMappings(KeyMapping[] vanillaMappings) {
        KeyMapping[] result = Arrays.copyOf(vanillaMappings, vanillaMappings.length + KEY_MAPPINGS.size());
        for (int i = 0; i < KEY_MAPPINGS.size(); i++) {
            result[vanillaMappings.length + i] = KEY_MAPPINGS.get(i);
        }
        return result;
    }

    public static void loadSavedKeyMappings(File gameDirectory) {
        File optionsFile = new File(gameDirectory, "options.txt");
        if (optionsFile.isFile()) {
            try {
                List<String> lines = Files.readAllLines(optionsFile.toPath(), StandardCharsets.UTF_8);
                for (KeyMapping mapping : KEY_MAPPINGS) {
                    String prefix = "key_" + mapping.getName() + ":";
                    for (String line : lines) {
                        if (line.startsWith(prefix)) {
                            mapping.setKey(InputConstants.getKey(line.substring(prefix.length())));
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
                // Invalid options must never prevent the game from starting.
            }
        }
        KeyMapping.resetMapping();
    }

    @FunctionalInterface
    public interface ItemTooltipListener {
        void onTooltip(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines);
    }

    @FunctionalInterface
    public interface ScreenKeyListener {
        void onKeyPressed(Screen screen, KeyEvent event);
    }

    public record ResourcePackRegistration(String modId, String markerResource) {
        public ResourcePackRegistration {
            Objects.requireNonNull(modId, "modId");
            Objects.requireNonNull(markerResource, "markerResource");
            if (modId.isBlank() || markerResource.isBlank()) {
                throw new IllegalArgumentException("Resource pack registration values cannot be blank");
            }
        }
    }
}
