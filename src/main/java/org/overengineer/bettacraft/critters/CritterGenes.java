package org.overengineer.bettacraft.critters;

import java.util.random.RandomGenerator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Genetics of a shrimp, crayfish or snail (M14). Every colour of a species is a <b>recessive allele at its own
 * locus</b> against the wild colour (Mendel): an animal shows a colour only when it carries it twice. Crossing two
 * different colours gives wild-coloured young that carry both (the F1 "falls back to the wild form"), and an animal that
 * shows two colours at once looks wild as well (mixed lines; for Neocaridina black + yellow is the exception: chocolate).
 * <p>
 * On top of that a quantitative <b>grade</b> (colour density, 0..1) is inherited as the mean of both parents with a
 * little noise and a slight pull towards the average: bred up by keeping only the best animals for many generations.
 * For the red Neocaridina line the grade names the variety: Red Cherry, Sakura, Fire Red, Painted Fire Red.
 *
 * @param a     first allele of every locus (bit i set = the colour allele of locus i)
 * @param b     second allele of every locus
 * @param grade colour density 0..1
 */
public record CritterGenes(int a, int b, float grade) {
    public static final CritterGenes WILD = new CritterGenes(0, 0, 0.1F);
    /** Grade the offspring is pulled towards a little (an unselected line drifts back to average). */
    public static final float AVERAGE_GRADE = 0.25F;
    public static final float GRADE_NOISE = 0.06F;
    public static final float GRADE_PULL = 0.08F;

    public CritterGenes {
        grade = Math.max(0, Math.min(1, grade));
    }

    /** Bits of the loci where both alleles are the colour allele. */
    public int homozygous() {
        return a & b;
    }

    /** Bits of the loci that carry the colour allele at least once. */
    public int carried() {
        return a | b;
    }

    public boolean shows(int locus) {
        return (homozygous() & (1 << locus)) != 0;
    }

    public boolean carries(int locus) {
        return (carried() & (1 << locus)) != 0;
    }

    /** An animal that shows the colour of {@code locus} (pure line) with the given grade. */
    public static CritterGenes pure(int locus, float grade) {
        int m = locus < 0 ? 0 : 1 << locus;
        return new CritterGenes(m, m, grade);
    }

    /**
     * The young of {@code mother} and {@code father}: one allele of every locus from each parent (at random), the
     * grade as the parents' mean plus noise, pulled a little towards {@link #AVERAGE_GRADE}.
     */
    public static CritterGenes cross(CritterGenes mother, CritterGenes father, int loci, RandomGenerator rnd) {
        int ca = 0, cb = 0;
        for (int i = 0; i < loci; i++) {
            int bit = 1 << i;
            if (((rnd.nextBoolean() ? mother.a : mother.b) & bit) != 0) ca |= bit;
            if (((rnd.nextBoolean() ? father.a : father.b) & bit) != 0) cb |= bit;
        }
        float mean = (mother.grade + father.grade) / 2F;
        float g = mean + GRADE_PULL * (AVERAGE_GRADE - mean) + (float) (rnd.nextGaussian() * GRADE_NOISE);
        return new CritterGenes(ca, cb, g);
    }

    /**
     * A wild animal: wild colour, a low grade, and with {@code carrierChance} per locus one hidden colour allele (so a
     * wild red-brown ramshorn can carry pink; breeding brings it out).
     */
    public static CritterGenes wild(int loci, double carrierChance, RandomGenerator rnd) {
        int a = 0;
        for (int i = 0; i < loci; i++) if (rnd.nextDouble() < carrierChance) a |= 1 << i;
        return new CritterGenes(a, 0, (float) (rnd.nextDouble() * 0.2));
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("a", a);
        t.putInt("b", b);
        t.putFloat("grade", grade);
        return t;
    }

    public static CritterGenes load(CompoundTag t) {
        return new CritterGenes(t.getInt("a"), t.getInt("b"), t.contains("grade", Tag.TAG_ANY_NUMERIC) ? t.getFloat("grade") : 0.1F);
    }

    // ------------------------------------------------------------------ phenotype

    /** Wild colour (no colour shown, or several colours at once). */
    public static final int WILD_COLOR = -1;
    /** Neocaridina black + yellow at once. */
    public static final int CHOCOLATE = 100;

