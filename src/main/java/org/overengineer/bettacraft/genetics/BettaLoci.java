package org.overengineer.bettacraft.genetics;

import java.util.List;

/**
 * The loci of the betta genotype (PLAN 5.2, M5: nine loci). Ids are stored as strings (NBT key = locus id). The
 * built-in dominance ranks and frequencies are in {@link GeneticsData#DEFAULT_GENES}; datapacks may change them.
 * <ul>
 *     <li>finform (tail shape): round (wild type) &gt; spade &gt; veil &gt; delta &gt; halfmoon &gt; rosetail &gt; crowntail &gt; doubletail</li>
 *     <li>fin_length: short (plakat, wild type) &gt; long</li>
 *     <li>pectoral: normal &gt; dumbo (recessive)</li>
 *     <li>colours: darker colours dominate lighter ones (rank from the luminance of the tint)</li>
 *     <li>pattern: marble &gt; dragon &gt; mask &gt; koi &gt; galaxy &gt; grizzle &gt; piebald &gt; dalmatian &gt; butterfly &gt; none
 *     &gt; fade, bars, tipped</li>
 *     <li>iridescence: high &gt; low &gt; none</li>
 * </ul>
 */
public final class BettaLoci {
    private BettaLoci() {}

    public static final Locus FINFORM = new Locus("finform", Locus.Kind.GENE);
    public static final Locus FIN_LENGTH = new Locus("fin_length", Locus.Kind.GENE);
    public static final Locus PECTORAL = new Locus("pectoral", Locus.Kind.GENE);
    public static final Locus BODY_COLOR = new Locus("body_color", Locus.Kind.COLOR);
    public static final Locus FIN_COLOR = new Locus("fin_color", Locus.Kind.COLOR);
    public static final Locus PATTERN = new Locus("pattern", Locus.Kind.GENE);
    public static final Locus PATTERN_COLOR = new Locus("pattern_color", Locus.Kind.COLOR);
    public static final Locus IRIDESCENCE = new Locus("iridescence", Locus.Kind.GENE);
    public static final Locus IRIDESCENCE_COLOR = new Locus("iridescence_color", Locus.Kind.SHEEN);

    /** All loci in display / storage order. */
    public static final List<Locus> ALL = List.of(FINFORM, FIN_LENGTH, PECTORAL, BODY_COLOR, FIN_COLOR, PATTERN, PATTERN_COLOR, IRIDESCENCE,
            IRIDESCENCE_COLOR);

    /** Loci whose alleles are defined by gene files ({@code genes/<locus>.json}). */
    public static final List<Locus> GENES = List.of(FINFORM, FIN_LENGTH, PECTORAL, PATTERN, IRIDESCENCE);

    public static Locus byId(String id) {
        for (Locus l : ALL) if (l.id().equals(id)) return l;
        return null;
    }

    public static String translationKey(Locus locus) {
        return "locus.bettacraft." + locus.id();
    }
}
