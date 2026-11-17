---
navigation:
  title: Liquid fertilizer
  icon: bettacraft:liquid_fertilizer
  parent: plants.md
  position: 12
feature: fertilizer.enabled
item_ids:
  - bettacraft:liquid_fertilizer
  - bettacraft:kelp_meal
  - bettacraft:nutrient_salt
  - bettacraft:fertilizer_canister
---

# Liquid fertilizer

<Row>
  <ItemImage id="bettacraft:kelp_meal" scale="2" />
  <ItemImage id="bettacraft:nutrient_salt" scale="2" />
  <ItemImage id="bettacraft:liquid_fertilizer" scale="2" />
  <ItemImage id="bettacraft:fertilizer_canister" scale="2" />
</Row>

Liquid fertilizer makes the [aquarium plants](../plants.md) and [floating plants](floating_plants.md) of Bettacraft grow
faster. They grow without it as well; plants of other mods in a tank are not affected.

* **Dosing by hand:** right-click a tank with a bottle of <ItemLink id="bettacraft:liquid_fertilizer" />. A bottle has five
  portions; one portion brings 4 water blocks to the right level (100 %). The last portion leaves the glass bottle.
* **Up to 100 %** the plants grow up to **one and a half times as fast** (`fertilizer.growthBoost`); less fertilizer
  helps less. The **Water** tab of the tank menu shows the level: none, low, good or overdosed.
* The **plants use it up**, a dense tank faster than a sparse one, and a little goes by itself. Water changes dilute it;
  in pipes it travels with the water, so a filter loop keeps it.
* **Overdosed** from 150 % (`fertilizer.overdoseAt`): more brings no more growth, but the **algae** grow faster and
  spread over the glass (three times as fast at 300 %). Change some water to bring it down.

## Automatic dosing

* The [fertilizer maker](fertilizer_maker.md) pushes its fertilizer through a pipe into a tank: the tank takes it only up
  to 100 % (`fertilizer.pipeUpTo`), so a pipe never overdoses it.
* The **dosing pump** of the reef tanks also doses fertilizer (`features.fertilizer.dosing`): put a
  <ItemLink id="bettacraft:fertilizer_canister" /> (500 mL, two bottles) into a channel and set its mL per day. It doses
  fertilizer into fresh and salt water tanks (50 mL = one portion).

## Intermediates

* <ItemLink id="bettacraft:kelp_meal" />: two dried kelp ground with bone meal, the organic part.
* <ItemLink id="bettacraft:nutrient_salt" />: bone meal (phosphate), charcoal (potash) and an iron nugget, the minerals.

## Obtaining

Crafting:

<RecipesFor id="bettacraft:kelp_meal" />
<RecipesFor id="bettacraft:nutrient_salt" />
<RecipesFor id="bettacraft:fertilizer_canister" />
