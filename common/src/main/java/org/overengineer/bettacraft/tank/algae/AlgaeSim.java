package org.overengineer.bettacraft.tank.algae;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * Green algae on the inside of the glass (M14), one value 0..1 per glass face of a tank block (the four sides; the
 * bottom is under the substrate, the top is the surface). They grow with light, waste and time on the faces under water,
 * the cleaning crew (snails, nerites best, and shrimp) grazes them off, the algae scraper cleans a face at once. Pure
 * logic (unit-tested); the levels live in {@code TankData#algae()}, keyed by block position, four floats (south, west,
 * north, east = 2D data values).
 */
public final class AlgaeSim {
    /** Visible stages 0..4 (0 = clean). */
    public static final int STAGES = 4;

    private AlgaeSim() {}

    /** One glass face: block and 2D direction. */
    public record Face(long pos, int dir) {}

    public static int stepX(int dir) {
        return dir == 1 ? -1 : dir == 3 ? 1 : 0;
    }

    public static int stepZ(int dir) {
        return dir == 0 ? 1 : dir == 2 ? -1 : 0;
    }

    /** The glass faces of the tank's side walls (no tank block behind them), in the layers up to {@code topLayer}. */
    public static List<Face> faces(LongSet members, int topLayer) {
        List<Face> out = new ArrayList<>();
        for (long l : members) {
            int x = BlockPos.getX(l), y = BlockPos.getY(l), z = BlockPos.getZ(l);
            if (y > topLayer) continue;
            for (int d = 0; d < 4; d++) {
                if (!members.contains(BlockPos.asLong(x + stepX(d), y, z + stepZ(d)))) out.add(new Face(l, d));
            }
        }
        return out;
    }

    public static float get(Long2ObjectMap<float[]> algae, long pos, int dir) {
        float[] a = algae.get(pos);
        return a == null ? 0 : a[dir];
    }

    public static void set(Long2ObjectMap<float[]> algae, long pos, int dir, float v) {
        v = Math.max(0, Math.min(1, v));
        float[] a = algae.get(pos);
        if (a == null) {
            if (v <= 0) return;
            a = new float[4];
            algae.put(pos, a);
        }
        a[dir] = v;
        if (a[0] <= 0 && a[1] <= 0 && a[2] <= 0 && a[3] <= 0) algae.remove(pos);
    }

    /** Growth per face and day: speed x light x nutrients (waste) x shade of the plants (0 in the dark). */
    public static double growthPerDay(double speed, int light, float quality, boolean qualityEnabled, double plantShare, double floatingCover) {
        if (speed <= 0 || light < 7) return 0;
        double l = Math.min(1, (light - 6) / 9.0);
        double waste = qualityEnabled ? 1 - quality : 0.3;
        double nutrients = 0.35 + 0.65 * Math.max(0, Math.min(1, waste * 2));
        double plants = 1 - Math.min(0.5, plantShare * 6);
        double shade = 1 - 0.5 * Math.max(0, Math.min(1, floatingCover));
        return speed * l * nutrients * plants * shade;
    }

    /**
     * Grows the algae on {@code faces} by {@code perDay * days}, then the grazers eat up to {@code grazePerDay * days}
     * face-units (spread over the faces by how much each has). Returns the face-units eaten.
     */
    public static double step(Long2ObjectMap<float[]> algae, List<Face> faces, double perDay, double grazePerDay, double days) {
        if (faces.isEmpty() || days <= 0) return 0;
        double grow = perDay * days;
        if (grow > 0) for (Face f : faces) set(algae, f.pos(), f.dir(), get(algae, f.pos(), f.dir()) + (float) grow);
        double budget = grazePerDay * days;
        if (budget <= 0) return 0;
        double total = 0;
        for (Face f : faces) total += get(algae, f.pos(), f.dir());
        if (total <= 0) return 0;
        double eaten = Math.min(total, budget);
        double k = eaten / total;
        for (Face f : faces) {
            float v = get(algae, f.pos(), f.dir());
            if (v > 0) set(algae, f.pos(), f.dir(), (float) (v - v * k));
        }
        return eaten;
    }

    /** Visible stage of an algae level: 0 clean .. 4 green. */
    public static int stage(float level) {
        if (level < 0.12F) return 0;
        if (level < 0.35F) return 1;
        if (level < 0.6F) return 2;
        if (level < 0.85F) return 3;
        return 4;
    }

    /** Mean algae level of the faces (0..1, menu and overlays). */
    public static double mean(Long2ObjectMap<float[]> algae, List<Face> faces) {
        if (faces.isEmpty()) return 0;
        double s = 0;
        for (Face f : faces) s += get(algae, f.pos(), f.dir());
        return s / faces.size();
    }

    /** Client format: the faces with a visible stage (block relative to the tank min, direction, stage). */
    public static byte[] encode(Long2ObjectMap<float[]> algae, int minX, int minY, int minZ) {
        List<long[]> rows = new ArrayList<>();
        for (var e : algae.long2ObjectEntrySet()) {
            for (int d = 0; d < 4; d++) {
                int st = stage(e.getValue()[d]);
                if (st > 0) rows.add(new long[]{e.getLongKey(), d, st});
            }
        }
        ByteBuffer b = ByteBuffer.allocate(4 + rows.size() * 5);
        b.putInt(rows.size());
        for (long[] r : rows) {
            long p = r[0];
            b.put((byte) (BlockPos.getX(p) - minX));
            b.put((byte) (BlockPos.getY(p) - minY));
            b.put((byte) (BlockPos.getZ(p) - minZ));
            b.put((byte) r[1]);
            b.put((byte) r[2]);
        }
        return b.array();
    }

    /** Client: stage per face (stage / STAGES as the level). */
    public static Long2ObjectOpenHashMap<float[]> decode(byte[] data, int minX, int minY, int minZ) {
        Long2ObjectOpenHashMap<float[]> out = new Long2ObjectOpenHashMap<>();
        ByteBuffer b = ByteBuffer.wrap(data);
        try {
            int n = b.getInt();
            for (int i = 0; i < n; i++) {
                int x = minX + (b.get() & 0xFF), y = minY + (b.get() & 0xFF), z = minZ + (b.get() & 0xFF);
                int d = b.get() & 3, st = b.get();
                out.computeIfAbsent(BlockPos.asLong(x, y, z), k -> new float[4])[d] = st / (float) STAGES;
            }
        } catch (java.nio.BufferUnderflowException e) {
            // truncated
        }
        return out;
    }
}
