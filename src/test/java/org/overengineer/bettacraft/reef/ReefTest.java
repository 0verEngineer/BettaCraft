package org.overengineer.bettacraft.reef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantColors;
import org.overengineer.bettacraft.plants.PlantPart;
import org.overengineer.bettacraft.plants.PlantShapes;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/** M20: salinity mixing, tolerances and salt shock, corals' needs and colours, the reef technique (pure parts). */
class ReefTest {
    @Test
    void salinityMixesByVolumeAndEvaporationConcentrates() {
        // a bucket of sea water into 3 buckets of fresh water: a quarter of sea water
        assertEquals(8.75F, Salinity.mix(0F, 3000, Salinity.SEA, 1000), 1e-4);
        // fresh water into sea water dilutes it
        assertEquals(17.5F, Salinity.mix(35F, 1000, 0F, 1000), 1e-4);
        // an empty tank takes the salinity of what comes in
        assertEquals(35F, Salinity.mix(0F, 0, 35F, 1000), 1e-4);
        // a tenth evaporates: the salt stays (35 -> 38.9)
        assertEquals(38.888F, Salinity.concentrate(35F, 10000, 1000, Salinity.MAX), 1e-2);
        assertEquals(Salinity.MAX, Salinity.concentrate(55F, 10000, 5000, Salinity.MAX), 1e-4);
        assertEquals(0F, Salinity.concentrate(0F, 10000, 1000, Salinity.MAX), 1e-6, "fresh water stays fresh");
        // one portion of sea salt per bucket: 4 portions in 4 buckets = sea water
        assertEquals(35F, Salinity.addSalt(0F, 4000, 4), 1e-4);
        assertEquals(4, ReefItems.itemsToSea(0F, 4000));
        assertEquals(2, ReefItems.itemsToSea(17.5F, 4000));
        assertEquals(0, ReefItems.itemsToSea(35F, 4000));
        // calcium follows the salt
        assertEquals(Salinity.SEA_CALCIUM, Salinity.calciumOf(Salinity.SEA), 1e-3);
        assertEquals(Salinity.Kind.FRESH, Salinity.kind(0.5));
        assertEquals(Salinity.Kind.BRACKISH, Salinity.kind(8));
        assertEquals(Salinity.Kind.MARINE, Salinity.kind(34));
    }

    @Test
    void tankDataKeepsSaltOnMergeLikeSettings() {
        TankData t = TankData.create(java.util.List.of(net.minecraft.core.BlockPos.ZERO), 16);
        assertEquals(0F, t.salinity(), "a new tank is a freshwater tank");
        t.setSalinity(35F);
        t.setCalcium(420F);
        TankData copy = TankData.create(java.util.List.of(net.minecraft.core.BlockPos.ZERO), 16);
        copy.copySettingsFrom(t);
        assertEquals(35F, copy.salinity(), 1e-5);
        assertEquals(420F, copy.calcium(), 1e-3);
        t.setSalinity(500F);
        assertEquals(Salinity.MAX, t.salinity(), 1e-5);
    }

