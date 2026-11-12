package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;
import org.overengineer.bettacraft.genetics.BettaColorTable;

/** English texts (en_us is generated from this; de_de.json is maintained by hand, LangCompletenessTest checks it). */
final class LangEn {
    private LangEn() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("itemGroup.bettacraft", "Bettacraft");
        a.accept("block.bettacraft.tank", "Tank");
        a.accept("block.bettacraft.tank.with_glass", "%s Tank");
        a.accept("block.bettacraft.interior_floor", "Interior Floor");
        a.accept("block.bettacraft.interior_wall", "Interior Wall");
        a.accept("block.bettacraft.feeder", "Aquarium Feeder");
        a.accept("block.bettacraft.tank_light", "Aquarium Light");
        a.accept("entity.bettacraft.betta", "Betta");
        a.accept("item.bettacraft.betta_bucket", "Bucket of Betta");
        a.accept("item.bettacraft.fish_bag", "Fish Bag");
        a.accept("item.bettacraft.fish_bag.filled", "Fish Bag (%s)");
        a.accept("item.bettacraft.betta_spawn_egg", "Betta Spawn Egg");
        a.accept("item.bettacraft.swamp_water_bottle", "Bottle of Swamp Water");
        a.accept("item.bettacraft.betta_pellets", "Betta Pellets");
        a.accept("item.bettacraft.mosquito_larvae", "Mosquito Larvae");
        a.accept("item.bettacraft.heater_small", "Small Aquarium Heater");
        a.accept("item.bettacraft.heater_medium", "Medium Aquarium Heater");
        a.accept("item.bettacraft.heater_large", "Large Aquarium Heater");

        a.accept("tooltip.bettacraft.tank", "Aquarium block. Place several of the same glass next to each other to build a larger tank.");
        a.accept("tooltip.bettacraft.heater.capacity", "Heats up to %s water blocks");
        a.accept("tooltip.bettacraft.heater.usage", "Put it into an equipment slot of the tank menu");
        a.accept("tooltip.bettacraft.fish_bag.empty", "Right-click a small fish to catch it. Holds the fish and 100 mB of water.");
        a.accept("tooltip.bettacraft.betta.sex_shape", "%s, %s");
        a.accept("tooltip.bettacraft.betta.colors", "Body %s, fins %s");
        a.accept("tooltip.bettacraft.betta.pattern", "Pattern: %s");
        a.accept("tooltip.bettacraft.betta.iridescence", "Iridescence: %s");

        // genetics
        for (BettaColorTable.Entry e : BettaColorTable.COLORS) a.accept("color.bettacraft." + e.id(), e.en());
        a.accept("shape.bettacraft.plakat", "Plakat");
        a.accept("shape.bettacraft.veil", "Veiltail");
        a.accept("shape.bettacraft.halfmoon", "Halfmoon");
        a.accept("shape.bettacraft.crowntail", "Crowntail");
        a.accept("shape.bettacraft.doubletail", "Doubletail");
        a.accept("pattern.bettacraft.none", "none");
        a.accept("pattern.bettacraft.butterfly", "Butterfly");
        a.accept("pattern.bettacraft.marble", "Marble");
        a.accept("pattern.bettacraft.dragon", "Dragon scale");
        a.accept("pattern.bettacraft.koi", "Koi");
        a.accept("pattern.bettacraft.mask", "Mask");
        a.accept("pattern.bettacraft.tipped", "Tipped");
        a.accept("pattern.bettacraft.fade", "Fade");
        a.accept("pattern.bettacraft.grizzle", "Grizzle");
        a.accept("pattern.bettacraft.bars", "Bars");
        a.accept("iridescence.bettacraft.none", "none");
        a.accept("iridescence.bettacraft.low", "low");
        a.accept("iridescence.bettacraft.high", "high");
        a.accept("stage.bettacraft.egg", "Egg");
        a.accept("stage.bettacraft.larva", "Larva");
        a.accept("stage.bettacraft.fry", "Fry");
        a.accept("stage.bettacraft.juvenile", "Juvenile");
        a.accept("stage.bettacraft.subadult", "Subadult");
        a.accept("stage.bettacraft.adult", "Adult");

