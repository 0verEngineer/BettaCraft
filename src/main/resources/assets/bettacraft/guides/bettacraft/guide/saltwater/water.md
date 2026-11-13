---
navigation:
  title: Salt water
  icon: bettacraft:sea_salt
  parent: saltwater.md
  position: 1
item_ids:
  - bettacraft:sea_water_bucket
  - bettacraft:sea_salt
  - bettacraft:calcium_additive
---

# Salt water

## Getting sea water

* **Bucket of sea water**: use an empty bucket on water in an ocean or at a beach (server config
  `saltwater.oceanWaterIsSalty`). In the world it pours like plain water; poured into a tank it brings 35 ‰ of salt.
* **Sea salt**: from a bucket of sea water (crafting, the bucket stays) or from the wandering trader. A water bucket and sea
  salt make a bucket of sea water.
* Right-click a tank with **sea salt**: one portion salts one bucket (1000 mB) of its water to sea water; **sneak** to put
  in as much as it takes to reach 35 ‰.

## Mixing

The tank mixes what you pour in by volume: two buckets of sea water and two of fresh water give 17.5 ‰ (brackish water).
Draining keeps the salinity, and a salt water tank fills your buckets with sea water. **Evaporation takes only water**: the
salt stays and the water gets saltier, so top a reef tank up with **fresh** water. Pipes and pumps of other mods bring fresh
water. The tank log notes when the water turns brackish, salty or fresh again.

| Water | ‰ | Who lives in it |
|---|---|---|
| Fresh water | 0–2 | bettas, all tankmates, shrimp, crayfish, snails, dwarf clawed frogs, aquarium plants |
| Brackish | 2–20 | guppies, endlers and platys up to 18 ‰, nerite snails up to 30 ‰; aquarium plants stop growing from 4 ‰ |
| Sea water | 20–40 | the reef fish (28–40 ‰, the yellowtail damsel from 20), cleaner shrimp and turbo snails (28–40), hermit crabs (20–42), corals |

An animal outside of its range gets a **salt shock**: a few ‰ too much only stress it (like bad water), a freshwater fish
in sea water dies within hours, and so does a reef fish in fresh water. The tank refuses an animal when the water would
kill it (config `saltwater.refuseDeadlyWater`). Vanilla and modded water animals do not care about salt by default
(`saltwater.foreignAnimals = LENIENT`); with `REALISTIC` cod, tropical fish, pufferfish and squid need salt water and
axolotls and tadpoles fresh water.

## Calcium and nitrate

* **Calcium** (mg/L) comes with sea water and sea salt (sea water: 420). The stony corals build their skeletons from it and
  use it up; below 380 they slow down, below 250 they stop. Water changes with sea water and the **calcium additive**
  (from calcite or bone meal and sea salt) bring it back.
* **Nitrate** (NO₃) is the waste in the water as reefers know it: the menu shows it next to the salt. Stony corals slow down
  from 40 mg/L and stop at 60; a protein skimmer and live rock keep it low.

The water tab of the [tank menu](../aquarium/menu.md) shows salt, calcium and nitrate instead of the glass line, and in
the last line the skimmers, the flow pumps with their flow, the live rock and how much of the light the corals can use.
