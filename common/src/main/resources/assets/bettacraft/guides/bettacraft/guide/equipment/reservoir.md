---
navigation:
  title: Reservoir
  icon: bettacraft:reservoir
  parent: equipment/water_system.md
  position: 9
item_ids:
  - bettacraft:reservoir
---

# Reservoir

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:reservoir" p:fill="6" />
  <Block id="bettacraft:glass_pipe" x="1" p:west="true" p:east="true" p:ring="fresh" />
  <Block id="bettacraft:water_change_controller" x="2" p:facing="south" />
</GameScene>

The <ItemLink id="bettacraft:reservoir" /> holds **16 buckets** of water. Fill it with buckets or [pipes](pipes.md); any
water mixes in (normal water, reverse osmosis water, sea water). Right-click shows how full it is and what water it holds; a
comparator reads it.

* **Fresh water for the controller:** at the end of the fresh water line of a [water change controller](water_change.md) it
  is the source for water changes and the top-off; the controller takes the water itself, no pump needed. Several
  controllers may share one reservoir.
* **Waste water:** it can also take the old water at the end of a waste water line. Use another reservoir than the one for
  fresh water, or the waste water mixes into it; a [drain](drain.md) simply discards it.
* The [hired aquarist](../getting_started/hired_aquarist.md) takes the water for his water changes from a reservoir, and a
  [mister](../terrarium/mister.md) fills itself from one next to it.

## Obtaining

Crafting:

<RecipesFor id="bettacraft:reservoir" />
