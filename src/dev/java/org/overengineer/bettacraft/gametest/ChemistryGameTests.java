package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.chem.ChemItem;
import org.overengineer.bettacraft.chem.ChemTolerance;
import org.overengineer.bettacraft.chem.TankChemistry;
import org.overengineer.bettacraft.chem.WaterChemistry;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateCare;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.WaterValues;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M25 GameTests (part a, water chemistry): the substrate, limestone rocks and driftwood of the interior move the pH, the
 * TDS rises with the stocking, water changes mix TDS and pH (tap and reverse osmosis water, pipes), tanks of earlier
 * versions start with tap water, the measuring tools and additives, stress of animals out of their range and the feature
 * switch. Deterministic: every care step is called directly.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChemistryGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private ChemistryGameTests() {}

    private static boolean near(double a, double b, double eps) {
        return Math.abs(a - b) <= eps;
    }

    private static void floor(ServerLevel in, InteriorRegion r, BlockState s) {
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), s, false);
    }

    private static ServerPlayer player(GameTestHelper h, ItemStack stack) {
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        sp.getInventory().clearContent();
        sp.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return sp;
    }

    /**
     * Soil on the floor lowers the pH and softens the water, limestone raises it and hardens it, limestone rocks count
     * half; driftwood and almond leaves lower it a little. The counts follow the substrate and blocks as they change.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m25_chem", timeoutTicks = 300)
    public static void substrateMovesThePh(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 1, 2, 1, 2, 2, 2);
        InteriorRegion r = AerationGameTests.region(h, c);
        ServerLevel in = AerationGameTests.interior(h);
        TankData t = c.getTank();
        var eq = InteriorManager.equipment(level.getServer(), r);
        h.assertTrue(eq.floorBlocks() == r.sizeX() * r.sizeZ() && eq.acidFloor() == 0, "floor counted: " + eq.floorBlocks());
        h.assertTrue(near(t.tds(), WaterChemistry.TAP_TDS, 0.01) && near(t.ph(), WaterChemistry.TAP_PH, 0.01), "a new tank holds tap water");
        // sand: nothing changes
        TankChemistry.step(level, c, 5);
        h.assertTrue(near(t.ph(), WaterChemistry.TAP_PH, 0.01), "sand keeps the pH: " + t.ph());
        floor(in, r, Blocks.DIRT.defaultBlockState());
        h.assertTrue(eq.acidFloor() == eq.floorBlocks(), "soil counted: " + eq.acidFloor());
        var d = TankChemistry.drivers(level, c);
        // M25 part d: plain soil counts half; active soil fully
        h.assertTrue(near(d.acid(), TankChemistry.DIRT_SHARE, 1e-6) && d.activeSoil() == 0, "plain soil share " + d.acid());
        TankChemistry.step(level, c, 5);
        h.assertTrue(t.ph() < 7.2F && t.ph() > 6.7F, "plain soil lowers the pH a little: " + t.ph());
        h.assertTrue(TankChemistry.hardness(t, d) == WaterChemistry.Hardness.MEDIUM, "plain soil does not make it soft: " + TankChemistry.hardness(t, d));
        h.assertTrue(Substrate.isValid(org.overengineer.bettacraft.registry.ModBlocks.ACTIVE_SOIL.get()), "active soil is a substrate");
        floor(in, r, org.overengineer.bettacraft.registry.ModBlocks.ACTIVE_SOIL.get().defaultBlockState());
        h.assertTrue(eq.activeSoil() == eq.floorBlocks() && eq.acidFloor() == 0, "active soil counted: " + eq.activeSoil());
        d = TankChemistry.drivers(level, c);
        h.assertTrue(near(d.acid(), 1, 1e-6) && near(d.activeSoil(), 1, 1e-6), "active soil share " + d.acid());
        h.assertTrue(near(org.overengineer.bettacraft.plants.InteriorPlants.activeSoilBoost(level, r), org.overengineer.bettacraft.plants.InteriorPlants.ACTIVE_SOIL_BOOST, 1e-6),
                "active soil feeds the plants");
        TankChemistry.step(level, c, 5);
        h.assertTrue(t.ph() < 6.6F && t.ph() > 6.2F, "active soil lowers the pH: " + t.ph());
        h.assertTrue(TankChemistry.hardness(t, d) == WaterChemistry.Hardness.SOFT, "active soil softens: " + TankChemistry.hardness(t, d));
        // it does not wear out: a year later the same pull (the TDS it takes up only makes the water softer)
        float tds = t.tds();
        TankChemistry.step(level, c, 365);
        d = TankChemistry.drivers(level, c);
        h.assertTrue(near(d.acid(), 1, 1e-6) && t.ph() < 6.6F && t.tds() < tds, "a year later: pH " + t.ph() + ", TDS " + t.tds());
        // a rescan (regions of earlier versions) counts it the same
        eq.markNeedsScan();
        eq = InteriorManager.equipment(level.getServer(), r);
        h.assertTrue(eq.activeSoil() == eq.floorBlocks(), "rescan active soil: " + eq.activeSoil());
        t.setTds(WaterChemistry.TAP_TDS); // a water change back to normal water for the limestone part
        floor(in, r, Blocks.CALCITE.defaultBlockState());
        h.assertTrue(eq.acidFloor() == 0 && eq.limeFloor() == eq.floorBlocks(), "limestone counted, soil gone: " + eq.limeFloor());
        TankChemistry.step(level, c, 5);
        h.assertTrue(t.ph() > 7.8F, "limestone raises the pH: " + t.ph());
        h.assertTrue(TankChemistry.hardness(t, TankChemistry.drivers(level, c)) == WaterChemistry.Hardness.HARD, "limestone hardens");
        // back to sand; rocks and wood in the volume follow the block changes
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) Substrate.clear(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z));
        h.assertTrue(eq.limeFloor() == 0, "cleared: " + eq.limeFloor());
        int placed = 0;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 0; y < 13; y++) {
            AerationGameTests.put(h, r, x, y, z, Blocks.OAK_LOG.defaultBlockState());
            placed++;
        }
        h.assertTrue(eq.driftwood() == placed, "driftwood counted: " + eq.driftwood() + " of " + placed);
        d = TankChemistry.drivers(level, c);
        h.assertTrue(d.wood() > 0.3, "wood share " + d.wood());
        t.setPh(7.5F);
        TankChemistry.step(level, c, 5);
        h.assertTrue(t.ph() < 7.45F && t.ph() > 7.0F, "driftwood lowers the pH a little: " + t.ph());
        // a rescan (regions of earlier versions) counts the same
        int wood = eq.driftwood();
        eq.markNeedsScan();
        eq = InteriorManager.equipment(level.getServer(), r);
        h.assertTrue(eq.driftwood() == wood && eq.floorBlocks() == r.sizeX() * r.sizeZ(), "rescan: " + eq.driftwood());
        h.succeed();
    }

    /**
     * The TDS rises with the stocking; water changes with tap water bring TDS and pH back by volume (the pH as acid,
     * logarithmic), reverse osmosis water softens; pipes carry TDS and pH; evaporation leaves the minerals behind.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m25_chem", timeoutTicks = 200)
    public static void waterChangesMixTheValues(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 1, 2, 1, 2, 2, 2);
        TankData t = c.getTank();
        for (int i = 0; i < 4; i++) AerationGameTests.add(h, c, TankmateSpecies.PLATY, LifeStage.ADULT);
        var d = TankChemistry.drivers(level, c);
        double rate = TankChemistry.tdsPerDay(level, c, d);
        h.assertTrue(rate > 0, "fish raise the TDS: " + rate);
        TankChemistry.step(level, c, 10);
        h.assertTrue(near(t.tds(), WaterChemistry.TAP_TDS + rate * 10, 0.5), "10 days: " + t.tds());
        // a quarter of tap water into soft acidic water
        t.setTds(0F);
        t.setPh(6.0F);
        long quarter = t.capacityMb() / 4;
        TankInteractions.changeWater(level, c, -quarter);
        TankInteractions.changeWater(level, c, quarter);
        h.assertTrue(near(t.tds(), WaterChemistry.TAP_TDS / 4, 0.5), "TDS by volume: " + t.tds());
        h.assertTrue(near(t.ph(), WaterChemistry.mixPh(6.0F, 3, WaterChemistry.TAP_PH, 1), 0.01) && t.ph() < 6.2F, "pH as acid: " + t.ph());
        // reverse osmosis water (part b makes it) softens
        t.setTds(400F);
        TankInteractions.changeWater(level, c, -quarter);
        TankInteractions.addWater(level, c, quarter, WaterValues.ro(25F));
        h.assertTrue(near(t.tds(), 300, 0.5), "RO water: " + t.tds());
        // the values travel with the water; older water without them is tap water
        WaterValues v = WaterValues.of(t);
        h.assertTrue(near(v.tds(), 300, 0.5) && near(v.ph(), t.ph(), 0.01), "carried: " + v);
        var json = WaterValues.CODEC.encodeStart(JsonOps.INSTANCE, new WaterValues(25, 0, 0, 1, 0)).getOrThrow().getAsJsonObject();
        json.remove("tds");
        json.remove("ph");
        WaterValues old = WaterValues.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        h.assertTrue(old.tds() == WaterChemistry.TAP_TDS && old.ph() == WaterChemistry.TAP_PH, "old water is tap water: " + old);
        // evaporation: the minerals stay
        float before = t.tds();
        long water = t.waterMb();
        float after = WaterChemistry.concentrate(before, water, water / 4, WaterChemistry.TDS_MAX);
        h.assertTrue(after > before, "concentrated");
        h.succeed();
    }

    /** Tanks of earlier versions load with tap water (TDS 250, pH 7.5, no tannins, the air's CO2); joining tanks mix. */
    @GameTest(template = EMPTY, batch = "bettacraft_m25_chem", timeoutTicks = 100)
    public static void oldTanksStartWithTapWater(GameTestHelper h) {
        TankBlockEntity c = AerationGameTests.tank(h, 1, 2, 1, 2, 2, 2);
        TankData t = c.getTank();
        t.setTds(80F);
        t.setPh(6.1F);
        t.setTannin(0.5F);
        TankData same = TankData.load(t.save());
        h.assertTrue(same.tds() == 80F && same.ph() == 6.1F && same.tannin() == 0.5F, "saved and loaded");
        CompoundTag old = t.save();
        for (String k : new String[]{"tds", "ph", "tannin", "co2"}) old.remove(k);
        TankData loaded = TankData.load(old);
        h.assertTrue(loaded.tds() == WaterChemistry.TAP_TDS && loaded.ph() == WaterChemistry.TAP_PH && loaded.tannin() == 0
                && loaded.co2() == WaterChemistry.CO2_AIR, "an old tank holds tap water");
        // a tank of tap water joined to this one through a tank block: TDS by volume, pH as acid
        AerationGameTests.tank(h, 4, 2, 1, 2, 2, 2);
        t.setTds(0F);
        t.setPh(6.0F);
        h.setBlock(new BlockPos(3, 2, 1), org.overengineer.bettacraft.registry.ModBlocks.TANK.get().defaultBlockState());
        TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(new BlockPos(3, 2, 1)), null);
        TankBlockEntity merged = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(1, 2, 1)));
        h.assertTrue(merged != null && merged == TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(5, 2, 1))), "joined");
        TankData m = merged.getTank();
        h.assertTrue(near(m.tds(), WaterChemistry.TAP_TDS / 2, 1) && near(m.ph(), WaterChemistry.mixPh(6.0F, 1, WaterChemistry.TAP_PH, 1), 0.02),
                "mixed: " + m.tds() + " " + m.ph());
        h.succeed();
    }

    /** The TDS meter and pH drop test measure (the drop test wears), remineralising salt raises the TDS, almond leaves add tannins. */
    @GameTest(template = EMPTY, batch = "bettacraft_m25_chem", timeoutTicks = 100)
    public static void measuringAndAdditives(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 1, 2, 1, 2, 2, 2);
        TankData t = c.getTank();
        BlockPos pos = c.getBlockPos();
        ItemStack meter = new ItemStack(ModItems.TDS_METER.get());
        ServerPlayer sp = player(h, meter);
        ChemItem.useOnTank(meter, level, pos, sp, InteractionHand.MAIN_HAND);
        h.assertTrue(meter.getCount() == 1 && meter.getDamageValue() == 0 && t.tds() == WaterChemistry.TAP_TDS, "the meter only measures");
        ItemStack kit = new ItemStack(ModItems.PH_TEST_KIT.get());
        sp.setItemInHand(InteractionHand.MAIN_HAND, kit);
        ChemItem.useOnTank(kit, level, pos, sp, InteractionHand.MAIN_HAND);
        h.assertTrue(kit.getDamageValue() == 1, "a drop test is used: " + kit.getDamageValue());
        // reverse osmosis water, then remineralising salt (sneaking: up to 150 ppm)
        t.setTds(0F);
        ItemStack salt = new ItemStack(ModItems.REMINERALISING_SALT.get(), 64);
        sp.setItemInHand(InteractionHand.MAIN_HAND, salt);
        ChemItem.useOnTank(salt, level, pos, sp, InteractionHand.MAIN_HAND);
        float one = t.tds();
        h.assertTrue(near(one, 400.0 * 1000 / t.waterMb(), 0.5) && salt.getCount() == 63, "one portion: " + one);
        sp.setShiftKeyDown(true);
        ChemItem.useOnTank(salt, level, pos, sp, InteractionHand.MAIN_HAND);
        h.assertTrue(t.tds() >= TankChemistry.REMINERALISE_TARGET && t.tds() < TankChemistry.REMINERALISE_TARGET + 50, "sneaking: " + t.tds());
        int left = salt.getCount();
        ChemItem.useOnTank(salt, level, pos, sp, InteractionHand.MAIN_HAND);
        h.assertTrue(salt.getCount() == left, "hard enough: nothing used");
        sp.setShiftKeyDown(false);
        ItemStack leaf = new ItemStack(ModItems.ALMOND_LEAF.get(), 4);
        sp.setItemInHand(InteractionHand.MAIN_HAND, leaf);
        ChemItem.useOnTank(leaf, level, pos, sp, InteractionHand.MAIN_HAND);
        h.assertTrue(near(t.tannin(), 1000.0 / t.waterMb(), 1e-3) && leaf.getCount() == 3, "a leaf: " + t.tannin());
        float ph = t.ph();
        TankChemistry.step(level, c, 1);
        h.assertTrue(t.ph() < ph && t.tannin() < 1000F / t.waterMb(), "tannins lower the pH and fade: " + t.ph() + " " + t.tannin());
        h.succeed();
    }

    /**
     * Livebearers in very soft, acidic water: stressed, then the water counts as bad for them (sick); soft-water fish in
     * the same water are fine. A little out of the range only stresses.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m25_chem", timeoutTicks = 200)
    public static void animalsOutOfTheirRange(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 1, 2, 1, 2, 2, 2);
        TankData t = c.getTank();
        var platy = AerationGameTests.add(h, c, TankmateSpecies.PLATY, LifeStage.ADULT);
        var neon = AerationGameTests.add(h, c, TankmateSpecies.NEON_TETRA, LifeStage.ADULT);
        TankmateCare.advance(level, c, platy, 0.05);
        TankmateCare.advance(level, c, neon, 0.05);
        h.assertTrue((platy.stressBits() & TankmateCare.STRESS_CHEMISTRY) == 0, "tap water suits the platy");
        // a little too soft: only stress
        t.setTds(80F);
        TankmateCare.advance(level, c, platy, 0.05);
        h.assertTrue((platy.stressBits() & TankmateCare.STRESS_CHEMISTRY) != 0 && platy.health().waterStress() == 0, "a little soft: stressed");
        // reverse osmosis water at pH 6: bad water for the platy, fine for the neon
        t.setTds(0F);
        t.setPh(6.0F);
        h.assertTrue(TankChemistry.bad(t, ChemTolerance.tankmate(TankmateSpecies.PLATY)), "bad for livebearers");
        TankmateCare.advance(level, c, platy, 0.5);
        TankmateCare.advance(level, c, neon, 0.5);
        h.assertTrue(platy.health().waterStress() > 0, "the platy suffers: " + platy.health().waterStress());
        h.assertTrue((neon.stressBits() & TankmateCare.STRESS_CHEMISTRY) == 0 && neon.health().waterStress() == 0, "the neon is fine");
        h.assertTrue(TankChemistry.bad(t, ChemTolerance.BETTA) == false, "pH 6 still suits a betta");
        t.setPh(5.0F);
        h.assertTrue(TankChemistry.bad(t, ChemTolerance.BETTA), "pH 5 does not");
        h.succeed();
    }

    /** features.waterChemistry.enabled off: the values stand still, no animal minds them, the tools say so. */
    @GameTest(template = EMPTY, batch = "bettacraft_m25_chem_off", timeoutTicks = 100)
    public static void featureOff(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        boolean saved = Features.own(Feature.WATER_CHEMISTRY);
        try {
            Features.set(Feature.WATER_CHEMISTRY, false);
            TankBlockEntity c = AerationGameTests.tank(h, 1, 2, 1, 2, 2, 2);
            TankData t = c.getTank();
            var platy = AerationGameTests.add(h, c, TankmateSpecies.PLATY, LifeStage.ADULT);
            t.setTds(0F);
            t.setPh(5.0F);
            t.setTannin(1F);
            TankChemistry.step(level, c, 5);
            h.assertTrue(t.ph() == 5.0F && t.tannin() == 1F, "values stand still");
            TankmateCare.advance(level, c, platy, 0.5);
            h.assertTrue((platy.stressBits() & TankmateCare.STRESS_CHEMISTRY) == 0 && platy.health().waterStress() == 0, "nobody minds");
            ItemStack salt = new ItemStack(ModItems.REMINERALISING_SALT.get(), 4);
            ServerPlayer sp = player(h, salt);
            ChemItem.useOnTank(salt, level, c.getBlockPos(), sp, InteractionHand.MAIN_HAND);
            h.assertTrue(salt.getCount() == 4 && t.tds() == 0F, "the salt is not used");
        } finally {
            Features.set(Feature.WATER_CHEMISTRY, saved);
        }
        h.succeed();
    }
}
