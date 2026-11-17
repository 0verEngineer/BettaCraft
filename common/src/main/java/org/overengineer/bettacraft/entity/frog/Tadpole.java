package org.overengineer.bettacraft.entity.frog;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;

/**
 * M47 part c: one tadpole of the dwarf clawed frog in a tank (a data animal like the shrimp, M14): the spot it hovers
 * around in the open water (the client lets it wiggle around it), its age in days and its hunger (days without enough fine
 * food). After {@link FrogBreeding#tadpoleDays()} it becomes a froglet ({@link DwarfFrogEntity}).
 */
public final class Tadpole {
    public final int id;
    public double x, y, z;
    public double age;
    public double hunger;

    public Tadpole(int id, double x, double y, double z) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Tadpole copy() {
        Tadpole k = new Tadpole(id, x, y, z);
        k.age = age;
        k.hunger = hunger;
        return k;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putDouble("x", x);
        t.putDouble("y", y);
        t.putDouble("z", z);
        t.putDouble("age", age);
        if (hunger > 0) t.putDouble("hunger", hunger);
        return t;
    }

    @Nullable
    public static Tadpole load(CompoundTag t) {
        Tadpole k = new Tadpole(t.getIntOr("id", 0), t.getDoubleOr("x", 0d), t.getDoubleOr("y", 0d), t.getDoubleOr("z", 0d));
        k.age = Math.max(0, t.getDoubleOr("age", 0d));
        k.hunger = Math.max(0, t.getDoubleOr("hunger", 0d));
        if (!Double.isFinite(k.x) || !Double.isFinite(k.y) || !Double.isFinite(k.z) || !Double.isFinite(k.age) || !Double.isFinite(k.hunger)) return null;
        return k;
    }

    /** Client view: the place and the growth (0..1 of the way to the froglet). */
    public CompoundTag saveClientView(double tadpoleDays) {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putFloat("x", (float) x);
        t.putFloat("y", (float) y);
        t.putFloat("z", (float) z);
        t.putFloat("g", (float) Math.min(1, age / Math.max(0.01, tadpoleDays)));
        return t;
    }

    /** Client: the growth 0..1 as sent. */
    public float clientGrowth;

    public static Tadpole loadClientView(CompoundTag t) {
        Tadpole k = new Tadpole(t.getIntOr("id", 0), t.getFloatOr("x", 0f), t.getFloatOr("y", 0f), t.getFloatOr("z", 0f));
        k.clientGrowth = t.getFloatOr("g", 0f);
        return k;
    }
}
