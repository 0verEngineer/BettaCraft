package org.overengineer.bettacraft.tankmates;

import java.util.random.RandomGenerator;
import net.minecraft.nbt.CompoundTag;

/**
 * M19: the colour variety of a tankmate as one Mendelian locus with two alleles (variety ids of the species). The wild
 * type (the first variety) is dominant over every bred variety; of two different bred varieties the one listed first in
 * {@link TankmateSpecies#varieties()} shows. So an albino bristlenose needs two albino alleles, a guppy from a red and a
 * blue grass parent looks like its red parent and carries blue grass.
 */
public record TankmateGenes(String a, String b) {
    public static TankmateGenes wild(TankmateSpecies s) {
        String w = s.variety(0);
        return new TankmateGenes(w, w);
    }

    public static TankmateGenes pure(TankmateSpecies s, String variety) {
        String v = s.varieties().contains(variety) ? variety : s.variety(0);
        return new TankmateGenes(v, v);
    }

    /** The variety that shows. */
    public String phenotype(TankmateSpecies s) {
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

    /** One allele from each parent, chosen at random. */
    public static TankmateGenes cross(TankmateGenes mother, TankmateGenes father, RandomGenerator r) {
        return new TankmateGenes(r.nextBoolean() ? mother.a : mother.b, r.nextBoolean() ? father.a : father.b);
    }

    /**
     * Wild animals: the wild type, now and then hiding a bred variety ({@code carrier} chance per allele of a random
     * other variety), the way albino bristlenoses turn up in normal broods.
     */
    public static TankmateGenes wild(TankmateSpecies s, double carrier, RandomGenerator r) {
        String w = s.variety(0);
        if (s.varieties().size() < 2 || carrier <= 0) return new TankmateGenes(w, w);
        String a = r.nextDouble() < carrier ? s.variety(1 + r.nextInt(s.varieties().size() - 1)) : w;
        return new TankmateGenes(a, w);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("a", a);
        t.putString("b", b);
        return t;
    }

    public static TankmateGenes load(CompoundTag t, TankmateSpecies s) {
        String w = s.variety(0);
        String a = t.getString("a"), b = t.getString("b");
        return new TankmateGenes(a.isEmpty() ? w : a, b.isEmpty() ? w : b);
    }

    /** "red/blue_grass" for the menu tooltip. */
    public String text() {
        return a + "/" + b;
    }
}
