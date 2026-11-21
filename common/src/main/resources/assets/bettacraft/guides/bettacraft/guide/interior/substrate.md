---
navigation:
  title: Substrate
  icon: minecraft:gravel
  parent: interior.md
  position: 10
---

# Substrate

The floor of an interior consists of special floor blocks. Give each of them the look of another block:

* **Sneak + right-click** a floor block **with a block** in your hand: the floor block takes its texture (sand,
  gravel, moss, anything). Only the surface changes, no block is placed on top. Mix as you like, block by block.
* **Sneak + right-click** with an **empty hand** removes the substrate again.
* A normal right-click builds on the floor as usual.

Each covered floor block **costs one block**; you get it back when you replace or remove the substrate. Servers can make
substrate free (`interior.substrateCostsBlock = false`).

The substrate also sets the [water chemistry](../aquarium/water_chemistry.md): [active soil](active_soil.md) keeps the
water soft and acidic, plain soil a little, limestone (calcite, dripstone) makes it hard.

Only full blocks with a normal model and without a block entity can be used (and the mod's own active soil). The block tag
`#bettacraft:substrate_blacklist` excludes more blocks.

In the aquarium the substrate shows as a thin layer at the bottom. The tooltip of the interior button in the menu counts the covered
floor blocks.
