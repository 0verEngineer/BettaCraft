---
navigation:
  title: Water change controller
  icon: bettacraft:water_change_controller
  parent: equipment.md
  position: 45
item_ids:
  - bettacraft:water_change_controller
  - bettacraft:drain
---

# Water change controller

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:water_change_controller" p:facing="south" />
</GameScene>

Aquariums need **water changes** because [nitrate](../aquarium/nitrate.md) builds up. By hand: take water out with an
empty bucket and pour fresh water in (25 % of a 4-block aquarium is one bucket). Fresh water has the temperature of
the place, so a big change is a cool water change (corydoras love it).

The <ItemLink id="bettacraft:water_change_controller" /> does it by itself. Right-click it:

* **interval** 1 to 60 days (default: what the difficulty asks for, 14 days) and **share** 5 to 50 % (default 25 %);
* **automatic on/off** and **change now**.

It has **four pipe ports** (seen from the front): **fresh water in** on the left (a [pipe](pipes.md) from a
[reservoir](top_off.md), a water pump at a water source, an osmosis unit or another mod's tank), **fresh water out** to the
aquariums at the back, **waste water in** from the aquariums on top, **waste water out** on the right (to a
<ItemLink id="bettacraft:drain" />, a reservoir or another mod's tank). Placed **against an aquarium** or a multiblock filter,
the touch stands for the two aquarium-side ports, then two pipes do. Fresh water never comes from what merely stands next to
it. A change moves what its pipes carry; it only drains what it can refill, and never below half full while animals live in
the aquarium. It also tops off aquariums with a [level sensor](top_off.md). Where it drains an aquarium, an intake guard
inside keeps the young animals in.
The tank log notes every change. With power costs (`power.costs` `EASY` or `HARD`) it needs FE (8 FE per 100 mB).

## Obtaining

Crafting:

<RecipesFor id="bettacraft:water_change_controller" />
<RecipesFor id="bettacraft:drain" />
