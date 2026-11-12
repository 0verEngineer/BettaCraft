---
navigation:
  title: Inhabitants
  icon: bettacraft:fish_bag
  parent: aquarium.md
  position: 20
---

# Inhabitants

## Putting animals in

Right-click the aquarium with a **betta bucket**, any other **fish bucket** or a filled **fish bag**. The animal
appears in the water with all its data. The aquarium must be at least half full. A filled *Fish Bag* of Better Fish
Tanks (+100 mB) and a full *Fishnet* of Fintastic (no water) work as well.

Accepted are:

1. animals in the entity tag `#bettacraft:tank_inhabitants` (axolotl, tadpole),
2. every small water animal that fits in a bucket (hitbox up to 0.8 x 0.8),
3. never animals in `#bettacraft:tank_blacklist` (giant fish of other mods).

Other animals keep their own behaviour and have **no needs** in the aquarium; only bettas get hungry, need warmth and
breed. Small water animals that get into the aquarium without a bucket (spawn egg, command) are adopted as well.

## Taking animals out

* **Menu:** open the tank menu, select the animal on the *Animals* tab and click *Bucket* or *Fish bag*. You need an
  empty bucket or fish bag in your inventory. The button is greyed out if there is not enough water. The *Fish bag*
  button also fills an empty Better Fish Tanks fish bag (-100 mB) or a Fintastic fishnet (no water).
* **Directly:** right-click a fish in the aquarium with a water bucket, like anywhere else.

## Stocking

Only **adult** animals count. Eggs, larvae, fry, juveniles and subadults do not.

* One adult per water block is fine.
* From **2 adults per water block** the aquarium is overcrowded: the bettas get stressed and eventually sick
  (see [Health](../bettas/health.md)). The menu shows "Overcrowded!".
* At most **6 adults per water block** fit in at all.
* An aquarium holds at most 64 animals in total, young ones included (`tank.maxEntities`).

## Predators

Pufferfish and the predator fish of other mods eat small fish. See [Fights, flaring and predators](../bettas/fighting.md).

## Live prey

Small prey animals of other mods (`#bettacraft:betta_prey`: Fintastic fairy shrimp, daphnia and copepods) are live
food: a hungry betta eats one and counts as fed with live food, which also helps breeding.
