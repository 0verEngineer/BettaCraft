package org.overengineer.bettacraft.tank.interior;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.overengineer.bettacraft.data.schema.Schemas;

/** SavedData with all interior regions (stored in the overworld's data folder). Region ids are never reused. */
public final class InteriorRegions extends SavedData {
    public static final String NAME = "bettacraft_interior_regions";

    private final Int2ObjectOpenHashMap<InteriorRegion> regions = new Int2ObjectOpenHashMap<>();
    private int nextId = 0;

    public static InteriorRegions get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(InteriorRegions::new, InteriorRegions::load, null), NAME);
    }

    @Nullable
    public InteriorRegion get(int id) {
        return id < 0 ? null : regions.get(id);
    }

    public InteriorRegion allocate(int sizeX, int sizeY, int sizeZ, int scale, ResourceKey<Level> dim, BlockPos tankMin,
                                   @Nullable UUID tankId, @Nullable UUID owner) {
        // id 0 is skipped so that no region touches the world origin of the dimension
        if (nextId == 0) nextId = 1;
        int id = nextId++;
        InteriorRegion r = new InteriorRegion(id, sizeX, sizeY, sizeZ, scale, dim, tankMin, tankId, owner);
        regions.put(id, r);
        setDirty();
        return r;
    }

    public void remove(int id) {
        if (regions.remove(id) != null) setDirty();
    }

    public Iterable<InteriorRegion> all() {
        return regions.values();
    }

    public int count() {
        return regions.size();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        Schemas.INTERIOR_REGIONS.stamp(tag);
        tag.putInt("next_id", nextId);
        ListTag list = new ListTag();
        for (InteriorRegion r : regions.values()) list.add(r.save());
        tag.put("regions", list);
        return tag;
    }

    private static InteriorRegions load(CompoundTag raw, HolderLookup.Provider registries) {
        CompoundTag tag = Schemas.INTERIOR_REGIONS.migrate(raw);
        InteriorRegions d = new InteriorRegions();
        d.nextId = tag.getInt("next_id");
        for (Tag t : tag.getList("regions", Tag.TAG_COMPOUND)) {
            InteriorRegion r = InteriorRegion.load((CompoundTag) t);
            d.regions.put(r.id(), r);
            d.nextId = Math.max(d.nextId, r.id() + 1);
        }
        return d;
    }
}
