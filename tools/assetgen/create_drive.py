"""M41 assets: the drive adapter (Create's rotation for the tank buffer) and the flange of the Create shaft on a cabinet.

A Create-like workshop look made from scratch: a grey stone casing in a dark wooden frame, brass bearing ring and
mounting plate with bolts, a grey alloy shaft with a groove. Deterministic pixel grids and procedural faces, no pixels of
Create or vanilla.

Run from the repo root:  py tools/assetgen/create_drive.py
"""
from __future__ import annotations

import numpy as np

from common import BLOCK_TEX, save_png
from plumbing import _img, _rgb, _rng

STONE = "#9a9c94"      # the casing's stone (andesite-like grey)
WOOD = "#5c4630"       # the frame (dark wood)
BRASS = "#c8a04a"
BRASS_DARK = "#8a6624"
BRASS_LIGHT = "#ecd08a"
ALLOY = "#a2a8a4"      # the shaft


def _stone(r, x, y):
    v = _rgb(STONE) * (0.9 + 0.1 * r.random())
    if (x * 7 + y * 3) % 11 == 0:  # a few darker grains
        v = v * 0.88
    return v


def casing():
    """Stone casing in a two pixel wooden frame with light corner pegs."""
    r = _rng(4101)

    def px(x, y):
        edge = min(x, y, 15 - x, 15 - y)
        if edge == 0:
            return _rgb(WOOD) * 0.72
        if edge == 1:
            v = _rgb(WOOD) * (0.92 + 0.12 * r.random())
            if (x + y) % 5 == 0:
                v = v * 0.85  # grain
            if (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
                return _rgb("#b8b2a2")  # pegs
            return v
        return _stone(r, x, y)
    return _img(px)


def front():
    """The casing's front: the frame, the stone and a dark round hole for the shaft with a brass rim."""
    base = casing()
    r = _rng(4102)
    for y in range(16):
        for x in range(16):
            d2 = (x - 7.5) ** 2 + (y - 7.5) ** 2
            if d2 <= 5.5:
                base[y, x, :3] = np.clip(_rgb("#26241f") * (0.9 + 0.1 * r.random()), 0, 255)
            elif d2 <= 12.5:
                c = BRASS_LIGHT if x + y < 14 else BRASS if x + y < 17 else BRASS_DARK
                base[y, x, :3] = np.clip(_rgb(c), 0, 255)
    return base


def plate():
    """Brass mounting plate: a darker rim, brushed face, four bolts."""
    r = _rng(4103)
    streak = r.random(16) * 0.08

    def px(x, y):
        if x in (0, 15) or y in (0, 15):
            return _rgb(BRASS_DARK)
        v = _rgb(BRASS) * (0.93 + streak[y] + 0.03 * r.random())
        for bx, by in ((3, 3), (12, 3), (3, 12), (12, 12)):
            dx, dy = x - bx, y - by
            if dx * dx + dy * dy <= 1:
                return _rgb("#6e6a62") * (1.15 if (dx, dy) == (-1, 0) or (dx, dy) == (0, -1) else 1.0)
        return v
    return _img(px)


def ring():
    """Brass bearing ring: light at the top left, dark at the bottom right."""
    r = _rng(4104)

    def px(x, y):
        t = (x + y) / 30.0
        c = _rgb(BRASS_LIGHT) * (1 - t) + _rgb(BRASS_DARK) * t
        if x in (0, 15) or y in (0, 15):
            c = c * 0.8
        return c * (0.95 + 0.05 * r.random())
    return _img(px)


def shaft():
    """Grey alloy shaft: lengthwise (v) light edge and a groove ring every eight pixels."""
    r = _rng(4105)

    def px(x, y):
        v = _rgb(ALLOY) * (0.92 + 0.06 * r.random())
        if x in (0, 3, 4, 7, 8, 11, 12, 15):
            v = v * 0.86
        if x in (1, 5, 9, 13):
            v = v * 1.1
        if y % 8 == 6:
            v = v * 0.7  # the groove
        return v
    return _img(px)


def coupling():
    """The flange of the Create shaft on a cabinet: dark iron with a brass hub face and six bolts."""
    r = _rng(4107)

    def px(x, y):
        d2 = (x - 7.5) ** 2 + (y - 7.5) ** 2
        if x in (0, 15) or y in (0, 15):
            return _rgb("#3c3e40")
        if d2 <= 9:
            return _rgb("#2a2826")  # the shaft hole
        if d2 <= 20:
            return _rgb(BRASS) * (1.1 if x + y < 15 else 0.85)
        v = _rgb("#6a6e72") * (0.9 + 0.1 * r.random())
        for bx, by in ((2, 2), (13, 2), (2, 13), (13, 13), (7, 1), (8, 14)):
            if abs(x - bx) + abs(y - by) <= 0:
                return _rgb("#c8ccd0")
        return v
    return _img(px)


def write_all():
    out = []
    blocks = {"drive_adapter_casing": casing(), "drive_adapter_front": front(), "drive_adapter_plate": plate(),
              "drive_adapter_ring": ring(), "drive_adapter_shaft": shaft(),
              "drive_coupling": coupling()}
    for name, img in blocks.items():
        out.append(save_png(img, BLOCK_TEX / f"{name}.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print(p)
