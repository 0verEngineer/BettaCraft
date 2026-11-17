package org.overengineer.bettacraft.tankmates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.config.Feature;

/**
 * M48 part a: the bred forms of the tetras and corydoras on the colour locus (A1): recessive against the wild type, the order
 * of dominance as listed, the rare longfin, the new species; old fish (single-variety saves) load as wild type.
 */
class TetraCoryFormsTest {
    @Test
    void theFormsAndTheirOrder() {
        assertEquals(List.of("wild", "diamond", "gold", "albino"), TankmateSpecies.NEON_TETRA.varieties());
        assertEquals(List.of("wild", "gold"), TankmateSpecies.CARDINAL_TETRA.varieties());
        assertEquals(List.of("wild", "albino"), TankmateSpecies.GLOWLIGHT_TETRA.varieties());
        assertEquals(List.of("wild", "albino", "longfin"), TankmateSpecies.PANDA_CORY.varieties());
        assertEquals(List.of("wild", "albino"), TankmateSpecies.STERBAI_CORY.varieties());
        assertEquals(List.of("wild", "albino", "gold_laser"), TankmateSpecies.BRONZE_CORY.varieties());
        assertEquals(List.of("wild"), TankmateSpecies.EMBER_TETRA.varieties(), "the ember tetra keeps only its wild form");
        TankmateSpecies neon = TankmateSpecies.NEON_TETRA;
        assertEquals("wild", new TankmateGenes("wild", "gold").phenotype(neon));
        assertEquals("gold", new TankmateGenes("albino", "gold").phenotype(neon));
        assertEquals("diamond", new TankmateGenes("diamond", "albino").phenotype(neon));
        assertEquals("albino", new TankmateGenes("albino", "gold_laser").phenotype(TankmateSpecies.BRONZE_CORY));
        assertEquals("panda_cory_longfin", TankmateSpecies.PANDA_CORY.texture("longfin", false));
        assertEquals("bronze_cory_gold_laser", TankmateSpecies.BRONZE_CORY.texture("gold_laser", true));
        assertEquals("panda_garra", TankmateSpecies.PANDA_GARRA.texture("wild", false));
        for (TankmateSpecies s : TankmateSpecies.values()) {
            boolean m48 = s == TankmateSpecies.NEON_TETRA || s == TankmateSpecies.CARDINAL_TETRA || s == TankmateSpecies.GLOWLIGHT_TETRA
                    || s == TankmateSpecies.PANDA_CORY || s == TankmateSpecies.STERBAI_CORY || s == TankmateSpecies.BRONZE_CORY;
            assertEquals(m48, s.m48Forms(), s.id());
        }
    }

    @Test
    void goldTimesAlbinoNeonsGiveGoldThenAThirdOfTheF2Albino() {
        Random r = new Random(48);
        TankmateSpecies neon = TankmateSpecies.NEON_TETRA;
        TankmateGenes gold = TankmateGenes.pure(neon, "gold"), albino = TankmateGenes.pure(neon, "albino");
        TankmateGenes f1 = TankmateGenes.cross(gold, albino, r::nextLong);
        assertEquals("gold", f1.phenotype(neon));
        assertTrue(f1.carries("albino"));
        int albinos = 0, n = 4000;
        for (int i = 0; i < n; i++) if (TankmateGenes.cross(f1, f1, r::nextLong).phenotype(neon).equals("albino")) albinos++;
        assertEquals(0.25, albinos / (double) n, 0.04, "F2 albino");
        // wild carrier x albino: half albino
        int a = 0;
        for (int i = 0; i < n; i++) if (TankmateGenes.cross(new TankmateGenes("wild", "albino"), albino, r::nextLong).phenotype(neon).equals("albino")) a++;
        assertEquals(0.5, a / (double) n, 0.04);
    }

    @Test
    void theLongfinIsRare() {
        Random r = new Random(4801);
        Map<String, Integer> n = new HashMap<>();
        for (int i = 0; i < 10000; i++) n.merge(TankmateSpecies.PANDA_CORY.randomBred(r::nextLong), 1, Integer::sum);
        assertEquals(0.8, n.get("albino") / 10000.0, 0.03, "albino four times as often as longfin");
        assertEquals(0.2, n.get("longfin") / 10000.0, 0.03);
        assertNull(TankmateSpecies.EMBER_TETRA.randomBred(r::nextLong), "no bred form");
        assertTrue(TankmateSpecies.PANDA_CORY.rareVariety("longfin") && !TankmateSpecies.PANDA_CORY.rareVariety("albino"));
    }

    @Test
    void wildFishCarryTheFormsHidden() {
        Random r = new Random(4848);
        Map<String, Boolean> seen = new HashMap<>();
        for (int i = 0; i < 20000; i++) {
            TankmateGenes g = TankmateGenes.wild(TankmateSpecies.NEON_TETRA, 0.08, r::nextLong);
            assertEquals("wild", g.phenotype(TankmateSpecies.NEON_TETRA));
            for (String v : List.of("diamond", "gold", "albino")) if (g.carries(v)) seen.put(v, true);
        }
        assertEquals(3, seen.size(), "wild neons carry every form hidden: " + seen);
    }

    @Test
    void oldFishLoadAsWildType() {
        // a neon of 1.37 saved its single variety as "wild"
        CompoundTag t = new CompoundTag();
        t.putString("a", "wild");
        t.putString("b", "wild");
        TankmateGenes g = TankmateGenes.load(t, TankmateSpecies.NEON_TETRA);
        assertEquals("wild", g.phenotype(TankmateSpecies.NEON_TETRA));
        assertEquals("neon_tetra_wild", TankmateSpecies.NEON_TETRA.texture(g.phenotype(TankmateSpecies.NEON_TETRA), false));
        // empty genes (older still) too
        assertEquals("wild", TankmateGenes.load(new CompoundTag(), TankmateSpecies.PANDA_CORY).phenotype(TankmateSpecies.PANDA_CORY));
    }

    @Test
    void theSwitchesOfTheNewSpecies() {
        assertEquals(Feature.MORE_BOTTOM_FISH, TankmateSpecies.BRONZE_CORY.feature());
        assertEquals(Feature.MORE_BOTTOM_FISH, TankmateSpecies.PANDA_GARRA.feature());
        assertEquals("features.tankmates.moreBottomFish", TankmateSpecies.PANDA_GARRA.featureKey());
        assertTrue(TankmateSpecies.PANDA_GARRA.grazer());
        assertTrue(TankmateSpecies.BRONZE_CORY.airGulper());
    }
}
