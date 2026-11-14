package org.overengineer.bettacraft.tank;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.equipment.FeederBlock;
import org.overengineer.bettacraft.equipment.TankLidBlock;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;

/**
 * Water care of a tank controller (M9): water quality ({@link WaterQuality}: waste of the animals and of rotting food,
 * filters, water changes) and evaporation from uncovered top blocks. Called from {@link TankCare#serverTick}.
 */
public final class TankWater {
    private TankWater() {}

    public static boolean qualityEnabled() {
        return ServerConfig.getOr(ServerConfig.WATER_QUALITY_ENABLED, true);
    }

    public static boolean evaporationEnabled() {
        return ServerConfig.getOr(ServerConfig.EVAPORATION_ENABLED, true);
    }

    public static WaterQuality.Params params() {
        return new WaterQuality.Params(
                ServerConfig.getOr(ServerConfig.WATER_QUALITY_BASE_DECLINE, 0.01),
                ServerConfig.getOr(ServerConfig.WATER_QUALITY_PER_ANIMAL, 0.04),
                ServerConfig.getOr(ServerConfig.WATER_QUALITY_PER_OVERFED, 0.01),
                ServerConfig.getOr(ServerConfig.WATER_QUALITY_FILTER_EFFICIENCY, 0.8),
                ServerConfig.getOr(ServerConfig.WATER_QUALITY_BAD_BELOW, 0.35));
    }

    /** Whether the water of this tank stresses its bettas. */
    public static boolean isBad(TankData tank) {
        // M24: very much nitrate (months without a water change) is bad water too
        if (TankNitrate.lethal(tank)) return true;
        return qualityEnabled() && WaterQuality.isBad(tank.waterQuality(), params());
    }

    public static WaterQuality.State state(TankData tank) {
        return WaterQuality.state(tank.waterQuality(), params());
    }

    // ------------------------------------------------------------------ filters and animals

    /** Internal filters standing under water in the interior. */
    public static int internalFilters(ServerLevel level, TankData tank) {
        var r = InteriorRegions.get(level.getServer()).get(tank.regionId());
        if (r == null || !r.built()) return 0;
        var eq = InteriorManager.equipment(level.getServer(), r);
        return eq.filters().isEmpty() ? 0 : eq.filtersBelow(TankCare.waterLocalHeight(tank, r.scale()));
    }

    /** M23: corner filters standing under water in the interior. */
    public static int cornerFilters(ServerLevel level, TankData tank) {
        if (!org.overengineer.bettacraft.config.CommonConfig.moreInternalFilters()) return 0;
        var r = InteriorRegions.get(level.getServer()).get(tank.regionId());
        if (r == null || !r.built()) return 0;
        var eq = InteriorManager.equipment(level.getServer(), r);
        double y = TankCare.waterLocalHeight(tank, r.scale());
        int n = 0;
        for (long l : eq.corners()) if (net.minecraft.core.BlockPos.getY(l) < y) n++;
        return n;
    }

    /** M23: regulator shares {internal filters, corner filters, flow pumps} under water. */
    public static double[] interiorShares(ServerLevel level, TankData tank) {
        var r = InteriorRegions.get(level.getServer()).get(tank.regionId());
        if (r == null || !r.built()) return new double[3];
        var eq = InteriorManager.equipment(level.getServer(), r);
        double y = TankCare.waterLocalHeight(tank, r.scale());
        return new double[]{eq.shareBelow(eq.filters(), y),
                org.overengineer.bettacraft.config.CommonConfig.moreInternalFilters() ? eq.shareBelow(eq.corners(), y) : 0, eq.shareBelow(eq.pumps(), y)};
    }

    /** M23: the FE-using devices that run with the filters, weighted by their regulators (external, internal, corner, skimmers, flow pumps). */
    public static double filterLoad(ServerLevel level, TankBlockEntity c) {
        double n = 0;
        for (var f : TankEquipment.filters(level, c)) n += f.powerShare();
        double[] in = interiorShares(level, c.getTank());
        return n + in[0] + in[1] + in[2] + org.overengineer.bettacraft.reef.TankReef.skimmerCount(level, c);
    }

