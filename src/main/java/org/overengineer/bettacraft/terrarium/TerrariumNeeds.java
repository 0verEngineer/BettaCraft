package org.overengineer.bettacraft.terrarium;

import java.util.EnumSet;
import java.util.Set;

/**
 * M31: what a terrarium animal needs (the reusable care hook; turtles, frogs, geckos and snakes register against it in
 * {@link TerrariumAnimals}). Pure: {@link #assess} compares the needs with a {@link Conditions} snapshot of the tank and
 * names every problem; {@link Assessment#stress} turns them into a stress 0..1 the animal's care uses (slower growth, no
 * breeding, health loss from {@link #HARMFUL}).
 *
 * @param habitat   which tank modes the animal can live in
 * @param air       air temperature range (°C) in which it is fine
 * @param humidity  relative humidity range (%)
 * @param uv        UV index it needs at the basking spot (0 = none)
 * @param land      share of the floor that must be land (min, max): a musk turtle 0.05..0.5, a tortoise 0.9..1
 * @param basking   needs a basking spot (a lit basking lamp) warmer than the air
 * @param water     water temperature range for animals that swim (NaN..NaN = not checked)
 * @param minBlocks M31 part b: smallest tank (water and land blocks) the grown animal needs (0 = any; a grown slider 12)
 */
public record TerrariumNeeds(Habitat habitat, Range air, Range humidity, double uv, Range land, boolean basking, Range water, double minBlocks) {
    public TerrariumNeeds(Habitat habitat, Range air, Range humidity, double uv, Range land, boolean basking, Range water) {
        this(habitat, air, humidity, uv, land, basking, water, 0);
    }

    /** The same needs without the tank size (a young animal). */
    public TerrariumNeeds young() {
        return minBlocks <= 0 ? this : new TerrariumNeeds(habitat, air, humidity, uv, land, basking, water, 0);
    }

    /** The tank modes an animal lives in. */
    public enum Habitat {
        /** Terrarium without water (a water dish is fine). */
        DRY,
        /** Paludarium: water and land. */
        PALUDARIUM,
        /** Either. */
        ANY,
        /** M31 part d: water: an aquarium or a paludarium (the drowned). */
        WET,
        /** M31 part d: any tank, an aquarium too (the zombie, which becomes a drowned in the water). */
        ALL;

        public boolean allows(TerrariumMode m) {
            return switch (this) {
                case DRY -> m == TerrariumMode.TERRARIUM;
                case PALUDARIUM -> m == TerrariumMode.PALUDARIUM;
                case ANY -> m.hasLand();
                case WET -> m != TerrariumMode.TERRARIUM;
                case ALL -> true;
            };
        }
    }

    /** A closed range; NaN bounds are open. */
    public record Range(double min, double max) {
        public static final Range ANY = new Range(Double.NaN, Double.NaN);

        public static Range of(double min, double max) {
            return new Range(min, max);
        }

        public boolean below(double v) {
            return !Double.isNaN(min) && v < min - 1e-9;
        }

        public boolean above(double v) {
            return !Double.isNaN(max) && v > max + 1e-9;
        }

        /** How far outside the range (0 inside). */
        public double off(double v) {
            if (below(v)) return min - v;
            if (above(v)) return v - max;
            return 0;
        }
    }

    /** The problems an animal can have with its tank. */
    public enum Problem {
        WRONG_HABITAT, TOO_COLD, TOO_HOT, TOO_DRY, TOO_HUMID, NO_UV, NO_BASKING, TOO_LITTLE_LAND, TOO_LITTLE_WATER, WATER_COLD, WATER_HOT,
        /** M31 part b: the tank is smaller than the grown animal needs (stress only). */
        TOO_SMALL,
        /** M49: a brackish water animal (the mudskipper) in fresh water (stress only). */
        FRESH_WATER;

