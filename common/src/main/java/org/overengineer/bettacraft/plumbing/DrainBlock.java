package org.overengineer.bettacraft.plumbing;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;

/**
 * M68 part B: the drain ({@code features.pipes.drain}). A pipe joins it on every face; it takes all the water that comes down the
 * pipe and discards it: where the water change controller's waste water line ends (or right at its waste water outlet). It holds
 * nothing and gives nothing; other mods' pipes may use it as a sink too. Only water (with its values), no other fluid.
 */
public class DrainBlock extends Block implements org.overengineer.bettacraft.util.BlockRemoval {
    public static final MapCodec<DrainBlock> CODEC = simpleCodec(DrainBlock::new);

    public DrainBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        PipeNetwork.invalidate(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        PipeNetwork.invalidate(level, pos);
    }

    /** The drain's fluid handler on any face: fills of water are taken and discarded (nothing with the switch off). */
    public static IFluidHandler handler(Level level, BlockPos pos, @Nullable Direction side) {
        return SINK;
    }

    private static final IFluidHandler SINK = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return Features.enabled(Feature.DRAIN) ? 1000 : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return org.overengineer.bettacraft.registry.ModFluids.isWater(stack.getFluid());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !isFluidValid(0, resource) || !Features.enabled(Feature.DRAIN)) return 0;
            return resource.getAmount();
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };
}
