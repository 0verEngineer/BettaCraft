package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/** M21.5 English texts (polish milestone): feature switches, green neon, snail colours, break protection, clip-on feeder. de_de.json holds the German ones. */
final class LangM215 {
    private LangM215() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("block.bettacraft.clip_feeder", "Clip-on Feeder");
        a.accept("info.bettacraft.clip_feeder", "A small automatic feeder that hangs on the side of the glass, at the top edge of a tank (for a tank whose top is taken by the light, e.g. one block high).\nPlace it on a side face of a top tank block. Right-click with food: fills it (one stack); empty hand: 1, 2 or 3 feedings a day; sneak + empty hand: takes the food out.\nNo hopper; it does not cover the water against evaporation. The big feeder on top of the tank comes first if there is one.");
        a.accept("message.bettacraft.clip_feeder.status", "Clip-on feeder: %s food, %sx a day");
        a.accept("message.bettacraft.clip_feeder.full", "The clip-on feeder is full (one stack)");
        a.accept("message.bettacraft.clip_feeder.where", "The clip-on feeder hangs on a side face of a top tank block");
        a.accept("message.bettacraft.clip_feeder.off", "Clip-on feeders are switched off (features.clipOnFeeder)");
        a.accept("gui.bettacraft.tank.protect_on", "Protected");
        a.accept("gui.bettacraft.tank.protect_off", "Unprotected");
        a.accept("gui.bettacraft.tank.protect_tooltip", "Break protection (owner or OP): while it is on nobody can break the tank, not even the owner or in creative mode: glass, lid, light, feeders and the cabinet below. Explosions and pistons leave it alone too (server config).");
        a.accept("message.bettacraft.tank.break_protected", "Break protection is on: switch it off in the tank menu first");
        a.accept("message.bettacraft.tank.break_protection_off", "Break protection is switched off on this server (features.breakProtection)");
        a.accept("log.bettacraft.protection_on", "%s switched the break protection on");
        a.accept("log.bettacraft.protection_off", "%s switched the break protection off");
        a.accept("bettacraft.configuration.protectFromExplosionsAndPistons", "Protection against explosions and pistons");
        a.accept("bettacraft.configuration.protectFromExplosionsAndPistons.tooltip", "Break protection (switch in the tank menu) also stops explosions and pistons (false: it only stops players breaking the tank)");
        a.accept("entity.bettacraft.green_neon_tetra", "Green Neon Tetra");
        a.accept("item.bettacraft.green_neon_tetra_bucket", "Bucket of Green Neon Tetra");
        a.accept("item.bettacraft.green_neon_tetra_spawn_egg", "Green Neon Tetra Spawn Egg");
        a.accept("variety.bettacraft.green_neon_tetra.wild", "Green Neon (wild type)");
        a.accept("variety.bettacraft.green_neon_tetra.blue", "Blue Neon");
        a.accept("info.bettacraft.tankmate.green_neon_tetra", "Green Neon Tetra (Paracheirodon simulans), also sold as Blue Neon, from the blackwater of the upper Rio Negro and Orinoco (Brazil, Colombia, Venezuela).\nSmaller than the neon; the stripe runs over almost the whole body, green or (Blue Neon) blue, with hardly any red. Blue is recessive.\nShoal of 6 or more, 24–30 °C, water: very sensitive. Counts 0.25 fish in the stocking.\nScatters its eggs (fine plants, spawning mop).\nCatch: aquarium net in jungle water, wandering trader.");
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
