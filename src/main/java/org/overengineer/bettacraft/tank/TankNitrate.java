package org.overengineer.bettacraft.tank;

import net.minecraft.server.level.ServerLevel;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.config.ServerConfig.WaterChangeDifficulty;

/**
 * M24: nitrate, the dissolved waste that even the best filter leaves behind (the end product of the bacteria). It rises
 * slowly with the animals and only water changes and plants lower it ({@code waterchange.difficulty}): a normally
 * stocked tank without plants needs 25 % fresh water about every 20 (EASY), 14 (NORMAL) or 7 (REALISTIC) days to stay
 * below 40 mg/L. From {@code nitrateStressFrom} (50) the fish breed less and the algae grow faster, from
 * {@code nitrateLethalFrom} (120) the water counts as bad (sick animals, deaths only after many weeks without a change).
 * Evaporation concentrates it like the salt; topping off with fresh water dilutes it back.
 */
public final class TankNitrate {
    private TankNitrate() {}

    /** Nitrate (mg/L) right after a water change that the recommended schedule reaches at its peak: 4 x rate x interval. */
    public static final double SCHEDULE_PEAK = 40.0;
    /** The share of the water the recommended schedule changes. */
    public static final double SCHEDULE_SHARE = 0.25;

    public static WaterChangeDifficulty difficulty() {
        return ServerConfig.getOr(ServerConfig.WATER_CHANGE_DIFFICULTY, WaterChangeDifficulty.NORMAL);
    }

    public static boolean enabled() {
        return difficulty() != WaterChangeDifficulty.OFF;
    }

    /** Days between two 25 % water changes the difficulty asks for (0 = never). */
    public static int intervalDays(WaterChangeDifficulty d) {
        return switch (d) {
            case OFF -> 0;
            case EASY -> 20;
            case NORMAL -> 14;
            case REALISTIC -> 7;
        };
    }

    /**
     * Pure: mg/L per day a normally stocked tank without plants gains, chosen so that 25 % every {@link #intervalDays}
     * keeps it at {@link #SCHEDULE_PEAK}: the peak of the saw tooth is rate x interval / share.
     */
    public static double baseRate(WaterChangeDifficulty d) {
        int days = intervalDays(d);
        return days <= 0 ? 0 : SCHEDULE_PEAK * SCHEDULE_SHARE / days;
    }

    /**
     * Pure: change of the nitrate per day. {@code load} = animals per water block / reference stocking (a normally
     * stocked tank is 1; never below 0.1 with water in the tank: leftovers and dead leaves), {@code plants} = 0..1 how
     * densely planted it is in good light, {@code uptake} the uptake of a dense planting in multiples of the base rate.
     */
    public static double ratePerDay(WaterChangeDifficulty d, double load, double plants, double uptake) {
        double base = baseRate(d);
        return base * Math.max(0.1, load) - base * uptake * Math.max(0, Math.min(1, plants));
    }

    /** Pure: nitrate after {@code days} (never below 0). */
    public static float step(float nitrate, double days, double ratePerDay) {
        return (float) Math.max(0, Math.min(1000, nitrate + ratePerDay * days));
    }

    /** Pure: nitrate after mixing {@code addedMb} of water with {@code addedNitrate} into {@code waterMb}. */
    public static float mix(float nitrate, long waterMb, float addedNitrate, long addedMb) {
        if (addedMb <= 0) return nitrate;
        if (waterMb <= 0) return addedNitrate;
        return (float) ((nitrate * (double) waterMb + addedNitrate * (double) addedMb) / (waterMb + addedMb));
    }

    /** Pure: nitrate after {@code lostMb} of pure water evaporated. */
    public static float concentrate(float nitrate, long waterMb, long lostMb) {
        long left = waterMb - lostMb;
        if (lostMb <= 0 || waterMb <= 0 || left <= 0) return nitrate;
        return (float) Math.min(1000, nitrate * (double) waterMb / left);
    }

    public static double stressFrom() {
        return ServerConfig.getOr(ServerConfig.NITRATE_STRESS_FROM, 50.0);
    }

    public static double lethalFrom() {
        return ServerConfig.getOr(ServerConfig.NITRATE_LETHAL_FROM, 120.0);
    }

