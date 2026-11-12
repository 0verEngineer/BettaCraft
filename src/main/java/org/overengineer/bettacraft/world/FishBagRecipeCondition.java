package org.overengineer.bettacraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.registry.ModCodecs;

/**
 * Recipe condition {@code bettacraft:config}: true if the COMMON option {@code recipes.fishBag} has the given value
 * (AUTO, KELP or PLASTIC). Combined with {@code neoforge:tag_empty} on {@code #bettacraft:plastic_sheets} in the two
 * fish bag recipes (PLAN 4.3).
 */
public record FishBagRecipeCondition(String fishBag) implements ICondition {
    public static final MapCodec<FishBagRecipeCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("fish_bag").forGetter(FishBagRecipeCondition::fishBag)
    ).apply(i, FishBagRecipeCondition::new));

    @Override
    public boolean test(IContext context) {
        return CommonConfig.getOr(CommonConfig.RECIPE_FISH_BAG, CommonConfig.FishBagRecipe.AUTO).name().equalsIgnoreCase(fishBag);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return ModCodecs.CONFIG_CONDITION.get();
    }
}
