package org.overengineer.bettacraft.tank;

/**
 * Water quality model (M9), pure functions (unit-tested). The quality {@code q} is 1 for clean water and 0 for
 * fouled water; {@code 1 - q} is the concentration of waste in the water.
 * <ul>
 *     <li>Per day the water loses {@code base + animalsPerBlock * perAnimal}; every portion of food a fish gets while not
 *     hungry adds {@code perOverfed / waterBlocks} at once (rotting food).</li>
 *     <li>Filters remove {@code efficiency * coverage} of that waste (coverage = filter capacity / water blocks, at most
 *     1), so even filtered water needs a water change now and then.</li>
 *     <li>Fresh water dilutes the waste ({@link #mix}); evaporation removes pure water and concentrates it
 *     ({@link #concentrate}). Draining water does not change the quality.</li>
 * </ul>
 */
public final class WaterQuality {
    /** Below this the menu shows "poor" (a warning), below {@link Params#badBelow} bettas are stressed. */
    public static final float POOR_BELOW = 0.6F;
    public static final float GOOD_FROM = 0.85F;

    public record Params(double baseDeclinePerDay, double perAnimalPerDay, double perOverfedPortion, double filterEfficiency,
                         double badBelow) {
        public static final Params DEFAULT = new Params(0.01, 0.04, 0.01, 0.8, 0.35);
    }

    public enum State { CLEAN, GOOD, POOR, BAD }

    private WaterQuality() {}

    /** Waste added per day as quality loss (before the filter). */
    public static double wastePerDay(double animals, double waterBlocks, Params p) {
        double perBlock = waterBlocks <= 0 ? animals : animals / waterBlocks;
        return p.baseDeclinePerDay() + perBlock * p.perAnimalPerDay();
    }

    /** Share of the waste the filters remove (0..efficiency). */
    public static double filterShare(double filterBlocks, double waterBlocks, Params p) {
        return p.filterEfficiency() * TankTemperature.coverage(filterBlocks, waterBlocks);
    }

    /** Quality after {@code days} with the given animals and filter capacity. */
    public static float step(float quality, double days, double animals, double waterBlocks, double filterBlocks, Params p) {
        double loss = wastePerDay(animals, waterBlocks, p) * (1 - filterShare(filterBlocks, waterBlocks, p)) * Math.max(0, days);
        return clamp((float) (quality - loss));
    }

    /** Quality loss of {@code portions} portions of uneaten / unneeded food. */
    public static float overfed(float quality, int portions, double waterBlocks, double filterBlocks, Params p) {
        if (portions <= 0) return quality;
        double loss = portions * p.perOverfedPortion() / Math.max(1.0, waterBlocks) * (1 - filterShare(filterBlocks, waterBlocks, p));
        return clamp((float) (quality - loss));
    }

    /** {@code addedMb} of clean water poured into {@code waterMb} of water of the given quality. */
    public static float mix(float quality, long waterMb, long addedMb) {
        if (addedMb <= 0) return quality;
        if (waterMb <= 0) return 1.0F;
        double waste = (1 - quality) * waterMb;
        return clamp((float) (1 - waste / (waterMb + addedMb)));
    }

    /** {@code lostMb} of pure water evaporated from {@code waterMb}: the waste stays behind. */
    public static float concentrate(float quality, long waterMb, long lostMb) {
        if (lostMb <= 0 || waterMb <= 0) return quality;
        long left = waterMb - lostMb;
        if (left <= 0) return quality;
        double waste = (1 - quality) * waterMb;
        return clamp((float) (1 - waste / left));
    }

    public static State state(float quality, Params p) {
        if (quality < p.badBelow()) return State.BAD;
        if (quality < POOR_BELOW) return State.POOR;
        if (quality < GOOD_FROM) return State.GOOD;
        return State.CLEAN;
    }

    public static boolean isBad(float quality, Params p) {
        return quality < p.badBelow();
    }

    static float clamp(float q) {
        return Float.isNaN(q) ? 1.0F : Math.max(0.0F, Math.min(1.0F, q));
    }

    /**
     * Evaporation (M9): mB lost per day from {@code openBlocks} uncovered top blocks at water temperature {@code celsius}
     * ({@code mbPerBlockPerDay} at 25 C, twice as much at 45 C, a quarter at 10 C and colder).
     */
    public static double evaporationPerDay(int openBlocks, float celsius, double mbPerBlockPerDay) {
        if (openBlocks <= 0 || mbPerBlockPerDay <= 0) return 0;
        double factor = Math.max(0.25, Math.min(3.0, 1.0 + (celsius - 25.0) / 20.0));
        return openBlocks * mbPerBlockPerDay * factor;
    }
}
