package org.overengineer.bettacraft.plumbing;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * M68: the colour ring a pipe shows at its block borders (M65 rings), set by the role of its network ({@link PipeRole#ring()}):
 * none for a plain water line, blue for a filter intake line, orange for a return line, green for fresh water, brown for waste
 * water and red for a short circuit (a network that touches ports of two different roles). In the block state of a single pipe
 * ({@link PipeBlock#RING}) and in each slot of a bundle; the order is saved in bundles (never reorder, only append).
 */
public enum PipeRing implements StringRepresentable {
    NONE, INTAKE, RETURN, FRESH, WASTE, SHORT;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static PipeRing byIndex(int i) {
        PipeRing[] v = values();
        return i >= 0 && i < v.length ? v[i] : NONE;
    }
}
