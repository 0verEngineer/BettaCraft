package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M32 part b English texts (tanks travelling on Create contraptions and Create Aeronautics airships). The German ones are
 * in de_de.json; edit both.
 */
final class LangM32b {
    private LangM32b() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("message.bettacraft.travel.blocked", "This tank cannot travel now: %s");
        a.accept("message.bettacraft.travel.blocked.interior", "someone is inside it");
        a.accept("message.bettacraft.travel.blocked.nest", "there are eggs in the bubble nest");
        a.accept("message.bettacraft.travel.blocked.eggs", "eggs or young would not survive the trip");
        a.accept("message.bettacraft.travel.travelling", "The tank is travelling: its animals are packed until it is put down");

    }
}
