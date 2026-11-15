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

    /** M33 part c: {@code care} = the care mode ({@code care.mode}): NO_DEATH never dies, DECOR has no needs at all. */
    public record Params(double daysUntilSick, double daysSickUntilDeath, double bothBadMultiplier, double recoveryPerDay,
                         double hungryAfterDays, Diet diet, org.overengineer.bettacraft.config.ServerConfig.CareMode care) {
        public static final Params DEFAULT = new Params(7, 14, 2, 2, 1);

        public Params(double daysUntilSick, double daysSickUntilDeath, double bothBadMultiplier, double recoveryPerDay, double hungryAfterDays) {
            this(daysUntilSick, daysSickUntilDeath, bothBadMultiplier, recoveryPerDay, hungryAfterDays, Diet.DEFAULT);
        }

        public Params(double daysUntilSick, double daysSickUntilDeath, double bothBadMultiplier, double recoveryPerDay, double hungryAfterDays, Diet diet) {
            this(daysUntilSick, daysSickUntilDeath, bothBadMultiplier, recoveryPerDay, hungryAfterDays, diet,
                    org.overengineer.bettacraft.config.ServerConfig.CareMode.NORMAL);
        }

        public boolean canDie() {
            return org.overengineer.bettacraft.config.Care.canDie(care);
        }

        public boolean needs() {
            return org.overengineer.bettacraft.config.Care.needs(care);
        }
    }

    /**
     * M26 part b: a varied diet. A fish that ate {@code groups} different food groups within the last {@code windowDays}
     * recovers {@code recovery} times as fast from stress and sickness.
     */
    public record Diet(double windowDays, int groups, double recovery) {
        public static final Diet DEFAULT = new Diet(3, 3, 1.5);
        public static final Diet OFF = new Diet(0, Integer.MAX_VALUE, 1);
    }

    /** M26 part b: food groups a fish remembers (pellets, flakes, granules, wafers, frozen, live food ...). */
    public static final int FOOD_GROUPS = 8;

    /**
     * @param salt M20: salt shock per day from the wrong salinity (0 = the salinity is fine; 1 = deadly within a day,
     *             see {@link org.overengineer.bettacraft.tank.Salinity#shockPerDay})
     */
    public record Conditions(boolean tempBad, boolean crowdBad, boolean waterBad, double salt) {
        public static final Conditions GOOD = new Conditions(false, false, false, 0);

        public Conditions(boolean tempBad, boolean crowdBad) {
            this(tempBad, crowdBad, false, 0);
        }

        public Conditions(boolean tempBad, boolean crowdBad, boolean waterBad) {
            this(tempBad, crowdBad, waterBad, 0);
        }

        public boolean saltBad() {
            return salt > 0;
        }
    }

    public enum Event { NONE, GOT_SICK, RECOVERED, DIED }

    /** M33 part c: how close to death an animal waits without dying (care.mode NO_DEATH). */
    static final double ALMOST = 0.999;

    double daysSinceFed;
    double foodStress;
    double tempStress;
    double crowdStress;
    double waterStress;
    /** M20: salt shock (0..1); at 1 the animal dies, it fades with {@link Params#recoveryPerDay} in the right water. */
    double saltShock;
    boolean sick;
    double sickDays;
    /** M33 part d: days the animal has lived in a tank (its age for {@link OldAge}); -1 = not counted yet (older data). */
    double ageDays = -1;
    /** M26 part b: days since the fish last ate food of each group (infinite = not within the last month). */
    final double[] ateAgo = new double[FOOD_GROUPS];

    public BettaHealth() {
        java.util.Arrays.fill(ateAgo, Double.POSITIVE_INFINITY);
    }

    // ------------------------------------------------------------------ logic

    public void feed() {
        daysSinceFed = 0;
        foodStress = 0;
    }

    /** M26 part b: an unsuitable food: fed, but hungry again {@code hungryAfter} minus {@code daysFedAgo} days later. */
    public void feedPoorly(double daysFedAgo) {
        feed();
        daysSinceFed = Math.max(0, daysFedAgo);
        foodStress = daysSinceFed;
    }

    /** M26 part b: the fish ate food of this group (index of {@link org.overengineer.bettacraft.food.FoodDiet.Group}). */
    public void ate(int group) {
        if (group >= 0 && group < FOOD_GROUPS) ateAgo[group] = 0;
    }

    /** M26 part b: how many food groups the fish ate within the last {@code windowDays}. */
    public int variety(double windowDays) {
        int n = 0;
        for (double d : ateAgo) if (d < windowDays) n++;
        return n;
    }

    /** M26 part b: whether the diet is varied enough for the bonus. */
    public boolean varied(Diet d) {
        return variety(d.windowDays()) >= d.groups();
    }

    /** M26: food from a poor culture brought germs: adds bad-water stress (sick after {@code daysUntilSick} of it). */
    public void addWaterStress(double days) {
        if (days > 0) waterStress += days;
    }

    public boolean isHungry(Params p) {
        return daysSinceFed > p.hungryAfterDays();
    }

    /** Advances the timers by {@code days}. Returns the most important event that happened (DIED wins). */
    public Event step(double days, Conditions c, Params p) {
        Event result = Event.NONE;
        if (ageDays >= 0) ageDays += Math.max(0, days); // M33 part d: old age
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
        if (!p.needs()) { // M33 part c, care.mode DECOR: always fed, every condition fine (stress and sickness fade)
            daysSinceFed = 0;
            foodStress = 0;
            c = Conditions.GOOD;
        }
        boolean foodBad = isHungry(p);
        int bad = (foodBad ? 1 : 0) + (c.tempBad() ? 1 : 0) + (c.crowdBad() ? 1 : 0) + (c.waterBad() ? 1 : 0) + (c.saltBad() ? 1 : 0);
        double m = bad >= 2 ? p.bothBadMultiplier() : 1.0;
        // M26 part b: a varied diet speeds up the recovery
        double rec = p.recoveryPerDay() * (varied(p.diet()) ? Math.max(1, p.diet().recovery()) : 1.0);
        for (int i = 0; i < FOOD_GROUPS; i++) if (ateAgo[i] < 30) ateAgo[i] += dt; else ateAgo[i] = Double.POSITIVE_INFINITY;
        // M20: the wrong salinity is a shock of its own (faster than the other stresses; a sea fish in fresh water dies in hours)
        saltShock = c.saltBad() ? saltShock + dt * c.salt() : Math.max(0, saltShock - dt * rec);
        if (saltShock >= 1) {
            if (p.canDie()) return Event.DIED;
            saltShock = ALMOST; // M33 part c, NO_DEATH: at the brink, it waits for the right water
        }

        daysSinceFed += dt;
        foodStress += dt * m;
        tempStress = c.tempBad() ? tempStress + dt * m : Math.max(0, tempStress - dt * rec);
        crowdStress = c.crowdBad() ? crowdStress + dt * m : Math.max(0, crowdStress - dt * rec);
        waterStress = c.waterBad() ? waterStress + dt * m : Math.max(0, waterStress - dt * rec);

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
            if (sickDays >= p.daysSickUntilDeath()) {
                if (p.canDie()) return Event.DIED;
                sickDays = p.daysSickUntilDeath() * ALMOST; // M33 part c, NO_DEATH
            }
        } else {
            sickDays -= dt * rec;
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
        float f = (float) (1 - 0.5 * Math.min(1, worst / p.daysUntilSick()));
        return saltShock > 0 ? Math.min(f, (float) (1 - Math.min(1, saltShock))) : f;
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
    public double saltShock() { return saltShock; }
    /** M33 part d: the age in days, -1 if not counted yet. */
    public double ageDays() { return ageDays; }
    public boolean ageKnown() { return ageDays >= 0; }
    public void setAgeDays(double d) { ageDays = Double.isFinite(d) ? Math.max(0, d) : 0; }

    // ------------------------------------------------------------------ NBT

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putDouble("days_since_fed", daysSinceFed);
        t.putDouble("food_stress", foodStress);
        t.putDouble("temp_stress", tempStress);
        t.putDouble("crowd_stress", crowdStress);
        t.putDouble("water_stress", waterStress);
        if (saltShock > 0) t.putDouble("salt_shock", saltShock);
        t.putBoolean("sick", sick);
        t.putDouble("sick_days", sickDays);
        CompoundTag diet = new CompoundTag(); // M26 part b
        for (int i = 0; i < FOOD_GROUPS; i++) if (ateAgo[i] < 30) diet.putDouble(String.valueOf(i), ateAgo[i]);
        if (!diet.isEmpty()) t.put("diet", diet);
        if (ageDays >= 0) t.putDouble("age_days", ageDays); // M33 part d
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
        h.saltShock = Math.min(1, days(t, "salt_shock")); // M20
        h.sick = t.getBoolean("sick");
        h.sickDays = days(t, "sick_days");
        CompoundTag diet = t.getCompound("diet"); // M26 part b; empty for older fish
        for (int i = 0; i < FOOD_GROUPS; i++) if (diet.contains(String.valueOf(i))) h.ateAgo[i] = Math.min(30, days(diet, String.valueOf(i)));
        h.ageDays = t.contains("age_days") ? days(t, "age_days") : -1; // M33 part d; older animals: counted from their stage
        return h;
    }

    public BettaHealth copy() {
        return load(save());
    }
}
