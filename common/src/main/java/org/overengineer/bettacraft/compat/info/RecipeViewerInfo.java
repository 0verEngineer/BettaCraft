package org.overengineer.bettacraft.compat.info;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * M52: the information pages of the recipe viewers (JEI, REI, EMI): which items get which text (lang key). The items of a
 * feature that is off are left out; with {@code features.compat.jei} off there are none.
 */
public final class RecipeViewerInfo {
    public record Page(List<ItemStack> stacks, String key) {}

    private RecipeViewerInfo() {}

    public static List<Page> pages() {
        List<Page> out = new ArrayList<>();
        if (!org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.COMPAT_JEI)) return out;
        for (String path : org.overengineer.bettacraft.progression.ItemHints.ITEMS) {
            Item item = BuiltInRegistries.ITEM.getValue(BettaCraft.id(path));
            if (item == Items.AIR) continue; // the drive adapter exists only with Create
            add(out, List.of(new ItemStack(item)), "info.bettacraft." + path);
        }
        List<ItemStack> decor = new ArrayList<>();
        for (var d : ModItems.DECOR.values()) decor.add(new ItemStack(d.get()));
        add(out, decor, "info.bettacraft.decor");
        List<ItemStack> wild = new ArrayList<>();
        for (var c : ModItems.WILD_CORALS.values()) wild.add(new ItemStack(c.get()));
        add(out, wild, "info.bettacraft.wild_coral");
        for (var sp : org.overengineer.bettacraft.tankmates.TankmateSpecies.values()) {
            var bucket = ModItems.TANKMATE_BUCKETS.get(sp);
            add(out, bucket == null ? List.of(new ItemStack(ModItems.TANKMATE_SPAWN_EGGS.get(sp).get()))
                    : List.of(new ItemStack(bucket.get()), new ItemStack(ModItems.TANKMATE_SPAWN_EGGS.get(sp).get())), "info.bettacraft.tankmate." + sp.id());
        }
        return out;
    }

    private static void add(List<Page> out, List<ItemStack> stacks, String key) {
        stacks = stacks.stream().filter(org.overengineer.bettacraft.config.FeatureGate::stack).toList();
        if (!stacks.isEmpty()) out.add(new Page(stacks, key));
    }
}