    /** All filters that work in the water: external filters (blocks and cabinets) and internal filters under water. */
    public static int filterCount(ServerLevel level, TankBlockEntity c) {
        // M20: protein skimmers and flow pumps run on the same power as the filters
        return TankEquipment.filters(level, c).size() + internalFilters(level, c.getTank())
                + (int) org.overengineer.bettacraft.filter.MultiblockFilter.serving(level, c)[0] // M24
                + org.overengineer.bettacraft.reef.TankReef.skimmerCount(level, c) + org.overengineer.bettacraft.reef.TankReef.pumps(level, c.getTank());
    }

    /** Water blocks the filters keep clean (0 without power when filters need it). */
    public static double filterBlocks(ServerLevel level, TankBlockEntity c) {
        // M20: live rock cleans salt water without power
        double rock = org.overengineer.bettacraft.reef.TankReef.liveRockBlocks(level, c.getTank());
        // M23: the sponge filters run on the air pump (its own FE), by the air they get
        double sponge = TankAeration.runningSpongeFilters(level, c);
        // M24: the multiblock filters (their pump head has its own power)
        double multi = org.overengineer.bettacraft.filter.MultiblockFilter.serving(level, c)[1];
        if (!c.filterPowered) return rock + multi + sponge * (double) ServerConfig.getOr(ServerConfig.FILTER_SPONGE_BLOCKS, 12);
        // M23: every filter by its power regulator, the corner filters too
        double ext = 0;
        for (var f : TankEquipment.filters(level, c)) ext += f.powerShare();
        double[] in = interiorShares(level, c.getTank());
        return ext * (double) ServerConfig.getOr(ServerConfig.WATER_QUALITY_EXTERNAL_FILTER_BLOCKS, 32)
                + in[0] * (double) ServerConfig.getOr(ServerConfig.WATER_QUALITY_INTERNAL_FILTER_BLOCKS, 16)
                + in[1] * (double) ServerConfig.getOr(ServerConfig.FILTER_CORNER_BLOCKS, 20)
                + sponge * (double) ServerConfig.getOr(ServerConfig.FILTER_SPONGE_BLOCKS, 12)
                // M20: in salt water the protein skimmers and the live rock clean too
                + org.overengineer.bettacraft.reef.TankReef.skimmerBlocks(level, c) + rock + multi;
    }

    /** Bioload: adult animals count 1, young bettas a quarter. */
    public static double animals(List<Entity> live) {
        double n = 0;
        for (Entity e : live) {
            if (e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity m) {
                // M19: by the species' size (a neon a third of a betta), the young a quarter of that
                n += m.species().stock() * (m.isAdult() ? 1.0 : 0.25);
                continue;
            }
            n += e instanceof BettaEntity b && !b.isAdult() ? 0.25 : 1.0;
        }
        return n;
    }

    /** M14: with the bioload of the shrimp, crayfish and snails (stocking.*PerFish of them count as one fish). */
    public static double animals(List<Entity> live, TankData tank) {
        return animals(live) + org.overengineer.bettacraft.critters.TankCritters.bioload(tank);
    }

    // ------------------------------------------------------------------ cover / evaporation

    /** Whether something on top of a tank block keeps the water from evaporating: a lid, the feeder, a light or a full block. */
    public static boolean covers(Level level, BlockPos above) {
        BlockState s = level.getBlockState(above);
        if (s.getBlock() instanceof TankLidBlock || s.getBlock() instanceof FeederBlock || s.getBlock() instanceof TankLightBlock) return true;
        return !s.isAir() && !(s.getBlock() instanceof TankBlock) && s.isFaceSturdy(level, above, Direction.DOWN);
    }

    /**
     * {open, total} top blocks of the tank (loaded ones only). M10: only tops above the water count (for a free shape a
     * closed-off step that is dry does not evaporate); for a cuboid that is every top block.
     */
    public static int[] openTop(Level level, TankData tank) {
        int open = 0, total = 0;
        int surfaceLayer = (int) Math.floor(tank.waterSurfaceY() - 1e-6);
        for (BlockPos p : TankShape.exposedTops(tank.members(), surfaceLayer)) {
            if (!level.isLoaded(p)) continue;
            total++;
            if (!covers(level, p)) open++;
        }
        return new int[]{open, total};
    }

