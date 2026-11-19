package org.overengineer.bettacraft.tank.interior;

import java.util.concurrent.CompletableFuture;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStatusTask;

/**
 * M31.1: the generation pyramid of the interior dimension. Vanilla generates every chunk with the structure starts of all
 * chunks within 8 (plus the steps around it: 10 rings in all), so the 30 chunk columns of a 4x3x3 tank's shell made the
 * world generate and keep ~750 chunks of 1040 blocks height, and wrote them all to disk (~3 MB per tank). The interior is
 * a structureless void ({@code minecraft:flat}, no layers, no features, no structures), so these neighbours are never
 * read: here the structure references step does nothing and the steps that asked for structure starts 8 chunks around
 * ask for them one chunk around (the radius of their other needs). Every other step and task is vanilla's, so the
 * chunks come out the same, 3 rings around the shell (~130 chunks for that tank). Used for the interior dimension only
 * ({@code ChunkGenerationTaskMixin}); at every distance it asks for at most the status vanilla allows there.
 */
public final class InteriorChunkPyramid {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("bettacraft/interior");
    public static final ChunkPyramid PYRAMID = build();
    /**
     * Whether the interior uses it: not if it would ask for more than vanilla somewhere (a changed vanilla pyramid) or
     * with {@code -Dbettacraft.vanillaInteriorGeneration=true}.
     */
    public static final boolean ACTIVE = active();

    private InteriorChunkPyramid() {}

    private static ChunkStatusTask vanilla(ChunkStatus status) {
        return ChunkPyramid.GENERATION_PYRAMID.getStepTo(status).task();
    }

    private static ChunkPyramid build() {
        ChunkStatusTask nothing = (context, step, cache, chunk) -> CompletableFuture.completedFuture(chunk);
        return new ChunkPyramid.Builder()
                .step(ChunkStatus.EMPTY, b -> b)
                .step(ChunkStatus.STRUCTURE_STARTS, b -> b.setTask(vanilla(ChunkStatus.STRUCTURE_STARTS)))
                // vanilla collects the references to the structure starts 8 chunks around: there are none
                .step(ChunkStatus.STRUCTURE_REFERENCES, b -> b.setTask(nothing))
                // M52e: 26.3 merged noise, surface and carvers into TERRAIN (the carvers run in BIOMES, which now asks for the
                // structure starts around like the other generation steps)
                .step(ChunkStatus.BIOMES, b -> b.addRequirement(ChunkStatus.STRUCTURE_STARTS, 1).setTask(vanilla(ChunkStatus.BIOMES)))
                .step(ChunkStatus.TERRAIN, b -> b.addRequirement(ChunkStatus.STRUCTURE_STARTS, 1).addRequirement(ChunkStatus.BIOMES, 1)
                        .blockStateWriteRadius(0).setTask(vanilla(ChunkStatus.TERRAIN)))
                .step(ChunkStatus.FEATURES, b -> b.addRequirement(ChunkStatus.STRUCTURE_STARTS, 1).addRequirement(ChunkStatus.TERRAIN, 1)
                        .blockStateWriteRadius(1).setTask(vanilla(ChunkStatus.FEATURES)))
                .step(ChunkStatus.INITIALIZE_LIGHT, b -> b.setTask(vanilla(ChunkStatus.INITIALIZE_LIGHT)))
                .step(ChunkStatus.LIGHT, b -> b.addRequirement(ChunkStatus.INITIALIZE_LIGHT, 1).setTask(vanilla(ChunkStatus.LIGHT)))
                .step(ChunkStatus.SPAWN, b -> b.addRequirement(ChunkStatus.BIOMES, 1).setTask(vanilla(ChunkStatus.SPAWN)))
                .step(ChunkStatus.FULL, b -> b.setTask(vanilla(ChunkStatus.FULL)))
                .build();
    }

    private static boolean active() {
        if (Boolean.getBoolean("bettacraft.vanillaInteriorGeneration")) return false;
        if (withinVanilla()) return true;
        LOGGER.warn("The interior chunk pyramid would need more than vanilla's: the interior is generated with vanilla's");
        return false;
    }

    /** Whether the pyramid never asks for more than vanilla at any distance. */
    public static boolean withinVanilla() {
        var ours = PYRAMID.getStepTo(ChunkStatus.FULL).accumulatedDependencies();
        var theirs = ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FULL).accumulatedDependencies();
        if (ours.getRadius() > theirs.getRadius()) return false;
        for (int d = 0; d <= ours.getRadius(); d++) if (ours.get(d).isAfter(theirs.get(d))) return false;
        return true;
    }
}
