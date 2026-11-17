package org.overengineer.bettacraft.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * M52b: the recipe lookups of 1.21.1 on the recipe API of 1.21.2+, where only the server has the recipes
 * ({@link RecipeSync} sends ours to the clients).
 */
public final class RecipeCompat {
    private RecipeCompat() {}

    /** The server's recipes, or null on a client (1.21.1 {@code Level#getRecipeManager}). */
    @Nullable
    public static RecipeManager manager(@Nullable Level level) {
        return level instanceof ServerLevel sl ? sl.recipeAccess() : null;
    }

    /** Every recipe of one type ({@code RecipeManager#getAllRecipesFor} of 1.21.1). */
    @SuppressWarnings("unchecked")
    public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> byType(RecipeManager rm, RecipeType<T> type) {
        List<RecipeHolder<T>> out = new ArrayList<>();
        for (RecipeHolder<?> h : rm.getRecipes()) if (h.value().getType() == type) out.add((RecipeHolder<T>) h);
        return out;
    }

    /** The stacks an ingredient accepts ({@code Ingredient#getItems} of 1.21.1). */
    public static List<ItemStack> stacks(Ingredient ingredient) {
        return ingredient.items().map(ItemStack::new).toList();
    }

    public static List<ItemStack> stacks(Optional<Ingredient> ingredient) {
        return ingredient.map(RecipeCompat::stacks).orElse(List.of());
    }
}
