package org.overengineer.bettacraft.mixin.interior;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.overengineer.bettacraft.registry.ModDimensions;
import org.overengineer.bettacraft.tank.interior.InteriorChunkMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** M31.1: lets the chunk generation tell the interior dimension apart ({@link ChunkGenerationTaskMixin}). */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin implements InteriorChunkMap {
    @Shadow
    @Final
    ServerLevel level;

    @Override
    public boolean bettacraft$isInterior() {
        return level.dimension() == ModDimensions.TANK_INTERIOR;
    }
}
