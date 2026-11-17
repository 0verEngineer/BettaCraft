"""M23 assets: aeration, internal filters, light tiers.

Block textures of the air pump, the air stone, the sponge filter, the corner filter, the intake sponge, the LED panel and
the high-power LED (housing, heat sink, emissive panels on/off); item icons (air pump, air stone, air hose, sponge filter,
corner filter, intake sponge, LED panel and high-power LED with a white glow layer tinted in the light colour); the
entity texture of the clear air hose. Deterministic, procedural, no vanilla pixels.

Run from the repo root:  py tools/assetgen/aeration.py
"""
from __future__ import annotations

import math

import numpy as np

from blocks import _panel
from common import BLOCK_TEX, ITEM_TEX, ROOT, save_png
from icons import grid

S = 16


def _rng(seed):
    return np.random.default_rng(seed)


def _rgb(h):
    return np.array([int(h[i:i + 2], 16) for i in (1, 3, 5)], np.float64)


def _solid(fn, alpha=None):
    out = np.zeros((S, S, 4), np.uint8)
    for y in range(S):
        for x in range(S):
            out[y, x, :3] = np.clip(fn(x, y), 0, 255)
            out[y, x, 3] = 255 if alpha is None else alpha(x, y)
    return out


# ---------------------------------------------------------------------------------------------- air pump

def pump_side():
    """Light grey plastic housing with a darker seam and a small vent."""
    r = _rng(2301)

    def px(x, y):
        if y in (7, 8):
            return _rgb("#8a9098") * (0.95 + 0.1 * r.random())
        if 10 <= y <= 12 and 5 <= x <= 10 and x % 2 == 0:
            return _rgb("#5a6068")
        return _rgb("#c4cad2") * (0.93 + 0.1 * r.random())
    return _solid(px)


def pump_top():
    """Top: housing with a round grille over the membrane and the brand-free dial."""
    r = _rng(2302)

    def px(x, y):
        d = math.hypot(x - 7.5, y - 7.5)
        if d < 3.2:
            return _rgb("#3a3e44") if (x + y) % 2 == 0 else _rgb("#6a7078")
        if d < 3.9:
            return _rgb("#9aa0a8")
        return _rgb("#c8ced6") * (0.93 + 0.1 * r.random())
    return _solid(px)


def pump_front():
    """Front with the hose nipple and the dark feet colour."""
    def px(x, y):
        if 6 <= x <= 9 and 9 <= y <= 11:
            return _rgb("#2a2c30")
        return _rgb("#3c4046")
    return _solid(px)


# ---------------------------------------------------------------------------------------------- air stone

def stone_side():
    """Porous blue-grey air stone (sintered corundum): fine dark pores."""
    r = _rng(2311)

    def px(x, y):
        v = 0.88 + 0.14 * r.random()
        if r.random() < 0.16:
            v = 0.62
        return _rgb("#8e9ca8") * v
    return _solid(px)


def stone_top():
    r = _rng(2312)

    def px(x, y):
        d = math.hypot(x - 7.5, y - 7.5)
        v = 0.9 + 0.12 * r.random()
        if r.random() < 0.18:
            v = 0.66
        if d > 6.5:
            v *= 0.85
        return _rgb("#9aa8b4") * v
    return _solid(px)


# ---------------------------------------------------------------------------------------------- sponge filter, intake sponge

def sponge(seed, base="#2a2c2e"):
    """Coarse black filter foam: dark cells with lighter walls."""
    r = _rng(seed)

    def px(x, y):
        v = 0.8 + 0.5 * r.random()
        if (x * 7 + y * 3) % 5 == 0:
            v = 1.6
        return _rgb(base) * v
    return _solid(px)


def sponge_top_side():
    """The cap of the sponge filter: a clear lift tube in a grey cap."""
    def px(x, y):
        if 6 <= x <= 9:
            return _rgb("#b8d4e0") if x in (6, 9) else _rgb("#d8eef6")
        return _rgb("#5a6068") if y < 4 else _rgb("#2e3034")
    return _solid(px)


def sponge_top():
    def px(x, y):
        d = math.hypot(x - 7.5, y - 7.5)
        if d < 2.2:
            return _rgb("#1a1c1e")
        if d < 3.2:
            return _rgb("#c8e0ea")
        return _rgb("#5a6068")
    return _solid(px)


# ---------------------------------------------------------------------------------------------- corner filter

def corner_side():
    """Corner filter: translucent-looking grey body with vertical ribs."""
    r = _rng(2321)

    def px(x, y):
        v = 0.95 + 0.08 * r.random()
        if x % 4 == 0:
            v = 0.78
        return _rgb("#9ca6ae") * v
    return _solid(px)


def corner_intake():
    """The intake: horizontal slots."""
    def px(x, y):
        if y % 3 == 1 and 2 <= x <= 13:
            return _rgb("#24282c")
        return _rgb("#8a949c")
    return _solid(px)


