"""M37 assets: the fertilizer maker (a dark green steel case with a gauge window, a glass mixing tank with the green
fertilizer on top; the crank textures are the food maker's), the crank, the intermediates kelp meal and nutrient salt,
the liquid fertilizer bottle and the fertilizer canister of the dosing pump.

Deterministic pixel grids and procedural faces, no vanilla pixels.

Run from the repo root:  py tools/assetgen/fertilizer.py
"""
from __future__ import annotations

from common import BLOCK_TEX, ITEM_TEX, save_png
from icons import grid
from plumbing import _img, _rgb, _rng, steel
from reef_dosing import K, canister_rows

FERT = "#7a9a2a"      # the liquid fertilizer (olive green)
FERT_DARK = "#4e6a14"


# ---------------------------------------------------------------------------------------------- blocks

def maker_front():
    """Dark green enamelled case: a round gauge (needle), the level window of the tank and the outlet spout."""
    r = _rng(3701)

    def px(x, y):
        base = _rgb("#2f5a3a") * (0.93 + 0.07 * r.random())
        if x in (0, 15) or y in (0, 15):
            return base * 0.7
        # the level window (fertilizer up to a line)
        if 9 <= x <= 12 and 2 <= y <= 10:
            if x in (9, 12) or y in (2, 10):
                return _rgb("#22262a")
            return _rgb(FERT) * (0.9 + 0.1 * r.random()) if y >= 5 else _rgb("#9ab8c4") * 0.8
        # the gauge: a white dial with a red needle
        dx, dy = x - 5, y - 5
        d2 = dx * dx + dy * dy
        if d2 <= 9:
            if d2 >= 7:
                return _rgb("#c8ced4")
            if (dx, dy) in ((0, 0), (1, -1), (2, -2)):
                return _rgb("#c02a2a")
            return _rgb("#eef0ea")
        if 3 <= x <= 7 and 11 <= y <= 14:  # the spout
            return _rgb("#9aa2aa") * (1.1 if x == 3 else 0.9 if x == 7 else 1.0)
        return base
    return _img(px)


def maker_side():
    r = _rng(3702)

    def px(x, y):
        base = _rgb("#2c5636") * (0.93 + 0.07 * r.random())
        if x in (0, 15) or y in (0, 15):
            return base * 0.7
        if y in (4, 11) and 2 <= x <= 13:  # pressed ribs
            return base * 1.22
        if (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
            return _rgb("#c8ced4")
        return base
    return _img(px)


def maker_top():
    img = steel(3703, "#a4acb4")
    img[3:13, 3:13, :3] = (54, 60, 64)
    return img


def maker_tank():
    """The glass mixing tank: green fertilizer in the lower two thirds, a stirrer blade, light glass above."""
    r = _rng(3704)

    def px(x, y):
        if x in (0, 15):
            return _rgb("#5a6a70")
        if y >= 5:
            v = _rgb(FERT) * (0.85 + 0.15 * r.random())
            if y == 5:
                v = _rgb("#a8c860")
            if x in (7, 8) and y >= 6:  # the stirrer shaft
                v = _rgb("#3a3e42")
            if y in (10, 11) and 4 <= x <= 11:  # its blade
                v = _rgb("#55595e")
            return v
        return _rgb("#b8d0d8") * (0.9 + 0.1 * r.random()) * (1.15 if x in (2, 3) else 1.0)
    return _img(px)


def maker_lid():
    img = steel(3705, "#bcc4cc")
    img[6:10, 6:10, :3] = (70, 76, 82)  # the filler cap
    return img


# ---------------------------------------------------------------------------------------------- items

CRANK_ROWS = [
    "................",
    "................",
    "..........rr....",
    ".........rRRr...",
    ".........rRRr...",
    "..........rr....",
    "..........ii....",
    "..........iI....",
    ".........iIi....",
    "........iIi.....",
    ".......iIi......",
    "......iIi.......",
    ".....sS.........",
    "....sS..........",
    "...sS...........",
    "..ss............",
]


def crank():
    """An iron crank arm with a red knob at the top and a wooden shaft below."""
    return grid(CRANK_ROWS, {"r": "#7e1e20", "R": "#c8343a", "i": "#5a5e66", "I": "#c8ced6", "s": "#5a3c1e", "S": "#9a6a38"})


SACK_ROWS = [
    "................",
    "................",
    ".......kk.......",
    "......kTTk......",
    ".......kk.......",
    ".....kSSSSk.....",
    "....kSSsSSSk....",
    "...kSSSSSsSSk...",
    "...kSsSSSSSSk...",
    "...kSSSLLSSSk...",
    "...kSSLLLLSSk...",
    "...kSSSLLSSsk...",
    "...kSsSSSSSSk...",
    "....kSSSSSSk....",
    ".....kkkkkk.....",
    "................",
]


def sack(body, dark, label):
    """A small paper sack tied at the top with a label."""
    return grid(SACK_ROWS, {"k": "#2e2a22", "T": "#c8b080", "S": body, "s": dark, "L": label})


BOTTLE_ROWS = [
    "................",
    "......kkkk......",
    "......kCCk......",
    ".......kk.......",
    "......kGGk......",
    "......kGGk......",
    ".....kGGGGk.....",
    "....kGgggggk....",
    "...kGgfffffgk...",
    "...kGfLLLLffk...",
    "...kGfLllLffk...",
    "...kGfLLLLffk...",
    "...kGffffffFk...",
    "....kGffffFk....",
    ".....kkkkkk.....",
    "................",
]


def bottle():
    """A glass bottle of liquid fertilizer with a cork and a white label with a green leaf."""
    return grid(BOTTLE_ROWS, {"k": "#24302a", "C": "#a07848", "G": "#d8ecf0", "g": "#b8d890", "f": FERT, "F": FERT_DARK,
                              "L": "#f2f2ea", "l": "#4a9a3a"})


def fertilizer_canister():
    """The dosing pump's canister with the green fertilizer and a leaf on the label."""
    pal = {"k": K, "c": "#3a3e44", "K": "#5a5e64", "w": "#dfe8ee", "W": "#ffffff", "L": "#5aa83a", "f": FERT + "c0"}
    return grid(canister_rows(True), pal)


def write_all():
    out = []
    blocks = {"fertilizer_maker_front": maker_front(), "fertilizer_maker_side": maker_side(), "fertilizer_maker_top": maker_top(),
              "fertilizer_maker_tank": maker_tank(), "fertilizer_maker_lid": maker_lid()}
    for name, img in blocks.items():
        out.append(save_png(img, BLOCK_TEX / f"{name}.png"))
    items = {
        "crank": crank(),
        "kelp_meal": sack("#5a7a3a", "#3e5a26", "#c8d8a0"),
        "nutrient_salt": sack("#e0dccc", "#b0a890", "#5a8ad0"),
        "liquid_fertilizer": bottle(),
        "fertilizer_canister": fertilizer_canister(),
    }
    for name, img in items.items():
        out.append(save_png(img, ITEM_TEX / f"{name}.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print(p)
