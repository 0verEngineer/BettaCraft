package org.overengineer.bettacraft.tank;

import org.overengineer.bettacraft.util.NbtCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.genetics.BettaGenotype;

/**
 * Bubble nest of a tank (PLAN 5.4), held as data by the controller: built by a male at the surface, eggs and larvae
 * (stages 1-2) are data here and become fry entities when they swim free ({@link TankBreeding}).
 */
public final class BubbleNest {
    public enum Phase { BUILDING, BUILT, EGGS }

    /** One egg: the offspring is decided at spawning (genotype + sex). */
    public record Egg(BettaGenotype genotype, boolean female) {}

    Phase phase = Phase.BUILDING;
    UUID male;
    @Nullable
    UUID female;
    UUID fatherFishId;
    @Nullable
    UUID motherFishId;
    String fatherName = "";
    String motherName = "";
    BettaGenotype fatherGenotype;
    @Nullable
    BettaGenotype motherGenotype;
    int generation;
    Vec3 pos = Vec3.ZERO;
    /** BUILDING: days built so far; EGGS: age of the eggs in days. */
    double progress;
    /** BUILT: days without a female. */
    double waiting;
    final List<Egg> eggs = new ArrayList<>();
    /** Integral of the care score (0..1) over the egg/larva time and that time (survival rate). */
    double careSum, careTime;
    boolean hatchedLogged;

    BubbleNest() {}

    public Phase phase() { return phase; }
    public UUID male() { return male; }
    @Nullable public UUID female() { return female; }
    public Vec3 pos() { return pos; }
    public double progress() { return progress; }
    public int eggCount() { return eggs.size(); }
    public List<Egg> eggs() { return eggs; }
    public String fatherName() { return fatherName; }
    public String motherName() { return motherName; }
    public BettaGenotype fatherGenotype() { return fatherGenotype; }
    @Nullable public BettaGenotype motherGenotype() { return motherGenotype; }

    public boolean hasEggs() {
        return phase == Phase.EGGS && visibleEggCount() > 0;
    }

    /** EGG or LARVA while there are eggs. */
    public LifeStage stage() {
        return progress < LifeStage.EGG.days() ? LifeStage.EGG : LifeStage.LARVA;
    }

    /** Days until the larvae swim free. */
    public double daysUntilFree() {
        return Math.max(0, LifeStage.EGG.days() + LifeStage.LARVA.days() - progress);
    }

    public double careScore() {
        return careTime <= 0 ? 1.0 : careSum / careTime;
    }

    public BubbleNest copy() {
        return load(save());
    }

