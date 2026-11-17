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

# ---------------------------------------------------------------- M28 part c

BOAT_P = {"k": K, "H": "#c8a46a", "L": "#a8844c", "M": "#86663a", "D": "#644a28", "w": "#17140f", "g": "#5d7f30",
          "s": "#8f6d45", "S": "#6b5234"}
ROWING_BOAT = [
    "................",
    "................",
    "................",
    "......kkkk......",
    "...kkkSSSSkkk...",
    ".kkHHHLLgLHHHkk.",
    "kHHLLLLLLLLLLLHk",
    "kLLLMMMMMMMMMLLk",
    "kMMMkkkkkkkkkMMk",
    "kDDkwwwwwwwwwkDk",
    "kssk.wwwwwwww.kk",
    "kkk...........k.",
    "................",
    "..kkkkkkkkkk....",
    "..kssssssssskkk.",
    "..kkkkkkkkkkkSk.",
]

BOW_P = {"k": K, "H": "#b48a55", "L": "#946c3d", "M": "#74522c", "D": "#553a1d", "s": "#e9e2cf",
         "o": "#b0623a", "O": "#d07a4a", "w": "#2a1810"}
SHIP_BOW = [
    "kk..............",
    ".kHk......k.....",
    "..kHk.....k.....",
    "...kHk...kskk...",
    "...kHHk..ksssk..",
    "...kHLLk.kkkk...",
    "...kHLLLkk.k....",
    "...kHLLLLLLLk.k.",
    "...kHLLLLLLLLkHk",
    "...kHLLkkkkkkLLk",
    "...kHLkoOOOOokLk",
    "...kHLkOwwwwOkMk",
    "....kLkOwwwwOkMk",
    "....kMkoOOOOokDk",
    ".....kkkkkkkkkk.",
    "................",
]

HOLEY_P = {"k": K, "H": "#f2f0e8", "L": "#dcdad0", "M": "#bdb9ae", "D": "#9a958a", "w": "#2e2a26", "b": "#866b5c"}
HOLEY_ROCK = [
    "................",
    "................",
    "................",
    "......kkkk......",
    "....kkHHLLkk....",
    "...kHHLwLLMLk...",
    "..kHLLLLLbMMMk..",
    "..kHwLLLLMMwDk..",
    ".kHLLLkkkkMMMDk.",
    ".kHLLkwwwwkMwDk.",
    "kHLbLkwwwwkMMDDk",
    "kHLLMkwwwwkMDwDk",
    "kLwMMkwwwwkMMDDk",
    "kLLMMkwwwwkDDDDk",
    "kkkkkkkkkkkkkkkk",
    "................",
]

SLATE_P = {"k": "#0e0e10", "H": "#6a6a70", "L": "#525258", "M": "#3e3e44", "D": "#2c2c31", "w": "#0a0a0c"}
SLATE_CAVE = [
    "................",
    "................",
    "................",
    "................",
    "....kkkkkkkk....",
    "...kHHHHHHHHk...",
    ".kkkkkkkkkkkkkk.",
    "kHHHHHHHHHHHHHLk",
    "kLMMMMMMMMMMMMDk",
    "kkkkkkkkkkkkkkkk",
    ".kHLkwwwwwwkHLk.",
    ".kMMkwwwwwwkMMk.",
    ".kHLkwwwwwwkHLk.",
    ".kMDkwwwwwwkMDk.",
    ".kkkkkkkkkkkkkk.",
    "................",
]

SEIRYU_P = {"k": K, "H": "#a9a9ad", "L": "#8a8a8f", "M": "#6d6d72", "D": "#4f4f55", "v": "#ececec"}
SEIRYU_STONE = [
    "....k...........",
    "....kk..........",
    "...kHk..........",
    "...kHLk.........",
    "..kHvLk.........",
    "..kHLvMk........",
    "..kHLLvk........",
    ".kHLLMMk...k....",
    ".kHLvMMDk.kHk...",
    ".kHLLvMDkkHLMk..",
    "kHLLLMvMDkHvMk..",
    "kHLLMMMDDkLLvDk.",
    "kHLvMMMDDkHLMDk.",
    "kLLLMMMDDkLMMDDk",
    "kkkkkkkkkkkkkkkk",
    "................",
]

