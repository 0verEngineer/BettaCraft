---
navigation:
  title: Daphnia barrel
  icon: bettacraft:daphnia_barrel
  parent: food/live_food.md
  position: 40
item_ids:
  - bettacraft:daphnia_barrel
  - bettacraft:daphnia
  - bettacraft:green_water_culture
  - bettacraft:green_water_bottle
---

# Daphnia barrel

<Row>
  <ItemImage id="bettacraft:daphnia_barrel" scale="2" />
  <ItemImage id="bettacraft:daphnia" scale="2" />
  <ItemImage id="bettacraft:green_water_bottle" scale="2" />
</Row>

**Daphnia** (water fleas) are live food for bettas, tankmates, dwarf frogs and seahorses. The daphnia barrel keeps a
culture of them, with no fish in it.

1. Fill it with **water**: at least 2 buckets (2000 mB), it holds 4000; pipes fill it too.
2. Start the culture with a few **daphnia**: a bottle of swamp water sometimes brings some up with it
   (`liveFood.bottleDaphniaChance`).
3. Feed **yeast** or **green water** (`#bettacraft:daphnia_food`; green water feeds more). Green water: swamp water with
   bone meal gives a green water culture, which turns green in the light after a day. By hand you keep the empty bottle,
   from a hopper it stays in the barrel (a hopper below takes it out).
4. It needs **light** 9 or more and no **hot sun**: in a hot biome (desert, savanna, badlands) a barrel under the open sky
   dies by day, so put it in the shade with a lamp. In the Nether it is always too hot.
5. Fed and lit, it fills up in about **3 in-game days** (`liveFood.daphniaDays`) and then holds 16 portions
   (`liveFood.daphniaRoom`).

## Harvest in time

A full barrel is **crowded**. If nobody harvests it for a day (`liveFood.crowdedDays`), the culture **collapses** to a
few animals and has to grow again. Harvest with the **aquarium net** (up to 4 portions), a hopper below or a
[live food pump](live_food_pump.md) that feeds your tanks on its own; a few always stay for the culture.

Right-click with an empty hand shows the water, the culture, the food and the portions ready; a comparator reads how
full it is.

## Obtaining

* <ItemLink id="bettacraft:daphnia" />: **Glass bottle** filled with water in swamps: 8 % per bottle, along with the water.
* <ItemLink id="bettacraft:green_water_culture" />: **[Aquarist villager](../getting_started/aquarist.md)** (Expert): 1 for 3 emeralds; one of about 4 in this offer (config `features.sources.aquaristVillager`).

Crafting:

<RecipesFor id="bettacraft:daphnia_barrel" />
<RecipesFor id="bettacraft:green_water_culture" />
