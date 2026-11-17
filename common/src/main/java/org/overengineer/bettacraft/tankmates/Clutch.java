package org.overengineer.bettacraft.tankmates;

import java.util.Locale;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.ServerConfig;

/**
 * M19: eggs, larvae or young of a tankmate species that are data in the tank (like the betta's bubble nest), schema 9.
 * One clutch is one spot: a few corydoras eggs glued to the glass, eggs scattered into plants or a spawning mop, a pleco
 * clutch in a cave, a gourami foam nest at the surface, or the young in a breeding box. When the larvae are free
 * swimming they become fry entities (from the box: juveniles).
 */
public final class Clutch {
    public enum Kind {
        /** Glued to the glass (face = direction of the glass seen from inside), decoration or leaves (corydoras, otocinclus). */
        GLASS,
        /** Scattered into plants, moss or a spawning mop, or onto the bottom (tetras, rasboras, kuhli loaches). */
        SCATTER,
        /** In a spawning cave, guarded by the male (plecos). */
        CAVE,
        /** A foam nest at the surface, guarded by the male (dwarf gourami); {@link #building} while he builds it. */
        FOAM,
        /** Young livebearers in the breeding box (safe from the adults until they are juveniles). */
        BOX,
        /**
         * M29: open brooders' eggs on a flat stone, a broad leaf or the glass (face as for {@link #GLASS}), guarded by the
         * pair ({@link #guardian} the male, {@link #partner} the female); without both the brood dies.
         */
        OPEN,
        /**
         * M30 part c: one leathery egg case of a shark (mermaid's purse) under the decoration or on the ground, one pup in it;
         * the adults cannot eat it, it hatches only with room by the size rule.
         */
        EGG_CASE;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Kind byId(String id) {
            for (Kind k : values()) if (k.id().equals(id)) return k;
            return SCATTER;
        }
    }

    public final int id;
    public final TankmateSpecies species;
    public final Kind kind;
    public double x, y, z;
    /** Glass face (0..3 = N, S, W, E like the algae) or -1. */
    public int face = -1;
    public int count;
    /** Days since laid (box: since born). */
    public double age;
    public TankmateGenes mother, father;
    public int generation;
    @Nullable
    public UUID guardian;
    /** M29: the second guarding parent of an open brood (the female). */
    @Nullable
    public UUID partner;
    /** Share of the eggs that the adults cannot reach (dense plants, a spawning mop). */
    public double cover;
    /** Foam nest: still being built (progress in days). */
    public boolean building;
    public double progress;
    /** Days without a guarding male (cave and foam clutches die without him after a while). */
    public double unguarded;
    /** M48 part b: clownfish larvae that got rotifers (saved as "rotifers" when true). */
    public boolean rotifers;

    // client view
    public int clientStage = -1;

    public Clutch(int id, TankmateSpecies species, Kind kind, double x, double y, double z, int count, TankmateGenes mother, TankmateGenes father) {
        this.id = id;
        this.species = species;
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.z = z;
        this.count = count;
        this.mother = mother;
        this.father = father;
    }

    public Vec3 pos() {
        return new Vec3(x, y, z);
    }

    // ------------------------------------------------------------------ timing

    /** Egg days of the species and kind (config tankmates.clutchSpeed divides them). */
    public static double eggDays(TankmateSpecies s) {
        double base = switch (s.breeding()) {
            case GLASS_EGGS -> 1.0;
            case CAVE -> 1.0;
            case ANEMONE -> 1.5; // M20: clownfish eggs take a week in nature
            case OPEN, CICHLID_CAVE -> 0.8; // M29: cichlid eggs hatch in two to three days
            case SCATTER -> 0.3;
            case FOAM_NEST -> 0.3;
            case EGG_CASE -> TankmateBreeding.EGG_CASE_DAYS; // M30 part c
            default -> 0;
        };
        return base / speed();
    }

    /** Larva days (yolk sac; plecos stay in the cave longer). */
    public static double larvaDays(TankmateSpecies s) {
        double base = switch (s.breeding()) {
            case GLASS_EGGS -> 0.5;
            case CAVE -> 1.0;
            case ANEMONE -> org.overengineer.bettacraft.reef.ClownLarvae.active() ? org.overengineer.bettacraft.reef.ClownLarvae.larvaDays() : 1.0; // M48 part b
            // M29: the parents keep the wrigglers in a pit or cave; discus young feed on their parents' skin for days more
            case OPEN, CICHLID_CAVE -> s.parentsFeed() ? 2.2 : 1.0;
            case SCATTER -> 0.6;
            case FOAM_NEST -> 0.5;
            default -> 0;
        };
        return base / speed();
    }

