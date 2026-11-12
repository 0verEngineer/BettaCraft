package org.overengineer.bettacraft.tank;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Collection;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

/**
 * Data of one tank, held by its controller block entity.
 * The controller stores the <b>set</b> of member blocks (not only a box) so free shapes can come later (M10).
 */
public final class TankData {
    public static final long MB_PER_BLOCK = 1000L;

    private UUID id = UUID.randomUUID();
    private final LongSet members = new LongOpenHashSet();
    private int minX, minY, minZ, maxX, maxY, maxZ;
    private int scale = 16;
    private long waterMb;
    private int regionId = -1;
    @Nullable
    private UUID owner;

    // Prepared for later milestones (PLAN 2), no logic yet.
    private float waterQuality = 1.0F;
    private float evaporation = 0.0F;
    private CompoundTag zones = new CompoundTag();
    private long powerBuffer = 0L;

    /** Client-side copy only knows box and summary values, not the member set. */
    private boolean clientView;
    private int clientMemberCount;

    public TankData() {}

    public static TankData create(Collection<BlockPos> blocks, int scale) {
        TankData d = new TankData();
        d.scale = scale;
        for (BlockPos p : blocks) {
            d.members.add(p.asLong());
        }
        d.recomputeBounds();
        return d;
    }

    // ------------------------------------------------------------------ members / geometry

    public LongSet members() {
        return members;
    }

    public int memberCount() {
        return clientView ? clientMemberCount : members.size();
    }

    public boolean contains(BlockPos pos) {
        return members.contains(pos.asLong());
    }

    public void recomputeBounds() {
        minX = minY = minZ = Integer.MAX_VALUE;
        maxX = maxY = maxZ = Integer.MIN_VALUE;
        for (long l : members) {
            int x = BlockPos.getX(l), y = BlockPos.getY(l), z = BlockPos.getZ(l);
            minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
        }
    }

    public BlockPos min() {
        return new BlockPos(minX, minY, minZ);
    }

    public BlockPos max() {
        return new BlockPos(maxX, maxY, maxZ);
    }

    public int sizeX() { return maxX - minX + 1; }
    public int sizeY() { return maxY - minY + 1; }
    public int sizeZ() { return maxZ - minZ + 1; }

    public boolean isCuboid() {
        return (long) sizeX() * sizeY() * sizeZ() == memberCount();
    }

    // ------------------------------------------------------------------ water

    public long capacityMb() {
        return memberCount() * MB_PER_BLOCK;
    }

    public long waterMb() {
        return waterMb;
    }

    public void setWaterMb(long mb) {
        this.waterMb = Math.max(0, Math.min(mb, capacityMb()));
    }

    /**
     * Water height (in 1/16) of every block in the given layer, filling bottom-up.
     * Partial layers are rounded up so that any water is visible.
     */
    public int fillOfLayer(int y) {
        // Count blocks per layer below y (works for arbitrary shapes, cheap for cuboids).
        long below = 0;
        int layerCount = 0;
        for (long l : members) {
            int ly = BlockPos.getY(l);
            if (ly < y) below++;
            else if (ly == y) layerCount++;
        }
        return fillFor(waterMb, below * MB_PER_BLOCK, layerCount * MB_PER_BLOCK);
    }

    /** Pure helper (unit-tested): fill in 1/16 of a layer with the given capacity whose bottom starts at {@code belowMb}. */
    public static int fillFor(long waterMb, long belowMb, long layerCapacityMb) {
        if (layerCapacityMb <= 0) return 0;
        long inLayer = Math.max(0, Math.min(waterMb - belowMb, layerCapacityMb));
        if (inLayer <= 0) return 0;
        return (int) Math.min(16, Math.max(1, (inLayer * 16 + layerCapacityMb - 1) / layerCapacityMb));
    }

    // ------------------------------------------------------------------ misc accessors

    public UUID id() { return id; }
    public void setId(UUID id) { this.id = id; }
    public int scale() { return scale; }
    public void setScale(int scale) { this.scale = scale; }
    public int regionId() { return regionId; }
    public void setRegionId(int regionId) { this.regionId = regionId; }
    @Nullable public UUID owner() { return owner; }
    public void setOwner(@Nullable UUID owner) { this.owner = owner; }
    public float waterQuality() { return waterQuality; }
    public float evaporation() { return evaporation; }
    public long powerBuffer() { return powerBuffer; }

    // ------------------------------------------------------------------ NBT

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", id);
        t.putLongArray("members", members.toLongArray());
        t.putInt("scale", scale);
        t.putLong("water_mb", waterMb);
        t.putInt("region", regionId);
        if (owner != null) t.putUUID("owner", owner);
        t.putFloat("water_quality", waterQuality);
        t.putFloat("evaporation", evaporation);
        t.put("zones", zones.copy());
        t.putLong("power", powerBuffer);
        return t;
    }

    public static TankData load(CompoundTag t) {
        TankData d = new TankData();
        if (t.hasUUID("id")) d.id = t.getUUID("id");
        for (long l : t.getLongArray("members")) d.members.add(l);
        d.scale = t.getInt("scale");
        d.waterMb = t.getLong("water_mb");
        d.regionId = t.contains("region", Tag.TAG_ANY_NUMERIC) ? t.getInt("region") : -1;
        d.owner = t.hasUUID("owner") ? t.getUUID("owner") : null;
        if (t.contains("water_quality")) d.waterQuality = t.getFloat("water_quality");
        if (t.contains("evaporation")) d.evaporation = t.getFloat("evaporation");
        d.zones = t.getCompound("zones").copy();
        d.powerBuffer = t.getLong("power");
        d.recomputeBounds();
        return d;
    }

    /** Small client view: box, scale, region, water. */
    public CompoundTag saveClientView() {
        CompoundTag t = new CompoundTag();
        t.putIntArray("min", new int[]{minX, minY, minZ});
        t.putIntArray("max", new int[]{maxX, maxY, maxZ});
        t.putInt("count", members.size());
        t.putInt("scale", scale);
        t.putLong("water_mb", waterMb);
        t.putInt("region", regionId);
        return t;
    }

    public static TankData loadClientView(CompoundTag t) {
        TankData d = new TankData();
        d.clientView = true;
        int[] mn = t.getIntArray("min"), mx = t.getIntArray("max");
        if (mn.length == 3 && mx.length == 3) {
            d.minX = mn[0]; d.minY = mn[1]; d.minZ = mn[2];
            d.maxX = mx[0]; d.maxY = mx[1]; d.maxZ = mx[2];
        }
        d.clientMemberCount = t.getInt("count");
        d.scale = t.getInt("scale");
        d.waterMb = t.getLong("water_mb");
        d.regionId = t.getInt("region");
        return d;
    }

    @Override
    public String toString() {
        return "Tank[" + id + " " + sizeX() + "x" + sizeY() + "x" + sizeZ() + " at " + NbtUtils.writeBlockPos(min()) + ", "
                + waterMb + "/" + capacityMb() + " mB, scale " + scale + ", region " + regionId + "]";
    }
}
