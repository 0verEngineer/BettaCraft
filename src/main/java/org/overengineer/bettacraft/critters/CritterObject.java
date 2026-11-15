package org.overengineer.bettacraft.critters;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Something the small animals leave in a tank (M14): the pink clutch of an apple snail on the glass above the water line
 * (hatches into young unless it is taken off) or the shed shell of a crayfish after a moult (disappears after a while).
 */
public final class CritterObject {
    /** M29 part d: AXOLOTL_EGGS, the eggs of a vanilla axolotl on a plant ({@link org.overengineer.bettacraft.tank.AxolotlCare}). */
    public enum Kind { CLUTCH, MOLT, AXOLOTL_EGGS }

    public final int id;
    public final Kind kind;
    public double x, y, z;
    /** Glass face of a clutch (as {@link Critter#face}), 0 on the floor. */
    public byte face;
    public float yaw;
    /** Days since it was laid / shed. */
    public double age;
    /** Clutch: the parents' genes and the number of young. */
    @Nullable
    public CritterGenes mother, father;
    public int count;
    public int generation;
    /** Moult: the species and size of the animal that shed it. */
    public CritterSpecies species = CritterSpecies.APPLE_SNAIL;
    public float size = 1;

    public CritterObject(int id, Kind kind, double x, double y, double z) {
        this.id = id;
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public CritterObject copy(int newId) {
        CritterObject o = new CritterObject(newId, kind, x, y, z);
        o.face = face;
        o.yaw = yaw;
        o.age = age;
        o.mother = mother;
        o.father = father;
        o.count = count;
        o.generation = generation;
        o.species = species;
        o.size = size;
        return o;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putString("kind", kind.name().toLowerCase(java.util.Locale.ROOT));
        t.putDouble("x", x);
        t.putDouble("y", y);
        t.putDouble("z", z);
        t.putByte("face", face);
        t.putFloat("yaw", yaw);
        t.putDouble("age", age);
        if (mother != null) t.put("mother", mother.save());
        if (father != null) t.put("father", father.save());
        t.putInt("count", count);
        t.putInt("gen", generation);
        t.putString("species", species.id());
        t.putFloat("size", size);
        return t;
    }

    @Nullable
    public static CritterObject load(CompoundTag t) {
        Kind k = switch (t.getString("kind")) {
            case "clutch" -> Kind.CLUTCH;
            case "molt" -> Kind.MOLT;
            case "axolotl_eggs" -> Kind.AXOLOTL_EGGS;
            default -> null;
        };
        if (k == null) return null;
        CritterObject o = new CritterObject(t.getInt("id"), k, t.getDouble("x"), t.getDouble("y"), t.getDouble("z"));
        if (!Double.isFinite(o.x) || !Double.isFinite(o.y) || !Double.isFinite(o.z)) return null;
        o.face = t.getByte("face");
        o.yaw = t.getFloat("yaw");
        o.age = Math.max(0, Critter.finite(t.getDouble("age"), 0)); // M17: no NaN / negative ages
        o.mother = t.contains("mother", Tag.TAG_COMPOUND) ? CritterGenes.load(t.getCompound("mother")) : null;
        o.father = t.contains("father", Tag.TAG_COMPOUND) ? CritterGenes.load(t.getCompound("father")) : null;
        o.count = Math.max(0, Math.min(1000, t.getInt("count"))); // M17: a corrupt count cannot hatch thousands
        o.generation = Math.max(0, t.getInt("gen"));
        o.species = CritterSpecies.byId(t.getString("species"));
        o.size = t.contains("size") && Float.isFinite(t.getFloat("size")) ? t.getFloat("size") : 1F;
        return o;
    }
}
