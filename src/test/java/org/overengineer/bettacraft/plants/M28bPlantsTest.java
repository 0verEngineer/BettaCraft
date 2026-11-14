package org.overengineer.bettacraft.plants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.plants.PlantShapes.Voxel;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;

/**
 * M28 part b: carpets, mosses, rosettes and tubers appended after part a; the Marimo is a ball; Riccia floats too; the
 * new floating plants are appended; the hammer and the mushroom coral of generator version 3 (old plants unchanged).
 */
class M28bPlantsTest {
    static final List<PlantSpecies> NEW = List.of(PlantSpecies.HC_CUBA, PlantSpecies.ELEOCHARIS, PlantSpecies.STAUROGYNE, PlantSpecies.MARSILEA,
            PlantSpecies.CHRISTMAS_MOSS, PlantSpecies.FLAME_MOSS, PlantSpecies.RICCIA, PlantSpecies.MARIMO, PlantSpecies.ECHINODORUS_RED_DIAMOND,
            PlantSpecies.CRYPT_PARVA, PlantSpecies.SAGITTARIA, PlantSpecies.APONOGETON, PlantSpecies.NYMPHAEA_GREEN);

    @Test
    void appendedAfterPartA() {
        assertEquals(PlantSpecies.BACOPA_CAROLINIANA.ordinal() + 1, PlantSpecies.HC_CUBA.ordinal());
        for (int i = 0; i < NEW.size(); i++) assertEquals(PlantSpecies.HC_CUBA.ordinal() + i, NEW.get(i).ordinal());
        assertEquals(PlantSpecies.values().length, PlantSpecies.NYMPHAEA_GREEN.ordinal() + 1);
        for (PlantSpecies s : NEW) assertTrue(!s.marine() && !s.form().coral() && s.meshed(), s.toString());
        // the floating plants: after the lily pad (the ordinal goes over the network)
        assertEquals(FloatingPlantType.LILY_PAD.ordinal() + 1, FloatingPlantType.WATER_LETTUCE.ordinal());
        assertTrue(FloatingPlantType.RICCIA.plantItem() && FloatingPlantType.RICCIA.grows());
        assertEquals(PlantSpecies.RICCIA.id(), FloatingPlantType.RICCIA.id(), "floating Riccia is the plant item");
        for (FloatingPlantType f : FloatingPlantType.values()) if (f != FloatingPlantType.RICCIA) assertFalse(f.plantItem(), f.toString());
        assertTrue(FloatingPlantType.HORNWORT.rootLength() > FloatingPlantType.FROGBIT.rootLength(), "hornwort hangs deep into the water");
    }

    @Test
    void needsAndForms() {
        assertTrue(PlantSpecies.HC_CUBA.needsCo2());
        assertEquals(PlantSpecies.Light.VERY_HIGH, PlantSpecies.HC_CUBA.light());
        assertTrue(PlantSpecies.RICCIA.onlyOnHardscape() && PlantSpecies.RICCIA.form() == PlantSpecies.Form.MOSS);
        assertTrue(PlantSpecies.MARIMO.rolls());
        for (PlantSpecies s : PlantSpecies.values()) {
            if (s != PlantSpecies.MARIMO) assertFalse(s.rolls(), s.toString());
            if (s != PlantSpecies.RICCIA) assertFalse(s.onlyOnHardscape(), s.toString());
        }
        assertTrue(PlantSpecies.CRYPT_PARVA.spreads() && PlantSpecies.SAGITTARIA.spreads() && PlantSpecies.ELEOCHARIS.spreads());
        assertFalse(PlantSpecies.ECHINODORUS_RED_DIAMOND.spreads() || PlantSpecies.MARIMO.spreads());
        assertTrue(PlantSpecies.MARIMO.daysToFull() >= 50, "the Marimo grows very slowly");
    }

