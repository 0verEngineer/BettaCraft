package org.overengineer.bettacraft.reef;

import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.tank.TankData;

/**
 * M20: what the corals (and the anemone) of a reef tank need, pure functions (unit-tested). A coral grows like an
 * aquarium plant of M13 ({@link InteriorPlants}), but its rate is light (and its spectrum: actinic blue with white is
 * best) x salinity x flow x temperature, and the stony corals also need calcium and clean water (little nitrate).
 * Its colour shows how it is doing: bleached, pale, healthy, glowing under actinic light.
 */
public final class Corals {
    private Corals() {}

    /** Salinity: nothing below 25 or above 42 ppt, half at 25-30 and 38-42, full at 30-38. */
    public static double salinityFactor(double ppt) {
        if (ppt < 25 || ppt > 42) return 0;
        if (ppt < 30 || ppt > 38) return 0.5;
        return 1;
    }

    /** Freshwater plants: full up to 4 ppt, nothing from 10 ppt. */
    public static double freshFactor(double ppt) {
        if (ppt <= 4) return 1;
        if (ppt >= 10) return 0;
        return (10 - ppt) / 6;
    }

    /** Reef temperature: 23-29 °C full, 21-31 half, else nothing. */
    public static double temperatureFactor(double celsius) {
        if (celsius >= 23 && celsius <= 29) return 1;
        if (celsius >= 21 && celsius <= 31) return 0.5;
        return 0;
    }

    /** Flow: still water a tenth, then up to full at the need of the coral; too strong a third. */
    public static double flowFactor(double flow, PlantSpecies.Flow need) {
        if (flow <= 0) return 0.1;
        if (flow > need.max) return 0.33;
        return Math.max(0.1, Math.min(1, flow / need.need));
    }

    /** Calcium for the stony corals: nothing below 250 mg/L, full from 380. */
    public static double calciumFactor(double calcium) {
        if (calcium <= 250) return 0;
        return Math.min(1, (calcium - 250) / 130);
    }

    /** Clean water: stony corals slow down from 40 mg/L nitrate (quality 0.6) and stop at 60; soft corals only at 65. */
    public static double waterFactor(double quality, boolean stony) {
        if (stony) {
            if (quality < 0.4) return 0;
            return quality < 0.6 ? 0.4 : 1;
        }
        return quality < 0.35 ? 0.5 : 1;
    }

    /** All conditions except the light (0..1): the colour and the menu use it. */
    public static double conditions(InteriorPlants.Env env, PlantSpecies s) {
        double c = salinityFactor(env.salinity()) * env.coralTemp() * flowFactor(env.flow(), s.flow()) * waterFactor(env.quality(), s.stony());
        if (s.stony()) c *= calciumFactor(env.calcium()) * env.reefChem(); // M25 part c: alkalinity, magnesium, pH
        return c;
    }

    /** Growth rate of a coral (0..1) at the given effective light. */
    public static double rate(InteriorPlants.Env env, PlantSpecies s, int effectiveLight) {
        double light = InteriorPlants.lightFactor(effectiveLight, s.light()) * env.spectrum();
        return light * conditions(env, s) * growthSpeed();
    }

    static double growthSpeed() {
        return ServerConfig.getOr(ServerConfig.REEF_CORAL_GROWTH, 1.0);
    }

    /**
     * Colour level (0..3): bleached in the wrong salinity, too warm / cold water or the dark, pale while the conditions
     * are poor, healthy, glowing under actinic light.
     */
    public static int tint(InteriorPlants.Env env, PlantSpecies s) {
        if (salinityFactor(env.salinity()) <= 0 || env.coralTemp() <= 0 || env.light() < 4) return 0;
        double c = salinityFactor(env.salinity()) * env.coralTemp() * waterFactor(env.quality(), s.stony()) * flowFactor(env.flow(), s.flow());
        if (c < 0.3) return 1;
        return env.glow() || env.trace() ? 3 : 2; // M25 part c: trace elements bring out the strong colours too
    }

    /**
     * M20: whether a vanilla coral (block, plant or fan) in a tank interior counts as wet: always, unless
     * {@code saltwater.vanillaCoralsNeedSalt} is on and its tank is not a salt water tank (then it dries out and dies
     * like on land). The client always says yes (it has no tank data; the server decides).
     */
    public static boolean vanillaCoralLives(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        if (level.isClientSide() || !ServerConfig.getOr(ServerConfig.SALT_VANILLA_CORALS, false)) return true;
        var server = level.getServer();
        if (server == null) return true;
        var r = org.overengineer.bettacraft.tank.interior.InteriorManager.regionAt(server, pos);
        if (r == null) return true;
        var c = InteriorPlants.controller(server, r);
        return c == null || c.getTank().salinity() >= 15;
    }

    /** A stony coral grew a stage: it takes calcium out of the water (shared by the whole volume). */
    public static void useCalcium(TankData t) {
        double per = ServerConfig.getOr(ServerConfig.REEF_CALCIUM_PER_STAGE, 40.0);
        if (per <= 0 || t.waterMb() <= 0) return;
        t.setCalcium((float) Math.max(0, t.calcium() - per / Math.max(1.0, t.systemBlocks())));
        ReefDosing.useWithCalcium(t, per); // M25 part c: the alkalinity and a little magnesium go with it
    }
}
