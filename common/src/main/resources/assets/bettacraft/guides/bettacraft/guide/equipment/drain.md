---
navigation:
  title: Drain
  icon: bettacraft:drain
  parent: equipment/water_system.md
  position: 8
feature: pipes.drain
item_ids:
  - bettacraft:drain
---

# Drain

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:water_change_controller" p:facing="south" />
  <Block id="bettacraft:drain" x="1" />
</GameScene>

The <ItemLink id="bettacraft:drain" /> is the **end of a waste water line**: a [pipe](pipes.md) joins it on every side, and it
takes all the water that comes down the pipe and **discards** it. It holds nothing and gives nothing.

* Put it at the **waste water outlet** of a [water change controller](water_change.md) (on its right, seen from the front):
  right at the port, as in the scene above, or at the end of a pipe. Several controllers may share one drain line.
* The old water may also go into a [reservoir](reservoir.md) or another mod's tank instead; the drain is the simple way to
  get rid of it.
* Other mods' pipes may use it as a sink too. It takes water (with its values), no other fluid.

With `features.pipes.drain = false` it takes no water (pipes still join it).

## Obtaining

Crafting:

<RecipesFor id="bettacraft:drain" />
