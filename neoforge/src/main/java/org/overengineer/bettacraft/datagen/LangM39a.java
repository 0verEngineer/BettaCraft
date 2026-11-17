package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M39 part a English texts (the water animals of other mods: refusals with a reason, sexes of the foreign turtles). The German
 * ones are in de_de.json; edit both.
 */
final class LangM39a {
    private LangM39a() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("message.bettacraft.tank.refused.deep_sea", "%s is a deep-sea animal: it needs the cold, dark pressure of the deep sea and does not live in an aquarium");
        a.accept("message.bettacraft.tank.refused.too_big", "%s is far too big for any tank");
        a.accept("message.bettacraft.tank.refused.hostile", "%s is a hostile creature and does not go into a tank");
        a.accept("message.bettacraft.tank.refused.fungus", "%s is a mushroom creature, no water animal");
        a.accept("message.bettacraft.tank.refused.cave", "%s is bound to its cave world and does not live in an aquarium");
        a.accept("message.bettacraft.tank.refused.land", "%s is no animal for a tank");
        a.accept("message.bettacraft.tank.refused.unsuitable", "%s does not live in a Bettacraft tank");
        a.accept("gui.bettacraft.foreign.blocked.no_female", "there is no female");
        a.accept("gui.bettacraft.foreign.blocked.no_male", "there is no male");
        a.accept("log.bettacraft.foreign.hatched_sexes", "%2$s young %1$s hatched from the nest (%3$s female, %4$s male)");
    }
}
