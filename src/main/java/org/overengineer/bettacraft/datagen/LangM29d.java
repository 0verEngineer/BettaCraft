package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M29 part d English texts (big crayfish, axolotl care). Generated together with the German ones in de_de.json; edit both.
 */
final class LangM29d {
    private LangM29d() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("critter.bettacraft.florida_crayfish", "Florida Crayfish");
        a.accept("critter.bettacraft.marbled_crayfish", "Marbled Crayfish");
        a.accept("variety.bettacraft.florida_crayfish.wild", "Wild Form");
        a.accept("variety.bettacraft.florida_crayfish.blue", "Blue");
        a.accept("variety.bettacraft.florida_crayfish.orange", "Orange");
        a.accept("variety.bettacraft.marbled_crayfish.wild", "Marbled");
        a.accept("cause.bettacraft.critter.big_crayfish", "big crayfish");
        a.accept("log.bettacraft.critter_big_crayfish", "A big crayfish caught a small animal (%s)");
        a.accept("log.bettacraft.big_crayfish_uprooted", "A big crayfish dug up a young plant (%s)");
        a.accept("log.bettacraft.big_crayfish_plant", "A big crayfish ate at a plant (%s)");
        a.accept("log.bettacraft.big_crayfish_ate", "A big crayfish ate %s");
        a.accept("log.bettacraft.big_crayfish_pinched", "A big crayfish pinched %s with its claws");
        a.accept("gui.bettacraft.critters.breeds.partheno", "Females only: eggs without a male");
        a.accept("log.bettacraft.axolotl_warm", "Too warm for the axolotls (%s °C): they want 15–20 °C (a cooler), above 23 °C they lose health");
        a.accept("log.bettacraft.axolotl_no_plants", "The axolotls want to spawn but find no plants for their eggs");
        a.accept("log.bettacraft.axolotl_eggs", "%s laid eggs on a plant");
        a.accept("log.bettacraft.axolotl_hatched", "%s axolotl larvae hatched");
        a.accept("message.bettacraft.magnifier.axolotl", "%s: %s, colour genes %s");
        a.accept("variety.bettacraft.axolotl.lucy", "Leucistic (pink)");
        a.accept("variety.bettacraft.axolotl.wild", "Wild (brown)");
        a.accept("variety.bettacraft.axolotl.gold", "Gold");
        a.accept("variety.bettacraft.axolotl.cyan", "Cyan");
        a.accept("variety.bettacraft.axolotl.blue", "Blue");
        a.accept("bettacraft.configuration.bigCrayfishPreyPerDay", "Small animals a big crayfish catches");
        a.accept("bettacraft.configuration.bigCrayfishPreyPerDay.tooltip", "Small animals (shrimp, small snails, dwarf crayfish) one big crayfish (Florida, marbled) catches per day; plants, moss and shelters hide them (0 = never)");
        a.accept("bettacraft.configuration.bigCrayfishFishPerDay", "Fish a big crayfish goes for");
        a.accept("bettacraft.configuration.bigCrayfishFishPerDay.tooltip", "Fish one big crayfish goes for per day: it eats fry and young fish and small bottom fish, and pinches bettas and other slow fish resting near the floor (plants hide them; 0 = never)");
        a.accept("bettacraft.configuration.bigCrayfishPlantsPerDay", "Plants a big crayfish damages");
        a.accept("bettacraft.configuration.bigCrayfishPlantsPerDay.tooltip", "Plants one big crayfish damages per day: it eats a grown plant back by a stage or digs a young rooted plant out of the substrate (epiphytes and moss on wood and stone are safe; 0 = never)");
        a.accept("bettacraft.configuration.axolotlBreeding", "Axolotl breeding");
        a.accept("bettacraft.configuration.axolotlBreeding.tooltip", "Breeding speed of vanilla axolotls in a tank: a pair in cool water (15-20 °C) with plants lays eggs on the plants (1 = normal, 0 = they never breed there)");
        a.accept("bettacraft.configuration.axolotlEggDays", "Axolotl egg days");
        a.accept("bettacraft.configuration.axolotlEggDays.tooltip", "Days the axolotl eggs on the plants need to hatch into larvae (baby axolotls)");
        a.accept("bettacraft.configuration.axolotlHeatDamage", "Axolotl heat damage");
        a.accept("bettacraft.configuration.axolotlHeatDamage.tooltip", "Damage (hit points per day) an axolotl takes per degree above 23 °C; from 21 °C it is stressed and does not breed (0 = warm water only stops the breeding)");
        a.accept("bettacraft.configuration.bigCrayfish", "Big crayfish");
        a.accept("bettacraft.configuration.bigCrayfish.tooltip", "Big crayfish: the Florida crayfish (wild, blue, orange) and the marbled crayfish (only females, multiplies on its own) at the wandering trader, in the aquarium net and wild; they eat plants, dig them up and catch slow fish and small animals (off: no new ones; those in tanks stay)");
        a.accept("bettacraft.configuration.axolotlCare", "Axolotl care");
        a.accept("bettacraft.configuration.axolotlCare.tooltip", "Axolotl care: vanilla axolotls in a tank want cool water (15-20 °C, a cooler), suffer in warm water and breed there: eggs on the plants, larvae as baby axolotls, the five colours inherited by Mendel (blue recessive and rare; off: axolotls in tanks are left to vanilla)");
    }
}
