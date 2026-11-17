package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import net.minecraft.world.InteractionResult;
import static org.overengineer.bettacraft.gametest.ForeignTerrariumGameTests.container;
import static org.overengineer.bettacraft.gametest.ForeignTerrariumGameTests.insert;
import static org.overengineer.bettacraft.gametest.ForeignTerrariumGameTests.interior;
import static org.overengineer.bettacraft.gametest.ForeignTerrariumGameTests.logged;
import static org.overengineer.bettacraft.gametest.ForeignTerrariumGameTests.ofType;
import static org.overengineer.bettacraft.gametest.ForeignTerrariumGameTests.paludarium;
import static org.overengineer.bettacraft.gametest.ForeignTerrariumGameTests.put;
import static org.overengineer.bettacraft.gametest.ForeignTerrariumGameTests.says;
import static org.overengineer.bettacraft.gametest.ForeignTerrariumGameTests.terrarium;
import static org.overengineer.bettacraft.gametest.ForeignTerrariumGameTests.wild;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.big.BigAnimals;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.foreign.ForeignBreeding;
import org.overengineer.bettacraft.foreign.ForeignCare;
import org.overengineer.bettacraft.foreign.ForeignNests;
import org.overengineer.bettacraft.foreign.ForeignProfile;
import org.overengineer.bettacraft.foreign.ForeignProfiles;
import org.overengineer.bettacraft.foreign.ForeignTerrarium;
import org.overengineer.bettacraft.foreign.MinecraftPets;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.terrarium.Climate;
import org.overengineer.bettacraft.terrarium.TerrariumAnimals;

