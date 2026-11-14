package org.overengineer.bettacraft.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.containers.AnimalContainers;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.genetics.GeneVisibility;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/** M22.3: capacities of buckets and bags, the analysis flag of old and new animals, mutations, the wild waters. */
class WildlifeTest {
    @Test
    void bucketsHoldManySmallAnimalsAndBagsHalf() {
        assertEquals(32, AnimalContainers.bucket(CritterSpecies.NEOCARIDINA));
        assertEquals(16, AnimalContainers.bag(CritterSpecies.NEOCARIDINA), "a bag holds as many shrimp as before");
        assertEquals(24, AnimalContainers.bucket(TankmateSpecies.HARLEQUIN_RASBORA));
        assertEquals(12, AnimalContainers.bag(TankmateSpecies.HARLEQUIN_RASBORA), "a bag holds as many rasboras as before");
        assertEquals(32, AnimalContainers.bucket(TankmateSpecies.NEON_TETRA));
        assertEquals(1, AnimalContainers.bucket(TankmateSpecies.CLOWN_LOACH), "big fish: one always fits");
        assertEquals(1, AnimalContainers.bag(TankmateSpecies.CLOWN_LOACH), "at least one in a bag");
        for (TankmateSpecies s : TankmateSpecies.values()) {
            assertTrue(AnimalContainers.bucket(s) >= 1, s.id());
            assertEquals(Math.max(1, AnimalContainers.bucket(s) / 2), AnimalContainers.bag(s), s.id());
        }
        for (CritterSpecies s : CritterSpecies.values()) assertEquals(AnimalContainers.bucket(s) / 2, AnimalContainers.bag(s), s.id());
    }

    @Test
    void oldAnimalsCountAsAnalysedNewOnesDoNot() {
        assertTrue(GeneVisibility.analysed(new CompoundTag()), "no flag = an animal of an older world");
        Critter k = new Critter(1, CritterSpecies.NEOCARIDINA, true, CritterGenes.pure(0, 0.5F), 0, 0, 0);
        assertFalse(k.analysed);
        CompoundTag saved = k.saveAnimal();
        assertFalse(Critter.loadAnimal(saved, 0, 0, 0, 0).analysed, "a new animal stays unanalysed in a bag");
        k.analysed = true;
        assertTrue(Critter.loadAnimal(k.saveAnimal(), 0, 0, 0, 0).analysed, "the analysis is kept");
        assertTrue(k.copy(5).analysed);
        CompoundTag old = k.saveAnimal();
        old.remove(GeneVisibility.KEY);
        assertTrue(Critter.loadAnimal(old, 0, 0, 0, 0).analysed, "an old bag: analysed");
    }

    @Test
    void crittersMutateOnlyWithTheSupplier() {
        CritterGenes wild = CritterGenes.WILD;
        Random r = new Random(1);
        for (int i = 0; i < 200; i++) assertEquals(0, CritterGenes.cross(wild, wild, 6, r).carried(), "no mutation by default");
        var before = CritterGenes.MUTATION;
        try {
            CritterGenes.MUTATION = () -> 1.0;
            assertEquals(0b111111, CritterGenes.cross(wild, wild, 6, r).homozygous(), "every allele flips at 100 %");
            CritterGenes.MUTATION = () -> 0.003;
            int mutated = 0;
            for (int i = 0; i < 20000; i++) if (CritterGenes.cross(wild, wild, 6, r).carried() != 0) mutated++;
            // 12 alleles at 0.3 %: about 3.5 % of the young carry a new allele
            assertTrue(mutated > 400 && mutated < 1100, "mutations " + mutated);
        } finally {
            CritterGenes.MUTATION = before;
        }
    }

    @Test
    void tankmatesWithVarietiesMutateLivebearersNot() {
        Random r = new Random(2);
        TankmateGenes g = TankmateGenes.wild(TankmateSpecies.BRISTLENOSE_PLECO);
        int changed = 0;
        for (int i = 0; i < 20000; i++) if (!Wildlife.mutate(TankmateSpecies.BRISTLENOSE_PLECO, g, r).text().equals(g.text())) changed++;
        assertTrue(changed > 60 && changed < 200, "about 0.6 % of the young: " + changed);
        TankmateGenes one = TankmateGenes.wild(TankmateSpecies.NEON_TETRA);
        for (int i = 0; i < 2000; i++) assertEquals(one, Wildlife.mutate(TankmateSpecies.NEON_TETRA, one, r), "one variety: nothing to mutate");
    }

    @Test
    void wildFishLiveInTheWatersOfTheNet() {
        assertTrue(Wildlife.fishWeight(TankmateSpecies.NEON_TETRA, NetCatching.Water.JUNGLE) > 0);
        assertEquals(0, Wildlife.fishWeight(TankmateSpecies.NEON_TETRA, NetCatching.Water.RIVER));
        assertTrue(Wildlife.fishWeight(TankmateSpecies.GUPPY, NetCatching.Water.RIVER) > 0);
        assertTrue(Wildlife.fishWeight(TankmateSpecies.HARLEQUIN_RASBORA, NetCatching.Water.SWAMP) > 0);
        assertTrue(Wildlife.fishWeight(TankmateSpecies.OCELLARIS_CLOWNFISH, NetCatching.Water.WARM_OCEAN) > 0);
        assertTrue(Wildlife.frogWeight(NetCatching.Water.SWAMP) > 0);
        assertEquals(0, Wildlife.critterWeight(NetCatching.Water.NONE));
        assertTrue(Wildlife.critterWeight(NetCatching.Water.WARM_OCEAN) > 0);
        assertArrayEquals1(new int[]{1, 1}, Wildlife.group(TankmateSpecies.DWARF_GOURAMI));
        assertNotEquals(1, Wildlife.group(TankmateSpecies.NEON_TETRA)[1]);
        // the cleaner shrimp is a rare warm ocean catch, the red and yellow nerites rare in mangroves
        var warm = Wildlife.critterTable(NetCatching.Water.WARM_OCEAN, false, false);
        assertTrue(warm.stream().anyMatch(c -> c.species() == CritterSpecies.CLEANER_SHRIMP));
        var mangrove = Wildlife.critterTable(NetCatching.Water.MANGROVE, false, false);
        assertTrue(mangrove.stream().anyMatch(c -> c.species() == CritterSpecies.RED_NERITE));
    }

    private static void assertArrayEquals1(int[] a, int[] b) {
        assertEquals(a[0], b[0]);
        assertEquals(a[1], b[1]);
    }
}
