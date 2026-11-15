package org.overengineer.bettacraft.tankmates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/** M19: the species table, the variety genetics and the clutch data of the tankmates. */
class TankmateDataTest {
    private static Path textures() {
        return Path.of(System.getProperty("bettacraft.projectDir", ".")).resolve("src/main/resources/assets/bettacraft/textures/entity/tankmates");
    }

    @Test
    void speciesTableIsSane() {
        Set<String> ids = new HashSet<>();
        for (TankmateSpecies s : TankmateSpecies.values()) {
            assertTrue(ids.add(s.id()), "unique id " + s.id());
            assertFalse(s.id().contains("aquarium") || s.id().contains("fishtank") || s.id().contains("fishbowl"), "id " + s.id());
            assertTrue(s.tempMin() < s.tempMax(), s.id() + " temperature range");
            if (s.breeding() != TankmateSpecies.Breeding.NONE) {
                assertTrue(s.breedMin() >= s.tempMin() && s.breedMax() <= s.tempMax(), s.id() + " breeds inside its tolerable range");
                assertTrue(s.broodMin() >= 1 && s.broodMax() >= s.broodMin(), s.id() + " brood");
            }
            assertTrue(s.hitboxWidth() > 0 && s.hitboxWidth() <= 0.8F && s.hitboxHeight() > 0, s.id() + " fits the tank size rule");
            assertTrue(s.length() >= 0.1 && s.length() <= 1.0, s.id() + " length");
            assertTrue(s.stock() > 0 && s.stock() <= 3, s.id() + " stocking");
            assertTrue(s.minQuality() > 0 && s.minQuality() < 1, s.id() + " water");
            assertFalse(s.waters().isEmpty(), s.id() + " can be caught somewhere");
            assertEquals("wild", s.variety(0), s.id() + " wild type first");
        }
        assertEquals(43, TankmateSpecies.values().length); // M20: + 5 reef fish, M21.5: + green neon, M21.8: + 2 seahorses, M29: + 6 cichlids, + 9 puffers, barbs, loaches
        // the user's wishes
        assertNotNull(TankmateSpecies.byId("leopard_frog_pleco"));
        assertNotNull(TankmateSpecies.byId("mosquito_rasbora"));
        assertEquals("Corydoras melanotaenia", TankmateSpecies.GREEN_GOLD_CORY.latin());
        assertEquals("Boraras brigittae", TankmateSpecies.MOSQUITO_RASBORA.latin());
        assertTrue(TankmateSpecies.LEOPARD_FROG_PLECO.latin().contains("L134"));
    }

    @Test
    void everyTextureExists() {
        for (TankmateSpecies s : TankmateSpecies.values()) {
            for (String t : s.textures()) assertTrue(Files.exists(textures().resolve(t + ".png")), "texture " + t);
        }
        assertEquals("guppy_blue_grass_female", TankmateSpecies.GUPPY.texture("blue_grass", true));
        assertEquals("neon_tetra", TankmateSpecies.NEON_TETRA.texture("wild", true));
        assertEquals("bristlenose_pleco_albino", TankmateSpecies.BRISTLENOSE_PLECO.texture("albino", false));
    }

    @Test
    void wildTypeIsDominantBredVarietiesRecessive() {
        TankmateSpecies b = TankmateSpecies.BRISTLENOSE_PLECO;
        assertEquals("wild", new TankmateGenes("albino", "wild").phenotype(b), "an albino carrier looks wild");
        assertEquals("albino", TankmateGenes.pure(b, "albino").phenotype(b));
        // two different bred varieties: the one listed first shows
        assertEquals("albino", new TankmateGenes("super_red", "albino").phenotype(b));
        // unknown alleles (removed varieties) count as wild
        assertEquals("wild", new TankmateGenes("purple", "albino").phenotype(b));
        TankmateGenes g = TankmateGenes.load(new TankmateGenes("albino", "wild").save(), b);
        assertEquals(new TankmateGenes("albino", "wild"), g);
        assertEquals("wild", TankmateGenes.load(new CompoundTag(), b).a());
    }

