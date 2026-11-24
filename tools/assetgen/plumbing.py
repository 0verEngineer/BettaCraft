"""M24 assets: stainless steel, crucible, pipes, water pump, interior plumbing, multiblock filter, water devices.

Block textures (brushed stainless steel, clear glass pipe walls, the crucible's nether brick body and glowing melt, the
pump, the reservoir with its water window, the level sensor, the water change controller, the filter chambers with a
window per medium, the pump head and connectors, the interior bulkhead, pipes and outlets), item icons (stainless steel
ingot, the four filter media, the interior pieces) and the crucible GUI. Deterministic and procedural, no vanilla pixels.

Run from the repo root:  py tools/assetgen/plumbing.py
"""
from __future__ import annotations

import math

import numpy as np

import gui
from common import BLOCK_TEX, GUI_TEX, ITEM_TEX, save_png
from icons import grid

S = 16


def _rng(seed):
    return np.random.default_rng(seed)


def _rgb(h):
    return np.array([int(h[i:i + 2], 16) for i in (1, 3, 5)], np.float64)


def _img(fn, alpha=None):
    out = np.zeros((S, S, 4), np.uint8)
    for y in range(S):
        for x in range(S):
            out[y, x, :3] = np.clip(fn(x, y), 0, 255)
            out[y, x, 3] = 255 if alpha is None else alpha(x, y)
    return out


# ---------------------------------------------------------------------------------------------- materials

def steel(seed, base="#b9c0c8", frame=True):
    """Brushed stainless steel: fine horizontal streaks, a soft darker frame."""
    r = _rng(seed)
    streak = r.random(S) * 0.08

    def px(x, y):
        v = 0.94 + streak[y] + 0.03 * r.random()
        if frame and (x in (0, 15) or y in (0, 15)):
            v *= 0.78
        return _rgb(base) * v
    return _img(px)


# M65: the pipes are 3 px thick (columns 6.5..9.5 of the texture across the pipe: half of 6, 7, 8, half of 9) and drawn with the texture's columns across
# the pipe and its rows along it on every face (the models pick the texture or its transposed copy "_r" per face). The
# shading depends on the column only, so a pipe has no lines across it at the block borders or in the middle of a block.
PIPE_SHADE = {6: 0.8, 7: 1.0, 8: 0.94, 9: 0.8}


def pipe_shade(x):
    return PIPE_SHADE.get(x, 0.95)


def transposed(img):
    """The copy of a pipe texture for the faces whose rows run across the pipe (columns and rows swapped)."""
    return np.ascontiguousarray(np.transpose(img, (1, 0, 2)))


def glass_pipe_tex():
    """Glass pipe wall (3 px): denser edges along both sides and a highlight, nothing across."""
    def px(x, y):
        if x in (6, 9):
            return _rgb("#d8f0f4")
        if x == 7:
            return _rgb("#ffffff")
        return _rgb("#bfe6ee")

    def a(x, y):
        if x in (6, 9):
            return 200
        if x == 7:
            return 150
        return 80
    return _img(px, a)


def steel_pipe_tex(base="#aeb6bf", seed=2401):
    """Brushed stainless steel pipe: lighter in the middle, darker along both edges, brushed along the pipe."""
    r = _rng(seed)
    brush = 1 + (r.random(S) - 0.5) * 0.03

    def px(x, y):
        return _rgb(base) * pipe_shade(x) * brush[x]
    return _img(px)


def nether_brick(seed):
    """Crucible body: dark red bricks with mortar."""
    r = _rng(seed)

    def px(x, y):
        row = y // 4
        off = 0 if row % 2 == 0 else 4
        if y % 4 == 3 or (x + off) % 8 == 7:
            return _rgb("#1e0e10") * (0.9 + 0.2 * r.random())
        return _rgb("#5a2026") * (0.85 + 0.25 * r.random())
    return _img(px)


