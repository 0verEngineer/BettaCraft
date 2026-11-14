package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M26 part a English texts (tubifex dish, daphnia barrel, live food pump, the live food items, the config).
 * Generated together with the German ones in de_de.json; edit both.
 */
final class LangM26 {
    private LangM26() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("block.bettacraft.tubifex_dish", "Tubifex Dish");
        a.accept("block.bettacraft.daphnia_barrel", "Daphnia Barrel");
        a.accept("block.bettacraft.live_food_pump", "Live Food Pump");
        a.accept("item.bettacraft.tubifex", "Tubifex Worms");
        a.accept("item.bettacraft.daphnia", "Daphnia");
        a.accept("item.bettacraft.mulm", "Mulm");
        a.accept("item.bettacraft.green_water_bottle", "Bottle of Green Water");
        a.accept("item.bettacraft.green_water_culture", "Green Water Culture");
        a.accept("fluid_type.bettacraft.live_food", "Live Food Suspension");
        a.accept("tooltip.bettacraft.live_food.poor", "From a foul dish: may make the fish sick");

        a.accept("message.bettacraft.live_culture.status", "Water %s/%s mB · culture %s%% · food %s%% · %s ready");
        a.accept("message.bettacraft.live_culture.foul", "· stale %s%%");
        a.accept("message.bettacraft.live_culture.blocked.off", "Live food cultures are switched off (features.liveFoodCultures)");
        a.accept("message.bettacraft.live_culture.blocked.mud", "Needs mud");
        a.accept("message.bettacraft.live_culture.blocked.water", "Needs more water");
        a.accept("message.bettacraft.live_culture.blocked.animals", "No animals: put some in to start");
        a.accept("message.bettacraft.live_culture.blocked.hot", "Too hot: put it in the shade");
        a.accept("message.bettacraft.live_culture.blocked.dark", "Too dark (needs light 9)");
        a.accept("message.bettacraft.live_culture.blocked.hungry", "Hungry: needs food");
        a.accept("message.bettacraft.live_culture.blocked.foul", "The water is stale: rinse with fresh water");
        a.accept("message.bettacraft.live_culture.blocked.crowded", "Crowded: harvest soon or it collapses");
        a.accept("message.bettacraft.live_culture.rinsed", "Fresh water rinses the dish");
        a.accept("message.bettacraft.live_culture.poor_harvest", "The water was stale: these worms are of poor quality");
        a.accept("message.bettacraft.live_food.daphnia_in_bottle", "A few daphnia came up with the water");
        a.accept("log.bettacraft.live_food_pumped", "Live food pump: %s portions of %s");
        a.accept("log.bettacraft.live_food_pumped_one", "Live food pump: %s portion of %s");

        a.accept("gui.bettacraft.live_food_pump.doses", "Doses per day");
        a.accept("gui.bettacraft.live_food_pump.portions", "Portions per dose");
        a.accept("gui.bettacraft.live_food_pump.doses_off", "off");
        a.accept("gui.bettacraft.live_food_pump.dose_now", "Dose now");
        a.accept("gui.bettacraft.live_food_pump.manual", "Only by the button");
        a.accept("gui.bettacraft.live_food_pump.reach", "Food next to it: %s · tanks reached: %s");
        a.accept("gui.bettacraft.live_food_pump.idle", "Waiting for the next dose");
        a.accept("gui.bettacraft.live_food_pump.pumped", "Pumped %s portions");
        a.accept("gui.bettacraft.live_food_pump.pumped_one", "Pumped %s portion");
        a.accept("gui.bettacraft.live_food_pump.no_source", "No breeder next to it");
        a.accept("gui.bettacraft.live_food_pump.no_food", "The breeder has no food ready");
        a.accept("gui.bettacraft.live_food_pump.no_target", "No tank reached (pipes or a tank next to it)");
        a.accept("gui.bettacraft.live_food_pump.nobody_eats", "Nobody in the tanks eats this food now");
        a.accept("gui.bettacraft.live_food_pump.no_power", "Needs FE (pipes.pumpsRequireEnergy)");
        a.accept("gui.bettacraft.live_food_pump.bottles_full", "Full of empty bottles: take them out");
        a.accept("gui.bettacraft.live_food_pump.off", "The live food pump is switched off (features.liveFoodPump)");
        a.accept("gui.bettacraft.live_food_pump.paused", "Paused by a redstone signal");
        a.accept("gui.bettacraft.live_food_pump.busy", "Pumped %s portions, the pipes are at their limit: the rest follows");
        a.accept("gui.bettacraft.live_food_pump.busy_one", "Pumped %s portion, the pipes are at their limit: the rest follows");

