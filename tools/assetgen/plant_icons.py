"""M62: an own item icon for every aquarium plant and coral (before: 13 pictures for 57 species, e.g. all 17 epiphytes showed
the same Anubias).

Each species gets two grey 16x16 layers like before (``textures/item/plant_<id>.png`` = leaves / tissue, tinted with the
leaf colour; ``plant_<id>_detail.png`` = rhizome, stems, rock plug, tinted with the stem or rhizome colour, see
``ClientSetup#itemColors`` and ``PlantColors.java``), so the colours stay in one place (the Java species table). The shapes
follow the plants: the small round leaves of Anubias petite, the big round ones of the round-leaf Anubias, the long fronds of
the Java fern, the narrow wavy blades of the Bucephalandras, needles of Rotala, opposite round leaves of Bacopa, the clover
of Marsilea, the tiers of Christmas moss, the pads of the lotuses, the hammer tips of the hammer coral ... Species that still
look alike differ clearly in colour (``AnimalIconsTest`` / ``PlantIconsTest`` compare the tinted pictures).

``build/preview/plant_icons.png`` shows all of them in the colours of the game (parsed from ``PlantSpecies.java``).

Run from the repo root:  py tools/assetgen/plant_icons.py
"""
from __future__ import annotations

import math
import re

import numpy as np
from PIL import Image

from common import ITEM_TEX, JAVA_SRC, PREVIEW, save_png

S = 16


# ---------------------------------------------------------------------------------------------------------- canvas

class Icon:
    def __init__(self, seed: int):
        self.leaf = np.zeros((S, S, 4))
        self.base = np.zeros((S, S, 4))
        self.r = np.random.default_rng(seed)

    def px(self, layer, x, y, v):
        img = self.leaf if layer == 0 else self.base
        x, y = int(round(x)), int(round(y))
        if 0 <= x < S and 0 <= y < S:
            img[y, x, :3] = max(0.0, min(1.0, v))
            img[y, x, 3] = 1.0

    def clear(self, layer, x, y):
        img = self.leaf if layer == 0 else self.base
        if 0 <= x < S and 0 <= y < S:
            img[y, x] = 0

    def finish(self, outline_leaf=0.22, outline_base=0.3):
        for img, v in ((self.leaf, outline_leaf), (self.base, outline_base)):
            if v is None:
                continue
            a = img[..., 3] > 0.5
            n = np.zeros_like(a)
            n[1:, :] |= a[:-1, :]
            n[:-1, :] |= a[1:, :]
            n[:, 1:] |= a[:, :-1]
            n[:, :-1] |= a[:, 1:]
            other = (self.base if img is self.leaf else self.leaf)[..., 3] > 0.5
            edge = n & ~a & ~other
            img[edge, :3] = v
            img[edge, 3] = 1.0
        out = []
        for img in (self.leaf, self.base):
            o = img.copy()
            o[..., 3] = (o[..., 3] > 0.5).astype(np.float64)
            o[..., :3] *= o[..., 3:4]
            out.append(o)
        return out


def profile(kind: str, u: float) -> float:
    """Half width of a leaf (0..1 of its width) at u = 0 (base) .. 1 (tip)."""
    if kind == "oval":
        return math.sin(math.pi * min(1.0, u * 0.92 + 0.04)) ** 0.6
    if kind == "round":
        return math.sqrt(max(0.0, 1 - (2 * u - 1) ** 2)) ** 0.8
    if kind == "heart":
        return math.sqrt(max(0.0, 1 - (2 * u - 0.8) ** 2)) if u < 0.9 else 0.0
    if kind == "lance":
        return math.sin(math.pi * u) ** 0.8 * (1.0 - 0.35 * u)
    if kind == "sword":
        return min(1.0, u * 4) * (1 - u) ** 0.5
    if kind == "needle":
        return 0.5 * (1 - u * 0.6)
    if kind == "strap":
        return min(1.0, u * 5) * (1 - u ** 4)
    return math.sin(math.pi * u)


def leaf(ic: Icon, x0, y0, ang, length, width, kind="oval", wave=0.0, rib=True, light=0.9, dark=0.74, layer=0,
         blotch=0.0, edge=None, veins=None, stalk=0.0, spots=None):
    """Draws a leaf from (x0, y0) along ``ang`` (radians, 0 = right, pi/2 = up) into a layer: the near half lighter."""
    ca, sa = math.cos(ang), -math.sin(ang)
    for y in range(S):
        for x in range(S):
            dx, dy = x - x0, y - y0
            along = dx * ca + dy * sa
            across = -dx * sa + dy * ca
            if along < -0.3 or along > length + 0.3:
                continue
            if along < stalk * length:  # the petiole: a thin line
                if abs(across) < 0.5:
                    ic.px(layer, x, y, dark - 0.04)
                continue
            u = max(0.0, min(1.0, (along - stalk * length) / (length * (1 - stalk))))
            w = width * profile(kind, u) * (1 + wave * math.sin(along * 2.6))
            if abs(across) > w + 0.35:
                continue
            v = light if across <= 0 else dark
            if rib and abs(across) < 0.45 and u < 0.9:
                v = min(1.0, light + 0.06)
            if veins and abs(math.sin(along * veins)) > 0.8 and abs(across) > 0.6:
                v -= 0.12
            if blotch and ic.r.random() < blotch:
                v = 1.0 if ic.r.random() < 0.6 else 0.6
            if edge is not None and abs(across) > w - 0.55:
                v = edge
            if spots and (x * 5 + y * 3) % spots == 0 and abs(across) < w - 0.4:
                v = 0.5
            ic.px(layer, x, y, v)
    return x0 + math.cos(ang) * length, y0 - math.sin(ang) * length


