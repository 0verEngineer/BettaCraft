"""M30 part a: item icons of the catching tools for big animals (large net, transport tub empty and filled, steel leader).

16x16 pixel art in the style of icons.py (dark outline, light from the top left). Run from the repo root:
    py tools/assetgen/big_animals.py
"""
from __future__ import annotations

import numpy as np

import icons
from common import ITEM_TEX, save_png
from icons import grid

STEEL = {"k": "#26282c", "H": "#f0f2f4", "L": "#c8ccd2", "M": "#9aa0a8", "D": "#6a7078"}

TUB = [
    "................",
    "................",
    "................",
    "................",
    "..kk........kk..",
    ".kMLkkkkkkkkLMk.",
    ".kkHHHHHHHHHHkk.",
    "..kLwwwwwwwwDk..",
    "..kLLMMMMMMDDk..",
    "..kLLMMMMMMDDk..",
    "..kHLMMMMMMDDk..",
    "...kLMMMMMMDk...",
    "...kLLMMMMDDk...",
    "....kkkkkkkk....",
    "................",
    "................",
]

TUB_FILLED = [
    "................",
    "................",
    "........k.......",
    ".......kFk......",
    "..kk..kFFFk.kk..",
    ".kMLkkkFFFkkLMk.",
    ".kkHWWWWWWWWHkk.",
    "..kLwwvwwwvwDk..",
    "..kLLMMMMMMDDk..",
    "..kLLMMMMMMDDk..",
    "..kHLMMMMMMDDk..",
    "...kLMMMMMMDk...",
    "...kLLMMMMDDk...",
    "....kkkkkkkk....",
    "................",
    "................",
]

LEADER = [
    "................",
    "...........kkk..",
    "..........kHLMk.",
    "..........kL.Dk.",
    "..........kMDDk.",
    "...........kkk..",
    "..........kL....",
    ".........kM.....",
    "........kL......",
    ".......kM.......",
    "......kL........",
    ".....kM.........",
    "..kk.kD.........",
    "..kDkkM.........",
    "...kDDk.........",
    "................",
]


def transport_tub():
    return grid(TUB, {**STEEL, "w": "#5c8ff0"})


def transport_tub_filled():
    return grid(TUB_FILLED, {**STEEL, "W": "#6a9cf4", "w": "#3a68d6", "v": "#2b4fb0", "F": "#4a5260"})


def steel_leader():
    return grid(LEADER, STEEL)


def large_net():
    """The aquarium net with a stainless steel handle and a wider, dark green cord mesh (big animals)."""
    img = icons.tank_net().astype(np.int32)
    out = img.copy()
    for y in range(16):
        for x in range(16):
            r, g, b, a = img[y, x]
            if a == 0:
                continue
            # wood (brownish) -> steel greys; the pale string mesh -> dark green cord
            if r > g + 18 and g > b:
                v = int(0.5 * r + 0.5 * g)
                out[y, x] = (v, v + 2, v + 6, 255)
            elif r > 0xb0 and abs(r - g) < 16 and abs(g - b) < 16 and (x + y) % 4 != 1:
                out[y, x] = (0x3a, 0x6a, 0x48, 255)
    return np.clip(out, 0, 255).astype(np.uint8)


ICONS = {
    "large_net": large_net,
    "transport_tub": transport_tub,
    "transport_tub_filled": transport_tub_filled,
    "steel_leader": steel_leader,
}


def write_all() -> list:
    return [save_png(f(), ITEM_TEX / f"{name}.png") for name, f in ICONS.items()]


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
