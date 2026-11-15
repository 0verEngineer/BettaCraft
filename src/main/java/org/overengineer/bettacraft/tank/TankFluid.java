package org.overengineer.bettacraft.tank;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing;

/**
 * Water of a tank for pipes and pumps of other mods (M9): fill = fresh water (dilutes the waste like a bucket), drain
 * = used water. Draining stops at the minimum fill while animals live in the tank, like the empty bucket.
 * <p>
 * M24: the water that leaves carries the tank's values ({@link WaterValues}: temperature, salt, quality, nitrate);
 * water with values that comes in mixes all of them (water without values is fresh tap water). From below, a floor block
 * with a bulkhead ({@link InteriorPlumbing}) only lets water in through the outlets of its interior pipes (lily pipe,
 * nozzle, spray bar) and out through an intake strainer; their current then acts where they sit.
 */
public final class TankFluid implements IFluidHandler {
    private final Level level;
    private final BlockPos pos;
    @Nullable
    private final Direction side;

    public TankFluid(Level level, BlockPos pos) {
        this(level, pos, null);
    }

    public TankFluid(Level level, BlockPos pos, @Nullable Direction side) {
        this.level = level;
        this.pos = pos.immutable();
        this.side = side;
    }

    @Nullable
    private TankBlockEntity controller() {
        if (!(level instanceof ServerLevel) || !level.isLoaded(pos)) return null;
        return TankMultiblock.getController(level, pos);
    }

    /** M24: the bulkhead under this block (only seen from below), or null. */
    @Nullable
    private InteriorPlumbing.Bulkhead bulkhead(TankBlockEntity c) {
        if (side != Direction.DOWN || !(level instanceof ServerLevel s)) return null;
        return InteriorPlumbing.bulkheadAt(s, c, pos);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        TankBlockEntity c = controller();
        if (c == null || c.getTank().waterMb() <= 0) return FluidStack.EMPTY;
        return WaterValues.stack((int) Math.min(Integer.MAX_VALUE, c.getTank().waterMb()), WaterValues.of(c.getTank()));
    }

    @Override
    public int getTankCapacity(int tank) {
        TankBlockEntity c = controller();
        return c == null ? 0 : (int) Math.min(Integer.MAX_VALUE, c.getTank().capacityMb());
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        // M25: also reverse osmosis and sea water; M26: live food suspension from the live food pump
        return org.overengineer.bettacraft.registry.ModFluids.isWater(stack.getFluid()) || org.overengineer.bettacraft.food.LiveFood.isSuspension(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
        TankBlockEntity c = controller();
        if (c == null) return 0;
        InteriorPlumbing.Bulkhead b = bulkhead(c);
        if (b != null && !b.canFill()) return 0;
        if (org.overengineer.bettacraft.food.LiveFood.isSuspension(resource)) {
            int n = org.overengineer.bettacraft.food.LiveFood.fillTank((ServerLevel) level, c, resource, action);
            if (n > 0 && b != null && action.execute()) b.flowIn(level.getGameTime(), n);
            return n;
        }
        TankData t = c.getTank();
        // M33.1: up to the capacity and the limit of a fixed tank type (a pipe gets the part that fits, not a refusal)
        long n = Math.min(resource.getAmount(), org.overengineer.bettacraft.terrarium.WaterLimit.room((ServerLevel) level, c));
        if (n <= 0) return 0;
        if (action.execute()) {
            TankInteractions.addWater((ServerLevel) level, c, n, WaterValues.from(resource, TankCare.ambient((ServerLevel) level, t)));
            if (b != null) b.flowIn(level.getGameTime(), n);
        }
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
        if (resource.isEmpty() || !org.overengineer.bettacraft.registry.ModFluids.isWater(resource.getFluid())) return FluidStack.EMPTY;
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        TankBlockEntity c = controller();
        if (c == null || maxDrain <= 0) return FluidStack.EMPTY;
        InteriorPlumbing.Bulkhead b = bulkhead(c);
        if (b != null && !b.canDrain()) return FluidStack.EMPTY;
        long n = Math.min(maxDrain, drainable(c));
        if (n <= 0) return FluidStack.EMPTY;
        WaterValues v = WaterValues.of(c.getTank());
        if (action.execute()) {
            TankInteractions.changeWater((ServerLevel) level, c, -n);
            if (b != null) b.flowOut(level.getGameTime(), n);
        }
        return WaterValues.stack((int) n, v);
    }
}