def line(ic: Icon, layer, x0, y0, x1, y1, v=0.8, width=0):
    n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
    for i in range(n + 1):
        t = i / n
        x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        for k in range(-width, width + 1):
            ic.px(layer, x + k, y, v)


def blob(ic: Icon, layer, cx, cy, rx, ry, v=0.85, shade=True, noise=0.0):
    for y in range(S):
        for x in range(S):
            if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1:
                g = v + (0.08 * (x < cx) - 0.08 * (y > cy) if shade else 0) + (ic.r.random() - 0.5) * noise
                ic.px(layer, x, y, g)


# ---------------------------------------------------------------------------------------------------------- forms

def epiphyte(seed, leaves, rhizome=(3, 13, 13), kind="oval", wave=0.0, blotch=0.0, edge=None, veins=None, roots=True, stalk=0.0):
    """Leaves on a creeping rhizome (Anubias, Bucephalandra, Java fern): ``leaves`` = (x0, ang, length, width)."""
    ic = Icon(seed)
    x0, x1, y = rhizome
    for x in range(x0, x1 + 1):
        ic.px(1, x, y, 0.8)
        ic.px(1, x, y + 1, 0.62)
    if roots:
        for x in range(x0 + 1, x1, 3):
            ic.px(1, x, y + 2, 0.55)
    for lx, ang, ln, w in leaves:
        leaf(ic, lx, y - 0.5, ang, ln, w, kind, wave, blotch=blotch, edge=edge, veins=veins, stalk=stalk)
    return ic


def fern_fronds(seed, fronds):
    """Java fern: long, narrow, slightly jagged fronds."""
    ic = Icon(seed)
    for x in range(3, 13):
        ic.px(1, x, 13, 0.75)
        ic.px(1, x, 14, 0.55)
    for lx, ang, ln, w in fronds:
        leaf(ic, lx, 12.5, ang, ln, w, "lance", wave=0.25)
    return ic


def stems(seed, stalks, kind, leaf_len=2.2, spacing=2, tip=None, width=0.8, spread=0.75):
    """Stem plants: ``stalks`` = (x, top y, lean); leaves along them by ``kind`` (pairs of small ``round`` / ``oval`` leaves,
    fine ``needle`` whorls, alternate ``lance`` leaves, ``star`` whorls of Pogostemon, ``lobed`` Hygrophila leaves, ``feather``
    whorls of Myriophyllum); ``tip``: the brighter young leaves at the top (the colour of a red-tipped plant)."""
    ic = Icon(seed)
    for sx, top, lean in stalks:
        def at(y):
            return sx + lean * (15 - y) / 12
        for y in range(top, 16):
            ic.px(1, at(y), y, 0.78)
        for i, y in enumerate(range(top + 1, 14, spacing)):
            x = at(y)
            side = 1 if (i + sx) % 2 == 0 else -1
            shade = 0.06 if i % 2 else 0.0
            if kind in ("round", "oval"):
                for s_ in (-1, 1):
                    leaf(ic, x + s_ * 0.5, y, UP - s_ * spread * 1.6, leaf_len, width, "oval" if kind == "oval" else "round", rib=False,
                         light=0.92 - shade, dark=0.78 - shade)
            elif kind == "needle":
                for s_ in (-1, 1):
                    line(ic, 0, x + s_, y, x + s_ * leaf_len, y - leaf_len * 0.8, 0.88 - shade)
            elif kind == "lance":
                leaf(ic, x, y, UP - side * spread * 1.6, leaf_len, width, "lance", rib=False)
            elif kind == "star":
                for a in (0.25, P - 0.25, 1.0, P - 1.0):
                    line(ic, 0, x, y, x + math.cos(a) * leaf_len, y - math.sin(a) * leaf_len * 0.7, 0.86)
            elif kind == "lobed":
                leaf(ic, x, y, UP - side * 1.15, leaf_len, width, "lance", wave=0.7, rib=True)
            elif kind == "feather":
                for s_ in (-1, 1):
                    for k in range(1, int(leaf_len) + 1):
                        ic.px(0, x + s_ * k, y - k * 0.6, 0.9 - 0.05 * k)
        if tip is not None:
            for dy in range(0, 2):
                for dx in (-1, 0, 1):
                    if dy == 0 or dx == 0:
                        ic.px(0, at(top) + dx, top + dy - 1, tip)
    return ic


def rosette(seed, angles, length, width, kind="sword", base=(7.5, 14), wave=0.0, veins=None, crown=True):
    ic = Icon(seed)
    bx, by = base
    if crown:
        for x in range(int(bx) - 1, int(bx) + 2):
            ic.px(1, x, by + 1, 0.72)
            ic.px(1, x, by, 0.85)
    for i, ang in enumerate(angles):
        ln = length[i] if isinstance(length, (list, tuple)) else length
        leaf(ic, bx, by, ang, ln, width, kind, wave, veins=veins, light=0.9 if i % 2 else 0.84, dark=0.72 if i % 2 else 0.66)
    return ic


