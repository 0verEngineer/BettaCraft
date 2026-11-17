package org.overengineer.bettacraft.tank.algae;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.plants.TankPlants;

/**
 * Algae on the glass of a tank (M14) on the server: growth with light, waste and time, grazing by the snails and shrimp
 * (their food), cleaning with the algae scraper, and the sync of the visible stages to the clients (with the tank view,
 * only when a stage changes).
 */
public final class TankAlgae {
    private TankAlgae() {}

    public static double speed() {
        return ServerConfig.getOr(ServerConfig.ALGAE_GROWTH_SPEED, 0.25);
    }

    /** Wet glass faces (the layers up to the water surface). */
    public static List<AlgaeSim.Face> wetFaces(TankData t) {
        if (t.waterMb() <= 0) return List.of();
        return AlgaeSim.faces(t.members(), (int) Math.floor(t.waterSurfaceY() - 1e-4));
    }

    /** Algae growth per face and day in this tank now. */
    public static double growthPerDay(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        double strong = TankPlants.strongLight(level, c);
        // M23: strong LEDs without CO2 and fertiliser (M25) grow more algae
        return AlgaeSim.growthPerDay(speed(), (int) Math.round(Math.min(15, strong)), t.waterQuality(), TankWater.qualityEnabled(),
                org.overengineer.bettacraft.plants.InteriorPlants.volumeShare(level.getServer(), t), TankPlants.coverage(t))
                * org.overengineer.bettacraft.tank.TankLighting.algaeBoost(strong, org.overengineer.bettacraft.co2.TankCo2.algaeBalance(t))
                * org.overengineer.bettacraft.tank.TankNitrate.algaeFactor(t) // M24: nitrate feeds the algae
                * org.overengineer.bettacraft.fertilizer.Fertilizer.algae(t) // M37: too much fertilizer feeds them too
                * org.overengineer.bettacraft.equipment.Sump.algaeFactor(level, c); // M49: the Chaetomorpha of a refugium takes their nutrients
    }

    /** Faces the cleaning crew grazes clean per day (snails best, nerites most, shrimp a little; young less). */
    public static double grazingPerDay(TankData t) {
        double s = 0;
        for (Critter k : t.critters()) if (k.alive()) s += k.species.algaePerDay() * (k.adult() ? 1.0 : 0.4);
        // M19: otocinclus and bristlenoses (set by the tankmates' step, already with the cleaner strength)
        return s * ServerConfig.getOr(ServerConfig.ALGAE_CLEANER_STRENGTH, 1.0) + t.fishGrazing();
    }

    /** Mean algae level of the wet glass, 0..1. */
    public static double mean(TankData t) {
        return AlgaeSim.mean(t.algae(), wetFaces(t));
    }

    /** Care step: growth and grazing (the grazed algae feed the animals), the log, the sync of the stages. */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        if (!org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.ALGAE)) return; // M33 part b: the algae rest (no growth, no grazing)
        TankData t = c.getTank();
        List<AlgaeSim.Face> faces = wetFaces(t);
        if (faces.isEmpty()) {
            syncIfChanged(c);
            return;
        }
        double before = AlgaeSim.mean(t.algae(), faces);
        double eaten = AlgaeSim.step(t.algae(), faces, growthPerDay(level, c), grazingPerDay(t), days);
        if (eaten > 0) TankCritters.addFood(t, eaten * TankCritters.FOOD_PER_ALGAE);
        double after = AlgaeSim.mean(t.algae(), faces);
        if (before < 0.5 && after >= 0.5) TankCare.log(level, c, "log.bettacraft.algae_growing", String.valueOf(Math.round(after * 100)));
        c.setChanged();
        syncIfChanged(c);
    }

    /** Sends the tank view when a visible stage changed. */
    public static void syncIfChanged(TankBlockEntity c) {
        long sig = signature(c.getTank());
        if (sig != c.algaeSignature) {
            boolean first = c.algaeSignature == Long.MIN_VALUE;
            c.algaeSignature = sig;
            if (!first || sig != 0) c.markTankChanged();
        }
    }

    private static long signature(TankData t) {
        long h = 0;
        for (var e : t.algae().long2ObjectEntrySet()) {
            float[] v = e.getValue();
            for (int d = 0; d < 4; d++) {
                int st = AlgaeSim.stage(v[d]);
                if (st > 0) h += (e.getLongKey() * 31 + d) * 0x9E3779B97F4A7C15L * st;
            }
        }
        return h;
    }

    /**
     * The algae scraper on the face {@code face} of tank block {@code pos} (seen from outside): cleans that glass face,
     * with {@code wholeSide} every face of that side of the tank. Returns the faces cleaned (that had algae).
     */
    public static int scrape(ServerLevel level, TankBlockEntity c, BlockPos pos, Direction face, boolean wholeSide) {
        TankData t = c.getTank();
        if (!face.getAxis().isHorizontal()) return 0;
        int dir = face.get2DDataValue();
        int n = 0;
        if (wholeSide) {
            for (long l : t.members()) {
                int x = BlockPos.getX(l), y = BlockPos.getY(l), z = BlockPos.getZ(l);
                if (t.contains(BlockPos.asLong(x + AlgaeSim.stepX(dir), y, z + AlgaeSim.stepZ(dir)))) continue;
                if (AlgaeSim.get(t.algae(), l, dir) > 0) {
                    AlgaeSim.set(t.algae(), l, dir, 0);
                    n++;
                }
            }
        } else if (t.contains(pos) && !t.contains(pos.relative(face)) && AlgaeSim.get(t.algae(), pos.asLong(), dir) > 0) {
            AlgaeSim.set(t.algae(), pos.asLong(), dir, 0);
            n++;
        }
        if (n > 0) {
            c.setChanged();
            syncIfChanged(c);
        }
        return n;
    }
}
