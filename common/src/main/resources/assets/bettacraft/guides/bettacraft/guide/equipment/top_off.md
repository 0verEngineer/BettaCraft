---
navigation:
  title: Top-off
  icon: bettacraft:level_sensor
  parent: equipment.md
  position: 46
item_ids:
  - bettacraft:level_sensor
  - bettacraft:reservoir
---

# Top-off

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:tank" x="0" p:fill="14" />
  <Block id="bettacraft:level_sensor" x="1" p:facing="east" />
  <Block id="bettacraft:reservoir" x="2" p:fill="6" />
</GameScene>

Open aquariums lose water to evaporation (a lid helps). The <ItemLink id="bettacraft:level_sensor" /> tops it off:

* Place it **next to an aquarium** (or at a [multiblock filter](multiblock_filter.md): then it tops off the aquarium
  the filter cleans).
* When the water has dropped a little below its **target** (100 %; right-click: 95, 90, 80, 70, 60, 50 %), it fills
  up from what is **next to it**: a <ItemLink id="bettacraft:reservoir" />, a [pipe](pipes.md), another mod's tank
  (also AE2 or RS through their interfaces) or a water source block.
* **Fresh water only.** When water evaporates, the salt of a [reef tank](../saltwater.md) stays behind; topping off with
  fresh water brings the salinity back exactly where it was. A salty source is refused.

The **reservoir** holds 16 buckets. Fill it with buckets or pipes; any water mixes in (right-click shows how full it
is and what water it holds; a comparator reads it).

## Obtaining

Crafting:

<RecipesFor id="bettacraft:level_sensor" />
<RecipesFor id="bettacraft:reservoir" />
