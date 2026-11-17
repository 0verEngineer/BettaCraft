package org.overengineer.bettacraft.tank;

import org.overengineer.bettacraft.util.NbtCompat;
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
import net.minecraft.resources.Identifier;
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
    /** M21.5: break protection: nobody can break the tank or the equipment on and under it (off for new tanks). */
    private boolean breakProtected;
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
    /** M23: dissolved oxygen in mg/L (NaN = not known yet: the first care step starts at saturation). */
    private float oxygen = Float.NaN;
    /** M24: nitrate (dissolved waste) in mg/L; 0 for every tank of an earlier version. */
    private float nitrate;
    /**
     * M25: water chemistry ({@link org.overengineer.bettacraft.chem.TankChemistry}): TDS (ppm), pH, the tannins of almond
     * leaves (0..1) and dissolved CO2 (mg/L, dosed by the CO2 devices). Tanks of earlier versions start with tap water.
     */
    private float tds = org.overengineer.bettacraft.chem.WaterChemistry.TAP_TDS, ph = org.overengineer.bettacraft.chem.WaterChemistry.TAP_PH;
    private float tannin, co2 = org.overengineer.bettacraft.chem.WaterChemistry.CO2_AIR;
    /**
     * M25 part c ("Balling light"): alkalinity (dKH), magnesium (mg/L) and trace elements (0..1) of the water. They follow
     * the salt (sea water 8 dKH, 1300 mg/L, 0.5; {@link #setSalinity} moves them along), stony corals use them up, the
     * dosing pump adds them. Tanks of earlier versions get the values of their salinity.
     */
    private float kh = org.overengineer.bettacraft.chem.WaterChemistry.TAP_KH, magnesium = org.overengineer.bettacraft.chem.WaterChemistry.TAP_MG, trace;
    /** M24: game time of the last water change (bucket, pipe or controller; -1 = none yet) and the share changed then. */
    private long lastWaterChange = -1L;
    /** M37: liquid fertilizer in the water (1 = the right level, 100 %; from {@code fertilizer.overdoseAt} overdosed). */
    private float fertilizer;
    /** M23: light settings of the tank: dimmer 0..100 %, daylight timer with on/off time (minutes of the day, 6:00 = 360). */
    private int lightDimmer = 100;
    private boolean lightTimer;
    private int lightOn = 8 * 60, lightOff = 20 * 60;
    /**
     * M31: terrarium and paludarium ({@link org.overengineer.bettacraft.terrarium.TankClimate}): the mode worked out from
     * the water and the land, the land share of the floor and the climate (NaN while the tank is an aquarium or the
     * feature is off). Saved under "terrarium" only once a tank was a terrarium or paludarium; old tanks have none.
     */
    private org.overengineer.bettacraft.terrarium.TerrariumMode terrariumMode = org.overengineer.bettacraft.terrarium.TerrariumMode.AQUARIUM;
    /** M33.1: the tank type chosen in the menu (automatic for new and old tanks). */
    private org.overengineer.bettacraft.terrarium.TankModeSetting tankMode = org.overengineer.bettacraft.terrarium.TankModeSetting.AUTO;
    private float landShare = -1F, airTemp = Float.NaN, groundTemp = Float.NaN, baskTemp = Float.NaN, humidity = Float.NaN, uv = 0F, matDuty = 0F;
    /** M31 part b: turtle eggs buried in the land (saved under "turtle_eggs" only while there are some). */
    private final List<org.overengineer.bettacraft.terrarium.HerpClutch> herpClutches = new ArrayList<>();
    private int nextHerpClutchId = 1;
    /**
     * M47 part c: frog spawn at the surface and the tadpoles of the dwarf clawed frogs (saved under "frog_spawn" and
     * "tadpoles" only while there are some; older worlds have none), and the fine food in the water for the tadpoles.
     */
    private final List<org.overengineer.bettacraft.entity.frog.FrogSpawn> frogSpawn = new ArrayList<>();
    private final List<org.overengineer.bettacraft.entity.frog.Tadpole> tadpoles = new ArrayList<>();
    private int nextFrogBroodId = 1;
    private double tadpoleFood;
    /**
     * M48 part b: the plankton of a reef tank in food portions: the copepods of the live rock (the mandarin's food), the
     * rotifers, the phytoplankton and the artemia nauplii in the water (the clownfish larvae's food chain). Saved under
     * "plankton" only while there is some; older worlds have none.
     */
    private double copepods, rotifers, phytoplankton, nauplii;
    /** M49: flies for the archerfish (in the plankton compound) and the mangroves of the interior. */
    private double flies;
    private final List<org.overengineer.bettacraft.plants.Mangroves.Tree> mangroves = new ArrayList<>();
    /**
     * M51: which hired aquarists may work at this tank ({@link org.overengineer.bettacraft.aquarist.AquaristAccess}: the
     * owner's and his team's only, also those of other players, none) and when an aquarist last did each of his tasks here
     * (game times by task id; shared by every aquarist, so two never do the same twice). Saved under "aquarist" only once
     * set; older worlds have none.
     */
    private org.overengineer.bettacraft.aquarist.AquaristAccess aquaristAccess = org.overengineer.bettacraft.aquarist.AquaristAccess.OWNER;
    private final CompoundTag aquaristWork = new CompoundTag();

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
            ListTag a = t.getListOrEmpty("a");
            for (int i = 0; i < a.size(); i++) args.add(a.getStringOr(i, ""));
            return new LogEntry(t.getLongOr("t", 0L), t.getStringOr("k", ""), List.copyOf(args));
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
    public boolean breakProtected() { return breakProtected; }
    public void setBreakProtected(boolean b) { this.breakProtected = b; }
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
    // M23
    public float oxygen() { return oxygen; }
    // M24
    public float nitrate() { return nitrate; }
    public void setNitrate(float mgl) { this.nitrate = Float.isFinite(mgl) ? Math.max(0F, Math.min(1000F, mgl)) : 0F; }
    public long lastWaterChange() { return lastWaterChange; }
    // M25
    public float tds() { return tds; }
    public void setTds(float ppm) { this.tds = org.overengineer.bettacraft.chem.WaterChemistry.clampTds(ppm); }
    public float ph() { return ph; }
    public void setPh(float v) { this.ph = org.overengineer.bettacraft.chem.WaterChemistry.clampPh(v); }
    public float tannin() { return tannin; }
    public void setTannin(float v) { this.tannin = Float.isFinite(v) ? Math.max(0F, Math.min(1F, v)) : 0F; }
    public float co2() { return co2; }
    public void setCo2(float mgl) {
        this.co2 = Float.isFinite(mgl) ? Math.max(0F, Math.min(org.overengineer.bettacraft.chem.WaterChemistry.CO2_MAX, mgl))
                : org.overengineer.bettacraft.chem.WaterChemistry.CO2_AIR;
    }
    public void setLastWaterChange(long t) { this.lastWaterChange = t; }
    public void setOxygen(float mgl) { this.oxygen = Float.isNaN(mgl) ? Float.NaN : Math.max(0F, Math.min(25F, mgl)); }
    public int lightDimmer() { return lightDimmer; }
    public void setLightDimmer(int pct) { this.lightDimmer = Math.max(0, Math.min(100, pct)); }
    public boolean lightTimer() { return lightTimer; }
    public void setLightTimer(boolean on) { this.lightTimer = on; }
    public int lightOn() { return lightOn; }
    public int lightOff() { return lightOff; }
    public void setLightOn(int minutes) { this.lightOn = Math.floorMod(minutes, 24 * 60); }
    public void setLightOff(int minutes) { this.lightOff = Math.floorMod(minutes, 24 * 60); }

    // M47 part c: frog breeding
    public List<org.overengineer.bettacraft.entity.frog.FrogSpawn> frogSpawn() { return frogSpawn; }
    public List<org.overengineer.bettacraft.entity.frog.Tadpole> tadpoles() { return tadpoles; }
    public int nextFrogBroodId() { return nextFrogBroodId++; }
    public double tadpoleFood() { return tadpoleFood; }
    public void setTadpoleFood(double f) { this.tadpoleFood = Double.isFinite(f) ? Math.max(0, f) : 0; }
    // M48 part b: plankton
    public double copepods() { return copepods; }
    public void setCopepods(double v) { this.copepods = clean(v); }
    public double rotifers() { return rotifers; }
    public void setRotifers(double v) { this.rotifers = clean(v); }
    public double phytoplankton() { return phytoplankton; }
    /** M49: insects above the water that an archerfish shoots down (fruit flies let out over the tank, insects at the plants). */
    public double flies() { return flies; }
    public void setFlies(double v) { this.flies = Math.min(64, clean(v)); }
    /** M49: the mangroves growing in the interior ({@link org.overengineer.bettacraft.plants.Mangroves}). */
    public List<org.overengineer.bettacraft.plants.Mangroves.Tree> mangroves() { return mangroves; }
    /** M51: who may let his hired aquarist work here. */
    public org.overengineer.bettacraft.aquarist.AquaristAccess aquaristAccess() { return aquaristAccess; }
    public void setAquaristAccess(org.overengineer.bettacraft.aquarist.AquaristAccess a) { this.aquaristAccess = a == null ? org.overengineer.bettacraft.aquarist.AquaristAccess.OWNER : a; }
    /** M51: game time an aquarist last did task {@code task} here (Long.MIN_VALUE: never). */
    public long aquaristDone(String task) { return aquaristWork.contains(task) ? aquaristWork.getLongOr(task, 0L) : Long.MIN_VALUE; }
    public void setAquaristDone(String task, long time) { aquaristWork.putLong(task, time); }
    public void setPhytoplankton(double v) { this.phytoplankton = clean(v); }
    public double nauplii() { return nauplii; }
    public void setNauplii(double v) { this.nauplii = clean(v); }

    private static double clean(double v) {
        return Double.isFinite(v) && v > 1e-6 ? v : 0;
    }

    // M31 terrarium
    public List<org.overengineer.bettacraft.terrarium.HerpClutch> herpClutches() { return herpClutches; }
    public int nextHerpClutchId() { return nextHerpClutchId++; }
    public org.overengineer.bettacraft.terrarium.TerrariumMode terrariumMode() { return terrariumMode; }
    public void setTerrariumMode(org.overengineer.bettacraft.terrarium.TerrariumMode m) { this.terrariumMode = m == null ? org.overengineer.bettacraft.terrarium.TerrariumMode.AQUARIUM : m; }
    /** M33.1: the tank type set in the menu ({@link org.overengineer.bettacraft.terrarium.TankModeSetting#AUTO}: detected). */
    public org.overengineer.bettacraft.terrarium.TankModeSetting tankMode() { return tankMode; }
    public void setTankMode(org.overengineer.bettacraft.terrarium.TankModeSetting m) { this.tankMode = m == null ? org.overengineer.bettacraft.terrarium.TankModeSetting.AUTO : m; }
    /** Share of the floor that is land (-1 = not worked out yet). */
    public float landShare() { return landShare; }
    public void setLandShare(float s) { this.landShare = Float.isFinite(s) ? Math.max(-1F, Math.min(1F, s)) : -1F; }
    public float airTemp() { return airTemp; }
    public float groundTemp() { return groundTemp; }
    public float baskTemp() { return baskTemp; }
    public float humidity() { return humidity; }
    public float uv() { return uv; }
    public float matDuty() { return matDuty; }
    /** Whether the tank has a climate now (a terrarium or paludarium with the feature on). */
    public boolean hasClimate() { return Float.isFinite(airTemp) && Float.isFinite(humidity); }

    @Nullable
    public org.overengineer.bettacraft.terrarium.Climate.State climate() {
        return hasClimate() ? new org.overengineer.bettacraft.terrarium.Climate.State(airTemp, groundTemp, baskTemp, humidity, uv, matDuty) : null;
    }

    public void setClimate(@Nullable org.overengineer.bettacraft.terrarium.Climate.State c) {
        if (c == null || !c.valid()) {
            airTemp = groundTemp = baskTemp = humidity = Float.NaN;
            uv = matDuty = 0F;
            return;
        }
        airTemp = Math.max(-40F, Math.min(80F, c.air()));
        groundTemp = Float.isFinite(c.ground()) ? Math.max(-40F, Math.min(90F, c.ground())) : airTemp;
        baskTemp = Float.isFinite(c.bask()) ? Math.max(-40F, Math.min(90F, c.bask())) : airTemp;
        humidity = Math.max(0F, Math.min(100F, c.humidity()));
        uv = Float.isFinite(c.uv()) ? Math.max(0F, Math.min(20F, c.uv())) : 0F;
        matDuty = Float.isFinite(c.matDuty()) ? Math.max(0F, Math.min(1F, c.matDuty())) : 0F;
    }

    private CompoundTag saveTerrarium() {
        CompoundTag c = new CompoundTag();
        c.putByte("mode", (byte) terrariumMode.ordinal());
        c.putFloat("land", landShare);
        if (tankMode != org.overengineer.bettacraft.terrarium.TankModeSetting.AUTO) c.putString("type", tankMode.id()); // M33.1
        if (hasClimate()) {
            c.putFloat("air", airTemp);
            c.putFloat("ground", groundTemp);
            c.putFloat("bask", baskTemp);
            c.putFloat("rh", humidity);
            c.putFloat("uv", uv);
            c.putFloat("duty", matDuty);
        }
        return c;
    }

    private void loadTerrarium(CompoundTag c) {
        terrariumMode = org.overengineer.bettacraft.terrarium.TerrariumMode.byOrdinal(c.getByteOr("mode", (byte) 0));
        setLandShare(NbtCompat.has(c, "land", NbtCompat.ANY_NUMERIC) ? c.getFloatOr("land", 0f) : -1F);
        tankMode = org.overengineer.bettacraft.terrarium.TankModeSetting.byId(c.getStringOr("type", "")); // M33.1: absent = automatic
        if (NbtCompat.has(c, "air", NbtCompat.ANY_NUMERIC) && NbtCompat.has(c, "rh", NbtCompat.ANY_NUMERIC)) {
            setClimate(new org.overengineer.bettacraft.terrarium.Climate.State(c.getFloatOr("air", 0f), c.getFloatOr("ground", 0f), c.getFloatOr("bask", 0f),
                    c.getFloatOr("rh", 0f), c.getFloatOr("uv", 0f), c.getFloatOr("duty", 0f)));
        } else {
            setClimate(null);
        }
    }

    private boolean terrariumWorthSaving() {
        return terrariumMode != org.overengineer.bettacraft.terrarium.TerrariumMode.AQUARIUM || hasClimate()
                || tankMode != org.overengineer.bettacraft.terrarium.TankModeSetting.AUTO;
    }

    public float salinity() { return salinity; }
    public void setSalinity(float ppt) {
        float before = salinity;
        this.salinity = Salinity.clampSalinity(ppt);
        // M25 part c: salt brings the alkalinity, magnesium and trace elements of sea water (and taking it away the reverse)
        if (salinity != before) {
            kh = org.overengineer.bettacraft.chem.WaterChemistry.clampKh(kh + org.overengineer.bettacraft.chem.WaterChemistry.khOf(salinity)
                    - org.overengineer.bettacraft.chem.WaterChemistry.khOf(before));
            magnesium = org.overengineer.bettacraft.chem.WaterChemistry.clampMagnesium(magnesium + org.overengineer.bettacraft.chem.WaterChemistry.magnesiumOf(salinity)
                    - org.overengineer.bettacraft.chem.WaterChemistry.magnesiumOf(before));
            trace = org.overengineer.bettacraft.chem.WaterChemistry.clampTrace(trace + org.overengineer.bettacraft.chem.WaterChemistry.traceOf(salinity)
                    - org.overengineer.bettacraft.chem.WaterChemistry.traceOf(before));
        }
    }
    /** M25 part c: alkalinity in dKH. */
    public float kh() { return kh; }
    public void setKh(float v) { this.kh = org.overengineer.bettacraft.chem.WaterChemistry.clampKh(v); }
    /** M25 part c: magnesium in mg/L. */
    public float magnesium() { return magnesium; }
    public void setMagnesium(float v) { this.magnesium = org.overengineer.bettacraft.chem.WaterChemistry.clampMagnesium(v); }
    /** M25 part c: trace elements 0..1 (from 0.4 the corals show their stronger colours). */
    public float trace() { return trace; }
    public void setTrace(float v) { this.trace = org.overengineer.bettacraft.chem.WaterChemistry.clampTrace(v); }
    /** M37: the fertilizer level (0 .. {@link #FERTILIZER_MAX}, 1 = 100 %). */
    public float fertilizer() { return fertilizer; }
    public void setFertilizer(float v) { this.fertilizer = Float.isFinite(v) ? Math.max(0F, Math.min(FERTILIZER_MAX, v)) : 0F; }
    public static final float FERTILIZER_MAX = 20F;
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

    /** M49: water blocks of the sumps in the cabinets below (set by every care step, not saved; 0 without water in the tank). */
    private double sumpBlocks;

    public double sumpBlocks() {
        return sumpBlocks;
    }

    public void setSumpBlocks(double blocks) {
        sumpBlocks = Double.isFinite(blocks) ? Math.max(0, blocks) : 0;
    }

    /**
     * M49: the water of the whole system: the tank and its sumps (the waste, the nitrate and the dosed values spread over it, and
     * it counts for the stocking). The same as {@link #waterBlocks()} without a sump or water.
     */
    public double systemBlocks() {
        return waterMb <= 0 ? 0 : waterBlocks() + sumpBlocks;
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
        breakProtected = o.breakProtected;
        aquaristAccess = o.aquaristAccess; // M51
        for (String k : o.aquaristWork.keySet()) aquaristWork.putLong(k, o.aquaristWork.getLongOr(k, 0L));
        tankMode = o.tankMode; // M33.1: the parts of a split tank and a merged tank keep the type
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
        oxygen = o.oxygen; // M23
        nitrate = o.nitrate; // M24
        tds = o.tds; // M25
        ph = o.ph;
        tannin = o.tannin;
        co2 = o.co2;
        kh = o.kh; // M25 part c
        magnesium = o.magnesium;
        trace = o.trace;
        fertilizer = o.fertilizer; // M37
        lastWaterChange = o.lastWaterChange;
        lightDimmer = o.lightDimmer;
        lightTimer = o.lightTimer;
        lightOn = o.lightOn;
        lightOff = o.lightOff;
        setClimate(o.climate()); // M31 (the mode and the land share are worked out again)
        // M31 part b: the turtle eggs inside this part stay
        herpClutches.clear();
        nextHerpClutchId = Math.max(nextHerpClutchId, o.nextHerpClutchId);
        for (var c : o.herpClutches) if (contains(BlockPos.containing(c.x, c.y + 0.01, c.z))) herpClutches.add(c.copy());
        // M47 part c: the frog spawn and the tadpoles inside this part stay
        frogSpawn.clear();
        tadpoles.clear();
        nextFrogBroodId = Math.max(nextFrogBroodId, o.nextFrogBroodId);
        for (var s : o.frogSpawn) if (containsColumn(s.x, s.z)) frogSpawn.add(s.copy());
        for (var k : o.tadpoles) if (contains(BlockPos.containing(k.x, k.y, k.z))) tadpoles.add(k.copy());
        tadpoleFood = o.tadpoleFood;
        copepods = o.copepods; // M48 part b
        rotifers = o.rotifers;
        phytoplankton = o.phytoplankton;
        nauplii = o.nauplii;
        flies = o.flies; // M49
        mangroves.clear();
        for (var m : o.mangroves) mangroves.add(m.copy());
    }

    /**
     * M32 part b: everything with a world position goes along with a rigid move of the tank (Create contraption, Sable
     * sub-level): the blocks, the nest, floating plants, small animals and their clutches and moults, algae, the clutches
     * of the tankmates, turtle eggs ({@code interior} maps their egg blocks when the interior was turned too), the front.
     * A tank that lies on its side afterwards (not {@link TankMoveMap#upright()}) keeps its small animals on the floor
     * of the new bottom; floating plants that would be outside the shape drift to the anchor's column. Works on the
     * server data and on a client view.
     */
    public void relocate(TankMoveMap map, @Nullable java.util.function.LongUnaryOperator interior) {
        boolean upright = map.upright();
        LongSet moved = new LongOpenHashSet();
        for (long l : members) moved.add(map.block(l));
        members.clear();
        members.addAll(moved);
        recomputeBounds();
        BlockPos a = anchor();
        if (nest != null) {
            if (upright) nest.pos = map.point(nest.pos);
            else nest = null; // the bubbles do not survive the tank lying on its side (a nest with eggs is never moved)
        }
        for (var p : plants) {
            net.minecraft.world.phys.Vec3 v = map.point(p.x, minY + 0.5, p.z), pv = map.point(p.prevX, minY + 0.5, p.prevZ);
            p.x = v.x;
            p.z = v.z;
            p.prevX = pv.x;
            p.prevZ = pv.z;
            if (!containsColumn(p.x, p.z)) {
                p.x = p.prevX = a.getX() + 0.5;
                p.z = p.prevZ = a.getZ() + 0.5;
            }
        }
        for (var c : critters) {
            net.minecraft.world.phys.Vec3 v = map.point(c.x, c.y, c.z), t = map.point(c.tx, c.ty, c.tz), pp = map.point(c.px, c.py, c.pz);
            if (upright && contains(BlockPos.containing(v.x, v.y + 0.01, v.z))) {
                c.x = v.x; c.y = v.y; c.z = v.z;
                c.tx = t.x; c.ty = t.y; c.tz = t.z;
                c.px = pp.x; c.py = pp.y; c.pz = pp.z;
                c.yaw = map.yaw(c.yaw);
                c.pyaw = map.yaw(c.pyaw);
                c.face = turnFace(map, c.face);
            } else {
                c.x = c.tx = c.px = a.getX() + 0.5;
                c.y = c.ty = c.py = a.getY() + 0.05;
                c.z = c.tz = c.pz = a.getZ() + 0.5;
                c.face = org.overengineer.bettacraft.critters.Critter.FLOOR;
            }
        }
        for (var o : critterObjects) {
            net.minecraft.world.phys.Vec3 v = map.point(o.x, o.y, o.z);
            if (upright && contains(BlockPos.containing(v.x, v.y, v.z))) {
                o.x = v.x; o.y = v.y; o.z = v.z;
                o.yaw = map.yaw(o.yaw);
                o.face = turnFace(map, o.face);
            } else {
                o.x = a.getX() + 0.5;
                o.y = a.getY() + 0.05;
                o.z = a.getZ() + 0.5;
                o.face = org.overengineer.bettacraft.critters.Critter.FLOOR;
            }
        }
        it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<float[]> turned = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
        for (var e : algae.long2ObjectEntrySet()) {
            float[] v = e.getValue(), n = v.clone();
            if (upright) for (int d = 0; d < 4; d++) n[map.direction(Direction.from2DDataValue(d)).get2DDataValue()] = v[d];
            turned.put(map.block(e.getLongKey()), n);
        }
        algae.clear();
        algae.putAll(turned);
        for (var c : clutches) {
            net.minecraft.world.phys.Vec3 v = map.point(c.x, c.y, c.z);
            c.x = v.x; c.y = v.y; c.z = v.z;
            if (c.face >= 0) c.face = upright ? map.direction(Direction.from2DDataValue(c.face)).get2DDataValue() : -1;
        }
        for (var c : herpClutches) {
            net.minecraft.world.phys.Vec3 v = map.point(c.x, c.y, c.z);
            c.x = v.x; c.y = v.y; c.z = v.z;
            if (interior != null) c.interior = interior.applyAsLong(c.interior);
        }
        // M47 part c: the frog spawn floats with the surface (lying on its side the tank loses it), the tadpoles go along
        if (!upright) frogSpawn.clear();
        for (var s : frogSpawn) {
            net.minecraft.world.phys.Vec3 v = map.point(s.x, s.y, s.z);
            s.x = v.x; s.y = v.y; s.z = v.z;
        }
        for (var k : tadpoles) {
            net.minecraft.world.phys.Vec3 v = map.point(k.x, k.y, k.z);
            if (contains(BlockPos.containing(v.x, v.y, v.z))) {
                k.x = v.x; k.y = v.y; k.z = v.z;
            } else {
                k.x = a.getX() + 0.5; k.y = a.getY() + 0.5; k.z = a.getZ() + 0.5;
            }
        }
        if (upright) front = map.direction(front);
        layerCounts = null;
        surfaceCacheMb = Long.MIN_VALUE;
    }

    /** A glass face of a small animal (1 + 2D data value) turned with the tank; the floor and swimming stay. */
    private static byte turnFace(TankMoveMap map, byte face) {
        if (face < org.overengineer.bettacraft.critters.Critter.GLASS_SOUTH || face > org.overengineer.bettacraft.critters.Critter.GLASS_EAST) return face;
        return (byte) (1 + map.direction(Direction.from2DDataValue(face - 1)).get2DDataValue());
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
        NbtCompat.putUUID(t, "id", id);
        t.putLongArray("members", members.toLongArray());
        t.putInt("scale", scale);
        t.putLong("water_mb", waterMb);
        t.putInt("region", regionId);
        if (owner != null) NbtCompat.putUUID(t, "owner", owner);
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
        if (breakProtected) t.putBoolean("break_protected", true);
        ListTag inh = new ListTag();
        for (UUID u : inhabitants) inh.add(NbtCompat.createUUID(u));
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
        // M23 (no schema step: absent = unknown oxygen and the light settings of earlier versions)
        if (!Float.isNaN(oxygen)) t.putFloat("oxygen", oxygen);
        // M24 (no schema step: absent = nitrate 0, no water change yet)
        t.putFloat("nitrate", nitrate);
        t.putLong("last_water_change", lastWaterChange);
        // M25 (no schema step: absent = tap water)
        t.putFloat("tds", tds);
        t.putFloat("ph", ph);
        if (tannin > 0) t.putFloat("tannin", tannin);
        t.putFloat("co2", co2);
        t.putFloat("kh", kh); // M25 part c (absent = the values of the salinity)
        t.putFloat("magnesium", magnesium);
        t.putFloat("trace", trace);
        t.putFloat("fertilizer", fertilizer); // M37 (schema 11)
        t.putInt("light_dimmer", lightDimmer);
        t.putBoolean("light_timer", lightTimer);
        t.putInt("light_on", lightOn);
        t.putInt("light_off", lightOff);
        if (terrariumWorthSaving()) t.put("terrarium", saveTerrarium()); // M31
        if (!herpClutches.isEmpty()) { // M31 part b
            ListTag te = new ListTag();
            for (var c : herpClutches) te.add(c.save());
            t.put("turtle_eggs", te);
            t.putInt("next_turtle_clutch", nextHerpClutchId);
        }
        if (!frogSpawn.isEmpty() || !tadpoles.isEmpty() || tadpoleFood > 0) { // M47 part c
            ListTag fs = new ListTag();
            for (var s : frogSpawn) fs.add(s.save());
            t.put("frog_spawn", fs);
            ListTag tp = new ListTag();
            for (var k : tadpoles) tp.add(k.save());
            t.put("tadpoles", tp);
            t.putInt("next_frog_brood", nextFrogBroodId);
            if (tadpoleFood > 0) t.putDouble("tadpole_food", tadpoleFood);
        }
        if (copepods > 0 || rotifers > 0 || phytoplankton > 0 || nauplii > 0 || flies > 0) { // M48 part b
            CompoundTag p = new CompoundTag();
            if (flies > 0) p.putDouble("flies", flies); // M49
            if (copepods > 0) p.putDouble("copepods", copepods);
            if (rotifers > 0) p.putDouble("rotifers", rotifers);
            if (phytoplankton > 0) p.putDouble("phytoplankton", phytoplankton);
            if (nauplii > 0) p.putDouble("nauplii", nauplii);
            t.put("plankton", p);
        }
        if (!mangroves.isEmpty()) { // M49
            ListTag ml = new ListTag();
            for (var m : mangroves) ml.add(m.save());
            t.put("mangroves", ml);
        }
        if (aquaristAccess != org.overengineer.bettacraft.aquarist.AquaristAccess.OWNER || !aquaristWork.isEmpty()) { // M51
            CompoundTag a = new CompoundTag();
            if (aquaristAccess != org.overengineer.bettacraft.aquarist.AquaristAccess.OWNER) a.putString("access", aquaristAccess.id());
            if (!aquaristWork.isEmpty()) a.put("done", aquaristWork.copy());
            t.put("aquarist", a);
        }
        return t;
    }

    public static TankData load(CompoundTag t) {
        TankData d = new TankData();
        if (NbtCompat.hasUUID(t, "id")) d.id = NbtCompat.getUUID(t, "id");
        for (long l : t.getLongArray("members").orElse(new long[0])) d.members.add(l);
        d.scale = t.getIntOr("scale", 0);
        if (d.scale != 4 && d.scale != 8 && d.scale != 16) d.scale = 16; // M17: corrupt scale
        d.waterMb = Math.max(0, t.getLongOr("water_mb", 0L));
        d.regionId = NbtCompat.has(t, "region", NbtCompat.ANY_NUMERIC) ? t.getIntOr("region", 0) : -1;
        d.owner = NbtCompat.hasUUID(t, "owner") ? NbtCompat.getUUID(t, "owner") : null;
        d.waterQuality = NbtCompat.has(t, "water_quality", NbtCompat.ANY_NUMERIC) ? WaterQuality.clamp(t.getFloatOr("water_quality", 0f)) : 1.0F;
        d.setEvaporationCarry(t.getDoubleOr("evaporation_carry", 0d));
        d.zones = t.getCompoundOrEmpty("zones").copy();
        d.powerBuffer = Math.max(0, t.getLongOr("power", 0L));
        d.lastFreshWater = NbtCompat.has(t, "last_fresh_water", NbtCompat.ANY_NUMERIC) ? t.getLongOr("last_fresh_water", 0L) : -1L;
        d.targetTemp = t.contains("target_temp") ? TankTemperature.clampTarget(t.getFloatOr("target_temp", 0f)) : TankTemperature.DEFAULT_TARGET;
        d.waterTemp = t.contains("water_temp") && Float.isFinite(t.getFloatOr("water_temp", 0f)) ? t.getFloatOr("water_temp", 0f) : d.targetTemp;
        ListTag hl = t.getListOrEmpty("legacy_heaters");
        for (int i = 0; i < Math.min(LEGACY_HEATER_SLOTS, hl.size()); i++) {
            String id = hl.getStringOr(i, "");
            if (id.isEmpty()) continue;
            Identifier rl = Identifier.tryParse(id);
            Item item = rl == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(rl);
            if (item == Items.AIR) d.unknownHeaters[i] = id; // keep ids of removed mods
            else d.legacyHeaters().setItem(i, new ItemStack(item));
        }
        d.name = t.getStringOr("name", "");
        d.signVisible = !t.contains("sign_visible") || t.getBooleanOr("sign_visible", false);
        d.signPos = SignPosition.byName(t.getStringOr("sign_pos", ""));
        Direction f = Direction.byName(t.getStringOr("front", ""));
        d.front = f != null && f.getAxis().isHorizontal() ? f : Direction.SOUTH;
        d.locked = t.getBooleanOr("locked", false);
        d.breakProtected = t.getBooleanOr("break_protected", false); // M21.5: missing in older worlds = off
        ListTag inh = t.getListOrEmpty("inhabitants");
        // M17: a malformed UUID threw here, vanilla then dropped the whole block entity and the tank was lost
        for (Tag u : inh) if (u instanceof net.minecraft.nbt.IntArrayTag ia && ia.size() == 4) d.inhabitants.add(NbtCompat.loadUUID(u));
        ListTag lg = t.getListOrEmpty("log");
        for (int i = Math.max(0, lg.size() - MAX_LOG); i < lg.size(); i++) d.log.add(LogEntry.load(lg.getCompoundOrEmpty(i)));
        try {
            d.nest = NbtCompat.has(t, "nest", Tag.TAG_COMPOUND) ? BubbleNest.load(t.getCompoundOrEmpty("nest")) : null;
        } catch (RuntimeException e) { // M17: a broken nest must not take the tank with it
            org.slf4j.LoggerFactory.getLogger("bettacraft/tank").warn("Dropped an unreadable bubble nest of tank {}", d.id, e);
            d.nest = null;
        }
        ListTag pl = t.getListOrEmpty("plants");
        int maxId = 0;
        for (int i = 0; i < pl.size(); i++) {
            var p = org.overengineer.bettacraft.tank.plants.FloatingPlant.load(pl.getCompoundOrEmpty(i));
            if (p == null) continue;
            d.plants.add(p);
            maxId = Math.max(maxId, p.id);
        }
        d.nextPlantId = Math.max(maxId + 1, NbtCompat.has(t, "next_plant_id", NbtCompat.ANY_NUMERIC) ? t.getIntOr("next_plant_id", 0) : 1);
        // M14 (schema 8): small animals, clutches, moults, food pool, algae
        int maxCritter = 0;
        ListTag cl = t.getListOrEmpty("critters");
        for (int i = 0; i < cl.size(); i++) {
            var c = org.overengineer.bettacraft.critters.Critter.load(cl.getCompoundOrEmpty(i));
            if (c == null) continue;
            d.critters.add(c);
            maxCritter = Math.max(maxCritter, c.id);
        }
        ListTag ol = t.getListOrEmpty("critter_objects");
        for (int i = 0; i < ol.size(); i++) {
            var o = org.overengineer.bettacraft.critters.CritterObject.load(ol.getCompoundOrEmpty(i));
            if (o == null) continue;
            d.critterObjects.add(o);
            maxCritter = Math.max(maxCritter, o.id);
        }
        d.nextCritterId = Math.max(maxCritter + 1, NbtCompat.has(t, "next_critter_id", NbtCompat.ANY_NUMERIC) ? t.getIntOr("next_critter_id", 0) : 1);
        d.setCritterFood(t.getDoubleOr("critter_food", 0d));
        ListTag al = t.getListOrEmpty("algae");
        for (int i = 0; i < al.size(); i++) {
            CompoundTag a = al.getCompoundOrEmpty(i);
            int[] v = a.getIntArray("a").orElse(new int[0]);
            if (v.length != 4) continue;
            long p = a.getLongOr("p", 0L);
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
        ListTag tc = t.getListOrEmpty("clutches");
        int maxClutch = 0;
        for (int i = 0; i < tc.size(); i++) {
            var c = org.overengineer.bettacraft.tankmates.Clutch.load(tc.getCompoundOrEmpty(i));
            if (c == null) continue;
            d.clutches.add(c);
            maxClutch = Math.max(maxClutch, c.id);
        }
        d.nextClutchId = Math.max(maxClutch + 1, NbtCompat.has(t, "next_clutch_id", NbtCompat.ANY_NUMERIC) ? t.getIntOr("next_clutch_id", 0) : 1);
        d.breedingBox = t.getBooleanOr("breeding_box", false);
        d.freshWindowStart = NbtCompat.has(t, "fresh_window_start", NbtCompat.ANY_NUMERIC) ? t.getLongOr("fresh_window_start", 0L) : -1L;
        d.freshWindowMb = Math.max(0, t.getLongOr("fresh_window_mb", 0L));
        d.freshWindowTemp = NbtCompat.has(t, "fresh_window_temp", NbtCompat.ANY_NUMERIC) ? t.getFloatOr("fresh_window_temp", 0f) : Float.NaN;
        d.lastCoolChange = NbtCompat.has(t, "last_cool_change", NbtCompat.ANY_NUMERIC) ? t.getLongOr("last_cool_change", 0L) : -1L;
        d.salinity = Salinity.clampSalinity(t.getFloatOr("salinity", 0f));
        d.calcium = Salinity.clampCalcium(t.getFloatOr("calcium", 0f));
        d.oxygen = NbtCompat.has(t, "oxygen", NbtCompat.ANY_NUMERIC) ? t.getFloatOr("oxygen", 0f) : Float.NaN;
        d.setNitrate(t.getFloatOr("nitrate", 0f));
        d.lastWaterChange = NbtCompat.has(t, "last_water_change", NbtCompat.ANY_NUMERIC) ? t.getLongOr("last_water_change", 0L) : -1L;
        if (NbtCompat.has(t, "tds", NbtCompat.ANY_NUMERIC)) d.setTds(t.getFloatOr("tds", 0f)); // M25
        d.setPh(NbtCompat.has(t, "ph", NbtCompat.ANY_NUMERIC) ? t.getFloatOr("ph", 0f) : org.overengineer.bettacraft.chem.WaterChemistry.startPh(d.salinity));
        d.setTannin(t.getFloatOr("tannin", 0f));
        if (NbtCompat.has(t, "co2", NbtCompat.ANY_NUMERIC)) d.setCo2(t.getFloatOr("co2", 0f));
        // M25 part c: tanks of earlier versions get the alkalinity, magnesium and trace elements of their salt
        d.setKh(NbtCompat.has(t, "kh", NbtCompat.ANY_NUMERIC) ? t.getFloatOr("kh", 0f) : org.overengineer.bettacraft.chem.WaterChemistry.khOf(d.salinity));
        d.setMagnesium(NbtCompat.has(t, "magnesium", NbtCompat.ANY_NUMERIC) ? t.getFloatOr("magnesium", 0f) : org.overengineer.bettacraft.chem.WaterChemistry.magnesiumOf(d.salinity));
        d.setTrace(NbtCompat.has(t, "trace", NbtCompat.ANY_NUMERIC) ? t.getFloatOr("trace", 0f) : org.overengineer.bettacraft.chem.WaterChemistry.traceOf(d.salinity));
        d.setFertilizer(t.getFloatOr("fertilizer", 0f)); // M37
        d.lightDimmer = NbtCompat.has(t, "light_dimmer", NbtCompat.ANY_NUMERIC) ? Math.max(0, Math.min(100, t.getIntOr("light_dimmer", 0))) : 100;
        d.lightTimer = t.getBooleanOr("light_timer", false);
        d.lightOn = NbtCompat.has(t, "light_on", NbtCompat.ANY_NUMERIC) ? Math.floorMod(t.getIntOr("light_on", 0), 1440) : 8 * 60;
        d.lightOff = NbtCompat.has(t, "light_off", NbtCompat.ANY_NUMERIC) ? Math.floorMod(t.getIntOr("light_off", 0), 1440) : 20 * 60;
        if (NbtCompat.has(t, "terrarium", Tag.TAG_COMPOUND)) d.loadTerrarium(t.getCompoundOrEmpty("terrarium")); // M31
        ListTag te = t.getListOrEmpty("turtle_eggs"); // M31 part b
        int maxTurtle = 0;
        for (int i = 0; i < te.size(); i++) {
            var c = org.overengineer.bettacraft.terrarium.HerpClutch.load(te.getCompoundOrEmpty(i));
            if (c == null) continue;
            d.herpClutches.add(c);
            maxTurtle = Math.max(maxTurtle, c.id);
        }
        d.nextHerpClutchId = Math.max(maxTurtle + 1, NbtCompat.has(t, "next_turtle_clutch", NbtCompat.ANY_NUMERIC) ? t.getIntOr("next_turtle_clutch", 0) : 1);
        // M47 part c: frog spawn and tadpoles (absent in older worlds: none)
        int maxBrood = 0;
        ListTag fs = t.getListOrEmpty("frog_spawn");
        for (int i = 0; i < fs.size(); i++) {
            var s = org.overengineer.bettacraft.entity.frog.FrogSpawn.load(fs.getCompoundOrEmpty(i));
            if (s == null) continue;
            d.frogSpawn.add(s);
            maxBrood = Math.max(maxBrood, s.id);
        }
        ListTag tp = t.getListOrEmpty("tadpoles");
        for (int i = 0; i < tp.size(); i++) {
            var k = org.overengineer.bettacraft.entity.frog.Tadpole.load(tp.getCompoundOrEmpty(i));
            if (k == null) continue;
            d.tadpoles.add(k);
            maxBrood = Math.max(maxBrood, k.id);
        }
        d.nextFrogBroodId = Math.max(maxBrood + 1, NbtCompat.has(t, "next_frog_brood", NbtCompat.ANY_NUMERIC) ? t.getIntOr("next_frog_brood", 0) : 1);
        d.setTadpoleFood(t.getDoubleOr("tadpole_food", 0d));
        // M48 part b: plankton (absent in older worlds: none)
        CompoundTag plankton = t.getCompoundOrEmpty("plankton");
        d.setCopepods(plankton.getDoubleOr("copepods", 0d));
        d.setRotifers(plankton.getDoubleOr("rotifers", 0d));
        d.setPhytoplankton(plankton.getDoubleOr("phytoplankton", 0d));
        d.setNauplii(plankton.getDoubleOr("nauplii", 0d));
        d.setFlies(plankton.getDoubleOr("flies", 0d)); // M49
        for (Tag e : t.getListOrEmpty("mangroves")) {
            var m = org.overengineer.bettacraft.plants.Mangroves.Tree.load((CompoundTag) e);
            if (m != null) d.mangroves.add(m);
        }
        if (NbtCompat.has(t, "aquarist", Tag.TAG_COMPOUND)) { // M51
            CompoundTag a = t.getCompoundOrEmpty("aquarist");
            d.aquaristAccess = org.overengineer.bettacraft.aquarist.AquaristAccess.byId(a.getStringOr("access", ""));
            CompoundTag done = a.getCompoundOrEmpty("done");
            for (String k : done.keySet()) d.aquaristWork.putLong(k, done.getLongOr(k, 0L));
        }
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
        if (!frogSpawn.isEmpty()) { // M47 part c
            ListTag fs = new ListTag();
            for (var s : frogSpawn) fs.add(s.save());
            t.put("frog_spawn", fs);
        }
        if (!tadpoles.isEmpty()) {
            ListTag tp = new ListTag();
            double days = org.overengineer.bettacraft.entity.frog.FrogBreeding.tadpoleDays();
            for (var k : tadpoles) tp.add(k.saveClientView(days));
            t.put("tadpoles", tp);
        }
        if (salinity > 0) t.putFloat("salinity", salinity);
        if (terrariumWorthSaving()) t.put("terrarium", saveTerrarium()); // M31: the thermometer, the miniature and the turtles' client side
        return t;
    }

    public static TankData loadClientView(CompoundTag t) {
        TankData d = new TankData();
        d.clientView = true;
        int[] mn = t.getIntArray("min").orElse(new int[0]), mx = t.getIntArray("max").orElse(new int[0]);
        if (mn.length == 3 && mx.length == 3) {
            d.minX = mn[0]; d.minY = mn[1]; d.minZ = mn[2];
            d.maxX = mx[0]; d.maxY = mx[1]; d.maxZ = mx[2];
        }
        d.clientMemberCount = t.getIntOr("count", 0);
        if (mn.length == 3 && mx.length == 3) {
            if (NbtCompat.has(t, "shape", Tag.TAG_BYTE_ARRAY)) {
                d.members.addAll(TankShape.decode(t.getByteArray("shape").orElse(new byte[0]), d.minX, d.minY, d.minZ, d.sizeX(), d.sizeY(), d.sizeZ()));
            } else if ((long) d.sizeX() * d.sizeY() * d.sizeZ() <= 1 << 20) {
                for (int y = d.minY; y <= d.maxY; y++)
                    for (int z = d.minZ; z <= d.maxZ; z++)
                        for (int x = d.minX; x <= d.maxX; x++) d.members.add(BlockPos.asLong(x, y, z));
            }
        }
        d.scale = t.getIntOr("scale", 0);
        d.waterMb = t.getLongOr("water_mb", 0L);
        d.regionId = t.getIntOr("region", 0);
        d.name = t.getStringOr("name", "");
        d.signVisible = t.getBooleanOr("sign_visible", false);
        d.signPos = SignPosition.byName(t.getStringOr("sign_pos", ""));
        Direction f = Direction.byName(t.getStringOr("front", ""));
        d.front = f != null && f.getAxis().isHorizontal() ? f : Direction.SOUTH;
        d.waterTemp = t.getFloatOr("water_temp", 0f);
        d.clientInhabitants = t.getIntOr("inhabitants", 0);
        d.occupants = t.getIntOr("occupants", 0);
        d.nest = NbtCompat.has(t, "nest", Tag.TAG_COMPOUND) ? BubbleNest.loadClientView(t.getCompoundOrEmpty("nest")) : null;
        if (NbtCompat.has(t, "plants", Tag.TAG_BYTE_ARRAY)) d.plants.addAll(org.overengineer.bettacraft.tank.plants.FloatingPlantSim.decode(t.getByteArray("plants").orElse(new byte[0]), d.minX, d.minZ));
        if (NbtCompat.has(t, "critters", Tag.TAG_BYTE_ARRAY)) {
            var dec = org.overengineer.bettacraft.critters.CritterSync.decode(t.getByteArray("critters").orElse(new byte[0]), d.minX, d.minY, d.minZ);
            d.critters.addAll(dec.critters());
            d.critterObjects.addAll(dec.objects());
        }
        if (NbtCompat.has(t, "algae", Tag.TAG_BYTE_ARRAY)) d.algae.putAll(org.overengineer.bettacraft.tank.algae.AlgaeSim.decode(t.getByteArray("algae").orElse(new byte[0]), d.minX, d.minY, d.minZ));
        ListTag tc = t.getListOrEmpty("clutches");
        for (int i = 0; i < tc.size(); i++) {
            var c = org.overengineer.bettacraft.tankmates.Clutch.loadClientView(tc.getCompoundOrEmpty(i));
            if (c != null) d.clutches.add(c);
        }
        d.breedingBox = t.getBooleanOr("breeding_box", false);
        ListTag fs = t.getListOrEmpty("frog_spawn"); // M47 part c
        for (int i = 0; i < fs.size(); i++) {
            var s = org.overengineer.bettacraft.entity.frog.FrogSpawn.load(fs.getCompoundOrEmpty(i));
            if (s != null) d.frogSpawn.add(s);
        }
        ListTag tp = t.getListOrEmpty("tadpoles");
        for (int i = 0; i < tp.size(); i++) d.tadpoles.add(org.overengineer.bettacraft.entity.frog.Tadpole.loadClientView(tp.getCompoundOrEmpty(i)));
        d.salinity = Salinity.clampSalinity(t.getFloatOr("salinity", 0f));
        if (NbtCompat.has(t, "terrarium", Tag.TAG_COMPOUND)) d.loadTerrarium(t.getCompoundOrEmpty("terrarium")); // M31
        return d;
    }

    @Override
    public String toString() {
        return "Tank[" + id + " " + sizeX() + "x" + sizeY() + "x" + sizeZ() + " at " + NbtCompat.writeBlockPos(min()) + ", "
                + waterMb + "/" + capacityMb() + " mB, scale " + scale + ", region " + regionId + "]";
    }
}
