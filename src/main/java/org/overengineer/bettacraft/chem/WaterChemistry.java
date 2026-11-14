package org.overengineer.bettacraft.chem;

/**
 * M25: the simplified water chemistry of a tank, pure functions (unit-tested, {@code WaterChemistryTest}).
 * <ul>
 * <li><b>TDS</b> (ppm, dissolved solids besides the sea salt): tap water about 250, reverse osmosis water 0. It rises with
 * the waste of the animals, the food and remineralising salt, limestone dissolves slowly, active soil takes a little;
 * evaporation leaves it behind (minerals above soft water partly settle as lime scale: {@link #evaporateTds}), water
 * changes mix it by volume. Topping off with normal water alone lets it climb for months; with regular partial water
 * changes it settles a little above the value of normal water (M25 part d).</li>
 * <li><b>Hardness</b> is no value of its own: soft / medium / hard from the TDS and the substrate ({@link #hardness}).</li>
 * <li><b>pH</b> (5..9) drifts to an equilibrium ({@link #phTarget}): the buffer of the water (TDS; in salt water the
 * alkalinity), pulled towards 6.2 by active soil and towards 8.2 by limestone, lowered by peat in a filter, almond leaves,
 * driftwood and dissolved CO2 ({@link #co2Shift}).</li>
 * <li><b>CO2</b> (mg/L): air gives about 3; a CO2 source raises it, the surface and the aeration drive it out
 * ({@link #co2Exchange}), plants in light take it up. Plants grow much faster with it ({@link #co2Boost}); too much
 * stresses and kills the animals.</li>
 * <li><b>Salt water</b> ("Balling light"): calcium (M20), alkalinity (KH, dKH) and magnesium, which the stony corals use
 * up, and trace elements for the colours of the corals.</li>
 * </ul>
 */
public final class WaterChemistry {
    private WaterChemistry() {}

    /** Tap water (fresh water without values). */
    public static final float TAP_TDS = 250F, TAP_PH = 7.5F, TAP_KH = 4F, TAP_MG = 10F;
    /** Reverse osmosis water: almost nothing dissolved, unbuffered, slightly acidic. */
    public static final float RO_TDS = 0F, RO_PH = 6.8F;
    /** Natural sea water. */
    public static final float SEA_PH = 8.2F, SEA_KH = 8F, SEA_MG = 1300F, SEA_TRACE = 0.5F;
    /** CO2 of water in balance with the air (mg/L). */
    public static final float CO2_AIR = 3F;
    public static final float PH_MIN = 5F, PH_MAX = 9F;
    public static final float TDS_MAX = 2000F, KH_MAX = 30F, MG_MAX = 2500F, CO2_MAX = 200F;

    /** Soft below this effective hardness, hard from {@link #HARD_FROM}. */
    public static final double SOFT_BELOW = 150, HARD_FROM = 400;

    /** Default TDS (ppm/day) a normally stocked tank gains from food and waste (server config {@code chemistry.tdsPerDay}). */
    public static final double WASTE_TDS_PER_DAY = 0.6;
    /** M25 part d: share of the minerals evaporation leaves behind that settle as lime scale (hard water; none below {@link #SCALE_FROM}). */
    public static final double SCALE_SHARE = 0.6, SCALE_FROM = 100, SCALE_FULL = 250;

    public enum Hardness { SOFT, MEDIUM, HARD }

    // ------------------------------------------------------------------ inputs of the pH

    /**
     * What sets the pH of a tank.
     *
     * @param tds      TDS of the water (ppm)
     * @param salinity salt (ppt): the more sea salt, the more the alkalinity buffers it instead of the TDS (sea water: only it)
     * @param kh       alkalinity (dKH, salt water)
     * @param acid     0..1 share of the floor with active soil (dirt, mud, podzol ...)
     * @param alkaline 0..1 share of the floor with limestone (calcite, dripstone ...), rocks of it count half
     * @param peat     0..1 how much peat filters the water (filter chambers with peat per water block)
     * @param tannin   0..1 tannins of almond leaves in the water
     * @param wood     0..1 driftwood (logs, mangrove roots) in the tank
     * @param co2      dissolved CO2 (mg/L)
     */
    public record Inputs(double tds, double salinity, double kh, double acid, double alkaline, double peat, double tannin, double wood, double co2) {
        public static Inputs plain(double tds, double co2) {
            return new Inputs(tds, 0, SEA_KH, 0, 0, 0, 0, 0, co2);
        }
    }

    /** pH the buffer of fresh water holds without anything else: reverse osmosis water 6.2, tap water 7.5, at most 7.8. */
    public static double basePh(double tds) {
        return 6.2 + 1.3 * Math.min(1.2, Math.max(0, tds) / TAP_TDS);
    }

