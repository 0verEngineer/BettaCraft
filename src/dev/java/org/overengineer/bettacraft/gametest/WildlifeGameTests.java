package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.containers.AnimalContainers;
import org.overengineer.bettacraft.containers.ContainerRules;
import org.overengineer.bettacraft.containers.CritterBucketItem;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.critter.WildCritterEntity;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.genetics.GeneVisibility;
import org.overengineer.bettacraft.genetics.MagnifierItem;
import org.overengineer.bettacraft.plants.AquaticPlantItem;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.reef.WildCoralBlock;
import org.overengineer.bettacraft.reef.WildCoralFeature;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateBucketItem;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.world.Wildlife;

/**
 * M22.3 GameTests: wild spawns (biome lists, spawn rules: rare, never in a tank), the wild small animals and their
 * lossless way between the world, bags, buckets and tanks, the wild corals of the reefs (feature, shears), the beautiful
 * wild colours and the mutations, the magnifier (one animal, the whole tank, kept through buckets, bags and tank moves,
 * old animals analysed), the single-animal take-out by sex, the capacities of buckets and bags, the water of a bucket,
 * the sea water bucket on fish, and letting animals go only after the confirmation.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WildlifeGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private WildlifeGameTests() {}

    // ------------------------------------------------------------------ helpers

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestAssertException(what);
    }

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        sp.getInventory().clearContent();
        sp.getInventory().selected = 0;
        return sp;
    }

    private static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, long water, float salt) {
        for (int a = 0; a < 3; a++)
            for (int b = 0; b < 2; b++)
                for (int c = 0; c < 3; c++) {
                    BlockPos p = new BlockPos(x + a, y + b, z + c);
                    h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
                }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(x, y, z)));
        if (c == null) throw new GameTestAssertException("tank did not form");
        if (water > 0) TankInteractions.changeWater(h.getLevel(), c, water, salt);
        c.getTank().setWaterTemp(25F);
        c.getTank().setTargetTemp(25F);
        return c;
    }

    private static ItemStack useOn(ServerPlayer sp, GameTestHelper h, BlockPos rel, ItemStack stack) {
        sp.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = h.absolutePos(rel);
        sp.gameMode.useItemOn(sp, sp.serverLevel(), stack, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false));
        return sp.getItemInHand(InteractionHand.MAIN_HAND);
    }

    private static ItemStack use(ServerPlayer sp, ItemStack stack) {
        sp.setItemInHand(InteractionHand.MAIN_HAND, stack);
        sp.gameMode.useItem(sp, sp.serverLevel(), stack, InteractionHand.MAIN_HAND);
        return sp.getItemInHand(InteractionHand.MAIN_HAND);
    }

    private static void stand(GameTestHelper h, ServerPlayer sp, BlockPos rel, float pitch) {
        Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(rel));
        sp.moveTo(at.x, at.y, at.z, 0F, pitch);
        sp.setYHeadRot(0F);
    }

    /** A pool of water {@code depth} deep on stone (glass walls), x0..x0+size-1. */
    private static void pool(GameTestHelper h, int x0, int z0, int size, int depth) {
        for (int x = x0 - 1; x <= x0 + size; x++)
            for (int z = z0 - 1; z <= z0 + size; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.STONE.defaultBlockState());
                for (int y = 2; y < 2 + depth; y++) {
                    boolean wall = x == x0 - 1 || x == x0 + size || z == z0 - 1 || z == z0 + size;
                    h.setBlock(new BlockPos(x, y, z), wall ? Blocks.GLASS.defaultBlockState() : Blocks.WATER.defaultBlockState());
                }
            }
    }

    private static Critter critter(CritterSpecies s, boolean female, CritterGenes g, double age) {
        Critter k = new Critter(0, s, female, g, 0, 0, 0);
        k.age = age;
        return k;
    }

    // ------------------------------------------------------------------ spawns

    /** The biome lists: tankmates in their waters, the frog in swamps, the small animals, all in the water ambient group. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void wildSpawnsInTheirBiomes(GameTestHelper h) {
        var biomes = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        record Want(net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> biome, EntityType<?> type) {}
        for (Want w : List.of(new Want(Biomes.JUNGLE, ModEntities.TANKMATES.get(TankmateSpecies.NEON_TETRA).get()),
                new Want(Biomes.SWAMP, ModEntities.TANKMATES.get(TankmateSpecies.HARLEQUIN_RASBORA).get()),
                new Want(Biomes.SWAMP, ModEntities.DWARF_FROG.get()),
                new Want(Biomes.RIVER, ModEntities.TANKMATES.get(TankmateSpecies.GUPPY).get()),
                new Want(Biomes.WARM_OCEAN, ModEntities.TANKMATES.get(TankmateSpecies.OCELLARIS_CLOWNFISH).get()),
                new Want(Biomes.LUKEWARM_OCEAN, ModEntities.TANKMATES.get(TankmateSpecies.LINED_SEAHORSE).get()),
                new Want(Biomes.RIVER, ModEntities.WILD_CRITTER.get()),
                new Want(Biomes.MANGROVE_SWAMP, ModEntities.WILD_CRITTER.get()),
                new Want(Biomes.WARM_OCEAN, ModEntities.WILD_CRITTER.get()))) {
            var spawns = biomes.getHolderOrThrow(w.biome()).value().getMobSettings().getMobs(MobCategory.WATER_AMBIENT).unwrap();
            check(spawns.stream().anyMatch(d -> d.type == w.type()), w.type() + " does not spawn in " + w.biome().location());
        }
        var plains = biomes.getHolderOrThrow(Biomes.PLAINS).value().getMobSettings().getMobs(MobCategory.WATER_AMBIENT).unwrap();
        check(plains.stream().noneMatch(d -> d.type == ModEntities.WILD_CRITTER.get()), "small animals in the plains");
        var desert = biomes.getHolderOrThrow(Biomes.JUNGLE).value().getMobSettings().getMobs(MobCategory.WATER_AMBIENT).unwrap();
        check(desert.stream().noneMatch(d -> d.type == ModEntities.TANKMATES.get(TankmateSpecies.GUPPY).get() && d.getWeight().asInt() > 99), "weights too high");
        h.succeed();
    }

    /** The spawn rules: water near the surface yes, a tank never, a dry spot never, and no more than the cap nearby. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void wildFishSpawnRulesKeepThemRare(GameTestHelper h) {
        var level = h.getLevel();
        var type = ModEntities.TANKMATES.get(TankmateSpecies.NEON_TETRA).get();
        int sea = level.getSeaLevel();
        // a column of water at sea level for the rule (the test area lies elsewhere in height)
        BlockPos base = new BlockPos(h.absolutePos(new BlockPos(3, 0, 3)).getX(), sea - 3, h.absolutePos(new BlockPos(3, 0, 3)).getZ());
        for (int y = 0; y < 4; y++) level.setBlock(base.above(y), Blocks.WATER.defaultBlockState(), 2);
        level.setBlock(base.below(), Blocks.STONE.defaultBlockState(), 2);
        BlockPos spot = base.above(1);
        try {
            check(Wildlife.checkFish(type, level, MobSpawnType.NATURAL, spot, level.random), "no wild fish in open water");
            check(!Wildlife.checkFish(type, level, MobSpawnType.NATURAL, base.above(10), level.random), "a wild fish in the air");
            check(Wildlife.checkFish(type, level, MobSpawnType.SPAWN_EGG, base.above(10), level.random), "spawn eggs are not limited");
            // the cap: as many wild fish as allowed nearby, then no more
            int cap = org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.WILD_FISH_NEARBY, 6);
            List<TankmateEntity> made = new java.util.ArrayList<>();
            for (int i = 0; i < cap; i++) {
                TankmateEntity e = type.spawn(level, spot, MobSpawnType.NATURAL);
                check(e != null && e.isWild(), "a natural spawn is not wild");
                made.add(e);
            }
            check(!Wildlife.checkFish(type, level, MobSpawnType.NATURAL, spot, level.random), "more wild fish than the cap nearby");
            made.forEach(e -> e.discard());
            // a wild one despawns far away, a caught one never; its genes are not analysed
            TankmateEntity wild = type.spawn(level, spot, MobSpawnType.NATURAL);
            check(wild.removeWhenFarAway(100) && !wild.analysed(), "a wild fish does not despawn or knows its genes");
            ItemStack b = wild.getBucketItemStack();
            wild.saveToBucketTag(b);
            TankmateEntity caught = type.create(level);
            caught.loadFromBucketTag(b.get(DataComponents.BUCKET_ENTITY_DATA).copyTag());
            check(!caught.isWild(), "a caught fish is still wild");
            wild.discard();
            caught.discard();
        } finally {
            for (int y = -1; y < 4; y++) level.setBlock(base.above(y), Blocks.AIR.defaultBlockState(), 2);
        }
        // never in a tank (tank blocks hold water)
        TankBlockEntity c = tank(h, 10, 2, 10, 18000, 0F);
        BlockPos inTank = h.absolutePos(new BlockPos(11, 2, 11));
        check(Wildlife.inTank(level, inTank), "the tank is not seen");
        check(!Wildlife.checkCritter(ModEntities.WILD_CRITTER.get(), level, MobSpawnType.NATURAL, inTank, level.random), "a wild critter in a tank");
        check(c.getTank().waterMb() > 0, "tank");
        h.succeed();
    }

    /** The small animals: a natural spawn is a full animal (adult, wild genes, not analysed) on a solid bottom. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void wildCrittersSpawnOnTheBottom(GameTestHelper h) {
        var level = h.getLevel();
        pool(h, 4, 4, 4, 3);
        BlockPos bottom = h.absolutePos(new BlockPos(5, 2, 5));
        check(Wildlife.checkCritter(ModEntities.WILD_CRITTER.get(), level, MobSpawnType.SPAWN_EGG, bottom, level.random), "not natural: allowed");
        WildCritterEntity e = ModEntities.WILD_CRITTER.get().spawn(level, bottom, MobSpawnType.NATURAL);
        check(e != null && e.isWild() && e.animal().adult() && !e.animal().analysed, "a wild critter is no full animal: " + (e == null ? null : e.saveAnimal()));
        check(e.removeWhenFarAway(100), "a wild critter does not despawn");
        e.setPersistenceRequired(); // the mock players of other tests are far away: it would despawn at once
        // it sinks to the bottom and stays in the water
        h.runAfterDelay(60, () -> {
            check(e.isAlive() && e.isInWater(), "the critter left the water or died");
            check(e.getY() < bottom.getY() + 0.6, "the critter floats: " + e.getY());
            e.discard();
            h.succeed();
        });
    }

    /** One animal goes from the tank to a bucket, into the world, into a bag and into a tank again: nothing is lost. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void crittersConvertLosslessly(GameTestHelper h) {
        var level = h.getLevel();
        ServerPlayer sp = player(h);
        Critter k = critter(CritterSpecies.NEOCARIDINA, true, new CritterGenes(0b101, 0b001, 0.73F), 11.25);
        k.analysed = true;
        k.generation = 4;
        k.look = 0;
        CompoundTag original = k.saveAnimal();
        // into the world and back: the entity carries the same data
        WildCritterEntity e = WildCritterEntity.of(level, original);
        check(e != null && e.saveAnimal().equals(original), "the entity changed the animal: " + (e == null ? null : e.saveAnimal()) + " vs " + original);
        CompoundTag saved = new CompoundTag();
        e.saveWithoutId(saved);
        WildCritterEntity loaded = ModEntities.WILD_CRITTER.get().create(level);
        loaded.load(saved);
        check(loaded.saveAnimal().equals(original), "saving the entity lost data: " + loaded.saveAnimal());
        // caught with a bucket of water in the world: the bucket holds exactly that animal
        pool(h, 4, 4, 4, 3);
        loaded.moveTo(h.absolutePos(new BlockPos(5, 2, 5)), 0, 0);
        level.addFreshEntity(loaded);
        sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        sp.interactOn(loaded, InteractionHand.MAIN_HAND);
        ItemStack bucket = sp.getItemInHand(InteractionHand.MAIN_HAND);
        check(bucket.getItem() instanceof CritterBucketItem && CritterBucketItem.animals(bucket).getFirst().equals(original) && loaded.isRemoved(),
                "the bucket did not take the animal: " + bucket);
        // the next one of the same species goes into the same bucket
        WildCritterEntity second = WildCritterEntity.of(level, critter(CritterSpecies.NEOCARIDINA, false, CritterGenes.WILD, 9).saveAnimal());
        second.moveTo(h.absolutePos(new BlockPos(6, 2, 6)), 0, 0);
        level.addFreshEntity(second);
        sp.interactOn(second, InteractionHand.MAIN_HAND);
        check(CritterBucketItem.animals(sp.getItemInHand(InteractionHand.MAIN_HAND)).size() == 2, "the second shrimp is not in the same bucket");
        // into a tank: the animal keeps everything
        TankBlockEntity c = tank(h, 14, 2, 14, 18000, 0F);
        ItemStack after = useOn(sp, h, new BlockPos(14, 3, 14), sp.getItemInHand(InteractionHand.MAIN_HAND));
        check(after.is(Items.BUCKET), "the bucket did not go into the tank: " + after);
        Critter inTank = c.getTank().critters().stream().filter(x -> x.female).findFirst().orElseThrow(() -> new GameTestAssertException("not in the tank"));
        check(inTank.saveAnimal().equals(original), "the tank changed the animal: " + inTank.saveAnimal() + " vs " + original);
        // out again by id into a fish bag, then into the world (confirmed): a wild entity with the same data
        sp.getInventory().clearContent();
        sp.getInventory().add(new ItemStack(ModItems.FISH_BAG.get()));
        check(TankCritters.takeOutById(level, c, sp, inTank.id, false) == null, "could not take the animal out");
        ItemStack bag = null;
        for (ItemStack s : sp.getInventory().items) if (AnimalContainers.critters(s) != null) bag = s;
        check(bag != null && AnimalContainers.critters(bag).getFirst().equals(original), "the bag changed the animal: " + bag);
        stand(h, sp, new BlockPos(5, 6, 5), 90F);
        ItemStack held = use(sp, bag.copy());
        check(FishBagItem.isFilled(held), "let go without asking");
        ContainerRules.confirm(sp, InteractionHand.MAIN_HAND);
        var free = level.getEntitiesOfClass(WildCritterEntity.class, new AABB(h.absolutePos(new BlockPos(5, 3, 5))).inflate(4));
        check(free.size() == 1 && free.getFirst().saveAnimal().equals(original) && !free.getFirst().isWild(), "the released animal changed: " + free);
        free.forEach(x -> x.discard());
        h.succeed();
    }

    // ------------------------------------------------------------------ colours, mutations

    /** About 3 % of the wild animals show a bred colour openly, the rest are wild-coloured. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void beautifulWildColoursAreRare(GameTestHelper h) {
        Random r = new Random(22);
        int n = 20000, shrimp = 0, plecos = 0;
        for (int i = 0; i < n; i++) {
            if (Wildlife.critter(CritterSpecies.NEOCARIDINA, r).color(CritterSpecies.NEOCARIDINA) != CritterGenes.WILD_COLOR) shrimp++;
            if (!Wildlife.tankmate(TankmateSpecies.BRISTLENOSE_PLECO, r).phenotype(TankmateSpecies.BRISTLENOSE_PLECO).equals("wild")) plecos++;
        }
        check(shrimp > n * 0.02 && shrimp < n * 0.04, "shrimp showing a colour: " + shrimp);
        check(plecos > n * 0.02 && plecos < n * 0.04, "plecos showing a bred variety: " + plecos);
        h.succeed();
    }

    /** With features.breeding.wildColors off no wild animal shows a bred colour (config test: own batch, restored). */
    @GameTest(template = EMPTY, timeoutTicks = 60, batch = "bettacraft_wildlife_config")
    public static void wildColoursCanBeSwitchedOff(GameTestHelper h) {
        boolean before = Features.own(Feature.WILD_COLORS);
        try {
            Features.set(Feature.WILD_COLORS, false);
            Random r = new Random(5);
            for (int i = 0; i < 5000; i++) {
                check(Wildlife.critter(CritterSpecies.NEOCARIDINA, r).color(CritterSpecies.NEOCARIDINA) == CritterGenes.WILD_COLOR, "a coloured wild shrimp");
            }
        } finally {
            Features.set(Feature.WILD_COLORS, before);
        }
        h.succeed();
    }

    /** Spontaneous mutations: wild x wild shrimp now and then give a young with a colour allele; bred tankmates too. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void mutationsHappenRarely(GameTestHelper h) {
        Random r = new Random(9);
        int carriers = 0, n = 20000;
        for (int i = 0; i < n; i++) if (CritterGenes.cross(CritterGenes.WILD, CritterGenes.WILD, 6, r).carried() != 0) carriers++;
        check(carriers > n * 0.02 && carriers < n * 0.055, "young shrimp with a new allele: " + carriers);
        int changed = 0;
        TankmateGenes g = TankmateGenes.wild(TankmateSpecies.PLATY);
        for (int i = 0; i < n; i++) if (!Wildlife.mutate(TankmateSpecies.PLATY, g, r).text().equals(g.text())) changed++;
        check(changed > n * 0.003 && changed < n * 0.01, "young platys with a new allele: " + changed);
        h.succeed();
    }

    // ------------------------------------------------------------------ the magnifier

    /** Right-click on a tank analyses every animal (one use each); the result survives buckets, bags and another tank. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void magnifierAnalysesAndTheResultStays(GameTestHelper h) {
        var level = h.getLevel();
        ServerPlayer sp = player(h);
        TankBlockEntity c = tank(h, 2, 1, 2, 18000, 0F);
        TankBlockEntity other = tank(h, 12, 1, 12, 18000, 0F);
        BettaEntity b = ModEntities.BETTA.get().create(level);
        b.setBettaData(BettaData.create(BettaGenotype.randomWild(level.random), false, 0));
        ItemStack bb = b.getBucketItemStack();
        b.saveToBucketTag(bb);
        b.discard();
        check(!useOn(sp, h, new BlockPos(3, 2, 3), bb).is(ModItems.BETTA_BUCKET.get()), "the betta did not go in");
        Vec3 p = Vec3.atCenterOf(h.absolutePos(new BlockPos(3, 1, 3)));
        TankmateEntity neon = TankmateBreeding.spawn(level, c, TankmateSpecies.NEON_TETRA, TankmateGenes.wild(TankmateSpecies.NEON_TETRA), true, LifeStage.ADULT, 0, p);
        TankCritters.add(level, c, CritterSpecies.NEOCARIDINA, CritterGenes.pure(0, 0.5F), true, 20);
        TankCritters.add(level, c, CritterSpecies.NEOCARIDINA, CritterGenes.pure(0, 0.4F), false, 20);
        var live = TankInhabitants.liveInhabitants(level, c.getTank());
        check(live.size() == 2 && !neon.analysed(), "setup: " + live);
        BettaEntity inTank = (BettaEntity) live.stream().filter(e -> e instanceof BettaEntity).findFirst().orElseThrow();
        check(!inTank.getBettaData().analysed(), "a new betta knows its genes");
        // the whole tank: 4 animals, 4 uses
        ItemStack lupe = new ItemStack(ModItems.MAGNIFIER.get());
        ItemStack used = useOn(sp, h, new BlockPos(3, 2, 3), lupe);
        check(used.getDamageValue() == 4, "the magnifier was not used once per animal: " + used.getDamageValue());
        check(inTank.getBettaData().analysed() && neon.analysed() && c.getTank().critters().stream().allMatch(k -> k.analysed), "not every animal is analysed");
        // again: nothing left to analyse, no wear
        used = useOn(sp, h, new BlockPos(3, 2, 3), used);
        check(used.getDamageValue() == 4, "analysing again wore the magnifier");
        // the betta into a bucket (an empty bucket), into the other tank: still analysed
        sp.getInventory().clearContent();
        sp.getInventory().add(new ItemStack(Items.BUCKET));
        check(TankInhabitants.takeOut(sp, c, inTank.getUUID(), false) == null, "the betta could not be taken out");
        ItemStack bettaBucket = null;
        for (ItemStack s : sp.getInventory().items) if (s.is(ModItems.BETTA_BUCKET.get())) bettaBucket = s;
        check(bettaBucket != null && org.overengineer.bettacraft.entity.betta.BettaBucketItem.data(bettaBucket).analysed(), "the bucket lost the analysis");
        useOn(sp, h, new BlockPos(13, 2, 13), bettaBucket.copy());
        BettaEntity moved = (BettaEntity) TankInhabitants.liveInhabitants(level, other.getTank()).stream().filter(e -> e instanceof BettaEntity).findFirst()
                .orElseThrow(() -> new GameTestAssertException("the betta is not in the other tank"));
        check(moved.getBettaData().analysed(), "the other tank lost the analysis");
        // the neon into a bag, the shrimp into a bucket: still analysed
        sp.getInventory().clearContent();
        sp.getInventory().add(new ItemStack(ModItems.FISH_BAG.get()));
        sp.getInventory().add(new ItemStack(Items.BUCKET));
        check(TankInhabitants.takeOut(sp, c, neon.getUUID(), true) == null, "the neon could not be taken out");
        check(TankCritters.takeOut(level, c, sp, c.getTank().critters().getFirst().species.id() + ":" + c.getTank().critters().getFirst().genes.variety(CritterSpecies.NEOCARIDINA),
                false, 1, TankCritters.SEX_ANY, true) == null, "the shrimp could not be taken out");
        boolean bagOk = false, bucketOk = false;
        for (ItemStack s : sp.getInventory().items) {
            if (s.getItem() instanceof FishBagItem && FishBagItem.isFilled(s)) {
                CompoundTag fish = TankmateBucketItem.data(s);
                bagOk = fish != null && GeneVisibility.analysed(fish) && fish.contains(GeneVisibility.KEY);
            }
            if (s.getItem() instanceof CritterBucketItem) bucketOk = CritterBucketItem.animals(s).stream().allMatch(GeneVisibility::analysed);
        }
        check(bagOk && bucketOk, "bag " + bagOk + ", bucket " + bucketOk);
        h.succeed();
    }

    /** One selected animal (the menu's button), the free genes of the config, and animals of older worlds analysed. */
    @GameTest(template = EMPTY, timeoutTicks = 100, batch = "bettacraft_wildlife_config")
    public static void magnifierOneAnimalAndOldWorlds(GameTestHelper h) {
        var level = h.getLevel();
        TankmateEntity m = ModEntities.TANKMATES.get(TankmateSpecies.PLATY).get().create(level);
        check(!m.analysed() && MagnifierItem.analyse(m) && m.analysed() && !MagnifierItem.analyse(m), "one tankmate");
        // an old tankmate (no flag in its data): analysed
        CompoundTag t = m.saveTankmate();
        t.remove(GeneVisibility.KEY);
        m.setAnalysed(false);
        m.loadTankmate(t);
        check(m.analysed(), "an old tankmate is not analysed");
        // an old betta (schema 3)
        CompoundTag v3 = new CompoundTag();
        v3.putInt("schema", 3);
        v3.putString("sex", "female");
        v3.putString("stage", "adult");
        check(BettaData.load(v3).analysed(), "an old betta is not analysed");
        // genes always visible (config)
        boolean before = org.overengineer.bettacraft.config.ServerConfig.GENES_ALWAYS_VISIBLE.get();
        try {
            org.overengineer.bettacraft.config.ServerConfig.GENES_ALWAYS_VISIBLE.set(true);
            check(GeneVisibility.free() && GeneVisibility.visible(false), "genes not free with the config");
        } finally {
            org.overengineer.bettacraft.config.ServerConfig.GENES_ALWAYS_VISIBLE.set(before);
        }
        check(!GeneVisibility.visible(false), "genes free without the config");
        m.discard();
        h.succeed();
    }

    // ------------------------------------------------------------------ taking out: by sex, capacities, water

    /** The quick take-out by sex picks the best of that sex; the list takes exactly the chosen one. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void takeOutBySexAndExactlyOne(GameTestHelper h) {
        var level = h.getLevel();
        ServerPlayer sp = player(h);
        TankBlockEntity c = tank(h, 2, 1, 2, 18000, 0F);
        // one variety (Sakura: grade 0.3-0.55), males and females of different grades
        Critter m1 = TankCritters.add(level, c, CritterSpecies.NEOCARIDINA, CritterGenes.pure(0, 0.52F), false, 20);
        Critter f1 = TankCritters.add(level, c, CritterSpecies.NEOCARIDINA, CritterGenes.pure(0, 0.35F), true, 20);
        Critter f2 = TankCritters.add(level, c, CritterSpecies.NEOCARIDINA, CritterGenes.pure(0, 0.45F), true, 20);
        Critter m2 = TankCritters.add(level, c, CritterSpecies.NEOCARIDINA, CritterGenes.pure(0, 0.32F), false, 20);
        String kind = TankCritters.groupKey(f1);
        sp.getInventory().add(new ItemStack(ModItems.FISH_BAG.get(), 4));
        check(TankCritters.takeOut(level, c, sp, kind, false, 1, TankCritters.SEX_FEMALE, false) == null, "no female taken");
        check(!c.getTank().critters().contains(f2) && c.getTank().critters().contains(f1) && c.getTank().critters().contains(m1), "not the best female");
        check(TankCritters.takeOutById(level, c, sp, m2.id, false) == null && !c.getTank().critters().contains(m2), "not exactly the chosen male");
        // both went into the same bag (one species), which used 100 mB once
        int bags = 0;
        for (ItemStack s : sp.getInventory().items) if (AnimalContainers.critters(s) != null) bags++;
        check(bags == 1, "the second animal did not join the bag: " + bags);
        check(TankCritters.takeOut(level, c, sp, kind, false, 1, TankCritters.SEX_MALE, false) == null && !c.getTank().critters().contains(m1), "no male");
        check(TankCritters.takeOut(level, c, sp, kind, false, 1, TankCritters.SEX_MALE, false) != null, "a male from a tank without males");
        h.succeed();
    }

    /** Buckets: 1000 mB with an empty bucket, nothing with a fitting water bucket, the wrong water refused, the capacity. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void bucketsTakeTankWaterOrUseTheirOwn(GameTestHelper h) {
        var level = h.getLevel();
        ServerPlayer sp = player(h);
        TankBlockEntity fresh = tank(h, 2, 1, 2, 18000, 0F);
        for (int i = 0; i < 35; i++) TankCritters.add(level, fresh, CritterSpecies.RAMSHORN, CritterGenes.WILD, false, 20);
        String kind = TankCritters.groupKey(fresh.getTank().critters().getFirst());
        sp.getInventory().add(new ItemStack(Items.BUCKET));
        long w = fresh.getTank().waterMb();
        check(TankCritters.takeOut(level, fresh, sp, kind, false, 40, TankCritters.SEX_ANY, true) == null, "no snails into the bucket");
        ItemStack bucket = null;
        for (ItemStack s : sp.getInventory().items) if (s.getItem() instanceof CritterBucketItem) bucket = s;
        check(bucket != null && CritterBucketItem.animals(bucket).size() == AnimalContainers.bucket(CritterSpecies.RAMSHORN), "the bucket does not hold "
                + AnimalContainers.bucket(CritterSpecies.RAMSHORN) + ": " + (bucket == null ? 0 : CritterBucketItem.animals(bucket).size()));
        check(fresh.getTank().waterMb() == w - 1000, "the empty bucket did not take 1000 mB: " + (w - fresh.getTank().waterMb()));
        // a bucket of water: the tank keeps its water
        sp.getInventory().clearContent();
        sp.getInventory().add(new ItemStack(Items.WATER_BUCKET));
        w = fresh.getTank().waterMb();
        check(TankCritters.takeOut(level, fresh, sp, kind, false, 1, TankCritters.SEX_ANY, true) == null && fresh.getTank().waterMb() == w, "the water bucket took tank water");
        check(sp.getInventory().items.stream().anyMatch(s -> s.getItem() instanceof CritterBucketItem), "the water bucket was not used");
        // a reef animal: plain water refused, sea water fine
        TankBlockEntity reef = tank(h, 14, 1, 14, 18000, 35F);
        TankCritters.add(level, reef, CritterSpecies.CLEANER_SHRIMP, CritterGenes.WILD, false, 20);
        sp.getInventory().clearContent();
        sp.getInventory().add(new ItemStack(Items.WATER_BUCKET));
        String rk = TankCritters.groupKey(reef.getTank().critters().getFirst());
        check(TankCritters.takeOut(level, reef, sp, rk, false, 1, TankCritters.SEX_ANY, true) != null, "a reef animal into plain water");
        sp.getInventory().clearContent();
        sp.getInventory().add(new ItemStack(ModItems.SEA_WATER_BUCKET.get()));
        check(TankCritters.takeOut(level, reef, sp, rk, false, 1, TankCritters.SEX_ANY, true) == null, "a reef animal not into sea water");
        // tankmates: several neons in one bucket, one betta per bucket
        TankBlockEntity fish = tank(h, 2, 5, 14, 18000, 0F);
        Vec3 p = Vec3.atCenterOf(h.absolutePos(new BlockPos(3, 5, 15)));
        List<TankmateEntity> neons = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) neons.add(TankmateBreeding.spawn(level, fish, TankmateSpecies.NEON_TETRA, TankmateGenes.wild(TankmateSpecies.NEON_TETRA), i % 2 == 0,
                LifeStage.ADULT, 0, p));
        sp.getInventory().clearContent();
        sp.getInventory().add(new ItemStack(Items.BUCKET));
        w = fish.getTank().waterMb();
        for (TankmateEntity n : neons) check(TankInhabitants.takeOut(sp, fish, n.getUUID(), false) == null, "a neon could not be taken out");
        ItemStack nb = null;
        for (ItemStack s : sp.getInventory().items) if (s.getItem() instanceof TankmateBucketItem) nb = s;
        check(nb != null && AnimalContainers.fish(nb).size() == 3, "the neons are not in one bucket: " + nb);
        check(fish.getTank().waterMb() == w - 1000, "the neons took more than one bucket of water");
        // into the tank again: all of them, with the water of the bucket
        useOn(sp, h, new BlockPos(3, 6, 15), nb.copy());
        check(TankInhabitants.liveInhabitants(level, fish.getTank()).size() == 3, "the bucket of neons did not go back in");
        h.succeed();
    }

    /** The sea water bucket picks up vanilla fish like a water bucket; our reef fish need it, freshwater fish refuse it. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void seaWaterBucketCatchesFish(GameTestHelper h) {
        var level = h.getLevel();
        ServerPlayer sp = player(h);
        pool(h, 4, 4, 6, 3);
        BlockPos at = h.absolutePos(new BlockPos(6, 3, 6));
        var cod = EntityType.COD.spawn(level, at, MobSpawnType.SPAWN_EGG);
        sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SEA_WATER_BUCKET.get()));
        sp.interactOn(cod, InteractionHand.MAIN_HAND);
        check(sp.getItemInHand(InteractionHand.MAIN_HAND).is(Items.COD_BUCKET), "the sea water bucket did not catch a cod: " + sp.getItemInHand(InteractionHand.MAIN_HAND));
        var clown = ModEntities.TANKMATES.get(TankmateSpecies.OCELLARIS_CLOWNFISH).get().spawn(level, at, MobSpawnType.SPAWN_EGG);
        sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        sp.interactOn(clown, InteractionHand.MAIN_HAND);
        check(sp.getItemInHand(InteractionHand.MAIN_HAND).is(Items.WATER_BUCKET) && clown.isAlive(), "a reef fish went into plain water");
        sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SEA_WATER_BUCKET.get()));
        sp.interactOn(clown, InteractionHand.MAIN_HAND);
        check(sp.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.TANKMATE_BUCKETS.get(TankmateSpecies.OCELLARIS_CLOWNFISH).get()), "the clownfish did not go into sea water");
        var neon = ModEntities.TANKMATES.get(TankmateSpecies.NEON_TETRA).get().spawn(level, at, MobSpawnType.SPAWN_EGG);
        sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SEA_WATER_BUCKET.get()));
        sp.interactOn(neon, InteractionHand.MAIN_HAND);
        check(sp.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.SEA_WATER_BUCKET.get()) && neon.isAlive(), "a freshwater fish went into sea water");
        // a bucket of neons takes the next one of its species
        sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        sp.interactOn(neon, InteractionHand.MAIN_HAND);
        var neon2 = ModEntities.TANKMATES.get(TankmateSpecies.NEON_TETRA).get().spawn(level, at, MobSpawnType.SPAWN_EGG);
        sp.interactOn(neon2, InteractionHand.MAIN_HAND);
        check(AnimalContainers.fish(sp.getItemInHand(InteractionHand.MAIN_HAND)).size() == 2 && neon2.isRemoved(), "the second neon did not join the bucket");
        h.succeed();
    }

    /** A bucket of small animals lets them go into the world only after the confirmation: they crawl off as entities. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void releasingSmallAnimalsNeedsTheConfirmation(GameTestHelper h) {
        var level = h.getLevel();
        ServerPlayer sp = player(h);
        pool(h, 4, 4, 4, 3);
        stand(h, sp, new BlockPos(5, 6, 5), 90F);
        List<CompoundTag> animals = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) animals.add(critter(CritterSpecies.RAMSHORN, false, CritterGenes.pure(0, 0.3F), 12).saveAnimal());
        ItemStack bucket = AnimalContainers.critterBucket(animals);
        ItemStack after = use(sp, bucket);
        check(after.getItem() instanceof CritterBucketItem, "emptied without asking");
        check(level.getEntitiesOfClass(WildCritterEntity.class, new AABB(h.absolutePos(new BlockPos(5, 3, 5))).inflate(5)).isEmpty(), "let go without asking");
        ContainerRules.confirm(sp, InteractionHand.MAIN_HAND);
        check(sp.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "the confirmed bucket did not empty: " + sp.getItemInHand(InteractionHand.MAIN_HAND));
        var free = level.getEntitiesOfClass(WildCritterEntity.class, new AABB(h.absolutePos(new BlockPos(5, 3, 5))).inflate(5));
        check(free.size() == 5, "not five snails in the world: " + free.size());
        free.forEach(e -> e.discard());
        h.succeed();
    }

    // ------------------------------------------------------------------ wild corals

    /** The reef feature settles colonies on top of coral blocks under water; shears cut a frag, then it is trimmed. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void wildCoralsGrowOnReefsAndGiveFrags(GameTestHelper h) {
        var level = h.getLevel();
        // the feature is in the warm ocean
        var warm = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.WARM_OCEAN).value();
        boolean listed = warm.getGenerationSettings().features().stream().anyMatch(set -> set.stream()
                .anyMatch(f -> f.unwrapKey().map(k -> k.location().equals(BettaCraft.id("wild_coral"))).orElse(false)));
        check(listed, "the wild corals are not in the warm ocean");
        // a small reef: coral blocks with water above
        for (int x = 2; x <= 12; x++)
            for (int z = 2; z <= 12; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.STONE.defaultBlockState());
                h.setBlock(new BlockPos(x, 2, z), (x + z) % 2 == 0 ? Blocks.BRAIN_CORAL_BLOCK.defaultBlockState() : Blocks.TUBE_CORAL_BLOCK.defaultBlockState());
                for (int y = 3; y <= 6; y++) h.setBlock(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState());
            }
        boolean grew = false;
        var r = net.minecraft.util.RandomSource.create(4);
        for (int i = 0; i < 10 && !grew; i++) grew = WildCoralFeature.place(level, h.absolutePos(new BlockPos(7, 3, 7)), r);
        check(grew, "no wild coral grew on the reef");
        BlockPos coral = null;
        for (int x = 2; x <= 12 && coral == null; x++)
            for (int z = 2; z <= 12 && coral == null; z++) if (h.getBlockState(new BlockPos(x, 3, z)).getBlock() instanceof WildCoralBlock) coral = new BlockPos(x, 3, z);
        check(coral != null, "the coral is not on top of the reef");
        check(h.getBlockState(coral).getFluidState().isSource(), "the coral is not under water");
        PlantSpecies s = ((WildCoralBlock) h.getBlockState(coral).getBlock()).species();
        // shears: a frag, the colony trimmed; a second cut gives nothing
        ServerPlayer sp = player(h);
        useOn(sp, h, coral, new ItemStack(Items.SHEARS));
        check(h.getBlockState(coral).getValue(WildCoralBlock.TRIMMED), "the colony is not trimmed");
        var frags = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(h.absolutePos(coral)).inflate(2));
        check(frags.size() == 1 && frags.getFirst().getItem().getItem() instanceof AquaticPlantItem a && a.species() == s, "no frag of " + s + ": " + frags);
        useOn(sp, h, coral, new ItemStack(Items.SHEARS));
        check(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(h.absolutePos(coral)).inflate(2)).size() == 1, "a trimmed colony gave a frag");
        // off the reef nothing grows
        check(!WildCoralFeature.place(level, h.absolutePos(new BlockPos(25, 3, 25)), r), "a coral grew without a reef");
        h.succeed();
    }
}
