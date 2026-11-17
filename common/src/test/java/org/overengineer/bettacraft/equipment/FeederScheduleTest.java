package org.overengineer.bettacraft.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Feeding times of the feeder (1-3 per day). */
class FeederScheduleTest {
    @Test
    void oncePerDayInTheMorning() {
        long before = FeedSchedule.eventKey(999, 1);
        long at = FeedSchedule.eventKey(1000, 1);
        assertTrue(at > before, "a new event starts at day time 1000");
        assertEquals(at, FeedSchedule.eventKey(23999, 1), "no second event that day");
        assertTrue(FeedSchedule.eventKey(25000, 1) > at, "next morning");
    }

    @Test
    void threeTimesPerDay() {
        long a = FeedSchedule.eventKey(1000, 3);
        long b = FeedSchedule.eventKey(7000, 3);
        long c = FeedSchedule.eventKey(13000, 3);
        assertTrue(a < b && b < c);
        assertEquals(c, FeedSchedule.eventKey(20000, 3));
        assertEquals(c, FeedSchedule.eventKey(24999, 3), "until the next morning");
    }

    @Test
    void negativeAndLargeTimes() {
        assertTrue(FeedSchedule.eventKey(24000L * 1000 + 1000, 2) > FeedSchedule.eventKey(24000L * 999 + 11000, 2));
        assertTrue(FeedSchedule.eventKey(0, 2) < FeedSchedule.eventKey(1000, 2));
    }
}
