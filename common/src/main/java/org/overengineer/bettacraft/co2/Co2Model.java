package org.overengineer.bettacraft.co2;

import org.overengineer.bettacraft.config.ServerConfig;

/**
 * The pure model of the CO2 devices (bio CO2 bottle, CO2 station), apart from their block entities so that the JUnit tests
 * reach it without loading Minecraft's block entity classes (M52b: NeoForge 21.11 needs FML for those).
 */
public final class Co2Model {
    private Co2Model() {}

    /**
     * Pure: how strong the fermentation is at {@code p} (0..1 of its time): it starts within the first sixth, holds and
     * fades over the last 40 %; 0 before and after.
     */
    public static double curve(double p) {
        if (p <= 0 || p >= 1) return 0;
        if (p < 0.15) return p / 0.15;
        if (p > 0.6) return (1 - p) / 0.4;
        return 1;
    }

    /** Pure: the uneven yeast: 0.7..1.3, changes every few hours (deterministic from the position and the time). */
    public static double unevenness(long seed, long gameTime) {
        long h = seed * 0x9E3779B97F4A7C15L + Math.floorDiv(gameTime, 4000L) * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return 0.7 + 0.6 * ((h >>> 11) / (double) (1L << 53));
    }

    /** Pure: days a brew with this much sugar ferments. */
    public static double daysFor(int sugar) {
        return ServerConfig.getOr(ServerConfig.CO2_BIO_DAYS, 4.0) * Math.max(1, Math.min(BioCo2BlockEntity.MAX_SUGAR, sugar)) / BioCo2BlockEntity.MAX_SUGAR;
    }

    /** Pure: bubble-days a station adds in {@code ticks} with {@code fires} burning fires. */
    public static double fill(int fires, long ticks, double capacity, int ticksPerCylinder) {
        return fires <= 0 ? 0 : capacity * fires * ticks / Math.max(1.0, ticksPerCylinder);
    }
}
