package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M67 English texts (one energy store per tank, the crank at the cabinet and the external filter, power.costs). The German
 * ones are in de_de.json; edit both.
 */
final class LangM67 {
    private LangM67() {}

    static void addAll(BiConsumer<String, String> a) {
        // ---- config
        a.accept("bettacraft.configuration.costs", "Power costs");
        a.accept("bettacraft.configuration.costs.tooltip", "What the devices cost: OFF = no device needs power, EASY = a third of the FE values (every consumer), HARD = the FE values as they are. Replaces the old switches heaterRequiresEnergy, filterRequiresEnergy, lightRequiresEnergy, airPumpRequiresEnergy, pumpsRequireEnergy and misterRequiresEnergy (an old config with any of them on becomes HARD)");
        a.accept("bettacraft.configuration.storePerWaterBlock", "Store per water block");
        a.accept("bettacraft.configuration.storePerWaterBlock.tooltip", "FE the tank's store holds per water block of the tank (80000: a full store runs a 2x2x2 tank with filter, light and a heater that heats one in-game day on EASY)");
        a.accept("bettacraft.configuration.storePerCabinet", "Store per cabinet");
        a.accept("bettacraft.configuration.storePerCabinet.tooltip", "FE every cabinet block under the tank (and every cabinet joined to them) adds to the tank's store");
        a.accept("bettacraft.configuration.crankFePerClick", "FE per crank click");
        a.accept("bettacraft.configuration.crankFePerClick.tooltip", "FE one click of the crank at a cabinet or an attached external filter puts into the tank's store (holding the crank clicks about five times a second: 6500 = about 20 seconds for one day of a 2x2x2 tank on EASY)");
        // ---- the crank
        a.accept("message.bettacraft.crank.store", "%s: tank store %s%% (%s FE, %s h at this demand)");
        a.accept("message.bettacraft.crank.store_full", "%s: the tank's store is full");
        a.accept("message.bettacraft.crank.no_tank", "%s is not attached to a tank: the crank charges a tank's store");
        a.accept("jade.bettacraft.store", "Tank store: %s%% (%s / %s FE)");
        // ---- the tank menu
        a.accept("gui.bettacraft.water.power_store", "One store for every device of the tank: charge it with FE cables or with the crank at a cabinet under the tank or at the external filter.");
        a.accept("gui.bettacraft.water.power_hours", "Lasts %s in-game hours at this demand");
        a.accept("gui.bettacraft.water.power_costs", "Power costs: %s");
        a.accept("gui.bettacraft.power_costs.off", "off");
        a.accept("gui.bettacraft.power_costs.easy", "easy");
        a.accept("gui.bettacraft.power_costs.hard", "hard");
    }
}
