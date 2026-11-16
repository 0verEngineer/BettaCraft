package org.overengineer.bettacraft.equipment;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * M44: the front one cabinet block shows (block state {@code front}). Left and right are seen by someone standing in front
 * of the cabinet.
 * <ul>
 *     <li>{@link #TWO}: two narrow doors with their handles in the middle (the front of every cabinet before M44);</li>
 *     <li>{@link #PAIR_L} / {@link #PAIR_R}: the left / right leaf of a double door over two blocks (the handles at the joint
 *     between the two blocks);</li>
 *     <li>{@link #SINGLE}: one wide door with its handle at the right edge;</li>
 *     <li>{@link #DRAWERS}: three drawers.</li>
 * </ul>
 * {@link #layout} is the automatic front of a connected row (pattern E of the M44 analysis): a drawer block at both ends
 * (from three blocks on), double doors between them, an odd rest gets a single door in the middle.
 */
public enum CabinetFront implements StringRepresentable {
    TWO, PAIR_L, PAIR_R, SINGLE, DRAWERS;

    /** Rows up to this length get the layout; longer ones get double doors by their world position. */
    public static final int MAX_ROW = 32;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The front seen in a mirror (a double door's leaves swap). */
    public CabinetFront mirrored() {
        return this == PAIR_L ? PAIR_R : this == PAIR_R ? PAIR_L : this;
    }

    /**
     * The automatic front of block {@code i} (0 = the leftmost, seen from the front) of a connected row of {@code n} blocks
     * ({@code 1 <= n <= MAX_ROW}).
     * <ul>
     *     <li>1 block: {@link #TWO} as before; 2 blocks: one double door;</li>
     *     <li>3 and more: drawers at both ends; between them double doors. An even rest is filled with double doors. An odd
     *     rest puts a single door in the middle; when the double doors on each side of it would not come out even (5, 9,
     *     13 ... blocks) the single door gets a drawer block on each side ({@code D LR D S D LR D}), so the row stays
     *     symmetric.</li>
     * </ul>
     */
    public static CabinetFront layout(int n, int i) {
        if (n < 1 || i < 0 || i >= n) throw new IllegalArgumentException("block " + i + " of " + n);
        if (n == 1) return TWO;
        if (n == 2) return i == 0 ? PAIR_L : PAIR_R;
        if (i == 0 || i == n - 1) return DRAWERS;
        int inner = n - 2;
        if (inner % 2 == 0) return (i - 1) % 2 == 0 ? PAIR_L : PAIR_R;
        int centre = n / 2;
        if (i == centre) return SINGLE;
        int half = (inner - 1) / 2; // blocks between an end drawer and the centre
        boolean flank = half % 2 == 1;
        if (flank && (i == centre - 1 || i == centre + 1)) return DRAWERS;
        // double doors from the end drawer inwards: on the left from block 1, on the right from the centre side
        int from = i < centre ? 1 : centre + 1 + (flank ? 1 : 0);
        return (i - from) % 2 == 0 ? PAIR_L : PAIR_R;
    }

    /** The front of a block in a row longer than {@link #MAX_ROW}: double doors by the position along the row. */
    public static CabinetFront longRow(int along) {
        return Math.floorMod(along, 2) == 0 ? PAIR_L : PAIR_R;
    }
}
