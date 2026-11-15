package org.overengineer.bettacraft.genetics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;

/**
 * Genotype of a betta (M2): an allele pair per {@link BettaLoci locus} plus the species (M29 part c: the
 * {@link BettaSpecies.Ancestry ancestry} of the Betta splendens complex; the species id follows from it). The visible fish
 * ({@link BettaGenome}, used by the renderer) is {@link #express() expressed} from it.
 * <p>
 * NBT: {@code {species:"bettacraft:betta", ancestry:{imbellis:32, smaragdina:0}, finform:["plakat","veil"], ...}} (no
 * ancestry: the pure species of the id); keys this version does not
 * know are kept in {@link #extra} and written back unchanged. Pure logic (unit-tested without a Minecraft bootstrap).
 */
public final class BettaGenotype {
    public record AllelePair(String a, String b) {
        public boolean homozygous() {
            return a.equals(b);
        }

        public String expressed(Locus l) {
            return l.express(a, b);
        }

        /** The allele that does not show (equals the expressed one when homozygous). */
        public String hidden(Locus l) {
            String e = expressed(l);
            return e.equals(a) ? b : a;
        }
    }

    /** One phenotype outcome of a cross and its probability. */
    public record Outcome(String allele, double probability) {}

    public static final String SPECIES_KEY = "species";
    public static final String ANCESTRY_KEY = "ancestry";

    private final String species;
    private final BettaSpecies.Ancestry ancestry;
    private final Map<Locus, AllelePair> genes;
    private final CompoundTag extra;

    private BettaGenotype(String species, Map<Locus, AllelePair> genes, CompoundTag extra) {
        this(species, BettaSpecies.Ancestry.of(species), genes, extra);
    }

    private BettaGenotype(String species, BettaSpecies.Ancestry ancestry, Map<Locus, AllelePair> genes, CompoundTag extra) {
        // ids of the splendens complex follow the ancestry; foreign species ids (datapacks) stay as they are
        this.species = BettaSpecies.byId(species) != null || BettaSpecies.HYBRID_ID.equals(species) ? ancestry.speciesId() : species;
        this.ancestry = ancestry;
        this.genes = genes;
        this.extra = extra;
    }

    public String species() {
        return species;
    }

    /** M29 part c: shares of Betta splendens, imbellis and smaragdina. */
    public BettaSpecies.Ancestry ancestry() {
        return ancestry;
    }

    /** M29 part c: the same genes with another ancestry (wild species, tests). */
    public BettaGenotype withAncestry(BettaSpecies.Ancestry a) {
        return new BettaGenotype(a.speciesId(), a, new LinkedHashMap<>(genes), extra.copy());
    }

    public AllelePair pair(Locus l) {
        return genes.get(l);
    }

    // ------------------------------------------------------------------ construction

    /** Homozygous genotype that shows exactly the given phenotype (used for M1 data and creative fish). */
    public static BettaGenotype homozygous(BettaGenome g) {
        Map<Locus, AllelePair> m = new LinkedHashMap<>();
        for (Locus l : BettaLoci.ALL) {
            String v = phenotypeValue(g, l);
            m.put(l, new AllelePair(v, v));
        }
        return new BettaGenotype(g.species(), m, new CompoundTag());
    }

    public static BettaGenotype of(String species, Map<Locus, AllelePair> pairs) {
        Map<Locus, AllelePair> m = new LinkedHashMap<>();
        BettaGenome def = BettaGenome.DEFAULT;
        for (Locus l : BettaLoci.ALL) {
            AllelePair p = pairs.get(l);
            if (p == null) {
                String v = phenotypeValue(def, l);
                p = new AllelePair(v, v);
            }
            m.put(l, p);
        }
        return new BettaGenotype(species, m, new CompoundTag());
    }

    public BettaGenotype with(Locus l, AllelePair p) {
        Map<Locus, AllelePair> m = new LinkedHashMap<>(genes);
        m.put(l, p);
        return new BettaGenotype(species, ancestry, m, extra.copy());
    }

    static String phenotypeValue(BettaGenome g, Locus l) {
        return switch (l.id()) {
            case "finform", "fin_length", "pectoral", "body_color", "fin_color", "pattern", "pattern_color", "iridescence",
                 "iridescence_color" -> g.value(l.id());
            default -> throw new IllegalArgumentException(l.id());
        };
    }

