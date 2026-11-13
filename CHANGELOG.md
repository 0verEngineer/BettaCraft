# Changelog

All notable changes to Bettacraft. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versions follow [Semantic Versioning](https://semver.org/). The release workflow publishes the section of the tagged
version as release notes.

## [1.8.0-beta.1] - 2026-11-14

### Added
- **Water animals of all mods in aquariums:** every small water animal of vanilla and of other mods can live in a
  Bettacraft aquarium, with its own bucket or with a fish bag. Bettacraft looks at the animal itself (no list per mod):
  fish, axolotls, tadpoles, squids and glow squids, seahorses, sea slugs, starfish, isopods and more. Tested with every
  mod of the ATM10 pack that has water animals (Alex's Caves, Aquaculture, Critters and Companions, Deeper Darker,
  DragN's Just Bettas, Fintastic, Fish of Thieves, Ice and Fire, L_Ender's Cataclysm, Living Things, Naturalist, Shiny,
  Sushi Go Crafting, The Undergarden): 150 kinds, 70 of them go in.
- **Clear refusals:** too big (`inhabitants.maxHitbox`, 0.8 blocks; the size of the animal itself, so the variants of
  other mods count: a dwarf gourami fits, a giant gourami does not), bosses (`#c:bosses`), hostile mobs
  (`inhabitants.refuseHostile`), animals that are no water animals (bugs in buckets) and the blacklist each have their
  own message. `inhabitants.allow` and `inhabitants.deny` (`modid:name`, `modid:*`) win over everything.
- The **fish bag** holds water animals without a bucket (squids) with all their data; the tank menu takes every animal
  out with a fish bag.
- **Tadpoles** that grow up in an aquarium leave it as frogs and land in front of the tank (log entry);
  `inhabitants.tadpoles = STAY_TADPOLE` keeps them tadpoles.
- **No tank inside a tank:** aquarium blocks cannot be placed in the interior dimension (by hand, by machines, by
  commands or building tools: then it breaks and drops), with a message. Block tag `#bettacraft:not_in_tank_interior`
  (also the Better Fish Tanks tank).
- `/bettacraft wateranimals` (operators) writes a table of every water animal of the loaded mods and whether
  aquariums take it (`bettacraft-water-animals.tsv`).
- Handbook page "Animals of other mods" (English, German).

### Changed
- Axolotls, the shiny pufferfish and axolotl (Shiny), the Otherside angler fish (Deeper Darker) and the Cataclysm
  lionfish are predators. The manta ray (Living Things), the tuna (Sushi Go Crafting), frogs, the otter (Critters and
  Companions) and the pets of L_Ender's Cataclysm are blacklisted.

### Fixed
- A vanilla cod or salmon from a bucket vanished (or turned up next to the aquarium as another fish) when Fintastic
  was installed: Fintastic swaps them while they spawn. The animal from the bucket now lives in the aquarium as it is.

### Tests
- GameTests over every water animal found at runtime: each one is put into its own tank (bucket or fish bag), lives
  200 ticks in it and is taken out again, or is refused with a message (vanilla only without other mods, 150 kinds
  with `-Paquatic`, a new dev group with every water animal mod of ATM10); the inventory, the rules and config lists,
  a glow squid in a fish bag, tadpoles growing up (both settings), no tank in the interior.
- Server smoke test: one of every accepted kind in a mixed tank for the rest of the run, every refused kind refused.
- Client smoke scene M16: a mixed tank of foreign animals, the animals tab, a refused dolphin, the "no tank inside a
  tank" message.

## [1.7.0-beta.1] - 2026-11-14

### Changed
- **Name sign** half as high as before (0.12 blocks); each player sets its size with the client config
  `render.signScale` (0.5 to 2).
- **Leaving the interior:** sneak **and look steeply up** for 3 seconds (`interior.leaveLookUpDegrees`, default 60°,
  0 = sneaking alone as before). Sneaking to set substrate no longer takes you out, and placing or breaking a block
  starts the time over. A small hint at the bottom left ("Sneak + look up: leave") shows while you are inside and fills
  a bar while you hold it (client config `hud.interiorLeaveHint` turns the hint off).
- **Bubble nest** is now a domed heap of foam in several layers that reaches below the surface, so it can be seen through
  the side glass; the eggs sit in its underside as small beads, the larvae hang from it.

### Fixed
- An aquarium could be broken (any block of it, also by an explosion) while a player was in its interior. Now it is
  refused with a message that names who is inside; clients learn about it too, so the block does not flicker away.
- Handbook menu images show the current buttons again.

### Tests
- GameTests: breaking a free-shape tank by hand and by an explosion while a player is inside, the leave rule (looking
  down or a little up never leaves, placing / breaking / letting go start over) and the look-up rule switched off.
- Multiplayer test: Dev1 and Dev2 enter the same interior, build at the same time and see each other's blocks, the
  miniature outside follows, breaking the tank from outside is refused while one is inside, both leave by sneaking and
  looking up (looking down does not) and come out in front of the tank, then the block can be broken.
- Client smoke scene M15: the name sign at three sizes, the domed nest with eggs and with larvae from above, the side
  and below, shrimp lying flat seen from the side, the leave hint (English, German, off, while leaving).

## [1.6.0-beta.1] - 2026-11-13

### Added
- **Small animals** for the aquarium, eleven species: the dwarf shrimp **Neocaridina** (Red Cherry, Sakura, Fire Red,
  Painted Fire Red, Blue Dream, Yellow, Orange, Green Jade, Black Rose, Chocolate, wild form), the dwarf crayfish
  **CPO** (orange and the brown-grey wild form), **nerites** (zebra, horned, olive, red), **apple snails** (wild brown,
  gold, blue, purple, ivory), **ramshorns** (red-brown, pink, blue), **bladder**, **Malaysian trumpet** and **assassin
  snails**. They walk over the floor, the decoration and the plants of the interior, climb the glass (snails up the
  stones too), shrimp swim short jumps; trumpet snails dig into the substrate by day. Up to 200 per aquarium, drawn as
  small animated models.
- **Genetics:** every colour is a recessive allele at its own locus, mixed colours fall back to the wild form, plus a
  **grade** (colour density) that you breed up by selection: a red line goes from Red Cherry to Painted Fire Red. Pink
  and blue ramshorns come out of wild red-brown carriers and breed true.
- **Breeding** by type: shrimp and crayfish carry their eggs under the tail, fully formed young hatch; an apple snail
  lays a pink **clutch above the water line** (needs the air gap; take it off with the net or the menu button);
  ramshorn, bladder and trumpet snails slowly, one is enough; assassin snails rarely; **nerites never** in fresh water.
  Breeding needs food, water that is not bad and the right temperature and stops at the limit.
- Food for them grows in the tank (biofilm, more with plants and algae) and comes from the leftovers of the fish food
  (overfeeding hardly fouls the water any more). Too many animals for the food: some starve.
- Shrimp are sensitive to bad water (below 45 %) and 18-28 °C, crayfish like 15-26 °C and moult (the shed shell lies
  there, the crayfish is soft for a while). **Bettas hunt the young** (now and then an adult shrimp); dense plants and
  Java moss hide them. Assassin snails eat other snails (nerites rarely), crayfish clean up dead animals.
- **Dwarf clawed frogs**: a new animal (bucket, fish bag, spawn egg). They paddle along the bottom and swim up to
  breathe: without air above the water they drown slowly. Slow eaters of larvae, artemia and pellets: with bettas they
  often miss out, sneak + right-click with food feeds them directly.
- **Algae** on the inside of the glass in four stages: they grow with light, waste and time, the cleaning crew of snails
  (nerites best) and shrimp grazes them; three nerites keep a 4x2x2 aquarium clean. The new **algae scraper** cleans a
  glass face (sneaking: the whole side).
- **Catching:** the aquarium net catches small animals in open water by biome (rivers, swamps, mangroves, beaches,
  jungle water, lush caves) into an empty fish bag; a shovel on mud in a swamp now and then finds a trumpet snail; the
  wandering trader sells crayfish and apple snails, rarely shrimp of a colour line, coloured ramshorns, an assassin
  snail or a frog. In an aquarium the net catches the animal (or clutch, shed shell) you look at.
- **Stocking:** 50 snails, 25 shrimp, 12 crayfish or 1 frog count as one fish (config `stocking.*`).
- Tank menu: a **Small animals** page in the animals tab (every kind with sexes, young, eggs and grade, the algae, the
  food, the hiding spots; take one out into a fish bag, the best or with Shift the worst), frog details (health, food,
  air), the stocking in the overview. Jade, The One Probe and WTHIT show the small animals and the algae.
- Server settings `invertebrates.*`, `frogs.*`, `stocking.*`, `algae.*`, handbook section "Small animals" with seven
  pages (English and German), JEI information.

### Changed
- Aquariums are saved as schema 8 (with no small animals and no algae for older worlds). The aquarium net now has a
  durability (it wears in open water). Living small animals keep an aquarium from being broken, like the bettas.

## [1.5.0-beta.1] - 2026-11-13

### Added
- **Aquarium plants** for the interior of an aquarium, 17 of them: Java fern, Anubias nana, Bucephalandra "Green Wavy",
  "Brownie Red" (red), "Blue" (bluish) and "Mini Needle", Java moss, Cryptocoryne wendtii green and brown,
  Cryptocoryne "Flamingo" (whitish pink), Amazon sword, Rotala rotundifolia (red tips), Ludwigia repens (red),
  Alternanthera reineckii (deep red), Vallisneria, Monte Carlo and the red tiger lotus.
- Plant a **cutting** (smaller than a block) inside the interior: epiphytes on stone and wood (also the decoration),
  Java moss on any solid block, the others on substrate (the floor with a substrate, sand, gravel, dirt, mud, clay).
  Over the days it grows into a plant of **many blocks** (fronds, leaves, stems, ribbons, a carpet, floating leaves),
  each plant a little different. Buceps take about 20 days to full size, the Java fern 12, stem plants and
  Vallisneria 2-3 days.
- Plants grow with light at the surface, the waste in the water as nutrients and 20-30 °C, **also while nobody is in
  the interior**: the aquarium keeps track of every plant and writes the blocks of a new stage when it is due, the
  miniature follows (also for other players in multiplayer). Red plants get redder in strong light; the Flamingo
  crypt gets brighter and pinker.
- Stem plants grow up to the surface and run along it, form side shoots; Vallisneria, Monte Carlo and the crypts
  send out runners; the tiger lotus sends floating leaves up. Plants at the surface shade the aquarium together with
  dense floating plants (warning in the menu).
- **Shears** trim a plant inside the interior, or **from outside on the glass**: the plant you look at in the
  miniature; you get cuttings. Breaking a rhizome piece divides an epiphyte, breaking the root takes the whole plant
  out (the item keeps its size). Plants follow when aquariums are joined or split and drop when their block is broken.
- Plants take up waste while they have light, are **hiding spots** for the female after spawning (the male chases her
  less often) and a well planted aquarium makes the male ready a little faster. Bettas swim around them.
- Getting them: fishing in jungles and rivers (epiphytes; "Blue" and "Brownie Red" rare), fishing in swamps and
  swamp water bottles (Cryptocoryne, Ludwigia, Rotala), shears on a moss block next to water (Java moss), the
  wandering trader (sword, Vallisneria, Monte Carlo, Alternanthera; rarely the tiger lotus and the Flamingo crypt).
- Second page of the **Plants** tab in the tank menu: the aquarium plants per species (in their colour), plant mass,
  light, growth, waste uptake, hiding spots, breeding bonus and the shade warning.
- Server settings `interiorPlants.*`, handbook page "Aquarium plants" (English and German), JEI information.

### Changed
- Interior regions are saved as schema 4 (with an empty plant index for older worlds).

## [1.4.0-beta.1] - 2026-11-12

### Added
- Floating plants for the water surface of aquariums: **duckweed** (tiny leaves, grows fastest), **Amazon frogbit**
  (round leaves, long roots, the favourite nest site), **salvinia** (paired hairy leaves, robust) and the **red root
  floater** (red under strong light, green under weak light). Use one on an aquarium and it floats where you look at
  the water. A vanilla lily pad used on an aquarium becomes a fixed lily pad on the surface.
- The plants drift slowly over the surface, push each other aside, stay inside the glass and over the water of free
  shapes (never over a notch or a closed-off step) and keep away from where a filter jet hits the water.
- With light (a lit tank light or daylight) and nutrients (the waste in the water) they grow, put out offshoots up to
  60 % cover and take up a little waste. In the dark they do not grow. Dense cover (from 50 %) warns in the menu and the
  info overlays, shades the plants (slower growth) and lets some of the feeder's food stay on the leaves and rot.
- The male builds his bubble nest under the densest cluster of floating plants; floating plants (from 10 % cover)
  make him ready 1.15 times faster.
- Getting them: duckweed sometimes comes up with a bottle of swamp water, fishing in swamps and mangrove swamps
  sometimes brings up a floating plant (the red root floater only in mangrove swamps), the wandering trader sells
  frogbit and salvinia (the red root floater among his rare offers). No crafting recipe; they multiply in the aquarium.
- **Aquarium net** (four string and a stick): right-click an aquarium to take out the floating plants you look at,
  from any angle, also from the side through the glass; sneaking with the net thins the whole aquarium out by half
  (lily pads stay). An empty hand also takes plants out while looking down at the surface (where no plant floats the
  menu opens as before). The plants of a broken aquarium drop as items.
- **Plants** tab in the tank menu: number per species, cover, light, growth, waste uptake, the dense cover warning and
  a **Thin out** button; Jade, The One Probe and WTHIT show the number and the cover.
- Server settings `floatingPlants.*`: growth speed (0 = off), most cover, dense cover, waste uptake, breeding bonus,
  food on the leaves, plants per tank, the chances of the bottle and of fishing.
- Handbook page "Floating plants" (English and German) with links from the aquarium, water quality, breeding, food and
  menu pages; JEI information and item tooltips.

### Changed
- Tank menu: the tab labels use a smaller font, so seven tabs fit (also the longer German labels); lines of text that
  would run past the edge of the menu in a longer translation are drawn a little smaller instead.
- Aquariums from earlier versions get an empty list of floating plants (tank data schema 7). Lily pads in the interior
  stay blocks of the interior; the nest still uses them when there are no plants on the surface.

## [1.3.0-beta.1] - 2026-11-12

### Added
- Two aquarium styles. **Textured** aquariums show the texture of their glass with connected textures: the frame of the
  glass texture runs only along the outer edges of the whole aquarium (works with the glass of other mods; the frame
  width is read from the texture). **Clear glass** aquariums are perfectly clear vanilla glass without any texture:
  6 glass, an iron nugget in the middle and 3 iron ingots below give 4. A single vanilla-glass aquarium converts between
  both styles in the crafting grid. The two styles never join.
- Jade, The One Probe and WTHIT show the glass of an aquarium (its localized name) and its style; so does the water tab
  of the tank menu.
- Mirror stick (glass pane, iron nugget, stick): adult males that see it flare at their reflection, hold still and turn
  towards it. A male that flared at the mirror during the last day gets ready for a bubble nest 1.25 times faster
  (`breeding.mirrorReadinessBonus`, 1 = purely cosmetic). Advancement "Mirror, Mirror".
- Client settings `render.connectedGlass` and `render.tankEdgeWidth` (in pixels of 1/16 block, 0.1 to 1, default 0.25; 0 = no edge lines).

### Changed
- Heaters and coolers need 8 FE per tick for each water block they heat or cool (was 1), far more than a filter (10) or a light (4).
- Separate server setting `power.coolerFePerBlock` for coolers (default 8, like `heaterFePerBlock`).
- The dark edge lines are much thinner (1/64 block instead of 1/16) and close the inner corners of flat walls.
- Aquariums from earlier versions keep their glass and become textured (tank data schema 6).

### Fixed
- Switching a power setting on or off at runtime: connected cables and pipes notice it at once (before, only after a chunk reload).
- Water behind translucent (stained) glass looked darker along every block border, and in some blocks (the middle of
  a T) the water surface was missing: water faces now always sort behind the glass in front of them, and the frames
  of the glass texture no longer show between the blocks of one aquarium.

## [1.2.0-beta.1] - 2026-11-12

### Added
- Free aquarium shapes: any set of tank blocks that touch face to face (L, T, U, steps, a tower on a wide tank) forms
  one aquarium, as long as its bounding box fits the maximum size. A block between two aquariums joins them.
  `tank.freeShapes = false` keeps aquariums cuboid as before.
- Dark edge lines along the inner corners of a free shape.
- Handbook page "Shapes" (English and German).

### Changed
- Water fills a free shape layer by layer; breaking a block removes only the water of that block, and when an aquarium
  falls apart every piece keeps the water of its own blocks (before, the rest was split by block count).
- Fish, food, the bubble nest and new fry stay in the water of the actual shape, never in the empty corner of an L.
- The feeder, lights and lids work on every open top (also on a lower step), external filters on every outer wall
  (also in an inner corner), cabinets under every block with no tank block below it. The filter hose follows the
  wall it hangs on. Evaporation counts only tops the water reaches.
- The name sign sits on the widest flat part of the front.
- The interior of a free shape covers its bounding box; the parts outside the shape are walled off and have no floor.
  On a split every piece keeps the interior of its own blocks; substrate that is no longer on the floor after a merge
  drops as items.
- Interior region data schema 3: regions of existing aquariums are full boxes. The tank data itself is unchanged.

## [1.1.0-beta.1] - 2026-11-12

### Added
- Water quality: waste of the animals and food given to fish that are not hungry lower it, filters remove most of
  the waste (external filter 32, internal filter 16 water blocks), fresh water dilutes it (water change with buckets
  or pipes). Bad water (below 35 %) stresses bettas like a wrong temperature. Water tab in the tank menu, warnings in
  the overview, the log and the info overlays.
- Evaporation: open aquariums lose about 20 mB per top block and day (more in warm water); an aquarium lid, the feeder,
  a light or any full block covers a top block.
- Coolers in three sizes (16, 32, 64 water blocks) for the interior, the external filter and the cabinet filter;
  heaters and coolers can share a filter.
- Optional power: `power.heaterRequiresEnergy`, `power.filterRequiresEnergy` and `power.lightRequiresEnergy` (all off
  by default). Each aquarium stores FE (100 000 by default) through any tank block, an attached filter, a cabinet
  below or a light on it.
- Larva tub (mosquito larvae from water and organic matter under the open sky) and artemia hatchery (nauplii from salt
  water, cysts and glass bottles in the light), both with hoppers, pipes and comparator output.
- Aquariums accept water from pipes and give it to pumps (down to the minimum fill while animals live in them).
- Three advancements (Fresh Start, Keep Cool, Larva Farmer), four handbook pages, config screen labels.

### Changed
- Filters clean the water now (the flow itself still has no effect on the fish).
- The tank menu has a sixth tab (Water); the tab widths follow their labels. In German the animals tab is called
  „Tiere“.
- Tank data schema 5: existing aquariums start with clean water; the unused evaporation placeholder of earlier
  versions is replaced.

## [1.0.0-beta.1] - 2026-11-12

First public beta for Minecraft 1.21.1 and NeoForge 21.1.200 or newer.

### Added
- Bettas: wild bettas in swamps (`#bettacraft:betta_spawn_biomes`), catching with a water bucket or a fish bag (kelp or
  plastic recipe), betta buckets and bags that keep every gene, creative tab with every tail shape and named variety.
- Genetics: nine loci with two alleles (tail shape, fin length, pectorals, body and fin colour, pattern and its
  colour, iridescence and its colour), dominance, carriers and mutations; 8 tail shapes, short and long fins, dumbo,
  43 colours, 12 patterns and 16 named varieties; colours, genes and varieties as datapack JSON.
- Aquariums: multiblock tanks of any glass (one kind per tank) with vanilla water, filling bucket by bucket, clear
  water rendering (also with Sodium and Iris), real entities as inhabitants, foreign fish by tag and size rules,
  break lock while animals or eggs are inside, name sign, owner lock, tank menu with overview,
  inhabitants, breeding, interior and log tabs.
- Care: biome and day/night temperature, heaters for 16, 32 and 64 water blocks, health with hunger, temperature
  and overcrowding stress, sickness, recovery and optional death of old age; time stands still in unloaded chunks
  unless `simulateUnloadedTime` is on.
- Breeding: conditioning with live food, bubble nest, spawning, eggs, larvae and six life stages, female chased after
  spawning, males eating fry, survival from the care; fry food (infusoria and artemia cultures, artemia cysts from
  sand, salt water from oceans).
- Fights between adult males with flaring, predators (`#bettacraft:predators`) hunting small inhabitants, live prey.
- Interior dimension: step into an aquarium (sneak + right-click with an empty hand) and build at 16 blocks per tank
  block (4 or 8 configurable); miniature rendering with delta sync and threaded meshing, substrate per floor block,
  10 decoration structures with hologram preview, heater rods, internal filters, floating plants, drops when the
  tank is broken.
- Equipment: internal and external filters with water jet and bubbles, aquarium cabinets of any wood (filter slot,
  heaters in the filter, storage the feeder uses), automatic feeder (1 to 3 times a day), light bar switched off by
  redstone, sounds with subtitles.
- Handbook with 26 pages in English and German (GuideME, optional), 25 advancements, item details on Shift and JEI
  information pages.
- Optional integrations: Jade, The One Probe and WTHIT (aquarium, betta behind the glass, filter, cabinet, nest);
  Better Fish Tanks (fish bag in both directions, bettas in its tanks), Fintastic (fish, fishnet, prey, food, plants
  in the interior), Fish of Thieves (fish, predators, bettas on the fish plaque), Aquaculture 2 and Naturalist.
- Config screen labels and tooltips in English and German.

### Fixed (since the internal milestone builds)
- Shift + right-click on an aquarium with a filled bucket, fish bag, Better Fish Tanks bag or Fintastic net inserts the
  animal instead of releasing it in front of the aquarium.
- A fish caught out of an aquarium in the world (bucket or bag of any mod) shows up in the tank log at once.

[1.1.0-beta.1]: https://github.com/0verEngineer/BettaCraft/releases/tag/v1.1.0-beta.1
[1.0.0-beta.1]: https://github.com/0verEngineer/BettaCraft/releases/tag/v1.0.0-beta.1
