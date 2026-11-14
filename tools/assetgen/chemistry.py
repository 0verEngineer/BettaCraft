"""M25 assets: water chemistry item icons (TDS meter, pH drop test, remineralising salt, Indian almond leaf) and the
block texture of the active soil (part d).

Deterministic pixel grids, no vanilla pixels.

Run from the repo root:  py tools/assetgen/chemistry.py
"""
from __future__ import annotations

import numpy as np

from common import BLOCK_TEX, ITEM_TEX, save_png
from icons import grid

# a pen-shaped meter: grey body, display window with digits, steel probe tips at the bottom
TDS = [
    "..........oo....",
    ".........obbo...",
    "........obbbbo..",
    ".......obddddbo.",
    "......obdgdgdbo.",
    ".....obbddddbo..",
    "....obbbbbbbo...",
    "...obblbbbbo....",
    "..obblbbbbo.....",
    "..obbbbbbo......",
    "...obbbbo.......",
    "...oossoo.......",
    "..oss.oo........",
    ".oso............",
    ".so.............",
    "................",
]
TDS_P = {"o": "#1c2228", "b": "#5b6a78", "l": "#8fa0b0", "d": "#2a4a2a", "g": "#7cf07c", "s": "#c8d0d8"}

# a small test tube with coloured liquid and a reagent bottle with a dropper
PH = [
    "................",
    "...oo......o....",
    "..owwo....oro...",
    "..owwo....oro...",
    "..owwo...ooooo..",
    "..owwo...owwwo..",
    "..oyyo...obbbo..",
    "..oyyo...obbbo..",
    "..oyyo...obbbo..",
    "..oggo...obbbo..",
    "..oggo...obbbo..",
    "..oggo...ooooo..",
    "...oo...........",
    "................",
    "................",
    "................",
]
PH_P = {"o": "#26303a", "w": "#dfeef4", "y": "#e8c832", "g": "#5cb84a", "r": "#c83a3a", "b": "#4a70c8"}

# a heap of white mineral salt with some grey and pinkish grains
SALT = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".......oo.......",
    "......owwo......",
    ".....owwgwo.....",
    "....owpwwwwo....",
    "...owwwwgwpwo...",
    "..owgwwpwwwwwo..",
    "..owwwwwwwgwwo..",
    "...oooooooooo...",
    "................",
    "................",
]
SALT_P = {"o": "#6a6a70", "w": "#f2f2ee", "g": "#b8bcc0", "p": "#e8c8c0"}

# a dried brown leaf with a lighter midrib and veins
LEAF = [
    "................",
    "..........oooo..",
    "........oobbbbo.",
    ".......obbbbbbo.",
    "......obbbvbbbo.",
    ".....obbbvbbbbo.",
    "....obvbvbbbbo..",
    "...obbvvbbvbo...",
    "...obbbvbvbbo...",
    "..obbbvvbbbo....",
    "..obbvbbbbo.....",
    "..obvbbbbo......",
    "..ovbbboo.......",
    ".ovooo..........",
    "ov..............",
    "................",
]
LEAF_P = {"o": "#3a2412", "b": "#8a5426", "v": "#c88a48"}


def active_soil():
    """Active soil (part d): round dark brown-black clay granules with lighter rims and dark gaps, seeded and tileable."""
    rng = np.random.default_rng(2501)
    s = 16
    out = np.zeros((s, s, 4), np.float64)
    gap = np.array([22, 16, 12], np.float64)
    out[..., :3] = gap
    out[..., 3] = 255
    # granules on a jittered 4x4 grid (radius about 1.8 px), wrapped at the edges so the texture tiles
    for gy in range(4):
        for gx in range(4):
            cx = gx * 4 + 1.5 + rng.uniform(-0.6, 0.6)
            cy = gy * 4 + 1.5 + rng.uniform(-0.6, 0.6)
            base = np.array([58, 42, 30], np.float64) * rng.uniform(0.75, 1.15)
            if rng.random() < 0.18:
                base = np.array([84, 62, 40], np.float64)  # a few lighter, browner grains
            for y in range(s):
                for x in range(s):
                    dx = min(abs(x - cx), s - abs(x - cx))
                    dy = min(abs(y - cy), s - abs(y - cy))
                    d = (dx * dx + dy * dy) ** 0.5
                    if d < 2.0:
                        shade = 1.0 + 0.35 * max(0.0, (cy - y) / 2.0 if abs(y - cy) < 2 else 0) - 0.25 * (d / 2.0)
                        out[y, x, :3] = np.clip(base * shade, 0, 255)
    return np.clip(out, 0, 255).astype(np.uint8)


def write_all():
    icons = {
        "tds_meter": grid(TDS, TDS_P), "ph_test_kit": grid(PH, PH_P),
        "remineralising_salt": grid(SALT, SALT_P), "almond_leaf": grid(LEAF, LEAF_P),
    }
    out = [save_png(img, ITEM_TEX / f"{name}.png") for name, img in icons.items()]
    out.append(save_png(active_soil(), BLOCK_TEX / "active_soil.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print(p)
