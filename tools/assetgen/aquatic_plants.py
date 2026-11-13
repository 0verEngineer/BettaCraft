"""M13 assets: aquarium plants of the tank interior.

Grey block textures (``textures/block/plant_*``, 16x16, binary alpha for the cutout layer) that the game tints per
species, part and redness (block colour handler, see ``PlantColors.java``), and two-layer grey item icons per growth
form (``textures/item/plant_<form>``: layer 0 = leaves, tinted with the leaf colour; ``_detail`` = layer 1, tinted with
the stem / rhizome colour). The leaves are kept dense (few holes): a plant block is one pixel in the miniature of the
tank, and the mipmaps of a sparse cutout texture would make it vanish there.

Run from the repo root:  py tools/assetgen/aquatic_plants.py
"""
from __future__ import annotations

import math

import numpy as np

from common import BLOCK_TEX, ITEM_TEX, save_png

S = 16


def _rng(seed):
    return np.random.default_rng(seed)


def _img():
    return np.zeros((S, S, 4), np.float64)


def _grey(img, x, y, v, a=1.0):
    if 0 <= x < S and 0 <= y < S:
        v = max(0.0, min(1.0, v))
        img[y, x, :3] = v
        img[y, x, 3] = a


def _finish(img):
    out = img.copy()
    out[..., 3] = (out[..., 3] > 0.5).astype(np.float64)
    out[..., :3] *= out[..., 3:4]
    return out


# ------------------------------------------------------------------ leaf textures


def leaf_broad(seed=1301, holes=0.03, vein=True, gloss=True):
    """Solid leaf surface: soft light/dark variation, a lighter midrib and side veins, a glossy streak."""
    r = _rng(seed)
    img = _img()
    noise = r.random((S, S))
    for y in range(S):
        for x in range(S):
            v = 0.78 + 0.12 * noise[y, x] - 0.06 * abs((x - 7.5) / 8)
            if vein and x in (7, 8):
                v = 0.97
            if vein and ((x + y) % 5 == 0 and x < 7 or (y - x) % 5 == 0 and x > 8):
                v = max(v, 0.9)
            if gloss and 2 <= x <= 4 and 3 <= y <= 11 and (y + x) % 3:
                v = min(1.0, v + 0.08)
            _grey(img, x, y, v)
    for _ in range(int(holes * S * S)):
        x, y = int(r.integers(0, S)), int(r.integers(0, S))
        if x in (0, S - 1) or y in (0, S - 1):
            img[y, x, 3] = 0
    return _finish(img)


def leaf_wavy(seed=1302):
    """Bucephalandra / brown crypt: broad leaf with darker wavy bands across."""
    img = leaf_broad(seed, holes=0.04, vein=True, gloss=False)
    for y in range(S):
        for x in range(S):
            if img[y, x, 3] == 0:
                continue
            band = math.sin((y + 1.6 * math.sin(x * 0.7)) * 1.15)
            if band > 0.55:
                img[y, x, :3] *= 0.82
            elif band < -0.75:
                img[y, x, :3] = np.minimum(1.0, img[y, x, :3] * 1.08)
    return img


def leaf_frond(seed=1303):
    """Java fern: lance-shaped frond surface, dark midrib with diagonal side veins, a few ragged gaps at the edge."""
    r = _rng(seed)
    img = _img()
    for y in range(S):
        for x in range(S):
            v = 0.76 + 0.1 * r.random()
            d = abs(x - 7.5)
            if d < 1:
                v = 0.62
            elif (y + int(d)) % 4 == 0:
                v = 0.66
            elif (y + int(d)) % 4 == 2:
                v = 0.9
            _grey(img, x, y, v)
    for y in range(S):
        if r.random() < 0.35:
            img[y, 0 if r.random() < 0.5 else S - 1, 3] = 0
    return _finish(img)


def leaf_needle(seed=1304):
    """Mini Needle: narrow strap leaves side by side with small gaps."""
    r = _rng(seed)
    img = _img()
    for x in range(S):
        if x % 4 == 3 and r.random() < 0.7:
            continue
        top = int(r.integers(0, 3))
        for y in range(top, S):
            v = 0.78 + 0.14 * (1 if x % 4 == 1 else 0) + 0.05 * r.random()
            _grey(img, x, y, v)
    return _finish(img)


