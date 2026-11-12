---
navigation:
  title: Health
  icon: minecraft:glistering_melon_slice
  parent: bettas.md
  position: 20
---

# Health

Bettas in a Bettacraft aquarium have three needs: **food**, the **right temperature** and **enough room**.
The tank menu shows a health bar and a "fed" bar for each fish, Jade shows both when you look at a fish.

## What makes a betta sick

| Stress | Cause |
|---|---|
| Hunger | no food for 7 days (a fish counts as hungry from one day after its last meal) |
| Temperature | 7 days at a temperature outside 24 to 30 °C |
| Overcrowding | 7 days with 2 or more adult animals per water block |

The temperature and overcrowding stress only grows while the condition is bad and goes down by 2 days for every good
day. When two or more conditions are bad at the same time, all stress grows **twice as fast**.

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

Dying of old age is off by default. If a server turns it on (`health.oldAge.enabled`), bettas die after 2 in-game years
of 96 days each.

## Time while you are away

By default time stands still for aquariums in unloaded chunks: nothing gets hungry or sick while nobody is near. With
`simulateUnloadedTime = true` the time is caught up when the chunk loads again, including the feeder.
