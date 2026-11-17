package org.overengineer.bettacraft.tank;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.equipment.TankLightBlock;

/**
 * M23: the lights of a tank as a whole: the three tiers (light bar = light 15, LED panel and high-power LED add
 * {@code lights.panelBoost} / {@code lights.highPowerBoost} on the share of the tank they cover), the dimmer and the
 * daylight timer with a sunrise and sunset ramp (tank menu). The factor (dimmer x curve) dims the lights (block state
 * {@link TankLightBlock#DIM}, off at 0) and scales the light the plants and algae get. Pure functions are unit-tested.
 */
public final class TankLighting {
    /** Length of the sunrise and the sunset ramp of the daylight timer, in minutes of the day. */
    public static final int RAMP_MINUTES = 60;

    private TankLighting() {}

    /** Minute of the day (0..1439) of a game day time (day time 0 = 6:00). */
    public static int minuteOfDay(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return (int) ((t * 1440L / 24000L + 360) % 1440);
    }

    /**
     * Daylight curve 0..1 at minute {@code m}: 0 before {@code on}, ramps up over {@link #RAMP_MINUTES}, 1 until
     * {@code off}, ramps down over the next ramp. Times may wrap around midnight. on == off: always on.
     */
    public static double curve(int m, int on, int off) {
        if (on == off) return 1;
        int len = Math.floorMod(off - on, 1440);
        int since = Math.floorMod(m - on, 1440);
        if (since < len) return Math.min(1.0, (since + 1) / (double) RAMP_MINUTES);
        int after = since - len;
        if (after < RAMP_MINUTES) return 1.0 - (after + 1) / (double) RAMP_MINUTES;
        return 0;
    }

    /** Light factor 0..1 of the tank's settings at this time (1 with features.equipment.lightTimer off). */
    public static double factor(TankData t, long dayTime) {
        if (!Features.enabled(Feature.LIGHT_TIMER)) return 1;
        double f = t.lightDimmer() / 100.0;
        if (t.lightTimer()) f *= curve(minuteOfDay(dayTime), t.lightOn(), t.lightOff());
        return Math.max(0, Math.min(1, f));
    }

    /** Dimmer step of the lights for a factor: 0 = full (> 0.875), 3 = darkest. */
    public static int dimStep(double f) {
        if (f > 0.875) return 0;
        if (f > 0.625) return 1;
        if (f > 0.375) return 2;
        return 3;
    }

    /**
     * {lit allowed (0/1), dim step} for a light fixture: from the tank below its first block that stands on a tank;
     * lights elsewhere shine fully.
     */
    public static int[] fixtureState(Level level, List<BlockPos> fixture) {
        for (BlockPos p : fixture) {
            BlockPos below = p.below();
            if (!level.isLoaded(below)) continue;
            TankBlockEntity c = TankMultiblock.getController(level, below);
            if (c == null || c.getTank() == null) continue;
            double f = factor(c.getTank(), level.getDayTime());
            return new int[]{f > 0.001 ? 1 : 0, dimStep(f)};
        }
        return new int[]{1, 0};
    }

    /**
     * Light of the lit lights on the tank (0 = none lit): 15 x factor for the bar, plus the boost of the stronger tiers
     * on the share of the open top blocks they cover. Up to 20 with high-power LEDs everywhere.
     */
    public static double ledLight(Level level, TankBlockEntity c) {
        TankData t = c.getTank();
        if (!c.lightPowered()) return 0;
        List<BlockPos> tops = TankCare.aboveTop(t);
        int n = 0, lit = 0;
        double boost = 0;
        for (BlockPos p : tops) {
            if (!level.isLoaded(p)) continue;
            n++;
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof TankLightBlock l && s.getValue(TankLightBlock.LIT)) {
                lit++;
                boost += l.boost();
            }
        }
        if (lit == 0 || n == 0) return 0;
        return factor(t, level.getDayTime()) * (15 + boost / n);
    }

    /** Pure: light with the lit share of a tier's boost (tests). */
    public static double ledLight(double factor, double boostSum, int topBlocks) {
        return topBlocks <= 0 ? 0 : factor * (15 + boostSum / topBlocks);
    }

    /** One care step: when the factor changed noticeably, the lights recompute LIT and DIM. */
    public static void step(ServerLevel level, TankBlockEntity c) {
        double f = factor(c.getTank(), level.getDayTime());
        int step = f > 0.001 ? dimStep(f) : -1;
        int was = c.appliedLightFactor < 0 ? -2 : c.appliedLightFactor > 0.001 ? dimStep(c.appliedLightFactor) : -1;
        if (step != was) {
            c.appliedLightFactor = f;
            TankPower.updateLights(level, c.getTank());
        } else {
            c.appliedLightFactor = f;
        }
    }

    /** Extra algae growth from strong light (above 15), times the missing CO2 and fertiliser (M25 hook; 1 = nothing of either yet). */
    public static double algaeBoost(double light) {
        return algaeBoost(light, noNutrientBalance());
    }

    /** M25: as {@link #algaeBoost(double)} with the missing CO2 of the tank ({@code TankCo2.algaeBalance}: 1 none, 0.3 well dosed). */
    public static double algaeBoost(double light, double missing) {
        return light <= 15 ? 1 : 1 + (light - 15) / 10.0 * Math.max(0, missing);
    }

    /** M25 hook: how much CO2 and fertiliser are missing for strong light (1 = none dosed; there is no CO2 yet). */
    public static double noNutrientBalance() {
        return 1;
    }
}
