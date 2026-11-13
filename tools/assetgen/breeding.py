"""M2 assets: fry food item icons (bottles, cyst sachet) and the bubble nest textures (foam, eggs, larvae).

Everything is procedural and deterministic (fixed seeds). The bottles reuse the shape of the swamp water
bottle in icons.py with their own palettes and contents.
"""
from __future__ import annotations

import math

import numpy as np

from common import ITEM_TEX, TEX, save_png
from icons import BOTTLE, BOTTLE_ROWS, S, grid

NEST_TEX = TEX / "entity" / "nest"
N = 32  # nest textures are 32x32 (drawn as a ~0.7 block disc)


def _rng(seed):
    return np.random.default_rng(seed)


# ---------------------------------------------------------------- bottles

def _bottle(palette: dict, dots: list[tuple[int, int, str]] | None = None, seed: int = 0) -> np.ndarray:
    cmap = dict(BOTTLE)
    cmap.update(palette)
    img = grid(BOTTLE_ROWS, cmap)
    for x, y, col in dots or []:
        h = col.lstrip("#")
        img[y, x] = (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)
    return img


# liquid area of BOTTLE_ROWS: rows 8..13, columns 4..11 (inside the glass)
def _liquid_cells():
    cells = []
    for y, row in enumerate(BOTTLE_ROWS):
        for x, ch in enumerate(row):
            if ch in "Wwvds":
                cells.append((x, y))
    return cells


SALT = {"W": "#8cc4f0", "w": "#5ea2e0", "v": "#3f7fc4", "d": "#5ea2e0", "s": "#e8f6ff"}


def salt_water_bottle():
    return _bottle(SALT)


def infusoria_culture():
    """Swamp water with leaf bits, still clear-ish (not ready)."""
    pal = {"W": "#7c8a4a", "w": "#5f6e36", "v": "#46522a", "d": "#3f7a2a", "s": "#8e9a58"}
    leaf = [(5, 12, "#4c8c2c"), (6, 12, "#3a6e22"), (9, 11, "#5a9a34"), (8, 13, "#3a6e22"), (10, 13, "#4c8c2c")]
    return _bottle(pal, leaf)


def infusoria():
    """Ready infusoria: cloudy, milky green water with tiny specks."""
    pal = {"W": "#c4d49a", "w": "#a9bd7c", "v": "#8ea262", "d": "#a9bd7c", "s": "#e2ecc4"}
    rng = _rng(31)
    cells = _liquid_cells()
    specks = [cells[i] for i in rng.choice(len(cells), 7, replace=False)]
    return _bottle(pal, [(x, y, "#f4f8e4") for x, y in specks])


def artemia_culture():
    """Salt water with brown cysts settled at the bottom."""
    dots = [(5, 13, "#8a5a2a"), (6, 13, "#6e4420"), (8, 13, "#8a5a2a"), (9, 13, "#a06a34"), (7, 12, "#8a5a2a")]
    return _bottle(SALT, dots)


def artemia_nauplii():
    """Hatched nauplii: orange specks swimming in the salt water."""
    rng = _rng(47)
    cells = [c for c in _liquid_cells() if c[1] >= 9]
    specks = [cells[i] for i in rng.choice(len(cells), 9, replace=False)]
    return _bottle(SALT, [(x, y, "#f08a2a" if (x + y) % 2 else "#d8661a") for x, y in specks])


CYSTS = {"k": "#4a3a24", "p": "#d9c49a", "P": "#efe0bc", "q": "#b9a274", "c": "#7a4a22", "C": "#a0662e", "t": "#c0402a"}
CYST_ROWS = [
    "................",
    "................",
    "....kkkkkkkk....",
    "....kPPPPPPk....",
    "...kkkkkkkkkk...",
    "...kPPPPPPPPk...",
    "...kPttttttqk...",
    "...kPPPPPPPqk...",
    "...kPcCpcCpqk...",
    "...kpCcCpCcqk...",
    "...kPcpCcpCqk...",
    "...kpCcpCcpqk...",
    "...kPPPPPPPqk...",
    "...kqqqqqqqqk...",
    "....kkkkkkkk....",
    "................",
]


def artemia_cysts():
    return grid(CYST_ROWS, CYSTS)


ICONS = {
    "salt_water_bottle": salt_water_bottle,
    "infusoria_culture": infusoria_culture,
    "infusoria": infusoria,
    "artemia_culture": artemia_culture,
    "artemia_nauplii": artemia_nauplii,
    "artemia_cysts": artemia_cysts,
}


# ---------------------------------------------------------------- nest

def _disc_alpha(r, edge=0.1):
    """1 inside radius 0.42 of the texture, soft falloff to 0 at 0.5."""
    yy, xx = np.mgrid[0:N, 0:N]
    d = np.hypot((xx + 0.5) / N - 0.5, (yy + 0.5) / N - 0.5)
    return np.clip((r - d) / edge, 0, 1), d


