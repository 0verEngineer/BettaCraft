package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M32 part a English texts (animals of other mods with our care and breeding: logs, messages, the config). The German
 * ones are in de_de.json; edit both.
 */
final class LangM32a {
    private LangM32a() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("log.bettacraft.foreign.pregnant", "%s is carrying young");
        a.accept("log.bettacraft.foreign.spawned", "%s has spawned");
        a.accept("log.bettacraft.foreign.young", "%s: %s young were born");
        a.accept("log.bettacraft.foreign.cannot_breed", "%s cannot breed here: %s");
        a.accept("gui.bettacraft.foreign.blocked.tank_size", "the tank is too small for its brood");
        a.accept("gui.bettacraft.foreign.blocked.plants", "its eggs need plants");
        a.accept("message.bettacraft.foreign.no_love", "%s does not breed by its own rules in a tank");

        a.accept("bettacraft.configuration.foreignCare", "Care for animals of other mods");
        a.accept("bettacraft.configuration.foreignCare.tooltip", "Care for animals of other mods: the fish and water animals of other mods that have a profile (data/<ns>/bettacraft/foreign_animals, defaults for Fintastic, Aquaculture, Critters and Companions and Naturalist) get our needs in our tanks: hunger, temperature, water quality, salinity, pH/TDS, stress, sickness and death (off: they are left to their mod as before; outside our tanks nothing changes either way)");
        a.accept("bettacraft.configuration.foreignBreeding", "Breeding of animals of other mods");
        a.accept("bettacraft.configuration.foreignBreeding.tooltip", "Breeding of animals of other mods in our tanks by their profile: livebearers give birth, egg layers spawn (some on plants, food fish only in big tanks); the young take the variants of their parents (off: they do not breed in our tanks)");
        a.accept("bettacraft.configuration.suppressForeignBreeding", "Suppress their own breeding");
        a.accept("bettacraft.configuration.suppressForeignBreeding.tooltip", "Suppress the own breeding of vanilla and mod animals in our tanks (love from food, breeding goals, babies of their mod), so only our breeding applies there (off: their mod may breed them in the tank as well)");
        a.accept("bettacraft.configuration.foreign", "Animals of other mods");
        a.accept("bettacraft.configuration.foreignBreedingSpeed", "Breeding speed");
        a.accept("bettacraft.configuration.foreignBreedingSpeed.tooltip", "Breeding speed of the animals of other mods with a profile (1 = by the profile, 0 = they never breed in a tank)");
        a.accept("bettacraft.configuration.foreignHealthSpeed", "Health timer speed");
        a.accept("bettacraft.configuration.foreignHealthSpeed.tooltip", "Speed of their health timers against the bettas' (hunger, sickness, death; 0.5 = they last twice as long)");
    }
}
