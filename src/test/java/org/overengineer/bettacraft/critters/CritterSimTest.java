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
import org.junit.jupiter.api.Test;

/**
 * M14: movement (floor, steps, glass, jumps, no way out of the tank, burrowing) and life (breeding by type, nerites never,
 * clutches only with air, water and temperature stress, bettas and hiding spots, assassin snails, food, sync).
 */
class CritterSimTest {
    /** A 4x2x2 tank at the origin with a flat floor (1/32), an optional rock in the middle and water up to {@code surface}. */
    static final class Box implements CritterMovement.Terrain {
        double surface = 1.9;
        double rockTop = -1;
        boolean day = true;
        double air = Double.NaN;
        final java.util.Set<Long> notch = new java.util.HashSet<>();

        @Override
        public double ground(double x, double z) {
            if (x < 0 || z < 0 || x >= 4 || z >= 2) return Double.NaN;
            if (rockTop > 0 && Math.abs(x - 2) < 0.4 && Math.abs(z - 1) < 0.4) return rockTop;
            return 1.0 / 32;
        }

        @Override public double surface() { return surface; }

        @Override
        public boolean member(int x, int y, int z) {
            return x >= 0 && x < 4 && y >= 0 && y < 2 && z >= 0 && z < 2 && !notch.contains(net.minecraft.core.BlockPos.asLong(x, y, z));
        }

        @Override public double algae(int x, int y, int z, int dir) { return x == 3 && dir == 3 ? 1 : 0; }
        @Override public boolean day() { return day; }
        @Override public int minX() { return 0; }
        @Override public int minY() { return 0; }
        @Override public int minZ() { return 0; }
        @Override public int maxX() { return 3; }
        @Override public int maxY() { return 1; }
        @Override public int maxZ() { return 1; }
        @Override public double airTop() { return air; }
    }

    private static CritterLife.Env env(float quality, float temp, int bettas, double hiding, boolean air) {
        Map<CritterSpecies.Group, Double> b = new EnumMap<>(CritterSpecies.Group.class);
        for (CritterSpecies.Group g : CritterSpecies.Group.values()) b.put(g, 1.0);
        return new CritterLife.Env(quality, true, temp, bettas, hiding, 200, b, air, true);
    }

    private static Critter adult(int id, CritterSpecies s, boolean female) {
        Critter c = new Critter(id, s, female, CritterGenes.WILD, 1.5, 1.0 / 32, 1.0);
        c.age = s.juvenileDays() + 1;
        return c;
    }

    // ------------------------------------------------------------------ movement

    @Test
    void animalsStayInsideTheTankAndUnderWater() {
        Box t = new Box();
        t.rockTop = 0.6;
        t.notch.add(net.minecraft.core.BlockPos.asLong(0, 0, 0));
        t.notch.add(net.minecraft.core.BlockPos.asLong(0, 1, 0));
        Random rnd = new Random(5);
        List<Critter> all = new ArrayList<>();
        int id = 1;
        for (CritterSpecies s : CritterSpecies.values()) {
            for (int i = 0; i < 3; i++) {
                Critter c = adult(id++, s, i % 2 == 0);
                c.x = 2.5 + i * 0.3;
                c.z = 1.0;
                all.add(c);
            }
        }
        int onGlass = 0, jumped = 0, onRock = 0;
        for (int sec = 0; sec < 1500; sec++) {
            for (Critter c : all) {
                CritterMovement.step(c, t, rnd);
                assertTrue(c.x > 0 && c.x < 4 && c.z > 0 && c.z < 2, c.species + " left the tank: " + c.x + " " + c.z);
                assertFalse(c.x < 1 && c.z < 1 && c.face == Critter.FLOOR, c.species + " walked into the notch");
                assertTrue(c.y <= t.surface() + 0.01, c.species + " above the water");
                assertTrue(c.y >= 0, "below the floor");
                if (c.onGlass()) onGlass++;
                if (c.face == Critter.SWIM) jumped++;
                if (c.face == Critter.FLOOR && c.y > 0.5) onRock++;
            }
        }
        assertTrue(onGlass > 0, "snails and shrimp climb the glass");
        assertTrue(jumped > 0, "shrimp jump through the water");
        assertTrue(onRock > 0, "they walk on the decoration");
    }

