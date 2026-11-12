# Bettacraft asset pipeline

Everything here is procedural and deterministic, with fixed seeds and no external textures.
Regenerate it all from the repo root with:

```
python -m pip install --user -r tools/assetgen/requirements.txt
python tools/assetgen/build.py
```

The generated PNGs under `src/main/resources` are committed, so the Gradle build does not need Python.
`tools/assetgen/build/` is gitignored and only holds debug and preview output. The review sheet
is copied to `builds/m0-preview.png`.

## Scripts

| Script | Makes |
|---|---|
| `model_defs.py` | The single source of truth for the betta model: cuboids per body shape (plakat, veil, halfmoon, crowntail, doubletail; dumbo = second pectoral set), the shared 64x64 box-UV layout (auto-packed), life-stage scales and female scales. Also writes `build/generated/BettaModelDefinitions.java`, `betta_<shape>.bbmodel` (Blockbench, modded entity) and `uv_layout.txt`. |
| `fins.py` | Fin silhouettes (alpha cut-outs: crowntail spikes, doubletail notch), fin-ray field, distance-to-edge field. |
| `uv_template.py` | Per-texel metadata (part, face, model-space position, fin coordinates). Writes `build/uv_template.png` (debug) and `region_body.png` / `region_fins.png`. |
| `patterns.py` | `shade.png`, `pattern_*.png` (butterfly, marble, dragon, koi, mask, tipped, fade, grizzle, bars), `iridescence.png`, `eye.png`. |
| `palette.py` | 43 named colours (id, en_us, de_de, hex) -> `build/generated/colors.json`. |
| `icons.py` | 16x16 item icons plus greyscale fish overlay layers for the bucket and the bag. |
| `blocks.py` | Tank block textures, including the animated greyscale water. |
| `render.py` | Texture compositing (same order as the runtime) and a side-view software rasterizer. |
| `preview.py` | Preview sheets in `build/preview/`, `builds/m0-preview.png` and the mod logo `src/main/resources/bettacraft.png`. |
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
