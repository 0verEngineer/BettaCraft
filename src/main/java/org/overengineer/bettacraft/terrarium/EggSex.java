package org.overengineer.bettacraft.terrarium;

/**
 * M31 part b: temperature-dependent sex determination (TSD) of the turtles (part c: and the leopard gecko), as measured in
 * real eggs; the species with genetic sex ({@link HerpSpecies#tsd} false) hatch half and half. No sex chromosomes:
 * the temperature of the nest in the middle third of the incubation (the thermosensitive period) decides.
 * <ul>
 *     <li>Pattern MF (slider, Hermann's tortoise, Reeves' turtle): cool nests give males, warm ones females; at the
 *     pivotal temperature half and half. The range in which both sexes hatch (the transitional range) is about 2-3 K
 *     wide: the share of females follows a logistic curve with a width of {@link #WIDTH} K.</li>
 *     <li>Pattern FMF (the musk turtle, <i>Sternotherus</i>): females at both ends, males in the middle (about 22-26 °C);
 *     part c: the leopard gecko the same at higher temperatures (males around 32-33 °C).</li>
 * </ul>
 * The speed of the incubation follows the temperature too: from {@link #STALL} °C it develops, at the pivot in
 * {@link HerpSpecies#eggDays} days, warmer faster (at most {@link #MAX_RATE} times); above {@link #LETHAL} °C the
 * embryos die. Pure functions (unit-tested).
 */
public final class EggSex {
    /** Width of the logistic curve (K): from 10 % to 90 % females takes about 4.4 widths. */
    public static final double WIDTH = 0.55;
    /** Below this the embryos do not develop (they wait). */
    public static final double STALL = 18.0;
    /** Above this the embryos die (after {@link #LETHAL_DAYS} days in total). */
    public static final double LETHAL = 36.0, LETHAL_DAYS = 0.25;
    public static final double MAX_RATE = 1.6;
    /** The thermosensitive period: the middle third of the incubation. */
    public static final double TSP_FROM = 1.0 / 3, TSP_TO = 2.0 / 3;

    private EggSex() {}

    /** Share of females (0..1) from eggs incubated at this temperature (°C) during the thermosensitive period. */
    public static double femaleChance(HerpSpecies s, double temp) {
        if (!s.tsd() || !Double.isFinite(temp)) return 0.5; // part c: genetic sex (frogs, the crested gecko, the snake)
        double warm = logistic((temp - s.pivot()) / WIDTH);
        if (!s.patternFmf()) return warm;
        double cool = logistic((s.lowPivot() - temp) / WIDTH);
        return Math.min(1.0, warm + cool);
    }

    private static double logistic(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    /** Incubation progress per day at this temperature (1 / eggDays at the pivot; 0 below {@link #STALL}). */
    public static double rate(HerpSpecies s, double temp, double speed) {
        return rate(s.pivot(), s.eggDays(), temp, speed);
    }

    /** M35: the same for eggs that hatch in {@code eggDays} at the {@code pivot} temperature (the egg layers of other mods). */
    public static double rate(double pivot, double eggDays, double temp, double speed) {
        if (!Double.isFinite(temp) || temp <= STALL) return 0;
        double r = Math.min(MAX_RATE, (temp - STALL) / Math.max(1.0, pivot - STALL));
        return r * Math.max(0.01, speed) / Math.max(0.05, eggDays);
    }

    /** The temperature that gives the most males (the middle of the male range for FMF, 3 K below the pivot for MF). */
    public static double maleTemp(HerpSpecies s) {
        return s.patternFmf() ? (s.pivot() + s.lowPivot()) / 2 : s.pivot() - 3;
    }

    /** The temperature that gives the most females (3 K above the pivot). */
    public static double femaleTemp(HerpSpecies s) {
        return s.pivot() + 3;
    }
}
