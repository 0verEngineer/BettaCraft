---
navigation:
  title: Filter media
  icon: bettacraft:ceramic_rings
  parent: equipment.md
  position: 39
item_ids:
  - bettacraft:filter_foam
  - bettacraft:ceramic_rings
  - bettacraft:activated_carbon
  - bettacraft:filter_peat
---

# Filter media

What the chambers of the [multiblock filter](multiblock_filter.md) hold:

| Medium | Cleans | Wears |
|---|---|---|
| <ItemLink id="bettacraft:filter_foam" /> | 12 water blocks (mechanical) | clogs fastest; a clogged sponge also slows the flow |
| <ItemLink id="bettacraft:ceramic_rings" /> | 24 water blocks (bacteria) | clogs at half the speed |
| <ItemLink id="bettacraft:activated_carbon" /> | 16 water blocks (chemical) | used up, then replace it |
| <ItemLink id="bettacraft:filter_peat" /> | 6 water blocks; softens the water (pH comes later) | used up slowly |

Media get dirty while the filter runs, faster with more fish (`multiblockFilter.dirtPerDay`). The tooltip and the
filter's screen show the dirt; a clogged sponge or ceramic still cleans a fifth.

**Cleaning:** right-click a **water cauldron** with a dirty sponge or ceramic rings: it is washed clean and the
cauldron loses one level. Carbon and peat are not cleaned: put in fresh ones.

## Obtaining

Crafting:

<RecipesFor id="bettacraft:filter_foam" />
<RecipesFor id="bettacraft:ceramic_rings" />
<RecipesFor id="bettacraft:activated_carbon" />
<RecipesFor id="bettacraft:filter_peat" />
