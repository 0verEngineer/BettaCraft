<p align="center"><img src="src/main/resources/bettacraft.png" width="128" alt="Bettacraft logo"></p>

# Bettacraft

Betta fish and real aquariums for Minecraft 1.21.1 (NeoForge): catch wild bettas and tankmates, keep them in multiblock
aquariums of any glass and shape, aquascape the inside block by block with plants and corals that sway in the water,
run filters, lights, CO2 and reef dosing, and breed new colours with real Mendel genetics.

![An aquarium room: a reef tank, a planted show tank with LED panels, CO2 and a multiblock filter, and a betta nano tank](docs/images/bettacraft-aquarium-room.jpg)

<table>
  <tr>
    <td><img src="docs/images/bettacraft-planted-tank-swimming.gif" alt="A planted tank: a Koi Galaxy betta, neon tetras, harlequin rasboras, corydoras and shrimp swim between swaying plants"></td>
    <td><img src="docs/images/bettacraft-reef-tank-swimming.gif" alt="A reef tank: clownfish at their anemone, green chromis, a yellow tang, seahorses and corals"></td>
  </tr>
  <tr>
    <td><img src="docs/images/bettacraft-betta-varieties.jpg" alt="Six betta varieties in a row: Koi Galaxy, Black Orchid, Mustard Gas, Samurai, Avatar, Hellboy"></td>
    <td><img src="docs/images/bettacraft-tank-interior.jpg" alt="Inside the planted tank at 16 blocks per tank block"></td>
  </tr>
</table>

![The tank menu with the genes of a betta and the water values, and the handbook page on genetics](docs/images/bettacraft-menus.png)

More pictures and animations (also with Complementary shaders) are in [docs/images](docs/images).

## Features

### Bettas and genetics
- **Wild bettas** spawn in swamps (also those of biome mods); catch them with a water bucket or a fish bag, the fish keeps
  all its genes. A **magnifier** (or the aquarium) analyses the hidden genes of a new animal.
- **Mendel genetics:** nine loci with two alleles each (tail shape, fin length, pectorals, body colour, fin colour,
  pattern, pattern colour, iridescence and its colour), dominance, carriers and mutations: 8 tail shapes, short and
  long fins, dumbo ears, 43 colours, 12 patterns and 16 named varieties (Koi Galaxy, Nemo, Black Orchid, Mustard Gas
  ...), all data-driven and changeable with datapacks.
- **Breeding:** conditioning with live food, bubble nest under floating plants, spawning, eggs, larvae and six life
  stages; fry need infusoria and artemia that you culture yourself (larva tub, artemia hatchery). Two adult males
  fight (they flare first, also at a mirror stick).

### Aquariums and the interior
- **Multiblock tanks** of any glass and any connected shape (box, L, T, steps ...), up to 16x16x16 (configurable to
  64), with the connected texture of the glass or perfectly clear. Real vanilla water, filled bucket by bucket,
  rendered clear (also with Sodium and Iris). The animals are real entities.
- **Interior:** sneak and right-click with an empty hand to step inside a pocket dimension at 16 blocks per tank block
  and build with any block: substrates, decoration (castle, shipwreck, skull, driftwood, cave ...), equipment. The
  aquarium shows the result as a live miniature; several players can build at once.
- **Care:** temperature from biome and time of day, heaters and coolers, hunger, overcrowding, water quality, oxygen,
  sickness and recovery; a tank log tells you what happened. Tank menu with overview, inhabitants and their
  genotype, breeding tab with Punnett squares, water, devices, plants, interior and log; name sign, owner lock and a
  break protection.

### Plants and corals
- **17 aquarium plants** (Java fern, Anubias, four Bucephalandras, Java moss, Cryptocorynes, Amazon sword, Rotala,
  Ludwigia, Alternanthera, Vallisneria, Monte Carlo, tiger lotus) grow over the days from a cutting into plants of many
  blocks, get redder in strong light, are trimmed with shears and clean the water. **Floating plants** (duckweed,
  frogbit, salvinia, red root floater) drift on the surface, grow and spread; lily pads stay put.