    /** pH of salt water by its alkalinity (8 dKH: 8.2). */
    public static double marinePh(double kh) {
        return 7.6 + 0.075 * Math.max(0, Math.min(14, kh));
    }

    /** pH shift of dissolved CO2: none at the air's 3 mg/L, -1 at 30 mg/L (one pH unit per tenfold CO2). */
    public static double co2Shift(double co2) {
        return -Math.log10(Math.max(CO2_AIR, co2) / CO2_AIR);
    }

    /** The pH the water drifts to. */
    public static double phTarget(Inputs in) {
        double sea = clamp01((in.salinity() - 2.0) / 33.0);
        double ph = basePh(in.tds()) * (1 - sea) + marinePh(in.kh()) * sea;
        // active soil and limestone buffer the water towards their own pH
        double acid = clamp01(in.acid()) * 0.85, alk = clamp01(in.alkaline()) * 0.85;
        if (acid + alk > 0 && sea < 1) {
            double sum = acid + alk;
            double pull = Math.min(0.85, sum) * (1 - sea);
            double towards = (acid * 6.2 + alk * 8.2) / sum;
            ph = ph + (towards - ph) * pull;
        }
        ph -= 0.6 * clamp01(in.peat()) + 0.4 * clamp01(in.tannin()) + 0.25 * clamp01(in.wood());
        ph += co2Shift(in.co2());
        return clampPh(ph);
    }

    /** pH of tap water at this salinity without anything else (tanks of earlier versions start with it; sea water 8.2). */
    public static float startPh(double salinity) {
        return clampPh(phTarget(new Inputs(TAP_TDS, salinity, SEA_KH, 0, 0, 0, 0, 0, CO2_AIR)));
    }

    /** pH after {@code days} on its way to {@code target} (about a day to get most of the way). */
    public static float approachPh(float ph, double target, double days) {
        double k = 1.5;
        return clampPh(target + (ph - target) * Math.exp(-k * Math.max(0, days)));
    }

    // ------------------------------------------------------------------ TDS and hardness

    /**
     * Change of the TDS per day: waste of the animals ({@code load} = stocking against a normally stocked tank, rate
     * {@code wastePerDay} ppm/day), limestone dissolving (up to 4 ppm/day, less the harder the water already is), active
     * soil taking a little.
     */
    public static double tdsPerDay(double tds, double load, double wastePerDay, double acid, double alkaline) {
        double waste = wastePerDay * Math.max(0, load);
        double lime = 4.0 * clamp01(alkaline) * Math.max(0, 1 - tds / 450.0);
        double soil = 1.5 * clamp01(acid) * Math.max(0, tds) / TAP_TDS;
        return waste + lime - soil;
    }

    /** TDS after {@code days}. */
    public static float stepTds(float tds, double perDay, double days) {
        return clampTds((float) (tds + perDay * Math.max(0, days)));
    }

    /**
     * M25 part d: TDS after {@code lostMb} of pure water evaporated from {@code waterMb}. The minerals stay behind, but in
     * hard water a share of them settles as lime scale (on the glass, the substrate, the plants): none below
     * {@link #SCALE_FROM} ppm, {@link #SCALE_SHARE} from {@link #SCALE_FULL}. That is why regular water changes hold the
     * TDS a little above the normal water's although every top-off brings new minerals.
     */
    public static float evaporateTds(float tds, long waterMb, long lostMb) {
        float conc = concentrate(tds, waterMb, lostMb, TDS_MAX);
        double scale = SCALE_SHARE * clamp01((tds - SCALE_FROM) / (SCALE_FULL - SCALE_FROM));
        return clampTds((float) (tds + (conc - tds) * (1 - scale)));
    }

    /** Effective hardness: the TDS, softened by active soil, hardened by limestone. */
    public static double effectiveHardness(double tds, double acid, double alkaline) {
        return Math.max(0, tds) * (1 - 0.5 * clamp01(acid)) + 200 * clamp01(alkaline);
    }

    public static Hardness hardness(double tds, double acid, double alkaline, boolean salty) {
        if (salty) return Hardness.HARD;
        double h = effectiveHardness(tds, acid, alkaline);
        return h < SOFT_BELOW ? Hardness.SOFT : h < HARD_FROM ? Hardness.MEDIUM : Hardness.HARD;
    }

    /** TDS after dissolving {@code items} portions of remineralising salt (one raises 1000 mB by {@code perBlock} ppm). */
    public static float remineralise(float tds, long waterMb, int items, double perBlock) {
        if (items <= 0 || waterMb <= 0) return tds;
        return clampTds((float) (tds + items * perBlock * 1000.0 / waterMb));
    }

