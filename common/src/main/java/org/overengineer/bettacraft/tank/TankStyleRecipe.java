package org.overengineer.bettacraft.tank;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.registry.ModRecipes;

/**
 * M11: converts a single vanilla-glass tank between the two styles (textured glass to clean glass and back), so that
 * tanks built before M11 can become clean ones and the other way round. Only the style {@link #from()} matches (the
 * displayed ingredient carries that style, but items made before M11 have no style component, so the check is done
 * here instead of by the ingredient). Being a {@link ShapelessRecipe} subclass it shows in the recipe book and JEI.
 */
public class TankStyleRecipe extends ShapelessRecipe {
    private final TankStyle from;

    public TankStyleRecipe(ShapelessRecipe base, TankStyle from) {
        super(new Recipe.CommonInfo(base.showNotification()), new CraftingRecipe.CraftingBookInfo(base.category(), base.group()),
                org.overengineer.bettacraft.crafting.RecipeCompat.resultTemplate(base), base.placementInfo().ingredients());
        this.from = from;
    }

    public TankStyle from() {
        return from;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack found = ItemStack.EMPTY;
        for (ItemStack s : input.items()) {
            if (s.isEmpty()) continue;
            if (!found.isEmpty()) return false;
            found = s;
        }
        return convertible(found, from);
    }

    /** A tank of vanilla glass with the given style (items without style component are textured). */
    public static boolean convertible(ItemStack stack, TankStyle from) {
        return stack.getItem() instanceof TankBlockItem && TankBlockEntity.DEFAULT_GLASS.equals(TankBlockItem.getGlass(stack))
                && TankBlockItem.getStyle(stack) == from;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return super.assemble(input);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        return (RecipeSerializer) ModRecipes.TANK_STYLE.get();
    }

    private static final MapCodec<TankStyleRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ShapelessRecipe.MAP_CODEC.forGetter(r -> r),
            TankStyle.CODEC.fieldOf("from").forGetter(TankStyleRecipe::from)
    ).apply(i, TankStyleRecipe::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, TankStyleRecipe> STREAM_CODEC = StreamCodec.composite(
            ShapelessRecipe.STREAM_CODEC, r -> r,
            TankStyle.STREAM_CODEC, TankStyleRecipe::from,
            TankStyleRecipe::new);

    /** M52c (26.1): a recipe serializer is a record of the two codecs. */
    public static RecipeSerializer<TankStyleRecipe> serializer() {
        return new RecipeSerializer<>(CODEC, STREAM_CODEC);
    }
}
