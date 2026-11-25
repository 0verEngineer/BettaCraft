package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M40 part b English texts (the pipe bundle, the fresh and waste water lines and ports, the water devices on the filter).
 * The German ones are in de_de.json; edit both.
 */
final class LangM40b {
    private LangM40b() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("block.bettacraft.filter_fresh_pipe", "Filter Fresh Water Pipe");
        a.accept("block.bettacraft.filter_waste_pipe", "Filter Waste Water Pipe");
        a.accept("block.bettacraft.filter_pipe_bundle", "Filter Pipe Bundle");
        a.accept("block.bettacraft.filter_fresh_port", "Filter Fresh Water Port");
        a.accept("block.bettacraft.filter_waste_port", "Filter Waste Water Port");
        a.accept("tooltip.bettacraft.filter_pipe.bundle", "Right-click a filter pipe of another line with it: both run in one block (up to four; sneaking places it beside); sneak-right-click with an empty hand takes the line you look at out");
        a.accept("tooltip.bettacraft.filter_water_pipe.usage", "Joins at most two neighbours (a T-piece: all): its port on the filter, water pumps, reservoirs, the pipes of other mods, ME/RS buses");
        a.accept("tooltip.bettacraft.filter_port.usage", "Place it against a filter chamber; a level sensor or water change controller on the filter then works on all its tanks");
        a.accept("message.bettacraft.pipe_bundle.has_line", "This line runs here already");
        a.accept("gui.bettacraft.mb_filter.water", "Fresh water: %s · waste water: %s mB");
        a.accept("gui.bettacraft.mb_filter.no_waste", "Fresh water: %s · no waste water port: old water is discarded");
        a.accept("gui.bettacraft.water_changer.filter", "On the multiblock filter: %s tanks");
        a.accept("gui.bettacraft.water_changer.filter_waste", "Old water to the waste water port: %s mB");
        a.accept("info.bettacraft.filter_fresh_pipe", "The fresh water line of the multiblock filter: from a water pump, a reservoir, an ME or RS export bus or the pipes of other mods to the filter's fresh water port. It joins at most two neighbours (a T-piece: all) and never a tank. Right-click another filter pipe with it to run both in one block (a pipe bundle, up to four lines).");
        a.accept("info.bettacraft.filter_waste_pipe", "The waste water line of the multiblock filter: from its waste water port to a tank, a reservoir, the pipes of other mods or an ME/RS import bus. Other mods' pipes and pumps can pull the old water from it. It joins at most two neighbours (a T-piece: all).");
        a.accept("info.bettacraft.filter_fresh_port", "The fresh water connection of the multiblock filter: place it against a chamber, its green socket facing out. Whatever pushes water into it or its fresh water line fills it (8 buckets); a water source block next to it is endless tap water. A level sensor or water change controller touching any part of the filter then tops off and changes the water of every tank the filter serves, shared by what each tank needs.");
        a.accept("info.bettacraft.filter_waste_port", "The waste water connection of the multiblock filter: the old water of a water change through the filter goes out here, on to the end of its waste water line (a tank, a reservoir, another mod's pipe or storage); it keeps up to 8 buckets for pipes that pull. Without a waste water port the old water is discarded.");
    }
}