    /** Portions of remineralising salt that bring {@code waterMb} of water from {@code tds} to {@code target}. */
    public static int portionsTo(float tds, long waterMb, double target, double perBlock) {
        if (tds >= target || waterMb <= 0 || perBlock <= 0) return 0;
        return (int) Math.ceil((target - tds) * waterMb / (perBlock * 1000.0) - 1e-6);
    }

    // ------------------------------------------------------------------ CO2

    /**
     * CO2 exchange per day: the surface of the water ({@code o2Exchange}, the oxygen exchange of M23: surface and devices),
     * stronger aeration drives out more ({@code stripping} 0..1, the M23 hook).
     */
    public static double co2Exchange(double o2Exchange, double stripping) {
        return Math.max(0.2, o2Exchange) * (1 + 2 * clamp01(stripping));
    }

    /**
     * Equilibrium CO2 (mg/L) for a dose ({@code dosePerDay} mg/L per day for the whole water), the animals' breath
     * ({@code respiration}) and the plants' uptake (mg/L per day), at an exchange of {@code k} per day.
     */
    public static double co2Target(double dosePerDay, double respiration, double uptake, double k) {
        double net = dosePerDay + respiration - uptake;
        return Math.max(0.5, Math.min(CO2_MAX, CO2_AIR + net / Math.max(0.05, k)));
    }

    /** CO2 after {@code days} on its way to the target (exact solution of dC/dt = k (target - C)). */
    public static float approachCo2(float co2, double target, double k, double days) {
        double v = target + (co2 - target) * Math.exp(-Math.max(0.05, k) * Math.max(0, days));
        return (float) Math.max(0, Math.min(CO2_MAX, v));
    }

    /**
     * Plant uptake of CO2 (mg/L per day): {@code plants} 0..1 how densely planted, {@code light} the light at the surface
     * (nothing below 7), less when there is little CO2 left.
     */
    public static double plantUptake(double plants, double light, double co2, double max) {
        if (light < 7 || plants <= 0) return 0;
        double l = Math.min(1.0, (light - 6) / 9.0);
        return max * clamp01(plants) * l * Math.min(1.0, Math.max(0, co2 - 1) / 10.0);
    }

    /** 0..1: how well CO2 is dosed for the plants (nothing below 5 mg/L, full from 25). */
    public static double co2Boost(double co2) {
        return clamp01((co2 - 5.0) / 20.0);
    }

    /** Growth factor of a plant with CO2: up to x2.5 for strong-light plants, x2 medium, x1.5 low-light plants. */
    public static double plantGrowth(double co2, int lightNeed) {
        double b = co2Boost(co2);
        double max = lightNeed >= 2 ? 1.5 : lightNeed == 1 ? 1.0 : 0.5;
        return 1 + b * max;
    }

    public enum Co2State { LOW, GOOD, HIGH, DEADLY }

    public static Co2State co2State(double co2, double stressFrom, double lethalFrom) {
        if (co2 >= lethalFrom) return Co2State.DEADLY;
        if (co2 >= stressFrom) return Co2State.HIGH;
        if (co2 >= 15) return Co2State.GOOD;
        return Co2State.LOW;
    }

    /**
     * Shock per day of too much CO2 (0 below {@code lethalFrom}): grows with the excess; {@code sensitivity} 1 for fish,
     * less for labyrinth fish (they breathe air), more for shrimp.
     */
    public static double co2Shock(double co2, double lethalFrom, double sensitivity) {
        if (co2 < lethalFrom) return 0;
        return (0.3 + (co2 - lethalFrom) / 15.0) * Math.max(0, sensitivity);
    }

    // ------------------------------------------------------------------ salt water

    /** Alkalinity (dKH) a stony coral uses together with {@code calcium} mg/L (7.14 mg/L calcium per dKH). */
    public static double khForCalcium(double calcium) {
        return calcium / 7.14;
    }

    /** Magnesium a stony coral takes along with the calcium (a little). */
    public static double magnesiumForCalcium(double calcium) {
        return calcium * 0.15;
    }

    /** Stony corals: alkalinity factor (nothing below 3 dKH, half below 5, full from 6). */
    public static double khFactor(double kh) {
        if (kh < 3) return 0;
        if (kh < 5) return 0.5;
        if (kh < 6) return 0.8;
        return 1;
    }

    /** Stony corals: magnesium factor (below 1150 mg/L calcium and alkalinity fall out of the water). */
    public static double magnesiumFactor(double mg) {
        if (mg < 1000) return 0.3;
        if (mg < 1150) return 0.6;
        return 1;
    }

    /** Corals: pH factor (salt water below 7.8 slows the skeletons). */
    public static double reefPhFactor(double ph) {
        if (ph < 7.6) return 0.3;
        if (ph < 7.8) return 0.6;
        return 1;
    }

