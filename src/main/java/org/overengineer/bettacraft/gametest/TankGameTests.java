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

    @GameTest(template = EMPTY)
    public static void nonCuboidStaysSeparateThenMerges(GameTestHelper h) {
        BlockPos a = new BlockPos(2, 1, 2), b = new BlockPos(3, 1, 2), c = new BlockPos(2, 1, 3), d = new BlockPos(3, 1, 3);
        place(h, List.of(a, b));
        h.runAfterDelay(2, () -> {
            place(h, List.of(c));
            h.runAfterDelay(2, () -> {
                h.assertTrue(controller(h, c) != controller(h, a), "L-shape must not connect");
                h.assertTrue(controller(h, c).getTank().memberCount() == 1, "C alone");
                place(h, List.of(d));
                h.runAfterDelay(2, () -> {
                    TankBlockEntity ctl = controller(h, a);
                    for (BlockPos p : List.of(a, b, c, d)) h.assertTrue(controller(h, p) == ctl, "merged 2x1x2");
                    h.assertTrue(ctl.getTank().memberCount() == 4, "4 members");
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
            h.assertTrue(h.getBlockState(new BlockPos(4, 2, 4)).getValue(TankBlock.FILL) == 8, "top layer half");
            h.assertTrue(h.getLevel().getFluidState(h.absolutePos(new BlockPos(4, 2, 4))).is(Fluids.WATER), "vanilla water");
            for (int i = 0; i < 2; i++) TankInteractions.changeWater(level, c, TankData.MB_PER_BLOCK);
            h.assertTrue(h.getBlockState(new BlockPos(5, 2, 5)).getValue(TankBlock.FILL) == 16, "full");
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
            h.assertTrue(seen.size() == 2, "L remainder split into 2 cuboids, got " + seen.size());
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

    /** Logs the interior cost numbers (M0 spike). Real shells need the interior dimension, which the GameTest server lacks. */
    @GameTest(template = EMPTY, timeoutTicks = 2000)
    public static void interiorCost(GameTestHelper h) {
        InteriorBench.runDefault();
        InteriorBench.realRegions(h.getLevel().getServer());
        h.succeed();
    }
}
