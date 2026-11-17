package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M48 English texts: part a the bred forms of tetras and corydoras, the bronze corydoras and the panda garra; part b the new
 * reef animals, the copepods of the live rock and the clownfish larvae with their food chain. The German ones are in
 * de_de.json; edit both.
 */
final class LangM48 {
    private LangM48() {}

    static void addAll(BiConsumer<String, String> a) {
        // part a: the bred forms (recessive on the colour locus)
        a.accept("variety.bettacraft.neon_tetra.wild", "Wild type");
        a.accept("variety.bettacraft.neon_tetra.diamond", "Diamond");
        a.accept("variety.bettacraft.neon_tetra.gold", "Gold");
        a.accept("variety.bettacraft.neon_tetra.albino", "Albino");
        a.accept("variety.bettacraft.cardinal_tetra.wild", "Wild type");
        a.accept("variety.bettacraft.cardinal_tetra.gold", "Gold");
        a.accept("variety.bettacraft.glowlight_tetra.wild", "Wild type");
        a.accept("variety.bettacraft.glowlight_tetra.albino", "Albino");
        a.accept("variety.bettacraft.panda_cory.wild", "Wild type");
        a.accept("variety.bettacraft.panda_cory.albino", "Albino");
        a.accept("variety.bettacraft.panda_cory.longfin", "Longfin");
        a.accept("variety.bettacraft.sterbai_cory.wild", "Wild type");
        a.accept("variety.bettacraft.sterbai_cory.albino", "Albino");
        // part a: the bronze corydoras and the panda garra
        a.accept("entity.bettacraft.bronze_cory", "Bronze Cory");
        a.accept("item.bettacraft.bronze_cory_bucket", "Bucket of Bronze Cory");
        a.accept("item.bettacraft.bronze_cory_spawn_egg", "Bronze Cory Spawn Egg");
        a.accept("variety.bettacraft.bronze_cory.wild", "Wild type");
        a.accept("variety.bettacraft.bronze_cory.albino", "Albino");
        a.accept("variety.bettacraft.bronze_cory.gold_laser", "Gold Laser");
        a.accept("info.bettacraft.tankmate.bronze_cory", "Bronze Cory (Corydoras aeneus) from the rivers east of the Andes, from Colombia to the Rio de la Plata.\n"
                + "Metallic green-bronze flanks, a pale belly; the most common cory of the trade and one of the hardiest. Albino and Gold Laser (an orange-gold stripe along the back) are recessive bred forms.\n"
                + "Shoal of 6 or more, 20–28 °C, water: hardy. Counts 0.6 fish in the stocking.\nGlues its eggs to the glass (after a cool water change).\n"
                + "Catch: aquarium net in rivers and jungle water, wandering trader.");
        a.accept("entity.bettacraft.panda_garra", "Panda Garra");
        a.accept("item.bettacraft.panda_garra_bucket", "Bucket of Panda Garra");
        a.accept("item.bettacraft.panda_garra_spawn_egg", "Panda Garra Spawn Egg");
        a.accept("variety.bettacraft.panda_garra.wild", "Wild type");
        a.accept("info.bettacraft.tankmate.panda_garra", "Panda Garra (Garra flavatra) from the clear hill streams of the Rakhine Yoma in Myanmar.\n"
                + "Small and slim, golden yellow with broad black bands, a sucker mouth; busy all day on stones and glass.\n"
                + "In a group of 5 or more, 20–26 °C, water: sensitive (likes oxygen and current). Counts 0.6 fish in the stocking.\n"
                + "Grazes algae from stones, glass and decoration. Egg scatterer.\nCatch: aquarium net in rivers, wandering trader.");
        a.accept("advancements.bettacraft.tetra_cory_form.title", "Gold, Albino, Diamond");
        a.accept("advancements.bettacraft.tetra_cory_form.description", "A bred form of a tetra or a cory you bred yourself grows up");
        q(a, "tetra_cory_form", "Bred Forms of Tetras and Corys",
                "Gold and albino neons, gold cardinals, albino glowlights, albino and longfin corys: the forms are recessive. Wild fish carry them hidden now and then; pair the young with each other.");
        q(a, "bottom_fish", "Bronze Cory and Panda Garra",
                "The hardy bronze cory (also albino and gold laser) and the banded panda garra, a small algae grazer for cool, clear water.");
        partB(a);
    }

