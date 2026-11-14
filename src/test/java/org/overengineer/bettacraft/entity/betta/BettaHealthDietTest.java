package org.overengineer.bettacraft.entity.betta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** M26 part b: the food memory of a fish, the varied-diet bonus and food that suits it badly (no Minecraft bootstrap). */
class BettaHealthDietTest {
    private static final BettaHealth.Params P = BettaHealth.Params.DEFAULT;
    private static final BettaHealth.Conditions COLD = new BettaHealth.Conditions(true, false);

    @Test
    void remembersFoodGroupsForTheWindow() {
        BettaHealth h = new BettaHealth();
        assertEquals(0, h.variety(3));
        h.ate(0);
        h.ate(1);
        h.ate(1);
        assertEquals(2, h.variety(3));
        h.step(2.0, BettaHealth.Conditions.GOOD, P);
        h.ate(4);
        assertEquals(3, h.variety(3));
        assertTrue(h.varied(P.diet()));
        h.step(1.5, BettaHealth.Conditions.GOOD, P); // groups 0 and 1 are 3.5 days old now
        assertEquals(1, h.variety(3));
        assertFalse(h.varied(P.diet()));
        h.ate(-1);
        h.ate(BettaHealth.FOOD_GROUPS); // ignored
        assertEquals(1, h.variety(3));
    }

    @Test
    void variedDietRecoversFaster() {
        BettaHealth plain = new BettaHealth(), varied = new BettaHealth();
        for (BettaHealth h : new BettaHealth[]{plain, varied}) {
            h.step(3.0, COLD, P); // 3 days of temperature stress
            h.feed();
        }
        for (int g = 0; g < 3; g++) varied.ate(g);
        plain.step(1.0, BettaHealth.Conditions.GOOD, P);
        varied.step(1.0, BettaHealth.Conditions.GOOD, P);
        assertTrue(plain.tempStress() > 1.5, "still stressed: " + plain.tempStress());
        assertEquals(plain.tempStress() - 1.0, varied.tempStress(), 0.05, "3 instead of 2 per day with the bonus");
        // without the bonus (feature off) the same
        BettaHealth.Params off = new BettaHealth.Params(7, 14, 2, 2, 1, BettaHealth.Diet.OFF);
        BettaHealth h = new BettaHealth();
        h.step(3.0, COLD, off);
        for (int g = 0; g < 6; g++) h.ate(g);
        h.step(1.0, BettaHealth.Conditions.GOOD, off);
        assertEquals(plain.tempStress(), h.tempStress(), 0.05);
    }

    @Test
    void poorFoodMakesHungrySooner() {
        BettaHealth h = new BettaHealth();
        h.step(0.9, BettaHealth.Conditions.GOOD, P);
        h.feedPoorly(0.5);
        assertEquals(0.5, h.daysSinceFed(), 1e-9);
        h.step(0.6, BettaHealth.Conditions.GOOD, P);
        assertTrue(h.isHungry(P), "hungry after 0.6 days instead of 1");
    }

    @Test
    void dietSurvivesSaveAndOldFishHaveNone() {
        BettaHealth h = new BettaHealth();
        h.ate(2);
        h.step(0.5, BettaHealth.Conditions.GOOD, P);
        h.ate(5);
        BettaHealth back = BettaHealth.load(h.save());
        assertEquals(2, back.variety(3));
        assertEquals(1, back.variety(0.25));
        BettaHealth old = BettaHealth.load(new BettaHealth().save());
        assertEquals(0, old.variety(30));
        assertFalse(new BettaHealth().save().contains("diet"), "nothing saved without a diet");
    }
}
