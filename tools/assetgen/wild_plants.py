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


def carpet_mat():
    c = Canvas(28131)
    for i in range(22):
        x = c.r.uniform(3, N - 3)
        h = c.r.uniform(10, 34)
        c.line(ACCENT, [(x, BY), (x + c.r.uniform(-3, 3), BY - h)], 2, 0.7)
        c.ellipse(TOP if c.r.random() < 0.3 else LEAF, x, BY - h - 2, 5, 3.5, 0.85)
    return c


def carpet_clover():
    c = Canvas(28132)
    for i in range(12):
        x = c.r.uniform(5, N - 5)
        h = c.r.uniform(18, 44)
        c.line(ACCENT, [(x, BY), (x + c.r.uniform(-2, 2), BY - h)], 2, 0.72)
        layer = TOP if i % 4 == 0 else LEAF
        for dx, dy in ((-3.5, 0), (3.5, 0), (0, -2.5), (0, 2.0)):
            c.ellipse(layer, x + dx, BY - h + dy, 3.6, 2.6, 0.84)
    return c


def carpet_needle():
    c = Canvas(28133)
    for i in range(30):
        x = c.r.uniform(2, N - 2)
        h = c.r.uniform(30, 60)
        ang = c.r.uniform(-0.25, 0.25)
        layer = TOP if c.r.random() < 0.25 else LEAF
        c.line(layer, [(x, BY), (x + math.sin(ang) * h, BY - h)], 2, c.r.uniform(0.75, 0.9))
        c.line(ACCENT, [(x, BY), (x + math.sin(ang) * 5, BY - 5)], 2, 0.6)
    return c


def carpet_fine():
    c = Canvas(28134)
    for x, h in [(10, 34), (24, 44), (38, 38), (52, 30)]:
        _stem(c, x, h, 4, "fine")
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
    "wild_carpet_mat": carpet_mat,
    "wild_carpet_clover": carpet_clover,
    "wild_carpet_needle": carpet_needle,
    "wild_carpet_fine": carpet_fine,
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
        "template_wild_carpet": _model([_box([0, 0, 0], [16, 0.6, 16], "#mat", 0, ("up",))] + _cross_layers(0.8, 15.2, 7)),
        "template_wild_carpet_short": _model([_box([0, 0, 0], [16, 0.6, 16], "#mat", 0, ("up",))] + _cross_layers(3.5, 12.5, 3)),
        "template_wild_moss": _model([_box([1, 0, 1], [15, 2.5, 15], "#moss", 0), _box([3, 2.5, 4], [12, 4, 12], "#moss", 0),
                                      _box([9, 2.5, 2], [14, 3.5, 7], "#moss", 0)], "#moss"),
        "template_wild_moss_short": _model([_box([2, 0, 2], [14, 1, 14], "#moss", 0)], "#moss"),
        "template_wild_marimo": _model([_box([2, 0, 3], [8, 6, 9], "#moss", 0), _box([9, 0, 8], [14, 5, 13], "#moss", 0),
                                        _box([8, 0, 2], [12, 4, 6], "#moss", 0)], "#moss"),
        "template_wild_marimo_short": _model([_box([5, 0, 5], [10, 5, 10], "#moss", 0)], "#moss"),
        "template_wild_floating": {"ambientocclusion": False, "render_type": "minecraft:cutout", "textures": {"particle": "#tex"},
                                   "elements": [{"from": [0, 0.25, 0], "to": [16, 0.25, 16],
                                                 "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#tex"},
                                                           "down": {"uv": [0, 16, 16, 0], "texture": "#tex"}}}]},
    }


def write_all() -> list:
    out = []
    for name, f in CROSSES.items():
        c = f()
        out.append(save_png(c.texture(LEAF), BLOCK_TEX / f"{name}.png"))
        out.append(save_png(c.texture(ACCENT), BLOCK_TEX / f"{name}_accent.png"))
        out.append(save_png(c.texture(TOP), BLOCK_TEX / f"{name}_top.png"))
    # the moss cushions and Marimo balls: the interior moss texture with its holes filled darker (closed boxes)
    moss = np.asarray(Image.open(BLOCK_TEX / "plant_moss.png").convert("RGBA")).astype(np.float64)
    hole = moss[..., 3] == 0
    moss[hole, :3] = 0.5 * 255
    moss[..., 3] = 255
    out.append(save_png(moss.astype(np.uint8), BLOCK_TEX / "wild_moss.png"))
    for name, arr in floating().items():
        out.append(save_png(arr, BLOCK_TEX / f"{name}.png"))
    for name, m in templates().items():
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
