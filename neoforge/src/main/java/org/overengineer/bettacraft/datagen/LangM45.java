package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M45 English texts: the new advancements and the new quests of the FTB Quests chapter of {@code modpack/} (made by
 * {@code tools/modpack/ftbquests_chapter.py}). The German ones are in de_de.json; edit both.
 */
final class LangM45 {
    private LangM45() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("advancements.bettacraft.tankmate_inserted.title", "Community Tank");
        a.accept("advancements.bettacraft.tankmate_inserted.description", "Put a tankmate fish into a tank");
        a.accept("advancements.bettacraft.livebearer_litter.title", "Born Alive");
        a.accept("advancements.bettacraft.livebearer_litter.description", "Livebearers give birth in one of your tanks");
        a.accept("advancements.bettacraft.seahorse_fry.title", "Proud Father");
        a.accept("advancements.bettacraft.seahorse_fry.description", "A male seahorse releases fry from his pouch");
        a.accept("advancements.bettacraft.critters_bred.title", "Colony");
        a.accept("advancements.bettacraft.critters_bred.description", "Young shrimp, snails or crayfish are born in a tank");
        a.accept("advancements.bettacraft.algae_scraped.title", "Clear View");
        a.accept("advancements.bettacraft.algae_scraped.description", "Scrape algae off the glass of a tank with the algae scraper");
        a.accept("advancements.bettacraft.herp_clutch.title", "Hatchlings");
        a.accept("advancements.bettacraft.herp_clutch.description", "A clutch of a terrarium animal hatches");
        a.accept("advancements.bettacraft.turtle_clutch.title", "Warm or Cool");
        a.accept("advancements.bettacraft.turtle_clutch.description", "Turtle eggs hatch: the nest temperature decided their sex");
        a.accept("advancements.bettacraft.paludarium.title", "Half and Half");
        a.accept("advancements.bettacraft.paludarium.description", "Open the menu of a paludarium: water and land in one tank");
        a.accept("advancements.bettacraft.sea_turtle.title", "Ocean Giant");
        a.accept("advancements.bettacraft.sea_turtle.description", "Put a sea turtle into a big sea water paludarium");
        a.accept("advancements.bettacraft.pet_terrarium.title", "House Pet");
        a.accept("advancements.bettacraft.pet_terrarium.description", "A peaceful Minecraft animal moves into a terrarium or paludarium");
        a.accept("advancements.bettacraft.foreign_animal.title", "Guest from Afar");
        a.accept("advancements.bettacraft.foreign_animal.description", "Put an axolotl or an animal of another mod with a Bettacraft profile into a tank");
        a.accept("advancements.bettacraft.mob_in_glass.title", "Ship in a Bottle");
        a.accept("advancements.bettacraft.mob_in_glass.description", "Get a mob into a tank, shrunk and harmless");
        a.accept("advancements.bettacraft.big_animal.title", "Big Fish");
        a.accept("advancements.bettacraft.big_animal.description", "Put a big animal such as a shark, a ray or a giant catfish into a big tank");
        a.accept("advancements.bettacraft.shared_filter.title", "One Filter for All");
        a.accept("advancements.bettacraft.shared_filter.description", "A multiblock filter cleans the water of two tanks at once");
        a.accept("advancements.bettacraft.auto_water_change.title", "Hands Free");
        a.accept("advancements.bettacraft.auto_water_change.description", "A water change controller changes the water of a tank by itself");
        a.accept("advancements.bettacraft.varied_diet.title", "Balanced Diet");
        a.accept("advancements.bettacraft.varied_diet.description", "Open the menu of a tank with a fish on a varied diet");
        a.accept("advancements.bettacraft.regulator.title", "Turn It Down");
        a.accept("advancements.bettacraft.regulator.description", "Turn a filter, an air pump or a flow pump below 100 % with its power regulator");
        a.accept("advancements.bettacraft.light_timer.title", "Sunrise, Sunset");
        a.accept("advancements.bettacraft.light_timer.description", "Switch on the daylight timer of a tank's lights");
        a.accept("advancements.bettacraft.powered.title", "Plugged In");
        a.accept("advancements.bettacraft.powered.description", "Open the menu of a tank with energy in its buffer while devices need power");
        a.accept("advancements.bettacraft.wound_up.title", "Wound Up");
        a.accept("advancements.bettacraft.wound_up.description", "Wind a device with the crank");
        a.accept("advancements.bettacraft.create_drive.title", "Shaft Power");
        a.accept("advancements.bettacraft.create_drive.description", "A turning Create shaft drives a tank through a drive adapter or its cabinet");
        a.accept("advancements.bettacraft.travelling_tank.title", "Mobile Aquarium");
        a.accept("advancements.bettacraft.travelling_tank.description", "A tank arrives in one piece somewhere else on a contraption");

