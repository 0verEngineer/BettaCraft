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
    // M52c (26.1): the loot condition registry holds the map codecs themselves
    public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.storage.loot.predicates.LootItemCondition>> LOOT_CONDITIONS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.LOOT_CONDITION_TYPE, BettaCraft.MOD_ID);
    public static final Supplier<com.mojang.serialization.MapCodec<org.overengineer.bettacraft.config.FeatureLootCondition>> FEATURE_LOOT_CONDITION =
            LOOT_CONDITIONS.register("feature", () -> org.overengineer.bettacraft.config.FeatureLootCondition.CODEC);

    /** M52c (26.1): item modifier {@code bettacraft:trade} of our villager trade entries ({@link org.overengineer.bettacraft.villager.TradeFunction}). */
    public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.storage.loot.functions.LootItemFunction>> LOOT_FUNCTIONS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.LOOT_FUNCTION_TYPE, BettaCraft.MOD_ID);
    public static final Supplier<com.mojang.serialization.MapCodec<org.overengineer.bettacraft.villager.TradeFunction>> TRADE_FUNCTION =
            LOOT_FUNCTIONS.register("trade", () -> org.overengineer.bettacraft.villager.TradeFunction.CODEC);

    public static void register() {
        FEATURES.register();
        LOOT_CONDITIONS.register();
        LOOT_FUNCTIONS.register();
    }

    private ModCodecs() {}
}
