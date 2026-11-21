package org.overengineer.bettacraft.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** M32 part d: one waypoint per tank, opened by the first alarm, closed by the last, kept by everyone who got it. */
class AlarmWaypointBookTest {
    private final UUID tank = UUID.randomUUID(), other = UUID.randomUUID();
    private final UUID owner = UUID.randomUUID(), near = UUID.randomUUID(), late = UUID.randomUUID();

    @Test
    void firstAlarmOpensLastClears() {
        AlarmWaypointBook b = new AlarmWaypointBook();
        assertEquals(Set.of(owner, near), b.open(tank, "m1#1", Set.of(owner), "wp", () -> Set.of(owner, near)));
        assertTrue(b.isOpen(tank));
        // a second tile (or monitor) of the same tank: no second waypoint
        assertEquals(Set.of(), b.open(tank, "m2#4", Set.of(owner), "wp", () -> Set.of(late)));
        // the same tile again (no change) does nothing either
        assertEquals(Set.of(), b.open(tank, "m1#1", Set.of(owner), "wp", () -> Set.of(late)));
        assertEquals(Set.of(), b.close(tank, "m1#1"), "another alarm of the tank is still on");
        assertTrue(b.isOpen(tank));
        assertEquals(Set.of(owner, near), b.close(tank, "m2#4"), "the last alarm clears: everyone who got it");
        assertFalse(b.isOpen(tank));
        assertEquals(Set.of(), b.close(tank, "m2#4"), "closing twice does nothing");
    }

    @Test
    void ownerGetsItOnLoginOnce() {
        AlarmWaypointBook b = new AlarmWaypointBook();
        b.open(tank, "k", Set.of(owner), "wp", () -> Set.of(near)); // the owner was offline
        b.open(other, "k", Set.of(near), "wp2", () -> Set.of(near));
        assertEquals(Set.of(tank), b.joined(owner));
        assertEquals(Set.of(), b.joined(owner), "only once");
        assertEquals(Set.of(), b.joined(late), "no member");
        assertEquals("wp", b.entry(tank).data);
        assertEquals(Set.of(near, owner), b.close(tank, "k"));
        assertEquals(1, b.size());
        b.clear();
        assertEquals(0, b.size());
    }

    @Test
    void tanksAreSeparate() {
        AlarmWaypointBook b = new AlarmWaypointBook();
        assertEquals(Set.of(near), b.open(tank, "k", Set.of(), null, () -> Set.of(near)));
        assertEquals(Set.of(late), b.open(other, "k", Set.of(), null, () -> Set.of(late)));
        assertEquals(Set.of(near), b.close(tank, "k"));
        assertTrue(b.isOpen(other));
    }

    /** M53: the waypoint switches change while the waypoint is open: who gets it now, who loses it. */
    @Test
    void updateFollowsTheSwitches() {
        AlarmWaypointBook b = new AlarmWaypointBook();
        b.open(tank, "k", Set.of(owner), "wp", () -> Set.of(owner));
        AlarmWaypointBook.Change c = b.update(tank, Set.of(owner, near, late), Set.of(owner, near));
        assertEquals(Set.of(near), c.added(), "an online user switched his on");
        assertEquals(Set.of(), c.removed());
        assertEquals(Set.of(tank), b.joined(late), "the offline one gets it when he comes online");
        c = b.update(tank, Set.of(near), Set.of(owner, near, late));
        assertEquals(Set.of(owner, late), c.removed(), "switched off: they lose it");
        assertEquals(Set.of(), c.added());
        assertEquals(Set.of(), b.joined(owner), "no longer a member");
        assertEquals(Set.of(near), b.close(tank, "k"));
        assertEquals(AlarmWaypointBook.Change.NONE, b.update(tank, Set.of(owner), Set.of(owner)), "no open waypoint: nothing");
    }
}
