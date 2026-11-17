package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M49 English texts: part a the sump with its chambers and refugium, the calcium reactor; part b the brackish water animals
 * (archerfish, mudskipper, Amano shrimp) and the mangroves. The German ones are in de_de.json; edit both.
 */
final class LangM49 {
    private LangM49() {}

    static void addAll(BiConsumer<String, String> a) {
        partA(a);
        LangM49b.addAll(a);
    }

    private static void partA(BiConsumer<String, String> a) {
        // config
        a.accept("bettacraft.configuration.sump", "Sump");
        a.accept("bettacraft.configuration.sumpWaterBlocks", "Water per sump");
        a.accept("bettacraft.configuration.sumpWaterBlocks.tooltip", "Water blocks a sump in a cabinet adds to the tank above (the waste, the nitrate and the dosed values spread over it, and it counts for the stocking)");
        a.accept("bettacraft.configuration.refugiumNitratePerAlgae", "Nitrate per Chaetomorpha");
        a.accept("bettacraft.configuration.refugiumNitratePerAlgae.tooltip", "Nitrate (mg/L in one water block) one Chaetomorpha in a lit refugium takes up per day");
        a.accept("bettacraft.configuration.chaetoDoublingDays", "Chaetomorpha doubling (days)");
        a.accept("bettacraft.configuration.chaetoDoublingDays.tooltip", "Days in which the Chaetomorpha of a lit refugium doubles while it is young (it slows down towards a full chamber of 16)");
        a.accept("bettacraft.configuration.calciumReactorCalciumPerDay", "Calcium of the calcium reactor per day");
        a.accept("bettacraft.configuration.calciumReactorCalciumPerDay.tooltip", "Calcium (mg/L in one water block) a running calcium reactor adds per day; the alkalinity follows in the ratio the stony corals use up");
        a.accept("bettacraft.configuration.calciumReactorRubblePerDay", "Coral rubble of the calcium reactor per day");
        a.accept("bettacraft.configuration.calciumReactorRubblePerDay.tooltip", "Coral rubble a running calcium reactor dissolves per day");
        // items and blocks
        a.accept("item.bettacraft.sump", "Sump");
        a.accept("item.bettacraft.chaetomorpha", "Chaetomorpha");
        a.accept("item.bettacraft.coral_rubble", "Coral Rubble");
        a.accept("block.bettacraft.calcium_reactor", "Calcium Reactor");
        a.accept("tooltip.bettacraft.sump.usage", "Goes into the sump slot of a cabinet under a tank");
        a.accept("tooltip.bettacraft.sump.empty", "Chambers empty");
        a.accept("info.bettacraft.sump", "A small tank in the cabinet under an aquarium: put it into the sump slot of the cabinet (between the heater slots and the air pump).\n"
                + "Its water counts with the tank (0.5 blocks each): the waste, the nitrate and the dosed values change more slowly, and there is room for more animals. Its return pump runs like an external filter (with FE where the tank needs it).\n"
                + "The button above its slot opens the chambers: two heaters or coolers, a protein skimmer, two filter media and the refugium (Chaetomorpha and a tank light). A level sensor or a dosing pump next to the cabinet serves the tank above.");
        a.accept("info.bettacraft.chaetomorpha", "Spaghetti algae for the refugium of a sump: with a tank light in the refugium they take up nitrate, starve the algae on the glass of nutrients and grow (up to 16).\n"
                + "Take the extra ones out: food for shrimp and snails, a vegetable for the food maker, or into the composter. Sold by the wandering trader.");
        a.accept("info.bettacraft.calcium_reactor", "The calcium reactor of a reef tank: tank water runs through a column of coral rubble that CO2 dissolves, and brings calcium and alkalinity (KH) back for the stony corals.\n"
                + "Place it next to a salt water tank, the cabinet of its sump, or on a pipe to the tanks; a CO2 gas cylinder next to it. Coral rubble in (by hand or hopper). It holds the calcium at about 440 mg/L.");
        a.accept("info.bettacraft.coral_rubble", "Broken coral skeletons (calcium carbonate): the filling of the calcium reactor. From dead coral blocks or calcite.");
        // cabinet and sump windows
        a.accept("gui.bettacraft.cabinet.sump_open", "»");
        a.accept("gui.bettacraft.cabinet.sump_open.tooltip", "Open the chambers of the sump");
        a.accept("gui.bettacraft.cabinet.sump.tooltip", "Sump: a small tank whose water counts with the tank above; its chambers hold heaters, a skimmer, filter media and a refugium");
        a.accept("gui.bettacraft.sump.title", "Sump");
        a.accept("gui.bettacraft.sump.heaters", "Heaters");
        a.accept("gui.bettacraft.sump.skimmer", "Skimmer");
        a.accept("gui.bettacraft.sump.media", "Filter media");
        a.accept("gui.bettacraft.sump.refugium", "Refugium");
        a.accept("gui.bettacraft.sump.refugium_slots", "Algae · Light");
        a.accept("gui.bettacraft.sump.slot.heater", "A heater or cooler (like in an external filter)");
        a.accept("gui.bettacraft.sump.slot.skimmer", "A protein skimmer (cleans salt water like one on the wall)");
        a.accept("gui.bettacraft.sump.slot.media", "A filter medium: filter sponge, ceramic rings, activated carbon or filter peat (it clogs, wash it in a water cauldron)");
        a.accept("gui.bettacraft.sump.slot.algae", "Chaetomorpha (up to 16): takes up nitrate with a light and grows");
        a.accept("gui.bettacraft.sump.slot.light", "A tank light, LED panel or high power LED for the refugium");
        // the tab of the tank menu
        a.accept("gui.bettacraft.tab.sump", "Sump");
        a.accept("gui.bettacraft.sump.line", "Sumps: %s · their water %s blocks · together %s blocks");
        a.accept("gui.bettacraft.sump.pump_on", "Return pump runs");
        a.accept("gui.bettacraft.sump.pump_off", "Return pump stands still (no power)");
        a.accept("gui.bettacraft.sump.heaters_line", "Heaters and coolers in the sump: %s");
        a.accept("gui.bettacraft.sump.skimmer_line", "Protein skimmers in the sump: %s");
        a.accept("gui.bettacraft.sump.media_none", "No filter media in the sump");
        a.accept("gui.bettacraft.sump.media_line", "Filter media: %s · clean %s blocks · dirt up to %s %%");
        a.accept("gui.bettacraft.sump.refugium_off", "Refugium switched off");
        a.accept("gui.bettacraft.sump.refugium_empty", "Refugium: no Chaetomorpha");
        a.accept("gui.bettacraft.sump.refugium_dark", "Refugium: %s Chaetomorpha without light (or pump)");
        a.accept("gui.bettacraft.sump.refugium_line", "Refugium: %s/%s Chaetomorpha · nitrate -%s mg/L a day · glass algae -%s %%");
        a.accept("gui.bettacraft.sump.devices", "Beside the cabinet: %s level sensors, %s dosing pumps");
        a.accept("gui.bettacraft.sump.reactor", "a calcium reactor");
        a.accept("gui.bettacraft.sump.hint", "The chambers: the button above the sump in the cabinet");
        // overlays
        a.accept("jade.bettacraft.cabinet.sump", "Sump: %s heaters, %s skimmer, %s media");
        a.accept("jade.bettacraft.cabinet.refugium", "Refugium: %s Chaetomorpha");
        a.accept("jade.bettacraft.cabinet.refugium_dark", "Refugium: %s Chaetomorpha, no light");
        // the calcium reactor window
        a.accept("gui.bettacraft.calcium_reactor.running", "Dissolves coral rubble into %s tank(s)");
        a.accept("gui.bettacraft.calcium_reactor.fresh", "Only salt water: the tank is fresh water");
        a.accept("gui.bettacraft.calcium_reactor.off", "Switched off (features.sump.calciumReactor)");
        a.accept("gui.bettacraft.calcium_reactor.no_rubble", "No coral rubble");
        a.accept("gui.bettacraft.calcium_reactor.no_co2", "No CO2: a gas cylinder with gas next to it");
        a.accept("gui.bettacraft.calcium_reactor.full", "Calcium reached %s mg/L: the valve is closed");
        a.accept("gui.bettacraft.calcium_reactor.values", "Calcium %s mg/L · KH %s dKH");
    }
}
