package org.overengineer.bettacraft.critters;

/**
 * M54: which way the model of a small animal is turned. The critter models face -z and are drawn with z flipped (scale -1), so
 * their head points to +z before they are turned; a heading of yaw (Minecraft's: 0 = south, +z; 90 = west, -x) is then the turn
 * {@link #modelTurn} about y. The renderers of the tank ({@code CritterRenderer#orient}) and of the world
 * ({@code WildCritterRenderer}) both use it (the world one used the vanilla 180 - yaw of the mob models and drew the animals
 * crawling backwards). Pure, unit-tested.
 */
public final class CritterFacing {
    private CritterFacing() {}

    /** The turn about y (degrees, positive = counter-clockwise seen from above as in {@code Axis.YP}) for a heading of {@code yaw}. */
    public static float modelTurn(float yaw) {
        return -yaw;
    }

    /** Where the head of a model turned by {@code turn} degrees points (x, z): +z rotated about y. */
    public static double[] head(float turn) {
        double a = Math.toRadians(turn);
        // Axis.YP rotation of (0, 0, 1): x' = sin a, z' = cos a
        return new double[]{Math.sin(a), Math.cos(a)};
    }

    /** The direction (x, z) of a heading of Minecraft's yaw. */
    public static double[] heading(float yaw) {
        double a = Math.toRadians(yaw);
        return new double[]{-Math.sin(a), Math.cos(a)};
    }
}
