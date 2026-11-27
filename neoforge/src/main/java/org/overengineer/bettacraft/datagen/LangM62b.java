package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/** M62 parts 2 and 3 English texts (the shared spring of the under-cabinet, the tank of an external filter). The German ones are in de_de.json; edit both. */
final class LangM62b {
    private LangM62b() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("tooltip.bettacraft.external_filter.link", "Also one or two blocks lower beside the tank: sneak-right-click it with an empty hand to choose the tank");
        a.accept("message.bettacraft.filter_link.none", "No tank in reach: put the filter beside a tank or one or two blocks lower beside it");
        a.accept("message.bettacraft.filter_link.none_beside", "No tank beside the filter");
        a.accept("message.bettacraft.filter_link.linked", "The filter now pumps into %s");
        a.accept("message.bettacraft.filter_link.hint", "Not connected: sneak-right-click the filter with an empty hand to choose its tank");
        a.accept("gui.bettacraft.filter_link.title", "Which tank does the filter feed?");
        a.accept("gui.bettacraft.filter_link.help", "The tank under the mouse is outlined");
        a.accept("gui.bettacraft.filter_link.unnamed", "Tank");
        a.accept("gui.bettacraft.filter_link.option", "%s at %s %s %s (%s)");
        a.accept("gui.bettacraft.filter_link.beside", "%s, beside");
        a.accept("gui.bettacraft.filter_link.below", "%s, %s below");
        a.accept("gui.bettacraft.filter_link.dir.north", "north");
        a.accept("gui.bettacraft.filter_link.dir.south", "south");
        a.accept("gui.bettacraft.filter_link.dir.east", "east");
        a.accept("gui.bettacraft.filter_link.dir.west", "west");
        a.accept("jade.bettacraft.filter.link_hint", "Sneak-right-click with an empty hand to choose a tank");
        a.accept("jade.bettacraft.filter.below", "Stands %s below the tank");
    }
}
