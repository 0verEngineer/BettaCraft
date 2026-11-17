package org.overengineer.bettacraft.tank;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.registry.ModRecipes;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * Shaped tank recipe ({@code GGG / GIG / III -> 4 tanks}) where all six glass blocks must be identical.
 * The result carries that glass in the {@code bettacraft:glass} component. Being a {@link ShapedRecipe} subclass it is
 * shown by the recipe book and JEI like a normal shaped recipe.
 */
public class TankRecipe extends ShapedRecipe {
    public TankRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification) {
        super(group, category, pattern, result, showNotification);
    }

    public static TankRecipe of(ShapedRecipe base) {
        return new TankRecipe(base.group(), base.category(), base.pattern, base.assemble(net.minecraft.world.item.crafting.CraftingInput.EMPTY, null), base.showNotification());
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return super.matches(input, level) && findGlass(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = super.assemble(input, registries);
        Item glass = findGlass(input);
        if (glass instanceof BlockItem bi) {
            TankBlockItem.setGlass(result, BuiltInRegistries.BLOCK.getKey(bi.getBlock()));
            TankBlockItem.setStyle(result, TankStyle.TEXTURED);
        }
        return result;
    }

    /** The single glass item used in the grid, or null if the glass blocks differ. */
    static Item findGlass(CraftingInput input) {
        Item found = null;
        for (ItemStack s : input.items()) {
            if (s.isEmpty() || !s.is(ModTags.TANK_GLASS_ITEMS)) continue;
            if (found == null) found = s.getItem();
            else if (found != s.getItem()) return null;
        }
        return found;
    }

    @Override
    public RecipeSerializer<TankRecipe> getSerializer() {
        return ModRecipes.TANK.get();
    }

    public static class Serializer implements RecipeSerializer<TankRecipe> {
        private static final MapCodec<TankRecipe> CODEC = ShapedRecipe.Serializer.CODEC.xmap(TankRecipe::of, r -> r);
        private static final StreamCodec<RegistryFriendlyByteBuf, TankRecipe> STREAM_CODEC =
                ShapedRecipe.Serializer.STREAM_CODEC.map(TankRecipe::of, r -> r);

        @Override
        public MapCodec<TankRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TankRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
