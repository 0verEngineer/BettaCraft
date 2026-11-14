package org.overengineer.bettacraft.co2;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.overengineer.bettacraft.chem.WaterChemistry;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.tank.TankAeration;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWater;

/**
 * M25 part b: the CO2 of a tank (mg/L, {@link TankData#co2()}). Every care step it moves to an equilibrium
 * ({@link WaterChemistry#co2Target}): the sources next to the tank (bio CO2 bottles, gas cylinders: bubbles per second,
 * {@code co2Dosing.co2PerBubble} per water block) and the animals' breath add CO2, the plants take it up in light, the
 * surface and the aeration of M23 drive it out ({@link WaterChemistry#co2Exchange} with the oxygen exchange and
 * {@link TankAeration#co2Stripping}). Without a source (or with {@code features.co2} off) the water goes back to the
 * air's 3 mg/L. The CO2 lowers the pH ({@link org.overengineer.bettacraft.chem.TankChemistry}), makes the plants grow
 * faster ({@link #growth}), holds back the algae of strong light ({@link #algaeBalance}) and too much harms the animals
 * ({@link #distance}).
 */
public final class TankCo2 {
    private TankCo2() {}

    public static boolean enabled() {
        return CommonConfig.co2();
    }

    /** The CO2 sources that serve this tank. */
    public static List<Co2Source> sources(ServerLevel level, TankBlockEntity c) {
        List<Co2Source> out = new ArrayList<>();
        for (BlockPos p : TankEquipment.co2Sources(level, c)) if (level.getBlockEntity(p) instanceof Co2Source s) out.add(s);
        return out;
    }

    /** What a care step works out (mg/L per day for the whole water; exchange per day; equilibrium mg/L). */
    public record Balance(double bubbles, double dose, double breath, double uptake, double exchange, double target) {}

    /** Pure: the balance of a tank with these numbers. */
    public static Balance balance(double bubbles, double waterBlocks, double animals, double temp, double plants, double light, double co2, double o2Exchange) {
        double wb = Math.max(1.0, waterBlocks);
        double dose = bubbles * ServerConfig.getOr(ServerConfig.CO2_PER_BUBBLE, 150.0) / wb;
        double breath = ServerConfig.getOr(ServerConfig.CO2_BREATH, 2.0) * animals / wb * TankAeration.warmth(temp);
        double uptake = WaterChemistry.plantUptake(plants, light, co2, ServerConfig.getOr(ServerConfig.CO2_PLANT_UPTAKE, 30.0));
        double k = WaterChemistry.co2Exchange(o2Exchange, TankAeration.co2Stripping(o2Exchange));
        return new Balance(bubbles, dose, breath, uptake, k, WaterChemistry.co2Target(dose, breath, uptake, k));
    }

