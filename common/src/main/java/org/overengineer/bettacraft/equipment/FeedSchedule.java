package org.overengineer.bettacraft.equipment;

/**
 * When a feeder feeds: day time 1000 (morning), plus 11000 for two and 7000/13000 for three feedings. Apart from the block
 * entity so that the JUnit tests reach it without Minecraft's block entity classes.
 */
public final class FeedSchedule {
    private static final long[][] SCHEDULE = {{1000}, {1000, 11000}, {1000, 7000, 13000}};

    private FeedSchedule() {}

    /** Key of the latest scheduled feeding at or before {@code dayTime}. */
    public static long eventKey(long dayTime, int feedings) {
        long[] times = SCHEDULE[Math.max(1, Math.min(3, feedings)) - 1];
        long day = Math.floorDiv(dayTime, 24000L);
        long tod = Math.floorMod(dayTime, 24000L);
        for (int i = times.length - 1; i >= 0; i--) {
            if (tod >= times[i]) return day * 4 + i;
        }
        return (day - 1) * 4 + (times.length - 1);
    }
}