def corner_top():
    def px(x, y):
        if 1 <= x <= 14 and 1 <= y <= 14 and (x + y) % 3 == 0:
            return _rgb("#6a737a")
        return _rgb("#a8b2ba")
    return _solid(px)


def corner_outlet():
    """The outlet nozzle: a dark round opening in the ribbed body."""
    def px(x, y):
        d = math.hypot(x - 7.5, y - 7.5)
        if d < 2.8:
            return _rgb("#141618")
        if d < 4.0:
            return _rgb("#5a6268")
        return _rgb("#9ca6ae") * (0.78 if x % 4 == 0 else 1.0)
    return _solid(px)


# ---------------------------------------------------------------------------------------------- lights

def led_housing(seed, base):
    """Brushed aluminium (panel) or anthracite (high-power) housing, brushed along X, lighter top edge."""
    r = _rng(seed)
    img = np.zeros((S, S, 4), np.uint8)
    row = r.integers(-3, 4, S)
    b = _rgb(base)
    for y in range(S):
        v = np.clip(b[None, :] + row[y] + r.integers(-2, 3, (S, 1)), 0, 255)
        img[y, :, :3] = v
    img[..., 3] = 255
    img[0, :, :3] = np.clip(b + 0x28, 0, 255)
    img[S - 1, :, :3] = np.clip(b - 0x14, 0, 255)
    return img


def heat_sink():
    """High-power LED top: fins of the heat sink (light and dark stripes) and a mounting plate in the middle."""
    def px(x, y):
        if 6 <= y <= 9 and 6 <= x <= 9:
            return _rgb("#54585e")
        return _rgb("#6e737a") if x % 2 == 0 else _rgb("#2c2f33")
    return _solid(px)


def hp_panel(on):
    """High-power LED panel: a grid of single bright LED dots in a dark board (on), dim dots (off)."""
    def px(x, y):
        led = x % 3 == 1 and y % 3 == 1
        if on:
            return _rgb("#ffffff") if led else _rgb("#e0e6f0")
        return _rgb("#6a6e76") if led else _rgb("#3c3f44")
    return _solid(px)


# ---------------------------------------------------------------------------------------------- icons

K = "#16161a"
PUMP = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "........kk......",
    ".......kSSk.....",
    "...kkkkkSSkkkk..",
    "..kGGGGGGGGGGGk.",
    "..kSSSSdddSSSSk.",
    "..kSSSdgggdSSSk.",
    "..kSSSSdddSSSSk.",
    "..kssssssssssssk",
    "..kkkkkkkkkkkkk.",
    "...dd.......dd..",
    "................",
]
PUMP_P = {"k": K, "S": "#c4cad2", "s": "#8a9098", "G": "#e0e4ea", "d": "#3a3e44", "g": "#6a7078"}

STONE = [
    "................",
    "........h.......",
    ".......h........",
    "........h.......",
    ".......h........",
    "........h.......",
    ".......h........",
    "......kkkk......",
    "....kkBBBBkk....",
    "...kBBbBBbBBk...",
    "..kBbBBBBBBbBk..",
    "..kBBBbBBbBBBk..",
    "..kbBBBBBBBBbk..",
    "...kkbBBBBbkk...",
    ".....kkkkkk.....",
    "................",
]
STONE_P = {"k": "#3a4048", "B": "#9aa8b4", "b": "#66727c", "h": "#b8dce8"}

HOSE = [
    "................",
    "..........hhh...",
    ".........hHHHh..",
    "........hH..hH..",
    ".......hH....Hh.",
    "......hH.....Hh.",
    ".....hH......hH.",
    "....hH......hH..",
    "...hH......hH...",
    "..hH......hH....",
    ".hH......hH.....",
    ".hH.....hH......",
    "..hHHHHHH.......",
    "...hhhhh........",
    "................",
    "................",
]
HOSE_P = {"h": "#7aa8b8", "H": "#c8eaf4"}

SPONGE_F = [
    "......hh........",
    "......Hh........",
    "......Hh........",
    ".....kkkk.......",
    "....kGGGGk......",
    "....kcHHck......",
    "...kssssssk.....",
    "...ksSssSsk.....",
    "...kssssssk.....",
    "...ksSssSsk.....",
    "...kssssssk.....",
    "...ksSssSsk.....",
    "...kssssssk.....",
    "...kGGGGGGk.....",
    "....kkkkkk......",
    "................",
]
SPONGE_F_P = {"k": K, "s": "#2a2c2e", "S": "#4a4e52", "G": "#5a6068", "c": "#3a3e44", "H": "#d8eef6", "h": "#9ac4d4"}

CORNER = [
    "................",
    "....kkkkkkk.....",
    "....kGGGGGGk....",
    "....kbbbbbbbk...",
    "....kbdbbbbbk...",
    "....kbbbbbbbk...",
    "....kbbbbbbbk...",
    "....kbbbbbbbk...",
    "....kbbbbbbbk...",
    "....kbbbbbbbk...",
    "....kbsbsbsbk...",
    "....kbbbbbbbk...",
    "....kbsbsbsbk...",
    "....kbbbbbbbk...",
    ".....kkkkkkk....",
    "................",
]
CORNER_P = {"k": K, "b": "#9ca6ae", "G": "#c8d0d8", "s": "#24282c", "d": "#141618"}

