package org.overengineer.bettacraft.progression;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.decor.DecorItem;

/**
 * Short explanations per item (M6, PLAN 9 "without GuideME"): shown as "hold Shift for details" tooltip lines and as JEI
 * information pages. Text in {@code info.bettacraft.<item>} (all decoration items share {@code info.bettacraft.decor}),
 * lines separated by {@code \n}.
 */
public final class ItemHints {
    /** Item paths with their own info text (order = JEI order). */
    public static final List<String> ITEMS = List.of("tank", "betta_bucket", "fish_bag", "swamp_water_bottle", "mosquito_larvae",
            "betta_pellets", "salt_water_bottle", "artemia_cysts", "infusoria_culture", "infusoria", "artemia_culture", "artemia_nauplii",
            "heater_small", "heater_medium", "heater_large", "cooler_small", "cooler_medium", "cooler_large", "internal_filter", "external_filter",
            "feeder", "tank_light", "tank_lid", "cabinet", "larva_tub", "artemia_hatchery", "mirror_stick",
            "tank_net", "duckweed", "amazon_frogbit", "salvinia", "red_root_floater",
            // M13: the aquarium plants
            "java_fern", "anubias_nana", "bucephalandra_green_wavy", "bucephalandra_brownie_red", "bucephalandra_blue",
            "bucephalandra_mini_needle", "java_moss", "cryptocoryne_wendtii_green", "cryptocoryne_wendtii_brown", "cryptocoryne_flamingo",
            "amazon_sword", "rotala_rotundifolia", "ludwigia_repens", "alternanthera_reineckii", "vallisneria", "monte_carlo", "red_tiger_lotus",
            "betta_spawn_egg",
            // M14: algae, frogs
            "algae_scraper", "dwarf_frog_bucket", "dwarf_frog_spawn_egg");

    private ItemHints() {}

    /** Translation key of the details of {@code item}, or null if it has none. */
    public static String key(Item item) {
        var id = BuiltInRegistries.ITEM.getKey(item);
        if (!BettaCraft.MOD_ID.equals(id.getNamespace())) return null;
        if (item instanceof DecorItem) return "info.bettacraft.decor";
        return ITEMS.contains(id.getPath()) ? "info.bettacraft." + id.getPath() : null;
    }

    /** The lines of a (resolved) info text. */
    public static List<Component> lines(String resolved) {
        List<Component> out = new ArrayList<>();
        for (String line : resolved.split("\n")) out.add(Component.literal(line));
        return out;
    }
}
