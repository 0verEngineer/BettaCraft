package org.overengineer.bettacraft.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.filter.FilterChamberBlockEntity;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity;
import org.overengineer.bettacraft.filter.MultiblockFilter;
import org.overengineer.bettacraft.gametest.FilterLinesGameTests;
import org.overengineer.bettacraft.osmosis.RoUnitBlockEntity;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.plumbing.LevelSensorBlockEntity;
import org.overengineer.bettacraft.plumbing.PipeBundleBlock;
import org.overengineer.bettacraft.plumbing.PipeNetwork;
import org.overengineer.bettacraft.plumbing.PipeRole;
import org.overengineer.bettacraft.plumbing.PipeUpdates;
import org.overengineer.bettacraft.plumbing.PipeWrenchItem;
import org.overengineer.bettacraft.plumbing.ReservoirBlockEntity;
import org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity;
import org.overengineer.bettacraft.plumbing.WaterDeviceBlock;
import org.overengineer.bettacraft.plumbing.interior.InteriorBundleBlock;
import org.overengineer.bettacraft.plumbing.interior.Plumb;
import org.overengineer.bettacraft.plumbing.interior.PlumbingBlock;
import org.overengineer.bettacraft.plumbing.interior.TankAnchors;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M68 part E: the two showcase rows of the one pipe system (built by {@link DevShowcase} and by the M68 client smoke, which
 * takes its pictures of exactly these stations).
 * <ul>
 *     <li>{@link #pipeRow}: pipes join in every direction (no T-piece), the pipe wrench keeps two neighbouring lines apart (caps),
 *     the rings of the roles, a short circuit, glass carries half of what the filter pumps, two intake lines of one role in one
 *     bundle, a bundle at the glass with a dock per line inside and an interior bundle that carries both lines away.</li>
 *     <li>{@link #waterRow}: top-off and water change: the controller against a single tank (two pipes, a drain, the intake guard
 *     where it drains, a level sensor with a lamp), a level sensor alone (it only measures: redstone), the controller at a
 *     multiblock filter with three tanks, and a central technical room (the controller standing free, all four lines by pipe,
 *     fresh water from a reservoir and a reverse osmosis unit, the old water into a drain).</li>
 * </ul>
 * Every station is checked after building (the filters and controllers run once); what is wrong goes to {@link #PROBLEMS} and the
 * log ({@code [showcase] M68 ...}). Coordinates: {@code at} is the row start at tank height (the ground one below, the path north).
 */
public final class DevShowcaseM68 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/showcase");
    /** What the last build found wrong (the showcase log and the M68 client smoke read it). */
    public static final List<String> PROBLEMS = new ArrayList<>();
    private static final int GAP = 5;

    private DevShowcaseM68() {}

    // ------------------------------------------------------------------ row 1: the pipes

    /** Builds the pipe row; returns the positions the M68 client smoke looks at (and {@code "end"}: the x after the row). */
    public static Map<String, BlockPos> pipeRow(ServerLevel level, BlockPos at) {
        Map<String, BlockPos> p = new LinkedHashMap<>();
        RandomSource r = RandomSource.create(68);
        int x = 0;
        // 1: pipes join in every direction, steel and glass (no T-piece any more)
        floor(level, at, x, x + 6, 0, 2);
        junction(level, at.offset(x + 1, 1, 1), true);
        junction(level, at.offset(x + 5, 1, 1), false);
        p.put("junction", at.offset(x + 1, 1, 1));
        DevShowcase.sign(level, at.offset(x + 1, -1, -2), "Rohre verbinden", "sich in jede", "Richtung, ohne", "T-Stück");
        DevShowcase.sign(level, at.offset(x + 5, -1, -2), "Glasrohr:", "50 mB/t", "Edelstahlrohr:", "250 mB/t");
        x += 7 + GAP;
        // 2: the wrench: the intake line (blue) and the return line (orange) run on top of each other, caps keep them apart
        p.put("wrench_tank", at.offset(x, 0, 0));
        p.put("wrench_head", twoLines(level, at.offset(x, 0, 0), true, true, r, "Rohrzange: zwei Leitungen"));
        p.put("wrench_pipe", at.offset(x + 2, 0, 0));
        DevShowcase.sign(level, at.offset(x, -1, -2), "Rohrzange:", "Seite zu, die", "Nachbarrohre", "sind getrennt");
        DevShowcase.sign(level, at.offset(x + 3, -1, -2), "Ringe: blau =", "Zulauf, orange", "= Rücklauf, vom", "Anschluss");
        expectFilter(level, "wrench", p.get("wrench_head"), 1, false, 100);
        x += 6 + GAP;
        // 3: the same without the wrench: one network at the inlet and the outlet, a short circuit (red), the filter stops
        p.put("short_tank", at.offset(x, 0, 0));
        p.put("short_head", twoLines(level, at.offset(x, 0, 0), true, false, r, "Kurzschluss"));
        p.put("short_pipe", at.offset(x + 2, 0, 0));
        DevShowcase.sign(level, at.offset(x, -1, -2), "Kurzschluss:", "ein Netz an", "Ein- + Auslass", "= rote Ringe");
        DevShowcase.sign(level, at.offset(x + 3, -1, -2), "Der Filter", "filtert nicht,", "Warnung im", "Filtermenü");
        expectFilter(level, "short circuit", p.get("short_head"), 0, true, -1);
        x += 6 + GAP;
        // 4: glass lines: 50 mB/t each, the filter (100) runs at half
        p.put("glass_tank", at.offset(x, 0, 0));
        p.put("glass_head", twoLines(level, at.offset(x, 0, 0), false, true, r, "Glasrohre: halber Filter"));
        p.put("glass_pipe", at.offset(x + 2, 0, 0));
        DevShowcase.sign(level, at.offset(x, -1, -2), "Glasrohre:", "50 mB/t je", "Leitung: Filter", "läuft halb");
        DevShowcase.sign(level, at.offset(x + 3, -1, -2), "Edelstahl:", "250 mB/t, der", "Filter läuft", "voll (100 mB/t)");
        expectFilter(level, "glass", p.get("glass_head"), 1, false, 50);
        x += 6 + GAP;
        // 5: two intake lines of glass in one bundle (one role twice): 2 x 50 = the filter's 100 mB/t; a dock per line inside
        sameRoleBundle(level, at.offset(x, 0, 0), p, r);
        DevShowcase.sign(level, at.offset(x + 1, -1, -2), "Bündel: 2-4", "Rohre in einem", "Block, auch mit", "gleicher Rolle");
        DevShowcase.sign(level, at.offset(x + 4, -1, -2), "2 Glas-Zuläufe", "= 100 mB/t:", "Leitungen", "addieren sich");
        x += 7 + GAP;
        // 6: a bundle at the glass: a dock per line inside, each with its guard, the lines run on in an interior bundle
        interiorBundle(level, at.offset(x, 0, 0), p, r);
        DevShowcase.sign(level, at.offset(x + 1, -1, -2), "Bündel am Glas:", "je Leitung ein", "Anschluss innen", "mit Farbring");
        DevShowcase.sign(level, at.offset(x + 4, -1, -2), "Innen-Bündel:", "die Leitungen", "laufen gebündelt", "und teilen sich");
        x += 8 + GAP;
        p.put("end", at.offset(x, 0, 0));
        PipeUpdates.flush(level);
        return p;
    }

    /** A pipe with an arm in each of the six directions at {@code c}. */
    private static void junction(ServerLevel level, BlockPos c, boolean steel) {
        pipe(level, c, steel);
        for (Direction d : Direction.values()) pipe(level, c.relative(d), steel);
    }

    /**
     * A 2 x 2 x 2 tank at {@code t} (x 0..1, z 0..1), a multiblock filter east of it (chambers x 5, ports x 4 facing west), the intake
     * line at the floor (x 2..3, y 0) and the return line above it (y 1); {@code apart}: the wrench closes the sides between them.
     * Returns the pump head.
     */
    private static BlockPos twoLines(ServerLevel level, BlockPos t, boolean steel, boolean apart, RandomSource r, String name) {
        TankBlockEntity c = DevShowcase.tank(level, t, 2, 2, 2, 0F, true);
        DevShowcase.region(level, c, Blocks.SAND.defaultBlockState());
        BlockPos head = filter(level, t.offset(5, 0, 0), Direction.WEST, 2);
        for (int x = 2; x <= 3; x++) for (int y = 0; y <= 1; y++) pipe(level, t.offset(x, y, 0), steel);
        if (apart) for (int x = 2; x <= 3; x++) PipeWrenchItem.setClosed(level, t.offset(x, 0, 0), -1, Direction.UP, true);
        floor(level, t, 2, 5, 0, 0);
        PipeUpdates.flush(level);
        if (apart) FilterLinesGameTests.guardAll(level, head);
        else guardDirect(level, t.offset(1, 0, 0), t.offset(1, 1, 0));
        DevShowcase.fish(level, c, TankmateSpecies.GUPPY, 3, r);
        DevShowcase.name(level, t, name);
        return head;
    }

    /** The guards of the short circuit station (the filter does not list its docks then): intake below, outlet guard above. */
    private static void guardDirect(ServerLevel level, BlockPos low, BlockPos high) {
        TankAnchors.installGuard(level, low, Direction.EAST, null, new ItemStack(ModItems.INTAKE_GUARD.get()), Plumb.INTAKE_GUARD);
        TankAnchors.installGuard(level, high, Direction.EAST, null, new ItemStack(ModItems.OUTLET_GUARD.get()), Plumb.OUTLET_GUARD);
    }

    /**
     * Two intake lines of glass in one bundle. Tank 3 x 2 x 2 at x 0..2; filter chambers at x 6 (z 1), inlet A west of it (x 5, z 1),
     * inlet B north of it (x 6, z 0), the outlet above inlet A. Intake A runs west at the floor (x 4) into the bundle at x 3, intake B
     * round the north (z -1) and in from the north; the return line of steel above (y 1) docks beside them. Both intake lines dock at
     * the same face of the same tank block, each with its own dock inside.
     */
    private static void sameRoleBundle(ServerLevel level, BlockPos t, Map<String, BlockPos> p, RandomSource r) {
        TankBlockEntity c = DevShowcase.tank(level, t, 3, 2, 2, 0F, true);
        DevShowcase.region(level, c, Blocks.SAND.defaultBlockState());
        BlockPos chamber = t.offset(6, 0, 1);
        BlockPos head = filter(level, chamber, Direction.WEST, 2);
        level.setBlockAndUpdate(chamber.north(), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.NORTH));
        // intake A (glass) and the return line (steel) above it
        pipe(level, t.offset(4, 0, 1), false);
        pipe(level, t.offset(4, 1, 1), true);
        pipe(level, t.offset(3, 1, 1), true);
        PipeWrenchItem.setClosed(level, t.offset(4, 0, 1), -1, Direction.UP, true);
        PipeWrenchItem.setClosed(level, t.offset(3, 1, 1), -1, Direction.DOWN, true);
        // intake B (glass) round the north
        for (int x = 3; x <= 6; x++) pipe(level, t.offset(x, 0, -1), false);
        pipe(level, t.offset(3, 0, 0), false);
        PipeWrenchItem.setClosed(level, t.offset(3, 0, 0), -1, Direction.WEST, true); // not into the tank block beside it
        // the bundle: intake A in slot 0 (east), intake B in slot 1 (from the north), both dock west at the glass
        BlockPos b = t.offset(3, 0, 1);
        pipe(level, b, false);
        PipeUpdates.flush(level);
        if (PipeBundleBlock.addPipe(level, b, false, null, null) != 1) problem("same role bundle: no second pipe at " + b);
        PipeWrenchItem.setClosed(level, b, 0, Direction.NORTH, true);
        floor(level, t, 3, 6, -1, 1);
        PipeUpdates.flush(level);
        FilterLinesGameTests.guardAll(level, head);
        PipeNetwork a = PipeNetwork.of(level, b, 0), bb = PipeNetwork.of(level, b, 1);
        if (a == null || bb == null || a == bb || a.role() != PipeRole.INTAKE || bb.role() != PipeRole.INTAKE)
            problem("same role bundle: two intake lines expected, got " + (a == null ? null : a.role()) + " / " + (bb == null ? null : bb.role()) + (a == bb ? " (one network)" : ""));
        else if (a.docks().size() != 1 || bb.docks().size() != 1) problem("same role bundle: a dock each expected: " + a.docks() + " / " + bb.docks());
        DevShowcase.fish(level, c, TankmateSpecies.GUPPY, 3, r);
        DevShowcase.name(level, t, "Bündel: zwei Zuläufe");
        p.put("same_tank", t);
        p.put("same_head", head);
        p.put("same_bundle", b);
        expectFilter(level, "same role bundle", head, 1, false, 100);
    }

    /**
     * A filter's intake and return line end in one bundle at the east glass of the upper block of a 3 x 2 x 2 tank (x 0..2): a dock
     * per line inside (blue, orange). Inside both lines run on in an interior bundle along x and part at its west end: the intake line
     * into an intake strainer, the return line into a spray bar. Filter: chambers x 7 (y 0..2), ports x 6 (y 1, 2), lines x 3..5.
     */
    private static void interiorBundle(ServerLevel level, BlockPos t, Map<String, BlockPos> p, RandomSource r) {
        TankBlockEntity c = DevShowcase.tank(level, t, 3, 2, 2, 0F, true);
        InteriorRegion reg = DevShowcase.region(level, c, Blocks.SAND.defaultBlockState());
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        BlockPos chamber = t.offset(7, 1, 0);
        level.setBlockAndUpdate(chamber.below(), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
        BlockPos head = filter(level, chamber, Direction.WEST, 2);
        for (int x = 4; x <= 5; x++) {
            pipe(level, t.offset(x, 1, 0), true);
            pipe(level, t.offset(x, 2, 0), true);
            PipeWrenchItem.setClosed(level, t.offset(x, 1, 0), -1, Direction.UP, true);
        }
        pipe(level, t.offset(3, 2, 0), true);
        BlockPos b = t.offset(3, 1, 0);
        pipe(level, b, true);
        PipeUpdates.flush(level);
        if (PipeBundleBlock.addPipe(level, b, true, null, null) != 1) problem("interior bundle: no second pipe at " + b);
        PipeWrenchItem.setClosed(level, b, 0, Direction.UP, true); // the return line comes down into slot 1
        floor(level, t, 3, 7, 0, 0);
        PipeUpdates.flush(level);
        BlockPos member = t.offset(2, 1, 0);
        long[] d = new long[2];
        for (int i = 0; i < 2; i++) d[i] = TankAnchors.ensure(level, c, member, Direction.EAST, i);
        if (d[0] == Long.MIN_VALUE || d[1] == Long.MIN_VALUE) {
            problem("interior bundle: no line docks at " + member);
        } else {
            BlockState glass = ModBlocks.INTERIOR_PIPE.get().defaultBlockState();
            int x = BlockPos.getX(d[0]), ya = BlockPos.getY(d[0]), za = BlockPos.getZ(d[0]), yb = BlockPos.getY(d[1]), zb = BlockPos.getZ(d[1]);
            int ry = ya - 1, rz = za + 1, x0 = x - 7, x1 = x - 2;
            for (int rx = x0; rx <= x1; rx++) in.setBlock(reg.toWorld(rx, ry, rz), glass, Block.UPDATE_ALL);
            for (int rx = x0; rx <= x1; rx++)
                for (int k = 0; k < 2; k++)
                    if (InteriorBundleBlock.addPipe(in, reg.toWorld(rx, ry, rz), false, null, null, Direction.WEST) < 0) problem("interior bundle: no lane at " + rx);
            // line 1 (intake): from its dock down into the middle lane, out of the west end into an intake strainer
            in.setBlock(reg.toWorld(x - 1, ya, za), glass, Block.UPDATE_ALL);
            in.setBlock(reg.toWorld(x - 1, ry, za), glass, Block.UPDATE_ALL);
            in.setBlock(reg.toWorld(x - 1, ry, rz), glass, Block.UPDATE_ALL);
            in.setBlock(reg.toWorld(x0 - 1, ry, rz), glass, Block.UPDATE_ALL);
            in.setBlock(reg.toWorld(x0 - 2, ry, rz), ModBlocks.INTAKE_STRAINER.get().defaultBlockState().setValue(PlumbingBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
            // line 2 (return): along z to above the run, into its top lane, out of the top at the west end into a spray bar
            for (int xx = x - 1; xx >= x - 3; xx--) in.setBlock(reg.toWorld(xx, yb, zb), glass, Block.UPDATE_ALL);
            for (int zz = zb - 1; zz >= rz; zz--) in.setBlock(reg.toWorld(x - 3, yb, zz), glass, Block.UPDATE_ALL);
            in.setBlock(reg.toWorld(x0, ry + 1, rz), ModBlocks.SPRAY_BAR.get().defaultBlockState().setValue(PlumbingBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
            p.put("lines_dock", reg.toWorld(x, ya, za));
            p.put("lines_run", reg.toWorld((x0 + x1) / 2, ry, rz));
        }
        PipeNetwork a = PipeNetwork.of(level, b, 0), bb = PipeNetwork.of(level, b, 1);
        if (a == null || bb == null || a.role() != PipeRole.INTAKE || bb.role() != PipeRole.RETURN)
            problem("interior bundle: intake and return expected in the bundle, got " + (a == null ? null : a.role()) + " / " + (bb == null ? null : bb.role()));
        DevShowcase.fish(level, c, TankmateSpecies.GUPPY, 3, r);
        DevShowcase.name(level, t, "Innen-Bündel");
        p.put("lines_tank", t);
        p.put("lines_head", head);
        p.put("lines_bundle", b);
        expectFilter(level, "interior bundle", head, 1, false, 100);
    }

    // ------------------------------------------------------------------ row 2: top-off and water change

    /** Builds the top-off and water change row; returns the positions for the M68 client smoke (and {@code "end"}). */
    public static Map<String, BlockPos> waterRow(ServerLevel level, BlockPos at) {
        Map<String, BlockPos> p = new LinkedHashMap<>();
        RandomSource r = RandomSource.create(681);
        int x = 1;
        // 1: the controller against a single tank: two pipes (fresh water in from a reservoir, old water out into a drain), the
        // intake guard where it drains (the face it touches), a level sensor with a lamp
        contact(level, at.offset(x, 0, 0), true, false, p, "contact");
        DevShowcase.sign(level, at.offset(x - 1, -1, -2), "Niveausensor:", "misst nur;", "Lampe an =", "zu wenig Wasser");
        DevShowcase.sign(level, at.offset(x + 1, -1, -2), "Steuerung am", "Becken: die", "Berührung", "ersetzt 2 Rohre");
        DevShowcase.sign(level, at.offset(x + 3, -1, -2), "grün: Frisch-", "wasser rein", "braun: Abwasser", "raus (Abfluss)");
        DevShowcase.sign(level, at.offset(x + 5, -1, -2), "Innen: Ansaug-", "schutz, wo die", "Steuerung", "absaugt");
        x += 6 + GAP;
        // 2: a level sensor alone: it only measures (the water is low: redstone on, the lamp lights); nothing fills it
        TankBlockEntity alone = DevShowcase.tank(level, at.offset(x, 0, 0), 2, 1, 2, 0F, true);
        TankInteractions.changeWater(level, alone, -600);
        BlockPos s2 = sensor(level, at.offset(x - 1, 0, 0));
        DevShowcase.critters(level, alone, CritterSpecies.RAMSHORN, 4);
        DevShowcase.name(level, at.offset(x, 0, 0), "Niveausensor allein");
        DevShowcase.sign(level, at.offset(x, -1, -2), "Ohne Steuerung:", "Sensor misst", "nur: Redstone", "-> Lampe an");
        if (level.getBlockEntity(s2) instanceof LevelSensorBlockEntity s) {
            s.check(level);
            if (s.status() != LevelSensorBlockEntity.Status.LOW || !level.getBlockState(s2.above()).getValue(RedstoneLampBlock.LIT))
                problem("the sensor alone: " + s.status() + ", lamp " + level.getBlockState(s2.above()));
        }
        p.put("alone_tank", at.offset(x, 0, 0));
        p.put("alone_sensor", s2);
        x += 2 + GAP;
        // 3: the controller against a multiblock filter: it changes the water of the filter's three tanks
        filterChange(level, at.offset(x, 0, 0), p, r);
        DevShowcase.sign(level, at.offset(x + 12, -1, -2), "Steuerung am", "Filter: Wechsel", "für alle drei", "Becken");
        DevShowcase.sign(level, at.offset(x + 14, -1, -2), "Frischwasser:", "Behälter;", "Abwasser:", "Abfluss");
        x += 16 + GAP;
        // 4: a central technical room: the controller stands free, all four lines are pipes
        technicalRoom(level, at.offset(x, 0, 0), p, r);
        DevShowcase.sign(level, at.offset(x + 9, -1, -2), "Technikraum:", "Steuerung frei,", "4 Anschlüsse", "über Rohre");
        DevShowcase.sign(level, at.offset(x + 11, -1, -2), "links: frisch", "rein; hinten:", "frisch raus");
        DevShowcase.sign(level, at.offset(x + 12, -1, -2), "oben: Abwasser", "rein; rechts:", "Abwasser raus");
        DevShowcase.sign(level, at.offset(x + 14, -1, -2), "Frischwasser:", "Behälter +", "Osmoseanlage", "an Wasserquelle");
        x += 16 + GAP;
        p.put("end", at.offset(x, 0, 0));
        PipeUpdates.flush(level);
        return p;
    }

    /**
     * The controller against a 3 x 2 x 2 tank at {@code t} (x 0..2): at x 3, z 1, facing east (its back against the glass). Fresh
     * water in (south): pipes x 3..4, z 2 to a reservoir at x 5; old water out (north): pipes x 3..4, z 0 (closed towards the tank)
     * into a drain at x 5. The level sensor west of the tank with a lamp on top. {@code guard}: an intake guard where it drains;
     * {@code young}: young shrimp in the tank (the M68 smoke's warning picture).
     */
    public static void contact(ServerLevel level, BlockPos t, boolean guard, boolean young, Map<String, BlockPos> p, String key) {
        TankBlockEntity c = DevShowcase.tank(level, t, 3, 2, 2, 0F, true);
        DevShowcase.region(level, c, Blocks.SAND.defaultBlockState());
        c.getTank().setNitrate(30F);
        BlockPos ch = t.offset(3, 0, 1);
        level.setBlockAndUpdate(ch, ModBlocks.WATER_CHANGE_CONTROLLER.get().defaultBlockState().setValue(WaterDeviceBlock.FACING, Direction.EAST));
        pipe(level, t.offset(3, 0, 2), true);
        pipe(level, t.offset(4, 0, 2), true);
        reservoir(level, t.offset(5, 0, 2), 16000);
        pipe(level, t.offset(3, 0, 0), true);
        PipeWrenchItem.setClosed(level, t.offset(3, 0, 0), -1, Direction.WEST, true);
        pipe(level, t.offset(4, 0, 0), true);
        level.setBlockAndUpdate(t.offset(5, 0, 0), ModBlocks.DRAIN.get().defaultBlockState());
        BlockPos sensor = sensor(level, t.offset(-1, 0, 0));
        floor(level, t, 3, 5, 0, 2);
        PipeUpdates.flush(level);
        if (guard) TankAnchors.installGuard(level, t.offset(2, 0, 1), Direction.EAST, null, new ItemStack(ModItems.INTAKE_GUARD.get()), Plumb.INTAKE_GUARD);
        DevShowcase.critters(level, c, CritterSpecies.NEOCARIDINA, 6);
        if (young) for (int i = 0; i < 12; i++) TankCritters.add(level, c, CritterSpecies.NEOCARIDINA, CritterGenes.WILD, i % 2 == 0, 0);
        DevShowcase.name(level, t, young ? "Ohne Ansaugschutz" : "Wasserwechsel am Becken");
        p.put(key + "_tank", t);
        p.put(key + "_changer", ch);
        p.put(key + "_sensor", sensor);
        p.put(key + "_drain", t.offset(5, 0, 0));
        p.put(key + "_reservoir", t.offset(5, 0, 2));
        expectChanger(level, key, ch, 1, 0, WaterChangerBlockEntity.CONTACT | WaterChangerBlockEntity.FRESH_LINE | WaterChangerBlockEntity.WASTE_LINE, !guard && young);
        if (level.getBlockEntity(sensor) instanceof LevelSensorBlockEntity s) {
            s.check(level);
            if (s.status() == LevelSensorBlockEntity.Status.NO_TANK || s.status() == LevelSensorBlockEntity.Status.OFF) problem(key + ": the sensor " + s.status());
        }
    }

    /**
     * Three 2 x 2 x 2 tanks at x 0, 4 and 8 on one multiblock filter (chambers x 13, z 4; ports x 12 facing west): the intake line
     * at the floor along z 4, the return line above it, branches to the south faces of the tanks; the wrench keeps the two lines apart.
     * The controller against the north of the lower chamber (facing north): fresh water in (east) from a reservoir, old water out
     * (west) through a pipe into a drain. A level sensor with a lamp in front of every tank.
     */
    private static void filterChange(ServerLevel level, BlockPos t, Map<String, BlockPos> p, RandomSource r) {
        int[] xs = {0, 4, 8};
        List<TankBlockEntity> tanks = new ArrayList<>();
        for (int i = 0; i < xs.length; i++) {
            BlockPos tp = t.offset(xs[i], 0, 0);
            TankBlockEntity c = DevShowcase.tank(level, tp, 2, 2, 2, 0F, true);
            DevShowcase.region(level, c, Blocks.GRAVEL.defaultBlockState());
            c.getTank().setNitrate(30F);
            tanks.add(c);
            sensor(level, tp.offset(-1, 0, 0));
            DevShowcase.fish(level, c, i == 1 ? TankmateSpecies.PLATY : TankmateSpecies.GUPPY, 3, r);
            DevShowcase.name(level, tp, "Filterbecken " + (i + 1));
            p.put("filter_tank" + i, tp);
            // branches: the intake from the south face of the lower front-left block, the return into the upper right one
            pipe(level, tp.offset(0, 0, 2), true);
            pipe(level, tp.offset(0, 0, 3), true);
            pipe(level, tp.offset(1, 1, 2), true);
            pipe(level, tp.offset(1, 1, 3), true);
        }
        for (int x = 0; x <= 11; x++) pipe(level, t.offset(x, 0, 4), true);
        for (int x = 1; x <= 11; x++) pipe(level, t.offset(x, 1, 4), true);
        for (int x = 1; x <= 11; x++) PipeWrenchItem.setClosed(level, t.offset(x, 0, 4), -1, Direction.UP, true);
        BlockPos chamber = t.offset(13, 0, 4);
        BlockPos head = filter(level, chamber, Direction.WEST, 2);
        BlockPos ch = chamber.north();
        level.setBlockAndUpdate(ch, ModBlocks.WATER_CHANGE_CONTROLLER.get().defaultBlockState().setValue(WaterDeviceBlock.FACING, Direction.NORTH));
        pipe(level, ch.east(), true);
        reservoir(level, ch.east(2), 16000);
        pipe(level, ch.west(), true);
        level.setBlockAndUpdate(ch.west().north(), ModBlocks.DRAIN.get().defaultBlockState());
        floor(level, t, -1, 15, 0, 4);
        PipeUpdates.flush(level);
        FilterLinesGameTests.guardAll(level, head);
        p.put("filter_head", head);
        p.put("filter_changer", ch);
        expectFilter(level, "filter with three tanks", head, 3, false, 100);
        expectChanger(level, "filter", ch, 3, 3, WaterChangerBlockEntity.FILTER | WaterChangerBlockEntity.FRESH_LINE | WaterChangerBlockEntity.WASTE_LINE, false);
    }

    /**
     * Two 2 x 2 x 2 tanks at x 0 and 4; the controller stands free at x 11, z 2 facing north (its panel to the path). Fresh water in
     * (east): pipes x 12..13 from a reservoir (x 14) and a reverse osmosis unit (x 13, z 1) at a water source (x 14, z 1), its waste
     * water into a drain below it. Fresh water out (back, south): down to z 4, west along z 4, branches north to the south faces of
     * the tanks. Waste water in (top): up to y 2, west along z 2 above the floor, branches docking on top of the tanks (intake guards
     * inside). Waste water out (west): pipes x 10..9 into a drain at x 8. A level sensor with a lamp in front of each tank.
     */
    private static void technicalRoom(ServerLevel level, BlockPos t, Map<String, BlockPos> p, RandomSource r) {
        int[] xs = {0, 4};
        for (int i = 0; i < xs.length; i++) {
            BlockPos tp = t.offset(xs[i], 0, 0);
            TankBlockEntity c = DevShowcase.tank(level, tp, 2, 2, 2, 0F, true);
            DevShowcase.region(level, c, Blocks.SAND.defaultBlockState());
            c.getTank().setNitrate(30F);
            sensor(level, tp.offset(-1, 0, 0));
            DevShowcase.fish(level, c, i == 0 ? TankmateSpecies.GUPPY : TankmateSpecies.PLATY, 3, r);
            DevShowcase.name(level, tp, "Technikraum: Becken " + (i + 1));
            p.put("room_tank" + i, tp);
        }
        BlockPos ch = t.offset(11, 0, 2);
        level.setBlockAndUpdate(ch, ModBlocks.WATER_CHANGE_CONTROLLER.get().defaultBlockState().setValue(WaterDeviceBlock.FACING, Direction.NORTH));
        // fresh water in: a reservoir and a reverse osmosis unit at a water source (its waste water into a drain below it)
        pipe(level, t.offset(12, 0, 2), true);
        pipe(level, t.offset(13, 0, 2), true);
        reservoir(level, t.offset(14, 0, 2), 16000);
        BlockPos ro = t.offset(13, 0, 1);
        level.setBlockAndUpdate(ro.below(), ModBlocks.DRAIN.get().defaultBlockState());
        level.setBlockAndUpdate(ro, ModBlocks.RO_UNIT.get().defaultBlockState());
        if (level.getBlockEntity(ro) instanceof RoUnitBlockEntity u) {
            u.parts().setStackInSlot(RoUnitBlockEntity.MEMBRANE, new ItemStack(ModItems.RO_MEMBRANE.get()));
            u.parts().setStackInSlot(RoUnitBlockEntity.PREFILTER, new ItemStack(ModItems.RO_PREFILTER.get()));
            u.setPure(RoUnitBlockEntity.PURE_CAPACITY);
        }
        BlockPos src = t.offset(14, 0, 1);
        for (BlockPos q : new BlockPos[] {src.below(), src.east(), src.north()}) level.setBlockAndUpdate(q, Blocks.POLISHED_ANDESITE.defaultBlockState());
        level.setBlockAndUpdate(src, Blocks.WATER.defaultBlockState());
        // fresh water out (south) to the tanks
        pipe(level, t.offset(11, 0, 3), true);
        for (int x = 1; x <= 11; x++) pipe(level, t.offset(x, 0, 4), true);
        for (int x : xs) {
            pipe(level, t.offset(x + 1, 0, 3), true);
            pipe(level, t.offset(x + 1, 0, 2), true);
        }
        // waste water in (top) from the tops of the tanks
        pipe(level, t.offset(11, 1, 2), true);
        for (int x = 1; x <= 11; x++) pipe(level, t.offset(x, 2, 2), true);
        for (int x : xs) pipe(level, t.offset(x + 1, 2, 1), true);
        // waste water out (west) into a drain
        pipe(level, t.offset(10, 0, 2), true);
        pipe(level, t.offset(9, 0, 2), true);
        level.setBlockAndUpdate(t.offset(8, 0, 2), ModBlocks.DRAIN.get().defaultBlockState());
        floor(level, t, -1, 15, 0, 4);
        PipeUpdates.flush(level);
        for (int x : xs)
            TankAnchors.installGuard(level, t.offset(x + 1, 1, 1), Direction.UP, null, new ItemStack(ModItems.INTAKE_GUARD.get()), Plumb.INTAKE_GUARD);
        p.put("room_changer", ch);
        p.put("room_ro", ro);
        p.put("room_drain", t.offset(8, 0, 2));
        expectChanger(level, "technical room", ch, 2, 0, WaterChangerBlockEntity.FRESH_LINE | WaterChangerBlockEntity.WASTE_LINE
                | WaterChangerBlockEntity.FRESH_OUT_LINE | WaterChangerBlockEntity.WASTE_IN_LINE, false);
        if (level.getBlockEntity(ch) instanceof WaterChangerBlockEntity w && w.flag(WaterChangerBlockEntity.CONTACT)) problem("technical room: the controller touches a tank");
    }

    // ------------------------------------------------------------------ checks

    /** Runs the filter once and checks the tanks it reaches, the short circuit and its line limit ({@code limit} < 0: not checked). */
    private static void expectFilter(ServerLevel level, String what, BlockPos headPos, int tanks, boolean shortCircuit, int limit) {
        MultiblockFilter.Structure st = MultiblockFilter.scan(level, headPos);
        var flow = MultiblockFilter.flow(level, st);
        if (flow.shortCircuit() != shortCircuit) problem(what + ": short circuit " + flow.shortCircuit());
        if (!(level.getBlockEntity(headPos) instanceof FilterPumpHeadBlockEntity head)) {
            problem(what + ": no pump head at " + headPos);
            return;
        }
        head.rescan();
        head.run(level, true);
        if (shortCircuit) {
            if (head.problem() != MultiblockFilter.Problem.SHORT_CIRCUIT) problem(what + ": the filter shows " + head.problem() + ", not the short circuit");
            LOGGER.info("[showcase] M68 {}: short circuit as wanted ({})", what, head.problem());
            return;
        }
        int ok = 0;
        for (var t : head.tanks()) if (t.status() == MultiblockFilter.LinkStatus.OK) ok++;
        if (head.problem() != MultiblockFilter.Problem.NONE || ok != tanks || head.tanks().size() != tanks)
            problem(what + ": " + head.problem() + ", " + ok + " of " + head.tanks().size() + " tanks OK (" + tanks + " wanted): "
                    + head.tanks().stream().map(t -> t.status().name()).toList());
        if (limit > 0 && Math.min(head.lineLimit(), 100) != limit) problem(what + ": the lines carry " + head.lineLimit() + " mB/t, " + limit + " wanted");
        LOGGER.info("[showcase] M68 {}: {} tanks, flow {} mB/t, lines carry {} mB/t", what, head.tanks().size(), head.flow(), head.lineLimit());
    }

    /** Starts a water change and checks what the controller serves, its flags and its status. */
    private static void expectChanger(ServerLevel level, String what, BlockPos at, int tanks, int filterTanks, int flags, boolean young) {
        if (!(level.getBlockEntity(at) instanceof WaterChangerBlockEntity w)) {
            problem(what + ": no water change controller at " + at);
            return;
        }
        w.set(7, 25, true);
        int moved = w.check(level, true);
        var s = w.status();
        boolean statusOk = s == WaterChangerBlockEntity.Status.CHANGING || s == WaterChangerBlockEntity.Status.CHANGED || s == WaterChangerBlockEntity.Status.TOPPED
                || s == WaterChangerBlockEntity.Status.WAITING;
        if (!statusOk || moved <= 0) problem(what + ": the controller " + s + " (moved " + moved + " mB)");
        if (w.tanks() != tanks || w.filterTanks() != filterTanks) problem(what + ": serves " + w.tanks() + " tanks (" + w.filterTanks() + " by the filter), " + tanks + " wanted");
        if ((w.flags() & flags) != flags) problem(what + ": flags " + Integer.toBinaryString(w.flags()) + ", wanted " + Integer.toBinaryString(flags));
        if (w.flag(WaterChangerBlockEntity.SHORT)) problem(what + ": a short circuit at the controller");
        if (w.flag(WaterChangerBlockEntity.YOUNG) != young) problem(what + ": sucks in young " + w.flag(WaterChangerBlockEntity.YOUNG) + ", " + young + " wanted");
        LOGGER.info("[showcase] M68 {}: {} ({} mB), {} tanks, flags {}", what, s, moved, w.tanks(), Integer.toBinaryString(w.flags()));
    }

    private static void problem(String what) {
        PROBLEMS.add(what);
        LOGGER.warn("[showcase] M68 PROBLEM: {}", what);
    }

    // ------------------------------------------------------------------ helpers

    private static void pipe(ServerLevel level, BlockPos pos, boolean steel) {
        level.setBlockAndUpdate(pos, (steel ? ModBlocks.STAINLESS_STEEL_PIPE : ModBlocks.GLASS_PIPE).get().defaultBlockState());
    }

    /** Two chambers (ceramic rings, foam) at {@code chamber} and above, the pump head on top, inlet and outlet towards {@code out}. */
    private static BlockPos filter(ServerLevel level, BlockPos chamber, Direction out, int chambers) {
        for (int y = 0; y < chambers; y++) {
            level.setBlockAndUpdate(chamber.above(y), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
            if (level.getBlockEntity(chamber.above(y)) instanceof FilterChamberBlockEntity f)
                f.setMedium(new ItemStack(y == 0 ? ModItems.CERAMIC_RINGS.get() : ModItems.FILTER_FOAM.get()));
        }
        level.setBlockAndUpdate(chamber.above(chambers), ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState());
        level.setBlockAndUpdate(chamber.relative(out), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, out));
        level.setBlockAndUpdate(chamber.above().relative(out), ModBlocks.FILTER_OUTLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, out));
        return chamber.above(chambers);
    }

    private static void reservoir(ServerLevel level, BlockPos pos, int mb) {
        level.setBlockAndUpdate(pos, ModBlocks.RESERVOIR.get().defaultBlockState());
        if (level.getBlockEntity(pos) instanceof ReservoirBlockEntity res)
            res.water().fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, mb), IFluidHandler.FluidAction.EXECUTE);
    }

    /** A level sensor at {@code pos} (beside a tank) with a redstone lamp on top and a block below. */
    private static BlockPos sensor(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos.below()).isAir()) level.setBlockAndUpdate(pos.below(), Blocks.POLISHED_ANDESITE.defaultBlockState());
        level.setBlockAndUpdate(pos, ModBlocks.LEVEL_SENSOR.get().defaultBlockState());
        level.setBlockAndUpdate(pos.above(), Blocks.REDSTONE_LAMP.defaultBlockState());
        return pos;
    }

    /** Polished andesite on the ground (one below {@code at}) where it is air, x0..x1, z0..z1 relative to {@code at}. */
    private static void floor(ServerLevel level, BlockPos at, int x0, int x1, int z0, int z1) {
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++) {
                BlockPos q = at.offset(x, -1, z);
                if (level.getBlockState(q).isAir()) level.setBlockAndUpdate(q, Blocks.POLISHED_ANDESITE.defaultBlockState());
            }
    }
}
