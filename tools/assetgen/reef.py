"""M20 assets: salt water and reef tanks.

Grey coral block textures (``textures/block/plant_leaf_<name>``: polyp mat, mushroom disc, tentacles, brain ridges, branch
skin; tinted per species like the M13 plants), two-layer coral frag icons per growth form (coral + the plug it sits on),
reef rock and live rock, the protein skimmer, the flow pump segment and the item icons (sea water bucket, sea salt,
calcium additive, flow pump). Deterministic, procedural, no vanilla pixels.

Run from the repo root:  py tools/assetgen/reef.py
"""
from __future__ import annotations

import math

import numpy as np

from aquatic_plants import _finish, _grey, _img, _rng
from common import BLOCK_TEX, ITEM_TEX, save_png

S = 16


# ---------------------------------------------------------------------------------------------- coral block textures (grey)

def coral_polyp(seed=2001):
    """A mat of polyps: round mouths (dark centre, light ring) on a mottled tissue."""
    r = _rng(seed)
    img = _img()
    for y in range(S):
        for x in range(S):
            _grey(img, x, y, 0.62 + 0.12 * r.random())
    for cy in (3, 8, 13):
        for cx in ((2, 7, 12) if cy != 8 else (4, 10, 15)):
            for dy in range(-2, 3):
                for dx in range(-2, 3):
                    d = math.hypot(dx, dy)
                    if d <= 2.2:
                        _grey(img, (cx + dx) % S, (cy + dy) % S, 0.55 if d < 0.8 else 0.98 if d < 1.7 else 0.85)
    return _finish(img)


def coral_disc(seed=2002):
    """Mushroom disc: radial stripes and small warts."""
    r = _rng(seed)
    img = _img()
    for y in range(S):
        for x in range(S):
            a = math.atan2(y - 7.5, x - 7.5)
            v = 0.72 + 0.1 * math.sin(a * 9) + 0.06 * r.random()
            if r.random() < 0.08:
                v = 0.95
            _grey(img, x, y, v)
    return _finish(img)


