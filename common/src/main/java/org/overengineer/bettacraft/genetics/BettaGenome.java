package org.overengineer.bettacraft.genetics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.RandomSource;

/**
 * The visible fish (phenotype): one value per locus, expressed from the {@link BettaGenotype}. All values are string
 * ids so that reordering or removing values never breaks saved data; unknown ids are carried along unchanged and
 * rendered with a neutral fallback.
 *
 * @param species          species id (prepared for further species, PLAN 2)
 * @param shape            tail shape (finform): round, spade, veil, delta, halfmoon, rosetail, crowntail, doubletail
 * @param finLength        short (plakat types) or long
 * @param pectoral         normal or dumbo
 * @param bodyColor        colour id of {@link BettaColors}
 * @param finColor         colour id
 * @param pattern          "none" or a pattern of {@link BettaPatterns}
 * @param patternColor     colour id of the pattern
 * @param iridescence      none, low, high
 * @param iridescenceColor colour id of the sheen
 */
public record BettaGenome(String species, String shape, String finLength, String pectoral, String bodyColor, String finColor,
                          String pattern, String patternColor, String iridescence, String iridescenceColor) {
    public static final String SPECIES_BETTA = "bettacraft:betta";
    /** Tail shapes with a model (BettaModelDefinitions.Shape). */
    public static final List<String> SHAPES = List.of("round", "spade", "veil", "delta", "halfmoon", "rosetail", "crowntail", "doubletail");
    public static final String SHORT = "short", LONG = "long";
    public static final List<String> IRIDESCENCE = List.of("none", "low", "high");

    /** Wild Betta splendens: short-finned roundtail (plakat), dull colours, iridescent scales. */
    private static final List<String> WILD_BODY = List.of("olive", "bronze", "chocolate", "smoke", "oxblood", "copper", "cinnamon");
    private static final List<String> WILD_FINS = List.of("oxblood", "red", "crimson", "olive", "smoke", "wine");
    private static final List<String> WILD_SHEEN = List.of("turquoise", "teal", "emerald", "steel");

    /** Synced look of an entity (not persisted; the genotype is). */
    public static final Codec<BettaGenome> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("species", SPECIES_BETTA).forGetter(BettaGenome::species),
            Codec.STRING.optionalFieldOf("shape", "round").forGetter(BettaGenome::shape),
            Codec.STRING.optionalFieldOf("fin_length", SHORT).forGetter(BettaGenome::finLength),
            Codec.STRING.optionalFieldOf("pectoral", "normal").forGetter(BettaGenome::pectoral),
            Codec.STRING.optionalFieldOf("body_color", "olive").forGetter(BettaGenome::bodyColor),
            Codec.STRING.optionalFieldOf("fin_color", "oxblood").forGetter(BettaGenome::finColor),
            Codec.STRING.optionalFieldOf("pattern", "none").forGetter(BettaGenome::pattern),
            Codec.STRING.optionalFieldOf("pattern_color", "black").forGetter(BettaGenome::patternColor),
            Codec.STRING.optionalFieldOf("iridescence", "low").forGetter(BettaGenome::iridescence),
            Codec.STRING.optionalFieldOf("iridescence_color", "turquoise").forGetter(BettaGenome::iridescenceColor)
    ).apply(i, BettaGenome::new));

    public static final BettaGenome DEFAULT = new BettaGenome(SPECIES_BETTA, "round", SHORT, "normal", "olive", "oxblood", "none",
            "black", "low", "turquoise");

    public boolean isDumbo() {
        return "dumbo".equals(pectoral);
    }

    public boolean isShortFinned() {
        return SHORT.equals(finLength);
    }

    /** Value of a locus by id (null for unknown loci). */
    @Nullable
    public String value(String locus) {
        return switch (locus) {
            case "finform" -> shape;
            case "fin_length" -> finLength;
            case "pectoral" -> pectoral;
            case "body_color" -> bodyColor;
            case "fin_color" -> finColor;
            case "pattern" -> pattern;
            case "pattern_color" -> patternColor;
            case "iridescence" -> iridescence;
            case "iridescence_color" -> iridescenceColor;
            case "species" -> species;
            default -> null;
        };
    }

    /** Phenotype from locus id -> allele (missing loci keep the value of {@code base}). */
    public static BettaGenome of(java.util.Map<String, String> v, BettaGenome base) {
        return new BettaGenome(base.species, v.getOrDefault("finform", base.shape), v.getOrDefault("fin_length", base.finLength),
                v.getOrDefault("pectoral", base.pectoral), v.getOrDefault("body_color", base.bodyColor),
                v.getOrDefault("fin_color", base.finColor), v.getOrDefault("pattern", base.pattern),
                v.getOrDefault("pattern_color", base.patternColor), v.getOrDefault("iridescence", base.iridescence),
                v.getOrDefault("iridescence_color", base.iridescenceColor));
    }

    public BettaGenome withShape(String s) {
        return new BettaGenome(species, s, finLength, pectoral, bodyColor, finColor, pattern, patternColor, iridescence, iridescenceColor);
    }

    public BettaGenome withFinLength(String l) {
        return new BettaGenome(species, shape, l, pectoral, bodyColor, finColor, pattern, patternColor, iridescence, iridescenceColor);
    }

    /** Wild type found in swamps. */
    public static BettaGenome randomWild(RandomSource r) {
        String body = pick(r, WILD_BODY);
        String fins = r.nextInt(3) == 0 ? body : pick(r, WILD_FINS);
        String pattern = r.nextInt(4) == 0 ? "fade" : "none";
        return new BettaGenome(SPECIES_BETTA, "round", SHORT, "normal", body, fins, pattern, "black",
                r.nextInt(3) == 0 ? "high" : "low", pick(r, WILD_SHEEN));
    }

    /** Any combination (spawn eggs, creative, tests). */
    public static BettaGenome randomDomestic(RandomSource r) {
        List<String> colors = BettaColors.ids();
        String body = pick(r, colors);
        String fins = r.nextInt(3) == 0 ? body : pick(r, colors);
        return new BettaGenome(SPECIES_BETTA, BettaLoci.FINFORM.pick(r), BettaLoci.FIN_LENGTH.pick(r), BettaLoci.PECTORAL.pick(r), body,
                fins, BettaLoci.PATTERN.pick(r), pick(r, colors), BettaLoci.IRIDESCENCE.pick(r), pick(r, BettaColors.iridescentIds()));
    }

    private static String pick(RandomSource r, List<String> l) {
        return l.get(r.nextInt(l.size()));
    }
}
