package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M66 English texts (the UI overhaul: tank menu tabs, cabinet and filter energy, feeders, aquarist tabs, pipe tooltip, multiblock
 * filter). The German ones are in de_de.json; edit both.
 */
final class LangM66 {
    private LangM66() {}

    static void addAll(BiConsumer<String, String> a) {
        // ---- the tank menu
        a.accept("gui.bettacraft.section.air", "Aeration");
        a.accept("gui.bettacraft.section.light", "Light");
        a.accept("gui.bettacraft.water.power_tip", "The equipment that needs power and whether it has it:");
        a.accept("gui.bettacraft.plants.uptake_tooltip", "Floating plants: +%s %% per day\nPlants in the tank interior: +%s %% per day");
        a.accept("gui.bettacraft.breeding.conditions", "Breeding conditions:");
        // ---- cabinet and external filter
        a.accept("gui.bettacraft.filter.energy", "Energy: %s / %s FE");
        a.accept("gui.bettacraft.filter.energy_none", "Energy: not attached to a tank");
        a.accept("gui.bettacraft.filter.energy_tooltip", "Takes power (by cable, or the crank right here) and passes it to the one store of the tank it is attached to; the bar shows that store. All the devices of that tank draw from there.");
        a.accept("gui.bettacraft.filter.energy_off", "No device needs power at the moment (config).");
        // ---- feeders
        a.accept("gui.bettacraft.feeder.cabinet_hint", "Also takes food from a cabinet under the tank.");
        // ---- multiblock filter
        a.accept("gui.bettacraft.mb_filter.tanks_header", "Serves %s tanks:");
        // ---- pipes (Jade)
        a.accept("jade.bettacraft.pipe.rate", "Carries up to %s mB/s");
        // ---- the aquarist
        a.accept("gui.bettacraft.aquarist.tab.work", "Work");
        a.accept("gui.bettacraft.aquarist.tab.rearing", "Rearing tanks");
        a.accept("gui.bettacraft.aquarist.tab.tanks", "Tanks");
        a.accept("gui.bettacraft.aquarist.assign", "Assign...");
        a.accept("gui.bettacraft.aquarist.assign.bed.tooltip", "Then right-click the bed he should sleep in (within %s blocks of his breeding basin).");
        a.accept("gui.bettacraft.aquarist.assign.chest.tooltip", "Then right-click the chest or barrel for his wage, his food and his cuttings (within %s blocks of his breeding basin).");
        a.accept("gui.bettacraft.aquarist.auto", "Auto");
        a.accept("gui.bettacraft.aquarist.auto.bed.tooltip", "Gives back his bed; he looks for a free one himself.");
        a.accept("gui.bettacraft.aquarist.auto.chest.tooltip", "He looks in the containers next to his breeding basin again.");
        a.accept("gui.bettacraft.aquarist.chest_assigned", "Chest at %s (assigned)");
        a.accept("gui.bettacraft.aquarist.chest_auto", "Chest: the containers next to the basin");
        a.accept("gui.bettacraft.aquarist.tasks", "Tasks");
        a.accept("gui.bettacraft.aquarist.rearing.title", "Young animals go into the first of these rearing tanks that takes them (species, salt and room):");
        a.accept("gui.bettacraft.aquarist.rearing.none", "No rearing tank yet: choose one below and press the check mark.");
        a.accept("gui.bettacraft.aquarist.rearing.not_his", "(he may not work there)");
        a.accept("gui.bettacraft.aquarist.rearing.remove", "Take this tank out of the list");
        a.accept("gui.bettacraft.aquarist.rearing.species_all", "All species");
        a.accept("gui.bettacraft.aquarist.rearing.species_n", "%s species");
        a.accept("gui.bettacraft.aquarist.rearing.species_tooltip", "Click to choose which species this tank takes.");
        a.accept("gui.bettacraft.aquarist.rearing.add", "Add a rearing tank:");
        a.accept("gui.bettacraft.aquarist.rearing.add_tooltip", "Puts the tank shown at the end of his rearing tanks.");
        a.accept("gui.bettacraft.aquarist.rearing.no_candidate", "No further tank he may work at");
        a.accept("gui.bettacraft.aquarist.species.title", "Species for %s");
        a.accept("gui.bettacraft.aquarist.species.hint", "Click to take a species in or leave it out. Nothing ticked: every species.");
        a.accept("gui.bettacraft.aquarist.species.all", "All species");
        a.accept("gui.bettacraft.aquarist.species.done", "Done");
        a.accept("gui.bettacraft.aquarist.tanks.some_off", "%s of %s tasks");
        a.accept("gui.bettacraft.aquarist.tanks.select", "Click a tank to choose what he does there.");
        a.accept("gui.bettacraft.aquarist.tanks.at", "At %s he:");
        a.accept("gui.bettacraft.aquarist.no_tanks_no_workplace", "No workplace, so he looks after no tanks");
        a.accept("message.bettacraft.aquarist.assign_chest", "Right-click the chest he should use (within %s blocks of his breeding basin).");
        a.accept("message.bettacraft.aquarist.assign_bed", "Right-click the bed he should sleep in (within %s blocks of his breeding basin).");
        a.accept("message.bettacraft.aquarist.assigned_chest", "That is his chest now.");
        a.accept("message.bettacraft.aquarist.assigned_bed", "That is his bed now.");
        a.accept("message.bettacraft.aquarist.assign_far", "Too far from his workplace (at most %s blocks).");
        a.accept("message.bettacraft.aquarist.assign_no_bed", "That is no bed.");
        a.accept("message.bettacraft.aquarist.assign_bed_taken", "Somebody else sleeps in that bed.");
        a.accept("message.bettacraft.aquarist.assign_no_chest", "That is no chest: it needs an inventory.");
        a.accept("message.bettacraft.aquarist.assign_basin", "Right-click the free breeding basin he should work at (within %s blocks of where he is).");
        a.accept("message.bettacraft.aquarist.assigned_basin", "That is his breeding basin now.");
        a.accept("message.bettacraft.aquarist.assign_no_basin", "That is no breeding basin.");
        a.accept("message.bettacraft.aquarist.assign_basin_taken", "Somebody else works at that breeding basin.");
        a.accept("gui.bettacraft.aquarist.assign.basin.tooltip", "Then right-click the free breeding basin he should work at (within %s blocks).");
        a.accept("gui.bettacraft.aquarist.auto.basin.tooltip", "Gives his basin back; he looks for another free one himself.");
        a.accept("message.bettacraft.aquarist.rearing_max", "At most %s rearing tanks.");
        a.accept("gui.bettacraft.section.values", "Values");
        a.accept("gui.bettacraft.section.quality", "Quality");
        a.accept("gui.bettacraft.section.filter", "Filter");
        a.accept("gui.bettacraft.section.temperature", "Temperature");
        a.accept("gui.bettacraft.section.plants_floating", "Floating plants");
        a.accept("gui.bettacraft.section.plants_water", "Water plants (interior)");
        a.accept("gui.bettacraft.section.plants_corals", "Corals");
        a.accept("gui.bettacraft.section.plants_land", "Terrarium plants");
        a.accept("gui.bettacraft.water.quality_tip", "Water quality: the levels");
        a.accept("gui.bettacraft.water.quality_tip.clean", "clean: from %s %%");
        a.accept("gui.bettacraft.water.quality_tip.good", "good: from %s %%");
        a.accept("gui.bettacraft.water.quality_tip.poor", "poor: from %s %%");
        a.accept("gui.bettacraft.water.quality_tip.bad", "bad: below %s %% (stress for the animals)");
        a.accept("gui.bettacraft.water.filters_tip.capacity", "The filters can clean %s water blocks (the tank has %s).");
        a.accept("gui.bettacraft.water.filters_tip.share", "So they remove %s %% of the waste (at most %s %%).");
        a.accept("gui.bettacraft.water.filters_tip.more", "More filter capacity helps until the whole tank is covered.");
        a.accept("gui.bettacraft.devices.motors_none", "No motor filter");
        a.accept("gui.bettacraft.devices.motor.inside", "Inside filters: %1$s (%2$s with %3$s)");
        a.accept("gui.bettacraft.devices.motor.corner", "Corner filters: %1$s (%2$s with %3$s)");
        a.accept("gui.bettacraft.devices.motor.outside", "External filters: %1$s (%2$s with %3$s)");
        a.accept("gui.bettacraft.devices.motor.cabinet", "Cabinet filters: %1$s (%2$s with %3$s)");
        a.accept("gui.bettacraft.devices.fry_tip", "To fit the %1$s: hold it in your hand and right-click the inside, corner or external filter (inside and corner filters stand in the tank interior). A filter in a cabinet: right-click the cabinet.");
        a.accept("gui.bettacraft.plants.hint_both", "Out: hand, net · trim: shears");
        a.accept("gui.bettacraft.plants.group_count", "Plants: %s");
        a.accept("gui.bettacraft.plants.fx.quality", "Water quality: +%s %% per day");
        a.accept("gui.bettacraft.plants.fx.nitrate", "Nitrate: -%s mg/L per day");
        a.accept("gui.bettacraft.plants.fx.oxygen", "Oxygen: +%s mg/L per day (in light)");
        a.accept("gui.bettacraft.plants.fx.co2", "CO2: takes up %s mg/L per day");
        a.accept("gui.bettacraft.plants.fx.none", "No effect on the water at the moment");
        a.accept("gui.bettacraft.plants.fx.land", "They grow by the climate and do not clean the water.");
        a.accept("gui.bettacraft.plants.list", "Plants of the interior by species (%s)");
        a.accept("gui.bettacraft.critters.take_bucket_tooltip", "One animal into a bucket (the best grade; with Shift the worst; with Ctrl a full bucket at once)");
        a.accept("gui.bettacraft.breeding.group", "Group: %s (%s/%s)");
        a.accept("gui.bettacraft.breeding.group_tooltip", "Click: the next group of animals (only the groups that are in the tank)");
        a.accept("gui.bettacraft.breeding.group.bettas", "Bettas");
        a.accept("gui.bettacraft.breeding.group.tankmates", "Tankmates");
        a.accept("gui.bettacraft.breeding.group.small", "Small animals");
        a.accept("gui.bettacraft.breeding.group.frogs", "Frogs");
        a.accept("gui.bettacraft.breeding.group.herps", "Reptiles and amphibians");
        a.accept("gui.bettacraft.breeding.group.foreign", "Other mods");
        a.accept("gui.bettacraft.breeding.g.header", "%s: ♂ %s, ♀ %s, young %s");
        a.accept("gui.bettacraft.breeding.c.pair", "A grown male and female");
        a.accept("gui.bettacraft.breeding.c.pair_one", "A grown animal (no mate needed)");
        a.accept("gui.bettacraft.breeding.c.pair_two", "Two grown animals");
        a.accept("gui.bettacraft.breeding.c.temp", "Temperature %s-%s °C (now %s)");
        a.accept("gui.bettacraft.breeding.c.water", "Water quality at least %s %%");
        a.accept("gui.bettacraft.breeding.c.room", "Room in the tank (%s of %s animals)");
        a.accept("gui.bettacraft.breeding.c.air", "Air above the water (for the clutch)");
        a.accept("gui.bettacraft.breeding.c.fresh", "Fresh water (they mate there)");
        a.accept("gui.bettacraft.breeding.c.brackish", "Brackish or salt water (the larvae grow up there)");
        a.accept("gui.bettacraft.breeding.c.ready", "A fed, healthy, rested male and female");
        a.accept("gui.bettacraft.breeding.c.frog_air", "Air to breathe at the surface");
        a.accept("gui.bettacraft.breeding.c.cool", "A cool water change within the last day");
        a.accept("gui.bettacraft.breeding.c.tadpole_room", "Room for tadpoles (%s of %s)");
        a.accept("gui.bettacraft.breeding.c.off", "Breeding is switched off (config)");
        a.accept("gui.bettacraft.breeding.c.never", "Does not breed in a tank");
        a.accept("gui.bettacraft.breeding.c.climate", "Climate without harm and little stress");
        a.accept("gui.bettacraft.breeding.c.fed", "Fed within two days");
        a.accept("gui.bettacraft.breeding.c.rest", "Female ready to lay (grown, rested)");
        a.accept("gui.bettacraft.breeding.c.nest", "A nest spot on the land (sand, soil or moss)");
        a.accept("gui.bettacraft.breeding.c.herp_room", "Room in the tank for the hatchlings");
        a.accept("gui.bettacraft.breeding.c.ready_two", "Two fed, healthy, grown animals");
        a.accept("gui.bettacraft.breeding.c.f_free", "Water, tank and surroundings fit");
        a.accept("gui.bettacraft.breeding.c.f_none", "This animal does not breed here");
        a.accept("gui.bettacraft.breeding.c.f_dry", "Water in the tank");
        a.accept("gui.bettacraft.breeding.c.f_temperature", "Temperature in its breeding range");
        a.accept("gui.bettacraft.breeding.c.f_tank_size", "The tank is big enough");
        a.accept("gui.bettacraft.breeding.c.f_crowded", "The tank is not overcrowded");
        a.accept("gui.bettacraft.breeding.c.f_water", "Water quality good enough");
        a.accept("gui.bettacraft.breeding.c.f_climate", "A climate that suits it");
        a.accept("gui.bettacraft.breeding.c.f_plants", "Plants for the eggs");
        a.accept("gui.bettacraft.breeding.c.f_need", "Needs: %s");
        a.accept("gui.bettacraft.breeding.s.berried", "%s berried females");
        a.accept("gui.bettacraft.breeding.s.clutches", "%s clutches on the glass");
        a.accept("gui.bettacraft.breeding.s.larvae", "%s larvae in %s clouds");
        a.accept("gui.bettacraft.breeding.s.amplexus", "%s pairs in amplexus");
        a.accept("gui.bettacraft.breeding.s.spawn", "%s eggs of spawn in %s clumps");
        a.accept("gui.bettacraft.breeding.s.tadpoles", "%s tadpoles");
        a.accept("gui.bettacraft.breeding.s.clutch", "%s eggs in %s clutches: %s %% (nest %s °C)");
        a.accept("gui.bettacraft.breeding.s.brood", "Brood: %s %% done");
        a.accept("gui.bettacraft.breeding.care_tip", "The care over the egg phase (the average):");
        a.accept("gui.bettacraft.breeding.care_tip.temp.0", "Temperature ideal (50 % of the care)");
        a.accept("gui.bettacraft.breeding.care_tip.temp.1", "Temperature only tolerable: half of those 50 %");
        a.accept("gui.bettacraft.breeding.care_tip.temp.2", "Temperature too extreme: none of those 50 %");
        a.accept("gui.bettacraft.breeding.care_tip.crowded", "Tank not overcrowded (25 %)");
        a.accept("gui.bettacraft.breeding.care_tip.fed", "Father not hungry (25 %)");
        a.accept("gui.bettacraft.breeding.care_tip.survival", "Expected: about %4$s of %5$s larvae swim free (%3$s %% survive now; between %1$s and %2$s %%)");
        a.accept("jade.bettacraft.tank.title_quality", "%s %% (%s)");
        a.accept("gui.bettacraft.filter.energy_tooltip.cabinet", "Takes power (by cable, a Create shaft, or the crank right here) and passes it to the one store of the tank above; the bar shows that store. Every cabinet under the tank makes the store bigger, and all the devices of the tank draw from it.");
        a.accept("gui.bettacraft.mb_filter.link_tip", "Cleans %1$s water blocks of this tank (tank: %2$s) · share %3$s %% of the filter's power · %4$s mB/t flow");
        a.accept("gui.bettacraft.mb_filter.link_tip_off", "Does not clean this tank: %s");
        a.accept("log.bettacraft.fry_intake.tadpole", "A tadpole");
        a.accept("config.jade.plugin_bettacraft.pipe", "Bettacraft water pipe");
    }
}
