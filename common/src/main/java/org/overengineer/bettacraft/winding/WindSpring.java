package org.overengineer.bettacraft.winding;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import net.minecraft.nbt.CompoundTag;

/**
 * M37: the spring of a device that the crank winds. It holds device-ticks: one tick of one device's work (a filter, a
 * heater, a pump at full power ...). Its capacity is the device's charges times {@code winding.ticksPerCharge} (one
 * charge = one in-game day of one device); every crank click winds {@code capacity / clicks} (one charge takes
 * {@code winding.clicksPerCharge} clicks, about five seconds of holding the crank). It only runs down while the device
 * works without FE in a loaded chunk.
 */
public final class WindSpring {
    private final LongSupplier capacity;
    private final IntSupplier clicks;
    private final Runnable changed;
    private long ticks;

    public WindSpring(LongSupplier capacity, IntSupplier clicks, Runnable changed) {
        this.capacity = capacity;
        this.clicks = clicks;
        this.changed = changed;
    }

    /** A spring of a {@link Winding.Device}. */
    public static WindSpring of(Winding.Device d, Runnable changed) {
        return new WindSpring(d::capacity, d::clicksToFull, changed);
    }

    public long ticks() {
        return ticks;
    }

    public long capacity() {
        return Math.max(1, capacity.getAsLong());
    }

    /** 0..1. */
    public double fill() {
        return Math.min(1.0, ticks / (double) capacity());
    }

    public int percent() {
        return (int) Math.round(fill() * 100);
    }

    public boolean full() {
        return ticks >= capacity();
    }

    public boolean empty() {
        return ticks <= 0;
    }

    public void set(long v) {
        long n = Math.max(0, Math.min(capacity(), v));
        if (n == ticks) return;
        ticks = n;
        changed.run();
    }

    /** What one click of the crank winds. */
    public long step() {
        return Math.max(1, (long) Math.ceil(capacity() / (double) Math.max(1, clicks.getAsInt())));
    }

    /** One click of the crank; false when it is already full. */
    public boolean windStep() {
        if (full()) return false;
        set(ticks + step());
        return true;
    }

    /** Takes {@code n} device-ticks if it holds that much (true), else nothing. */
    public boolean use(long n) {
        if (n <= 0) return true;
        if (ticks < n) return false;
        ticks -= n;
        changed.run();
        return true;
    }

    /** Takes up to {@code n}; returns what it took. */
    public long drain(long n) {
        long took = Math.max(0, Math.min(n, ticks));
        if (took > 0) {
            ticks -= took;
            changed.run();
        }
        return took;
    }

    public void save(CompoundTag t, String key) {
        if (ticks > 0) t.putLong(key, ticks);
    }

    /** Reads it back (an int written by the food maker before M37 too). */
    public void load(CompoundTag t, String key) {
        ticks = Math.max(0, t.getLongOr(key, 0L));
    }
}
