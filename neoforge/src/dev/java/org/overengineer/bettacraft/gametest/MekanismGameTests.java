package org.overengineer.bettacraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.osmosis.RoUnitBlockEntity;
import org.overengineer.bettacraft.plumbing.FluidMoves;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModFluids;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * M32 part c, dev group {@code -Ppower} (Mekanism in run/power-mods): our fluids (reverse osmosis water, sea water, live
 * food) go into Mekanism's fluid tanks and come out again through the plain fluid capability, and the reverse osmosis
 * water of our unit moves into a Mekanism tank (the way pumps and pipes of both mods move it). Without Mekanism the test
 * passes at once. Mekanism ids only.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MekanismGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private MekanismGameTests() {}

    private static IFluidHandler mekTank(GameTestHelper h, BlockPos rel) {
        Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mekanism:basic_fluid_tank"));
        if (b == Blocks.AIR) throw new GameTestAssertException("no Mekanism fluid tank");
        h.setBlock(rel, b.defaultBlockState());
        IFluidHandler cap = org.overengineer.bettacraft.platform.Platform.get().fluidHandler(h.getLevel(), h.absolutePos(rel), Direction.UP);
        if (cap == null) throw new GameTestAssertException("the Mekanism tank has no fluid capability");
        return cap;
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void ourFluidsInMekanismTanks(GameTestHelper h) {
        if (!org.overengineer.bettacraft.platform.Platform.get().isModLoaded("mekanism")) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        Fluid[] ours = {ModFluids.RO_WATER.get(), ModFluids.SEA_WATER.get(), ModFluids.LIVE_FOOD.get()};
        for (int i = 0; i < ours.length; i++) {
            IFluidHandler t = mekTank(h, new BlockPos(2 + 2 * i, 2, 2));
            FluidStack in = new FluidStack(ours[i], 1000);
            h.assertTrue(t.fill(in, IFluidHandler.FluidAction.EXECUTE) == 1000, "Mekanism takes " + BuiltInRegistries.FLUID.getKey(ours[i]));
            FluidStack out = t.drain(400, IFluidHandler.FluidAction.EXECUTE);
            h.assertTrue(out.getFluid() == ours[i] && out.getAmount() == 400, "and gives it back: " + out.getFluid() + " " + out.getAmount());
        }
        // reverse osmosis water from our unit into a Mekanism tank
        BlockPos water = new BlockPos(2, 2, 8), ro = new BlockPos(3, 2, 8);
        h.setBlock(water, Blocks.WATER.defaultBlockState());
        h.setBlock(ro, ModBlocks.RO_UNIT.get().defaultBlockState());
        RoUnitBlockEntity unit = (RoUnitBlockEntity) level.getBlockEntity(h.absolutePos(ro));
        if (unit == null) throw new GameTestAssertException("no RO unit");
        unit.parts().setStackInSlot(RoUnitBlockEntity.MEMBRANE, new ItemStack(ModItems.RO_MEMBRANE.get()));
        unit.parts().setStackInSlot(RoUnitBlockEntity.PREFILTER, new ItemStack(ModItems.RO_PREFILTER.get()));
        for (int i = 0; i < 30; i++) unit.step(level, 20);
        IFluidHandler mek = mekTank(h, new BlockPos(4, 2, 8));
        IFluidHandler from = org.overengineer.bettacraft.platform.Platform.get().fluidHandler(level, h.absolutePos(ro), Direction.EAST);
        h.assertTrue(from != null, "the RO unit has a fluid capability");
        int moved = FluidMoves.move(from, mek, 1000);
        FluidStack got = mek.getFluidInTank(0);
        h.assertTrue(moved > 0 && got.getFluid() == ModFluids.RO_WATER.get() && got.getAmount() == moved,
                "RO water in the Mekanism tank: " + moved + " " + got.getFluid());
        h.succeed();
    }
}
