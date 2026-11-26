package org.overengineer.bettacraft.tank.plants;

import net.minecraft.server.level.ServerLevel;
import org.overengineer.bettacraft.chem.WaterChemistry;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.tank.TankAeration;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankNitrate;

/**
 * M66: what each group of plants of a tank does for the water now (the plants tab): the floating plants, the water plants of the
 * interior, the corals and the terrarium plants. The effects of the whole tank are split by the share each group has in them, so
 * the groups add up to what {@link TankNitrate}, {@link TankAeration} and the CO2 balance really use.
 */
public final class PlantEffects {
    public static final int FLOATING = 0, WATER = 1, CORAL = 2, LAND = 3, GROUPS = 4;
    /** Values per group: water quality per day (share, 0.01 = 1 %), nitrate taken up (mg/L per day), oxygen made (mg/L per day), CO2 taken up (mg/L per day). */
    public static final int QUALITY = 0, NITRATE = 1, OXYGEN = 2, CO2 = 3, VALUES = 4;

    private PlantEffects() {}

    /** The group of an interior plant species. */
    public static int group(PlantSpecies s) {
        return s.land() ? LAND : s.marine() ? CORAL : WATER;
    }

    /** {@code [group * VALUES + value]}; {@code floatingQuality} and {@code interiorQuality} are the tank's water quality gains per day (without / with the plants of other mods). */
    public static double[] compute(ServerLevel level, TankBlockEntity c, double floatingQuality, double interiorQuality, double foreignQuality,
                                   double[] activeBlocks) {
        double[] out = new double[GROUPS * VALUES];
        TankData t = c.getTank();
        out[FLOATING * VALUES + QUALITY] = floatingQuality;
        double own = interiorQuality - foreignQuality;
        double activeW = activeBlocks[WATER], activeC = activeBlocks[CORAL], active = activeW + activeC;
        double shareW = active <= 0 ? 0 : activeW / active, shareC = active <= 0 ? 0 : activeC / active;
        out[WATER * VALUES + QUALITY] = own * shareW + foreignQuality;
        out[CORAL * VALUES + QUALITY] = own * shareC;
        // nitrate: the terms of TankNitrate.plants, scaled by the light factor, share of the uptake they make
        if (TankNitrate.enabled() && t.waterMb() > 0) {
            var server = level.getServer();
            double tf = TankPlants.coverage(t) * 0.5, ti = InteriorPlants.ownShare(server, t) * 4, tfo = InteriorPlants.foreignShare(server, t) * 4;
            int light = TankPlants.light(level, c);
            double lf = (tf > 0 || ti > 0) && light < 7 ? 0.3 : 1.0;
            double sum = tf * lf + ti * lf + tfo;
            if (sum > 0) {
                double factor = Math.min(1.0, sum);
                double taken = TankNitrate.baseRate(TankNitrate.difficulty()) * ServerConfig.getOr(ServerConfig.NITRATE_PLANT_UPTAKE, 1.2) * factor;
                out[FLOATING * VALUES + NITRATE] = taken * tf * lf / sum;
                double interior = taken * ti * lf / sum;
                out[WATER * VALUES + NITRATE] = interior * shareW + taken * tfo / sum;
                out[CORAL * VALUES + NITRATE] = interior * shareC;
            }
        }
        // oxygen and CO2: the plants of the interior (by their share of the active blocks)
        double o2 = Math.max(0, TankAeration.plantNet(level, c));
        double share = Math.min(1.0, InteriorPlants.volumeShare(level.getServer(), t) / 0.05);
        double co2 = 0;
        if (org.overengineer.bettacraft.co2.TankCo2.enabled()) {
            co2 = WaterChemistry.plantUptake(share, TankPlants.strongLight(level, c), t.co2(), ServerConfig.getOr(ServerConfig.CO2_PLANT_UPTAKE, 30.0));
        }
        out[WATER * VALUES + OXYGEN] = o2 * shareW;
        out[CORAL * VALUES + OXYGEN] = o2 * shareC;
        out[WATER * VALUES + CO2] = co2 * shareW;
        out[CORAL * VALUES + CO2] = co2 * shareC;
        return out;
    }
}
