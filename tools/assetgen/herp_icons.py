"""M31 part c item icons: the amphibians and reptiles in their terrarium boxes (the crate of ``buckets._box`` with the
animal looking out over the lid), the feeder insects (crickets, fruit flies) and their cultures.

Run from the repo root:  py tools/assetgen/herp_icons.py
"""
from __future__ import annotations

import numpy as np

from buckets import _box
from common import ITEM_TEX, save_png

S = 16


def _c(hex_: str) -> np.ndarray:
    h = hex_.lstrip("#")
    return np.array([int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255], np.uint8)


def _paint(img, rows, palette, x0=0, y0=0):
    """Paints a small pixel map (``.`` = keep) onto the image at (x0, y0)."""
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch == ".":
                continue
            x, y = x0 + i, y0 + j
            if 0 <= x < S and 0 <= y < S:
                img[y, x] = _c(palette[ch])
    return img


def dart_frog_box():
    # a blue frog with black spots sitting on the lid, its black eyes up
    return _paint(_box(False), [
        "..k..k..",
        ".bBbbBb.",
        "bBkbbkBb",
        "dbbbbbbd",
    ], {"k": "#101216", "b": "#4f9ef2", "B": "#7fbcff", "d": "#1e46b4"}, 4, 0)


def horned_frog_box():
    # the wide round head with the little horns and the huge mouth over the lid
    return _paint(_box(False), [
        "..hk..kh..",
        ".gGGggGGg.",
        "gGbggggbGg",
        "gmmmmmmmmg",
    ], {"h": "#4a7a22", "k": "#2a1a10", "g": "#7ab238", "G": "#9ad050", "b": "#5a3a1e", "m": "#2a1e14"}, 3, 0)


def leopard_gecko_box():
    # the yellow, black-spotted head looking out to the right, the banded tail hanging out on the left
    img = _paint(_box(False), [
        "......yyyy..",
        ".....ykyyyk.",
        "t...yyyyyyye",
        "tT..wwwwwww.",
    ], {"y": "#ecc84a", "k": "#1a1612", "t": "#2a2420", "T": "#f2e2a0", "w": "#f4ecd8", "e": "#141210"}, 2, 0)
    return img


def crested_gecko_box():
    # the big triangular head with the crests over the eyes, the toe pads on the edge
    return _paint(_box(False), [
        ".c....c.",
        "cCbbbbCc",
        "bbebbebb",
        ".pbbbbp.",
    ], {"c": "#e8cc96", "C": "#d0a868", "b": "#b07038", "e": "#3a2a14", "p": "#c88a50"}, 4, 0)


def corn_snake_box():
    # a coil of the orange snake with red, black-bordered saddles over the lid, the head looking out
    return _paint(_box(False), [
        "........hhk.",
        "..oRRooRRoh.",
        ".oRkRooRkRo.",
        ".ooooooooo..",
    ], {"o": "#e08a4a", "R": "#c8301c", "k": "#141010", "h": "#d87038"}, 1, 0)


def crickets():
    """Three brown crickets with long antennae and hind legs."""
    img = np.zeros((S, S, 4), np.uint8)
    pal = {"b": "#6a4424", "B": "#8a5a30", "d": "#3a2410", "a": "#2a1a0c", "w": "#c8a070"}
    cricket = [
        "a.......",
        ".a......",
        "..bBBBw.",
        ".dbBBBBd",
        "d.d..d.d",
    ]
    _paint(img, cricket, pal, 1, 2)
    _paint(img, cricket, pal, 7, 6)
    _paint(img, cricket, pal, 2, 10)
    return img


def fruit_flies():
    """A slice of fruit with small flies (red eyes) sitting on it."""
    img = np.zeros((S, S, 4), np.uint8)
    _paint(img, [
        "....yyyyyy....",
        "..yYYYYYYYYy..",
        ".yYYYYYYYYYYy.",
        "gggggggggggggg",
    ], {"y": "#e6c860", "Y": "#f2dc8a", "g": "#7a9a40"}, 1, 10)
    fly = {"r": "#c02a1a", "b": "#5a4426", "w": "#d8dce0"}
    for x, y in ((3, 7), (8, 6), (11, 9), (6, 9), (2, 3), (10, 2)):
        _paint(img, ["wrw", ".b."], fly, x, y)
    return img


