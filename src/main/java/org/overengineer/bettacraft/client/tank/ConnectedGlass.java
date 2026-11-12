package org.overengineer.bettacraft.client.tank;

import net.minecraft.core.Direction;

/**
 * M11: connected glass textures for textured tanks, as pure math (unit-tested, no client classes).
 * <p>
 * Glass textures have a frame (vanilla: the outer ring of texels; stained glass draws it more opaque, which looked like
 * darker water along every block border). Inside one tank that frame must only show along the outer edges of the whole
 * tank. Instead of cutting faces into pieces (the extra translucent quads would sort differently against the water
 * behind them and bring the seams back), every face keeps <b>one</b> quad per glass quad and only its UVs change: on an
 * edge where the face goes on in the neighbouring block, the texture is stretched over the frame so the frame ring is
 * not sampled there. The stretch is at most 2 frame widths (2/16 for vanilla glass), so the streaks stay in place.
 * <p>
 * Edge slots of a face: 0 = low end of the face-local u, 1 = high u, 2 = low v, 3 = high v (u/v as in
 * {@link BoxFaces#u}/{@link BoxFaces#v}).
 */
public final class ConnectedGlass {
    private ConnectedGlass() {}

    /** Slot (0..3) of the edge of the face towards {@code side} that lies towards the in-plane direction {@code e}. */
    public static int slot(Direction side, Direction e) {
        float x = 0.5F + 0.5F * e.getStepX(), y = 0.5F + 0.5F * e.getStepY(), z = 0.5F + 0.5F * e.getStepZ();
        float u = BoxFaces.u(side, x, y, z), v = BoxFaces.v(side, x, y, z);
        if (u < 0.25F) return 0;
        if (u > 0.75F) return 1;
        return v < 0.25F ? 2 : 3;
    }

    /**
     * Edge slots (bit = slot) of the face towards {@code side} whose glass frame is hidden: the face continues into the
     * neighbour (connected, and no concave crease, bit {@code side * 6 + e} of {@code concave}).
     */
    public static int hiddenSlots(int connectionMask, long concave, Direction side) {
        int out = 0;
        for (Direction e : Direction.values()) {
            if (e.getAxis() == side.getAxis()) continue;
            boolean connected = (connectionMask & (1 << e.ordinal())) != 0;
            boolean crease = (concave & (1L << (side.ordinal() * 6 + e.ordinal()))) != 0;
            if (connected && !crease) out |= 1 << slot(side, e);
        }
        return out;
    }

    /** Face-local texture coordinate for the face-local position {@code s}: a hidden end skips the frame width {@code f}. */
    public static float remap(float s, boolean lowHidden, boolean highHidden, float f) {
        float lo = lowHidden ? f : 0F, hi = highHidden ? 1F - f : 1F;
        return lo + s * (hi - lo);
    }

    /**
     * New atlas UVs of one quad. {@code s,t} = face-local position of the 4 vertices, {@code u,v} = their atlas UVs (any
     * rotation or mirroring, as long as the mapping is affine like on every box face). The affine map position -> UV is
     * solved from the vertices and evaluated at the remapped positions; the result is written to {@code u,v}.
     * Returns false (UVs untouched) for a degenerate quad.
     */
    public static boolean remapUv(float[] s, float[] t, float[] u, float[] v, int hidden, float f) {
        return clipAndRemapUv(s, t, u, v, hidden, f, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
    }

    /**
     * Like {@link #remapUv}, but first clamps the face-local {@code t} of every vertex into {@code [tMin, tMax]} (written
     * back to {@code t}): one piece of the quad, with the UVs it had there. Used to split the glass of a side face at
     * the water height (the water seam fix, see {@link TankWaterGeometry}).
     */
    public static boolean clipAndRemapUv(float[] s, float[] t, float[] u, float[] v, int hidden, float f, float tMin, float tMax) {
        int[][] tri = {{0, 1, 2}, {0, 1, 3}, {0, 2, 3}, {1, 2, 3}};
        for (int[] k : tri) {
            int i0 = k[0], i1 = k[1], i2 = k[2];
            float ds1 = s[i1] - s[i0], dt1 = t[i1] - t[i0], ds2 = s[i2] - s[i0], dt2 = t[i2] - t[i0];
            float det = ds1 * dt2 - ds2 * dt1;
            if (Math.abs(det) < 1e-6F) continue;
            float du1 = u[i1] - u[i0], du2 = u[i2] - u[i0], dv1 = v[i1] - v[i0], dv2 = v[i2] - v[i0];
            // U = u0 + aU * (s - s0) + bU * (t - t0)
            float aU = (du1 * dt2 - du2 * dt1) / det, bU = (ds1 * du2 - ds2 * du1) / det;
            float aV = (dv1 * dt2 - dv2 * dt1) / det, bV = (ds1 * dv2 - ds2 * dv1) / det;
            float s0 = s[i0], t0 = t[i0], u0 = u[i0], v0 = v[i0];
            for (int i = 0; i < 4; i++) {
                t[i] = Math.max(tMin, Math.min(tMax, t[i]));
                float ns = remap(s[i], (hidden & 1) != 0, (hidden & 2) != 0, f);
                float nt = remap(t[i], (hidden & 4) != 0, (hidden & 8) != 0, f);
                u[i] = u0 + aU * (ns - s0) + bU * (nt - t0);
                v[i] = v0 + aV * (ns - s0) + bV * (nt - t0);
            }
            return true;
        }
        return false;
    }

    /**
     * Width of the frame of a square glass texture, in texels: how many rings from the border inwards differ clearly
     * (alpha or colour) from the inside of the texture. {@code argb} row by row, {@code w x h} (first frame only).
     * 0 = no frame (nothing to hide), at most {@code w / 8}.
     */
    public static int frameTexels(int[] argb, int w, int h) {
        if (w < 8 || h < 8 || argb.length < w * h) return w / 16;
        int rings = Math.min(w, h) / 2;
        double[][] sum = new double[rings][4];
        int[] count = new int[rings];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int ring = Math.min(Math.min(x, y), Math.min(w - 1 - x, h - 1 - y));
                int c = argb[y * w + x];
                double a = (c >>> 24) & 0xFF;
                sum[ring][0] += a;
                // colour weighted by alpha (the colour of invisible texels does not matter)
                sum[ring][1] += ((c >> 16) & 0xFF) * a / 255.0;
                sum[ring][2] += ((c >> 8) & 0xFF) * a / 255.0;
                sum[ring][3] += (c & 0xFF) * a / 255.0;
                count[ring]++;
            }
        }
        double[] ref = new double[4];
        int refCount = 0;
        for (int r = Math.max(1, rings / 2); r < rings; r++) {
            for (int k = 0; k < 4; k++) ref[k] += sum[r][k];
            refCount += count[r];
        }
        if (refCount == 0) return 0;
        for (int k = 0; k < 4; k++) ref[k] /= refCount;
        int max = Math.max(1, w / 8);
        int n = 0;
        while (n < max) {
            double da = Math.abs(sum[n][0] / count[n] - ref[0]);
            double dc = 0;
            for (int k = 1; k < 4; k++) dc = Math.max(dc, Math.abs(sum[n][k] / count[n] - ref[k]));
            if (da < 24 && dc < 28) break;
            n++;
        }
        return n;
    }
}
