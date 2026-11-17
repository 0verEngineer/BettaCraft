package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaCare;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateCare;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.reef.ProteinSkimmerBlock;
import org.overengineer.bettacraft.reef.ReefCare;
import org.overengineer.bettacraft.reef.TankReef;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tankmates.Clutch;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M20 GameTests: salt water tanks. Sea water from the ocean with an empty bucket, mixing and diluting the salt, sea salt
 * and calcium additive like a player, freshwater animals dying in salt water and reef animals in fresh water (and being
 * refused at the glass), brackish-tolerant nerites and livebearers, the protein skimmer and live rock cleaning only salt
 * water, corals growing on reef rock with light, flow and calcium (and not in fresh water), fragging with shears,
 * clownfish spawning at the foot of their anemone, the net on the reef, the foreign-animal config and old freshwater
 * tanks staying as they were.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ReefGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private ReefGameTests() {}

    // ------------------------------------------------------------------ helpers

    private static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz, long water, float salt) {
        for (int a = 0; a < sx; a++)
            for (int b = 0; b < sy; b++)
                for (int c = 0; c < sz; c++) {
                    BlockPos p = new BlockPos(x + a, y + b, z + c);
                    h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
                }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(x, y, z)));
        if (c == null) throw new GameTestFailure("tank did not form");
        if (water > 0) TankInteractions.changeWater(h.getLevel(), c, water, salt);
        c.getTank().setWaterTemp(26F);
        c.getTank().setTargetTemp(26F);
        return c;
    }

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        sp.getInventory().clearContent();
        sp.getInventory().setSelectedSlot(8);
        return sp;
    }

    private static void use(ServerPlayer sp, BlockPos pos, Direction face, ItemStack stack) {
        sp.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        sp.gameMode.useItemOn(sp, sp.level(), stack, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
    }

    private static void biome(ServerLevel level, BlockPos at, String biome) {
        for (int x = at.getX() - 4; x <= at.getX() + 4; x += 4) for (int z = at.getZ() - 4; z <= at.getZ() + 4; z += 4) level.getChunk(x >> 4, z >> 4);
        var src = level.getServer().createCommandSourceStack().withSuppressedOutput().withLevel(level);
        level.getServer().getCommands().performPrefixedCommand(src, "fillbiome " + (at.getX() - 4) + " " + (at.getY() - 3) + " " + (at.getZ() - 4) + " "
                + (at.getX() + 4) + " " + (at.getY() + 4) + " " + (at.getZ() + 4) + " " + biome);
    }

    private static ItemStack bettaBucket(long seed) {
        BettaGenome g = BettaGenome.randomDomestic(RandomSource.create(seed));
        return BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), BettaData.create(g, seed % 2 == 0, 0));
    }

    private static Component insert(GameTestHelper h, TankBlockEntity c, ItemStack stack) {
        EntityType<?> type = TankInhabitants.containedType(stack);
        if (type == null) throw new GameTestFailure("no entity type in " + stack);
        return TankInhabitants.insert(h.getLevel(), c, stack, type, null, 0L);
    }

    private static List<BettaEntity> bettas(GameTestHelper h, TankBlockEntity c) {
        List<BettaEntity> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) if (e instanceof BettaEntity b && b.isAlive()) out.add(b);
        return out;
    }

    private static List<TankmateEntity> mates(GameTestHelper h, TankBlockEntity c) {
        List<TankmateEntity> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) if (e instanceof TankmateEntity m && m.isAlive()) out.add(m);
        return out;
    }

    private static TankmateEntity add(GameTestHelper h, TankBlockEntity c, TankmateSpecies s, boolean female) {
        var w = new org.overengineer.bettacraft.tank.TankWaterVolume(c.getTank());
        Vec3 p = w.clamp(new Vec3((w.box().minX + w.box().maxX) / 2, w.box().minY + 0.4, (w.box().minZ + w.box().maxZ) / 2), 0.35, 0.1, 0.2);
        TankmateEntity e = TankmateBreeding.spawn(h.getLevel(), c, s, TankmateGenes.wild(s), female, LifeStage.ADULT, 0, p);
        if (e == null) throw new GameTestFailure("could not spawn " + s.id());
        e.health().feed();
        return e;
    }

    private static boolean logged(TankData t, String key) {
        return t.log().stream().anyMatch(e -> e.key().equals(key));
    }

    private static InteriorRegion region(GameTestHelper h, TankBlockEntity c) {
        if (InteriorManager.interiorLevel(h.getLevel().getServer()) == null) throw new GameTestFailure("no interior dimension");
        return InteriorManager.ensureRegion(h.getLevel(), c);
    }

    private static ServerLevel interior(GameTestHelper h) {
        return InteriorManager.interiorLevel(h.getLevel().getServer());
    }

    private static void light(GameTestHelper h, BlockPos rel, DyeColor color) {
        h.setBlock(rel, ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true).setValue(TankLightBlock.COLOR, color));
    }

    // ------------------------------------------------------------------ salt water

    /** An empty bucket in a warm ocean gives sea water; buckets, sea salt, draining and evaporation change the salinity. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void seaWaterSaltAndMixing(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer sp = player(h);
        try {
            // the ocean: a water source in a warm ocean biome
            BlockPos water = h.absolutePos(new BlockPos(20, 2, 20));
            h.setBlock(new BlockPos(20, 1, 20), Blocks.STONE);
            h.setBlock(new BlockPos(20, 2, 20), Blocks.WATER);
            biome(level, water, "minecraft:warm_ocean");
            sp.teleportTo(level, water.getX() + 0.5, water.getY() + 1.0, water.getZ() + 0.5, java.util.Set.of(), 0F, 90F, false); // right above, looking down
            sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
            sp.gameMode.useItem(sp, level, sp.getMainHandItem(), InteractionHand.MAIN_HAND);
            h.assertTrue(sp.getMainHandItem().is(ModItems.SEA_WATER_BUCKET.get()), "sea water from the ocean: " + sp.getMainHandItem());
            h.assertTrue(level.getBlockState(water).isAir(), "the source was taken");
            // a tank of 4 blocks: two buckets of sea water, two of fresh water = half sea water
            TankBlockEntity c = tank(h, 2, 2, 2, 2, 1, 2, 0, 0F);
            TankData t = c.getTank();
            BlockPos tankPos = c.getBlockPos();
            use(sp, tankPos, Direction.UP, new ItemStack(ModItems.SEA_WATER_BUCKET.get()));
            h.assertTrue(sp.getMainHandItem().is(Items.BUCKET), "the bucket is empty again");
            h.assertTrue(Math.abs(t.salinity() - 35F) < 0.01F && t.marine(), "an empty tank filled with sea water: " + t.salinity());
            h.assertTrue(Math.abs(t.calcium() - Salinity.SEA_CALCIUM) < 1, "with the calcium of the sea: " + t.calcium());
            use(sp, tankPos, Direction.UP, new ItemStack(ModItems.SEA_WATER_BUCKET.get()));
            use(sp, tankPos, Direction.UP, new ItemStack(Items.WATER_BUCKET));
            use(sp, tankPos, Direction.UP, new ItemStack(Items.WATER_BUCKET));
            h.assertTrue(t.waterMb() == 4000 && Math.abs(t.salinity() - 17.5F) < 0.01F, "half sea water: " + t.waterMb() + " mB, " + t.salinity());
            h.assertTrue(Salinity.kind(t.salinity()) == Salinity.Kind.BRACKISH && logged(t, "log.bettacraft.salinity_brackish"), "brackish, logged");
            // sea salt (sneaking: as much as it takes) makes it sea water again
            sp.setShiftKeyDown(true);
            use(sp, tankPos, Direction.UP, new ItemStack(ModItems.SEA_SALT.get(), 10));
            sp.setShiftKeyDown(false);
            h.assertTrue(Math.abs(t.salinity() - 35F) < 0.01F, "salted up: " + t.salinity());
            h.assertTrue(sp.getMainHandItem().getCount() == 8, "two portions for 4 buckets at half salt: " + sp.getMainHandItem().getCount());
            use(sp, tankPos, Direction.UP, sp.getMainHandItem());
            h.assertTrue(sp.getMainHandItem().getCount() == 8, "no salt into sea water");
            // draining a salt water tank gives sea water and keeps the salinity
            use(sp, tankPos, Direction.UP, new ItemStack(Items.BUCKET));
            h.assertTrue(sp.getMainHandItem().is(ModItems.SEA_WATER_BUCKET.get()) && Math.abs(t.salinity() - 35F) < 0.01F, "sea water out: " + sp.getMainHandItem());
            // evaporation leaves the salt: a third of the water gone
            float before = t.salinity();
            t.setEvaporationCarry(0);
            long mb = t.waterMb();
            TankInteractions.changeWater(level, c, -1000);
            t.setSalinity(Salinity.concentrate(before, mb, 1000, Salinity.MAX));
            h.assertTrue(t.salinity() > 50F, "concentrated: " + t.salinity());
            TankWater.step(level, c, 0); // no time: nothing
            // calcium additive
            float ca = t.calcium();
            use(sp, tankPos, Direction.UP, new ItemStack(ModItems.CALCIUM_ADDITIVE.get()));
            h.assertTrue(t.calcium() > ca + 50, "calcium raised: " + ca + " -> " + t.calcium());
            // pipes (fluid handler) bring fresh water: it dilutes
            new org.overengineer.bettacraft.tank.TankFluid(level, tankPos).fill(new org.overengineer.bettacraft.platform.fluid.FluidStack(
                    net.minecraft.world.level.material.Fluids.WATER, 2000), org.overengineer.bettacraft.platform.fluid.IFluidHandler.FluidAction.EXECUTE);
            h.assertTrue(t.salinity() < 40F, "pipes bring fresh water: " + t.salinity());
            // with the config off, an ocean bucket is plain water
            boolean saved = ServerConfig.SALT_OCEAN_BUCKETS.get();
            try {
                ServerConfig.SALT_OCEAN_BUCKETS.set(false);
                h.setBlock(new BlockPos(20, 2, 20), Blocks.WATER);
                sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
                sp.gameMode.useItem(sp, level, sp.getMainHandItem(), InteractionHand.MAIN_HAND);
                h.assertTrue(sp.getMainHandItem().is(Items.WATER_BUCKET), "config off: a water bucket: " + sp.getMainHandItem());
            } finally {
                ServerConfig.SALT_OCEAN_BUCKETS.set(saved);
            }
        } finally {
            sp.level().getServer().getPlayerList().remove(sp);
        }
        h.succeed();
    }

    /**
     * A betta in a freshwater tank dies when it is salted to sea water; a reef fish dies in fresh water. Both are refused
     * at the glass when the water would kill them. Nerites live in brackish water, freshwater shrimp die in it.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void wrongSalinityKills(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity fresh = tank(h, 2, 2, 2, 3, 2, 2, 12000, 0F);
        h.assertTrue(insert(h, fresh, bettaBucket(3)) == null, "a betta into fresh water");
        TankmateEntity guppy = add(h, fresh, TankmateSpecies.GUPPY, true);
        List<BettaEntity> b = bettas(h, fresh);
        h.assertTrue(b.size() == 1, "one betta");
        // brackish water (12 ppt): the guppy is fine, the betta is stressed but alive after half a day
        fresh.getTank().setSalinity(12F);
        BettaCare.advance(level, fresh, b.getFirst(), 0.1);
        TankmateCare.advance(level, fresh, guppy, 0.5);
        h.assertTrue(guppy.isAlive() && guppy.health().saltShock() == 0, "guppies tolerate brackish water");
        h.assertTrue(b.getFirst().isAlive() && b.getFirst().health().saltShock() > 0, "the betta feels it: " + b.getFirst().health().saltShock());
        // sea water: dead within a day, the log says why
        fresh.getTank().setSalinity(35F);
        for (int i = 0; i < 10 && b.getFirst().isAlive(); i++) BettaCare.advance(level, fresh, b.getFirst(), 0.1);
        h.assertTrue(!b.getFirst().isAlive() || b.getFirst().isRemoved(), "the betta died in sea water");
        h.assertTrue(fresh.getTank().log().stream().anyMatch(e -> e.key().equals("log.bettacraft.died") && e.args().contains("@cause.bettacraft.salinity")),
                "logged: salinity");
        // a sea water tank refuses a betta, a fresh tank a clownfish
        TankBlockEntity sea = tank(h, 8, 2, 2, 3, 2, 2, 12000, 35F);
        Component no = insert(h, sea, bettaBucket(4));
        h.assertTrue(no != null && no.getString().length() > 0 && bettas(h, sea).isEmpty(), "betta refused: " + no);
        TankBlockEntity fresh2 = tank(h, 14, 2, 2, 3, 2, 2, 12000, 0F);
        ItemStack clownBucket = new ItemStack(ModItems.TANKMATE_BUCKETS.get(TankmateSpecies.OCELLARIS_CLOWNFISH).get());
        Component no2 = insert(h, fresh2, clownBucket);
        h.assertTrue(no2 != null && mates(h, fresh2).isEmpty(), "clownfish refused in fresh water: " + no2);
        h.assertTrue(insert(h, sea, clownBucket.copy()) == null && mates(h, sea).size() == 1, "clownfish into sea water");
        // a reef fish in fresh water (e.g. the tank was diluted) dies
        TankmateEntity clown = add(h, fresh2, TankmateSpecies.OCELLARIS_CLOWNFISH, false);
        for (int i = 0; i < 10 && clown.isAlive(); i++) TankmateCare.advance(level, fresh2, clown, 0.1);
        h.assertTrue(!clown.isAlive() || clown.isRemoved(), "clownfish died in fresh water");
        // small animals: nerites live in brackish water, Neocaridina do not; cleaner shrimp need sea water
        TankBlockEntity brack = tank(h, 20, 2, 2, 3, 2, 2, 12000, 20F);
        for (int i = 0; i < 3; i++) TankCritters.add(level, brack, CritterSpecies.ZEBRA_NERITE, CritterGenes.WILD, i % 2 == 0, 5);
        for (int i = 0; i < 3; i++) TankCritters.add(level, brack, CritterSpecies.NEOCARIDINA, CritterGenes.WILD, i % 2 == 0, 5);
        Component noShrimp = TankCritters.insert(level, sea, List.of(TankCritters.bagged(TankCritters.bagOf(CritterSpecies.NEOCARIDINA, 1,
                () -> CritterGenes.WILD, new java.util.Random(1)::nextLong, false)).getFirst()), null, 0);
        h.assertTrue(noShrimp != null, "freshwater shrimp refused in sea water");
        for (int i = 0; i < 48; i++) {
            brack.getTank().setCritterFood(50);
            TankCritters.step(level, brack, 1.0 / 24);
        }
        long nerites = brack.getTank().critters().stream().filter(k -> k.alive() && k.species == CritterSpecies.ZEBRA_NERITE).count();
        long shrimp = brack.getTank().critters().stream().filter(k -> k.alive() && k.species == CritterSpecies.NEOCARIDINA).count();
        h.assertTrue(nerites == 3, "nerites tolerate brackish water: " + nerites);
        h.assertTrue(shrimp == 0, "freshwater shrimp die in brackish water: " + shrimp);
        h.succeed();
    }

    /** The skimmer cleans salt water but not fresh water; live rock cleans salt water without power. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void skimmerAndLiveRockCleanSaltWater(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity sea = tank(h, 2, 2, 2, 2, 2, 2, 8000, 35F);
        TankBlockEntity fresh = tank(h, 8, 2, 2, 2, 2, 2, 8000, 0F);
        TankBlockEntity bare = tank(h, 14, 2, 2, 2, 2, 2, 8000, 35F);
        // skimmers hang on the south side of the first two tanks
        h.setBlock(new BlockPos(2, 2, 4), ModBlocks.PROTEIN_SKIMMER.get().defaultBlockState().setValue(ProteinSkimmerBlock.FACING, Direction.NORTH));
        h.setBlock(new BlockPos(8, 2, 4), ModBlocks.PROTEIN_SKIMMER.get().defaultBlockState().setValue(ProteinSkimmerBlock.FACING, Direction.NORTH));
        TankEquipment.invalidate(sea);
        TankEquipment.invalidate(fresh);
        h.assertTrue(TankReef.skimmerCount(level, sea) == 1 && TankReef.skimmerCount(level, fresh) == 1, "skimmers found");
        h.assertTrue(TankReef.skimmerBlocks(level, sea) > 0 && TankReef.skimmerBlocks(level, fresh) == 0, "they only foam in salt water");
        h.assertTrue(TankWater.filterBlocks(level, sea) > TankWater.filterBlocks(level, bare), "the skimmer is a filter in salt water");
        h.assertTrue(TankWater.filterBlocks(level, fresh) == 0, "no effect in fresh water");
        // the same animals for two days: the skimmed salt water stays cleaner
        for (TankBlockEntity c : List.of(sea, fresh, bare)) {
            insert(h, c, c == fresh ? bettaBucket(5) : new ItemStack(ModItems.TANKMATE_BUCKETS.get(TankmateSpecies.YELLOW_TANG).get()));
            c.getTank().setWaterQuality(1F);
            TankWater.step(level, c, 2.0);
        }
        h.assertTrue(sea.getTank().waterQuality() > bare.getTank().waterQuality() + 0.02F, "skimmed water cleaner: " + sea.getTank().waterQuality()
                + " vs " + bare.getTank().waterQuality());
        // live rock in the interior: filter capacity in salt water, nothing in fresh water
        InteriorRegion r = region(h, bare);
        ServerLevel in = interior(h);
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) for (int y = 0; y < 4; y++) in.setBlock(r.toWorld(x, y, z), ModBlocks.LIVE_ROCK.get().defaultBlockState(), 3);
        h.assertTrue(TankReef.liveRock(level, bare.getTank()) == 256, "live rock counted: " + TankReef.liveRock(level, bare.getTank()));
        h.assertTrue(TankReef.liveRockBlocks(level, bare.getTank()) > 0, "live rock cleans");
        bare.getTank().setSalinity(0F);
        h.assertTrue(TankReef.liveRockBlocks(level, bare.getTank()) == 0, "not in fresh water");
        h.succeed();
    }

    // ------------------------------------------------------------------ corals

    /**
     * Corals only go onto reef rock (like a player), grow under actinic + white light with flow pumps and calcium in salt
     * water (stony ones use the calcium up), stay small and bleach in fresh water, and give frags with shears.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void coralsGrowOnReefRock(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 1, 1, 2000, 35F);
        light(h, new BlockPos(2, 2, 2), DyeColor.BLUE);
        light(h, new BlockPos(3, 2, 2), DyeColor.WHITE);
        TankBlockEntity freshTank = tank(h, 2, 1, 8, 2, 1, 1, 2000, 0F);
        light(h, new BlockPos(2, 2, 8), DyeColor.BLUE);
        light(h, new BlockPos(3, 2, 8), DyeColor.WHITE);
        InteriorRegion r = region(h, c), rf = region(h, freshTank);
        ServerLevel in = interior(h);
        ServerPlayer sp = player(h);
        InteriorPlant acro, freshAcro;
        try {
            sp.teleportTo(in, r.toWorld(2, 0, 2).getX() + 0.5, r.toWorld(2, 0, 2).getY(), r.toWorld(2, 0, 2).getZ() + 0.5, java.util.Set.of(), 0F, 0F, true);
            // on stone: refused; on reef rock: glued on
            BlockPos stone = r.toWorld(6, 0, 6);
            in.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
            use(sp, stone, Direction.UP, InteriorPlants.item(PlantSpecies.ACROPORA, 0));
            h.assertTrue(r.plants().isEmpty(), "no coral on plain stone");
            BlockPos rock = r.toWorld(12, 0, 6);
            in.setBlock(rock, ModBlocks.REEF_ROCK.get().defaultBlockState(), 3);
            use(sp, rock, Direction.UP, InteriorPlants.item(PlantSpecies.ACROPORA, 0));
            acro = r.plants().at(12, 1, 6);
            h.assertTrue(acro != null && acro.species == PlantSpecies.ACROPORA, "Acropora on reef rock: " + r.plants().all());
            // flow pumps in the interior (like a player)
            use(sp, r.toWorld(20, 0, 8).below(), Direction.UP, new ItemStack(ModItems.FLOW_PUMP.get()));
            h.assertTrue(TankReef.pumps(level, c.getTank()) == 1, "a flow pump: " + TankReef.pumps(level, c.getTank()));
        } finally {
            sp.level().getServer().getPlayerList().remove(sp);
        }
        in.setBlock(rf.toWorld(12, 0, 6), ModBlocks.REEF_ROCK.get().defaultBlockState(), 3);
        freshAcro = InteriorPlants.plantAt(in, rf, PlantSpecies.ACROPORA, 12, 1, 6, 0, 11L);
        h.assertTrue(freshAcro != null, "planted in the fresh twin");
        h.runAfterDelay(20, () -> {
            h.assertTrue(TankReef.spectrum(level, c.getTank()) > 0.99, "blue + white: " + TankReef.spectrum(level, c.getTank()));
            h.assertTrue(TankReef.flow(level, c) >= 0.7, "strong flow in 2 blocks of water: " + TankReef.flow(level, c));
            float ca0 = c.getTank().calcium();
            for (int i = 0; i < 40 && acro.stage < 4; i++) {
                InteriorPlants.grow(level, c, r, 0.5);
                InteriorPlants.grow(level, freshTank, rf, 0.5);
            }
            h.assertTrue(acro.stage >= 4, "Acropora grows in the reef tank: stage " + acro.stage);
            h.assertTrue(c.getTank().calcium() < ca0 - 30, "calcium used: " + ca0 + " -> " + c.getTank().calcium());
            h.assertTrue(acro.tint == 3, "glows under actinic light: " + acro.tint);
            h.assertTrue(freshAcro.stage == 0, "no growth in fresh water: " + freshAcro.stage);
            h.assertTrue(freshAcro.tint == 0, "bleached in fresh water: " + freshAcro.tint);
            // without calcium a stony coral stops
            c.getTank().setCalcium(100F);
            int st = acro.stage;
            double prog = acro.progress;
            InteriorPlants.grow(level, c, r, 2.0);
            h.assertTrue(acro.stage == st && Math.abs(acro.progress - prog) < 1e-9, "no calcium, no growth");
            // frags with shears (from inside: the M13 trim)
            ServerPlayer sp2 = player(h);
            try {
                sp2.teleportTo(in, r.toWorld(12, 2, 3).getX() + 0.5, r.toWorld(12, 2, 3).getY(), r.toWorld(12, 2, 3).getZ() + 0.5, java.util.Set.of(), 0F, 0F, true);
                int got = InteriorPlants.trim(in, r, acro, sp2, new ItemStack(Items.SHEARS), InteractionHand.MAIN_HAND);
                int frags = 0;
                for (ItemStack s : sp2.getInventory().getNonEquipmentItems()) if (s.is(ModItems.AQUATIC_PLANTS.get(PlantSpecies.ACROPORA).get())) frags += s.getCount();
                h.assertTrue(got > 0 && frags > 0, "frags cut: " + got + " / " + frags);
            } finally {
                sp2.level().getServer().getPlayerList().remove(sp2);
            }
            h.succeed();
        });
    }

    /** A clownfish pair moves into its bubble tip anemone and lays its eggs at the anemone's foot; the male guards them. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void clownfishSpawnAtTheAnemone(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 2, 3, 2, 3, 18000, 35F);
        c.getTank().setWaterTemp(26.5F);
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        in.setBlock(r.toWorld(20, 0, 20), ModBlocks.LIVE_ROCK.get().defaultBlockState(), 3);
        InteriorPlant anemone = InteriorPlants.plantAt(in, r, PlantSpecies.BUBBLE_TIP_ANEMONE, 20, 1, 20, 4, 5L);
        h.assertTrue(anemone != null && anemone.stage >= 1, "an anemone");
        h.assertTrue(org.overengineer.bettacraft.tankmates.TankmateSpots.anemones(level.getServer(), c.getTank()).size() == 1, "found as a host");
        TankmateEntity f = add(h, c, TankmateSpecies.OCELLARIS_CLOWNFISH, true);
        TankmateEntity m = add(h, c, TankmateSpecies.OCELLARIS_CLOWNFISH, false);
        Clutch k = null;
        for (int i = 0; i < 60 && k == null; i++) {
            for (TankmateEntity x : List.of(f, m)) {
                x.health().feed();
                TankmateCare.advance(level, c, x, 0.1);
            }
            c.getTank().setWaterQuality(1F);
            TankmateBreeding.step(level, c, 0.1);
            for (Clutch cl : c.getTank().clutches()) if (cl.species == TankmateSpecies.OCELLARIS_CLOWNFISH) k = cl;
        }
        h.assertTrue(f.cave() != null && m.cave() != null && f.cave().distanceToSqr(m.cave()) < 1e-6, "the pair lives in the anemone");
        h.assertTrue(k != null, "eggs laid: " + c.getTank().clutches());
        h.assertTrue(m.getUUID().equals(k.guardian), "the male guards");
        h.assertTrue(k.pos().y < m.cave().y, "the eggs at the foot of the anemone");
        h.assertTrue(logged(c.getTank(), "log.bettacraft.tm.spawned_anemone"), "logged");
        // without an anemone: no eggs
        TankBlockEntity none = tank(h, 10, 2, 2, 3, 2, 3, 18000, 35F);
        none.getTank().setWaterTemp(26.5F);
        TankmateEntity f2 = add(h, none, TankmateSpecies.OCELLARIS_CLOWNFISH, true), m2 = add(h, none, TankmateSpecies.OCELLARIS_CLOWNFISH, false);
        for (int i = 0; i < 40; i++) {
            for (TankmateEntity x : List.of(f2, m2)) {
                x.health().feed();
                TankmateCare.advance(level, none, x, 0.1);
            }
            TankmateBreeding.step(level, none, 0.1);
        }
        h.assertTrue(none.getTank().clutches().isEmpty(), "no anemone, no eggs");
        h.assertTrue(f2.isAlive() && m2.isAlive(), "but they live fine");
        h.succeed();
    }

    // ------------------------------------------------------------------ sources and config

    /** The net on a warm reef catches reef fish, reef critters and sometimes a coral frag. */
    @GameTest(template = EMPTY, batch = "bettacraft_reef_net", timeoutTicks = 200)
    public static void netOnTheReef(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        double chance = ServerConfig.CRITTERS_NET_CHANCE.get(), share = ServerConfig.REEF_NET_SHARE.get();
        try {
            ServerConfig.CRITTERS_NET_CHANCE.set(1.0);
            ServerConfig.REEF_NET_SHARE.set(0.5);
            BlockPos water = h.absolutePos(new BlockPos(16, 2, 16));
            h.setBlock(new BlockPos(16, 2, 16), Blocks.WATER);
            biome(level, water, "minecraft:warm_ocean");
            h.assertTrue(NetCatching.water(level, water) == NetCatching.Water.WARM_OCEAN, "warm ocean water: " + NetCatching.water(level, water));
            h.assertTrue(NetCatching.fishTable(NetCatching.Water.WARM_OCEAN).containsKey(TankmateSpecies.OCELLARIS_CLOWNFISH), "clownfish on the reef");
            h.assertTrue(!NetCatching.fishTable(NetCatching.Water.JUNGLE).containsKey(TankmateSpecies.OCELLARIS_CLOWNFISH), "not in the jungle");
            ServerPlayer sp = player(h);
            try {
                for (int i = 0; i < 4; i++) sp.getInventory().add(new ItemStack(ModItems.FISH_BAG.get(), 16));
                sp.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.AQUARIUM_NET.get()));
                int reefFish = 0, critters = 0, frags = 0;
                for (int i = 0; i < 40; i++) {
                    sp.getCooldowns().removeCooldown(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ModItems.AQUARIUM_NET.get()));
                    NetCatching.tryCatch(level, sp, InteractionHand.OFF_HAND, water);
                }
                for (ItemStack s : sp.getInventory().getNonEquipmentItems()) {
                    var tm = org.overengineer.bettacraft.tankmates.TankmateItems.bagSpecies(s);
                    if (tm != null && tm.marine()) reefFish++;
                    var l = TankCritters.bagged(s);
                    if (l != null) for (var tag : l) if (org.overengineer.bettacraft.critters.Critter.loadAnimal(tag, 0, 0, 0, 0).species.marine()) critters++;
                    if (s.getItem() instanceof org.overengineer.bettacraft.plants.AquaticPlantItem a && a.species().marine()) frags += s.getCount();
                }
                h.assertTrue(reefFish > 0, "reef fish caught: " + reefFish);
                h.assertTrue(critters > 0, "reef critters caught: " + critters);
                h.assertTrue(reefFish + critters + frags >= 30, "almost every try catches: " + (reefFish + critters + frags));
            } finally {
                sp.level().getServer().getPlayerList().remove(sp);
            }
        } finally {
            ServerConfig.CRITTERS_NET_CHANCE.set(chance);
            ServerConfig.REEF_NET_SHARE.set(share);
        }
        h.succeed();
    }

    /**
     * Foreign animals: lenient by default (a tropical fish lives in fresh water as before M20); with REALISTIC a tropical
     * fish dies in fresh water and lives in sea water.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_reef_foreign", timeoutTicks = 200)
    public static void foreignAnimalsConfig(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var saved = ServerConfig.SALT_FOREIGN.get();
        try {
            TankBlockEntity fresh = tank(h, 2, 2, 2, 3, 2, 2, 12000, 0F), sea = tank(h, 8, 2, 2, 3, 2, 2, 12000, 35F);
            h.assertTrue(insert(h, fresh, new ItemStack(Items.TROPICAL_FISH_BUCKET)) == null, "lenient: a tropical fish into fresh water");
            Entity fish = TankInhabitants.liveInhabitants(level, fresh.getTank()).getFirst();
            ReefCare.step(level, fresh, 2.0);
            h.assertTrue(fish.isAlive() && ReefCare.shock(fish) == 0, "lenient: no harm");
            ServerConfig.SALT_FOREIGN.set(ServerConfig.ForeignSalinity.REALISTIC);
            h.assertTrue(insert(h, fresh, new ItemStack(Items.TROPICAL_FISH_BUCKET)) != null, "realistic: refused in fresh water");
            h.assertTrue(insert(h, sea, new ItemStack(Items.TROPICAL_FISH_BUCKET)) == null, "realistic: into sea water");
            Entity seaFish = TankInhabitants.liveInhabitants(level, sea.getTank()).getFirst();
            for (int i = 0; i < 10; i++) ReefCare.step(level, fresh, 0.1);
            ReefCare.step(level, sea, 1.0);
            h.assertTrue(!fish.isAlive() || fish.isRemoved(), "realistic: the tropical fish died in fresh water");
            h.assertTrue(seaFish.isAlive() && ReefCare.shock(seaFish) == 0, "it lives in sea water");
            h.assertTrue(logged(fresh.getTank(), "log.bettacraft.died"), "logged");
        } finally {
            ServerConfig.SALT_FOREIGN.set(saved);
        }
        h.succeed();
    }

    /**
     * A freshwater tank of an older world (salinity 0) works as before: freshwater plants grow, skimmers do nothing,
     * vanilla corals in the interior live (config off) and die in fresh water with the config on.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_reef_corals_config", timeoutTicks = 200)
    public static void freshwaterTanksStayAsTheyWere(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 1, 1, 2000, 0F);
        light(h, new BlockPos(2, 2, 2), DyeColor.WHITE);
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++)
            org.overengineer.bettacraft.tank.interior.Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z),
                    Blocks.GRAVEL.defaultBlockState(), false);
        InteriorPlant rotala = InteriorPlants.plantAt(in, r, PlantSpecies.ROTALA, 8, 0, 8, 0, 7L);
        BlockPos coral = r.toWorld(20, 0, 6);
        in.setBlock(coral, Blocks.BRAIN_CORAL_BLOCK.defaultBlockState(), 3);
        boolean saved = ServerConfig.SALT_VANILLA_CORALS.get();
        try {
            h.runAfterDelay(20, () -> {
                for (int i = 0; i < 12; i++) InteriorPlants.grow(level, c, r, 0.25);
                h.assertTrue(rotala.stage >= 3, "freshwater plants grow as before: " + rotala.stage);
                // a salted twin stops them
                c.getTank().setSalinity(20F);
                int st = rotala.stage;
                double pr = rotala.progress;
                InteriorPlants.grow(level, c, r, 1.0);
                h.assertTrue(rotala.stage == st && Math.abs(rotala.progress - pr) < 1e-9, "no growth in brackish water");
                c.getTank().setSalinity(0F);
                // vanilla corals: alive with the config off, dead in fresh water with it on
                in.getBlockState(coral).tick(in, coral, in.getRandom());
                h.assertTrue(in.getBlockState(coral).is(Blocks.BRAIN_CORAL_BLOCK), "config off: the coral lives");
                ServerConfig.SALT_VANILLA_CORALS.set(true);
                in.getBlockState(coral).tick(in, coral, in.getRandom());
                h.assertTrue(in.getBlockState(coral).is(Blocks.DEAD_BRAIN_CORAL_BLOCK), "config on: dies in fresh water: " + in.getBlockState(coral));
                in.setBlock(coral, Blocks.BRAIN_CORAL_BLOCK.defaultBlockState(), 3);
                c.getTank().setSalinity(35F);
                in.getBlockState(coral).tick(in, coral, in.getRandom());
                h.assertTrue(in.getBlockState(coral).is(Blocks.BRAIN_CORAL_BLOCK), "config on: lives in sea water");
                ServerConfig.SALT_VANILLA_CORALS.set(saved);
                h.succeed();
            });
        } catch (RuntimeException e) {
            ServerConfig.SALT_VANILLA_CORALS.set(saved);
            throw e;
        }
    }
}