    /** One care step (after the aeration, which sets the exchange; before the chemistry, which reads the CO2 for the pH). */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        TankData t = c.getTank();
        List<Co2Source> sources = sources(level, c);
        double bubbles = 0;
        for (Co2Source s : sources) {
            double b = enabled() && t.waterMb() > 0 ? Math.max(0, s.bubbles(level, c)) : 0;
            s.used(level, b, days);
            bubbles += b;
        }
        c.co2Bubbles = bubbles;
        if (t.waterMb() <= 0 || days <= 0) return;
        float before = t.co2();
        double target, k;
        if (!enabled() || sources.isEmpty()) {
            // nothing doses: back to the air's CO2 (the animals' breath alone hardly moves it)
            k = WaterChemistry.co2Exchange(c.o2Exchange, TankAeration.co2Stripping(c.o2Exchange));
            target = WaterChemistry.CO2_AIR;
        } else {
            double animals = TankWater.animals(TankInhabitants.liveInhabitants(level, t), t);
            double plants = Math.min(1.0, org.overengineer.bettacraft.plants.InteriorPlants.volumeShare(level.getServer(), t) / 0.05);
            double light = org.overengineer.bettacraft.tank.plants.TankPlants.strongLight(level, c);
            Balance b = balance(bubbles, t.waterBlocks(), animals, t.waterTemp(), plants, light, before, c.o2Exchange);
            k = b.exchange();
            target = b.target();
        }
        c.co2Target = target;
        if (Math.abs(before - target) < 1e-4) return;
        t.setCo2(WaterChemistry.approachCo2(before, target, k, days));
        if (Math.abs(t.co2() - before) > 1e-4) {
            c.setChanged();
            WaterChemistry.Co2State s0 = state(before), s1 = state(t.co2());
            if (s0 != s1 && (s1 == WaterChemistry.Co2State.HIGH || s1 == WaterChemistry.Co2State.DEADLY)) {
                org.overengineer.bettacraft.tank.TankCare.log(level, c, s1 == WaterChemistry.Co2State.DEADLY ? "log.bettacraft.co2_deadly" : "log.bettacraft.co2_high",
                        String.format(java.util.Locale.ROOT, "%.0f", t.co2()));
            }
        }
    }

    public static double stressFrom() {
        return ServerConfig.getOr(ServerConfig.CO2_STRESS_FROM, 35.0);
    }

    public static double lethalFrom() {
        return Math.max(stressFrom() + 1, ServerConfig.getOr(ServerConfig.CO2_LETHAL_FROM, 50.0));
    }

    public static WaterChemistry.Co2State state(double co2) {
        return WaterChemistry.co2State(co2, stressFrom(), lethalFrom());
    }

    /** Tolerance of the air-breathing labyrinth fish (bettas, gouramis) and of shrimp and crayfish. */
    public static final double LABYRINTH = 1.2, SHRIMP = 0.85;

    /**
     * Pure: how far too much CO2 is "out of the range" of an animal, on the scale of the pH / TDS ranges
     * ({@link org.overengineer.bettacraft.chem.ChemTolerance#BAD_FROM} = bad water): 0 below the stress level (times
     * {@code tolerance}), up to just under bad at the lethal level, bad above it.
     */
    public static double distance(double co2, double stressFrom, double lethalFrom, double tolerance) {
        double s = stressFrom * tolerance, l = Math.max(s + 1, lethalFrom * tolerance);
        if (co2 < s) return 0;
        double bad = org.overengineer.bettacraft.chem.ChemTolerance.BAD_FROM;
        if (co2 < l) return 0.01 + (co2 - s) / (l - s) * (bad - 0.02);
        return bad + (co2 - l) / 10.0;
    }

    /** The CO2 distance of this tank for an animal of that tolerance (0 with features.co2 off or a dry tank). */
    public static double distance(TankData t, double tolerance) {
        if (!enabled() || t.waterMb() <= 0) return 0;
        return distance(t.co2(), stressFrom(), lethalFrom(), tolerance);
    }

    /** Whether too much CO2 is what harms an animal of that tolerance here (for the cause of death). */
    public static boolean harms(TankData t, double tolerance) {
        return distance(t, tolerance) > 1e-6;
    }

    /** Growth factor of a freshwater plant of this light need in this tank (1 without CO2 or with the feature off). */
    public static double growth(TankData t, org.overengineer.bettacraft.plants.PlantSpecies.Light need) {
        if (!enabled()) return 1;
        return WaterChemistry.plantGrowth(t.co2(), Math.min(2, need.ordinal()));
    }

    /** How much CO2 is missing for strong light (1 = none dosed, 0.3 = well dosed): the algae of strong LEDs grow by it. */
    public static double algaeBalance(TankData t) {
        if (!enabled()) return 1;
        return 1 - 0.7 * WaterChemistry.co2Boost(t.co2());
    }

    /** Whether the lights of the tank are on now (its daylight timer, or the daylight without one): the solenoid valve opens. */
    public static boolean lightsOn(ServerLevel level, TankData t) {
        if (CommonConfig.lightTimer() && t.lightTimer()) return org.overengineer.bettacraft.tank.TankLighting.factor(t, level.getDayTime()) > 0;
        if (!level.dimensionType().hasSkyLight() || level.dimensionType().hasFixedTime()) return true;
        int m = org.overengineer.bettacraft.tank.TankLighting.minuteOfDay(level.getDayTime());
        return m >= 6 * 60 && m < 19 * 60; // 6:00 - 19:00 (sunrise to sunset)
    }
}
