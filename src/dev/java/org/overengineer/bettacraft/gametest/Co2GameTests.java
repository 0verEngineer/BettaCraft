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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.chem.ChemTolerance;
import org.overengineer.bettacraft.chem.TankChemistry;
import org.overengineer.bettacraft.chem.WaterChemistry;
import org.overengineer.bettacraft.co2.BioCo2Block;
import org.overengineer.bettacraft.co2.BioCo2BlockEntity;
import org.overengineer.bettacraft.co2.Co2CylinderBlock;
import org.overengineer.bettacraft.co2.Co2CylinderBlockEntity;
import org.overengineer.bettacraft.co2.Co2CylinderItem;
import org.overengineer.bettacraft.co2.Co2StationBlockEntity;
import org.overengineer.bettacraft.co2.TankCo2;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateCare;
import org.overengineer.bettacraft.osmosis.RoUnitBlockEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModFluids;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankFluid;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankLighting;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M25 part b GameTests: the CO2 cylinder doses CO2 (pH drops, aeration strips it, the gas is used up), the solenoid
 * valve closes it while the lights are off, too much CO2 stresses the fish, bio CO2 ferments and is spent, the filling
 * station fills a cylinder from a burning furnace (with power: only with FE), the reverse osmosis unit makes RO water at
 * 1 : 3 from a water source and wears its parts, RO and sea water fluids become water with their values in a tank,
 * the sea water bucket gives other mods the sea water fluid, the feature switches. Deterministic: steps are called directly.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Co2GameTests {
    private static final String EMPTY = "empty_32x10x32";

    private Co2GameTests() {}

    private static Co2CylinderBlockEntity cylinder(GameTestHelper h, TankBlockEntity c, BlockPos rel, boolean solenoid) {
        h.setBlock(rel, ModBlocks.CO2_CYLINDER.get().defaultBlockState().setValue(Co2CylinderBlock.FACING, Direction.NORTH).setValue(Co2CylinderBlock.SOLENOID, solenoid));
        TankEquipment.invalidate(c);
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof Co2CylinderBlockEntity b)) throw new GameTestAssertException("no cylinder");
        b.setGas(Co2CylinderBlockEntity.capacity());
        return b;
    }

    private static ServerPlayer player(GameTestHelper h, ItemStack stack) {
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        sp.getInventory().clearContent();
        sp.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return sp;
    }

    /** The cylinder doses CO2: it rises to a good level, the pH drops, aeration strips it, the gas is used up. */
    @GameTest(template = EMPTY, batch = "bettacraft_m25_co2", timeoutTicks = 200)
    public static void cylinderDosesCo2(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 1, 2, 1, 2, 2, 2);
        TankData t = c.getTank();
        Co2CylinderBlockEntity cyl = cylinder(h, c, new BlockPos(3, 2, 1), false);
        h.assertTrue(TankEquipment.co2Sources(level, c).size() == 1, "the cylinder serves the tank");
        cyl.setPower(2);
        c.o2Exchange = 1.0;
        float gas = cyl.gas();
        for (int i = 0; i < 6; i++) TankCo2.step(level, c, 1.0);
        h.assertTrue(c.co2Bubbles == 2, "2 bubbles per second: " + c.co2Bubbles);
        h.assertTrue(t.co2() > 15 && t.co2() < 50, "a good CO2 level: " + t.co2());
        h.assertTrue(Math.abs(cyl.gas() - (gas - 12)) < 1e-3, "2 bubbles for 6 days use 12 bubble-days: " + cyl.gas());
        h.assertTrue(level.getBlockState(cyl.getBlockPos()).getValue(Co2CylinderBlock.ACTIVE), "the bubble counter runs");
        t.setPh(7.5F);
        TankChemistry.step(level, c, 3);
        h.assertTrue(t.ph() < 7.0F, "CO2 lowers the pH: " + t.ph());
        // strong aeration drives it out
        float dosed = t.co2();
        c.o2Exchange = 6.0;
        for (int i = 0; i < 4; i++) TankCo2.step(level, c, 1.0);
        h.assertTrue(t.co2() < dosed * 0.5, "aeration strips the CO2: " + t.co2() + " from " + dosed);
        // empty: back to the air
        cyl.setGas(0);
        for (int i = 0; i < 10; i++) TankCo2.step(level, c, 1.0);
        h.assertTrue(c.co2Bubbles == 0 && t.co2() < WaterChemistry.CO2_AIR + 0.5, "an empty cylinder doses nothing: " + t.co2());
        // the cylinder keeps its gas as an item
        cyl.setGas(30F);
        ItemStack drop = new ItemStack(ModItems.CO2_CYLINDER.get());
        drop.applyComponents(cyl.collectComponents());
        h.assertTrue(Math.abs(Co2CylinderItem.gas(drop) - 30F) < 1e-3, "the item keeps the gas: " + Co2CylinderItem.gas(drop));
        h.succeed();
    }

    /** The solenoid valve closes the cylinder while the tank's lights are off (daylight timer) and opens it in the light. */
    @GameTest(template = EMPTY, batch = "bettacraft_m25_co2", timeoutTicks = 100)
    public static void solenoidClosesAtNight(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 1, 2, 1, 2, 2, 2);
        TankData t = c.getTank();
        Co2CylinderBlockEntity cyl = cylinder(h, c, new BlockPos(3, 2, 1), true);
        cyl.setPower(4);
        int now = TankLighting.minuteOfDay(level.getDayTime());
        t.setLightTimer(true);
        // lights on two to four hours from now: off right now
        t.setLightOn((now + 120) % 1440);
        t.setLightOff((now + 240) % 1440);
        h.assertTrue(cyl.bubbles(level, c) == 0 && cyl.closed(), "closed while the lights are off");
        // lights on since an hour ago
        t.setLightOn((now + 1440 - 120) % 1440);
        t.setLightOff((now + 240) % 1440);
        h.assertTrue(cyl.bubbles(level, c) == 4 && !cyl.closed(), "open while the lights are on");
        // a cylinder without a valve runs regardless
        Co2CylinderBlockEntity plain = cylinder(h, c, new BlockPos(1, 2, 3), false);
        plain.setPower(3);
        t.setLightOn((now + 120) % 1440);
        h.assertTrue(plain.bubbles(level, c) == 3, "no valve: it bubbles at night too");
        h.succeed();
    }

    /** Too much CO2 stresses the fish (bettas bear more), far too much is bad water; shrimp suffer earlier. */
    @GameTest(template = EMPTY, batch = "bettacraft_m25_co2", timeoutTicks = 100)
    public static void overdoseHarmsTheAnimals(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 1, 2, 1, 2, 2, 2);
        TankData t = c.getTank();
        var neon = AerationGameTests.add(h, c, TankmateSpecies.NEON_TETRA, LifeStage.ADULT);
        t.setCo2(20F);
        TankmateCare.advance(level, c, neon, 0.2);
        h.assertTrue((neon.stressBits() & TankmateCare.STRESS_CHEMISTRY) == 0, "20 mg/L is fine");
        t.setCo2(40F);
        TankmateCare.advance(level, c, neon, 0.2);
        h.assertTrue((neon.stressBits() & TankmateCare.STRESS_CHEMISTRY) != 0, "40 mg/L stresses a tetra");
        h.assertTrue(TankChemistry.fit(t, ChemTolerance.BETTA) == ChemTolerance.Fit.OK, "a betta breathes air: 40 mg/L is fine");
        t.setCo2(70F);
        h.assertTrue(TankChemistry.bad(t, ChemTolerance.SOFT) && TankChemistry.co2Bad(t, ChemTolerance.SOFT), "70 mg/L is bad water");
        h.assertTrue(TankChemistry.bad(t, ChemTolerance.BETTA), "70 mg/L is bad for a betta too");
        h.succeed();
    }

    /** Bio CO2: water, sugar and yeast from the hand start the fermentation; it bubbles weakly and is spent at the end. */
    @GameTest(template = EMPTY, batch = "bettacraft_m25_co2", timeoutTicks = 100)
    public static void bioCo2Ferments(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 1, 2, 1, 2, 2, 2);
        BlockPos rel = new BlockPos(3, 2, 1);
        h.setBlock(rel, ModBlocks.BIO_CO2.get().defaultBlockState());
        TankEquipment.invalidate(c);
        BioCo2BlockEntity bio = (BioCo2BlockEntity) level.getBlockEntity(h.absolutePos(rel));
        if (bio == null) throw new GameTestAssertException("no bio CO2");
        ServerPlayer sp = player(h, new ItemStack(Items.WATER_BUCKET));
        bio.use(sp, InteractionHand.MAIN_HAND, sp.getMainHandItem());
        h.assertTrue(sp.getMainHandItem().is(Items.BUCKET), "the water went in");
        ItemStack sugar = new ItemStack(Items.SUGAR, 6);
        for (int i = 0; i < 6; i++) bio.use(sp, InteractionHand.MAIN_HAND, sugar);
        h.assertTrue(bio.sugar() == 4 && sugar.getCount() == 2, "four sugar at most: " + bio.sugar());
        ItemStack yeast = new ItemStack(ModItems.YEAST.get());
        bio.use(sp, InteractionHand.MAIN_HAND, yeast);
        h.assertTrue(yeast.isEmpty() && bio.fermenting(), "the yeast starts it");
        h.assertTrue(h.getBlockState(rel).getValue(BioCo2Block.STAGE) == BioCo2Block.FERMENTING, "fermenting stage");
        long now = level.getGameTime();
        double peak = bio.bubblesAt(now + 24000L);
        h.assertTrue(peak > 0.5 && peak < 1.5, "about a bubble a second at its best: " + peak);
        h.assertTrue(bio.bubblesAt(now + 600) < peak, "it starts slowly");
        // long ago: spent
        bio.start(now - 5 * 24000L);
        TankCo2.step(level, c, 0.1);
        h.assertTrue(!bio.fermenting() && h.getBlockState(rel).getValue(BioCo2Block.STAGE) == BioCo2Block.SPENT, "spent after its days");
        h.succeed();
    }

    /** The filling station fills a cylinder from the flue gas of a burning furnace, not from a cold one. */
    @GameTest(template = EMPTY, batch = "bettacraft_m25_co2", timeoutTicks = 100)
    public static void stationFillsFromFlueGas(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos furnace = new BlockPos(2, 1, 2), station = new BlockPos(2, 2, 2);
        h.setBlock(furnace, Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.LIT, false));
        h.setBlock(station, ModBlocks.CO2_STATION.get().defaultBlockState());
        Co2StationBlockEntity st = (Co2StationBlockEntity) level.getBlockEntity(h.absolutePos(station));
        if (st == null) throw new GameTestAssertException("no station");
        st.items().setStackInSlot(0, new ItemStack(ModItems.CO2_CYLINDER.get()));
        h.assertTrue(st.step(level, 1200) == 0, "a cold furnace gives nothing");
        h.setBlock(furnace, Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        double added = st.step(level, 1200);
        double want = Co2CylinderBlockEntity.capacity() * 1200.0 / ServerConfig.getOr(ServerConfig.CO2_STATION_TICKS, 24000);
        h.assertTrue(Math.abs(added - want) < 1e-3, "one fire: " + added + " of " + want);
        h.assertTrue(Math.abs(Co2CylinderItem.gas(st.items().getStackInSlot(0)) - want) < 1e-3, "the cylinder holds it");
        // hoppers take only full cylinders out
        h.assertTrue(st.automation().extractItem(0, 1, true).isEmpty(), "not full: stays in");
        Co2CylinderItem.setGas(st.items().getStackInSlot(0), Co2CylinderBlockEntity.capacity());
        h.assertTrue(!st.automation().extractItem(0, 1, true).isEmpty(), "full: a hopper may take it");
        h.assertTrue(st.step(level, 1200) == 0, "a full cylinder takes no more");
        h.succeed();
    }

    /** With a power option on the compressor needs FE. */
    @GameTest(template = EMPTY, batch = "bettacraft_m25_co2_power", timeoutTicks = 100)
    public static void stationAndRoUnitWithPower(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        boolean saved = ServerConfig.POWER_PUMPS.get();
        try {
            ServerConfig.POWER_PUMPS.set(true);
            BlockPos furnace = new BlockPos(2, 1, 2), station = new BlockPos(2, 2, 2);
            h.setBlock(furnace, Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.LIT, true));
            h.setBlock(station, ModBlocks.CO2_STATION.get().defaultBlockState());
            Co2StationBlockEntity st = (Co2StationBlockEntity) level.getBlockEntity(h.absolutePos(station));
            if (st == null) throw new GameTestAssertException("no station");
            st.items().setStackInSlot(0, new ItemStack(ModItems.CO2_CYLINDER.get()));
            h.assertTrue(st.step(level, 20) == 0 && st.lastNoPower(), "no FE: the compressor stands still");
            st.energy().receiveEnergy(20000, false);
            h.assertTrue(st.step(level, 20) > 0, "with FE it fills");
            // the reverse osmosis unit: slow without FE, four times faster with it
            BlockPos water = new BlockPos(5, 2, 5), ro = new BlockPos(6, 2, 5);
            h.setBlock(water, Blocks.WATER.defaultBlockState());
            h.setBlock(ro, ModBlocks.RO_UNIT.get().defaultBlockState());
            RoUnitBlockEntity unit = (RoUnitBlockEntity) level.getBlockEntity(h.absolutePos(ro));
            if (unit == null) throw new GameTestAssertException("no RO unit");
            unit.parts().setStackInSlot(RoUnitBlockEntity.MEMBRANE, new ItemStack(ModItems.RO_MEMBRANE.get()));
            unit.parts().setStackInSlot(RoUnitBlockEntity.PREFILTER, new ItemStack(ModItems.RO_PREFILTER.get()));
            int slow = unit.step(level, 20);
            unit.energy().receiveEnergy(20000, false);
            int fast = unit.step(level, 20);
            h.assertTrue(slow == 20 && fast == 80 && unit.boosted(), "slow " + slow + ", with FE " + fast);
        } finally {
            ServerConfig.POWER_PUMPS.set(saved);
        }
        h.succeed();
    }

    /** The reverse osmosis unit: RO water at 1 : 3 from the water it gets, parts wear out, without parts it stops. */
    @GameTest(template = EMPTY, batch = "bettacraft_m25_co2", timeoutTicks = 100)
    public static void roUnitMakesRoWater(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos ro = new BlockPos(2, 2, 2);
        h.setBlock(ro, ModBlocks.RO_UNIT.get().defaultBlockState());
        RoUnitBlockEntity unit = (RoUnitBlockEntity) level.getBlockEntity(h.absolutePos(ro));
        if (unit == null) throw new GameTestAssertException("no RO unit");
        unit.setFeed(1000);
        h.assertTrue(unit.step(level, 20) == 0 && unit.stopReason().equals("no_membrane"), "no membrane: stops");
        unit.parts().setStackInSlot(RoUnitBlockEntity.MEMBRANE, new ItemStack(ModItems.RO_MEMBRANE.get()));
        unit.parts().setStackInSlot(RoUnitBlockEntity.PREFILTER, new ItemStack(ModItems.RO_PREFILTER.get()));
        int made = 0;
        for (int i = 0; i < 30; i++) made += unit.step(level, 20);
        h.assertTrue(made == 250 && unit.feed() == 0 && unit.pure() == 250, "a bucket gives a quarter: " + made + " feed " + unit.feed());
        h.assertTrue(unit.stopReason().equals("no_water"), "then it waits for water");
        // the prefilter wears faster than the membrane
        unit.setFeed(4000);
        for (int i = 0; i < 50; i++) unit.step(level, 20);
        ItemStack pre = unit.parts().getStackInSlot(RoUnitBlockEntity.PREFILTER), mem = unit.parts().getStackInSlot(RoUnitBlockEntity.MEMBRANE);
        h.assertTrue(pre.getDamageValue() > mem.getDamageValue() && pre.getDamageValue() > 0, "wear: prefilter " + pre.getDamageValue() + ", membrane " + mem.getDamageValue());
        // an empty bucket takes RO water out; other mods get bettacraft:ro_water
        IFluidHandler fh = unit.fluid();
        FluidStack sim = fh.drain(1000, IFluidHandler.FluidAction.SIMULATE);
        h.assertTrue(sim.getFluid().isSame(ModFluids.RO_WATER.get()) && sim.getAmount() == 1000, "RO water for other mods: " + sim.getAmount());
        h.assertTrue(fh.fill(new FluidStack(ModFluids.RO_WATER.get(), 100), IFluidHandler.FluidAction.SIMULATE) == 0, "it does not take its own water");
        h.assertTrue(fh.fill(new FluidStack(ModFluids.SEA_WATER.get(), 100), IFluidHandler.FluidAction.SIMULATE) == 100, "sea water goes in (the membrane holds the salt back)");
        // a water source next to it is endless
        unit.setFeed(0);
        unit.setPure(0);
        h.setBlock(new BlockPos(1, 2, 2), Blocks.WATER.defaultBlockState());
        h.assertTrue(unit.step(level, 20) == 20 && unit.feed() == 0, "a water source block feeds it");
        h.succeed();
    }

    /** RO water and sea water as fluids become water with their values in a tank; the sea water bucket gives other mods sea water. */
    @GameTest(template = EMPTY, batch = "bettacraft_m25_co2", timeoutTicks = 100)
    public static void waterFluidsCarryTheirValues(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 1, 2, 1, 2, 2, 2);
        TankData t = c.getTank();
        long half = t.capacityMb() / 2;
        TankInteractions.changeWater(level, c, -half);
        TankFluid tf = new TankFluid(level, c.getBlockPos());
        int n = tf.fill(new FluidStack(ModFluids.RO_WATER.get(), (int) half), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(n == half, "the tank takes RO water: " + n);
        h.assertTrue(Math.abs(t.tds() - WaterChemistry.TAP_TDS / 2) < 1, "half RO water halves the TDS: " + t.tds());
        // sea water
        TankBlockEntity sea = AerationGameTests.tank(h, 5, 2, 1, 2, 2, 2);
        TankInteractions.changeWater(level, sea, -sea.getTank().capacityMb());
        new TankFluid(level, sea.getBlockPos()).fill(new FluidStack(ModFluids.SEA_WATER.get(), 4000), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(Math.abs(sea.getTank().salinity() - Salinity.SEA) < 0.01, "sea water salts the tank: " + sea.getTank().salinity());
        // the old sea water bucket: other mods see the sea water fluid, emptied it is a bucket
        var handler = FluidUtil.getFluidHandler(new ItemStack(ModItems.SEA_WATER_BUCKET.get())).orElse(null);
        h.assertTrue(handler != null, "the sea water bucket has a fluid handler");
        FluidStack in = handler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(in.getFluid().isSame(ModFluids.SEA_WATER.get()) && handler.getContainer().is(Items.BUCKET), "sea water out, bucket left");
        // an empty bucket filled with RO water is an RO water bucket
        var empty = FluidUtil.getFluidHandler(new ItemStack(Items.BUCKET)).orElse(null);
        h.assertTrue(empty != null && empty.fill(new FluidStack(ModFluids.RO_WATER.get(), 1000), IFluidHandler.FluidAction.EXECUTE) == 1000
                && empty.getContainer().is(ModItems.RO_WATER_BUCKET.get()), "a bucket of RO water");
        // the RO water bucket on a tank is a water change with nothing dissolved
        TankInteractions.changeWater(level, c, -1000);
        float before = t.tds();
        ServerPlayer sp = player(h, new ItemStack(ModItems.RO_WATER_BUCKET.get()));
        TankInteractions.useItemOn(sp.getMainHandItem(), level, c.getBlockPos(), sp, InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(c.getBlockPos().getCenter(), Direction.UP, c.getBlockPos(), false));
        h.assertTrue(sp.getMainHandItem().is(Items.BUCKET) && t.tds() < before, "the RO bucket softens the water: " + t.tds());
        // the reservoir takes RO water with its values
        var store = new org.overengineer.bettacraft.plumbing.WaterStore(4000, () -> {});
        store.fill(new FluidStack(ModFluids.RO_WATER.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(store.values() != null && store.values().tds() == 0F, "the reservoir keeps RO water's values");
        h.succeed();
    }

    /** features.co2 and features.reverseOsmosis off: no CO2 from the devices, the unit stands still. */
    @GameTest(template = EMPTY, batch = "bettacraft_m25_co2_off", timeoutTicks = 100)
    public static void featuresOff(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        boolean co2 = CommonConfig.FEATURE_CO2.get(), ro = CommonConfig.FEATURE_REVERSE_OSMOSIS.get();
        try {
            CommonConfig.FEATURE_CO2.set(false);
            CommonConfig.FEATURE_REVERSE_OSMOSIS.set(false);
            TankBlockEntity c = AerationGameTests.tank(h, 1, 2, 1, 2, 2, 2);
            TankData t = c.getTank();
            Co2CylinderBlockEntity cyl = cylinder(h, c, new BlockPos(3, 2, 1), false);
            cyl.setPower(10);
            t.setCo2(60F);
            c.o2Exchange = 1.0;
            for (int i = 0; i < 10; i++) TankCo2.step(level, c, 1.0);
            h.assertTrue(c.co2Bubbles == 0 && t.co2() < WaterChemistry.CO2_AIR + 0.5, "no CO2 with the feature off: " + t.co2());
            h.assertTrue(cyl.gas() == Co2CylinderBlockEntity.capacity(), "no gas used");
            t.setCo2(60F);
            h.assertTrue(TankChemistry.fit(t, ChemTolerance.SOFT) == ChemTolerance.Fit.OK, "nobody minds the CO2");
            BlockPos ro2 = new BlockPos(6, 2, 6);
            h.setBlock(ro2, ModBlocks.RO_UNIT.get().defaultBlockState());
            RoUnitBlockEntity unit = (RoUnitBlockEntity) level.getBlockEntity(h.absolutePos(ro2));
            if (unit == null) throw new GameTestAssertException("no RO unit");
            unit.parts().setStackInSlot(RoUnitBlockEntity.MEMBRANE, new ItemStack(ModItems.RO_MEMBRANE.get()));
            unit.parts().setStackInSlot(RoUnitBlockEntity.PREFILTER, new ItemStack(ModItems.RO_PREFILTER.get()));
            unit.setFeed(1000);
            h.assertTrue(unit.step(level, 20) == 0 && unit.stopReason().equals("off"), "the unit is off");
        } finally {
            CommonConfig.FEATURE_CO2.set(co2);
            CommonConfig.FEATURE_REVERSE_OSMOSIS.set(ro);
        }
        h.succeed();
    }
}
