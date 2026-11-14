---
navigation:
  title: Cabinet
  icon: bettacraft:cabinet
  parent: equipment.md
  position: 80
item_ids:
  - bettacraft:cabinet
---

# Aquarium cabinet

![Cabinets of different woods with aquariums](../images/cabinets.png)

The <ItemLink id="bettacraft:cabinet" /> is a stand for the aquarium, made of **any wood**: five planks of one kind
(also from other mods), three wooden slabs and a wooden chest. The cabinet takes the look of its planks.

* Cabinets next to each other that face the same way join into one long cabinet with a continuous top.
* **Filter slot:** put an [external filter](filters.md) in. A cabinet directly under the aquarium then counts as an
  attached external filter; the hose runs up the wall of its **outlet side** (see below; the back wall unless you
  choose another). The two heater slots of the cabinet menu are the heater slots of that filter.
* **Nine storage slots** for food and other things. Hoppers can use them, and the [feeder](feeder.md) on the aquarium
  takes food from the cabinets under it.
* Broken, the cabinet drops its contents like a chest and keeps its wood and its outlet side.

## Outlet side

![The four outlet sides: back, left, right and front](../images/cabinet_outlets.png)

The button **"Outlet: back"** in the cabinet menu chooses the tank wall the filter hose goes up, seen from the cabinet
door: **back** (the default), **left**, **right** or **front** (unusual, but possible). Each cabinet has its own side,
also several cabinets under one big tank. Click for the next side, sneak + click for the previous one.

* Hose, outlet, water jet and bubbles move to that wall.
* The **current** comes from there: floating plants drift away from the outlet, and in front of it the water moves
  faster than in the rest of the tank (up to almost twice the tank's flow, fading over eight blocks).
  [Seahorses](../saltwater/seahorses.md) there are stressed by the flow sooner, so put the outlet on the other side of
  their hitching posts; corals in front of it get more flow (good for acropora, too much for mushroom corals).
* Broken and placed again, the cabinet keeps the side (its item shows it). Cabinets of older worlds stay on the back
  wall. The setting `features.cabinetOutletSide` switches the button off (every cabinet then uses the back wall and the
  flow is even again).

## Stone cabinets

![Cabinets of different stones with aquariums](../images/cabinets_stone.png)

A cabinet can also be made of **stone**: five blocks of one stone, three **slabs of the same stone** and a wooden chest
(stone bricks with stone brick slabs, sandstone with sandstone slabs ...). Every stone with its own slabs works: stone,
cobblestone, granite, diorite, andesite, deepslate, tuff, blackstone, sandstone, bricks, nether bricks, quartz, terracotta
with slabs and the stones of other mods. Metal and copper blocks, glass, purpur, end stone, prismarine, concrete and mud
bricks do not.

<Recipe id="bettacraft:stone_cabinet" />

* The stone cabinet looks like its stone and can do the same as a wooden one (filter, heaters, storage, joined counters).
* It is mined like its stone: with a **pickaxe** (by hand it drops nothing but its contents), as hard and as blast
  resistant as the stone.
* Modpacks can add stones with the item tag `#bettacraft:cabinet_stones` and rule some out with
  `#bettacraft:cabinet_excluded`. The setting `features.stoneCabinets` switches the stone recipe off.

## Obtaining

Crafting:

<RecipesFor id="bettacraft:cabinet" />
