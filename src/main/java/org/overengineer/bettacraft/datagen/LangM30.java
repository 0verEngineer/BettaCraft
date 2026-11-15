package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M30 part a English texts (big tanks, the size rule of the big animals, the dolphin, the catching tools, the pangasius).
 * The German ones are in de_de.json; edit both.
 */
final class LangM30 {
    private LangM30() {}

    static void addAll(BiConsumer<String, String> a) {
        // config
        a.accept("bettacraft.configuration.bigTanks", "Big tanks");
        a.accept("bettacraft.configuration.bigTanks.tooltip", "Big tanks: up to bigTankRules.bigMaxSize* blocks (32 x 16 x 32), with a smaller interior scale (8 or 4) from bigTankRules.interiorScale8Above (off: tank.maxSize* as before; big tanks already built stay)");
        a.accept("bettacraft.configuration.bigAnimals", "Big animals");
        a.accept("bettacraft.configuration.bigAnimals.tooltip", "Big animals: giant catfish, sharks, rays and the vanilla dolphin in big tanks by the size rule (bigAnimalRules.*), the large net, the transport tub and the steel leader, wild big animals (off: none go into tanks, no new ones from the wild; those in tanks stay)");
        a.accept("bettacraft.configuration.bigTankRules", "Big tanks");
        a.accept("bettacraft.configuration.bigAnimalRules", "Big animals");
        a.accept("bettacraft.configuration.bigMaxSizeX", "Big tank: maximum size X");
        a.accept("bettacraft.configuration.bigMaxSizeX.tooltip", "Maximum tank size along X with features.bigTanks (the larger of this and tank.maxSizeX applies)");
        a.accept("bettacraft.configuration.bigMaxSizeY", "Big tank: maximum size Y");
        a.accept("bettacraft.configuration.bigMaxSizeY.tooltip", "Maximum tank size along Y with features.bigTanks (the larger of this and tank.maxSizeY applies)");
        a.accept("bettacraft.configuration.bigMaxSizeZ", "Big tank: maximum size Z");
        a.accept("bettacraft.configuration.bigMaxSizeZ.tooltip", "Maximum tank size along Z with features.bigTanks (the larger of this and tank.maxSizeZ applies)");
        a.accept("bettacraft.configuration.interiorScale8Above", "Interior scale 8 above");
        a.accept("bettacraft.configuration.interiorScale8Above.tooltip", "A tank whose longest side is longer than this many blocks gets interior scale 8 instead of 16 (its interior stays at most 256 blocks long); an existing interior is shrunk when the tank grows past it");
        a.accept("bettacraft.configuration.interiorScale4Above", "Interior scale 4 above");
        a.accept("bettacraft.configuration.interiorScale4Above.tooltip", "A tank whose longest side is longer than this many blocks gets interior scale 4");
        a.accept("bettacraft.configuration.longSidePerMetre", "Longer side per metre");
        a.accept("bettacraft.configuration.longSidePerMetre.tooltip", "Blocks of the longer horizontal side of a tank per metre of the animal's length (a 1 m shark: 12)");
        a.accept("bettacraft.configuration.shortSidePerMetre", "Shorter side per metre");
        a.accept("bettacraft.configuration.shortSidePerMetre.tooltip", "Blocks of the shorter horizontal side of a tank per metre of the animal's length (a 1 m shark: 6)");
        a.accept("bettacraft.configuration.heightPerMetre", "Height per metre");
        a.accept("bettacraft.configuration.heightPerMetre.tooltip", "Blocks of the tank height per metre of the animal's length (a 1 m shark: 3)");
        a.accept("bettacraft.configuration.waterPerCubicMetre", "Water per cubic metre");
        a.accept("bettacraft.configuration.waterPerCubicMetre.tooltip", "Water blocks of the full tank per big animal and cubic metre of its length (a 1 m shark 100, a 2.6 m dolphin 1758)");
        a.accept("bettacraft.configuration.dolphinHungryDays", "Dolphin hungry after");
        a.accept("bettacraft.configuration.dolphinHungryDays.tooltip", "Days a dolphin in a tank goes without raw fish (fed by hand) before it starts to lose health");
        a.accept("bettacraft.configuration.bigWildNearby", "Wild big animals nearby");
        a.accept("bettacraft.configuration.bigWildNearby.tooltip", "Wild big animals only spawn while fewer than this many of them live within 64 blocks (0 = none spawn)");
        a.accept("bettacraft.configuration.bigSpawnChance", "Big animal spawn chance");
        a.accept("bettacraft.configuration.bigSpawnChance.tooltip", "Chance that a spawn of a wild big animal that passed all other rules really happens (keeps them rare)");
        a.accept("bettacraft.configuration.tubSlowness", "Transport tub slowness");
        a.accept("bettacraft.configuration.tubSlowness.tooltip", "How much slower a player walks while carrying a transport tub with an animal (0.35 = 35 % slower)");
        a.accept("bettacraft.configuration.largeNetChance", "Large net chance");
        a.accept("bettacraft.configuration.largeNetChance.tooltip", "Chance per swing of the large net in open water to catch a big animal of that water (an empty transport tub must be in the inventory)");
        a.accept("bettacraft.configuration.steelLeaderChance", "Steel leader chance");
        a.accept("bettacraft.configuration.steelLeaderChance.tooltip", "Chance per catch with a fishing rod and a steel leader in the other hand to hook a big animal of that water instead (an empty transport tub must be in the inventory)");
        a.accept("bettacraft.configuration.bigAnimalWeight", "Big animal spawn weight");
        a.accept("bettacraft.configuration.bigAnimalWeight.tooltip", "Spawn weight of the wild big animals (giant catfish, sharks, rays) in their biomes per weight point of the species; they also need deep water and few of their kind nearby (bigAnimalRules.*); 0 disables them (features.bigAnimals)");
        // messages
        a.accept("message.bettacraft.big.too_small", "%1$s needs a tank of at least %2$s × %3$s blocks and %4$s blocks high (this one: %5$s × %6$s, %7$s high)");
        a.accept("message.bettacraft.big.too_little_water", "Not enough water for %1$s: the big animals need %2$s water blocks together, this tank holds %3$s");
        a.accept("message.bettacraft.big.dolphin_salt", "%1$s only lives in sea water (from %2$s ppt salt)");
        a.accept("message.bettacraft.big.no_bucket", "%s is too big for a bucket or a bag: catch it with the large net into a transport tub");
        a.accept("message.bettacraft.big.tub_small", "The transport tub is for big animals; %s goes into a fish bag");
        a.accept("message.bettacraft.big.needs_net", "%s is too quick to scoop up: catch it with the large net (an empty transport tub in your inventory)");
        a.accept("message.bettacraft.big.need_tub", "You need an empty transport tub for %s");
        a.accept("message.bettacraft.big.need_tub_net", "The large net needs an empty transport tub in your inventory");
        a.accept("message.bettacraft.big.nothing_here", "No big animals live in this water");
        a.accept("message.bettacraft.big.caught", "Caught: %s (in the transport tub)");
        a.accept("message.bettacraft.big.net_small", "The large net is for big animals; catch %s with the aquarium net");
        a.accept("message.bettacraft.big.dolphin_fed", "%s ate the fish");
        a.accept("log.bettacraft.dolphin_hungry", "%s is hungry: feed it raw fish (right-click the tank)");
        a.accept("log.bettacraft.dolphin_fresh", "%s suffers in too fresh water: dolphins need sea water");
        // items
        a.accept("item.bettacraft.large_net", "Large Net");
        a.accept("tooltip.bettacraft.large_net", "Catches a big animal (giant catfish, shark, ray, dolphin) into an empty transport tub in your inventory; swung in open water it now and then brings one up");
        a.accept("item.bettacraft.transport_tub", "Transport Tub");
        a.accept("item.bettacraft.transport_tub.filled", "Transport Tub (%s)");
        a.accept("tooltip.bettacraft.transport_tub", "Carries one big animal. Right-click one in a tank (or take it out in the tank menu); wild ones need the large net");
        a.accept("tooltip.bettacraft.transport_tub.filled", "Heavy: you walk slower while you carry it. Right-click a big enough tank to put the animal in, or open water to release it");
        a.accept("item.bettacraft.steel_leader", "Steel Leader");
        a.accept("tooltip.bettacraft.steel_leader", "Hold it in the other hand while fishing: now and then a big fish of the water bites (an empty transport tub in your inventory)");
        a.accept("info.bettacraft.large_net", "Large net for big animals (giant catfish, sharks, rays, dolphins). Right-click a wild big animal with an empty transport tub in your inventory to catch it; swung in open water it now and then brings up a big animal of that water.");
        a.accept("info.bettacraft.transport_tub", "Transport tub for one big animal. It is heavy: you walk slower while you carry a full one. Right-click a big enough tank to put the animal in (size rule: about 12 × 6 × 3 blocks per metre of the animal), open water to release it. Take big animals out of a tank in the tank menu.");
        a.accept("info.bettacraft.steel_leader", "Steel leader: hold it in the other hand while fishing with any rod. Now and then a big fish of the water bites instead of the normal catch and goes into an empty transport tub.");
        // the pangasius
        a.accept("entity.bettacraft.pangasius", "Pangasius");
        a.accept("item.bettacraft.pangasius_spawn_egg", "Pangasius Spawn Egg");
        a.accept("info.bettacraft.tankmate.pangasius", "Pangasius (Pangasianodon hypophthalmus), the iridescent shark catfish of the Mekong: a restless, silvery giant of big rivers, in a home tank about 1 m long.\nBig animal: needs a tank of at least 12 × 6 × 3 blocks and 100 water blocks each; swims wide curves.\n22–30 °C, eats everything and, once grown, small fish, young bettas and shrimp. Does not breed in a tank.\nCatch: large net or steel leader in rivers and jungle water (into a transport tub); rarely wild in deep rivers.");
        a.accept("variety.bettacraft.pangasius.wild", "Wild type");
        a.accept("tankmate.bettacraft.group.catfish", "Catfish");
        // M30 part b: the other freshwater giants
        a.accept("tankmate.bettacraft.group.bonytongue", "Bonytongues");
        a.accept("tankmate.bettacraft.group.ray", "Rays");
        a.accept("entity.bettacraft.redtail_catfish", "Redtail Catfish");
        a.accept("item.bettacraft.redtail_catfish_spawn_egg", "Redtail Catfish Spawn Egg");
        a.accept("info.bettacraft.tankmate.redtail_catfish", "Redtail catfish (Phractocephalus hemioliopterus) of the Amazon and the Orinoco: a heavy catfish with a broad, flat head, long barbels, a white flank band and a bright red tail; 1.2 m in the game.\nBig animal: needs a tank of at least 15 × 8 × 4 blocks and 173 water blocks each; rests on the ground by day.\n20–28 °C, soft to medium water. A predator: swallows every small fish, betta and shrimp it can reach. Does not breed in a tank.\nCatch: large net or steel leader in jungle water (into a transport tub); rarely wild there.");
        a.accept("variety.bettacraft.redtail_catfish.wild", "Wild type");
        a.accept("entity.bettacraft.wels_catfish", "Wels Catfish");
        a.accept("item.bettacraft.wels_catfish_spawn_egg", "Wels Catfish Spawn Egg");
        a.accept("info.bettacraft.tankmate.wels_catfish", "Wels catfish (Silurus glanis), the giant catfish of the deep rivers of Europe: a long, marbled body with an endless anal fin and two long barbels; 1.5 m in the game (wild up to 2.7 m).\nBig animal: needs a tank of at least 18 × 9 × 5 blocks and 338 water blocks each; a night hunter that lies on the ground by day.\n4–26 °C (no heater needed), hardy, neutral to hard water. Eats small fish, bettas and shrimp. Does not breed in a tank.\nCatch: large net or steel leader in rivers (into a transport tub); very rarely wild in deep rivers.");
        a.accept("variety.bettacraft.wels_catfish.wild", "Wild type");
        a.accept("entity.bettacraft.arapaima", "Arapaima");
        a.accept("item.bettacraft.arapaima_spawn_egg", "Arapaima Spawn Egg");
        a.accept("info.bettacraft.tankmate.arapaima", "Arapaima (Arapaima gigas), the giant of the Amazon floodplains: a long, round body whose scales turn red towards the tail; 2 m in the game (wild up to 3 m).\nBig animal: needs a tank of at least 24 × 12 × 6 blocks and 800 water blocks each.\nAn air breather: comes up to the surface for air every few minutes and does not mind low oxygen. 24–31 °C, soft to medium water. Eats fish, bettas and shrimp. Does not breed in a tank.\nCatch: large net or steel leader in jungle water (into a transport tub); very rarely wild there.");
        a.accept("variety.bettacraft.arapaima.wild", "Wild type");
        a.accept("entity.bettacraft.motoro_stingray", "Motoro Stingray");
        a.accept("item.bettacraft.motoro_stingray_spawn_egg", "Motoro Stingray Spawn Egg");
        a.accept("info.bettacraft.tankmate.motoro_stingray", "Motoro stingray (Potamotrygon motoro), the freshwater stingray of the Amazon: a round, flat disc with orange, black-ringed eye spots; 0.6 m with the tail.\nBig animal: needs a tank of at least 8 × 4 × 2 blocks and 22 water blocks each; glides low over the ground with a ripple along its disc and lies on the substrate (sand) for long.\n24–30 °C, soft, very clean water. Eats worms, shrimp and small bottom fish.\nLivebearing: a pair has 2–5 pups after a long pregnancy, only as many as the tank has room for (size rule).\nCatch: large net or steel leader in jungle water (into a transport tub); rarely wild there.");
        a.accept("variety.bettacraft.motoro_stingray.wild", "Wild type");
    }
}
