package org.overengineer.bettacraft.chem;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.config.ServerConfig.WaterChangeDifficulty;
import org.overengineer.bettacraft.filter.MultiblockFilter;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankNitrate;
import org.overengineer.bettacraft.tank.interior.InteriorEquipment;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;

/**
 * M25: the water chemistry of a tank in the game (the pure model is {@link WaterChemistry}). Every care step the TDS
 * changes (food and waste of the animals, limestone, soil) and the pH drifts to the equilibrium of the water and what is
 * in the tank: the substrate of the interior floor (soil lowers, limestone raises), limestone rocks, driftwood, peat in a
 * multiblock filter, almond leaves and the CO2. Water changes mix the values by volume ({@link #mixIn}). With
 * {@code features.waterChemistry.enabled} off the values stand still and no animal minds them.
 * <p>
 * Hooks for the later parts of M25: the CO2 ({@link TankData#co2()}, mg/L) is read here for the pH; the CO2 devices set it
 * (aeration drives it out: {@link WaterChemistry#co2Exchange} with
 * {@link org.overengineer.bettacraft.tank.TankAeration#co2Stripping}). Reverse osmosis water comes in through
 * {@link org.overengineer.bettacraft.tank.TankInteractions#addWater} with {@link org.overengineer.bettacraft.tank.WaterValues#ro}.
 */
public final class TankChemistry {
    private TankChemistry() {}

    /** TDS the remineralising salt aims for when sneaking (the middle of the Caridina's range). */
    public static final float REMINERALISE_TARGET = 150F;

    public static boolean enabled() {
        return Features.enabled(Feature.WATER_CHEMISTRY);
    }

    /**
     * M25 part d: how much a floor block of plain soil ({@code #bettacraft:acid_substrate}: dirt, mud, podzol ...) counts
     * against {@link org.overengineer.bettacraft.registry.ModTags#ACTIVE_SOIL active soil}: half (it still lowers the pH a
     * little, but only active soil keeps the water soft enough for bee shrimp).
     */
    public static final double DIRT_SHARE = 0.5;

    /**
     * What sets the pH of a tank besides the water itself (each 0..1): soil and limestone of the floor (limestone rocks
     * count half), peat in the filters, tannins of almond leaves, driftwood. {@code acid} is the soil share (active soil
     * fully, plain soil {@link #DIRT_SHARE}); {@code activeSoil} the share of the floor covered with active soil.
     */
    public record Drivers(double acid, double alkaline, double peat, double tannin, double wood, double activeSoil) {
        public static final Drivers NONE = new Drivers(0, 0, 0, 0, 0, 0);
    }

    /** Pure: the drivers from the counts of the interior (floor blocks, rock and wood blocks at the interior scale). */
    public static Drivers drivers(int floorBlocks, int activeSoil, int acidFloor, int limeFloor, int limeRocks, int driftwood, int scale,
                                  double waterBlocks, double peatBlocks, double tannin) {
        double floor = Math.max(1, floorBlocks);
        double volume = Math.pow(Math.max(1, scale), 3) * Math.max(1.0, waterBlocks);
        // rocks and wood: a tenth of the water volume counts fully
        double rocks = Math.min(1.0, limeRocks * 10.0 / volume);
        double active = Math.min(1.0, activeSoil / floor);
        double acid = Math.min(1.0, (activeSoil + DIRT_SHARE * acidFloor) / floor);
        double alkaline = Math.min(1.0, limeFloor / floor + 0.5 * rocks);
        double wood = Math.min(1.0, driftwood * 10.0 / volume);
        double peat = Math.min(1.0, Math.max(0, peatBlocks) / Math.max(1.0, waterBlocks));
        return new Drivers(acid, alkaline, peat, WaterChemistry.clamp01(tannin), wood, active);
    }

    /** The drivers of this tank now (the counts of its interior, the peat of its multiblock filters, the leaves). */
    public static Drivers drivers(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        InteriorRegion r = InteriorRegions.get(level.getServer()).get(t.regionId());
        InteriorEquipment eq = r != null && r.built() ? InteriorManager.equipment(level.getServer(), r) : null;
        double peat = MultiblockFilter.peat(level, c);
        if (eq == null) return drivers(0, 0, 0, 0, 0, 0, 16, t.waterBlocks(), peat, t.tannin());
        return drivers(eq.floorBlocks(), eq.activeSoil(), eq.acidFloor(), eq.limeFloor(), eq.limeRocks(), eq.driftwood(), r.scale(), t.waterBlocks(), peat, t.tannin());
    }

    public static WaterChemistry.Inputs inputs(TankData t, Drivers d) {
        // M25 part c: the alkalinity of the tank buffers salt water (with features.corals.reefDosing off: that of sea water)
        double kh = org.overengineer.bettacraft.reef.ReefDosing.enabled() ? t.kh() : WaterChemistry.SEA_KH;
        return new WaterChemistry.Inputs(t.tds(), t.salinity(), kh, d.acid(), d.alkaline(), d.peat(), d.tannin(), d.wood(), t.co2());
    }

    /** The pH this tank drifts to. */
    public static double phTarget(TankData t, Drivers d) {
        return WaterChemistry.phTarget(inputs(t, d));
    }

