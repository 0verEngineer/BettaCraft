"""M28.1 assets: the aquarium and floating plants growing wild in the world.

Grey 16x16 cross textures per growth form and leaf shape (``textures/block/wild_<kind>_<look>``) in three disjoint
layers that the game tints per species (``WildPlantBlock``, block colour handler): the leaves (``wild_..``, tint 0), the
stalks, stems or rhizome (``_accent``, tint 1) and the young leaves or shoot tips (``_top``, tint 2). The layers never
cover the same pixel, so the coplanar faces of a layer never fight. Drawn at 4x with polygons and reduced with a
coverage vote, binary alpha for the cutout layer.

Also the coloured 32x32 patches of the wild floating plants (``textures/block/wild_floating_<type>``, composed from the
floating plant textures of M12/M28 in ``textures/entity/plants``) and the template models
(``models/block/template_wild_*.json``) the datagen gives the textures to.

Run from the repo root:  py tools/assetgen/wild_plants.py
"""
from __future__ import annotations

import json
import math

import numpy as np
from PIL import Image, ImageDraw

from common import BLOCK_TEX, RES, TEX, save_png

S = 16
K = 4  # supersampling
N = S * K
MODELS = RES / "assets" / "bettacraft" / "models" / "block"
PLANT_TEX = TEX / "entity" / "plants"

LEAF, ACCENT, TOP = 1, 2, 3


