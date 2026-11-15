package org.overengineer.bettacraft.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

/** M29 part c: the wild species of the Betta splendens complex and their hybrids. */
class BettaSpeciesTest {
    @Test
    void wildSpeciesHaveTheirIdAndLook() {
        RandomSource r = RandomSource.create(29);
        BettaGenotype imb = BettaGenotype.randomWild(BettaSpecies.IMBELLIS, r);
        assertEquals("bettacraft:betta_imbellis", imb.species());
        assertEquals(BettaSpecies.IMBELLIS, imb.ancestry().species());
        BettaGenome g = imb.express();
        assertEquals("bettacraft:betta_imbellis", g.species());
        assertEquals("round", g.shape());
        assertEquals("tipped", g.pattern(), "the red fin edge");
        BettaGenotype sma = BettaGenotype.randomWild(BettaSpecies.SMARAGDINA, r);
        assertEquals("bettacraft:betta_smaragdina", sma.species());
        assertEquals("high", sma.express().iridescence());
        assertTrue(BettaSpecies.IMBELLIS.scale() < 1 && BettaSpecies.SMARAGDINA.scale() > 1);
    }

    @Test
    void crossesGiveFertileHybridsWithMixedAncestry() {
        RandomSource r = RandomSource.create(7);
        BettaGenotype spl = BettaGenotype.randomWild(r);
        BettaGenotype imb = BettaGenotype.randomWild(BettaSpecies.IMBELLIS, r);
        BettaGenotype f1 = BettaGenotype.cross(imb, spl, r, 0);
        assertEquals(BettaSpecies.HYBRID_ID, f1.species());
        assertEquals(32, f1.ancestry().imbellis());
        assertEquals(32, f1.ancestry().splendens());
        assertNull(f1.ancestry().species());
        // back-crossed to splendens three times: 1/16 imbellis, counts as splendens again
        BettaGenotype b = f1;
        for (int i = 0; i < 3; i++) b = BettaGenotype.cross(spl, b, r, 0);
        assertEquals(4, b.ancestry().imbellis());
        assertEquals(BettaGenome.SPECIES_BETTA, b.species());
        // F1 x F1 stays a hybrid; the mean of the parents
        assertEquals(32, BettaGenotype.cross(f1, f1, r, 0).ancestry().imbellis());
        // the males of the peaceful betta fight less
        assertTrue(f1.ancestry().aggression() < 1 && f1.ancestry().aggression() > BettaSpecies.IMBELLIS.aggression());
    }

    @Test
    void ancestryIsSavedOnlyForMixedFish() {
        RandomSource r = RandomSource.create(3);
        BettaGenotype imb = BettaGenotype.randomWild(BettaSpecies.IMBELLIS, r);
        CompoundTag pure = imb.save();
        assertTrue(!pure.contains(BettaGenotype.ANCESTRY_KEY), "a pure fish has the ancestry of its id");
        assertEquals(imb, BettaGenotype.load(pure));
        BettaGenotype hyb = BettaGenotype.cross(imb, BettaGenotype.randomWild(BettaSpecies.SMARAGDINA, r), r, 0);
        CompoundTag t = hyb.save();
        assertTrue(t.contains(BettaGenotype.ANCESTRY_KEY));
        BettaGenotype back = BettaGenotype.load(t);
        assertEquals(hyb, back);
        assertEquals(32, back.ancestry().smaragdina());
        // old fish without ancestry are pure Betta splendens
        BettaGenotype old = BettaGenotype.homozygous(BettaGenome.DEFAULT);
        assertTrue(old.ancestry().pureSplendens());
        assertEquals(BettaGenome.SPECIES_BETTA, BettaGenotype.load(old.save()).species());
    }
}
