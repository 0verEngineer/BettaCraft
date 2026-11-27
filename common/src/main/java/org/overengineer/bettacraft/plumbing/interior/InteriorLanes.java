package org.overengineer.bettacraft.plumbing.interior;

import net.minecraft.core.Direction;

/**
 * M68 part C: the rule and the geometry of an interior bundle ({@link InteriorBundleBlock}), pure (no registry: the index search and
 * the unit tests use it). Lane 0 runs in the middle along the run's axis and goes on into single pieces at both ends; side lanes 1 to
 * 3 each leave on their own side ({@link #laneSide}); along the axis every lane goes on into the same lane of the next bundle.
 */
public final class InteriorLanes {
    private InteriorLanes() {}

    /** Model and shape geometry (pixels): half the width of the middle lane and of a side lane, the side lanes' distance from the middle, the rod end pieces. */
    public static final float MIDDLE = 2F, LANE = 1.5F, OFFSET = 4.5F, END = 3F;
    /** Pieces of a lane: its rod in the block, the rod's end towards the negative / positive axis direction, its arm to its side. */
    public static final int ROD = 0, END_NEG = 1, END_POS = 2, ARM = 3;

    /** Pure: the side lane {@code lane} (1..3) of a run along {@code axis} leaves on. */
    public static Direction laneSide(Direction.Axis axis, int lane) {
        return switch (axis) {
            case X -> new Direction[]{Direction.EAST, Direction.UP, Direction.DOWN, Direction.SOUTH}[lane];
            case Y -> new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST}[lane];
            case Z -> new Direction[]{Direction.SOUTH, Direction.UP, Direction.DOWN, Direction.EAST}[lane];
        };
    }

    /** Pure: the lane of a bundle ({@code axis}, {@code lanes}) a single piece on its side {@code d} joins: 0 along the axis, else the side lane there, or -1. */
    public static int laneOn(Direction.Axis axis, int lanes, Direction d) {
        if (d.getAxis() == axis) return 0;
        for (int i = 1; i < lanes; i++) if (laneSide(axis, i) == d) return i;
        return -1;
    }

    /** Pure: the same for an index code ({@link Plumb#BUNDLE}). */
    public static int laneOn(int code, Direction d) {
        return laneOn(axisOf(code), Plumb.extra(code), d);
    }

    public static Direction.Axis axisOf(int code) {
        return Direction.Axis.values()[Math.min(2, Plumb.facing(code))];
    }

    /**
     * The box (pixels: minX, minY, minZ, maxX, maxY, maxZ) of piece {@code piece} ({@link #ROD}, {@link #END_NEG}, {@link #END_POS},
     * {@link #ARM}) of lane {@code lane} of a run along {@code axis}.
     */
    public static float[] box(Direction.Axis axis, int lane, int piece) {
        float[] min = new float[3], max = new float[3];
        int a = axis.ordinal();
        Direction side = lane == 0 ? null : laneSide(axis, lane);
        for (int i = 0; i < 3; i++) {
            if (i == a) continue;
            float c = 8, h = lane == 0 ? MIDDLE : LANE;
            if (side != null && side.getAxis().ordinal() == i) c = 8 + OFFSET * (side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1);
            min[i] = c - h;
            max[i] = c + h;
        }
        float h = lane == 0 ? MIDDLE : LANE;
        switch (piece) {
            case END_NEG -> {
                min[a] = 0;
                max[a] = END;
            }
            case END_POS -> {
                min[a] = 16 - END;
                max[a] = 16;
            }
            case ARM -> {
                // in the middle of the block, from the rod's outer face to its side
                min[a] = 8 - h;
                max[a] = 8 + h;
                int si = side.getAxis().ordinal();
                if (side.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                    min[si] = max[si];
                    max[si] = 16;
                } else {
                    max[si] = min[si];
                    min[si] = 0;
                }
            }
            default -> {
                min[a] = END;
                max[a] = 16 - END;
            }
        }
        return new float[]{min[0], min[1], min[2], max[0], max[1], max[2]};
    }

    /** Pure: the run axis an interior pipe with joined sides {@code sides} (bit = 3D data value) gets as a bundle: its own if straight or an end, else {@code clicked}. */
    public static Direction.Axis axisFor(int sides, Direction.Axis clicked) {
        if (Integer.bitCount(sides) == 1) return Direction.from3DDataValue(Integer.numberOfTrailingZeros(sides)).getAxis();
        for (Direction.Axis a : Direction.Axis.values()) {
            int both = 1 << Direction.fromAxisAndDirection(a, Direction.AxisDirection.NEGATIVE).get3DDataValue()
                    | 1 << Direction.fromAxisAndDirection(a, Direction.AxisDirection.POSITIVE).get3DDataValue();
            if (sides == both) return a;
        }
        return clicked;
    }
}
