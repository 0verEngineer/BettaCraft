package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M49 English texts of part b (the brackish water animals: archerfish, mudskipper, Amano shrimp, the mangroves) and the
 * advancements and quests of both parts. German: de_de.json.
 */
final class LangM49b {
    private LangM49b() {}

    static void addAll(BiConsumer<String, String> a) {
        // config
        a.accept("bettacraft.configuration.brackish", "Brackish water");
        a.accept("bettacraft.configuration.archerfishInsectsPerDay", "Insects for the archerfish per day");
        a.accept("bettacraft.configuration.archerfishInsectsPerDay.tooltip", "Insects per day that come to the plants above an open tank with an archerfish (the plants of a paludarium, a mangrove over the water, leaves or flowers on top of the tank) by day");
        a.accept("bettacraft.configuration.archerfishSplashChance", "Archerfish splashes players");
        a.accept("bettacraft.configuration.archerfishSplashChance.tooltip", "Chance per second that an archerfish splashes a player whose head is over its open tank (harmless)");
        a.accept("bettacraft.configuration.amanoLarvaDays", "Larva days of the Amano shrimp");
        a.accept("bettacraft.configuration.amanoLarvaDays.tooltip", "Days the Amano larvae drift in brackish water before they settle as young shrimp");
        a.accept("bettacraft.configuration.amanoFreshLarvaDays", "Amano larvae in fresh water (days)");
        a.accept("bettacraft.configuration.amanoFreshLarvaDays.tooltip", "Days the Amano larvae live in fresh water (time to net them into a brackish tank)");
        a.accept("bettacraft.configuration.amanoLarvaSurvival", "Survival of the Amano larvae");
        a.accept("bettacraft.configuration.amanoLarvaSurvival.tooltip", "Share of the Amano larvae that come through in brackish water without phytoplankton in the tank (with phytoplankton about twice as many, at most 80 %)");
        a.accept("bettacraft.configuration.mangroveStageDays", "Days per mangrove growth stage");
        a.accept("bettacraft.configuration.mangroveStageDays.tooltip", "Days between two growth stages of a mangrove in a tank interior (in light; brackish water a third faster)");
        // the archerfish
        a.accept("entity.bettacraft.archerfish", "Archerfish");
        a.accept("item.bettacraft.archerfish_bucket", "Bucket of Archerfish");
        a.accept("item.bettacraft.archerfish_spawn_egg", "Archerfish Spawn Egg");
        a.accept("tankmate.bettacraft.group.archerfish", "Archerfish");
        a.accept("info.bettacraft.tankmate.archerfish", "Banded archerfish (Toxotes jaculatrix) from the mangroves and river mouths from India to northern Australia.\n"
                + "Silver-white with four or five black wedge bars, big eyes, a pointed head, the dorsal fin far back; about 12 cm in a tank.\n"
                + "Small group of 3 or more, 24–30 °C, fresh to brackish water (best brackish). Counts 2 fish in the stocking. Swims under the surface.\n"
                + "Shoots insects off the leaves above the water with a jet of water: fruit flies let out over an open tank, insects at the plants above it (a paludarium, a mangrove crown). Now and then it splashes a player who leans over the tank.\n"
                + "Does not breed in a tank. Catch: aquarium net in mangrove swamps, wandering trader.");
        a.accept("message.bettacraft.archerfish.splash", "Splash! An archerfish took you for an insect");
        a.accept("log.bettacraft.archerfish.flies", "%s fruit flies let out over the water for the archerfish");
        // the mudskipper
        a.accept("entity.bettacraft.mudskipper", "Mudskipper");
        a.accept("item.bettacraft.mudskipper_box", "Mudskipper in a Terrarium Box");
        a.accept("item.bettacraft.mudskipper_spawn_egg", "Mudskipper Spawn Egg");
                a.accept("gui.bettacraft.terrarium.problem.fresh_water", "needs brackish water");
        // the mangroves
        a.accept("item.bettacraft.decor_mangrove_stilt_roots", "Mangrove Stilt Roots");
        a.accept("tooltip.bettacraft.decor_mangrove_stilt_roots", "Tall: stands out of the water of a paludarium; mudskippers climb on it");
        a.accept("message.bettacraft.mangrove.planted", "The propagule takes root on the floor of the tank");
        a.accept("message.bettacraft.mangrove.off", "Mangroves are switched off (features.brackish.mangroves)");
        a.accept("message.bettacraft.mangrove.full", "There are as many mangroves in this tank as it has room for");
        a.accept("message.bettacraft.mangrove.no_interior", "The interior of this tank is not ready yet");
        a.accept("message.bettacraft.mangrove.crowded", "Too close to another mangrove");
        a.accept("message.bettacraft.mangrove.no_room", "No room on the floor here");
        a.accept("log.bettacraft.mangrove.planted", "%s planted a mangrove propagule");
        a.accept("log.bettacraft.mangrove.crown", "A mangrove grew its crown over the water");
        // the Amano shrimp
        a.accept("critter.bettacraft.amano_shrimp", "Amano Shrimp");
                a.accept("log.bettacraft.amano.larvae_died", "%s Amano larvae died (they need brackish water)");
        a.accept("log.bettacraft.amano.young", "%s of %s Amano larvae settled as young shrimp");
        a.accept("log.bettacraft.amano.none_settled", "None of %2$s Amano larvae came through");
        a.accept("log.bettacraft.amano.larvae_out", "%s netted out %s Amano larvae");
        a.accept("log.bettacraft.amano.larvae_in", "%s let in %s Amano larvae");
        a.accept("message.bettacraft.amano.larvae_out", "%s Amano larvae in the bag: into a brackish tank with them");
        a.accept("message.bettacraft.amano.larvae_in", "%s Amano larvae drift in the brackish water");
        a.accept("message.bettacraft.amano.larvae_in_fresh", "%s Amano larvae in fresh water: they will not live long");
        a.accept("message.bettacraft.amano.larvae_need_tank", "The larvae go into a tank (brackish water)");
        // advancements
        a.accept("advancements.bettacraft.sump_installed.title", "Below Deck");
        a.accept("advancements.bettacraft.sump_installed.description", "Put a sump into a cabinet under a tank");
        a.accept("advancements.bettacraft.calcium_reactor.title", "Dissolved Coral");
        a.accept("advancements.bettacraft.calcium_reactor.description", "A calcium reactor dissolves coral rubble with CO2 into a reef tank");
        a.accept("advancements.bettacraft.archerfish_shot.title", "Sharpshooter");
        a.accept("advancements.bettacraft.archerfish_shot.description", "An archerfish shoots an insect off the leaves above its tank");
        a.accept("advancements.bettacraft.archerfish_splash.title", "Mistaken for a Fly");
        a.accept("advancements.bettacraft.archerfish_splash.description", "Get splashed by an archerfish while leaning over its tank");
        a.accept("advancements.bettacraft.mudskipper_paludarium.title", "Fish Out of Water");
        a.accept("advancements.bettacraft.mudskipper_paludarium.description", "Keep a mudskipper in a brackish paludarium");
        a.accept("advancements.bettacraft.mangrove_grown.title", "Roots in the Brine");
        a.accept("advancements.bettacraft.mangrove_grown.description", "A mangrove in a tank grows its crown over the water");
        a.accept("advancements.bettacraft.amano_young.title", "Back from the Sea");
        a.accept("advancements.bettacraft.amano_young.description", "Raise Amano larvae in brackish water until they settle as young shrimp");
        // quests
        q(a, "sump", "The Sump", "A sump is a second, small tank in the cabinet under the aquarium: its water counts with the tank, so the values change more slowly. Its chambers take the heaters, a protein skimmer and filter media out of the tank.");
        q(a, "refugium", "Refugium", "Put Chaetomorpha and a tank light into the refugium chamber of the sump: the algae take up nitrate, starve the algae on the glass and grow. Harvest the extra ones.");
        q(a, "calcium_reactor", "Calcium Reactor", "The calcium reactor dissolves coral rubble with CO2 from a gas cylinder and brings calcium and alkalinity back for the stony corals: the other way besides the dosing pump.");
        q(a, "archerfish", "Sharpshooter", "The archerfish shoots insects above the water with a jet of water. Keep the tank open and let fruit flies out over it, or plant something above the water.");
        q(a, "mudskipper", "Fish on Land", "The mudskipper lives on the mud of the mangroves: a paludarium with land and brackish water, warm, humid air and roots to climb.");
        q(a, "mangrove", "Mangrove", "Plant a mangrove propagule in a tank: it grows stilt roots and a crown over the water. Brackish water and light make it grow faster.");
        q(a, "amano", "Back from the Sea", "Amano shrimp mate in fresh water, but their larvae need brackish water. Net the cloud of larvae into a fish bag, let it out into a brackish tank with phytoplankton and wait a few days.");
    }

    private static void q(BiConsumer<String, String> a, String name, String title, String desc) {
        a.accept("quest.bettacraft." + name + ".title", title);
        a.accept("quest.bettacraft." + name + ".desc", desc);
    }
}
