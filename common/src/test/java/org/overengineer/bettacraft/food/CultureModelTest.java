package org.overengineer.bettacraft.food;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** M26: the live food cultures (tubifex dish, daphnia barrel) and the schedule of the live food pump. */
class CultureModelTest {
    @Test
    void daphniaFillTheirRoomInTheConfiguredDays() {
        CultureModel m = new CultureModel();
        m.population = 0.1;
        m.food = 1;
        double t = 0;
        while (m.population < 0.9 && t < 10) {
            m.food = 1; // fed all the time
            m.stepDaphnia(0.05, true, true, false, 3.0, 1.0);
            t += 0.05;
        }
        assertEquals(3.0, t, 0.2, "from 10 % to 90 % in about daphniaDays");
    }

    @Test
    void daphniaNeedLightToGrow() {
        CultureModel m = new CultureModel();
        m.population = 0.2;
        m.food = 1;
        m.stepDaphnia(1, true, false, false, 3.0, 1.0);
        assertTrue(m.population <= 0.2 + 1e-9, "no growth in the dark");
    }

    @Test
    void crowdedDaphniaCollapse() {
        CultureModel m = new CultureModel();
        m.population = 0.95;
        m.food = 1;
        CultureModel.Event e = m.stepDaphnia(0.5, true, true, false, 3.0, 1.0);
        assertEquals(CultureModel.Event.NONE, e, "half a day crowded is fine");
        m.food = 1;
        e = m.stepDaphnia(0.6, true, true, false, 3.0, 1.0);
        assertEquals(CultureModel.Event.COLLAPSE_CROWDED, e);
        assertTrue(m.population < 0.2, "collapsed: " + m.population);
    }

    @Test
    void harvestingInTimePreventsTheCollapse() {
        CultureModel m = new CultureModel();
        m.population = 0.95;
        m.food = 1;
        m.stepDaphnia(0.5, true, true, false, 3.0, 1.0);
        assertTrue(m.take(16, 16) >= 13, "16 room: everything above the seed");
        assertEquals(0, m.crowded, 1e-9);
        assertTrue(m.population >= CultureModel.SEED - 1e-9);
        assertEquals(0, m.harvestable(16));
    }

    @Test
    void heatKillsDaphnia() {
        CultureModel m = new CultureModel();
        m.population = 0.6;
        m.food = 1;
        assertEquals(CultureModel.Event.COLLAPSE_HEAT, m.stepDaphnia(0.3, true, true, true, 3.0, 1.0));
        assertTrue(m.population < 0.1, "collapsed: " + m.population);
    }

    @Test
    void starvingCulturesShrinkAndDie() {
        CultureModel m = new CultureModel();
        m.population = 0.3;
        CultureModel.Event e = CultureModel.Event.NONE;
        for (int i = 0; i < 100 && e != CultureModel.Event.DIED_OUT; i++) e = m.stepDaphnia(1, true, true, false, 3.0, 1.0);
        assertEquals(CultureModel.Event.DIED_OUT, e);
        assertEquals(0, m.population, 1e-9);
    }

    @Test
    void tubifexNeedMudAndFood() {
        CultureModel m = new CultureModel();
        m.population = 0.125;
        m.food = 0.5;
        m.stepTubifex(1, false, true, 4.0);
        assertTrue(m.population < 0.125, "no mud: they die off");
        CultureModel g = new CultureModel();
        g.population = 0.125;
        g.food = 0.5;
        g.stepTubifex(1, true, true, 4.0);
        assertTrue(g.population > 0.2, "with mud and mulm they grow: " + g.population);
        assertTrue(g.food < 0.5, "and eat");
    }

    @Test
    void standingWaterGoesStaleAndFreshWaterRinsesIt() {
        CultureModel m = new CultureModel();
        m.population = 0.5;
        m.food = 0.3;
        m.stepTubifex(5, true, true, 4.0);
        assertTrue(m.foul > 0.2 && !m.poor(), "slowly stale: " + m.foul);
        CultureModel over = new CultureModel();
        over.population = 0.5;
        over.food = 1;
        over.stepTubifex(3, true, true, 4.0);
        assertTrue(over.poor(), "overfed: fast stale: " + over.foul);
        over.rinse(1000);
        over.rinse(1000);
        assertTrue(over.foul < CultureModel.POOR, "two buckets rinse it");
    }

    @Test
    void mulmFromWashedMedia() {
        assertEquals(0, org.overengineer.bettacraft.filter.FilterMedium.mulmFrom(0.1));
        assertEquals(1, org.overengineer.bettacraft.filter.FilterMedium.mulmFrom(0.5));
        assertEquals(2, org.overengineer.bettacraft.filter.FilterMedium.mulmFrom(0.9));
    }

    @Test
    void pumpScheduleSpreadsTheDoses() {
        assertEquals(LiveFoodSchedule.eventKey(999, 1) + 1, LiveFoodSchedule.eventKey(1000, 1), "7:00");
        assertEquals(LiveFoodSchedule.eventKey(1000, 1), LiveFoodSchedule.eventKey(24999, 1), "once a day");
        long a = LiveFoodSchedule.eventKey(1000, 2), b = LiveFoodSchedule.eventKey(13000, 2);
        assertEquals(a + 1, b, "12 hours later");
        assertEquals(b, LiveFoodSchedule.eventKey(24999, 2));
        assertEquals(b + 1, LiveFoodSchedule.eventKey(25000, 2), "next morning");
        assertTrue(LiveFoodSchedule.eventKey(24000L * 500 + 4000, 8) > LiveFoodSchedule.eventKey(24000L * 500 + 1000, 8));
    }
}
