package org.overengineer.bettacraft.plumbing;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * M24: the fluid handler of a pipe, seen from the block on face {@code side}: filling pushes into the network (to every
 * end but that block), draining pulls from the network's ends. Only water (with its values).
 */
public final class PipeFluid implements IFluidHandler {
    private final Level level;
    private final BlockPos pos;
    @Nullable
    private final BlockPos from;

    public PipeFluid(Level level, BlockPos pos, @Nullable Direction side) {
        this.level = level;
        this.pos = pos.immutable();
        this.from = side == null ? null : pos.relative(side).immutable();
    }

    @Nullable
    private PipeNetwork net() {
        return level.isClientSide ? null : PipeNetwork.of(level, pos);
    }

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
        PipeNetwork n = net();
        return n == null ? 0 : n.rate() * PipeNetwork.WINDOW;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return org.overengineer.bettacraft.registry.ModFluids.isWater(stack.getFluid()); // M25: also reverse osmosis and sea water
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
        PipeNetwork n = net();
        return n == null ? 0 : n.fill(level, resource, action, from);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) return FluidStack.EMPTY;
        PipeNetwork n = net();
        return n == null ? FluidStack.EMPTY : n.drain(level, resource.getAmount(), null, action, from);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        PipeNetwork n = net();
        return n == null ? FluidStack.EMPTY : n.drain(level, maxDrain, null, action, from);
    }
}
