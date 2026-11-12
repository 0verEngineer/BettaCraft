package org.overengineer.bettacraft.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.overengineer.bettacraft.compat.FishContainers;
import org.overengineer.bettacraft.compat.betterfishtanks.BetterFishTanksCompat;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M7 part of the server smoke test: which aquarium mods are present, their fish in a Bettacraft tank, the Better Fish
 * Tanks bag, a betta in a Better Fish Tanks tank, a betta on the Fish of Thieves plaque and Fintastic plants placed in the
 * dry interior. Every step only runs if its mod is loaded, so the test passes with and without them.
 */
final class DevSmokeM7 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    static final List<String> MODS = List.of("betterfishtanks", "fintastic", "fishofthieves", "aquaculture", "naturalist",
            "terrablender", "regions_unexplored", "biomesoplenty");
    /** Fish buckets of the aquarium mods that should all be accepted. */
    static final List<String> BUCKETS = List.of("fishofthieves:splashtail_bucket", "fishofthieves:pondie_bucket",
            "aquaculture:minnow_bucket", "aquaculture:bluegill_bucket", "fintastic:guppy_bucket", "fintastic:pleco_bucket",
            "fintastic:gourami_bucket", "naturalist:bass_bucket");

    private DevSmokeM7() {}

    private static boolean loaded(String mod) {
        return ModList.get().isLoaded(mod);
    }

    private static BlockPos compatTank(BlockPos base) {
        return base.offset(12, 0, 0);
    }

    private static BlockPos bftTank(BlockPos base) {
        return base.offset(18, 0, 0);
    }

    /** First phase (tick 60): tanks are built. */
    static void setup(ServerLevel level, BlockPos base) {
        Map<String, Boolean> mods = new TreeMap<>();
        for (String m : MODS) mods.put(m, loaded(m));
        LOGGER.info("[smoketest] M7 mods: {}", mods);
        BlockPos t = compatTank(base);
        for (int x = 0; x < 4; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++)
            if (!level.getBlockState(t.offset(x, y, z)).is(ModBlocks.TANK.get())) level.setBlockAndUpdate(t.offset(x, y, z), ModBlocks.TANK.get().defaultBlockState());
        if (loaded(BetterFishTanksCompat.MOD_ID)) {
            Block bft = BuiltInRegistries.BLOCK.get(BetterFishTanksCompat.TANK_BLOCK);
            BlockPos b = bftTank(base);
            for (int x = 0; x < 3; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++)
                if (!level.getBlockState(b.offset(x, y, z)).is(bft)) level.setBlockAndUpdate(b.offset(x, y, z), bft.defaultBlockState());
        }
    }

    /** Second phase (before PASSED). */
    static void check(ServerLevel level, BlockPos base) {
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        TankBlockEntity c = TankMultiblock.getController(level, compatTank(base));
        if (c == null) throw new IllegalStateException("compat tank did not form");
        // leftovers of an earlier run (the dev world persists)
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) e.discard();
        TankInhabitants.reconcile(level, c);
        TankInteractions.changeWater(level, c, c.getTank().capacityMb() - c.getTank().waterMb());
        List<String> inserted = new ArrayList<>();
        for (String id : BUCKETS) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
            if (item == Items.AIR) continue;
            ItemStack stack = new ItemStack(item);
            var err = TankInhabitants.insert(level, c, stack, TankInhabitants.containedType(stack), fake, 0L);
            if (err != null) throw new IllegalStateException(id + ": " + err.getString());
            inserted.add(id);
        }
        int expected = inserted.size();
        if (loaded(BetterFishTanksCompat.MOD_ID)) {
            // a betta through the BFT fish bag, and out again into an empty BFT bag
            BettaEntity b = ModEntities.BETTA.get().create(level);
            BettaData d = BettaData.create(BettaGenome.randomDomestic(level.getRandom()), false, level.getGameTime());
            b.loadFromBucketTag(BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d).get(DataComponents.BUCKET_ENTITY_DATA).copyTag());
            ItemStack bag = FishContainers.fill(new ItemStack(BuiltInRegistries.ITEM.get(FishContainers.BFT_FISH_BAG)), b, null);
            b.discard();
            var err = TankInhabitants.insert(level, c, bag, ModEntities.BETTA.get(), fake, TankInhabitants.carriedWater(bag));
            if (err != null) throw new IllegalStateException("BFT bag: " + err.getString());
            inserted.add("betterfishtanks:fish_bag_item(betta)");
            expected++;
        }
        int live = TankInhabitants.liveInhabitants(level, c.getTank()).size();
        LOGGER.info("[smoketest] M7 compat tank: inserted {} -> {} animals inside", inserted, live);
        if (live != expected) throw new IllegalStateException("compat tank: expected " + expected + " animals, found " + live);
        // tags
        List<String> predators = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) if (type.is(ModTags.PREDATORS)) predators.add(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        List<String> blacklist = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) if (type.is(ModTags.TANK_BLACKLIST)) blacklist.add(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        int pelletFish = 0;
        for (Item i : BuiltInRegistries.ITEM) if (new ItemStack(i).is(ModTags.PELLET_FISH)) pelletFish++;
        LOGGER.info("[smoketest] M7 predators {}, blacklist {} entries, {} raw fish for pellets", predators, blacklist.size(), pelletFish);
        if (!predators.contains("minecraft:pufferfish") || pelletFish < 2) throw new IllegalStateException("tags");
        var biomes = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getTag(ModTags.BETTA_SPAWN_BIOMES)
                .map(s -> s.stream().map(h -> h.unwrapKey().map(k -> k.location().toString()).orElse("?")).toList()).orElse(List.of());
        LOGGER.info("[smoketest] M7 betta spawn biomes: {}", biomes);
        if (biomes.isEmpty()) throw new IllegalStateException("no spawn biomes");
        if (loaded(BetterFishTanksCompat.MOD_ID)) bftRelease(level, base, fake);
        if (loaded("fishofthieves")) plaque(level, base, fake);
        if (loaded("fintastic")) interiorPlants(level, c, fake);
    }

    private static void bftRelease(ServerLevel level, BlockPos base, FakePlayer fake) {
        BlockPos b = bftTank(base);
        var be = level.getBlockEntity(b);
        try {
            be.getClass().getMethod("addWater", int.class).invoke(be, 6000);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("BFT addWater", e);
        }
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(b).expandTowards(2, 1, 1);
        for (BettaEntity old : level.getEntitiesOfClass(BettaEntity.class, box)) old.discard();
        ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()),
                BettaData.create(BettaGenome.randomDomestic(level.getRandom()), true, level.getGameTime()));
        fake.setItemInHand(InteractionHand.MAIN_HAND, bucket);
        var err = BetterFishTanksCompat.release(level, b, fake, InteractionHand.MAIN_HAND, bucket);
        if (err != null) throw new IllegalStateException("BFT release: " + err.getString());
        int n = level.getEntitiesOfClass(BettaEntity.class, box).size();
        LOGGER.info("[smoketest] M7 betta released into the Better Fish Tanks tank: {} betta(s) inside", n);
        if (n != 1) throw new IllegalStateException("betta not in the BFT tank");
    }

    private static void plaque(ServerLevel level, BlockPos base, FakePlayer fake) {
        BlockPos wall = base.offset(24, 0, 1), pos = base.offset(24, 0, 0);
        level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        Block oak = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("fishofthieves", "oak_fish_plaque"));
        level.setBlockAndUpdate(pos, oak.defaultBlockState());
        ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()),
                BettaData.create(BettaGenome.randomDomestic(level.getRandom()), false, level.getGameTime()));
        fake.setItemInHand(InteractionHand.MAIN_HAND, bucket);
        level.getBlockState(pos).useItemOn(bucket, level, fake, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
        var nbt = level.getBlockEntity(pos).saveWithoutMetadata(level.registryAccess()).getCompound("PlaqueData");
        LOGGER.info("[smoketest] M7 fish plaque holds {} (betta data {})", nbt.getString("id"), nbt.contains(BettaData.KEY));
        if (!"bettacraft:betta".equals(nbt.getString("id")) || !nbt.contains(BettaData.KEY)) throw new IllegalStateException("plaque");
    }

    /** Fintastic water plants placed with their items in the dry interior (mixins), duckweed included. */
    private static void interiorPlants(ServerLevel level, TankBlockEntity c, FakePlayer fake) {
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        List<String> placed = new ArrayList<>();
        String[] plants = {"hornwort", "anubias", "amazon_sword", "duckweed"};
        for (int i = 0; i < plants.length; i++) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("fintastic", plants[i]));
            BlockPos on = r.toWorld(4 + 3 * i, -1, 4); // floor block
            in.removeBlock(on.above(), false);
            ItemStack stack = new ItemStack(item);
            var ctx = new BlockPlaceContext(in, fake, InteractionHand.MAIN_HAND, stack,
                    new BlockHitResult(Vec3.atCenterOf(on).add(0, 0.5, 0), Direction.UP, on, false));
            item.useOn(ctx);
            var s = in.getBlockState(on.above());
            if (item instanceof BlockItem bi && s.is(bi.getBlock())) placed.add(plants[i]);
        }
        LOGGER.info("[smoketest] M7 Fintastic plants placed in the interior: {}", placed);
        if (placed.size() != plants.length) throw new IllegalStateException("interior plants: " + placed);
    }
}
