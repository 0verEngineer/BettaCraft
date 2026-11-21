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


TURTLE_SHELL = [(6, 5), (7, 5), (8, 5), (9, 5), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7),
                (6, 8), (7, 8), (8, 8), (9, 8)]
TURTLE_SKIN = [(11, 6), (12, 6), (11, 7), (4, 6), (5, 8), (10, 8), (4, 8)]


def _turtle(base, front, dx, dy, shell=(70, 58, 38), dark=(40, 33, 22), skin=(58, 58, 50), stripe=(210, 200, 150), patch=None):
    """M31: a turtle bucket: a small turtle in the water (domed shell, head to the right, feet); part b colours it per
    species (the slider with its red ear patch, Reeves' turtle with the yellow head line)."""
    img = base.copy()
    shell, dark, skin = (np.array([*c, 255], np.uint8) for c in (shell, dark, skin))
    for (x, y) in TURTLE_SHELL:
        img[y + dy, x + dx] = dark if (x + y) % 3 == 0 else shell
    for (x, y) in TURTLE_SKIN:
        img[y + dy, x + dx] = skin
    img[6 + dy, 12 + dx] = np.array([*stripe, 255], np.uint8)  # the pale stripe of the head
    if patch is not None:
        img[7 + dy, 11 + dx] = np.array([*patch, 255], np.uint8)
    return _over(img, front)


BOX_WOOD, BOX_DARK, BOX_HOLE = (176, 132, 82), (120, 86, 50), (40, 28, 18)


def _box(tortoise: bool):
    """M31 part b: the terrarium box: a wooden crate with a row of air holes in its lid; filled, a tortoise looks out."""
    img = np.zeros((S, S, 4), np.uint8)
    wood, dark, hole = (np.array([*c, 255], np.uint8) for c in (BOX_WOOD, BOX_DARK, BOX_HOLE))
    for y in range(5, 15):
        for x in range(1, 15):
            edge = x in (1, 14) or y in (5, 14)
            seam = y == 10 and not edge  # the joint between the two boards
            img[y, x] = dark if edge or seam else wood
            if not edge and (x * 7 + y * 3) % 11 == 0:
                img[y, x] = (wood * 0.9).astype(np.uint8)
    for x in range(2, 14):  # the lid, a little wider
        img[4, x] = dark
        img[3, x] = wood if 2 < x < 13 else dark
    for x in (3, 6, 9, 12):  # a row of air holes under the lid
        img[7, x] = hole
    if tortoise:
        shell, black, skin = (np.array([*c, 255], np.uint8) for c in ((200, 154, 70), (30, 24, 14), (118, 110, 82)))
        for x in range(3, 11):  # the dome above the open lid, black scute seams
            img[2, x] = black if x in (5, 8) else shell
        for x in range(4, 10):
            img[1, x] = black if x == 7 else shell
        for x in range(5, 9):
            img[0, x] = shell
        img[2, 11] = skin  # the head looking out to the right
        img[1, 11] = skin
        img[1, 12] = skin
        img[2, 12] = skin
        img[1, 13] = np.array([20, 18, 12, 255], np.uint8)
    return img


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
    turtle = _turtle(base, front, 0, 4)
    slider = _turtle(base, front, 0, 4, shell=(70, 86, 40), dark=(150, 140, 60), skin=(62, 84, 44), stripe=(220, 200, 80), patch=(200, 36, 26))
    reeves = _turtle(base, front, 0, 4, shell=(72, 52, 30), dark=(110, 82, 50), skin=(52, 52, 44), stripe=(170, 186, 80))
    return {"betta_bucket": base, "betta_bucket_fish": body, "betta_bucket_fish_fins": fins,
            "betta_bucket_glass": front, "dwarf_frog_bucket": frog, "musk_turtle_bucket": turtle,
            "slider_turtle_bucket": slider, "reeves_turtle_bucket": reeves, "hermann_tortoise_box": _box(True), "terrarium_box": _box(False)}


def write_all() -> list:
    return [save_png(img, ITEM_TEX / f"{name}.png") for name, img in textures().items()]


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
