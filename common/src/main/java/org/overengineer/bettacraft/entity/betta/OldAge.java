package org.overengineer.bettacraft.entity.betta;

import java.util.function.ToDoubleFunction;
import org.overengineer.bettacraft.config.Care;
import org.overengineer.bettacraft.config.ServerConfig;

/**
 * M33 part d (PLAN 5.5): death of old age, {@code health.oldAge.enabled} (off by default). With it on, a betta or a
 * tankmate dies after {@code health.oldAge.years} in-game years of {@code health.oldAge.daysPerYear} days (2 x 96 = 192
 * days). The age is counted in {@link BettaHealth} with the care itself, so it only runs while the animal lives in a tank
 * and its care runs (unloaded chunks, a feature that is off: time stands still, as for every other timer). An animal from
 * before 1.26.0 has no age yet: it starts at the age its life stage shows (the days of the stages it went through plus the
 * days in its current one; for a grown animal the age at which it grew up). Never in {@code care.mode} NO_DEATH or DECOR.
 * Not for the small animals (their colonies live on by breeding), the amphibians, reptiles and turtles (they live for
 * decades), the animals of other mods (their mod decides) and the mobs in glass.
 */
public final class OldAge {
    /** The cause of death in the log, the message ({@code cause.bettacraft.old_age}) and the KubeJS {@code AnimalDiedEvent}. */
    public static final String CAUSE = "old_age";

    private OldAge() {}

    public static boolean enabled() {
        return ServerConfig.getOr(ServerConfig.HEALTH_OLD_AGE_ENABLED, false);
    }

    /** The lifespan in in-game days. */
    public static double lifespanDays() {
        return lifespanDays(ServerConfig.getOr(ServerConfig.HEALTH_OLD_AGE_YEARS, 2.0), ServerConfig.getOr(ServerConfig.HEALTH_OLD_AGE_DAYS_PER_YEAR, 96));
    }

    /** Pure: years of {@code daysPerYear} days. */
    public static double lifespanDays(double years, int daysPerYear) {
        return Math.max(0, years) * Math.max(1, daysPerYear);
    }

    /** Pure: the age of an animal without a counted age, from its life stage (see the class comment). */
    public static double ageFromStage(LifeStage stage, double stageDays, ToDoubleFunction<LifeStage> duration) {
        double age = 0;
        for (LifeStage s : LifeStage.values()) {
            if (s == stage) break;
            double d = duration.applyAsDouble(s);
            if (Double.isFinite(d)) age += Math.max(0, d);
        }
        return age + (Double.isFinite(stageDays) ? Math.max(0, stageDays) : 0);
    }

    /** Gives an animal without a counted age the age of its stage (once). */
    public static void start(BettaHealth h, LifeStage stage, double stageDays, ToDoubleFunction<LifeStage> duration) {
        if (!h.ageKnown()) h.setAgeDays(ageFromStage(stage, stageDays, duration));
    }

    /** Pure: whether an animal of this age dies of old age now. */
    public static boolean due(boolean enabled, ServerConfig.CareMode mode, double ageDays, double lifespanDays) {
        return enabled && Care.canDie(mode) && ageDays >= 0 && ageDays >= lifespanDays;
    }

    /** Whether the animal dies of old age now (the config). */
    public static boolean due(BettaHealth h) {
        return due(enabled(), Care.mode(), h.ageDays(), lifespanDays());
    }
}
