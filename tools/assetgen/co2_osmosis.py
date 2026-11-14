"""M25 part b assets: CO2 dosing (bio CO2 bottle, CO2 cylinder with regulator, bubble counter and solenoid valve, the
filling station) and the reverse osmosis unit (cartridges, membrane, prefilter), the item icons (yeast, solenoid valve,
membrane, prefilter, bucket of RO water).

Deterministic pixel grids and procedural faces, no vanilla pixels.

Run from the repo root:  py tools/assetgen/co2_osmosis.py
"""
from __future__ import annotations

import math

import numpy as np

from common import BLOCK_TEX, ITEM_TEX, save_png
from icons import grid
from plumbing import _img, _rgb, _rng, steel

K = "#1c2228"

# ---------------------------------------------------------------------------------------------- bio CO2 bottle

BREW = {"empty": None, "mixed": "#c8d8e0", "fermenting": "#d8b860", "spent": "#8a7a5a"}


def bio_side(stage):
    """The glass bottle: clear glass with a highlight; the brew fills it up to 3/4 (foam on top while it ferments)."""
    r = _rng(2500 + len(stage))
    brew = BREW[stage]

    def px(x, y):
        if x in (0, 15):
            return _rgb("#a8c8d0")
        if x == 3 and 2 <= y <= 12:
            return _rgb("#f4fbff")
        if brew is not None and y >= 5:
            c = _rgb(brew) * (0.88 + 0.15 * r.random())
            if stage == "fermenting" and y == 5:
                return _rgb("#f2ead0")
            if stage == "fermenting" and r.random() < 0.08:
                return _rgb("#fff6dc")
            return c
        return _rgb("#d8eef2") * (0.95 + 0.05 * r.random())

    def alpha(x, y):
        if x in (0, 15) or (brew is not None and y >= 5) or (x == 3 and 2 <= y <= 12):
            return 255
        return 110
    return _img(px, alpha)


def bio_cap():
    """Rubber cap and hose: dark grey with a lighter rim."""
    r = _rng(2510)

    def px(x, y):
        v = 0.9 + 0.15 * r.random()
        if x in (0, 15) or y in (0, 15):
            return _rgb("#4a4e54") * v
        return _rgb("#2c2f33") * v
    return _img(px)


# ---------------------------------------------------------------------------------------------- CO2 cylinder

def cylinder_side():
    """Painted steel cylinder: dark grey with a white shoulder band and a vertical highlight."""
    r = _rng(2520)

    def px(x, y):
        v = 0.92 + 0.08 * r.random()
        if y <= 2:
            return _rgb("#e8eaec") * v
        if x in (4, 5):
            return _rgb("#7a8590") * v
        if x in (0, 15):
            return _rgb("#2a3036") * v
        return _rgb("#48525c") * v
    return _img(px)


def cylinder_top():
    """The neck: brushed brass."""
    r = _rng(2521)

    def px(x, y):
        v = 0.9 + 0.12 * r.random()
        if x in (0, 15) or y in (0, 15):
            return _rgb("#8a6a2a") * v
        return _rgb("#c8a048") * v
    return _img(px)


def regulator():
    """Pressure regulator: chrome body with a round gauge (white dial, red needle)."""
    r = _rng(2522)

    def px(x, y):
        d = math.hypot(x - 7.5, y - 7.5)
        if d < 4.5:
            if abs((x - 7.5) - (7.5 - y)) < 0.8 and x >= 7 and y <= 8:
                return _rgb("#d02a2a")
            return _rgb("#f2f2ee")
        if d < 5.6:
            return _rgb("#30363c")
        v = 0.9 + 0.12 * r.random()
        return _rgb("#b8c0c8") * v
    return _img(px)


def bubble_counter():
    """Bubble counter: a small glass tube with water and bubbles."""
    def px(x, y):
        if x in (0, 15) or y in (0, 15):
            return _rgb("#c8d8e0")
        if (x * 7 + y * 3) % 11 == 0:
            return _rgb("#f4fbff")
        return _rgb("#5aa8e0")
    return _img(px)


