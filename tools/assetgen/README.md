# Bettacraft asset pipeline

Everything here is procedural and deterministic, with fixed seeds and no external textures.
Regenerate it all from the repo root with:

```
python -m pip install --user -r tools/assetgen/requirements.txt
python tools/assetgen/build.py
```

The generated PNGs under `src/main/resources` are committed, so the Gradle build does not need Python.
`tools/assetgen/build/` is gitignored and only holds debug and preview output. The review sheet
is copied to `builds/m5-types.png`.

## Scripts

| Script | Makes |
|---|---|
| `model_defs.py` | The single source of truth for the betta model: cuboids per tail shape (M5: round, spade, veil, delta, halfmoon, rosetail, crowntail, doubletail; dumbo = second pectoral set; short-finned plakat types reuse the mesh with `SHORT_*` scales of caudal/dorsal/anal), the shared 64x64 box-UV layout (auto-packed), life-stage scales and female scales. Also writes `BettaModelDefinitions.java` (compiled copy in `src/main/java/.../client/model/`, reference copy in `build/generated/`; includes `PATTERNS`), `betta_<shape>.bbmodel` (Blockbench, modded entity) and `uv_layout.txt`. |
| `fins.py` | Fin silhouettes (alpha cut-outs: crowntail spikes, doubletail notch), fin-ray field, distance-to-edge field. |
| `uv_template.py` | Per-texel metadata (part, face, model-space position, fin coordinates). Writes `build/uv_template.png` (debug) and `region_body.png` / `region_fins.png`. |
| `patterns.py` | `shade.png`, `pattern_*.png` (butterfly, marble, dragon, koi, mask, tipped, fade, grizzle, bars, M5: piebald, dalmatian, galaxy), `iridescence.png`, `eye.png`. |
| `palette.py` | 43 named colours (id, en_us, de_de, hex) -> `build/generated/colors.json` and `src/main/java/.../genetics/BettaColorTable.java` (built-in defaults; the datagen writes them as `data/bettacraft/bettacraft/colors/*.json`). |
| `varieties.py` | M5: the named varieties (Mustard Gas, Black Orchid, Koi Galaxy, ...): phenotype rules, priority, en/de name and an example fish -> `genetics/BettaVarietyTable.java` (datagen: `data/bettacraft/bettacraft/varieties/*.json`). Checks that every example is named by its own rule. |
| `lang_de.py` | Writes the generated keys of `de_de.json` (colour, tail and variety names) and keeps every hand-written key. |
| `decor_icons.py` | M5: hand-drawn 16x16 icons of the decoration items and the internal filter (replace the blurry isometric renders of M3). |
| `icons.py` | 16x16 item icons (incl. `swamp_water_bottle`) plus greyscale fish overlay layers for the bucket and the bag. |
| `blocks.py` | Tank block textures, including the animated greyscale water; feeder (`feeder_side/top/bottom`, design in rows 8..15 / uv 3..13) and tank light (`tank_light`, `tank_light_panel_on/off`). |
| `breeding.py` | M2: fry food icons (`salt_water_bottle`, `infusoria_culture`, `infusoria`, `artemia_culture`, `artemia_nauplii`, `artemia_cysts`) and the bubble nest textures `entity/nest/bubble_nest`, `nest_eggs`, `nest_larvae` (32x32, drawn as flat discs at the water surface). |
| `decor.py` | M3: the decoration structures `data/bettacraft/structure/decor/*.nbt` (vanilla blocks only, footprints 1x1 to 8x8, written with a tiny NBT writer, gzip mtime 0 so the files stay byte-identical), the interior equipment textures `heater_segment_*` / `filter_segment_*` (anchor, middle, top) and an isometric debug sheet of the structures `build/preview/decor_sheet.png` (the item icons moved to `decor_icons.py` in M5). |
| `equipment.py` | M4: external filter canister (`external_filter_side/top/bottom`, side rows 4..15 = body, 2..3 = lid), cabinet placeholder planks `cabinet_wood` (swapped at runtime for the planks of the cabinet) and `cabinet_trim`, the internal filter outlet face `filter_segment_outlet`, and the entity textures `entity/filter/hose.png` (ribs repeat along u) and `entity/filter/jet.png` (16x64 water ribbon, seamless along v; the renderer scrolls v to animate the flow). |
| `aquatic_plants.py` | M13: grey block textures of the aquarium plants (`plant_leaf_*`, `plant_stem`, `plant_rhizome`, `plant_ribbon`, `plant_pad`, `plant_mat`, `plant_moss`, `plant_cutting`), tinted at runtime per species, part and redness, and two-layer grey item icons per growth form (`item/plant_<form>` and `_detail`). Leaves are dense so a plant block (one pixel in the miniature) does not vanish in the mipmaps. |
| `critters.py` | M14: the models of the small tank animals (shrimp, crayfish, nerite, apple snail, ramshorn, bladder, trumpet and assassin snail; named groups of cuboids drawn with their own tints) and of the dwarf clawed frog -> `client/model/CritterModelDefinitions.java`, their greyscale box-UV textures `entity/critters/*.png` (frog in colour), the algae overlays `entity/algae/algae_1..4.png` (alpha in the texture) and the icons `algae_scraper`, `dwarf_frog_bucket`. |
| `gui.py` | `textures/gui/tank.png` (256x256): 248x230 vanilla-style panel, content inset 7..240 x 17..139 (dark fill since M2), player inventory (x 44+18c, y 148+18r, hotbar y 206), icons at (0,232) heater ghost 16x16, (16,232) lock and (26,232) open lock 10x10, (36,232) slot frame 18x18 (since M4 the four old heater slots at x 12, y 148+18i are only drawn while a heater is left in them). M4: `external_filter.png` and `cabinet.png` (176x166, vanilla container layout; side sprites at (176,0) slot frame, (176,18) heater ghost, (176,34) filter ghost). Layout constants are mirrored in the Java screens. |
| `render.py` | Texture compositing (same order as the runtime) and a side-view software rasterizer. |
| `preview.py` | Preview sheets in `build/preview/` (forms, patterns, stages, icons, `types.png` = all named varieties, forms and patterns, copied to `builds/m5-types.png`) and the mod logo `src/main/resources/bettacraft.png`. |
| `guide_images.py` | M6: crops the screenshots of `gradlew runClient -Psmoketestworld` (`run/screenshots/guide_*.png`, window 1280x960) into the handbook images `assets/bettacraft/guides/bettacraft/guide/images/` (tank menu tabs also German, `_de_de/images/`). Not part of `build.py` (needs a client run). |
| `build.py` | Runs all of the above. |

