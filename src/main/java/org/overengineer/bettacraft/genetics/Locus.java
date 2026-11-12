package org.overengineer.bettacraft.genetics;

import java.util.List;
import net.minecraft.util.RandomSource;

/**
 * One gene locus of the simplified Mendel genetics (PLAN 5.2): two alleles per fish, complete dominance by rank
 * (higher rank is expressed; equal rank is broken by the id so the result never depends on the allele order).
 * Unknown allele ids (e.g. from a removed datapack) get the lowest rank and are carried along unchanged.
 * <p>
 * Alleles, ranks and frequencies come from {@link GeneticsData} (built-in defaults, replaced by the datapack files
 * {@code data/<ns>/bettacraft/genes/*.json} and {@code .../colors/*.json} on the server and synced to clients).
 *
 * @param id   NBT key and translation part
 * @param kind where the alleles come from
 */
public record Locus(String id, Kind kind) {
    public enum Kind {
        /** alleles from {@code genes/<id>.json} */
        GENE,
        /** every colour of {@code colors/} */
        COLOR,
        /** colours flagged {@code iridescent} */
        SHEEN
    }

    /** Larger = more dominant. */
    public int dominance(String allele) {
        return GeneticsData.get().rank(this, allele);
    }

    /** The allele that shows (phenotype). */
    public String express(String a, String b) {
        int ra = dominance(a), rb = dominance(b);
        if (ra != rb) return ra > rb ? a : b;
        return a.compareTo(b) <= 0 ? a : b;
    }

    /** Alleles that random fish and mutations can get (frequency weight &gt; 0). */
    public List<String> alleles() {
        return GeneticsData.get().pool(this);
    }

    /** A random allele, weighted by the frequencies of the gene data. */
    public String pick(RandomSource r) {
        return GeneticsData.get().pick(this, r);
    }

    public String key(String allele) {
        return GeneticsData.translationKey(this, allele);
    }
}
