package org.overengineer.bettacraft.terrarium;

import java.util.List;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;

/**
 * M54: how an amphibian or reptile of the mod lives besides its {@link LandWalker.Gait} (switch
 * {@code features.visuals.livelyMovement}): the time of day it is active, its tempo against the old gait (the gaits of M31 were
 * tuned for the tiny tank and made a snake crawl), short fast phases (a snake shoots forward, a lizard sprints, a frog makes a
 * series of quick hops), climbing onto the decoration and the plants (branches, rocks, leaves), hiding under the decoration and
 * the little things it does while it sits still ({@link Action}: a snake raises its head and flicks its tongue or lies coiled,
 * a lizard looks around and waves its tail, a frog calls, a turtle stretches its neck). The same in the world, in a tank and in
 * the miniature (the entity carries the action, every renderer shows it). Pure data, unit-tested.
 *
 * @param rhythm     when it is active
 * @param tempo      factor on {@link LandWalker.Gait#walk} and {@link LandWalker.Gait#swim} (the calm pace)
 * @param dashChance share of its walks that start with a fast phase
 * @param dashFactor speed of the fast phase against the calm pace
 * @param dashTicks  length of the fast phase
 * @param perch      share of its goals high up on the decoration or the plants (branches, rocks, leaves)
 * @param hide       share of its goals under the decoration (caves, roots, leaves), more while it is not active
 * @param climb      highest face it climbs on the way to a perch or a hide (tank blocks)
 * @param idle       the things it does while it sits still (the first is the most common)
 * @param coils      lies coiled while it rests (the snake)
 */
