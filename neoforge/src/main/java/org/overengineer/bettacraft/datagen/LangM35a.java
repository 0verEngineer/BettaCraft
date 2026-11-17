package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M35 part a English texts (land and marsh animals of other mods in terraria and paludaria, the sea turtle as a big animal,
 * the terrarium box for foreign animals). The German ones are in de_de.json; edit both.
 */
final class LangM35a {
    private LangM35a() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("message.bettacraft.big.too_little_land", "Not enough land for %1$s: the big land animals need %2$s blocks of land together, this tank has %3$s");
        a.accept("message.bettacraft.big.too_little_room", "Not enough room for %1$s: the big animals need %2$s blocks of tank together, this tank holds %3$s");
        a.accept("message.bettacraft.sea_turtle.no_beach", "%s needs a beach: sand or soil on the land of the paludarium for its eggs");
        a.accept("message.bettacraft.box.refused", "The terrarium box does not take %s (no terrarium profile, not on terrarium.boxAllow)");
        a.accept("message.bettacraft.box.too_big", "%s is too big for the terrarium box: the large net catches it into a transport tub");
        a.accept("message.bettacraft.box.not_yours", "%s belongs to someone else");
        a.accept("message.bettacraft.box.need_box", "%s goes into an empty terrarium box: have one in your inventory");
        a.accept("item.bettacraft.terrarium_box.filled", "Terrarium Box with %s");
        a.accept("tooltip.bettacraft.terrarium_box", "Catches land animals: our tortoises, frogs, geckos and snakes, and the land animals of other mods with a terrarium profile");
        a.accept("tooltip.bettacraft.terrarium_box.filled", "Right-click a terrarium or paludarium to put it in, or the ground to let it go");
        a.accept("log.bettacraft.foreign.gravid", "%s carries eggs and looks for a nest");
        a.accept("log.bettacraft.foreign.hatched", "%2$s young %1$s hatched from the nest");
        a.accept("gui.bettacraft.foreign.blocked.climate", "the climate of the tank harms it");

        a.accept("bettacraft.configuration.boxAllow", "Terrarium box: more animals");
        a.accept("bettacraft.configuration.boxAllow.tooltip", "M35: land animals of other mods without a terrarium profile that the terrarium box catches all the same (\"modid:name\", \"modid:*\"); they live in terraria and paludaria without our care, moved by our walker, small ones only (inhabitants.maxHitbox)");
    }
}
