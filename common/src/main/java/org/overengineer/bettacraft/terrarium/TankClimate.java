package org.overengineer.bettacraft.terrarium;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.interior.InteriorGround;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;

/**
 * M31: the terrarium part of the care step (every second, {@link TankCare#serverTick}): works out the mode of the tank
 * ({@link TerrariumMode}) from its water and the land of its interior, and for a terrarium or paludarium the climate
 * ({@link Climate}) from the room, the heat mats, the basking lamps, the misters, the water surface and the lid. Aquaria
 * get no climate (their menu, overlays and care stay as they were). The land scan samples the interior height map
 * ({@link InteriorGround}) every 10 seconds or when the water changes; while interior chunks are still loading the mode
 * stays as it was.
 */
public final class TankClimate {
    /** Ticks between two land scans. */
    public static final long SCAN_TICKS = 200;
    /** The clients get the climate again after this many ticks at most when it changed. */
    public static final long SYNC_TICKS = 100;

    private TankClimate() {}

    public static boolean enabled() {
        return Features.enabled(Feature.TERRARIUMS);
    }

    public static TerrariumMode.Thresholds thresholds() {
        return new TerrariumMode.Thresholds(ServerConfig.getOr(ServerConfig.TERRA_PALUDARIUM_LAND, 0.15),
                ServerConfig.getOr(ServerConfig.TERRA_TERRARIUM_LAND, 0.9));
    }

    public static Climate.Params params() {
        return new Climate.Params(ServerConfig.getOr(ServerConfig.TERRA_MAT_LIFT, 8.0), ServerConfig.getOr(ServerConfig.TERRA_LAMP_AIR, 4.0),
                ServerConfig.getOr(ServerConfig.TERRA_BASK_LIFT, 12.0), ServerConfig.getOr(ServerConfig.TERRA_LAMP_UV, 5.0),
                ServerConfig.getOr(ServerConfig.TERRA_SKY_UV, 3.0), ServerConfig.getOr(ServerConfig.TERRA_MIST_HUMIDITY, 40.0),
                ServerConfig.getOr(ServerConfig.TERRA_WATER_HUMIDITY, 35.0), ServerConfig.getOr(ServerConfig.TERRA_PLANT_HUMIDITY, 15.0),
                ServerConfig.getOr(ServerConfig.TERRA_TAU_AIR, 0.05), ServerConfig.getOr(ServerConfig.TERRA_TAU_HUMIDITY, 0.03));
    }

    public static String f1(float v) {
        return Float.isFinite(v) ? String.format(Locale.ROOT, "%.1f", v) : "--";
    }

    /** A heat mat, mister or lamp at a tank changed: the equipment is scanned again at once. */
    public static void equipmentChanged(Level level, BlockPos tankPos) {
        if (level.isClientSide() || !level.isLoaded(tankPos)) return;
        TankBlockEntity c = TankMultiblock.getController(level, tankPos);
        if (c != null) TankEquipment.invalidate(c);
    }

    // ------------------------------------------------------------------ land

    /**
     * Share of the floor that is land and the share of that land covered with plants; M33.1: {@code tops} are the walkable
     * tops (1/16 interior block) of the sampled columns, for the water limit of a fixed paludarium ({@link WaterLimit}).
     */
    public record Land(double land, double plants, boolean complete, int[] tops) {
        public Land(double land, double plants, boolean complete) {
            this(land, plants, complete, new int[0]);
        }
    }

