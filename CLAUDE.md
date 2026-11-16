# Bettacraft – notes for Claude Code

Betta fish and aquarium mod for Minecraft 1.21.1 (NeoForge). modid `bettacraft`, package `org.overengineer.bettacraft`,
MIT licence (code, generator and all assets). Target: the ATM10 modpack (NeoForge 21.1.x; built against
`neo_version` in `gradle.properties`, runs on `[21.1.200,)`), Java 21, ModDevGradle, Gradle 9. The user is German and
writes in German; code, comments, commits, README and CHANGELOG are English, the handbook is English and German.

## Repo layout

| Path | What |
|---|---|
| `src/main` | the mod (the only code in the released jar) |
| `src/dev` | development-only source set, merged into the mod in every `gradlew run*` task but never in the jar: smoke scenes `DevSmoke*` / `DevClientSmoke*`, the showcase world (`command/DevShowcase`), GameTests (`gametest/`) and their template |
| `src/generated` | output of `gradlew runData` (recipes, tags, models, `en_us.json`, advancements), committed |
| `src/test` | JUnit tests (genetics, chemistry, lang completeness, config labels, ids, handbook generator `guide/GuideGen` + `GuideGenTest`, `GuidePagesTest`) |
| `tools/assetgen` | procedural textures, models, sounds, logo, handbook image crops, README media (Python, `py` launcher with PIL + numpy); outputs are committed |
| `modpack/` | files for modpack makers, not in the jar: FTB Quests chapter (made by `tools/modpack/ftbquests_chapter.py`), KubeJS example; `gradlew modpackZip` |
| `tools/ci` | `fetch_mods.py` + `power-mods.json` / `integration-mods.json`: pinned Modrinth jars for the `-Ppower` GameTests and the manual integration run in CI |
| `tools/upgrade` | builds the reference world of an old version for `smoketestUpgrade` |
| `docs/description.md` | the CurseForge mod page (English, short German summary); `gradlew curseforgeDescription` makes the paste-ready version with pinned image links |
| `docs/images` | README / CurseForge media (made by `tools/assetgen/readme_media.py`; banner by `preview.py`) |
| `.github/workflows` | `build.yml` (push to master, PRs; manual *full* run), `release.yml` (tag `v<mod_version>`), `integration.yml` (manual only) |

## Key systems (where to look)

- **Tank multiblock** `tank/`: `TankBlock`, `TankBlockEntity` (controller), `TankMultiblock`, `TankData` (+ schema and
  migrations in `data/schema/Schemas.java`), `TankCare` (care loop every 20 ticks), `TankWater`, `TankBreeding`,
  `TankPower`, `TankWaterVolume` (free shapes), `TankInteractions` / `TankInhabitants` (putting animals in).
- **Interior dimension** `tank/interior/`: a void pocket dimension at 16 blocks per tank block (`InteriorManager`,
  `InteriorRegion`, `InteriorEquipment` index, `Substrate`, `InteriorSync` deltas to the client). The client draws it
  in the tank as a **miniature** (`client/interior/MiniatureRenderer`, plant meshes `MiniaturePlants` /
  `PlantMeshRenderer`, sway `MiniatureSway`, Iris support `IrisCompat`).
- **Bettas and genetics**: `entity/betta/` (entity, care, swim goal), `genetics/` (loci, alleles, Mendel crosses,
  data-driven colours / genes / varieties, `BettaVarietyTable`), livebearer genetics `genetics/livebearer/`; renderers
  and composited skins in `client/render/`; models from `tools/assetgen/model_defs.py`.
- **Tankmates and critters**: fish `tankmates/` + `entity/tankmate/` (real entities); shrimp, snails, crayfish, hermit
  crabs are lightweight `critters/` stored in the tank data and synced. Wild spawns in `world/` and `sources/`.
- **Plants and corals**: `plants/` (`PlantSpecies`, `InteriorPlants`, growth in the interior), floating plants
  `tank/plants/`, reef `reef/` (corals, dosing pump, skimmer). Leaf / polyp meshes `client/interior/PlantMesh*`.
- **Equipment and tech**: `equipment/` (filters, cabinets, lights, feeders, air pump), `filter/` (multiblock filter),
  `plumbing/` (pipes, pumps, reservoir, water changer), `co2/`, `osmosis/`, `chem/` (TDS, pH, hardness), `crucible/`.
- **Food** `food/`: food kinds by tags (`BettaFood`), live food breeders and cultures, the live food pump (`LiveFood`,
  fluid `bettacraft:live_food`), the food maker (`FoodMakerBlockEntity`, recipes `bettacraft:food_maker`) and food
  variety (`FoodDiet`: food groups and diets as item tags, the diet memory in `BettaHealth`).
- **Menu**: `client/screen/TankScreen` (tabs) + `tank/menu/`. Info overlays (Jade/TOP/WTHIT) share `compat/info`.
  Other mods only through ids, tags and reflection (`compat/`, `datagen/Compat.java`), no compile dependencies.
