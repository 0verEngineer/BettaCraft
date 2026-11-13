package org.overengineer.bettacraft.tank;

/**
 * M20: salt in the water of a tank, pure functions (unit-tested). Salinity is kept in grams of salt per litre (ppt,
 * per mille): fresh water 0, natural sea water 35. Calcium in mg/L follows the salt (sea water about 420 mg/L) and is
 * used up by the stony corals.
 * <ul>
 *     <li>Water that is poured in brings its own salinity: the tank mixes by volume ({@link #mix}).</li>
 *     <li>Evaporation takes pure water: salt and calcium stay behind and get stronger ({@link #concentrate}); topping
 *     up with fresh water brings it back.</li>
 *     <li>Draining keeps the salinity.</li>
 * </ul>
 * Animals have a tolerated range ({@link Range}). Out of it they get a salt shock whose speed grows with the distance
 * to the range ({@link #shockPerDay}): a freshwater fish in full sea water dies within a few hours, a few ppt too much
 * only stress it over days.
 */
public final class Salinity {
    /** Natural sea water (ppt). */
    public static final float SEA = 35.0F;
    /** Calcium of natural sea water (mg/L). */
    public static final float SEA_CALCIUM = 420.0F;
    /** Highest salinity a tank can reach (by evaporating or too much salt). */
    public static final float MAX = 60.0F;
    /** Fresh water below this, brackish up to {@link #MARINE_FROM}, marine from there. */
    public static final float BRACKISH_FROM = 2.0F;
    public static final float MARINE_FROM = 20.0F;
    /** ppt outside of the range per unit of shock per day (8 ppt too much = one "fatal" shock per day at speed 1). */
    public static final double SHOCK_PPT = 8.0;
    /** Shock that is not deadly yet but stresses the animal (counts as bad water). */
    public static final double STRESS_FROM = 0.05;

    public enum Kind { FRESH, BRACKISH, MARINE }

    /** Range of salinity an animal or plant lives in (ppt, inclusive). */
    public record Range(float min, float max) {
        /** Freshwater animals: a pinch of salt does no harm. */
        public static final Range FRESH = new Range(0, 3);
        /** Brackish-tolerant freshwater animals (nerites, guppies, platys ...). */
        public static final Range BRACKISH = new Range(0, 18);
        /** Nerite snails come from estuaries: from fresh water up to almost sea water. */
        public static final Range NERITE = new Range(0, 30);
        /** Reef animals: sea water with a little play. */
        public static final Range MARINE = new Range(28, 40);
        /** Hardy marine animals (hermit crabs, damselfish, salmon). */
        public static final Range HARDY_MARINE = new Range(20, 42);
        /** Anything (euryhaline or unknown). */
        public static final Range ANY = new Range(0, MAX);

        public boolean contains(double ppt) {
            return ppt >= min - 1e-4 && ppt <= max + 1e-4;
        }

        /** ppt outside of the range (0 inside). */
        public double distance(double ppt) {
            return ppt < min ? min - ppt : ppt > max ? ppt - max : 0;
        }

        public boolean marine() {
            return min >= MARINE_FROM - 1e-4;
        }
    }

    private Salinity() {}

    public static Kind kind(double ppt) {
        return ppt >= MARINE_FROM ? Kind.MARINE : ppt >= BRACKISH_FROM ? Kind.BRACKISH : Kind.FRESH;
    }

    public static boolean marine(double ppt) {
        return kind(ppt) == Kind.MARINE;
    }

    /** {@code addedMb} of water with {@code addedPpt} poured into {@code waterMb} of water with {@code ppt}. */
    public static float mix(float ppt, long waterMb, float addedPpt, long addedMb) {
        if (addedMb <= 0) return ppt;
        if (waterMb <= 0) return clamp(addedPpt);
        return clamp((float) ((ppt * (double) waterMb + addedPpt * (double) addedMb) / (waterMb + addedMb)));
    }

    /** Calcium (mg/L) after mixing {@code addedMb} with {@code addedCa} into {@code waterMb} with {@code ca}. */
    public static float mixCalcium(float ca, long waterMb, float addedCa, long addedMb) {
        if (addedMb <= 0) return ca;
        if (waterMb <= 0) return clampCalcium(addedCa);
        return clampCalcium((float) ((ca * (double) waterMb + addedCa * (double) addedMb) / (waterMb + addedMb)));
    }

    /** {@code lostMb} of pure water evaporated from {@code waterMb}: what is dissolved stays behind. */
    public static float concentrate(float value, long waterMb, long lostMb, float max) {
        if (lostMb <= 0 || waterMb <= 0 || value <= 0) return value;
        long left = waterMb - lostMb;
        if (left <= 0) return value;
        return Math.min(max, (float) (value * (double) waterMb / left));
    }

    /** Salinity after dissolving {@code items} portions of sea salt (one salts 1000 mB to sea water) in {@code waterMb}. */
    public static float addSalt(float ppt, long waterMb, int items) {
        if (items <= 0 || waterMb <= 0) return ppt;
        return clamp((float) (ppt + items * SEA * (double) TankData.MB_PER_BLOCK / waterMb));
    }

    /** Calcium after dissolving {@code items} doses of calcium additive (one raises 1000 mB by 100 mg/L). */
    public static float addCalcium(float ca, long waterMb, int items, float perBlock) {
        if (items <= 0 || waterMb <= 0) return ca;
        return Math.min(2000.0F, (float) (ca + items * perBlock * (double) TankData.MB_PER_BLOCK / waterMb));
    }

    /** Calcium that comes with salt water of this salinity (sea water 420 mg/L). */
    public static float calciumOf(float ppt) {
        return Math.max(0, ppt) / SEA * SEA_CALCIUM;
    }

    /** Salt shock per day (1 = deadly within a day) for an animal with {@code range} in water of {@code ppt}. */
    public static double shockPerDay(Range range, double ppt, double speed) {
        double d = range.distance(ppt);
        if (d <= 0 || speed <= 0) return 0;
        return d / SHOCK_PPT * speed;
    }

    /** Whether inserting an animal is refused: the water would kill it quickly (more than one shock per day). */
    public static boolean deadly(Range range, double ppt) {
        return range.distance(ppt) > SHOCK_PPT;
    }

    /** Nitrate (mg/L) as shown in the menu: follows the waste in the water (clean water 0-5, fouled water 100). */
    public static int nitrate(float quality) {
        return Math.round(Math.max(0, 1 - quality) * 100);
    }

    /**
     * Colour of the water (RGB) for this salinity: the vanilla water colour of the biome blended towards a clear sea
     * blue the saltier it is.
     */
    public static int waterColor(int fresh, double ppt) {
        if (ppt < BRACKISH_FROM) return fresh;
        double t = Math.min(1.0, (ppt - BRACKISH_FROM) / (SEA - BRACKISH_FROM)) * 0.85;
        return lerp(fresh, SEA_COLOR, t);
    }

    /** Clear blue of a reef tank. */
    public static final int SEA_COLOR = 0x1E9AE0;

    static int lerp(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    static float clamp(float ppt) {
        return Float.isFinite(ppt) ? Math.max(0, Math.min(MAX, ppt)) : 0;
    }

    /** Clamp for loaded data. */
    public static float clampSalinity(float ppt) {
        return clamp(ppt);
    }

    public static float clampCalcium(float ca) {
        return Float.isFinite(ca) ? Math.max(0, Math.min(2000, ca)) : 0;
    }
}
