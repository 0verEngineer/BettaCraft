package org.overengineer.bettacraft.plants;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * The aquarium plants of one interior region (M13). The care loop of the tank grows them on these data only; their
 * blocks are written when a stage is due (also while nobody is in the interior, see {@link InteriorPlants}).
 */
public final class PlantIndex {
    private final List<InteriorPlant> plants = new ArrayList<>();
    private int nextId = 1;

    public List<InteriorPlant> all() {
        return plants;
    }

    public boolean isEmpty() {
        return plants.isEmpty();
    }

    public int size() {
        return plants.size();
    }

    public int nextId() {
        return nextId++;
    }

    public InteriorPlant add(PlantSpecies s, int x, int y, int z, long seed, int stage) {
        InteriorPlant p = new InteriorPlant(nextId(), s, x, y, z, seed, stage);
        plants.add(p);
        return p;
    }

    /** Adds a plant moved over from another region (merge / split) under a new id. */
    public InteriorPlant adopt(InteriorPlant o, int x, int y, int z) {
        InteriorPlant p = new InteriorPlant(nextId(), o.species, x, y, z, o.seed, o.stage);
        p.target = o.target;
        p.progress = o.progress;
        p.tint = o.tint;
        p.blocks = o.blocks;
        p.surface = o.surface;
        p.room = o.room;
        p.redness = o.redness;
        plants.add(p);
        return p;
    }

    @Nullable
    public InteriorPlant at(int x, int y, int z) {
        for (InteriorPlant p : plants) if (p.x == x && p.y == y && p.z == z) return p;
        return null;
    }

    @Nullable
    public InteriorPlant byId(int id) {
        for (InteriorPlant p : plants) if (p.id == id) return p;
        return null;
    }

    public boolean remove(InteriorPlant p) {
        return plants.remove(p);
    }

    /** Plant mass: all plant blocks. */
    public int blocks() {
        int n = 0;
        for (InteriorPlant p : plants) n += p.blocks;
        return n;
    }

    /** Plant blocks at the water surface (stem tops, lotus pads). */
    public int surfaceBlocks() {
        int n = 0;
        for (InteriorPlant p : plants) n += p.surface;
        return n;
    }

    public ListTag save() {
        ListTag l = new ListTag();
        for (InteriorPlant p : plants) l.add(p.save());
        return l;
    }

    public CompoundTag saveAll() {
        CompoundTag t = new CompoundTag();
        t.put("list", save());
        t.putInt("next_id", nextId);
        return t;
    }

    public void load(CompoundTag t) {
        plants.clear();
        nextId = Math.max(1, t.getInt("next_id"));
        for (Tag e : t.getList("list", Tag.TAG_COMPOUND)) {
            InteriorPlant p = InteriorPlant.load((CompoundTag) e);
            if (p == null) continue;
            plants.add(p);
            nextId = Math.max(nextId, p.id + 1);
        }
    }
}
