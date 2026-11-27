package org.overengineer.bettacraft.filter;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.plumbing.WaterSources;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M40 part b (F1, {@code features.pipes.filterWater}): water devices at the multiblock filter. A level sensor or a water change
 * controller that touches a part of the filter works on every tank the filter serves ({@link #tanks}): the water goes in with
 * the filter's return line. When the water is short, every tank gets its part of what there is by what it needs
 * ({@link #distribute}): the share of a water change is a share of each tank's volume, so the bigger tank gets more.
 * <p>
 * M68: the filter's fresh and waste water ports are gone (they load as stainless steel pipes). The devices take their water from
 * what is next to them ({@link #supply}) and the old water of a water change is discarded ({@link #dispose}); part B gives the
 * water change controller its own fresh and waste water ports (pipe roles) and serves the filter's tanks through them.
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

    /** Where a device on the filter gets water: what is next to it (M68: the filter has no fresh water port any more). */
    public static WaterSources.Supply supply(ServerLevel level, BlockPos device, FilterPumpHeadBlockEntity head) {
        return new WaterSources.Supply(WaterSources.endless(level, device), new ArrayList<>(WaterSources.handlers(level, device)));
    }

    /** Old water of a water change through the filter: discarded (M68: no waste water port any more). Returns the mB kept: 0. */
    public static int dispose(ServerLevel level, FilterPumpHeadBlockEntity head, FluidStack old) {
        return 0;
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