    /** Too much nitrate: the fish breed less, the algae grow faster. */
    public static boolean high(TankData t) {
        return enabled() && t.nitrate() >= stressFrom();
    }

    /** So much nitrate that the water counts as bad. */
    public static boolean lethal(TankData t) {
        return enabled() && t.nitrate() >= lethalFrom();
    }

    /** Factor on breeding chances: 1 below the stress level, 0.5 above it (bad water stops breeding anyway). */
    public static double breedingFactor(TankData t) {
        return high(t) ? 0.5 : 1.0;
    }

    /** Pure: factor on the algae growth: 1 below {@code stress}, rising by 1 per 100 mg/L above it (at most 2.5). */
    public static double algaeFactor(double nitrate, double stress) {
        return nitrate <= stress ? 1.0 : Math.min(2.5, 1.0 + (nitrate - stress) / 100.0);
    }

    public static double algaeFactor(TankData t) {
        return enabled() ? algaeFactor(t.nitrate(), stressFrom()) : 1.0;
    }

    public enum State { LOW, RISING, HIGH, LETHAL }

    /** Pure: how the nitrate stands (rising from 3/4 of the stress level). */
    public static State state(double nitrate, double stress, double lethal) {
        if (nitrate >= lethal) return State.LETHAL;
        if (nitrate >= stress) return State.HIGH;
        if (nitrate >= stress * 0.75) return State.RISING;
        return State.LOW;
    }

    /** How the stocking of this tank compares to a normally stocked one (animals per water block / reference). */
    public static double load(ServerLevel level, TankData t) {
        double ref = ServerConfig.getOr(ServerConfig.NITRATE_REFERENCE_STOCKING, 0.5);
        return TankWater.animals(TankInhabitants.liveInhabitants(level, t), t) / Math.max(1.0, t.waterBlocks()) / ref;
    }

    /**
     * 0..1 how densely planted the tank is (plants in the interior and floating plants), ours times their light; the plants
     * of Minecraft and other mods (M38) without a light minimum.
     */
    public static double plants(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        double own = Math.min(1.0, org.overengineer.bettacraft.plants.InteriorPlants.ownShare(level.getServer(), t) * 4
                + org.overengineer.bettacraft.tank.plants.TankPlants.coverage(t) * 0.5);
        double foreign = org.overengineer.bettacraft.plants.InteriorPlants.foreignShare(level.getServer(), t) * 4;
        if (own <= 0 && foreign <= 0) return 0;
        int light = own > 0 ? org.overengineer.bettacraft.tank.plants.TankPlants.light(level, c) : 15;
        return Math.min(1.0, own * (light >= 7 ? 1.0 : 0.3) + foreign);
    }

    /** mg/L per day in this tank now. */
    public static double ratePerDay(ServerLevel level, TankBlockEntity c) {
        if (!enabled() || c.getTank().waterMb() <= 0) return 0;
        return ratePerDay(difficulty(), load(level, c.getTank()), plants(level, c), ServerConfig.getOr(ServerConfig.NITRATE_PLANT_UPTAKE, 1.2));
    }

    /** One care step: nitrate rises (or falls with many plants); the log warns when it crosses the stress / lethal level. */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        TankData t = c.getTank();
        if (!enabled() || days <= 0 || t.waterMb() <= 0) return;
        float before = t.nitrate();
        float next = step(before, days, ratePerDay(level, c));
        if (Math.abs(next - before) < 1e-6) return;
        t.setNitrate(next);
        c.setChanged();
        double stress = stressFrom(), lethal = lethalFrom();
        if (before < lethal && next >= lethal) TankCare.log(level, c, "log.bettacraft.nitrate_lethal", String.valueOf(Math.round(next)));
        else if (before < stress && next >= stress) TankCare.log(level, c, "log.bettacraft.nitrate_high", String.valueOf(Math.round(next)));
    }

    /** Days until the nitrate reaches the stress level at the current rate (-1 = never / already there). */
    public static double daysUntilHigh(TankData t, double ratePerDay) {
        double left = stressFrom() - t.nitrate();
        if (left <= 0 || ratePerDay <= 1e-9) return -1;
        return left / ratePerDay;
    }
}
