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
    /** Plastic from other mods; non-empty switches the fish bag recipe to plastic (PLAN 4.3). */
    public static final TagKey<Item> PLASTIC_SHEETS = TagKey.create(Registries.ITEM, BettaCraft.id("plastic_sheets"));
    public static final TagKey<Item> ENTITY_WATER_BUCKETS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "buckets/entity_water"));

    /** Never accepted in tanks. */
    public static final TagKey<EntityType<?>> TANK_BLACKLIST = TagKey.create(Registries.ENTITY_TYPE, BettaCraft.id("tank_blacklist"));
    /** Always accepted in tanks (even if larger than the size rule). */
    public static final TagKey<EntityType<?>> TANK_INHABITANTS = TagKey.create(Registries.ENTITY_TYPE, BettaCraft.id("tank_inhabitants"));
    /** Kill bettas and other small inhabitants (behaviour in M2). */
    public static final TagKey<EntityType<?>> PREDATORS = TagKey.create(Registries.ENTITY_TYPE, BettaCraft.id("predators"));
    public static final TagKey<EntityType<?>> C_FISH = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "fish"));
    public static final TagKey<EntityType<?>> C_FISHES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "fishes"));
    public static final TagKey<EntityType<?>> BFT_CATCHABLE = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("betterfishtanks", "catchable_fish"));

    /** Biomes with wild bettas; their water gives swamp water bottles. Contains {@code #c:is_swamp}. */
    public static final TagKey<Biome> BETTA_SPAWN_BIOMES = TagKey.create(Registries.BIOME, BettaCraft.id("betta_spawn_biomes"));

    private ModTags() {}
}
