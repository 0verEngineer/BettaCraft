package org.overengineer.bettacraft.entity.betta;

import net.minecraft.nbt.CompoundTag;

/**
 * Health timers of one betta (PLAN 5.5). Pure logic without Minecraft dependencies besides NBT (unit-tested).
 * All values are in in-game days.
 * <ul>
 *     <li><b>Food:</b> {@code foodStress} counts the time since the last meal. A fish counts as hungry ("food bad")
 *     once more than {@link Params#hungryAfterDays} have passed since it was fed.</li>
 *     <li><b>Temperature / overcrowding / water quality (M9):</b> their stress counters grow while the condition is bad and shrink by
 *     {@link Params#recoveryPerDay} per good day.</li>
 *     <li>If two or more conditions are bad at once, all counters run {@link Params#bothBadMultiplier} times as fast.</li>
 *     <li>A counter reaching {@link Params#daysUntilSick} makes the fish sick. While sick, sick days accumulate as long
 *     as anything is bad; when everything is good, {@code recoveryPerDay} sick days are removed per day. At 0 the fish
 *     is healthy again, at {@link Params#daysSickUntilDeath} it dies.</li>
 * </ul>
 */
public final class BettaHealth {
    /** Largest internal integration step: conditions are assumed constant over one call, events are exact enough. */
    static final double MAX_STEP_DAYS = 0.02;

    public record Params(double daysUntilSick, double daysSickUntilDeath, double bothBadMultiplier, double recoveryPerDay,
                         double hungryAfterDays) {
        public static final Params DEFAULT = new Params(7, 14, 2, 2, 1);
    }

    public record Conditions(boolean tempBad, boolean crowdBad, boolean waterBad) {
        public static final Conditions GOOD = new Conditions(false, false, false);

        public Conditions(boolean tempBad, boolean crowdBad) {
            this(tempBad, crowdBad, false);
        }
    }

    public enum Event { NONE, GOT_SICK, RECOVERED, DIED }

    double daysSinceFed;
    double foodStress;
    double tempStress;
    double crowdStress;
    double waterStress;
    boolean sick;
    double sickDays;

    public BettaHealth() {}

    // ------------------------------------------------------------------ logic

    public void feed() {
        daysSinceFed = 0;
        foodStress = 0;
    }

    public boolean isHungry(Params p) {
        return daysSinceFed > p.hungryAfterDays();
    }

    /** Advances the timers by {@code days}. Returns the most important event that happened (DIED wins). */
    public Event step(double days, Conditions c, Params p) {
        Event result = Event.NONE;
        double left = Math.max(0, days);
        while (left > 1e-9) {
            double dt = Math.min(MAX_STEP_DAYS, left);
            left -= dt;
            Event e = stepOnce(dt, c, p);
            if (e == Event.DIED) return Event.DIED;
            if (e != Event.NONE) result = e;
        }
        return result;
    }

    private Event stepOnce(double dt, Conditions c, Params p) {
        boolean foodBad = isHungry(p);
        int bad = (foodBad ? 1 : 0) + (c.tempBad() ? 1 : 0) + (c.crowdBad() ? 1 : 0) + (c.waterBad() ? 1 : 0);
        double m = bad >= 2 ? p.bothBadMultiplier() : 1.0;

        daysSinceFed += dt;
        foodStress += dt * m;
        tempStress = c.tempBad() ? tempStress + dt * m : Math.max(0, tempStress - dt * p.recoveryPerDay());
        crowdStress = c.crowdBad() ? crowdStress + dt * m : Math.max(0, crowdStress - dt * p.recoveryPerDay());
        waterStress = c.waterBad() ? waterStress + dt * m : Math.max(0, waterStress - dt * p.recoveryPerDay());

        if (!sick) {
            if (foodStress >= p.daysUntilSick() || tempStress >= p.daysUntilSick() || crowdStress >= p.daysUntilSick()
                    || waterStress >= p.daysUntilSick()) {
                sick = true;
                sickDays = 0;
                return Event.GOT_SICK;
            }
            return Event.NONE;
        }
        if (bad > 0) {
            sickDays += dt * m;
            if (sickDays >= p.daysSickUntilDeath()) return Event.DIED;
        } else {
            sickDays -= dt * p.recoveryPerDay();
            if (sickDays <= 0) {
                sick = false;
                sickDays = 0;
                tempStress = 0;
                crowdStress = 0;
                waterStress = 0;
                return Event.RECOVERED;
            }
        }
        return Event.NONE;
    }

    // ------------------------------------------------------------------ display

    /** 1 = healthy, 0.5 = just got sick, 0 = dead. */
    public float healthFraction(Params p) {
        if (sick) return (float) (0.5 * (1 - Math.min(1, sickDays / p.daysSickUntilDeath())));
        double worst = Math.max(Math.max(foodStress, waterStress), Math.max(tempStress, crowdStress));
        return (float) (1 - 0.5 * Math.min(1, worst / p.daysUntilSick()));
    }

    /** 1 = just fed, 0 = gets sick from hunger now. */
    public float satiation(Params p) {
        return (float) Math.max(0, 1 - foodStress / p.daysUntilSick());
    }

    public boolean isSick() { return sick; }
    public double sickDays() { return sickDays; }
    public double daysSinceFed() { return daysSinceFed; }
    public double foodStress() { return foodStress; }
    public double tempStress() { return tempStress; }
    public double crowdStress() { return crowdStress; }
    public double waterStress() { return waterStress; }

    // ------------------------------------------------------------------ NBT

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putDouble("days_since_fed", daysSinceFed);
        t.putDouble("food_stress", foodStress);
        t.putDouble("temp_stress", tempStress);
        t.putDouble("crowd_stress", crowdStress);
        t.putDouble("water_stress", waterStress);
        t.putBoolean("sick", sick);
        t.putDouble("sick_days", sickDays);
        return t;
    }

    /** M17: NaN, infinite or negative days (corrupt data) froze a fish: it never got sick, recovered or died. */
    static double days(CompoundTag t, String key) {
        double v = t.getDouble(key);
        return Double.isFinite(v) ? Math.max(0, v) : 0;
    }

    public static BettaHealth load(CompoundTag t) {
        BettaHealth h = new BettaHealth();
        h.daysSinceFed = days(t, "days_since_fed");
        h.foodStress = days(t, "food_stress");
        h.tempStress = days(t, "temp_stress");
        h.crowdStress = days(t, "crowd_stress");
        h.waterStress = days(t, "water_stress"); // M9; 0 for older fish
        h.sick = t.getBoolean("sick");
        h.sickDays = days(t, "sick_days");
        return h;
    }

    public BettaHealth copy() {
        return load(save());
    }
}