- **Config**: `config/ServerConfig`, `CommonConfig`, `ClientConfig`; every option needs an en label (datagen `LangEn.java`
  → `runData`) and a de label (`assets/bettacraft/lang/de_de.json`), see `ConfigLangTest`.
- **Feature switches** (M33) `config/`: the tree `features.<group>.enabled` + sub-switches in the common config, one enum
  `Feature` (path, parent, `requires`, old pre-M33 keys, en label + tooltip; de in `de_de.json`). Ask only
  `Features.enabled(Feature.X)` (own switch, parents and requirements). `FeatureContent` maps items, recipes and source
  targets to features; datagen gives every recipe a `bettacraft:feature` condition (`FeatureCondition`), loot modifiers a
  `bettacraft:feature` loot condition, the trader offers, net tables and spawn rules check `Features`. Old flat keys are
  moved once by `FeatureMigration` before the common config loads.
- **Handbook** (GuideME): `assets/bettacraft/guides/bettacraft/guide/` (en) and `_de_de/` (de), images in
  `guide/images/` cropped from smoke screenshots by `tools/assetgen/guide_images.py`. Obtaining sections, profiles and
  species tables are generated from the game data: sources in `sources/` (`WildTables`, `TraderOffers`, `Sources`),
  page catalogue in `src/test/.../guide/GuideGen`, then `gradlew updateGuide`.
- IDs must never contain `aquarium`, `fishtank`, `fish_tank`, `fishbowl`, `fish_bowl` (`CompatIdsTest`).

## Build and test

Always add `--no-configuration-cache` to `runClient`, `runServer`, `runData` and the GameTest runs.

```
./gradlew build                                   # jar + unit tests + verifyReleaseJar
./gradlew runGameTestServer --no-configuration-cache          # GameTests (add -Ppower for the FE mods)
./gradlew integrationTests --no-configuration-cache           # optional integration suite: every integrated mod (~6 min)
./gradlew runData --no-configuration-cache        # regenerate src/generated
./gradlew updateGuide                             # regenerate the data-driven handbook sections
./gradlew runServer -Psmoketest --no-configuration-cache      # server smoke (also with -PnoCompat)
./gradlew runClient -Psmoketestm25c --no-configuration-cache  # a milestone's client smoke scene (background window)
./gradlew runClient -Psmoketestreadme -PnoJei --no-configuration-cache # README media, then py tools/assetgen/readme_media.py
./gradlew runMultiplayerTest --no-configuration-cache         # dedicated server + two background clients
./gradlew smoketestUpgrade                        # loads a reference world of an old version, nothing may be lost
./gradlew buildShowcase                           # showcase world "Bettacraft Schaukasten" (then -Pshowcasetour)
./gradlew verifyReleaseJar                        # jar has no dev/GameTest code, no foreign assets, right version
./gradlew releaseNotes                            # build/release-notes.md from the top CHANGELOG section
./gradlew curseforgeDescription                   # build/curseforge/ description (md + html) from docs/description.md
```

- Client smoke scenes: one `DevClientSmokeMxx` per milestone, flag `-PsmoketestmXX` in `build.gradle`, screenshots in
  `run/screenshots/bettacraft_mXX_*.png`; freeze animals with `SmokeFreeze.freeze`. Look at the screenshots.
- Optional dev mod groups, synced from local folders into the run's mods folder (jars are not in git; copy them from a
  local ATM10 instance): `-Pshaders` (`run/shader-mods`: Sodium, Iris; Complementary in `run/shaderpacks`), `-Ppower`
  (`run/power-mods`), `-Paquatic` (`run/aquatic-mods`), `-Pbiomes` (`run/biome-mods`: biome mods such as Regions Unexplored and Oh The
  Biomes We've Gone; servers then use the world `run/world_biomes`; the biome tags `#bettacraft:*_waters` map their
  biomes to the wild animals, see `world/` and the GameTest `biomeModBiomesMapToTheirWaters`), `-Pstorage`
  (`run/storage-mods`: Applied Energistics 2 and Refined Storage; `StorageGameTests` move items with their buses), `-Pcreate`
  (`run/create-mods`: Create 6, Create Aeronautics bundled and Sable from ATM10AA; `CreateGameTests` move tanks on a bearing and a
  Sable sub-level; the travelling tanks are `tank/TankMoves`, `compat/create/`), `-Pquests` (`run/quest-mods`: FTB Quests +
  libraries; the chapter of `modpack/` is copied into the run, `QuestGameTests`), `-Pkubejs` (`run/kubejs-mods`: KubeJS, Rhino;
  the example script is copied into the run), `-Pmaps` (`run/map-mods`: JourneyMap, client only; alarm waypoints), `-Pfacades`
  (`run/facade-mods`: Cable Facades; its block tag `cable_facades:supports_facade` is in our data, `PipeCoverGameTests`; Mekanism
  Covers lies in `run/power-mods`). The public
  events for scripts are in `api/event` (posted by `api/ApiEvents`). Leave runtime mods out
  with `-PnoJade`, `-PnoJei`, `-PnoGuideme`, `-PnoCompat`; add `-Ptop`, `-Pwthit`. `-Pbackground` makes a plain
  `runClient` a background window.
