package org.overengineer.bettacraft.plants;

/**
 * Voxel walk along a ray through a box of interior blocks (Amanatides-Woo), used to aim shears from outside the tank
 * into the miniature (M13, like the M12 net aims at the floating plants). Pure (unit-tested).
 */
public final class PlantRay {
    /** Visitor of the voxels along the ray; return true to stop. */
    @FunctionalInterface
    public interface Visitor {
        boolean visit(int x, int y, int z);
    }

    private PlantRay() {}

    /**
     * Walks the voxels of the box [0, sx) x [0, sy) x [0, sz) that the ray o + t d (t in [0, maxT]) passes, in order.
     * Returns true if the visitor stopped the walk.
     */
    public static boolean walk(double ox, double oy, double oz, double dx, double dy, double dz, int sx, int sy, int sz, double maxT, Visitor v) {
        // clip the ray to the box
        double t0 = 0, t1 = maxT;
        double[] o = {ox, oy, oz}, d = {dx, dy, dz};
        int[] size = {sx, sy, sz};
        for (int a = 0; a < 3; a++) {
            if (Math.abs(d[a]) < 1e-12) {
                if (o[a] < 0 || o[a] >= size[a]) return false;
                continue;
            }
            double ta = (0 - o[a]) / d[a], tb = (size[a] - o[a]) / d[a];
            if (ta > tb) {
                double k = ta;
                ta = tb;
                tb = k;
            }
            t0 = Math.max(t0, ta);
            t1 = Math.min(t1, tb);
            if (t0 > t1) return false;
        }
        double e = 1e-7;
        double px = ox + dx * (t0 + e), py = oy + dy * (t0 + e), pz = oz + dz * (t0 + e);
        int x = clamp((int) Math.floor(px), sx), y = clamp((int) Math.floor(py), sy), z = clamp((int) Math.floor(pz), sz);
        int stepX = dx > 0 ? 1 : -1, stepY = dy > 0 ? 1 : -1, stepZ = dz > 0 ? 1 : -1;
        double tMaxX = Math.abs(dx) < 1e-12 ? Double.MAX_VALUE : ((dx > 0 ? x + 1 : x) - ox) / dx;
        double tMaxY = Math.abs(dy) < 1e-12 ? Double.MAX_VALUE : ((dy > 0 ? y + 1 : y) - oy) / dy;
        double tMaxZ = Math.abs(dz) < 1e-12 ? Double.MAX_VALUE : ((dz > 0 ? z + 1 : z) - oz) / dz;
        double tdX = Math.abs(dx) < 1e-12 ? Double.MAX_VALUE : Math.abs(1 / dx);
        double tdY = Math.abs(dy) < 1e-12 ? Double.MAX_VALUE : Math.abs(1 / dy);
        double tdZ = Math.abs(dz) < 1e-12 ? Double.MAX_VALUE : Math.abs(1 / dz);
        int guard = sx + sy + sz + 8;
        while (guard-- > 0) {
            if (x < 0 || y < 0 || z < 0 || x >= sx || y >= sy || z >= sz) return false;
            if (v.visit(x, y, z)) return true;
            double next = Math.min(tMaxX, Math.min(tMaxY, tMaxZ));
            if (next > t1) return false;
            if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
                x += stepX;
                tMaxX += tdX;
            } else if (tMaxY <= tMaxZ) {
                y += stepY;
                tMaxY += tdY;
            } else {
                z += stepZ;
                tMaxZ += tdZ;
            }
        }
        return false;
    }

    private static int clamp(int v, int size) {
        return Math.max(0, Math.min(size - 1, v));
    }
}
