package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/** M44 English texts (cabinet fronts and joints, the walk animation in tanks). The German ones are in de_de.json; edit both. */
final class LangM44 {
    private LangM44() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("gui.bettacraft.cabinet.front", "Front: %s");
        a.accept("gui.bettacraft.cabinet.front.tooltip", "Auto: cabinets side by side make one front (drawers at both ends, double doors between them, a single door in the middle of an odd row). The others fix the front of this cabinet. Click: next, sneak + click: previous. Broken, the cabinet keeps the setting.");
        String[][] fronts = {
                {"auto", "Auto (from the row)", "Auto"},
                {"pair_l", "double door, left leaf", "Pair left"},
                {"pair_r", "double door, right leaf", "Pair right"},
                {"single", "single door", "Single"},
                {"drawers", "drawers", "Drawers"},
                {"two", "two doors (old)", "Two doors"}};
        for (String[] f : fronts) {
            a.accept("gui.bettacraft.cabinet.front." + f[0], f[1]);
            a.accept("gui.bettacraft.cabinet.front." + f[0] + ".short", f[2]);
        }
        a.accept("gui.bettacraft.cabinet.joints", "Joints");
        a.accept("gui.bettacraft.cabinet.joints.on", "Joints: on");
        a.accept("gui.bettacraft.cabinet.joints.off", "Joints: off");
        a.accept("gui.bettacraft.cabinet.joints.tooltip", "Thin dark joints between the doors and drawers of this cabinet (off: planks only, like before). Click: switch.");
        a.accept("tooltip.bettacraft.cabinet.no_joints", "Front without joints");
        a.accept("bettacraft.configuration.walkAnimationBoost", "Walk animation in tanks");
        a.accept("bettacraft.configuration.walkAnimationBoost.tooltip", "Leg movement of mobs and animals walking in tanks and terraria: they walk slowly there and the mobs in glass are shrunk, so vanilla barely moves their legs (they glide). auto = matched to their size (1.5 for shrunk mobs, 3 for animals in real size), a number from 0.1 to 10 = that factor for all, 0 = like vanilla");
    }
}
