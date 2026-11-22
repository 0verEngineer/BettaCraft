package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/** M62 part 4 English texts: the shoals that are too small (which fish, how many of how many). The German ones are in de_de.json. */
final class LangM62Menu {
    private LangM62Menu() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("gui.bettacraft.shoal.line", "%s: %s of %s");
        a.accept("gui.bettacraft.shoal.more", " (+%s more)");
        a.accept("gui.bettacraft.shoal.title", "Shoals too small (fish now / at least):");
        a.accept("gui.bettacraft.shoal.hint", "Shoaling fish in a smaller group are stressed and fall sick: add more of the same species or take the few out.");
        a.accept("gui.bettacraft.tank.shoal_warning", "Shoal too small: %s");
    }
}
