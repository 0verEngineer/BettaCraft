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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.overengineer.bettacraft.registry.ModRecipes;

/**
 * M24: a melt in the crucible: up to four sized ingredients (in any of the four input slots) to one result, in
 * {@code crucible.meltTicks} ticks. Data-driven ({@code bettacraft:crucible}), so modpacks can add their own.
 */
public record CrucibleRecipe(List<SizedIngredient> ingredients, ItemStack result) implements Recipe<CrucibleRecipe.Input> {
    public static final int INPUTS = 4;

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
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> l = NonNullList.create();
        for (SizedIngredient s : ingredients) l.add(s.ingredient());
        return l;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CRUCIBLE.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.CRUCIBLE_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<CrucibleRecipe> {
        static final MapCodec<CrucibleRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                SizedIngredient.FLAT_CODEC.listOf(1, INPUTS).fieldOf("ingredients").forGetter(CrucibleRecipe::ingredients),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CrucibleRecipe::result)).apply(i, CrucibleRecipe::new));
        static final StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipe> STREAM_CODEC = StreamCodec.composite(
                SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(INPUTS)), CrucibleRecipe::ingredients,
                ItemStack.STREAM_CODEC, CrucibleRecipe::result, CrucibleRecipe::new);

        @Override
        public MapCodec<CrucibleRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    /** Copies of the stacks (the input must not change while matching). */
    static List<ItemStack> copy(List<ItemStack> l) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : l) out.add(s.copy());
        return out;
    }
}