def ribbon(seed, blades, curl=0.0):
    ic = Icon(seed)
    for x0, lean, h in blades:
        for y in range(15 - h, 15):
            t = (14 - y) / max(1, h)
            x = x0 + lean * (14 - y) * 0.4 + curl * math.sin(t * 3)
            ic.px(0, x, y, 0.88 if (y + int(x0)) % 3 else 0.74)
    for x in range(4, 13):
        ic.px(1, x, 15, 0.6)
    return ic


def carpet(seed, kind):
    ic = Icon(seed)
    r = ic.r
    for x in range(2, 14):
        ic.px(1, x, 15, 0.55 + 0.1 * (x % 2))
    if kind == "hc":  # the finest carpet: a dense cushion of tiny round leaves
        for y in range(8, 15):
            for x in range(2, 14):
                if math.hypot(x - 7.5, (y - 12) * 1.5) < 6.3 - r.random():
                    ic.px(0, x, y, 0.72 + 0.25 * ((x * 3 + y * 5) % 4 == 0) + 0.06 * r.random())
    elif kind == "monte":  # round coins on runners
        for cy in range(9, 15, 2):
            for cx in range(2 + (cy % 4), 14, 4):
                blob(ic, 0, cx, cy, 1.5, 1.1, 0.86)
        for x in range(2, 14):
            ic.px(1, x, 14, 0.75)
    elif kind == "needle":  # hairgrass: thin upright needles
        for x in range(2, 14):
            h = 4 + (x * 7) % 5
            for y in range(15 - h, 15):
                if x % 2 == 0 or y > 12:
                    ic.px(0, x + (1 if y < 15 - h + 2 and x % 4 == 0 else 0), y, 0.86 if x % 3 else 0.7)
    elif kind == "staurogyne":  # small oval leaves in pairs, low and bushy
        for cx in (3, 6, 9, 12):
            for k, y in enumerate((13, 11, 9)):
                if (cx + k) % 5 == 0 and y == 9:
                    continue
                leaf(ic, cx, y + 1, 1.57 + (0.5 if k % 2 else -0.5), 2.4, 1.0, "oval", rib=False)
    elif kind == "clover":  # water clover: four leaflets on a stalk
        for cx, cy in ((4, 10), (9, 8), (12, 12), (6, 13)):
            line(ic, 1, cx, cy + 1, cx, 15, 0.7)
            for ox, oy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
                blob(ic, 0, cx + ox * 1.0, cy + oy * 0.9, 0.95, 0.85, 0.88, shade=False)
            ic.px(0, cx, cy, 0.62)
    return ic


def moss(seed, kind):
    ic = Icon(seed)
    r = ic.r
    if kind == "java":  # an untidy tangle of fine strands
        for y in range(S):
            for x in range(S):
                d = math.hypot(x - 7.5, (y - 9) * 1.3)
                if d < 6.4 - r.random() * 1.6 and (x + y) % 2 == 0 or d < 4.2:
                    ic.px(0, x, y, 0.6 + 0.36 * r.random())
        for x, y in ((1, 9), (14, 8), (3, 4), (12, 4)):
            ic.px(0, x, y, 0.7)
    elif kind == "christmas":  # tiers of triangular fronds, like a fir tree
        for i, (cy, w) in enumerate(((4, 2.2), (7, 3.6), (10, 5.0), (13, 6.2))):
            for y in range(cy - 2, cy + 1):
                half = w * (y - cy + 3) / 3
                for x in range(int(7.5 - half), int(7.5 + half) + 1):
                    ic.px(0, x, y, 0.9 if x < 7.5 else 0.72)
        line(ic, 1, 7.5, 3, 7.5, 15, 0.7)
    elif kind == "flame":  # upright twisted flames
        for x0, h in ((3, 8), (6, 11), (9, 12), (12, 9)):
            for y in range(15 - h, 15):
                x = x0 + math.sin(y * 0.9) * 0.8
                ic.px(0, x, y, 0.88)
                ic.px(0, x + 1, y, 0.7)
    elif kind == "riccia":  # a bright cushion of forked thalli
        for y in range(5, 14):
            for x in range(2, 14):
                if math.hypot(x - 7.5, (y - 10) * 1.4) < 6.2 and (x * 2 + y) % 5 != 0:
                    ic.px(0, x, y, 0.95 if (x + y) % 3 == 0 else 0.82)
    elif kind == "marimo":  # a velvet ball
        for y in range(S):
            for x in range(S):
                d = math.hypot(x - 7.5, y - 8.5)
                if d < 6.2:
                    lit = 0.95 - 0.06 * d - 0.05 * ((x - 7.5) + (y - 8.5)) / 6
                    ic.px(0, x, y, lit + (r.random() - 0.5) * 0.12)
        for x in range(4, 12):
            ic.px(1, x, 15, 0.5)
    return ic


