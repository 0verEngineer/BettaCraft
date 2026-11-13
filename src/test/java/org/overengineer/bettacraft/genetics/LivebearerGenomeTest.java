package org.overengineer.bettacraft.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.genetics.livebearer.LbLocus;
import org.overengineer.bettacraft.genetics.livebearer.LbPhenotype;
import org.overengineer.bettacraft.genetics.livebearer.LivebearerGenome;
import org.overengineer.bettacraft.genetics.livebearer.LivebearerLegacy;
import org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/** M21.6: the guppy and endler genetics: dominance, Mendel ratios, the Y and X linkage, hybrids, strains, old varieties. */
class LivebearerGenomeTest {
    private static LivebearerGenome.Builder guppy() {
        return LivebearerGenome.builder(false);
    }

    @Test
    void dominance() {
        assertEquals("double_sword", LbLocus.TAIL.express("top_sword", "bottom_sword"));
        assertEquals("round", LbLocus.TAIL.express("delta", "round"), "the wild round tail hides the delta");
        assertEquals("lyre", LbLocus.TAIL.express("lyre", "delta"));
        assertEquals("red", LbLocus.TAIL_COLOR.express("blue", "red"));
        assertEquals("clear", LbLocus.TAIL_COLOR.express("clear", "white"));
        assertEquals("dark", LbLocus.DARK.express("wild", "dark"), "dark is dominant");
        assertEquals("wild", LbLocus.GOLD.express("gold", "wild"), "gold is recessive");
        assertEquals("delta", LbLocus.TAIL.express("unknown_new", "delta"), "unknown alleles are the most recessive");
        // body colours
        assertEquals("gold", guppy().pure(LbLocus.GOLD, "gold").build().express(false).body());
        assertEquals("wild", guppy().pair(LbLocus.GOLD, "gold", "wild").build().express(false).body());
        assertEquals("white", guppy().pure(LbLocus.BLOND, "blond").pure(LbLocus.BLUE, "blue").build().express(false).body());
        assertEquals("albino", guppy().pure(LbLocus.ALBINO, "albino").pure(LbLocus.DARK, "dark").build().express(false).body());
    }

    @Test
    void mendelRatios() {
        LivebearerGenome carrier = guppy().pair(LbLocus.GOLD, "wild", "gold").build();
        Random r = new Random(1);
        int gold = 0, n = 8000;
        for (int i = 0; i < n; i++) {
            if (LivebearerGenome.cross(carrier, carrier, r.nextBoolean(), r, 0).express(false).body().equals("gold")) gold++;
        }
        assertEquals(0.25, gold / (double) n, 0.02, "carrier x carrier: a quarter gold");
        // a top-sword and a bottom-sword parent: double swords
        LivebearerGenome top = guppy().pure(LbLocus.TAIL, "top_sword").build(), bottom = guppy().pure(LbLocus.TAIL, "bottom_sword").build();
        for (int i = 0; i < 200; i++) assertEquals("double_sword", LivebearerGenome.cross(top, bottom, i % 2 == 0, r, 0).express(false).tail());
    }

    @Test
    void yPatternsGoOnlyFromFatherToSons() {
        LivebearerGenome mother = guppy().x("none").y("none").build();
        LivebearerGenome father = guppy().x("none").y("cobra").build();
        Random r = new Random(2);
        for (int i = 0; i < 500; i++) {
            LivebearerGenome son = LivebearerGenome.cross(mother, father, false, r, 0);
            LivebearerGenome daughter = LivebearerGenome.cross(mother, father, true, r, 0);
            assertEquals("cobra", son.y, "every son gets the father's Y");
            assertEquals("cobra", son.express(false).y());
            assertEquals("none", daughter.y, "no daughter gets a Y");
            assertEquals("none", daughter.express(true).y());
            // the daughter's sons (the father's grandsons through her) do not get it either
            LivebearerGenome grandson = LivebearerGenome.cross(daughter, guppy().y("none").build(), false, r, 0);
            assertEquals("none", grandson.y);
        }
        // a cobra female cannot pass a Y she does not have: the Y pattern of the young comes from the father alone
        LivebearerGenome wildFather = guppy().y("wild").build();
        for (int i = 0; i < 100; i++) assertEquals("wild", LivebearerGenome.cross(father.toBuilder().build(), wildFather, false, r, 0).y);
    }

