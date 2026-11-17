package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M47 English texts: the aquarist villager and his breeding basin, the frog breeding (logs, config, advancement), the new
 * quests. The ram forms are in {@link LangM29}. The German ones are in de_de.json; edit both.
 */
final class LangM47 {
    private LangM47() {}

    static void addAll(BiConsumer<String, String> a) {
        // part a: the aquarist villager
        a.accept("block.bettacraft.breeding_basin", "Breeding Basin");
        a.accept("entity.minecraft.villager.bettacraft.aquarist", "Aquarist");
        a.accept("trade.bettacraft.aquarist.by_genes", "Price by the genes of the fish");
        a.accept("info.bettacraft.breeding_basin", "A small glass basin with gravel, water and plants on a wooden stand: the workplace of the aquarist villager.\n"
                + "Place it in a village: an unemployed villager nearby becomes an aquarist. He sells food, floating and aquarium plants, shrimp, snails, test kits, substrate and live food cultures, and as a master a pair of a rare bred variety of a tankmate.\n"
                + "He buys grown bettas (bucket or fish bag) and bred tankmates (fish bag): the more rare genes, the more emeralds (data/<ns>/bettacraft/aquarist_prices.json).\n"
                + "Decoration only: nothing lives in it. Config features.sources.aquaristVillager.");
        // part c: frog breeding
        a.accept("log.bettacraft.frog.amplexus", "%s clasps %s (amplexus)");
        a.accept("log.bettacraft.frog.spawned", "%s released %s eggs at the surface");
        a.accept("log.bettacraft.frog.spawn_eaten", "The frog spawn was eaten");
        a.accept("log.bettacraft.frog.tadpoles_hatched", "%s tadpoles hatched");
        a.accept("log.bettacraft.frog.tadpoles_starved", "%s tadpole(s) starved (they need fine food: infusoria, artemia nauplii)");
        a.accept("log.bettacraft.frog.tadpoles_eaten", "%s tadpole(s) were eaten");
        a.accept("log.bettacraft.frog.tadpoles_dried", "The tadpoles died without water");
        a.accept("log.bettacraft.frog.tadpoles_fed", "The tadpoles ate %s");
        a.accept("log.bettacraft.frog.froglets", "%s tadpole(s) became froglets");
        a.accept("bettacraft.configuration.frogBreedingSpeed", "Frog breeding speed");
        a.accept("bettacraft.configuration.frogBreedingSpeed.tooltip", "How readily a frog pair goes into amplexus after a cool water change at 24-28 °C (1 = normal, 0 = never)");
        a.accept("bettacraft.configuration.frogSpawnHatchDays", "Frog spawn: days to hatch");
        a.accept("bettacraft.configuration.frogSpawnHatchDays.tooltip", "Days the frog spawn floats at the surface until the tadpoles hatch");
        a.accept("bettacraft.configuration.tadpoleDays", "Tadpole days");
        a.accept("bettacraft.configuration.tadpoleDays.tooltip", "Days a tadpole needs (with fine food) to become a froglet");
        a.accept("bettacraft.configuration.frogletDays", "Froglet days");
        a.accept("bettacraft.configuration.frogletDays.tooltip", "Days a froglet needs to grow up (it breeds only then)");
        a.accept("bettacraft.configuration.maxTadpoles", "Most tadpoles per tank");
        a.accept("bettacraft.configuration.maxTadpoles.tooltip", "Most tadpoles in one tank (eggs beyond that do not hatch)");
        // advancements and quests
        a.accept("advancements.bettacraft.frog_tadpole.title", "Tadpoles!");
        a.accept("advancements.bettacraft.frog_tadpole.description", "The spawn of your dwarf clawed frogs hatches in a tank");
        a.accept("advancements.bettacraft.aquarist_trade.title", "An Aquarist in the Village");
        a.accept("advancements.bettacraft.aquarist_trade.description", "Trade with an aquarist villager");
        q(a, "aquarist", "An Aquarist in the Village",
                "Place a breeding basin in a village: an unemployed villager becomes an aquarist. He sells plants, food and small animals and buys your bettas: the rarer the genes, the more emeralds.");
        q(a, "frog_tadpole", "Tadpoles",
                "A cool water change at 24-28 °C sets a frog pair off. Floating plants hide the spawn; the tadpoles need infusoria and a tank without bettas.");
    }

    private static void q(BiConsumer<String, String> a, String name, String title, String desc) {
        a.accept("quest.bettacraft." + name + ".title", title);
        a.accept("quest.bettacraft." + name + ".desc", desc);
    }
}
