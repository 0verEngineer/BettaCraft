package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M31 part c English texts (the amphibians and reptiles: species, varieties, their food, eggs and tadpoles, wild spawns).
 * The German ones are in de_de.json; edit both.
 */
final class LangM31c {
    private LangM31c() {}

    static void addAll(BiConsumer<String, String> a) {

        a.accept("entity.bettacraft.dart_frog", "Poison Dart Frog");
        a.accept("entity.bettacraft.dart_frog.tinctorius", "Dyeing Poison Dart Frog");
        a.accept("entity.bettacraft.dart_frog.auratus", "Green-and-black Poison Dart Frog");
        a.accept("entity.bettacraft.dart_frog.leucomelas", "Yellow-banded Poison Dart Frog");
        a.accept("entity.bettacraft.horned_frog", "Cranwell's Horned Frog");
        a.accept("entity.bettacraft.horned_frog.green", "Cranwell's Horned Frog");
        a.accept("entity.bettacraft.horned_frog.brown", "Cranwell's Horned Frog");
        a.accept("entity.bettacraft.leopard_gecko", "Leopard Gecko");
        a.accept("entity.bettacraft.leopard_gecko.normal", "Leopard Gecko");
        a.accept("entity.bettacraft.leopard_gecko.tangerine", "Tangerine Leopard Gecko");
        a.accept("entity.bettacraft.leopard_gecko.albino", "Albino Leopard Gecko");
        a.accept("entity.bettacraft.crested_gecko", "Crested Gecko");
        a.accept("entity.bettacraft.crested_gecko.tiger", "Crested Gecko");
        a.accept("entity.bettacraft.crested_gecko.flame", "Flame Crested Gecko");
        a.accept("entity.bettacraft.crested_gecko.harlequin", "Harlequin Crested Gecko");
        a.accept("entity.bettacraft.corn_snake", "Corn Snake");
        a.accept("entity.bettacraft.corn_snake.wild", "Corn Snake");
        a.accept("entity.bettacraft.corn_snake.amelanistic", "Amelanistic Corn Snake");
        a.accept("entity.bettacraft.corn_snake.anerythristic", "Anerythristic Corn Snake");

        a.accept("turtle.bettacraft.variety.tinctorius", "D. tinctorius (blue \"azureus\" form)");
        a.accept("turtle.bettacraft.variety.auratus", "D. auratus (green and black)");
        a.accept("turtle.bettacraft.variety.leucomelas", "D. leucomelas (yellow and black)");
        a.accept("turtle.bettacraft.variety.green", "Green form");
        a.accept("turtle.bettacraft.variety.brown", "Brown form");
        a.accept("turtle.bettacraft.variety.normal", "Normal (wild type)");
        a.accept("turtle.bettacraft.variety.tangerine", "Tangerine morph");
        a.accept("turtle.bettacraft.variety.albino", "Albino morph");
        a.accept("turtle.bettacraft.variety.tiger", "Tiger (wild type)");
        a.accept("turtle.bettacraft.variety.flame", "Flame morph");
        a.accept("turtle.bettacraft.variety.harlequin", "Harlequin morph");
        a.accept("turtle.bettacraft.variety.amelanistic", "Amelanistic morph (no black)");
        a.accept("turtle.bettacraft.variety.anerythristic", "Anerythristic morph (no red)");
        a.accept("tooltip.bettacraft.herp.tadpole", "Tadpole (needs water)");

        a.accept("item.bettacraft.dart_frog_box", "Boxed Poison Dart Frog");
        a.accept("item.bettacraft.horned_frog_box", "Boxed Horned Frog");
        a.accept("item.bettacraft.leopard_gecko_box", "Boxed Leopard Gecko");
        a.accept("item.bettacraft.crested_gecko_box", "Boxed Crested Gecko");
        a.accept("item.bettacraft.corn_snake_box", "Boxed Corn Snake");
        a.accept("item.bettacraft.dart_frog_spawn_egg", "Poison Dart Frog Spawn Egg");
        a.accept("item.bettacraft.horned_frog_spawn_egg", "Horned Frog Spawn Egg");
        a.accept("item.bettacraft.leopard_gecko_spawn_egg", "Leopard Gecko Spawn Egg");
        a.accept("item.bettacraft.crested_gecko_spawn_egg", "Crested Gecko Spawn Egg");
        a.accept("item.bettacraft.corn_snake_spawn_egg", "Corn Snake Spawn Egg");
        a.accept("item.bettacraft.crickets", "Crickets");
        a.accept("item.bettacraft.fruit_flies", "Fruit Flies");
        a.accept("item.bettacraft.cricket_culture", "Cricket Culture");
        a.accept("item.bettacraft.fruit_fly_culture", "Fruit Fly Culture");
        a.accept("block.bettacraft.frog_eggs", "Frog Eggs");

        a.accept("log.bettacraft.herp.no_nest_frog", "%s finds no place for her eggs: she needs moss, leaves or soil on the land");
        a.accept("log.bettacraft.herp.no_tadpole_water", "Tadpoles of the %s died: no water in the tank");
        a.accept("log.bettacraft.herp.tadpoles", "%2$s %1$s tadpoles hatched into the water");
        a.accept("log.bettacraft.herp.morph", "A hatchling of the %1$s shows a new morph: %2$s");
        a.accept("message.bettacraft.terrarium.ate", "%1$s eats the %2$s");
    }
}
