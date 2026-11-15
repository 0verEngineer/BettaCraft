package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M28.1 English texts (the own spawn pass of the wild animals, the command {@code /bettacraft wildlife}).
 * Generated together with the German ones in de_de.json; edit both.
 */
final class LangM281 {
    private LangM281() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("bettacraft.configuration.spawnPassInterval", "Spawn pass interval");
        a.accept("bettacraft.configuration.spawnPassInterval.tooltip", "Ticks between two tries of the own spawn pass per player (20 ticks = 1 second)");
        a.accept("bettacraft.configuration.spawnPassPerPlayer", "Wild animals per player at most");
        a.accept("bettacraft.configuration.spawnPassPerPlayer.tooltip", "The own spawn pass only spawns while fewer than this many wild animals of the mod live within 48 blocks of the player (0 = it spawns nothing)");
        a.accept("command.bettacraft.wildlife.interior", "This is the inside of a tank: no wild animals here.");
        a.accept("command.bettacraft.wildlife.biome", "Biome %s: water for spawns %s, for the net %s");
        a.accept("command.bettacraft.wildlife.none", "No animals of Bettacraft spawn in this biome.");
        a.accept("command.bettacraft.wildlife.species", "Spawn here: weight %s of %s in the water-ambient group (%s %%): %s");
        a.accept("command.bettacraft.wildlife.nearby", "Our wild animals within %s blocks: %s");
        a.accept("command.bettacraft.wildlife.cap", "Water-ambient mobs around you: %s of %s (vanilla cap per player); most from: %s");
        a.accept("command.bettacraft.wildlife.vanilla.room", "Vanilla spawning: possible now (the cap has room)");
        a.accept("command.bettacraft.wildlife.vanilla.full", "Vanilla spawning: no, the cap is full (despawning fish make room again)");
        a.accept("command.bettacraft.wildlife.vanilla.no_species", "Vanilla spawning: none of our animals in this biome");
        a.accept("command.bettacraft.wildlife.pass.off", "Own spawn pass: off (features.wildSpawnPass.enabled)");
        a.accept("command.bettacraft.wildlife.pass.full", "Own spawn pass: waiting, %s of %s wild animals already nearby");
        a.accept("command.bettacraft.wildlife.pass.on", "Own spawn pass: tries every %s s in water %s to %s blocks away, up to %s more animals");
        a.accept("command.bettacraft.wildlife.list.none", "No animals of Bettacraft within %s blocks (tank inhabitants left out).");
        a.accept("command.bettacraft.wildlife.list.header", "%s animals of Bettacraft within %s blocks (tank inhabitants left out):");
        a.accept("command.bettacraft.wildlife.list.entry", "%s at %s %s %s (%s blocks, %s)");
        a.accept("command.bettacraft.wildlife.list.wild", "wild");
        a.accept("command.bettacraft.wildlife.list.tame", "caught or released");
        a.accept("command.bettacraft.wildlife.list.more", "... and %s more");
        a.accept("command.bettacraft.wildlife.glow", "%s animals within %s blocks glow for 30 seconds.");
        a.accept("command.bettacraft.wildlife.water.river", "river");
        a.accept("command.bettacraft.wildlife.water.swamp", "swamp");
        a.accept("command.bettacraft.wildlife.water.mangrove", "mangrove swamp");
        a.accept("command.bettacraft.wildlife.water.beach", "beach");
        a.accept("command.bettacraft.wildlife.water.jungle", "jungle water");
        a.accept("command.bettacraft.wildlife.water.cave", "cave water");
        a.accept("command.bettacraft.wildlife.water.warm_ocean", "warm ocean");
        a.accept("command.bettacraft.wildlife.water.lukewarm_ocean", "lukewarm ocean");
        a.accept("command.bettacraft.wildlife.water.none", "none");
        // M28.1 part b: wild aquarium and floating plants
        a.accept("command.bettacraft.wildlife.plants", "Wild plants here (new chunks): %s, %s aquarium plants and %s floating plants%s");
        a.accept("command.bettacraft.wildlife.plants.none", "No wild plants of Bettacraft grow in this biome.");
        a.accept("command.bettacraft.wildlife.plants.jungle", "jungle water");
        a.accept("command.bettacraft.wildlife.plants.swamp", "swamp");
        a.accept("command.bettacraft.wildlife.plants.river", "river");
        a.accept("command.bettacraft.wildlife.plants.mangrove", "mangrove swamp");
        a.accept("command.bettacraft.wildlife.plants.cold", "cold water");
        a.accept("command.bettacraft.wildlife.plants.lush_caves", "lush cave pools");
        a.accept("block.bettacraft.wild_epiphyte", "Wild Epiphyte");
        a.accept("block.bettacraft.wild_moss", "Wild Moss");
        a.accept("block.bettacraft.wild_rosette", "Wild Rosette Plant");
        a.accept("block.bettacraft.wild_stem", "Wild Stem Plants");
        a.accept("block.bettacraft.wild_carpet", "Wild Carpet Plant");
        a.accept("block.bettacraft.wild_floating_plant", "Wild Floating Plants");
        a.accept("message.bettacraft.wild_plant.trimmed", "This plant was cut just now; it has to grow back first");
        a.accept("bettacraft.configuration.wildPlantFrequency", "Wild plant frequency");
        a.accept("bettacraft.configuration.wildPlantFrequency.tooltip", "A factor on the chance of a wild plant group per chunk (1 = about one small group in every two or three chunks with water of their kind; 0 = none)");
    }
}
