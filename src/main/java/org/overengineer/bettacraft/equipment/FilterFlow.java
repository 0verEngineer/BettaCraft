package org.overengineer.bettacraft.equipment;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankShape;

/**
 * Geometry of the hose and outlet of an external filter (PLAN 6.2), shared by the client renderer and the bubble
 * particles: up along the outside of the glass wall, over the rim, down into the tank, and a jet that falls into the
 * water (or a current under the surface when the outlet is submerged). Purely visual.
 */
public final class FilterFlow {
    /** Gap between the hose and the glass. */
    private static final double GAP = 0.09;
    /** How far the outlet reaches into the tank from the wall. */
    private static final double REACH = 0.26;
    private static final double GRAVITY = 5.0, SPEED = 1.1;

    /**
     * @param bottom   where the hose leaves the filter (or the cabinet)
     * @param top      top of the vertical hose, above the rim
     * @param inside   above the water, past the rim
     * @param nozzle   outlet, pointing {@code inward}
     * @param surfaceY water surface of the tank
     * @param wet      the tank has water at all (no jet when dry)
     */
    public record Path(Vec3 bottom, Vec3 top, Vec3 inside, Vec3 nozzle, Direction inward, double surfaceY, boolean wet) {
        public boolean submerged() {
            return nozzle.y < surfaceY - 0.02;
        }

        /**
         * Centre line of the jet: a parabola from the nozzle down to the water surface, or a short, slowly sinking
         * current when the nozzle is under water. {@code n + 1} points.
         */
        public List<Vec3> jet(int n) {
            return jet(n, 1.0);
        }

        /** As {@link #jet(int)}, for a smaller outlet ({@code scale} &lt; 1, e.g. the internal filter in a miniature). */
        public List<Vec3> jet(int n, double scale) {
            List<Vec3> out = new ArrayList<>(n + 1);
            Vec3 dir = new Vec3(inward.getStepX(), 0, inward.getStepZ());
            if (submerged()) {
                for (int i = 0; i <= n; i++) {
                    double f = i / (double) n;
                    out.add(nozzle.add(dir.scale(0.9 * scale * f)).add(0, -0.08 * scale * f * f, 0));
                }
                return out;
            }
            // parabola down to the surface; even a nozzle just above the water throws the jet a little forward
            double drop = Math.max(0.02, nozzle.y - surfaceY);
            double g = GRAVITY / Math.max(0.2, scale);
            double reach = Math.max(SPEED * scale * Math.sqrt(2 * drop / g), 0.45 * scale);
            for (int i = 0; i <= n; i++) {
                double f = i / (double) n;
                out.add(nozzle.add(dir.scale(reach * f)).add(0, -drop * f * f, 0));
            }
            return out;
        }

        /** Where the jet meets the water (splash / bubbles). */
        public Vec3 impact() {
            List<Vec3> j = jet(8);
            return j.getLast();
        }
    }

    private FilterFlow() {}

    /** Largest jet scale (see {@link Path#jet(int, double)}) that keeps a submerged current inside the glass. */
    public static double fitScale(TankData t, Path p, double wanted) {
        double wall = 1.0 / 16, n;
        BlockPos at = BlockPos.containing(p.nozzle());
        if (t.contains(at)) {
            // M10: up to the glass opposite the outlet in this row of the shape (a notch wall ends the current earlier)
            BlockPos far = TankShape.walk(t.members(), at, p.inward());
            switch (p.inward()) {
                case NORTH -> n = p.nozzle().z - (far.getZ() + wall);
                case SOUTH -> n = far.getZ() + 1 - wall - p.nozzle().z;
                case WEST -> n = p.nozzle().x - (far.getX() + wall);
                default -> n = far.getX() + 1 - wall - p.nozzle().x;
            }
        } else {
            BlockPos mn = t.min(), mx = t.max();
            switch (p.inward()) {
                case NORTH -> n = p.nozzle().z - (mn.getZ() + wall);
                case SOUTH -> n = mx.getZ() + 1 - wall - p.nozzle().z;
                case WEST -> n = p.nozzle().x - (mn.getX() + wall);
                default -> n = mx.getX() + 1 - wall - p.nozzle().x;
            }
        }
        return Math.max(0.05, Math.min(wanted, n / 0.9));
    }

