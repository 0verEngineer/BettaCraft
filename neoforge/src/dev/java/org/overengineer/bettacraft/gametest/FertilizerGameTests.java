package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.fertilizer.Fertilizer;
import org.overengineer.bettacraft.fertilizer.FertilizerMakerBlock;
import org.overengineer.bettacraft.fertilizer.FertilizerMakerBlockEntity;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.reef.DosingPumpBlock;
import org.overengineer.bettacraft.reef.DosingPumpBlockEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.WaterValues;
import org.overengineer.bettacraft.tank.algae.TankAlgae;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tank.plants.FloatingPlant;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.overengineer.bettacraft.winding.Winding;

/**
 * M37 GameTests: liquid fertilizer (our plants grow faster, they use it up, water changes dilute it, overdosing feeds the
 * algae, plants of other mods are not touched), the bottle on a tank, the fertilizer maker on its spring and on FE, the
 * pipe into a tank (up to 100 %), the dosing pump with a fertilizer canister and the feature switches. Deterministic:
 * growth and the machines are stepped directly.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FertilizerGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private FertilizerGameTests() {}

    // ------------------------------------------------------------------ helpers

    static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz) {
        return AerationGameTests.tank(h, x, y, z, sx, sy, sz);
    }

    private static void light(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true));
    }

    private static ServerLevel interior(GameTestHelper h) {
        ServerLevel l = InteriorManager.interiorLevel(h.getLevel().getServer());
        if (l == null) throw new GameTestFailure("interior dimension not loaded");
        return l;
    }

    private static void substrate(ServerLevel in, InteriorRegion r) {
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++)
                Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.GRAVEL.defaultBlockState(), false);
    }

    static FertilizerMakerBlockEntity maker(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, ModBlocks.FERTILIZER_MAKER.get().defaultBlockState());
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof FertilizerMakerBlockEntity be)) throw new GameTestFailure("no fertilizer maker");
        return be;
    }

    private static void run(ServerLevel level, FertilizerMakerBlockEntity m, int ticks) {
        for (int i = 0; i < ticks; i++) m.tick(level, m.getBlockPos(), level.getBlockState(m.getBlockPos()));
    }

    /** A player right-clicks {@code pos} with {@code stack} (crank, bottle). */
    static ServerPlayer use(GameTestHelper h, ServerPlayer sp, BlockPos abs, ItemStack stack) {
        sp.setItemInHand(InteractionHand.MAIN_HAND, stack);
        sp.gameMode.useItemOn(sp, h.getLevel(), stack, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false));
        return sp;
    }

    static ServerPlayer player(GameTestHelper h) {
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        return sp;
    }

    static boolean near(double a, double b, double eps) {
        return Math.abs(a - b) <= eps;
    }

    // ------------------------------------------------------------------ the plants

    /**
     * Two lit tanks with a Rotala cutting each: the one at 100 % fertilizer grows half as fast again, the other still grows;
     * the plants use the fertilizer; floating plants grow faster too; a vanilla seagrass in the interior is left alone.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m37_fert", timeoutTicks = 200)
    public static void fertilizerSpeedsUpOurPlantsOnly(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerLevel in = interior(h);
        TankBlockEntity plain = tank(h, 2, 1, 2, 2, 1, 1), fed = tank(h, 2, 1, 8, 2, 1, 1);
        light(h, new BlockPos(2, 2, 2));
        light(h, new BlockPos(2, 2, 8));
        InteriorRegion rp = InteriorManager.ensureRegion(level, plain), rf = InteriorManager.ensureRegion(level, fed);
        substrate(in, rp);
        substrate(in, rf);
        InteriorPlant a = InteriorPlants.plantAt(in, rp, PlantSpecies.ROTALA, 8, 0, 8, 0, 7L);
        InteriorPlant b = InteriorPlants.plantAt(in, rf, PlantSpecies.ROTALA, 8, 0, 8, 0, 7L);
        // a plant of Minecraft in the fertilized interior: our growth never touches it
        BlockPos grass = new BlockPos(rf.originX() + 20, InteriorRegion.FLOOR_Y + 1, rf.originZ() + 8);
        in.setBlock(grass, Blocks.SEAGRASS.defaultBlockState(), 3);
        BlockState grassBefore = in.getBlockState(grass);
        h.assertTrue(a != null && b != null, "cuttings planted");
        for (int i = 0; i < 6; i++) {
            fed.getTank().setFertilizer(1F); // kept at the right level
            InteriorPlants.grow(level, plain, rp, 0.25);
            InteriorPlants.grow(level, fed, rf, 0.25);
        }
        double pa = a.target + a.progress, pb = b.target + b.progress;
        h.assertTrue(pa > 0, "without fertilizer it grows too: " + pa);
        double boost = 1 + ServerConfig.getOr(ServerConfig.FERT_BOOST, 0.5);
        h.assertTrue(near(pb / pa, boost, 0.05), "fertilized: " + pb + " vs " + pa + " (x" + pb / pa + ", want x" + boost + ")");
        h.assertTrue(near(Fertilizer.growth(0.5, 0.5), 1.25, 1e-9) && near(Fertilizer.growth(2.0, 0.5), 1.5, 1e-9), "less helps less, more no more");
        // the plants used some
        fed.getTank().setFertilizer(1F);
        InteriorPlants.grow(level, fed, rf, 1.0);
        h.assertTrue(fed.getTank().fertilizer() < 1F, "the plants use it: " + fed.getTank().fertilizer());
        h.assertTrue(in.getBlockState(grass) == grassBefore && InteriorPlants.speciesOf(grassBefore) == null, "the seagrass is not ours and untouched");
        for (var p : rf.plants().all()) h.assertTrue(p.species == PlantSpecies.ROTALA, "only our plants on the index: " + p.species);
        // floating plants
        FloatingPlant fp = TankPlants.add(plain, FloatingPlantType.SALVINIA, plain.getTank().min().getX() + 1.0, plain.getTank().min().getZ() + 0.5, 0.3F, 0F);
        FloatingPlant ff = TankPlants.add(fed, FloatingPlantType.SALVINIA, fed.getTank().min().getX() + 1.0, fed.getTank().min().getZ() + 0.5, 0.3F, 0F);
        h.assertTrue(fp != null && ff != null, "floating plants placed");
        float s0 = fp.size;
        fed.getTank().setFertilizer(1F);
        TankPlants.grow(level, plain, 0.2);
        TankPlants.grow(level, fed, 0.2);
        h.assertTrue(fp.size > s0 && ff.size - s0 > (fp.size - s0) * 1.2, "floating plants grow faster with it: " + (ff.size - s0) + " vs " + (fp.size - s0));
        h.succeed();
    }

    /** Overdosed from 150 %: the algae grow up to three times as fast; up to the threshold they do not notice it. */
    @GameTest(template = EMPTY, batch = "bettacraft_m37_fert", timeoutTicks = 100)
    public static void overdosingFeedsTheAlgae(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 1, 2);
        light(h, new BlockPos(2, 2, 2));
        light(h, new BlockPos(3, 2, 2));
        h.runAfterDelay(5, () -> {
            TankData t = c.getTank();
            t.setWaterQuality(0.6F);
            t.setFertilizer(0F);
            double g0 = TankAlgae.growthPerDay(level, c);
            h.assertTrue(g0 > 0, "algae grow in this light: " + g0);
            t.setFertilizer(1.5F);
            h.assertTrue(near(TankAlgae.growthPerDay(level, c), g0, 1e-9) && !Fertilizer.overdosed(t), "150 %: not yet overdosed");
            t.setFertilizer(3F);
            double max = ServerConfig.getOr(ServerConfig.FERT_ALGAE, 3.0);
            h.assertTrue(Fertilizer.overdosed(t) && near(TankAlgae.growthPerDay(level, c) / g0, max, 1e-6), "300 %: x" + TankAlgae.growthPerDay(level, c) / g0);
            t.setFertilizer(2.25F);
            h.assertTrue(near(TankAlgae.growthPerDay(level, c) / g0, 1 + (max - 1) * 0.5, 1e-6), "in between: half the way");
            h.succeed();
        });
    }

    /**
     * A bottle on a tank: one portion each (4 water blocks to 100 %), five portions, then the glass bottle; overdosing is
     * logged. Fresh water dilutes it, a fade over the days, the water values carry it (pipes, filter loops).
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m37_fert", timeoutTicks = 100)
    public static void bottlePortionsAndDilution(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 2, 2); // 8 water blocks
        TankData t = c.getTank();
        ServerPlayer sp = player(h);
        BlockPos top = h.absolutePos(new BlockPos(2, 2, 2));
        ItemStack bottle = new ItemStack(ModItems.LIQUID_FERTILIZER.get());
        double per = Fertilizer.portionBlocks() / t.waterBlocks();
        for (int i = 1; i <= 4; i++) {
            use(h, sp, top, i == 1 ? bottle : sp.getMainHandItem());
            h.assertTrue(near(t.fertilizer(), i * per, 1e-4), "portion " + i + ": " + t.fertilizer());
        }
        h.assertTrue(sp.getMainHandItem().is(ModItems.LIQUID_FERTILIZER.get()) && sp.getMainHandItem().getDamageValue() == 4, "one portion left");
        h.assertTrue(Fertilizer.overdosed(t) && AerationGameTests.logged(c, "log.bettacraft.fertilizer_overdosed"), "overdosed at " + t.fertilizer());
        use(h, sp, top, sp.getMainHandItem());
        h.assertTrue(sp.getMainHandItem().is(Items.GLASS_BOTTLE), "the glass bottle is left: " + sp.getMainHandItem());
        // half the water fresh: half the fertilizer
        t.setFertilizer(1F);
        long half = t.waterMb() / 2;
        TankInteractions.changeWater(level, c, -half);
        TankInteractions.changeWater(level, c, half);
        h.assertTrue(near(t.fertilizer(), 0.5, 1e-3), "diluted: " + t.fertilizer());
        // water with its values: carries the fertilizer (a filter loop keeps it), old water without it is none
        t.setFertilizer(1F);
        WaterValues v = WaterValues.of(t);
        h.assertTrue(near(v.fertilizer(), 1, 1e-3), "carried: " + v);
        TankInteractions.changeWater(level, c, -half);
        TankInteractions.addWater(level, c, half, v);
        h.assertTrue(near(t.fertilizer(), 1, 1e-3), "the same water back: the same level " + t.fertilizer());
        var json = WaterValues.CODEC.encodeStart(JsonOps.INSTANCE, v).getOrThrow().getAsJsonObject();
        json.remove("fertilizer");
        h.assertTrue(WaterValues.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().fertilizer() == 0F, "old water has none");
        // it fades a little by itself
        Fertilizer.step(level, c, 2.0);
        double k = ServerConfig.getOr(ServerConfig.FERT_DECAY, 0.05);
        h.assertTrue(near(t.fertilizer(), Math.exp(-2 * k), 1e-3), "fades: " + t.fertilizer());
        h.succeed();
    }

    // ------------------------------------------------------------------ the fertilizer maker

    /**
     * Without power: kelp meal, nutrient salt and water, the crank winds its spring (25 clicks), a batch makes 250 mB, a
     * glass bottle is filled; the rest goes through a pipe into a tank, only up to 100 %; the dosing pump doses a canister.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m37_fert", timeoutTicks = 200)
    public static void fertilizerMakerOnTheSpringAndThePipe(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        if (FertilizerMakerBlockEntity.powered()) {
            h.succeed(); // a power option is on in this run: the FE test covers it
            return;
        }
        FertilizerMakerBlockEntity m = maker(h, new BlockPos(6, 1, 6));
        m.items().setStackInSlot(FertilizerMakerBlockEntity.MEAL, new ItemStack(ModItems.KELP_MEAL.get(), 2));
        m.items().setStackInSlot(FertilizerMakerBlockEntity.SALT, new ItemStack(ModItems.NUTRIENT_SALT.get(), 2));
        run(level, m, 1);
        h.assertTrue(m.status() == FertilizerMakerBlockEntity.NO_WATER, "needs water: " + m.status());
        IFluidHandler fh = org.overengineer.bettacraft.platform.Platform.get().fluidHandler(level, m.getBlockPos(), Direction.UP);
        h.assertTrue(fh != null && fh.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "water in");
        h.assertTrue(fh.fill(Fertilizer.fluid(100), IFluidHandler.FluidAction.SIMULATE) == 0, "no fertilizer into the water tank");
        run(level, m, 1);
        h.assertTrue(m.status() == FertilizerMakerBlockEntity.WIND_IT, "needs winding: " + m.status());
        // the crank: one click a step, the handle turns; 25 clicks wind one charge
        ServerPlayer sp = player(h);
        ItemStack crank = new ItemStack(ModItems.CRANK.get());
        int crank0 = level.getBlockState(m.getBlockPos()).getValue(FertilizerMakerBlock.CRANK);
        use(h, sp, m.getBlockPos(), crank);
        h.assertTrue(m.windSpring().ticks() == m.windSpring().step() && level.getBlockState(m.getBlockPos()).getValue(FertilizerMakerBlock.CRANK) == (crank0 + 1) % 4,
                "one click winds a step and turns the handle");
        int clicks = 1;
        while (!m.windSpring().full() && clicks < 200) {
            use(h, sp, m.getBlockPos(), crank);
            clicks++;
        }
        h.assertTrue(clicks == Winding.Device.FERTILIZER_MAKER.clicksToFull() && clicks == ServerConfig.getOr(ServerConfig.WIND_CLICKS_PER_CHARGE, 25),
                "one charge in " + clicks + " clicks");
        h.assertTrue(m.windSpring().capacity() == Winding.ticksPerCharge(), "one charge = a day: " + m.windSpring().capacity());
        int batch = FertilizerMakerBlockEntity.batchTicks();
        run(level, m, batch);
        h.assertTrue(m.fertilizer() == Fertilizer.BATCH_MB && m.water() == 1000 - Fertilizer.BATCH_MB, "a batch: " + m.fertilizer() + " / " + m.water());
        h.assertTrue(m.items().getStackInSlot(FertilizerMakerBlockEntity.MEAL).getCount() == 1, "used a kelp meal");
        h.assertTrue(m.windSpring().ticks() == m.windSpring().capacity() - batch, "the spring ran down by the batch");
        // a glass bottle is filled
        m.items().setStackInSlot(FertilizerMakerBlockEntity.BOTTLES, new ItemStack(Items.GLASS_BOTTLE));
        h.assertTrue(m.bottle() && m.items().getStackInSlot(FertilizerMakerBlockEntity.OUTPUT).is(ModItems.LIQUID_FERTILIZER.get()) && m.fertilizer() == 0, "bottled");
        var cap = org.overengineer.bettacraft.platform.Platform.get().itemHandler(level, m.getBlockPos(), Direction.DOWN);
        h.assertTrue(cap != null && cap.extractItem(FertilizerMakerBlockEntity.OUTPUT, 1, false).is(ModItems.LIQUID_FERTILIZER.get())
                && cap.extractItem(FertilizerMakerBlockEntity.MEAL, 1, true).isEmpty(), "hoppers take the bottle, not the ingredients");
        // a second batch into a pipe and a tank (8 water blocks): the tank takes it up to 100 % only
        run(level, m, batch);
        h.assertTrue(m.fertilizer() == Fertilizer.BATCH_MB, "second batch");
        PlumbingGameTests.pipe(h, false, new BlockPos(7, 1, 6), new BlockPos(8, 1, 6));
        TankBlockEntity c = tank(h, 9, 1, 5, 2, 2, 2);
        int given = m.push(level);
        TankData t = c.getTank();
        double up = ServerConfig.getOr(ServerConfig.FERT_PIPE_UP_TO, 1.0);
        h.assertTrue(given > 0 && near(t.fertilizer(), up, 0.03), "through the pipe up to 100 %: " + given + " mB, " + t.fertilizer());
        h.assertTrue(m.fertilizer() == Fertilizer.BATCH_MB - given && m.push(level) == 0, "the rest stays in the maker: " + m.fertilizer());
        // the dosing pump with a fertilizer canister doses a fresh water tank
        TankBlockEntity d = tank(h, 2, 1, 14, 2, 2, 2);
        h.setBlock(new BlockPos(4, 1, 14), ModBlocks.DOSING_PUMP.get().defaultBlockState().setValue(DosingPumpBlock.FACING, Direction.NORTH));
        DosingPumpBlockEntity pump = (DosingPumpBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(4, 1, 14)));
        pump.items().setStackInSlot(0, new ItemStack(ModItems.FERTILIZER_CANISTER.get()));
        pump.setPerDay(0, 100);
        pump.step(1.0);
        double want = 100.0 / Fertilizer.PORTION_MB * Fertilizer.portionBlocks() / d.getTank().waterBlocks();
        h.assertTrue(pump.status() == DosingPumpBlockEntity.RUNNING && near(d.getTank().fertilizer(), want, 1e-3), "dosed 100 mL: " + d.getTank().fertilizer());
        h.assertTrue(pump.items().getStackInSlot(0).getDamageValue() > 0, "the canister is used");
        h.succeed();
    }

    /** With a power option on: FE first, the spring when the FE is short; with power.winding off only FE (own batch). */
    @GameTest(template = EMPTY, batch = "bettacraft_m37_fert_fe", timeoutTicks = 200)
    public static void fertilizerMakerOnFeOrTheSpring(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerConfig.PowerCosts pumps = ServerConfig.POWER_COSTS.get();
        boolean winding = Features.own(Feature.WINDING);
        try {
            ServerConfig.POWER_COSTS.set(ServerConfig.PowerCosts.HARD);
            Features.set(Feature.WINDING, true);
            FertilizerMakerBlockEntity m = maker(h, new BlockPos(2, 1, 2));
            m.items().setStackInSlot(FertilizerMakerBlockEntity.MEAL, new ItemStack(ModItems.KELP_MEAL.get(), 4));
            m.items().setStackInSlot(FertilizerMakerBlockEntity.SALT, new ItemStack(ModItems.NUTRIENT_SALT.get(), 4));
            m.setWater(4000);
            h.assertTrue(org.overengineer.bettacraft.platform.Platform.get().energyStorage(level, m.getBlockPos(), null) != null, "energy capability");
            run(level, m, 1);
            h.assertTrue(m.status() == FertilizerMakerBlockEntity.FE_OR_WIND, "FE or the crank: " + m.status());
            int batch = FertilizerMakerBlockEntity.batchTicks(), fe = ServerConfig.getOr(ServerConfig.FERT_MAKER_FE, 10);
            // the spring only
            m.windSpring().set(batch + 5);
            run(level, m, batch);
            h.assertTrue(m.fertilizer() == Fertilizer.BATCH_MB && m.windSpring().ticks() == 5, "a batch on the spring: " + m.windSpring().ticks());
            // FE first: the spring stays
            m.energy().receiveEnergy(fe * batch, false);
            run(level, m, batch);
            h.assertTrue(m.fertilizer() == 2 * Fertilizer.BATCH_MB && m.windSpring().ticks() == 5 && m.energy().getEnergyStored() == 0, "a batch on FE, the spring kept");
            // power.winding off: only FE, the crank is refused
            Features.set(Feature.WINDING, false);
            run(level, m, 1);
            h.assertTrue(m.status() == FertilizerMakerBlockEntity.NO_POWER, "FE only: " + m.status());
            h.assertTrue(Winding.wind(level, m.getBlockPos(), null) == Winding.Result.REFUSED, "no crank");
        } finally {
            ServerConfig.POWER_COSTS.set(pumps);
            Features.set(Feature.WINDING, winding);
        }
        h.succeed();
    }

    /** features.fertilizer.enabled off: no effect, the maker stands still, bottles do nothing, pipes give nothing (own batch). */
    @GameTest(template = EMPTY, batch = "bettacraft_m37_fert_off", timeoutTicks = 100)
    public static void fertilizerSwitchedOff(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        boolean on = Features.own(Feature.FERTILIZER), dosing = Features.own(Feature.FERTILIZER_DOSING);
        try {
            Features.set(Feature.FERTILIZER, false);
            TankBlockEntity c = tank(h, 2, 1, 2, 2, 2, 2);
            TankData t = c.getTank();
            t.setFertilizer(3F);
            h.assertTrue(Fertilizer.growth(t) == 1.0 && Fertilizer.algae(t) == 1.0 && !Fertilizer.overdosed(t), "no effect");
            t.setFertilizer(0F);
            use(h, player(h), h.absolutePos(new BlockPos(2, 2, 2)), new ItemStack(ModItems.LIQUID_FERTILIZER.get()));
            h.assertTrue(t.fertilizer() == 0F, "the bottle does nothing");
            IFluidHandler tf = org.overengineer.bettacraft.platform.Platform.get().fluidHandler(level, c.getBlockPos(), null);
            h.assertTrue(tf != null && tf.fill(Fertilizer.fluid(250), IFluidHandler.FluidAction.EXECUTE) == 0, "the tank takes none from a pipe");
            FertilizerMakerBlockEntity m = maker(h, new BlockPos(8, 1, 8));
            m.items().setStackInSlot(FertilizerMakerBlockEntity.MEAL, new ItemStack(ModItems.KELP_MEAL.get()));
            m.items().setStackInSlot(FertilizerMakerBlockEntity.SALT, new ItemStack(ModItems.NUTRIENT_SALT.get()));
            m.setWater(1000);
            m.windSpring().set(m.windSpring().capacity());
            run(level, m, FertilizerMakerBlockEntity.batchTicks());
            h.assertTrue(m.status() == FertilizerMakerBlockEntity.OFF && m.fertilizer() == 0, "the maker stands still: " + m.status());
            h.assertTrue(Winding.wind(level, m.getBlockPos(), null) == Winding.Result.REFUSED, "and takes no crank");
            // dosing off: the pump skips the fertilizer canister
            Features.set(Feature.FERTILIZER, true);
            Features.set(Feature.FERTILIZER_DOSING, false);
            h.setBlock(new BlockPos(4, 1, 2), ModBlocks.DOSING_PUMP.get().defaultBlockState().setValue(DosingPumpBlock.FACING, Direction.NORTH));
            DosingPumpBlockEntity pump = (DosingPumpBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(4, 1, 2)));
            pump.items().setStackInSlot(0, new ItemStack(ModItems.FERTILIZER_CANISTER.get()));
            pump.setPerDay(0, 100);
            pump.step(1.0);
            h.assertTrue(t.fertilizer() == 0F && pump.items().getStackInSlot(0).getDamageValue() == 0, "no fertilizer from the pump");
        } finally {
            Features.set(Feature.FERTILIZER, on);
            Features.set(Feature.FERTILIZER_DOSING, dosing);
        }
        h.succeed();
    }
}
