package org.overengineer.bettacraft.crucible;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.platform.crafting.SizedIngredient;
import org.overengineer.bettacraft.registry.ModRecipes;

/**
 * M24: a melt in the crucible: up to four sized ingredients (in any of the four input slots) to one result, in
 * {@code crucible.meltTicks} ticks. Data-driven ({@code bettacraft:crucible}), so modpacks can add their own.
 */
public record CrucibleRecipe(List<SizedIngredient> ingredients, ItemStackTemplate output) implements Recipe<CrucibleRecipe.Input> {
    public static final int INPUTS = 4;

    /** M52c (26.1): recipes keep an immutable stack template; this takes a stack (datagen, tests). */
    public CrucibleRecipe(List<SizedIngredient> ingredients, ItemStack result) {
        this(ingredients, ItemStackTemplate.fromNonEmptyStack(result));
    }

    /** The result (a new stack). */
    public ItemStack result() {
        return output.create();
    }

    /** The four input slots. */
    public record Input(List<ItemStack> items) implements RecipeInput {
        @Override
        public ItemStack getItem(int i) {
            return items.get(i);
        }

        @Override
        public int size() {
            return items.size();
        }
    }

    /** Pure: which slots pay how much for this recipe (index -> count), or null if the slots do not hold enough. */
    public int[] take(List<ItemStack> slots) {
        int[] left = new int[slots.size()];
        for (int i = 0; i < slots.size(); i++) left[i] = slots.get(i).getCount();
        int[] take = new int[slots.size()];
        for (SizedIngredient si : ingredients) {
            int need = si.count();
            for (int i = 0; i < slots.size() && need > 0; i++) {
                ItemStack s = slots.get(i);
                if (s.isEmpty() || left[i] <= 0 || !si.ingredient().test(s)) continue;
                int n = Math.min(need, left[i]);
                left[i] -= n;
                take[i] += n;
                need -= n;
            }
            if (need > 0) return null;
        }
        return take;
    }

    @Override
    public boolean matches(Input input, Level level) {
        return take(input.items()) != null;
    }

    @Override
    public ItemStack assemble(Input input) {
        return output.create();
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    /** The result (1.21.1 {@code getResultItem}). */
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return output.create();
    }

    /** The ingredients without their counts (1.21.1 {@code getIngredients}). */
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> l = NonNullList.create();
        for (SizedIngredient s : ingredients) l.add(s.ingredient());
        return l;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    /** M52b: not in the recipe book (a device of its own), so nothing to place. */
    @Override
    public net.minecraft.world.item.crafting.PlacementInfo placementInfo() {
        return net.minecraft.world.item.crafting.PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public net.minecraft.world.item.crafting.RecipeBookCategory recipeBookCategory() {
        return net.minecraft.world.item.crafting.RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<CrucibleRecipe> getSerializer() {
        return ModRecipes.CRUCIBLE.get();
    }

    @Override
    public RecipeType<CrucibleRecipe> getType() {
        return ModRecipes.CRUCIBLE_TYPE.get();
    }

    static final MapCodec<CrucibleRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            SizedIngredient.FLAT_CODEC.listOf(1, INPUTS).fieldOf("ingredients").forGetter(CrucibleRecipe::ingredients),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(CrucibleRecipe::output)).apply(i, CrucibleRecipe::new));
    static final StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(INPUTS)), CrucibleRecipe::ingredients,
            ItemStackTemplate.STREAM_CODEC, CrucibleRecipe::output, CrucibleRecipe::new);

    /** M52c (26.1): a recipe serializer is a record of the two codecs. */
    public static RecipeSerializer<CrucibleRecipe> serializer() {
        return new RecipeSerializer<>(CODEC, STREAM_CODEC);
    }

    /** Copies of the stacks (the input must not change while matching). */
    static List<ItemStack> copy(List<ItemStack> l) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : l) out.add(s.copy());
        return out;
    }
}
