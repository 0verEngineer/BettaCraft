package org.overengineer.bettacraft.datagen;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * Ids of other mods used in our tags (M7). All entries are optional ({@code "required": false}); the versions checked
 * are the ones of ATM10 (Better Fish Tanks 1.4.2, Fintastic 3.3, Fish of Thieves 21.1.3.0, Aquaculture 2.7.22,
 * Naturalist 2.0.7). A unit test checks that none of our own ids contain the words Better Fish Tanks uses to remove
 * other aquariums.
 */
public final class Compat {
    public static final String BFT = "betterfishtanks", FINTASTIC = "fintastic", FOT = "fishofthieves",
            AQUACULTURE = "aquaculture", NATURALIST = "naturalist";
    /** M16: other mods of the ATM10 pack with water animals (see meilensteine/M16.md for the whole inventory). */
    public static final String SHINY = "shiny", DEEPER_DARKER = "deeperdarker", CATACLYSM = "cataclysm", CRITTERS = "crittersandcompanions",
            LIVING_THINGS = "livingthings", SUSHI = "sushigocrafting", ALEXS_CAVES = "alexscaves";

    /**
     * M25.1: biomes of biome mods in our water tags where their own tags don't already say it (rivers, beaches, swamps
     * and jungles mostly come through the common tags). Read from the jars: Regions Unexplored 0.6.3 and Twilight Forest
     * 4.8 (ATM10AA), Oh The Biomes We've Gone 2.5.1 (ATM10), Biomes O' Plenty 19 and Terralith 2.5 (1.20.1, same ids).
     */
    public static final String RU = "regions_unexplored", BWG = "biomeswevegone", BOP = "biomesoplenty", TF = "twilightforest";
    public static final List<ResourceLocation> WARM_OCEAN_BIOMES = List.of(rl(RU, "rocky_reef"));
    public static final List<ResourceLocation> MANGROVE_BIOMES = List.of(rl(BWG, "white_mangrove_marshes"));
    public static final List<ResourceLocation> SWAMP_BIOMES = List.of(rl(TF, "swamp"));
    public static final List<ResourceLocation> RIVER_BIOMES = List.of(rl(TF, "stream"));
    public static final List<ResourceLocation> JUNGLE_BIOMES = List.of(rl(RU, "tropics"), rl(BOP, "tropics"));
    /** M31 part b: dry shrubland and steppe of the biome mods (#bettacraft:tortoise_lands; their savannas come with #c:is_savanna). */
    public static final List<ResourceLocation> TORTOISE_BIOMES = List.of(rl(RU, "shrubland"), rl(RU, "dry_bushland"), rl(RU, "steppe"),
            rl(BWG, "allium_shrubland"), rl(BWG, "firecracker_chaparral"));

    /** Predators: kill bettas and small inhabitants (PLAN 4.3). */
    public static final List<ResourceLocation> PREDATORS = List.of(
            rl(NATURALIST, "piranha"), rl(NATURALIST, "anglerfish"),
            rl(FOT, "battlegill"), rl(FOT, "wrecker"), rl(FOT, "devilfish"),
            rl(AQUACULTURE, "piranha"),
            rl(FINTASTIC, "freshwater_shark"), rl(FINTASTIC, "catfish"), rl(FINTASTIC, "featherback"),
            // M16: the shiny variants of vanilla predators, the angler fish of the Otherside, the Cataclysm lionfish (hostile,
            // only in tanks when allowed in the config)
            rl(SHINY, "shiny_pufferfish"), rl(SHINY, "shiny_axolotl"), rl(DEEPER_DARKER, "angler_fish"), rl(CATACLYSM, "lionfish"));

    /** Never in our tanks: giant fish whose hitbox is small (Aquaculture uses at most 0.8 x 0.4 for every fish). */
    public static final List<ResourceLocation> BLACKLIST = List.of(
            rl(FINTASTIC, "arapaima"), rl(FINTASTIC, "coelacanth"),
            rl(AQUACULTURE, "arapaima"), rl(AQUACULTURE, "tuna"), rl(AQUACULTURE, "pacific_halibut"),
            rl(AQUACULTURE, "atlantic_halibut"), rl(AQUACULTURE, "red_grouper"), rl(AQUACULTURE, "muskellunge"),
            rl(AQUACULTURE, "gar"), rl(AQUACULTURE, "catfish"), rl(AQUACULTURE, "bayad"), rl(AQUACULTURE, "capitaine"),
            rl(AQUACULTURE, "tambaqui"), rl(AQUACULTURE, "carp"), rl(AQUACULTURE, "pollock"),
            rl(AQUACULTURE, "atlantic_cod"), rl(AQUACULTURE, "pink_salmon"), rl(AQUACULTURE, "blackfish"),
            // M16: manta ray, tuna; land and lava pets of Cataclysm in buckets (the baby leviathan follows and teleports to its
            // owner) and the frogs (no tank animals; M35 part b: the otter of Critters and Companions has a paludarium profile now and
            // left the list: the profile keeps it out of aquariums). Fintastic gouramis go by the size
            // of their variant (the giant gourami is too big, three-spot and dwarf gouramis fit).
            rl(LIVING_THINGS, "mantaray"), rl(SUSHI, "tuna"),
            rl(CATACLYSM, "the_baby_leviathan"), rl(CATACLYSM, "modern_remnant"), rl(CATACLYSM, "netherite_ministrosity"),
            rl(SHINY, "shiny_frog"));

    /** Typical aquarium animals that are a bit larger than the size rule allows. */
    public static final List<ResourceLocation> INHABITANTS = List.of(rl(FINTASTIC, "pleco"));

    /** Live prey that hungry bettas eat in our tanks. */
    public static final List<ResourceLocation> PREY = List.of(
            rl(FINTASTIC, "artemia"), rl(FINTASTIC, "daphnia"), rl(FINTASTIC, "copepod"));

    /** Fish food of other mods that bettas eat like pellets. */
    public static final List<ResourceLocation> FOOD = List.of(
            rl(FINTASTIC, "regular_feed"), rl(FINTASTIC, "quality_feed"), rl(FINTASTIC, "great_feed"),
            rl(FINTASTIC, "premium_feed"), rl(BFT, "pellet_mix"));

    /** Live food of other mods (conditions for breeding like mosquito larvae). */
    public static final List<ResourceLocation> LIVE_FOOD = List.of(
            rl(AQUACULTURE, "worm"), rl(FOT, "earthworms"), rl(BFT, "raw_minishrimp"));

    /** Fintastic water plants that may stand in the dry interior. */
    public static final List<String> FINTASTIC_PLANTS = List.of(
            "duckweed", "hornwort", "anubias", "amazon_sword", "tall_amazon_sword", "blue_hypnea", "mermaid_fan",
            "caulerpa", "red_algae", "red_algae_fan", "red_algae_wall_fan", "dragons_breath_algae",
            "aquatic_moss_phyllid", "aquatic_moss_carpet", "green_algae_carpet", "red_algae_carpet",
            "sea_grapes", "sea_grapes_plant", "stromatolite_growths", "fossil_stromatolite_growths",
            "lotus", "lotus_flower", "lotus_pad");

    private Compat() {}

    public static ResourceLocation rl(String ns, String path) {
        return ResourceLocation.fromNamespaceAndPath(ns, path);
    }
}
