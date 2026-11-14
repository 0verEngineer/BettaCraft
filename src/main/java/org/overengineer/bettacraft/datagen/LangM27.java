package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M27 English texts (the monitors, their values, the config).
 * Generated together with the German ones in de_de.json; edit both.
 */
final class LangM27 {
    private LangM27() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("block.bettacraft.monitor", "Monitor");
        a.accept("info.bettacraft.monitor", "A screen for your tanks. Monitors next to each other join into one screen of up to 8 x 5 blocks. It finds every tank within 32 blocks by itself and shows its values, named after the tank sign.\nWith a power option on it needs 1 FE per tick and block.");
        a.accept("message.bettacraft.monitor.info", "Monitor %s x %s: %s tanks within %s blocks, %s");
        a.accept("monitor.bettacraft.status.on", "on");
        a.accept("monitor.bettacraft.status.no_power", "No power");
        a.accept("monitor.bettacraft.status.off", "Switched off");
        a.accept("monitor.bettacraft.waiting", "Waiting for data...");
        a.accept("monitor.bettacraft.no_tanks", "No tanks in range");
        a.accept("monitor.bettacraft.unnamed", "Tank");
        a.accept("monitor.bettacraft.not_loaded", "not loaded");
        a.accept("monitor.bettacraft.no_data", "no data");
        a.accept("monitor.bettacraft.value.temperature", "Temperature");
        a.accept("monitor.bettacraft.value.water_quality", "Water quality");
        a.accept("monitor.bettacraft.value.ph", "pH");
        a.accept("monitor.bettacraft.value.tds", "TDS");
        a.accept("monitor.bettacraft.value.salinity", "Salinity");
        a.accept("monitor.bettacraft.value.calcium", "Calcium");
        a.accept("monitor.bettacraft.value.oxygen", "Oxygen");
        a.accept("monitor.bettacraft.value.co2", "CO2");
        a.accept("monitor.bettacraft.value.nitrate", "Nitrate");
        a.accept("monitor.bettacraft.value.fill", "Water level");
        a.accept("monitor.bettacraft.value.stocking", "Animals");
        a.accept("monitor.bettacraft.value.algae", "Algae");
        a.accept("monitor.bettacraft.value.food", "Food in the feeder");

        a.accept("bettacraft.configuration.monitor", "Monitors");
        a.accept("bettacraft.configuration.monitor.tooltip", "Screens of monitor blocks that show every tank in range");
        a.accept("bettacraft.configuration.range", "Monitor range");
        a.accept("bettacraft.configuration.range.tooltip", "Blocks from a monitor within which it finds the tanks (nearest point of the tank)");
        a.accept("bettacraft.configuration.fePerBlock", "Monitor FE per block");
        a.accept("bettacraft.configuration.fePerBlock.tooltip", "FE per tick of each monitor block while a power option is on (power.* or pipes.pumpsRequireEnergy)");
        a.accept("bettacraft.configuration.monitors", "Monitors");
        a.accept("bettacraft.configuration.monitors.tooltip", "Monitors: screens of monitor blocks that show the values of every tank in range, with graphs of the last days (off: the screens stay dark and record nothing)");
    }
}
