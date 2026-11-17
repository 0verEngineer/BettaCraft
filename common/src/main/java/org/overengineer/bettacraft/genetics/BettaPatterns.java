package org.overengineer.bettacraft.genetics;

import java.util.List;

/** Pattern ids; each built-in one has a mask texture {@code textures/entity/betta/pattern_<id>.png} (tools/assetgen). */
public final class BettaPatterns {
    public static final String NONE = "none";

    private BettaPatterns() {}

    /** All patterns of the gene data (without "none"). */
    public static List<String> ids() {
        return GeneticsData.get().alleles(BettaLoci.PATTERN.id()).keySet().stream().filter(p -> !NONE.equals(p)).toList();
    }

    public static boolean isKnown(String id) {
        return GeneticsData.get().alleles(BettaLoci.PATTERN.id()).containsKey(id);
    }

    public static String translationKey(String id) {
        return GeneticsData.translationKey(BettaLoci.PATTERN, id);
    }

    public static String shapeTranslationKey(String shape) {
        return GeneticsData.translationKey(BettaLoci.FINFORM, shape);
    }
}
