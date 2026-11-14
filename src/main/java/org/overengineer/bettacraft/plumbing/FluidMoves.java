package org.overengineer.bettacraft.plumbing;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.overengineer.bettacraft.tank.WaterValues;

/** M24: moving water between two fluid handlers (pumps, filters, the water change controller, the top-off). */
public final class FluidMoves {
    private FluidMoves() {}

    /** The fluid handler of the block next to {@code pos} on side {@code d} (seen from {@code pos}), or null. */
    @Nullable
    public static IFluidHandler neighbour(Level level, BlockPos pos, Direction d) {
        BlockPos n = pos.relative(d);
        if (!level.isLoaded(n)) return null;
        return level.getCapability(Capabilities.FluidHandler.BLOCK, n, d.getOpposite());
    }

    /** Water that {@code from} can give, up to {@code max} (looked at, not taken). */
    public static FluidStack peekWater(IFluidHandler from, int max) {
        FluidStack sim = from.drain(max, IFluidHandler.FluidAction.SIMULATE);
        return sim.isEmpty() || !sim.getFluid().isSame(Fluids.WATER) ? FluidStack.EMPTY : sim;
    }

    /** How much of {@code water} {@code to} takes (with the plain-water fallback for tanks that refuse the values). */
    public static int accepts(IFluidHandler to, FluidStack water) {
        int n = to.fill(water.copy(), IFluidHandler.FluidAction.SIMULATE);
        if (n <= 0 && WaterValues.from(water) != null) n = to.fill(new FluidStack(water.getFluidHolder(), water.getAmount()), IFluidHandler.FluidAction.SIMULATE);
        return n;
    }

    private static int fill(IFluidHandler to, FluidStack water) {
        int n = to.fill(water.copy(), IFluidHandler.FluidAction.EXECUTE);
        if (n <= 0 && WaterValues.from(water) != null) n = to.fill(new FluidStack(water.getFluidHolder(), water.getAmount()), IFluidHandler.FluidAction.EXECUTE);
        return n;
    }

    /** Moves up to {@code max} mB of water from {@code from} to {@code to}. Returns the mB moved. */
    public static int move(IFluidHandler from, IFluidHandler to, int max) {
        return move(from, to, max, false);
    }

    /**
     * As {@link #move(IFluidHandler, IFluidHandler, int)}; with {@code circulate} a full target may be the source itself (a
     * filter circulating a full tank): it takes first and gives back what the target did not take.
     */
    public static int move(IFluidHandler from, IFluidHandler to, int max, boolean circulate) {
        if (max <= 0) return 0;
        FluidStack sim = peekWater(from, max);
        if (sim.isEmpty()) return 0;
        int room = accepts(to, sim);
        if (room <= 0) {
            if (!circulate) return 0;
            room = sim.getAmount();
        }
        FluidStack got = from.drain(sim.copyWithAmount(Math.min(room, sim.getAmount())), IFluidHandler.FluidAction.EXECUTE);
        if (got.isEmpty()) return 0;
        int put = fill(to, got);
        if (put < got.getAmount()) from.fill(got.copyWithAmount(got.getAmount() - put), IFluidHandler.FluidAction.EXECUTE); // give back the rest
        return Math.max(0, put);
    }
}
