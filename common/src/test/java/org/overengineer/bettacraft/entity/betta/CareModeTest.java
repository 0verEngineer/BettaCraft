package org.overengineer.bettacraft.entity.betta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.config.ServerConfig.CareMode;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterLife;
import org.overengineer.bettacraft.critters.CritterSpecies;

/**
 * M33 part c: {@code care.mode} in the pure health and small animal logic. NORMAL dies in hopeless conditions, NO_DEATH
 * stays sick at the brink without dying (and recovers when things get better), DECOR never gets hungry or sick.
 */
class CareModeTest {
    /** Cold, crowded, bad water and the wrong salinity, never fed. */
    private static final BettaHealth.Conditions HOPELESS = new BettaHealth.Conditions(true, true, true, 3.0);

    private static BettaHealth.Params params(CareMode m) {
        BettaHealth.Params d = BettaHealth.Params.DEFAULT;
        return new BettaHealth.Params(d.daysUntilSick(), d.daysSickUntilDeath(), d.bothBadMultiplier(), d.recoveryPerDay(), d.hungryAfterDays(), d.diet(), m);
    }

    private static boolean diesWithin(BettaHealth h, BettaHealth.Conditions c, BettaHealth.Params p, double days) {
        for (int i = 0; i < days * 100; i++) if (h.step(0.01, c, p) == BettaHealth.Event.DIED) return true;
        return false;
    }

    @Test
    void normalDies() {
        assertTrue(diesWithin(new BettaHealth(), HOPELESS, params(CareMode.NORMAL), 30));
        assertTrue(diesWithin(new BettaHealth(), new BettaHealth.Conditions(false, false, false, 0), params(CareMode.NORMAL), 60), "starves");
    }

    @Test
    void noDeathWaitsAtTheBrink() {
        BettaHealth.Params p = params(CareMode.NO_DEATH);
        BettaHealth h = new BettaHealth();
        assertFalse(diesWithin(h, HOPELESS, p, 200));
        assertTrue(h.isSick() && h.isHungry(p), "the needs stay");
        assertTrue(h.healthFraction(p) < 0.05F, "at the brink");
        // better water and food: it recovers
        for (int i = 0; i < 4000; i++) {
            if (i % 50 == 0) h.feed();
            h.step(0.01, BettaHealth.Conditions.GOOD, p);
        }
        assertFalse(h.isSick(), "recovered");
    }

    @Test
    void decorHasNoNeeds() {
        BettaHealth.Params p = params(CareMode.DECOR);
        BettaHealth h = new BettaHealth();
        BettaHealth.Event worst = BettaHealth.Event.NONE;
        for (int i = 0; i < 20000; i++) {
            BettaHealth.Event e = h.step(0.01, HOPELESS, p);
            if (e != BettaHealth.Event.NONE) worst = e;
        }
        assertEquals(BettaHealth.Event.NONE, worst, "neither sick nor dead in 200 days");
        assertFalse(h.isHungry(p));
        assertEquals(0.0, h.saltShock(), 1e-9);
        assertTrue(h.healthFraction(p) > 0.99F);
    }

    private static CritterLife.Env env(CareMode m) {
        Map<CritterSpecies.Group, Double> b = new EnumMap<>(CritterSpecies.Group.class);
        for (CritterSpecies.Group g : CritterSpecies.Group.values()) b.put(g, 0.0);
        // bad water, far too warm, four hunting bettas, no hiding places, no food
        return new CritterLife.Env(0.05F, true, 36F, 4, 0, 200, b, true, true, 0, 1, 0.5, Double.NaN, Double.NaN, Double.NaN, m);
    }

    private static List<Critter> colony() {
        List<Critter> out = new ArrayList<>();
        int id = 1;
        for (CritterSpecies s : List.of(CritterSpecies.NEOCARIDINA, CritterSpecies.RAMSHORN, CritterSpecies.ASSASSIN_SNAIL, CritterSpecies.DWARF_CRAYFISH)) {
            for (int i = 0; i < 4; i++) {
                Critter c = new Critter(id++, s, i % 2 == 0, CritterGenes.WILD, 1.5, 1.0 / 32, 1.0);
                c.age = i < 2 ? s.juvenileDays() + 1 : 0.1; // adults and young (prey)
                out.add(c);
            }
        }
        return out;
    }

    private static int alive(List<Critter> l) {
        return CritterLife.alive(l);
    }

    @Test
    void smallAnimals() {
        for (CareMode m : CareMode.values()) {
            List<Critter> list = colony();
            int before = list.size();
            Random rnd = new Random(7);
            int[] ids = {1000};
            double[] pool = {0};
            int died = 0, eaten = 0;
            for (int day = 0; day < 30; day++) {
                CritterLife.Result r = CritterLife.step(list, new ArrayList<>(), env(m), 1.0, pool, rnd, () -> ids[0]++);
                died += r.died.size();
                eaten += r.eaten.size();
            }
            if (m == CareMode.NORMAL) {
                assertNotEquals(before, alive(list), "normal: the colony dies");
            } else {
                assertEquals(0, died + eaten, () -> m + ": nobody dies or is eaten");
                assertEquals(before, alive(list), m::name);
            }
            if (m == CareMode.DECOR) for (Critter c : list) assertEquals(0.0, c.hunger, 1e-9, "decor: never hungry");
        }
    }
}
