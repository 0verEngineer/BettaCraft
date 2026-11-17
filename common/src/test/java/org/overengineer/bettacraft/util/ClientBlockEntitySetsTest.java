package org.overengineer.bettacraft.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** M17 review H1: entries of an unloaded client level must leave the static block entity sets. */
class ClientBlockEntitySetsTest {
    record Be(Object level) {}

    @Test
    void unloadingALevelRemovesOnlyItsEntries() {
        Object a = new Object(), b = new Object();
        List<Be> set = new ArrayList<>(List.of(new Be(a), new Be(a), new Be(b)));
        assertEquals(2, ClientBlockEntitySets.purge(set, Be::level, a));
        assertEquals(List.of(new Be(b)), set);
    }

    @Test
    void logoutClearsEverything() {
        List<Be> set = new ArrayList<>(List.of(new Be(new Object()), new Be(new Object())));
        assertEquals(2, ClientBlockEntitySets.purge(set, Be::level, null));
        assertTrue(set.isEmpty());
    }
}
