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

    /**
     * M52c (26.1): the result template of a crafting recipe, without making a stack (recipes are read before the item
     * components are bound, so {@code assemble} cannot run while a recipe is decoded).
     */
    public static net.minecraft.world.item.ItemStackTemplate resultTemplate(Recipe<?> recipe) {
        for (var d : recipe.display()) {
            if (d.result() instanceof net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay i) return i.stack();
        }
        throw new IllegalArgumentException("no fixed result: " + recipe);
    }

    /** M52c: the template with a component set in its patch unless the patch already has it. */
    public static <T> net.minecraft.world.item.ItemStackTemplate withDefault(net.minecraft.world.item.ItemStackTemplate t,
            net.minecraft.core.component.DataComponentType<T> type, T value) {
        // M52e: 26.3 has no entry view of a patch any more, only its split into added components and removed types
        var split = t.components().split();
        if (split.added().has(type) || split.removed().contains(type)) return t;
        var b = net.minecraft.core.component.DataComponentPatch.builder();
        b.set(split.added());
        for (var removed : split.removed()) b.remove(removed);
        b.set(type, value);
        return new net.minecraft.world.item.ItemStackTemplate(t.item(), t.count(), b.build());
    }
}
