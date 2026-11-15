"""M26 part b assets: the food maker (case, feed hopper, crank) and the new foods (flakes, granules, shrimp food and the
three frozen foods).

Deterministic pixel grids and procedural faces, no vanilla pixels.

Run from the repo root:  py tools/assetgen/food_maker.py
"""
from __future__ import annotations

import numpy as np

from common import BLOCK_TEX, ITEM_TEX, save_png
from icons import grid
from plumbing import _img, _rgb, _rng, steel

S = 16


# ---------------------------------------------------------------------------------------------- blocks

def maker_front():
    """Green-enamelled steel case with a window on the mill (food crumbs inside) and the spout at the bottom."""
    r = _rng(2651)

    def px(x, y):
        base = _rgb("#4e7a5a") * (0.93 + 0.07 * r.random())
        if x in (0, 15) or y in (0, 15):
            return base * 0.7
        if 3 <= x <= 12 and 2 <= y <= 8:  # window
            if x in (3, 12) or y in (2, 8):
                return _rgb("#2a2e33")
            crumbs = ("#c49a5a", "#8a5a2a", "#d8c070", "#6a8a3a")
            return _rgb(crumbs[int(r.integers(0, 4))]) * (0.85 + 0.15 * r.random()) if r.random() < 0.6 else _rgb("#3a3a36")
        if 6 <= x <= 9 and 11 <= y <= 14:  # spout
            return _rgb("#9aa2aa") * (1.1 if x == 6 else 0.9 if x == 9 else 1.0)
        if y == 10 and x in (2, 13):  # rivets
            return _rgb("#c8ced4")
        return base
    return _img(px)


def maker_side():
    r = _rng(2652)

    def px(x, y):
        base = _rgb("#4a7456") * (0.93 + 0.07 * r.random())
        if x in (0, 15) or y in (0, 15):
            return base * 0.7
        if y in (4, 11) and 2 <= x <= 13:  # pressed ribs
            return base * 1.2
        if (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
            return _rgb("#c8ced4")
        return base
    return _img(px)


def maker_top():
    img = steel(2653, "#a8b0b8")
    img[3:13, 3:13, :3] = (60, 64, 70)
    return img


def maker_hopper():
    """The feed funnel: steel with the dark opening and some grain at its rim."""
    r = _rng(2654)

    def px(x, y):
        v = _rgb("#b4bcc4") * (0.9 + 0.1 * r.random())
        if y <= 1:
            return v * 1.15
        if 3 <= x <= 12 and 3 <= y <= 12:
            return _rgb("#2c2f33") if r.random() < 0.8 else _rgb("#d8c070")
        return v * (0.85 if x in (0, 15) else 1.0)
    return _img(px)


def crank():
    r = _rng(2655)
    return _img(lambda x, y: _rgb("#3a3c42") * (0.85 + 0.25 * r.random()) * (1.2 if x % 4 == 0 else 1.0))


def knob():
    r = _rng(2656)
    return _img(lambda x, y: _rgb("#a8282a") * (0.85 + 0.2 * r.random()) * (1.25 if y < 4 else 1.0))


# ---------------------------------------------------------------------------------------------- items

TUB_ROWS = [
    "................",
    "................",
    "....kkkkkkkk....",
    "...kLLLLLLLLk...",
    "...kkkkkkkkkk...",
    "...kWffffffWk...",
    "...kWfFffFfWk...",
    "...kWffFfffWk...",
    "...kWfffFffWk...",
    "...kWFfffFfWk...",
    "...kWffFfffWk...",
    "...kWfffffFWk...",
    "...kWWWWWWWWk...",
    "....kkkkkkkk....",
    "................",
    "................",
]


def tub(food, food2, label, lid="#d8d8d0"):
    """A food tub with a lid and the food behind the label window."""
    return grid(TUB_ROWS, {"k": "#2e2e2c", "L": lid, "W": label, "f": food, "F": food2})


FLAT_ROWS = [
    "................",
    "................",
    "................",
    "................",
    "....kkkkkkkk....",
    "...kIIIIIIIIk...",
    "..kIiFiiiFiiIk..",
    "..kIiiiFiiiiIk..",
    "..kIiFiiiiFiIk..",
    "..kIiiiFiiiiIk..",
    "..kIiFiiiiFiIk..",
    "...kIIIIIIIIk...",
    "....kkkkkkkk....",
    "................",
    "................",
    "................",
]


def frozen(food, food2):
    """A frozen blister pack: frosty plastic with the frozen food cubes."""
    return grid(FLAT_ROWS, {"k": "#5a6a74", "I": "#e8f4fa", "i": food, "F": food2})


def write_all():
    out = []
    blocks = {"food_maker_front": maker_front(), "food_maker_side": maker_side(), "food_maker_top": maker_top(),
              "food_maker_hopper": maker_hopper(), "food_maker_crank": crank(), "food_maker_knob": knob()}
    for name, img in blocks.items():
        out.append(save_png(img, BLOCK_TEX / f"{name}.png"))
    items = {
        "fish_flakes": tub("#e0a040", "#c84a2a", "#f0e8c0", "#3a8ad0"),
        "food_granules": tub("#7a4a2a", "#a86a3a", "#e8d8b0", "#c86a2a"),
        "shrimp_food": tub("#5a8a3a", "#d87a4a", "#f0e0d0", "#d04a40"),
        "frozen_mosquito_larvae": frozen("#b8323a", "#7e1e26"),
        "frozen_artemia": frozen("#e08a6a", "#c06048"),
        "frozen_daphnia": frozen("#b08a50", "#7a5a30"),
        "shell_food": tub("#e8e0c8", "#8a7058", "#d8e8f0", "#6a8aa0"),  # M29 part b: crushed shells for the puffers
    }
    for name, img in items.items():
        out.append(save_png(img, ITEM_TEX / f"{name}.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print(p)