def crucible_top(lit):
    """Top: the rim and the melt (dull grey cold, glowing orange hot)."""
    r = _rng(2403 if lit else 2404)

    def px(x, y):
        d = max(abs(x - 7.5), abs(y - 7.5))
        if d > 6:
            return _rgb("#3a1a1e") * (0.9 + 0.2 * r.random())
        if d > 5:
            return _rgb("#20100f")
        if lit:
            t = 0.5 + 0.5 * math.sin(x * 1.3 + y * 0.7) * math.cos(y * 1.1)
            return _rgb("#ff9a20") * (0.8 + 0.25 * t) + _rgb("#401000") * (1 - t) * 0.3
        return _rgb("#4a4c52") * (0.85 + 0.2 * r.random())
    return _img(px)


def water_window(seed, base_fn, fill_rows):
    """A block face with a glass window showing water up to the given rows."""
    img = base_fn(seed)
    r = _rng(seed + 7)
    for y in range(3, 13):
        for x in range(4, 12):
            if y >= 13 - fill_rows:
                c = _rgb("#2f6fc8") * (0.9 + 0.15 * r.random())
            else:
                c = _rgb("#c8dde6") * (0.95 + 0.05 * r.random())
            img[y, x, :3] = np.clip(c, 0, 255).astype(np.uint8)
    for x in range(3, 13):
        img[2, x, :3] = img[13, x, :3] = (60, 64, 70)
    for y in range(2, 14):
        img[y, 3, :3] = img[y, 12, :3] = (60, 64, 70)
    return img


MEDIUM_COL = {"none": None, "sponge": "#2a2c30", "ceramic": "#d8cfc0", "carbon": "#151516", "peat": "#5a3a22"}


def chamber_side(medium):
    """Filter chamber: steel with a window; behind it the medium (sponge foam, ceramic rings, carbon, peat)."""
    img = steel(2410, "#c3c9d0")
    r = _rng(2411 + len(medium))
    for y in range(3, 13):
        for x in range(3, 13):
            col = MEDIUM_COL[medium]
            if col is None:
                c = _rgb("#a9cdd8") * (0.95 + 0.05 * r.random())
            elif medium == "ceramic":
                ring = ((x - 3) % 4 in (0, 3)) or ((y - 3) % 4 in (0, 3))
                c = _rgb(col) * (0.9 + 0.15 * r.random()) if ring else _rgb("#8a8478")
            else:
                c = _rgb(col) * (0.75 + 0.5 * r.random())
            img[y, x, :3] = np.clip(c, 0, 255).astype(np.uint8)
    for i in range(2, 14):
        for (x, y) in ((i, 2), (i, 13), (2, i), (13, i)):
            img[y, x, :3] = (70, 76, 84)
    return img


def chamber_top():
    img = steel(2412, "#b4bbc3")
    for x in range(4, 12):  # a handle bar
        img[7, x, :3] = img[8, x, :3] = (80, 86, 94)
    return img


def head_side(active):
    img = steel(2420, "#9aa3ad")
    # a motor stripe and a small lamp
    for x in range(1, 15):
        img[5, x, :3] = (60, 66, 74)
    lamp = (80, 220, 120) if active else (40, 70, 50)
    img[10:12, 11:13, :3] = lamp
    return img


def head_top():
    """Top of the pump head: the round motor cap."""
    r = _rng(2422)

    def px(x, y):
        d = math.hypot(x - 7.5, y - 7.5)
        if d < 4:
            return _rgb("#3c4248") * (0.9 + 0.2 * r.random())
        if d < 5:
            return _rgb("#20242a")
        return _rgb("#a8b0b8") * (0.93 + 0.1 * r.random())
    return _img(px)