        // messages
        a.accept("message.bettacraft.tank.not_cuboid", "This block does not complete a cuboid, so it forms a separate tank");
        a.accept("message.bettacraft.tank.unloaded", "The neighbouring tank is not completely loaded");
        a.accept("message.bettacraft.tank.other_glass", "Tanks only connect when they are made of the same glass");
        a.accept("message.bettacraft.tank.split", "The tank was split into %s tanks");
        a.accept("message.bettacraft.tank.full", "The tank is full");
        a.accept("message.bettacraft.tank.not_enough_water", "There is not enough water in the tank");
        a.accept("message.bettacraft.tank.not_formed", "This tank is not ready (its controller is not loaded)");
        a.accept("message.bettacraft.tank.info", "Tank %sx%sx%s | %s/%s mB | scale %s | %s");
        a.accept("message.bettacraft.tank.locked", "This tank is locked by its owner");
        a.accept("message.bettacraft.tank.only_owner", "Only the owner can lock or unlock this tank");
        a.accept("message.bettacraft.tank.too_little_water", "Fill the tank to at least %s%% before adding animals");
        a.accept("message.bettacraft.tank.keep_water", "The inhabitants need at least %s%% water in the tank");
        a.accept("message.bettacraft.tank.rejected", "%s cannot live in this tank");
        a.accept("message.bettacraft.tank.too_big", "%s is too big for an aquarium");
        a.accept("message.bettacraft.tank.max_entities", "This tank already holds the maximum of %s animals");
        a.accept("message.bettacraft.tank.crowded_full", "The tank is full: at most %s adult animals for this amount of water");
        a.accept("message.bettacraft.tank.not_inside", "That animal is no longer in this tank");
        a.accept("message.bettacraft.tank.cannot_take", "%s cannot be put into a bucket");
        a.accept("message.bettacraft.tank.need_bucket", "You need an empty bucket in your inventory");
        a.accept("message.bettacraft.tank.need_bag", "You need an empty fish bag in your inventory");
        a.accept("message.bettacraft.tank.no_bettas", "There are no bettas in this tank to feed");
        a.accept("message.bettacraft.tank.break_blocked", "%s animal(s) still live in this tank (%s). Take them out first.");
        a.accept("message.bettacraft.fish_bag.needs_water", "Release the fish into water");
        a.accept("message.bettacraft.betta.died", "%s died (%s)");
        a.accept("message.bettacraft.interior.nested", "You cannot enter a tank from inside a tank interior");
        a.accept("message.bettacraft.interior.missing_dimension", "The tank interior dimension is missing");
        a.accept("message.bettacraft.interior.entered", "Tank interior %sx%sx%s - hold sneak to leave");
        a.accept("message.bettacraft.interior.leaving", "Leaving in %s s...");
        a.accept("message.bettacraft.interior.invalid", "This tank interior no longer exists");
        a.accept("cause.bettacraft.hunger", "hunger");
        a.accept("cause.bettacraft.temperature", "wrong temperature");
        a.accept("cause.bettacraft.overcrowding", "overcrowding");
        a.accept("cause.bettacraft.illness", "illness");
        a.accept("cause.bettacraft.both", "hunger and wrong temperature");
        a.accept("cause.bettacraft.and", "%s and %s");

        // tank log
        a.accept("log.bettacraft.inserted", "%s was put in");
        a.accept("log.bettacraft.taken_out", "%s was taken out");
        a.accept("log.bettacraft.fed", "%s fish fed with %s");
        a.accept("log.bettacraft.feeder_empty", "The feeder is empty");
        a.accept("log.bettacraft.sick", "%s is sick");
        a.accept("log.bettacraft.recovered", "%s has recovered");
        a.accept("log.bettacraft.died", "%s died (%s)");

