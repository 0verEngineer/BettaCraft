package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.filter.FilterChamberBlockEntity;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity;
import org.overengineer.bettacraft.filter.MultiblockFilter;
import org.overengineer.bettacraft.plumbing.PipeBundleBlock;
import org.overengineer.bettacraft.plumbing.PipeSlots;
import org.overengineer.bettacraft.plumbing.Pipes;
import org.overengineer.bettacraft.plumbing.LevelSensorBlockEntity;
import org.overengineer.bettacraft.plumbing.ReservoirBlockEntity;
import org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity;
import org.overengineer.bettacraft.plumbing.WaterPumpBlock;
import org.overengineer.bettacraft.plumbing.WaterPumpBlockEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M40 part b client smoke ({@code -Psmoketestm40b}; background window): a row of pipe bundles carrying all four lines of a
 * multiblock filter (intake blue, return orange, fresh water green, waste water brown) between a tank and the filter; at
 * both ends the lines leave the bundles as single pipes: to the tank, to a water pump with its reservoir, to a waste
 * reservoir, and to the filter's connectors and its fresh and waste water ports. A level sensor and a water change
 * controller sit on the filter. Screenshots {@code bettacraft_m40b_*}: the whole run, the end of the bundles, the filter
 * with its water ports, the filter screen.
 */
final class DevClientSmokeM40b {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos base, head;

