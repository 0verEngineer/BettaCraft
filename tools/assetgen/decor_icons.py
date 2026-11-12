"""M5: hand-authored 16x16 pixel-art item icons for the decoration items and the internal filter.

The M3 icons were isometric renders of the structure voxels, downsampled to 16x16, which made them blurry blobs.
These are drawn as character grids in the style of icons.py (dark outline, 3-4 step ramps, light from the top left).
"""
from __future__ import annotations

import numpy as np

from common import ITEM_TEX, save_png
from icons import grid

K = "#24232a"  # outline

STONE = {"k": K, "H": "#b4b4b8", "L": "#96969b", "M": "#7a7a80", "D": "#5c5c63", "w": "#18171c",
         "r": "#d0312c", "R": "#8f1c1f", "p": "#6b4a2b", "g": "#4a3320", "G": "#2f2015"}
CASTLE = [
    "....kk..........",
    "....krrk........",
    "....krRRk.......",
    "....kk.k........",
    ".kk.kk.k..kk.kk.",
    ".kHkkHkk..kHkkHk",
    ".kHLLLMk..kHLLMk",
    ".kHwLLMkkkkLwLMk",
    ".kHLLLMHLLMLLLMk",
    ".kHLLMMLLLMMLLMk",
    ".kLLMwMLkkkLMwDk",
    ".kHLMMLkGGGkLMDk",
    ".kHLMMLkGgGkLMDk",
    ".kLMMDMkGgGkMDDk",
    ".kkkkkkkkkkkkkkk",
    "................",
]

WOOD = {"k": K, "H": "#b48a55", "L": "#946c3d", "M": "#74522c", "D": "#553a1d", "s": "#e9e2cf", "S": "#c6bda4",
        "t": "#a59d86", "w": "#2a1d10", "b": "#3c6d8f"}
SHIPWRECK = [
    "................",
    ".......k........",
    ".......kk.......",
    "......kMk.......",
    "......kMksk.....",
    ".....kMkssSk....",
    ".....kMksSSSk...",
    ".....kMkSStk....",
    "kk...kMkkkk.....",
    "kHkk.kMk...kkk..",
    "kHHLkkkkkkkLLHk.",
    ".kHLLLwLLwLLMMk.",
    ".kLLMMMMMMMMMDk.",
    "..kMMDDDDDDDDk..",
    "...kkkkkkkkkk...",
    "................",
]

BONE = {"k": K, "H": "#f6f0dc", "L": "#e2d9bc", "M": "#c8bd99", "D": "#a39670", "e": "#1a1716", "t": "#fbf8ee"}
DINO_SKULL = [
    "................",
    "................",
    "......kkkkkk....",
    "....kkHHHHLLkk..",
    "...kHHHLLLLLMMk.",
    "..kHHkkLLLLLLMk.",
    "..kHkeekLLkkLMk.",
    ".kHHkeekLkeekMk.",
    ".kHLLkkLLkeekMk.",
    "kHLLLLLLLLkkMDk.",
    "kLLLMMMMMMMMMDk.",
    "kkttktktktkkkk..",
    ".kkLLLLMMMMDk...",
    "..kktktktktk....",
    "....kkkkkkk.....",
    "................",
]

ROCK = {"k": K, "H": "#5f5c62", "L": "#48464d", "M": "#36343a", "D": "#252328", "o": "#ffb43a", "O": "#f06a1c",
        "r": "#b8341a", "s": "#c9c9cc", "S": "#9a9aa0"}
VOLCANO = [
    ".....ss.ss......",
    "....sSSssSs.....",
    ".....sSSSs......",
    "......kkkk......",
    ".....kooOOk.....",
    ".....kLOrMk.....",
    "....kHLOMMMk....",
    "....kHLrOMMk....",
    "...kHLLMOrMDk...",
    "...kHLLMMOMDk...",
    "..kHHLLMMrODDk..",
    "..kHLLMMMMODDk..",
    ".kHLLLMMMMMODDk.",
    ".kLLMMMMMMMrDDk.",
    "kkkkkkkkkkkkkkkk",
    "................",
]

DRIFT = {"k": K, "H": "#c9a774", "L": "#a88655", "M": "#86663c", "D": "#5f4627", "m": "#5d7f30"}
DRIFTWOOD = [
    "................",
    "...........kk...",
    "..........kHLk..",
    "...kk....kHLk...",
    "..kHLk..kHLk....",
    "...kHLkkHLk.....",
    "....kHLHLk......",
    ".....kHLLk......",
    "....kHLMMk......",
    "...kHLMMDk......",
    "..kHLMMDk.......",
    ".kHLMMDk........",
    "kHLMmmk.........",
    "kLMDmk..........",
    ".kkkk...........",
    "................",
]

PEBBLE = {"k": K, "H": "#b9b6b0", "L": "#9b9891", "M": "#7d7a74", "D": "#5e5b56",
          "h": "#c8b08a", "l": "#a8906a", "m": "#86704e", "d": "#66533a", "x": "#8a96a0", "y": "#6c7882", "z": "#525d66"}
