package org.overengineer.bettacraft.compat.jei;

import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.food.FoodMakerRecipe;
import org.overengineer.bettacraft.registry.ModItems;

/** M26 part b: the batches of the food maker in JEI: up to four sized ingredients and the result. */
final class FoodMakerJei implements IRecipeCategory<FoodMakerRecipe> {
    static final RecipeType<FoodMakerRecipe> TYPE = RecipeType.create(BettaCraft.MOD_ID, "food_maker", FoodMakerRecipe.class);
    private final IDrawable icon;
    private final IDrawable arrow;

    FoodMakerJei(IGuiHelper gui) {
        this.icon = gui.createDrawableItemStack(new ItemStack(ModItems.FOOD_MAKER.get()));
        this.arrow = gui.getRecipeArrow();
    }

    @Override
    public RecipeType<FoodMakerRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.bettacraft.food_maker");
    }

    @Override
    public int getWidth() {
        return 120;
    }

    @Override
    public int getHeight() {
        return 40;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, FoodMakerRecipe r, IFocusGroup focuses) {
        List<SizedIngredient> in = r.ingredients();
        for (int i = 0; i < in.size(); i++) {
            b.addSlot(RecipeIngredientRole.INPUT, 1 + (i % 2) * 18, 1 + (i / 2) * 18).setStandardSlotBackground()
                    .addItemStacks(List.of(in.get(i).getItems()));
        }
        b.addSlot(RecipeIngredientRole.OUTPUT, 98, 11).setOutputSlotBackground().addItemStack(r.result());
    }

    @Override
    public void draw(FoodMakerRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots, net.minecraft.client.gui.GuiGraphics g, double mouseX, double mouseY) {
        arrow.draw(g, 66, 12);
    }
}
