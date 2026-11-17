package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/** M41 English texts (Create's rotation for the tank buffer: the drive adapter and the shaft on the cabinet). The German ones are in de_de.json; edit both. */
final class LangM41 {
    private LangM41() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("block.bettacraft.drive_adapter", "Drive Adapter");
        a.accept("info.bettacraft.drive_adapter", "Only with Create. Set it on a tank wall or under a tank (its plate on the tank, the shaft away from it) and connect a Create shaft, cogwheel or motor to its shaft: the rotation charges the tank's FE buffer, so every device of the tank that takes the tank's FE runs on it (filters, flow pumps and skimmers, heaters, coolers and heat mats, lights, air pumps, misters).\n64 RPM give the full power (256 FE/t), slower proportionally. Several adapters on one tank add up to a limit (1024 FE/t).\nIt costs real stress: the more the devices need (and while the buffer fills), the more SU; turning without load costs a little. An overstressed network gives the tank nothing.\nThe cabinet under a tank takes a shaft too: set its side in the cabinet menu.");
        a.accept("goggles.bettacraft.drive_adapter", "Tank drive:");
        a.accept("goggles.bettacraft.drive_adapter.charging", "Charges the tank: %s FE/t");
        a.accept("goggles.bettacraft.drive_adapter.overstressed", "Overstressed: the tank gets nothing");
        a.accept("goggles.bettacraft.drive_adapter.idle", "The tank needs no FE right now");
        a.accept("goggles.bettacraft.drive_adapter.no_tank", "No tank at its back: set it on a tank wall or under a tank");
        a.accept("gui.bettacraft.cabinet.drive", "Create shaft: %s");
        a.accept("gui.bettacraft.cabinet.drive.tooltip", "Click: next side, sneak + click: back. A Create shaft, cogwheel or gearbox on this side charges the FE buffer of the tank above (it costs stress); with winding on, what the tank does not need winds the cabinet's spring");
        a.accept("gui.bettacraft.cabinet.drive.not_allowed", "This server does not allow the shaft on this side (createDrive.cabinetSides)");
        String[][] sides = {{"none", "none", ""}, {"back", "back", "B"}, {"left", "left", "L"}, {"right", "right", "R"}, {"front", "front", "F"}, {"bottom", "bottom", "D"}};
        for (String[] s : sides) {
            a.accept("gui.bettacraft.cabinet.drive." + s[0], s[1]);
            if (!s[2].isEmpty()) a.accept("gui.bettacraft.cabinet.drive." + s[0] + ".short", s[2]);
        }
        a.accept("jade.bettacraft.cabinet.drive", "Create shaft (%s): %s FE/t to the tank");
        a.accept("jade.bettacraft.cabinet.drive_none", "Create shaft (%s): nothing connected");
        a.accept("jade.bettacraft.cabinet.drive_overstressed", "Create shaft (%s): overstressed, no power");
        a.accept("jade.bettacraft.tank.drive", "Create: %s FE/t from %s inputs");
        a.accept("jade.bettacraft.tank.drive_overstressed", "Create: overstressed, no power (%s inputs)");
        a.accept("bettacraft.configuration.createDrive", "Create drive (tank buffer)");
        a.accept("bettacraft.configuration.fullSpeedRpm", "Full speed (RPM)");
        a.accept("bettacraft.configuration.fullSpeedRpm.tooltip", "RPM at which one input gives its full power (slower proportionally, faster no more)");
        a.accept("bettacraft.configuration.fePerTickPerInput", "FE per tick per input");
        a.accept("bettacraft.configuration.fePerTickPerInput.tooltip", "FE per tick one input gives at full speed");
        a.accept("bettacraft.configuration.maxFePerTickPerTank", "Limit per tank (FE/t)");
        a.accept("bettacraft.configuration.maxFePerTickPerTank.tooltip", "FE per tick all inputs of one tank give together at most (several adapters add up to this)");
        a.accept("bettacraft.configuration.stressPerFe", "Stress per FE/t");
        a.accept("bettacraft.configuration.stressPerFe.tooltip", "Stress units (SU) an input costs for every FE per tick it gives (2: 128 FE/t cost 256 SU)");
        a.accept("bettacraft.configuration.idleStressPerRpm", "Idle stress per RPM");
        a.accept("bettacraft.configuration.idleStressPerRpm.tooltip", "Stress units per RPM an input costs while it turns without load (0.5: 32 SU at 64 RPM)");
        a.accept("bettacraft.configuration.fePerSpringTick", "FE per spring tick");
        a.accept("bettacraft.configuration.fePerSpringTick.tooltip", "With power.winding on, a cabinet's shaft winds the cabinet's spring with the FE the tank does not need: this many FE wind one device-tick");
        a.accept("bettacraft.configuration.cabinetSides", "Cabinet sides for the shaft");
        a.accept("bettacraft.configuration.cabinetSides.tooltip", "Sides of a cabinet that may take the shaft (back, left, right, front, bottom; seen from the cabinet's door). Empty: no cabinet takes a shaft");
    }
}
