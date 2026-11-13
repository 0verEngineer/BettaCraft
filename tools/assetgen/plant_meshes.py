"""M21: textures of the plant mesh prototype (Rotala, Vallisneria, Anubias drawn with real leaf shapes in the tank
interior, ``client/interior/PlantMeshRenderer.java``).

Grey with alpha, tinted per species, part and redness at runtime (like the M13 block textures). Leaves are drawn on
quads in the leaf's own coordinates: u across the blade (0..1, the midrib at 0.5), v along it from the tip (v = 0) to the
base (v = 1). Cut-out alpha (binary), so they work in the cutout pipeline and with shader packs.

* ``leaf_round``  - Rotala rotundifolia: a small round-ovate leaf with a midrib.
* ``leaf_ovate``  - Anubias: a broad, thick ovate leaf with a short point, midrib and curved lateral veins, a darker rim.
* ``ribbon``      - Vallisneria: a long strap, tileable along v, fine parallel veins.
* ``stem``        - stems, petioles and the rhizome (tileable).

Run from the repo root:  py tools/assetgen/plant_meshes.py
"""
from __future__ import annotations

import math

import numpy as np

from common import BLOCK_TEX, save_png

OUT = BLOCK_TEX / "plant_mesh"
N = 64


def _rng(seed):
    return np.random.default_rng(seed)


def _finish(v, a):
    img = np.zeros(v.shape + (4,), np.float64)
    a = (a > 0.5).astype(np.float64)
    img[..., 0] = img[..., 1] = img[..., 2] = np.clip(v, 0, 1) * a
    img[..., 3] = a
    return img


def _leaf(width_at, veins: int, rim: float, seed: int, point: float = 0.0):
    """A leaf blade: width_at(t) = half width (0..0.5) at t = 0 (tip) .. 1 (base)."""
    r = _rng(seed)
    ys, xs = np.mgrid[0:N, 0:N]
    u = (xs + 0.5) / N - 0.5          # -0.5 .. 0.5 across
    t = (ys + 0.5) / N                # 0 tip .. 1 base
    half = np.vectorize(width_at)(t)
    inside = np.abs(u) <= half
    d_edge = np.clip(half - np.abs(u), 0, 1) / np.maximum(half, 1e-3)  # 0 at the rim, 1 at the midrib
    v = 0.80 + 0.12 * d_edge
    # midrib and lateral veins curving to the tip
    v -= 0.16 * (np.abs(u) < 0.018) * (t > 0.04)
    if veins:
        phase = (t + np.abs(u) * 1.2) * veins
        vein = np.abs(phase - np.round(phase)) < 0.06
        v -= 0.08 * vein * (d_edge > 0.12)
    v -= rim * (d_edge < 0.12)
    v += r.normal(0, 0.025, v.shape)
    # a little gloss on one side of the midrib
    v += 0.04 * ((u > 0.03) & (u < 0.12) & (t > 0.3) & (t < 0.65)) * d_edge
    return _finish(v, inside)


def leaf_round():
    # round-ovate: widest a little below the middle, blunt tip, short taper to the base
    return _leaf(lambda t: 0.47 * math.sin(math.pi * min(1.0, t * 1.02)) ** 0.75 if t < 0.97 else 0.0, 0, 0.10, 2101)


def leaf_ovate():
    # anubias: broad, widest at 60 %, a short point at the tip, rounded base
    def w(t):
        if t < 0.06:
            return 0.06 * t / 0.06
        s = (t - 0.06) / 0.94
        return 0.49 * math.sin(math.pi * min(1.0, s)) ** 0.6 * (1.0 - 0.15 * s)
    return _leaf(w, 6, 0.16, 2102)


def ribbon():
    r = _rng(2103)
    ys, xs = np.mgrid[0:N, 0:16]
    u = (xs + 0.5) / 16 - 0.5
    inside = np.abs(u) < 0.42
    v = 0.84 - 0.10 * (np.abs(np.abs(u) - 0.40) < 0.04)
    # fine parallel veins and soft cross bands (tileable along v)
    v -= 0.06 * ((xs % 4) == 1)
    v += 0.04 * np.sin(ys / N * 2 * math.pi * 3)
    v += r.normal(0, 0.02, v.shape)
    return _finish(v, inside)


def stem():
    r = _rng(2104)
    ys, xs = np.mgrid[0:16, 0:16]
    v = 0.78 + 0.08 * np.sin(xs / 16 * 2 * math.pi) + r.normal(0, 0.03, (16, 16))
    v -= 0.08 * ((ys % 8) == 0)  # nodes
    return _finish(v, np.ones((16, 16)))


def write_all() -> list:
    return [save_png(leaf_round(), OUT / "leaf_round.png"), save_png(leaf_ovate(), OUT / "leaf_ovate.png"),
            save_png(ribbon(), OUT / "ribbon.png"), save_png(stem(), OUT / "stem.png")]


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
