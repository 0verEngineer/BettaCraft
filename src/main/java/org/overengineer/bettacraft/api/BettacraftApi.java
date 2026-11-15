package org.overengineer.bettacraft.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.monitor.MonitorValue;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M32 part d: read-only helpers for modpack scripts (KubeJS: {@code Java.loadClass('org.overengineer.bettacraft.api.BettacraftApi')})
 * and other mods, server side. {@code pos} may be any block of a tank. The events are in {@code api.event}.
 */
public final class BettacraftApi {
    private BettacraftApi() {}

    @Nullable
    private static TankBlockEntity tank(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel) || !level.isLoaded(pos)) return null;
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        return c != null && c.isController() && !c.isRemoved() ? c : null;
    }

    /** Whether {@code pos} is a block of a Bettacraft tank. */
    public static boolean isTank(Level level, BlockPos pos) {
        return tank(level, pos) != null;
    }

    /** The stable id of the tank, null without one. */
    @Nullable
    public static UUID tankId(Level level, BlockPos pos) {
        TankBlockEntity c = tank(level, pos);
        return c == null ? null : c.getTank().id();
    }

    /** The name of the tank's sign ("" without one, null without a tank). */
    @Nullable
    public static String tankName(Level level, BlockPos pos) {
        TankBlockEntity c = tank(level, pos);
        return c == null ? null : c.getTank().name();
    }

    /** The controller block of the tank, null without one. */
    @Nullable
    public static BlockPos tankController(Level level, BlockPos pos) {
        TankBlockEntity c = tank(level, pos);
        return c == null ? null : c.getBlockPos().immutable();
    }

    /**
     * The values the monitors show, by id ({@code temperature}, {@code water_quality}, {@code ph}, {@code tds},
     * {@code salinity}, {@code fill}, ...; NaN where a value does not apply). Empty without a tank.
     */
    public static Map<String, Float> tankValues(Level level, BlockPos pos) {
        Map<String, Float> out = new LinkedHashMap<>();
        TankBlockEntity c = tank(level, pos);
        if (c == null) return out;
        float[] v = MonitorValue.sampleAll((ServerLevel) level, c);
        for (MonitorValue m : MonitorValue.ALL) out.put(m.id, v[m.ordinal()]);
        return out;
    }
}
