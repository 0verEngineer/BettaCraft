---
navigation:
  title: Settings and other mods
  icon: minecraft:comparator
  position: 120
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
| `inhabitants.allow`, `inhabitants.deny` | empty | animals always / never allowed in aquariums (`modid:name`, `modid:*`) |
| `inhabitants.maxHitbox` | 0.8 | largest animal (hitbox width and height in blocks) |
| `inhabitants.refuseHostile` | true | no hostile mobs in aquariums |
| `inhabitants.tadpoles` | LEAVE_AS_FROG | a grown tadpole hops out as a frog; STAY_TADPOLE = it stays a tadpole |
| `inhabitants.outgrown` | BAG_IN_FRONT | an animal that grew bigger than `maxHitbox` in the aquarium waits in a fish bag in front of it; KEEP = it stays |
| `health.daysUntilSick` | 7 | days of hunger or wrong temperature until sick |
| `health.daysSickUntilDeath` | 14 | sick days until death |
| `health.bothBadMultiplier` | 2 | faster stress with two bad conditions |
| `health.recoveryPerDay` | 2 | sick days healed per good day |
| `health.hungryAfterDays` | 1 | days after a meal until a betta is hungry again |
| `health.oldAge.enabled` | false | death of old age (`years` 2, `daysPerYear` 96) |
| `temperature.tauDays` | 0.1 | how fast the water changes its temperature |
| `temperature.toleratedMin`, `toleratedMax` | 24, 30 | water temperatures bettas tolerate (°C); the best range is `breeding.minTemp` to `maxTemp` |
| `breeding.*` | | temperatures, nest time, eggs, survival, female cooldown and damage, males eating fry, mutation, mirror training (`mirrorReadinessBonus` 1.25), days live food keeps a fish conditioned (`conditioningDays` 2) |
| `combat.*` | | `malesFight`, `fightDays`, `predatorAttacksPerDay`, `healPerDay` |
| `food.fryPerItem`, `food.cultureDays` | 20, 1 | fry per bottle, days a culture needs |
| `food.artemiaCystStackSize` | 16 | jars of artemia cysts per stack: 16, 32 or 64 (each keeps its 16 uses) |
| `stages.*Days` | | duration of the life stages |
| `interior.defaultScale` | 16 | 4, 8 or 16 interior blocks per tank block (new aquariums) |
| `interior.substrateCostsBlock` | true | substrate costs a block |
| `interior.maxRenderedBlocks` | 262144 | limit of the miniature |
| `interior.leaveSneakTicks` | 60 | how long to sneak (and look up) to leave |
| `interior.leaveLookUpDegrees` | 60 | how steeply up you have to look while sneaking to leave (0 = sneaking alone) |
| `interior.syncRange`, `preparationBudgetMs` | 96, 8 | players this close get the miniature; server time per tick for building new interiors |
| `drops.artemiaFromSandChance` | 0.02 | artemia cysts from sand |
| `waterQuality.qualityEnabled` | true | water quality on (loss per day 0.01, per animal 0.04, per overfed portion 0.01, filters remove 0.8, bad below 0.35) |
| `waterQuality.externalFilterBlocks`, `internalFilterBlocks` | 32, 16 | water blocks a filter keeps clean |
| `evaporation.evaporationEnabled` | true | open tanks lose water (`mbPerBlockPerDay` 20) |
| `power.heaterRequiresEnergy`, `filterRequiresEnergy`, `lightRequiresEnergy` | false | equipment needs FE |
| `power.heaterFePerBlock`, `coolerFePerBlock`, `filterFePerTick`, `lightFePerTick`, `bufferFe` | 8, 8, 10, 4, 100000 | FE use and storage |
| `foodBreeding.*` | | larva tub (`larvaTubDays` 2, `larvaTubYield` 3) and artemia hatchery (`hatcheryDays` 1, `hatcheryYield` 2); at most 16 per batch |
| `floatingPlants.growthSpeed`, `maxCoverage`, `denseCoverFrom`, `maxPerTank` | 1, 0.6, 0.5, 256 | [floating plants](plants/floating_plants.md): growth (0 = none), the most of the surface they cover, dense cover from, plants per aquarium |
| `floatingPlants.wasteUptake`, `breedingBonus`, `pelletsOnLeaves` | 0.04, 1.15, 0.25 | waste they take up, faster nest under them, share of pellets that stay on the leaves |
| `floatingPlants.bottleDuckweedChance`, `fishingChance` | 0.15, 0.06 | duckweed in a bottle of swamp water, a floating plant from fishing in swamps (see [Where to find what](getting_started/sources.md)) |
| `interiorPlants.plantGrowthSpeed`, `plantWasteUptake`, `plantBreedingBonus`, `hidingSpots` | 1, 0.5, 1.1, 0.6 | [aquarium plants](plants.md): growth (0 = none), waste uptake, nest bonus in a planted tank, hiding spots |
| `interiorPlants.maxPlantsPerTank`, `plantsSpread` | 48, true | plants per aquarium, side shoots and runners |
| `interiorPlants.plantFishingChance`, `bottleCuttingChance`, `mossChance` | 0.05, 0.05, 0.5 | a plant from fishing, a cutting in swamp water, Java moss from shears on moss |
| `invertebrates.maxCrittersPerTank` | 200 | shrimp, crayfish and snails per aquarium |
| `invertebrates.shrimpBreeding`, `crayfishBreeding`, `snailBreeding` | 1 | breeding speed (0 = never) |
| `invertebrates.bettasHuntShrimp` | true | bettas hunt young shrimp and crayfish |
| `invertebrates.biofilmPerWaterBlock` | 0.03 | food a water block grows on its own |
| `invertebrates.netCatchChance`, `netCooldownTicks`, `mudSnailChance` | 0.3, 40, 0.08 | catching in open water, snails in mud |
| `frogs.frogFoodShareWithBettas`, `frogAirMinutes`, `frogStarveDays` | 0.4, 2, 4 | frogs: their share of the food, air under water, days without food until they starve |
| `stocking.snailsPerFish`, `shrimpPerFish`, `crayfishPerFish`, `frogsPerFish` | 50, 25, 12, 1 | small animals per fish |
| `stocking.eggStocking`, `larvaStocking`, `fryStocking`, `juvenileStocking`, `subadultStocking` | 0, 0, 0.02, 0.1, 0.3 | how much of a fish one young animal counts (juvenile also for young animals of other mods and young small animals) |
| `algae.algaeGrowthSpeed`, `cleanerStrength` | 0.25, 1 | algae on the glass (0 = none), how much the crew grazes |
| `tankmates.breeding.tankmateBreedingSpeed`, `clutchSpeed`, `gestationDays` | 1, 1, 3 | [tankmates](tankmates.md): how readily they spawn, how fast eggs develop, days a livebearer carries |
| `tankmates.breeding.maxFryPerClutch`, `breedingBoxCapacity` | 20, 20 | fry that swim free from one clutch, young in a breeding box |
| `tankmates.breeding.waterChangeCools`, `coolChangeShare`, `coolChangeDrop` | true, 0.25, 1.5 | fresh water cools the tank; the cool water change that sets the corydoras off |
| `tankmates.behaviour.fryEatenPerDay`, `eggsEatenPerDay` | 0.5, 1.5 | how fast the adults hunt fry and eat eggs (plants and mops protect them) |
| `tankmates.behaviour.shoalStress`, `bettaAggression`, `clownLoachSnailsPerDay` | true, 1, 0.5 | stress in small shoals, male bettas against guppy and gourami males, snails eaten by clown loaches |
| `tankmates.behaviour.bottomFoodShare`, `tankmateStockingFactor`, `netFishShare` | 0.6, 1, 0.4 | food for the bottom fish, how much tankmates count for the stocking, share of fish in the net |
| `saltwater.salinityShock`, `refuseDeadlyWater` | 1, true | [reef tanks](saltwater.md): how fast the wrong salinity harms animals (0 = never); refuse animals the water would kill |
| `saltwater.oceanWaterIsSalty`, `foreignAnimals`, `vanillaCoralsNeedSalt` | true, LENIENT, false | sea water buckets from the ocean; salt and animals of vanilla / other mods (LENIENT or REALISTIC); vanilla corals die in fresh water |
| `saltwater.skimmerBlocks`, `liveRockBlocks`, `pumpFlowBlocks` | 48, 24, 24 | water blocks a protein skimmer and a tank block of live rock keep clean in salt water, water blocks a flow pump moves |
| `saltwater.coralGrowthSpeed`, `calciumPerStage`, `calciumPerDose`, `reefNetFishShare` | 1, 40, 200, 0.4 | coral growth, calcium a stony coral uses per stage and one additive adds (mg/L in one water block), share of reef fish in the net |
| `seahorses.maxFlow`, `needHitchingPosts`, `feedingsPerDay`, `foodShareWithFastFish`, `harassedByFastFish`, `pouchDays` | 0.6, on, 2, 0.35, on, 4 | [seahorses](saltwater/seahorses.md): highest flow, stress without a post, feedings a day, food share next to fast fish, tangs and damsels harass them, days in the brood pouch |
| `guide.giveBook` | ON_FIRST_CATCH_OR_CRAFT | when players get this handbook |
| `simulateUnloadedTime` | false | time passes in unloaded chunks |

