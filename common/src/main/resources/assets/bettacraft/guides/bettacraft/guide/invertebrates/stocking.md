---
navigation:
  title: Stocking
  icon: bettacraft:tank
  parent: invertebrates.md
  position: 50
---

# Stocking

The stocking limits of an aquarium count fish: from 2 adults per water block the bettas get stressed, at most 6 per
water block fit in (`tank.overcrowdedPerWaterBlock`, `tank.hardCapPerWaterBlock`). Small animals count in fractions:

| Animals | count as one fish | Config |
|---|---|---|
| snails | 50 | `stocking.snailsPerFish` |
| shrimp | 25 | `stocking.shrimpPerFish` |
| dwarf crayfish | 12 | `stocking.crayfishPerFish` |
| dwarf clawed frogs | 1 | `stocking.frogsPerFish` |

* The young count at the share of a juvenile betta (`stocking.juvenileStocking`, 0.1) of that: 25 young shrimp are
  a tenth of a fish. Young bettas: see [Inhabitants](../aquarium/inhabitants.md).
* The water gets their waste too (in the same fractions, young at half).
* Independent of the stocking a tank holds at most 200 shrimp, crayfish and snails (`invertebrates.maxCrittersPerTank`);
  there they stop breeding. They also stop when the whole stocking (fish and small animals) reaches the hard cap.
* The overview of the [tank menu](../aquarium/menu.md) and Jade, The One Probe and WTHIT show the stocking.
