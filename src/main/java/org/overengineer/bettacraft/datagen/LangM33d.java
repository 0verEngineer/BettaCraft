package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/** M33 part d English texts (the foreign animal list, old age). The German ones are in de_de.json; edit both. */
final class LangM33d {
    private LangM33d() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("command.bettacraft.foreign.tank", "%s at %s: %s foreign animal(s)");
        a.accept("command.bettacraft.foreign.animal", "  %s (%s): profile %s");
        a.accept("command.bettacraft.foreign.no_profile", "  %s (%s): no profile");
        a.accept("command.bettacraft.foreign.more", "  ... and %s more");
        a.accept("command.bettacraft.foreign.none", "No tank within %s blocks has animals of vanilla or other mods");
        a.accept("command.bettacraft.foreign.none_here", "This tank has no animals of vanilla or other mods");
        a.accept("command.bettacraft.foreign.unprofiled", "Entity types of other mods living in these tanks without a profile: %s");
        a.accept("command.bettacraft.foreign.unprofiled.none", "Every animal of another mod in these tanks has a profile");
        a.accept("command.bettacraft.foreign.care_off", "features.foreignAnimals.care is off: the profiles rest");
    }
}
