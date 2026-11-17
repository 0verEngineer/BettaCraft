package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.compat.FishContainers;
import org.overengineer.bettacraft.compat.betterfishtanks.BetterFishTanksCompat;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaCare;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.food.BettaFood;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankBreeding;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M7: compatibility with the aquarium mods. Every test checks only what its mod provides and succeeds right away when
 * the mod is not installed (gradlew runGameTestServer -PnoCompat), so the suite runs in both setups.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CompatGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private CompatGameTests() {}

    // ------------------------------------------------------------------ helpers

    /** True if {@code modId} is missing: the test then succeeds without checking anything. */
    private static boolean skip(GameTestHelper h, String modId) {
        if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded(modId)) return false;
        org.slf4j.LoggerFactory.getLogger("bettacraft/gametest").info("{} not installed: compat test skipped", modId);
        h.succeed();
        return true;
    }

    private static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz, long water) {
        for (int a = 0; a < sx; a++)
            for (int b = 0; b < sy; b++)
                for (int c = 0; c < sz; c++) {
                    BlockPos p = new BlockPos(x + a, y + b, z + c);
                    h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
                }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(x, y, z)));
        if (c == null) throw new GameTestFailure("tank did not form");
        TankInteractions.changeWater(h.getLevel(), c, water);
        return c;
    }

    private static Item item(String ns, String path) {
        Identifier id = Identifier.fromNamespaceAndPath(ns, path);
        Item i = BuiltInRegistries.ITEM.getValue(id);
        if (i == Items.AIR) throw new GameTestFailure("missing item " + id);
        return i;
    }

    private static EntityType<?> entity(String ns, String path) {
        Identifier id = Identifier.fromNamespaceAndPath(ns, path);
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElseThrow(() -> new GameTestFailure("missing entity " + id));
    }

    private static ItemStack bettaBucket(long seed) {
        BettaGenome g = BettaGenome.randomDomestic(RandomSource.create(seed));
        return BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), BettaData.create(g, seed % 2 == 0, 0));
    }

    /** A betta entity (not added to the level) with the data of {@link #bettaBucket}. */
    private static BettaEntity betta(GameTestHelper h, long seed) {
        BettaEntity b = ModEntities.BETTA.get().create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (b == null) throw new GameTestFailure("no betta");
        b.loadFromBucketTag(bettaBucket(seed).get(DataComponents.BUCKET_ENTITY_DATA).copyTag());
        return b;
    }

    @Nullable
    private static Component insert(GameTestHelper h, TankBlockEntity c, ItemStack stack, Player p) {
        EntityType<?> type = TankInhabitants.containedType(stack);
        if (type == null) throw new GameTestFailure("no entity type in " + stack);
        return TankInhabitants.insert(h.getLevel(), c, stack, type, p, TankInhabitants.carriedWater(stack));
    }

    private static void ok(@Nullable Component error) {
        if (error != null) throw new GameTestFailure("insert failed: " + error.getString());
    }

    private static Object call(String cls, String method, Class<?>[] types, Object... args) {
        try {
            return Class.forName(cls).getMethod(method, types).invoke(null, args);
        } catch (ReflectiveOperationException e) {
            throw new GameTestFailure(cls + "#" + method + ": " + e);
        }
    }

    private static ItemStack findFilled(ServerPlayer sp, Item item) {
        for (ItemStack s : sp.getInventory().getNonEquipmentItems()) if (s.is(item) && FishContainers.storedEntity(s) != null) return s;
        throw new GameTestFailure("no filled " + BuiltInRegistries.ITEM.getKey(item) + " in the inventory");
    }

    // ------------------------------------------------------------------ Better Fish Tanks

    /** Their fish bag puts a betta into our tank (+100 mB) and takes it out again (-100 mB) in their own format. */
    @GameTest(template = EMPTY)
    public static void betterFishTanksBagBothWays(GameTestHelper h) {
        if (skip(h, "betterfishtanks")) return;
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 2, 2, 5000);
        Item bagItem = item("betterfishtanks", "fish_bag_item");
        BettaEntity source = betta(h, 11);
        BettaData expected = source.getBettaData();
        h.assertTrue(BetterFishTanksCompat.catchable(source), "BFT's own classifier accepts bettas (catchable_fish tag)");
        ItemStack bag = FishContainers.fill(new ItemStack(bagItem), source, null);
        source.discard();
        CompoundTag fishData = (CompoundTag) call("net.mint_gaming_studios.betterfishtanksmod.item.custom.FishBagItem", "getFishData",
                new Class<?>[]{ItemStack.class}, bag);
        h.assertTrue(fishData != null && "bettacraft:betta".equals(fishData.getStringOr("id", "")), "BFT reads our bag format");
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        try {
            ok(insert(h, c, bag, sp));
            h.assertTrue(c.getTank().waterMb() == 5100, "BFT bag adds 100 mB, got " + c.getTank().waterMb());
            List<Entity> live = TankInhabitants.liveInhabitants(h.getLevel(), c.getTank());
            h.assertTrue(live.size() == 1 && live.getFirst() instanceof BettaEntity, "one betta inside");
            BettaEntity in = (BettaEntity) live.getFirst();
            h.assertTrue(in.getBettaData().genome().equals(expected.genome()) && in.getBettaData().fishId().equals(expected.fishId()),
                    "genome and id survive the BFT bag");
            h.assertTrue(TankInhabitants.takeOut(sp, c, in.getUUID(), true) != null, "no empty bag -> refused");
            sp.getInventory().add(new ItemStack(bagItem));
            h.assertTrue(TankInhabitants.bagWater(sp) == 100, "BFT bag takes 100 mB");
            h.assertTrue(TankInhabitants.takeOut(sp, c, in.getUUID(), true) == null, "take out with the BFT bag");
            h.assertTrue(c.getTank().waterMb() == 5000, "BFT bag removes 100 mB, got " + c.getTank().waterMb());
            ItemStack filled = findFilled(sp, bagItem);
            CompoundTag stored = FishContainers.storedEntity(filled);
            h.assertTrue("bettacraft:betta".equals(stored.getStringOr("id", "")) && stored.contains(BettaData.KEY), "full entity NBT in StoredEntity");
            h.assertTrue(!stored.getCompoundOrEmpty("neoforge:attachments").contains(BettaCraft.id("tank_ref").toString()), "tank_ref released");
            h.succeed();
        } finally {
            sp.level().getServer().getPlayerList().remove(sp);
        }
    }

    /** Our betta bucket releases its betta into a Better Fish Tanks aquarium (their releaseEntityInside). */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void bettaIntoBetterFishTanksTank(GameTestHelper h) {
        if (skip(h, "betterfishtanks")) return;
        Block bft = BuiltInRegistries.BLOCK.getValue(BetterFishTanksCompat.TANK_BLOCK);
        for (int x = 0; x < 3; x++)
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 3; z++) h.setBlock(new BlockPos(4 + x, 1 + y, 4 + z), bft.defaultBlockState());
        BlockPos rel = new BlockPos(5, 1, 5);
        h.runAfterDelay(20, () -> {
            BlockEntity be = h.getLevel().getBlockEntity(h.absolutePos(rel));
            h.assertTrue(BetterFishTanksCompat.fillWater(be, 9000) == 9000, "9000 mB into the 18 block BFT tank");
            h.assertTrue(BetterFishTanksCompat.waterAmount(be) == 9000, "BFT tank holds the water");
            h.assertTrue(h.getLevel().getFluidState(h.absolutePos(rel)).is(net.minecraft.tags.FluidTags.WATER), "lower layer is water");
            Player p = h.makeMockPlayer(GameType.SURVIVAL);
            ItemStack bucket = bettaBucket(21);
            p.setItemInHand(InteractionHand.MAIN_HAND, bucket);
            Component err = BetterFishTanksCompat.release(h.getLevel(), h.absolutePos(rel), p, InteractionHand.MAIN_HAND, bucket);
            h.assertTrue(err == null, "release failed: " + (err == null ? "" : err.getString()));
            h.assertTrue(p.getMainHandItem().is(Items.BUCKET), "empty bucket back");
            h.assertTrue(BetterFishTanksCompat.waterAmount(be) == 10000, "the bucket brought 1000 mB");
            AABB box = new AABB(h.absolutePos(new BlockPos(4, 1, 4))).expandTowards(3, 2, 3);
            List<BettaEntity> in = h.getLevel().getEntitiesOfClass(BettaEntity.class, box);
            h.assertTrue(in.size() == 1, "one betta in the BFT tank, found " + in.size());
            h.assertTrue(!TankInhabitants.hasTankRef(in.getFirst()), "no Bettacraft tank_ref in a BFT tank");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ Fintastic

    /** The Fintastic fishnet works both ways (no water), plus the tags: blacklist, size, pleco, predators, feed. */
    @GameTest(template = EMPTY)
    public static void fintasticNetAndTags(GameTestHelper h) {
        if (skip(h, "fintastic")) return;
        TankBlockEntity c = tank(h, 2, 1, 2, 3, 2, 3, 12000);
        Item fishnet = item("fintastic", "fishnet");
        Entity guppy = entity("fintastic", "guppy").create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        ItemStack full = FishContainers.fill(new ItemStack(fishnet), guppy, null);
        guppy.discard();
        h.assertTrue((boolean) call("net.voidarkana.fintastic.common.item.custom.FishnetItem", "containsEntity",
                new Class<?>[]{ItemStack.class}, full), "Fintastic reads our fishnet format");
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        try {
            ok(insert(h, c, full, sp));
            h.assertTrue(c.getTank().waterMb() == 12000, "a fishnet brings no water");
            Entity in = TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).getFirst();
            h.assertTrue(in.getType() == entity("fintastic", "guppy"), "guppy inside");
            sp.getInventory().add(new ItemStack(fishnet));
            h.assertTrue(TankInhabitants.bagWater(sp) == 0, "a fishnet takes no water");
            h.assertTrue(TankInhabitants.takeOut(sp, c, in.getUUID(), true) == null, "take out with the fishnet");
            h.assertTrue(c.getTank().waterMb() == 12000, "still no water change");
            h.assertTrue((boolean) call("net.voidarkana.fintastic.common.item.custom.FishnetItem", "containsEntity",
                    new Class<?>[]{ItemStack.class}, findFilled(sp, fishnet)), "Fintastic sees the guppy in the net");
        } finally {
            sp.level().getServer().getPlayerList().remove(sp);
        }
        h.assertTrue(TankInhabitants.accepts(entity("fintastic", "arapaima"), null) == TankInhabitants.Acceptance.BIG, "arapaima a big animal (M39)");
        h.assertTrue(TankInhabitants.accepts(entity("fintastic", "coelacanth"), null) != TankInhabitants.Acceptance.OK, "coelacanth refused");
        h.assertTrue(TankInhabitants.accepts(entity("fintastic", "pleco"), null) == TankInhabitants.Acceptance.OK, "pleco via tank_inhabitants");
        h.assertTrue(entity("fintastic", "freshwater_shark").is(ModTags.PREDATORS), "sharkminnow is a predator");
        h.assertTrue(BettaFood.kind(new ItemStack(item("fintastic", "regular_feed"))) == BettaFood.Kind.NORMAL, "fish feed feeds bettas");
        h.assertTrue(Blocks.LILY_PAD.defaultBlockState().is(ModTags.FLOATING_PLANTS)
                && BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("fintastic", "duckweed")).defaultBlockState().is(ModTags.FLOATING_PLANTS),
                "duckweed floats like a lily pad");
        h.assertTrue(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("fintastic", "hornwort")).defaultBlockState()
                .is(ModTags.INTERIOR_WATER_PLANTS), "hornwort is an interior water plant");
        h.succeed();
    }

    /** A hungry betta eats Fintastic fairy shrimp (artemia) as live food. */
    @GameTest(template = EMPTY)
    public static void bettaEatsFintasticPrey(GameTestHelper h) {
        if (skip(h, "fintastic")) return;
        TankBlockEntity c = tank(h, 2, 1, 2, 3, 2, 3, 12000);
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        ok(insert(h, c, bettaBucket(31), p));
        ok(insert(h, c, new ItemStack(item("fintastic", "artemia_bucket")), p));
        BettaEntity b = (BettaEntity) TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).stream()
                .filter(e -> e instanceof BettaEntity).findFirst().orElseThrow();
        h.assertTrue(entity("fintastic", "artemia").is(ModTags.BETTA_PREY), "artemia is prey");
        BettaHealth.Params params = BettaCare.params();
        b.health().step(2.0, BettaHealth.Conditions.GOOD, params);
        h.assertTrue(b.health().isHungry(params), "betta is hungry after two days");
        TankBreeding.step(h.getLevel(), c, 0.01);
        h.assertTrue(!b.health().isHungry(params), "betta was fed by the prey");
        h.assertTrue(TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).size() == 1, "the artemia was eaten");
        h.succeed();
    }

    // ------------------------------------------------------------------ Fish of Thieves

    /** Their fish live in our tanks (predators tagged) and a betta bucket puts a betta on their fish plaque. */
    @GameTest(template = EMPTY)
    public static void fishOfThievesFishAndPlaque(GameTestHelper h) {
        if (skip(h, "fishofthieves")) return;
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 2, 2, 6000);
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        ok(insert(h, c, new ItemStack(item("fishofthieves", "splashtail_bucket")), p));
        h.assertTrue(TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).getFirst().getType() == entity("fishofthieves", "splashtail"),
                "splashtail inside");
        for (String pred : List.of("battlegill", "wrecker", "devilfish")) {
            h.assertTrue(entity("fishofthieves", pred).is(ModTags.PREDATORS), pred + " is a predator");
        }
        h.assertTrue(BettaFood.kind(new ItemStack(item("fishofthieves", "earthworms"))) == BettaFood.Kind.LIVE, "earthworms are live food");
        // fish plaque on a wall
        BlockPos wall = new BlockPos(10, 2, 11), plaque = new BlockPos(10, 2, 10);
        h.setBlock(wall, Blocks.STONE);
        Block oak = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("fishofthieves", "oak_fish_plaque"));
        h.setBlock(plaque, oak.defaultBlockState());
        ItemStack bucket = bettaBucket(41);
        p.setItemInHand(InteractionHand.MAIN_HAND, bucket);
        h.useBlock(plaque, p);
        CompoundTag nbt = h.getLevel().getBlockEntity(h.absolutePos(plaque)).saveWithoutMetadata(h.getLevel().registryAccess());
        CompoundTag data = nbt.getCompoundOrEmpty("PlaqueData");
        h.assertTrue("bettacraft:betta".equals(data.getStringOr("id", "")), "betta on the plaque, got " + nbt);
        h.assertTrue(data.contains(BettaData.KEY), "with its genome");
        h.assertTrue(p.getMainHandItem().is(Items.WATER_BUCKET), "water bucket back");
        h.succeed();
    }

    // ------------------------------------------------------------------ Aquaculture

    /** Small Aquaculture fish are accepted, the big ones (M39: by their profile length) only into a big tank although their hitbox is small. */
    @GameTest(template = EMPTY)
    public static void aquacultureSizes(GameTestHelper h) {
        if (skip(h, "aquaculture")) return;
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 2, 2, 6000);
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        ok(insert(h, c, new ItemStack(item("aquaculture", "minnow_bucket")), p));
        h.assertTrue(insert(h, c, new ItemStack(item("aquaculture", "tuna_bucket")), p) != null, "tuna refused");
        h.assertTrue(TankInhabitants.accepts(entity("aquaculture", "tuna"), null) == TankInhabitants.Acceptance.BIG, "tuna a big animal (M39)");
        h.assertTrue(TankInhabitants.accepts(entity("aquaculture", "arapaima"), null) == TankInhabitants.Acceptance.BIG, "arapaima a big animal (M39)");
        h.assertTrue(entity("aquaculture", "piranha").is(ModTags.PREDATORS), "piranha is a predator");
        h.assertTrue(BettaFood.kind(new ItemStack(item("aquaculture", "worm"))) == BettaFood.Kind.LIVE, "worms are live food");
        h.assertTrue(new ItemStack(item("aquaculture", "fish_fillet_raw")).is(ModTags.PELLET_FISH), "fillets make pellets");
        h.assertTrue(new ItemStack(item("aquaculture", "perch")).is(ModTags.PELLET_FISH), "raw fish (c:foods/raw_fish) make pellets");
        h.assertTrue(TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).size() == 1, "only the minnow");
        h.succeed();
    }

    // ------------------------------------------------------------------ Naturalist

    @GameTest(template = EMPTY)
    public static void naturalistFish(GameTestHelper h) {
        if (skip(h, "naturalist")) return;
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 2, 2, 6000);
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        ok(insert(h, c, new ItemStack(item("naturalist", "bass_bucket")), p));
        Entity bass = TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).getFirst();
        h.assertTrue(bass instanceof Bucketable && bass.getType() == entity("naturalist", "bass"), "bass inside");
        h.assertTrue(entity("naturalist", "piranha").is(ModTags.PREDATORS) && entity("naturalist", "anglerfish").is(ModTags.PREDATORS),
                "piranha and anglerfish are predators");
        h.succeed();
    }
}