    @Test
    void marimoIsARoundBallOnItsRoot() {
        for (long seed = 1; seed <= 6; seed++) {
            PlantShapes.Shape sh = PlantShapes.of(PlantSpecies.MARIMO, seed, 16, 30);
            List<Voxel> all = sh.all();
            int minX = 99, maxX = -99, minY = 99, maxY = -99, minZ = 99, maxZ = -99;
            for (Voxel v : all) {
                minX = Math.min(minX, v.x()); maxX = Math.max(maxX, v.x());
                minY = Math.min(minY, v.y()); maxY = Math.max(maxY, v.y());
                minZ = Math.min(minZ, v.z()); maxZ = Math.max(maxZ, v.z());
                assertEquals(PlantPart.MOSS, v.part() == PlantPart.ROOT ? PlantPart.MOSS : v.part());
            }
            assertEquals(0, minY, "it lies on its root");
            int w = maxX - minX + 1, d = maxZ - minZ + 1, h = maxY - minY + 1;
            assertTrue(Math.abs(w - h) <= 1 && Math.abs(d - h) <= 1, "round: " + w + "x" + h + "x" + d);
            assertTrue(h >= 6 && h <= 10, "size " + h);
            // nearly a full ball (filled, not a hollow shell)
            double vol = 4.0 / 3 * Math.PI * Math.pow(h / 2.0, 3);
            assertTrue(all.size() > vol * 0.55 && all.size() < vol * 1.5, all.size() + " blocks for a ball of " + h);
        }
    }

    @Test
    void carpetsStayLowExceptTheHairgrass() {
        for (long seed = 1; seed <= 6; seed++) {
            assertTrue(maxY(PlantSpecies.HC_CUBA, seed) <= 1, "HC a cushion");
            assertTrue(maxY(PlantSpecies.MARSILEA, seed) <= 1, "Marsilea low");
            assertTrue(maxY(PlantSpecies.STAUROGYNE, seed) <= 4, "Staurogyne low");
        }
        int grass = 0;
        for (long seed = 1; seed <= 6; seed++) grass = Math.max(grass, maxY(PlantSpecies.ELEOCHARIS, seed));
        assertTrue(grass >= 5, "hairgrass needles stand up: " + grass);
        // the flame moss stands higher than the flat Christmas moss
        int flame = 0, christmas = 0;
        for (long seed = 1; seed <= 6; seed++) {
            flame = Math.max(flame, maxY(PlantSpecies.FLAME_MOSS, seed));
            christmas = Math.max(christmas, maxY(PlantSpecies.CHRISTMAS_MOSS, seed));
        }
        assertTrue(flame > christmas, "flame " + flame + " > christmas " + christmas);
    }

    @Test
    void aponogetonRaisesItsSpikeToTheSurface() {
        boolean spike = false;
        for (long seed = 1; seed <= 6; seed++) {
            for (Voxel v : PlantShapes.of(PlantSpecies.APONOGETON, seed, 16, 30).all()) spike |= v.part() == PlantPart.TOP && v.y() == 29;
        }
        assertTrue(spike, "flower spike at the surface");
        assertTrue(PlantShapes.of(PlantSpecies.NYMPHAEA_GREEN, 5, 16, 20).all().stream().anyMatch(v -> v.part() == PlantPart.PAD && v.y() == 19),
                "the green lotus has floating leaves");
    }

    private static int maxY(PlantSpecies s, long seed) {
        int m = 0;
        for (Voxel v : PlantShapes.of(s, seed, 16, 30).all()) m = Math.max(m, v.y());
        return m;
    }

    @Test
    void marimoRollsAlongTheCurrent() {
        var cur = List.of(new org.overengineer.bettacraft.tank.TankCurrents.Current(0, 0, 1, 0, 1.0));
        double[] at = InteriorPlants.currentAt(cur, 2, 0.2);
        assertTrue(at != null && at[0] == 1 && at[1] == 0 && at[2] > 0.5, "in front of the outlet: along it");
        assertTrue(InteriorPlants.currentAt(cur, 0, 4) == null, "beside the current: none");
        assertTrue(InteriorPlants.currentAt(List.of(), 1, 1) == null, "no outlet: none");
        double near = InteriorPlants.currentAt(cur, 1, 0)[2], far = InteriorPlants.currentAt(cur, 7, 0)[2];
        assertTrue(far < near, "weaker further away: " + near + " -> " + far);
    }

    // ------------------------------------------------------------------ the corals of generator version 3

    private static Set<Long> keys(List<Voxel> vs) {
        Set<Long> out = new HashSet<>();
        for (Voxel v : vs) out.add(((long) v.x() & 0xFFFFF) << 40 | ((long) v.y() & 0xFFFFF) << 20 | ((long) v.z() & 0xFFFFF));
        return out;
    }

