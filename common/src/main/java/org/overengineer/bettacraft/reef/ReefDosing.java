package org.overengineer.bettacraft.reef;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import net.minecraft.server.level.ServerLevel;
import org.overengineer.bettacraft.chem.WaterChemistry;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;

/**
 * M25 part c: "Balling light" in a salt water tank. Besides the calcium (M20) the water has an alkalinity (KH, dKH),
 * magnesium (mg/L) and trace elements (0..1). Sea water brings 8 dKH, 1300 mg/L and 0.5. Every growth stage of a stony
 * coral takes calcium, the matching alkalinity ({@link WaterChemistry#khForCalcium}) and a little magnesium; the magnesium
 * also falls slowly ({@code reefDosing.magnesiumDropPerDay}) and the trace elements are used up
 * ({@code reefDosing.traceUsePerDay}). Low alkalinity or magnesium slows the stony corals, trace elements above fresh sea
 * water give the corals their strong colours. The dosing pump ({@link DosingPumpBlockEntity}) and water changes with sea
 * water bring them back. With {@code features.corals.reefDosing} off the values stand still and do not matter.
 */
public final class ReefDosing {
    private ReefDosing() {}

    /** The four additives of the dosing pump. */
    public enum Additive {
        CALCIUM("calcium"), ALKALINITY("alkalinity"), MAGNESIUM("magnesium"), TRACE("trace"),
        /** M37: liquid fertilizer (fresh and salt water, {@code features.fertilizer.dosing}). */
        FERTILIZER("fertilizer");

        private final String id;

        Additive(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        /** The rise in one block of water per mL ({@code reefDosing.*PerMl}). */
        public double perMl() {
            return switch (this) {
                case CALCIUM -> ServerConfig.getOr(ServerConfig.DOSING_CA_PER_ML, 2.0);
                case ALKALINITY -> ServerConfig.getOr(ServerConfig.DOSING_KH_PER_ML, 0.07);
                case MAGNESIUM -> ServerConfig.getOr(ServerConfig.DOSING_MG_PER_ML, 4.0);
                case TRACE -> ServerConfig.getOr(ServerConfig.DOSING_TRACE_PER_ML, 0.15);
                case FERTILIZER -> org.overengineer.bettacraft.fertilizer.Fertilizer.portionBlocks() / org.overengineer.bettacraft.fertilizer.Fertilizer.PORTION_MB;
            };
        }
    }

    public static boolean enabled() {
        return Features.enabled(Feature.REEF_DOSING);
    }

    /** Whether the pump doses into this tank: salt water with water in it. */
    public static boolean dosable(TankData t) {
        return t.waterMb() > 0 && Salinity.marine(t.salinity());
    }

    /** Adds {@code ml} of an additive to the tank (shared by its whole volume). */
    public static void dose(TankData t, Additive a, double ml) {
        if (a == Additive.FERTILIZER) { // M37: its own level (1 = 100 %)
            org.overengineer.bettacraft.fertilizer.Fertilizer.doseMl(t, ml);
            return;
        }
        double add = WaterChemistry.dose(ml, a.perMl(), Math.max(1.0, t.systemBlocks()));
        if (add <= 0) return;
        switch (a) {
            case CALCIUM -> t.setCalcium((float) (t.calcium() + add));
            case ALKALINITY -> t.setKh((float) (t.kh() + add));
            case MAGNESIUM -> t.setMagnesium((float) (t.magnesium() + add));
            case TRACE -> t.setTrace((float) (t.trace() + add));
            case FERTILIZER -> {}
        }
    }

    /**
     * Share of the real alkalinity ({@link WaterChemistry#khForCalcium}, 7.14 mg/L calcium per dKH) the corals use: a quarter,
     * so that calcium (420, low from 380) and alkalinity (8 dKH, low from 6) of sea water run low about together.
     */
    public static final double KH_SHARE = 0.25;

    /** A stony coral used {@code calcium} (mg/L in one water block): the matching alkalinity and magnesium go too. */
    public static void useWithCalcium(TankData t, double calcium) {
        if (!enabled() || calcium <= 0 || t.waterMb() <= 0) return;
        double blocks = Math.max(1.0, t.systemBlocks());
        t.setKh((float) (t.kh() - WaterChemistry.khForCalcium(calcium) * KH_SHARE / blocks));
        t.setMagnesium((float) (t.magnesium() - WaterChemistry.magnesiumForCalcium(calcium) / blocks));
    }

    /** The factor of the stony corals (0..1) from the alkalinity and magnesium (1 with the feature off or fresh water). */
    public static double stonyFactor(TankData t) {
        if (!enabled() || !Salinity.marine(t.salinity())) return 1;
        return WaterChemistry.khFactor(t.kh()) * WaterChemistry.magnesiumFactor(t.magnesium());
    }

    /** Whether the trace elements give the corals of this tank their strong colours. */
    public static boolean traceBonus(TankData t) {
        return enabled() && Salinity.marine(t.salinity()) && WaterChemistry.traceBonus(t.trace());
    }

    /** The care step of a salt water tank: the magnesium falls slowly, the trace elements are used up. */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        TankData t = c.getTank();
        if (!enabled() || days <= 0 || !dosable(t)) return;
        step(t, days);
        c.setChanged();
    }

    /** Pure care step (also for the tests). */
    public static void step(TankData t, double days) {
        t.setMagnesium((float) (t.magnesium() - ServerConfig.getOr(ServerConfig.DOSING_MG_DROP, 1.0) * days));
        t.setTrace(WaterChemistry.stepTrace(t.trace(), days, ServerConfig.getOr(ServerConfig.DOSING_TRACE_USE, 0.04)));
    }

    /** "KH 8.0 dKH · Mg 1300 mg/L · trace 50 %" for the menu and the pump. */
    public static String fmtKh(double kh) {
        return String.format(java.util.Locale.ROOT, "%.1f", kh);
    }

    /**
     * The alkalinity, magnesium and trace elements of a tank before its water changes (a water change, top-off or
     * evaporation): after the change they are mixed by volume ({@link #mixIn}) or concentrated ({@link #concentrate}).
     */
    public record Snapshot(float kh, float magnesium, float trace) {
        public static Snapshot of(TankData t) {
            return new Snapshot(t.kh(), t.magnesium(), t.trace());
        }

        /** {@code added} mB of water with {@code addedPpt} of salt (its values follow the salt) mixed into {@code before} mB. */
        public void mixIn(TankData t, long before, float addedPpt, long added) {
            if (added <= 0) return;
            t.setKh(WaterChemistry.mix(kh, before, WaterChemistry.khOf(addedPpt), added));
            t.setMagnesium(WaterChemistry.mix(magnesium, before, WaterChemistry.magnesiumOf(addedPpt), added));
            t.setTrace(WaterChemistry.mix(trace, before, WaterChemistry.traceOf(addedPpt), added));
        }

        /** {@code lost} mB of pure water evaporated from {@code had} mB: the minerals stay behind. */
        public void concentrate(TankData t, long had, long lost) {
            t.setKh(WaterChemistry.concentrate(kh, had, lost, WaterChemistry.KH_MAX));
            t.setMagnesium(WaterChemistry.concentrate(magnesium, had, lost, WaterChemistry.MG_MAX));
            t.setTrace(trace);
        }
    }
}
