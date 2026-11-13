package org.overengineer.bettacraft.plants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** M13: growth forms, colours and the shears ray of the aquarium plants (pure rules). */
class PlantShapesTest {
    private static long key(int x, int y, int z) {
        return ((long) (x + 512) << 20) | ((long) (y + 512) << 10) | (z + 512);
    }

    private static Set<Long> keys(List<PlantShapes.Voxel> vs) {
        Set<Long> s = new HashSet<>();
        for (PlantShapes.Voxel v : vs) s.add(key(v.x(), v.y(), v.z()));
        return s;
    }

    @Test
    void everySpeciesGrowsBlockByBlockFromItsRoot() {
        for (PlantSpecies s : PlantSpecies.values()) {
            for (long seed = 1; seed <= 12; seed++) {
                PlantShapes.Shape sh = PlantShapes.of(s, seed, 16, 30);
                assertEquals(0, sh.at(0).size(), s + ": the cutting is only the root block");
                int prev = 1;
                Set<Long> before = new HashSet<>();
                for (int st = 1; st <= s.stages(); st++) {
                    List<PlantShapes.Voxel> at = sh.at(st);
                    Set<Long> now = keys(at);
                    assertTrue(now.containsAll(before), s + " stage " + st + ": grows by adding blocks");
                    assertTrue(at.size() >= prev, s + ": never shrinks");
                    prev = at.size();
                    before = now;
                    for (PlantShapes.Voxel v : at) {
                        assertTrue(v.y() < 30, s + ": stays under the surface");
                        assertTrue(v.y() >= (s.form().rooted() ? 0 : -3), s + ": not into the substrate");
                    }
                }
                assertTrue(now(sh, s.stages()).contains(key(0, 0, 0)), s + ": root");
                assertTrue(sh.at(s.stages()).size() > sh.at(1).size(), s + ": the plant grows");
                // every block of every stage hangs on the root (26-neighbourhood within the stage)
                for (int st = 1; st <= s.stages(); st++) assertTrue(connected(sh.at(st)), s + " seed " + seed + " stage " + st + " connected");
            }
        }
    }

    private static Set<Long> now(PlantShapes.Shape sh, int st) {
        return keys(sh.at(st));
    }