The **common config** sets the spawn weight and group size of wild bettas and the fish bag recipe (`recipes.fishBag`:
AUTO, KELP or PLASTIC), which overlay mod shows the Bettacraft lines (`compat.overlayProvider`: AUTO = only the first
installed one of Jade, WTHIT and The One Probe, so two overlay mods do not show every aquarium twice; JADE, TOP, WTHIT or
ALL) and switches for single features (`features.*`): `interiorPathfinding` (frogs walk around the decoration, small
animals go under overhangs and into caves, plecos swim into their spawning cave; off = straight lines) and `plantMeshes`
(the new look of the plants and corals in the interior and the miniature; off = the blocks, for everyone), since 1.14.2
`swayingForeignPlants` (kelp, seagrass, corals and other mods' water plants sway in the miniature; the tag
`#bettacraft:swaying_plants`, other underwater plants found automatically: `swaying.autoDetect`, never sway:
`swaying.exclude` with block ids, `#tags` or `modid:*`),
since 1.13.2 `smoothSwimming` (fish turn in curves and tilt smoothly, kuhli loaches undulate and hide by day, plecos stand
up only on the glass), `greenNeonTetra` (the green neon at the trader and in the net), `snailColors` (individual shades of
the racer nerites, mostly gold apple snails with bands, red ramshorns), `detailedDecor` (the big skull and shipwreck),
`breakProtection` (the switch in the tank menu), `clipOnFeeder` and since 1.13.3 `livebearerGenetics` (the [guppy and endler genetics](tankmates/guppy_genetics.md); off = the old fixed varieties) and since 1.13.4 `stoneCabinets` ([cabinets of stone](equipment/cabinet.md); off = only wooden ones can be crafted) and since 1.14.1 `seahorses` ([seahorses](saltwater/seahorses.md); off = none from the net and the trader). The server config `tank.protectFromExplosionsAndPistons`
decides whether the break protection also stops explosions and pistons. The **client config** has the animation and miniature distances, the distance up to which the
miniature shows those plants with real leaves (`render.miniaturePlantMeshDistance`, 24; further away pixels, 0 = always
pixels), the plant look and the swaying water plants for this player alone (`render.plantMeshes`, `render.swayingPlants`;
both in the Mod settings, also on a server), the water surface glitter,
the filter sound, the connected glass textures (`render.connectedGlass`) and the width of the tank edge lines
(`render.tankEdgeWidth`, in pixels of 1/16 block, 0.1 to 1, default 0.25; 0 = none), the size of the name sign
(`render.signScale`, 0.5 to 2 times the default height of 0.12 blocks), the distance up to which the name sign, nest,
floating plants, small animals and algae are drawn (`render.tankDetailDistance`, 48), how strongly coloured lights tint
the water (`render.lightTint`, 0 to 1, default 0.6; 0 = the colour only shows at the lamp) and the leave hint in the
interior (`hud.interiorLeaveHint`).

