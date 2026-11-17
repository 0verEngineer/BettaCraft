package org.overengineer.bettacraft.entity.betta;

import net.minecraft.nbt.CompoundTag;

/**
 * Breeding state of one betta (PLAN 5.4), stored in {@link BettaData} under {@code breeding}. All values in in-game days.
 * <ul>
 *     <li>{@code conditioned}: remaining days of live-food conditioning (mosquito larvae), speeds up readiness.</li>
 *     <li>{@code ready}: time the conditions for breeding have held (male: nest building starts at the threshold).</li>
 *     <li>{@code cooldown}: female rest time after spawning.</li>
 *     <li>{@code trained} (M11): remaining days of mirror training (a male flared at a mirror stick), speeds up readiness
 *     by {@code breeding.mirrorReadinessBonus}. Additive key, missing = 0.</li>
 * </ul>
 */
public final class BettaBreeding {
    public static final double CONDITIONING_DAYS = 2.0;
    /** Mirror training lasts one in-game day after the last flare at the mirror. */
    public static final double TRAINING_DAYS = 1.0;

    double conditioned;
    double ready;
    double cooldown;
    double trained;

    /** M11: the male flared at a mirror stick (refreshes the training to a full day). */
    public void train() {
        trained = TRAINING_DAYS;
    }

    public boolean isTrained() {
        return trained > 0;
    }

    public void condition() {
        condition(1.0);
    }

    /** M26: conditioning that lasts {@code factor} times as long (tubifex); never shortens a longer one. */
    public void condition(double factor) {
        double d = org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.BREEDING_CONDITIONING_DAYS, CONDITIONING_DAYS) * factor;
        conditioned = Math.max(conditioned, d);
    }

    public double conditionedDays() {
        return conditioned;
    }

    public boolean isConditioned() {
        return conditioned > 0;
    }

    public double ready() { return ready; }
    public double cooldown() { return cooldown; }

    public void setCooldown(double days) {
        cooldown = Math.max(0, days);
    }

    public void addReady(double days) {
        ready = Math.max(0, ready + days);
    }

    public void resetReady() {
        ready = 0;
    }

    /** Time passes: conditioning and cooldown run out. */
    public void tick(double days) {
        conditioned = Math.max(0, conditioned - days);
        cooldown = Math.max(0, cooldown - days);
        trained = Math.max(0, trained - days);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        if (conditioned > 0) t.putDouble("conditioned", conditioned);
        if (ready > 0) t.putDouble("ready", ready);
        if (cooldown > 0) t.putDouble("cooldown", cooldown);
        if (trained > 0) t.putDouble("trained", trained);
        return t;
    }

    public static BettaBreeding load(CompoundTag t) {
        BettaBreeding b = new BettaBreeding();
        b.conditioned = BettaHealth.days(t, "conditioned");
        b.ready = BettaHealth.days(t, "ready");
        b.cooldown = BettaHealth.days(t, "cooldown");
        b.trained = BettaHealth.days(t, "trained");
        return b;
    }
}
