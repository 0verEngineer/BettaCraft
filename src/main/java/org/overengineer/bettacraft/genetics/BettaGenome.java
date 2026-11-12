package org.overengineer.bettacraft.genetics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.util.RandomSource;

/**
 * Basic betta genome of M1: one value per locus (phenotype). M2 turns every locus into an allele pair (schema
 * migration duplicates the value). All values are string ids so that reordering or removing values never breaks
 * saved data; unknown ids are carried along unchanged and rendered with a neutral fallback.
 *
 * @param species     species id (prepared for further species, PLAN 2)
 * @param shape       body / fin shape: plakat, veil, halfmoon, crowntail, doubletail
 * @param pectoral    normal or dumbo
 * @param bodyColor   colour id of {@link BettaColors}
 * @param finColor    colour id
 * @param pattern     "none" or a pattern of {@link BettaPatterns}
 * @param patternColor colour id of the pattern
 * @param iridescence none, low, high
 * @param iridescenceColor colour id of the sheen
 */
public record BettaGenome(String species, String shape, String pectoral, String bodyColor, String finColor,
                          String pattern, String patternColor, String iridescence, String iridescenceColor) {
    public static final String SPECIES_BETTA = "bettacraft:betta";
    public static final List<String> SHAPES = List.of("plakat", "veil", "halfmoon", "crowntail", "doubletail");
    public static final List<String> IRIDESCENCE = List.of("none", "low", "high");

    /** Wild Betta splendens: plakat, dull colours, iridescent scales. */
    private static final List<String> WILD_BODY = List.of("olive", "bronze", "chocolate", "smoke", "oxblood", "copper", "cinnamon");
    private static final List<String> WILD_FINS = List.of("oxblood", "red", "crimson", "olive", "smoke", "wine");
    private static final List<String> WILD_SHEEN = List.of("turquoise", "teal", "emerald", "steel");

    public static final Codec<BettaGenome> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("species", SPECIES_BETTA).forGetter(BettaGenome::species),
            Codec.STRING.optionalFieldOf("shape", "plakat").forGetter(BettaGenome::shape),
            Codec.STRING.optionalFieldOf("pectoral", "normal").forGetter(BettaGenome::pectoral),
            Codec.STRING.optionalFieldOf("body_color", "olive").forGetter(BettaGenome::bodyColor),
            Codec.STRING.optionalFieldOf("fin_color", "oxblood").forGetter(BettaGenome::finColor),
            Codec.STRING.optionalFieldOf("pattern", "none").forGetter(BettaGenome::pattern),
            Codec.STRING.optionalFieldOf("pattern_color", "black").forGetter(BettaGenome::patternColor),
            Codec.STRING.optionalFieldOf("iridescence", "low").forGetter(BettaGenome::iridescence),
            Codec.STRING.optionalFieldOf("iridescence_color", "turquoise").forGetter(BettaGenome::iridescenceColor)
    ).apply(i, BettaGenome::new));

    public static final BettaGenome DEFAULT = new BettaGenome(SPECIES_BETTA, "plakat", "normal", "olive", "oxblood", "none",
            "black", "low", "turquoise");

    public boolean isDumbo() {
        return "dumbo".equals(pectoral);
    }

    public BettaGenome withShape(String s) {
        return new BettaGenome(species, s, pectoral, bodyColor, finColor, pattern, patternColor, iridescence, iridescenceColor);
    }

    /** Wild type found in swamps. */
    public static BettaGenome randomWild(RandomSource r) {
        String body = pick(r, WILD_BODY);
        String fins = r.nextInt(3) == 0 ? body : pick(r, WILD_FINS);
        String pattern = r.nextInt(4) == 0 ? "fade" : "none";
        return new BettaGenome(SPECIES_BETTA, "plakat", "normal", body, fins, pattern, "black",
                r.nextInt(3) == 0 ? "high" : "low", pick(r, WILD_SHEEN));
    }

    /** Any combination (spawn eggs, creative, tests). */
    public static BettaGenome randomDomestic(RandomSource r) {
        List<String> colors = BettaColors.ids();
        String body = pick(r, colors);
        String fins = r.nextInt(3) == 0 ? body : pick(r, colors);
        String pattern = r.nextInt(3) == 0 ? "none" : pick(r, BettaPatterns.ids());
        return new BettaGenome(SPECIES_BETTA, pick(r, SHAPES), r.nextInt(6) == 0 ? "dumbo" : "normal", body, fins, pattern,
                pick(r, colors), pick(r, IRIDESCENCE), pick(r, BettaColors.iridescentIds()));
    }

    private static String pick(RandomSource r, List<String> l) {
        return l.get(r.nextInt(l.size()));
    }
}
