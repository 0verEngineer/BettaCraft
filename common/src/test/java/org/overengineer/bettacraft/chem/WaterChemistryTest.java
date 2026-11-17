package org.overengineer.bettacraft.chem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/** M25: the pure water chemistry model (pH equilibrium, TDS, hardness, mixing) and the species' ranges. */
class WaterChemistryTest {
    private static WaterChemistry.Inputs in(double tds, double acid, double alkaline, double peat, double tannin, double wood, double co2) {
        return new WaterChemistry.Inputs(tds, 0, WaterChemistry.SEA_KH, acid, alkaline, peat, tannin, wood, co2);
    }

    @Test
    void tapWaterStaysWhereItIs() {
        assertEquals(WaterChemistry.TAP_PH, WaterChemistry.phTarget(WaterChemistry.Inputs.plain(WaterChemistry.TAP_TDS, WaterChemistry.CO2_AIR)), 1e-6);
        assertEquals(WaterChemistry.Hardness.MEDIUM, WaterChemistry.hardness(WaterChemistry.TAP_TDS, 0, 0, false));
        // approaching a target it already has changes nothing
        assertEquals(7.5F, WaterChemistry.approachPh(7.5F, 7.5, 3), 1e-6);
    }

    @Test
    void substrateAndAdditivesMoveThePh() {
        double tap = WaterChemistry.phTarget(in(250, 0, 0, 0, 0, 0, 3));
        double soil = WaterChemistry.phTarget(in(250, 1, 0, 0, 0, 0, 3));
        double lime = WaterChemistry.phTarget(in(250, 0, 1, 0, 0, 0, 3));
        assertTrue(soil < 6.6 && soil > 6.2, "soil: " + soil);
        assertTrue(lime > 7.9 && lime < 8.3, "limestone: " + lime);
        assertTrue(WaterChemistry.phTarget(in(250, 0, 0, 1, 0, 0, 3)) < tap - 0.5, "peat");
        assertTrue(WaterChemistry.phTarget(in(250, 0, 0, 0, 1, 0, 3)) < tap - 0.3, "leaves");
        assertTrue(WaterChemistry.phTarget(in(250, 0, 0, 0, 0, 1, 3)) < tap - 0.2, "wood");
        // CO2: one pH unit per tenfold CO2
        assertEquals(tap - 1, WaterChemistry.phTarget(in(250, 0, 0, 0, 0, 0, 30)), 1e-6);
        // reverse osmosis water is unbuffered and slightly acidic; everything at once ends at the floor of 5
        assertEquals(6.2, WaterChemistry.phTarget(in(0, 0, 0, 0, 0, 0, 3)), 1e-6);
        assertEquals(WaterChemistry.PH_MIN, WaterChemistry.phTarget(in(0, 1, 0, 1, 1, 1, 100)), 1e-6);
        // the pH gets most of the way in a day
        float p = WaterChemistry.approachPh(7.5F, 6.5, 1);
        assertTrue(p < 6.8 && p > 6.5, "after a day: " + p);
    }

    @Test
    void saltWaterIsBufferedByItsAlkalinity() {
        var sea = new WaterChemistry.Inputs(250, 35, WaterChemistry.SEA_KH, 1, 0, 0, 0, 0, 3);
        assertEquals(WaterChemistry.SEA_PH, WaterChemistry.phTarget(sea), 1e-4);
        assertEquals(WaterChemistry.Hardness.HARD, WaterChemistry.hardness(0, 1, 0, true));
        var brackish = new WaterChemistry.Inputs(250, 10, WaterChemistry.SEA_KH, 0, 0, 0, 0, 0, 3);
        double b = WaterChemistry.phTarget(brackish);
        assertTrue(b > 7.5 && b < 8.2, "brackish: " + b);
    }

    @Test
    void hardnessFollowsTdsAndSubstrate() {
        assertEquals(WaterChemistry.Hardness.SOFT, WaterChemistry.hardness(0, 0, 0, false));
        assertEquals(WaterChemistry.Hardness.SOFT, WaterChemistry.hardness(200, 1, 0, false), "soil softens");
        assertEquals(WaterChemistry.Hardness.HARD, WaterChemistry.hardness(250, 0, 1, false), "limestone hardens");
        assertEquals(WaterChemistry.Hardness.HARD, WaterChemistry.hardness(400, 0, 0, false));
    }

    @Test
    void tdsRisesWithWasteAndWaterChangesHoldIt() {
        assertEquals(0, WaterChemistry.tdsPerDay(250, 0, 1.5, 0, 0), 1e-9);
        assertEquals(1.5, WaterChemistry.tdsPerDay(250, 1, 1.5, 0, 0), 1e-9);
        assertTrue(WaterChemistry.tdsPerDay(100, 0, 1.5, 0, 1) > 0, "limestone dissolves");
        assertTrue(WaterChemistry.tdsPerDay(250, 0, 1.5, 1, 0) < 0, "soil takes a little");
        // a normally stocked tank with 25 % tap water every 14 days stays well inside every tolerant range
        float tds = WaterChemistry.TAP_TDS;
        float max = 0;
        for (int change = 0; change < 40; change++) {
            tds = WaterChemistry.stepTds(tds, WaterChemistry.tdsPerDay(tds, 1, 1.5, 0, 0), 14);
            max = Math.max(max, tds);
            tds = WaterChemistry.mix(tds, 750, WaterChemistry.TAP_TDS, 250);
        }
        assertTrue(max > 300 && max < 350, "peak " + max);
        // evaporation leaves the minerals behind
        assertEquals(500F, WaterChemistry.concentrate(250F, 2000, 1000, WaterChemistry.TDS_MAX), 1e-3);
    }

