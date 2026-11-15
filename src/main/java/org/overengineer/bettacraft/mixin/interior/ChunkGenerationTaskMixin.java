package org.overengineer.bettacraft.mixin.interior;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ChunkGenerationTask;
import net.minecraft.server.level.GeneratingChunkMap;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import org.overengineer.bettacraft.tank.interior.InteriorChunkMap;
import org.overengineer.bettacraft.tank.interior.InteriorChunkPyramid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * M31.1: chunks of the interior dimension are generated with {@link InteriorChunkPyramid} (3 rings of neighbours
 * instead of vanilla's 10), both for how far around the layers are scheduled and for the steps that are applied. The
 * cache of the task keeps vanilla's radius (it only holds the chunk holders, which exist that far anyway).
 */
@Mixin(ChunkGenerationTask.class)
public abstract class ChunkGenerationTaskMixin {
    @Shadow
    @Final
    private GeneratingChunkMap chunkMap;

    @ModifyExpressionValue(method = {"getRadiusForLayer", "scheduleChunkInLayer"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/chunk/status/ChunkPyramid;GENERATION_PYRAMID:Lnet/minecraft/world/level/chunk/status/ChunkPyramid;"))
    private ChunkPyramid bettacraft$interiorPyramid(ChunkPyramid original) {
        return InteriorChunkPyramid.ACTIVE && chunkMap instanceof InteriorChunkMap m && m.bettacraft$isInterior() ? InteriorChunkPyramid.PYRAMID : original;
    }
}
