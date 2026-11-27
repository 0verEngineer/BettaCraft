package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity;
import org.overengineer.bettacraft.filter.MultiblockFilter;
import org.overengineer.bettacraft.plumbing.ChangeIntake;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.PipeBundleBlock;
import org.overengineer.bettacraft.plumbing.PipeNetwork;
import org.overengineer.bettacraft.plumbing.PipeRing;
import org.overengineer.bettacraft.plumbing.PipeRole;
import org.overengineer.bettacraft.plumbing.PipeSlots;
import org.overengineer.bettacraft.plumbing.PipeUpdates;
import org.overengineer.bettacraft.plumbing.PipeWrenchItem;
import org.overengineer.bettacraft.plumbing.Pipes;
import org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity;
import org.overengineer.bettacraft.plumbing.interior.InteriorBundleBlock;
import org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing;
import org.overengineer.bettacraft.plumbing.interior.PipeAnchorBlock;
import org.overengineer.bettacraft.plumbing.interior.Plumb;
import org.overengineer.bettacraft.plumbing.interior.PlumbingBlock;
import org.overengineer.bettacraft.plumbing.interior.TankAnchors;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;

/**
 * M68 part C GameTests: a dock per line of a pipe bundle inside the tank. A filter's intake and return line dock on one face as a
 * bundle: each line gets its own dock at its flange (its line, its ring colour), its own guards (a guard of the wrong line does not
 * count), a guard used on the glass lands behind the clicked flange, the water runs through each line's own outlets (currents).
 * Four lines: four docks that never touch, one guard each from outside; lines taken out of the bundle take their docks along, the
 * last one keeps its guard as the face's dock. Interior bundles carry two lines on together and part them. The water change
 * controller's waste water line needs its own intake guard. Migration: an old dock of the whole face splits into the lines (guards,
 * strainers and outlets go to their line; pipes keep the old dock for their line; pipes with both kinds stay shared until taken apart).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PipeLinesGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private PipeLinesGameTests() {}

    // ------------------------------------------------------------------ helpers

    private static BlockPos abs(GameTestHelper h, int x, int y, int z) {
        return h.absolutePos(new BlockPos(x, y, z));
    }

    private static long key(int x, int y, int z) {
        return BlockPos.asLong(x, y, z);
    }

    private static BlockState at(GameTestHelper h, InteriorRegion r, long k) {
        return AerationGameTests.interior(h).getBlockState(r.toWorld(BlockPos.getX(k), BlockPos.getY(k), BlockPos.getZ(k)));
    }

    private static boolean touching(long a, long b) {
        return Math.abs(BlockPos.getX(a) - BlockPos.getX(b)) + Math.abs(BlockPos.getY(a) - BlockPos.getY(b)) + Math.abs(BlockPos.getZ(a) - BlockPos.getZ(b)) == 1;
    }

    private static void forget() {
        MultiblockFilter.forget();
        InteriorPlumbing.forget();
        TankAnchors.forget();
    }

    /** The point in the middle of the flange of bundle slot {@code slot} on side {@code face} of tank block {@code member}. */
    static Vec3 flange(BlockPos member, Direction face, int slot) {
        float[] a = PipeBundleBlock.arm(slot, face.getOpposite(), PipeBundleBlock.ARM, 0);
        double[] p = {(a[0] + a[3]) / 32.0, (a[1] + a[4]) / 32.0, (a[2] + a[5]) / 32.0};
        int ax = face.getAxis().ordinal();
        p[ax] = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
        return new Vec3(member.getX() + p[0], member.getY() + p[1], member.getZ() + p[2]);
    }

    private static int count(InteriorRegion r, Plumb kind) {
        int n = 0;
        for (int code : r.equipment().plumbing().values()) if (Plumb.kind(code) == kind) n++;
        return n;
    }

    private static ItemStack guardItem(Plumb kind) {
        return new ItemStack(kind == Plumb.INTAKE_GUARD ? ModItems.INTAKE_GUARD.get() : ModItems.OUTLET_GUARD.get());
    }

    /** A filter (0,2,4) facing east, a tank 2x1x2 at (4,2,4); the bundle at (3,2,4) docks on the tank's west face. */
    record Rig(FilterPumpHeadBlockEntity head, TankBlockEntity tank, BlockPos member, BlockPos bundle, InteriorRegion region) {}

    /** The filter's intake line (y 2) and its return line (y 3) end in one bundle at (3,2,4): slot 0 the intake, slot 1 the return. */
    static Rig rig(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        forget();
        FilterPumpHeadBlockEntity head = FilterLinesGameTests.filter(h, new BlockPos(0, 2, 4), Direction.EAST);
        TankBlockEntity t = PlumbingGameTests.tank(h, 4, 2, 4, 2, 1, 2);
        Block steel = ModBlocks.STAINLESS_STEEL_PIPE.get();
        FilterLinesGameTests.pipes(h, steel, new BlockPos(2, 2, 4), new BlockPos(3, 2, 4), new BlockPos(2, 3, 4), new BlockPos(3, 3, 4));
        FilterLinesGameTests.apart(h, new BlockPos(2, 2, 4), new BlockPos(2, 3, 4));
        BlockPos b = abs(h, 3, 2, 4);
        h.assertTrue(PipeBundleBlock.addPipe(level, b, true, null, null) == 1, "a second pipe in the block");
        PipeWrenchItem.setClosed(level, b, 0, Direction.UP, true);
        PipeUpdates.flush(level);
        h.assertTrue(PipeNetwork.of(level, b, 0).role() == PipeRole.INTAKE && PipeNetwork.of(level, b, 1).role() == PipeRole.RETURN,
                "slot 0 the intake, slot 1 the return: " + PipeNetwork.of(level, b, 0).role() + " / " + PipeNetwork.of(level, b, 1).role());
        BlockPos member = abs(h, 4, 2, 4);
        h.assertTrue(TankAnchors.lines(level, member, Direction.WEST).length == 2, "two lines dock on the west face");
        return new Rig(head, t, member, b, AerationGameTests.region(h, t));
    }

    private static MultiblockFilter.LinkStatus status(Rig k, ServerLevel level) {
        k.head().run(level, true);
        return k.head().tanks().isEmpty() ? null : k.head().tanks().get(0).status();
    }

    // ------------------------------------------------------------------ a dock per line

    /**
     * Two lines through one bundle: two docks at their flanges (line 1 intake blue, line 2 return orange), none in the middle. An
     * outlet guard behind the intake line counts for nothing; with the intake guard the return line still misses its own; a guard
     * used on the glass at the return line's flange goes behind that line. The filter then runs, and only each line's own guards run
     * (two currents; the outlet guard of the intake line stands still).
     */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void bundleLinesHaveTheirOwnDocks(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Rig k = rig(h);
        InteriorRegion r = k.region();
        int s = r.scale();
        h.assertTrue(status(k, level) == MultiblockFilter.LinkStatus.NO_INTAKE_GUARD, "no guards: " + k.head().tanks());
        long d1 = TankAnchors.existing(level, k.tank(), k.member(), Direction.WEST, 0), d2 = TankAnchors.existing(level, k.tank(), k.member(), Direction.WEST, 1);
        h.assertTrue(d1 == TankAnchors.localKey(0, 0, 0, Direction.WEST, s, 1) && d2 == TankAnchors.localKey(0, 0, 0, Direction.WEST, s, 2),
                "the docks sit at their flanges: " + BlockPos.of(d1) + " " + BlockPos.of(d2));
        h.assertTrue(!touching(d1, d2), "and do not touch");
        h.assertTrue(TankAnchors.find(r.equipment().plumbing(), 0, 0, 0, Direction.WEST, s, 0) == Long.MIN_VALUE, "no dock of the whole face");
        BlockState s1 = at(h, r, d1), s2 = at(h, r, d2);
        h.assertTrue(s1.getBlock() instanceof PipeAnchorBlock && s1.getValue(PipeAnchorBlock.LINE) == 1 && s1.getValue(PipeAnchorBlock.RING) == PipeRing.INTAKE
                && s1.getValue(PipeAnchorBlock.MOUNT) == Direction.WEST, "line 1, blue: " + s1);
        h.assertTrue(s2.getBlock() instanceof PipeAnchorBlock && s2.getValue(PipeAnchorBlock.LINE) == 2 && s2.getValue(PipeAnchorBlock.RING) == PipeRing.RETURN, "line 2, orange: " + s2);
        // an outlet guard behind the intake line: no use
        h.assertTrue(TankAnchors.installGuard(level, k.member(), Direction.WEST, 0, null, guardItem(Plumb.OUTLET_GUARD), Plumb.OUTLET_GUARD).consumesAction(), "outlet guard on line 1");
        h.assertTrue(status(k, level) == MultiblockFilter.LinkStatus.NO_INTAKE_GUARD, "still no intake guard: " + k.head().tanks());
        h.assertTrue(TankAnchors.installGuard(level, k.member(), Direction.WEST, 0, null, guardItem(Plumb.INTAKE_GUARD), Plumb.INTAKE_GUARD).consumesAction(), "intake guard on line 1");
        h.assertTrue(status(k, level) == MultiblockFilter.LinkStatus.NO_OUTLET_GUARD, "the return line has none of its own: " + k.head().tanks());
        // from outside, on the return line's flange
        int slot = TankAnchors.slotAt(level, k.member(), Direction.WEST, flange(k.member(), Direction.WEST, 1));
        h.assertTrue(slot == 1, "the clicked flange is slot 1's: " + slot);
        h.assertTrue(TankAnchors.slotAt(level, k.member(), Direction.WEST, flange(k.member(), Direction.WEST, 0)) == 0, "and the other one slot 0's");
        h.assertTrue(TankAnchors.installGuard(level, k.member(), Direction.WEST, slot, null, guardItem(Plumb.OUTLET_GUARD), Plumb.OUTLET_GUARD).consumesAction(), "outlet guard on line 2");
        InteriorPlumbing.Net n1 = InteriorPlumbing.net(r, d1), n2 = InteriorPlumbing.net(r, d2);
        h.assertTrue(n1.intakes().size() == 1 && n1.outflows().size() == 1 && n2.outflows().size() == 1 && n2.intakes().isEmpty(), "each line its guards: " + n1 + " / " + n2);
        h.assertTrue(touching(n2.outflows().getLong(0), d2) && !touching(n2.outflows().getLong(0), d1), "the guard sits at the return line's dock");
        double w0 = TankWater.filterBlocks(level, k.tank());
        int moved = k.head().run(level, true);
        h.assertTrue(moved > 0 && k.head().tanks().get(0).status() == MultiblockFilter.LinkStatus.OK, "it filters: " + k.head().tanks());
        h.assertTrue(PlumbingGameTests.near(TankWater.filterBlocks(level, k.tank()) - w0, 36, 0.01), "36 water blocks");
        long now = level.getGameTime();
        h.assertTrue(InteriorPlumbing.active(r.id(), n1.intakes().getLong(0), now) && InteriorPlumbing.active(r.id(), n2.outflows().getLong(0), now), "its guards run");
        h.assertTrue(!InteriorPlumbing.active(r.id(), n1.outflows().getLong(0), now), "the outlet guard of the intake line stands still");
        h.assertTrue(InteriorPlumbing.currents(level, k.tank()).size() == 2, "two currents: " + InteriorPlumbing.currents(level, k.tank()).size());
        h.succeed();
    }

    /**
     * A bundle of four pipes docks on one face: four docks at their flanges that never touch; a guard used on each flange lands behind
     * that line. Lines taken out of the bundle take their docks along; when one pipe is left, its dock is the face's dock and keeps its
     * guard.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void fourLinesFourDocks(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        forget();
        TankBlockEntity t = PlumbingGameTests.tank(h, 4, 2, 4, 2, 1, 2);
        InteriorRegion r = AerationGameTests.region(h, t);
        h.setBlock(new BlockPos(3, 2, 4), ModBlocks.STAINLESS_STEEL_PIPE.get().defaultBlockState());
        BlockPos b = abs(h, 3, 2, 4), member = abs(h, 4, 2, 4);
        for (int i = 1; i <= 3; i++) h.assertTrue(PipeBundleBlock.addPipe(level, b, i % 2 == 0, null, null) == i, "pipe " + i + " in the bundle");
        PipeUpdates.flush(level);
        int[] slots = Pipes.slotsAt(level, b);
        for (int i = 0; i < 4; i++) h.assertTrue(PipeSlots.joined(slots[i], Direction.EAST), "slot " + i + " docks on the glass");
        int s = r.scale();
        List<Long> docks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            long d = TankAnchors.ensure(level, t, member, Direction.WEST, i);
            h.assertTrue(d == TankAnchors.localKey(0, 0, 0, Direction.WEST, s, i + 1), "line " + (i + 1) + " at its flange: " + BlockPos.of(d));
            h.assertTrue(at(h, r, d).getValue(PipeAnchorBlock.LINE) == i + 1, "its line: " + at(h, r, d));
            docks.add(d);
        }
        for (int i = 0; i < 4; i++) for (int j = i + 1; j < 4; j++) h.assertTrue(!touching(docks.get(i), docks.get(j)), "docks " + i + " and " + j + " apart");
        // a guard on each flange from outside
        for (int i = 0; i < 4; i++) {
            int slot = TankAnchors.slotAt(level, member, Direction.WEST, flange(member, Direction.WEST, i));
            h.assertTrue(slot == i, "flange " + i + " is slot " + slot);
            h.assertTrue(TankAnchors.installGuard(level, member, Direction.WEST, slot, null, guardItem(Plumb.INTAKE_GUARD), Plumb.INTAKE_GUARD).consumesAction(), "guard " + i);
        }
        for (int i = 0; i < 4; i++) {
            InteriorPlumbing.Net n = InteriorPlumbing.net(r, docks.get(i));
            h.assertTrue(n.intakes().size() == 1 && n.ports().isEmpty(), "line " + (i + 1) + " has its one guard: " + n);
            for (int j = 0; j < 4; j++) if (j != i) h.assertTrue(!touching(n.intakes().getLong(0), docks.get(j)), "guard " + i + " away from dock " + j);
        }
        // lines out of the bundle: their docks go; the last pipe keeps its dock (now the face's) and its guard
        PipeBundleBlock.removePipe(level, b, 3);
        PipeUpdates.flush(level);
        TankAnchors.sweep(level, t);
        h.assertTrue(!(at(h, r, docks.get(3)).getBlock() instanceof PipeAnchorBlock), "line 4's dock is gone");
        h.assertTrue(at(h, r, docks.get(0)).getBlock() instanceof PipeAnchorBlock && at(h, r, docks.get(2)).getBlock() instanceof PipeAnchorBlock, "the others stay");
        PipeBundleBlock.removePipe(level, b, 2);
        PipeBundleBlock.removePipe(level, b, 1);
        PipeUpdates.flush(level);
        h.assertTrue(level.getBlockState(b).getBlock() instanceof PipeBlock, "a single pipe again");
        TankAnchors.sweep(level, t);
        BlockState last = at(h, r, docks.get(0));
        h.assertTrue(last.getBlock() instanceof PipeAnchorBlock && last.getValue(PipeAnchorBlock.LINE) == 0, "line 1's dock is the face's dock now: " + last);
        h.assertTrue(TankAnchors.existing(level, t, member, Direction.WEST) == docks.get(0), "and serves the single pipe");
        h.assertTrue(TankAnchors.guardOf(level, t, member, Direction.WEST, true) == TankAnchors.Guard.OK, "with its guard");
        h.assertTrue(count(r, Plumb.ANCHOR) == 1, "one dock left: " + count(r, Plumb.ANCHOR));
        h.succeed();
    }

    // ------------------------------------------------------------------ interior bundles

    /**
     * The two lines of the bundle run on together in a run of four interior bundles (glass, along x): the intake line comes into the
     * middle lane at the run's west end and leaves at its east end into an intake guard; the return line joins side lane 3 (south) at
     * the second block and parts at the last into an outlet guard. The lines stay apart (no pipe through the tank), the filter runs.
     * An interior bundle takes pipes of one kind only; the last lane comes out again.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void interiorBundleCarriesTwoLinesAndPartsThem(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Rig k = rig(h);
        InteriorRegion r = k.region();
        long d1 = TankAnchors.ensure(level, k.tank(), k.member(), Direction.WEST, 0), d2 = TankAnchors.ensure(level, k.tank(), k.member(), Direction.WEST, 1);
        int y = BlockPos.getY(d1), z1 = BlockPos.getZ(d1), z2 = BlockPos.getZ(d2);
        h.assertTrue(BlockPos.getY(d2) == y && z2 - z1 == 3, "the docks side by side: " + BlockPos.of(d1) + " " + BlockPos.of(d2));
        BlockState glass = ModBlocks.INTERIOR_PIPE.get().defaultBlockState();
        int rz = z1 + 1; // the run between them, one block further
        // the run: four interior pipes along x, each made a bundle of four lanes
        for (int x = 3; x <= 6; x++) AerationGameTests.put(h, r, x, y, rz, glass);
        for (int x = 3; x <= 6; x++)
            for (int lanes = 2; lanes <= 4; lanes++)
                h.assertTrue(InteriorBundleBlock.addPipe(AerationGameTests.interior(h), r.toWorld(x, y, rz), false, null, null, Direction.WEST) == lanes, "lane " + lanes + " at x " + x);
        BlockState run = at(h, r, key(4, y, rz));
        h.assertTrue(run.getBlock() == ModBlocks.INTERIOR_PIPE_BUNDLE.get() && run.getValue(InteriorBundleBlock.AXIS) == Direction.Axis.X && run.getValue(InteriorBundleBlock.LANES) == 4, "a bundle along x: " + run);
        h.assertTrue(InteriorBundleBlock.addPipe(AerationGameTests.interior(h), r.toWorld(4, y, rz), true, null, null, Direction.WEST) == -1, "no steel pipe in a glass bundle");
        Direction south = InteriorBundleBlock.laneSide(Direction.Axis.X, 3);
        h.assertTrue(south == Direction.SOUTH, "lane 3 leaves south");
        // line 1 (intake) from its dock into the west end, out of the east end into an intake guard
        AerationGameTests.put(h, r, 1, y, z1, glass);
        AerationGameTests.put(h, r, 2, y, z1, glass);
        AerationGameTests.put(h, r, 2, y, rz, glass);
        AerationGameTests.put(h, r, 7, y, rz, glass);
        AerationGameTests.put(h, r, 8, y, rz, ModBlocks.INTAKE_GUARD.get().defaultBlockState().setValue(PlumbingBlock.FACING, Direction.EAST));
        // line 2 (return) from its dock along z2 to the side of the second block, out of the side of the last into an outlet guard
        for (int x = 1; x <= 4; x++) AerationGameTests.put(h, r, x, y, z2, glass);
        AerationGameTests.put(h, r, 4, y, z2 - 1, glass);
        AerationGameTests.put(h, r, 6, y, rz + 1, glass);
        AerationGameTests.put(h, r, 6, y, rz + 2, ModBlocks.OUTLET_GUARD.get().defaultBlockState().setValue(PlumbingBlock.FACING, Direction.SOUTH));
        InteriorPlumbing.Net n1 = InteriorPlumbing.net(r, d1), n2 = InteriorPlumbing.net(r, d2);
        h.assertTrue(n1.intakes().equals(List.of(key(8, y, rz))) && n1.outflows().isEmpty() && n1.ports().isEmpty(), "line 1 through the middle lane: " + n1);
        h.assertTrue(n2.outflows().equals(List.of(key(6, y, rz + 2))) && n2.intakes().isEmpty() && n2.ports().isEmpty(), "line 2 through lane 3: " + n2);
        BlockState second = at(h, r, key(4, y, rz)), first = at(h, r, key(3, y, rz)), third = at(h, r, key(5, y, rz));
        h.assertTrue(second.getValue(PipeBlock.SIDES.get(Direction.SOUTH)) && !third.getValue(PipeBlock.SIDES.get(Direction.SOUTH)), "the arm of lane 3 where line 2 joins");
        h.assertTrue(first.getValue(PipeBlock.SIDES.get(Direction.WEST)) && first.getValue(PipeBlock.SIDES.get(Direction.EAST)), "the run's ends join on");
        h.assertTrue(status(k, level) == MultiblockFilter.LinkStatus.OK, "the filter runs through them: " + k.head().tanks());
        // the last lane out again: a three-lane bundle, then (from a two-lane one) a plain interior pipe
        AerationGameTests.put(h, r, 10, y + 3, 10, glass);
        InteriorBundleBlock.addPipe(AerationGameTests.interior(h), r.toWorld(10, y + 3, 10), false, null, null, Direction.UP);
        ItemStack back = InteriorBundleBlock.removeLane(AerationGameTests.interior(h), r.toWorld(10, y + 3, 10));
        h.assertTrue(back.is(ModItems.GLASS_PIPE.get()) && at(h, r, key(10, y + 3, 10)).is(ModBlocks.INTERIOR_PIPE.get()), "a glass pipe back, a plain interior pipe left");
        h.succeed();
    }

    // ------------------------------------------------------------------ the water change controller

    /**
     * The controller's fresh water line (slot 0) and waste water line (slot 1) dock as a bundle: the waste water dock needs its own
     * intake guard; one behind the fresh water line does not guard it. Put on without a clicked flange, an intake guard goes behind the
     * line that sucks.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void changerWasteLineNeedsItsOwnGuard(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        forget();
        TankBlockEntity c = PlumbingGameTests.tank(h, 12, 2, 7, 2, 2, 2);
        WaterChangerBlockEntity wc = WaterChangeGameTests.westChanger(h);
        WaterChangeGameTests.pipes(h, true, new int[]{9, 2, 8}, new int[]{10, 2, 8}, new int[]{11, 2, 8});
        WaterChangeGameTests.pipes(h, true, new int[]{8, 3, 8}, new int[]{9, 3, 8}, new int[]{10, 3, 8}, new int[]{11, 3, 8});
        WaterChangeGameTests.apart(h, new int[]{9, 2, 8}, new int[]{9, 3, 8});
        WaterChangeGameTests.apart(h, new int[]{10, 2, 8}, new int[]{10, 3, 8});
        PipeWrenchItem.setClosed(level, abs(h, 11, 3, 8), -1, Direction.EAST, true);
        h.assertTrue(PipeBundleBlock.addPipe(level, abs(h, 11, 2, 8), true, null, null) == 1, "a second pipe in the block");
        PipeWrenchItem.setClosed(level, abs(h, 11, 2, 8), 0, Direction.UP, true);
        PipeUpdates.flush(level);
        InteriorRegion r = AerationGameTests.region(h, c);
        PipeNetwork in = WaterChangeGameTests.line(h, wc, PipeRole.WASTE_IN).network();
        h.assertTrue(in != null && in.docks().size() == 1 && in.docks().get(0).slot() == 1, "the waste water line docks with slot 1: " + (in == null ? null : in.docks()));
        PipeNetwork.Dock waste = in.docks().get(0);
        BlockPos member = abs(h, 12, 2, 8);
        h.assertTrue(!ChangeIntake.guarded(level, c, waste), "no guard yet");
        h.assertTrue(TankAnchors.installGuard(level, member, Direction.WEST, 0, null, guardItem(Plumb.INTAKE_GUARD), Plumb.INTAKE_GUARD).consumesAction(), "an intake guard on the fresh water line");
        h.assertTrue(!ChangeIntake.guarded(level, c, waste), "it does not guard the waste water line");
        h.assertTrue(ChangeIntake.openShare(level, c, in.docks()) == 1, "its share stays open");
        h.assertTrue(TankAnchors.installGuard(level, member, Direction.WEST, null, guardItem(Plumb.INTAKE_GUARD), Plumb.INTAKE_GUARD).consumesAction(), "an intake guard put on the glass");
        h.assertTrue(ChangeIntake.guarded(level, c, waste) && ChangeIntake.openShare(level, c, in.docks()) == 0, "it went behind the waste water line");
        long dw = TankAnchors.existing(level, c, member, Direction.WEST, 1);
        h.assertTrue(at(h, r, dw).getValue(PipeAnchorBlock.RING) == PipeRing.WASTE, "the waste water dock is brown: " + at(h, r, dw));
        h.assertTrue(at(h, r, TankAnchors.existing(level, c, member, Direction.WEST, 0)).getValue(PipeAnchorBlock.RING) == PipeRing.FRESH, "the fresh water dock green");
        h.succeed();
    }

    // ------------------------------------------------------------------ migration from before part C

    /** The state of an old dock (before part C: one per face, with the facing and activity every interior piece had). */
    private static BlockState oldDock(ServerLevel level, Direction mount) {
        Map<String, String> p = new HashMap<>();
        for (Direction d : Direction.values()) p.put(d.getSerializedName(), "false");
        p.put("mount", mount.getSerializedName());
        p.put("facing", "north");
        p.put("active", "false");
        return PipeMigrationGameTests.oldState(level, "pipe_anchor", p);
    }

    /**
     * An old world: the M40 filter with its bundle (intake and return in one block, old block entity data) docks on the west face of
     * a tank; inside, the one old dock in the middle of that face with the intake guard in front of it and the outlet guard beside it
     * (where guards from outside went). Loading splits it: a dock per line at its flange, the intake guard behind the intake line, the
     * outlet guard behind the return line, the old dock gone, nothing lost; the filter filters without anything done.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void oldSharedDockGuardsGoToTheirLines(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Old o = oldWorld(h);
        InteriorRegion r = o.region();
        int y = 8, z = 8;
        AerationGameTests.put(h, r, 0, y, z, oldDock(level, Direction.WEST));
        AerationGameTests.put(h, r, 1, y, z, ModBlocks.INTAKE_GUARD.get().defaultBlockState().setValue(PlumbingBlock.FACING, Direction.EAST));
        AerationGameTests.put(h, r, 0, y - 1, z, ModBlocks.OUTLET_GUARD.get().defaultBlockState().setValue(PlumbingBlock.FACING, Direction.EAST));
        var m = r.equipment().plumbing();
        long old = key(0, y, z);
        // red: as saved, one dock of the whole face with both guards behind it, no line docks
        h.assertTrue(old == TankAnchors.localKey(0, 0, 0, Direction.WEST, r.scale()) && TankAnchors.find(m, 0, 0, 0, Direction.WEST, r.scale(), 0) == old, "the old dock");
        h.assertTrue(at(h, r, old).getValue(PipeAnchorBlock.LINE) == 0, "an old state is line 0: " + at(h, r, old));
        h.assertTrue(TankAnchors.find(m, 0, 0, 0, Direction.WEST, r.scale(), 1) == Long.MIN_VALUE, "no line docks yet");
        // green: the world loads (the pipes migrate and are joined, the filter lines ask for their docks)
        PipeUpdates.flush(level);
        long d1 = TankAnchors.find(m, 0, 0, 0, Direction.WEST, r.scale(), 1), d2 = TankAnchors.find(m, 0, 0, 0, Direction.WEST, r.scale(), 2);
        h.assertTrue(d1 == TankAnchors.localKey(0, 0, 0, Direction.WEST, r.scale(), 1) && d2 == TankAnchors.localKey(0, 0, 0, Direction.WEST, r.scale(), 2), "a dock per line: " + d1 + " " + d2);
        h.assertTrue(TankAnchors.find(m, 0, 0, 0, Direction.WEST, r.scale(), 0) == Long.MIN_VALUE && !(at(h, r, old).getBlock() instanceof PipeAnchorBlock), "the old dock is gone");
        InteriorPlumbing.Net n1 = InteriorPlumbing.net(r, d1), n2 = InteriorPlumbing.net(r, d2);
        h.assertTrue(n1.intakes().size() == 1 && n1.outflows().isEmpty(), "the intake guard behind the intake line: " + n1);
        h.assertTrue(n2.outflows().size() == 1 && n2.intakes().isEmpty(), "the outlet guard behind the return line: " + n2);
        h.assertTrue(count(r, Plumb.INTAKE_GUARD) == 1 && count(r, Plumb.OUTLET_GUARD) == 1 && count(r, Plumb.ANCHOR) == 2, "nothing lost, nothing doubled");
        double w0 = TankWater.filterBlocks(level, o.tank());
        int moved = o.head().run(level, true);
        h.assertTrue(moved > 0 && o.head().tanks().get(0).status() == MultiblockFilter.LinkStatus.OK, "the old filter filters on: " + o.head().tanks());
        h.assertTrue(PlumbingGameTests.near(TankWater.filterBlocks(level, o.tank()) - w0, 36, 0.01), "36 water blocks");
        h.succeed();
    }

    /**
     * Old dock with interior pipes down the wall to a nozzle (the return's outlet) and the intake guard in front of it: the pipes keep
     * the old dock, which becomes the return line's (where it is); the intake guard goes to the intake line's new dock; the nozzle runs.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void oldSharedDockWithPipesKeepsThemForTheirLine(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Old o = oldWorld(h);
        InteriorRegion r = o.region();
        int y = 8, z = 8;
        BlockState glass = ModBlocks.INTERIOR_PIPE.get().defaultBlockState();
        AerationGameTests.put(h, r, 0, y, z, oldDock(level, Direction.WEST));
        AerationGameTests.put(h, r, 1, y, z, ModBlocks.INTAKE_GUARD.get().defaultBlockState().setValue(PlumbingBlock.FACING, Direction.EAST));
        AerationGameTests.put(h, r, 0, y - 1, z, glass);
        AerationGameTests.put(h, r, 0, y - 2, z, glass);
        AerationGameTests.put(h, r, 0, y - 3, z, ModBlocks.NOZZLE.get().defaultBlockState().setValue(PlumbingBlock.FACING, Direction.EAST));
        PipeUpdates.flush(level);
        var m = r.equipment().plumbing();
        long old = key(0, y, z);
        long d1 = TankAnchors.find(m, 0, 0, 0, Direction.WEST, r.scale(), 1), d2 = TankAnchors.find(m, 0, 0, 0, Direction.WEST, r.scale(), 2);
        h.assertTrue(d2 == old && at(h, r, old).getValue(PipeAnchorBlock.LINE) == 2 && at(h, r, old).getValue(PipeAnchorBlock.RING) == PipeRing.RETURN,
                "the old dock is the return line's now: " + at(h, r, old));
        h.assertTrue(d1 == TankAnchors.localKey(0, 0, 0, Direction.WEST, r.scale(), 1), "the intake line has a new dock at its flange");
        InteriorPlumbing.Net n1 = InteriorPlumbing.net(r, d1), n2 = InteriorPlumbing.net(r, d2);
        h.assertTrue(n1.intakes().size() == 1 && n1.outflows().isEmpty(), "the intake guard moved to it: " + n1);
        h.assertTrue(n2.outflows().equals(List.of(key(0, y - 3, z))) && n2.intakes().isEmpty(), "the nozzle stays on the return line: " + n2);
        h.assertTrue(count(r, Plumb.INTAKE_GUARD) == 1 && count(r, Plumb.NOZZLE) == 1 && count(r, Plumb.PIPE) == 2, "nothing lost");
        int moved = o.head().run(level, true);
        h.assertTrue(moved > 0 && o.head().tanks().get(0).status() == MultiblockFilter.LinkStatus.OK, "it filters: " + o.head().tanks());
        h.assertTrue(InteriorPlumbing.active(r.id(), key(0, y - 3, z), level.getGameTime()), "the nozzle runs");
        h.succeed();
    }

    /**
     * Old dock whose interior pipe leads to both a nozzle and an intake strainer: that cannot be told apart, the old dock stays shared by
     * both lines and works as before (the filter runs). Once the strainer is taken out, it splits.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void oldSharedDockWithMixedPipesStaysShared(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Old o = oldWorld(h);
        InteriorRegion r = o.region();
        int y = 8, z = 8;
        BlockState glass = ModBlocks.INTERIOR_PIPE.get().defaultBlockState();
        AerationGameTests.put(h, r, 0, y, z, oldDock(level, Direction.WEST));
        AerationGameTests.put(h, r, 1, y, z, glass);
        AerationGameTests.put(h, r, 2, y, z, glass);
        AerationGameTests.put(h, r, 3, y, z, ModBlocks.NOZZLE.get().defaultBlockState().setValue(PlumbingBlock.FACING, Direction.EAST));
        AerationGameTests.put(h, r, 2, y - 1, z, ModBlocks.INTAKE_STRAINER.get().defaultBlockState());
        PipeUpdates.flush(level);
        var m = r.equipment().plumbing();
        long old = key(0, y, z);
        h.assertTrue(TankAnchors.find(m, 0, 0, 0, Direction.WEST, r.scale(), 1) == Long.MIN_VALUE && TankAnchors.find(m, 0, 0, 0, Direction.WEST, r.scale(), 2) == Long.MIN_VALUE,
                "no line docks");
        TankAnchors.Anchor a = TankAnchors.anchor(level, o.tank(), o.member(), Direction.WEST, 0, false);
        h.assertTrue(a != null && a.shared() && a.key() == old, "the old dock serves both lines: " + a);
        int moved = o.head().run(level, true);
        h.assertTrue(moved > 0 && o.head().tanks().get(0).status() == MultiblockFilter.LinkStatus.OK, "it filters as before: " + o.head().tanks());
        // the player takes the strainer out: now the pipes lead to the nozzle only, the dock splits
        AerationGameTests.put(h, r, 2, y - 1, z, Blocks.AIR.defaultBlockState());
        a = TankAnchors.anchor(level, o.tank(), o.member(), Direction.WEST, 1, false);
        h.assertTrue(a != null && !a.shared() && a.key() == old && a.line() == 2, "split: the old dock is the return line's: " + a);
        h.assertTrue(TankAnchors.find(m, 0, 0, 0, Direction.WEST, r.scale(), 1) == TankAnchors.localKey(0, 0, 0, Direction.WEST, r.scale(), 1), "the intake line got its dock");
        h.succeed();
    }

    record Old(FilterPumpHeadBlockEntity head, TankBlockEntity tank, BlockPos member, InteriorRegion region) {}

    /**
     * The M40 world of {@code PipeMigrationGameTests#pipeMigrationOldBundleAndTee}: the filter (0,2,4) facing east, its old intake pipe
     * at (2,2,4), its old return pipes (2,3,4) and (3,3,4), the old bundle at (3,2,4) (intake west-east, return up-east) docking on the
     * west face of the tank (4,2,4) 2x1x2. Not yet joined (that happens when the world ticks: {@link PipeUpdates#flush}).
     */
    static Old oldWorld(GameTestHelper h) {
        forget();
        FilterPumpHeadBlockEntity head = FilterLinesGameTests.filter(h, new BlockPos(0, 2, 4), Direction.EAST);
        TankBlockEntity t = PlumbingGameTests.tank(h, 4, 2, 4, 2, 1, 2);
        PipeMigrationGameTests.oldPipe(h, 2, 2, 4, "filter_inlet_pipe", false, Direction.WEST, Direction.EAST);
        PipeMigrationGameTests.oldPipe(h, 2, 3, 4, "filter_outlet_pipe", false, Direction.WEST, Direction.EAST);
        PipeMigrationGameTests.oldPipe(h, 3, 3, 4, "filter_outlet_pipe", false, Direction.WEST, Direction.DOWN);
        int intake = 0x80 | PipeSlots.bit(Direction.WEST) | PipeSlots.bit(Direction.EAST), ret = 0x80 | PipeSlots.bit(Direction.UP) | PipeSlots.bit(Direction.EAST);
        CompoundTag bt = new CompoundTag();
        bt.putInt("lines", intake | ret << 8);
        PipeMigrationGameTests.old(h, new BlockPos(3, 2, 4), ModBlocks.FILTER_PIPE_BUNDLE.get().defaultBlockState(), "filter_pipe", bt);
        return new Old(head, t, abs(h, 4, 2, 4), AerationGameTests.region(h, t));
    }
}