DRAGON_P = {"k": "#2a170e", "H": "#c98a62", "L": "#a86e4a", "M": "#8a5638", "D": "#6a3f27", "w": "#1e120b", "c": "#d9a47c"}
DRAGON_STONE = [
    "................",
    "................",
    "................",
    "....kkkk........",
    "...kHcLwk.......",
    "..kHLwLLMk..kk..",
    "..kHLLMwMk.kHck.",
    ".kHwLLMMDkkHLwMk",
    ".kHLLwMMDkHLLMwk",
    "kHcLMMwMDkHwLMDk",
    "kHLwMMMDDkLLMMDk",
    "kLLMkkkkkkkMwMDk",
    "kLwMk.....kMMDDk",
    "kLLMk.....kMDDDk",
    "kkkkk.....kkkkkk",
    "................",
]

LAVA_P = {"k": "#140c0c", "H": "#a8473a", "L": "#8a3529", "M": "#2e2a2e", "D": "#1c1a1e", "w": "#080608", "r": "#c25a44"}
LAVA_ROCK = [
    "................",
    "................",
    "................",
    "................",
    ".....kkkkk......",
    "...kkrHwMHkk....",
    "..kHLwMLLwMMk...",
    "..kHMLLwMLMDk...",
    ".kHLwMMLLwMDDk..",
    ".krLLMkkkkLMwDk.",
    "kHwMLkwwwwkMDDk.",
    "kLMMwkwwwwkwMDk.",
    "kLLwMkwwwwkMDDDk",
    "kMLMMkwwwwkMDwDk",
    "kkkkkkkkkkkkkkkk",
    "................",
]

MANGROVE_P = {"k": "#1a120c", "H": "#7a4a34", "L": "#6b3c2a", "M": "#4a3b2a", "D": "#352a1e", "R": "#5e4a36"}
MANGROVE_ROOT = [
    "......kkk.......",
    ".....kHLLk......",
    ".....kHLLk......",
    ".....kHLMk......",
    "....kRHLMRk.....",
    "...kRRkkkRRk....",
    "..kRMk...kMRk...",
    "..kRk.....kRk...",
    ".kRMk.....kMRk..",
    ".kRk...k...kRk..",
    "kRMk..kRk..kMRk.",
    "kRk..kRMk...kRk.",
    "kRk..kRk....kRk.",
    "kMk..kMk....kMk.",
    "kkk..kkk....kkk.",
    "................",
]

# M49: the tall stilt roots: a trunk high up on long arches
MANGROVE_STILT = [
    "....kkk.kkk.....",
    "....kLk.kLk.....",
    ".....kLkLk......",
    "......kHLk......",
    "......kHLk......",
    "......kHMk......",
    ".....kRkkRk.....",
    "....kRk..kRk....",
    "...kRk....kRk...",
    "..kRMk....kMRk..",
    "..kRk..kk..kRk..",
    ".kRMk.kRRk.kMRk.",
    ".kRk.kRk.kRk.kRk",
    "kRMk.kRk..kRkkMk",
    "kDDk.kDk..kDk.kD",
    "................",
]

SPIDER_P = {"k": "#3a2a14", "H": "#e0c48e", "L": "#c4a46a", "M": "#a2844e", "D": "#806434"}
SPIDERWOOD = [
    "k.....k...k....k",
    "Hk...kL..kH...kL",
    ".Lk..kH.kL...kL.",
    "..Lk.kLkkL..kL..",
    "...LkLHkL..kL...",
    "kk..LHLLk.kL..kk",
    "HLk..LHLkkL..kLH",
    "..LLk.LHHLLkkL..",
    "....LkkHLMkLL...",
    ".....LkHLMk.....",
    "......kHLMk.....",
    "......kHLMk.....",
    ".....kHLLMDk....",
    "....kHLLMMDDk...",
    "....kkkkkkkkk...",
    "................",
]

