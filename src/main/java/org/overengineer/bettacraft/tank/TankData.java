package org.overengineer.bettacraft.tank;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Data of one tank, held by its controller block entity.
 * The controller stores the <b>set</b> of member blocks (not only a box): since M10 a tank can have any face-connected
 * shape (L, T, steps ...). Water fills layer by layer ({@link TankShape}).
 */
public final class TankData {
    public static final long MB_PER_BLOCK = 1000L;
    /** Evaporation is taken from the water in steps of this many mB (fewer block updates). */
    public static final double EVAPORATION_BATCH_MB = 10.0;

    private UUID id = UUID.randomUUID();
    private final LongSet members = new LongOpenHashSet();
    private int minX, minY, minZ, maxX, maxY, maxZ;
    private int scale = 16;
    private long waterMb;
    private int regionId = -1;
    @Nullable
    private UUID owner;

    // Water care and power (M9; prepared in M0). zones: prepared for a divider (optional-features), no logic.
    private float waterQuality = 1.0F;
    /** Evaporated water (mB) not yet taken from {@link #waterMb}: collected up to {@link #EVAPORATION_BATCH_MB} (M9). */
    private double evaporationCarry = 0.0;
    private CompoundTag zones = new CompoundTag();
    private long powerBuffer = 0L;
    /** Game time of the last fresh water poured in (-1 = never). */
    private long lastFreshWater = -1L;

    // Care data (M1).
    /** Heater slots of the tank menu up to M3; since M4 only taken out / moved into a filter (schema v3). */
    public static final int LEGACY_HEATER_SLOTS = 4;
    public static final int MAX_NAME_LENGTH = 32;
    public static final int MAX_LOG = 100;
    private float targetTemp = TankTemperature.DEFAULT_TARGET;
    private float waterTemp = TankTemperature.DEFAULT_TARGET;
    @Nullable
    private SimpleContainer legacyHeaters;
    private final String[] unknownHeaters = new String[LEGACY_HEATER_SLOTS];
    private String name = "";
    private boolean signVisible = true;
    private SignPosition signPos = SignPosition.TOP_CENTER;
    private Direction front = Direction.SOUTH;
    private boolean locked;
    private final Set<UUID> inhabitants = new LinkedHashSet<>();
    private final List<LogEntry> log = new ArrayList<>();
    /** Bubble nest (M2), null if there is none. */
    @Nullable
    private BubbleNest nest;
    /** M12: floating plants on the water surface (absolute x/z) and the next free plant id. */
    private final List<org.overengineer.bettacraft.tank.plants.FloatingPlant> plants = new ArrayList<>();
    private int nextPlantId = 1;

    /** Client-side copy: box, summary values and (M10) the member set, sent as a bit set for non-cuboid shapes. */
    private boolean clientView;
    /** Blocks per layer from minY (cached, the member set never changes after creation). */
    @Nullable
    private int[] layerCounts;
    private long surfaceCacheMb = Long.MIN_VALUE;
    private double surfaceCache;
    private int surfaceCacheHeadroom;
    private int clientMemberCount;
    private int clientInhabitants;

    public record LogEntry(long time, String key, List<String> args) {
        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putLong("t", time);
            t.putString("k", key);
            ListTag a = new ListTag();
            for (String s : args) a.add(StringTag.valueOf(s));
            t.put("a", a);
            return t;
        }