class Canvas:
    """Three high-resolution layers (coverage + grey value) for one cross texture."""

    def __init__(self, seed: int):
        self.r = np.random.default_rng(seed)
        self.layers = {k: (Image.new("L", (N, N), 0), Image.new("L", (N, N), 0)) for k in (LEAF, ACCENT, TOP)}

    def poly(self, layer, pts, grey):
        a, g = self.layers[layer]
        ImageDraw.Draw(a).polygon([(float(x), float(y)) for x, y in pts], fill=255)
        ImageDraw.Draw(g).polygon([(float(x), float(y)) for x, y in pts], fill=int(255 * grey))

    def line(self, layer, pts, width, grey):
        a, g = self.layers[layer]
        p = [(float(x), float(y)) for x, y in pts]
        ImageDraw.Draw(a).line(p, fill=255, width=int(width), joint="curve")
        ImageDraw.Draw(g).line(p, fill=int(255 * grey), width=int(width), joint="curve")

    def ellipse(self, layer, cx, cy, rx, ry, grey):
        a, g = self.layers[layer]
        box = [cx - rx, cy - ry, cx + rx, cy + ry]
        ImageDraw.Draw(a).ellipse(box, fill=255)
        ImageDraw.Draw(g).ellipse(box, fill=int(255 * grey))

    def leaf(self, layer, x0, y0, ang, length, width, profile="sword", droop=0.0, grey=0.8, rib=True, wave=0.0, steps=24):
        """A leaf from (x0, y0) along a curved midline; ``ang`` 0 = straight up, radians to the right."""
        mid, left, right = [], [], []
        for i in range(steps + 1):
            t = i / steps
            a = ang + droop * t * t * (1 if ang >= 0 else -1)
            if i == 0:
                x, y = x0, y0
            else:
                x, y = mid[-1]
                x += math.sin(a) * length / steps
                y -= math.cos(a) * length / steps
            mid.append((x, y))
            w = width * _profile(profile, t) * (1 + wave * math.sin(t * 22))
            nx, ny = math.cos(a), math.sin(a)
            left.append((x - nx * w / 2, y - ny * w / 2))
            right.append((x + nx * w / 2, y + ny * w / 2))
        self.poly(layer, left + right[::-1], grey)
        if rib and width > 5:
            self.line(layer, mid[: int(steps * 0.85)], max(1, K // 2), min(1.0, grey + 0.17))
        return mid[-1]

    def texture(self, layer) -> np.ndarray:
        """The layer at 16x16: a pixel belongs to the layer with the most coverage (at least 40 %)."""
        cov = {}
        val = {}
        for k, (a, g) in self.layers.items():
            aa = np.asarray(a, np.float64).reshape(S, K, S, K).mean(axis=(1, 3)) / 255
            gg = np.asarray(g, np.float64).reshape(S, K, S, K).sum(axis=(1, 3)) / 255
            cov[k] = aa
            val[k] = np.where(aa > 0, gg / np.maximum(aa * K * K, 1e-6), 0)
        stack = np.stack([cov[LEAF], cov[ACCENT], cov[TOP]])
        win = np.argmax(stack, axis=0) + 1
        best = stack.max(axis=0)
        out = np.zeros((S, S, 4), np.float64)
        mask = (win == layer) & (best >= 0.4)
        noise = self.r.random((S, S)) * 0.06
        out[..., :3] = np.clip(val[layer] + noise - 0.03, 0, 1)[..., None]
        out[..., 3] = mask
        out[..., :3] *= out[..., 3:4]
        return out


def _profile(p, t):
    if p == "sword":  # widest in the middle
        return math.sin(math.pi * min(1, t)) ** 0.8
    if p == "ovate":  # anubias: broad, round tip
        return math.sin(math.pi * min(1, t * 0.85 + 0.05)) ** 0.6
    if p == "lance":  # narrow, widest low
        return math.sin(math.pi * min(1, t ** 0.7)) ** 1.0
    if p == "ribbon":  # strap, rounded tip
        return 1.0 if t < 0.9 else math.sqrt(max(0, (1 - t) / 0.1))
    if p == "grass":  # tapering
        return (1 - t) ** 0.7
    if p == "round":
        return math.sqrt(max(0, 1 - (2 * t - 1) ** 2))
    return 1.0


BX, BY = N / 2, N - 1  # base point (bottom middle)


def _fan(c, n, spread, length, width, profile, droop, stalk=0.0, wave=0.0, top=True, jitter=0.15):
    """A rosette fan of ``n`` leaves on stalks (accent) from the base; the middle leaves young (top)."""
    for i in range(n):
        f = (i / (n - 1) - 0.5) * 2 if n > 1 else 0
        ang = f * spread + c.r.uniform(-jitter, jitter)
        ln = length * (1 - 0.25 * abs(f)) * c.r.uniform(0.85, 1.05)
        x, y = BX + f * 3, BY
        if stalk > 0:
            sl = ln * stalk
            ex, ey = x + math.sin(ang) * sl, y - math.cos(ang) * sl
            c.line(ACCENT, [(x, y), (ex, ey)], 3, 0.8)
            x, y = ex, ey
            ln -= sl
        young = top and abs(f) < 0.25
        c.leaf(TOP if young else LEAF, x, y, ang, ln * (0.8 if young else 1), width * (0.85 if young else 1), profile, droop, 0.82, wave=wave)


def rosette_broad():
    c = Canvas(28101)
    _fan(c, 7, 1.05, 60, 13, "sword", 0.5, stalk=0.3)
    return c


def rosette_wavy():
    c = Canvas(28102)
    _fan(c, 8, 1.1, 54, 10, "lance", 0.7, stalk=0.2, wave=0.18)
    return c


def rosette_grass():
    c = Canvas(28103)
    for i in range(11):
        f = (i / 10 - 0.5) * 2
        young = abs(f) < 0.2
        c.leaf(TOP if young else LEAF, BX + f * 10, BY, f * 0.55 + c.r.uniform(-0.12, 0.12), c.r.uniform(26, 38), 4, "grass", 0.4, 0.8, rib=False)
        c.line(ACCENT, [(BX + f * 10, BY), (BX + f * 10 + f * 1.5, BY - 4)], 3, 0.7)
    return c


def ribbon():
    c = Canvas(28104)
    for i in range(5):
        f = (i / 4 - 0.5) * 2
        young = i == 2
        x = BX + f * 22
        c.line(ACCENT, [(x, BY), (x, BY - 5)], 4, 0.7)
        c.leaf(TOP if young else LEAF, x, BY - 5, f * 0.1 + c.r.uniform(-0.05, 0.05), c.r.uniform(52, 59), 6, "ribbon", 0.2 * (1 if i % 2 else -1), 0.8,
               rib=False, wave=0.1)
    return c


def rosette_lotus():
    c = Canvas(28105)
    for i, (f, h) in enumerate([(-0.9, 30), (-0.4, 44), (0.15, 54), (0.6, 38), (1.0, 24)]):
        x2, y2 = BX + f * 20, BY - h
        c.line(ACCENT, [(BX + f * 3, BY), (BX + f * 12, BY - h * 0.6), (x2, y2)], 3, 0.75)
        c.ellipse(TOP if i == 2 else LEAF, x2, y2 - 4, 9, 6.5, 0.82)
        c.line(LEAF if i != 2 else TOP, [(x2, y2 + 2), (x2, y2 - 8)], 1, 0.95)
    return c


def epiphyte_broad():
    c = Canvas(28111)
    for i, (ang, ln) in enumerate([(-1.1, 30), (-0.55, 40), (0.0, 46), (0.5, 40), (1.05, 32)]):
        x, y = BX + ang * 6, BY - 6
        sl = ln * 0.3
        ex, ey = x + math.sin(ang) * sl, y - math.cos(ang) * sl
        c.line(ACCENT, [(x, y), (ex, ey)], 3, 0.75)
        c.leaf(TOP if i == 2 else LEAF, ex, ey, ang, ln - sl, 18, "ovate", 0.3, 0.8)
    return c


def epiphyte_wavy():
    c = Canvas(28112)
    for i in range(7):
        f = (i / 6 - 0.5) * 2
        ang = f * 1.15
        x, y = BX + f * 9, BY - 6
        sl = 7
        ex, ey = x + math.sin(ang) * sl, y - math.cos(ang) * sl
        c.line(ACCENT, [(x, y), (ex, ey)], 3, 0.75)
        c.leaf(TOP if i == 3 else LEAF, ex, ey, ang, c.r.uniform(24, 32), 11, "lance", 0.4, 0.8, wave=0.16)
    return c


def epiphyte_needle():
    c = Canvas(28113)
    for i in range(10):
        f = (i / 9 - 0.5) * 2
        ang = f * 1.0 + c.r.uniform(-0.1, 0.1)
        x, y = BX + f * 10, BY - 6
        c.line(ACCENT, [(x, y), (x + math.sin(ang) * 4, y - math.cos(ang) * 4)], 3, 0.75)
        c.leaf(TOP if abs(f) < 0.15 else LEAF, x + math.sin(ang) * 4, y - math.cos(ang) * 4, ang, c.r.uniform(20, 28), 5, "lance", 0.3, 0.8, rib=False)
    return c


def epiphyte_frond():
    c = Canvas(28114)
    for i, (ang, ln) in enumerate([(-0.75, 44), (-0.3, 56), (0.1, 60), (0.45, 52), (0.85, 40)]):
        x, y = BX + ang * 8, BY - 6
        c.line(ACCENT, [(x, y), (x + math.sin(ang) * 6, y - math.cos(ang) * 6)], 3, 0.7)
        tip = c.leaf(TOP if i == 2 else LEAF, x + math.sin(ang) * 6, y - math.cos(ang) * 6, ang, ln - 6, 11, "lance", 0.25, 0.74)
    return c


def _stem(c, x, height, leaves, kind):
    top_y = BY - height
    sway = c.r.uniform(-4, 4)
    pts = [(x, BY), (x + sway * 0.5, BY - height * 0.5), (x + sway, top_y)]
    c.line(ACCENT, pts, 3, 0.75)
    for k in range(leaves):
        t = (k + 1) / (leaves + 1)
        yy = BY - height * t
        xx = x + sway * t
        young = t > 0.84
        layer = TOP if young else LEAF
        size = 1 - 0.35 * t
        if kind == "fine":
            for s in (-1, 1):
                c.leaf(layer, xx, yy, s * 1.3, 6 * size + 2.5, 4.5 * size + 1.5, "ovate", -0.2, 0.82, rib=False)
        elif kind == "feathery":
            for s in (-1, -0.45, 0.45, 1):
                c.line(layer, [(xx, yy), (xx + s * 8 * size, yy - 3 - 3 * abs(s))], 2, 0.85)
        else:  # lobed: Hygrophila pinnatifida
            for s in (-1, 1):
                c.leaf(layer, xx, yy, s * 1.1, 15 * size + 4, 8 * size + 3, "lance", -0.15, 0.8, wave=0.35)
    if kind != "lobed":
        c.ellipse(TOP, x + sway, top_y - 1, 2.5, 3, 0.85)


def stem_fine():
    c = Canvas(28121)
    for x, h in [(BX - 18, 50), (BX + 1, 61), (BX + 19, 54)]:
        _stem(c, x, h, 6, "fine")
    return c


def stem_feathery():
    c = Canvas(28122)
    for x, h in [(BX - 18, 54), (BX + 1, 61), (BX + 18, 50)]:
        _stem(c, x, h, 7, "feathery")
    return c


def stem_lobed():
    c = Canvas(28123)
    for x, h in [(BX - 12, 46), (BX + 8, 58)]:
        _stem(c, x, h, 4, "lobed")
    return c


CROSSES = {
    "wild_epiphyte_broad": epiphyte_broad,
    "wild_epiphyte_wavy": epiphyte_wavy,
    "wild_epiphyte_needle": epiphyte_needle,
    "wild_epiphyte_frond": epiphyte_frond,
    "wild_rosette_broad": rosette_broad,
    "wild_rosette_wavy": rosette_wavy,
    "wild_rosette_grass": rosette_grass,
    "wild_rosette_ribbon": ribbon,
    "wild_rosette_lotus": rosette_lotus,
    "wild_stem_fine": stem_fine,
    "wild_stem_feathery": stem_feathery,
    "wild_stem_lobed": stem_lobed,
}


# ------------------------------------------------------------------ floating plants (coloured patches, 32x32)


def _load(name):
    return Image.open(PLANT_TEX / f"{name}.png").convert("RGBA")


def _patch(seed, pieces, n=32):
    """Pastes (image, scale, count) pieces at random places and turns onto a transparent 32x32 patch."""
    r = np.random.default_rng(seed)
    out = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    for img, scale, count, spread in pieces:
        for _ in range(count):
            w = max(2, int(round(img.width * scale)))
            h = max(2, int(round(img.height * scale)))
            piece = img.resize((w, h), Image.NEAREST).rotate(float(r.uniform(0, 360)), Image.NEAREST, expand=True)
            rad = (n / 2 - max(piece.width, piece.height) / 2) * spread
            a = r.uniform(0, math.tau)
            d = rad * math.sqrt(r.random())
            x = int(n / 2 + d * math.cos(a) - piece.width / 2)
            y = int(n / 2 + d * math.sin(a) - piece.height / 2)
            out.alpha_composite(piece, (max(0, min(n - piece.width, x)), max(0, min(n - piece.height, y))))
    arr = np.asarray(out).copy()
    arr[..., 3] = np.where(arr[..., 3] > 127, 255, 0)
    return arr


def _tint(img, rgb):
    """A grey floating plant texture (tinted in the tank by the renderer) in a fixed colour."""
    arr = np.asarray(img).astype(np.float64)
    arr[..., :3] = arr[..., :3] * np.array(rgb, np.float64) / 255.0
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


def floating():
    duck = _load("duckweed")
    frog = _load("frogbit_leaf")
    salv = _load("salvinia")
    # the red root floater in daylight: half way from green to red (FloatingPlantSim.RRF_GREEN / RRF_RED)
    rrf = _tint(_load("red_root_floater_leaf"), (0xA7, 0x85, 0x4C))
    lettuce = _load("water_lettuce_leaf")
    horn = _tint(_load("hornwort"), (0x4E, 0x8A, 0x34))  # FloatingPlantRenderer.HORNWORT_TINT
    ricc = _load("riccia")
    return {
        "wild_floating_duckweed": _patch(28141, [(duck, 0.55, 5, 1.0)]),
        "wild_floating_amazon_frogbit": _patch(28142, [(frog, 0.38, 6, 1.0)]),
        "wild_floating_salvinia": _patch(28143, [(salv, 0.5, 5, 1.0)]),
        "wild_floating_red_root_floater": _patch(28144, [(rrf, 0.6, 7, 1.0)]),
        "wild_floating_water_lettuce": _patch(28145, [(lettuce, 0.75, 6, 0.55), (lettuce, 0.6, 3, 1.0)]),
        "wild_floating_hornwort": _patch(28146, [(horn, 1.0, 4, 1.0)]),
        "wild_floating_riccia_fluitans": _patch(28147, [(ricc, 1.0, 4, 1.0)]),
    }


# ------------------------------------------------------------------ template models


def _cross_layers(lo, hi, h, y0=0.0, shade=False):
    """Two crossed planes from ``lo`` to ``hi`` (x) and ``y0`` to ``y0 + h``, each with the three layers."""
    els = []
    for angle in (45, -45):
        for tex, tint in (("#leaf", 0), ("#accent", 1), ("#top", 2)):
            face = {"uv": [0, 0, 16, 16], "texture": tex, "tintindex": tint}
            els.append({"from": [lo, y0, 8], "to": [hi, y0 + h, 8],
                        "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": angle, "rescale": True},
                        "shade": shade, "faces": {"north": dict(face), "south": dict(face)}})
    return els


def _box(fr, to, tex, tint, faces=("north", "south", "east", "west", "up", "down")):
    out = {}
    for f in faces:
        if f in ("up", "down"):
            uv = [fr[0], fr[2], to[0], to[2]]
        elif f in ("north", "south"):
            uv = [fr[0], 16 - to[1], to[0], 16 - fr[1]]
        else:
            uv = [fr[2], 16 - to[1], to[2], 16 - fr[1]]
        out[f] = {"uv": uv, "texture": tex, "tintindex": tint}
        if f == "down" and fr[1] == 0:
            out[f]["cullface"] = "down"
    return {"from": fr, "to": to, "faces": out}


def _model(elements, particle="#leaf", ao=False):
    return {"ambientocclusion": ao, "render_type": "minecraft:cutout",
            "textures": {"particle": particle}, "elements": elements}


def templates():
    rhizome = _box([3, 0, 6.5], [13, 2, 9.5], "#rhizome", 1)
    return {
        "template_wild_plant": _model(_cross_layers(0.8, 15.2, 16)),
        "template_wild_plant_short": _model(_cross_layers(4.4, 11.6, 8)),
        "template_wild_epiphyte": _model([rhizome] + _cross_layers(1.6, 14.4, 13, 0.5)),
        "template_wild_epiphyte_short": _model([rhizome] + _cross_layers(4.8, 11.2, 6.5, 0.5)),
        "template_wild_floating": {"ambientocclusion": False, "render_type": "minecraft:cutout", "textures": {"particle": "#tex"},
                                   "elements": [{"from": [0, 0.25, 0], "to": [16, 0.25, 16],
                                                 "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#tex"},
                                                           "down": {"uv": [0, 16, 16, 0], "texture": "#tex"}}}]},
    }


