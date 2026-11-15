package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M29.1 English texts (pump sound switches, the sound test, the air pump in the cabinet). Generated together with the
 * German ones in de_de.json; edit both.
 */
final class LangM291 {
    private LangM291() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("bettacraft.configuration.airPumpSound", "Air pump sound");
        a.accept("bettacraft.configuration.airPumpSound.tooltip", "Play the hum of the air pumps (off by default; its volume is airPumpVolume)");
        a.accept("bettacraft.configuration.flowPumpSound", "Flow pump sound");
        a.accept("bettacraft.configuration.flowPumpSound.tooltip", "Play the whir of the flow pumps (off by default; its volume is flowPumpVolume)");
        a.accept("command.bettacraft.sounds.start", "Sound test: %s device sounds, one every %s seconds (only you hear them)");
        a.accept("command.bettacraft.sounds.playing", "%s/%s: %s – %s");
        a.accept("command.bettacraft.sounds.done", "Sound test finished");
        a.accept("command.bettacraft.sounds.off_by_default", "(off by default)");
        a.accept("sound_test.bettacraft.filter", "filter outlet (external, cabinet, internal, corner filter)");
        a.accept("sound_test.bettacraft.sponge_filter", "sponge filter");
        a.accept("sound_test.bettacraft.air_stone", "air stone");
        a.accept("sound_test.bettacraft.air_pump", "air pump");
        a.accept("sound_test.bettacraft.flow_pump", "flow pump and water pump outlet");
        a.accept("sound_test.bettacraft.feeder", "automatic feeder");
        a.accept("sound_test.bettacraft.food_maker", "food maker");
        a.accept("sound_test.bettacraft.tank_fill", "water poured into a tank");
        a.accept("sound_test.bettacraft.tank_drain", "water scooped out of a tank");
        a.accept("sound_test.bettacraft.vanilla", "Minecraft twin (sounds.useVanilla)");
        a.accept("sound_test.bettacraft.own", "own sound");
    }
}
