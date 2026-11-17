package org.overengineer.bettacraft.client.interior;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.overengineer.bettacraft.plants.PlantPart;

/**
 * M21.1: the structure behind the plant meshes ({@link PlantMeshRenderer}), worked out from the blocks of one plant
 * alone (plain Java, unit tested over every growth form). Positions are block offsets from the root, packed with
 * {@link #key}.
 * <ul>
 *     <li>{@link #stems} (Rotala): every shoot as a connected chain of stem nodes, also where its blocks were taken over
 *     by the leaves of a neighbouring shoot (the stem passes through), with branches drawn back to the main shoot; every
 *     leaf block hangs on a stem node right next to it (a leaf further away pulls the blocks between into the stem, so
 *     the tips running along the surface become a creeping stem with leaves).</li>
 *     <li>{@link #ribbons} (Vallisneria): every ribbon as one continuous chain from its base on the floor up to the
 *     surface and along it to its tip; blocks no ribbon took become a ribbon branching off the nearest one, never a
 *     loose piece.</li>
 * </ul>
 */
public final class PlantSkeleton {
    /** Parent of a stem node that stands on the floor. */
    public static final long FLOOR = Long.MIN_VALUE;
    private static final int BITS = 21, MASK = (1 << BITS) - 1, BIAS = 1 << (BITS - 1);
    private static final int[][] ORTH = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] CORNERS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private PlantSkeleton() {}

    public static long key(int x, int y, int z) {
        return ((long) ((x + BIAS) & MASK) << (2 * BITS)) | ((long) ((y + BIAS) & MASK) << BITS) | ((z + BIAS) & MASK);
    }

    public static int x(long k) {
        return (int) ((k >>> (2 * BITS)) & MASK) - BIAS;
    }

    public static int y(long k) {
        return (int) ((k >>> BITS) & MASK) - BIAS;
    }

    public static int z(long k) {
        return (int) (k & MASK) - BIAS;
    }

    private static long off(long k, int dx, int dy, int dz) {
        return key(x(k) + dx, y(k) + dy, z(k) + dz);
    }

    private static int cheb(long a, long b) {
        return Math.max(Math.abs(x(a) - x(b)), Math.max(Math.abs(y(a) - y(b)), Math.abs(z(a) - z(b))));
    }

    private static double dist2(long a, long b) {
        double dx = x(a) - x(b), dy = y(a) - y(b), dz = z(a) - z(b);
        return dx * dx + dy * dy + dz * dz;
    }

    /** Keys sorted bottom-up (then x, z): the order everything is worked out in, so the result is reproducible. */
    private static LongArrayList sorted(LongIterableSet keys) {
        LongArrayList l = new LongArrayList(keys.size());
        keys.forEach(l::add);
        l.sort((a, b) -> {
            int c = Integer.compare(y(a), y(b));
            if (c != 0) return c;
            c = Integer.compare(x(a), x(b));
            return c != 0 ? c : Integer.compare(z(a), z(b));
        });
        return l;
    }

    private interface LongIterableSet {
        int size();

        void forEach(java.util.function.LongConsumer c);
    }

    private static LongIterableSet of(Long2ObjectMap<PlantPart> m) {
        return new LongIterableSet() {
            public int size() {
                return m.size();
            }

            public void forEach(java.util.function.LongConsumer c) {
                for (long k : m.keySet()) c.accept(k);
            }
        };
    }

    // ------------------------------------------------------------------ stems (Rotala)

    /** Stem nodes with their parents (another node or {@link #FLOOR}) and the node every leaf block hangs on. */
    public static final class Stems {
        /** Node -> parent node, or {@link #FLOOR}. */
        public final Long2LongOpenHashMap parent = new Long2LongOpenHashMap();
        /** Leaf block -> the stem node it hangs on (always right next to it). */
        public final Long2LongOpenHashMap leafAt = new Long2LongOpenHashMap();
        /** Nodes in the order they were found (parents before their children). */
        public final LongArrayList order = new LongArrayList();

        boolean node(long k) {
            return parent.containsKey(k);
        }

        void add(long k, long p) {
            parent.put(k, p);
            order.add(k);
        }

        /** Nodes that no other node hangs on: the growing tips. */
        public LongOpenHashSet tips() {
            LongOpenHashSet t = new LongOpenHashSet(parent.keySet());
            for (long p : parent.values()) t.remove(p);
            return t;
        }
    }

    /**
     * The stems of a stem plant: ROOT and STEM blocks are stem nodes; a TOP block continues the stem below it (the tip of
     * a shoot) if it stands right above it, or diagonally above where the shoot leans (only one TOP per stem node, the
     * one that looks most like a stem: right above it, with leaves on both sides); the other LEAF and TOP blocks are
     * leaves. A node hangs on the node right below it, else diagonally below, else up to three blocks below in its
     * column (its own blocks there were taken over by the leaves of a neighbouring shoot), else on a node of its own
     * height that hangs somewhere, else on the nearest lower node (a branch drawn back to the main shoot). Nodes of the
     * lowest layer stand on the floor.
     */
    public static Stems stems(Long2ObjectMap<PlantPart> blocks) {
        Stems s = new Stems();
        if (blocks.isEmpty()) return s;
        LongArrayList all = sorted(of(blocks));
        int minY = y(all.getLong(0));
        Long2ObjectOpenHashMap<LongArrayList> levels = new Long2ObjectOpenHashMap<>();
        for (long k : all) levels.computeIfAbsent(y(k), q -> new LongArrayList()).add(k);
        int maxY = y(all.getLong(all.size() - 1));
        for (int y = minY; y <= maxY; y++) {
            LongArrayList level = levels.get(y);
            if (level == null) continue;
            LongArrayList open = new LongArrayList();
            for (long k : level) {
                PlantPart p = blocks.get(k);
                if (p != PlantPart.ROOT && p != PlantPart.STEM) continue;
                if (y == minY) {
                    s.add(k, FLOOR);
                    continue;
                }
                long par = below(blocks, s, k);
                if (par != Long.MAX_VALUE) s.add(k, par);
                else open.add(k);
            }
            // TOP blocks that continue a stem node below (one per node)
            Long2LongOpenHashMap best = new Long2LongOpenHashMap();
            Long2LongOpenHashMap bestScore = new Long2LongOpenHashMap();
            for (long k : level) {
                if (blocks.get(k) != PlantPart.TOP) continue;
                long direct = off(k, 0, -1, 0);
                long from = Long.MAX_VALUE;
                int score = 0;
                if (s.node(direct)) {
                    from = direct;
                    score = 2;
                } else {
                    for (int[] d : ORTH) {
                        long b = off(k, d[0], -1, d[1]);
                        // a lean: the node below has no stem block right above it
                        if (s.node(b) && !stemPart(blocks, off(b, 0, 1, 0))) {
                            from = b;
                            score = 1;
                            break;
                        }
                    }
                }
                if (from == Long.MAX_VALUE) continue;
                if (pairAround(blocks, k)) score += 3;
                if (!best.containsKey(from) || score > bestScore.get(from)) {
                    best.put(from, k);
                    bestScore.put(from, score);
                }
            }
            LongArrayList tops = new LongArrayList(best.keySet());
            tops.sort(Long::compare);
            for (long from : tops) {
                long k = best.get(from);
                // a stem node of this level next to it already carries the shoot: this TOP is one of its leaves
                if (!s.node(k)) s.add(k, from);
            }
            // nodes of this level that found nothing below: a neighbour of the same height that hangs somewhere
            boolean progress = true;
            while (!open.isEmpty() && progress) {
                progress = false;
                for (int i = 0; i < open.size(); i++) {
                    long k = open.getLong(i);
                    long par = Long.MAX_VALUE;
                    for (int[] d : ORTH) if (par == Long.MAX_VALUE && s.node(off(k, d[0], 0, d[1]))) par = off(k, d[0], 0, d[1]);
                    for (int[] d : CORNERS) if (par == Long.MAX_VALUE && s.node(off(k, d[0], 0, d[1]))) par = off(k, d[0], 0, d[1]);
                    if (par != Long.MAX_VALUE) {
                        s.add(k, par);
                        open.removeLong(i--);
                        progress = true;
                    }
                }
            }
            // still nothing: a branch drawn back to the nearest lower node of the plant (the main shoot)
            for (long k : open) s.add(k, nearestNode(s, k, y));
        }
        attachLeaves(blocks, s, all);
        return s;
    }

    private static boolean stemPart(Long2ObjectMap<PlantPart> blocks, long k) {
        PlantPart p = blocks.get(k);
        return p == PlantPart.STEM || p == PlantPart.ROOT;
    }

    /** Leaf blocks (LEAF or TOP) on both sides of it along x or z: it looks like the stem between a pair of leaves. */
    private static boolean pairAround(Long2ObjectMap<PlantPart> blocks, long k) {
        return leafy(blocks.get(off(k, 1, 0, 0))) && leafy(blocks.get(off(k, -1, 0, 0)))
                || leafy(blocks.get(off(k, 0, 0, 1))) && leafy(blocks.get(off(k, 0, 0, -1)));
    }

    private static boolean leafy(PlantPart p) {
        return p == PlantPart.LEAF || p == PlantPart.TOP;
    }

    /**
     * The node a stem block hangs on below it, or {@code Long.MAX_VALUE}: right below; diagonally below where the shoot
     * leans (a node whose own shoot does not go on straight up); two or three blocks below in its column (its blocks
     * between were taken over by the leaves of a neighbouring shoot, the stem passes through them); any node diagonally
     * below; two or three blocks below and one aside.
     */
    private static long below(Long2ObjectMap<PlantPart> blocks, Stems s, long k) {
        if (s.node(off(k, 0, -1, 0))) return off(k, 0, -1, 0);
        for (int[][] dirs : new int[][][] {ORTH, CORNERS}) {
            for (int[] d : dirs) {
                long b = off(k, d[0], -1, d[1]);
                if (s.node(b) && !stemPart(blocks, off(b, 0, 1, 0))) return b;
            }
        }
        for (int dy = 2; dy <= 3; dy++) if (s.node(off(k, 0, -dy, 0))) return off(k, 0, -dy, 0);
        for (int[][] dirs : new int[][][] {ORTH, CORNERS}) for (int[] d : dirs) if (s.node(off(k, d[0], -1, d[1]))) return off(k, d[0], -1, d[1]);
        for (int dy = 2; dy <= 3; dy++) for (int[] d : ORTH) if (s.node(off(k, d[0], -dy, d[1]))) return off(k, d[0], -dy, d[1]);
        return Long.MAX_VALUE;
    }

    private static long nearestNode(Stems s, long k, int belowY) {
        long best = FLOOR;
        double bd = Double.MAX_VALUE;
        for (long n : s.order) {
            if (y(n) >= belowY) continue;
            double d = dist2(n, k);
            if (d < bd) {
                bd = d;
                best = n;
            }
        }
        return best;
    }

    /**
     * Every block that is no stem node hangs on a node right next to it: of its height first (the pair of leaves of a
     * node), then below, then above, then any. A leaf block with no node next to it pulls the blocks on the shortest way to
     * the nearest node into the stem (a creeping stem along the surface, leaves around it).
     */
    private static void attachLeaves(Long2ObjectMap<PlantPart> blocks, Stems s, LongArrayList all) {
        for (long k : all) {
            if (s.node(k) || s.leafAt.containsKey(k)) continue;
            long n = adjacentNode(s, k);
            if (n == Long.MAX_VALUE) n = outerLeafNode(s, k);
            if (n == Long.MAX_VALUE) {
                LongList path = pathToNode(blocks, s, k);
                if (path == null) {
                    // cannot happen for a connected plant; hang it on the nearest node as a stem of its own
                    s.add(k, nearestNode(s, k, Integer.MAX_VALUE));
                    continue;
                }
                // path: k, b1, b2, ..., node; b_i become nodes hanging towards the node
                for (int i = path.size() - 2; i >= 1; i--) {
                    long b = path.getLong(i);
                    s.leafAt.remove(b);
                    if (!s.node(b)) s.add(b, path.getLong(i + 1));
                }
                n = path.size() >= 2 ? path.getLong(1) : nearestNode(s, k, Integer.MAX_VALUE);
            }
            s.leafAt.put(k, n);
        }
        // an outer leaf part whose inner part became a stem later hangs on that stem (right next to it)
        for (var e : s.leafAt.long2LongEntrySet()) {
            long k = e.getLongKey(), n = e.getLongValue();
            if (cheb(k, n) != 2) continue;
            long mid = off(n, Integer.signum(x(k) - x(n)), 0, Integer.signum(z(k) - z(n)));
            if (s.node(mid)) e.setValue(mid);
        }
    }

    /**
     * M22.1: the outer part of a big leaf (Ludwigia, Alternanthera: two blocks out and one up from its node): the node two
     * blocks aside and one below whose leaf lies right between them. Or {@code Long.MAX_VALUE}.
     */
    private static long outerLeafNode(Stems s, long k) {
        for (int[] d : ORTH) {
            long n = off(k, -2 * d[0], -1, -2 * d[1]);
            if (!s.node(n)) continue;
            long mid = off(n, d[0], 0, d[1]);
            if (s.leafAt.containsKey(mid) && s.leafAt.get(mid) == n) return n;
        }
        return Long.MAX_VALUE;
    }

    private static long adjacentNode(Stems s, long k) {
        for (int[] d : ORTH) if (s.node(off(k, d[0], 0, d[1]))) return off(k, d[0], 0, d[1]);
        for (int[] d : CORNERS) if (s.node(off(k, d[0], 0, d[1]))) return off(k, d[0], 0, d[1]);
        for (int dy : new int[] {-1, 1}) {
            if (s.node(off(k, 0, dy, 0))) return off(k, 0, dy, 0);
            for (int[] d : ORTH) if (s.node(off(k, d[0], dy, d[1]))) return off(k, d[0], dy, d[1]);
            for (int[] d : CORNERS) if (s.node(off(k, d[0], dy, d[1]))) return off(k, d[0], dy, d[1]);
        }
        return Long.MAX_VALUE;
    }

    /** Shortest way (26-neighbourhood, through blocks of the plant) from k to a stem node: k first, the node last. */
    private static LongList pathToNode(Long2ObjectMap<PlantPart> blocks, Stems s, long start) {
        // M22.1: first a way that never goes up (the blocks on it become a stem that hangs on the next one, never on
        // one above it), only then any way
        LongList down = pathToNode(blocks, s, start, false);
        return down != null ? down : pathToNode(blocks, s, start, true);
    }

    private static LongList pathToNode(Long2ObjectMap<PlantPart> blocks, Stems s, long start, boolean up) {
        Long2LongOpenHashMap from = new Long2LongOpenHashMap();
        ArrayDeque<Long> q = new ArrayDeque<>();
        q.add(start);
        from.put(start, start);
        while (!q.isEmpty()) {
            long c = q.poll();
            for (int dy = -1; dy <= (up ? 1 : 0); dy++)
                for (int dz = -1; dz <= 1; dz++)
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        long n = off(c, dx, dy, dz);
                        if (from.containsKey(n) || !blocks.containsKey(n)) continue;
                        from.put(n, c);
                        if (s.node(n)) {
                            LongArrayList path = new LongArrayList();
                            for (long p = n; p != start; p = from.get(p)) path.add(p);
                            path.add(start);
                            java.util.Collections.reverse(path);
                            return path;
                        }
                        q.add(n);
                    }
        }
        return null;
    }

    // ------------------------------------------------------------------ ribbons (Vallisneria)

    /** One ribbon: its blocks in order from the base to the tip, and where it starts (FLOOR or a block of another ribbon). */
    public record Ribbon(LongList cells, long from) {}

    /**
     * The ribbons of a ribbon plant. Every block of the lowest layer is the base of a ribbon. All ribbons grow together a
     * layer at a time (see {@link #walk}): up, then along the surface; where ribbons cross they share the block. Blocks no
     * ribbon took become ribbons that branch off a ribbon next to them.
     */
    public static List<Ribbon> ribbons(Long2ObjectMap<PlantPart> blocks) {
        List<Ribbon> out = new ArrayList<>();
        if (blocks.isEmpty()) return out;
        LongArrayList all = sorted(of(blocks));
        int minY = y(all.getLong(0));
        LongOpenHashSet taken = new LongOpenHashSet();
        List<Walker> walkers = new ArrayList<>();
        for (long k : all) {
            if (y(k) != minY) break;
            Walker w = new Walker(k, FLOOR);
            taken.add(k);
            walkers.add(w);
        }
        walk(blocks, taken, new ArrayList<>(walkers));
        // leftovers, lowest first: right after the tip of a ribbon they continue it, else a branch off the ribbon next to them
        while (true) {
            long start = Long.MAX_VALUE;
            Walker cont = null;
            for (long k : all) {
                if (taken.contains(k)) continue;
                for (Walker w : walkers) {
                    long t = w.tip();
                    if (cheb(t, k) == 1 && y(k) >= y(t)) {
                        cont = w;
                        break;
                    }
                }
                if (cont != null) {
                    start = k;
                    break;
                }
            }
            if (cont != null) {
                long t = cont.tip();
                int dx = x(start) - x(t), dz = z(start) - z(t);
                taken.add(start);
                cont.add(start);
                cont.flat = y(start) == y(t);
                if ((dx != 0) != (dz != 0)) {
                    cont.lx = dx;
                    cont.lz = dz;
                }
                walk(blocks, taken, new ArrayList<>(List.of(cont)));
                continue;
            }
            long at = Long.MAX_VALUE;
            for (long k : all) {
                if (taken.contains(k)) continue;
                long n = adjacentTaken(taken, k);
                if (n != Long.MAX_VALUE) {
                    start = k;
                    at = n;
                    break;
                }
            }
            if (start == Long.MAX_VALUE) {
                // blocks not connected to anything taken (cannot happen for a plant found by a flood fill): own ribbons
                for (long k : all) if (!taken.contains(k)) {
                    start = k;
                    break;
                }
                if (start == Long.MAX_VALUE) break;
                at = FLOOR;
            }
            taken.add(start);
            Walker w = new Walker(start, at);
            walkers.add(w);
            walk(blocks, taken, new ArrayList<>(List.of(w)));
        }
        for (Walker w : walkers) out.add(new Ribbon(w.cells, w.from));
        return out;
    }

    private static final class Walker {
        final LongArrayList cells = new LongArrayList();
        final LongOpenHashSet own = new LongOpenHashSet();
        final long from;
        boolean flat, moved;
        /** Direction of its lean (the generator leans a ribbon one way only and runs on along the surface that way). */
        int lx, lz;

        Walker(long base, long from) {
            add(base);
            this.from = from;
        }

        void add(long k) {
            cells.add(k);
            own.add(k);
        }

        long tip() {
            return cells.getLong(cells.size() - 1);
        }
    }

    /** A block above k (straight or leaning) or beside it: a ribbon passing through k can go on. */
    private static boolean goesOn(Long2ObjectMap<PlantPart> blocks, long k, int dx, int dz) {
        if (blocks.containsKey(off(k, 0, 1, 0))) return true;
        if (dx != 0 || dz != 0) return blocks.containsKey(off(k, dx, 1, dz)) || blocks.containsKey(off(k, dx, 0, dz));
        for (int[] d : ORTH) if (blocks.containsKey(off(k, d[0], 1, d[1]))) return true;
        return false;
    }

    /**
     * All walkers a layer at a time. Up: the free block right above, else the free block diagonally above in the way it
     * leans (any way before its first lean); where another ribbon crosses, it passes through that ribbon's block (the
     * generator merges crossing ribbons into one block) if it goes on from there. At the top: along the surface in the
     * way of its lean, also through a block of another ribbon if there is more of its way behind it.
     */
    private static void walk(Long2ObjectMap<PlantPart> blocks, LongOpenHashSet taken, List<Walker> active) {
        for (int guard = 0; !active.isEmpty() && guard < 1024; guard++) {
            // up first for all ribbons (so a ribbon running along the surface never takes the top of another one) ...
            for (int pass = 0; pass < 5; pass++) {
                for (Walker w : active) {
                    if (w.flat || w.moved) continue;
                    long t = w.tip();
                    long up = off(t, 0, 1, 0);
                    long n = Long.MAX_VALUE;
                    boolean share = false;
                    switch (pass) {
                        case 0 -> n = free(blocks, taken, up);
                        case 1 -> {
                            if (w.lx != 0 || w.lz != 0) n = free(blocks, taken, off(t, w.lx, 1, w.lz));
                            else for (int[] d : ORTH) if (n == Long.MAX_VALUE) n = free(blocks, taken, off(t, d[0], 1, d[1]));
                        }
                        // M22.1: the generator of new plants turns a lean aside where another ribbon is: a free block
                        // diagonally above to any side before sharing a block
                        case 2 -> {
                            if (w.lx != 0 || w.lz != 0) for (int[] d : ORTH) if (n == Long.MAX_VALUE) n = free(blocks, taken, off(t, d[0], 1, d[1]));
                        }
                        case 3 -> {
                            if (blocks.containsKey(up) && !w.own.contains(up) && goesOn(blocks, up, w.lx, w.lz)) {
                                n = up;
                                share = true;
                            } else {
                                for (int[] dd : ORTH) {
                                    if ((w.lx != 0 || w.lz != 0) && (dd[0] != w.lx || dd[1] != w.lz)) continue;
                                    long d = off(t, dd[0], 1, dd[1]);
                                    if (n == Long.MAX_VALUE && blocks.containsKey(d) && !w.own.contains(d) && goesOn(blocks, d, dd[0], dd[1])) {
                                        n = d;
                                        share = true;
                                    }
                                }
                            }
                        }
                        default -> {
                            for (int[] d : CORNERS) if (n == Long.MAX_VALUE) n = free(blocks, taken, off(t, d[0], 1, d[1]));
                        }
                    }
                    if (n == Long.MAX_VALUE) continue;
                    if (!share) taken.add(n);
                    int dx = x(n) - x(t), dz = z(n) - z(t);
                    if ((dx != 0) != (dz != 0) && w.lx == 0 && w.lz == 0) {
                        w.lx = dx;
                        w.lz = dz;
                    }
                    w.add(n);
                    w.moved = true;
                }
            }
            for (Walker w : active) {
                if (!w.flat && !w.moved) w.flat = true;
                w.moved = false;
            }
            // ... then along the surface for the ones that reached their top
            List<Walker> next = new ArrayList<>();
            for (Walker w : active) {
                if (!w.flat) {
                    next.add(w);
                    continue;
                }
                long t = w.tip();
                long n = Long.MAX_VALUE;
                boolean share = false;
                if (w.lx != 0 || w.lz != 0) {
                    long f = off(t, w.lx, 0, w.lz);
                    n = free(blocks, taken, f);
                    if (n == Long.MAX_VALUE && blocks.containsKey(f) && !w.own.contains(f) && free(blocks, taken, off(f, w.lx, 0, w.lz)) != Long.MAX_VALUE) {
                        n = f;
                        share = true;
                    }
                } else {
                    for (int[] d : ORTH) if (n == Long.MAX_VALUE) n = free(blocks, taken, off(t, d[0], 0, d[1]));
                    if (n != Long.MAX_VALUE) {
                        w.lx = x(n) - x(t);
                        w.lz = z(n) - z(t);
                    }
                }
                if (n != Long.MAX_VALUE && w.cells.size() < 512) {
                    if (!share) taken.add(n);
                    w.add(n);
                    next.add(w);
                }
            }
            active = next;
        }
    }

    private static long free(Long2ObjectMap<PlantPart> blocks, LongOpenHashSet taken, long k) {
        return blocks.containsKey(k) && !taken.contains(k) ? k : Long.MAX_VALUE;
    }

    private static long adjacentTaken(LongOpenHashSet taken, long k) {
        long best = Long.MAX_VALUE;
        int bestRank = Integer.MAX_VALUE;
        for (int dy = -1; dy <= 1; dy++)
            for (int dz = -1; dz <= 1; dz++)
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    long n = off(k, dx, dy, dz);
                    if (!taken.contains(n)) continue;
                    // prefer the block below, then beside, then diagonals
                    int rank = (dy == -1 && dx == 0 && dz == 0 ? 0 : dy == 0 && Math.abs(dx) + Math.abs(dz) == 1 ? 1 : 2 + Math.abs(dx) + Math.abs(dz) + Math.abs(dy));
                    if (rank < bestRank) {
                        bestRank = rank;
                        best = n;
                    }
                }
        return best;
    }

    /** Chebyshev distance of two keys (tests). */
    public static int distance(long a, long b) {
        return cheb(a, b);
    }

    // ------------------------------------------------------------------ M22.1 trees, leaves, clusters

    /** Every offset of the 26-neighbourhood in a fixed order (straight before diagonal), so results are reproducible. */
    private static final int[][] N26 = n26();

    private static int[][] n26() {
        List<int[]> l = new ArrayList<>();
        for (int dy = -1; dy <= 1; dy++)
            for (int dz = -1; dz <= 1; dz++)
                for (int dx = -1; dx <= 1; dx++) if (dx != 0 || dy != 0 || dz != 0) l.add(new int[] {dx, dy, dz});
        l.sort((a, b) -> Integer.compare(Math.abs(a[0]) + Math.abs(a[1]) + Math.abs(a[2]), Math.abs(b[0]) + Math.abs(b[1]) + Math.abs(b[2])));
        return l.toArray(new int[0][]);
    }

    /**
     * M22.1: a shortest-path tree over the blocks of a plant: from every source block (the root, the rhizome ...) out
     * through the blocks {@code walk} accepts (26-neighbourhood). Every block reached knows its parent (sources:
     * {@link #FLOOR}) and its depth (steps from a source).
     */
    public static final class Tree {
        public final Long2LongOpenHashMap parent = new Long2LongOpenHashMap();
        public final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap depth = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
        /** Blocks in the order they were reached (parents before their children). */
        public final LongArrayList order = new LongArrayList();

        public boolean contains(long k) {
            return parent.containsKey(k);
        }

        /** Blocks no other block hangs on. */
        public LongOpenHashSet ends() {
            LongOpenHashSet t = new LongOpenHashSet(parent.keySet());
            for (long p : parent.values()) t.remove(p);
            return t;
        }

        /** Steps from every block to the deepest end below it (0 for an end): thick near the base, thin at the tips. */
        public it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap reach() {
            var r = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
            for (int i = order.size() - 1; i >= 0; i--) {
                long k = order.getLong(i);
                int mine = r.getOrDefault(k, 0);
                r.putIfAbsent(k, 0);
                long p = parent.get(k);
                if (p != FLOOR) r.put(p, Math.max(r.getOrDefault(p, 0), mine + 1));
            }
            return r;
        }

        /** The blocks from the source down to {@code k} (the source first, {@code k} last). */
        public LongArrayList path(long k) {
            LongArrayList l = new LongArrayList();
            for (long c = k; c != FLOOR && l.size() <= order.size(); c = parent.get(c)) l.add(c);
            java.util.Collections.reverse(l);
            return l;
        }
    }

    public static Tree tree(Long2ObjectMap<PlantPart> blocks, java.util.function.LongPredicate source, java.util.function.LongPredicate walk) {
        Tree t = new Tree();
        ArrayDeque<Long> q = new ArrayDeque<>();
        for (long k : sorted(of(blocks))) {
            if (!source.test(k)) continue;
            t.parent.put(k, FLOOR);
            t.depth.put(k, 0);
            t.order.add(k);
            q.add(k);
        }
        while (!q.isEmpty()) {
            long c = q.poll();
            int d = t.depth.get(c);
            for (int[] o : N26) {
                long n = off(c, o[0], o[1], o[2]);
                if (t.parent.containsKey(n) || !blocks.containsKey(n) || !walk.test(n)) continue;
                t.parent.put(n, c);
                t.depth.put(n, d + 1);
                t.order.add(n);
                q.add(n);
            }
        }
        return t;
    }

    /**
     * M22.1: the tips of the leaves of a rosette or a rhizome: blocks of the tree no neighbour of which lies deeper, at
     * least {@code minDepth} steps from a source; tips closer than {@code radius} to a deeper tip belong to the same leaf
     * (the width of a blade ends in several such blocks). Deepest first.
     */
    public static LongArrayList leafTips(Tree t, int minDepth, double radius) {
        LongArrayList cand = new LongArrayList();
        for (long k : t.order) {
            int d = t.depth.get(k);
            if (d < minDepth) continue;
            boolean max = true;
            for (int[] o : N26) {
                long n = off(k, o[0], o[1], o[2]);
                if (t.depth.getOrDefault(n, -1) > d) {
                    max = false;
                    break;
                }
            }
            if (max) cand.add(k);
        }
        // deepest first; same depth in the tree's order (reproducible)
        LongArrayList byDepth = new LongArrayList(cand);
        byDepth.sort((a, b) -> Integer.compare(t.depth.get(b), t.depth.get(a)));
        LongArrayList tips = new LongArrayList();
        double r2 = radius * radius;
        for (long k : byDepth) {
            boolean near = false;
            for (long o : tips) {
                if (dist2(o, k) <= r2) {
                    near = true;
                    break;
                }
            }
            if (!near) tips.add(k);
        }
        return tips;
    }

    /** M22.1: the 26-connected groups of the blocks {@code in} accepts (polyp heads, discs, floating pads), each sorted. */
    public static List<LongArrayList> clusters(Long2ObjectMap<PlantPart> blocks, java.util.function.LongPredicate in) {
        List<LongArrayList> out = new ArrayList<>();
        LongOpenHashSet seen = new LongOpenHashSet();
        for (long k : sorted(of(blocks))) {
            if (seen.contains(k) || !in.test(k)) continue;
            LongArrayList c = new LongArrayList();
            ArrayDeque<Long> q = new ArrayDeque<>();
            q.add(k);
            seen.add(k);
            while (!q.isEmpty()) {
                long p = q.poll();
                c.add(p);
                for (int[] o : N26) {
                    long n = off(p, o[0], o[1], o[2]);
                    if (seen.contains(n) || !blocks.containsKey(n) || !in.test(n)) continue;
                    seen.add(n);
                    q.add(n);
                }
            }
            out.add(c);
        }
        return out;
    }
}
