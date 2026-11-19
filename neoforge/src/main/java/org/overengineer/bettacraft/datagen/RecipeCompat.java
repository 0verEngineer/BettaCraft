package org.overengineer.bettacraft.datagen;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

/**
 * M52b: what the recipe generators of the mod need since 1.21.2 (recipe builders and tag ingredients take the item
 * lookup of the run, recipes are saved under resource keys), so the many static recipe methods keep their shape.
 */
public final class RecipeCompat {
    private static HolderGetter<Item> items;

    private RecipeCompat() {}

    /** Set by the recipe provider before it builds the recipes. */
    static void begin(HolderGetter<Item> lookup) {
        items = lookup;
    }

    public static HolderGetter<Item> items() {
        if (items == null) throw new IllegalStateException("recipes are only made inside the recipe provider");
        return items;
    }

    /** M52c (26.1): the dye item of a colour ({@code DyeItem.byColor} is gone). */
    public static Item dye(net.minecraft.world.item.DyeColor c) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(c.getName() + "_dye"));
    }

    public static Ingredient tag(TagKey<Item> tag) {
        return Ingredient.of(items().getOrThrow(tag));
    }

    /**
     * M52c (26.1): the item of the fixed result of a recipe, or null; without making a stack (the item components are not
     * bound while the data generator runs).
     */
    public static @org.jetbrains.annotations.Nullable Item resultItem(Recipe<?> recipe) {
        if (recipe instanceof org.overengineer.bettacraft.crucible.CrucibleRecipe c) return c.output().item().value();
        if (recipe instanceof org.overengineer.bettacraft.food.FoodMakerRecipe f) return f.output().item().value();
        for (var d : recipe.display()) {
            if (d.result() instanceof net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay i) return i.stack().item().value();
            if (d.result() instanceof net.minecraft.world.item.crafting.display.SlotDisplay.ItemSlotDisplay i) return i.item().value();
        }
        return null;
    }

    /** The fixed result of a recipe (1.21.1 {@code getResultItem}; empty when it has none). */
    public static net.minecraft.world.item.ItemStack result(Recipe<?> recipe) {
        if (recipe instanceof org.overengineer.bettacraft.crucible.CrucibleRecipe c) return c.getResultItem(null);
        if (recipe instanceof org.overengineer.bettacraft.food.FoodMakerRecipe f) return f.getResultItem(null);
        for (var d : recipe.display()) {
            if (d.result() instanceof net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay i) return i.stack().create();
            if (d.result() instanceof net.minecraft.world.item.crafting.display.SlotDisplay.ItemSlotDisplay i) return new net.minecraft.world.item.ItemStack(i.item());
        }
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    public static ResourceKey<Recipe<?>> key(Identifier id) {
        return ResourceKey.create(Registries.RECIPE, id);
    }
}