    @Test
    void nerivesGoForTheAlgaeAndTrumpetSnailsDigInByDay() {
        Box t = new Box();
        Random rnd = new Random(11);
        Critter n = adult(1, CritterSpecies.ZEBRA_NERITE, true);
        int eastGlass = 0, otherGlass = 0;
        for (int sec = 0; sec < 6000; sec++) {
            CritterMovement.step(n, t, rnd);
            if (n.face == Critter.GLASS_EAST) eastGlass++;
            else if (n.onGlass()) otherGlass++;
        }
        assertTrue(eastGlass > otherGlass, "the algae face is grazed most (" + eastGlass + " vs " + otherGlass + ")");
        Critter m = adult(2, CritterSpecies.TRUMPET_SNAIL, true);
        boolean buried = false;
        for (int sec = 0; sec < 2000 && !buried; sec++) {
            CritterMovement.step(m, t, rnd);
            buried = m.buried;
        }
        assertTrue(buried, "dug in by day");
        t.day = false;
        CritterMovement.step(m, t, rnd);
        assertFalse(m.buried, "comes out at night");
        // a dry tank: nobody moves
        t.surface = 0;
        double x = n.x;
        for (int i = 0; i < 50; i++) CritterMovement.step(n, t, rnd);
        assertEquals(x, n.x);
    }

    @Test
    void appleSnailClimbsAboveTheWaterToLay() {
        Box t = new Box();
        t.surface = 1.9;
        t.air = 1.98;
        Random rnd = new Random(2);
        Critter a = adult(1, CritterSpecies.APPLE_SNAIL, true);
        a.eggs = 0;
        a.father = CritterGenes.pure(0, 0.3F);
        boolean above = false;
        for (int sec = 0; sec < 6000 && !above; sec++) {
            CritterMovement.step(a, t, rnd);
            above = a.onGlass() && a.y > t.surface();
        }
        assertTrue(above, "on the glass above the water line");
        CritterObject clutch = CritterLife.layClutch(a, 9, rnd);
        assertTrue(clutch.y > t.surface());
        assertFalse(a.berried());
        assertTrue(clutch.count >= CritterSpecies.APPLE_SNAIL.broodMin());
    }

    // ------------------------------------------------------------------ life

    @Test
    void shrimpBreedAndTheYoungGrowUp() {
        Random rnd = new Random(4);
        List<Critter> l = new ArrayList<>(List.of(adult(1, CritterSpecies.NEOCARIDINA, true), adult(2, CritterSpecies.NEOCARIDINA, false)));
        l.get(0).genes = CritterGenes.pure(0, 0.5F);
        l.get(1).genes = CritterGenes.pure(0, 0.5F);
        AtomicInteger ids = new AtomicInteger(10);
        double[] pool = {100};
        boolean berried = false;
        for (int i = 0; i < 24 * 20; i++) {
            pool[0] = 100;
            CritterLife.step(l, new ArrayList<>(), env(0.9F, 25, 0, 0, true), 1.0 / 24, pool, rnd, ids::getAndIncrement);
            berried |= l.get(0).berried();
        }
        assertTrue(berried, "the female carried eggs");
        long young = l.stream().filter(c -> c.generation == 1).count();
        assertTrue(young >= 3, "young hatched: " + young);
        assertTrue(l.stream().filter(c -> c.generation == 1).allMatch(c -> "sakura".equals(c.genes.variety(CritterSpecies.NEOCARIDINA))
                || "red_cherry".equals(c.genes.variety(CritterSpecies.NEOCARIDINA)) || "fire_red".equals(c.genes.variety(CritterSpecies.NEOCARIDINA))),
                "red x red gives red young");
        assertTrue(l.stream().anyMatch(c -> c.generation == 1 && c.adult()), "young grow up");
    }

