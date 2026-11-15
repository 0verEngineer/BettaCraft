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

        a.accept("bettacraft.configuration.createRotation", "Create rotation for devices");
        a.accept("bettacraft.configuration.createRotation.tooltip", "Create's rotation drives the water pumps, filter pump head, water change controller, level sensor, reverse osmosis unit and food maker instead of FE: a shaft or cogwheel pointing at the device, 64 RPM for full work, slower proportionally; no stress cost (off: only FE, the crank and the other sources as before)");
        a.accept("bettacraft.configuration.createDisplayLinks", "Create Display Links");
        a.accept("bettacraft.configuration.createDisplayLinks.tooltip", "Tanks and monitors are sources for Create's Display Link: the values of a tank, or one line per tank a monitor shows, on display boards, nixie tubes, signs and lecterns (off: Display Links cannot read them)");
    }
}