STONES = [
    "................",
    "................",
    "................",
    "................",
    "......kkkk......",
    ".....kHHLLk.....",
    "....kHLLLMMk....",
    "....kHLLMMDk....",
    "..kkkLMMMDDkkk..",
    ".khhlkkkkkkxxyk.",
    "khhllmmkkxxxyyzk",
    "khlllmmdkxxyyyzk",
    "khllmmmdkxyyyzzk",
    ".kllmmddkkyyzzk.",
    "..kkkkkk..kkkk..",
    "................",
]

MOSS = {"k": "#1f2a12", "H": "#8fc04a", "L": "#6fa034", "M": "#557f26", "D": "#3d5e1a", "s": "#a8d468"}
MOSS_BALL = [
    "................",
    "................",
    "......kkkk......",
    "....kkHHLLkk....",
    "...kHsHLLMLMk...",
    "...kHsLLMLMMk...",
    "..kHHLLMLMMDMk..",
    "..kHLLMLMMMMDk..",
    "..kLHLMMLMDMDk..",
    "..kLLMMMMDMDDk..",
    "...kLMDMMDDDk...",
    "...kMMMDDMDDk...",
    "....kkDDDDkk....",
    "......kkkk......",
    "................",
    "................",
]

LEAF = {"k": "#1d2a12", "H": "#7fc24a", "L": "#5fa035", "M": "#468026", "D": "#33611b", "v": "#9ad466",
        "c": "#d8e6ea", "C": "#a9bcc4", "s": "#7a8a90"}
HAMMOCK = [
    "................",
    "................",
    "..kk......kk....",
    ".kHHkk..kkLLk...",
    ".kHvHLkkLLMMk...",
    "..kHLvLLLMMDk...",
    "..kHLLvvLMMDk...",
    "...kHLLLvvDk....",
    "....kHLMMMk.....",
    ".....kkMDk......",
    ".......kk.......",
    ".......ks.......",
    "......kCck......",
    ".....kCcccK.....".replace("K", "k"),
    "......kkkk......",
    "................",
]

ALMOND = {"k": "#2a170a", "H": "#d58a3a", "L": "#b86b26", "M": "#954f19", "D": "#6e3810", "v": "#e8b070", "s": "#5a3010"}
ALMOND_LEAF = [
    "................",
    "..........kkk...",
    "........kkHHLk..",
    "......kkHHLLMk..",
    ".....kHHvLLMMk..",
    "....kHHLvLMMDk..",
    "....kHLLLvMMDk..",
    "...kHLvLLMvMDk..",
    "...kHLLvLMvDk...",
    "..kHLLLLvvMDk...",
    "..kHLLMMMvDk....",
    "..kLLMMMMvk.....",
    "...kMMDDvk......",
    "....kkksk.......",
    "......ks........",
    "......k.........",
]

CAVE_P = {"k": K, "H": "#8f8b84", "L": "#76726b", "M": "#5f5b55", "D": "#47443f", "w": "#141216", "W": "#26232a",
          "m": "#5d7f30", "n": "#7ca444"}
CAVE = [
    "................",
    "................",
    "................",
    "......kkkk......",
    "....kkmnHLkk....",
    "...kHmnHLLMLk...",
    "..kHHLLLLMMMDk..",
    "..kHLLkkkkMMDk..",
    ".kHLLkWwwWkMDDk.",
    ".kHLkWwwwwWkMDk.",
    "kHLLkwwwwwwkMDDk",
    "kHLMkwwwwwwkMDDk",
    "kLLMkwwwwwwkDDDk",
    "kLMMkwwwwwwkMDDk",
    "kkkkkkkkkkkkkkkk",
    "................",
]

FILTER = {"k": K, "H": "#5a5a62", "L": "#47474e", "M": "#38383e", "D": "#2a2a2f", "s": "#16161a",
          "b": "#6cb4e0", "B": "#3c86c0", "c": "#cfd3d8"}
INTERNAL_FILTER = [
    "................",
    ".....kkkkkk.....",
    ".....kcbBck.....",
    "....kkkkkkkk....",
    "....kHLLLMMk....",
    "....kHssssDk....",
    "....kHLLLMDk....",
    "....kHssssDk....",
    "....kHLLLMDk....",
    "....kHssssDk....",
    "....kHLLLMDk....",
    "....kHssssDk....",
    "....kHLLMMDk....",
    "....kkkkkkkk....",
    ".....kc..ck.....",
    "................",
]

ICONS = {
    "decor_castle": (CASTLE, STONE),
    "decor_shipwreck": (SHIPWRECK, WOOD),
    "decor_dino_skull": (DINO_SKULL, BONE),
    "decor_volcano": (VOLCANO, ROCK),
    "decor_driftwood": (DRIFTWOOD, DRIFT),
    "decor_stones": (STONES, PEBBLE),
    "decor_moss_ball": (MOSS_BALL, MOSS),
    "decor_hammock": (HAMMOCK, LEAF),
    "decor_almond_leaf": (ALMOND_LEAF, ALMOND),
    "decor_cave": (CAVE, CAVE_P),
    "internal_filter": (INTERNAL_FILTER, FILTER),
}


def all_icons() -> dict[str, np.ndarray]:
    return {k: grid(rows, cmap) for k, (rows, cmap) in ICONS.items()}


def write_all() -> list:
    return [save_png(img, ITEM_TEX / f"{name}.png") for name, img in all_icons().items()]


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
