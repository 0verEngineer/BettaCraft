package org.overengineer.bettacraft.plumbing;

import net.minecraft.core.Direction;

/**
 * M68: one pipe of a pipe bundle ({@link PipeBundleBlock}) packed into an int: the sides it is joined on (bits 0-5, bit =
 * {@code 1 << direction.get3DDataValue()}), the sides closed with the pipe wrench (bits 6-11), present (bit 12), stainless
 * steel (bit 13, else glass) and the ring of its network (bits 14-16, {@link PipeRing}). Saved as is; never move bits.
 */
public final class PipeSlots {
    private PipeSlots() {}

    /** Slots of a bundle. */
    public static final int COUNT = 4;
    public static final int SIDES = 0x3F, CLOSED_SHIFT = 6, PRESENT = 1 << 12, STEEL = 1 << 13, RING_SHIFT = 14;

    public static int bit(Direction d) {
        return 1 << d.get3DDataValue();
    }

    public static boolean present(int slot) {
        return (slot & PRESENT) != 0;
    }

    public static boolean steel(int slot) {
        return (slot & STEEL) != 0;
    }

    public static int sides(int slot) {
        return slot & SIDES;
    }

    public static boolean joined(int slot, Direction d) {
        return (slot & bit(d)) != 0;
    }

    public static int closed(int slot) {
        return (slot >>> CLOSED_SHIFT) & SIDES;
    }

    public static boolean closed(int slot, Direction d) {
        return (closed(slot) & bit(d)) != 0;
    }

    public static PipeRing ring(int slot) {
        return PipeRing.byIndex((slot >>> RING_SHIFT) & 7);
    }

    public static int of(boolean steel, int sides, int closed, PipeRing ring) {
        return PRESENT | (steel ? STEEL : 0) | (sides & SIDES) | (closed & SIDES) << CLOSED_SHIFT | ring.ordinal() << RING_SHIFT;
    }

    public static int withSides(int slot, int sides) {
        return (slot & ~SIDES) | (sides & SIDES);
    }

    public static int withClosed(int slot, int closed) {
        return (slot & ~(SIDES << CLOSED_SHIFT)) | (closed & SIDES) << CLOSED_SHIFT;
    }

    public static int withRing(int slot, PipeRing ring) {
        return (slot & ~(7 << RING_SHIFT)) | ring.ordinal() << RING_SHIFT;
    }

    public static int count(int[] slots) {
        int n = 0;
        for (int s : slots) if (present(s)) n++;
        return n;
    }

    /**
     * Pure: the slot that meets a single pipe, a port or another device on side {@code d} (-1: none): the one joined there already,
     * else of the slots not closed there the one with the fewest joined sides (an open end that wants to go on; the first of
     * them). Depends on the bundle alone, so the bundle and its neighbour come to the same slot.
     */
    public static int facing(int[] slots, Direction d) {
        int best = -1, bestSides = Integer.MAX_VALUE;
        for (int i = 0; i < slots.length; i++) {
            if (!present(slots[i]) || closed(slots[i], d)) continue;
            if (joined(slots[i], d)) return i;
            int n = Integer.bitCount(sides(slots[i]));
            if (n < bestSides) {
                bestSides = n;
                best = i;
            }
        }
        return best;
    }

    /** Pure: the old bundle format (M40 part b: eight bits per filter line, PRESENT 0x80, T-piece 0x40, six side bits) as slots of steel pipes. */
    public static int[] fromLegacy(int lines) {
        int[] out = new int[COUNT];
        for (int i = 0; i < COUNT; i++) {
            int old = (lines >>> (i * 8)) & 0xFF;
            if ((old & 0x80) == 0) continue;
            int sides = old & SIDES;
            out[i] = of(true, sides, ~sides & SIDES, PipeRing.NONE);
        }
        return out;
    }
}
