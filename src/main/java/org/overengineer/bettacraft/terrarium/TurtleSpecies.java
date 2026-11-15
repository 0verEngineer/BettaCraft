package org.overengineer.bettacraft.terrarium;

import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;

/**
 * M31 part b: the turtles of the mod and what makes each one (pure data, unit-tested with {@link TurtleSex}).
 * <ul>
 *     <li>{@link #MUSK}: common musk turtle (<i>Sternotherus odoratus</i>), about 10 cm: paludarium with little land, a bottom
 *     walker, eats animal food; sex by temperature with females at both ends (pattern FMF).</li>
 *     <li>{@link #SLIDER}: pond slider (<i>Trachemys scripta</i>), one species with two varieties: the red-eared slider
 *     (<i>T. s. elegans</i>) and the yellow-bellied slider (<i>T. s. scripta</i>); half water, half land, basking spot and UV;
 *     grows large (a grown one needs a tank of 12 blocks and counts as 8 bettas); omnivore, adults also eat greens.</li>
 *     <li>{@link #HERMANN}: Hermann's tortoise (<i>Testudo hermanni</i>): a dry terrarium, warmth and strong UV, eats greens only
 *     (the food tag {@code #bettacraft:turtle_greens}); brumates in the cold.</li>
 *     <li>{@link #REEVES}: Chinese three-keeled pond turtle (<i>Mauremys reevesii</i>): paludarium, cooler than the slider,
 *     omnivore.</li>
 * </ul>
 * Temperature-dependent sex determination ({@link TurtleSex}): {@code pivot} is the temperature that gives half females;
 * warmer gives females, cooler males (pattern MF). The musk turtle also has {@code lowPivot}: below it the eggs give
 * females again (pattern FMF, as measured for <i>Sternotherus</i>).
 *
 * @param id          entity id
 * @param latin       scientific name
 * @param needs       needs of a grown animal (a young one: {@link TerrariumNeeds#young()})
 * @param gait        how it moves in a tank
 * @param animalFood  eats pellets, frozen and live food
 * @param greens      eats greens ({@code #bettacraft:turtle_greens}: dandelion, clover, herbs)
 * @param stock       stocking weight of a grown animal (an adult betta = 1)
 * @param starveDays  days without food after which it starves
 * @param pivot       pivotal temperature (°C) of the sex determination: half females
 * @param lowPivot    lower pivot of the pattern FMF (NaN = pattern MF)
 * @param eggDays     incubation at the pivot temperature (in-game days, {@code terrarium.incubationSpeed} divides them)
 * @param eggsMin     smallest clutch
 * @param eggsMax     largest clutch
 * @param box         carried in a terrarium box (a land animal) instead of a bucket of water
 * @param brumates    rests through the cold (brumation, {@code terrarium.brumation})
 * @param growDays    in-game days from hatchling to grown
 * @param hatchScale  size of a hatchling against a grown animal
 * @param varieties   the varieties (the first is the default)
 */
public record TurtleSpecies(String id, String latin, TerrariumNeeds needs, LandWalker.Gait gait, boolean animalFood, boolean greens, double stock,
                            double starveDays, double pivot, double lowPivot, double eggDays, int eggsMin, int eggsMax, boolean box,
                            boolean brumates, double growDays, float hatchScale, List<String> varieties) {

    public static final TurtleSpecies MUSK = new TurtleSpecies("musk_turtle", "Sternotherus odoratus",
            TerrariumNeeds.builder(TerrariumNeeds.Habitat.PALUDARIUM).air(18, 32).water(20, 28).land(0.05, 0.6).build(),
            new LandWalker.Gait(0.02, 0.03, true, 0.25, 0.6, 0.08, 0.2, 0.22),
            true, false, 3.0, 7.0, 27.0, 21.0, 3.0, 2, 4, false, false, 4.0, 0.35F, List.of("wild"));

    public static final TurtleSpecies SLIDER = new TurtleSpecies("slider_turtle", "Trachemys scripta",
            TerrariumNeeds.builder(TerrariumNeeds.Habitat.PALUDARIUM).air(22, 32).water(22, 28).land(0.25, 0.75).uv(3).basking().minBlocks(12).build(),
            new LandWalker.Gait(0.025, 0.045, false, 0.3, 0.45, 0.25, 0.15, 0.3),
            true, true, 8.0, 8.0, 29.2, Double.NaN, 3.5, 3, 6, false, false, 8.0, 0.3F, List.of("elegans", "scripta"));

    public static final TurtleSpecies HERMANN = new TurtleSpecies("hermann_tortoise", "Testudo hermanni",
            TerrariumNeeds.builder(TerrariumNeeds.Habitat.DRY).air(20, 32).humidity(35, 75).land(0.9, 1.0).uv(4).basking().minBlocks(8).build(),
            new LandWalker.Gait(0.012, 0.0, true, 0.3, 0.0, 0.3, 0.25, 0.35),
            false, true, 5.0, 10.0, 31.5, Double.NaN, 4.0, 2, 5, true, true, 8.0, 0.35F, List.of("wild"));

    public static final TurtleSpecies REEVES = new TurtleSpecies("reeves_turtle", "Mauremys reevesii",
            TerrariumNeeds.builder(TerrariumNeeds.Habitat.PALUDARIUM).air(18, 30).water(18, 28).land(0.2, 0.6).uv(2).basking().minBlocks(6).build(),
            new LandWalker.Gait(0.02, 0.035, false, 0.3, 0.5, 0.2, 0.2, 0.26),
            true, true, 4.0, 8.0, 28.5, Double.NaN, 3.5, 2, 5, false, false, 6.0, 0.35F, List.of("wild"));

    public static final List<TurtleSpecies> ALL = List.of(MUSK, SLIDER, HERMANN, REEVES);

    /** Entity of a variety other than the first ({@code slider_turtle_scripta}) or the plain id. */
    public String texture(int variety) {
        return variety <= 0 || variety >= varieties.size() ? id + (varieties.size() > 1 ? "_" + varieties.getFirst() : "") : id + "_" + varieties.get(variety);
    }

    public int variety(String name) {
        int i = varieties.indexOf(name);
        return Math.max(0, i);
    }

    /** The bucket or box item of the species. */
    public String containerId() {
        return box ? id + "_box" : id + "_bucket";
    }

    public String spawnEggId() {
        return id + "_spawn_egg";
    }

    public String translationKey() {
        return "entity.bettacraft." + id;
    }

    public String varietyKey(int variety) {
        return "turtle.bettacraft.variety." + varieties.get(Math.max(0, Math.min(varieties.size() - 1, variety))).toLowerCase(Locale.ROOT);
    }

    /** Needs by age (the tank size only for a grown animal). */
    public TerrariumNeeds needs(boolean grown) {
        return grown ? needs : needs.young();
    }

    /** Females at both ends of the temperature range (the musk turtle). */
    public boolean patternFmf() {
        return Double.isFinite(lowPivot);
    }

    @Nullable
    public static TurtleSpecies byId(String id) {
        for (TurtleSpecies s : ALL) if (s.id.equals(id)) return s;
        return null;
    }
}
