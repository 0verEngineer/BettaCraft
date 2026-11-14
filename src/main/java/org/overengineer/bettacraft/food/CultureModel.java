package org.overengineer.bettacraft.food;

/**
 * M26: the pure model of a live food culture (tubifex dish, daphnia barrel), without the world, for the block entity and
 * the unit tests. {@link #population} is the share of the culture's room (0..1), {@link #food} what is left to eat (0..1:
 * one ingredient adds a quarter), {@link #foul} how stale the water of a tubifex dish is (0..1; from half on the worms are
 * of poor quality), {@link #crowded} the days a daphnia culture has spent near its room ({@link #CROWDED}).
 * <p>
 * Growth is logistic: with food (and, for daphnia, light) a culture started with one portion fills its room in about
 * {@code days} (from 10 % to 90 % in exactly {@code days}). Without food it shrinks, below {@link #DEAD} it has died out.
 * Daphnia collapse to {@link #AFTER_COLLAPSE} when they stay crowded for {@code crowdDays} (harvest in time) or when
 * the water gets hot (a hot biome in the sun, the Nether) for {@link #HEAT_DAYS}.
 */
public final class CultureModel {
    /** Below this share the culture has died out. */
    public static final double DEAD = 0.02;
    /** The share a harvest always leaves for the culture to go on. */
    public static final double SEED = 0.1;
    /** From this share a daphnia culture is crowded. */
    public static final double CROWDED = 0.9;
    /** What is left of a daphnia culture after a collapse. */
    public static final double AFTER_COLLAPSE = 0.08;
    /** Days of hot water a daphnia culture survives. */
    public static final double HEAT_DAYS = 0.25;
    /** One ingredient (mulm, yeast) adds this much food; green water more. */
    public static final double FOOD_PER_ITEM = 0.25;
    /** Above this much food a tubifex dish fouls faster (overfed). */
    public static final double OVERFED = 0.5;
    /** From this foul the worms are of poor quality. */
    public static final double POOR = 0.5;
    private static final double MAX_STEP = 0.05;

    public enum Event { NONE, DIED_OUT, COLLAPSE_CROWDED, COLLAPSE_HEAT }

    public double population, food, foul, crowded, heat;

    /** Growth rate per day for a culture that grows from 10 % to 90 % in {@code days}. */
    public static double rate(double days) {
        return Math.log(81) / Math.max(0.01, days);
    }

    /** Portions that can be taken now (always leaving {@link #SEED}). */
    public int harvestable(int room) {
        return Math.max(0, (int) Math.floor((population - SEED) * room + 1e-9));
    }

    /** Takes up to {@code n} portions; returns how many. */
    public int take(int n, int room) {
        int k = Math.min(n, harvestable(room));
        if (k > 0) population = Math.max(0, population - k / (double) room);
        if (population < CROWDED) crowded = 0;
        return k;
    }

    /** Adds one starter portion (the animals put in). */
    public void seed(int room) {
        population = Math.min(1, population + 1.0 / room);
    }

    public boolean alive() {
        return population >= DEAD;
    }

    public boolean poor() {
        return foul >= POOR;
    }

    /** Fresh water through the dish: {@code mb} of it washes out stale water (1000 mB = half of the foul). */
    public void rinse(int mb) {
        foul = Math.max(0, foul - mb / 1000.0 * 0.5);
    }

    /**
     * Advances a tubifex dish by {@code days}. The worms need mud and water; they eat {@link #food} (mulm, waste water),
     * the standing water slowly fouls (fast when overfed) and at full foul the worms die off.
     */
    public Event stepTubifex(double days, boolean mud, boolean water, double growthDays) {
        Event result = Event.NONE;
        double left = Math.max(0, days);
        double r = rate(growthDays);
        while (left > 1e-9 && population > 0) {
            double dt = Math.min(MAX_STEP, left);
            left -= dt;
            if (!mud || !water) {
                population -= 0.3 * population * dt;
            } else {
                if (food > 0) {
                    population += r * population * (1 - population) * dt;
                    food = Math.max(0, food - 0.25 * population * dt);
                } else {
                    population -= 0.1 * population * dt;
                }
                foul = Math.min(1, foul + dt * (0.05 + 0.6 * Math.max(0, food - OVERFED)));
                if (foul >= 1) population -= 0.3 * population * dt;
            }
            population = Math.max(0, Math.min(1, population));
            if (population < DEAD) {
                population = 0;
                result = Event.DIED_OUT;
            }
        }
        return result;
    }

    /**
     * Advances a daphnia culture by {@code days}. They need water; with light and food (yeast, green water) they
     * multiply, without food they starve. Crowded for {@code crowdDays} or hot for {@link #HEAT_DAYS}, they collapse.
     */
    public Event stepDaphnia(double days, boolean water, boolean lit, boolean hot, double growthDays, double crowdDays) {
        Event result = Event.NONE;
        double left = Math.max(0, days);
        double r = rate(growthDays);
        while (left > 1e-9 && population > 0) {
            double dt = Math.min(MAX_STEP, left);
            left -= dt;
            if (!water) {
                population -= 0.5 * population * dt;
            } else {
                if (food > 0) {
                    if (lit) population += r * population * (1 - population) * dt;
                    food = Math.max(0, food - 0.3 * population * dt);
                } else {
                    population -= 0.15 * population * dt;
                }
            }
            heat = hot ? heat + dt : 0;
            if (heat >= HEAT_DAYS) {
                heat = 0;
                population = Math.min(population, AFTER_COLLAPSE);
                result = Event.COLLAPSE_HEAT;
            }
            if (population >= CROWDED) {
                crowded += dt;
                if (crowded >= crowdDays) {
                    crowded = 0;
                    population = AFTER_COLLAPSE;
                    result = Event.COLLAPSE_CROWDED;
                }
            } else {
                crowded = 0;
            }
            population = Math.max(0, Math.min(1, population));
            if (population < DEAD) {
                population = 0;
                result = Event.DIED_OUT;
            }
        }
        return result;
    }
}
