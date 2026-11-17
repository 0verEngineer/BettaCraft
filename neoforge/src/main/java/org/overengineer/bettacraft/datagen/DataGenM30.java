package org.overengineer.bettacraft.datagen;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import org.overengineer.bettacraft.datagen.model.ItemModelProvider;
import org.overengineer.bettacraft.datagen.model.ModelFile;
import org.overengineer.bettacraft.platform.Tags;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M30 part a: item models and recipes of the catching tools for big animals (large net, transport tub, steel leader). The
 * stainless steel of M24 makes the frame of the net, the rim of the tub and the leader wire. Textures:
 * {@code tools/assetgen/big_animals.py}.
 */
final class DataGenM30 {
    private DataGenM30() {}

    static void itemModels(ItemModelProvider p) {
        p.withExistingParent("large_net", p.mcLoc("item/handheld")).texture("layer0", p.modLoc("item/large_net"));
        ModelFile filled = p.withExistingParent("transport_tub_filled", p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/transport_tub_filled"));
        p.withExistingParent("transport_tub", p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/transport_tub"))
                .override().predicate(BettaCraft.id("filled"), 1.0F).model(filled).end();
        p.withExistingParent("steel_leader", p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/steel_leader"));
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.Criterion<?> hasTank) {
        // the large net: a stainless steel hoop with a string mesh on a long handle
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.TOOLS, ModItems.LARGE_NET.get())
                .pattern("SSS").pattern("SWS").pattern(" T ")
                .define('S', ModTags.STAINLESS_STEEL_INGOTS).define('W', Tags.Items.STRINGS).define('T', ModItems.AQUARIUM_NET.get())
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("large_net")));
        // the transport tub: a big tub with a steel rim and handles
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.TOOLS, ModItems.TRANSPORT_TUB.get())
                .pattern("S S").pattern("B B").pattern("BBB")
                .define('S', ModTags.STAINLESS_STEEL_INGOTS).define('B', Items.BUCKET)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("transport_tub")));
        // the steel leader: steel wire with a swivel and a hook
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.TOOLS, ModItems.STEEL_LEADER.get())
                .pattern("  S").pattern(" W ").pattern("N  ")
                .define('S', ModTags.STAINLESS_STEEL_INGOTS).define('W', Tags.Items.STRINGS).define('N', Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("steel_leader")));
    }
}
