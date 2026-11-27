"""M40 assets: the guards and the pipe dock in the interior, the collar steel (the caps and flanges), the steel manifold of the
pipe bundle. M68: the filter pipes, their T-piece and the filter's water ports are gone; the glass and the steel pipe come with
the ring of every role of their network instead (blue: intake to a filter's inlet, orange: return from its outlet, green: fresh
water, brown: waste water, red: a short circuit), and the pipe wrench.

Block textures: brushed steel pipes with a coloured band per line (blue: the intake to the filter's inlet, orange: the
return from its outlet, the colours of the connectors), the brass collar of a T-piece, the fine mesh of the intake guard,
the mesh cap of the outlet guard and the flange of a pipe dock; the item icons of the T-piece and (part c) the pipe cover. Deterministic and
procedural, no vanilla pixels.

Run from the repo root:  py tools/assetgen/filter_lines.py
"""
from __future__ import annotations

import math

import numpy as np

from common import BLOCK_TEX, ITEM_TEX, save_png
from icons import grid
from plumbing import S, _img, _rgb, glass_pipe_tex, steel, steel_pipe_tex, transposed

INTAKE = "#3a8ad8"
RETURN = "#e08a30"
FRESH = "#4cb85a"
WASTE = "#7a5234"
SHORT = "#d8343a"
RINGS = {"intake": INTAKE, "return": RETURN, "fresh": FRESH, "waste": WASTE, "short": SHORT}


def line_pipe(colour, seed):
    """M65: a steel filter pipe (the steel pipe's look) with a ring of its line's colour at both ends of every block (rows 0
    and 15: a band at every joint), nothing else across it."""
    img = steel_pipe_tex("#b4bcc5", seed)
    c = _rgb(colour)
    for x in range(S):
        for y in (0, 15):
            img[y, x, :3] = np.clip(c * (0.9 + 0.1 * (x in (7, 8))), 0, 255).astype(np.uint8)
    return img


def ring_pipe(base, colour, glass):
    """M68: a glass or steel pipe texture with a ring of its role's colour at both ends of every block (rows 0 and 15)."""
    img = base.copy()
    c = _rgb(colour)
    for x in range(S):
        for y in (0, 15):
            img[y, x, :3] = np.clip(c * (0.9 + 0.1 * (x in (7, 8))), 0, 255).astype(np.uint8)
            if glass:
                img[y, x, 3] = 235
    return img


def tee_collar():
    """M65: the collar of a T-piece in the steel of the pipes (it was brass): a 4.5 px sleeve (columns 5.75..10.25), plain steel a
    little darker than the pipe."""
    from plumbing import pipe_shade

    def px(x, y):
        return _rgb("#a3abb4") * (0.97 + 0.03 * math.sin(x * 0.9))
    return _img(px)


def mesh(fine):
    """A steel mesh: thin wires over the dark opening (fine for the intake guard)."""
    step = 2 if fine else 3

    def px(x, y):
        if x in (0, 15) or y in (0, 15):
            return _rgb("#8e98a2")
        if x % step == 0 or y % step == 0:
            return _rgb("#c6ced6")
        return _rgb("#1c2228")
    return _img(px)


def flange():
    """The flange of a pipe dock: a steel ring with bolts around the dark opening."""
    def px(x, y):
        d = math.hypot(x - 7.5, y - 7.5)
        if d < 2.4:
            return _rgb("#14181c")
        if d < 3.4:
            return _rgb("#d2dae2")
        for a in range(6):
            bx, by = 7.5 + 5.2 * math.cos(a * math.pi / 3), 7.5 + 5.2 * math.sin(a * math.pi / 3)
            if math.hypot(x - bx, y - by) < 1.0:
                return _rgb("#4c545c")
        return _rgb("#a8b1ba") * (0.94 + 0.05 * math.sin(x * 0.7 + y))
    return _img(px)


