package org.overengineer.bettacraft.tank;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * M32 part b: a rigid move of tank blocks (Create contraptions, Sable sub-levels, any mod that moves block entities by
 * their data): block {@code from} went to {@code to}, turned by the integer rotation {@code r} (row-major 3x3, entries
 * -1, 0, 1; a mirror is allowed). Block P goes to {@code to + r (P - from)}; a point in a block turns about the block's
 * centre with it, so every block, entity and small animal keeps its place in the tank.
 */
public record TankMoveMap(BlockPos from, BlockPos to, int[] r) {
    public static final int[] IDENTITY = {1, 0, 0, 0, 1, 0, 0, 0, 1};
    private static final List<int[]> ROTATIONS = buildRotations();

    public TankMoveMap {
        from = from.immutable();
        to = to.immutable();
        r = r.clone();
    }

    public static TankMoveMap translation(BlockPos from, BlockPos to) {
        return new TankMoveMap(from, to, IDENTITY);
    }

    /** The 24 proper rotations, the identity first, then the turns about the vertical axis, then the rest. */
    public static List<int[]> rotations() {
        return ROTATIONS;
    }

    private static List<int[]> buildRotations() {
        List<int[]> out = new ArrayList<>();
        out.add(IDENTITY);
        out.add(new int[]{0, 0, -1, 0, 1, 0, 1, 0, 0}); // 90 degrees clockwise seen from above (Rotation.CLOCKWISE_90)
        out.add(new int[]{-1, 0, 0, 0, 1, 0, 0, 0, -1});
        out.add(new int[]{0, 0, 1, 0, 1, 0, -1, 0, 0});
        int[][] perms = {{0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}};
        for (int[] p : perms) {
            for (int s = 0; s < 8; s++) {
                int[] m = new int[9];
                for (int row = 0; row < 3; row++) m[row * 3 + p[row]] = (s >> row & 1) == 0 ? 1 : -1;
                if (det(m) != 1) continue;
                boolean known = false;
                for (int[] o : out) known |= java.util.Arrays.equals(o, m);
                if (!known) out.add(m);
            }
        }
        return List.copyOf(out);
    }

    static int det(int[] m) {
        return m[0] * (m[4] * m[8] - m[5] * m[7]) - m[1] * (m[3] * m[8] - m[5] * m[6]) + m[2] * (m[3] * m[7] - m[4] * m[6]);
    }

    public boolean identityRotation() {
        return java.util.Arrays.equals(r, IDENTITY);
    }

    /** Up stays up and no mirror: a turn about the vertical axis (the tank still stands; floor, plants and faces keep working). */
    public boolean upright() {
        return r[4] == 1 && det(r) == 1;
    }

    /** Quarter turns about the vertical axis (clockwise seen from above), for an upright move. */
    public int quarterTurns() {
        Direction d = direction(Direction.SOUTH);
        return switch (d) {
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
    }

    public BlockPos block(BlockPos p) {
        int dx = p.getX() - from.getX(), dy = p.getY() - from.getY(), dz = p.getZ() - from.getZ();
        return new BlockPos(to.getX() + r[0] * dx + r[1] * dy + r[2] * dz,
                to.getY() + r[3] * dx + r[4] * dy + r[5] * dz,
                to.getZ() + r[6] * dx + r[7] * dy + r[8] * dz);
    }

    public long block(long p) {
        return block(BlockPos.of(p)).asLong();
    }

    /** A point (entity, small animal, plant) goes with its block, turned about the block's centre. */
    public Vec3 point(double x, double y, double z) {
        double dx = x - from.getX() - 0.5, dy = y - from.getY() - 0.5, dz = z - from.getZ() - 0.5;
        return new Vec3(to.getX() + 0.5 + r[0] * dx + r[1] * dy + r[2] * dz,
                to.getY() + 0.5 + r[3] * dx + r[4] * dy + r[5] * dz,
                to.getZ() + 0.5 + r[6] * dx + r[7] * dy + r[8] * dz);
    }

    public Vec3 point(Vec3 p) {
        return point(p.x, p.y, p.z);
    }

    /** A direction vector turned (no translation). */
    public Vec3 vector(double x, double y, double z) {
        return new Vec3(r[0] * x + r[1] * y + r[2] * z, r[3] * x + r[4] * y + r[5] * z, r[6] * x + r[7] * y + r[8] * z);
    }

    public Direction direction(Direction d) {
        Vec3 v = vector(d.getStepX(), d.getStepY(), d.getStepZ());
        return Direction.getNearest(v.x, v.y, v.z);
    }

    /** An entity's yaw (degrees, 0 = facing south) after the move; unchanged when its heading would point up or down. */
    public float yaw(float yaw) {
        double rad = Math.toRadians(yaw);
        Vec3 v = vector(-Math.sin(rad), 0, Math.cos(rad));
        if (Math.abs(v.x) < 1e-6 && Math.abs(v.z) < 1e-6) return yaw;
        return (float) Math.toDegrees(Math.atan2(-v.x, v.z));
    }

    /** The rotation a mover reported, as the row-major matrix of its effect on the three unit steps. */
    public static int[] fromColumns(BlockPos ex, BlockPos ey, BlockPos ez) {
        return new int[]{ex.getX(), ey.getX(), ez.getX(), ex.getY(), ey.getY(), ez.getY(), ex.getZ(), ey.getZ(), ez.getZ()};
    }

    /** A plausible rotation matrix (entries -1..1, one non-zero per row and column). */
    public static boolean valid(int[] m) {
        if (m == null || m.length != 9) return false;
        int d = det(m);
        if (d != 1 && d != -1) return false;
        for (int i = 0; i < 9; i++) if (m[i] < -1 || m[i] > 1) return false;
        return true;
    }
}