    @Test
    void xPatternsZigZag() {
        // the father's X goes to every daughter, never to a son; the mother's X to both
        LivebearerGenome mother = guppy().x("none", "none").build();
        LivebearerGenome father = guppy().x("tuxedo").y("none").build();
        Random r = new Random(3);
        for (int i = 0; i < 300; i++) {
            assertEquals("tuxedo", LivebearerGenome.cross(mother, father, true, r, 0).xb, "daughters carry the father's X");
            assertEquals("none", LivebearerGenome.cross(mother, father, false, r, 0).express(false).x(), "sons get the mother's X");
        }
        LivebearerGenome tuxMother = guppy().x("tuxedo", "none").build();
        int tux = 0;
        for (int i = 0; i < 4000; i++) if (LivebearerGenome.cross(tuxMother, guppy().y("none").build(), false, r, 0).express(false).x().equals("tuxedo")) tux++;
        assertEquals(0.5, tux / 4000.0, 0.03, "half the sons of a carrier mother show tuxedo");
        // a female shows the tuxedo of either X, no Y pattern
        LbPhenotype f = guppy().x("none", "tuxedo").y("cobra").build().express(true);
        assertEquals("tuxedo", f.x());
        assertEquals("none", f.y());
    }

    @Test
    void hybrids() {
        LivebearerGenome endler = LivebearerGenome.builder(true).build(), g = guppy().build();
        Random r = new Random(4);
        LivebearerGenome h = LivebearerGenome.cross(g, endler, false, r, 0);
        assertEquals(0.5F, h.endler, 1e-6);
        assertTrue(h.express(false).hybrid());
        assertNull(LivebearerStrain.of(h.express(false)), "hybrids have no strain");
        assertEquals("peacock", h.y, "the endler father's Y");
        // back-cross to endlers: 3/4, 7/8 endler
        LivebearerGenome b1 = LivebearerGenome.cross(h, endler, true, r, 0), b2 = LivebearerGenome.cross(b1, endler, false, r, 0);
        assertEquals(0.75F, b1.endler, 1e-6);
        assertEquals(0.875F, b2.endler, 1e-6);
        assertTrue(b2.express(false).endlerType());
    }

    @Test
    void strainsMatchTheirExamples() {
        Set<String> ids = new HashSet<>();
        for (LivebearerStrain s : LivebearerStrain.values()) {
            assertTrue(ids.add(s.id()));
            LivebearerGenome g = s.genome();
            assertEquals(s, LivebearerStrain.of(g.express(false)), s.id() + " male of its own example");
            if (!s.yLinked()) {
                LivebearerStrain f = LivebearerStrain.of(g.express(true));
                assertNotNull(f, s.id() + " female has a name");
            }
            assertTrue(s.endler ? TankmateSpecies.ENDLER_GUPPY.varieties().contains(s.legacy) : TankmateSpecies.GUPPY.varieties().contains(s.legacy),
                    s.id() + " legacy variety");
            assertFalse(s.en.isEmpty() || s.de.isEmpty());
        }
        assertTrue(LivebearerStrain.values().length >= 20);
        assertTrue(LivebearerStrain.TUXEDO_KOI.yLinked());
        assertFalse(LivebearerStrain.FULL_GOLD.yLinked());
    }