    @Test
    void carrierCrossGivesAQuarterAlbinos() {
        TankmateSpecies b = TankmateSpecies.BRISTLENOSE_PLECO;
        TankmateGenes carrier = new TankmateGenes("wild", "albino");
        Random r = new Random(19);
        int albino = 0, n = 8000;
        for (int i = 0; i < n; i++) if (TankmateGenes.cross(carrier, carrier, r::nextLong).phenotype(b).equals("albino")) albino++;
        assertEquals(0.25, albino / (double) n, 0.02, "Aa x Aa: 1/4 albino");
        // albino x albino breeds true
        TankmateGenes a = TankmateGenes.pure(b, "albino");
        for (int i = 0; i < 200; i++) assertEquals("albino", TankmateGenes.cross(a, a, r::nextLong).phenotype(b));
        // wild animals now and then carry a bred variety
        int carriers = 0;
        for (int i = 0; i < 4000; i++) if (!TankmateGenes.wild(b, 0.1, r::nextLong).pure()) carriers++;
        assertEquals(0.1, carriers / 4000.0, 0.02);
    }

    @Test
    void clutchRoundTripsAndKeepsTime() {
        Clutch c = new Clutch(7, TankmateSpecies.PANDA_CORY, Clutch.Kind.GLASS, 10.5, 64.2, -3.06, 14,
                TankmateGenes.wild(TankmateSpecies.PANDA_CORY), TankmateGenes.wild(TankmateSpecies.PANDA_CORY));
        c.face = 2;
        c.age = 0.7;
        c.cover = 0.3;
        c.generation = 3;
        Clutch d = Clutch.load(c.save());
        assertNotNull(d);
        assertEquals(7, d.id);
        assertEquals(Clutch.Kind.GLASS, d.kind);
        assertEquals(14, d.count);
        assertEquals(2, d.face);
        assertEquals(0.7, d.age, 1e-9);
        assertEquals(3, d.generation);
        // corydoras eggs need a day, then half a day as larvae; at 0.7 days they are still eggs
        assertEquals(1.5, c.totalDays(), 1e-9);
        assertEquals(0, c.stage());
        c.age = 1.2;
        assertEquals(1, c.stage());
        // the client sees stage and count, no genes
        Clutch v = Clutch.loadClientView(c.saveClientView());
        assertNotNull(v);
        assertEquals(1, v.stage());
        assertEquals(14, v.count);
        // an unknown species is dropped
        CompoundTag bad = c.save();
        bad.putString("species", "megalodon");
        assertNull(Clutch.load(bad));
        // broken positions too
        CompoundTag nan = c.save();
        nan.putDouble("x", Double.NaN);
        assertNull(Clutch.load(nan));
    }

    @Test
    void scattererEggsAreFastPlecoLarvaeSlow() {
        assertTrue(Clutch.eggDays(TankmateSpecies.NEON_TETRA) < Clutch.eggDays(TankmateSpecies.PANDA_CORY));
        assertTrue(Clutch.larvaDays(TankmateSpecies.BRISTLENOSE_PLECO) > Clutch.larvaDays(TankmateSpecies.NEON_TETRA));
        assertEquals(0, Clutch.eggDays(TankmateSpecies.CLOWN_LOACH));
    }

    @Test
    void puffersBarbsAndLoaches() {
        // M29 part b: puffers eat snails, need them for their teeth and nip fins; tiger barbs nip, cherry barbs do not
        for (TankmateSpecies s : java.util.List.of(TankmateSpecies.DWARF_PUFFER, TankmateSpecies.RED_EYE_PUFFER, TankmateSpecies.GREEN_SPOTTED_PUFFER)) {
            assertTrue(s.puffer() && s.eatsSnails() && s.finNipper(), s.id());
            assertEquals("features.puffers", s.featureKey());
            assertEquals(org.overengineer.bettacraft.food.FoodDiet.Diet.PUFFER, org.overengineer.bettacraft.food.FoodDiet.of(s));
        }
        assertTrue(TankmateSpecies.TIGER_BARB.finNipper() && !TankmateSpecies.CHERRY_BARB.finNipper() && !TankmateSpecies.ODESSA_BARB.finNipper());
        assertTrue(TankmateSpecies.ANGELFISH.longFins(false) && TankmateSpecies.GUPPY.longFins(false) && !TankmateSpecies.GUPPY.longFins(true));
        assertTrue(!TankmateSpecies.NEON_TETRA.longFins(false));
        // the green spotted puffer: brackish water only
        assertTrue(!TankmateSpecies.GREEN_SPOTTED_PUFFER.salinity().contains(0) && TankmateSpecies.GREEN_SPOTTED_PUFFER.salinity().contains(15));
        assertTrue(!TankmateSpecies.GREEN_SPOTTED_PUFFER.marine(), "brackish, not a reef fish");
        assertTrue(TankmateSpecies.GREEN_SPOTTED_PUFFER.nativeWeight(TankmateSpecies.Water.MANGROVE) > 0);
        // the loaches eat snails, the garra grazes
        assertTrue(TankmateSpecies.DWARF_CHAIN_LOACH.eatsSnails() && TankmateSpecies.RETICULATED_LOACH.eatsSnails());
        assertTrue(TankmateSpecies.GARRA_RUFA.grazer() && !TankmateSpecies.GARRA_RUFA.eatsSnails());
        assertEquals("features.barbsAndLoaches", TankmateSpecies.GARRA_RUFA.featureKey());
        assertEquals("features.wildFish", TankmateSpecies.CLOWN_LOACH.featureKey());
        assertTrue(TankmateSpecies.CHERRY_BARB.tolerates(26) && TankmateSpecies.CHERRY_BARB.tolerates(28), "cherry barbs live in a betta tank");
    }