def solenoid_block():
    """Solenoid valve: a black coil housing with a red wire."""
    r = _rng(2523)

    def px(x, y):
        if y in (7, 8):
            return _rgb("#c83a2a")
        v = 0.85 + 0.2 * r.random()
        return _rgb("#1e2226") * v
    return _img(px)


# ---------------------------------------------------------------------------------------------- filling station

def station_side():
    img = steel(2530, "#a8b0b8")
    for y in range(4, 12):
        if y % 2 == 0:
            for x in range(3, 13):
                img[y, x, :3] = (88, 96, 104)
    return img


def station_top():
    """The flue gas hood: a grille over a soot-dark intake."""
    r = _rng(2531)

    def px(x, y):
        if x in (0, 15) or y in (0, 15):
            return _rgb("#6c747c")
        if (x + y) % 3 == 0:
            return _rgb("#2a2c2e") * (0.9 + 0.2 * r.random())
        return _rgb("#8c949c") * (0.92 + 0.1 * r.random())
    return _img(px)


def station_front(kind):
    """Front: a bay with a pressure gauge on top; 'loaded' shows a cylinder in it, 'active' also a lit indicator."""
    img = steel(2532, "#b4bcc4")
    r = _rng(2533)
    for y in range(5, 15):
        for x in range(3, 13):
            if x in (3, 12) or y == 14:
                c = _rgb("#40464c")
            elif kind == "empty":
                c = _rgb("#1a1d20") * (0.9 + 0.15 * r.random())
            else:
                c = _rgb("#48525c") * (0.9 + 0.1 * r.random())
                if x in (6, 7):
                    c = _rgb("#7a8590")
                if y <= 6:
                    c = _rgb("#e8eaec")
            img[y, x, :3] = np.clip(c, 0, 255).astype(np.uint8)
    for y in range(1, 4):
        for x in range(1, 15):
            d = math.hypot(x - 4, y - 2)
            if d < 1.7:
                img[y, x, :3] = (242, 242, 236)
    lamp = (90, 230, 120) if kind == "active" else (60, 70, 66)
    for y in range(1, 3):
        for x in range(11, 14):
            img[y, x, :3] = lamp
    return img


# ---------------------------------------------------------------------------------------------- reverse osmosis unit

def ro_front(active):
    """A white board with three cartridges (sediment, carbon, carbon) and the long membrane housing below."""
    r = _rng(2540)
    cart = ["#e8e4dc", "#30343a", "#30343a"]

    def px(x, y):
        base = _rgb("#e6e9ec") * (0.95 + 0.05 * r.random())
        if x in (0, 15) or y in (0, 15):
            return base * 0.75
        for i, cx in enumerate((3, 7, 11)):
            if cx - 1 <= x <= cx + 1 and 1 <= y <= 9:
                if y == 1:
                    return _rgb("#2c6fb8")
                c = _rgb(cart[i]) * (0.9 + 0.1 * r.random())
                if x == cx - 1:
                    c = c * 1.15
                return c
        if 2 <= x <= 13 and 11 <= y <= 13:
            if y == 12 and active and (x + 1) % 3 == 0:
                return _rgb("#a8e0ff")
            return _rgb("#3a8ad8") if y == 12 else _rgb("#d0d6dc")
        return base
    return _img(px)


def ro_side():
    img = steel(2541, "#d4d9de")
    for y in (4, 11):
        for x in range(2, 14):
            img[y, x, :3] = (44, 111, 184)
    return img


def ro_top():
    return steel(2542, "#c8ced4")


# ---------------------------------------------------------------------------------------------- items

YEAST = [
    "................",
    "................",
    "................",
    "................",
    "................",
    ".....oooooo.....",
    "...oowyywyyoo...",
    "..owyywyyyywyo..",
    "..oyyyyywyyyyo..",
    "..obbbbbbbbbbo..",
    "...obbbbbbbbo...",
    "....obbbbbbo....",
    ".....oooooo.....",
    "................",
    "................",
    "................",
]
YEAST_P = {"o": "#3a2a1c", "y": "#e8d8a8", "w": "#f8f0d8", "b": "#9a6a3a"}