def _rgba(grey: np.ndarray, alpha: np.ndarray) -> np.ndarray:
    out = np.zeros(grey.shape + (4,))
    out[..., :3] = grey[..., None]
    out[..., 3] = alpha
    return out



# ------------------------------------------------------------------ M55: the wild carpets (three designs)

CARPET_LOOKS = ("mat", "clover", "needle", "fine")
# M55: the design the wild carpets use. All three templates are written (``template_wild_carpet_<design>``); the shared
# ``template_wild_carpet`` only names one of them as its parent, so switching is this one line (then run this script):
#   "lawn"     a dense lawn: a closed ground of leaves, loose leaf layers over it and a few sprigs,
#   "cushions" loose round cushions (stacked leafy discs) joined by runners over open ground,
#   "blades"   fine sprigs standing apart, the ground showing between them.
CARPET_DESIGNS = ("lawn", "cushions", "blades")
CARPET_DESIGN = __import__("os").environ.get("BETTACRAFT_CARPET_DESIGN", "blades")


def _stamp(g, a, x, y, pix, v, r):
    """Pixels ``pix`` (dx, dy, shade) of one leaf at (x, y), wrapped round the tile; grey ``v`` times the shade."""
    for dx, dy, sh in pix:
        xx, yy = (x + dx) % S, (y + dy) % S
        g[yy, xx] = np.clip(v * sh + r.uniform(-0.03, 0.03), 0, 1)
        a[yy, xx] = 1


