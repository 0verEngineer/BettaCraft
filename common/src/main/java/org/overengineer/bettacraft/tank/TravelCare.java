package org.overengineer.bettacraft.tank;

import org.overengineer.bettacraft.util.NbtCompat;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.Care;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaCare;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.entity.tankmate.TankmateCare;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.FilterHost;

/**
 * M50 (A1, C1): the care of a tank that travels on a Create contraption, where it has no block entity, only its data
 * (the controller's block entity data in the contraption). Every second of the trip this advances that data:
 * <ul>
 *     <li><b>Power:</b> the filters and heaters that travel along need FE like on the ground (power.* options). It comes from
 *     the tank's own buffer, which the drive adapters of the contraption at the tank fill while the contraption moves, and
 *     from FE batteries of the contraption next to the tank ({@link Env#battery}). Without power heaters and filters stop
 *     and the values slowly get worse, as in a power cut.</li>
 *     <li><b>Water:</b> the temperature goes towards the air of the biome the contraption is in (heaters and coolers with
 *     power hold the target), the water quality falls by the waste of the animals, the filters with power clean.</li>
 *     <li><b>Animals:</b> the packed bettas, tankmates and animals of other mods with a care profile get hungry and stressed
 *     by bad water or temperature like in the tank ({@link #annotate} wrote what they bear when they were packed); nobody
 *     dies on the road (at the brink an animal waits for the arrival). Breeding pauses.</li>
 * </ul>
 * A summary of the trip ({@link #TRIP_KEY}) goes along; the tank writes it into its log when it is put down. Create-free:
 * the contraption side ({@code compat/create/CreateTravel}) gathers the {@link Env}.
 */
public final class TravelCare {
    /** Key of what a packed animal bears (and how it is now, for drawing it), in its entity data. */
    public static final String CARE_KEY = "bettacraft:travel_care";
    /** Key of the trip summary in the controller's block entity data. */
    public static final String TRIP_KEY = "bettacraft_trip";

    private TravelCare() {}

    /** {@code features.compat.create.travelCare}: tanks keep living while they travel. */
    public static boolean enabled() {
        return Features.enabled(Feature.CREATE_TRAVEL_CARE);
    }

    // ------------------------------------------------------------------ packing

    /**
     * What a packed animal bears, written into its entity data when it is packed: its bioload, and for animals with our
     * health timers where they are kept ({@code path}), their temperature range, the water quality they need and the
     * parameters of their timers.
     */
    public static void annotate(CompoundTag tag, Entity e) {
        CompoundTag c = new CompoundTag();
        if (e instanceof BettaEntity b) {
            c.putString("kind", "betta");
            c.putDouble("load", b.isAdult() ? 1.0 : 0.25);
            c.putDouble("lo", ServerConfig.getOr(ServerConfig.TEMPERATURE_TOLERATED_MIN, (double) TankTemperature.TOLERANCE_MIN));
            c.putDouble("hi", ServerConfig.getOr(ServerConfig.TEMPERATURE_TOLERATED_MAX, (double) TankTemperature.TOLERANCE_MAX));
            c.putDouble("q", -1);
            params(c, BettaCare.params());
        } else if (e instanceof TankmateEntity m) {
            c.putString("kind", "tankmate");
            c.putDouble("load", m.species().stock() * (m.isAdult() ? 1.0 : 0.25));
            c.putDouble("lo", m.species().tempMin());
            c.putDouble("hi", m.species().tempMax());
            c.putDouble("q", m.species().minQuality());
            params(c, TankmateCare.params(m.species()));
        } else {
            var p = org.overengineer.bettacraft.foreign.ForeignProfiles.of(e.getType());
            c.putDouble("load", 1.0);
            if (p != null && Features.enabled(Feature.FOREIGN_CARE) && p.terrarium() == null) {
                c.putString("kind", "foreign");
                c.putDouble("lo", p.tempMin());
                c.putDouble("hi", p.tempMax());
                c.putDouble("q", p.minQuality());
                params(c, org.overengineer.bettacraft.foreign.ForeignCare.params(p));
            }
        }
        BettaHealth h = health(tag, c.getStringOr("kind", ""));
        if (h != null) look(c, h, c);
        tag.put(CARE_KEY, c);
    }

    private static void params(CompoundTag c, BettaHealth.Params p) {
        c.putDouble("p_sick", p.daysUntilSick());
        c.putDouble("p_death", p.daysSickUntilDeath());
        c.putDouble("p_both", p.bothBadMultiplier());
        c.putDouble("p_rec", p.recoveryPerDay());
        c.putDouble("p_hungry", p.hungryAfterDays());
    }

