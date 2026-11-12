package org.overengineer.bettacraft.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

/** Mendel genetics without a Minecraft bootstrap. */
class BettaGenotypeTest {
    private static BettaGenotype with(BettaGenotype g, Locus l, String a, String b) {
        return g.with(l, new BettaGenotype.AllelePair(a, b));
    }

    private static final BettaGenotype BASE = BettaGenotype.homozygous(BettaGenome.DEFAULT);

    @Test
    void dominanceDecidesThePhenotype() {
        BettaGenotype g = with(BASE, BettaLoci.FINFORM, "veil", "plakat");
        assertEquals("plakat", g.express().shape(), "plakat (wild type) is dominant");
        g = with(g, BettaLoci.PECTORAL, "dumbo", "normal");
        assertEquals("normal", g.express().pectoral(), "dumbo is recessive");
        g = with(g, BettaLoci.PECTORAL, "dumbo", "dumbo");
        assertEquals("dumbo", g.express().pectoral());
        g = with(g, BettaLoci.IRIDESCENCE, "none", "high");
        assertEquals("high", g.express().iridescence());
        g = with(g, BettaLoci.PATTERN, "none", "marble");
        assertEquals("marble", g.express().pattern());
        // colours: the darker one dominates, independent of the allele order
        BettaGenotype a = with(BASE, BettaLoci.BODY_COLOR, "black", "yellow");
        BettaGenotype b = with(BASE, BettaLoci.BODY_COLOR, "yellow", "black");
        assertEquals(a.express().bodyColor(), b.express().bodyColor());
        assertEquals("black", a.express().bodyColor());
        // unknown ids are recessive but kept
        BettaGenotype u = with(BASE, BettaLoci.BODY_COLOR, "unknown_mod_color", "red");
        assertEquals("red", u.express().bodyColor());
        assertEquals("unknown_mod_color", BettaGenotype.load(u.save()).pair(BettaLoci.BODY_COLOR).a());
    }

    @Test
    void punnettSquareOfTwoCarriers() {
        BettaGenotype carrier = with(BASE, BettaLoci.FINFORM, "plakat", "halfmoon");
        Map<Locus, List<BettaGenotype.Outcome>> p = BettaGenotype.predict(carrier, carrier);
        List<BettaGenotype.Outcome> fins = p.get(BettaLoci.FINFORM);
        assertEquals(2, fins.size());
        assertEquals("plakat", fins.get(0).allele());
        assertEquals(0.75, fins.get(0).probability(), 1e-9);
        assertEquals("halfmoon", fins.get(1).allele());
        assertEquals(0.25, fins.get(1).probability(), 1e-9);
        // homozygous loci: one outcome with 100 %
        assertEquals(1.0, p.get(BettaLoci.PECTORAL).getFirst().probability(), 1e-9);
    }

    @Test
    void crossFollowsTheSquare() {
        BettaGenotype carrier = with(BASE, BettaLoci.FINFORM, "plakat", "halfmoon");
        RandomSource r = RandomSource.create(1234);
        Map<String, Integer> count = new HashMap<>();
        int n = 8000;
        for (int i = 0; i < n; i++) {
            BettaGenotype child = BettaGenotype.cross(carrier, carrier, r, 0.0);
            count.merge(child.express().shape(), 1, Integer::sum);
            BettaGenotype.AllelePair pair = child.pair(BettaLoci.FINFORM);
            assertTrue(List.of("plakat", "halfmoon").contains(pair.a()) && List.of("plakat", "halfmoon").contains(pair.b()));
        }
        double halfmoon = count.getOrDefault("halfmoon", 0) / (double) n;
        assertEquals(0.25, halfmoon, 0.03, "a quarter of the offspring of two carriers show the recessive trait");
    }

    @Test
    void mutationsOnlyWithChance() {
        RandomSource r = RandomSource.create(7);
        for (int i = 0; i < 200; i++) assertEquals(BASE, BettaGenotype.cross(BASE, BASE, r, 0.0));
        int changed = 0;
        for (int i = 0; i < 200; i++) if (!BettaGenotype.cross(BASE, BASE, r, 1.0).equals(BASE)) changed++;
        assertTrue(changed > 150, "mutation chance 1 changes almost every child");
    }

    @Test
    void saveLoadRoundTrip() {
        RandomSource r = RandomSource.create(99);
        for (int i = 0; i < 50; i++) {
            BettaGenotype g = BettaGenotype.randomDomestic(r);
            assertEquals(g, BettaGenotype.load(g.save()));
            BettaGenotype w = BettaGenotype.randomWild(r);
            assertEquals(w, BettaGenotype.load(w.save()));
        }
        // homozygous genotype shows exactly its phenotype
        BettaGenome ph = BettaGenome.randomDomestic(r);
        assertEquals(ph, BettaGenotype.homozygous(ph).express());
    }
}