        static LogEntry load(CompoundTag t) {
            List<String> args = new ArrayList<>();
            ListTag a = t.getList("a", Tag.TAG_STRING);
            for (int i = 0; i < a.size(); i++) args.add(a.getString(i));
            return new LogEntry(t.getLong("t"), t.getString("k"), List.copyOf(args));
        }
    }

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

    public boolean contains(long pos) {
        return members.contains(pos);
    }

    public void recomputeBounds() {
        layerCounts = null;
        surfaceCacheMb = Long.MIN_VALUE;
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

    /**
     * The tank's lowest block (y, then z, then x; the controller position). Unlike {@link #min()} it is always a block of
     * the tank, also for a free shape whose bounding-box corner is empty.
     */
    public BlockPos anchor() {
        long best = 0;
        boolean any = false;
        for (long l : members) {
            if (!any || BlockPos.getY(l) < BlockPos.getY(best) || (BlockPos.getY(l) == BlockPos.getY(best) && (BlockPos.getZ(l) < BlockPos.getZ(best)
                    || (BlockPos.getZ(l) == BlockPos.getZ(best) && BlockPos.getX(l) < BlockPos.getX(best))))) {
                best = l;
                any = true;
            }
        }
        return any ? BlockPos.of(best) : min();
    }

    /** Blocks per layer, index 0 = {@link #min()} y. */
    public int[] layerCounts() {
        if (layerCounts == null) layerCounts = TankShape.layerCounts(members, minY, maxY);
        return layerCounts;
    }

    /** Absolute height of the water surface (M10: correct for any shape; bottom of the tank when dry). */
    public double waterSurfaceY() {
        int headroom = headroom();
        if (surfaceCacheMb != waterMb || surfaceCacheHeadroom != headroom) {
            surfaceCache = members.isEmpty() ? minY : TankShape.surfaceY(layerCounts(), minY, waterMb, headroom);
            surfaceCacheMb = waterMb;
            surfaceCacheHeadroom = headroom;
        }
        return surfaceCache;
    }

    /** Positions directly above the blocks whose top is open (no tank block above): lid, feeder, light, evaporation. */
    public java.util.List<BlockPos> openTops() {
        return TankShape.openTops(members);
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
        int[] c = layerCounts();
        int i = y - minY;
        if (i < 0 || i >= c.length) return 0;
        long below = 0;
        for (int k = 0; k < i; k++) below += c[k];
        return fillFor(waterMb, below * MB_PER_BLOCK, c[i] * MB_PER_BLOCK, i == c.length - 1 ? 16 - headroom() : 16);
    }

    /** Pure helper (unit-tested): fill in 1/16 of a layer with the given capacity whose bottom starts at {@code belowMb}. */
    public static int fillFor(long waterMb, long belowMb, long layerCapacityMb) {
        return fillFor(waterMb, belowMb, layerCapacityMb, 16);
    }

    /** Like {@link #fillFor(long, long, long)}, a full layer shows {@code maxFill} (top layer: 16 minus the headroom). */
    public static int fillFor(long waterMb, long belowMb, long layerCapacityMb, int maxFill) {
        if (layerCapacityMb <= 0) return 0;
        long inLayer = Math.max(0, Math.min(waterMb - belowMb, layerCapacityMb));
        if (inLayer <= 0) return 0;
        return (int) Math.min(maxFill, Math.max(1, (inLayer * maxFill + layerCapacityMb - 1) / layerCapacityMb));
    }

    /**
     * Air gap below the top glass of a full tank, in 1/16 block (server config {@code tank.waterHeadroom}): the top layer
     * of the tank shows its water squeezed into 16 - headroom sixteenths. Same amount of water, it only looks less full.
     */
    public static int headroom() {
        return org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.TANK_WATER_HEADROOM, 1);
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
    public float targetTemp() { return targetTemp; }
    public void setTargetTemp(float t) { this.targetTemp = TankTemperature.clampTarget(Math.round(t * 2) / 2.0F); }
    public float waterTemp() { return waterTemp; }
    public void setWaterTemp(float t) { this.waterTemp = t; }
    public String name() { return name; }
    public void setName(String n) { this.name = n.length() > MAX_NAME_LENGTH ? n.substring(0, MAX_NAME_LENGTH) : n; }
    public boolean signVisible() { return signVisible; }
    public void setSignVisible(boolean v) { this.signVisible = v; }
    public SignPosition signPos() { return signPos; }
    public void setSignPos(SignPosition p) { this.signPos = p; }
    public Direction front() { return front; }
    public void setFront(Direction d) { if (d.getAxis().isHorizontal()) this.front = d; }
    public boolean locked() { return locked; }
    public void setLocked(boolean l) { this.locked = l; }
    public Set<UUID> inhabitants() { return inhabitants; }
    public int inhabitantCount() { return clientView ? clientInhabitants : inhabitants.size(); }
    public List<LogEntry> log() { return log; }
    @Nullable public BubbleNest nest() { return nest; }
    public void setNest(@Nullable BubbleNest n) { this.nest = n; }
    /** M12: the floating plants of the surface (server: the simulation; client: the latest update). */
    public List<org.overengineer.bettacraft.tank.plants.FloatingPlant> plants() { return plants; }
    public int nextPlantId() { return nextPlantId++; }

    /** Whether the column of (x, z) belongs to this tank (any layer). */
    public boolean containsColumn(double x, double z) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        for (int y = minY; y <= maxY; y++) if (members.contains(BlockPos.asLong(bx, y, bz))) return true;
        return false;
    }

    public void addLog(long time, String key, String... args) {
        log.add(new LogEntry(time, key, List.of(args)));
        while (log.size() > MAX_LOG) log.removeFirst();
    }

    /**
     * Heaters left in the old tank menu slots (server side; created lazily so that pure unit tests never touch item
     * registries). They still heat, are moved into an attached external filter automatically and can be taken out.
     */
    public SimpleContainer legacyHeaters() {
        if (legacyHeaters == null) legacyHeaters = new SimpleContainer(LEGACY_HEATER_SLOTS);
        return legacyHeaters;
    }

    public boolean hasLegacyHeaters() {
        return legacyHeaters != null && !legacyHeaters.isEmpty();
    }

    /** Water blocks (may be fractional): 1000 mB = 1 block. */
    public double waterBlocks() {
        return waterMb / (double) MB_PER_BLOCK;
    }

    /** Copies name, sign, front, lock, temperatures, owner and log (not geometry, water, equipment or inhabitants). */
    public void copySettingsFrom(TankData o) {
        owner = o.owner;
        targetTemp = o.targetTemp;
        waterTemp = o.waterTemp;
        name = o.name;
        signVisible = o.signVisible;
        signPos = o.signPos;
        front = o.front;
        locked = o.locked;
        waterQuality = o.waterQuality;
        lastFreshWater = o.lastFreshWater;
        log.clear();
        log.addAll(o.log);
        // the nest belongs to the part of the tank that contains it (never duplicated on a split)
        nest = o.nest != null && contains(BlockPos.containing(o.nest.pos())) ? o.nest.copy() : null;
        // M12: the floating plants above this part stay (a merge adds the plants of the other tanks, see TankMultiblock)
        plants.clear();
        nextPlantId = Math.max(nextPlantId, o.nextPlantId);
        for (var p : o.plants) if (containsColumn(p.x, p.z)) plants.add(p.copy());
    }

    public float waterQuality() { return waterQuality; }
    public void setWaterQuality(float q) { this.waterQuality = WaterQuality.clamp(q); }
    public double evaporationCarry() { return evaporationCarry; }
    public void setEvaporationCarry(double c) { this.evaporationCarry = Math.max(0, Math.min(EVAPORATION_BATCH_MB, c)); }
    public long powerBuffer() { return powerBuffer; }
    public void setPowerBuffer(long fe) { this.powerBuffer = Math.max(0, fe); }
    public long lastFreshWater() { return lastFreshWater; }
    public void setLastFreshWater(long t) { this.lastFreshWater = t; }

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
        t.putDouble("evaporation_carry", evaporationCarry);
        t.put("zones", zones.copy());
        t.putLong("power", powerBuffer);
        t.putLong("last_fresh_water", lastFreshWater);
        t.putFloat("target_temp", targetTemp);
        t.putFloat("water_temp", waterTemp);
        ListTag hl = new ListTag();
        for (int i = 0; i < LEGACY_HEATER_SLOTS; i++) {
            ItemStack st = legacyHeaters == null ? ItemStack.EMPTY : legacyHeaters.getItem(i);
            String id = st.isEmpty() ? (unknownHeaters[i] == null ? "" : unknownHeaters[i]) : BuiltInRegistries.ITEM.getKey(st.getItem()).toString();
            hl.add(StringTag.valueOf(id));
        }
        t.put("legacy_heaters", hl);
        t.putString("name", name);
        t.putBoolean("sign_visible", signVisible);
        t.putString("sign_pos", signPos.getSerializedName());
        t.putString("front", front.getSerializedName());
        t.putBoolean("locked", locked);
        ListTag inh = new ListTag();
        for (UUID u : inhabitants) inh.add(NbtUtils.createUUID(u));
        t.put("inhabitants", inh);
        ListTag lg = new ListTag();
        for (LogEntry e : log) lg.add(e.save());
        t.put("log", lg);
        if (nest != null) t.put("nest", nest.save());
        ListTag pl = new ListTag();
        for (var p : plants) pl.add(p.save());
        t.put("plants", pl);
        t.putInt("next_plant_id", nextPlantId);
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
        d.waterQuality = t.contains("water_quality", Tag.TAG_ANY_NUMERIC) ? WaterQuality.clamp(t.getFloat("water_quality")) : 1.0F;
        d.setEvaporationCarry(t.getDouble("evaporation_carry"));
        d.zones = t.getCompound("zones").copy();
        d.powerBuffer = Math.max(0, t.getLong("power"));
        d.lastFreshWater = t.contains("last_fresh_water", Tag.TAG_ANY_NUMERIC) ? t.getLong("last_fresh_water") : -1L;
        d.targetTemp = t.contains("target_temp") ? TankTemperature.clampTarget(t.getFloat("target_temp")) : TankTemperature.DEFAULT_TARGET;
        d.waterTemp = t.contains("water_temp") ? t.getFloat("water_temp") : d.targetTemp;
        ListTag hl = t.getList("legacy_heaters", Tag.TAG_STRING);
        for (int i = 0; i < Math.min(LEGACY_HEATER_SLOTS, hl.size()); i++) {
            String id = hl.getString(i);
            if (id.isEmpty()) continue;
            ResourceLocation rl = ResourceLocation.tryParse(id);
            Item item = rl == null ? Items.AIR : BuiltInRegistries.ITEM.get(rl);
            if (item == Items.AIR) d.unknownHeaters[i] = id; // keep ids of removed mods
            else d.legacyHeaters().setItem(i, new ItemStack(item));
        }
        d.name = t.getString("name");
        d.signVisible = !t.contains("sign_visible") || t.getBoolean("sign_visible");
        d.signPos = SignPosition.byName(t.getString("sign_pos"));
        Direction f = Direction.byName(t.getString("front"));
        d.front = f != null && f.getAxis().isHorizontal() ? f : Direction.SOUTH;
        d.locked = t.getBoolean("locked");
        ListTag inh = t.getList("inhabitants", Tag.TAG_INT_ARRAY);
        for (Tag u : inh) d.inhabitants.add(NbtUtils.loadUUID(u));
        ListTag lg = t.getList("log", Tag.TAG_COMPOUND);
        for (int i = 0; i < lg.size(); i++) d.log.add(LogEntry.load(lg.getCompound(i)));
        d.nest = t.contains("nest", Tag.TAG_COMPOUND) ? BubbleNest.load(t.getCompound("nest")) : null;
        ListTag pl = t.getList("plants", Tag.TAG_COMPOUND);
        int maxId = 0;
        for (int i = 0; i < pl.size(); i++) {
            var p = org.overengineer.bettacraft.tank.plants.FloatingPlant.load(pl.getCompound(i));
            if (p == null) continue;
            d.plants.add(p);
            maxId = Math.max(maxId, p.id);
        }
        d.nextPlantId = Math.max(maxId + 1, t.contains("next_plant_id", Tag.TAG_ANY_NUMERIC) ? t.getInt("next_plant_id") : 1);
        d.recomputeBounds();
        return d;
    }

    /** Small client view: box, scale, region, water. */
    public CompoundTag saveClientView() {
        CompoundTag t = new CompoundTag();
        t.putIntArray("min", new int[]{minX, minY, minZ});
        t.putIntArray("max", new int[]{maxX, maxY, maxZ});
        t.putInt("count", members.size());
        // M10: non-cuboid shapes send their blocks as a bit set over the box (512 bytes for 16^3)
        if (!members.isEmpty() && !isCuboid()) t.putByteArray("shape", TankShape.encode(members, minX, minY, minZ, sizeX(), sizeY(), sizeZ()));
        t.putInt("scale", scale);
        t.putLong("water_mb", waterMb);
        t.putInt("region", regionId);
        t.putString("name", name);
        t.putBoolean("sign_visible", signVisible);
        t.putString("sign_pos", signPos.getSerializedName());
        t.putString("front", front.getSerializedName());
        t.putFloat("water_temp", waterTemp);
        t.putInt("inhabitants", inhabitants.size());
        if (nest != null) t.put("nest", nest.saveClientView());
        if (!plants.isEmpty()) t.putByteArray("plants", org.overengineer.bettacraft.tank.plants.FloatingPlantSim.encode(plants, minX, minZ));
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
        if (mn.length == 3 && mx.length == 3) {
            if (t.contains("shape", Tag.TAG_BYTE_ARRAY)) {
                d.members.addAll(TankShape.decode(t.getByteArray("shape"), d.minX, d.minY, d.minZ, d.sizeX(), d.sizeY(), d.sizeZ()));
            } else if ((long) d.sizeX() * d.sizeY() * d.sizeZ() <= 1 << 20) {
                for (int y = d.minY; y <= d.maxY; y++)
                    for (int z = d.minZ; z <= d.maxZ; z++)
                        for (int x = d.minX; x <= d.maxX; x++) d.members.add(BlockPos.asLong(x, y, z));
            }
        }
        d.scale = t.getInt("scale");
        d.waterMb = t.getLong("water_mb");
        d.regionId = t.getInt("region");
        d.name = t.getString("name");
        d.signVisible = t.getBoolean("sign_visible");
        d.signPos = SignPosition.byName(t.getString("sign_pos"));
        Direction f = Direction.byName(t.getString("front"));
        d.front = f != null && f.getAxis().isHorizontal() ? f : Direction.SOUTH;
        d.waterTemp = t.getFloat("water_temp");
        d.clientInhabitants = t.getInt("inhabitants");
        d.nest = t.contains("nest", Tag.TAG_COMPOUND) ? BubbleNest.loadClientView(t.getCompound("nest")) : null;
        if (t.contains("plants", Tag.TAG_BYTE_ARRAY)) d.plants.addAll(org.overengineer.bettacraft.tank.plants.FloatingPlantSim.decode(t.getByteArray("plants"), d.minX, d.minZ));
        return d;
    }

    @Override
    public String toString() {
        return "Tank[" + id + " " + sizeX() + "x" + sizeY() + "x" + sizeZ() + " at " + NbtUtils.writeBlockPos(min()) + ", "
                + waterMb + "/" + capacityMb() + " mB, scale " + scale + ", region " + regionId + "]";
    }
}
