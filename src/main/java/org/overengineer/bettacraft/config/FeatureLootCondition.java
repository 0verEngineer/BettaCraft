package org.overengineer.bettacraft.config;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import org.overengineer.bettacraft.registry.ModCodecs;

/**
 * M33: loot condition {@code bettacraft:feature} ({@code {"condition": "bettacraft:feature", "feature": "liveFood.enabled"}}):
 * true while the feature is on. Our global loot modifiers (fishing plants, mud and sand drops) carry it; it is read at
 * every roll, so a change of the config applies at once.
 */
public record FeatureLootCondition(Feature feature) implements LootItemCondition {
    public static final MapCodec<FeatureLootCondition> CODEC = FeatureCondition.FEATURE.fieldOf("feature")
            .xmap(FeatureLootCondition::new, FeatureLootCondition::feature);

    @Override
    public LootItemConditionType getType() {
        return ModCodecs.FEATURE_LOOT_CONDITION.get();
    }

    @Override
    public boolean test(LootContext context) {
        return Features.enabled(feature);
    }
}
