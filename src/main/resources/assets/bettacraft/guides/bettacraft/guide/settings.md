---
navigation:
  title: Settings and other mods
  icon: minecraft:comparator
  parent: index.md
  position: 80
---

# Settings and other mods

## Server config (per world)

| Option | Default | |
|---|---|---|
| `tank.maxSizeX/Y/Z` | 16 | largest aquarium (up to 64), the bounding box for free shapes |
| `tank.freeShapes` | true | any connected shape (L, T, steps); false = cuboids only |
| `tank.maxEntities` | 64 | animals per aquarium |
| `tank.hardCapPerWaterBlock` | 6 | adults per water block at most |
| `tank.overcrowdedPerWaterBlock` | 2 | adults per water block from which the fish get stressed |
| `tank.minFillToInsert` | 0.5 | fill needed to put animals in |
| `health.daysUntilSick` | 7 | days of hunger or wrong temperature until sick |
| `health.daysSickUntilDeath` | 14 | sick days until death |
| `health.bothBadMultiplier` | 2 | faster stress with two bad conditions |
| `health.recoveryPerDay` | 2 | sick days healed per good day |
| `health.oldAge.enabled` | false | death of old age (`years` 2, `daysPerYear` 96) |
| `temperature.tauDays` | 0.1 | how fast the water changes its temperature |
| `breeding.*` | | temperatures, nest time, eggs, survival, female cooldown and damage, males eating fry, mutation, mirror training (`mirrorReadinessBonus` 1.25) |
| `combat.*` | | `malesFight`, `fightDays`, `predatorAttacksPerDay`, `healPerDay` |
| `food.fryPerItem`, `food.cultureDays` | 20, 1 | fry per bottle, days a culture needs |
| `stages.*Days` | | duration of the life stages |
| `interior.defaultScale` | 16 | 4, 8 or 16 interior blocks per tank block (new aquariums) |
| `interior.substrateCostsBlock` | true | substrate costs a block |
| `interior.maxRenderedBlocks` | 262144 | limit of the miniature |
| `interior.leaveSneakTicks` | 60 | how long to sneak to leave |
| `drops.artemiaFromSandChance` | 0.02 | artemia cysts from sand |
| `waterQuality.qualityEnabled` | true | water quality on (loss per day 0.01, per animal 0.04, per overfed portion 0.01, filters remove 0.8, bad below 0.35) |
| `waterQuality.externalFilterBlocks`, `internalFilterBlocks` | 32, 16 | water blocks a filter keeps clean |
| `evaporation.evaporationEnabled` | true | open tanks lose water (`mbPerBlockPerDay` 20) |
| `power.heaterRequiresEnergy`, `filterRequiresEnergy`, `lightRequiresEnergy` | false | equipment needs FE |
| `power.heaterFePerBlock`, `filterFePerTick`, `lightFePerTick`, `bufferFe` | 1, 10, 4, 100000 | FE use and storage |
| `foodBreeding.*` | | larva tub (`larvaTubDays` 2, `larvaTubYield` 3) and artemia hatchery (`hatcheryDays` 1, `hatcheryYield` 2) |
| `guide.giveBook` | ON_FIRST_CATCH_OR_CRAFT | when players get this handbook |
| `simulateUnloadedTime` | false | time passes in unloaded chunks |

The **common config** sets the spawn weight and group size of wild bettas and the fish bag recipe (`recipes.fishBag`:
AUTO, KELP or PLASTIC). The **client config** has the animation and miniature distances, the water surface glitter,
the filter sound, the connected glass textures (`render.connectedGlass`) and the width of the tank edge lines
(`render.tankEdgeWidth`, in 1/64 block, 0 = none).

## This handbook

The handbook needs the mod **GuideME**. Each player gets it once, depending on `guide.giveBook`:

* `ON_JOIN`: on the first join,
* `ON_FIRST_CATCH_OR_CRAFT`: with the first caught betta or the first crafted Bettacraft item,
* `NEVER`: not at all.

If the inventory is full, the book is dropped at your feet and for 60 seconds only you can pick it up. A new copy:
craft a book with betta pellets, or ask an operator for `/guideme give <player> bettacraft:guide`.

Without GuideME every Bettacraft item explains itself in its tooltip (hold Shift) and in JEI.

## Other mods

* **Jade, The One Probe and WTHIT:** looking at an aquarium shows its name, temperature, water and animals (sneak for
  the list; The One Probe needs its probe for that), the nest, filters and warnings; looking at a betta through the
  glass shows its type, sex, health, hunger, stage and genes. Filters and cabinets show their heaters and coolers and
  the aquarium they pump into; aquariums show the water quality and the stored power, larva tubs and hatcheries their
  batch.
* **Energy mods:** every mod with Forge Energy (FE) cables can power aquariums when a server turns on `power.*`; pipes
  for water can fill and drain aquariums and fill the larva tub, hoppers and item pipes work with the feeder, the
  cabinet, the larva tub and the hatchery.
* **JEI:** information pages for all Bettacraft items.
* **Sodium and Iris:** the aquarium water and the miniatures work with both.
* **Better Fish Tanks:** its *Fish Bag* puts animals into Bettacraft aquariums and takes them out through the menu.
  The other way round, bettas fit in its bag and its fish tanks; a filled Bettacraft fish bag or a betta bucket
  releases its animal into a Better Fish Tanks aquarium with a right-click.
* **Fintastic:** its fish and the *Fishnet* work in both directions; its fish feed feeds bettas, fairy shrimp, daphnia
  and copepods are live prey, and its water plants (also duckweed and lotus pads) can be placed in the dry interior.
  Arapaima and coelacanth are too big for an aquarium.
* **Fish of Thieves:** its fish live in Bettacraft aquariums (battlegill, wrecker and devilfish are predators), and a
  betta bucket puts a betta on its *Fish Plaque*. Earthworms are live food.
* **Aquaculture 2:** its small fish are welcome, the large ones (tuna, arapaima, halibut, catfish, carp, ...) are not;
  the piranha is a predator. Worms are live food, its raw fish and fillets make betta pellets.
* **Naturalist:** bass, catfish and other small animals are accepted; piranha and anglerfish are predators.
* **Swamps of other mods** (Biomes O' Plenty, Regions Unexplored, ...): wild bettas spawn in every biome of
  `#c:is_swamp` through `#bettacraft:betta_spawn_biomes`.

## Commands

* `/bettacraft interior leave`: leave an interior.
* Operators: `/bettacraft interior resync` (send all miniatures again), `/bettacraft interior bench` (measure).
