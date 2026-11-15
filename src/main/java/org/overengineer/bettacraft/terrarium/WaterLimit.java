package org.overengineer.bettacraft.terrarium;

import java.util.Arrays;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankShape;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;

/**
 * M33.1: the water limit of a tank with a fixed type ({@link TankModeSetting}). Every way water comes into a tank asks
 * here first (the bucket, the fluid capability of pipes, pumps, Create and other mods, the top-off, the water an animal
 * brings along); a water change only replaces what it drained, rain never fills a tank. A <b>terrarium</b> takes no
 * water; a <b>paludarium</b> takes water up to the height where the land part (the interior columns whose walkable top
 * lies above the water, as in {@link TankClimate#scanLand}) is still {@code terrarium.paludariumLandShare} of the floor;
 * an <b>aquarium</b> or an automatic tank as much as fits. Water already in the tank is never removed: above the limit
 * nothing more comes in, draining works as always.
 */
public final class WaterLimit {
    /** Interior blocks the limit stays below the land tops (the scan counts a column as land from 0.05 above the water). */
    static final double MARGIN = 0.07;
    /** Ticks between two type changes from the menu (per tank). */
    public static final long SET_TICKS = 5;

    private WaterLimit() {}

    /** Whether a fixed type does anything (its switch and the terraria are on). */
    public static boolean enabled() {
        return Features.enabled(Feature.TANK_MODE) && TankClimate.enabled();
    }

    /** The type in force: the one set in the menu, automatic while the switch is off. */
    public static TankModeSetting setting(TankData t) {
        return enabled() ? t.tankMode() : TankModeSetting.AUTO;
    }

    /** Whether the type of this tank limits its water (a fixed terrarium or paludarium). */
    public static boolean limits(TankData t) {
        TankModeSetting s = setting(t);
        return s == TankModeSetting.TERRARIUM || s == TankModeSetting.PALUDARIUM;
    }

    // ------------------------------------------------------------------ pure (unit-tested)

    /**
     * The highest water surface (interior blocks above the interior floor) that leaves at least {@code share} of the
     * sampled columns above the water. {@code tops16} are their walkable tops in 1/16 interior block. Infinite for a share
     * of 0, 0 without columns, negative when no water at all is possible.
     */
    public static double maxSurface(int[] tops16, double share) {
        if (!(share > 1e-9)) return Double.POSITIVE_INFINITY;
        int n = tops16.length;
        if (n == 0) return 0;
        int k = Math.max(1, (int) Math.ceil(share * n - 1e-9));
        if (k > n) return -1;
        int[] sorted = tops16.clone();
        Arrays.sort(sorted);
        return sorted[n - k] / 16.0 - MARGIN;
    }

    /** The most water (mB) of a tank whose surface stays at {@code surface} interior blocks above its floor (scale: interior blocks per tank block). */
    public static long waterFor(TankData t, double surface, int scale) {
        if (surface == Double.POSITIVE_INFINITY) return Long.MAX_VALUE;
        if (!(surface > 0)) return 0;
        int minY = t.min().getY();
        return TankShape.waterBelow(t.layerCounts(), minY, minY + surface / Math.max(1, scale), TankData.headroom());
    }

    // ------------------------------------------------------------------ server

    /** The most water this tank may hold by its type ({@link Long#MAX_VALUE}: no limit). */
    public static long maxWaterMb(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        TankModeSetting s = setting(t);
        if (s == TankModeSetting.TERRARIUM) return 0;
        if (s != TankModeSetting.PALUDARIUM) return Long.MAX_VALUE;
        if (c.climateTops == null) TankClimate.scanNow(level, c);
        int[] tops = c.climateTops == null ? new int[0] : c.climateTops;
        double share = TankClimate.thresholds().paludarium();
        if (tops != c.limitTops || share != c.limitShare || c.limitMb < 0) {
            InteriorRegion r = InteriorRegions.get(level.getServer()).get(t.regionId());
            c.limitMb = waterFor(t, maxSurface(tops, share), r != null ? r.scale() : t.scale());
            c.limitTops = tops;
            c.limitShare = share;
        }
        return c.limitMb;
    }

    /** Water (mB) that may still come in: up to the capacity and the limit of the type. */
    public static long room(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        long free = Math.max(0, t.capacityMb() - t.waterMb());
        if (!limits(t)) return free;
        return Math.max(0, Math.min(free, maxWaterMb(level, c) - t.waterMb()));
    }

    /** {@code mb} cut down to {@link #room}. */
    public static long clamp(ServerLevel level, TankBlockEntity c, long mb) {
        return mb <= 0 ? mb : Math.min(mb, room(level, c));
    }

    /** Whether the type, not the glass, keeps {@code mb} more water out. */
    public static boolean blocks(ServerLevel level, TankBlockEntity c, long mb) {
        TankData t = c.getTank();
        return limits(t) && t.waterMb() + mb <= t.capacityMb() && room(level, c) < mb;
    }

    /** The action bar message when the type keeps water out ("Terrarium: no water", "Paludarium: water only up to ..."). */
    public static Component message(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        if (setting(t) == TankModeSetting.TERRARIUM) return Component.translatable("message.bettacraft.tank_mode.terrarium_no_water").withStyle(ChatFormatting.GOLD);
        long max = Math.max(0, maxWaterMb(level, c));
        return Component.translatable("message.bettacraft.tank_mode.paludarium_limit", height(t, max), max).withStyle(ChatFormatting.GOLD);
    }

    /** Height of {@code mb} of water above the tank floor in blocks ("1.4"). */
    public static String height(TankData t, long mb) {
        double y = TankShape.surfaceY(t.layerCounts(), t.min().getY(), mb, TankData.headroom()) - t.min().getY();
        return String.format(Locale.ROOT, "%.1f", Math.max(0, y));
    }

    /** Tells the player why the water stays out (action bar). */
    public static void tell(ServerLevel level, TankBlockEntity c, @Nullable Player player) {
        if (player != null) player.displayClientMessage(message(level, c), true);
    }
}
