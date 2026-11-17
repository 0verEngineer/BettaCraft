package org.overengineer.bettacraft.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.equipment.SegmentBlock;
import org.overengineer.bettacraft.food.CultureTubBlockEntity;
import org.overengineer.bettacraft.network.InteriorRequestPayload;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.BubbleNest;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorFloorBlockEntity;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorSync;
import org.overengineer.bettacraft.tank.interior.Substrate;

/**
 * M17 hardening: one GameTest per bug found in the code review (see meilensteine/M17.md) plus a small benchmark of the
 * care step of a tank with 200 small animals.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HardeningGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private HardeningGameTests() {}

    private static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz, long water) {
        return InteriorGameTests.tank(h, x, y, z, sx, sy, sz, water);
    }

    /** Review H2: a client asking for the whole interior again and again gets it once per cooldown. */
    @GameTest(template = EMPTY)
    public static void interiorRequestsAreRateLimited(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 1, 1, 1, 0);
        InteriorRegion r = InteriorGameTests.region(h, c);
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            p.teleportTo(h.getLevel(), c.getBlockPos().getX() + 0.5, c.getBlockPos().getY() + 2, c.getBlockPos().getZ() + 0.5, java.util.Set.of(), 0, 0, false);
            int before = InteriorSync.fullSends;
            for (int i = 0; i < 10; i++) InteriorSync.handleRequest(p, new InteriorRequestPayload(r.id(), c.getBlockPos(), -1));
            h.assertTrue(InteriorSync.fullSends - before == 1, "one full send for 10 requests, got " + (InteriorSync.fullSends - before));
            h.succeed();
        } finally {
            p.level().getServer().getPlayerList().remove(p);
        }
    }

    /** Review C1: taking the substrate out left its (paid) block entity behind; a merge copied it and it could be taken twice. */
    @GameTest(template = EMPTY)
    public static void removedSubstrateIsReallyGone(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 1, 2, 1, 1, 1, 0);
        InteriorRegion r = InteriorGameTests.region(h, c);
        ServerLevel in = InteriorGameTests.interior(h);
        BlockPos floor = new BlockPos(r.originX() + 3, InteriorRegion.FLOOR_Y, r.originZ() + 3);
        Substrate.set(in, floor, Blocks.SAND.defaultBlockState(), true);
        h.assertTrue(Substrate.active(in, floor) != null, "substrate set");
        ItemStack back = Substrate.clear(in, floor);
        h.assertTrue(back.is(Items.SAND), "paid sand back: " + back);
        h.assertTrue(!(in.getBlockEntity(floor) instanceof InteriorFloorBlockEntity), "no block entity left");
        // a neighbour block: the tank (and its interior) is rebuilt by a merge
        h.setBlock(new BlockPos(3, 1, 2), ModBlocks.TANK.get().defaultBlockState());
        TankMultiblock.onTankBlockAdded(level, h.absolutePos(new BlockPos(3, 1, 2)), null);
        TankBlockEntity m = TankMultiblock.getController(level, h.absolutePos(new BlockPos(2, 1, 2)));
        h.assertTrue(m != null && m.getTank().memberCount() == 2, "merged");
        InteriorRegion r2 = InteriorManager.ensureRegion(level, m);
        int found = 0;
        for (int x = 0; x < r2.sizeX(); x++)
            for (int z = 0; z < r2.sizeZ(); z++)
                if (Substrate.active(in, new BlockPos(r2.originX() + x, InteriorRegion.FLOOR_Y, r2.originZ() + z)) != null) found++;
        h.assertTrue(found == 0, "no substrate came back with the merge, found " + found);
        h.succeed();
    }

    /** Review D2: joining a small tank with eggs to a bigger one kept only the bigger tank's (missing) nest. */
    @GameTest(template = EMPTY)
    public static void mergeKeepsTheEggs(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity small = tank(h, 2, 1, 2, 1, 1, 1, 1000);
        tank(h, 4, 1, 2, 2, 1, 2, 4000);
        BlockPos sp = small.getBlockPos();
        CompoundTag n = new CompoundTag();
        n.putString("phase", "eggs");
        n.putDouble("x", sp.getX() + 0.5);
        n.putDouble("y", sp.getY() + 0.9);
        n.putDouble("z", sp.getZ() + 0.5);
        ListTag eggs = new ListTag();
        for (int i = 0; i < 5; i++) {
            CompoundTag e = new CompoundTag();
            e.put("genome", new CompoundTag());
            e.putBoolean("female", i % 2 == 0);
            eggs.add(e);
        }
        n.put("eggs", eggs);
        small.getTank().setNest(BubbleNest.load(n));
        h.setBlock(new BlockPos(3, 1, 2), ModBlocks.TANK.get().defaultBlockState());
        TankMultiblock.onTankBlockAdded(level, h.absolutePos(new BlockPos(3, 1, 2)), null);
        TankBlockEntity m = TankMultiblock.getController(level, sp);
        h.assertTrue(m != null && m.getTank().memberCount() == 6, "merged into one tank of 6");
        BubbleNest nest = m.getTank().nest();
        h.assertTrue(nest != null && nest.hasEggs() && nest.eggCount() == 5, "the 5 eggs survived the merge");
        h.succeed();
    }

    /** Review D1: a block whose controller does not know it stayed "not formed" forever; it forms a tank again. */
    @GameTest(template = EMPTY)
    public static void orphanedTankBlockFormsAgain(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 1, 1, 0);
        BlockPos other = c.getBlockPos().equals(h.absolutePos(new BlockPos(2, 1, 2))) ? h.absolutePos(new BlockPos(3, 1, 2)) : h.absolutePos(new BlockPos(2, 1, 2));
        TankBlockEntity be = TankMultiblock.getBE(level, other);
        h.assertTrue(be != null && !be.isController(), "member block");
        // point it at a position that is no controller (as after a controller that could not be loaded)
        CompoundTag tag = be.saveWithoutMetadata(level.registryAccess());
        BlockPos bogus = other.above(3);
        tag.putIntArray("controller", new int[]{bogus.getX(), bogus.getY(), bogus.getZ()});
        be.loadWithComponents(org.overengineer.bettacraft.util.NbtCompat.input(level.registryAccess(), tag));
        h.assertTrue(TankMultiblock.getController(level, other) == null, "orphaned");
        h.assertTrue(TankMultiblock.repairOrphan(level, other, be), "repaired");
        TankBlockEntity now = TankMultiblock.getController(level, other);
        h.assertTrue(now != null && now.getTank().contains(other), "part of a tank again");
        h.assertTrue(!TankMultiblock.repairOrphan(level, other, be), "a healthy block is left alone");
        h.succeed();
    }

    /** Review D1: corrupt tank data (a bad inhabitant id, NaN temperature, odd scale) loads instead of losing the tank. */
    @GameTest(template = EMPTY)
    public static void corruptTankDataStillLoads(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 1, 2, 2000);
        CompoundTag t = c.getTank().save();
        ListTag inh = new ListTag();
        inh.add(new IntArrayTag(new int[]{1, 2, 3}));
        inh.add(new IntArrayTag(new int[]{1, 2, 3, 4}));
        t.put("inhabitants", inh);
        t.putFloat("water_temp", Float.NaN);
        t.putInt("scale", 5);
        t.putLong("water_mb", -50);
        CompoundTag nest = new CompoundTag();
        nest.putString("phase", "eggs");
        ListTag eggs = new ListTag();
        eggs.add(net.minecraft.nbt.StringTag.valueOf("not an egg"));
        nest.put("eggs", eggs);
        t.put("nest", nest);
        TankData d = TankData.load(t);
        h.assertTrue(d.inhabitants().size() == 1, "the valid inhabitant id is kept, the broken one skipped");
        h.assertTrue(Float.isFinite(d.waterTemp()), "temperature repaired");
        h.assertTrue(d.scale() == 16, "scale repaired: " + d.scale());
        h.assertTrue(d.waterMb() >= 0, "water not negative");
        h.assertTrue(d.memberCount() == 4, "the shape is intact");
        h.succeed();
    }

    /** Review F1: catch-up feeding (simulateUnloadedTime) fed fish from an empty feeder 3 times out of 4. */
    @GameTest(template = EMPTY)
    public static void emptyFeederFeedsNobody(GameTestHelper h) {
        BlockPos fp = new BlockPos(4, 1, 4);
        h.setBlock(fp, ModBlocks.FEEDER.get().defaultBlockState());
        FeederBlockEntity f = h.getBlockEntity(fp, FeederBlockEntity.class);
        for (int i = 0; i < 8; i++) h.assertTrue(!f.consumeVirtualPortion(0.25), "empty feeder, portion " + i);
        f.items().insertItem(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 1), false);
        int fed = 0;
        for (int i = 0; i < 8; i++) if (f.consumeVirtualPortion(0.25)) fed++;
        h.assertTrue(fed == 4, "one pellet = 4 portions, fed " + fed);
        h.assertTrue(f.items().getStackInSlot(0).isEmpty(), "pellet used up");
        h.succeed();
    }

    /** Review F2: two heater rods side by side are removed together and both give their item back. */
    @GameTest(template = EMPTY)
    public static void touchingHeaterRodsBothDrop(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 2, 2, 16000);
        InteriorRegion r = InteriorGameTests.region(h, c);
        ServerLevel in = InteriorGameTests.interior(h);
        BlockPos a = r.toWorld(4, 0, 4), b = a.east();
        for (BlockPos base : List.of(a, b)) {
            for (int y = 0; y < 6; y++) {
                SegmentBlock.Part part = y == 0 ? SegmentBlock.Part.ANCHOR : y == 5 ? SegmentBlock.Part.TOP : SegmentBlock.Part.MIDDLE;
                in.setBlockAndUpdate(base.above(y), ModBlocks.HEATER_SEGMENT.get().defaultBlockState().setValue(SegmentBlock.PART, part)
                        .setValue(SegmentBlock.SIZE, SegmentBlock.Size.MEDIUM));
            }
        }
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            p.teleportTo(in, a.getX() + 2, a.getY(), a.getZ(), java.util.Set.of(), 0, 0, true);
            p.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            p.gameMode.destroyBlock(a.above(2));
            int items = 0;
            for (ItemEntity e : in.getEntitiesOfClass(ItemEntity.class, new AABB(a).inflate(4), e -> e.getItem().is(ModItems.HEATER_MEDIUM.get()))) {
                items += e.getItem().getCount();
            }
            h.assertTrue(in.getBlockState(b).isAir(), "the touching rod went too");
            h.assertTrue(items == 2, "both heaters dropped, got " + items);
            h.succeed();
        } finally {
            p.level().getServer().getPlayerList().remove(p);
        }
    }

    /** Review F3: a bag whose animal cannot be created (removed mod) stays full instead of losing it. */
    @GameTest(template = EMPTY)
    public static void fishBagStaysFullWhenTheAnimalIsUnknown(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos water = new BlockPos(10, 1, 10);
        h.setBlock(water, Blocks.WATER.defaultBlockState());
        ItemStack bag = new ItemStack(ModItems.FISH_BAG.get());
        CompoundTag stored = new CompoundTag();
        stored.putString("id", "nonexistent:thing");
        CompoundTag data = new CompoundTag();
        data.put(FishBagItem.STORED_KEY, stored);
        bag.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        bag.set(ModDataComponents.BAG_ENTITY.get(), net.minecraft.resources.Identifier.withDefaultNamespace("squid"));
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            BlockPos w = h.absolutePos(water);
            p.teleportTo(level, w.getX() + 0.5, w.getY() + 1.2, w.getZ() + 0.5, java.util.Set.of(), 0, 90, false);
            p.setItemInHand(InteractionHand.MAIN_HAND, bag);
            var result = bag.getItem().use(level, p, InteractionHand.MAIN_HAND);
            h.assertTrue(FishBagItem.isFilled(GameTestCompat.heldAfter(result, bag)), "bag still holds its animal");
            h.succeed();
        } finally {
            p.level().getServer().getPlayerList().remove(p);
        }
    }

    /** Review D4: a player who died in the interior (death screen) does not keep the tank locked. */
    @GameTest(template = EMPTY)
    public static void deadPlayerDoesNotLockTheTank(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 1, 1, 1, 0);
        InteriorRegion r = InteriorGameTests.region(h, c);
        ServerLevel in = InteriorGameTests.interior(h);
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            BlockPos inside = r.toWorld(4, 1, 4);
            p.teleportTo(in, inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5, java.util.Set.of(), 0, 0, true);
            h.assertTrue(InteriorManager.playersInside(h.getLevel().getServer(), c.getTank()).size() == 1, "player inside");
            p.setHealth(0);
            h.assertTrue(InteriorManager.playersInside(h.getLevel().getServer(), c.getTank()).isEmpty(), "a dead player does not count");
            h.succeed();
        } finally {
            p.level().getServer().getPlayerList().remove(p);
        }
    }

    /** Review F6: the hatchery starts a batch only when the whole yield fits (it used food and water for nothing before). */
    @GameTest(template = EMPTY)
    public static void hatcheryWaitsForRoomForAWholeBatch(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos rel = new BlockPos(12, 2, 12);
        h.setBlock(rel, ModBlocks.ARTEMIA_HATCHERY.get().defaultBlockState());
        CultureTubBlockEntity tub = (CultureTubBlockEntity) level.getBlockEntity(h.absolutePos(rel));
        tub.setWater(1000);
        tub.items().setStackInSlot(CultureTubBlockEntity.INPUT, new ItemStack(ModItems.ARTEMIA_CYSTS.get()));
        tub.items().setStackInSlot(CultureTubBlockEntity.BOTTLES, new ItemStack(Items.GLASS_BOTTLE, 8));
        tub.items().setStackInSlot(CultureTubBlockEntity.OUTPUT, new ItemStack(ModItems.ARTEMIA_NAUPLII.get(), CultureTubBlockEntity.OUTPUT_MAX - 1));
        h.assertTrue("full".equals(tub.blocker(level)), "one free place for a batch of " + tub.batchYield() + ": " + tub.blocker(level));
        tub.items().setStackInSlot(CultureTubBlockEntity.OUTPUT, ItemStack.EMPTY);
        tub.items().setStackInSlot(CultureTubBlockEntity.BOTTLES, new ItemStack(Items.GLASS_BOTTLE, 1));
        h.assertTrue("bottles".equals(tub.blocker(level)), "too few bottles for a batch: " + tub.blocker(level));
        h.succeed();
    }

    /**
     * Review D5: an animal of another mod that grew past inhabitants.maxHitbox in the tank leaves in a fish bag in front of
     * it. Here the limit is lowered below a cod instead of letting a young animal grow.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m17_outgrown")
    public static void outgrownAnimalLeavesInABag(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        double saved = ServerConfig.INHABITANTS_MAX_HITBOX.get();
        try {
            TankBlockEntity c = tank(h, 2, 1, 2, 3, 2, 3, 18000);
            ItemStack bucket = new ItemStack(Items.COD_BUCKET);
            var err = TankInhabitants.insert(level, c, bucket, EntityType.COD, null, 0L);
            h.assertTrue(err == null, "cod inserted: " + (err == null ? "" : err.getString()));
            ServerConfig.INHABITANTS_MAX_HITBOX.set(0.4);
            TankInhabitants.reconcile(level, c);
            List<Entity> cods = level.getEntities((Entity) null, TankInhabitants.tankBox(c.getTank()), e -> e.getType() == EntityType.COD && e.isAlive());
            h.assertTrue(cods.isEmpty(), "the cod left the tank");
            h.assertTrue(c.getTank().inhabitants().isEmpty(), "inhabitant list empty");
            List<ItemEntity> bags = level.getEntitiesOfClass(ItemEntity.class, TankInhabitants.tankBox(c.getTank()).inflate(4),
                    e -> e.getItem().is(ModItems.FISH_BAG.get()) && FishBagItem.containedType(e.getItem()) == EntityType.COD);
            h.assertTrue(bags.size() == 1, "one bag with the cod in front of the tank, found " + bags.size());
            h.assertTrue(c.getTank().log().stream().anyMatch(l -> l.key().equals("log.bettacraft.outgrown")), "logged");
            h.succeed();
        } finally {
            ServerConfig.INHABITANTS_MAX_HITBOX.set(saved);
        }
    }

    /** Benchmark (M17): one care step of the small animals in a tank with 200 of them, averaged over 20 steps. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void benchmarkTwoHundredCritters(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 1, 2, 8, 3, 4, 96000);
        CritterSpecies[] kinds = {CritterSpecies.NEOCARIDINA, CritterSpecies.ZEBRA_NERITE};
        for (int i = 0; i < 200; i++) TankCritters.add(level, c, kinds[i % 2], CritterGenes.WILD, i % 2 == 0, kinds[i % 2].juvenileDays() + 1);
        h.assertTrue(TankCritters.alive(c.getTank()) >= 190, "about 200 animals: " + TankCritters.alive(c.getTank()));
        for (int i = 0; i < 3; i++) TankCritters.step(level, c, 20 / 24000.0); // warm up
        long t0 = System.nanoTime();
        int steps = 20;
        for (int i = 0; i < steps; i++) TankCritters.step(level, c, 20 / 24000.0);
        double ms = (System.nanoTime() - t0) / 1e6 / steps;
        com.mojang.logging.LogUtils.getLogger().info("[gametest] benchmark: care step of {} small animals in an 8x3x4 tank: {} ms",
                TankCritters.alive(c.getTank()), String.format(java.util.Locale.ROOT, "%.2f", ms));
        h.assertTrue(ms < 25, "a care step of 200 animals stays well below a tick: " + ms + " ms");
        h.succeed();
    }
}
