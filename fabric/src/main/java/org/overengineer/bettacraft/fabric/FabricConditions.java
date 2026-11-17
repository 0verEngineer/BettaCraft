package org.overengineer.bettacraft.fabric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.FeatureCodec;
import org.overengineer.bettacraft.config.Features;

/**
 * M52: our recipe conditions as Fabric resource conditions ({@code fabric:load_conditions}; the build converts NeoForge's
 * {@code neoforge:conditions} of our data, see fabric/build.gradle fabricData): {@code bettacraft:feature} (M33) and
 * {@code bettacraft:config} (the fish bag recipe).
 */
final class FabricConditions {
    private FabricConditions() {}

    record FeatureOn(Feature feature) implements ResourceCondition {
        static final MapCodec<FeatureOn> CODEC = FeatureCodec.FEATURE.fieldOf("feature").xmap(FeatureOn::new, FeatureOn::feature);
        static final ResourceConditionType<FeatureOn> TYPE = ResourceConditionType.create(BettaCraft.id("feature"), CODEC);

        @Override
        public ResourceConditionType<?> getType() {
            return TYPE;
        }

        @Override
        public boolean test(@Nullable net.minecraft.core.HolderLookup.Provider registries) {
            return Features.enabled(feature);
        }
    }

    record FishBag(String fishBag) implements ResourceCondition {
        static final MapCodec<FishBag> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.fieldOf("fish_bag").forGetter(FishBag::fishBag)).apply(i, FishBag::new));
        static final ResourceConditionType<FishBag> TYPE = ResourceConditionType.create(BettaCraft.id("config"), CODEC);

        @Override
        public ResourceConditionType<?> getType() {
            return TYPE;
        }

        @Override
        public boolean test(@Nullable net.minecraft.core.HolderLookup.Provider registries) {
            return CommonConfig.getOr(CommonConfig.RECIPE_FISH_BAG, CommonConfig.FishBagRecipe.AUTO).name().equalsIgnoreCase(fishBag);
        }
    }

    static void register() {
        ResourceConditions.register(FeatureOn.TYPE);
        ResourceConditions.register(FishBag.TYPE);
    }
}