    private static double speed() {
        return Math.max(0.05, ServerConfig.getOr(ServerConfig.TANKMATES_CLUTCH_SPEED, 1.0));
    }

    /** Days until the young swim free (box: until they are juveniles and leave the box). */
    public double totalDays() {
        if (kind == Kind.BOX) return org.overengineer.bettacraft.entity.betta.LifeStage.FRY.days() * species.growth();
        return eggDays(species) + larvaDays(species);
    }

    /** 0 = eggs, 1 = larvae, 2 = fry (box). */
    public int stage() {
        if (clientStage >= 0) return clientStage;
        if (kind == Kind.BOX) return 2;
        if (building) return 0;
        return age < eggDays(species) ? 0 : 1;
    }

    public boolean hasYoung() {
        return !building && count > 0;
    }

    public double daysLeft() {
        return Math.max(0, totalDays() - age);
    }

    public Clutch copy(int newId) {
        Clutch c = new Clutch(newId, species, kind, x, y, z, count, mother, father);
        c.face = face;
        c.age = age;
        c.generation = generation;
        c.guardian = guardian;
        c.partner = partner;
        c.cover = cover;
        c.building = building;
        c.progress = progress;
        c.unguarded = unguarded;
        c.rotifers = rotifers;
        return c;
    }

    // ------------------------------------------------------------------ NBT

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putString("species", species.id());
        t.putString("kind", kind.id());
        t.putDouble("x", x);
        t.putDouble("y", y);
        t.putDouble("z", z);
        t.putInt("face", face);
        t.putInt("count", count);
        t.putDouble("age", age);
        t.put("mother", mother.save());
        t.put("father", father.save());
        t.putInt("gen", generation);
        if (guardian != null) t.putUUID("guardian", guardian);
        if (partner != null) t.putUUID("partner", partner);
        t.putDouble("cover", cover);
        if (building) t.putBoolean("building", true);
        t.putDouble("progress", progress);
        t.putDouble("unguarded", unguarded);
        if (rotifers) t.putBoolean("rotifers", true); // M48 part b
        return t;
    }

    /** Null for a species that no longer exists (data of a newer version or a broken entry). */
    @Nullable
    public static Clutch load(CompoundTag t) {
        TankmateSpecies s = TankmateSpecies.byId(t.getString("species"));
        if (s == null) return null;
        double x = t.getDouble("x"), y = t.getDouble("y"), z = t.getDouble("z");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return null;
        Clutch c = new Clutch(t.getInt("id"), s, Kind.byId(t.getString("kind")), x, y, z, Math.max(0, t.getInt("count")),
                TankmateGenes.load(t.getCompound("mother"), s), TankmateGenes.load(t.getCompound("father"), s));
        c.face = t.contains("face", Tag.TAG_ANY_NUMERIC) ? t.getInt("face") : -1;
        c.age = Math.max(0, t.getDouble("age"));
        c.generation = t.getInt("gen");
        c.guardian = t.hasUUID("guardian") ? t.getUUID("guardian") : null;
        c.partner = t.hasUUID("partner") ? t.getUUID("partner") : null;
        c.cover = Math.max(0, Math.min(1, t.getDouble("cover")));
        c.building = t.getBoolean("building");
        c.progress = t.getDouble("progress");
        c.unguarded = t.getDouble("unguarded");
        c.rotifers = t.getBoolean("rotifers");
        return c;
    }

    /** What the clients draw: species, kind, place, count, stage (and the foam nest's progress). */
    public CompoundTag saveClientView() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putString("species", species.id());
        t.putString("kind", kind.id());
        t.putFloat("x", (float) x);
        t.putFloat("y", (float) y);
        t.putFloat("z", (float) z);
        t.putByte("face", (byte) face);
        t.putShort("count", (short) Math.min(Short.MAX_VALUE, count));
        t.putByte("stage", (byte) stage());
        if (building) t.putBoolean("building", true);
        t.putFloat("progress", (float) progress);
        return t;
    }

    @Nullable
    public static Clutch loadClientView(CompoundTag t) {
        TankmateSpecies s = TankmateSpecies.byId(t.getString("species"));
        if (s == null) return null;
        TankmateGenes w = TankmateGenes.wild(s);
        Clutch c = new Clutch(t.getInt("id"), s, Kind.byId(t.getString("kind")), t.getFloat("x"), t.getFloat("y"), t.getFloat("z"),
                t.getShort("count"), w, w);
        c.face = t.getByte("face");
        c.clientStage = t.getByte("stage");
        c.building = t.getBoolean("building");
        c.progress = t.getFloat("progress");
        return c;
    }
}