    /**
     * Samples the interior columns (at most about 48 x 48): a column is land when its walkable top lies above the water
     * surface (every column without water); a plant (flowers, grass, ferns, moss, leaves, aquarium plants) on its top
     * makes it planted. {@code complete} is false while some interior chunks were not loaded.
     */
    public static Land scanLand(MinecraftServer server, TankData t) {
        InteriorRegion r = InteriorRegions.get(server).get(t.regionId());
        ServerLevel interior = InteriorManager.interiorLevel(server);
        if (r == null || !r.built() || interior == null) return new Land(t.waterMb() <= 0 ? 1 : 0, 0, true);
        double water = t.waterMb() <= 0 ? -1 : TankCare.waterLocalHeight(t, r.scale());
        int step = Math.max(1, Math.max(r.sizeX(), r.sizeZ()) / 48);
        int total = 0, land = 0, planted = 0;
        it.unimi.dsi.fastutil.ints.IntArrayList tops = new it.unimi.dsi.fastutil.ints.IntArrayList();
        boolean complete = true;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int lz = step / 2; lz < r.sizeZ(); lz += step) {
            for (int lx = step / 2; lx < r.sizeX(); lx += step) {
                if (!r.inShape(lx, 0, lz)) continue;
                int wx = r.originX() + lx, wz = r.originZ() + lz;
                if (interior.getChunkSource().getChunkNow(wx >> 4, wz >> 4) == null) {
                    complete = false;
                    InteriorGround.top16(server, r, lx, lz); // asks for the chunk
                    continue;
                }
                total++;
                int top16 = InteriorGround.top16(server, r, lx, lz);
                tops.add(top16);
                if (water >= 0 && top16 / 16.0 <= water + 0.05) continue;
                land++;
                int ly = Math.max(0, (top16 - 1) / 16);
                p.set(wx, InteriorRegion.BASE_Y + ly, wz);
                if (plant(interior.getBlockState(p))) planted++;
                else if (ly + 1 < r.sizeY() && plant(interior.getBlockState(p.move(0, 1, 0)))) planted++;
            }
        }
        if (total == 0) return new Land(t.waterMb() <= 0 ? 1 : 0, 0, complete, tops.toIntArray());
        return new Land(land / (double) total, land == 0 ? 0 : planted / (double) land, complete, tops.toIntArray());
    }

    static boolean plant(BlockState s) {
        if (s.isAir()) return false;
        Block b = s.getBlock();
        return b instanceof net.minecraft.world.level.block.VegetationBlock || b instanceof org.overengineer.bettacraft.plants.AquaticPlantBlock
                || s.is(BlockTags.LEAVES) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS) || s.is(net.minecraft.world.level.block.Blocks.MOSS_BLOCK)
                || s.is(net.minecraft.world.level.block.Blocks.MOSS_CARPET) || s.is(BlockTags.CAVE_VINES) || b instanceof net.minecraft.world.level.block.VineBlock
                || b instanceof net.minecraft.world.level.block.GrowingPlantBlock;
    }

    /** Floor columns of the tank. */
    public static int footprint(TankData t) {
        it.unimi.dsi.fastutil.longs.LongOpenHashSet cols = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        for (long l : t.members()) cols.add(BlockPos.asLong(BlockPos.getX(l), 0, BlockPos.getZ(l)));
        return Math.max(1, cols.size());
    }

    // ------------------------------------------------------------------ the room

    /** Relative humidity of the room around the tank (biome rainfall, rain on the tank). */
    public static float roomHumidity(ServerLevel level, TankData t) {
        BlockPos c = BlockPos.containing(org.overengineer.bettacraft.tank.TankInhabitants.tankBox(t).getCenter());
        var biome = level.getBiome(c).value();
        float downfall = org.overengineer.bettacraft.platform.Platform.get().biomeDownfall(biome);
        boolean rain = false;
        for (BlockPos top : t.openTops()) {
            if (level.isLoaded(top) && level.isRainingAt(top)) {
                rain = true;
                break;
            }
        }
        return Climate.roomHumidity(downfall, rain);
    }

    /** 0..1 sunlight UV through open tops that see the sky (only by day; rain lets a third through). */
    public static double sky(ServerLevel level, TankData t) {
        if (!level.dimensionType().hasSkyLight() || !level.isBrightOutside()) return 0;
        int n = 0, open = 0;
        for (BlockPos top : t.openTops()) {
            if (!level.isLoaded(top)) continue;
            n++;
            if (!TankWater.covers(level, top) && level.canSeeSky(top)) open++;
        }
        if (n == 0 || open == 0) return 0;
        return Math.min(1.0, 2.0 * open / n) * (level.isRaining() ? 0.3 : 1.0);
    }

    // ------------------------------------------------------------------ devices

    /** Lit basking lamps on the tank's top blocks (positions). */
    public static List<BlockPos> litLamps(Level level, TankData t) {
        List<BlockPos> out = new java.util.ArrayList<>();
        for (BlockPos top : t.openTops()) {
            if (!level.isLoaded(top)) continue;
            BlockState s = level.getBlockState(top);
            if (s.getBlock() instanceof BaskingLampBlock && s.getValue(TankLightBlock.LIT)) out.add(top);
        }
        return out;
    }

    /**
     * The basking spots: the land under every lit basking lamp (tank-world position on the ground), for the animals that
     * go there. Empty without a lit lamp or without land under it.
     */
    public static List<Vec3> baskingSpots(ServerLevel level, TankData t) {
        List<Vec3> out = new java.util.ArrayList<>();
        if (!Features.enabled(Feature.TERRARIUM_DEVICES)) return out;
        var terrain = new org.overengineer.bettacraft.critters.TankTerrain(t, level.getServer(),
                InteriorRegions.get(level.getServer()).get(t.regionId()), level.isBrightOutside());
        double surface = t.waterMb() <= 0 ? Double.NEGATIVE_INFINITY : t.waterSurfaceY();
        for (BlockPos lamp : litLamps(level, t)) {
            double x = lamp.getX() + 0.5, z = lamp.getZ() + 0.5;
            double g = terrain.ground(x, z);
            if (Double.isFinite(g) && g > surface + 0.01) out.add(new Vec3(x, g, z));
        }
        return out;
    }

    // ------------------------------------------------------------------ the step

    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        TankData t = c.getTank();
        if (!enabled()) {
            if (t.terrariumMode() != TerrariumMode.AQUARIUM || t.hasClimate()) {
                t.setTerrariumMode(TerrariumMode.AQUARIUM);
                t.setClimate(null);
                c.markTankChanged();
            }
            return;
        }
        long now = level.getGameTime();
        scanIfDue(level, c, false);
        TerrariumMode mode = mode(t, TerrariumMode.detect(t.waterMb(), c.climateLand, thresholds()));
        boolean sync = false;
        if (mode != t.terrariumMode()) {
            TerrariumMode was = t.terrariumMode();
            t.setTerrariumMode(mode);
            // a tank that was never anything but an aquarium does not log (old empty tanks become terraria quietly)
            if (was != TerrariumMode.AQUARIUM || mode != TerrariumMode.TERRARIUM || !t.inhabitants().isEmpty())
                TankCare.log(level, c, "log.bettacraft.terrarium.mode", "@" + mode.translationKey());
            sync = true;
        }
        if (!mode.hasLand()) {
            if (t.hasClimate()) {
                t.setClimate(null);
                sync = true;
            }
            if (c.climateMats > 0 || sync) offMats(level, c); // mats that glowed on a terrarium go dark
            c.climateMats = 0;
            c.climateMisting = 0;
            c.climateMistWant = 0;
            c.climateLamps = 0;
            if (sync) sync(c, now, true);
            return;
        }
        boolean devices = Features.enabled(Feature.TERRARIUM_DEVICES);
        // heat mats (with the heaters' power), basking lamps, misters
        double ground = 0, wall = 0;
        List<BlockPos> mats = devices ? TankEquipment.heatMats(level, c) : List.of();
        for (BlockPos m : mats) {
            if (HeatMatBlock.underFloor(level.getBlockState(m))) ground++;
            else wall++;
        }
        double rawGround = ground, rawWall = wall;
        if (!c.heatPowered()) ground = wall = 0; // without FE the mats stay cold
        int lamps = devices ? litLamps(level, t).size() : 0;
        int[] top = TankWater.openTop(level, t);
        int tops = Math.max(1, t.openTops().size());
        double open = top[1] <= 0 ? 0 : top[0] / (double) top[1];
        double waterShare = t.waterMb() <= 0 ? 0 : Math.max(0, 1 - c.climateLand);
        Climate.Inputs in = new Climate.Inputs(TankCare.ambient(level, t), roomHumidity(level, t), t.targetTemp(), open, waterShare, t.waterTemp(),
                ground, wall, footprint(t), lamps, tops, c.climatePlants, 0, sky(level, t));
        Climate.Params p = params();
        Climate.State before = t.climate();
        // the misters spray while the humidity is below their setting (and they have water and power)
        int misting = 0, wanting = 0;
        float rhNow = before != null ? before.humidity() : Climate.equilibrium(in, p).humidity();
        if (devices) {
            double seconds = days * 24000.0 / 20.0;
            for (MisterBlockEntity m : TankEquipment.misters(level, c)) {
                if (!m.wants(rhNow, now) || m.amount() <= 0) continue;
                wanting++;
                if (c.misterPowered() && m.spray(now, seconds)) misting++;
            }
        }
        Climate.State next = Climate.step(before, in.withMisting(misting), p, days);
        t.setClimate(next);
        // the FE demand of the next step: what the mats and misters would use with power
        double rawDuty = c.heatPowered() ? next.matDuty() : Climate.equilibrium(new Climate.Inputs(in.ambient(), in.roomHumidity(), in.target(), open,
                waterShare, in.waterTemp(), rawGround, rawWall, in.footprint(), lamps, tops, in.plantShare(), misting, in.sky()), p).matDuty();
        c.climateMats = (rawGround + rawWall) * rawDuty;
        c.climateMisting = misting;
        c.climateMistWant = wanting;
        c.climateLamps = lamps;
        c.setChanged();
        updateMats(level, c, next.matDuty());
        sync(c, now, sync);
    }

    /** The land: every 10 s, at once when the water changed (or {@code force}). */
    private static void scanIfDue(ServerLevel level, TankBlockEntity c, boolean force) {
        TankData t = c.getTank();
        long now = level.getGameTime();
        if (force || c.climateScanTime == Long.MIN_VALUE || now - c.climateScanTime >= SCAN_TICKS || now < c.climateScanTime || c.climateScanWater != t.waterMb()) {
            Land land = scanLand(level.getServer(), t);
            if (land.complete() || c.climateLand < 0) {
                c.climateScanTime = now;
                c.climateScanWater = t.waterMb();
                c.climateLand = land.land();
                c.climatePlants = land.plants();
                if (land.complete() || land.tops().length > 0 || c.climateTops == null) c.climateTops = land.tops(); // M33.1
                t.setLandShare((float) land.land());
            }
        }
    }

    /**
     * The mode of the tank right now (scans the land at once; for putting an animal in, before the next care step). Sets
     * it on the tank without logging; the next care step works out the climate.
     */
    public static TerrariumMode refreshMode(ServerLevel level, TankBlockEntity c) {
        if (!enabled()) return TerrariumMode.AQUARIUM;
        scanIfDue(level, c, true);
        TerrariumMode m = mode(c.getTank(), TerrariumMode.detect(c.getTank().waterMb(), c.climateLand, thresholds()));
        if (m != c.getTank().terrariumMode()) {
            c.getTank().setTerrariumMode(m);
            c.markTankChanged();
        }
        return m;
    }

    /** M33.1: the mode of a tank: the type set in the menu, or (automatic) the {@code detected} one. */
    public static TerrariumMode mode(TankData t, TerrariumMode detected) {
        TerrariumMode fixed = WaterLimit.setting(t).fixed();
        return fixed != null ? fixed : detected;
    }

    /** M33.1: scans the land now (the water limit of a paludarium whose land was never scanned). */
    public static void scanNow(ServerLevel level, TankBlockEntity c) {
        scanIfDue(level, c, true);
    }

    public static boolean misterPowerRequired() {
        return org.overengineer.bettacraft.tank.TankPower.power() && ServerConfig.getOr(ServerConfig.POWER_MISTER, false);
    }

    private static void offMats(ServerLevel level, TankBlockEntity c) {
        for (BlockPos m : TankEquipment.heatMats(level, c)) {
            BlockState s = level.getBlockState(m);
            if (s.getBlock() instanceof HeatMatBlock && s.getValue(HeatMatBlock.HEATING)) level.setBlock(m, s.setValue(HeatMatBlock.HEATING, false), Block.UPDATE_CLIENTS);
        }
    }

    /** The heat mats glow while they heat. */
    private static void updateMats(ServerLevel level, TankBlockEntity c, double duty) {
        boolean on = duty > 0.02 && c.heatPowered() && Features.enabled(Feature.TERRARIUM_DEVICES);
        for (BlockPos m : TankEquipment.heatMats(level, c)) {
            BlockState s = level.getBlockState(m);
            if (s.getBlock() instanceof HeatMatBlock && s.getValue(HeatMatBlock.HEATING) != on) level.setBlock(m, s.setValue(HeatMatBlock.HEATING, on), Block.UPDATE_CLIENTS);
        }
    }

    /** Sends the tank to the clients when the mode changed or the climate moved noticeably (at most every 5 s). */
    private static void sync(TankBlockEntity c, long now, boolean force) {
        TankData t = c.getTank();
        float air = t.airTemp(), rh = t.humidity(), uv = t.uv();
        boolean moved = Float.isFinite(air) != Float.isFinite(c.syncedAir) || Math.abs(air - c.syncedAir) >= 0.3F || Math.abs(rh - c.syncedRh) >= 1.5F
                || Math.abs(uv - c.syncedUv) >= 0.2F;
        if (!force && (!moved || (c.climateSyncTime != Long.MIN_VALUE && now - c.climateSyncTime >= 0 && now - c.climateSyncTime < SYNC_TICKS))) return;
        c.syncedAir = air;
        c.syncedRh = rh;
        c.syncedUv = uv;
        c.climateSyncTime = now;
        c.markTankChanged();
    }

    /** The tank's climate as the needs of the animals see it. */
    public static TerrariumNeeds.Conditions conditions(TankData t) {
        double air = t.hasClimate() ? t.airTemp() : Double.NaN;
        return new TerrariumNeeds.Conditions(t.terrariumMode(), air, t.hasClimate() ? t.humidity() : Double.NaN, t.uv(),
                t.hasClimate() ? t.baskTemp() : air, Math.max(0, t.landShare()), t.waterMb() > 0 ? t.waterTemp() : Double.NaN,
                t.capacityMb() / (double) TankData.MB_PER_BLOCK); // M31 part b: the size for the big turtles
    }

    /** The climate state of a tank, or null. */
    @Nullable
    public static Climate.State of(TankData t) {
        return t.climate();
    }
}