    DevClientSmokeM40b() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(200, () -> look(base.getX() + 6.5, base.getY() + 5.0, base.getZ() + 13.5, 180F, 32F));
        then(200, () -> shot("bundles"));
        then(1, () -> look(base.getX() + 10.0, base.getY() + 3.4, base.getZ() + 9.8, 150F, 42F));
        then(150, () -> shot("bundle_end"));
        then(1, () -> look(base.getX() + 15.5, base.getY() + 2.6, base.getZ() + 9.5, 130F, 22F));
        then(150, () -> shot("filter_water"));
        then(1, () -> look(base.getX() + 1.6, base.getY() + 3.4, base.getZ() + 9.6, 210F, 42F));
        then(150, () -> shot("bundle_start"));
        then(1, () -> server(p -> p.teleportTo(p.level().getServer().overworld(), head.getX() + 3.5, head.getY() - 1.0, head.getZ() + 0.5, java.util.Set.of(), 90F, 10F, true)));
        then(80, () -> {
            mc.options.hideGui = false;
            mc.setScreen(new org.overengineer.bettacraft.client.screen.MultiblockFilterScreen(head));
        });
        then(30, () -> shot("screen"));
        then(1, () -> mc.setScreen(null));
    }

    // ------------------------------------------------------------------ the scene

    private static TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankData t = TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        t.setFront(Direction.SOUTH);
        t.setSignVisible(false);
        t.setOwner(p.getUUID());
        TankInteractions.changeWater(level, c, t.capacityMb(), 0F);
        t.setWaterTemp(25F);
        t.setTargetTemp(25F);
        c.markTankChanged();
        return c;
    }

    private static void line(ServerLevel level, Block b, BlockPos base, int[]... path) {
        for (int[] q : path) level.setBlock(base.offset(q[0], q[1], q[2]), b.defaultBlockState(), Block.UPDATE_ALL);
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        base = p.blockPosition().offset(-10, 2, 200);
        for (BlockPos q : BlockPos.betweenClosed(base.offset(-3, -1, -2), base.offset(18, 6, 12)))
            level.setBlock(q, q.getY() == base.getY() - 1 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
        TankBlockEntity a = tank(level, p, base.offset(0, 0, 2), 3, 2, 2);
        // the filter east: chambers (11,0,6) and (11,1,6), head above; inlet and outlet west of them, the fresh water port north,
        // the waste water port south; the level sensor and the water change controller east of the chambers
        for (int y = 0; y < 2; y++) {
            level.setBlock(base.offset(11, y, 6), ModBlocks.FILTER_CHAMBER.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(base.offset(11, y, 6)) instanceof FilterChamberBlockEntity ch)
                ch.setMedium(new ItemStack(y == 0 ? ModItems.CERAMIC_RINGS.get() : ModItems.FILTER_FOAM.get()));
        }
        head = base.offset(11, 2, 6);
        level.setBlock(head, ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(base.offset(10, 0, 6), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
        level.setBlock(base.offset(10, 1, 6), ModBlocks.FILTER_OUTLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
        level.setBlock(base.offset(11, 0, 5), ModBlocks.STAINLESS_STEEL_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(base.offset(11, 0, 7), ModBlocks.STAINLESS_STEEL_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(base.offset(12, 0, 6), ModBlocks.LEVEL_SENSOR.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(base.offset(12, 1, 6), ModBlocks.WATER_CHANGE_CONTROLLER.get().defaultBlockState(), Block.UPDATE_ALL);
        // the west end: water pump (pushing south) on its reservoir, the waste reservoir
        level.setBlock(base.offset(3, 0, 3), ModBlocks.RESERVOIR.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(base.offset(3, 0, 4), ModBlocks.WATER_PUMP.get().defaultBlockState().setValue(WaterPumpBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
        level.setBlock(base.offset(3, 0, 8), ModBlocks.RESERVOIR.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(base.offset(3, 0, 3)) instanceof ReservoirBlockEntity r)
            r.water().fill(new org.overengineer.bettacraft.platform.fluid.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 12000), IFluidHandler.FluidAction.EXECUTE);
        Block in = ModBlocks.STAINLESS_STEEL_PIPE.get(), out = ModBlocks.STAINLESS_STEEL_PIPE.get(), fresh = ModBlocks.STAINLESS_STEEL_PIPE.get(), waste = ModBlocks.STAINLESS_STEEL_PIPE.get();
        // the intake line: from the tank's front glass down the run to the inlet
        line(level, in, base, new int[]{2, 0, 4}, new int[]{2, 0, 5}, new int[]{2, 0, 6});
        for (int x = 3; x <= 9; x++) line(level, in, base, new int[]{x, 0, 6});
        // the return line: tank, over the run, to the outlet
        line(level, out, base, new int[]{2, 1, 4}, new int[]{2, 1, 5}, new int[]{2, 1, 6}, new int[]{3, 1, 6});
        line(level, out, base, new int[]{9, 1, 6}, new int[]{8, 1, 6});
        // fresh water: pump to the run, the run to the fresh water port
        line(level, fresh, base, new int[]{3, 0, 5});
        line(level, fresh, base, new int[]{11, 0, 4}, new int[]{10, 0, 4}, new int[]{9, 0, 4}, new int[]{8, 0, 4}, new int[]{8, 0, 5});
        // waste water: the waste water port to the run, the run to the waste reservoir
        line(level, waste, base, new int[]{3, 0, 7});
        line(level, waste, base, new int[]{11, 0, 8}, new int[]{10, 0, 8}, new int[]{9, 0, 8}, new int[]{8, 0, 8}, new int[]{8, 0, 7});
        // the bundles: (3..8, 0, 6) carry all four lines
        for (int x = 3; x <= 8; x++)
            for (int l = 0; l < 3; l++) PipeBundleBlock.addPipe(level, base.offset(x, 0, 6), true, null, null);
        for (int x = 3; x <= 8; x++) {
            int lines = PipeSlots.count(Pipes.slotsAt(level, base.offset(x, 0, 6)));
            if (lines != 4) LOGGER.error("[smoketest] M40b bundle at x {} carries {}", x, lines);
        }
        MultiblockFilter.Structure s = MultiblockFilter.scan(level, head);
        List<MultiblockFilter.Link> links = MultiblockFilter.links(level, s);
        LOGGER.info("[smoketest] M40b filter {}: {} tanks", s.problem(), links.size());
        if (links.size() != 1) LOGGER.error("[smoketest] M40b the filter does not reach its tank and ports");
        org.overengineer.bettacraft.gametest.FilterLinesGameTests.guardAll(level, head);
        if (level.getBlockEntity(head) instanceof FilterPumpHeadBlockEntity h) {
            // fresh water: the pump fills the port through the bundles, the sensor on the filter tops the tank off from it
            if (level.getBlockEntity(base.offset(3, 0, 4)) instanceof WaterPumpBlockEntity pump) for (int i = 0; i < 3; i++) pump.pump(level, pump.getBlockPos(), pump.getBlockState());
            TankInteractions.changeWater(level, a, -800);
            int topped = level.getBlockEntity(base.offset(12, 0, 6)) instanceof LevelSensorBlockEntity ls ? ls.check(level) : 0;
            int changed = 0, wasted = 0;
            if (level.getBlockEntity(base.offset(12, 1, 6)) instanceof WaterChangerBlockEntity wc) {
                changed = wc.check(level, true);
                wasted = wc.lastWasteMb();
            }
            int moved = h.run(level, true);
            int wasteRes = level.getBlockEntity(base.offset(3, 0, 8)) instanceof ReservoirBlockEntity r ? r.water().amount() : -1;
            LOGGER.info("[smoketest] M40b topped off {} mB, changed {} mB, {} mB old water out ({} in the waste reservoir), filter moved {}: {}",
                    topped, changed, wasted, wasteRes, moved, h.tanks());
            if (topped != 800 || changed <= 0 || wasted != changed || wasteRes != changed || moved <= 0)
                LOGGER.error("[smoketest] M40b fresh/waste water through the bundles does not work");
        }
        a.markTankChanged();
    }

    // ------------------------------------------------------------------ steps

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.level().getServer().overworld(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, true));
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (steps.isEmpty()) return true;
        Step s = steps.peek();
        if (wait < s.delay()) {
            wait++;
            return false;
        }
        steps.poll();
        wait = 0;
        s.action().run();
        return steps.isEmpty();
    }

    private void server(Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m40b_" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