    @Test
    void oldCoralsKeepTheirShape() {
        for (PlantSpecies s : List.of(PlantSpecies.HAMMER_CORAL, PlantSpecies.MUSHROOM_CORAL)) {
            for (int v = 1; v <= 2; v++) {
                // a plant of version 1 or 2 is built like before M28 (version 1 of the coral generator)
                assertEquals(PlantShapes.build(s, 9, 16, 30, 1).all(), PlantShapes.of(s, 9, 16, 30, v).all(), s + " v" + v);
            }
            assertNotEquals(keys(PlantShapes.of(s, 9, 16, 30, 2).all()), keys(PlantShapes.of(s, 9, 16, 30, 3).all()), s + " v3 is new");
            assertEquals(PlantShapes.of(s, 9, 16, 30).all(), PlantShapes.of(s, 9, 16, 30, 3).all(), s + ": new plants get v3");
        }
        // the others are the same in every version
        for (PlantSpecies s : List.of(PlantSpecies.ACROPORA, PlantSpecies.ZOANTHUS, PlantSpecies.JAVA_MOSS, PlantSpecies.MONTE_CARLO, PlantSpecies.TIGER_LOTUS)) {
            assertEquals(PlantShapes.build(s, 9, 16, 30, 1).all(), PlantShapes.of(s, 9, 16, 30, 3).all(), s.toString());
        }
    }

    @Test
    void hammerCoralIsALowClumpNow() {
        double oldRatio = 0, newRatio = 0;
        for (long seed = 1; seed <= 10; seed++) {
            oldRatio += heightPerWidth(PlantShapes.of(PlantSpecies.HAMMER_CORAL, seed, 16, 30, 1).all());
            newRatio += heightPerWidth(PlantShapes.of(PlantSpecies.HAMMER_CORAL, seed, 16, 30, 3).all());
        }
        assertTrue(newRatio < oldRatio * 0.75, "lower and wider: " + newRatio / 10 + " (was " + oldRatio / 10 + ")");
        assertTrue(newRatio / 10 < 0.8, "wider than high: " + newRatio / 10);
    }

    private static double heightPerWidth(List<Voxel> vs) {
        int minX = 99, maxX = -99, maxY = 0, minZ = 99, maxZ = -99;
        for (Voxel v : vs) {
            minX = Math.min(minX, v.x()); maxX = Math.max(maxX, v.x());
            maxY = Math.max(maxY, v.y());
            minZ = Math.min(minZ, v.z()); maxZ = Math.max(maxZ, v.z());
        }
        return (maxY + 1.0) / Math.max(maxX - minX + 1, maxZ - minZ + 1);
    }

    @Test
    void mushroomDiscsKeepApart() {
        for (long seed = 1; seed <= 12; seed++) {
            List<Voxel> all = PlantShapes.of(PlantSpecies.MUSHROOM_CORAL, seed, 16, 30, 3).all();
            // the middles of the discs (TOP blocks with TOP on three sides) and the disc blocks around them
            Set<Long> top = new HashSet<>();
            for (Voxel v : all) if (v.part() == PlantPart.TOP) top.add(key2(v.x(), v.y(), v.z()));
            int discs = 0;
            for (Voxel v : all) {
                if (v.part() != PlantPart.TOP) continue;
                int n = 0;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) if (top.contains(key2(v.x() + d[0], v.y(), v.z() + d[1]))) n++;
                if (n >= 3) discs++;
            }
            assertTrue(discs >= 1, "at least one disc");
            // every disc is a piece of its own: the disc blocks fall into as many separate groups as there are discs
            Set<Long> disc = new HashSet<>();
            for (Voxel v : all) if (v.part() == PlantPart.LEAF || v.part() == PlantPart.TOP) disc.add(key2(v.x(), v.y(), v.z()));
            assertEquals(discs, groups(disc), "seed " + seed + ": discs apart");
        }
    }

    /** Number of 26-connected groups of the given blocks. */
    private static int groups(Set<Long> blocks) {
        Set<Long> seen = new HashSet<>();
        int n = 0;
        for (long k : blocks) {
            if (!seen.add(k)) continue;
            n++;
            java.util.ArrayDeque<Long> q = new java.util.ArrayDeque<>(List.of(k));
            while (!q.isEmpty()) {
                long c = q.poll();
                int x = (int) (c >> 40 << 44 >> 44), y = (int) (c >> 20 << 44 >> 44), z = (int) (c << 44 >> 44);
                for (int dx = -1; dx <= 1; dx++)
                    for (int dy = -1; dy <= 1; dy++)
                        for (int dz = -1; dz <= 1; dz++) {
                            long o = key2(x + dx, y + dy, z + dz);
                            if (blocks.contains(o) && seen.add(o)) q.add(o);
                        }
            }
        }
        return n;
    }

    private static long key2(int x, int y, int z) {
        return ((long) x & 0xFFFFF) << 40 | ((long) y & 0xFFFFF) << 20 | ((long) z & 0xFFFFF);
    }
}