    @Test
    void tolerancesAndSaltShock() {
        // a betta in sea water: deadly within hours
        double betta = Salinity.shockPerDay(SaltTolerance.BETTA, 35, 1);
        assertTrue(betta > 3, "betta in sea water: " + betta);
        assertTrue(Salinity.deadly(SaltTolerance.BETTA, 35));
        // a pinch of salt is fine, brackish water only stresses
        assertEquals(0, Salinity.shockPerDay(SaltTolerance.BETTA, 3, 1));
        assertFalse(Salinity.deadly(SaltTolerance.BETTA, 8));
        // the nerites and the livebearers tolerate brackish water
        assertEquals(0, Salinity.shockPerDay(SaltTolerance.critter(CritterSpecies.ZEBRA_NERITE), 20, 1));
        assertEquals(0, Salinity.shockPerDay(TankmateSpecies.GUPPY.salinity(), 12, 1));
        assertTrue(Salinity.shockPerDay(TankmateSpecies.NEON_TETRA.salinity(), 12, 1) > 1);
        // the reef animals need sea water
        assertTrue(Salinity.deadly(TankmateSpecies.OCELLARIS_CLOWNFISH.salinity(), 0));
        assertEquals(0, Salinity.shockPerDay(TankmateSpecies.OCELLARIS_CLOWNFISH.salinity(), 35, 1));
        assertTrue(Salinity.deadly(SaltTolerance.critter(CritterSpecies.CLEANER_SHRIMP), 2));
        assertEquals(0, Salinity.shockPerDay(SaltTolerance.critter(CritterSpecies.HERMIT_CRAB), 22, 1), "hermits are hardy");
        assertTrue(CritterSpecies.TURBO_SNAIL.marine() && !CritterSpecies.NEOCARIDINA.marine());
        assertEquals(0, Salinity.shockPerDay(SaltTolerance.BETTA, 35, 0), "config 0: salt does no harm");
        for (TankmateSpecies s : TankmateSpecies.values()) {
            boolean reef = s.group() == TankmateSpecies.Group.CLOWNFISH || s.group() == TankmateSpecies.Group.TANG
                    || s.group() == TankmateSpecies.Group.GOBY || s.group() == TankmateSpecies.Group.DAMSEL;
            assertEquals(reef, s.marine(), s + " marine");
        }
    }

    @Test
    void healthDiesOfSaltShockAndRecovers() {
        BettaHealth.Params p = BettaHealth.Params.DEFAULT;
        BettaHealth h = new BettaHealth();
        h.feed();
        // in sea water a betta dies within a day
        BettaHealth.Event e = h.step(0.5, new BettaHealth.Conditions(false, false, true, Salinity.shockPerDay(SaltTolerance.BETTA, 35, 1)), p);
        assertEquals(BettaHealth.Event.DIED, e);
        // a short shock fades in the right water
        BettaHealth k = new BettaHealth();
        k.feed();
        assertEquals(BettaHealth.Event.NONE, k.step(0.05, new BettaHealth.Conditions(false, false, false, 4.0), p));
        assertTrue(k.saltShock() > 0.15);
        assertTrue(k.healthFraction(p) < 0.85F);
        k.step(0.5, BettaHealth.Conditions.GOOD, p);
        assertEquals(0, k.saltShock(), 1e-9);
        // saved and loaded
        BettaHealth m = new BettaHealth();
        m.step(0.05, new BettaHealth.Conditions(false, false, false, 4.0), p);
        assertEquals(m.saltShock(), BettaHealth.load(m.save()).saltShock(), 1e-9);
    }

    private static InteriorPlants.Env env(double salt, double flow, double calcium, double spectrum, double quality, boolean glow, double temp) {
        return new InteriorPlants.Env(15, 15, 1, 1, 100, 0, salt, flow, calcium, spectrum, quality, glow, Corals.temperatureFactor(temp));
    }

