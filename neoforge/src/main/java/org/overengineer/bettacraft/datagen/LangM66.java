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
        a.accept("gui.bettacraft.filter.energy_tooltip", "The filter keeps no energy of its own. What flows into it goes into the store of the tank it is attached to, and all the devices of that tank draw from there.");
        a.accept("gui.bettacraft.filter.energy_off", "No device needs power at the moment (config).");
        // ---- feeders
        a.accept("gui.bettacraft.feeder.cabinet_hint", "Also takes food from a cabinet under the tank.");
        // ---- multiblock filter
        a.accept("gui.bettacraft.mb_filter.tanks_header", "Serves %s tanks:");
        a.accept("gui.bettacraft.mb_filter.tap", "tap water, unlimited");
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
        a.accept("gui.bettacraft.aquarist.tasks", "Tasks (the name: on or off, the number: level)");
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
    }
}