def lotus(seed, leaves, spots=None, pad=None, flower=False):
    """Tiger lotus / Nymphaea: broad round leaves on long stalks from a bulb (``leaves`` = angle, length, width), spotted;
    ``pad``: a floating leaf at the top (x, y, radius)."""
    ic = Icon(seed)
    blob(ic, 1, 7.5, 13.5, 2.1, 1.6, 0.8)
    for ang, ln, w in leaves:
        leaf(ic, 7.5, 12.5, ang, ln, w, "round", stalk=0.45, spots=spots)
    if pad is not None:
        cx, cy, rr = pad
        for x in range(int(cx - rr - 1), int(cx + rr + 2)):
            if abs(x - cx) <= rr:
                ic.px(0, x, cy, 0.9 if abs(x - cx) > 0.6 else 0.6)
                if abs(x - cx) <= rr - 1:
                    ic.px(0, x, cy - 1, 0.95)
    if flower:
        for x, y in ((11, 3), (12, 2), (10, 2), (11, 1)):
            ic.px(0, x, y, 1.0)
    return ic


def aponogeton(seed):
    ic = Icon(seed)
    blob(ic, 1, 7.5, 13.5, 1.9, 1.5, 0.8)
    for ang, ln in ((2.4, 9), (1.9, 11), (1.4, 12), (0.9, 10), (0.55, 7)):
        leaf(ic, 7.5, 12.5, ang, ln, 1.2, "strap", wave=0.45)
    line(ic, 1, 8, 12, 10, 1, 0.92)  # the flower spike
    return ic


# corals: tissue in layer 0, the rock plug (and a skeleton) in layer 1

def plug(ic: Icon):
    for x in range(4, 12):
        ic.px(1, x, 14, 0.78)
        ic.px(1, x, 15, 0.6)
    for x in range(5, 11):
        ic.px(1, x, 13, 0.85)


def coral(seed, kind):
    ic = Icon(seed)
    plug(ic)
    if kind == "zoanthus":  # button polyps with a bright mouth ring
        for cx, cy in ((5, 11), (8, 10), (11, 11), (6, 7), (10, 7)):
            blob(ic, 0, cx, cy, 1.6, 1.4, 0.72, shade=False)
            ic.px(0, cx, cy, 1.0)
            ic.px(0, cx - 1, cy, 0.9)
    elif kind == "gsp":  # a mat with many tiny star polyps on stalks
        for x in range(3, 13):
            ic.px(0, x, 12, 0.66)
        for cx in range(3, 13, 2):
            h = 3 + (cx * 3) % 4
            line(ic, 0, cx, 12, cx, 12 - h, 0.72)
            for dx, dy in ((-1, 0), (1, 0), (0, -1)):
                ic.px(0, cx + dx, 12 - h + dy, 0.98)
    elif kind == "mushroom":
        for x in range(7, 9):
            ic.px(1, x, 12, 0.7)
        for y in range(6, 12):
            for x in range(2, 14):
                if ((x - 7.5) / 6) ** 2 + ((y - 9) / 3) ** 2 <= 1:
                    ic.px(0, x, y, 0.95 if math.hypot(x - 7.5, (y - 9) * 2) < 1.6 else 0.8 - 0.1 * ((x + y) % 3 == 0))
    elif kind == "leather":
        for y in range(8, 13):
            for x in range(6, 10):
                ic.px(1, x, y, 0.82)
        for y in range(3, 8):
            for x in range(1, 15):
                if ((x - 7.5) / 7) ** 2 + ((y - 7.5) / 4.5) ** 2 <= 1:
                    ic.px(0, x, y, 0.9 if (x + y) % 3 else 0.7)
    elif kind == "hammer":  # short branches ending in hammer-shaped tentacle tips
        for x0, top in ((5, 6), (8, 4), (11, 7)):
            line(ic, 1, x0, 13, x0, top + 1, 0.75)
            for dx in range(-2, 3):
                ic.px(0, x0 + dx, top, 0.95 if abs(dx) < 2 else 0.8)
                ic.px(0, x0 + dx, top - 1, 0.85)
            ic.px(0, x0, top + 1, 0.7)
    elif kind == "acropora":  # a staghorn: thin forked branches with pale tips
        for x0, ang, ln in ((8, 1.57, 10), (8, 1.15, 8), (8, 2.0, 8), (6, 1.4, 5), (10, 1.75, 5)):
            for i in range(ln * 2):
                t = i / 2
                x = x0 + math.cos(ang) * t
                y = 12 - math.sin(ang) * t
                ic.px(0, x, y, 0.98 if t > ln - 1.5 else 0.74)
            fx, fy = x0 + math.cos(ang) * ln * 0.6, 12 - math.sin(ang) * ln * 0.6
            line(ic, 0, fx, fy, fx + math.cos(ang + 0.6) * 2.5, fy - math.sin(ang + 0.6) * 2.5, 0.8)
    elif kind == "dome":
        for y in range(4, 13):
            for x in range(2, 14):
                if ((x - 7.5) / 6) ** 2 + ((y - 12.5) / 8) ** 2 <= 1:
                    ic.px(0, x, y, 0.95 if math.sin(x * 1.1 + math.sin(y) * 2) > 0.5 else 0.72)
    elif kind == "anemone":
        for i, x in enumerate(range(3, 14, 2)):
            h = 5 + (i % 3)
            for y in range(12 - h, 12):
                ic.px(0, x, y, 0.75)
            blob(ic, 0, x, 12 - h - 0.5, 0.9, 0.9, 0.97, shade=False)
        for x in range(3, 13):
            ic.px(0, x, 12, 0.8)
    return ic


