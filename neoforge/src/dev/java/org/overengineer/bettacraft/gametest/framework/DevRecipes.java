package org.overengineer.bettacraft.gametest.framework;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;

/** M52b: the fixed result of a recipe ({@code getResultItem} of 1.21.1; 1.21.2 shows it through the recipe's displays). */
public final class DevRecipes {
    private DevRecipes() {}

    public static ItemStack result(Recipe<?> recipe) {
        if (recipe instanceof org.overengineer.bettacraft.crucible.CrucibleRecipe c) return c.getResultItem(null);
        if (recipe instanceof org.overengineer.bettacraft.food.FoodMakerRecipe f) return f.getResultItem(null);
        for (var d : recipe.display()) {
            if (d.result() instanceof SlotDisplay.ItemStackSlotDisplay i) return i.stack().create();
            if (d.result() instanceof SlotDisplay.ItemSlotDisplay i) return new ItemStack(i.item());
        }
        return ItemStack.EMPTY;
    }
}