        // tank menu
        a.accept("gui.bettacraft.tank.title", "Aquarium");
        a.accept("gui.bettacraft.loading", "Loading...");
        a.accept("gui.bettacraft.tab.overview", "Overview");
        a.accept("gui.bettacraft.tab.inhabitants", "Animals");
        a.accept("gui.bettacraft.tab.log", "Log");
        a.accept("gui.bettacraft.tank.name", "Tank name");
        a.accept("gui.bettacraft.tank.name_hint", "Name (shown on the sign)");
        a.accept("gui.bettacraft.tank.locked", "Locked");
        a.accept("gui.bettacraft.tank.unlocked", "Unlocked");
        a.accept("gui.bettacraft.tank.lock_tooltip", "Owner: %s. When locked, only the owner and operators can change this tank.");
        a.accept("gui.bettacraft.tank.no_owner", "nobody");
        a.accept("gui.bettacraft.tank.sign_on", "Sign: on");
        a.accept("gui.bettacraft.tank.sign_off", "Sign: off");
        a.accept("gui.bettacraft.tank.front", "Front: %s");
        a.accept("gui.bettacraft.sign_pos.top_left", "Top left");
        a.accept("gui.bettacraft.sign_pos.top_center", "Top centre");
        a.accept("gui.bettacraft.sign_pos.top_right", "Top right");
        a.accept("gui.bettacraft.sign_pos.bottom_left", "Bottom left");
        a.accept("gui.bettacraft.sign_pos.bottom_center", "Bottom centre");
        a.accept("gui.bettacraft.sign_pos.bottom_right", "Bottom right");
        a.accept("gui.bettacraft.direction.north", "North");
        a.accept("gui.bettacraft.direction.south", "South");
        a.accept("gui.bettacraft.direction.east", "East");
        a.accept("gui.bettacraft.direction.west", "West");
        a.accept("gui.bettacraft.tank.size_water", "%sx%sx%s | Water %s / %s mB (%s%%)");
        a.accept("gui.bettacraft.tank.temperature", "Water %s °C (surroundings %s °C)");
        a.accept("gui.bettacraft.tank.target", "Target temperature:");
        a.accept("gui.bettacraft.tank.heaters", "Heaters: %s");
        a.accept("gui.bettacraft.tank.heaters_none", "none");
        a.accept("gui.bettacraft.tank.heaters_ok", "%s blocks for %s water blocks");
        a.accept("gui.bettacraft.tank.heaters_weak", "only %s blocks for %s water blocks");
        a.accept("gui.bettacraft.tank.feeder", "Feeder: %s");
        a.accept("gui.bettacraft.tank.feeder_state", "%s food, %sx per day");
        a.accept("gui.bettacraft.tank.light", "Light: %s");
        a.accept("gui.bettacraft.tank.light_state", "%s of %s on");
        a.accept("gui.bettacraft.tank.none", "none");
        a.accept("gui.bettacraft.tank.inhabitants", "Animals: %s (%s adults, max %s)");
        a.accept("gui.bettacraft.tank.crowded", "Overcrowded!");
        a.accept("gui.bettacraft.tank.fill_hint", "Fill to %s mB to add animals");
        a.accept("gui.bettacraft.tank.enter", "Interior");
        a.accept("gui.bettacraft.male", "♂ Male");
        a.accept("gui.bettacraft.female", "♀ Female");
        a.accept("gui.bettacraft.dumbo", "%s Dumbo");
        a.accept("gui.bettacraft.pattern_with_color", "%s (%s)");
        a.accept("gui.bettacraft.fish.empty", "No animals");
        a.accept("gui.bettacraft.fish.foreign", "No needs in this tank");
        a.accept("gui.bettacraft.fish.sex_stage", "%s, %s");
        a.accept("gui.bettacraft.fish.health", "Health");
        a.accept("gui.bettacraft.fish.satiation", "Fed");
        a.accept("gui.bettacraft.fish.healthy", "Healthy");
        a.accept("gui.bettacraft.fish.sick", "Sick");
        a.accept("gui.bettacraft.fish.name", "Name");
        a.accept("gui.bettacraft.fish.rename", "Name");
        a.accept("gui.bettacraft.fish.take_bucket", "Bucket");
        a.accept("gui.bettacraft.fish.take_bag", "Fish bag");
        a.accept("gui.bettacraft.fish.body", "Body: %s");
        a.accept("gui.bettacraft.fish.fins", "Fins: %s");
        a.accept("gui.bettacraft.fish.take_bucket_tooltip", "Take out with an empty bucket from your inventory (-%s mB)");
        a.accept("gui.bettacraft.fish.take_bag_tooltip", "Take out with an empty fish bag from your inventory (-%s mB)");
        a.accept("gui.bettacraft.fish.none_selected", "Select an animal");
        a.accept("gui.bettacraft.fish.not_bucketable", "This animal does not fit into a bucket");
        a.accept("gui.bettacraft.log.empty", "Nothing happened yet");
        a.accept("gui.bettacraft.log.line", "Day %s: %s");
        a.accept("gui.bettacraft.feeder.per_day", "%sx / day");
        a.accept("gui.bettacraft.feeder.tooltip", "Feedings per in-game day (morning, afternoon, evening). Place the feeder on top of a tank.");
    }
}
