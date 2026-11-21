package org.overengineer.bettacraft.plants;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;

/**
 * One aquarium plant in a tank interior (M13), as stored in the plant index of its region: species, root position
 * (interior-local), growth stage, the fixed seed of its shape, the progress towards the next stage and the redness
 * its blocks show. The blocks in the interior follow these data ({@link InteriorPlants#apply}).
 */
public final class InteriorPlant {
    public final int id;
    public final PlantSpecies species;
    /** Interior-local root position (0,0,0 = first block above the floor, min corner). */
    public int x, y, z;
    public final long seed;
    /** Stage the blocks in the interior show (0 = cutting). */
    public int stage;
    /** Stage the plant has reached by its growth; applied to the blocks when it differs from {@link #stage}. */
    public int target;
    /** Progress towards the next stage (0..1), or towards the next side shoot / runner once full-grown. */
    public double progress;
    /** Redness of the blocks (0..3, red species only). */
    public int tint;
    /** Blocks the plant has at {@link #stage} (plant mass), and of them at the water surface (shade). */
    public int blocks = 1, surface;
    /**
     * Interior blocks from the root up to the water surface when the plant began to grow (0 = not yet): fixes its shape,
     * so the same plant keeps its blocks when the water level changes later.
     */
    public int room;
    /** Smoothed redness 0..1 the {@link #tint} follows (day and night under the sky must not flip it twice a day). */
    public double redness;
    /**
     * M22.1: version of the shape generator of this plant ({@link PlantShapes#VERSION}): new plants get the current one,
     * plants saved before M22.1 have none and keep version 1 (their blocks never change shape).
     */
    public int shapeVersion = PlantShapes.VERSION;
    /** M56: how far a terrarium plant languishes in a bad climate (at 1 it loses a stage, {@link LandPlants#wilt}). */
    public double wilt;

    public InteriorPlant(int id, PlantSpecies species, int x, int y, int z, long seed, int stage) {
        this.id = id;
        this.species = species;
        this.x = x;
        this.y = y;
        this.z = z;
        this.seed = seed;
        this.stage = stage;
        this.target = stage;
    }

    public boolean fullGrown() {
        return target >= species.stages();
    }

    public long rootKey() {
        return net.minecraft.core.BlockPos.asLong(x, y, z);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putString("species", species.id());
        t.putIntArray("pos", new int[]{x, y, z});
        t.putLong("seed", seed);
        t.putInt("stage", stage);
        if (target != stage) t.putInt("target", target);
        t.putDouble("progress", progress);
        t.putInt("tint", tint);
        t.putInt("blocks", blocks);
        t.putInt("surface", surface);
        t.putInt("room", room);
        t.putDouble("redness", redness);
        t.putInt("shape", shapeVersion);
        if (wilt > 0) t.putDouble("wilt", wilt); // M56
        return t;
    }

    /** Null for a species this version does not know. */
    @Nullable
    public static InteriorPlant load(CompoundTag t) {
        PlantSpecies s = PlantSpecies.byIdOrNull(t.getStringOr("species", ""));
        int[] p = t.getIntArray("pos").orElse(new int[0]);
        if (s == null || p.length != 3) return null;
        InteriorPlant pl = new InteriorPlant(t.getIntOr("id", 0), s, p[0], p[1], p[2], t.getLongOr("seed", 0L),
                Math.max(0, Math.min(s.stages(), t.getIntOr("stage", 0))));
        pl.target = t.contains("target") ? Math.max(0, Math.min(s.stages(), t.getIntOr("target", 0))) : pl.stage;
        pl.progress = t.getDoubleOr("progress", 0d);
        pl.tint = t.getIntOr("tint", 0);
        pl.blocks = Math.max(1, t.getIntOr("blocks", 0));
        pl.surface = t.getIntOr("surface", 0);
        pl.room = t.getIntOr("room", 0);
        pl.redness = t.getDoubleOr("redness", 0d);
        pl.shapeVersion = t.contains("shape") ? t.getIntOr("shape", 0) : 1;
        pl.wilt = t.getDoubleOr("wilt", 0d);
        return pl;
    }

    @Override
    public String toString() {
        return species.id() + "#" + id + "@" + x + "," + y + "," + z + " stage " + stage + (target != stage ? "->" + target : "");
    }
}
