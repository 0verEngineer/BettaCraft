package org.overengineer.bettacraft.filter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.plumbing.WaterStore;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/**
 * M40 part b (F1): the fresh water and the waste water port of a multiblock filter. Each holds a small store of water
 * ({@link #CAPACITY}). The fresh water port takes water from whatever pushes into it or into its fresh water line (a water
 * pump, an ME/RS export bus, the pipes of other mods); the level sensor and the water change controller on the filter
 * draw from it ({@link FilterWater}). Old water of a water change goes into the waste water port: it pushes it on to its
 * line's ends (a tank, a reservoir, another mod's pipe or storage) and keeps what they do not take until something pulls it
 * (another mod's pipe, a water pump). Seen from outside, the fresh port only takes water and the waste port only gives it.
 */
public class FilterPortBlockEntity extends BlockEntity {
    public static final int CAPACITY = 8000;
    private final WaterStore water = new WaterStore(CAPACITY, this::setChanged);
    private final IFluidHandler outside = new Outside();

    public FilterPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FILTER_PORT.get(), pos, state);
    }

    /** The water in the port (inside: both ways). */
    public WaterStore water() {
        return water;
    }

    public boolean fresh() {
        return getBlockState().getBlock() instanceof FilterPartBlock p && p.part() == FilterPartBlock.Part.FRESH;
    }

    public Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(FilterPartBlock.FACING) ? s.getValue(FilterPartBlock.FACING) : Direction.NORTH;
    }

    /** The fluid handler other blocks see: the fresh port takes water, the waste port gives it. */
    public IFluidHandler outside() {
        return outside;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, FilterPortBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0 || be.fresh() || be.water.amount() <= 0) return;
        if (!Features.enabled(Feature.FILTER_WATER)) return;
        FilterWater.pushOn((ServerLevel) level, be); // the waste port: on to the ends of its line
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        water.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water.load(tag, registries);
    }

    private final class Outside implements IFluidHandler {
        private boolean on() {
            return level != null && !level.isClientSide && Features.enabled(Feature.FILTER_WATER);
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return water.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return fresh() && water.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return on() && fresh() ? water.fill(resource, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return on() && !fresh() ? water.drain(resource, action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return on() && !fresh() ? water.drain(maxDrain, action) : FluidStack.EMPTY;
        }
    }
}
