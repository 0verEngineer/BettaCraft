package org.overengineer.bettacraft.plants;

import net.minecraft.server.level.ServerLevel;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;

/**
 * M56: the growth of the terrarium plants ({@link PlantSpecies#land()}) on the land of a terrarium or paludarium, and of the
 * aquarium plants that stand above the water there (their emersed form). Like the aquarium plants by light, CO2 and
 * nutrients, the terrarium plants grow by the light, the humidity and the air temperature of the tank ({@code TankClimate}):
 * <ul>
 *     <li>light: the need of the species ({@link PlantSpecies.Light}), from the tank light, the sky and the basking lamps;</li>
 *     <li>humidity: full within the range of the species, slower outside, nothing 20 % beyond (a cactus in a rainforest rots,
 *     a fern in a desert dries up);</li>
 *     <li>air temperature: full within the range, half up to 3 K outside, nothing beyond.</li>
 * </ul>
 * In an unsuitable climate (all together under a quarter of the growth) a plant <b>languishes</b>: it loses a stage every
 * {@link #WILT_DAYS} days down to its first stage; under water a terrarium plant does the same. Pure functions (unit-tested)
 * plus the conditions of a tank.
 */
public final class LandPlants {
    /** Days of a bad climate after which a plant loses a stage (faster the worse it is). */
    public static final double WILT_DAYS = 3.0;
    /** Below this growth factor a plant languishes. */
    public static final double WILT_BELOW = 0.25;
    /** Humidity (%) beyond the range of a species at which it stops growing. */
    public static final double HUMIDITY_FALLOFF = 20;
    /** The emersed form of an aquarium plant grows fully from this humidity (%) on, at {@link #EMERSED_SPEED}. */
    public static final double EMERSED_HUMIDITY = 75, EMERSED_SPEED = 0.5;
    /** Light a lit basking lamp gives the plants under it (it is a strong lamp). */
    public static final int LAMP_LIGHT = 13;

    private LandPlants() {}

    /**
     * The conditions on the land of a tank.
     *
     * @param light    light level (0..20) at the plants
     * @param humidity relative humidity (%)
     * @param air      air temperature (degrees C)
     * @param climate  the tank has a climate (a terrarium or paludarium); in an aquarium the room counts
     */
    public record Env(int light, double humidity, double air, boolean climate) {}

    public static boolean enabled() {
        return Features.enabled(Feature.TERRARIUM_PLANTS);
    }

    /** Humidity factor 0..1 of a species. */
    public static double humidityFactor(PlantSpecies s, double rh) {
        if (!Double.isFinite(rh)) return 0;
        if (rh < s.humidityMin()) return Math.max(0, 1 - (s.humidityMin() - rh) / HUMIDITY_FALLOFF);
        if (rh > s.humidityMax()) return Math.max(0, 1 - (rh - s.humidityMax()) / HUMIDITY_FALLOFF);
        return 1;
    }

    /** Temperature factor of a species: 1 within its range, 0.5 up to 3 K outside, else 0. */
    public static double temperatureFactor(PlantSpecies s, double air) {
        if (!Double.isFinite(air)) return 0;
        if (air >= s.tempMin() && air <= s.tempMax()) return 1;
        if (air >= s.tempMin() - 3 && air <= s.tempMax() + 3) return 0.5;
        return 0;
    }

    /** Growth factor 0..1 of a terrarium plant (0 under water). */
    public static double rate(PlantSpecies s, Env env, boolean submerged) {
        if (submerged) return 0;
        return InteriorPlants.lightFactor(env.light(), PlantSpecies.Light.need(s.light())) * humidityFactor(s, env.humidity()) * temperatureFactor(s, env.air());
    }

    /**
     * Growth factor of an aquarium plant above the water of a terrarium or paludarium (its emersed form, slower): from
     * {@code EMERSED_HUMIDITY - 20} % humidity on, full at {@link #EMERSED_HUMIDITY} %, times its light and temperature; none
     * without a climate (dry air) and for corals.
     */
    public static double emersedRate(PlantSpecies s, Env env, double temperatureFactor) {
        if (s.marine() || s.land() || !env.climate() || s.rolls()) return 0;
        double h = Math.max(0, Math.min(1, (env.humidity() - (EMERSED_HUMIDITY - HUMIDITY_FALLOFF)) / HUMIDITY_FALLOFF));
        return EMERSED_SPEED * h * InteriorPlants.lightFactor(env.light(), PlantSpecies.Light.need(s.light())) * temperatureFactor;
    }

    /**
     * The languishing of a plant for {@code days} at growth factor {@code factor}: below {@link #WILT_BELOW} the wilt rises
     * (faster the worse), at half the growth and more it heals again. Returns the stages lost (it never goes below stage 1;
     * a cutting just waits).
     */
    public static int wilt(InteriorPlant p, double factor, double days) {
        if (days <= 0) return 0;
        if (factor < WILT_BELOW) {
            p.wilt += days * (1 - factor / WILT_BELOW) / WILT_DAYS;
        } else if (factor >= 0.5 && p.wilt > 0) {
            p.wilt = Math.max(0, p.wilt - days / WILT_DAYS);
        }
        int lost = 0;
        while (p.wilt >= 1) {
            p.wilt -= 1;
            if (p.target > 1) {
                p.target--;
                p.progress = 0;
                lost++;
            }
        }
        if (p.target <= 1) p.wilt = Math.min(p.wilt, 0.99);
        return lost;
    }

    /** Whether a plant languishes now (for the menu). */
    public static boolean languishing(InteriorPlant p) {
        return p.wilt > 0.05;
    }

    /** The conditions on the land of a tank now. */
    public static Env env(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        int light = (int) Math.round(org.overengineer.bettacraft.tank.plants.TankPlants.strongLight(level, c));
        if (Features.enabled(Feature.TERRARIUM_DEVICES) && !org.overengineer.bettacraft.terrarium.TankClimate.litLamps(level, t).isEmpty()) {
            light = Math.max(light, LAMP_LIGHT);
        }
        boolean climate = t.hasClimate();
        double rh = climate ? t.humidity() : org.overengineer.bettacraft.terrarium.TankClimate.roomHumidity(level, t);
        double air = climate ? t.airTemp() : TankCare.ambient(level, t);
        return new Env(light, rh, air, climate);
    }
}
