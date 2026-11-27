package org.overengineer.bettacraft.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.filter.FilterChamberBlockEntity;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity;
import org.overengineer.bettacraft.filter.MultiblockFilter;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.PipeBlockEntity;
import org.overengineer.bettacraft.plumbing.PipeBundleBlock;
import org.overengineer.bettacraft.plumbing.PipeItem;
import org.overengineer.bettacraft.plumbing.PipeNetwork;
import org.overengineer.bettacraft.plumbing.PipePorts;
import org.overengineer.bettacraft.plumbing.PipeRing;
import org.overengineer.bettacraft.plumbing.PipeRole;
import org.overengineer.bettacraft.plumbing.PipeSlots;
import org.overengineer.bettacraft.plumbing.PipeUpdates;
import org.overengineer.bettacraft.plumbing.PipeWrenchItem;
import org.overengineer.bettacraft.plumbing.Pipes;
import org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing;
import org.overengineer.bettacraft.plumbing.interior.Plumb;
import org.overengineer.bettacraft.plumbing.interior.PlumbingBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;

/**
 * M68 GameTests: one pipe system. Glass and steel pipes join every neighbour on every side (a network carries what its weakest
 * pipe carries); the pipe wrench closes and opens a side (two lines side by side stay apart); the ports give a network its role
 * and its pipes their ring (intake, return, none; a short circuit stops the filter); a filter line carries at most what its
 * weakest pipe carries and several lines add up (glass halves a 100 mB/t filter, two glass lines each way give it back); a bundle
 * keeps two lines of the same role apart (glass and steel mixed, by hand with sneak + pipe, taken out again, a pipe moved to the
 * next spot); a glass or steel pipe in a tank interior is an interior pipe; the wrench's switch off. Deterministic: the pump
 * heads run directly, the rings are given right away ({@link PipeUpdates#flush}).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PipeSystemGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private PipeSystemGameTests() {}

    private static BlockPos abs(GameTestHelper h, int x, int y, int z) {
        return h.absolutePos(new BlockPos(x, y, z));
    }

    private static void steel(GameTestHelper h, int[]... at) {
        for (int[] p : at) h.setBlock(new BlockPos(p[0], p[1], p[2]), ModBlocks.STAINLESS_STEEL_PIPE.get().defaultBlockState());
    }

    private static void glass(GameTestHelper h, int[]... at) {
        for (int[] p : at) h.setBlock(new BlockPos(p[0], p[1], p[2]), ModBlocks.GLASS_PIPE.get().defaultBlockState());
    }

    private static PipeNetwork net(GameTestHelper h, int x, int y, int z) {
        return PipeNetwork.of(h.getLevel(), abs(h, x, y, z));
    }

    private static PipeRing ring(GameTestHelper h, int x, int y, int z) {
        return h.getLevel().getBlockState(abs(h, x, y, z)).getValue(PipeBlock.RING);
    }

    // ------------------------------------------------------------------ joining

    /** A steel pipe with six neighbours (two glass) joins all six; the network carries what its glass pipes carry; a reservoir joins. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void pipeJoinsEveryNeighbour(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        steel(h, new int[]{3, 3, 3}, new int[]{3, 3, 2}, new int[]{3, 3, 4}, new int[]{3, 4, 3}, new int[]{3, 2, 3});
        glass(h, new int[]{2, 3, 3}, new int[]{4, 3, 3});
        h.assertTrue(PipeBlock.sides(level.getBlockState(abs(h, 3, 3, 3))) == PipeSlots.SIDES, "joined on all six sides: " + level.getBlockState(abs(h, 3, 3, 3)));
        PipeNetwork n = net(h, 3, 3, 3);
        h.assertTrue(n.size() == 7, "one network of seven pipes: " + n.size());
        h.assertTrue(n.rate() == PipeBlock.rate(false), "it carries what its glass pipes carry: " + n.rate());
        h.assertTrue(n.role() == null && n.ring() == PipeRing.NONE && n.carriesWater(), "a plain water line");
        // without the glass pipes: steel
        h.setBlock(new BlockPos(2, 3, 3), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        h.setBlock(new BlockPos(4, 3, 3), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        h.assertTrue(net(h, 3, 3, 3).rate() == PipeBlock.rate(true), "steel only: " + net(h, 3, 3, 3).rate());
        // a reservoir at an end joins (a fluid end of the network)
        PlumbingGameTests.reservoir(h, new BlockPos(3, 3, 1), 1000, null);
        h.assertTrue(level.getBlockState(abs(h, 3, 3, 2)).getValue(PipeBlock.SIDES.get(Direction.NORTH)), "the pipe joins the reservoir");
        h.assertTrue(net(h, 3, 3, 3).ends().stream().anyMatch(e -> e.pos().equals(abs(h, 3, 3, 1))), "an end of the network");
        h.succeed();
    }

    /**
     * Two lines side by side (z 2 and z 3) join into one; the wrench on the first pipe's south side (a click like a player's)
     * and on the others keeps them apart: two networks, a closed side in the block entity; clicked again, one network again.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void pipeWrenchKeepsTwoLinesApart(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 1; x <= 5; x++) steel(h, new int[]{x, 2, 2}, new int[]{x, 2, 3});
        h.assertTrue(net(h, 1, 2, 2).contains(abs(h, 1, 2, 3)), "side by side they join");
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            ItemStack wrench = new ItemStack(ModItems.PIPE_WRENCH.get());
            p.setItemInHand(InteractionHand.MAIN_HAND, wrench);
            BlockPos first = abs(h, 1, 2, 2);
            // the south arm of the first pipe
            Vec3 hit = Vec3.atCenterOf(first).add(0, 0, 0.4);
            InteractionResult r = wrench.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.SOUTH, first, false)));
            h.assertTrue(r.consumesAction(), "the wrench works: " + r);
            h.assertTrue(level.getBlockEntity(first) instanceof PipeBlockEntity be && be.closed(Direction.SOUTH), "the south side is closed");
            h.assertTrue(!level.getBlockState(first).getValue(PipeBlock.SIDES.get(Direction.SOUTH))
                    && !level.getBlockState(abs(h, 1, 2, 3)).getValue(PipeBlock.SIDES.get(Direction.NORTH)), "neither pipe joins the other there");
            for (int x = 2; x <= 5; x++) PipeWrenchItem.setClosed(level, abs(h, x, 2, 2), -1, Direction.SOUTH, true);
            h.assertTrue(!net(h, 1, 2, 2).contains(abs(h, 1, 2, 3)) && net(h, 1, 2, 2).size() == 5 && net(h, 1, 2, 3).size() == 5, "two lines of five");
            h.assertTrue(PipeWrenchItem.side(first, hit) == Direction.SOUTH, "the side clicked");
            // opened again: one line
            wrench.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.SOUTH, first, false)));
            h.assertTrue(level.getBlockState(first).getValue(PipeBlock.SIDES.get(Direction.SOUTH)) && net(h, 1, 2, 2).size() == 10, "open again: one network");
            h.assertTrue(wrench.getCount() == 1, "the wrench is not used up");
        } finally {
            p.level().getServer().getPlayerList().remove(p);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ roles

    /** A filter (chambers (0,2,4) and (0,3,4), head above), inlet (1,2,4) and outlet (1,3,4) facing east. */
    private static FilterPumpHeadBlockEntity filter(GameTestHelper h) {
        return FilterLinesGameTests.filter(h, new BlockPos(0, 2, 4), Direction.EAST);
    }

    /**
     * The pipes at the filter's inlet are its intake line (blue rings), at its outlet the return line (orange); a pipe without a
     * port is plain. One pipe that joins both lines makes a short circuit: red rings, the filter stops and says so; the wrench
     * splits it again.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void pipeRolesFromThePorts(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MultiblockFilter.forget();
        InteriorPlumbing.forget();
        FilterPumpHeadBlockEntity head = filter(h);
        TankBlockEntity t = PlumbingGameTests.tank(h, 5, 2, 4, 2, 2, 2);
        steel(h, new int[]{2, 2, 4}, new int[]{3, 2, 4}, new int[]{4, 2, 4});
        steel(h, new int[]{2, 3, 4}, new int[]{3, 3, 4}, new int[]{4, 3, 4});
        for (int x = 2; x <= 4; x++) FilterLinesGameTests.apart(h, new BlockPos(x, 2, 4), new BlockPos(x, 3, 4));
        steel(h, new int[]{8, 2, 8});
        PipeUpdates.flush(level);
        h.assertTrue(ring(h, 3, 2, 4) == PipeRing.INTAKE && net(h, 3, 2, 4).role() == PipeRole.INTAKE, "the intake line: " + ring(h, 3, 2, 4));
        h.assertTrue(ring(h, 3, 3, 4) == PipeRing.RETURN && net(h, 3, 3, 4).role() == PipeRole.RETURN, "the return line: " + ring(h, 3, 3, 4));
        h.assertTrue(ring(h, 8, 2, 8) == PipeRing.NONE, "a plain pipe");
        h.assertTrue(!net(h, 3, 2, 4).carriesWater(), "a filter line carries no water for others");
        PipePorts.Line in = PipePorts.line(level, abs(h, 1, 2, 4), Direction.EAST);
        h.assertTrue(in.network() == net(h, 2, 2, 4) && in.rate() == PipeBlock.rate(true) && in.docks(level).size() == 1, "the line at the inlet: " + in);
        FilterLinesGameTests.guardAll(level, head.getBlockPos());
        h.assertTrue(head.run(level, true) > 0 && head.problem() == MultiblockFilter.Problem.NONE, "it filters: " + head.problem());
        // the short circuit: the joint at x 3 opened
        PipeWrenchItem.setClosed(level, abs(h, 3, 2, 4), -1, Direction.UP, false);
        PipeUpdates.flush(level);
        h.assertTrue(net(h, 3, 2, 4).shortCircuit() && ring(h, 2, 2, 4) == PipeRing.SHORT && ring(h, 4, 3, 4) == PipeRing.SHORT, "red rings: " + ring(h, 2, 2, 4));
        h.assertTrue(head.run(level, true) == 0 && head.problem() == MultiblockFilter.Problem.SHORT_CIRCUIT, "the filter stops: " + head.problem());
        var lines = org.overengineer.bettacraft.compat.info.InfoLines.pipe(level, abs(h, 3, 2, 4), level.getBlockState(abs(h, 3, 2, 4)), null);
        h.assertTrue(!lines.isEmpty() && lines.get(0).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                && tc.getKey().equals("jade.bettacraft.pipe.role.short"), "the overlays say so: " + lines);
        PipeWrenchItem.setClosed(level, abs(h, 3, 2, 4), -1, Direction.UP, true);
        PipeUpdates.flush(level);
        h.assertTrue(ring(h, 2, 2, 4) == PipeRing.INTAKE && head.run(level, true) > 0 && head.problem() == MultiblockFilter.Problem.NONE, "apart again: it filters");
        h.succeed();
    }

    // ------------------------------------------------------------------ throughput

    /**
     * A filter into one tank: one glass line each way halves the 100 mB/t of its pump head to 50 mB/t, steel lines give the full
     * 100; with a second inlet and outlet (facing south) two glass lines each way add up to 100 again, every line its own network
     * of the same role, each reporting its part of the flow.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void pipeLinesLimitTheFilterFlow(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MultiblockFilter.forget();
        InteriorPlumbing.forget();
        var facing = FilterPartBlock.FACING;
        BlockPos c = new BlockPos(4, 2, 4);
        h.setBlock(c, ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
        h.setBlock(c.above(), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
        h.setBlock(c.above(2), ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState());
        ((FilterChamberBlockEntity) level.getBlockEntity(h.absolutePos(c))).setMedium(new ItemStack(ModItems.CERAMIC_RINGS.get()));
        FilterPumpHeadBlockEntity head = (FilterPumpHeadBlockEntity) level.getBlockEntity(h.absolutePos(c.above(2)));
        h.setBlock(c.east(), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(facing, Direction.EAST));
        h.setBlock(c.above().east(), ModBlocks.FILTER_OUTLET.get().defaultBlockState().setValue(facing, Direction.EAST));
        TankBlockEntity t = PlumbingGameTests.tank(h, 7, 2, 3, 3, 2, 5);
        glass(h, new int[]{6, 2, 4});
        glass(h, new int[]{6, 3, 4});
        FilterLinesGameTests.apart(h, new BlockPos(6, 2, 4), new BlockPos(6, 3, 4));
        FilterLinesGameTests.guardAll(level, head.getBlockPos());
        head.run(level, true);
        h.assertTrue(head.flow() == PipeBlock.rate(false) && head.lineLimit() == PipeBlock.rate(false), "glass: half the pump head: " + head.flow() + " (limit " + head.lineLimit() + ")");
        // steel
        steel(h, new int[]{6, 2, 4});
        steel(h, new int[]{6, 3, 4});
        FilterLinesGameTests.apart(h, new BlockPos(6, 2, 4), new BlockPos(6, 3, 4));
        FilterLinesGameTests.guardAll(level, head.getBlockPos());
        head.run(level, true);
        int full = head.flow();
        h.assertTrue(full == 100, "steel: the pump head's 100 mB/t: " + full);
        // two glass lines each way: the second inlet and outlet face south, their lines run round to the tank
        glass(h, new int[]{6, 2, 4});
        glass(h, new int[]{6, 3, 4});
        FilterLinesGameTests.apart(h, new BlockPos(6, 2, 4), new BlockPos(6, 3, 4));
        h.setBlock(c.south(), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(facing, Direction.SOUTH));
        h.setBlock(c.above().south(), ModBlocks.FILTER_OUTLET.get().defaultBlockState().setValue(facing, Direction.SOUTH));
        glass(h, new int[]{4, 2, 6}, new int[]{5, 2, 6}, new int[]{6, 2, 6});
        glass(h, new int[]{4, 3, 6}, new int[]{5, 3, 6}, new int[]{6, 3, 6});
        for (int x = 4; x <= 6; x++) FilterLinesGameTests.apart(h, new BlockPos(x, 2, 6), new BlockPos(x, 3, 6));
        PipeUpdates.flush(level);
        h.assertTrue(net(h, 6, 2, 4) != net(h, 6, 2, 6) && net(h, 6, 2, 4).role() == PipeRole.INTAKE && net(h, 6, 2, 6).role() == PipeRole.INTAKE,
                "two intake lines, each its own network");
        FilterLinesGameTests.guardAll(level, head.getBlockPos());
        head.run(level, true);
        h.assertTrue(head.flow() == full && head.lineLimit() == 2 * PipeBlock.rate(false), "two glass lines each way add up: " + head.flow() + " (limit " + head.lineLimit() + ")");
        h.assertTrue(head.tanks().size() == 1 && head.tanks().get(0).tank().equals(t.getBlockPos()), "one tank");
        h.assertTrue(net(h, 6, 2, 4).flow(level) + net(h, 6, 2, 6).flow(level) == head.flow(), "the lines report their part: "
                + net(h, 6, 2, 4).flow(level) + " + " + net(h, 6, 2, 6).flow(level));
        h.succeed();
    }

    // ------------------------------------------------------------------ bundles

    /**
     * A run of bundles (10,2,10) and (11,2,10): the first pipe from the west goes on east, the second (glass) comes from the north
     * and leaves to the south; both end at filter inlets: two intake lines in one bundle that never join. Sneak + pipe makes a
     * bundle by hand, up to four; one taken out again; a pipe moved to the next spot.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void pipeBundleKeepsLinesOfOneRoleApart(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        steel(h, new int[]{9, 2, 10}, new int[]{10, 2, 10}, new int[]{11, 2, 10});
        BlockPos b1 = abs(h, 10, 2, 10), b2 = abs(h, 11, 2, 10);
        h.assertTrue(PipeBundleBlock.addPipe(level, b2, false, null, null) == 1, "a glass pipe into the steel pipe: a bundle");
        // by hand: sneak + right-click with a glass pipe
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            p.getAbilities().instabuild = false;
            ItemStack pipes = new ItemStack(ModItems.GLASS_PIPE.get(), 3);
            p.setItemInHand(InteractionHand.MAIN_HAND, pipes);
            p.setShiftKeyDown(true);
            // the item's use while sneaking (the game does not ask the block then)
            pipes.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(b1), Direction.UP, b1, false)));
            p.setShiftKeyDown(false);
            h.assertTrue(level.getBlockState(b1).getBlock() instanceof PipeBundleBlock && pipes.getCount() == 2, "sneak + pipe: a bundle, one pipe used");
        } finally {
            p.level().getServer().getPlayerList().remove(p);
        }
        h.assertTrue(PipeSlots.present(Pipes.slotsAt(level, b1)[1]) && !PipeSlots.steel(Pipes.slotsAt(level, b1)[1]), "a glass pipe in slot 1");
        steel(h, new int[]{10, 2, 9}, new int[]{12, 2, 10}, new int[]{11, 2, 11});
        h.setBlock(new BlockPos(8, 2, 10), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.EAST));
        h.setBlock(new BlockPos(10, 2, 8), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.SOUTH));
        PipeUpdates.flush(level);
        PipeNetwork a = PipeNetwork.of(level, b1, 0), b = PipeNetwork.of(level, b1, 1);
        h.assertTrue(a != b && a.contains(abs(h, 9, 2, 10)) && a.contains(abs(h, 12, 2, 10)) && !a.contains(abs(h, 10, 2, 9)), "the steel line west to east");
        h.assertTrue(b.contains(abs(h, 10, 2, 9)) && b.contains(abs(h, 11, 2, 11)) && !b.contains(abs(h, 9, 2, 10)), "the glass line north to south");
        h.assertTrue(PipeNetwork.of(level, b2, 1) == b && PipeNetwork.of(level, b2, 0) == a, "slot to slot through the next bundle");
        h.assertTrue(a.role() == PipeRole.INTAKE && b.role() == PipeRole.INTAKE && !a.shortCircuit() && !b.shortCircuit(), "both intake lines, no short circuit");
        h.assertTrue(PipeSlots.ring(Pipes.slotsAt(level, b1)[0]) == PipeRing.INTAKE && PipeSlots.ring(Pipes.slotsAt(level, b1)[1]) == PipeRing.INTAKE, "blue rings on both");
        h.assertTrue(a.rate() == PipeBlock.rate(true) && b.rate() == PipeBlock.rate(false), "steel and glass in one bundle: " + a.rate() + " / " + b.rate());
        // up to four
        h.assertTrue(PipeBundleBlock.addPipe(level, b1, true, null, null) == 2 && PipeBundleBlock.addPipe(level, b1, true, null, null) == 3, "a third and a fourth");
        h.assertTrue(PipeBundleBlock.addPipe(level, b1, true, null, null) == -1, "not a fifth");
        List<ItemStack> drops = Block.getDrops(level.getBlockState(b1), level, b1, level.getBlockEntity(b1));
        h.assertTrue(drops.size() == 4 && drops.stream().filter(s -> s.is(ModItems.GLASS_PIPE.get())).count() == 1, "drops: " + drops);
        h.assertTrue(PipeBundleBlock.removePipe(level, b1, 3).size() == 1 && PipeBundleBlock.removePipe(level, b1, 2).size() == 1, "two out again");
        // the glass pipe moved to the next spot: it leaves its lines (slot 2 meets no slot 2 in the next bundle)
        h.assertTrue(PipeBundleBlock.reslot(level, b1, 1) == 2 && PipeSlots.present(Pipes.slotsAt(level, b1)[2]) && !PipeSlots.present(Pipes.slotsAt(level, b1)[1]), "moved");
        h.assertTrue(PipeBundleBlock.reslot(level, b1, 2) == 3 && PipeBundleBlock.reslot(level, b1, 3) == 0 && PipeBundleBlock.reslot(level, b1, 0) == 1, "round");
        PipeUpdates.flush(level);
        h.assertTrue(PipeNetwork.of(level, b1, 1).contains(abs(h, 11, 2, 11)), "back in its spot it joins its line again");
        // down to one: a single pipe of its material with its sides
        h.assertTrue(PipeBundleBlock.removePipe(level, b1, 3).size() == 1, "the steel pipe (moved round to slot 3) out");
        BlockState single = level.getBlockState(b1);
        h.assertTrue(single.is(ModBlocks.GLASS_PIPE.get()) && single.getValue(PipeBlock.SIDES.get(Direction.NORTH)), "a single glass pipe that keeps its sides: " + single);
        h.succeed();
    }

    // ------------------------------------------------------------------ the interior

    /** A glass and a steel pipe placed in a tank interior are interior pipes (glass and steel to look at); each gives its pipe back. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void pipeInTheInteriorIsAnInteriorPipe(GameTestHelper h) {
        TankBlockEntity t = PlumbingGameTests.tank(h, 2, 2, 2, 2, 2, 2);
        InteriorRegion r = AerationGameTests.region(h, t);
        ServerLevel in = AerationGameTests.interior(h);
        BlockPos a = r.toWorld(5, 5, 5), b = r.toWorld(6, 5, 5);
        for (var e : List.of(java.util.Map.entry(a, ModItems.GLASS_PIPE.get()), java.util.Map.entry(b, ModItems.STAINLESS_STEEL_PIPE.get()))) {
            ItemStack stack = new ItemStack(e.getValue());
            InteractionResult res = ((PipeItem) e.getValue()).place(new BlockPlaceContext(in, null, InteractionHand.MAIN_HAND, stack,
                    new BlockHitResult(Vec3.atCenterOf(e.getKey()), Direction.UP, e.getKey(), false)));
            h.assertTrue(res.consumesAction(), "placed: " + res);
        }
        h.assertTrue(in.getBlockState(a).is(ModBlocks.INTERIOR_PIPE.get()) && in.getBlockState(b).is(ModBlocks.INTERIOR_STEEL_PIPE.get()),
                "interior pipes: " + in.getBlockState(a) + " / " + in.getBlockState(b));
        h.assertTrue(PlumbingBlock.code(in.getBlockState(b)) == Plumb.code(Plumb.PIPE, 0), "a steel one works as an interior pipe");
        h.assertTrue(in.getBlockState(a).getValue(PipeBlock.SIDES.get(Direction.EAST)), "joined to each other");
        List<ItemStack> da = Block.getDrops(in.getBlockState(a), in, a, null), db = Block.getDrops(in.getBlockState(b), in, b, null);
        h.assertTrue(da.size() == 1 && da.get(0).is(ModItems.GLASS_PIPE.get()) && db.size() == 1 && db.get(0).is(ModItems.STAINLESS_STEEL_PIPE.get()), "drops: " + da + " / " + db);
        h.assertTrue(in.getBlockState(b).getCloneItemStack(in, b, false).is(ModItems.STAINLESS_STEEL_PIPE.get()), "picked: the steel pipe");
        h.succeed();
    }

    // ------------------------------------------------------------------ the switch

    /** features.pipes.pipeWrench off: the wrench closes nothing; a closed side stays closed (own batch). */
    @GameTest(template = EMPTY, batch = "bettacraft_m68_wrench_off", timeoutTicks = 100)
    public static void pipeWrenchOff(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        steel(h, new int[]{2, 2, 2}, new int[]{3, 2, 2}, new int[]{4, 2, 2});
        PipeWrenchItem.setClosed(level, abs(h, 4, 2, 2), -1, Direction.WEST, true);
        boolean saved = Features.own(Feature.PIPE_WRENCH);
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            Features.set(Feature.PIPE_WRENCH, false);
            ItemStack wrench = new ItemStack(ModItems.PIPE_WRENCH.get());
            p.setItemInHand(InteractionHand.MAIN_HAND, wrench);
            BlockPos at = abs(h, 2, 2, 2);
            InteractionResult r = wrench.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(at).add(0.4, 0, 0), Direction.EAST, at, false)));
            h.assertTrue(!r.consumesAction() && level.getBlockState(at).getValue(PipeBlock.SIDES.get(Direction.EAST)), "the wrench does nothing: " + r);
            h.assertTrue(!level.getBlockState(abs(h, 4, 2, 2)).getValue(PipeBlock.SIDES.get(Direction.WEST)), "the closed side stays closed");
            h.assertTrue(org.overengineer.bettacraft.config.FeatureContent.ofItem("pipe_wrench").contains(Feature.PIPE_WRENCH), "pipe_wrench belongs to pipes.pipeWrench");
        } finally {
            Features.set(Feature.PIPE_WRENCH, saved);
            p.level().getServer().getPlayerList().remove(p);
        }
        h.succeed();
    }
}
