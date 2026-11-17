package org.overengineer.bettacraft.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** M9 water quality and evaporation model. */
class WaterQualityTest {
    private static final WaterQuality.Params P = WaterQuality.Params.DEFAULT;

    @Test
    void declinesWithAnimalsAndTime() {
        // empty tank: only the base loss of 1 % per day
        assertEquals(0.99F, WaterQuality.step(1.0F, 1.0, 0, 4, 0, P), 1e-5);
        // 2 animals in 4 blocks: 1 % + 0.5 x 4 % = 3 % per day
        assertEquals(0.97F, WaterQuality.step(1.0F, 1.0, 2, 4, 0, P), 1e-5);
        assertEquals(0.70F, WaterQuality.step(1.0F, 10.0, 2, 4, 0, P), 1e-4);
        // never below 0
        assertEquals(0.0F, WaterQuality.step(0.1F, 100.0, 10, 1, 0, P), 1e-6);
        assertEquals(1.0F, WaterQuality.step(1.0F, 0.0, 10, 1, 0, P), 1e-6, "no time, no change");
    }

    @Test
    void filtersRemoveMostOfTheWaste() {
        float without = WaterQuality.step(1.0F, 10, 2, 4, 0, P);
        float filtered = WaterQuality.step(1.0F, 10, 2, 4, 32, P);
        assertEquals(1 - 0.3F * 0.2F, filtered, 1e-4, "a large enough filter removes 80 %");
        assertTrue(filtered > without);
        // a filter half as large as the water removes half of that
        float half = WaterQuality.step(1.0F, 10, 2, 4, 2, P);
        assertEquals(1 - 0.3F * 0.6F, half, 1e-4);
        assertEquals(0.4, WaterQuality.filterShare(2, 4, P), 1e-9);
    }

    @Test
    void overfeedingFoulsTheWater() {
        assertEquals(0.99F, WaterQuality.overfed(1.0F, 4, 4, 0, P), 1e-5, "4 portions in 4 blocks: 1 %");
        assertEquals(1.0F, WaterQuality.overfed(1.0F, 0, 4, 0, P), 1e-6);
        assertEquals(1 - 0.002F, WaterQuality.overfed(1.0F, 4, 4, 100, P), 1e-5, "the filter takes most of it");
    }

    @Test
    void freshWaterDilutesEvaporationConcentrates() {
        // 1000 mB at 50 % + 1000 mB fresh = 75 %
        assertEquals(0.75F, WaterQuality.mix(0.5F, 1000, 1000), 1e-5);
        assertEquals(1.0F, WaterQuality.mix(0.2F, 0, 500), 1e-6, "an empty tank gets clean water");
        assertEquals(0.5F, WaterQuality.mix(0.5F, 1000, 0), 1e-6);
        // drain half and refill: a classic 50 % water change
        float q = WaterQuality.mix(0.4F, 2000, 2000);
        assertEquals(0.7F, q, 1e-5);
        // 2000 mB at 80 % loses 1000 mB of pure water: the waste stays, 60 %
        assertEquals(0.6F, WaterQuality.concentrate(0.8F, 2000, 1000), 1e-5);
        assertEquals(0.8F, WaterQuality.concentrate(0.8F, 2000, 0), 1e-6);
        assertEquals(0.0F, WaterQuality.concentrate(0.2F, 1000, 900), 1e-6, "clamped");
    }

    @Test
    void states() {
        assertEquals(WaterQuality.State.CLEAN, WaterQuality.state(0.9F, P));
        assertEquals(WaterQuality.State.GOOD, WaterQuality.state(0.7F, P));
        assertEquals(WaterQuality.State.POOR, WaterQuality.state(0.5F, P));
        assertEquals(WaterQuality.State.BAD, WaterQuality.state(0.2F, P));
        assertTrue(WaterQuality.isBad(0.34F, P) && !WaterQuality.isBad(0.36F, P));
        assertEquals(1.0F, WaterQuality.clamp(Float.NaN));
    }

    @Test
    void evaporationGrowsWithOpenBlocksAndTemperature() {
        assertEquals(0.0, WaterQuality.evaporationPerDay(0, 25, 20), 1e-9, "covered");
        assertEquals(80.0, WaterQuality.evaporationPerDay(4, 25, 20), 1e-9);
        assertEquals(40.0, WaterQuality.evaporationPerDay(1, 45, 20), 1e-9, "twice as much at 45 C");
        assertEquals(5.0, WaterQuality.evaporationPerDay(1, 5, 20), 1e-9, "a quarter when cold");
        assertEquals(0.0, WaterQuality.evaporationPerDay(4, 25, 0), 1e-9, "switched off");
    }
}
