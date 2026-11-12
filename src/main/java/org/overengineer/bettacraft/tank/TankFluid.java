package org.overengineer.bettacraft.tank;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.overengineer.bettacraft.config.ServerConfig;

/**
 * Water of a tank for pipes and pumps of other mods (M9): fill = fresh water (dilutes the waste like a bucket), drain
 * = used water. Draining stops at the minimum fill while animals live in the tank, like the empty bucket.
 */
public final class TankFluid implements IFluidHandler {
    private final Level level;
    private final BlockPos pos;

    public TankFluid(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    @Nullable
    private TankBlockEntity controller() {
        if (!(level instanceof ServerLevel) || !level.isLoaded(pos)) return null;
        return TankMultiblock.getController(level, pos);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        TankBlockEntity c = controller();
        if (c == null || c.getTank().waterMb() <= 0) return FluidStack.EMPTY;
        return new FluidStack(Fluids.WATER, (int) Math.min(Integer.MAX_VALUE, c.getTank().waterMb()));
    }

    @Override
    public int getTankCapacity(int tank) {
        TankBlockEntity c = controller();
        return c == null ? 0 : (int) Math.min(Integer.MAX_VALUE, c.getTank().capacityMb());
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.getFluid().isSame(Fluids.WATER);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
        TankBlockEntity c = controller();
        if (c == null) return 0;
        TankData t = c.getTank();
        long n = Math.min(resource.getAmount(), t.capacityMb() - t.waterMb());
        if (n <= 0) return 0;
        if (action.execute()) TankInteractions.changeWater((ServerLevel) level, c, n);
        return (int) n;
    }

    /** Water that may leave: all of it in an empty tank, down to the minimum fill with animals inside. */
    private long drainable(TankBlockEntity c) {
        TankData t = c.getTank();
        if (t.inhabitants().isEmpty()) return t.waterMb();
        long min = Math.round(ServerConfig.getOr(ServerConfig.TANK_MIN_FILL_TO_INSERT, 0.5) * t.capacityMb());
        return Math.max(0, t.waterMb() - min);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) return FluidStack.EMPTY;
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        TankBlockEntity c = controller();
        if (c == null || maxDrain <= 0) return FluidStack.EMPTY;
        long n = Math.min(maxDrain, drainable(c));
        if (n <= 0) return FluidStack.EMPTY;
        if (action.execute()) TankInteractions.changeWater((ServerLevel) level, c, -n);
        return new FluidStack(Fluids.WATER, (int) n);
    }
}
