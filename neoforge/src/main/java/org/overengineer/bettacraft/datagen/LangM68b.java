package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M68 part B English texts (the water change controller with four pipe ports, the level sensor that only measures, the drain,
 * the waste water intake that sucks in young animals). The German ones are in de_de.json; edit both.
 */
final class LangM68b {
    private LangM68b() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("block.bettacraft.drain", "Drain");
        a.accept("info.bettacraft.drain", "The end of a waste water line: a pipe joins it on every side and it discards all the water that comes down the pipe. Put it at the waste water outlet of a water change controller (right, seen from the front), right at the port or at the end of a pipe. It holds nothing; other mods' pipes may use it as a sink too.");
        a.accept("log.bettacraft.water_change_intake", "%s was sucked into the water change");
        a.accept("message.bettacraft.water_changer.started", "Water change started: %s mB so far, the rest follows as its pipes carry it");
        a.accept("gui.bettacraft.water_changer.status.changing", "Changing the water: %s mB to go");
        a.accept("gui.bettacraft.water_changer.status.topped", "Topped off: %s mB");
        a.accept("gui.bettacraft.water_changer.status.no_drain", "The old water has nowhere to go: lead its waste water outlet (right) to a drain, a reservoir or another tank, and the waste water inlet (top) to the tank");
        a.accept("gui.bettacraft.water_changer.status.salty", "Top-off: only salty water at the fresh water line (fresh water only)");
        a.accept("gui.bettacraft.water_changer.status.short_circuit", "Short circuit: one of its lines touches ports of two roles (pipe wrench!)");
        a.accept("gui.bettacraft.water_changer.tanks", "Serves %s tanks (%s through a multiblock filter) · %s level sensors");
        a.accept("gui.bettacraft.water_changer.stores", "Its stores: fresh water %s mB, waste water %s mB · topped off %s mB in all");
        a.accept("gui.bettacraft.water_changer.port.fresh_in", "Fresh in");
        a.accept("gui.bettacraft.water_changer.port.fresh_out", "fresh out");
        a.accept("gui.bettacraft.water_changer.port.waste_in", "waste in");
        a.accept("gui.bettacraft.water_changer.port.waste_out", "waste out");
        a.accept("gui.bettacraft.water_changer.port.touch", "touch");
        a.accept("gui.bettacraft.water_changer.warn.short", "Short circuit: a line touches ports of two roles");
        a.accept("gui.bettacraft.water_changer.warn.no_fresh", "No fresh water line at its inlet (left)");
        a.accept("gui.bettacraft.water_changer.warn.no_drain", "The old water has nowhere to go: no line or drain at its outlet (right)");
        a.accept("gui.bettacraft.water_changer.warn.young", "Sucks in young animals: no intake guard where it drains a tank");
        a.accept("gui.bettacraft.level_sensor.status.low", "below the target");
        a.accept("jade.bettacraft.sensor.target", "Target %s %%: %s");
        a.accept("jade.bettacraft.sensor.missing", "%s mB below the target");
        a.accept("jade.bettacraft.sensor.signal_on", "Redstone signal: on");
        a.accept("jade.bettacraft.sensor.signal_off", "Redstone signal: off");
        a.accept("jade.bettacraft.sensor.served", "A water change controller tops this tank off");
        a.accept("jade.bettacraft.sensor.unserved", "No water change controller tops it off (it only measures)");
        a.accept("jade.bettacraft.drain", "Discards the water of the pipe that ends here");
        a.accept("jade.bettacraft.drain.off", "Switched off (features.pipes.drain)");
        a.accept("jade.bettacraft.pipe.role.fresh_in", "Fresh water line (into a water change controller)");
        a.accept("jade.bettacraft.pipe.role.fresh_out", "Fresh water line (from the controller to the tanks)");
        a.accept("jade.bettacraft.pipe.role.waste_in", "Waste water line (from the tanks to the controller)");
        a.accept("jade.bettacraft.pipe.role.waste_out", "Waste water line (from the controller to a drain)");
        a.accept("gui.bettacraft.water.changer", "Water change every %s days, %s %% · next in %s days");
        a.accept("gui.bettacraft.water.changer_status", "Water change: %s");
        a.accept("gui.bettacraft.water.changer_more", "and %s more controllers");
        a.accept("gui.bettacraft.water.sensor", "Level sensor %s %%: %s · no controller tops off");
        a.accept("gui.bettacraft.water.sensor_served", "Level sensor %s %%: %s · topped off by the controller");
        a.accept("gui.bettacraft.devices.change_young", "The water change sucks in young animals (%s without an intake guard)");
        a.accept("gui.bettacraft.devices.change_young_tip", "Where a water change controller drains this tank (its waste water line, or the controller against the glass) is a suction point: put an intake guard or the intake strainer behind its connection in the interior, or use the intake guard on the glass there.");
    }
}