    /** The visible fish. */
    public BettaGenome express() {
        return new BettaGenome(species, ex(BettaLoci.FINFORM), ex(BettaLoci.FIN_LENGTH), ex(BettaLoci.PECTORAL), ex(BettaLoci.BODY_COLOR),
                ex(BettaLoci.FIN_COLOR), ex(BettaLoci.PATTERN), ex(BettaLoci.PATTERN_COLOR), ex(BettaLoci.IRIDESCENCE),
                ex(BettaLoci.IRIDESCENCE_COLOR));
    }

    private String ex(Locus l) {
        return genes.get(l).expressed(l);
    }

    // ------------------------------------------------------------------ inheritance

    /**
     * Offspring of two parents: per locus one random allele of each parent, each allele mutates with
     * {@code mutationChance} into a random allele of the locus pool.
     */
    public static BettaGenotype cross(BettaGenotype mother, BettaGenotype father, RandomSource r, double mutationChance) {
        Map<Locus, AllelePair> m = new LinkedHashMap<>();
        for (Locus l : BettaLoci.ALL) {
            AllelePair pm = mother.pair(l), pf = father.pair(l);
            String a = r.nextBoolean() ? pm.a() : pm.b();
            String b = r.nextBoolean() ? pf.a() : pf.b();
            m.put(l, new AllelePair(mutate(l, a, r, mutationChance), mutate(l, b, r, mutationChance)));
        }
        BettaSpecies.Ancestry a = BettaSpecies.Ancestry.mix(mother.ancestry, father.ancestry); // M29 part c
        return new BettaGenotype(BettaSpecies.byId(mother.species) != null || BettaSpecies.HYBRID_ID.equals(mother.species) ? a.speciesId() : mother.species,
                a, m, new CompoundTag());
    }

    private static String mutate(Locus l, String allele, RandomSource r, double chance) {
        if (chance <= 0 || r.nextDouble() >= chance) return allele;
        return l.alleles().isEmpty() ? allele : l.pick(r);
    }

    /** Punnett square per locus: phenotype outcomes with probability (without mutations), most likely first. */
    public static Map<Locus, List<Outcome>> predict(BettaGenotype mother, BettaGenotype father) {
        Map<Locus, List<Outcome>> out = new LinkedHashMap<>();
        for (Locus l : BettaLoci.ALL) {
            AllelePair pm = mother.pair(l), pf = father.pair(l);
            Map<String, Double> p = new LinkedHashMap<>();
            for (String a : new String[]{pm.a(), pm.b()}) {
                for (String b : new String[]{pf.a(), pf.b()}) {
                    p.merge(l.express(a, b), 0.25, Double::sum);
                }
            }
            List<Outcome> list = new ArrayList<>();
            p.forEach((k, v) -> list.add(new Outcome(k, v)));
            list.sort(Comparator.comparingDouble(Outcome::probability).reversed().thenComparing(Outcome::allele));
            out.put(l, List.copyOf(list));
        }
        return out;
    }

    // ------------------------------------------------------------------ random fish

    /** Wild type: phenotype of {@link BettaGenome#randomWild}; some fish carry hidden recessive alleles. */
    public static BettaGenotype randomWild(RandomSource r) {
        BettaGenotype g = homozygous(BettaGenome.randomWild(r));
        if (r.nextInt(6) == 0) {
            g = g.with(BettaLoci.FINFORM, new AllelePair("round", pick(r, List.of("spade", "veil", "delta", "halfmoon", "crowntail", "doubletail"))));
        }
        if (r.nextInt(5) == 0) g = g.with(BettaLoci.FIN_LENGTH, new AllelePair(BettaGenome.SHORT, BettaGenome.LONG));
        if (r.nextInt(8) == 0) g = g.with(BettaLoci.PECTORAL, new AllelePair("normal", "dumbo"));
        BettaGenome wild = BettaGenome.randomWild(r);
        String body = g.pair(BettaLoci.BODY_COLOR).a();
        // a second wild colour; the dominant (darker) one shows
        g = g.with(BettaLoci.BODY_COLOR, new AllelePair(body, wild.bodyColor()));
        return g;
    }

