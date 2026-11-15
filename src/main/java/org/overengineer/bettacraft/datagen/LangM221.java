package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/** M22.1 English texts (plant look for every species, swaying water plants, emptying containers). de_de.json holds the German ones. */
final class LangM221 {
    private LangM221() {}

    static void addAll(BiConsumer<String, String> a) {
        // config: swaying water plants in the miniature
        a.accept("bettacraft.configuration.swayingPlants", "Swaying water plants in miniatures");
        a.accept("bettacraft.configuration.swayingPlants.tooltip", "Kelp, seagrass, corals and the water plants of other mods sway in the miniature outside the tank (off: they stand still, only for you)");
        a.accept("bettacraft.configuration.swaying", "Swaying water plants");
        a.accept("bettacraft.configuration.autoDetect", "Find other water plants by themselves");
        a.accept("bettacraft.configuration.autoDetect.tooltip", "Besides #bettacraft:swaying_plants also every other underwater plant (plant material, waterlogged or a water plant, drawn as a cross or a plant model) sways (off: only the tag)");
        a.accept("bettacraft.configuration.exclude", "Never sway");
        // containers (M22.1): the question before animals leave a bucket or a bag, pouring bottles out
        a.accept("gui.bettacraft.confirm_empty.title", "Let the animals go?");
        a.accept("gui.bettacraft.confirm_empty.text", "%s is emptied here: the animals in it go free into the world. To keep them, put them into a tank instead.");
        a.accept("gui.bettacraft.confirm_empty.cancel", "Cancel");
        a.accept("gui.bettacraft.confirm_empty.empty", "Empty");
        a.accept("tooltip.bettacraft.pour", "Sneak + right-click (or into the air): pour out");
        a.accept("bettacraft.configuration.exclude.tooltip", "Blocks that never sway: block ids (\"modid:block\"), block tags (\"#modid:tag\") or whole mods (\"modid:*\")");
    }
}
