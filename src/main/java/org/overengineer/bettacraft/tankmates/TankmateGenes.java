package org.overengineer.bettacraft.tankmates;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.overengineer.bettacraft.genetics.livebearer.LivebearerGenome;
import org.overengineer.bettacraft.genetics.livebearer.LivebearerLegacy;
import org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain;

/**
 * M19: the colour variety of a tankmate as one Mendelian locus with two alleles (variety ids of the species). The wild
 * type (the first variety) is dominant over every bred variety; of two different bred varieties the one listed first in
 * {@link TankmateSpecies#varieties()} shows. So an albino bristlenose needs two albino alleles, a guppy from a red and a
 * blue grass parent looks like its red parent and carries blue grass.
 * <p>
 * M21.6: guppies and endlers also carry a full {@link LivebearerGenome} ({@code lb}; tail shape, colours, patterns, the
 * X and Y chromosome, the endler share). The variety alleles are still inherited alongside it, so the old fixed
 * varieties come back when {@code features.tankmates.livebearerGenetics} is switched off. Fish without a genome (older worlds)
 * get one from their varieties when loaded ({@link LivebearerLegacy}).
 */
public record TankmateGenes(String a, String b, @Nullable LivebearerGenome lb) {
    public TankmateGenes(String a, String b) {
        this(a, b, null);
    }

    public static TankmateGenes wild(TankmateSpecies s) {
        String w = s.variety(0);
        return withLegacy(s, w, w);
    }

    public static TankmateGenes pure(TankmateSpecies s, String variety) {
        String v = s.varieties().contains(variety) ? variety : s.variety(0);
        return withLegacy(s, v, v);
    }

    /** M21.6: a pure-bred named guppy or endler strain (the varieties: the closest old one). */
    public static TankmateGenes strain(TankmateSpecies s, LivebearerStrain strain) {
        String v = s.varieties().contains(strain.legacy) ? strain.legacy : s.variety(0);
        return new TankmateGenes(v, v, strain.genome());
    }

    /** Varieties plus, for a guppy or an endler, the genome those varieties stand for. */
    private static TankmateGenes withLegacy(TankmateSpecies s, String a, String b) {
        return new TankmateGenes(a, b, livebearer(s) ? LivebearerLegacy.convert(s == TankmateSpecies.ENDLER_GUPPY, a, b, phenotype(s, a, b)) : null);
    }

    /** The species with a livebearer genome. */
    public static boolean livebearer(TankmateSpecies s) {
        return s == TankmateSpecies.GUPPY || s == TankmateSpecies.ENDLER_GUPPY;
    }

    /** The variety that shows. */
    public String phenotype(TankmateSpecies s) {
        return phenotype(s, a, b);
    }

    private static String phenotype(TankmateSpecies s, String a, String b) {
        int ia = s.varietyIndex(a), ib = s.varietyIndex(b);
        if (!s.varieties().contains(a)) ia = 0;
        if (!s.varieties().contains(b)) ib = 0;
        return s.variety(Math.min(ia, ib));
    }

    public boolean carries(String variety) {
        return a.equals(variety) || b.equals(variety);
    }

    public boolean pure() {
        return a.equals(b);
    }

    /** One allele from each parent, chosen at random (the sex of the young at random too, for the genome). */
    public static TankmateGenes cross(TankmateGenes mother, TankmateGenes father, RandomGenerator r) {
        return cross(mother, father, r.nextBoolean(), r);
    }

    /** M21.6: a young of the given sex (the X and Y of the livebearer genome need it). */
    public static TankmateGenes cross(TankmateGenes mother, TankmateGenes father, boolean female, RandomGenerator r) {
        String a = r.nextBoolean() ? mother.a : mother.b, b = r.nextBoolean() ? father.a : father.b;
        LivebearerGenome g = null;
        if (mother.lb != null && father.lb != null) g = LivebearerGenome.cross(mother.lb, father.lb, female, r, LivebearerGenome.MUTATION);
        else if (mother.lb != null) g = mother.lb;
        return new TankmateGenes(a, b, g);
    }

    /**
     * Wild animals: the wild type, now and then hiding a bred variety ({@code carrier} chance per allele of a random
     * other variety), the way albino bristlenoses turn up in normal broods. M21.6: a guppy or an endler gets a random
     * wild genome (with {@code features.tankmates.livebearerGenetics}; otherwise the one of its variety).
     */
    public static TankmateGenes wild(TankmateSpecies s, double carrier, RandomGenerator r) {
        String w = s.variety(0);
        String a = w;
        if (s.varieties().size() >= 2 && carrier > 0 && r.nextDouble() < carrier) a = s.variety(1 + r.nextInt(s.varieties().size() - 1));
        if (livebearer(s) && Features.enabled(Feature.LIVEBEARER_GENETICS)) {
            return new TankmateGenes(a, w, LivebearerGenome.wild(s == TankmateSpecies.ENDLER_GUPPY, r));
        }
        return withLegacy(s, a, w);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("a", a);
        t.putString("b", b);
        if (lb != null) t.put("lb", lb.save());
        return t;
    }

    public static TankmateGenes load(CompoundTag t, TankmateSpecies s) {
        String w = s.variety(0);
        String a = t.getString("a"), b = t.getString("b");
        a = a.isEmpty() ? w : a;
        b = b.isEmpty() ? w : b;
        if (!livebearer(s)) return new TankmateGenes(a, b, null);
        if (t.contains("lb", Tag.TAG_COMPOUND)) return new TankmateGenes(a, b, LivebearerGenome.load(t.getCompound("lb"), s == TankmateSpecies.ENDLER_GUPPY));
        return withLegacy(s, a, b);
    }

    /** "red/blue_grass" for the menu tooltip. */
    public String text() {
        return a + "/" + b;
    }
}