## Layer / tint concept (runtime)

Every entity texture uses the same 64x64 UV layout. Each layer is one render pass of the same
model with `RenderType.entityTranslucent(layer)` and an ARGB vertex colour:

1. `region_body.png` x body colour. RGB is the grey shading, A is the body coverage.
2. `region_fins.png` x fin colour. RGB is the grey shading with fin rays, A is the fin coverage.
   `shade.png` is the same shading over the whole model. Use it as a single base pass when the
   body and fins have the same colour.
3. `pattern_<name>.png` x pattern colour. RGB is the shading, A is the pattern coverage (partial alpha is used).
4. `iridescence.png` x iridophore colour. Set the vertex alpha to gene strength (e.g. low 0.5, high 1.0) and optionally pulse it.
5. `eye.png` with a white tint (the eyes are already coloured).

Base layers have binary alpha. Fins are fully opaque inside their silhouette and transparent
outside it, so the sorting problems of translucent entities don't arise.

Fins are width-0 cuboids. Their west and east faces are coplanar and both are drawn, and their
textures are mirror images, so they show identical pixels. One side effect is that a
partial-alpha overlay is blended twice on fins. All random fields are computed from model-space
position, so this stays consistent.

Sickness, juveniles and females: desaturate or fade the tint colours in Java (see
`LIFE_STAGES[*].saturation` and `FEMALE_SATURATION`). No extra assets are needed.

## Items

- `betta_bucket.png` / `fish_bag.png` = layer0 (untinted).
- `*_fish.png` = layer1, tinted with the body colour.
- `*_fish_fins.png` = layer2, tinted with the fin colour. This layer is optional.
- The eye is a dark pixel inside the body layer.
