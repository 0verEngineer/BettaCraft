---
navigation:
  title: Coolers
  icon: bettacraft:cooler_medium
  parent: equipment.md
  position: 15
item_ids:
  - bettacraft:cooler_small
  - bettacraft:cooler_medium
  - bettacraft:cooler_large
---

# Coolers

<Row>
  <ItemImage id="bettacraft:cooler_small" scale="2" />
  <ItemImage id="bettacraft:cooler_medium" scale="2" />
  <ItemImage id="bettacraft:cooler_large" scale="2" />
</Row>

Coolers are the counterpart of the [heaters](heaters.md): where it is warmer than the target temperature (deserts,
badlands, the Nether), they pull the water down to it. Heaters only heat and coolers only cool, so an aquarium can have
both and always uses the one that fits.

| Cooler | Cools up to |
|---|---|
| <ItemLink id="bettacraft:cooler_small" /> | 16 water blocks |
| <ItemLink id="bettacraft:cooler_medium" /> | 32 water blocks |
| <ItemLink id="bettacraft:cooler_large" /> | 64 water blocks |

<RecipeFor id="bettacraft:cooler_small" />
<RecipeFor id="bettacraft:cooler_medium" />
<RecipeFor id="bettacraft:cooler_large" />

Coolers go wherever heaters go:

* **In the interior** as a rod (6, 9 or 12 blocks high at scale 16); it only cools while its lowest block is under water.
* **In an external filter**, in one of its two slots, also mixed with a heater (for example one heater and one cooler).
* In the filter of an [aquarium cabinet](cabinet.md).

With too little capacity the water only gets the matching part of the way to the target. When the server needs power
for heaters, coolers need it as well (see [Power](power.md)).
