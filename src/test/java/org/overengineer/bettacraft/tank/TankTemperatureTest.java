package org.overengineer.bettacraft.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Temperature model (PLAN 6.1). */
class TankTemperatureTest {
    @Test
    void biomeMapping() {
        assertEquals(25.0F, TankTemperature.ambientFromBiome(0.8F), 1e-4, "swamp");
        assertEquals(12.0F, TankTemperature.ambientFromBiome(0.25F), 1e-4, "taiga");
        assertEquals(32.0F, TankTemperature.ambientFromBiome(2.0F), 1e-4, "desert");
        assertEquals(5.0F, TankTemperature.ambientFromBiome(0.0F), 1e-4, "snow");
        assertTrue(TankTemperature.ambientFromBiome(-0.5F) < 5.0F, "frozen is colder");
        assertTrue(TankTemperature.ambientFromBiome(-10F) >= -5.0F, "clamped");
        assertEquals(32.0F, TankTemperature.ambientFromBiome(3.0F), 1e-4);
        float mid = TankTemperature.ambientFromBiome(1.4F);
        assertTrue(mid > 25 && mid < 32, "interpolated");
    }

    @Test
    void dayNight() {
        assertEquals(2.0F, TankTemperature.dayNightOffset(6000), 1e-4, "noon");
        assertEquals(-2.0F, TankTemperature.dayNightOffset(18000), 1e-4, "midnight");
        assertEquals(0.0F, TankTemperature.dayNightOffset(0), 1e-4, "sunrise");
        assertEquals(2.0F, TankTemperature.dayNightOffset(24000 * 5 + 6000), 1e-4, "any day");
    }

    @Test
    void heatersOnlyHeatAndScaleWithCapacity() {
        assertEquals(27.0F, TankTemperature.equilibrium(12, 27, 16, 16), 1e-4, "enough capacity");
        assertEquals(27.0F, TankTemperature.equilibrium(12, 27, 64, 16), 1e-4, "more than enough");
        assertEquals(19.5F, TankTemperature.equilibrium(12, 27, 8, 16), 1e-4, "half capacity, half way");
        assertEquals(12.0F, TankTemperature.equilibrium(12, 27, 0, 16), 1e-4, "no heater");
        assertEquals(32.0F, TankTemperature.equilibrium(32, 27, 64, 16), 1e-4, "no cooling");
    }

    @Test
    void exponentialApproach() {
        float t = TankTemperature.approach(12, 27, 0.1, 0.1);
        assertEquals(27 - 15 / Math.E, t, 1e-3, "one time constant");
        assertEquals(27.0F, TankTemperature.approach(12, 27, 10, 0.1), 1e-3, "settles");
        assertEquals(12.0F, TankTemperature.approach(12, 27, 0, 0.1), 1e-6, "no time");
        // two half steps equal one full step
        float a = TankTemperature.approach(TankTemperature.approach(10, 30, 0.05, 0.2), 30, 0.05, 0.2);
        assertEquals(TankTemperature.approach(10, 30, 0.1, 0.2), a, 1e-4);
    }

    @Test
    void tolerance() {
        assertTrue(TankTemperature.isTolerable(24));
        assertTrue(TankTemperature.isTolerable(30));
        assertFalse(TankTemperature.isTolerable(23.9F));
        assertFalse(TankTemperature.isTolerable(30.1F));
        assertEquals(20.0F, TankTemperature.clampTarget(5));
        assertEquals(32.0F, TankTemperature.clampTarget(50));
    }
}
