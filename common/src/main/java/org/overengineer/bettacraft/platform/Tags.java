package org.overengineer.bettacraft.platform;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;

/**
 * M52: the convention tags ({@code c:}) the mod uses, with the same ids as NeoForge's {@code Tags} (Fabric uses the
 * same {@code c:} ids since 1.21). Generated list; add a constant here when the code needs another one.
 */
public final class Tags {
    private Tags() {}

    public static final class Items {
        private Items() {}

        public static final TagKey<net.minecraft.world.item.Item> BUCKETS_WATER = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "buckets/water"));
        public static final TagKey<net.minecraft.world.item.Item> CHESTS_WOODEN = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "chests/wooden"));
        public static final TagKey<net.minecraft.world.item.Item> COBBLESTONES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "cobblestones"));
        public static final TagKey<net.minecraft.world.item.Item> CONCRETES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "concretes"));
        public static final TagKey<net.minecraft.world.item.Item> DUSTS_GLOWSTONE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "dusts/glowstone"));
        public static final TagKey<net.minecraft.world.item.Item> DUSTS_REDSTONE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "dusts/redstone"));
        public static final TagKey<net.minecraft.world.item.Item> DYES_BLUE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "dyes/blue"));
        public static final TagKey<net.minecraft.world.item.Item> DYES_BROWN = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "dyes/brown"));
        public static final TagKey<net.minecraft.world.item.Item> DYES_LIME = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "dyes/lime"));
        public static final TagKey<net.minecraft.world.item.Item> DYES_ORANGE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "dyes/orange"));
        public static final TagKey<net.minecraft.world.item.Item> DYES_RED = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "dyes/red"));
        public static final TagKey<net.minecraft.world.item.Item> DYES_YELLOW = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "dyes/yellow"));
        public static final TagKey<net.minecraft.world.item.Item> END_STONES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "end_stones"));
        public static final TagKey<net.minecraft.world.item.Item> FOODS_RAW_FISH = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "foods/raw_fish"));
        public static final TagKey<net.minecraft.world.item.Item> FOODS_RAW_MEAT = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "foods/raw_meat"));
        public static final TagKey<net.minecraft.world.item.Item> GEMS_QUARTZ = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "gems/quartz"));
        public static final TagKey<net.minecraft.world.item.Item> GLASS_BLOCKS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "glass_blocks"));
        public static final TagKey<net.minecraft.world.item.Item> GLASS_BLOCKS_CHEAP = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "glass_blocks/cheap"));
        public static final TagKey<net.minecraft.world.item.Item> GLASS_BLOCKS_COLORLESS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "glass_blocks/colorless"));
        public static final TagKey<net.minecraft.world.item.Item> GLASS_PANES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "glass_panes"));
        public static final TagKey<net.minecraft.world.item.Item> GLAZED_TERRACOTTAS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "glazed_terracottas"));
        public static final TagKey<net.minecraft.world.item.Item> INGOTS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots"));
        public static final TagKey<net.minecraft.world.item.Item> INGOTS_COPPER = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/copper"));
        public static final TagKey<net.minecraft.world.item.Item> INGOTS_GOLD = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/gold"));
        public static final TagKey<net.minecraft.world.item.Item> INGOTS_IRON = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/iron"));
        public static final TagKey<net.minecraft.world.item.Item> NUGGETS_GOLD = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "nuggets/gold"));
        public static final TagKey<net.minecraft.world.item.Item> NUGGETS_IRON = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "nuggets/iron"));
        public static final TagKey<net.minecraft.world.item.Item> OBSIDIANS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "obsidians"));
        public static final TagKey<net.minecraft.world.item.Item> ORES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ores"));
        public static final TagKey<net.minecraft.world.item.Item> RODS_WOODEN = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "rods/wooden"));
        public static final TagKey<net.minecraft.world.item.Item> SANDS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "sands"));
        public static final TagKey<net.minecraft.world.item.Item> SANDSTONE_BLOCKS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "sandstone/blocks"));
        public static final TagKey<net.minecraft.world.item.Item> SLIME_BALLS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "slime_balls"));
        public static final TagKey<net.minecraft.world.item.Item> STONES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "stones"));
        public static final TagKey<net.minecraft.world.item.Item> STORAGE_BLOCKS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "storage_blocks"));
        public static final TagKey<net.minecraft.world.item.Item> STRINGS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "strings"));
        public static final TagKey<net.minecraft.world.item.Item> TOOLS_SHEAR = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "tools/shear"));
    }

    public static final class Blocks {
        private Blocks() {}

        public static final TagKey<net.minecraft.world.level.block.Block> COBBLESTONES = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "cobblestones"));
        public static final TagKey<net.minecraft.world.level.block.Block> GLASS_BLOCKS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "glass_blocks"));
        public static final TagKey<net.minecraft.world.level.block.Block> GRAVELS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "gravels"));
        public static final TagKey<net.minecraft.world.level.block.Block> SANDS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "sands"));
    }

    public static final class Biomes {
        private Biomes() {}

        public static final TagKey<net.minecraft.world.level.biome.Biome> IS_BEACH = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", "is_beach"));
        public static final TagKey<net.minecraft.world.level.biome.Biome> IS_CAVE = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", "is_cave"));
        public static final TagKey<net.minecraft.world.level.biome.Biome> IS_COLD_OVERWORLD = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", "is_cold/overworld"));
        public static final TagKey<net.minecraft.world.level.biome.Biome> IS_DESERT = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", "is_desert"));
        public static final TagKey<net.minecraft.world.level.biome.Biome> IS_HOT = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", "is_hot"));
        public static final TagKey<net.minecraft.world.level.biome.Biome> IS_LUSH = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", "is_lush"));
        public static final TagKey<net.minecraft.world.level.biome.Biome> IS_OCEAN = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", "is_ocean"));
        public static final TagKey<net.minecraft.world.level.biome.Biome> IS_SAVANNA = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", "is_savanna"));
        public static final TagKey<net.minecraft.world.level.biome.Biome> IS_SWAMP = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", "is_swamp"));
    }

    public static final class EntityTypes {
        private EntityTypes() {}

        public static final TagKey<net.minecraft.world.entity.EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("c", "bosses"));
    }
}
