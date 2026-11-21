package org.overengineer.bettacraft.plants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.plants.wild.LandLook;
import org.overengineer.bettacraft.sources.WildTables;

/**
 * M56: the terrarium plants: about 35-40 species in three habitats, their climate (humidity, air temperature), the growth
 * factors and the languishing, the emersed aquarium plants, how they are propagated, where they grow wild, and that every one
 * has its textures, icon and world look.
 */
class LandPlantsTest {
    private static Path res() {
        return Path.of(System.getProperty("bettacraft.projectDir", ".")).resolve("src/main/resources/assets/bettacraft");
    }

    @Test
    void thirtyFiveToFortySpeciesInThreeHabitats() {
        var land = PlantSpecies.landSpecies();
        assertTrue(land.size() >= 35 && land.size() <= 40, "species: " + land.size());
        Set<PlantSpecies.Habitat> habitats = EnumSet.noneOf(PlantSpecies.Habitat.class);
        for (PlantSpecies p : land) habitats.add(p.habitat());
        assertEquals(EnumSet.of(PlantSpecies.Habitat.TROPICAL, PlantSpecies.Habitat.SWAMP, PlantSpecies.Habitat.DRY), habitats);
        for (PlantSpecies p : PlantSpecies.values()) assertEquals(p.ordinal() > PlantSpecies.NYMPHAEA_GREEN.ordinal(), p.land(), p + ": appended");
        for (PlantSpecies p : land) {
            assertTrue(!p.marine() && !p.form().coral() && p.form() != PlantSpecies.Form.BULB && p.form() != PlantSpecies.Form.RIBBON, p + ": form");
            assertTrue(p.humidityMin() < p.humidityMax() && p.tempMin() < p.tempMax(), p + ": ranges");
        }
        // the three bromeliads, the cacti offset, the mosses divide
        assertEquals(3, land.stream().filter(PlantSpecies::bromeliad).count());
        for (PlantSpecies p : land) {
            if (p.form() == PlantSpecies.Form.CACTUS || p.bromeliad()) assertEquals(PlantSpecies.Propagation.OFFSET, p.propagation(), p.toString());
            if (p.form() == PlantSpecies.Form.MOSS) assertEquals(PlantSpecies.Propagation.DIVISION, p.propagation(), p.toString());
        }
        for (PlantSpecies p : PlantSpecies.values()) if (!p.land()) assertEquals(PlantSpecies.Propagation.CUTTING, p.propagation(), p.toString());
        assertTrue(PlantSpecies.PRICKLY_PEAR.edible() && PlantSpecies.SEDUM.edible() && !PlantSpecies.NEOREGELIA.edible());
    }

    @Test
    void climateFactors() {
        PlantSpecies fern = PlantSpecies.BOSTON_FERN, cactus = PlantSpecies.BARREL_CACTUS;
        assertEquals(1.0, LandPlants.humidityFactor(fern, 85), 1e-9);
        assertEquals(0.5, LandPlants.humidityFactor(fern, fern.humidityMin() - 10), 1e-9);
        assertEquals(0.0, LandPlants.humidityFactor(fern, 30), 1e-9);
        assertEquals(1.0, LandPlants.humidityFactor(cactus, 30), 1e-9);
        assertEquals(0.0, LandPlants.humidityFactor(cactus, 95), 1e-9, "a cactus rots in a rainforest");
        assertEquals(1.0, LandPlants.temperatureFactor(cactus, 35), 1e-9);
        assertEquals(0.5, LandPlants.temperatureFactor(fern, 32), 1e-9);
        assertEquals(0.0, LandPlants.temperatureFactor(fern, 40), 1e-9);
        LandPlants.Env rainforest = new LandPlants.Env(12, 90, 25, true), desert = new LandPlants.Env(15, 25, 32, true);
        assertTrue(LandPlants.rate(fern, rainforest, false) > 0.9);
        assertEquals(0, LandPlants.rate(fern, rainforest, true), 1e-9, "under water");
        assertEquals(0, LandPlants.rate(fern, desert, false), 1e-9);
        assertTrue(LandPlants.rate(cactus, desert, false) > 0.9);
        assertEquals(0, LandPlants.rate(cactus, rainforest, false), 1e-9);
        // the carnivorous plants want strong light
        assertTrue(LandPlants.rate(PlantSpecies.VENUS_FLYTRAP, new LandPlants.Env(6, 80, 22, true), false) == 0);
    }

