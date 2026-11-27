package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaCare;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.filter.FilterChamberBlockEntity;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity;
import org.overengineer.bettacraft.filter.MultiblockFilter;
import org.overengineer.bettacraft.food.FoodMakerBlockEntity;
import org.overengineer.bettacraft.osmosis.RoUnitBlockEntity;
import org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity;
import org.overengineer.bettacraft.plumbing.WaterPumpBlockEntity;
import org.overengineer.bettacraft.plumbing.PipeNetwork;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankPower;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.WaterValues;
import org.overengineer.bettacraft.winding.Windable;
import org.overengineer.bettacraft.winding.Winding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M37 GameTests: the crank. With power.costs OFF only the food maker and the fertilizer maker take it; with EASY or HARD
 * also the multiblock filter, the reverse osmosis unit, the water change controller and the water pump (own springs), and
 * (M67) the cabinet under a tank and an attached external filter, which charge the tank's one store. Wound springs stand in
 * for the FE that is short (FE first), run down by the devices they drive and stand still for unloaded time. Keeping fish
 * without a generator over several in-game weeks. Every test changes the power config and has its own batch; the config
 * is restored.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrankGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/gametest");

    private CrankGameTests() {}

    // ------------------------------------------------------------------ helpers

    /** The power settings, restored after a test. */
    record Power(ServerConfig.PowerCosts costs, boolean power, boolean winding, boolean tankCrank) {
        static Power current() {
            return new Power(ServerConfig.POWER_COSTS.get(), Features.own(Feature.POWER), Features.own(Feature.WINDING), Features.own(Feature.TANK_CRANK));
        }

        void apply() {
            ServerConfig.POWER_COSTS.set(costs);
            Features.set(Feature.POWER, power);
            Features.set(Feature.WINDING, winding);
            Features.set(Feature.TANK_CRANK, tankCrank);
        }

        /** M67: power.costs HARD (on) or OFF, winding as given. */
        static void all(boolean on, boolean winding) {
            new Power(on ? ServerConfig.PowerCosts.HARD : ServerConfig.PowerCosts.OFF, true, winding, true).apply();
        }
    }

    private static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz) {
        TankBlockEntity c = AerationGameTests.tank(h, x, y, z, sx, sy, sz);
        c.getTank().setPowerBuffer(0);
        return c;
    }

    private static ExternalFilterBlockEntity filter(GameTestHelper h, BlockPos rel, Direction towardsTank) {
        h.setBlock(rel, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, towardsTank));
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof ExternalFilterBlockEntity f)) throw new GameTestFailure("no filter");
        return f;
    }

    private static CabinetBlockEntity cabinet(GameTestHelper h, BlockPos rel, boolean withFilter) {
        h.setBlock(rel, ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
        CabinetBlockEntity cab = (CabinetBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel));
        if (cab == null) throw new GameTestFailure("no cabinet");
        if (withFilter) cab.items().setStackInSlot(CabinetBlockEntity.FILTER_SLOT, new ItemStack(ModItems.EXTERNAL_FILTER.get()));
        return cab;
    }

    private static <T extends BlockEntity> T be(GameTestHelper h, BlockPos rel, Class<T> type) {
        BlockEntity b = h.getLevel().getBlockEntity(h.absolutePos(rel));
        if (!type.isInstance(b)) throw new GameTestFailure("no " + type.getSimpleName() + " at " + rel);
        return type.cast(b);
    }

    private static Winding.Result crank(GameTestHelper h, BlockEntity b) {
        return Winding.wind(h.getLevel(), b.getBlockPos(), null);
    }

    private static Winding.Result crank(GameTestHelper h, BlockPos rel) {
        return Winding.wind(h.getLevel(), h.absolutePos(rel), null);
    }

    /** Every windable device of the tests, placed apart: food maker, fertilizer maker, filter, cabinet, pump, osmosis, changer, multiblock filter. */
    private record Devices(FoodMakerBlockEntity food, org.overengineer.bettacraft.fertilizer.FertilizerMakerBlockEntity fert, ExternalFilterBlockEntity filter,
                           CabinetBlockEntity cabinet, WaterPumpBlockEntity pump, RoUnitBlockEntity ro, WaterChangerBlockEntity changer,
                           FilterPumpHeadBlockEntity head, BlockPos chamber) {}

    private static Devices devices(GameTestHelper h) {
        FoodMakerBlockEntity food = FoodMakerGameTests.maker(h, new BlockPos(1, 1, 1));
        var fert = FertilizerGameTests.maker(h, new BlockPos(3, 1, 1));
        TankBlockEntity c = tank(h, 6, 2, 6, 2, 1, 2);
        ExternalFilterBlockEntity f = filter(h, new BlockPos(8, 2, 6), Direction.WEST);
        CabinetBlockEntity cab = cabinet(h, new BlockPos(6, 1, 6), true);
        TankEquipment.invalidate(c);
        WaterPumpBlockEntity pump = PlumbingGameTests.pump(h, new BlockPos(1, 1, 5), Direction.EAST);
        h.setBlock(new BlockPos(1, 1, 8), ModBlocks.RO_UNIT.get().defaultBlockState());
        RoUnitBlockEntity ro = be(h, new BlockPos(1, 1, 8), RoUnitBlockEntity.class);
        h.setBlock(new BlockPos(1, 1, 11), ModBlocks.WATER_CHANGE_CONTROLLER.get().defaultBlockState());
        WaterChangerBlockEntity changer = be(h, new BlockPos(1, 1, 11), WaterChangerBlockEntity.class);
        h.setBlock(new BlockPos(12, 1, 1), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
        h.setBlock(new BlockPos(12, 2, 1), ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState());
        FilterPumpHeadBlockEntity head = be(h, new BlockPos(12, 2, 1), FilterPumpHeadBlockEntity.class);
        return new Devices(food, fert, f, cab, pump, ro, changer, head, new BlockPos(12, 1, 1));
    }

    // ------------------------------------------------------------------ which devices take the crank

    /** Power options off: only the food maker and the fertilizer maker take the crank; the rest runs without it. */
    @GameTest(template = EMPTY, batch = "bettacraft_m37_crank_off", timeoutTicks = 100)
    public static void withoutPowerOnlyTheMachinesTakeTheCrank(GameTestHelper h) {
        Power saved = Power.current();
        try {
            Power.all(false, true);
            Devices d = devices(h);
            h.assertTrue(crank(h, d.food()) == Winding.Result.WOUND && crank(h, d.fert()) == Winding.Result.WOUND, "the two machines wind");
            for (BlockEntity b : List.<BlockEntity>of(d.filter(), d.cabinet(), d.pump(), d.ro(), d.changer(), d.head())) {
                h.assertTrue(crank(h, b) == Winding.Result.REFUSED, "runs without power, no crank: " + b.getType());
                h.assertTrue(((Windable) b).windDevice().refusal().equals("message.bettacraft.crank.no_need"), "says why: " + b.getType());
            }
            // devices without a spring at all
            h.setBlock(new BlockPos(20, 1, 20), ModBlocks.FEEDER.get().defaultBlockState());
            h.setBlock(new BlockPos(22, 1, 20), ModBlocks.TANK_LIGHT.get().defaultBlockState());
            h.setBlock(new BlockPos(24, 1, 20), ModBlocks.DOSING_PUMP.get().defaultBlockState());
            for (BlockPos p : List.of(new BlockPos(20, 1, 20), new BlockPos(22, 1, 20), new BlockPos(24, 1, 20), new BlockPos(6, 2, 6))) {
                h.assertTrue(crank(h, p) == Winding.Result.NOTHING, "no spring: " + h.getLevel().getBlockState(h.absolutePos(p)).getBlock());
            }
        } finally {
            saved.apply();
        }
        h.succeed();
    }

    /**
     * With power costs every device with a spring takes the crank; a charge is 25 clicks, the multiblock filter holds six
     * (150 clicks); a part of the multiblock filter winds its pump head. M67: the cabinet and the attached external filter
     * charge the tank's store, power.crankFePerClick a click, until it is full.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m37_crank_on", timeoutTicks = 100)
    public static void withPowerEveryPoweredDeviceTakesTheCrank(GameTestHelper h) {
        Power saved = Power.current();
        try {
            Power.all(true, true);
            Devices d = devices(h);
            int perCharge = Winding.clicksPerCharge(), day = Winding.ticksPerCharge();
            for (BlockEntity b : List.<BlockEntity>of(d.food(), d.fert(), d.filter(), d.cabinet(), d.pump(), d.ro(), d.changer(), d.head())) {
                h.assertTrue(crank(h, b) == Winding.Result.WOUND, "winds: " + b.getType());
            }
            h.assertTrue(d.ro().windSpring().capacity() == day && d.pump().windSpring().capacity() == day
                    && d.changer().windSpring().capacity() == day, "one charge each");
            h.assertTrue(d.head().windSpring().capacity() == 6L * day && Winding.Device.MULTIBLOCK_FILTER.clicksToFull() == 6 * perCharge,
                    "multiblock filter: six charges, six times as long to wind");
            h.assertTrue(d.filter().windSpring().empty() && d.cabinet().windSpring().empty(), "M67: the cabinet and the filter wind no spring of their own");
            // a click on a chamber winds the pump head
            long before = d.head().windSpring().ticks();
            h.assertTrue(crank(h, d.chamber()) == Winding.Result.WOUND && d.head().windSpring().ticks() > before, "a chamber winds its pump head");
            // M45: the crank of a player gives the advancement
            var sp = MockPlayers.serverPlayer(h);
            h.assertTrue(Winding.wind(h.getLevel(), d.pump().getBlockPos(), sp) == Winding.Result.WOUND, "the player winds the pump");
            QuestAdv.check(h, sp, "wound_up");
            QuestAdv.gone(h, sp);
            // M67: the tank's store to full, at the cabinet
            TankBlockEntity tank = org.overengineer.bettacraft.tank.TankStore.tankOf(h.getLevel(), d.cabinet());
            h.assertTrue(tank != null && tank == org.overengineer.bettacraft.tank.TankStore.tankOf(h.getLevel(), d.filter()), "cabinet and filter charge the same tank");
            tank.getTank().setPowerBuffer(0);
            long cap = TankPower.capacity(tank), per = org.overengineer.bettacraft.tank.TankStore.fePerClick();
            int clicks = 0;
            while (crank(h, d.cabinet()) == Winding.Result.WOUND) clicks++;
            h.assertTrue(clicks == (int) ((cap + per - 1) / per) && tank.getTank().powerBuffer() == cap, "store full after " + clicks + " clicks: "
                    + tank.getTank().powerBuffer() + " of " + cap);
            h.assertTrue(crank(h, d.cabinet()) == Winding.Result.FULL && crank(h, d.filter()) == Winding.Result.FULL, "full: no more");
            // power.winding off: only the two machines, and only without FE options ... with them FE only
            Features.set(Feature.WINDING, false);
            for (BlockEntity b : List.<BlockEntity>of(d.food(), d.fert(), d.filter(), d.cabinet(), d.pump(), d.ro(), d.changer(), d.head())) {
                h.assertTrue(crank(h, b) == Winding.Result.REFUSED, "FE only with power.winding off: " + b.getType());
            }
        } finally {
            saved.apply();
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ springs stand in for FE

    /**
     * M67: the crank at an external filter attached to a tank charges the tank's store; with it the filter cleans and its heater
     * heats, the devices take their FE from the store. Unwound and without FE: no cleaning, no heating. An external filter
     * without a tank refuses the crank.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m37_filter", timeoutTicks = 100)
    public static void crankAtTheExternalFilterChargesTheStore(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Power saved = Power.current();
        try {
            Power.all(true, true);
            TankBlockEntity c = tank(h, 2, 1, 2, 2, 2, 2);
            ExternalFilterBlockEntity f = filter(h, new BlockPos(4, 1, 2), Direction.WEST);
            f.insertHeater(new ItemStack(ModItems.HEATER_SMALL.get()));
            TankEquipment.invalidate(c);
            c.getTank().setTargetTemp(34F); // it must heat
            TankPower.step(level, c, 20);
            h.assertTrue(!c.filterPowered() && TankWater.filterBlocks(level, c) == 0 && TankCare.heaterBlocks(level, c.getTank()) == 0, "no FE: nothing");
            for (int i = 0; i < 10; i++) h.assertTrue(crank(h, f) == Winding.Result.WOUND, "the crank charges the store");
            long fe = c.getTank().powerBuffer();
            h.assertTrue(fe == 10 * org.overengineer.bettacraft.tank.TankStore.fePerClick(), "10 clicks: " + fe);
            TankPower.step(level, c, 20);
            h.assertTrue(c.filterPowered() && c.heatPowered(), "the filter and its heater run on the store");
            double ext = ServerConfig.getOr(ServerConfig.WATER_QUALITY_EXTERNAL_FILTER_BLOCKS, 32);
            h.assertTrue(TankWater.filterBlocks(level, c) == ext && TankCare.heaterBlocks(level, c.getTank()) > 0, "cleans " + TankWater.filterBlocks(level, c)
                    + ", heats " + TankCare.heaterBlocks(level, c.getTank()));
            h.assertTrue(fe - c.getTank().powerBuffer() == 20L * c.lastDemand() && c.lastDemand() > 0, "the demand comes from the store: "
                    + (fe - c.getTank().powerBuffer()) + " for " + c.lastDemand() + " FE/t");
            // a filter that hangs on no tank: nothing to charge
            ExternalFilterBlockEntity lone = filter(h, new BlockPos(12, 1, 12), Direction.WEST);
            h.assertTrue(crank(h, lone) == Winding.Result.REFUSED, "no tank: refused");
        } finally {
            saved.apply();
        }
        h.succeed();
    }

    /**
     * M67: the crank at a cabinet charges the store of its tank, which runs every device of the tank (the cabinet's filter, a
     * filter block, a light, the heater). With features.power.tankCrank off the crank is refused and only FE charges it.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m37_cabinet", timeoutTicks = 100)
    public static void crankAtTheCabinetPowersTheWholeTank(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Power saved = Power.current();
        try {
            Power.all(true, true);
            TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 2);
            CabinetBlockEntity cab = cabinet(h, new BlockPos(2, 1, 2), true);
            ExternalFilterBlockEntity f = filter(h, new BlockPos(4, 2, 2), Direction.WEST);
            f.insertHeater(new ItemStack(ModItems.HEATER_SMALL.get()));
            h.setBlock(new BlockPos(2, 4, 2), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true));
            TankEquipment.invalidate(c);
            c.getTank().setTargetTemp(34F);
            for (int i = 0; i < 20; i++) crank(h, cab);
            h.assertTrue(c.getTank().powerBuffer() == 20 * org.overengineer.bettacraft.tank.TankStore.fePerClick(), "20 clicks in the store");
            TankPower.step(level, c, 20);
            h.assertTrue(c.filterPowered() && c.heatPowered() && c.lightPowered(), "every group runs on the store the crank filled");
            // the capacity: 8 water blocks and one cabinet
            long cap = TankPower.capacity(c);
            h.assertTrue(cap == 8 * org.overengineer.bettacraft.tank.TankStore.perWaterBlock() + org.overengineer.bettacraft.tank.TankStore.perCabinet(),
                    "store of 8 water blocks + 1 cabinet: " + cap);
            // features.power.tankCrank off: refused, the store stays
            Features.set(Feature.TANK_CRANK, false);
            long s = c.getTank().powerBuffer();
            h.assertTrue(crank(h, cab) == Winding.Result.REFUSED && crank(h, f) == Winding.Result.REFUSED && c.getTank().powerBuffer() == s,
                    "tankCrank off: FE only");
            h.assertTrue(Winding.Device.CABINET.refusal().equals("message.bettacraft.crank.fe_only"), "says why");
        } finally {
            saved.apply();
        }
        h.succeed();
    }

    /**
     * No FE: the water pump pumps on its spring (a window of work per batch), the reverse osmosis unit boosts on it, the
     * water change controller changes on it, the multiblock filter pumps on it; unwound they stand still (osmosis: slow).
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m37_water", timeoutTicks = 200)
    public static void woundWaterDevicesRun(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Power saved = Power.current();
        try {
            Power.all(true, true);
            // water pump between two reservoirs
            PlumbingGameTests.reservoir(h, new BlockPos(1, 1, 1), 4000, null);
            WaterPumpBlockEntity pump = PlumbingGameTests.pump(h, new BlockPos(2, 1, 1), Direction.EAST);
            PlumbingGameTests.reservoir(h, new BlockPos(3, 1, 1), 0, null);
            h.assertTrue(PlumbingGameTests.batch(h, pump) == 0, "no FE, unwound: still");
            pump.windSpring().set(pump.windSpring().capacity());
            int moved = PlumbingGameTests.batch(h, pump);
            h.assertTrue(moved > 0 && pump.windSpring().capacity() - pump.windSpring().ticks() == PipeNetwork.WINDOW, "pumps on the spring: " + moved
                    + ", spring -" + (pump.windSpring().capacity() - pump.windSpring().ticks()));
            // reverse osmosis: slow without, boosted on the spring
            h.setBlock(new BlockPos(5, 1, 5), Blocks.WATER.defaultBlockState());
            h.setBlock(new BlockPos(6, 1, 5), ModBlocks.RO_UNIT.get().defaultBlockState());
            RoUnitBlockEntity ro = be(h, new BlockPos(6, 1, 5), RoUnitBlockEntity.class);
            ro.parts().setStackInSlot(RoUnitBlockEntity.MEMBRANE, new ItemStack(ModItems.RO_MEMBRANE.get()));
            ro.parts().setStackInSlot(RoUnitBlockEntity.PREFILTER, new ItemStack(ModItems.RO_PREFILTER.get()));
            int slow = ro.step(level, 20);
            ro.windSpring().set(ro.windSpring().capacity());
            int fast = ro.step(level, 20);
            h.assertTrue(slow == 20 && fast == 80 && ro.boosted() && ro.windSpring().capacity() - ro.windSpring().ticks() == 20, "osmosis " + slow + " -> " + fast);
            // the water change controller
            TankBlockEntity c = tank(h, 1, 2, 10, 2, 2, 2);
            h.setBlock(new BlockPos(3, 2, 10), ModBlocks.WATER_CHANGE_CONTROLLER.get().defaultBlockState());
            WaterChangerBlockEntity wc = be(h, new BlockPos(3, 2, 10), WaterChangerBlockEntity.class);
            wc.set(7, 25, true);
            // M68 part B: fresh water through a pipe at its inlet (east), from a reservoir
            h.setBlock(new BlockPos(4, 2, 10), ModBlocks.STAINLESS_STEEL_PIPE.get().defaultBlockState());
            PlumbingGameTests.reservoir(h, new BlockPos(5, 2, 10), 4000, null);
            h.assertTrue(wc.check(level, true) == 0 && wc.status() == WaterChangerBlockEntity.Status.NO_POWER, "no FE, unwound: " + wc.status());
            wc.windSpring().set(wc.windSpring().capacity());
            int changed = wc.check(level, true);
            h.assertTrue(changed == 2000 && wc.windSpring().ticks() < wc.windSpring().capacity(), "changed on the spring: " + changed);
            // the multiblock filter
            MultiblockFilter.forget();
            TankBlockEntity t = tank(h, 10, 2, 10, 2, 2, 2);
            h.setBlock(new BlockPos(13, 2, 10), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
            h.setBlock(new BlockPos(13, 3, 10), ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState());
            h.setBlock(new BlockPos(12, 2, 10), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.WEST));
            h.setBlock(new BlockPos(12, 3, 10), ModBlocks.FILTER_OUTLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.WEST));
            be(h, new BlockPos(13, 2, 10), FilterChamberBlockEntity.class).setMedium(new ItemStack(ModItems.CERAMIC_RINGS.get()));
            FilterPumpHeadBlockEntity head = be(h, new BlockPos(13, 3, 10), FilterPumpHeadBlockEntity.class);
            FilterLinesGameTests.guardAll(level, head.getBlockPos()); // M40
            h.assertTrue(head.run(level, true) == 0, "no FE, unwound: the pump head stands still");
            crank(h, new BlockPos(13, 2, 10)); // a click on the chamber
            head.windSpring().set(head.windSpring().capacity());
            h.assertTrue(head.run(level, true) > 0 && t.getBlockPos().equals(head.servedTank()), "pumps and cleans on its spring");
        } finally {
            saved.apply();
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ keeping fish without a generator

    /** One in-game day of a kept tank: fed bettas, two care steps, the fish's own care. */
    private static void day(ServerLevel level, TankBlockEntity c, List<BettaEntity> fish) {
        for (BettaEntity b : fish) if (b.isAlive()) b.health().feed();
        for (int i = 0; i < 2; i++) {
            TankCare.care(level, c, 12_000);
            for (BettaEntity b : fish) if (b.isAlive()) BettaCare.advance(level, c, b, 0.5);
        }
    }

    private static List<BettaEntity> stock(GameTestHelper h, TankBlockEntity c, long seed) {
        List<BettaEntity> out = new ArrayList<>();
        out.add(LiveFoodGameTests.betta(h, c, false, seed));
        out.add(LiveFoodGameTests.betta(h, c, true, seed + 1));
        return out;
    }

    /**
     * M37 question 3: with power costs (M67: EASY) and no generator (no FE at all), can fish be kept for weeks? Tank A only
     * gets a weekly water change (and no winding: the state before M37), tank B the same with an external filter and a
     * heater whose tank store the crank at the filter fills every day, tank P is densely planted with floating plants (no water change, no filter).
     * 28 days. All keep their fish healthy; the wound filter keeps the water cleanest and its heater holds a target above
     * the room (without power a heater cannot: the temperature stays at the room's).
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m37_weeks", timeoutTicks = 600)
    public static void keepingFishWithoutAGenerator(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Power saved = Power.current();
        long savedTime = level.getDayTime();
        try {
            Power.all(true, true);
            ServerConfig.POWER_COSTS.set(ServerConfig.PowerCosts.EASY); // M67: the store is sized for a day on EASY
            // the room temperature follows the time of day the batch happens to start at (at night 23 °C chilled tank A's bettas
            // to death on Fabric 1.21.11): the test runs at noon
            level.setDayTime(6000);
            TankBlockEntity a = tank(h, 2, 1, 2, 3, 2, 2), b = tank(h, 2, 1, 10, 3, 2, 2), p = tank(h, 2, 1, 18, 3, 2, 2);
            ExternalFilterBlockEntity f = filter(h, new BlockPos(5, 1, 10), Direction.WEST);
            f.insertHeater(new ItemStack(ModItems.HEATER_MEDIUM.get()));
            TankEquipment.invalidate(b);
            // M52: P holds two females: a pair bred in the 28 days, and its fry (2 to 23 bettas in the tank, by the shared random
            // of the suite) set the nitrate and the quality, 0.40-0.69 at the end (0.396 failed the check once on 26.3)
            List<BettaEntity> fa = stock(h, a, 3701), fb = stock(h, b, 3711),
                    fp = List.of(LiveFoodGameTests.betta(h, p, true, 3721), LiveFoodGameTests.betta(h, p, true, 3722));
            float ambient = TankCare.ambient(level, a.getTank());
            // B wants it warmer than the room: only the heater can do that
            b.getTank().setTargetTemp(Math.min(29F, ambient + 2F));
            // P: densely planted with floating plants, no water changes, no filter
            for (int i = 0; i < 12; i++) {
                org.overengineer.bettacraft.tank.plants.TankPlants.add(p, org.overengineer.bettacraft.tank.plants.FloatingPlantType.SALVINIA,
                        p.getTank().min().getX() + 0.3 + (i % 4) * 0.7, p.getTank().min().getZ() + 0.3 + (i / 4) * 0.6, 0.8F, i * 30F);
            }
            double minQp = 1, maxNp = 0;
            double minQa = 1, minQb = 1, maxNa = 0, maxNb = 0, minTb = 99, maxTb = 0;
            for (int d = 1; d <= 28; d++) {
                while (crank(h, f) == Winding.Result.WOUND) { } // M67: the keeper cranks the tank's store full once a day, at the filter
                day(level, a, fa);
                day(level, b, fb);
                day(level, p, fp);
                minQp = Math.min(minQp, p.getTank().waterQuality());
                maxNp = Math.max(maxNp, p.getTank().nitrate());
                TankData ta = a.getTank(), tb = b.getTank();
                minQa = Math.min(minQa, ta.waterQuality());
                minQb = Math.min(minQb, tb.waterQuality());
                maxNa = Math.max(maxNa, ta.nitrate());
                maxNb = Math.max(maxNb, tb.nitrate());
                if (d > 2) {
                    minTb = Math.min(minTb, tb.waterTemp());
                    maxTb = Math.max(maxTb, tb.waterTemp());
                }
                if (d % 7 == 0) { // a weekly water change of a third with buckets
                    for (TankBlockEntity c : List.of(a, b)) {
                        long third = c.getTank().waterMb() / 3;
                        TankInteractions.changeWater(level, c, -third);
                        TankInteractions.changeWater(level, c, third);
                    }
                }
            }
            boolean aliveA = fa.stream().allMatch(BettaEntity::isAlive), aliveB = fb.stream().allMatch(BettaEntity::isAlive),
                    aliveP = fp.stream().allMatch(BettaEntity::isAlive);
            LOGGER.info("M37 keeping without a generator, P (dense floating plants, no water change, no filter): quality min {}, nitrate max {}, plants {}, "
                    + "light {}, alive {}, sick {}", minQp, maxNp, p.getTank().plants().size(), org.overengineer.bettacraft.tank.plants.TankPlants.light(level, p),
                    aliveP, fp.stream().map(x -> x.health().isSick()).toList());
            LOGGER.info("M37 keeping without a generator, 28 days, ambient {} °C: A (water changes only) quality min {}, nitrate max {}, temp {}, alive {}, sick {}; "
                            + "B (wound filter + heater) quality min {}, nitrate max {}, temp {}..{}, alive {}, sick {}",
                    ambient, minQa, maxNa, a.getTank().waterTemp(), aliveA, fa.stream().map(x -> x.health().isSick()).toList(),
                    minQb, maxNb, minTb, maxTb, aliveB, fb.stream().map(x -> x.health().isSick()).toList());
            h.assertTrue(aliveB && fb.stream().noneMatch(x -> x.health().isSick()), "B: the bettas are fine");
            h.assertTrue(minQb >= minQa && minQb > 0.5, "B: the wound filter keeps the water clean: " + minQb + " (A " + minQa + ")");
            h.assertTrue(Math.abs(minTb - b.getTank().targetTemp()) < 1.5 && Math.abs(maxTb - b.getTank().targetTemp()) < 1.5, "B: the heater on the cranked store holds "
                    + b.getTank().targetTemp() + ": " + minTb + ".." + maxTb);
            h.assertTrue(aliveA, "A: water changes alone keep them alive (ambient " + ambient + " °C)");
            h.assertTrue(aliveP && fp.stream().noneMatch(x -> x.health().isSick()) && minQp > 0.4, "P: dense floating plants alone keep the water: " + minQp);
        } finally {
            level.setDayTime(savedTime);
            saved.apply();
        }
        h.succeed();
    }
}
