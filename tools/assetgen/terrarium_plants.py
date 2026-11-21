"""M56 assets: the terrarium and paludarium plants (bromeliads, ferns, mosses, orchids, carnivorous plants, sedges, cacti and
succulents).

All grey and tinted at runtime in the colours of the species (``PlantColors.java``), like the aquarium plants:

* block textures of the interior blocks (``textures/block/plant_leaf_<name>``, 16x16, binary alpha) for the new leaf shapes;
* the textures of the plant meshes (``textures/block/plant_mesh/<name>`` 64x64 and their ``_mini`` variants for the miniature;
  the pitcher tube and the cactus body tiled and opaque);
* the item icons per look (``textures/item/terrarium_<icon>`` = leaves, tinted with the leaf colour, and ``_detail`` =
  flowers, bracts, spines, tinted with the top colour), the icon of each species by ``ICON`` (``LandLook.icon``);
* the plants in the world (``WildLandPlantBlock``): three-layer cross textures per look (``textures/block/wild_land_<look>``,
  ``_accent``, ``_top``; drawn with the canvas of ``wild_plants.py``), the cactus body (``wild_land_cactus``) and the box
  templates of the cacti (``models/block/template_wild_cactus_*``).

Run from the repo root:  py tools/assetgen/terrarium_plants.py [preview.png]
"""
from __future__ import annotations

import json
import math

import numpy as np

import aquatic_plants as aq
import plant_meshes as pm
import wild_plants as wp
from common import BLOCK_TEX, ITEM_TEX, save_png

S = 16
N = pm.N


def _rng(seed):
    return np.random.default_rng(seed)


# ------------------------------------------------------------------ block textures (16x16) of the interior blocks


def _grey_img(v, a):
    img = np.zeros((S, S, 4))
    a = a.astype(np.float64)
    img[..., 0] = img[..., 1] = img[..., 2] = np.clip(v, 0, 1) * a
    img[..., 3] = a
    return img


def block_strap(seed=5601, bands=False):
    """Bromeliad straps: long parallel leaves across the block, lighter edges, a central furrow; ``bands``: dark cross bands."""
    r = _rng(seed)
    ys, xs = np.mgrid[0:S, 0:S]
    v = 0.80 + 0.06 * np.sin(xs * 1.6) + r.normal(0, 0.025, (S, S))
    v -= 0.08 * ((xs % 4) == 0)
    if bands:
        v -= 0.16 * ((ys % 5) < 2)
    return _grey_img(v, np.ones((S, S)))


def block_spiky(seed=5602):
    """Aloe, agave, air plant: thick pointed leaves with teeth along the rim."""
    img = aq.leaf_needle(seed)
    a = img[..., 3] > 0.5
    r = _rng(seed)
    for _ in range(14):
        x, y = int(r.integers(0, S)), int(r.integers(0, S))
        if a[y, x]:
            img[y, x, :3] = 0.95
    return img


def block_veined(seed=5603):
    """Fittonia: a dark leaf netted with light veins."""
    ys, xs = np.mgrid[0:S, 0:S]
    v = np.full((S, S), 0.66)
    v += 0.30 * (((xs + ys) % 5 == 0) | ((xs - ys) % 5 == 0) | (xs == 7) | (xs == 8))
    v += _rng(seed).normal(0, 0.02, (S, S))
    return _grey_img(v, np.ones((S, S)))


def block_heart(seed=5604):
    """Pothos: broad heart leaves with pale marbling."""
    img = aq.leaf_broad(seed, holes=0.03, vein=True, gloss=True)
    r = _rng(seed)
    for _ in range(18):
        x, y = int(r.integers(0, S)), int(r.integers(0, S))
        if img[y, x, 3] > 0:
            img[y, x, :3] = min(1.0, img[y, x, 0] + 0.18)
    return img