    /** M25 part c: Caridina breed among themselves, but never with the Neocaridina. */
    @Test
    void caridinaDoNotCrossWithNeocaridina() {
        Random rnd = new Random(25);
        AtomicInteger ids = new AtomicInteger(10);
        List<Critter> mixed = new ArrayList<>(List.of(adult(1, CritterSpecies.NEOCARIDINA, true), adult(2, CritterSpecies.CARIDINA, false),
                adult(3, CritterSpecies.CARIDINA, true), adult(4, CritterSpecies.NEOCARIDINA, false)));
        mixed.get(1).genes = CritterGenes.pure(CritterGenes.CARIDINA_RED, 0.5F);
        mixed.get(2).genes = CritterGenes.pure(CritterGenes.CARIDINA_RED, 0.5F);
        for (int i = 0; i < 24 * 30; i++) {
            CritterLife.step(mixed, new ArrayList<>(), env(0.9F, 22, 0, 0, true), 1.0 / 24, new double[]{100}, rnd, ids::getAndIncrement);
        }
        long caridina = mixed.stream().filter(c -> c.generation == 1 && c.species == CritterSpecies.CARIDINA).count();
        assertTrue(caridina >= 2, "the bee shrimp bred: " + caridina);
        // every young Caridina is a crystal red (red x red), and every young Neocaridina wild (wild x wild): no hybrids
        assertTrue(mixed.stream().filter(c -> c.generation == 1 && c.species == CritterSpecies.CARIDINA)
                .allMatch(c -> c.genes.variety(CritterSpecies.CARIDINA).startsWith("crystal_red")), "red x red gives crystal reds");
        assertTrue(mixed.stream().filter(c -> c.generation == 1 && c.species == CritterSpecies.NEOCARIDINA)
                .allMatch(c -> c.genes.carried() == 0), "no colour of the Caridina in a Neocaridina");
        // a lone Neocaridina female with Caridina males: nothing
        List<Critter> lone = new ArrayList<>(List.of(adult(1, CritterSpecies.NEOCARIDINA, true), adult(2, CritterSpecies.CARIDINA, false)));
        for (int i = 0; i < 24 * 20; i++) CritterLife.step(lone, new ArrayList<>(), env(0.9F, 22, 0, 0, true), 1.0 / 24, new double[]{100}, rnd, ids::getAndIncrement);
        assertTrue(lone.stream().noneMatch(c -> c.generation == 1) && !lone.get(0).berried(), "no young without a mate of the own species");
    }

    @Test
    void neritesNeverBreedOthersSlowly() {
        Random rnd = new Random(8);
        AtomicInteger ids = new AtomicInteger(100);
        List<Critter> nerites = new ArrayList<>();
        for (int i = 0; i < 10; i++) nerites.add(adult(i, CritterSpecies.ZEBRA_NERITE, i % 2 == 0));
        List<Critter> ram = new ArrayList<>(List.of(adult(50, CritterSpecies.RAMSHORN, false), adult(51, CritterSpecies.RAMSHORN, false),
                adult(52, CritterSpecies.RAMSHORN, false)));
        for (int i = 0; i < 20 * 24; i++) {
            CritterLife.step(nerites, new ArrayList<>(), env(0.9F, 25, 0, 0, true), 1.0 / 24, new double[]{100}, rnd, ids::getAndIncrement);
            CritterLife.step(ram, new ArrayList<>(), env(0.9F, 25, 0, 0, true), 1.0 / 24, new double[]{100}, rnd, ids::getAndIncrement);
        }
        assertEquals(10, nerites.size(), "nerites lay no eggs in fresh water");
        assertTrue(ram.size() > 3, "hermaphrodites: no partner of the other sex needed");
        assertTrue(ram.size() < 40, "but not rapidly: " + ram.size());
    }

