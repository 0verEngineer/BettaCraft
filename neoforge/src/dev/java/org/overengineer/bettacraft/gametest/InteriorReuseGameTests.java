package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.lang.reflect.Field;
import net.minecraft.core.BlockPos;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;

/**
 * M42 part b: the region list of the interiors is saved data (written at autosave and shutdown), the interior chunks are written
 * whenever they unload. After a crash (or a GameTest world that is reused, and once a full disk) the list can be older than the
 * chunks: its counter hands out an id whose interior is still there. A new tank then took over that old interior (its blocks,
 * plants and chests: a dupe; in the GameTests the land of an old paludarium made a sea turtle paludarium hold less water, the
 * flaky {@code seaTurtlesOnlyInBigSeaWaterPaludaria}). Such an id is skipped now.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InteriorReuseGameTests {
    private InteriorReuseGameTests() {}

    private static Object field(Object o, String name) throws ReflectiveOperationException {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    /** A new tank never gets the interior of an earlier region that the region list forgot (its counter set back). */
    @GameTest(template = "empty_32x10x32", batch = "bettacraft_m42_interior_reuse", timeoutTicks = 100)
    public static void aNewTankNeverTakesOverAForgottenInterior(GameTestHelper h) throws ReflectiveOperationException {
        ServerLevel level = h.getLevel();
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegions regions = InteriorRegions.get(level.getServer());
        // M53c: no number of a removed region is free during this test (each region gets a new number from the counter)
        var free = (it.unimi.dsi.fastutil.ints.IntSortedSet) field(regions, "free");
        int[] wasFree = free.toIntArray();
        free.clear();
        try {
            run(h, level, in, regions);
        } finally {
            for (int f : wasFree) if (!regions.living(f)) free.add(f);
        }
    }

    private static void run(GameTestHelper h, ServerLevel level, ServerLevel in, InteriorRegions regions) throws ReflectiveOperationException {
        TankBlockEntity old = CritterGameTests.tank(h, 2, 2, 2, 2, 2, 2, 8000);
        InteriorRegion r = InteriorManager.ensureRegion(level, old);
        int id = r.id();
        BlockPos chest = r.toWorld(5, 0, 5);
        in.setBlock(chest, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
        // as after a crash: the list was saved before this region existed (the counter at its id, the region not in it)
        @SuppressWarnings("unchecked")
        var map = (it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<InteriorRegion>) field(regions, "regions");
        map.remove(id);
        Field next = InteriorRegions.class.getDeclaredField("nextId");
        next.setAccessible(true);
        next.setInt(regions, id);
        TankBlockEntity fresh = CritterGameTests.tank(h, 8, 2, 2, 2, 2, 2, 8000);
        InteriorRegion n = InteriorManager.ensureRegion(level, fresh);
        h.assertTrue(n.id() != id, "the forgotten region " + id + " was handed out again");
        h.assertTrue(in.getBlockState(n.toWorld(5, 0, 5)).isAir(), "the new interior is empty");
        h.assertTrue(in.getBlockState(chest).is(Blocks.DIAMOND_BLOCK), "the old interior is left alone");
        // and a region that was never used is taken at once
        int before = (int) next.get(regions);
        TankBlockEntity third = CritterGameTests.tank(h, 14, 2, 2, 2, 2, 2, 8000);
        InteriorRegion t = InteriorManager.ensureRegion(level, third);
        if (t.id() != before) throw new GameTestFailure("a never used region was skipped: " + before + " -> " + t.id());
        map.put(id, r); // the first tank keeps its region for the clean-up of the test area (M53c: and it stays kept, never deleted)
        h.succeed();
    }
}