    private static boolean connected(List<PlantShapes.Voxel> vs) {
        Set<Long> all = keys(vs), seen = new HashSet<>();
        List<int[]> q = new ArrayList<>();
        q.add(new int[]{0, 0, 0});
        seen.add(key(0, 0, 0));
        while (!q.isEmpty()) {
            int[] c = q.removeLast();
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        long k = key(c[0] + dx, c[1] + dy, c[2] + dz);
                        if (all.contains(k) && seen.add(k)) q.add(new int[]{c[0] + dx, c[1] + dy, c[2] + dz});
                    }
        }
        return seen.size() == all.size();
    }

    @Test
    void sameSeedSameShapeOtherSeedOtherShape() {
        for (PlantSpecies s : PlantSpecies.values()) {
            assertEquals(PlantShapes.of(s, 42, 16, 30).all(), PlantShapes.of(s, 42, 16, 30).all(), s + " reproducible");
            if (s.form() != PlantSpecies.Form.CARPET) {
                assertNotEquals(keys(PlantShapes.of(s, 42, 16, 30).all()), keys(PlantShapes.of(s, 43, 16, 30).all()), s + " varies");
            }
        }
    }

    @Test
    void sizesFitTheScaleAndTheSpecies() {
        int fern = height(PlantSpecies.JAVA_FERN), buce = height(PlantSpecies.BUCE_GREEN_WAVY);
        assertTrue(fern >= 15 && fern <= 29, "Java fern 15-29 blocks (9-17 cm) at scale 16: " + fern);
        assertTrue(buce >= 4 && buce <= 12, "Bucephalandra small: " + buce);
        assertTrue(height(PlantSpecies.AMAZON_SWORD) > height(PlantSpecies.CRYPT_GREEN), "sword taller than the crypt");
        // stem plants and Vallisneria reach the surface (headroom 20: top layer 19)
        for (PlantSpecies s : List.of(PlantSpecies.ROTALA, PlantSpecies.LUDWIGIA, PlantSpecies.VALLISNERIA)) {
            assertEquals(19, maxY(PlantShapes.of(s, 7, 16, 20).at(s.stages())), s + " up to the surface");
        }
        assertEquals(0, maxY(PlantShapes.of(PlantSpecies.MONTE_CARLO, 7, 16, 20).all()), "a carpet stays flat");
        // the tiger lotus sends pads to the surface
        assertTrue(PlantShapes.of(PlantSpecies.TIGER_LOTUS, 5, 16, 20).all().stream().anyMatch(v -> v.part() == PlantPart.PAD && v.y() == 19));
        // scale 4 is a quarter of the size, shallow water shrinks plants
        assertTrue(maxY(PlantShapes.of(PlantSpecies.JAVA_FERN, 7, 4, 8).all()) < 8);
        assertTrue(PlantShapes.of(PlantSpecies.JAVA_FERN, 7, 4, 8).all().size() < PlantShapes.of(PlantSpecies.JAVA_FERN, 7, 16, 30).all().size() / 4);
        assertTrue(maxY(PlantShapes.of(PlantSpecies.AMAZON_SWORD, 7, 16, 8).all()) < 8, "shallow water");
    }

    private static int height(PlantSpecies s) {
        int sum = 0;
        for (long seed = 1; seed <= 8; seed++) sum += maxY(PlantShapes.of(s, seed, 16, 40).all());
        return sum / 8;
    }

    private static int maxY(List<PlantShapes.Voxel> vs) {
        int m = 0;
        for (PlantShapes.Voxel v : vs) m = Math.max(m, v.y());
        return m;
    }

    @Test
    void redSpeciesGetRedderWithLight() {
        for (PlantSpecies s : PlantSpecies.values()) {
            if (s.red() == PlantSpecies.Red.NONE) {
                assertEquals(PlantColors.color(s, PlantPart.LEAF, 0), PlantColors.color(s, PlantPart.LEAF, 3), s + " stays the same");
                continue;
            }
            PlantPart part = s.red() == PlantSpecies.Red.TIPS ? PlantPart.TOP : PlantPart.LEAF;
            double weak = PlantColors.redness(PlantColors.color(s, part, 0)), strong = PlantColors.redness(PlantColors.color(s, part, 3));
            assertTrue(strong > weak, s + " redder in strong light: " + weak + " -> " + strong);
        }
        assertTrue(PlantColors.redness(PlantColors.color(PlantSpecies.ROTALA, PlantPart.TOP, 3)) > 0.2, "Rotala tips red");
        assertTrue(PlantColors.redness(PlantColors.color(PlantSpecies.ROTALA, PlantPart.LEAF, 3)) < 0.1, "Rotala leaves stay green");
        int flamingoWeak = PlantColors.color(PlantSpecies.CRYPT_FLAMINGO, PlantPart.LEAF, 0), flamingoStrong = PlantColors.color(PlantSpecies.CRYPT_FLAMINGO, PlantPart.LEAF, 3);
        assertTrue(brightness(flamingoStrong) > brightness(flamingoWeak), "Flamingo brighter in strong light");
        assertTrue(brightness(flamingoWeak) > 150, "Flamingo whitish");
        assertEquals(0, PlantColors.tintFor(8));
        assertEquals(3, PlantColors.tintFor(15));
    }

    private static int brightness(int c) {
        return (((c >> 16) & 0xFF) + ((c >> 8) & 0xFF) + (c & 0xFF)) / 3;
    }

    @Test
    void rayWalksThroughTheBoxInOrder() {
        List<int[]> seen = new ArrayList<>();
        // from outside in front of the box (z < 0), straight along +z through x 2, y 3
        PlantRay.walk(2.5, 3.5, -5, 0, 0, 1, 8, 8, 8, 100, (x, y, z) -> {
            seen.add(new int[]{x, y, z});
            return false;
        });
        assertEquals(8, seen.size());
        for (int i = 0; i < 8; i++) assertEquals(i, seen.get(i)[2]);
        // stops where asked
        int[] stop = new int[1];
        assertTrue(PlantRay.walk(0.5, 0.5, 0.5, 1, 1, 1, 16, 16, 16, 100, (x, y, z) -> {
            stop[0] = x;
            return x == 5;
        }));
        assertEquals(5, stop[0]);
        // misses the box
        assertFalse(PlantRay.walk(-5, 20, -5, 1, 0, 1, 8, 8, 8, 100, (x, y, z) -> true));
        // reach limit
        assertFalse(PlantRay.walk(0.5, 0.5, -10, 0, 0, 1, 8, 8, 8, 5, (x, y, z) -> true));
    }

    @Test
    void growthFactors() {
        assertEquals(0, InteriorPlants.lightFactor(3, PlantSpecies.Light.LOW));
        assertEquals(1, InteriorPlants.lightFactor(8, PlantSpecies.Light.LOW));
        assertEquals(0, InteriorPlants.lightFactor(8, PlantSpecies.Light.HIGH), "stem plants need strong light");
        assertEquals(1, InteriorPlants.lightFactor(15, PlantSpecies.Light.HIGH));
        assertEquals(0.5, InteriorPlants.nutrientFactor(1.0F), 1e-9);
        assertEquals(1.0, InteriorPlants.nutrientFactor(0.7F), 1e-9);
        assertEquals(1.0, InteriorPlants.temperatureFactor(26));
        assertEquals(0.0, InteriorPlants.temperatureFactor(12));
        // growth times: Bucephalandra about 20 days, Java fern 12, stem plants 2-3
        assertEquals(20, PlantSpecies.BUCE_GREEN_WAVY.daysToFull(), 1e-9);
        assertEquals(12, PlantSpecies.JAVA_FERN.daysToFull(), 1e-9);
        assertTrue(PlantSpecies.ROTALA.daysToFull() <= 3);
    }
}
