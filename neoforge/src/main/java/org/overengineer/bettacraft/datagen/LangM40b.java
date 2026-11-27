package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M40 part b English texts (the pipe bundle, the fresh and waste water lines and ports, the water devices on the filter).
 * The German ones are in de_de.json; edit both.
 */
final class LangM40b {
    private LangM40b() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("block.bettacraft.filter_pipe_bundle", "Pipe Bundle");
    }
}
