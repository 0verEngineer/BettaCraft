package org.overengineer.bettacraft.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.filter.FilterPortBlockEntity;
import org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity;
import org.overengineer.bettacraft.filter.MultiblockFilter;
import org.overengineer.bettacraft.plumbing.FilterLines;
import org.overengineer.bettacraft.plumbing.FilterPipeBlock;
import org.overengineer.bettacraft.plumbing.FilterPipeBundleBlock;
import org.overengineer.bettacraft.plumbing.FilterPipes;
import org.overengineer.bettacraft.plumbing.LevelSensorBlockEntity;
import org.overengineer.bettacraft.plumbing.PipeLine;
import org.overengineer.bettacraft.plumbing.ReservoirBlockEntity;
import org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity;
import org.overengineer.bettacraft.plumbing.WaterPumpBlockEntity;
import org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.WaterValues;

/**
 * M40 part b GameTests: the pipe bundle (four lines in one block kept apart, a line put in and taken out again by hand,
 * a T-piece on one line, the drops; a two-line bundle carrying a filter's intake and return line to a tank), the fresh water
 * at the multiblock filter (a water pump and a pipe of another block fill it; the level sensor on the filter tops off both
 * tanks, shared by need when short), the water change through the filter (both tanks by volume, without and with a waste
 * water port, short fresh water by share, the waste port holding old water for pipes that pull) and the switch
 * {@code features.pipes.filterWater} off. With {@code -Ppower} / {@code -Pstorage}: the pipes of Pipez, EnderIO and
 * Mekanism and AE2's export bus fill the fresh water line, Refined Storage's importer empties the waste water line.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FilterWaterGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private FilterWaterGameTests() {}

    private static BlockPos abs(GameTestHelper h, int x, int y, int z) {
        return h.absolutePos(new BlockPos(x, y, z));
    }

    private static int slot(GameTestHelper h, int x, int y, int z, PipeLine l) {
        return FilterPipes.at(h.getLevel(), abs(h, x, y, z), l);
    }

    private static int sides(int slot) {
        return slot & FilterPipes.SIDES;
    }

    private static int bits(Direction... ds) {
        int b = 0;
        for (Direction d : ds) b |= FilterPipes.bit(d);
        return b;
    }

    private static void pipes(GameTestHelper h, Block b, int[]... at) {
        for (int[] p : at) h.setBlock(new BlockPos(p[0], p[1], p[2]), b.defaultBlockState());
    }

    // ------------------------------------------------------------------ the bundle

    /**
     * Four lines in one block at (3,2,2): the intake line runs west-east to a tank, the return line up to a second tank,
     * the fresh line north, the waste line south. Each keeps its own sides and reaches only its own pipes and tanks. A
     * T-piece goes on the intake line only. Breaking would give four pipes and the T-piece. Sneaking with an empty hand on
     * the waste arm takes the waste line out (its pipe back, its neighbour lets go); with one line left the block is a
     * single pipe again (its sides and its T-piece kept); the outlet pipe used on it by hand makes a bundle again.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void bundleKeepsFourLinesApart(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MultiblockFilter.forget();
        TankBlockEntity east = PlumbingGameTests.tank(h, 6, 2, 2, 2, 2, 2);
        TankBlockEntity above = PlumbingGameTests.tank(h, 2, 5, 1, 2, 2, 2);
        for (int x = 1; x <= 5; x++) pipes(h, ModBlocks.FILTER_INLET_PIPE.get(), new int[]{x, 2, 2});
        BlockPos b = abs(h, 3, 2, 2);
        h.assertTrue(FilterPipeBundleBlock.addLine(level, b, PipeLine.RETURN, null, null), "a second line goes in");
        h.assertTrue(level.getBlockState(b).getBlock() instanceof FilterPipeBundleBlock, "the pipe is a bundle now");
        h.assertTrue(FilterPipeBundleBlock.addLine(level, b, PipeLine.FRESH, null, null) && FilterPipeBundleBlock.addLine(level, b, PipeLine.WASTE, null, null),
                "a third and a fourth line");
        h.assertTrue(!FilterPipeBundleBlock.addLine(level, b, PipeLine.WASTE, null, null), "not twice the same line");
        pipes(h, ModBlocks.FILTER_OUTLET_PIPE.get(), new int[]{3, 3, 2}, new int[]{3, 4, 2});
        pipes(h, ModBlocks.FILTER_FRESH_PIPE.get(), new int[]{3, 2, 0}, new int[]{3, 2, 1});
        pipes(h, ModBlocks.FILTER_WASTE_PIPE.get(), new int[]{3, 2, 4}, new int[]{3, 2, 3});
        h.assertTrue(FilterPipes.lines(level, b).size() == 4, "four lines: " + FilterPipes.lines(level, b));
        h.assertTrue(sides(slot(h, 3, 2, 2, PipeLine.INTAKE)) == bits(Direction.WEST, Direction.EAST), "intake west-east: " + slot(h, 3, 2, 2, PipeLine.INTAKE));
        h.assertTrue(sides(slot(h, 3, 2, 2, PipeLine.RETURN)) == bits(Direction.UP), "return up: " + slot(h, 3, 2, 2, PipeLine.RETURN));
        h.assertTrue(sides(slot(h, 3, 2, 2, PipeLine.FRESH)) == bits(Direction.NORTH), "fresh north: " + slot(h, 3, 2, 2, PipeLine.FRESH));
        h.assertTrue(sides(slot(h, 3, 2, 2, PipeLine.WASTE)) == bits(Direction.SOUTH), "waste south: " + slot(h, 3, 2, 2, PipeLine.WASTE));
        // each line reaches only its own: the intake the east tank, the return the tank above
        var in = FilterLines.reach(level, abs(h, 1, 2, 2), PipeLine.INTAKE).docks();
        var ret = FilterLines.reach(level, abs(h, 3, 4, 2), PipeLine.RETURN).docks();
        h.assertTrue(in.equals(List.of(new FilterLines.Dock(abs(h, 6, 2, 2), Direction.WEST))), "intake through the bundle to the east tank: " + in);
        h.assertTrue(ret.equals(List.of(new FilterLines.Dock(abs(h, 3, 5, 2), Direction.DOWN))), "return to the tank above: " + ret);
        h.assertTrue(FilterLines.reach(level, abs(h, 3, 2, 0), PipeLine.FRESH).docks().isEmpty(), "the fresh line never docks on a tank");
        h.assertTrue(east != above, "two tanks");
        // a T-piece on the intake line only: it joins the inlet pipe below too
        pipes(h, ModBlocks.FILTER_INLET_PIPE.get(), new int[]{3, 1, 2});
        h.assertTrue(!FilterPipes.joined(slot(h, 3, 1, 2, PipeLine.INTAKE), Direction.UP), "the intake line of the bundle is full");
        h.assertTrue(FilterPipeBundleBlock.makeJunction(level, b, PipeLine.INTAKE), "T-piece on the intake line");
        h.assertTrue(FilterPipes.count(slot(h, 3, 2, 2, PipeLine.INTAKE)) == 3 && FilterPipes.joined(slot(h, 3, 1, 2, PipeLine.INTAKE), Direction.UP),
                "it branches down: " + slot(h, 3, 2, 2, PipeLine.INTAKE));
        h.assertTrue(!FilterPipes.junction(slot(h, 3, 2, 2, PipeLine.RETURN)), "the return line has none");
        // breaking it would give the four pipes and the T-piece
        List<ItemStack> drops = Block.getDrops(level.getBlockState(b), level, b, level.getBlockEntity(b));
        h.assertTrue(drops.size() == 5 && drops.stream().anyMatch(s -> s.is(ModItems.PIPE_TEE.get())) && drops.stream().anyMatch(s -> s.is(ModItems.FILTER_WASTE_PIPE.get())),
                "drops: " + drops);
        // sneaking with an empty hand on the waste arm (south, low, east of the middle) takes the waste line out
        ServerPlayer p = MockPlayers.serverPlayer(h);
        p.getAbilities().instabuild = false;
        p.setShiftKeyDown(true);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.getInventory().clearContent();
        Vec3 hit = Vec3.atLowerCornerOf(b).add(10.5 / 16, 5.5 / 16, 15.0 / 16);
        level.getBlockState(b).useWithoutItem(level, p, new BlockHitResult(hit, Direction.SOUTH, b, false));
        h.assertTrue(!FilterPipes.lines(level, b).contains(PipeLine.WASTE) && FilterPipes.lines(level, b).size() == 3, "the waste line is out: " + FilterPipes.lines(level, b));
        h.assertTrue(p.getInventory().countItem(ModItems.FILTER_WASTE_PIPE.get()) == 1, "its pipe back");
        h.assertTrue(!FilterPipes.joined(slot(h, 3, 2, 3, PipeLine.WASTE), Direction.NORTH), "the waste pipe lets go of the block");
        // down to one line: a single inlet pipe again, with its sides and its T-piece
        h.assertTrue(FilterPipeBundleBlock.removeLine(level, b, PipeLine.FRESH).size() == 1, "the fresh pipe back");
        h.assertTrue(FilterPipeBundleBlock.removeLine(level, b, PipeLine.RETURN).size() == 1, "the return pipe back");
        BlockState single = level.getBlockState(b);
        h.assertTrue(single.is(ModBlocks.FILTER_INLET_PIPE.get()) && single.getValue(FilterPipeBlock.JUNCTION), "a single inlet pipe with its T-piece: " + single);
        h.assertTrue(sides(FilterPipeBlock.slot(single)) == bits(Direction.WEST, Direction.EAST, Direction.DOWN), "its sides stay: " + single);
        h.assertTrue(!FilterPipes.joined(slot(h, 3, 3, 2, PipeLine.RETURN), Direction.DOWN), "the return pipe lets go");
        h.assertTrue(FilterLines.reach(level, abs(h, 1, 2, 2), PipeLine.INTAKE).docks().size() == 1, "the intake line still reaches its tank");
        // by hand: the outlet pipe used on it makes a bundle again, the return line joins its pipe above
        p.setShiftKeyDown(false);
        ItemStack outlet = new ItemStack(ModItems.FILTER_OUTLET_PIPE.get(), 2);
        p.setItemInHand(InteractionHand.MAIN_HAND, outlet);
        single.useItemOn(outlet, level, p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(b), Direction.UP, b, false));
        h.assertTrue(level.getBlockState(b).getBlock() instanceof FilterPipeBundleBlock && outlet.getCount() == 1, "a bundle again, one pipe used");
        h.assertTrue(FilterPipes.joined(slot(h, 3, 2, 2, PipeLine.RETURN), Direction.UP) && FilterPipes.joined(slot(h, 3, 3, 2, PipeLine.RETURN), Direction.DOWN),
                "the return line joins its pipe again");
        h.assertTrue(FilterPipes.junction(slot(h, 3, 2, 2, PipeLine.INTAKE)), "the intake keeps its T-piece");
        h.succeed();
    }

    /**
     * A bundle of two lines carries a filter's intake and return line into the same glass of a tank (both dock on one
     * spot); with the guards the filter cleans the tank as with separate pipes.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void bundleCarriesBothFilterLinesToATank(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MultiblockFilter.forget();
        InteriorPlumbing.forget();
        FilterPumpHeadBlockEntity head = FilterLinesGameTests.filter(h, new BlockPos(0, 2, 4), Direction.EAST);
        pipes(h, ModBlocks.FILTER_INLET_PIPE.get(), new int[]{2, 2, 4});
        pipes(h, ModBlocks.FILTER_OUTLET_PIPE.get(), new int[]{2, 3, 4}, new int[]{3, 3, 4});
        TankBlockEntity t = PlumbingGameTests.tank(h, 4, 2, 4, 2, 1, 2);
        pipes(h, ModBlocks.FILTER_INLET_PIPE.get(), new int[]{3, 2, 4});
        h.assertTrue(FilterPipeBundleBlock.addLine(level, abs(h, 3, 2, 4), PipeLine.RETURN, null, null), "the return line into the inlet pipe");
        h.assertTrue(sides(slot(h, 3, 2, 4, PipeLine.RETURN)) == bits(Direction.UP, Direction.EAST), "return: from above, into the tank: " + slot(h, 3, 2, 4, PipeLine.RETURN));
        h.assertTrue(sides(slot(h, 3, 2, 4, PipeLine.INTAKE)) == bits(Direction.WEST, Direction.EAST), "intake: from the pipe, into the tank");
        var links = MultiblockFilter.links(level, MultiblockFilter.scan(level, head.getBlockPos()));
        h.assertTrue(links.size() == 1 && links.get(0).intakes().equals(links.get(0).returns()), "both lines dock on the same spot: " + links);
        FilterLinesGameTests.guardAll(level, head.getBlockPos());
        double w0 = TankWater.filterBlocks(level, t);
        int moved = head.run(level, true);
        h.assertTrue(moved > 0 && head.tanks().get(0).status() == MultiblockFilter.LinkStatus.OK, "it filters: " + head.tanks());
        h.assertTrue(PlumbingGameTests.near(TankWater.filterBlocks(level, t) - w0, 36, 0.01), "36 water blocks: " + (TankWater.filterBlocks(level, t) - w0));
        h.succeed();
    }

    // ------------------------------------------------------------------ the filter with two tanks and its water ports

    /** A filter (chambers (10,2,4) and (10,3,4), head above) with tank A (8 blocks) and tank B (16 blocks), both guarded. */
    private record Rig(FilterPumpHeadBlockEntity head, TankBlockEntity a, TankBlockEntity b) {}

    private static Rig rig(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MultiblockFilter.forget();
        InteriorPlumbing.forget();
        TankBlockEntity a = PlumbingGameTests.tank(h, 0, 2, 0, 2, 2, 2);
        TankBlockEntity b = PlumbingGameTests.tank(h, 4, 2, 0, 4, 2, 2);
        FilterPumpHeadBlockEntity head = FilterLinesGameTests.filter(h, new BlockPos(10, 2, 4), Direction.WEST);
        Block in = ModBlocks.FILTER_INLET_PIPE.get(), out = ModBlocks.FILTER_OUTLET_PIPE.get();
        pipes(h, in, new int[]{0, 2, 2}, new int[]{0, 2, 3}, new int[]{4, 2, 2}, new int[]{4, 2, 3});
        pipes(h, out, new int[]{1, 3, 2}, new int[]{1, 3, 3}, new int[]{5, 3, 2}, new int[]{5, 3, 3});
        for (int x = 0; x <= 8; x++) pipes(h, in, new int[]{x, 2, 4});
        for (int x = 1; x <= 8; x++) pipes(h, out, new int[]{x, 3, 4});
        FilterPipeBlock.makeJunction(level, abs(h, 4, 2, 4));
        FilterPipeBlock.makeJunction(level, abs(h, 5, 3, 4));
        h.setBlock(new BlockPos(11, 2, 4), ModBlocks.FILTER_FRESH_PORT.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.EAST));
        head.rescan();
        if (MultiblockFilter.links(level, head.structure(level)).size() != 2) throw new GameTestAssertException("the filter does not reach both tanks");
        return new Rig(head, a, b);
    }

    private static FilterPortBlockEntity port(GameTestHelper h, int x, int y, int z) {
        if (!(h.getLevel().getBlockEntity(abs(h, x, y, z)) instanceof FilterPortBlockEntity p)) throw new GameTestAssertException("no port at " + x + " " + y + " " + z);
        return p;
    }

    private static LevelSensorBlockEntity sensor(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, ModBlocks.LEVEL_SENSOR.get().defaultBlockState());
        return (LevelSensorBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel));
    }

    private static WaterChangerBlockEntity changer(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, ModBlocks.WATER_CHANGE_CONTROLLER.get().defaultBlockState());
        return (WaterChangerBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel));
    }

    /**
     * Fresh water by a water pump: it pumps from a reservoir into the fresh water line, which fills the fresh water port.
     * The level sensor on the filter (it touches a chamber, no tank) finds no tank while the guards are missing, then tops
     * off both tanks from the port; short of water, each gets its part by what it misses.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void freshWaterByAPumpTopsOffEveryTank(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Rig r = rig(h);
        WaterPumpBlockEntity pump = PlumbingGameTests.pump(h, new BlockPos(14, 2, 4), Direction.WEST);
        ReservoirBlockEntity res = PlumbingGameTests.reservoir(h, new BlockPos(15, 2, 4), 16000, null);
        pipes(h, ModBlocks.FILTER_FRESH_PIPE.get(), new int[]{12, 2, 4}, new int[]{13, 2, 4});
        h.assertTrue(sides(slot(h, 12, 2, 4, PipeLine.FRESH)) == bits(Direction.WEST, Direction.EAST), "the fresh pipe joins its port");
        h.assertTrue(sides(slot(h, 13, 2, 4, PipeLine.FRESH)) == bits(Direction.WEST, Direction.EAST), "and the pump");
        FilterPortBlockEntity fresh = port(h, 11, 2, 4);
        for (int i = 0; i < 5; i++) PlumbingGameTests.batch(h, pump);
        h.assertTrue(fresh.water().amount() == FilterPortBlockEntity.CAPACITY && res.water().amount() == 16000 - FilterPortBlockEntity.CAPACITY,
                "the pump filled the port through the line: " + fresh.water().amount() + ", reservoir " + res.water().amount());
        IFluidHandler outside = org.overengineer.bettacraft.platform.Platform.get().fluidHandler(level, abs(h, 11, 2, 4), Direction.EAST);
        h.assertTrue(outside != null && outside.drain(100, IFluidHandler.FluidAction.SIMULATE).isEmpty(), "nothing comes out of the fresh port");
        TankInteractions.changeWater(level, r.a(), -600);
        TankInteractions.changeWater(level, r.b(), -1500);
        LevelSensorBlockEntity s = sensor(h, new BlockPos(10, 2, 5));
        h.assertTrue(s.check(level) == 0 && s.status() == LevelSensorBlockEntity.Status.NO_TANK, "no guards: no tank of the filter takes water: " + s.status());
        FilterLinesGameTests.guardAll(level, r.head().getBlockPos());
        int added = s.check(level);
        h.assertTrue(added == 1600 && s.filterTanks() == 2, "600 into A, 1000 (one step) into B: " + added + ", " + s.filterTanks() + " tanks");
        h.assertTrue(r.a().getTank().waterMb() == 8000 && r.b().getTank().waterMb() == 16000 - 500, "A full, B nearly");
        h.assertTrue(fresh.water().amount() == FilterPortBlockEntity.CAPACITY - 1600, "from the port");
        h.assertTrue(s.check(level) == 500 && r.b().getTank().waterMb() == 16000, "B full on the next step");
        h.assertTrue(s.check(level) == 0 && s.status() == LevelSensorBlockEntity.Status.IDLE, "full: idle");
        // short: 300 mB for 2 x 1000 missing: 150 each
        fresh.water().drain(fresh.water().amount() - 300, IFluidHandler.FluidAction.EXECUTE);
        TankInteractions.changeWater(level, r.a(), -1000);
        TankInteractions.changeWater(level, r.b(), -1000);
        h.assertTrue(s.check(level) == 300, "all there was");
        h.assertTrue(r.a().getTank().waterMb() == 7150 && r.b().getTank().waterMb() == 15150, "shared by need: " + r.a().getTank().waterMb() + " " + r.b().getTank().waterMb());
        h.succeed();
    }

    /**
     * Fresh water from the pipes of another block: the fresh water line ends at a glass pipe that leads to a reservoir; the
     * level sensor on the filter pulls from there (no pump, no water in the port).
     */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void freshWaterFromAnotherPipe(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Rig r = rig(h);
        FilterLinesGameTests.guardAll(level, r.head().getBlockPos());
        ReservoirBlockEntity res = PlumbingGameTests.reservoir(h, new BlockPos(15, 2, 4), 4000, null);
        PlumbingGameTests.pipe(h, true, new BlockPos(14, 2, 4)); // steel: 2500 mB per window
        pipes(h, ModBlocks.FILTER_FRESH_PIPE.get(), new int[]{12, 2, 4}, new int[]{13, 2, 4});
        h.assertTrue(FilterPipes.joined(slot(h, 13, 2, 4, PipeLine.FRESH), Direction.EAST), "the fresh pipe joins the steel pipe");
        h.assertTrue(level.getBlockState(abs(h, 14, 2, 4)).getValue(org.overengineer.bettacraft.plumbing.PipeBlock.SIDES.get(Direction.WEST)), "and the steel pipe the fresh pipe");
        TankInteractions.changeWater(level, r.a(), -400);
        TankInteractions.changeWater(level, r.b(), -800);
        LevelSensorBlockEntity s = sensor(h, new BlockPos(10, 2, 5));
        int added = s.check(level);
        h.assertTrue(added == 1200 && res.water().amount() == 2800, "both topped off from the reservoir behind the steel pipe: " + added + ", " + res.water().amount());
        h.assertTrue(port(h, 11, 2, 4).water().amount() == 0, "straight, not through the port");
        h.succeed();
    }

    /**
     * The water change controller on the filter changes 25 % of both tanks (2000 and 4000 mB) from endless tap water (a
     * water source block next to the fresh port). Without a waste port the old water is discarded; with one (its line to a
     * reservoir) the reservoir gets the old water. Short of fresh water (3000 mB in the port), each tank gets its part by
     * its share: both lose an eighth of their nitrate. With nothing at the end of the waste line, the port keeps the old
     * water, and a pipe pulling from the waste line gets it.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void waterChangeThroughTheFilter(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Rig r = rig(h);
        FilterLinesGameTests.guardAll(level, r.head().getBlockPos());
        WaterChangerBlockEntity wc = changer(h, new BlockPos(10, 3, 5));
        wc.set(7, 25, true);
        h.assertTrue(wc.check(level, true) == 0 && wc.status() == WaterChangerBlockEntity.Status.NO_WATER, "no fresh water: " + wc.status());
        h.setBlock(new BlockPos(12, 2, 4), Blocks.WATER); // tap water at the fresh port
        r.a().getTank().setNitrate(40);
        r.b().getTank().setNitrate(40);
        int done = wc.check(level, true);
        h.assertTrue(done == 6000 && wc.filterTanks() == 2, "25 % of both: " + done + ", " + wc.filterTanks());
        h.assertTrue(r.a().getTank().waterMb() == 8000 && r.b().getTank().waterMb() == 16000, "the volume stays");
        h.assertTrue(PlumbingGameTests.near(r.a().getTank().nitrate(), 30, 0.01) && PlumbingGameTests.near(r.b().getTank().nitrate(), 30, 0.01),
                "both a quarter fresh: " + r.a().getTank().nitrate() + " " + r.b().getTank().nitrate());
        h.assertTrue(wc.lastWasteMb() == 0, "no waste port: the old water is discarded");
        // a waste port with its line to a reservoir
        h.setBlock(new BlockPos(11, 3, 4), ModBlocks.FILTER_WASTE_PORT.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.EAST));
        ReservoirBlockEntity waste = PlumbingGameTests.reservoir(h, new BlockPos(14, 3, 4), 0, null);
        pipes(h, ModBlocks.FILTER_WASTE_PIPE.get(), new int[]{12, 3, 4}, new int[]{13, 3, 4});
        r.head().rescan();
        r.a().getTank().setNitrate(40);
        r.b().getTank().setNitrate(40);
        h.assertTrue(wc.check(level, true) == 6000 && wc.lastWasteMb() == 6000 && waste.water().amount() == 6000, "the old water into the reservoir: " + waste.water().amount());
        WaterValues old = waste.water().values();
        h.assertTrue(old != null && old.nitrate() > 35, "it is the old water: " + old);
        // short of fresh water: 3000 mB in the port
        h.setBlock(new BlockPos(12, 2, 4), Blocks.AIR);
        port(h, 11, 2, 4).water().fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 3000), IFluidHandler.FluidAction.EXECUTE);
        r.a().getTank().setNitrate(40);
        r.b().getTank().setNitrate(40);
        h.assertTrue(wc.check(level, true) == 3000 && port(h, 11, 2, 4).water().amount() == 0, "all of the port");
        h.assertTrue(PlumbingGameTests.near(r.a().getTank().nitrate(), 35, 0.01) && PlumbingGameTests.near(r.b().getTank().nitrate(), 35, 0.01),
                "1000 + 2000: by share, both an eighth fresh: " + r.a().getTank().nitrate() + " " + r.b().getTank().nitrate());
        // nothing at the end of the waste line: the port keeps it for pipes that pull
        h.setBlock(new BlockPos(14, 3, 4), Blocks.AIR);
        h.setBlock(new BlockPos(12, 2, 4), Blocks.WATER);
        int before = port(h, 11, 3, 4).water().amount();
        h.assertTrue(wc.check(level, true) == 6000 && port(h, 11, 3, 4).water().amount() == Math.min(FilterPortBlockEntity.CAPACITY, before + 6000),
                "kept in the waste port: " + port(h, 11, 3, 4).water().amount());
        IFluidHandler line = org.overengineer.bettacraft.platform.Platform.get().fluidHandler(level, abs(h, 13, 3, 4), Direction.EAST);
        h.assertTrue(line != null && line.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 100), IFluidHandler.FluidAction.SIMULATE) == 0,
                "nothing goes into the waste line");
        FluidStack pulled = line.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(pulled.getAmount() == 1000, "a pipe pulls the old water from the waste line: " + pulled.getAmount());
        h.succeed();
    }

    /** features.pipes.filterWater off: the devices on the filter work as before (the sensor its first tank, the changer none), the fresh line takes nothing (own batch). */
    @GameTest(template = EMPTY, batch = "bettacraft_m40_water_off", timeoutTicks = 300)
    public static void filterWaterOff(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        boolean saved = Features.own(Feature.FILTER_WATER);
        try {
            Features.set(Feature.FILTER_WATER, false);
            Rig r = rig(h);
            FilterLinesGameTests.guardAll(level, r.head().getBlockPos());
            r.head().run(level, true);
            pipes(h, ModBlocks.FILTER_FRESH_PIPE.get(), new int[]{12, 2, 4}, new int[]{13, 2, 4});
            IFluidHandler line = org.overengineer.bettacraft.platform.Platform.get().fluidHandler(level, abs(h, 13, 2, 4), Direction.EAST);
            h.assertTrue(line == null || line.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0,
                    "the fresh line takes nothing");
            h.setBlock(new BlockPos(11, 2, 5), Blocks.WATER); // tap water next to the sensor
            TankInteractions.changeWater(level, r.a(), -500);
            TankInteractions.changeWater(level, r.b(), -500);
            LevelSensorBlockEntity s = sensor(h, new BlockPos(10, 2, 5));
            int added = s.check(level);
            h.assertTrue(added == 500 && s.filterTanks() == 0, "as before: only the first tank of the filter: " + added);
            WaterChangerBlockEntity wc = changer(h, new BlockPos(10, 3, 5));
            h.assertTrue(wc.check(level, true) == 0 && wc.status() == WaterChangerBlockEntity.Status.NO_TANK, "the changer on the filter: no tank, as before");
        } finally {
            Features.set(Feature.FILTER_WATER, saved);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ other mods (-Ppower, -Pstorage)

    private static boolean loaded(String... mods) {
        for (String m : mods) if (!org.overengineer.bettacraft.platform.Platform.get().isModLoaded(m)) return false;
        return true;
    }

    private static Block block(String id) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.ResourceLocation.parse(id))
                .orElseThrow(() -> new GameTestAssertException("no block " + id));
    }

    /** The fresh water port of a rig with a fresh pipe at (12..13, 2, 4) and the foreign block at (14, 2, 4); a reservoir with water at (15, 2, 4). */
    private static FilterPortBlockEntity foreignRig(GameTestHelper h, Block foreign) {
        rig(h);
        PlumbingGameTests.reservoir(h, new BlockPos(15, 2, 4), 16000, null);
        h.setBlock(new BlockPos(14, 2, 4), foreign.defaultBlockState());
        pipes(h, ModBlocks.FILTER_FRESH_PIPE.get(), new int[]{12, 2, 4}, new int[]{13, 2, 4});
        return port(h, 11, 2, 4);
    }

    /** Pipez: a fluid pipe extracting from a reservoir pushes into our fresh water line. */
    @GameTest(template = EMPTY, batch = "bettacraft_power_pipez_water", timeoutTicks = 300)
    public static void pipezPipeFillsTheFreshLine(GameTestHelper h) {
        if (!loaded("pipez")) {
            h.succeed();
            return;
        }
        BlockState pz = block("pipez:fluid_pipe").defaultBlockState();
        for (var pr : pz.getProperties()) if (pr.getName().equals("has_data")) pz = pz.setValue((net.minecraft.world.level.block.state.properties.BooleanProperty) pr, true);
        rig(h);
        PlumbingGameTests.reservoir(h, new BlockPos(15, 2, 4), 16000, null);
        h.setBlock(new BlockPos(14, 2, 4), pz);
        pipes(h, ModBlocks.FILTER_FRESH_PIPE.get(), new int[]{12, 2, 4}, new int[]{13, 2, 4});
        FilterPortBlockEntity fresh = port(h, 11, 2, 4);
        try {
            Object pipe = h.getLevel().getBlockEntity(abs(h, 14, 2, 4));
            pipe.getClass().getMethod("setExtracting", Direction.class, boolean.class).invoke(pipe, Direction.EAST, true);
        } catch (ReflectiveOperationException | NullPointerException e) {
            throw new GameTestAssertException("Pipez API changed: " + e);
        }
        h.succeedWhen(() -> h.assertTrue(fresh.water().amount() > 0, "the Pipez pipe fills the fresh port: " + fresh.water().amount()));
    }

    /** EnderIO: a fluid conduit extracting from a reservoir pushes into our fresh water line. */
    @GameTest(template = EMPTY, batch = "bettacraft_power_enderio_water", timeoutTicks = 300)
    public static void enderioConduitFillsTheFreshLine(GameTestHelper h) {
        if (!loaded("enderio")) {
            h.succeed();
            return;
        }
        rig(h);
        ServerLevel level = h.getLevel();
        PlumbingGameTests.reservoir(h, new BlockPos(15, 2, 4), 16000, null);
        try {
            var reg = level.registryAccess().registryOrThrow(net.minecraft.resources.ResourceKey.createRegistryKey(net.minecraft.resources.ResourceLocation.parse("enderio:conduit")));
            net.minecraft.core.Holder<?> fluid = reg.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(reg.key(), net.minecraft.resources.ResourceLocation.parse("enderio:fluid")));
            h.setBlock(new BlockPos(14, 2, 4), block("enderio:conduit"));
            Object be = level.getBlockEntity(abs(h, 14, 2, 4));
            be.getClass().getMethod("addConduit", net.minecraft.core.Holder.class, Direction.class, net.minecraft.world.entity.player.Player.class)
                    .invoke(be, fluid, Direction.UP, MockPlayers.serverPlayer(h));
            pipes(h, ModBlocks.FILTER_FRESH_PIPE.get(), new int[]{12, 2, 4}, new int[]{13, 2, 4});
            var get = be.getClass().getMethod("getConnectionConfig", net.minecraft.core.Holder.class, Direction.class);
            var set = be.getClass().getMethod("setConnectionConfig", net.minecraft.core.Holder.class, Direction.class,
                    Class.forName("com.enderio.enderio.api.conduits.connection.config.ConnectionConfig"));
            Object east = get.invoke(be, fluid, Direction.EAST);
            Object extract = east.getClass().getMethod("withIsExtract", boolean.class).invoke(east, true);
            Class<?> rc = Class.forName("com.enderio.enderio.api.io.RedstoneControl");
            extract = extract.getClass().getMethod("withExtractRedstoneControl", rc).invoke(extract, rc.getField("ALWAYS_ACTIVE").get(null));
            extract = extract.getClass().getMethod("withIsInsert", boolean.class).invoke(extract, false);
            set.invoke(be, fluid, Direction.EAST, extract);
            Object west = get.invoke(be, fluid, Direction.WEST);
            Object insert = west.getClass().getMethod("withIsInsert", boolean.class).invoke(west, true);
            set.invoke(be, fluid, Direction.WEST, insert);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new GameTestAssertException("EnderIO API changed: " + e);
        }
        FilterPortBlockEntity fresh = port(h, 11, 2, 4);
        Object be = level.getBlockEntity(abs(h, 14, 2, 4));
        h.succeedWhen(() -> {
            String why = "";
            try {
                var reg = level.registryAccess().registryOrThrow(net.minecraft.resources.ResourceKey.createRegistryKey(net.minecraft.resources.ResourceLocation.parse("enderio:conduit")));
                Object fluid = reg.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(reg.key(), net.minecraft.resources.ResourceLocation.parse("enderio:fluid")));
                var st = be.getClass().getMethod("getConnectionStatus", net.minecraft.core.Holder.class, Direction.class);
                var cf = be.getClass().getMethod("getConnectionConfig", net.minecraft.core.Holder.class, Direction.class);
                why = "east " + st.invoke(be, fluid, Direction.EAST) + " " + cf.invoke(be, fluid, Direction.EAST) + ", west " + st.invoke(be, fluid, Direction.WEST) + " " + cf.invoke(be, fluid, Direction.WEST);
            } catch (ReflectiveOperationException | RuntimeException e) {
                why = e.toString();
            }
            h.assertTrue(fresh.water().amount() > 0, "the EnderIO conduit fills the fresh port: " + fresh.water().amount() + "; " + why);
        });
    }

    /** Mekanism: a mechanical pipe pulling from a reservoir pushes into our fresh water line, and the line joins the pipe. */
    @GameTest(template = EMPTY, batch = "bettacraft_power_mekanism_water", timeoutTicks = 300)
    public static void mekanismPipeFillsTheFreshLine(GameTestHelper h) {
        if (!loaded("mekanism")) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        FilterPortBlockEntity fresh = foreignRig(h, block("mekanism:basic_mechanical_pipe"));
        try {
            Object tile = level.getBlockEntity(abs(h, 14, 2, 4));
            Object transmitter = tile.getClass().getMethod("getTransmitter").invoke(tile);
            Class<?> type = Class.forName("mekanism.common.lib.transmitter.ConnectionType");
            Object pull = type.getField("PULL").get(null);
            transmitter.getClass().getMethod("setConnectionTypeRaw", Direction.class, type).invoke(transmitter, Direction.EAST, pull);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new GameTestAssertException("Mekanism API changed: " + e);
        }
        h.succeedWhen(() -> {
            h.assertTrue(FilterPipes.joined(slot(h, 13, 2, 4, PipeLine.FRESH), Direction.EAST), "our fresh pipe joins the Mekanism pipe");
            h.assertTrue(fresh.water().amount() > 0, "Mekanism pulls from the reservoir into the fresh port: " + fresh.water().amount());
        });
    }

    /** AE2: an export bus with water in its config exports from the network (a storage bus on a reservoir) into the fresh water line. */
    @GameTest(template = EMPTY, batch = "bettacraft_storage_water", timeoutTicks = 600)
    public static void ae2ExportBusFillsTheFreshLine(GameTestHelper h) throws Exception {
        if (!loaded("ae2")) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        rig(h);
        PlumbingGameTests.reservoir(h, new BlockPos(16, 2, 4), 16000, null);
        pipes(h, ModBlocks.FILTER_FRESH_PIPE.get(), new int[]{12, 2, 4}, new int[]{13, 2, 4});
        java.lang.reflect.Method set = null;
        for (java.lang.reflect.Method m : Class.forName("appeng.api.parts.PartHelper").getMethods()) if (m.getName().equals("setPart") && m.getParameterCount() == 5) set = m;
        if (set == null) throw new GameTestAssertException("AE2 API changed: no PartHelper.setPart");
        BlockPos a = abs(h, 14, 2, 4), b = abs(h, 15, 2, 4);
        net.minecraft.world.item.Item cable = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("ae2:fluix_glass_cable"));
        net.minecraft.world.item.Item export = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("ae2:export_bus"));
        net.minecraft.world.item.Item storage = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("ae2:storage_bus"));
        set.invoke(null, level, a, null, null, cable);
        set.invoke(null, level, b, null, null, cable);
        Object bus = set.invoke(null, level, a, Direction.WEST, null, export);
        set.invoke(null, level, b, Direction.EAST, null, storage);
        if (bus == null) throw new GameTestAssertException("AE2 did not place the export bus");
        java.lang.reflect.Method getConfig = bus.getClass().getMethod("getConfig");
        getConfig.setAccessible(true);
        Object config = getConfig.invoke(bus);
        Object key = Class.forName("appeng.api.stacks.AEFluidKey").getMethod("of", net.minecraft.world.level.material.Fluid.class).invoke(null, net.minecraft.world.level.material.Fluids.WATER);
        Class<?> aeKey = Class.forName("appeng.api.stacks.AEKey");
        Object stack = Class.forName("appeng.api.stacks.GenericStack").getConstructor(aeKey, long.class).newInstance(key, 1L);
        for (java.lang.reflect.Method m : config.getClass().getMethods()) {
            if (m.getName().equals("setStack") && m.getParameterCount() == 2) {
                m.setAccessible(true);
                m.invoke(config, 0, stack);
            }
        }
        h.setBlock(new BlockPos(14, 3, 4), block("ae2:creative_energy_cell"));
        FilterPortBlockEntity fresh = port(h, 11, 2, 4);
        h.succeedWhen(() -> h.assertTrue(fresh.water().amount() > 0, "the AE2 export bus fills the fresh port: " + fresh.water().amount()));
    }

    /** Refined Storage: an importer next to the waste water line pulls the old water out of the waste port into the network. */
    @GameTest(template = EMPTY, batch = "bettacraft_storage_water", timeoutTicks = 600)
    public static void refinedStorageImporterEmptiesTheWasteLine(GameTestHelper h) {
        if (!loaded("refinedstorage")) {
            h.succeed();
            return;
        }
        rig(h);
        h.setBlock(new BlockPos(11, 3, 4), ModBlocks.FILTER_WASTE_PORT.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.EAST));
        pipes(h, ModBlocks.FILTER_WASTE_PIPE.get(), new int[]{12, 3, 4}, new int[]{13, 3, 4});
        FilterPortBlockEntity waste = port(h, 11, 3, 4);
        waste.water().fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
        BlockState importer = block("refinedstorage:importer").defaultBlockState();
        for (var p : importer.getProperties()) {
            if (p.getName().equals("direction")) importer = with(importer, p, "west");
        }
        h.setBlock(new BlockPos(14, 3, 4), importer);
        h.setBlock(new BlockPos(15, 3, 4), block("refinedstorage:creative_controller").defaultBlockState());
        h.setBlock(new BlockPos(15, 4, 4), block("refinedstorage:creative_fluid_storage_block").defaultBlockState()); // fluids need a fluid storage
        h.succeedWhen(() -> h.assertTrue(waste.water().amount() < 4000, "the RS importer pulls from the waste line: " + waste.water().amount()));
    }

    private static <T extends Comparable<T>> BlockState with(BlockState s, net.minecraft.world.level.block.state.properties.Property<T> p, String value) {
        return p.getValue(value).map(v -> s.setValue(p, v)).orElse(s);
    }
}
