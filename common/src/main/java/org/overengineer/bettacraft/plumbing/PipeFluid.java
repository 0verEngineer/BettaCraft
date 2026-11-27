package org.overengineer.bettacraft.plumbing;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;

/**
 * M24: the fluid handler of a pipe, seen from the block on face {@code side}: filling pushes into the network (to every
 * end but that block), draining pulls from the network's ends. Only water (with its values).
 */
public final class PipeFluid implements IFluidHandler {
    private final Level level;
    private final BlockPos pos;
    @Nullable
    private final BlockPos from;
    @Nullable
    private final Direction side;

    public PipeFluid(Level level, BlockPos pos, @Nullable Direction side) {
        this.level = level;
        this.pos = pos.immutable();
        this.side = side;
        this.from = side == null ? null : pos.relative(side).immutable();
    }

    /** M68: the network of the pipe (a bundle: the pipe joined on this face; none joined there: its first pipe). */
    @Nullable
    private PipeNetwork net() {
        if (level.isClientSide) return null;
        if (side != null && level.getBlockState(pos).getBlock() instanceof PipeBundleBlock) {
            PipeNetwork n = PipeNetwork.towards(level, pos, side);
            if (n != null) return n;
        }
        return PipeNetwork.of(level, pos);
    }

    /**
     * M66: the water that lies at the ends of this pipe's network (reservoirs, other mods' tanks), apart from the block this handler is
     * seen from: what the pipe could deliver in time, not what one window of its throughput moves. 0 on the client.
     */
    public int stored() {
        PipeNetwork n = net();
        if (n == null) return 0;
        int sum = 0;
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        for (PipeNetwork.End e : n.ends()) {
            if (e.pos().equals(from) || !seen.add(e.pos()) || !level.isLoaded(e.pos())) continue;
            IFluidHandler h = org.overengineer.bettacraft.platform.Platform.get().fluidHandler(level, e.pos(), e.face());
            if (h == null || h instanceof PipeFluid) continue;
            for (int i = 0; i < h.getTanks(); i++) {
                FluidStack s = h.getFluidInTank(i);
                if (org.overengineer.bettacraft.registry.ModFluids.isWater(s.getFluid())) sum += s.getAmount();
            }
        }
        return sum;
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
        // M25: also reverse osmosis and sea water; M26: live food suspension (only pushed, never pulled)
        return org.overengineer.bettacraft.registry.ModFluids.isWater(stack.getFluid()) || org.overengineer.bettacraft.food.LiveFood.isSuspension(stack)
                || org.overengineer.bettacraft.fertilizer.Fertilizer.isFluid(stack); // M37: fertilizer from the fertilizer maker (only pushed)
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
        PipeNetwork n = net();
        return n == null ? 0 : n.fill(level, resource, action, from);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !org.overengineer.bettacraft.registry.ModFluids.isWater(resource.getFluid())) return FluidStack.EMPTY;
        PipeNetwork n = net();
        return n == null ? FluidStack.EMPTY : n.drain(level, resource.getAmount(), resource, action, from);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        PipeNetwork n = net();
        return n == null ? FluidStack.EMPTY : n.drain(level, maxDrain, null, action, from);
    }
}
