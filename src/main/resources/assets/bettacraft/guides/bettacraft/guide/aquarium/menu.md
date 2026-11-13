---
navigation:
  title: Tank menu
  icon: minecraft:name_tag
  parent: aquarium.md
  position: 40
---

# Tank menu, name sign and lock

**Right-click** an aquarium with an empty hand to open its menu. (**Sneak + right-click** enters the
[interior](../interior.md) instead.)

## Overview

![The overview tab](../images/menu_overview.png)

* **Name** (up to 32 characters) and the **name sign**: on or off, its position (top or bottom, left, centre or
  right) and the **front** of the aquarium it is drawn on. The sign is black with white text, 0.12 blocks high (each player
  can make it bigger or smaller with the client config `render.signScale`, 0.5 to 2); long names get a smaller font. The name also shows in the menu and in Jade when the sign is off.
* **Lock**: the player who placed the aquarium is its owner. When it is locked, only the owner and operators can
  change it (water, animals, settings, interior); everyone can still look.
* **Break protection** (the button between the name and the lock; owner and operators only, off for a new aquarium):
  while it is on **nobody can break the aquarium**, not even the owner and not in creative mode: the glass, the lid, the
  light and the feeders on it, the clip-on feeder on its glass and the cabinet under it. Trying it shows "Break
  protection is on". Explosions and pistons leave it alone too (server config `tank.protectFromExplosionsAndPistons`,
  on by default). Config switch of the whole feature: `features.breakProtection`.
* Size, water volume, water and surrounding temperature, the **target temperature** (– / +).
* Heaters (or coolers, when the target is below the surroundings), feeder, light and filters, the number of animals
  and the button to enter the interior. The last line warns about poor water or missing power.

## Animals

The list of all animals with name, type, sex and stage. The details of the selected fish: health and fed bars, stage
progress, the genotype (tail, body, fins, pattern; all nine loci in the tooltip), the generation, a name field and the
buttons to take it out with a bucket or a fish bag. A dwarf clawed frog shows its health, its food and its air.

![The animals tab](../images/menu_inhabitants.png)

The button **Small animals** switches to the [shrimp, crayfish and snails](../invertebrates.md): every kind (species
and colour) with its count, sexes, young, females with eggs and grade; for the whole aquarium the stocking, the algae
on the glass, how much the cleaning crew grazes, the food and the hiding spots. **Into bag** takes the animal with the
best grade into a fish bag (Shift: the worst, Ctrl: a full bag of 16), **Clutches off** takes the apple snail clutches off the glass.

![The small animals page](../images/menu_critters.png)

## Breeding

Warnings, the bubble nest, the breeding conditions with ✔ and ✘, the young fish, the parents and the expected offspring.
See [Breeding](../breeding.md).

## Water

![The water tab](../images/menu_water.png)

Water volume, the [water quality](water_quality.md) with a bar (the red mark is where the bettas start to suffer), how
fast it drops and why (animals per water block, how much the filters remove), the last fresh water, evaporation and
open top blocks, heaters and coolers, and the stored [power](../equipment/power.md) when the server needs it.

## Plants

![The plants tab](../images/menu_plants.png)

The [floating plants](floating_plants.md) of the aquarium: number per species, cover, light, growth, waste uptake, the
warning for a dense cover and the **Thin out** button.

## Interior

Scale and size of the interior, how many blocks are shown in the miniature, substrate, heaters and filters inside,
floating plants, the decoration catalogue and the button to enter.

## Log

What happened in the aquarium: feeding, eggs, fights, sickness, deaths and their causes, with the in-game day. It keeps
100 entries and can be filtered (all, breeding, health, feeding).