    @Test
    void clutchesNeedAirAndHatch() {
        Random rnd = new Random(9);
        AtomicInteger ids = new AtomicInteger(100);
        List<Critter> l = new ArrayList<>(List.of(adult(1, CritterSpecies.APPLE_SNAIL, true), adult(2, CritterSpecies.APPLE_SNAIL, false)));
        for (int i = 0; i < 20 * 24; i++) CritterLife.step(l, new ArrayList<>(), env(0.9F, 25, 0, 0, false), 1.0 / 24, new double[]{100}, rnd, ids::getAndIncrement);
        assertFalse(l.get(0).berried(), "no air: no clutch");
        assertEquals(2, l.size());
        List<CritterObject> objects = new ArrayList<>();
        CritterObject c = CritterLife.layClutch(l.get(0), 5, rnd);
        objects.add(c);
        int before = l.size();
        for (int i = 0; i < 4 * 24; i++) CritterLife.step(l, objects, env(0.9F, 25, 0, 0, true), 1.0 / 24, new double[]{100}, rnd, ids::getAndIncrement);
        assertTrue(objects.stream().noneMatch(o -> o.kind == CritterObject.Kind.CLUTCH && o.id == 5), "the clutch hatched");
        assertTrue(l.size() >= before + CritterSpecies.APPLE_SNAIL.broodMin(), "young snails: " + (l.size() - before));
    }

    @Test
    void badWaterAndWrongTemperatureKillShrimpNotSnails() {
        Random rnd = new Random(12);
        AtomicInteger ids = new AtomicInteger(100);
        List<Critter> l = new ArrayList<>();
        for (int i = 0; i < 20; i++) l.add(adult(i, CritterSpecies.NEOCARIDINA, i % 2 == 0));
        for (int i = 0; i < 20; i++) l.add(adult(100 + i, CritterSpecies.RAMSHORN, false));
        long deadShrimp = 0, deadSnails = 0;
        for (int i = 0; i < 2 * 24; i++) {
            var r = CritterLife.step(l, new ArrayList<>(), env(0.4F, 25, 0, 0, true), 1.0 / 24, new double[]{100}, rnd, ids::getAndIncrement);
            deadShrimp += r.died.stream().filter(c -> c.species == CritterSpecies.NEOCARIDINA).count();
            deadSnails += r.died.stream().filter(c -> c.species == CritterSpecies.RAMSHORN).count();
        }
        assertTrue(deadShrimp >= 2, "shrimp die in poor water: " + deadShrimp);
        assertEquals(0, deadSnails, "ramshorns take it");
        List<Critter> cold = new ArrayList<>();
        for (int i = 0; i < 20; i++) cold.add(adult(i, CritterSpecies.NEOCARIDINA, true));
        for (int i = 0; i < 2 * 24; i++) CritterLife.step(cold, new ArrayList<>(), env(1F, 12, 0, 0, true), 1.0 / 24, new double[]{100}, rnd, ids::getAndIncrement);
        assertTrue(cold.stream().anyMatch(c -> !c.alive()), "12 C is too cold");
        // bodies are eaten / rot away
        for (int i = 0; i < 3 * 24; i++) CritterLife.step(l, new ArrayList<>(), env(0.95F, 25, 0, 0, true), 1.0 / 24, new double[]{100}, rnd, ids::getAndIncrement);
        assertTrue(l.stream().allMatch(Critter::alive), "the bodies are gone");
    }

