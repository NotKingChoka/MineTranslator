package net.kingchoka.minetranslatorlib.mixin;

import net.kingchoka.minetranslatorlib.resource.MineTranslatorResourceLibrary;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {
    @Shadow @Final @Mutable private Set<RepositorySource> sources;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void minetranslatorlib$addRegisteredResources(RepositorySource[] initialSources, CallbackInfo ci) {
        Set<RepositorySource> extended = new LinkedHashSet<>(sources);
        extended.add(MineTranslatorResourceLibrary.createSource());
        sources = Set.copyOf(extended);
    }
}
