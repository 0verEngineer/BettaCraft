"""The fish bucket icons (since M21; variant B, the only one since M21.5): a glass bucket with an iron rim and handle,
water, the fish at ~60 % fully inside the water, a water line and a reflection on the glass in front of the fish.

Four layers (0 = container, untinted; 1 = fish body and 2 = fins, greyscale, tinted at runtime with the fish's
colours; 3 = glass in front of the fish, untinted) plus the frog bucket, written to the mod's item textures.
"""
from __future__ import annotations

import numpy as np

import icons
from common import ITEM_TEX, save_png


S = 16
IRON = icons.IRON
GLASS = {
    # iron rim and handle
    "k": "#2c2c31", "H": "#f4f4f4", "L": "#d6d6d6", "M": "#a9a9ad", "D": "#76767c",
    # glass: outline, wall, air inside, the lip of the jar
    "o": "#3b4f57", "g": "#d8eef2", "a": "#b4cfd6", "l": "#f2fbfc", "r": "#e8f6f8",
    # water: surface, body, depth
    "W": "#8fc4f4", "w": "#4f8be0", "v": "#3a6bc4",
}

B_BASE = [
    "................",
    ".....kkkkkk.....",
    "....kD....Dk....",
    "...kD......Dk...",
    ".kkkkkkkkkkkkkk.",
    ".kHLLLLLLLLLLMk.",
    ".kkkkkkkkkkkkkk.",
    ".oglaaaaaaaaago.",
    ".ogWWWWWWWWWWgo.",
    ".ogwwwwwwwwwwgo.",
    ".ogwwwwwwwwwwgo.",
    ".ogwwwwwwwwwwgo.",
    ".ogwwwwwwwwwwgo.",
    "..ogvvvvvvvvgo..",
    "...oggggggggo...",
    "....oooooooo....",
]
# ~60 % of the old fish, facing left, fully inside the water (rows 8..13, columns 3..12)
B_FISH = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "......FF........",
    ".....bbbbF.F....",
    "....bebbbbFF....",
    ".....bbbbF.F....",
    "......FF........",
    "................",
    "................",
]
B_INSIDE = [(x, y) for y in range(8, 14) for x in range(3, 13)]
# glass in front of the fish: a reflection streak on the left, a faint one on the right, the water line
B_FRONT = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "...ssssssssss...",
    "...H........h...",
    "...Hh.......h...",
    "...Hh...........",
    "...H............",
    "................",
    "................",
    "................",
]
FRONT = {"H": "#ffffff8c", "h": "#ffffff40", "s": "#ffffff38"}

def _fish(rows, inside):
    body, fins = icons.fish_layers(rows)
    if inside is not None:
        keep = np.zeros((S, S), bool)
        for x, y in inside:
            keep[y, x] = True
        body[~keep] = 0
        fins[~keep] = 0
    return body, fins


FROG_BODY = [(7, 5), (8, 5), (6, 6), (7, 6), (8, 6), (9, 6), (7, 7), (8, 7), (5, 7), (10, 7), (6, 8), (9, 8)]


def _frog(base, front, dx, dy):
    """The dwarf frog bucket: the container with a small frog in the water (and the glass in front)."""
    img = base.copy()
    body = np.array([112, 104, 80, 255], np.uint8)
    for (x, y) in FROG_BODY:
        img[y + dy, x + dx] = body
    img[6 + dy, 7 + dx] = np.array([60, 56, 42, 255], np.uint8)
    img[5 + dy, 7 + dx] = np.array([20, 20, 18, 255], np.uint8)
    img[5 + dy, 8 + dx] = np.array([20, 20, 18, 255], np.uint8)
    return _over(img, front)


def _over(dst, src):
    """Alpha-composite src over dst (uint8 RGBA)."""
    a = src[..., 3:4].astype(np.float32) / 255
    out = dst.astype(np.float32)
    out[..., :3] = src[..., :3] * a + out[..., :3] * (1 - a)
    out[..., 3:4] = np.maximum(out[..., 3:4], src[..., 3:4])
    return out.round().astype(np.uint8)


def textures() -> dict[str, np.ndarray]:
    """The bucket textures by file name (without .png)."""
    base = icons.grid(B_BASE, GLASS)
    body, fins = _fish(B_FISH, B_INSIDE)
    front = icons.grid(B_FRONT, FRONT)
    frog = _frog(base, front, 0, 4)
    return {"betta_bucket": base, "betta_bucket_fish": body, "betta_bucket_fish_fins": fins,
            "betta_bucket_glass": front, "dwarf_frog_bucket": frog}


def write_all() -> list:
    return [save_png(img, ITEM_TEX / f"{name}.png") for name, img in textures().items()]


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
