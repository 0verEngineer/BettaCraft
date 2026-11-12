---
navigation:
  title: Light
  icon: bettacraft:tank_light
  parent: equipment.md
  position: 40
item_ids:
  - bettacraft:tank_light
---

# Aquarium light

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:tank_light" x="0" p:axis="x" p:pos="true" />
  <Block id="bettacraft:tank_light" x="1" p:axis="x" p:neg="true" p:pos="true" />
  <Block id="bettacraft:tank_light" x="2" p:axis="x" p:neg="true" />
</GameScene>

The <ItemLink id="bettacraft:tank_light" /> gives light level 15.

<RecipeFor id="bettacraft:tank_light" />

* Lights next to each other in a row join into **one long bar** (only lengthwise, not into a surface).
* Place it on top of the aquarium, on the glass rim or **hanging** under a block above the aquarium.
* It is on by default. A **redstone signal** at any part of the bar switches the whole bar off, for example with a
  daylight sensor.
* The tank menu shows how many lights are on ("Light: 4/4 on").