    /** The timers on the road: nobody dies there (NO_DEATH; DECOR stays DECOR), no diet bonus. */
    private static BettaHealth.Params paramsOf(CompoundTag c) {
        ServerConfig.CareMode mode = Care.mode() == ServerConfig.CareMode.DECOR ? ServerConfig.CareMode.DECOR : ServerConfig.CareMode.NO_DEATH;
        return new BettaHealth.Params(c.getDoubleOr("p_sick", 0d), c.getDoubleOr("p_death", 0d), c.getDoubleOr("p_both", 0d), c.getDoubleOr("p_rec", 0d),
                c.getDoubleOr("p_hungry", 0d), BettaHealth.Diet.OFF, mode);
    }

    /** The health timers of a packed animal by its kind, or null (no timers of ours). */
    @Nullable
    private static BettaHealth health(CompoundTag tag, String kind) {
        CompoundTag h = healthTag(tag, kind);
        return h == null ? null : BettaHealth.load(h);
    }

    @Nullable
    private static CompoundTag healthTag(CompoundTag tag, String kind) {
        return switch (kind) {
            case "betta" -> NbtCompat.has(tag.getCompoundOrEmpty(org.overengineer.bettacraft.entity.betta.BettaData.KEY), "health", Tag.TAG_COMPOUND)
                    ? tag.getCompoundOrEmpty(org.overengineer.bettacraft.entity.betta.BettaData.KEY).getCompoundOrEmpty("health") : null;
            case "tankmate" -> NbtCompat.has(tag.getCompoundOrEmpty(TankmateEntity.KEY), "health", Tag.TAG_COMPOUND)
                    ? tag.getCompoundOrEmpty(TankmateEntity.KEY).getCompoundOrEmpty("health") : null;
            case "foreign" -> {
                CompoundTag d = tag.getCompoundOrEmpty("NeoForgeData");
                yield NbtCompat.has(d, org.overengineer.bettacraft.foreign.ForeignCare.KEY, Tag.TAG_COMPOUND)
                        ? d.getCompoundOrEmpty(org.overengineer.bettacraft.foreign.ForeignCare.KEY) : new BettaHealth().save();
            }
            default -> null;
        };
    }

    private static void putHealth(CompoundTag tag, String kind, BettaHealth h) {
        switch (kind) {
            case "betta" -> tag.getCompoundOrEmpty(org.overengineer.bettacraft.entity.betta.BettaData.KEY).put("health", h.save());
            case "tankmate" -> tag.getCompoundOrEmpty(TankmateEntity.KEY).put("health", h.save());
            case "foreign" -> {
                CompoundTag d = tag.getCompoundOrEmpty("NeoForgeData");
                d.put(org.overengineer.bettacraft.foreign.ForeignCare.KEY, h.save());
                tag.put("NeoForgeData", d);
            }
            default -> {}
        }
    }

    /** How the animal is now (drawn on the contraption): satiation, sick, health 0..1. */
    private static void look(CompoundTag c, BettaHealth h, CompoundTag params) {
        BettaHealth.Params p = paramsOf(params);
        c.putFloat("sat", h.satiation(p));
        c.putBoolean("sick", h.isSick());
        c.putFloat("hp", h.healthFraction(p));
    }

    /** Satiation 0..1 of a packed animal as last simulated (1 if unknown), for drawing it. */
    public static float satiation(CompoundTag tag) {
        CompoundTag c = tag.getCompoundOrEmpty(CARE_KEY);
        return NbtCompat.has(c, "sat", Tag.TAG_FLOAT) ? c.getFloatOr("sat", 0f) : 1F;
    }

    public static boolean sick(CompoundTag tag) {
        return tag.getCompoundOrEmpty(CARE_KEY).getBooleanOr("sick", false);
    }

    // ------------------------------------------------------------------ the trip

    /** What the contraption gives the tank this step. */
    public interface Battery {
        /** Takes up to {@code fe} FE from the batteries of the contraption; returns what it got. */
        long pull(long fe);
    }

    /**
     * The surroundings of a travelling tank for one step.
     *
     * @param ambient     air temperature at the contraption (biome, day and night)
     * @param heatBlocks  heating capacity of the heaters that travel along (interior rods, filters on the contraption)
     * @param coolBlocks  cooling capacity of the coolers that travel along
     * @param cleanBlocks water blocks the filters that travel along keep clean while they run
     * @param filterLoad  their power share (filters by their regulators) for the FE they take
     * @param passive     water blocks cleaned without power (live rock)
     * @param dynamoFe    FE per tick the drive adapters at the tank made on average while the contraption moved
     * @param battery     the FE batteries of the contraption next to the tank
     */
    public record Env(float ambient, double heatBlocks, double coolBlocks, double cleanBlocks, double filterLoad, double passive,
                      double dynamoFe, Battery battery) {}

