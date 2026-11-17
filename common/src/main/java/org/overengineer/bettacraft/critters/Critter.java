package org.overengineer.bettacraft.critters;

import org.overengineer.bettacraft.util.NbtCompat;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * One shrimp, crayfish or snail of a tank (M14): a data animal, no entity. Species, sex, genes, age, hunger, a brood
 * (eggs carried by a female), the last moult (crayfish), whether it is dead (the body lies there for a while), and where
 * it is: absolute world position, on which surface ({@link #face}: floor, the glass of one side, or swimming), heading and
 * the spot it walks to. The {@code p*} / {@code lerpStart} fields are only used on the client to move it smoothly
 * between two updates.
 */
public final class Critter {
    public static final byte FLOOR = 0, SWIM = 5;
    /** Glass faces: 1 + 2D data value of the direction the glass lies in (south 0, west 1, north 2, east 3). */
    public static final byte GLASS_SOUTH = 1, GLASS_WEST = 2, GLASS_NORTH = 3, GLASS_EAST = 4;

    public final int id;
    public final CritterSpecies species;
    public boolean female;
    public CritterGenes genes;
    public int generation;
    /** Days since hatching (adult from {@link CritterSpecies#juvenileDays()}). */
    public double age;
    /** Days without enough food. */
    public double hunger;
    /** Days the female has been carrying eggs (-1: none). */
    public double eggs = -1;
    /** Genes of the father of the eggs (null without eggs or for hermaphrodites). */
    @Nullable
    public CritterGenes father;
    /** Crayfish: days since the last moult (soft and vulnerable right after it). */
    public double sinceMolt = 10;
    /** Days since the animal died (-1: alive); the body lies there and is eaten or rots. */
    public double dead = -1;
    /** M21.5: the individual look of racer nerites and apple snails ({@link CritterLooks}); 0 = the classic look. */
    public int look;
    /** M22.3: the hidden genes were analysed with the magnifier ({@link org.overengineer.bettacraft.genetics.GeneVisibility}). */
    public boolean analysed;

    public double x, y, z;
    public float yaw;
    public byte face = FLOOR;
    public double tx, ty, tz;
    /** Seconds it stays where it is before it picks a new spot. */
    public int idle;
    /** Trumpet snails: dug into the substrate (by day). */
    public boolean buried;
    // transient
    /** Swimming: where the jump started (for the arc). */
    double sx, sy, sz;
    /** Glass the animal walks to (face and block), 0 when it walks on the floor. */
    byte goalFace;
    int goalBx, goalBy, goalBz;
    /** M21: the way on the floor to the target ({x, y, z} per waypoint), null when it walks straight. */
    @javax.annotation.Nullable
    java.util.List<double[]> path;
    /** Server time until the feelers stay pulled in (a betta nipped at them). */
    public long retractUntil;

    // client
    public double px, py, pz;
    public float pyaw;
    public long lerpStart = Long.MIN_VALUE;
    /** Client: whether it moved in the last update (legs walk). */
    public boolean moving;
    /** Client: colour index and grade as sent (the genes are not sent). */
    public int clientColor = CritterGenes.WILD_COLOR;
    public float clientGrade;
    public float clientSize = 1;
    public byte clientFlags;

    public Critter(int id, CritterSpecies species, boolean female, CritterGenes genes, double x, double y, double z) {
        this.id = id;
        this.species = species;
        this.female = female || species.allFemale(); // M29 part d: marbled crayfish are all female
        this.genes = genes;
        this.x = this.px = this.tx = x;
        this.y = this.py = this.ty = y;
        this.z = this.pz = this.tz = z;
    }

    public boolean alive() {
        return dead < 0;
    }

    public boolean adult() {
        return age >= species.juvenileDays();
    }

    public boolean berried() {
        return eggs >= 0;
    }

    /** Crayfish right after a moult (half a day): soft shell. */
    public boolean soft() {
        return species.isCrayfish() && sinceMolt < 0.5;
    }

    /** Size factor 0.3 (just hatched) .. 1 (adult). */
    public double size() {
        double j = species.juvenileDays();
        if (j <= 0 || age >= j) return 1.0;
        return 0.3 + 0.7 * Math.max(0, age) / j;
    }

    public boolean onGlass() {
        return face >= GLASS_SOUTH && face <= GLASS_EAST;
    }

    /** Debug text (GameTests, smoke logs): position, surface, target, rest. */
    public String debug() {
        return String.format(java.util.Locale.ROOT, "%s#%d at %.3f %.3f %.3f face %d -> %.3f %.3f %.3f goal %d idle %d", species.id(), id, x, y, z, face, tx, ty, tz,
                goalFace, idle);
    }

    public Critter copy(int newId) {
        Critter c = new Critter(newId, species, female, genes, x, y, z);
        c.generation = generation;
        c.age = age;
        c.hunger = hunger;
        c.eggs = eggs;
        c.father = father;
        c.sinceMolt = sinceMolt;
        c.dead = dead;
        c.yaw = yaw;
        c.face = face;
        c.tx = tx;
        c.ty = ty;
        c.tz = tz;
        c.buried = buried;
        c.look = look;
        c.analysed = analysed;
        return c;
    }

    public CompoundTag save() {
        CompoundTag t = saveAnimal();
        t.putInt("id", id);
        t.putDouble("x", x);
        t.putDouble("y", y);
        t.putDouble("z", z);
        t.putFloat("yaw", yaw);
        t.putByte("face", face);
        if (buried) t.putBoolean("buried", true);
        return t;
    }

    /** The animal without its place in a tank (fish bags, moving between tanks). */
    public CompoundTag saveAnimal() {
        CompoundTag t = new CompoundTag();
        t.putString("species", species.id());
        t.putBoolean("female", female);
        t.put("genes", genes.save());
        t.putInt("gen", generation);
        t.putDouble("age", age);
        if (hunger > 0) t.putDouble("hunger", hunger);
        if (eggs >= 0) {
            t.putDouble("eggs", eggs);
            if (father != null) t.put("father", father.save());
        }
        if (species.isCrayfish()) t.putDouble("molt", sinceMolt);
        if (dead >= 0) t.putDouble("dead", dead);
        if (look != 0) t.putInt("look", look);
        org.overengineer.bettacraft.genetics.GeneVisibility.write(t, analysed); // M22.3
        return t;
    }

    static double finite(double v, double fallback) {
        return Double.isFinite(v) ? v : fallback;
    }

    /** Null for an unknown species. */
    @Nullable
    public static Critter load(CompoundTag t) {
        Critter c = loadAnimal(t, t.getIntOr("id", 0), t.getDoubleOr("x", 0d), t.getDoubleOr("y", 0d), t.getDoubleOr("z", 0d));
        if (c == null) return null;
        if (!Double.isFinite(c.x) || !Double.isFinite(c.y) || !Double.isFinite(c.z)) return null;
        c.yaw = t.getFloatOr("yaw", 0f);
        c.face = t.getByteOr("face", (byte) 0);
        if (c.face < 0 || c.face > SWIM) c.face = FLOOR;
        if (c.face == SWIM) c.face = FLOOR;
        c.buried = t.getBooleanOr("buried", false);
        return c;
    }

    @Nullable
    public static Critter loadAnimal(CompoundTag t, int id, double x, double y, double z) {
        CritterSpecies s = CritterSpecies.byIdOrNull(t.getStringOr("species", ""));
        if (s == null) return null;
        Critter c = new Critter(id, s, t.getBooleanOr("female", false), CritterGenes.load(t.getCompoundOrEmpty("genes")), x, y, z);
        c.generation = Math.max(0, t.getIntOr("gen", 0));
        // M17: NaN or infinite numbers (hand-edited or corrupt data) fall back to defaults (NaN broke sorting and hunger death)
        c.age = Math.max(0, finite(NbtCompat.has(t, "age", NbtCompat.ANY_NUMERIC) ? t.getDoubleOr("age", 0d) : s.juvenileDays(), s.juvenileDays()));
        c.hunger = Math.max(0, finite(t.getDoubleOr("hunger", 0d), 0));
        c.eggs = finite(NbtCompat.has(t, "eggs", NbtCompat.ANY_NUMERIC) ? t.getDoubleOr("eggs", 0d) : -1, -1);
        c.father = NbtCompat.has(t, "father", Tag.TAG_COMPOUND) ? CritterGenes.load(t.getCompoundOrEmpty("father")) : null;
        c.sinceMolt = finite(NbtCompat.has(t, "molt", NbtCompat.ANY_NUMERIC) ? t.getDoubleOr("molt", 0d) : 10, 10);
        c.dead = finite(NbtCompat.has(t, "dead", NbtCompat.ANY_NUMERIC) ? t.getDoubleOr("dead", 0d) : -1, -1);
        c.look = t.getIntOr("look", 0) & 0xFFFF; // M21.5: missing in older worlds -> 0, the look they had
        c.analysed = org.overengineer.bettacraft.genetics.GeneVisibility.analysed(t); // M22.3: older worlds: analysed
        return c;
    }

    /** Client: drawn position at {@code time} (client game time + partial tick), moving linearly over {@code duration} ticks. */
    public double renderX(double time, double duration) {
        return px + (x - px) * lerp(time, duration);
    }

    public double renderY(double time, double duration) {
        return py + (y - py) * lerp(time, duration);
    }

    public double renderZ(double time, double duration) {
        return pz + (z - pz) * lerp(time, duration);
    }

    public float renderYaw(double time, double duration) {
        float d = net.minecraft.util.Mth.wrapDegrees(yaw - pyaw);
        return pyaw + d * (float) Math.min(1, lerp(time, duration) * 3);
    }

    private double lerp(double time, double duration) {
        if (lerpStart == Long.MIN_VALUE) return 1;
        double f = (time - lerpStart) / duration;
        return Math.max(0, Math.min(1, f));
    }
}
