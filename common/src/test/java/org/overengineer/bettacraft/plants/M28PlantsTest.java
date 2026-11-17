package org.overengineer.bettacraft.plants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.plants.PlantShapes.Voxel;

/**
 * M28 part a: the new species are appended (the shapes of the old ones stay), the white and pink Bucephalandras whiten
 * only in strong light, Downoi stays low, the soft water factor.
 */
class M28PlantsTest {
    private static final List<PlantSpecies> NEW = List.of(PlantSpecies.ANUBIAS_PETITE, PlantSpecies.ANUBIAS_PINTO, PlantSpecies.ANUBIAS_WHITE,
            PlantSpecies.ANUBIAS_COFFEEFOLIA, PlantSpecies.ANUBIAS_ROUND_LEAF, PlantSpecies.BUCE_KEDAGANG, PlantSpecies.BUCE_THEIA,
            PlantSpecies.BUCE_GODZILLA, PlantSpecies.BUCE_PINK_LADY, PlantSpecies.BUCE_MINI_PINK, PlantSpecies.BUCE_SNOW_WHITE,
            PlantSpecies.LUDWIGIA_SUPER_RED, PlantSpecies.ROTALA_HRA, PlantSpecies.ROTALA_WALLICHII, PlantSpecies.HYGROPHILA_PINNATIFIDA,
            PlantSpecies.POGOSTEMON_DOWNOI, PlantSpecies.LIMNOPHILA_SESSILIFLORA, PlantSpecies.MYRIOPHYLLUM_RED, PlantSpecies.BACOPA_CAROLINIANA);

    @Test
    void newSpeciesAreAppended() {
        // the ordinal seeds the shapes: the 25 species before M28 keep theirs
        assertEquals(24, PlantSpecies.BUBBLE_TIP_ANEMONE.ordinal());
        for (PlantSpecies s : PlantSpecies.values()) assertEquals(NEW.contains(s) || M28bPlantsTest.NEW.contains(s), s.newerThanConfigLists(), s.toString());
        for (PlantSpecies s : NEW) assertTrue(!s.marine() && s.meshed(), s.toString());
    }

    private static double brightness(int rgb) {
        return ((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF);
    }

    @Test
    void whiteBucephalandrasWhitenOnlyInStrongLight() {
        for (PlantSpecies s : List.of(PlantSpecies.BUCE_PINK_LADY, PlantSpecies.BUCE_MINI_PINK, PlantSpecies.BUCE_SNOW_WHITE)) {
            assertEquals(PlantSpecies.Red.WHITE, s.red());
            int[] leaf = new int[4];
            for (int t = 0; t <= 3; t++) leaf[t] = PlantColors.color(s, PlantPart.LEAF, t);
            // greenish in weak and ordinary light
            assertTrue(((leaf[0] >> 8) & 0xFF) > ((leaf[0] >> 16) & 0xFF), s + " green at tint 0");
            assertTrue(((leaf[1] >> 8) & 0xFF) > ((leaf[1] >> 16) & 0xFF), s + " green at tint 1");
            for (int t = 1; t <= 3; t++) assertTrue(brightness(leaf[t]) > brightness(leaf[t - 1]), s + " brighter with light");
            // white or whitish pink only at the top tint: every channel high
            assertTrue((leaf[3] & 0xFF) > 0xC0 && ((leaf[3] >> 8) & 0xFF) > 0xB0 && ((leaf[3] >> 16) & 0xFF) > 0xE0, s + " white " + Integer.toHexString(leaf[3]));
            assertTrue(brightness(leaf[3]) - brightness(leaf[2]) > brightness(leaf[1]) - brightness(leaf[0]), s + " the big step is the last");
        }
    }

    @Test
    void downoiStaysLowOthersReachTheSurface() {
        int downoi = 0, rotala = 0;
        for (long seed = 0; seed < 10; seed++) {
            for (Voxel v : PlantShapes.of(PlantSpecies.POGOSTEMON_DOWNOI, seed, 16, 31).all()) downoi = Math.max(downoi, v.y());
            for (Voxel v : PlantShapes.of(PlantSpecies.ROTALA_HRA, seed, 16, 31).all()) rotala = Math.max(rotala, v.y());
        }
        assertTrue(downoi <= 10, "Downoi low: " + downoi);
        assertTrue(rotala >= 28, "H'ra up to the surface: " + rotala);
    }

    @Test
    void softWaterFactor() {
        assertEquals(1.0, InteriorPlants.softWaterFactor(6.5, 150), 1e-9);
        assertEquals(1.0, InteriorPlants.softWaterFactor(7.0, 300), 1e-9);
        assertEquals(0.5, InteriorPlants.softWaterFactor(8.0, 200), 1e-9);
        assertEquals(0.25, InteriorPlants.softWaterFactor(8.4, 900), 1e-9);
        assertTrue(InteriorPlants.softWaterFactor(7.5, 250) < 1 && InteriorPlants.softWaterFactor(7.5, 250) > 0.5, "tap water: a bit slower");
    }

    @Test
    void needsAreWhereTheyBelong() {
        for (PlantSpecies s : PlantSpecies.values()) {
            // part b: Hemianthus "Cuba" also needs CO2
            if (s == PlantSpecies.HC_CUBA) assertTrue(s.needsCo2() && !s.softWater() && s.light() == PlantSpecies.Light.VERY_HIGH, s.toString());
            else if (s.needsCo2() || s.softWater()) assertTrue(NEW.contains(s) && s.form() == PlantSpecies.Form.STEM, s + ": only the new stem plants");
            if (s.alsoOnHardscape()) assertTrue(s.form().rooted(), s.toString());
        }
    }
}
