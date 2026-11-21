package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M32 part d English texts (modpack events, alarm waypoints, the FTB Quests chapter of {@code modpack/}). The German
 * ones are in de_de.json; edit both. The quest keys are used by {@code tools/modpack/ftbquests_chapter.py}.
 */
final class LangM32d {
    private LangM32d() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("bettacraft.configuration.map", "Map mods");
        a.accept("bettacraft.configuration.showAlarmWaypoints", "Alarm waypoints of all tanks");
        a.accept("bettacraft.configuration.showAlarmWaypoints.tooltip", "A waypoint on your map at a tank of yours (or one you are a user of) whose monitor alarm went off or whose hired aquarist warns, until it clears. Off: none from any tank. Each tank also has its own switch in the users tab of its menu; the pack switch features.compat.journeymap turns them off for everyone");

        a.accept("waypoint.bettacraft.alarm", "Tank alarm: %s (%s)");
        a.accept("waypoint.bettacraft.tank", "Tank");

        // the FTB Quests chapter (modpack/config/ftbquests/quests/chapters/bettacraft.snbt)
        a.accept("quest.bettacraft.chapter.title", "Bettacraft");
        a.accept("quest.bettacraft.chapter.subtitle", "From the first tank to the reef and the terrarium");
        q(a, "first_tank", "Glass House", "Place tank blocks: one block is a small aquarium, blocks next to or on top of each other join into a bigger one, in any connected shape. The Bettacraft handbook explains everything step by step.");
        q(a, "fill_tank", "Water Works", "Fill the tank at least halfway with water buckets or pipes. Animals only go into a tank with enough water.");
        q(a, "equipment", "The Basics", "A heater keeps the water warm, a filter keeps it clean and a light lets plants grow and shows the day. Put them on the outside of the glass.");
        q(a, "catch_betta", "Labyrinth Fish", "Wild bettas live in swamps. Catch one with a water bucket or a fish bag.");
        q(a, "insert_betta", "Moving In", "Right-click the tank with the betta to put it in. Two adult males in one tank will fight!");
        q(a, "pellets", "Fish Flakes", "Craft betta pellets and feed by right-clicking the tank. The tank menu shows who is hungry.");
        q(a, "feeder", "Room Service", "An automatic feeder on the tank feeds the fish while you are away.");
        q(a, "test_kits", "Test Your Water", "The pH test kit and the TDS meter show the water values. Bettas like soft, slightly acid water.");
        q(a, "water_change", "Fresh Start", "Waste builds up in every tank. Pour fresh water into a tank with used water to change it.");
        q(a, "tropical", "Tropical", "Keep a tank with bettas at 26 to 28 °C. The tank menu shows the temperature.");
        q(a, "external_filter", "Plumbing", "An external filter with good media keeps even a crowded tank clean.");
        q(a, "reverse_osmosis", "Pure Water", "The reverse osmosis unit makes soft water for demanding fish, shrimp and the reef.");
        q(a, "nest", "Bubble Architect", "A healthy, well-fed male in warm, calm water builds a bubble nest. Floating plants help.");
        q(a, "spawned", "Clutch", "Put a ready female in with him: the pair spawns into the nest. Take her out afterwards.");
        q(a, "fry_food", "Microscopic Menu", "The fry are tiny: culture infusoria or hatch artemia nauplii for them.");
        q(a, "fry_free", "Free Swimming", "Raise the larvae until the fry swim free. Remove the father in time.");
        q(a, "grown_up", "All Grown Up", "Raise a tank-bred betta to adulthood.");
        q(a, "variety", "Designer Fish", "Breed a named variety such as Koi Galaxy, Samurai or Nemo. The magnifier shows the genes.");
        q(a, "dynasty", "Dynasty", "Raise a betta of the fifth generation.");
        q(a, "interior", "Honey, I Shrunk Myself", "Sneak and right-click a tank to step into its interior, a world of its own at 16 blocks per tank block.");
        q(a, "substrate", "Groundwork", "Lay substrate on the interior floor: sneak and right-click it with sand, gravel or soil.");
        q(a, "plants", "Green Thumb", "Plant aquarium plants in the substrate. Plants take up waste and give the fish cover.");
        q(a, "co2", "Fizz", "Plants need CO2: a bio CO2 bottle with yeast beside the tank makes them grow faster, a CO2 cylinder even more. Too much CO2 harms the fish, so watch the values.");
        q(a, "sea_water", "Salt Water", "Mix sea salt into reverse osmosis water, or bring sea water from the ocean, for a reef tank.");
        q(a, "skimmer", "Clean Reef", "A protein skimmer and live rock keep salt water clean.");
        q(a, "corals", "Polyps", "Corals grow on rock in a reef tank. Green star polyps are an easy start.");
        q(a, "dosing", "Reef Chemistry", "Stony corals use calcium and carbonate. A dosing pump keeps the reef values steady.");
        q(a, "big_tank", "Public Aquarium", "Build a tank of at least 64 blocks. Big tanks hold sharks, rays and food fish.");
        q(a, "food_maker", "Food Factory", "The food maker turns fish, grain and kelp into betta pellets by the dozen. Wind it up, or power it.");
        q(a, "monitor", "Control Room", "Monitors show the values of the tanks around them. An alarm tile warns you, with a redstone signal and a waypoint on the map.");
        q(a, "terrarium", "Dry Land", "Every tank can be a terrarium or paludarium: build land above the water line in its interior, or leave the water out. Land animals travel in a terrarium box.");
        q(a, "terrarium_gear", "Desert and Rainforest", "A basking lamp gives warmth and UV, a mister humidity, and the thermo-hygrometer shows both.");
        q(a, "gecko", "Leopard Gecko", "Move a leopard gecko into a warm, dry terrarium.");
    }

    private static void q(BiConsumer<String, String> a, String name, String title, String desc) {
        a.accept("quest.bettacraft." + name + ".title", title);
        a.accept("quest.bettacraft." + name + ".desc", desc);
    }
}
