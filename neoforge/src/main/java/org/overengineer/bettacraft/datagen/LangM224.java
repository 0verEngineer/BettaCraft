package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M22.4 English texts (plants from the aquarium net, the plant tables of the net and of fishing, the outlet side of the
 * cabinet filter). de_de.json holds the German ones.
 */
final class LangM224 {
    private LangM224() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("message.bettacraft.net.plant", "The net brought up a plant from the bottom: %s");
        // the cabinet's outlet side
        a.accept("gui.bettacraft.cabinet.outlet", "Outlet: %s");
        a.accept("gui.bettacraft.cabinet.outlet.back", "back");
        a.accept("gui.bettacraft.cabinet.outlet.left", "left");
        a.accept("gui.bettacraft.cabinet.outlet.right", "right");
        a.accept("gui.bettacraft.cabinet.outlet.front", "front");
        a.accept("gui.bettacraft.cabinet.outlet.tooltip", "Which tank wall the filter hose goes up (seen from the cabinet door). Jet, bubbles and the current come from there; in front of the outlet the water moves faster. Click: next side, sneak + click: previous.");
        a.accept("tooltip.bettacraft.cabinet.outlet", "Filter outlet: %s");
        // config
        a.accept("bettacraft.configuration.netPlantShare", "Plants in the net");
        a.accept("bettacraft.configuration.netPlantShare.tooltip", "Share of the aquarium net's catches in rivers, jungle water and swamps that is an aquarium plant from the bottom (times critters.netChance; 0 = the net brings up no plants)");
        a.accept("bettacraft.configuration.netPlantsRiver", "Net plants in rivers");
        a.accept("bettacraft.configuration.netPlantsRiver.tooltip", "Plants the net brings up in rivers: entries \"plant_id=weight\" (item ids without \"bettacraft:\"); empty = none");
        a.accept("bettacraft.configuration.netPlantsJungle", "Net plants in jungle water");
        a.accept("bettacraft.configuration.netPlantsJungle.tooltip", "Plants the net brings up in jungle water: entries \"plant_id=weight\" (item ids without \"bettacraft:\"); empty = none");
        a.accept("bettacraft.configuration.netPlantsSwamp", "Net plants in swamps");
        a.accept("bettacraft.configuration.netPlantsSwamp.tooltip", "Plants the net brings up in swamps: entries \"plant_id=weight\" (item ids without \"bettacraft:\"); empty = none");
        a.accept("bettacraft.configuration.fishingPlantsJungle", "Fishing plants in jungles");
        a.accept("bettacraft.configuration.fishingPlantsJungle.tooltip", "Plants fishing brings up in jungles (plantFishingChance): entries \"plant_id=weight\"; empty = none");
        a.accept("bettacraft.configuration.fishingPlantsRiver", "Fishing plants in rivers");
        a.accept("bettacraft.configuration.fishingPlantsRiver.tooltip", "Plants fishing brings up in rivers (plantFishingChance): entries \"plant_id=weight\"; empty = none");
        a.accept("bettacraft.configuration.fishingPlantsSwamp", "Fishing plants in swamps");
        a.accept("bettacraft.configuration.fishingPlantsSwamp.tooltip", "Plants fishing brings up in swamps (plantFishingChance): entries \"plant_id=weight\"; empty = none");
        a.accept("bettacraft.configuration.fishingPlantsMangrove", "Fishing plants in mangrove swamps");
        a.accept("bettacraft.configuration.fishingPlantsMangrove.tooltip", "Plants fishing brings up in mangrove swamps (plantFishingChance): entries \"plant_id=weight\"; empty = none");
    }
}
