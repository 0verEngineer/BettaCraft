package org.overengineer.bettacraft.tank.interior;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synthetic cost estimate for interior miniatures (M0 spike, PLAN 12): builds an in-memory interior for a tank of
 * {@code tx*ty*tz} blocks at a given scale, encodes it like the real sync and estimates the mesh size
 * (visible faces after culling, as the BlockRenderDispatcher with side culling would emit).
 */
public final class InteriorBench {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/interior-bench");

    private static final BlockState[] STATES = {
            Blocks.AIR.defaultBlockState(), Blocks.SAND.defaultBlockState(), Blocks.STONE.defaultBlockState(),
            Blocks.SEAGRASS.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState()
    };
    private static final int SAND = 1, STONE = 2, PLANT = 3, NOISE = 4;

    public record Result(String scene, int scale, int sizeX, int sizeY, int sizeZ, int nonAir, int sections, int bytes,
                         double encodeMs, long quads, double genMs) {
        public long meshBytes() {
            return quads * 4 * 32; // DefaultVertexFormat.BLOCK: 32 bytes per vertex
        }

        @Override
        public String toString() {
            return String.format("%s scale %d (%dx%dx%d): %,d blocks, %,d sections, sync %,d KiB (encode %.1f ms), est. %,d quads = %,d KiB VRAM",
                    scene, scale, sizeX, sizeY, sizeZ, nonAir, sections, bytes / 1024, encodeMs, quads, meshBytes() / 1024);
        }
    }

    private InteriorBench() {}

    /** Running real-region benchmark (async: the region shell is prepared like for a player). */
    private static final class RealBench {
        final java.util.function.Consumer<String> out;
        final int[] scales = {16, 8};
        int index = -1;
        InteriorRegion region;
        final List<String> lines = new ArrayList<>();

        RealBench(java.util.function.Consumer<String> out) {
            this.out = out;
        }
    }

    private static RealBench running;

    public static boolean realRunning() {
        return running != null;
    }

    /**
     * Allocates real regions for a 16x16x16 tank at scale 16 and 8 in the interior dimension and prepares them
     * asynchronously (chunk generation on the world-gen workers, shell written within a tick budget), then measures
     * sample decoration sync (raw and zlib-compressed size), a one-block delta, and removes them again. Results go to
     * {@code out} and the log; progress continues in {@link #tick}.
     */
    public static boolean startRealRegions(net.minecraft.server.MinecraftServer server, java.util.function.Consumer<String> out) {
        if (running != null || InteriorManager.interiorLevel(server) == null) return false;
        running = new RealBench(out);
        next(server);
        return true;
    }

    private static void next(net.minecraft.server.MinecraftServer server) {
        RealBench b = running;
        b.index++;
        if (b.index >= b.scales.length) {
            running = null;
            return;
        }
        int scale = b.scales[b.index], n = 16 * scale;
        b.region = InteriorRegions.get(server).allocate(n, n, n, scale, net.minecraft.world.level.Level.OVERWORLD,
                net.minecraft.core.BlockPos.ZERO, null, null);
        InteriorPreparation.prepare(server, b.region, null);
    }

    /** Server tick (after {@link InteriorPreparation#tick}). */
    public static void tick(net.minecraft.server.MinecraftServer server) {
        RealBench b = running;
        if (b == null || b.region == null || !b.region.built()) return;
        net.minecraft.server.level.ServerLevel interior = InteriorManager.interiorLevel(server);
        InteriorRegion r = b.region;
        int n = r.sizeX(), scale = r.scale();
        String prep = InteriorPreparation.lastStats;
        it.unimi.dsi.fastutil.longs.LongOpenHashSet touched = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        InteriorWorldOps.fill(interior, r.originX(), InteriorRegion.BASE_Y, r.originZ(), r.originX() + n - 1, InteriorRegion.BASE_Y,
                r.originZ() + n - 1, Blocks.SAND.defaultBlockState(), touched);
        for (int i = 0; i < 200; i++) {
            int x = (i * 37) % n, z = (i * 91) % n;
            InteriorWorldOps.fill(interior, r.originX() + x, InteriorRegion.BASE_Y + 1, r.originZ() + z, r.originX() + x,
                    InteriorRegion.BASE_Y + scale * 3 - 1, r.originZ() + z, Blocks.SEAGRASS.defaultBlockState(), touched);
        }
        InteriorSnapshot snap = new InteriorSnapshot();
        long t1 = System.nanoTime();
        InteriorSync.encodeRegion(interior, r, snap, Integer.MAX_VALUE);
        double encMs = (System.nanoTime() - t1) / 1e6;
        InteriorSnapshot.Stats st = snap.stats();
        int zipped = InteriorSnapshot.deflatedSize(snap.bytes());
        // one changed block -> delta of one section
        InteriorSync.markAllDirty(r.id());
        InteriorSync.prepareFull(server, r.id());
        interior.setBlockAndUpdate(r.toWorld(n / 2, 2, n / 2), Blocks.STONE.defaultBlockState());
        InteriorSync.flush(server);
        String line = String.format(java.util.Locale.ROOT,
                "real region 16x16x16 tank scale %d (%d^3): prepare %s; sample deco %,d blocks -> full sync %,d KiB (zlib %,d KiB) in %d sections, "
                        + "scan+encode %.1f ms; one-block delta %d bytes",
                scale, n, prep, st.nonAir(), st.bytes() / 1024, zipped / 1024, st.sections(), encMs, InteriorSync.lastDeltaBytes);
        LOGGER.info("[bench] {}", line);
        b.lines.add(line);
        b.out.accept(line);
        InteriorManager.destroyRegionForBench(server, r);
        b.region = null;
        next(server);
    }

