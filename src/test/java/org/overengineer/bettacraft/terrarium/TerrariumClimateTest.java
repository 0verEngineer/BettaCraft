package org.overengineer.bettacraft.terrarium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.terrarium.Climate.Inputs;
import org.overengineer.bettacraft.terrarium.Climate.Params;
import org.overengineer.bettacraft.terrarium.Climate.State;
import org.overengineer.bettacraft.terrarium.TerrariumNeeds.Conditions;
import org.overengineer.bettacraft.terrarium.TerrariumNeeds.Habitat;
import org.overengineer.bettacraft.terrarium.TerrariumNeeds.Problem;

/** M31: tank modes, the climate model and the needs of the terrarium animals. */
class TerrariumClimateTest {
    private static final Params P = Params.DEFAULT;

    /** A closed, dry 4 x 4 terrarium in a 20 °C room at 50 % humidity without devices. */
    private static Inputs room() {
        return new Inputs(20F, 50F, 27F, 0, 0, 20F, 0, 0, 16, 0, 16, 0, 0, 0);
    }

    private static Inputs with(Inputs b, double open, double water, double groundMats, double wallMats, int lamps, double plants, int mist, double sky) {
        return new Inputs(b.ambient(), b.roomHumidity(), b.target(), open, water, b.waterTemp(), groundMats, wallMats, b.footprint(), lamps,
                b.topBlocks(), plants, mist, sky);
    }

    @Test
    void modeFromWaterAndLand() {
        var th = TerrariumMode.Thresholds.DEFAULT;
        assertEquals(TerrariumMode.TERRARIUM, TerrariumMode.detect(0, -1, th), "no water: always a terrarium");
        assertEquals(TerrariumMode.AQUARIUM, TerrariumMode.detect(16000, 0, th), "a fish tank");
        assertEquals(TerrariumMode.AQUARIUM, TerrariumMode.detect(16000, 0.1, th), "a few stones above the water");
        assertEquals(TerrariumMode.PALUDARIUM, TerrariumMode.detect(8000, 0.15, th));
        assertEquals(TerrariumMode.PALUDARIUM, TerrariumMode.detect(8000, 0.6, th));
        assertEquals(TerrariumMode.TERRARIUM, TerrariumMode.detect(500, 0.95, th), "the water is only a dish");
        assertEquals(TerrariumMode.AQUARIUM, TerrariumMode.detect(16000, -1, th), "land not known yet");
    }

    @Test
    void anEmptyClosedTerrariumIsTheRoom() {
        State s = Climate.equilibrium(room(), P);
        assertEquals(20F, s.air(), 1e-4);
        assertEquals(20F, s.ground(), 1e-4);
        assertEquals(20F, s.bask(), 1e-4, "no lamp: no basking spot");
        assertEquals(50F, s.humidity(), 1e-4);
        assertEquals(0F, s.uv(), 1e-4);
        assertEquals(0F, s.matDuty(), 1e-4);
    }

    @Test
    void heatMatsWarmTheGroundMoreThanTheAirAndStopAtTheTarget() {
        // 8 mats under a 16-block floor: full lift, the thermostat (27 °C) is not reached
        State s = Climate.equilibrium(with(room(), 0, 0, 8, 0, 0, 0, 0, 0), P);
        assertEquals(1F, s.matDuty(), 1e-4);
        assertEquals(20 + 8 * 0.4, s.air(), 1e-3);
        assertEquals(20 + 8, s.ground(), 1e-3);
        // a warm room: the mats run only part of the time
        Inputs warm = new Inputs(25F, 50F, 27F, 0, 0, 25F, 8, 0, 16, 0, 16, 0, 0, 0);
        State w = Climate.equilibrium(warm, P);
        assertTrue(w.matDuty() > 0 && w.matDuty() < 1, "duty " + w.matDuty());
        assertEquals(27F, w.air(), 1e-3, "held at the target");
        // above the target they are off
        Inputs hot = new Inputs(29F, 50F, 27F, 0, 0, 29F, 8, 0, 16, 0, 16, 0, 0, 0);
        assertEquals(0F, Climate.equilibrium(hot, P).matDuty(), 1e-4);
        // one mat on a 16-block floor lifts an eighth as much as full cover; wall mats count half and warm the floor less
        assertEquals(1.0, Climate.matLift(1, 0, 16, P), 1e-6);
        assertEquals(0.5, Climate.matLift(0, 1, 16, P), 1e-6);
        State wall = Climate.equilibrium(with(room(), 0, 0, 0, 16, 0, 0, 0, 0), P);
        assertTrue(wall.ground() - wall.air() < s.ground() - s.air(), "wall mats warm the floor less");
    }

    @Test
    void anOpenTopLosesWarmthAndMoisture() {
        State closed = Climate.equilibrium(with(room(), 0, 0.5, 8, 0, 0, 0, 0, 0), P);
        State open = Climate.equilibrium(with(room(), 1, 0.5, 8, 0, 0, 0, 0, 0), P);
        assertTrue(open.air() < closed.air());
        assertTrue(open.humidity() < closed.humidity());
    }