INTAKE = [
    "................",
    "................",
    "................",
    ".....kkkkkk.....",
    "....ksSssSsk....",
    "...ksssSsssSk...",
    "...kSsssSsssk...",
    "...ksssSsssSk...",
    "...kSsssSsssk...",
    "...ksssSsssSk...",
    "...kSsssSsssk...",
    "....ksSssSsk....",
    ".....kkkkkk.....",
    "................",
    "................",
    "................",
]
INTAKE_P = {"k": "#0e0f10", "s": "#2a2c2e", "S": "#4e5256"}

PANEL = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".kkkkkkkkkkkkkk.",
    "kAAAAAAAAAAAAAAk",
    "kMMMMMMMMMMMMMMk",
    ".kkkkkkkkkkkkkk.",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
]
PANEL_P = {"k": K, "A": "#c8ccd2", "M": "#8c9096"}
PANEL_GLOW = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".PPPPPPPPPPPPPP.",
    "aaaaaaaaaaaaaaaa",
    "bbbbbbbbbbbbbbbb",
    "cccccccccccccccc",
    "dddddddddddddddd",
    "................",
    "................",
]

HP = [
    "................",
    "................",
    "................",
    ".k.k.k.k.k.k.k..",
    ".kFkFkFkFkFkFk..",
    ".kFkFkFkFkFkFk..",
    "kkkkkkkkkkkkkkkk",
    "kHHHHHHHHHHHHHHk",
    "kMMMMMMMMMMMMMMk",
    "kkkkkkkkkkkkkkkk",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
]
HP_P = {"k": K, "F": "#6e737a", "H": "#5c5c62", "M": "#3c3c42"}
HP_GLOW = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "PPPPPPPPPPPPPPPP",
    "aaaaaaaaaaaaaaaa",
    "aaaaaaaaaaaaaaaa",
    "bbbbbbbbbbbbbbbb",
    "cccccccccccccccc",
    "dddddddddddddddd",
]
GLOW_P = {"P": "#ffffff", "a": "#ffffffa0", "b": "#ffffff70", "c": "#ffffff48", "d": "#ffffff24"}


def air_hose_entity():
    """Clear silicone hose (ribs repeat along u like the filter hose): light blue-grey with a highlight."""
    img = np.zeros((S, S, 4), np.uint8)
    for y in range(S):
        for x in range(S):
            v = _rgb("#9cc4d0") if y not in (4, 5) else _rgb("#e4f4fa")
            if x % 8 == 0:
                v = v * 0.85
            img[y, x, :3] = np.clip(v, 0, 255)
            img[y, x, 3] = 255
    return img


def write_all():
    out = []
    blocks = {
        "air_pump_side": pump_side(), "air_pump_top": pump_top(), "air_pump_front": pump_front(),
        "air_stone_side": stone_side(), "air_stone_top": stone_top(),
        "sponge_filter_sponge": sponge(2331), "sponge_filter_top_side": sponge_top_side(), "sponge_filter_top": sponge_top(),
        "intake_sponge_side": sponge(2332, "#202224"), "intake_sponge_top": sponge(2333, "#202224"),
        "corner_filter_segment_side": corner_side(), "corner_filter_segment_intake": corner_intake(),
        "corner_filter_segment_top": corner_top(), "corner_filter_segment_outlet": corner_outlet(),
        "led_panel": led_housing(2341, "#a8adb4"), "led_panel_panel_on": _panel(True), "led_panel_panel_off": _panel(False),
        "high_power_led": led_housing(2342, "#3a3c42"), "high_power_led_top": heat_sink(),
        "high_power_led_panel_on": hp_panel(True), "high_power_led_panel_off": hp_panel(False),
    }
    for name, img in blocks.items():
        out.append(save_png(img, BLOCK_TEX / f"{name}.png"))
    icons = {
        "air_pump": grid(PUMP, PUMP_P), "air_stone": grid(STONE, STONE_P), "air_hose": grid(HOSE, HOSE_P),
        "sponge_filter": grid(SPONGE_F, SPONGE_F_P), "corner_filter": grid(CORNER, CORNER_P), "intake_sponge": grid(INTAKE, INTAKE_P),
        "led_panel": grid(PANEL, PANEL_P), "led_panel_glow": grid(PANEL_GLOW, GLOW_P),
        "high_power_led": grid(HP, HP_P), "high_power_led_glow": grid(HP_GLOW, GLOW_P),
    }
    for name, img in icons.items():
        out.append(save_png(img, ITEM_TEX / f"{name}.png"))
    out.append(save_png(air_hose_entity(), ROOT / "common/src/main/resources/assets/bettacraft/textures/entity/filter/air_hose.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print(p)
