package org.overengineer.bettacraft.fabric.compat;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.compat.info.RecipeViewerInfo;
import org.overengineer.bettacraft.platform.crafting.SizedIngredient;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModRecipes;

/**
 * M52: EMI (Fabric): the information pages and the recipes of the crucible and the food maker, like our JEI plugin.
 */
public final class BettaCraftEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        // M62: the tank menu grows with the screen beyond its slot frame: EMI keeps clear of the whole panel
        registry.addExclusionArea(org.overengineer.bettacraft.client.screen.TankScreen.class, (screen, out) -> {
            int[] p = screen.panelBounds();
            out.accept(new dev.emi.emi.api.widget.Bounds(p[0], p[1], p[2], p[3]));
        });
        EmiRecipeCategory crucible = category("crucible", ModItems.CRUCIBLE.get().getDefaultInstance(), "block.bettacraft.crucible");
        EmiRecipeCategory foodMaker = category("food_maker", ModItems.FOOD_MAKER.get().getDefaultInstance(), "block.bettacraft.food_maker");
        registry.addCategory(crucible);
        registry.addCategory(foodMaker);
        registry.addWorkstation(crucible, EmiStack.of(ModItems.CRUCIBLE.get()));
        registry.addWorkstation(foodMaker, EmiStack.of(ModItems.FOOD_MAKER.get()));
        var rm = registry.getRecipeManager();
        for (var h : rm.getAllRecipesFor(ModRecipes.CRUCIBLE_TYPE.get())) {
            registry.addRecipe(new Simple(crucible, h.id(), h.value().ingredients(), h.value().result(), true));
        }
        for (var h : org.overengineer.bettacraft.food.FoodMakerBlockEntity.recipes(rm)) {
            registry.addRecipe(new Simple(foodMaker, h.id(), h.value().ingredients(), h.value().result(), false));
        }
        int i = 0;
        for (RecipeViewerInfo.Page p : RecipeViewerInfo.pages()) {
            List<EmiIngredient> stacks = new ArrayList<>();
            for (ItemStack s : p.stacks()) stacks.add(EmiStack.of(s));
            registry.addRecipe(new EmiInfoRecipe(stacks, List.of(Component.translatable(p.key())), BettaCraft.id("/info/" + i++)));
        }
    }

    private static EmiRecipeCategory category(String id, ItemStack icon, String title) {
        return new EmiRecipeCategory(BettaCraft.id(id), EmiStack.of(icon)) {
            @Override
            public Component getName() {
                return Component.translatable(title);
            }
        };
    }

    private static EmiIngredient ingredient(SizedIngredient s) {
        return EmiIngredient.of(Ingredient.of(s.getItems()), s.count());
    }

    /** Up to four inputs in a 2x2 grid, the heat sources of the crucible, the result. */
    private static final class Simple extends BasicEmiRecipe {
        private final boolean heat;

        Simple(EmiRecipeCategory cat, Identifier id, List<SizedIngredient> in, ItemStack out, boolean heat) {
            super(cat, id, 120, 38);
            this.heat = heat;
            for (SizedIngredient s : in) inputs.add(ingredient(s));
            outputs.add(EmiStack.of(out));
            if (heat) catalysts.add(EmiIngredient.of(Ingredient.of(Items.LAVA_BUCKET, Items.MAGMA_BLOCK, Items.BLAZE_ROD, Items.BLAZE_POWDER)));
        }

        @Override
        public void addWidgets(WidgetHolder w) {
            for (int i = 0; i < inputs.size(); i++) w.addSlot(inputs.get(i), (i % 2) * 18, (i / 2) * 18);
            if (heat) w.addSlot(catalysts.get(0), 43, 20).catalyst(true);
            w.addTexture(dev.emi.emi.api.render.EmiTexture.EMPTY_ARROW, 64, 10);
            w.addSlot(outputs.get(0), 94, 6).large(true).recipeContext(this);
        }
    }
}