    @Test
    void emersedAquariumPlants() {
        LandPlants.Env humid = new LandPlants.Env(12, 85, 25, true), dry = new LandPlants.Env(12, 40, 25, true), aquarium = new LandPlants.Env(12, 85, 25, false);
        assertEquals(LandPlants.EMERSED_SPEED, LandPlants.emersedRate(PlantSpecies.AMAZON_SWORD, humid, 1.0), 1e-9);
        assertEquals(0, LandPlants.emersedRate(PlantSpecies.AMAZON_SWORD, dry, 1.0), 1e-9);
        assertEquals(0, LandPlants.emersedRate(PlantSpecies.AMAZON_SWORD, aquarium, 1.0), 1e-9, "no climate: no emersed growth");
        assertEquals(0, LandPlants.emersedRate(PlantSpecies.ZOANTHUS, humid, 1.0), 1e-9);
        assertEquals(0, LandPlants.emersedRate(PlantSpecies.NEOREGELIA, humid, 1.0), 1e-9);
    }

    @Test
    void languishing() {
        InteriorPlant p = new InteriorPlant(1, PlantSpecies.BOSTON_FERN, 0, 0, 0, 1, 6);
        assertEquals(0, LandPlants.wilt(p, 0.0, LandPlants.WILT_DAYS * 0.9));
        assertEquals(1, LandPlants.wilt(p, 0.0, LandPlants.WILT_DAYS * 0.2), "a stage lost after about WILT_DAYS");
        assertEquals(5, p.target);
        assertTrue(LandPlants.languishing(p) || p.wilt >= 0);
        LandPlants.wilt(p, 1.0, LandPlants.WILT_DAYS * 2);
        assertEquals(0, p.wilt, 1e-9, "heals in a good climate");
        assertEquals(4, LandPlants.wilt(p, 0.0, LandPlants.WILT_DAYS * 10), "never below stage 1");
        assertEquals(1, p.target);
        // a slightly bad climate (above the wilt line) does no harm
        InteriorPlant q = new InteriorPlant(2, PlantSpecies.ALOE, 0, 0, 0, 1, 6);
        assertEquals(0, LandPlants.wilt(q, 0.3, 100));
    }

    @Test
    void everyOneGrowsWildAndHasItsLooks() {
        for (PlantSpecies p : PlantSpecies.landSpecies()) {
            boolean wild = false;
            for (WildTables.LandPlace w : WildTables.LandPlace.values()) if (WildTables.wildLandPlants(w).entries().stream().anyMatch(e -> e.value() == p)) wild = true;
            assertTrue(wild, p + ": grows wild somewhere");
            for (WildTables.LandPlace w : WildTables.LandPlace.values()) {
                for (var e : WildTables.wildLandPlants(w).entries()) assertTrue(e.value().land(), w + ": only terrarium plants");
            }
            String icon = LandLook.icon(p);
            assertTrue(Files.exists(res().resolve("textures/item/terrarium_" + icon + ".png")), p + ": icon " + icon);
            assertTrue(Files.exists(res().resolve("textures/item/terrarium_" + icon + "_detail.png")), p + ": icon detail " + icon);
            assertTrue(Files.exists(res().resolve("textures/block/plant_leaf_" + p.leafTexture() + ".png")), p + ": leaf " + p.leafTexture());
            LandLook look = LandLook.of(p);
            if (look.model == LandLook.Model.CROSS) {
                for (String suffix : new String[]{"", "_accent", "_top"}) {
                    assertTrue(Files.exists(res().resolve("textures/block/wild_land_" + look.id() + suffix + ".png")), p + ": world look " + look.id() + suffix);
                }
            }
            if (look.model == LandLook.Model.CACTUS) {
                assertTrue(Files.exists(res().resolve("models/block/template_wild_cactus_" + look.id() + ".json")), p + ": cactus template");
                assertTrue(Files.exists(res().resolve("models/block/template_wild_cactus_" + look.id() + "_short.json")), p + ": trimmed cactus template");
            }
        }
    }

    @Test
    void shapesStayLowOrClimb() {
        // the low plants stay low, the climbers and papyrus reach up (in a tank three blocks high)
        for (long seed = 1; seed < 6; seed++) {
            int lithops = top(PlantShapes.of(PlantSpecies.LITHOPS, seed, 16, 48)), papyrus = top(PlantShapes.of(PlantSpecies.PAPYRUS, seed, 16, 48));
            int column = top(PlantShapes.of(PlantSpecies.COLUMN_CACTUS, seed, 16, 48)), moss = top(PlantShapes.of(PlantSpecies.CUSHION_MOSS, seed, 16, 48));
            assertTrue(lithops <= 4, "Lithops low: " + lithops);
            assertTrue(moss <= 6, "cushion moss low: " + moss);
            assertTrue(papyrus >= 18, "papyrus tall: " + papyrus);
            assertTrue(column >= 15, "column cactus tall: " + column);
        }
    }

    private static int top(PlantShapes.Shape s) {
        int top = 0;
        for (PlantShapes.Voxel v : s.all()) top = Math.max(top, v.y());
        return top;
    }
}
