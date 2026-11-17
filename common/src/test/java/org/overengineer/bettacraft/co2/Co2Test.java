package org.overengineer.bettacraft.co2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.chem.ChemTolerance;
import org.overengineer.bettacraft.chem.WaterChemistry;
import org.overengineer.bettacraft.osmosis.RoUnitBlockEntity;

/** M25 part b: the pure CO2 model (balance, harm, bio CO2, filling) and the reverse osmosis numbers. */
class Co2Test {
    @Test
    void cylinderDosesAGoodLevelAndAerationStripsIt() {
        // an 18-block tank with an open top (oxygen exchange 1 per day), a few fish, no plants: three bubbles per second
        TankCo2.Balance still = TankCo2.balance(3, 18, 6, 25, 0, 0, 3, 1.0);
        assertTrue(still.target() > 15 && still.target() < 35, "3 bubbles/s in a still tank: " + still.target());
        // strong aeration (an air stone: exchange about 2.7) drives most of it out
        TankCo2.Balance aerated = TankCo2.balance(3, 18, 6, 25, 0, 0, 3, 2.7);
        assertTrue(aerated.target() < still.target() / 2, "aeration strips CO2: " + aerated.target());
        // ten bubbles per second in the still tank is deadly
        assertTrue(TankCo2.balance(10, 18, 6, 25, 0, 0, 3, 1.0).target() > 50, "overdose");
        // dense plants in strong light take a good part of it up
        TankCo2.Balance planted = TankCo2.balance(3, 18, 6, 25, 1, 18, 25, 1.0);
        assertTrue(planted.uptake() > 20 && planted.target() < still.target(), "plants: " + planted.target());
        // no source at all: the breath alone hardly moves it from the air's value
        assertTrue(TankCo2.balance(0, 18, 6, 25, 0, 0, 3, 1.0).target() < WaterChemistry.CO2_AIR + 1.5, "breath only");
    }

    @Test
    void tooMuchCo2HarmsTheAnimals() {
        assertEquals(0, TankCo2.distance(30, 35, 50, 1), 1e-9);
        double stress = TankCo2.distance(40, 35, 50, 1);
        assertTrue(stress > 0 && stress < ChemTolerance.BAD_FROM, "stressed: " + stress);
        assertEquals(ChemTolerance.Fit.STRESS, ChemTolerance.fit(stress));
        assertEquals(ChemTolerance.Fit.BAD, ChemTolerance.fit(TankCo2.distance(55, 35, 50, 1)));
        // bettas and gouramis breathe air: 40 mg/L does not trouble them; shrimp mind it earlier
        assertEquals(0, TankCo2.distance(40, 35, 50, TankCo2.LABYRINTH), 1e-9);
        assertEquals(ChemTolerance.Fit.BAD, ChemTolerance.fit(TankCo2.distance(45, 35, 50, TankCo2.SHRIMP)));
        assertEquals(TankCo2.LABYRINTH, ChemTolerance.co2Tolerance(ChemTolerance.BETTA), 1e-9);
        assertEquals(TankCo2.LABYRINTH, ChemTolerance.co2Tolerance(ChemTolerance.GOURAMI), 1e-9);
        assertEquals(1.0, ChemTolerance.co2Tolerance(ChemTolerance.SOFT), 1e-9);
    }

    @Test
    void plantsGrowFasterWithCo2() {
        assertEquals(1.0, WaterChemistry.plantGrowth(3, 2), 1e-9);
        assertEquals(2.5, WaterChemistry.plantGrowth(25, 2), 1e-9);
        assertEquals(2.0, WaterChemistry.plantGrowth(25, 1), 1e-9);
        assertEquals(1.5, WaterChemistry.plantGrowth(25, 0), 1e-9);
    }

    @Test
    void bioCo2StartsSlowlyHoldsAndFades() {
        assertEquals(0, BioCo2BlockEntity.curve(0), 1e-9);
        assertTrue(BioCo2BlockEntity.curve(0.05) < 0.5);
        assertEquals(1, BioCo2BlockEntity.curve(0.4), 1e-9);
        assertTrue(BioCo2BlockEntity.curve(0.9) < 0.3);
        assertEquals(0, BioCo2BlockEntity.curve(1), 1e-9);
        // uneven, but always 0.7..1.3 and the same for the same time
        double min = 2, max = 0;
        for (long t = 0; t < 400000; t += 4000) {
            double u = BioCo2BlockEntity.unevenness(12345L, t);
            min = Math.min(min, u);
            max = Math.max(max, u);
            assertEquals(u, BioCo2BlockEntity.unevenness(12345L, t + 100), 1e-12);
        }
        assertTrue(min >= 0.7 && max <= 1.3 && max - min > 0.2, min + ".." + max);
        // four sugar: the full time, one sugar a quarter
        assertEquals(4.0, BioCo2BlockEntity.daysFor(4), 1e-9);
        assertEquals(1.0, BioCo2BlockEntity.daysFor(1), 1e-9);
    }

    @Test
    void stationFillsWithFlueGas() {
        assertEquals(0, Co2StationBlockEntity.fill(0, 24000, 60, 24000), 1e-9);
        assertEquals(60, Co2StationBlockEntity.fill(1, 24000, 60, 24000), 1e-9);
        assertEquals(60, Co2StationBlockEntity.fill(2, 12000, 60, 24000), 1e-9);
    }

    @Test
    void reverseOsmosisYieldAndWear() {
        assertEquals(4000, RoUnitBlockEntity.feedFor(1000, 3), 1e-9);
        assertEquals(250, RoUnitBlockEntity.roFrom(1000, 3), 1e-9);
        // a membrane for 128 buckets: 100 wear points after 128000 mB
        assertEquals(100, RoUnitBlockEntity.wear(128000, 128), 1e-9);
        assertEquals(1, RoUnitBlockEntity.wear(1280, 128), 1e-9);
    }
}