        a.accept("info.bettacraft.tubifex", "Tubifex worms: live food with a strong conditioning value; a breeding pair fed with them stays in the mood twice as long.\nMining mud in a swamp sometimes drops a few: put them into a tubifex dish to start a culture.\nWorms from a stale dish are of poor quality and may make the fish sick.");
        a.accept("info.bettacraft.daphnia", "Daphnia (water fleas): live food for bettas, tankmates, frogs and seahorses.\nA bottle of swamp water sometimes brings up a few: put them into a daphnia barrel to start a culture.");
        a.accept("info.bettacraft.mulm", "Mulm: the dirt washed out of filter sponges and ceramic rings (clean them in a water cauldron). Food for the tubifex dish.");
        a.accept("info.bettacraft.green_water_culture", "Swamp water with bone meal: in the light it turns into green water after a day (in the inventory or dropped).");
        a.accept("info.bettacraft.green_water_bottle", "Green water full of algae: food for the daphnia barrel (more than yeast). The empty bottle stays with you.");
        a.accept("info.bettacraft.tubifex_dish", "Breeds tubifex worms. Right-click with mud, a bucket of water and some tubifex to start; feed it mulm, or let a pipe bring in the waste water of a tank.\nThe water goes stale and evaporates: a bucket of water on a full dish (or clean water from a pipe) rinses it. Harvest with the aquarium net; hoppers take the worms out too.\nRight-click with an empty hand shows the state.");
        a.accept("info.bettacraft.daphnia_barrel", "Breeds daphnia. Fill it with at least 2 buckets of water, put daphnia in and feed yeast or green water. It needs light 9 or more and no hot sun (shade in the desert).\nHarvest with the aquarium net, a hopper or a live food pump before it gets crowded, or the culture collapses.");
        a.accept("info.bettacraft.live_food_pump", "Pumps live food from the breeder next to it (daphnia barrel, artemia hatchery, larva tub, tubifex dish) through the pipes into the tanks, on a schedule.\nRight-click: doses per day, portions per dose and \"Dose now\". A tank only takes the food when somebody there eats it. Redstone pauses it.");

        a.accept("bettacraft.configuration.liveFood", "Live food cultures");
        a.accept("bettacraft.configuration.liveFood.tooltip", "Tubifex dish, daphnia barrel and the live food pump");
        a.accept("bettacraft.configuration.tubifexDays", "Tubifex growth days");
        a.accept("bettacraft.configuration.tubifexDays.tooltip", "In-game days a fed tubifex dish needs to grow from a tenth to nine tenths of its room");
        a.accept("bettacraft.configuration.tubifexRoom", "Tubifex dish room");
        a.accept("bettacraft.configuration.tubifexRoom.tooltip", "Portions of tubifex a full dish holds");
        a.accept("bettacraft.configuration.daphniaDays", "Daphnia growth days");
        a.accept("bettacraft.configuration.daphniaDays.tooltip", "In-game days a fed and lit daphnia barrel needs to grow from a tenth to nine tenths of its room");
        a.accept("bettacraft.configuration.daphniaRoom", "Daphnia barrel room");
        a.accept("bettacraft.configuration.daphniaRoom.tooltip", "Portions of daphnia a full barrel holds");
        a.accept("bettacraft.configuration.crowdedDays", "Days until a crowded culture collapses");
        a.accept("bettacraft.configuration.crowdedDays.tooltip", "In-game days a daphnia barrel stays crowded (90 % of its room) before the culture collapses (harvest in time)");
        a.accept("bettacraft.configuration.tubifexConditioning", "Tubifex conditioning");
        a.accept("bettacraft.configuration.tubifexConditioning.tooltip", "How many times as long as other live food tubifex condition a breeding fish");
        a.accept("bettacraft.configuration.poorTubifexStress", "Stress of poor tubifex");
        a.accept("bettacraft.configuration.poorTubifexStress.tooltip", "Days of bad-water stress a fish gets from one meal of tubifex from a foul dish (0 = harmless)");
        a.accept("bettacraft.configuration.bottleDaphniaChance", "Daphnia in swamp water");
        a.accept("bettacraft.configuration.bottleDaphniaChance.tooltip", "Chance that a bottle of swamp water brings up a few daphnia as well");
        a.accept("bettacraft.configuration.pumpFePerPortion", "Live food pump FE per portion");
        a.accept("bettacraft.configuration.pumpFePerPortion.tooltip", "FE the live food pump needs per portion while pipes.pumpsRequireEnergy is on");
        a.accept("bettacraft.configuration.liveFoodCultures", "Live food cultures");
        a.accept("bettacraft.configuration.liveFoodCultures.tooltip", "Live food cultures: the tubifex dish (mud, mulm or waste water) and the daphnia barrel (yeast or green water, light), harvested with the net or by hoppers and pumps (off: the cultures stand still)");
        a.accept("bettacraft.configuration.liveFoodPump", "Live food pump");
        a.accept("bettacraft.configuration.liveFoodPump.tooltip", "Live food pump: pumps daphnia, artemia and other live food from a breeder through the pipes into tanks on a schedule (off: it does not pump and tanks take no live food from pipes)");
    }
}
