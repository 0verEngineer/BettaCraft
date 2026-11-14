package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M22.3 English texts (wild animals and corals in the world, wild colours, mutations, the magnifier, single small animals,
 * several animals per bucket and bag). de_de.json holds the German ones.
 */
final class LangM223 {
    private LangM223() {}

    static void addAll(BiConsumer<String, String> a) {
        // items, blocks, the wild small animal
        a.accept("item.bettacraft.critter_bucket", "Bucket of Small Animals");
        a.accept("item.bettacraft.critter_bucket.filled", "Bucket of %s %s");
        a.accept("item.bettacraft.bucket.group", "Bucket of %s %s");
        a.accept("item.bettacraft.magnifier", "Magnifier");
        a.accept("entity.bettacraft.wild_critter", "Small Animal");
        a.accept("block.bettacraft.wild_zoanthus", "Wild Zoanthids");
        a.accept("block.bettacraft.wild_green_star_polyps", "Wild Green Star Polyps");
        a.accept("block.bettacraft.wild_mushroom_coral", "Wild Mushroom Coral");
        a.accept("block.bettacraft.wild_leather_coral", "Wild Leather Coral");
        a.accept("block.bettacraft.wild_hammer_coral", "Wild Hammer Coral");
        a.accept("block.bettacraft.wild_lobophyllia", "Wild Lobophyllia");
        a.accept("block.bettacraft.wild_acropora", "Wild Acropora");
        a.accept("block.bettacraft.wild_bubble_tip_anemone", "Wild Bubble Tip Anemone");
        a.accept("info.bettacraft.magnifier", "Analyses the hidden genes of the animals in a tank.\nRight-click a tank: every animal in it is analysed. In the tank menu the \"Analyse\" button analyses the selected fish or small animal.\nNew animals (caught, bought, born) do not show their hidden genes until they are analysed; the result stays with the animal in tanks, buckets and bags.\n64 uses (one per animal), repaired at the anvil with glass panes.\nCraft: a glass pane in a ring of copper or gold ingots on a stick.");
        a.accept("info.bettacraft.critter_bucket", "Shrimp, crayfish, snails or hermit crabs of one species in a plain bucket of water (up to 32 small ones, 24 bigger ones).\nTake them out of a tank in the tank menu (bucket button), or catch them in the world with a bucket of water.\nUse it on a tank to put them in (with 1000 mB of water); poured out in the world they crawl off.");
        a.accept("info.bettacraft.wild_coral", "A coral colony from a warm ocean reef.\nShears cut a frag off it for a reef tank; the colony grows back after a while.\nIt lives under water only; broken it drops nothing (silk touch: itself).");
        // tooltips
        a.accept("tooltip.bettacraft.magnifier", "Right-click a tank: analyses the hidden genes of every animal in it");
        a.accept("tooltip.bettacraft.magnifier.menu", "Tank menu: \"Analyse\" for one selected animal");
        a.accept("tooltip.bettacraft.magnifier.free", "On this server every animal shows its genes anyway");
        a.accept("tooltip.bettacraft.genes", "Genes: %s");
        a.accept("tooltip.bettacraft.genes.short", " [%s]");
        a.accept("tooltip.bettacraft.genes.unknown", "Genes not analysed (magnifier)");
        a.accept("tooltip.bettacraft.genes.unknown_short", " [?]");
        // messages
        a.accept("message.bettacraft.magnifier.analysed", "Analysed %s animals: their genes are known now");
        a.accept("message.bettacraft.magnifier.none", "Every animal in this tank is analysed already");
        a.accept("message.bettacraft.magnifier.already", "This animal is analysed already");
        a.accept("message.bettacraft.magnifier.need", "You need a magnifier");
        a.accept("message.bettacraft.magnifier.free", "On this server every animal shows its genes anyway");
        a.accept("message.bettacraft.container.full", "This container is full (%s animals)");
        a.accept("message.bettacraft.container.needs_sea_water", "A reef animal needs a bucket of sea water (or an empty bucket)");
        a.accept("message.bettacraft.container.needs_fresh_water", "A freshwater animal needs a bucket of fresh water (or an empty bucket)");
        a.accept("message.bettacraft.container.no_bucket", "You need a bucket of water to catch %s");
        a.accept("message.bettacraft.critters.none_of_sex", "None of this kind and sex in the tank");
        a.accept("message.bettacraft.critters.released_world", "%s animals crawl off");
        a.accept("message.bettacraft.wild_coral.trimmed", "This colony was cut just now; it has to grow back first");
        a.accept("log.bettacraft.analysed", "%s analysed %s animals with the magnifier");
        // the tank menu: single small animals, sex and container of the take-out, the magnifier
        a.accept("gui.bettacraft.magnifier.analyse", "Analyse");
        a.accept("gui.bettacraft.magnifier.tooltip", "Analyse the hidden genes of the selected animal with your magnifier (one use)");
        a.accept("gui.bettacraft.magnifier.nothing", "Select an animal that is not analysed yet");
        a.accept("gui.bettacraft.critters.sex.any", "Any sex");
        a.accept("gui.bettacraft.critters.sex.male", "♂ Males");
        a.accept("gui.bettacraft.critters.sex.female", "♀ Females");
        a.accept("gui.bettacraft.critters.sex_tooltip", "Which animals \"Take out\" takes from the selected kind: any, only males or only females");
        a.accept("gui.bettacraft.critters.into_bag", "Bag");
        a.accept("gui.bettacraft.critters.into_bucket", "Bucket");
        a.accept("gui.bettacraft.critters.take_bucket", "Into bucket");
        a.accept("gui.bettacraft.critters.container_tooltip", "Take out into fish bags (100 mB each) or buckets (a bucket of water that suits them, or an empty bucket with 1000 mB of the tank)");
        a.accept("gui.bettacraft.critters.take_one_tooltip", "Takes exactly the selected animal out");
        a.accept("gui.bettacraft.critters.young_short", "young");
        a.accept("gui.bettacraft.critters.hermaphrodite_one", "Hermaphrodite");
        a.accept("gui.bettacraft.critters.adult", "adult");
        a.accept("gui.bettacraft.critters.young_one", "young");
        a.accept("gui.bettacraft.critters.one_age", "%s, %s, %s days old");
        a.accept("gui.bettacraft.critters.one_grade", "Colour density %s %%");
        a.accept("gui.bettacraft.critters.genes", "Genes: %s");
        a.accept("gui.bettacraft.critters.genes_unknown", "Genes not analysed (magnifier)");
        a.accept("gui.bettacraft.critters.one_eggs", "Carries eggs");
        a.accept("gui.bettacraft.fish.genes_unknown", "Genes not analysed");
        a.accept("gui.bettacraft.fish.genes_unknown_hint", "Analyse it with a magnifier to see them");
        a.accept("gui.bettacraft.breeding.prediction_hidden", "Analyse both parents with the magnifier to see what the young will look like");
        // config
        a.accept("bettacraft.configuration.wildlife", "Wild animals, colours, mutations");
        a.accept("bettacraft.configuration.beautifulWildChance", "Beautiful wild animals");
        a.accept("bettacraft.configuration.beautifulWildChance.tooltip", "Chance that a wild animal (spawned in the world or caught with the net) shows a bred colour openly; most are wild-coloured, some carry a bred colour hidden");
        a.accept("bettacraft.configuration.otherMutationChance", "Mutations of the other animals");
        a.accept("bettacraft.configuration.otherMutationChance.tooltip", "Chance per gene and birth that a shrimp, crayfish, snail or a tankmate without its own genetics mutates (bettas: breeding.mutationChance, guppies and endlers 0.4 %)");
        a.accept("bettacraft.configuration.genesAlwaysVisible", "Genes always visible");
        a.accept("bettacraft.configuration.genesAlwaysVisible.tooltip", "Hidden genes of every animal are always visible, without analysing them with the magnifier");
        a.accept("bettacraft.configuration.wildFishNearby", "Wild fish nearby at most");
        a.accept("bettacraft.configuration.wildFishNearby.tooltip", "Wild tankmate fish and frogs only spawn while fewer than this many of them live within 32 blocks (keeps them rare, in small shoals)");
        a.accept("bettacraft.configuration.wildCrittersNearby", "Wild small animals nearby at most");
        a.accept("bettacraft.configuration.wildCrittersNearby.tooltip", "Wild shrimp, crayfish, snails and hermit crabs only spawn while fewer than this many of them live within 32 blocks");
        a.accept("bettacraft.configuration.wildFishWeight", "Spawn weight of wild fish");
        a.accept("bettacraft.configuration.wildFishWeight.tooltip", "Spawn weight of the wild tankmate fish and dwarf clawed frogs in their biomes, in tenths of their net catch weight; 0 disables them");
        a.accept("bettacraft.configuration.wildCritterWeight", "Spawn weight of wild small animals");
        a.accept("bettacraft.configuration.wildCritterWeight.tooltip", "Spawn weight of wild shrimp, crayfish, snails and hermit crabs crawling on the bottom of their waters; 0 disables them");
        a.accept("bettacraft.configuration.wildFish", "Wild fish in the world");
        a.accept("bettacraft.configuration.wildFish.tooltip", "Wild tankmate fish, seahorses and dwarf clawed frogs spawn in their biomes, rarely and in small shoals (off: only the net, the trader and breeding bring them)");
        a.accept("bettacraft.configuration.wildCritters", "Wild small animals in the world");
        a.accept("bettacraft.configuration.wildCritters.tooltip", "Wild shrimp, crayfish, snails and hermit crabs crawl on the bottom of rivers, swamps, mangroves, beaches, jungle water and warm oceans; caught with the net, a fish bag or a bucket, and they can be let go again (off: they do not spawn, and let go into the world they are gone like before)");
        a.accept("bettacraft.configuration.wildCorals", "Wild corals on the reefs");
        a.accept("bettacraft.configuration.wildCorals.tooltip", "Every Bettacraft coral and the bubble tip anemone grow on the warm ocean coral reefs (new chunks); shears cut a frag off them (off: no new ones in new chunks)");
        a.accept("bettacraft.configuration.wildColors", "Beautiful wild colours");
        a.accept("bettacraft.configuration.wildColors.tooltip", "Now and then a wild animal shows a bred colour openly (wildlife.beautifulWildChance; off: wild animals are wild-coloured, bred colours only hidden)");
        a.accept("bettacraft.configuration.mutations", "Mutations of every animal");
        a.accept("bettacraft.configuration.mutations.tooltip", "Spontaneous mutations also for shrimp, crayfish, snails and the tankmates without their own genetics (off: only bettas, guppies and endlers mutate)");
        a.accept("bettacraft.configuration.magnifier", "Magnifier");
        a.accept("bettacraft.configuration.magnifier.tooltip", "The hidden genes of a new animal are unknown until it is analysed with the magnifier in a tank (off: every animal shows its genes like before)");
        a.accept("bettacraft.configuration.bigContainers", "Several animals per bucket and bag");
        a.accept("bettacraft.configuration.bigContainers.tooltip", "Several animals of one species per bucket and fish bag by their size (a bucket: 32 shrimp, 24 rasboras, 1 betta; a bag half of that), small animals also in a plain bucket (off: one fish per bucket, bags as before)");
        a.accept("bettacraft.configuration.animalList", "List of single small animals");
        a.accept("bettacraft.configuration.animalList.tooltip", "List of every single shrimp, crayfish and snail in the tank menu (sex, age, colour density, genes) to take out exactly that one, and the sex choice for the quick take-out (off: only the kinds like before)");
    }
}