        // the quests (modpack/config/ftbquests/quests/chapters/bettacraft.snbt)
        q(a, "varied_diet", "Balanced Diet", "Feed different kinds of food: pellets, flakes, granules, frozen and live food. A varied diet speeds up the recovery and shows in the colours of the bettas; the tank menu shows the food groups a fish ate lately. Open the menu of such a tank.");
        q(a, "mirror", "Mirror, Mirror", "Hold a mirror stick to the glass: a male betta flares at his reflection. A little flaring keeps him fit.");
        q(a, "recovered", "Back on Fin", "Nurse a sick or injured betta back to health: clean, warm water and good food.");
        q(a, "magnifier", "Under the Lens", "The magnifier shows the genes of a fish: the colours and fin forms it carries and can pass on.");
        q(a, "fight", "A Lesson Learned", "Two adult male bettas in one tank fight. Optional: better learn this one from the handbook.");
        q(a, "fish_bag", "Travel Bag", "Fish, shrimp and frogs travel in a bucket or a fish bag. Right-click a tank with it to put the animal in, the tank menu takes them out again.");
        q(a, "cooler", "Cool Down", "Some fish want cooler water than the room: a cooler beside the tank brings the temperature down.");
        q(a, "cabinet", "Under the Tank", "A cabinet under the tank holds devices and gives it a stand. Cabinets in a row get a common front with doors and drawers, cabinets behind them make the stand of a big tank, and all cabinets under a tank share one spring; stone cabinets match your build.");
        q(a, "aeration", "Bubbles", "An air pump with air stones in the interior makes bubble columns and brings oxygen into the water.");
        q(a, "sponge_filter", "Gentle Flow", "Sponge and corner filters clean gently: safe for fry and shrimp.");
        q(a, "led_lights", "Strong Light", "The LED panel and the high-power LED light big tanks: red plants turn redder and the strong-light plants grow, but algae like it too.");
        q(a, "light_timer", "Sunrise, Sunset", "In the tank menu (Devices) the lights get a dimmer and a daylight timer with a sunrise and a sunset. Switch the timer on.");
        q(a, "regulator", "Turn It Down", "Filters, air pumps and flow pumps have a power regulator (menu, or sneak and mouse wheel): less power moves less water and is quieter.");
        q(a, "powered", "Plugged In", "Optional, only when the pack lets devices use energy: connect cables of another mod to a tank and open its menu while energy is stored.");
        q(a, "wound_up", "Wound Up", "Optional, only when the pack lets devices use energy: instead of energy, wind a device with the crank, for example the food maker, or charge the tank at its cabinet or filter.");
        q(a, "stainless_steel", "Stainless", "The crucible melts stainless steel (the machines of Mekanism, EnderIO, Create and Thermal make it too): for pipes that never rust in salt water.");
        q(a, "pipes", "Pipework", "Water pumps and glass pipes move water between tanks, reservoirs and devices.");
        q(a, "interior_pipes", "Through the Glass", "A bulkhead takes a pipe through the glass; in the interior, spray bars, nozzles and lily pipes return the water.");
        q(a, "auto_water_change", "Hands Free", "A water change controller with a pipe from fresh water and a drain at its waste water outlet changes the water on its own schedule.");
        q(a, "top_off", "Top It Off", "A level sensor at a tank measures the water level; the water change controller that fills the tank reads it and tops off evaporated water through its fresh water line, so the salt and the values stay steady.");
        q(a, "multiblock_filter", "Filter Room", "Build a multiblock filter: a pump head on filter chambers with an inlet and an outlet. Fill the chambers with sponge, rings, carbon or peat.");
        q(a, "filter_lines", "Filter Lines", "Pipes at the filter's inlet and outlet become its intake and return line and reach tanks further away. Every place where a line docks on a tank needs a guard against the fish.");
        q(a, "drain", "Down the Drain", "A drain at the end of a waste water line discards the old water of a water change."); // M68
        q(a, "pipe_wrench", "The Right Tool", "The pipe wrench closes a side of a pipe: two lines side by side stay apart."); // M68
        q(a, "shared_filter", "One Filter for All", "Connect two tanks to the same multiblock filter: the flow and the cleaning are shared by their water volumes.");
        q(a, "pipe_covers", "Out of Sight", "Pipe covers hide pipes and pipe bundles in walls and floors in the look of any block.");
        q(a, "decor", "Interior Design", "Place decoration in the interior: caves, roots, stones and wrecks give the fish cover and the tank character.");
        q(a, "floating_plants", "Floating Carpet", "Floating plants spread on the surface, take up waste and give shade. Bettas build their nests under them.");
        q(a, "algae", "Clear View", "Strong light and waste grow algae on the glass. Scrape them off with the algae scraper, or let snails and algae eaters do it.");
        q(a, "fertilizer", "Plant Food", "The fertilizer maker makes liquid fertilizer (wind it or power it): in the water the plants grow faster, but too much feeds the algae.");
        q(a, "fertilizer_dosing", "Automatic Gardener", "A fertilizer canister in the dosing pump doses the plants every day.");
        q(a, "swamp_water", "Swamp Soup", "Fill a bottle with water in a swamp: it is full of life, the start of every live food culture.");
        q(a, "mosquito_larvae", "Wriggling", "Mosquito larvae from swamp water are the favourite live food of bettas.");
        q(a, "food_bred", "Food Farm", "Breed live food: a larva tub or an artemia hatchery makes it every day.");
        q(a, "cultures", "Cultures", "Tubifex dishes and daphnia barrels breed more live food from mulm, yeast and green water.");
        q(a, "live_food_pump", "Live Delivery", "The live food pump moves live food from the cultures through pipes into the tanks.");
        q(a, "tankmates", "Community Tank", "Tetras, rasboras, corys and many more share a tank with peaceful bettas. Check the temperature and the water values of each species in the handbook.");
        q(a, "livebearer_litter", "Born Alive", "Guppies, endlers, platies, mollies and swordtails give birth to live young. A breeding box keeps them safe from hungry adults.");
        q(a, "guppy_strain", "Strain Breeder", "Breed a named guppy or endler strain such as Red Cobra, Blue Moscow or Endler Tiger.");
        q(a, "guppy_hybrid", "Crossbreed", "Cross an endler with a guppy and raise the hybrid.");
        q(a, "guppy_collection", "Strain Collection", "Breed every guppy and endler strain.");
        q(a, "community_fish", "Schooling Fish", "Danios, rainbowfish and the bigger tetras swim in schools: keep at least six.");
        q(a, "cichlids", "Dwarf Cichlids", "Rams and dwarf cichlids guard their eggs and fry. They want warm, soft water. The ram forms Gold, Electric Blue, Black and German Blue are recessive.");
        q(a, "puffers", "Puffers", "Pufferfish hunt snails and nip fins: keep them on their own.");
        q(a, "barbs_loaches", "Barbs and Loaches", "Lively barbs and bottom-dwelling loaches: busy fish for a bigger tank.");
        q(a, "gouramis", "Gouramis", "Honey, chocolate and pearl gouramis breathe air like bettas and build bubble nests too.");
        q(a, "critters", "Cleanup Crew", "Shrimp, snails and crayfish come in a critter bucket. They eat leftovers and algae.");
        q(a, "critters_bred", "Colony", "With plants, soft cover and stable water the small animals breed on their own.");
        q(a, "frogs", "Tiny Frogs", "Dwarf clawed frogs live fully in the water. Feed them by sneaking and right-clicking with food, before the fish snatch it.");
        q(a, "reef_fish", "Clownfish", "Reef fish such as clownfish, damsels and tangs live in a mature salt water tank.");
        q(a, "seahorse", "Seahorses", "Seahorses hold on to plants and corals, want low flow and live food.");
        q(a, "seahorse_fry", "Proud Father", "The female lays her eggs into the pouch of the male, and he gives birth.");
        q(a, "paludarium", "Half and Half", "Build land above the water line in a tank's interior: a paludarium has both. Open its menu.");
        q(a, "turtles", "Turtles", "Musk, Reeves' and slider turtles swim in a paludarium and bask on its land.");
        q(a, "turtle_clutch", "Warm or Cool", "A turtle pair lays eggs into sand or soil on the land. Warm nests give females, cool nests males.");
        q(a, "sea_turtle", "Ocean Giant", "Sea turtles need a huge sea water paludarium with sand on the land.");
        q(a, "pets", "House Pet", "Rabbits, chickens, frogs, cats and other peaceful animals live in terraria and paludaria. Lure one in with its food or bring it in a terrarium box.");
        q(a, "herp_clutch", "Hatchlings", "A gecko pair in a warm terrarium lays eggs; keep them warm until they hatch.");
        q(a, "big_gear", "Heavy Lifting", "Big animals do not fit into a bucket: catch them with the large net and carry them in the transport tub.");
        q(a, "big_animal", "Big Fish", "Sharks, rays and giant catfish need big tanks by their size. Put one in.");
        q(a, "foreign_animal", "Guest from Afar", "Axolotls, and animals of other mods with a profile, get Bettacraft's care and breed in tanks.");
        q(a, "mob_in_glass", "Ship in a Bottle", "Catch a drowned, a zombie, a spider or a slime with the mob jar: in a tank it lives shrunk and harmless.");
        q(a, "travelling_tank", "Mobile Aquarium", "Optional, with Create: put a tank on a contraption and move it. The animals travel along.");
        q(a, "create_drive", "Shaft Power", "Optional, with Create: a drive adapter on the tank, or its cabinet, takes a turning shaft and charges the tank's energy.");
    }

    private static void q(BiConsumer<String, String> a, String name, String title, String desc) {
        a.accept("quest.bettacraft." + name + ".title", title);
        a.accept("quest.bettacraft." + name + ".desc", desc);
    }
}
