---
navigation:
  title: Feeder
  icon: bettacraft:feeder
  parent: equipment.md
  position: 40
item_ids:
  - bettacraft:feeder
---

# Feeder

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:tank" p:fill="14" />
  <Block id="bettacraft:feeder" y="1" />
</GameScene>

Place the <ItemLink id="bettacraft:feeder" /> **on top of an aquarium** and put food into its nine slots. Hoppers,
pipes of other mods and the buses and interfaces of AE2 or Refined Storage can fill it (and take food out).

* It feeds **once a day** in the morning by default. In its menu you can choose 1, 2 or 3 times a day (morning,
  afternoon, evening).
* Every fish gets the first food in the slots that suits it: **fry food** for fry, pellets or larvae for the older
  ones.
* The slots **take turns**: each feeding starts with the slot after the one that fed first last time, so pellets,
  flakes and frozen food in the feeder give the fish a [varied diet](../food/food_variety.md).
* When its own slots are empty it takes food from the [cabinets](cabinet.md) under the aquarium.
* The tank log notes every feeding and "The feeder is empty".

No room on top (a one-block tank with a light)? The [clip-on feeder](clip_feeder.md) hangs on the side of the glass.

One pellet or one portion of mosquito larvae feeds up to 4 fish, one bottle of fry food 20 fry. Overfeeding does no
harm. See [Food](../food.md).

## Obtaining

Crafting:

<RecipesFor id="bettacraft:feeder" />
