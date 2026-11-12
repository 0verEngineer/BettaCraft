---
navigation:
  title: Aquarium
  icon: bettacraft:tank
  parent: index.md
  position: 30
item_ids:
  - bettacraft:tank
---

# Aquarium

The <ItemLink id="bettacraft:tank" /> is the heart of Bettacraft: a glass block that joins with its neighbours into one
aquarium of real water.

<RecipeFor id="bettacraft:tank" />

All six glass blocks of the recipe must be the same; the aquarium uses that glass. **Any glass works**, including
stained glass, tinted glass and the glass of other mods (`#c:glass_blocks`).

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:tank" x="0" p:east="true" p:fill="12" />
  <Block id="bettacraft:tank" x="1" p:west="true" p:east="true" p:fill="12" />
  <Block id="bettacraft:tank" x="2" p:west="true" p:fill="12" />
</GameScene>

## Building

* Place tank blocks next to each other: blocks of the **same glass** join into one aquarium.
* An aquarium is a **cuboid**, from 1x1x1 up to 16x16x16 blocks (servers can allow up to 64). A block that does not
  complete a cuboid forms a separate aquarium.
* Two aquariums only join if they are made of the same glass and have the same [interior scale](interior.md).
* Dark edge lines are drawn only on the outer edges of the whole aquarium, never between its blocks.
* A new aquarium faces the player who placed it; the front (for the name sign) can be changed in the menu.
* Pistons cannot push tank blocks; they are as blast resistant as glass.

## Breaking

You can break tank blocks like glass, **except while animals live in the aquarium** or the bubble nest still holds eggs
or larvae: the message tells you what is still inside. Take the animals out first, they would die on land.

The water in a broken block simply disappears (nothing floods), the rest of the aquarium keeps its share. The interior
blocks behind the broken block drop as items (more than 27 stacks come packed into chests). Breaking a block in the
middle splits the aquarium into several smaller ones.

## More

* [Water](aquarium/water.md)
* [Inhabitants](aquarium/inhabitants.md)
* [Temperature](aquarium/temperature.md)
* [Tank menu, name sign and lock](aquarium/menu.md)
