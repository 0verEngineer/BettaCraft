package org.overengineer.bettacraft.plumbing;

import net.minecraft.util.StringRepresentable;
import org.overengineer.bettacraft.filter.FilterPartBlock;

/**
 * M40: the lines a filter pipe carries. The intake line leads the water of the tanks to the filter's inlet connector, the
 * return line from its outlet connector back into the tanks. Each line only joins pipes of the same line, its connector
 * and the tanks; lines never mix.
 * <p>
 * Part b: the fresh water line (from a water pump, a reservoir, an ME/RS export or the pipes of other mods to the
 * filter's fresh water port) and the waste water line (from its waste water port away). They do not dock on tanks: they
 * join the fluid handlers of other blocks instead ({@link #water()}). Up to all four lines run in one block, the pipe
 * bundle ({@link FilterPipeBundleBlock}); the order of the constants is saved there (never reorder).
 */
public enum PipeLine implements StringRepresentable {
    INTAKE("intake", "filter_inlet_pipe", FilterPartBlock.Part.INLET, true),
    RETURN("return", "filter_outlet_pipe", FilterPartBlock.Part.OUTLET, false),
    FRESH("fresh", "filter_fresh_pipe", FilterPartBlock.Part.FRESH, false),
    WASTE("waste", "filter_waste_pipe", FilterPartBlock.Part.WASTE, false);

    private final String name;
    private final String block;
    private final FilterPartBlock.Part connector;
    private final boolean intake;

    PipeLine(String name, String block, FilterPartBlock.Part connector, boolean intake) {
        this.name = name;
        this.block = block;
        this.connector = connector;
        this.intake = intake;
    }

    /** The filter connector this line joins. */
    public FilterPartBlock.Part connector() {
        return connector;
    }

    /** Water leaves the tanks through this line (it needs an intake guard there; otherwise an outlet guard). */
    public boolean intake() {
        return intake;
    }

    /** The intake and the return line dock on tanks. */
    public boolean toTanks() {
        return this == INTAKE || this == RETURN;
    }

    /** The fresh and the waste water line join the fluid handlers of other blocks (pumps, reservoirs, other mods' pipes). */
    public boolean water() {
        return this == FRESH || this == WASTE;
    }

    /** The id of the single pipe block of this line. */
    public String blockId() {
        return block;
    }

    /** The line that joins this filter connector, or null. */
    public static PipeLine forConnector(FilterPartBlock.Part p) {
        for (PipeLine l : values()) if (l.connector == p) return l;
        return null;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