# ---------------------------------------------------------------------------------------------------------- the species

P = math.pi
UP = P / 2

SPECIES = {
    # epiphytes: (x0 on the rhizome, angle, length, half width)
    "java_fern": lambda: fern_fronds(6201, [(4, 2.25, 10, 1.1), (6, 1.8, 12, 1.2), (9, 1.4, 12, 1.2), (11, 0.95, 9, 1.0)]),
    "anubias_nana": lambda: epiphyte(6202, [(4, 2.25, 7, 1.6), (7, 1.7, 9, 1.8), (10, 1.2, 8, 1.7), (12, 0.75, 5, 1.4)], stalk=0.3),
    "anubias_nana_petite": lambda: epiphyte(6203, [(4, 2.5, 4, 1.2), (5, 2.0, 5, 1.3), (7, 1.6, 5.5, 1.3), (9, 1.25, 5, 1.3),
                                                  (11, 0.8, 4, 1.2), (12, 0.45, 3, 1.0)], stalk=0.3, kind="round", rhizome=(3, 13, 14)),
    "anubias_pinto": lambda: epiphyte(6204, [(4, 2.15, 7, 2.0), (8, 1.55, 9, 2.2), (11, 0.95, 7, 2.0)], stalk=0.3, blotch=0.35),
    "anubias_white": lambda: epiphyte(6205, [(4, 2.2, 6, 1.5), (7, 1.7, 8, 1.6), (10, 1.25, 8, 1.6), (12, 0.8, 5, 1.3)], stalk=0.35,
                                      edge=1.0),
    "anubias_coffeefolia": lambda: epiphyte(6206, [(4, 2.15, 8, 1.6), (7, 1.6, 10, 1.8), (10, 1.05, 8, 1.6)], kind="lance", veins=2.2,
                                            wave=0.12, stalk=0.2),
    "anubias_barteri_round_leaf": lambda: epiphyte(6207, [(4, 2.1, 7, 2.6), (8, 1.55, 9, 2.9), (11, 0.95, 6, 2.4)], kind="round", stalk=0.3),
    "bucephalandra_green_wavy": lambda: epiphyte(6208, [(4, 2.3, 7, 1.1), (6, 1.85, 9, 1.2), (8, 1.45, 9, 1.2), (10, 1.05, 8, 1.1),
                                                        (12, 0.65, 5, 1.0)], kind="lance", wave=0.35),
    "bucephalandra_brownie_red": lambda: epiphyte(6209, [(5, 2.15, 6, 1.3), (7, 1.65, 7, 1.4), (10, 1.15, 6, 1.3)], kind="lance", wave=0.3,
                                                  rhizome=(4, 12, 13)),
    "bucephalandra_blue": lambda: epiphyte(6210, [(4, 2.2, 6, 1.4), (6, 1.75, 8, 1.5), (9, 1.35, 8, 1.5), (11, 0.9, 6, 1.3)], kind="oval",
                                           wave=0.2, blotch=0.12),
    "bucephalandra_mini_needle": lambda: epiphyte(6211, [(x, a, 6 + (i % 3) * 1.5, 0.7) for i, (x, a) in enumerate(
        [(3, 2.45), (5, 2.1), (6, 1.8), (8, 1.6), (9, 1.4), (10, 1.15), (12, 0.75)])], kind="needle"),
    "bucephalandra_kedagang": lambda: epiphyte(6212, [(4, 2.05, 9, 0.9), (7, 1.7, 11, 1.0), (9, 1.35, 10, 1.0), (11, 1.0, 8, 0.9)],
                                               kind="lance", wave=0.2),
    "bucephalandra_theia": lambda: epiphyte(6213, [(5, 1.95, 7, 1.6), (7, 1.65, 9, 1.7), (9, 1.4, 9, 1.7), (11, 1.1, 7, 1.6)], kind="oval",
                                            rhizome=(4, 12, 13)),
    "bucephalandra_godzilla": lambda: epiphyte(6214, [(4, 2.25, 8, 2.0), (8, 1.6, 10, 2.2), (11, 0.95, 8, 2.0)], kind="lance", wave=0.4),
    "bucephalandra_pink_lady": lambda: epiphyte(6215, [(4, 2.1, 6, 1.4), (6, 1.7, 8, 1.5), (9, 1.35, 8, 1.5), (11, 0.9, 6, 1.3)],
                                                kind="lance", wave=0.25, edge=1.0),
    "bucephalandra_mini_pink": lambda: epiphyte(6216, [(4, 2.45, 4, 1.0), (5, 2.0, 5, 1.1), (7, 1.65, 5.5, 1.1), (9, 1.35, 5.5, 1.1),
                                                       (11, 0.95, 4.5, 1.0), (12, 0.55, 3.5, 0.9)], kind="lance", edge=1.0, rhizome=(3, 13, 14)),
    "bucephalandra_snow_white": lambda: epiphyte(6217, [(5, 2.0, 7, 1.7), (8, 1.55, 8, 1.8), (11, 1.0, 7, 1.7)], kind="oval", edge=1.0,
                                                 blotch=0.2),
    # mosses
    "java_moss": lambda: moss(6220, "java"),
    "christmas_moss": lambda: moss(6221, "christmas"),
    "flame_moss": lambda: moss(6222, "flame"),
    "riccia_fluitans": lambda: moss(6223, "riccia"),
    "marimo": lambda: moss(6224, "marimo"),
    # rosettes
    "cryptocoryne_wendtii_green": lambda: rosette(6230, [2.5, 2.0, 1.57, 1.15, 0.65], [7, 9, 10, 9, 7], 1.5, "lance", wave=0.15),
    "cryptocoryne_wendtii_brown": lambda: rosette(6231, [2.6, 2.1, 1.7, 1.3, 0.9, 0.5], [6, 8, 9, 9, 8, 6], 1.4, "lance", wave=0.4),
    "cryptocoryne_flamingo": lambda: rosette(6232, [2.4, 1.9, 1.5, 1.1, 0.7], [7, 10, 11, 10, 7], 1.2, "sword"),
    "cryptocoryne_parva": lambda: rosette(6233, [2.6, 2.25, 1.95, 1.7, 1.45, 1.2, 0.9, 0.55], [5, 6, 7, 7, 7, 7, 6, 5], 0.8, "sword"),
    "amazon_sword": lambda: rosette(6234, [2.35, 1.9, 1.57, 1.25, 0.8], [9, 11, 12, 11, 9], 1.6, "sword", veins=1.6),
    "echinodorus_red_diamond": lambda: rosette(6235, [2.5, 1.95, 1.4, 0.75], [8, 9, 9, 8], 2.4, "oval"),
    "sagittaria_subulata": lambda: rosette(6236, [2.4, 2.1, 1.85, 1.62, 1.45, 1.2, 0.95, 0.7], [7, 9, 10, 11, 10, 9, 8, 6], 0.55, "needle"),
    # stem plants: (x, top y, lean)
    "rotala_rotundifolia": lambda: stems(6240, [(4, 2, -1), (11, 4, 1)], "round", leaf_len=2.2, spacing=3, tip=1.0, width=0.8, spread=0.5),
    "rotala_hra": lambda: stems(6241, [(3, 4, -1), (8, 1, 0), (13, 3, 1)], "oval", leaf_len=2.0, spacing=3, tip=1.0, width=0.6, spread=0.45),
    "rotala_wallichii": lambda: stems(6242, [(4, 2, -1), (11, 3, 1)], "needle", leaf_len=3.0, spacing=2, tip=1.0),
    "ludwigia_repens": lambda: stems(6243, [(4, 3, -1), (11, 5, 1)], "round", leaf_len=3.0, spacing=4, width=1.1, spread=0.5),
    "ludwigia_super_red": lambda: stems(6244, [(7, 1, 0)], "oval", leaf_len=3.6, spacing=4, width=1.2, spread=0.72),
    "alternanthera_reineckii": lambda: stems(6245, [(5, 2, 0), (11, 4, 1)], "lance", leaf_len=4.0, spacing=3, width=1.0, spread=0.6),
    "hygrophila_pinnatifida": lambda: stems(6246, [(7, 3, 0)], "lobed", leaf_len=5.5, spacing=3, width=1.5),
    "pogostemon_helferi": lambda: stems(6247, [(5, 6, -1), (10, 5, 1)], "star", leaf_len=3.5, spacing=3),
    "limnophila_sessiliflora": lambda: stems(6248, [(7, 1, 0)], "needle", leaf_len=4.5, spacing=2),
    "myriophyllum_red": lambda: stems(6249, [(4, 2, -1), (11, 3, 1)], "feather", leaf_len=3, spacing=2, tip=0.95),
    "bacopa_caroliniana": lambda: stems(6250, [(5, 2, 0), (11, 4, 0)], "oval", leaf_len=2.8, spacing=3, tip=1.0, width=1.0, spread=0.68),
    # ribbon and carpets
    "vallisneria": lambda: ribbon(6260, [(3, -0.4, 10), (5, -0.1, 13), (7, 0.15, 14), (9, 0.35, 13), (11, 0.55, 11), (13, 0.3, 8)], curl=0.8),
    "monte_carlo": lambda: carpet(6261, "monte"),
    "hemianthus_callitrichoides": lambda: carpet(6262, "hc"),
    "eleocharis_acicularis": lambda: carpet(6263, "needle"),
    "staurogyne_repens": lambda: carpet(6264, "staurogyne"),
    "marsilea_hirsuta": lambda: carpet(6265, "clover"),
    # tubers: (angle, length, half width) of the leaves
    "red_tiger_lotus": lambda: lotus(6270, [(2.4, 8, 2.4), (1.6, 11, 2.6), (0.8, 8, 2.3)], spots=6),
    "nymphaea_lotus_green": lambda: lotus(6271, [(2.2, 8, 2.2), (0.95, 9, 2.4)], spots=7, pad=(8, 2, 3.5), flower=True),
    "aponogeton_ulvaceus": lambda: aponogeton(6272),
    # corals and the anemone
    "zoanthus": lambda: coral(6280, "zoanthus"),
    "green_star_polyps": lambda: coral(6281, "gsp"),
    "mushroom_coral": lambda: coral(6282, "mushroom"),
    "leather_coral": lambda: coral(6283, "leather"),
    "hammer_coral": lambda: coral(6284, "hammer"),
    "lobophyllia": lambda: coral(6285, "dome"),
    "acropora": lambda: coral(6286, "acropora"),
    "bubble_tip_anemone": lambda: coral(6287, "anemone"),
}


