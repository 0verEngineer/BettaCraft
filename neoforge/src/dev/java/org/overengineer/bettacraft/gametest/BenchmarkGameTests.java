package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaCare;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateCare;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.monitor.DashboardLayout;
import org.overengineer.bettacraft.monitor.DashboardTile;
import org.overengineer.bettacraft.monitor.MonitorBlock;
import org.overengineer.bettacraft.monitor.MonitorBlockEntity;
import org.overengineer.bettacraft.monitor.MonitorSync;
import org.overengineer.bettacraft.monitor.MonitorValue;
import org.overengineer.bettacraft.plumbing.PipeNetwork;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.terrarium.HeatMatBlock;
import org.overengineer.bettacraft.terrarium.MisterBlockEntity;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.tank.TankEquipment;

/**
 * M33 part d (Hardening 2): benchmarks of the systems of M21-M32 with loose upper bounds (they catch an order of magnitude,
 * not a few percent). Each one times the server work directly, so the numbers are the cost of our code alone; the log line
 * {@code [gametest] benchmark: ...} gives the time per step and per tick (the care runs every 20 ticks, the monitors every
 * 20, the pumps every 10). Results go into meilensteine/M33.md.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BenchmarkGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final String BATCH = "bettacraft_m33d_bench";

    private BenchmarkGameTests() {}

    private static void report(String what, double msPerStep, int ticksPerStep) {
        com.mojang.logging.LogUtils.getLogger().info("[gametest] benchmark: {}: {} ms per step, {} ms per tick", what,
                String.format(Locale.ROOT, "%.3f", msPerStep), String.format(Locale.ROOT, "%.3f", msPerStep / ticksPerStep));
    }

    /** Adult females (no fights, no nests) and neons; returns the live entities. */
    private static List<Entity> stock(GameTestHelper h, TankBlockEntity c, int bettas, int neons, int critters, long seed) {
        ServerLevel level = h.getLevel();
        for (int i = 0; i < bettas; i++) {
            BettaData d = BettaData.create(BettaGenotype.randomDomestic(RandomSource.create(seed + i)), true, 0);
            var err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d), ModEntities.BETTA.get(), null, false);
            if (err != null) throw new GameTestFailure("betta: " + err.getString());
        }
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        for (int i = 0; i < neons; i++) {
            Vec3 p = w.clamp(new Vec3(w.box().getCenter().x + (i % 5) * 0.3, w.box().minY + 0.4 + (i / 5) * 0.2, w.box().getCenter().z), 0.35, 0.1, 0.2);
            TankmateEntity e = TankmateBreeding.spawn(level, c, TankmateSpecies.NEON_TETRA, TankmateGenes.wild(TankmateSpecies.NEON_TETRA), i % 2 == 0,
                    LifeStage.ADULT, 0, p);
            if (e == null) throw new GameTestFailure("could not spawn a neon");
        }
        CritterSpecies[] kinds = {CritterSpecies.NEOCARIDINA, CritterSpecies.ZEBRA_NERITE, CritterSpecies.RAMSHORN};
        for (int i = 0; i < critters; i++) TankCritters.add(level, c, kinds[i % 3], CritterGenes.WILD, i % 2 == 0, kinds[i % 3].juvenileDays() + 1);
        return new ArrayList<>(TankInhabitants.liveInhabitants(level, c.getTank()));
    }

    /** One whole care step of a tank as the game runs it every 20 ticks: the tank, then the entity side of its fish. */
    private static void careStep(ServerLevel level, TankBlockEntity c, List<Entity> animals) {
        TankCare.care(level, c, 20);
        for (Entity e : animals) {
            if (!e.isAlive()) continue;
            if (e instanceof BettaEntity b) BettaCare.advance(level, c, b, 20 / 24000.0);
            else if (e instanceof TankmateEntity m) TankmateCare.advance(level, c, m, 20 / 24000.0);
        }
    }

    private static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz) {
        return AerationGameTests.tank(h, x, y, z, sx, sy, sz);
    }

    // ------------------------------------------------------------------ many tanks

    /** 36 tanks (2x2x2) with 2 bettas, 2 neons and 4 small animals each: the care of all of them in one step. */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 600)
    public static void benchmarkCareOfManyTanks(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        List<TankBlockEntity> tanks = new ArrayList<>();
        List<List<Entity>> animals = new ArrayList<>();
        for (int i = 0; i < 6; i++) for (int j = 0; j < 6; j++) {
            TankBlockEntity c = tank(h, 1 + i * 5, 1, 1 + j * 5, 2, 2, 2);
            tanks.add(c);
            animals.add(stock(h, c, 2, 2, 4, 100L * i + j));
        }
        for (int r = 0; r < 3; r++) for (int i = 0; i < tanks.size(); i++) careStep(level, tanks.get(i), animals.get(i)); // warm up
        int steps = 10;
        long t0 = System.nanoTime();
        for (int r = 0; r < steps; r++) for (int i = 0; i < tanks.size(); i++) careStep(level, tanks.get(i), animals.get(i));
        double ms = (System.nanoTime() - t0) / 1e6 / steps;
        report("care of 36 stocked 2x2x2 tanks (72 bettas, 72 neons, 144 small animals)", ms, 20);
        h.assertTrue(ms < 40, "the care of 36 tanks stays below 2 ms per tick: " + ms + " ms per step");
        h.succeed();
    }

    // ------------------------------------------------------------------ a big tank

    /** A 12x4x8 tank with 20 bettas, 60 neons and 150 small animals: one care step. */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 900)
    public static void benchmarkBigTankWithManyAnimals(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 1, 2, 12, 4, 8);
        List<Entity> animals = stock(h, c, 20, 60, 150, 7);
        h.assertTrue(animals.size() >= 70, "stocked: " + animals.size());
        for (int r = 0; r < 3; r++) careStep(level, c, animals);
        int steps = 10;
        long t0 = System.nanoTime();
        for (int r = 0; r < steps; r++) careStep(level, c, animals);
        double ms = (System.nanoTime() - t0) / 1e6 / steps;
        report("care of a 12x4x8 tank with " + animals.size() + " fish and " + TankCritters.alive(c.getTank()) + " small animals", ms, 20);
        h.assertTrue(ms < 15, "a big stocked tank stays below 0.75 ms per tick: " + ms + " ms per step");
        h.succeed();
    }

    // ------------------------------------------------------------------ pipes

    /** 1000 glass pipes (a 10x9x12 block, cut at 1000) between two tanks: building the network and moving a batch. */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 600)
    public static void benchmarkPipeNetwork(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity a = tank(h, 1, 1, 1, 2, 2, 2);
        TankBlockEntity b = tank(h, 13, 1, 1, 2, 2, 2);
        BlockState pipe = ModBlocks.GLASS_PIPE.get().defaultBlockState();
        for (int x = 3; x < 13; x++) for (int y = 1; y < 10; y++) for (int z = 1; z < 13; z++) {
            if ((x - 3) * 108 + (y - 1) * 12 + (z - 1) >= 1000) continue;
            h.setBlock(new BlockPos(x, y, z), pipe);
        }
        b.getTank().setWaterQuality(1F);
        BlockPos start = h.absolutePos(new BlockPos(3, 1, 1));
        int builds = 20;
        long t0 = System.nanoTime();
        PipeNetwork n = null;
        for (int i = 0; i < builds; i++) {
            PipeNetwork.invalidate(level);
            n = PipeNetwork.of(level, start);
        }
        double buildMs = (System.nanoTime() - t0) / 1e6 / builds;
        if (n == null) throw new GameTestFailure("no network");
        h.assertTrue(n.pipes().size() >= 1000 || n.pipes().size() == PipeNetwork.MAX_PIPES, "1000 pipes in one network: " + n.pipes().size());
        h.assertTrue(n.tankEnds(level).size() >= 2, "both tanks are ends: " + n.tankEnds(level).size());
        report("building a network of " + n.pipes().size() + " pipes with " + n.ends().size() + " ends", buildMs, 1);
        // moving water through it (the cached network), one batch per window as a pump does
        int batches = 50;
        long moved = 0;
        long t1 = System.nanoTime();
        for (int i = 0; i < batches; i++) {
            moved += n.fill(level, new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.SIMULATE, null);
            moved += n.drain(level, 100, null, IFluidHandler.FluidAction.SIMULATE, null).getAmount();
        }
        double moveMs = (System.nanoTime() - t1) / 1e6 / batches;
        report("a pump batch (fill + drain, simulated) through the 1000-pipe network", moveMs, PipeNetwork.WINDOW);
        h.assertTrue(moved > 0, "water moves");
        h.assertTrue(buildMs < 20, "building 1000 pipes stays below 20 ms: " + buildMs);
        h.assertTrue(moveMs < 1, "a batch stays below 1 ms: " + moveMs);
        h.succeed();
    }

    // ------------------------------------------------------------------ monitors

    /** An 8x5 screen watching 12 tanks with 12 alarm tiles and 12 value tiles (history): one cycle. */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 600)
    public static void benchmarkMonitorWithAlarms(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        List<TankBlockEntity> tanks = new ArrayList<>();
        for (int i = 0; i < 4; i++) for (int j = 0; j < 3; j++) {
            TankBlockEntity c = tank(h, 1 + i * 4, 1, 1 + j * 4, 2, 2, 2);
            c.getTank().setWaterTemp(i % 2 == 0 ? 30F : 25F); // half of them in alarm
            tanks.add(c);
        }
        BlockState s = ModBlocks.MONITOR.get().defaultBlockState().setValue(MonitorBlock.FACING, Direction.NORTH);
        BlockPos topLeft = new BlockPos(28, 6, 20);
        for (int y = 0; y < 5; y++) for (int x = 0; x < 8; x++) h.setBlock(topLeft.west(x).below(y), s);
        if (!(level.getBlockEntity(h.absolutePos(topLeft)) instanceof MonitorBlockEntity m) || m.controller() == null) {
            throw new GameTestFailure("no monitor screen");
        }
        MonitorBlockEntity c = m.controller();
        DashboardLayout l = new DashboardLayout();
        l.auto = false;
        for (int i = 0; i < tanks.size(); i++) {
            int x = (i % 4) * 8, y = (i / 4) * 6;
            l.tiles.add(DashboardTile.alarm(l.nextId++, x, y, 4, 3, tanks.get(i).getTank().id(), MonitorValue.TEMPERATURE, false, 28F, true));
            l.tiles.add(DashboardTile.value(l.nextId++, x + 4, y, 4, 3, tanks.get(i).getTank().id(), MonitorValue.TEMPERATURE, true));
        }
        c.setLayout(l);
        for (int i = 0; i < 6; i++) MonitorSync.cycle(level, c); // discovers the tanks
        // all 12 (a tank left by a neighbouring test may be in its range too)
        h.assertTrue(tanks.stream().allMatch(t -> c.tanks().stream().anyMatch(w -> w.id.equals(t.getTank().id()))),
                "the screen watches all 12 tanks: " + c.tanks().size());
        h.assertTrue(c.alarms() == 6, "six alarms: " + c.alarms());
        int cycles = 25;
        long t0 = System.nanoTime();
        for (int i = 0; i < cycles; i++) MonitorSync.cycle(level, c);
        double ms = (System.nanoTime() - t0) / 1e6 / cycles;
        report("an 8x5 monitor with 12 tanks, 12 alarms and 12 value tiles (every 5th cycle discovers)", ms, MonitorSync.INTERVAL);
        h.assertTrue(ms < 4, "a monitor cycle stays below 4 ms: " + ms);
        h.succeed();
    }

    // ------------------------------------------------------------------ terrarium

    /** A 3x2x3 terrarium with land, heat mats, a basking lamp and a mister: one climate step and one land scan. */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 600)
    public static void benchmarkTerrariumClimate(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = CritterGameTests.tank(h, 2, 2, 2, 3, 2, 3, 0);
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        if (r == null || in == null) throw new GameTestFailure("no interior");
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
            if (x < r.sizeX() / 2) for (int y = 0; y < 6; y++) in.setBlock(r.toWorld(x, y, z), Blocks.DIRT.defaultBlockState(), 3);
        }
        for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++) {
            h.setBlock(new BlockPos(x, 1, z), ModBlocks.HEAT_MAT.get().defaultBlockState().setValue(HeatMatBlock.FACING, Direction.UP));
            if (x == 2 && z == 2) h.setBlock(new BlockPos(x, 4, z), ModBlocks.BASKING_LAMP.get().defaultBlockState());
            else if (x == 4 && z == 4) h.setBlock(new BlockPos(x, 4, z), ModBlocks.MISTER.get().defaultBlockState());
            else h.setBlock(new BlockPos(x, 4, z), ModBlocks.TANK_LID.get().defaultBlockState());
        }
        TankEquipment.invalidate(c);
        if (level.getBlockEntity(h.absolutePos(new BlockPos(4, 4, 4))) instanceof MisterBlockEntity mister) {
            mister.water().fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
            mister.setSetting(90);
        }
        TankData t = c.getTank();
        t.setTargetTemp(30F);
        for (int i = 0; i < 5; i++) TankClimate.step(level, c, 20 / 24000.0);
        int steps = 40;
        long t0 = System.nanoTime();
        for (int i = 0; i < steps; i++) TankClimate.step(level, c, 20 / 24000.0);
        double ms = (System.nanoTime() - t0) / 1e6 / steps;
        report("terrarium climate step (3x2x3, 9 heat mats, lamp, mister)", ms, 20);
        int scans = 10;
        long t1 = System.nanoTime();
        for (int i = 0; i < scans; i++) TankClimate.scanLand(level.getServer(), t);
        double scanMs = (System.nanoTime() - t1) / 1e6 / scans;
        report("terrarium land scan (every " + TankClimate.SCAN_TICKS + " ticks)", scanMs, (int) TankClimate.SCAN_TICKS);
        h.assertTrue(ms < 1, "a climate step stays below 1 ms: " + ms);
        h.assertTrue(scanMs < 5, "a land scan stays below 5 ms: " + scanMs);
        h.succeed();
    }
}
