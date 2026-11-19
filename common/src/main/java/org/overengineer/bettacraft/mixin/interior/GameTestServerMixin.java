package org.overengineer.bettacraft.mixin.interior;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.WorldLoader;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * GameTest server only: vanilla bakes the test world with an empty dimension registry, so datapack dimensions (our
 * {@code bettacraft:tank_interior}) are missing. This passes the datapack dimensions instead, so interior GameTests
 * run against the real dimension. Never active in normal play (the class is only loaded by the GameTest server).
 */
@Mixin(GameTestServer.class)
public abstract class GameTestServerMixin {
    // M52c (26.1): no intermediary any more, both loaders see the lambda by its Mojang name
    @WrapOperation(method = "lambda$create$1",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/WorldDimensions;bake(Lnet/minecraft/core/Registry;)Lnet/minecraft/world/level/levelgen/WorldDimensions$Complete;"))
    private static WorldDimensions.Complete bettacraft$withDatapackDimensions(WorldDimensions dims, Registry<LevelStem> empty,
                                                                             Operation<WorldDimensions.Complete> original,
                                                                             @Local(argsOnly = true) WorldLoader.DataLoadContext context) {
        return original.call(dims, context.datapackDimensions().lookupOrThrow(net.minecraft.core.registries.Registries.LEVEL_STEM));
    }

    /**
     * M52e: 26.3 tests in a sandstone desert ({@code flat_all_dimensions}); the GameTests of the mod were written for the
     * plains of the flat world (temperature, rain, the ground at the test structures), as vanilla had it up to 26.2.
     */
    @ModifyArg(method = "lambda$create$1", require = 1, at = @At(value = "INVOKE",
            target = "getOrThrow(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/core/Holder$Reference;"))
    private static net.minecraft.resources.ResourceKey<?> bettacraft$plainsWorld(net.minecraft.resources.ResourceKey<?> preset) {
        return preset == net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT_ALL_DIMENSIONS
                ? net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT : preset;
    }
}
