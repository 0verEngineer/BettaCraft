package org.overengineer.bettacraft.terrarium;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

/**
 * M31 part b: a clutch of turtle eggs buried in the land of a tank (tank data, saved with the tank under
 * {@code turtle_eggs}; old tanks have none). Its eggs are a {@link TurtleEggsBlock} in the interior at {@link #interior}
 * (the miniature shows them); {@link #x}/{@link #y}/{@link #z} is the same spot in the tank, where the hatchlings come out.
 * {@link #incubate} adds a step at the nest temperature (pure, unit-tested): the progress, the mean temperature of the
 * thermosensitive period and the heat the embryos took.
 */
public final class HerpClutch {
    public final int id;
    public final HerpSpecies species;
    public int eggs;
    public double x, y, z;
    /** The egg block in the interior dimension. */
    public long interior;
    /** 0..1; hatches at 1. */
    public double progress;
    /** Degree-days and days within the thermosensitive period (their ratio is its mean temperature). */
    public double tspSum, tspDays;
    /** Days above the lethal temperature. */
    public double hotDays;
    /** Varieties of the parents (index in {@link HerpSpecies#varieties()}). */
    public int motherVariety, fatherVariety;
    /** Seed of the hatching (the sexes, the varieties): fixed when laid, so a clutch always gives the same young. */
    public long seed;
    public int generation;

    public HerpClutch(int id, HerpSpecies species, int eggs, Vec3 at, BlockPos interior, long seed) {
        this.id = id;
        this.species = species;
        this.eggs = eggs;
        this.x = at.x;
        this.y = at.y;
        this.z = at.z;
        this.interior = interior.asLong();
        this.seed = seed;
    }

    public Vec3 pos() {
        return new Vec3(x, y, z);
    }

    public BlockPos interiorPos() {
        return BlockPos.of(interior);
    }

    /** One step at the nest temperature {@code temp} for {@code days} ({@code speed}: terrarium.incubationSpeed). */
    public void incubate(double temp, double days, double speed) {
        if (days <= 0 || eggs <= 0) return;
        if (Double.isFinite(temp) && temp > EggSex.LETHAL) hotDays += days;
        double before = progress;
        progress = Math.min(1.0, progress + EggSex.rate(species, temp, speed) * days);
        // the part of this step within the thermosensitive period, weighted by the time it took
        double from = Math.max(before, EggSex.TSP_FROM), to = Math.min(progress, EggSex.TSP_TO);
        if (to > from && progress > before && Double.isFinite(temp)) {
            double share = (to - from) / (progress - before);
            tspSum += temp * days * share;
            tspDays += days * share;
        }
    }

    /** Whether the heat killed the embryos. */
    public boolean cooked() {
        return hotDays >= EggSex.LETHAL_DAYS;
    }

    public boolean ready() {
        return progress >= 1.0 - 1e-9;
    }

    /** Mean nest temperature of the thermosensitive period (NaN before it). */
    public double tspTemp() {
        return tspDays > 1e-9 ? tspSum / tspDays : Double.NaN;
    }

    /** The share of females this clutch will give (from what it had so far; 0.5 before the thermosensitive period). */
    public double femaleShare() {
        return EggSex.femaleChance(species, tspTemp());
    }

    public HerpClutch copy() {
        HerpClutch c = new HerpClutch(id, species, eggs, pos(), interiorPos(), seed);
        c.progress = progress;
        c.tspSum = tspSum;
        c.tspDays = tspDays;
        c.hotDays = hotDays;
        c.motherVariety = motherVariety;
        c.fatherVariety = fatherVariety;
        c.generation = generation;
        return c;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putString("species", species.id());
        t.putInt("eggs", eggs);
        t.putDouble("x", x);
        t.putDouble("y", y);
        t.putDouble("z", z);
        t.putLong("interior", interior);
        t.putDouble("progress", progress);
        t.putDouble("tsp_sum", tspSum);
        t.putDouble("tsp_days", tspDays);
        t.putDouble("hot", hotDays);
        t.putInt("mother", motherVariety);
        t.putInt("father", fatherVariety);
        t.putLong("seed", seed);
        t.putInt("gen", generation);
        return t;
    }

    /** Null for a species that no longer exists or broken numbers. */
    @Nullable
    public static HerpClutch load(CompoundTag t) {
        HerpSpecies s = HerpSpecies.byId(t.getStringOr("species", ""));
        if (s == null) return null;
        double x = t.getDoubleOr("x", 0d), y = t.getDoubleOr("y", 0d), z = t.getDoubleOr("z", 0d);
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return null;
        HerpClutch c = new HerpClutch(t.getIntOr("id", 0), s, Math.max(0, Math.min(64, t.getIntOr("eggs", 0))), new Vec3(x, y, z),
                BlockPos.of(t.getLongOr("interior", 0L)), t.getLongOr("seed", 0L));
        c.progress = finite(t.getDoubleOr("progress", 0d), 0, 1);
        c.tspSum = finite(t.getDoubleOr("tsp_sum", 0d), 0, 1e6);
        c.tspDays = finite(t.getDoubleOr("tsp_days", 0d), 0, 1e4);
        c.hotDays = finite(t.getDoubleOr("hot", 0d), 0, 1e4);
        c.motherVariety = Math.max(0, Math.min(s.varieties().size() - 1, t.getIntOr("mother", 0)));
        c.fatherVariety = Math.max(0, Math.min(s.varieties().size() - 1, t.getIntOr("father", 0)));
        c.generation = Math.max(0, t.getIntOr("gen", 0));
        return c;
    }

    private static double finite(double v, double min, double max) {
        return Double.isFinite(v) ? Math.max(min, Math.min(max, v)) : min;
    }
}
