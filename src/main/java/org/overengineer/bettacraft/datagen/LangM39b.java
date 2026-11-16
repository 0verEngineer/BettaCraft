package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M39 part b English texts (insects, mammals and birds of other mods in terraria: the lid of the fliers, the clean-up crew). The
 * German ones are in de_de.json; edit both.
 */
final class LangM39b {
    private LangM39b() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("message.bettacraft.terrarium.needs_lid", "%s would fly away: cover every top block of the tank first (a lid, a light or a feeder)");
        a.accept("log.bettacraft.foreign.flew_out", "%s flew out through the open top of the tank");
        a.accept("log.bettacraft.foreign.cleanup", "The clean-up crew (%s and others) ate %s leftover portions");
    }
}
