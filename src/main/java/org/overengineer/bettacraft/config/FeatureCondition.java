package org.overengineer.bettacraft.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.overengineer.bettacraft.registry.ModCodecs;

/**
 * M33: recipe condition {@code bettacraft:feature} ({@code {"type": "bettacraft:feature", "feature": "tankmates.cichlids"}}):
 * true while the feature is on ({@link Features#enabled}, with its parents). The common config is loaded before the
 * first datapack load, so the recipes follow the config of the server start; after a change, {@code /reload} applies it.
 * Every recipe of ours gets one per feature of {@link FeatureContent#ofRecipe} in datagen; modpack recipes can use it too.
 */
public record FeatureCondition(Feature feature) implements ICondition {
    /** A feature by its path below {@code features}; an unknown path is an error (the recipe is left out). */
    public static final Codec<Feature> FEATURE = Codec.STRING.comapFlatMap(s -> {
        Feature f = Feature.byPath(s);
        return f == null ? DataResult.error(() -> "unknown Bettacraft feature: " + s) : DataResult.success(f);
    }, Feature::path);

    public static final MapCodec<FeatureCondition> CODEC = FEATURE.fieldOf("feature").xmap(FeatureCondition::new, FeatureCondition::feature);

    @Override
    public boolean test(IContext context) {
        return Features.enabled(feature);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return ModCodecs.FEATURE_CONDITION.get();
    }
}
