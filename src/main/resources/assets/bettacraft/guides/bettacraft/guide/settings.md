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
| `waterchange.difficulty` | NORMAL | [nitrate](aquarium/nitrate.md): OFF, EASY (25 % every 20 days), NORMAL (14 days), REALISTIC (7 days) |
| `waterchange.nitrateStressFrom`, `nitrateLethalFrom`, `plantUptake`, `referenceFishPerBlock` | 50, 120, 1.2, 0.5 | less breeding and more algae from, bad water from, uptake of dense plants, a normally stocked tank |
| `chemistry.tdsPerDay`, `remineraliserPpm`, `leafBlocks`, `leafHalfLifeDays` | 0.6, 400, 1.0, 7 | [water chemistry](aquarium/water_chemistry.md): TDS from food and waste per day, TDS of a remineralising salt per bucket, water blocks an almond leaf colours, days until half of its tannins are gone |
| `co2Dosing.co2PerBubble`, `co2StressFrom`, `co2LethalFrom`, `co2Breath`, `co2PlantUptake` | 150, 35, 50, 2, 30 | [CO2 dosing](equipment/co2.md): mg/L a day one bubble per second gives a water block, CO2 that stresses and that harms the animals, breath per animal, uptake of a densely planted aquarium |
| `co2Dosing.bioCo2Bubbles`, `bioCo2Days`, `cylinderBubbleDays`, `stationTicksPerCylinder`, `stationFe`, `foreignCo2Mb` | 1, 4, 60, 24000, 20, 1000 | bubbles and days of bio CO2, days a cylinder lasts at one bubble per second, ticks of burning to fill one, FE of the compressor, mB of another mod's CO2 per cylinder |
| `osmosis.roRate`, `roPumpBoost`, `roPumpFe`, `roWastePerPart`, `roMembraneBuckets`, `roPrefilterBuckets` | 1, 4, 20, 3, 128, 32 | [reverse osmosis unit](equipment/reverse_osmosis.md): mB per tick, how much faster with FE, FE per tick, waste water per part, life of membrane and prefilter |
| `dosing.calciumPerMl`, `alkalinityPerMl`, `magnesiumPerMl`, `tracePerMl`, `magnesiumDropPerDay`, `traceUsePerDay`, `canisterMl`, `maxMlPerDay` | 2, 0.07, 4, 0.15, 1, 0.04, 500, 100 | [dosing pump](saltwater/dosing_pump.md) and [reef chemistry](saltwater/reef_chemistry.md): what one mL adds to a water block, magnesium loss and trace element use per day, mL of a canister, most mL per day of a channel |
| `pipes.glassPipeRate`, `steelPipeRate`, `pumpRate` | 50, 250, 200 | [pipes and the water pump](equipment/pipes.md) in mB per tick |
| `pipes.pumpsRequireEnergy`, `pumpFePerTick` | false, 8 | water pumps, filter pump heads, water change controllers and level sensors need FE |
| `crucible.meltTicks`, `fePerTick` | 200, 20 | [crucible](equipment/crucible.md): time of a melt, FE instead of fuel |
| `multiblockFilter.spongeBlocks`, `ceramicBlocks`, `carbonBlocks`, `peatBlocks`, `dirtPerDay`, `maxChambers` | 12, 24, 16, 6, 0.04, 16 | [multiblock filter](equipment/multiblock_filter.md): water blocks per medium, how fast media clog, chambers at most |
| `power.heaterRequiresEnergy`, `filterRequiresEnergy`, `lightRequiresEnergy` | false | equipment needs FE |
| `power.heaterFePerBlock`, `coolerFePerBlock`, `filterFePerTick`, `lightFePerTick`, `bufferFe` | 8, 8, 10, 4, 100000 | FE use and storage |
| `foodBreeding.*` | | larva tub (`larvaTubDays` 2, `larvaTubYield` 3) and artemia hatchery (`hatcheryDays` 1, `hatcheryYield` 2); at most 16 per batch |
| `liveFood.*` | | [tubifex dish](food/tubifex_dish.md) (`tubifexDays` 4, `tubifexRoom` 8, `tubifexConditioning` 2, `poorTubifexStress` 0.5), [daphnia barrel](food/daphnia_barrel.md) (`daphniaDays` 3, `daphniaRoom` 16, `crowdedDays` 1, `bottleDaphniaChance` 0.08), [live food pump](food/live_food_pump.md) (`pumpFePerPortion` 200) |
| `foodMaking.*` | | [food maker](food/food_maker.md): `ticksPerBatch` 200, `springTicks` 24000 (a full spring), `windsToFull` 8, `makerFePerTick` 10 |
| `monitor.*` | | [monitors](equipment/monitor.md): `range` 32 (blocks to the tanks), `fePerBlock` 1 (FE per tick and block while a power option is on) |
| `diet.*` | | [food variety](food/food_variety.md): `varietyDays` 3, `varietyGroups` 3, `varietyRecovery` 1.5, `poorFoodFedDays` 0.5, `shrimpFoodFactor` 2 |
| `floatingPlants.growthSpeed`, `maxCoverage`, `denseCoverFrom`, `maxPerTank` | 1, 0.6, 0.5, 256 | [floating plants](plants/floating_plants.md): growth (0 = none), the most of the surface they cover, dense cover from, plants per aquarium |
| `floatingPlants.wasteUptake`, `breedingBonus`, `pelletsOnLeaves` | 0.04, 1.15, 0.25 | waste they take up, faster nest under them, share of pellets that stay on the leaves |
| `floatingPlants.bottleDuckweedChance`, `fishingChance` | 0.15, 0.06 | duckweed in a bottle of swamp water, a floating plant from fishing in swamps (see [Where to find what](getting_started/sources.md)) |
| `interiorPlants.plantGrowthSpeed`, `plantWasteUptake`, `plantBreedingBonus`, `hidingSpots` | 1, 0.5, 1.1, 0.6 | [aquarium plants](plants.md): growth (0 = none), waste uptake, nest bonus in a planted tank, hiding spots |
| `interiorPlants.maxPlantsPerTank`, `plantsSpread` | 48, true | plants per aquarium, side shoots and runners |
| `interiorPlants.plantFishingChance`, `bottleCuttingChance`, `mossChance` | 0.05, 0.05, 0.5 | a plant from fishing, a cutting in swamp water, Java moss from shears on moss |
| `interiorPlants.netPlantShare`, `netPlantsRiver`, `netPlantsJungle`, `netPlantsSwamp` | 0.2, lists | share of the [net's](equipment/tank_net.md) catches in rivers, jungle water and swamps that is a plant, and the plants per water (`plant_id=weight`) |
| `interiorPlants.fishingPlantsJungle`, `fishingPlantsRiver`, `fishingPlantsSwamp`, `fishingPlantsMangrove` | lists | the plants fishing brings up per water (`plant_id=weight`; see [Where to find what](getting_started/sources.md)) |
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
| `foreign.foreignBreedingSpeed`, `foreignHealthSpeed` | 1, 1 | [animals of other mods](settings/foreign_profiles.md) with a profile: their breeding speed (0 = never), how fast their hunger and sickness run |
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
`breakProtection` (the switch in the tank menu), `clipOnFeeder` and since 1.13.3 `livebearerGenetics` (the [guppy and endler genetics](tankmates/guppy_genetics.md); off = the old fixed varieties) and since 1.13.4 `stoneCabinets` ([cabinets of stone](equipment/cabinet.md); off = only wooden ones can be crafted) and since 1.14.1 `seahorses` ([seahorses](saltwater/seahorses.md); off = none from the net and the trader) and since 1.15.1 `plantNetCatches` (aquarium plants from the [net](equipment/tank_net.md); off = plants only from fishing, bottles, shears and the trader) and `cabinetOutletSide` (the [outlet side](equipment/cabinet.md) of the cabinet filter; off = always the back wall, even flow) and since 1.16.0 `oxygen` ([oxygen](aquarium/oxygen.md)), `aeration`
([air pump and air stones](equipment/air_pump.md)), `moreInternalFilters` (sponge and corner filter, intake sponge),
`fryIntake` (motor filters suck in fry), `powerRegulator` ([power regulator](equipment/regulator.md)), `ledTiers`
([LED panel and high-power LED](equipment/light.md)) and `lightTimer` (dimmer and daylight timer) and since 1.17.0 `stainlessSteel` ([crucible](equipment/crucible.md)), `pipes` ([pipes and the water pump](equipment/pipes.md)), `interiorPipes` ([bulkhead and interior pipes](equipment/bulkhead.md)), `multiblockFilter` ([multiblock filter](equipment/multiblock_filter.md)), `autoWaterChange` ([water change controller](equipment/water_change.md)) and `topOff` ([level sensor](equipment/top_off.md)) and since 1.18.0 `waterChemistry` ([water chemistry](aquarium/water_chemistry.md); off = the values stand still and no animal minds them), `co2` ([CO2 dosing](equipment/co2.md); off = the devices give no CO2), `reverseOsmosis` ([reverse osmosis unit](equipment/reverse_osmosis.md); off = it does not run), `caridina` ([bee shrimp](invertebrates/caridina.md)), `sulawesi` ([cardinal shrimp](invertebrates/sulawesi_shrimp.md) and [Tylomelania](invertebrates/tylomelania.md); off = no new ones from the net, the wild and the trader) and `reefDosing` ([reef chemistry](saltwater/reef_chemistry.md) and the [dosing pump](saltwater/dosing_pump.md); off = the values stand still and the pump does not dose) and since 1.19.0 `liveFoodCultures` ([tubifex dish](food/tubifex_dish.md) and [daphnia barrel](food/daphnia_barrel.md); off = the cultures stand still) and `liveFoodPump` ([live food pump](food/live_food_pump.md); off = it does not pump and tanks take no live food from pipes), `foodMaker` ([food maker](food/food_maker.md); off = it stands still) and `foodVariety` ([food variety](food/food_variety.md); off = every food is the same) and since 1.20.0 `monitors` ([monitors](equipment/monitor.md); off = the screens stay dark and record nothing) and since 1.25.0 `foreignCare`, `foreignBreeding` and `suppressForeignBreeding` ([animals of other mods](settings/foreign_profiles.md): our needs and our breeding for the animals with a profile, their own breeding suppressed in the tank; off = left to their mod). The common config
`sounds.useVanilla` switches the device sounds back to Minecraft's own; the client config has a volume per kind of
device (`sound.filterVolume`, `airPumpVolume`, `airStoneVolume`, `flowPumpVolume`, `feederVolume`); the hum of the air pumps
and the whir of the flow pumps are off until `sound.airPumpSound` or `sound.flowPumpSound` is switched on. The client
command `/bettacraft sounds` plays every device sound once, one after another, with its name in the chat. The server config `tank.protectFromExplosionsAndPistons`
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
* **Our care and breeding for their animals:** Fintastic's guppy, gourami, pleco, small catfish, minnow, moony, cod and
  salmon, Aquaculture's small fish, the koi of Critters and Companions and Naturalist's bass, catfish and piranha have a
  profile: in a Bettacraft aquarium they get hungry, mind the water and breed by our rules, and their own breeding is
  suppressed there. Modpacks can add profiles for any animal, see [Profiles for animals of other mods](settings/foreign_profiles.md).
* **Every other mod with water animals** (Alex's Caves, Critters and Companions, Living Things, Deeper Darker, The
  Undergarden, Shiny, Sushi Go Crafting, DragN's Just Bettas, ...): its small water animals go into aquariums, large
  and hostile ones are refused. See [Animals of other mods](settings/other_animals.md).
* **Biome mods** (Regions Unexplored, Oh The Biomes We've Gone, Biomes O' Plenty, Terralith, Twilight Forest, ...): the
  wild animals, the net, fishing and the wild corals go by biome tags of Bettacraft, so the swamps, rivers, mangroves,
  reefs and lush caves of biome mods count too. Wild bettas spawn in `#bettacraft:betta_spawn_biomes` (`#c:is_swamp`).

### Biome tags for modpacks

Add biomes to these tags with a data pack (or `"replace": true` for a list of your own). The checks run in this order;
the first that fits decides.

| Tag | Water | Default |
|---|---|---|
| `#bettacraft:mangrove_waters` | Mangrove swamp | `minecraft:mangrove_swamp`, Oh The Biomes We've Gone white mangrove marshes |
| `#bettacraft:swamp_waters` | Swamp | `minecraft:swamp`, `#c:is_swamp`, the Twilight Forest swamp |
| `#bettacraft:lush_cave_waters` | Lush caves (net) | `minecraft:lush_caves`; also every biome in both `#c:is_lush` and `#c:is_cave` |
| `#bettacraft:cold_waters` | Cold lakes (wild Marimo, hairgrass, hornwort) | `minecraft:frozen_river`, the taigas and snowy plains; also every land biome in `#c:is_cold/overworld` |
| `#bettacraft:river_waters` | River | `#minecraft:is_river`, the Twilight Forest stream |
| `#bettacraft:beach_waters` | Beach | `#minecraft:is_beach` |
| `#bettacraft:warm_ocean_waters` | Warm ocean (reef) | `minecraft:warm_ocean`, the Regions Unexplored rocky reef; also every biome in both `#c:is_ocean` and `#c:is_hot` |
| `#bettacraft:lukewarm_ocean_waters` | Lukewarm ocean | `minecraft:lukewarm_ocean`, `minecraft:deep_lukewarm_ocean` |
| `#bettacraft:jungle_waters` | Jungle water | `#minecraft:is_jungle`, the tropics of Regions Unexplored and Biomes O' Plenty |
| `#bettacraft:coral_reef_biomes` | Wild corals (new chunks) | `#bettacraft:warm_ocean_waters`; also `#c:is_ocean` and `#c:is_hot` |

Plants are fished in `#bettacraft:swamp_waters` and `#bettacraft:betta_spawn_biomes`. Spawns and corals are set when
the world loads, a changed tag needs a restart. Entries of other mods are optional: without the mod they are skipped.

### Wild animals in the world

The wild fish, frogs and small animals belong to the vanilla spawn group *water ambient*, like cod, salmon and tropical
fish. Its cap is 20 per player, and with many fish mods (Fish of Thieves, Naturalist, ...) the fish of the oceans nearby
often fill it. So Bettacraft also has its own frugal spawn pass (common config `features.wildSpawnPass`, on): every
`wildlife.spawnPassInterval` ticks (200 = 10 seconds) each player gets one try in fitting water 24 to 48 blocks away,
while fewer than `wildlife.spawnPassPerPlayer` (10) wild animals of the mod live within 48 blocks. The usual rules stay:
a few per 32 blocks (`wildlife.wildFishNearby` 6, `wildCrittersNearby` 8), never in an aquarium, and far from the player
they vanish again like vanilla fish. The weights are `spawn.wildFishWeight` and `spawn.wildCritterWeight` (common config).

All animals of the mod are in the entity tag `#bettacraft:all_animals`, the ones that spawn by themselves in
`#bettacraft:wild_animals`, for example `/effect give @e[type=#bettacraft:all_animals,distance=..64] minecraft:glowing 30`.

### Wild plants in the world

Every aquarium and floating plant grows wild in new chunks (common config `features.wildPlants`, on): at most one try per
chunk (three in the lush caves), a group of one species on the bottom or a patch of floating plants on calm water. The
chance per try is 50 % in jungle water, 40 % in swamps and mangroves, 35 % in rivers and the lush caves, 15 % in cold
waters, times `spawn.wildPlantFrequency` (1.0; 0 = none, up to 10). The biome modifier `bettacraft:wild_plants` adds the
features to the tags of the waters above, so the biomes of biome mods count; which plants grow where is listed in
[Plants](plants.md).

## Commands

* `/bettacraft interior leave`: leave an interior.
* Operators: `/bettacraft interior resync` (send all miniatures again), `/bettacraft interior bench` (measure),
  `/bettacraft wateranimals` (every water animal of the loaded mods and whether aquariums take it, as a table in
  `bettacraft-water-animals.tsv`).
* Operators: `/bettacraft wildlife` shows for your position the biome, its kind of water, the animals of the mod that
  spawn there with their weights, our wild animals nearby, how full the water-ambient cap is (and which mods fill it)
  and whether vanilla or the own spawn pass can spawn now. `/bettacraft wildlife list [radius]` lists our animals nearby
  with their coordinates (64 blocks; tank inhabitants left out), `/bettacraft wildlife glow [radius]` lets them glow for
  30 seconds.

## In this chapter

<SubPages icons={true} />
