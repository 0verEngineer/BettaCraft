package org.overengineer.bettacraft.plumbing;

import net.minecraft.util.StringRepresentable;
import org.overengineer.bettacraft.filter.FilterPartBlock;

/**
 * M40: the lines a filter pipe carries. The intake line leads the water of the tanks to the filter's inlet connector, the
 * return line from its outlet connector back into the tanks. Each line only joins pipes of the same line, its connector
 * and the tanks; lines never mix. (Part b adds the fresh water and the waste water line and the blocks that carry
 * several lines at once.)
 */
public enum PipeLine implements StringRepresentable {
    INTAKE("intake", FilterPartBlock.Part.INLET, true),
    RETURN("return", FilterPartBlock.Part.OUTLET, false);

    private final String name;
    private final FilterPartBlock.Part connector;
    private final boolean intake;

    PipeLine(String name, FilterPartBlock.Part connector, boolean intake) {
        this.name = name;
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
