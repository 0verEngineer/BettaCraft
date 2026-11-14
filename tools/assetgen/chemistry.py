"""M25 assets: water chemistry item icons (TDS meter, pH drop test, remineralising salt, Indian almond leaf).

Deterministic pixel grids, no vanilla pixels.

Run from the repo root:  py tools/assetgen/chemistry.py
"""
from __future__ import annotations

from common import ITEM_TEX, save_png
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


def write_all():
    icons = {
        "tds_meter": grid(TDS, TDS_P), "ph_test_kit": grid(PH, PH_P),
        "remineralising_salt": grid(SALT, SALT_P), "almond_leaf": grid(LEAF, LEAF_P),
    }
    return [save_png(img, ITEM_TEX / f"{name}.png") for name, img in icons.items()]


if __name__ == "__main__":
    for p in write_all():
        print(p)
