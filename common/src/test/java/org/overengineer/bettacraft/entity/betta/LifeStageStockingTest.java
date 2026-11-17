package org.overengineer.bettacraft.entity.betta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** M18: default shares of a fish per life stage for the stocking (much less than the old proposal 1/4, 1/2, 3/4). */
class LifeStageStockingTest {
    @Test
    void defaults() {
        assertEquals(0.0, LifeStage.EGG.stocking());
        assertEquals(0.0, LifeStage.LARVA.stocking());
        assertEquals(0.02, LifeStage.FRY.stocking(), 1e-9);
        assertEquals(0.1, LifeStage.JUVENILE.stocking(), 1e-9);
        assertEquals(0.3, LifeStage.SUBADULT.stocking(), 1e-9);
        assertEquals(1.0, LifeStage.ADULT.stocking());
    }

    @Test
    void growsWithTheStage() {
        LifeStage[] s = LifeStage.values();
        for (int i = 1; i < s.length; i++) assertTrue(s[i].stocking() >= s[i - 1].stocking(), s[i] + " counts at least as much as " + s[i - 1]);
    }
}
