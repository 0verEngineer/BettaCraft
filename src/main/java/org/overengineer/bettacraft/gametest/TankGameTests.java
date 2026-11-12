package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorBench;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Multiblock, water and interior GameTests (run with {@code gradlew runGameTestServer}). */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TankGameTests {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/gametest");
    private static final String EMPTY = "empty_32x10x32";

    private TankGameTests() {}

    private static List<BlockPos> box(int x, int y, int z, int sx, int sy, int sz) {
        List<BlockPos> l = new ArrayList<>();
        for (int a = 0; a < sx; a++) for (int b = 0; b < sy; b++) for (int c = 0; c < sz; c++) l.add(new BlockPos(x + a, y + b, z + c));
        return l;
    }

    private static void place(GameTestHelper h, List<BlockPos> rel) {
        for (BlockPos p : rel) h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
    }

    private static TankBlockEntity controller(GameTestHelper h, BlockPos rel) {
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(rel));
        if (c == null) throw new net.minecraft.gametest.framework.GameTestAssertException("no controller for " + rel);
        return c;
    }

    @GameTest(template = EMPTY)
    public static void cuboidForms(GameTestHelper h) {
        List<BlockPos> blocks = box(2, 1, 2, 3, 2, 2);
        place(h, blocks);
        h.runAfterDelay(3, () -> {
            TankBlockEntity c = controller(h, blocks.get(0));
            for (BlockPos p : blocks) h.assertTrue(controller(h, p) == c, "all blocks share one controller");
            TankData t = c.getTank();
            h.assertTrue(t.memberCount() == 12 && t.isCuboid() && t.sizeX() == 3 && t.sizeY() == 2, "3x2x2 tank: " + t);
            BlockState s = h.getBlockState(new BlockPos(3, 1, 2));
            h.assertTrue(TankBlock.isConnected(s, Direction.EAST) && TankBlock.isConnected(s, Direction.WEST)
                    && !TankBlock.isConnected(s, Direction.NORTH) && TankBlock.isConnected(s, Direction.UP), "connection flags");
            h.succeed();
        });
    }

    /** Placing a 3x3x3 block by block (like a player, layer by layer) must end as one tank. */
    @GameTest(template = EMPTY)
    public static void blockByBlockBuildsOneTank(GameTestHelper h) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 1; y <= 3; y++) for (int z = 2; z <= 4; z++) for (int x = 2; x <= 4; x++) blocks.add(new BlockPos(x, y, z));
        for (BlockPos p : blocks) {
            h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
            TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
        }
        h.runAfterDelay(2, () -> {
            TankBlockEntity c = controller(h, blocks.get(0));
            h.assertTrue(c.getTank().memberCount() == 27, "one 3x3x3 tank, got " + c.getTank());
            for (BlockPos p : blocks) h.assertTrue(controller(h, p) == c, "same controller");
            h.succeed();
        });
    }

    /** M10: an L connects (free shapes); the fourth block completes it to a 2x1x2. Cuboid-only: see ShapeGameTests. */
    @GameTest(template = EMPTY)
    public static void lShapeConnectsThenCompletes(GameTestHelper h) {
        BlockPos a = new BlockPos(2, 1, 2), b = new BlockPos(3, 1, 2), c = new BlockPos(2, 1, 3), d = new BlockPos(3, 1, 3);
        place(h, List.of(a, b));
        h.runAfterDelay(2, () -> {
            place(h, List.of(c));
            h.runAfterDelay(2, () -> {
                h.assertTrue(controller(h, c) == controller(h, a), "L-shape connects");
                h.assertTrue(controller(h, c).getTank().memberCount() == 3 && !controller(h, c).getTank().isCuboid(), "3-block L");
                place(h, List.of(d));
                h.runAfterDelay(2, () -> {
                    TankBlockEntity ctl = controller(h, a);
                    for (BlockPos p : List.of(a, b, c, d)) h.assertTrue(controller(h, p) == ctl, "merged 2x1x2");
                    h.assertTrue(ctl.getTank().memberCount() == 4 && ctl.getTank().isCuboid(), "4 members, cuboid");
                    h.succeed();
                });
            });
        });
    }

    @GameTest(template = EMPTY)
    public static void differentGlassDoesNotConnect(GameTestHelper h) {
        BlockPos a = new BlockPos(2, 1, 2), b = new BlockPos(3, 1, 2);
        place(h, List.of(a));
        h.runAfterDelay(2, () -> {
            h.setBlock(b, ModBlocks.TANK.get().defaultBlockState());
            if (h.getLevel().getBlockEntity(h.absolutePos(b)) instanceof TankBlockEntity be) {
                be.setGlassId(net.minecraft.resources.ResourceLocation.withDefaultNamespace("tinted_glass"));
            }
            h.runAfterDelay(3, () -> {
                h.assertTrue(controller(h, a) != controller(h, b), "different glass stays separate");
                h.succeed();
            });
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void waterFillsBottomUpAndDoesNotLeak(GameTestHelper h) {
        List<BlockPos> blocks = box(4, 1, 4, 2, 2, 2);
        place(h, blocks);
        h.runAfterDelay(3, () -> {
            TankBlockEntity c = controller(h, blocks.get(0));
            ServerLevel level = h.getLevel();
            for (int i = 0; i < 6; i++) TankInteractions.changeWater(level, c, TankData.MB_PER_BLOCK);
            h.assertTrue(c.getTank().waterMb() == 6000, "6000 mB");
            h.assertTrue(h.getBlockState(new BlockPos(4, 1, 4)).getValue(TankBlock.FILL) == 16, "bottom layer full");
            h.assertTrue(h.getBlockState(new BlockPos(4, 2, 4)).getValue(TankBlock.FILL) == (16 - TankData.headroom()) / 2,
                    "top layer half (16 - headroom sixteenths when full)");
            h.assertTrue(h.getLevel().getFluidState(h.absolutePos(new BlockPos(4, 2, 4))).is(Fluids.WATER), "vanilla water");
            for (int i = 0; i < 2; i++) TankInteractions.changeWater(level, c, TankData.MB_PER_BLOCK);
            h.assertTrue(h.getBlockState(new BlockPos(5, 2, 5)).getValue(TankBlock.FILL) == 16 - TankData.headroom(), "full (top layer: air gap below the glass)");
            TankInteractions.changeWater(level, c, TankData.MB_PER_BLOCK);
            h.assertTrue(c.getTank().waterMb() == 8000, "capacity is capped");
            // poke neighbours so any fluid logic would run
            h.setBlock(new BlockPos(6, 1, 4), Blocks.STONE);
            h.setBlock(new BlockPos(6, 1, 4), Blocks.AIR);
        });
        h.runAfterDelay(120, () -> {
            for (int x = 3; x <= 6; x++)
                for (int y = 1; y <= 3; y++)
                    for (int z = 3; z <= 6; z++) {
                        BlockPos p = new BlockPos(x, y, z);
                        boolean inside = x >= 4 && x <= 5 && y <= 2 && z >= 4 && z <= 5;
                        if (!inside) h.assertTrue(h.getBlockState(p).isAir(), "water leaked to " + p);
                    }
            h.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void breakingRemovesWaterAndSplits(GameTestHelper h) {
        List<BlockPos> blocks = box(4, 1, 4, 2, 1, 2);
        place(h, blocks);
        h.runAfterDelay(3, () -> {
            TankBlockEntity c = controller(h, blocks.get(0));
            TankInteractions.changeWater(h.getLevel(), c, 4000);
            // destroyBlock would normally leave the fluid's legacy block (a water source) behind
            h.getLevel().destroyBlock(h.absolutePos(new BlockPos(4, 1, 4)), true);
        });
        h.runAfterDelay(8, () -> {
            h.assertTrue(h.getBlockState(new BlockPos(4, 1, 4)).isAir(), "broken block is air, not water");
            long total = 0;
            java.util.Set<TankBlockEntity> seen = new java.util.HashSet<>();
            for (BlockPos p : List.of(new BlockPos(5, 1, 4), new BlockPos(4, 1, 5), new BlockPos(5, 1, 5))) {
                TankBlockEntity ctl = controller(h, p);
                if (seen.add(ctl)) total += ctl.getTank().waterMb();
            }
            h.assertTrue(total == 3000, "remaining water 3000 mB, got " + total);
            h.assertTrue(seen.size() == 1, "M10: the L remainder stays one tank, got " + seen.size());
        });
        h.runAfterDelay(100, () -> {
            h.assertTrue(h.getBlockState(new BlockPos(4, 1, 4)).isAir(), "no water came back");
            h.assertTrue(h.getBlockState(new BlockPos(3, 1, 4)).isAir(), "no leak");
            h.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void connectsAcrossChunkBorder(GameTestHelper h) {
        int ax = h.absolutePos(BlockPos.ZERO).getX();
        int r = 1;
        while (Math.floorMod(ax + r, 16) != 15) r++;
        BlockPos a = new BlockPos(r, 1, 2), b = new BlockPos(r + 1, 1, 2);
        place(h, List.of(a, b));
        h.runAfterDelay(3, () -> {
            h.assertTrue(h.absolutePos(a).getX() >> 4 != h.absolutePos(b).getX() >> 4, "blocks are in different chunks");
            h.assertTrue(controller(h, a) == controller(h, b) && controller(h, a).getTank().memberCount() == 2, "one tank across the border");
            h.succeed();
        });
    }

    /**
     * Logs the interior cost numbers: synthetic scenes and real 16x16x16 regions prepared asynchronously (M3). It is a
     * measurement, not a check: preparing two 256^3 regions while the other GameTests run in parallel can take longer
     * than any fixed timeout (that made it flaky in M7). After {@link #BENCH_BUDGET} ticks the real-region part is
     * aborted and logged as incomplete; the synthetic part has been measured by then in any case.
     */
    @GameTest(template = EMPTY, timeoutTicks = BENCH_BUDGET + 200)
    public static void interiorCost(GameTestHelper h) {
        InteriorBench.runDefault();
        var server = h.getLevel().getServer();
        if (InteriorBench.realRunning()) InteriorBench.abortReal(server); // left over from an earlier run on this server
        if (!InteriorBench.startRealRegions(server, line -> {})) {
            h.succeed();
            return;
        }
        long start = h.getTick();
        h.onEachTick(() -> {
            if (!InteriorBench.realRunning()) {
                h.succeed();
            } else if (h.getTick() - start >= BENCH_BUDGET) {
                LOGGER.warn("interiorCost: real-region bench incomplete after {} ticks ({}), aborted", BENCH_BUDGET, InteriorBench.realProgress());
                InteriorBench.abortReal(server);
                h.succeed();
            }
        });
    }

    private static final int BENCH_BUDGET = 3000;
}