    /** Trace elements after {@code days}: they are used up (about 4 % of the full level per day). */
    public static float stepTrace(float trace, double days, double perDay) {
        return (float) Math.max(0, Math.min(1, trace - perDay * Math.max(0, days)));
    }

    /**
     * Whether the trace elements give the corals their stronger colours (M25 part c: above fresh sea water's 0.5, so only
     * with dosing).
     */
    public static boolean traceBonus(double trace) {
        return trace >= 0.6;
    }

    /** M25 part c: alkalinity (dKH) of water with {@code ppt} of salt: tap water's, sea water's at 35 ppt, mixed in between. */
    public static float khOf(float ppt) {
        float f = Math.max(0, Math.min(1.5F, ppt / 35F));
        return TAP_KH + (SEA_KH - TAP_KH) * f;
    }

    /** M25 part c: magnesium (mg/L) of water with {@code ppt} of salt. */
    public static float magnesiumOf(float ppt) {
        float f = Math.max(0, Math.min(1.5F, ppt / 35F));
        return TAP_MG + (SEA_MG - TAP_MG) * f;
    }

    /** M25 part c: trace elements (0..1) of water with {@code ppt} of salt (fresh sea water 0.5, tap water none). */
    public static float traceOf(float ppt) {
        return Math.max(0, Math.min(1F, SEA_TRACE * ppt / 35F));
    }

    public static float clampKh(float kh) {
        return Float.isFinite(kh) ? Math.max(0, Math.min(KH_MAX, kh)) : TAP_KH;
    }

    public static float clampMagnesium(float mg) {
        return Float.isFinite(mg) ? Math.max(0, Math.min(MG_MAX, mg)) : TAP_MG;
    }

    public static float clampTrace(float t) {
        return Float.isFinite(t) ? Math.max(0, Math.min(1, t)) : 0;
    }

    /**
     * M25 part c: what {@code ml} of a dosing channel adds to {@code waterBlocks} of water ({@code perMlBlock}: the rise in
     * one block of water per mL).
     */
    public static double dose(double ml, double perMlBlock, double waterBlocks) {
        if (ml <= 0 || waterBlocks <= 0) return 0;
        return ml * perMlBlock / waterBlocks;
    }

    // ------------------------------------------------------------------ almond leaves

    /** Tannins (0..1) after adding {@code leaves} leaves ({@code perLeaf} water blocks each) to {@code waterMb}. */
    public static float addLeaves(float tannin, long waterMb, int leaves, double perLeaf) {
        if (leaves <= 0 || waterMb <= 0) return tannin;
        return (float) Math.min(1, tannin + leaves * perLeaf * 1000.0 / waterMb);
    }

    /** Tannins fade with a half-life of {@code halfLifeDays}. */
    public static float fadeTannin(float tannin, double days, double halfLifeDays) {
        if (tannin <= 0 || halfLifeDays <= 0) return tannin;
        return (float) (tannin * Math.pow(0.5, Math.max(0, days) / halfLifeDays));
    }

    // ------------------------------------------------------------------ mixing

    /** A value after mixing {@code addedMb} with {@code added} into {@code waterMb} with {@code v}. */
    public static float mix(float v, long waterMb, float added, long addedMb) {
        if (addedMb <= 0) return v;
        if (waterMb <= 0) return added;
        return (float) ((v * (double) waterMb + added * (double) addedMb) / (waterMb + addedMb));
    }

    /** pH of mixed water: mixed as the H+ concentration (logarithmic), not linearly. */
    public static float mixPh(float ph, long waterMb, float added, long addedMb) {
        if (addedMb <= 0) return ph;
        if (waterMb <= 0) return clampPh(added);
        double h = (Math.pow(10, -ph) * waterMb + Math.pow(10, -added) * addedMb) / (waterMb + addedMb);
        return clampPh(-Math.log10(h));
    }

    /** What stays behind when {@code lostMb} of pure water evaporate from {@code waterMb}. */
    public static float concentrate(float v, long waterMb, long lostMb, float max) {
        long left = waterMb - lostMb;
        if (lostMb <= 0 || waterMb <= 0 || left <= 0 || v <= 0) return v;
        return Math.min(max, (float) (v * (double) waterMb / left));
    }

    // ------------------------------------------------------------------ clamps

    public static float clampPh(double ph) {
        return Double.isFinite(ph) ? (float) Math.max(PH_MIN, Math.min(PH_MAX, ph)) : TAP_PH;
    }

    public static float clampTds(float tds) {
        return Float.isFinite(tds) ? Math.max(0, Math.min(TDS_MAX, tds)) : TAP_TDS;
    }

    static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
