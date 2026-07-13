package net.kingchoka.minetranslatorlib.resource;

import net.fabricmc.loader.api.FabricLoader;
import net.kingchoka.minetranslatorlib.api.MineTranslatorLibrary;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.flag.FeatureFlags;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/** Turns resource roots registered through the public API into required vanilla packs. */
public final class MineTranslatorResourceLibrary {
    private MineTranslatorResourceLibrary() {}

    public static RepositorySource createSource() {
        return consumer -> {
            List<RegisteredRoot> registeredRoots = collectRegisteredRoots();
            for (RegisteredRoot registeredRoot : registeredRoots) {
                String id = registeredRoot.modId() + "_library_resources_" + registeredRoot.index();
                PackLocationInfo location = new PackLocationInfo(
                    id,
                    Component.literal(registeredRoot.modId() + " Resources"),
                    PackSource.BUILT_IN,
                    Optional.empty()
                );
                Pack.ResourcesSupplier resources = new Pack.ResourcesSupplier() {
                    @Override
                    public PackResources openPrimary(PackLocationInfo info) {
                        return new PathPackResources(info, registeredRoot.root());
                    }

                    @Override
                    public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                        return new PathPackResources(info, registeredRoot.root());
                    }
                };
                Pack.Metadata metadata = new Pack.Metadata(
                    Component.literal("Resources registered by MineTranslator Library"),
                    PackCompatibility.COMPATIBLE,
                    FeatureFlags.DEFAULT_FLAGS,
                    List.of()
                );
                PackSelectionConfig selection = new PackSelectionConfig(true, Pack.Position.TOP, true);
                consumer.accept(new Pack(location, resources, metadata, selection));
            }
        };
    }

    /** Registrations are collected lazily because mod entrypoints run after PackRepository is constructed. */
    private static List<RegisteredRoot> collectRegisteredRoots() {
        List<RegisteredRoot> registeredRoots = new ArrayList<>();
        for (MineTranslatorLibrary.ResourcePackRegistration registration : MineTranslatorLibrary.resourcePacks()) {
            LinkedHashSet<Path> roots = new LinkedHashSet<>();
            FabricLoader.getInstance().getModContainer(registration.modId())
                .ifPresent(container -> roots.addAll(container.getRootPaths()));
            FabricLoader.getInstance().getAllMods().stream()
                .flatMap(container -> container.getRootPaths().stream())
                .filter(root -> Files.isRegularFile(root.resolve(registration.markerResource())))
                .forEach(roots::add);
            int index = 0;
            for (Path root : roots) {
                registeredRoots.add(new RegisteredRoot(registration.modId(), index++, root));
            }
        }
        return registeredRoots;
    }

    private record RegisteredRoot(String modId, int index, Path root) {}
}
