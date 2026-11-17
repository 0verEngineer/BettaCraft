package org.overengineer.bettacraft.client.tank;

import net.minecraft.core.Direction;

/**
 * Water body of one tank block (pure geometry from the block state, unit-tested by {@code TankWaterGeometryTest}).
 * The water of a tank is drawn block by block; seamless water across blocks needs:
 * <ul>
 *     <li>no face towards a connected neighbour (no wall of water inside the tank),</li>
 *     <li>towards a connected neighbour the volume reaches the block border (0 or 1) so neighbouring blocks abut without
 *     gap or overlap - an overlap of two translucent faces would be darker, a gap lighter,</li>
 *     <li>the surface of every block of one layer at the same height (the layer's fill), up to the block top where
 *     water goes on in the block above (then there is no top face).</li>
 * </ul>
 * M11, the water seam behind translucent (stained) glass: chunk meshes sort translucent quads by their centre, and
 * translucent quads write depth. When the centre of a water face was nearer to the camera than the centre of the glass
 * face in front of it, the glass was drawn first and hid the water behind it (the junction of the T, block borders).
 * A face next to an outer side is inset there too, so its centre is shifted sideways and the order flipped at flat view
 * angles (M10). The first M11 fix let every face span the full block sideways instead; at the outer corners the faces
 * then stuck out past each other by {@link #INSET} (visible strips at the edges, the surface reaching past the side
 * faces when looking at a corner at a flat angle). Now the water is a closed box again ({@link #face}: the body's own
 * bounds, inset on the outer sides, full width towards connected blocks) and the translucent glass in front of it is
 * cut at the same lines ({@link #glassCuts}): at the water height and {@link #INSET} from every outer edge. So each
 * water face has exactly the centre of the glass piece in front of it, moved inwards: for every camera that can see
 * that glass piece the water is the farther one and is drawn first.
 */
public final class TankWaterGeometry {
    public static final float INSET = 0.5F / 16F;

    private TankWaterGeometry() {}

    /**
     * The water volume {x0, y0, z0, x1, y1, z1} (inset on the outer sides) plus the faces to draw (bit = direction
     * ordinal). The faces themselves come from {@link #face}.
     */
    public record Body(float x0, float y0, float z0, float x1, float y1, float z1, int faces) {
        public boolean has(Direction d) {
            return (faces & (1 << d.ordinal())) != 0;
        }
    }

    /**
     * @param mask       connection mask of the block state (bit = direction ordinal, see {@code TankBlock.connectionMask})
     * @param fill       fill height in 1/16 (1..16)
     * @param waterAbove the connected block above holds water too
     * @return the water body, or null for a dry block
     */
    public static Body body(int mask, int fill, boolean waterAbove) {
        if (fill <= 0) return null;
        float x0 = connected(mask, Direction.WEST) ? 0 : INSET;
        float x1 = connected(mask, Direction.EAST) ? 1 : 1 - INSET;
        float z0 = connected(mask, Direction.NORTH) ? 0 : INSET;
        float z1 = connected(mask, Direction.SOUTH) ? 1 : 1 - INSET;
        float y0 = connected(mask, Direction.DOWN) ? 0 : INSET;
        float y1 = surface(fill, waterAbove);
        int faces = 0;
        for (Direction d : Direction.values()) {
            boolean render = d == Direction.UP ? !waterAbove : !connected(mask, d);
            if (render) faces |= 1 << d.ordinal();
        }
        return new Body(x0, y0, z0, x1, y1, z1, faces);
    }

    /** Top of the water in a block: the fill height, just below the rim when full, the block top with water above. */
    public static float surface(int fill, boolean waterAbove) {
        if (fill <= 0) return 0;
        return waterAbove ? 1 : Math.min(fill / 16F, 1 - INSET);
    }

    /**
     * The quad of face {@code d} of the body into {@code out[12]} (vanilla winding, see {@link BoxFaces#corners}): inset
     * by {@link #INSET} along the normal only, full block width sideways, side faces from 0 up to the water height.
     */
    public static void face(Body b, Direction d, float[] out) {
        BoxFaces.corners(d, b.x0(), b.y0(), b.z0(), b.x1(), b.y1(), b.z1(), out);
    }

    /**
     * Where the translucent glass face {@code side} of a block with this water is cut, as {sCuts..., NaN, tCuts...} in the
     * face coordinates of {@link BoxFaces#u}/{@link BoxFaces#v} (0..1): {@link #INSET} from each outer edge of the water
     * face behind it and, on the side faces, at the water height. Empty without water behind the glass.
     */
    public static float[] glassCuts(int mask, int fill, boolean waterAbove, Direction side) {
        Body b = body(mask, fill, waterAbove);
        if (b == null || !b.has(side)) return new float[0];
        float[] q = new float[12];
        face(b, side, q);
        float sMin = 1, sMax = 0, tMin = 1, tMax = 0;
        for (int i = 0; i < 4; i++) {
            float s = BoxFaces.u(side, q[i * 3], q[i * 3 + 1], q[i * 3 + 2]), t = BoxFaces.v(side, q[i * 3], q[i * 3 + 1], q[i * 3 + 2]);
            sMin = Math.min(sMin, s);
            sMax = Math.max(sMax, s);
            tMin = Math.min(tMin, t);
            tMax = Math.max(tMax, t);
        }
        float[] out = new float[5];
        int n = 0;
        if (sMin > 1e-4F) out[n++] = sMin;
        if (sMax < 1 - 1e-4F) out[n++] = sMax;
        out[n++] = Float.NaN;
        if (tMin > 1e-4F) out[n++] = tMin;
        if (tMax < 1 - 1e-4F) out[n++] = tMax;
        return java.util.Arrays.copyOf(out, n);
    }

    /**
     * Height (0..1) at which the glass of a horizontal outer face is split so that the lower piece lies exactly in front
     * of the water side face; 0 = no split (dry block, or water up to the block top).
     */
    public static float glassSplit(int fill, boolean waterAbove) {
        float y1 = surface(fill, waterAbove);
        return y1 > 0 && y1 < 1 ? y1 : 0;
    }

    public static boolean connected(int mask, Direction d) {
        return (mask & (1 << d.ordinal())) != 0;
    }
}
