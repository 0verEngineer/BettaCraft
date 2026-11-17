package org.overengineer.bettacraft.tank;

/**
 * Water temperature model (PLAN 6.1), pure functions (unit-tested).
 * <ul>
 *     <li>Ambient temperature from the vanilla biome temperature: snow 0.0 = 5 C, taiga 0.25 = 12 C, swamp/plains 0.8 =
 *     25 C, desert 2.0 = 32 C (piecewise linear), plus +-2 C between noon and midnight.</li>
 *     <li>Heaters can only heat, coolers (M9) can only cool. With enough capacity the water goes to the target
 *     temperature, with too little capacity only the matching fraction of the way.</li>
 *     <li>The water approaches its equilibrium exponentially with time constant {@code tauDays}.</li>
 * </ul>
 */
public final class TankTemperature {
    public static final float MIN_TARGET = 20.0F;
    public static final float MAX_TARGET = 32.0F;
    public static final float DEFAULT_TARGET = 27.0F;
    public static final float TOLERANCE_MIN = 24.0F;
    public static final float TOLERANCE_MAX = 30.0F;
    public static final float OPTIMAL_MIN = 26.0F;
    public static final float OPTIMAL_MAX = 28.0F;

    private static final float[] BIOME = {0.0F, 0.25F, 0.8F, 2.0F};
    private static final float[] CELSIUS = {5.0F, 12.0F, 25.0F, 32.0F};

    private TankTemperature() {}

    public static float ambientFromBiome(float biomeTemperature) {
        if (biomeTemperature <= BIOME[0]) {
            float slope = (CELSIUS[1] - CELSIUS[0]) / (BIOME[1] - BIOME[0]);
            return Math.max(-5.0F, CELSIUS[0] + (biomeTemperature - BIOME[0]) * slope);
        }
        for (int i = 1; i < BIOME.length; i++) {
            if (biomeTemperature <= BIOME[i]) {
                float f = (biomeTemperature - BIOME[i - 1]) / (BIOME[i] - BIOME[i - 1]);
                return CELSIUS[i - 1] + f * (CELSIUS[i] - CELSIUS[i - 1]);
            }
        }
        return CELSIUS[CELSIUS.length - 1];
    }

    /** +2 C at noon (day time 6000), -2 C at midnight (18000). */
    public static float dayNightOffset(long dayTime) {
        double phase = ((dayTime % 24000L) - 6000L) / 24000.0 * 2 * Math.PI;
        return (float) (2.0 * Math.cos(phase));
    }

    /** Temperature the water settles at. {@code heaterBlocks} = summed heater capacity in water blocks. */
    public static float equilibrium(float ambient, float target, double heaterBlocks, double waterBlocks) {
        return equilibrium(ambient, target, heaterBlocks, 0, waterBlocks);
    }

    /**
     * Temperature the water settles at with heaters and coolers (capacities in water blocks): heaters pull it up
     * towards a target above the ambient temperature, coolers down towards a target below it.
     */
    public static float equilibrium(float ambient, float target, double heaterBlocks, double coolerBlocks, double waterBlocks) {
        double capacity = target > ambient ? heaterBlocks : target < ambient ? coolerBlocks : 0;
        if (capacity <= 0) return ambient;
        return (float) (ambient + (target - ambient) * coverage(capacity, waterBlocks));
    }

    /** Share (0..1) of the water that equipment with this capacity (water blocks) can handle. */
    public static double coverage(double capacityBlocks, double waterBlocks) {
        if (capacityBlocks <= 0) return 0;
        return waterBlocks <= 0 ? 1.0 : Math.min(1.0, capacityBlocks / waterBlocks);
    }

    /** Whether heaters (true) or coolers (false) work at this ambient temperature and target; null if neither. */
    public static Boolean heating(float ambient, float target) {
        return target > ambient ? Boolean.TRUE : target < ambient ? Boolean.FALSE : null;
    }

    public static float approach(float current, float equilibrium, double dtDays, double tauDays) {
        if (tauDays <= 0) return equilibrium;
        return (float) (equilibrium + (current - equilibrium) * Math.exp(-dtDays / tauDays));
    }

    public static boolean isTolerable(float celsius) {
        // M17: temperature.toleratedMin / toleratedMax (the constants are the defaults)
        double min = org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.TEMPERATURE_TOLERATED_MIN, (double) TOLERANCE_MIN);
        double max = org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.TEMPERATURE_TOLERATED_MAX, (double) TOLERANCE_MAX);
        return celsius >= min && celsius <= max;
    }

    /** M17: the best range is the breeding range (breeding.minTemp / maxTemp; the menu colour and the advancement used fixed 26-28 C). */
    public static boolean isOptimal(float celsius) {
        double min = org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.BREEDING_MIN_TEMP, (double) OPTIMAL_MIN);
        double max = org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.BREEDING_MAX_TEMP, (double) OPTIMAL_MAX);
        return celsius >= min && celsius <= max;
    }

    public static float clampTarget(float t) {
        return Math.max(MIN_TARGET, Math.min(MAX_TARGET, t));
    }
}
