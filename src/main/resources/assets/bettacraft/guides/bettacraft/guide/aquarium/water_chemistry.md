---
navigation:
  title: Water chemistry
  icon: bettacraft:ph_test_kit
  parent: aquarium.md
  position: 37
item_ids:
  - bettacraft:remineralising_salt
  - bettacraft:almond_leaf
---

# Water chemistry

Besides temperature, quality and nitrate, the water of an aquarium has three simple chemical values. The tab
**Water** of the tank menu shows them in its second line (hover over it for what drives them and which animals the
water does not suit); the [TDS meter and the pH drop test](../equipment/water_tests.md) measure them at the tank.

* **TDS** (total dissolved solids, ppm): the minerals in the water. **Tap water has about 250 ppm**, [reverse osmosis
  water](../equipment/reverse_osmosis.md) 0. Food and waste of the animals raise it slowly (`chemistry.tdsPerDay`, 1.5 ppm a day in a normally stocked
  tank), evaporation leaves it behind (topping off with tap water raises it), water changes bring it back.
  <ItemLink id="bettacraft:remineralising_salt" /> raises it on purpose: one portion adds 400 ppm to a bucket of
  water; sneaking adds as much as brings the aquarium to 150 ppm.
* **pH** (5 to 9): tap water has 7.5. The pH drifts within about a day to what the aquarium holds:
  * soil on the floor (dirt, mud, podzol, moss ... `#bettacraft:acid_substrate`) pulls it towards 6.2;
  * limestone (calcite, dripstone, dead coral, Create's limestone ... `#bettacraft:limestone`) pulls it towards 8.2,
    as substrate fully, as rocks in the aquarium half;
  * peat in the [multiblock filter](../equipment/filter_media.md), driftwood (logs and roots in the aquarium,
    `#bettacraft:driftwood`), <ItemLink id="bettacraft:almond_leaf" /> and [CO2](../equipment/co2.md) lower it;
  * soft water (low TDS) is barely buffered and turns slightly acidic on its own.
* **Hardness** is no value of its own: **soft, medium hard or hard** from the TDS and the substrate (soil softens,
  limestone hardens). Tap water is medium hard.

Almond leaves colour the water with tannins (one leaf per water block colours it fully); they fade within about a
week (`chemistry.leafHalfLifeDays`). Dry jungle leaves in a smoker, furnace or on a campfire to get them.

## The animals

Every animal has its range of pH and TDS; **tap water suits all of them** but the
[bee shrimp](../invertebrates/caridina.md) (they need soft water: reverse osmosis water, remineralised a little, and
soil), so nothing changes for aquariums of older versions. A little out of the range the animals are only stressed (the menu warns, small animals breed slower); far
out (half a pH unit or 50 ppm beyond the range) the water counts as bad for them: they get sick and in the end die.

| Animals | pH | TDS (ppm) |
|---|---|---|
| Bettas | 6.0–8.0 | 0–600 |
| Tetras, rasboras, kuhli loach (soft water) | 5.0–7.8 | 0–450 |
| Corydoras, otocinclus | 5.8–8.0 | 0–500 |
| Plecos, clown loach | 5.8–8.0 | 0–550 |
| Guppy, endler, platy (livebearers) | 6.8–8.6 | 100–800 |
| Dwarf gourami | 6.0–8.0 | 0–500 |
| Neocaridina | 6.2–8.2 | 80–500 |
| Dwarf crayfish | 6.5–8.5 | 100–600 |
| [Bee shrimp (Caridina)](../invertebrates/caridina.md) | 5.5–6.8 | 100–200 |
| [Cardinal shrimp](../invertebrates/sulawesi_shrimp.md), [Tylomelania](../invertebrates/tylomelania.md) (Sulawesi) | 7.5–8.6 | 150–350 |
| Snails (ramshorn, bladder and trumpet snails: 6.2–8.6, 50–800) | 6.8–8.6 | 100–700 |
| Salt water animals | 7.8–8.6 | (only the pH) |

The species pages list the range of every animal. Salt water is buffered by its alkalinity: its pH stays near 8.2
and the substrate hardly moves it; when the stony corals use the alkalinity up, it sinks (see
[reef chemistry](../saltwater/reef_chemistry.md)).

`features.waterChemistry` (common config) switches it all off: the values stand still and no animal minds them.

## Obtaining

Crafting:

<RecipesFor id="bettacraft:remineralising_salt" />
<RecipesFor id="bettacraft:almond_leaf" />
