package org.overengineer.bettacraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import org.overengineer.bettacraft.platform.Tags;
import org.overengineer.bettacraft.BettaCraft;

public final class ModTags {
    /** Glass blocks usable for tanks ({@code #c:glass_blocks}). */
    public static final TagKey<Block> TANK_GLASS_BLOCKS = Tags.Blocks.GLASS_BLOCKS;
    public static final TagKey<Item> TANK_GLASS_ITEMS = Tags.Items.GLASS_BLOCKS;

    /** Food the feeder and hand feeding accept. */
    /** M24: stainless steel (ours; the steel of other mods, #c:ingots/steel, is no stainless steel). */
    public static final TagKey<Item> STAINLESS_STEEL_INGOTS = TagKey.create(Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/stainless_steel"));
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
    /** M26: live food that conditions twice as well (tubifex). */
    public static final TagKey<Item> CONDITIONING_FOOD = TagKey.create(Registries.ITEM, BettaCraft.id("conditioning_food"));
    /** M26: what feeds a tubifex dish (mulm). */
    public static final TagKey<Item> TUBIFEX_FOOD = TagKey.create(Registries.ITEM, BettaCraft.id("tubifex_food"));
    /** M26: what feeds a daphnia barrel (yeast, green water). */
    public static final TagKey<Item> DAPHNIA_FOOD = TagKey.create(Registries.ITEM, BettaCraft.id("daphnia_food"));
    /** M39: what starts a daphnia barrel (our daphnia, Fintastic's). */
    public static final TagKey<Item> DAPHNIA_STARTERS = TagKey.create(Registries.ITEM, BettaCraft.id("daphnia_starters"));
    /** M26 part b: frozen food (live food frozen with ice): eaten like live food, but not pumped. */
    public static final TagKey<Item> FROZEN_FOOD = TagKey.create(Registries.ITEM, BettaCraft.id("frozen_food"));
    /** M29 part b: hard food that wears down the teeth of the puffers (shell food). */
    public static final TagKey<Item> HARD_FOOD = TagKey.create(Registries.ITEM, BettaCraft.id("hard_food"));
    /** M26 part b: food only the shrimp, crayfish and snails eat (shrimp food). */
    public static final TagKey<Item> CRITTER_FOOD = TagKey.create(Registries.ITEM, BettaCraft.id("critter_food"));
    /** M26 part b: food that sinks to the bottom fish (granules, wafers): they get their share even next to fast fish. */
    public static final TagKey<Item> SINKING_FOOD = TagKey.create(Registries.ITEM, BettaCraft.id("sinking_food"));
    /** M26 part b: vegetables for the food maker (carrot, pumpkin, melon, beetroot, {@code #c:foods/vegetable}). */
    public static final TagKey<Item> FOOD_VEGETABLES = TagKey.create(Registries.ITEM, BettaCraft.id("food_vegetables"));
    /** M26 part b: ice for frozen food (ice, packed ice, blue ice, snow blocks). */
    public static final TagKey<Item> FREEZING_ICE = TagKey.create(Registries.ITEM, BettaCraft.id("freezing_ice"));
    public static final TagKey<Item> ENTITY_WATER_BUCKETS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "buckets/entity_water"));

    /** Never accepted in tanks. */
    public static final TagKey<EntityType<?>> TANK_BLACKLIST = TagKey.create(Registries.ENTITY_TYPE, BettaCraft.id("tank_blacklist"));
    /** Always accepted in tanks (even if larger than the size rule). */
    public static final TagKey<EntityType<?>> TANK_INHABITANTS = TagKey.create(Registries.ENTITY_TYPE, BettaCraft.id("tank_inhabitants"));
    /** M30: big animals of other mods (and the vanilla dolphin) that go into big tanks by the size rule (length from the hitbox). */
    public static final TagKey<EntityType<?>> BIG_TANK_ANIMALS = TagKey.create(Registries.ENTITY_TYPE, BettaCraft.id("big_tank_animals"));
    /** Kill bettas and other small inhabitants. */
    public static final TagKey<EntityType<?>> PREDATORS = TagKey.create(Registries.ENTITY_TYPE, BettaCraft.id("predators"));
    /** Live prey of other mods (Fintastic artemia, daphnia, copepods): hungry bettas in a tank eat it as live food (M7). */
    public static final TagKey<EntityType<?>> BETTA_PREY = TagKey.create(Registries.ENTITY_TYPE, BettaCraft.id("betta_prey"));
    /** M28.1: every animal entity of the mod (betta, dwarf clawed frog, wild small animals, every tankmate and seahorse). */
    public static final TagKey<EntityType<?>> ALL_ANIMALS = TagKey.create(Registries.ENTITY_TYPE, BettaCraft.id("all_animals"));
    /** M28.1: the animals of the mod that spawn in the world by themselves (default config). */
    public static final TagKey<EntityType<?>> WILD_ANIMALS = TagKey.create(Registries.ENTITY_TYPE, BettaCraft.id("wild_animals"));
    public static final TagKey<EntityType<?>> C_FISH = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "fish"));
    public static final TagKey<EntityType<?>> C_FISHES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "fishes"));
    public static final TagKey<EntityType<?>> BFT_CATCHABLE = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("betterfishtanks", "catchable_fish"));

    /** Biomes with wild bettas; their water gives swamp water bottles. Contains {@code #c:is_swamp}. */
    public static final TagKey<Biome> BETTA_SPAWN_BIOMES = TagKey.create(Registries.BIOME, BettaCraft.id("betta_spawn_biomes"));

    // M25.1: the waters of the wild animals, the net and fishing by biome (see world/BiomeWaters); vanilla biomes, the
    // common tags and the biomes of known biome mods (optional entries), for modpacks to extend in one place
    /** Coral reefs: the reef fish, the reef net catches. Also every biome in both {@code #c:is_ocean} and {@code #c:is_hot}. */
    public static final TagKey<Biome> WARM_OCEAN_WATERS = TagKey.create(Registries.BIOME, BettaCraft.id("warm_ocean_waters"));
    /** Seahorses and the lukewarm ocean animals. */
    public static final TagKey<Biome> LUKEWARM_OCEAN_WATERS = TagKey.create(Registries.BIOME, BettaCraft.id("lukewarm_ocean_waters"));
    /** Nerites, racer nerites and the mangrove plants. Checked before the swamps. */
    public static final TagKey<Biome> MANGROVE_WATERS = TagKey.create(Registries.BIOME, BettaCraft.id("mangrove_waters"));
    /** Trumpet snails and ramshorns of the lush caves. Also every biome in both {@code #c:is_lush} and {@code #c:is_cave}. */
    public static final TagKey<Biome> LUSH_CAVE_WATERS = TagKey.create(Registries.BIOME, BettaCraft.id("lush_cave_waters"));
    /** Swamps ({@code #c:is_swamp}). */
    public static final TagKey<Biome> SWAMP_WATERS = TagKey.create(Registries.BIOME, BettaCraft.id("swamp_waters"));
    /** Rivers ({@code #minecraft:is_river}). */
    public static final TagKey<Biome> RIVER_WATERS = TagKey.create(Registries.BIOME, BettaCraft.id("river_waters"));
    /** Beaches, brackish water ({@code #minecraft:is_beach}). */
    public static final TagKey<Biome> BEACH_WATERS = TagKey.create(Registries.BIOME, BettaCraft.id("beach_waters"));
    /**
     * M28.1: cold fresh water (the wild Marimo balls): frozen rivers and the lakes of taigas and snowy plains. Also every
     * land biome in {@code #c:is_cold/overworld} ({@link org.overengineer.bettacraft.world.BiomeWaters#coldWater}).
     */
    public static final TagKey<Biome> COLD_WATERS = TagKey.create(Registries.BIOME, BettaCraft.id("cold_waters"));
    /** Jungle water ({@code #minecraft:is_jungle}). */
    public static final TagKey<Biome> JUNGLE_WATERS = TagKey.create(Registries.BIOME, BettaCraft.id("jungle_waters"));
    /** The wild corals grow here (world generation, new chunks). Contains {@code #bettacraft:warm_ocean_waters}. */
    public static final TagKey<Biome> CORAL_REEF_BIOMES = TagKey.create(Registries.BIOME, BettaCraft.id("coral_reef_biomes"));

    /** M31 part b: Hermann's tortoise lives wild here (savannas, dry shrubland). */
    public static final TagKey<Biome> TORTOISE_LANDS = TagKey.create(Registries.BIOME, BettaCraft.id("tortoise_lands"));
    /** M31 part b: turtles lay their eggs into these blocks on the land of a tank (sand and soil). */
    public static final TagKey<Block> TURTLE_NEST_GROUND = TagKey.create(Registries.BLOCK, BettaCraft.id("turtle_nest_ground"));
    /** M31 part b: greens for the turtles and the tortoise (dandelion, clover, herbs: dandelion, pink petals, short grass, fern, oxeye daisy). */
    public static final TagKey<net.minecraft.world.item.Item> TURTLE_GREENS = TagKey.create(Registries.ITEM, BettaCraft.id("turtle_greens"));
    /** M31 part c: crickets for the horned frog and the geckos. */
    public static final TagKey<net.minecraft.world.item.Item> FEEDER_INSECTS = TagKey.create(Registries.ITEM, BettaCraft.id("feeder_insects"));
    /** M31 part c: fruit flies for the dart frogs and the crested gecko. */
    public static final TagKey<net.minecraft.world.item.Item> SMALL_FEEDER_INSECTS = TagKey.create(Registries.ITEM, BettaCraft.id("small_feeder_insects"));
    /** M31 part c: soft fruit for the crested gecko (sweet berries, glow berries, melon). */
    public static final TagKey<net.minecraft.world.item.Item> GECKO_FRUIT = TagKey.create(Registries.ITEM, BettaCraft.id("gecko_fruit"));
    /** M31 part c: prey for the corn snake (raw rabbit and chicken, for the frozen mice of the hobby). */
    public static final TagKey<net.minecraft.world.item.Item> SNAKE_PREY = TagKey.create(Registries.ITEM, BettaCraft.id("snake_prey"));
    /** M31 part d: capture items of other mods whose mob goes into a tank (besides the EnderIO soul vial and the IF mob imprisonment tool). */
    public static final TagKey<net.minecraft.world.item.Item> MOB_CAPTURE_ITEMS = TagKey.create(Registries.ITEM, BettaCraft.id("mob_capture_items"));
    /** M31 part c: dart frogs lay their eggs onto these blocks on the land (moss, leaves, soil). */
    public static final TagKey<Block> FROG_NEST_GROUND = TagKey.create(Registries.BLOCK, BettaCraft.id("frog_nest_ground"));
    /** M31 part c: the leopard gecko lives wild here (deserts and badlands). */
    public static final TagKey<Biome> ARID_LANDS = TagKey.create(Registries.BIOME, BettaCraft.id("arid_lands"));
    /** M31 part c: the corn snake lives wild here (temperate forests and plains). */
    public static final TagKey<Biome> SNAKE_LANDS = TagKey.create(Registries.BIOME, BettaCraft.id("snake_lands"));

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
    /**
     * M22.1: blocks that sway in the tank miniature outside (kelp, seagrass, corals, coral fans by default; modpacks and
     * mods add their water plants; others are found automatically, config swaying.autoDetect).
     */
    public static final TagKey<Block> SWAYING_PLANTS = TagKey.create(Registries.BLOCK, BettaCraft.id("swaying_plants"));
    /** Blocks that cannot become an interior floor substrate (PLAN 7.2). */
    public static final TagKey<Block> SUBSTRATE_BLACKLIST = TagKey.create(Registries.BLOCK, BettaCraft.id("substrate_blacklist"));

    /** M13: blocks epiphytes (Java fern, Anubias, Bucephalandra) grow on: stone and wood, as in the decoration. */
    public static final TagKey<Block> EPIPHYTE_SURFACES = TagKey.create(Registries.BLOCK, BettaCraft.id("epiphyte_surfaces"));
    /** M13: blocks rooted aquarium plants grow in besides the interior floor with a substrate: sand, gravel, dirt ... */
    public static final TagKey<Block> PLANT_SUBSTRATE = TagKey.create(Registries.BLOCK, BettaCraft.id("plant_substrate"));
    /** M25: substrate that lowers the pH and softens the water (soil, mud). */
    public static final TagKey<Block> ACID_SUBSTRATE = TagKey.create(Registries.BLOCK, BettaCraft.id("acid_substrate"));
    /** M25 part d: active soil: as substrate it lowers the pH and the hardness at full strength (the soil tag counts half). */
    public static final TagKey<Block> ACTIVE_SOIL = TagKey.create(Registries.BLOCK, BettaCraft.id("active_soil"));
    /** M25: limestone: as substrate or rocks in the tank it raises the pH and hardens the water. */
    public static final TagKey<Block> LIMESTONE = TagKey.create(Registries.BLOCK, BettaCraft.id("limestone"));
    /** M25: driftwood in the tank (logs, roots): releases tannins, lowers the pH a little. */
    public static final TagKey<Block> DRIFTWOOD = TagKey.create(Registries.BLOCK, BettaCraft.id("driftwood"));
    /** M28: clay and terracotta in a tank interior: the young shrimp hide in its holes and tubes ({@code features.decor.effects}). */
    public static final TagKey<Block> SHRIMP_SHELTER = TagKey.create(Registries.BLOCK, BettaCraft.id("shrimp_shelter"));
    /** M29: blocks of the small caves the Apistogramma females spawn in (the coconut shell, the slate cave). */
    public static final TagKey<Block> SMALL_CAVES = TagKey.create(Registries.BLOCK, BettaCraft.id("small_caves"));
    /** M20: where corals and the anemone can be glued on (reef rock, live rock, coral blocks). */
    public static final TagKey<Block> REEF_SURFACES = TagKey.create(Registries.BLOCK, BettaCraft.id("reef_surfaces"));
    /** M13: all aquarium plant items. */
    public static final TagKey<Item> AQUATIC_PLANT_ITEMS = TagKey.create(Registries.ITEM, BettaCraft.id("aquatic_plants"));

    /** M21.7: stone blocks a cabinet can be made of (five of them, three of their slabs and a chest); modpacks can add to it. */
    public static final TagKey<Item> CABINET_STONES = TagKey.create(Registries.ITEM, BettaCraft.id("cabinet_stones"));
    /** M21.7: never a cabinet stone (copper, ores, glass, purpur, prismarine, concrete, mud bricks ...). */
    public static final TagKey<Item> CABINET_EXCLUDED = TagKey.create(Registries.ITEM, BettaCraft.id("cabinet_excluded"));

    /** M25: fires whose flue gas the CO2 filling station collects while they burn (LIT): furnace, smoker, blast furnace, campfires. */
    public static final TagKey<Block> FLUE_GAS_SOURCES = TagKey.create(Registries.BLOCK, BettaCraft.id("flue_gas_sources"));
    /** M25: CO2 gas of other mods (fluids) the filling station takes. */
    public static final TagKey<net.minecraft.world.level.material.Fluid> CO2_GAS = TagKey.create(Registries.FLUID, BettaCraft.id("co2_gas"));

    private ModTags() {}
}
