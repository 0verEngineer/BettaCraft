package org.overengineer.bettacraft.plumbing.interior;

/**
 * M24: the kinds of plumbing pieces in a tank interior. Their code in the equipment index is {@code (ordinal + 1) * 8 + facing}
 * (2D data value of the horizontal facing; 0 for pieces without one) in the low byte. M68 part C: the bits above it carry
 * {@link #extra}: the line of a pipe dock ({@link #ANCHOR}: 0 = the dock of a single pipe, 1..4 = slot 0..3 of a bundle) and
 * the lane count of an interior bundle ({@link #BUNDLE}). Codes are saved with the region (append kinds only, never reorder).
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
    SPRAY_BAR(1, 0.6),
    /**
     * M40: the connection point of a filter pipe that docks on the tank from outside (any side, the top or the bottom):
     * it sits where the pipe docks, on that wall, the top or the floor of the tank block. Placed and removed by the game;
     * its facing is the 3D data value of the side it docks on (pointing out of the tank).
     */
    ANCHOR(0, 0),
    /** M40: intake guard: the mesh over a filter intake that keeps the animals out (water leaves the tank here). */
    INTAKE_GUARD(0, 0.15),
    /** M40: outlet guard: the mesh cap on a filter outlet (water comes in here, a soft current). */
    OUTLET_GUARD(1, 0.4),
    /**
     * M68 part C: an interior pipe bundle: 2 to 4 separate lanes in one block along an axis (facing = axis ordinal, extra = lane
     * count). A lane continues into the next bundle of the same axis and joins one single piece on its own side
     * ({@link InteriorBundleBlock#laneSide}).
     */
    BUNDLE(0, 0);

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
        return this == STRAINER || this == INTAKE_GUARD;
    }

    /** M40: a connection to the outside (the bulkhead's connection on the floor or the dock of a filter pipe). */
    public boolean port() {
        return this == PORT || this == ANCHOR;
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

    /** M68 part C: a code with {@link #extra} bits (the line of a dock, the lanes of a bundle). */
    public static int code(Plumb p, int facing, int extra) {
        return code(p, facing) | (extra & 0xFF) << 8;
    }

    public static Plumb kind(int code) {
        return values()[(code & 0xFF) / 8 - 1];
    }

    /** M68 part C: the bits above the kind and facing (0 in codes of earlier versions). */
    public static int extra(int code) {
        return (code >>> 8) & 0xFF;
    }

    /** M68 part C: interior pipes and bundles: pieces a line runs through. */
    public boolean conduit() {
        return this == PIPE || this == BUNDLE;
    }

    public static int facing(int code) {
        return code & 7;
    }
}
