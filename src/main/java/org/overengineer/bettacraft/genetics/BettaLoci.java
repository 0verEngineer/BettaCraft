package org.overengineer.bettacraft.genetics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * The loci of the betta genotype (PLAN 5.2) and their dominance rules. Ids are stored as strings (NBT key = locus id).
 * <ul>
 *     <li>finform: plakat (wild type) &gt; veil &gt; halfmoon &gt; crowntail &gt; doubletail</li>
 *     <li>pectoral: normal &gt; dumbo (recessive)</li>
 *     <li>colours: darker colours dominate lighter ones (rank from the luminance of the tint)</li>
 *     <li>pattern: marble &gt; dragon &gt; mask &gt; koi &gt; grizzle &gt; butterfly &gt; none &gt; fade, bars, tipped</li>
 *     <li>iridescence: high &gt; low &gt; none</li>
 * </ul>
 */
public final class BettaLoci {
    private BettaLoci() {}

    private static final ToIntFunction<String> COLOR_RANK = id -> {
        if (!BettaColors.isKnown(id)) return -1;
        int rgb = BettaColors.rgb(id);
        double luma = 0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF);
        return 1000 - (int) Math.round(luma);
    };

    public static final Locus FINFORM = new Locus("finform", () -> BettaGenome.SHAPES,
            Locus.table(Map.of("plakat", 4, "veil", 3, "halfmoon", 2, "crowntail", 1, "doubletail", 0)),
            BettaPatterns::shapeTranslationKey);
    public static final Locus PECTORAL = new Locus("pectoral", () -> List.of("normal", "dumbo"),
            Locus.table(Map.of("normal", 1, "dumbo", 0)), a -> "pectoral.bettacraft." + a);
    public static final Locus BODY_COLOR = new Locus("body_color", BettaColors::ids, COLOR_RANK, BettaColors::translationKey);
    public static final Locus FIN_COLOR = new Locus("fin_color", BettaColors::ids, COLOR_RANK, BettaColors::translationKey);
    public static final Locus PATTERN = new Locus("pattern", BettaLoci::patternPool,
            Locus.table(Map.of("marble", 8, "dragon", 7, "mask", 6, "koi", 5, "grizzle", 4, "butterfly", 3, "none", 2,
                    "fade", 1, "bars", 1, "tipped", 1)), BettaPatterns::translationKey);
    public static final Locus PATTERN_COLOR = new Locus("pattern_color", BettaColors::ids, COLOR_RANK, BettaColors::translationKey);
    public static final Locus IRIDESCENCE = new Locus("iridescence", () -> BettaGenome.IRIDESCENCE,
            Locus.table(Map.of("high", 2, "low", 1, "none", 0)), a -> "iridescence.bettacraft." + a);
    public static final Locus IRIDESCENCE_COLOR = new Locus("iridescence_color", BettaColors::iridescentIds, COLOR_RANK,
            BettaColors::translationKey);

    /** All loci in display / storage order. */
    public static final List<Locus> ALL = List.of(FINFORM, PECTORAL, BODY_COLOR, FIN_COLOR, PATTERN, PATTERN_COLOR, IRIDESCENCE,
            IRIDESCENCE_COLOR);

    private static List<String> patternPool() {
        List<String> l = new ArrayList<>(BettaPatterns.ids());
        l.add(BettaPatterns.NONE);
        return l;
    }

    public static Locus byId(String id) {
        for (Locus l : ALL) if (l.id().equals(id)) return l;
        return null;
    }

    public static String translationKey(Locus locus) {
        return "locus.bettacraft." + locus.id();
    }
}
