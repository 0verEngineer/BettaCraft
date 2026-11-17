package org.overengineer.bettacraft.entity.betta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Health timers (PLAN 5.5) without Minecraft bootstrap. */
class BettaHealthTest {
    private static final BettaHealth.Params P = BettaHealth.Params.DEFAULT;
    private static final BettaHealth.Conditions GOOD = BettaHealth.Conditions.GOOD;
    private static final BettaHealth.Conditions COLD = new BettaHealth.Conditions(true, false);
    private static final double EPS = 0.05;

    /** Steps in small chunks (like the entity does every second) until the event happens; returns the elapsed days. */
    private static double daysUntil(BettaHealth h, BettaHealth.Conditions c, BettaHealth.Event wanted, double max, boolean feedDaily) {
        double t = 0, sinceFeed = 0;
        while (t < max) {
            if (feedDaily && sinceFeed >= 0.995) {
                h.feed();
                sinceFeed = 0;
            }
            BettaHealth.Event e = h.step(0.01, c, P);
            t += 0.01;
            sinceFeed += 0.01;
            if (e == wanted) return t;
        }
        return Double.NaN;
    }

    /** Runs {@code days} in 0.01 steps, feeding once per day. */
    private static void runFed(BettaHealth h, BettaHealth.Conditions c, double days) {
        double since = 0;
        for (int i = 0; i < Math.round(days * 100); i++) {
            if (since >= 0.995) {
                h.feed();
                since = 0;
            }
            h.step(0.01, c, P);
            since += 0.01;
        }
    }

    @Test
    void wellFedFishInGoodWaterStaysHealthy() {
        BettaHealth h = new BettaHealth();
        assertTrue(Double.isNaN(daysUntil(h, GOOD, BettaHealth.Event.GOT_SICK, 60, true)));
        assertFalse(h.isSick());
        assertTrue(h.healthFraction(P) > 0.9F, "only the normal daily hunger");
    }

    @Test
    void sevenDaysUnfedMakesSick() {
        BettaHealth h = new BettaHealth();
        double d = daysUntil(h, GOOD, BettaHealth.Event.GOT_SICK, 20, false);
        // the first day is "fed"; afterwards only food is bad -> normal speed, sick after 7 days without food
        assertEquals(7.0, d, EPS);
        assertTrue(h.isSick());
        assertEquals(0.5F, h.healthFraction(P), 0.01);
    }

    @Test
    void sevenDaysWrongTemperatureMakesSick() {
        BettaHealth h = new BettaHealth();
        assertEquals(7.0, daysUntil(h, COLD, BettaHealth.Event.GOT_SICK, 20, true), EPS);
    }

    @Test
    void bothBadRunsTwiceAsFast() {
        BettaHealth h = new BettaHealth();
        // day 0..1: fed (only temperature bad, x1) -> stress 1; then both bad (x2): food stress 1 + 2*(t-1) = 7 -> t = 4
        double sick = daysUntil(h, COLD, BettaHealth.Event.GOT_SICK, 20, false);
        assertEquals(4.0, sick, EPS);
        // 14 sick days at double speed = 7 days
        double death = daysUntil(h, COLD, BettaHealth.Event.DIED, 30, false);
        assertEquals(7.0, death, EPS);
    }

    @Test
    void fourteenSickDaysKill() {
        BettaHealth h = new BettaHealth();
        daysUntil(h, COLD, BettaHealth.Event.GOT_SICK, 20, true);
        assertEquals(14.0, daysUntil(h, COLD, BettaHealth.Event.DIED, 30, true), EPS);
    }

    @Test
    void recoveryRemovesTwoSickDaysPerGoodDay() {
        BettaHealth h = new BettaHealth();
        daysUntil(h, COLD, BettaHealth.Event.GOT_SICK, 20, true);
        h.feed();
        runFed(h, COLD, 6.0); // 6 sick days (fed, only temperature bad)
        assertEquals(6.0, h.sickDays(), 0.05);
        h.feed();
        double rec = daysUntil(h, GOOD, BettaHealth.Event.RECOVERED, 10, true);
        assertEquals(3.0, rec, EPS);
        assertFalse(h.isSick());
        assertEquals(0.0, h.tempStress(), 1e-9);
    }