    /** The heaters, coolers and filters in the interior of the tank (they always travel along). */
    public static Env interior(ServerLevel level, TankData t, float ambient, List<FilterHost> hosts, double dynamoFe, Battery battery) {
        double heat = TankCare.interiorHeaterBlocks(level, t), cool = TankCare.interiorCoolerBlocks(level, t);
        double[] in = TankWater.interiorShares(level, t);
        double clean = in[0] * (double) ServerConfig.getOr(ServerConfig.WATER_QUALITY_INTERNAL_FILTER_BLOCKS, 16)
                + in[1] * (double) ServerConfig.getOr(ServerConfig.FILTER_CORNER_BLOCKS, 20);
        double load = in[0] + in[1] + in[2];
        for (FilterHost f : hosts) {
            if (!f.hasFilter()) continue;
            heat += f.heaterBlocks();
            cool += f.coolerBlocks();
            clean += f.powerShare() * (double) ServerConfig.getOr(ServerConfig.WATER_QUALITY_EXTERNAL_FILTER_BLOCKS, 32);
            load += f.powerShare();
        }
        double passive = org.overengineer.bettacraft.reef.TankReef.liveRockBlocks(level, t);
        return new Env(ambient, heat, cool, clean, load, passive, dynamoFe, battery);
    }

    /** The air temperature at a point of the world (the biome there, day and night), like {@link TankCare#ambient}. */
    public static float ambientAt(ServerLevel level, Vec3 at) {
        BlockPos p = BlockPos.containing(at);
        float t = level.isLoaded(p) ? TankTemperature.ambientFromBiome(level.getBiome(p).value().getBaseTemperature()) : TankTemperature.ambientFromBiome(0.8F);
        if (level.dimensionType().hasSkyLight() && !level.dimensionType().hasFixedTime()) t += TankTemperature.dayNightOffset(level.getDayTime());
        return t;
    }

    /** What one step did (for the tests and the client view). */
    public record Result(boolean filterPowered, boolean heatPowered, int demand, long fromBattery, long fromDynamo) {}