def connector(outlet):
    """Inlet / outlet face: the pipe socket; blue ring for the inlet, orange for the outlet."""
    ring = _rgb("#e08a30") if outlet else _rgb("#3a8ad8")

    def px(x, y):
        d = math.hypot(x - 7.5, y - 7.5)
        if d < 2.6:
            return _rgb("#14181c")
        if d < 3.8:
            return _rgb("#c8d0d8")
        if d < 4.8:
            return ring
        return _rgb("#aab2bb") * (0.95 + 0.05 * math.sin(y))
    return _img(px)


def pump_face(kind):
    """Water pump: 'front' with the outlet, 'back' with the intake grille, 'side' with the motor fins."""
    r = _rng(2430)

    def px(x, y):
        base = _rgb("#7b8794") * (0.93 + 0.1 * r.random())
        if x in (0, 15) or y in (0, 15):
            return base * 0.75
        if kind == "front":
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 3:
                return _rgb("#121518")
            if d < 4.2:
                return _rgb("#e08a30")
        elif kind == "back":
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 4.2:
                return _rgb("#20262c") if (x + y) % 2 == 0 else _rgb("#3a8ad8")
        elif kind == "side":
            if 3 <= y <= 12 and x % 3 == 0 and 2 <= x <= 13:
                return _rgb("#4c5560")
        return base
    return _img(px)


def sensor_face():
    """Level sensor: a float switch housing with a green window."""
    def px(x, y):
        if 5 <= x <= 10 and 4 <= y <= 9:
            return _rgb("#3cc070") if y > 6 else _rgb("#c8f0d8")
        if x in (0, 15) or y in (0, 15):
            return _rgb("#5a636d")
        return _rgb("#8e98a4")
    return _img(px)


def changer_face(front):
    """Water change controller: a panel with two dials (front) or vents (sides)."""
    r = _rng(2440)

    def px(x, y):
        base = _rgb("#d8dde2") * (0.95 + 0.05 * r.random())
        if x in (0, 15) or y in (0, 15):
            return base * 0.7
        if front:
            if 2 <= x <= 13 and 2 <= y <= 6:
                return _rgb("#1c2a36") if not (3 <= x <= 12 and 3 <= y <= 5) else _rgb("#3ad0e0")
            for cx in (5, 10):
                d = math.hypot(x - cx, y - 10.5)
                if d < 2.2:
                    return _rgb("#30363c")
                if d < 2.8:
                    return _rgb("#90989f")
        elif 4 <= y <= 11 and y % 2 == 0 and 3 <= x <= 12:
            return _rgb("#7c858e")
        return base
    return _img(px)


def plumbing_glass():
    """Interior pipe: clear glass (seen up close inside the tank)."""
    def px(x, y):
        return _rgb("#e4f6f8") if (x in (0, 15) or y in (0, 15) or x == 4) else _rgb("#c8eef2")

    def a(x, y):
        return 220 if (x in (0, 15) or y in (0, 15)) else 150 if x == 4 else 90
    return _img(px, a)


def strainer_tex():
    """Intake strainer: a slotted steel basket."""
    def px(x, y):
        if y % 3 == 1 and 2 <= x <= 13:
            return _rgb("#20262c")
        return _rgb("#b8c0c8")
    return _img(px)


def nozzle_tex():
    def px(x, y):
        d = math.hypot(x - 7.5, y - 7.5)
        return _rgb("#1a1e22") if d < 2.5 else _rgb("#9aa4ae") if d < 6 else _rgb("#c4ccd4")
    return _img(px)


# ---------------------------------------------------------------------------------------------- items

INGOT = [
    "................",
    "................",
    "................",
    "................",
    ".......hhhhh....",
    ".....hhwwwwwh...",
    "...hhwwwwwwwsh..",
    "..hwwwwwwwwssd..",
    ".hlwwwwwwwssdd..",
    ".hllllwwwssddd..",
    "..dlllllsdddd...",
    "...ddllldddd....",
    ".....dddd.......",
    "................",
    "................",
    "................",
]
INGOT_P = {"h": "#3c4248", "w": "#e8eef4", "l": "#c4ccd4", "s": "#a6b0ba", "d": "#78828c"}

