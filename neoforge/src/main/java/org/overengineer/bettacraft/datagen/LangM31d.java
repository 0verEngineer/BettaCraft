package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M31 part d English texts (mobs in glass: the mob jar, bait, breeding, the config). The German ones are in de_de.json;
 * edit both.
 */
final class LangM31d {
    private LangM31d() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("bettacraft.configuration.mobCatchHealth", "Mob jar: health at most");
        a.accept("bettacraft.configuration.mobCatchHealth.tooltip", "Share of its health a mob may have at most to go into the mob jar (0.25 = weakened to a quarter)");
        a.accept("bettacraft.configuration.mobUndeadBurn", "Undead burn without a lid");
        a.accept("bettacraft.configuration.mobUndeadBurn.tooltip", "Undead mobs in glass (zombies, skeletons ...) burn in daylight in a tank without a lid (off: the glass protects them)");
        a.accept("bettacraft.configuration.mobBreeding", "Mobs in glass breed");
        a.accept("bettacraft.configuration.mobBreeding.tooltip", "Mobs in glass breed: two fed drowned, zombies or husks (rotten flesh) a baby, spiders (crickets) spiderlings, small slimes and magma cubes (slime ball, magma cream) a new small one; bred mobs drop nothing either");

        a.accept("item.bettacraft.mob_jar", "Mob Jar");
        a.accept("tooltip.bettacraft.mob_jar.empty", "Right-click a mob weakened below %s %% of its health");
        a.accept("tooltip.bettacraft.mob_jar.health", "Health: %s");
        a.accept("gui.bettacraft.terrarium.habitat.wet", "an aquarium or a paludarium");
        a.accept("gui.bettacraft.terrarium.habitat.all", "any tank");

        a.accept("message.bettacraft.glass.not_allowed", "%s does not go into glass");
        a.accept("message.bettacraft.glass.too_strong", "%1$s is too strong: weaken it below %2$s %% of its health first");
        a.accept("message.bettacraft.glass.need_jar", "%s needs an empty mob jar");
        a.accept("message.bettacraft.glass.bait", "%s laid as bait: it lures mobs into the open tank for a day");
        a.accept("message.bettacraft.glass.bait_lid", "Bait only lures through an open top: take a lid off");
        a.accept("log.bettacraft.glass.fed", "%1$s ate %2$s");
        a.accept("log.bettacraft.glass.born", "A young %s was born");
        a.accept("log.bettacraft.glass.grown", "%s has grown up");
        a.accept("log.bettacraft.glass.converted", "A %1$s under water became a %2$s");
        a.accept("log.bettacraft.glass.bait", "%s laid as bait");
        a.accept("log.bettacraft.glass.lured", "%s was lured in by the bait");
    }
}
