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
    /** M14: shrimp, crayfish and snails (data animals, absolute positions), their clutches and moults, the food pool. */
    private final List<org.overengineer.bettacraft.critters.Critter> critters = new ArrayList<>();
    private final List<org.overengineer.bettacraft.critters.CritterObject> critterObjects = new ArrayList<>();
    private int nextCritterId = 1;
    private double critterFood;
    /** M14: algae on the inside of the side glass, per block four faces (south, west, north, east). */
    private final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<float[]> algae = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
    /** M19 (schema 9): eggs, larvae and boxed young of the tankmates, the breeding box, the last cool water change. */
    private final List<org.overengineer.bettacraft.tankmates.Clutch> clutches = new ArrayList<>();
    private int nextClutchId = 1;
    private boolean breedingBox;
    /** Water change bookkeeping: start of the current window, fresh water added in it, temperature before it. */
    private long freshWindowStart = -1L;
    private long freshWindowMb;
    private float freshWindowTemp = Float.NaN;
    private long lastCoolChange = -1L;
    /** M19: glass faces the tankmate grazers (otocinclus, bristlenose) eat per day (transient, set by the care step). */
    private double fishGrazing;
    /** M20: salt (ppt, sea water 35) and calcium (mg/L) of the water; 0 = fresh water (all tanks before M20). */
    private float salinity;
    private float calcium;

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
    /** M15: players in the interior right now (not saved; kept up to date by InteriorManager, sent to clients). */
    private int occupants;

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
    /** M15: players in the tank's interior (server: as last counted, client: as synced). */
    public int occupants() { return occupants; }
    public void setOccupants(int n) { this.occupants = n; }
    public List<LogEntry> log() { return log; }
    @Nullable public BubbleNest nest() { return nest; }
    public void setNest(@Nullable BubbleNest n) { this.nest = n; }
    /** M12: the floating plants of the surface (server: the simulation; client: the latest update). */
    public List<org.overengineer.bettacraft.tank.plants.FloatingPlant> plants() { return plants; }
    public int nextPlantId() { return nextPlantId++; }
    /** M14: the shrimp, crayfish and snails (server: the simulation; client: the latest update). */
    public List<org.overengineer.bettacraft.critters.Critter> critters() { return critters; }
    /** M14: apple snail clutches and crayfish moults. */
    public List<org.overengineer.bettacraft.critters.CritterObject> critterObjects() { return critterObjects; }
    public int nextCritterId() { return nextCritterId++; }
    public double critterFood() { return critterFood; }
    public void setCritterFood(double f) { this.critterFood = Math.max(0, Double.isFinite(f) ? f : 0); }
    /** M14: algae per block and side face (server: 0..1; client: stage / 4). */
    public it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<float[]> algae() { return algae; }

    public List<org.overengineer.bettacraft.tankmates.Clutch> clutches() { return clutches; }
    public int nextClutchId() { return nextClutchId++; }
    public boolean breedingBox() { return breedingBox; }
    public void setBreedingBox(boolean b) { this.breedingBox = b; }
    public long freshWindowStart() { return freshWindowStart; }
    public long freshWindowMb() { return freshWindowMb; }
    public float freshWindowTemp() { return freshWindowTemp; }
    public void setFreshWindow(long start, long mb, float temp) { freshWindowStart = start; freshWindowMb = mb; freshWindowTemp = temp; }
    public long lastCoolChange() { return lastCoolChange; }
    public void setLastCoolChange(long t) { this.lastCoolChange = t; }
    public double fishGrazing() { return fishGrazing; }
    public void setFishGrazing(double g) { this.fishGrazing = Double.isFinite(g) ? Math.max(0, g) : 0; }
    public float salinity() { return salinity; }
    public void setSalinity(float ppt) { this.salinity = Salinity.clampSalinity(ppt); }
    public float calcium() { return calcium; }
    public void setCalcium(float ca) { this.calcium = Salinity.clampCalcium(ca); }
    /** M20: whether this is a salt water tank (from {@link Salinity#MARINE_FROM} ppt). */
    public boolean marine() { return Salinity.marine(salinity); }

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
        // M14: the small animals, clutches and moults inside this part stay, so do the algae on its glass
        critters.clear();
        critterObjects.clear();
        nextCritterId = Math.max(nextCritterId, o.nextCritterId);
        for (var c : o.critters) if (contains(BlockPos.containing(c.x, c.y + 0.01, c.z))) critters.add(c.copy(c.id));
        for (var c : o.critterObjects) if (contains(BlockPos.containing(c.x, c.y, c.z))) critterObjects.add(c.copy(c.id));
        critterFood = o.critterFood * (o.memberCount() <= 0 ? 1 : Math.min(1.0, memberCount() / (double) o.memberCount()));
        algae.clear();
        for (var e : o.algae.long2ObjectEntrySet()) if (members.contains(e.getLongKey())) algae.put(e.getLongKey(), e.getValue().clone());
        // M19: the clutches inside this part stay; the breeding box and the water change history go with the settings
        clutches.clear();
        nextClutchId = Math.max(nextClutchId, o.nextClutchId);
        for (var c : o.clutches) if (contains(BlockPos.containing(c.x, c.y, c.z))) clutches.add(c.copy(c.id));
        // the breeding box hangs in the block of the old tank's anchor: the part that keeps that block keeps the box
        breedingBox = o.breedingBox && contains(o.anchor());
        lastCoolChange = o.lastCoolChange;
        salinity = o.salinity;
        calcium = o.calcium;
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
        ListTag cl = new ListTag();
        for (var c : critters) cl.add(c.save());
        t.put("critters", cl);
        ListTag ol = new ListTag();
        for (var o : critterObjects) ol.add(o.save());
        t.put("critter_objects", ol);
        t.putInt("next_critter_id", nextCritterId);
        t.putDouble("critter_food", critterFood);
        ListTag al = new ListTag();
        for (var e : algae.long2ObjectEntrySet()) {
            CompoundTag a = new CompoundTag();
            a.putLong("p", e.getLongKey());
            float[] v = e.getValue();
            a.putIntArray("a", new int[]{Math.round(v[0] * 1000), Math.round(v[1] * 1000), Math.round(v[2] * 1000), Math.round(v[3] * 1000)});
            al.add(a);
        }
        t.put("algae", al);
        // M19 (schema 9)
        ListTag tc = new ListTag();
        for (var c : clutches) tc.add(c.save());
        t.put("clutches", tc);
        t.putInt("next_clutch_id", nextClutchId);
        t.putBoolean("breeding_box", breedingBox);
        t.putLong("fresh_window_start", freshWindowStart);
        t.putLong("fresh_window_mb", freshWindowMb);
        if (!Float.isNaN(freshWindowTemp)) t.putFloat("fresh_window_temp", freshWindowTemp);
        t.putLong("last_cool_change", lastCoolChange);
        t.putFloat("salinity", salinity);
        t.putFloat("calcium", calcium);
        return t;
    }

    public static TankData load(CompoundTag t) {
        TankData d = new TankData();
        if (t.hasUUID("id")) d.id = t.getUUID("id");
        for (long l : t.getLongArray("members")) d.members.add(l);
        d.scale = t.getInt("scale");
        if (d.scale != 4 && d.scale != 8 && d.scale != 16) d.scale = 16; // M17: corrupt scale
        d.waterMb = Math.max(0, t.getLong("water_mb"));
        d.regionId = t.contains("region", Tag.TAG_ANY_NUMERIC) ? t.getInt("region") : -1;
        d.owner = t.hasUUID("owner") ? t.getUUID("owner") : null;
        d.waterQuality = t.contains("water_quality", Tag.TAG_ANY_NUMERIC) ? WaterQuality.clamp(t.getFloat("water_quality")) : 1.0F;
        d.setEvaporationCarry(t.getDouble("evaporation_carry"));
        d.zones = t.getCompound("zones").copy();
        d.powerBuffer = Math.max(0, t.getLong("power"));
        d.lastFreshWater = t.contains("last_fresh_water", Tag.TAG_ANY_NUMERIC) ? t.getLong("last_fresh_water") : -1L;
        d.targetTemp = t.contains("target_temp") ? TankTemperature.clampTarget(t.getFloat("target_temp")) : TankTemperature.DEFAULT_TARGET;
        d.waterTemp = t.contains("water_temp") && Float.isFinite(t.getFloat("water_temp")) ? t.getFloat("water_temp") : d.targetTemp;
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
        // M17: a malformed UUID threw here, vanilla then dropped the whole block entity and the tank was lost
        for (Tag u : inh) if (u instanceof net.minecraft.nbt.IntArrayTag ia && ia.size() == 4) d.inhabitants.add(NbtUtils.loadUUID(u));
        ListTag lg = t.getList("log", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, lg.size() - MAX_LOG); i < lg.size(); i++) d.log.add(LogEntry.load(lg.getCompound(i)));
        try {
            d.nest = t.contains("nest", Tag.TAG_COMPOUND) ? BubbleNest.load(t.getCompound("nest")) : null;
        } catch (RuntimeException e) { // M17: a broken nest must not take the tank with it
            org.slf4j.LoggerFactory.getLogger("bettacraft/tank").warn("Dropped an unreadable bubble nest of tank {}", d.id, e);
            d.nest = null;
        }
        ListTag pl = t.getList("plants", Tag.TAG_COMPOUND);
        int maxId = 0;
        for (int i = 0; i < pl.size(); i++) {
            var p = org.overengineer.bettacraft.tank.plants.FloatingPlant.load(pl.getCompound(i));
            if (p == null) continue;
            d.plants.add(p);
            maxId = Math.max(maxId, p.id);
        }
        d.nextPlantId = Math.max(maxId + 1, t.contains("next_plant_id", Tag.TAG_ANY_NUMERIC) ? t.getInt("next_plant_id") : 1);
        // M14 (schema 8): small animals, clutches, moults, food pool, algae
        int maxCritter = 0;
        ListTag cl = t.getList("critters", Tag.TAG_COMPOUND);
        for (int i = 0; i < cl.size(); i++) {
            var c = org.overengineer.bettacraft.critters.Critter.load(cl.getCompound(i));
            if (c == null) continue;
            d.critters.add(c);
            maxCritter = Math.max(maxCritter, c.id);
        }
        ListTag ol = t.getList("critter_objects", Tag.TAG_COMPOUND);
        for (int i = 0; i < ol.size(); i++) {
            var o = org.overengineer.bettacraft.critters.CritterObject.load(ol.getCompound(i));
            if (o == null) continue;
            d.critterObjects.add(o);
            maxCritter = Math.max(maxCritter, o.id);
        }
        d.nextCritterId = Math.max(maxCritter + 1, t.contains("next_critter_id", Tag.TAG_ANY_NUMERIC) ? t.getInt("next_critter_id") : 1);
        d.setCritterFood(t.getDouble("critter_food"));
        ListTag al = t.getList("algae", Tag.TAG_COMPOUND);
        for (int i = 0; i < al.size(); i++) {
            CompoundTag a = al.getCompound(i);
            int[] v = a.getIntArray("a");
            if (v.length != 4) continue;
            long p = a.getLong("p");
            if (!d.members.contains(p)) continue;
            float[] lv = new float[4];
            boolean any = false;
            for (int k = 0; k < 4; k++) {
                lv[k] = Math.max(0, Math.min(1, v[k] / 1000F));
                any |= lv[k] > 0;
            }
            if (any) d.algae.put(p, lv);
        }
        // M19 (schema 9): tankmate clutches, breeding box, water changes
        ListTag tc = t.getList("clutches", Tag.TAG_COMPOUND);
        int maxClutch = 0;
        for (int i = 0; i < tc.size(); i++) {
            var c = org.overengineer.bettacraft.tankmates.Clutch.load(tc.getCompound(i));
            if (c == null) continue;
            d.clutches.add(c);
            maxClutch = Math.max(maxClutch, c.id);
        }
        d.nextClutchId = Math.max(maxClutch + 1, t.contains("next_clutch_id", Tag.TAG_ANY_NUMERIC) ? t.getInt("next_clutch_id") : 1);
        d.breedingBox = t.getBoolean("breeding_box");
        d.freshWindowStart = t.contains("fresh_window_start", Tag.TAG_ANY_NUMERIC) ? t.getLong("fresh_window_start") : -1L;
        d.freshWindowMb = Math.max(0, t.getLong("fresh_window_mb"));
        d.freshWindowTemp = t.contains("fresh_window_temp", Tag.TAG_ANY_NUMERIC) ? t.getFloat("fresh_window_temp") : Float.NaN;
        d.lastCoolChange = t.contains("last_cool_change", Tag.TAG_ANY_NUMERIC) ? t.getLong("last_cool_change") : -1L;
        d.salinity = Salinity.clampSalinity(t.getFloat("salinity"));
        d.calcium = Salinity.clampCalcium(t.getFloat("calcium"));
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
        if (occupants > 0) t.putInt("occupants", occupants);
        if (nest != null) t.put("nest", nest.saveClientView());
        if (!plants.isEmpty()) t.putByteArray("plants", org.overengineer.bettacraft.tank.plants.FloatingPlantSim.encode(plants, minX, minZ));
        if (!critters.isEmpty() || !critterObjects.isEmpty()) t.putByteArray("critters",
                org.overengineer.bettacraft.critters.CritterSync.encode(critters, critterObjects, minX, minY, minZ, 0L));
        if (!algae.isEmpty()) t.putByteArray("algae", org.overengineer.bettacraft.tank.algae.AlgaeSim.encode(algae, minX, minY, minZ));
        if (!clutches.isEmpty()) {
            ListTag tc = new ListTag();
            for (var c : clutches) tc.add(c.saveClientView());
            t.put("clutches", tc);
        }
        if (breedingBox) t.putBoolean("breeding_box", true);
        if (salinity > 0) t.putFloat("salinity", salinity);
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
        d.occupants = t.getInt("occupants");
        d.nest = t.contains("nest", Tag.TAG_COMPOUND) ? BubbleNest.loadClientView(t.getCompound("nest")) : null;
        if (t.contains("plants", Tag.TAG_BYTE_ARRAY)) d.plants.addAll(org.overengineer.bettacraft.tank.plants.FloatingPlantSim.decode(t.getByteArray("plants"), d.minX, d.minZ));
        if (t.contains("critters", Tag.TAG_BYTE_ARRAY)) {
            var dec = org.overengineer.bettacraft.critters.CritterSync.decode(t.getByteArray("critters"), d.minX, d.minY, d.minZ);
            d.critters.addAll(dec.critters());
            d.critterObjects.addAll(dec.objects());
        }
        if (t.contains("algae", Tag.TAG_BYTE_ARRAY)) d.algae.putAll(org.overengineer.bettacraft.tank.algae.AlgaeSim.decode(t.getByteArray("algae"), d.minX, d.minY, d.minZ));
        ListTag tc = t.getList("clutches", Tag.TAG_COMPOUND);
        for (int i = 0; i < tc.size(); i++) {
            var c = org.overengineer.bettacraft.tankmates.Clutch.loadClientView(tc.getCompound(i));
            if (c != null) d.clutches.add(c);
        }
        d.breedingBox = t.getBoolean("breeding_box");
        d.salinity = Salinity.clampSalinity(t.getFloat("salinity"));
        return d;
    }

    @Override
    public String toString() {
        return "Tank[" + id + " " + sizeX() + "x" + sizeY() + "x" + sizeZ() + " at " + NbtUtils.writeBlockPos(min()) + ", "
                + waterMb + "/" + capacityMb() + " mB, scale " + scale + ", region " + regionId + "]";
    }
}