FOAM = [
    "................",
    "................",
    "...ooooooooo....",
    "..oxbxbxbxbxo...",
    "..obxbxbxbxbo...",
    "..oxbxbxbxbxo...",
    "..obxbxbxbxbo...",
    "..oxbxbxbxbxo...",
    "..obxbxbxbxbo...",
    "..oxbxbxbxbxo...",
    "..obxbxbxbxbo...",
    "...ooooooooo....",
    "................",
    "................",
    "................",
    "................",
]
FOAM_P = {"o": "#101214", "x": "#2c3034", "b": "#3c4248"}

RINGS = [
    "................",
    "................",
    "...ccc....ccc...",
    "..c.d.c..c.d.c..",
    "..cdddc..cdddc..",
    "...ccc....ccc...",
    "......ccc.......",
    ".....c.d.c......",
    ".....cdddc......",
    "......ccc.......",
    "..ccc......ccc..",
    ".c.d.c....c.d.c.",
    ".cdddc....cdddc.",
    "..ccc......ccc..",
    "................",
    "................",
]
RINGS_P = {"c": "#e2d8c8", "d": "#8a7e6e"}

CARBON = [
    "................",
    "................",
    "....kk....kk....",
    "...kgkk..kgkk...",
    "...kkk....kk....",
    ".......kk.......",
    "..kk..kgkk..kk..",
    ".kgkk.kkk..kgkk.",
    ".kkk.......kkk..",
    "......kk........",
    "....kkgkk..kk...",
    "....kkkk..kgkk..",
    "..kk......kkk...",
    ".kgkk...........",
    "..kk............",
    "................",
]
CARBON_P = {"k": "#141416", "g": "#5a5e66"}

PEAT = [
    "................",
    "................",
    "................",
    "....pppppppp....",
    "...pqpqpppqpp...",
    "..ppppqpqppppp..",
    "..pqppppppqpqp..",
    "..ppqpqppppppp..",
    "..pppppqpqpppp..",
    "..pqpppppppqpp..",
    "...ppqpqppppp...",
    "....pppppppp....",
    "................",
    "................",
    "................",
    "................",
]
PEAT_P = {"p": "#4a2e1a", "q": "#7a5032"}

BULK = [
    "................",
    "................",
    "................",
    "......pppp......",
    "......pggp......",
    "......pggp......",
    "......pggp......",
    "..ssssssssssss..",
    "..svvvvvvvvvvs..",
    "..ssssssssssss..",
    "....nnnnnnnn....",
    "....nnnnnnnn....",
    "................",
    "................",
    "................",
    "................",
]
BULK_P = {"p": "#8aa4b0", "g": "#cfeef4", "s": "#7e8892", "v": "#c8d0d8", "n": "#4a525a"}

IPIPE = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "eeeeeeeeeeeeeeee",
    "gwgggggggggggggg",
    "gggggggggggggggg",
    "eeeeeeeeeeeeeeee",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
]
IPIPE_P = {"e": "#9ccad4", "g": "#d4f2f6", "w": "#ffffff"}

STRAIN = [
    "................",
    "......eeee......",
    "......egge......",
    "......egge......",
    "....ssssssss....",
    "....s######s....",
    "....ssssssss....",
    "....s######s....",
    "....ssssssss....",
    "....s######s....",
    "....ssssssss....",
    "....s######s....",
    "....ssssssss....",
    "................",
    "................",
    "................",
]
STRAIN_P = {"e": "#9ccad4", "g": "#d4f2f6", "s": "#b8c0c8", "#": "#20262c"}

LILY = [
    "................",
    ".......eeeeee...",
    "......egggggge..",
    "......eg....ge..",
    "......eg.....e..",
    "......eg........",
    "......eg........",
    "......eg........",
    "......eg........",
    "......eg........",
    "......eg........",
    "......eg........",
    "......eg........",
    "......eg........",
    "................",
    "................",
]
LILY_P = {"e": "#8cc4d0", "g": "#e0f8fb"}

