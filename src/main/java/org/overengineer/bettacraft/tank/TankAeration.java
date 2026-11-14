package org.overengineer.bettacraft.tank;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.AirPumpBlockEntity;
import org.overengineer.bettacraft.equipment.AirStoneBlockEntity;
import org.overengineer.bettacraft.equipment.DeviceSegmentBlock;
import org.overengineer.bettacraft.equipment.FilterHost;
import org.overengineer.bettacraft.tank.interior.InteriorEquipment;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;

/**
 * M23: aeration and oxygen of a tank, every care step.
 * <ul>
 * <li><b>Air:</b> the air pumps next to the tank drive the air stones and sponge filters under water in its interior;
 * one pump drives {@code aeration.outletsPerPump} at full strength (its regulator scales that), more share the air.
 * With {@code aeration.airPumpRequiresEnergy} the pumps need FE (TankPower). Air stones and sponge filters show whether
 * they get air in their block state ({@link DeviceSegmentBlock#ACTIVE}: bubble columns, sound).</li>
 * <li><b>Oxygen</b> (mg/L, {@code features.oxygen}): the water tends to an equilibrium {@code saturation - use / exchange}:
 * saturation falls with warm and salty water; the exchange (per day) comes from the still surface (top blocks / water
 * blocks), the air stones, sponge filters, motor filters and flow pumps (each by its regulator); the animals use oxygen
 * (more in warm water), the plants make it in light and use a little in the dark. Below {@code aeration.oxygenStressBelow}
 * fish, shrimp and crayfish are stressed; bettas and dwarf gouramis (labyrinth fish) only below
 * {@code aeration.labyrinthStressBelow}.</li>
 * <li><b>Fry intake</b> ({@code features.fryIntake}): running motor filters without an intake sponge suck in fry.</li>
 * </ul>
 * The pure model is unit-tested ({@code TankAerationTest}).
 */
public final class TankAeration {
    /** Oxygen classes for the menu: good from 5 mg/L, low below 5, critical below 2. */
    public static final double GOOD_FROM = 5.0, CRITICAL_BELOW = 2.0;

    private TankAeration() {}

    // ------------------------------------------------------------------ pure model

    /** Oxygen saturation in mg/L of fresh water at {@code t} °C, less in salt water (about 80 % at 35 ppt). */
    public static double saturation(double t, double salinityPpt) {
        t = Math.max(0, Math.min(40, t));
        double fresh = 14.62 - 0.3898 * t + 0.006969 * t * t - 0.00005897 * t * t * t;
        return fresh * Math.max(0.5, 1.0 - 0.0057 * Math.max(0, salinityPpt));
    }

    /** Respiration factor of warm water: doubles every 10 °C above 25 °C. */
    public static double warmth(double t) {
        return Math.pow(2.0, (t - 25.0) / 10.0);
    }

    /** Equilibrium oxygen for a saturation, an exchange rate (per day) and a net use (mg/L per day; negative = the plants make more). */
    public static double target(double sat, double exchange, double use) {
        double k = Math.max(0.05, exchange);
        return Math.max(0, Math.min(sat * 1.3, sat - use / k));
    }

    /** Oxygen after {@code days} on its way to {@code target} at the exchange rate (exact solution of dO/dt = k (target - O)). */
    public static double approach(double o, double target, double exchange, double days) {
        double k = Math.max(0.05, exchange);
        return target + (o - target) * Math.exp(-k * Math.max(0, days));
    }

    /** "good", "low" or "critical". */
    public static String state(double o) {
        return o >= GOOD_FROM ? "good" : o >= CRITICAL_BELOW ? "low" : "critical";
    }

    /** Share 0..1 of the air the outlets get: the pumps' capacity over the demand. */
    public static double supply(double capacity, double demand) {
        if (demand <= 0) return 0;
        return Math.max(0, Math.min(1, capacity / demand));
    }

    public static boolean enabled() {
        return CommonConfig.oxygen();
    }

    // ------------------------------------------------------------------ stress

    /** Whether the oxygen of the tank stresses a non-labyrinth fish (shrimp and crayfish use the same limit). */
    public static boolean lowFor(TankData t, boolean labyrinth) {
        if (!enabled() || Float.isNaN(t.oxygen()) || t.waterMb() <= 0) return false;
        double below = labyrinth ? ServerConfig.getOr(ServerConfig.OXYGEN_LABYRINTH_STRESS_BELOW, 1.0)
                : ServerConfig.getOr(ServerConfig.OXYGEN_STRESS_BELOW, 3.5);
        return t.oxygen() < below;
    }

