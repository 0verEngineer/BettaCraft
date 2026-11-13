<p align="center"><img src="src/main/resources/bettacraft.png" width="128" alt="Bettacraft logo"></p>

# Bettacraft

Betta fish for Minecraft 1.21.1 (NeoForge): catch wild bettas in swamps, keep them in multiblock aquariums of any glass,
care for heat, food and space, breed them with real Mendel genetics and decorate the inside of the aquarium block by
block.

![An aquarium with bettas](src/main/resources/assets/bettacraft/guides/bettacraft/guide/images/tank.png)

## Features

- **Bettas** spawn in swamps (`#c:is_swamp`, so also the swamps of Biomes O' Plenty, Regions Unexplored and others).
  Catch them with a water bucket or a fish bag; the fish keeps all its genes.
- **Genetics:** nine loci with two alleles each (tail shape, fin length, pectorals, body colour, fin colour, pattern,
  pattern colour, iridescence and its colour), dominance, carriers and a small mutation chance. 8 tail shapes,
  short- and long-finned forms, dumbo ears, 43 colours, 12 patterns and 16 named varieties (Koi Galaxy, Nemo,
  Black Orchid, Mustard Gas, ...). Colours, genes and varieties are data-driven and can be changed with datapacks.
- **Aquariums:** a multiblock tank of any glass and any connected shape (box, L, T, steps ...), up to 16x16x16
  (configurable up to 64), either with the connected texture of its glass or as perfectly clear glass, with thin dark
  edges only along the outer edges. Real vanilla water, filled
  bucket by bucket from the bottom, rendered clear and without biome tint, also with Sodium and Iris. The animals are
  real entities, so frogs, axolotls and the fish of other mods keep their own behaviour.
- **Care:** water temperature from the biome and the time of day, heaters and coolers in three sizes, health with
  hunger, temperature, overcrowding (young fish count in fractions) and water quality stress, sickness and recovery;
  a tank log tells you what happened.
- **Water quality and evaporation:** waste of the animals and overfeeding foul the water, filters keep it clean,
  water changes with buckets (or pipes) bring it back; open aquariums slowly evaporate, a lid stops that.
- **Floating plants:** duckweed, Amazon frogbit, salvinia and the red root floater drift on the water surface of an
  aquarium (lily pads stay put). With light and nutrients they grow, put out offshoots and take up waste; the red root
  floater turns red under strong light; the male builds his nest under them. Found in swamp water bottles, by fishing
  in swamps and from the wandering trader; an empty hand or the aquarium net take them out again.
- **Aquarium plants:** 17 plants for the interior (Java fern, Anubias, four Bucephalandras, Java moss, Cryptocorynes
  incl. the pink "Flamingo", Amazon sword, Rotala, Ludwigia, Alternanthera, Vallisneria, Monte Carlo, red tiger lotus).
  A cutting smaller than a block grows over the days into a plant of many blocks, also while nobody is inside, and the
  miniature grows along; red plants get redder in strong light. Trim with shears (inside or from outside on the
  glass), divide rhizomes, take plants out by their root. They clean the water, hide the female after spawning and
  give a small breeding bonus. Found by fishing in jungles, rivers and swamps, in swamp water, on moss next to water
  and from the wandering trader.
- **Small animals:** Neocaridina shrimp in eleven colours, the orange dwarf crayfish (CPO), nerites (the red and yellow
  racer nerites each in their own shade), apple snails (mostly gold, some with bands), ramshorns (red-brown, pink, blue,
  red), bladder, trumpet and assassin snails crawl over the floor, the decoration, the
  plants and the glass. Mendel colours plus a grade bred up by selection (Red Cherry to Painted Fire Red), breeding by
  type (eggs under the tail, apple snail clutches above the water line, nerites never), bettas hunt the young and
  plants hide them. Dwarf clawed frogs breathe at the surface and are slow eaters. Algae grow on the glass, the
  cleaning crew and the algae scraper keep it clean. Caught with the aquarium net by biome.
- **Tankmates:** twenty-one popular aquarium fish as real animals with their own models: tetras (neon, cardinal, green
  neon in green and blue, ember, glowlight), rasboras (harlequin, mosquito rasbora), corydoras (pygmy, panda, sterbai, green gold), plecos (L134
  leopard frog, L046 zebra, bristlenose), otocinclus, kuhli and clown loaches, guppies and endlers (real genetics:
  tail shapes, body and tail colours, X and Y patterns, 28 named strains, hybrids), platys (colour varieties) and the
  dwarf gourami. Shoals, temperature and water ranges per species, bettas chase guppy and gourami
  males, everybody hunts fry and eggs, clown loaches eat snails, otocinclus graze algae. Every species breeds its own
  way: corydoras glue eggs to the glass after a cool water change, plecos spawn in a cave and the male guards, egg
  scatterers need fine plants or a spawning mop, livebearers bear live young (breeding box), the dwarf gourami builds
  a foam nest. Caught with the aquarium net by biome or bought from the wandering trader. They swim in smooth curves;
  kuhli loaches undulate like eels and hide by day.
- **Reef tanks:** sea water from the ocean or with sea salt, salinity that mixes and concentrates, freshwater animals
  die in salt water and reef animals in fresh water (nerites and livebearers tolerate brackish water). Protein
  skimmer, flow pump, reef rock and live rock, actinic light. Eight corals and the bubble tip anemone grow on reef
  rock in the interior with light, flow and calcium, bleach or glow, and give frags. Clownfish (they spawn at their
  anemone), yellow tang, watchman goby, green chromis, yellowtail damsel, cleaner shrimp, hermit crabs, turbo snails.
- **Power (optional):** a server can make heaters, coolers, filters and lights need Forge Energy from any FE mod.
- **Live food breeding:** a larva tub for mosquito larvae and an artemia hatchery for nauplii, both with hoppers.
- **Breeding:** conditioning with live food, bubble nest, spawning, eggs, larvae and six life stages. Take the
  female out after spawning and the male once the fry swim, or they get hurt or eaten. Fry need infusoria and
  artemia nauplii, which you culture yourself.
- **Water animals of all mods:** every small water animal of vanilla and of other mods lives in the aquariums (fish,
  axolotls, tadpoles, squids, seahorses, sea slugs, starfish ...); too big ones, bosses, hostile mobs and land animals
  are refused with a message that says why. Rules by size, tags and config lists; the fish bag holds animals without a
  bucket too. Tadpoles leave as frogs when they grow up, and animals that outgrow the aquarium wait in a fish bag in
  front of it. No aquarium can be placed inside an aquarium's interior.
- **Fights and predators:** two adult males fight (they flare at each other first, or at a mirror stick), pufferfish,
  axolotls and the predators of other mods hunt small fish.
- **Path finding:** frogs walk around the interior decoration, small animals go under overhangs and into caves,
  plecos swim into their spawning cave (`features.interiorPathfinding`).
- **New plant look (prototype):** Rotala, Vallisneria and Anubias with real leaves that sway in the water, inside the
  interior and in the miniature outside (from 24 blocks on as blocks again; `features.plantMeshes`).
- **Interior:** sneak and right-click an aquarium with an empty hand to step inside a void dimension at 16 blocks per
  tank block (4 or 8 configurable) and build with any block. The result is shown in the aquarium as a miniature.
  Floor textures (substrate), decoration structures (castle, shipwreck, skull, volcano, driftwood, cave, ...),
  heaters, internal filters and floating plants. Several players can build in the same interior at once; nobody can
  break the aquarium while someone is inside. Leave by sneaking and looking up (a small hint shows how).
- **Equipment:** internal and external filters with a water jet, aquarium cabinets of any wood (they hold the
  external filter and food), an automatic feeder (1 to 3 times per day, takes food from the cabinets below), a small
  clip-on feeder for the side of the glass and a connecting light bar that redstone switches off, in 16 colours that tint the water (dye it).
- **Tank menu:** overview with target temperature, inhabitants with genotype, breeding tab with Punnett squares,
  interior and log; name sign on the glass (its size is a client setting), a lock for the owner and a break protection
  that stops everyone (also the owner, creative, explosions and pistons) from breaking the aquarium.
- **Handbook and progression:** 72 handbook pages in English and German (with GuideME) and 29 advancements.
- **Languages:** English and German.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.200 or newer |
| Java | 21 |
| Side | client and dedicated server (install on both) |

No other mod is required.

## Optional integrations

| Mod | What you get |
|---|---|
| [GuideME](https://modrinth.com/mod/guideme) | the in-game handbook (given once per player, see `guide.giveBook`) |
| [JEI](https://modrinth.com/mod/jei) | information pages for every Bettacraft item |
| [Jade](https://modrinth.com/mod/jade), [The One Probe](https://modrinth.com/mod/the-one-probe), [WTHIT](https://modrinth.com/mod/wthit) | aquarium (name, glass and style, temperature, water, animals, nest, filters, warnings), betta behind the glass (type, sex, health, hunger, stage, genes), filter and cabinet. Sneak for details; The One Probe needs its probe for the extended view.; with several overlay mods only the first shows them, see `compat.overlayProvider` |
| Better Fish Tanks | its fish bag puts animals into and takes them out of Bettacraft aquariums; bettas live in its tanks |
| [Fintastic](https://modrinth.com/mod/fintastic) | fish and fishnet in both directions, live prey, fish food, water plants in the interior |
| [Fish of Thieves](https://modrinth.com/mod/fish-of-thieves) | fish and predators in Bettacraft aquariums, bettas on the fish plaque |
| [Aquaculture 2](https://modrinth.com/mod/aquaculture) | small fish welcome, piranha as predator, worms as live food, fillets for pellets |
| [Naturalist](https://modrinth.com/mod/naturalist) | small animals welcome, piranha and anglerfish as predators |
| any mod with water animals | its small water animals go into aquariums (tested with Alex's Caves, Critters and Companions, Living Things, Deeper Darker, The Undergarden, Shiny, Sushi Go Crafting, L_Ender's Cataclysm, Ice and Fire, DragN's Just Bettas), large, boss and hostile ones are refused |
| Sodium, Iris | supported (water and miniatures render correctly, also with shaders) |

Without GuideME every Bettacraft item explains itself in its tooltip (hold Shift).

## Getting started

1. Find a swamp and catch a betta with a water bucket.
2. Craft aquarium blocks (six glass of one kind and three iron ingots give four; an iron nugget in the middle instead
   of the ingot makes clear glass aquariums from vanilla glass) and place them next to each other.
3. Fill the aquarium with water buckets, at least half full.
4. Right-click it with the betta bucket.
5. Outside warm biomes add a heater (in the interior or in an external filter) and feed every day with betta pellets
   or mosquito larvae, or place a feeder on top.

The handbook (GuideME) and the JEI pages explain everything else.

## Configuration

| File | Content |
|---|---|
| `<world>/serverconfig/bettacraft-server.toml` | game rules per world: tank size, shapes and limits, which animals aquariums take, health timers, temperature and tolerated range, breeding, fights, food, life stages, interior, drops, water quality, floating plants, aquarium plants, small animals, frogs, tankmates (breeding, getting along, food), stocking, algae, salt water and reef tanks, evaporation, power, food breeding, handbook, time in unloaded chunks |
| `config/bettacraft-common.toml` | spawn weight and group size of wild bettas, fish bag recipe (kelp or plastic), which overlay mod shows the Bettacraft lines (`compat.overlayProvider`), feature switches (`features.*`) |
| `config/bettacraft-client.toml` | animation, miniature and tank detail distances, water surface animation, connected glass, edge width, name sign size, water tint of coloured lights, interior leave hint, filter sound |

Every option has a comment in the file and can also be changed in the in-game config screen (Mods > Bettacraft >
Config). By default, time stands still for aquariums in unloaded chunks (`simulateUnloadedTime = false`), water quality
and evaporation are on but gentle, and no equipment needs power (`power.*RequiresEnergy = false`).

### For pack makers

- **Tags:** `#bettacraft:betta_spawn_biomes`, `#bettacraft:salt_water_biomes` (biomes); `#bettacraft:tank_inhabitants`,
  `#bettacraft:tank_blacklist`, `#bettacraft:predators`, `#bettacraft:betta_prey` (entity types);
  `#bettacraft:betta_food`, `#bettacraft:live_food`, `#bettacraft:fry_food`, `#bettacraft:fry_food_start`,
  `#bettacraft:pellet_fish`, `#bettacraft:plastic_sheets`, `#bettacraft:aquatic_plants` (items); `#bettacraft:floating_plants`,
  `#bettacraft:interior_water_plants`, `#bettacraft:substrate_blacklist`, `#bettacraft:artemia_sand`, `#bettacraft:epiphyte_surfaces`,
  `#bettacraft:plant_substrate`, `#bettacraft:not_in_tank_interior` (blocks).
- **Animals in aquariums:** `inhabitants.allow` / `inhabitants.deny` in the server config (`modid:name`, `modid:*`),
  `inhabitants.maxHitbox`, `inhabitants.refuseHostile`, `inhabitants.tadpoles`, `inhabitants.outgrown`; `/bettacraft wateranimals` writes a table
  of every water animal of the loaded mods and whether aquariums take it.
- **Datapacks:** `data/<namespace>/bettacraft/colors/*.json`, `.../genes/<locus>.json` and `.../varieties/*.json`
  add or change colours, allele ranks and frequencies, and named varieties.
- **Commands:** `/bettacraft interior leave` for everyone; operators: `/bettacraft interior resync`,
  `/bettacraft interior bench` and `/bettacraft wateranimals`.

## Building from source

```
./gradlew build                 # jar in build/libs, runs the unit tests
./gradlew runGameTestServer     # GameTests
./gradlew runData               # regenerate src/generated (recipes, tags, models, en_us, advancements)
./gradlew runClient             # dev client with Jade, JEI, GuideME and the aquarium mods
./gradlew buildShowcase         # (re)builds the showcase world "Bettacraft Schaukasten" in run/saves (no window)
./gradlew runMultiplayerTest    # dedicated server + two background clients (Dev1, Dev2) play a scripted session
```

Leave optional mods out of the dev runtime with `-PnoJade`, `-PnoJei`, `-PnoGuideme` and `-PnoCompat`; add The One
Probe and WTHIT with `-Ptop` and `-Pwthit` (config `compat.overlayProvider` picks which overlay shows the Bettacraft
lines; AUTO = only the first of Jade, WTHIT, The One Probe). Textures, models and the logo come from the procedural generator in `tools/assetgen` (Python, see its
README); the generated files are committed, so the Gradle build does not need Python.

## License

MIT, see [LICENSE](LICENSE). This covers the code, the asset generator and all assets, so modpacks may include the mod
freely.

Bug reports and ideas: [GitHub issues](https://github.com/0verEngineer/BettaCraft/issues).
