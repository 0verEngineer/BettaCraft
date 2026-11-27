package org.overengineer.bettacraft.plumbing;

import java.util.Locale;

/**
 * M68: what a pipe network does, given by the ports it touches ({@link PipePort}): a network at a multiblock filter's inlet
 * is its intake line, at its outlet the return line. Part B: the four ports of the water change controller (fresh water in and
 * out, waste water in and out). A network without a port is a plain water line (no role); one that touches ports of two
 * different roles is a short circuit ({@link PipeNetwork#shortCircuit()}). The role picks the ring colour of the pipes.
 */
public enum PipeRole {
    /** From the tanks to a filter's inlet (water leaves the tanks: a suction point that needs an intake guard). */
    INTAKE(PipeRing.INTAKE, false),
    /** From a filter's outlet back into the tanks. */
    RETURN(PipeRing.RETURN, false),
    /** Part B: fresh water into the water change controller (from a reservoir, a pump, an osmosis unit, another mod's tank). */
    FRESH_IN(PipeRing.FRESH, true),
    /** Part B: fresh water out of the controller (into a tank or to a filter). */
    FRESH_OUT(PipeRing.FRESH, true),
    /** Part B: old water from a tank into the controller (a suction point like a filter intake). */
    WASTE_IN(PipeRing.WASTE, true),
    /** Part B: old water out of the controller (a drain, a reservoir, another mod's tank). */
    WASTE_OUT(PipeRing.WASTE, true);

    private final PipeRing ring;
    private final boolean carriesWater;

    PipeRole(PipeRing ring, boolean carriesWater) {
        this.ring = ring;
        this.carriesWater = carriesWater;
    }

    public PipeRing ring() {
        return ring;
    }

    /**
     * Whether the network still works as a water pipe for others (pumps, other mods' pipes push and pull through it). The filter
     * lines do not: the filter's pump head works through them alone.
     */
    public boolean carriesWater() {
        return carriesWater;
    }

    /** The intake or the return line of a multiblock filter. */
    public boolean filterLine() {
        return this == INTAKE || this == RETURN;
    }

    /** Water leaves the tanks through such a line (its docks need an intake guard inside). */
    public boolean suction() {
        return this == INTAKE || this == WASTE_IN;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
