package org.overengineer.bettacraft.tank.plants;

import org.overengineer.bettacraft.util.NbtCompat;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * One plant of the surface layer of a tank (M12): species, position on the water surface (absolute world x/z, so it
 * stays where it is when tanks merge or split), size (0 = young, 1 = ready for an offshoot), leaf rotation and how red
 * it is (red root floater). The drift velocity is transient; the {@code prev*} / {@code lerp*} fields are only used on
 * the client to move the plant smoothly between two updates.
 */
public final class FloatingPlant {
    public final int id;
    public final FloatingPlantType type;
    public double x, z;
    public float size;
    /** Leaf rotation (radians). */
    public float rot;
    /** 0 = green, 1 = fully red (red root floater, follows the light). */
    public float red;

    // transient
    double vx, vz;
    /** Client: position the interpolation starts from and the client time the update came in. */
    public double prevX, prevZ;
    public long lerpStart = Long.MIN_VALUE;

    public FloatingPlant(int id, FloatingPlantType type, double x, double z, float size, float rot) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.z = z;
        this.size = Math.max(0, Math.min(1, size));
        this.rot = rot;
        this.prevX = x;
        this.prevZ = z;
    }

    public double radius() {
        return type.radius(size);
    }

    public double area() {
        double r = radius();
        return Math.PI * r * r;
    }

    public FloatingPlant copy() {
        FloatingPlant p = new FloatingPlant(id, type, x, z, size, rot);
        p.red = red;
        return p;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putString("type", type.id());
        t.putDouble("x", x);
        t.putDouble("z", z);
        t.putFloat("size", size);
        t.putFloat("rot", rot);
        if (red > 0) t.putFloat("red", red);
        return t;
    }

    /** Null for an unknown species (kept out of the list, logged by the caller if needed). */
    @Nullable
    public static FloatingPlant load(CompoundTag t) {
        FloatingPlantType type = FloatingPlantType.byIdOrNull(t.getStringOr("type", ""));
        if (type == null || !NbtCompat.has(t, "x", NbtCompat.ANY_NUMERIC) || !NbtCompat.has(t, "z", NbtCompat.ANY_NUMERIC)) return null;
        double x = t.getDoubleOr("x", 0d), z = t.getDoubleOr("z", 0d);
        if (!Double.isFinite(x) || !Double.isFinite(z)) return null;
        FloatingPlant p = new FloatingPlant(t.getIntOr("id", 0), type, x, z, t.contains("size") ? t.getFloatOr("size", 0f) : 0.5F, t.getFloatOr("rot", 0f));
        p.red = Math.max(0, Math.min(1, t.getFloatOr("red", 0f)));
        return p;
    }

    /** Client: the drawn position at {@code time} (client game time + partial tick), moving over {@code duration} ticks. */
    public double renderX(double time, double duration) {
        return prevX + (x - prevX) * lerp(time, duration);
    }

    public double renderZ(double time, double duration) {
        return prevZ + (z - prevZ) * lerp(time, duration);
    }

    private double lerp(double time, double duration) {
        if (lerpStart == Long.MIN_VALUE) return 1;
        double f = (time - lerpStart) / duration;
        f = Math.max(0, Math.min(1, f));
        return f * f * (3 - 2 * f);
    }
}
