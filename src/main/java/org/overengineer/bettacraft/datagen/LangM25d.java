package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/** M25 part d English texts (active soil). Generated together with the German ones in de_de.json; edit both. */
final class LangM25d {
    private LangM25d() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("block.bettacraft.active_soil", "Active Soil");
        a.accept("info.bettacraft.active_soil", "Substrate for soft water: sneak + right-click an interior floor block with it.\nIt pulls the pH towards 6.2, softens the water and slowly takes up minerals: just right for bee shrimp. Freshwater plants grow up to a quarter faster on it. It keeps working, it never wears out.\nPlain soil (dirt, mud ...) works only half as strongly.");
    }
}