    @Test
    void cichlidsBroodAndSize() {
        // M29 part a: open brooders and cave brooders, the discus young stay with their parents longer
        assertEquals(TankmateSpecies.Breeding.OPEN, TankmateSpecies.RAM_CICHLID.breeding());
        assertEquals(TankmateSpecies.Breeding.CICHLID_CAVE, TankmateSpecies.COCKATOO_CICHLID.breeding());
        assertTrue(Clutch.larvaDays(TankmateSpecies.DISCUS) > Clutch.larvaDays(TankmateSpecies.ANGELFISH) + 1.0);
        assertTrue(TankmateSpecies.DISCUS.parentsFeed() && !TankmateSpecies.ANGELFISH.parentsFeed());
        assertTrue(TankmateSpecies.DISCUS.tolerates(30) && TankmateSpecies.DISCUS.breedingTemp(30), "discus at 30 C");
        assertTrue(TankmateSpecies.ANGELFISH.preyLength() >= TankmateSpecies.NEON_TETRA.length(), "an angelfish eats neons");
        assertTrue(TankmateSpecies.ANGELFISH.preyLength() < TankmateSpecies.PLATY.length(), "not platys");
        assertEquals(1, org.overengineer.bettacraft.containers.AnimalContainers.bucket(TankmateSpecies.ANGELFISH), "one angelfish per bucket");
        assertEquals(1, org.overengineer.bettacraft.containers.AnimalContainers.bucket(TankmateSpecies.DISCUS), "one discus per bucket");
        assertEquals(12, org.overengineer.bettacraft.containers.AnimalContainers.bucket(TankmateSpecies.RAM_CICHLID));
        assertTrue(TankmateSpecies.RAM_CICHLID.tolerates(26) && TankmateSpecies.RAM_CICHLID.tolerates(28), "rams live in a betta tank");
        assertTrue(TankmateSpecies.DWARF_GOURAMI.labyrinth() && !TankmateSpecies.RAM_CICHLID.labyrinth());
        assertEquals("features.cichlids", TankmateSpecies.DISCUS.featureKey());
        Clutch c = new Clutch(3, TankmateSpecies.ANGELFISH, Clutch.Kind.OPEN, 1, 2, 3, 30, TankmateGenes.wild(TankmateSpecies.ANGELFISH),
                TankmateGenes.wild(TankmateSpecies.ANGELFISH));
        c.guardian = java.util.UUID.randomUUID();
        c.partner = java.util.UUID.randomUUID();
        Clutch d = Clutch.load(c.save());
        assertNotNull(d);
        assertEquals(Clutch.Kind.OPEN, d.kind);
        assertEquals(c.partner, d.partner);
    }

    @Test
    void bettaCompatibleChoices() {
        // species that share the bettas' 26-28 C (the handbook recommends them)
        for (TankmateSpecies s : new TankmateSpecies[]{TankmateSpecies.CARDINAL_TETRA, TankmateSpecies.EMBER_TETRA, TankmateSpecies.HARLEQUIN_RASBORA,
                TankmateSpecies.MOSQUITO_RASBORA, TankmateSpecies.STERBAI_CORY, TankmateSpecies.KUHLI_LOACH, TankmateSpecies.LEOPARD_FROG_PLECO}) {
            assertTrue(s.tolerates(26) && s.tolerates(28), s.id() + " lives at 26-28 C");
        }
        // the cool-water species do not like a warm betta tank
        assertFalse(TankmateSpecies.PANDA_CORY.tolerates(28));
        assertTrue(TankmateSpecies.GUPPY.bettaTarget() && TankmateSpecies.DWARF_GOURAMI.bettaTarget());
        assertTrue(TankmateSpecies.CLOWN_LOACH.eatsSnails());
        assertEquals(TankmateSpecies.Breeding.NONE, TankmateSpecies.CLOWN_LOACH.breeding());
    }
}
