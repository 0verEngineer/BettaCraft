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
        return clipAndRemapUv(s, t, u, v, hidden, f, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, tMin, tMax);
    }

    /** Like {@link #clipAndRemapUv(float[], float[], float[], float[], int, float, float, float)}, also clipped in s. */
    public static boolean clipAndRemapUv(float[] s, float[] t, float[] u, float[] v, int hidden, float f, float sMin, float sMax,
                                         float tMin, float tMax) {
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
                s[i] = Math.max(sMin, Math.min(sMax, s[i]));
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
     * M64: the texture edges ({@link GlassVariants#LEFT} ...) of the sprite that lie on the face-local edge slots {@code hidden}.
     * {@code s,t} = face-local positions of the 4 vertices, {@code u,v} = their texture coordinates within the sprite (0..1,
     * any rotation or mirroring). -1 when the quad does not cover the whole sprite (then the frameless copies do not fit) or
     * is degenerate.
     */
    public static int textureEdges(float[] s, float[] t, float[] u, float[] v, int hidden) {
        float uMin = Math.min(Math.min(u[0], u[1]), Math.min(u[2], u[3])), uMax = Math.max(Math.max(u[0], u[1]), Math.max(u[2], u[3]));
        float vMin = Math.min(Math.min(v[0], v[1]), Math.min(v[2], v[3])), vMax = Math.max(Math.max(v[0], v[1]), Math.max(v[2], v[3]));
        if (uMin > 0.02F || vMin > 0.02F || uMax < 0.98F || vMax < 0.98F) return -1;
        int[][] tri = {{0, 1, 2}, {0, 1, 3}, {0, 2, 3}, {1, 2, 3}};
        for (int[] k : tri) {
            int i0 = k[0], i1 = k[1], i2 = k[2];
            float ds1 = s[i1] - s[i0], dt1 = t[i1] - t[i0], ds2 = s[i2] - s[i0], dt2 = t[i2] - t[i0];
            float det = ds1 * dt2 - ds2 * dt1;
            if (Math.abs(det) < 1e-6F) continue;
            float du1 = u[i1] - u[i0], du2 = u[i2] - u[i0], dv1 = v[i1] - v[i0], dv2 = v[i2] - v[i0];
            float aU = (du1 * dt2 - du2 * dt1) / det, bU = (ds1 * du2 - ds2 * du1) / det;
            float aV = (dv1 * dt2 - dv2 * dt1) / det, bV = (ds1 * dv2 - ds2 * dv1) / det;
            int out = 0;
            // the middle of each face edge: slot 0 = (0, .5), 1 = (1, .5), 2 = (.5, 0), 3 = (.5, 1)
            float[][] mid = {{0F, 0.5F}, {1F, 0.5F}, {0.5F, 0F}, {0.5F, 1F}};
            for (int slot = 0; slot < 4; slot++) {
                if ((hidden & (1 << slot)) == 0) continue;
                float uu = u[i0] + aU * (mid[slot][0] - s[i0]) + bU * (mid[slot][1] - t[i0]);
                float vv = v[i0] + aV * (mid[slot][0] - s[i0]) + bV * (mid[slot][1] - t[i0]);
                if (uu < 0.25F) out |= GlassVariants.LEFT;
                else if (uu > 0.75F) out |= GlassVariants.RIGHT;
                else if (vv < 0.25F) out |= GlassVariants.TOP;
                else if (vv > 0.75F) out |= GlassVariants.BOTTOM;
            }
            return out;
        }
        return -1;
    }

    /**
     * M64: a copy of a square glass texture ({@code argb} row by row) without its frame ({@code f} texels wide) on the texture
     * edges {@code edges}: a frame texel is replaced when every edge whose frame it belongs to is one of them; a corner between a
     * hidden and a kept edge takes the colour of the kept edge's frame next to it (the frame goes on along the tank's outer edge,
     * in its own colour). It gets the colour most of the inside has
     * (vanilla glass: clear; stained glass: its tint), or, for an inside without such a colour, the texel mirrored inwards
     * across the frame line.
     */
    public static int[] withoutFrame(int[] argb, int w, int h, int f, int edges) {
        int[] out = argb.clone();
        if (f <= 0 || edges == 0) return out;
        // the most common colour of the inside
        java.util.HashMap<Integer, Integer> count = new java.util.HashMap<>();
        int inside = 0;
        for (int y = f; y < h - f; y++) for (int x = f; x < w - f; x++) {
            count.merge(argb[y * w + x], 1, Integer::sum);
            inside++;
        }
        int mode = 0, best = 0;
        for (var e : count.entrySet()) if (e.getValue() > best) {
            best = e.getValue();
            mode = e.getKey();
        }
        boolean useMode = inside > 0 && best >= inside * 0.4;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int in = 0;
            if (x < f) in |= GlassVariants.LEFT;
            if (x >= w - f) in |= GlassVariants.RIGHT;
            if (y < f) in |= GlassVariants.TOP;
            if (y >= h - f) in |= GlassVariants.BOTTOM;
            if (in == 0) continue;
            if ((in & ~edges) != 0) {
                // a corner of a kept edge next to a hidden one: the kept edge's frame goes on, in its own colour (vanilla glass
                // shades its frame: the corner has the colour of the side frame, which put a dark texel into the light top line
                // at every block junction) - the frame texel next to it along the kept edge, out of the hidden edge's frame
                if ((in & edges) == 0) continue;
                int sx = x, sy = y;
                if ((in & edges & GlassVariants.LEFT) != 0) sx = f;
                if ((in & edges & GlassVariants.RIGHT) != 0) sx = w - f - 1;
                if ((in & edges & GlassVariants.TOP) != 0) sy = f;
                if ((in & edges & GlassVariants.BOTTOM) != 0) sy = h - f - 1;
                out[y * w + x] = argb[sy * w + sx];
                continue;
            }
            if (useMode) {
                out[y * w + x] = mode;
            } else {
                int mx = x < f ? 2 * f - 1 - x : x >= w - f ? 2 * (w - f) - 1 - x : x;
                int my = y < f ? 2 * f - 1 - y : y >= h - f ? 2 * (h - f) - 1 - y : y;
                out[y * w + x] = argb[Math.clamp(my, 0, h - 1) * w + Math.clamp(mx, 0, w - 1)];
            }
        }
        return out;
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
