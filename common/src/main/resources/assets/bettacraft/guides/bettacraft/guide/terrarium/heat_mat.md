---
navigation:
  title: Heat mat
  icon: bettacraft:heat_mat
  parent: terrarium.md
  position: 10
item_ids:
  - bettacraft:heat_mat
---

# Heat mat

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:heat_mat" p:facing="north" p:heating="true" />
</GameScene>

The <ItemLink id="bettacraft:heat_mat" /> is a thin heating foil that sticks to the **outside of the glass**: click the
**underside** of a tank block (the space under the tank must be free) or one of its **walls**. Under the floor it warms
the ground fully, on a wall half as much.

* It warms the **ground** over it and a part of the **air** (40 %). Mats under **half of the floor** or more give the full
  lift of 8 K (`terrarium.heatMatLift`); fewer mats give less.
* Its thermostat is the tank's **target temperature** (overview tab): it heats while the air is colder and rests when the
  air has reached it. While it heats it glows orange.
* It only works on a [terrarium or paludarium](../terrarium.md). A lid keeps more of the warmth in.
* **Energy:** with power costs (`power.costs` `EASY` or `HARD`) it uses the tank's FE like a heater, 4 FE/t while it heats
  (`power.heatMatFePerTick`).

## Obtaining

Crafting:

<RecipesFor id="bettacraft:heat_mat" />
