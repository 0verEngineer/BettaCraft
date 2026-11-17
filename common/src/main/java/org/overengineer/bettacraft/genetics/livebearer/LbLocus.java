package org.overengineer.bettacraft.genetics.livebearer;

import java.util.List;
import java.util.Locale;

/**
 * M21.6: the autosomal loci of the guppy and endler genetics ({@link LivebearerGenome}), modelled on real guppy genetics:
 * two alleles per fish, the allele listed first in {@link #alleles} is the most dominant. The body colours are separate
 * recessive mutations of their own loci like in real guppies (gold g, blond b, albino a, blue r), "dark" (melanistic)
 * is dominant. The sex-linked patterns are not here: they sit on the X and the Y chromosome ({@link #X_ALLELES},
 * {@link #Y_ALLELES}).
 */
public enum LbLocus {
    /** Tail shape (real: the swords are dominant, delta and the long tails are polygenic; here one locus). */
    TAIL("round", List.of("lyre", "double_sword", "top_sword", "bottom_sword", "round", "spade", "delta", "triangle", "flag")),
    TAIL_COLOR("clear", List.of("red", "black", "orange", "clear", "yellow", "green", "blue", "white")),
    TAIL_PATTERN("spots", List.of("mosaic", "grass", "leopard", "spots", "none")),
    GOLD("wild", List.of("wild", "gold")),
    BLOND("wild", List.of("wild", "blond")),
    ALBINO("wild", List.of("wild", "albino")),
    BLUE("wild", List.of("wild", "blue")),
    DARK("wild", List.of("dark", "wild"));

    /** Patterns on the X chromosome (males have one X, females two; females show only a faint tuxedo). */
    public static final List<String> X_ALLELES = List.of("tuxedo", "cobra", "moscow", "none");
    /**
     * Patterns on the Y chromosome: only males have one, a son gets his father's Y. "wild" is the orange and black
     * spotting of wild guppy males; peacock, tiger and black bar are the endler patterns.
     */
    public static final List<String> Y_ALLELES = List.of("wild", "cobra", "tuxedo", "japan_blue", "moscow", "red_tail", "red_head", "peacock",
            "tiger", "black_bar", "none");
    public static final List<String> BODY_COLORS = List.of("wild", "gold", "albino", "blond", "white", "blue", "dark");

    private final String wildKey;
    public final List<String> alleles;

    LbLocus(String wild, List<String> alleles) {
        this.wildKey = wild;
        this.alleles = alleles;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The wild-type allele of a guppy. */
    public String wild() {
        return wildKey;
    }

    /** Smaller = more dominant; unknown alleles (newer data) are the most recessive. */
    public int rank(String allele) {
        int i = alleles.indexOf(allele);
        return i < 0 ? alleles.size() : i;
    }

    /** The allele that shows. A top and a bottom sword together make a double sword (like in real guppies). */
    public String express(String a, String b) {
        if (this == TAIL && (a.equals("top_sword") && b.equals("bottom_sword") || a.equals("bottom_sword") && b.equals("top_sword"))) return "double_sword";
        int ra = rank(a), rb = rank(b);
        if (ra != rb) return ra < rb ? a : b;
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static int xRank(String allele) {
        int i = X_ALLELES.indexOf(allele);
        return i < 0 ? X_ALLELES.size() : i;
    }

    public static LbLocus byId(String id) {
        for (LbLocus l : values()) if (l.id().equals(id)) return l;
        return null;
    }
}
