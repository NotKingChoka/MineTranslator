package net.kingchoka.minetranslatorlib.mixin;

import net.kingchoka.minetranslatorlib.api.MineTranslatorLibrary;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(Options.class)
public abstract class OptionsKeyMappingsMixin {
    @Shadow @Final @Mutable public KeyMapping[] keyMappings;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void minetranslatorlib$registerKeyMappings(Minecraft client, File gameDirectory, CallbackInfo ci) {
        keyMappings = MineTranslatorLibrary.appendKeyMappings(keyMappings);
        MineTranslatorLibrary.loadSavedKeyMappings(gameDirectory);
    }
}
