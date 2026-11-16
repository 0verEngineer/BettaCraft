package org.overengineer.bettacraft.datagen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
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
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.holdersets.AndHolderSet;
import net.neoforged.neoforge.registries.holdersets.OrHolderSet;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.plants.wild.WildFloatingPlantBlock;
import org.overengineer.bettacraft.plants.wild.WildPlantBlock;
import org.overengineer.bettacraft.plants.wild.WildPlantKind;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModCodecs;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;

/**
 * M28.1 data: the models of the wild plant blocks (a shared model per growth form and leaf shape, see
 * {@link WildPlantBlock#look}; the colours come from the block colour handler), the wild floating plants, the features,
 * the biome modifier over the {@code #bettacraft:*_waters} tags and the cold waters tag.
 */
final class DataGenM281 {
    private DataGenM281() {}

    static final ResourceKey<ConfiguredFeature<?, ?>> WILD_PLANTS_CONFIGURED = ResourceKey.create(Registries.CONFIGURED_FEATURE, BettaCraft.id("wild_plants"));
    static final ResourceKey<ConfiguredFeature<?, ?>> WILD_CAVE_PLANTS_CONFIGURED = ResourceKey.create(Registries.CONFIGURED_FEATURE, BettaCraft.id("wild_cave_plants"));
    static final ResourceKey<PlacedFeature> WILD_PLANTS_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, BettaCraft.id("wild_plants"));
    static final ResourceKey<PlacedFeature> WILD_CAVE_PLANTS_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, BettaCraft.id("wild_cave_plants"));
    static final ResourceKey<BiomeModifier> WILD_PLANTS_MODIFIER = ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, BettaCraft.id("wild_plants"));

    static void configuredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        ctx.register(WILD_PLANTS_CONFIGURED, new ConfiguredFeature<>(ModCodecs.WILD_PLANTS.get(), FeatureConfiguration.NONE));
        ctx.register(WILD_CAVE_PLANTS_CONFIGURED, new ConfiguredFeature<>(ModCodecs.WILD_CAVE_PLANTS.get(), FeatureConfiguration.NONE));
    }

    /** One try per chunk on the floor (the feature rolls its chance per water), three tries per chunk in the caves. */
    static void placedFeatures(BootstrapContext<PlacedFeature> ctx) {
        var cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
        ctx.register(WILD_PLANTS_PLACED, new PlacedFeature(cf.getOrThrow(WILD_PLANTS_CONFIGURED), List.of(
                InSquarePlacement.spread(), HeightmapPlacement.onHeightmap(Heightmap.Types.OCEAN_FLOOR_WG), BiomeFilter.biome())));
        ctx.register(WILD_CAVE_PLANTS_PLACED, new PlacedFeature(cf.getOrThrow(WILD_CAVE_PLANTS_CONFIGURED), List.of(
                CountPlacement.of(3), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-56), VerticalAnchor.absolute(48)), BiomeFilter.biome())));
    }

    /**
     * The waters of the wild plants: jungle, swamp, river, mangrove, cold and lush cave waters, plus the common tags the
     * {@code BiomeWaters} checks add (lush caves, cold land biomes). The feature itself picks the water per biome.
     */
    static void biomeModifiers(BootstrapContext<BiomeModifier> ctx) {
        HolderGetter<Biome> b = ctx.lookup(Registries.BIOME);
        var biomes = new OrHolderSet<Biome>(b.getOrThrow(ModTags.JUNGLE_WATERS), b.getOrThrow(ModTags.SWAMP_WATERS), b.getOrThrow(ModTags.RIVER_WATERS),
                b.getOrThrow(ModTags.MANGROVE_WATERS), b.getOrThrow(ModTags.COLD_WATERS), b.getOrThrow(ModTags.LUSH_CAVE_WATERS),
                new AndHolderSet<>(b.getOrThrow(Tags.Biomes.IS_LUSH), b.getOrThrow(Tags.Biomes.IS_CAVE)), b.getOrThrow(Tags.Biomes.IS_COLD_OVERWORLD));
        var pf = ctx.lookup(Registries.PLACED_FEATURE);
        ctx.register(WILD_PLANTS_MODIFIER, new BiomeModifiers.AddFeaturesBiomeModifier(biomes,
                HolderSet.direct(pf.getOrThrow(WILD_PLANTS_PLACED), pf.getOrThrow(WILD_CAVE_PLANTS_PLACED)), GenerationStep.Decoration.VEGETAL_DECORATION));
    }

    /** The template of a species' model (full grown or trimmed). */
    static String template(WildPlantKind kind, String look, boolean trimmed) {
        String t = switch (kind) {
            case EPIPHYTE -> "template_wild_epiphyte";
            case CARPET -> "template_wild_carpet";
            case MOSS -> "marimo".equals(look) ? "template_wild_marimo" : "template_wild_moss";
            default -> "template_wild_plant";
        };
        return trimmed ? t + "_short" : t;
    }

    static void blockStates(BlockStateProvider p) {
        for (var e : ModBlocks.WILD_PLANTS.entrySet()) {
            WildPlantKind kind = e.getKey();
            WildPlantBlock block = e.getValue().get();
            Map<String, ModelFile> models = new HashMap<>();
            p.getVariantBuilder(block).forAllStates(state -> {
                PlantSpecies s = block.species(state);
                String look = WildPlantKind.look(s);
                boolean trimmed = state.getValue(WildPlantBlock.TRIMMED);
                String name = kind.id() + "_" + look + (trimmed ? "_trimmed" : "");
                ModelFile m = models.computeIfAbsent(name, n -> model(p, kind, look, trimmed, n));
                return new ConfiguredModel[]{new ConfiguredModel(m), new ConfiguredModel(m, 0, 90, false), new ConfiguredModel(m, 0, 180, false),
                        new ConfiguredModel(m, 0, 270, false)};
            });
        }
        Map<FloatingPlantType, ModelFile> floating = new HashMap<>();
        p.getVariantBuilder(ModBlocks.WILD_FLOATING_PLANT.get()).forAllStates(state -> {
            FloatingPlantType t = WildFloatingPlantBlock.type(state);
            ModelFile m = floating.computeIfAbsent(t, k -> p.models().withExistingParent("wild_floating_" + k.id(), p.modLoc("block/template_wild_floating"))
                    .texture("tex", p.modLoc("block/wild_floating_" + k.id())));
            return new ConfiguredModel[]{new ConfiguredModel(m), new ConfiguredModel(m, 0, 90, false), new ConfiguredModel(m, 0, 180, false),
                    new ConfiguredModel(m, 0, 270, false)};
        });
    }

    private static ModelFile model(BlockStateProvider p, WildPlantKind kind, String look, boolean trimmed, String name) {
        var b = p.models().withExistingParent(name, p.modLoc("block/" + template(kind, look, trimmed)));
        if (kind == WildPlantKind.MOSS) {
            return b.texture("moss", p.modLoc("block/wild_moss"));
        }
        String tex = "block/" + kind.id() + "_" + look;
        b.texture("leaf", p.modLoc(tex)).texture("accent", p.modLoc(tex + "_accent")).texture("top", p.modLoc(tex + "_top"));
        if (kind == WildPlantKind.EPIPHYTE) b.texture("rhizome", p.modLoc("block/plant_rhizome"));
        if (kind == WildPlantKind.CARPET) { // M46: cushions with leafy tops and sides on a ground layer with a ragged rim
            b.texture("cushion", p.modLoc(tex + "_cushion")).texture("side", p.modLoc(tex + "_side")).texture("ground", p.modLoc(tex + "_ground"));
        }
        return b;
    }
}
