package org.overengineer.bettacraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.monitor.DashboardLayout;
import org.overengineer.bettacraft.monitor.DashboardTile;
import org.overengineer.bettacraft.monitor.MonitorBlock;
import org.overengineer.bettacraft.monitor.MonitorBlockEntity;
import org.overengineer.bettacraft.monitor.MonitorSync;
import org.overengineer.bettacraft.monitor.MonitorTank;
import org.overengineer.bettacraft.monitor.MonitorValue;
import org.overengineer.bettacraft.monitor.TankHistory;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M27 part a GameTests: monitor blocks join into screens (size, frame flags, the layout goes on when a screen splits),
 * the screen finds the tanks in range with the name of their sign and forgets broken ones, the history records a point an
 * hour, FE with a power option, the feature switch. Deterministic: the screens' work ({@link MonitorSync}) is called
 * directly; the GameTest server has no viewers, so nothing is sent.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MonitorGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private MonitorGameTests() {}

    /** A wall of monitors facing north, {@code w} x {@code h}, its top left block (as the viewer sees it) at {@code topLeft}. */
    private static void wall(GameTestHelper h, BlockPos topLeft, int w, int hgt) {
        BlockState s = ModBlocks.MONITOR.get().defaultBlockState().setValue(MonitorBlock.FACING, Direction.NORTH);
        for (int y = 0; y < hgt; y++) for (int x = 0; x < w; x++) h.setBlock(topLeft.west(x).below(y), s); // north-facing: right = west
    }

    private static MonitorBlockEntity be(GameTestHelper h, BlockPos rel) {
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof MonitorBlockEntity be)) throw new GameTestAssertException("no monitor at " + rel);
        return be;
    }

    @GameTest(template = EMPTY, batch = "bettacraft_m27", timeoutTicks = 40)
    public static void monitorsJoinIntoScreens(GameTestHelper h) {
        BlockPos tl = new BlockPos(14, 3, 6);
        wall(h, tl, 3, 2);
        MonitorBlockEntity c = be(h, tl);
        h.assertTrue(c.isController() && c.width() == 3 && c.height() == 2, "one 3x2 screen: " + c.width() + "x" + c.height());
        for (int y = 0; y < 2; y++) for (int x = 0; x < 3; x++) {
            MonitorBlockEntity m = be(h, tl.west(x).below(y));
            h.assertTrue(m.origin().equals(c.getBlockPos()), "every block knows the top left block");
        }
        BlockState mid = h.getBlockState(tl.west(1));
        h.assertTrue(mid.getValue(MonitorBlock.LEFT) && mid.getValue(MonitorBlock.RIGHT) && !mid.getValue(MonitorBlock.UP) && mid.getValue(MonitorBlock.DOWN),
                "frame flags of the top middle block: " + mid);
        // a hand-made layout survives the split of the screen
        DashboardLayout l = new DashboardLayout();
        l.auto = false;
        l.tiles.add(DashboardTile.value(l.nextId++, 0, 4, 4, 4, null, MonitorValue.PH, false));
        c.setLayout(l);
        h.setBlock(tl.west(1), Blocks.AIR.defaultBlockState());
        MonitorBlockEntity left = be(h, tl);
        MonitorBlockEntity right = be(h, tl.west(2));
        h.assertTrue(left.controller() != null && right.controller() != null, "both parts have a controller");
        h.assertTrue(left.controller() != right.controller(), "two screens after the gap");
        MonitorBlockEntity lc = left.controller();
        h.assertTrue(lc.width() == 1 && lc.height() == 2 && lc.getBlockPos().equals(h.absolutePos(tl)), "left screen 1x2: " + lc.width() + "x" + lc.height());
        int kept = 0;
        for (MonitorBlockEntity sc : new MonitorBlockEntity[]{lc, right.controller()}) if (!sc.layout().auto && sc.layout().tiles.size() == 1) kept++;
        h.assertTrue(kept == 1, "the layout went on in one of the screens: " + kept);
        // the gap filled again: one screen, the layout is still there
        h.setBlock(tl.west(1), ModBlocks.MONITOR.get().defaultBlockState().setValue(MonitorBlock.FACING, Direction.NORTH));
        MonitorBlockEntity again = be(h, tl);
        h.assertTrue(again.width() == 3 && again.height() == 2, "joined again: " + again.width() + "x" + again.height());
        h.assertTrue(!again.layout().auto && again.layout().tiles.size() == 1, "the layout is back on the big screen");
        // a monitor with another facing does not join
        h.setBlock(tl.east(), ModBlocks.MONITOR.get().defaultBlockState().setValue(MonitorBlock.FACING, Direction.SOUTH));
        h.assertTrue(be(h, tl).width() == 3, "other facing stays apart");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "bettacraft_m27", timeoutTicks = 40)
    public static void bigWallsAreCutAt8x5(GameTestHelper h) {
        BlockPos tl = new BlockPos(20, 6, 6);
        wall(h, tl, 9, 6);
        MonitorBlockEntity c = be(h, tl);
        h.assertTrue(c.width() == 8 && c.height() == 5, "8x5: " + c.width() + "x" + c.height());
        MonitorBlockEntity last = be(h, tl.west(8));
        h.assertTrue(last.controller() != c, "the 9th column is a screen of its own");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "bettacraft_m27", timeoutTicks = 60)
    public static void monitorFindsTheTanksInRange(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity t = AerationGameTests.tank(h, 2, 1, 2, 3, 2, 2);
        t.getTank().setName("Betta Nano");
        t.getTank().setWaterTemp(26.5F);
        BlockPos tl = new BlockPos(10, 2, 8);
        wall(h, tl, 2, 1);
        MonitorBlockEntity c = be(h, tl);
        MonitorSync.discover(level, c);
        MonitorTank k = null;
        for (MonitorTank m : c.tanks()) if (m.id.equals(t.getTank().id())) k = m;
        h.assertTrue(k != null, "found the tank: " + c.tanks().size());
        h.assertTrue(k.live && "Betta Nano".equals(k.name), "live, named after the sign: " + k.name);
        h.assertTrue(Math.abs(k.value(MonitorValue.TEMPERATURE) - 26.5F) < 0.01F, "temperature " + k.value(MonitorValue.TEMPERATURE));
        h.assertTrue(Math.abs(k.value(MonitorValue.FILL) - 100F) < 0.5F, "full: " + k.value(MonitorValue.FILL));
        t.getTank().setWaterTemp(24F);
        MonitorSync.refresh(level, c);
        h.assertTrue(Math.abs(k.value(MonitorValue.TEMPERATURE) - 24F) < 0.01F, "refreshed: " + k.value(MonitorValue.TEMPERATURE));
        // the automatic layout: two tiles for the tank
        MonitorSync.work(level, c);
        h.assertTrue(c.layout().auto && c.layout().tiles.size() == 2 && t.getTank().id().equals(c.layout().tiles.get(0).tank),
                "auto layout: " + c.layout().tiles.size());
        // the tank is broken: not live, then forgotten
        for (int x = 2; x < 5; x++) for (int y = 1; y < 3; y++) for (int z = 2; z < 4; z++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        MonitorSync.refresh(level, c);
        h.assertTrue(!k.live, "gone: not live");
        MonitorSync.discover(level, c);
        h.assertTrue(c.tanks().isEmpty(), "forgotten: " + c.tanks().size());
        h.succeed();
    }

    /** Range 4: a tank 8 blocks away is not found; config restored even on failure. */
    @GameTest(template = EMPTY, batch = "bettacraft_m27_range", timeoutTicks = 60)
    public static void tanksOutOfRangeAreLeftOut(GameTestHelper h) {
        int range = ServerConfig.MONITOR_RANGE.get();
        try {
            ServerConfig.MONITOR_RANGE.set(4);
            TankBlockEntity near = AerationGameTests.tank(h, 2, 1, 2, 2, 2, 2);
            TankBlockEntity far = AerationGameTests.tank(h, 20, 1, 2, 2, 2, 2);
            BlockPos tl = new BlockPos(6, 2, 2);
            wall(h, tl, 1, 1);
            MonitorBlockEntity c = be(h, tl);
            MonitorSync.discover(h.getLevel(), c);
            h.assertTrue(c.tanks().size() == 1 && c.tanks().get(0).id.equals(near.getTank().id()), "only the near tank: " + c.tanks().size());
            h.assertTrue(!far.getTank().id().equals(near.getTank().id()), "two tanks");
        } finally {
            ServerConfig.MONITOR_RANGE.set(range);
        }
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "bettacraft_m27", timeoutTicks = 40)
    public static void tanksRecordAPointAnHour(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity t = AerationGameTests.tank(h, 2, 1, 2, 2, 2, 2);
        t.getTank().setWaterTemp(27F);
        TankHistory.onCare(level, t);
        TankHistory.onCare(level, t);
        TankHistory.Series s = TankHistory.get(level).series(t.getTank().id());
        h.assertTrue(s != null && s.size() == 1, "one point this hour: " + (s == null ? -1 : s.size()));
        h.assertTrue(s.lastHour() == TankHistory.hour(level.getGameTime()), "this hour");
        float[] v = s.values(MonitorValue.TEMPERATURE, 24);
        h.assertTrue(v.length == 1 && Math.abs(v[0] - 27F) < 0.01F, "27 °C recorded");
        h.succeed();
    }

    /** With a power option on the screen needs FE; any block takes it into the controller's buffer. */
    @GameTest(template = EMPTY, batch = "bettacraft_m27_fe", timeoutTicks = 40)
    public static void monitorsNeedFeWithPower(GameTestHelper h) {
        boolean pumps = ServerConfig.POWER_PUMPS.get();
        try {
            ServerConfig.POWER_PUMPS.set(true);
            BlockPos tl = new BlockPos(10, 2, 8);
            wall(h, tl, 2, 1);
            MonitorBlockEntity c = be(h, tl);
            MonitorSync.cycle(h.getLevel(), c);
            h.assertTrue(c.status() == MonitorBlockEntity.NO_POWER, "no power: " + c.status());
            IEnergyStorage cap = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(tl.west()), null);
            h.assertTrue(cap != null, "energy capability on the second block");
            int need = MonitorSync.fePerBlock() * 2 * MonitorSync.INTERVAL;
            h.assertTrue(cap.receiveEnergy(need * 3, false) == need * 3, "took FE");
            h.assertTrue(c.energy().getEnergyStored() == need * 3, "into the controller: " + c.energy().getEnergyStored());
            MonitorSync.cycle(h.getLevel(), c);
            h.assertTrue(c.status() == MonitorBlockEntity.ON, "on: " + c.status());
            h.assertTrue(c.energy().getEnergyStored() == need * 2, "used 1 FE/t per block: " + c.energy().getEnergyStored());
        } finally {
            ServerConfig.POWER_PUMPS.set(pumps);
        }
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "bettacraft_m27_off", timeoutTicks = 40)
    public static void monitorsSwitchOff(GameTestHelper h) {
        boolean on = CommonConfig.FEATURE_MONITORS.get();
        try {
            CommonConfig.FEATURE_MONITORS.set(false);
            BlockPos tl = new BlockPos(10, 2, 8);
            wall(h, tl, 1, 1);
            MonitorBlockEntity c = be(h, tl);
            MonitorSync.cycle(h.getLevel(), c);
            h.assertTrue(c.status() == MonitorBlockEntity.OFF, "off: " + c.status());
            TankBlockEntity t = AerationGameTests.tank(h, 2, 1, 2, 2, 2, 2);
            TankHistory.onCare(h.getLevel(), t);
            h.assertTrue(TankHistory.get(h.getLevel()).series(t.getTank().id()) == null, "records nothing");
        } finally {
            CommonConfig.FEATURE_MONITORS.set(on);
        }
        h.succeed();
    }
}