    @Test
    void coralsNeedSaltLightFlowAndCalcium() {
        InteriorPlants.Env good = env(35, 1.0, 420, 1.0, 0.9, true, 26);
        for (PlantSpecies s : PlantSpecies.values()) {
            if (!s.marine()) continue;
            assertTrue(Corals.rate(good, s, 15) > 0.9, s + " grows in a good reef tank: " + Corals.rate(good, s, 15));
            assertEquals(0, Corals.rate(env(0, 1.0, 420, 1.0, 0.9, true, 26), s, 15), s + " not in fresh water");
            assertEquals(0, Corals.rate(env(35, 1.0, 420, 1.0, 0.9, true, 35), s, 15), s + " not in hot water");
            assertEquals(3, Corals.tint(good, s), s + " glows under actinic light");
            assertEquals(0, Corals.tint(env(5, 1.0, 420, 1.0, 0.9, true, 26), s), s + " bleaches in brackish water");
            assertEquals(2, Corals.tint(env(35, 1.0, 420, 0.7, 0.9, false, 26), s), s + " healthy under white light");
        }
        // stony corals need calcium and clean water, soft corals do not
        InteriorPlants.Env noCa = env(35, 1.0, 200, 1.0, 0.9, true, 26);
        assertEquals(0, Corals.rate(noCa, PlantSpecies.ACROPORA, 15));
        assertTrue(Corals.rate(noCa, PlantSpecies.ZOANTHUS, 15) > 0.9);
        assertEquals(0, Corals.rate(env(35, 1.0, 420, 1.0, 0.3, true, 26), PlantSpecies.ACROPORA, 15), "Acropora stops in dirty water");
        // Acropora wants strong flow, mushrooms little; still water is bad for all
        assertTrue(Corals.rate(env(35, 0.2, 420, 1.0, 0.9, true, 26), PlantSpecies.ACROPORA, 15) < 0.35);
        assertTrue(Corals.rate(env(35, 0.2, 420, 1.0, 0.9, true, 26), PlantSpecies.MUSHROOM_CORAL, 15) > 0.9);
        assertTrue(Corals.rate(env(35, 0, 420, 1.0, 0.9, true, 26), PlantSpecies.MUSHROOM_CORAL, 15) <= 0.1 + 1e-9);
        // white light only gives 70 %, actinic + white 100 %
        assertEquals(1.0, TankReef.spectrum(1, 1, 0), 1e-9);
        assertEquals(0.7, TankReef.spectrum(2, 0, 0), 1e-9);
        assertEquals(0.85, TankReef.spectrum(0, 3, 0), 1e-9);
        assertTrue(TankReef.spectrum(0, 0, 2) < 0.5);
        assertEquals(0.6, TankReef.spectrum(0, 0, 0), 1e-9);
        // flow: pumps over the volume
        assertEquals(1.0, TankReef.flow(24, 24), 1e-9);
        assertEquals(3.0, TankReef.flow(1000, 1), 1e-9);
        // freshwater plants stop in salt water
        assertEquals(1, Corals.freshFactor(2), 1e-9);
        assertEquals(0, Corals.freshFactor(35), 1e-9);
        assertEquals(0, new InteriorPlants.Env(15, 15, 1, 1, 100, 0, 35, 0, 0, 0.6, 0.9, false, 1).rate(rotala()), 1e-9);
    }

    private static org.overengineer.bettacraft.plants.InteriorPlant rotala() {
        var r = new org.overengineer.bettacraft.plants.PlantIndex();
        return r.add(PlantSpecies.ROTALA, 1, 1, 1, 7L, 0);
    }

    @Test
    void coralColoursAndShapes() {
        for (PlantSpecies s : PlantSpecies.values()) {
            if (!s.marine()) continue;
            int bleached = PlantColors.color(s, PlantPart.LEAF, 0), healthy = PlantColors.color(s, PlantPart.LEAF, 2), glow = PlantColors.color(s, PlantPart.TOP, 3);
            assertNotEquals(bleached, healthy, s + " bleaches");
            assertTrue(brightness(bleached) > brightness(healthy), s + " bleached is whiter");
            assertNotEquals(PlantColors.color(s, PlantPart.TOP, 2), glow, s + " glows");
            // a coral sits on its rock: nothing below the root except the polyp mats
            for (long seed = 1; seed <= 6; seed++) {
                for (PlantShapes.Voxel v : PlantShapes.of(s, seed, 16, 30).all()) {
                    if (s.form() != PlantSpecies.Form.POLYPS) assertTrue(v.y() >= 0, s + " below its rock");
                }
            }
            assertTrue(s.form().coral() && !s.form().rooted());
        }
        assertTrue(PlantSpecies.ACROPORA.stony() && !PlantSpecies.ZOANTHUS.stony() && PlantSpecies.BUBBLE_TIP_ANEMONE.anemone());
        assertTrue(PlantShapes.of(PlantSpecies.LOBOPHYLLIA, 3, 16, 30).all().size() > PlantShapes.of(PlantSpecies.LOBOPHYLLIA, 3, 4, 30).all().size());
    }

    private static int brightness(int rgb) {
        return ((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF);
    }

    @Test
    void waterColourTurnsBlueWithSalt() {
        int fresh = 0x4A90E2;
        assertEquals(fresh, Salinity.waterColor(fresh, 0));
        int sea = Salinity.waterColor(fresh, 35);
        assertNotEquals(fresh, sea);
        assertEquals(35, org.overengineer.bettacraft.tank.Salinity.nitrate(0.65F));
    }
}
