package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/** M18 English texts: young animals in the stocking, coloured aquarium lights (de_de.json by hand). */
final class LangM18 {
    private LangM18() {}

    static void addAll(BiConsumer<String, String> a) {
        // stocking
        a.accept("gui.bettacraft.tank.stocking_fish", "Animals: %s (stocking %s of %s)");
        a.accept("gui.bettacraft.stock.title", "Stocking: %s of %s fish");
        a.accept("gui.bettacraft.stock.adults", "%s adults: %s");
        a.accept("gui.bettacraft.stock.young", "%s young: %s");
        a.accept("gui.bettacraft.stock.critters", "%s small animals: %s");
        a.accept("gui.bettacraft.stock.nest", "Eggs and larvae in the nest: %s");
        a.accept("gui.bettacraft.stock.hint", "Each fry counts %s, juvenile %s, subadult %s of a fish");
        a.accept("gui.bettacraft.fish.counts", "Counts %s of a fish for the stocking");
        a.accept("bettacraft.configuration.eggStocking", "Egg (share of a fish)");
        a.accept("bettacraft.configuration.eggStocking.tooltip", "How much of a fish one egg in the bubble nest counts for the stocking (0 = not at all)");
        a.accept("bettacraft.configuration.larvaStocking", "Larva (share of a fish)");
        a.accept("bettacraft.configuration.larvaStocking.tooltip", "How much of a fish one larva in the bubble nest counts for the stocking (0 = not at all)");
        a.accept("bettacraft.configuration.fryStocking", "Fry (share of a fish)");
        a.accept("bettacraft.configuration.fryStocking.tooltip", "How much of a fish one fry counts for the stocking");
        a.accept("bettacraft.configuration.juvenileStocking", "Juvenile (share of a fish)");
        a.accept("bettacraft.configuration.juvenileStocking.tooltip", "How much of a fish one juvenile counts for the stocking; also young animals of other mods, and young shrimp, crayfish and snails in their share of a fish");
        a.accept("bettacraft.configuration.subadultStocking", "Subadult (share of a fish)");
        a.accept("bettacraft.configuration.subadultStocking.tooltip", "How much of a fish one subadult counts for the stocking");
        // coloured lights
        a.accept("block.bettacraft.tank_light.colored", "Aquarium Light (%s)");
        a.accept("bettacraft.configuration.lightTint", "Water tint of coloured lights");
        a.accept("bettacraft.configuration.lightTint.tooltip", "How strongly coloured aquarium lights tint the water of their tank: 0 = not at all (the colour only shows at the lamp), 1 = water in the light colour");
    }
}
