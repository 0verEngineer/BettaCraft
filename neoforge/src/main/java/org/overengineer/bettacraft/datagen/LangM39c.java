package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M39 part c English texts (the peaceful animals of Minecraft as terrarium pets, optional feature #25: their needs, no farm, bait).
 * The German ones are in de_de.json; edit both.
 */
final class LangM39c {
    private LangM39c() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("gui.bettacraft.terrarium.problem.no_flowers", "no flowers on the land");
        a.accept("gui.bettacraft.terrarium.problem.no_grass", "too little grass on the land");
        a.accept("gui.bettacraft.terrarium.problem.no_hive", "no beehive in the interior");
        a.accept("gui.bettacraft.foreign.blocked.no_flowers", "it needs flowers on the land");
        a.accept("gui.bettacraft.foreign.blocked.no_grass", "it needs grass on the land");
        a.accept("gui.bettacraft.foreign.blocked.no_hive", "it needs a beehive in the interior");
        a.accept("message.bettacraft.pet.no_farm", "%s gives nothing here: the pets of our tanks are no farm");
        a.accept("message.bettacraft.pet.bait", "%s laid as bait: an animal that eats it walks into the open tank (for a day)");
        a.accept("log.bettacraft.pet.bait", "%s laid as bait for the animals around");
        a.accept("log.bettacraft.pet.lured", "%s came in for the bait");
    }
}
