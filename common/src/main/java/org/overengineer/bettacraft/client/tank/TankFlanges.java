package org.overengineer.bettacraft.client.tank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.platform.client.QuadBaker;
import org.overengineer.bettacraft.plumbing.FilterPipeBlock;
import org.overengineer.bettacraft.plumbing.FilterPipeBundleBlock;
import org.overengineer.bettacraft.plumbing.FilterPipes;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.PipeCovers;
import org.overengineer.bettacraft.plumbing.PipeLine;
import org.overengineer.bettacraft.tank.TankBlock;

/**
 * M65 (Nachtrag 2), the glass bulkhead of a pipe on a tank: where a water pipe, a filter pipe or a line of a pipe bundle joins a
 * tank block, the tank draws a steel flange on the glass outside (4.5 px, like the collar of a T-piece, 0.75 px thick) and
 * exactly opposite, inside the glass, an equally big counter-flange (1 px, behind the water's side face) with a 3 px stub
 * 2 px into the tank, at world scale. The miniature's own pipe dock ({@code PipeAnchorBlock}, in the middle of the same face)
 * sits inside that stub, so the thin interior hose leaves the stub. A side where several lines of a bundle join the tank gets
 * one smaller flange (3.6 px) per line at the line's spot.
 * <p>
 * {@link #mask} reads the neighbours while the chunk is meshed (5 bits per side: bit 0 a flange in the middle, bits 1..4 the
 * lines of a bundle at their spots); {@link #quads} are cached per mask.
 */
public final class TankFlanges {
    private static final float FLANGE = 4.5F, SMALL = 3.6F, STUB = 3F;
    private static final float OUT = 0.75F, IN = 1F, STUB_DEPTH = 3F;
    private static final Map<Integer, List<BakedQuad>> CACHE = new ConcurrentHashMap<>();

    private TankFlanges() {}

    public static void clearCache() {
        CACHE.clear();
    }

    /** The flanges of the outer faces of the tank block at {@code pos} (0 = none). */
    public static int mask(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        int out = 0;
        for (Direction d : Direction.values()) {
            if (TankBlock.isConnected(state, d)) continue;
            int bits = side(level, pos.relative(d), d.getOpposite());
            out |= bits << (d.ordinal() * 5);
        }
        return out;
    }

    /** The bits of one side: the pipe at {@code n} joined towards the tank ({@code back}). */
    private static int side(BlockAndTintGetter level, BlockPos n, Direction back) {
        BlockState s = level.getBlockState(n);
        if (s.getBlock() instanceof PipeBlock) return s.getValue(PipeBlock.SIDES.get(back)) ? 1 : 0;
        if (s.getBlock() instanceof FilterPipeBlock || s.getBlock() instanceof FilterPipeBundleBlock) {
            if (PipeCovers.coverAt(level, n) != null) return 0; // a covered pipe is a full block: no flange on it
            int lines = FilterPipes.packed(level, n, s);
            int bits = 0, count = 0;
            for (PipeLine l : PipeLine.values()) {
                if (FilterPipes.joined(FilterPipes.slot(lines, l), back)) {
                    bits |= 1 << (1 + l.ordinal());
                    count++;
                }
            }
            if (count == 0) return 0;
            // a single pipe, or the one line of a bundle on that side: its arm is in the middle (FilterPipeBundleBlock#centredSides)
            return count == 1 ? 1 : bits;
        }
        return 0;
    }

    public static List<BakedQuad> quads(int mask) {
        return CACHE.computeIfAbsent(mask, TankFlanges::build);
    }

    private static List<BakedQuad> build(int mask) {
        TextureAtlasSprite sprite = org.overengineer.bettacraft.client.ClientCompat.blockSprites().apply(BettaCraft.id("block/pipe_tee"));
        List<BakedQuad> out = new ArrayList<>();
        for (Direction d : Direction.values()) {
            int bits = (mask >>> (d.ordinal() * 5)) & 31;
            if (bits == 0) continue;
            if ((bits & 1) != 0) flange(out, sprite, d, 8, 8, FLANGE);
            for (PipeLine l : PipeLine.values()) {
                if ((bits & (1 << (1 + l.ordinal()))) == 0) continue;
                // the spot of the line's arm on the bundle's face towards the tank (the same in-plane box on the tank's face)
                float[] a = FilterPipeBundleBlock.arm(l, d.getOpposite(), FilterPipeBundleBlock.ARM, 0);
                int[] ax = inPlane(d);
                flange(out, sprite, d, (a[ax[0]] + a[ax[0] + 3]) / 2, (a[ax[1]] + a[ax[1] + 3]) / 2, SMALL);
            }
        }
        return List.copyOf(out);
    }

