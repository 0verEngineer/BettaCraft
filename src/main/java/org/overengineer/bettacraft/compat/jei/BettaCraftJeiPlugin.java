package org.overengineer.bettacraft.compat.jei;

import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.progression.ItemHints;
import org.overengineer.bettacraft.registry.ModItems;

/** JEI information pages for Bettacraft items (M6). Only loaded by JEI (annotation scan), never without it. */
@JeiPlugin
public final class BettaCraftJeiPlugin implements IModPlugin {
    private static mezz.jei.api.runtime.IJeiRuntime runtime;

    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    /** Dev smoke test: opens the JEI information page of {@code stack}. False if JEI is not ready or there is none. */
    public static boolean showInfo(ItemStack stack) {
        var rt = runtime;
        if (rt == null) return false;
        for (var role : mezz.jei.api.recipe.RecipeIngredientRole.values()) {
            var focus = rt.getJeiHelpers().getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, stack);
            var recipes = rt.getRecipeManager().createRecipeLookup(mezz.jei.api.constants.RecipeTypes.INFORMATION)
                    .limitFocus(List.of(focus)).get().toList();
            if (recipes.isEmpty()) continue;
            rt.getRecipesGui().showRecipes(rt.getRecipeManager().getRecipeCategory(mezz.jei.api.constants.RecipeTypes.INFORMATION),
                    recipes, List.of(focus));
            return true;
        }
        return false;
    }

    @Override
    public ResourceLocation getPluginUid() {
        return BettaCraft.id("jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (String path : ItemHints.ITEMS) {
            Item item = BuiltInRegistries.ITEM.get(BettaCraft.id(path));
            info(registration, List.of(new ItemStack(item)), "info.bettacraft." + path);
        }
        List<ItemStack> decor = new ArrayList<>();
        for (var d : ModItems.DECOR.values()) decor.add(new ItemStack(d.get()));
        info(registration, decor, "info.bettacraft.decor");
    }

    /** A translatable text (JEI wraps it; the line breaks in the text stay), no client-only classes here. */
    private static void info(IRecipeRegistration registration, List<ItemStack> stacks, String key) {
        if (stacks.isEmpty()) return;
        registration.addIngredientInfo(stacks, VanillaTypes.ITEM_STACK, Component.translatable(key));
    }
}
