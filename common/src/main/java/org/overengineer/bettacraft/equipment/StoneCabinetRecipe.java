package org.overengineer.bettacraft.equipment;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.platform.Tags;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.registry.ModRecipes;

/**
 * M21.7: cabinet of stone, {@code SSS / PCP / PPP} (P = five identical stone blocks, S = three slabs of the same stone,
 * C = wooden chest). Which blocks count as stone and which slab belongs to them is decided live by {@link CabinetStones}
 * (tags and the game's own slab recipes), so stones of every mod work. The ingredient lists in the JSON (vanilla stones
 * and their slabs in the same order) are only for JEI, the recipe book and the handbook. The result remembers the stone
 * in {@code bettacraft:wood}, like the planks of a wooden cabinet. Off with {@code features.equipment.stoneCabinets}.
 */
public class StoneCabinetRecipe extends ShapedRecipe {
    public StoneCabinetRecipe(Recipe.CommonInfo common, CraftingRecipe.CraftingBookInfo book, ShapedRecipePattern pattern, ItemStackTemplate result) {
        super(common, book, pattern, result);
    }

    /** The shown result is a cabinet of stone bricks (JEI without the extension, recipe book, handbook); {@link #assemble} sets the real stone. */
    public static StoneCabinetRecipe of(ShapedRecipe base) {
        // M52c (26.1): the template, not a stack (no item components while recipes are read)
        ItemStackTemplate shown = org.overengineer.bettacraft.crafting.RecipeCompat.withDefault(org.overengineer.bettacraft.crafting.RecipeCompat.resultTemplate(base),
                org.overengineer.bettacraft.registry.ModDataComponents.WOOD.get(), net.minecraft.resources.Identifier.withDefaultNamespace("stone_bricks"));
        return new StoneCabinetRecipe(new Recipe.CommonInfo(base.showNotification()), new CraftingRecipe.CraftingBookInfo(base.category(), base.group()), base.pattern,
                shown);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return level instanceof net.minecraft.server.level.ServerLevel sl && findStone(input, sl.recipeAccess(), level.registryAccess()) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack result = super.assemble(input);
        Item stone = input.width() == 3 && input.height() == 3 ? input.getItem(0, 2).getItem() : null;
        if (stone instanceof BlockItem bi) CabinetBlockItem.setWood(result, BuiltInRegistries.BLOCK.getKey(bi.getBlock()));
        return result;
    }

    /** The stone of a matching grid (five identical stones, three of their own slabs, a chest), or null. */
    @Nullable
    public static Item findStone(CraftingInput input, RecipeManager recipes, HolderLookup.Provider registries) {
        if (!Features.enabled(Feature.STONE_CABINETS) || input.width() != 3 || input.height() != 3) return null;
        Item stone = null, slab = null;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                ItemStack s = input.getItem(x, y);
                if (s.isEmpty()) return null;
                if (y == 0) {
                    if (slab == null) slab = s.getItem();
                    else if (slab != s.getItem()) return null;
                } else if (y == 1 && x == 1) {
                    if (!s.is(Tags.Items.CHESTS_WOODEN)) return null;
                } else {
                    if (stone == null) {
                        if (!CabinetStones.isStone(s)) return null;
                        stone = s.getItem();
                    } else if (stone != s.getItem()) return null;
                }
            }
        }
        return slab == CabinetStones.slabFor(recipes, registries, stone) ? stone : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        // M52c (26.1): ShapedRecipe narrows the serializer type; ours serializes a subclass
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) ModRecipes.STONE_CABINET.get();
    }

    private static final MapCodec<StoneCabinetRecipe> CODEC = ShapedRecipe.MAP_CODEC.xmap(StoneCabinetRecipe::of, r -> r);
    private static final StreamCodec<RegistryFriendlyByteBuf, StoneCabinetRecipe> STREAM_CODEC = ShapedRecipe.STREAM_CODEC.map(StoneCabinetRecipe::of, r -> r);

    /** M52c (26.1): a recipe serializer is a record of the two codecs. */
    public static RecipeSerializer<StoneCabinetRecipe> serializer() {
        return new RecipeSerializer<>(CODEC, STREAM_CODEC);
    }
}
