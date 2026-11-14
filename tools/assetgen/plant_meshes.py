"""M21: textures of the plant mesh prototype (Rotala, Vallisneria, Anubias drawn with real leaf shapes in the tank
interior, ``client/interior/PlantMeshRenderer.java``).

Grey with alpha, tinted per species, part and redness at runtime (like the M13 block textures). Leaves are drawn on
quads in the leaf's own coordinates: u across the blade (0..1, the midrib at 0.5), v along it from the tip (v = 0) to the
base (v = 1). Cut-out alpha (binary), so they work in the cutout pipeline and with shader packs.

* ``leaf_round``  - Rotala rotundifolia: a small round-ovate leaf with a midrib.
* ``leaf_ovate``  - Anubias: a broad, thick ovate leaf with a short point, midrib and curved lateral veins, a darker rim.
* ``ribbon``      - Vallisneria: a long strap, tileable along v, fine parallel veins.
* ``stem``        - stems, petioles and the rhizome (tileable).

M21.1, the miniature outside the tank (1/16 scale, from the block atlas with mipmaps): ``*_mini`` variants of the leaves
and the ribbon. The shape grown by 2 px, the colour flatter and a little lighter, and the colour bled into the transparent
texels, so the mipmap levels neither darken the rim nor eat the thin leaves (the alpha of the smaller mip levels stays
above the cut-out threshold where the leaf is).

M22.1: every other plant and coral (``M22_1`` below: leaves of each species, moss, polyps, coral tissue, tentacles).

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


def _mini(img, grow: int = 2, flat: float = 0.5, lift: float = 0.06):
    """The miniature variant: alpha dilated by ``grow`` texels, shading flattened towards the mean, colour bled out."""
    a = img[..., 3] > 0.5
    v = img[..., 0].copy()
    mean = v[a].mean() if a.any() else 0.8
    v = np.where(a, mean + (v - mean) * flat + lift, 0)
    grown = a.copy()
    for _ in range(grow):
        g = grown.copy()
        g[1:, :] |= grown[:-1, :]
        g[:-1, :] |= grown[1:, :]
        g[:, 1:] |= grown[:, :-1]
        g[:, :-1] |= grown[:, 1:]
        grown = g
    # bleed: every texel outside the leaf gets the mean colour (mipmaps average colour with transparent texels too)
    v = np.where(a, v, mean + lift)
    out = np.zeros_like(img)
    out[..., 0] = out[..., 1] = out[..., 2] = np.clip(v, 0, 1)
    out[..., 3] = grown.astype(np.float64)
    return out


# ---------------------------------------------------------------------------- M22.1

def _wavy_edge(base, amp, waves):
    return lambda t: max(0.0, base(t) * (1.0 + amp * math.sin(t * math.pi * 2 * waves)))


def leaf_lance():
    def w(t):  # tip t=0 .. base t=1; widest towards the base, long taper to the tip
        if t > 0.97:
            return 0.0
        s = t / 0.97
        return 0.30 * math.sin(math.pi * min(1.0, s ** 0.75)) ** 0.9
    return _leaf(_wavy_edge(w, 0.035, 6), 9, 0.10, 2201)


def leaf_wavy():
    def w(t):
        return 0.44 * math.sin(math.pi * min(1.0, t * 1.03)) ** 0.7 if t < 0.97 else 0.0
    return _leaf(_wavy_edge(w, 0.07, 4), 4, 0.14, 2202)


def leaf_needle():
    return _leaf(lambda t: 0.16 * math.sin(math.pi * min(1.0, t * 1.02)) ** 0.5 if t < 0.98 else 0.0, 0, 0.08, 2203)


def leaf_sword():
    def w(t):
        if t > 0.98:
            return 0.0
        return 0.36 * math.sin(math.pi * min(1.0, t ** 0.85)) ** 0.85
    img = _leaf(w, 0, 0.10, 2204)
    # two curved veins either side of the midrib, from the base to the tip
    ys, xs = np.mgrid[0:N, 0:N]
    u = (xs + 0.5) / N - 0.5
    t = (ys + 0.5) / N
    half = np.vectorize(w)(t)
    for f in (0.45, 0.75):
        vein = np.abs(np.abs(u) - half * f) < 0.012
        img[..., :3] -= (0.07 * vein * (img[..., 3] > 0))[..., None]
    img[..., :3] = np.clip(img[..., :3], 0, 1)
    return img


def leaf_crypt():
    def w(t):
        if t > 0.97:
            return 0.0
        return 0.40 * math.sin(math.pi * min(1.0, t ** 0.9)) ** 0.7
    img = _leaf(_wavy_edge(w, 0.045, 7), 5, 0.12, 2205)
    r = _rng(2215)
    blot = r.normal(0, 1, (N // 8, N // 8))
    blot = np.kron(blot, np.ones((8, 8)))
    img[..., :3] += (0.035 * blot * (img[..., 3] > 0))[..., None]
    img[..., :3] = np.clip(img[..., :3], 0, 1)
    return img


def leaf_elliptic():
    return _leaf(lambda t: 0.46 * math.sin(math.pi * min(1.0, t * 1.02)) ** 0.62 if t < 0.97 else 0.0, 5, 0.10, 2206)


def leaf_narrow():
    def w(t):
        if t > 0.98:
            return 0.0
        return 0.25 * math.sin(math.pi * min(1.0, t ** 0.8)) ** 0.8
    return _leaf(_wavy_edge(w, 0.05, 5), 6, 0.10, 2207)


def _blotches(r, share):
    """Soft round blotches covering about ``share`` of the texture (smooth noise, thresholded)."""
    from PIL import Image
    low = (r.random((9, 9)) * 255).astype(np.uint8)
    big = np.asarray(Image.fromarray(low).resize((N, N), Image.BICUBIC), np.float64) / 255
    return big > np.quantile(big, 1 - share)


def lotus_leaf():
    # ovate, the notch (sinus) at the base where the stalk sits, mottled with darker blotches
    r = _rng(2208)
    ys, xs = np.mgrid[0:N, 0:N]
    u = (xs + 0.5) / N - 0.5
    t = (ys + 0.5) / N
    half = 0.48 * np.sin(np.pi * np.clip(t, 0, 1)) ** 0.55
    inside = (np.abs(u) <= half) & ~((t > 0.82) & (np.abs(u) < (t - 0.82) * 0.9))
    d_edge = np.clip(half - np.abs(u), 0, 1) / np.maximum(half, 1e-3)
    v = 0.78 + 0.12 * d_edge
    v -= 0.14 * (np.abs(u) < 0.016) * (t < 0.84)
    ang = np.arctan2(u, 0.85 - t)
    v -= 0.06 * (np.abs(np.sin(ang * 7)) < 0.12) * (d_edge > 0.1)
    blot = _blotches(r, 0.30)
    v -= 0.20 * blot
    v -= 0.12 * (d_edge < 0.08)
    v += r.normal(0, 0.02, v.shape)
    return _finish(v, inside)


def lotus_pad():
    r = _rng(2209)
    ys, xs = np.mgrid[0:N, 0:N]
    x = (xs + 0.5) / N * 2 - 1
    z = (ys + 0.5) / N * 2 - 1
    rr = np.sqrt(x * x + z * z)
    ang = np.arctan2(x, z)  # the notch towards +z (v = 1)
    inside = (rr < 0.97) & ~((np.abs(ang) < 0.16) & (rr > 0.05))
    v = 0.80 - 0.06 * rr
    v -= 0.07 * (np.abs(np.sin(ang * 9)) < 0.1) * (rr > 0.12)
    blot = _blotches(r, 0.32)
    v -= 0.20 * blot
    v -= 0.12 * (rr > 0.9)
    v += r.normal(0, 0.02, v.shape)
    return _finish(v, inside)


def moss():
    r = _rng(2210)
    a = np.zeros((N, N), bool)
    v = np.zeros((N, N))

    def walk(x, y, ang, n, depth):
        for i in range(n):
            ix, iy = int(round(x)), int(round(y))
            if 0 <= ix < N and 0 <= iy < N:
                a[iy, ix] = True
                v[iy, ix] = 0.72 + 0.12 * (i / max(1, n)) + r.normal(0, 0.03)
            x += math.cos(ang)
            y -= math.sin(ang)
            ang += r.normal(0, 0.25)
            if depth < 3 and r.random() < 0.18:
                walk(x, y, ang + r.choice([-1, 1]) * r.uniform(0.5, 1.1), int(n * 0.55), depth + 1)

    for _ in range(3):
        walk(N / 2 + r.normal(0, 5), N - 1, math.pi / 2 + r.normal(0, 0.4), 50, 0)
    # one texel thicker so the sprigs keep their colour in the smaller mip levels
    g = a.copy()
    g[:, 1:] |= a[:, :-1]
    v2 = v.copy()
    v2[:, 1:] = np.where(a[:, 1:], v[:, 1:], v[:, :-1])
    return _finish(np.where(g, np.maximum(v2, 0.6), 0), g)


def coral_mat():
    r = _rng(2211)
    m = 32
    ys, xs = np.mgrid[0:m, 0:m]
    pts = r.random((14, 2)) * m
    d = np.full((m, m), 1e9)
    d2 = np.full((m, m), 1e9)
    for px, py in pts:
        for ox in (-m, 0, m):
            for oy in (-m, 0, m):
                dd = np.sqrt((xs - px - ox) ** 2 + (ys - py - oy) ** 2)
                d2 = np.where(dd < d, d, np.minimum(d2, dd))
                d = np.minimum(d, dd)
    edge = (d2 - d) < 1.6
    v = 0.80 - 0.10 * edge + 0.05 * np.clip(1 - d / 6, 0, 1) + r.normal(0, 0.02, (m, m))
    return _finish(v, np.ones((m, m)))


def _radial(seed, n=N):
    ys, xs = np.mgrid[0:n, 0:n]
    x = (xs + 0.5) / n * 2 - 1
    z = (ys + 0.5) / n * 2 - 1
    return x, z, np.sqrt(x * x + z * z), np.arctan2(z, x), _rng(seed)


def zoa_polyp():
    x, z, rr, ang, r = _radial(2212)
    fringe = 0.78 + 0.16 * np.maximum(0, np.cos(ang * 13))
    inside = rr < fringe
    v = np.where(rr < 0.62, 0.92 - 0.25 * (rr < 0.2) + 0.05 * np.cos(ang * 6), 0.70)
    v -= 0.10 * ((rr > 0.58) & (rr < 0.66))
    v += r.normal(0, 0.02, v.shape)
    return _finish(v, inside)


def star_polyp():
    x, z, rr, ang, r = _radial(2213)
    k = 8
    a = (ang % (2 * math.pi / k)) - math.pi / k  # angle to the nearest tentacle axis
    width = 0.11 * (1 - rr) + 0.02
    arm = (np.abs(a) * rr < width) & (rr < 0.96)
    pinnule = (np.abs(np.abs(a) * rr - width - 0.03) < 0.03) & (np.sin(rr * 40) > 0.3) & (rr < 0.9) & (rr > 0.25)
    disc = rr < 0.22
    inside = arm | pinnule | disc
    v = np.where(disc, 0.70 - 0.3 * (rr < 0.07), 0.95 - 0.15 * rr)
    v += r.normal(0, 0.02, v.shape)
    return _finish(v, inside)


def mushroom_disc():
    x, z, rr, ang, r = _radial(2214)
    rim = 0.94 + 0.04 * np.cos(ang * 11)
    inside = rr < rim
    v = 0.80 - 0.06 * (np.abs(np.sin(ang * 14)) < 0.18) * (rr > 0.2)
    v -= 0.22 * (rr < 0.07)
    dots = r.random(v.shape) < 0.05
    v += 0.10 * dots * (rr > 0.15)
    v -= 0.08 * (rr > rim - 0.08)
    v += r.normal(0, 0.02, v.shape)
    return _finish(v, inside)


def coral_flesh():
    r = _rng(2216)
    m = 32
    v = 0.82 + r.normal(0, 0.03, (m, m))
    pores = r.random((m, m)) < 0.06
    v -= 0.12 * pores
    return _finish(v, np.ones((m, m)))


def acro():
    r = _rng(2217)
    ys, xs = np.mgrid[0:32, 0:16]
    v = 0.84 + r.normal(0, 0.02, (32, 16))
    cx = (xs % 8) - 3.5
    cy = ((ys + (xs // 8) * 4) % 8) - 3.5
    d = np.sqrt(cx * cx + cy * cy)
    v -= 0.14 * ((d > 1.4) & (d < 2.4))
    v -= 0.20 * (d < 0.9)
    return _finish(v, np.ones((32, 16)))


def tentacle():
    r = _rng(2218)
    ys, xs = np.mgrid[0:32, 0:16]
    v = 0.84 + 0.05 * np.sin(xs / 16 * 2 * math.pi) - 0.05 * ((ys % 8) == 0) + r.normal(0, 0.02, (32, 16))
    return _finish(v, np.ones((32, 16)))


def tuft():
    x, z, rr, ang, r = _radial(2219, 32)
    k = 7
    a = (ang % (2 * math.pi / k)) - math.pi / k
    arm = (np.abs(a) * rr < 0.13 * (1 - rr) + 0.05) & (rr < 0.95)
    disc = rr < 0.3
    v = np.where(rr > 0.75, 0.98, 0.82) - 0.2 * (rr < 0.1)
    return _finish(v + r.normal(0, 0.02, v.shape), arm | disc)


def bubble():
    x, z, rr, ang, r = _radial(2220, 32)
    inside = rr < 0.95
    light = np.clip(1 - np.sqrt((x + 0.35) ** 2 + (z + 0.35) ** 2) / 0.6, 0, 1)
    v = 0.70 + 0.18 * (1 - rr) + 0.22 * light ** 2
    v -= 0.15 * (rr > 0.82)
    return _finish(v, inside)


# texture name -> (generator, mini variant: None = none, else the dilation in texels)
M22_1 = {
    "leaf_lance": (leaf_lance, 2), "leaf_wavy": (leaf_wavy, 2), "leaf_needle": (leaf_needle, 2),
    "leaf_sword": (leaf_sword, 2), "leaf_crypt": (leaf_crypt, 2), "leaf_elliptic": (leaf_elliptic, 2),
    "leaf_narrow": (leaf_narrow, 2), "lotus_leaf": (lotus_leaf, 2), "lotus_pad": (lotus_pad, 1),
    "moss": (moss, 1), "coral_mat": (coral_mat, None), "zoa_polyp": (zoa_polyp, 1), "star_polyp": (star_polyp, 2),
    "mushroom_disc": (mushroom_disc, 1), "coral_flesh": (coral_flesh, None), "acro": (acro, None),
    "tentacle": (tentacle, None), "tuft": (tuft, 2), "bubble": (bubble, 1),
}


def write_m22_1() -> list:
    out = []
    for name, (gen, grow) in M22_1.items():
        img = gen()
        out.append(save_png(img, OUT / f"{name}.png"))
        if grow is not None:
            out.append(save_png(_mini(img, grow=grow), OUT / f"{name}_mini.png"))
    return out


def write_all() -> list:
    lr, lo, rb = leaf_round(), leaf_ovate(), ribbon()
    return [save_png(lr, OUT / "leaf_round.png"), save_png(lo, OUT / "leaf_ovate.png"),
            save_png(rb, OUT / "ribbon.png"), save_png(stem(), OUT / "stem.png"),
            save_png(_mini(lr), OUT / "leaf_round_mini.png"), save_png(_mini(lo), OUT / "leaf_ovate_mini.png"),
            save_png(_mini(rb, grow=1), OUT / "ribbon_mini.png")] + write_m22_1()


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