# ---------------------------------------------------------------------------------------------------------- colours (preview)

_ENUM = re.compile(r'^\s+([A-Z_0-9]+)\((?:Habitat\.([A-Z]+), )?"([a-z_0-9]+)", Form\.([A-Z]+), [^,]+, \d+, Light\.[A-Z_]+, Red\.([A-Z]+), "[a-z_]+",'
                   r' (0x[0-9A-Fa-f]+|0), (0x[0-9A-Fa-f]+|0), (0x[0-9A-Fa-f]+|0), (0x[0-9A-Fa-f]+|0), (0x[0-9A-Fa-f]+|0)\)', re.M)


def species_table():
    """id -> dict(form, red, land, leaf, top, red_c, stem, rhizome) from PlantSpecies.java."""
    src = (JAVA_SRC / "plants" / "PlantSpecies.java").read_text(encoding="utf-8")
    out = {}
    for m in _ENUM.finditer(src):
        _, habitat, sid, form, red, *cols = m.groups()
        leaf_c, top, red_c, stem, rhizome = (int(c, 16) for c in cols)
        out[sid] = dict(form=form, red=red, land=habitat is not None, leaf=leaf_c, top=top, red_c=red_c, stem=stem, rhizome=rhizome)
    return out


def _lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(((a >> s) & 255) * (1 - t) + ((b >> s) & 255) * t) for s in (16, 8, 0))