    @Test
    void feedingResetsHunger() {
        BettaHealth h = new BettaHealth();
        h.step(5.0, GOOD, P);
        assertEquals(5.0, h.foodStress(), 1e-6);
        assertTrue(h.isHungry(P));
        h.feed();
        assertEquals(0.0, h.foodStress());
        assertFalse(h.isHungry(P));
        assertEquals(1.0F, h.satiation(P), 1e-6);
    }

    @Test
    void temperatureStressDecaysWhenGood() {
        BettaHealth h = new BettaHealth();
        runFed(h, COLD, 3.0);
        h.feed();
        h.step(1.0, GOOD, P);
        assertEquals(1.0, h.tempStress(), 0.05, "3 bad days minus 2 per good day");
    }

    @Test
    void largeCatchUpStepIsEquivalentToSmallSteps() {
        BettaHealth a = new BettaHealth(), b = new BettaHealth();
        a.step(10.0, COLD, P);
        for (int i = 0; i < 1000; i++) b.step(0.01, COLD, P);
        assertEquals(a.isSick(), b.isSick());
        assertEquals(a.sickDays(), b.sickDays(), 0.05);
    }

    @Test
    void nbtRoundTrip() {
        BettaHealth h = new BettaHealth();
        h.step(8.0, COLD, P);
        BettaHealth c = BettaHealth.load(h.save());
        assertEquals(h.isSick(), c.isSick());
        assertEquals(h.sickDays(), c.sickDays(), 1e-9);
        assertEquals(h.foodStress(), c.foodStress(), 1e-9);
        assertEquals(h.tempStress(), c.tempStress(), 1e-9);
    }

    /** M9: bad water is a fourth stress like the temperature; with hunger it doubles all counters. */
    @Test
    void badWaterMakesSickAndRecovers() {
        BettaHealth.Conditions dirty = new BettaHealth.Conditions(false, false, true);
        BettaHealth h = new BettaHealth();
        double t = daysUntil(h, dirty, BettaHealth.Event.GOT_SICK, 30, true);
        assertEquals(7.0, t, EPS, "sick after 7 days of bad water");
        assertTrue(h.waterStress() > 6.9);
        assertEquals(BettaHealth.Event.RECOVERED, h.step(0.5, GOOD, P));
        assertEquals(0.0, h.waterStress(), 1e-9);
        BettaHealth both = new BettaHealth();
        assertEquals(3.5, daysUntil(both, new BettaHealth.Conditions(true, false, true), BettaHealth.Event.GOT_SICK, 30, true), EPS,
                "temperature and water bad: twice as fast");
        BettaHealth saved = BettaHealth.load(dirtyFish().save());
        assertEquals(2.0, saved.waterStress(), 1e-9, "saved");
        assertEquals(0.0, BettaHealth.load(new net.minecraft.nbt.CompoundTag()).waterStress(), 1e-9, "older fish have none");
    }

    private static BettaHealth dirtyFish() {
        BettaHealth h = new BettaHealth();
        h.feed();
        h.step(1.0, new BettaHealth.Conditions(false, false, true), P);
        h.feed();
        h.step(1.0, new BettaHealth.Conditions(false, false, true), P);
        return h;
    }

    /** M17 review: NaN days froze a sick fish (it never died nor recovered); they load as 0 now. */
    @Test
    void corruptDaysLoadAsZero() {
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        t.putBoolean("sick", true);
        t.putDouble("sick_days", Double.NaN);
        t.putDouble("food_stress", -3);
        t.putDouble("days_since_fed", Double.POSITIVE_INFINITY);
        BettaHealth h = BettaHealth.load(t);
        assertTrue(h.isSick());
        double d = daysUntil(h, COLD, BettaHealth.Event.DIED, 1000, false);
        assertFalse(Double.isNaN(d), "a sick fish in the cold dies eventually");
    }
}
