# Changelog

All notable changes to Bettacraft. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versions follow [Semantic Versioning](https://semver.org/). The release workflow publishes the section of the tagged
version as release notes.

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
