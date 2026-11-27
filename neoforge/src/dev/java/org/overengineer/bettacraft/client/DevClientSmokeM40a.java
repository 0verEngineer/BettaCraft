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
import org.overengineer.bettacraft.filter.FilterChamberBlockEntity;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity;
import org.overengineer.bettacraft.filter.MultiblockFilter;
import org.overengineer.bettacraft.plumbing.FilterLines;
import org.overengineer.bettacraft.plumbing.interior.Plumb;
import org.overengineer.bettacraft.plumbing.interior.PlumbingBlock;
import org.overengineer.bettacraft.plumbing.interior.TankAnchors;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M40 part a client smoke ({@code -Psmoketestm40a}; background window): one multiblock filter for three tanks. The inlet line
 * (blue) runs along the front with T-pieces to the front glass of the three tanks, the return line (orange) above it docks on
 * the front of tank A, on the top of tank B (with interior pipes from its dock down to an outlet guard) and on the front of
 * tank C. A and B are guarded, C is not (warning). Screenshots {@code bettacraft_m40a_*}: the filter with its pipes, the
 * dock and guard inside tank A (interior), the top dock with its interior pipes in tank B, the filter screen.
 */
final class DevClientSmokeM40a {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos base, head;
    private BlockPos innerA, innerB;