BOG_P = {"k": "#120c06", "H": "#6a5236", "L": "#4e3a24", "M": "#3a2a18", "D": "#281c10"}
BOGWOOD_BRIDGE = [
    "................",
    "................",
    "................",
    "................",
    ".....kkkkkk.....",
    "...kkHHLLLLkk...",
    "..kHHLLMMMMLLk..",
    ".kHLLMkkkkkkMMk.",
    ".kHLMk......kMDk",
    "kHLMk........kDk",
    "kHLk..........kk",
    "kLMk..........kD",
    "kLMk..........kD",
    "kHLMk........kMD",
    "kkkkk........kkk",
    "................",
]

COCO_P = {"k": "#1a0f08", "H": "#8a5a36", "L": "#6e4426", "M": "#55331a", "D": "#3e2412", "f": "#a0784e", "w": "#120a05"}
COCONUT_CAVE = [
    "................",
    "................",
    "................",
    "................",
    "................",
    ".....kkkkkk.....",
    "...kkHfLLLMkk...",
    "..kHHLLfLMMMDk..",
    ".kHLfLLLMfMMDDk.",
    ".kHLLLkkkkMMDDk.",
    "kHfLLkwwwwkMfDDk",
    "kHLLMkwwwwkMMDDk",
    "kLLMMkwwwwkMDDDk",
    "kkkkkkkkkkkkkkkk",
    "................",
    "................",
]

CLAY_P = {"k": "#2a140a", "H": "#d88a5a", "L": "#b8693e", "M": "#9a5530", "D": "#7a4022", "w": "#1e0f08", "W": "#3a1e10"}
CLAY_TUBES = [
    "................",
    "................",
    "................",
    "................",
    "kkkkkkk.kkkkkkk.",
    "kHHHHLk.kHHHHLk.",
    "kHkkkLk.kHkkkLk.",
    "kHwWwMk.kHwWwMk.",
    "kHwwwMk.kHwwwMk.",
    "kLwwwMk.kLwwwMk.",
    "kLkkkMk.kLkkkMk.",
    "kLMMMDkkkLMMMDk.",
    "kkkkkkkkkkkkkkk.",
    "................",
    "................",
    "................",
]

SHRIMP_P = {"k": "#24140c", "H": "#c28a68", "L": "#a86c4c", "O": "#c66a2e", "o": "#a2521f", "W": "#e4cbb8", "V": "#c4a896",
            "w": "#1a0e08"}
SHRIMP_TUBES = [
    "................",
    "................",
    "................",
    "....kkkkkk......",
    "....kWWWVk......",
    "....kWkkVk......",
    "....kWkwkVk.....",
    "....kVkkVk......",
    "kkkkkkkkkkkkk...",
    "kHHHLkkOOOok....",
    "kHkkLkkOkkok....",
    "kHkwLkkOkwok....",
    "kHkkLkkOkkok....",
    "kLLLLkkooook....",
    "kkkkkkkkkkkk....",
    "................",
]

RUIN_P = {"k": K, "H": "#e8e8ea", "L": "#c8c8cc", "M": "#a6a6ac", "D": "#84848a", "m": "#5d7f30", "n": "#7ca444"}
RUIN_COLUMN = [
    "....kk..........",
    "....kHk.........",
    "...kHLkk........",
    "...kHLMk........",
    "...kHLMk........",
    "...kHLMk........",
    "...kHLMk........",
    "...kHLMk........",
    "...kHLMk........",
    "...knLMk........",
    "...kHLmk........",
    "..kkHLMkk.kkkkk.",
    ".kHHLLMMDkHLLMDk",
    ".knHLLLMDkLMMnDk",
    ".kkkkkkkkkkkkkkk",
    "................",
]

