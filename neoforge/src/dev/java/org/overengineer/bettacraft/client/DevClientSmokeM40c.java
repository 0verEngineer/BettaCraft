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
 * M40 part c client smoke ({@code -Psmoketestm40c [-Ppower] [-Pstorage] [-Pfacades]}; background window): the scene of part b
 * (a row of four-line pipe bundles between a tank and a multiblock filter) with covers: bundles in the middle under stone
 * bricks, oak planks, glass and glowstone (put on with the pipe cover and a block in the off hand, by a real right-click of
 * the player), single pipes at the tank end under the facades of AE2, EnderIO and Mekanism Covers and the fresh water pipe
 * at the filter under a Cable Facades facade, each only with its mod (otherwise our own cover of the same block). The
 * filter must still clean its tank and move fresh and waste water through the covered pipes. Screenshots
 * {@code bettacraft_m40c_*}: the whole run, the covered bundles close, the facades at the tank end, the filter end.
 */
final class DevClientSmokeM40c {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos base, head;

    DevClientSmokeM40c() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(200, () -> look(base.getX() + 6.5, base.getY() + 5.0, base.getZ() + 13.5, 180F, 32F));
        then(200, () -> shot("covers"));
        then(1, () -> look(base.getX() + 5.5, base.getY() + 2.6, base.getZ() + 9.8, 180F, 25F));
        then(150, () -> shot("covers_close"));
        then(1, () -> look(base.getX() - 1.5, base.getY() + 2.4, base.getZ() + 7.5, 240F, 30F));
        then(150, () -> shot("facades"));
        then(1, () -> look(base.getX() + 10.0, base.getY() + 3.4, base.getZ() + 10.8, 160F, 38F));
        then(150, () -> shot("filter_end"));
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
        base = p.blockPosition().offset(-10, 2, 260);
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
            if (lines != 4) LOGGER.error("[smoketest] M40c bundle at x {} carries {}", x, lines);
        }
        MultiblockFilter.Structure s = MultiblockFilter.scan(level, head);
        List<MultiblockFilter.Link> links = MultiblockFilter.links(level, s);
        LOGGER.info("[smoketest] M40c filter {}: {} tanks", s.problem(), links.size());
        if (links.size() != 1) LOGGER.error("[smoketest] M40c the filter does not reach its tank and ports");
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
            LOGGER.info("[smoketest] M40c topped off {} mB, changed {} mB, {} mB old water out ({} in the waste reservoir), filter moved {}: {}",
                    topped, changed, wasted, wasteRes, moved, h.tanks());
            if (topped != 800 || changed <= 0 || wasted != changed || wasteRes != changed || moved <= 0)
                LOGGER.error("[smoketest] M40c fresh/waste water through the bundles does not work");
        }
        covers(level, p);
        if (level.getBlockEntity(head) instanceof FilterPumpHeadBlockEntity h) {
            h.rescan();
            int moved = h.run(level, true);
            LOGGER.info("[smoketest] M40c under the covers the filter moved {}: {}", moved, h.tanks());
            if (moved <= 0) LOGGER.error("[smoketest] M40c the filter does not work under the covers");
        }
        a.markTankChanged();
    }

    // ------------------------------------------------------------------ the covers

    /** A right-click of the player on {@code pos} with {@code main} in the main hand and {@code off} in the off hand. */
    private static void click(ServerPlayer p, BlockPos pos, ItemStack main, ItemStack off) {
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, main);
        p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, off);
        p.gameMode.useItemOn(p, p.level(), main, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false));
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
    }

    private static boolean loaded(String mod) {
        return org.overengineer.bettacraft.platform.Platform.get().isModLoaded(mod);
    }

    @SuppressWarnings("unchecked")
    private static ItemStack withData(String item, String component, Object value) {
        ItemStack s = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(item)));
        var t = (net.minecraft.core.component.DataComponentType<Object>) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
                .getValue(net.minecraft.resources.Identifier.parse(component));
        if (t != null) s.set(t, value);
        return s;
    }

    /** Our covers on four bundles (4..7), the facades of other mods on single pipes at the tank end and the filter end. */
    private void covers(ServerLevel level, ServerPlayer p) {
        ItemStack tool = new ItemStack(ModItems.PIPE_COVER.get());
        Block[] ours = {Blocks.STONE_BRICKS, Blocks.OAK_PLANKS, Blocks.GLASS, Blocks.GLOWSTONE};
        for (int i = 0; i < ours.length; i++) click(p, base.offset(4 + i, 0, 6), tool.copy(), new ItemStack(ours[i]));
        // the pipes between the tank and the bundles: AE2, EnderIO, Mekanism Covers (or our cover of the same block)
        try {
            ItemStack ae2 = ItemStack.EMPTY, eio = ItemStack.EMPTY, mek = ItemStack.EMPTY;
            if (loaded("ae2")) {
                var f = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("ae2:facade"));
                ae2 = (ItemStack) f.getClass().getMethod("createFacadeForItem", ItemStack.class, boolean.class).invoke(f, new ItemStack(Blocks.SMOOTH_QUARTZ), false);
            }
            if (loaded("enderio")) {
                Object paint = Class.forName("com.enderio.enderio.content.paint.BlockPaintData").getMethod("of", Block.class).invoke(null, Blocks.POLISHED_DEEPSLATE);
                eio = withData("enderio:conduit_facade", "enderio:block_paint", paint);
            }
            if (loaded("mekanismcovers")) mek = withData("mekanismcovers:cover", "mekanismcovers:cover_block", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(Blocks.CUT_COPPER));
            click(p, base.offset(2, 0, 5), ae2.isEmpty() ? tool.copy() : ae2, ae2.isEmpty() ? new ItemStack(Blocks.SMOOTH_QUARTZ) : ItemStack.EMPTY);
            click(p, base.offset(2, 1, 5), eio.isEmpty() ? tool.copy() : eio, eio.isEmpty() ? new ItemStack(Blocks.POLISHED_DEEPSLATE) : ItemStack.EMPTY);
            click(p, base.offset(2, 0, 6), mek.isEmpty() ? tool.copy() : mek, mek.isEmpty() ? new ItemStack(Blocks.CUT_COPPER) : ItemStack.EMPTY);
            LOGGER.info("[smoketest] M40c facades: ae2 {}, enderio {}, mekanismcovers {}", !ae2.isEmpty(), !eio.isEmpty(), !mek.isEmpty());
        } catch (ReflectiveOperationException e) {
            LOGGER.error("[smoketest] M40c a facade of another mod failed: {}", e.toString());
        }
        // Cable Facades: its own facade (its world data) on the fresh water pipe at the filter end
        if (loaded("cable_facades")) {
            try {
                Class.forName("com.portingdeadmods.cable_facades.utils.FacadeUtils").getMethod("addFacade", net.minecraft.world.level.Level.class, BlockPos.class,
                        net.minecraft.world.level.block.state.BlockState.class).invoke(null, level, base.offset(10, 0, 4), Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
                LOGGER.info("[smoketest] M40c Cable Facades facade on the fresh water pipe");
            } catch (ReflectiveOperationException e) {
                LOGGER.error("[smoketest] M40c Cable Facades failed: {}", e.toString());
            }
        } else {
            click(p, base.offset(10, 0, 4), tool.copy(), new ItemStack(Blocks.MOSSY_STONE_BRICKS));
        }
        int covered = 0;
        for (BlockPos q : List.of(base.offset(4, 0, 6), base.offset(5, 0, 6), base.offset(6, 0, 6), base.offset(7, 0, 6), base.offset(2, 0, 5),
                base.offset(2, 1, 5), base.offset(2, 0, 6)))
            if (org.overengineer.bettacraft.plumbing.PipeCovers.coverAt(level, q) != null) covered++;
        LOGGER.info("[smoketest] M40c {} of 7 pipes covered", covered);
        if (covered != 7) LOGGER.error("[smoketest] M40c not every pipe got its cover");
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m40c_" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
