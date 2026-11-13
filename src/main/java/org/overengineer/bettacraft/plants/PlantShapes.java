package org.overengineer.bettacraft.plants;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Growth forms of the aquarium plants (M13): the blocks of a plant at every stage, computed from the species, a fixed
 * seed per plant, the interior scale and the room up to the water surface. Reproducible (the same plant always looks
 * the same) and every plant a little different. Each block has a birth stage, so a plant at stage n is exactly the
 * blocks born up to n and grows by adding blocks (trimming takes the newest off again).
 * <p>
 * Coordinates are relative to the root block (0, 0, 0), y up. Sizes are given for scale 16 (one interior block = one
 * pixel of the tank) and shrink with smaller scales. Stem plants and Vallisneria grow up to the water surface
 * ({@code headroom} blocks above the root are under water) and run along it; the others are scaled down when the
 * water is too shallow for them.
 */
public final class PlantShapes {
    /** One block of a plant: offset from the root, part and the stage it appears in (1..stages). */
    public record Voxel(int x, int y, int z, PlantPart part, int birth) {}

    /** All blocks of a full-grown plant (the root first) with their birth stages. */
    public static final class Shape {
        private final List<Voxel> voxels;
        private final int stages;

        Shape(List<Voxel> voxels, int stages) {
            this.voxels = Collections.unmodifiableList(voxels);
            this.stages = stages;
        }

        /** Every block of the full-grown plant. */
        public List<Voxel> all() {
            return voxels;
        }

        /** The blocks at a stage (stage 0 = only the cutting at the root, not part of this list). */
        public List<Voxel> at(int stage) {
            if (stage <= 0) return List.of();
            List<Voxel> out = new ArrayList<>();
            for (Voxel v : voxels) if (v.birth <= stage) out.add(v);
            return out;
        }

        /** Number of blocks at a stage (1 for the cutting). */
        public int count(int stage) {
            if (stage <= 0) return 1;
            int n = 0;
            for (Voxel v : voxels) if (v.birth <= stage) n++;
            return n;
        }

        public int stages() {
            return stages;
        }
    }

    private PlantShapes() {}

    /**
     * The shape of a plant.
     *
     * @param scale    interior scale of the tank (16, 8 or 4 interior blocks per tank block)
     * @param headroom interior blocks from the root up to the water surface (the root's layer counts); blocks are
     *                 kept below it
     */
    public static Shape of(PlantSpecies s, long seed, int scale, int headroom) {
        // M17: shapes are pure functions of these four values and immutable; removing a plant asked for the shapes of
        // all plants of the species once per removed block (thousands of random walks for a tall stem plant)
        Key key = new Key(s, seed, scale, headroom);
        synchronized (CACHE) {
            Shape cached = CACHE.get(key);
            if (cached != null) return cached;
        }
        Shape shape = build(s, seed, scale, headroom);
        synchronized (CACHE) {
            CACHE.put(key, shape);
        }
        return shape;
    }

    private record Key(PlantSpecies s, long seed, int scale, int headroom) {}

