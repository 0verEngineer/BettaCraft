package org.overengineer.bettacraft.osmosis;

/** The pure model of the reverse osmosis unit, apart from its block entity (JUnit tests without Minecraft's block entities). */
public final class RoModel {
    private RoModel() {}

    /** Pure: mB of feed water for {@code ro} mB of reverse osmosis water with {@code wastePerPart} parts of waste. */
    public static double feedFor(double ro, double wastePerPart) {
        return ro * (1 + Math.max(0, wastePerPart));
    }

    /** Pure: mB of reverse osmosis water from this much feed. */
    public static double roFrom(double feed, double wastePerPart) {
        return feed / (1 + Math.max(0, wastePerPart));
    }

    /** Pure: wear points (of 100) of a part that lasts {@code buckets} buckets after {@code mb} mB. */
    public static double wear(double mb, double buckets) {
        return mb * 100.0 / (Math.max(0.001, buckets) * 1000.0);
    }
}
