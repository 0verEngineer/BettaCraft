package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/** M36 English texts (the size setting of the bettas). The German ones are in de_de.json; edit both. */
final class LangM36 {
    private LangM36() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("bettacraft.configuration.sizes", "Animal sizes");
        a.accept("bettacraft.configuration.betta", "Betta size");
        a.accept("bettacraft.configuration.betta.tooltip", "How big the bettas are drawn, with their hitbox (all species, every life stage in proportion): Normal = smaller, about halfway between DragN's Just Bettas and the size before Bettacraft 1.28; Large = the size before 1.28. Only the look and the hitbox: the stocking and the fish's data stay the same. Works at once, for every player on the server.");
        a.accept("bettacraft.configuration.size.normal", "Normal");
        a.accept("bettacraft.configuration.size.large", "Large");
    }
}
