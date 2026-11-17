package org.overengineer.bettacraft.terrarium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/**
 * M31 part b: temperature-dependent sex determination and the incubation of a clutch (pure), the tank size the grown
 * turtles need, the species data.
 */
class EggSexTest {
    @Test
    void warmGivesFemalesCoolGivesMales() {
        for (HerpSpecies s : new HerpSpecies[]{HerpSpecies.SLIDER, HerpSpecies.HERMANN, HerpSpecies.REEVES}) {
            assertEquals(0.5, EggSex.femaleChance(s, s.pivot()), 1e-9, s.id() + " half at the pivot");
            assertTrue(EggSex.femaleChance(s, s.pivot() + 1.3) > 0.9, s.id() + " warm: females");
            assertTrue(EggSex.femaleChance(s, s.pivot() - 1.3) < 0.1, s.id() + " cool: males");
            assertTrue(EggSex.femaleChance(s, EggSex.femaleTemp(s)) > 0.99);
            assertTrue(EggSex.femaleChance(s, EggSex.maleTemp(s)) < 0.01);
        }
        // the real pivots: slider 29.2, Hermann's tortoise 31.5 (a slider nest at 30 gives mostly females, a tortoise nest mostly males)
        assertTrue(EggSex.femaleChance(HerpSpecies.SLIDER, 30.5) > 0.9);
        assertTrue(EggSex.femaleChance(HerpSpecies.HERMANN, 30.0) < 0.1);
    }

    @Test
    void muskTurtlesHaveFemalesAtBothEnds() {
        HerpSpecies m = HerpSpecies.MUSK;
        assertTrue(m.patternFmf());
        assertTrue(EggSex.femaleChance(m, 19.0) > 0.9, "cold nest: females");
        assertTrue(EggSex.femaleChance(m, 24.0) < 0.05, "middle: males");
        assertTrue(EggSex.femaleChance(m, 29.0) > 0.9, "warm nest: females");
        assertTrue(EggSex.femaleChance(m, EggSex.maleTemp(m)) < 0.01);
    }

    @Test
    void incubationFollowsTheTemperature() {
        HerpSpecies s = HerpSpecies.SLIDER;
        assertEquals(0, EggSex.rate(s, 17, 1), 1e-9, "below 18 °C it waits");
        assertEquals(1 / s.eggDays(), EggSex.rate(s, s.pivot(), 1), 1e-9, "eggDays at the pivot");
        assertTrue(EggSex.rate(s, 31, 1) > EggSex.rate(s, 27, 1), "warmer is faster");
        assertEquals(2 * EggSex.rate(s, 27, 1), EggSex.rate(s, 27, 2), 1e-9, "incubationSpeed");
    }

    @Test
    void theThermosensitivePeriodDecidesTheSex() {
        HerpClutch k = new HerpClutch(1, HerpSpecies.SLIDER, 5, Vec3.ZERO, BlockPos.ZERO, 7L);
        // warm first third, cool middle third, warm again: males (only the middle counts)
        int steps = 0;
        while (!k.ready() && steps++ < 100000) {
            double temp = k.progress < EggSex.TSP_FROM || k.progress >= EggSex.TSP_TO ? 32 : 26;
            k.incubate(temp, 0.01, 1);
        }
        assertTrue(k.ready(), "hatched");
        assertEquals(26, k.tspTemp(), 0.5, "the mean of the middle third");
        assertTrue(k.femaleShare() < 0.05, "a cool middle third: males");
        assertFalse(k.cooked());
    }

    @Test
    void heatKillsTheEmbryos() {
        HerpClutch k = new HerpClutch(1, HerpSpecies.HERMANN, 3, Vec3.ZERO, BlockPos.ZERO, 7L);
        for (int i = 0; i < 40; i++) k.incubate(38, 0.01, 1);
        assertTrue(k.cooked(), "above 36 °C for a quarter day");
    }

    @Test
    void clutchSavesAndLoads() {
        HerpClutch k = new HerpClutch(4, HerpSpecies.REEVES, 3, new Vec3(1.5, 2.25, 3.5), new BlockPos(10, 2, 30), 99L);
        k.incubate(28, 1.5, 1);
        k.motherVariety = 0;
        k.generation = 2;
        HerpClutch l = HerpClutch.load(k.save());
        assertEquals(k.progress, l.progress, 1e-12);
        assertEquals(k.tspTemp(), l.tspTemp(), 1e-9);
        assertEquals(99L, l.seed);
        assertEquals(new BlockPos(10, 2, 30), l.interiorPos());
        assertEquals(2, l.generation);
    }