    DevClientSmokeM40a() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(200, () -> look(base.getX() + 9.5, base.getY() + 4.5, base.getZ() + 11.5, 180F, 18F));
        then(200, () -> shot("filter"));
        then(1, () -> look(base.getX() + 2.5, base.getY() + 2.4, base.getZ() + 4.2, 180F, 20F));
        then(150, () -> shot("front"));
        then(1, () -> server(p -> p.teleportTo(InteriorManager.interiorLevel(p.server), innerA.getX() + 0.5, innerA.getY(), innerA.getZ() + 0.5, -38F, 6F)));
        then(200, () -> shot("dock_side"));
        then(1, () -> server(p -> p.teleportTo(InteriorManager.interiorLevel(p.server), innerB.getX() + 0.5, innerB.getY(), innerB.getZ() + 0.5, 0F, -28F)));
        then(200, () -> shot("dock_top"));
        then(1, () -> server(p -> p.teleportTo(p.server.overworld(), head.getX() + 0.5, head.getY() - 1.0, head.getZ() + 3.5, 180F, 10F)));
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
        ServerLevel level = p.serverLevel();
        ServerLevel in = InteriorManager.interiorLevel(p.server);
        base = p.blockPosition().offset(-10, 2, 200);
        for (BlockPos q : BlockPos.betweenClosed(base.offset(-2, -1, -2), base.offset(20, 6, 8)))
            level.setBlock(q, q.getY() == base.getY() - 1 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
        TankBlockEntity a = tank(level, p, base, 4, 2, 2);
        TankBlockEntity b = tank(level, p, base.offset(8, 0, 0), 4, 2, 2);
        TankBlockEntity c = tank(level, p, base.offset(16, 0, 0), 2, 2, 2);
        // the filter: two chambers, the pump head, inlet and outlet facing north
        for (int y = 0; y < 2; y++) {
            level.setBlock(base.offset(6, y, 5), ModBlocks.FILTER_CHAMBER.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(base.offset(6, y, 5)) instanceof FilterChamberBlockEntity ch)
                ch.setMedium(new ItemStack(y == 0 ? ModItems.CERAMIC_RINGS.get() : ModItems.FILTER_FOAM.get()));
        }
        head = base.offset(6, 2, 5);
        level.setBlock(head, ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(base.offset(6, 0, 4), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        level.setBlock(base.offset(6, 1, 4), ModBlocks.FILTER_OUTLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        // the inlet line along z = 3 at the floor, T-pieces at the connector and at tank B
        Block inlet = ModBlocks.STAINLESS_STEEL_PIPE.get(), outlet = ModBlocks.STAINLESS_STEEL_PIPE.get();
        line(level, inlet, base, new int[]{1, 0, 2});
        for (int x = 1; x <= 17; x++) line(level, inlet, base, new int[]{x, 0, 3});
        line(level, inlet, base, new int[]{9, 0, 2}, new int[]{17, 0, 2});
        // the return line at y = 2: down to the front of A, onto the top of B, down to the front of C
        line(level, outlet, base, new int[]{2, 1, 2}, new int[]{2, 1, 3});
        for (int x = 2; x <= 6; x++) line(level, outlet, base, new int[]{x, 2, 3});
        line(level, outlet, base, new int[]{6, 1, 3});
        for (int x = 7; x <= 10; x++) line(level, outlet, base, new int[]{x, 2, 3});
        line(level, outlet, base, new int[]{10, 2, 2}, new int[]{10, 2, 1});
        for (int x = 11; x <= 16; x++) line(level, outlet, base, new int[]{x, 2, 3});
        line(level, outlet, base, new int[]{16, 1, 3}, new int[]{16, 1, 2});
        // the guards: A and B (B's return dock on the top leads on through interior pipes to an outlet guard), not C
        MultiblockFilter.Structure s = MultiblockFilter.scan(level, head);
        List<MultiblockFilter.Link> links = MultiblockFilter.links(level, s);
        LOGGER.info("[smoketest] M40a {} tanks on the filter: {}", links.size(), links.stream().map(l -> l.tank().getBlockPos().toShortString()).toList());
        if (links.size() != 3) LOGGER.error("[smoketest] M40a the filter does not reach its three tanks: {}", links.size());
        InteriorRegion rb = InteriorManager.ensureRegion(level, b);
        long top = TankAnchors.ensure(level, b, base.offset(10, 1, 1), Direction.UP);
        int tx = BlockPos.getX(top), ty = BlockPos.getY(top), tz = BlockPos.getZ(top);
        for (int i = 1; i <= 4; i++) in.setBlock(rb.toWorld(tx, ty - i, tz), ModBlocks.INTERIOR_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        for (int i = 1; i <= 3; i++) in.setBlock(rb.toWorld(tx, ty - 4, tz + i), ModBlocks.INTERIOR_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        in.setBlock(rb.toWorld(tx, ty - 4, tz + 4), ModBlocks.OUTLET_GUARD.get().defaultBlockState().setValue(PlumbingBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
        for (MultiblockFilter.Link l : links) {
            if (l.tank() == c) continue;
            for (FilterLines.Dock d : l.intakes()) TankAnchors.installGuard(level, d.member(), d.face(), null, new ItemStack(ModItems.INTAKE_GUARD.get()), Plumb.INTAKE_GUARD);
            for (FilterLines.Dock d : l.returns()) TankAnchors.installGuard(level, d.member(), d.face(), null, new ItemStack(ModItems.OUTLET_GUARD.get()), Plumb.OUTLET_GUARD);
        }
        if (level.getBlockEntity(head) instanceof FilterPumpHeadBlockEntity h) {
            int moved = h.run(level, true);
            LOGGER.info("[smoketest] M40a filter moved {} mB: {}", moved, h.tanks());
            if (moved <= 0 || h.tanks().stream().filter(t -> t.status() == MultiblockFilter.LinkStatus.OK).count() != 2)
                LOGGER.error("[smoketest] M40a the filter does not serve tanks A and B: {}", h.tanks());
            org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing.syncStates(level, a);
            org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing.syncStates(level, b);
        }
        // where the player stands inside: before the south wall of A's block (2,1,1) and under B's top dock
        InteriorRegion ra = InteriorManager.ensureRegion(level, a);
        long side = TankAnchors.existing(level, a, base.offset(2, 1, 1), Direction.SOUTH);
        innerA = ra.toWorld(BlockPos.getX(side) - 4, BlockPos.getY(side) - 1, BlockPos.getZ(side) - 5); // beside it: dock and guard both seen
        innerB = rb.toWorld(tx, ty - 7, tz - 3);
        for (InteriorRegion r : List.of(ra, rb)) {
            for (int cx = r.originX() >> 4; cx <= (r.originX() + r.sizeX()) >> 4; cx++)
                for (int cz = r.originZ() >> 4; cz <= (r.originZ() + r.sizeZ()) >> 4; cz++) in.getChunk(cx, cz);
        }
        a.markTankChanged();
        b.markTankChanged();
        c.markTankChanged();
    }

    // ------------------------------------------------------------------ steps

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.server.overworld(), x, eyeY - p.getEyeHeight(), z, yaw, pitch));
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
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m40a_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