def bubble_nest(seed=77, base_alpha=0.45, count=70):
    """Foam: packed small bubbles, white rims with a highlight, translucent centres; ragged outline."""
    rng = _rng(seed)
    img = np.zeros((N, N, 4), np.float64)
    alpha_disc, d = _disc_alpha(0.46, 0.08)
    # ragged outline: modulate the radius by angle
    yy, xx = np.mgrid[0:N, 0:N]
    ang = np.arctan2((yy + 0.5) / N - 0.5, (xx + 0.5) / N - 0.5)
    rag = 0.40 + 0.05 * np.sin(ang * 5 + 1.3) + 0.03 * np.sin(ang * 11 + 0.4)
    inside = d < rag
    base = np.where(inside, base_alpha, 0.0) * alpha_disc
    img[..., :3] = 0.86
    img[..., 3] = base
    for _ in range(count):
        cx, cy = rng.uniform(4, N - 4, 2)
        r = rng.uniform(1.2, 3.2)
        dd = np.hypot(xx + 0.5 - cx, yy + 0.5 - cy)
        if np.hypot(cx / N - 0.5, cy / N - 0.5) > 0.40:
            continue
        rim = (dd < r) & (dd > r - 1.0)
        img[rim, :3] = 0.97
        img[rim, 3] = np.maximum(img[rim, 3], 0.85)
        hx, hy = int(cx - r * 0.4), int(cy - r * 0.4)
        if 0 <= hx < N and 0 <= hy < N:
            img[hy, hx] = (1.0, 1.0, 1.0, 1.0)
        centre = dd < r - 1.0
        img[centre, 3] = np.maximum(img[centre, 3], 0.22)
    img[..., 3] *= inside
    return img


def nest_eggs():
    """Eggs (M5: bigger and with contrast): 2x2 cream-yellow spheres with a dark amber rim and a highlight,
    clustered in the middle, so they read against the white foam."""
    rng = _rng(91)
    img = np.zeros((N, N, 4), np.float64)
    taken = np.zeros((N, N), bool)
    placed = 0
    for _ in range(400):
        if placed >= 26:
            break
        a = rng.uniform(0, 2 * math.pi)
        rr = abs(rng.normal(0, 0.15)) * N
        x, y = int(N / 2 + math.cos(a) * rr), int(N / 2 + math.sin(a) * rr)
        if not (2 <= x < N - 3 and 2 <= y < N - 3) or taken[y - 1:y + 3, x - 1:x + 3].any():
            continue
        taken[y - 1:y + 3, x - 1:x + 3] = True
        for dx, dy in ((-1, 0), (2, 0), (0, -1), (1, -1), (-1, 1), (2, 1), (0, 2), (1, 2)):
            img[y + dy, x + dx] = (0.55, 0.38, 0.12, 0.85)
        img[y, x] = (1.0, 0.98, 0.86, 1.0)
        img[y, x + 1] = (0.98, 0.88, 0.50, 1.0)
        img[y + 1, x] = (0.96, 0.84, 0.44, 1.0)
        img[y + 1, x + 1] = (0.88, 0.70, 0.30, 1.0)
        placed += 1
    return img


def nest_larvae():
    """Larvae (M5: more visible): dark 2-texel heads with a yolk dot and a 3-texel tail hanging from the foam."""
    rng = _rng(93)
    img = np.zeros((N, N, 4), np.float64)
    taken = np.zeros((N, N), bool)
    placed = 0
    for _ in range(400):
        if placed >= 20:
            break
        a = rng.uniform(0, 2 * math.pi)
        rr = abs(rng.normal(0, 0.17)) * N
        x, y = int(N / 2 + math.cos(a) * rr), int(N / 2 + math.sin(a) * rr)
        horizontal = bool(rng.integers(2))
        dx, dy = (1, 0) if horizontal else (0, 1)
        if not (2 <= x < N - 6 and 2 <= y < N - 6) or taken[y - 1:y + 6, x - 1:x + 6].any():
            continue
        taken[y - 1:y + 6 if not horizontal else y + 2, x - 1:x + 6 if horizontal else x + 2] = True
        img[y, x] = (0.10, 0.09, 0.08, 1.0)
        img[y + dy, x + dx] = (0.20, 0.17, 0.14, 1.0)
        img[y + (1 - dy), x + (1 - dx)] = (0.95, 0.80, 0.40, 1.0)  # yolk sac beside the head
        for k, al in ((2, 0.95), (3, 0.8), (4, 0.55)):
            img[y + k * dy, x + k * dx] = (0.62, 0.56, 0.46, al)
        placed += 1
    return img


def nest_foam():
    """M15: the denser inner foam of the domed nest (more bubbles, less see-through), same ragged outline."""
    return bubble_nest(seed=78, base_alpha=0.68, count=110)


def nest_beads():
    """M15: 4x4 swatches for the 3D eggs and larvae in the domed nest (each cube samples one texel): row 0 egg cream,
    egg amber, larva head, larva tail; the rows below are slightly darker shades of the same."""
    img = np.zeros((4, 4, 4), np.float64)
    cols = [(1.0, 0.95, 0.74), (0.90, 0.70, 0.30), (0.13, 0.11, 0.09), (0.66, 0.60, 0.50)]
    for y in range(4):
        for x, (r, g, b) in enumerate(cols):
            k = 1.0 - 0.08 * y
            img[y, x] = (r * k, g * k, b * k, 1.0)
    return img


NEST = {"bubble_nest": bubble_nest, "nest_eggs": nest_eggs, "nest_larvae": nest_larvae, "nest_beads": nest_beads, "nest_foam": nest_foam}


def write_all() -> list:
    out = [save_png(f(), ITEM_TEX / f"{name}.png") for name, f in ICONS.items()]
    out += [save_png(f(), NEST_TEX / f"{name}.png") for name, f in NEST.items()]
    return out


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
