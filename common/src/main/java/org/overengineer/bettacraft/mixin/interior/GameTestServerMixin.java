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

/**
 * GameTest server only: vanilla bakes the test world with an empty dimension registry, so datapack dimensions (our
 * {@code bettacraft:tank_interior}) are missing. This passes the datapack dimensions instead, so interior GameTests
 * run against the real dimension. Never active in normal play (the class is only loaded by the GameTest server).
 */
@Mixin(GameTestServer.class)
public abstract class GameTestServerMixin {
    @WrapOperation(method = "lambda$create$1", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/WorldDimensions;bake(Lnet/minecraft/core/Registry;)Lnet/minecraft/world/level/levelgen/WorldDimensions$Complete;"))
    private static WorldDimensions.Complete bettacraft$withDatapackDimensions(WorldDimensions dims, Registry<LevelStem> empty,
                                                                             Operation<WorldDimensions.Complete> original,
                                                                             @Local(argsOnly = true) WorldLoader.DataLoadContext context) {
        return original.call(dims, context.datapackDimensions().registryOrThrow(net.minecraft.core.registries.Registries.LEVEL_STEM));
    }
}
