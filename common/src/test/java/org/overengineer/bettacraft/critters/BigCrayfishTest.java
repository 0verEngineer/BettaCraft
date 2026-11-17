package org.overengineer.bettacraft.critters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.tank.AxolotlCare;

/** M29 part d: the big crayfish (parthenogenesis, catching small animals) and the axolotl colour genetics. */
class BigCrayfishTest {
    private static CritterLife.Env env(double hiding) {
        Map<CritterSpecies.Group, Double> b = new EnumMap<>(CritterSpecies.Group.class);
        for (CritterSpecies.Group g : CritterSpecies.Group.values()) b.put(g, 1.0);
        return new CritterLife.Env(0.95F, true, 22, 0, hiding, 200, b, true, true);
    }

    private static Critter adult(int id, CritterSpecies s, boolean female) {
        Critter c = new Critter(id, s, female, CritterGenes.WILD, 1.5, 1.0 / 32, 1.0);
        c.age = s.juvenileDays() + 1;
        return c;
    }

    @Test
    void speciesData() {
        assertTrue(CritterSpecies.FLORIDA_CRAYFISH.big() && CritterSpecies.MARBLED_CRAYFISH.big());
        assertFalse(CritterSpecies.DWARF_CRAYFISH.big());
        assertTrue(CritterSpecies.MARBLED_CRAYFISH.allFemale() && CritterSpecies.MARBLED_CRAYFISH.hasSexes());
        assertEquals("blue", CritterGenes.pure(0, 0.5F).variety(CritterSpecies.FLORIDA_CRAYFISH));
        assertEquals("orange", CritterGenes.pure(1, 0.5F).variety(CritterSpecies.FLORIDA_CRAYFISH));
        assertEquals("wild", CritterGenes.WILD.variety(CritterSpecies.MARBLED_CRAYFISH));
        // a marbled crayfish is always female, whatever the caller asks for
        assertTrue(new Critter(1, CritterSpecies.MARBLED_CRAYFISH, false, CritterGenes.WILD, 0, 0, 0).female);
    }

    @Test
    void oneMarbledCrayfishFoundsAColonyOfFemales() {
        Random rnd = new Random(29);
        AtomicInteger ids = new AtomicInteger(100);
        List<Critter> l = new ArrayList<>(List.of(adult(1, CritterSpecies.MARBLED_CRAYFISH, true)));
        double[] pool = {1000};
        for (int i = 0; i < 30 * 24; i++) {
            pool[0] = 1000;
            CritterLife.step(l, new ArrayList<>(), env(0.9), 1.0 / 24, pool, rnd, ids::getAndIncrement);
        }
        long alive = l.stream().filter(Critter::alive).count();
        assertTrue(alive >= 6, "colony " + alive);
        assertTrue(l.stream().allMatch(c -> c.female), "only females");
    }

    @Test
    void floridaCrayfishNeedAPair() {
        Random rnd = new Random(30);
        AtomicInteger ids = new AtomicInteger(100);
        List<Critter> l = new ArrayList<>(List.of(adult(1, CritterSpecies.FLORIDA_CRAYFISH, true)));
        for (int i = 0; i < 20 * 24; i++) CritterLife.step(l, new ArrayList<>(), env(0.9), 1.0 / 24, new double[]{1000}, rnd, ids::getAndIncrement);
        assertEquals(1, l.size(), "no young without a male");
    }

    @Test
    void bigCrayfishCatchShrimpAndPlantsHideThem() {
        int open = eaten(0.0), hidden = eaten(0.9);
        assertTrue(open >= 3, "open " + open);
        assertTrue(hidden < open, "hidden " + hidden + " open " + open);
        assertEquals(0, CritterLife.bigCrayfishWeight(adult(9, CritterSpecies.FLORIDA_CRAYFISH, true)), 1e-9);
        assertTrue(CritterLife.bigCrayfishWeight(adult(9, CritterSpecies.NEOCARIDINA, true)) > 0);
    }

    private static int eaten(double hiding) {
        Random rnd = new Random(31);
        AtomicInteger ids = new AtomicInteger(1000);
        List<Critter> l = new ArrayList<>();
        l.add(adult(1, CritterSpecies.FLORIDA_CRAYFISH, true));
        for (int i = 0; i < 40; i++) l.add(adult(10 + i, CritterSpecies.NEOCARIDINA, i % 2 == 0));
        int n = 0;
        Map<CritterSpecies.Group, Double> none = new EnumMap<>(CritterSpecies.Group.class);
        for (CritterSpecies.Group g : CritterSpecies.Group.values()) none.put(g, 0.0);
        CritterLife.Env e = new CritterLife.Env(0.95F, true, 22, 0, hiding, 200, none, true, true);
        for (int i = 0; i < 10 * 24; i++) {
            n += (int) CritterLife.step(l, new ArrayList<>(), e, 1.0 / 24, new double[]{1000}, rnd, ids::getAndIncrement).eaten.stream()
                    .filter(c -> c.species == CritterSpecies.NEOCARIDINA).count();
        }
        return n;
    }

    @Test
    void axolotlColoursFollowMendel() {
        // every colour is recessive: a pure line shows it, a cross of two colours gives wild young
        for (Axolotl.Variant v : Axolotl.Variant.values()) {
            int l = AxolotlCare.locus(v);
            CritterGenes g = l < 0 ? CritterGenes.WILD : CritterGenes.pure(l, 0.1F);
            assertEquals(v, AxolotlCare.variant(g));
        }
        Random rnd = new Random(32);
        CritterGenes blue = CritterGenes.pure(AxolotlCare.BLUE, 0.1F), gold = CritterGenes.pure(AxolotlCare.GOLD, 0.1F);
        for (int i = 0; i < 20; i++) assertEquals(Axolotl.Variant.WILD, AxolotlCare.variant(AxolotlCare.cross(blue, gold, rnd::nextLong)));
        // two carriers of blue: about a quarter blue young
        CritterGenes carrier = new CritterGenes(1 << AxolotlCare.BLUE, 0, 0.1F);
        int b = 0;
        for (int i = 0; i < 4000; i++) if (AxolotlCare.variant(AxolotlCare.cross(carrier, carrier, rnd::nextLong)) == Axolotl.Variant.BLUE) b++;
        assertTrue(b > 850 && b < 1150, "blue " + b);
        // blue over the other colours
        assertEquals(Axolotl.Variant.BLUE, AxolotlCare.variant(new CritterGenes(0b1001, 0b1001, 0.1F)));
        // a wild-caught axolotl keeps its colour and rarely carries blue hidden
        int carriers = 0;
        for (int i = 0; i < 5000; i++) {
            CritterGenes g = AxolotlCare.genesFor(Axolotl.Variant.LUCY, rnd::nextLong);
            assertEquals(Axolotl.Variant.LUCY, AxolotlCare.variant(g));
            if (g.carries(AxolotlCare.BLUE)) carriers++;
        }
        assertTrue(carriers > 200 && carriers < 400, "carriers " + carriers);
        assertEquals("gold/gold blue/+", AxolotlCare.genotypeText(new CritterGenes(0b1010, 0b0010, 0.1F)));
        assertTrue(AxolotlCare.breedingTemp(18) && !AxolotlCare.breedingTemp(22) && !AxolotlCare.breedingTemp(12));
        assertEquals(0, AxolotlCare.heatDamagePerDay(23, 1), 1e-9);
        assertEquals(3, AxolotlCare.heatDamagePerDay(26, 1), 1e-9);
    }
}
