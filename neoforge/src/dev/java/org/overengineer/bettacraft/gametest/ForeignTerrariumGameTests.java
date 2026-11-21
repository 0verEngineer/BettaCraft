package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import net.minecraft.world.InteractionResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.platform.item.ItemStackHandler;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.big.BigAnimals;
import org.overengineer.bettacraft.big.TransportTubItem;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.foreign.ForeignBreeding;
import org.overengineer.bettacraft.foreign.ForeignCare;
import org.overengineer.bettacraft.foreign.ForeignFood;
import org.overengineer.bettacraft.foreign.ForeignNests;
import org.overengineer.bettacraft.foreign.ForeignProfile;
import org.overengineer.bettacraft.foreign.ForeignProfiles;
import org.overengineer.bettacraft.foreign.ForeignTerrarium;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.terrarium.Climate;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TerrariumAnimals;
import org.overengineer.bettacraft.terrarium.TerrariumBoxItem;
import org.overengineer.bettacraft.terrarium.TerrariumMode;

/**
 * M35 part a: land and marsh animals of other mods in terraria and paludaria, by the real Naturalist (on the dev runtime unless
 * {@code -PnoCompat}; without it the shipped profiles stand in on vanilla animals: the armadillo for the tortoise, the camel for
 * the alligator). The tortoise only into a dry terrarium big enough for it (the size rule over the land), fed greens by hand
 * and from the feeder, eggs into the sand that hatch by the nest temperature; the alligator only into a big paludarium (the
 * whole tank), eats the small fish, takes raw meat from the feeder; the terrarium box catches animals with a profile and
 * refuses others; the vanilla sea turtle grown and as a hatchling only into a big sea water paludarium with a beach, where
 * a pair lays its eggs. The climate is set by hand within one tick (deterministic).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForeignTerrariumGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private ForeignTerrariumGameTests() {}

    // ------------------------------------------------------------------ helpers

    /** The animal of a shipped profile: the Naturalist one, or (without Naturalist) a vanilla stand-in with the same profile. */
    record Animal(EntityType<?> type, ForeignProfile profile, boolean real) {}

    static Animal animal(String file, String naturalistId, EntityType<?> standIn) {
        ForeignProfile p = ForeignProfiles.files().get(BettaCraft.id(file));
        if (p == null) throw new GameTestFailure("no shipped profile " + file);
        if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded(Identifier.parse(naturalistId).getNamespace())) {
            EntityType<?> t = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(naturalistId)).orElseThrow();
            if (ForeignProfiles.of(t) != p) throw new GameTestFailure(naturalistId + " is not mapped to its profile");
            return new Animal(t, p, true);
        }
        return new Animal(standIn, new ForeignProfile(List.of(BuiltInRegistries.ENTITY_TYPE.getKey(standIn)), false, p.group(), p.species(), p.tempMin(),
                p.tempMax(), p.minQuality(), p.salinity(), p.chemistry(), p.labyrinth(), p.hungerDays(), p.stock(), p.food(), p.breeding(), p.suppressOwn(),
                p.terrarium(), p.land()), false);
    }

    /** Puts the stand-in profiles in place (nothing to do with Naturalist); the returned runnable restores the profiles. */
    static Runnable install(List<Animal> animals) {
        Map<Identifier, ForeignProfile> saved = ForeignProfiles.files();
        if (animals.stream().allMatch(Animal::real)) return () -> {};
        Map<Identifier, ForeignProfile> files = new LinkedHashMap<>(saved);
        int i = 0;
        for (Animal a : animals) if (!a.real()) files.put(BettaCraft.id("gametest_stand_in_" + i++), a.profile());
        ForeignProfiles.set(files);
        return () -> ForeignProfiles.set(saved);
    }

    /** Runs the test with the stand-in profiles in place (nothing to do with Naturalist), restoring the profiles afterwards. */
    static void with(List<Animal> animals, Runnable body) {
        Map<Identifier, ForeignProfile> saved = ForeignProfiles.files();
        boolean stand = animals.stream().anyMatch(a -> !a.real());
        if (stand) {
            Map<Identifier, ForeignProfile> files = new LinkedHashMap<>(saved);
            int i = 0;
            for (Animal a : animals) if (!a.real()) files.put(BettaCraft.id("gametest_stand_in_" + i++), a.profile());
            ForeignProfiles.set(files);
        }
        try {
            body.run();
        } finally {
            if (stand) ForeignProfiles.set(saved);
        }
    }

    private static Animal tortoise() {
        return animal("naturalist_tortoise", "naturalist:tortoise", EntityTypes.ARMADILLO);
    }

    private static Animal alligator() {
        return animal("naturalist_alligator", "naturalist:alligator", EntityTypes.CAMEL);
    }

    static ServerLevel interior(GameTestHelper h) {
        ServerLevel in = InteriorManager.interiorLevel(h.getLevel().getServer());
        if (in == null) throw new GameTestFailure("no interior dimension");
        return in;
    }

    /** A tank with a sand floor in its interior. */
    static InteriorRegion sandFloor(GameTestHelper h, TankBlockEntity c) {
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        ServerLevel in = interior(h);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        }
        return r;
    }

    /** A dry terrarium of sx * sy * sz with a sand floor and a warm climate with a basking spot and UV. */
    static TankBlockEntity terrarium(GameTestHelper h, int x, int z, int sx, int sy, int sz) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 2, z, sx, sy, sz, 0);
        sandFloor(h, c);
        TankClimate.step(h.getLevel(), c, 0.01);
        if (c.getTank().terrariumMode() != TerrariumMode.TERRARIUM) throw new GameTestFailure("no terrarium: " + c.getTank().terrariumMode());
        c.getTank().setClimate(new Climate.State(30, 31, 42, 50, 5, 0));
        return c;
    }

    /**
     * A paludarium of sx * sy * sz: water one third of its height (salt {@code ppt}), land of {@code top} over the interior
     * columns x < {@code landShare} of the width (one layer just above the water line), the climate warm with a basking spot.
     */
    static TankBlockEntity paludarium(GameTestHelper h, int x, int z, int sx, int sy, int sz, float ppt, double landShare, BlockState top) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 2, z, sx, sy, sz, 0);
        TankData t = c.getTank();
        InteriorRegion r = sandFloor(h, c);
        long water = t.capacityMb() / 3;
        TankInteractions.changeWater(h.getLevel(), c, water, ppt);
        ServerLevel in = interior(h);
        int y = r.sizeY() / 3 + 2;
        int x1 = (int) Math.round(r.sizeX() * landShare);
        for (int lx = 0; lx < x1; lx++) for (int lz = 0; lz < r.sizeZ(); lz++) in.setBlock(r.toWorld(lx, y, lz), top, 2);
        TankClimate.step(h.getLevel(), c, 0.01);
        if (t.terrariumMode() != TerrariumMode.PALUDARIUM) throw new GameTestFailure("no paludarium: " + t.terrariumMode() + " land " + t.landShare());
        t.setClimate(new Climate.State(30, 31, 42, 70, 5, 0));
        t.setWaterTemp(27F);
        t.setTargetTemp(27F);
        t.setWaterQuality(1F);
        return c;
    }

    /** A live animal of this type in the world at a spot of the test (not in a tank). */
    static LivingEntity wild(GameTestHelper h, EntityType<?> type, Vec3 rel) {
        Entity e = type.create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (!(e instanceof LivingEntity le)) throw new GameTestFailure("no " + type);
        Vec3 a = h.absoluteVec(rel);
        e.snapTo(a.x, a.y, a.z, 0, 0);
        if (e instanceof Mob m) m.setPersistenceRequired();
        if (!h.getLevel().addFreshEntity(e)) throw new GameTestFailure("not added");
        return le;
    }

    /** A container (filled terrarium box or transport tub) holding a new animal of this type (a baby if asked). */
    static ItemStack container(GameTestHelper h, EntityType<?> type, boolean tub, boolean baby) {
        Entity e = type.create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (e == null) throw new GameTestFailure("no " + type);
        if (baby && e instanceof Mob m) m.setBaby(true);
        ItemStack s = tub ? TransportTubItem.fillWith(e) : TerrariumBoxItem.fillWith(e);
        e.discard();
        return s;
    }

    static Component insert(GameTestHelper h, TankBlockEntity c, ItemStack stack) {
        EntityType<?> type = TankInhabitants.containedType(stack);
        if (type == null) throw new GameTestFailure("no animal in " + stack);
        return TankInhabitants.insert(h.getLevel(), c, stack, type, null, TankInhabitants.carriedWater(stack));
    }

    static LivingEntity put(GameTestHelper h, TankBlockEntity c, ItemStack stack) {
        List<Entity> before = TankInhabitants.liveInhabitants(h.getLevel(), c.getTank());
        Component err = insert(h, c, stack);
        if (err != null) throw new GameTestFailure("refused: " + err.getString());
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) if (!before.contains(e) && e instanceof LivingEntity le) return le;
        throw new GameTestFailure("not in the tank");
    }

    static List<LivingEntity> ofType(GameTestHelper h, TankBlockEntity c, EntityType<?> type) {
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) if (e.getType() == type && e instanceof LivingEntity le) out.add(le);
        return out;
    }

    static boolean says(Component c, String key) {
        return BigTankGameTests.says(c, key);
    }

    static boolean logged(TankData t, String key) {
        return t.log().stream().anyMatch(e -> e.key().equals(key));
    }

    static void feed(LivingEntity e) {
        ForeignCare.feed(List.of(e), 1, null);
    }

    /** Mates the fed pair of the tank now (a long step), lays the eggs (a short step) and returns the mother. */
    static LivingEntity layEggs(GameTestHelper h, TankBlockEntity c, EntityType<?> type) {
        ServerLevel level = h.getLevel();
        for (LivingEntity e : ofType(h, c, type)) feed(e);
        // M54: the mating is a draw of the level's random (shared by every running test): 30 days at the lizard's rate leave a
        // chance of 1 in 1800 that no pair mated ("no rabbit carries eggs", seen once); a fixed seed makes it the same every run
        level.getRandom().setSeed(54_000L + BuiltInRegistries.ENTITY_TYPE.getKey(type).hashCode());
        ForeignBreeding.step(level, c, 30.0);
        LivingEntity mother = null;
        for (LivingEntity e : ofType(h, c, type)) if (org.overengineer.bettacraft.platform.Platform.get().persistentData(e).contains(ForeignBreeding.BROOD)) mother = e;
        if (mother == null) throw new GameTestFailure("no " + type.getDescriptionId() + " carries eggs");
        if (!logged(c.getTank(), "log.bettacraft.foreign.gravid")) throw new GameTestFailure("not logged as carrying eggs");
        ForeignBreeding.step(level, c, ForeignNests.WALK_DAYS + 0.01);
        if (ForeignNests.nestOf(mother) == null) throw new GameTestFailure("no nest laid: " + c.getTank().log());
        return mother;
    }

    // ------------------------------------------------------------------ tortoise

    /**
     * The tortoise (1 m) goes into a dry terrarium of 12 x 3 x 6 with land enough (from its box, no water), not into a small
     * terrarium (the size rule names the minimum) nor a paludarium; the walker holds it there (its own AI off).
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m35_tortoise_walk", timeoutTicks = 200)
    public static void tortoiseGoesIntoABigDryTerrarium(GameTestHelper h) {
        Animal a = tortoise();
        Runnable restore = install(List.of(a)); // the stand-in profile stays until the delayed check
        try {
            ServerLevel level = h.getLevel();
            h.assertTrue(BigAnimals.length(a.type(), null) == 1.0, "1 m for the size rule");
            h.assertTrue(TerrariumAnimals.isTerrariumAnimal(a.type()), "a terrarium animal by its profile");
            TankBlockEntity small = terrarium(h, 2, 20, 3, 3, 3);
            Component no = insert(h, small, container(h, a.type(), false, false));
            h.assertTrue(says(no, "message.bettacraft.big.too_small"), "a small terrarium names the minimum: " + (no == null ? "-" : no.getString()));
            h.assertTrue(no.getString().contains("12") && no.getString().contains("6"), "12 x 6 blocks: " + no.getString());
            TankBlockEntity palu = paludarium(h, 16, 2, 12, 3, 6, 0, 0.5, Blocks.SAND.defaultBlockState());
            h.assertTrue(says(insert(h, palu, container(h, a.type(), false, false)), "message.bettacraft.terrarium.wrong_mode"), "a paludarium refuses it");
            TankBlockEntity dry = terrarium(h, 2, 2, 12, 3, 6);
            ItemStack box = container(h, a.type(), false, false);
            h.assertTrue(TankInhabitants.carriedWater(box) == 0, "the box brings no water");
            LivingEntity t = put(h, dry, box);
            h.assertTrue(dry.getTank().waterMb() == 0, "still dry");
            h.assertTrue(ForeignCare.menuValues(t) != null, "our care");
            h.runAfterDelay(30, () -> {
                try {
                h.assertTrue(ForeignTerrarium.held(t), "the walker holds it in its tank");
                h.assertTrue(TankInhabitants.tankBox(dry.getTank()).contains(t.position()), "it stays in the tank");
                h.succeed();
                } finally {
                    restore.run();
                }
            });
        } catch (RuntimeException ex) {
            restore.run();
            throw ex;
        }
    }

    /** Greens by hand on the tank and from the feeder (the feeder waits until it is hungry); a cow's wheat is no food for it. */
    @GameTest(template = EMPTY, batch = "bettacraft_m35_foreign_terrarium", timeoutTicks = 200)
    public static void tortoiseEatsGreensByHandAndFromTheFeeder(GameTestHelper h) {
        Animal a = tortoise();
        with(List.of(a), () -> {
            ServerLevel level = h.getLevel();
            TankBlockEntity c = terrarium(h, 2, 2, 12, 3, 6);
            LivingEntity t = put(h, c, container(h, a.type(), false, false));
            h.assertTrue(ForeignFood.isFood(new ItemStack(Items.DANDELION)), "the feeder takes dandelions");
            h.assertFalse(ForeignFood.isFood(new ItemStack(Items.STICK)), "but not sticks"); // M39 part b: wheat is food of the rat, zebra ...
            ForeignProfile p = ForeignProfiles.of(t.getType());
            ForeignCare.advance(level, c, t, p, 1.2);
            h.assertTrue(ForeignCare.health(t).daysSinceFed() >= 1.2 - 1e-6, "hungry now");
            ServerPlayer player = MockPlayers.serverPlayer(h);
            ItemStack greens = new ItemStack(Items.DANDELION, 4);
            player.setItemInHand(InteractionHand.MAIN_HAND, greens);
            var r = ForeignFood.handFeed(greens, level, c.getBlockPos(), player, InteractionHand.MAIN_HAND);
            h.assertTrue(r == net.minecraft.world.InteractionResult.CONSUME, "it eats from the hand");
            h.assertTrue(ForeignCare.health(t).daysSinceFed() < 0.01, "fed by hand");
            player.discard();
            ItemStackHandler feeder = new ItemStackHandler(3);
            feeder.setStackInSlot(0, new ItemStack(Items.WHEAT, 4));
            feeder.setStackInSlot(1, new ItemStack(Items.SHORT_GRASS, 4));
            h.assertTrue(ForeignFood.feedFromFeeder(level, c, feeder) == 0, "just fed: the feeder waits");
            ForeignCare.advance(level, c, t, p, 1.0);
            h.assertTrue(ForeignFood.feedFromFeeder(level, c, feeder) == 1, "hungry: one portion of grass");
            h.assertTrue(feeder.getStackInSlot(1).getCount() == 3 && feeder.getStackInSlot(0).getCount() == 4, "grass, not wheat");
            h.assertTrue(ForeignCare.health(t).daysSinceFed() < 0.01, "fed from the feeder");
            h.succeed();
        });
    }

    /** A fed pair: the mother lays her eggs into the sand (our egg block in the interior); at a warm nest they hatch as babies. */
    @GameTest(template = EMPTY, batch = "bettacraft_m35_foreign_terrarium", timeoutTicks = 200)
    public static void tortoisesLayEggsInTheSand(GameTestHelper h) {
        Animal a = tortoise();
        with(List.of(a), () -> {
            ServerLevel level = h.getLevel();
            TankBlockEntity c = terrarium(h, 2, 2, 12, 3, 6);
            put(h, c, container(h, a.type(), false, false));
            put(h, c, container(h, a.type(), false, false));
            LivingEntity mother = layEggs(h, c, a.type());
            BlockPos egg = ForeignNests.eggBlockOf(mother);
            h.assertTrue(egg != null && interior(h).getBlockState(egg).is(ModBlocks.TURTLE_EGGS.get()), "the eggs in the interior");
            h.assertTrue(logged(c.getTank(), "log.bettacraft.turtle.laid"), "the laying logged");
            // cold: the eggs wait
            c.getTank().setClimate(new Climate.State(16, 16, 28, 50, 5, 0));
            ForeignBreeding.step(level, c, 3.0);
            h.assertTrue(ForeignNests.nestOf(mother) != null, "at 16 °C the eggs wait");
            c.getTank().setClimate(new Climate.State(30, 31, 42, 50, 5, 0));
            int before = ofType(h, c, a.type()).size();
            ForeignBreeding.step(level, c, 5.0);
            List<LivingEntity> all = ofType(h, c, a.type());
            long babies = all.stream().filter(LivingEntity::isBaby).count();
            h.assertTrue(babies >= 2 && babies <= 4 && all.size() == before + babies, "2-4 hatchlings, got " + babies);
            h.assertFalse(interior(h).getBlockState(egg).is(ModBlocks.TURTLE_EGGS.get()), "the egg block is gone");
            h.assertTrue(logged(c.getTank(), "log.bettacraft.foreign.hatched"), "the hatching logged");
            h.succeed();
        });
    }

    /**
     * M52: the young of a nest right by the glass hatch inside the tank and stay its inhabitants. They came out up to 0.1 off
     * the nest, past the glass now and then (the nest of the edge column lies 0.06 inside); the next reconcile let them go.
     * A clutch of 20 at a nest by the west wall, the spots drawn from a fixed seed.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m35_foreign_terrarium", timeoutTicks = 200)
    public static void hatchlingsOfANestByTheGlassStayInside(GameTestHelper h) {
        Animal a = tortoise();
        with(List.of(a), () -> {
            ServerLevel level = h.getLevel();
            TankBlockEntity c = terrarium(h, 2, 2, 12, 3, 6);
            TankData t = c.getTank();
            put(h, c, container(h, a.type(), false, false));
            put(h, c, container(h, a.type(), false, false));
            LivingEntity mother = layEggs(h, c, a.type());
            var data = org.overengineer.bettacraft.platform.Platform.get().persistentData(mother);
            var brood = data.getCompoundOrEmpty(ForeignBreeding.BROOD);
            var nest = brood.getCompoundOrEmpty("nest");
            nest.putDouble("x", t.min().getX() + 0.06);
            nest.putInt("eggs", 20);
            brood.put("nest", nest);
            data.put(ForeignBreeding.BROOD, brood);
            int before = ofType(h, c, a.type()).size();
            level.getRandom().setSeed(52_400L);
            ForeignBreeding.step(level, c, 5.0);
            List<LivingEntity> young = ofType(h, c, a.type()).stream().filter(LivingEntity::isBaby).toList();
            h.assertTrue(young.size() == 20 && ofType(h, c, a.type()).size() == before + 20, "20 hatchlings, got " + young.size());
            for (LivingEntity y : young) h.assertTrue(TankInhabitants.inside(t, y), "a hatchling past the glass at " + y.position() + " (tank from x " + t.min().getX() + ")");
            TankInhabitants.reconcile(level, c);
            for (LivingEntity y : young) {
                h.assertTrue(TankInhabitants.hasTankRef(y) && t.inhabitants().contains(y.getUUID()), "a hatchling let go at " + y.position());
            }
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ alligator

    /**
     * The alligator (2 m) only into a big paludarium (24 x 6 x 12, the whole tank counts): a small paludarium names the
     * minimum, a terrarium refuses it; in its tank it eats the neons, and takes raw meat from the feeder when hungry.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m35_alligator", timeoutTicks = 300)
    public static void alligatorInABigPaludariumHuntsAndTakesMeat(GameTestHelper h) {
        Animal a = alligator();
        Runnable restore = install(List.of(a)); // the stand-in profile stays until the delayed check
        try {
            ServerLevel level = h.getLevel();
            TankBlockEntity small = paludarium(h, 2, 20, 6, 3, 6, 0, 0.5, Blocks.SAND.defaultBlockState());
            Component no = insert(h, small, container(h, a.type(), true, false));
            h.assertTrue(says(no, "message.bettacraft.big.too_small"), "a small paludarium names the minimum: " + (no == null ? "-" : no.getString()));
            h.assertTrue(no.getString().contains("24"), "24 blocks long: " + no.getString());
            TankBlockEntity dry = terrarium(h, 10, 20, 3, 3, 3);
            h.assertTrue(says(insert(h, dry, container(h, a.type(), true, false)), "message.bettacraft.terrarium.wrong_mode"), "a terrarium refuses it");
            TankBlockEntity c = paludarium(h, 2, 2, 24, 6, 12, 0, 0.5, Blocks.SAND.defaultBlockState());
            LivingEntity g = put(h, c, container(h, a.type(), true, false));
            for (int i = 0; i < 3; i++) AerationGameTests.add(h, c, TankmateSpecies.NEON_TETRA, LifeStage.ADULT);
            ForeignProfile p = ForeignProfiles.of(g.getType());
            ForeignCare.advance(level, c, g, p, 2.0);
            ForeignTerrarium.hunt(level, c, 30.0);
            h.assertTrue(ofType(h, c, org.overengineer.bettacraft.registry.ModEntities.TANKMATES.get(TankmateSpecies.NEON_TETRA).get()).isEmpty(),
                    "the neons were eaten");
            h.assertTrue(ForeignCare.health(g).daysSinceFed() < 0.01, "a meal");
            h.assertTrue(logged(c.getTank(), "log.bettacraft.eaten"), "logged");
            ForeignCare.advance(level, c, g, p, 2.0);
            ItemStackHandler feeder = new ItemStackHandler(2);
            feeder.setStackInSlot(0, new ItemStack(Items.DANDELION, 2));
            feeder.setStackInSlot(1, new ItemStack(Items.BEEF, 2));
            h.assertTrue(ForeignFood.feedFromFeeder(level, c, feeder) == 1 && feeder.getStackInSlot(1).getCount() == 1, "raw beef from the feeder");
            h.assertTrue(ForeignCare.health(g).daysSinceFed() < 0.01, "fed");
            h.runAfterDelay(30, () -> {
                try {
                h.assertTrue(ForeignTerrarium.held(g), "the walker holds it");
                h.succeed();
                } finally {
                    restore.run();
                }
            });
        } catch (RuntimeException ex) {
            restore.run();
            throw ex;
        }
    }

    // ------------------------------------------------------------------ the terrarium box

    /**
     * The empty box on a tortoise in the world catches it (the real click: the event before the animal's own); a cow is
     * refused with a message, the alligator is too big for the box; the caught tortoise goes from the box into a tank.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m35_foreign_terrarium", timeoutTicks = 200)
    public static void terrariumBoxCatchesProfiledAnimalsOnly(GameTestHelper h) {
        Animal tortoise = tortoise(), alligator = alligator();
        with(List.of(tortoise, alligator), () -> {
            ServerPlayer player = MockPlayers.serverPlayer(h);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TERRARIUM_BOX.get(), 4));
            LivingEntity t = wild(h, tortoise.type(), new Vec3(4, 2, 28));
            player.snapTo(t.getX() + 1, t.getY(), t.getZ());
            player.interactOn(t, InteractionHand.MAIN_HAND, net.minecraft.world.phys.Vec3.ZERO);
            h.assertFalse(t.isAlive(), "the tortoise went into the box");
            ItemStack filled = ItemStack.EMPTY;
            for (ItemStack s : player.getInventory().getNonEquipmentItems()) if (TerrariumBoxItem.isFilled(s)) filled = s;
            h.assertTrue(TerrariumBoxItem.containedType(filled) == tortoise.type(), "a box with the tortoise");
            h.assertTrue(filled.getMaxStackSize() == 1, "one animal per box");
            LivingEntity cow = wild(h, EntityTypes.COW, new Vec3(8, 2, 28));
            // M39 part c: the cow is a big Minecraft pet now (the large net and the tub); an allay has no profile
            h.assertTrue(says(ForeignTerrarium.boxRefusal(player, cow), "message.bettacraft.box.too_big"), "a cow is too big for the box");
            LivingEntity allay = wild(h, EntityTypes.ALLAY, new Vec3(10, 2, 28));
            h.assertTrue(says(ForeignTerrarium.boxRefusal(player, allay), "message.bettacraft.box.refused"), "an allay has no profile");
            allay.discard();
            player.interactOn(cow, InteractionHand.MAIN_HAND, net.minecraft.world.phys.Vec3.ZERO);
            h.assertTrue(cow.isAlive(), "the cow stays");
            LivingEntity g = wild(h, alligator.type(), new Vec3(14, 2, 28));
            h.assertTrue(says(ForeignTerrarium.boxRefusal(player, g), "message.bettacraft.box.too_big"), "the alligator is too big for the box");
            TankBlockEntity dry = terrarium(h, 2, 2, 12, 3, 6);
            put(h, dry, filled.copy());
            h.assertTrue(ofType(h, dry, tortoise.type()).size() == 1, "from the box into the terrarium");
            cow.discard();
            g.discard();
            player.discard();
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ sea turtle

    /**
     * Sea turtles (1 m, over the water part): grown from a tub and a hatchling from a bag go into a sea water paludarium of
     * 12 x 3 x 8 with a sand beach (216 water blocks: room for both); not into fresh water, not without a beach; a hatchling that grows up stays (no bag in
     * front of the tank). A pair lays eggs into the beach; they hatch as baby sea turtles.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m35_foreign_terrarium", timeoutTicks = 200)
    public static void seaTurtlesOnlyInBigSeaWaterPaludaria(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        h.assertTrue(BigAnimals.length(EntityTypes.TURTLE, null) == 1.0, "1 m");
        TankBlockEntity fresh = paludarium(h, 2, 18, 12, 3, 8, 0, 0.25, Blocks.SAND.defaultBlockState());
        h.assertTrue(says(insert(h, fresh, container(h, EntityTypes.TURTLE, true, false)), "message.bettacraft.big.dolphin_salt"), "fresh water refuses it");
        TankBlockEntity stone = paludarium(h, 16, 18, 12, 3, 8, 35, 0.25, Blocks.STONE.defaultBlockState());
        h.assertTrue(says(insert(h, stone, container(h, EntityTypes.TURTLE, true, false)), "message.bettacraft.sea_turtle.no_beach"), "no beach: refused");
        TankBlockEntity c = paludarium(h, 2, 2, 12, 3, 8, 35, 0.25, Blocks.SAND.defaultBlockState());
        var watcher = QuestAdv.watcher(h, c); // M45
        put(h, c, container(h, EntityTypes.TURTLE, true, false));
        QuestAdv.check(h, watcher, "sea_turtle");
        h.assertTrue(!QuestAdv.done(watcher, "big_animal"), "a sea turtle has its own advancement");
        QuestAdv.gone(h, watcher);
        Turtle baby = EntityTypes.TURTLE.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        baby.setAge(-24000);
        baby.snapTo(h.absoluteVec(new Vec3(20, 2, 2)));
        h.assertTrue(TankInhabitants.bagTakes(baby), "a hatchling goes into a bag");
        ItemStack bag = FishBagItem.fillWith(baby);
        baby.discard();
        LivingEntity young = put(h, c, bag);
        h.assertTrue(young.isBaby(), "the hatchling lives in the tank");
        ((Turtle) young).setAge(0);
        TankInhabitants.reconcile(level, c);
        h.assertTrue(young.isAlive() && TankInhabitants.liveInhabitants(level, c.getTank()).contains(young), "grown up, it stays");
        LivingEntity mother = layEggs(h, c, EntityTypes.TURTLE);
        int before = ofType(h, c, EntityTypes.TURTLE).size();
        ForeignBreeding.step(level, c, 5.0);
        long babies = ofType(h, c, EntityTypes.TURTLE).stream().filter(LivingEntity::isBaby).count();
        h.assertTrue(babies >= 2 && ofType(h, c, EntityTypes.TURTLE).size() == before + babies, "hatchlings from the beach: " + babies);
        h.assertTrue(ForeignNests.nestOf(mother) == null, "the nest is done");
        h.succeed();
    }
}
