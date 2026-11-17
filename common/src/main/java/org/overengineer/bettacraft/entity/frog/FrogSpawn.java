package org.overengineer.bettacraft.entity.frog;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;

/**
 * M47 part c: one clump of dwarf clawed frog spawn floating at the water surface of a tank (data, like the clutches of the
 * tankmates): where it floats, how many eggs are left and how old it is (days). Fish and bettas eat the eggs, floating
 * plants hide them; after {@link FrogBreeding#hatchDays()} the eggs hatch into {@link Tadpole}s.
 */
public final class FrogSpawn {
    public final int id;
    public double x, y, z;
    public int count;
    public double age;

    public FrogSpawn(int id, double x, double y, double z, int count) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.count = count;
    }

    public FrogSpawn copy() {
        FrogSpawn s = new FrogSpawn(id, x, y, z, count);
        s.age = age;
        return s;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putDouble("x", x);
        t.putDouble("y", y);
        t.putDouble("z", z);
        t.putInt("count", count);
        t.putDouble("age", age);
        return t;
    }

    @Nullable
    public static FrogSpawn load(CompoundTag t) {
        FrogSpawn s = new FrogSpawn(t.getIntOr("id", 0), t.getDoubleOr("x", 0d), t.getDoubleOr("y", 0d), t.getDoubleOr("z", 0d), Math.max(0, t.getIntOr("count", 0)));
        s.age = Math.max(0, t.getDoubleOr("age", 0d));
        if (!Double.isFinite(s.x) || !Double.isFinite(s.y) || !Double.isFinite(s.z) || !Double.isFinite(s.age) || s.count <= 0) return null;
        return s;
    }
}
