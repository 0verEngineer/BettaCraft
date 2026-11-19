package org.overengineer.bettacraft.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.ItemStack;
import org.slf4j.LoggerFactory;

/**
 * M52 (dev): opens the uses of an item in whichever recipe viewer runs (JEI, REI or EMI), for the screenshots. Each viewer
 * in a class of its own (only loaded with its mod).
 */
final class RecipeViewerShots {
    private RecipeViewerShots() {}

    static void show(ItemStack stack) {
        try {
            if (FabricLoader.getInstance().isModLoaded("jei")) Jei.show(stack);
            else if (FabricLoader.getInstance().isModLoaded("roughlyenoughitems")) Rei.show(stack);
        } catch (Throwable t) {
            LoggerFactory.getLogger("bettacraft/smoketest").warn("[smoketest] recipe viewer: {}", t.toString());
        }
    }

    private static final class Jei {
        static void show(ItemStack stack) {
            var rt = org.overengineer.bettacraft.compat.jei.BettaCraftJeiPlugin.runtime();
            if (rt == null) throw new IllegalStateException("JEI has no runtime");
            var ff = rt.getJeiHelpers().getFocusFactory();
            // like JEI's own "uses" key: the item as input and as catalyst (workstation)
            rt.getRecipesGui().show(java.util.List.of(
                    ff.createFocus(mezz.jei.api.recipe.RecipeIngredientRole.CRAFTING_STATION, mezz.jei.api.constants.VanillaTypes.ITEM_STACK, stack),
                    ff.createFocus(mezz.jei.api.recipe.RecipeIngredientRole.INPUT, mezz.jei.api.constants.VanillaTypes.ITEM_STACK, stack)));
        }
    }

    private static final class Rei {
        static void show(ItemStack stack) {
            me.shedaniel.rei.api.client.view.ViewSearchBuilder.builder().addUsagesFor(me.shedaniel.rei.api.common.util.EntryStacks.of(stack)).open();
        }
    }
}
