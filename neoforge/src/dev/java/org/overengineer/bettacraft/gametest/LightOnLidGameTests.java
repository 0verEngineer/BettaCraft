package org.overengineer.bettacraft.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.equipment.LedPanelBlock;
import org.overengineer.bettacraft.equipment.TankLidBlock;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankWater;

/**
 * M64: the aquarium light bar lies directly on a tank lid (one block: the lid with the bar on it), put onto the lid or a lid slid
 * under a bar on a tank; the tank stays covered and gets the light; breaking gives back both. With
 * {@code features.equipment.lightOnLid} off the bar goes into the block above the lid as before. An LED panel directly on a tank
 * covers it like a lid (no evaporation, closed for mobs and fliers).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LightOnLidGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private LightOnLidGameTests() {}

    private static void useOn(ServerPlayer sp, BlockPos pos, ItemStack stack) {
        sp.setItemInHand(InteractionHand.MAIN_HAND, stack);
        sp.gameMode.useItemOn(sp, sp.serverLevel(), stack, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.45, 0), Direction.UP, pos, false));
    }

    /** A 3 x 1 x 2 tank with lids on every top block; the bar goes onto two lids, a lid goes under a bar on a free top. */
    @GameTest(template = EMPTY, batch = "bettacraft_m64_light_on_lid", timeoutTicks = 100)
    public static void lightBarLiesOnTheLid(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 4, 1, 4, 3, 1, 2);
        for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) h.setBlock(new BlockPos(4 + x, 2, 4 + z), ModBlocks.TANK_LID.get().defaultBlockState());
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); // the lid is used up
        sp.setYRot(180); // facing north: the bar runs along x
        for (int x = 0; x < 2; x++) useOn(sp, h.absolutePos(new BlockPos(4 + x, 2, 4)), new ItemStack(ModItems.TANK_LIGHT.get(), 4));
        BlockState a = level.getBlockState(h.absolutePos(new BlockPos(4, 2, 4))), b = level.getBlockState(h.absolutePos(new BlockPos(5, 2, 4)));
        h.assertTrue(a.getBlock() instanceof TankLightBlock && TankLightBlock.onLid(a), "the bar lies on the first lid: " + a);
        h.assertTrue(b.getBlock() instanceof TankLightBlock && TankLightBlock.onLid(b), "and on the second: " + b);
        h.assertTrue(a.getValue(TankLightBlock.AXIS) == Direction.Axis.X && a.getValue(TankLightBlock.POS) && b.getValue(TankLightBlock.NEG), "one bar: " + a + " / " + b);
        h.assertTrue(level.getBlockState(h.absolutePos(new BlockPos(4, 3, 4))).isAir(), "nothing in the block above");
        // a lid slid under a bar standing on the free top block
        BlockPos free = h.absolutePos(new BlockPos(6, 2, 5));
        level.setBlock(free, ModBlocks.TANK_LIGHT.get().defaultBlockState(), Block.UPDATE_ALL);
        ItemStack lids = new ItemStack(ModItems.TANK_LID.get(), 2);
        useOn(sp, free, lids);
        h.assertTrue(TankLightBlock.onLid(level.getBlockState(free)) && lids.getCount() == 1, "a lid under the bar: " + level.getBlockState(free) + ", lids left " + lids.getCount());
        // the free top (6, 2, 4) is the only open one
        int[] top = TankWater.openTop(level, c.getTank());
        h.assertTrue(top[0] == 1 && top[1] == 6, "one of six tops open: " + top[0] + " of " + top[1]);
        // breaking gives back the bar and the lid
        List<ItemStack> drops = Block.getDrops(a, level, h.absolutePos(new BlockPos(4, 2, 4)), null);
        boolean light = drops.stream().anyMatch(s -> s.is(ModItems.TANK_LIGHT.get())), lid = drops.stream().anyMatch(s -> s.is(ModItems.TANK_LID.get()));
        h.assertTrue(light && lid, "drops the bar and the lid: " + drops);
        h.succeed();
    }

    /** {@code features.equipment.lightOnLid} off: the bar goes above the lid, no lid slides under a bar. */
    @GameTest(template = EMPTY, batch = "bettacraft_m64_light_on_lid_off", timeoutTicks = 100)
    public static void lightOnLidSwitchedOff(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        boolean was = Features.own(Feature.LIGHT_ON_LID);
        try {
            Features.set(Feature.LIGHT_ON_LID, false);
            AerationGameTests.tank(h, 4, 1, 4, 2, 1, 1);
            BlockPos lidPos = h.absolutePos(new BlockPos(4, 2, 4));
            level.setBlock(lidPos, ModBlocks.TANK_LID.get().defaultBlockState(), Block.UPDATE_ALL);
            ServerPlayer sp = MockPlayers.serverPlayer(h);
            useOn(sp, lidPos, new ItemStack(ModItems.TANK_LIGHT.get()));
            h.assertTrue(level.getBlockState(lidPos).getBlock() instanceof TankLidBlock, "the lid stays a lid");
            h.assertTrue(level.getBlockState(lidPos.above()).getBlock() instanceof TankLightBlock, "the bar above it as before");
            BlockPos bar = h.absolutePos(new BlockPos(5, 2, 4));
            level.setBlock(bar, ModBlocks.TANK_LIGHT.get().defaultBlockState(), Block.UPDATE_ALL);
            ItemStack lids = new ItemStack(ModItems.TANK_LID.get());
            useOn(sp, bar, lids);
            h.assertTrue(!TankLightBlock.onLid(level.getBlockState(bar)), "no lid slides under the bar");
        } finally {
            Features.set(Feature.LIGHT_ON_LID, was);
        }
        h.succeed();
    }

    /** An LED panel directly on a tank covers it like a lid: no open top, closed for mobs and fliers. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void ledPanelCoversLikeALid(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 4, 1, 4, 2, 1, 2);
        BlockState panel = ModBlocks.LED_PANEL.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X);
        for (int x = 0; x < 2; x++) {
            h.setBlock(new BlockPos(4 + x, 2, 4), panel.setValue(LedPanelBlock.PARTNER_POS, true));
            h.setBlock(new BlockPos(4 + x, 2, 5), panel.setValue(LedPanelBlock.PARTNER_POS, false));
        }
        int[] top = TankWater.openTop(level, c.getTank());
        h.assertTrue(top[0] == 0 && top[1] == 4, "no top open under the panel: " + top[0] + " of " + top[1]);
        h.assertTrue(!org.overengineer.bettacraft.mobs.GlassMobs.openTop(level, c.getTank()), "closed for mobs and fliers");
        h.assertTrue(TankWater.evaporationPerDay(level, c.getTank()) == 0, "no evaporation");
        h.succeed();
    }
}
