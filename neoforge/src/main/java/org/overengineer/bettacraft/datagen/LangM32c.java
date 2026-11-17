package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M32 part c English texts (packed tanks, Create rotation and Display Links). The German ones are in de_de.json; edit
 * both.
 */
final class LangM32c {
    private LangM32c() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("tooltip.bettacraft.tank.packed", "Packed tank with %s animal(s): its water, interior and animals travel inside. Place it to set the tank up again.");

        a.accept("bettacraft.display_source.tank_values", "Tank values");
        a.accept("bettacraft.display_source.monitored_tanks", "Monitored tanks");
        a.accept("display.bettacraft.not_loaded", "not loaded");

    }
}
