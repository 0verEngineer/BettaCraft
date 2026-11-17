"""M49 assets, part a: the sump (a glass box with baffles and water), the Chaetomorpha of its refugium, coral rubble and the
calcium reactor (a dark pump foot, a clear column of coral rubble, bubbles while it runs).

Deterministic pixel grids and procedural faces, no vanilla pixels.

Run from the repo root:  py tools/assetgen/sump_brackish.py
"""
from __future__ import annotations

import numpy as np

from common import BLOCK_TEX, ITEM_TEX, save_png
from icons import grid
from plumbing import _img, _rgb, _rng, steel

S = 16


def sump_icon():
    rows = [
        "................",
        "................",
        "................",
        "kkkkkkkkkkkkkkkk",
        "kgggggkgggkggggk",
        "kgwwwgkgwgkgggGk",
        "kwwwwwkwwwkwGwwk",
        "kwbwwwkwwwkGwGwk",
        "kwwwwwkwwwkwGwGk",
        "kwwwwwwwwwkwwGwk",
        "kwwwbwwwwwwwGwwk",
        "kwwwwwwwwwwwwwwk",
        "kwwwwwwwwwwwwwwk",
        "kkkkkkkkkkkkkkkk",
        "................",
        "................",
    ]
    return grid(rows, {"k": "#4a5a62", "g": "#c8e4ee", "w": "#5aa8d0", "b": "#d8f0ff", "G": "#3a9a3a"})


def chaeto_icon():
    """A tangled green ball of Chaetomorpha (spaghetti algae)."""
    r = _rng(4901)
    out = np.zeros((S, S, 4), np.uint8)
    cx, cy = 7.5, 8.5
    for i in range(46):
        a = r.random() * 6.283
        rad = 2.0 + r.random() * 4.6
        x = int(round(cx + np.cos(a) * rad * 1.05))
        y = int(round(cy + np.sin(a) * rad * 0.9))
        for dx, dy in ((0, 0), (1, 0), (0, 1)):
            xx, yy = x + dx, y + dy
            if 1 <= xx < 15 and 2 <= yy < 15:
                v = 0.75 + 0.35 * r.random()
                out[yy, xx] = (int(70 * v), int(170 * v), int(50 * v), 255)
    for y in range(4, 13):  # the denser middle
        for x in range(4, 12):
            if (x - cx) ** 2 + (y - cy) ** 2 < 10 and r.random() < 0.7:
                v = 0.6 + 0.3 * r.random()
                out[y, x] = (int(60 * v), int(150 * v), int(40 * v), 255)
    return out


def rubble_icon():
    """Pale broken coral pieces."""
    rows = [
        "................",
        "................",
        "................",
        "........ww......",
        ".......wWww.....",
        "...ww..wwws.....",
        "..wWws..ss......",
        "..wwss......w...",
        "...ss.....wWwe..",
        "......ww..wwwse.",
        ".....wWwe..sse..",
        "....wwwwse......",
        ".....ssse.......",
        "................",
        "................",
        "................",
    ]
    return grid(rows, {"w": "#e8e0d0", "W": "#fffaf0", "s": "#b8ac98", "e": "#8a806e"})


def reactor_base():
    img = steel(4902, "#3a4046")
    return img


def reactor_front():
    img = steel(4903, "#3a4046")
    for x in range(5, 11):  # the small display
        for y in range(1, 3):
            img[y + 9, x, :3] = (70, 200, 120) if (x + y) % 3 else (30, 80, 50)
    return img


def reactor_column(on: bool):
    """Clear glass with coral rubble inside; bubbles rise in the water while it runs."""
    r = _rng(4904)
    pieces = r.random((S, S))

    def px(x, y):
        if x in (0, 15):
            return _rgb("#9ab8c4")
        if y >= 3:  # the rubble bed
            v = pieces[y, x]
            if v < 0.25:
                return _rgb("#c8bca8")
            if v < 0.6:
                return _rgb("#e8e0d0") * (0.92 + 0.08 * r.random())
            return _rgb("#a8c8d8")
        return _rgb("#b8dce8")

    def alpha(x, y):
        if x in (0, 15):
            return 230
        return 255 if y >= 3 and pieces[y, x] < 0.6 else 150

    img = _img(px, alpha)
    if on:
        for (bx, by) in ((4, 2), (9, 5), (6, 9), (11, 11), (3, 13), (8, 1)):
            img[by, bx] = (240, 250, 255, 255)
    return img


def reactor_top():
    img = steel(4905, "#4a5258")
    for x in range(5, 11):
        for y in range(5, 11):
            img[y, x, :3] = (200, 220, 230)
    return img


def write_all():
    out = [
        save_png(sump_icon(), ITEM_TEX / "sump.png"),
        save_png(chaeto_icon(), ITEM_TEX / "chaetomorpha.png"),
        save_png(rubble_icon(), ITEM_TEX / "coral_rubble.png"),
        save_png(reactor_base(), BLOCK_TEX / "calcium_reactor_base.png"),
        save_png(reactor_front(), BLOCK_TEX / "calcium_reactor_front.png"),
        save_png(reactor_column(False), BLOCK_TEX / "calcium_reactor_column.png"),
        save_png(reactor_column(True), BLOCK_TEX / "calcium_reactor_column_on.png"),
        save_png(reactor_top(), BLOCK_TEX / "calcium_reactor_top.png"),
    ]
    return out


if __name__ == "__main__":
    for p in write_all():
        print(p)
