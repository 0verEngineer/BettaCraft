package org.overengineer.bettacraft.tank;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
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
    public TankRecipe(Recipe.CommonInfo common, CraftingRecipe.CraftingBookInfo book, ShapedRecipePattern pattern, ItemStackTemplate result) {
        super(common, book, pattern, result);
    }

    public static TankRecipe of(ShapedRecipe base) {
        return new TankRecipe(new Recipe.CommonInfo(base.showNotification()), new CraftingRecipe.CraftingBookInfo(base.category(), base.group()), base.pattern,
                org.overengineer.bettacraft.crafting.RecipeCompat.resultTemplate(base));
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return super.matches(input, level) && findGlass(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack result = super.assemble(input);
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
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        // M52c (26.1): ShapedRecipe narrows the serializer type; ours serializes a subclass
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) ModRecipes.TANK.get();
    }

    private static final MapCodec<TankRecipe> CODEC = ShapedRecipe.MAP_CODEC.xmap(TankRecipe::of, r -> r);
    private static final StreamCodec<RegistryFriendlyByteBuf, TankRecipe> STREAM_CODEC = ShapedRecipe.STREAM_CODEC.map(TankRecipe::of, r -> r);

    /** M52c (26.1): a recipe serializer is a record of the two codecs. */
    public static RecipeSerializer<TankRecipe> serializer() {
        return new RecipeSerializer<>(CODEC, STREAM_CODEC);
    }
}
