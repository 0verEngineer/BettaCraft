package org.overengineer.bettacraft.genetics.livebearer;

import java.util.Map;

/**
 * M21.6: the fixed varieties up to M21.5 (guppy: wild, red, blue grass, green cobra, tuxedo; endler: wild, tiger,
 * black bar) as genomes, for fish of older worlds, buckets and bags: a fish with the variety alleles {@code a/b} gets
 * per locus the alleles of the two varieties' haplotypes, the Y of the variety it showed. Deterministic (no chance), so
 * an old fish converts the same way on every load until it is saved with its genome. The haplotypes keep the old look
 * as far as possible: the delta tail of the old guppy model, the orange-edged double sword of the old endler tail.
 */
public final class LivebearerLegacy {
    private LivebearerLegacy() {}

    /** One variety: tail, tail colour, tail pattern, body colour locus (or ""), X and Y pattern. */
    private record H(String tail, String color, String pattern, String body, String x, String y) {}

    private static final Map<String, H> GUPPY = Map.of(
            "wild", new H("delta", "clear", "spots", "", "none", "wild"),
            "red", new H("delta", "red", "none", "", "none", "red_tail"),
            "blue_grass", new H("delta", "blue", "grass", "blue", "none", "none"),
            "green_cobra", new H("delta", "green", "leopard", "", "none", "cobra"),
            "tuxedo", new H("delta", "red", "none", "", "tuxedo", "tuxedo"));
    private static final Map<String, H> ENDLER = Map.of(
            "wild", new H("double_sword", "orange", "spots", "", "none", "peacock"),
            "tiger", new H("double_sword", "orange", "spots", "", "none", "tiger"),
            "black_bar", new H("double_sword", "orange", "spots", "", "none", "black_bar"));

    /** The strain an old variety becomes. */
    public static final Map<String, LivebearerStrain> GUPPY_STRAINS = Map.of("wild", LivebearerStrain.WILD_GUPPY, "red", LivebearerStrain.FULL_RED,
            "blue_grass", LivebearerStrain.BLUE_GRASS, "green_cobra", LivebearerStrain.GREEN_COBRA, "tuxedo", LivebearerStrain.TUXEDO);
    public static final Map<String, LivebearerStrain> ENDLER_STRAINS = Map.of("wild", LivebearerStrain.ENDLER_WILD, "tiger", LivebearerStrain.ENDLER_TIGER,
            "black_bar", LivebearerStrain.ENDLER_BLACK_BAR);

    private static H h(boolean endler, String variety) {
        Map<String, H> m = endler ? ENDLER : GUPPY;
        H h = m.get(variety);
        return h == null ? m.get("wild") : h;
    }

    /**
     * The genome of an old fish with the variety alleles {@code a} and {@code b}; {@code shown} is the variety it showed
     * (gives the Y).
     */
    public static LivebearerGenome convert(boolean endler, String a, String b, String shown) {
        H ha = h(endler, a), hb = h(endler, b), hs = h(endler, shown);
        LivebearerGenome.Builder g = LivebearerGenome.builder(endler);
        // the shown variety's alleles first: with equal dominance its look wins
        H first = hs, second = hs == ha ? hb : ha;
        g.pair(LbLocus.TAIL, first.tail, second.tail);
        g.pair(LbLocus.TAIL_COLOR, first.color, second.color);
        g.pair(LbLocus.TAIL_PATTERN, first.pattern, second.pattern);
        for (LbLocus l : new LbLocus[] {LbLocus.GOLD, LbLocus.BLOND, LbLocus.ALBINO, LbLocus.BLUE, LbLocus.DARK}) {
            String fa = first.body.equals(l.id()) ? l.id() : l.wild(), fb = second.body.equals(l.id()) ? l.id() : l.wild();
            g.pair(l, fa, fb);
        }
        g.x(first.x, second.x);
        g.y(hs.y);
        g.endler(endler ? 1F : 0F);
        return g.build();
    }

    /** Whether this is a species with the genetics (guppy or endler) by id. */
    public static boolean livebearer(String speciesId) {
        return speciesId.equals("guppy") || speciesId.equals("endler_guppy");
    }
}
