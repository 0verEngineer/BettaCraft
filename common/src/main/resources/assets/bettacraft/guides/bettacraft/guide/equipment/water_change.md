---
navigation:
  title: Water change controller
  icon: bettacraft:water_change_controller
  parent: equipment.md
  position: 45
item_ids:
  - bettacraft:water_change_controller
---

# Water change controller

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:water_change_controller" p:facing="south" />
</GameScene>

Aquariums need **water changes** because [nitrate](../aquarium/nitrate.md) builds up. By hand: take water out with an
empty bucket and pour fresh water in (25 % of a 4-block aquarium is one bucket). Fresh water has the temperature of
the place, so a big change is a cool water change (corydoras love it).

The <ItemLink id="bettacraft:water_change_controller" /> does it by itself. Place it **next to an aquarium** and
right-click it:

* **interval** 1 to 60 days (default: what the difficulty asks for, 14 days) and **share** 5 to 50 % (default 25 %);
* **automatic on/off** and **change now**.

When it is due, its drain pump lets the old water go down the drain and fills the same amount of fresh water from a
[reservoir](top_off.md), a [pipe](pipes.md) or another mod's tank **next to it**, or endlessly from a **water source
block** next to it. It only drains what it can refill, and never below half full while animals live in the aquarium.
The tank log notes every change. With power costs (`power.costs` `EASY` or `HARD`) it needs FE (8 FE per 100 mB).

## Obtaining

Crafting:

<RecipesFor id="bettacraft:water_change_controller" />
