package org.overengineer.bettacraft.compat.jei;

import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.progression.ItemHints;
import org.overengineer.bettacraft.registry.ModItems;

/** JEI information pages for Bettacraft items (M6). Only loaded by JEI (annotation scan), never without it. */
@JeiPlugin
public final class BettaCraftJeiPlugin implements IModPlugin {
    private static mezz.jei.api.runtime.IJeiRuntime runtime;

    /** The running JEI (null before it started); the dev smokes open recipe pages with it. */
    public static mezz.jei.api.runtime.IJeiRuntime runtime() {
        return runtime;
    }

    /** M33 part b: the stacks this plugin took out of JEI's list (features that are off), to give back when they are on. */
    private static final List<ItemStack> removed = new ArrayList<>();
    private static final java.util.Set<mezz.jei.api.recipe.RecipeType<?>> hiddenCategories = new java.util.HashSet<>();

    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        removed.clear();
        hiddenCategories.clear();
        applyFeatures();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        removed.clear();
        hiddenCategories.clear();
    }

    /**
     * M33 part b: JEI follows the feature switches (of the server on a remote one, {@link org.overengineer.bettacraft.client.ClientFeatures}):
     * the items of a feature that is off leave the item list, the crucible and food maker categories hide with their
     * features; {@code features.compat.jei} off hides our categories and hides nothing else. Called when the runtime starts
     * and whenever the switches change (client thread).
     */
    public static void applyFeatures() {
        var rt = runtime;
        if (rt == null) return;
        boolean on = org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.COMPAT_JEI);
        category(rt, CrucibleJei.TYPE, !on || !org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.STAINLESS_STEEL));
        category(rt, FoodMakerJei.TYPE, !on || !org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.FOOD_MACHINE));
        var im = rt.getIngredientManager();
        List<ItemStack> show = new ArrayList<>();
        for (ItemStack s : removed) if (!on || org.overengineer.bettacraft.config.FeatureGate.stack(s)) show.add(s);
        List<ItemStack> hide = new ArrayList<>();
        if (on) {
            for (ItemStack s : im.getAllItemStacks()) {
                if (BettaCraft.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace()) && !org.overengineer.bettacraft.config.FeatureGate.stack(s)) {
                    hide.add(s);
                }
            }
        }
        if (!show.isEmpty()) {
            im.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, show);
            removed.removeIf(s -> show.stream().anyMatch(t -> t == s));
        }
        if (!hide.isEmpty()) {
            im.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hide);
            removed.addAll(hide);
        }
    }

    private static void category(mezz.jei.api.runtime.IJeiRuntime rt, mezz.jei.api.recipe.RecipeType<?> type, boolean hidden) {
        if (hidden == hiddenCategories.contains(type)) return;
        if (hidden) {
            rt.getRecipeManager().hideRecipeCategory(type);
            hiddenCategories.add(type);
        } else {
            rt.getRecipeManager().unhideRecipeCategory(type);
            hiddenCategories.remove(type);
        }
    }

    /** M33 part b smoke scenes / tests: how many of our stacks JEI has hidden (-1 without a runtime). */
    public static int hiddenForTest() {
        return runtime == null ? -1 : removed.size();
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
    public Identifier getPluginUid() {
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
        registration.addRecipeCategories(new FoodMakerJei(registration.getJeiHelpers().getGuiHelper())); // M26 part b
    }

    @Override
    public void registerRecipeCatalysts(mezz.jei.api.registration.IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.CRUCIBLE.get()), CrucibleJei.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.FOOD_MAKER.get()), FoodMakerJei.TYPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // M24: the melts of the crucible (from the recipe manager of the world)
        // M52: the recipes the server sent (Fabric's JEI starts before the client level exists)
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        var mcLevel = net.minecraft.client.Minecraft.getInstance().level;
        var rm = connection != null ? connection.getRecipeManager() : mcLevel != null ? mcLevel.getRecipeManager() : null;
        if (rm != null) {
            registration.addRecipes(CrucibleJei.TYPE, rm.getAllRecipesFor(org.overengineer.bettacraft.registry.ModRecipes.CRUCIBLE_TYPE.get())
                    .stream().map(net.minecraft.world.item.crafting.RecipeHolder::value).toList());
            registration.addRecipes(FoodMakerJei.TYPE, org.overengineer.bettacraft.food.FoodMakerBlockEntity.recipes(rm)
                    .stream().map(net.minecraft.world.item.crafting.RecipeHolder::value).toList()); // M26 part b
        }
        // M33 part b: no info pages with features.compat.jei off, none for the items of a feature that is off (at JEI's start)
        if (!org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.COMPAT_JEI)) return;
        for (String path : ItemHints.ITEMS) {
            Item item = BuiltInRegistries.ITEM.getValue(BettaCraft.id(path));
            if (item == net.minecraft.world.item.Items.AIR) continue; // M41: the drive adapter exists only with Create
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
            var bucket = ModItems.TANKMATE_BUCKETS.get(sp); // M30: none for the big animals
            info(registration, bucket == null ? List.of(new ItemStack(ModItems.TANKMATE_SPAWN_EGGS.get(sp).get()))
                            : List.of(new ItemStack(bucket.get()), new ItemStack(ModItems.TANKMATE_SPAWN_EGGS.get(sp).get())),
                    "info.bettacraft.tankmate." + sp.id());
        }
    }

    /** A translatable text (JEI wraps it; the line breaks in the text stay), no client-only classes here. */
    private static void info(IRecipeRegistration registration, List<ItemStack> stacks, String key) {
        stacks = stacks.stream().filter(org.overengineer.bettacraft.config.FeatureGate::stack).toList();
        if (stacks.isEmpty()) return;
        registration.addIngredientInfo(stacks, VanillaTypes.ITEM_STACK, Component.translatable(key));
    }
}