    @Test
    void lampsMakeABaskingSpotWithUv() {
        State s = Climate.equilibrium(with(room(), 0, 0, 0, 0, 1, 0, 0, 0), P);
        assertEquals(21F, s.air(), 1e-3, "one lamp on 16 tops: a quarter of the full air lift");
        assertEquals(s.air() + 12, s.bask(), 1e-3);
        assertEquals(5F, s.uv(), 1e-4);
        // sunlight through an open top
        assertEquals(3F, Climate.equilibrium(with(room(), 1, 0, 0, 0, 0, 0, 0, 1), P).uv(), 1e-4);
    }

    @Test
    void waterPlantsAndMistersMakeItHumidWarmAirDries() {
        double dry = Climate.equilibrium(room(), P).humidity();
        double water = Climate.equilibrium(with(room(), 0, 0.5, 0, 0, 0, 0, 0, 0), P).humidity();
        double plants = Climate.equilibrium(with(room(), 0, 0, 0, 0, 0, 1, 0, 0), P).humidity();
        double mist = Climate.equilibrium(with(room(), 0, 0, 0, 0, 0, 0, 1, 0), P).humidity();
        double warm = Climate.equilibrium(with(room(), 0, 0, 8, 0, 0, 0, 0, 0), P).humidity();
        assertTrue(water > dry && plants > dry && mist > water, dry + " " + water + " " + plants + " " + mist);
        assertTrue(warm < dry, "heated air holds more water");
        assertEquals(100F, Climate.equilibrium(with(room(), 0, 1, 0, 0, 0, 1, 3, 0), P).humidity(), 1e-4, "capped");
        assertEquals(30F + 45F, Climate.roomHumidity(1F, false), 1e-4);
        assertEquals(30F + 20F, Climate.roomHumidity(0F, true), 1e-4);
    }

    @Test
    void stepsApproachTheEquilibrium() {
        Inputs in = with(room(), 0, 0, 8, 0, 1, 0, 0, 0);
        State eq = Climate.equilibrium(in, P);
        assertEquals(eq, Climate.step(null, in, P, 0.001), "first step: at once");
        State cold = new State(10F, 10F, 10F, 90F, 0F, 0F);
        State one = Climate.step(cold, in, P, P.tauAir());
        assertEquals(eq.air() + (10 - eq.air()) * Math.exp(-1), one.air(), 1e-3);
        assertEquals(one.air() + (eq.bask() - eq.air()), one.bask(), 1e-4, "the basking spot keeps its lift over the air");
        State far = Climate.step(cold, in, P, 10);
        assertEquals(eq.air(), far.air(), 1e-3);
        assertEquals(eq.humidity(), far.humidity(), 1e-3);
    }

    @Test
    void needsNameTheProblems() {
        TerrariumNeeds tortoise = TerrariumNeeds.builder(Habitat.DRY).air(22, 32).humidity(30, 70).uv(3).land(0.9, 1).basking().build();
        Conditions fine = new Conditions(TerrariumMode.TERRARIUM, 26, 50, 5, 38, 1, Double.NaN);
        assertTrue(tortoise.assess(fine).fine());
        assertEquals(0, tortoise.assess(fine).stress(), 1e-9);
        // cold air, but the basking spot reaches the range: fine for an animal that basks
        assertTrue(tortoise.assess(new Conditions(TerrariumMode.TERRARIUM, 20, 50, 5, 32, 1, Double.NaN)).fine());
        var bad = tortoise.assess(new Conditions(TerrariumMode.PALUDARIUM, 18, 90, 0, 18, 0.5, 25));
        assertTrue(bad.problems().containsAll(java.util.List.of(Problem.WRONG_HABITAT, Problem.TOO_COLD, Problem.TOO_HUMID, Problem.NO_UV,
                Problem.NO_BASKING, Problem.TOO_LITTLE_LAND)), bad.problems().toString());
        assertTrue(bad.harmful());
        assertTrue(bad.stress() > 0.9);
        TerrariumNeeds musk = TerrariumNeeds.builder(Habitat.PALUDARIUM).air(20, 32).water(22, 28).land(0.05, 0.5).build();
        var wet = musk.assess(new Conditions(TerrariumMode.PALUDARIUM, 25, 80, 0, 25, 0.2, 18));
        assertEquals(java.util.Set.of(Problem.WATER_COLD), wet.problems());
        var noLand = musk.assess(new Conditions(TerrariumMode.PALUDARIUM, 25, 80, 0, 25, 0.8, 25));
        assertEquals(java.util.Set.of(Problem.TOO_LITTLE_WATER), noLand.problems());
        assertFalse(noLand.harmful(), "only stress");
        assertTrue(Habitat.ANY.allows(TerrariumMode.TERRARIUM) && Habitat.ANY.allows(TerrariumMode.PALUDARIUM) && !Habitat.ANY.allows(TerrariumMode.AQUARIUM));
    }
}