    /** One game year of a 3x3x3 tank (27 water blocks, 9 open on top, 27 °C), normally stocked, topped off every day with normal water. */
    private static float[] year(int changeEveryDays, double share) {
        long water = 27_000;
        long lost = Math.round(org.overengineer.bettacraft.tank.WaterQuality.evaporationPerDay(9, 27F, 20.0));
        float tds = WaterChemistry.TAP_TDS;
        float[] days = new float[365];
        for (int d = 0; d < 365; d++) {
            tds = WaterChemistry.stepTds(tds, WaterChemistry.tdsPerDay(tds, 1, WaterChemistry.WASTE_TDS_PER_DAY, 0, 0), 1);
            tds = WaterChemistry.evaporateTds(tds, water, lost);
            tds = WaterChemistry.mix(tds, water - lost, WaterChemistry.TAP_TDS, lost); // the level sensor tops off
            if (changeEveryDays > 0 && (d + 1) % changeEveryDays == 0) {
                long change = Math.round(water * share);
                tds = WaterChemistry.mix(tds, water - change, WaterChemistry.TAP_TDS, change);
            }
            days[d] = tds;
        }
        return days;
    }

    @Test
    void topOffAloneClimbsButWaterChangesHoldTheTds() {
        // M25 part d: evaporation + top-off + 25 % water change every 14 days: the TDS settles near the normal water's
        float[] y = year(14, 0.25);
        float lo = Float.MAX_VALUE, hi = 0, hiMid = 0, hiEnd = 0;
        for (int d = 60; d < 365; d++) {
            lo = Math.min(lo, y[d]);
            hi = Math.max(hi, y[d]);
            if (d >= 150 && d < 240) hiMid = Math.max(hiMid, y[d]);
            if (d >= 275) hiEnd = Math.max(hiEnd, y[d]);
        }
        assertTrue(lo > WaterChemistry.TAP_TDS && hi < WaterChemistry.TAP_TDS * 1.3F, "band " + lo + ".." + hi);
        assertTrue(hi - lo < 40, "narrow band " + lo + ".." + hi);
        assertTrue(Math.abs(hiEnd - hiMid) < 2, "settled, does not keep rising: " + hiMid + " -> " + hiEnd);
        assertEquals(WaterChemistry.Hardness.MEDIUM, WaterChemistry.hardness(hi, 0, 0, false), "stays medium hard");
        // weekly changes hold it even closer
        float[] w = year(7, 0.25);
        assertTrue(w[364] < y[364] || w[364] < WaterChemistry.TAP_TDS * 1.2F, "weekly " + w[364]);
        // only topping off with normal water: it climbs for months (wanted, as in the real hobby)
        float[] t = year(0, 0);
        assertTrue(t[89] > hi && t[179] > t[89] + 20 && t[364] > t[179] + 20 && t[364] > 400, "top-off only " + t[89] + " " + t[179] + " " + t[364]);
        // soft water leaves no lime scale: evaporation concentrates it fully
        assertEquals(100F, WaterChemistry.evaporateTds(50F, 2000, 1000), 1e-3);
        assertTrue(WaterChemistry.evaporateTds(250F, 2000, 1000) < 400F, "hard water: part of it settles as lime scale");
    }

    @Test
    void mixingAndAdditives() {
        assertEquals(125F, WaterChemistry.mix(250F, 1000, 0F, 1000), 1e-4);
        // the pH mixes as acid: half pH 6 and half pH 8 is close to 6.3, not 7
        float ph = WaterChemistry.mixPh(6F, 1000, 8F, 1000);
        assertEquals(6.3F, ph, 0.01F);
        assertEquals(7.5F, WaterChemistry.mixPh(7.5F, 0, 7.5F, 1000), 1e-4);
        // remineralising salt: 400 ppm per bucket in 8 buckets = 50 ppm per portion
        assertEquals(50F, WaterChemistry.remineralise(0F, 8000, 1, 400), 1e-3);
        assertEquals(3, WaterChemistry.portionsTo(0F, 8000, 150, 400));
        assertEquals(0, WaterChemistry.portionsTo(200F, 8000, 150, 400));
        // almond leaves: one per block colours fully, half gone after the half-life
        assertEquals(0.5F, WaterChemistry.addLeaves(0F, 2000, 1, 1.0), 1e-4);
        assertEquals(1F, WaterChemistry.addLeaves(0.5F, 2000, 5, 1.0), 1e-4);
        assertEquals(0.25F, WaterChemistry.fadeTannin(0.5F, 7, 7), 1e-4);
    }

