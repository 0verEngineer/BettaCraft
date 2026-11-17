package org.overengineer.bettacraft.fabric.compat;

import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.client.BuiltinClientPlugin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.overengineer.bettacraft.compat.info.RecipeViewerInfo;
import org.overengineer.bettacraft.platform.crafting.SizedIngredient;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModRecipes;

/** M52: REI (Fabric): the information pages and the recipes of the crucible and the food maker, like our JEI plugin. */
public final class BettaCraftReiPlugin implements REIClientPlugin {
    static final CategoryIdentifier<Simple> CRUCIBLE = CategoryIdentifier.of("bettacraft", "crucible");
    static final CategoryIdentifier<Simple> FOOD_MAKER = CategoryIdentifier.of("bettacraft", "food_maker");

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new Category(CRUCIBLE, ModItems.CRUCIBLE.get().getDefaultInstance(), "block.bettacraft.crucible"));
        registry.add(new Category(FOOD_MAKER, ModItems.FOOD_MAKER.get().getDefaultInstance(), "block.bettacraft.food_maker"));
        registry.addWorkstations(CRUCIBLE, EntryStacks.of(ModItems.CRUCIBLE.get()));
        registry.addWorkstations(FOOD_MAKER, EntryStacks.of(ModItems.FOOD_MAKER.get()));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        var rm = registry.getRecipeManager();
        for (var h : rm.getAllRecipesFor(ModRecipes.CRUCIBLE_TYPE.get())) registry.add(new Simple(CRUCIBLE, h.value().ingredients(), h.value().result()));
        for (var h : org.overengineer.bettacraft.food.FoodMakerBlockEntity.recipes(rm)) {
            registry.add(new Simple(FOOD_MAKER, h.value().ingredients(), h.value().result()));
        }
        for (RecipeViewerInfo.Page p : RecipeViewerInfo.pages()) {
            BuiltinClientPlugin.getInstance().registerInformation(EntryIngredients.ofItemStacks(p.stacks()), p.stacks().get(0).getHoverName(),
                    lines -> {
                        lines.add(Component.translatable(p.key()));
                        return lines;
                    });
        }
    }

    record Simple(CategoryIdentifier<Simple> category, List<EntryIngredient> inputs, List<EntryIngredient> outputs) implements Display {
        Simple(CategoryIdentifier<Simple> category, List<SizedIngredient> in, ItemStack out) {
            this(category, in.stream().map(s -> EntryIngredients.ofItemStacks(List.of(s.getItems()))).toList(), List.of(EntryIngredients.of(out)));
        }

        @Override
        public List<EntryIngredient> getInputEntries() {
            return inputs;
        }

        @Override
        public List<EntryIngredient> getOutputEntries() {
            return outputs;
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return category;
        }
    }

    private record Category(CategoryIdentifier<Simple> id, ItemStack icon, String title) implements DisplayCategory<Simple> {
        @Override
        public CategoryIdentifier<? extends Simple> getCategoryIdentifier() {
            return id;
        }

        @Override
        public Component getTitle() {
            return Component.translatable(title);
        }

        @Override
        public Renderer getIcon() {
            return EntryStacks.of(icon);
        }

        @Override
        public int getDisplayHeight() {
            return 50;
        }

        @Override
        public List<Widget> setupDisplay(Simple d, Rectangle bounds) {
            List<Widget> w = new ArrayList<>();
            w.add(Widgets.createRecipeBase(bounds));
            int x = bounds.getCenterX() - 60, y = bounds.y + 7;
            for (int i = 0; i < d.inputs().size(); i++) {
                w.add(Widgets.createSlot(new Point(x + (i % 2) * 18, y + (i / 2) * 18)).entries(d.inputs().get(i)).markInput());
            }
            if (id == CRUCIBLE) {
                w.add(Widgets.createSlot(new Point(x + 43, y + 20)).entries(EntryIngredients.ofItemStacks(List.of(new ItemStack(Items.LAVA_BUCKET),
                        new ItemStack(Items.MAGMA_BLOCK), new ItemStack(Items.BLAZE_ROD), new ItemStack(Items.BLAZE_POWDER)))));
            }
            w.add(Widgets.createArrow(new Point(x + 64, y + 9)));
            w.add(Widgets.createResultSlotBackground(new Point(x + 98, y + 9)));
            w.add(Widgets.createSlot(new Point(x + 98, y + 9)).entries(d.outputs().get(0)).disableBackground().markOutput());
            return w;
        }
    }
}
