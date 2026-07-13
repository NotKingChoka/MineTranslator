package net.kingchoka.minetranslatorlib.api;

/** Implement this Fabric entrypoint to register a mod with MineTranslator Library. */
@FunctionalInterface
public interface MineTranslatorLibraryEntrypoint {
    void registerLibraryHooks();
}
