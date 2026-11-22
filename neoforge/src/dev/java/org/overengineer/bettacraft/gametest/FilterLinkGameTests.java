package org.overengineer.bettacraft.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.compat.info.InfoData;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.equipment.FilterFlow;
import org.overengineer.bettacraft.equipment.FilterLinks;
import org.overengineer.bettacraft.network.FilterLinkPayload;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankEquipment;

/**
 * M62 part 3: the external filter below the tank. One or two blocks lower it finds no tank by itself (unlinked, the overlay
 * says how to choose one); the choice lists the tanks in reach (also two tanks side by side) and connects it: it cleans that
 * tank, the hose starts down at the filter. A filter right at a tank connects as before, also one saved before M62 (no link
 * in its data). With {@code features.equipment.filterBelowTank} off only filters beside a tank work; the link is kept.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FilterLinkGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private FilterLinkGameTests() {}

    private static ExternalFilterBlockEntity filter(GameTestHelper h, BlockPos rel, Direction facing) {
        h.setBlock(rel, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, facing));
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof ExternalFilterBlockEntity f)) throw new org.overengineer.bettacraft.gametest.framework.GameTestFailure("no filter");
        return f;
    }

    private static boolean feeds(GameTestHelper h, TankBlockEntity c, ExternalFilterBlockEntity f) {
        TankEquipment.invalidate(c);
        return TankEquipment.filters(h.getLevel(), c).contains(f);
    }

    /** Two blocks below a tank on a cabinet: unlinked until chosen; then it cleans the tank and its hose starts down there. */
    @GameTest(template = EMPTY, batch = "bettacraft_m62_filter_link", timeoutTicks = 100)
    public static void filterBelowIsChosen(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        // tank x 4..5, z 4..5 at y 3, a cabinet and a block under it; the filter beside the cabinet at y 1 (two below)
        h.setBlock(new BlockPos(4, 2, 4), ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
        TankBlockEntity c = AerationGameTests.tank(h, 4, 3, 4, 2, 2, 2);
        ExternalFilterBlockEntity f = filter(h, new BlockPos(3, 1, 4), Direction.NORTH);
        h.assertTrue(FilterLinks.unlinked(level, f) && !feeds(h, c, f), "unlinked at first (no guessing)");
        CompoundTag info = InfoData.filter(level, f);
        h.assertTrue(info != null && !info.getBooleanOr("attached", false) && info.getBooleanOr("filter_block", false), "the overlay says it is not attached and how to choose");
        List<FilterLinkPayload.Option> options = FilterLinks.options(level, f.getBlockPos());
        h.assertTrue(options.size() == 1 && options.getFirst().below() == 2 && options.getFirst().wall().equals(h.absolutePos(new BlockPos(4, 3, 4))),
                "one tank in reach, two below: " + options);
        // chosen through the payload the client sends back (the server checks it again)
        ServerPlayer p = MockPlayers.serverPlayer(h);
        p.snapTo(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 1, 2))));
        FilterLinks.choose(p, new FilterLinkPayload(f.getBlockPos(), options, 0));
        h.assertTrue(h.getBlockState(new BlockPos(3, 1, 4)).getValue(ExternalFilterBlock.FACING) == Direction.EAST, "turned to the tank");
        h.assertTrue(feeds(h, c, f) && f.below() == 2, "it feeds the tank from two below");
        CompoundTag info2 = InfoData.filter(level, f);
        h.assertTrue(info2.getBooleanOr("attached", false) && info2.getIntOr("below", 0) == 2, "the overlay: attached, two below");
        FilterFlow.Path path = FilterFlow.forFilter(c.getTank(), f);
        h.assertTrue(path != null && path.bottom().y < c.getTank().min().getY() - 1.0, "the hose starts down at the filter: " + (path == null ? null : path.bottom()));
        // a forged choice of a block that is no option is ignored
        FilterLinks.choose(p, new FilterLinkPayload(f.getBlockPos(), List.of(new FilterLinkPayload.Option(h.absolutePos(new BlockPos(9, 9, 9)), BlockPos.ZERO, "", 0, 1, 1, 1)), 0));
        h.assertTrue(feeds(h, c, f), "a forged choice changes nothing");
        // the tank broken: unlinked again
        for (int x = 4; x <= 5; x++) for (int y = 3; y <= 4; y++) for (int z = 4; z <= 5; z++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        h.assertTrue(FilterLinks.unlinked(level, f), "its tank gone: unlinked");
        h.succeed();
    }

    /** Between two tanks the choice lists both; the filter feeds only the chosen one. */
    @GameTest(template = EMPTY, batch = "bettacraft_m62_filter_link", timeoutTicks = 100)
    public static void twoTanksSideBySide(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity west = AerationGameTests.tank(h, 2, 3, 4, 2, 1, 1);
        TankBlockEntity east = AerationGameTests.tank(h, 5, 3, 4, 2, 1, 1);
        ExternalFilterBlockEntity f = filter(h, new BlockPos(4, 2, 4), Direction.NORTH); // one below, between them
        List<FilterLinkPayload.Option> options = FilterLinks.options(level, f.getBlockPos());
        h.assertTrue(options.size() == 2, "both tanks: " + options);
        int ie = options.get(0).wall().getX() > f.getBlockPos().getX() ? 0 : 1;
        FilterLinks.link(level, f, options.get(ie).wall());
        h.assertTrue(feeds(h, east, f) && !feeds(h, west, f), "only the chosen (east) tank");
        FilterLinks.link(level, f, options.get(1 - ie).wall());
        h.assertTrue(feeds(h, west, f) && !feeds(h, east, f), "chosen again: the west one");
        h.succeed();
    }

    /** A filter right at a tank connects by itself as before, also one saved before M62 (its data has no link). */
    @GameTest(template = EMPTY, batch = "bettacraft_m62_filter_link", timeoutTicks = 100)
    public static void filterBesideStaysConnected(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 4, 2, 4, 2, 2, 2);
        ExternalFilterBlockEntity f = filter(h, new BlockPos(3, 2, 4), Direction.EAST);
        h.assertTrue(f.link() == null && feeds(h, c, f), "beside the tank: connected without a choice");
        // as an older world saved it: no link in the data; loaded again it still feeds the tank
        CompoundTag tag = f.saveWithFullMetadata(level.registryAccess());
        tag.remove("link");
        f.loadWithComponents(org.overengineer.bettacraft.util.NbtCompat.input(level.registryAccess(), tag));
        h.assertTrue(f.link() == null && feeds(h, c, f), "an old filter stays connected");
        // the choice also offers it (and keeps it)
        List<FilterLinkPayload.Option> options = FilterLinks.options(level, f.getBlockPos());
        h.assertTrue(options.size() == 1 && options.getFirst().below() == 0, "beside: one option " + options);
        h.succeed();
    }

    /** {@code features.equipment.filterBelowTank} off: a chosen filter below does not work, the choice lists only tanks beside it. */
    @GameTest(template = EMPTY, batch = "bettacraft_m62_filter_link_off", timeoutTicks = 100)
    public static void filterBelowSwitchedOff(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        boolean was = Features.own(Feature.FILTER_BELOW_TANK);
        try {
            TankBlockEntity c = AerationGameTests.tank(h, 4, 3, 4, 2, 1, 2);
            ExternalFilterBlockEntity f = filter(h, new BlockPos(3, 2, 4), Direction.EAST);
            FilterLinks.link(level, f, h.absolutePos(new BlockPos(4, 3, 4)));
            h.assertTrue(feeds(h, c, f), "on: it feeds from below");
            Features.set(Feature.FILTER_BELOW_TANK, false);
            h.assertTrue(!feeds(h, c, f) && FilterLinks.unlinked(level, f), "off: not connected");
            h.assertTrue(FilterLinks.options(level, f.getBlockPos()).isEmpty(), "off: no tank in reach below");
            h.assertTrue(f.link() != null, "the choice is kept");
            Features.set(Feature.FILTER_BELOW_TANK, true);
            h.assertTrue(feeds(h, c, f), "on again: connected again");
        } finally {
            Features.set(Feature.FILTER_BELOW_TANK, was);
        }
        h.succeed();
    }
}
