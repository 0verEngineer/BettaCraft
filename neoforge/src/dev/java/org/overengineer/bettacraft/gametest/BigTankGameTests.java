package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.big.BigAnimals;
import org.overengineer.bettacraft.big.BigCatching;
import org.overengineer.bettacraft.big.DolphinCare;
import org.overengineer.bettacraft.big.TransportTubItem;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M30 part a: big tanks and big animals. The size rule (with its messages), big tanks with the coarser interior scale (an
 * interior shrunk when its tank grows past 16 blocks, nothing lost), the dolphin only in very big sea water tanks with its
 * care, the transport tub (in and out of a tank, slower while carried), buckets and bags refusing big animals, and a
 * benchmark of eight pangasius in a 32 x 8 x 16 tank (their slow turns, the tick time).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BigTankGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private BigTankGameTests() {}

    /** A tank of sx * sy * sz blocks at x/y/z, full of water of this salinity. */
    static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz, float salt) {
        for (int b = 0; b < sy; b++)
            for (int a = 0; a < sx; a++)
                for (int c = 0; c < sz; c++) {
                    BlockPos p = new BlockPos(x + a, y + b, z + c);
                    h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
                }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(x, y, z)));
        if (c == null) throw new GameTestAssertException("tank did not form");
        TankData t = c.getTank();
        if (t.sizeX() != sx || t.sizeY() != sy || t.sizeZ() != sz) throw new GameTestAssertException("tank is " + t.sizeX() + "x" + t.sizeY() + "x" + t.sizeZ());
        TankInteractions.changeWater(h.getLevel(), c, t.capacityMb(), salt);
        t.setWaterTemp(25F);
        t.setTargetTemp(25F);
        t.setWaterQuality(1F);
        return c;
    }

    static ItemStack pangasiusTub(GameTestHelper h) {
        ItemStack tub = BigCatching.caught(h.getLevel(), TankmateSpecies.PANGASIUS, NetCatching.random(h.getLevel()));
        if (tub == null) throw new GameTestAssertException("no pangasius");
        return tub;
    }

    static ItemStack dolphinTub(GameTestHelper h) {
        Dolphin d = EntityType.DOLPHIN.create(h.getLevel());
        if (d == null) throw new GameTestAssertException("no dolphin");
        ItemStack tub = TransportTubItem.fillWith(d);
        d.discard();
        return tub;
    }

    static Component insert(GameTestHelper h, TankBlockEntity c, ItemStack stack) {
        EntityType<?> type = TankInhabitants.containedType(stack);
        if (type == null) throw new GameTestAssertException("no entity type in " + stack);
        return TankInhabitants.insert(h.getLevel(), c, stack, type, null, TankInhabitants.carriedWater(stack));
    }

    static void ok(Component error) {
        if (error != null) throw new GameTestAssertException("insert failed: " + error.getString());
    }

    /** The first filled transport tub in the inventory (a creative player keeps the empty one, like a bucket). */
    static ItemStack filledTub(ServerPlayer p) {
        for (ItemStack s : p.getInventory().items) if (TransportTubItem.isFilled(s)) return s;
        return ItemStack.EMPTY;
    }

    static boolean says(Component error, String key) {
        return error != null && error.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc && tc.getKey().equals(key);
    }

    // ------------------------------------------------------------------ size rule

    /** A 1 m pangasius needs 12 x 6 x 3 blocks and 100 water blocks each; the message names the size. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void sizeRuleOfTheBigAnimals(GameTestHelper h) {
        h.assertTrue(TankInhabitants.accepts(org.overengineer.bettacraft.registry.ModEntities.TANKMATES.get(TankmateSpecies.PANGASIUS).get(), null)
                == TankInhabitants.Acceptance.BIG, "a pangasius depends on the tank");
        h.assertTrue(BigAnimals.length(EntityType.DOLPHIN, null) > 2.5 && BigAnimals.length(EntityType.DOLPHIN, null) < 2.7, "a dolphin is 2.6 m");
        h.assertTrue(BigAnimals.length(EntityType.SQUID, null) == 0 && BigAnimals.length(EntityType.COD, null) == 0, "small animals have no big length");
        TankBlockEntity small = tank(h, 0, 1, 0, 11, 3, 6, 0F);
        Component e = insert(h, small, pangasiusTub(h));
        h.assertTrue(says(e, "message.bettacraft.big.too_small"), "11 x 6 is too short: " + (e == null ? "accepted" : e.getString()));
        h.assertTrue(e.getString().contains("12") && e.getString().contains("6"), "the message names 12 x 6: " + e.getString());
        TankBlockEntity c = tank(h, 0, 1, 10, 12, 3, 6, 0F);
        var watcher = QuestAdv.watcher(h, c); // M45
        ok(insert(h, c, pangasiusTub(h)));
        QuestAdv.check(h, watcher, "big_animal");
        h.assertTrue(!QuestAdv.done(watcher, "tankmate_inserted"), "a big animal, no tankmate");
        QuestAdv.gone(h, watcher);
        ok(insert(h, c, pangasiusTub(h))); // 216 water blocks: two (200)
        Component third = insert(h, c, pangasiusTub(h));
        h.assertTrue(says(third, "message.bettacraft.big.too_little_water"), "a third needs 300 water blocks: " + (third == null ? "accepted" : third.getString()));
        h.assertTrue(TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).size() == 2, "two inside");
        // small animals as before: a betta into the small tank
        h.succeed();
    }

    // ------------------------------------------------------------------ buckets and bags refuse big animals

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void bucketsAndBagsRefuseBigAnimals(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankmateEntity fish = org.overengineer.bettacraft.registry.ModEntities.TANKMATES.get(TankmateSpecies.PANGASIUS).get().create(level);
        fish.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(5, 2, 5)));
        level.addFreshEntity(fish);
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            fish.interact(p, InteractionHand.MAIN_HAND);
            h.assertTrue(fish.isAlive() && !fish.isRemoved(), "a water bucket does not take a pangasius");
            h.assertTrue(p.getMainHandItem().is(Items.WATER_BUCKET), "the bucket stays a water bucket");
            ItemStack bag = new ItemStack(ModItems.FISH_BAG.get());
            p.setItemInHand(InteractionHand.MAIN_HAND, bag);
            ModItems.FISH_BAG.get().interactLivingEntity(bag, p, fish, InteractionHand.MAIN_HAND);
            h.assertTrue(fish.isAlive() && !FishBagItem.isFilled(p.getMainHandItem()), "a fish bag does not take a pangasius");
            h.assertTrue(TankInhabitants.rejection(TankInhabitants.Acceptance.BIG, fish.getType()).getString().contains("transport"), "the message names the tub");
            h.assertTrue(ModItems.TANKMATE_BUCKETS.get(TankmateSpecies.PANGASIUS) == null, "there is no pangasius bucket");
            // a wild one is too quick for the tub alone: the large net catches it into an empty tub of the inventory
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LARGE_NET.get()));
            p.getInventory().setItem(5, new ItemStack(ModItems.TRANSPORT_TUB.get()));
            ModItems.LARGE_NET.get().interactLivingEntity(p.getMainHandItem(), p, fish, InteractionHand.MAIN_HAND);
            h.assertTrue(fish.isRemoved(), "the large net caught it");
            h.assertTrue(TransportTubItem.containedType(filledTub(p)) == fish.getType(), "into the tub: " + filledTub(p));
        } finally {
            p.discard();
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ the transport tub

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void transportTubCarriesABigAnimal(GameTestHelper h) {
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            TankBlockEntity c = tank(h, 0, 1, 0, 12, 3, 6, 0F);
            double base = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
            ItemStack tub = pangasiusTub(h);
            p.getInventory().setItem(3, tub);
            TransportTubItem.updateSlowness(p);
            double slow = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
            h.assertTrue(slow < base * 0.7 && slow > base * 0.6, "35 % slower with a full tub: " + base + " -> " + slow);
            ok(insert(h, c, p.getInventory().getItem(3)));
            p.getInventory().setItem(3, new ItemStack(ModItems.TRANSPORT_TUB.get()));
            TransportTubItem.updateSlowness(p);
            h.assertTrue(Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED) - base) < 1e-9, "the empty tub is light");
            var in = TankInhabitants.liveInhabitants(h.getLevel(), c.getTank());
            h.assertTrue(in.size() == 1 && in.getFirst() instanceof TankmateEntity m && m.species() == TankmateSpecies.PANGASIUS, "the pangasius swims in the tank");
            TankmateEntity fish = (TankmateEntity) in.getFirst();
            String genes = fish.genes().toString();
            // out again through the tank menu: into the empty tub (no bucket, no bag needed)
            Component out = TankInhabitants.takeOut(p, c, fish.getUUID(), false);
            h.assertTrue(out == null, "taken out: " + (out == null ? "" : out.getString()));
            ItemStack back = filledTub(p);
            h.assertTrue(TransportTubItem.isFilled(back), "into the tub: " + back);
            var stored = TransportTubItem.storedEntity(back);
            h.assertTrue(stored != null && stored.contains("tankmate"), "with all its data: " + stored);
            h.assertTrue(TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).isEmpty(), "the tank is empty");
            Component again = TankInhabitants.takeOut(p, c, java.util.UUID.randomUUID(), false);
            h.assertTrue(again != null, "nothing more to take out");
            ok(insert(h, c, back));
            TankmateEntity back2 = (TankmateEntity) TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).getFirst();
            h.assertTrue(back2.genes().toString().equals(genes), "the same fish came back: " + genes + " / " + back2.genes());
        } finally {
            p.discard();
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ dolphins

    /** The dolphin only in a very big sea water tank (32 x 16 x 8, two of them), fed with raw fish, never drowning. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void dolphinOnlyInAVeryBigSeaWaterTank(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 0, 1, 0, 32, 8, 16, 0F);
        Component e = insert(h, c, dolphinTub(h));
        h.assertTrue(says(e, "message.bettacraft.big.dolphin_salt") || says(e, "message.bettacraft.reef.needs_salt"), "no dolphin in fresh water: "
                + (e == null ? "accepted" : e.getString()));
        c.getTank().setSalinity(35F);
        ok(insert(h, c, dolphinTub(h)));
        ok(insert(h, c, dolphinTub(h)));
        Component third = insert(h, c, dolphinTub(h));
        h.assertTrue(says(third, "message.bettacraft.big.too_little_water"), "a third dolphin needs more water: " + (third == null ? "accepted" : third.getString()));
        List<Dolphin> ds = DolphinCare.dolphins(level, c.getTank());
        h.assertTrue(ds.size() == 2, "two dolphins: " + ds.size());
        Dolphin d = ds.getFirst();
        d.setAirSupply(10);
        DolphinCare.step(level, c, 0.01);
        h.assertTrue(d.getAirSupply() == d.getMaxAirSupply(), "it breathes at the surface of the tank: " + d.getAirSupply());
        // hungry after two days: it loses health; raw fish on the tank feeds the hungriest one
        float hp = d.getHealth();
        org.overengineer.bettacraft.platform.Platform.get().persistentData(d).putLong(DolphinCare.FED, level.getGameTime() - 3 * 24000L);
        ds.get(1).getPersistentData().putLong(DolphinCare.FED, level.getGameTime());
        DolphinCare.step(level, c, 0.5);
        h.assertTrue(d.getHealth() < hp, "a hungry dolphin loses health: " + hp + " -> " + d.getHealth());
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            ItemStack cod = new ItemStack(Items.COD, 2);
            var r = DolphinCare.feed(cod, level, c.getBlockPos(), p, InteractionHand.MAIN_HAND);
            h.assertTrue(r == net.minecraft.world.ItemInteractionResult.CONSUME, "fed: " + r);
            h.assertTrue(DolphinCare.daysHungry(d, level.getGameTime()) < 0.01, "the hungry one ate");
        } finally {
            p.discard();
        }
        // a 16 x 8 x 16 sea water tank is too small for it
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void dolphinNeedsThirtyTwoBlocks(GameTestHelper h) {
        TankBlockEntity c = tank(h, 0, 1, 0, 16, 8, 16, 35F);
        Component e = insert(h, c, dolphinTub(h));
        h.assertTrue(says(e, "message.bettacraft.big.too_small") && e.getString().contains("32"), "16 x 16 is too small: "
                + (e == null ? "accepted" : e.getString()));
        h.succeed();
    }

    // ------------------------------------------------------------------ interior scale

    /**
     * A 16 blocks long tank keeps scale 16; joined to 17 blocks it gets scale 8 and its interior is shrunk: the layout
     * stays at half the size, the rest (and a chest with its contents) comes out as items.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void bigTankGetsTheCoarserInterior(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 0, 1, 0, 16, 2, 4, 0F);
        h.assertTrue(c.getTank().scale() == 16, "16 blocks long: scale 16");
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        for (int x = 0; x < 2; x++)
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 2; z++) in.setBlock(r.toWorld(32 + x, y, 32 + z), Blocks.STONE.defaultBlockState(), 2);
        in.setBlock(r.toWorld(101, 5, 11), Blocks.DIRT.defaultBlockState(), 2);
        BlockPos chest = r.toWorld(60, 0, 20);
        in.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
        ((Container) in.getBlockEntity(chest)).setItem(0, new ItemStack(Items.DIAMOND, 3));
        // grow to 17 blocks
        h.setBlock(new BlockPos(16, 1, 0), ModBlocks.TANK.get().defaultBlockState());
        TankMultiblock.onTankBlockAdded(level, h.absolutePos(new BlockPos(16, 1, 0)), null);
        TankBlockEntity big = TankMultiblock.getController(level, h.absolutePos(new BlockPos(0, 1, 0)));
        h.assertTrue(big != null && big.getTank().sizeX() == 17, "17 blocks long");
        h.assertTrue(big.getTank().scale() == 8, "scale 8: " + big.getTank().scale());
        InteriorRegion nr = InteriorRegions.get(level.getServer()).get(big.getTank().regionId());
        h.assertTrue(nr != null && nr.scale() == 8 && nr.built() && nr.sizeX() == 17 * 8, "the new interior: " + (nr == null ? "none" : nr.sizeX() + " scale " + nr.scale()));
        h.assertTrue(in.getBlockState(nr.toWorld(16, 0, 16)).is(Blocks.STONE), "the stone cube is one stone block at half the place");
        h.assertTrue(in.getBlockState(nr.toWorld(50, 2, 5)).isAir(), "a single block of a cube does not stay");
        // the rest as items in front of the tank
        Map<net.minecraft.world.item.Item, Integer> items = new HashMap<>();
        AABB box = new AABB(h.absolutePos(new BlockPos(-2, 0, -2))).expandTowards(22, 8, 10);
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, box)) items.merge(ie.getItem().getItem(), ie.getItem().getCount(), Integer::sum);
        h.assertTrue(items.getOrDefault(Items.STONE, 0) == 7, "7 of the 8 stones back: " + items);
        h.assertTrue(items.getOrDefault(Items.DIRT, 0) == 1 && items.getOrDefault(Items.DIAMOND, 0) == 3 && items.getOrDefault(Items.CHEST, 0) == 1,
                "the dirt, the chest and its diamonds back: " + items);
        h.succeed();
    }

    // ------------------------------------------------------------------ benchmark

    /**
     * Eight pangasius in a 32 x 8 x 16 tank for 300 ticks: they stay in the water, swim (wide curves, never turning faster
     * than their turn rate allows) and the server tick stays cheap. Logs the numbers (runs alone in its batch).
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m30_bench", timeoutTicks = 500)
    public static void benchmarkBigAnimals(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 0, 1, 0, 32, 8, 16, 0F);
        for (int i = 0; i < 8; i++) ok(insert(h, c, pangasiusTub(h)));
        List<TankmateEntity> fish = new ArrayList<>();
        for (var e : TankInhabitants.liveInhabitants(level, c.getTank())) if (e instanceof TankmateEntity m) fish.add(m);
        h.assertTrue(fish.size() == 8, "eight pangasius: " + fish.size());
        Map<TankmateEntity, Float> yaw = new HashMap<>();
        Map<TankmateEntity, Double> path = new HashMap<>();
        Map<TankmateEntity, net.minecraft.world.phys.Vec3> last = new HashMap<>();
        double[] maxTurn = {0};
        long[] nanos = {0};
        int[] ticks = {0};
        long[] start = {0};
        float rate = (float) TankmateSpecies.PANGASIUS.large().turnRate();
        h.onEachTick(() -> {
            long now = System.nanoTime();
            if (start[0] != 0) {
                nanos[0] += now - start[0];
                ticks[0]++;
            }
            start[0] = now;
            for (TankmateEntity m : fish) {
                Float y = yaw.put(m, m.getYRot());
                if (y != null) maxTurn[0] = Math.max(maxTurn[0], Math.abs(net.minecraft.util.Mth.wrapDegrees(m.getYRot() - y)));
                var p = last.put(m, m.position());
                if (p != null) path.merge(m, p.distanceTo(m.position()), Double::sum);
            }
        });
        h.runAfterDelay(300, () -> {
            var tb = TankInhabitants.tankBox(c.getTank());
            for (TankmateEntity m : fish) {
                h.assertTrue(m.isAlive() && tb.contains(m.position()), "stays in the tank: " + m.position());
                h.assertTrue(path.getOrDefault(m, 0.0) > 3.0, "it swims: " + path.getOrDefault(m, 0.0));
            }
            h.assertTrue(maxTurn[0] <= rate * 1.6 + 0.5, "slow turns: at most " + maxTurn[0] + " degrees per tick");
            double avgMs = ticks[0] == 0 ? 0 : nanos[0] / 1e6 / ticks[0];
            org.slf4j.LoggerFactory.getLogger("bettacraft/m30").info("M30 benchmark: 8 pangasius in 32x8x16, {} ticks, average server tick {} ms, max turn {} deg/tick, paths {}",
                    ticks[0], String.format(java.util.Locale.ROOT, "%.2f", avgMs), String.format(java.util.Locale.ROOT, "%.2f", maxTurn[0]),
                    path.values().stream().map(v -> String.format(java.util.Locale.ROOT, "%.1f", v)).toList());
            h.assertTrue(avgMs < 50, "the server tick stays cheap: " + avgMs + " ms");
            h.succeed();
        });
    }
}
