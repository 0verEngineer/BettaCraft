package org.overengineer.bettacraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaBreeding;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.food.CultureModel;
import org.overengineer.bettacraft.food.CultureTubBlockEntity;
import org.overengineer.bettacraft.food.LiveCultureBlockEntity;
import org.overengineer.bettacraft.food.LiveFood;
import org.overengineer.bettacraft.food.LiveFoodPumpBlockEntity;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.WaterValues;

/**
 * M26 part a GameTests: the tubifex dish (mud, mulm, waste water, stale water, poor worms, the net, hoppers), the daphnia
 * barrel (light, growth, collapse when crowded, green water bottles), the live food pump (through pipes and next to a
 * tank, only when somebody eats, the artemia hatchery gets its bottles back), the feeding (tubifex condition twice as
 * long, poor worms stress, the feeder gives live food first) and the feature switches. Deterministic: the cultures and
 * the pump are stepped directly.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LiveFoodGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private LiveFoodGameTests() {}

    static LiveCultureBlockEntity culture(GameTestHelper h, BlockPos rel, boolean tubifex) {
        h.setBlock(rel, (tubifex ? ModBlocks.TUBIFEX_DISH : ModBlocks.DAPHNIA_BARREL).get().defaultBlockState());
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof LiveCultureBlockEntity be)) throw new GameTestAssertException("no culture");
        return be;
    }

    static LiveFoodPumpBlockEntity pump(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, ModBlocks.LIVE_FOOD_PUMP.get().defaultBlockState());
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof LiveFoodPumpBlockEntity p)) throw new GameTestAssertException("no pump");
        return p;
    }

    static BettaEntity betta(GameTestHelper h, TankBlockEntity c, boolean female, long seed) {
        BettaData d = BettaData.create(BettaGenotype.randomDomestic(RandomSource.create(seed)), female, 0);
        ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d);
        Component err = TankInhabitants.insert(h.getLevel(), c, bucket, ModEntities.BETTA.get(), null, false);
        if (err != null) throw new GameTestAssertException("insert failed: " + err.getString());
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) {
            if (e instanceof BettaEntity b && b.getBettaData().fishId().equals(d.fishId())) return b;
        }
        throw new GameTestAssertException("inserted betta not found");
    }

    private static void light(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15));
    }

    private static boolean logged(TankData t, String key) {
        return t.log().stream().anyMatch(e -> e.key().equals(key));
    }

    /** A dish with mud, water, a few worms and mulm grows; stale water makes poor worms; fresh water rinses it. */
    @GameTest(template = EMPTY, batch = "bettacraft_m26", timeoutTicks = 100)
    public static void tubifexDishGrowsAndGoesStale(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        LiveCultureBlockEntity d = culture(h, new BlockPos(2, 1, 2), true);
        d.setWater(1000);
        d.insertOne(new ItemStack(ModItems.TUBIFEX.get()), false);
        d.insertOne(new ItemStack(ModItems.MULM.get()), false);
        h.assertTrue("mud".equals(d.blocker(level)), "needs mud first: " + d.blocker(level));
        h.assertTrue(d.automation().insertItem(LiveCultureBlockEntity.INPUT, new ItemStack(Items.MUD), false).isEmpty(), "a hopper puts the mud in");
        h.assertTrue(d.mud() && !d.automation().insertItem(LiveCultureBlockEntity.INPUT, new ItemStack(Items.MUD), true).isEmpty(), "only one mud");
        d.insertOne(new ItemStack(ModItems.MULM.get()), false);
        h.assertTrue(d.blocker(level) == null, "growing: " + d.blocker(level));
        double p0 = d.model().population;
        d.advance(level, 3.0);
        h.assertTrue(d.model().population > p0 + 0.2, "grew " + p0 + " -> " + d.model().population);
        h.assertTrue(d.water() < 1000, "evaporates: " + d.water());
        // a hopper takes the worms out, the seed stays
        int n = d.model().harvestable(d.room());
        ItemStack out = d.automation().extractItem(LiveCultureBlockEntity.OUTPUT, 64, false);
        h.assertTrue(n > 0 && out.getCount() == n && out.is(ModItems.TUBIFEX.get()) && !LiveFood.poor(out), "harvested " + out);
        h.assertTrue(d.model().population >= CultureModel.SEED - 1e-9 && d.model().harvestable(d.room()) == 0, "the seed stays");
        // waste water of a tank feeds them, clean water rinses
        double food = d.model().food;
        d.setWater(0);
        d.fluid().fill(WaterValues.stack(500, new WaterValues(25F, 0F, 0F, 0.3F, 40F)), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(d.model().food > food + 0.3, "waste water feeds: " + food + " -> " + d.model().food);
        // overfed and old water: poor worms
        d.model().food = 1;
        d.model().population = 0.8;
        d.advance(level, 2.0);
        h.assertTrue(d.model().poor(), "stale: " + d.model().foul);
        h.assertTrue("foul".equals(d.blocker(level)) || d.model().food <= 0, "shows it: " + d.blocker(level));
        ServerPlayer player = MockPlayers.serverPlayer(h);
        h.assertTrue(d.harvest(level, player, LiveCultureBlockEntity.NET_PORTIONS), "the net scoops worms");
        ItemStack caught = ItemStack.EMPTY;
        for (ItemStack s : player.getInventory().items) if (s.is(ModItems.TUBIFEX.get())) caught = s;
        h.assertTrue(caught.getCount() == LiveCultureBlockEntity.NET_PORTIONS && LiveFood.poor(caught), "four poor worms: " + caught);
        d.setWater(1000);
        double foul = d.model().foul;
        d.fluid().fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), IFluidHandler.FluidAction.SIMULATE);
        d.setWater(0);
        d.fluid().fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(d.model().foul < foul - 0.4, "fresh water rinses: " + foul + " -> " + d.model().foul);
        h.succeed();
    }

    /** Daphnia need light, grow with yeast and green water and collapse when nobody harvests them. */
    @GameTest(template = EMPTY, batch = "bettacraft_m26", timeoutTicks = 100)
    public static void daphniaBarrelGrowsAndCollapsesWhenCrowded(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        LiveCultureBlockEntity b = culture(h, new BlockPos(2, 1, 2), false);
        light(h, new BlockPos(2, 2, 2));
        h.assertTrue("water".equals(b.blocker(level)), "needs water: " + b.blocker(level));
        b.setWater(4000);
        h.assertTrue("animals".equals(b.blocker(level)), "needs daphnia: " + b.blocker(level));
        h.assertTrue(b.insertOne(new ItemStack(ModItems.DAPHNIA.get()), false) && b.insertOne(new ItemStack(ModItems.DAPHNIA.get()), false), "two starters");
        h.assertTrue("hungry".equals(b.blocker(level)), "needs food: " + b.blocker(level));
        h.assertTrue(b.automation().insertItem(LiveCultureBlockEntity.INPUT, new ItemStack(ModItems.GREEN_WATER_BOTTLE.get()), false).isEmpty(), "green water");
        h.assertTrue(b.bottles() == 1 && b.automation().extractItem(LiveCultureBlockEntity.BOTTLES, 4, false).is(Items.GLASS_BOTTLE), "its bottle comes out");
        b.insertOne(new ItemStack(ModItems.YEAST.get()), false);
        h.assertTrue(b.blocker(level) == null, "lit, fed: " + b.blocker(level) + " light " + level.getMaxLocalRawBrightness(h.absolutePos(new BlockPos(2, 2, 2))));
        double p = b.model().population;
        for (int i = 0; i < 3; i++) {
            b.model().food = 1;
            b.advance(level, 1.0);
        }
        h.assertTrue(b.model().population > 0.7, "grew " + p + " -> " + b.model().population);
        CultureModel.Event e = CultureModel.Event.NONE;
        for (int i = 0; i < 6 && e != CultureModel.Event.COLLAPSE_CROWDED; i++) {
            b.model().food = 1;
            e = b.advance(level, 0.5);
        }
        h.assertTrue(e == CultureModel.Event.COLLAPSE_CROWDED && b.model().population < 0.2, "collapsed: " + e + " " + b.model().population);
        h.assertTrue(!b.hot(level), "the test biome is not hot");
        h.succeed();
    }

    /** The pump takes daphnia from the barrel and pumps them through a pipe into a tank where a betta eats them. */
    @GameTest(template = EMPTY, batch = "bettacraft_m26", timeoutTicks = 100)
    public static void pumpFeedsATankThroughPipes(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 1, 2, 1, 2, 2, 2);
        PlumbingGameTests.pipe(h, true, new BlockPos(3, 2, 1), new BlockPos(4, 2, 1), new BlockPos(5, 2, 1));
        LiveCultureBlockEntity b = culture(h, new BlockPos(7, 2, 1), false);
        LiveFoodPumpBlockEntity p = pump(h, new BlockPos(6, 2, 1));
        b.setWater(4000);
        b.model().population = 0.6;
        // nobody in the tank: nothing goes out
        p.setDoses(0);
        p.setPortions(2);
        p.doseNow();
        p.step(level);
        h.assertTrue(p.status() == LiveFoodPumpBlockEntity.NOBODY_EATS, "nobody eats: " + p.status());
        h.assertTrue(Math.abs(b.model().population - 0.6) < 1e-9, "nothing taken");
        BettaEntity fish = betta(h, c, false, 26);
        int before = b.model().harvestable(b.room());
        p.doseNow();
        p.step(level);
        h.assertTrue(p.status() == LiveFoodPumpBlockEntity.PUMPED && p.lastPumped() == 2, "pumped: " + p.status() + " " + p.lastPumped());
        h.assertTrue(b.model().harvestable(b.room()) == before - 2, "two portions taken: " + before + " -> " + b.model().harvestable(b.room()));
        h.assertTrue(fish.health().daysSinceFed() == 0 && fish.breeding().isConditioned(), "the betta ate live food");
        h.assertTrue(logged(c.getTank(), "log.bettacraft.live_food_pumped") || logged(c.getTank(), "log.bettacraft.live_food_pumped_one"), "logged");
        // the schedule: two doses a day, the first one at 7:00
        p.setDoses(2);
        level.setDayTime(500);
        p.step(level);
        h.assertTrue(p.pending() == 0, "waits for 7:00");
        level.setDayTime(1000);
        p.step(level);
        h.assertTrue(p.status() == LiveFoodPumpBlockEntity.PUMPED && p.pending() == 0, "dosed at 7:00: " + p.status());
        // a glass pipe carries 500 mB per 10 ticks: a big dose goes out in parts
        PlumbingGameTests.pipe(h, false, new BlockPos(4, 2, 1));
        b.model().population = 0.9;
        p.setPortions(5);
        p.doseNow();
        p.step(level);
        h.assertTrue(p.status() == LiveFoodPumpBlockEntity.BUSY && p.pending() > 0, "the glass pipe is at its limit: " + p.status() + " " + p.pending());
        h.succeed();
    }

    /** Next to a tank without pipes; the artemia hatchery gets its bottles back; fry food reaches only the fry eaters. */
    @GameTest(template = EMPTY, batch = "bettacraft_m26", timeoutTicks = 100)
    public static void pumpNextToATankAndTheHatchery(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 1, 2, 1, 2, 2, 2);
        betta(h, c, true, 27);
        h.setBlock(new BlockPos(4, 2, 1), ModBlocks.ARTEMIA_HATCHERY.get().defaultBlockState());
        if (!(level.getBlockEntity(h.absolutePos(new BlockPos(4, 2, 1))) instanceof CultureTubBlockEntity hatchery)) throw new GameTestAssertException("no hatchery");
        LiveFoodPumpBlockEntity p = pump(h, new BlockPos(3, 2, 1));
        hatchery.items().setStackInSlot(CultureTubBlockEntity.OUTPUT, new ItemStack(ModItems.ARTEMIA_NAUPLII.get(), 3));
        p.setDoses(0);
        p.doseNow();
        p.step(level);
        h.assertTrue(p.status() == LiveFoodPumpBlockEntity.NOBODY_EATS, "adults do not eat nauplii: " + p.status());
        // a tubifex dish instead: live food
        h.setBlock(new BlockPos(4, 2, 1), Blocks.AIR.defaultBlockState());
        LiveCultureBlockEntity d = culture(h, new BlockPos(4, 2, 1), true);
        d.setMud(true);
        d.setWater(1000);
        d.model().population = 0.5;
        p.doseNow();
        p.step(level);
        h.assertTrue(p.status() == LiveFoodPumpBlockEntity.PUMPED, "tubifex into the tank next to it: " + p.status());
        // the hatchery gets the bottle of each pumped portion back (the shrimp in the tank eat nauplii)
        h.setBlock(new BlockPos(4, 2, 1), ModBlocks.ARTEMIA_HATCHERY.get().defaultBlockState());
        if (!(level.getBlockEntity(h.absolutePos(new BlockPos(4, 2, 1))) instanceof CultureTubBlockEntity hatch2)) throw new GameTestAssertException("no hatchery");
        hatch2.items().setStackInSlot(CultureTubBlockEntity.OUTPUT, new ItemStack(ModItems.ARTEMIA_NAUPLII.get(), 2));
        c.getTank().setCritterFood(0); // the leftovers of the tubifex fed them already
        for (int i = 0; i < 4; i++) {
            org.overengineer.bettacraft.critters.TankCritters.add(level, c, org.overengineer.bettacraft.critters.CritterSpecies.NEOCARIDINA,
                    org.overengineer.bettacraft.critters.CritterGenes.WILD, i % 2 == 0, org.overengineer.bettacraft.critters.CritterSpecies.NEOCARIDINA.juvenileDays() + 1);
        }
        p.doseNow();
        p.step(level);
        h.assertTrue(p.status() == LiveFoodPumpBlockEntity.PUMPED, "the shrimp eat the nauplii: " + p.status());
        h.assertTrue(hatch2.items().getStackInSlot(CultureTubBlockEntity.BOTTLES).is(Items.GLASS_BOTTLE), "the bottle went back to the hatchery");
        h.succeed();
    }

    /** Tubifex condition twice as long as mosquito larvae; poor ones add stress; the feeder gives live food first. */
    @GameTest(template = EMPTY, batch = "bettacraft_m26", timeoutTicks = 100)
    public static void feedingWithLiveFood(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 1, 1, 1, 2, 2, 2);
        BettaEntity fish = betta(h, c, false, 28);
        double days = ServerConfig.getOr(ServerConfig.BREEDING_CONDITIONING_DAYS, BettaBreeding.CONDITIONING_DAYS);
        TankCare.feed(level, c, new ItemStack(ModItems.MOSQUITO_LARVAE.get()), 1);
        h.assertTrue(Math.abs(fish.breeding().conditionedDays() - days) < 1e-6, "larvae: " + fish.breeding().conditionedDays());
        TankCare.feed(level, c, new ItemStack(ModItems.TUBIFEX.get()), 1);
        h.assertTrue(Math.abs(fish.breeding().conditionedDays() - 2 * days) < 1e-6, "tubifex: twice " + fish.breeding().conditionedDays());
        double stress = fish.health().waterStress();
        ItemStack poor = new ItemStack(ModItems.TUBIFEX.get());
        poor.set(ModDataComponents.POOR_CULTURE.get(), true);
        TankCare.feed(level, c, poor, 1);
        h.assertTrue(fish.health().waterStress() > stress + 0.4, "poor worms stress: " + stress + " -> " + fish.health().waterStress());
        // the feeder: pellets in the first slot, daphnia in the second: the daphnia go first
        h.setBlock(new BlockPos(1, 3, 1), ModBlocks.FEEDER.get().defaultBlockState());
        if (!(level.getBlockEntity(h.absolutePos(new BlockPos(1, 3, 1))) instanceof FeederBlockEntity feeder)) throw new GameTestAssertException("no feeder");
        feeder.items().setStackInSlot(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 5));
        feeder.items().setStackInSlot(1, new ItemStack(ModItems.DAPHNIA.get(), 5));
        feeder.feedNow(level, c);
        h.assertTrue(feeder.items().getStackInSlot(0).getCount() == 5 && feeder.items().getStackInSlot(1).getCount() == 4,
                "live food first: pellets " + feeder.items().getStackInSlot(0) + ", daphnia " + feeder.items().getStackInSlot(1));
        h.succeed();
    }

    /** Switched off: the cultures stand still, the pump does not pump and tanks take no suspension. */
    @GameTest(template = EMPTY, batch = "bettacraft_m26_off", timeoutTicks = 100)
    public static void featureSwitches(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        boolean cul = CommonConfig.FEATURE_LIVE_FOOD_CULTURES.get(), pmp = CommonConfig.FEATURE_LIVE_FOOD_PUMP.get();
        try {
            CommonConfig.FEATURE_LIVE_FOOD_CULTURES.set(false);
            CommonConfig.FEATURE_LIVE_FOOD_PUMP.set(false);
            LiveCultureBlockEntity d = culture(h, new BlockPos(8, 1, 8), true);
            d.setMud(true);
            d.setWater(1000);
            d.model().population = 0.3;
            d.model().food = 0.5;
            d.advance(level, 2.0);
            h.assertTrue(d.model().population == 0.3 && "off".equals(d.blocker(level)), "stands still: " + d.model().population);
            TankBlockEntity c = AerationGameTests.tank(h, 1, 2, 1, 2, 2, 2);
            betta(h, c, false, 29);
            LiveFoodPumpBlockEntity p = pump(h, new BlockPos(3, 2, 1));
            LiveCultureBlockEntity b = culture(h, new BlockPos(4, 2, 1), false);
            b.setWater(4000);
            b.model().population = 0.6;
            p.doseNow();
            p.step(level);
            h.assertTrue(p.status() == LiveFoodPumpBlockEntity.OFF && b.model().population == 0.6, "the pump is off: " + p.status());
            IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, c.getBlockPos(), null);
            h.assertTrue(tank != null && tank.fill(LiveFood.suspension(new ItemStack(ModItems.DAPHNIA.get()), 1), IFluidHandler.FluidAction.EXECUTE) == 0,
                    "the tank takes no suspension");
        } finally {
            CommonConfig.FEATURE_LIVE_FOOD_CULTURES.set(cul);
            CommonConfig.FEATURE_LIVE_FOOD_PUMP.set(pmp);
        }
        h.succeed();
    }
}