def _pack(c):
    return c[0] << 16 | c[1] << 8 | c[2]


def _scale(c, k):
    return _pack(tuple(min(255, round(((c >> s) & 255) * k)) for s in (16, 8, 0)))


def color(sp, part, tint=2):
    """PlantColors#color (the parts the icons use: LEAF, STEM, RHIZOME, TOP)."""
    f = tint / 3
    base = {"STEM": sp["stem"], "RHIZOME": sp["rhizome"], "TOP": sp["top"]}.get(part, sp["leaf"])
    red = sp["red"]
    if red == "NONE":
        return base
    if red == "RED":
        if part == "TOP":
            return _pack(_lerp(sp["top"], _scale(sp["red_c"], 1.12), min(1, 0.2 + f)))
        if part == "LEAF":
            return _pack(_lerp(sp["leaf"], sp["red_c"], 0.1 + 0.8 * f))
        if part == "STEM":
            return _pack(_lerp(sp["stem"], _scale(sp["red_c"], 0.8), 0.15 + 0.5 * f))
        return _pack(_lerp(base, _scale(sp["red_c"], 0.7), 0.3 * f))
    if red == "TIPS":
        if part == "TOP":
            return _pack(_lerp(sp["top"], sp["red_c"], f))
        if part == "LEAF":
            return _pack(_lerp(sp["leaf"], sp["red_c"], 0.12 * f))
        if part == "STEM":
            return _pack(_lerp(sp["stem"], _scale(sp["red_c"], 0.8), 0.4 * f))
        return base
    if red == "FLAMINGO":
        if part == "TOP":
            return _pack(_lerp(sp["top"], _scale(sp["red_c"], 1.05), 0.25 + 0.75 * f))
        if part == "LEAF":
            return _scale(_pack(_lerp(sp["leaf"], sp["red_c"], 0.1 + 0.6 * f)), 1.0 + 0.08 * f)
        return _pack(_lerp(base, sp["red_c"], 0.3 * f))
    if red == "CORAL":
        return base  # tint 2: healthy
    if red == "WHITE":
        w = 0.12 + 0.33 * (f - 0.34) / 0.33
        if part == "TOP":
            return _pack(_lerp(sp["top"], _scale(sp["red_c"], 1.03), min(1, 0.08 + w)))
        if part == "LEAF":
            return _pack(_lerp(sp["leaf"], sp["red_c"], w))
        if part == "STEM":
            return _pack(_lerp(sp["stem"], sp["red_c"], 0.4 * w))
        return base
    return base


def detail_part(sp):
    if sp["land"]:
        return "TOP"
    return "STEM" if sp["form"] in ("STEM", "DISC", "TOADSTOOL") else "RHIZOME"


def tinted(layers, cols):
    out = np.zeros((S, S, 4))
    for img, c in zip(layers, cols):
        rgb = np.array([(c >> 16) & 255, (c >> 8) & 255, c & 255]) / 255.0
        a = img[..., 3:4]
        out[..., :3] = out[..., :3] * (1 - a) + img[..., :3] * rgb * a
        out[..., 3:] = np.maximum(out[..., 3:], a)
    return out


