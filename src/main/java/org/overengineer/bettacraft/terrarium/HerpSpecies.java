package org.overengineer.bettacraft.terrarium;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * M31: the egg-laying terrarium animals of the mod (reptiles and amphibians) and what makes each one (pure data,
 * unit-tested with {@link EggSex}). Part b: the turtles; part c: the frogs, geckos and the snake.
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
 *     <li>part c {@link #DART_FROG}: poison dart frogs (<i>Dendrobates</i>), three species as varieties that do not cross
 *     (<i>D. tinctorius</i>, <i>D. auratus</i>, <i>D. leucomelas</i>): a humid paludarium, small groups, fruit flies; eggs on the
 *     land, tadpoles in the water.</li>
 *     <li>part c {@link #HORNED_FROG}: Cranwell's horned frog (<i>Ceratophrys cranwelli</i>): a moist terrarium, sits buried in
 *     the ground and waits for its prey (crickets); no breeding in the tank (in captivity only with hormones).</li>
 *     <li>part c {@link #LEOPARD_GECKO}: leopard gecko (<i>Eublepharis macularius</i>): a dry, warm terrarium, crickets; two eggs,
 *     sex by temperature (pattern FMF: males around 32-33 °C, females cooler and hotter); morphs tangerine and albino.</li>
 *     <li>part c {@link #CRESTED_GECKO}: crested gecko (<i>Correlophus ciliatus</i>): climbs glass and decoration, moderate
 *     temperature, humid; fruit and insects; two eggs, genetic sex.</li>
 *     <li>part c {@link #CORN_SNAKE}: corn snake (<i>Pantherophis guttatus</i>): a dry terrarium with a basking spot, eats a prey
 *     animal about once a week (raw rabbit or chicken stand in for the frozen mice of the hobby), brumates in the cold.</li>
 * </ul>
 * Sex by temperature ({@link EggSex}, {@link #tsd}): {@code pivot} is the temperature that gives half females; warmer gives
 * females, cooler males (pattern MF). With {@code lowPivot} the eggs give females below it too (pattern FMF). Without
 * {@code tsd} the sex is genetic (half and half) and {@code pivot} is only the temperature at which the eggs take
 * {@code eggDays}.
 *
 * @param id          entity id
 * @param latin       scientific name
 * @param body        body plan (model, sounds)
 * @param needs       needs of a grown animal (a young one: {@link TerrariumNeeds#young()})
 * @param gait        how it moves in a tank
 * @param diet        what it eats
 * @param stock       stocking weight of a grown animal (an adult betta = 1)
 * @param starveDays  days without food after which it starves
 * @param mealDays    days after a meal before it eats again (the snake about a week; 0 = whenever there is food)
 * @param tsd         sex by the nest temperature (else genetic)
 * @param pivot       pivotal temperature (°C) of the sex determination (half females), or the reference temperature
 * @param lowPivot    lower pivot of the pattern FMF (NaN = pattern MF)
 * @param eggDays     incubation at the pivot temperature (in-game days, {@code terrarium.incubationSpeed} divides them)
 * @param eggsMin     smallest clutch
 * @param eggsMax     largest clutch
 * @param box         carried in a terrarium box (a land animal) instead of a bucket of water
 * @param brumates    rests through the cold (brumation, {@code terrarium.brumation})
 * @param growDays    in-game days from hatchling to grown
 * @param hatchScale  size of a hatchling against a grown animal
 * @param varieties   the varieties (the first is the default)
 * @param wildVarieties how many of the first varieties live in the wild (the others are morphs bred in captivity)
 * @param breeds      breeds in a tank at all
 * @param tadpoles    the young hatch as tadpoles and go into the water (dart frogs)
 * @param pureVarieties the varieties are species of their own: only the same variety mates
 * @param morphChance chance of each young to show a captive morph (a variety after {@code wildVarieties}) of its own
 */
public record HerpSpecies(String id, String latin, Body body, TerrariumNeeds needs, LandWalker.Gait gait, Set<TerrariumFood.Diet> diet, double stock,
                          double starveDays, double mealDays, boolean tsd, double pivot, double lowPivot, double eggDays, int eggsMin, int eggsMax,
                          boolean box, boolean brumates, double growDays, float hatchScale, List<String> varieties, int wildVarieties, boolean breeds,
                          boolean tadpoles, boolean pureVarieties, double morphChance) {

    /** Body plans: each has its own model, gait and sounds. */
    public enum Body { TURTLE, FROG, GECKO, SNAKE }

    public static final HerpSpecies MUSK = builder("musk_turtle", "Sternotherus odoratus", Body.TURTLE)
            .needs(TerrariumNeeds.builder(TerrariumNeeds.Habitat.PALUDARIUM).air(18, 32).water(20, 28).land(0.05, 0.6).build())
            .gait(new LandWalker.Gait(0.02, 0.03, true, 0.25, 0.6, 0.08, 0.2, 0.22))
            .diet(TerrariumFood.Diet.ANIMAL).stock(3.0).starve(7.0).sexFmf(27.0, 21.0).eggs(3.0, 2, 4).grow(4.0, 0.35F).bucket().build();

    public static final HerpSpecies SLIDER = builder("slider_turtle", "Trachemys scripta", Body.TURTLE)
            .needs(TerrariumNeeds.builder(TerrariumNeeds.Habitat.PALUDARIUM).air(22, 32).water(22, 28).land(0.25, 0.75).uv(3).basking().minBlocks(12).build())
            .gait(new LandWalker.Gait(0.025, 0.045, false, 0.3, 0.45, 0.25, 0.15, 0.3))
            .diet(TerrariumFood.Diet.ANIMAL, TerrariumFood.Diet.GREENS).stock(8.0).starve(8.0).sex(29.2).eggs(3.5, 3, 6).grow(8.0, 0.3F)
            .varieties(2, "elegans", "scripta").bucket().build();

    public static final HerpSpecies HERMANN = builder("hermann_tortoise", "Testudo hermanni", Body.TURTLE)
            .needs(TerrariumNeeds.builder(TerrariumNeeds.Habitat.DRY).air(20, 32).humidity(35, 75).land(0.9, 1.0).uv(4).basking().minBlocks(8).build())
            .gait(new LandWalker.Gait(0.012, 0.0, true, 0.3, 0.0, 0.3, 0.25, 0.35))
            .diet(TerrariumFood.Diet.GREENS).stock(5.0).starve(10.0).sex(31.5).eggs(4.0, 2, 5).grow(8.0, 0.35F).brumates().build();

    public static final HerpSpecies REEVES = builder("reeves_turtle", "Mauremys reevesii", Body.TURTLE)
            .needs(TerrariumNeeds.builder(TerrariumNeeds.Habitat.PALUDARIUM).air(18, 30).water(18, 28).land(0.2, 0.6).uv(2).basking().minBlocks(6).build())
            .gait(new LandWalker.Gait(0.02, 0.035, false, 0.3, 0.5, 0.2, 0.2, 0.26))
            .diet(TerrariumFood.Diet.ANIMAL, TerrariumFood.Diet.GREENS).stock(4.0).starve(8.0).sex(28.5).eggs(3.5, 2, 5).grow(6.0, 0.35F).bucket().build();

    public static final HerpSpecies DART_FROG = builder("dart_frog", "Dendrobates", Body.FROG)
            .needs(TerrariumNeeds.builder(TerrariumNeeds.Habitat.PALUDARIUM).air(21, 28).humidity(75, 100).water(20, 28).land(0.5, 0.95).build())
            .gait(new LandWalker.Gait(0.03, 0.02, false, 0.3, 0.08, 0.0, 0.3, 0.08, LandWalker.Style.HOP, 0, false))
            .diet(TerrariumFood.Diet.SMALL_INSECTS).stock(0.5).starve(5.0).genetic(24.0).eggs(2.5, 3, 6).grow(5.0, 0.35F)
            .varieties(3, "tinctorius", "auratus", "leucomelas").pure().tadpoles().build();

    public static final HerpSpecies HORNED_FROG = builder("horned_frog", "Ceratophrys cranwelli", Body.FROG)
            .needs(TerrariumNeeds.builder(TerrariumNeeds.Habitat.DRY).air(23, 30).humidity(60, 95).land(0.9, 1.0).build())
            .gait(new LandWalker.Gait(0.016, 0.015, false, 0.3, 0.0, 0.0, 0.7, 0.22, LandWalker.Style.HOP, 0.85, true))
            .diet(TerrariumFood.Diet.INSECTS).stock(2.0).starve(12.0).genetic(26.0).grow(6.0, 0.4F).varieties(2, "green", "brown").noBreeding().build();

    public static final HerpSpecies LEOPARD_GECKO = builder("leopard_gecko", "Eublepharis macularius", Body.GECKO)
            .needs(TerrariumNeeds.builder(TerrariumNeeds.Habitat.DRY).air(24, 33).humidity(20, 55).land(0.9, 1.0).basking().build())
            .gait(new LandWalker.Gait(0.022, 0.0, true, 0.35, 0.0, 0.25, 0.35, 0.12))
            .diet(TerrariumFood.Diet.INSECTS).stock(2.0).starve(14.0).sexFmf(33.8, 30.5).eggs(4.0, 2, 2).grow(8.0, 0.4F)
            .varieties(1, "normal", "tangerine", "albino").morphs(0.04).build();

    public static final HerpSpecies CRESTED_GECKO = builder("crested_gecko", "Correlophus ciliatus", Body.GECKO)
            .needs(TerrariumNeeds.builder(TerrariumNeeds.Habitat.DRY).air(19, 27).humidity(55, 95).land(0.9, 1.0).build())
            .gait(new LandWalker.Gait(0.025, 0.0, true, 1.5, 0.0, 0.0, 0.25, 0.1, LandWalker.Style.CLIMB, 0.45, false))
            .diet(TerrariumFood.Diet.FRUIT, TerrariumFood.Diet.INSECTS, TerrariumFood.Diet.SMALL_INSECTS).stock(2.0).starve(10.0).genetic(24.0)
            .eggs(4.0, 2, 2).grow(8.0, 0.4F).varieties(1, "tiger", "flame", "harlequin").morphs(0.04).build();

    public static final HerpSpecies CORN_SNAKE = builder("corn_snake", "Pantherophis guttatus", Body.SNAKE)
            .needs(TerrariumNeeds.builder(TerrariumNeeds.Habitat.DRY).air(21, 31).humidity(35, 70).land(0.9, 1.0).basking().minBlocks(8).build())
            .gait(new LandWalker.Gait(0.02, 0.02, false, 0.4, 0.0, 0.25, 0.45, 0.08, LandWalker.Style.SLITHER, 0, false))
            .diet(TerrariumFood.Diet.PREY).stock(4.0).starve(40.0).meals(5.0).genetic(28.0).eggs(5.0, 4, 8).grow(10.0, 0.35F)
            .varieties(1, "wild", "amelanistic", "anerythristic").morphs(0.04).brumates().build();

    public static final List<HerpSpecies> TURTLES = List.of(MUSK, SLIDER, HERMANN, REEVES);
    public static final List<HerpSpecies> ALL = List.of(MUSK, SLIDER, HERMANN, REEVES, DART_FROG, HORNED_FROG, LEOPARD_GECKO, CRESTED_GECKO, CORN_SNAKE);

    /** Texture of a variety ({@code slider_turtle_scripta}); the plain id for a species with one variety. */
    public String texture(int variety) {
        if (varieties.size() <= 1) return id;
        return id + "_" + varieties.get(Math.max(0, Math.min(varieties.size() - 1, variety)));
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

    /** Females at both ends of the temperature range (the musk turtle, the leopard gecko). */
    public boolean patternFmf() {
        return tsd && Double.isFinite(lowPivot);
    }

    public boolean eats(TerrariumFood.Diet d) {
        return diet.contains(d);
    }

    public boolean turtle() {
        return body == Body.TURTLE;
    }

    @Nullable
    public static HerpSpecies byId(String id) {
        for (HerpSpecies s : ALL) if (s.id.equals(id)) return s;
        return null;
    }

    static Builder builder(String id, String latin, Body body) {
        return new Builder(id, latin, body);
    }

    /** Builds a species: a box, temperature sex, one wild variety and breeding unless set otherwise. */
    static final class Builder {
        private final String id, latin;
        private final Body body;
        private TerrariumNeeds needs;
        private LandWalker.Gait gait;
        private Set<TerrariumFood.Diet> diet = EnumSet.noneOf(TerrariumFood.Diet.class);
        private double stock = 1, starve = 7, meals, pivot = 28, lowPivot = Double.NaN, eggDays = 3, growDays = 6, morph;
        private boolean tsd = true, box = true, brumates, breeds = true, tadpoles, pure;
        private int eggsMin = 2, eggsMax = 4, wild = 1;
        private float hatchScale = 0.35F;
        private List<String> varieties = List.of("wild");

        Builder(String id, String latin, Body body) {
            this.id = id;
            this.latin = latin;
            this.body = body;
        }

        Builder needs(TerrariumNeeds n) { needs = n; return this; }
        Builder gait(LandWalker.Gait g) { gait = g; return this; }
        Builder diet(TerrariumFood.Diet... d) { diet = EnumSet.copyOf(List.of(d)); return this; }
        Builder stock(double s) { stock = s; return this; }
        Builder starve(double d) { starve = d; return this; }
        Builder meals(double d) { meals = d; return this; }
        Builder sex(double p) { tsd = true; pivot = p; return this; }
        Builder sexFmf(double p, double low) { tsd = true; pivot = p; lowPivot = low; return this; }
        Builder genetic(double reference) { tsd = false; pivot = reference; return this; }
        Builder eggs(double days, int min, int max) { eggDays = days; eggsMin = min; eggsMax = max; return this; }
        Builder grow(double days, float scale) { growDays = days; hatchScale = scale; return this; }
        Builder varieties(int wildOnes, String... v) { varieties = List.of(v); wild = wildOnes; return this; }
        Builder morphs(double chance) { morph = chance; return this; }
        Builder bucket() { box = false; return this; }
        Builder brumates() { brumates = true; return this; }
        Builder noBreeding() { breeds = false; return this; }
        Builder tadpoles() { tadpoles = true; return this; }
        Builder pure() { pure = true; return this; }

        HerpSpecies build() {
            return new HerpSpecies(id, latin, body, needs, gait, diet, stock, starve, meals, tsd, pivot, lowPivot, eggDays, eggsMin, eggsMax, box, brumates,
                    growDays, hatchScale, varieties, Math.max(1, Math.min(varieties.size(), wild)), breeds, tadpoles, pure, morph);
        }
    }
}