def block_shingle(seed=5605):
    """Marcgravia: round leaves overlapping like shingles."""
    ys, xs = np.mgrid[0:S, 0:S]
    v = np.full((S, S), 0.78)
    for cy in range(2, S + 4, 5):
        for cx in range(2 + (cy // 5 % 2) * 3, S + 4, 6):
            d = np.hypot(xs - cx, ys - cy)
            v = np.where(d < 3.2, 0.82 - 0.06 * (d > 2.4) + 0.04 * (ys < cy), v)
    v += _rng(seed).normal(0, 0.02, (S, S))
    return _grey_img(v, np.ones((S, S)))


def block_fern(seed=5606):
    """Fern fronds: a midrib with leaflets on both sides (open, cutout)."""
    a = np.zeros((S, S), bool)
    v = np.zeros((S, S))
    for x0 in (3, 11):
        for y in range(S):
            a[y, x0] = True
            v[y, x0] = 0.7
            if y % 2 == 0:
                for k in range(1, 4):
                    for x in (x0 - k, x0 + k):
                        if 0 <= x < S:
                            a[y, x] = True
                            v[y, x] = 0.86 - 0.04 * k
    return _grey_img(v, a)


def block_lichen(seed=5607):
    """Reindeer lichen: a dense tangle of pale branchlets."""
    r = _rng(seed)
    v = 0.72 + 0.2 * r.random((S, S))
    a = r.random((S, S)) < 0.86
    return _grey_img(v, a)


def block_velvet(seed=5608):
    """Begonia rex: a silvery leaf with a dark centre and a spiral band."""
    ys, xs = np.mgrid[0:S, 0:S]
    d = np.hypot(xs - 7.5, ys - 7.5)
    ang = np.arctan2(ys - 7.5, xs - 7.5)
    v = 0.86 - 0.32 * (d < 2.6) - 0.22 * (np.abs(np.sin(ang * 2 + d * 0.6)) < 0.25) * (d > 3) + _rng(seed).normal(0, 0.02, (S, S))
    return _grey_img(v, np.ones((S, S)))


def block_sundew(seed=5609):
    """Sundew: small round pads covered in glistening tentacles (light dots)."""
    r = _rng(seed)
    ys, xs = np.mgrid[0:S, 0:S]
    v = np.full((S, S), 0.7)
    a = np.zeros((S, S), bool)
    for cy, cx in ((4, 4), (4, 12), (12, 4), (12, 12), (8, 8)):
        d = np.hypot(xs - cx, ys - cy)
        a |= d < 3.1
        v = np.where((d > 2.0) & (d < 3.1), 0.95, v)
    v += r.normal(0, 0.02, (S, S))
    return _grey_img(v, a)


def block_pitcher(seed=5610):
    """A pitcher tube: veined walls (the veins run up), the rim darker."""
    ys, xs = np.mgrid[0:S, 0:S]
    v = 0.80 - 0.12 * ((xs % 3) == 0) + 0.05 * np.sin(ys * 0.8) + _rng(seed).normal(0, 0.02, (S, S))
    return _grey_img(v, np.ones((S, S)))


def block_trap(seed=5611):
    """Flytrap: lobes with a fringe of teeth."""
    a = np.ones((S, S), bool)
    v = np.full((S, S), 0.8)
    for x in range(0, S, 2):
        a[0, x] = False
        a[S - 1, x + 1 if x + 1 < S else x] = False
    v[1, :] = 0.95
    v[S - 2, :] = 0.95
    return _grey_img(v + _rng(seed).normal(0, 0.02, (S, S)), a)


def block_cactus(seed=5612):
    """Cactus body: vertical ribs, areoles (light dots) on the ribs."""
    ys, xs = np.mgrid[0:S, 0:S]
    v = 0.74 + 0.12 * np.cos(xs / S * 2 * math.pi * 4) + _rng(seed).normal(0, 0.02, (S, S))
    v = np.where(((xs % 4) == 0) & ((ys % 4) == 1), 1.0, v)
    return _grey_img(v, np.ones((S, S)))


def block_pad(seed=5613):
    """Prickly pear pad: a flat green surface dotted with areoles."""
    ys, xs = np.mgrid[0:S, 0:S]
    v = 0.8 + _rng(seed).normal(0, 0.02, (S, S))
    v = np.where(((xs + 2 * (ys // 4)) % 4 == 0) & ((ys % 4) == 2), 1.0, v)
    return _grey_img(v, np.ones((S, S)))


def block_succulent(seed=5614):
    """Fat succulent leaves: plump, smooth, with a pale bloom (light rims)."""
    ys, xs = np.mgrid[0:S, 0:S]
    d = np.minimum(np.abs(((xs + 2) % 6) - 3), 3) / 3.0
    v = 0.92 - 0.14 * d + _rng(seed).normal(0, 0.015, (S, S))
    return _grey_img(v, np.ones((S, S)))


def block_zebra(seed=5615):
    """Haworthia: dark green with white cross bands of warts."""
    ys, xs = np.mgrid[0:S, 0:S]
    v = 0.62 + _rng(seed).normal(0, 0.02, (S, S))
    v = np.where(((ys % 3) == 0) & ((xs + ys) % 2 == 0), 1.0, v)
    return _grey_img(v, np.ones((S, S)))


def block_round(seed=5616):
    """Small round leaves (Peperomia, stonecrop) packed together."""
    ys, xs = np.mgrid[0:S, 0:S]
    v = np.full((S, S), 0.0)
    a = np.zeros((S, S), bool)
    for cy in range(2, S, 4):
        for cx in range(2 + (cy // 4 % 2) * 2, S, 4):
            d = np.hypot(xs - cx, ys - cy)
            m = d < 2.2
            a |= m
            v = np.where(m, 0.9 - 0.1 * d / 2.2, v)
    return _grey_img(v, a)


def block_narrow(seed=5617):
    """Sedges and grasses: narrow upright blades."""
    r = _rng(seed)
    a = np.zeros((S, S), bool)
    v = np.zeros((S, S))
    for x in range(0, S, 2):
        top = int(r.integers(0, 6))
        a[top:, x] = True
        v[top:, x] = 0.78 + 0.1 * r.random()
    return _grey_img(v, a)


BLOCKS = {
    "strap": block_strap, "banded": lambda: block_strap(5618, True), "spiky": block_spiky, "veined": block_veined, "heart": block_heart,
    "shingle": block_shingle, "fern": block_fern, "lichen": block_lichen, "velvet": block_velvet, "sundew": block_sundew,
    "pitcher": block_pitcher, "trap": block_trap, "cactus": block_cactus, "pad": block_pad, "succulent": block_succulent,
    "zebra": block_zebra, "round": block_round, "narrow": block_narrow,
}


# ------------------------------------------------------------------ mesh textures (64x64, the leaf from tip v=0 to base v=1)


def _strap_w(t):
    return 0.0 if t < 0.02 else 0.42 * min(1.0, (t - 0.02) / 0.12) ** 0.6


def mesh_strap(bands=False, seed=5621):
    img = pm._leaf(_strap_w, 0, 0.12, seed)
    if bands:
        ys, _ = np.mgrid[0:N, 0:N]
        band = (ys % 10) < 4
        img[..., :3] = np.where(band[..., None] & (img[..., 3:4] > 0), img[..., :3] * 0.72, img[..., :3])
    return img


def mesh_spiky():
    img = pm._leaf(lambda t: 0.36 * min(1.0, t * 1.1) ** 1.2, 0, 0.10, 5622)
    ys, xs = np.mgrid[0:N, 0:N]
    u = (xs + 0.5) / N - 0.5
    t = (ys + 0.5) / N
    half = 0.36 * np.minimum(1.0, t * 1.1) ** 1.2
    teeth = (np.abs(np.abs(u) - half) < 0.05) & ((ys % 6) < 2) & (t > 0.2)
    img[teeth, :3] = 0.97
    img[teeth, 3] = 1.0
    return img


def mesh_veined():
    img = pm._leaf(lambda t: 0.46 * math.sin(math.pi * min(1.0, t * 1.02)) ** 0.7, 9, 0.08, 5623)
    ys, xs = np.mgrid[0:N, 0:N]
    u = (xs + 0.5) / N - 0.5
    t = (ys + 0.5) / N
    net = (np.abs(((t * 9 + np.abs(u) * 6) % 1) - 0.5) < 0.07) | (np.abs(((t * 9 - np.abs(u) * 6) % 1) - 0.5) < 0.05) | (np.abs(u) < 0.02)
    inside = img[..., 3] > 0
    img[..., :3] = np.where((net & inside)[..., None], 0.97, img[..., :3] * 0.85)
    return img


def mesh_heart():
    def w(t):
        if t < 0.05:
            return 0.08 * t / 0.05
        s = (t - 0.05) / 0.95
        return 0.48 * math.sin(math.pi * min(1.0, s * 0.92)) ** 0.55 * (1.0 if s < 0.85 else max(0.6, 1 - (s - 0.85) * 2.4))
    img = pm._leaf(w, 5, 0.12, 5624)
    r = _rng(5624)
    inside = img[..., 3] > 0
    streak = r.random((N, N)) < 0.04
    for _ in range(2):
        s = streak.copy()
        s[1:, :] |= streak[:-1, :]
        streak = s
    img[..., :3] = np.where((streak & inside)[..., None], np.minimum(1, img[..., :3] + 0.2), img[..., :3])
    return img


def mesh_shingle():
    return pm._leaf(lambda t: 0.48 * math.sqrt(max(0.0, 1 - (2 * t - 1) ** 2)), 4, 0.14, 5625)


def mesh_fern():
    """A pinnate frond: a midrib, leaflets on both sides that get shorter to the tip."""
    ys, xs = np.mgrid[0:N, 0:N]
    u = (xs + 0.5) / N - 0.5
    t = (ys + 0.5) / N
    reach = 0.46 * np.minimum(1.0, t * 1.3)
    seg = (ys % 6)
    leaflet = (np.abs(u) < reach * (1 - np.abs(seg - 2.5) / 4.0)) & (seg < 5)
    inside = leaflet | (np.abs(u) < 0.03)
    v = 0.84 - 0.12 * (np.abs(u) < 0.03) + _rng(5626).normal(0, 0.02, (N, N))
    return pm._finish(v, inside)


def mesh_lichen():
    """Reindeer lichen: a bushy tuft of forked, pale branchlets (a sprig texture)."""
    r = _rng(5627)
    a = np.zeros((N, N), bool)
    v = np.zeros((N, N))

    def walk(x, y, ang, n, depth):
        for i in range(n):
            ix, iy = int(round(x)), int(round(y))
            for dx in (0, 1):
                if 0 <= ix + dx < N and 0 <= iy < N:
                    a[iy, ix + dx] = True
                    v[iy, ix + dx] = 0.82 + 0.1 * r.random()
            x += math.cos(ang)
            y -= math.sin(ang)
            ang += r.normal(0, 0.12)
            if depth < 4 and i > 4 and r.random() < 0.12:
                walk(x, y, ang + r.choice([-1, 1]) * r.uniform(0.4, 0.8), int(n * 0.6), depth + 1)

    for k in range(4):
        walk(N / 2 + (k - 1.5) * 8, N - 1, math.pi / 2 + r.normal(0, 0.25), 44, 0)
    return pm._finish(v, a)


def mesh_velvet():
    def w(t):
        return 0.48 * math.sin(math.pi * min(1.0, t)) ** 0.5 if t > 0.02 else 0.0
    img = pm._leaf(w, 6, 0.06, 5628)
    ys, xs = np.mgrid[0:N, 0:N]
    u = (xs + 0.5) / N - 0.5
    t = (ys + 0.5) / N
    d = np.hypot(u, (t - 0.6) * 0.9)
    inside = img[..., 3] > 0
    shade = np.where(d < 0.14, 0.62, np.where(np.abs(d - 0.3) < 0.06, 0.66, 1.0))
    img[..., :3] = np.where(inside[..., None], np.clip(img[..., :3] * shade[..., None] + 0.04, 0, 1), 0)
    return img


def mesh_sundew():
    """The round pad of a sundew at the tip of its blade, fringed with tentacles."""
    ys, xs = np.mgrid[0:N, 0:N]
    u = (xs + 0.5) / N - 0.5
    t = (ys + 0.5) / N
    d = np.hypot(u, t - 0.32)
    pad = d < 0.3
    tent = (d >= 0.3) & (d < 0.44) & (np.abs(np.sin(np.arctan2(t - 0.32, u) * 14)) > 0.75)
    stalk = (np.abs(u) < 0.06) & (t > 0.5)
    inside = pad | tent | stalk
    v = np.where(tent | (pad & (d > 0.22)), 0.96, 0.76) + _rng(5629).normal(0, 0.02, (N, N))
    return pm._finish(v, inside)


def mesh_pitcher():
    """The pitcher wall, tiled along v: veins running up, a few windows (light spots) near the mouth."""
    r = _rng(5630)
    ys, xs = np.mgrid[0:N, 0:16]
    v = 0.78 - 0.14 * ((xs % 4) == 1) + 0.04 * np.sin(ys / N * 2 * math.pi * 2) + r.normal(0, 0.02, (N, 16))
    return pm._finish(v, np.ones((N, 16)))


def mesh_trap():
    """One lobe of the flytrap: round, the teeth (cilia) along its outer edge."""
    ys, xs = np.mgrid[0:N, 0:N]
    u = (xs + 0.5) / N - 0.5
    t = (ys + 0.5) / N
    lobe = (u * u) / 0.2 + (t - 0.5) ** 2 / 0.2 < 1
    cilia = (np.abs(u) > 0.36) & (np.abs(u) < 0.5) & ((ys % 6) < 2) & (t > 0.1) & (t < 0.9)
    v = np.where(np.abs(u) < 0.3, 0.70, 0.88) + _rng(5631).normal(0, 0.02, (N, N))
    return pm._finish(v, lobe | cilia)


def mesh_cactus():
    """The cactus body, tiled: ribs along v (u around), areoles with spines on the ribs."""
    ys, xs = np.mgrid[0:N, 0:16]
    rib = np.cos((xs + 0.5) / 16 * 2 * math.pi)
    v = 0.72 + 0.14 * rib + _rng(5632).normal(0, 0.015, (N, 16))
    are = (np.abs(xs - 8) <= 1) & ((ys % 8) < 2)
    v = np.where(are, 1.0, v)
    return pm._finish(v, np.ones((N, 16)))


def mesh_cactus_pad():
    """A round prickly pear pad (a disc): green with areoles in a diamond pattern and a lighter rim."""
    ys, xs = np.mgrid[0:N, 0:N]
    x = (xs + 0.5) / N * 2 - 1
    z = (ys + 0.5) / N * 2 - 1
    rr = np.hypot(x, z)
    inside = rr < 0.98
    v = 0.80 + 0.06 * (rr > 0.85) + _rng(5633).normal(0, 0.015, (N, N))
    are = ((((xs // 8) + (ys // 8)) % 2) == 0) & ((xs % 8) < 2) & ((ys % 8) < 2)
    v = np.where(are, 1.0, v)
    return pm._finish(v, inside)


def mesh_succulent():
    def w(t):
        return 0.0 if t < 0.03 else 0.46 * math.sin(math.pi * min(1.0, t * 0.9 + 0.05)) ** 0.45
    img = pm._leaf(w, 0, 0.0, 5634)
    ys, xs = np.mgrid[0:N, 0:N]
    u = (xs + 0.5) / N - 0.5
    inside = img[..., 3] > 0
    img[..., :3] = np.where(inside[..., None], np.clip(0.95 - 0.18 * np.abs(u)[..., None] * 2, 0, 1), 0)
    return img


def mesh_zebra():
    img = pm._leaf(lambda t: 0.3 * min(1.0, t * 1.05) ** 0.9, 0, 0.06, 5635)
    ys, xs = np.mgrid[0:N, 0:N]
    inside = img[..., 3] > 0
    band = ((ys % 7) < 2) & (((xs + ys) % 3) < 2)
    img[..., :3] = np.where((band & inside)[..., None], 0.98, img[..., :3] * 0.8)
    return img


def mesh_flower():
    """A small flower from above: five round petals, a light centre."""
    ys, xs = np.mgrid[0:N, 0:N]
    x = (xs + 0.5) / N * 2 - 1
    z = (ys + 0.5) / N * 2 - 1
    rr = np.hypot(x, z)
    ang = np.arctan2(z, x)
    petal = rr < 0.55 + 0.4 * np.maximum(0, np.cos(ang * 5)) ** 0.6
    v = np.where(rr < 0.22, 1.0, 0.84 - 0.1 * rr) + _rng(5636).normal(0, 0.02, (N, N))
    return pm._finish(v, petal)


# name -> (generator, mini dilation or None)
MESHES = {
    "leaf_strap": (lambda: mesh_strap(False), 2), "leaf_banded": (lambda: mesh_strap(True, 5637), 2), "leaf_spiky": (mesh_spiky, 2),
    "leaf_veined": (mesh_veined, 2), "leaf_heart": (mesh_heart, 2), "leaf_shingle": (mesh_shingle, 2), "leaf_fern": (mesh_fern, 2),
    "lichen": (mesh_lichen, 1), "leaf_velvet": (mesh_velvet, 2), "leaf_sundew": (mesh_sundew, 2), "pitcher": (mesh_pitcher, None),
    "leaf_trap": (mesh_trap, 2), "cactus": (mesh_cactus, None), "cactus_pad": (mesh_cactus_pad, 1), "leaf_succulent": (mesh_succulent, 2),
    "leaf_zebra": (mesh_zebra, 2), "flower": (mesh_flower, 1),
}


# ------------------------------------------------------------------ item icons (16x16, two layers: leaves / tops)


def _icon():
    return aq._img(), aq._img()


def _blob(img, cx, cy, rx, ry, v=0.85):
    for y in range(S):
        for x in range(S):
            if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1:
                aq._grey(img, x, y, v + 0.1 * (x < cx) - 0.08 * (y > cy))


def _line(img, x0, y0, x1, y1, v=0.8):
    n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
    for i in range(n + 1):
        t = i / max(1, n)
        aq._grey(img, int(round(x0 + (x1 - x0) * t)), int(round(y0 + (y1 - y0) * t)), v)


def _fan(img, x0, y0, angles, length, width):
    for ang in angles:
        aq._leaf_line(img, x0, y0, ang, length, width)


def icon_bromeliad(spike=False):
    leaves, top = _icon()
    _fan(leaves, 7.5, 14, [2.8, 2.4, 2.0, 1.15, 0.75, 0.35], 7, 1.2)
    _fan(leaves, 7.5, 14, [1.75, 1.4], 6, 1.1)
    if spike:
        for y in range(3, 11):
            aq._grey(top, 7, y, 0.9)
            aq._grey(top, 8, y, 0.75)
        _fan(top, 7.5, 7, [2.4, 0.75], 3, 0.8)
    else:
        _blob(top, 7.5, 11, 2.2, 1.4)
    return leaves, top


def icon_airplant():
    leaves, top = _icon()
    _line(top, 3, 14, 12, 13, 0.6)
    _fan(leaves, 7.5, 12, [3.0, 2.6, 2.2, 1.8, 1.57, 1.3, 0.9, 0.5, 0.15], 6, 0.6)
    _blob(top, 7.5, 5, 1, 1.5, 0.9)
    return leaves, top


def icon_vine():
    leaves, top = _icon()
    for x0, y0 in [(4, 3), (9, 5), (5, 8), (10, 10), (6, 13)]:
        _blob(leaves, x0, y0, 2.1, 1.7)
    _line(top, 7, 1, 7, 15, 0.7)
    return leaves, top


def icon_bush():
    leaves, top = _icon()
    for x0, y0 in [(5, 6), (10, 6), (7.5, 4), (4, 10), (11, 10), (7.5, 9)]:
        _blob(leaves, x0, y0, 2.4, 1.9)
    _line(top, 7, 12, 7, 15, 0.7)
    return leaves, top


def icon_fern(broad=False):
    leaves, top = _icon()
    if broad:
        _fan(leaves, 7.5, 14, [2.5, 2.0, 1.57, 1.15, 0.65], 10, 1.8)
    else:
        for ang in [2.7, 2.25, 1.85, 1.3, 0.9, 0.45]:
            for i in range(10):
                x = 7.5 + math.cos(ang) * i
                y = 14 - math.sin(ang) * i + (i * i) * 0.04
                aq._grey(leaves, int(round(x)), int(round(y)), 0.85)
                if i % 2 == 0:
                    aq._grey(leaves, int(round(x - math.sin(ang))), int(round(y - math.cos(ang))), 0.75)
                    aq._grey(leaves, int(round(x + math.sin(ang))), int(round(y + math.cos(ang))), 0.75)
    return leaves, top


def icon_moss(lichen=False):
    leaves, top = _icon()
    r = _rng(5640 + lichen)
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 7.5, (y - 10) * (1.0 if lichen else 1.4))
            if d < (5.6 if lichen else 6.4) - r.random() * 1.4 and y > 3:
                aq._grey(leaves, x, y, 0.62 + 0.36 * r.random())
    return leaves, top


def icon_carpet(flowers=False):
    leaves, top = _icon()
    r = _rng(5642)
    for y in range(8, 15):
        for x in range(2, 14):
            if r.random() < 0.7 and math.hypot(x - 7.5, (y - 11.5) * 1.6) < 6.5:
                aq._grey(leaves, x, y, 0.7 + 0.25 * r.random())
    if flowers:
        for x, y in [(4, 8), (9, 7), (12, 9)]:
            aq._grey(top, x, y, 0.95)
    return leaves, top


def icon_orchid():
    leaves, top = _icon()
    _fan(leaves, 7, 14, [2.6, 2.2, 0.9, 0.5], 6, 1.6)
    _line(leaves, 8, 13, 11, 5, 0.7)
    for x, y in [(11, 4), (12, 7), (9, 3)]:
        _blob(top, x, y, 1.5, 1.3, 0.92)
    return leaves, top


def icon_begonia():
    leaves, top = _icon()
    for x0, y0 in [(5, 7), (10.5, 7), (7.5, 11)]:
        _blob(leaves, x0, y0, 3.3, 2.7)
        aq._grey(top, int(x0), int(y0), 0.8)
        aq._grey(top, int(x0) + 1, int(y0), 0.7)
    return leaves, top


def icon_sundew():
    leaves, top = _icon()
    for ang in [2.8, 2.2, 1.57, 0.95, 0.35]:
        x = 7.5 + math.cos(ang) * 5
        y = 13 - math.sin(ang) * 4
        _line(leaves, 7.5, 13.5, x, y, 0.75)
        _blob(top, x, y - 0.5, 1.4, 1.4, 0.9)
    _line(leaves, 8, 13, 9, 3, 0.7)
    aq._grey(top, 9, 2, 0.95)
    return leaves, top


def icon_pitcher():
    leaves, top = _icon()
    for x0, h in [(4, 9), (7.5, 12), (11, 10)]:
        for y in range(15 - h, 15):
            w = 0.6 + (15 - y) / h * 1.2
            for x in range(int(x0 - w), int(x0 + w) + 1):
                aq._grey(leaves if y > 15 - h + 3 else top, x, y, 0.85 - 0.1 * (x > x0))
        _blob(top, x0 - 0.5, 15 - h - 0.5, 1.8, 1.0, 0.9)
    return leaves, top


def icon_nepenthes():
    leaves, top = _icon()
    _fan(leaves, 7.5, 6, [2.8, 2.2, 0.9, 0.35], 6, 1.4)
    _line(leaves, 3, 7, 3, 9, 0.7)
    _line(leaves, 12, 7, 12, 8, 0.7)
    for x0 in (3, 12):
        for y in range(9, 15):
            w = 1.6 if y > 11 else 1.0
            for x in range(int(x0 - w), int(x0 + w) + 1):
                aq._grey(top, x, y, 0.85 - 0.1 * (x > x0))
    return leaves, top


def icon_flytrap():
    leaves, top = _icon()
    for ang in [2.6, 1.57, 0.55]:
        x = 7.5 + math.cos(ang) * 4
        y = 13 - math.sin(ang) * 4
        _line(leaves, 7.5, 14, x, y, 0.8)
        cx, cy = 7.5 + math.cos(ang) * 6.3, 13 - math.sin(ang) * 6.3
        _blob(top, cx, cy, 1.9, 1.5, 0.85)
        aq._grey(leaves, int(round(cx)), int(round(cy - 2)), 0.95)
    return leaves, top


def icon_grass(needle=False):
    leaves, top = _icon()
    for x0, lean in [(3, -0.5), (5, -0.25), (6.5, -0.1), (8, 0.05), (9.5, 0.2), (11, 0.4), (12.5, 0.6)]:
        h = 12 if needle else 10
        for y in range(15 - h, 15):
            x = int(round(x0 + lean * (14 - y) * (0.3 if needle else 0.55)))
            aq._grey(leaves, x, y, 0.85)
        if needle:
            aq._grey(top, int(round(x0 + lean * 11 * 0.3)), 2, 0.9)
    return leaves, top


def icon_papyrus():
    leaves, top = _icon()
    for x0 in (5, 9):
        _line(leaves, x0, 15, x0 + (1 if x0 > 6 else -1), 6, 0.75)
    for cx in (4, 10):
        for ang in np.linspace(0.2, math.pi - 0.2, 7):
            _line(top, cx, 6, cx + math.cos(ang) * 3.5, 6 - math.sin(ang) * 3.2, 0.85)
    return leaves, top


def icon_barrel(cluster=False):
    leaves, top = _icon()
    heads = [(5, 11, 3), (10.5, 10, 3.3), (7.5, 6.5, 2.8)] if cluster else [(7.5, 9.5, 5.5)]
    for cx, cy, rr in heads:
        for y in range(S):
            for x in range(S):
                d = math.hypot(x - cx, (y - cy) * 1.15)
                if d <= rr:
                    v = 0.72 + 0.16 * math.cos((x - cx) * 1.6) - 0.08 * (y > cy)
                    aq._grey(leaves, x, y, v)
        if cluster:
            for ang in np.linspace(0, math.pi, 4):
                aq._grey(top, int(round(cx + math.cos(ang) * rr * 0.6)), int(round(cy - rr * 0.85)), 0.92)
        else:
            for x in range(int(cx - 2), int(cx + 3)):
                aq._grey(top, x, int(cy - rr * 0.95), 0.92)
    return leaves, top


def icon_column():
    leaves, top = _icon()
    for y in range(2, 15):
        for x in range(6, 10):
            aq._grey(leaves, x, y, 0.72 + 0.16 * math.cos((x - 7.5) * 1.4))
    for y in range(6, 10):
        for x in range(2, 5):
            aq._grey(leaves, x, y, 0.72 + 0.16 * math.cos((x - 3) * 1.4))
    for x in range(3, 7):
        aq._grey(leaves, x, 10, 0.75)
        aq._grey(leaves, x, 11, 0.72)
    for x in range(6, 10):
        aq._grey(top, x, 1, 0.95)
    for x in range(2, 5):
        aq._grey(top, x, 5, 0.95)
    return leaves, top


def icon_opuntia():
    leaves, top = _icon()
    for cx, cy, rx, ry in [(7.5, 11.5, 3.2, 3.6), (4.5, 5.5, 2.5, 2.9), (11, 5, 2.4, 2.8)]:
        _blob(leaves, cx, cy, rx, ry, 0.8)
    for x, y in [(4, 2), (11, 2)]:
        _blob(top, x, y, 1.2, 1.1, 0.92)
    return leaves, top


def icon_succulent(spiky=False):
    leaves, top = _icon()
    if spiky:
        _fan(leaves, 7.5, 14, [2.9, 2.5, 2.1, 1.75, 1.4, 1.05, 0.65, 0.25], 8, 1.0)
    else:
        for ring, (n, rr, w) in enumerate([(8, 5.5, 1.5), (6, 3.6, 1.3), (4, 1.8, 1.1)]):
            for i in range(n):
                ang = i * 2 * math.pi / n + ring * 0.4
                _blob(leaves, 7.5 + math.cos(ang) * rr, 10 + math.sin(ang) * rr * 0.45, w + 0.3, w, 0.85 - 0.05 * ring)
    return leaves, top


def icon_lithops():
    leaves, top = _icon()
    _blob(leaves, 5.5, 11, 2.6, 2.6, 0.82)
    _blob(leaves, 10, 11, 2.6, 2.6, 0.75)
    _blob(top, 7.7, 6.5, 2.2, 1.6, 0.92)
    return leaves, top


ICONS = {
    "bromeliad": icon_bromeliad, "bromeliad_spike": lambda: icon_bromeliad(True), "airplant": icon_airplant, "vine": icon_vine,
    "bush": icon_bush, "fern": icon_fern, "broadfern": lambda: icon_fern(True), "moss": icon_moss, "lichen": lambda: icon_moss(True),
    "carpet": icon_carpet, "carpet_flowers": lambda: icon_carpet(True), "orchid": icon_orchid, "begonia": icon_begonia,
    "sundew": icon_sundew, "pitcher": icon_pitcher, "nepenthes": icon_nepenthes, "flytrap": icon_flytrap, "grass": icon_grass,
    "rush": lambda: icon_grass(True), "papyrus": icon_papyrus, "barrel": icon_barrel, "cluster": lambda: icon_barrel(True),
    "column": icon_column, "opuntia": icon_opuntia, "succulent": icon_succulent, "spiky": lambda: icon_succulent(True),
    "lithops": icon_lithops,
}


# ------------------------------------------------------------------ the plants in the world (cross textures, cactus boxes)

LEAF, ACCENT, TOP = wp.LEAF, wp.ACCENT, wp.TOP
BX, BY = wp.BX, wp.BY
K = wp.K


def _cut(c, layer, cx, cy, rx, ry):
    """Clears a layer inside an ellipse (another layer is drawn there; equal coverage would go to the first layer)."""
    from PIL import ImageDraw
    a, g = c.layers[layer]
    ImageDraw.Draw(a).ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=0)


def cross_bromeliad():
    c = wp.Canvas(56101)
    wp._fan(c, 9, 1.35, 40, 11, "ribbon", 0.9, top=False)
    _cut(c, LEAF, BX, BY - 12, 10, 7)
    c.ellipse(TOP, BX, BY - 12, 10, 7, 0.85)
    return c


def cross_fern():
    c = wp.Canvas(56102)
    for i in range(7):
        f = (i / 6 - 0.5) * 2
        ang = f * 1.2
        end = c.leaf(LEAF, BX + f * 4, BY, ang, 52, 6, "lance", 1.0, 0.8, rib=True)
    # leaflets along the fronds: a fine comb drawn as short cross strokes
    for i in range(7):
        f = (i / 6 - 0.5) * 2
        for j in range(1, 9):
            t = j / 9
            ang = f * 1.2 + f * t * t
            x = BX + f * 4 + math.sin(ang) * 52 * t
            y = BY - math.cos(ang) * 52 * t
            c.line(LEAF, [(x - 6 * (1 - t), y + 2), (x + 6 * (1 - t), y + 2)], 3, 0.82)
    return c


def cross_broadfern():
    c = wp.Canvas(56103)
    wp._fan(c, 7, 0.9, 58, 16, "sword", 0.35, wave=0.08, top=False)
    return c


def cross_broadleaf():
    c = wp.Canvas(56104)
    wp._fan(c, 6, 1.25, 32, 22, "ovate", 0.9, stalk=0.35, top=False)
    return c


def cross_vine():
    c = wp.Canvas(56105)
    c.line(ACCENT, [(BX, BY), (BX - 6, BY - 20), (BX + 4, BY - 36), (BX - 2, BY - 50)], 3, 0.8)
    for i, (x, y) in enumerate([(BX - 10, BY - 12), (BX + 8, BY - 22), (BX - 8, BY - 32), (BX + 10, BY - 40), (BX - 4, BY - 50)]):
        c.leaf(LEAF, x, y, (-1) ** i * 1.1, 24, 20, "ovate", 0.2, 0.8)
    return c


def cross_orchid():
    c = wp.Canvas(56106)
    wp._fan(c, 4, 1.3, 26, 14, "ovate", 0.8, top=False)
    c.line(ACCENT, [(BX + 2, BY - 6), (BX + 8, BY - 30), (BX + 18, BY - 44)], 2, 0.8)
    for x, y in [(BX + 10, BY - 36), (BX + 16, BY - 44), (BX + 21, BY - 38)]:
        c.ellipse(TOP, x, y, 5, 4, 0.9)
    return c


def cross_airplant():
    c = wp.Canvas(56107)
    for i in range(13):
        f = (i / 12 - 0.5) * 2
        c.leaf(LEAF, BX, BY - 10, f * 1.9, 22 - 6 * abs(f), 4, "grass", 0.2, 0.85, rib=False)
    c.ellipse(TOP, BX, BY - 30, 3, 5, 0.85)
    c.line(ACCENT, [(BX - 24, BY - 2), (BX + 24, BY - 6)], 7, 0.65)
    return c


def cross_grass(rush=False):
    c = wp.Canvas(56108 + rush)
    n = 11 if rush else 9
    for i in range(n):
        f = (i / (n - 1) - 0.5) * 2
        ang = f * (0.3 if rush else 0.7) + c.r.uniform(-0.1, 0.1)
        end = c.leaf(LEAF, BX + f * 6, BY, ang, (60 if rush else 52) * c.r.uniform(0.75, 1.0), 3 if rush else 5, "grass", 0.0 if rush else 0.5, 0.82, rib=False)
        if rush and i % 3 == 0:
            c.ellipse(TOP, end[0], end[1] + 18, 3, 4, 0.8)
    return c


def cross_papyrus():
    c = wp.Canvas(56110)
    for x0 in (-10, 4, 14):
        top = (BX + x0 + 2, BY - 44 - abs(x0))
        c.line(ACCENT, [(BX + x0 * 0.4, BY), top], 3, 0.8)
        for k in range(9):
            ang = -1.4 + k * 0.35
            c.line(TOP, [top, (top[0] + math.sin(ang) * 14, top[1] - math.cos(ang) * 10)], 2, 0.85)
    return c


def cross_sundew():
    c = wp.Canvas(56111)
    for i in range(7):
        f = (i / 6 - 0.5) * 2
        end = c.leaf(ACCENT, BX + f * 4, BY, f * 1.35, 24, 3, "grass", 0.0, 0.8, rib=False)
        c.ellipse(TOP, end[0], end[1], 4, 4, 0.85)
    c.line(ACCENT, [(BX, BY), (BX + 3, BY - 34)], 2, 0.8)
    c.ellipse(TOP, BX + 3, BY - 36, 4, 3, 0.95)
    return c


def cross_flytrap():
    c = wp.Canvas(56112)
    for i in range(5):
        f = (i / 4 - 0.5) * 2
        end = c.leaf(LEAF, BX + f * 4, BY, f * 1.25, 22, 6, "sword", 0.0, 0.8, rib=False)
        c.leaf(TOP, end[0], end[1], f * 1.25 - 0.5, 12, 8, "round", 0.0, 0.85, rib=False)
        c.leaf(TOP, end[0], end[1], f * 1.25 + 0.5, 12, 8, "round", 0.0, 0.75, rib=False)
    return c


def cross_pitcher():
    c = wp.Canvas(56113)
    for x0, h in [(-14, 44), (0, 58), (13, 50)]:
        x = BX + x0
        c.poly(LEAF, [(x - 3, BY), (x + 3, BY), (x + 7, BY - h + 14), (x - 7, BY - h + 14)], 0.8)
        c.poly(TOP, [(x - 7, BY - h + 14), (x + 7, BY - h + 14), (x + 8, BY - h + 4), (x - 8, BY - h + 4)], 0.82)
        c.ellipse(TOP, x - 2, BY - h + 3, 8, 4, 0.88)
    return c


def cross_nepenthes():
    c = wp.Canvas(56114)
    wp._fan(c, 5, 1.2, 30, 11, "sword", 0.9, top=False)
    for x0 in (-18, 18):
        x = BX + x0
        c.line(ACCENT, [(BX + x0 * 0.5, BY - 22), (x, BY - 26), (x, BY - 18)], 2, 0.8)
        c.poly(TOP, [(x - 4, BY - 18), (x + 4, BY - 18), (x + 6, BY - 4), (x - 6, BY - 4)], 0.82)
        c.ellipse(TOP, x, BY - 4, 6, 3, 0.85)
    return c


def cross_succulent():
    c = wp.Canvas(56115)
    for ring, (n, ln, w) in enumerate([(7, 22, 11), (5, 16, 10), (3, 10, 8)]):
        for i in range(n):
            f = (i / (n - 1) - 0.5) * 2
            c.leaf(LEAF if ring < 2 else TOP, BX + f * 3, BY - ring * 4, f * (1.3 - ring * 0.2), ln, w, "ovate", 0.1, 0.85 - 0.05 * ring, rib=False)
    return c


def cross_spiky():
    c = wp.Canvas(56116)
    wp._fan(c, 9, 1.25, 50, 10, "grass", 0.35, top=False, jitter=0.08)
    return c


def cross_opuntia():
    c = wp.Canvas(56117)
    for cx, cy, rx, ry in [(BX, BY - 14, 13, 15), (BX - 14, BY - 38, 10, 12), (BX + 13, BY - 40, 10, 13)]:
        c.ellipse(LEAF, cx, cy, rx, ry, 0.8)
        for k in range(8):
            ang = k * 0.8
            c.ellipse(ACCENT, cx + math.cos(ang) * rx * 0.55, cy + math.sin(ang) * ry * 0.55, 1.4, 1.4, 0.95)
    for x, y in [(BX - 16, BY - 52), (BX + 14, BY - 55)]:
        c.ellipse(TOP, x, y, 5, 4, 0.9)
    return c


CROSSES = {
    "bromeliad": cross_bromeliad, "fern": cross_fern, "broadfern": cross_broadfern, "broadleaf": cross_broadleaf, "vine": cross_vine,
    "orchid": cross_orchid, "airplant": cross_airplant, "grass": cross_grass, "rush": lambda: cross_grass(True), "papyrus": cross_papyrus,
    "sundew": cross_sundew, "flytrap": cross_flytrap, "pitcher": cross_pitcher, "nepenthes": cross_nepenthes, "succulent": cross_succulent,
    "spiky": cross_spiky, "opuntia": cross_opuntia,
}


def world_cactus():
    """The cactus body of the world blocks (16x16, tinted: ribs and areoles) and its top (a round crown)."""
    side = block_cactus(56120)
    ys, xs = np.mgrid[0:S, 0:S]
    d = np.hypot(xs - 7.5, ys - 7.5)
    top = _grey_img(0.82 - 0.1 * (np.cos(np.arctan2(ys - 7.5, xs - 7.5) * 8) > 0.6) + 0.15 * (d < 2), np.ones((S, S)))
    spines = _grey_img(np.full((S, S), 0.92), (((xs + ys) % 3) == 0) & (d < 7.5))
    return {"wild_land_cactus": side, "wild_land_cactus_top": top, "wild_land_cactus_spines": spines}


def _cactus_box(fr, to):
    out = []
    faces = {}
    for f in ("north", "south", "east", "west"):
        if f in ("north", "south"):
            uv = [fr[0], 16 - to[1], to[0], 16 - fr[1]]
        else:
            uv = [fr[2], 16 - to[1], to[2], 16 - fr[1]]
        faces[f] = {"uv": uv, "texture": "#body", "tintindex": 0}
    faces["up"] = {"uv": [fr[0], fr[2], to[0], to[2]], "texture": "#crown", "tintindex": 0}
    out.append({"from": fr, "to": to, "faces": faces})
    # the spines or the flowers on the top
    out.append({"from": [fr[0] + 0.5, to[1] + 0.1, fr[2] + 0.5], "to": [to[0] - 0.5, to[1] + 0.1, to[2] - 0.5],
                "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#spines", "tintindex": 2},
                          "down": {"uv": [0, 0, 16, 16], "texture": "#spines", "tintindex": 2}}})
    return out


def cactus_templates():
    def model(boxes):
        els = []
        for fr, to in boxes:
            els += _cactus_box(fr, to)
        return {"ambientocclusion": True, "render_type": "minecraft:cutout", "textures": {"particle": "#body"}, "elements": els}

    def half(boxes):
        return [(fr, [to[0], fr[1] + (to[1] - fr[1]) / 2, to[2]]) for fr, to in boxes]

    barrel = [([3, 0, 3], [13, 9, 13])]
    cluster = [([2, 0, 3], [8, 6, 9]), ([8, 0, 2], [14, 7, 8]), ([6, 0, 8], [12, 5, 14])]
    column = [([6, 0, 6], [10, 16, 10]), ([1, 6, 7], [6, 8, 9]), ([1, 8, 7], [3, 13, 9])]
    out = {}
    for name, boxes in (("barrel", barrel), ("cluster", cluster), ("column", column)):
        out[f"template_wild_cactus_{name}"] = model(boxes)
        out[f"template_wild_cactus_{name}_short"] = model(half(boxes))
    return out


# ------------------------------------------------------------------ write


def write_all() -> list:
    out = []
    for name, fn in BLOCKS.items():
        out.append(save_png(fn(), BLOCK_TEX / f"plant_leaf_{name}.png"))
    for name, (gen, grow) in MESHES.items():
        img = gen()
        out.append(save_png(img, pm.OUT / f"{name}.png"))
        if grow is not None:
            out.append(save_png(pm._mini(img, grow=grow), pm.OUT / f"{name}_mini.png"))
    for name, fn in ICONS.items():
        leaves, top = fn()
        aq._outline(leaves)
        out.append(save_png(aq._finish(leaves), ITEM_TEX / f"terrarium_{name}.png"))
        out.append(save_png(aq._finish(top), ITEM_TEX / f"terrarium_{name}_detail.png"))
    for name, f in CROSSES.items():
        c = f()
        out.append(save_png(c.texture(LEAF), BLOCK_TEX / f"wild_land_{name}.png"))
        out.append(save_png(c.texture(ACCENT), BLOCK_TEX / f"wild_land_{name}_accent.png"))
        out.append(save_png(c.texture(TOP), BLOCK_TEX / f"wild_land_{name}_top.png"))
    for name, arr in world_cactus().items():
        out.append(save_png(arr, BLOCK_TEX / f"{name}.png"))
    for name, m in cactus_templates().items():
        p = wp.MODELS / f"{name}.json"
        p.write_text(json.dumps(m, indent=2) + "\n", encoding="utf-8", newline="\n")
        out.append(p)
    return out


def preview(path):
    """A sheet: every icon (tinted green + red) and every world cross, 8x."""
    from PIL import Image
    tiles = []
    for name, fn in ICONS.items():
        leaves, top = fn()
        aq._outline(leaves)
        lf, tp = aq._finish(leaves), aq._finish(top)
        rgb = np.zeros((S, S, 4))
        m = lf[..., 3] > 0
        rgb[m, :3] = lf[m, :3] * np.array([0.35, 0.7, 0.3])
        rgb[m, 3] = 1
        m = tp[..., 3] > 0
        rgb[m, :3] = tp[m, :3] * np.array([0.95, 0.35, 0.4])
        rgb[m, 3] = 1
        tiles.append(rgb)
    for name, f in CROSSES.items():
        c = f()
        rgb = np.zeros((S, S, 4))
        for layer, col in ((LEAF, (0.35, 0.65, 0.25)), (ACCENT, (0.45, 0.35, 0.2)), (TOP, (0.85, 0.3, 0.25))):
            t = c.texture(layer)
            m = t[..., 3] > 0
            rgb[m, :3] = t[m, :3] * np.array(col) / 0.8
            rgb[m, 3] = 1
        tiles.append(np.clip(rgb, 0, 1))
    cols = 12
    rows = (len(tiles) + cols - 1) // cols
    sheet = np.ones((rows * S, cols * S, 4)) * np.array([0.2, 0.18, 0.15, 1])
    for i, t in enumerate(tiles):
        y, x = divmod(i, cols)
        m = t[..., 3] > 0
        sheet[y * S:(y + 1) * S, x * S:(x + 1) * S][m] = t[m]
    Image.fromarray((sheet * 255).astype(np.uint8), "RGBA").resize((cols * S * 8, rows * S * 8), Image.NEAREST).save(path)


if __name__ == "__main__":
    import sys
    for p in write_all():
        print("wrote", p)
    if len(sys.argv) > 1:
        preview(sys.argv[1])
