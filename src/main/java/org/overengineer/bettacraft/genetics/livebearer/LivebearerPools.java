package org.overengineer.bettacraft.genetics.livebearer;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * M21.6: the alleles of wild fish and of mutations, per background: wild guppies (round or spade tails, clear tails with
 * spots, the orange and black Y spotting) and the endler's own, smaller allele set (swords, orange or yellow tails, the
 * peacock, tiger, black bar and japan blue Y patterns). Bred alleles turn up hidden in wild fish now and then.
 */
public final class LivebearerPools {
    private record W(String allele, int weight) {}

    private LivebearerPools() {}

    private static List<W> pool(LbLocus l, boolean endler) {
        return switch (l) {
            case TAIL -> endler
                    ? List.of(w("round", 30), w("double_sword", 25), w("top_sword", 15), w("bottom_sword", 15), w("lyre", 5), w("spade", 10))
                    : List.of(w("round", 45), w("spade", 25), w("delta", 8), w("triangle", 6), w("flag", 4), w("double_sword", 4), w("top_sword", 3),
                    w("bottom_sword", 3), w("lyre", 2));
            case TAIL_COLOR -> endler
                    ? List.of(w("orange", 60), w("yellow", 25), w("clear", 10), w("green", 5))
                    : List.of(w("clear", 55), w("red", 12), w("yellow", 10), w("blue", 8), w("green", 6), w("black", 4), w("orange", 3), w("white", 2));
            case TAIL_PATTERN -> endler
                    ? List.of(w("spots", 60), w("none", 35), w("leopard", 5))
                    : List.of(w("spots", 60), w("none", 15), w("grass", 10), w("mosaic", 8), w("leopard", 7));
            case GOLD -> List.of(w("wild", 95), w("gold", 5));
            case BLOND -> List.of(w("wild", 96), w("blond", 4));
            case ALBINO -> List.of(w("wild", 98), w("albino", 2));
            case BLUE -> List.of(w("wild", endler ? 98 : 95), w("blue", endler ? 2 : 5));
            case DARK -> List.of(w("wild", 96), w("dark", 4));
        };
    }

    private static List<W> yPool(boolean endler) {
        return endler ? List.of(w("peacock", 60), w("tiger", 14), w("black_bar", 14), w("japan_blue", 6), w("none", 6))
                : List.of(w("wild", 78), w("none", 8), w("cobra", 5), w("red_tail", 4), w("japan_blue", 2), w("moscow", 2), w("tuxedo", 1));
    }

    private static List<W> xPool(boolean endler) {
        return endler ? List.of(w("none", 100)) : List.of(w("none", 92), w("tuxedo", 4), w("cobra", 3), w("moscow", 1));
    }

    private static W w(String a, int weight) {
        return new W(a, weight);
    }

    private static String pick(List<W> pool, RandomGenerator r) {
        int total = 0;
        for (W x : pool) total += x.weight;
        int k = r.nextInt(total);
        for (W x : pool) {
            k -= x.weight;
            if (k < 0) return x.allele;
        }
        return pool.getLast().allele;
    }

    /** A random allele of the locus; {@code anyBred} picks among all alleles equally (mutations). */
    public static String pick(LbLocus l, boolean endler, RandomGenerator r, boolean anyBred) {
        if (anyBred) {
            List<W> p = pool(l, endler);
            return p.get(r.nextInt(p.size())).allele;
        }
        return pick(pool(l, endler), r);
    }

    /**
     * A wild fish: the visible traits of its background (guppy: round or spade tail, clear tail with spots; endler: its
     * swords and orange or yellow tails), one allele in five of the tail loci and one in twelve of the body colour
     * loci is a random other allele carried hidden (a dominant one may show now and then, like a sword in the wild).
     */
    public static LivebearerGenome wild(boolean endler, RandomGenerator r) {
        LivebearerGenome.Builder b = LivebearerGenome.builder(endler);
        for (LbLocus l : LbLocus.values()) {
            List<W> p = pool(l, endler);
            String a, h;
            switch (l) {
                case TAIL -> {
                    a = endler ? pick(p, r) : pick(List.of(w("round", 60), w("spade", 40)), r);
                    h = r.nextInt(5) == 0 ? pick(p, r) : a;
                }
                case TAIL_PATTERN -> {
                    a = endler ? pick(p, r) : pick(List.of(w("spots", 80), w("none", 20)), r);
                    h = r.nextInt(5) == 0 ? pick(p, r) : a;
                }
                case TAIL_COLOR -> {
                    a = endler ? (r.nextInt(4) == 0 ? "yellow" : "orange") : "clear";
                    h = r.nextInt(5) == 0 ? pick(p, r) : a;
                    if (l.rank(h) < l.rank(a)) h = a; // a bred colour stays hidden in the wild
                }
                default -> {
                    a = p.getFirst().allele;
                    // only the recessive body colours hide in wild fish (a dominant dark one would show)
                    h = l != LbLocus.DARK && r.nextInt(12) == 0 ? p.get(1 + r.nextInt(p.size() - 1)).allele : a;
                }
            }
            b.pair(l, a, h);
        }
        b.x(pick(xPool(endler), r), pick(xPool(endler), r));
        b.y(pick(yPool(endler), r));
        b.endler(endler ? 1F : 0F);
        return b.build();
    }
}
