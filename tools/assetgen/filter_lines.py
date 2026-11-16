"""M40 assets: the filter pipes (intake and return line), the pipe T-piece, the guards and the pipe dock in the interior.

Block textures: brushed steel pipes with a coloured band per line (blue: the intake to the filter's inlet, orange: the
return from its outlet, the colours of the connectors), the brass collar of a T-piece, the fine mesh of the intake guard,
the mesh cap of the outlet guard and the flange of a pipe dock; the item icon of the T-piece. Deterministic and
procedural, no vanilla pixels.

Run from the repo root:  py tools/assetgen/filter_lines.py
"""
from __future__ import annotations

import math

import numpy as np

from common import BLOCK_TEX, ITEM_TEX, save_png
from icons import grid
from plumbing import S, _img, _rgb, steel

INTAKE = "#3a8ad8"
RETURN = "#e08a30"


def line_pipe(colour, seed):
    """A steel filter pipe with two coloured bands (the line) and a dark seam."""
    img = steel(seed, "#b4bcc5")
    c = _rgb(colour)
    for x in range(S):
        for y in (3, 4, 11, 12):
            img[y, x, :3] = np.clip(c * (0.92 + 0.08 * math.sin(x * 0.9)), 0, 255).astype(np.uint8)
        img[7, x, :3] = (np.array(img[7, x, :3], float) * 0.85).astype(np.uint8)
    return img


def tee_collar():
    """The collar of a T-piece: brushed brass with rivets in the corners."""
    def px(x, y):
        base = _rgb("#c9a04a") * (0.92 + 0.06 * math.sin(y * 1.3) + 0.02 * ((x * 7 + y * 3) % 5) / 5)
        if x in (0, 15) or y in (0, 15):
            base = base * 0.75
        if (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
            return _rgb("#5a4720")
        return base
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
TEE_P = {"b": "#5c646c", "S": "#d0d8e0", "s": "#9aa4ae", "c": "#8a6a2a", "C": "#d8b05a"}


def write_all():
    out = []
    blocks = {
        "filter_inlet_pipe": line_pipe(INTAKE, 4001), "filter_outlet_pipe": line_pipe(RETURN, 4002),
        "pipe_tee": tee_collar(), "intake_guard": mesh(True), "outlet_guard": mesh(False), "pipe_anchor": flange(),
    }
    for name, img in blocks.items():
        out.append(save_png(img, BLOCK_TEX / f"{name}.png"))
    out.append(save_png(grid(TEE, TEE_P), ITEM_TEX / "pipe_tee.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print(p)