        public String translationKey() {
            return "gui.bettacraft.terrarium.problem." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Problems that cost health over time (the others only stress). */
    public static final Set<Problem> HARMFUL = EnumSet.of(Problem.WRONG_HABITAT, Problem.TOO_COLD, Problem.TOO_HOT, Problem.WATER_COLD, Problem.WATER_HOT);

    /**
     * What the tank offers now.
     *
     * @param mode      the tank's mode
     * @param air       air temperature (°C)
     * @param humidity  relative humidity (%)
     * @param uv        UV index at the basking spot
     * @param bask      basking spot temperature (°C; = air without a lit lamp)
     * @param land      land share of the floor
     * @param waterTemp water temperature (NaN without water)
     * @param blocks    M31 part b: size of the tank in blocks (NaN = not checked)
     */
    public record Conditions(TerrariumMode mode, double air, double humidity, double uv, double bask, double land, double waterTemp, double blocks) {
        public Conditions(TerrariumMode mode, double air, double humidity, double uv, double bask, double land, double waterTemp) {
            this(mode, air, humidity, uv, bask, land, waterTemp, Double.NaN);
        }
    }

    /** The result of {@link #assess}: the problems and how much each is off (K, % or share). */
    public record Assessment(Set<Problem> problems, double severity) {
        public static final Assessment FINE = new Assessment(EnumSet.noneOf(Problem.class), 0);

        public boolean fine() {
            return problems.isEmpty();
        }

        public boolean harmful() {
            for (Problem p : problems) if (HARMFUL.contains(p)) return true;
            return false;
        }

        /** Stress 0..1: each problem a part, more the farther off. */
        public double stress() {
            return Math.min(1.0, problems.size() * 0.2 + severity);
        }
    }

    public static Builder builder(Habitat habitat) {
        return new Builder(habitat);
    }

    public Assessment assess(Conditions c) {
        Set<Problem> out = EnumSet.noneOf(Problem.class);
        double sev = 0;
        if (!habitat.allows(c.mode())) {
            out.add(Problem.WRONG_HABITAT);
            sev += 0.5;
        }
        if (Double.isFinite(c.air())) {
            // an animal that can bask warms up there: a cool room is fine while the basking spot reaches the range
            double warmest = basking ? Math.max(c.air(), c.bask()) : c.air();
            if (air.below(warmest)) {
                out.add(Problem.TOO_COLD);
                sev += air.off(warmest) / 10.0;
            } else if (air.above(c.air())) {
                out.add(Problem.TOO_HOT);
                sev += air.off(c.air()) / 8.0;
            }
        }
        if (Double.isFinite(c.humidity())) {
            if (humidity.below(c.humidity())) {
                out.add(Problem.TOO_DRY);
                sev += humidity.off(c.humidity()) / 100.0;
            } else if (humidity.above(c.humidity())) {
                out.add(Problem.TOO_HUMID);
                sev += humidity.off(c.humidity()) / 100.0;
            }
        }
        if (uv > 0 && c.uv() < uv - 1e-9) {
            out.add(Problem.NO_UV);
            sev += (uv - Math.max(0, c.uv())) / (uv * 10.0);
        }
        if (basking && !(c.bask() > c.air() + 1)) out.add(Problem.NO_BASKING);
        if (land.below(c.land())) out.add(Problem.TOO_LITTLE_LAND);
        else if (land.above(c.land())) out.add(Problem.TOO_LITTLE_WATER);
        if (Double.isFinite(c.waterTemp()) && c.mode() != TerrariumMode.TERRARIUM) {
            if (water.below(c.waterTemp())) {
                out.add(Problem.WATER_COLD);
                sev += water.off(c.waterTemp()) / 10.0;
            } else if (water.above(c.waterTemp())) {
                out.add(Problem.WATER_HOT);
                sev += water.off(c.waterTemp()) / 8.0;
            }
        }
        if (minBlocks > 0 && Double.isFinite(c.blocks()) && c.blocks() < minBlocks - 1e-6) {
            out.add(Problem.TOO_SMALL);
            sev += (minBlocks - c.blocks()) / (minBlocks * 4.0);
        }
        return out.isEmpty() ? Assessment.FINE : new Assessment(out, sev);
    }

    /** Builds needs with sensible open defaults. */
    public static final class Builder {
        private final Habitat habitat;
        private Range air = Range.ANY, humidity = Range.ANY, land = Range.ANY, water = Range.ANY;
        private double uv, minBlocks;
        private boolean basking;

        Builder(Habitat habitat) {
            this.habitat = habitat;
        }

        public Builder air(double min, double max) { air = Range.of(min, max); return this; }
        public Builder humidity(double min, double max) { humidity = Range.of(min, max); return this; }
        public Builder land(double min, double max) { land = Range.of(min, max); return this; }
        public Builder water(double min, double max) { water = Range.of(min, max); return this; }
        public Builder uv(double index) { uv = index; return this; }
        public Builder basking() { basking = true; return this; }
        public Builder minBlocks(double blocks) { minBlocks = blocks; return this; }

        public TerrariumNeeds build() {
            return new TerrariumNeeds(habitat, air, humidity, uv, land, basking, water, minBlocks);
        }
    }
}
