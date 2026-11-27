package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.crucible.CrucibleBlockEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.filter.FilterChamberBlockEntity;
import org.overengineer.bettacraft.filter.FilterMedium;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity;
import org.overengineer.bettacraft.filter.MultiblockFilter;
import org.overengineer.bettacraft.plumbing.LevelSensorBlockEntity;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.PipeNetwork;
import org.overengineer.bettacraft.plumbing.ReservoirBlockEntity;
import org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity;
import org.overengineer.bettacraft.plumbing.WaterPumpBlock;
import org.overengineer.bettacraft.plumbing.WaterPumpBlockEntity;
import org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing;
import org.overengineer.bettacraft.plumbing.interior.PlumbingBlock;
import org.overengineer.bettacraft.plumbing.interior.PlumbingItem;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCurrents;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankNitrate;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.WaterValues;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M24 GameTests: the crucible and stainless steel (recipes, tag, foreign steel), pipes and the water pump (water values,
 * throughput, foreign fluid handlers: a cauldron), the bulkhead with interior pipes and outlets (closed without outlets,
 * the current where the nozzle sits), the multiblock filter (forming, cleaning, clogging, washing), nitrate per
 * difficulty (M68 part B: the water change controller and the top-off are in {@link WaterChangeGameTests}). Deterministic: every step is called
 * directly (pump batches, care steps, melt ticks), nothing waits for chance; config changes in their own batches.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlumbingGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private PlumbingGameTests() {}

    // ------------------------------------------------------------------ helpers

    static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz) {
        return AerationGameTests.tank(h, x, y, z, sx, sy, sz);
    }

    static WaterPumpBlockEntity pump(GameTestHelper h, BlockPos rel, Direction out) {
        h.setBlock(rel, ModBlocks.WATER_PUMP.get().defaultBlockState().setValue(WaterPumpBlock.FACING, out));
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof WaterPumpBlockEntity p)) throw new GameTestFailure("no pump");
        return p;
    }

    static int batch(GameTestHelper h, WaterPumpBlockEntity p) {
        return p.pump(h.getLevel(), p.getBlockPos(), p.getBlockState());
    }

    static void pipe(GameTestHelper h, boolean steel, BlockPos... rel) {
        for (BlockPos p : rel) h.setBlock(p, (steel ? ModBlocks.STAINLESS_STEEL_PIPE : ModBlocks.GLASS_PIPE).get().defaultBlockState());
    }

    static ReservoirBlockEntity reservoir(GameTestHelper h, BlockPos rel, int mb, WaterValues v) {
        h.setBlock(rel, ModBlocks.RESERVOIR.get().defaultBlockState());
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof ReservoirBlockEntity r)) throw new GameTestFailure("no reservoir");
        if (mb > 0) r.water().fill(WaterValues.stack(mb, v), IFluidHandler.FluidAction.EXECUTE);
        return r;
    }

    static boolean near(double a, double b, double eps) {
        return Math.abs(a - b) <= eps;
    }

    // ------------------------------------------------------------------ crucible and stainless steel

    /** 4 iron + coal + quartz melt into 3 stainless steel over lava (free) and with a blaze rod as fuel (M29.1: no blaze powder). */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void crucibleMeltsStainlessSteel(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (boolean lava : new boolean[]{true, false}) {
            BlockPos rel = new BlockPos(lava ? 2 : 6, 2, 2);
            if (lava) h.setBlock(rel.below(), Blocks.MAGMA_BLOCK);
            h.setBlock(rel, ModBlocks.CRUCIBLE.get().defaultBlockState());
            BlockPos pos = h.absolutePos(rel);
            if (!(level.getBlockEntity(pos) instanceof CrucibleBlockEntity be)) throw new GameTestFailure("no crucible");
            be.items().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 5));
            be.items().setStackInSlot(1, new ItemStack(Items.COAL, 1));
            be.items().setStackInSlot(2, new ItemStack(Items.QUARTZ, 1));
            if (!lava) be.items().setStackInSlot(CrucibleBlockEntity.FUEL, new ItemStack(Items.BLAZE_ROD, 1));
            for (int t = 0; t < CrucibleBlockEntity.meltTicks(); t++) be.tick(level, pos, level.getBlockState(pos));
            ItemStack out = be.items().getStackInSlot(CrucibleBlockEntity.OUTPUT);
            h.assertTrue(out.is(ModItems.STAINLESS_STEEL_INGOT.get()) && out.getCount() == 3, (lava ? "lava" : "blaze rod") + ": 3 stainless steel: " + out);
            h.assertTrue(be.items().getStackInSlot(0).getCount() == 1 && be.items().getStackInSlot(1).isEmpty() && be.items().getStackInSlot(2).isEmpty(),
                    "the ingredients are used (one iron left)");
            h.assertTrue(be.source() == (lava ? CrucibleBlockEntity.Heat.LAVA : CrucibleBlockEntity.Heat.FUEL) || be.source() == CrucibleBlockEntity.Heat.NONE,
                    "heat source: " + be.source());
            if (!lava) h.assertTrue(be.items().getStackInSlot(CrucibleBlockEntity.FUEL).isEmpty(), "the blaze rod burned");
            // nothing left to melt: it stays cold
            for (int t = 0; t < 20; t++) be.tick(level, pos, level.getBlockState(pos));
            h.assertTrue(be.source() == CrucibleBlockEntity.Heat.NONE && be.progress() == 0, "cold without a recipe");
        }
        // without heat it does not melt
        h.setBlock(new BlockPos(10, 2, 2), ModBlocks.CRUCIBLE.get().defaultBlockState());
        BlockPos cold = h.absolutePos(new BlockPos(10, 2, 2));
        CrucibleBlockEntity c = (CrucibleBlockEntity) level.getBlockEntity(cold);
        c.items().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 4));
        c.items().setStackInSlot(1, new ItemStack(Items.CHARCOAL, 1));
        c.items().setStackInSlot(2, new ItemStack(Items.QUARTZ, 1));
        for (int t = 0; t < CrucibleBlockEntity.meltTicks() + 5; t++) c.tick(level, cold, level.getBlockState(cold));
        h.assertTrue(c.items().getStackInSlot(CrucibleBlockEntity.OUTPUT).isEmpty(), "no heat, no steel");
        h.succeed();
    }

    /**
     * Our ingot is #c:ingots/stainless_steel and the pipes need that tag; steel of other mods (#c:ingots/steel) is not
     * accepted. With the power mods installed (-Ppower) the Mekanism and EnderIO recipes are loaded, without them not.
     */
    @GameTest(template = EMPTY, timeoutTicks = 50)
    public static void stainlessSteelTagAndForeignRecipes(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ItemStack ours = new ItemStack(ModItems.STAINLESS_STEEL_INGOT.get());
        h.assertTrue(ours.is(ModTags.STAINLESS_STEEL_INGOTS), "our ingot carries #c:ingots/stainless_steel");
        var steelTag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/steel"));
        h.assertTrue(!ours.is(steelTag), "our ingot is no plain steel");
        var pipeRecipe = level.recipeAccess().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, BettaCraft.id("stainless_steel_pipe"))).orElseThrow();
        var ok = net.minecraft.world.item.crafting.CraftingInput.of(3, 1, List.of(ours.copy(), ours.copy(), ours.copy()));
        h.assertTrue(((net.minecraft.world.item.crafting.CraftingRecipe) pipeRecipe.value()).matches(ok, level), "pipes from stainless steel");
        int foreign = 0;
        for (var item : BuiltInRegistries.ITEM) {
            ItemStack s = new ItemStack(item);
            if (!s.is(steelTag)) continue;
            foreign++;
            h.assertTrue(!s.is(ModTags.STAINLESS_STEEL_INGOTS), "foreign steel is no stainless steel: " + BuiltInRegistries.ITEM.getKey(item));
            var in = net.minecraft.world.item.crafting.CraftingInput.of(3, 1, List.of(s.copy(), s.copy(), s.copy()));
            h.assertTrue(!((net.minecraft.world.item.crafting.CraftingRecipe) pipeRecipe.value()).matches(in, level), "no pipes from foreign steel");
        }
        boolean mek = org.overengineer.bettacraft.platform.Platform.get().isModLoaded("mekanism"), eio = org.overengineer.bettacraft.platform.Platform.get().isModLoaded("enderio");
        h.assertTrue(level.recipeAccess().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, BettaCraft.id("compat/mekanism_stainless_steel_ingot"))).isPresent() == mek, "Mekanism recipe only with Mekanism");
        h.assertTrue(level.recipeAccess().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, BettaCraft.id("compat/enderio_stainless_steel_ingot"))).isPresent() == eio, "EnderIO recipe only with EnderIO");
        h.assertTrue(level.recipeAccess().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, BettaCraft.id("stainless_steel_ingot_from_crucible"))).isPresent(), "the crucible recipe");
        com.mojang.logging.LogUtils.getLogger().info("M24 stainless steel: {} foreign steel items rejected, mekanism {}, enderio {}", foreign, mek, eio);
        h.succeed();
    }

    // ------------------------------------------------------------------ pipes

    /** A pump moves water from one tank through pipes into another; temperature, salt, quality and nitrate go with it. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void pipesCarryWaterWithItsValues(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity a = tank(h, 1, 2, 1, 2, 2, 2);
        TankData ta = a.getTank();
        ta.setWaterTemp(27F);
        ta.setSalinity(10F);
        ta.setNitrate(30F);
        ta.setWaterQuality(0.6F);
        TankBlockEntity b = tank(h, 10, 2, 1, 2, 2, 2);
        TankInteractions.changeWater(level, b, -b.getTank().waterMb());
        long before = ta.waterMb();
        // a(x 1..2) -> glass pipe x3 -> pump x4 (east) -> steel pipes x5..9 -> b(x 10)
        pipe(h, false, new BlockPos(3, 2, 1));
        WaterPumpBlockEntity p = pump(h, new BlockPos(4, 2, 1), Direction.EAST);
        pipe(h, true, new BlockPos(5, 2, 1), new BlockPos(6, 2, 1), new BlockPos(7, 2, 1), new BlockPos(8, 2, 1), new BlockPos(9, 2, 1));
        h.assertTrue(level.getBlockState(h.absolutePos(new BlockPos(3, 2, 1))).getValue(PipeBlock.SIDES.get(Direction.WEST)), "the pipe joins the tank");
        h.assertTrue(level.getBlockState(h.absolutePos(new BlockPos(9, 2, 1))).getValue(PipeBlock.SIDES.get(Direction.EAST)), "and the other tank");
        int moved = batch(h, p);
        h.assertTrue(moved == ServerConfig.PIPE_GLASS_RATE.get() * PipeNetwork.WINDOW, "the glass pipe limits the batch: " + moved);
        TankData tb = b.getTank();
        h.assertTrue(tb.waterMb() == moved && ta.waterMb() == before - moved, "water moved: " + tb.waterMb());
        h.assertTrue(near(tb.waterTemp(), 27, 0.05) && near(tb.salinity(), 10, 0.05) && near(tb.nitrate(), 30, 0.05) && near(tb.waterQuality(), 0.6, 0.01),
                "values carried: " + tb.waterTemp() + " " + tb.salinity() + " " + tb.nitrate() + " " + tb.waterQuality());
        h.assertTrue(p.getBlockState().getValue(WaterPumpBlock.ACTIVE), "the pump shows it runs");
        // the pump at 0 % and with redstone stands still
        p.setPower(0);
        h.assertTrue(batch(h, p) == 0, "0 %: still");
        p.setPower(10);
        h.succeed();
    }

    /** Glass carries 50 mB/t, stainless steel 250: a pump between a tank and steel pipes moves its full 200 mB/t. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void steelPipesCarryMore(GameTestHelper h) {
        TankBlockEntity a = tank(h, 1, 2, 1, 2, 2, 2);
        TankBlockEntity b = tank(h, 10, 2, 1, 2, 2, 2);
        TankInteractions.changeWater(h.getLevel(), b, -b.getTank().waterMb());
        WaterPumpBlockEntity p = pump(h, new BlockPos(3, 2, 1), Direction.EAST);
        pipe(h, true, new BlockPos(4, 2, 1), new BlockPos(5, 2, 1), new BlockPos(6, 2, 1), new BlockPos(7, 2, 1), new BlockPos(8, 2, 1), new BlockPos(9, 2, 1));
        int moved = batch(h, p);
        h.assertTrue(moved == ServerConfig.PIPE_PUMP_RATE.get() * PipeNetwork.WINDOW, "the pump's full rate: " + moved);
        PipeNetwork net = PipeNetwork.of(h.getLevel(), h.absolutePos(new BlockPos(4, 2, 1)));
        h.assertTrue(net != null && net.rate() == ServerConfig.PIPE_STEEL_RATE.get() && net.size() == 6, "one steel network of 6 pipes");
        // one glass pipe in the line: the whole network carries what glass carries
        h.setBlock(new BlockPos(6, 2, 1), ModBlocks.GLASS_PIPE.get().defaultBlockState());
        PipeNetwork mixed = PipeNetwork.of(h.getLevel(), h.absolutePos(new BlockPos(4, 2, 1)));
        h.assertTrue(mixed != null && mixed.rate() == ServerConfig.PIPE_GLASS_RATE.get(), "the weakest pipe counts: " + (mixed == null ? -1 : mixed.rate()));
        h.succeed();
    }

    /** Other mods' fluid handlers at the ends: a vanilla cauldron is filled through a pipe (whole buckets) and emptied again. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void pipesFillAndEmptyACauldron(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity a = tank(h, 1, 2, 1, 2, 2, 2);
        long before = a.getTank().waterMb();
        WaterPumpBlockEntity p = pump(h, new BlockPos(3, 2, 1), Direction.EAST);
        pipe(h, true, new BlockPos(4, 2, 1));
        h.setBlock(new BlockPos(5, 2, 1), Blocks.CAULDRON.defaultBlockState());
        h.assertTrue(level.getBlockState(h.absolutePos(new BlockPos(4, 2, 1))).getValue(PipeBlock.SIDES.get(Direction.EAST)), "the pipe joins the cauldron");
        int moved = batch(h, p);
        BlockState cs = level.getBlockState(h.absolutePos(new BlockPos(5, 2, 1)));
        h.assertTrue(cs.is(Blocks.WATER_CAULDRON) && cs.getValue(LayeredCauldronBlock.LEVEL) == 3, "the cauldron is full: " + cs);
        h.assertTrue(moved == 1000 && a.getTank().waterMb() == before - 1000, "one bucket left the tank: " + moved);
        // the other way: the pump turned round pulls the cauldron empty into the tank
        h.setBlock(new BlockPos(3, 2, 1), ModBlocks.WATER_PUMP.get().defaultBlockState().setValue(WaterPumpBlock.FACING, Direction.WEST));
        WaterPumpBlockEntity back = (WaterPumpBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(3, 2, 1)));
        int got = batch(h, back);
        h.assertTrue(got == 1000 && level.getBlockState(h.absolutePos(new BlockPos(5, 2, 1))).is(Blocks.CAULDRON), "the cauldron is empty again: " + got);
        h.assertTrue(a.getTank().waterMb() == before, "the tank is full again");
        h.succeed();
    }

    /** With Mekanism (-Ppower): a Mekanism fluid tank at the end of a pipe takes our water and gives it back. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void pipesReachAMekanismTank(GameTestHelper h) {
        Identifier id = Identifier.fromNamespaceAndPath("mekanism", "basic_fluid_tank");
        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
            h.succeed(); // only with the power mods
            return;
        }
        ServerLevel level = h.getLevel();
        TankBlockEntity a = tank(h, 1, 2, 1, 2, 2, 2);
        a.getTank().setWaterTemp(28F);
        long before = a.getTank().waterMb();
        WaterPumpBlockEntity p = pump(h, new BlockPos(3, 2, 1), Direction.EAST);
        pipe(h, true, new BlockPos(4, 2, 1));
        h.setBlock(new BlockPos(5, 2, 1), BuiltInRegistries.BLOCK.getValue(id).defaultBlockState());
        h.assertTrue(level.getBlockState(h.absolutePos(new BlockPos(4, 2, 1))).getValue(PipeBlock.SIDES.get(Direction.EAST)), "the pipe joins the Mekanism tank");
        int moved = batch(h, p);
        var cap = org.overengineer.bettacraft.platform.Platform.get().fluidHandler(level, h.absolutePos(new BlockPos(5, 2, 1)), Direction.WEST);
        h.assertTrue(moved > 0 && cap != null && cap.getFluidInTank(0).getAmount() == moved && a.getTank().waterMb() == before - moved,
                "water in the Mekanism tank: " + moved + " / " + (cap == null ? "no handler" : cap.getFluidInTank(0)));
        com.mojang.logging.LogUtils.getLogger().info("M24 Mekanism tank holds {} with values {}", cap.getFluidInTank(0), WaterValues.from(cap.getFluidInTank(0)));
        // and back
        h.setBlock(new BlockPos(3, 2, 1), ModBlocks.WATER_PUMP.get().defaultBlockState().setValue(WaterPumpBlock.FACING, Direction.WEST));
        WaterPumpBlockEntity back = (WaterPumpBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(3, 2, 1)));
        h.assertTrue(batch(h, back) == moved && a.getTank().waterMb() == before, "back into the aquarium");
        h.assertTrue(near(a.getTank().waterTemp(), 28, 0.05), "with its temperature: " + a.getTank().waterTemp());
        h.succeed();
    }

    // ------------------------------------------------------------------ bulkhead and interior pipes

    /**
     * A bulkhead in a floor block: closed without outlets; with interior pipes to a nozzle, water pumped in from below
     * comes out of the nozzle, which then runs (ACTIVE) and makes a current where it sits; a strainer lets water out.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void bulkheadAndInteriorPipes(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 3, 2, 2, 2, 2);
        TankInteractions.changeWater(level, c, -2000);
        InteriorRegion r = AerationGameTests.region(h, c);
        InteriorPlumbing.forget();
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        BlockPos floor = h.absolutePos(new BlockPos(3, 3, 3));
        var res = PlumbingItem.installFromOutside(level, floor, sp, new ItemStack(ModItems.BULKHEAD.get()));
        h.assertTrue(res.consumesAction(), "bulkhead fitted");
        h.assertTrue(!PlumbingItem.installFromOutside(level, h.absolutePos(new BlockPos(3, 4, 3)), sp, new ItemStack(ModItems.BULKHEAD.get())).consumesAction(),
                "not into an upper block");
        long port = InteriorPlumbing.portInCell(r.equipment().plumbing(), 1, 1, r.scale());
        h.assertTrue(port != Long.MIN_VALUE, "its connection stands on the interior floor of that block");
        // below the floor block: a pump (up) fed from a reservoir
        ReservoirBlockEntity res0 = reservoir(h, new BlockPos(3, 1, 3), 8000, null);
        WaterPumpBlockEntity p = pump(h, new BlockPos(3, 2, 3), Direction.UP);
        long w0 = c.getTank().waterMb();
        h.assertTrue(batch(h, p) == 0 && c.getTank().waterMb() == w0, "a bulkhead without outlets stays closed");
        // pipes up from the connection and a nozzle facing east
        int px = BlockPos.getX(port), pz = BlockPos.getZ(port);
        AerationGameTests.put(h, r, px, 1, pz, ModBlocks.INTERIOR_PIPE.get().defaultBlockState());
        AerationGameTests.put(h, r, px, 2, pz, ModBlocks.INTERIOR_PIPE.get().defaultBlockState());
        BlockPos nozzle = AerationGameTests.put(h, r, px + 1, 2, pz, ModBlocks.NOZZLE.get().defaultBlockState().setValue(PlumbingBlock.FACING, Direction.EAST));
        int moved = batch(h, p);
        h.assertTrue(moved > 0 && c.getTank().waterMb() == w0 + moved, "water comes in through the nozzle: " + moved);
        h.assertTrue(res0.water().amount() == 8000 - moved, "from the reservoir");
        InteriorPlumbing.syncStates(level, c);
        h.assertTrue(AerationGameTests.interior(h).getBlockState(nozzle).getValue(PlumbingBlock.ACTIVE), "the nozzle runs (stream)");
        var cur = InteriorPlumbing.currents(level, c);
        h.assertTrue(cur.size() == 1 && cur.getFirst().dx() == 1 && cur.getFirst().strength() == 1.0, "one strong current to the east: " + cur);
        double wx = c.getTank().min().getX() + (px + 1 + 0.5) / r.scale(), wz = c.getTank().min().getZ() + (pz + 0.5) / r.scale();
        TankCurrents.invalidate(c);
        h.assertTrue(TankCurrents.factor(level, c, wx + 0.3, wz) > 1.5, "the water moves faster in front of the nozzle");
        h.assertTrue(TankCurrents.factor(level, c, wx - 0.6, wz + 0.9) == 1.0, "but not beside or behind it");
        // a strainer: the pump turned down pulls water out of the tank through it
        h.assertTrue(InteriorPlumbing.bulkheadAt(level, c, floor) != null && !InteriorPlumbing.bulkheadAt(level, c, floor).canDrain(), "no strainer yet");
        AerationGameTests.put(h, r, px - 1, 1, pz, ModBlocks.INTAKE_STRAINER.get().defaultBlockState());
        h.setBlock(new BlockPos(3, 2, 3), ModBlocks.WATER_PUMP.get().defaultBlockState().setValue(WaterPumpBlock.FACING, Direction.DOWN));
        WaterPumpBlockEntity down = (WaterPumpBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(3, 2, 3)));
        long w1 = c.getTank().waterMb();
        int out = batch(h, down);
        h.assertTrue(out > 0 && c.getTank().waterMb() == w1 - out, "water leaves through the strainer: " + out);
        // the time passes: the nozzle stops
        InteriorPlumbing.forget();
        InteriorPlumbing.syncStates(level, c);
        h.assertTrue(!AerationGameTests.interior(h).getBlockState(nozzle).getValue(PlumbingBlock.ACTIVE), "no water, no stream");
        h.succeed();
    }

    // ------------------------------------------------------------------ multiblock filter

    /**
     * A filter of two chambers (sponge, ceramic), a pump head, an inlet and an outlet at a tank: it forms, circulates the
     * water, cleans the tank by its media (12 + 24 water blocks), the media clog with time and are washed in a cauldron;
     * a missing outlet or a second pump head is reported.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void multiblockFilterFormsCleansAndClogs(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MultiblockFilter.forget();
        TankBlockEntity c = tank(h, 0, 2, 0, 2, 2, 2);
        for (int i = 0; i < 4; i++) AerationGameTests.add(h, c, TankmateSpecies.PLATY, LifeStage.ADULT);
        h.setBlock(new BlockPos(3, 2, 0), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
        h.setBlock(new BlockPos(3, 3, 0), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
        h.setBlock(new BlockPos(3, 4, 0), ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState());
        h.setBlock(new BlockPos(2, 2, 0), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.WEST));
        FilterChamberBlockEntity sponge = (FilterChamberBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(3, 2, 0)));
        FilterChamberBlockEntity ceramic = (FilterChamberBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(3, 3, 0)));
        sponge.setMedium(new ItemStack(ModItems.FILTER_FOAM.get()));
        ceramic.setMedium(new ItemStack(ModItems.CERAMIC_RINGS.get()));
        FilterPumpHeadBlockEntity head = (FilterPumpHeadBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(3, 4, 0)));
        h.assertTrue(head.run(level, true) == 0 && head.problem() == MultiblockFilter.Problem.NO_OUTLET, "no outlet yet: " + head.problem());
        h.setBlock(new BlockPos(2, 3, 0), ModBlocks.FILTER_OUTLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.WEST));
        FilterLinesGameTests.guardAll(level, head.getBlockPos()); // M40: every connection in the tank needs its guard
        double without = TankWater.filterBlocks(level, c);
        long w = c.getTank().waterMb();
        int moved = head.run(level, true);
        h.assertTrue(head.problem() == MultiblockFilter.Problem.NONE && head.chambers() == 2 && head.inlets() == 1 && head.outlets() == 1, "formed");
        h.assertTrue(moved > 0 && c.getTank().waterMb() == w, "it circulates the water (none lost): " + moved);
        h.assertTrue(c.getBlockPos().equals(head.servedTank()), "it serves the tank");
        double with = TankWater.filterBlocks(level, c);
        h.assertTrue(near(with - without, 36, 0.01), "sponge 12 + ceramic 24 water blocks: " + (with - without));
        h.assertTrue(level.getBlockState(head.getBlockPos()).getValue(FilterPartBlock.ACTIVE), "the pump head shows it runs");
        // 5 days: the sponge clogs twice as fast as the ceramic
        double s0 = sponge.dirt(), c0 = ceramic.dirt();
        head.clogDays(level, 5);
        double ds = sponge.dirt() - s0, dc = ceramic.dirt() - c0;
        h.assertTrue(ds > 0.04 && near(ds, 2 * dc, 0.003), "clogged: sponge +" + ds + ", ceramic +" + dc);
        head.run(level, true);
        h.assertTrue(head.cleanBlocks() < 36 - 0.5, "a dirty filter cleans less: " + head.cleanBlocks());
        // washing the sponge in a cauldron
        ItemStack dirty = sponge.medium().copy();
        sponge.setMedium(ItemStack.EMPTY);
        h.setBlock(new BlockPos(8, 2, 8), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.setItemInHand(InteractionHand.MAIN_HAND, dirty);
        BlockPos cauldron = h.absolutePos(new BlockPos(8, 2, 8));
        dirty.useOn(new UseOnContext(sp, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(cauldron), Direction.UP, cauldron, false)));
        h.assertTrue(dirty.getDamageValue() == 0, "washed clean");
        h.assertTrue(level.getBlockState(cauldron).getValue(LayeredCauldronBlock.LEVEL) == 2, "the cauldron lost a level");
        sponge.setMedium(dirty);
        h.assertTrue(sponge.getBlockState().getValue(FilterPartBlock.MEDIUM) == FilterMedium.SPONGE, "the chamber shows its sponge");
        // a second pump head: not formed
        h.setBlock(new BlockPos(4, 2, 0), ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState());
        head.run(level, true);
        h.assertTrue(head.problem() == MultiblockFilter.Problem.TWO_HEADS, "two heads: " + head.problem());
        h.succeed();
    }

    /** features.equipment.multiblockFilter off: the filter does not clean (own batch). */
    @GameTest(template = EMPTY, batch = "bettacraft_m24_filter_off", timeoutTicks = 100)
    public static void multiblockFilterOff(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        boolean saved = Features.own(Feature.MULTIBLOCK_FILTER);
        try {
            Features.set(Feature.MULTIBLOCK_FILTER, false);
            MultiblockFilter.forget();
            TankBlockEntity c = tank(h, 0, 2, 0, 2, 2, 2);
            h.setBlock(new BlockPos(3, 2, 0), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
            h.setBlock(new BlockPos(3, 3, 0), ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState());
            h.setBlock(new BlockPos(2, 2, 0), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.WEST));
            h.setBlock(new BlockPos(3, 2, 1), ModBlocks.FILTER_OUTLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.SOUTH));
            ((FilterChamberBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(3, 2, 0)))).setMedium(new ItemStack(ModItems.CERAMIC_RINGS.get()));
            FilterPumpHeadBlockEntity head = (FilterPumpHeadBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(3, 3, 0)));
            h.assertTrue(head.run(level, true) == 0 && MultiblockFilter.serving(level, c)[1] == 0, "switched off: nothing");
        } finally {
            Features.set(Feature.MULTIBLOCK_FILTER, saved);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ nitrate and water changes

    /**
     * Nitrate per difficulty: the rate follows 1/20, 1/14, 1/7 (EASY, NORMAL, REALISTIC), OFF makes none; a 25 % water
     * change takes a quarter; from 50 mg/L the fish breed at half speed, from 120 the water is bad. Old tanks load with 0.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m24_nitrate", timeoutTicks = 200)
    public static void nitratePerDifficulty(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var saved = ServerConfig.WATER_CHANGE_DIFFICULTY.get();
        try {
            TankBlockEntity c = tank(h, 1, 2, 1, 4, 2, 2); // 16 water blocks
            for (int i = 0; i < 8; i++) AerationGameTests.add(h, c, TankmateSpecies.PLATY, LifeStage.ADULT);
            double load = TankNitrate.load(level, c.getTank());
            h.assertTrue(load > 0.1, "stocked: load " + load);
            double[] rate = new double[4];
            for (var d : ServerConfig.WaterChangeDifficulty.values()) {
                ServerConfig.WATER_CHANGE_DIFFICULTY.set(d);
                rate[d.ordinal()] = TankNitrate.ratePerDay(level, c);
            }
            h.assertTrue(rate[0] == 0, "OFF: no nitrate");
            h.assertTrue(near(rate[1] * 20, rate[2] * 14, 1e-6) && near(rate[2] * 14, rate[3] * 7, 1e-6), "rates by interval: " + java.util.Arrays.toString(rate));
            ServerConfig.WATER_CHANGE_DIFFICULTY.set(ServerConfig.WaterChangeDifficulty.NORMAL);
            c.getTank().setNitrate(0);
            TankNitrate.step(level, c, 10);
            h.assertTrue(near(c.getTank().nitrate(), rate[2] * 10, 0.01), "10 days: " + c.getTank().nitrate());
            // a 25 % water change by bucket takes a quarter
            c.getTank().setNitrate(40);
            long quarter = c.getTank().capacityMb() / 4;
            TankInteractions.changeWater(level, c, -quarter);
            TankInteractions.changeWater(level, c, quarter);
            h.assertTrue(near(c.getTank().nitrate(), 30, 0.01), "25 % fresh water: 40 -> " + c.getTank().nitrate());
            h.assertTrue(c.getTank().lastWaterChange() >= 0, "the water change is noted");
            c.getTank().setNitrate(60);
            h.assertTrue(TankNitrate.high(c.getTank()) && TankNitrate.breedingFactor(c.getTank()) == 0.5 && !TankWater.isBad(c.getTank()),
                    "60: less breeding, not yet bad water");
            h.assertTrue(TankNitrate.algaeFactor(c.getTank()) > 1, "more algae");
            c.getTank().setNitrate(130);
            h.assertTrue(TankWater.isBad(c.getTank()), "130: bad water");
            ServerConfig.WATER_CHANGE_DIFFICULTY.set(ServerConfig.WaterChangeDifficulty.OFF);
            h.assertTrue(!TankWater.isBad(c.getTank()) && TankNitrate.breedingFactor(c.getTank()) == 1, "OFF: nitrate does nothing");
            // tanks of earlier versions have no nitrate value: 0
            CompoundTag old = c.getTank().save();
            old.remove("nitrate");
            old.remove("last_water_change");
            TankData loaded = TankData.load(old);
            h.assertTrue(loaded.nitrate() == 0 && loaded.lastWaterChange() == -1, "an old tank starts at 0");
        } finally {
            ServerConfig.WATER_CHANGE_DIFFICULTY.set(saved);
        }
        h.succeed();
    }

    /** features.pipes.enabled off: pumps stand still and pipes carry nothing (own batch). */
    @GameTest(template = EMPTY, batch = "bettacraft_m24_pipes_off", timeoutTicks = 100)
    public static void pipesOff(GameTestHelper h) {
        boolean saved = Features.own(Feature.PIPES);
        try {
            Features.set(Feature.PIPES, false);
            TankBlockEntity a = tank(h, 1, 2, 1, 2, 2, 2);
            WaterPumpBlockEntity p = pump(h, new BlockPos(3, 2, 1), Direction.EAST);
            reservoir(h, new BlockPos(4, 2, 1), 0, null);
            h.assertTrue(batch(h, p) == 0 && a.getTank().waterMb() == a.getTank().capacityMb(), "switched off: the pump stands still");
        } finally {
            Features.set(Feature.PIPES, saved);
        }
        h.succeed();
    }
}
