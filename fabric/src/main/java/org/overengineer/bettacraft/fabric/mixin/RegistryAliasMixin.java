package org.overengineer.bettacraft.fabric.mixin;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import org.overengineer.bettacraft.fabric.FabricRegistryAliases;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * M68: NeoForge's registry aliases on Fabric ({@link FabricRegistryAliases}): the id lookups of a registry resolve an old id
 * of ours to its successor before they look. Saved block states (chunk palettes, NBT), item stacks and block entities go
 * through these methods. 1.21.11: the lookups are {@code getValue(Identifier|ResourceKey)} (the value),
 * {@code get(Identifier|ResourceKey)} (the holder reference) and {@code containsKey}.
 */
@Mixin(MappedRegistry.class)
public abstract class RegistryAliasMixin<T> {
    @Shadow
    public abstract ResourceKey<? extends Registry<T>> key();

    @ModifyVariable(method = {"getValue(Lnet/minecraft/resources/Identifier;)Ljava/lang/Object;",
            "get(Lnet/minecraft/resources/Identifier;)Ljava/util/Optional;",
            "containsKey(Lnet/minecraft/resources/Identifier;)Z"}, at = @At("HEAD"), argsOnly = true)
    private Identifier bettacraft$aliasId(Identifier id) {
        return FabricRegistryAliases.resolve(key().identifier(), id);
    }

    @ModifyVariable(method = {"getValue(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;",
            "get(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;",
            "containsKey(Lnet/minecraft/resources/ResourceKey;)Z"}, at = @At("HEAD"), argsOnly = true)
    private ResourceKey<T> bettacraft$aliasKey(ResourceKey<T> key) {
        return FabricRegistryAliases.resolve(key);
    }
}
