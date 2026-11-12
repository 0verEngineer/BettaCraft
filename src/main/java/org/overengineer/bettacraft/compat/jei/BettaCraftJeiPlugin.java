package org.overengineer.bettacraft.compat.jei;

import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.resources.language.I18n;
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

    private static void info(IRecipeRegistration registration, List<ItemStack> stacks, String key) {
        if (stacks.isEmpty() || !I18n.exists(key)) return;
        Component[] lines = ItemHints.lines(I18n.get(key)).toArray(Component[]::new);
        registration.addIngredientInfo(stacks, VanillaTypes.ITEM_STACK, lines);
    }
}
