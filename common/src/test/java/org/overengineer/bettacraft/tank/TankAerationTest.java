package org.overengineer.bettacraft.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** M23: the oxygen model (saturation, equilibrium, approach), the air supply and the light timer curve. */
class TankAerationTest {
    @Test
    void saturationFallsWithWarmthAndSalt() {
        assertEquals(8.26, TankAeration.saturation(25, 0), 0.05);
        assertEquals(10.1, TankAeration.saturation(15, 0), 0.1);
        assertTrue(TankAeration.saturation(30, 0) < TankAeration.saturation(25, 0), "warm water holds less oxygen");
        assertTrue(TankAeration.saturation(25, 35) < 0.85 * TankAeration.saturation(25, 0), "sea water holds less oxygen");
        assertEquals(2.0, TankAeration.warmth(35), 1e-9);
        assertEquals(1.0, TankAeration.warmth(25), 1e-9);
    }

    @Test
    void equilibriumAndApproach() {
        double sat = 8.0;
        // no use: saturation; more exchange: closer to saturation
        assertEquals(sat, TankAeration.target(sat, 1.0, 0), 1e-9);
        assertTrue(TankAeration.target(sat, 2.0, 3) > TankAeration.target(sat, 0.5, 3), "an air stone keeps the oxygen up");
        assertEquals(0, TankAeration.target(sat, 0.5, 100), 1e-9);
        // plants in light can supersaturate a little, at most 30 %
        assertEquals(sat * 1.3, TankAeration.target(sat, 0.1, -100), 1e-9);
        // exact approach: after a long time at the target, after 0 days unchanged
        assertEquals(5.0, TankAeration.approach(8.0, 5.0, 1.0, 50), 1e-6);
        assertEquals(8.0, TankAeration.approach(8.0, 5.0, 1.0, 0), 1e-9);
        double half = TankAeration.approach(8.0, 4.0, Math.log(2), 1.0);
        assertEquals(6.0, half, 1e-6);
        assertEquals("good", TankAeration.state(6));
        assertEquals("low", TankAeration.state(3));
        assertEquals("critical", TankAeration.state(1));
    }

    @Test
    void airIsShared() {
        assertEquals(0, TankAeration.supply(4, 0), 1e-9);
        assertEquals(1, TankAeration.supply(4, 3), 1e-9);
        assertEquals(0.5, TankAeration.supply(4, 8), 1e-9);
        assertEquals(0, TankAeration.supply(0, 3), 1e-9);
        assertTrue(TankAeration.co2Stripping(6) == 1 && TankAeration.co2Stripping(0) == 0, "strong aeration strips all CO2");
    }

    @Test
    void daylightCurve() {
        // day time 0 = 6:00, 18000 = 0:00
        assertEquals(360, TankLighting.minuteOfDay(0));
        assertEquals(0, TankLighting.minuteOfDay(18000));
        assertEquals(720, TankLighting.minuteOfDay(6000));
        int on = 8 * 60, off = 20 * 60;
        assertEquals(0, TankLighting.curve(7 * 60, on, off), 1e-9);
        assertEquals(0.5, TankLighting.curve(on + 29, on, off), 1e-9);
        assertEquals(1, TankLighting.curve(12 * 60, on, off), 1e-9);
        assertTrue(TankLighting.curve(off + 30, on, off) > 0 && TankLighting.curve(off + 30, on, off) < 1, "sunset ramp");
        assertEquals(0, TankLighting.curve(22 * 60, on, off), 1e-9);
        // over midnight: on 22:00, off 2:00
        assertEquals(1, TankLighting.curve(0, 22 * 60, 2 * 60), 1e-9);
        assertEquals(0, TankLighting.curve(12 * 60, 22 * 60, 2 * 60), 1e-9);
        assertEquals(1, TankLighting.curve(5, 600, 600), 1e-9);
        assertEquals(0, TankLighting.dimStep(1.0));
        assertEquals(3, TankLighting.dimStep(0.2));
    }

    @Test
    void ledTiersAddLight() {
        // bar only: 15; a panel over half the tops: +1.25; high-power everywhere: +5
        assertEquals(15, TankLighting.ledLight(1.0, 0, 4), 1e-9);
        assertEquals(16.25, TankLighting.ledLight(1.0, 2 * 2.5, 4), 1e-9);
        assertEquals(20, TankLighting.ledLight(1.0, 4 * 5.0, 4), 1e-9);
        assertEquals(7.5, TankLighting.ledLight(0.5, 0, 4), 1e-9);
        assertTrue(TankLighting.algaeBoost(20) > TankLighting.algaeBoost(15), "strong light without CO2 brings more algae");
        assertEquals(1, TankLighting.algaeBoost(12), 1e-9);
    }
}
