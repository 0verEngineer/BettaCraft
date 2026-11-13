package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/** M21.5 English texts (polish milestone): feature switches, green neon, snail colours, break protection, clip-on feeder. de_de.json holds the German ones. */
final class LangM215 {
    private LangM215() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("critter.bettacraft.yellow_nerite", "Yellow Racer Nerite");
        a.accept("critter.bettacraft.look.orange", "orange");
        a.accept("critter.bettacraft.look.red", "red");
        a.accept("critter.bettacraft.look.deep_red", "deep red");
        a.accept("critter.bettacraft.look.red_lines", "red with yellow lines");
        a.accept("critter.bettacraft.look.yellow", "yellow");
        a.accept("critter.bettacraft.look.golden", "golden yellow");
        a.accept("critter.bettacraft.look.yellow_lines", "yellow with dark lines");
        a.accept("variety.bettacraft.ramshorn.red", "Red");
        a.accept("bettacraft.configuration.smoothSwimming", "Smooth swimming");
        a.accept("bettacraft.configuration.smoothSwimming.tooltip", "Smooth swimming: fish turn in curves, tilt and right themselves smoothly, kuhli loaches undulate slowly like eels and hide by day, plecos and otocinclus only stand upright once they reach the glass (off: the old, jerkier movement)");
        a.accept("bettacraft.configuration.greenNeonTetra", "Green neon tetra");
        a.accept("bettacraft.configuration.greenNeonTetra.tooltip", "Green neon tetra (Paracheirodon simulans, blue and green form) at the wandering trader and in the aquarium net (off: no new ones; fish already caught stay)");
        a.accept("bettacraft.configuration.snailColors", "Snail colours");
        a.accept("bettacraft.configuration.snailColors.tooltip", "Snail colours: every nerite gets its own random shade (red and yellow racer nerites), apple snails are mostly gold with shades and bands, ramshorn snails also come in red (off: one fixed colour per variety, brown apple snails, no new red ramshorns)");
        a.accept("bettacraft.configuration.detailedDecor", "Detailed skull and shipwreck");
        a.accept("bettacraft.configuration.detailedDecor.tooltip", "Bigger, more detailed dino skull and shipwreck decoration (off: the small old shapes for newly placed ones)");
        a.accept("bettacraft.configuration.breakProtection", "Break protection");
        a.accept("bettacraft.configuration.breakProtection.tooltip", "Break protection switch in the tank menu: nobody can break a protected tank, its lid, light, feeders and the cabinet below (off: the switch is hidden and does nothing)");
        a.accept("bettacraft.configuration.clipOnFeeder", "Clip-on feeder");
        a.accept("bettacraft.configuration.clipOnFeeder.tooltip", "Clip-on feeder: a small automatic feeder that hangs on the side of the glass (for tanks whose top is taken by a light) (off: it cannot be placed and placed ones do not feed)");
    }
}
