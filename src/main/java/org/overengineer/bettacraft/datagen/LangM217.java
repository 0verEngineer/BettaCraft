package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/** M21.7 English texts (stone cabinets). de_de.json holds the German ones. */
final class LangM217 {
    private LangM217() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("bettacraft.configuration.stoneCabinets", "Stone cabinets");
        a.accept("bettacraft.configuration.stoneCabinets.tooltip", "Stone cabinets: the aquarium cabinet can also be crafted from stone (also of other mods) with the slabs of the same stone; it looks like that stone and is mined with a pickaxe (off: no new ones can be crafted; placed ones stay)");
    }
}
