package org.overengineer.bettacraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.chem.ChemTolerance;
import org.overengineer.bettacraft.chem.TankChemistry;
import org.overengineer.bettacraft.chem.WaterChemistry;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.reef.Corals;
import org.overengineer.bettacraft.reef.DosingCanisterItem;
import org.overengineer.bettacraft.reef.DosingPumpBlock;
import org.overengineer.bettacraft.reef.DosingPumpBlockEntity;
import org.overengineer.bettacraft.reef.ReefDosing;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;

/**
 * M25 part c GameTests: the dosing pump doses a salt water tank next to it and through pipes (and leaves fresh water
 * alone), canisters are used up and become empty ones, stony corals use alkalinity and magnesium, water changes with sea
 * water and old tanks get the values of their salt; the Caridina need soft water and the Sulawesi animals hard water,
 * the new animals are in the net tables; the feature switches. Deterministic: steps are called directly.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ReefDosingGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private ReefDosingGameTests() {}

    private static TankBlockEntity seaTank(GameTestHelper h, int x) {
        TankBlockEntity c = AerationGameTests.tank(h, x, 2, 1, 2, 2, 2);
        c.getTank().setSalinity(35F);
        c.getTank().setCalcium(420F);
        return c;
    }

    private static DosingPumpBlockEntity pump(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, ModBlocks.DOSING_PUMP.get().defaultBlockState().setValue(DosingPumpBlock.FACING, Direction.NORTH));
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof DosingPumpBlockEntity p)) throw new GameTestAssertException("no pump");
        return p;
    }

    /** The pump doses calcium and alkalinity into the salt water tank next to it; the canisters are used up. */
    @GameTest(template = EMPTY, batch = "bettacraft_m25c", timeoutTicks = 100)
    public static void pumpDosesTheTankNextToIt(GameTestHelper h) {
        TankBlockEntity c = seaTank(h, 1);
        TankData t = c.getTank();
        DosingPumpBlockEntity p = pump(h, new BlockPos(3, 2, 1));
        p.items().setStackInSlot(0, new ItemStack(ModItems.CALCIUM_CANISTER.get()));
        p.items().setStackInSlot(1, new ItemStack(ModItems.ALKALINITY_CANISTER.get()));
        p.setPerDay(0, 40);
        p.setPerDay(1, 40);
        p.setPerDay(2, 30); // no canister: nothing
        float ca = t.calcium(), kh = t.kh();
        p.step(1.0);
        h.assertTrue(p.status() == DosingPumpBlockEntity.RUNNING, "running: " + p.status());
        double blocks = t.waterBlocks();
        h.assertTrue(Math.abs(t.calcium() - (ca + 40 * ReefDosing.Additive.CALCIUM.perMl() / blocks)) < 0.05, "calcium " + ca + " -> " + t.calcium() + " in " + blocks + " blocks");
        h.assertTrue(Math.abs(t.kh() - (kh + 40 * ReefDosing.Additive.ALKALINITY.perMl() / blocks)) < 0.01, "alkalinity " + kh + " -> " + t.kh());
        ItemStack can = p.items().getStackInSlot(0);
        h.assertTrue(Math.round(DosingCanisterItem.mlLeft(can)) == DosingCanisterItem.capacityMl() - 40, "40 mL used: " + DosingCanisterItem.mlLeft(can));
        // nearly empty: it becomes an empty canister
        ItemStack low = new ItemStack(ModItems.MAGNESIUM_CANISTER.get());
        low.setDamageValue(low.getMaxDamage() - 1);
        p.items().setStackInSlot(2, low);
        p.step(1.0);
        h.assertTrue(p.items().getStackInSlot(2).is(ModItems.DOSING_CANISTER.get()), "used up: an empty canister " + p.items().getStackInSlot(2));
        h.assertTrue(p.comparator() == 0, "the comparator sees the empty canister");
        p.step(1.0);
        h.assertTrue(p.status() == DosingPumpBlockEntity.RUNNING, "the other channels keep running");
        // a freshwater tank is left alone
        TankBlockEntity fresh = AerationGameTests.tank(h, 6, 2, 1, 2, 2, 2);
        DosingPumpBlockEntity p2 = pump(h, new BlockPos(8, 2, 1));
        p2.items().setStackInSlot(0, new ItemStack(ModItems.CALCIUM_CANISTER.get()));
        float before = fresh.getTank().calcium();
        p2.step(1.0);
        h.assertTrue(p2.status() == DosingPumpBlockEntity.FRESH && fresh.getTank().calcium() == before, "no dosing into fresh water");
        h.assertTrue(p2.items().getStackInSlot(0).getDamageValue() == 0, "nothing used");
        DosingPumpBlockEntity alone = pump(h, new BlockPos(12, 2, 12));
        alone.step(1.0);
        h.assertTrue(alone.status() == DosingPumpBlockEntity.NO_TANK, "no tank: " + alone.status());
        h.succeed();
    }

    /** Through a pipe the pump reaches a tank further away. */
    @GameTest(template = EMPTY, batch = "bettacraft_m25c", timeoutTicks = 100)
    public static void pumpDosesThroughPipes(GameTestHelper h) {
        TankBlockEntity c = seaTank(h, 1);
        PlumbingGameTests.pipe(h, false, new BlockPos(3, 2, 1), new BlockPos(4, 2, 1), new BlockPos(5, 2, 1));
        DosingPumpBlockEntity p = pump(h, new BlockPos(6, 2, 1));
        p.items().setStackInSlot(3, new ItemStack(ModItems.TRACE_CANISTER.get()));
        p.setPerDay(3, 80);
        h.assertTrue(p.targets().size() == 1 && p.targets().getFirst().getBlockPos().equals(c.getBlockPos()), "the pipe leads to the tank: " + p.targets());
        float tr = c.getTank().trace();
        p.step(1.0);
        h.assertTrue(c.getTank().trace() > tr + 0.1, "trace elements " + tr + " -> " + c.getTank().trace());
        h.assertTrue(p.status() == DosingPumpBlockEntity.RUNNING && p.items().getStackInSlot(3).getDamageValue() > 0, "dosed through the pipe");
        h.succeed();
    }

    /** Stony corals use alkalinity and magnesium; low alkalinity slows them and lowers the pH; sea water brings it back. */
    @GameTest(template = EMPTY, batch = "bettacraft_m25c", timeoutTicks = 100)
    public static void coralsUseAlkalinityAndWaterChangesBringItBack(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = seaTank(h, 1);
        TankData t = c.getTank();
        h.assertTrue(Math.abs(t.kh() - WaterChemistry.SEA_KH) < 0.01 && Math.abs(t.magnesium() - WaterChemistry.SEA_MG) < 1, "sea water values: " + t.kh() + " " + t.magnesium());
        float kh = t.kh(), mg = t.magnesium();
        for (int i = 0; i < 20; i++) Corals.useCalcium(t);
        h.assertTrue(t.kh() < kh - 0.5 && t.magnesium() < mg, "used: KH " + t.kh() + ", Mg " + t.magnesium());
        t.setKh(WaterChemistry.SEA_KH);
        double phSea = TankChemistry.phTarget(t, TankChemistry.Drivers.NONE);
        t.setKh(3.5F);
        h.assertTrue(ReefDosing.stonyFactor(t) == 0.5, "low alkalinity halves the stony corals");
        h.assertTrue(TankChemistry.phTarget(t, TankChemistry.Drivers.NONE) < phSea - 0.1, "and lowers the pH");
        // a big water change with sea water
        TankInteractions.changeWater(level, c, -t.waterMb() / 2);
        TankInteractions.changeWater(level, c, t.capacityMb() - t.waterMb(), 35F);
        h.assertTrue(t.kh() > 5.5, "half sea water: " + t.kh());
        // the care step: magnesium and trace elements fall
        float m = t.magnesium(), tr = t.trace();
        ReefDosing.step(level, c, 3.0);
        h.assertTrue(t.magnesium() < m - 2 && t.trace() < tr, "Mg " + m + " -> " + t.magnesium() + ", trace " + tr + " -> " + t.trace());
        // tanks of earlier versions get the values of their salt
        CompoundTag tag = t.save();
        tag.remove("kh");
        tag.remove("magnesium");
        tag.remove("trace");
        TankData old = TankData.load(tag);
        h.assertTrue(Math.abs(old.kh() - WaterChemistry.khOf(t.salinity())) < 0.01 && Math.abs(old.magnesium() - WaterChemistry.magnesiumOf(t.salinity())) < 1,
                "old tank: " + old.kh() + " " + old.magnesium());
        t.setKh(6.6F);
        h.assertTrue(Math.abs(TankData.load(t.save()).kh() - 6.6F) < 1e-4, "saved");
        h.succeed();
    }

    /** Bee shrimp die in tap water and live in soft water; cardinal shrimp and Tylomelania take hard water. */
    @GameTest(template = EMPTY, batch = "bettacraft_m25c", timeoutTicks = 200)
    public static void caridinaNeedSoftWater(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity tap = AerationGameTests.tank(h, 1, 2, 1, 2, 2, 2);
        TankBlockEntity soft = AerationGameTests.tank(h, 6, 2, 1, 2, 2, 2);
        soft.getTank().setTds(150F);
        soft.getTank().setPh(6.3F);
        for (TankBlockEntity c : new TankBlockEntity[]{tap, soft}) {
            for (int i = 0; i < 6; i++) TankCritters.add(level, c, CritterSpecies.CARIDINA, CritterGenes.pure(CritterGenes.CARIDINA_RED, 0.5F), i % 2 == 0, 10);
        }
        h.assertTrue(TankChemistry.bad(tap.getTank(), ChemTolerance.critter(CritterSpecies.CARIDINA)), "tap water is bad for them");
        h.assertTrue(TankChemistry.fit(soft.getTank(), ChemTolerance.critter(CritterSpecies.CARIDINA)) == ChemTolerance.Fit.OK, "soft water suits them");
        h.assertTrue(TankChemistry.fit(tap.getTank(), ChemTolerance.critter(CritterSpecies.SULAWESI_SHRIMP)) == ChemTolerance.Fit.OK
                && TankChemistry.fit(tap.getTank(), ChemTolerance.critter(CritterSpecies.TYLOMELANIA)) == ChemTolerance.Fit.OK, "tap water suits the Sulawesi animals");
        h.assertTrue(TankChemistry.bad(soft.getTank(), ChemTolerance.critter(CritterSpecies.TYLOMELANIA)), "soft acidic water is bad for Tylomelania");
        for (int i = 0; i < 24 * 12; i++) {
            for (TankBlockEntity c : new TankBlockEntity[]{tap, soft}) {
                c.getTank().setTds(c == soft ? 150F : WaterChemistry.TAP_TDS); // hold the water still
                c.getTank().setPh(c == soft ? 6.3F : WaterChemistry.TAP_PH);
                TankCritters.step(level, c, 1.0 / 24);
            }
        }
        long tapAlive = tap.getTank().critters().stream().filter(k -> k.alive() && k.species == CritterSpecies.CARIDINA).count();
        long softAlive = soft.getTank().critters().stream().filter(k -> k.alive() && k.species == CritterSpecies.CARIDINA).count();
        h.assertTrue(tapAlive < 3, "bee shrimp die in tap water: " + tapAlive + " alive");
        h.assertTrue(softAlive >= 5, "bee shrimp live in soft water: " + softAlive + " alive");
        h.succeed();
    }

    /** The new animals are in the net tables (and so in the wild), with their feature switches. */
    @GameTest(template = EMPTY, batch = "bettacraft_m25c_off", timeoutTicks = 100)
    public static void featureSwitches(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        boolean car = CommonConfig.FEATURE_CARIDINA.get(), sul = CommonConfig.FEATURE_SULAWESI.get(), dos = CommonConfig.FEATURE_REEF_DOSING.get();
        try {
            h.assertTrue(NetCatching.table(NetCatching.Water.RIVER, true, false, false).stream().anyMatch(x -> x.species() == CritterSpecies.CARIDINA), "bee shrimp in rivers");
            h.assertTrue(NetCatching.table(NetCatching.Water.JUNGLE, false, true, false).stream().anyMatch(x -> x.species() == CritterSpecies.TYLOMELANIA)
                    && NetCatching.table(NetCatching.Water.JUNGLE, false, true, false).stream().anyMatch(x -> x.species() == CritterSpecies.SULAWESI_SHRIMP),
                    "Sulawesi animals in jungle water");
            CommonConfig.FEATURE_CARIDINA.set(false);
            CommonConfig.FEATURE_SULAWESI.set(false);
            CommonConfig.FEATURE_REEF_DOSING.set(false);
            h.assertTrue(NetCatching.table(NetCatching.Water.RIVER, true, false, false).stream().noneMatch(x -> x.species() == CritterSpecies.CARIDINA)
                    && NetCatching.table(NetCatching.Water.JUNGLE, false, true, false).stream().noneMatch(x -> x.species() == CritterSpecies.TYLOMELANIA),
                    "switched off: not in the net");
            TankBlockEntity c = seaTank(h, 1);
            TankData t = c.getTank();
            DosingPumpBlockEntity p = pump(h, new BlockPos(3, 2, 1));
            p.items().setStackInSlot(0, new ItemStack(ModItems.CALCIUM_CANISTER.get()));
            float ca = t.calcium(), kh = t.kh();
            p.step(1.0);
            h.assertTrue(p.status() == DosingPumpBlockEntity.OFF && t.calcium() == ca, "the pump does not dose");
            Corals.useCalcium(t);
            h.assertTrue(t.kh() == kh, "the corals use no alkalinity");
            t.setKh(1F);
            h.assertTrue(ReefDosing.stonyFactor(t) == 1, "and it does not slow them");
        } finally {
            CommonConfig.FEATURE_CARIDINA.set(car);
            CommonConfig.FEATURE_SULAWESI.set(sul);
            CommonConfig.FEATURE_REEF_DOSING.set(dos);
        }
        h.succeed();
    }
}
