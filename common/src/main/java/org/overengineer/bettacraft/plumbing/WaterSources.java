package org.overengineer.bettacraft.plumbing;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.WaterValues;

/**
 * M24: water next to a device: the fluid handlers next to it (a reservoir, a pipe, another mod's tank, an AE2/RS interface) and
 * water source blocks next to it (an endless supply of tap water). The tank itself and the multiblock filter are never a source.
 * <p>
 * M68 part B: the level sensor and the water change controller no longer take water from what stands next to them (fresh water
 * always comes through the controller's fresh water line, {@link WaterChangerBlockEntity}); what is left here serves the devices
 * that still fill themselves from their neighbours: the reverse osmosis unit, the mister and the hired aquarist's workplace.
 */
public final class WaterSources {
    private WaterSources() {}

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

    /** A portion of water taken from a source: amount and values (null = tap water). */
    public record Portion(int mb, @Nullable WaterValues values) {}

    /** Takes up to {@code max} mB of water from the handlers next to the device (fresh only if asked). */
    public static List<Portion> take(Level level, BlockPos pos, int max, boolean freshOnly) {
        return take(handlers(level, pos), max, freshOnly);
    }

    /** Takes up to {@code max} mB of water from {@code handlers} in turn (fresh only if asked). */
    public static List<Portion> take(List<IFluidHandler> handlers, int max, boolean freshOnly) {
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
}