    /** ppm per day the TDS changes in this tank now (no waste without water changes: {@code waterchange.difficulty} OFF). */
    public static double tdsPerDay(ServerLevel level, TankBlockEntity c, Drivers d) {
        TankData t = c.getTank();
        if (!enabled() || t.waterMb() <= 0) return 0;
        double waste = TankNitrate.difficulty() == WaterChangeDifficulty.OFF ? 0 : ServerConfig.getOr(ServerConfig.CHEM_TDS_PER_DAY, WaterChemistry.WASTE_TDS_PER_DAY);
        return WaterChemistry.tdsPerDay(t.tds(), TankNitrate.load(level, t), waste, d.acid(), d.alkaline());
    }

    public static WaterChemistry.Hardness hardness(TankData t, Drivers d) {
        return WaterChemistry.hardness(t.tds(), d.acid(), d.alkaline(), t.salinity() >= Salinity.BRACKISH_FROM);
    }

    /** One care step: the TDS changes, the pH drifts, the tannins fade. */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        TankData t = c.getTank();
        if (!enabled() || days <= 0 || t.waterMb() <= 0) return;
        Drivers d = drivers(level, c);
        float tds = t.tds(), ph = t.ph(), tannin = t.tannin();
        // M28: leaves and alder cones on the floor give off tannins
        double litter = litterPerDay(level, c);
        if (litter > 0) t.setTannin((float) Math.min(1.0, t.tannin() + litter * days));
        step(t, d, tdsPerDay(level, c, d), days);
        if (Math.abs(tds - t.tds()) > 1e-4 || Math.abs(ph - t.ph()) > 1e-5 || tannin != t.tannin()) c.setChanged();
    }

    /** M28: tannins per day of the leaf litter in the interior of this tank ({@code features.decor.effects}). */
    public static double litterPerDay(ServerLevel level, TankBlockEntity c) {
        if (!Features.enabled(Feature.DECOR_EFFECTS)) return 0;
        TankData t = c.getTank();
        InteriorRegion r = InteriorRegions.get(level.getServer()).get(t.regionId());
        if (r == null || !r.built()) return 0;
        InteriorEquipment eq = InteriorManager.equipment(level.getServer(), r);
        return WaterChemistry.litterTanninPerDay(eq.leafLitter(), eq.alderCones(), t.waterMb(), ServerConfig.getOr(ServerConfig.CHEM_LEAF_TANNIN, 1.0),
                ServerConfig.getOr(ServerConfig.CHEM_LEAF_HALF_LIFE, 7.0));
    }

    /** Pure step of the values of {@code t} (also for the GameTests). */
    public static void step(TankData t, Drivers d, double tdsPerDay, double days) {
        t.setTds(WaterChemistry.stepTds(t.tds(), tdsPerDay, days));
        t.setPh(WaterChemistry.approachPh(t.ph(), phTarget(t, d), days));
        t.setTannin(WaterChemistry.fadeTannin(t.tannin(), days, ServerConfig.getOr(ServerConfig.CHEM_LEAF_HALF_LIFE, 7.0)));
    }

    /** {@code added} mB of water with these values mix into the {@code before} mB of the tank (tannins dilute, CO2 goes to the air's). */
    public static void mixIn(TankData t, long before, long added, float tds, float ph) {
        if (added <= 0) return;
        t.setTds(WaterChemistry.mix(t.tds(), before, tds, added));
        t.setPh(WaterChemistry.mixPh(t.ph(), before, ph, added));
        t.setTannin(WaterChemistry.mix(t.tannin(), before, 0F, added));
        t.setCo2(WaterChemistry.mix(t.co2(), before, WaterChemistry.CO2_AIR, added));
    }

    // ------------------------------------------------------------------ the animals

    /** How far the water of {@code t} is out of the range (0 when the feature is off or the tank is dry). */
    public static double distance(TankData t, @Nullable ChemTolerance.Range r) {
        if (r == null || t.waterMb() <= 0) return 0;
        double d = enabled() ? r.distance(t.ph(), t.tds()) : 0;
        // M25 part b: too much CO2 counts the same way (features.co2.enabled)
        return Math.max(d, org.overengineer.bettacraft.co2.TankCo2.distance(t, ChemTolerance.co2Tolerance(r)));
    }

    /** M25 part b: too much CO2 is what is wrong for an animal with this range (the cause of death). */
    public static boolean co2Bad(TankData t, @Nullable ChemTolerance.Range r) {
        return r != null && org.overengineer.bettacraft.co2.TankCo2.distance(t, ChemTolerance.co2Tolerance(r)) >= ChemTolerance.BAD_FROM;
    }

    public static ChemTolerance.Fit fit(TankData t, @Nullable ChemTolerance.Range r) {
        return ChemTolerance.fit(distance(t, r));
    }

    /** The water is bad for an animal with this range (sick, in the end dead). */
    public static boolean bad(TankData t, @Nullable ChemTolerance.Range r) {
        return fit(t, r) == ChemTolerance.Fit.BAD;
    }

    /** The water is not quite right for an animal with this range (stressed or bad). */
    public static boolean off(TankData t, @Nullable ChemTolerance.Range r) {
        return fit(t, r) != ChemTolerance.Fit.OK;
    }
}
