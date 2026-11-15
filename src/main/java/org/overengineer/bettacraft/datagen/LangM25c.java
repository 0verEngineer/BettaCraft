package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M25 part c English texts (Caridina, the Sulawesi animals, reef dosing, the pressure regulator of the CO2 cylinder).
 * Generated together with the German ones in de_de.json; edit both.
 */
final class LangM25c {
    private LangM25c() {}

    static void addAll(BiConsumer<String, String> a) {
        // animals
        a.accept("critter.bettacraft.caridina", "Bee Shrimp");
        a.accept("critter.bettacraft.sulawesi_shrimp", "Cardinal Shrimp");
        a.accept("critter.bettacraft.tylomelania", "Tylomelania Snail");
        a.accept("variety.bettacraft.caridina.wild", "Wild Form");
        for (String line : new String[]{"red", "black"}) {
            String name = "Crystal " + (line.equals("red") ? "Red" : "Black");
            a.accept("variety.bettacraft.caridina.crystal_" + line, name);
            a.accept("variety.bettacraft.caridina.crystal_" + line + "_s", name + " S");
            a.accept("variety.bettacraft.caridina.crystal_" + line + "_ss", name + " SS");
            a.accept("variety.bettacraft.caridina.crystal_" + line + "_sss", name + " SSS");
        }
        a.accept("variety.bettacraft.caridina.blue_bolt", "Blue Bolt");
        a.accept("variety.bettacraft.caridina.wine_red", "Wine Red");
        a.accept("variety.bettacraft.caridina.orange_eye_blue_tiger", "Orange Eye Blue Tiger");
        // reef dosing
        a.accept("block.bettacraft.dosing_pump", "Dosing Pump");
        a.accept("item.bettacraft.dosing_canister", "Empty Dosing Canister");
        a.accept("item.bettacraft.calcium_canister", "Calcium Canister");
        a.accept("item.bettacraft.alkalinity_canister", "Alkalinity (KH) Canister");
        a.accept("item.bettacraft.magnesium_canister", "Magnesium Canister");
        a.accept("item.bettacraft.trace_canister", "Trace Element Canister");
        a.accept("tooltip.bettacraft.dosing_canister.empty", "Empty: fill it in the crafting grid");
        a.accept("tooltip.bettacraft.dosing_canister.left", "%s of %s mL left");
        a.accept("info.bettacraft.dosing_pump", "Dosing pump for a reef tank (\"Balling light\"): four channels, each with a canister and its mL per day.\nPlace it next to a salt water tank, or join it to pipes that lead into tanks (several tanks share the dose). Right-click: canisters in, mL per day with - and + (sneak: by 10), what it does and the values of the tank.\nIt only doses into salt water. Hoppers put full canisters in and take empty ones out.");
        a.accept("info.bettacraft.dosing_canister", "Empty canister of the dosing pump. Fill it in the crafting grid: calcium (two calcium additives), alkalinity (calcite and two dried kelp), magnesium (two sea salt and a prismarine shard) or trace elements (sea salt, prismarine crystals and a glow ink sac).");
        a.accept("info.bettacraft.calcium_canister", "Calcium solution for the dosing pump. Stony corals use calcium to build their skeletons.\nDose calcium and alkalinity in about equal mL.");
        a.accept("info.bettacraft.alkalinity_canister", "Alkalinity (KH) solution for the dosing pump. Stony corals use it together with the calcium; it also holds the pH of salt water up.\nBelow 6 dKH they slow down, below 3 they stop.");
        a.accept("info.bettacraft.magnesium_canister", "Magnesium solution for the dosing pump. Magnesium falls slowly in every salt water tank; below 1150 mg/L calcium and alkalinity drop out and the stony corals grow slower.");
        a.accept("info.bettacraft.trace_canister", "Trace element solution for the dosing pump. Fresh sea water has some; they are used up within days. Above fresh sea water's level the corals show their strong colours.");
        // window of the pump
        a.accept("gui.bettacraft.dosing.button", "mL per day of this channel (sneak: by 10)");
        a.accept("gui.bettacraft.dosing.additive.calcium", "Calcium");
        a.accept("gui.bettacraft.dosing.additive.alkalinity", "Alkalinity");
        a.accept("gui.bettacraft.dosing.additive.magnesium", "Magnesium");
        a.accept("gui.bettacraft.dosing.additive.trace", "Trace el.");
        a.accept("gui.bettacraft.dosing.dry", "Empty!");
        a.accept("gui.bettacraft.dosing.no_canister", "No canister");
        a.accept("gui.bettacraft.dosing.channel_off", "off");
        a.accept("gui.bettacraft.dosing.per_day", "%s mL/day");
        a.accept("gui.bettacraft.dosing.running", "Dosing into %s salt water tank(s)");
        a.accept("gui.bettacraft.dosing.idle", "Ready: put canisters in and set the mL per day");
        a.accept("gui.bettacraft.dosing.empty", "A canister is empty: refill it");
        a.accept("gui.bettacraft.dosing.no_tank", "No tank next to it or at the end of its pipes");
        a.accept("gui.bettacraft.dosing.fresh", "Not a salt water tank: it does not dose");
        a.accept("gui.bettacraft.dosing.off", "Reef dosing is switched off (features.corals.reefDosing)");
        a.accept("gui.bettacraft.dosing.values", "Tank: Ca %s · KH %s · Mg %s · trace %s %%");
        // tank menu
        a.accept("gui.bettacraft.reef.kh", " · KH %s");
        a.accept("gui.bettacraft.water.reef_tip", "Alkalinity %s dKH · magnesium %s mg/L · trace elements %s %%");
        a.accept("gui.bettacraft.water.reef_tip.low", "Too little alkalinity or magnesium: the stony corals slow down (dosing pump, water change with sea water)");
        a.accept("gui.bettacraft.water.reef_tip.trace", "Trace elements: the corals show their strong colours");
        // the pressure regulator of the CO2 cylinder (part b)
        a.accept("gui.bettacraft.co2.quick", "%s bubbles/s");
        a.accept("gui.bettacraft.co2.hint", "Bubbles per second: a bigger tank needs more. Also sneak + mouse wheel.");
        // config
        a.accept("bettacraft.configuration.dosing", "Reef dosing");
        a.accept("bettacraft.configuration.calciumPerMl", "Calcium per mL");
        a.accept("bettacraft.configuration.calciumPerMl.tooltip", "Calcium (mg/L in one water block) one mL of calcium solution adds (a bigger tank needs more)");
        a.accept("bettacraft.configuration.alkalinityPerMl", "Alkalinity per mL");
        a.accept("bettacraft.configuration.alkalinityPerMl.tooltip", "Alkalinity (dKH in one water block) one mL of alkalinity solution adds (2 mg/L calcium match 0.07 dKH)");
        a.accept("bettacraft.configuration.magnesiumPerMl", "Magnesium per mL");
        a.accept("bettacraft.configuration.magnesiumPerMl.tooltip", "Magnesium (mg/L in one water block) one mL of magnesium solution adds");
        a.accept("bettacraft.configuration.tracePerMl", "Trace elements per mL");
        a.accept("bettacraft.configuration.tracePerMl.tooltip", "Trace elements (share of the full level in one water block) one mL of trace element solution adds");
        a.accept("bettacraft.configuration.magnesiumDropPerDay", "Magnesium loss");
        a.accept("bettacraft.configuration.magnesiumDropPerDay.tooltip", "Magnesium (mg/L) a salt water tank loses per day besides what the stony corals use (it falls out with the calcium; 0 = only the corals use it)");
        a.accept("bettacraft.configuration.traceUsePerDay", "Trace element use");
        a.accept("bettacraft.configuration.traceUsePerDay.tooltip", "Trace elements (share of the full level) a salt water tank uses up per day");
        a.accept("bettacraft.configuration.canisterMl", "Canister size");
        a.accept("bettacraft.configuration.canisterMl.tooltip", "mL of solution in one canister of the dosing pump");
        a.accept("bettacraft.configuration.maxMlPerDay", "Most mL per day");
        a.accept("bettacraft.configuration.maxMlPerDay.tooltip", "Most mL per day one channel of the dosing pump can be set to");
    }
}
