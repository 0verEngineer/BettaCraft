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
class TurtleSexTest {
    @Test
    void warmGivesFemalesCoolGivesMales() {
        for (TurtleSpecies s : new TurtleSpecies[]{TurtleSpecies.SLIDER, TurtleSpecies.HERMANN, TurtleSpecies.REEVES}) {
            assertEquals(0.5, TurtleSex.femaleChance(s, s.pivot()), 1e-9, s.id() + " half at the pivot");
            assertTrue(TurtleSex.femaleChance(s, s.pivot() + 1.3) > 0.9, s.id() + " warm: females");
            assertTrue(TurtleSex.femaleChance(s, s.pivot() - 1.3) < 0.1, s.id() + " cool: males");
            assertTrue(TurtleSex.femaleChance(s, TurtleSex.femaleTemp(s)) > 0.99);
            assertTrue(TurtleSex.femaleChance(s, TurtleSex.maleTemp(s)) < 0.01);
        }
        // the real pivots: slider 29.2, Hermann's tortoise 31.5 (a slider nest at 30 gives mostly females, a tortoise nest mostly males)
        assertTrue(TurtleSex.femaleChance(TurtleSpecies.SLIDER, 30.5) > 0.9);
        assertTrue(TurtleSex.femaleChance(TurtleSpecies.HERMANN, 30.0) < 0.1);
    }

    @Test
    void muskTurtlesHaveFemalesAtBothEnds() {
        TurtleSpecies m = TurtleSpecies.MUSK;
        assertTrue(m.patternFmf());
        assertTrue(TurtleSex.femaleChance(m, 19.0) > 0.9, "cold nest: females");
        assertTrue(TurtleSex.femaleChance(m, 24.0) < 0.05, "middle: males");
        assertTrue(TurtleSex.femaleChance(m, 29.0) > 0.9, "warm nest: females");
        assertTrue(TurtleSex.femaleChance(m, TurtleSex.maleTemp(m)) < 0.01);
    }

    @Test
    void incubationFollowsTheTemperature() {
        TurtleSpecies s = TurtleSpecies.SLIDER;
        assertEquals(0, TurtleSex.rate(s, 17, 1), 1e-9, "below 18 °C it waits");
        assertEquals(1 / s.eggDays(), TurtleSex.rate(s, s.pivot(), 1), 1e-9, "eggDays at the pivot");
        assertTrue(TurtleSex.rate(s, 31, 1) > TurtleSex.rate(s, 27, 1), "warmer is faster");
        assertEquals(2 * TurtleSex.rate(s, 27, 1), TurtleSex.rate(s, 27, 2), 1e-9, "incubationSpeed");
    }

    @Test
    void theThermosensitivePeriodDecidesTheSex() {
        TurtleClutch k = new TurtleClutch(1, TurtleSpecies.SLIDER, 5, Vec3.ZERO, BlockPos.ZERO, 7L);
        // warm first third, cool middle third, warm again: males (only the middle counts)
        int steps = 0;
        while (!k.ready() && steps++ < 100000) {
            double temp = k.progress < TurtleSex.TSP_FROM || k.progress >= TurtleSex.TSP_TO ? 32 : 26;
            k.incubate(temp, 0.01, 1);
        }
        assertTrue(k.ready(), "hatched");
        assertEquals(26, k.tspTemp(), 0.5, "the mean of the middle third");
        assertTrue(k.femaleShare() < 0.05, "a cool middle third: males");
        assertFalse(k.cooked());
    }

    @Test
    void heatKillsTheEmbryos() {
        TurtleClutch k = new TurtleClutch(1, TurtleSpecies.HERMANN, 3, Vec3.ZERO, BlockPos.ZERO, 7L);
        for (int i = 0; i < 40; i++) k.incubate(38, 0.01, 1);
        assertTrue(k.cooked(), "above 36 °C for a quarter day");
    }

    @Test
    void clutchSavesAndLoads() {
        TurtleClutch k = new TurtleClutch(4, TurtleSpecies.REEVES, 3, new Vec3(1.5, 2.25, 3.5), new BlockPos(10, 2, 30), 99L);
        k.incubate(28, 1.5, 1);
        k.motherVariety = 0;
        k.generation = 2;
        TurtleClutch l = TurtleClutch.load(k.save());
        assertEquals(k.progress, l.progress, 1e-12);
        assertEquals(k.tspTemp(), l.tspTemp(), 1e-9);
        assertEquals(99L, l.seed);
        assertEquals(new BlockPos(10, 2, 30), l.interiorPos());
        assertEquals(2, l.generation);
    }

    @Test
    void grownSlidersNeedABigTank() {
        TerrariumNeeds n = TurtleSpecies.SLIDER.needs(true);
        var small = new TerrariumNeeds.Conditions(TerrariumMode.PALUDARIUM, 27, 70, 5, 39, 0.5, 25, 8);
        var big = new TerrariumNeeds.Conditions(TerrariumMode.PALUDARIUM, 27, 70, 5, 39, 0.5, 25, 18);
        assertTrue(n.assess(small).problems().contains(TerrariumNeeds.Problem.TOO_SMALL));
        assertFalse(n.assess(small).harmful(), "a small tank only stresses");
        assertTrue(n.assess(big).fine(), "fine: " + n.assess(big).problems());
        assertTrue(TurtleSpecies.SLIDER.needs(false).assess(small).fine(), "a hatchling fits anywhere");
    }

    @Test
    void speciesData() {
        assertEquals("hermann_tortoise_box", TurtleSpecies.HERMANN.containerId());
        assertEquals("slider_turtle_bucket", TurtleSpecies.SLIDER.containerId());
        assertEquals("slider_turtle_elegans", TurtleSpecies.SLIDER.texture(0));
        assertEquals("slider_turtle_scripta", TurtleSpecies.SLIDER.texture(1));
        assertEquals("musk_turtle", TurtleSpecies.MUSK.texture(0));
        assertTrue(TurtleSpecies.HERMANN.brumates() && TurtleSpecies.HERMANN.box() && !TurtleSpecies.HERMANN.animalFood() && TurtleSpecies.HERMANN.greens());
        assertEquals(TerrariumNeeds.Habitat.DRY, TurtleSpecies.HERMANN.needs().habitat());
        for (TurtleSpecies s : TurtleSpecies.ALL) assertTrue(s.eggsMin() >= 1 && s.eggsMax() >= s.eggsMin() && s.eggsMax() <= 6, s.id());
    }
}