def port(colour):
    """Fresh / waste water port face: a pipe socket with the colour ring of its line, a drop mark above it."""
    ring = _rgb(colour)

    def px(x, y):
        d = math.hypot(x - 7.5, y - 8.5)
        if d < 2.6:
            return _rgb("#14181c")
        if d < 3.8:
            return _rgb("#c8d0d8")
        if d < 4.8:
            return ring
        if 6 <= x <= 9 and 1 <= y <= 2:
            return ring * 0.9
        if x in (0, 15) or y in (0, 15):
            return _rgb("#8e98a2")
        return _rgb("#aab2bb") * (0.95 + 0.05 * math.sin(y))
    return _img(px)


def manifold():
    """The steel manifold of a pipe bundle: brushed steel with a darker frame and four rivets."""
    def px(x, y):
        base = _rgb("#a9b2bc") * (0.93 + 0.05 * math.sin(x * 0.8) + 0.02 * ((x * 5 + y * 11) % 7) / 7)
        if x in (0, 15) or y in (0, 15):
            return base * 0.7
        if x in (1, 14) or y in (1, 14):
            return base * 0.86
        if (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
            return _rgb("#5c646c")
        return base
    return _img(px)


TEE = [
    "................",
    "................",
    "......bbbb......",
    "......bSSb......",
    "......bSSb......",
    "......bSSb......",
    ".bbbbbcccccbbbb.",
    ".bSSSScCCcSSSSb.",
    ".bsssscCCcssssb.",
    ".bbbbbcccccbbbb.",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
]
TEE_P = {"b": "#5c646c", "S": "#d0d8e0", "s": "#9aa4ae", "c": "#6c757e", "C": "#b9c1ca"}  # M65: a steel collar (was brass)


# M68: the pipe wrench, a stainless steel pipe wrench with an adjustable jaw and a dark grip
WRENCH = [
    "................",
    "........bbb.....",
    ".......bSSSb....",
    "......bSssSb....",
    "......bSbbbb....",
    ".......bSSSb....",
    "........bSsb....",
    ".......bSsb.....",
    "......bSsb......",
    ".....bgGb.......",
    "....bgGb........",
    "...bgGb.........",
    "..bgGb..........",
    "..bGb...........",
    "...b............",
    "................",
]
WRENCH_P = {"b": "#3c4248", "S": "#d4dbe2", "s": "#98a2ac", "g": "#2a2f35", "G": "#5a3a2a"}


# M40 part c: the pipe cover, a steel trowel with a brick-faced plate on its blade (the cover it puts on)
COVER = [
    "................",
    "................",
    "........bbbbbbb.",
    ".......bRRrRRRb.",
    "......bRRrRRRRb.",
    ".....brrrrrrrrb.",
    ".....bRRRRrRRb..",
    "....bRRRRrRRb...",
    "....bbbbbbbbb...",
    "......sSs.......",
    ".....sSs........",
    "....hHs.........",
    "...hHh..........",
    "..hHh...........",
    "..hh............",
    "................",
]
COVER_P = {"b": "#4c545c", "R": "#b65a3c", "r": "#d8cfc0", "s": "#9aa4ae", "S": "#d0d8e0", "h": "#5a3a22", "H": "#8a5a32"}


def write_all():
    out = []
    blocks = {
        "filter_pipe_bundle": manifold(),
        "pipe_tee": tee_collar(), "intake_guard": mesh(True), "outlet_guard": mesh(False), "pipe_anchor": flange(),
    }
    # M68: the rings of the roles on both pipes (and the transposed copies, see plumbing.py)
    for name, col in RINGS.items():
        g = ring_pipe(glass_pipe_tex(), col, True)
        st = ring_pipe(steel_pipe_tex(), col, False)
        blocks[f"glass_pipe_{name}"] = g
        blocks[f"glass_pipe_{name}_r"] = transposed(g)
        blocks[f"stainless_steel_pipe_{name}"] = st
        blocks[f"stainless_steel_pipe_{name}_r"] = transposed(st)
    for name, img in blocks.items():
        out.append(save_png(img, BLOCK_TEX / f"{name}.png"))
    out.append(save_png(grid(WRENCH, WRENCH_P), ITEM_TEX / "pipe_wrench.png"))
    out.append(save_png(grid(COVER, COVER_P), ITEM_TEX / "pipe_cover.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print(p)
