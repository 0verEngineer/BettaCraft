package org.overengineer.bettacraft.filter;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.plumbing.FilterLines;
import org.overengineer.bettacraft.plumbing.FluidMoves;
import org.overengineer.bettacraft.plumbing.PipeLine;
import org.overengineer.bettacraft.plumbing.WaterSources;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M40 part b (F1, {@code features.pipes.filterWater}): fresh and waste water at the multiblock filter. A level sensor or a
 * water change controller that touches a part of the filter works on every tank the filter serves ({@link #tanks}): the
 * water goes in with the filter's return line. They take their water from what is next to them and from the filter's
 * fresh water ports ({@link #supply}: the water in the ports, the blocks at the ends of their fresh water lines, a water
 * source block next to a port = tap water); the old water of a water change goes out of the waste water ports
 * ({@link #dispose}), without one it is discarded like the water change controller does next to a tank. When the water
 * is short, every tank gets its part of what there is by what it needs ({@link #distribute}): the share of a water change
 * is a share of each tank's volume, so the bigger tank gets more.
 */
public final class FilterWater {
    private FilterWater() {}

    /** The pump head of the multiblock filter one of whose parts touches {@code pos}, or null (or with the switch off). */
    @Nullable
    public static FilterPumpHeadBlockEntity filterNextTo(Level level, BlockPos pos) {
        if (!Features.enabled(Feature.FILTER_WATER)) return null;
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (!level.isLoaded(n) || !(level.getBlockState(n).getBlock() instanceof FilterPartBlock)) continue;
            BlockPos head = MultiblockFilter.headNear(level, n);
            if (head != null && level.getBlockEntity(head) instanceof FilterPumpHeadBlockEntity h) return h;
        }
        return null;
    }

    /** The tanks of a formed filter its water reaches: every tank both lines reach that is guarded (or only empty). */
    public static List<TankBlockEntity> tanks(ServerLevel level, FilterPumpHeadBlockEntity head) {
        List<TankBlockEntity> out = new ArrayList<>();
        if (!head.structure(level).formed()) return out;
        for (MultiblockFilter.Link l : head.currentLinks(level)) {
            if (l.tank().isRemoved() || out.contains(l.tank())) continue;
            if (!MultiblockFilter.status(level, l).warning()) out.add(l.tank());
        }
        return out;
    }

    /** Where a device on the filter gets water: next to it, the filter's fresh water ports and their lines. */
    public static WaterSources.Supply supply(ServerLevel level, BlockPos device, FilterPumpHeadBlockEntity head) {
        List<IFluidHandler> handlers = new ArrayList<>(WaterSources.handlers(level, device));
        boolean endless = WaterSources.endless(level, device);
        for (BlockPos p : head.structure(level).fresh()) {
            if (!(level.getBlockEntity(p) instanceof FilterPortBlockEntity port)) continue;
            handlers.add(port.water());
            endless |= WaterSources.endless(level, p);
            for (FilterLines.End e : FilterLines.portReach(level, p, port.facing(), PipeLine.FRESH).ends()) {
                IFluidHandler h = e.handler(level);
                if (h != null && !handlers.contains(h)) handlers.add(h);
            }
        }
        return new WaterSources.Supply(endless, handlers);
    }

    /** Whether the filter has a waste water port (else old water is discarded). */
    public static boolean hasWaste(ServerLevel level, FilterPumpHeadBlockEntity head) {
        return !head.structure(level).waste().isEmpty();
    }

    /**
     * Old water of a water change: out of the waste water ports, to the ends of their lines first, then into the ports;
     * the rest is discarded. Returns the mB that went somewhere.
     */
    public static int dispose(ServerLevel level, FilterPumpHeadBlockEntity head, FluidStack old) {
        if (old.isEmpty()) return 0;
        int left = old.getAmount();
        List<FilterPortBlockEntity> ports = new ArrayList<>();
        for (BlockPos p : head.structure(level).waste()) if (level.getBlockEntity(p) instanceof FilterPortBlockEntity port) ports.add(port);
        for (FilterPortBlockEntity port : ports) {
            if (left <= 0) break;
            left -= toEnds(level, port, old.copyWithAmount(left));
        }
        for (FilterPortBlockEntity port : ports) {
            if (left <= 0) break;
            left -= port.water().fill(old.copyWithAmount(left), IFluidHandler.FluidAction.EXECUTE);
        }
        return old.getAmount() - left;
    }

    private static int toEnds(ServerLevel level, FilterPortBlockEntity port, FluidStack water) {
        int done = 0;
        for (FilterLines.End e : FilterLines.portReach(level, port.getBlockPos(), port.facing(), PipeLine.WASTE).ends()) {
            if (done >= water.getAmount()) break;
            IFluidHandler h = e.handler(level);
            if (h != null) done += Math.max(0, FluidMoves.fill(h, water.copyWithAmount(water.getAmount() - done)));
        }
        return done;
    }

    /** The waste water port pushes what it holds on to the ends of its line (every second). */
    static void pushOn(ServerLevel level, FilterPortBlockEntity port) {
        FluidStack in = port.water().drain(port.water().amount(), IFluidHandler.FluidAction.SIMULATE);
        if (in.isEmpty()) return;
        int put = toEnds(level, port, in);
        if (put > 0) port.water().drain(put, IFluidHandler.FluidAction.EXECUTE);
    }

    /**
     * Pure: shares {@code available} mB among wants (largest remainder): everything when there is enough, else each its
     * part by its want. Never more than a want.
     */
    public static long[] distribute(long[] want, long available) {
        long[] out = new long[want.length];
        long sum = 0;
        for (long w : want) if (w > 0) sum += w;
        if (sum <= 0 || available <= 0) return out;
        if (available >= sum) {
            for (int i = 0; i < want.length; i++) out[i] = Math.max(0, want[i]);
            return out;
        }
        double[] frac = new double[want.length];
        long given = 0;
        for (int i = 0; i < want.length; i++) {
            if (want[i] <= 0) {
                frac[i] = -1;
                continue;
            }
            double exact = (double) want[i] * available / sum;
            out[i] = (long) Math.floor(exact);
            frac[i] = exact - out[i];
            given += out[i];
        }
        for (long left = available - given; left > 0; left--) {
            int best = -1;
            for (int i = 0; i < want.length; i++) if (frac[i] >= 0 && out[i] < want[i] && (best < 0 || frac[i] > frac[best])) best = i;
            if (best < 0) break;
            out[best]++;
            frac[best] = -1;
        }
        return out;
    }
}