    /**
     * Hose path from a filter outside the wall {@code out} (direction pointing away from the tank) of the tank block
     * {@code wallBlock} (M10: the wall the filter hangs on, also the wall of a notch; the hose goes over the rim of
     * that column).
     *
     * @param lateral position along the wall (x for north/south walls, z for east/west walls), clamped into the wall
     * @param startY  height where the hose starts
     */
    @Nullable
    public static Path compute(TankData t, BlockPos wallBlock, Direction out, double lateral, double startY) {
        if (!out.getAxis().isHorizontal() || t.sizeX() <= 0) return null;
        boolean shape = t.contains(wallBlock);
        BlockPos topBlock = shape ? TankShape.walk(t.members(), wallBlock, Direction.UP) : new BlockPos(wallBlock.getX(), t.max().getY(), wallBlock.getZ());
        BlockPos lowBlock = shape ? TankShape.walk(t.members(), wallBlock, Direction.DOWN) : new BlockPos(wallBlock.getX(), t.min().getY(), wallBlock.getZ());
        double x0 = wallBlock.getX(), x1 = wallBlock.getX() + 1, z0 = wallBlock.getZ(), z1 = wallBlock.getZ() + 1;
        if (!shape) {
            BlockPos mn = t.min(), mx = t.max();
            x0 = mn.getX(); x1 = mx.getX() + 1; z0 = mn.getZ(); z1 = mx.getZ() + 1;
        }
        double yTop = topBlock.getY() + 1, yBottom = lowBlock.getY();
        boolean alongX = out.getAxis() == Direction.Axis.Z; // north/south wall runs along x
        double lo = alongX ? x0 : z0, hi = alongX ? x1 : z1;
        double lat = Math.max(lo + 0.22, Math.min(hi - 0.22, lateral));
        double wallOut, wallIn;
        switch (out) {
            case NORTH -> { wallOut = z0 - GAP; wallIn = z0 + REACH; }
            case SOUTH -> { wallOut = z1 + GAP; wallIn = z1 - REACH; }
            case WEST -> { wallOut = x0 - GAP; wallIn = x0 + REACH; }
            default -> { wallOut = x1 + GAP; wallIn = x1 - REACH; }
        }
        double rim = yTop + 0.1;
        double sy = Math.max(yBottom - 0.5, Math.min(rim - 0.2, startY));
        Vec3 bottom = alongX ? new Vec3(lat, sy, wallOut) : new Vec3(wallOut, sy, lat);
        Vec3 top = alongX ? new Vec3(lat, rim, wallOut) : new Vec3(wallOut, rim, lat);
        Vec3 inside = alongX ? new Vec3(lat, rim, wallIn) : new Vec3(wallIn, rim, lat);
        Vec3 nozzle = inside.add(0, -0.3, 0);
        double surface = Math.min(yTop - 1.0 / 16, Math.max(yBottom + 1.0 / 16, t.waterSurfaceY()));
        return new Path(bottom, top, inside, nozzle, out.getOpposite(), surface, t.waterMb() > 0);
    }

    /** Path of the filter block at {@code pos} facing the tank ({@code facing} = direction to the tank). */
    @Nullable
    public static Path forFilterBlock(TankData t, BlockPos pos, Direction facing) {
        double lateral = facing.getAxis() == Direction.Axis.Z ? pos.getX() + 0.5 : pos.getZ() + 0.5;
        return compute(t, pos.relative(facing), facing.getOpposite(), lateral, pos.getY() + 14 / 16.0);
    }

    /** Path of a filter inside the cabinet at {@code pos} (tank above it): up the tank's back wall. */
    @Nullable
    public static Path forCabinet(TankData t, BlockPos pos, Direction cabinetFront) {
        Direction back = cabinetFront.getOpposite();
        double lateral = back.getAxis() == Direction.Axis.Z ? pos.getX() + 0.5 : pos.getZ() + 0.5;
        BlockPos above = pos.above();
        // M10: the back wall of this row of the shape
        BlockPos wall = t.contains(above) ? TankShape.walk(t.members(), above, back) : above;
        return compute(t, wall, back, lateral, pos.getY() + 0.75);
    }
}
