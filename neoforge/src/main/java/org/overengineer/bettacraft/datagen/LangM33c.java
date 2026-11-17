package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/** M33 part c English texts (presets, care mode, the preset command). The German ones are in de_de.json; edit both. */
final class LangM33c {
    private LangM33c() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("bettacraft.configuration.preset", "Preset");
        a.accept("bettacraft.configuration.preset.tooltip", "A ready-made profile for the whole mod: CUSTOM = none; SIMPLE = no water chemistry, CO2 or reverse osmosis, no forced water changes, no evaporation, no power, gentler care; REALISTIC = everything on, strict values, devices need FE; DECOR = no care (nothing gets hungry, sick or dies), no chemistry, power, algae or breeding. Values still at their default follow it, values you changed win. In a running game /reload brings the recipes up to date");
        a.accept("bettacraft.configuration.presetKeep", "Keep despite the preset");
        a.accept("bettacraft.configuration.presetKeep.tooltip", "Paths that keep their own value although the preset sets another one, also when it is the default (e.g. features.breeding.enabled, waterchange.difficulty)");
        a.accept("bettacraft.configuration.care", "Care");
        a.accept("bettacraft.configuration.careMode", "Care mode");
        a.accept("bettacraft.configuration.careMode.tooltip", "NORMAL = needs, sickness and death; NO_DEATH = needs and sickness stay, but no animal in a tank or terrarium dies of them or is eaten; DECOR = no needs at all, never hungry or sick, nothing dies. A player and /kill still kill");

        a.accept("preset.bettacraft.custom", "Custom");
        a.accept("preset.bettacraft.simple", "Simple");
        a.accept("preset.bettacraft.realistic", "Realistic");
        a.accept("preset.bettacraft.decor", "Decor only");
        a.accept("command.bettacraft.preset.active", "Preset: %s, kept from the files: %s");
        a.accept("command.bettacraft.preset.none", "nothing");
        a.accept("command.bettacraft.preset.overridden", "%s values of the preset are changed in the files (or kept) and stay as they are");
        a.accept("command.bettacraft.preset.set", "Preset %s is active (%s values set by it, saved in bettacraft-common.toml)");
        a.accept("command.bettacraft.preset.reload", "Run %s so the recipes and loot follow the feature switches");
        a.accept("command.bettacraft.preset.unknown", "Unknown preset: %s (custom, simple, realistic, decor)");
    }
}