    @Test
    void driversFromTheInterior() {
        // half the floor soil, a 16-scale tank of 8 water blocks with a tenth of its volume in limestone rocks
        int vol = 16 * 16 * 16 * 8;
        var d = TankChemistry.drivers(100, 0, 50, 0, vol / 10, 0, 16, 8, 4, 0.2);
        assertEquals(0.25, d.acid(), 1e-9, "plain soil counts half");
        assertEquals(0, d.activeSoil(), 1e-9);
        // M25 part d: active soil counts fully, together with plain soil at most 1
        var a = TankChemistry.drivers(100, 50, 50, 0, 0, 0, 16, 8, 0, 0);
        assertEquals(0.75, a.acid(), 1e-9);
        assertEquals(0.5, a.activeSoil(), 1e-9);
        assertEquals(1.0, TankChemistry.drivers(100, 100, 0, 0, 0, 0, 16, 8, 0, 0).acid(), 1e-9);
        assertEquals(0.5, d.alkaline(), 1e-3, "rocks count half");
        assertEquals(0.5, d.peat(), 1e-9, "4 of 8 blocks filtered through peat");
        assertEquals(0.2, d.tannin(), 1e-9);
        assertEquals(TankChemistry.Drivers.NONE, TankChemistry.drivers(0, 0, 0, 0, 0, 0, 16, 8, 0, 0));
    }

    @Test
    void everyAnimalTakesTapWater() {
        // tanks of earlier versions start with tap water: no animal of ours may mind it
        for (TankmateSpecies s : TankmateSpecies.values()) {
            var r = ChemTolerance.tankmate(s);
            assertNotNull(r);
            double ph = s.marine() ? WaterChemistry.SEA_PH : WaterChemistry.TAP_PH;
            // M29: the soft-water cichlids and the chocolate gourami came after the water chemistry (no old tank holds them): soft water only
            if (s == TankmateSpecies.DISCUS || s == TankmateSpecies.AGASSIZ_DWARF_CICHLID || s == TankmateSpecies.CHOCOLATE_GOURAMI) {
                assertFalse(r.contains(ph, WaterChemistry.TAP_TDS), s + " needs soft water");
                assertTrue(r.contains(6.2, 150), r.describe());
                continue;
            }
            assertTrue(r.contains(ph, WaterChemistry.TAP_TDS), s + " " + r.describe());
        }
        for (CritterSpecies s : CritterSpecies.values()) {
            var r = ChemTolerance.critter(s);
            double ph = s.marine() ? WaterChemistry.SEA_PH : WaterChemistry.TAP_PH;
            // M25 part c: the Caridina came after the water chemistry and need soft water (no old tank holds them)
            if (s == CritterSpecies.CARIDINA) {
                assertFalse(r.contains(ph, WaterChemistry.TAP_TDS), "Caridina need soft water");
                assertTrue(r.contains(6.2, 150), r.describe());
                continue;
            }
            assertTrue(r.contains(ph, WaterChemistry.TAP_TDS), s + " " + r.describe());
        }
        assertTrue(ChemTolerance.BETTA.contains(WaterChemistry.TAP_PH, WaterChemistry.TAP_TDS));
        // a full soil floor only stresses the livebearers and snails, it does not make them sick
        double soil = WaterChemistry.phTarget(in(250, 1, 0, 0, 0, 0, 3));
        assertEquals(ChemTolerance.Fit.STRESS, ChemTolerance.fit(ChemTolerance.LIVEBEARER.distance(soil, 250)));
        assertEquals(ChemTolerance.Fit.STRESS, ChemTolerance.fit(ChemTolerance.critter(CritterSpecies.APPLE_SNAIL).distance(soil, 250)));
    }

    @Test
    void rangesAndFits() {
        var r = ChemTolerance.Range.fresh(6, 7, 100, 200);
        assertEquals(0, r.distance(6.5, 150), 1e-9);
        assertEquals(0.5, r.distance(7.5, 150), 1e-9);
        assertEquals(1.0, r.distance(6.5, 300), 1e-9);
        assertEquals(ChemTolerance.Fit.OK, ChemTolerance.fit(0));
        assertEquals(ChemTolerance.Fit.STRESS, ChemTolerance.fit(0.3));
        assertEquals(ChemTolerance.Fit.BAD, ChemTolerance.fit(0.5));
        // salt water animals only mind the pH
        assertEquals(0, ChemTolerance.Range.MARINE.distance(8.2, 2000), 1e-9);
        // soft-water fish take reverse osmosis water, livebearers do not; Caridina (part c) need it softened
        assertTrue(ChemTolerance.SOFT.contains(6.2, 0));
        assertEquals(ChemTolerance.Fit.BAD, ChemTolerance.fit(ChemTolerance.LIVEBEARER.distance(6.2, 0)));
        assertEquals(ChemTolerance.Fit.BAD, ChemTolerance.fit(ChemTolerance.CARIDINA.distance(WaterChemistry.TAP_PH, WaterChemistry.TAP_TDS)));
        assertEquals("pH 5.5–6.8 · TDS 100–200 ppm", ChemTolerance.CARIDINA.describe());
    }
}