def leaf_fine(seed=1305):
    """Stem plants: many small round leaflets (Rotala, Ludwigia, Alternanthera)."""
    r = _rng(seed)
    img = _img()
    img[..., :3] = 0.6
    img[..., 3] = 1.0
    centres = [(2, 2), (7, 1), (12, 3), (4, 7), (10, 7), (14, 9), (1, 11), (6, 12), (11, 13), (15, 14), (8, 5), (3, 15)]
    for cx, cy in centres:
        rad = 2.3 + r.random() * 0.6
        for y in range(S):
            for x in range(S):
                dx, dy = x - cx, y - cy
                d = math.hypot(dx, dy)
                if d <= rad:
                    v = 0.95 - 0.2 * d / rad + (0.05 if dx < 0 and dy < 0 else 0)
                    _grey(img, x, y, v)
    for _ in range(18):
        x, y = int(r.integers(0, S)), int(r.integers(0, S))
        if img[y, x, 0] <= 0.62:
            img[y, x, 3] = 0
    return _finish(img)


def leaf_lotus(seed=1306):
    """Tiger lotus: broad leaf with dark tiger mottling."""
    r = _rng(seed)
    img = leaf_broad(seed, holes=0.02, vein=True, gloss=True)
    for _ in range(9):
        cx, cy, rad = r.random() * S, r.random() * S, 1.0 + r.random() * 1.6
        for y in range(S):
            for x in range(S):
                if math.hypot(x - cx, y - cy) <= rad and img[y, x, 3] > 0:
                    img[y, x, :3] *= 0.62
    return img


def moss(seed=1307):
    """Java moss: fuzzy threads, darker inside, about a fifth open."""
    r = _rng(seed)
    img = _img()
    n = r.random((S, S))
    for y in range(S):
        for x in range(S):
            if n[y, x] < 0.18:
                continue
            v = 0.62 + 0.36 * r.random()
            _grey(img, x, y, v)
    # a few lighter strands
    for _ in range(10):
        x, y = int(r.integers(0, S)), int(r.integers(0, S))
        for k in range(3):
            _grey(img, (x + k) % S, (y - k) % S, 0.98)
    return _finish(img)


