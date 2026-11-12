package org.overengineer.bettacraft.genetics;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * One gene locus of the simplified Mendel genetics (PLAN 5.2): two alleles per fish, complete dominance by rank
 * (higher rank is expressed; equal rank is broken by the id so the result never depends on the allele order).
 * Unknown allele ids (e.g. from a removed datapack) get the lowest rank and are carried along unchanged.
 *
 * @param id             NBT key and translation part
 * @param pool           alleles used for random fish and mutations
 * @param rank           dominance rank of an allele
 * @param translationKey translation key of an allele
 */
public record Locus(String id, Supplier<List<String>> pool, ToIntFunction<String> rank, Function<String, String> translationKey) {
    /** Larger = more dominant. */
    public int dominance(String allele) {
        return rank.applyAsInt(allele);
    }

    /** The allele that shows (phenotype). */
    public String express(String a, String b) {
        int ra = dominance(a), rb = dominance(b);
        if (ra != rb) return ra > rb ? a : b;
        return a.compareTo(b) <= 0 ? a : b;
    }

    public List<String> alleles() {
        return pool.get();
    }

    public String key(String allele) {
        return translationKey.apply(allele);
    }

    static ToIntFunction<String> table(Map<String, Integer> ranks) {
        return a -> ranks.getOrDefault(a, -1);
    }
}