HELMET_P = {"k": "#2a1408", "H": "#e0a050", "L": "#c47a34", "M": "#a15325", "D": "#7a3c1a", "y": "#f0c860",
            "w": "#14100c", "g": "#9a9a9a", "G": "#c8d0d4"}
DIVER_HELMET = [
    "......kkk.......",
    "......kyk.......",
    "....kkkkkkk.....",
    "...kHHLLLMMk....",
    "..kHHLLLLMMDk...",
    "..kHLkkkkkMDk...",
    ".kGHkwwwwwkMDk..",
    ".kgHkwwwwwkMgk..",
    ".kGLkwwwwwkMDk..",
    "..kLyk...kyDk...",
    "..kLLkkkkkMDk...",
    "..kkkkkkkkkkk...",
    ".kHLyLLLLLyMDk..",
    "kHHLLLLLLLLMMDk.",
    "kkkkkkkkkkkkkkk.",
    "................",
]

CHEST_P = {"k": "#1c1008", "H": "#9a7448", "L": "#7a5632", "M": "#5e4024", "D": "#3e2814", "G": "#ffd84a",
           "g": "#e0a020", "b": "#bfe8ff", "B": "#7fc0ea"}
TREASURE_CHEST = [
    "........b.......",
    ".......B.b......",
    "..kkkkkkbkkkk...",
    "..kHLLLLLLLDk...",
    "..kHLLLLLLLDk...",
    "..kMMMMMMMMDk...",
    "..kkkkkbkkkkk...",
    ".kGgGGgBGgGGgk..",
    ".kDkkkkkkkkkDk..",
    ".kDHLLLggLLLDk..",
    ".kDHLLkGGkLLDk..",
    ".kDHLLLkkLLLDk..",
    ".kDMMMMMMMMMDk..",
    ".kkkkkkkkkkkkkk.",
    "..kGgk.kGgGk.kGk",
    "...k....kkk...k.",
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
    "decor_rowing_boat": (ROWING_BOAT, BOAT_P),  # M28
    "decor_ship_bow": (SHIP_BOW, BOW_P),  # M28
    "decor_holey_rock": (HOLEY_ROCK, HOLEY_P),  # M28
    "decor_slate_cave": (SLATE_CAVE, SLATE_P),  # M28
    "decor_seiryu_stone": (SEIRYU_STONE, SEIRYU_P),  # M28
    "decor_dragon_stone": (DRAGON_STONE, DRAGON_P),  # M28
    "decor_lava_rock": (LAVA_ROCK, LAVA_P),  # M28
    "decor_mangrove_root": (MANGROVE_ROOT, MANGROVE_P),  # M28
    "decor_spiderwood": (SPIDERWOOD, SPIDER_P),  # M28
    "decor_bogwood_bridge": (BOGWOOD_BRIDGE, BOG_P),  # M28
    "decor_coconut_cave": (COCONUT_CAVE, COCO_P),  # M28
    "decor_clay_tubes": (CLAY_TUBES, CLAY_P),  # M28
    "decor_shrimp_tubes": (SHRIMP_TUBES, SHRIMP_P),  # M28
    "decor_ruin_column": (RUIN_COLUMN, RUIN_P),  # M28
    "decor_diver_helmet": (DIVER_HELMET, HELMET_P),  # M28
    "decor_treasure_chest": (TREASURE_CHEST, CHEST_P),  # M28
    "decor_mangrove_stilt_roots": (MANGROVE_STILT, MANGROVE_P),  # M49
}


def all_icons() -> dict[str, np.ndarray]:
    return {k: grid(rows, cmap) for k, (rows, cmap) in ICONS.items()}


def write_all() -> list:
    return [save_png(img, ITEM_TEX / f"{name}.png") for name, img in all_icons().items()]


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
