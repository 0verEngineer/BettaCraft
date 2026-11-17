---
navigation:
  title: Health
  icon: minecraft:glistering_melon_slice
  parent: bettas.md
  position: 20
---

# Health

Bettas in a Bettacraft aquarium have four needs: **food**, the **right temperature**, **enough room** and **clean
water**.
The tank menu shows a health bar and a "fed" bar for each fish, Jade shows both when you look at a fish.

## What makes a betta sick

| Stress | Cause |
|---|---|
| Hunger | no food for 7 days (a fish counts as hungry from one day after its last meal) |
| Temperature | 7 days at a temperature outside 24 to 30 °C |
| Overcrowding | 7 days with 2 or more adult animals per water block |
| Water quality | 7 days with bad water (below 35 %, see [Water quality](../aquarium/water_quality.md)) |

The temperature, overcrowding and water stress only grows while the condition is bad and goes down by 2 days for every
good day. When two or more conditions are bad at the same time, all stress grows **twice as fast**.

## Sick fish

A sick betta looks pale, keeps its fins close and swims slower. It stays sick while anything is wrong; every good day
removes 2 sick days. After **14 days of sickness** it dies. The health bar goes from 100 % (healthy) to 50 % (just got
sick) down to 0 %.

When a betta dies it disappears, players within 32 blocks get a chat message and the tank log names the cause. There is
no drop.

## Wounds

Bettas have 6 health points. Fights, a male guarding his nest and predators hurt them. A fed, healthy betta heals
2 points per day.

## Old age

Dying of old age is off by default. If a server turns it on (`health.oldAge.enabled`), bettas and tankmates die after
`health.oldAge.years` in-game years (2) of `health.oldAge.daysPerYear` days (96), so after 192 days in an aquarium. The
age counts like every other timer: while the fish lives in an aquarium whose chunk is loaded. A fish from before 1.26.0
starts at the age of its life stage (a grown fish at the age at which it grew up). The death shows in the tank log with
the cause "old age". With `care.mode` NO_DEATH or DECOR nothing dies of old age. Shrimp, snails, amphibians, reptiles,
turtles, the animals of other mods and the mobs in glass do not age to death.

## Time while you are away

By default time stands still for aquariums in unloaded chunks: nothing gets hungry or sick while nobody is near. With
`simulateUnloadedTime = true` the time is caught up when the chunk loads again, including the feeder.
