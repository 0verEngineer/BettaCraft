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
        return of(s, seed, scale, headroom, VERSION);
    }

    /**
     * Version of the generator a new plant gets (stored per plant, {@link InteriorPlant#shapeVersion}): 1 = up to M22,
     * 2 = M22.1 (Vallisneria keeps its ribbons apart). A plant keeps the version it started with, so its blocks never
     * change shape in an existing world.
     */
    public static final int VERSION = 2;

    /** The shape of a plant with the generator of {@code version} (older plants keep theirs). */
    public static Shape of(PlantSpecies s, long seed, int scale, int headroom, int version) {
        // only the Vallisneria changed with version 2
        int v = s.form() == PlantSpecies.Form.RIBBON ? Math.max(1, Math.min(VERSION, version)) : 1;
        // M17: shapes are pure functions of these values and immutable; removing a plant asked for the shapes of
        // all plants of the species once per removed block (thousands of random walks for a tall stem plant)
        Key key = new Key(s, seed, scale, headroom, v);
        synchronized (CACHE) {
            Shape cached = CACHE.get(key);
            if (cached != null) return cached;
        }
        Shape shape = build(s, seed, scale, headroom, v);
        synchronized (CACHE) {
            CACHE.put(key, shape);
        }
        return shape;
    }

    private record Key(PlantSpecies s, long seed, int scale, int headroom, int version) {}

    /** At most this many shapes are kept (a few KB each). */
    static final int CACHE_SIZE = 1024;
    private static final java.util.LinkedHashMap<Key, Shape> CACHE = new java.util.LinkedHashMap<>(256, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<Key, Shape> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    static Shape build(PlantSpecies s, long seed, int scale, int headroom, int version) {
        Builder b = new Builder(s, seed, Math.max(4, Math.min(16, scale)) / 16.0, Math.max(1, headroom));
        b.version = version;
        b.add(0, 0, 0, PlantPart.ROOT, 1);
        switch (s.form()) {
            case EPIPHYTE -> b.epiphyte();
            case MOSS -> b.moss();
            case ROSETTE -> b.rosette();
            case STEM -> b.stems();
            case RIBBON -> b.ribbons();
            case CARPET -> b.carpet();
            case BULB -> b.bulb();
            case POLYPS -> b.polyps();
            case DISC -> b.discs();
            case TOADSTOOL -> b.toadstool();
            case BRANCH -> b.branches();
            case DOME -> b.dome();
            case ANEMONE -> b.anemone();
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
        int version = VERSION;

        Builder(PlantSpecies s, long seed, double u, int headroom) {
            this.s = s;
            this.r = new Random(seed * 31 + s.ordinal() * 0x9E3779B97F4A7C15L);
            this.u = u;
            this.headroom = headroom;
            this.stages = s.stages();
            // M20: corals sit on their rock (only the polyp mats creep a little down its sides)
            this.minY = s.form().rooted() || (s.marine() && s.form() != PlantSpecies.Form.POLYPS) ? 0 : -(int) Math.round(3 * u);
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
                case ANUBIAS_NANA, ANUBIAS_PINTO, ANUBIAS_WHITE -> {
                    rhiz = between(3, 5); leaves = between(5, 8); stalkA = 2; stalkB = 4; bladeA = 4; bladeB = 6; halfW = 2;
                    stalkElevA = 55; stalkElevB = 75; e0A = 18; e0B = 35; droop = 10; nominal = 8;
                }
                // M28: the dwarf "Petite", the larger coffeefolia and "Round Leaf", the Bucephalandras by size
                case ANUBIAS_PETITE -> {
                    rhiz = between(2, 3); leaves = between(6, 9); stalkA = 1; stalkB = 2; bladeA = 2; bladeB = 4; halfW = 1;
                    stalkElevA = 50; stalkElevB = 70; e0A = 15; e0B = 30; droop = 8; nominal = 5;
                }
                case ANUBIAS_COFFEEFOLIA -> {
                    rhiz = between(3, 5); leaves = between(5, 8); stalkA = 3; stalkB = 5; bladeA = 5; bladeB = 8; halfW = 2;
                    stalkElevA = 60; stalkElevB = 80; e0A = 25; e0B = 45; droop = 15; nominal = 11;
                }
                case ANUBIAS_ROUND_LEAF -> {
                    rhiz = between(3, 5); leaves = between(5, 7); stalkA = 3; stalkB = 5; bladeA = 5; bladeB = 7; halfW = 2;
                    stalkElevA = 55; stalkElevB = 75; e0A = 15; e0B = 30; droop = 10; nominal = 11;
                }
                case BUCE_KEDAGANG -> {
                    rhiz = between(2, 3); leaves = between(7, 10); stalkA = 1; stalkB = 2; bladeA = 4; bladeB = 6; halfW = 1;
                    stalkElevA = 45; stalkElevB = 75; e0A = 30; e0B = 60; droop = 15; nominal = 8;
                }
                case BUCE_GODZILLA -> {
                    rhiz = between(3, 4); leaves = between(7, 10); stalkA = 1; stalkB = 2; bladeA = 4; bladeB = 7; halfW = 1;
                    stalkElevA = 40; stalkElevB = 70; e0A = 25; e0B = 50; droop = 18; nominal = 9;
                }
                case BUCE_MINI_PINK -> {
                    rhiz = 2; leaves = between(8, 12); stalkA = 1; stalkB = 1; bladeA = 2; bladeB = 3; halfW = 1;
                    stalkElevA = 40; stalkElevB = 75; e0A = 25; e0B = 55; droop = 12; nominal = 5;
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
            // M28: Downoi stays a low bush of many short shoots; the fine-leaved stem plants carry leaves on every node
            boolean bush = s == PlantSpecies.POGOSTEMON_DOWNOI;
            int count = s == PlantSpecies.ROTALA || bush ? between(3, 4) : between(2, 3);
            int nodeEvery = switch (s) {
                case ROTALA, ROTALA_HRA, ROTALA_WALLICHII, LIMNOPHILA_SESSILIFLORA, MYRIOPHYLLUM_RED, POGOSTEMON_DOWNOI -> 1;
                default -> 2;
            };
            int top = bush ? Math.min(headroom - 1, Math.max(3, len(10))) : headroom - 1;
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
                if (!bush && height >= top - 1 && si < 2) {
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
            if (version >= 2) {
                ribbonsApart();
                return;
            }
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

        /**
         * M22.1 (version 2): like {@link #ribbons} (the same random numbers in the same order), but every ribbon keeps out
         * of the blocks of the ribbons before it: a lean into a taken column turns to a free side (or the ribbon grows on
         * straight), a ribbon whose own column was taken leans away, and the run along the surface ends before a taken
         * block. So the ribbons no longer share blocks, cross and twist at the top.
         */
        void ribbonsApart() {
            int count = between(4, 7);
            int top = headroom - 1;
            List<int[]> bases = new ArrayList<>();
            bases.add(new int[]{0, 0});
            List<int[]> around = new ArrayList<>(List.of(NEIGHBOURS));
            Collections.shuffle(around, r);
            for (int i = 1; i < count && i <= around.size(); i++) bases.add(around.get(i - 1));
            it.unimi.dsi.fastutil.longs.LongOpenHashSet taken = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
            for (int k = 0; k < bases.size(); k++) {
                int[] base = bases.get(k);
                int b0 = 1 + k * Math.max(1, stages - 3) / bases.size();
                int height = Math.max(2, top - r.nextInt(Math.max(1, top / 5) + 1));
                int[] d = DIRS[r.nextInt(4)];
                int swayEvery = between(4, 7);
                int run = height >= top - 1 ? len(between(2, 6)) : 0;
                int total = height + 1 + run;
                int x = base[0], z = base[1], n = 0;
                if (k > 0 && taken.contains(key(x, 0, z))) continue; // the base itself is taken: no room for this ribbon
                it.unimi.dsi.fastutil.longs.LongArrayList mine = new it.unimi.dsi.fastutil.longs.LongArrayList();
                int h;
                for (h = 0; h <= height; h++, n++) {
                    boolean lean = h > 0 && h % swayEvery == 0;
                    if (h > 0 && taken.contains(key(x, h, z))) lean = true; // a ribbon before leaned into this column
                    if (lean) {
                        int[] step = free(taken, x, h, z, d);
                        if (step != null) {
                            x += step[0];
                            z += step[1];
                            d = step;
                        } else if (taken.contains(key(x, h, z))) {
                            break; // boxed in: the ribbon ends here
                        }
                    }
                    if (h > 0 || k > 0) add(x, h, z, n >= total - 2 ? PlantPart.TOP : PlantPart.RIBBON, birth(b0, stages, n / (double) total));
                    mine.add(key(x, h, z));
                }
                if (h > height) {
                    for (int j = 1; j <= run; j++, n++) {
                        int rx = x + d[0] * j, rz = z + d[1] * j;
                        if (taken.contains(key(rx, height, rz)) || taken.contains(key(rx, height - 1, rz))) break;
                        add(rx, height, rz, n >= total - 2 ? PlantPart.TOP : PlantPart.RIBBON, birth(b0, stages, n / (double) total));
                        mine.add(key(rx, height, rz));
                    }
                }
                taken.addAll(mine);
            }
        }

        /** A side to lean to from (x, h, z) whose block at the same height is free: the wanted one first, then the others. */
        private int[] free(it.unimi.dsi.fastutil.longs.LongOpenHashSet taken, int x, int h, int z, int[] want) {
            if (!taken.contains(key(x + want[0], h, z + want[1])) && !taken.contains(key(x + want[0], h - 1, z + want[1]))) return want;
            for (int[] d : DIRS) {
                if (!taken.contains(key(x + d[0], h, z + d[1])) && !taken.contains(key(x + d[0], h - 1, z + d[1]))) return d;
            }
            return null;
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

        // ------------------------------------------------------------------ M20 corals

        /** Zoanthids / star polyps: a creeping mat over the rock, coloured oral discs (TOP) dotted in it. */
        void polyps() {
            int n = Math.max(5, (int) Math.round(150 * u * u));
            int maxR = Math.max(2, len(9)), maxUp = Math.max(1, len(3));
            List<int[]> cells = new ArrayList<>();
            cells.add(new int[]{0, 0, 0});
            int tries = 0;
            while (cells.size() < n && tries++ < n * 40) {
                int[] c = cells.get(r.nextInt(cells.size()));
                int roll = r.nextInt(10);
                int dx = 0, dy = 0, dz = 0;
                if (roll < 8) {
                    int[] d = DIRS[roll % 4];
                    dx = d[0];
                    dz = d[1];
                } else if (roll == 8) dy = 1;
                else dy = -1;
                int x = c[0] + dx, y = c[1] + dy, z = c[2] + dz;
                if (x * x + z * z > maxR * maxR || y > maxUp || y < minY || map.containsKey(key(x, y, z))) continue;
                int born = 1 + cells.size() * stages / n;
                if (add(x, y, z, r.nextInt(3) == 0 ? PlantPart.TOP : PlantPart.LEAF, born)) cells.add(new int[]{x, y, z});
            }
        }

        /** Mushroom corals: a few flat discs on short stalks, the oldest in the middle. */
        void discs() {
            int count = between(3, 6);
            int rad = Math.max(1, len(3.6));
            List<int[]> spots = new ArrayList<>();
            spots.add(new int[]{0, 0});
            for (int i = 1; i < count; i++) {
                double a = r.nextDouble() * Math.PI * 2, d = len(betweenD(5, 8));
                spots.add(new int[]{(int) Math.round(Math.cos(a) * d), (int) Math.round(Math.sin(a) * d)});
            }
            for (int i = 0; i < spots.size(); i++) {
                int[] sp = spots.get(i);
                int b0 = 1 + i * Math.max(1, stages - 2) / spots.size();
                int h = Math.max(1, len(between(1, 2)));
                // a short stalk from the root out to the disc (the discs fan out over the rock)
                int steps = Math.max(1, (int) Math.ceil(Math.sqrt(sp[0] * sp[0] + sp[1] * sp[1] + h * h) * 2));
                for (int k = 1; k <= steps; k++) {
                    double t = k / (double) steps;
                    add((int) Math.round(sp[0] * t), (int) Math.round((h - 1) * t), (int) Math.round(sp[1] * t), PlantPart.STEM, b0);
                }
                int rr = i == 0 ? rad : Math.max(1, rad - r.nextInt(2));
                for (int dx = -rr; dx <= rr; dx++)
                    for (int dz = -rr; dz <= rr; dz++) {
                        double d2 = dx * dx + dz * dz;
                        if (d2 > rr * rr + 0.6) continue;
                        int born = Math.min(stages, b0 + (int) Math.round(Math.sqrt(d2) / Math.max(1, rr) * 2));
                        add(sp[0] + dx, h, sp[1] + dz, d2 < 1.1 ? PlantPart.TOP : PlantPart.LEAF, born);
                    }
            }
        }

        /** Leather coral: a thick stalk and a wide domed cap with polyps on top. */
        void toadstool() {
            double f = fit(20);
            int stalk = Math.max(2, (int) Math.round(len(between(8, 12)) * f));
            int rad = Math.max(2, (int) Math.round(len(between(7, 9)) * f));
            int thick = u >= 0.75 ? 1 : 0;
            for (int y = 0; y < stalk; y++) {
                int born = 1 + y * Math.max(1, stages / 2) / stalk;
                for (int dx = -thick; dx <= thick; dx++)
                    for (int dz = -thick; dz <= thick; dz++) if (y > 0 || dx != 0 || dz != 0) add(dx, y, dz, PlantPart.STEM, born);
            }
            int lean = r.nextInt(4);
            int[] d = DIRS[lean];
            for (int dx = -rad; dx <= rad; dx++)
                for (int dz = -rad; dz <= rad; dz++) {
                    double d2 = dx * dx + dz * dz;
                    if (d2 > rad * rad + 0.5) continue;
                    double k = Math.sqrt(d2) / rad;
                    int born = Math.min(stages, stages / 2 + 1 + (int) Math.floor(k * (stages - stages / 2)));
                    // a smooth cap (neighbours differ by one block at most): the rim droops, the whole cap tilts a little
                    int y = stalk + (int) Math.round(-Math.max(0, k - 0.7) * 3 + (dx * d[0] + dz * d[1]) / (double) Math.max(1, rad) * 0.6);
                    add(dx, y, dz, PlantPart.LEAF, born);
                    if (k < 0.75 && r.nextInt(3) == 0) add(dx, y + 1, dz, PlantPart.TOP, Math.min(stages, born + 1));
                }
        }

        /**
         * Branching stony corals: Acropora (many thin branches with pale tips) and the hammer coral (a few stout
         * skeleton branches with fleshy, hammer-shaped heads).
         */
        void branches() {
            boolean hammer = s == PlantSpecies.HAMMER_CORAL;
            double f = fit(hammer ? 16 : 26);
            int trunks = hammer ? between(4, 6) : between(5, 7);
            int top = headroom - 1;
            for (int i = 0; i < trunks; i++) {
                double yaw = r.nextDouble() * Math.PI * 2 + i * 2.39996;
                double elev = Math.toRadians(hammer ? betweenD(55, 75) : betweenD(50, 80));
                int length = Math.max(2, (int) Math.round(len(hammer ? between(8, 13) : between(15, 24)) * f));
                int b0 = 1 + i * Math.max(1, stages / 2) / trunks;
                branch(0, 0, 0, yaw, elev, length, b0, hammer ? 0 : 2, hammer, top);
            }
        }

        private void branch(double x, double y, double z, double yaw, double elev, int length, int b0, int depth, boolean hammer, int top) {
            double step = 0.5;
            int lastX = (int) Math.round(x), lastY = (int) Math.round(y), lastZ = (int) Math.round(z), lastBorn = b0;
            for (double d = 0; d < length; d += step) {
                double nx = x + Math.cos(yaw) * Math.cos(elev) * step, nz = z + Math.sin(yaw) * Math.cos(elev) * step, ny = y + Math.sin(elev) * step;
                if ((int) Math.round(ny) >= top - (hammer ? 2 : 0)) break;
                x = nx;
                y = ny;
                z = nz;
                elev = Math.min(Math.toRadians(85), elev + Math.toRadians(hammer ? 1.5 : 2.5) * step);
                boolean tip = d >= length - 1.5;
                int born = Math.max(lastBorn, birth(b0, stages, d / length));
                if (!add((int) Math.round(x), (int) Math.round(y), (int) Math.round(z), hammer ? PlantPart.STEM : tip ? PlantPart.TOP : PlantPart.LEAF, born)) break;
                lastX = (int) Math.round(x);
                lastY = (int) Math.round(y);
                lastZ = (int) Math.round(z);
                lastBorn = born;
                if (depth > 0 && d > 2 && r.nextInt(hammer ? 12 : 7) == 0) {
                    branch(x, y, z, yaw + (r.nextBoolean() ? 1 : -1) * betweenD(0.6, 1.2), elev - 0.3, Math.max(2, (int) ((length - d) * 0.8)),
                            Math.min(stages, born + 1), depth - 1, hammer, top);
                }
            }
            if (hammer) {
                // the fleshy head: a lump of tentacles, wider than high
                int hr = Math.max(1, len(2.4));
                int born = lastBorn;
                for (int dx = -hr; dx <= hr; dx++)
                    for (int dz = -hr; dz <= hr; dz++)
                        for (int dy = 0; dy <= hr; dy++) {
                            if (dx * dx + dz * dz + dy * dy * 2 > hr * hr + 1) continue;
                            add(lastX + dx, lastY + dy, lastZ + dz, dy == hr || dx * dx + dz * dz >= hr * hr ? PlantPart.TOP : PlantPart.LEAF, born);
                        }
            }
        }

        /** Lobophyllia: a dome growing outwards from the root, its ridges (TOP) in a meandering pattern. */
        void dome() {
            double f = fit(12);
            int rad = Math.max(2, (int) Math.round(len(between(8, 10)) * f));
            double phase = r.nextDouble() * 6;
            for (int dx = -rad; dx <= rad; dx++)
                for (int dz = -rad; dz <= rad; dz++)
                    for (int dy = 0; dy <= rad; dy++) {
                        double d2 = dx * dx + dz * dz + dy * dy * 1.6;
                        if (d2 > rad * rad + 0.5) continue;
                        double k = Math.sqrt(d2) / rad;
                        int born = Math.min(stages, 1 + (int) Math.floor(k * stages));
                        boolean ridge = Math.sin(dx * 0.9 + Math.sin(dz * 0.7 + phase) * 1.8) > 0.55;
                        add(dx, dy, dz, ridge ? PlantPart.TOP : PlantPart.LEAF, born);
                    }
        }

        /** Bubble tip anemone: a short foot, an oral disc and a ring of tentacles with bubble tips. */
        void anemone() {
            double f = fit(12);
            int disc = Math.max(1, (int) Math.round(len(5.5) * f));
            int foot = Math.max(1, len(2));
            for (int y = 1; y <= foot; y++) add(0, y, 0, PlantPart.LEAF, 1);
            for (int dx = -disc; dx <= disc; dx++)
                for (int dz = -disc; dz <= disc; dz++) {
                    double d2 = dx * dx + dz * dz;
                    if (d2 > disc * disc + 0.6) continue;
                    double k = Math.sqrt(d2) / disc;
                    int born = 1 + (int) Math.floor(k * (stages / 2.0));
                    add(dx, foot, dz, PlantPart.LEAF, born);
                    // tentacles stand on the disc, longer outside, swollen tips
                    if ((dx + dz) % 2 == 0 && d2 >= 1) {
                        int h = Math.max(1, (int) Math.round(len(betweenD(3, 6)) * f * (0.6 + 0.4 * k)));
                        for (int y = 1; y <= h; y++) {
                            add(dx + (k > 0.7 && y == h ? Integer.signum(dx) : 0), foot + y, dz + (k > 0.7 && y == h ? Integer.signum(dz) : 0),
                                    y == h ? PlantPart.TOP : PlantPart.LEAF, Math.min(stages, born + 1 + y * (stages / 2) / Math.max(1, h)));
                        }
                    }
                }
        }
    }

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
}
