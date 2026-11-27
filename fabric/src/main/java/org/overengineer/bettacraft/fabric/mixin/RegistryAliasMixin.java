package org.overengineer.bettacraft.fabric.mixin;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.overengineer.bettacraft.fabric.FabricRegistryAliases;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * M68: NeoForge's registry aliases on Fabric ({@link FabricRegistryAliases}): the id lookups of a registry resolve an old id
 * of ours to its successor before they look. Saved block states (chunk palettes, NBT), item stacks and block entities go
 * through these methods (the holder lookups of a registry call {@code getHolder(ResourceKey)}).
 */
@Mixin(MappedRegistry.class)
public abstract class RegistryAliasMixin<T> {
    @Shadow
    public abstract ResourceKey<? extends Registry<T>> key();

    @ModifyVariable(method = {"get(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/Object;",
            "getHolder(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;",
            "containsKey(Lnet/minecraft/resources/ResourceLocation;)Z"}, at = @At("HEAD"), argsOnly = true)
    private ResourceLocation bettacraft$aliasId(ResourceLocation id) {
        return FabricRegistryAliases.resolve(key().location(), id);
    }

    @ModifyVariable(method = {"get(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;",
            "getHolder(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;",
            "containsKey(Lnet/minecraft/resources/ResourceKey;)Z"}, at = @At("HEAD"), argsOnly = true)
    private ResourceKey<T> bettacraft$aliasKey(ResourceKey<T> key) {
        return FabricRegistryAliases.resolve(key);
    }
}