    @Test
    void bettasHuntTheYoungPlantsHideThem() {
        Random rnd = new Random(13);
        AtomicInteger ids = new AtomicInteger(1000);
        int open = survivors(rnd, ids, 0.0), hidden = survivors(rnd, ids, 0.9);
        assertTrue(open < 16, "without plants most young are eaten: " + open + " of 40");
        assertTrue(hidden > 25, "dense plants and moss hide them: " + hidden + " of 40");
        // adults only now and then
        List<Critter> adults = new ArrayList<>();
        for (int i = 0; i < 40; i++) adults.add(adult(i, CritterSpecies.NEOCARIDINA, true));
        CritterLife.Result r = null;
        int eaten = 0;
        for (int i = 0; i < 24; i++) {
            r = CritterLife.step(adults, new ArrayList<>(), env(0.95F, 25, 1, 0, true), 1.0 / 24, new double[]{100}, rnd, ids::getAndIncrement);
            eaten += r.eaten.size();
        }
        assertTrue(eaten <= 4, "adults rarely: " + eaten);
        // snails and adult crayfish are safe
        List<Critter> safe = new ArrayList<>(List.of(adult(1, CritterSpecies.ZEBRA_NERITE, true), adult(2, CritterSpecies.DWARF_CRAYFISH, true)));
        safe.get(1).sinceMolt = 10;
        for (int i = 0; i < 48; i++) CritterLife.step(safe, new ArrayList<>(), env(0.95F, 22, 3, 0, true), 1.0 / 24, new double[]{100}, rnd, ids::getAndIncrement);
        assertEquals(2, safe.size());
    }

