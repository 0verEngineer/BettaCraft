package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import org.overengineer.bettacraft.util.NbtCompat;
import java.util.ArrayList;
import java.util.List;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.frog.DwarfFrogEntity;
import org.overengineer.bettacraft.entity.frog.FrogBreeding;
import org.overengineer.bettacraft.entity.frog.FrogSpawn;
import org.overengineer.bettacraft.entity.frog.Tadpole;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.food.BettaFood;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.plants.FloatingPlant;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;

/**
 * M47 parts b and c: the breeding of the dwarf clawed frogs (a cool water change at 24-28 °C, amplexus, spawn at the
 * surface, tadpoles that need fine food, froglets), the spawn and the tadpoles eaten by bettas (floating plants hide the
 * spawn), the switch {@code features.frogs.breeding} off, frogs of older worlds with their sex from the UUID; the two new
 * ram forms in real fish (crosses, save and load of an old ram).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FrogBreedingGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private FrogBreedingGameTests() {}

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestFailure(what);
    }

    private static TankBlockEntity tank(GameTestHelper h, int x, int z, int sx, int sz, float temp) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 2, z, sx, 2, sz, sx * 2L * sz * 1000L);
        c.getTank().setWaterTemp(temp);
        c.getTank().setTargetTemp(temp);
        c.getTank().setWaterQuality(1F);
        return c;
    }

    static DwarfFrogEntity frog(GameTestHelper h, TankBlockEntity c, boolean female) {
        ServerLevel level = h.getLevel();
        DwarfFrogEntity f = ModEntities.DWARF_FROG.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (f == null) throw new GameTestFailure("no frog");
        f.setFemale(female);
        ItemStack bag = FishBagItem.fillWith(f);
        f.discard();
        var err = TankInhabitants.insert(level, c, bag, ModEntities.DWARF_FROG.get(), null, 0L);
        if (err != null) throw new GameTestFailure("frog in: " + err.getString());
        DwarfFrogEntity found = null;
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
            if (e instanceof DwarfFrogEntity d && d.isFemale() == female && !d.inAmplexus()) found = d;
        }
        if (found == null) throw new GameTestFailure("no frog in the tank");
        found.makeReadyForTest();
        return found;
    }

    private static List<DwarfFrogEntity> frogs(GameTestHelper h, TankBlockEntity c) {
        List<DwarfFrogEntity> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) if (e instanceof DwarfFrogEntity d) out.add(d);
        return out;
    }

    private static boolean logged(TankData t, String key) {
        return t.log().stream().anyMatch(e -> e.key().equals(key));
    }

    private static List<Tadpole> tadpoles(TankBlockEntity c, int n) {
        TankData t = c.getTank();
        TankWaterVolume w = new TankWaterVolume(t);
        var b = w.box();
        List<Tadpole> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Vec3 p = w.clamp(new Vec3(b.minX + 0.5 + (i % 4) * 0.4, b.minY + 1.0, b.minZ + 0.5 + (i / 4) * 0.3), 0.1, 0.1, 0.1);
            Tadpole k = new Tadpole(t.nextFrogBroodId(), p.x, p.y, p.z);
            t.tadpoles().add(k);
            out.add(k);
        }
        return out;
    }

    /**
     * The whole cycle: a cool water change at 26 °C sets the ready pair off (amplexus), the female carries the male up and
     * releases her spawn at the surface, the eggs hatch into tadpoles (log, advancement event), fed with infusoria they
     * become froglets that are drawn smaller and are not grown up.
     */
    @GameTest(template = EMPTY, timeoutTicks = 1400)
    public static void frogsBreedFromWaterChangeToFroglet(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 3, 2, 26F);
        DwarfFrogEntity male = frog(h, c, false), female = frog(h, c, true);
        TankData t = c.getTank();
        h.runAfterDelay(5, () -> {
            check(t.frogSpawn().isEmpty() && t.tadpoles().isEmpty(), "no brood before");
            // without the cool water change nothing happens
            FrogBreeding.step(level, c, 2.0);
            check(!male.inAmplexus(), "amplexus without a cool water change");
            t.setLastCoolChange(level.getGameTime());
            FrogBreeding.step(level, c, 2.0);
            check(male.inAmplexus() && female.inAmplexus(), "the pair went into amplexus after the cool water change");
            check(logged(t, "log.bettacraft.frog.amplexus"), "logged the amplexus");
        });
        h.runAfterDelay(30, () -> check(male.claspedForRender() == female, "the male clasps the female (synced)"));
        h.runAfterDelay(DwarfFrogEntity.AMPLEXUS_TICKS * 2 + 60, () -> {
            check(!male.inAmplexus(), "the amplexus ended");
            check(t.frogSpawn().size() == 1, "one clump of spawn: " + t.frogSpawn().size());
            FrogSpawn s = t.frogSpawn().getFirst();
            check(s.count >= FrogBreeding.EGGS_MIN && s.count <= FrogBreeding.EGGS_MAX, "eggs " + s.count);
            check(Math.abs(s.y - new TankWaterVolume(t).surface()) < 1e-3, "the spawn floats at the surface");
            check(female.restDays() > 0 && male.restDays() > 0, "the pair rests");
            // the eggs hatch
            int eggs = s.count;
            FrogBreeding.step(level, c, FrogBreeding.hatchDays() + 0.01);
            check(t.frogSpawn().isEmpty(), "the spawn hatched");
            // (the frogs snap at a few eggs)
            check(t.tadpoles().size() >= Math.min(eggs, FrogBreeding.maxTadpoles()) * 0.6 && t.tadpoles().size() <= FrogBreeding.maxTadpoles(),
                    "tadpoles " + t.tadpoles().size() + " of " + eggs);
            check(logged(t, "log.bettacraft.frog.tadpoles_hatched"), "logged the tadpoles");
            // fed with infusoria they grow up into froglets
            int before = frogs(h, c).size();
            for (double d = 0; d < FrogBreeding.tadpoleDays() + 0.5; d += 0.5) {
                FrogBreeding.feed(t, BettaFood.Kind.FRY_START, 200);
                FrogBreeding.step(level, c, 0.5);
            }
            List<DwarfFrogEntity> all = frogs(h, c);
            long froglets = all.stream().filter(f -> !f.isAdult()).count();
            check(all.size() > before && froglets == all.size() - before, "froglets: " + froglets + " of " + all.size());
            check(all.stream().filter(f -> !f.isAdult()).allMatch(f -> f.sizeFactor() < 1F), "froglets are drawn smaller");
            check(logged(t, "log.bettacraft.frog.froglets"), "logged the froglets");
            check(!logged(t, "log.bettacraft.frog.tadpoles_starved"), "no tadpole starved");
            h.succeed();
        });
    }

    /** Two bettas eat almost all of an open clump of spawn in a day; under dense floating plants much of it survives. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void bettasEatTheSpawnFloatingPlantsHideIt(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity open = tank(h, 1, 1, 3, 3, 26F), covered = tank(h, 6, 1, 3, 3, 26F), alone = tank(h, 11, 1, 3, 3, 26F);
        BettaGenotype g = BettaGenotype.homozygous(BettaGenome.DEFAULT);
        for (TankBlockEntity c : List.of(open, covered)) {
            CommunityFishGameTests.betta(h, c, g, false);
            CommunityFishGameTests.betta(h, c, g, true);
        }
        TankData ct = covered.getTank();
        double surf = new TankWaterVolume(ct).surface();
        for (int i = 0; TankPlants.coverage(ct) < 0.8 && i < 200; i++) {
            double x = ct.min().getX() + 0.25 + (i % 6) * 0.5, z = ct.min().getZ() + 0.25 + (i / 6 % 6) * 0.5;
            ct.plants().add(new FloatingPlant(ct.nextPlantId(), FloatingPlantType.WATER_LETTUCE, x, z, 1F, 0F));
        }
        check(FrogBreeding.spawnCover(ct) >= 0.85, "the plants cover the surface: " + TankPlants.coverage(ct));
        h.runAfterDelay(5, () -> {
            for (TankBlockEntity c : List.of(open, covered, alone)) {
                TankData t = c.getTank();
                double s = new TankWaterVolume(t).surface();
                t.frogSpawn().add(new FrogSpawn(t.nextFrogBroodId(), t.min().getX() + 1.5, s, t.min().getZ() + 1.5, 50));
                FrogBreeding.step(level, c, 1.0);
            }
            int o = FrogBreeding.eggs(open.getTank()), cv = FrogBreeding.eggs(covered.getTank()), a = FrogBreeding.eggs(alone.getTank());
            check(o <= 3, "eggs left in the open tank with bettas: " + o);
            check(cv >= 10, "eggs left under the floating plants: " + cv);
            check(a == 50, "without fish all eggs stay: " + a);
            if (o == 0) check(logged(open.getTank(), "log.bettacraft.frog.spawn_eaten"), "logged the eaten spawn");
            check(Math.abs(surf - new TankWaterVolume(ct).surface()) < 1e-6, "same water");
            h.succeed();
        });
    }

    /**
     * Tadpoles without fine food starve in a bare tank within three days; fed, all live; with two bettas in the tank hardly
     * one survives four days (fed).
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void tadpolesNeedFineFoodAndBettasEatThem(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity hungry = tank(h, 1, 1, 3, 3, 26F), fed = tank(h, 6, 1, 3, 3, 26F), bettas = tank(h, 11, 1, 3, 3, 26F);
        BettaGenotype g = BettaGenotype.homozygous(BettaGenome.DEFAULT);
        CommunityFishGameTests.betta(h, bettas, g, false);
        CommunityFishGameTests.betta(h, bettas, g, true);
        h.runAfterDelay(5, () -> {
            for (TankBlockEntity c : List.of(hungry, fed, bettas)) tadpoles(c, 10);
            check(TankPlants.coverage(hungry.getTank()) == 0 && !FrogBreeding.planted(level, hungry.getTank()), "a bare tank");
            for (double d = 0; d < 4.0; d += 0.5) {
                for (TankBlockEntity c : List.of(fed, bettas)) FrogBreeding.feed(c.getTank(), BettaFood.Kind.FRY, 100);
                for (TankBlockEntity c : List.of(hungry, fed, bettas)) FrogBreeding.step(level, c, 0.5);
            }
            check(hungry.getTank().tadpoles().isEmpty(), "unfed tadpoles starved: " + hungry.getTank().tadpoles().size());
            check(logged(hungry.getTank(), "log.bettacraft.frog.tadpoles_starved"), "logged the starved tadpoles");
            check(fed.getTank().tadpoles().size() == 10, "fed tadpoles live: " + fed.getTank().tadpoles().size());
            check(bettas.getTank().tadpoles().size() <= 1, "tadpoles left with bettas: " + bettas.getTank().tadpoles().size());
            check(logged(bettas.getTank(), "log.bettacraft.frog.tadpoles_eaten"), "logged the eaten tadpoles");
            // the food: only fine food, and a fry food item reaches the tadpoles through the normal feeding
            check(!FrogBreeding.wantFood(fed.getTank(), BettaFood.Kind.NORMAL) && FrogBreeding.eats(BettaFood.Kind.FRY_START), "fine food only");
            h.succeed();
        });
    }

    /** With {@code features.frogs.breeding} off a cool water change sets no pair off; spawn already laid still hatches. */
    @GameTest(template = EMPTY, batch = "bettacraft_m47_frogs_off", timeoutTicks = 100)
    public static void frogBreedingOffLaysNoSpawn(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 3, 2, 26F);
        DwarfFrogEntity male = frog(h, c, false), female = frog(h, c, true);
        Features.set(Feature.FROG_BREEDING, false);
        try {
            TankData t = c.getTank();
            t.setLastCoolChange(level.getGameTime());
            for (int i = 0; i < 5; i++) FrogBreeding.step(level, c, 2.0);
            check(!male.inAmplexus() && !female.inAmplexus(), "a pair in amplexus with the switch off");
            t.frogSpawn().add(new FrogSpawn(t.nextFrogBroodId(), t.min().getX() + 1.5, new TankWaterVolume(t).surface(), t.min().getZ() + 1.0, 12));
            FrogBreeding.step(level, c, FrogBreeding.hatchDays() + 0.1);
            check(t.tadpoles().size() >= 7, "laid spawn still hatches: " + t.tadpoles().size());
        } finally {
            Features.set(Feature.FROG_BREEDING, true);
        }
        h.succeed();
    }

    /**
     * A frog of an older world (no "female" in its data) gets its sex from its UUID and keeps it; a froglet keeps its age
     * through a fish bag; the tank's spawn and tadpoles survive a save and load of the tank data, a tank without them saves
     * no keys.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void oldFrogsLoadWithASexAndBroodIsSaved(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int i = 0; i < 6; i++) {
            DwarfFrogEntity f = ModEntities.DWARF_FROG.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            CompoundTag tag = new CompoundTag();
            tag.merge(NbtCompat.saveEntityWithoutId(f));
            tag.getCompoundOrEmpty("frog").remove("female");
            tag.getCompoundOrEmpty("frog").remove("young_days");
            DwarfFrogEntity g = ModEntities.DWARF_FROG.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            org.overengineer.bettacraft.util.NbtCompat.loadEntity(g, tag);
            boolean expected = (f.getUUID().getLeastSignificantBits() & 1L) == 1L;
            check(g.getUUID().equals(f.getUUID()) && g.isFemale() == expected && g.isAdult(), "old frog: sex from the UUID, grown");
            CompoundTag again = new CompoundTag();
            again.merge(NbtCompat.saveEntityWithoutId(g));
            check(again.getCompoundOrEmpty("frog").contains("female") && again.getCompoundOrEmpty("frog").getBooleanOr("female", false) == expected, "the sex is saved");
            f.discard();
            g.discard();
        }
        DwarfFrogEntity young = ModEntities.DWARF_FROG.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        young.makeFroglet();
        young.setFemale(true);
        ItemStack bag = FishBagItem.fillWith(young);
        DwarfFrogEntity back = ModEntities.DWARF_FROG.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        back.loadFromBucketTag(bag.get(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA).copyTag());
        check(back.isFemale() && !back.isAdult() && back.sizeFactor() < 1F, "a froglet in a bag stays a young female");
        young.discard();
        back.discard();
        TankBlockEntity c = tank(h, 2, 2, 2, 2, 26F);
        TankData t = c.getTank();
        check(!t.save().contains("frog_spawn") && !t.save().contains("tadpoles"), "no keys without brood");
        t.frogSpawn().add(new FrogSpawn(t.nextFrogBroodId(), t.min().getX() + 1.0, 3.5, t.min().getZ() + 1.0, 17));
        tadpoles(c, 3);
        t.setTadpoleFood(4);
        TankData l = TankData.load(t.save());
        check(l.frogSpawn().size() == 1 && l.frogSpawn().getFirst().count == 17 && l.tadpoles().size() == 3 && Math.abs(l.tadpoleFood() - 4) < 1e-9,
                "brood saved and loaded");
        TankData view = TankData.loadClientView(t.saveClientView());
        check(view.frogSpawn().size() == 1 && view.tadpoles().size() == 3, "the client sees the brood");
        h.succeed();
    }

    // ------------------------------------------------------------------ part b: the ram forms

    /**
     * Rams in a tank: a Black male and a German Blue female (the textures of their forms), their young are Black and carry
     * German Blue; a ram saved with the genes of an older world (Gold / Balloon) loads unchanged.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void ramFormsCrossAndOldRamsLoad(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 3, 3, 28F);
        TankmateSpecies s = TankmateSpecies.RAM_CICHLID;
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        Vec3 p = w.clamp(w.box().getCenter(), 0.3, 0.1, 0.2);
        TankmateEntity black = TankmateBreeding.spawn(level, c, s, TankmateGenes.pure(s, "black"), false, LifeStage.ADULT, 0, p);
        TankmateEntity blue = TankmateBreeding.spawn(level, c, s, TankmateGenes.pure(s, "german_blue"), true, LifeStage.ADULT, 0, p);
        check(black != null && blue != null, "rams in the tank");
        check(black.variety().equals("black") && blue.variety().equals("german_blue"), "the forms show: " + black.variety() + ", " + blue.variety());
        check(s.texture(black.variety(), false).equals("ram_cichlid_black_male") && s.texture(blue.variety(), true).equals("ram_cichlid_german_blue_female"),
                "the textures of the forms");
        var r = level.getRandom();
        for (int i = 0; i < 40; i++) {
            TankmateGenes y = TankmateGenes.cross(blue.genes(), black.genes(), r::nextLong);
            check(y.phenotype(s).equals("black") && y.carries("german_blue"), "young " + y.text());
        }
        // an old ram (Gold over Balloon)
        TankmateEntity old = TankmateBreeding.spawn(level, c, s, new TankmateGenes("gold", "balloon"), true, LifeStage.ADULT, 2, p);
        CompoundTag tag = new CompoundTag();
        tag.merge(NbtCompat.saveEntityWithoutId(old));
        TankmateEntity loaded = ModEntities.TANKMATES.get(s).get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        org.overengineer.bettacraft.util.NbtCompat.loadEntity(loaded, tag);
        check(loaded.genes().a().equals("gold") && loaded.genes().b().equals("balloon") && loaded.variety().equals("gold") && loaded.generation() == 2,
                "the old ram loads unchanged: " + loaded.genes().text());
        loaded.discard();
        h.succeed();
    }
}
