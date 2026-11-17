package org.overengineer.bettacraft.terrarium;

import javax.annotation.Nullable;

/**
 * M31: the air of a terrarium or paludarium as plain functions (unit-tested): air temperature, the ground over the heat
 * mats, the basking spot under a basking lamp, the humidity and the UV at the basking spot. Every value approaches its
 * equilibrium like the water temperature does ({@code TankTemperature#approach}); the equilibrium follows from the room
 * (biome and day/night, the rain), the devices (heat mats with the tank's target temperature as their thermostat, basking
 * lamps, misters) and the tank (water surface, plants on the land, how much of the top is open).
 * <ul>
 *     <li><b>Lid:</b> a covered top keeps warmth and moisture in; an open top lets them out (and lets sunlight UV in, glass
 *     and lids block it).</li>
 *     <li><b>Heat mats</b> lift the ground most and the air a part of it; covering half the floor is enough (as in real
 *     terraria). They run only while the air is below the target (duty 0..1).</li>
 *     <li><b>Water</b> pulls the air towards its own temperature by its surface share and evaporates (more when warm).</li>
 *     <li><b>Warm air</b> above the room temperature holds more water: the humidity drops by {@link #DRYING_PER_K}.</li>
 * </ul>
 */
public final class Climate {
    /** Relative humidity lost per kelvin the air is warmer than the room. */
    public static final double DRYING_PER_K = 1.2;
    /** Share of the heat mats' lift that reaches the air (the ground gets the rest on top). */
    public static final double MAT_AIR_SHARE = 0.4;

    private Climate() {}

    /**
     * Strength of the devices (config {@code terrarium.*}).
     *
     * @param matLift       K the ground over full heat mat cover rises (air: {@link #MAT_AIR_SHARE} of it)
     * @param lampAir       K the air rises under basking lamps over a quarter of the top
     * @param baskLift      K the basking spot is warmer than the air
     * @param lampUv        UV index at the basking spot of a lit lamp
     * @param skyUv         UV index of sunlight through an open top
     * @param mistHumidity  % humidity one misting mister adds (closed tank)
     * @param waterHumidity % humidity a tank full of water surface adds (closed, 20 °C water)
     * @param plantHumidity % humidity land fully covered with plants adds (closed)
     * @param tauAir        days in which the air gets 63 % of the way to its equilibrium
     * @param tauHumidity   the same for the humidity
     */
    public record Params(double matLift, double lampAir, double baskLift, double lampUv, double skyUv, double mistHumidity, double waterHumidity,
                         double plantHumidity, double tauAir, double tauHumidity) {
        public static final Params DEFAULT = new Params(8, 4, 12, 5, 3, 40, 35, 15, 0.05, 0.03);
    }

    /**
     * What the tank and its devices are now.
     *
     * @param ambient      room temperature (°C, biome and day/night)
     * @param roomHumidity relative humidity of the room (%, biome rainfall and rain)
     * @param target       thermostat of the heat mats (the tank's target temperature)
     * @param openShare    share of the top blocks that are open (0 = fully covered)
     * @param waterShare   share of the floor that is water surface (0 for a dry terrarium)
     * @param waterTemp    water temperature (only used with water)
     * @param groundMats   powered heat mats under the tank
     * @param wallMats     powered heat mats on its walls (count half for the floor)
     * @param footprint    floor blocks of the tank (columns)
     * @param lampsLit     lit basking lamps on the tank
     * @param topBlocks    top blocks of the tank
     * @param plantShare   share of the land covered with plants
     * @param misting      misters spraying now
     * @param sky          0..1: how much sunlight UV comes in through open tops (day, sky, no rain)
     */
    public record Inputs(float ambient, float roomHumidity, float target, double openShare, double waterShare, float waterTemp, double groundMats,
                         double wallMats, int footprint, int lampsLit, int topBlocks, double plantShare, int misting, double sky) {
        /** The same with this many misters spraying. */
        public Inputs withMisting(int n) {
            return new Inputs(ambient, roomHumidity, target, openShare, waterShare, waterTemp, groundMats, wallMats, footprint, lampsLit, topBlocks,
                    plantShare, n, sky);
        }
    }