    /** 0..1: how far below the stress limit the oxygen is (for the small animals' death rate). */
    public static double shortage(TankData t) {
        if (!enabled() || Float.isNaN(t.oxygen()) || t.waterMb() <= 0) return 0;
        double below = ServerConfig.getOr(ServerConfig.OXYGEN_STRESS_BELOW, 3.5);
        return below <= 0 || t.oxygen() >= below ? 0 : (below - t.oxygen()) / below;
    }

    /** Labyrinth fish breathe air at the surface: bettas and the dwarf gourami. */
    public static boolean labyrinth(Entity e) {
        return e instanceof BettaEntity || e instanceof TankmateEntity m && m.species() == org.overengineer.bettacraft.tankmates.TankmateSpecies.DWARF_GOURAMI;
    }

    // ------------------------------------------------------------------ the care step

    /** What the air pumps and outlets of a tank do right now. */
    public record Air(int stones, int spongeFilters, double stoneShare, double supply, double capacity) {}

    /** One care step of {@code days}. */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        TankData t = c.getTank();
        InteriorRegion r = InteriorRegions.get(level.getServer()).get(t.regionId());
        InteriorEquipment eq = r != null && r.built() ? InteriorManager.equipment(level.getServer(), r) : null;
        double waterY = r == null ? 0 : TankCare.waterLocalHeight(t, r.scale());
        Air air = air(level, c, eq, waterY);
        updateOutlets(level, c, r, eq, waterY, air);
        if (enabled() && t.waterMb() > 0) oxygen(level, c, eq, waterY, air, days);
        if (CommonConfig.fryIntake() && days > 0) fryIntake(level, c, eq, waterY, days);
    }

    /** The pumps' air for the stones and sponge filters under water. */
    public static Air air(ServerLevel level, TankBlockEntity c, InteriorEquipment eq, double waterY) {
        if (eq == null || !CommonConfig.aeration()) return new Air(0, 0, 0, 0, 0);
        int stones = 0, sponges = 0;
        double stoneShare = 0;
        for (long l : eq.airStones()) {
            if (BlockPos.getY(l) >= waterY) continue;
            stones++;
            stoneShare += eq.shareOf(l);
        }
        if (CommonConfig.moreInternalFilters()) for (long l : eq.spongeFilters()) if (BlockPos.getY(l) < waterY) sponges++;
        double capacity = 0;
        if (c.airPowered) {
            int per = ServerConfig.getOr(ServerConfig.AIR_OUTLETS_PER_PUMP, 4);
            for (AirPumpBlockEntity p : TankEquipment.airPumps(level, c)) capacity += per * p.powerShare();
        }
        return new Air(stones, sponges, stoneShare, supply(capacity, stoneShare + sponges), capacity);
    }

    /** Outlets the pumps need FE for (TankPower): every stone by its regulator and every sponge filter, times the pumps' regulators. */
    public static double airDemand(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        InteriorRegion r = InteriorRegions.get(level.getServer()).get(t.regionId());
        if (r == null || !r.built() || !CommonConfig.aeration()) return 0;
        List<AirPumpBlockEntity> pumps = TankEquipment.airPumps(level, c);
        if (pumps.isEmpty()) return 0;
        InteriorEquipment eq = InteriorManager.equipment(level.getServer(), r);
        double waterY = TankCare.waterLocalHeight(t, r.scale());
        double outlets = 0;
        for (long l : eq.airStones()) if (BlockPos.getY(l) < waterY) outlets += eq.shareOf(l);
        if (CommonConfig.moreInternalFilters()) for (long l : eq.spongeFilters()) if (BlockPos.getY(l) < waterY) outlets += 1;
        double pumpShare = 0;
        for (AirPumpBlockEntity p : pumps) pumpShare += p.powerShare();
        return Math.min(outlets, pumpShare * ServerConfig.getOr(ServerConfig.AIR_OUTLETS_PER_PUMP, 4));
    }

    /** Sets the pumps' state and the ACTIVE state of the stones and sponge filters (only where it changes). */
    private static void updateOutlets(ServerLevel level, TankBlockEntity c, InteriorRegion r, InteriorEquipment eq, double waterY, Air air) {
        List<AirPumpBlockEntity> pumps = TankEquipment.airPumps(level, c);
        int outlets = air.stones() + air.spongeFilters();
        int driving = 0;
        for (AirPumpBlockEntity p : pumps) if (air.supply() > 0 && p.powerShare() > 0) driving++;
        for (AirPumpBlockEntity p : pumps) {
            boolean runs = air.supply() > 0 && p.powerShare() > 0;
            p.setDriven(runs ? Math.max(1, (int) Math.ceil(outlets / (double) Math.max(1, driving))) : 0);
        }
        c.airOutlets = outlets;
        c.airDriven = air.supply() > 0 ? outlets : 0;
        if (r == null || eq == null) return;
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        if (interior == null) return;
        float surface = (float) (org.overengineer.bettacraft.tank.interior.InteriorRegion.BASE_Y + waterY);
        for (long l : eq.airStones()) {
            BlockPos p = r.toWorld(BlockPos.getX(l), BlockPos.getY(l), BlockPos.getZ(l));
            if (!interior.isLoaded(p)) continue;
            BlockState s = interior.getBlockState(p);
            if (!s.hasProperty(DeviceSegmentBlock.ACTIVE)) continue;
            boolean on = air.supply() > 0 && BlockPos.getY(l) < waterY && org.overengineer.bettacraft.equipment.Regulator.share(s) > 0;
            if (s.getValue(DeviceSegmentBlock.ACTIVE) != on) interior.setBlock(p, s.setValue(DeviceSegmentBlock.ACTIVE, on), Block.UPDATE_CLIENTS);
            if (on && interior.getBlockEntity(p) instanceof AirStoneBlockEntity be) be.setSurfaceY(surface);
        }
        for (long l : eq.spongeFilters()) {
            BlockPos p = r.toWorld(BlockPos.getX(l), BlockPos.getY(l), BlockPos.getZ(l));
            if (!interior.isLoaded(p)) continue;
            BlockState s = interior.getBlockState(p);
            if (!s.hasProperty(DeviceSegmentBlock.ACTIVE)) continue;
            boolean on = air.supply() > 0 && BlockPos.getY(l) < waterY && CommonConfig.moreInternalFilters();
            if (s.getValue(DeviceSegmentBlock.ACTIVE) != on) interior.setBlock(p, s.setValue(DeviceSegmentBlock.ACTIVE, on), Block.UPDATE_CLIENTS);
        }
    }

    /** Sponge filters under water that get air: their number times the supply (each one cleans and aerates by it). */
    public static double runningSpongeFilters(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        InteriorRegion r = InteriorRegions.get(level.getServer()).get(t.regionId());
        if (r == null || !r.built() || !CommonConfig.moreInternalFilters()) return 0;
        InteriorEquipment eq = InteriorManager.equipment(level.getServer(), r);
        if (eq.spongeFilters().isEmpty()) return 0;
        double waterY = TankCare.waterLocalHeight(t, r.scale());
        Air air = air(level, c, eq, waterY);
        return air.spongeFilters() * air.supply();
    }

    /** Oxygen exchange per day of the whole tank (surface and devices). */
    public static double exchange(ServerLevel level, TankBlockEntity c, InteriorEquipment eq, double waterY, Air air) {
        TankData t = c.getTank();
        double wb = Math.max(1.0, t.waterBlocks());
        int tops = TankWater.openTop(level, t)[1];
        double k = ServerConfig.getOr(ServerConfig.OXYGEN_SURFACE, 2.0) * Math.max(1, tops) / wb;
        double devices = air.stoneShare() * air.supply() * ServerConfig.getOr(ServerConfig.OXYGEN_AIR_STONE, 30.0)
                + air.spongeFilters() * air.supply() * ServerConfig.getOr(ServerConfig.OXYGEN_SPONGE_FILTER, 15.0);
        if (c.filterPowered) {
            double motor = 0;
            for (FilterHost f : TankEquipment.filters(level, c)) motor += f.powerShare();
            if (eq != null) motor += eq.shareBelow(eq.filters(), waterY) + (CommonConfig.moreInternalFilters() ? eq.shareBelow(eq.corners(), waterY) : 0);
            devices += motor * ServerConfig.getOr(ServerConfig.OXYGEN_FILTER, 6.0);
            if (eq != null) devices += eq.shareBelow(eq.pumps(), waterY) * ServerConfig.getOr(ServerConfig.OXYGEN_FLOW_PUMP, 8.0);
        }
        return k + devices / wb;
    }

    /** Net oxygen use in mg/L per day: the animals (warm water more), the plants' respiration minus what they make in light. */
    public static double use(ServerLevel level, TankBlockEntity c, double light) {
        TankData t = c.getTank();
        double wb = Math.max(1.0, t.waterBlocks());
        double animals = TankWater.animals(TankInhabitants.liveInhabitants(level, t), t);
        double use = ServerConfig.getOr(ServerConfig.OXYGEN_PER_ANIMAL, 6.0) * animals / wb * warmth(t.waterTemp());
        double plants = Math.min(1.0, org.overengineer.bettacraft.plants.InteriorPlants.volumeShare(level.getServer(), t) / 0.05);
        double p = ServerConfig.getOr(ServerConfig.OXYGEN_PLANTS, 8.0) * plants;
        double photo = light < 7 ? 0 : p * Math.min(4.0 / 3.0, (light - 6) / 9.0);
        // a little waste rots and uses oxygen too
        double rot = TankWater.qualityEnabled() ? 1.5 * Math.max(0, 1 - t.waterQuality()) : 0;
        return use + rot + 0.25 * p - photo;
    }

    private static void oxygen(ServerLevel level, TankBlockEntity c, InteriorEquipment eq, double waterY, Air air, double days) {
        TankData t = c.getTank();
        double sat = saturation(t.waterTemp(), t.salinity());
        double k = exchange(level, c, eq, waterY, air);
        double light = org.overengineer.bettacraft.tank.plants.TankPlants.strongLight(level, c);
        double use = use(level, c, light);
        double target = target(sat, k, use);
        c.o2Exchange = k;
        c.o2Use = use;
        c.o2Target = target;
        float before = t.oxygen();
        double o = Float.isNaN(before) ? sat : approach(before, target, k, days);
        if (Float.isNaN(before) || Math.abs(o - before) > 1e-4) {
            t.setOxygen((float) o);
            c.setChanged();
            if (!Float.isNaN(before)) {
                String s0 = state(before), s1 = state(o);
                if (!s0.equals(s1) && !s1.equals("good")) {
                    TankCare.log(level, c, s1.equals("critical") ? "log.bettacraft.oxygen_critical" : "log.bettacraft.oxygen_low",
                            String.format(java.util.Locale.ROOT, "%.1f", o));
                }
            }
        }
    }

    /** CO2 share the aeration drives out (0..1), the hook for M25 (CO2 dosing): strong exchange strips CO2. */
    public static double co2Stripping(double exchange) {
        return Math.max(0, Math.min(1, exchange / 3.0));
    }

    // ------------------------------------------------------------------ fry intake

    /** Share of running unprotected motor filters (external, cabinet, internal, corner) by their regulators. */
    public static double unprotectedMotors(ServerLevel level, TankBlockEntity c, InteriorEquipment eq, double waterY) {
        if (!c.filterPowered) return 0;
        double n = 0;
        for (FilterHost f : TankEquipment.filters(level, c)) if (!f.intakeSponge()) n += f.powerShare();
        if (eq != null) {
            for (long l : eq.filters()) if (BlockPos.getY(l) < waterY && !eq.hasIntakeSponge(l)) n += eq.shareOf(l);
            if (CommonConfig.moreInternalFilters()) for (long l : eq.corners()) if (BlockPos.getY(l) < waterY && !eq.hasIntakeSponge(l)) n += eq.shareOf(l);
        }
        return n;
    }

    private static void fryIntake(ServerLevel level, TankBlockEntity c, InteriorEquipment eq, double waterY, double days) {
        double rate = ServerConfig.getOr(ServerConfig.FILTER_FRY_INTAKE, 0.15);
        if (rate <= 0) return;
        TankData t = c.getTank();
        List<Entity> fry = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            if (e instanceof BettaEntity b && b.lifeStage() == LifeStage.FRY) fry.add(e);
            else if (e instanceof TankmateEntity m && m.lifeStage() == LifeStage.FRY) fry.add(e);
        }
        if (fry.isEmpty()) return;
        double motors = unprotectedMotors(level, c, eq, waterY);
        if (motors <= 0) return;
        double p = 1 - Math.exp(-rate * motors * days);
        RandomSource rnd = level.getRandom();
        for (Entity e : fry) {
            if (rnd.nextDouble() >= p) continue;
            TankCare.log(level, c, "log.bettacraft.fry_intake", TankCare.nameArg(e));
            t.inhabitants().remove(e.getUUID());
            c.markTankChanged();
            e.discard();
        }
    }
}