    /**
     * One care step of {@code ticks} game ticks on the data of a travelling controller ({@code be}: its block entity data
     * with {@code tank} and {@code stowed}). Returns null if the data holds no tank.
     */
    @Nullable
    public static Result step(ServerLevel level, CompoundTag be, Env env, long ticks) {
        if (!NbtCompat.has(be, "tank", Tag.TAG_COMPOUND) || ticks <= 0) return null;
        TankData t = TankData.load(be.getCompoundOrEmpty("tank"));
        double days = ticks / 24000.0;
        long n = ticks;
        // power: what the devices that travel along need, as on the ground (TankPower.step)
        boolean anyPower = TankPower.anyRequired();
        int filterFe = anyPower && TankPower.filterRequired() && env.filterLoad() > 0
                ? (int) Math.ceil(env.filterLoad() * ServerConfig.getOr(ServerConfig.POWER_FILTER_FE, 10) - 1e-9) : 0;
        Boolean heating = t.waterMb() > 0 ? TankTemperature.heating(env.ambient(), t.targetTemp()) : null;
        int heatFe = 0;
        if (anyPower && TankPower.heaterRequired() && heating != null) {
            double cap = heating ? env.heatBlocks() : env.coolBlocks();
            double perBlock = heating ? ServerConfig.getOr(ServerConfig.POWER_HEATER_FE_PER_BLOCK, 8.0) : ServerConfig.getOr(ServerConfig.POWER_COOLER_FE_PER_BLOCK, 8.0);
            if (cap > 0) heatFe = (int) Math.ceil(Math.min(cap, Math.max(1.0, t.waterBlocks())) * perBlock);
        }
        long cap = TankPower.capacity();
        long buf = t.powerBuffer();
        long dyn = 0;
        if (env.dynamoFe() > 0 && TankPower.power()) {
            dyn = Math.min(Math.max(0, cap - buf), (long) Math.floor(env.dynamoFe() * n));
            buf += dyn;
        }
        long need = (long) (filterFe + heatFe) * n;
        long fromBattery = 0;
        if (need > buf && env.battery() != null) {
            fromBattery = env.battery().pull(need - buf);
            buf += fromBattery;
        }
        boolean filterOn = filterFe <= 0 || buf >= (long) filterFe * n;
        if (filterFe > 0 && filterOn) buf -= (long) filterFe * n;
        boolean heatOn = heatFe <= 0 || buf >= (long) heatFe * n;
        if (heatFe > 0 && heatOn) buf -= (long) heatFe * n;
        t.setPowerBuffer(Math.min(cap, buf));
        // temperature
        float eq = t.waterMb() <= 0 ? env.ambient()
                : TankTemperature.equilibrium(env.ambient(), t.targetTemp(), heatOn ? env.heatBlocks() : 0, heatOn ? env.coolBlocks() : 0, t.waterBlocks());
        t.setWaterTemp(TankTemperature.approach(t.waterTemp(), eq, days, ServerConfig.getOr(ServerConfig.TEMPERATURE_TAU_DAYS, 0.1)));
        // water quality
        ListTag stowed = be.getListOrEmpty("stowed");
        double load = org.overengineer.bettacraft.critters.TankCritters.bioload(t);
        for (int i = 0; i < stowed.size(); i++) {
            CompoundTag c = stowed.getCompoundOrEmpty(i).getCompoundOrEmpty(CARE_KEY);
            load += NbtCompat.has(c, "load", NbtCompat.ANY_NUMERIC) ? c.getDoubleOr("load", 0d) : 1.0;
        }
        if (TankWater.qualityEnabled() && t.waterMb() > 0) {
            t.setWaterQuality(WaterQuality.step(t.waterQuality(), days, load, t.systemBlocks(), (filterOn ? env.cleanBlocks() : 0) + env.passive(),
                    TankWater.params()));
        }
        // the animals
        float temp = t.waterTemp();
        boolean waterBadDefault = TankWater.isBad(t);
        long now = level.getGameTime();
        for (int i = 0; i < stowed.size(); i++) {
            CompoundTag tag = stowed.getCompoundOrEmpty(i);
            CompoundTag c = tag.getCompoundOrEmpty(CARE_KEY);
            String kind = c.getStringOr("kind", "");
            if (NbtCompat.has(tag, "last_sim", Tag.TAG_LONG)) tag.putLong("last_sim", now); // the trip is simulated here, not again later
            BettaHealth h = health(tag, kind);
            if (h == null || !kindEnabled(kind)) continue;
            boolean tempBad = t.waterMb() > 0 && (temp < c.getDoubleOr("lo", 0d) - 1e-4 || temp > c.getDoubleOr("hi", 0d) + 1e-4);
            double q = c.getDoubleOr("q", 0d);
            boolean waterBad = t.waterMb() > 0 && (q < 0 ? waterBadDefault : TankWater.qualityEnabled() && t.waterQuality() < q);
            h.step(days, new BettaHealth.Conditions(tempBad, false, waterBad, 0), paramsOf(c));
            putHealth(tag, kind, h);
            look(c, h, c);
            tag.put(CARE_KEY, c);
        }
        be.put("stowed", stowed);
        be.put("tank", t.save());
        // the trip
        CompoundTag trip = be.getCompoundOrEmpty(TRIP_KEY);
        trip.putLong("ticks", trip.getLongOr("ticks", 0L) + n);
        if (filterFe + heatFe > 0 && filterOn && heatOn) trip.putLong("powered", trip.getLongOr("powered", 0L) + n);
        if (filterFe + heatFe > 0 && !(filterOn && heatOn)) trip.putLong("unpowered", trip.getLongOr("unpowered", 0L) + n);
        trip.putLong("battery", trip.getLongOr("battery", 0L) + fromBattery);
        trip.putLong("dynamo", trip.getLongOr("dynamo", 0L) + dyn);
        if (!NbtCompat.has(trip, "minQuality", Tag.TAG_FLOAT) || t.waterQuality() < trip.getFloatOr("minQuality", 0f)) trip.putFloat("minQuality", t.waterQuality());
        trip.putFloat("lastTemp", t.waterTemp());
        be.put(TRIP_KEY, trip);
        return new Result(filterOn, heatOn, filterFe + heatFe, fromBattery, dyn);
    }

    private static boolean kindEnabled(String kind) {
        return switch (kind) {
            case "betta" -> Features.enabled(Feature.BETTAS);
            case "foreign" -> Features.enabled(Feature.FOREIGN_CARE);
            default -> true;
        };
    }

    /**
     * The tank arrived: the trip summary goes into its log (minutes on the road, how long the devices had power, the FE
     * the contraption gave), and the players near a tank that travelled with contraption power get the advancement.
     */
    public static void arrived(ServerLevel level, TankBlockEntity c, @Nullable CompoundTag trip) {
        if (trip == null || trip.getLongOr("ticks", 0L) <= 0) return;
        long ticks = trip.getLongOr("ticks", 0L);
        long powered = trip.getLongOr("powered", 0L), unpowered = trip.getLongOr("unpowered", 0L);
        long given = trip.getLongOr("battery", 0L) + trip.getLongOr("dynamo", 0L);
        String minutes = String.valueOf(Math.max(1, Math.round(ticks / 1200.0)));
        String share = powered + unpowered <= 0 ? "-" : Math.round(100.0 * powered / (powered + unpowered)) + "%";
        TankCare.log(level, c, "log.bettacraft.travel.arrived", minutes, share, String.valueOf(given));
        if (given > 0 && powered > 0) org.overengineer.bettacraft.progression.Progress.awardNear(level, c,
                org.overengineer.bettacraft.progression.Progress.POWERED_TRIP, 0);
    }
}
