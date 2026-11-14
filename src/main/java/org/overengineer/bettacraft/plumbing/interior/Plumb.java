package org.overengineer.bettacraft.plumbing.interior;

/**
 * M24: the kinds of plumbing pieces in a tank interior. Their code in the equipment index is {@code ordinal * 8 + facing}
 * (2D data value of the horizontal facing; 0 for pieces without one).
 */
public enum Plumb {
    PORT(0, 0), PIPE(0, 0),
    /** Intake strainer: water leaves the tank here (a gentle pull towards it). */
    STRAINER(0, 0.25),
    /** Lily pipe: a glass outlet that spreads the water softly over the surface. */
    LILY(1, 0.5),
    /** Nozzle: a narrow, strong jet. */
    NOZZLE(1, 1.0),
    /** Spray bar: a wide, soft current along a wall. */
    SPRAY_BAR(1, 0.6);

    private final int out;
    private final double strength;

    Plumb(int out, double strength) {
        this.out = out;
        this.strength = strength;
    }

    /** Water enters the tank through it. */
    public boolean outflow() {
        return out == 1;
    }

    public boolean intake() {
        return this == STRAINER;
    }

    public boolean outlet() {
        return outflow() || intake();
    }

    /** Strength of its current (a filter outlet under water is 0.6). */
    public double strength() {
        return strength;
    }

    public static int code(Plumb p, int facing2d) {
        return (p.ordinal() + 1) * 8 + (facing2d & 7);
    }

    public static Plumb kind(int code) {
        return values()[code / 8 - 1];
    }

    public static int facing(int code) {
        return code & 7;
    }
}
