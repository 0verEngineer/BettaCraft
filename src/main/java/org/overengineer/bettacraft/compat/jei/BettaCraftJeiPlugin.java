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

    /** M21.7 dev smoke test: opens JEI's crafting page of the stone cabinet. False if JEI is not ready or it is missing. */
    /** M21.8 smoke scenes: sets the search text of the JEI item list (false without JEI). */
    public static boolean filterForTest(String text) {
        var rt = runtime;
        if (rt == null) return false;
        rt.getIngredientFilter().setFilterText(text);
        return true;
    }

    public static boolean showStoneCabinet() {
        var rt = runtime;
        if (rt == null) return false;
        var focus = rt.getJeiHelpers().getFocusFactory().createFocus(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK,
                new ItemStack(ModItems.CABINET.get()));
        var recipes = rt.getRecipeManager().createRecipeLookup(mezz.jei.api.constants.RecipeTypes.CRAFTING).limitFocus(List.of(focus)).get()
                .filter(r -> r.value() instanceof org.overengineer.bettacraft.equipment.StoneCabinetRecipe).toList();
        if (recipes.isEmpty()) return false;
        rt.getRecipesGui().showRecipes(rt.getRecipeManager().getRecipeCategory(mezz.jei.api.constants.RecipeTypes.CRAFTING), recipes, List.of());
        return true;
    }

    @Override
    public ResourceLocation getPluginUid() {
        return BettaCraft.id("jei");
    }

    /** M21.7: the stone cabinet recipe with matching stones, slabs and cabinets. */
    @Override
    public void registerVanillaCategoryExtensions(mezz.jei.api.registration.IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addExtension(org.overengineer.bettacraft.equipment.StoneCabinetRecipe.class, new StoneCabinetJei());
    }

    /** M24: the crucible. */
    @Override
    public void registerCategories(mezz.jei.api.registration.IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new CrucibleJei(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(mezz.jei.api.registration.IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.CRUCIBLE.get()), CrucibleJei.TYPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // M24: the melts of the crucible (from the recipe manager of the world)
        var mcLevel = net.minecraft.client.Minecraft.getInstance().level;
        if (mcLevel != null) {
            registration.addRecipes(CrucibleJei.TYPE, mcLevel.getRecipeManager().getAllRecipesFor(org.overengineer.bettacraft.registry.ModRecipes.CRUCIBLE_TYPE.get())
                    .stream().map(net.minecraft.world.item.crafting.RecipeHolder::value).toList());
        }
        for (String path : ItemHints.ITEMS) {
            Item item = BuiltInRegistries.ITEM.get(BettaCraft.id(path));
            info(registration, List.of(new ItemStack(item)), "info.bettacraft." + path);
        }
        List<ItemStack> decor = new ArrayList<>();
        for (var d : ModItems.DECOR.values()) decor.add(new ItemStack(d.get()));
        info(registration, decor, "info.bettacraft.decor");
        // M22.3: the wild corals of the reefs
        List<ItemStack> wild = new ArrayList<>();
        for (var c : ModItems.WILD_CORALS.values()) wild.add(new ItemStack(c.get()));
        info(registration, wild, "info.bettacraft.wild_coral");
        // M19: a care sheet per tankmate species (bucket and spawn egg)
        for (var sp : org.overengineer.bettacraft.tankmates.TankmateSpecies.values()) {
            info(registration, List.of(new ItemStack(ModItems.TANKMATE_BUCKETS.get(sp).get()), new ItemStack(ModItems.TANKMATE_SPAWN_EGGS.get(sp).get())),
                    "info.bettacraft.tankmate." + sp.id());
        }
    }

    /** A translatable text (JEI wraps it; the line breaks in the text stay), no client-only classes here. */
    private static void info(IRecipeRegistration registration, List<ItemStack> stacks, String key) {
        if (stacks.isEmpty()) return;
        registration.addIngredientInfo(stacks, VanillaTypes.ITEM_STACK, Component.translatable(key));
    }
}