    @Test
    void grownSlidersNeedABigTank() {
        TerrariumNeeds n = HerpSpecies.SLIDER.needs(true);
        var small = new TerrariumNeeds.Conditions(TerrariumMode.PALUDARIUM, 27, 70, 5, 39, 0.5, 25, 8);
        var big = new TerrariumNeeds.Conditions(TerrariumMode.PALUDARIUM, 27, 70, 5, 39, 0.5, 25, 18);
        assertTrue(n.assess(small).problems().contains(TerrariumNeeds.Problem.TOO_SMALL));
        assertFalse(n.assess(small).harmful(), "a small tank only stresses");
        assertTrue(n.assess(big).fine(), "fine: " + n.assess(big).problems());
        assertTrue(HerpSpecies.SLIDER.needs(false).assess(small).fine(), "a hatchling fits anywhere");
    }

    @Test
    void speciesData() {
        assertEquals("hermann_tortoise_box", HerpSpecies.HERMANN.containerId());
        assertEquals("slider_turtle_bucket", HerpSpecies.SLIDER.containerId());
        assertEquals("slider_turtle_elegans", HerpSpecies.SLIDER.texture(0));
        assertEquals("slider_turtle_scripta", HerpSpecies.SLIDER.texture(1));
        assertEquals("musk_turtle", HerpSpecies.MUSK.texture(0));
        assertTrue(HerpSpecies.HERMANN.brumates() && HerpSpecies.HERMANN.box() && !HerpSpecies.HERMANN.eats(org.overengineer.bettacraft.terrarium.TerrariumFood.Diet.ANIMAL) && HerpSpecies.HERMANN.eats(org.overengineer.bettacraft.terrarium.TerrariumFood.Diet.GREENS));
        assertEquals(TerrariumNeeds.Habitat.DRY, HerpSpecies.HERMANN.needs().habitat());
        for (HerpSpecies s : HerpSpecies.ALL) assertTrue(s.eggsMin() >= 1 && s.eggsMax() >= s.eggsMin() && s.eggsMax() <= 8, s.id());
    }

    /** Part c: the leopard gecko: females from cool nests, males around 32-33 °C, females again from hot ones (FMF). */
    @Test
    void leopardGeckoSexByTemperature() {
        HerpSpecies g = HerpSpecies.LEOPARD_GECKO;
        assertTrue(g.tsd() && g.patternFmf());
        assertTrue(EggSex.femaleChance(g, 27.0) > 0.95, "cool: females");
        assertTrue(EggSex.femaleChance(g, EggSex.maleTemp(g)) < 0.15, "32 °C: males");
        assertTrue(EggSex.femaleChance(g, 35.5) > 0.9, "hot: females again");
        assertTrue(EggSex.rate(g, 30.0, 1.0) > 0, "develops at 30 °C");
    }

    /** Part c: genetic sex: half and half at every temperature; the dart frog's young are tadpoles, the horned frog does not breed. */
    @Test
    void geneticSexAndPartCData() {
        for (HerpSpecies s : new HerpSpecies[]{HerpSpecies.DART_FROG, HerpSpecies.CRESTED_GECKO, HerpSpecies.CORN_SNAKE}) {
            assertFalse(s.tsd(), s.id());
            for (double t : new double[]{22, 26, 30, 34}) assertEquals(0.5, EggSex.femaleChance(s, t), 1e-9, s.id());
            assertTrue(EggSex.rate(s, s.pivot(), 1.0) > 0, s.id());
        }
        assertTrue(HerpSpecies.DART_FROG.tadpoles() && HerpSpecies.DART_FROG.pureVarieties());
        assertEquals(3, HerpSpecies.DART_FROG.wildVarieties(), "the three species live wild");
        assertFalse(HerpSpecies.HORNED_FROG.breeds(), "no horned frog breeding in a tank");
        assertEquals(1, HerpSpecies.LEOPARD_GECKO.wildVarieties(), "the morphs are bred, not wild");
        assertEquals("leopard_gecko_tangerine", HerpSpecies.LEOPARD_GECKO.texture(1));
        assertEquals("corn_snake_box", HerpSpecies.CORN_SNAKE.containerId());
        assertTrue(HerpSpecies.CORN_SNAKE.mealDays() >= 5 && HerpSpecies.CORN_SNAKE.eats(TerrariumFood.Diet.PREY));
        assertTrue(HerpSpecies.CRESTED_GECKO.gait().style() == LandWalker.Style.CLIMB);
        assertTrue(HerpSpecies.HORNED_FROG.gait().burrows() && HerpSpecies.HORNED_FROG.gait().style() == LandWalker.Style.HOP);
        for (HerpSpecies s : HerpSpecies.ALL) assertTrue(s.turtle() || s.box(), s.id() + ": a land animal travels in a box");
        for (LandWalker.Pose p : LandWalker.Pose.values()) assertEquals(p, LandWalker.Pose.of(p.ordinal()));
        assertEquals(LandWalker.Pose.NONE, LandWalker.Pose.of(99));
    }
}