    @Test
    void oldVarietiesConvert() {
        for (var e : LivebearerLegacy.GUPPY_STRAINS.entrySet()) {
            LivebearerGenome g = LivebearerLegacy.convert(false, e.getKey(), e.getKey(), e.getKey());
            assertEquals(e.getValue(), LivebearerStrain.of(g.express(false)), "guppy " + e.getKey());
            assertEquals(g, LivebearerLegacy.convert(false, e.getKey(), e.getKey(), e.getKey()), "deterministic");
            assertEquals(g, TankmateGenes.pure(TankmateSpecies.GUPPY, e.getKey()).lb());
        }
        for (var e : LivebearerLegacy.ENDLER_STRAINS.entrySet()) {
            LivebearerGenome g = LivebearerLegacy.convert(true, e.getKey(), e.getKey(), e.getKey());
            assertEquals(e.getValue(), LivebearerStrain.of(g.express(false)), "endler " + e.getKey());
        }
        // a tuxedo female keeps her half-black X
        assertEquals("tuxedo", LivebearerLegacy.convert(false, "tuxedo", "tuxedo", "tuxedo").express(true).x());
        // a heterozygous old fish: the variety it showed decides the Y
        assertEquals("red_tail", LivebearerLegacy.convert(false, "blue_grass", "red", "red").y);
        // loaded without a genome (an old world): converted from its varieties
        CompoundTag old = new CompoundTag();
        old.putString("a", "green_cobra");
        old.putString("b", "green_cobra");
        TankmateGenes t = TankmateGenes.load(old, TankmateSpecies.GUPPY);
        assertNotNull(t.lb());
        assertEquals(LivebearerStrain.GREEN_COBRA, LivebearerStrain.of(t.lb().express(false)));
        assertEquals("green_cobra", t.phenotype(TankmateSpecies.GUPPY), "the variety stays");
        assertNull(TankmateGenes.load(old, TankmateSpecies.PLATY).lb(), "other species have no genome");
    }

    @Test
    void saveAndLoad() {
        Random r = new Random(5);
        for (int i = 0; i < 200; i++) {
            LivebearerGenome g = LivebearerGenome.wild(i % 3 == 0, r);
            assertEquals(g, LivebearerGenome.load(g.save(), i % 3 == 0));
            TankmateGenes t = new TankmateGenes("wild", "red", g);
            assertEquals(t, TankmateGenes.load(t.save(), TankmateSpecies.GUPPY));
        }
        // unknown alleles of a newer version are kept
        CompoundTag tag = guppy().build().save();
        tag.putString("tail", "veil_new/delta");
        LivebearerGenome g = LivebearerGenome.load(tag, false);
        assertEquals("veil_new", g.a(LbLocus.TAIL));
        assertEquals("delta", g.express(false).tail());
        assertEquals(g, LivebearerGenome.load(g.save(), false));
        // partial tag: wild type of the background
        assertEquals("orange", LivebearerGenome.load(new CompoundTag(), true).express(false).tailColor());
    }

    @Test
    void wildFish() {
        Random r = new Random(6);
        int wildGuppy = 0, endlerNamed = 0, n = 2000;
        for (int i = 0; i < n; i++) {
            LbPhenotype p = LivebearerGenome.wild(false, r).express(false);
            assertTrue(p.tail().equals("round") || p.tail().equals("spade") || LbLocus.TAIL.rank(p.tail()) < LbLocus.TAIL.rank("round"),
                    "wild guppies have short tails or a (dominant) sword, got " + p.tail());
            assertEquals("clear", p.tailColor(), "wild guppies have clear tails");
            if (LivebearerStrain.of(p) == LivebearerStrain.WILD_GUPPY) wildGuppy++;
            LbPhenotype e = LivebearerGenome.wild(true, r).express(false);
            if (LivebearerStrain.of(e) != null) endlerNamed++;
        }
        assertTrue(wildGuppy > n * 0.6, "most wild guppy males are the wild type: " + wildGuppy);
        assertTrue(endlerNamed > n * 0.85, "wild endlers are mostly named strains: " + endlerNamed);
    }
}