    /** The two axes (0 = x, 1 = y, 2 = z) in the plane of face {@code d}. */
    private static int[] inPlane(Direction d) {
        return switch (d.getAxis()) {
            case X -> new int[]{1, 2};
            case Y -> new int[]{0, 2};
            case Z -> new int[]{0, 1};
        };
    }

    /** Flange outside, counter-flange inside and stub on face {@code d}, centred at (cu, cv) pixels of the face plane. */
    private static void flange(List<BakedQuad> out, TextureAtlasSprite sprite, Direction d, float cu, float cv, float size) {
        boolean pos = d.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        float face = pos ? 16 : 0, sign = pos ? 1 : -1;
        // outside on the glass, inside behind it, the stub into the tank
        box(out, sprite, d, cu, cv, size, face, face + sign * OUT, true, false);
        box(out, sprite, d, cu, cv, size, face, face - sign * IN, false, true);
        float stub = size == SMALL ? STUB * SMALL / FLANGE : STUB;
        box(out, sprite, d, cu, cv, stub, face - sign * IN, face - sign * STUB_DEPTH, false, false);
    }

    /**
     * A box of {@code size} pixels around (cu, cv) in the face plane, from depth {@code n0} to {@code n1} (pixels along the face's
     * axis); {@code skipInner} / {@code skipOuter}: no face on the glass plane (the side at n0).
     */
    private static void box(List<BakedQuad> out, TextureAtlasSprite sprite, Direction d, float cu, float cv, float size, float n0, float n1,
                            boolean skipInner, boolean skipOuter) {
        int[] ax = inPlane(d);
        int na = d.getAxis().ordinal();
        float[] min = new float[3], max = new float[3];
        min[ax[0]] = cu - size / 2;
        max[ax[0]] = cu + size / 2;
        min[ax[1]] = cv - size / 2;
        max[ax[1]] = cv + size / 2;
        min[na] = Math.min(n0, n1);
        max[na] = Math.max(n0, n1);
        float[] c = new float[12];
        for (Direction f : Direction.values()) {
            // the face lying on the glass plane (n0) is hidden by the glass / the other half: left out
            if (f.getAxis().ordinal() == na) {
                float plane = f.getAxisDirection() == Direction.AxisDirection.POSITIVE ? max[na] : min[na];
                if (Math.abs(plane - n0) < 1e-4F && (skipInner || skipOuter)) continue;
            }
            BoxFaces.corners(f, min[0] / 16, min[1] / 16, min[2] / 16, max[0] / 16, max[1] / 16, max[2] / 16, c);
            QuadBaker q = new QuadBaker().sprite(sprite).direction(f).shade(true).tint(-1);
            float fu0 = Float.MAX_VALUE, fu1 = -Float.MAX_VALUE, fv0 = Float.MAX_VALUE, fv1 = -Float.MAX_VALUE;
            for (int i = 0; i < 4; i++) {
                float u = BoxFaces.u(f, c[i * 3], c[i * 3 + 1], c[i * 3 + 2]), v = BoxFaces.v(f, c[i * 3], c[i * 3 + 1], c[i * 3 + 2]);
                fu0 = Math.min(fu0, u);
                fu1 = Math.max(fu1, u);
                fv0 = Math.min(fv0, v);
                fv1 = Math.max(fv1, v);
            }
            for (int i = 0; i < 4; i++) {
                float x = c[i * 3], y = c[i * 3 + 1], z = c[i * 3 + 2];
                // the texture's middle (the collar texture is plain steel there), whatever the box's place in the block
                float u = 0.36F + 0.28F * frac(BoxFaces.u(f, x, y, z), fu0, fu1);
                float v = 0.36F + 0.28F * frac(BoxFaces.v(f, x, y, z), fv0, fv1);
                q.vertex(x, y, z, 0xFFFFFFFF, sprite.getU(u), sprite.getV(v), f.getStepX(), f.getStepY(), f.getStepZ());
            }
            out.add(q.bake());
        }
    }

    private static float frac(float x, float a, float b) {
        return b - a < 1e-6F ? 0 : (x - a) / (b - a);
    }
}
