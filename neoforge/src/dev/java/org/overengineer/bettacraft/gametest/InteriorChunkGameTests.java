package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** M31.1: how many interior chunks a tank keeps loaded (the showcase held ~750 per tank). */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InteriorChunkGameTests {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/gametest");
    private static final String EMPTY = "empty_32x10x32";

    private InteriorChunkGameTests() {}

    /** Loaded interior chunks around a region: {holders, holders with a chunk, full chunks}. */
    public static int[] loadedAround(ServerLevel interior, InteriorRegion r) {
        int x0 = (r.originX() >> 4) - 24, x1 = ((r.originX() + r.sizeX()) >> 4) + 24;
        int z0 = (r.originZ() >> 4) - 24, z1 = ((r.originZ() + r.sizeZ()) >> 4) + 24;
        int holders = 0, withChunk = 0, full = 0;
        for (ChunkHolder h : holders(interior.getChunkSource().chunkMap)) {
            var p = h.getPos();
            if (p.x < x0 || p.x > x1 || p.z < z0 || p.z > z1) continue;
            holders++;
            ChunkAccess c = h.getLatestChunk();
            if (c != null) withChunk++;
            if (c instanceof LevelChunk || c instanceof net.minecraft.world.level.chunk.ImposterProtoChunk) full++;
        }
        return new int[]{holders, withChunk, full};
    }

    /** All interior chunks: {holders, holders with a chunk, full chunks} (showcase log). */
    public static int[] loadedAll(ServerLevel interior) {
        int holders = 0, withChunk = 0, full = 0;
        for (ChunkHolder h : holders(interior.getChunkSource().chunkMap)) {
            holders++;
            ChunkAccess c = h.getLatestChunk();
            if (c != null) withChunk++;
            if (c instanceof LevelChunk || c instanceof net.minecraft.world.level.chunk.ImposterProtoChunk) full++;
        }
        return new int[]{holders, withChunk, full};
    }

    /** One log line: interior chunks and the used heap (after a GC). */
    public static String stats(ServerLevel interior) {
        int[] n = loadedAll(interior);
        System.gc();
        Runtime rt = Runtime.getRuntime();
        return fmt(n) + ", heap " + (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024) + " MB used of " + rt.maxMemory() / (1024 * 1024) + " MB";
    }

    @SuppressWarnings("unchecked")
    static Iterable<ChunkHolder> holders(ChunkMap map) {
        try {
            Method m = ChunkMap.class.getDeclaredMethod("getChunks");
            m.setAccessible(true);
            return (Iterable<ChunkHolder>) m.invoke(map);
        } catch (ReflectiveOperationException e) {
            throw new GameTestFailure("cannot list the chunk holders: " + e);
        }
    }

    private static String fmt(int[] n) {
        return n[0] + " holders, " + n[1] + " loaded, " + n[2] + " full";
    }

    /** Chunk columns of a region's shell: {columns in x, columns in z}. */
    static int[] shellColumns(InteriorRegion r) {
        return new int[]{((r.originX() + r.sizeX()) >> 4) - ((r.originX() - 1) >> 4) + 1, ((r.originZ() + r.sizeZ()) >> 4) - ((r.originZ() - 1) >> 4) + 1};
    }

    /**
     * Building interiors loads their shell columns and 3 rings of neighbours (vanilla's generation pyramid: 10 rings, ~750
     * chunks for a 4x3x3 tank); a few ticks later only the shell columns stay loaded. For four tanks of different sizes.
     */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void interiorsLoadOnlyTheirOwnChunks(GameTestHelper h) {
        ServerLevel in = InteriorGameTests.interior(h);
        int[][] tanks = {{2, 1, 2, 4, 3, 3}, {12, 1, 2, 1, 1, 1}, {16, 1, 2, 2, 1, 2}, {22, 1, 2, 3, 2, 2}};
        java.util.List<InteriorRegion> regions = new java.util.ArrayList<>();
        for (int[] t : tanks) {
            TankBlockEntity c = InteriorGameTests.tank(h, t[0], t[1], t[2], t[3], t[4], t[5], (long) t[3] * t[4] * t[5] * 1000L);
            InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
            regions.add(r);
            int[] cols = shellColumns(r), n = loadedAround(in, r);
            int bound = (cols[0] + 8) * (cols[1] + 8);
            LOGGER.info("[m31.1] region {} {}x{}x{} ({}x{} shell columns) after the build: {} (bound {})", r.id(), r.sizeX(), r.sizeY(), r.sizeZ(),
                    cols[0], cols[1], fmt(n), bound);
            h.assertTrue(n[1] <= bound, "region " + r.id() + ": building the interior loads at most " + bound + " chunks, not " + n[1]);
            h.assertTrue(n[2] >= cols[0] * cols[1], "region " + r.id() + ": the shell columns are loaded (" + n[2] + ")");
        }
        TankBlockEntity big = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 2)));
        LiveFoodGameTests.betta(h, big, false, 3L);
        InteriorPlants.plantAt(in, regions.get(0), PlantSpecies.ROTALA, 8, 0, 8, 2, 7L);
        for (int t : new int[]{5, 100, 400})
            h.runAtTickTime(t, () -> {
                for (InteriorRegion r : regions) LOGGER.info("[m31.1] region {} tick {}: {}", r.id(), t, fmt(loadedAround(in, r)));
            });
        h.runAtTickTime(450, () -> {
            for (InteriorRegion r : regions) {
                int[] cols = shellColumns(r), n = loadedAround(in, r);
                // at rest (care loop, a betta, a growing plant): the shell columns and the light neighbours around them
                int bound = (cols[0] + 2) * (cols[1] + 2);
                h.assertTrue(n[1] <= bound, "region " + r.id() + ": at rest at most " + bound + " chunks stay loaded, not " + n[1]);
            }
            h.succeed();
        });
    }
}
