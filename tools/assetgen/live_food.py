"""M26 part a assets: the live food cultures (tubifex dish, daphnia barrel), the live food pump and the live food items
(tubifex, daphnia, mulm, green water and its culture).

Deterministic pixel grids and procedural faces, no vanilla pixels.

Run from the repo root:  py tools/assetgen/live_food.py
"""
from __future__ import annotations

import numpy as np

from common import BLOCK_TEX, ITEM_TEX, save_png
from icons import BOTTLE, BOTTLE_ROWS, JAR, JAR_ROWS, grid
from plumbing import _img, _rgb, _rng, steel

S = 16


# ---------------------------------------------------------------------------------------------- blocks

def dish_side():
    """Fired terracotta with a lighter rim line and a darker foot."""
    r = _rng(2601)

    def px(x, y):
        base = _rgb("#b4683e") * (0.92 + 0.08 * r.random())
        if y <= 1:
            return base * 1.15
        if y >= 14:
            return base * 0.75
        return base
    return _img(px)


def dish_bottom():
    r = _rng(2602)
    return _img(lambda x, y: _rgb("#9a5634") * (0.9 + 0.1 * r.random()))


def barrel_side():
    """Dark oak staves with two iron hoops."""
    r = _rng(2603)

    def px(x, y):
        shade = [0.0, 0.08, -0.05, 0.04][(x // 4) % 4]
        v = _rgb("#6a4a2c") * (1 + shade + 0.06 * r.random())
        if x % 4 == 0:
            v = v * 0.7
        if y in (2, 3, 12, 13):
            v = _rgb("#4a4c52") * (1.25 if y in (2, 12) else 1.0) * (0.95 + 0.05 * r.random())
        return v
    return _img(px)


def barrel_bottom():
    r = _rng(2604)

    def px(x, y):
        v = _rgb("#5e4226") * (0.92 + 0.08 * r.random())
        return v * 0.75 if y % 5 == 0 else v
    return _img(px)


def _specks(seed, colours, count, size):
    """Transparent overlay: small animals (size 1: a dot, 2: a short worm)."""
    r = np.random.default_rng(seed)
    img = np.zeros((S, S, 4), np.uint8)
    for _ in range(count):
        x, y = int(r.integers(2, S - 3)), int(r.integers(2, S - 3))
        c = colours[int(r.integers(0, len(colours)))]
        img[y, x] = (*c, 255)
        if size > 1:
            for _ in range(size - 1):
                dx, dy = [(1, 0), (0, 1), (1, 1), (-1, 1)][int(r.integers(0, 4))]
                x, y = max(0, min(S - 1, x + dx)), max(0, min(S - 1, y + dy))
                img[y, x] = (*c, 240)
    return img


def tubifex_overlay(stage):
    return _specks(2610 + stage, [(176, 40, 36), (206, 64, 52), (140, 28, 30)], [0, 8, 18, 30][stage], 3)


def daphnia_overlay(stage):
    # M29.1: lighter, warmer water fleas, so they stand out on the green water (the barrel icon reads as daphnia)
    return _specks(2620 + stage, [(240, 204, 132), (222, 128, 74), (252, 232, 178)], [0, 10, 24, 44][stage], 1)


def pump_front():
    """Steel case with a small window that shows the suspension and a timer dial."""
    r = _rng(2630)

    def px(x, y):
        base = _rgb("#c8ced4") * (0.94 + 0.06 * r.random())
        if x in (0, 15) or y in (0, 15):
            return base * 0.75
        if 3 <= x <= 7 and 4 <= y <= 11:  # window with the suspension
            if x in (3, 7) or y in (4, 11):
                return _rgb("#2a2e33")
            return _rgb("#7a8a46") * (0.85 + 0.15 * r.random()) if (x + y) % 3 else _rgb("#c49a5a")
        if 10 <= x <= 13 and 5 <= y <= 8:  # the dial
            if (x, y) in ((11, 6), (12, 6), (11, 7)):
                return _rgb("#d83a2a")
            return _rgb("#f0f0ea")
        if 10 <= x <= 13 and y == 10:
            return _rgb("#3ad06a") if x == 11 else _rgb("#3a3e44")
        return base
    return _img(px)


def pump_side():
    img = steel(2631, "#bcc3ca")
    for y in range(6, 10):
        img[y, 6:10, :3] = (90, 160, 200) if y in (6, 9) else (60, 120, 170)
    return img


def pump_top():
    return steel(2632, "#c4cad0")


# ---------------------------------------------------------------------------------------------- items

TUBIFEX_ROWS = [
    "................",
    "................",
    "......r.........",
    ".....rR...r.....",
    ".....R...rR.....",
    "....rR..rR......",
    "....R..rR..r....",
    "...rR.rR..rR....",
    "...R.rR..rR.....",
    "..mmrRmmmRmmm...",
    ".mmMRmmMRmMmmm..",
    ".mMmmmMmmmmMmm..",
    "..mmMmmmMmmmm...",
    "...mmmmmmmmm....",
    "................",
    "................",
]


def tubifex():
    return grid(TUBIFEX_ROWS, {"r": "#c23a30", "R": "#8e2424", "m": "#4a3a2a", "M": "#5e4a36"})


def daphnia():
    """A jar of green water with the brown dots of the water fleas."""
    pal = dict(JAR)
    pal.update({"W": "#8fbf6ac0", "w": "#6a9e4ab8", "v": "#4e7e36c0", "l": "#c8904a", "r": "#a8743a"})
    return grid(JAR_ROWS, pal)


MULM_ROWS = [
    "................",
    "................",
    "................",
    "................",
    "......dddd......",
    "....ddmmmmdd....",
    "...dmmMmmmmmd...",
    "..dmmmmmmMmmmd..",
    "..dmMmmmmmmmMd..",
    "..dmmmmMmmmmmd..",
    "...dmmmmmmMmd...",
    "....ddmmmmdd....",
    "......dddd......",
    "................",
    "................",
    "................",
]


def mulm():
    return grid(MULM_ROWS, {"d": "#3a2a1c", "m": "#5a4630", "M": "#7a6244"})


def green_water(culture: bool):
    pal = dict(BOTTLE)
    if culture:
        pal.update({"W": "#a2c48a", "w": "#86ad6a", "v": "#6a9050", "d": "#f0f0e8", "s": "#b8d49c"})
    else:
        pal.update({"W": "#6ab03a", "w": "#4e9a2a", "v": "#3a7a1e", "d": "#2e6a18", "s": "#8ac85a"})
    return grid(BOTTLE_ROWS, pal)


def write_all():
    out = []
    blocks = {"tubifex_dish_side": dish_side(), "tubifex_dish_bottom": dish_bottom(), "daphnia_barrel_side": barrel_side(),
              "daphnia_barrel_bottom": barrel_bottom(), "live_food_pump_front": pump_front(), "live_food_pump_side": pump_side(),
              "live_food_pump_top": pump_top()}
    for st in (1, 2, 3):
        blocks[f"culture_tubifex_{st}"] = tubifex_overlay(st)
        blocks[f"culture_daphnia_{st}"] = daphnia_overlay(st)
    for name, img in blocks.items():
        out.append(save_png(img, BLOCK_TEX / f"{name}.png"))
    items = {"tubifex": tubifex(), "daphnia": daphnia(), "mulm": mulm(), "green_water_bottle": green_water(False),
             "green_water_culture": green_water(True)}
    for name, img in items.items():
        out.append(save_png(img, ITEM_TEX / f"{name}.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print(p)