def mat(seed=1308):
    """Monte Carlo: tiny round leaves packed tightly."""
    r = _rng(seed)
    img = _img()
    img[..., :3] = 0.58
    img[..., 3] = 1.0
    for cy in range(1, S, 3):
        for cx in range((cy // 3) % 2, S, 3):
            jx, jy = cx + r.random() - 0.5, cy + r.random() - 0.5
            for y in range(S):
                for x in range(S):
                    d = math.hypot(x - jx, y - jy)
                    if d <= 1.5:
                        _grey(img, x, y, 0.97 - 0.18 * d)
    for _ in range(14):
        x, y = int(r.integers(0, S)), int(r.integers(0, 4))
        img[y, x, 3] = 0
    return _finish(img)


def ribbon(seed=1309):
    """Vallisneria: two long ribbon leaves per face with a light midline."""
    r = _rng(seed)
    img = _img()
    for x0 in (1, 9):
        w = 5 + int(r.integers(0, 2))
        for y in range(S):
            off = int(round(math.sin(y * 0.45 + x0) * 0.6))
            for k in range(w):
                x = x0 + k + off
                v = 0.82 + (0.14 if k == w // 2 else 0) + 0.04 * r.random()
                _grey(img, x, y, v)
    return _finish(img)


def stem(seed=1310):
    """Stem: fibrous with darker nodes."""
    r = _rng(seed)
    img = _img()
    for y in range(S):
        for x in range(S):
            v = 0.76 + 0.1 * math.sin(x * 1.7) + 0.05 * r.random()
            if y % 8 == 0:
                v *= 0.8
            _grey(img, x, y, v)
    return _finish(img)


def rhizome(seed=1311):
    """Rhizome / bulb: rough, ringed, with fine root hairs at the bottom."""
    r = _rng(seed)
    img = _img()
    for y in range(S):
        for x in range(S):
            v = 0.66 + 0.2 * r.random()
            if (x + int(2 * math.sin(y))) % 5 == 0:
                v *= 0.8
            _grey(img, x, y, v)
    for x in range(0, S, 2):
        if r.random() < 0.6:
            _grey(img, x, S - 1, 0.95)
    return _finish(img)


def pad(seed=1312):
    """Floating lotus leaf from above: round with a notch, radial veins and mottling."""
    r = _rng(seed)
    img = _img()
    for y in range(S):
        for x in range(S):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            d = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            if d > 7.8 or (abs(ang - 0.3) < 0.18 and d > 1.2):
                continue
            v = 0.8 + 0.1 * r.random()
            if abs(math.sin(ang * 6)) < 0.12:
                v = 0.95
            if d > 7.0:
                v = 0.7
            _grey(img, x, y, v)
    for _ in range(7):
        cx, cy, rad = 3 + r.random() * 10, 3 + r.random() * 10, 0.8 + r.random() * 1.2
        for y in range(S):
            for x in range(S):
                if math.hypot(x - cx, y - cy) <= rad and img[y, x, 3] > 0:
                    img[y, x, :3] *= 0.65
    return _finish(img)


def cutting(seed=1313):
    """A cutting: a short stalk with four small leaves (drawn on the two crossing planes of the model)."""
    img = _img()
    for y in range(6, S):
        _grey(img, 7, y, 0.7)
        _grey(img, 8, y, 0.8)
    leaves = [(5, 9, -1), (10, 8, 1), (5, 4, -1), (10, 3, 1), (7, 1, 0)]
    for cx, cy, side in leaves:
        for y in range(cy - 1, cy + 3):
            for x in range(cx - 2, cx + 3):
                if (x - cx) ** 2 / 4.5 + (y - cy - 0.5) ** 2 / 2.6 <= 1.0:
                    _grey(img, x, y, 0.92 if x <= cx else 0.8)
    return _finish(img)


# ------------------------------------------------------------------ item icons


def _icon_pair():
    return _img(), _img()


def _outline(img, v=0.22):
    a = img[..., 3] > 0.5
    n = np.zeros_like(a)
    n[1:, :] |= a[:-1, :]
    n[:-1, :] |= a[1:, :]
    n[:, 1:] |= a[:, :-1]
    n[:, :-1] |= a[:, 1:]
    edge = n & ~a
    img[edge, :3] = v
    img[edge, 3] = 1.0


def _leaf_line(img, x0, y0, ang, length, width):
    for i in range(int(length * 2)):
        t = i / 2.0
        x = x0 + math.cos(ang) * t
        y = y0 - math.sin(ang) * t
        w = width * math.sin(math.pi * min(1, (t + 0.5) / length))
        for k in range(-int(round(w)), int(round(w)) + 1):
            px = int(round(x - math.sin(ang) * k))
            py = int(round(y - math.cos(ang) * k))
            _grey(img, px, py, 0.9 if k <= 0 else 0.75)


def icon_epiphyte():
    leaves, base = _icon_pair()
    for x in range(3, 13):
        _grey(base, x, 13, 0.75)
        _grey(base, x, 14, 0.6)
    for x0, ang, ln, w in [(4, 1.9, 7, 1.3), (6, 1.55, 9, 1.4), (9, 1.25, 8, 1.3), (11, 0.9, 6, 1.1), (7, 2.4, 5, 1.0)]:
        _leaf_line(leaves, x0, 12, ang, ln, w)
    _outline(leaves)
    _outline(base, 0.3)
    return leaves, base


def icon_moss():
    leaves, base = _icon_pair()
    r = _rng(1320)
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 7.5, (y - 9) * 1.3)
            if d < 6.2 - r.random() * 1.2:
                _grey(leaves, x, y, 0.6 + 0.38 * r.random())
    _outline(leaves)
    return leaves, base


def icon_rosette():
    leaves, base = _icon_pair()
    for x in range(6, 10):
        _grey(base, x, 14, 0.7)
    for ang, ln, w in [(2.6, 7, 1.2), (2.0, 9, 1.3), (1.57, 10, 1.3), (1.15, 9, 1.3), (0.55, 7, 1.2)]:
        _leaf_line(leaves, 7.5, 13.5, ang, ln, w)
    _outline(leaves)
    return leaves, base


def icon_stem():
    leaves, base = _icon_pair()
    for x0 in (6, 10):
        for y in range(2 if x0 == 6 else 4, 15):
            _grey(base, x0, y, 0.75)
        for y in range(3 if x0 == 6 else 5, 14, 2):
            _grey(leaves, x0 - 1, y, 0.9)
            _grey(leaves, x0 - 2, y, 0.8)
            _grey(leaves, x0 + 1, y + 1, 0.85)
            _grey(leaves, x0 + 2, y + 1, 0.75)
    _outline(leaves)
    _outline(base, 0.3)
    return leaves, base


def icon_ribbon():
    leaves, base = _icon_pair()
    for x0, lean in [(4, -0.3), (6, 0.1), (8, 0.25), (10, 0.5), (12, 0.2)]:
        for y in range(1 + abs(int(lean * 6)), 15):
            x = int(round(x0 + lean * (14 - y) * 0.4))
            _grey(leaves, x, y, 0.85)
    for x in range(4, 13):
        _grey(base, x, 15, 0.6)
    _outline(leaves)
    return leaves, base


def icon_carpet():
    leaves, base = _icon_pair()
    r = _rng(1321)
    for cy in range(7, 15, 2):
        for cx in range(2 + (cy % 4) // 2, 15, 2):
            if math.hypot(cx - 8, (cy - 11) * 1.6) > 7:
                continue
            _grey(leaves, cx, cy, 0.9 - 0.15 * r.random())
            _grey(leaves, cx + 1, cy, 0.75)
    _outline(leaves)
    return leaves, base


def icon_bulb():
    leaves, base = _icon_pair()
    for y in range(11, 15):
        for x in range(6, 10):
            if (x - 7.5) ** 2 + (y - 12.8) ** 2 <= 4.5:
                _grey(base, x, y, 0.85 if x < 8 else 0.65)
    for x0, y0, ang, ln, w in [(7, 11, 2.3, 7, 2.0), (8, 11, 0.8, 7, 2.0), (8, 11, 1.55, 9, 1.6)]:
        _leaf_line(leaves, x0, y0, ang, ln, w)
    _outline(leaves)
    _outline(base, 0.3)
    return leaves, base


ICONS = {
    "epiphyte": icon_epiphyte,
    "moss": icon_moss,
    "rosette": icon_rosette,
    "stem": icon_stem,
    "ribbon": icon_ribbon,
    "carpet": icon_carpet,
    "bulb": icon_bulb,
}

BLOCKS = {
    "plant_leaf_broad": leaf_broad,
    "plant_leaf_wavy": leaf_wavy,
    "plant_leaf_frond": leaf_frond,
    "plant_leaf_needle": leaf_needle,
    "plant_leaf_fine": leaf_fine,
    "plant_leaf_lotus": leaf_lotus,
    "plant_leaf_moss": moss,
    "plant_leaf_mat": mat,
    "plant_leaf_ribbon": ribbon,
    "plant_moss": moss,
    "plant_mat": mat,
    "plant_ribbon": ribbon,
    "plant_stem": stem,
    "plant_rhizome": rhizome,
    "plant_pad": pad,
    "plant_cutting": cutting,
}


def write_all():
    out = []
    for name, fn in BLOCKS.items():
        out.append(save_png(fn(), BLOCK_TEX / f"{name}.png"))
    for form, fn in ICONS.items():
        leaves, base = fn()
        out.append(save_png(_finish(leaves), ITEM_TEX / f"plant_{form}.png"))
        out.append(save_png(_finish(base), ITEM_TEX / f"plant_{form}_detail.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print(p)
