---
navigation:
  title: Reverse osmosis unit
  icon: bettacraft:ro_unit
  parent: equipment.md
  position: 103
item_ids:
  - bettacraft:ro_unit
  - bettacraft:ro_membrane
  - bettacraft:ro_prefilter
  - bettacraft:ro_water_bucket
---

# Reverse osmosis unit

<GameScene zoom="4" interactive={true}>
  <Block id="minecraft:water" x="-1" />
  <Block id="bettacraft:ro_unit" x="0" p:facing="north" p:active="true" />
  <Block id="bettacraft:glass_pipe" x="1" p:west="true" p:east="true" />
  <Block id="bettacraft:tank" x="2" p:fill="14" />
</GameScene>

The <ItemLink id="bettacraft:ro_unit" /> makes **reverse osmosis water**: water with nothing dissolved (TDS 0, pH 6.8).
Soft-water fish and shrimp need it; mixed with normal water or [remineralising salt](../aquarium/water_chemistry.md) it
gives any hardness you want.

* **Water in**: through pipes on any side, a bucket of water (or sea water: the membrane holds the salt back) or a
  **water source block next to it** (endless, like a tap).
* **It simply runs**, without power and without water pressure, slowly: 1 mB of RO water per tick
  (`osmosis.roRate`).
* **With [power costs](power.md) on and FE** its pressure pump makes it **four times faster** (`roPumpBoost`,
  `roPumpFe` 20 FE/t). Without FE it keeps running at the slow rate.
* **Yield**: for each part of RO water three parts of **waste water** leave (`roWastePerPart`). It goes into a pipe or
  a tank of another mod **below** the unit, otherwise down the drain.
* **RO water out**: an empty bucket gives a <ItemLink id="bettacraft:ro_water_bucket" />; pipes and
  [water pumps](pipes.md) take it out. It holds 8 buckets.
* **Membrane and prefilter**: <ItemLink id="bettacraft:ro_membrane" /> (about 128 buckets) and
  <ItemLink id="bettacraft:ro_prefilter" /> (about 32 buckets) wear out and must be replaced: right-click the unit with
  a new one (the old one comes out), or let a hopper put it in. Sneak + empty hand takes them out. Without both it
  stops.
* Right-click with an empty hand shows what it does: how fast it runs, the RO water, the water at the inlet and how
  much is left of the membrane and the prefilter.

Create's rotation does not drive it (no hard dependency on Create); it runs without anyway.

A water change with RO water mixes its values into the aquarium (TDS and pH by volume, see
[water fluids](../aquarium/water_fluids.md)).

`features.osmosis.enabled` (common config) switches the unit off.

## Obtaining

Crafting:

<RecipesFor id="bettacraft:ro_unit" />
<RecipesFor id="bettacraft:ro_membrane" />
<RecipesFor id="bettacraft:ro_prefilter" />
