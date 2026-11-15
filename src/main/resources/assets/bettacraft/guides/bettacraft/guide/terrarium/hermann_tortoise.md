---
navigation:
  title: Hermann's tortoise
  icon: bettacraft:hermann_tortoise_box
  parent: terrarium.md
  position: 52
item_ids:
  - bettacraft:hermann_tortoise_box
  - bettacraft:hermann_tortoise_spawn_egg
  - bettacraft:terrarium_box
---

# Hermann's tortoise

Hermann's tortoise (*Testudo hermanni*, about 18 cm) is a land tortoise of the dry, sunny scrub and meadows around the
Mediterranean: a high domed shell of ochre scutes with black blotches. It never swims; it lives in a **dry terrarium**.

## Care

| | |
|---|---|
| Tank | a [terrarium](../terrarium.md) without water (a water dish is fine: 90 % land or more); 8 blocks or more for a grown one |
| Air | 20 to 32 °C; humidity 35 to 75 % |
| UV, basking spot | needs a [basking lamp](basking_lamp.md) (UV 4 or more: only a lamp gives that much) |
| Ground | sand or soil (for its eggs); [heat mats](heat_mat.md) keep the ground warm |
| Food | **greens only**: dandelion, clover (pink petals), herbs (short grass, fern), oxeye daisy |
| Stocking | a hatchling 0.5 bettas, growing to 5 |

* It travels in a **terrarium box**, not in water: right-click the tortoise with an empty
  <ItemLink id="bettacraft:terrarium_box" />, then right-click the tank with the full box (or a block to let it go). The
  empty box comes back.
* Feed the greens by right-clicking the tank (the hungriest tortoise eats one) or put them into a
  [feeder](../equipment/feeder.md).
* It walks slowly over the land and goes to the basking spot to warm up; it climbs steps of a quarter block.

## Greens

Real Hermann's tortoises eat wild herbs, dandelion and clover. Minecraft has no clover, so the food tag
`#bettacraft:turtle_greens` stands in: **dandelion**, **pink petals** (for the clover), **short grass** and **fern** (for
the herbs) and the **oxeye daisy**. Modpacks can add their own greens to the tag. Sliders and Reeves' turtles eat them too.

## Brumation

In the wild the tortoise sleeps through the winter. In the tank it does the same: when the **air falls below 12 °C**
(`terrarium.brumationBelow`) it goes into **brumation**: head and legs pulled into the shell, it does not move, does not
eat and gets no hungrier, and the cold does not harm it. It wakes up when the air is 2 K warmer again. Only **frost**
(air below 2 °C) still harms it. With `terrarium.brumation` off, the cold harms it like every other turtle.

The climate tab shows "in brumation"; the log notes when it starts and ends. A brumating female lays no eggs.

## Breeding

It lays its eggs into sand or soil (see [turtle breeding](turtle_breeding.md)). Pivotal temperature 31.5 °C: a nest
needs heat mats under the tank to get warm enough for females.

## Obtaining

* <ItemLink id="bettacraft:hermann_tortoise_box" />: **Wild** in savannas and dry shrublands, alone (spawn weight 4) (config `features.turtles.wildSpawns`).

Crafting:

<RecipesFor id="bettacraft:terrarium_box" />