# top views of one leaf (dx, dy, shade) per look, light from the upper left; shade 0 = a gap (the clover's notches)
_LEAVES = {
    "mat": [[(0, 0, 1.0), (1, 0, 0.92), (0, 1, 0.9), (1, 1, 0.8)], [(0, 0, 1.0), (1, 0, 0.88)], [(0, 0, 0.95), (0, 1, 0.82)]],
    "clover": [[(0, 0, 1.0), (1, 0, 0.92), (0, 1, 0.9), (1, 1, 0.55), (2, 1, 0.9), (1, 2, 0.85), (2, 2, 0.8), (2, 0, 0.0), (0, 2, 0.0)]],
    "fine": [[(0, 0, 1.0), (1, 0, 0.9), (2, 1, 0.95), (3, 1, 0.85)], [(0, 1, 0.95), (1, 1, 0.88), (2, 0, 1.0), (3, 0, 0.9)],
             [(0, 0, 1.0), (0, 1, 0.9), (1, 2, 0.95), (1, 3, 0.85)]],
    "needle": [[(0, 0, 1.0)], [(0, 0, 1.0), (1, 1, 0.85)], [(1, 0, 0.95), (0, 1, 0.85)]],
}


def _leaf_layer(look: str, seed: int, cover: float, base=0.82) -> tuple:
    """Grey and alpha of a tile of loose leaves of the look seen from above (tileable), about ``cover`` of it covered."""
    r = np.random.default_rng(seed)
    g = np.zeros((S, S))
    a = np.zeros((S, S))
    tries = 0
    while a.mean() < cover and tries < 4000:
        tries += 1
        x, y = int(r.integers(0, S)), int(r.integers(0, S))
        pix = _LEAVES[look][int(r.integers(0, len(_LEAVES[look])))]
        v = base + r.uniform(-0.1, 0.18)
        _stamp(g, a, x, y, [p for p in pix if p[2] > 0], v, r)
        for dx, dy, sh in pix:
            if sh == 0:
                a[(y + dy) % S, (x + dx) % S] = 0
    return g, a


