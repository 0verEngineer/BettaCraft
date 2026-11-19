package org.overengineer.bettacraft.equipment;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
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
import org.overengineer.bettacraft.platform.Tags;
import org.overengineer.bettacraft.registry.ModRecipes;

/**
 * Cabinet recipe {@code SSS / PCP / PPP} (S = any wooden slab, P = planks, C = wooden chest). All five planks must be
 * the same; the result remembers them in {@code bettacraft:wood}. Works with every planks of every mod, because only
 * the tags {@code #minecraft:planks}, {@code #minecraft:wooden_slabs} and {@code #c:chests/wooden} are checked (live,
 * not through cached ingredient lists). JEI and the recipe book see a normal shaped recipe.
 */
public class CabinetRecipe extends ShapedRecipe {
    public CabinetRecipe(Recipe.CommonInfo common, CraftingRecipe.CraftingBookInfo book, ShapedRecipePattern pattern, ItemStackTemplate result) {
        super(common, book, pattern, result);
    }

    public static CabinetRecipe of(ShapedRecipe base) {
        return new CabinetRecipe(new Recipe.CommonInfo(base.showNotification()), new CraftingRecipe.CraftingBookInfo(base.category(), base.group()), base.pattern,
                org.overengineer.bettacraft.crafting.RecipeCompat.resultTemplate(base));
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findWood(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack result = super.assemble(input);
        Item wood = findWood(input);
        if (wood instanceof BlockItem bi) CabinetBlockItem.setWood(result, BuiltInRegistries.BLOCK.getKey(bi.getBlock()));
        return result;
    }

    /** The planks of a matching grid (all five the same block item), or null if the grid does not match. */
    @Nullable
    public static Item findWood(CraftingInput input) {
        if (input.width() != 3 || input.height() != 3) return null;
        Item planks = null;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                ItemStack s = input.getItem(x, y);
                if (y == 0) {
                    if (!s.is(ItemTags.WOODEN_SLABS)) return null;
                } else if (y == 1 && x == 1) {
                    if (!s.is(Tags.Items.CHESTS_WOODEN)) return null;
                } else {
                    if (!s.is(ItemTags.PLANKS) || !(s.getItem() instanceof BlockItem)) return null;
                    if (planks == null) planks = s.getItem();
                    else if (planks != s.getItem()) return null;
                }
            }
        }
        return planks;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        // M52c (26.1): ShapedRecipe narrows the serializer type; ours serializes a subclass
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) ModRecipes.CABINET.get();
    }

    private static final MapCodec<CabinetRecipe> CODEC = ShapedRecipe.MAP_CODEC.xmap(CabinetRecipe::of, r -> r);
    private static final StreamCodec<RegistryFriendlyByteBuf, CabinetRecipe> STREAM_CODEC = ShapedRecipe.STREAM_CODEC.map(CabinetRecipe::of, r -> r);

    /** M52c (26.1): a recipe serializer is a record of the two codecs. */
    public static RecipeSerializer<CabinetRecipe> serializer() {
        return new RecipeSerializer<>(CODEC, STREAM_CODEC);
    }
}
