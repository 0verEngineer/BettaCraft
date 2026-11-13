package org.overengineer.bettacraft.compat.jei;

import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.overengineer.bettacraft.equipment.CabinetBlockItem;
import org.overengineer.bettacraft.equipment.CabinetStones;
import org.overengineer.bettacraft.equipment.StoneCabinetRecipe;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * M21.7: the stone cabinet in JEI's crafting category: every stone the recipes know a slab for (also of other mods),
 * the slabs, the stones and the resulting cabinet cycle together (focus link), so the grid always shows a valid recipe.
 */
final class StoneCabinetJei implements ICraftingCategoryExtension<StoneCabinetRecipe> {
    @Override
    public void setRecipe(RecipeHolder<StoneCabinetRecipe> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) {
        var level = Minecraft.getInstance().level;
        List<CabinetStones.Pair> pairs = level == null ? CabinetStones.VANILLA : CabinetStones.all(level.getRecipeManager(), level.registryAccess());
        if (pairs.isEmpty()) pairs = CabinetStones.VANILLA;
        List<ItemStack> slabs = new ArrayList<>(), stones = new ArrayList<>(), cabinets = new ArrayList<>();
        for (CabinetStones.Pair p : pairs) {
            if (!(p.stone() instanceof BlockItem bi)) continue;
            slabs.add(new ItemStack(p.slab()));
            stones.add(new ItemStack(p.stone()));
            cabinets.add(CabinetBlockItem.setWood(new ItemStack(ModItems.CABINET.get()), BuiltInRegistries.BLOCK.getKey(bi.getBlock())));
        }
        List<ItemStack> chests = List.of(holder.value().getIngredients().get(4).getItems());
        List<List<ItemStack>> inputs = List.of(slabs, slabs, slabs, stones, chests, stones, stones, stones, stones);
        List<IRecipeSlotBuilder> in = grid.createAndSetInputs(builder, inputs, 3, 3);
        IRecipeSlotBuilder out = grid.createAndSetOutputs(builder, cabinets);
        List<IRecipeSlotBuilder> linked = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) if (i != 4) linked.add(in.get(i));
        linked.add(out);
        builder.createFocusLink(linked);
    }

    @Override
    public int getWidth(RecipeHolder<StoneCabinetRecipe> holder) {
        return 3;
    }

    @Override
    public int getHeight(RecipeHolder<StoneCabinetRecipe> holder) {
        return 3;
    }
}
