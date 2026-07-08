package net.kingchoka.minetranslator.platform;

public interface IPlatform {
    boolean isNeoForge();
    boolean isFabric();
    boolean isModLoaded(String modId);
}