    /** The climate: air, ground over the mats, basking spot (°C), humidity (%), UV index, heat mat duty (0..1). */
    public record State(float air, float ground, float bask, float humidity, float uv, float matDuty) {
        public boolean valid() {
            return Float.isFinite(air) && Float.isFinite(humidity);
        }
    }

    /** Heat mat lift (K of the ground at full duty) for the mats and the floor. */
    public static double matLift(double groundMats, double wallMats, int footprint, Params p) {
        double mats = groundMats + 0.5 * wallMats;
        if (mats <= 0) return 0;
        return p.matLift() * Math.min(1.0, mats / Math.max(1.0, footprint * 0.5));
    }

    /** How much warmth and moisture stay in: 1 closed, 0.6 fully open. */
    public static double retain(double openShare) {
        return 1 - 0.4 * clamp01(openShare);
    }

    public static State equilibrium(Inputs in, Params p) {
        double open = clamp01(in.openShare()), water = clamp01(in.waterShare());
        double retain = retain(open);
        double lampAir = in.lampsLit() > 0 ? p.lampAir() * Math.min(1.0, in.lampsLit() / Math.max(1.0, in.topBlocks() * 0.25)) * retain : 0;
        double base = in.ambient() + lampAir;
        if (water > 0) base += (in.waterTemp() - base) * 0.5 * water;
        double lift = matLift(in.groundMats(), in.wallMats(), in.footprint(), p);
        double airPerDuty = lift * MAT_AIR_SHARE * retain;
        double duty = airPerDuty <= 1e-9 ? 0 : clamp01((in.target() - base) / airPerDuty);
        double air = base + airPerDuty * duty;
        double mats = in.groundMats() + 0.5 * in.wallMats();
        // mats on a wall warm the floor only half as much as those under it
        double groundShare = mats <= 0 ? 0 : (in.groundMats() + 0.25 * in.wallMats()) / mats;
        double ground = air + lift * (1 - MAT_AIR_SHARE) * duty * groundShare;
        double bask = in.lampsLit() > 0 ? air + p.baskLift() : air;
        double evap = p.waterHumidity() * water * Math.max(0.5, Math.min(1.6, 1 + (in.waterTemp() - 20) / 30.0));
        double plants = p.plantHumidity() * clamp01(in.plantShare());
        double mist = in.misting() <= 0 ? 0 : p.mistHumidity() * (1 + 0.25 * (Math.min(3, in.misting()) - 1));
        double closed = 1 - 0.7 * open;
        double rh = in.roomHumidity() + (evap + plants + mist) * closed - DRYING_PER_K * Math.max(0, air - in.ambient());
        rh = Math.max(5, Math.min(100, rh));
        double uv = Math.max(in.lampsLit() > 0 ? p.lampUv() : 0, p.skyUv() * clamp01(in.sky()));
        return new State((float) air, (float) ground, (float) bask, (float) rh, (float) uv, (float) duty);
    }

    /** One step of {@code days}: air and humidity move towards the equilibrium, ground and basking spot keep their lift over the air. */
    public static State step(@Nullable State now, Inputs in, Params p, double days) {
        State eq = equilibrium(in, p);
        if (now == null || !now.valid()) return eq;
        float air = approach(now.air(), eq.air(), days, p.tauAir());
        float rh = approach(now.humidity(), eq.humidity(), days, p.tauHumidity());
        return new State(air, air + (eq.ground() - eq.air()), air + (eq.bask() - eq.air()), Math.max(5F, Math.min(100F, rh)), eq.uv(), eq.matDuty());
    }

    public static float approach(float current, float target, double days, double tau) {
        if (tau <= 0) return target;
        if (days <= 0) return current;
        return (float) (target + (current - target) * Math.exp(-days / tau));
    }

    /** Relative humidity of a room from the rainfall of its biome (0 desert .. 1 jungle) and rain falling on the tank. */
    public static float roomHumidity(float downfall, boolean raining) {
        double rh = 30 + 45 * clamp01(downfall) + (raining ? 20 : 0);
        return (float) Math.min(100, rh);
    }

    static double clamp01(double v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }
}
