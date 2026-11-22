package org.overengineer.bettacraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.CabinetFront;
import org.overengineer.bettacraft.equipment.CabinetSprings;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankPower;
import org.overengineer.bettacraft.winding.WindSpring;
import org.overengineer.bettacraft.winding.Winding;

/**
 * M62 part 2: the under-cabinet over the whole area. Cabinets join to the back: the front row shows the fronts, every cabinet
 * with another one in front of it a plain carcass ({@link CabinetBlock#COVERED}), a full 4 x 4 as well as a ring around normal
 * blocks under a 10 x 5 tank; a cabinet placed against another takes its facing; with {@code features.equipment.cabinetArea}
 * off nothing is covered. Question A2: one spring store for the whole under-cabinet, also for two cabinets under a long tank
 * that do not touch, the cabinets joined beyond the tank too; one click winds the store; the springs of an older world are
 * the store (nothing lost); a broken cabinet hands its share over; the tank draws from the store evenly; switched off every
 * cabinet winds its own spring.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CabinetAreaGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private CabinetAreaGameTests() {}

    private static BlockState cabinet(Direction facing) {
        return ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, facing);
    }

    private static BlockState at(GameTestHelper h, int x, int z) {
        return h.getBlockState(new BlockPos(x, 1, z));
    }

    private static CabinetBlockEntity cab(GameTestHelper h, int x, int z) {
        if (!(h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(x, 1, z))) instanceof CabinetBlockEntity c)) throw new org.overengineer.bettacraft.gametest.framework.GameTestFailure("no cabinet at " + x + " " + z);
        return c;
    }

    /** A 4 x 4 of cabinets and a ring of cabinets around stone under a 10 x 5 area: the fronts only in the front row. */
    @GameTest(template = EMPTY, batch = "bettacraft_m62_cabinet_area", timeoutTicks = 100)
    public static void cabinetsJoinToTheBack(GameTestHelper h) {
        // 4 x 4, facing north (the front row is z = 2)
        for (int x = 2; x <= 5; x++) for (int z = 2; z <= 5; z++) h.setBlock(new BlockPos(x, 1, z), cabinet(Direction.NORTH));
        // 10 x 5 ring around stone, facing north (front row z = 2)
        for (int x = 8; x <= 17; x++) for (int z = 2; z <= 6; z++) {
            boolean ring = x == 8 || x == 17 || z == 2 || z == 6;
            h.setBlock(new BlockPos(x, 1, z), ring ? cabinet(Direction.NORTH) : Blocks.STONE.defaultBlockState());
        }
        h.runAfterDelay(2, () -> {
            for (int x = 2; x <= 5; x++) for (int z = 2; z <= 5; z++) {
                BlockState s = at(h, x, z);
                h.assertTrue(s.getValue(CabinetBlock.COVERED) == (z > 2), "4x4 " + x + "/" + z + " covered " + s.getValue(CabinetBlock.COVERED));
            }
            // the front row of the 4 x 4: one counter with drawers at the ends
            h.assertTrue(at(h, 2, 2).getValue(CabinetBlock.FRONT) == CabinetFront.DRAWERS && at(h, 5, 2).getValue(CabinetBlock.FRONT) == CabinetFront.DRAWERS,
                    "front row of 4: drawers at the ends");
            // the inner and back blocks still join left and right (the counter top runs through)
            h.assertTrue(at(h, 3, 4).getValue(CabinetBlock.LEFT) && at(h, 3, 4).getValue(CabinetBlock.RIGHT), "inner block joined on both sides");
            for (int x = 8; x <= 17; x++) {
                h.assertTrue(!at(h, x, 2).getValue(CabinetBlock.COVERED), "ring front row " + x + " shows its front");
                h.assertTrue(at(h, x, 2).getValue(CabinetBlock.FRONT) == CabinetFront.layout(10, 17 - x), "ring front row " + x + ": layout of 10");
            }
            for (int z = 3; z <= 6; z++) {
                h.assertTrue(at(h, 8, z).getValue(CabinetBlock.COVERED) && at(h, 17, z).getValue(CabinetBlock.COVERED), "the side columns are carcass at z " + z);
            }
            // the outer sides overhang (not joined outwards), the front row's ends as well
            h.assertTrue(!at(h, 17, 4).getValue(CabinetBlock.LEFT) && !at(h, 8, 4).getValue(CabinetBlock.RIGHT), "the side columns are free outwards");
            // broken in front: the block behind shows its front again
            h.setBlock(new BlockPos(3, 1, 2), Blocks.AIR.defaultBlockState());
            h.assertTrue(!at(h, 3, 3).getValue(CabinetBlock.COVERED), "a gap in the front row opens the front behind it");
            h.succeed();
        });
    }

    /** A cabinet placed against the side or back of another takes its facing (otherwise towards the player). */
    @GameTest(template = EMPTY, batch = "bettacraft_m62_cabinet_area", timeoutTicks = 100)
    public static void placedAgainstACabinetTakesItsFacing(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        h.setBlock(new BlockPos(4, 1, 4), cabinet(Direction.NORTH));
        ServerPlayer p = MockPlayers.serverPlayer(h);
        p.setYRot(90F); // looking west: a cabinet of its own would face east
        BlockPos target = h.absolutePos(new BlockPos(4, 1, 4));
        ItemStack stack = new ItemStack(ModItems.CABINET.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        // clicked on the back (south face) of the cabinet
        BlockPlaceContext back = new BlockPlaceContext(p, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(target).add(0, 0, 0.5), Direction.SOUTH, target, false));
        h.assertTrue(CabinetBlock.placedFacing(back) == Direction.NORTH, "against the back: " + CabinetBlock.placedFacing(back));
        // clicked on the ground next to it: its own facing
        BlockPos ground = h.absolutePos(new BlockPos(7, 0, 7));
        BlockPlaceContext floor = new BlockPlaceContext(p, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false));
        h.assertTrue(CabinetBlock.placedFacing(floor) == Direction.EAST, "on the ground: " + CabinetBlock.placedFacing(floor));
        boolean was = Features.own(Feature.CABINET_AREA);
        try {
            Features.set(Feature.CABINET_AREA, false);
            h.assertTrue(CabinetBlock.placedFacing(back) == Direction.EAST, "switch off: its own facing");
        } finally {
            Features.set(Feature.CABINET_AREA, was);
        }
        h.succeed();
    }

    /** {@code features.equipment.cabinetArea} off: nothing is covered, every cabinet shows its front; on again: the area. */
    @GameTest(template = EMPTY, batch = "bettacraft_m62_cabinet_area_off", timeoutTicks = 100)
    public static void areaSwitchedOff(GameTestHelper h) {
        boolean was = Features.own(Feature.CABINET_AREA);
        try {
            for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++) h.setBlock(new BlockPos(x, 1, z), cabinet(Direction.NORTH));
            h.assertTrue(at(h, 3, 3).getValue(CabinetBlock.COVERED), "on: covered");
            Features.set(Feature.CABINET_AREA, false);
            CabinetBlockEntity.refreshAllFronts();
            for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++) h.assertTrue(!at(h, x, z).getValue(CabinetBlock.COVERED), "off: no carcass at " + x + "/" + z);
            Features.set(Feature.CABINET_AREA, true);
            CabinetBlockEntity.refreshAllFronts();
            h.assertTrue(at(h, 3, 3).getValue(CabinetBlock.COVERED) && !at(h, 3, 2).getValue(CabinetBlock.COVERED), "on again");
        } finally {
            Features.set(Feature.CABINET_AREA, was);
            CabinetBlockEntity.refreshAllFronts();
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ A2: the shared spring

    private static void power(boolean on) {
        ServerConfig.POWER_FILTER.set(on);
        Features.set(Feature.POWER, true);
        Features.set(Feature.WINDING, true);
    }

    /**
     * A 4 x 1 tank with a cabinet at each end (they do not touch) and one more joined to the right one beyond the tank: one
     * store of three; a click at one winds all of them evenly; the store's capacity is the sum.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m62_cabinet_spring", timeoutTicks = 100)
    public static void separateCabinetsUnderOneTankShareTheSpring(GameTestHelper h) {
        boolean filter = ServerConfig.POWER_FILTER.get(), pw = Features.own(Feature.POWER), wind = Features.own(Feature.WINDING), shared = Features.own(Feature.CABINET_SHARED_SPRING);
        try {
            power(true);
            Features.set(Feature.CABINET_SHARED_SPRING, true);
            TankBlockEntity c = AerationGameTests.tank(h, 2, 2, 2, 4, 1, 1);
            h.setBlock(new BlockPos(2, 1, 2), cabinet(Direction.NORTH));
            h.setBlock(new BlockPos(5, 1, 2), cabinet(Direction.NORTH));
            h.setBlock(new BlockPos(6, 1, 2), cabinet(Direction.NORTH)); // beyond the tank, joined to the right one
            TankEquipment.invalidate(c);
            CabinetBlockEntity a = cab(h, 2, 2), b = cab(h, 5, 2), x = cab(h, 6, 2);
            WindSpring store = a.windStore();
            h.assertTrue(store instanceof CabinetSprings.Store s && s.size() == 3, "one store of 3: " + store);
            long one = a.windSpring().capacity();
            h.assertTrue(store.capacity() == 3 * one, "capacity grows with every block");
            h.assertTrue(Winding.wind(h.getLevel(), b.getBlockPos(), null) == Winding.Result.WOUND, "the crank winds at any of them");
            long step = a.windSpring().step();
            h.assertTrue(a.windStore().ticks() == step, "one click = one block's step: " + a.windStore().ticks());
            long lo = Math.min(a.windSpring().ticks(), Math.min(b.windSpring().ticks(), x.windSpring().ticks()));
            long hi = Math.max(a.windSpring().ticks(), Math.max(b.windSpring().ticks(), x.windSpring().ticks()));
            h.assertTrue(hi - lo <= 1, "spread evenly: " + a.windSpring().ticks() + "/" + b.windSpring().ticks() + "/" + x.windSpring().ticks());
            // the percent shown is that of the store, the same at every cabinet
            h.assertTrue(Winding.shown(a) == Winding.shown(x), "every cabinet shows the store");
            // the tank draws from the store evenly (a filter in a cabinet, no FE)
            a.items().setStackInSlot(CabinetBlockEntity.FILTER_SLOT, new ItemStack(ModItems.EXTERNAL_FILTER.get()));
            TankEquipment.invalidate(c);
            c.getTank().setPowerBuffer(0);
            a.windStore().set(a.windStore().capacity());
            TankPower.step(h.getLevel(), c, 20);
            h.assertTrue(c.filterPowered(), "the filter runs on the store");
            h.assertTrue(a.windStore().ticks() == 3 * one - 20, "20 device-ticks from the store: " + (3 * one - a.windStore().ticks()));
            lo = Math.min(a.windSpring().ticks(), Math.min(b.windSpring().ticks(), x.windSpring().ticks()));
            hi = Math.max(a.windSpring().ticks(), Math.max(b.windSpring().ticks(), x.windSpring().ticks()));
            h.assertTrue(hi - lo <= 1, "still even after the drain");
        } finally {
            ServerConfig.POWER_FILTER.set(filter);
            Features.set(Feature.POWER, pw);
            Features.set(Feature.WINDING, wind);
            Features.set(Feature.CABINET_SHARED_SPRING, shared);
        }
        h.succeed();
    }

    /** The per-block springs of an older world form the store (the sum stays); a broken cabinet hands its share over. */
    @GameTest(template = EMPTY, batch = "bettacraft_m62_cabinet_spring", timeoutTicks = 100)
    public static void oldSpringsAndBreaking(GameTestHelper h) {
        boolean filter = ServerConfig.POWER_FILTER.get(), pw = Features.own(Feature.POWER), wind = Features.own(Feature.WINDING), shared = Features.own(Feature.CABINET_SHARED_SPRING);
        try {
            power(true);
            Features.set(Feature.CABINET_SHARED_SPRING, true);
            for (int x = 2; x <= 4; x++) h.setBlock(new BlockPos(x, 1, 2), cabinet(Direction.NORTH));
            CabinetBlockEntity a = cab(h, 2, 2), b = cab(h, 3, 2), d = cab(h, 4, 2);
            long cap = a.windSpring().capacity();
            // as an older world left them: one full, one half, one empty (each its own spring)
            a.windSpring().set(cap);
            b.windSpring().set(cap / 2);
            d.windSpring().set(0);
            long sum = cap + cap / 2;
            h.assertTrue(a.windStore().ticks() == sum, "nothing lost: the store is the sum " + a.windStore().ticks());
            CabinetSprings.level(CabinetSprings.store(a));
            h.assertTrue(a.windSpring().ticks() + b.windSpring().ticks() + d.windSpring().ticks() == sum, "levelling keeps the sum");
            h.assertTrue(Math.abs(a.windSpring().ticks() - d.windSpring().ticks()) <= 1, "levelled");
            // broken: its share goes to the other two (they have room)
            long before = a.windStore().ticks();
            h.getLevel().destroyBlock(d.getBlockPos(), false);
            h.assertTrue(a.windStore().ticks() == before, "the share of the broken cabinet stays: " + a.windStore().ticks() + " of " + before);
            // full store, one broken: the rest stays full, the part that does not fit goes with the block
            a.windStore().set(a.windStore().capacity());
            h.getLevel().destroyBlock(b.getBlockPos(), false);
            h.assertTrue(a.windSpring().full(), "the last one is full");
        } finally {
            ServerConfig.POWER_FILTER.set(filter);
            Features.set(Feature.POWER, pw);
            Features.set(Feature.WINDING, wind);
            Features.set(Feature.CABINET_SHARED_SPRING, shared);
        }
        h.succeed();
    }

    /** {@code features.power.cabinetSharedSpring} off: every cabinet winds and shows its own spring. */
    @GameTest(template = EMPTY, batch = "bettacraft_m62_cabinet_spring_off", timeoutTicks = 100)
    public static void sharedSpringSwitchedOff(GameTestHelper h) {
        boolean filter = ServerConfig.POWER_FILTER.get(), pw = Features.own(Feature.POWER), wind = Features.own(Feature.WINDING), shared = Features.own(Feature.CABINET_SHARED_SPRING);
        try {
            power(true);
            Features.set(Feature.CABINET_SHARED_SPRING, false);
            h.setBlock(new BlockPos(2, 1, 2), cabinet(Direction.NORTH));
            h.setBlock(new BlockPos(3, 1, 2), cabinet(Direction.NORTH));
            CabinetBlockEntity a = cab(h, 2, 2), b = cab(h, 3, 2);
            h.assertTrue(a.windStore() == a.windSpring(), "its own spring");
            Winding.wind(h.getLevel(), a.getBlockPos(), null);
            h.assertTrue(a.windSpring().ticks() > 0 && b.windSpring().ticks() == 0, "only the cabinet clicked");
        } finally {
            ServerConfig.POWER_FILTER.set(filter);
            Features.set(Feature.POWER, pw);
            Features.set(Feature.WINDING, wind);
            Features.set(Feature.CABINET_SHARED_SPRING, shared);
        }
        h.succeed();
    }
}
