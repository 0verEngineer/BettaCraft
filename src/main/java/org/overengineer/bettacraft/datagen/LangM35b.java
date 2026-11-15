package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M35 part b English texts (more land animals of other mods, fish in the water part of a paludarium). The German ones are in
 * de_de.json; edit both.
 */
final class LangM35b {
    private LangM35b() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("message.bettacraft.paludarium.too_shallow", "The water part is too shallow for %1$s: %2$s blocks deep, it needs %3$s");
    }
}
