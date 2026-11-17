package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M31 part b English texts (the turtles: species, breeding, brumation, wild spawns). The German ones are in de_de.json;
 * edit both.
 */
final class LangM31b {
    private LangM31b() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("bettacraft.configuration.brumation", "Brumation");
        a.accept("bettacraft.configuration.brumation.tooltip", "Hermann's tortoises brumate (rest without food or harm) while the air of their tank is below brumationBelow (off: the cold harms them like the other turtles)");
        a.accept("bettacraft.configuration.brumationBelow", "Brumation below");
        a.accept("bettacraft.configuration.brumationBelow.tooltip", "Air temperature (°C) below which a tortoise brumates (it wakes up 2 K above; frost below 2 °C still harms)");
        a.accept("bettacraft.configuration.incubationSpeed", "Incubation speed");
        a.accept("bettacraft.configuration.incubationSpeed.tooltip", "Speed of the incubation of turtle eggs (2 = twice as fast; at the pivotal temperature a clutch takes 3-4 in-game days)");
        a.accept("bettacraft.configuration.clutchIntervalDays", "Days between clutches");
        a.accept("bettacraft.configuration.clutchIntervalDays.tooltip", "In-game days a female turtle waits after laying before she lays again");
        a.accept("entity.bettacraft.slider_turtle", "Pond Slider");
        a.accept("entity.bettacraft.slider_turtle.elegans", "Red-eared Slider");
        a.accept("entity.bettacraft.slider_turtle.scripta", "Yellow-bellied Slider");
        a.accept("entity.bettacraft.hermann_tortoise", "Hermann's Tortoise");
        a.accept("entity.bettacraft.reeves_turtle", "Reeves' Turtle");
        a.accept("item.bettacraft.slider_turtle_bucket", "Bucket of Pond Slider");
        a.accept("item.bettacraft.reeves_turtle_bucket", "Bucket of Reeves' Turtle");
        a.accept("item.bettacraft.hermann_tortoise_box", "Boxed Hermann's Tortoise");
        a.accept("item.bettacraft.terrarium_box", "Terrarium Box");
        a.accept("item.bettacraft.slider_turtle_spawn_egg", "Pond Slider Spawn Egg");
        a.accept("item.bettacraft.hermann_tortoise_spawn_egg", "Hermann's Tortoise Spawn Egg");
        a.accept("item.bettacraft.reeves_turtle_spawn_egg", "Reeves' Turtle Spawn Egg");
        a.accept("block.bettacraft.turtle_eggs", "Reptile Eggs"); // part c: the geckos and the snake lay them too
        a.accept("turtle.bettacraft.variety.elegans", "Red-eared (T. s. elegans)");
        a.accept("turtle.bettacraft.variety.scripta", "Yellow-bellied (T. s. scripta)");
        a.accept("turtle.bettacraft.variety.wild", "Wild form");
        a.accept("tooltip.bettacraft.turtle.young", "Young (sex not visible yet)");
        a.accept("tooltip.bettacraft.turtle.female", "Female");
        a.accept("tooltip.bettacraft.turtle.male", "Male");
        a.accept("tooltip.bettacraft.turtle.brumating", "In brumation");
        a.accept("gui.bettacraft.terrarium.problem.too_small", "tank too small for a grown one");
        a.accept("gui.bettacraft.terrarium.brumating", "in brumation (no food, no harm)");
        a.accept("gui.bettacraft.terrarium.eggs", "Eggs: %1$s × %2$s, %3$s %% · nest %4$s °C → %5$s");
        a.accept("gui.bettacraft.terrarium.eggs.females", "mostly females");
        a.accept("gui.bettacraft.terrarium.eggs.males", "mostly males");
        a.accept("gui.bettacraft.terrarium.eggs.mixed", "both sexes");
        a.accept("gui.bettacraft.terrarium.eggs.open", "sex not decided yet");
        a.accept("log.bettacraft.turtle.brumation_start", "%s went into brumation (the air is cold)");
        a.accept("log.bettacraft.turtle.brumation_end", "%s woke up from brumation");
        a.accept("log.bettacraft.turtle.laid", "%1$s laid %2$s eggs");
        a.accept("log.bettacraft.turtle.too_crowded", "%s does not lay: the hatchlings would crowd the tank");
        a.accept("log.bettacraft.turtle.no_nest", "%s finds no nest: she needs sand or soil on the land");
        a.accept("log.bettacraft.turtle.eggs_gone", "Eggs of the %s were destroyed");
        a.accept("log.bettacraft.turtle.eggs_drowned", "Eggs of the %s drowned (water over the nest)");
        a.accept("log.bettacraft.turtle.eggs_cooked", "Eggs of the %s died of heat (nest above 36 °C)");
        a.accept("log.bettacraft.turtle.hatched", "%2$s %1$s hatched: %3$s females, %4$s males");
        a.accept("log.bettacraft.turtle.hatch_crowded", "%s eggs did not hatch: no room in the tank");
        a.accept("log.bettacraft.turtle.greens", "%1$s ate %2$s");
        a.accept("message.bettacraft.turtle.greens", "%s eats the greens");
    }
}
