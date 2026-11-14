package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/** M22.1 English texts (plant look for every species, swaying water plants, emptying containers). de_de.json holds the German ones. */
final class LangM221 {
    private LangM221() {}

    static void addAll(BiConsumer<String, String> a) {
        // config: swaying water plants in the miniature
        a.accept("bettacraft.configuration.swayingForeignPlants", "Swaying water plants (Minecraft, other mods)");
        a.accept("bettacraft.configuration.swayingForeignPlants.tooltip", "Kelp, seagrass, corals (#bettacraft:swaying_plants) and the water plants of other mods sway in the miniature outside the tank like inside (off: they stand still for everyone)");
        a.accept("bettacraft.configuration.swayingPlants", "Swaying water plants in miniatures");
        a.accept("bettacraft.configuration.swayingPlants.tooltip", "Kelp, seagrass, corals and the water plants of other mods sway in the miniature outside the tank (off: they stand still, only for you)");
        a.accept("bettacraft.configuration.swaying", "Swaying water plants");
        a.accept("bettacraft.configuration.autoDetect", "Find other water plants by themselves");
        a.accept("bettacraft.configuration.autoDetect.tooltip", "Besides #bettacraft:swaying_plants also every other underwater plant (plant material, waterlogged or a water plant, drawn as a cross or a plant model) sways (off: only the tag)");
        a.accept("bettacraft.configuration.exclude", "Never sway");
        a.accept("bettacraft.configuration.exclude.tooltip", "Blocks that never sway: block ids (\"modid:block\"), block tags (\"#modid:tag\") or whole mods (\"modid:*\")");
    }
}
