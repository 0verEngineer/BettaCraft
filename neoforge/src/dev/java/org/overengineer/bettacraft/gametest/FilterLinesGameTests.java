package org.overengineer.bettacraft.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.filter.FilterChamberBlockEntity;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity;
import org.overengineer.bettacraft.filter.MultiblockFilter;
import org.overengineer.bettacraft.plumbing.FilterLines;
import org.overengineer.bettacraft.plumbing.FilterPipeBlock;
import org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing;
import org.overengineer.bettacraft.plumbing.interior.PipeAnchorBlock;
import org.overengineer.bettacraft.plumbing.interior.Plumb;
import org.overengineer.bettacraft.plumbing.interior.PlumbingBlock;
import org.overengineer.bettacraft.plumbing.interior.PlumbingItem;
import org.overengineer.bettacraft.plumbing.interior.TankAnchors;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;

/**
 * M40 part a GameTests: one multiblock filter for several tanks through filter pipes and T-pieces (A1: cleaning and flow
 * by water volume), the docks at a side and the top with their connection inside (E2) and interior pipes from there,
 * no pipe through a tank, the guards (missing: warning and no filtering), a filter from before M40 after its migration
 * (D2: warning until the guards are fitted) and the switch {@code features.pipes.filterLines} off. Deterministic: the
 * pump head's batches are called directly.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FilterLinesGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private FilterLinesGameTests() {}

    // ------------------------------------------------------------------ helpers

    /** A filter of chambers (ceramic 24 + sponge 12 water blocks) with its head at {@code head}, inlet and outlet facing {@code out}. */
    static FilterPumpHeadBlockEntity filter(GameTestHelper h, BlockPos chamber, Direction out) {
        ServerLevel level = h.getLevel();
        h.setBlock(chamber, ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
        h.setBlock(chamber.above(), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
        h.setBlock(chamber.above(2), ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState());
        h.setBlock(chamber.relative(out), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, out));
        h.setBlock(chamber.above().relative(out), ModBlocks.FILTER_OUTLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, out));
        ((FilterChamberBlockEntity) level.getBlockEntity(h.absolutePos(chamber))).setMedium(new ItemStack(ModItems.CERAMIC_RINGS.get()));
        ((FilterChamberBlockEntity) level.getBlockEntity(h.absolutePos(chamber.above()))).setMedium(new ItemStack(ModItems.FILTER_FOAM.get()));
        return (FilterPumpHeadBlockEntity) level.getBlockEntity(h.absolutePos(chamber.above(2)));
    }

    static void pipes(GameTestHelper h, Block b, BlockPos... rel) {
        for (BlockPos p : rel) h.setBlock(p, b.defaultBlockState());
    }

    /** Fits an intake guard behind every dock of the filter's inlet line and an outlet guard behind every dock of its return line. */
    public static void guardAll(ServerLevel level, BlockPos headPos) {
        for (MultiblockFilter.Link l : MultiblockFilter.links(level, MultiblockFilter.scan(level, headPos))) {
            for (FilterLines.Dock d : l.intakes())
                TankAnchors.installGuard(level, d.member(), d.face(), null, new ItemStack(ModItems.INTAKE_GUARD.get()), Plumb.INTAKE_GUARD);
            for (FilterLines.Dock d : l.returns())
                TankAnchors.installGuard(level, d.member(), d.face(), null, new ItemStack(ModItems.OUTLET_GUARD.get()), Plumb.OUTLET_GUARD);
        }
    }

    private static List<MultiblockFilter.LinkStatus> statuses(FilterPumpHeadBlockEntity head) {
        return head.tanks().stream().map(FilterPumpHeadBlockEntity.TankLink::status).toList();
    }

    private static boolean near(double a, double b, double eps) {
        return Math.abs(a - b) <= eps;
    }

    // ------------------------------------------------------------------ one filter, three tanks

    /**
     * Three tanks (8, 8 and 16 blocks) on one filter: the inlet line along z = 4 with branches (T-pieces) to the south
     * glass of each tank, the return line above it. Without guards: a warning per tank and no cleaning; the dock of each
     * pipe stands inside on the south wall of that block. With intake guards only: no outlet guard. With both: the filter
     * cleans all three, its 36 water blocks and its flow shared 25 / 25 / 50 % by their water.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void oneFilterServesThreeTanksByVolume(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MultiblockFilter.forget();
        InteriorPlumbing.forget();
        TankBlockEntity a = PlumbingGameTests.tank(h, 0, 2, 0, 2, 2, 2);
        TankBlockEntity b = PlumbingGameTests.tank(h, 4, 2, 0, 2, 2, 2);
        TankBlockEntity c = PlumbingGameTests.tank(h, 8, 2, 0, 4, 2, 2);
        FilterPumpHeadBlockEntity head = filter(h, new BlockPos(13, 2, 4), Direction.WEST);
        Block in = ModBlocks.FILTER_INLET_PIPE.get(), out = ModBlocks.FILTER_OUTLET_PIPE.get();
        // the branches first (from the glass outwards), then the trunks
        for (int x : new int[]{0, 4, 8}) pipes(h, in, new BlockPos(x, 2, 2), new BlockPos(x, 2, 3));
        for (int x : new int[]{1, 5, 9}) pipes(h, out, new BlockPos(x, 3, 2), new BlockPos(x, 3, 3));
        for (int x = 0; x <= 11; x++) pipes(h, in, new BlockPos(x, 2, 4));
        for (int x = 1; x <= 11; x++) pipes(h, out, new BlockPos(x, 3, 4));
        h.assertTrue(FilterPipeBlock.count(level.getBlockState(h.absolutePos(new BlockPos(4, 2, 4)))) == 2, "a plain pipe joins two sides");
        int docksBefore = FilterLines.docks(level, h.absolutePos(new BlockPos(12, 2, 4)), Direction.WEST, org.overengineer.bettacraft.plumbing.PipeLine.INTAKE).size();
        h.assertTrue(docksBefore == 0, "without T-pieces the plain pipes at the branches are full: the line ends before them: " + docksBefore);
        for (int x : new int[]{4, 8}) FilterPipeBlock.makeJunction(level, h.absolutePos(new BlockPos(x, 2, 4)));
        for (int x : new int[]{5, 9}) FilterPipeBlock.makeJunction(level, h.absolutePos(new BlockPos(x, 3, 4)));
        h.assertTrue(FilterPipeBlock.count(level.getBlockState(h.absolutePos(new BlockPos(4, 2, 4)))) == 3, "the T-piece joins three sides");
        double a0 = TankWater.filterBlocks(level, a), b0 = TankWater.filterBlocks(level, b), c0 = TankWater.filterBlocks(level, c);
        // no guards: every tank warns, nothing is cleaned
        h.assertTrue(head.run(level, true) == 0, "no guards: the filter stands still");
        h.assertTrue(head.tanks().size() == 3, "three tanks on the filter: " + head.tanks());
        h.assertTrue(statuses(head).stream().allMatch(s -> s == MultiblockFilter.LinkStatus.NO_INTAKE_GUARD), "no intake guards: " + statuses(head));
        h.assertTrue(MultiblockFilter.warnings(level, a).containsValue(MultiblockFilter.LinkStatus.NO_INTAKE_GUARD), "the tank menu warns");
        h.assertTrue(TankWater.filterBlocks(level, a) == a0, "and it does not clean");
        // E2: the dock of the pipe is inside on the south wall of that tank block
        InteriorRegion ra = AerationGameTests.region(h, a);
        long dock = TankAnchors.existing(level, a, h.absolutePos(new BlockPos(0, 2, 1)), Direction.SOUTH);
        h.assertTrue(dock == TankAnchors.localKey(0, 0, 1, Direction.SOUTH, ra.scale()), "the dock sits in the middle of the south face: " + BlockPos.of(dock));
        BlockState ds = AerationGameTests.interior(h).getBlockState(ra.toWorld(BlockPos.getX(dock), BlockPos.getY(dock), BlockPos.getZ(dock)));
        h.assertTrue(ds.getBlock() instanceof PipeAnchorBlock && ds.getValue(PipeAnchorBlock.MOUNT) == Direction.SOUTH, "a pipe dock on the south wall: " + ds);
        // intake guards only
        for (TankBlockEntity t : List.of(a, b, c)) {
            BlockPos member = t == a ? new BlockPos(0, 2, 1) : t == b ? new BlockPos(4, 2, 1) : new BlockPos(8, 2, 1);
            h.assertTrue(TankAnchors.installGuard(level, h.absolutePos(member), Direction.SOUTH, null, new ItemStack(ModItems.INTAKE_GUARD.get()),
                    Plumb.INTAKE_GUARD).consumesAction(), "intake guard on " + member);
        }
        head.run(level, true);
        h.assertTrue(statuses(head).stream().allMatch(s -> s == MultiblockFilter.LinkStatus.NO_OUTLET_GUARD), "no outlet guards: " + statuses(head));
        guardAll(level, head.getBlockPos());
        var watcher = QuestAdv.watcher(h, b); // M45
        int moved = head.run(level, true);
        h.assertTrue(statuses(head).stream().allMatch(s -> s == MultiblockFilter.LinkStatus.OK), "guarded: " + statuses(head));
        QuestAdv.check(h, watcher, "shared_filter");
        QuestAdv.gone(h, watcher);
        h.assertTrue(moved > 0 && MultiblockFilter.warnings(level, a).isEmpty(), "it runs, no warning: " + moved);
        // A1: by water volume (8, 8, 16 blocks)
        double da = TankWater.filterBlocks(level, a) - a0, db = TankWater.filterBlocks(level, b) - b0, dc = TankWater.filterBlocks(level, c) - c0;
        h.assertTrue(near(da, 9, 0.01) && near(db, 9, 0.01) && near(dc, 18, 0.01), "36 water blocks shared 9 / 9 / 18: " + da + " " + db + " " + dc);
        var links = head.tanks();
        h.assertTrue(links.stream().map(FilterPumpHeadBlockEntity.TankLink::percent).sorted().toList().equals(List.of(25, 25, 50)), "the shares: " + links);
        int fa = links.stream().filter(l -> l.tank().equals(a.getBlockPos())).findFirst().orElseThrow().flow();
        int fc = links.stream().filter(l -> l.tank().equals(c.getBlockPos())).findFirst().orElseThrow().flow();
        h.assertTrue(fa > 0 && near(fc, 2 * fa, 1), "the flow too: " + fa + " / " + fc);
        h.assertTrue(near(fa + fa + fc, head.flow(), 1), "the shares add up to the filter's flow: " + head.flow());
        // the guards behind the docks run (stream, current)
        InteriorPlumbing.syncStates(level, a);
        long gk = InteriorPlumbing.net(ra, dock).intakes().getLong(0);
        h.assertTrue(AerationGameTests.interior(h).getBlockState(ra.toWorld(BlockPos.getX(gk), BlockPos.getY(gk), BlockPos.getZ(gk))).getValue(PlumbingBlock.ACTIVE),
                "the intake guard runs");
        // M65: the guard beside the dock is drawn joined to it (both have the side towards the other)
        BlockPos gp = ra.toWorld(BlockPos.getX(gk), BlockPos.getY(gk), BlockPos.getZ(gk)), dp = ra.toWorld(BlockPos.getX(dock), BlockPos.getY(dock), BlockPos.getZ(dock));
        Direction toDock = Direction.NORTH;
        for (Direction dd : Direction.values()) if (gp.relative(dd).equals(dp)) toDock = dd;
        BlockState gs = AerationGameTests.interior(h).getBlockState(gp), dks = AerationGameTests.interior(h).getBlockState(dp);
        h.assertTrue(gp.relative(toDock).equals(dp), "the guard sits next to the dock: " + gp + " / " + dp);
        h.assertTrue(gs.getValue(org.overengineer.bettacraft.plumbing.PipeBlock.SIDES.get(toDock))
                && dks.getValue(org.overengineer.bettacraft.plumbing.PipeBlock.SIDES.get(toDock.getOpposite())), "guard and dock joined: " + gs + " / " + dks);
        // breaking a branch: that tank leaves the filter, its dock goes
        level.destroyBlock(h.absolutePos(new BlockPos(0, 2, 2)), false);
        head.run(level, true);
        h.assertTrue(head.tanks().size() == 2, "two tanks left: " + head.tanks());
        TankAnchors.sweep(level, a);
        h.assertTrue(TankAnchors.existing(level, a, h.absolutePos(new BlockPos(0, 2, 1)), Direction.SOUTH) == Long.MIN_VALUE, "the dock without its pipe is gone");
        h.succeed();
    }

    // ------------------------------------------------------------------ docks on the side and on top, interior pipes

    /**
     * One tank: the inlet line docks on the west glass, the return line comes over from above and docks on the top. Inside,
     * the top dock hangs under the top of that block (E2: where the pipe docks outside, not where the first interior pipe
     * goes); interior pipes from it lead down to an outlet guard, which then is the guard of that dock and runs; an intake
     * guard goes on the west dock from outside.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void docksAtTheSideAndTheTopWithInteriorPipes(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MultiblockFilter.forget();
        InteriorPlumbing.forget();
        TankBlockEntity t = PlumbingGameTests.tank(h, 4, 2, 4, 2, 2, 2);
        FilterPumpHeadBlockEntity head = filter(h, new BlockPos(0, 2, 4), Direction.EAST);
        pipes(h, ModBlocks.FILTER_INLET_PIPE.get(), new BlockPos(2, 2, 4), new BlockPos(3, 2, 4));
        pipes(h, ModBlocks.FILTER_OUTLET_PIPE.get(), new BlockPos(2, 3, 4), new BlockPos(2, 4, 4), new BlockPos(3, 4, 4), new BlockPos(4, 4, 4));
        h.assertTrue(FilterPipeBlock.joined(level.getBlockState(h.absolutePos(new BlockPos(4, 4, 4))), Direction.DOWN), "the return pipe docks on the top");
        InteriorRegion r = AerationGameTests.region(h, t);
        int s = r.scale();
        long west = TankAnchors.existing(level, t, h.absolutePos(new BlockPos(4, 2, 4)), Direction.WEST);
        long top = TankAnchors.existing(level, t, h.absolutePos(new BlockPos(4, 3, 4)), Direction.UP);
        h.assertTrue(west == TankAnchors.localKey(0, 0, 0, Direction.WEST, s), "the west dock is inside on the west wall: " + BlockPos.of(west));
        h.assertTrue(top == TankAnchors.localKey(0, 1, 0, Direction.UP, s), "the top dock hangs under the top of the upper block: " + BlockPos.of(top));
        head.run(level, true);
        h.assertTrue(statuses(head).equals(List.of(MultiblockFilter.LinkStatus.NO_INTAKE_GUARD)), "no guards yet: " + statuses(head));
        // interior pipes from the top dock down to an outlet guard
        int x = BlockPos.getX(top), y = BlockPos.getY(top), z = BlockPos.getZ(top);
        for (int i = 1; i <= 3; i++) AerationGameTests.put(h, r, x, y - i, z, ModBlocks.INTERIOR_PIPE.get().defaultBlockState());
        BlockPos guard = AerationGameTests.put(h, r, x, y - 4, z, ModBlocks.OUTLET_GUARD.get().defaultBlockState().setValue(PlumbingBlock.FACING, Direction.EAST));
        h.assertTrue(TankAnchors.installGuard(level, h.absolutePos(new BlockPos(4, 2, 4)), Direction.WEST, null, new ItemStack(ModItems.INTAKE_GUARD.get()),
                Plumb.INTAKE_GUARD).consumesAction(), "intake guard from outside");
        h.assertTrue(!TankAnchors.installGuard(level, h.absolutePos(new BlockPos(4, 2, 4)), Direction.WEST, null, new ItemStack(ModItems.INTAKE_GUARD.get()),
                Plumb.INTAKE_GUARD).consumesAction(), "not twice");
        h.assertTrue(!TankAnchors.installGuard(level, h.absolutePos(new BlockPos(5, 2, 5)), Direction.EAST, null, new ItemStack(ModItems.INTAKE_GUARD.get()),
                Plumb.INTAKE_GUARD).consumesAction(), "not where no pipe docks");
        double w0 = TankWater.filterBlocks(level, t);
        int moved = head.run(level, true);
        h.assertTrue(moved > 0 && statuses(head).equals(List.of(MultiblockFilter.LinkStatus.OK)), "guarded through the interior pipes: " + statuses(head));
        h.assertTrue(near(TankWater.filterBlocks(level, t) - w0, 36, 0.01), "one tank gets all: " + (TankWater.filterBlocks(level, t) - w0));
        InteriorPlumbing.syncStates(level, t);
        h.assertTrue(AerationGameTests.interior(h).getBlockState(guard).getValue(PlumbingBlock.ACTIVE), "the outlet guard at the end of the pipes runs");
        h.assertTrue(InteriorPlumbing.currents(level, t).stream().anyMatch(cu -> cu.dx() == 1 && cu.strength() == Plumb.OUTLET_GUARD.strength()),
                "its soft current: " + InteriorPlumbing.currents(level, t));
        h.succeed();
    }

    /**
     * No pipe through a tank: the inlet and the return line dock on the west glass of the lower and the upper block; interior
     * pipes from one dock may not reach the other (placing the last piece is refused). Built anyway (a command), both docks
     * stay closed: the filter warns and does not filter, guards or not.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void noPipeThroughATank(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MultiblockFilter.forget();
        InteriorPlumbing.forget();
        TankBlockEntity t = PlumbingGameTests.tank(h, 4, 2, 4, 2, 2, 2);
        FilterPumpHeadBlockEntity head = filter(h, new BlockPos(0, 2, 4), Direction.EAST);
        pipes(h, ModBlocks.FILTER_INLET_PIPE.get(), new BlockPos(2, 2, 4), new BlockPos(3, 2, 4));
        pipes(h, ModBlocks.FILTER_OUTLET_PIPE.get(), new BlockPos(2, 3, 4), new BlockPos(3, 3, 4));
        guardAll(level, head.getBlockPos());
        double w0 = TankWater.filterBlocks(level, t);
        h.assertTrue(head.run(level, true) > 0 && statuses(head).equals(List.of(MultiblockFilter.LinkStatus.OK)), "guarded: " + statuses(head));
        MultiblockFilter.forget();
        InteriorRegion r = AerationGameTests.region(h, t);
        long low = TankAnchors.existing(level, t, h.absolutePos(new BlockPos(4, 2, 4)), Direction.WEST);
        long high = TankAnchors.existing(level, t, h.absolutePos(new BlockPos(4, 3, 4)), Direction.WEST);
        int x = BlockPos.getX(low), z = BlockPos.getZ(low);
        for (int y = BlockPos.getY(low) + 1; y < BlockPos.getY(high) - 1; y++) AerationGameTests.put(h, r, x, y, z, ModBlocks.INTERIOR_PIPE.get().defaultBlockState());
        BlockPos last = r.toWorld(x, BlockPos.getY(high) - 1, z);
        h.assertTrue(PlumbingItem.joinsTwoPorts(AerationGameTests.interior(h), last), "the last piece would lead the pipe out again: refused");
        h.assertTrue(!PlumbingItem.joinsTwoPorts(AerationGameTests.interior(h), r.toWorld(x + 3, BlockPos.getY(high) - 1, z)), "elsewhere it may go");
        AerationGameTests.put(h, r, x, BlockPos.getY(high) - 1, z, ModBlocks.INTERIOR_PIPE.get().defaultBlockState());
        h.assertTrue(head.run(level, true) == 0 && statuses(head).equals(List.of(MultiblockFilter.LinkStatus.PASS_THROUGH)), "through the tank: " + statuses(head));
        h.assertTrue(MultiblockFilter.warnings(level, t).containsValue(MultiblockFilter.LinkStatus.PASS_THROUGH), "the tank menu warns");
        h.assertTrue(near(TankWater.filterBlocks(level, t), w0, 0.01), "and nothing is cleaned");
        h.succeed();
    }

    // ------------------------------------------------------------------ old filters (D2) and the switch

    /**
     * A filter built before M40 (its pump head saved without a schema): its connectors touch the tank directly. After the
     * migration it is marked retrofit; it does not filter and warns until the guards are fitted, then it cleans as before
     * (36 water blocks) and the mark goes.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void oldFilterNeedsGuardsAfterTheUpdate(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MultiblockFilter.forget();
        TankBlockEntity c = PlumbingGameTests.tank(h, 0, 2, 0, 2, 2, 2);
        FilterPumpHeadBlockEntity head = filter(h, new BlockPos(3, 2, 0), Direction.WEST);
        // as saved by 1.31 (no schema, no retrofit mark)
        CompoundTag old = head.saveWithoutMetadata(level.registryAccess());
        old.remove("schema");
        old.remove("retrofit");
        old.putInt("power", 10);
        head.loadWithComponents(org.overengineer.bettacraft.util.NbtCompat.input(level.registryAccess(), old));
        h.assertTrue(head.retrofit(), "the migration marks the old filter");
        double w0 = TankWater.filterBlocks(level, c);
        h.assertTrue(head.run(level, true) == 0 && statuses(head).equals(List.of(MultiblockFilter.LinkStatus.NO_INTAKE_GUARD)), "no guard: " + statuses(head));
        h.assertTrue(TankWater.filterBlocks(level, c) == w0 && !MultiblockFilter.warnings(level, c).isEmpty(), "warning, not filtering");
        h.assertTrue(head.retrofit(), "still marked");
        // the docks of the connectors are inside the east wall of the blocks they touch
        h.assertTrue(TankAnchors.existing(level, c, h.absolutePos(new BlockPos(1, 2, 0)), Direction.EAST) != Long.MIN_VALUE, "dock of the inlet");
        guardAll(level, head.getBlockPos());
        int moved = head.run(level, true);
        h.assertTrue(moved > 0 && statuses(head).equals(List.of(MultiblockFilter.LinkStatus.OK)), "guarded: " + statuses(head));
        h.assertTrue(near(TankWater.filterBlocks(level, c) - w0, 36, 0.01), "cleans as before: " + (TankWater.filterBlocks(level, c) - w0));
        h.assertTrue(!head.retrofit(), "retrofitted: the mark is gone");
        // saved again: stays unmarked
        FilterPumpHeadBlockEntity copy = new FilterPumpHeadBlockEntity(head.getBlockPos(), head.getBlockState());
        copy.loadWithComponents(org.overengineer.bettacraft.util.NbtCompat.input(level.registryAccess(), head.saveWithoutMetadata(level.registryAccess())));
        h.assertTrue(!copy.retrofit(), "a pump head saved by M40 is no old one");
        h.succeed();
    }

    /** features.pipes.filterLines off: the filter works as before M40 (one tank, no guards), the filter pipes carry nothing (own batch). */
    @GameTest(template = EMPTY, batch = "bettacraft_m40_lines_off", timeoutTicks = 200)
    public static void filterLinesOff(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        boolean saved = Features.own(Feature.FILTER_LINES);
        try {
            Features.set(Feature.FILTER_LINES, false);
            MultiblockFilter.forget();
            TankBlockEntity c = PlumbingGameTests.tank(h, 0, 2, 0, 2, 2, 2);
            FilterPumpHeadBlockEntity head = filter(h, new BlockPos(3, 2, 0), Direction.WEST);
            double w0 = TankWater.filterBlocks(level, c);
            h.assertTrue(head.run(level, true) > 0 && c.getBlockPos().equals(head.servedTank()), "old way: it serves the tank without guards");
            h.assertTrue(near(TankWater.filterBlocks(level, c) - w0, 36, 0.01) && head.tanks().isEmpty(), "and cleans it");
            // a second filter on filter pipes only: nothing
            TankBlockEntity d = PlumbingGameTests.tank(h, 0, 2, 10, 2, 2, 2);
            FilterPumpHeadBlockEntity h2 = filter(h, new BlockPos(6, 2, 10), Direction.WEST);
            pipes(h, ModBlocks.FILTER_INLET_PIPE.get(), new BlockPos(2, 2, 10), new BlockPos(3, 2, 10), new BlockPos(4, 2, 10), new BlockPos(5, 2, 10));
            pipes(h, ModBlocks.FILTER_OUTLET_PIPE.get(), new BlockPos(2, 3, 10), new BlockPos(3, 3, 10), new BlockPos(4, 3, 10), new BlockPos(5, 3, 10));
            double d0 = TankWater.filterBlocks(level, d);
            h.assertTrue(h2.run(level, true) == 0 && h2.servedTank() == null && TankWater.filterBlocks(level, d) == d0, "the filter pipes carry nothing");
            h.assertTrue(TankAnchors.existing(level, d, h.absolutePos(new BlockPos(1, 2, 10)), Direction.EAST) == Long.MIN_VALUE, "and no dock appears");
        } finally {
            Features.set(Feature.FILTER_LINES, saved);
        }
        h.succeed();
    }
}