/**
 * M39 part c (optional feature #25, question H3): the peaceful animals of Minecraft as terrarium pets at their real size: into
 * their tanks by the habitat, the lid and the size rule; their needs (grass, flowers, a beehive in the interior) with grazing;
 * chicks from eggs in the sand and young rabbits; no drops, wool or milk; bait; the big ones by the size rule, the refused ones;
 * the switch {@code features.foreignAnimals.vanillaPets} off.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MinecraftPetsGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final String LID = "message.bettacraft.terrarium.needs_lid", WRONG = "message.bettacraft.terrarium.wrong_mode",
            TOO_SMALL = "message.bettacraft.big.too_small";

    private MinecraftPetsGameTests() {}

    // ------------------------------------------------------------------ helpers

    private static ForeignProfile profile(EntityType<?> t) {
        ForeignProfile p = ForeignProfiles.of(t);
        if (p == null) throw new GameTestFailure("no profile for " + BuiltInRegistries.ENTITY_TYPE.getKey(t));
        return p;
    }

    /** A mild climate (22 °C, 55 % humidity), as most Minecraft pets like it. */
    private static void mild(TankBlockEntity c) {
        c.getTank().setClimate(new Climate.State(22, 22, 28, 55, 2, 0));
    }

    /** Inserts a new animal of the type from its box (or tub) and expects the message {@code key} (null: accepted, then taken out). */
    private static void expect(GameTestHelper h, TankBlockEntity c, EntityType<?> type, String key, String what) {
        boolean tub = !profile(type).land().box();
        Component err = insert(h, c, container(h, type, tub, false));
        if (key == null) {
            if (err != null) throw new GameTestFailure(what + " refused: " + err.getString());
            for (LivingEntity e : ofType(h, c, type)) {
                c.getTank().inhabitants().remove(e.getUUID());
                TankInhabitants.release(e);
                e.discard();
            }
        } else if (!says(err, key)) {
            throw new GameTestFailure(what + ": expected " + key + ", got " + (err == null ? "accepted" : err.getString()));
        }
    }

    /** Sets blocks on the interior land of a tank: {@code floor} as the ground of the columns x < xs (null: leave), {@code on} on it. */
    private static void land(GameTestHelper h, TankBlockEntity c, int xs, @javax.annotation.Nullable net.minecraft.world.level.block.state.BlockState floor,
                             @javax.annotation.Nullable net.minecraft.world.level.block.state.BlockState on, int every) {
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        ServerLevel in = interior(h);
        for (int x = 0; x < Math.min(xs, r.sizeX()); x++) {
            for (int z = 0; z < r.sizeZ(); z++) {
                if (floor != null) in.setBlock(new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), floor, 2);
                if (on != null && (x * 31 + z) % every == 0) in.setBlock(new BlockPos(r.originX() + x, InteriorRegion.BASE_Y, r.originZ() + z), on, 2);
            }
        }
        MinecraftPets.rescan(c);
    }

    // ------------------------------------------------------------------ into their tanks

    /**
     * Chickens, rabbits, cats, foxes, armadillos, parrots, bats and bees go into a dry terrarium of 8 x 3 x 6, the fliers (and the
     * fluttering chickens) only with a lid; the frog only into a paludarium (it needs water; no longer on the blacklist); the wolf
     * (1 m) is too big for that terrarium; in its tank our walker holds a chicken.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39c_pets", timeoutTicks = 200)
    public static void minecraftPetsGoIntoTheirTanks(GameTestHelper h) {
        TankBlockEntity dry = terrarium(h, 2, 2, 8, 3, 6);
        mild(dry);
        TankBlockEntity palu = paludarium(h, 12, 2, 8, 3, 6, 0, 0.5, Blocks.SAND.defaultBlockState());
        mild(palu);
        h.assertTrue(TankInhabitants.accepts(EntityType.FROG, null) == TankInhabitants.Acceptance.OK, "the frog is a pet, off the blacklist");
        for (EntityType<?> t : List.of(EntityType.CHICKEN, EntityType.BEE, EntityType.PARROT, EntityType.BAT)) expect(h, dry, t, LID, t.toShortString() + " into an open terrarium");
        for (EntityType<?> t : List.of(EntityType.RABBIT, EntityType.CAT, EntityType.FOX, EntityType.ARMADILLO, EntityType.OCELOT)) {
            expect(h, dry, t, null, t.toShortString() + " into a dry terrarium");
        }
        expect(h, dry, EntityType.FROG, WRONG, "a frog into a dry terrarium");
        expect(h, palu, EntityType.FROG, null, "a frog into a paludarium");
        expect(h, dry, EntityType.WOLF, TOO_SMALL, "a wolf (1 m) into 8 x 3 x 6");
        ForeignLandAnimalsGameTests.lid(h, dry);
        for (EntityType<?> t : List.of(EntityType.BEE, EntityType.PARROT, EntityType.BAT)) expect(h, dry, t, null, t.toShortString() + " with a lid");
        LivingEntity chicken = put(h, dry, container(h, EntityType.CHICKEN, false, false));
        h.assertTrue(TerrariumAnimals.isTerrariumAnimal(EntityType.CHICKEN), "a terrarium animal by its profile");
        h.runAfterDelay(30, () -> {
            h.assertTrue(ForeignTerrarium.held(chicken), "our walker holds the chicken");
            h.assertTrue(TankInhabitants.tankBox(dry.getTank()).inflate(0.1).contains(chicken.position()), "in its tank");
            MinecraftPets.step(h.getLevel(), dry, 0.01);
            h.assertTrue(MinecraftPets.marked(chicken), "marked as a pet");
            h.assertTrue(((Chicken) chicken).eggTime >= MinecraftPets.NO_EGG_TICKS, "no eggs laid in a tank");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ needs

    /**
     * Rabbits need grass, bees flowers and a beehive in the interior: missing ones are problems (log) and stop the breeding; with
     * a grass floor, a few flowers and a beehive (a mini hive in the interior) nothing is missing and the grazers are fed by them.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39c_pets", timeoutTicks = 200)
    public static void petNeedsGrassFlowersAndAHive(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = terrarium(h, 2, 2, 8, 3, 6);
        mild(c);
        ForeignLandAnimalsGameTests.lid(h, c);
        LivingEntity a = put(h, c, container(h, EntityType.RABBIT, false, false));
        put(h, c, container(h, EntityType.RABBIT, false, false));
        LivingEntity bee = put(h, c, container(h, EntityType.BEE, false, false));
        ForeignProfile rabbit = profile(EntityType.RABBIT), bees = profile(EntityType.BEE);
        h.assertTrue(MinecraftPets.missing(level, c, rabbit.land()).equals(List.of(ForeignProfile.Need.GRASS)), "rabbits miss grass on sand");
        h.assertTrue(MinecraftPets.missing(level, c, bees.land()).equals(List.of(ForeignProfile.Need.FLOWERS, ForeignProfile.Need.HIVE)), "bees miss flowers and a hive");
        h.assertTrue("no_grass".equals(ForeignBreeding.blocked(level, c, rabbit)), "no grass: no young");
        h.assertTrue("no_flowers".equals(ForeignBreeding.blocked(level, c, bees)), "no flowers: no young");
        MinecraftPets.step(level, c, 0.01);
        h.assertTrue(logged(c.getTank(), "log.bettacraft.terrarium.problem"), "the missing needs logged");
        // hungry, then the grass: grazing feeds them
        ForeignCare.advance(level, c, a, rabbit, 0.6);
        ForeignCare.advance(level, c, bee, bees, 0.6);
        h.assertTrue(ForeignCare.health(a).daysSinceFed() >= 0.5, "the rabbit got hungry");
        land(h, c, 1000, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DANDELION.defaultBlockState(), 7);
        MinecraftPets.step(level, c, 0.01);
        h.assertTrue(MinecraftPets.missing(level, c, rabbit.land()).isEmpty(), "grass on the land");
        h.assertTrue(ForeignCare.health(a).daysSinceFed() < 0.1, "the rabbit grazed");
        h.assertTrue(ForeignCare.health(bee).daysSinceFed() < 0.1, "the bee found nectar");
        h.assertTrue(MinecraftPets.missing(level, c, bees.land()).equals(List.of(ForeignProfile.Need.HIVE)), "still no hive");
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        interior(h).setBlock(new BlockPos(r.originX() + r.sizeX() - 3, InteriorRegion.BASE_Y, r.originZ() + 3), Blocks.BEEHIVE.defaultBlockState(), 2);
        MinecraftPets.rescan(c);
        h.assertTrue(MinecraftPets.missing(level, c, bees.land()).isEmpty(), "a beehive in the interior");
        h.assertTrue(ForeignBreeding.blocked(level, c, rabbit) == null && ForeignBreeding.blocked(level, c, bees) == null, "now they may breed");
        h.succeed();
    }

    // ------------------------------------------------------------------ breeding

    /**
     * A hen and a rooster: the hen lays her eggs into the sand (our egg block in the interior), chicks hatch at a warm nest and
     * count as pets; a doe and a buck on grass give birth to young rabbits.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39c_pets", timeoutTicks = 200)
    public static void chicksFromEggsAndYoungRabbits(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity coop = terrarium(h, 2, 2, 8, 3, 6); // 30 °C: a warm nest
        ForeignLandAnimalsGameTests.lid(h, coop);
        LivingEntity hen = put(h, coop, container(h, EntityType.CHICKEN, false, false)), rooster = put(h, coop, container(h, EntityType.CHICKEN, false, false));
        ForeignBreeding.setFemale(hen, true);
        ForeignBreeding.setFemale(rooster, false);
        LivingEntity mother = ForeignTerrariumGameTests.layEggs(h, coop, EntityType.CHICKEN);
        h.assertTrue(mother == hen, "the hen lays");
        BlockPos egg = ForeignNests.eggBlockOf(mother);
        h.assertTrue(egg != null && interior(h).getBlockState(egg).is(ModBlocks.TURTLE_EGGS.get()), "the eggs in the sand of the interior");
        int before = ofType(h, coop, EntityType.CHICKEN).size();
        ForeignBreeding.step(level, coop, 6.0);
        List<LivingEntity> chicks = ofType(h, coop, EntityType.CHICKEN).stream().filter(LivingEntity::isBaby).toList();
        h.assertTrue(chicks.size() >= 2 && ofType(h, coop, EntityType.CHICKEN).size() == before + chicks.size(), "chicks hatched: " + chicks.size());
        MinecraftPets.step(level, coop, 0.01);
        for (LivingEntity k : chicks) h.assertTrue(MinecraftPets.marked(k), "a chick is a pet too (no drops)");

        TankBlockEntity hutch = terrarium(h, 2, 12, 8, 3, 6);
        mild(hutch);
        land(h, hutch, 1000, Blocks.GRASS_BLOCK.defaultBlockState(), null, 1);
        LivingEntity doe = put(h, hutch, container(h, EntityType.RABBIT, false, false)), buck = put(h, hutch, container(h, EntityType.RABBIT, false, false));
        ForeignBreeding.setFemale(doe, true);
        ForeignBreeding.setFemale(buck, false);
        int rabbits = ofType(h, hutch, EntityType.RABBIT).size();
        for (int i = 0; i < 160 && ofType(h, hutch, EntityType.RABBIT).size() == rabbits; i++) {
            ForeignCare.feed(ofType(h, hutch, EntityType.RABBIT), 9, null);
            ForeignBreeding.step(level, hutch, 0.5);
        }
        h.assertTrue(ofType(h, hutch, EntityType.RABBIT).size() > rabbits, "young rabbits are born");
        h.assertTrue(ofType(h, hutch, EntityType.RABBIT).stream().anyMatch(LivingEntity::isBaby), "as babies");
        h.succeed();
    }

    // ------------------------------------------------------------------ no farm

    /**
     * A chicken that dies in its tank drops nothing and gives no experience; a marked sheep keeps its wool against shears and a
     * marked cow gives no milk (the bucket stays empty).
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39c_pets", timeoutTicks = 200)
    public static void petsAreNoFarm(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = terrarium(h, 2, 2, 8, 3, 6);
        mild(c);
        ForeignLandAnimalsGameTests.lid(h, c);
        LivingEntity chicken = put(h, c, container(h, EntityType.CHICKEN, false, false));
        MinecraftPets.step(level, c, 0.01);
        AABB around = TankInhabitants.tankBox(c.getTank()).inflate(3);
        chicken.hurt(level.damageSources().playerAttack(MockPlayers.serverPlayer(h)), 100F);
        h.assertFalse(chicken.isAlive(), "it died");
        h.assertTrue(level.getEntitiesOfClass(ItemEntity.class, around).isEmpty(), "no feathers, no meat");
        h.assertTrue(level.getEntitiesOfClass(ExperienceOrb.class, around).isEmpty(), "no experience");
        ServerPlayer player = MockPlayers.serverPlayer(h);
        Sheep sheep = (Sheep) wild(h, EntityType.SHEEP, new Vec3(20, 2, 26));
        MinecraftPets.mark(sheep);
        player.snapTo(sheep.getX() + 1, sheep.getY(), sheep.getZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        player.interactOn(sheep, InteractionHand.MAIN_HAND);
        h.assertFalse(sheep.isSheared(), "a pet keeps its wool");
        LivingEntity cow = wild(h, EntityType.COW, new Vec3(26, 2, 26));
        MinecraftPets.mark(cow);
        player.snapTo(cow.getX() + 1, cow.getY(), cow.getZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        player.interactOn(cow, InteractionHand.MAIN_HAND);
        h.assertTrue(player.getMainHandItem().is(Items.BUCKET) && !player.getInventory().contains(new ItemStack(Items.MILK_BUCKET)), "no milk");
        sheep.discard();
        cow.discard();
        player.discard();
        h.succeed();
    }

    // ------------------------------------------------------------------ big ones and refusals

    /**
     * The big pets by the size rule at their real length: a cow (2 m) not into 8 x 3 x 6 (the minimum named) but into a dry
     * terrarium of 24 x 6 x 12 from a tub, the box does not take it; a pig (1.2 m) goes by the tub too; the camel and the sniffer
     * are refused as too big, the strider and the undead horses as unsuitable.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39c_pets", timeoutTicks = 200)
    public static void bigPetsByTheSizeRuleAndRefusals(GameTestHelper h) {
        h.assertTrue(Math.abs(BigAnimals.length(EntityType.COW, null) - 2.0) < 1e-6, "a cow is 2 m long");
        h.assertTrue(TankInhabitants.accepts(EntityType.COW, null) == TankInhabitants.Acceptance.BIG, "by the size rule");
        h.assertFalse(profile(EntityType.PIG).land().box() || profile(EntityType.COW).land().box(), "the large net and the tub");
        TankBlockEntity small = terrarium(h, 2, 2, 8, 3, 6);
        mild(small);
        expect(h, small, EntityType.COW, TOO_SMALL, "a cow into 8 x 3 x 6");
        TankBlockEntity big = terrarium(h, 2, 12, 24, 6, 12);
        mild(big);
        land(h, big, 1000, Blocks.GRASS_BLOCK.defaultBlockState(), null, 1);
        expect(h, big, EntityType.COW, null, "a cow into 24 x 6 x 12");
        expect(h, big, EntityType.PIG, null, "a pig into 24 x 6 x 12");
        ServerPlayer player = MockPlayers.serverPlayer(h);
        LivingEntity cow = wild(h, EntityType.COW, new Vec3(28, 2, 4));
        h.assertTrue(says(ForeignTerrarium.boxRefusal(player, cow), "message.bettacraft.box.too_big"), "the box does not take a cow");
        cow.discard();
        player.discard();
        for (EntityType<?> t : List.of(EntityType.CAMEL, EntityType.SNIFFER)) h.assertTrue("too_big".equals(ForeignProfiles.refusal(t)), t.toShortString() + " too big");
        for (EntityType<?> t : List.of(EntityType.STRIDER, EntityType.SKELETON_HORSE, EntityType.ZOMBIE_HORSE)) {
            h.assertTrue("unsuitable".equals(ForeignProfiles.refusal(t)), t.toShortString() + " unsuitable");
        }
        Component camel = TankInhabitants.insert(h.getLevel(), big, container(h, EntityType.CAMEL, true, false), EntityType.CAMEL, null, 0);
        h.assertTrue(says(camel, "message.bettacraft.tank.refused.too_big"), "a camel is refused with its reason");
        h.succeed();
    }

    // ------------------------------------------------------------------ bait

    /** Carrots by hand lure a rabbit beside an open terrarium into it (one bait, one animal); a lid keeps the bait off. */
    @GameTest(template = EMPTY, batch = "bettacraft_m39c_pets", timeoutTicks = 200)
    public static void baitLuresARabbitIntoAnOpenTerrarium(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = terrarium(h, 2, 2, 6, 2, 4);
        mild(c);
        MinecraftPets.clear();
        h.assertTrue(MinecraftPets.lures(new ItemStack(Items.CARROT)), "carrots lure rabbits");
        h.assertFalse(MinecraftPets.lures(new ItemStack(Items.COBBLESTONE)), "stone lures nobody");
        BlockPos top = c.getTank().openTops().stream().min(java.util.Comparator.comparingInt((BlockPos b) -> b.getX()).thenComparingInt(b -> b.getZ())).orElseThrow();
        LivingEntity rabbit = wild(h, EntityType.RABBIT, h.relativeVec(Vec3.atBottomCenterOf(top)).add(-1.0, -1.0, 0));
        rabbit.setNoGravity(true);
        MinecraftPets.layBait(level, c, new ItemStack(Items.CARROT));
        var watcher = QuestAdv.watcher(h, c); // M45
        MinecraftPets.lure(level, c);
        h.assertFalse(rabbit.isAlive(), "the rabbit went in");
        QuestAdv.check(h, watcher, "pet_terrarium");
        QuestAdv.gone(h, watcher);
        h.assertTrue(ofType(h, c, EntityType.RABBIT).size() == 1, "one rabbit in the terrarium");
        h.assertTrue(MinecraftPets.bait(c) == null, "one bait, one animal");
        h.assertTrue(logged(c.getTank(), "log.bettacraft.pet.lured"), "the luring logged");
        ForeignLandAnimalsGameTests.lid(h, c);
        ServerPlayer player = MockPlayers.serverPlayer(h);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CARROT, 4));
        h.assertTrue(MinecraftPets.handBait(player.getMainHandItem(), level, c.getBlockPos(), player, InteractionHand.MAIN_HAND)
                != net.minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND, "a carrot on a closed tank: the lid message");
        h.assertTrue(player.getMainHandItem().getCount() == 4, "the carrot stays in the hand");
        h.assertTrue(MinecraftPets.bait(c) == null, "no bait under a lid");
        player.discard();
        h.succeed();
    }

    // ------------------------------------------------------------------ the switch

    /**
     * {@code features.foreignAnimals.vanillaPets} off: a chicken is no terrarium animal and the box refuses it, the frog is on the
     * blacklist again, the camel has no refusal of ours (the old rules: too big by its hitbox); on again everything is back.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39c_pets_off", timeoutTicks = 100)
    public static void minecraftPetsSwitchOff(GameTestHelper h) {
        boolean saved = Features.own(Feature.VANILLA_PETS);
        TankBlockEntity c = terrarium(h, 2, 2, 8, 3, 6);
        mild(c);
        ForeignLandAnimalsGameTests.lid(h, c);
        ServerPlayer player = MockPlayers.serverPlayer(h);
        LivingEntity chicken = wild(h, EntityType.CHICKEN, new Vec3(20, 2, 20));
        try {
            Features.set(Feature.VANILLA_PETS, false);
            h.assertFalse(TerrariumAnimals.enabled(EntityType.CHICKEN), "its terrarium switch is off");
            h.assertTrue(ForeignProfiles.of(EntityType.CHICKEN) == null && !ForeignProfiles.pet(EntityType.CHICKEN), "no profile");
            h.assertTrue(TankInhabitants.accepts(EntityType.FROG, null) == TankInhabitants.Acceptance.BLACKLISTED, "the frog on the blacklist again");
            h.assertTrue(ForeignProfiles.refusal(EntityType.CAMEL) == null, "no refusal of ours");
            h.assertTrue(says(ForeignTerrarium.boxRefusal(player, chicken), "message.bettacraft.box.refused"), "the box refuses a chicken");
            h.assertTrue(insert(h, c, container(h, EntityType.CHICKEN, false, false)) != null, "a chicken does not go in");
            h.assertFalse(MinecraftPets.lures(new ItemStack(Items.CARROT)), "no bait");
        } finally {
            Features.set(Feature.VANILLA_PETS, saved);
        }
        h.assertTrue(TerrariumAnimals.enabled(EntityType.CHICKEN) && ForeignProfiles.pet(EntityType.CHICKEN), "on again");
        chicken.discard();
        player.discard();
        h.succeed();
    }
}