def lawn_ground(look, seed):
    """Closed ground of the lawn: dense dark leaves, every texel covered (darker between the leaves)."""
    g, a = _leaf_layer(look, seed, 0.75, base=0.7)
    g = np.where(a > 0, g, 0.48 + np.random.default_rng(seed + 1).random((S, S)) * 0.06)
    return _rgba(g, np.ones((S, S)))


def lawn_layer(look, seed, cover):
    g, a = _leaf_layer(look, seed, cover, base=0.86)
    return _rgba(g, a)


def _disc_mask(d, r):
    """A round mask of diameter ``d`` with a ragged edge."""
    m = np.zeros((d, d))
    c = (d - 1) / 2
    for y in range(d):
        for x in range(d):
            q = math.hypot(x - c, y - c) / (d / 2)
            m[y, x] = 1 if q < 0.78 or (q < 1.02 and r.random() < 0.55) else 0
    return m


# the discs of the cushion texture: diameter -> (u, v); a cushion of diameter d stacks d, d - 2, d - 4 ...
DOME_DISCS = {9: (0, 0), 7: (9, 0), 5: (9, 7), 3: (0, 9)}


def dome_texture(look, seed):
    """Leafy discs of diameter 9, 7, 5 and 3 (an atlas, 1:1 texels in the model) with ragged rims, the leaves of the look."""
    r = np.random.default_rng(seed)
    g, a = _leaf_layer(look, seed + 1, 0.9, base=0.88)
    g = np.where(a > 0, g, 0.62)
    out_a = np.zeros((S, S))
    for d, (u, v) in DOME_DISCS.items():
        out_a[v:v + d, u:u + d] = np.maximum(out_a[v:v + d, u:u + d], _disc_mask(d, r))
    return _rgba(np.clip(g, 0, 1), out_a)


# cushions: (centre x, centre z, diameter); runners between them and out to the block edges
CUSHIONS = [(4.5, 4.5, 9), (11.5, 10.5, 9), (12.5, 3.5, 7), (3.5, 12.5, 7)]
CUSHION_RUNNERS = [((4, 4), (12, 3)), ((4, 4), (4, 12)), ((12, 3), (11, 10)), ((4, 12), (11, 10)), ((11, 10), (15, 15)),
                   ((12, 3), (15, 0)), ((4, 12), (0, 15)), ((4, 4), (0, 1)), ((11, 10), (15, 8)), ((4, 12), (7, 15))]


def runner_texture(look, seed):
    """The runners between the cushions over open ground: thin dark stems with a leaf of the look here and there."""
    r = np.random.default_rng(seed)
    g = np.zeros((S, S))
    a = np.zeros((S, S))
    for (x0, y0), (x1, y1) in CUSHION_RUNNERS:
        n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
        for i in range(n):
            t = i / max(1, n - 1)
            x = int(round(x0 + (x1 - x0) * t + math.sin(t * 6 + seed) * 0.6))
            y = int(round(y0 + (y1 - y0) * t + math.cos(t * 5 + seed) * 0.6))
            if 0 <= x < S and 0 <= y < S:
                g[y, x], a[y, x] = 0.62, 1
                if i % 3 == 1:
                    _stamp(g, a, x, y, [p for p in _LEAVES[look][0] if p[2] > 0], 0.86, r)
    return _rgba(g, a)


def blade_ground(look, seed):
    """Loose leaves at the feet of the sprigs, the ground showing between them (about 40 % covered)."""
    g, a = _leaf_layer(look, seed, 0.4, base=0.72)
    return _rgba(g, a)


# the sprigs: four columns (4 texels wide) in two bands, the lower band (rows 8..15) for the grown carpet, the upper band
# (rows 0..7) for the trimmed one; each sprig stands on the bottom row of its band
SPRIG_HEIGHT = {"mat": (4, 2), "clover": (5, 3), "fine": (6, 3), "needle": (8, 4)}


