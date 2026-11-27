package org.overengineer.bettacraft.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.tank.BoxFaces;
import org.overengineer.bettacraft.platform.client.QuadBaker;
import org.overengineer.bettacraft.plumbing.PipeBundleBlock;
import org.overengineer.bettacraft.plumbing.PipeConnectable;
import org.overengineer.bettacraft.plumbing.PipePort;
import org.overengineer.bettacraft.plumbing.PipeRing;
import org.overengineer.bettacraft.plumbing.Pipes;
import org.overengineer.bettacraft.tank.TankBlock;

/**
 * M68: the quads of the pipes made in code, like the M65 pipe models of the datagen ({@code PipeModels}): every face textured
 * with the pipe texture's columns across the pipe and its rows along it ({@code <texture>} where the face's V runs along the
 * pipe, its transposed copy {@code <texture>_r} where U does), the default UVs from the position, so the ring of the texture
 * sits at the block borders. Used for the arms of a bundle's pipes (each in the texture of its material and ring, off the
 * middle with the pipe's shaded columns forced across it) and the caps of the sides closed with the pipe wrench. Cached.
 */
public final class PipeQuads {
    private PipeQuads() {}

    static final float A = 6.5F, B = 9.5F;
    /** The cap on a closed side: 4.5 px wide, 1 px out of the pipe's core (a single pipe) or the bundle's manifold. */
    static final float CAP = 4.5F, CAP_DEPTH = 1.0F;
    private static final Map<Long, List<BakedQuad>> CACHE = new ConcurrentHashMap<>();

    public static void clearCache() {
        CACHE.clear();
    }

    static TextureAtlasSprite sprite(String path) {
        return org.overengineer.bettacraft.client.ClientCompat.blockSprites().apply(BettaCraft.id(path));
    }

    /** The texture of a pipe of this material and ring ({@code transposed}: the {@code _r} copy). */
    public static String texture(boolean steel, PipeRing ring, boolean transposed) {
        return "block/" + (steel ? "stainless_steel_pipe" : "glass_pipe") + (ring == PipeRing.NONE ? "" : "_" + ring.getSerializedName()) + (transposed ? "_r" : "");
    }

    private static Direction.Axis vAxis(Direction f) {
        return f.getAxis() == Direction.Axis.Y ? Direction.Axis.Z : Direction.Axis.Y;
    }

    /**
     * A piece along {@code along} (box {@code from}..{@code to}, pixels): its long faces textured along it, the faces at its ends
     * only those in {@code ends}; {@code across} != null: the coordinate across the pipe forced to that range of the texture.
     */
    static void piece(List<BakedQuad> out, float[] from, float[] to, Direction.Axis along, List<Direction> ends, @Nullable float[] across,
                      TextureAtlasSprite t, TextureAtlasSprite r) {
        float[] c = new float[12];
        for (Direction f : Direction.values()) {
            boolean end = f.getAxis() == along;
            if (end && !ends.contains(f)) continue;
            TextureAtlasSprite sp = end || vAxis(f) == along ? t : r;
            BoxFaces.corners(f, from[0] / 16, from[1] / 16, from[2] / 16, to[0] / 16, to[1] / 16, to[2] / 16, c);
            float u0 = Float.MAX_VALUE, u1 = -Float.MAX_VALUE, v0 = Float.MAX_VALUE, v1 = -Float.MAX_VALUE;
            for (int i = 0; i < 4; i++) {
                float u = BoxFaces.u(f, c[i * 3], c[i * 3 + 1], c[i * 3 + 2]), v = BoxFaces.v(f, c[i * 3], c[i * 3 + 1], c[i * 3 + 2]);
                u0 = Math.min(u0, u);
                u1 = Math.max(u1, u);
                v0 = Math.min(v0, v);
                v1 = Math.max(v1, v);
            }
            QuadBaker q = new QuadBaker().sprite(sp).direction(f).shade(true).tint(-1);
            for (int i = 0; i < 4; i++) {
                float x = c[i * 3], y = c[i * 3 + 1], z = c[i * 3 + 2];
                float u = BoxFaces.u(f, x, y, z), v = BoxFaces.v(f, x, y, z);
                if (across != null && !end) {
                    if (vAxis(f) == along) u = (across[0] + (across[1] - across[0]) * frac(u, u0, u1)) / 16;
                    else v = (across[0] + (across[1] - across[0]) * frac(v, v0, v1)) / 16;
                }
                q.vertex(x, y, z, 0xFFFFFFFF, sp.getU(u), sp.getV(v), f.getStepX(), f.getStepY(), f.getStepZ());
            }
            out.add(q.bake());
        }
    }

    private static float frac(float x, float a, float b) {
        return b - a < 1e-6F ? 0 : (x - a) / (b - a);
    }

