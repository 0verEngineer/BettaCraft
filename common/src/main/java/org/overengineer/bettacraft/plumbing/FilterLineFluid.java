package org.overengineer.bettacraft.plumbing;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.filter.FilterPortBlockEntity;

/**
 * M40 part b: the fluid handler of a fresh or waste water line (a single pipe or a bundle that carries one), for the pumps
 * and pipes of other mods, AE2/RS export and import buses and our water pump. Water pushed into the fresh water line goes
 * into the fresh water ports of the filter the line leads to; water pulled from the waste water line comes out of its waste
 * water ports. Nothing else: the intake and the return line stay inside the filter (no handler).
 */
public final class FilterLineFluid implements IFluidHandler {
    private final Level level;
    private final BlockPos pos;
    private final boolean fresh, waste;

    private FilterLineFluid(Level level, BlockPos pos, boolean fresh, boolean waste) {
        this.level = level;
        this.pos = pos.immutable();
        this.fresh = fresh;
        this.waste = waste;
    }

    /** The handler of the block at {@code pos}, or null when it carries neither water line. */
    @Nullable
    public static IFluidHandler of(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
        int lines = state.getBlock() instanceof FilterPipeBlock f ? FilterPipes.with(0, f.line(), FilterPipeBlock.slot(state))
                : be instanceof FilterPipeBlockEntity p ? p.lines() : 0;
        boolean fresh = FilterPipes.present(FilterPipes.slot(lines, PipeLine.FRESH)), waste = FilterPipes.present(FilterPipes.slot(lines, PipeLine.WASTE));
        return fresh || waste ? new FilterLineFluid(level, pos, fresh, waste) : null;
    }

    private boolean on() {
        return !level.isClientSide && Features.enabled(Feature.FILTER_WATER);
    }

    private java.util.List<FilterPortBlockEntity> ports(PipeLine line) {
        java.util.List<FilterPortBlockEntity> out = new java.util.ArrayList<>();
        for (BlockPos p : FilterLines.reach(level, pos, line).connectors()) if (level.getBlockEntity(p) instanceof FilterPortBlockEntity port) out.add(port);
        return out;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        if (!waste || !on()) return FluidStack.EMPTY;
        for (FilterPortBlockEntity p : ports(PipeLine.WASTE)) if (p.water().amount() > 0) return p.water().getFluidInTank(0);
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return FilterPortBlockEntity.CAPACITY;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return fresh && org.overengineer.bettacraft.registry.ModFluids.isWater(stack.getFluid());
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!fresh || resource.isEmpty() || !on() || !org.overengineer.bettacraft.registry.ModFluids.isWater(resource.getFluid())) return 0;
        int done = 0;
        for (FilterPortBlockEntity p : ports(PipeLine.FRESH)) {
            if (done >= resource.getAmount()) break;
            done += p.water().fill(resource.copyWithAmount(resource.getAmount() - done), action);
        }
        return done;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !org.overengineer.bettacraft.registry.ModFluids.isWater(resource.getFluid())) return FluidStack.EMPTY;
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (!waste || maxDrain <= 0 || !on()) return FluidStack.EMPTY;
        for (FilterPortBlockEntity p : ports(PipeLine.WASTE)) {
            FluidStack got = p.water().drain(maxDrain, action);
            if (!got.isEmpty()) return got;
        }
        return FluidStack.EMPTY;
    }
}
