---
navigation:
  title: Food variety
  icon: bettacraft:fish_flakes
  parent: food.md
  position: 40
item_ids:
  - bettacraft:fish_flakes
  - bettacraft:food_granules
  - bettacraft:shrimp_food
  - bettacraft:frozen_mosquito_larvae
  - bettacraft:frozen_artemia
  - bettacraft:frozen_daphnia
---

# Food variety

<Row>
  <ItemImage id="bettacraft:fish_flakes" scale="2" />
  <ItemImage id="bettacraft:food_granules" scale="2" />
  <ItemImage id="bettacraft:frozen_mosquito_larvae" scale="2" />
  <ItemImage id="bettacraft:frozen_artemia" scale="2" />
  <ItemImage id="bettacraft:frozen_daphnia" scale="2" />
  <ItemImage id="bettacraft:shrimp_food" scale="2" />
</Row>

Every fish food belongs to a **food group**: pellets, flakes, granules, wafers, frozen food or live food. Every fish
remembers which groups it ate in the last 3 days (`diet.varietyDays`).

* **Varied diet:** a fish that ate **3 different groups** in that time (`diet.varietyGroups`) recovers **one and a half
  times as fast** from stress and sickness (`diet.varietyRecovery`), and a betta shows a little stronger colours. Hover
  the food bar of a fish in the tank menu to see how many kinds it had. Put several foods into the
  [feeder](../equipment/feeder.md): its slots take turns.
* **Favourite food** of a species conditions an adult for breeding (like live food).
* **Food that suits a species badly** fills only for a while: the fish is hungry again half a day sooner
  (`diet.poorFoodFedDays`).

| Eaters | Favourite | Suits badly |
|---|---|---|
| Bettas | live and frozen food | wafers (they do not eat them) |
| Midwater fish (tetras, rasboras, livebearers, gouramis, marine fish) | flakes, frozen food | wafers |
| Bottom fish (corydoras, loaches) | granules, wafers, frozen food | flakes (they float) |
| Grazers (plecos, otocinclus, tangs) | wafers | live and frozen food, flakes |
| Seahorses | frozen food | |

Datapacks change these with the item tags `#bettacraft:food_groups/<group>` and `#bettacraft:diet/<eaters>/favourite`
and `/poor` (`betta`, `omnivore`, `bottom`, `grazer`, `seahorse`).

## The foods

* **Fish flakes** float at the surface: the midwater fish like them, the bottom fish hardly get any.
* **Food granules** sink slowly: the bottom fish get their share even next to faster fish.
* **Frozen food** (mosquito larvae, artemia, daphnia frozen with ice): eaten like live food by juveniles and older,
  conditions for breeding and keeps. It cannot go through the [live food pump](live_food_pump.md).
* **Shrimp food** sinks to the shrimp, crayfish and snails; the fish leave it. It gives them twice the food of a leftover
  pellet (`diet.shrimpFoodFactor`).

All of them come from the [food maker](food_maker.md).

## Obtaining

* <ItemLink id="bettacraft:fish_flakes" />: **[Food maker](food_maker.md)**: 1 × <ItemLink id="minecraft:cod" /> + 1 × <ItemLink id="minecraft:wheat" /> + 1 × <ItemLink id="minecraft:kelp" /> + 1 × <ItemLink id="minecraft:sugar" /> → 12.
* <ItemLink id="bettacraft:food_granules" />: **[Food maker](food_maker.md)**: 1 × <ItemLink id="minecraft:spider_eye" /> + 2 × <ItemLink id="minecraft:wheat" /> + 1 × <ItemLink id="minecraft:dried_kelp" /> → 12.
* <ItemLink id="bettacraft:shrimp_food" />: **[Food maker](food_maker.md)**: 1 × <ItemLink id="minecraft:kelp" /> + 1 × <ItemLink id="minecraft:carrot" /> + 1 × <ItemLink id="minecraft:bone_meal" /> → 8.
* <ItemLink id="bettacraft:frozen_mosquito_larvae" />: **[Food maker](food_maker.md)**: 4 × <ItemLink id="bettacraft:mosquito_larvae" /> + 1 × <ItemLink id="minecraft:ice" /> → 4.
* <ItemLink id="bettacraft:frozen_artemia" />: **[Food maker](food_maker.md)**: 4 × <ItemLink id="bettacraft:artemia_nauplii" /> + 1 × <ItemLink id="minecraft:ice" /> → 4.
* <ItemLink id="bettacraft:frozen_daphnia" />: **[Food maker](food_maker.md)**: 4 × <ItemLink id="bettacraft:daphnia" /> + 1 × <ItemLink id="minecraft:ice" /> → 4.
