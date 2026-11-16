package org.overengineer.bettacraft.plumbing;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.overengineer.bettacraft.equipment.AirPumpBlockEntity;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.WaterValues;

/**
 * M24: where the level sensor and the water change controller get fresh water: the fluid handlers next to them (a
 * reservoir, a pipe, another mod's tank, an AE2/RS interface) and water source blocks next to them (an endless supply of
 * tap water). The tank itself and the multiblock filter are never a source.
 */
public final class WaterSources {
    private WaterSources() {}

    /** The tank a device next to a tank serves (a tank block below or beside it), or the tank of a multiblock filter next to it. */
    @Nullable
    public static TankBlockEntity tank(Level level, BlockPos pos, boolean viaFilter) {
        TankBlockEntity c = AirPumpBlockEntity.findTank(level, pos);
        if (c == null && viaFilter) c = org.overengineer.bettacraft.filter.MultiblockFilter.tankNextTo(level, pos);
        return c;
    }

    /** Whether a water source block touches the device (endless tap water). */
    public static boolean endless(Level level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (!level.isLoaded(n)) continue;
            // a real water source block (not a tank, whose glass reports water too)
            BlockState s = level.getBlockState(n);
            if (s.is(net.minecraft.world.level.block.Blocks.WATER) && s.getFluidState().is(FluidTags.WATER) && s.getFluidState().isSource()) return true;
        }
        return false;
    }

    /** The fluid handlers next to the device that hold water (not tanks, not filter parts). */
    public static List<IFluidHandler> handlers(Level level, BlockPos pos) {
        List<IFluidHandler> out = new ArrayList<>();
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (!level.isLoaded(n)) continue;
            BlockState s = level.getBlockState(n);
            if (s.getBlock() instanceof TankBlock || s.getBlock() instanceof org.overengineer.bettacraft.filter.FilterPartBlock) continue;
            IFluidHandler h = FluidMoves.neighbour(level, pos, d);
            if (h != null) out.add(h);
        }
        return out;
    }

    /** Fresh water the handlers next to the device can give (salty water does not count). */
    public static long available(Level level, BlockPos pos, boolean freshOnly) {
        return at(level, pos).available(freshOnly);
    }

    /** A portion of water taken from a source: amount and values (null = tap water). */
    public record Portion(int mb, @Nullable WaterValues values) {}

    /** Takes up to {@code max} mB of water from the handlers next to the device (fresh only if asked). */
    public static List<Portion> take(Level level, BlockPos pos, int max, boolean freshOnly) {
        return at(level, pos).take(max, freshOnly);
    }

    /** Whether a handler next to the device holds only salty water (for the status line). */
    public static boolean onlySalty(Level level, BlockPos pos) {
        return at(level, pos).onlySalty();
    }

    /** The water next to a device: endless from a water source block, else from the fluid handlers next to it. */
    public static Supply at(Level level, BlockPos pos) {
        return new Supply(endless(level, pos), handlers(level, pos));
    }

    /**
     * M40 part b: where a device gets its water: endless tap water, or a list of fluid handlers (those next to it; on a
     * multiblock filter also its fresh water ports and their lines, {@code filter.FilterWater#supply}).
     */
    public static final class Supply {
        private final boolean endless;
        private final List<IFluidHandler> handlers;

        public Supply(boolean endless, List<IFluidHandler> handlers) {
            this.endless = endless;
            this.handlers = handlers;
        }

        public boolean endless() {
            return endless;
        }

        public List<IFluidHandler> handlers() {
            return handlers;
        }

        /** Water the handlers can give (salty water does not count with {@code freshOnly}). */
        public long available(boolean freshOnly) {
            long n = 0;
            for (IFluidHandler h : handlers) {
                FluidStack s = FluidMoves.peekWater(h, Integer.MAX_VALUE);
                WaterValues v = WaterValues.from(s);
                if (s.isEmpty() || (freshOnly && v != null && !v.fresh())) continue;
                n += s.getAmount();
            }
            return n;
        }

        /** Takes up to {@code max} mB of water from the handlers (fresh only if asked). */
        public List<Portion> take(int max, boolean freshOnly) {
            List<Portion> out = new ArrayList<>();
            int left = max;
            for (IFluidHandler h : handlers) {
                if (left <= 0) break;
                FluidStack s = FluidMoves.peekWater(h, left);
                WaterValues v = WaterValues.from(s);
                if (s.isEmpty() || (freshOnly && v != null && !v.fresh())) continue;
                FluidStack got = h.drain(s, IFluidHandler.FluidAction.EXECUTE);
                if (got.isEmpty()) continue;
                out.add(new Portion(got.getAmount(), WaterValues.from(got)));
                left -= got.getAmount();
            }
            return out;
        }

        /** Whether the handlers hold only salty water (for the status line). */
        public boolean onlySalty() {
            boolean salty = false;
            for (IFluidHandler h : handlers) {
                FluidStack s = FluidMoves.peekWater(h, Integer.MAX_VALUE);
                if (s.isEmpty()) continue;
                WaterValues v = WaterValues.from(s);
                if (v == null || v.fresh()) return false;
                salty = true;
            }
            return salty;
        }
    }
}