- Every plant and coral has **real leaves, polyps and tentacles that sway** in the water, inside and in the miniature
  outside; zoanthids and star polyps close when a fish swims by. Kelp and seagrass sway too.

### Tankmates and small animals
- **21 tankmates** with their own models and behaviour: tetras, rasboras, corydoras, plecos, otocinclus, kuhli and clown
  loaches, guppies and endlers (own genetics with 28 strains), platys and the dwarf gourami. Shoals, water ranges,
  predators and prey, and a breeding style per species (glued eggs, caves, egg scatterers, livebearers, foam nests).
- **Small animals:** Neocaridina in eleven colours with selection grades, bee shrimp (Crystal Red/Black S to SSS, Blue
  Bolt, Wine Red, Tiger), Sulawesi shrimp and snails, dwarf crayfish, nerites, apple snails, ramshorns and more crawl
  over floor, plants and glass; algae grow and the cleaning crew eats them. Dwarf clawed frogs.
- **Wild animals and corals** live in the waters they come from (jungle, swamp, river, mangrove, warm and lukewarm
  ocean); the waters are biome tags, so the biomes of biome mods count too. Catch them with the aquarium net.
- **Water animals of all mods** (fish, axolotls, tadpoles, seahorses ...) live in the aquariums; big, boss and hostile
  ones are refused with a reason.

### Reef tanks and seahorses
- Sea water, salinity, protein skimmer, flow pump, reef and live rock, actinic light. Eight corals and the bubble tip
  anemone grow with light, flow, calcium, alkalinity and magnesium (four-channel dosing pump), bleach or glow and give
  frags. Clownfish spawn at their anemone; tang, goby, chromis, damsel, cleaner shrimp, hermit crabs, turbo snails.
- **Seahorses** (lined and dwarf) swim upright, hold on to plants and corals, eat live food, and the male gives birth
  from his brood pouch.

### Equipment and water
- Internal, external, sponge and corner filters, cabinets of any wood or stone (with the filter inside), automatic and
  clip-on feeders, air pumps with air stones (bubble columns), light bar, LED panel and high-power LED in 16 colours
  with dimmer and daylight timer.
- **Pipes and water changes:** glass and stainless steel pipes and a water pump carry water with all its values, a
  bulkhead leads to interior pipes, a multiblock filter of any size with media that clog; nitrate makes water changes
  matter, a water change controller and a top-off do them for you.
- **Water chemistry:** TDS, pH and hardness from water, substrate (active soil), driftwood, leaves and CO2, with a range
  for every animal; CO2 cylinders and bio CO2, a reverse osmosis unit, and RO and sea water as fluids for other mods.
- Optional Forge Energy for the equipment, power regulator, own procedural sounds.

### Handbook, progression, languages
- 178 handbook pages (GuideME) in English and German with a page for every animal, plant, coral and device and where to
  get it (generated from the game data), 31 advancements, JEI pages and tooltips.
- Every larger feature has a switch (`features.*`) so modpacks can turn it off.

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
| Mekanism, EnderIO, Create, Thermal | stainless steel from their machines (metallurgic infuser, alloy smelter, heated mixing, induction smelter); their tanks and pipes join Bettacraft pipes, aquariums and reservoirs (standard fluid handler, also AE2 and RS interfaces), also with the fluids `bettacraft:ro_water` and `bettacraft:sea_water`; CO2 gas of other mods (`#bettacraft:co2_gas`) fills CO2 cylinders |
| Biome mods (Regions Unexplored, Oh The Biomes We've Gone, Biomes O' Plenty, Twilight Forest, ...) | their swamps, rivers, mangroves, lush caves and warm oceans get the wild animals, net catches, fishing loot and corals of their vanilla counterparts (biome tags `#bettacraft:*_waters`, `#bettacraft:coral_reef_biomes`) |
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

