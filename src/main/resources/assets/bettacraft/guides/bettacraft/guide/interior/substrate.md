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

Only full blocks with a normal model and without a block entity can be used. The block tag
`#bettacraft:substrate_blacklist` excludes more blocks.

In the aquarium the substrate shows as a thin layer at the bottom. The interior tab of the menu counts the covered
floor blocks.
