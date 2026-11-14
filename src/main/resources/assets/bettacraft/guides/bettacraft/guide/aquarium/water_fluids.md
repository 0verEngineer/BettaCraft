---
navigation:
  title: Water as a fluid
  icon: bettacraft:ro_water_bucket
  parent: aquarium.md
  position: 38
---

# Water as a fluid

Minecraft has one water. Bettacraft adds two fluids of its own, so that the tanks and storage of other mods (AE2,
Refined Storage, Mekanism, Create ...) keep them apart from water:

| Fluid | Values | Bucket |
|---|---|---|
| `bettacraft:ro_water` reverse osmosis water | TDS 0, pH 6.8, nothing dissolved | <ItemLink id="bettacraft:ro_water_bucket" /> |
| `bettacraft:sea_water` sea water | 35 ppt salt, the calcium of the sea, pH 8.2 | <ItemLink id="bettacraft:sea_water_bucket" /> |

* **Poured into the world** both are plain water (there is only one water block).
* **Into an aquarium** (bucket, pipe, [water pump](../equipment/pipes.md), the [water change controller](../equipment/water_change.md)
  or another mod's pipe) they become **water with their values**: they mix into the aquarium by volume like every
  water change (salt, calcium, TDS and pH; see [water chemistry](water_chemistry.md)). So the animals of other mods
  that live in water stay happy too.
* The [pipes](../equipment/pipes.md) and the reservoir carry them like water; water that leaves an aquarium is plain
  water with the aquarium's values.
* Buckets of sea water from earlier versions stay valid (same item); other mods now see `bettacraft:sea_water` in them.
* RO water comes from the [reverse osmosis unit](../equipment/reverse_osmosis.md), sea water from the ocean or from water
  and [sea salt](../saltwater/water.md).
* The [reverse osmosis unit](../equipment/reverse_osmosis.md) also takes sea water: the membrane holds the salt back.