def sprig_texture(look, seed):
    r = np.random.default_rng(seed)
    g = np.zeros((S, S))
    a = np.zeros((S, S))

    def px(x, y, v):
        if 0 <= x < S and 0 <= y < S:
            g[y, x], a[y, x] = np.clip(v + r.uniform(-0.03, 0.03), 0, 1), 1

    for bottom, full in ((15, SPRIG_HEIGHT[look][0]), (7, SPRIG_HEIGHT[look][1])):
        for col in range(4):
            x0 = col * 4
            h = max(2, full - int(r.integers(0, 2)))
            top = bottom - h + 1
            if look == "needle":  # Eleocharis: three or four thin needles fanning out
                for k, (bx, lean) in enumerate(((1, -1), (2, 1), (1, 0), (2, 0))[: 3 + col % 2]):
                    hh = h - (k % 2)
                    for i in range(hh):
                        x = x0 + bx + (lean if i > hh * 0.6 else 0)
                        px(x, bottom - i, 0.72 + 0.25 * i / hh)
            elif look == "clover":  # Marsilea: a stalk with a four-leafed head
                sx = x0 + 1 + col % 2
                for i in range(h - 2):
                    px(sx, bottom - i, 0.6)
                for dx, dy, sh in ((-1, 0, 0.95), (1, 0, 0.95), (0, -1, 1.0), (0, 0, 0.7), (2, 0, 0.85), (-1, 1, 0.8), (1, 1, 0.8)):
                    if x0 <= sx + dx < x0 + 4:
                        px(sx + dx, top + 1 + dy, 0.9 * sh)
            elif look == "fine":  # Staurogyne: a short stem with pairs of oval leaves
                sx = x0 + 1 + col % 2
                for i in range(h):
                    px(sx, bottom - i, 0.62)
                for i in range(1, h, 2):
                    y = bottom - i
                    px(sx - 1, y, 0.9)
                    px(sx + 1, y - 1 if i > 1 else y, 0.95)
                    if sx + 2 < x0 + 4:
                        px(sx + 2, y - 1, 0.85)
                px(sx, top, 1.0)
            else:  # mat (Hemianthus, Monte Carlo): a tiny clump of round leaves
                for i in range(h):
                    y = bottom - i
                    for x in range(x0 + (i % 2), x0 + 4 - (1 - i % 2)):
                        if r.random() < 0.75:
                            px(x, y, 0.78 + 0.18 * i / h)
    return _rgba(g, a)


def carpet_textures() -> dict:
    out = {}
    for i, look in enumerate(CARPET_LOOKS):
        out[f"wild_carpet_{look}_lawn_ground"] = lawn_ground(look, 55100 + i)
        out[f"wild_carpet_{look}_lawn_low"] = lawn_layer(look, 55110 + i, 0.62)
        out[f"wild_carpet_{look}_lawn_high"] = lawn_layer(look, 55120 + i, 0.3)
        out[f"wild_carpet_{look}_dome"] = dome_texture(look, 55130 + i)
        out[f"wild_carpet_{look}_runners"] = runner_texture(look, 55140 + i)
        out[f"wild_carpet_{look}_blade_ground"] = blade_ground(look, 55150 + i)
        out[f"wild_carpet_{look}_sprig"] = sprig_texture(look, 55160 + i)
    return out


# the texture variables the carpet templates use (the datagen gives all of them to every carpet model)
CARPET_VARIABLES = ("lawn_ground", "lawn_low", "lawn_high", "dome", "runners", "blade_ground", "sprig")


def _plane(y, tex, tint, fr=(0, 0), to=(16, 16), uv=(0, 0, 16, 16), rot=0):
    """A horizontal leaf layer at height ``y``, seen from above."""
    face = {"uv": list(uv), "texture": tex, "tintindex": tint}
    if rot:
        face["rotation"] = rot
    return {"from": [fr[0], y, fr[1]], "to": [to[0], y, to[1]], "shade": False, "faces": {"up": face}}


def _sprig(x, z, h, col, band, angle, tint=0):
    """A sprig: two crossed planes 4 texels wide and ``h`` high at (x, z), column ``col`` of the sprig texture, turned."""
    v1 = 16 if band == 0 else 8
    uv = [col * 4, v1 - h, col * 4 + 4, v1]
    els = []
    for along_x in (True, False):
        face = {"uv": uv, "texture": "#sprig", "tintindex": tint}
        fr = [x - 2, 0, z] if along_x else [x, 0, z - 2]
        to = [x + 2, h, z] if along_x else [x, h, z + 2]
        faces = {"north": dict(face), "south": dict(face)} if along_x else {"east": dict(face), "west": dict(face)}
        els.append({"from": [round(c, 3) for c in fr], "to": [round(c, 3) for c in to],
                    "rotation": {"origin": [x, 0, z], "axis": "y", "angle": angle, "rescale": False},
                    "shade": False, "faces": faces})
    return els


_ANGLES = (22.5, -22.5, 45, 0, -45)


def _cushion_discs(cx, cz, d, heights):
    els = []
    for k, y in enumerate(heights):
        dd = d - 2 * k
        if dd < 3:
            break
        u, v = DOME_DISCS[dd]
        h = dd / 2
        els.append(_plane(y, "#dome", 2 if k == len(heights) - 1 or dd == 3 else 0, (cx - h, cz - h), (cx + h, cz + h), (u, v, u + dd, v + dd)))
    return els