public record Habits(Rhythm rhythm, double tempo, double dashChance, double dashFactor, int dashTicks, double perch, double hide, double climb,
                     List<Action> idle, boolean coils) {

    /** When an animal is active. */
    public enum Rhythm {
        /** by day (dart frogs, turtles, the mudskipper) */
        DIURNAL,
        /** at night (geckos, the horned frog, the musk turtle) */
        NOCTURNAL,
        /** at dusk and dawn, a little at night (the corn snake) */
        CREPUSCULAR
    }

    /**
     * What an animal does while it sits still (synced as its ordinal, {@link TerrariumAnimalEntity#action}); the models
     * play it.
     */
    public enum Action {
        NONE,
        /** turns its head from side to side */
        LOOK,
        /** the snake: raises the front of its body, the head up, and flicks its tongue */
        HEAD_UP,
        /** the snake: flicks its tongue */
        TONGUE,
        /** the snake: lies coiled up */
        COIL,
        /** a frog: the throat pumps fast (calling) */
        CALL,
        /** a turtle: stretches its neck far out; a gecko: stretches up on its legs */
        STRETCH,
        /** a gecko: waves the tip of its tail */
        TAIL;

        public static Action of(int ordinal) {
            Action[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
        }

        /** How long it lasts (ticks; {@link #COIL} lasts the whole rest). */
        public int ticks(RandomGenerator r) {
            return switch (this) {
                case LOOK -> 40 + r.nextInt(50);
                case HEAD_UP -> 70 + r.nextInt(90);
                case TONGUE -> 18 + r.nextInt(14);
                case CALL -> 40 + r.nextInt(40);
                case STRETCH -> 50 + r.nextInt(60);
                case TAIL -> 25 + r.nextInt(25);
                case COIL -> Integer.MAX_VALUE;
                case NONE -> 0;
            };
        }
    }

    /** Game time of day (0..24000, 0 = 6 am). */
    public static boolean dusk(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return t >= 22500 || t < 1500 || t >= 10500 && t < 13500;
    }

    /** Whether it is active now (by its rhythm and the time of day). */
    public boolean active(long dayTime, boolean day) {
        return switch (rhythm) {
            case DIURNAL -> day;
            case NOCTURNAL -> !day || dusk(dayTime);
            case CREPUSCULAR -> dusk(dayTime) || !day;
        };
    }

    /** Factor on its share of rests: few while it is active, many while it is not. */
    public double restFactor(long dayTime, boolean day) {
        if (rhythm == Rhythm.CREPUSCULAR && dusk(dayTime)) return 0.4; // its best hours
        return active(dayTime, day) ? 0.6 : 2.2;
    }

    /** Factor on its share of hides: it hides while it is not active (a gecko by day, a frog at night). */
    public double hideFactor(long dayTime, boolean day) {
        return active(dayTime, day) ? 0.4 : 2.0;
    }

    /** A random idle action (the first of {@link #idle} twice as likely), NONE without any. */
    public Action randomIdle(RandomGenerator r) {
        if (idle.isEmpty()) return Action.NONE;
        int n = idle.size() + 1;
        int i = r.nextInt(n);
        return idle.get(Math.max(0, i - 1));
    }

    /** M54: the switch: lively movement on. */
    public static boolean enabled() {
        return Features.enabled(Feature.LIVELY_MOVEMENT);
    }

    /** The habits of a species (null: none, the M31 behaviour). */
    @Nullable
    public static Habits of(HerpSpecies s) {
        if (s == HerpSpecies.CORN_SNAKE) return SNAKE;
        if (s == HerpSpecies.LEOPARD_GECKO) return LEOPARD_GECKO;
        if (s == HerpSpecies.CRESTED_GECKO) return CRESTED_GECKO;
        if (s == HerpSpecies.DART_FROG) return DART_FROG;
        if (s == HerpSpecies.HORNED_FROG) return HORNED_FROG;
        if (s == HerpSpecies.MUDSKIPPER) return MUDSKIPPER;
        if (s == HerpSpecies.MUSK) return MUSK;
        if (s == HerpSpecies.SLIDER) return SLIDER;
        if (s == HerpSpecies.HERMANN) return HERMANN;
        if (s == HerpSpecies.REEVES) return REEVES;
        return null;
    }

    /**
     * Corn snake: active at dusk and at night, explores at a calm crawl (about a fifth of its length a second), shoots forward
     * now and then, climbs branches and roots, hides in caves by day, lies coiled while it rests; raises its head and flicks
     * its tongue.
     */
    public static final Habits SNAKE = new Habits(Rhythm.CREPUSCULAR, 1.6, 0.22, 3.0, 22, 0.22, 0.25, 0.9,
            List.of(Action.HEAD_UP, Action.TONGUE, Action.LOOK), true);
    /** Leopard gecko: a night animal of the rocks, walks deliberately and sprints off; hides by day, climbs low rocks. */
    public static final Habits LEOPARD_GECKO = new Habits(Rhythm.NOCTURNAL, 1.4, 0.2, 3.2, 14, 0.12, 0.35, 0.5,
            List.of(Action.LOOK, Action.TAIL, Action.STRETCH), false);
    /** Crested gecko: a night climber, slow and jumpy; sits on branches and leaves high up. */
    public static final Habits CRESTED_GECKO = new Habits(Rhythm.NOCTURNAL, 1.1, 0.15, 3.0, 10, 0.3, 0.12, 0.8,
            List.of(Action.LOOK, Action.TAIL), false);
    /** Dart frogs: busy by day, hop in quick series, sit on leaves, call; under the leaves at night. */
    public static final Habits DART_FROG = new Habits(Rhythm.DIURNAL, 1.2, 0.18, 1.8, 40, 0.32, 0.12, 0.5,
            List.of(Action.LOOK, Action.CALL), false);
    /** Horned frog: an ambush hunter at night that barely moves; lunges short and fast. */
    public static final Habits HORNED_FROG = new Habits(Rhythm.NOCTURNAL, 1.0, 0.3, 3.5, 8, 0.0, 0.0, 0.3,
            List.of(Action.LOOK, Action.CALL), false);
    /** Mudskipper: busy by day, skips fast over the mud, sits on roots. */
    public static final Habits MUDSKIPPER = new Habits(Rhythm.DIURNAL, 1.3, 0.28, 2.8, 12, 0.22, 0.05, 0.6,
            List.of(Action.LOOK), false);
    /** Musk turtle: a bottom walker of the night, hides under roots and in caves by day. */
    public static final Habits MUSK = new Habits(Rhythm.NOCTURNAL, 1.25, 0.1, 2.0, 20, 0.0, 0.3, 0.3,
            List.of(Action.LOOK, Action.STRETCH), false);
    /** Slider: basks by day, slips into the water fast. */
    public static final Habits SLIDER = new Habits(Rhythm.DIURNAL, 1.2, 0.15, 2.4, 20, 0.05, 0.05, 0.3,
            List.of(Action.STRETCH, Action.LOOK), false);
    /** Hermann's tortoise: slow and steady by day, under shelter at night. */
    public static final Habits HERMANN = new Habits(Rhythm.DIURNAL, 1.0, 0.0, 1.0, 0, 0.0, 0.25, 0.35,
            List.of(Action.LOOK, Action.STRETCH), false);
    /** Reeves' turtle: by day, between water and land. */
    public static final Habits REEVES = new Habits(Rhythm.DIURNAL, 1.15, 0.1, 2.2, 20, 0.0, 0.15, 0.3,
            List.of(Action.LOOK, Action.STRETCH), false);
}
