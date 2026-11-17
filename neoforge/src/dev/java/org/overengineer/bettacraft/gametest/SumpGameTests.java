package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.Sump;
import org.overengineer.bettacraft.equipment.SumpHost;
import org.overengineer.bettacraft.reef.CalciumReactorBlock;
import org.overengineer.bettacraft.reef.CalciumReactorBlockEntity;
import org.overengineer.bettacraft.reef.DosingPumpBlock;
import org.overengineer.bettacraft.reef.DosingPumpBlockEntity;
import org.overengineer.bettacraft.reef.TankReef;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWater;

/**
 * M49 part a: the sump in a cabinet adds its water to the tank (more room, the values spread over more water), works as a
 * filter with its heaters, skimmer and media; the refugium takes up nitrate and grows; a dosing pump beside the cabinet
 * reaches the tank; the calcium reactor raises calcium and alkalinity with CO2 and rubble; the switches turn each of them off.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SumpGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private SumpGameTests() {}

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestFailure(what);
    }

    /** A 2x2x2 tank at (x, 2, z) with a cabinet under its corner; returns the controller. */
    static TankBlockEntity tankWithCabinet(GameTestHelper h, int x, int z, float salt) {
        BlockPos cab = new BlockPos(x, 1, z);
        h.setBlock(cab, ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
        TankBlockEntity c = SeahorseGameTests.tank(h, x, 2, z, 2, 2, 2, 8000, salt, 25F);
        c.getTank().setWaterQuality(1F);
        TankEquipment.invalidate(c);
        return c;
    }

    static CabinetBlockEntity cabinet(GameTestHelper h, int x, int z) {
        return h.getBlockEntity(new BlockPos(x, 1, z), CabinetBlockEntity.class);
    }

    /** A sump with these chambers into the cabinet. */
    static void putSump(CabinetBlockEntity cab, ItemStack... chambers) {
        ItemStack sump = new ItemStack(ModItems.SUMP.get());
        NonNullList<ItemStack> ch = NonNullList.withSize(Sump.CHAMBERS, ItemStack.EMPTY);
        for (int i = 0; i < chambers.length && i < Sump.CHAMBERS; i++) ch.set(i, chambers[i]);
        Sump.write(sump, ch);
        cab.setSumpItem(sump);
    }

    private static void step(GameTestHelper h, TankBlockEntity c, double days) {
        TankEquipment.invalidate(c);
        Sump.step(h.getLevel(), c, days);
    }

    /** The sump adds 0.5 water blocks for the stocking and the values, its return pump is a filter, its chambers work. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void sumpAddsWaterAndItsChambersWork(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tankWithCabinet(h, 2, 2, 35F);
        TankData t = c.getTank();
        step(h, c, 0.01);
        check(t.sumpBlocks() == 0 && Math.abs(t.systemBlocks() - t.waterBlocks()) < 1e-9, "no sump yet");
        int filtersBefore = TankEquipment.filters(level, c).size();
        double cleanBefore = TankWater.filterBlocks(level, c);
        int skimmersBefore = TankReef.skimmerCount(level, c);
        int capBefore = TankInhabitants.hardCap(t.systemBlocks());
        putSump(cabinet(h, 2, 2), new ItemStack(ModItems.HEATER_MEDIUM.get()), ItemStack.EMPTY, new ItemStack(ModItems.PROTEIN_SKIMMER.get()),
                new ItemStack(ModItems.CERAMIC_RINGS.get()));
        step(h, c, 0.01);
        check(Math.abs(t.sumpBlocks() - 0.5) < 1e-9 && t.systemBlocks() > t.waterBlocks() + 0.49, "the sump adds its water: " + t.sumpBlocks());
        check(TankInhabitants.hardCap(t.systemBlocks()) >= capBefore, "room for at least as many animals");
        List<?> filters = TankEquipment.filters(level, c);
        check(filters.size() == filtersBefore + 1 && filters.stream().anyMatch(f -> f instanceof SumpHost), "the return pump is a filter: " + filters);
        check(TankCare.filterHeaterBlocks(level, t) > 0, "the heater in the sump heats");
        check(TankReef.skimmerCount(level, c) == skimmersBefore + 1, "the skimmer in the sump counts");
        check(TankWater.filterBlocks(level, c) > cleanBefore + 20, "pump and ceramic rings clean: " + TankWater.filterBlocks(level, c) + " vs " + cleanBefore);
        // the media clog while the pump runs
        ItemStack rings = Sump.read(cabinet(h, 2, 2).sumpItem()).get(Sump.MEDIA0);
        int dirt = rings.getDamageValue();
        step(h, c, 5.0);
        check(Sump.read(cabinet(h, 2, 2).sumpItem()).get(Sump.MEDIA0).getDamageValue() > dirt, "the ceramic rings clogged");
        // the same values spread over more water: a dose raises the calcium less
        float ca = t.calcium();
        org.overengineer.bettacraft.reef.ReefDosing.dose(t, org.overengineer.bettacraft.reef.ReefDosing.Additive.CALCIUM, 10);
        double withSump = t.calcium() - ca;
        double expected = org.overengineer.bettacraft.chem.WaterChemistry.dose(10, org.overengineer.bettacraft.reef.ReefDosing.Additive.CALCIUM.perMl(), t.waterBlocks());
        check(withSump < expected - 1e-6, "the dose spreads over the sump too: " + withSump + " vs " + expected);
        h.succeed();
    }

    /** A lit refugium takes up nitrate and the Chaetomorpha grows; without light it does neither. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void refugiumLowersNitrateAndGrows(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity lit = tankWithCabinet(h, 2, 2, 0F), dark = tankWithCabinet(h, 8, 2, 0F);
        putSump(cabinet(h, 2, 2), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                new ItemStack(ModItems.CHAETOMORPHA.get(), 4), new ItemStack(ModItems.TANK_LIGHT.get()));
        putSump(cabinet(h, 8, 2), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                new ItemStack(ModItems.CHAETOMORPHA.get(), 4));
        for (TankBlockEntity c : List.of(lit, dark)) {
            c.getTank().setNitrate(40F);
            step(h, c, 0.001);
        }
        check(Sump.nitrateUptakePerDay(level, lit) > 0 && Sump.nitrateUptakePerDay(level, dark) == 0, "only the lit refugium takes up nitrate");
        check(Sump.algaeFactor(level, lit) < 1 && Sump.algaeFactor(level, dark) == 1, "the lit Chaetomorpha starves the glass algae");
        for (int i = 0; i < 20; i++) for (TankBlockEntity c : List.of(lit, dark)) step(h, c, 0.5);
        check(lit.getTank().nitrate() < 40F - 3, "the nitrate fell in the lit tank: " + lit.getTank().nitrate());
        check(Math.abs(dark.getTank().nitrate() - 40F) < 1e-3, "the dark refugium did nothing: " + dark.getTank().nitrate());
        int grown = Sump.read(cabinet(h, 2, 2).sumpItem()).get(Sump.ALGAE).getCount();
        check(grown > 4 && grown <= Sump.MAX_ALGAE, "the Chaetomorpha grew: " + grown);
        check(Sump.read(cabinet(h, 8, 2).sumpItem()).get(Sump.ALGAE).getCount() == 4, "without light it does not grow");
        h.succeed();
    }

    /** A dosing pump and a level sensor beside the cabinet of a sump reach the tank above it. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void devicesBesideTheCabinetServeTheTank(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tankWithCabinet(h, 4, 4, 35F);
        BlockPos pumpRel = new BlockPos(4, 1, 3);
        h.setBlock(pumpRel, ModBlocks.DOSING_PUMP.get().defaultBlockState().setValue(DosingPumpBlock.FACING, Direction.NORTH));
        DosingPumpBlockEntity pump = h.getBlockEntity(pumpRel, DosingPumpBlockEntity.class);
        check(pump.targets().isEmpty(), "no sump: the pump beside the cabinet reaches nothing");
        check(Sump.tankBeside(level, h.absolutePos(pumpRel)) == null, "no tank beside without a sump");
        putSump(cabinet(h, 4, 4));
        check(pump.targets().size() == 1 && pump.targets().getFirst() == c, "the pump reaches the tank above the sump");
        check(Sump.tankBeside(level, h.absolutePos(new BlockPos(5, 1, 3))) == null, "only right beside the cabinet");
        check(Sump.tankBeside(level, h.absolutePos(new BlockPos(3, 1, 4))) == c, "a level sensor there tops off this tank");
        h.succeed();
    }

    private static CalciumReactorBlockEntity reactor(GameTestHelper h, BlockPos rel, int rubble, boolean gas) {
        h.setBlock(rel, ModBlocks.CALCIUM_REACTOR.get().defaultBlockState().setValue(CalciumReactorBlock.FACING, Direction.NORTH));
        CalciumReactorBlockEntity r = h.getBlockEntity(rel, CalciumReactorBlockEntity.class);
        if (rubble > 0) r.items().setStackInSlot(0, new ItemStack(ModItems.CORAL_RUBBLE.get(), rubble));
        if (gas) {
            BlockPos cyl = rel.north();
            h.setBlock(cyl, ModBlocks.CO2_CYLINDER.get().defaultBlockState());
            (h.getBlockEntity(cyl, org.overengineer.bettacraft.co2.Co2CylinderBlockEntity.class)).setGas(1000F);
        }
        return r;
    }

    /** The calcium reactor next to a reef tank raises calcium and alkalinity, uses rubble and gas; without CO2 it waits. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void calciumReactorRaisesCalciumAndKh(GameTestHelper h) {
        TankBlockEntity c = SeahorseGameTests.tank(h, 4, 2, 4, 2, 2, 2, 8000, 35F, 25F);
        TankData t = c.getTank();
        t.setCalcium(340F);
        t.setKh(6F);
        CalciumReactorBlockEntity r = reactor(h, new BlockPos(6, 2, 4), 32, true);
        r.step(0);
        check(r.status() == CalciumReactorBlockEntity.RUNNING, "it runs: " + r.status());
        float ca = t.calcium(), kh = t.kh();
        for (int i = 0; i < 10; i++) r.step(0.2);
        check(t.calcium() > ca + 5 && t.kh() > kh + 0.05, "calcium and KH rose: " + t.calcium() + " / " + t.kh());
        check(r.items().getStackInSlot(0).getCount() < 32, "it used rubble: " + r.items().getStackInSlot(0).getCount());
        // calcium up to the target: the valve closes
        t.setCalcium(CalciumReactorBlockEntity.TARGET_CALCIUM + 5);
        r.step(0.1);
        check(r.status() == CalciumReactorBlockEntity.FULL, "full at the target");
        // a second reactor without gas waits
        TankBlockEntity c2 = SeahorseGameTests.tank(h, 12, 2, 4, 2, 2, 2, 8000, 35F, 25F);
        c2.getTank().setCalcium(340F);
        CalciumReactorBlockEntity dry = reactor(h, new BlockPos(14, 2, 4), 32, false);
        dry.step(1.0);
        check(dry.status() == CalciumReactorBlockEntity.NO_CO2 && c2.getTank().calcium() == 340F, "no CO2: nothing dissolves");
        h.succeed();
    }

    /** The switches: the sump off adds no water and no filter; the refugium off takes up nothing; the reactor off stands still. */
    @GameTest(template = EMPTY, batch = "bettacraft_m49_sump_off", timeoutTicks = 200)
    public static void sumpSwitchesOff(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tankWithCabinet(h, 2, 2, 35F);
        putSump(cabinet(h, 2, 2), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                new ItemStack(ModItems.CHAETOMORPHA.get(), 8), new ItemStack(ModItems.TANK_LIGHT.get()));
        c.getTank().setNitrate(40F);
        CalciumReactorBlockEntity r = reactor(h, new BlockPos(4, 2, 3), 16, true);
        c.getTank().setCalcium(340F);
        boolean sump = Features.enabled(Feature.SUMP);
        try {
            Features.set(Feature.REFUGIUM, false);
            step(h, c, 1.0);
            check(c.getTank().sumpBlocks() > 0 && Math.abs(c.getTank().nitrate() - 40F) < 1e-3, "refugium off: the sump runs, no uptake");
            Features.set(Feature.REFUGIUM, true);
            Features.set(Feature.CALCIUM_REACTOR, false);
            r.step(1.0);
            check(r.status() == CalciumReactorBlockEntity.OFF && c.getTank().calcium() == 340F, "reactor off: no calcium");
            Features.set(Feature.CALCIUM_REACTOR, true);
            Features.set(Feature.SUMP, false);
            step(h, c, 1.0);
            check(c.getTank().sumpBlocks() == 0 && Sump.hosts(level, c).isEmpty(), "sump off: no water, no filter");
            check(Math.abs(c.getTank().nitrate() - 40F) < 1e-3, "sump off: no refugium either");
            check(!Features.enabled(Feature.REFUGIUM) && !Features.enabled(Feature.CALCIUM_REACTOR), "the children go off with the sump");
        } finally {
            Features.set(Feature.SUMP, sump);
            Features.set(Feature.REFUGIUM, true);
            Features.set(Feature.CALCIUM_REACTOR, true);
        }
        h.succeed();
    }
}