    private static int survivors(Random rnd, AtomicInteger ids, double hiding) {
        List<Critter> young = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            Critter c = new Critter(ids.getAndIncrement(), CritterSpecies.NEOCARIDINA, true, CritterGenes.WILD, 1, 0.03, 1);
            c.age = 0;
            young.add(c);
        }
        for (int i = 0; i < 3 * 24; i++) {
            CritterLife.step(young, new ArrayList<>(), env(0.95F, 25, 1, hiding, true), 1.0 / 24, new double[]{100}, rnd, ids::getAndIncrement);
        }
        return (int) young.stream().filter(c -> c.species == CritterSpecies.NEOCARIDINA && c.generation == 0 && c.alive()).count();
    }

    @Test
    void assassinSnailsEatOtherSnailsNeritesRarely() {
        Random rnd = new Random(14);
        AtomicInteger ids = new AtomicInteger(1000);
        List<Critter> l = new ArrayList<>();
        for (int i = 0; i < 2; i++) l.add(adult(i, CritterSpecies.ASSASSIN_SNAIL, i == 0));
        for (int i = 0; i < 20; i++) l.add(adult(10 + i, CritterSpecies.BLADDER_SNAIL, false));
        for (int i = 0; i < 20; i++) l.add(adult(40 + i, CritterSpecies.OLIVE_NERITE, false));
        CritterLife.Env e = env(0.95F, 25, 0, 0, true);
        Map<CritterSpecies.Group, Double> none = new EnumMap<>(CritterSpecies.Group.class);
        for (CritterSpecies.Group g : CritterSpecies.Group.values()) none.put(g, 0.0);
        e = new CritterLife.Env(e.quality(), true, e.temp(), 0, 0, 200, none, true, true);
        for (int i = 0; i < 10 * 24; i++) CritterLife.step(l, new ArrayList<>(), e, 1.0 / 24, new double[]{100}, rnd, ids::getAndIncrement);
        long bladder = l.stream().filter(c -> c.species == CritterSpecies.BLADDER_SNAIL).count();
        long nerites = l.stream().filter(c -> c.species == CritterSpecies.OLIVE_NERITE).count();
        assertTrue(bladder < 17, "assassins thin the bladder snails out: " + bladder);
        assertTrue(20 - nerites < 20 - bladder, "nerites much less (" + nerites + " left)");
        assertEquals(2, l.stream().filter(c -> c.species == CritterSpecies.ASSASSIN_SNAIL).count(), "assassins never eat each other");
    }

    @Test
    void foodLimitsTheColony() {
        Random rnd = new Random(15);
        AtomicInteger ids = new AtomicInteger(1000);
        List<Critter> l = new ArrayList<>();
        for (int i = 0; i < 30; i++) l.add(adult(i, CritterSpecies.NEOCARIDINA, i % 2 == 0));
        double[] pool = {0};
        for (int i = 0; i < 12 * 24; i++) {
            pool[0] += 0.1 / 24; // a tank that grows little food: enough for 3 shrimp
            CritterLife.step(l, new ArrayList<>(), env(0.95F, 25, 0, 0, true), 1.0 / 24, pool, rnd, ids::getAndIncrement);
        }
        long alive = l.stream().filter(Critter::alive).count();
        assertTrue(alive < 20, "starving shrimp die: " + alive);
        assertTrue(CritterLife.poolCap(l) > 1);
    }

    @Test
    void crayfishMoultAndAreSoftAfterwards() {
        Random rnd = new Random(16);
        AtomicInteger ids = new AtomicInteger(1000);
        List<Critter> l = new ArrayList<>(List.of(adult(1, CritterSpecies.DWARF_CRAYFISH, true)));
        boolean molted = false, soft = false;
        for (int i = 0; i < 12 * 24; i++) {
            var r = CritterLife.step(l, new ArrayList<>(), env(0.95F, 22, 0, 0, true), 1.0 / 24, new double[]{100}, rnd, ids::getAndIncrement);
            molted |= !r.molted.isEmpty();
            soft |= l.getFirst().soft();
        }
        assertTrue(molted && soft);
        CritterObject m = CritterLife.molt(l.getFirst(), 3);
        assertEquals(CritterObject.Kind.MOLT, m.kind);
    }

    @Test
    void syncRoundTripAndSaveLoad() {
        List<Critter> l = new ArrayList<>();
        Critter a = adult(7, CritterSpecies.NEOCARIDINA, true);
        a.genes = CritterGenes.pure(1, 0.66F);
        a.eggs = 0.5;
        a.x = 103.25;
        a.y = 64.03;
        a.z = -20.5;
        a.yaw = 90;
        a.face = Critter.GLASS_WEST;
        l.add(a);
        Critter b = adult(8, CritterSpecies.TRUMPET_SNAIL, false);
        b.buried = true;
        b.x = 101;
        b.y = 64.1;
        b.z = -21;
        l.add(b);
        CritterObject clutch = new CritterObject(9, CritterObject.Kind.CLUTCH, 102, 65.9, -20);
        clutch.face = Critter.GLASS_NORTH;
        byte[] data = CritterSync.encode(l, List.of(clutch), 100, 64, -22, 0);
        var d = CritterSync.decode(data, 100, 64, -22);
        assertEquals(2, d.critters().size());
        Critter c = d.critters().getFirst();
        assertEquals(7, c.id);
        assertEquals(103.25, c.x, 0.002);
        assertEquals(-20.5, c.z, 0.002);
        assertEquals(1, c.clientColor);
        assertEquals(0.66F, c.clientGrade, 0.01F);
        assertEquals(Critter.GLASS_WEST, c.face);
        assertTrue((c.clientFlags & CritterSync.F_BERRIED) != 0 && (c.clientFlags & CritterSync.F_FEMALE) != 0);
        assertTrue(d.critters().get(1).buried);
        assertEquals(1, d.objects().size());
        assertEquals(CritterObject.Kind.CLUTCH, d.objects().getFirst().kind);
        // client gliding: new position starts where the old one was drawn
        List<Critter> fresh = CritterSync.decode(data, 100, 64, -22).critters();
        fresh.getFirst().x += 0.08;
        CritterSync.adopt(d.critters(), fresh, 100, 20);
        assertEquals(103.25, fresh.getFirst().renderX(100, 20), 0.003);
        assertEquals(103.33, fresh.getFirst().renderX(120, 20), 0.003);
        // NBT
        Critter back = Critter.load(a.save());
        assertEquals(a.genes, back.genes);
        assertEquals(a.eggs, back.eggs);
        assertEquals(Critter.GLASS_WEST, back.face);
        assertEquals(clutch.face, CritterObject.load(clutch.save()).face);
    }
}
