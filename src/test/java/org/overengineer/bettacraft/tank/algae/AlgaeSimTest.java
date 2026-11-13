package org.overengineer.bettacraft.tank.algae;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.critters.CritterSpecies;

/** M14: algae on the glass: faces, growth with light and waste, a few nerites keep it clean, stages and the sync format. */
class AlgaeSimTest {
    private static LongOpenHashSet box(int sx, int sy, int sz) {
        LongOpenHashSet m = new LongOpenHashSet();
        for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) m.add(BlockPos.asLong(x, y, z));
        return m;
    }

    @Test
    void facesAreTheSideGlassUnderWater() {
        LongOpenHashSet m = box(4, 2, 2);
        assertEquals(24, AlgaeSim.faces(m, 1).size(), "4x2x2: 2 layers x 12 side faces");
        assertEquals(12, AlgaeSim.faces(m, 0).size(), "only the wet layer");
        m.remove(BlockPos.asLong(0, 1, 0)); // a notch: its neighbours get faces towards it
        assertEquals(24 - 2 + 2, AlgaeSim.faces(m, 1).size());
    }

    @Test
    void growthNeedsLightAndWaste() {
        assertEquals(0, AlgaeSim.growthPerDay(0.25, 5, 0.5F, true, 0, 0));
        double clean = AlgaeSim.growthPerDay(0.25, 15, 1F, true, 0, 0), dirty = AlgaeSim.growthPerDay(0.25, 15, 0.5F, true, 0, 0);
        assertTrue(dirty > 2 * clean, "waste feeds the algae");
        assertTrue(AlgaeSim.growthPerDay(0.25, 15, 0.5F, true, 0.08, 0) < dirty, "plants take the nutrients");
        assertTrue(AlgaeSim.growthPerDay(0.25, 15, 0.5F, true, 0, 1) < dirty, "floating plants shade the glass");
        assertEquals(0, AlgaeSim.growthPerDay(0, 15, 0.5F, true, 0, 0), "config 0: no algae");
    }

    @Test
    void aFewNeritesKeepTheGlassClean() {
        var faces = AlgaeSim.faces(box(4, 2, 2), 1);
        double g = AlgaeSim.growthPerDay(0.25, 15, 0.5F, true, 0, 0);
        Long2ObjectOpenHashMap<float[]> none = new Long2ObjectOpenHashMap<>(), three = new Long2ObjectOpenHashMap<>();
        double nerite = CritterSpecies.ZEBRA_NERITE.algaePerDay();
        double eaten = 0;
        for (int i = 0; i < 10 * 24; i++) {
            AlgaeSim.step(none, faces, g, 0, 1.0 / 24);
            eaten += AlgaeSim.step(three, faces, g, 3 * nerite, 1.0 / 24);
        }
        assertTrue(AlgaeSim.mean(none, faces) > 0.9, "without cleaners the glass turns green");
        assertTrue(AlgaeSim.mean(three, faces) < 0.05, "three nerites keep it clean: " + AlgaeSim.mean(three, faces));
        assertTrue(eaten > 1, "and they ate the algae");
        assertEquals(4, AlgaeSim.stage(AlgaeSim.get(none, faces.getFirst().pos(), faces.getFirst().dir())));
        assertEquals(0, AlgaeSim.stage(0.05F));
        // a few shrimp alone are not enough
        Long2ObjectOpenHashMap<float[]> shrimp = new Long2ObjectOpenHashMap<>();
        for (int i = 0; i < 10 * 24; i++) AlgaeSim.step(shrimp, faces, g, 10 * CritterSpecies.NEOCARIDINA.algaePerDay(), 1.0 / 24);
        assertTrue(AlgaeSim.mean(shrimp, faces) > 0.3);
    }

    @Test
    void encodeDecode() {
        Long2ObjectOpenHashMap<float[]> a = new Long2ObjectOpenHashMap<>();
        AlgaeSim.set(a, BlockPos.asLong(10, 64, 5), 3, 0.9F);
        AlgaeSim.set(a, BlockPos.asLong(11, 64, 5), 0, 0.4F);
        AlgaeSim.set(a, BlockPos.asLong(12, 64, 5), 1, 0.05F); // invisible
        var d = AlgaeSim.decode(AlgaeSim.encode(a, 10, 64, 5), 10, 64, 5);
        assertEquals(2, d.size());
        assertEquals(1.0F, d.get(BlockPos.asLong(10, 64, 5))[3]);
        assertEquals(0.5F, d.get(BlockPos.asLong(11, 64, 5))[0]);
        AlgaeSim.set(a, BlockPos.asLong(10, 64, 5), 3, 0);
        assertTrue(!a.containsKey(BlockPos.asLong(10, 64, 5)), "a clean block is forgotten");
        assertEquals(List.of(), AlgaeSim.faces(new LongOpenHashSet(), 5));
    }
}
