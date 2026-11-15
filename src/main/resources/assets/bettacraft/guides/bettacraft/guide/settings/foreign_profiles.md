---
navigation:
  title: Profiles for animals of other mods
  icon: minecraft:tropical_fish_bucket
  parent: settings.md
  position: 20
---

# Profiles for animals of other mods

A **profile** gives an animal of another mod (or of vanilla) our care and our breeding in a Bettacraft aquarium. Outside
the aquarium nothing changes: the animal lives by its own mod there.

## What a profile does

* **Care** (`features.foreignCare`): the animal gets hungry like a betta (it eats the tank's food: by hand, from the
  feeder, the live food pump), minds its temperature range, the water quality, the salinity, pH and TDS and the stocking.
  Days of trouble make it sick, a sick animal that is not helped dies (tank log with the cause). A fed animal heals. The
  tank menu shows its health and hunger.
* **Breeding** (`features.foreignBreeding`): two grown, fed, healthy animals of the same kind in water of their breeding
  temperature breed now and then. A **livebearer** carries the young and gives birth, an **egg layer** spawns and the
  eggs hatch (some need plants, food fish a big aquarium). The young take the variants of their parents (colours,
  patterns: what their mod stores about them), one parent's value for each.
* **Their own breeding suppressed** (`features.suppressForeignBreeding`, for every animal of vanilla and other mods in
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
| `terrarium` | instead of an aquarium: `habitat` (`dry`, `paludarium`, `any`), `air`, `humidity`, `land`, `water`, `uv`, `basking` like the [terrarium animals](../terrarium.md) |
| `suppress_own_breeding` | `false` lets the animal's own breeding run in the aquarium too |

Animals with a care of their own in Bettacraft (our animals, the axolotl, dolphin, sea turtle and the mobs in glass) cannot
be given a profile. The server log lists the profiles it loaded and every mistake in a file.
