package org.overengineer.bettacraft.datagen;

import java.util.function.Function;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M31 part d data: the mob jar (a glass jar with an iron lid; full: a small dark mob shape inside; textures
 * {@code tools/assetgen/herp_icons.py}) and its recipe (five glass, iron bars as the lid, a slime ball to seal it), the
 * empty tag {@code #bettacraft:mob_capture_items} for capture items of other mods.
 */
final class DataGenM31d {
    private DataGenM31d() {}

    static void itemModels(ItemModelProvider p) {
        ModelFile filled = p.withExistingParent("mob_jar_filled", p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/mob_jar_filled"));
        p.withExistingParent("mob_jar", p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/mob_jar"))
                .override().predicate(BettaCraft.id("filled"), 1.0F).model(filled).end();
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.Criterion<?> hasTank) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.MOB_JAR.get())
                .pattern(" I ").pattern("GSG").pattern("GGG")
                .define('I', Items.IRON_BARS).define('G', net.neoforged.neoforge.common.Tags.Items.GLASS_BLOCKS_COLORLESS).define('S', Items.SLIME_BALL)
                .unlockedBy("has_tank", hasTank).save(out, BettaCraft.id("mob_jar"));
    }

    static void itemTags(Function<TagKey<Item>, IntrinsicHolderTagsProvider.IntrinsicTagAppender<Item>> tag) {
        tag.apply(ModTags.MOB_CAPTURE_ITEMS);
    }
}
