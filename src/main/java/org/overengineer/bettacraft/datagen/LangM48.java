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
    }

    private static void q(BiConsumer<String, String> a, String name, String title, String desc) {
        a.accept("quest.bettacraft." + name + ".title", title);
        a.accept("quest.bettacraft." + name + ".desc", desc);
    }
}