## This handbook

The handbook needs the mod **GuideME**. Each player gets it once, depending on `guide.giveBook`:

* `ON_JOIN`: on the first join,
* `ON_FIRST_CATCH_OR_CRAFT`: with the first caught betta or the first crafted Bettacraft item,
* `NEVER`: not at all.

If the inventory is full, the book is dropped at your feet and for 60 seconds only you can pick it up. A new copy:
craft a book with betta pellets, or ask an operator for `/guideme give <player> bettacraft:guide`.

Without GuideME every Bettacraft item explains itself in its tooltip (hold Shift) and in JEI.

## Other mods

* **Jade, The One Probe and WTHIT:** (with several of them only the first one, see `compat.overlayProvider`) looking at an aquarium shows its name, temperature, water and animals (sneak for
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
* **Every other mod with water animals** (Alex's Caves, Critters and Companions, Living Things, Deeper Darker, The
  Undergarden, Shiny, Sushi Go Crafting, DragN's Just Bettas, ...): its small water animals go into aquariums, large
  and hostile ones are refused. See [Animals of other mods](settings/other_animals.md).
* **Swamps of other mods** (Biomes O' Plenty, Regions Unexplored, ...): wild bettas spawn in every biome of
  `#c:is_swamp` through `#bettacraft:betta_spawn_biomes`.

## Commands

* `/bettacraft interior leave`: leave an interior.
* Operators: `/bettacraft interior resync` (send all miniatures again), `/bettacraft interior bench` (measure),
  `/bettacraft wateranimals` (every water animal of the loaded mods and whether aquariums take it, as a table in
  `bettacraft-water-animals.tsv`).

## In this chapter

<SubPages icons={true} />
