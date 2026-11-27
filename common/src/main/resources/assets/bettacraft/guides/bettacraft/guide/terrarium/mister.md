---
navigation:
  title: Mister
  icon: bettacraft:mister
  parent: terrarium.md
  position: 30
item_ids:
  - bettacraft:mister
---

# Mister

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:mister" p:facing="north" />
</GameScene>

The <ItemLink id="bettacraft:mister" /> (a fogger) sprays a fine mist into a [terrarium or paludarium](../terrarium.md).
Put it **on a top block** or **beside the tank**.

* It is a **hygrostat**: it sprays while the humidity is below its setting. **Right-click** raises the setting by 5 %,
  **sneak + right-click** lowers it (40 to 95 %, 75 % at first); the message also tells its water and the humidity now.
* A spraying mister adds up to 40 % humidity in a closed tank (`terrarium.misterHumidity`); an open top lets most of it
  out. Two or three misters spray a little more.
* It holds **4 buckets of water** and uses 2 mB per second while it sprays (`terrarium.misterMbPerSecond`). Fill it with a
  bucket, by [pipes](../equipment/pipes.md) or another mod's pipes, or put it next to a reservoir or a water source block:
  it refills itself from there. A comparator reads how full it is.
* **Energy:** with power costs (`power.costs` `EASY` or `HARD`) it uses the tank's FE while it sprays (6 FE/t).

## Obtaining

Crafting:

<RecipesFor id="bettacraft:mister" />