def carpet_template(trimmed: bool, design: str) -> dict:
    els = []
    if design == "lawn":
        els.append(_plane(0.2, "#lawn_ground", 0))
        els.append(_plane(0.9 if not trimmed else 0.6, "#lawn_low", 0, rot=90))
        if not trimmed:
            els.append(_plane(1.6, "#lawn_high", 2, rot=180))
            els.append(_plane(2.2, "#lawn_high", 0, rot=270))
        spots = [(3, 3), (10, 5), (6, 11), (13, 13), (8, 8), (14, 2)]
        for i, (x, z) in enumerate(spots[: 6 if not trimmed else 3]):
            h = (4, 3, 4)[i % 3] if not trimmed else 2
            els += _sprig(x, z, h, i % 4, 0 if not trimmed else 1, _ANGLES[i % 5], 2 if i % 2 else 0)
    elif design == "cushions":
        els.append(_plane(0.15, "#runners", 1))
        heights = (0.5, 1.3, 2.0, 2.6) if not trimmed else (0.4, 0.9)
        for cx, cz, d in CUSHIONS:
            els += _cushion_discs(cx, cz, d if not trimmed else d - 2, heights)
        for i, (x, z) in enumerate([(4.5, 4.5), (11.5, 10.5), (8, 8), (12.5, 3.5)][: 4 if not trimmed else 2]):
            els += _sprig(x, z, 3 if not trimmed else 2, i, 0 if not trimmed else 1, _ANGLES[i], 2 if i % 2 else 0)
    else:  # blades
        els.append(_plane(0.2, "#blade_ground", 0))
        r = np.random.default_rng(5501)
        n = 0
        for gz in range(4):
            for gx in range(4):
                if trimmed and (gx + gz) % 2:
                    continue
                x = round(2 + gx * 4 + r.uniform(-1.2, 1.2), 2)
                z = round(2 + gz * 4 + r.uniform(-1.2, 1.2), 2)
                h = 8 - int(r.integers(0, 3)) if not trimmed else 4 - int(r.integers(0, 2))
                els += _sprig(x, z, h, (gx + 2 * gz) % 4, 0 if not trimmed else 1, _ANGLES[n % 5], 2 if n % 3 == 0 else 0)
                n += 1
    particle = {"lawn": "#lawn_low", "cushions": "#dome", "blades": "#sprig"}[design]
    return {"ambientocclusion": False, "render_type": "minecraft:cutout", "textures": {"particle": particle}, "elements": els}


def carpet_templates() -> dict:
    out = {}
    for d in CARPET_DESIGNS:
        out[f"template_wild_carpet_{d}"] = carpet_template(False, d)
        out[f"template_wild_carpet_{d}_short"] = carpet_template(True, d)
    # the shared templates the carpet models name: only a parent (the design in use)
    out["template_wild_carpet"] = {"parent": f"bettacraft:block/template_wild_carpet_{CARPET_DESIGN}"}
    out["template_wild_carpet_short"] = {"parent": f"bettacraft:block/template_wild_carpet_{CARPET_DESIGN}_short"}
    return out


# ------------------------------------------------------------------ M55: round moss cushions and Marimo balls (B1)

# the discs of the moss cushions, 1:1 texels: texture -> {diameter: (u, v)}
MOSS_DISCS = {"wild_moss_disc": {13: (0, 0), 3: (13, 0)}, "wild_moss_disc_small": {10: (0, 0), 6: (10, 0), 4: (10, 6)}}
# the Marimo: discs of diameter 6, 5 and 4 and 3 (balls of three sizes, each from crossed discs)
MARIMO_DISCS = {6: (0, 0), 5: (6, 0), 4: (11, 0), 3: (0, 6)}


def _fuzzy_disc(d, r, rim=0.55):
    m = np.zeros((d, d))
    c = (d - 1) / 2
    for y in range(d):
        for x in range(d):
            q = math.hypot(x - c, y - c) / (d / 2)
            m[y, x] = 1 if q < 0.72 or (q < 1.05 and r.random() < rim) else 0
    return m


def _moss_grey(seed):
    """The interior moss texture (fine fronds) as grey 0..1, its holes filled darker."""
    moss = np.asarray(Image.open(BLOCK_TEX / "plant_moss.png").convert("RGBA")).astype(np.float64) / 255
    g = moss[..., :3].mean(axis=2)
    g = np.where(moss[..., 3] > 0, g, g.mean() * 0.62)
    r = np.random.default_rng(seed)
    return np.clip(g + r.uniform(-0.04, 0.04, g.shape), 0, 1)


def moss_disc_textures():
    out = {}
    for k, (name, discs) in enumerate(MOSS_DISCS.items()):
        r = np.random.default_rng(55300 + k)
        g = _moss_grey(55310 + k)
        a = np.zeros((S, S))
        for d, (u, v) in discs.items():
            a[v:v + d, u:u + d] = np.maximum(a[v:v + d, u:u + d], _fuzzy_disc(d, r))
        out[name] = _rgba(g, a)
    return out


def marimo_texture():
    """Discs of the Marimo ball: fine green filaments, lighter in the middle, darker to the fuzzy rim (a ball's shading)."""
    r = np.random.default_rng(55320)
    g = np.zeros((S, S))
    a = np.zeros((S, S))
    base = _moss_grey(55321)
    for d, (u, v) in MARIMO_DISCS.items():
        m = _fuzzy_disc(d, r, rim=0.7)
        c = (d - 1) / 2
        for y in range(d):
            for x in range(d):
                q = math.hypot(x - c + 0.4, y - c + 0.4) / (d / 2)
                g[v + y, u + x] = np.clip(base[v + y, u + x] * (1.12 - 0.35 * q * q), 0, 1)
        a[v:v + d, u:u + d] = np.maximum(a[v:v + d, u:u + d], m)
    return _rgba(g, a)


def _disc(cx, y, cz, d, uv, tex, tint=0, both=False):
    u, v = uv
    face = {"uv": [u, v, u + d, v + d], "texture": tex, "tintindex": tint}
    faces = {"up": dict(face)}
    if both:
        faces["down"] = dict(face)
    h = d / 2
    return {"from": [cx - h, y, cz - h], "to": [cx + h, y, cz + h], "shade": False, "faces": faces}


