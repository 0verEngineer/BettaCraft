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
import net.minecraft.world.item.Items;
import org.overengineer.bettacraft.platform.crafting.SizedIngredient;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.crucible.CrucibleRecipe;
import org.overengineer.bettacraft.registry.ModItems;

/** M24: the melts of the crucible in JEI: up to four sized ingredients, the heat (lava, blaze rod, blaze powder) and the result. */
final class CrucibleJei implements IRecipeCategory<CrucibleRecipe> {
    static final RecipeType<CrucibleRecipe> TYPE = RecipeType.create(BettaCraft.MOD_ID, "crucible", CrucibleRecipe.class);
    private final IDrawable icon;
    private final IDrawable arrow;

    CrucibleJei(IGuiHelper gui) {
        this.icon = gui.createDrawableItemStack(new ItemStack(ModItems.CRUCIBLE.get()));
        this.arrow = gui.getRecipeArrow();
    }

    @Override
    public RecipeType<CrucibleRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.bettacraft.crucible");
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
    public void setRecipe(IRecipeLayoutBuilder b, CrucibleRecipe r, IFocusGroup focuses) {
        List<SizedIngredient> in = r.ingredients();
        for (int i = 0; i < in.size(); i++) {
            b.addSlot(RecipeIngredientRole.INPUT, 1 + (i % 2) * 18, 1 + (i / 2) * 18).setStandardSlotBackground()
                    .addItemStacks(List.of(in.get(i).getItems()));
        }
        b.addSlot(RecipeIngredientRole.CRAFTING_STATION, 44, 22).setStandardSlotBackground()
                .addItemStacks(List.of(new ItemStack(Items.LAVA_BUCKET), new ItemStack(Items.MAGMA_BLOCK), new ItemStack(Items.BLAZE_ROD), new ItemStack(Items.BLAZE_POWDER)));
        b.addSlot(RecipeIngredientRole.OUTPUT, 98, 11).setOutputSlotBackground().addItemStack(r.result());
    }

    @Override
    public void draw(CrucibleRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots, net.minecraft.client.gui.GuiGraphicsExtractor g, double mouseX, double mouseY) {
        arrow.draw(g, 66, 12);
    }
}
