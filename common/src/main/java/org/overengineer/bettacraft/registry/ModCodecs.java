package org.overengineer.bettacraft.registry;

import java.util.function.Supplier;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.platform.registry.DeferredRegister;

/** Data-driven serializers of vanilla registries: world features, the feature loot condition (M52: the NeoForge-only ones
 * (biome modifiers, recipe conditions, global loot modifiers) are in the neoforge module's NeoForgeCodecs). */
public final class ModCodecs {
    /** M22.3: the wild corals of the warm ocean reefs. */
    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.FEATURE, BettaCraft.MOD_ID);
    public static final Supplier<org.overengineer.bettacraft.reef.WildCoralFeature> WILD_CORAL = FEATURES.register("wild_coral",
            org.overengineer.bettacraft.reef.WildCoralFeature::new);
    /** M28.1: wild aquarium and floating plants in their waters, and the cave pools of the lush caves. */
    public static final Supplier<org.overengineer.bettacraft.plants.wild.WildPlantFeature> WILD_PLANTS = FEATURES.register("wild_plants",
            () -> new org.overengineer.bettacraft.plants.wild.WildPlantFeature(false));
    public static final Supplier<org.overengineer.bettacraft.plants.wild.WildPlantFeature> WILD_CAVE_PLANTS = FEATURES.register("wild_cave_plants",
            () -> new org.overengineer.bettacraft.plants.wild.WildPlantFeature(true));
    /** M56: wild terrarium plants on the land of their biomes, and on the moss of the lush caves. */
    public static final Supplier<org.overengineer.bettacraft.plants.wild.WildLandPlantFeature> WILD_LAND_PLANTS = FEATURES.register("wild_land_plants",
            () -> new org.overengineer.bettacraft.plants.wild.WildLandPlantFeature(false));
    public static final Supplier<org.overengineer.bettacraft.plants.wild.WildLandPlantFeature> WILD_CAVE_LAND_PLANTS = FEATURES.register("wild_cave_land_plants",
            () -> new org.overengineer.bettacraft.plants.wild.WildLandPlantFeature(true));
    public static final DeferredRegister<net.minecraft.world.level.storage.loot.predicates.LootItemConditionType> LOOT_CONDITIONS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.LOOT_CONDITION_TYPE, BettaCraft.MOD_ID);
    public static final Supplier<net.minecraft.world.level.storage.loot.predicates.LootItemConditionType> FEATURE_LOOT_CONDITION =
            LOOT_CONDITIONS.register("feature", () -> new net.minecraft.world.level.storage.loot.predicates.LootItemConditionType(
                    org.overengineer.bettacraft.config.FeatureLootCondition.CODEC));

    public static void register() {
        FEATURES.register();
        LOOT_CONDITIONS.register();
    }

    private ModCodecs() {}
}
