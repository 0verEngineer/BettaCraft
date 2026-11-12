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
* An aquarium can have **any shape** (L, T, steps ...) whose blocks touch face to face, from 1 block up to a bounding
  box of 16x16x16 blocks (servers can allow up to 64). See [Shapes](aquarium/shapes.md).
* Two aquariums only join if they are made of the same glass and have the same [interior scale](interior.md).
* Dark edge lines are drawn only on the outer edges of the whole aquarium, never between its blocks.
* A new aquarium faces the player who placed it; the front (for the name sign) can be changed in the menu.
* Pistons cannot push tank blocks; they are as blast resistant as glass.

## Breaking

You can break tank blocks like glass, **except while animals live in the aquarium** or the bubble nest still holds eggs
or larvae: the message tells you what is still inside. Take the animals out first, they would die on land.

The water in a broken block simply disappears (nothing floods), the rest of the aquarium keeps its share. The interior
blocks behind the broken block drop as items (more than 27 stacks come packed into chests). Breaking a block in the
middle can split the aquarium into several smaller ones; each keeps the water and the interior of its own blocks.

## More

* [Shapes](aquarium/shapes.md)
* [Water](aquarium/water.md)
* [Inhabitants](aquarium/inhabitants.md)
* [Temperature](aquarium/temperature.md)
* [Tank menu, name sign and lock](aquarium/menu.md)
