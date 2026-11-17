package org.overengineer.bettacraft.tankmates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/**
 * M47 part b: the Black and the German Blue ram on the colour locus (D1): recessive against the wild type, the order of
 * dominance Gold, Electric Blue, Black, German Blue, Balloon; old rams load with the genes they had.
 */
class RamFormsTest {
    private static final TankmateSpecies RAM = TankmateSpecies.RAM_CICHLID;

    @Test
    void theOrderOfTheForms() {
        assertEquals(List.of("wild", "gold", "electric_blue", "black", "german_blue", "balloon"), RAM.varieties());
        assertEquals("black", new TankmateGenes("black", "german_blue").phenotype(RAM));
        assertEquals("electric_blue", new TankmateGenes("black", "electric_blue").phenotype(RAM));
        assertEquals("german_blue", new TankmateGenes("balloon", "german_blue").phenotype(RAM));
        assertEquals("wild", new TankmateGenes("wild", "black").phenotype(RAM));
        assertEquals("ram_cichlid_german_blue_female", RAM.texture("german_blue", true));
        assertEquals("ram_cichlid_black_male", RAM.texture("black", false));
    }

    @Test
    void blackTimesGermanBlueGivesBlackCarriersThenBoth() {
        Random r = new Random(47);
        TankmateGenes black = TankmateGenes.pure(RAM, "black"), blue = TankmateGenes.pure(RAM, "german_blue");
        for (int i = 0; i < 50; i++) {
            TankmateGenes f1 = TankmateGenes.cross(black, blue, r::nextLong);
            assertEquals("black", f1.phenotype(RAM));
            assertTrue(f1.carries("german_blue") && f1.carries("black"));
        }
        TankmateGenes f1 = new TankmateGenes("black", "german_blue");
        int blacks = 0, blues = 0, n = 4000;
        for (int i = 0; i < n; i++) {
            String p = TankmateGenes.cross(f1, f1, r::nextLong).phenotype(RAM);
            if (p.equals("black")) blacks++;
            else if (p.equals("german_blue")) blues++;
        }
        assertEquals(0.75, blacks / (double) n, 0.04, "F2 black");
        assertEquals(0.25, blues / (double) n, 0.04, "F2 German Blue");
        // a wild carrier of German Blue with a German Blue: half German Blue
        int b = 0;
        for (int i = 0; i < n; i++) if (TankmateGenes.cross(new TankmateGenes("wild", "german_blue"), blue, r::nextLong).phenotype(RAM).equals("german_blue")) b++;
        assertEquals(0.5, b / (double) n, 0.04);
    }

    @Test
    void wildRamsCanCarryTheNewForms() {
        Random r = new Random(4747);
        boolean black = false, blue = false;
        for (int i = 0; i < 20000 && !(black && blue); i++) {
            TankmateGenes g = TankmateGenes.wild(RAM, 0.08, r::nextLong);
            assertEquals("wild", g.phenotype(RAM));
            black |= g.carries("black");
            blue |= g.carries("german_blue");
        }
        assertTrue(black && blue, "wild rams carry Black and German Blue hidden");
    }

    @Test
    void oldRamsLoadAsTheyWere() {
        CompoundTag t = new CompoundTag();
        t.putString("a", "gold");
        t.putString("b", "balloon");
        TankmateGenes g = TankmateGenes.load(t, RAM);
        assertEquals("gold", g.a());
        assertEquals("balloon", g.b());
        assertEquals("gold", g.phenotype(RAM));
        CompoundTag e = new CompoundTag();
        e.putString("a", "electric_blue");
        e.putString("b", "electric_blue");
        assertEquals("electric_blue", TankmateGenes.load(e, RAM).phenotype(RAM));
        assertEquals(TankmateGenes.load(t, RAM).save(), g.save());
    }
}
