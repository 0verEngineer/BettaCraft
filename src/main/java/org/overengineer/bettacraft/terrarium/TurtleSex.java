package org.overengineer.bettacraft.terrarium;

/**
 * M31 part b: temperature-dependent sex determination (TSD) of the turtles, as measured in real eggs. No sex chromosomes:
 * the temperature of the nest in the middle third of the incubation (the thermosensitive period) decides.
 * <ul>
 *     <li>Pattern MF (slider, Hermann's tortoise, Reeves' turtle): cool nests give males, warm ones females; at the
 *     pivotal temperature half and half. The range in which both sexes hatch (the transitional range) is about 2-3 K
 *     wide: the share of females follows a logistic curve with a width of {@link #WIDTH} K.</li>
 *     <li>Pattern FMF (the musk turtle, <i>Sternotherus</i>): females at both ends, males in the middle (about 22-26 °C).</li>
 * </ul>
 * The speed of the incubation follows the temperature too: from {@link #STALL} °C it develops, at the pivot in
 * {@link TurtleSpecies#eggDays} days, warmer faster (at most {@link #MAX_RATE} times); above {@link #LETHAL} °C the
 * embryos die. Pure functions (unit-tested).
 */
public final class TurtleSex {
    /** Width of the logistic curve (K): from 10 % to 90 % females takes about 4.4 widths. */
    public static final double WIDTH = 0.55;
    /** Below this the embryos do not develop (they wait). */
    public static final double STALL = 18.0;
    /** Above this the embryos die (after {@link #LETHAL_DAYS} days in total). */
    public static final double LETHAL = 36.0, LETHAL_DAYS = 0.25;
    public static final double MAX_RATE = 1.6;
    /** The thermosensitive period: the middle third of the incubation. */
    public static final double TSP_FROM = 1.0 / 3, TSP_TO = 2.0 / 3;

    private TurtleSex() {}

    /** Share of females (0..1) from eggs incubated at this temperature (°C) during the thermosensitive period. */
    public static double femaleChance(TurtleSpecies s, double temp) {
        if (!Double.isFinite(temp)) return 0.5;
        double warm = logistic((temp - s.pivot()) / WIDTH);
        if (!s.patternFmf()) return warm;
        double cool = logistic((s.lowPivot() - temp) / WIDTH);
        return Math.min(1.0, warm + cool);
    }

    private static double logistic(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    /** Incubation progress per day at this temperature (1 / eggDays at the pivot; 0 below {@link #STALL}). */
    public static double rate(TurtleSpecies s, double temp, double speed) {
        if (!Double.isFinite(temp) || temp <= STALL) return 0;
        double r = Math.min(MAX_RATE, (temp - STALL) / Math.max(1.0, s.pivot() - STALL));
        return r * Math.max(0.01, speed) / Math.max(0.05, s.eggDays());
    }

    /** The temperature that gives the most males (the middle of the male range for FMF, 3 K below the pivot for MF). */
    public static double maleTemp(TurtleSpecies s) {
        return s.patternFmf() ? (s.pivot() + s.lowPivot()) / 2 : s.pivot() - 3;
    }

    /** The temperature that gives the most females (3 K above the pivot). */
    public static double femaleTemp(TurtleSpecies s) {
        return s.pivot() + 3;
    }
}