NOZ = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "........ss......",
    "eeeeeessssss....",
    "ggggggsvvvvvs.bb",
    "ggggggsvvvvvs.b.",
    "eeeeeessssss..b.",
    "........ss......",
    "................",
    "................",
    "................",
    "................",
    "................",
]
NOZ_P = {"e": "#9ccad4", "g": "#d4f2f6", "s": "#7e8892", "v": "#c8d0d8", "b": "#7fd0ff"}

SPRAY = [
    "................",
    "................",
    "................",
    "................",
    "..b..b..b..b..b.",
    "................",
    "..b..b..b..b..b.",
    "ssssssssssssssss",
    "vvvvvvvvvvvvvvvv",
    "s#ss#ss#ss#ss#ss",
    "ssssssssssssssss",
    "................",
    "................",
    "................",
    "................",
    "................",
]
SPRAY_P = {"s": "#8a949e", "v": "#c8d0d8", "#": "#20262c", "b": "#7fd0ff"}


def crucible_gui():
    img = gui._small_panel()
    from gui import slot, inset
    for (x, y) in ((38, 17), (56, 17), (38, 35), (56, 35), (47, 53)):
        slot(img, x, y)
    inset(img, 116, 26, 26, 26)
    return img


def write_all():
    out = []
    blocks = {
        "glass_pipe": glass_pipe_tex(), "stainless_steel_pipe": steel_pipe_tex(),
        "glass_pipe_r": transposed(glass_pipe_tex()), "stainless_steel_pipe_r": transposed(steel_pipe_tex()),
        "stainless_steel_block": steel(2400),
        "crucible_side": nether_brick(2402), "crucible_top": crucible_top(False), "crucible_top_lit": crucible_top(True),
        "water_pump_front": pump_face("front"), "water_pump_back": pump_face("back"), "water_pump_side": pump_face("side"),
        "reservoir_side": water_window(2450, lambda s: steel(s, "#c0c7ce"), 0), "reservoir_side_full": water_window(2451, lambda s: steel(s, "#c0c7ce"), 10),
        "reservoir_top": steel(2452, "#b0b8c0"),
        "level_sensor": sensor_face(), "water_change_controller_front": changer_face(True), "water_change_controller_side": changer_face(False),
        "filter_chamber_top": chamber_top(), "filter_pump_head_side": head_side(False), "filter_pump_head_side_on": head_side(True),
        "filter_pump_head_top": head_top(), "filter_inlet": connector(False), "filter_outlet": connector(True),
        "plumbing_glass": plumbing_glass(), "plumbing_steel": steel(2460, "#9aa4ae", False), "intake_strainer": strainer_tex(), "nozzle": nozzle_tex(),
    }
    for m in MEDIUM_COL:
        blocks[f"filter_chamber_{m}"] = chamber_side(m)
    for name, img in blocks.items():
        out.append(save_png(img, BLOCK_TEX / f"{name}.png"))
    icons = {
        "stainless_steel_ingot": grid(INGOT, INGOT_P), "filter_foam": grid(FOAM, FOAM_P), "ceramic_rings": grid(RINGS, RINGS_P),
        "activated_carbon": grid(CARBON, CARBON_P), "filter_peat": grid(PEAT, PEAT_P), "bulkhead": grid(BULK, BULK_P),
        "interior_pipe": grid(IPIPE, IPIPE_P), "intake_strainer": grid(STRAIN, STRAIN_P), "lily_pipe": grid(LILY, LILY_P),
        "nozzle": grid(NOZ, NOZ_P), "spray_bar": grid(SPRAY, SPRAY_P),
    }
    for name, img in icons.items():
        out.append(save_png(img, ITEM_TEX / f"{name}.png"))
    out.append(save_png(crucible_gui(), GUI_TEX / "crucible.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print(p)
