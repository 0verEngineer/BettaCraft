---
navigation:
  title: Turtle breeding
  icon: bettacraft:slider_turtle_spawn_egg
  parent: terrarium.md
  position: 55
---

# Turtle breeding

All turtles of the mod breed in the tank: the [musk turtle](musk_turtle.md), the [slider](slider_turtle.md),
[Hermann's tortoise](hermann_tortoise.md) and [Reeves' turtle](reeves_turtle.md).

## Laying

A grown **female** lays when a grown **male** of her species lives in the same tank, she ate within the last two days,
she is not in brumation and nothing harms or stresses her much. She walks to a **nest**: a spot on the **land** whose top
is **sand or soil** (sand, red sand, dirt, grass, podzol, mud, coarse dirt, moss ...; the block tag
`#bettacraft:turtle_nest_ground`), with room above it. A sand substrate on the floor counts where the floor is land (a dry
terrarium). Stone, gravel, wood or land under water: no nest, no eggs (the log says so once a day).

There she lays **2 to 6 eggs** (by species), half buried: you see them in the miniature, and as a block in the
[interior](../interior.md). Then she waits 4 game days (`terrarium.clutchIntervalDays`).

She does not lay while her hatchlings would **overcrowd** the tank (the stocking counts water and land, the eggs too).
When they hatch, only as many come out as the tank's hard limit allows; the others die in the egg.

## Incubation and the sex of the hatchlings

Turtles have **no sex chromosomes**: the **temperature of the nest** decides whether a hatchling becomes a female or a
male (temperature-dependent sex determination). What counts is the middle third of the incubation; the climate tab shows
the clutches, how far they are, the nest temperature and what it makes of them.

The nest has the temperature of the **ground**: with [heat mats](heat_mat.md) under the tank warmer than the air, without
them the air. Warmer also hatches faster.

| Species | Males (90 % or more) | Half and half | Females (90 % or more) | Incubation at the pivot |
|---|---|---|---|---|
| Slider | below 28 °C | 29.2 °C | above 30.4 °C | 3.5 days |
| Hermann's tortoise | below 30.3 °C | 31.5 °C | above 32.7 °C | 4 days |
| Reeves' turtle | below 27.3 °C | 28.5 °C | above 29.7 °C | 3.5 days |
| Musk turtle | 22.2 to 25.8 °C | 21 and 27 °C | below 19.8 and above 28.2 °C | 3 days |

The musk turtle has the pattern of its genus (*Sternotherus*): females at **both** ends, males in the middle. The others:
cool makes males, warm makes females. Below 18 °C the eggs do not develop (they wait); above 36 °C the embryos die;
water over the nest drowns them. A player who breaks the eggs in the interior destroys them. Each clutch has a fixed seed:
it always gives the same young. The config `terrarium.incubationSpeed` speeds it up.

Every hatchling takes the variety of its mother or its father (the slider: red-eared or yellow-bellied). A hatchling is
small (about a third of a grown one) and grows up in 4 to 8 game days; its sex shows when it is grown (the tooltip of the
bucket).

## Sea turtle hatchlings

The vanilla **sea turtle** is far too big for any tank. Its **hatchlings** fit: catch one with the
[fish bag](../equipment/fish_bag.md) on the beach and put it into a paludarium (20 to 30 °C water, some land).
When it grows up it leaves the tank in a fish bag in front of it, like every animal that outgrows its tank. A grown sea
turtle is refused.

## Greens

See [Hermann's tortoise](hermann_tortoise.md#greens): `#bettacraft:turtle_greens` (dandelion, pink petals, short grass,
fern, oxeye daisy).

## Settings

Common config `features.turtles.breeding` (off: no new clutches; laid ones still hatch). Server config
`terrarium.incubationSpeed`, `terrarium.clutchIntervalDays`, `terrarium.brumation`, `terrarium.brumationBelow`.
