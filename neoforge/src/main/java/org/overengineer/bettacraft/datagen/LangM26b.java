package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M26 part b English texts (the food maker, the new foods, food variety, the config).
 * Generated together with the German ones in de_de.json; edit both.
 */
final class LangM26b {
    private LangM26b() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("block.bettacraft.food_maker", "Food Maker");
        a.accept("item.bettacraft.fish_flakes", "Fish Flakes");
        a.accept("item.bettacraft.food_granules", "Food Granules");
        a.accept("item.bettacraft.shrimp_food", "Shrimp Food");
        a.accept("item.bettacraft.frozen_mosquito_larvae", "Frozen Mosquito Larvae");
        a.accept("item.bettacraft.frozen_artemia", "Frozen Artemia");
        a.accept("item.bettacraft.frozen_daphnia", "Frozen Daphnia");
        a.accept("subtitles.bettacraft.block.food_maker.wind", "Food maker spring winds");

        a.accept("message.bettacraft.food_maker.on_fe", "It runs on FE (a power option is on): no spring to wind");
        a.accept("message.bettacraft.food_maker.wound", "The spring is fully wound");
        a.accept("message.bettacraft.food_maker.spring", "Spring wound to %s%%");
        a.accept("gui.bettacraft.food_maker.auto", "Automatic");
        a.accept("gui.bettacraft.food_maker.spring", "Spring: %s%% (wind it with the crank)");
        a.accept("gui.bettacraft.food_maker.energy", "%s / %s FE");
        a.accept("gui.bettacraft.food_maker.status.idle", "Waiting");
        a.accept("gui.bettacraft.food_maker.status.working", "Working");
        a.accept("gui.bettacraft.food_maker.status.no_recipe", "The inputs do not hold the ingredients of a recipe");
        a.accept("gui.bettacraft.food_maker.status.output_full", "The output is full: take the food out");
        a.accept("gui.bettacraft.food_maker.status.wind", "The spring has run down: wind it with the crank");
        a.accept("gui.bettacraft.food_maker.status.no_power", "Needs FE");
        a.accept("gui.bettacraft.food_maker.status.paused", "Paused by a redstone signal");
        a.accept("gui.bettacraft.food_maker.status.off", "The food maker is switched off (features.foodMachine.enabled)");
        a.accept("log.bettacraft.critters_fed", "The small animals got %s");
        a.accept("gui.bettacraft.fish.diet", "Food kinds in the last days: %s");
        a.accept("gui.bettacraft.fish.diet_varied", "Varied diet: recovers faster from stress and sickness");
        a.accept("gui.bettacraft.fish.diet_need", "Varied from %s kinds (feeder slots take turns)");

        a.accept("info.bettacraft.food_maker", "Makes fish food in batches: pellets, flakes, granules, algae wafers, shrimp food, fry powder and frozen food (live food with ice) from wheat, kelp, fish, spider eyes, sugar, eggs and vegetables.\nChoose the product with < and > or leave it on Automatic. Without power it runs on a spring: right-click it with the crank to wind it (hold the button; a full spring lasts a day); with a power option on it takes FE, and its spring when the FE is short.\nHoppers and storage mods put ingredients in from every side and take the food out.");
        a.accept("info.bettacraft.fish_flakes", "Fish flakes: float at the surface. A favourite of the midwater fish (tetras, rasboras, livebearers, gouramis); bottom fish hardly get any.\nMade in the food maker.");
        a.accept("info.bettacraft.food_granules", "Food granules: sink slowly, so the bottom fish (corydoras, loaches) get their share even next to fast fish. Their favourite.\nMade in the food maker.");
        a.accept("info.bettacraft.shrimp_food", "Shrimp food: sinks to the shrimp, crayfish and snails (the fish leave it), twice the food of a leftover pellet, with calcium for the moult.\nMade in the food maker.");
        a.accept("info.bettacraft.frozen_mosquito_larvae", "Frozen mosquito larvae: eaten like live food (juveniles and older) and a favourite of the bettas, bottom fish and seahorses; it keeps and conditions for breeding.\nMade in the food maker from live food and ice; cannot be pumped.");
        a.accept("info.bettacraft.frozen_artemia", "Frozen artemia: eaten like live food by juveniles and older (not by fry); a favourite of the bettas and seahorses.\nMade in the food maker from artemia nauplii and ice.");
        a.accept("info.bettacraft.frozen_daphnia", "Frozen daphnia: eaten like live food; a favourite of the bettas and the midwater fish.\nMade in the food maker from daphnia and ice.");

        a.accept("bettacraft.configuration.foodMaking", "Food maker");
        a.accept("bettacraft.configuration.foodMaking.tooltip", "The food maker: a spring wound with the crank, or FE (then the spring when the FE is short) while a power option is on");
        a.accept("bettacraft.configuration.ticksPerBatch", "Ticks per batch");
        a.accept("bettacraft.configuration.ticksPerBatch.tooltip", "Ticks the food maker needs for one batch");
        a.accept("bettacraft.configuration.springTicks", "Spring ticks");
        a.accept("bettacraft.configuration.springTicks.tooltip", "Ticks of work a fully wound spring gives (24000 = one in-game day)");
        a.accept("bettacraft.configuration.windsToFull", "Winds to a full spring");
        a.accept("bettacraft.configuration.windsToFull.tooltip", "Clicks with the crank that wind the spring fully (holding the crank clicks about five times a second)");
        a.accept("bettacraft.configuration.makerFePerTick", "Food maker FE per tick");
        a.accept("bettacraft.configuration.makerFePerTick.tooltip", "FE per tick the food maker takes while it works and a power option is on (it takes FE first, then its spring)");
        a.accept("bettacraft.configuration.diet", "Food variety");
        a.accept("bettacraft.configuration.diet.tooltip", "Food groups, favourite and poor food of each diet (item tags #bettacraft:diet/<diet>/favourite and /poor)");
        a.accept("bettacraft.configuration.varietyDays", "Days a fish remembers its food");
        a.accept("bettacraft.configuration.varietyDays.tooltip", "In-game days a fish remembers what it ate");
        a.accept("bettacraft.configuration.varietyGroups", "Food kinds for a varied diet");
        a.accept("bettacraft.configuration.varietyGroups.tooltip", "Food groups (pellets, flakes, granules, wafers, frozen, live) a fish must have eaten in that time for a varied diet");
        a.accept("bettacraft.configuration.varietyRecovery", "Recovery with a varied diet");
        a.accept("bettacraft.configuration.varietyRecovery.tooltip", "How many times as fast a fish with a varied diet recovers from stress and sickness");
        a.accept("bettacraft.configuration.poorFoodFedDays", "Poor food: hungry sooner");
        a.accept("bettacraft.configuration.poorFoodFedDays.tooltip", "A meal of a food that suits the species badly counts as eaten this many days ago (it is hungry again sooner)");
        a.accept("bettacraft.configuration.shrimpFoodFactor", "Shrimp food strength");
        a.accept("bettacraft.configuration.shrimpFoodFactor.tooltip", "Shrimp food gives the shrimp, crayfish and snails this many times the food of a leftover pellet");
    }
}