    public static List<Result> runDefault() {
        List<Result> out = new ArrayList<>();
        for (int scale : new int[]{16, 8}) {
            out.add(run("typical", 16, 16, 16, scale));
            out.add(run("heavy", 16, 16, 16, scale));
        }
        for (Result r : out) LOGGER.info("[bench] {}", r);
        return out;
    }

    /**
     * typical: substrate 1/16 of the height, 40 rock blobs, 300 plant columns (tank-relative, so comparable across scales).
     * heavy: typical + 10% random fill of the whole volume (stress case).
     */
    public static Result run(String scene, int tx, int ty, int tz, int scale) {
        long t0 = System.nanoTime();
        int sx = tx * scale, sy = ty * scale, sz = tz * scale;
        byte[] v = new byte[sx * sy * sz];
        RandomSource rnd = RandomSource.create(1234);
        int substrate = Math.max(1, sy / 16);
        for (int y = 0; y < substrate; y++)
            for (int z = 0; z < sz; z++)
                for (int x = 0; x < sx; x++) v[(y * sz + z) * sx + x] = SAND;
        for (int i = 0; i < 40; i++) {
            int cx = rnd.nextInt(sx), cz = rnd.nextInt(sz), cy = substrate;
            double r = scale * (0.5 + rnd.nextDouble());
            int ri = (int) Math.ceil(r);
            for (int y = Math.max(0, cy - ri); y < Math.min(sy, cy + ri); y++)
                for (int z = Math.max(0, cz - ri); z < Math.min(sz, cz + ri); z++)
                    for (int x = Math.max(0, cx - ri); x < Math.min(sx, cx + ri); x++) {
                        double dx = x - cx, dy = (y - cy) * 1.4, dz = z - cz;
                        if (dx * dx + dy * dy + dz * dz <= r * r) v[(y * sz + z) * sx + x] = STONE;
                    }
        }
        for (int i = 0; i < 300; i++) {
            int x = rnd.nextInt(sx), z = rnd.nextInt(sz);
            int h = scale * (2 + rnd.nextInt(5));
            for (int y = substrate; y < Math.min(sy, substrate + h); y++) {
                int idx = (y * sz + z) * sx + x;
                if (v[idx] == 0) v[idx] = PLANT;
            }
        }
        if (scene.equals("heavy")) {
            for (int i = 0; i < v.length; i++) if (v[i] == 0 && rnd.nextInt(10) == 0) v[i] = NOISE;
        }
        double genMs = (System.nanoTime() - t0) / 1e6;

        // encode exactly like the network sync
        InteriorSnapshot snap = new InteriorSnapshot();
        BlockState[] buf = new BlockState[InteriorSnapshot.SECTION_VOLUME];
        for (int csy = 0; csy < (sy + 15) / 16; csy++)
            for (int csz = 0; csz < (sz + 15) / 16; csz++)
                for (int csx = 0; csx < (sx + 15) / 16; csx++) {
                    java.util.Arrays.fill(buf, null);
                    for (int y = 0; y < 16; y++)
                        for (int z = 0; z < 16; z++)
                            for (int x = 0; x < 16; x++) {
                                int wx = csx * 16 + x, wy = csy * 16 + y, wz = csz * 16 + z;
                                if (wx >= sx || wy >= sy || wz >= sz) continue;
                                byte b = v[(wy * sz + wz) * sx + wx];
                                if (b != 0) buf[InteriorSnapshot.index(x, y, z)] = STATES[b];
                            }
                    snap.addSection(csx, csy, csz, buf);
                }
        InteriorSnapshot.Stats st = snap.stats();

        // estimate quads: full cubes emit a face where the neighbour is not a full cube; plants = 4 quads (cross model)
        long quads = 0;
        for (int y = 0; y < sy; y++)
            for (int z = 0; z < sz; z++)
                for (int x = 0; x < sx; x++) {
                    byte b = v[(y * sz + z) * sx + x];
                    if (b == 0) continue;
                    if (b == PLANT) {
                        quads += 4;
                        continue;
                    }
                    quads += open(v, sx, sy, sz, x + 1, y, z) + open(v, sx, sy, sz, x - 1, y, z)
                            + open(v, sx, sy, sz, x, y + 1, z) + open(v, sx, sy, sz, x, y - 1, z)
                            + open(v, sx, sy, sz, x, y, z + 1) + open(v, sx, sy, sz, x, y, z - 1);
                }
        return new Result(scene, scale, sx, sy, sz, st.nonAir(), st.sections(), st.bytes(), st.encodeNanos() / 1e6, quads, genMs);
    }

    private static int open(byte[] v, int sx, int sy, int sz, int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= sx || y >= sy || z >= sz) return 1;
        byte b = v[(y * sz + z) * sx + x];
        return (b == 0 || b == PLANT) ? 1 : 0;
    }
}
