package org.overengineer.bettacraft.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * M33: every feature switch of Bettacraft, one tree in the common config section {@code features} (generated from the
 * table in the M33 tooling; edit here). A switch is on when its own value, its parent and every feature it
 * {@code requires} are on ({@link Features#enabled}). A group switch is called {@code <group>.enabled}; sub-switches sit
 * next to it. Sections without an own switch ({@code equipment}, {@code visuals}, {@code compat}) only group switches.
 * {@code old} are the keys of the same switch before M33 ({@code features.<old>}, read once by {@link FeatureMigration}).
 * <p>
 * Every new feature gets a constant here (en label and tooltip; the German ones in {@code de_de.json}).
 */
public enum Feature {
    BETTAS("bettas.enabled", null, List.of(), List.of(), "Bettas", "Bettas (Betta splendens and the wild species): the betta items and their recipes, wild bettas, bettas from the net and the trader; the betta care rests. Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    BETTAS_WILD("bettas.wildSpawns", BETTAS, List.of(), List.of(), "Wild bettas spawn", "Wild bettas spawn in swamps (spawn.weight, spawn.minGroupSize, spawn.maxGroupSize) (off: no wild bettas)"),
    WILD_BETTAS("bettas.wildSpecies", BETTAS, List.of(), List.of("wildBettas"), "Wild betta species", "Wild bettas: some wild bettas in swamps are Betta imbellis or Betta smaragdina instead of Betta splendens; they cross with each other and with Betta splendens into fertile hybrids (off: only Betta splendens in the wild; fish already caught stay)"),
    BREEDING("breeding.enabled", null, List.of(), List.of(), "Breeding", "Breeding of every animal in the tanks: bettas, tankmates, small animals, frogs, turtles, amphibians and reptiles and the animals of other mods; breeding box, spawning caves and mops, fry powder. Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    MUTATIONS("breeding.mutations", BREEDING, List.of(), List.of("mutations"), "Mutations of every animal", "Spontaneous mutations also for shrimp, crayfish, snails and the tankmates without their own genetics (off: only bettas, guppies and endlers mutate)"),
    WILD_COLORS("breeding.wildColors", BREEDING, List.of(), List.of("wildColors"), "Beautiful wild colours", "Now and then a wild animal shows a bred colour openly (wildlife.beautifulWildChance; off: wild animals are wild-coloured, bred colours only hidden)"),
    MAGNIFIER("breeding.magnifier", BREEDING, List.of(), List.of("magnifier"), "Magnifier", "The hidden genes of a new animal are unknown until it is analysed with the magnifier in a tank (off: every animal shows its genes like before)"),
    FRY_INTAKE("breeding.fryIntake", BREEDING, List.of(), List.of("fryIntake"), "Filters suck in fry", "Motor filters without an intake sponge suck in the fry of bettas and other fish now and then (internalFilters.fryIntakePerDay) (off: no fry is sucked in)"),
    INTERIOR("interior.enabled", null, List.of(), List.of(), "Tank interior", "The interior of the tanks (a pocket dimension at 16 blocks per tank block) and its miniature in the tank: going in, the equipment and plants placed in there. Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    PATHFINDING("interior.pathfinding", INTERIOR, List.of(), List.of("interiorPathfinding"), "Path finding in the interior", "Path finding in the tank interior: dwarf frogs walk around decoration, small animals go under overhangs and into caves, plecos swim into their spawning cave (off: straight lines like before)"),
    FLOATING_PLANTS("floatingPlants.enabled", null, List.of(), List.of(), "Floating plants", "Floating plants on the water surface of the tanks (duckweed, frogbit, salvinia, red root floater, water lettuce): from bottles, fishing, the trader and the wild. Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    FLOATING_WILD("floatingPlants.wildPlants", FLOATING_PLANTS, List.of(), List.of("wildPlants"), "Wild floating plants", "Every floating plant grows wild on the waters of its origin (new chunks); picked up by hand or with the net (spawn.wildPlantFrequency; off: no new ones in new chunks, plants already there stay)"),
    AQUATIC_PLANTS("aquaticPlants.enabled", null, List.of(), List.of(), "Aquarium plants", "Aquarium plants in the tank interior (stem plants, rosettes, carpets, mosses, epiphytes): from the net, fishing, bottles, shears, the trader and the wild; their growth in the interior. Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    AQUATIC_WILD("aquaticPlants.wildPlants", AQUATIC_PLANTS, List.of(), List.of("wildPlants"), "Wild aquarium plants", "Every aquarium plant grows wild in the waters of its origin (new chunks; jungle water, swamps, rivers, mangroves, cold lakes, lush caves); shears cut a cutting off them (spawn.wildPlantFrequency; off: no new ones in new chunks, plants already there stay)"),
    PLANT_NET_CATCHES("aquaticPlants.netCatches", AQUATIC_PLANTS, List.of(), List.of("plantNetCatches"), "Plants with the net", "The aquarium net now and then brings up an aquarium plant from the bottom of rivers, jungle water and swamps (interiorPlants.netPlantShare; off: plants only from fishing, bottles, shears and the trader)"),
    MARIMO_ROLLING("aquaticPlants.marimoRolling", AQUATIC_PLANTS, List.of(), List.of("marimoRolling"), "Rolling Marimo balls", "Marimo balls roll slowly over the tank floor with the current of filters and outlets (off: they stay where they were put)"),
    /** M38: plants and corals of Minecraft and other mods placed in the interior work like ours (datapack profiles {@code bettacraft/foreign_plants}). */
    FOREIGN_PLANTS("aquaticPlants.foreignPlants", AQUATIC_PLANTS, List.of(), List.of(), "Plants of Minecraft and other mods work", "Kelp, seagrass, lily pads, corals and the water plants of other mods (Fintastic, Regions Unexplored, Twilight Forest, Alex's Caves; data/<ns>/bettacraft/foreign_plants) placed in the tank interior lower the nitrate and the waste, count as plants for breeding, hiding, the small animals and the oxygen, floating leaves give shade, live rock of other mods is a small biological filter in salt water; like a full-grown plant of ours of the same growth form times interiorPlants.foreignPlantFactor, without growth and without needs (off: they are decoration only)"),
    ALGAE("algae.enabled", null, List.of(), List.of(), "Algae", "Algae on the glass and the decoration of the tanks (more with strong light and waste), the algae scraper, algae eaters cleaning them. Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    INVERTEBRATES("invertebrates.enabled", null, List.of(), List.of(), "Shrimp, snails and crayfish", "The small animals: shrimp, snails, crayfish and hermit crabs in the tanks, from the net, the shovel, the trader and the wild. Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    INVERTEBRATES_WILD("invertebrates.wildSpawns", INVERTEBRATES, List.of(), List.of("wildCritters"), "Wild small animals in the world", "Wild shrimp, crayfish, snails and hermit crabs crawl on the bottom of rivers, swamps, mangroves, beaches, jungle water and warm oceans; caught with the net, a fish bag or a bucket, and they can be let go again (off: they do not spawn, and let go into the world they are gone like before)"),
    CARIDINA("invertebrates.caridina", INVERTEBRATES, List.of(), List.of("caridina"), "Caridina bee shrimp", "Caridina bee shrimp (Crystal Red, Crystal Black, Taiwan bees, Tiger) for soft water, with their banding grades (off: no new ones from the net, the wild and the trader; those in tanks stay)"),
    SULAWESI("invertebrates.sulawesi", INVERTEBRATES, List.of(), List.of("sulawesi"), "Sulawesi animals", "Sulawesi animals: cardinal shrimp and Tylomelania snails for hard, warm water (off: no new ones from the net, the wild and the trader; those in tanks stay)"),
    BIG_CRAYFISH("invertebrates.bigCrayfish", INVERTEBRATES, List.of(), List.of("bigCrayfish"), "Big crayfish", "Big crayfish: the Florida crayfish (wild, blue, orange) and the marbled crayfish (only females, multiplies on its own) at the wandering trader, in the aquarium net and wild; they eat plants, dig them up and catch slow fish and small animals (off: no new ones; those in tanks stay)"),
    SNAIL_COLORS("invertebrates.snailColors", INVERTEBRATES, List.of(), List.of("snailColors"), "Snail colours", "Snail colours: every nerite gets its own random shade (red and yellow racer nerites), apple snails are mostly gold with shades and bands, ramshorn snails also come in red (off: one fixed colour per variety, brown apple snails, no new red ramshorns)"),
    ANIMAL_LIST("invertebrates.animalList", INVERTEBRATES, List.of(), List.of("animalList"), "List of single small animals", "List of every single shrimp, crayfish and snail in the tank menu (sex, age, colour density, genes) to take out exactly that one, and the sex choice for the quick take-out (off: only the kinds like before)"),
    FROGS("frogs.enabled", null, List.of(), List.of(), "Dwarf clawed frogs", "Dwarf clawed frogs (Hymenochirus): in tanks, from the net, the trader and the wild. Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    FROGS_WILD("frogs.wildSpawns", FROGS, List.of(), List.of("wildFish"), "Wild frogs spawn", "Wild dwarf clawed frogs spawn in swamps and jungle water, rarely (spawn.wildFishWeight) (off: only the net, the trader and breeding bring them)"),
    TANKMATES("tankmates.enabled", null, List.of(), List.of(), "Tankmate fish", "The tankmate fish (tetras, rasboras, corydoras, plecos, loaches, livebearers, gouramis and the later groups): in tanks, from the net, the trader and the wild. Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    TANKMATES_WILD("tankmates.wildSpawns", TANKMATES, List.of(), List.of("wildFish"), "Wild tankmates spawn", "Wild tankmate fish and seahorses spawn in their biomes, rarely and in small shoals (spawn.wildFishWeight) (off: only the net, the trader and breeding bring them)"),
    LIVEBEARER_GENETICS("tankmates.livebearerGenetics", TANKMATES, List.of(), List.of("livebearerGenetics"), "Guppy and endler genetics", "Guppy and endler genetics: every guppy and endler has its own genes (tail shape, body and tail colour, patterns on the X and Y chromosome) that are inherited at birth, named strains, endler x guppy hybrids (off: the old fixed varieties; the genes are kept)"),
    GREEN_NEON("tankmates.greenNeonTetra", TANKMATES, List.of(), List.of("greenNeonTetra"), "Green neon tetra", "Green neon tetra (Paracheirodon simulans, blue and green form) at the wandering trader and in the aquarium net (off: no new ones; fish already caught stay)"),
    CICHLIDS("tankmates.cichlids", TANKMATES, List.of(), List.of("cichlids"), "Cichlids", "Cichlids: ram, Bolivian ram, cockatoo and Agassiz's dwarf cichlid, angelfish and discus at the wandering trader, in the aquarium net and wild (open brooders and cave brooders that guard their brood; off: no new ones; fish already caught stay)"),
    PUFFERS("tankmates.puffers", TANKMATES, List.of(), List.of("puffers"), "Puffers", "Puffers: dwarf puffer, red-eye puffer and the brackish green spotted puffer at the wandering trader, in the aquarium net and wild (they eat snails, nip fins and need snails or hard food for their teeth; off: no new ones; fish already caught stay)"),
    BARBS_AND_LOACHES("tankmates.barbsAndLoaches", TANKMATES, List.of(), List.of("barbsAndLoaches"), "Barbs and loaches", "Barbs and loaches: tiger, odessa and cherry barb, dwarf chain loach, reticulated loach and garra at the wandering trader, in the aquarium net and wild (off: no new ones; fish already caught stay)"),
    COMMUNITY_FISH("tankmates.communityFish", TANKMATES, List.of(), List.of("communityFish"), "More community fish", "More community fish: zebra danio, celestial pearl danio, forktail and neon dwarf rainbowfish, emperor, rummy-nose and black phantom tetra, black molly and swordtail at the wandering trader, in the aquarium net and wild (off: no new ones; fish already caught stay)"),
    GOURAMIS("tankmates.gouramis", TANKMATES, List.of(), List.of("gouramis"), "Gouramis", "Gouramis: honey, chocolate and pearl gourami at the wandering trader, in the aquarium net and wild (labyrinth fish; foam nests, the chocolate gourami broods in her mouth; off: no new ones; fish already caught stay)"),
    SALTWATER("saltwater.enabled", null, List.of(), List.of(), "Salt water", "Salt water tanks: sea salt, sea water, salinity, protein skimmer, live and reef rock, reef fish and reef animals. Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    REEF_FISH("saltwater.reefFish", SALTWATER, List.of(TANKMATES), List.of(), "Reef fish", "Reef fish (clownfish, tangs, gobies, damsels) at the trader, in the net and wild in warm oceans (needs tankmates.enabled) (off: no new ones; fish already caught stay)"),
    SEAHORSES("saltwater.seahorses", SALTWATER, List.of(TANKMATES), List.of("seahorses"), "Seahorses", "Seahorses (lined seahorse Hippocampus erectus, dwarf seahorse H. zosterae) at the wandering trader and in the aquarium net in warm and lukewarm oceans (off: no new ones; seahorses already caught stay)"),
    CORALS("corals.enabled", null, List.of(SALTWATER), List.of(), "Corals", "Corals and the bubble tip anemone in salt water tanks: frags from the trader, the net and vanilla corals, wild corals on the reefs (needs saltwater.enabled). Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    WILD_CORALS("corals.wildCorals", CORALS, List.of(), List.of("wildCorals"), "Wild corals on the reefs", "Every Bettacraft coral and the bubble tip anemone grow on the warm ocean coral reefs (new chunks); shears cut a frag off them (off: no new ones in new chunks)"),
    REEF_DOSING("corals.reefDosing", CORALS, List.of(), List.of("reefDosing"), "Reef dosing", "Reef dosing: alkalinity (KH), magnesium and trace elements in salt water, used up by the stony corals, and the dosing pump with up to four canisters (off: the values stand still and the pump does not dose)"),
    FOREIGN_ANIMALS("foreignAnimals.enabled", null, List.of(), List.of(), "Vanilla and mod animals", "Vanilla animals (axolotls) and the animals of other mods in our tanks: our care and breeding for them by their profiles. Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    FOREIGN_CARE("foreignAnimals.care", FOREIGN_ANIMALS, List.of(), List.of("foreignCare"), "Care for animals of other mods", "Care for animals of other mods: the fish and water animals of other mods that have a profile (data/<ns>/bettacraft/foreign_animals, defaults for Fintastic, Aquaculture, Critters and Companions and Naturalist) get our needs in our tanks: hunger, temperature, water quality, salinity, pH/TDS, stress, sickness and death (off: they are left to their mod as before; outside our tanks nothing changes either way)"),
    FOREIGN_BREEDING("foreignAnimals.breeding", FOREIGN_ANIMALS, List.of(BREEDING), List.of("foreignBreeding"), "Breeding of animals of other mods", "Breeding of animals of other mods in our tanks by their profile: livebearers give birth, egg layers spawn (some on plants, food fish only in big tanks); the young take the variants of their parents (off: they do not breed in our tanks)"),
    SUPPRESS_FOREIGN_BREEDING("foreignAnimals.suppressOwnBreeding", FOREIGN_ANIMALS, List.of(), List.of("suppressForeignBreeding"), "Suppress their own breeding", "Suppress the own breeding of vanilla and mod animals in our tanks (love from food, breeding goals, babies of their mod), so only our breeding applies there (off: their mod may breed them in the tank as well)"),
    AXOLOTL_CARE("foreignAnimals.axolotlCare", FOREIGN_ANIMALS, List.of(), List.of("axolotlCare"), "Axolotl care", "Axolotl care: vanilla axolotls in a tank want cool water (15-20 °C, a cooler), suffer in warm water and breed there: eggs on the plants, larvae as baby axolotls, the five colours inherited by Mendel (blue recessive and rare; off: axolotls in tanks are left to vanilla)"),
    FOREIGN_TERRARIUM("foreignAnimals.terrarium", FOREIGN_ANIMALS, List.of(FOREIGN_CARE), List.of(), "Land and marsh animals of other mods", "Land and marsh animals of other mods with a terrarium profile (defaults: the Naturalist tortoise and alligator) in terraria and paludaria (they also need features.terrariums.enabled): by the size rule of the big animals over the land, the water or the whole tank, moved by our walker, fed land food by hand and from the feeder, eggs into sand or soil; the terrarium box catches them and the animals of terrarium.boxAllow (off: none go into tanks and the box takes none; those in tanks stay)"),
    VANILLA_PETS("foreignAnimals.vanillaPets", FOREIGN_ANIMALS, List.of(FOREIGN_TERRARIUM), List.of(), "Minecraft animals as terrarium pets", "The peaceful animals of Minecraft live in terraria and paludaria by their profiles (minecraft_*.json): chickens, rabbits, bees, frogs, cats, ocelots, foxes, parrots, armadillos, bats and wolves, and pigs, sheep, goats, cows, mooshrooms, horses, donkeys, mules, llamas, pandas and polar bears as big animals by the size rule; moved by our walker, fed by hand and from the feeder, with needs of their own (bees: flowers and a beehive in the interior; rabbits and the grazers: grass; frogs: a paludarium), chicks from eggs in the sand and young of their own; caught with the terrarium box, the large net and the tub or with bait; they drop nothing and give no wool, milk or eggs (off: none go in and the box takes none, the refusals of camels, sniffers, striders and the undead horses are off too; those in tanks stay)"),
    POWER("power.enabled", null, List.of(), List.of(), "Power (FE)", "Devices use Forge Energy (FE) from cables and batteries of other mods (off: the devices run without power). Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    POWER_REGULATOR("power.regulator", POWER, List.of(), List.of("powerRegulator"), "Power regulator", "Power regulator 0-100 % on filters, air stones, air pumps and flow pumps (menu or sneak + mouse wheel): less power cleans and moves less, needs less FE and is quieter (off: everything runs at 100 %)"),
    WINDING("power.winding", POWER, List.of(), List.of(), "Winding with the crank", "With a device's power option on, the external filter (with the heaters in it), the multiblock filter, the reverse osmosis unit, the water change controller, the water pump and the cabinet under a tank (for every device of its tank) can be wound with the crank instead of FE; the food maker and the fertilizer maker take FE or their spring. FE goes first (off: with the power options on these devices only run on FE)"),
    BREAK_PROTECTION("equipment.breakProtection", null, List.of(), List.of("breakProtection"), "Break protection", "Break protection switch in the tank menu: nobody can break a protected tank, its lid, light, feeders and the cabinet below (off: the switch is hidden and does nothing)"),
    CLIP_FEEDER("equipment.clipOnFeeder", null, List.of(), List.of("clipOnFeeder"), "Clip-on feeder", "Clip-on feeder: a small automatic feeder that hangs on the side of the glass (for tanks whose top is taken by a light) (off: it cannot be placed and placed ones do not feed)"),
    STONE_CABINETS("equipment.stoneCabinets", null, List.of(), List.of("stoneCabinets"), "Stone cabinets", "Stone cabinets: the aquarium cabinet can also be crafted from stone (also of other mods) with the slabs of the same stone; it looks like that stone and is mined with a pickaxe (off: no new ones can be crafted; placed ones stay)"),
    CABINET_OUTLET_SIDE("equipment.cabinetOutletSide", null, List.of(), List.of("cabinetOutletSide"), "Outlet side of the cabinet filter", "Setting in the cabinet menu: on which tank wall the hose of the external filter in the cabinet goes up (back, left, right, front); jet, bubbles and the current follow it, in front of the outlet the water moves faster (seahorses, corals) (off: always the back wall, the current evenly like before)"),
    BIG_CONTAINERS("equipment.bigContainers", null, List.of(), List.of("bigContainers"), "Several animals per bucket and bag", "Several animals of one species per bucket and fish bag by their size (a bucket: 32 shrimp, 24 rasboras, 1 betta; a bag half of that), small animals also in a plain bucket (off: one fish per bucket, bags as before)"),
    AERATION("equipment.aeration", null, List.of(), List.of("aeration"), "Aeration", "Air pump with air stones in the tank interior: bubble columns, more oxygen (off: they cannot be placed and placed ones do nothing)"),
    MORE_INTERNAL_FILTERS("equipment.moreInternalFilters", null, List.of(), List.of("moreInternalFilters"), "More internal filters", "The air-driven sponge filter (gentle, needs an air pump), the corner filter and the intake sponge for motor filters (off: they cannot be placed or fitted and placed ones do not clean)"),
    MULTIBLOCK_FILTER("equipment.multiblockFilter", null, List.of(), List.of("multiblockFilter"), "Multiblock external filter", "Multiblock external filter from filter chambers, a pump head and connectors, with filter media that clog and must be cleaned (off: it does not clean)"),
    /** M42: hoppers, pipes and ME / RS buses change the media of the multiblock filter (the chambers' item capability). */
    FILTER_MEDIA_AUTOMATION("equipment.filterMediaAutomation", null, List.of(MULTIBLOCK_FILTER), List.of(), "Filter media by automation", "Hoppers, pipes and the buses of ME and RS put a fresh filter medium into an empty chamber of the multiblock filter and take a used-up medium (fully clogged or spent) out; a medium that still works stays in (off: media only by hand)"),
    LED_TIERS("equipment.ledTiers", null, List.of(), List.of("ledTiers"), "LED tiers", "Stronger aquarium lights: the LED panel (2 blocks wide) and the high-power LED (any size); stronger light makes red plants redder, lets the strong-light plants grow and brings more algae (off: the new lights count like the light bar)"),
    LIGHT_TIMER("equipment.lightTimer", null, List.of(), List.of("lightTimer"), "Dimmer and daylight timer", "Dimmer and daylight timer with sunrise and sunset for the lights of a tank (tank menu, tab Devices) (off: lights are on at full strength like before)"),
    STAINLESS_STEEL("equipment.stainlessSteel", null, List.of(), List.of("stainlessSteel"), "Stainless steel", "Crucible and stainless steel (also from the machines of Mekanism, EnderIO, Create and Thermal) (off: the crucible does not melt)"),
    PIPES("pipes.enabled", null, List.of(), List.of("pipes"), "Pipes", "Glass and stainless steel pipes and the water pump: they carry water with its values between tanks, filters and the tanks and pipes of other mods (off: pipes carry nothing and pumps stand still) Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    INTERIOR_PIPES("pipes.interiorPipes", PIPES, List.of(INTERIOR), List.of("interiorPipes"), "Interior pipes", "Bulkhead in the tank floor and pipes, intake strainer, lily pipe, nozzle and spray bar in the tank interior; their current acts where they sit (off: they cannot be placed and the bulkheads stay closed)"),
    AUTO_WATER_CHANGE("pipes.autoWaterChange", PIPES, List.of(), List.of("autoWaterChange"), "Automatic water change", "Water change controller: changes a part of the water by itself at an interval (off: it does nothing)"),
    TOP_OFF("pipes.topOff", PIPES, List.of(), List.of("topOff"), "Top-off", "Top-off: a level sensor tops off evaporated water from a reservoir or a pipe (off: it does nothing)"),
    FILTER_LINES("pipes.filterLines", PIPES, List.of(MULTIBLOCK_FILTER), List.of(), "Filter lines and guards", "Inlet and outlet pipes of the multiblock filter with T-pieces: one filter serves several tanks (its cleaning and flow are shared by their water volumes); the pipes dock on any side, the top or the bottom of a tank, with their connection in the interior; every connection needs an intake or outlet guard there, also on filters built before (off: a filter serves one tank as before, without guards, and the filter pipes carry nothing)"),
    /** M40 part b (F1): fresh and waste water at the multiblock filter; the water devices on the filter act on all its tanks. */
    FILTER_WATER("pipes.filterWater", PIPES, List.of(FILTER_LINES), List.of(), "Fresh and waste water at the filter", "Fresh water and waste water lines and ports of the multiblock filter: a water pump, a reservoir, an ME/RS export bus or the pipes of other mods fill its fresh water port; a level sensor and a water change controller on the filter top off and change the water of all its tanks (shared by what each tank needs), the old water goes out of its waste water port or is discarded without one (off: the water ports and lines do nothing, the devices on the filter work as before)"),
    /** M40 part c (C1): covers (facades) on the filter pipes and bundles: our pipe cover tool, other mods' facades, Cable Facades. */
    PIPE_COVERS("pipes.pipeCovers", PIPES, List.of(FILTER_LINES), List.of(), "Pipe covers", "Pipe covers on the filter pipes and pipe bundles: with the pipe cover in the main hand and a full block in the off hand, right-click a filter pipe and it is drawn as that block (sneak-right-click with the pipe cover takes it off again, the block comes back); the pipe keeps its lines and connections. The facades of Applied Energistics 2, EnderIO and Mekanism Covers go on them too (off: no new covers go on, covers already on stay and can be taken off)"),
    WATER_CHEMISTRY("waterChemistry.enabled", null, List.of(), List.of("waterChemistry"), "Water chemistry", "Water chemistry: TDS, pH and hardness from the substrate, peat, almond leaves, wood, food and water changes; every animal has its range (off: the values stand still and no animal minds them) Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    OXYGEN("waterChemistry.oxygen", WATER_CHEMISTRY, List.of(), List.of("oxygen"), "Oxygen", "Oxygen as a water value: it drops with warm water, many animals and at night in planted tanks without light, and rises with air stones, sponge filters, filter outlets and flow pumps; too little stresses the animals (bettas and gouramis breathe air and hardly mind) (off: no oxygen value)"),
    CO2("co2.enabled", null, List.of(), List.of("co2"), "CO2 dosing", "CO2 dosing: bio CO2, gas cylinders with pressure regulator and solenoid valve, the CO2 filling station; CO2 makes the plants grow faster, lowers the pH and too much harms the animals (off: the devices give no CO2) Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    OSMOSIS("osmosis.enabled", null, List.of(), List.of("reverseOsmosis"), "Reverse osmosis", "Reverse osmosis unit: makes reverse osmosis water (nothing dissolved) from water, with a membrane and a prefilter that wear out (off: it does not run) Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    LIVE_FOOD("liveFood.enabled", null, List.of(), List.of(), "Live food", "Live food: mosquito larvae, infusoria, artemia, tubifex, daphnia, crickets and fruit flies, their breeders and cultures. Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    LIVE_FOOD_CULTURES("liveFood.cultures", LIVE_FOOD, List.of(), List.of("liveFoodCultures"), "Live food cultures", "Live food cultures: the tubifex dish (mud, mulm or waste water) and the daphnia barrel (yeast or green water, light), harvested with the net or by hoppers and pumps (off: the cultures stand still)"),
    LIVE_FOOD_PUMP("liveFood.pump", LIVE_FOOD, List.of(), List.of("liveFoodPump"), "Live food pump", "Live food pump: pumps daphnia, artemia and other live food from a breeder through the pipes into tanks on a schedule (off: it does not pump and tanks take no live food from pipes)"),
    FOOD_MACHINE("foodMachine.enabled", null, List.of(), List.of("foodMaker"), "Food maker", "Food maker: makes pellets, flakes, granules, wafers, fry powder, shrimp food and frozen food from vanilla ingredients and live food, with a wound spring or FE (off: it stands still) Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    FOOD_VARIETY("foodVariety.enabled", null, List.of(), List.of("foodVariety"), "Food variety", "Food variety: favourite and poor food per species, a varied diet speeds up the recovery and shows in the colours of the bettas (off: every food is the same) Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    FERTILIZER("fertilizer.enabled", null, List.of(), List.of(), "Liquid fertilizer", "Liquid fertilizer and the fertilizer maker: fertilizer in the water speeds up the growth of Bettacraft's aquarium and floating plants (not the plants of other mods), the plants use it up, water changes dilute it, too much makes the algae grow faster (off: the fertilizer does nothing and the maker stands still) Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    FERTILIZER_DOSING("fertilizer.dosing", FERTILIZER, List.of(REEF_DOSING), List.of(), "Fertilizer in the dosing pump", "The dosing pump also doses liquid fertilizer from a fertilizer canister, into fresh and salt water tanks (off: fertilizer canisters do nothing)"),
    MONITORS("monitors.enabled", null, List.of(), List.of("monitors"), "Monitors", "Monitors: screens of monitor blocks that show the values of every tank in range, with graphs of the last days (off: the screens stay dark and record nothing) Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    BIG_TANKS("bigTanks.enabled", null, List.of(), List.of("bigTanks"), "Big tanks", "Big tanks: up to bigTankRules.bigMaxSize* blocks (32 x 16 x 32), with a smaller interior scale (8 or 4) from bigTankRules.interiorScale8Above (off: tank.maxSize* as before; big tanks already built stay) Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    BIG_ANIMALS("bigTanks.bigAnimals", BIG_TANKS, List.of(), List.of("bigAnimals"), "Big animals", "Big animals: giant catfish, sharks, rays and the vanilla dolphin in big tanks by the size rule (bigAnimalRules.*), the large net, the transport tub and the steel leader, wild big animals (off: none go into tanks, no new ones from the wild; those in tanks stay)"),
    TERRARIUMS("terrariums.enabled", null, List.of(), List.of("terrarium"), "Terrarium and paludarium", "Terrarium and paludarium: a tank without water is a terrarium, one with a land part built in the interior above the water a paludarium (terrarium.*); they get air temperature, humidity and UV, terrarium animals live there (off: every tank is an aquarium as before, no climate values) Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    TERRARIUM_DEVICES("terrariums.devices", TERRARIUMS, List.of(), List.of("terrariumDevices"), "Terrarium devices", "Terrarium devices: heat mat, basking lamp, mister and thermo-hygrometer (off: they do nothing; a basking lamp still shines like a light)"),
    TANK_MODE("terrariums.tankMode", TERRARIUMS, List.of(), List.of(), "Tank type switch", "Tank type switch in the tank menu: Automatic (the mode from the water and the land, as before), Aquarium, Paludarium or Terrarium; a fixed terrarium takes no water and a fixed paludarium only up to the height where its land part stays at terrarium.paludariumLandShare, from buckets, pipes, pumps, the top-off and other mods alike; water already in the tank stays (off: every tank is automatic; the chosen type is kept)"),
    HERPS("terrariums.herps", TERRARIUMS, List.of(), List.of("herps"), "Amphibians and reptiles", "Amphibians and reptiles in terraria and paludaria: dart frogs, horned frog, leopard gecko, crested gecko and corn snake (off: none go into tanks; those in tanks stay)"),
    HERP_BREEDING("terrariums.herpBreeding", HERPS, List.of(BREEDING), List.of("herpBreeding"), "Breeding of amphibians and reptiles", "Breeding of the amphibians and reptiles: eggs on the land of the tank (dart frog tadpoles into the water, the leopard gecko's sex by the nest temperature; the horned frog does not breed) (off: no eggs; eggs already laid still hatch)"),
    WILD_HERPS("terrariums.wildHerps", HERPS, List.of(), List.of("wildHerps"), "Wild amphibians and reptiles", "Wild amphibians and reptiles: dart frogs and crested geckos in jungles, horned frogs in savannas and swamps, leopard geckos in deserts and badlands (#bettacraft:arid_lands), corn snakes in forests and plains (#bettacraft:snake_lands)"),
    TURTLES("turtles.enabled", null, List.of(TERRARIUMS), List.of("turtles"), "Turtles", "Turtles in paludaria and terraria: musk turtle, slider, Hermann's tortoise, Reeves' turtle and the hatchlings of the sea turtle (off: none go into tanks, none are sold; those in tanks stay) Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    TURTLE_BREEDING("turtles.breeding", TURTLES, List.of(BREEDING), List.of("turtleBreeding"), "Turtle breeding", "Turtle breeding: females lay eggs into sand or soil on the land of the tank, the nest temperature decides the sex of the hatchlings (off: no eggs; eggs already laid still hatch)"),
    WILD_TURTLES("turtles.wildSpawns", TURTLES, List.of(), List.of("wildTurtles"), "Wild turtles", "Wild turtles: sliders and musk turtles in swamps and rivers, Reeves' turtles in rivers, Hermann's tortoises in savannas and dry shrublands (#bettacraft:tortoise_lands)"),
    SEA_TURTLES("turtles.seaTurtles", TURTLES, List.of(BIG_ANIMALS), List.of(), "Sea turtles", "Sea turtles (vanilla) in big sea water paludaria: the grown ones and the hatchlings by the size rule of the big animals (1 m: 12 x 6 x 3 blocks, 100 water blocks each), from 25 ppt salt, with sand on the land, where a pair lays its eggs with features.turtles.breeding (off: no sea turtles go into tanks; those in tanks stay)"),
    MOBS_IN_GLASS("mobsInGlass.enabled", null, List.of(), List.of("mobsInGlass"), "Mobs in glass", "Mobs in glass: drowned, zombies, husks, skeletons, strays, spiders, cave spiders, silverfish, endermites and small slimes and magma cubes live shrunk and harmless in tanks (mob jar, bait, capture items of other mods); they drop nothing (off: none go in; those in tanks stay) Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    DECOR("decor.enabled", null, List.of(), List.of(), "Decoration", "The tank decoration (castle, caves, roots, stones, wrecks and the rest of the decor items) and its recipes. Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    DETAILED_DECOR("decor.detailed", DECOR, List.of(), List.of("detailedDecor"), "Detailed skull and shipwreck", "Bigger, more detailed dino skull and shipwreck decoration (off: the small old shapes for newly placed ones)"),
    DECOR_EFFECTS("decor.effects", DECOR, List.of(), List.of("decorEffects"), "Leaf litter and shelter", "Almond leaves and alder cones on the floor of a tank interior give off tannins (lower the pH a little) and feed the shrimp; clay and terracotta (tubes, caves, dragon stone) hide young shrimp (off: they are only decoration)"),
    PLANT_MESHES("visuals.plantMeshes", null, List.of(), List.of("plantMeshes"), "New plant look", "Aquarium plants and corals with real leaves, polyps and tentacles that move in the water, inside the tank and in the miniature outside (off: the old blocks). Under Features for everyone (modpack, server), under Rendering only for you"),
    SWAYING_PLANTS("visuals.swayingForeignPlants", null, List.of(), List.of("swayingForeignPlants"), "Swaying water plants (Minecraft, other mods)", "Kelp, seagrass, corals (#bettacraft:swaying_plants) and the water plants of other mods sway in the miniature outside the tank like inside (off: they stand still for everyone)"),
    SMOOTH_SWIMMING("visuals.smoothSwimming", null, List.of(), List.of("smoothSwimming"), "Smooth swimming", "Smooth swimming: fish turn in curves, tilt and right themselves smoothly, kuhli loaches undulate slowly like eels and hide by day, plecos and otocinclus only stand upright once they reach the glass (off: the old, jerkier movement)"),
    SOUNDS("sounds.enabled", null, List.of(), List.of(), "Device sounds", "The sounds of the devices: filters, air pump, air stones, sponge filter, flow pump, feeder and the bubbling of the tanks (off: the devices are silent; sounds.useVanilla picks the vanilla sounds instead). Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    WILD_SPAWN_PASS("wildSpawnPass.enabled", null, List.of(), List.of("wildSpawnPass"), "Own spawn pass for wild animals", "Now and then a few wild fish, frogs or small animals spawn in fitting water near each player, outside the vanilla water-ambient mob cap that cod, salmon, tropical fish and the fish of other mods fill (off: only the vanilla spawning) Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    CREATE("compat.create.enabled", null, List.of(), List.of(), "Create", "Integration with Create (and Create Aeronautics): tanks on contraptions, rotation for the devices, Display Links, the stainless steel mixing recipe. Off: everything below is off too, and its recipes, wandering trader offers, loot, net and fishing catches and wild spawns are gone; items and blocks already in the world stay."),
    CREATE_CONTRAPTIONS("compat.create.contraptions", CREATE, List.of(), List.of("createContraptions"), "Tanks on Create contraptions", "Tanks travel on Create contraptions (pistons, bearings, pulleys, gantries, carts, trains) and on Create Aeronautics airships: the animals are packed into the tank for the trip and drawn swimming, the care pauses, everything comes back where the tank is put down (off: Create cannot move tanks; a tank that is already travelling still unpacks)"),
    CREATE_ROTATION("compat.create.rotation", CREATE, List.of(), List.of("createRotation"), "Create rotation for devices", "Create's rotation drives the water pumps, filter pump head, water change controller, level sensor, reverse osmosis unit, food maker and fertilizer maker instead of FE: a shaft or cogwheel pointing at the device, 64 RPM for full work, slower proportionally; no stress cost (off: only FE, the crank and the other sources as before)"),
    CREATE_DISPLAY_LINKS("compat.create.displayLinks", CREATE, List.of(), List.of("createDisplayLinks"), "Create Display Links", "Tanks and monitors are sources for Create's Display Link: the values of a tank, or one line per tank a monitor shows, on display boards, nixie tubes, signs and lecterns (off: Display Links cannot read them)"),
    // M41: Create's rotation charges the tank's FE buffer through a drive adapter or a shaft on the cabinet, with real stress
    CREATE_DRIVE("compat.create.drive", CREATE, List.of(POWER), List.of(), "Create drive for tanks", "The drive adapter (only with Create): set on a tank wall or under a tank, a Create shaft at it charges the tank's FE buffer, so every device of the tank that takes the tank's FE runs on rotation (filters, flow pumps and skimmers, heaters, coolers and heat mats, lights, air pumps, misters); 64 RPM for the full power, slower proportionally, several adapters add up to a limit. It costs real stress (SU) by the load of the devices; an overstressed network gives nothing (off: no drive adapters, the cabinet shaft is off too; the FE in the buffer stays)"),
    CREATE_CABINET_DRIVE("compat.create.cabinetDrive", CREATE_DRIVE, List.of(), List.of(), "Create shaft on the cabinet", "A cabinet under a tank takes a Create shaft on the side set in its menu (createDrive.cabinetSides lists the sides allowed): it charges the tank's FE buffer like a drive adapter, and with power.winding on what the tank does not need winds the cabinet's spring (off: the cabinet takes no shaft; its setting is kept)"),
    COMPAT_MEKANISM("compat.mekanism", null, List.of(), List.of(), "Mekanism", "Integration with Mekanism: the stainless steel recipe of its metallurgic infuser."),
    COMPAT_ENDERIO("compat.enderio", null, List.of(), List.of(), "EnderIO", "Integration with EnderIO: the stainless steel recipe of its alloy smelter."),
    COMPAT_THERMAL("compat.thermal", null, List.of(), List.of(), "Thermal", "Integration with Thermal Expansion: the stainless steel recipe of its induction smelter."),
    COMPAT_JEI("compat.jei", null, List.of(), List.of(), "JEI", "Integration with JEI (and EMI / REI through JEI): our recipe categories, info pages and the hidden items of features that are off."),
    COMPAT_OVERLAYS("compat.overlays", null, List.of(), List.of(), "Jade, The One Probe, WTHIT", "The lines of tanks, bettas, filters and cultures in the overlay mods Jade, The One Probe and WTHIT (compat.overlayProvider picks which one)."),
    COMPAT_GUIDEME("compat.guideme", null, List.of(), List.of(), "GuideME handbook", "The Bettacraft handbook through GuideME (off: no handbook item and no handbook pages)."),
    COMPAT_BETTER_FISH_TANKS("compat.betterFishTanks", null, List.of(), List.of(), "Better Fish Tanks", "Integration with Better Fish Tanks: our filled fish bag and the betta bucket release their animal into a Better Fish Tanks tank."),
    ALARM_WAYPOINTS("compat.journeymap", null, List.of(MONITORS), List.of("alarmWaypoints"), "Waypoints for monitor alarms", "A monitor alarm puts a waypoint at its tank on the map of the tank's owner and of the players within 128 blocks (JourneyMap); it goes away when the alarm clears. Each player can turn it off in the client config (off: no waypoints)"),
    MODPACK_EVENTS("compat.modpackEvents", null, List.of(), List.of("modpackEvents"), "Events for modpack scripts", "Bettacraft posts its events for modpack scripts (KubeJS NativeEvents) and other mods on the NeoForge event bus: an animal was born or hatched in a tank, an animal of a tank died (with the cause), an alarm tile of a monitor went off or cleared (off: none are posted)");

    private final String path;
    @Nullable
    private final Feature parent;
    private final List<Feature> requires;
    private final List<String> old;
    private final String label;
    private final String tooltip;

    Feature(String path, @Nullable Feature parent, List<Feature> requires, List<String> old, String label, String tooltip) {
        this.path = path;
        this.parent = parent;
        this.requires = requires;
        this.old = old;
        this.label = label;
        this.tooltip = tooltip;
    }

    /** The path below {@code features} ({@code tankmates.cichlids}). */
    public String path() {
        return path;
    }

    /** The full config key ({@code features.tankmates.cichlids}), as the handbook and the log name it. */
    public String key() {
        return "features." + path;
    }

    /** The translation key of the label (tooltip: {@code + ".tooltip"}). */
    public String translationKey() {
        return "bettacraft.configuration.features." + path;
    }

    @Nullable
    public Feature parent() {
        return parent;
    }

    public List<Feature> requires() {
        return requires;
    }

    /** The keys below {@code features} this switch had before M33. */
    public List<String> oldKeys() {
        return old;
    }

    public String label() {
        return label;
    }

    public String tooltip() {
        return tooltip;
    }

    /** A group switch ({@code <group>.enabled}). */
    public boolean isGroup() {
        return path.endsWith(".enabled");
    }

    /** The feature with this path ({@code tankmates.cichlids}) or null. */
    @Nullable
    public static Feature byPath(String path) {
        for (Feature f : values()) if (f.path.equals(path)) return f;
        return null;
    }

    /** The sections of the tree below {@code features} with their en labels (in config order). */
    public static final Map<String, String> SECTIONS;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("bettas", "Bettas");
        m.put("breeding", "Breeding and genetics");
        m.put("interior", "Tank interior");
        m.put("floatingPlants", "Floating plants");
        m.put("aquaticPlants", "Aquarium plants");
        m.put("algae", "Algae");
        m.put("invertebrates", "Shrimp, snails and crayfish");
        m.put("frogs", "Dwarf clawed frogs");
        m.put("tankmates", "Tankmate fish");
        m.put("saltwater", "Salt water");
        m.put("corals", "Corals");
        m.put("foreignAnimals", "Vanilla and mod animals");
        m.put("power", "Power (FE)");
        m.put("equipment", "Equipment");
        m.put("pipes", "Pipes");
        m.put("waterChemistry", "Water chemistry");
        m.put("co2", "CO2 dosing");
        m.put("osmosis", "Reverse osmosis");
        m.put("liveFood", "Live food");
        m.put("foodMachine", "Food maker");
        m.put("foodVariety", "Food variety");
        m.put("fertilizer", "Liquid fertilizer"); // M37
        m.put("monitors", "Monitors");
        m.put("bigTanks", "Big tanks");
        m.put("terrariums", "Terrariums and paludariums");
        m.put("turtles", "Turtles");
        m.put("mobsInGlass", "Mobs in glass");
        m.put("decor", "Decoration");
        m.put("visuals", "Looks and movement");
        m.put("sounds", "Sounds");
        m.put("wildSpawnPass", "Own spawn pass");
        m.put("compat", "Other mods");
        m.put("compat.create", "Create");
        SECTIONS = Collections.unmodifiableMap(m);
    }
}
