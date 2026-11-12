package org.overengineer.bettacraft.client.tank;

import net.minecraft.core.Direction;

/**
 * Corner positions of an axis-aligned box face in vanilla winding order (counter-clockwise seen from outside,
 * identical to vanilla {@code FaceInfo}), plus matching face-local UVs (0..1).
 */
public final class BoxFaces {
    private BoxFaces() {}

    /** Writes the 4 corners (x,y,z) of the given face into {@code out[12]}. */
    public static void corners(Direction face, float x0, float y0, float z0, float x1, float y1, float z1, float[] out) {
        float[] c = switch (face) {
            case DOWN -> new float[]{x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1};
            case UP -> new float[]{x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0};
            case NORTH -> new float[]{x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0};
            case SOUTH -> new float[]{x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1};
            case WEST -> new float[]{x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1};
            case EAST -> new float[]{x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0};
        };
        System.arraycopy(c, 0, out, 0, 12);
    }

    /** Face-local UV (0..1) of a corner position, mapped like vanilla block faces (u right, v down). */
    public static float u(Direction face, float x, float y, float z) {
        return switch (face) {
            case DOWN, UP -> x;
            case NORTH -> 1 - x;
            case SOUTH -> x;
            case WEST -> z;
            case EAST -> 1 - z;
        };
    }

    public static float v(Direction face, float x, float y, float z) {
        return switch (face) {
            case DOWN -> 1 - z;
            case UP -> z;
            default -> 1 - y;
        };
    }
}