- **Tags:** `#bettacraft:betta_spawn_biomes`, `#bettacraft:salt_water_biomes`, `#bettacraft:warm_ocean_waters`,
  `lukewarm_ocean_waters`, `mangrove_waters`, `lush_cave_waters`, `swamp_waters`, `river_waters`, `beach_waters`,
  `jungle_waters`, `#bettacraft:coral_reef_biomes` (biomes); `#bettacraft:tank_inhabitants`,
  `#bettacraft:tank_blacklist`, `#bettacraft:predators`, `#bettacraft:betta_prey` (entity types);
  `#bettacraft:betta_food`, `#bettacraft:live_food`, `#bettacraft:fry_food`, `#bettacraft:fry_food_start`,
  `#bettacraft:pellet_fish`, `#bettacraft:plastic_sheets`, `#bettacraft:aquatic_plants` (items); `#bettacraft:floating_plants`,
  `#bettacraft:interior_water_plants`, `#bettacraft:substrate_blacklist`, `#bettacraft:artemia_sand`, `#bettacraft:epiphyte_surfaces`,
  `#bettacraft:plant_substrate`, `#bettacraft:not_in_tank_interior`, `#bettacraft:swaying_plants` (blocks).
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
./gradlew runClient -Psmoketestreadme -PnoJei   # README media scene (background window), then:
py tools/assetgen/readme_media.py       # run/screenshots -> docs/images (stills, GIF and WebP animations)
```

Leave optional mods out of the dev runtime with `-PnoJade`, `-PnoJei`, `-PnoGuideme` and `-PnoCompat`; add The One
Probe and WTHIT with `-Ptop` and `-Pwthit` (config `compat.overlayProvider` picks which overlay shows the Bettacraft
lines; AUTO = only the first of Jade, WTHIT, The One Probe). Textures, models and the logo come from the procedural generator in `tools/assetgen` (Python, see its
README); the generated files are committed, so the Gradle build does not need Python.

Development-only code (smoke tests, the showcase world, GameTests) lives in the source set `src/dev`: every `run*` task
loads it, the jar does not contain it.

## Continuous integration and releases

[![Build](https://github.com/0verEngineer/BettaCraft/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/0verEngineer/BettaCraft/actions/workflows/build.yml)

- **Build** (`.github/workflows/build.yml`, every push to `master` and every pull request): `gradlew build` (unit tests
  and `verifyReleaseJar`), the changelog section, `runData` must not change `src/generated`, GameTests and a dedicated
  server smoke test. Started by hand with *full* (and before every release) it also runs the server smoke without any
  optional mod and with The One Probe and WTHIT, and the GameTests with the power mods of `-Ppower` (pinned Modrinth
  jars, `tools/ci/power-mods.json`, fetched and SHA-512 checked by `tools/ci/fetch_mods.py`). A failed GameTest shows
  up with its message as an annotation on the run page.
- **Release** (`.github/workflows/release.yml`, a pushed tag `v<mod_version>`): the full test set, the jar of the tag,
  `verifyReleaseJar`, the top section of `CHANGELOG.md` as release notes, a GitHub release (a prerelease for `-alpha`,
  `-beta` and `-rc` versions) and, once the tokens and project ids are set, the upload to Modrinth and CurseForge.
- The same checks locally:

```
./gradlew verifyReleaseJar      # the jar holds only Bettacraft classes and namespaces, no dev / GameTest code, version ok
./gradlew releaseNotes          # build/release-notes.md from the top section of CHANGELOG.md (must be mod_version)
```

Client smoke tests, the multiplayer test, `buildShowcase`, `smoketestUpgrade`, `-Paquatic`, `-Pbiomes` and `-Pshaders` only run
locally (they need a window, local worlds or mod jars that cannot all be downloaded in CI).

## License

MIT, see [LICENSE](LICENSE). This covers the code, the asset generator and all assets, so modpacks may include the mod
freely.

### Sound credits

Every sound of Bettacraft is generated by `tools/assetgen/sounds.py` (own work, MIT like the rest); no third-party
recordings are used, so nothing needs attribution. The list of all files is in [SOUNDS-CREDITS.md](SOUNDS-CREDITS.md).

Bug reports and ideas: [GitHub issues](https://github.com/0verEngineer/BettaCraft/issues).
