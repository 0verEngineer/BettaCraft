# Bettacraft – notes for Claude Code

Betta fish and aquarium mod for Minecraft 1.21.1 (NeoForge). modid `bettacraft`, package `org.overengineer.bettacraft`,
MIT licence (code, generator and all assets). Target: the ATM10 modpack (NeoForge 21.1.x; built against
`neo_version` in `gradle.properties`, runs on `[21.1.200,)`), Java 21, ModDevGradle, Gradle 9. M52: multiloader layout
(`common/` + one module per loader), see the Ports section. The user is German and
writes in German; code, comments, commits, README and CHANGELOG are English, the handbook is English and German.

## Repo layout

| Path | What |
|---|---|
| `common/src/main` | the mod, loader-independent (compiled against vanilla only, NeoForm mode); everything loader-specific goes through `platform/` (`Platform`, `ClientPlatform`, shims with NeoForge's names: `DeferredRegister`, `IItemHandler`, `FluidStack`, `PacketDistributor` ...) |
| `common/src/generated` | output of `gradlew runData` (recipes, tags, models, `en_us.json`, advancements), committed |
| `common/src/test` | JUnit tests (genetics, chemistry, lang completeness, config labels, ids, handbook generator `guide/GuideGen` + `GuideGenTest`, `GuidePagesTest`); compiled in common, run by the neoforge module |
| `common/accesstransformer.cfg` | the vanilla members NeoForge's AT opens and common uses (compile only) |
| `neoforge/src/main` | the NeoForge mod: entry point `neoforge/BettaCraftNeoForge`, platform implementation, capabilities, NeoForge event glue (`*NeoForgeEvents`), datagen, baked models, JEI/Jade/WTHIT/TOP/GuideME/Create compat; jar = common + neoforge |
| `fabric/` | the Fabric mod (Loom): `fabric/BettaCraftFabric` (entry point), `FabricPlatform`, `FabricRegistration`, `FabricNetwork`, `FabricCapabilities`/`FabricTransfer` (transfer API, TR Energy), `FabricGameEvents` + `mixin/` (NeoForge events), `FabricWorld` (spawns, loot), `FabricConditions`, client `FabricClientPlatform`/`FabricDynamicModel`/`FabricModels`; dev: `fabric/src/dev` (smoke, GameTest runner, stubs in `src/devstubs`); handbook converter `tools/guide` |
| `neoforge/src/dev` | development-only source set, merged into the mod in every `gradlew run*` task but never in the jar: smoke scenes `DevSmoke*` / `DevClientSmoke*`, the showcase world (`command/DevShowcase`), GameTests (`gametest/`) and their template |
| `tools/assetgen` | procedural textures, models, sounds, logo, handbook image crops, README media (Python, `py` launcher with PIL + numpy); outputs are committed |
| `modpack/` | files for modpack makers, not in the jar: FTB Quests chapter (made by `tools/modpack/ftbquests_chapter.py`), KubeJS example; `gradlew modpackZip` |
| `tools/ci` | `fetch_mods.py` + `power-mods.json` / `integration-mods.json`: pinned Modrinth jars for the `-Ppower` GameTests and the manual integration run in CI |
| `tools/upgrade` | builds the reference world of an old version for `smoketestUpgrade` |
| `docs/description.md` | the CurseForge mod page (English only); `gradlew curseforgeDescription` makes the paste-ready version with pinned image links |
| `docs/images` | README / CurseForge media (made by `tools/assetgen/readme_media.py`; banner by `preview.py`) |
| `.github/workflows` | `build.yml` (push to master, PRs; manual *full* run), `release.yml` (tag `v<mod_version>+<loader>-<mc>`), `integration.yml` (manual only) |

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
  page catalogue in `common/src/test/.../guide/GuideGen`, then `gradlew updateGuide`.
- IDs must never contain `aquarium`, `fishtank`, `fish_tank`, `fishbowl`, `fish_bowl` (`CompatIdsTest`).

## Build and test

Always add `--no-configuration-cache` to `runClient`, `runServer`, `runData` and the GameTest runs.

```
./gradlew build                                   # jar + unit tests + verifyReleaseJar
./gradlew runGameTestServer --no-configuration-cache          # GameTests (add -Ppower for the FE mods)
./gradlew integrationTests --no-configuration-cache           # optional integration suite: every integrated mod (~6 min)
./gradlew zooTests --no-configuration-cache                   # optional zoo suite: mods we do NOT integrate (animals, plants)
./gradlew runData --no-configuration-cache        # regenerate common/src/generated
./gradlew updateGuide                             # regenerate the data-driven handbook sections
./gradlew runServer -Psmoketest --no-configuration-cache      # server smoke (also with -PnoCompat)
./gradlew runClient -Psmoketestm25c --no-configuration-cache  # a milestone's client smoke scene (background window)
./gradlew runClient -Psmoketestm43 -PnoJei --no-configuration-cache   # README / mod page media in a copy of the showcase world
                                                  # (after buildShowcase; again with -Pshaders), then py tools/assetgen/readme_media.py
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
  they fail when a mod of the group is missing, `IntegrationGroupTests` also checks every foreign id of our data). About 5-6
  minutes for ~750 tests. `gradlew zooTests` (below) takes about 7 minutes. `-Pgametests=<prefix>[,...]` runs only the tests whose method name or batch starts with a prefix
  (e.g. `-Pgametests=bettacraft_integration`), also for the normal suite. The client side: `runClient -Pintegration
  -Psmoketestm42` (JEI, GuideME), `-Psmoketestm8` (overlays; with `-Ptop -PnoJade` / `-Pwthit -PnoJade`), `-Psmoketestm32d`
  (JourneyMap). CI: the manual workflow `integration.yml` runs `integrationTests -PintegrationPartial` with the open-licence
  jars of `tools/ci/integration-mods.json`; tests whose mods are missing there pass with a log line.
- **Optional zoo suite** (M42 part b, the other half of the optional suite, never in `build` or the normal suite): `gradlew
  zooTests --no-configuration-cache` (or any run task with `-Pzoo`) puts `run/zoo-mods` (mods of ATM10AA with animals or
  water plants that we do NOT integrate: Aether, Ars Nouveau + Elemental, Productive Bees, Occultism, Thaumaturge, EvilCraft,
  Actually Additions, Mahou Tsukai, Psychadelic Chemistry, Fungi Delight, Oritech Things, Garnished + libraries; not
  `run/integration-mods`) on the run with the groups aquatic, biomes and create, GameTest dir `run/gametest-zoo`. The zoo
  tests (`gametest/zoo`, registered by `ZooSuite` only with `bettacraft.zoo`) survey every foreign mob and water plant without
  a profile and write what they do in our tanks, terraria and paludaria to `run/gametest-zoo/zoo/*.tsv`; they fail only on
  bugs of ours (exceptions in our code, an animal lost or doubled when taken out, put back, packed or released, refused animals
  left in the tank, a breakable inhabited tank, a plant lost when the tank breaks). Client: `runClient -Pzoo -Psmoketestm42b`
  (the miniature with those animals and plants; the Aether stays out of the dev client, its Accessories mixin crashes there).
- **Breeding runs** (M42 part c, in the normal suite): `GenerationsGameTests` breed bettas, guppies, Neocaridina (by grade),
  ramshorns, axolotls, dart frogs, sliders (TSD), leopard gecko morphs, bamboo sharks, stingrays, tropical fish by a test
  profile, Fintastic guppies and Aquaculture box turtles over two to four generations; the chosen young go by the menu's
  take-out (bucket, bag, net, tub, box) into new tanks (`breeder`, `takeOut`, `putIn`, `move` are public for other
  packages). Config only in their own batches through `withConfig` (also turns `breeding.mutations` off; guppies still
  mutate at 0.4 %). The care uses the level's random and the inhabitants' order, so their numbers differ from run to run:
  the assertions are statistical with wide margins. `-Pgametests=generations`; the koi of Critters and Companions run in the
  integration suite (`IntegrationBreedingTests`, batch `bettacraft_integration_breeding`).
- Other M42 test switches: `runMultiplayerTest -Pmplag` (dev `DevNetLag`: 40-250 ms both ways, every second interior
  delta lost), `bash tools/upgrade/chain.sh` (a reference world saved by an intermediate version, then `smoketestUpgrade
  -PupgradeWorld="Bettacraft Kette 1.13.1-1.31.0"`). Every GameTest run deletes its old world first (it used to grow by every
  run and filled the disk).
- CI (`build.yml`): build, changelog section, `runData` must not change `common/src/generated`, GameTests, server smoke; the
  manual *full* run adds smokes without optional mods / with TOP + WTHIT and GameTests with `-Ppower`. `release.yml`
  on a `v<mod_version>+<loader>-<mc>` tag (see Ports): full tests of that loader, GitHub release, CurseForge upload (the only mod site; no Modrinth). The local release checklist is
  `RELEASING.md` (not committed).

## Ports (M52)

Since 1.42.0 the mod is multiloader: `common/` (all game code, vanilla only), `neoforge/` and `fabric/` (thin glue).
Build: ModDevGradle (common in NeoForm mode, neoforge) + Fabric Loom (fabric), the layout of the MultiLoader-Template
(no Architectury, no runtime API mod). Both jars land in `build/libs/` (`bettacraft-neoforge-<mc>-<v>.jar`,
`bettacraft-fabric-<mc>-<v>.jar`), one CurseForge project, one file per loader and version.

| Branch | MC | Loaders | Java | Maintenance |
|---|---|---|---|---|
| `master` | 1.21.1 | NeoForge + Fabric | 21 | full (main target ATM10) |
| `1.21.11` (M52b) | 1.21.11 | NeoForge + Fabric | 21 | full |
| `26.1.2` (M52c) | 26.1.2 (Fabric: 26.1-26.1.2) | NeoForge + Fabric | 25 | full |
| `26.2` (M52d) | 26.2 | NeoForge + Fabric | 25 | full |
| `26.3` (M52e) | 26.3 (newest) | NeoForge + Fabric | 25 | full |

Fabric on 1.21.1 (this branch). Gradle 9.8.2 with the daemon on JDK 25 (`gradle/gradle-daemon-jvm.properties`, needed by
Fabric Loom 1.19.4; the game still targets Java 21):
```
./gradlew :fabric:build                                    # Fabric jar (+ Patchouli book from the GuideME pages, verifyFabricJar)
./gradlew runFabricGameTest --no-configuration-cache       # Fabric GameTests (shared classes; -Pgametests=<prefix>; with mod groups below)
./gradlew runFabricServer -Psmoketest --no-configuration-cache
./gradlew runFabricClient -Psmoketestfabric --no-configuration-cache   # background client: M11 + M13 scenes, run-fabric/screenshots
./gradlew runFabricClient -Psmoketestbook -Poverlays|-Prei|-Pemi --no-configuration-cache   # handbook (Patchouli) en+de + recipe viewer
./gradlew prodFabricServer -Psmoketest / prodFabricClient -Psmoketestfabric   # production (intermediary) runs with official jars
./gradlew fabricMultiplayerTest --no-configuration-cache   # Fabric server + two background clients (Dev1, Dev2)
./gradlew fabricSaveLoadTest --no-configuration-cache      # showcase world built on Fabric, reloaded twice, nothing lost
./gradlew runFabricGameTest -Pfabric_loader_version=<min> -Pfabric_api_version=<min> -PexactFabricVersions --no-configuration-cache   # the declared minimums exactly (fabric.mod.json, README)
```
- Fabric dev mod groups (official Fabric jars in `run-fabric/<group>`, not in git; nested libraries are extracted for the
  dev runtime): `-Poverlays` (Jade, JEI + Mezz Config, WTHIT + Bad Packets, The One Probe), `-Prei` (REI, Architectury,
  Cloth Config), `-Pemi`, `-Pfabricintegration` (Refined Storage, JourneyMap + Common Networking, Better Fish Tanks + MINT
  Lib, Naturalist, Fish of Thieves + YACL, Critters and Companions + GeckoLib, Architectury, MidnightLib, Cloth Config),
  `-Pquests` (FTB Quests, Library, Teams from FTB's maven; the chapter of `modpack/` is copied into the run).
- Fabric differences: config by Forge Config API Port (same TOML files and keys), Mod Menu for the screen; transfer API
  items/fluids (81 droplets = 1 mB) and Team Reborn Energy instead of capabilities; the handbook is a **Patchouli** book
  converted at build time from the GuideME pages (`tools/guide`, single source: edit the GuideME pages only); recipe
  conditions and NeoForge ingredient types of our data are converted at build time (`fabricData`), biome / loot
  modifiers come from code (`fabric/FabricWorld`); NeoForge events by Fabric callbacks and mixins (`fabric/mixin`).
  Jade/JEI/WTHIT/TOP plugins are shared (`neoforge/compat/{jade,jei,wthit,top}` compiled into both jars), REI and EMI
  plugins live in `fabric/compat`. Integrations that exist only on NeoForge (Create, AE2, KubeJS, GuideME, Aquaculture,
  Fintastic, Cable Facades) stay NeoForge-only. Blocks whose behaviour depends on the block entity (stone cabinets,
  covered pipes) implement `platform/PositionalBlock`; Fabric has no light by position (a glowing pipe cover does not
  emit light there).
- The Fabric dev source set compiles the NeoForge dev files that need nothing else of NeoForge (GameTests, a few smoke
  scenes); a NeoForge dev file using NeoForge APIs is left out automatically, `fabricDevSkip` in `fabric/build.gradle`
  lists the ones that fail otherwise. Prefer `Platform` / `GameTestCompat` in new GameTests so they run on both.

**Rule for every milestone from M53 on:**
- New code goes to `common/` (loader-free); only what a loader really needs goes to `neoforge/` / `fabric/`, behind
  `platform/`. A new NeoForge-only API use needs its Fabric counterpart in the same milestone (or a documented gap).
- Work on `master` first (1.21.1, both loaders). Green means: the NeoForge suites as before **and** `:fabric:build`,
  `runFabricGameTest`, `runFabricServer -Psmoketest` (client smoke when rendering changed).
- Then, in the same milestone, forward to all four port branches (`1.21.11`, `26.1.2`, `26.2`, `26.3`) by cherry-pick:
  every branch is fully maintained and gets every feature and every fix. MC-free parts apply cleanly, GUI / rendering by
  hand. Each port branch builds, tests and releases on its own (its own reference world for `smoketestUpgrade`, its own
  CHANGELOG section).
- **A milestone is done only when all 10 combinations (5 Minecraft versions x NeoForge + Fabric) are green** (the
  checks above, on every branch). Something that cannot exist on a version or loader (the foreign mod has no build
  there; checked on Modrinth, CurseForge and the mod's maven) is a documented gap (CHANGELOG, `VersionGaps`).
- Every foreign mod added for testing (a new integration, mod group or single test jar, on any branch and loader) goes
  into the reproducible test suite: pinned with its official source and hash in the branch's dev mod manifest, so a
  fresh clone runs the test without manual work (until that manifest exists: listed for it in the local planning notes).
- Push, tag and release of any branch only with the user's OK.
- **Release tags** (M52): `v<mod_version>+<loader>-<mc>`, e.g. `v1.42.0-beta.1+neoforge-1.21.1`,
  `v1.42.0-beta.1+fabric-26.1.2`; one tag per loader, on the branch of that Minecraft version. `release.yml` (parsing in
  `tools/ci/release_tag.sh`) fails at once when the mod version is not `mod_version` or the Minecraft version is not
  `minecraft_version` of the tagged commit, runs the full tests of that loader (`build.yml` input `loader`), builds only
  that loader's jar and uploads that one file to CurseForge (loader, Minecraft versions incl. widened ranges such as Fabric
  26.1-26.1.2, that loader's dependency slugs). A manual run (choose the loader) is a dry run.

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
  condition in `runData`), and the switch gets a GameTest that turns it off.
- **Handbook stays current:** every new or changed player-facing thing (species, device, block, mechanic, config option,
  menu) is in the handbook in English and German together: navigation, a page per species / device with `item_ids` for
  every new item, an Obtaining section, images from the smoke screenshots; then `gradlew updateGuide` for the generated
  sections (`GuidePagesTest` must stay green).
- **Quests stay current:** every new player-facing feature (species group, device, system) gets or extends a quest in the
  FTB Quests chapter: `tools/modpack/ftbquests_chapter.py` (advancement or item task, dependency, position), texts
  `quest.bettacraft.<quest>.title/.desc` en (datagen) + de, then `py tools/modpack/ftbquests_chapter.py`; `QuestChapterTest`
  and the `-Pquests` GameTests stay green.
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
