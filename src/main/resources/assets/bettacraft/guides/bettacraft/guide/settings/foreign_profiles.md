---
navigation:
  title: Profiles for animals of other mods
  icon: minecraft:tropical_fish_bucket
  parent: settings.md
  position: 20
feature: foreignAnimals.enabled
---

# Profiles for animals of other mods

A **profile** gives an animal of another mod (or of vanilla) our care and our breeding in a Bettacraft aquarium. Outside
the aquarium nothing changes: the animal lives by its own mod there.

## What a profile does

* **Care** (`features.foreignAnimals.care`): the animal gets hungry like a betta (it eats the tank's food: by hand, from the
  feeder, the live food pump), minds its temperature range, the water quality, pH and TDS, the stocking and (with
  `saltwater.foreignAnimals = REALISTIC`) the salinity.
  Days of trouble make it sick, a sick animal that is not helped dies (tank log with the cause). A fed animal heals. The
  tank menu shows its health and hunger.
* **Breeding** (`features.foreignAnimals.breeding`): two grown, fed, healthy animals of the same kind in water of their breeding
  temperature breed now and then. A **livebearer** carries the young and gives birth, an **egg layer** spawns and the
  eggs hatch (some need plants, food fish a big aquarium). The young take the variants of their parents (colours,
  patterns: what their mod stores about them), one parent's value for each.
* **Their own breeding suppressed** (`features.foreignAnimals.suppressOwnBreeding`, for every animal of vanilla and other mods in
  an aquarium): no love from food, no young of their mod's breeding. Our own breeding of axolotls, turtles and the mobs
  in glass is not affected.

## The shipped profiles

| Mod | Animals | Like | Breeds |
|---|---|---|---|
| Fintastic | guppy | our [guppy](../tankmates.md) | livebearer, fin and tail colours inherited |
| Fintastic | gourami | our dwarf gourami | egg layer, size and skin inherited (the giant gourami is too big) |
| Fintastic | pleco, small catfish | bristlenose pleco, panda cory | egg layers |
| Fintastic | minnow | cool water (14 to 26 °C) | eggs on plants |
| Fintastic | moony | brackish water | no |
| Fintastic, Aquaculture, Naturalist | cod, salmon, herring, bluegill, perch, bass, trout, boulti, catfish, piranha | food fish | only in aquariums of 24 water blocks and more (piranha 16) |
| Aquaculture | synodontis | catfish | no |
| Critters and Companions | koi | pond fish (4 to 28 °C) | eggs on plants, 16 water blocks |
| Naturalist | [tortoise](../terrarium/naturalist_tortoise.md) | dry terrarium, 1 m, greens | eggs into sand or soil |
| Naturalist | [alligator](../terrarium/naturalist_alligator.md) | big paludarium, 2 m, raw meat, hunts small fish | eggs into sand or soil |
| Naturalist | [lizard, snake](../terrarium/foreign_land_animals.md) | dry terrarium, crickets and greens / prey | eggs into sand or soil |
| Naturalist | [desert and jungle scorpion](../terrarium/foreign_land_animals.md) | dry terrarium (dry / humid air), crickets | live young |
| Naturalist | [snail](../terrarium/foreign_land_animals.md) | humid terrarium, greens | eggs into sand or soil |
| Naturalist | [crab](../terrarium/foreign_land_animals.md) | paludarium, fresh or sea water, pellets and fish | no |
| Naturalist, Critters and Companions | [dragonfly](../terrarium/foreign_land_animals.md) | paludarium, flies, fruit flies | no |
| Naturalist | [duck, capybara](../terrarium/foreign_land_animals.md) | big paludarium, 0.6 m / 1.2 m | eggs into sand or soil / live young |
| Critters and Companions | [leaf insect](../terrarium/foreign_land_animals.md) | humid terrarium, leaves | eggs into sand or soil |
| Critters and Companions | [otter](../terrarium/foreign_land_animals.md) | big paludarium, 1 m, fish, hunts small fish | live young |
| Critters and Companions | sea bunny | sea water aquarium, pellets | no |

## Writing a profile

Profiles are data: `data/<namespace>/bettacraft/foreign_animals/<name>.json` in a data pack (or a KubeJS data folder),
read again with `/reload`. A file for the same entity in a later pack replaces the earlier one. Entities whose mod is not
installed are skipped quietly (unless `"required": true`). Example:

```json
{
  "entity": ["somemod:neon", "somemod:cardinal"],
  "species": "neon_tetra",
  "group": "tetra",
  "care": {"temperature": [22, 27], "min_quality": 0.5, "salinity": "fresh", "ph": [5.5, 7.5], "tds": [0, 300], "stock": 0.35},
  "food": ["normal", "live", "fry", "#somemod:fish_food"],
  "breeding": {
    "type": "eggs", "temperature": [24, 26], "needs_plants": true, "min_water_blocks": 0,
    "days": 2, "young": [2, 5], "pause_days": 3, "rate": 0.5,
    "inherit": ["Variant"], "offspring_nbt": "{FromBucket:1b}", "baby": true
  },
  "suppress_own_breeding": true
}
```