- **Optional integration suite** (M42, never part of `build`/`check` or the normal `runGameTestServer`): `gradlew
  integrationTests --no-configuration-cache` (or any run task with `-Pintegration`) switches on every group above that we
  integrate (power, aquatic, biomes, storage, create, quests, kubejs, facades; maps for `runClient`) plus `run/integration-mods`
  (Integrated Dynamics + Tunnels, Cyclops Core, Common Capabilities, XNet, McJtyLib, RFTools Base, LaserIO, Modular Routers,
  Industrial Foregoing, Athena and Farmer's Delight for EnderIO's models and Ice and Fire's recipes, from ATM10AA). The GameTest server then runs in `run/gametest-integration`: the whole normal suite plus
  the integration-only tests of `gametest/integration` (registered by `IntegrationSuite` only with `bettacraft.integration`;
  they fail when a mod of the group is missing, `IntegrationGroupTests` also checks every foreign id of our data). About 6
  minutes for ~750 tests. `-Pgametests=<prefix>[,...]` runs only the tests whose method name or batch starts with a prefix
  (e.g. `-Pgametests=bettacraft_integration`), also for the normal suite. The client side: `runClient -Pintegration
  -Psmoketestm42` (JEI, GuideME), `-Psmoketestm8` (overlays; with `-Ptop -PnoJade` / `-Pwthit -PnoJade`), `-Psmoketestm32d`
  (JourneyMap). CI: the manual workflow `integration.yml` runs `integrationTests -PintegrationPartial` with the open-licence
  jars of `tools/ci/integration-mods.json`; tests whose mods are missing there pass with a log line.
- CI (`build.yml`): build, changelog section, `runData` must not change `src/generated`, GameTests, server smoke; the
  manual *full* run adds smokes without optional mods / with TOP + WTHIT and GameTests with `-Ppower`. `release.yml`
  on a `v<mod_version>` tag: full tests, GitHub release, CurseForge upload (the only mod site; no Modrinth). The local release checklist is
  `RELEASING.md` (not committed).

## Working rules

- **Commits:** English messages, logical steps, add paths explicitly (never `git add -A` at the root).
  **No AI attribution** in commits or PRs: no `Co-Authored-By`, no "Generated with" lines.
- **Never commit planning docs** (local only): `PLAN.md`, `OFFEN.md`, `SONSTIGES.md`, `ASSETS-HOW.md`,
  `optional-features.md`, `manual-testing.md`, `RELEASING.md`, `claude-feature-ideas.md`, the folder `meilensteine/`;
  also nothing under `run/` or `builds/`.
- **Milestones:** read `meilensteine/AGENT-RULES.md` first (local). Open questions go into `meilensteine/mXX-offen.md`;
  if they are unanswered, stop and ask. Branch from `master`, fast-forward `master` only when everything is green;
  push, tag or publish only with the user's OK. Finish with version bump, CHANGELOG section, README, handbook.
- Every new feature gets a switch in the feature tree: a `Feature` constant (a new group `<group>.enabled` or a child of an
  existing group, never one switch per milestone), en label/tooltip in the enum, de in `de_de.json`; code asks
  `Features.enabled(Feature.X)`, new items/recipes/sources get their feature in `FeatureContent` (recipes then get their
  condition in `runData`), and the switch gets a GameTest that turns it off. The handbook is kept up to date in English
  and German together (navigation, a page per species / device with `item_ids`, an Obtaining section).
- GameTests are deterministic (fixed seeds, config-changing tests in their own batch, config restored on failure).
- **Minecraft clients only in background mode** (`-Psmoketest*` flags or `-Pbackground`: unfocused window on the
  secondary monitor) – the user may be working on the PC. Servers / GameTest servers have no window and are fine.
- **Never kill java / gradle processes you did not start** (other agents or the user may be running the game). Do not
  rebuild while a dev client you started is still running.
- **No third-party assets or code** (other mods are ARR): textures, models and sounds are procedural from
  `tools/assetgen`; foreign mod jars are only read or loaded in the dev runtime and never committed.
- Things that cannot be tested automatically go into `manual-testing.md` (local).

## Local planning files (not in git, may be missing)

`PLAN.md` (roadmap), `meilensteine/` (`AGENT-RULES.md`, one report per milestone, `README.md` overview),
`manual-testing.md`, `optional-features.md`, `RELEASING.md`, `OFFEN.md`, `SONSTIGES.md`, `ASSETS-HOW.md`.
The test modpack is a local PolyMC/Prism instance of ATM10; copy optional mod jars from its `mods` folder into the
`run/*-mods` folders (read only, never move or edit the instance).