    /** Part b: the new reef animals, the copepods, the clownfish larvae and their food chain. */
    private static void partB(BiConsumer<String, String> a) {
        a.accept("entity.bettacraft.mandarinfish", "Mandarinfish");
        a.accept("item.bettacraft.mandarinfish_bucket", "Bucket of Mandarinfish");
        a.accept("item.bettacraft.mandarinfish_spawn_egg", "Mandarinfish Spawn Egg");
        a.accept("variety.bettacraft.mandarinfish.wild", "Wild type");
        a.accept("info.bettacraft.tankmate.mandarinfish", "Mandarinfish (Synchiropus splendidus) from the lagoons and reef flats of the western Pacific.\n"
                + "A small dragonet with blue and orange wavy lines; hovers slowly over the rock and picks copepods all day.\n"
                + "Alone, 24–28 °C, sea water, water: sensitive. Counts 0.6 fish in the stocking.\n"
                + "Takes no prepared or live food at all: it lives on the copepods of the live rock. Without enough live rock, or with many other small reef fish picking them too, it starves.\n"
                + "Does not breed in a tank. Catch: aquarium net in warm oceans, wandering trader.");
        a.accept("entity.bettacraft.banggai_cardinal", "Banggai Cardinalfish");
        a.accept("item.bettacraft.banggai_cardinal_bucket", "Bucket of Banggai Cardinalfish");
        a.accept("item.bettacraft.banggai_cardinal_spawn_egg", "Banggai Cardinalfish Spawn Egg");
        a.accept("variety.bettacraft.banggai_cardinal.wild", "Wild type");
        a.accept("info.bettacraft.tankmate.banggai_cardinal", "Banggai Cardinalfish (Pterapogon kauderni) from the shallow bays of the Banggai islands (Indonesia).\n"
                + "Silver with three black bars, long fins with white dots; calm and slow.\n"
                + "As a pair, 24–28 °C, sea water, water: normal. Counts 0.8 fish in the stocking.\n"
                + "Mouthbrooder: the female passes her eggs to the male, who carries them in his mouth; the young come out fully formed (no fry food needed). The easy reef fish to breed.\n"
                + "Catch: aquarium net in warm oceans, wandering trader.");
        a.accept("entity.bettacraft.lyretail_anthias", "Lyretail Anthias");
        a.accept("item.bettacraft.lyretail_anthias_bucket", "Bucket of Lyretail Anthias");
        a.accept("item.bettacraft.lyretail_anthias_spawn_egg", "Lyretail Anthias Spawn Egg");
        a.accept("variety.bettacraft.lyretail_anthias.wild", "Wild type");
        a.accept("info.bettacraft.tankmate.lyretail_anthias", "Lyretail Anthias (Pseudanthias squamipinnis) from the reef slopes of the Indo-Pacific and the Red Sea.\n"
                + "A fairy basslet: the female orange-gold, the male magenta with a long first dorsal ray and a lyre tail.\n"
                + "Shoal of 5 or more, 23–28 °C, sea water, water: sensitive. Counts 0.7 fish in the stocking.\n"
                + "Picks plankton from the open water and is hungry again after a third of a day: feed it often (an automatic feeder helps).\n"
                + "Does not breed in a tank. Catch: aquarium net in warm and lukewarm oceans, wandering trader.");
        a.accept("tankmate.bettacraft.group.dragonet", "Dragonets");
        a.accept("tankmate.bettacraft.group.cardinalfish", "Cardinalfish");
        a.accept("tankmate.bettacraft.group.anthias", "Anthias");
        a.accept("tankmate.bettacraft.breeding.mouth_male", "Mouthbrooder: the male carries the eggs in his mouth, the young come out fully formed");
        a.accept("critter.bettacraft.fire_shrimp", "Fire Shrimp");
        // the food chain of the clownfish larvae
        a.accept("item.bettacraft.phytoplankton_culture", "Phytoplankton Culture");
        a.accept("item.bettacraft.phytoplankton", "Bottle of Phytoplankton");
        a.accept("item.bettacraft.rotifer_culture", "Rotifer Culture");
        a.accept("item.bettacraft.rotifers", "Bottle of Rotifers");
        a.accept("info.bettacraft.phytoplankton_culture", "Salt water with a sea plant and bone meal: turns into a bottle of phytoplankton after one in-game day, but only in light (level 9 or more).");
        a.accept("info.bettacraft.phytoplankton", "Microalgae in salt water. Used on a reef tank it greens the water and keeps the rotifers in it alive (they eat it). The food of a rotifer culture.");
        a.accept("info.bettacraft.rotifer_culture", "Rotifers on phytoplankton: turns into a bottle of rotifers after one in-game day. Start one from phytoplankton and infusoria, then keep it going: a bottle of rotifers and two of phytoplankton make two cultures.");
        a.accept("info.bettacraft.rotifers", "Tiny live food for the smallest fry. In a reef tank they stay in the water (with phytoplankton) and are the first food of the clownfish larvae; without them only a few larvae survive.");
        a.accept("log.bettacraft.reef.phytoplankton", "Phytoplankton dosed (the rotifers live on it)");
        a.accept("log.bettacraft.reef.plankton_fed", "%s went into the water for the larvae");
        a.accept("log.bettacraft.reef.larvae_hungry", "The clownfish larvae are starving: they need rotifers (later also artemia nauplii)");
        a.accept("log.bettacraft.reef.larvae_died", "The %s larvae died (no rotifers in the water)");
        a.accept("log.bettacraft.reef.copepods_short", "Too few copepods for the mandarin: more live rock, fewer competing fish");
        a.accept("log.bettacraft.tm.clown_larvae_raised", "%2$s %1$s larvae raised on rotifers settled as young fish");
        a.accept("log.bettacraft.tm.banggai_eggs", "%s passed her eggs to %s, who carries them in his mouth");
        a.accept("log.bettacraft.tm.banggai_born", "%s released %s young from his mouth");
        a.accept("message.bettacraft.reef.phytoplankton_salt", "Phytoplankton only lives in salt water");
        a.accept("message.bettacraft.reef.phytoplankton_full", "The water is green enough");
        a.accept("gui.bettacraft.reef.copepods", " · copepods %s%%");
        a.accept("gui.bettacraft.reef.plankton", " · rotifers %s · phytoplankton %s");
        a.accept("bettacraft.configuration.copepodsPerLiveRock", "Copepods per live rock");
        a.accept("bettacraft.configuration.copepodsPerLiveRock.tooltip", "Copepods (food portions) the live rock holds at most in salt water, per tank block of live rock; they grow back by themselves");
        a.accept("bettacraft.configuration.mandarinCopepodsPerDay", "Copepods of a mandarin per day");
        a.accept("bettacraft.configuration.mandarinCopepodsPerDay.tooltip", "Copepod portions a grown mandarin eats per day (it takes no other food)");
        a.accept("bettacraft.configuration.clownLarvaDays", "Clownfish larva days");
        a.accept("bettacraft.configuration.clownLarvaDays.tooltip", "Days the clownfish larvae drift after hatching (they need rotifers, later also artemia nauplii)");
        a.accept("bettacraft.configuration.clownLarvaSurvival", "Clownfish larvae surviving without food");
        a.accept("bettacraft.configuration.clownLarvaSurvival.tooltip", "Share of the clownfish larvae that survive the larva days without any food in the tank (fed about 80 %)");
        a.accept("advancements.bettacraft.mandarin_copepods.title", "Living Rock");
        a.accept("advancements.bettacraft.mandarin_copepods.description", "A mandarinfish lives on the copepods of your live rock");
        a.accept("advancements.bettacraft.clown_larvae.title", "Rotifer Rearing");
        a.accept("advancements.bettacraft.clown_larvae.description", "Clownfish larvae raised on rotifers settle as young fish");
        a.accept("advancements.bettacraft.banggai_young.title", "A Mouthful of Young");
        a.accept("advancements.bettacraft.banggai_young.description", "A Banggai cardinalfish male releases his young from his mouth");
        q(a, "mandarin", "Living Rock",
                "The mandarinfish eats only copepods: they live in the live rock. Build plenty of live rock, let the tank mature and keep few other small reef fish.");
        q(a, "banggai", "Banggai Cardinalfish",
                "The easy reef fish to breed: a pair, and the male carries the eggs in his mouth until the young come out fully formed.");
        q(a, "clown_larvae", "Rotifer Rearing",
                "Clownfish larvae need rotifers in the water in their first days. Grow phytoplankton in light, start a rotifer culture with infusoria and feed the rotifers to the tank.");
    }

    private static void q(BiConsumer<String, String> a, String name, String title, String desc) {
        a.accept("quest.bettacraft." + name + ".title", title);
        a.accept("quest.bettacraft." + name + ".desc", desc);
    }
}
