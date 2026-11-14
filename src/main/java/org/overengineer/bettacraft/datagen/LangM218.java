package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/** M21.8 English texts (stacking artemia cyst jars, seahorses). de_de.json holds the German ones. */
final class LangM218 {
    private LangM218() {}

    static void addAll(BiConsumer<String, String> a) {
        // artemia cysts
        a.accept("tooltip.bettacraft.artemia_cysts.total", "%s jars, %s uses in total");
        a.accept("bettacraft.configuration.artemiaCystStackSize", "Artemia cyst jars per stack");
        a.accept("bettacraft.configuration.artemiaCystStackSize.tooltip", "How many jars of artemia cysts stack in one slot: 16, 32 or 64 (each jar keeps its 16 uses; only jars with the same uses left stack; stacks bigger than a smaller new limit stay as they are)");
        // config: seahorses
        a.accept("bettacraft.configuration.seahorses", "Seahorses");
        a.accept("bettacraft.configuration.seahorses.tooltip", "Seahorses (lined seahorse Hippocampus erectus, dwarf seahorse H. zosterae) at the wandering trader and in the aquarium net in warm and lukewarm oceans (off: no new ones; seahorses already caught stay)");
        a.accept("bettacraft.configuration.maxFlow", "Highest flow for seahorses");
        a.accept("bettacraft.configuration.maxFlow.tooltip", "Water movement above which seahorses are stressed (1 = the strong flow stony corals need; one external filter in a tank of 16 water blocks gives 0.5; 0 = flow never stresses them)");
        a.accept("bettacraft.configuration.needHitchingPosts", "Seahorses need hitching posts");
        a.accept("bettacraft.configuration.needHitchingPosts.tooltip", "Seahorses are stressed without a hitching post (aquarium plants, corals or decoration that stick up in the tank interior) to hold on to with their tail");
        a.accept("bettacraft.configuration.feedingsPerDay", "Seahorse feedings per day");
        a.accept("bettacraft.configuration.feedingsPerDay.tooltip", "Feedings a day a seahorse needs (they have no stomach; 1 = like the other fish)");
        a.accept("bettacraft.configuration.foodShareWithFastFish", "Seahorse food share next to fast fish");
        a.accept("bettacraft.configuration.foodShareWithFastFish.tooltip", "Chance that a slow seahorse gets its share of the food while fast fish are in the tank");
        a.accept("bettacraft.configuration.harassedByFastFish", "Fast fish harass seahorses");
        a.accept("bettacraft.configuration.harassedByFastFish.tooltip", "Fast or pushy reef fish (tangs, damselfish, chromis, clownfish) stress seahorses");
        a.accept("bettacraft.configuration.pouchDays", "Days in the brood pouch");
        a.accept("bettacraft.configuration.pouchDays.tooltip", "Days a male seahorse carries the eggs in his brood pouch before the fry are born");
        // species
        a.accept("entity.bettacraft.lined_seahorse", "Lined Seahorse");
        a.accept("item.bettacraft.lined_seahorse_bucket", "Bucket of Lined Seahorse");
        a.accept("item.bettacraft.lined_seahorse_spawn_egg", "Lined Seahorse Spawn Egg");
        a.accept("variety.bettacraft.lined_seahorse.wild", "Dark (wild type)");
        a.accept("variety.bettacraft.lined_seahorse.yellow", "Yellow");
        a.accept("variety.bettacraft.lined_seahorse.orange", "Orange");
        a.accept("info.bettacraft.tankmate.lined_seahorse", "Lined Seahorse (Hippocampus erectus) from sea grass, mangrove roots and estuaries of the western Atlantic; the most common captive-bred seahorse.\nSwims slowly and upright and holds on to plants, corals or decoration with its tail most of the time: needs hitching posts and low flow (no strong pumps).\nEats only live food: artemia nauplii and mosquito larvae, twice a day; slow next to fast fish. No tangs, damselfish, chromis or clownfish.\n18–27 °C, sea water and strong brackish water (20–42 ‰). Counts 1.5 fish in the stocking.\nThe female lays her eggs into the brood pouch of the male, who gives birth to the fry a few days later; they eat artemia from the start.\nCatch: aquarium net in lukewarm and warm oceans, wandering trader (rare).");
        a.accept("entity.bettacraft.dwarf_seahorse", "Dwarf Seahorse");
        a.accept("item.bettacraft.dwarf_seahorse_bucket", "Bucket of Dwarf Seahorse");
        a.accept("item.bettacraft.dwarf_seahorse_spawn_egg", "Dwarf Seahorse Spawn Egg");
        a.accept("variety.bettacraft.dwarf_seahorse.wild", "Wild type");
        a.accept("info.bettacraft.tankmate.dwarf_seahorse", "Dwarf Seahorse (Hippocampus zosterae) from the sea grass meadows of Florida and the Gulf of Mexico; only 2–3 cm, for small tanks of its own.\nHolds on to plants and corals, needs very little flow and many small meals of artemia nauplii (live food).\n21–28 °C, sea water and strong brackish water (20–42 ‰). Counts 0.3 fish in the stocking.\nThe male carries the eggs in his brood pouch and gives birth to a few fry.\nCatch: aquarium net in warm oceans, wandering trader (rare).");
        a.accept("tankmate.bettacraft.group.seahorse", "Seahorses");
        a.accept("tankmate.bettacraft.breeding.pouch", "Brood pouch: the male carries the eggs");
        // menu, log
        a.accept("gui.bettacraft.tm.flow", "Current too strong");
        a.accept("gui.bettacraft.tm.no_posts", "Nothing to hold on to");
        a.accept("gui.bettacraft.tm.hitched", "Holds on to a post");
        a.accept("gui.bettacraft.tm.pouch", "Brood pouch full: %s %%");
        a.accept("gui.bettacraft.tm.live_food", "Live food only (artemia, larvae)");
        a.accept("log.bettacraft.tm.pouch", "%s laid her eggs into the brood pouch of %s");
        a.accept("log.bettacraft.tm.pouch_born", "%s gave birth to %s young from his brood pouch");
        a.accept("log.bettacraft.tm.seahorse_harassed", "%s harass the seahorses (too fast and pushy for them)");
        a.accept("gui.bettacraft.breeding.tm.posts", "Hitching posts: %s");
        a.accept("gui.bettacraft.breeding.tm.pouches", "Full brood pouches: %s");
        a.accept("cause.bettacraft.flow", "a current that was too strong");
        a.accept("cause.bettacraft.no_posts", "having nothing to hold on to");
    }
}
