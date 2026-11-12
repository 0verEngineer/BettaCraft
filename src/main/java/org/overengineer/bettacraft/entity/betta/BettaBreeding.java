package org.overengineer.bettacraft.entity.betta;

import net.minecraft.nbt.CompoundTag;

/**
 * Breeding state of one betta (PLAN 5.4), stored in {@link BettaData} under {@code breeding}. All values in in-game days.
 * <ul>
 *     <li>{@code conditioned}: remaining days of live-food conditioning (mosquito larvae), speeds up readiness.</li>
 *     <li>{@code ready}: time the conditions for breeding have held (male: nest building starts at the threshold).</li>
 *     <li>{@code cooldown}: female rest time after spawning.</li>
 * </ul>
 */
public final class BettaBreeding {
    public static final double CONDITIONING_DAYS = 2.0;

    double conditioned;
    double ready;
    double cooldown;

    public void condition() {
        conditioned = CONDITIONING_DAYS;
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
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        if (conditioned > 0) t.putDouble("conditioned", conditioned);
        if (ready > 0) t.putDouble("ready", ready);
        if (cooldown > 0) t.putDouble("cooldown", cooldown);
        return t;
    }

    public static BettaBreeding load(CompoundTag t) {
        BettaBreeding b = new BettaBreeding();
        b.conditioned = t.getDouble("conditioned");
        b.ready = t.getDouble("ready");
        b.cooldown = t.getDouble("cooldown");
        return b;
    }
}
