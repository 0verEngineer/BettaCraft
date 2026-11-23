# Changelog

All notable changes to Bettacraft. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versions follow [Semantic Versioning](https://semver.org/). The release workflow publishes the section of the tagged
version as release notes.

## [1.53.0-beta.1] - 2026-11-23

Tank optics: connected glass at a distance, stalks on their base, swaying roots, the light on the lid, a shorter overlay.

### Added
- **Light bar on a lid** (`features.equipment.lightOnLid`): the aquarium light bar can lie directly on a tank lid instead of
  in the block above it. Put the bar onto the top of a lid, or slide a lid under a bar standing on a tank; the lid stays in
  the same block under the bar, the tank counts as covered and gets the light; breaking it gives back the bar and the lid.
  An LED panel directly on a tank already covered the blocks under it like a lid (no evaporation, closed for animals that
  fly out and mobs that climb in); its info text and the handbook say so now.

### Changed
- **Tank info overlay** (Jade, The One Probe, WTHIT): the first line is the tank's name (a tank without a name: its kind,
  aquarium, paludarium or terrarium) instead of the block name "Tank". Without sneaking it shows only the air of a terrarium
  or paludarium and the water quality (and how many warnings there are); everything else comes with sneaking. A terrarium
  shows no water line and no water quality any more.
- The roots of the floating plants (frogbit, salvinia, red root floater, water lettuce) and the hornwort shoots sway in the
  water (`render.swayingPlants`); the hornwort shoots are shorter (0.6 instead of 0.95 blocks) and looser.
- The air hose of an air pump goes down the glass to the floor and ends there; it no longer runs over the floor to the air
  stones and sponge filters.
- Development: the client smoke `-Psmoketestm64` (both loaders; `-Pm64parts=glass,hose,coral,float,lid`, `-Pm64packs`, `-Pm64tag`): big
  textured tanks from 8 to 64 blocks and at a flat angle with a guard that fails when the block borders of a face stand
  out from the middle of the blocks (every view also the old way, which shows the grid), the air hoses, mushroom corals
  inside and outside, swaying roots, the light on lids and an LED panel on a tank.
- Development: the plant mesh test checks every species (aquarium plants, corals, terrarium plants) for pieces that do not really touch
  what they grow from (within 0.15 blocks) and for pieces at the bottom that stand on nothing.

### Fixed
- **Connected glass broke up at a distance:** from far away, or at a flat angle from nearer, every block of a textured tank
  showed its own glass frame again (a grid over the whole tank, with and without shader packs). The frame was hidden only by
  stretching the texture over it, which works at full size but not for the smaller mipmaps the game uses at a distance:
  they mix the frame into the pixels next to it. The block atlas now holds frameless copies of every glass texture (one for
  each combination of hidden edges, made from the texture itself, so resource packs and the glass of other mods work), and
  their mipmaps have no frame where the tank goes on.
- **Mushroom coral stalks were not connected to their base:** the stalks of the outer discs began a third of the way out
  at the height of the root and ended in the water beside the rock, and every disc floated a little above its stalk. All
  stalks start on the coral's base now and reach the disc. The same kind of gap was closed in the bubble tip anemone (its
  column ended below the oral disc, the tentacles started under it), the mini orchid (flowers beside the end of their
  spike), Sedum (flowers above the tufts) and Mammillaria (flowers beside the ball). Every plant, coral and decoration was
  checked (the plant mesh test above; every decoration template has all its blocks on the ground).
- **CurseForge description images did not load:** `gradlew curseforgeDescription` put the release tag verbatim into the
  image and file links (`.../v1.52.0-beta.1+neoforge-26.3/docs/images/...`); CurseForge turned the '+' into a space and every
  image failed (404). The tag is URL-encoded in the links now ('+' as '%2B', which GitHub accepts), and the task fails on a
  GitHub link with an unencoded ref.

## [1.52.0-beta.1] - 2026-11-22

Tank water without block seams, with and without shaders.

### Fixed
- **Block rectangles on the tank water:** the water took the light of its own block, so in an unevenly lit room (a torch,
  a lamp, a roof over part of the tank) every block of the surface had its own brightness. Shader packs (Complementary
  Reimagined and Unbound, BSL, MakeUp) showed the surface as a grid of block-sized rectangles, without shaders they were
  faint. The water is now lit smoothly: every corner takes the brightest light around it and the light runs on from block
  to block. The surface stays flat and still, the view from the front and the side stays clear. Checked with Complementary
  Reimagined, Complementary Unbound, BSL and MakeUp (Iris) and without shaders.