    /**
     * M29 part c: a wild fish of one species of the splendens complex: its wild colours, now and then a hidden second wild
     * colour, rarely a hidden long fin.
     */
    public static BettaGenotype randomWild(BettaSpecies s, RandomSource r) {
        if (s == BettaSpecies.SPLENDENS) return randomWild(r);
        BettaGenotype g = homozygous(BettaSpecies.randomWildGenome(s, r)).withAncestry(BettaSpecies.Ancestry.pure(s));
        BettaGenome other = BettaSpecies.randomWildGenome(s, r);
        g = g.with(BettaLoci.BODY_COLOR, new AllelePair(g.pair(BettaLoci.BODY_COLOR).a(), other.bodyColor()));
        g = g.with(BettaLoci.FIN_COLOR, new AllelePair(g.pair(BettaLoci.FIN_COLOR).a(), other.finColor()));
        if (r.nextInt(8) == 0) g = g.with(BettaLoci.FIN_LENGTH, new AllelePair(BettaGenome.SHORT, BettaGenome.LONG));
        return g;
    }

    /** Any combination (spawn eggs, tests): two random alleles per locus, weighted by the gene frequencies. */
    public static BettaGenotype randomDomestic(RandomSource r) {
        Map<Locus, AllelePair> m = new LinkedHashMap<>();
        for (Locus l : BettaLoci.ALL) {
            String a = l.pick(r);
            String b = r.nextInt(3) == 0 ? a : l.pick(r);
            m.put(l, new AllelePair(a, b));
        }
        return new BettaGenotype(BettaGenome.SPECIES_BETTA, m, new CompoundTag());
    }

    private static String pick(RandomSource r, List<String> l) {
        return l.get(r.nextInt(l.size()));
    }

    // ------------------------------------------------------------------ NBT

    public CompoundTag save() {
        CompoundTag t = extra.copy();
        t.putString(SPECIES_KEY, species);
        // M29 part c: only hybrids need the shares (a pure fish has the ancestry of its id)
        if (!ancestry.equals(BettaSpecies.Ancestry.of(species))) t.put(ANCESTRY_KEY, ancestry.save());
        for (Locus l : BettaLoci.ALL) {
            AllelePair p = genes.get(l);
            ListTag list = new ListTag();
            list.add(StringTag.valueOf(p.a()));
            list.add(StringTag.valueOf(p.b()));
            t.put(l.id(), list);
        }
        return t;
    }

    public static BettaGenotype load(CompoundTag t) {
        String species = t.contains(SPECIES_KEY, Tag.TAG_STRING) ? t.getString(SPECIES_KEY) : BettaGenome.SPECIES_BETTA;
        Map<Locus, AllelePair> m = new LinkedHashMap<>();
        BettaGenome def = BettaGenome.DEFAULT;
        CompoundTag extra = t.copy();
        extra.remove(SPECIES_KEY);
        extra.remove(ANCESTRY_KEY);
        BettaSpecies.Ancestry ancestry = t.contains(ANCESTRY_KEY, Tag.TAG_COMPOUND) ? BettaSpecies.Ancestry.load(t.getCompound(ANCESTRY_KEY))
                : BettaSpecies.Ancestry.of(species);
        for (Locus l : BettaLoci.ALL) {
            extra.remove(l.id());
            String d = phenotypeValue(def, l);
            if (t.contains(l.id(), Tag.TAG_LIST)) {
                ListTag list = t.getList(l.id(), Tag.TAG_STRING);
                String a = list.isEmpty() ? d : list.getString(0);
                String b = list.size() < 2 ? a : list.getString(1);
                m.put(l, new AllelePair(a, b));
            } else if (t.contains(l.id(), Tag.TAG_STRING)) {
                String v = t.getString(l.id());
                m.put(l, new AllelePair(v, v));
            } else {
                m.put(l, new AllelePair(d, d));
            }
        }
        return new BettaGenotype(species, ancestry, m, extra);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BettaGenotype g && species.equals(g.species) && ancestry.equals(g.ancestry) && genes.equals(g.genes) && extra.equals(g.extra);
    }

    @Override
    public int hashCode() {
        return species.hashCode() * 31 + genes.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("Genotype[");
        for (Locus l : BettaLoci.ALL) {
            AllelePair p = genes.get(l);
            b.append(l.id()).append('=').append(p.a()).append('/').append(p.b()).append(' ');
        }
        return b.append(']').toString();
    }
}