SOLENOID = [
    "................",
    "................",
    "......kkkk......",
    ".....kddddk.....",
    ".....kdrrdk.....",
    ".....kddddk.....",
    ".....kdrrdk.....",
    ".....kddddk.....",
    "..kkkksssskkkk..",
    "..kcccsssscccc..",
    "..kkkksssskkkk..",
    "......ssss......",
    "................",
    "................",
    "................",
    "................",
]
SOLENOID_P = {"k": K, "d": "#2a2e33", "r": "#c83a2a", "s": "#b8c0c8", "c": "#c8a048"}

MEMBRANE = [
    "................",
    "..kkkkkkkkkkkk..",
    "..kbwwwwwwwwbk..",
    "..kbwgwgwgwgbk..",
    "..kbwwwwwwwwbk..",
    "..kbwgwgwgwgbk..",
    "..kbwwwwwwwwbk..",
    "..kbwgwgwgwgbk..",
    "..kbwwwwwwwwbk..",
    "..kbwgwgwgwgbk..",
    "..kbwwwwwwwwbk..",
    "..kbwgwgwgwgbk..",
    "..kbwwwwwwwwbk..",
    "..kkkkkkkkkkkk..",
    "................",
    "................",
]
MEMBRANE_P = {"k": K, "b": "#2c6fb8", "w": "#f2f4f6", "g": "#c8d0d8"}

PREFILTER = [
    "................",
    ".....kkkkkk.....",
    "....kbbbbbbk....",
    "....kwwwwwwk....",
    "....kwcwcwck....",
    "....kwwwwwwk....",
    "....kcwcwcwk....",
    "....kwwwwwwk....",
    "....kwcwcwck....",
    "....kwwwwwwk....",
    "....kcwcwcwk....",
    "....kwwwwwwk....",
    "....kbbbbbbk....",
    ".....kkkkkk.....",
    "................",
    "................",
]
PREFILTER_P = {"k": K, "b": "#2c6fb8", "w": "#e8e4dc", "c": "#30343a"}

RO_BUCKET = [
    "................",
    "................",
    "................",
    "....kkkkkkkk....",
    "...kGggggggGk...",
    "...kwWwwWwwwk...",
    "...kWwwbWwwWk...",
    "...kgwWwwwbgk...",
    "....kgggggGk....",
    "....kGgggggk....",
    "....kgggggGk....",
    ".....kgggGk.....",
    ".....kGgggk.....",
    "......kkkk......",
    "................",
    "................",
]
RO_BUCKET_P = {"k": "#1e2328", "g": "#a8b0b8", "G": "#d8dee4", "w": "#8ccaf0", "W": "#c4e6fa", "b": "#ffffff"}


def write_all():
    out = []
    blocks = {f"bio_co2_{s}": bio_side(s) for s in BREW}
    blocks.update({
        "bio_co2_cap": bio_cap(), "co2_cylinder_side": cylinder_side(), "co2_cylinder_top": cylinder_top(), "co2_regulator": regulator(),
        "co2_bubble_counter": bubble_counter(), "co2_solenoid_block": solenoid_block(),
        "co2_station_side": station_side(), "co2_station_top": station_top(),
        "co2_station_front_empty": station_front("empty"), "co2_station_front_loaded": station_front("loaded"),
        "co2_station_front_active": station_front("active"),
        "ro_unit_front": ro_front(False), "ro_unit_front_active": ro_front(True), "ro_unit_side": ro_side(), "ro_unit_top": ro_top(),
    })
    for name, img in blocks.items():
        out.append(save_png(img, BLOCK_TEX / f"{name}.png"))
    icons = {
        "yeast": grid(YEAST, YEAST_P), "co2_solenoid": grid(SOLENOID, SOLENOID_P), "ro_membrane": grid(MEMBRANE, MEMBRANE_P),
        "ro_prefilter": grid(PREFILTER, PREFILTER_P), "ro_water_bucket": grid(RO_BUCKET, RO_BUCKET_P),
    }
    for name, img in icons.items():
        out.append(save_png(img, ITEM_TEX / f"{name}.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print(p)