    // ------------------------------------------------------------------ NBT

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("phase", phase.name().toLowerCase(java.util.Locale.ROOT));
        if (male != null) NbtCompat.putUUID(t, "male", male);
        if (female != null) NbtCompat.putUUID(t, "female", female);
        if (fatherFishId != null) NbtCompat.putUUID(t, "father_id", fatherFishId);
        if (motherFishId != null) NbtCompat.putUUID(t, "mother_id", motherFishId);
        t.putString("father_name", fatherName);
        t.putString("mother_name", motherName);
        if (fatherGenotype != null) t.put("father_genome", fatherGenotype.save());
        if (motherGenotype != null) t.put("mother_genome", motherGenotype.save());
        t.putInt("generation", generation);
        t.putDouble("x", pos.x);
        t.putDouble("y", pos.y);
        t.putDouble("z", pos.z);
        t.putDouble("progress", progress);
        t.putDouble("waiting", waiting);
        ListTag el = new ListTag();
        for (Egg e : eggs) {
            CompoundTag c = new CompoundTag();
            c.put("genome", e.genotype().save());
            c.putBoolean("female", e.female());
            el.add(c);
        }
        t.put("eggs", el);
        t.putDouble("care_sum", careSum);
        t.putDouble("care_time", careTime);
        t.putBoolean("hatched", hatchedLogged);
        return t;
    }

    public static BubbleNest load(CompoundTag t) {
        BubbleNest n = new BubbleNest();
        String ph = t.getStringOr("phase", "");
        n.phase = "eggs".equals(ph) ? Phase.EGGS : "built".equals(ph) ? Phase.BUILT : Phase.BUILDING;
        n.male = NbtCompat.hasUUID(t, "male") ? NbtCompat.getUUID(t, "male") : new UUID(0, 0);
        n.female = NbtCompat.hasUUID(t, "female") ? NbtCompat.getUUID(t, "female") : null;
        n.fatherFishId = NbtCompat.hasUUID(t, "father_id") ? NbtCompat.getUUID(t, "father_id") : null;
        n.motherFishId = NbtCompat.hasUUID(t, "mother_id") ? NbtCompat.getUUID(t, "mother_id") : null;
        n.fatherName = t.getStringOr("father_name", "");
        n.motherName = t.getStringOr("mother_name", "");
        n.fatherGenotype = NbtCompat.has(t, "father_genome", Tag.TAG_COMPOUND) ? BettaGenotype.load(t.getCompoundOrEmpty("father_genome")) : null;
        n.motherGenotype = NbtCompat.has(t, "mother_genome", Tag.TAG_COMPOUND) ? BettaGenotype.load(t.getCompoundOrEmpty("mother_genome")) : null;
        n.generation = t.getIntOr("generation", 0);
        n.pos = new Vec3(t.getDoubleOr("x", 0d), t.getDoubleOr("y", 0d), t.getDoubleOr("z", 0d));
        n.progress = t.getDoubleOr("progress", 0d);
        n.waiting = t.getDoubleOr("waiting", 0d);
        ListTag el = t.getListOrEmpty("eggs");
        for (int i = 0; i < el.size(); i++) {
            CompoundTag c = el.getCompoundOrEmpty(i);
            n.eggs.add(new Egg(BettaGenotype.load(c.getCompoundOrEmpty("genome")), c.getBooleanOr("female", false)));
        }
        n.careSum = t.getDoubleOr("care_sum", 0d);
        n.careTime = t.getDoubleOr("care_time", 0d);
        n.hatchedLogged = t.getBooleanOr("hatched", false);
        return n;
    }

    /** Small client summary (rendering, break rule): phase, position, egg count, stage. */
    public CompoundTag saveClientView() {
        CompoundTag t = new CompoundTag();
        t.putString("phase", phase.name().toLowerCase(java.util.Locale.ROOT));
        t.putDouble("x", pos.x);
        t.putDouble("y", pos.y);
        t.putDouble("z", pos.z);
        t.putDouble("progress", progress);
        t.putInt("egg_count", eggs.size());
        t.putString("stage", phase == Phase.EGGS ? stage().id() : "");
        return t;
    }

    /** Client view: no genotypes, {@link #clientEggCount} instead of the egg list. */
    public static BubbleNest loadClientView(CompoundTag t) {
        BubbleNest n = new BubbleNest();
        String ph = t.getStringOr("phase", "");
        n.phase = "eggs".equals(ph) ? Phase.EGGS : "built".equals(ph) ? Phase.BUILT : Phase.BUILDING;
        n.pos = new Vec3(t.getDoubleOr("x", 0d), t.getDoubleOr("y", 0d), t.getDoubleOr("z", 0d));
        n.progress = t.getDoubleOr("progress", 0d);
        n.clientEggCount = t.getIntOr("egg_count", 0);
        n.clientStage = LifeStage.byId(t.getStringOr("stage", ""));
        return n;
    }

    int clientEggCount = -1;
    LifeStage clientStage = LifeStage.EGG;

    /** Egg count on either side. */
    public int visibleEggCount() {
        return clientEggCount >= 0 ? clientEggCount : eggs.size();
    }

    public LifeStage visibleStage() {
        return clientEggCount >= 0 ? clientStage : stage();
    }
}