def coral_tentacle(seed=2003):
    """Tentacles: rounded stubs with light tips (hammer heads, the bubble tips of the anemone)."""
    r = _rng(seed)
    img = _img()
    for y in range(S):
        for x in range(S):
            _grey(img, x, y, 0.55 + 0.08 * r.random())
    for cy in range(2, S, 5):
        for cx in range((cy // 5) % 2 * 2 + 1, S, 4):
            for dy in range(-2, 3):
                for dx in range(-1, 2):
                    if abs(dx) + abs(dy) <= 2:
                        _grey(img, cx + dx, cy + dy, 0.95 if dy <= -1 else 0.82)
    return _finish(img)


def coral_brain(seed=2004):
    """Lobophyllia: fleshy, meandering ridges."""
    r = _rng(seed)
    img = _img()
    ph = r.random() * 6
    for y in range(S):
        for x in range(S):
            k = math.sin(x * 0.9 + math.sin(y * 0.6 + ph) * 2.0)
            v = 0.95 if k > 0.6 else 0.58 if k < -0.5 else 0.76
            _grey(img, x, y, v + 0.04 * r.random())
    return _finish(img)


def coral_branch(seed=2005):
    """Acropora skin: tiny corallites (dots) on a smooth branch."""
    r = _rng(seed)
    img = _img()
    for y in range(S):
        for x in range(S):
            v = 0.74 + 0.08 * r.random()
            if (x + 2 * y) % 4 == 0 and (y % 3 != 1):
                v = 0.95
            _grey(img, x, y, v)
    return _finish(img)


# ---------------------------------------------------------------------------------------------- frag icons (2 grey layers)

# M62: the item icons of the corals are per species now (plant_icons.py)
BLOCKS = {"plant_leaf_polyp": coral_polyp, "plant_leaf_disc": coral_disc, "plant_leaf_tentacle": coral_tentacle,
          "plant_leaf_brain": coral_brain, "plant_leaf_branch": coral_branch}


# ---------------------------------------------------------------------------------------------- rock (colour, uint8)

def _rgb(h):
    return np.array([int(h[i:i + 2], 16) for i in (1, 3, 5)], np.float64)


def _solid(fn):
    out = np.zeros((S, S, 4), np.uint8)
    for y in range(S):
        for x in range(S):
            out[y, x, :3] = np.clip(fn(x, y), 0, 255)
            out[y, x, 3] = 255
    return out


def reef_rock():
    """Porous aragonite: cream with darker pores."""
    r = _rng(2201)
    pores = r.random((S, S))
    base, pore = _rgb("#d8ccb0"), _rgb("#8a7a60")

    def px(x, y):
        v = base * (0.9 + 0.12 * r.random())
        if pores[y, x] > 0.86:
            return pore
        if pores[y, x] > 0.78:
            return base * 0.8
        return v
    return _solid(px)


def live_rock():
    """Live rock: the reef rock overgrown with purple and pink coralline algae and a few green tufts."""
    r = _rng(2202)
    pores = r.random((S, S))
    patches = r.random((S // 2 + 1, S // 2 + 1))
    base, pore = _rgb("#cabb9c"), _rgb("#6a5a48")
    purple, pink, green = _rgb("#8a3a8a"), _rgb("#c85a9a"), _rgb("#5a7a3a")

    def px(x, y):
        p = patches[y // 2, x // 2]
        if pores[y, x] > 0.88:
            return pore
        if p > 0.55:
            return (purple if (x + y) % 3 else pink) * (0.9 + 0.15 * r.random())
        if p < 0.08:
            return green * (0.9 + 0.2 * r.random())
        return base * (0.88 + 0.12 * r.random())
    return _solid(px)


def skimmer_side():
    """Acrylic reaction tube: grey body with a window of foam on top of water; the cup above the model's y = 12."""
    r = _rng(2301)
    body, dark, water, foam = _rgb("#3a4048"), _rgb("#22262c"), _rgb("#4a90c0"), _rgb("#e8e2c8")

    def px(x, y):
        if y <= 3:  # the collection cup (v 0..4)
            return _rgb("#c8d0d8") * (0.85 + 0.1 * r.random()) if 1 <= y <= 2 else dark
        if x in (0, 15):
            return dark
        if 5 <= x <= 10 and 5 <= y <= 14:
            if y <= 8:
                return foam * (0.85 + 0.15 * r.random())
            return water * (0.9 + 0.15 * r.random()) if r.random() > 0.15 else _rgb("#f0f4f8")
        return body * (0.92 + 0.1 * r.random())
    return _solid(px)


def skimmer_top():
    dark, cup, foam = _rgb("#22262c"), _rgb("#c8d0d8"), _rgb("#b8a878")

    def px(x, y):
        d = math.hypot(x - 7.5, y - 7.5)
        if d < 2.5:
            return foam
        if d < 4:
            return cup
        return dark
    return _solid(px)


def pump_side():
    """Flow pump body: black plastic with a lighter seam."""
    r = _rng(2401)

    def px(x, y):
        if y in (7, 8):
            return _rgb("#4a4e54")
        return _rgb("#1e2024") * (0.9 + 0.2 * r.random())
    return _solid(px)


def pump_outlet():
    """The propeller grille of the flow pump."""
    def px(x, y):
        d = math.hypot(x - 7.5, y - 7.5)
        if d > 7:
            return _rgb("#1e2024")
        if d > 6:
            return _rgb("#3a3e44")
        a = math.atan2(y - 7.5, x - 7.5)
        if int((a + math.pi) / (math.pi / 4)) % 2 == 0 or d < 1.5:
            return _rgb("#5a6068")
        return _rgb("#101214")
    return _solid(px)


# ---------------------------------------------------------------------------------------------- item icons (grid art)

K = "#24232a"
SEA_BUCKET = [
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
SEA_BUCKET_P = {"k": K, "g": "#a8b0b8", "G": "#d8dee4", "w": "#1e88c8", "W": "#3ab0e8", "b": "#e8f4fa"}
SALT = [
    "................",
    "................",
    "................",
    "................",
    "................",
    ".......kk.......",
    "......kWWk......",
    ".....kWwWwk.....",
    "....kwWWwWWk....",
    "...kWwWwWwWwk...",
    "..kwWWwWWwWWwk..",
    "..kWwWwwWwWwWk..",
    "...kkkkkkkkkk...",
    "................",
    "................",
    "................",
]
SALT_P = {"k": K, "w": "#d8dce0", "W": "#f8faff"}
CALCIUM = [
    "................",
    "......kkkk......",
    "......kcck......",
    ".......kk.......",
    "......kGGk......",
    ".....kGwwGk.....",
    "....kGwwwwGk....",
    "....kwWwwwwk....",
    "....kwwWwwwk....",
    "....kwwwWwwk....",
    "....kwwwwwWk....",
    "....kwWwwwwk....",
    "....kGwwwwGk....",
    ".....kkkkkk.....",
    "................",
    "................",
]
CALCIUM_P = {"k": K, "c": "#3a7ac8", "G": "#c8d8e4", "w": "#eceef0", "W": "#ffffff"}
PUMP = [
    "................",
    "................",
    "................",
    "....kkkkkkk.....",
    "...kddddddgk....",
    "..kgdddddddgk...",
    "..kdddkkkdddkk..",
    "..kddkGsGkddkGk.",
    "..kddksSskddkSk.",
    "..kddkGsGkddkGk.",
    "..kdddkkkdddkk..",
    "..kgdddddddgk...",
    "...kddddddgk....",
    "....kkkkkkk.....",
    "................",
    "................",
]
PUMP_P = {"k": K, "d": "#2a2c32", "g": "#4a4e56", "G": "#6a7078", "s": "#8a9098", "S": "#b0b8c0"}


def grid_icon(rows, cmap):
    img = np.zeros((S, S, 4), np.uint8)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            h = cmap[ch]
            img[y, x, :3] = [int(h[i:i + 2], 16) for i in (1, 3, 5)]
            img[y, x, 3] = 255
    return img


def write_all():
    out = []
    for name, fn in BLOCKS.items():
        out.append(save_png(fn(), BLOCK_TEX / f"{name}.png"))
    for name, fn in {"reef_rock": reef_rock, "live_rock": live_rock, "protein_skimmer_side": skimmer_side,
                     "protein_skimmer_top": skimmer_top, "flow_pump_segment_side": pump_side,
                     "flow_pump_segment_outlet": pump_outlet}.items():
        out.append(save_png(fn(), BLOCK_TEX / f"{name}.png"))
    for name, rows, cmap in (("sea_water_bucket", SEA_BUCKET, SEA_BUCKET_P), ("sea_salt", SALT, SALT_P),
                             ("calcium_additive", CALCIUM, CALCIUM_P), ("flow_pump", PUMP, PUMP_P)):
        out.append(save_png(grid_icon(rows, cmap), ITEM_TEX / f"{name}.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print(p)
