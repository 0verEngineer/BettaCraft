package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.platform.item.IItemHandler;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.food.BettaFood;
import org.overengineer.bettacraft.food.FoodDiet;
import org.overengineer.bettacraft.food.FoodMakerBlock;
import org.overengineer.bettacraft.food.FoodMakerBlockEntity;
import org.overengineer.bettacraft.food.LiveFood;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M26 part b GameTests: the food maker (spring, FE, automatic and chosen product, hoppers and other mods through the item
 * capability, redstone) and the food variety (food groups, favourite and poor food per diet, the varied-diet bonus, the
 * feeder slots taking turns, frozen food, shrimp food) and the feature switches. Deterministic: the machine is ticked
 * directly, the fish are fed directly.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FoodMakerGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private FoodMakerGameTests() {}

    static FoodMakerBlockEntity maker(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, ModBlocks.FOOD_MAKER.get().defaultBlockState());
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof FoodMakerBlockEntity be)) throw new GameTestFailure("no food maker");
        return be;
    }

    private static void run(ServerLevel level, FoodMakerBlockEntity m, int ticks) {
        for (int i = 0; i < ticks; i++) m.tick(level, m.getBlockPos(), level.getBlockState(m.getBlockPos()));
    }

    /** Without power: the spring needs winding, then a batch of pellets comes out; hoppers fill it and empty the output. */
    @GameTest(template = EMPTY, batch = "bettacraft_m26b", timeoutTicks = 100)
    public static void foodMakerRunsOnTheSpring(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        if (FoodMakerBlockEntity.powered()) {
            h.succeed(); // a power option is on in this run: the FE test covers it
            return;
        }
        FoodMakerBlockEntity m = maker(h, new BlockPos(2, 1, 2));
        IItemHandler cap = org.overengineer.bettacraft.platform.Platform.get().itemHandler(level, m.getBlockPos(), Direction.UP);
        h.assertTrue(cap != null, "item capability");
        for (ItemStack s : new ItemStack[]{new ItemStack(Items.COD), new ItemStack(Items.WHEAT), new ItemStack(Items.DRIED_KELP, 2)}) {
            ItemStack rest = s;
            for (int i = 0; i < FoodMakerBlockEntity.INPUTS && !rest.isEmpty(); i++) rest = cap.insertItem(i, rest, false);
            h.assertTrue(rest.isEmpty(), "inserted " + s);
        }
        h.assertTrue(!cap.insertItem(FoodMakerBlockEntity.OUTPUT, new ItemStack(Items.WHEAT), true).isEmpty(), "nothing into the output");
        h.assertTrue(cap.extractItem(0, 1, true).isEmpty(), "the inputs stay in");
        run(level, m, 1);
        h.assertTrue(m.status() == FoodMakerBlockEntity.WIND_IT, "needs winding: " + m.status());
        int winds = ServerConfig.getOr(ServerConfig.FOOD_MAKER_WINDS, 8);
        BlockState before = level.getBlockState(m.getBlockPos());
        h.assertTrue(m.wind(null), "winds");
        h.assertTrue(level.getBlockState(m.getBlockPos()).getValue(FoodMakerBlock.CRANK) == (before.getValue(FoodMakerBlock.CRANK) + 1) % 4, "the crank turns");
        for (int i = 1; i < winds; i++) m.wind(null);
        h.assertTrue(m.spring() == FoodMakerBlockEntity.springMax() && !m.wind(null), "fully wound: " + m.spring());
        int batch = FoodMakerBlockEntity.batchTicks();
        run(level, m, batch);
        ItemStack out = cap.extractItem(FoodMakerBlockEntity.OUTPUT, 64, false);
        h.assertTrue(out.is(ModItems.BETTA_PELLETS.get()) && out.getCount() == 12, "12 pellets: " + out);
        h.assertTrue(m.spring() == FoodMakerBlockEntity.springMax() - batch, "the spring ran down by a batch: " + m.spring());
        h.assertTrue(m.items().getStackInSlot(0).isEmpty() && m.items().getStackInSlot(2).getCount() == 1, "used the ingredients");
        // automatic: with kelp and sugar the flakes (more ingredients) win; a chosen product only makes that one
        m.items().setStackInSlot(0, new ItemStack(Items.SALMON));
        m.items().setStackInSlot(1, new ItemStack(Items.WHEAT));
        m.items().setStackInSlot(2, new ItemStack(Items.KELP));
        m.items().setStackInSlot(3, new ItemStack(Items.SUGAR));
        h.assertTrue(m.current(level) != null && m.current(level).result().is(ModItems.FISH_FLAKES.get()), "flakes");
        m.select(BettaCraft.id("food_maker/food_granules"));
        run(level, m, 1);
        h.assertTrue(m.status() == FoodMakerBlockEntity.NO_RECIPE, "no granules from these: " + m.status());
        m.cycle(1);
        h.assertTrue(m.selected() != null && !m.selected().equals(BettaCraft.id("food_maker/food_granules")), "cycles on: " + m.selected());
        m.select(null);
        // frozen food: four larvae and ice
        m.items().setStackInSlot(0, new ItemStack(ModItems.MOSQUITO_LARVAE.get(), 4));
        m.items().setStackInSlot(1, new ItemStack(Items.ICE));
        m.items().setStackInSlot(2, ItemStack.EMPTY);
        m.items().setStackInSlot(3, ItemStack.EMPTY);
        run(level, m, batch);
        h.assertTrue(m.items().getStackInSlot(FoodMakerBlockEntity.OUTPUT).is(ModItems.FROZEN_MOSQUITO_LARVAE.get())
                && m.items().getStackInSlot(FoodMakerBlockEntity.OUTPUT).getCount() == 4, "frozen larvae: " + m.items().getStackInSlot(FoodMakerBlockEntity.OUTPUT));
        // redstone pauses it
        m.items().setStackInSlot(0, new ItemStack(ModItems.MOSQUITO_LARVAE.get(), 4));
        m.items().setStackInSlot(1, new ItemStack(Items.ICE));
        h.setBlock(new BlockPos(3, 1, 2), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState());
        run(level, m, 1);
        h.assertTrue(m.status() == FoodMakerBlockEntity.PAUSED, "paused: " + m.status());
        h.succeed();
    }

    /** With a power option on it has no spring and takes FE. */
    @GameTest(template = EMPTY, batch = "bettacraft_m26b_fe", timeoutTicks = 100)
    public static void foodMakerRunsOnFe(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        boolean pumps = ServerConfig.POWER_PUMPS.get();
        try {
            ServerConfig.POWER_PUMPS.set(true);
            FoodMakerBlockEntity m = maker(h, new BlockPos(2, 1, 2));
            // M37: with features.power.winding the crank winds it on FE too (FE first, then the spring)
            boolean winding = org.overengineer.bettacraft.winding.Winding.enabled();
            h.assertTrue(m.wind(null) == winding, "the crank winds it on FE only with power.winding");
            m.setSpring(0);
            h.assertTrue(org.overengineer.bettacraft.platform.Platform.get().energyStorage(level, m.getBlockPos(), null) != null, "energy capability");
            m.items().setStackInSlot(0, new ItemStack(Items.EGG));
            m.items().setStackInSlot(1, new ItemStack(Items.WHEAT));
            run(level, m, 1);
            h.assertTrue(m.status() == (winding ? FoodMakerBlockEntity.FE_OR_WIND : FoodMakerBlockEntity.NO_POWER), "needs FE: " + m.status());
            int fe = ServerConfig.getOr(ServerConfig.FOOD_MAKER_FE, 10);
            int batch = FoodMakerBlockEntity.batchTicks();
            m.energy().receiveEnergy(fe * batch, false);
            run(level, m, batch);
            h.assertTrue(m.items().getStackInSlot(FoodMakerBlockEntity.OUTPUT).is(ModItems.FRY_POWDER.get()), "fry powder: " + m.items().getStackInSlot(FoodMakerBlockEntity.OUTPUT));
            h.assertTrue(m.energy().getEnergyStored() == 0, "used the FE: " + m.energy().getEnergyStored());
        } finally {
            ServerConfig.POWER_PUMPS.set(pumps);
        }
        h.succeed();
    }

    /** Food groups make a varied diet; favourite food conditions, poor food fills less; granules reach the bottom fish. */
    @GameTest(template = EMPTY, batch = "bettacraft_m26b", timeoutTicks = 100)
    public static void foodVarietyAndDiets(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 1, 1, 1, 3, 2, 3);
        BettaEntity fish = LiveFoodGameTests.betta(h, c, false, 261);
        h.assertTrue(FoodDiet.group(new ItemStack(ModItems.FISH_FLAKES.get())) == FoodDiet.Group.FLAKES, "flakes are flakes");
        h.assertTrue(BettaFood.kind(new ItemStack(ModItems.FROZEN_DAPHNIA.get())) == BettaFood.Kind.LIVE, "frozen food eats like live food");
        h.assertTrue(!LiveFood.pumpable(new ItemStack(ModItems.FROZEN_DAPHNIA.get())), "frozen food is not pumped");
        TankCare.feed(level, c, new ItemStack(ModItems.BETTA_PELLETS.get()), 1);
        TankCare.feed(level, c, new ItemStack(ModItems.FISH_FLAKES.get()), 1);
        h.assertTrue(fish.health().variety(3) == 2 && !fish.health().varied(FoodDiet.params()), "two kinds: " + fish.health().variety(3));
        h.assertTrue(!fish.breeding().isConditioned(), "flakes do not condition a betta");
        TankCare.feed(level, c, new ItemStack(ModItems.FROZEN_MOSQUITO_LARVAE.get()), 1);
        h.assertTrue(fish.health().varied(FoodDiet.params()), "three kinds: varied");
        h.assertTrue(fish.breeding().isConditioned(), "frozen larvae condition (live-like)");
        // a corydoras: flakes suit it badly, granules are its favourite (and sink to it past the betta)
        TankmateEntity cory = AerationGameTests.add(h, c, TankmateSpecies.PANDA_CORY, LifeStage.ADULT);
        h.assertTrue(FoodDiet.of(TankmateSpecies.PANDA_CORY) == FoodDiet.Diet.BOTTOM && FoodDiet.of(TankmateSpecies.BRISTLENOSE_PLECO) == FoodDiet.Diet.GRAZER
                && FoodDiet.of(TankmateSpecies.NEON_TETRA) == FoodDiet.Diet.OMNIVORE && FoodDiet.of(TankmateSpecies.DWARF_SEAHORSE) == FoodDiet.Diet.SEAHORSE, "diets");
        java.util.Set<java.util.UUID> round = new java.util.HashSet<>();
        round.add(fish.getUUID()); // only the cory eats
        TankCare.feed(level, c, new ItemStack(ModItems.FISH_FLAKES.get()), 1, round);
        h.assertTrue(Math.abs(cory.health().daysSinceFed() - ServerConfig.getOr(ServerConfig.DIET_POOR_FED_DAYS, 0.5)) < 1e-6,
                "flakes fill a cory only a while: " + cory.health().daysSinceFed());
        h.assertTrue(!cory.breeding().isConditioned(), "not conditioned by flakes");
        for (int i = 0; i < 4; i++) { // granules sink: it gets them every time even next to the betta
            cory.health().feedPoorly(1.0);
            TankCare.feed(level, c, new ItemStack(ModItems.FOOD_GRANULES.get()), 1);
            h.assertTrue(cory.health().daysSinceFed() == 0, "granules reach the cory (" + i + ")");
        }
        h.assertTrue(cory.breeding().isConditioned(), "granules (favourite) condition the cory");
        // shrimp food: only the small animals
        for (int i = 0; i < 10; i++) TankCritters.add(level, c, CritterSpecies.NEOCARIDINA, CritterGenes.WILD, i % 2 == 0, CritterSpecies.NEOCARIDINA.juvenileDays() + 1);
        c.getTank().setCritterFood(0);
        fish.health().feedPoorly(0.8);
        int used = TankCare.feed(level, c, new ItemStack(ModItems.SHRIMP_FOOD.get()), 4);
        h.assertTrue(used == 1 && c.getTank().critterFood() > 0, "the shrimp got it: " + used + " " + c.getTank().critterFood());
        h.assertTrue(Math.abs(fish.health().daysSinceFed() - 0.8) < 1e-6, "the betta did not eat shrimp food");
        h.succeed();
    }

    /** The feeder slots take turns, so a feeder with three foods gives a varied diet. */
    @GameTest(template = EMPTY, batch = "bettacraft_m26b", timeoutTicks = 100)
    public static void feederSlotsTakeTurns(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 1, 1, 1, 2, 2, 2);
        BettaEntity fish = LiveFoodGameTests.betta(h, c, true, 262);
        h.setBlock(new BlockPos(1, 3, 1), ModBlocks.FEEDER.get().defaultBlockState());
        if (!(level.getBlockEntity(h.absolutePos(new BlockPos(1, 3, 1))) instanceof FeederBlockEntity feeder)) throw new GameTestFailure("no feeder");
        feeder.items().setStackInSlot(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 5));
        feeder.items().setStackInSlot(1, new ItemStack(ModItems.FISH_FLAKES.get(), 5));
        feeder.items().setStackInSlot(2, new ItemStack(ModItems.FOOD_GRANULES.get(), 5));
        for (int i = 0; i < 3; i++) feeder.feedNow(level, c);
        for (int i = 0; i < 3; i++) h.assertTrue(feeder.items().getStackInSlot(i).getCount() == 4, "slot " + i + " gave once: " + feeder.items().getStackInSlot(i));
        h.assertTrue(fish.health().varied(FoodDiet.params()) && fish.health().variety(3) == 3, "varied: " + fish.health().variety(3));
        h.succeed();
    }

    /** Switched off: the food maker stands still; without food variety every food is the same. */
    @GameTest(template = EMPTY, batch = "bettacraft_m26b_off", timeoutTicks = 100)
    public static void foodFeatureSwitches(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        boolean maker = Features.own(Feature.FOOD_MACHINE), variety = Features.own(Feature.FOOD_VARIETY);
        try {
            Features.set(Feature.FOOD_MACHINE, false);
            Features.set(Feature.FOOD_VARIETY, false);
            FoodMakerBlockEntity m = maker(h, new BlockPos(8, 1, 8));
            m.items().setStackInSlot(0, new ItemStack(Items.EGG));
            m.items().setStackInSlot(1, new ItemStack(Items.WHEAT));
            m.setSpring(FoodMakerBlockEntity.springMax());
            run(level, m, FoodMakerBlockEntity.batchTicks());
            h.assertTrue(m.status() == FoodMakerBlockEntity.OFF && m.items().getStackInSlot(FoodMakerBlockEntity.OUTPUT).isEmpty(), "off: " + m.status());
            TankBlockEntity c = AerationGameTests.tank(h, 1, 1, 1, 2, 2, 2);
            BettaEntity fish = LiveFoodGameTests.betta(h, c, false, 263);
            TankmateEntity cory = AerationGameTests.add(h, c, TankmateSpecies.PANDA_CORY, LifeStage.ADULT);
            cory.health().feedPoorly(1.0);
            java.util.Set<java.util.UUID> round = new java.util.HashSet<>();
            round.add(fish.getUUID());
            TankCare.feed(level, c, new ItemStack(ModItems.FISH_FLAKES.get()), 4, round);
            h.assertTrue(cory.health().variety(3) == 0, "no food memory");
            h.assertTrue(cory.health().daysSinceFed() == 0, "flakes fill like any food: " + cory.health().daysSinceFed());
        } finally {
            Features.set(Feature.FOOD_MACHINE, maker);
            Features.set(Feature.FOOD_VARIETY, variety);
        }
        h.succeed();
    }
}
