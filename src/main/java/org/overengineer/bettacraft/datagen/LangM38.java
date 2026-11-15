package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/** M38 English texts (plants of Minecraft and other mods in the tanks). The German ones are in de_de.json; edit both. */
final class LangM38 {
    private LangM38() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("bettacraft.configuration.foreignPlantFactor", "Plants of other mods");
        a.accept("bettacraft.configuration.foreignPlantFactor.tooltip", "A plant or coral of Minecraft or another mod in the interior works like a full-grown plant of ours of the same growth form times this (nitrate, waste, breeding, hiding; their live rock: times ours); they do not grow and have no needs (0 = no effect)");
        a.accept("gui.bettacraft.aquatic.foreign", "Plants of other mods: %s (mass %s, they do not grow)");
        a.accept("gui.bettacraft.aquatic.foreign_share", "Plant mass: %s%% of the water volume");
        a.accept("command.bettacraft.foreign.plants_off", "features.aquaticPlants.foreignPlants is off: these plants are decoration only");
        a.accept("command.bettacraft.foreign.plants_tank", "%s at %s: %s plant(s) of Minecraft and other mods, %s%% of the water volume");
        a.accept("command.bettacraft.foreign.plant", "  %s: %s plant(s) as %s, plant mass %s, profile %s");
        a.accept("command.bettacraft.foreign.live_rock", "  live rock of other mods: %s block(s) (biological filter in salt water)");
        a.accept("command.bettacraft.foreign.over_limit", "  %s more plant(s) over interiorPlants.maxPlantsPerTank do not count");
        a.accept("command.bettacraft.foreign.plants_none", "No tank within %s blocks has plants of Minecraft or other mods in its interior");
        a.accept("command.bettacraft.foreign.plants_none_here", "This tank has no plants of Minecraft or other mods in its interior");
        a.accept("command.bettacraft.foreign.plants_unprofiled", "Plant blocks of other mods without a profile in these interiors (decoration only): %s");
        a.accept("command.bettacraft.foreign.plants_unprofiled.none", "Every plant block of other mods in these interiors has a profile");
    }
}