def preview(icons, table, path, scale=6, cols=10, order=None):
    from PIL import ImageDraw
    names = order or list(icons)
    rows = (len(names) + cols - 1) // cols
    cell, label = S * scale + 8, 12
    sheet = Image.new("RGBA", (cols * cell, rows * (cell + label)), (139, 139, 139, 255))
    draw = ImageDraw.Draw(sheet)
    for i, n in enumerate(names):
        sp = table[n]
        img = tinted(icons[n], (color(sp, "LEAF"), color(sp, detail_part(sp))))
        im = Image.fromarray((img * 255).astype(np.uint8), "RGBA").resize((S * scale, S * scale), Image.NEAREST)
        x, y = (i % cols) * cell, (i // cols) * (cell + label)
        sheet.alpha_composite(im, (x + 4, y + 4))
        draw.text((x + 2, y + cell - 2), n[:17], fill=(20, 20, 20, 255))
    PREVIEW.mkdir(parents=True, exist_ok=True)
    sheet.save(path)
    return path


def write_all():
    out = []
    icons = {}
    for sid, fn in SPECIES.items():
        leaf_l, base_l = fn().finish()
        icons[sid] = (leaf_l, base_l)
        out.append(save_png(leaf_l, ITEM_TEX / f"plant_{sid}.png"))
        out.append(save_png(base_l, ITEM_TEX / f"plant_{sid}_detail.png"))
    table = species_table()
    missing = [s for s, sp in table.items() if not sp["land"] and s not in SPECIES]
    if missing:
        raise SystemExit(f"plant species without an icon: {missing}")
    out.append(preview(icons, table, PREVIEW / "plant_icons.png"))
    out.append(sheet_all(PREVIEW / "plant_items.png"))
    return out




# ---------------------------------------------------------------------------------------------------------- all plant items

_LAND_ICON = re.compile(r'case ([A-Z_, ]+) -> "([a-z_]+)";')


def land_icons():
    """species enum name -> icon of the terrarium plants (LandLook#icon)."""
    src = (JAVA_SRC / "plants" / "wild" / "LandLook.java").read_text(encoding="utf-8")
    body = src[src.index("public static String icon("):]
    body = body[:body.index("default ->")]
    out = {}
    for names, icon in _LAND_ICON.findall(body):
        for n in names.split(","):
            out[n.strip()] = icon
    return out


def _load(path):
    return np.array(Image.open(path).convert("RGBA")).astype(np.float64) / 255.0


def item_icons():
    """id -> the tinted 16x16 icon (RGBA 0..1) of every plant item, as the game draws it (tint 2)."""
    table = species_table()
    src = (JAVA_SRC / "plants" / "PlantSpecies.java").read_text(encoding="utf-8")
    enum_of = {m.group(3): m.group(1) for m in _ENUM.finditer(src)}
    land = land_icons()
    out = {}
    for sid, sp in table.items():
        if sp["land"]:
            icon = land.get(enum_of[sid], "grass")
            layers = (_load(ITEM_TEX / f"terrarium_{icon}.png"), _load(ITEM_TEX / f"terrarium_{icon}_detail.png"))
        else:
            layers = (_load(ITEM_TEX / f"plant_{sid}.png"), _load(ITEM_TEX / f"plant_{sid}_detail.png"))
        out[sid] = tinted(layers, (color(sp, "LEAF"), color(sp, detail_part(sp))))
    return out


SLOT = np.array([139, 139, 139]) / 255.0


def distance(a, b):
    """Mean of the largest channel difference over the pixels either icon covers, both drawn on the slot grey (0..255)."""
    ca = a[..., :3] * a[..., 3:] + SLOT * (1 - a[..., 3:])
    cb = b[..., :3] * b[..., 3:] + SLOT * (1 - b[..., 3:])
    mask = (a[..., 3] > 0.5) | (b[..., 3] > 0.5)
    return float(np.abs(ca - cb).max(axis=2)[mask].mean() * 255)


def closest(n=15):
    icons = item_icons()
    ids = sorted(icons)
    pairs = sorted((distance(icons[x], icons[y]), x, y) for i, x in enumerate(ids) for y in ids[i + 1:])
    return pairs[:n]


def sheet_all(path, scale=4, cols=12):
    """Contact sheet of every plant item (aquarium and terrarium plants) as the game tints it, with the names."""
    from PIL import ImageDraw
    icons = item_icons()
    names = sorted(icons)
    rows = (len(names) + cols - 1) // cols
    cell, label = max(S * scale + 8, 104), 12
    sheet = Image.new("RGBA", (cols * cell, rows * (S * scale + 8 + label)), (139, 139, 139, 255))
    draw = ImageDraw.Draw(sheet)
    for i, n in enumerate(names):
        im = Image.fromarray((icons[n] * 255).astype(np.uint8), "RGBA").resize((S * scale, S * scale), Image.NEAREST)
        x, y = (i % cols) * cell, (i // cols) * (S * scale + 8 + label)
        sheet.alpha_composite(im, (x + (cell - S * scale) // 2, y + 4))
        draw.text((x + 2, y + S * scale + 6), n[:16], fill=(20, 20, 20, 255))
    sheet.save(path)
    return path


if __name__ == "__main__":
    for p in write_all():
        print(p)
