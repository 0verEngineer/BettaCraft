package org.overengineer.bettacraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;

/**
 * M29.1: the interior is dry. Breaking kelp, seagrass, corals, sea pickles or waterlogged decor in the interior (shears,
 * hand, a piston, a water bucket or dispenser) leaves air, no vanilla water block; stray water blocks of earlier
 * versions go on the next rescan and the plants stay. Outside the interior a broken kelp still leaves its water.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InteriorWaterGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private InteriorWaterGameTests() {}

    private static InteriorRegion region(GameTestHelper h) {
        for (int a = 0; a < 2; a++)
            for (int c = 0; c < 2; c++) {
                BlockPos p = new BlockPos(2 + a, 1, 2 + c);
                h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
                TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
            }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 2)));
        if (c == null) throw new GameTestAssertException("tank did not form");
        if (InteriorManager.interiorLevel(h.getLevel().getServer()) == null) throw new GameTestAssertException("interior dimension not loaded");
        return InteriorManager.ensureRegion(h.getLevel(), c);
    }

    private static boolean noWater(BlockState s) {
        return !s.is(Blocks.WATER);
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void brokenWaterPlantsLeaveNoWaterInTheInterior(GameTestHelper h) {
        InteriorRegion r = region(h);
        ServerLevel in = InteriorManager.interiorLevel(h.getLevel().getServer());
        for (int x = 2; x < 28; x++) in.setBlock(r.toWorld(x, 0, 8), Blocks.STONE.defaultBlockState(), 3);
        BlockPos kelpBottom = r.toWorld(4, 1, 8), kelpTop = r.toWorld(4, 2, 8);
        in.setBlock(kelpBottom, Blocks.KELP_PLANT.defaultBlockState(), 3);
        in.setBlock(kelpTop, Blocks.KELP.defaultBlockState(), 3);
        BlockPos seagrass = r.toWorld(7, 1, 8);
        in.setBlock(seagrass, Blocks.SEAGRASS.defaultBlockState(), 3);
        BlockPos tallLow = r.toWorld(10, 1, 8), tallHigh = r.toWorld(10, 2, 8);
        in.setBlock(tallLow, Blocks.TALL_SEAGRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), 2);
        in.setBlock(tallHigh, Blocks.TALL_SEAGRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), 2);
        BlockPos coral = r.toWorld(13, 1, 8);
        in.setBlock(coral, Blocks.BRAIN_CORAL.defaultBlockState(), 3);
        BlockPos fan = r.toWorld(16, 1, 8);
        in.setBlock(fan, Blocks.TUBE_CORAL_FAN.defaultBlockState(), 3);
        BlockPos slab = r.toWorld(19, 1, 8);
        in.setBlock(slab, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.WATERLOGGED, true), 3);
        h.assertTrue(in.getFluidState(kelpBottom).isSource() && in.getFluidState(slab).isSource(), "the water plants and decor carry water");

        ServerPlayer sp = MockPlayers.serverPlayer(h);
        BlockPos stand = r.toWorld(12, 2, 4);
        sp.teleportTo(in, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0F, 0F);
        sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        try {
            // shears: kelp (the top, then the bottom), seagrass and tall seagrass
            sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
            sp.gameMode.destroyBlock(kelpTop);
            sp.gameMode.destroyBlock(kelpBottom);
            sp.gameMode.destroyBlock(seagrass);
            sp.gameMode.destroyBlock(tallLow);
            // hand: corals and the waterlogged slab
            sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            sp.gameMode.destroyBlock(coral);
            sp.gameMode.destroyBlock(fan);
            sp.gameMode.destroyBlock(slab);
            for (BlockPos p : new BlockPos[]{kelpTop, kelpBottom, seagrass, tallLow, tallHigh, coral, fan, slab}) {
                h.assertTrue(in.getBlockState(p).isAir(), "air after breaking at " + r.localX(p.getX()) + "," + r.localY(p.getY()) + ": "
                        + in.getBlockState(p));
            }
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }

        // a kelp column whose bottom is cut away: the rest never turns into water
        BlockPos colBottom = r.toWorld(22, 1, 8), colTop = r.toWorld(22, 2, 8);
        in.setBlock(colBottom, Blocks.KELP_PLANT.defaultBlockState(), 3);
        in.setBlock(colTop, Blocks.KELP.defaultBlockState(), 3);
        in.destroyBlock(colBottom, true);
        h.assertTrue(in.getBlockState(colBottom).isAir(), "level.destroyBlock: air");

        // a piston crushes a sea pickle (push reaction destroy): air, no water
        BlockPos power = r.toWorld(24, 1, 12), piston = r.toWorld(25, 1, 12), pickle = r.toWorld(26, 1, 12);
        in.setBlock(r.toWorld(26, 0, 12), Blocks.STONE.defaultBlockState(), 3);
        in.setBlock(pickle, Blocks.SEA_PICKLE.defaultBlockState(), 3);
        in.setBlock(power, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        BlockState ps = Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST);
        in.setBlock(piston, ps, 2);
        in.getBlockState(piston).triggerEvent(in, piston, 0, Direction.EAST.get3DDataValue());
        h.assertTrue(noWater(in.getBlockState(pickle)), "piston: no water where the sea pickle was: " + in.getBlockState(pickle));

        // a water bucket or a dispenser pours a water block: in the interior it is gone at once
        BlockPos poured = r.toWorld(5, 3, 14);
        in.setBlock(poured, Blocks.WATER.defaultBlockState(), 11);
        h.assertTrue(in.getBlockState(poured).isAir(), "no vanilla water block in the interior");

        // stray water of earlier versions (bypassing the fix): the rescan removes it and leaves the plants alone
        BlockPos stray = r.toWorld(8, 1, 14), kept = r.toWorld(9, 1, 14);
        in.setBlock(r.toWorld(9, 0, 14), Blocks.STONE.defaultBlockState(), 3);
        in.setBlock(kept, Blocks.SEAGRASS.defaultBlockState(), 3);
        in.getChunkAt(stray).setBlockState(stray, Blocks.WATER.defaultBlockState(), false);
        h.assertTrue(in.getBlockState(stray).is(Blocks.WATER), "stray water set up");
        r.equipment().markNeedsScan();
        InteriorManager.equipment(h.getLevel().getServer(), r);
        h.assertTrue(in.getBlockState(stray).isAir(), "the rescan removes the stray water: " + in.getBlockState(stray));
        h.assertTrue(in.getBlockState(kept).is(Blocks.SEAGRASS) && in.getBlockState(kept).getFluidState().isSource(), "the seagrass stays");

        // outside the interior everything stays vanilla: a broken kelp leaves its water
        BlockPos out = new BlockPos(20, 2, 20);
        h.setBlock(out.below(), Blocks.STONE);
        h.setBlock(out, Blocks.KELP);
        h.getLevel().destroyBlock(h.absolutePos(out), false);
        h.assertTrue(h.getLevel().getBlockState(h.absolutePos(out)).is(Blocks.WATER), "outside: vanilla water left behind");
        h.getLevel().setBlock(h.absolutePos(out), Blocks.AIR.defaultBlockState(), 3);
        h.runAfterDelay(5, () -> {
            h.assertTrue(noWater(in.getBlockState(colTop)), "the kelp above the cut never becomes water: " + in.getBlockState(colTop));
            h.succeed();
        });
    }
}
