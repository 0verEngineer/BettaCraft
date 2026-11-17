---
navigation:
  title: Basking lamp
  icon: bettacraft:basking_lamp
  parent: terrarium.md
  position: 20
item_ids:
  - bettacraft:basking_lamp
---

# Basking lamp

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:basking_lamp" p:lit="true" />
</GameScene>

The <ItemLink id="bettacraft:basking_lamp" /> is a heat and UV lamp in a reflector dome. Put it on a **top block** of the
tank (or hang it under a ceiling block right above it). It is a light like the [aquarium lights](../equipment/light.md):
light 15, the tank's **dimmer and daylight timer**, redstone switches it off, and with `power.lightRequiresEnergy` it
needs the tank's FE.

On a [terrarium or paludarium](../terrarium.md), while it is lit:

* the land under it becomes a **basking spot**, 12 K warmer than the air (`terrarium.baskingSpotLift`); terrarium animals
  go there to warm up;
* it gives **UV 5** at the basking spot (`terrarium.baskingLampUv`);
* it warms the air a little (up to 4 K with lamps over a quarter of the top, `terrarium.baskingLampAir`).

A basking spot needs **land under the lamp**: over water there is none.

## Obtaining

Crafting:

<RecipesFor id="bettacraft:basking_lamp" />