| Key | Meaning |
|---|---|
| `entity` | one entity id or a list |
| `species` | optional: one of our species (`guppy`, `dwarf_gourami`, `neon_tetra`, ...) whose values it takes; every key of the file wins |
| `group` | a label (`livebearer`, `pond_fish`, `food_fish`, ...) |
| `care.temperature`, `min_quality`, `salinity` | range in °C, lowest water quality (0 to 1), `fresh`, `brackish`, `brackish_only`, `marine`, `hardy_marine`, `any` or `[min, max]` in ppt |
| `care.ph`, `care.tds` | its pH and TDS range (left out: pH and TDS do not matter, unless the species has one) |
| `care.labyrinth`, `hunger_days`, `stock` | breathes air (low oxygen does not harm it), days until hungry (1 like a betta), how much it counts for the stocking |
| `food` | `normal` (pellets, flakes ...), `live` (live and frozen food), `fry` (fry food; babies always eat it), `#item_tag` or an item id |
| `breeding.type` | `livebearer`, `eggs` or `none` |
| `breeding.temperature`, `needs_plants`, `min_water_blocks` | when a pair breeds |
| `breeding.days`, `young`, `pause_days`, `rate` | days to the young, young per brood, rest of the mother, broods per pair and day |
| `breeding.offspring`, `offspring_nbt` | the entity of the young (default: the same) and NBT for every young |
| `breeding.inherit` | NBT keys every young takes from one of its parents (find them with `/data get entity`) |
| `breeding.baby` | the young are born as babies of their mod (default true) |
| `terrarium` | instead of an aquarium: `habitat` (`dry`, `paludarium`, `any`, `wet`), `air`, `humidity`, `land`, `water`, `uv`, `basking` like the [terrarium animals](../terrarium.md); the keys below |
| `terrarium.length` | its length in metres: a big animal by the [size rule](../big_tanks.md) (12 / 6 / 3 blocks per metre, 100 blocks per cubic metre); left out: its hitbox decides |
| `terrarium.volume` | what the size rule measures: `land` (the land with the air above it), `water` (the water part) or `all` (the whole tank); default by the habitat (`dry` the land, `wet` the water, else all) |
| `terrarium.movement` | how it moves in the tank: `walk` (default), `slither`, `hop`, `climb`, `swim`, `fly` (through the air, rests on the land), or `own` for the AI of its mod; or an object with `style` and `walk`, `swim` (blocks per tick), `bottom`, `max_step`, `water_time`, `bask_time`, `rest_time`, `height` |
| `terrarium.prey` | it eats the small fish and shrimp of its tank up to this length (blocks; from 0.5 also bettas and dwarf frogs) |
| `terrarium.box` | the terrarium box catches it (default true; false: the large net and the transport tub) |
| `terrarium.min_blocks` | a small animal's smallest tank (blocks of water and land; smaller stresses it) |
| `breeding.nest`, `nest_temperature` | eggs into a nest on the land whose top is of this block tag (`#bettacraft:turtle_nest_ground`: sand and soil), hatching in `days` at that nest temperature (°C; warmer faster, at 18 °C and below they wait, above 36 °C they die) |
| `suppress_own_breeding` | `false` lets the animal's own breeding run in the aquarium too |

Animals with a care of their own in Bettacraft (our animals, the axolotl, dolphin, sea turtle and the mobs in glass) cannot
be given a profile. The server log lists the profiles it loaded and every mistake in a file.

## Terrarium profiles

A land animal with a terrarium profile (example: the tortoise):

```json
{
  "entity": "naturalist:tortoise",
  "care": {"hunger_days": 1.5},
  "food": ["#bettacraft:turtle_greens"],
  "breeding": {"type": "eggs", "nest": "#bettacraft:turtle_nest_ground", "nest_temperature": 30, "days": 4, "young": [2, 4], "inherit": ["Variant"]},
  "terrarium": {"habitat": "dry", "air": [22, 35], "humidity": [30, 70], "uv": 4, "basking": true,
                "length": 1.0, "volume": "land", "movement": {"style": "walk", "walk": 0.012}}
}
```

The server option `terrarium.boxAllow` lists land animals without a profile that the terrarium box catches all the same
(small ones only; they live in terraria and paludaria without our care).

## Checking the profiles in a world

* **A renamed entity:** if the mod of a profile is installed but the entity id is not registered (a newer version of the
  mod renamed it), the server log warns with the profile file, the missing id and the similar ids of that mod, so the
  file can be fixed. A profile for a mod that is not installed stays quiet.
* **`/bettacraft foreign list [radius]`** (operators): every animal of vanilla or another mod in the tank you look at, or
  else in all tanks within the radius (default 32 blocks), with its entity id and the profile file that maps it, or "no
  profile". At the end, the entity types of other mods that live in these tanks without a profile: the ones a profile
  would be worth writing for.
