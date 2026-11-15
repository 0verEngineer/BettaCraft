---
navigation:
  title: Land animals of other mods
  icon: bettacraft:terrarium_box
  parent: terrarium.md
  position: 75
feature: foreignAnimals.terrarium
---

# Land animals of other mods

Land and marsh animals of other mods can live in our terraria and paludaria when they have a **terrarium profile**
([profiles for animals of other mods](../settings/foreign_profiles.md)). Bettacraft brings profiles for:

| Animal | Mod | Tank | Size rule |
|---|---|---|---|
| [Tortoise](naturalist_tortoise.md) | Naturalist | dry terrarium | 1 m: 12 × 6 × 3 blocks, 100 blocks of land |
| [Alligator](naturalist_alligator.md) | Naturalist | big paludarium | 2 m: 24 × 12 × 6 blocks, 800 blocks of tank |

With a profile an animal gets:

* **Its tank:** a dry terrarium, a paludarium or either, with its air, humidity, UV and basking spot like our
  [terrarium animals](../terrarium.md). A big one has a length and goes by the [size rule of the big animals](../big_tanks.md),
  measured over the **land** (a tortoise), the **water** or the **whole tank** (an alligator, at home in both).
* **Our care:** it gets hungry, eats its food by hand on the tank and from the [feeder](../equipment/feeder.md) (greens and
  raw meat too), minds its climate, falls sick and dies without care.
* **Our walker:** in its tank it walks on the land of the interior, swims in the water and goes to the basking spot, instead
  of the AI of its mod (which cannot see the land of the miniature).
* **Our breeding:** a fed pair breeds now and then; an egg layer lays its eggs into sand or soil on the land, and the nest
  temperature hatches them (warmer faster). Its own breeding is suppressed in the tank.
* **The terrarium box:** an empty <ItemLink id="bettacraft:terrarium_box" /> catches it (right-click the animal), a full one
  puts it into a tank or lets it go. Animals too big for a box (the alligator) go with the [large net](../big_tanks/large_net.md)
  into a [transport tub](../big_tanks/transport_tub.md). Animals without a profile are refused, unless the server lists them
  in `terrarium.boxAllow` (they then live in terraria and paludaria without our care). A tame animal goes into the box
  only for its owner.

Switch: `features.foreignAnimals.terrarium` (off: none of them go into tanks and the box takes none; those in tanks stay).