    /** mB per day the tank loses right now. */
    public static double evaporationPerDay(Level level, TankData tank) {
        if (!evaporationEnabled() || tank.waterMb() <= 0) return 0;
        return WaterQuality.evaporationPerDay(openTop(level, tank)[0], tank.waterTemp(),
                ServerConfig.getOr(ServerConfig.EVAPORATION_MB_PER_DAY, 20.0));
    }

    // ------------------------------------------------------------------ simulation

    /** Advances water quality and evaporation by {@code days}. */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        if (days <= 0) return;
        TankData tank = c.getTank();
        if (qualityEnabled() && tank.waterMb() > 0) {
            float before = tank.waterQuality();
            float q = WaterQuality.step(before, days, animals(TankInhabitants.liveInhabitants(level, tank), tank), tank.waterBlocks(),
                    filterBlocks(level, c), params());
            if (q != before) {
                tank.setWaterQuality(q);
                c.setChanged();
                WaterQuality.State s0 = WaterQuality.state(before, params()), s1 = WaterQuality.state(q, params());
                if (s1.ordinal() > s0.ordinal() && s1.ordinal() >= WaterQuality.State.POOR.ordinal()) {
                    TankCare.log(level, c, s1 == WaterQuality.State.BAD ? "log.bettacraft.water_bad" : "log.bettacraft.water_poor",
                            String.valueOf(Math.round(q * 100)));
                }
            }
        }
        double perDay = evaporationPerDay(level, tank);
        if (perDay > 0) {
            double carry = tank.evaporationCarry() + perDay * days;
            long whole = (long) Math.floor(carry / TankData.EVAPORATION_BATCH_MB) * (long) TankData.EVAPORATION_BATCH_MB;
            if (whole > 0) {
                long lost = Math.min(whole, tank.waterMb());
                float q = WaterQuality.concentrate(tank.waterQuality(), tank.waterMb(), lost);
                // M20: the salt and the calcium stay behind
                float salt = Salinity.concentrate(tank.salinity(), tank.waterMb(), lost, Salinity.MAX);
                float ca = Salinity.concentrate(tank.calcium(), tank.waterMb(), lost, 2000F);
                float no3 = TankNitrate.concentrate(tank.nitrate(), tank.waterMb(), lost); // M24
                float tds = org.overengineer.bettacraft.chem.WaterChemistry.concentrate(tank.tds(), tank.waterMb(), lost,
                        org.overengineer.bettacraft.chem.WaterChemistry.TDS_MAX); // M25: the minerals stay behind too
                TankInteractions.changeWater(level, c, -lost);
                tank.setSalinity(salt);
                tank.setCalcium(ca);
                tank.setNitrate(no3);
                tank.setTds(tds);
                if (qualityEnabled()) tank.setWaterQuality(q);
                carry -= whole;
                long min = Math.round(ServerConfig.getOr(ServerConfig.TANK_MIN_FILL_TO_INSERT, 0.5) * tank.capacityMb());
                if (tank.waterMb() < min && tank.waterMb() + lost >= min && !tank.inhabitants().isEmpty()) {
                    TankCare.log(level, c, "log.bettacraft.water_low", String.valueOf(tank.waterMb()));
                }
            }
            tank.setEvaporationCarry(carry);
            c.setChanged();
        }
    }

    /** Fish fed although they were not hungry: the food rots (M9 overfeeding). */
    public static void overfed(ServerLevel level, TankBlockEntity c, int portions) {
        if (portions <= 0) return;
        TankData tank = c.getTank();
        // M14: shrimp, crayfish and snails eat the leftovers first
        portions = org.overengineer.bettacraft.critters.TankCritters.eatLeftovers(tank, portions);
        if (!qualityEnabled() || portions <= 0) return;
        tank.setWaterQuality(WaterQuality.overfed(tank.waterQuality(), portions, tank.waterBlocks(), filterBlocks(level, c), params()));
        c.setChanged();
    }
}