    /**
     * The colour an animal of {@code species} shows: the index of its locus, {@link #WILD_COLOR}, or (Neocaridina)
     * {@link #CHOCOLATE}.
     */
    public int color(CritterSpecies species) {
        int n = species.loci().length;
        int hom = homozygous() & ((1 << n) - 1);
        if (hom == 0) return WILD_COLOR;
        if (Integer.bitCount(hom) == 1) return Integer.numberOfTrailingZeros(hom);
        if (species == CritterSpecies.NEOCARIDINA && hom == ((1 << 2) | (1 << 5))) return CHOCOLATE; // yellow + black
        return WILD_COLOR;
    }

    /**
     * Id of the variety (for names and the menu): {@code wild} or the colour; red Neocaridina by grade
     * ({@code red_cherry}, {@code sakura}, {@code fire_red}, {@code painted_fire_red}).
     */
    public String variety(CritterSpecies species) {
        int c = color(species);
        if (c == CHOCOLATE) return "chocolate";
        if (c == WILD_COLOR) return "wild";
        String locus = species.loci()[c];
        if (species == CritterSpecies.NEOCARIDINA) {
            return switch (locus) {
                case "red" -> grade < 0.3F ? "red_cherry" : grade < 0.55F ? "sakura" : grade < 0.8F ? "fire_red" : "painted_fire_red";
                case "blue" -> "blue_dream";
                case "green" -> "green_jade";
                case "black" -> "black_rose";
                default -> locus;
            };
        }
        return locus;
    }

    /** Translation key of the variety name, e.g. {@code variety.bettacraft.neocaridina.sakura}. */
    public String varietyKey(CritterSpecies species) {
        return "variety.bettacraft." + species.id() + "." + variety(species);
    }

    /** Body (shell) colour as RGB: the colour of the variety, denser with a higher grade. */
    public int rgb(CritterSpecies species) {
        return rgb(species, color(species), grade);
    }

    /** Body colour of a variety ({@code color} as from {@link #color}) at a grade (also on the client, which gets no genes). */
    public static int rgb(CritterSpecies species, int color, float grade) {
        int full = fullColor(species, color);
        if (!species.hasGrade() || color == WILD_COLOR) return full;
        // a low grade shows a washed-out, partly translucent colour: mix with the pale wild tone
        int pale = species == CritterSpecies.NEOCARIDINA ? 0xC8B8A0 : 0xB8A890;
        float f = 0.35F + 0.65F * Math.max(0, Math.min(1, grade));
        return mix(pale, full, f);
    }

    /** Full colour of a variety ({@code color} as from {@link #color}). */
    public static int fullColor(CritterSpecies species, int color) {
        if (color == CHOCOLATE) return 0x4A2A1A;
        if (color < 0 || color >= species.loci().length) return species.wildColor();
        return switch (species.id() + ":" + species.loci()[color]) {
            case "neocaridina:red" -> 0xD8241A;
            case "neocaridina:blue" -> 0x2452C0;
            case "neocaridina:yellow" -> 0xF0C820;
            case "neocaridina:orange" -> 0xF2781C;
            case "neocaridina:green" -> 0x2E7A40;
            case "neocaridina:black" -> 0x1C1A24;
            case "dwarf_crayfish:orange" -> 0xF06A1A;
            case "apple_snail:gold" -> 0xE8B828;
            case "apple_snail:blue" -> 0x5A7CB0;
            case "apple_snail:purple" -> 0x6A3A72;
            case "apple_snail:ivory" -> 0xEEE6D2;
            case "ramshorn:pink" -> 0xEC9AB2;
            case "ramshorn:blue" -> 0x6E92D0;
            default -> species.wildColor();
        };
    }

    public static int mix(int c0, int c1, float f) {
        f = Math.max(0, Math.min(1, f));
        int r = Math.round(((c0 >> 16) & 0xFF) * (1 - f) + ((c1 >> 16) & 0xFF) * f);
        int g = Math.round(((c0 >> 8) & 0xFF) * (1 - f) + ((c1 >> 8) & 0xFF) * f);
        int b = Math.round((c0 & 0xFF) * (1 - f) + (c1 & 0xFF) * f);
        return (r << 16) | (g << 8) | b;
    }

    /** Genotype text like {@code red/red blue/+} (+ = wild allele), loci the animal has no colour allele at are left out. */
    public String genotypeText(CritterSpecies species) {
        StringBuilder sb = new StringBuilder();
        String[] loci = species.loci();
        for (int i = 0; i < loci.length; i++) {
            boolean x = (a & (1 << i)) != 0, y = (b & (1 << i)) != 0;
            if (!x && !y) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(x ? loci[i] : "+").append('/').append(y ? loci[i] : "+");
        }
        return sb.isEmpty() ? "+/+" : sb.toString();
    }
}
