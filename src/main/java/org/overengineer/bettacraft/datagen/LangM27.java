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
        a.accept("info.bettacraft.monitor", "A screen for your tanks. Monitors next to each other join into one screen of up to 8 x 5 blocks. It finds every tank within 32 blocks by itself, named after the tank sign. Right-click it to arrange the dashboard: values, traffic lights, graphs, animals, devices, alarms (with a redstone signal) and the log. Sneak and right-click for its state.\nWith a power option on it needs 1 FE per tick and block.");
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

        // part b: widgets, editor, alarms
        a.accept("monitor.bettacraft.value.kh", "KH");
        a.accept("monitor.bettacraft.value.magnesium", "Magnesium");
        a.accept("monitor.bettacraft.tile.value", "Value");
        a.accept("monitor.bettacraft.tile.status", "State");
        a.accept("monitor.bettacraft.tile.graph", "Graph");
        a.accept("monitor.bettacraft.tile.animals", "Animals");
        a.accept("monitor.bettacraft.tile.devices", "Devices");
        a.accept("monitor.bettacraft.tile.alarm", "Alarm");
        a.accept("monitor.bettacraft.tile.log", "Log");
        a.accept("monitor.bettacraft.state.ok", "All fine");
        a.accept("monitor.bettacraft.state.warning", "Warning");
        a.accept("monitor.bettacraft.state.alarm", "Alarm");
        a.accept("monitor.bettacraft.state.not_loaded", "not loaded");
        a.accept("monitor.bettacraft.more", "+%s more");
        a.accept("monitor.bettacraft.days", "%s d");
        a.accept("monitor.bettacraft.no_history", "No history yet");
        a.accept("monitor.bettacraft.nest.building", "Nest being built");
        a.accept("monitor.bettacraft.nest.built", "Nest ready");
        a.accept("monitor.bettacraft.nest.eggs", "Nest with eggs");
        a.accept("monitor.bettacraft.eggs", "%s eggs");
        a.accept("monitor.bettacraft.young", "%s young");
        a.accept("monitor.bettacraft.young_short", "(%s young)");
        a.accept("monitor.bettacraft.berried", "%s carrying eggs");
        a.accept("monitor.bettacraft.no_animals", "No animals");
        a.accept("monitor.bettacraft.device.filter", "Filters");
        a.accept("monitor.bettacraft.device.heater", "Heaters");
        a.accept("monitor.bettacraft.device.cooler", "Coolers");
        a.accept("monitor.bettacraft.device.light", "Light");
        a.accept("monitor.bettacraft.device.co2", "CO2");
        a.accept("monitor.bettacraft.device.air_pump", "Air pumps");
        a.accept("monitor.bettacraft.none", "none");
        a.accept("monitor.bettacraft.energy", "Energy %s / %s FE, %s FE/t");
        a.accept("monitor.bettacraft.alarm", "ALARM");
        a.accept("monitor.bettacraft.above", "Alarm above %s");
        a.accept("monitor.bettacraft.below", "Alarm below %s");
        a.accept("monitor.bettacraft.redstone", "Redstone");
        a.accept("monitor.bettacraft.all_tanks", "All tanks");
        a.accept("monitor.bettacraft.log_empty", "No entries yet");
        a.accept("monitor.bettacraft.day", "D%s");
        a.accept("monitor.bettacraft.warn.low_water", "Too little water");
        a.accept("monitor.bettacraft.warn.water_bad", "Water bad (%s %%)");
        a.accept("monitor.bettacraft.warn.water_poor", "Water poor (%s %%)");
        a.accept("monitor.bettacraft.warn.no_filter", "No filter");
        a.accept("monitor.bettacraft.warn.nitrate_lethal", "Nitrate deadly (%s mg/l)");
        a.accept("monitor.bettacraft.warn.nitrate_high", "Nitrate high (%s mg/l)");
        a.accept("monitor.bettacraft.warn.no_power", "Devices without power");
        a.accept("monitor.bettacraft.warn.crowded", "Overcrowded");
        a.accept("monitor.bettacraft.warn.sick", "%s sick");
        a.accept("monitor.bettacraft.warn.feeder_empty", "Feeder empty");
        a.accept("gui.bettacraft.monitor.title", "Monitor dashboard");
        a.accept("gui.bettacraft.monitor.info", "Screen %s x %s blocks, %s tanks in range");
        a.accept("gui.bettacraft.monitor.add", "Add tile");
        a.accept("gui.bettacraft.monitor.auto", "Automatic");
        a.accept("gui.bettacraft.monitor.save", "Save");
        a.accept("gui.bettacraft.monitor.type", "Type: %s");
        a.accept("gui.bettacraft.monitor.tank", "Tank: %s");
        a.accept("gui.bettacraft.monitor.no_tank", "none");
        a.accept("gui.bettacraft.monitor.value", "Value: %s");
        a.accept("gui.bettacraft.monitor.line", "%s: %s");
        a.accept("gui.bettacraft.monitor.line_none", "%s: -");
        a.accept("gui.bettacraft.monitor.spark_on", "Graph of the last day: on");
        a.accept("gui.bettacraft.monitor.spark_off", "Graph of the last day: off");
        a.accept("gui.bettacraft.monitor.above", "Alarm above");
        a.accept("gui.bettacraft.monitor.below", "Alarm below");
        a.accept("gui.bettacraft.monitor.limit", "Limit");
        a.accept("gui.bettacraft.monitor.redstone_on", "Redstone signal: on");
        a.accept("gui.bettacraft.monitor.redstone_off", "Redstone signal: off");
        a.accept("gui.bettacraft.monitor.background", "Background");
        a.accept("gui.bettacraft.monitor.foreground", "Text colour");
        a.accept("gui.bettacraft.monitor.delete", "Remove tile");
        a.accept("gui.bettacraft.monitor.full", "No room for another tile");
        a.accept("gui.bettacraft.monitor.help", "Click a tile to edit it. Drag it to move it, drag its bottom right corner to resize it. Buttons with shift go backwards. Save to put the dashboard on the screen; Automatic gives it back the automatic layout.");

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