def cricket_culture():
    """A paper box with grass and a cricket on the lid."""
    img = np.zeros((S, S, 4), np.uint8)
    for y in range(6, 15):
        for x in range(2, 14):
            edge = x in (2, 13) or y in (6, 14)
            img[y, x] = _c("#b8a888") if edge else _c("#e8dcc0")
    for x in (4, 7, 10):  # air holes
        img[8, x] = _c("#5a4a30")
    _paint(img, ["g.g.gg.g", "gGgGgGgg"], {"g": "#4a8a2a", "G": "#6aaa3a"}, 4, 4)
    _paint(img, ["a.....", "bBBBw.", "d..d.d"], {"a": "#2a1a0c", "b": "#6a4424", "B": "#8a5a30", "w": "#c8a070", "d": "#3a2410"}, 6, 10)
    return img


def fruit_fly_culture():
    """A glass bottle with a piece of fruit at the bottom and flies inside."""
    img = np.zeros((S, S, 4), np.uint8)
    rows = [
        "....kkkk....",
        "....oggo....",
        "...oggggo...",
        "..oggggggo..",
        ".ogggggggggo",
        ".ogggggggggo",
        ".ogggggggggo",
        ".ogyYyyYyggo",
        ".oyyYYYYyyyo",
        "..oooooooooo",
    ]
    _paint(img, rows, {"k": "#8a6a44", "o": "#3b4f57", "g": "#d8eef2", "y": "#e6c860", "Y": "#c84a30"}, 2, 4)
    for x, y in ((5, 8), (8, 9), (7, 7), (10, 10)):
        img[y, x] = _c("#3a2a1a")
    return img


def _jar(filled: bool):
    """M31 part d: the mob jar: a glass jar with an iron lid (bars); full, a small zombie stands inside."""
    img = np.zeros((S, S, 4), np.uint8)
    rows = [
        "....LLLLLLLL....",
        "...LlLlLlLlLL...",
        "...LLLLLLLLLL...",
        "....oggggggo....",
        "...oggggggggo...",
        "..ohggggggggho..",
        ".ohgggggggggggo.",
        ".ohgggggggggggo.",
        ".ohgggggggggggo.",
        ".ohgggggggggggo.",
        ".ohgggggggggggo.",
        ".ohgggggggggggo.",
        ".ogggggggggggg o",
        "..ogggggggggggo.",
        "...oooooooooo...",
    ]
    rows[12] = ".ogggggggggggggo"[:16]
    _paint(img, rows, {"L": "#6e7378", "l": "#b8bec4", "o": "#3b4f57", "h": "#f4fbfc", "g": "#cfe6ea"}, 0, 1)
    for y in range(S):  # the glass lets the light through
        for x in range(S):
            if tuple(img[y, x, :3]) == tuple(_c("#cfe6ea")[:3]):
                img[y, x, 3] = 120
    if filled:
        _paint(img, [
            ".ggg.",
            ".kgk.",
            ".ggg.",
            "cCCCc",
            "gCCCg",
            ".bbb.",
            ".b.b.",
            ".b.b.",
        ], {"g": "#5a9a48", "k": "#1c2a18", "c": "#2e8a9a", "C": "#38a6b6", "b": "#3a3f8a"}, 5, 6)
    return img


def mob_jar():
    return _jar(False)


def mob_jar_filled():
    return _jar(True)


def mudskipper_box():
    # M49: the grey-olive mudskipper peeking over the lid, its two bulging eyes on top, a pectoral fin on the edge
    return _paint(_box(False), [
        "...ee.ee...",
        "..eKeeKe...",
        ".gggggggggf",
        "fgGgggGgggf",
        ".wwwwwwww..",
    ], {"e": "#5a5440", "K": "#141412", "g": "#6e6a52", "G": "#4c90d8", "f": "#4a4636", "w": "#d0c8b0"}, 2, 0)


def textures() -> dict[str, np.ndarray]:
    return {"dart_frog_box": dart_frog_box(), "horned_frog_box": horned_frog_box(), "leopard_gecko_box": leopard_gecko_box(),
            "crested_gecko_box": crested_gecko_box(), "corn_snake_box": corn_snake_box(), "crickets": crickets(),
            "fruit_flies": fruit_flies(), "cricket_culture": cricket_culture(), "fruit_fly_culture": fruit_fly_culture(),
            "mob_jar": mob_jar(), "mob_jar_filled": mob_jar_filled(), "mudskipper_box": mudskipper_box()}


def write_all() -> list:
    return [save_png(img, ITEM_TEX / f"{name}.png") for name, img in textures().items()]


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