- **Tank water with Sodium or Iris (26.x):** Sodium meshed the tank water as vanilla water (the flowing texture on the
  sides, a sloped surface; with a shader pack on top of the tank's own water, so the tank looked nearly opaque). The tank
  water is now drawn as always: a flat, still surface and clear sides.
- Two flaky GameTests: a "waiting" betta in `aWaitingBettaKeepsItsHeight` swam on (the test helper removed the goals but
  did not stop a swim already under way; it now stops the goals, the path and the straight swim, and a new test stops a
  betta mid-swim); `neonFormsCrossInTheTank` runs without the spontaneous mutations (a mutated young now and then).
- **A tank interior hung in the sky with shader packs:** with Iris and a shader pack (Complementary, BSL ...) the miniature of a
  tank was drawn at full size at the camera, moving with it (a castle, plants and decoration above the player), and the tank
  itself looked empty: Iris draws the terrain with the pack's programs, which take their transform from another uniform
  than the vanilla terrain shader. The miniature now gives Iris its transform too and stays in its tank. Without shaders
  nothing changed. In 1.51 as well.
- **Open tanks looked closed:** a tank with glass texture drew the glass on its top faces as well, so the water of an open
  tank (no lid) looked like it lay under a pane with white streaks. The top faces have no glass any more (the thin edge
  lines stay); a lid is a block of its own. The item icon keeps its glass block look.
- `ChunkUnloadGameTests` failed now and then on CI (26.3, "the animals did not come back"): an idle GameTest server ticks a
  few hundred times a second, while chunks and entities load in real time; the 600 ticks it waited were half a second. Every
  wait now gets at least a minute of real time; nothing was lost.
- **A tank looked broken after a refused break (Fabric):** breaking a block of a tank that may not be broken (the break
  protection, as on every tank of the showcase, a claim, another mod that refuses the break) removed the block on the
  client for a moment; when the server said no, the block came back without its tank data: the tank lost its miniature,
  water, sign or glass until the world was joined again. NeoForge kept the data. The client now leaves the removal of a
  tank block to the server (both loaders), so nothing flickers and nothing is lost.

### Changed
- Development: the client smoke `-Psmoketestm63` (both loaders) shows two tanks with every shader pack of the manifest
  (Complementary Reimagined and Unbound, BSL, MakeUp; pinned in `dev-mods.json`) and without; `-Pgametestrepeat=<n>` runs
  every GameTest of a run n times side by side.
- Development: `-Psmoketestm63` adds a decorated open tank (`-Pm63parts=water,decor`) and checks with every pack that no
  miniature is drawn outside its tank; the new real-world tour `-Psmoketestworldtour` (both loaders) flies through a copy of
  the showcase (or the branch's upgrade reference world): every tank from the front, the side, above and just below the
  surface, and 360 degree sweeps with the sky, with and without shader packs; guards for miniatures outside their tanks and
  glass on open tops; `py tools/tour/compare.py` compares two tours (e.g. this version and the last release) picture by
  picture.
- Development: the real-world tour takes `-PtourViews=break` (the refused break of the lower right block of every tank,
  pictures before and after with and without the miniatures, the client's blocks and tank data around it against the
  server's) and `-PtourNames=<part of a tank name>,...`; `-Psmoketestm63` has the same check in a fixed scene
  (`-Pm63parts=break`); `prodFabricClient -Pforeground` starts the production client as a normal window.

## [1.51.0-beta.1] - 2026-11-22

Animals behave in the world as in the tank, cabinets under the whole tank, the external filter below the tank, a roomier
tank menu, new icons and a catch guard.

### Added
- **Schools in the wild:** wild shoaling fish (and fish you let go) swim as a school outside a tank, following a leader of
  their kind like in a tank; schooling rays swim behind and beside theirs (`features.tankmates.wildSchools`).
- **Climbing in the world** (`features.visuals.worldClimbing`): turtles, the tortoise, the leopard gecko and the corn snake
  climb one-block steps instead of jumping, with a climbing pose. The crested gecko climbs whole walls, to the top and
  over, or part of the way to hold on for a while. The mudskipper climbs logs and mangrove roots. Frogs still hop. Snails
  and shrimp that climb the glass in a tank climb walls under water in the wild and graze there for a while.
- **Cabinets under the whole tank:** cabinets join to the back as well, so a whole area under a big tank (e.g. 10 x 5)
  becomes one under-cabinet. The front row shows the fronts; the cabinets behind are a plain carcass with the counter top
  and the sides running through. The middle may be normal blocks. A cabinet placed against another takes its facing
  (`features.equipment.cabinetArea`).
- **One spring for the whole under-cabinet:** all cabinets under a tank, and the cabinets joined to them, share one spring
  store, even two that do not touch (both ends of a long tank). It grows with every cabinet, one crank click at any of them
  winds the whole store, and a broken cabinet hands its share to the others. The springs of older worlds form the store,
  nothing is lost (`features.power.cabinetSharedSpring`).
- **External filter below the tank:** the external filter may stand one or two blocks lower than its tank, beside the
  cabinet or without one. Sneak-right-click it with an empty hand and choose the tank from a list (name, position, side;
  the tank under the mouse is outlined); the hose runs the longer way up. An unconnected filter says so when placed, in
  its tooltip and in the Jade / TOP / WTHIT overlay. Filters right at a tank stay connected as before
  (`features.equipment.filterBelowTank`).
- **Small shoals named:** "A shoal of tankmates is too small" now says which fish and how many: "Neon Tetra: 3 of 6" in
  the overview (every small shoal in its tooltip), under the warning in the breeding tab and in the info overlays; the
  fish of a small shoal show in orange in the animal list.
- **Catch guard:** for half a second after a catch no container lets an animal go, so a second click of a bouncing mouse
  button or a held key does not release the animal you just caught (`features.equipment.catchGuard`).

### Changed
- **The tank menu grows with the screen:** where the GUI scale leaves room it gets wider and taller (up to 380 x 214 of
  content), with full-size tab labels, longer lists and lines and all warnings at once; names that do not fit end in "..."
  and show in full under the mouse; JEI, REI and EMI keep clear of it. On a small screen it is the menu as before.
- **Every catcher asks first:** the terrarium box with an animal of another mod, the transport tub and the mob jar now ask
  for the confirmation before they let an animal go into the world, like buckets and bags (the terrarium box let its
  animal go at once).
- **Every plant has its own icon:** the 57 aquarium plants and corals had 13 shared pictures; each now has its own, and
  plants that look alike differ clearly in colour. Nine terrarium plants that shared a look got their own. A test makes
  sure no two items of the mod look the same.
- **Tylomelania:** the rabbit snail has a round, high-spired shell that rests on its body; no shell reaches through the
  sole or the glass any more (the trumpet and assassin snails sit a little higher too).
- **Clear Glass Tank icon:** only its edges, a short gloss stroke and a small white sparkle; the Glass Tank keeps its glass
  streaks.
- Shorter labels: "Fish %s" on the small-animals page button, German "Alle" for any sex.

### Fixed
- Development: every dev server takes the branch's own port and checks it before it starts (never 25565, a clear message
  when it is busy): the old version's server of the upgrade reference builder (it ran on the port written into that old
  commit, shared by every branch, or on 25565), the showcase, the upgrade smoke, the multiplayer and the Fabric
  save/load tests; the builder takes a free port when the branch's one is busy (`DevServerPortsTest`).
- Tests: the multiplayer test lets the aquarium plants grow only once the light at the surface is there (in two runs on
  26.2 / 26.3 under load no plant grew).

## [1.50.0-beta.1] - 2026-11-22

The whole test suite runs from a fresh clone without manual work. No change to the game.

### Added
- **`dev-mods.json`**, the manifest of every foreign mod the tests load: for each loader and group folder (`-Ppower`,
  `-Paquatic`, `-Pbiomes`, `-Pstorage`, `-Pcreate`, `-Pquests`, `-Pmaps`, `-Pperf`, `-Pshaders`, `-Pkubejs`, `-Pfacades`,
  `-Ppipes`, `-Pcompare`, the integration and zoo suites, the Fabric groups `-Poverlays`, `-Prei`, `-Pemi`,
  `-Pfabricintegration`, `-Pvulkanmod`) the exact file with its official source (Modrinth Maven, CurseMaven, FTB Maven),
  size, SHA-1 and SHA-256, plus the Complementary Reimagined shader pack. `gradle/dev-mods.gradle` downloads a group the
  first time a run switches it on (Gradle cache, `--offline` works afterwards), checks both hashes (a mismatch stops the
  build) and fills the group folders. A build without a group flag downloads nothing. The jars are never committed.
- `tools/devmods/update_manifest.py`: rewrites the manifest from the group folders (finds each jar's official source by its
  SHA-1 on Modrinth, the FTB Maven or CurseForge). `-Plocal` loads `run/local-mods` (mods to try out before they go into the
  manifest); a jar in a group folder that the manifest does not list is moved to `run/local-mods-backup/<date>/`.
- `DevModsManifestTest`: every group folder the loader builds read is in the manifest (or a documented gap), every entry has
  an official source and its hashes, one file name is one file.
- `tools/upgrade/references.json`: the commit each upgrade reference world is built from; `smoketestUpgrade` and
  `runServer -PsmoketestOldWorld` build a missing reference world themselves (the old version in a git worktree next to the
  checkout). `-PsmoketestOldWorld` loads the reference world by default (`-PoldWorld=<name>` for another one).
- `gradlew smoketestWorldCleanup`: two server smokes in a row; the world is deleted after each and does not grow.

### Changed
- **Test worlds are cleaned up:** every world a test run makes (the server smoke's `run/world`, `world_biomes` and
  `oldworld_check`, the client smoke worlds, the GameTest, upgrade, multiplayer and Fabric save/load and production worlds)
  is deleted before the run and after a successful one (`run/world` had grown to 18 GB of tank interiors); `-PkeepWorld`
  keeps it for debugging. The showcase and worlds of your own are never touched.
- The dev servers write their `eula.txt` and `server.properties` themselves (offline mode for the multiplayer test, never
  the default port 25565: `dev_server_port` / `dev_fabric_server_port` in `gradle.properties`).
- CI: the manual workflow `integration.yml` runs the full integration suite, the zoo suite and the Fabric GameTests with the
  Fabric mod groups (choose one or all), with every mod of the manifest; the `-Ppower` GameTests of the full build use the
  manifest too. Only logs are uploaded, never a mod.
- README: a section "Running the tests" (clone, Java, flags, where the mods come from).

### Version gaps (26.1.2)
- The manifest of this branch lists 124 files. Groups without a build for Minecraft 26.1.2 (listed under `gaps` in `dev-mods.json`, checked on Modrinth, CurseForge and the mods' mavens): `neoforge/compare-mods` (the size comparison with DragN's Just Bettas (M36) is set up on 1.21.1 only); `neoforge/create-mods` (Create and Sable have no 26.1.2 build (VersionGaps)); `fabric/emi-mods` (EMI has no build after Minecraft 1.21.1 (Modrinth emi, CurseForge 580555, maven.terraformersmc.com checked 2026-11-22)).

### Fixed (tests only)
- The FTB Quests GameTests expected 113 quests; the chapter has 115 since the terrarium plants (found by the first
  integration run from a fresh clone).
- The upgrade smoke counted a small animal born while the old world loads as "not analysed" (failed on a freshly built
  reference world).
- `runServer -PsmoketestOldWorld` lost its fresh copy of the old world to the new cleanup; it now loads the reference world
  of the branch by default. On this branch that world already has the current schema (the branch starts with 1.42.0), so
  the check reports "not an old world": there is no older world for this Minecraft version; `smoketestUpgrade` covers the
  upgrade from 1.42.0.
- Multiplayer test from a fresh run directory: the background clients get their `fml.toml` (no early window) and, where
  NeoForge shows load warnings on a screen that waits for a click (26.2, 26.3), `neoforge-client.toml` with the warnings off;
  the seeded `server.properties` switches the white list off; the two hired aquarists get names of their own (two equal
  random names failed a check).

### Removed
- `tools/ci/fetch_mods.py`, `power-mods.json`, `integration-mods.json` and the partial integration run
  (`-PintegrationPartial`): CI loads the same manifest as a local run.

## [1.49.0-beta.1] - 2026-11-22

Pictures of every kind of tank, a short README and every feature in detail in FEATURES.md.

### Added
- **New pictures and animations** in `docs/images` (each still also with Complementary Reimagined shaders): a rainforest
  paludarium with dart frogs (animated), a brackish mangrove paludarium where the archerfish spit at the leaves (animated), a
  seahorse tank (animated), a desert terrarium whose geckos and corn snake walk (animated), a swamp paludarium with frogs, a
  rainforest terrarium full of terrarium plants (also from inside), a sea turtle in a sea water paludarium with a beach, the
  shark, the eagle ray, the wels catfish and the dolphins one by one, the aquarist villager, the hired aquarists at work,
  a monitor wall, a pond with the wild water and terrarium plants, the users tab of the tank menu, the animal icons of
  the containers and a tank on a Create minecart contraption. The pictures of before were taken again with this version.
- **`FEATURES.md`**: every feature in detail with its pictures (the old feature list of the README, brought up to date
  with the users tab, the map waypoints, the lively movement, the wild plants, the icons, the terrarium plants and the
  debug log).

### Changed
- **README:** the feature list is short and folded (click to open); it links to `FEATURES.md`. The picture table has the
  new tanks.
- **Mod page** (`docs/description.md`): the new pictures in their sections, a link to `FEATURES.md`, the map waypoints
  and a FAQ entry on bug reports; the counts are current (115 quests, 72 advancements, 144 feature switches).
- Development: `gradlew buildShowcase -PshowcaseWorld=<name>` builds the showcase world under another name, and the media
  run takes `-PmediaSource=<world>`, `-PmediaWorld=<copy>` and `-Pmediaparts=room,extras,showcase,m58,create` (the new
  scenes are part `m58`, the Create picture part `create` with `-Pcreate`).

### Fixed
- **A crash of the server** ("Ticking entity", `NullPointerException` in `LandWalker.tick`): a land animal in a terrarium
  or paludarium that got stuck more than ten times in a row dropped its goal and then read it again. Seen with Minecraft
  frogs on the bank of a paludarium; the animal now picks a new goal on the next tick.

## [1.48.0-beta.1] - 2026-11-22

A debug log and a bug report for finding problems.

### Added
- **Debug log** (`logs/bettacraft-debug.log`): Bettacraft writes what happens and why in nine areas: `care` (deaths with
  their reason and the conditions behind it, the tank log), `breeding` (nests with eggs and parents, larvae that survive
  with the care score, young born), `tank` (animals taken or refused with the reason, the tank blocks), `interior`,
  `network` (interior syncs with their size, packets per type and minute), `compat` (other mods and their errors),
  `render` (miniature statistics), `aquarist` (the jobs of the hired aquarist) and `world` (wild spawns); TRACE adds
  every care step and every packet. The file is written in the background, has at most 10 MB and keeps the one before it
  (`bettacraft-debug.1.log`); the normal log stays clean (info and warnings only).
- **Switching it:** common config `debug.logLevel` (INFO, DEBUG or TRACE; default INFO = off) and `debug.areas` (`all` or a
  list), also in the config screen and at once; the command `/bettacraft debug <area|all> <on|off|trace>` switches areas
  at once until the restart, `/bettacraft debug` shows the state, `/bettacraft debug reset` goes back to the config.
  Operators and the owner of a single player world (also without cheats).
- **`/bettacraft report`** writes `logs/bettacraft-report-<date>.txt` to attach to a GitHub issue: the versions of the mod,
  loader, Minecraft and Java, the integrations and every loaded mod, the config values that differ from the default, the
  active preset, the debug switches, the data of the tank you look at with its animals, the animal you look at and the
  end of the debug log. The chat shows the path (click to copy) and a link to the bug report form.
- **Issue forms** on GitHub: a bug report form (description, steps, Minecraft version, loader, mod version, modpack and a
  field for the report, logs and crash reports; it explains the commands) and an idea form; no field is required, blank
  issues stay possible. README (Reporting bugs) and the handbook (Settings > Reporting bugs) link to them.

### Changed
- Warnings that can repeat every tick (a broken script listening to our events, map mod waypoints, Better Fish Tanks,
  Create contraptions, packed animals, tank blocks without their controller, miniature mesh jobs) come at most once a
  minute, with the number left out.
- `logMiniatureStats` (client config) still writes the miniature statistics into the normal log; with the debug areas
  `render` / `network` they go into the debug log instead.
- With the debug log off (the default) a debug line costs one read of a field: nothing is built or formatted (JUnit: a
  million switched-off lines allocate nothing; the M33 benchmarks of care, pipes, monitors and terraria are the same as
  before, see the M57 report).

## [1.47.0-beta.1] - 2026-11-22

A fix for servers with Modern Dynamics.

### Fixed
- **Players could not join a server with Modern Dynamics pipes** (or were thrown out a few seconds later): Modern Dynamics
  reports the re-meshing of its pipes on the client with `sendBlockUpdated(pos, null, null, 0)`, and the plant mesh hook
  of Bettacraft read those missing block states ("Failed to handle packet ... NullPointerException in
  PlantMeshRenderer.onBlockChanged"). A block change without its states now counts as "no plant". The bug was there since
  1.42 (the first version with that hook on every block change).
- Stone cabinets and covered filter pipes no longer throw when another mod asks their map colour, destroy progress, sound,
  blast resistance, right tool or light without a level or position (vanilla ignores both there, so mods pass null).

### Changed
- Test only: a JUnit test calls the plant mesh hook as Modern Dynamics does; a GameTest asks the positional blocks with
  null arguments; the client smoke `-Psmoketestm61` calls `sendBlockUpdated` with null states next to stone and a plant
  and leaves and joins the world again; with the new dev mod group `-Ppipes` (Modern Dynamics 26.1.2.1 from Modrinth, NeoForge)
  it puts fluid pipes between our blocks first and fails on any "Failed to handle packet".
- Gap: Modern Dynamics has no Fabric build (NeoForge only since Minecraft 1.20.4; checked on Modrinth and CurseForge, it is
  not on a maven): on Fabric the client smoke runs without the pipes.

## [1.46.0-beta.1] - 2026-11-22

Terraria and paludaria get plants of their own.

### Added
- **36 terrarium plants** in three habitats (switch `features.terrariumPlants.enabled`):
  - **tropical and humid:** three bromeliads (Neoregelia, Guzmania, Vriesea), the air plant, Fittonia, Pothos, Marcgravia,
    Boston and bird's nest fern, cushion moss, reindeer lichen, Selaginella, a miniature orchid, the rex begonia, Peperomia;
  - **swamp and shore:** sundew, trumpet pitcher, tropical pitcher plant (Nepenthes), Venus flytrap, butterwort, swamp
    sedge, soft rush, papyrus, umbrella sedge;
  - **dry and hot:** golden barrel, column cactus, prickly pear, Mammillaria, Echeveria, Haworthia, aloe, agave, living
    stones, stonecrop, blue grama, needlegrass.
- They grow on the **land** of a terrarium or paludarium by its **light, humidity and air temperature** (each species its
  own ranges, shown in its tooltip); in the wrong climate or under water they **languish** and lose stages. Grown, they
  spread (side shoots, runners, offsets) and their cover raises the humidity (cacti and succulents hardly). The plants
  tab counts the languishing ones.
- **Propagation** with the shears: cuttings, **offsets** of the cacti, succulents, bromeliads and the air plant (a small
  rooted plant), **division** of the mosses (half the clump; breaking a piece of moss divides it too).
- **Wild** in their biomes (`#bettacraft:*_plant_lands`: jungles, lush caves, swamps and mangroves, deserts and badlands,
  savannas; `features.terrariumPlants.wildPlants`): shears cut a cutting off and the plant grows back.
- **Decoration in the world:** used outside a tank, a terrarium plant is planted on soil or sand (it does not grow there;
  broken, it gives its plant back).
- **The animals:** tortoises **graze** the prickly pear, stonecrop and the grasses (`tortoiseGrazing`); dart frogs carry
  their **tadpoles into the cups of grown bromeliads**, where they need no water and no food (`bromeliadTadpoles`);
  snakes and geckos **hide** among the plants (`hiding`).
- **Emersed growth:** aquarium plants above the water of a humid paludarium grow at half speed.
- From the wandering trader and the aquarist; a few from vanilla plants (cactus, fern, large fern, moss block, dead bush,
  short grass). Two advancements and two quests ("Green Thumb Under Glass", "A Nursery in a Cup"), handbook chapter
  "Terrarium plants" with a page per species.

### Changed
- The name field of the tank overview says just **"Name"**; that it shows on the sign is its tooltip (the long hint ran
  under the protection button). The other buttons of the tank menu were checked in English and German: "Analyse" in
  German is "Prüfen", the small animals button lost its brackets.

## [1.45.0-beta.1] - 2026-11-21

Every animal shows in its own icon, and the wild carpets and mosses look like plants.

### Changed
- **Wild carpets** (HC Cuba, Monte Carlo, Marsilea, Staurogyne, hairgrass) are **fine sprigs** standing apart on a few
  loose leaves, the ground showing between them, each species in its own leaf form (round leaves, four-leafed clover,
  leaf pairs, needles); the trimmed carpet is short and sparse. Before they were flat cushions with straight edges. Three
  designs were compared (a dense lawn, loose cushions with runners, the sprigs); the other two stay in the generator.
- **Wild mosses** (Java, Christmas, flame moss, Riccia) are **round cushions** of stacked leafy discs, the **Marimo** are
  **round balls** instead of cubes.
- **Every animal has its own icon in its container:** every tankmate bucket shows its species (rendered from its model; the
  buckets all showed a betta in two tints), the **critter bucket** shows the shrimp, crayfish, snail or hermit crab inside
  (the apple snail showed a shrimp), a **filled fish bag** shows the tankmate or small animal inside and a **filled
  transport tub** the big animal. Neocaridina, Caridina, the crayfish, apple snails and ramshorns keep the colour of their
  genes. A betta, a clutch and the animals of other mods keep the fish of before.

## [1.44.0-beta.1] - 2026-11-21

The animals move like the real ones: amphibians and reptiles keep their hours, climb, hide and do things while they sit
still, fish dart off now and then, and the small animals in the world crawl head first again.

### Added
- **Lively movement** of the amphibians and reptiles, the same in the world, in tanks and in the miniature (switch
  `features.visuals.livelyMovement`, on by default; off: the movement of 1.43):
  - **their own hours:** dart frogs, the mudskipper and the turtles by day; the geckos, the horned frog and the musk turtle at
    night; the corn snake at dusk and dawn. Outside their hours they rest more and longer.
  - **a pace like the real animal:** mostly calm, with **short fast phases** (a snake shoots forward, a lizard sprints off, a
    frog hops in quick series). The corn snake crawls 1.6 times as fast as before, the leopard gecko 1.4 times.
  - **the furnishing:** snakes and geckos **climb** onto branches, roots and rocks (faces up to almost a block), dart frogs
    sit on **leaves**, geckos, the snake and the turtles **hide** in caves and under overhangs (geckos by day), turtles and
    lizards bask under the basking lamp as before.
  - **things they do while they sit still:** the corn snake **raises its head**, **flicks its tongue** (also now and then
    while it crawls) and lies **coiled** while it rests; lizards look around, stretch up and wave the tail tip; frogs look
    around and call (the throat pumps); turtles look around and stretch their necks.
  - **in the world:** they rest by their hours (a gecko goes under cover by day), bask in the sun on bright days, walk at a
    calm pace with sprints and run from a player who comes close (not the turtles).
- **Fish** (with the same switch): small fish dart off now and then (the shoal follows its leader), bottom fish scoot,
  bettas dart; fish that are active by day rest more at night. **Crayfish** stay in their hides by day and come out at night.

### Changed
- The quest **Open House** (add a user to a tank) is optional: it needs a second player on the server (only online players
  can be added), so it blocks nothing in single player; its description says so.
- Every dev client of the build is silent (smoke, background and multiplayer test clients, `runClient`); `-Psound` to hear one.

### Fixed
- Shrimp, snails, crayfish and hermit crabs **in the world crawled backwards** (the apple snails showed it best); they now
  crawl head first like in the tanks.
- Frogs and the mudskipper **outside a tank glided** over the ground with their legs still; they hop now.
- A **betta that waited rose slowly to the surface**: its swimming lifted it every tick, also when nothing pulled it down
  (the tankmates had the same fault until 1.42).
- Four GameTests that failed now and then (statistics with an unseeded random): the albino bronze cory young, the lizard
  mother that takes her clutch along, the Banggai cardinalfish male, the fish in the water part of a paludarium.
- Texts: the users tab and the Open House quest no longer call every player "he" ("everything the owner may", "their own
  switch").

## [1.43.0-beta.1] - 2026-11-21 (Minecraft 26.1.2)

Tank users and alarm waypoints you can switch in the menu, waypoints in Xaero's Minimap and FTB Chunks, and every animal texture
checked from all sides.

### Added
- **Users tab** in the tank menu (it replaces the interior tab): the owner (who built the tank) lists other players as
  **caretakers** (may do everything the owner may: care, settings, going in, the users and the lock) or **viewers** (see the
  menu, change nothing, cannot go into the tank). Everybody else is ruled by the lock as before; players of the owner's
  FTB Teams party count as caretakers. Switch: `features.equipment.tankUsers`.
- **Alarm waypoints per tank and per user:** in the users tab every user switches his own waypoint of the tank (on by default
  for the owner, off for the other users), and the owner and the caretakers switch them off for all of its users.
- **Map mods:** the alarm waypoints also go into **Xaero's Minimap** (and its World Map) and **FTB Chunks**, besides
  JourneyMap; the client config has a switch per map mod (`map.journeyMap`, `map.xaerosMinimap`, `map.ftbChunks`), for packs
  with several of them.
- A **gear button** right of the interior button in the overview opens Bettacraft's client settings (on Fabric the same
  screen is now also in Mod Menu).
- Advancement and quest "Open House": add a user to a tank.
- `/bettacraft interior cleanup` (operators): what the interiors of removed tanks take on the disk; `... cleanup run` saves the
  world and cleans up at once. Switch for the automatic clean-up: `features.interior.cleanup`.
- `tools/assetgen/check_textures.py`: checks every entity texture against its model (holes, faces outside the texture,
  paint of a neighbouring face, stretched pixels) and the block and item models and textures.
- Development: `-Pperf` (performance mods: ImmediatelyFast, Entity Culling, ModernFix, FerriteCore, Distant Horizons, More
  Culling and others), `-Pshaders` and `-Pmaps` on both loaders, and a client smoke scene for the users tab, the map mods, the
  tank interior and every animal from six sides (`-Psmoketestm53`).

### Changed
- **Alarm waypoints go only to the owner and the listed users** of a tank, no longer to every player within 128 blocks.
- The client option `map.showAlarmWaypoints` is now called "Alarm waypoints of all tanks".
- The interior button of the overview is a little narrower; its tooltip shows what the interior tab showed (scale and size,
  blocks in the miniature, substrate, heaters and filters inside).
- The lock also lets the caretakers change a locked tank; "only the owner" actions are open to the caretakers too.

### Fixed
- **Tank interior on Minecraft 26.x:** the walls and the floor were black and darkened towards the edges (the dimension had
  no sky light factor and a black ambient light since 26.1: without a day timeline these attributes default to 0); it is
  bright again, as on 1.21.1.
- Corn snake: one side and the belly showed slivers of the wrong paint along the edges (the back saddles on the belly, the belly
  checks on the flank), and the belly checks doubled at random. The same edge slivers are gone from every small animal (shrimp,
  snails, crayfish, frog) and every terrarium animal.
- Alarm waypoints come back after the player leaves and joins again while the alarm is still on.
- **The tank interior dimension no longer grows without end on the disk.** Every broken, grown, split, merged or turned tank
  left its old interior in the region files for ever (18 GB in a long-used test world). Now a removed interior is deleted
  after the next save of the world (its chunks while the game runs, its region files at the next start; a log line with the
  count and size first), and its number goes to the next new tank. Existing worlds are cleaned up the same way when they are
  loaded. Empty interior chunks (only air) are no longer stored: a small tank takes 12 to 30 chunks (50 to 120 KB) instead
  of 90 to 130 (360 to 530 KB), the interior dimension of the server smoke test 1.3 MB instead of 7.3 MB. The interiors of
  standing, packed and travelling tanks are never touched; an old interior whose walls are still there is kept.
- **Iris shader packs:** views with a tank miniature showed large black rectangles (NeoForge). The miniature's mesh builders
  only got Iris's extended vertex format when they happened to be made while the level was drawn; most were plain, and the
  terrain pipeline read them with the wrong stride. The builders are now made on the render thread during the level render,
  and a mesh in another vertex format than its pipeline takes is never drawn (it is rebuilt).

### Known gaps (Minecraft 26.1.2)
- ModernFix has no Fabric 26.1.2 build (NeoForge: tested).

## [1.42.0-beta.1] - 2026-11-19 (Minecraft 26.1.2)

The same version for **Minecraft 26.1.2**, on NeoForge and Fabric: `bettacraft-neoforge-26.1.2-<version>.jar` and
`bettacraft-fabric-26.1.2-<version>.jar`. Java 25. Worlds do not move between Minecraft versions; within 26.1.2 they load
as usual. Every Minecraft version gets every new feature and every fix.

### Added
- **Minecraft 26.1.2**: NeoForge 26.1.2 and Fabric (Fabric Loader 0.19.0+, Fabric API 0.155+, Forge Config API Port 26.1).
  The same content, ids, configs and handbook as on 1.21.1. The Fabric jar also runs on **26.1 and 26.1.1**; NeoForge
  needs 26.1.2 (NeoForge for 26.1 and 26.1.1 is beta only and lacks the 26.1.2 API).
- **Handbook on Fabric 26.1.2** as a **Modonomicon** book (as on 1.21.11), English and German; NeoForge keeps GuideME.
- The One Probe works on Fabric again (it has a Fabric 26.1.2 version).
- **Better Fish Tanks** on 26.1.2, NeoForge and Fabric (its fish bag in both directions, bettas and our bags into its
  tanks). Its 26.x versions are published on CurseForge and Modrinth; the port had missed them.

### Fixed
- Mining an aquarium no longer crashes the client on 26.1 (the crack overlay asks block models without a block).
- The tooltip lines of our blocks no longer push out those of other mods that add a block item tooltip too (sathlib in
  ATM11): they come from an inject instead of an added method.
- A tankmate fish heading for a spot on the glass right above the floor arrives there (it kept pushing into the floor
  and never stood upright).
- The sky and fog inside the tank interior are the overworld's again (they were dark when looking down from above).
- Fabric: mud gives mosquito larvae and tubifex only in swamps, as on NeoForge (the biome condition of the loot was left
  out, so mud dropped them in every biome).
- NeoForge: another mod's item, fluid or energy handler that we hand back to a third mod is passed on as it is, not
  wrapped twice (nested transactions).
- Fabric: a filled fish bag or betta bucket releases its animal into a Better Fish Tanks tank with a right-click (the
  click was only hooked up on NeoForge).
- Slow fish no longer drift upwards: the lift that keeps a tankmate hovering was also added when there was no sinking to
  undo (the game zeroes a very slow motion first), so a slowly swimming small fish rose by up to 0.005 blocks a tick; fish bobbed
  near their targets, and an otocinclus pressed to the surface never reached its spot on the glass just below it.
- Better Fish Tanks: its fish bag no longer catches a fish out of a Bettacraft tank through the glass (the fish left without
  its water, stayed listed in the tank and kept its tank reference in the bag); a hint points to the tank menu, whose bag
  button takes their bag.
- The young of foreign animals (the tortoises, box turtles and alligators of other mods, sea turtles) hatch inside the
  tank: a nest by the glass put a hatchling just past the wall now and then, outside the tank, and the next check of the
  inhabitants let it go into the world.
- An animal reaching a little over the top of its tank stays in it: a dwarf frog male riding his female at the surface
  was let go as if the tank were gone now and then, which ended the amplexus before she laid her spawn.
- Test only: background test clients no longer take the focus or the mouse on 26.x (the window is created elsewhere now).
- Test only: deterministic GameTests (sky darkness with the test time, basin test in the morning, frog spawn, far chunks);
  the multiplayer test clears the mod groups of this version.
- Test only: the clownfish larva tests seed the level's random for their larva days (the fry hunting of the adults took
  too many young now and then), the shrimp generations run at noon (day and night came from the shared test clock), the monitor test allows
  for the tanks of neighbouring tests (at most four tiles on its screen).
- Test only: the shrimp generations come out the same in every run (the life of the small animals draws from its own
  random, the number of draws of their movement no longer shifts it); the M46 world config test no longer saves the
  server config (the config watcher loaded the file again a moment later, over the values of the next tests:
  `frogWithoutAirDrowns` got the default air in the middle of its run); the glass cleaner test waits long enough for the
  far spots.
- Test only: the frog breeding test draws the eggs the frogs snap at from a fixed seed (its 60 % hatch check failed about
  once in 135 runs) and names the state of the pair when the amplexus check fails; a new test hatches a clutch of 20 at a
  nest by the glass.
- Test only: the floating plant tank of the test of keeping fish without a generator holds two female bettas (a pair
  bred there, and its fry, 2 to 23 bettas by chance, set the water quality; it fell below the check once).

### Changed (Minecraft 26.1 itself)
- **Trades are data**: the offers of the wandering trader and of the aquarist villager are `villager_trade` entries
  (`data/bettacraft/villager_trade/...`) in the trade tags `minecraft:wandering_trader/common` / `uncommon` and
  `bettacraft:aquarist/level_1` ... `level_5` (trade sets of the same names). The offers themselves are made as before (item
  modifier `bettacraft:trade`); data packs can now leave single offers out or add others.
- The global loot modifiers have priorities (1050 ... 1010): NeoForge 26.1 loads all of them without a list, the
  priorities keep their order.
- Blocks get their render layer from their textures (26.1 has no `render_type` any more); the look is unchanged.
- Recipes and loot keep their results as item templates (26.1); data packs for our recipe types use the same JSON.
- **Quest chapter as JSON5:** FTB Quests 26.1 reads chapters as `.json5` only; the chapter of the modpack files is
  `config/ftbquests/quests/chapters/bettacraft.json5` (same quests and ids). The KubeJS quest script and
  `BettacraftApi.sendQuestEdit` follow FTB Quests 26.1 (JSON5 quest data, FTB Library's network).
- The EnderIO 9 alloy recipe and soul vials (`EntityType` / `EntityTag`) of 26.1.

### Known gaps on 26.1.2
- Create (with Sable and Aeronautics), Mekanism (and Mekanism Covers), Flux Networks, Industrial Foregoing, Farmer's
  Delight, Fintastic, Naturalist, Critters and Companions, Alex's Caves, Cataclysm, Deeper and Darker,
  Fish Eggs, Oh The Biomes We've Gone and EMI have no 26.1.2 version: their integrations stay out of this version.
  Applied Energistics 2, KubeJS, Aquaculture and Cable Facades exist for NeoForge only.

## [1.42.0-beta.1] - 2026-11-19 (Minecraft 1.21.11)

The same version for **Minecraft 1.21.11**, on NeoForge and Fabric: `bettacraft-neoforge-1.21.11-<version>.jar` and
`bettacraft-fabric-1.21.11-<version>.jar`. Worlds do not move between Minecraft versions; within 1.21.11 they load as usual.

### Added
- **Minecraft 1.21.11**: NeoForge 21.11 and Fabric (Fabric Loader 0.18+, Fabric API 0.141+, Forge Config API Port 21.11).
  The same content, ids, data and configs as on 1.21.1.
- **Handbook on Fabric 1.21.11** as a **Modonomicon** book (optional dependency; Patchouli has no 1.21.11 version), made
  from the same pages as the GuideME handbook, English and German. NeoForge keeps GuideME.

### Changed (Minecraft 1.21.11 itself)
- Spawn eggs have their own procedural textures (since 1.21.5 spawn eggs are no longer tinted).
- Recipes use the ingredient format of 1.21.2+ (affects data packs of others for our recipe types only).
- Flat worlds have their generator's sea level (−63) since 1.21.2: wild bettas and the other wild animals spawn by it.
- An artemia culture in the inventory ripens on the server only (1.21.5 gives no slot any more), at the same rate.

### Known gaps on 1.21.11
- Create (with Sable and Aeronautics), Applied Energistics 2, Refined Storage, KubeJS, the power mods of ATM10, Cable
  Facades, Better Fish Tanks, Fintastic, Naturalist, Critters and Companions and EMI have no 1.21.11 version: their
  integrations stay out of this version. The One Probe exists for NeoForge only.
- Modonomicon handbook: the tank shapes and the animals of the GuideME scenes are shown as items (Modonomicon's
  multiblock and entity pages are drawn away from their frame in its 1.21.11 version); after changing the language,
  join the world again for the handbook's new language.

## [1.42.0-beta.1] - 2026-11-17

Bettacraft runs on Fabric too: the first multiloader version, one code base for NeoForge and Fabric 1.21.1.

### Added
- **Fabric 1.21.1** (Fabric Loader 0.16.1+, Fabric API 0.116+, Forge Config API Port): the whole mod, with the same ids, data,
  configs (the same TOML files and keys; Mod Menu shows the config screen) and worlds as on NeoForge. Items, fluids and
  energy go through Fabric's transfer API and Team Reborn Energy (pipes, tanks and cables of Fabric tech mods).
- **Handbook on Fabric** as a **Patchouli** book (optional dependency), made from the same pages as the GuideME
  handbook (English and German, images, recipes, the tank shapes as multiblocks).
- **Integrations on Fabric:** Jade, WTHIT and The One Probe (the tank, betta, filter and cabinet lines), JEI, REI and
  EMI (information pages, crucible and food maker recipes), FTB Quests (the quest chapter), Refined Storage (items and
  fluids through the transfer API), JourneyMap (alarm waypoints), Better Fish Tanks, Naturalist, Fish of Thieves and
  Critters and Companions (their animals in our tanks).
- Two files per release on CurseForge: `bettacraft-neoforge-1.21.1-<version>.jar` and
  `bettacraft-fabric-1.21.1-<version>.jar`.

### Changed
- The code is split into `common/`, `neoforge/` and `fabric/`; the NeoForge jar keeps its content and behaviour (the
  same data and assets, old worlds load).
- The NeoForge jar is now called `bettacraft-neoforge-1.21.1-<version>.jar`.

### Known gaps on Fabric
- A glowing pipe cover (glowstone, sea lantern ...) does not light up its surroundings (Fabric has no light by block
  entity).
- Create, AE2, KubeJS, GuideME, Aquaculture, Fintastic and Cable Facades have no Fabric 1.21.1 version: their
  integrations stay on NeoForge. The public events for scripts (`api/event`) are NeoForge events.

## [1.41.0-beta.1] - 2026-11-17

A hired aquarist comes to every player with a few tanks and a first breeding, and works for him: he walks to the tanks,
feeds, changes water, cleans, trims and moves the young, for an emerald a day.

### Added
- **Hired aquarist** (switch `features.aquaristNpc.enabled`, on by default; server config `hiredAquarist.*`):
  - **Arrival:** a player who owns at least 3 tanks and bred something (any breeding advancement) gets a visit by day: an
    aquarist walks up and offers his work in the chat with the buttons **[Hire]** and **[No, thanks]** (also in his window).
    Refused or unanswered for a day, he leaves; the next one comes after 5 to 10 in-game days and another breeding.
    `/bettacraft aquarist status` tells why none has come yet; operators: `/bettacraft aquarist arrive [player]`.
  - **A person of his own:** a human model with procedural work clothes (teal rubber apron with a betta, gloves, boots,
    cap), four looks, a first name (a name tag of his employer renames him). He cannot be hurt (only `/kill`), runs away
    from monsters, cannot be pushed, leashed, ridden or caught; only his employer or an operator uses his menu.
  - **Home and wage:** he takes a free bed and a free breeding basin within 32 blocks (their POI tickets, so villagers
    leave them alone), sleeps at night and takes his wage every morning from the chests next to the basin: 1 emerald, or 3
    pieces of food. Without wage he works at half speed and leaves after 3 days (also without a bed or basin).
  - **Six tasks**, each switched on or off in his menu with its own level 1 to 3: feeding from the chests (1 to 3 times a
    day, live food first; not tanks with a feeder), water changes with water from a reservoir, pipe or water source next to
    the basin (15 to 35 %, salt water for salt tanks), scraping algae, thinning floating plants and trimming grown aquarium
    plants (the plants and cuttings go into the chest), moving young bettas, tankmates and froglets into a rearing tank
    chosen in his menu (2 to 8 per trip), and warnings in the chat (sick animals, too little water, temperature, water,
    hunger, crowding, the breeding warnings) with a JourneyMap waypoint at the tank. He really walks there: every round he
    fetches what he needs at the basin, goes from tank to tank and brings the rest back. Up to 16 loaded tanks within 32
    blocks of his basin.
  - **Multiplayer:** each player has his own aquarist. He works only at his employer's tanks, with FTB Teams at those of
    the party's members (`hiredAquarist.ftbTeams`, through reflection, no dependency) and at tanks their owners released
    for every aquarist; never at locked tanks, tanks set to "no aquarist" or tanks of strangers, and he checks it again
    right before every job. The new **A** button of the tank menu (owner or operator) sets who may: the owner's aquarist,
    every player's (A+) or none (A-). A tank without an owner goes to the next player who opens it.
  - Nothing is lost or doubled: what he carries (food, cuttings, water, young) goes back when he leaves, is dismissed or
    is removed; young the rearing tank does not take go back into their tank (or as a filled fish bag into a chest). His
    chunk unloading keeps everything; tanks in unloaded chunks are left alone.
  - His menu: status, home, wage, the tasks with their levels, the rearing tank, the tanks around and whose they are; Jade,
    The One Probe and WTHIT show whom he works for and what he does.
  - Advancement and quest **An Aquarist Comes By** (chapter: 112 quests); handbook page "Hired aquarist" (en, de).

### Changed
- The tank menu's name box is a little narrower (room for the aquarist button); network version 19.

## [1.40.0-beta.1] - 2026-11-17

Tanks keep living on the road: on Create Aeronautics airships everything runs as on the ground, on Create contraptions the
tank data is cared for, powered by the contraption; the water sloshes in curves.

### Added
- **Care on the road** (switch `features.compat.create.travelCare`, on by default):
  - **Airships (Sable):** a tank on a Create Aeronautics airship lives on. The bettas and the other animals of Bettacraft
    stay real and swim in it on the ship (Sable keeps them there, our data puts them into `sable:retain_in_sub_level`), the
    care, breeding and every device on the ship work as on the ground, with power from the ship's cables, batteries and
    generators (a drive adapter on a shaft). The temperature comes from the air where the ship flies. Animals of Minecraft
    and other mods still travel packed. Put down, they are back in the tank exactly once.
  - **Create contraptions** (pistons, bearings, carts, trains): the tank's data is cared for every second of the trip:
    hunger and health of the packed bettas, tankmates and animals of other mods, the temperature (the air of the biome the
    contraption is in), the water quality, the filters, heaters and coolers that travel along (interior devices, an
    external filter or a cabinet glued to the tank). Breeding pauses there, and nobody dies on the road (an animal at the
    brink waits for the arrival). The tank's log tells about the trip when it arrives.
  - **Power on the road:** where the devices need FE they take it from the tank's buffer, from drive adapters at the tank
    that turn with the trip (`createTravel.dynamoFullSpeed`) and from FE batteries of other mods glued next to the tank
    (`createTravel.batteryFePerTick`); without power they stop and the values slowly get worse.
  - The animals drawn on a contraption show how they are (a hungry or sick animal swims slower and lower), the tank's sign
    shows its values on the road (a new packet every two seconds; network version 18).
- **Sloshing water:** the water of a tank on a contraption or an airship tilts when it speeds up, brakes or takes a curve,
  swings back after a stop and carries waves; it stays level when a bearing tilts the tank. Client option
  `render.travelSloshing` (0 = off).
- Advancement and optional Create quest **Fish Transport with Power** (the chapter has 111 quests).

### Changed
- Glued external filters and cabinets travel with their tank on a contraption (and run there).
- `runMultiplayerTest -Pcreate` puts Create on the server and both clients and turns a tank on a bearing.

### Fixed
- Bettas, tankmates and dwarf frogs that another mod moved together with their tank (Sable putting an airship down) no
  longer chase the water of the place they came from.

## [1.39.0-beta.1] - 2026-11-17

A sump under the tank with a refugium and a calcium reactor; brackish water with the archerfish, the mudskipper, mangroves
and the Amano shrimp.

### Added
- **Sump:** a small tank in the cabinet under an aquarium (a new slot in the cabinet). Its water counts with the tank (0.5
  blocks each, `sump.sumpWaterBlocks`): the waste, the nitrate and the dosed values change more slowly, and there is room for
  more animals. Its return pump runs like an external filter; its chambers (the button above the slot) hold two heaters or
  coolers, a protein skimmer and two filter media, so they take no room in or at the tank. A level sensor, a dosing pump or a
  calcium reactor next to the cabinet serves the tank above. The tank menu gets the tab "Sump". Switch `features.sump.enabled`.
- **Refugium:** a chamber of the sump with Chaetomorpha (up to 16) and a tank light: it takes up nitrate
  (`sump.refugiumNitratePerAlgae`), starves the algae on the glass of nutrients and grows (`sump.chaetoDoublingDays`); the
  extra algae are food for shrimp and snails, a vegetable for the food maker or go into the composter. The wandering trader
  sells Chaetomorpha. Switch `features.sump.refugium`.
- **Calcium reactor:** a column of coral rubble that CO2 from a gas cylinder dissolves: it raises calcium and alkalinity in
  the ratio the stony corals use them, up to about 440 mg/L calcium (`sump.calciumReactorCalciumPerDay`,
  `sump.calciumReactorRubblePerDay`). Next to a salt water tank, the cabinet of its sump or on a pipe; hoppers fill in the
  rubble (from dead coral blocks or calcite). Switch `features.sump.calciumReactor`.
- **Brackish water** as a theme of its own (a new chapter of the handbook), switch `features.brackish.enabled`:
  - **Archerfish:** shoots insects above the water with a jet of water: fruit flies let out over an open tank, insects at
    the plants above it (a mangrove crown, the land of a paludarium, leaves or flowers on top); now and then it splashes a
    player leaning over the tank. Net in mangrove swamps, wild there, wandering trader.
  - **Mudskipper:** a fish on land for a brackish paludarium (it is stressed in fresh water); it skips over the mud and climbs
    roots and the glass. Terrarium box in mangrove swamps, wild there, wandering trader.
  - **Mangroves:** a mangrove propagule on a tank (or placed in the interior) grows in six stages into a small mangrove of
    vanilla blocks: stilt roots in the water, a crown over the surface (`brackish.mangroveStageDays`, faster in brackish
    water). New decoration: the tall **mangrove stilt roots**.
  - **Amano shrimp:** strong algae eaters that mate in fresh water; their larvae die there after a day
    (`brackish.amanoFreshLarvaDays`): net the cloud of larvae into a fish bag and let it out into a brackish tank, where a
    part settles as young shrimp after three days (`brackish.amanoLarvaDays`, `brackish.amanoLarvaSurvival`, twice as many
    with phytoplankton). Net in mangrove swamps, wandering trader.
- Seven advancements ("Below Deck", "Dissolved Coral", "Sharpshooter", "Mistaken for a Fly", "Fish Out of Water", "Roots in
  the Brine", "Back from the Sea") and seven new quests.
- Handbook (English and German): the chapter "Brackish water" with pages for the archerfish, the mudskipper, the Amano shrimp
  and the mangroves; pages for the sump, the refugium and the calcium reactor; generated sections.

### Changed
- The mangrove swamps are a home of their own for the terrarium animals (the swamp animals live there as before).
- Mangrove leaves and propagules in a tank interior count as plants of Minecraft (`minecraft_mangrove` profile).

### Compatibility
- Cabinets of older worlds get the new sump slot empty; tanks without a sump work as before.

## [1.38.0-beta.1] - 2026-11-17

Gold and albino neons, a hardy bronze cory and the panda garra; on the reef the mandarin, the Banggai cardinalfish, the
anthias and the fire shrimp, and clownfish larvae that need rotifers.

### Added
- **Bred forms of tetras and corydoras**, recessive on the colour locus like the ram forms: neon tetra Diamond, Gold and
  Albino, cardinal tetra Gold, glowlight tetra Albino, panda cory Albino and the rare Longfin (long, flowing tail and
  dorsal fin), sterbai cory Albino. Wild fish and the wild-type fish of the wandering trader carry them hidden; the trader
  and the aquarist sell them now and then (the longfin a quarter as often). Switch `features.tankmates.tetraCoryForms`.
- **Bronze cory** (*Corydoras aeneus*): a hardy cory with metallic green-bronze flanks, in the forms Albino and Gold Laser;
  eggs on the glass after a cool water change. **Panda garra** (*Garra flavatra*): a small, yellow-and-black banded algae
  grazer for cool, clear water that scatters its eggs. Net in rivers and jungle water, wild, wandering trader, aquarist.
  Switch `features.tankmates.moreBottomFish`.
- **Mandarinfish:** a blue-and-orange dragonet that takes no fed food at all: it lives on the **copepods** of the live
  rock. Live rock in salt water holds copepods (`reef.copepodsPerLiveRock`) that grow back by themselves; other small reef
  fish pick some too, so it wants plenty of rock and a quiet tank, otherwise it starves. The menu shows the copepods.
- **Banggai cardinalfish:** the easy reef fish to breed: the female passes her eggs to the male, who carries them in his
  mouth; the young come out fully formed (juveniles, no fry food).
- **Lyretail anthias:** a shoaling fairy basslet (orange females, a magenta male) that is hungry again after a third of a
  day; **fire shrimp:** a deep red cleaner shrimp with white dots that cleans the fish like the cleaner shrimp. Net in warm
  oceans, wild, wandering trader. Switch `features.saltwater.moreReefAnimals`.
- **Clownfish larvae and their food chain:** after hatching the larvae drift for three days (`reef.clownLarvaDays`) and
  need **rotifers** in the water (in the second half also artemia nauplii); fed, most survive, without food only 5 %
  (`reef.clownLarvaSurvival`). **Phytoplankton** (a culture of salt water, seagrass or kelp and bone meal that ripens in
  light) keeps the rotifers in the tank alive; **rotifer cultures** start from phytoplankton and infusoria and go on with
  phytoplankton (one bottle of rotifers and two of phytoplankton make two cultures); the aquarist sells rotifer cultures.
  Rotifers are also the finest fry food. Switch `features.saltwater.larvaRearing`.
- Advancements "Gold, Albino, Diamond", "Living Rock", "Rotifer Rearing" and "A Mouthful of Young", five new quests.
- Handbook (English and German): pages for the bronze cory, the panda garra, the mandarinfish, the Banggai cardinalfish,
  the lyretail anthias, the fire shrimp and "Phytoplankton and rotifers"; the varieties of the neon, cardinal, glowlight,
  panda and sterbai cory; clownfish larvae on the reef pages; copepods on the live rock page; generated sections.

### Changed
- Wild-caught and trader fish pick a hidden bred form by weight (a rare form a quarter as often).
- The neon, cardinal, glowlight, panda and sterbai textures are named by their form (the wild form looks as before).

### Compatibility
- Fish of older worlds load as the wild form (their genes were "wild"); clownfish clutches of older worlds go on with the
  new larvae rules (without rotifers a few survive, so breeding never stops); tanks without plankton save nothing new.

## [1.37.0-beta.1] - 2026-11-17

An aquarist moves into the village, the ram gets two new forms, and the dwarf clawed frogs breed.

### Added
- **Aquarist villager:** a new villager profession with the **breeding basin** (a small glass basin with gravel, water and
  plants on a wooden stand; glass panes, a water plant and wooden slabs) as workplace: an unemployed villager next to it
  becomes an aquarist, in every village. Two offers on each of the five levels: food and floating plants, aquarium
  plants, shrimp and snails, test kits, artemia cysts and active soil, live food cultures and rare plants, and as a master
  pairs of rare bred tankmate varieties. He **buys bettas** (bucket or fish bag) and **bred tankmates** (fish bag) and pays
  by their type and the rarity of their visible genes (bred, generation, named variety, rare fin forms and patterns, long
  fins, wild species; for tankmates the bred colour and the species). The price table is a data file
  (`data/<ns>/bettacraft/aquarist_prices.json`) that datapacks can change. Switch `features.sources.aquaristVillager`.
- **Black and German Blue ram:** two new recessive forms of the ram cichlid on its colour locus (dominance Gold, Electric
  Blue, Black, German Blue, Balloon): Black with a velvet-black body and blue spangles, German Blue stronger blue than the
  wild form with a red belly and a red eye. Wild rams and the wild-type rams (and other wild-type fish) of the wandering
  trader can carry them hidden.
- **Dwarf clawed frogs breed:** frogs now have a sex; a cool water change into a tank at 24-28 °C sets a grown, fed pair
  off (amplexus: the male holds on to the female, who carries him to the surface), the 20-50 eggs float at the surface for
  two days (bettas and other fish eat them, floating plants hide them), the tadpoles live as data animals in the open water
  and need fine food (infusoria, artemia nauplii, fry food), with bettas in the tank hardly one survives, and after six days
  they turn into froglets that grow up in four. Switch `features.frogs.breeding`, server config `frogs.frogBreedingSpeed`,
  `frogSpawnHatchDays`, `tadpoleDays`, `frogletDays`, `maxTadpoles`.
- Advancements "Tadpoles!" and "An Aquarist in the Village", and their quests in the quest chapter; the cichlid quest names
  the ram forms.
- Handbook (English and German): the aquarist villager page, frog breeding, the ram forms, the aquarist in every
  Obtaining section and on "Where to find what".

### Compatibility
- Frogs of older worlds get their sex from their UUID once and keep it; old rams keep their genes; tanks without spawn or
  tadpoles save nothing new.

## [1.36.1-beta.1] - 2026-11-17

Small fixes: the heat mat icon, nicer wild carpets, the leopard gecko without holes, fewer artemia cysts from sand, and
shrimp and snails that one actually meets in the wild.

### Fixed
- **Leopard gecko (and the other terrarium animals) no longer grey or see-through:** the textures of the frogs, geckos, the
  corn snake and the turtles (and of the shrimp, snails, crayfish and hermit crabs) were laid out with the cube sizes
  rounded up, while Minecraft maps the exact sizes, so the faces sampled empty texels at their edges and one looked into
  the model. The textures now cover every face exactly; the patterns also run on across the edges. A test checks every
  face of every model.
- **Heat mat item lies flat** like the mat under a terrarium (inventory, hand, item frame, on the ground, JEI); placed on a
  wall it still stands upright.
- **Shrimp, snails and crayfish are seen in the wild again:** they no longer share the own spawn pass's limit with the
  fish. They have their own limit (`wildlife.spawnPassCrittersPerPlayer`, 12 within 48 blocks) and their own try per
  pass, which prefers shallow water (1 to 6 blocks deep) over sand, gravel and stone or by water plants, in small groups,
  so they can be seen from the bank.

### Changed
- **Wild carpets** (Hemianthus, Monte Carlo, Marsilea, Staurogyne, hairgrass in the world) are dense cushions of several
  heights with leafy tops, small tufts, a little colour variety and a soft, ragged rim instead of a flat plate.
- **Artemia cysts from sand: 4 % instead of 8 %** (`drops.artemiaFromSandChance`). A world whose server config still has
  the old default 0.08 is moved to 0.04 once (logged, old file kept as `bettacraft-server.toml.pre-m46`), the global
  `config/bettacraft-server.toml` before loading and a world's own `serverconfig/bettacraft-server.toml` when the server has
  started (the world remembers it, so 0.08 set again later stays); a chance you set yourself stays.
- When small animals spawn the **apple snail is picked a quarter as often** as the net catches it, so the dwarf crayfish,
  Sulawesi shrimp and Tylomelania of jungle water show up too (jungle spawns: about 40 % apple snails instead of 75 %).
- `wildlife.spawnPassPerPlayer` (10) now counts the wild fish, frogs and turtles only; `/bettacraft wildlife` shows both
  limits.

## [1.36.0-beta.1] - 2026-11-16

The quest chapter for modpacks catches up with everything since 1.25.0, and the advancements with it.

### Added
- **Quest chapter "Bettacraft" (FTB Quests, `bettacraft-modpack-<version>.zip`): 63 new quests, 96 in all**, in branches
  from the first tank: tankmates with the livebearer litters and the guppy strains, community fish, cichlids, puffers,
  barbs and loaches, gouramis; shrimp, snails and crayfish; frogs; reef fish and seahorses; the paludarium, turtles and
  their clutches, sea turtles, Minecraft pets, gecko clutches; big animals; axolotls and animals of other mods, mobs in
  glass; the technology (cabinet, cooler, aeration, sponge filters, LED lights and the daylight timer, the power regulator,
  pipes, stainless steel, bulkheads, the water change controller, the top-off, the multiblock filter with filter lines,
  ports and pipe covers); live food cultures and the pump, food variety, fertilizer and its dosing, algae, decoration; the
  mirror stick, the magnifier, the fish doctor. Optional quests for energy, the crank and Create (drive adapter, a
  travelling tank) never block the chapter.
- **Item rewards** besides the experience: plant cuttings, food, a spare test kit, filter media, salt, glass pipes, a few
  more (the old quests got some too).
- **22 new advancements** in the Bettacraft tab (also the tasks of the new quests): Community Tank, Born Alive, Proud
  Father, Colony, Clear View, Hatchlings, Warm or Cool, Half and Half, Ocean Giant, House Pet, Guest from Afar, Ship in a
  Bottle, Big Fish, One Filter for All, Hands Free, Balanced Diet, Turn It Down, Sunrise, Sunset, Plugged In, Wound Up,
  Shaft Power, Mobile Aquarium.
- **Quests of switched-off features hide themselves:** every quest carries FTB Quests tags for its feature switches; the
  new KubeJS script `kubejs/server_scripts/bettacraft_quests.js` makes the quests of switches that are off (and of Create
  without Create, of energy without a power option) invisible and optional, and shows them again when the switch is
  turned on. For scripts: `BettacraftApi.featureEnabled(path)`, `BettacraftApi.questTagsOn(tags)` and the event
  `FeaturesChangedEvent` (the configs were reloaded).
- The README lists which quests belong to which feature switch.

### Changed
- The 33 quests of the first chapter keep their ids, tasks, dependencies and rewards: players keep their progress when a
  pack updates the file; advancements they already have complete the new quests by themselves. The chapter got a new
  layout.

## [1.35.1-beta.1] - 2026-11-16

### Fixed
- **Cabinet fronts switch at once:** turning `features.equipment.cabinetFronts` (or a preset) on or off in the config file
  or the config screen now changes every loaded cabinet right away; before, a cabinet only showed it after its chunk was
  loaded again or its row changed.

## [1.35.0-beta.1] - 2026-11-16

Mobs and pets walk with real steps in their tanks, and long cabinet rows get one calm front.

### Added
- **Cabinet fronts:** cabinets side by side now show one front: a drawer block at both ends (from three blocks on), double
  doors over two blocks between them, a single door with its handle at the side in the middle of an odd row; two cabinets
  are one double door, a single cabinet keeps its two doors. The fronts follow the row automatically (up to 32 blocks)
  when cabinets are placed, broken or turned; rows of different directions meeting at a corner keep their own fronts.
- The button **"Front"** in the cabinet menu fixes the front of one cabinet: Auto (the default), double door left or right
  leaf, single door, drawers or the old two doors; the small button next to it switches the **joints**. Broken, the cabinet
  keeps both on its item.
- Thin **dark joints** between the doors and drawers (also of the old two-door front) make the fronts readable on planks.
- Feature switch `features.equipment.cabinetFronts` (off: every cabinet shows its two doors like before, the button is
  hidden, the settings are kept).
- Client option `walkAnimationBoost` (render): `auto` (the default), a factor from 0.1 to 10 for all, or 0 for vanilla.

### Fixed
- **Gliding mobs in tanks:** vanilla moves the legs by the distance a mob walks in a tick, without its size, so the shrunk
  mobs in glass and the slow terrarium pets barely moved their legs and glided. In a tank the distance is now divided by
  the mob's scale and multiplied by a factor (auto: 1.5 for the shrunk mobs, 3 for pets at their real size), so they step
  like the same mobs outside; spiders and geckos on the glass move their legs while they climb.
- Our own terrarium animals (geckos, tortoises, frogs, snakes) had no walk animation at all in a tank; they move their legs
  again.
- Animals moved by the terrarium walker were never on the ground (the chicken flapped its wings all the time); they are
  now while they stand, not while they hop, climb, swim or fly.

## [1.34.0-beta.1] - 2026-11-16

A new minor release that bundles the work since 1.33.0: the big test round with every integrated mod and its fixes
(1.33.1), the new release material (1.33.2) and the angelfish fin fix (1.33.3). See those sections for the details;
there are no further changes.

## [1.33.3-beta.1] - 2026-11-17

A fix for tall fish at the water surface.

### Fixed
- **Angelfish fins out of the water:** tall tankmates kept only most of their hitbox below the surface, but the angelfish
  is drawn almost twice as high as its hitbox, so near the surface its dorsal fin stuck out of the water and through the
  glass lid (the discus a little too). Every tankmate now keeps the top of its drawn model (taken from the model itself,
  for every body plan, sex, life stage, hybrid share and the balloon ram) below the surface, also while it gulps air, rests
  or swims up to its zone; a fish taller than the water left in its top block now sinks into the block below instead of
  sticking at the block's floor. The angelfish pair is back in the Amazon tank of the media.

## [1.33.2-beta.1] - 2026-11-17

Release material: new pictures and animations for the README and the CurseForge page, and the mod page in English only.
No change to the game.

### Changed
- **README and mod page media:** new pictures and animations, all taken in the game in the showcase world at a fixed time
  of day and weather, each still also with Complementary Reimagined: a betta flaring, a plant growth time lapse, an Amazon
  tank with discus, a shrimp tank with every Neocaridina colour, terraria and a paludarium, Minecraft pets, mobs in glass,
  the multiblock filter for several tanks and an overview of the showcase; the aquarium room, the planted and reef tanks,
  the varieties, the interior and the menus taken again (bettas at their normal size since 1.28).
- The CurseForge description (`docs/description.md`) is English only and follows the feature sections of the README
  (bettas, tanks and interior, plants, small animals, tankmates, salt water, terraria, technology, compatibility, FAQ,
  acknowledgements).
- The CurseForge upload of the release workflow names Fintastic, Aquaculture 2 and Naturalist as optional dependencies too
  (every slug checked on CurseForge).
- Developers: `gradlew runClient -Psmoketestm43` makes the media in a fresh copy of the showcase world (after `gradlew
  buildShowcase`; add `-Pshaders` for the shader stills), then `py tools/assetgen/readme_media.py`.

## [1.33.1-beta.1] - 2026-11-17

A big test round with every integrated mod, the animals of mods without a profile and complete breeding runs over several
generations; this release fixes what it found.

### Added
- **Filter media by automation:** hoppers, pipes and the import/export buses of Applied Energistics 2 and Refined Storage put
  a fresh filter medium into an empty chamber of the multiblock filter and take a used-up one (clogged or spent) out; a
  medium that still works stays in. Switch `features.equipment.filterMediaAutomation` (on; off: media only by hand). The
  handbook's filter media page says how.
- The tank menu shows the sex (♀/♂) of the turtles, geckos and dart frogs, so a breeder can pick a pair (the nest
  temperature decides it).

### Fixed
- **Fish Eggs** gave every fish of the world its own breeding and food hunting: our bettas and tankmates (and foreign fish
  in a tank whose own breeding is suppressed) laid eggs of that mod, whose young hatched without genes and stuck in the tank
  glass. Those goals are now taken from them.
- After a crash (the list of interior regions older than the interior itself) a new tank could take over the interior of an
  old one with its plants and blocks; a new tank now skips every region that already holds an interior.
- A copied packed tank item (creative middle click, another mod's duplication) and the creative copy of a standing tank
  (pick block with its data) became a second tank with the same id, interior, water and small animals; only the first
  placement now brings the tank back, every copy is a plain tank block.
- An axolotl taken out of a tank with a bucket lost its generation and the magnifier's analysis (only the colour genes went
  into the bucket); both now travel with it.

### Changed
- Developers: the optional test suites `gradlew integrationTests` (every integrated mod) and `gradlew zooTests` (mods with
  animals and plants that Bettacraft does not integrate), breeding runs over generations in the GameTests, `-Pgametests=` for
  subsets, a lag simulation for the multiplayer test (`-Pmplag`); every GameTest run starts with a fresh world.

## [1.33.0-beta.1] - 2026-11-17

With Create a whole aquarium runs on one shaft: a drive adapter on the tank, or a shaft on the cabinet under it, turns
Create's rotation into the tank's power, and it costs real stress in Create's network.

### Added
- **Drive adapter** (only with Create; recipe: a shaft, an andesite casing, two copper ingots and redstone): set on a tank
  wall or under a tank, a Create shaft, cogwheel or motor at it charges the tank's FE buffer, so every device that takes
  the tank's FE runs on rotation (filters, flow pumps and skimmers, heaters, coolers and heat mats, lights, air pumps,
  misters), without FE cables. **64 RPM** give the full 256 FE/t, slower proportionally; several adapters on one tank add
  up to 1024 FE/t.
- **Real stress:** the adapter is a kinetic block of Create's network; it costs **2 SU per FE/t** it gives (at least
  0.5 SU per RPM when idle), so a small tank costs little and a filling buffer or many heaters cost more. An overstressed
  network gives the tank nothing. Create's goggles show its stress and what it gives the tank.
- **Shaft on the cabinet:** the cabinet menu has a button (Create's shaft, left of the filter slot) for the side that takes
  a shaft: none, back, left, right, front or bottom. A shaft, cogwheel or gearbox there charges the tank above like an
  adapter (its stress is carried by that shaft's network), a brass flange marks the side, the cabinet keeps it on its
  item. With winding on, what the tank does not need **winds the cabinet's spring**, so the tank keeps running for a
  while after the rotation stops; the crank still winds it too.
- Jade / The One Probe / WTHIT: the cabinet's shaft (side, FE/t, overstressed) and on the aquarium what Create gives.
- Feature switches `features.compat.create.drive` and `features.compat.create.cabinetDrive`; server config section
  `createDrive` (full speed, FE per input, the limit per tank, stress per FE and when idle, FE per spring tick, the
  cabinet sides a server allows).
- Handbook (English and German): the drive adapter page; the cabinet, power and Create pages name it. Without Create the
  page, the item, the recipe and the cabinet button are not there.

### Changed
- Cabinets store their shaft side (data version 3; cabinets of older worlds take no shaft).

## [1.32.0-beta.1] - 2026-11-17

One multiblock filter serves several aquariums: its own filter pipes with T-pieces dock on any side of a tank, up to four
lines run in one pipe bundle, fresh and waste water ports do the water changes of all its tanks, guards keep the animals
out of the pipes, and covers hide the pipes behind any block.

> **Breaking for existing worlds:** every multiblock filter now needs an **intake guard and an outlet guard** in each of its
> aquariums, **also filters built before this update**. Until they are fitted an old filter keeps its media, regulator and
> power but **does not filter** (a red note in its screen and in the aquarium menu, and a chat message with its position to
> players nearby). Fit the guards where its pipes enter the aquarium (use them on the glass there). Servers that want the
> old behaviour switch the filter lines off: `features.pipes.filterLines = false` (a filter then serves one aquarium as
> before, without guards).

### Added
- **Filter lines:** the filter inlet pipe (blue, intake) and filter outlet pipe (orange, return). A pipe joins at most two
  neighbours; the **pipe T-piece** makes it a branch to every side, so one filter serves several aquariums. Its cleaning and
  flow are shared by the water volume of the aquariums; the filter screen lists every aquarium with its share or the reason
  it is not filtered.
- **Docking anywhere:** filter pipes dock on any side, the top or the bottom of an aquarium; a pipe dock appears inside at
  that spot and interior pipes lead on from it. A pipe that leads into an aquarium and out again is refused.
- **Guards:** the intake guard (fine mesh) and outlet guard (mesh cap) in the aquarium; the intake strainer, lily pipe,
  nozzle and spray bar count as guards. Without its guard a filter does not filter that aquarium (warnings in the aquarium
  menu, Jade / The One Probe / WTHIT and the filter screen).
- **Pipe bundle:** up to four filter lines in one block like conduits (right-click a filter pipe with the pipe of another
  line; sneak-right-click with an empty hand takes the line you look at out). Each line keeps its own sides and T-piece.
- **Fresh and waste water at the filter:** filter fresh water pipe (green) and waste water pipe (brown), fresh and waste
  water ports on the chambers (8 buckets each). A level sensor or water change controller touching the filter tops off and
  changes the water of all its aquariums (shared by need); the old water goes out of the waste port or is discarded
  without one. Water pumps, reservoirs, the pipes of Pipez, EnderIO and Mekanism, AE2 export buses and Refined Storage
  importers plug into the fresh and waste lines.
- **Pipe cover:** with the pipe cover in the main hand and a full block in the off hand, right-click a filter pipe or
  bundle and it is drawn as that block (a full block to walk on, with the block's light, shade and sounds); its lines and
  connections stay. The block comes back when the cover comes off (sneak-right-click with the pipe cover) or the pipe is
  broken.
- **Facades of other mods** on the filter pipes: Applied Energistics 2 cable facades, EnderIO conduit facades and
  Mekanism Covers put their block on as the cover (the facade is kept and given back). **Cable Facades** puts its own
  facades on the filter pipes and bundles: the block tag `cable_facades:supports_facade` is in our data (its config is not
  touched), and the pipes are hidden under its facades.
- Feature switches `features.pipes.filterLines`, `features.pipes.filterWater` and `features.pipes.pipeCovers`.
- Handbook (English and German): pages for the filter lines and guards, the pipe bundle with fresh and waste water, and
  the pipe cover and facades.

### Changed
- **Old multiblock filters need guards** (see above); the switch `features.pipes.filterLines = false` keeps the old way.
- Glass and stainless steel pipes also join blocks that only offer a fluid handler (the filter's water ports).
- A level sensor or water change controller on a multiblock filter works on all its aquariums instead of the first one.

### Fixed
- An animal put into a nearly full, low tank (a squid in a tank two blocks high) could start with its head in the lid,
  slip out and lose its tank; it now starts with its whole body below the lid.

## [1.31.0-beta.1] - 2026-11-17

The animals of the aquarium and animal mods of ATM10 live in our tanks by their real size: the giant fish and rays in big
tanks, the insects, small mammals and birds of Naturalist, Critters and Companions and Living Things in terraria, and the
peaceful animals of Minecraft as terrarium pets.

### Added
- **Water animals of other mods** (profiles): the fish and invertebrates of Fintastic, Aquaculture 2, Fish of Thieves (ten
  species, their colour variants inherited; trophy fish do not breed), Naturalist (rays, jellyfish, clams, starfish, giant
  isopods), the Critters and Companions dumbo octopus and Living Things (seahorses, crabs). The giants (arapaimas,
  coelacanth, tuna, gar, muskellunge, catfish, halibut, the Living Things manta ray and shark) go into big tanks by the size
  rule with their real length; they do not breed there.
- The Aquaculture turtles (box, arrau and starshell turtle) in terraria and paludaria by our turtle rules: basking spot,
  UV, eggs in the land, and the nest temperature decides the sex.
- Food of other mods: Fintastic copepods, daphnia and fairy shrimp are live food (Fintastic daphnia start the daphnia
  barrel); the fish flakes of Fish Eggs are flake food, its fish and squid eggs frozen food that conditions for breeding.
- **Refusals with a reason:** deep-sea animals (anglerfish, blobfish, the abyssal creatures of Alex's Caves), whales and
  great white sharks, mushroom and cave creatures and hostile ones (Twilight Forest beetles, deep ones) are refused with a
  message that says why; the handbook lists them.
- **Insects, small mammals and birds of other mods** in terraria: Naturalist caterpillar, butterfly, firefly, ant,
  hedgehog, mole, rat, songbird, turkey and vulture; Critters and Companions stag beetle, stick bug, jumping spider,
  ladybug, acorn weevil, roly-poly, snail, ferret, red panda and shima enaga; Living Things snail, koala, monkey, raccoon,
  owl, peacock, penguin (a cold paludarium, it hunts small fish) and flamingo. Fliers need a closed top and fly out of an
  open tank; woodlice and snails are the clean-up crew (they eat the leftovers and live from the waste); fireflies glow at
  night. The mammals and birds breed as a female and a male.
- The komodo dragon and the big mammals (boar, deer, bears, zebra, lions, tiger, ostriches) in big terraria by the size
  rule; the large net and the transport tub catch big land animals, and a full tub releases a land animal on the ground.
- **Minecraft animals as terrarium pets:** chickens, rabbits, bees, frogs, cats, ocelots, foxes, parrots, armadillos, bats
  and wolves at their real size, and pigs, goats, sheep, cows, mooshrooms, horses, donkeys, mules, llamas, pandas and polar
  bears in big terraria by the size rule. Moved by our walker and fed by hand or from the feeder; bees need flowers and a
  beehive in the interior (a mini hive), rabbits and the grazers grass, frogs a paludarium; chicks hatch from eggs in the
  sand, the others have young (the big ones from 1.5 m do not breed in a tank). Caught with the terrarium box, the large
  net and the tub, or lured with their food as bait into an open tank. They drop nothing and give no wool, milk or eggs.
  Camels, sniffers, striders and the undead horses are refused.
- Shiny: the shiny animals of the mod keep their shine in our tanks and fish bags.
- New profile keys: `length`, `refuse`, `breeding.sex` (`random`, `temperature`), `breeding.sterile`, `terrarium.lid`,
  `terrarium.cleanup`, `terrarium.glows`, `terrarium.needs` and `terrarium.grazes`.
- Feature switch `features.foreignAnimals.vanillaPets` (the other new animals use the switches of the animals of other
  mods and the big animals).
- Handbook (English and German): the list of the animals of other mods (generated tables of every profile), land animals
  of other mods (lids, clean-up crew, glow, the big ones), Minecraft animals as pets, and the new profile keys.

### Changed
- **The dragonflies of Naturalist and Critters and Companions need a lid now:** in an open terrarium or paludarium they
  fly away (alive) after the update.
- The vanilla frog is off `#bettacraft:tank_blacklist` while the Minecraft pets are on: it lives in paludaria.
- The giant fish of Fintastic and Aquaculture and the Living Things manta ray are off the blacklist (the size rule decides).
- The Naturalist jellyfish (0.3 m) and giant clam fit by their real size instead of their hitbox.
- Cownose rays keep closer to the leader of their school.

## [1.30.0-beta.1] - 2026-11-17

Plants and corals of Minecraft and other mods work in the aquariums like ours: kelp, seagrass, lily pads, corals and the
water plants of Fintastic, Regions Unexplored, Twilight Forest and Alex's Caves lower the nitrate and count for breeding.

### Added
- **Plants of other mods work:** kelp, seagrass, lily pads, sea pickles, corals and coral blocks of Minecraft, and the
  water plants of Fintastic (hornwort, Amazon sword, anubias, duckweed, lotus, the algae and moss, caulerpa, sea grapes
  ...), Regions Unexplored (cattail, duckweed, flowering and giant lily pads), Twilight Forest (huge lily pad, huge water
  lily) and Alex's Caves (ping-pong sponge) placed in the interior lower the nitrate and take up waste, count as plants
  for breeding (the bonus of a well planted tank, hiding spots), for the young shrimp and the food of the small animals,
  for the oxygen and against algae; floating leaves shade the surface. Each plant (the touching blocks of one kind) works
  like a full-grown Bettacraft plant of the same growth form times `interiorPlants.foreignPlantFactor` (0.6). They do not
  grow and have no needs: no light minimum, no fertilizer (they neither use it nor grow with it), no reef elements, no
  salinity. At most `interiorPlants.maxPlantsPerTank` count per tank.
- **Fintastic's live rock** (also with algae, porous) is a small biological filter in salt water (0.6 times ours); its
  dead live rock is decoration. Corals and epiphytes grow on both.
- Data pack profiles `data/<namespace>/bettacraft/foreign_plants/*.json` (blocks or tags, a role such as `stem`,
  `carpet`, `floating`, `coral` or `live_rock`, a factor; `none` takes a default out). Missing ids of a loaded mod are
  reported in the log with similar ids (renamed?), those of missing mods are skipped.
- `/bettacraft foreign plants [radius]` (operators): the plants of other mods in the tanks with their role, plant mass and
  profile, and the plant blocks of other mods there without one.
- The plants tab shows how many plants of other mods count and their plant mass.
- The water plants of Regions Unexplored, Alex's Caves and Twilight Forest stand in the dry interior and sway in the
  miniature like those of Fintastic; their floating leaves mark the surface for bubble nests.
- Feature switch `features.aquaticPlants.foreignPlants`.
- Handbook (English and German): the page "Plants of other mods" with the table of every plant.

### Changed
- Interiors of earlier versions count their kelp, seagrass and plants of other mods once when they are loaded.

## [1.29.0-beta.1] - 2026-11-16

Liquid fertilizer and the fertilizer maker; a crank that winds the machines, and with the power options on also the
filters, the water devices and the cabinet under a tank, so you can keep a tank without any generator.

### Added
- **Liquid fertilizer:** a bottle of five portions; right-click a tank to dose one (a portion brings 4 water blocks to the
  right level). Up to 100 % the aquarium plants and the floating plants of Bettacraft grow up to half as fast again
  (`fertilizer.growthBoost`); they grow without it as well, and plants of other mods are not affected. The plants use it
  up, a little fades by itself, water changes dilute it, and it travels with the water in pipes (a filter loop keeps it).
  From 150 % the tank is **overdosed**: no more growth, but the algae grow faster (three times as fast at 300 %). The
  Water tab shows the level (none, low, good, overdosed).
- **Fertilizer maker:** mixes a kelp meal, a nutrient salt and 250 mB of water into 250 mB of fertilizer (a bottle). Water
  from a bucket, a pipe or other mods; empty glass bottles are filled, otherwise the fertilizer goes through a pipe into a
  tank, which takes it only up to 100 % (`fertilizer.pipeUpTo`). It runs on a spring wound with the crank, or on FE with
  a power option on (then its spring when the FE is short), and on Create's rotation. Item, fluid and energy capabilities
  for hoppers, AE2, Refined Storage and other mods' pipes (fluid `bettacraft:fertilizer`). Comparator, redstone pause.
- New intermediates **kelp meal** (dried kelp and bone meal) and **nutrient salt** (bone meal, charcoal, an iron nugget).
- **Fertilizer canister** for the dosing pump: it doses fertilizer into fresh and salt water tanks, its mL per day
  (`features.fertilizer.dosing`).
- **Crank:** right-click a device with a spring to wind it; hold the button to keep winding (one charge, a day of a
  device's work, in about five seconds; `winding.clicksPerCharge`). Without the power options only the food maker and the
  fertilizer maker need it. **With a power option on** (`features.power.winding`) every device that needs FE can be wound
  instead: the external filter (it drives the heaters and coolers in it too), the multiblock filter (six charges, wind any
  part), the reverse osmosis unit, the water change controller, the water pump, and the **cabinet** under a tank (six
  charges): it stores the winding and gives it to every device of its tank that needs FE (filters, heaters and coolers,
  heat mats, misters, lights, air pumps), the more devices the faster. FE always goes first; a spring stands still in
  unloaded chunks. The windows of the filter, the cabinet and the machines show the spring, so do Jade, The One Probe
  and WTHIT; the Water tab says how many devices the springs drive.
- Feature switches `features.fertilizer.enabled`, `features.fertilizer.dosing` and `features.power.winding`; server config
  sections `winding` (charges per device, ticks and clicks per charge) and `fertilizer`.
- Handbook (English and German): pages for the crank, liquid fertilizer and the fertilizer maker; power, food maker and
  settings updated.

### Changed
- **The food maker is wound with the crank** instead of sneak + right-click with an empty hand (that now only says so); a
  full spring takes 25 clicks (`foodMaking.windsToFull`, before 8 winds). With a power option on it no longer has to run
  on FE alone: it takes FE first and its spring when the FE is short.
- The water values carried by pipes include the fertilizer; the network version is 17 (clients need this version).

### Upgrading
- Old worlds load unchanged: tanks get no fertilizer, filters and cabinets an unwound spring (tank schema 11, filter and
  cabinet schema 2); the food maker keeps its wound spring. A config written before keeps `windsToFull = 8`.

## [1.28.0-beta.1] - 2026-11-15

Smaller bettas: a new size setting with Normal as the default and the old size as Large.

### Added
- **Betta size setting** `sizes.betta` in the server config (`bettacraft-server.toml`): **NORMAL** (the default) or
  **LARGE**. Normal draws the bettas at 0.77 of their old size, about halfway between the old size and the bettas of
  DragN's Just Bettas (an adult halfmoon male with its tail is now about 0.55 blocks long instead of 0.72), which is also
  the scale of the tankmates (about 12 cm per block). Large is the size before 1.28. All betta species and every life
  stage scale in proportion.
- The model and the hitbox change together, everywhere a betta is drawn (world, tanks, the handbook scenes); the fins
  still stay inside the glass at either size.
- The server's setting counts for every player: it is sent to the clients at login and whenever the server config is
  reloaded, and a change works at once (the fish refresh their hitbox on their next tick), no restart needed.

### Changed
- **Bettas are smaller by default.** Only the look and the hitbox change: the stocking, the breeding, the fights, the
  predators' choice of prey and every saved value of a fish are the same at both sizes, so you can switch back and forth.
  The other animals keep their size (they were already on the 12 cm per block scale the Normal bettas now follow).
- Handbook: a section on the size on the betta page (with a picture of both sizes) and the option in the settings table;
  the betta pictures are new.

### Upgrading
- Existing worlds get Normal bettas after the update (no data changes). Set `sizes.betta = "LARGE"` in the world's
  `serverconfig/bettacraft-server.toml` to keep the old size.

## [1.27.1-beta.1] - 2026-11-15

Artemia cysts made simple: one jar is one culture, jars always stack to 64, and sand gives four times as many.

### Changed
- **One jar of artemia cysts is one culture:** crafting an artemia culture or one batch of the artemia hatchery uses the
  jar up. No more uses per jar: no durability bar, no "uses left" tooltip, no used jar coming back from the crafting grid.
- **Jars always stack to 64.** The server option `food.artemiaCystStackSize` is gone (and with it its sync to the
  clients); the config migration removes the old key from `bettacraft-server.toml` (backup `.pre-m35.1`).
- **Four times as many cysts:** breaking sand drops a jar with 8 % instead of 2 % (`drops.artemiaFromSandChance`; a
  server config that still had the old default 0.02 is moved to 0.08 by the migration, a value you set yourself stays).
  The sand drop is the only source of cysts.

### Upgrading
- **Old jars carry over 1:1 in count, remaining portions are dropped:** a stack of 16 jars (full or opened) becomes 16
  jars of one culture each and stacks with new ones. This happens when a stack is loaded (inventories, chests and other
  containers, the hatchery's slot and its open jar, item entities, pipes, Applied Energistics 2 cells), no world scan.
  Refined Storage keeps old jars as an entry of their own in its list until you take them out; then they are plain jars.

## [1.27.0-beta.1] - 2026-11-15

Land and marsh animals of other mods in terraria and paludaria, fish in the water part of a paludarium, sea turtles as
big animals.

### Added
- **Land and marsh animals of other mods** live in our terraria and paludaria when they have a **terrarium profile**
  (feature switch `features.foreignAnimals.terrarium`): our care (hunger, the climate, sickness), our breeding (egg layers
  lay into sand or soil on the land, the nest temperature hatches them; livebearers give birth in the tank; their own
  breeding is suppressed) and our walker instead of their AI (it walks the land of the miniature, swims, basks, climbs,
  slithers, hides buried, and a new **flying style** for the dragonflies). Item food by hand on the tank and from the
  feeder (greens, crickets, prey, raw meat and fish, leaves ...).
- **Shipped profiles:** Naturalist tortoise (dry terrarium, 1 m), alligator (big paludarium, 2 m, hunts small fish),
  lizard, snake, desert and jungle scorpion (dry terraria), snail (humid), crab (paludarium, fresh or sea water),
  dragonfly, duck (0.6 m) and capybara (1.2 m, big paludaria); Critters and Companions dragonfly, leaf insect, otter
  (big paludarium, hunts small fish) and the sea bunny (sea water aquarium). Scorpions are harmless in a tank.
- **Size rule for terraria and paludaria:** a land animal with a `length` goes by the size rule of the big animals (M30),
  measured over the land, the water part or the whole tank; small animals by their hitbox and an optional `min_blocks`.
- **Terrarium box for foreign animals:** catches the land animals with a profile (and those of `terrarium.boxAllow`,
  without our care) with all their data; big ones go with the large net into the transport tub.
- **Sea turtles as big animals** (`features.turtles.seaTurtles`): grown ones and hatchlings only into big sea water
  paludaria with a sand or soil beach, where pairs lay their eggs; a hatchling that grows up stays.
- Profile keys `terrarium.length`, `volume`, `movement` (`walk`, `slither`, `hop`, `climb`, `swim`, `fly`, `own`), `prey`,
  `box`, `min_blocks`, `breeding.nest`, `nest_temperature`; handbook pages en + de.

### Changed
- **Fish in the water part of a paludarium:** our fish, the fish of other mods with a profile and the animals the
  inhabitant rules admit go into a paludarium by its water part (deep enough for the fish; stocking and size rule over the
  water part) instead of the fill of the whole tank, come in over the open water and stay there (a fish of another mod
  that swims into the land is put back).
- A mother that leaves her tank takes her laid clutch along and lays it again in her next tank (no empty egg shell stays);
  a mother that dies in her tank loses her eggs.

## [1.26.1-beta.1] - 2026-11-15

A tank type switch, so a terrarium is never flooded by mistake.

### Added
- **Tank type** (button "Type" in the overview tab of the tank menu next to the target temperature, and at the top of the
  climate tab): *Automatic* (the default for new and existing tanks: the kind follows the water and the land as before),
  *Aquarium*, *Paludarium* or *Terrarium*. A fixed type makes the tank that kind.
- **Water limit of a fixed type:** a terrarium takes no water, a paludarium only up to the height where its land part stays
  at `terrarium.paludariumLandShare` (the climate tab shows the most water). The limit holds for buckets, pipes and pumps,
  the fluid capability of other mods (Create pipes and the like get the part that fits instead of a refusal), the top-off
  and the water an animal brings along; a water change only replaces what it drained. An action bar message says why
  ("Terrarium: no water", "Paludarium: water only up to 1.8 blocks high").
- Nothing is ever removed: a type that does not fit the water already in the tank keeps it (the menu says it is too much,
  no more comes in), draining works as always.
- The type is saved (absent in older worlds = automatic) and synced; the info overlays (Jade, The One Probe, WTHIT) show a
  fixed type as "(fixed)"; the tank log notes who set it.
- Feature switch `features.terrariums.tankMode` (off: every tank is automatic, the chosen type is kept); en + de; handbook
  page "Terrarium and paludarium".

### Changed
- Network version 14: the tank type packet (client -> server) is checked like the other menu actions: open menu and
  reach, the lock, claims and spawn protection, a valid type and at most one change every 5 ticks per tank.


Modpack configuration: every feature can be switched off on its own, presets for simple, realistic and decoration
tanks, a care mode without deaths, a second hardening pass over the systems of 1.14 to 1.25, tools for foreign animal
profiles and death of old age.

### Added
- **Feature switches per feature** (common config, section `features`): one tree per feature group with
  `features.<group>.enabled` and its sub-switches next to it (`features.tankmates.cichlids`, `features.saltwater.seahorses`,
  `features.pipes.autoWaterChange` ...), 105 switches in all, with parents and requirements (corals need salt water,
  herp breeding needs breeding ...). A feature that is off loses its recipes, trader offers, loot, net catches and wild
  spawns; its items and blocks stay registered (worlds stay safe), but are no longer obtainable.
- **Hidden when off:** the server sends its switches to every client (also after a config reload), so the creative tab,
  JEI (items and the crucible / food maker categories) and the handbook (navigation, search; a page reached by a link
  names the group that is off) follow the server.
- **Resting logic:** a feature that is off rests, nothing is deleted and nothing dies: betta and tankmate care (per
  species group), breeding, small animals, frogs, algae, floating and aquarium plants, corals, the interior (no entering,
  no growth), salt shock, FE demand (devices run without FE), live food breeders and cultures, device sounds, info
  overlays, Better Fish Tanks. Switched on again, everything goes on where it stopped.
- **`bettacraft:feature` condition for datapacks:** as a recipe condition (`{"type": "bettacraft:feature", "feature":
  "tankmates.cichlids"}`) and as a loot condition; every Bettacraft recipe and loot modifier of an optional feature
  carries it.
- **Presets** (common config `preset = CUSTOM | SIMPLE | REALISTIC | DECOR`, `presetKeep`): a second set of defaults over
  the feature switches and the server values. *Simple*: no water chemistry, CO2, reverse osmosis or power, no forced water
  changes or evaporation, gentler care. *Realistic*: everything on, strict values, the devices need FE. *Decor*: no care,
  no chemistry, power, algae or breeding. A value changed in the file or listed in `presetKeep` wins; nothing is written
  into the files. `/bettacraft preset [name]` shows or switches it (operators), the clients follow.
- **Care mode** (`care.mode = NORMAL | NO_DEATH | DECOR`): NO_DEATH keeps needs and sickness but no animal in a tank or
  terrarium dies of them or is eaten; DECOR has no needs at all. Players and `/kill` still kill.
- **Death of old age** (`health.oldAge.enabled`, off by default, not part of a preset): bettas and tankmates die after
  `health.oldAge.years` (2) in-game years of `health.oldAge.daysPerYear` (96) days in an aquarium; the tank log, the chat
  message and the KubeJS `AnimalDiedEvent` give the cause `old_age`. The age counts with the care (loaded chunks only); a
  fish from an older version starts at the age of its life stage. Never in `care.mode` NO_DEATH or DECOR. Small animals,
  amphibians, reptiles, turtles, foreign animals and mobs in glass do not age to death.
- **`/bettacraft foreign list [radius]`** (operators): the animals of vanilla and other mods in the tank you look at (or in
  the tanks nearby) with their entity id and the profile file that maps them or "no profile", and the entity types of
  other mods that live in these tanks without a profile.
- **Renamed foreign entities are reported:** if the mod of a foreign animal profile is loaded but its entity id is not
  registered (renamed in a newer version of the mod), the server log warns with the profile file, the missing id and the
  similar ids of that mod. A profile for a mod that is not installed stays quiet as before.

### Changed
- **Feature switch keys renamed** (for modpacks): the flat keys of 1.25 (`features.wildBettas`, `features.cichlids`,
  `features.foreignCare` ...) are now paths in the feature tree (`features.bettas.wildSpecies`,
  `features.tankmates.cichlids`, `features.foreignAnimals.care` ...). The game **migrates an existing
  `config/bettacraft-common.toml` automatically** when it starts: every value goes to its new key (what was off stays off),
  the old keys are removed, the old file is kept as `bettacraft-common.toml.pre-m33`, and each moved key is logged. The
  handbook's settings page has the whole tree.
- A species sub-switch (`features.tankmates.cichlids` ...) now also lets the care of the animals already in tanks rest.
- Network version 13 (feature switches and the preset are synced): clients and servers of 1.26 only connect to each other.
- `saltwater.foreignAnimals` stays LENIENT by default (the Realistic preset sets REALISTIC).

### Fixed
- A monitor layout sent by a client keeps only the settings the widgets know, with sane values and tile ids (it could
  carry any NBT into the block entity, the chunk and every client's sync).
- A pipe network built while part of it was in an unloaded chunk is built again shortly after (its pipes and ends there
  stayed missing until a pipe changed); a pipe asked for one fluid gives only that fluid.
- The water change controller's menu respects claims, spawn protection and the build rights of an interior, and "change
  now" runs at most once a second.
- Decoration template requests are answered at most 32 times per player and second.
- A mob in glass converted by vanilla or another mod (lightning, powder snow ...) keeps its mark and still drops nothing;
  a converted mob that may not live in glass leaves the tank's list.
- Caches by interior region or position (interior pipes, tankmate spots, seahorse posts, currents) no longer reach the
  next world of the same game (single player); currents are cached per dimension.
- Shrinking an interior into a big tank's coarser one no longer turns free substrate into paid substrate (an item from
  nothing when it was taken out).
- `health.oldAge.enabled` had no effect (see Added).

## [1.25.0-beta.1] - 2026-11-15

Full compatibility: the animals of other mods get our care and breeding, tanks travel on Create contraptions and
airships, Create, AE2, Refined Storage and Mekanism work with the devices, and modpacks get a quest chapter, script
events and map waypoints.

### Added
- **Animals of other mods with our care and breeding** (`features.foreignCare`, `features.foreignBreeding`): data-driven
  profiles (`data/<namespace>/bettacraft/foreign_animals/*.json`, `/reload`) give an animal of vanilla or another mod
  hunger, a temperature range, water quality, pH and TDS, stocking and (with `saltwater.foreignAnimals = REALISTIC`)
  salinity in a Bettacraft aquarium: days of trouble make it sick, a sick animal dies (tank log with the cause). Two
  grown, fed, healthy animals breed by our rules: livebearers give birth, egg layers spawn (some need plants, food fish a
  big aquarium), and the young take their parents' variants (colours, patterns) from the NBT their mod stores. Profiles
  ship for Fintastic (guppy, gourami, pleco, small catfish, minnow, moony, cod, salmon), Aquaculture's small fish,
  the koi of Critters and Companions and Naturalist's bass, catfish and piranha. A handbook page explains the format for
  modpack makers. `foreign.foreignBreedingSpeed`, `foreign.foreignHealthSpeed`.
- **Their own breeding suppressed in tanks** (`features.suppressForeignBreeding`): no love from food, no babies of their
  mod's breeding (vanilla, Fintastic) for the animals in a Bettacraft aquarium; our axolotl, turtle and mob breeding is
  unaffected.
- **Tanks on Create contraptions and Create Aeronautics airships** (`features.createContraptions`): pistons, bearings,
  pulleys, gantries, carts, trains and Sable airships move the whole tank; the animals are packed into it for the trip
  and drawn swimming in the travelling miniature, the care pauses, and water, plants, interior, small animals and values
  arrive where the tank is put down (also turned: the interior turns with it). Eggs in the nest, young in clutches and a
  player inside keep a tank in place.
- **Create rotation** (`features.createRotation`): a shaft, cogwheel or motor pointing at a water pump, filter pump head,
  water change controller, level sensor, reverse osmosis unit or food maker drives it (64 RPM full work, slower
  proportionally, overstressed none; no stress cost).
- **Create Display Links** (`features.createDisplayLinks`): tanks ("Tank values") and monitors ("Monitored tanks") are
  display sources for display boards, nixie tubes, signs and lecterns. Create pipes and pumps fill tanks.
- **Autocrafting with AE2 and Refined Storage:** pattern providers and autocrafters put the ingredients of a processing
  pattern into the food maker and the crucible; an import bus brings the product back.
- **Mekanism:** the Bettacraft fluids (reverse osmosis water, sea water, live food) go into its tanks and pipes; the CO2
  fluid of Create: The Factory Must Grow (`#c:carbon_dioxide`) feeds the CO2 station.
- **FTB Quests chapter "Bettacraft"** for modpacks: 33 quests from the first tank through the betta, water care,
  breeding, plants, the reef and big tanks to the terrarium, with advancement and item tasks and the texts as
  translation keys (English and German). Not in the jar: `bettacraft-modpack-<version>.zip` of the release
  (`gradlew modpackZip`, the folder `modpack/`), with a KubeJS example script.
- **Events for KubeJS and other mods** (`features.modpackEvents`): `AnimalBornEvent` (young born or hatched in a tank,
  ours and foreign), `AnimalDiedEvent` (with the cause, the animal and the killer) and `TankAlarmEvent` (a monitor alarm
  goes off or clears) on the NeoForge event bus (`org.overengineer.bettacraft.api.event`, KubeJS `NativeEvents.onEvent`),
  and read-only helpers in `org.overengineer.bettacraft.api.BettacraftApi` (`tankValues` and more).
- **JourneyMap waypoints for monitor alarms** (`features.alarmWaypoints`, client `map.showAlarmWaypoints`): an alarm
  puts a red waypoint at its tank for the owner and the players within 128 blocks, until it clears.

### Changed
- Monitor screens with an alarm tile check their tanks every second also when nobody looks (before only those with
  redstone alarms), for the events and waypoints.

### Fixed
- A tank whose controller Create cannot put down (the target is unbreakable) no longer loses its data and animals: the
  blocks that came down form the tank, or the dropped block becomes a **packed tank** item that sets it up again.
- A destroyed contraption (a minecart, bearing or piston entity that is gone for good) frees the interior region of its
  tank instead of leaving it behind.

## [1.24.1-beta.1] - 2026-11-15

Tank interiors are much cheaper to create.

### Fixed
- **Interior chunk generation**: the interior dimension is generated without vanilla's structure neighbourhood (the
  structure starts of all chunks 8 around each chunk). Creating a tank's interior used to generate, hold and save
  about 750 tall chunks (a 4x3x3 tank needs 30); now about 130, and only the interior's own chunk columns stay loaded.
  Interiors are built faster, the server needs far less memory when many are created at once (the showcase world:
  10.6 GB → 1.9 GB of heap, saving in 21 s instead of 8 min), and each new tank takes about a sixth of the disk space.
  Interiors that already exist are unchanged. `-Dbettacraft.vanillaInteriorGeneration=true` switches back.

## [1.24.0-beta.1] - 2026-11-15

Terraria and paludaria: tanks with land, their climate and devices, turtles with temperature-dependent sex, frogs,
geckos and a snake, and Minecraft mobs shrunk and harmless behind glass.

### Added
- **Terrarium and paludarium** (`features.terrarium`): a tank without water is a terrarium, a tank with water and land
  built in its interior above the water line a paludarium (`terrarium.paludariumLandShare`, `terrariumLandShare`); the
  mode is detected, never set. Fish tanks stay aquariums as before. Terraria and paludaria get an **air temperature**,
  a **humidity** and **UV**, each balanced from the room (biome, day and night, rain on an open top), the devices, the
  water surface, the plants on the land and the lid. A new **Climate** tab, a line in the overview, the overlays (Jade,
  TOP, WTHIT) and the monitor show them; a dry terrarium shows no water values.
- **Terrarium devices** (`features.terrariumDevices`): the **heat mat** (under the tank or on its wall, thermostat = the
  tank's target temperature, FE with `power.heaterRequiresEnergy`), the **basking lamp** (light, a basking spot 12 K
  above the air, UV 5), the **mister** (hygrostat 40-95 %, 4000 mB of water from buckets, pipes or a reservoir, FE with
  `terrarium.misterRequiresEnergy`) and the **thermo-hygrometer** (LCD, comparator output = humidity).
- **Turtles** (`features.turtles`): the musk turtle, the red-eared and yellow-bellied slider (one species, two
  varieties; grows big and wants 12 blocks), Hermann's tortoise (dry terrarium, strong UV, eats greens:
  `#bettacraft:turtle_greens`, carried in the new **terrarium box**, **brumates** in the cold: `terrarium.brumation`) and
  Reeves' turtle. Vanilla **sea turtle hatchlings** live in paludaria until they outgrow them.
- **Turtle breeding** (`features.turtleBreeding`): a female lays a clutch into sand or soil on the land (an egg block in the
  interior); the eggs incubate by the ground temperature (heat mats), and the **nest temperature decides the sex** of the
  hatchlings (warm females, cool males; the musk turtle with the female-male-female pattern). Hatchlings grow up.
- **Amphibians and reptiles** (`features.herps`, `features.herpBreeding`): poison dart frogs (three species that do not
  cross; clutches on moss or leaves, tadpoles into the water), Cranwell's horned frog (sits buried), the leopard gecko
  (sex by the nest temperature, morphs), the crested gecko (climbs the glass) and the corn snake (a prey animal a week,
  brumates). They hop, climb and slither in the tank. **Feeder insects**: crickets and fruit flies from their cultures.
- **Wild turtles, frogs, geckos and snakes** (`features.wildTurtles`, `features.wildHerps`): sliders and musk turtles in
  swamps and rivers, Reeves' turtles in rivers, tortoises in savannas and shrublands (`#bettacraft:tortoise_lands`), dart
  frogs and crested geckos in jungles, leopard geckos in deserts and badlands, corn snakes in forests and plains, horned
  frogs in savannas and swamps.
- **Mobs in glass** (`features.mobsInGlass`): drowned, zombies, husks, skeletons, strays, spiders, cave spiders,
  silverfish, endermites and small slimes and magma cubes live in tanks **shrunk** (the `generic.scale` attribute) and
  **harmless**: no targets, no burning in daylight (`terrarium.mobUndeadBurn` for an open top), no drowning, no
  despawning. Each has its tank (the drowned needs water, spiders climb the glass of a terrarium); a zombie under water
  becomes a drowned. Caught with the new **mob jar** below a quarter of its health (`terrarium.mobCatchHealth`, with a
  cooldown), **lured** into an open tank with bait (rotten flesh, bones, crickets, slime balls, magma cream), or put in
  from an EnderIO **soul vial** or an Industrial Foregoing **mob imprisonment tool** (`#bettacraft:mob_capture_items` for
  others). Fed pairs breed (babies, spiderlings, small slimes; `terrarium.mobBreeding`). A mob that lived in glass
  **never drops anything and gives no experience**, so they are no farm. `inhabitants.allow` adds more hostile mobs.
- Handbook chapter **Terrarium and paludarium** with pages for every device, animal, the turtle breeding, the feeder
  insects and the mobs in glass (English and German).

## [1.23.0-beta.1] - 2026-11-15

Big tanks and big animals: aquariums up to 32 x 16 x 32 blocks with a coarser interior, a size rule for big animals,
ten giants of fresh and sea water (catfish, arapaima, rays, sharks, a moray), shark egg cases and dolphins in very big
sea water tanks, and the tools to catch and carry them.

### Added
- **Big tanks** (`features.bigTanks`): tanks grow up to `bigTankRules.bigMaxSize*` (32 x 16 x 32). A tank longer than
  16 blocks gets interior scale 8 (longer than 32: scale 4), so its interior stays about 256 blocks long. When a
  decorated tank grows past the threshold its interior is shrunk into the coarser one; nothing is lost: blocks that do
  not stay, devices with their contents and the plants with their stage come out in front of the tank (in chests).
- **Size rule of the big animals** (`features.bigAnimals`, `bigAnimalRules.*`): an animal of length L metres needs a tank
  of 12 L x 6 L blocks, 3 L blocks high and 100 L³ water blocks of its own (they add up). The message names the size
  that is missing. Small animals keep the old hitbox limit; whales, bosses and hostile mobs never go in; animals of
  other mods count as big through the tag `#bettacraft:big_tank_animals`.
- **Freshwater giants:** the pangasius, the redtail catfish, the wels catfish (cool rivers, no heater), the arapaima (an
  air breather) and the motoro stingray (glides over the sand and lies on it; livebearing with room by the size rule),
  with real temperatures and water ranges; wild in rivers and jungle water.
- **Sea giants:** the brownbanded bamboo shark (a bottom shark that rests under ledges by day), the blacktip reef shark
  (has to keep swimming, eats every small fish), the spotted eagle ray and the cownose ray (fly through open water with
  slow wing beats; cownose rays school and bear one pup with room) and the honeycomb moray (keeps a lair under the
  decoration, breathes with its mouth open, comes out at night and for food); wild in warm (and lukewarm) oceans,
  cownose rays in small schools.
- **Shark egg cases:** the bamboo shark lays leathery egg cases (mermaid's purses) under the decoration, only while the
  size rule leaves room for the pups; each pup hatches after about ten days, only when there is room, and waits in its
  case otherwise. The aquarium net moves a case to another tank; the breeding tab shows the room left.
- **Dolphins** (vanilla) in very big sea water tanks (from 32 x 16 x 8): they breathe and stay wet in the tank, eat raw
  fish by hand or from a feeder on the tank, hunt one small fish of their tank a day when they are hungry, and lose
  health after two days without fish or in too fresh water.
- **Catching big animals:** the large net (into an empty transport tub), the transport tub (carries one animal with all
  its data and slows you down while it is full) and the steel leader for the fishing rod (in the off hand). Buckets and
  fish bags refuse big animals; the tank menu takes a big animal into an empty tub.
- Big animals swim wide curves and turn slowly (their own movement control), find their way around the decoration and
  eat the small fish, young bettas and shrimp of their tank by the size of their mouth.
- Handbook chapter **Big tanks** with a page for every big animal and tool (English and German); the showcase world has a
  row with a freshwater and a sea water big tank.

### Fixed
- The miniature of a tank in freshly loaded chunks could keep dark stripes on the floor: its meshes kept the light of
  the moment they were built; the light of every tank block now counts.

## [1.22.1-beta.1] - 2026-11-15

Fixes and polish: no water blocks in the tank interior, every block as a 3D item, device sounds without rustling, finer
Bucephalandras and Cryptocorynes, the air pump in the cabinet and the multiblock filter under a tank.

### Added
- **Air pump in the aquarium cabinet:** the cabinet menu has a slot for an air pump. It drives the air stones and sponge
  filters of the tank above like a pump beside it (FE, monitor and tank menu count it); its air hose goes up together
  with the filter hose at the cabinet's outlet side. Old cabinets keep their contents.
- **Sound test:** the client command `/bettacraft sounds` plays every device sound (own and Minecraft twin) one after
  another with its name in the chat.
- Client config `sound.airPumpSound` and `sound.flowPumpSound` (both off by default).
- The showcase world has a multiblock filter connected by pipes, one directly under its tank and an air pump in a
  cabinet.

### Changed
- **Device sounds:** the filter outlet is a calm trickle of bubbles instead of a rustling noise; air stones and sponge
  filters keep only their bubbling (the noise beds are gone, nothing above 3 to 5 kHz); all of them quieter. The hum of
  the air pumps and the whir of the flow pumps are quieter and **off by default** (switch them on in the client config).
- **Every block of the mod is a 3D block item** (inventory, hotbar, JEI, handbook), like vanilla blocks; plants and
  crossed models stay flat. Lights show their beam, heaters and coolers their glass tube; clearer icons for the crucible
  (glowing melt), the daphnia barrel (water fleas), the food maker and the monitor.
- **Stainless steel without blaze powder:** the crucible melts 4 iron, coal and nether quartz into 3 ingots (blaze rods
  and powder stay fuel).
- **Plants:** Bucephalandras have short, narrow lanceolate leaves close to the rhizome; Cryptocorynes have narrower,
  longer leaves straight from the rosette, without a stalk; Vallisneria sways at about 30 % of its old swing (inside and
  outside).
- **Air hoses** run down the glass to the floor and over the floor to the air stones and sponge filters (to the nearest,
  then on to the next) instead of ending in the middle of the water.

### Fixed
- Breaking kelp, seagrass, corals, sea pickles or any waterlogged block (also of other mods) in a tank interior, with
  shears, by hand or with a piston, left a vanilla water block; the interior now gets air, and stray water blocks of
  earlier versions go on the next rescan.
- A multiblock filter could not get a tank built on top of it: a click with a block on a filter part opened the filter
  screen instead of placing the block. A filter directly under its tank (connectors facing up into the floor) works.

## [1.22.0-beta.1] - 2026-11-15

New freshwater species: 27 tankmates (cichlids, puffers, barbs and loaches, danios, rainbowfish, tetras, livebearers and
gouramis), the wild betta species *Betta imbellis* and *Betta smaragdina*, two big crayfish, and our care and breeding
for the vanilla axolotl.

### Added
- **Cichlids** (`features.cichlids`): ram (gold, electric blue, balloon), Bolivian ram, cockatoo and Agassiz's dwarf
  cichlid, angelfish (black, marble, koi, platinum) and discus (red turquoise, pigeon blood, blue diamond). New breeding
  styles: **open brooders** spawn on a flat stone, in a pit or on a broad leaf and both parents guard the brood;
  **cave brooders** (Apistogramma) spawn in small caves (coconut shell, slate cave). Guarding parents chase other fish,
  males hold territories, discus feed their young, angelfish eat small fish and shrimp.
- **Puffers** (`features.puffers`): dwarf puffer, red-eye puffer and the brackish green spotted puffer. They eat snails
  (bladder and ramshorn snails first) and their teeth overgrow without snails or hard food: the new **shell food**
  (food maker, `#bettacraft:hard_food`). Puffers and tiger barbs **nip the fins** of bettas, angelfish, gouramis and
  male guppies (`tankmates.behaviour.finNipping`, `pufferSnailsPerDay`, `pufferTeethDays`).
- **Barbs and loaches** (`features.barbsAndLoaches`): tiger barb (green, albino), odessa barb, cherry barb, dwarf chain
  loach and reticulated loach (snail eaters) and the garra.
- **More community fish** (`features.communityFish`): zebra danio, celestial pearl danio, forktail and neon dwarf
  rainbowfish, emperor, rummy-nose and black phantom tetra, black molly (also brackish) and swordtail (the male's
  sword).
- **Gouramis** (`features.gouramis`): honey, chocolate and pearl gourami (labyrinth fish). The chocolate gourami is a
  **mouthbrooder**; only territorial gourami males fight each other.
- **Wild betta species** (`features.wildBettas`): some wild bettas in swamps are *Betta imbellis* or *Betta
  smaragdina*. They cross with each other and with *B. splendens* into fertile hybrids; size and fighting strength
  follow the ancestry, the tooltip names the species or the shares.
- **Big crayfish** (`features.bigCrayfish`): the **Florida crayfish** (*Procambarus alleni*; wild, blue and orange as
  recessive colours) and the **marbled crayfish** (*P. virginalis*: only females, every adult carries eggs without a
  male, the young are clones). About 10 cm: they catch small animals, eat fry and small bottom fish, pinch bettas and
  slow fish near the floor, eat plants back and dig up young rooted plants; epiphytes and moss are safe
  (`invertebrates.bigCrayfishPreyPerDay`, `bigCrayfishFishPerDay`, `bigCrayfishPlantsPerDay`). Aquarium net and wild
  in swamps (Florida) and rivers (marbled), wandering trader; one counts as 0.75 fish, 2 per bag, 4 per bucket.
- **Axolotl care** (`features.axolotlCare`): vanilla axolotls in a tank want cool water (15–20 °C, a cooler); from
  21 °C they do not breed, above 23 °C they lose health. Two adults with plants lay **eggs on the plants**, the larvae
  hatch as baby axolotls (`invertebrates.axolotlBreeding`, `axolotlEggDays`, `axolotlHeatDamage`). The five vanilla
  colours are inherited by Mendel (blue recessive and rare); the magnifier on the tank tells their colour genes, a
  bucket from the tank menu keeps them. No own entity: the vanilla model, bucket and other mods keep working.
- Handbook pages in English and German for every new species, the new groups (cichlids, puffers, barbs, rainbowfish,
  gouramis), the wild bettas, the big crayfish and the axolotl, with pictures.

### Changed
- Snail eating works for every snail eater (loaches, puffers); the log names the species.
- Gourami fights only between territorial gourami males (dwarf and pearl gourami).

## [1.21.1-beta.1] - 2026-11-15

Every aquarium and floating plant now grows wild in the waters of its origin, and wild animals are easier to find:
an own frugal spawn pass outside the vanilla mob cap and the operator command `/bettacraft wildlife`.

### Added
- **Wild plants in the world** (`features.wildPlants`, `spawn.wildPlantFrequency`; new chunks): all 49 aquarium plants
  grow in small groups in the waters they come from: Anubias, Bucephalandra and Java fern on stones and sunken roots
  in jungle water with the crypts, swords and lotus; stem plants, carpets and lotus in swamps; Vallisneria, Sagittaria,
  hairgrass and mosses in rivers; crypts, Java fern and moss in mangroves; **Marimo** balls in cold lakes and frozen
  rivers; mosses in the pools of the lush caves. Strong-light and soft-water plants are rare, strong-light ones only in
  shallow water. The waters are the biome tags `#bettacraft:*_waters` (new: `#bettacraft:cold_waters`), so biome mods
  count.
- **Shears** cut a cutting off a wild plant (two of a stem plant); it grows back in about an in-game day. By hand it
  gives nothing like seagrass, with silk touch the whole plant (half grown). Five shared world blocks (epiphyte, moss,
  rosette, stem plants, carpet) carry the species and look like it.
- **Wild floating plants** (duckweed, frogbit, salvinia, red root floater, water lettuce, hornwort, Riccia) lie on calm
  water like lily pads in swamps, mangroves, jungle water, rivers and cold lakes; pick them up by hand, with shears or
  scoop them with the aquarium net.
- **Own spawn pass for wild animals** (`features.wildSpawnPass`, `wildlife.spawnPassInterval`,
  `wildlife.spawnPassPerPlayer`): with many fish mods the vanilla water-ambient cap is full of ocean fish, so now and
  then a few wild fish, frogs or small animals spawn in fitting water near each player, at most 10 within 48 blocks.
- **`/bettacraft wildlife`** (operators): biome, water, the spawn entries of the mod with weight and share, wild
  animals nearby, the fill of the mob cap and whether spawning can happen; `list [radius]` and `glow [radius]`.
- Entity tags `#bettacraft:all_animals` and `#bettacraft:wild_animals` (e.g. `@e[type=#bettacraft:all_animals]`).
- Handbook: "Wild in the world" on the plants page, the new places in every "Obtaining" section, settings and commands
  (English and German).

## [1.21.0-beta.1] - 2026-11-14

Many more plants: 32 new aquarium plants (Anubias, Bucephalandra, stem plants, carpets, mosses, the rolling Marimo,
rosettes and tubers) and two new floating plants; 16 new decorations (wrecks, aquascaping stones, roots, caves and
tubes, ornaments) and leaf litter that lowers the pH and feeds the shrimp.

### Added
- **Anubias:** *A. barteri* var. *nana* 'Petite', 'Pinto' (white marbled), 'White', *A. coffeefolia*, *A. barteri*
  'Round Leaf'. **Bucephalandra:** 'Kedagang', 'Theia', 'Godzilla', 'Pink Lady', 'Mini Pink', 'Snow White' ('Biblis'):
  the white and pink ones only colour under strong light (light bar: half, LED panel or high-power LED: fully).
- **Stem plants:** Ludwigia 'Super Red', Rotala 'H'ra' and *R. wallichii*, *Hygrophila pinnatifida* (also grows on
  stone and wood), *Pogostemon helferi* 'Downoi', *Limnophila sessiliflora*, Myriophyllum 'Red', *Bacopa caroliniana*.
  Some need CO2 (40 % growth without) or soft water (`features.co2`, `features.waterChemistry`).
- **Carpets:** *Hemianthus callitrichoides* 'Cuba' (very strong light and CO2), Eleocharis hairgrass, *Staurogyne
  repens*, Marsilea water clover. **Mosses:** Christmas moss, flame moss, Riccia (tied onto stone or wood, or floating:
  the same item put on a tank from outside), the **Marimo** ball: grows very slowly, takes cool water and **rolls**
  over the floor with the current of filters and outlets (`features.marimoRolling`).
- **Rosettes and tubers:** Echinodorus 'Red Diamond', *Cryptocoryne parva*, *Sagittaria subulata*, *Aponogeton
  ulvaceus* (a flower spike at the surface), the green tiger lotus.
- **Floating plants:** water lettuce (*Pistia*) and hornwort (its shoots hang deep into the water).
- All new plants with their own leaf meshes (swaying, shadows with shaders), from fishing and the aquarium net, with
  handbook pages in English and German. Plant lists in an unchanged server config get the new plants by themselves.
- **16 decorations** (multiblock builds like the others, all working in the tank interior and in the world):
  - wrecks: a **sunken rowing boat** (a low cave for catfish) and the **bow of a sailing ship** whose clay pipe in the
    hold is a spawning cave for plecos;
  - stones: **holey rock** (limestone: hardens the water), **slate cave**, **Seiryu stone** (calcite veins),
    **dragon stone** and **lava rock**;
  - roots: **mangrove root**, **spiderwood**, **bogwood bridge** (wood: lowers the pH a little);
  - caves and tubes: **coconut cave**, **clay tubes** (two spawning caves for plecos), **shrimp tubes**;
  - ornaments: **ruined column**, **diving helmet** (small fish swim in through the face window) and a **treasure chest**
    whose coins bubble while an air pump runs (an air stone hides among them).
- **Leaf litter** (`features.decorEffects`): Indian almond leaves and the new **alder cones** (birch leaves dried in a
  smoker, furnace or on a campfire) can be laid on the floor. In a tank interior they give off tannins all the time
  (the pH sinks a little), the shrimp graze on them, and clay and terracotta (tubes, caves, dragon stone, lava rock)
  hide young shrimp from hunting bettas. Alder cones also go into a tank like almond leaves, with a third of the tannins.
- Handbook: pages for the new decorations and the leaf litter in English and German.

### Changed
- **Hammer coral and mushroom coral** grow in a new shape (coral generator version 3): the hammer coral as a low clump
  of heads, the mushroom discs keep apart. Corals already in a tank keep their shape.

## [1.20.0-beta.1] - 2026-11-14

Aquarium monitors: screens of monitor blocks that find the tanks around them and show a dashboard of values,
traffic lights, graphs, animals, devices, alarms with redstone and the logbook, arranged in an editor.

### Added
- **Monitor** (`features.monitors`, `monitor.*`): monitor blocks next to each other join into one screen of up to
  8 x 5 blocks with one frame (bigger walls are cut into several screens; the dashboard goes on when a screen grows,
  shrinks or splits). The screen finds every tank within `monitor.range` (32) blocks by itself, named after the tank
  sign; tanks out of range or unloaded show "not loaded" with their last values. While a power option is on it needs
  `monitor.fePerBlock` (1) FE per tick and block. Recipe: 4 iron, 4 glass panes and a redstone make 4 monitors.
- **Tank history:** every tank records all its values once per in-game hour for 7 in-game days (also without a
  monitor), so graphs show the past right away.
- **Widgets:** value tile (temperature, water quality, pH, TDS, salinity, calcium, KH, magnesium, oxygen, CO2, nitrate,
  water level, animals, algae, food in the feeder; optionally with the graph of the last day), traffic light (the
  tank's state with the warnings of its menu), history graph (one to three values over 1-7 in-game days), animal list
  (count per species, bubble nest, eggs, young), device status (filters, heaters, coolers, light with dimmer, CO2, air
  pumps, power, the tank's energy), alarm (blinks past a limit) and event log (the latest logbook entries of one or
  all tanks). Out of the box the screen arranges two value tiles per tank by itself.
- **Dashboard editor** (right-click a monitor; sneak + right-click shows its state): arrange the tiles on the screen's
  grid by drag and drop, resize them, pick type, tank, values, graph days, alarm limit and colours. Needs the build
  rights at the monitor; "Automatic" gives the screen back its automatic layout.
- **Redstone alarms:** an alarm tile with redstone makes every block of its screen give a signal of 15 while it is
  triggered; a comparator reads the number of triggered alarms. Such screens watch their tanks also when nobody looks.
- The data only goes to players who see a monitor, bundled into one packet per player and second.
- Handbook page for the monitor (widgets, editor, alarms) in English and German.

## [1.19.1-beta.1] - 2026-11-14

German translation checked, a wide logo for the mod list, and releases go to CurseForge only.

### Added
- **Wide logo** in the NeoForge mod list: a 512x128 pixel-art banner with the halfmoon betta of the icon (made by
  `tools/assetgen`); the square icon stays the mod icon (also for Catalogue).
- **Mod page text** `docs/description.md` (features of all versions with pictures and animations, compatibility, FAQ,
  credits, a short German summary) and `gradlew curseforgeDescription`, which makes the paste-ready CurseForge version
  with image links pinned to the release tag. The release workflow builds it as an artifact.

### Changed
- Releases are uploaded to **CurseForge only**; the CurseForge upload also names Fish of Thieves as an optional
  integration.
- German: config labels for drops and sounds, the log tab ("Protokoll"), wild-type alleles ("Wildtyp") and the info
  texts of several tankmates (grammar, capitalisation, a doubled line) are fixed. A test now fails on German texts that
  are still English.

### Fixed
- The handbook page of the food maker did not load (the button labels < and > broke GuideME's page parser); a test
  checks every page for this now.

## [1.19.0-beta.1] - 2026-11-14

Live food and fish food: tubifex and daphnia cultures, a live food pump that feeds tanks through pipes, a food maker
for flakes, granules, frozen food and more, and food variety that matters to the fish.

### Added
- **Tubifex dish** (`features.liveFoodCultures`, `liveFood.*`): tubifex worms in mud and stale water, fed with mulm
  (washed out of dirty filter media in a water cauldron) or the waste water of a tank through a pipe. The water goes stale
  and evaporates; worms from a stale dish carry germs (a little bad-water stress). Tubifex condition a breeding fish
  twice as long as other live food. Mining mud in a swamp sometimes drops a few.
- **Daphnia barrel**: water fleas in a barrel of water with yeast or green water (swamp water with bone meal ripened in
  the light); they need light, collapse when crowded or in the hot sun. A bottle of swamp water sometimes brings a few.
- **Live food pump** (`features.liveFoodPump`): next to any breeder it pumps the live food as the fluid
  `bettacraft:live_food` through the pipes (or straight into a tank next to it) on a schedule: doses per day, portions
  per dose and "Dose now" in its window. A tank only takes it when somebody there eats it.
- **Food maker** (`features.foodMaker`, `foodMaking.*`): makes pellets, fish flakes, food granules, algae wafers, fry
  powder, shrimp food and frozen mosquito larvae, artemia and daphnia from vanilla ingredients and live food, in
  batches. The product is chosen in its window or left on automatic. Without power it runs on a spring: sneak +
  right-click with an empty hand winds it (the crank turns, the ratchet clicks), a full spring works one in-game day;
  while a power option is on it takes FE instead. The recipes are data (`bettacraft:food_maker`) and shown in JEI.
- New foods: **fish flakes** (float; the midwater fish like them), **food granules** (sink to the bottom fish),
  **frozen food** (eaten like live food, keeps, cannot be pumped) and **shrimp food** (only for shrimp, crayfish and
  snails, twice the food of a leftover pellet).
- **Food variety** (`features.foodVariety`, `diet.*`): every food belongs to a food group and every fish remembers
  what it ate in the last 3 days. Three groups make a varied diet: the fish recovers one and a half times as fast from
  stress and sickness, a betta shows stronger colours. Each kind of eater (bettas, midwater fish, bottom fish, grazers,
  seahorses) has favourite food (it conditions for breeding) and food that suits it badly (hungry again sooner); all
  item tags (`#bettacraft:food_groups/*`, `#bettacraft:diet/*/favourite` and `/poor`). The tank menu shows the variety
  on the food bar.
- Dev: the group `-Pstorage` (Applied Energistics 2 and Refined Storage) with GameTests in which their buses fill and
  empty the food maker.

### Changed
- The feeder gives live food first, and its slots take turns: each feeding starts after the slot that fed first last
  time, so several foods in it give a varied diet.
- The food maker, the feeder and the reservoir offer plain item and fluid handlers: hoppers, pipes of other mods and
  the buses, interfaces and autocrafting of AE2 and Refined Storage work with them without special code.
- Handbook: pages for the tubifex dish, the daphnia barrel, the live food pump, the food maker and food variety; the
  food table, the feeder, the filter media (mulm), the pipes and the settings pages are updated.

## [1.18.1-beta.1] - 2026-11-14

Biome mods: wild animals, net catches, fishing and the wild corals also in the biomes of biome mods.

### Added
- **Biome tags** for the waters of the wild animals, the aquarium net, fishing and the mud snails:
  `#bettacraft:warm_ocean_waters`, `lukewarm_ocean_waters`, `mangrove_waters`, `lush_cave_waters`, `swamp_waters`,
  `river_waters`, `beach_waters` and `jungle_waters`, and `#bettacraft:coral_reef_biomes` for the wild corals. Defaults:
  the vanilla biomes, the common tags (`#c:is_swamp`, `#minecraft:is_river`, ...; a warm ocean is also every biome in
  both `#c:is_ocean` and `#c:is_hot`, a lush cave every biome in both `#c:is_lush` and `#c:is_cave`) and biomes of
  Regions Unexplored, Oh The Biomes We've Gone, Biomes O' Plenty and Twilight Forest as optional entries. Modpacks can
  extend them in one place; the handbook page "Settings and other mods" lists them.

### Changed
- Reef fish, seahorses, the reef animals, racer nerites, trumpet snails of the lush caves and the wild corals were
  bound to the vanilla warm ocean, lukewarm ocean, mangrove swamp and lush caves; they now live in the matching biomes
  of biome mods too (for example the rocky reef of Regions Unexplored). Vanilla worlds stay exactly as before.
- Handbook: the Obtaining sections say that the biomes of biome mods count too.

## [1.18.0-beta.1] - 2026-11-14

Water chemistry: TDS, pH and hardness of the water with a range for every animal, CO2 dosing for the plants, a reverse
osmosis unit for soft water, bee shrimp that need it, Sulawesi animals that need hard water, active soil, and reef
chemistry with a dosing pump.

### Added
- **Water chemistry** (`features.waterChemistry`, `chemistry.*`): every aquarium has a **TDS** (ppm, normal water about
  250, reverse osmosis water 0), a **pH** (5 to 9, normal water 7.5) and a **hardness** (soft, medium hard, hard). Food and
  waste raise the TDS, evaporation leaves the minerals behind, water changes, pipes and fusing tanks mix the values by
  volume. The pH drifts within a day to what the tank holds: active soil and plain soil pull it down, limestone
  (calcite, dripstone, dead coral, Create's limestone) as substrate or rocks pulls it up, peat in the multiblock filter,
  driftwood (logs and roots in the interior), Indian almond leaves (tannins) and CO2 lower it.
- Every animal has its **range of pH and TDS** (shown in the species pages and the water tab); a little out of range
  stresses it, far out it gets sick and dies. Normal water suits every animal of earlier versions.
- **TDS meter**, **pH drop test** (50 tests), **remineralising salt** (sneak: brings the tank to 150 ppm) and **Indian
  almond leaves** (dry jungle leaves in a furnace, smoker or on a campfire). The water tab shows TDS, hardness and pH in
  its second line, with a tooltip on what drives them and which animals the water does not suit.
- **Active soil**: a substrate block (clay, mud and filter peat) for the interior floor: it pulls the pH towards 6.2,
  keeps the water soft, slowly takes up minerals and lets freshwater plants grow up to a quarter faster. It never wears
  out. Plain soil (dirt, mud, podzol ... `#bettacraft:acid_substrate`) works half as strongly.
- **CO2 dosing** (`features.co2`, `co2Dosing.*`): bio CO2 (sugar and yeast, about 4 days), the **CO2 cylinder** with a
  pressure regulator (0 to 10 bubbles/s), bubble counter and **solenoid valve** (closes when the light is off), and a
  **CO2 filling station** on a burning furnace, smoker, blast furnace or campfire (with a power option FE for the
  compressor; CO2 gas of other mods via `#bettacraft:co2_gas`). CO2 makes plants grow up to 2.5 times faster and lowers
  the pH; aeration drives it out; too much stresses and kills the animals.
- **Reverse osmosis unit** (`features.reverseOsmosis`, `osmosis.*`): runs without power (faster with FE), 1 part reverse
  osmosis water to 3 parts waste water, membrane and prefilter wear out. **Reverse osmosis water bucket**.
- Reverse osmosis water and sea water are **fluids** (`bettacraft:ro_water`, `bettacraft:sea_water`) for the tanks, pipes
  and storage of other mods; in the world, tanks, pipes and the reservoir they become water with their values.
- **Bee shrimp** (Caridina, `features.caridina`): Crystal Red and Crystal Black with banding grades A, S, SS and SSS,
  the Taiwan bee factor (Blue Bolt, Wine Red) and Orange Eye Blue Tiger; their own species, not crossable with the
  Neocaridina, soft water only. Rare in rivers (net and wild), sometimes at the wandering trader.
- **Sulawesi animals** (`features.sulawesi`): the cardinal shrimp and the Tylomelania snail (its own model, livebearing)
  for hard, warm water with a high pH. Rare in jungle water, sometimes at the wandering trader.
- **Reef chemistry** (`features.reefDosing`, `dosing.*`): alkalinity (KH), magnesium and trace elements in salt water
  next to the calcium; stony corals use KH and magnesium, low values slow them, trace elements give strong colours,
  the pH of salt water follows the alkalinity. The **dosing pump** doses four canisters (calcium, alkalinity,
  magnesium, trace elements; mL per day each) into a reef tank next to it or through pipes.
- Handbook pages in English and German: water chemistry, TDS meter and pH drop test, CO2 dosing, reverse osmosis,
  water as a fluid, active soil, bee shrimp, cardinal shrimp, Tylomelania, reef chemistry, dosing pump.

### Changed
- Topping off only with normal water lets the TDS climb for months; with regular partial water changes it settles a
  little above the normal water's (hard water leaves part of its minerals as lime scale when it evaporates).
- Texts say "normal water" (any water of the world but reverse osmosis and sea water) instead of "tap water".
- Old aquariums start with normal water values (and CO2, alkalinity and magnesium matching their salt); old sea water
  buckets give the sea water fluid.

## [1.17.0-beta.1] - 2026-11-14

Pipes, stainless steel and water changes: water flows through glass and stainless steel pipes with all its values, a
bulkhead brings it into the tank where interior pipes end in outlets with their own current, a multiblock external
filter of any size cleans with media that clog, nitrate makes water changes necessary, and a water change controller
and a top-off do them by themselves.

### Added
- **Stainless steel** from the new **crucible**: 3 iron ingots, coal, nether quartz and blaze powder melt into 3 ingots.
  Heat from lava (or a lava cauldron or magma block) right under it, FE while a power option is on, or fuel (lava bucket,
  blaze rod, blaze powder). Hoppers fill and empty it. Also made in the metallurgic infuser (Mekanism), alloy smelter
  (EnderIO), heated mixing (Create) and induction smelter (Thermal), only when those mods are installed. Tag
  `#c:ingots/stainless_steel`; the steel of other mods (`#c:ingots/steel`) is not accepted (`features.stainlessSteel`,
  `crucible.*`).
- **Glass pipes** (50 mB/t) and **stainless steel pipes** (250 mB/t) and the **water pump** (200 mB/t, power regulator,
  redstone stops it). The water keeps its values (temperature, salt, calcium, quality, nitrate) on its way; it uses the
  standard fluid handler, so the tanks and pipes of Mekanism, EnderIO, Create, AE2, RS and cauldrons join
  (`features.pipes`, `pipes.*`).
- **Bulkhead** in a floor block of a tank: its connection stands on the interior floor, and **interior pipes** lead from
  it to an **intake strainer**, **lily pipe**, **nozzle** or **spray bar**. A pipe under that floor block puts water in
  through the outlets and takes it out through the strainers; while water flows the outlets show their stream and their
  current acts where they sit (seahorses, corals, floating plants; fish swim into it now and then)
  (`features.interiorPipes`).
- **Multiblock external filter** in any size: 1 to 16 **filter chambers**, a **pump head**, **inlet** and **outlet**
  connectors. Filter media: **filter sponge** (12 water blocks), **ceramic rings** (24), **activated carbon** (16, used
  up), **filter peat** (6, softens the water once pH comes). Media clog with the stocking (sponges fastest, a clogged
  sponge also slows the flow); wash sponges and ceramic in a water cauldron. When inlet and outlet lead to the same tank
  it cleans that tank; a screen shows size, problems, flow and every medium (`features.multiblockFilter`,
  `multiblockFilter.*`). The old external filter stays the small variant.
- **Nitrate** as its own water value: it rises slowly with the animals even with a good filter; only water changes and
  plants lower it. `waterchange.difficulty`: OFF, EASY (25 % every 20 days), NORMAL (every 14 days, default), REALISTIC
  (every 7 days) for a normally stocked tank. From 50 mg/L the fish breed at half speed, algae grow faster and the log
  warns; from 120 mg/L the water is bad (sickness, deaths only after many weeks). Shown in the water tab of the tank menu
  with an explanation; evaporation concentrates it, topping off dilutes it back. Tanks of older versions start at 0.
- **Water change controller**: next to a tank it changes 5 to 50 % of the water every 1 to 60 days (screen; "change
  now"), with fresh water from a reservoir, pipe or other mod's tank or endlessly from a water source block next to it;
  it only drains what it can refill (`features.autoWaterChange`).
- **Top-off**: the **level sensor** next to a tank (or at a multiblock filter) tops off evaporated water from a
  **reservoir** (16 buckets), a pipe, another mod's tank (also AE2/RS) or a water source block. Fresh water only, so the
  salinity of a reef tank stays where it was (`features.topOff`).
- `pipes.pumpsRequireEnergy`: water pumps, filter pump heads, water change controllers and level sensors need FE.
- Handbook pages (en + de): crucible and stainless steel, pipes and water pump, bulkhead and interior pipes, multiblock
  filter, filter media, water change controller, top-off, nitrate; equipment, filters, water quality and settings
  updated.

### Changed
- The nitrate of the reef line in the water tab is now the real nitrate value (it followed the water quality before).

## [1.16.0-beta.1] - 2026-11-14

Aeration, oxygen and stronger lights: an air pump drives air stones and sponge filters in the tank, oxygen becomes a
water value, motor filters suck in fry unless an intake sponge is fitted, every device gets a power regulator, the light
comes in three tiers with a dimmer and a daylight timer, and the devices sound like themselves.

### Added
- **Air pump** (next to or on a tank, with its air hose into the water) and **air stones** in the interior: columns of
  fine bubbles rise to the surface, inside the tank and in the miniature. One pump drives 4 air stones or sponge filters
  at full strength. Without power mods it simply runs; with `aeration.airPumpRequiresEnergy` it needs 2 FE/t per air
  stone. Air hose as a crafting part.
- **Oxygen** (mg/L) as a water value: saturation falls with warm and salty water; the still surface, air stones, sponge
  filters, filter outlets and flow pumps exchange oxygen; the animals use it (more in warm water), plants make it in
  light and use some in the dark. Below 3.5 mg/L fish, shrimp and crayfish are stressed (they fall ill and can die;
  snails a little, frogs not at all); bettas and dwarf gouramis breathe air and only suffer below 1 mg/L. Low and
  critical oxygen go into the tank log. The amount of CO2 strong aeration drives out is shown and ready for CO2 dosing
  later (`features.oxygen`, `aeration.*`).
- **Sponge filter** (air-driven, needs an air pump, cleans 12 water blocks, never sucks in fry), **corner filter**
  (motor filter, 20 water blocks) and **intake sponge**: running motor filters (internal, corner, external, cabinet)
  without one suck in fry now and then (logged; `features.fryIntake`, `internalFilters.fryIntakePerDay`).
- **Power regulator** 0-100 % on filters, air stones, air pumps and flow pumps: a small screen on right-click, a button
  in the external filter and cabinet menus, sneak + mouse wheel everywhere. Less power cleans, moves and aerates less,
  needs less FE (filters by their setting) and is quieter; the jet gets shorter (`features.powerRegulator`).
- **LED panel** (one item lays a panel 2 blocks wide, panels in a row join) and **high-power LED** (joins on all four
  sides into a fixture of any size), both dyeable in 16 colours like the light bar. They add 2.5 and 5 to the light 15
  of the bar on the part of the tank they cover (`features.ledTiers`, `lights.*`).
- **Dimmer and daylight timer** for the lights of a tank (default 08:00 to 20:00 with an hour of sunrise and sunset):
  the lights dim (light level 15/12/9/6) or go off, and the plants get less light (`features.lightTimer`).
- New tab **Devices** in the tank menu: oxygen with bar and the reasons, air pumps and stones, CO2 driven out, motor
  filters without intake sponge, the lights with light strength, dimmer and timer buttons.
- **Own sounds** for filters, air pump (looping hum), air stones, sponge filter, flow pump and feeder (procedural,
  `tools/assetgen/sounds.py`, see SOUNDS-CREDITS.md); `sounds.useVanilla` switches back to Minecraft's sounds; client
  volumes per kind of device (`sound.filterVolume`, `airPumpVolume`, `airStoneVolume`, `flowPumpVolume`, `feederVolume`).
- Handbook pages (en + de): air pump, air stone, sponge filter, corner filter, intake sponge, power regulator, LED panel,
  high-power LED, oxygen; light, filters, power, tank menu and settings updated.

### Changed
- Stronger light matters: the deepest red of red plants now needs an LED panel or a high-power LED (under the light bar
  or daylight they are one step less red), and Monte Carlo and Alternanthera reineckii are strong-light plants that grow
  at full speed only under the stronger tiers (under the bar a bit more than half). With `features.ledTiers` off both
  behave as before.
- The feeder clicks when it feeds.

## [1.15.2-beta.1] - 2026-11-14

The build on GitHub is green again: GameTests that depend on chance failed there now and then. In the game only one
small thing changes: shrimp and crayfish no longer step into the side of a pillar under a table or overhang.

### Fixed
- Small animals under an overhang (a table, a cave): beside a pillar a crayfish could take a height inside the pillar
  for the ground and stand in the stone. The ground there is now the top of the whole pillar, too high to step onto.
- The GameTests of the guppy and endler breeding waited for a pregnancy with the full care step, so a pair below the
  shoal size of three fell sick from shoal stress after seven days and the waiting ended early: about one run in 250
  per test had no pregnancy and failed (once on GitHub Actions). They now wait up to 30 days with only the breeding
  step; the neon tetra spawning test waits up to 12 days instead of 4.
- The GameTest of the aquarium net in jungle water expected a fish from every swing, but since 1.15.1 a fifth of the
  catches there is a plant (3 of 15 local runs failed). It now runs without net plants.
- The shrimp breeding GameTest counted a young of an early backcross (red twice, blue once, so red) as a red x blue F1
  that must look wild, and failed now and then; it now checks only the young that carry red once, and waits up to 40
  days (was 20) for the first brood. The shrimp poor-water test counts only the shrimp put in, not young born late.
- The GameTest of the net in the different waters swings 60 times per water instead of 25 (a rarer species was
  sometimes not caught).
- The seahorse hitching post GameTest keeps the interior chunks loaded while it looks for the posts (they could
  unload in between, and an unloaded interior looks like an empty floor).
- The guppy inheritance GameTest grows up a son without a spontaneous mutation (0.4 % per allele and birth).
- The pink ramshorn GameTest runs without the spontaneous mutations of 1.15.0, like the shrimp colour test, and waits
  up to 100 days (was 40) for the first pink young.

### Changed
- The build workflow shows a failed GameTest with its message as an annotation on the run page and keeps the GameTest
  log in the uploaded logs.

## [1.15.1-beta.1] - 2026-11-14

No plant comes only from the wandering trader any more, the handbook shows every way to get a plant with its chance,
and the filter in a cabinet can send its hose up any side of the tank.

### Added
- **Plants from the aquarium net**: now and then the net brings up an aquarium plant from the bottom (a fifth of its
  catches in fitting water): in **rivers** Vallisneria, Monte Carlo, Java moss, Alternanthera and the Amazon sword, in
  **jungle water** the Amazon sword, Alternanthera, the red tiger lotus and Java moss, in **swamps** Monte Carlo,
  Alternanthera, the tiger lotus, the green and brown Cryptocoryne and, rarely, the "Flamingo"
  (`features.plantNetCatches`, `interiorPlants.netPlantShare`).
- **More plants from fishing**, rarely: the Amazon sword and the tiger lotus in jungles, Vallisneria and Monte Carlo in
  rivers, Alternanthera, the tiger lotus and the Cryptocoryne "Flamingo" in swamps.
- On a reef the net now and then also brings up a piece of the leather coral and the stony corals (hammer coral,
  lobophyllia, acropora).
- The plant tables of the net and of fishing are server config lists (`interiorPlants.netPlants*`,
  `interiorPlants.fishingPlants*`, entries `plant_id=weight`) for modpacks.
- **Outlet side of the cabinet filter**: the new button "Outlet: back / left / right / front" in the cabinet menu
  chooses the tank wall the hose of the filter in the cabinet goes up (each cabinet its own; back is the default). Hose,
  outlet, water jet, bubbles and the current follow it: floating plants drift away from the outlet, and right in front
  of an outlet the water moves almost twice as fast as in the rest of the tank, which stresses seahorses there sooner
  and gives corals there more flow. Broken and placed again, the cabinet keeps its side (shown in its tooltip); cabinets
  of older worlds keep the back wall (`features.cabinetOutletSide`).

### Changed
- The Amazon sword, Alternanthera, Vallisneria, Monte Carlo, the Cryptocoryne "Flamingo" and the red tiger lotus were
  only sold by the wandering trader; the trader still has them.
- Fishing in a mangrove swamp brings up the crypts and stem plants as before, the swamp water bottle its cuttings as
  before; the jungle and river tables keep the epiphytes and add the new plants after them.
- Handbook: the Obtaining section of every plant names every way with its place and chance, now also as "about 1 in N
  swings / catches"; the cabinet page explains the outlet side, the seahorse page the current in front of an outlet; the
  info texts (JEI, EMI and the shift tooltip) name the same sources as the data (a test checks it).
- A test over every plant, floating plant and coral fails if one of them comes only from the trader (also for future
  species).

## [1.15.0-beta.1] - 2026-11-14

Wild animals and corals in the world, rare beautiful wild colours, a magnifier for the hidden genes, single small
animals in the tank menu and buckets and bags with several animals.

### Added
- **Wild tankmates in the world**: tetras, corydoras, plecos and otocinclus in jungle water, rasboras, kuhli and clown
  loaches, the dwarf gourami and dwarf clawed frogs in swamps, guppies, endlers and platys in rivers, the reef fish and
  seahorses in warm and lukewarm oceans. They spawn rarely and in small shoals (only while fewer than six wild ones live
  within 32 blocks, never inside an aquarium) and despawn far from players like vanilla fish; caught ones stay
  (`features.wildFish`, `spawn.wildFishWeight`, `wildlife.wildFishNearby`).
- **Wild small animals**: shrimp, dwarf crayfish, snails, cleaner shrimp and hermit crabs live in the world as small
  animals that crawl over the bottom of rivers, swamps, mangroves, beaches, jungle water and the oceans. Right-click one
  with the aquarium net, a fish bag or a bucket of water to catch it; let it go again and it crawls off. In the world,
  in buckets, bags and aquariums it keeps everything: species, sex, genes, colour density, age, look and the analysis
  (`features.wildCritters`, `spawn.wildCritterWeight`, `wildlife.wildCrittersNearby`).
- **Wild corals**: every Bettacraft coral and the bubble tip anemone grow as big colonies on the warm ocean reefs among
  the vanilla corals (new chunks). Shears cut a frag off a colony, which grows back after a while; with silk touch the
  colony itself (`features.wildCorals`).
- **Beautiful wild colours**: most wild animals are wild-coloured, some carry a bred colour hidden, and about 3 in 100
  show one openly: red shrimp, albino bristlenoses, named guppy strains, domestic-looking bettas
  (`features.wildColors`, `wildlife.beautifulWildChance`).
- **Spontaneous mutations** for every animal: shrimp, crayfish, snails and the tankmates without their own genetics
  now mutate too (0.3 % per gene and birth; bettas 1 % and guppies and endlers 0.4 % as before; `features.mutations`,
  `wildlife.otherMutationChance`).
- **Magnifier** (glass pane, copper or gold, a stick; 64 uses, repaired with glass panes): the hidden genes of a new
  animal are unknown until it is analysed in an aquarium. Right-click an aquarium to analyse every animal in it, or the
  new **Analyse** button of the tank menu for one fish or small animal. The result stays with the animal (tank lists,
  other tanks, bucket and bag tooltips). Animals of older worlds count as analysed. Server config
  `wildlife.genesAlwaysVisible` shows every animal's genes without a magnifier (`features.magnifier`).
- **Single small animals in the tank menu**: click a kind to fold open every single shrimp, crayfish or snail with its
  sex, age, colour density and genes; take out exactly that one or analyse it. The quick take-out of a kind now picks
  the sex (any, males, females) and goes into a fish bag or a bucket (`features.animalList`).
- **Several animals per bucket and bag**, by their size (one species per container): a bucket holds 32 shrimp, snails,
  neons or endlers, 24 rasboras or guppies, 12 corydoras or platys, 6 plecos or dwarf gouramis, 2 bristlenoses or tangs
  and one betta, clown loach or frog; a fish bag half of a bucket (16 shrimp, 12 rasboras as before). The next animal
  of the species joins the container in the hand or in the inventory. Shrimp, crayfish and snails also go into a plain
  bucket of water (no fish bag needed). Old bags with more animals stay valid (`features.bigContainers`).
- Bucket and bag tooltips list every animal with its sex, stage, colour and (once analysed) its genes.

### Changed
- Taking an animal out of an aquarium with a bucket: an empty bucket takes 1000 mB of the tank's water as before; a
  **bucket of water** is used as it is and the tank keeps its water, but the water must suit the animal (plain water for
  freshwater animals, sea water for reef animals).
- The **bucket of sea water** works like a water bucket everywhere: it catches fish (vanilla and other mods' fish too,
  reef fish need it), and it is in `#c:buckets/water` for the recipes and machines of other mods.
- Small animals let go into the world (after the confirmation) now crawl off instead of being gone.
- The breeding prediction of a betta pair needs both parents analysed.
- Betta data schema v4 (the analysis flag; older bettas are migrated as analysed).
- Handbook: page Magnifier, wild animals and corals on "Catching animals" and "Corals", buckets with several animals, the
  single small animals in the tank menu; every new spawn and the shears on wild corals in the generated Obtaining
  sections and "Where to find what" (en + de).

## [1.14.3-beta.1] - 2026-11-14

The handbook is complete: chapters and sub-chapters that fold open, a page for every animal, plant, coral and device,
and where to get each of them, written from the game's own data.

### Added
- **Navigation at any depth**: the handbook's navigation shows chapters, sub-chapters and pages (GuideME itself only
  showed two levels, so most pages were only reachable through links or the item key). Every row with sub-pages folds
  open and closed; when a page opens, the chapters above it open and the navigation scrolls to it.
- **New structure** with twelve chapters: First steps, Aquarium, Equipment (with tools and containers), Bettas,
  Tankmates (tetras, rasboras, corydoras, plecos, loaches, livebearers as sub-chapters), Small animals (snails as a
  sub-chapter), Reef tanks (technique, corals, reef fish, reef invertebrates, seahorses), **Plants** (new chapter:
  epiphytes and moss, rosettes and bulbs, stem plants, runners and carpets, floating plants, care and cuttings), Food and
  live food, Interior and decoration, **Progress** (all advancements), Settings and other mods.
- **A page for every species**: all 28 tankmates and reef fish, the 16 small animals, the dwarf clawed frog, the 17
  aquarium plants, the 8 corals and the 4 floating plants, each with a picture, real name and origin, a profile (values
  from the game: temperature, water, shoal, stocking, breeding, light, flow, calcium, growth, cuttings), care notes and
  **Obtaining**: every way to get it with place and chance (aquarium net per biome, fishing, swamp water bottle, shears,
  shovel, block drops, spawns, wandering trader with count and price, breeding, cuttings, recipes).
- **Own pages for the devices and tools**: tank lid, aquarium net, fish bag and buckets, algae scraper, mirror stick,
  breeding box, protein skimmer, flow pump, reef rock and live rock, larva tub, artemia hatchery; every item and block
  opens its page with the guide key in JEI or EMI.
- **"Where to find what"**: one page with every catch of the net per biome, the fishing finds, the other finds and every
  offer of the wandering trader.
- Overview tables on every group page (species with their key values, linked).

### Changed
- The "Obtaining" sections, profiles, species tables, "Where to find what" and the advancements list are **generated
  from the game data** (`gradlew updateGuide`); a test fails when the book and the game differ. The fishing tables, the
  coral frags of the net and every wandering trader offer now live in data tables that the game and the handbook share
  (behaviour unchanged).
- The German handbook shows German pages after a language switch in the dev smoke test (the switch now reloads the
  resources like the language screen does).
- Settings page: the floating plant and aquarium plant options are listed.

## [1.14.2-beta.1] - 2026-11-14

A new look for every aquarium plant and coral, swaying water plants in the miniature, and every container empties
everywhere (animals only after a confirmation).

### Added
- **Every aquarium plant with real leaves**, inside the tank and in the miniature outside, swaying in the water: Java
  fern (long lanceolate fronds on a creeping rhizome), the four Bucephalandras (small wavy leaves, the Mini Needle with
  needles), Java moss (fine sprigs), the three Cryptocorynes and the Amazon sword (rosettes of leaves on petioles),
  Ludwigia and Alternanthera (stems with pairs of elliptic or narrow red leaves), Monte Carlo (tiny round leaves on
  runners) and the red tiger lotus (bulb, mottled leaves under water, floating leaves on long petioles). Every species
  has its own procedural leaf texture.
- **Every coral and reef animal with a real shape**: zoanthids (a colony of polyps on columns over a thin mat) and green
  star polyps (purple mat, green stars) that **close for a moment when a fish swims by** (or a player inside), mushroom
  corals whose discs slowly rise and sink, the leather coral (rigid trunk, cap with a waving polyp lawn), Acropora
  (rigid branches, thicker towards the base, pale tips, small waving polyps), Lobophyllia (a fleshy dome with its ridges
  in the polyp colour), the hammer coral (stony skeleton with pulsing, hammer-tipped tentacles) and the bubble tip
  anemone (foot, oral disc, swaying and pulsing tentacles with bubble tips).
- **Kelp, seagrass, corals and coral fans sway in the miniature** outside the tank as well (they stood still at 1/16):
  tag `#bettacraft:swaying_plants` for modpacks and mods, and other mods' underwater plants are found automatically
  (`swaying.autoDetect`, exclusion list `swaying.exclude`); their own models and textures, swayed while drawn. Switch
  `features.swayingForeignPlants`.
- **Client settings** `render.plantMeshes` and `render.swayingPlants` (Mod settings in the game): every player chooses
  the new or the old plant look and the swaying for themselves, also on a server; `features.plantMeshes` stays the
  switch for the whole pack.
- **Confirmation before animals go free**: an animal bucket or a filled fish or critter bag used on the world opens a
  small screen with "Cancel" and "Empty"; only "Empty" lets the animals go. Putting them into a tank needs no
  confirmation.

### Changed
- **Every container empties everywhere**: the sea water bucket pours into the sea and other water surfaces (the bug:
  from the shore or a boat it found no bottom in reach, like a vanilla bucket, and did nothing), into a cauldron, from a
  dispenser and through the fluid capability into other mods' tanks and pipes. Animal buckets let their animal go into
  a water surface too. Bottles of salt water, swamp water, infusoria and artemia nauplii pour out anywhere with sneak +
  right-click (or right-click into the air): splashes and a sound, an empty glass bottle, no water block; in tanks they
  work as before. An empty fish bag can be shaken out with sneak + right-click.
- **Vallisneria** grown from now on keeps its ribbons apart (they no longer share blocks and cross at the top);
  existing plants keep their shape.
- Ludwigia and Alternanthera: the outer part of a big leaf hangs on its stem node (no little stems going up and down).

## [1.14.1-beta.1] - 2026-11-14

Seahorses (`features.seahorses`), stacking artemia cyst jars, a visible clear glass tank icon.

### Added
- **Seahorses** for reef tanks: the **lined seahorse** (*Hippocampus erectus*; dark, yellow or orange) and the **dwarf
  seahorse** (*Hippocampus zosterae*), own upright model with a curling tail, a fluttering dorsal fin and the male's
  brood pouch. They swim slowly and spend most of the time **holding on to a hitching post** with their tail:
  aquarium plants, corals, decoration, vanilla seagrass and kelp that stick up in the tank interior. Without a post
  they lie on the bottom and are stressed; **strong flow** (pumps, strong filters; `seahorses.maxFlow`) stresses them
  too. They eat **only live food** (mosquito larvae, artemia nauplii; the fry artemia from birth), need it **twice a
  day** (`seahorses.feedingsPerDay`) and are slow eaters next to fast fish. Tangs, damselfish, chromis and clownfish
  harass them. Sea water and strong brackish water (20-42 ‰); the lined seahorse likes it cooler (18-27 °C).
- **Brood pouch**: a fit pair breeds at 21-25 °C (dwarf 24-27 °C): the female lays her eggs into the male's pouch, his
  belly swells, and after `seahorses.pouchDays` (4) he gives birth to the fry. Menu: holds on to a post, current too
  strong, nothing to hold on to, brood pouch full; breeding tab with the hitching posts and full pouches.
- Seahorses from the aquarium net in lukewarm and warm oceans and, rarely, from the wandering trader; seahorse bucket
  icon, JEI care sheets, handbook page "Seahorses" (en, de), showcase row.
- Config `seahorses.*` (maxFlow, needHitchingPosts, feedingsPerDay, foodShareWithFastFish, harassedByFastFish,
  pouchDays) and `features.seahorses`, en and de labels.

### Changed
- **Artemia cyst jars stack**: 16 per stack by default, 32 or 64 with `food.artemiaCystStackSize` (server config, sent
  to the clients at login and when the config is reloaded). Every jar keeps its 16 uses; only jars with the same uses
  left stack together. Crafting a hatchery from a stack takes one jar off it and gives it back with one use less; the
  artemia hatchery opens one jar of a stack at a time and uses it up first (breaking it drops the open jar). Jars of
  older worlds keep their uses. A smaller new stack size leaves bigger stacks as they are.
- The **clear glass tank item** shows a faint tinted pane with highlights and a one-pixel frame; it was almost
  invisible in the inventory. The placed block is unchanged.

## [1.14.0-beta.1] - 2026-11-14

Build pipeline and releases. No gameplay changes.

### Changed
- The released jar no longer contains the development code (smoke tests, the showcase world builder, GameTests and
  their template): about 100 classes less. It now contains the licence text (`LICENSE_bettacraft`).

### Development
- Development-only code moved into its own source set `src/dev`, loaded by every `gradlew run*` task (also
  `buildShowcase`, `smoketestUpgrade`, `runMultiplayerTest`) but not packed into the jar.
- `gradlew verifyReleaseJar` (part of `check`): only Bettacraft classes, `assets/bettacraft`, `data/bettacraft`, tags of
  other namespaces and the NeoForge loot modifier list; no dev, smoke or GameTest classes, no tool sources, no file over
  2 MB; every mixin of the mixin config present; the version in `neoforge.mods.toml` is `mod_version` and, with
  `-PexpectedVersion`, the tag. `gradlew releaseNotes` writes the top section of this changelog to
  `build/release-notes.md` and fails if it is not the section of `mod_version`.
- GitHub Actions: the build workflow runs on pushes to `master` and pull requests (build, unit tests, jar check,
  release notes, `runData` unchanged, GameTests, one server smoke test); the full set (smoke without optional mods and
  with The One Probe and WTHIT, GameTests with the power mods) runs by hand and before every release. The power mods
  come from Modrinth at pinned versions with SHA-512 checks (`tools/ci/power-mods.json`, `tools/ci/fetch_mods.py`).
- The release workflow for `v*` tags: the tag must match `mod_version`, the jar is checked, the release notes come from
  this changelog, a GitHub release (a prerelease for betas) gets the jar and its SHA-256; Modrinth and CurseForge
  (channel beta for betas) only once their token and project id are set.

## [1.13.4-beta.1] - 2026-11-14

Stone cabinets (`features.stoneCabinets`).

### Added
- **Aquarium cabinets of stone**: `SSS / PCP / PPP` with five blocks of one stone (P), three **slabs of the same stone**
  (S) and a wooden chest (C). Every stone with its own slabs works - stone, cobblestone, granite, diorite, andesite and
  their polished forms, deepslate, tuff, blackstone, sandstone, bricks, nether bricks, quartz, terracotta of mods with
  terracotta slabs and the stones of other mods: the slab is found through the game's own recipes (crafting or
  stonecutter), stones of other mods are recognised by the tag `#bettacraft:cabinet_stones` (modpacks can add to it) or
  by being mined with a pickaxe and sounding like stone. Not: metal and copper blocks, glass, purpur, end stone,
  prismarine, concrete, mud bricks, ores (`#bettacraft:cabinet_excluded`).
- The cabinet takes the look of its stone like a wooden one takes its planks, is named after it ("Stone Bricks Aquarium
  Cabinet") and is mined like it: pickaxe (by hand only the contents drop), hardness, blast resistance and sound of the
  stone. Otherwise it is the same cabinet (filter, heaters, storage, joined counters).
- JEI shows the recipe with stone, slabs and cabinet cycling together; creative tab and handbook (en, de) show a
  selection of stones.

### Changed
- Wooden cabinets are unchanged; cabinets of older worlds load as before.

### Development
- GameTests: the slab of every vanilla stone from the recipes, cabinets of stone, cobblestone, stone bricks, deepslate,
  sandstone, bricks, nether bricks, quartz and the stones of other mods in the dev runtime; none of iron, gold, copper
  (also with copper slabs), glass, purpur, end stone, prismarine, mud bricks, concrete, terracotta without slabs, mixed
  stones or slabs of another stone; the stone survives placing and breaking, pickaxe, hardness, blast resistance and
  sound; the switch. Client smoke `-Psmoketestm21_7`, a showcase row of stone cabinets.
- The M19 GameTest `livebearersBearYoungIntoTheBreedingBox` waits up to 15 days for a pregnancy (was flaky); the upgrade
  test accepts small animals born while the world loads (every old one must still be there).

## [1.13.3-beta.1] - 2026-11-14

Guppy and endler genetics: real genes per fish instead of fixed varieties (`features.livebearerGenetics`).

### Added
- **Guppy and endler genetics**, modelled on real guppy genetics: every guppy and endler has its own genes for the
  **tail shape** (delta, triangle, double sword, top and bottom sword, lyretail, spade, round, flag), the **body
  colour** (wild grey, gold, albino, blond, pink white, blue, black; each its own recessive gene, dark is dominant), the
  **tail colour** (red, blue, yellow, green, black, white, orange) and the **tail pattern** (mosaic, grass, leopard,
  spots). The **patterns** sit on the sex chromosomes: Cobra, Tuxedo, Moscow on the X and the Y, Japan Blue, Red Tail
  and Red Head and the wild spotting on the Y - **Y patterns go only from the father to his sons**. The young inherit at
  birth (one allele of each parent; the sex is decided first). Females are grey with small tails but carry every gene.
- Endlers have their own alleles (peacock, tiger, black bar, japan blue on the Y, swords, orange or yellow tails with
  coloured edges) and **cross with guppies**: the young are **hybrids** of the mother's species, between the two in
  size and look, with the endler share of their ancestry.
- **28 named strains** like the betta varieties: Red, Green and Yellow Cobra, Blue, Green and Black Moscow, Full Gold,
  Tuxedo Koi, Half Black Red and Blue, Japan Blue, Full Red, Albino Full Red, Red and Blue Mosaic, Blue and Yellow
  Grass, Leopard, Platinum White, Full Black, Red Double Sword, Lyretail, Wild Guppy, Endler Wild Type, Tiger, Black
  Bar, Japan Blue and Yellow Tail. Advancements **Line Breeder** (a bred strain), **Guppy Collection** (every strain,
  with progress) and **Crossbreed** (a hybrid).
- The look is composited from one greyscale mask per gene (generated in `tools/assetgen/guppies.py`) on a new guppy
  model with one part per tail shape; works with shader packs like the bettas.
- Tank menu: the strain on the animals tab, every gene in the tooltip (shown and hidden allele, X and Y, endler share);
  bucket and bag tooltips name the strain; Jade / The One Probe / WTHIT list it; JEI care sheets explain the genetics.
- The wandering trader sells wild-caught guppies and endlers and pure-bred strains; the aquarium net brings wild fish
  (some carry a bred allele hidden) and now and then feral fancy guppies. The creative tab has a pair of every strain.
- Handbook page **Guppy Genetics** (en, de) with the genes, the X and Y, the tail shapes, every strain and the hybrids.

### Changed
- Guppies and endlers of older worlds keep their look: the old varieties become matching genomes when loaded (Wild ->
  Wild Guppy, Red -> Full Red, Blue Grass, Green Cobra, Tuxedo -> Half Black Red; endler Wild, Tiger, Black Bar). The
  variety alleles are kept and still inherited: with `features.livebearerGenetics` off the old fixed varieties show.

### Development
- Unit tests for the inheritance (dominance, Mendel ratios, Y only to sons, X zig-zag, hybrids, strains, old
  varieties, NBT); GameTests for births (Red Cobra x Full Gold, endler x guppy), the conversion on load, the trader and
  the switch; client smoke `-Psmoketestm21_6`; the upgrade test checks the conversion of every old variety and that the
  genomes survive save and reload.

## [1.13.2-beta.1] - 2026-11-14

Polish after the showcase tour: smoother fish, the green neon, more snail colours, break protection, a clip-on feeder and
bigger decoration. Every new part has its own switch in the common config (`features.*`).

### Added
- **Green Neon Tetra** (*Paracheirodon simulans*), also sold as Blue Neon: a tetra of its own with a green (wild) or blue
  stripe over almost the whole body and hardly any red; blue is recessive. Trader, aquarium net in jungle water,
  handbook page, JEI care sheet (`features.greenNeonTetra`).
- **Break protection** in the tank menu (owner and operators only, off for new tanks): while it is on nobody can break
  the aquarium, not even the owner or in creative mode - the glass, lid, light and feeders on it, the clip-on feeder and
  the cabinet under it. Explosions and pistons leave it alone too (server config `tank.protectFromExplosionsAndPistons`,
  on by default) (`features.breakProtection`).
- **Clip-on feeder**: a small feeder that hangs on the side of the glass at the top edge, for tanks whose top is taken by
  the light. One stack of food, 1-3 feedings a day (right-click with an empty hand), no hopper, no evaporation cover.
  Recipe, handbook page, JEI (`features.clipOnFeeder`).
- **Snail colours** (`features.snailColors`): the red racer nerite and the new **yellow racer nerite** get an individual
  shade each (orange, red, deep red, red with yellow lines; yellow, golden, yellow with dark lines; brightness and line
  spacing vary), named in the bag tooltip. Apple snails are mostly **gold** now (a stronger yellow) at the trader and in
  the net, wild brown is rare; every one has its own brightness, gold and wild ones sometimes dark bands. Ramshorn snails
  get a fourth colour, **red**. Snails of older worlds keep their look.
- **Detailed dino skull and shipwreck**, about twice as big: the skull with eye sockets, nostrils, teeth and an open lower
  jaw, the wreck with a planked hull, bowsprit, stern castle, a hole in its side and a broken mast with its yard, lying
  tilted. Placed ones stay as they are (`features.detailedDecor`; off: the old small shapes).
- Handbook start page: "Bettacraft by 0verEngineer" (the byline smaller and grey).

### Changed
- **Smooth swimming** (`features.smoothSwimming`): the fish tilt and right themselves smoothly instead of flipping above
  a speed threshold, the tail beat no longer jumps when the speed changes, the body follows the turns smoothly
  (tankmates, bettas and the dwarf frog); tankmates turn in curves and keep some speed while turning. Kuhli loaches
  undulate slowly like eels, glide calmly along the bottom and lie still or hide under the decoration by day.
- Real trade names: **Pacific Cleaner Shrimp** (Weißband-Putzergarnele, *Lysmata amboinensis*) and **Mexican Turbo
  Snail** (ids unchanged).
- The showcase: every tank stands on a cabinet with an external filter (the wood changes row by row), power is off in
  its config, every tank has the break protection on, a row per colour of the small animals and one of the tankmate
  colour forms.

### Fixed
- Plecos and otocinclus stood upright in the middle of the water on their way to the glass; they now turn upright
  smoothly only once they are there.
- Cabinets showed pink, purple or blue edges (the overhang of the counter top reached past the planks texture into
  neighbouring textures of the atlas).

### Development
- Upgrade test from 1.13.1-beta.1: `bash tools/upgrade/build_reference.sh` builds a reference world with the old version
  and a manifest of everything in it; `gradlew smoketestUpgrade` loads a copy twice and compares every tank, animal,
  small animal, interior, piece of equipment and chest item, and fails on errors in the log.
- Client smoke `-Psmoketestm21_5`; GameTests for the glass cleaners, the break protection, the clip-on feeder and the
  decoration; the bucket variants A and C are gone (B stays).

## [1.13.1-beta.1] - 2026-11-14

Real plant leaves in the miniature too, and fixes to the new plant look.

### Added
- **Plant leaves outside**: the miniature outside the aquarium now shows Rotala, Vallisneria and Anubias with the same
  real leaves as the interior (scaled down, swaying the same way) instead of their blocks; the leaves are a little
  larger and stronger outside and use mipmapped textures, so they do not flicker. Further away than 24 blocks the
  miniature shows the blocks again (client config `render.miniaturePlantMeshDistance`, 0 = always blocks). Rebuilt only
  when the plant changes; with a shader pack the leaves outside cast shadows too.

### Fixed
- Rotala: side shoots and shoots next to the main stem had leaves floating in the water without a stem; every shoot is
  now worked out as a chain (also through leaves of a neighbouring shoot and back to the main shoot), every leaf sits on
  a stem.
- Vallisneria: the part of a leaf running along the surface was drawn as a separate piece in the air; every leaf is now
  one continuous ribbon from the ground round the bend and along the surface, and sways as one.
- Floating plants (lily pads and the others) flickered where they overlapped (z-fighting): every plant of an aquarium
  now lies at a height of its own, a few thousandths of a block apart; a lily pad put onto another one slides a little
  aside.
- GameTest `smallAnimalsWalkUnderAnOverhang` failed now and then: its stone check did not map positions like the
  miniature does.

### Development
- `gradlew runClient -Psmoketestm21_1`: the M21.1 scene (plants outside near and far, LOD, shear cut, shadows with
  `-Pshaders`, Rotala and Vallisneria inside, floating plants from above); `-Pshowcasebench`: frame times of the plant
  meshes outside in the showcase world.

## [1.13.0-beta.1] - 2026-11-14

Fixes, path finding in the interior, glass fish buckets and a first look at real plant leaves.

### Added
- **Path finding in the interior**: dwarf clawed frogs find their way around the decoration (rocks, roots, plants,
  caves) instead of walking through it; shrimp, crayfish and snails walk under overhangs and into caves of the
  decoration and back out; a pleco male swims through the mouth into his spawning cave instead of stopping at it. A*
  on the interior blocks in cells of two tank pixels. Switch: `features.interiorPathfinding` (common config).
- **New plant look (prototype)**: Rotala, Vallisneria and Anubias are drawn with real leaves inside the interior (round
  leaves on thin stems, long ribbons, broad oval leaves on a rhizome); Rotala and Vallisneria sway in the water like
  kelp. The blocks of the plant stay (shears, breaking, growth and the miniature work as before), the miniature keeps
  the block look. Works with Sodium and Iris shader packs, and the leaves cast shadows with a shader pack. A plant is
  drawn anew as soon as one of its blocks changes (shears, growth, breaking), in the next frame; nothing is rebuilt on
  a timer. Switch: `features.plantMeshes` (common config).
- **Config `compat.overlayProvider`** (common config): AUTO (default) gives the Bettacraft lines only to the first
  installed overlay mod of Jade, WTHIT and The One Probe, JADE / TOP / WTHIT pick one, ALL shows them in every one.
- **Showcase world** for development: `gradlew buildShowcase` builds the world "Bettacraft Schaukasten" with every
  betta variety, tankmate, small animal, plant, floating plant, coral, reef animal, decoration, the equipment, the 16
  light colours, the glass types and shapes, a reef and a brackish tank, every tank named on its glass (no signs);
  `gradlew runClient -Pshowcasetour` takes screenshots of it.

### Changed
- **Fish bucket icons**: the fish now swims in a glass bucket of water (smaller, fully inside, with a water line and a
  reflection) instead of sticking out of an iron bucket; also the tankmate and frog buckets.

### Fixed
- With two overlay mods (e.g. Jade and The One Probe) every aquarium, filter and betta was shown twice.

### Development
- The dev client starts with Jade only; The One Probe and WTHIT come with `-Ptop` / `-Pwthit` (`-PnoTop` and
  `-PnoWthit` still work). The CI server smoke test runs with both.

## [1.12.0-beta.1] - 2026-11-14

Reef tanks: salt water, corals and the animals of the coral reef.

### Added
- **Salt water**: a tank becomes a reef tank with **sea water** - an empty bucket filled in an ocean or at a beach gives
  a bucket of sea water (`saltwater.oceanWaterIsSalty`), or salt the water with **sea salt** (right-click; sneaking adds
  all it needs for 35 ppt). The tank mixes by volume (sea and fresh water give brackish water), evaporation leaves the
  salt behind (top up with fresh water), draining a reef tank fills buckets of sea water. Salt water is drawn a clearer
  turquoise. The menu shows salinity, calcium and nitrate; Jade, The One Probe and WTHIT show the salt.
- **Animals live in their water**: bettas, the tankmates, shrimp, crayfish, most snails and the dwarf clawed frogs need
  fresh water; guppies, endlers and platys tolerate brackish water up to 18 ppt, nerites up to 30 ppt. The wrong water
  gives a salt shock (a freshwater fish in sea water dies within hours, a little too much salt only stresses), and the
  tank refuses an animal whose water would kill it (`saltwater.salinityShock`, `saltwater.refuseDeadlyWater`). Animals
  of vanilla and other mods do not care by default; `saltwater.foreignAnimals = REALISTIC` makes cod, tropical fish,
  pufferfish and squid need salt water and axolotls and tadpoles fresh water.
- **Reef technique**: the **protein skimmer** hangs on the tank like the external filter and cleans salt water (no
  effect in fresh water); the **flow pump** stands in the interior and moves the water for the corals; **reef rock** and
  **live rock** (cleans salt water without power) for the rock work; **actinic light**: blue, light blue, cyan or purple
  aquarium lights mixed with white give the corals the most light and make them glow; **calcium additive**.
- **Corals** grow on reef rock in the interior like the aquarium plants: zoanthids, green star polyps, mushroom coral and
  leather coral (soft), hammer coral, Lobophyllia and Acropora (stony: they use up calcium and need clean water), and the
  bubble tip anemone. They need salt water, light, flow and 23-29 °C, bleach in the wrong water or the dark and glow
  under actinic light; frags with shears, from vanilla corals (crafted with reef rock), the net on a reef and the
  wandering trader. Optionally vanilla corals die in fresh water (`saltwater.vanillaCoralsNeedSalt`, off).
- **Reef animals**: ocellaris clownfish (orange and black), yellow tang, yellow watchman goby, green chromis and
  yellowtail damsel as fish with their own models; cleaner shrimp (the fish heal faster), blue-leg hermit crabs and
  turbo snails as small animals. **Clownfish** move into their anemone and a pair lays its eggs at the anemone's foot,
  the male guards them; the other reef animals do not breed in a tank. Caught with the aquarium net in warm and
  lukewarm oceans (hermit crabs also at beaches) or bought from the wandering trader.
- Handbook section "Reef tanks" (English, German): salt water, technique, corals, reef animals; JEI information; settings
  `saltwater.*` with English and German labels.

### Changed
- Tank data schema 10 (salinity, calcium); every older tank is a freshwater tank and stays as it was.
- Aquarium plants stop growing in brackish and salt water.

## [1.11.0-beta.1] - 2026-11-14

Tankmates: twenty popular aquarium fish, each with its own way of breeding.

### Added
- **Tankmate fish** as real animals like the bettas: neon, cardinal, ember and glowlight tetras; harlequin and mosquito
  rasboras (*Boraras brigittae*, also sold as chili rasbora); pygmy, panda, sterbai and green gold corydoras
  (*C. melanotaenia*); the L134 leopard frog pleco, the L046 zebra pleco and the bristlenose (also albino and super red);
  otocinclus; kuhli and clown loaches; guppies, endlers and platys in several colour varieties; the dwarf gourami. Own
  models and textures, scaled to their real size. They swim in the zone of the water their species likes, shoal with
  their kind, forage and rest on the bottom, plecos and otocinclus stick to the glass, a pleco looks out of its cave.
- **Care like the bettas**: hunger, a temperature range per species, water quality per species, and stress - a shoal
  smaller than the species' minimum, overcrowding, being chased. The menu shows each fish with its shoal, temperature
  range, stress and breeding state; Jade, The One Probe and WTHIT list the tankmates, their eggs and the breeding box.
- **Breeding per species** (accelerated game time, settings in `tankmates.breeding`): corydoras glue eggs to the glass
  after a **cool water change** (fresh water now has the ambient temperature and cools a heated tank); plecos spawn in
  a **spawning cave** and the male guards the clutch; tetras, rasboras and kuhli loaches **scatter eggs** into
  fine-leaved plants or a **spawning mop** (harlequins under broad leaves) and the adults eat what they reach;
  guppies, endlers and platys bear **live young** (into a **breeding box** if one hangs in the tank); the dwarf gourami
  builds a **foam nest** like a betta. Clown loaches do not breed. Colour varieties are inherited (wild type dominant).
- **Getting along**: male bettas chase male guppies and male dwarf gouramis, gourami males fight each other, pleco males
  without a cave are chased off, every adult hunts fry and eats eggs (plants, moss, floating plants and the spawning
  mop protect them), clown loaches eat snails, otocinclus and bristlenoses graze the algae on the glass.
- **Items**: breeding box, spawning cave and spawning mop (decoration and their blocks), fry powder for the fry of every
  species, algae wafers that only the bottom fish eat. A bucket and a spawn egg per species; one fish bag holds a whole
  shoal. Eggs are taken out with the aquarium net into a fish bag and raised in another tank.
- **Getting them**: the aquarium net in jungle water (South American species), swamps (South-East Asian species) and
  rivers (livebearers, bristlenoses); the wandering trader sells shoals and pairs.
- Handbook section "Tankmates" (English, German) with a page per species, care and breeding; JEI information per
  species; settings `tankmates.*` with English and German labels.

### Changed
- Tank data schema 9 (clutches, breeding box, water change history); older tanks are converted on load.
- A water change cools a heated tank a little (`tankmates.breeding.waterChangeCools`, on by default).
- Infusoria and the new fry powder feed the fry of every species; tankmate eggs and young keep a tank from being broken.

## [1.10.0-beta.1] - 2026-11-14

Young fish in the stocking and coloured aquarium lights.

### Added
- **Coloured aquarium lights:** the light comes in the 16 dye colours. Right-click a light with a dye to colour the
  whole bar (one dye), or craft a light with a dye (eight lights around a dye give eight). The colour tints the water
  of the aquarium under it (several colours mix, white makes it paler) and the panel glows in it; black is a violet
  "black light". Optics only: the light level stays 15 and plants, the red root floater and algae get the same light.
  Lights drop and pick-block in their colour. How strongly the water is tinted is a client setting
  (`render.lightTint`, 0.6). With a shader pack (Iris) the water stays tinted, and packs with coloured block light
  (Complementary "Colored Lighting") light the surroundings in the colour of the lamp.
- **Young animals count in fractions for the stocking:** fry 0.02, juveniles 0.1, subadults 0.3 of a fish, eggs and
  larvae 0 (server settings `stocking.eggStocking`, `larvaStocking`, `fryStocking`, `juvenileStocking`,
  `subadultStocking`). Young animals of other mods count like a juvenile, young shrimp, crayfish and snails at the
  juvenile share of their own share. Hover over the animals line of the menu overview for the parts; the animals tab
  shows how much each young betta counts.

### Changed
- A juvenile or fry no longer fits into an aquarium that is full (at `tank.hardCapPerWaterBlock`). Only adults get
  stressed by overcrowding, as before.
- Existing lights stay white; nothing to convert.

## [1.9.0-beta.1] - 2026-11-14

Hardening release: a review of the whole code, with a test for every bug found.

### Added
- **More settings** (server): `inhabitants.outgrown` (an animal of another mod that grew too big in an aquarium waits
  in a fish bag in front of it; `KEEP` = it stays), `health.hungryAfterDays`, `temperature.toleratedMin` /
  `toleratedMax`, `breeding.conditioningDays`, `frogs.frogStarveDays`, `interior.preparationBudgetMs`; (client)
  `render.tankDetailDistance` (name sign, nest, floating plants, small animals and algae). English and German labels,
  handbook page "Settings".

### Changed
- The menu colour of the water temperature and the "ideal temperature" advancement use the breeding range
  (`breeding.minTemp` / `maxTemp`) instead of a fixed 26-28 °C.
- `foodBreeding.larvaTubYield` and `hatcheryYield` go up to 16 (the output slot holds 16); a batch only starts when it
  fits completely.
- Less network traffic: the bubble nest, the small animals and the floating plants are only sent to nearby players when
  something visible changed; a refused menu action sends nothing.

### Fixed
- Substrate taken out of the interior floor came back after the aquarium was extended or split and could be taken out
  again (item duplication).
- A tank whose data could not be read (a broken inhabitant id) was lost and its other blocks never formed a tank
  again; corrupt numbers (NaN, negative days) in tanks, bettas and small animals are repaired when loading.
- Joining a small aquarium with eggs to a bigger one deleted the eggs.
- A client could make the server send the whole interior again every tick, and make it load chunks.
- Leaving and entering worlds or interiors kept old client worlds in memory.
- Machines that break blocks for a player (fake players) could break an aquarium while a player was in its interior; a
  player who died inside kept it locked; the lock and trusted building also work while the aquarium's chunk would
  otherwise unload.
- Very large interiors could stay at "preparing ... %" for ever; players who walked away are no longer pulled in.
- Furnaces, hoppers and machines in the interior stopped working after the aquarium was extended or split.
- With `simulateUnloadedTime`, an empty feeder still fed the fish.
- Two heater or filter rods side by side in the interior were removed together but only one came back.
- A fish bag or a bucket was emptied when its animal could not be created; Better Fish Tanks tanks keep the name and
  data of a bagged squid.
- A name typed in the tank menu just before closing it (or resizing the window) was lost.
- Animals of other mods that grew past `inhabitants.maxHitbox` inside an aquarium stayed there.

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
