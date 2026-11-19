package org.overengineer.bettacraft.fabric;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.overengineer.bettacraft.platform.fluid.FluidSpec;

/**
 * M52: our fluids on Fabric (NeoForge: its BaseFlowingFluid with a fluid type). Water-like, never a source from two, no
 * block of their own: they live in tanks, pipes and buckets only (a poured bucket gives water, see the bucket items).
 */
public abstract class BettaFluid extends FlowingFluid {
    protected final FluidSpec spec;

    protected BettaFluid(FluidSpec spec) {
        this.spec = spec;
    }

    @Override
    public Fluid getFlowing() {
        return spec.flowing().get();
    }

    @Override
    public Fluid getSource() {
        return spec.source().get();
    }

    @Override
    public Item getBucket() {
        return spec.bucket() == null ? Items.AIR : spec.bucket().get();
    }

    @Override
    protected boolean canConvertToSource(net.minecraft.server.level.ServerLevel level) {
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockEntity be = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        net.minecraft.world.level.block.Block.dropResources(state, level, pos, be);
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return 4;
    }

    @Override
    protected int getDropOff(LevelReader level) {
        return 1;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return 5;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
        return direction == Direction.DOWN && !isSame(fluid);
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == getSource() || fluid == getFlowing();
    }

    public static final class Source extends BettaFluid {
        public Source(FluidSpec spec) {
            super(spec);
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }
    }

    public static final class Flowing extends BettaFluid {
        public Flowing(FluidSpec spec) {
            super(spec);
            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }
    }
}
