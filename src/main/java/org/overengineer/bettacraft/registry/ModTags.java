package org.overengineer.bettacraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import org.overengineer.bettacraft.BettaCraft;

public final class ModTags {
    /** Glass blocks usable for tanks ({@code #c:glass_blocks}). */
    public static final TagKey<Block> TANK_GLASS_BLOCKS = Tags.Blocks.GLASS_BLOCKS;
    public static final TagKey<Item> TANK_GLASS_ITEMS = Tags.Items.GLASS_BLOCKS;

    /** Food the feeder and hand feeding accept. */
    public static final TagKey<Item> BETTA_FOOD = TagKey.create(Registries.ITEM, BettaCraft.id("betta_food"));
    /** Fry food for the whole fry stage (infusoria). */
    public static final TagKey<Item> FRY_FOOD_START = TagKey.create(Registries.ITEM, BettaCraft.id("fry_food_start"));
    /** Fry food for the second half of the fry stage (artemia nauplii). */
    public static final TagKey<Item> FRY_FOOD = TagKey.create(Registries.ITEM, BettaCraft.id("fry_food"));
    /** Live food: conditions adults for breeding (mosquito larvae). */
    /** M19: sinking food only the bottom fish eat (algae wafers). */
    public static final TagKey<Item> BOTTOM_FOOD = TagKey.create(Registries.ITEM, BettaCraft.id("bottom_food"));
    public static final TagKey<Item> LIVE_FOOD = TagKey.create(Registries.ITEM, BettaCraft.id("live_food"));
    /** Raw fish for betta pellets: cod, salmon and {@code #c:foods/raw_fish} (fish of other mods, M7). */
    public static final TagKey<Item> PELLET_FISH = TagKey.create(Registries.ITEM, BettaCraft.id("pellet_fish"));
    /** Plastic from other mods; non-empty switches the fish bag recipe to plastic (PLAN 4.3). */
    public static final TagKey<Item> PLASTIC_SHEETS = TagKey.create(Registries.ITEM, BettaCraft.id("plastic_sheets"));
    /** Organic matter the larva tub turns into mosquito larvae (M9): leaves, wheat, rotten flesh, bone meal. */
    public static final TagKey<Item> LARVA_TUB_FOOD = TagKey.create(Registries.ITEM, BettaCraft.id("larva_tub_food"));
    public static final TagKey<Item> ENTITY_WATER_BUCKETS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "buckets/entity_water"));

    /** Never accepted in tanks. */
    public static final TagKey<EntityType<?>> TANK_BLACKLIST = TagKey.create(Registries.ENTITY_TYPE, BettaCraft.id("tank_blacklist"));
    /** Always accepted in tanks (even if larger than the size rule). */
    public static final TagKey<EntityType<?>> TANK_INHABITANTS = TagKey.create(Registries.ENTITY_TYPE, BettaCraft.id("tank_inhabitants"));
    /** Kill bettas and other small inhabitants. */
    public static final TagKey<EntityType<?>> PREDATORS = TagKey.create(Registries.ENTITY_TYPE, BettaCraft.id("predators"));
    /** Live prey of other mods (Fintastic artemia, daphnia, copepods): hungry bettas in a tank eat it as live food (M7). */
    public static final TagKey<EntityType<?>> BETTA_PREY = TagKey.create(Registries.ENTITY_TYPE, BettaCraft.id("betta_prey"));
    public static final TagKey<EntityType<?>> C_FISH = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "fish"));
    public static final TagKey<EntityType<?>> C_FISHES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "fishes"));
    public static final TagKey<EntityType<?>> BFT_CATCHABLE = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("betterfishtanks", "catchable_fish"));

    /** Biomes with wild bettas; their water gives swamp water bottles. Contains {@code #c:is_swamp}. */
    public static final TagKey<Biome> BETTA_SPAWN_BIOMES = TagKey.create(Registries.BIOME, BettaCraft.id("betta_spawn_biomes"));

    /** Water in these biomes gives salt water bottles (oceans). */
    public static final TagKey<Biome> SALT_WATER_BIOMES = TagKey.create(Registries.BIOME, BettaCraft.id("salt_water_biomes"));
    /** Mining these blocks may drop artemia cysts. */
    public static final TagKey<Block> ARTEMIA_SAND = TagKey.create(Registries.BLOCK, BettaCraft.id("artemia_sand"));
    /** Floating plants in the interior: the bubble nest is built under them (PLAN 5.4). */
    /** M16: blocks that cannot be placed in the interior dimension (no tank inside a tank): our tank, Better Fish Tanks. */
    public static final TagKey<Block> NOT_IN_TANK_INTERIOR = TagKey.create(Registries.BLOCK, BettaCraft.id("not_in_tank_interior"));
    public static final TagKey<Block> FLOATING_PLANTS = TagKey.create(Registries.BLOCK, BettaCraft.id("floating_plants"));
    /**
     * Water plants of other mods that may stand in the dry interior (placed without water, never break), like seagrass
     * and kelp (PLAN 7.3; Fintastic plants in M7).
     */
    public static final TagKey<Block> INTERIOR_WATER_PLANTS = TagKey.create(Registries.BLOCK, BettaCraft.id("interior_water_plants"));
    /** Blocks that cannot become an interior floor substrate (PLAN 7.2). */
    public static final TagKey<Block> SUBSTRATE_BLACKLIST = TagKey.create(Registries.BLOCK, BettaCraft.id("substrate_blacklist"));

    /** M13: blocks epiphytes (Java fern, Anubias, Bucephalandra) grow on: stone and wood, as in the decoration. */
    public static final TagKey<Block> EPIPHYTE_SURFACES = TagKey.create(Registries.BLOCK, BettaCraft.id("epiphyte_surfaces"));
    /** M13: blocks rooted aquarium plants grow in besides the interior floor with a substrate: sand, gravel, dirt ... */
    public static final TagKey<Block> PLANT_SUBSTRATE = TagKey.create(Registries.BLOCK, BettaCraft.id("plant_substrate"));
    /** M20: where corals and the anemone can be glued on (reef rock, live rock, coral blocks). */
    public static final TagKey<Block> REEF_SURFACES = TagKey.create(Registries.BLOCK, BettaCraft.id("reef_surfaces"));
    /** M13: all aquarium plant items. */
    public static final TagKey<Item> AQUATIC_PLANT_ITEMS = TagKey.create(Registries.ITEM, BettaCraft.id("aquatic_plants"));

    private ModTags() {}
}