def _upright(cx, y0, cz, d, uv, tex, angle, along_x=True):
    """A round disc standing upright (both sides) through (cx, cz), turned by ``angle``."""
    u, v = uv
    face = {"uv": [u, v, u + d, v + d], "texture": tex, "tintindex": 0}
    h = d / 2
    if along_x:
        fr, to, faces = [cx - h, y0, cz], [cx + h, y0 + d, cz], {"north": dict(face), "south": dict(face)}
    else:
        fr, to, faces = [cx, y0, cz - h], [cx, y0 + d, cz + h], {"east": dict(face), "west": dict(face)}
    return {"from": fr, "to": to, "shade": False, "faces": faces,
            "rotation": {"origin": [cx, y0, cz], "axis": "y", "angle": angle, "rescale": False}}


def _cushion_stack(cx, cz, sizes, heights):
    """A moss cushion: discs of the given diameters stacked at the given heights (the top one lighter: tint 0 still)."""
    els = []
    for d, y in zip(sizes, heights):
        tex = "#moss_disc" if d in MOSS_DISCS["wild_moss_disc"] else "#moss_disc_small"
        uv = MOSS_DISCS["wild_" + tex[1:]][d]
        els.append(_disc(cx, y, cz, d, uv, tex))
    return els


def _ball(cx, cz, d, lift=0.0):
    """A Marimo ball of diameter ``d``: two upright discs crossed, two diagonal, a horizontal one at its middle."""
    uv = MARIMO_DISCS[d]
    els = [_upright(cx, lift, cz, d, uv, "#marimo", 0, True), _upright(cx, lift, cz, d, uv, "#marimo", 0, False),
           _upright(cx, lift, cz, d, uv, "#marimo", 45, True), _upright(cx, lift, cz, d, uv, "#marimo", 45, False),
           _disc(cx, lift + d / 2, cz, d, uv, "#marimo", both=True)]
    if d - 2 in MARIMO_DISCS:  # a smaller disc near the top rounds it seen from above
        els.append(_disc(cx, lift + d * 0.85, cz, d - 2, MARIMO_DISCS[d - 2], "#marimo"))
    return els


def moss_templates():
    m = lambda els, particle: {"ambientocclusion": False, "render_type": "minecraft:cutout", "textures": {"particle": particle}, "elements": els}
    moss = _cushion_stack(7.0, 8.5, (13, 10, 6, 3), (0.4, 1.3, 2.2, 2.9)) + _cushion_stack(12.5, 3.5, (6, 4), (0.6, 1.5)) \
        + _cushion_stack(3.0, 13.0, (4,), (0.5,))
    moss_short = _cushion_stack(8.0, 8.0, (10, 6), (0.3, 0.9))
    marimo = _ball(5.0, 6.0, 6) + _ball(11.5, 10.5, 5) + _ball(10.0, 4.0, 4)
    marimo_short = _ball(7.5, 7.5, 5)
    return {"template_wild_moss": m(moss, "#moss_disc"), "template_wild_moss_short": m(moss_short, "#moss_disc"),
            "template_wild_marimo": m(marimo, "#marimo"), "template_wild_marimo_short": m(marimo_short, "#marimo")}


def write_all() -> list:
    out = []
    for name, f in CROSSES.items():
        c = f()
        out.append(save_png(c.texture(LEAF), BLOCK_TEX / f"{name}.png"))
        out.append(save_png(c.texture(ACCENT), BLOCK_TEX / f"{name}_accent.png"))
        out.append(save_png(c.texture(TOP), BLOCK_TEX / f"{name}_top.png"))
    # M55: the moss cushions (stacked round discs) and the Marimo balls (crossed round discs) instead of boxes
    for name, arr in moss_disc_textures().items():
        out.append(save_png(arr, BLOCK_TEX / f"{name}.png"))
    out.append(save_png(marimo_texture(), BLOCK_TEX / "wild_marimo.png"))
    for name, arr in floating().items():
        out.append(save_png(arr, BLOCK_TEX / f"{name}.png"))
    for name, arr in carpet_textures().items():  # M55
        out.append(save_png(arr, BLOCK_TEX / f"{name}.png"))
    tmpl = dict(templates())
    tmpl.update(carpet_templates())  # M55: three designs, the shared template names the one in use
    tmpl.update(moss_templates())  # M55: round cushions and balls
    for name, m in tmpl.items():
        p = MODELS / f"{name}.json"
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(json.dumps(m, indent=2) + "\n", encoding="utf-8", newline="\n")
        out.append(p)
    return out


def preview(path):
    """A sheet of every cross (layers tinted green / brown / red) at 8x for a quick look."""
    tiles = []
    for name, f in CROSSES.items():
        c = f()
        rgb = np.zeros((S, S, 4))
        for layer, col in ((LEAF, (0.35, 0.65, 0.25)), (ACCENT, (0.45, 0.35, 0.2)), (TOP, (0.85, 0.3, 0.25))):
            t = c.texture(layer)
            m = t[..., 3] > 0
            rgb[m, :3] = t[m, :3] * np.array(col) / 0.8
            rgb[m, 3] = 1
        tiles.append(np.clip(rgb, 0, 1))
    sheet = np.ones((S, S * len(tiles), 4)) * np.array([0.15, 0.3, 0.45, 1])
    for i, t in enumerate(tiles):
        m = t[..., 3] > 0
        sheet[:, i * S:(i + 1) * S][m] = t[m]
    img = Image.fromarray((sheet * 255).astype(np.uint8), "RGBA").resize((S * len(tiles) * 8, S * 8), Image.NEAREST)
    img.save(path)


if __name__ == "__main__":
    import sys
    for p in write_all():
        print("wrote", p)
    if len(sys.argv) > 1:
        preview(sys.argv[1])
