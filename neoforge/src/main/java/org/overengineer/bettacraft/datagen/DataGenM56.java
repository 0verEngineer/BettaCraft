package org.overengineer.bettacraft.datagen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.overengineer.bettacraft.datagen.model.BlockStateProvider;
import org.overengineer.bettacraft.datagen.model.ConfiguredModel;
import org.overengineer.bettacraft.datagen.model.ItemModelProvider;
import org.overengineer.bettacraft.datagen.model.ModelFile;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.holdersets.AndHolderSet;
import net.neoforged.neoforge.registries.holdersets.OrHolderSet;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.platform.Tags;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.plants.wild.LandLook;
import org.overengineer.bettacraft.plants.wild.WildLandPlantBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModCodecs;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M56 data: the terrarium plants: their item models (an icon per look, tinted), the models of the plants in the world (a
 * shared model per {@link LandLook}, tinted), the wild terrarium plants (features, the biome modifier over the
 * {@code #bettacraft:*_plant_lands} tags and those tags), the soil they grow on, a few recipes from vanilla plants.
 */
final class DataGenM56 {
    private DataGenM56() {}

    static final ResourceKey<ConfiguredFeature<?, ?>> LAND_CONFIGURED = ResourceKey.create(Registries.CONFIGURED_FEATURE, BettaCraft.id("wild_land_plants"));
    static final ResourceKey<ConfiguredFeature<?, ?>> CAVE_LAND_CONFIGURED = ResourceKey.create(Registries.CONFIGURED_FEATURE, BettaCraft.id("wild_cave_land_plants"));
    static final ResourceKey<PlacedFeature> LAND_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, BettaCraft.id("wild_land_plants"));
    static final ResourceKey<PlacedFeature> CAVE_LAND_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, BettaCraft.id("wild_cave_land_plants"));
    static final ResourceKey<BiomeModifier> LAND_MODIFIER = ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, BettaCraft.id("wild_land_plants"));

    static void configuredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        ctx.register(LAND_CONFIGURED, new ConfiguredFeature<>(ModCodecs.WILD_LAND_PLANTS.get(), FeatureConfiguration.NONE));
        ctx.register(CAVE_LAND_CONFIGURED, new ConfiguredFeature<>(ModCodecs.WILD_CAVE_LAND_PLANTS.get(), FeatureConfiguration.NONE));
    }

    /** One try per chunk on the surface (the feature rolls its chance per place), three per chunk in the lush caves. */
    static void placedFeatures(BootstrapContext<PlacedFeature> ctx) {
        var cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
        ctx.register(LAND_PLACED, new PlacedFeature(cf.getOrThrow(LAND_CONFIGURED), List.of(
                InSquarePlacement.spread(), HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES), BiomeFilter.biome())));
        ctx.register(CAVE_LAND_PLACED, new PlacedFeature(cf.getOrThrow(CAVE_LAND_CONFIGURED), List.of(
                CountPlacement.of(3), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-56), VerticalAnchor.absolute(48)), BiomeFilter.biome())));
    }

    /** The land of the wild terrarium plants: the five tags, the lush caves also by the common tags. */
    static void biomeModifiers(BootstrapContext<BiomeModifier> ctx) {
        HolderGetter<Biome> b = ctx.lookup(Registries.BIOME);
        var biomes = new OrHolderSet<Biome>(b.getOrThrow(ModTags.JUNGLE_PLANT_LANDS), b.getOrThrow(ModTags.SWAMP_PLANT_LANDS),
                b.getOrThrow(ModTags.DESERT_PLANT_LANDS), b.getOrThrow(ModTags.SAVANNA_PLANT_LANDS), b.getOrThrow(ModTags.LUSH_CAVE_PLANT_LANDS),
                new AndHolderSet<>(b.getOrThrow(Tags.Biomes.IS_LUSH), b.getOrThrow(Tags.Biomes.IS_CAVE)));
        var pf = ctx.lookup(Registries.PLACED_FEATURE);
        ctx.register(LAND_MODIFIER, new BiomeModifiers.AddFeaturesBiomeModifier(biomes,
                HolderSet.direct(pf.getOrThrow(LAND_PLACED), pf.getOrThrow(CAVE_LAND_PLACED)), GenerationStep.Decoration.VEGETAL_DECORATION));
    }

    /** The biome tags (vanilla, the common tags and the known biome mods). */
    static void biomeTags(Function<TagKey<Biome>, net.minecraft.data.tags.TagAppender<Biome>> tag) {
        var jungle = tag.apply(ModTags.JUNGLE_PLANT_LANDS).addTag(BiomeTags.IS_JUNGLE);
        Compat.JUNGLE_BIOMES.forEach(id -> jungle.add(net.minecraft.tags.TagEntry.optionalElement(id)));
        tag.apply(ModTags.LUSH_CAVE_PLANT_LANDS).add(Biomes.LUSH_CAVES);
        var swamp = tag.apply(ModTags.SWAMP_PLANT_LANDS).add(Biomes.SWAMP, Biomes.MANGROVE_SWAMP).addTag(Tags.Biomes.IS_SWAMP);
        Compat.SWAMP_BIOMES.forEach(id -> swamp.add(net.minecraft.tags.TagEntry.optionalElement(id)));
        Compat.MANGROVE_BIOMES.forEach(id -> swamp.add(net.minecraft.tags.TagEntry.optionalElement(id)));
        tag.apply(ModTags.DESERT_PLANT_LANDS).addTag(Tags.Biomes.IS_DESERT).addTag(BiomeTags.IS_BADLANDS);
        var savanna = tag.apply(ModTags.SAVANNA_PLANT_LANDS).addTag(Tags.Biomes.IS_SAVANNA);
        Compat.TORTOISE_BIOMES.forEach(id -> savanna.add(net.minecraft.tags.TagEntry.optionalElement(id)));
    }

    /** The soil of the plants in the world: the plant substrate (sand, soil, gravel), terracotta, the moss block. */
    static void blockTags(Function<TagKey<Block>, ObjTags.Appender<Block>> tag) {
        tag.apply(ModTags.LAND_PLANT_SOIL).addTag(ModTags.PLANT_SUBSTRATE).addTag(BlockTags.TERRACOTTA)
                .add(net.minecraft.world.level.block.Blocks.MOSS_BLOCK, net.minecraft.world.level.block.Blocks.PACKED_MUD);
        var sword = tag.apply(BlockTags.SWORD_EFFICIENT);
        for (var b : ModBlocks.WILD_LAND_PLANTS.values()) sword.add(b.get());
    }

    static void itemTags(Function<TagKey<Item>, ObjTags.Appender<Item>> tag) {
        var t = tag.apply(ModTags.TERRARIUM_PLANT_ITEMS);
        for (var e : ModItems.AQUATIC_PLANTS.entrySet()) if (e.getKey().land()) t.add(e.getValue().get());
    }

    /** The item of a terrarium plant: its icon (leaves, tinted 0) and the detail (flowers, bracts, spines; tinted 1). */
    static void itemModel(ItemModelProvider p, PlantSpecies s) {
        String icon = LandLook.icon(s);
        p.withExistingParent(s.id(), p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/terrarium_" + icon))
                .texture("layer1", p.modLoc("item/terrarium_" + icon + "_detail"));
    }

    /** The template of a look (full grown or trimmed). */
    static String template(PlantSpecies s, boolean trimmed) {
        LandLook look = LandLook.of(s);
        String t = switch (look.model) {
            case CROSS -> "template_wild_plant";
            case MOSS -> "template_wild_moss";
            case CARPET -> "template_wild_carpet";
            case CACTUS -> "template_wild_cactus_" + look.id();
        };
        return trimmed ? t + "_short" : t;
    }

    static void blockStates(BlockStateProvider p) {
        Map<String, ModelFile> models = new HashMap<>();
        for (var e : ModBlocks.WILD_LAND_PLANTS.entrySet()) {
            PlantSpecies s = e.getKey();
            LandLook look = LandLook.of(s);
            p.getVariantBuilder(e.getValue().get()).forAllStates(state -> {
                boolean trimmed = state.getValue(WildLandPlantBlock.TRIMMED);
                String key = look.model == LandLook.Model.CARPET ? "carpet_" + LandLook.carpet(s) : look.id();
                String name = "wild_land_" + key + (trimmed ? "_trimmed" : "");
                ModelFile m = models.computeIfAbsent(name, n -> model(p, s, look, trimmed, n));
                if (look.model == LandLook.Model.MOSS || look.model == LandLook.Model.CARPET) return new ConfiguredModel[]{new ConfiguredModel(m)};
                return new ConfiguredModel[]{new ConfiguredModel(m), new ConfiguredModel(m, 0, 90, false), new ConfiguredModel(m, 0, 180, false),
                        new ConfiguredModel(m, 0, 270, false)};
            });
        }
    }

    private static ModelFile model(BlockStateProvider p, PlantSpecies s, LandLook look, boolean trimmed, String name) {
        var b = p.models().withExistingParent(name, p.modLoc("block/" + template(s, trimmed)));
        return switch (look.model) {
            case MOSS -> b.texture("moss_disc", p.modLoc("block/wild_moss_disc")).texture("moss_disc_small", p.modLoc("block/wild_moss_disc_small"))
                    .texture("marimo", p.modLoc("block/wild_marimo"));
            case CARPET -> {
                for (String v : DataGenM281.CARPET_TEXTURES) b.texture(v, p.modLoc("block/wild_carpet_" + LandLook.carpet(s) + "_" + v));
                yield b;
            }
            case CACTUS -> b.texture("body", p.modLoc("block/wild_land_cactus")).texture("crown", p.modLoc("block/wild_land_cactus_top"))
                    .texture("spines", p.modLoc("block/wild_land_cactus_spines"));
            case CROSS -> {
                String tex = "block/wild_land_" + look.id();
                yield b.texture("leaf", p.modLoc(tex)).texture("accent", p.modLoc(tex + "_accent")).texture("top", p.modLoc(tex + "_top"));
            }
        };
    }

    /** A few terrarium plants from vanilla plants (one each): cactus, fern, large fern, moss, dead bush, short grass. */
    static void recipes(RecipeOutput output) {
        Map<Item, PlantSpecies> map = new java.util.LinkedHashMap<>();
        map.put(Items.CACTUS, PlantSpecies.COLUMN_CACTUS);
        map.put(Items.FERN, PlantSpecies.BOSTON_FERN);
        map.put(Items.LARGE_FERN, PlantSpecies.BIRDS_NEST_FERN);
        map.put(Items.MOSS_BLOCK, PlantSpecies.CUSHION_MOSS);
        map.put(Items.DEAD_BUSH, PlantSpecies.NEEDLEGRASS);
        map.put(Items.SHORT_GRASS, PlantSpecies.SWAMP_SEDGE);
        for (var e : map.entrySet()) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.getKey()).getPath();
            ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.AQUATIC_PLANTS.get(e.getValue()).get())
                    .requires(e.getKey())
                    .unlockedBy("has_" + id, net.minecraft.advancements.triggers.InventoryChangeTrigger.TriggerInstance.hasItems(e.getKey()))
                    .save(output, RecipeCompat.key(BettaCraft.id("terrarium_plant_from_" + id)));
        }
    }
}