    /** A plain box in the middle of the collar texture (the caps). */
    static void box(List<BakedQuad> out, float[] from, float[] to, TextureAtlasSprite sp) {
        float[] c = new float[12];
        for (Direction f : Direction.values()) {
            BoxFaces.corners(f, from[0] / 16, from[1] / 16, from[2] / 16, to[0] / 16, to[1] / 16, to[2] / 16, c);
            float u0 = Float.MAX_VALUE, u1 = -Float.MAX_VALUE, v0 = Float.MAX_VALUE, v1 = -Float.MAX_VALUE;
            for (int i = 0; i < 4; i++) {
                float u = BoxFaces.u(f, c[i * 3], c[i * 3 + 1], c[i * 3 + 2]), v = BoxFaces.v(f, c[i * 3], c[i * 3 + 1], c[i * 3 + 2]);
                u0 = Math.min(u0, u);
                u1 = Math.max(u1, u);
                v0 = Math.min(v0, v);
                v1 = Math.max(v1, v);
            }
            QuadBaker q = new QuadBaker().sprite(sp).direction(f).shade(true).tint(-1);
            for (int i = 0; i < 4; i++) {
                float x = c[i * 3], y = c[i * 3 + 1], z = c[i * 3 + 2];
                float u = 0.36F + 0.28F * frac(BoxFaces.u(f, x, y, z), u0, u1), v = 0.36F + 0.28F * frac(BoxFaces.v(f, x, y, z), v0, v1);
                q.vertex(x, y, z, 0xFFFFFFFF, sp.getU(u), sp.getV(v), f.getStepX(), f.getStepY(), f.getStepZ());
            }
            out.add(q.bake());
        }
    }

    /** The arm of slot {@code slot} of a bundle towards {@code d} ({@code mid}: in the middle of the face). */
    public static List<BakedQuad> bundleArm(int slot, Direction d, boolean mid, boolean steel, PipeRing ring) {
        long key = 1L << 40 | (long) slot << 32 | (long) d.ordinal() << 24 | (mid ? 1L << 20 : 0) | (steel ? 1L << 16 : 0) | ring.ordinal();
        return CACHE.computeIfAbsent(key, k -> {
            List<BakedQuad> out = new ArrayList<>();
            float[] b = PipeBundleBlock.arm(slot, d, PipeBundleBlock.ARM, 0, mid);
            piece(out, new float[]{b[0], b[1], b[2]}, new float[]{b[3], b[4], b[5]}, d.getAxis(), List.of(), new float[]{A, B},
                    sprite(texture(steel, ring, false)), sprite(texture(steel, ring, true)));
            return List.copyOf(out);
        });
    }

    /** The cap of a closed side of a single pipe (on its core, towards {@code d}). */
    public static List<BakedQuad> pipeCap(Direction d) {
        return CACHE.computeIfAbsent(2L << 40 | d.ordinal(), k -> {
            List<BakedQuad> out = new ArrayList<>();
            float lo = 8 - CAP / 2, hi = 8 + CAP / 2;
            float[] from = {lo, lo, lo}, to = {hi, hi, hi};
            int a = d.getAxis().ordinal();
            if (d.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                from[a] = B;
                to[a] = B + CAP_DEPTH;
            } else {
                from[a] = A - CAP_DEPTH;
                to[a] = A;
            }
            box(out, from, to, sprite("block/pipe_tee"));
            return List.copyOf(out);
        });
    }

    /** The cap of a closed side of a bundle's pipe (on the manifold at the slot's spot towards {@code d}). */
    public static List<BakedQuad> bundleCap(int slot, Direction d) {
        return CACHE.computeIfAbsent(3L << 40 | (long) slot << 8 | d.ordinal(), k -> {
            List<BakedQuad> out = new ArrayList<>();
            float[] b = PipeBundleBlock.arm(slot, d, PipeBundleBlock.COLLAR, CAP_DEPTH, false);
            box(out, new float[]{b[0], b[1], b[2]}, new float[]{b[3], b[4], b[5]}, sprite("block/pipe_tee"));
            return List.copyOf(out);
        });
    }

    /**
     * Whether a closed side shows its cap: something is there the pipe would join (a pipe, a tank, a port or device of ours, a
     * block with a block entity such as a reservoir or another mod's tank). Read from the level while the chunk is meshed.
     */
    public static boolean capWanted(BlockAndTintGetter level, BlockPos pos, Direction d) {
        BlockPos n = pos.relative(d);
        BlockState s = level.getBlockState(n);
        if (s.isAir()) return false;
        if (Pipes.isPipe(s) || s.getBlock() instanceof TankBlock || s.getBlock() instanceof PipePort || s.getBlock() instanceof PipeConnectable) return true;
        return s.hasBlockEntity();
    }
}
