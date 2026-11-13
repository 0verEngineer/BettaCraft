"""M21: the fish bucket icons in three variants (the fish used to stick out of the bucket).

* **A** - the bucket up to M20: iron bucket, the fish drawn over it (sticks out on the right).
* **B** - glass bucket: iron rim and handle, a glass body with water, the fish at ~60 % fully inside the water,
  a water line and a reflection on the glass in front of the fish (layer 3). **Active since M21.**
* **C** - a bigger bulbous jar (goldfish bowl), the fish a bit smaller, nothing sticks out.

Every variant has the same four layers (0 = container, untinted; 1 = fish body and 2 = fins, greyscale, tinted at
runtime with the fish's colours; 3 = glass in front of the fish, untinted) plus the frog bucket. The active variant
(``ACTIVE``) is written to the mod's item textures; the others go to ``tools/assetgen/bucket_variants/<x>/`` so they
can be compared (``bucket_grid.py``) or switched back to by changing ``ACTIVE`` and running this script. The user
decides after testing M21 whether A and C are deleted.
"""
from __future__ import annotations

from pathlib import Path

import numpy as np

import icons
from common import ITEM_TEX, save_png

ACTIVE = "B"
HERE = Path(__file__).resolve().parent
VARIANT_DIR = HERE / "bucket_variants"

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

# ---------------------------------------------------------------- B: glass bucket

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

# ---------------------------------------------------------------- C: goldfish bowl

C_BASE = [
    "................",
    "....oooooooo....",
    "...orrrrrrrro...",
    "....oglaaago....",
    "..ooglaaaaagoo..",
    ".oglWWWWWWWWWgo.",
    "oglwwwwwwwwwwwgo",
    "ogwwwwwwwwwwwwgo",
    "ogwwwwwwwwwwwwgo",
    "ogwwwwwwwwwwwwgo",
    "ogwwwwwwwwwwwwgo",
    "ogwwwwwwwwwwwwgo",
    ".ogwwwwwwwwwwgo.",
    "..ogvvvvvvvvgo..",
    "...ooggggggoo...",
    ".....oooooo.....",
]
C_FISH = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "......FF........",
    ".....bbbbbF..F..",
    "....bebbbbbFFF..",
    "....bbbbbbbFFF..",
    ".....bbbbbF..F..",
    "......FF........",
    "................",
    "................",
    "................",
]
C_INSIDE = [(x, y) for y in range(6, 13) for x in range(2, 14)] + [(x, 13) for x in range(4, 12)]
C_FRONT = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "....sssssssss...",
    "...H............",
    "..HH..........h.",
    "..H...........h.",
    "..H.............",
    "..h.............",
    "................",
    "................",
    "................",
    "................",
    "................",
]

EMPTY = ["." * S] * S


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


def _frog(base, front, dx, dy, water_rows=None):
    """The dwarf frog bucket: the container with a small frog in the water (and the glass in front)."""
    img = base.copy()
    if water_rows is not None:
        # A: cover the fish area of the old iron bucket with water first (as in M14)
        water = np.array([60, 90, 200, 255], np.uint8)
        for y in range(4, 9):
            for x in range(4, 12):
                if img[y, x, 3] > 0 and not (img[y, x, 0] > 150 and img[y, x, 1] > 150 and img[y, x, 2] > 150 and y < 5):
                    img[y, x] = water
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


def variant(name: str) -> dict[str, np.ndarray]:
    """The textures of one variant by file name (without .png)."""
    if name == "A":
        base = icons.grid(icons.BUCKET, IRON)
        body, fins = icons.fish_layers(icons.FISH_BUCKET)
        front = icons.grid(EMPTY, FRONT)
        frog = _frog(base, front, 0, 0, water_rows=True)
    elif name == "B":
        base = icons.grid(B_BASE, GLASS)
        body, fins = _fish(B_FISH, B_INSIDE)
        front = icons.grid(B_FRONT, FRONT)
        frog = _frog(base, front, 0, 4)
    elif name == "C":
        base = icons.grid(C_BASE, GLASS)
        body, fins = _fish(C_FISH, C_INSIDE)
        front = icons.grid(C_FRONT, FRONT)
        frog = _frog(base, front, 0, 3)
    else:
        raise ValueError(name)
    return {"betta_bucket": base, "betta_bucket_fish": body, "betta_bucket_fish_fins": fins,
            "betta_bucket_glass": front, "dwarf_frog_bucket": frog}


def write_all() -> list:
    written = []
    for v in ("A", "B", "C"):
        out = ITEM_TEX if v == ACTIVE else VARIANT_DIR / v.lower()
        for name, img in variant(v).items():
            written.append(save_png(img, out / f"{name}.png"))
    return written


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