    /** At most this many shapes are kept (a few KB each). */
    static final int CACHE_SIZE = 1024;
    private static final java.util.LinkedHashMap<Key, Shape> CACHE = new java.util.LinkedHashMap<>(256, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<Key, Shape> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    static Shape build(PlantSpecies s, long seed, int scale, int headroom) {
        Builder b = new Builder(s, seed, Math.max(4, Math.min(16, scale)) / 16.0, Math.max(1, headroom));
        b.add(0, 0, 0, PlantPart.ROOT, 1);
        switch (s.form()) {
            case EPIPHYTE -> b.epiphyte();
            case MOSS -> b.moss();
            case ROSETTE -> b.rosette();
            case STEM -> b.stems();
            case RIBBON -> b.ribbons();
            case CARPET -> b.carpet();
            case BULB -> b.bulb();
        }
        return new Shape(new ArrayList<>(b.map.values()), s.stages());
    }

    private static final class Builder {
        final PlantSpecies s;
        final Random r;
        final double u;
        final int headroom, stages;
        /** Lowest y blocks may have (epiphytes and moss may hang a little below their root). */
        final int minY;
        final Long2ObjectLinkedOpenHashMap<Voxel> map = new Long2ObjectLinkedOpenHashMap<>();

        Builder(PlantSpecies s, long seed, double u, int headroom) {
            this.s = s;
            this.r = new Random(seed * 31 + s.ordinal() * 0x9E3779B97F4A7C15L);
            this.u = u;
            this.headroom = headroom;
            this.stages = s.stages();
            this.minY = s.form().rooted() ? 0 : -(int) Math.round(3 * u);
        }

        static long key(int x, int y, int z) {
            return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
        }

        /** Adds a block; an existing block keeps the earlier birth (and the root stays the root). */
        boolean add(int x, int y, int z, PlantPart part, int birth) {
            if (y < minY || y >= headroom) return false;
            birth = Math.max(1, Math.min(stages, birth));
            long k = key(x, y, z);
            Voxel old = map.get(k);
            if (old != null) {
                if (old.part == PlantPart.ROOT || old.birth <= birth) return true;
                map.put(k, new Voxel(x, y, z, old.part == PlantPart.STEM && part != PlantPart.STEM ? old.part : part, birth));
                return true;
            }
            map.put(k, new Voxel(x, y, z, part, birth));
            return true;
        }

        int len(double scale16) {
            return Math.max(1, (int) Math.round(scale16 * u));
        }

        int between(int a, int b) {
            return a + r.nextInt(b - a + 1);
        }

        double betweenD(double a, double b) {
            return a + r.nextDouble() * (b - a);
        }

        /** Room factor: shrinks plants that would not fit under the surface. */
        double fit(double nominalHeight) {
            double room = headroom - 1;
            return Math.max(0.35, Math.min(1.0, room / Math.max(1, nominalHeight * u)));
        }

        // ------------------------------------------------------------------ leaves

        /**
         * A leaf from (x0, y0, z0): a stalk of {@code stalk} blocks rising at {@code stalkElev} degrees, then a blade of
         * {@code blade} blocks whose elevation goes from {@code elev0} to {@code elev1} (arching) and whose half width
         * swells to {@code halfW} in the middle. Born from stage b0 (base) to b1 (tip). Returns false if nothing fit.
         */
        void leaf(double x0, double y0, double z0, double yaw, int stalk, double stalkElev, int blade, double elev0, double elev1,
                  int halfW, int b0, int b1, boolean wavy) {
            double x = x0, y = y0, z = z0;
            double cy = Math.cos(yaw), sy = Math.sin(yaw);
            int total = stalk + blade;
            int i = 0;
            int wave = r.nextInt(2);
            double step = 0.5;
            for (double d = 0; d < stalk; d += step, i++) {
                double e = Math.toRadians(stalkElev);
                x += cy * Math.cos(e) * step;
                z += sy * Math.cos(e) * step;
                y += Math.sin(e) * step;
                if (!add((int) Math.round(x), (int) Math.round(y), (int) Math.round(z), PlantPart.STEM, birth(b0, b1, d / total))) return;
            }
            double px = -sy, pz = cy; // horizontal perpendicular
            for (double d = 0; d < blade; d += step) {
                double t = d / Math.max(1, blade);
                double e = Math.toRadians(elev0 + (elev1 - elev0) * t * t);
                x += cy * Math.cos(e) * step;
                z += sy * Math.cos(e) * step;
                y += Math.sin(e) * step;
                int bx = (int) Math.round(x), by = (int) Math.round(y), bz = (int) Math.round(z);
                int born = birth(b0, b1, (stalk + d) / total);
                PlantPart part = t > 0.8 ? PlantPart.TOP : PlantPart.LEAF;
                if (!add(bx, by, bz, part, born)) return;
                double swell = Math.sin(Math.PI * Math.pow(Math.min(1, t * 1.1), 0.8));
                int w = (int) Math.round(halfW * swell);
                for (int k = 1; k <= w; k++) {
                    int wy = wavy && ((int) (d * 2) + wave + k) % 3 == 0 ? 1 : 0;
                    add((int) Math.round(x + px * k), by + wy, (int) Math.round(z + pz * k), part, born);
                    add((int) Math.round(x - px * k), by + wy, (int) Math.round(z - pz * k), part, born);
                }
            }
        }

        int birth(int b0, int b1, double t) {
            return b0 + (int) Math.floor(Math.max(0, Math.min(0.999, t)) * (b1 - b0 + 1));
        }

        // ------------------------------------------------------------------ forms

        void epiphyte() {
            int rhiz, leaves, stalkA, stalkB, bladeA, bladeB, halfW;
            double stalkElevA, stalkElevB, e0A, e0B, droop, nominal;
            boolean wavy = s.leafTexture().equals("wavy");
            switch (s) {
                case JAVA_FERN -> {
                    rhiz = between(3, 5); leaves = between(6, 9); stalkA = 2; stalkB = 3; bladeA = 16; bladeB = 26; halfW = 1;
                    stalkElevA = 65; stalkElevB = 82; e0A = 62; e0B = 80; droop = 30; nominal = 24;
                }
                case ANUBIAS_NANA -> {
                    rhiz = between(3, 5); leaves = between(5, 8); stalkA = 2; stalkB = 4; bladeA = 4; bladeB = 6; halfW = 2;
                    stalkElevA = 55; stalkElevB = 75; e0A = 18; e0B = 35; droop = 10; nominal = 8;
                }
                case BUCE_MINI_NEEDLE -> {
                    rhiz = between(2, 3); leaves = between(8, 12); stalkA = 1; stalkB = 1; bladeA = 3; bladeB = 5; halfW = 0;
                    stalkElevA = 50; stalkElevB = 80; e0A = 45; e0B = 75; droop = 15; nominal = 6;
                }
                default -> { // the wavy bucephalandras
                    rhiz = between(2, 3); leaves = between(7, 11); stalkA = 1; stalkB = 2; bladeA = 3; bladeB = 5; halfW = 1;
                    stalkElevA = 40; stalkElevB = 75; e0A = 25; e0B = 55; droop = 15; nominal = 7;
                }
            }
            double f = fit(nominal);
            int[] dir = DIRS[r.nextInt(4)];
            int rl = Math.max(1, len(rhiz));
            int half = Math.max(1, stages / 2);
            for (int i = 1; i < rl; i++) add(dir[0] * i, 0, dir[1] * i, PlantPart.RHIZOME, 1 + i * half / rl);
            for (int k = 0; k < leaves; k++) {
                int at = k % rl;
                // a leaf starts once the rhizome reached its place
                int b0 = Math.max(1 + k * (stages - 1) / leaves, at == 0 ? 1 : 1 + at * half / rl);
                int b1 = Math.min(stages, b0 + (s == PlantSpecies.JAVA_FERN ? 3 : 2));
                double yaw = r.nextDouble() * Math.PI * 2;
                leaf(dir[0] * at, 0, dir[1] * at, yaw, Math.max(1, (int) Math.round(len(between(stalkA, stalkB)) * f)),
                        betweenD(stalkElevA, stalkElevB), Math.max(2, (int) Math.round(len(between(bladeA, bladeB)) * f)),
                        e0A + r.nextDouble() * (e0B - e0A), e0A - droop, halfW > 1 && u < 0.75 ? 1 : (u < 0.5 ? 0 : halfW), b0, b1, wavy);
            }
        }

        void moss() {
            int n = Math.max(6, (int) Math.round(110 * u * u));
            int maxR = Math.max(2, len(7)), maxUp = Math.max(1, len(4));
            List<int[]> cells = new ArrayList<>();
            cells.add(new int[]{0, 0, 0});
            int tries = 0;
            while (cells.size() < n && tries++ < n * 40) {
                int[] c = cells.get(r.nextInt(cells.size()));
                int roll = r.nextInt(14);
                int dx = 0, dy = 0, dz = 0;
                if (roll < 12) {
                    int[] d = DIRS[roll % 4];
                    dx = d[0];
                    dz = d[1];
                } else if (roll < 13) dy = 1;
                else dy = -1;
                int x = c[0] + dx, y = c[1] + dy, z = c[2] + dz;
                if (x * x + z * z > maxR * maxR || y > maxUp || y < minY) continue;
                // denser near the root, flatter further out
                if (y > 0 && x * x + z * z > (maxR * maxR) / 2) continue;
                if (map.containsKey(key(x, y, z))) continue;
                int born = 1 + cells.size() * stages / n;
                if (add(x, y, z, PlantPart.MOSS, born)) cells.add(new int[]{x, y, z});
            }
        }

        void rosette() {
            int leaves, stalkA, stalkB, bladeA, bladeB, halfW;
            double stalkElev, e0, e1, nominal;
            switch (s) {
                case AMAZON_SWORD -> {
                    leaves = between(9, 13); stalkA = 5; stalkB = 9; bladeA = 9; bladeB = 14; halfW = 2;
                    stalkElev = 76; e0 = 52; e1 = 12; nominal = 22;
                }
                case CRYPT_FLAMINGO -> {
                    leaves = between(6, 10); stalkA = 2; stalkB = 3; bladeA = 4; bladeB = 6; halfW = 1;
                    stalkElev = 80; e0 = 62; e1 = 25; nominal = 10;
                }
                default -> {
                    leaves = between(7, 12); stalkA = 2; stalkB = 4; bladeA = 4; bladeB = 7; halfW = 1;
                    stalkElev = 80; e0 = 62; e1 = 30; nominal = 12;
                }
            }
            double f = fit(nominal);
            boolean wavy = s.leafTexture().equals("wavy");
            double yaw0 = r.nextDouble() * Math.PI * 2;
            for (int k = 0; k < leaves; k++) {
                int b0 = 1 + k * Math.max(1, stages - 2) / leaves;
                int b1 = Math.min(stages, b0 + 2);
                double yaw = yaw0 + k * 2.39996 + (r.nextDouble() - 0.5) * 0.4; // golden angle: leaves all around
                leaf(0, 0, 0, yaw, Math.max(1, (int) Math.round(len(between(stalkA, stalkB)) * f)), stalkElev - r.nextDouble() * 8,
                        Math.max(2, (int) Math.round(len(between(bladeA, bladeB)) * f)), e0 + (r.nextDouble() - 0.5) * 16, e1,
                        u < 0.75 ? Math.min(1, halfW) : halfW, b0, b1, wavy);
            }
        }

        void stems() {
            int count = s == PlantSpecies.ROTALA ? between(3, 4) : between(2, 3);
            int nodeEvery = s == PlantSpecies.ROTALA ? 1 : 2;
            int top = headroom - 1;
            List<int[]> bases = new ArrayList<>();
            bases.add(new int[]{0, 0});
            List<int[]> around = new ArrayList<>(List.of(NEIGHBOURS));
            Collections.shuffle(around, r);
            for (int i = 1; i < count && i <= around.size(); i++) bases.add(around.get(i - 1));
            for (int si = 0; si < bases.size(); si++) {
                int[] base = bases.get(si);
                int b0 = 1 + Math.min(si, 2);
                int height = Math.max(2, top - (si == 0 ? 0 : r.nextInt(Math.max(1, top / 4) + 1)));
                int[] lean = DIRS[r.nextInt(4)];
                int leanEvery = between(5, 9);
                int x = base[0], z = base[1];
                int axis = r.nextInt(2);
                int last = stages - (si < 2 ? 1 : 0);
                for (int h = 0; h <= height; h++) {
                    if (h > 0 && h % leanEvery == 0) {
                        x += lean[0];
                        z += lean[1];
                    }
                    int born = birth(b0, last, h / (double) (height + 1));
                    boolean tip = h >= height - 2;
                    if (h > 0 || si > 0) add(x, h, z, tip ? PlantPart.TOP : PlantPart.STEM, born);
                    if (h >= 1 && h % nodeEvery == 0) {
                        int[] side = (axis + h / nodeEvery) % 2 == 0 ? new int[]{1, 0} : new int[]{0, 1};
                        PlantPart leafPart = h >= height - 3 ? PlantPart.TOP : PlantPart.LEAF;
                        add(x + side[0], h, z + side[1], leafPart, born);
                        add(x - side[0], h, z - side[1], leafPart, born);
                        if (s != PlantSpecies.ROTALA && u >= 1 && h % 4 == 0) {
                            add(x + side[0] * 2, h + 1, z + side[1] * 2, leafPart, born);
                            add(x - side[0] * 2, h + 1, z - side[1] * 2, leafPart, born);
                        }
                    }
                }
                // at the surface the tops run along it (they shade the tank below)
                if (height >= top - 1 && si < 2) {
                    int run = len(between(2, 5));
                    int[] d = DIRS[r.nextInt(4)];
                    for (int k = 1; k <= run; k++) {
                        add(x + d[0] * k, height, z + d[1] * k, PlantPart.TOP, stages);
                        add(x + d[0] * k + d[1], height, z + d[1] * k + d[0], PlantPart.TOP, stages);
                    }
                }
            }
        }

        void ribbons() {
            int count = between(4, 7);
            int top = headroom - 1;
            List<int[]> bases = new ArrayList<>();
            bases.add(new int[]{0, 0});
            List<int[]> around = new ArrayList<>(List.of(NEIGHBOURS));
            Collections.shuffle(around, r);
            for (int i = 1; i < count && i <= around.size(); i++) bases.add(around.get(i - 1));
            for (int k = 0; k < bases.size(); k++) {
                int[] base = bases.get(k);
                int b0 = 1 + k * Math.max(1, stages - 3) / bases.size();
                int height = Math.max(2, top - r.nextInt(Math.max(1, top / 5) + 1));
                int[] d = DIRS[r.nextInt(4)];
                int swayEvery = between(4, 7);
                int run = height >= top - 1 ? len(between(2, 6)) : 0;
                int total = height + 1 + run;
                int x = base[0], z = base[1], n = 0;
                for (int h = 0; h <= height; h++, n++) {
                    if (h > 0 && h % swayEvery == 0) {
                        x += d[0];
                        z += d[1];
                    }
                    if (h > 0 || k > 0) add(x, h, z, n >= total - 2 ? PlantPart.TOP : PlantPart.RIBBON, birth(b0, stages, n / (double) total));
                }
                for (int j = 1; j <= run; j++, n++) {
                    add(x + d[0] * j, height, z + d[1] * j, n >= total - 2 ? PlantPart.TOP : PlantPart.RIBBON, birth(b0, stages, n / (double) total));
                }
            }
        }

        void carpet() {
            int n = Math.max(4, (int) Math.round(42 * u * u));
            int maxR = Math.max(1, len(4));
            List<int[]> cells = new ArrayList<>();
            cells.add(new int[]{0, 0});
            int tries = 0;
            while (cells.size() < n && tries++ < n * 40) {
                int[] c = cells.get(r.nextInt(cells.size()));
                int[] d = DIRS[r.nextInt(4)];
                int x = c[0] + d[0], z = c[1] + d[1];
                if (x * x + z * z > maxR * maxR + 1 || map.containsKey(key(x, 0, z))) continue;
                if (add(x, 0, z, PlantPart.MAT, 1 + cells.size() * stages / n)) cells.add(new int[]{x, z});
            }
        }

        void bulb() {
            double f = fit(9);
            int leaves = between(4, 6);
            for (int k = 0; k < leaves; k++) {
                int b0 = 1 + k * Math.max(1, stages - 3) / leaves;
                int b1 = Math.min(stages, b0 + 2);
                double yaw = r.nextDouble() * Math.PI * 2;
                leaf(0, 0, 0, yaw, Math.max(1, (int) Math.round(len(between(3, 6)) * f)), betweenD(55, 75),
                        Math.max(2, (int) Math.round(len(between(4, 6)) * f)), betweenD(0, 14), -6, u < 0.75 ? 1 : 2, b0, b1, false);
            }
            int top = headroom - 1;
            if (top >= len(8)) {
                int pads = between(1, 2);
                for (int p = 0; p < pads; p++) {
                    int[] d = DIRS[r.nextInt(4)];
                    int born = stages - pads + 1 + p;
                    int x = d[0], z = d[1];
                    for (int h = 0; h < top; h++) {
                        if (h > 0 && h % 6 == 0) {
                            x += d[0];
                            z += d[1];
                        }
                        add(x, h, z, PlantPart.STEM, born - 1);
                    }
                    int rad = Math.max(1, len(2.4));
                    for (int dx = -rad; dx <= rad; dx++)
                        for (int dz = -rad; dz <= rad; dz++) {
                            if (dx * dx + dz * dz > rad * rad + 0.6) continue;
                            add(x + dx, top, z + dz, PlantPart.PAD, born);
                        }
                }
            }
        }
    }

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
}
