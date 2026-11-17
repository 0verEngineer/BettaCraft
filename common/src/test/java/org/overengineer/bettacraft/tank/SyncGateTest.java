package org.overengineer.bettacraft.tank;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** M17 review (network): periodic critter / plant syncs only go out when their bytes changed, or after a while anyway. */
class SyncGateTest {
    @Test
    void sameBytesAreNotSentAgainUntilTheSilenceRunsOut() {
        SyncGate g = new SyncGate();
        assertTrue(g.changed(new byte[]{1, 2, 3}, 100), "first time");
        assertFalse(g.changed(new byte[]{1, 2, 3}, 120), "unchanged");
        assertTrue(g.changed(new byte[]{1, 2, 4}, 140), "changed");
        assertFalse(g.changed(new byte[]{1, 2, 4}, 140 + SyncGate.MAX_SILENCE - 1), "still quiet");
        assertTrue(g.changed(new byte[]{1, 2, 4}, 140 + SyncGate.MAX_SILENCE), "resent after the silence");
        g.reset();
        assertTrue(g.changed(new byte[]{1, 2, 4}, 141 + SyncGate.MAX_SILENCE), "after reset");
    }
}
