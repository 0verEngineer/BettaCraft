package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/** M33.1 English texts (the tank type switch). The German ones are in de_de.json; edit both. */
final class LangM331 {
    private LangM331() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("gui.bettacraft.tank_mode.auto", "Automatic");
        a.accept("gui.bettacraft.tank_mode.aquarium", "Aquarium");
        a.accept("gui.bettacraft.tank_mode.paludarium", "Paludarium");
        a.accept("gui.bettacraft.tank_mode.terrarium", "Terrarium");
        a.accept("gui.bettacraft.tank_mode.button", "Type: %s");
        a.accept("gui.bettacraft.tank_mode.tooltip.auto", "Tank type automatic: the tank is what its water and land make it (now: %s). Water comes in as long as it fits.");
        a.accept("gui.bettacraft.tank_mode.tooltip.aquarium", "Tank type aquarium (fixed): the tank stays an aquarium whatever land it has (now: %s). Water comes in as long as it fits.");
        a.accept("gui.bettacraft.tank_mode.tooltip.paludarium", "Tank type paludarium (fixed): the tank stays a paludarium (now: %s). Water only comes in up to the height where the land part stays big enough.");
        a.accept("gui.bettacraft.tank_mode.tooltip.terrarium", "Tank type terrarium (fixed): the tank stays a terrarium (now: %s). No water comes in: no bucket, pipe, pump, top-off or other mod.");
        a.accept("gui.bettacraft.tank_mode.tooltip_cycle", "Click: next type, Shift-click: the one before. Water already in the tank is never removed; draining always works.");
        a.accept("gui.bettacraft.tank_mode.limit", "Water at most %s mB (the land part must stay)");
        a.accept("gui.bettacraft.tank_mode.too_much_water", "More water than this tank type allows: none comes in, drain some");
        a.accept("message.bettacraft.tank_mode.terrarium_no_water", "Terrarium: no water (the tank type is set in the tank menu)");
        a.accept("message.bettacraft.tank_mode.paludarium_limit", "Paludarium: water only up to %s blocks high (%s mB), the land part must stay");
        a.accept("message.bettacraft.tank_mode.protected", "You may not change this tank here");
        a.accept("message.bettacraft.tank_mode.set", "Tank type: %s");
        a.accept("message.bettacraft.tank_mode.water_stays_terrarium", "Terrarium set: the water stays, but no more comes in (drain it with a bucket or a pump)");
        a.accept("message.bettacraft.tank_mode.water_stays_paludarium", "Paludarium set: the water above %s mB stays, but no more comes in");
        a.accept("log.bettacraft.tank_mode.set", "%s set the tank type: %s");
        a.accept("jade.bettacraft.tank.mode_fixed", "%s (fixed)");
    }
}
