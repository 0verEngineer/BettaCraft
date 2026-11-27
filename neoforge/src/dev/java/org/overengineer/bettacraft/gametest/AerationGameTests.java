package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.AirPumpBlock;
import org.overengineer.bettacraft.equipment.AirPumpBlockEntity;
import org.overengineer.bettacraft.equipment.DeviceSegmentBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.FilterHost;
import org.overengineer.bettacraft.equipment.HighPowerLedBlock;
import org.overengineer.bettacraft.equipment.LedPanelBlock;
import org.overengineer.bettacraft.equipment.Regulator;
import org.overengineer.bettacraft.equipment.SegmentBlock;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.equipment.TankLightItem;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantColors;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModSounds;
import org.overengineer.bettacraft.reef.TankReef;
import org.overengineer.bettacraft.tank.TankAeration;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankLighting;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankPower;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M23 GameTests: the oxygen model in a real tank (stocking and warmth lower it, air stones raise it, stress), the air
 * pump with and without power, the power regulator (filters, flow pumps, air stones, the server side of the wheel and
 * screen), the internal filters (sponge filter needs air; motor filters suck in fry unless an intake sponge is fitted),
 * the LED tiers (panel placed in pairs, joining, dyeing, light strength, redder plants, strong-light plants), the
 * daylight timer and sounds.useVanilla. Deterministic: no random outcome is waited for (the fry intake runs with a
 * share far above 1 per day, so every fry goes in the first step).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AerationGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private AerationGameTests() {}

    // ------------------------------------------------------------------ helpers

    static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz) {
        for (int a = 0; a < sx; a++)
            for (int b = 0; b < sy; b++)
                for (int c = 0; c < sz; c++) {
                    BlockPos p = new BlockPos(x + a, y + b, z + c);
                    h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
                }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(x, y, z)));
        if (c == null) throw new GameTestAssertException("tank did not form");
        TankInteractions.changeWater(h.getLevel(), c, c.getTank().capacityMb(), 0F);
        c.getTank().setWaterTemp(25F);
        c.getTank().setTargetTemp(25F);
        c.getTank().setWaterQuality(1F);
        return c;
    }

    static InteriorRegion region(GameTestHelper h, TankBlockEntity c) {
        if (InteriorManager.interiorLevel(h.getLevel().getServer()) == null) throw new GameTestAssertException("no interior dimension");
        return InteriorManager.ensureRegion(h.getLevel(), c);
    }

    static ServerLevel interior(GameTestHelper h) {
        return InteriorManager.interiorLevel(h.getLevel().getServer());
    }

    /** Sets a block in the interior at local coordinates; returns its world position. */
    static BlockPos put(GameTestHelper h, InteriorRegion r, int x, int y, int z, BlockState s) {
        BlockPos p = r.toWorld(x, y, z);
        interior(h).setBlock(p, s, Block.UPDATE_ALL);
        return p;
    }

    static BlockState anchor(Block b) {
        return b.defaultBlockState().setValue(SegmentBlock.PART, SegmentBlock.Part.ANCHOR);
    }

    static AirPumpBlockEntity pump(GameTestHelper h, TankBlockEntity c, BlockPos rel) {
        h.setBlock(rel, ModBlocks.AIR_PUMP.get().defaultBlockState().setValue(AirPumpBlock.FACING, Direction.NORTH));
        TankEquipment.invalidate(c);
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof AirPumpBlockEntity p)) throw new GameTestAssertException("no air pump");
        return p;
    }

    static TankmateEntity add(GameTestHelper h, TankBlockEntity c, TankmateSpecies s, LifeStage stage) {
        var w = new org.overengineer.bettacraft.tank.TankWaterVolume(c.getTank());
        Vec3 p = w.clamp(new Vec3((w.box().minX + w.box().maxX) / 2, w.box().minY + 0.4, (w.box().minZ + w.box().maxZ) / 2), 0.35, 0.1, 0.2);
        TankmateEntity e = TankmateBreeding.spawn(h.getLevel(), c, s, TankmateGenes.wild(s), false, stage, 0, p);
        if (e == null) throw new GameTestAssertException("could not spawn " + s.id());
        e.health().feed();
        return e;
    }

    static boolean logged(TankBlockEntity c, String key) {
        return c.getTank().log().stream().anyMatch(e -> e.key().equals(key));
    }

    static boolean active(GameTestHelper h, BlockPos worldPos) {
        BlockState s = interior(h).getBlockState(worldPos);
        return s.hasProperty(DeviceSegmentBlock.ACTIVE) && s.getValue(DeviceSegmentBlock.ACTIVE);
    }

    // ------------------------------------------------------------------ oxygen

    /**
     * Many fish in a small, still tank use more oxygen than the surface brings in: the oxygen falls below the stress limit
     * (a tetra is stressed, a betta's labyrinth limit is lower); warm water holds less. An air pump with two air stones
     * brings it back up; the stones bubble (ACTIVE) only while the pump drives them.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m23_oxygen", timeoutTicks = 300)
    public static void oxygenFallsWithStockingAndAirStonesRaiseIt(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        double savedUse = ServerConfig.OXYGEN_PER_ANIMAL.get();
        try {
            ServerConfig.OXYGEN_PER_ANIMAL.set(40.0);
            TankBlockEntity c = tank(h, 2, 2, 2, 3, 2, 2);
            TankBlockEntity warm = tank(h, 8, 2, 2, 3, 2, 2);
            warm.getTank().setWaterTemp(31F);
            h.assertTrue(TankAeration.saturation(31, 0) < TankAeration.saturation(25, 0), "warm water holds less oxygen");
            for (int i = 0; i < 4; i++) add(h, c, TankmateSpecies.PLATY, LifeStage.ADULT);
            InteriorRegion r = region(h, c);
            // first step: starts at saturation
            TankAeration.step(level, c, 0.05);
            float start = c.getTank().oxygen();
            h.assertTrue(Math.abs(start - TankAeration.saturation(25, 0)) < 0.5, "starts saturated: " + start);
            for (int i = 0; i < 10; i++) TankAeration.step(level, c, 0.3);
            float low = c.getTank().oxygen();
            h.assertTrue(low < ServerConfig.OXYGEN_STRESS_BELOW.get(), "crowded still tank: oxygen falls below the stress limit: " + low);
            h.assertTrue(TankAeration.lowFor(c.getTank(), false), "a tetra or platy is stressed");
            h.assertTrue(logged(c, "log.bettacraft.oxygen_low") || logged(c, "log.bettacraft.oxygen_critical"), "logged");
            c.getTank().setOxygen(2.0F);
            h.assertTrue(!TankAeration.lowFor(c.getTank(), true), "a betta (labyrinth fish) hardly minds 2 mg/L");
            c.getTank().setOxygen(0.5F);
            h.assertTrue(TankAeration.lowFor(c.getTank(), true), "but 0.5 mg/L stresses it too");
            c.getTank().setOxygen(low);
            // two air stones without a pump: nothing bubbles
            BlockPos s1 = put(h, r, 4, 0, 4, ModBlocks.AIR_STONE.get().defaultBlockState());
            BlockPos s2 = put(h, r, 20, 0, 10, ModBlocks.AIR_STONE.get().defaultBlockState());
            TankAeration.step(level, c, 0.1);
            h.assertTrue(!active(h, s1) && !active(h, s2), "no pump: the stones stay still");
            double before = c.o2Exchange;
            AirPumpBlockEntity p = pump(h, c, new BlockPos(2, 2, 1));
            TankAeration.step(level, c, 0.1);
            h.assertTrue(active(h, s1) && active(h, s2), "the pump drives both stones");
            h.assertTrue(p.running() && p.driven() == 2, "pump runs and drives 2: " + p.driven());
            h.assertTrue(c.o2Exchange > before * 2, "much more exchange: " + before + " -> " + c.o2Exchange);
            for (int i = 0; i < 10; i++) TankAeration.step(level, c, 0.3);
            h.assertTrue(c.getTank().oxygen() > low + 1.5 && c.getTank().oxygen() >= ServerConfig.OXYGEN_STRESS_BELOW.get(),
                    "air stones raise the oxygen: " + low + " -> " + c.getTank().oxygen());
            h.assertTrue(TankAeration.co2Stripping(c.o2Exchange) > 0, "and drive out CO2 (M25 hook)");
            // the pump at 0 %: the stones stop
            p.setPower(0);
            TankAeration.step(level, c, 0.1);
            h.assertTrue(!active(h, s1) && !p.running(), "pump at 0 %: still");
        } finally {
            ServerConfig.OXYGEN_PER_ANIMAL.set(savedUse);
        }
        h.succeed();
    }

    /** features.waterChemistry.oxygen off: no value changes, nobody is stressed. */
    @GameTest(template = EMPTY, batch = "bettacraft_m23_oxygen_off", timeoutTicks = 200)
    public static void oxygenFeatureOff(GameTestHelper h) {
        boolean saved = Features.own(Feature.OXYGEN);
        try {
            Features.set(Feature.OXYGEN, false);
            TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 2);
            c.getTank().setOxygen(0.5F);
            TankAeration.step(h.getLevel(), c, 1.0);
            h.assertTrue(c.getTank().oxygen() == 0.5F, "the value stands still");
            h.assertTrue(!TankAeration.lowFor(c.getTank(), false), "nobody is stressed");
        } finally {
            Features.set(Feature.OXYGEN, saved);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ air pump and power

    /** Without power costs the pump simply runs; with power.costs HARD it needs 2 FE/t per air stone. */
    @GameTest(template = EMPTY, batch = "bettacraft_m23_power", timeoutTicks = 200)
    public static void airPumpWithAndWithoutPower(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        PowerSettings saved = PowerSettings.current();
        try {
            TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 2);
            InteriorRegion r = region(h, c);
            BlockPos s1 = put(h, r, 4, 0, 4, ModBlocks.AIR_STONE.get().defaultBlockState());
            BlockPos s2 = put(h, r, 10, 0, 4, ModBlocks.AIR_STONE.get().defaultBlockState());
            AirPumpBlockEntity p = pump(h, c, new BlockPos(2, 2, 1));
            PowerSettings.hard(false);
            TankPower.step(level, c, 20);
            TankAeration.step(level, c, 0.05);
            h.assertTrue(c.airPowered() && p.running() && active(h, s1), "no power needed: it runs");
            PowerSettings.hard(true);
            c.getTank().setPowerBuffer(0);
            TankPower.step(level, c, 20);
            TankAeration.step(level, c, 0.05);
            h.assertTrue(!c.airPowered() && !p.running() && !active(h, s1), "no FE: the pump stands still");
            h.assertTrue(Math.abs(TankAeration.airDemand(level, c) - 2) < 1e-6, "two outlets: " + TankAeration.airDemand(level, c));
            c.getTank().setPowerBuffer(100000);
            TankPower.step(level, c, 20);
            TankAeration.step(level, c, 0.05);
            h.assertTrue(c.airPowered() && p.running() && active(h, s2), "with FE it runs again");
            h.assertTrue(c.lastDemand() == 4, "2 FE/t per stone: " + c.lastDemand());
            long before = c.getTank().powerBuffer();
            TankPower.step(level, c, 20);
            h.assertTrue(before - c.getTank().powerBuffer() == 80, "used 4 FE/t x 20 ticks: " + (before - c.getTank().powerBuffer()));
            // a stone at half power needs half
            BlockState st = interior(h).getBlockState(s2);
            interior(h).setBlock(s2, st.setValue(DeviceSegmentBlock.POWER, 5), Block.UPDATE_ALL);
            TankPower.step(level, c, 20);
            h.assertTrue(c.lastDemand() == 3, "1.5 outlets: 3 FE/t: " + c.lastDemand());
        } finally {
            saved.apply();
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ power regulator

    /** Less power cleans and moves less: an external filter at 50 % cleans half; a flow pump at 50 % half the flow; the server side of the regulator. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void regulatorScalesTheEffect(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 2);
        h.setBlock(new BlockPos(2, 2, 4), ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.NORTH));
        TankEquipment.invalidate(c);
        double full = TankWater.filterBlocks(level, c);
        h.assertTrue(full > 0, "the filter cleans");
        FilterHost f = TankEquipment.filters(level, c).getFirst();
        f.setRegulator(5);
        h.assertTrue(Math.abs(TankWater.filterBlocks(level, c) - full / 2) < 1e-6, "at 50 % half: " + TankWater.filterBlocks(level, c));
        h.assertTrue(Math.abs(TankWater.filterLoad(level, c) - 0.5) < 1e-6, "and half the FE load");
        f.setRegulator(0);
        h.assertTrue(TankWater.filterBlocks(level, c) == 0, "at 0 % nothing");
        f.setRegulator(10);
        // flow pump in the interior
        c.getTank().setSalinity(35F);
        InteriorRegion r = region(h, c);
        BlockPos pump = put(h, r, 6, 2, 6, anchor(ModBlocks.PUMP_SEGMENT.get()));
        double flowFull = TankReef.flow(level, c);
        interior(h).setBlock(pump, interior(h).getBlockState(pump).setValue(DeviceSegmentBlock.POWER, 5), Block.UPDATE_ALL);
        double flowHalf = TankReef.flow(level, c);
        h.assertTrue(flowHalf < flowFull - 0.05, "the pump at 50 % moves less: " + flowFull + " -> " + flowHalf);
        // the regulator as the wheel / the screen set it (server side)
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 3, 5))));
        BlockPos filterPos = h.absolutePos(new BlockPos(2, 2, 4));
        h.assertTrue(!QuestAdv.done(sp, "regulator"), "not before");
        Regulator.apply(sp, filterPos, -3, false);
        h.assertTrue(f.regulator() == 7, "wheel down three steps: " + f.regulator());
        QuestAdv.check(h, sp, "regulator"); // M45
        Regulator.apply(sp, filterPos, 4, true);
        h.assertTrue(f.regulator() == 4, "set to 40 %: " + f.regulator());
        Regulator.apply(sp, filterPos, 20, false);
        h.assertTrue(f.regulator() == 10, "never above 100 %");
        // a device in the interior found from any of its blocks
        Regulator.Device d = Regulator.find(interior(h), pump);
        h.assertTrue(d != null && d.power() == 5 && d.sound() == Regulator.Sound.FLOW_PUMP, "flow pump device: " + d);
        h.succeed();
    }

    /** features.power.regulator off: every device works at 100 %. */
    @GameTest(template = EMPTY, batch = "bettacraft_m23_regulator_off", timeoutTicks = 200)
    public static void regulatorFeatureOff(GameTestHelper h) {
        boolean saved = Features.own(Feature.POWER_REGULATOR);
        try {
            TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 2);
            h.setBlock(new BlockPos(2, 2, 4), ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.NORTH));
            TankEquipment.invalidate(c);
            FilterHost f = TankEquipment.filters(h.getLevel(), c).getFirst();
            f.setRegulator(2);
            double low = TankWater.filterBlocks(h.getLevel(), c);
            Features.set(Feature.POWER_REGULATOR, false);
            h.assertTrue(TankWater.filterBlocks(h.getLevel(), c) > low * 4, "off: full strength");
        } finally {
            Features.set(Feature.POWER_REGULATOR, saved);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ internal filters and fry

    /**
     * Fry in three tanks: an internal filter without intake sponge sucks them in (logged), the same filter with an intake
     * sponge (fitted with the item) and a sponge filter on an air pump do not. The sponge filter cleans only with air; the
     * corner filter cleans like a motor filter.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m23_fry", timeoutTicks = 300)
    public static void motorFiltersSuckInFryUnlessProtected(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        double saved = ServerConfig.FILTER_FRY_INTAKE.get();
        try {
            ServerConfig.FILTER_FRY_INTAKE.set(1000.0);
            TankBlockEntity open = tank(h, 2, 2, 2, 2, 2, 2), guarded = tank(h, 8, 2, 2, 2, 2, 2), sponge = tank(h, 14, 2, 2, 2, 2, 2);
            InteriorRegion ro = region(h, open), rg = region(h, guarded), rs = region(h, sponge);
            put(h, ro, 4, 0, 4, anchor(ModBlocks.FILTER_SEGMENT.get()));
            BlockPos gf = put(h, rg, 4, 0, 4, anchor(ModBlocks.FILTER_SEGMENT.get()));
            BlockPos sf = put(h, rs, 4, 0, 4, anchor(ModBlocks.SPONGE_FILTER_SEGMENT.get()));
            // the intake sponge fitted like a player does it
            ServerPlayer sp = MockPlayers.serverPlayer(h);
            ItemStack spongeItem = new ItemStack(ModItems.INTAKE_SPONGE.get());
            sp.setItemInHand(InteractionHand.MAIN_HAND, spongeItem);
            spongeItem.useOn(new UseOnContext(interior(h), sp, InteractionHand.MAIN_HAND, spongeItem,
                    new BlockHitResult(Vec3.atCenterOf(gf), Direction.UP, gf, false)));
            h.assertTrue(interior(h).getBlockState(gf).getValue(DeviceSegmentBlock.SPONGE), "intake sponge fitted");
            // sponge filter without air: no cleaning
            double dry = TankWater.filterBlocks(level, sponge);
            h.assertTrue(dry == 0, "sponge filter without an air pump does not clean: " + dry);
            pump(h, sponge, new BlockPos(14, 2, 1));
            TankAeration.step(level, sponge, 0.01);
            h.assertTrue(active(h, sf), "the sponge filter gets air");
            h.assertTrue(TankWater.filterBlocks(level, sponge) > 0, "and cleans");
            int[] fry = new int[3];
            List<TankBlockEntity> tanks = List.of(open, guarded, sponge);
            for (TankBlockEntity c : tanks) for (int i = 0; i < 3; i++) add(h, c, TankmateSpecies.GUPPY, LifeStage.FRY);
            for (TankBlockEntity c : tanks) TankAeration.step(level, c, 0.5);
            for (int i = 0; i < 3; i++) {
                int n = 0;
                for (Entity e : TankInhabitants.liveInhabitants(level, tanks.get(i).getTank())) if (e instanceof TankmateEntity m && m.lifeStage() == LifeStage.FRY) n++;
                fry[i] = n;
            }
            h.assertTrue(fry[0] == 0, "the open filter sucked in every fry: " + fry[0] + " left");
            h.assertTrue(logged(open, "log.bettacraft.fry_intake"), "logged");
            h.assertTrue(fry[1] == 3, "intake sponge: all fry left: " + fry[1]);
            h.assertTrue(fry[2] == 3, "sponge filter: all fry left: " + fry[2]);
            // a corner filter cleans like a motor filter
            BlockPos cf = put(h, rg, 10, 0, 10, anchor(ModBlocks.CORNER_FILTER_SEGMENT.get()));
            h.assertTrue(TankWater.cornerFilters(level, guarded.getTank()) == 1, "corner filter counted");
            h.assertTrue(TankWater.filterBlocks(level, guarded) >= ServerConfig.FILTER_CORNER_BLOCKS.get() + ServerConfig.WATER_QUALITY_INTERNAL_FILTER_BLOCKS.get() - 1e-6,
                    "corner + internal filter clean: " + TankWater.filterBlocks(level, guarded));
            h.assertTrue(interior(h).getBlockState(cf).getValue(DeviceSegmentBlock.POWER) == 10, "placed at 100 %");
        } finally {
            ServerConfig.FILTER_FRY_INTAKE.set(saved);
        }
        h.succeed();
    }

    /**
     * M66: an unprotected motor filter also sucks in young shrimp and the tadpoles of the dwarf clawed frogs, but never snails and
     * never an adult shrimp; with an intake sponge nothing goes.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m23_fry", timeoutTicks = 300)
    public static void motorFiltersAlsoSuckInYoungShrimpAndTadpoles(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        double saved = ServerConfig.FILTER_FRY_INTAKE.get();
        try {
            ServerConfig.FILTER_FRY_INTAKE.set(1000.0);
            TankBlockEntity open = tank(h, 2, 2, 2, 2, 2, 2), guarded = tank(h, 8, 2, 2, 2, 2, 2);
            InteriorRegion ro = region(h, open), rg = region(h, guarded);
            put(h, ro, 4, 0, 4, anchor(ModBlocks.FILTER_SEGMENT.get()));
            BlockPos gf = put(h, rg, 4, 0, 4, anchor(ModBlocks.FILTER_SEGMENT.get()));
            ServerPlayer sp = MockPlayers.serverPlayer(h);
            ItemStack spongeItem = new ItemStack(ModItems.INTAKE_SPONGE.get());
            sp.setItemInHand(InteractionHand.MAIN_HAND, spongeItem);
            spongeItem.useOn(new UseOnContext(interior(h), sp, InteractionHand.MAIN_HAND, spongeItem,
                    new BlockHitResult(Vec3.atCenterOf(gf), Direction.UP, gf, false)));
            for (TankBlockEntity c : List.of(open, guarded)) {
                var t = c.getTank();
                var young = new org.overengineer.bettacraft.critters.Critter(t.nextCritterId(), org.overengineer.bettacraft.critters.CritterSpecies.NEOCARIDINA, false,
                        org.overengineer.bettacraft.critters.CritterGenes.pure(0, 0.8F), 1, 1, 1);
                young.age = 0;
                t.critters().add(young);
                var adult = new org.overengineer.bettacraft.critters.Critter(t.nextCritterId(), org.overengineer.bettacraft.critters.CritterSpecies.NEOCARIDINA, true,
                        org.overengineer.bettacraft.critters.CritterGenes.pure(0, 0.8F), 1, 1, 1);
                adult.age = 1000;
                t.critters().add(adult);
                var snail = new org.overengineer.bettacraft.critters.Critter(t.nextCritterId(), org.overengineer.bettacraft.critters.CritterSpecies.RAMSHORN, true,
                        org.overengineer.bettacraft.critters.CritterGenes.pure(0, 0.8F), 1, 1, 1);
                snail.age = 0;
                t.critters().add(snail);
                t.tadpoles().add(new org.overengineer.bettacraft.entity.frog.Tadpole(t.nextFrogBroodId(), 1, 1, 1));
                TankAeration.step(level, c, 0.5);
            }
            var o = open.getTank();
            long youngShrimp = o.critters().stream().filter(k -> k.species == org.overengineer.bettacraft.critters.CritterSpecies.NEOCARIDINA && !k.adult()).count();
            h.assertTrue(youngShrimp == 0, "the open filter sucked in the young shrimp: " + youngShrimp);
            h.assertTrue(o.tadpoles().isEmpty(), "and the tadpole: " + o.tadpoles().size());
            h.assertTrue(o.critters().stream().anyMatch(k -> k.species == org.overengineer.bettacraft.critters.CritterSpecies.NEOCARIDINA && k.adult()), "the adult shrimp stays");
            h.assertTrue(o.critters().stream().anyMatch(k -> k.species == org.overengineer.bettacraft.critters.CritterSpecies.RAMSHORN), "the snail stays");
            var g = guarded.getTank();
            h.assertTrue(g.critters().size() == 3 && g.tadpoles().size() == 1, "with an intake sponge nothing goes: " + g.critters().size() + " / " + g.tadpoles().size());
        } finally {
            ServerConfig.FILTER_FRY_INTAKE.set(saved);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ LED tiers

    /**
     * One LED panel item lays two halves; panels side by side join, a dye colours the whole panel. Light strength: bar 15,
     * panel more, high-power 20; under strong light red plants reach the deepest red and the strong-light plants grow at
     * full speed (under the bar slower).
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void ledTiersPlaceJoinAndShine(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        // the panel item places both halves
        h.setBlock(new BlockPos(20, 1, 20), Blocks.STONE.defaultBlockState());
        h.setBlock(new BlockPos(21, 1, 20), Blocks.STONE.defaultBlockState());
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.setYRot(180); // facing north
        BlockPos stone = h.absolutePos(new BlockPos(20, 1, 20));
        ItemStack red = TankLightItem.of(ModItems.LED_PANEL.get(), DyeColor.RED, 2);
        sp.setItemInHand(InteractionHand.MAIN_HAND, red);
        red.useOn(new UseOnContext(sp, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(stone).add(0, 0.5, 0), Direction.UP, stone, false)));
        BlockState a = level.getBlockState(stone.above());
        h.assertTrue(a.getBlock() instanceof LedPanelBlock && a.getValue(TankLightBlock.COLOR) == DyeColor.RED, "panel placed red: " + a);
        BlockPos partner = stone.above().relative(LedPanelBlock.partnerDir(a));
        BlockState b = level.getBlockState(partner);
        h.assertTrue(b.getBlock() instanceof LedPanelBlock && b.getValue(TankLightBlock.COLOR) == DyeColor.RED, "second half placed: " + b);
        BlockPos stone2 = stone.east();
        red.useOn(new UseOnContext(sp, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(stone2).add(0, 0.5, 0), Direction.UP, stone2, false)));
        BlockState a2 = level.getBlockState(stone.above());
        h.assertTrue(a2.getValue(TankLightBlock.POS) || a2.getValue(TankLightBlock.NEG), "panels join: " + a2);
        h.assertTrue(TankLightBlock.bar(level, stone.above(), a2).size() == 4, "one panel of 4 blocks: " + TankLightBlock.bar(level, stone.above(), a2).size());
        int dyed = TankLightBlock.dyeBar(level, stone.above(), a2, DyeColor.BLUE, false);
        h.assertTrue(dyed == 4, "a dye colours the whole panel: " + dyed);
        // breaking one half removes the other
        level.destroyBlock(partner, false);
        h.assertTrue(level.getBlockState(stone.above()).isAir() || !(level.getBlockState(stone.above()).getBlock() instanceof LedPanelBlock)
                || level.getBlockState(stone.above()).getBlock() instanceof LedPanelBlock && level.getBlockState(stone.above().relative(
                LedPanelBlock.partnerDir(level.getBlockState(stone.above())))).getBlock() instanceof LedPanelBlock, "a half never stays alone");

        // light strength of the tiers
        TankBlockEntity bar = tank(h, 2, 2, 2, 2, 2, 2), panel = tank(h, 8, 2, 2, 2, 2, 2), hp = tank(h, 14, 2, 2, 2, 2, 2);
        for (int x = 0; x < 2; x++)
            for (int z = 0; z < 2; z++) {
                h.setBlock(new BlockPos(2 + x, 4, 2 + z), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true));
                h.setBlock(new BlockPos(8 + x, 4, 2 + z), ModBlocks.LED_PANEL.get().defaultBlockState().setValue(TankLightBlock.LIT, true)
                        .setValue(LedPanelBlock.PARTNER_POS, z == 0)); // the two halves face each other
                h.setBlock(new BlockPos(14 + x, 4, 2 + z), ModBlocks.HIGH_POWER_LED.get().defaultBlockState().setValue(TankLightBlock.LIT, true));
            }
        double lb = TankPlants.strongLight(level, bar), lp = TankPlants.strongLight(level, panel), lh = TankPlants.strongLight(level, hp);
        h.assertTrue(Math.abs(lb - 15) < 1e-6, "bar: 15: " + lb);
        h.assertTrue(Math.abs(lp - 17.5) < 1e-6, "panel: 17.5: " + lp);
        h.assertTrue(Math.abs(lh - 20) < 1e-6, "high-power: 20: " + lh);
        h.assertTrue(TankPlants.light(level, hp) == 15, "the 0..15 light stays 15 for everything else");
        h.assertTrue(PlantColors.tintFor((int) Math.round(lh)) == 3 && PlantColors.tintFor((int) Math.round(lb)) == 2, "red plants redder under strong light");
        double slow = InteriorPlants.lightFactor((int) Math.round(lb), PlantSpecies.Light.need(PlantSpecies.MONTE_CARLO.light()));
        double fast = InteriorPlants.lightFactor((int) Math.round(lh), PlantSpecies.Light.need(PlantSpecies.MONTE_CARLO.light()));
        h.assertTrue(fast == 1.0 && slow < 0.7, "Monte Carlo needs strong light: bar " + slow + ", high-power " + fast);
        h.assertTrue(InteriorPlants.lightFactor((int) Math.round(lb), PlantSpecies.Light.need(PlantSpecies.JAVA_FERN.light())) == 1.0, "a java fern is happy under the bar");
        h.assertTrue(TankLighting.algaeBoost(lh) > TankLighting.algaeBoost(lb), "more algae under strong light without CO2");
        h.assertTrue(level.getBlockState(h.absolutePos(new BlockPos(14, 4, 2))).getBlock() instanceof HighPowerLedBlock, "high-power LED");
        h.succeed();
    }

    // ------------------------------------------------------------------ daylight timer

    /** The timer switches the lights of the tank off at night and on by day (dimmed during the ramp and by the dimmer). */
    @GameTest(template = EMPTY, batch = "bettacraft_m23_timer", timeoutTicks = 200)
    public static void daylightTimer(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        long savedTime = level.getDayTime();
        TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 2);
        BlockPos l = new BlockPos(2, 4, 2);
        h.setBlock(l, ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true));
        c.getTank().setLightTimer(true);
        c.getTank().setLightOn(8 * 60);
        c.getTank().setLightOff(20 * 60);
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 18000); // midnight
        TankLighting.step(level, c);
        h.runAfterDelay(5, () -> {
            h.assertTrue(!h.getBlockState(l).getValue(TankLightBlock.LIT), "off at midnight");
            h.assertTrue(TankPlants.strongLight(level, c) < 15, "no LED light at night");
            org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000); // noon
            TankLighting.step(level, c);
            h.runAfterDelay(5, () -> {
                h.assertTrue(h.getBlockState(l).getValue(TankLightBlock.LIT) && h.getBlockState(l).getValue(TankLightBlock.DIM) == 0, "on at noon, full");
                c.getTank().setLightDimmer(50);
                TankLighting.step(level, c);
                h.runAfterDelay(5, () -> {
                    h.assertTrue(h.getBlockState(l).getValue(TankLightBlock.DIM) == 2, "dimmer 50 %: dim step 2: " + h.getBlockState(l));
                    h.assertTrue(TankLightBlock.lightLevel(h.getBlockState(l)) == 9, "light level 9");
                    h.assertTrue(Math.abs(TankLighting.ledLight(level, c) - 7.5) < 1e-6, "half the light for the plants: " + TankLighting.ledLight(level, c));
                    org.overengineer.bettacraft.gametest.framework.DevTime.set(level, savedTime);
                    h.succeed();
                });
            });
        });
    }

    // ------------------------------------------------------------------ sounds

    /** sounds.useVanilla switches every own device sound to its Minecraft twin. */
    @GameTest(template = EMPTY, batch = "bettacraft_m23_sounds", timeoutTicks = 100)
    public static void vanillaSoundsSwitch(GameTestHelper h) {
        boolean saved = CommonConfig.SOUNDS_USE_VANILLA.get();
        try {
            CommonConfig.SOUNDS_USE_VANILLA.set(false);
            h.assertTrue(ModSounds.pick(ModSounds.AIR_PUMP_HUM, ModSounds.AIR_PUMP_HUM_VANILLA) == ModSounds.AIR_PUMP_HUM.get(), "own sound");
            h.assertTrue(ModSounds.pick(ModSounds.FILTER_RUNNING, ModSounds.FILTER_RUNNING_VANILLA) == ModSounds.FILTER_RUNNING.get(), "own filter sound");
            CommonConfig.SOUNDS_USE_VANILLA.set(true);
            h.assertTrue(ModSounds.pick(ModSounds.AIR_PUMP_HUM, ModSounds.AIR_PUMP_HUM_VANILLA) == ModSounds.AIR_PUMP_HUM_VANILLA.get(), "vanilla twin");
            h.assertTrue(ModSounds.pick(ModSounds.FEEDER_CLICK, ModSounds.FEEDER_CLICK_VANILLA) == ModSounds.FEEDER_CLICK_VANILLA.get(), "vanilla click");
            // every own event has a twin
            for (var e : net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.keySet()) {
                if (!e.getNamespace().equals(BettaCraft.MOD_ID) || e.getPath().endsWith("_vanilla") || e.getPath().startsWith("block.tank.")) continue;
                h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(BettaCraft.id(e.getPath() + "_vanilla")), "twin of " + e);
            }
        } finally {
            CommonConfig.SOUNDS_USE_VANILLA.set(saved);
        }
        h.succeed();
    }

    /** Filters of earlier versions (no regulator in their state) load at 100 %, without sponge. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void oldDeviceStatesRunFully(GameTestHelper h) {
        BlockState f = ModBlocks.FILTER_SEGMENT.get().defaultBlockState();
        h.assertTrue(f.getValue(DeviceSegmentBlock.POWER) == 10 && !f.getValue(DeviceSegmentBlock.SPONGE), "default: 100 %, no sponge");
        h.assertTrue(ModBlocks.TANK_LIGHT.get().defaultBlockState().getValue(TankLightBlock.DIM) == 0, "lights: not dimmed");
        h.assertTrue(TankLightBlock.lightLevel(ModBlocks.TANK_LIGHT.get().defaultBlockState()) == 15, "light 15");
        h.succeed();
    }
}
