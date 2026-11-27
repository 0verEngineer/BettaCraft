package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity;
import org.overengineer.bettacraft.filter.MultiblockFilter;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.plumbing.LevelSensorBlockEntity;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.PipeBundleBlock;
import org.overengineer.bettacraft.plumbing.PipeNetwork;
import org.overengineer.bettacraft.plumbing.PipePorts;
import org.overengineer.bettacraft.plumbing.PipeRing;
import org.overengineer.bettacraft.plumbing.PipeRole;
import org.overengineer.bettacraft.plumbing.PipeUpdates;
import org.overengineer.bettacraft.plumbing.PipeWrenchItem;
import org.overengineer.bettacraft.plumbing.ReservoirBlockEntity;
import org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity;
import org.overengineer.bettacraft.plumbing.WaterDeviceBlock;
import org.overengineer.bettacraft.plumbing.WaterPumpBlockEntity;
import org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing;
import org.overengineer.bettacraft.plumbing.interior.Plumb;
import org.overengineer.bettacraft.plumbing.interior.TankAnchors;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankNitrate;
import org.overengineer.bettacraft.tank.WaterValues;

/**
 * M68 part B GameTests: the water change controller with its four pipe ports (fresh water in and out, waste water in and out), the
 * level sensor that only measures, the drain. Against a tank two pipes do (the touch stands for the tank-side ports); free-standing
 * all four lines are pipes and a step moves what the weakest pipe carries; both tank-side lines may enter one tank block (a bundle
 * in one face, two faces of an edge block); top-off through the fresh water line with a sensor (fresh water only: the salinity comes
 * back), a change without a sensor tops nothing off; the sensor gives a redstone signal and moves no water; nothing comes from
 * what merely stands next to the controller; the drain discards, a reservoir keeps the old water with its values; a multiblock
 * filter's tanks through the touch or through lines that end at a part of the filter; the waste water dock sucks in young animals
 * by the drained share without an intake guard (never snails or adults); a short circuit of its ports; the drain's switch off.
 * Deterministic: the steps are called directly or the controller's own steps are awaited ({@code succeedWhen}).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WaterChangeGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private WaterChangeGameTests() {}

    // ------------------------------------------------------------------ helpers

    static BlockPos abs(GameTestHelper h, int x, int y, int z) {
        return h.absolutePos(new BlockPos(x, y, z));
    }

    static WaterChangerBlockEntity changer(GameTestHelper h, int x, int y, int z, Direction facing) {
        h.setBlock(new BlockPos(x, y, z), ModBlocks.WATER_CHANGE_CONTROLLER.get().defaultBlockState().setValue(WaterDeviceBlock.FACING, facing));
        if (!(h.getLevel().getBlockEntity(abs(h, x, y, z)) instanceof WaterChangerBlockEntity w)) throw new GameTestFailure("no water change controller");
        return w;
    }

    static LevelSensorBlockEntity sensor(GameTestHelper h, int x, int y, int z) {
        h.setBlock(new BlockPos(x, y, z), ModBlocks.LEVEL_SENSOR.get().defaultBlockState());
        if (!(h.getLevel().getBlockEntity(abs(h, x, y, z)) instanceof LevelSensorBlockEntity s)) throw new GameTestFailure("no level sensor");
        return s;
    }

    static void pipes(GameTestHelper h, boolean steel, int[]... at) {
        Block b = steel ? ModBlocks.STAINLESS_STEEL_PIPE.get() : ModBlocks.GLASS_PIPE.get();
        for (int[] p : at) h.setBlock(new BlockPos(p[0], p[1], p[2]), b.defaultBlockState());
    }

    /** Closes the sides of the two neighbouring single pipes at {@code a} and {@code b} towards each other (the wrench). */
    static void apart(GameTestHelper h, int[] a, int[] b) {
        BlockPos pa = new BlockPos(a[0], a[1], a[2]), pb = new BlockPos(b[0], b[1], b[2]);
        for (Direction d : Direction.values()) {
            if (pa.relative(d).equals(pb)) {
                PipeWrenchItem.setClosed(h.getLevel(), h.absolutePos(pa), -1, d, true);
                PipeWrenchItem.setClosed(h.getLevel(), h.absolutePos(pb), -1, d.getOpposite(), true);
            }
        }
    }

    static PipeRing ring(GameTestHelper h, int x, int y, int z) {
        return h.getLevel().getBlockState(abs(h, x, y, z)).getValue(PipeBlock.RING);
    }

    static PipePorts.Line line(GameTestHelper h, WaterChangerBlockEntity w, PipeRole role) {
        return PipePorts.line(h.getLevel(), w.getBlockPos(), WaterDeviceBlock.changerFace(w.getBlockState().getValue(WaterDeviceBlock.FACING), role));
    }

    static boolean near(double a, double b, double eps) {
        return Math.abs(a - b) <= eps;
    }

    static ReservoirBlockEntity reservoir(GameTestHelper h, int x, int y, int z, int mb) {
        return PlumbingGameTests.reservoir(h, new BlockPos(x, y, z), mb, null);
    }

    /** Takes {@code mb} of the water out as evaporation does it (salt and nitrate stay behind). */
    static void evaporate(ServerLevel level, TankBlockEntity c, long mb) {
        TankData t = c.getTank();
        float salt = Salinity.concentrate(t.salinity(), t.waterMb(), mb, Salinity.MAX);
        float no3 = TankNitrate.concentrate(t.nitrate(), t.waterMb(), mb);
        TankInteractions.changeWater(level, c, -mb);
        t.setSalinity(salt);
        t.setNitrate(no3);
    }

    /**
     * The contact layout: a 2x2x2 tank at (2..3, 2..3, 2..3), the controller south of it at (2,2,4) facing south (its back, the fresh
     * water outlet, against the glass), its fresh water inlet west: a pipe at (1,2,4) to a reservoir at (1,2,5); its waste water outlet
     * east: a pipe at (3,2,4) (closed towards the tank) and (4,2,4) to a drain at (5,2,4). Offset by {@code ox} in x.
     */
    record Contact(TankBlockEntity tank, WaterChangerBlockEntity changer, ReservoirBlockEntity fresh) {}

    static Contact contact(GameTestHelper h, int ox, int freshMb) {
        TankBlockEntity c = PlumbingGameTests.tank(h, 2 + ox, 2, 2, 2, 2, 2);
        WaterChangerBlockEntity w = changer(h, 2 + ox, 2, 4, Direction.SOUTH);
        pipes(h, true, new int[]{1 + ox, 2, 4});
        ReservoirBlockEntity r = reservoir(h, 1 + ox, 2, 5, freshMb);
        pipes(h, true, new int[]{3 + ox, 2, 4}, new int[]{4 + ox, 2, 4});
        PipeWrenchItem.setClosed(h.getLevel(), abs(h, 3 + ox, 2, 4), -1, Direction.NORTH, true);
        h.setBlock(new BlockPos(5 + ox, 2, 4), ModBlocks.DRAIN.get().defaultBlockState());
        PipeUpdates.flush(h.getLevel());
        return new Contact(c, w, r);
    }

    // ------------------------------------------------------------------ against a tank: two pipes

    /**
     * Against a tank two pipes do: fresh water from a reservoir through the inlet line, the old water through the outlet line into a
     * drain. The rings show the roles. 25 % of 8000 mB are changed (steel carries it in one step), the nitrate falls by a quarter, the
     * old water is gone; not due again right after. Without a level sensor nothing is topped off.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void changerAtATankWithTwoPipes(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Contact k = contact(h, 0, 4000);
        TankBlockEntity c = k.tank();
        WaterChangerBlockEntity wc = k.changer();
        c.getTank().setNitrate(40);
        var watcher = QuestAdv.watcher(h, c); // M45
        h.assertTrue(ring(h, 1, 2, 4) == PipeRing.FRESH && ring(h, 4, 2, 4) == PipeRing.WASTE, "fresh and waste rings: " + ring(h, 1, 2, 4) + " " + ring(h, 4, 2, 4));
        wc.set(7, 25, true);
        // no level sensor: evaporated water is not topped off
        evaporate(level, c, 600);
        h.assertTrue(wc.check(level, false) == 0 && c.getTank().waterMb() == 7400, "no sensor, no top-off: " + c.getTank().waterMb());
        h.assertTrue(wc.flag(WaterChangerBlockEntity.CONTACT) && wc.flag(WaterChangerBlockEntity.FRESH_LINE) && wc.flag(WaterChangerBlockEntity.WASTE_LINE),
                "touch, fresh and waste line: " + wc.flags());
        TankInteractions.changeWater(level, c, 600);
        c.getTank().setNitrate(40);
        int changed = wc.check(level, true);
        h.assertTrue(changed == 2000 && c.getTank().waterMb() == 8000, "25 % changed: " + changed);
        h.assertTrue(near(c.getTank().nitrate(), 30, 0.01), "the nitrate by a quarter: " + c.getTank().nitrate());
        h.assertTrue(k.fresh().water().amount() == 2000, "from the reservoir: " + k.fresh().water().amount());
        h.assertTrue(wc.wasteStore().amount() == 0, "the old water went down the drain: " + wc.wasteStore().amount());
        h.assertTrue(wc.status() == WaterChangerBlockEntity.Status.CHANGED && wc.lastMb() == 2000, "changed: " + wc.status());
        h.assertTrue(AerationGameTests.logged(c, "log.bettacraft.auto_water_change"), "logged");
        QuestAdv.check(h, watcher, "auto_water_change"); // M45
        QuestAdv.gone(h, watcher);
        h.assertTrue(wc.check(level, false) == 0 && wc.status() == WaterChangerBlockEntity.Status.CHANGED, "not due: " + wc.status());
        h.succeed();
    }

    // ------------------------------------------------------------------ free-standing: four pipes

    /**
     * Free-standing (a technical room): all four lines are glass pipes, the tank-side ones dock at two blocks of the tank. A step
     * moves what glass carries (50 mB/t, 500 mB a step); the controller goes on by itself until the 25 % are changed. The old water
     * goes into a reservoir with its values (the nitrate of the tank).
     */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void changerStandsFreeWithFourPipes(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = PlumbingGameTests.tank(h, 6, 2, 12, 2, 2, 2);
        c.getTank().setNitrate(40);
        WaterChangerBlockEntity wc = changer(h, 8, 2, 8, Direction.NORTH);
        pipes(h, false, new int[]{8, 2, 9}, new int[]{8, 2, 10}, new int[]{8, 2, 11}, new int[]{8, 2, 12}); // fresh out (back, south)
        pipes(h, false, new int[]{8, 3, 8}, new int[]{8, 4, 8}, new int[]{8, 4, 9}, new int[]{8, 4, 10}, new int[]{8, 4, 11}, new int[]{8, 4, 12},
                new int[]{7, 4, 12}); // waste in (top), docks on the top of the tank
        pipes(h, false, new int[]{9, 2, 8}, new int[]{10, 2, 8}); // fresh in (left, east)
        ReservoirBlockEntity fresh = reservoir(h, 11, 2, 8, 4000);
        pipes(h, false, new int[]{7, 2, 8}, new int[]{6, 2, 8}); // waste out (right, west)
        ReservoirBlockEntity old = reservoir(h, 5, 2, 8, 0);
        PipeUpdates.flush(level);
        h.assertTrue(ring(h, 10, 2, 8) == PipeRing.FRESH && ring(h, 8, 2, 10) == PipeRing.FRESH, "fresh rings");
        h.assertTrue(ring(h, 8, 4, 10) == PipeRing.WASTE && ring(h, 6, 2, 8) == PipeRing.WASTE, "waste rings");
        var out = line(h, wc, PipeRole.FRESH_OUT).network();
        var in = line(h, wc, PipeRole.WASTE_IN).network();
        h.assertTrue(out != null && out.role() == PipeRole.FRESH_OUT && out.docks().equals(List.of(new PipeNetwork.Dock(abs(h, 7, 2, 12), Direction.EAST))),
                "the fresh water line docks at the east of (7,2,12): " + (out == null ? null : out.docks()));
        h.assertTrue(in != null && in.role() == PipeRole.WASTE_IN && in.docks().equals(List.of(new PipeNetwork.Dock(abs(h, 7, 3, 12), Direction.UP))),
                "the waste water line docks on top of (7,3,12): " + (in == null ? null : in.docks()));
        wc.set(7, 25, true);
        int first = wc.check(level, true);
        h.assertTrue(first == 500, "glass carries 500 mB a step: " + first);
        h.assertTrue(!wc.flag(WaterChangerBlockEntity.CONTACT) && wc.tanks() == 1, "free-standing, one tank: " + wc.flags());
        h.succeedWhen(() -> {
            h.assertTrue(wc.pendingMb() == 0 && wc.status() == WaterChangerBlockEntity.Status.CHANGED, "the change goes on by itself: " + wc.pendingMb() + " " + wc.status());
            // four steps of 500 mB, fresh water mixed in between: 40 x (15/16)^4 = 30.9
            h.assertTrue(c.getTank().waterMb() == 8000 && near(c.getTank().nitrate(), 30.9, 0.1), "25 % changed in steps: " + c.getTank().nitrate());
            h.assertTrue(fresh.water().amount() == 2000, "fresh water used: " + fresh.water().amount());
            h.assertTrue(old.water().amount() == 2000 && old.water().values() != null && old.water().values().nitrate() > 31 && old.water().values().nitrate() < 40,
                    "the old water in the reservoir with its nitrate: " + old.water().amount() + " " + old.water().values());
        });
    }

    // ------------------------------------------------------------------ both lines into one tank block

    /** The common part of the one-block layouts: the controller at (8,2,8) facing west, its fresh water inlet and the drain. */
    private static WaterChangerBlockEntity westChanger(GameTestHelper h) {
        WaterChangerBlockEntity wc = changer(h, 8, 2, 8, Direction.WEST); // back east: fresh out; top: waste in; left north: fresh in; right south: waste out
        pipes(h, true, new int[]{8, 2, 7}, new int[]{8, 2, 6});
        reservoir(h, 8, 2, 5, 4000);
        pipes(h, true, new int[]{8, 2, 9});
        h.setBlock(new BlockPos(8, 2, 10), ModBlocks.DRAIN.get().defaultBlockState());
        return wc;
    }

    /** Both tank-side lines end in one bundle in front of one face of one tank block; both dock there, the change works. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void bothLinesIntoOneTankBlockAsABundle(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = PlumbingGameTests.tank(h, 12, 2, 7, 2, 2, 2);
        c.getTank().setNitrate(40);
        WaterChangerBlockEntity wc = westChanger(h);
        pipes(h, true, new int[]{9, 2, 8}, new int[]{10, 2, 8}, new int[]{11, 2, 8}); // fresh out
        pipes(h, true, new int[]{8, 3, 8}, new int[]{9, 3, 8}, new int[]{10, 3, 8}, new int[]{11, 3, 8}); // waste in, above it
        apart(h, new int[]{9, 2, 8}, new int[]{9, 3, 8});
        apart(h, new int[]{10, 2, 8}, new int[]{10, 3, 8});
        PipeWrenchItem.setClosed(level, abs(h, 11, 3, 8), -1, Direction.EAST, true); // the waste line goes down into the bundle, not into the glass
        // the bundle at (11,2,8): slot 0 the fresh water line (west), slot 1 the waste water line (from above); both dock east
        h.assertTrue(PipeBundleBlock.addPipe(level, abs(h, 11, 2, 8), true, null, null) == 1, "a second pipe in the block");
        PipeWrenchItem.setClosed(level, abs(h, 11, 2, 8), 0, Direction.UP, true);
        PipeUpdates.flush(level);
        var out = line(h, wc, PipeRole.FRESH_OUT).network();
        var in = line(h, wc, PipeRole.WASTE_IN).network();
        PipeNetwork.Dock dock = new PipeNetwork.Dock(abs(h, 12, 2, 8), Direction.WEST);
        h.assertTrue(out != null && out != in && !out.shortCircuit() && out.docks().equals(List.of(dock)), "fresh water through the bundle: " + (out == null ? null : out.docks()));
        h.assertTrue(in != null && !in.shortCircuit() && in.docks().equals(List.of(dock)), "waste water through the bundle, the same face: " + (in == null ? null : in.docks()));
        wc.set(7, 25, true);
        int changed = wc.check(level, true);
        h.assertTrue(changed == 2000 && near(c.getTank().nitrate(), 30, 0.01), "changed through one face: " + changed + " " + c.getTank().nitrate());
        h.succeed();
    }

    /** Both tank-side lines dock on two outer faces of one edge block of the tank (the side and the top). */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void bothLinesIntoOneEdgeBlock(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = PlumbingGameTests.tank(h, 12, 2, 7, 2, 2, 2);
        c.getTank().setNitrate(40);
        WaterChangerBlockEntity wc = westChanger(h);
        pipes(h, true, new int[]{9, 2, 8}, new int[]{10, 2, 8}, new int[]{10, 3, 8}, new int[]{11, 3, 8}); // fresh out: to the west face of (12,3,8)
        pipes(h, true, new int[]{8, 3, 8}, new int[]{8, 4, 8}, new int[]{9, 4, 8}, new int[]{10, 4, 8}, new int[]{11, 4, 8}, new int[]{12, 4, 8}); // waste in: its top
        apart(h, new int[]{10, 3, 8}, new int[]{10, 4, 8});
        apart(h, new int[]{11, 3, 8}, new int[]{11, 4, 8});
        PipeUpdates.flush(level);
        var out = line(h, wc, PipeRole.FRESH_OUT).network();
        var in = line(h, wc, PipeRole.WASTE_IN).network();
        h.assertTrue(out != null && out.docks().equals(List.of(new PipeNetwork.Dock(abs(h, 12, 3, 8), Direction.WEST))), "fresh water at the side: " + (out == null ? null : out.docks()));
        h.assertTrue(in != null && in.docks().equals(List.of(new PipeNetwork.Dock(abs(h, 12, 3, 8), Direction.UP))), "waste water on top of the same block: " + (in == null ? null : in.docks()));
        wc.set(7, 25, true);
        int changed = wc.check(level, true);
        h.assertTrue(changed == 2000 && near(c.getTank().nitrate(), 30, 0.01), "changed through one edge block: " + changed + " " + c.getTank().nitrate());
        h.succeed();
    }

    // ------------------------------------------------------------------ top-off and the level sensor

    /**
     * Top-off in a reef tank: evaporation leaves the salt behind; the level sensor reports it (redstone on); the controller tops off
     * through its fresh water line, a salty reservoir is refused, fresh water brings the salinity back; the sensor is quiet again.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void topOffWithASensorKeepsTheSalinity(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Contact k = contact(h, 0, 0);
        TankBlockEntity c = k.tank();
        TankData t = c.getTank();
        t.setSalinity(Salinity.SEA);
        t.setNitrate(20);
        evaporate(level, c, 600);
        h.assertTrue(t.salinity() > Salinity.SEA + 1, "saltier after evaporating: " + t.salinity());
        LevelSensorBlockEntity s = sensor(h, 4, 2, 2);
        s.check(level);
        h.assertTrue(s.status() == LevelSensorBlockEntity.Status.LOW && s.missingMb() == 600, "below the target: " + s.status() + " " + s.missingMb());
        h.assertTrue(level.hasNeighborSignal(abs(h, 5, 2, 2)), "a redstone signal while the water is low");
        WaterChangerBlockEntity wc = k.changer();
        // a salty reservoir is refused
        k.fresh().water().fill(WaterValues.stack(4000, new WaterValues(25, 35, 420, 1, 0)), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(wc.check(level, false) == 0 && wc.status() == WaterChangerBlockEntity.Status.SALTY, "salty water refused: " + wc.status());
        h.assertTrue(t.waterMb() == 7400, "nothing topped off");
        k.fresh().water().drain(4000, IFluidHandler.FluidAction.EXECUTE);
        k.fresh().water().fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
        int added = wc.check(level, false);
        h.assertTrue(added == 600 && t.waterMb() == t.capacityMb() && wc.status() == WaterChangerBlockEntity.Status.TOPPED, "topped off: " + added + " " + wc.status());
        h.assertTrue(near(t.salinity(), Salinity.SEA, 0.01) && near(t.nitrate(), 20, 0.01), "salinity and nitrate back: " + t.salinity() + " " + t.nitrate());
        h.assertTrue(k.fresh().water().amount() == 3400 && wc.sensors() == 1, "from the reservoir, one sensor read: " + k.fresh().water().amount());
        s.check(level);
        h.assertTrue(s.status() == LevelSensorBlockEntity.Status.IDLE && s.served() && !level.hasNeighborSignal(abs(h, 5, 2, 2)), "quiet again, served: " + s.status());
        h.assertTrue(wc.check(level, false) == 0 && wc.status() == WaterChangerBlockEntity.Status.WAITING, "full: " + wc.status());
        h.succeed();
    }

    /**
     * The level sensor only measures: below its target it gives a redstone signal, but it moves no water (a reservoir right next to
     * it stays full) and no pipe joins it. A lower target ends the signal.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void levelSensorOnlyMeasures(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = PlumbingGameTests.tank(h, 2, 2, 2, 2, 2, 2);
        LevelSensorBlockEntity s = sensor(h, 4, 2, 2);
        ReservoirBlockEntity r = reservoir(h, 5, 2, 2, 4000);
        pipes(h, true, new int[]{4, 3, 2});
        evaporate(level, c, 600);
        for (int i = 0; i < 3; i++) s.check(level);
        h.assertTrue(s.signal() && level.getBlockState(s.getBlockPos()).getValue(WaterDeviceBlock.POWERED), "signal: " + s.status());
        h.assertTrue(level.getBlockState(s.getBlockPos()).getSignal(level, s.getBlockPos(), Direction.WEST) == 15, "redstone 15");
        h.assertTrue(c.getTank().waterMb() == 7400 && r.water().amount() == 4000, "no water moved: " + c.getTank().waterMb() + " " + r.water().amount());
        h.assertTrue(!level.getBlockState(abs(h, 4, 3, 2)).getValue(PipeBlock.SIDES.get(Direction.DOWN)), "no pipe joins the sensor");
        h.assertTrue(!s.served(), "no controller reads it");
        s.setTarget(90);
        s.check(level);
        h.assertTrue(!s.signal() && level.getBlockState(abs(h, 5, 2, 2)).getBlock() == ModBlocks.RESERVOIR.get() && !level.getBlockState(s.getBlockPos()).getValue(WaterDeviceBlock.POWERED),
                "at 90 % the water is fine: " + s.status());
        h.succeed();
    }

    /**
     * The old setup does nothing: a reservoir right at the fresh water inlet (no pipe) and a water source next to the controller give
     * no fresh water; nothing is drained.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void noFreshWaterWithoutAPipe(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = PlumbingGameTests.tank(h, 2, 2, 2, 2, 2, 2);
        c.getTank().setNitrate(40);
        WaterChangerBlockEntity wc = changer(h, 2, 2, 4, Direction.SOUTH);
        ReservoirBlockEntity r = reservoir(h, 1, 2, 4, 4000);
        // a water source in a stone cup in front of it
        for (int[] p : new int[][]{{1, 2, 5}, {3, 2, 5}, {2, 2, 6}, {2, 1, 5}}) h.setBlock(new BlockPos(p[0], p[1], p[2]), Blocks.STONE.defaultBlockState());
        h.setBlock(new BlockPos(2, 2, 5), Blocks.WATER.defaultBlockState());
        h.setBlock(new BlockPos(3, 2, 4), ModBlocks.DRAIN.get().defaultBlockState());
        wc.set(7, 25, true);
        h.assertTrue(wc.check(level, true) == 0 && wc.status() == WaterChangerBlockEntity.Status.NO_WATER, "no fresh water: " + wc.status());
        h.assertTrue(near(c.getTank().nitrate(), 40, 1e-4) && c.getTank().waterMb() == 8000 && r.water().amount() == 4000, "nothing drained, nothing taken");
        h.assertTrue(!wc.flag(WaterChangerBlockEntity.FRESH_LINE) && wc.flag(WaterChangerBlockEntity.WASTE_LINE), "no fresh water line, a drain: " + wc.flags());
        h.succeed();
    }

    // ------------------------------------------------------------------ the drain

    /** A pump pushes water from a reservoir into a glass pipe that ends at a drain: the drain takes it all (what glass carries). */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void drainDiscardsWater(GameTestHelper h) {
        ReservoirBlockEntity r = reservoir(h, 2, 2, 2, 4000);
        WaterPumpBlockEntity p = PlumbingGameTests.pump(h, new BlockPos(3, 2, 2), Direction.EAST);
        pipes(h, false, new int[]{4, 2, 2});
        h.setBlock(new BlockPos(5, 2, 2), ModBlocks.DRAIN.get().defaultBlockState());
        h.assertTrue(h.getLevel().getBlockState(abs(h, 4, 2, 2)).getValue(PipeBlock.SIDES.get(Direction.EAST)), "the pipe joins the drain");
        int n = PlumbingGameTests.batch(h, p);
        h.assertTrue(n == 500 && r.water().amount() == 3500, "the drain took a glass pipe's batch: " + n);
        h.succeed();
    }

    /** {@code features.pipes.drain} off: the drain takes nothing (own batch). */
    @GameTest(template = EMPTY, batch = "bettacraft_m68_drain_off", timeoutTicks = 100)
    public static void drainOff(GameTestHelper h) {
        boolean saved = Features.own(Feature.DRAIN);
        try {
            Features.set(Feature.DRAIN, false);
            ReservoirBlockEntity r = reservoir(h, 2, 2, 2, 4000);
            WaterPumpBlockEntity p = PlumbingGameTests.pump(h, new BlockPos(3, 2, 2), Direction.EAST);
            pipes(h, false, new int[]{4, 2, 2});
            h.setBlock(new BlockPos(5, 2, 2), ModBlocks.DRAIN.get().defaultBlockState());
            h.assertTrue(PlumbingGameTests.batch(h, p) == 0 && r.water().amount() == 4000, "switched off: the drain takes nothing");
        } finally {
            Features.set(Feature.DRAIN, saved);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ at a multiblock filter

    private static final int OX = 1, OZ = 1;

    /** Two tanks on one multiblock filter (the rig of M40 part b, moved by one): tank a 8000 mB, tank b 16000 mB, guarded. */
    private static FilterPumpHeadBlockEntity filterRig(GameTestHelper h, TankBlockEntity[] out) {
        ServerLevel level = h.getLevel();
        MultiblockFilter.forget();
        InteriorPlumbing.forget();
        out[0] = PlumbingGameTests.tank(h, OX, 2, OZ, 2, 2, 2);
        out[1] = PlumbingGameTests.tank(h, 4 + OX, 2, OZ, 4, 2, 2);
        FilterPumpHeadBlockEntity head = FilterLinesGameTests.filter(h, new BlockPos(10 + OX, 2, 4 + OZ), Direction.WEST);
        pipes(h, true, new int[]{OX, 2, 2 + OZ}, new int[]{OX, 2, 3 + OZ}, new int[]{4 + OX, 2, 2 + OZ}, new int[]{4 + OX, 2, 3 + OZ});
        pipes(h, true, new int[]{1 + OX, 3, 2 + OZ}, new int[]{1 + OX, 3, 3 + OZ}, new int[]{5 + OX, 3, 2 + OZ}, new int[]{5 + OX, 3, 3 + OZ});
        for (int x = 0; x <= 8; x++) pipes(h, true, new int[]{x + OX, 2, 4 + OZ});
        for (int x = 1; x <= 8; x++) pipes(h, true, new int[]{x + OX, 3, 4 + OZ});
        for (int x = 1; x <= 8; x++) apart(h, new int[]{x + OX, 2, 4 + OZ}, new int[]{x + OX, 3, 4 + OZ});
        PipeUpdates.flush(level);
        head.rescan();
        if (MultiblockFilter.links(level, head.structure(level)).size() != 2) throw new GameTestFailure("the filter does not reach both tanks");
        FilterLinesGameTests.guardAll(level, head.getBlockPos());
        for (TankBlockEntity c : out) c.getTank().setNitrate(40);
        return head;
    }

    /**
     * The controller against a chamber of the filter changes the water of both tanks of the filter (25 % of each: 2000 and 4000 mB),
     * in steps as the fresh water line gives it.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void changerAtAFilterServesItsTanks(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity[] t = new TankBlockEntity[2];
        filterRig(h, t);
        // the chamber at (11,2,5); the controller east of it facing east: its back against the chamber
        WaterChangerBlockEntity wc = changer(h, 12, 2, 5, Direction.EAST);
        pipes(h, true, new int[]{12, 2, 6}); // fresh in: south
        reservoir(h, 12, 2, 7, 16000);
        h.setBlock(new BlockPos(12, 2, 4), ModBlocks.DRAIN.get().defaultBlockState()); // waste out: north, right at the port
        wc.set(7, 25, true);
        int first = wc.check(level, true);
        h.assertTrue(first > 0 && wc.tanks() == 2 && wc.filterTanks() == 2 && wc.flag(WaterChangerBlockEntity.FILTER), "both tanks of the filter: " + wc.tanks() + " " + first);
        h.succeedWhen(() -> {
            h.assertTrue(wc.pendingMb() == 0 && wc.lastMb() == 6000, "6000 mB changed: " + wc.pendingMb() + " " + wc.lastMb());
            for (TankBlockEntity c : t) h.assertTrue(c.getTank().nitrate() > 30 && c.getTank().nitrate() < 31.5, "a quarter in steps: " + c.getTank().nitrate());
        });
    }

    /** Free-standing, its fresh water line ends at the side of a chamber and its waste water line at the pump head: it serves the filter's tanks. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void changerServesAFilterThroughItsLines(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity[] t = new TankBlockEntity[2];
        filterRig(h, t);
        WaterChangerBlockEntity wc = changer(h, 14, 2, 5, Direction.EAST);
        pipes(h, true, new int[]{13, 2, 5}, new int[]{12, 2, 5}); // fresh out: to the east of the chamber (11,2,5)
        pipes(h, true, new int[]{14, 3, 5}, new int[]{14, 4, 5}, new int[]{13, 4, 5}, new int[]{12, 4, 5}); // waste in: to the pump head (11,4,5)
        pipes(h, true, new int[]{14, 2, 6});
        reservoir(h, 14, 2, 7, 16000);
        h.setBlock(new BlockPos(14, 2, 4), ModBlocks.DRAIN.get().defaultBlockState());
        PipeUpdates.flush(level);
        h.assertTrue(level.getBlockState(abs(h, 12, 2, 5)).getValue(PipeBlock.SIDES.get(Direction.WEST)), "the pipe joins the chamber");
        h.assertTrue(level.getBlockState(abs(h, 12, 4, 5)).getValue(PipeBlock.SIDES.get(Direction.WEST)), "the pipe joins the pump head");
        wc.set(7, 25, true);
        wc.check(level, true);
        h.assertTrue(wc.tanks() == 2 && wc.filterTanks() == 2 && !wc.flag(WaterChangerBlockEntity.CONTACT), "both tanks through its lines: " + wc.tanks());
        h.succeedWhen(() -> {
            h.assertTrue(wc.pendingMb() == 0 && wc.lastMb() == 6000, "6000 mB changed: " + wc.pendingMb() + " " + wc.lastMb());
            for (TankBlockEntity c : t) h.assertTrue(c.getTank().nitrate() > 30 && c.getTank().nitrate() < 31.5, "a quarter in steps: " + c.getTank().nitrate());
        });
    }

    // ------------------------------------------------------------------ the intake guard

    private static void young(TankData t, int youngShrimp, int adults, int snails) {
        for (int i = 0; i < youngShrimp; i++) {
            Critter k = new Critter(t.nextCritterId(), CritterSpecies.NEOCARIDINA, i % 2 == 0, CritterGenes.pure(0, 0.8F), 1, 1, 1);
            k.age = 0;
            t.critters().add(k);
        }
        for (int i = 0; i < adults; i++) {
            Critter k = new Critter(t.nextCritterId(), CritterSpecies.NEOCARIDINA, i % 2 == 0, CritterGenes.pure(0, 0.8F), 1, 1, 1);
            k.age = 1000;
            t.critters().add(k);
        }
        for (int i = 0; i < snails; i++) {
            Critter k = new Critter(t.nextCritterId(), CritterSpecies.RAMSHORN, true, CritterGenes.pure(0, 0.8F), 1, 1, 1);
            k.age = 0;
            t.critters().add(k);
        }
    }

    private static long count(TankData t, CritterSpecies s, boolean adult) {
        return t.critters().stream().filter(k -> k.species == s && (s == CritterSpecies.RAMSHORN || k.adult() == adult)).count();
    }

    /**
     * Where the controller drains a tank is a suction point: without an intake guard a 25 % change sucks in a quarter of the young
     * shrimp (10 of 40), never the adults or the snails; with an intake guard behind the dock none. The warning flag says it.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void wasteIntakeSucksInYoungWithoutAGuard(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Contact open = contact(h, 0, 4000), guarded = contact(h, 8, 4000);
        young(open.tank().getTank(), 40, 5, 5);
        young(guarded.tank().getTank(), 40, 5, 5);
        AerationGameTests.region(h, guarded.tank());
        h.assertTrue(TankAnchors.installGuard(level, abs(h, 10, 2, 3), Direction.SOUTH, null, new ItemStack(ModItems.INTAKE_GUARD.get()), Plumb.INTAKE_GUARD).consumesAction(),
                "an intake guard behind the dock of the guarded tank");
        for (Contact k : List.of(open, guarded)) {
            k.changer().set(7, 25, true);
            h.assertTrue(k.changer().check(level, true) == 2000, "25 % changed");
        }
        TankData o = open.tank().getTank(), g = guarded.tank().getTank();
        h.assertTrue(count(o, CritterSpecies.NEOCARIDINA, false) == 30, "a quarter of the young sucked in: " + count(o, CritterSpecies.NEOCARIDINA, false) + " left");
        h.assertTrue(count(o, CritterSpecies.NEOCARIDINA, true) == 5 && count(o, CritterSpecies.RAMSHORN, true) == 5, "no adult, no snail");
        h.assertTrue(AerationGameTests.logged(open.tank(), "log.bettacraft.water_change_intake"), "logged");
        h.assertTrue(open.changer().flag(WaterChangerBlockEntity.YOUNG), "the open dock warns");
        h.assertTrue(count(g, CritterSpecies.NEOCARIDINA, false) == 40 && !guarded.changer().flag(WaterChangerBlockEntity.YOUNG), "the guard keeps them in");
        h.succeed();
    }

    // ------------------------------------------------------------------ short circuit

    /** One pipe network at its fresh water inlet and outlet is a short circuit (red rings, its status); the wrench parts them. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void changerPortsShortCircuit(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        WaterChangerBlockEntity wc = changer(h, 5, 2, 5, Direction.SOUTH); // fresh in west (4,2,5), fresh out north (5,2,4)
        pipes(h, true, new int[]{4, 2, 5}, new int[]{4, 2, 4}, new int[]{5, 2, 4});
        PipeUpdates.flush(level);
        h.assertTrue(ring(h, 4, 2, 4) == PipeRing.SHORT, "red rings: " + ring(h, 4, 2, 4));
        wc.check(level, true);
        h.assertTrue(wc.status() == WaterChangerBlockEntity.Status.SHORT_CIRCUIT && wc.flag(WaterChangerBlockEntity.SHORT), "short circuit: " + wc.status());
        apart(h, new int[]{4, 2, 4}, new int[]{5, 2, 4});
        PipeUpdates.flush(level);
        h.assertTrue(ring(h, 4, 2, 4) == PipeRing.FRESH && ring(h, 5, 2, 4) == PipeRing.FRESH, "apart: two fresh water lines");
        h.assertTrue(line(h, wc, PipeRole.FRESH_IN).network().role() == PipeRole.FRESH_IN && line(h, wc, PipeRole.FRESH_OUT).network().role() == PipeRole.FRESH_OUT, "their roles");
        wc.check(level, true);
        h.assertTrue(wc.status() != WaterChangerBlockEntity.Status.SHORT_CIRCUIT && !wc.flag(WaterChangerBlockEntity.SHORT), "no short circuit any more: " + wc.status());
        h.succeed();
    }

    // ------------------------------------------------------------------ another mod's tank at the fresh water line

    /** A cauldron of water (a fluid handler of vanilla) at the end of the fresh water line feeds a top-off. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void foreignTankAtTheFreshLine(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Contact k = contact(h, 0, 0);
        h.setBlock(new BlockPos(1, 2, 5), Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
        TankBlockEntity c = k.tank();
        evaporate(level, c, 1000);
        LevelSensorBlockEntity s = sensor(h, 4, 2, 2);
        s.check(level);
        int added = k.changer().check(level, false);
        h.assertTrue(added == 1000 && c.getTank().waterMb() == 8000, "the cauldron's water topped off: " + added);
        h.assertTrue(level.getBlockState(abs(h, 1, 2, 5)).is(Blocks.CAULDRON), "the cauldron is empty");
        h.succeed();
    }
}
