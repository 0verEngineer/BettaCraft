package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.decor.DecorItem;
import org.overengineer.bettacraft.decor.DecorType;
import org.overengineer.bettacraft.equipment.SegmentBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorEvents;
import org.overengineer.bettacraft.tank.interior.InteriorFloorBlock;
import org.overengineer.bettacraft.tank.interior.InteriorFloorBlockEntity;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.tank.interior.InteriorSync;
import org.overengineer.bettacraft.tank.interior.InteriorWorldOps;
import org.overengineer.bettacraft.tank.interior.Substrate;

/**
 * M3 interior GameTests: enter/leave, substrate, decoration, equipment, drops on break, water plants, beds, delta
 * sync and the region migration. The GameTest server loads the interior dimension thanks to GameTestServerMixin.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InteriorGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private InteriorGameTests() {}

    static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz, long water) {
        for (int a = 0; a < sx; a++)
            for (int b = 0; b < sy; b++)
                for (int c = 0; c < sz; c++) {
                    BlockPos p = new BlockPos(x + a, y + b, z + c);
                    h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
                }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(x, y, z)));
        if (c == null) throw new GameTestFailure("tank did not form");
        if (water > 0) TankInteractions.changeWater(h.getLevel(), c, water);
        return c;
    }

    static ServerLevel interior(GameTestHelper h) {
        ServerLevel l = InteriorManager.interiorLevel(h.getLevel().getServer());
        if (l == null) throw new GameTestFailure("interior dimension not loaded in the GameTest server");
        return l;
    }

    static InteriorRegion region(GameTestHelper h, TankBlockEntity c) {
        interior(h);
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        h.assertTrue(r.built(), "region built");
        return r;
    }

    @GameTest(template = EMPTY)
    public static void enterAndLeaveBySneaking(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 1, 1, 1, 0);
        InteriorRegion r = region(h, c);
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            Vec3 start = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(5, 1, 5)));
            p.teleportTo(h.getLevel(), start.x, start.y, start.z, java.util.Set.of(), 0, 0, false);
            InteriorManager.enter(p, c.getBlockPos());
            h.assertTrue(InteriorManager.isInterior(p.level()), "player is in the interior dimension");
            h.assertTrue(InteriorManager.regionAt(p.level().getServer(), p.blockPosition()) == r, "player stands in the tank's region");
            boolean left = false;
            for (int i = 0; i < 59; i++) left |= InteriorEvents.sneakTick(p, true);
            h.assertFalse(left, "59 ticks of sneaking are not enough");
            InteriorEvents.sneakTick(p, false);
            for (int i = 0; i < 59; i++) left |= InteriorEvents.sneakTick(p, true);
            h.assertFalse(left, "releasing sneak resets the timer");
            left = InteriorEvents.sneakTick(p, true);
            h.assertTrue(left && p.level() == h.getLevel(), "60 ticks of sneaking bring the player back");
            h.assertTrue(p.position().distanceTo(start) < 0.5, "back at the return point");
            h.succeed();
        } finally {
            p.level().getServer().getPlayerList().remove(p);
        }
    }

    @GameTest(template = EMPTY)
    public static void lockedTankCannotBeEntered(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 1, 1, 1, 0);
        region(h, c);
        c.getTank().setOwner(java.util.UUID.randomUUID());
        c.getTank().setLocked(true);
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            InteriorManager.enter(p, c.getBlockPos());
            h.assertFalse(InteriorManager.isInterior(p.level()), "locked tank: stranger stays outside");
            h.succeed();
        } finally {
            p.level().getServer().getPlayerList().remove(p);
        }
    }

    @GameTest(template = EMPTY)
    public static void substrateCostsAndReturnsBlock(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 1, 1, 1, 0);
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        BlockPos floor = new BlockPos(r.originX() + 3, InteriorRegion.FLOOR_Y, r.originZ() + 4);
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            p.teleportTo(in, floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 0.5, java.util.Set.of(), 0, 0, true);
            p.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SAND, 10));
            h.assertTrue(Substrate.use(p, InteractionHand.MAIN_HAND, floor), "substrate interaction handled");
            BlockState s = in.getBlockState(floor);
            h.assertTrue(s.is(ModBlocks.INTERIOR_FLOOR.get()) && s.getValue(InteriorFloorBlock.SUBSTRATE), "floor carries a substrate");
            h.assertTrue(in.getBlockEntity(floor) instanceof InteriorFloorBlockEntity be && be.substrate().is(Blocks.SAND) && be.paid(), "sand, paid");
            h.assertTrue(p.getMainHandItem().getCount() == 9, "one block used");
            h.assertTrue(in.getBlockState(floor.above()).isAir(), "no block placed on top");
            // replace with gravel: sand comes back
            p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GRAVEL, 1));
            Substrate.use(p, InteractionHand.OFF_HAND, floor);
            h.assertTrue(in.getBlockEntity(floor) instanceof InteriorFloorBlockEntity be2 && be2.substrate().is(Blocks.GRAVEL), "now gravel");
            h.assertTrue(p.getInventory().countItem(Items.SAND) == 10, "sand returned on replace");
            // glass panes and chests are no substrate
            p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.CHEST, 1));
            Substrate.use(p, InteractionHand.OFF_HAND, floor);
            h.assertTrue(p.getOffhandItem().getCount() == 1, "chest refused");
            // empty hand removes it
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            p.getInventory().clearContent();
            Substrate.use(p, InteractionHand.MAIN_HAND, floor);
            h.assertFalse(in.getBlockState(floor).getValue(InteriorFloorBlock.SUBSTRATE), "substrate removed");
            h.assertTrue(p.getInventory().countItem(Items.GRAVEL) == 1, "gravel returned");
            h.assertTrue(r.equipment().substrateCount() == 0, "substrate counter back to 0");
            h.succeed();
        } finally {
            p.level().getServer().getPlayerList().remove(p);
        }
    }

    @GameTest(template = EMPTY)
    public static void decorPlacedInWorldAndConsumed(GameTestHelper h) {
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            p.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DECOR.get(DecorType.STONES).get(), 2));
            BlockPos ground = h.absolutePos(new BlockPos(8, 0, 8));
            h.getLevel().setBlockAndUpdate(ground, Blocks.STONE.defaultBlockState());
            var res = p.getMainHandItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false)));
            h.assertTrue(res.consumesAction(), "placement succeeded: " + res);
            h.assertTrue(p.getMainHandItem().getCount() == 1, "one decor item used");
            int n = 0;
            for (BlockPos q : BlockPos.betweenClosed(ground.offset(-3, 1, -3), ground.offset(3, 3, 3))) {
                BlockState s = h.getLevel().getBlockState(q);
                if (s.is(Blocks.COBBLESTONE) || s.is(Blocks.ANDESITE) || s.is(Blocks.TUFF) || s.is(Blocks.MOSSY_COBBLESTONE)) n++;
            }
            h.assertTrue(n == 7, "7 stone blocks of the structure placed, found " + n);
            // a second placement into the same spot is blocked and keeps the item
            res = p.getMainHandItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false)));
            h.assertTrue(p.getMainHandItem().getCount() == 1, "blocked placement keeps the item");
            h.succeed();
        } finally {
            p.level().getServer().getPlayerList().remove(p);
        }
    }

    @GameTest(template = EMPTY)
    public static void decorPlacedInInteriorIsSynced(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 1, 1, 1, 0);
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        BlockPos base = r.toWorld(8, 0, 8);
        int v0 = InteriorSync.version(r.id());
        InteriorSync.markAllDirty(r.id());
        InteriorSync.flush(h.getLevel().getServer());
        h.assertTrue(DecorItem.place(in, DecorType.CASTLE, base, net.minecraft.world.level.block.Rotation.NONE, null) == null, "castle placed");
        int n = 0;
        for (BlockPos q : BlockPos.betweenClosed(base.offset(-4, 0, -4), base.offset(4, 12, 4))) if (!in.getBlockState(q).isAir()) n++;
        h.assertTrue(n > 150, "castle blocks in the interior: " + n);
        // too close to the wall: does not fit
        h.assertTrue(DecorItem.place(in, DecorType.CASTLE, r.toWorld(0, 0, 0), net.minecraft.world.level.block.Rotation.NONE, null) != null,
                "castle at the edge does not fit");
        h.assertTrue(InteriorSync.version(r.id()) > v0, "version advanced");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void deltaSyncSendsOnlyChangedSections(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 1, 2, 0);
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        // first a full encode (as if a client asked), then one block change
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            p.teleportTo(h.getLevel(), c.getBlockPos().getX(), c.getBlockPos().getY() + 2, c.getBlockPos().getZ(), java.util.Set.of(), 0, 0, false);
            InteriorWorldOps.fill(in, r.originX(), InteriorRegion.BASE_Y, r.originZ(), r.originX() + r.sizeX() - 1, InteriorRegion.BASE_Y,
                    r.originZ() + r.sizeZ() - 1, Blocks.SAND.defaultBlockState(), new it.unimi.dsi.fastutil.longs.LongOpenHashSet());
            InteriorSync.markAllDirty(r.id());
            int full = InteriorSync.prepareFull(h.getLevel().getServer(), r.id());
            int v = InteriorSync.version(r.id());
            in.setBlockAndUpdate(r.toWorld(5, 3, 5), Blocks.STONE.defaultBlockState());
            InteriorSync.flush(h.getLevel().getServer());
            h.assertTrue(InteriorSync.version(r.id()) == v + 1, "delta advanced the version by one");
            h.assertTrue(InteriorSync.lastDeltaBytes > 0 && InteriorSync.lastDeltaBytes < 200, "delta is tiny: " + InteriorSync.lastDeltaBytes + " (full " + full + ")");
            h.assertTrue(InteriorSync.syncedBlocks(r.id()) == r.sizeX() * r.sizeZ() + 1, "counts follow the delta");
            h.succeed();
        } finally {
            p.level().getServer().getPlayerList().remove(p);
        }
    }

    @GameTest(template = EMPTY)
    public static void heaterRodInInteriorHeats(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 2, 2, 4000); // half full: water up to 1 block = 16 interior blocks
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        BlockPos low = r.toWorld(4, 0, 4), high = r.toWorld(10, 20, 10);
        for (BlockPos base : List.of(low, high)) {
            for (int y = 0; y < 6; y++) {
                SegmentBlock.Part part = y == 0 ? SegmentBlock.Part.ANCHOR : y == 5 ? SegmentBlock.Part.TOP : SegmentBlock.Part.MIDDLE;
                in.setBlockAndUpdate(base.above(y), ModBlocks.HEATER_SEGMENT.get().defaultBlockState().setValue(SegmentBlock.PART, part)
                        .setValue(SegmentBlock.SIZE, SegmentBlock.Size.MEDIUM));
            }
        }
        h.assertTrue(r.equipment().heaters().size() == 2, "two heaters counted");
        h.assertTrue(TankCare.interiorHeaterBlocks(h.getLevel(), c.getTank()) == 32, "only the submerged one heats (32), got "
                + TankCare.interiorHeaterBlocks(h.getLevel(), c.getTank()));
        // breaking a segment by a player removes the whole rod and gives the item back
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            p.teleportTo(in, low.getX() + 2, low.getY(), low.getZ(), java.util.Set.of(), 0, 0, true);
            p.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            p.gameMode.destroyBlock(low.above(3));
            h.assertTrue(in.getBlockState(low).isAir() && in.getBlockState(low.above(5)).isAir(), "whole rod removed");
            h.assertTrue(!in.getEntitiesOfClass(ItemEntity.class, new AABB(low).inflate(4), e -> e.getItem().is(ModItems.HEATER_MEDIUM.get())).isEmpty(),
                    "heater item dropped");
            h.assertTrue(r.equipment().heaters().size() == 1, "one heater left");
            h.succeed();
        } finally {
            p.level().getServer().getPlayerList().remove(p);
        }
    }

    @GameTest(template = EMPTY)
    public static void interiorDropsWhenTankBreaks(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 1, 1, 1, 0);
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        in.setBlockAndUpdate(r.toWorld(1, 0, 1), Blocks.COBBLESTONE.defaultBlockState());
        in.setBlockAndUpdate(r.toWorld(2, 0, 1), Blocks.KELP_PLANT.defaultBlockState());
        BlockPos chest = r.toWorld(3, 0, 3);
        in.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
        if (in.getBlockEntity(chest) instanceof ChestBlockEntity be) be.setItem(0, new ItemStack(Items.DIAMOND, 3));
        Substrate.set(in, new BlockPos(r.originX() + 5, InteriorRegion.FLOOR_Y, r.originZ() + 5), Blocks.RED_SAND.defaultBlockState(), true);
        Substrate.set(in, new BlockPos(r.originX() + 6, InteriorRegion.FLOOR_Y, r.originZ() + 5), Blocks.GRAVEL.defaultBlockState(), false);
        BlockPos tankPos = c.getBlockPos();
        h.getLevel().setBlockAndUpdate(tankPos, Blocks.AIR.defaultBlockState());
        List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(tankPos).inflate(2));
        java.util.function.Predicate<net.minecraft.world.item.Item> has = it -> drops.stream().anyMatch(e -> e.getItem().is(it));
        h.assertTrue(has.test(Items.COBBLESTONE), "cobblestone dropped");
        h.assertTrue(has.test(Items.KELP), "kelp plant drops kelp");
        h.assertTrue(has.test(Items.CHEST) && has.test(Items.DIAMOND), "chest and its contents dropped");
        h.assertTrue(has.test(Items.RED_SAND), "paid substrate dropped");
        h.assertFalse(has.test(Items.GRAVEL), "free substrate is not duplicated");
        h.assertTrue(InteriorRegions.get(h.getLevel().getServer()).get(r.id()) == null, "region freed");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void manyDropsArePackedIntoChests(GameTestHelper h) {
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (int i = 0; i < 40; i++) items.add(new ItemStack(Items.STONE, 64));
        items.add(new ItemStack(Items.DIRT, 5));
        BlockPos at = h.absolutePos(new BlockPos(10, 2, 10));
        org.overengineer.bettacraft.tank.interior.InteriorDrops.drop(h.getLevel(), at, items);
        List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(2));
        h.assertTrue(drops.size() == 2 && drops.stream().allMatch(e -> e.getItem().is(Items.CHEST)
                && e.getItem().has(net.minecraft.core.component.DataComponents.CONTAINER)), "41 stacks -> 2 chests with contents");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void waterPlantsStayDryAndAlive(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 1, 1, 1, 0);
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        BlockPos grass = r.toWorld(4, 1, 4);
        in.setBlockAndUpdate(grass.below(), Blocks.SAND.defaultBlockState());
        in.setBlockAndUpdate(grass, Blocks.SEAGRASS.defaultBlockState());
        BlockPos coral = r.toWorld(8, 1, 8);
        in.setBlockAndUpdate(coral.below(), Blocks.SAND.defaultBlockState());
        in.setBlockAndUpdate(coral, Blocks.TUBE_CORAL.defaultBlockState());
        // provoke updates: neighbour change next to the seagrass schedules its (implicit) water tick
        in.setBlockAndUpdate(grass.east(), Blocks.STONE.defaultBlockState());
        in.setBlockAndUpdate(grass.east(), Blocks.AIR.defaultBlockState());
        h.runAfterDelay(100, () -> {
            h.assertTrue(in.getBlockState(grass).is(Blocks.SEAGRASS), "seagrass survives");
            h.assertTrue(in.getBlockState(coral).is(Blocks.TUBE_CORAL), "coral does not dry out");
            for (Direction d : Direction.Plane.HORIZONTAL) {
                h.assertTrue(in.getFluidState(grass.relative(d)).isEmpty(), "no water flows out of the seagrass");
            }
            h.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void bedsDoNotExplodeInside(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 1, 1, 1, 0);
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        BlockPos foot = r.toWorld(5, 0, 5);
        in.setBlockAndUpdate(foot, Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT));
        in.setBlockAndUpdate(foot.north(), Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD));
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            p.teleportTo(in, foot.getX() + 0.5, foot.getY(), foot.getZ() + 2.5, java.util.Set.of(), 0, 0, true);
            p.gameMode.useItemOn(p, in, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(foot), Direction.UP, foot, false));
            h.assertTrue(in.getBlockState(foot).is(Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED)) && in.getBlockState(foot.north()).is(Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED)), "bed still there (no explosion)");
            h.succeed();
        } finally {
            p.level().getServer().getPlayerList().remove(p);
        }
    }

    @GameTest(template = EMPTY)
    public static void migratedRegionRescansEquipment(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 1, 1, 1, 0);
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        // written directly into the section (no block update), like a world from M2 with a heater placed by hand
        InteriorWorldOps.fill(in, r.originX() + 2, InteriorRegion.BASE_Y, r.originZ() + 2, r.originX() + 2, InteriorRegion.BASE_Y, r.originZ() + 2,
                ModBlocks.HEATER_SEGMENT.get().defaultBlockState().setValue(SegmentBlock.SIZE, SegmentBlock.Size.LARGE),
                new it.unimi.dsi.fastutil.longs.LongOpenHashSet());
        h.assertTrue(r.equipment().heaters().isEmpty(), "not seen without update");
        // v1 SavedData -> v2 marks every region for a rescan
        CompoundTag old = new CompoundTag();
        old.putInt("schema", 1);
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        CompoundTag reg = r.save();
        reg.remove("equipment");
        list.add(reg);
        old.put("regions", list);
        CompoundTag migrated = org.overengineer.bettacraft.data.schema.Schemas.INTERIOR_REGIONS.migrate(old);
        InteriorRegion loaded = InteriorRegion.load(migrated.getListOrEmpty("regions").getCompoundOrEmpty(0));
        h.assertTrue(loaded.equipment().needsScan(), "migrated region needs a rescan");
        r.equipment().markNeedsScan();
        h.assertTrue(InteriorManager.equipment(h.getLevel().getServer(), r).heaters().get(net.minecraft.core.BlockPos.asLong(2, 0, 2)) == 64,
                "rescan finds the large heater");
        h.succeed();
    }
}
