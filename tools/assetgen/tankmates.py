"""M19 assets: the tankmate fish (models, textures), the spawning cave and mop blocks, the clutch textures and icons.

Single source of truth for the tankmate models, like ``model_defs.py`` for the betta and ``critters.py`` for the small
animals:

* units are model pixels, +Y points DOWN, the head points to -Z; a model is 32 px long (snout z = -16, tail tip z = +16)
  and its belly rests on y = 0 (the Java code adds the entity offset 24); the renderer scales it to the species' length,
* a model is a tree of named groups (``body``, ``tail``, ``dorsal``, ``pec_r``/``pec_l``, ``male``/``female`` for parts
  only one sex has, ``filaments`` of the gourami, ``seg1``.. for the kuhli loach) of cuboids with integer sizes; fins
  are width-0 planes whose outline is an alpha cut-out (cutout render type),
* every species (and variety and sex where they look different) has its own coloured 128x64 texture painted in model
  space with the exact box-UV mapping of ``ModelPart.Cube``, so stripes run where they belong on both sides.

Writes ``client/model/TankmateModelDefinitions.java``, ``textures/entity/tankmates/*.png``, the block textures
``spawning_cave``/``spawning_mop``, ``entity/tankmates/eggs.png`` and the icons ``breeding_box``, ``fry_powder``,
``algae_wafer``, ``decor_spawning_cave``, ``decor_spawning_mop``. Preview sheet: ``build/preview/tankmates.png``.

Run from the repo root:  py tools/assetgen/tankmates.py
"""
from __future__ import annotations

import math
from dataclasses import dataclass, field

import numpy as np

from common import BLOCK_TEX, ITEM_TEX, JAVA_SRC, PREVIEW, TEX, hex_f, save_png

TM_TEX = TEX / "entity" / "tankmates"
TEX_W, TEX_H = 128, 64


# ---------------------------------------------------------------------------------------------------------- geometry

@dataclass
class Box:
    name: str
    part: str
    center: tuple
    size: tuple
    rot: tuple = (0.0, 0.0, 0.0)
    fin: str | None = None
    u: int = 0
    v: int = 0


@dataclass
class Group:
    name: str
    pivot: tuple = (0.0, 0.0, 0.0)  # absolute model coordinates
    parent: str | None = None
    boxes: list = field(default_factory=list)


def box(name, part, center, size, rot=(0, 0, 0), fin=None):
    return Box(name, part, tuple(float(c) for c in center), tuple(int(round(s)) for s in size), tuple(float(r) for r in rot), fin)


def smooth(a, b, t):
    t = max(0.0, min(1.0, (t - a) / (b - a)))
    return t * t * (3 - 2 * t)


class Plan:
    """Body profile of one model: depth, width and centre line along t = 0 (snout) .. 1 (end of the peduncle)."""

    def __init__(self, name, snout, ped, depth, width, head, ped_frac, peak, flat_belly=False, belly=0.0, mid=None, nseg=9):
        self.name, self.snout, self.ped = name, snout, ped
        self.depth, self.width, self.head, self.ped_frac, self.peak = depth, width, head, ped_frac, peak
        self.flat_belly, self.belly, self.mid, self.nseg = flat_belly, belly, mid, nseg

    def frac(self, t):
        if t < self.peak:
            return self.head + (1 - self.head) * math.sin(0.5 * math.pi * t / self.peak) ** 0.8
        return 1 - (1 - self.ped_frac) * smooth(self.peak, 1.0, t)

    def d(self, t):
        return self.depth * self.frac(t)

    def w(self, t):
        return max(1.0, self.width * (0.55 + 0.45 * self.frac(t)))

    def top(self, t):
        if self.flat_belly:
            return -(self.belly + self.d(t))
        return -(self.mid + self.d(t) / 2)

    def bottom(self, t):
        if self.flat_belly:
            return -self.belly
        return -(self.mid - self.d(t) / 2)

    def t_of(self, z):
        return (z - self.snout) / (self.ped - self.snout)

    def v_of(self, y, z):
        """0 = back, 1 = belly, at length position z."""
        t = max(0.0, min(1.0, self.t_of(z)))
        top, bot = self.top(t), self.bottom(t)
        return max(0.0, min(1.0, (y - top) / max(0.5, bot - top)))

    def segments(self, part="body"):
        L = self.ped - self.snout
        lens = [L // self.nseg] * self.nseg
        for i in range(L - sum(lens)):
            lens[self.nseg // 2 + (i % 3) - 1] += 1
        out = []
        z = self.snout
        for i, ln in enumerate(lens):
            tc = self.t_of(z + ln / 2)
            depth = max(1, round(self.d(tc)))
            width = max(1, round(self.w(tc)))
            if self.flat_belly:
                cy = -(self.belly + depth / 2)
            else:
                cy = -self.mid
            out.append(box(f"{part}{i}", "head" if i < 2 else part, (0, cy, z + ln / 2), (width, depth, ln)))
            z += ln
        return out


def eyes(plan: Plan, t, up=0.25, size=2, inset=0.3):
    tt = plan.t_of(t) if isinstance(t, int) and abs(t) > 1 else t
    z = plan.snout + tt * (plan.ped - plan.snout)
    wv = round(plan.w(tt))
    y = plan.top(tt) + (plan.bottom(tt) - plan.top(tt)) * up + size / 2
    x = wv / 2 - inset + size / 2 - 0.5
    return [box("eye_r", "eye", (x, y, z), (size, size, size)), box("eye_l", "eye", (-x, y, z), (size, size, size))]


def pair(name, part, center, size, rot, fin):
    x, y, z = center
    rx, ry, rz = rot
    return [box(name + "_r", part, (x, y, z), size, (rx, ry, rz), fin), box(name + "_l", part, (-x, y, z), size, (rx, -ry, -rz), fin)]


def fusiform_plan(name, depth, width, mid, peak=0.4, head=0.55, ped_frac=0.32, snout=-15, ped=9):
    return Plan(name, snout, ped, depth, width, head, ped_frac, peak, mid=mid)


def caudal(plan: Plan, length, height, shape, group_parent=None):
    t = 1.0
    cy = (plan.top(t) + plan.bottom(t)) / 2
    g = Group("tail", (0, cy, plan.ped - 1), group_parent)
    g.boxes.append(box("caudal", "tail", (0, cy, plan.ped - 1 + length / 2), (0, height, length), fin=shape))
    return g


def dorsal_fin(plan, t0, length, height, shape, name="dorsal", lean=-8.0, part="dorsal", parent=None):
    z0 = plan.snout + t0 * (plan.ped - plan.snout)
    top = plan.top(t0 + 0.5 * length / (plan.ped - plan.snout))
    g = Group(name, (0, top, z0), parent)
    g.boxes.append(box(name, part, (0, top - height / 2 + 0.5, z0 + length / 2), (0, height, length), (lean, 0, 0), fin=shape))
    return g


def anal_fin(plan, t0, length, height, shape, name="anal", part="anal"):
    z0 = plan.snout + t0 * (plan.ped - plan.snout)
    bot = plan.bottom(t0 + 0.5 * length / (plan.ped - plan.snout))
    return box(name, part, (0, bot + height / 2 - 0.5, z0 + length / 2), (0, height, length), (8, 0, 0), fin=shape)


def pectorals(plan, t, length, height, spread=35.0, droop=20.0, up=0.7, horizontal=False):
    z = plan.snout + t * (plan.ped - plan.snout)
    wv = plan.w(t)
    y = plan.top(t) + (plan.bottom(t) - plan.top(t)) * up
    groups = []
    for side, sx in (("r", 1), ("l", -1)):
        g = Group(f"pec_{side}", (sx * wv / 2, y, z))
        if horizontal:
            g.boxes.append(box(f"pec_{side}", "pectoral", (sx * (wv / 2 + length * 0.4), y + 0.2, z + length * 0.35),
                               (length, 0, height), (0, sx * spread, -sx * droop), fin="pec_flat"))
        else:
            g.boxes.append(box(f"pec_{side}", "pectoral", (sx * (wv / 2 + 0.6), y, z + length / 2), (0, height, length),
                               (0, sx * spread, sx * droop), fin="oval"))
        groups.append(g)
    return groups


# ---------------------------------------------------------------------------------------------------------- body plans

def tetra():
    p = fusiform_plan("tetra", depth=8.0, width=4.0, mid=6.0, peak=0.38, head=0.55, ped_frac=0.3)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.12, up=0.3)
    body.boxes.append(anal_fin(p, 0.62, 6, 3, "anal"))
    body.boxes.append(box("adipose", "adipose", (0, p.top(0.88) - 0.5, p.snout + 0.88 * (p.ped - p.snout)), (0, 2, 2), fin="round_small"))
    body.boxes += pair("pelvic", "pelvic", (0.8, p.bottom(0.45) + 0.6, p.snout + 0.45 * 24), (0, 2, 3), (14, 18, 0), "oval")
    return p, [body, caudal(p, 8, 10, "fork"), dorsal_fin(p, 0.43, 5, 5, "tri")] + pectorals(p, 0.22, 4, 2)


def rasbora():
    p = fusiform_plan("rasbora", depth=9.0, width=4.0, mid=6.5, peak=0.4, head=0.5, ped_frac=0.3)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.12, up=0.3)
    body.boxes.append(anal_fin(p, 0.6, 6, 3, "anal"))
    body.boxes += pair("pelvic", "pelvic", (0.8, p.bottom(0.45) + 0.6, p.snout + 0.45 * 24), (0, 2, 3), (14, 18, 0), "oval")
    return p, [body, caudal(p, 8, 11, "fork"), dorsal_fin(p, 0.45, 5, 5, "tri")] + pectorals(p, 0.22, 4, 2)


def cory():
    p = Plan("cory", -15, 9, 10.0, 6.0, 0.72, 0.42, 0.33, flat_belly=True, belly=0.0, nseg=9)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.16, up=0.15)
    body.boxes += pair("barbel", "barbel", (1.0, -1.0, -15.5), (1, 1, 2), (0, 25, 0), None)
    body.boxes.append(box("adipose", "adipose", (0, p.top(0.88) - 0.5, p.snout + 0.88 * 24), (0, 2, 2), fin="round_small"))
    body.boxes.append(anal_fin(p, 0.7, 4, 3, "oval"))
    body.boxes += pair("pelvic", "pelvic", (1.8, -0.6, p.snout + 0.52 * 24), (3, 0, 3), (0, 20, -10), "pec_flat")
    return p, [body, caudal(p, 8, 11, "fork"), dorsal_fin(p, 0.36, 5, 5, "tri", lean=-16)] + pectorals(p, 0.3, 5, 3, spread=30, droop=10, up=0.92,
                                                                                                    horizontal=True)


def pleco():
    p = Plan("pleco", -15, 9, 6.0, 10.0, 0.62, 0.34, 0.3, flat_belly=True, belly=0.0, nseg=9)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.18, up=0.05, size=2, inset=1.6)
    body.boxes.append(box("mouth", "mouth", (0, -0.5, -12.5), (5, 1, 4)))
    body.boxes.append(box("adipose", "adipose", (0, p.top(0.86) - 0.5, p.snout + 0.86 * 24), (0, 2, 2), fin="round_small"))
    body.boxes += pair("pelvic", "pelvic", (2.8, -0.4, p.snout + 0.6 * 24), (4, 0, 4), (0, 20, -6), "pec_flat")
    male = Group("male", (0, 0, 0))
    for i, (x, z) in enumerate(((1.6, -15.0), (2.8, -14.0), (0.6, -15.4), (3.6, -12.8))):
        male.boxes += pair(f"bristle{i}", "bristle", (x, p.top(0.03) - 1.0 + i * 0.2, z), (1, 2, 1), (-10, 0, 10 + i * 6), None)
    return p, [body, male, caudal(p, 8, 10, "lyre"), dorsal_fin(p, 0.3, 8, 6, "sail", lean=-6)] + pectorals(p, 0.26, 7, 5, spread=25, droop=6,
                                                                                                          up=0.95, horizontal=True)


def oto():
    p = Plan("oto", -15, 9, 6.0, 5.0, 0.6, 0.34, 0.38, flat_belly=True, belly=0.0, nseg=9)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.14, up=0.15, size=2, inset=0.7)
    body.boxes.append(box("mouth", "mouth", (0, -0.5, -13.5), (3, 1, 3)))
    body.boxes += pair("pelvic", "pelvic", (1.5, -0.4, p.snout + 0.58 * 24), (3, 0, 3), (0, 20, -6), "pec_flat")
    return p, [body, caudal(p, 7, 9, "fork"), dorsal_fin(p, 0.36, 4, 5, "tri", lean=-8)] + pectorals(p, 0.26, 4, 3, spread=25, droop=6,
                                                                                                  up=0.95, horizontal=True)


def kuhli():
    """Eel-like: a chain of segments that each follow the one in front (the renderer bends them in a wave)."""
    p = Plan("kuhli", -16, 13, 4.0, 3.0, 0.75, 0.6, 0.3, flat_belly=True, belly=0.0, nseg=1)
    groups = []
    zs = [-16, -12, -7, -2, 3, 8, 13]
    parent = None
    for i in range(6):
        z0, z1 = zs[i], zs[i + 1]
        tc = p.t_of((z0 + z1) / 2)
        depth = max(2, round(p.d(tc)))
        width = max(2, round(p.w(tc)))
        name = "body" if i == 0 else f"seg{i}"
        g = Group(name, (0, -depth / 2, z0), parent)
        g.boxes.append(box(f"seg{i}", "head" if i == 0 else "body", (0, -depth / 2, (z0 + z1) / 2), (width, depth, z1 - z0)))
        if i == 0:
            g.boxes += eyes(p, 0.08, up=0.15, size=1, inset=0.4)
            g.boxes += pair("barbel", "barbel", (0.8, -0.6, -16.5), (1, 1, 2), (0, 25, 0), None)
        if i == 4:
            g.boxes.append(box("dorsal", "dorsal", (0, -depth - 0.5, z0 + 3), (0, 2, 3), (-8, 0, 0), fin="round_small"))
        groups.append(g)
        parent = name
    tail = Group("tail", (0, -1.5, 13), "seg5")
    tail.boxes.append(box("caudal", "tail", (0, -1.5, 14.5), (0, 3, 3), fin="round"))
    groups.append(tail)
    return p, groups


def loach():
    p = Plan("loach", -15, 9, 10.0, 5.0, 0.5, 0.34, 0.42, flat_belly=True, belly=1.0, nseg=9)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.13, up=0.35, size=2, inset=0.4)
    body.boxes += pair("barbel", "barbel", (0.8, -2.0, -15.6), (1, 1, 2), (20, 25, 0), None)
    body.boxes.append(anal_fin(p, 0.68, 4, 3, "anal"))
    body.boxes += pair("pelvic", "pelvic", (1.2, -1.0, p.snout + 0.5 * 24), (0, 3, 3), (16, 20, 0), "oval")
    return p, [body, caudal(p, 9, 13, "fork"), dorsal_fin(p, 0.42, 5, 5, "tri", lean=-10)] + pectorals(p, 0.25, 4, 2, up=0.85)


def livebearer():
    p = fusiform_plan("livebearer", depth=8.0, width=4.0, mid=6.0, peak=0.36, head=0.55, ped_frac=0.36)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.12, up=0.28)
    body.boxes += pair("pelvic", "pelvic", (0.8, p.bottom(0.45) + 0.6, p.snout + 0.45 * 24), (0, 2, 2), (14, 18, 0), "oval")
    male = Group("male", (0, 0, 0))
    male.boxes.append(box("gonopodium", "anal", (0, p.bottom(0.55) + 1.2, p.snout + 0.55 * 24 + 2), (0, 1, 5), (30, 0, 0), fin="rod"))
    male.boxes.append(box("caudal_m", "tail", (0, -6.0, p.ped - 1 + 6), (0, 14, 12), fin="fan"))
    male.boxes.append(box("dorsal_m", "dorsal", (0, p.top(0.55) - 2.5, p.snout + 0.5 * 24 + 4), (0, 5, 8), (-14, 0, 0), fin="flag"))
    female = Group("female", (0, 0, 0))
    female.boxes.append(box("anal_f", "anal", (0, p.bottom(0.62) + 1.0, p.snout + 0.6 * 24 + 2), (0, 3, 4), (8, 0, 0), fin="anal"))
    female.boxes.append(box("caudal_f", "tail", (0, -6.0, p.ped - 1 + 3.5), (0, 8, 7), fin="round"))
    female.boxes.append(box("dorsal_f", "dorsal", (0, p.top(0.5) - 1.5, p.snout + 0.48 * 24 + 2), (0, 3, 4), (-10, 0, 0), fin="tri"))
    tail = Group("tail", (0, -6.0, p.ped - 1))
    # the caudal fins sit in the sex groups; this empty group keeps the animation code uniform
    return p, [body, male, female, tail] + pectorals(p, 0.22, 4, 2)


def platy():
    p = fusiform_plan("platy", depth=11.0, width=5.0, mid=7.0, peak=0.42, head=0.5, ped_frac=0.38)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.12, up=0.3)
    body.boxes += pair("pelvic", "pelvic", (0.9, p.bottom(0.45) + 0.6, p.snout + 0.45 * 24), (0, 2, 3), (14, 18, 0), "oval")
    male = Group("male", (0, 0, 0))
    male.boxes.append(box("gonopodium", "anal", (0, p.bottom(0.55) + 1.2, p.snout + 0.55 * 24 + 2), (0, 1, 5), (30, 0, 0), fin="rod"))
    female = Group("female", (0, 0, 0))
    female.boxes.append(box("anal_f", "anal", (0, p.bottom(0.62) + 1.2, p.snout + 0.6 * 24 + 2), (0, 3, 4), (8, 0, 0), fin="anal"))
    return p, [body, male, female, caudal(p, 8, 12, "round"), dorsal_fin(p, 0.42, 7, 5, "tri", lean=-6)] + pectorals(p, 0.24, 4, 3)


def gourami():
    p = fusiform_plan("gourami", depth=14.0, width=4.0, mid=9.5, peak=0.45, head=0.45, ped_frac=0.3, snout=-14, ped=8)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.12, up=0.33)
    body.boxes.append(anal_fin(p, 0.42, 13, 5, "long_anal"))
    fil = Group("filaments", (0, p.bottom(0.3) + 0.5, p.snout + 0.3 * 22))
    fil.boxes += pair("filament", "filament", (0.6, p.bottom(0.3) + 3.5, p.snout + 0.3 * 22 + 4.5), (1, 1, 11), (40, 8, 0), None)
    return p, [body, fil, caudal(p, 8, 12, "round"), dorsal_fin(p, 0.38, 13, 4, "long", lean=-4)] + pectorals(p, 0.24, 4, 3)


def clown():
    """M20 ocellaris clownfish: a stout oval body, rounded fins, a notched (two-part) dorsal fin."""
    p = fusiform_plan("clown", depth=10.0, width=5.0, mid=6.5, peak=0.4, head=0.6, ped_frac=0.45)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.13, up=0.3)
    body.boxes.append(anal_fin(p, 0.62, 5, 4, "oval"))
    body.boxes += pair("pelvic", "pelvic", (0.9, p.bottom(0.42) + 0.6, p.snout + 0.42 * 24), (0, 3, 3), (14, 18, 0), "oval")
    soft = dorsal_fin(p, 0.56, 6, 4, "oval", name="dorsal2", lean=-4)
    return p, [body, caudal(p, 7, 10, "round"), dorsal_fin(p, 0.3, 6, 4, "tri", lean=-6), soft] + pectorals(p, 0.24, 4, 3)


def tang():
    """M20 yellow tang: a tall, flat disc with a pointed snout and long dorsal and anal fins."""
    p = fusiform_plan("tang", depth=18.0, width=3.5, mid=9.5, peak=0.45, head=0.45, ped_frac=0.2, snout=-14, ped=8)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.2, up=0.32)
    body.boxes.append(box("snout", "head", (0, -9.0, -15.0), (2, 2, 2)))
    body.boxes.append(anal_fin(p, 0.42, 11, 5, "long_anal"))
    body.boxes.append(box("spine", "spine", (0, -9.5, p.ped - 2.5), (1, 1, 2)))
    return p, [body, caudal(p, 6, 11, "fork"), dorsal_fin(p, 0.28, 15, 6, "long", lean=-2)] + pectorals(p, 0.28, 4, 3)


def goby():
    """M20 watchman goby: a long bottom fish with eyes on top, a tall first dorsal fin and a rounded tail."""
    p = Plan("goby", -16, 10, 6.0, 5.0, 0.85, 0.5, 0.3, flat_belly=True, belly=0.0, nseg=9)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.1, up=0.0, size=2, inset=0.9)
    body.boxes.append(anal_fin(p, 0.6, 6, 3, "oval"))
    body.boxes.append(box("pelvic_disc", "pelvic", (0, -0.4, p.snout + 0.3 * 26), (3, 0, 3), fin="pec_flat"))
    second = dorsal_fin(p, 0.55, 8, 4, "long", name="dorsal2", lean=-4)
    return p, [body, caudal(p, 7, 8, "round"), dorsal_fin(p, 0.28, 5, 7, "sail", lean=-10), second] + pectorals(p, 0.22, 4, 3, up=0.75)


def damsel():
    """M20 damselfish and chromis: a compact oval body, long dorsal fin, forked tail."""
    p = fusiform_plan("damsel", depth=11.0, width=4.0, mid=7.0, peak=0.4, head=0.55, ped_frac=0.32)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.12, up=0.3)
    body.boxes.append(anal_fin(p, 0.58, 7, 4, "anal"))
    body.boxes += pair("pelvic", "pelvic", (0.8, p.bottom(0.42) + 0.6, p.snout + 0.42 * 24), (0, 3, 3), (14, 18, 0), "oval")
    return p, [body, caudal(p, 8, 12, "fork"), dorsal_fin(p, 0.3, 11, 5, "long", lean=-4)] + pectorals(p, 0.24, 4, 3)


PLANS = {
    "tetra": tetra, "rasbora": rasbora, "cory": cory, "pleco": pleco, "oto": oto, "kuhli": kuhli, "loach": loach,
    "livebearer": livebearer, "platy": platy, "gourami": gourami,
    "clown": clown, "tang": tang, "goby": goby, "damsel": damsel,
}


# ---------------------------------------------------------------------------------------------------------- fin outlines

def fin_alpha(shape, a, b):
    """a: 0..1 along the fin from its base (front edge for dorsal/anal, attachment for the tail), b: -1..1 or 0..1."""
    if shape is None:
        return True
    s = b
    if shape == "fork":
        return abs(s) <= 0.3 + 0.7 * a ** 0.7 and a <= 0.55 + 0.45 * abs(s) ** 0.9
    if shape == "lyre":
        return abs(s) <= 0.35 + 0.65 * a ** 0.6 and a <= 0.7 + 0.3 * abs(s) ** 1.5
    if shape == "round":
        return abs(s) <= 0.4 + 0.6 * a ** 0.5 and a <= 1.0 - 0.3 * s * s
    if shape == "fan":
        return abs(s) <= 0.18 + 0.82 * a and a <= 1.0 - 0.08 * s * s
    if shape == "oval" or shape == "round_small" or shape == "pec_flat":
        return (2 * a - 1) ** 2 + s * s <= 1.0
    if shape == "rod":
        return abs(s) <= 1
    h = (s + 1) / 2  # 0 = base, 1 = tip (dorsal/anal: base at the body)
    if shape == "tri":
        return h <= 1.05 - 0.85 * a
    if shape == "sail":
        return h <= 1.0 - 0.55 * a ** 1.4
    if shape == "flag":
        return h <= 0.35 + 0.65 * a and a <= 1.0
    if shape == "long":
        return h <= 0.55 + 0.45 * a - 0.25 * a * a
    if shape == "anal":
        return h <= 1.05 - 0.8 * a
    if shape == "long_anal":
        return h <= 0.45 + 0.55 * a
    return True


# ---------------------------------------------------------------------------------------------------------- colours

def C(h):
    return hex_f(h)


def mix(a, b, t):
    return a * (1 - t) + b * t


def band(x, c, half, soft=0.15):
    return 1 - smooth(half, half + soft, abs(x - c))


class Look:
    """Colours of one texture: body(t, v, x, side), fin(part, a, b, t, v), eye."""

    def __init__(self, body, fins=None, eye="#141414", ring="#c0c0c0", belly=None):
        self.body_fn = body
        self.fin_fn = fins or (lambda part, a, b, t, v, z, y: C("#d8d8d0"))
        self.eye = C(eye)
        self.ring = C(ring)
        self.belly = belly


def clear(col="#d8d8d0"):
    return lambda part, a, b, t, v, z, y: C(col)


def looks(species, variety, female):
    sp, var = species, variety
    if sp == "neon_tetra" or sp == "cardinal_tetra":
        cardinal = sp == "cardinal_tetra"

        def body(t, v, x, z, y):
            if v < 0.32:
                return mix(C("#6e6448"), C("#8a7c58"), v / 0.32)
            if v < 0.47 and 0.06 < t < 0.92:
                return mix(C("#30a8ff"), C("#5ad0ff"), (v - 0.32) / 0.15)
            red_from = 0.05 if cardinal else 0.48
            if t > red_from and v < 0.92 and t < 0.98:
                return C("#e02a24") if v < 0.85 else C("#f04a3a")
            return C("#e8e8e4")
        return Look(body, clear("#dcdcd4"))
    if sp == "green_neon_tetra":
        # M21.5: a green (wild) or blue stripe over almost the whole body, a pale belly, only a trace of red at the rear
        blue = var == "blue"

        def body(t, v, x, z, y):
            if v < 0.30:
                return mix(C("#6a6650"), C("#868060"), v / 0.30)
            if v < 0.50 and 0.04 < t < 0.97:
                lo, hi = ("#2a72f0", "#58a8ff") if blue else ("#22c8a8", "#5af0d0")
                return mix(C(lo), C(hi), (v - 0.30) / 0.20)
            if t > 0.72 and 0.78 < v < 0.9 and t < 0.95:
                return C("#c84a3a")
            return C("#ecece6")
        return Look(body, clear("#dcdcd4"))
    if sp == "ember_tetra":
        def body(t, v, x, z, y):
            base = mix(C("#f07a2a"), C("#ff9a4a"), v)
            return mix(base, C("#c44a1a"), 0.4 * (v < 0.25))
        return Look(body, lambda part, a, b, t, v, z, y: C("#f08040") if part != "pectoral" else C("#e8c0a0"))
    if sp == "glowlight_tetra":
        def body(t, v, x, z, y):
            if 0.38 < v < 0.52 and t > 0.04:
                return mix(C("#ff6a2a"), C("#ffa040"), abs(v - 0.45) / 0.07)
            return mix(C("#b8b4a4"), C("#e4e2d8"), v)

        def fins(part, a, b, t, v, z, y):
            if part == "dorsal" and a < 0.4:
                return C("#ff5030")
            if part in ("dorsal", "anal", "pelvic") and (b > 0.6 or (part == "pelvic" and a > 0.6)):
                return C("#f8f8f8")
            return C("#cfcabc")
        return Look(body, fins)
    if sp == "harlequin_rasbora":
        def body(t, v, x, z, y):
            # the black wedge: from below the dorsal fin, narrowing to the tail base
            if 0.42 < t < 0.98:
                k = (t - 0.42) / 0.56
                top, bot = 0.18 + 0.32 * k, 0.88 - 0.38 * k
                if top < v < bot:
                    return C("#1a1418")
            if v < 0.25:
                return C("#b06a3a")
            return mix(C("#e89a5a"), C("#f4c890"), v)
        return Look(body, lambda part, a, b, t, v, z, y: C("#e85a3a") if part in ("tail", "dorsal") else C("#e8c8b0"))
    if sp == "mosquito_rasbora":
        def body(t, v, x, z, y):
            if 0.45 < v < 0.58 and t > 0.35:
                return C("#1e1414")
            return mix(C("#d8401e"), C("#f07a4a"), v)

        def fins(part, a, b, t, v, z, y):
            if part in ("dorsal", "anal") and a < 0.25:
                return C("#1a1010")
            return C("#e8502a") if part in ("dorsal", "anal", "tail") else C("#e8c0a8")
        return Look(body, fins)
    if sp == "pygmy_cory":
        def body(t, v, x, z, y):
            if 0.42 < v < 0.55 and t > 0.3:
                return C("#202020")
            if t > 0.92 and 0.3 < v < 0.7:
                return C("#202020")
            return mix(C("#8a8a86"), C("#e6e6e2"), v)
        return Look(body, clear("#d4d4cc"))
    if sp == "panda_cory":
        def body(t, v, x, z, y):
            if 0.08 < t < 0.24 and v < 0.75:
                return C("#141414")  # the eye mask
            if t > 0.86 and 0.15 < v < 0.75:
                return C("#141414")  # spot at the tail base
            return mix(C("#e8dcc8"), C("#f6ecdc"), v)

        def fins(part, a, b, t, v, z, y):
            if part == "dorsal":
                return C("#181818")
            return C("#e8e0d4")
        return Look(body, fins)
    if sp == "sterbai_cory":
        rnd = np.random.default_rng(301)
        spots = rnd.random((64, 64))

        def body(t, v, x, z, y):
            k = spots[int(abs(z) * 3.7) % 64, int((y + 20) * 3.3) % 64]
            if t < 0.3 and v < 0.6:
                return C("#d8d8cc") if k > 0.62 else C("#3a3430")
            if v > 0.9:
                return C("#e8d8b0")
            return C("#f0ece0") if k > 0.72 else C("#3e3834")

        def fins(part, a, b, t, v, z, y):
            if part == "pectoral" or part == "pelvic":
                return C("#f0902a")
            k = spots[int(z * 2.1) % 64, int((y + 20) * 2.3) % 64]
            return C("#f4f0e8") if k > 0.6 else C("#4a4440")
        return Look(body, fins)
    if sp == "green_gold_cory":
        def body(t, v, x, z, y):
            if 0.4 < v < 0.56 and t > 0.25:
                return C("#1c2e1a")
            if v > 0.85:
                return C("#e4d4a8")
            return mix(C("#5c7a3a"), C("#b8a04a"), v)
        return Look(body, lambda part, a, b, t, v, z, y: C("#f0a830") if part in ("pectoral", "pelvic", "anal") else C("#d8c890"))
    if sp == "leopard_frog_pleco":
        def body(t, v, x, z, y):
            k = math.sin(z * 0.8) + 0.5 * math.sin(x * 1.3 + z * 0.4)
            if v > 0.96:
                return C("#c8b080")
            return C("#f0b838") if k > 0.95 else C("#2a2420")

        def fins(part, a, b, t, v, z, y):
            k = math.sin((a * 7 + b * 2) * 1.6)
            return C("#f0b838") if k > 0.2 else C("#2a2420")
        return Look(body, fins)
    if sp == "zebra_pleco":
        def body(t, v, x, z, y):
            k = math.sin((y * 1.25 + z * 0.35 + 0.5 * math.sin(z * 0.6)) * 1.3)
            if v > 0.96:
                return C("#e8e8e8")
            return C("#141414") if k > 0.25 else C("#f4f4f4")

        def fins(part, a, b, t, v, z, y):
            k = math.sin((a * 5 - abs(b) * 3) * 2.0)
            return C("#141414") if k > 0.3 else C("#f0f0f0")
        return Look(body, fins)
    if sp == "bristlenose_pleco":
        if var == "albino":
            base, spot, eye = C("#f0dca0"), C("#fff4d8"), "#d02020"
        elif var == "super_red":
            base, spot, eye = C("#e05a28"), C("#f8a060"), "#401010"
        else:
            base, spot, eye = C("#4a4038"), C("#b8ac90"), "#101010"
        rnd = np.random.default_rng(303)
        spots = rnd.random((64, 64))

        def body(t, v, x, z, y):
            k = spots[int(abs(z) * 3.1 + abs(x) * 1.7) % 64, int((y + 20) * 2.7) % 64]
            return spot if k > 0.82 else base
        return Look(body, lambda part, a, b, t, v, z, y: mix(base, spot, 0.35 if part != "bristle" else 0.6), eye=eye,
                    ring="#806040" if var == "albino" else "#606060")
    if sp == "otocinclus":
        def body(t, v, x, z, y):
            if 0.42 < v < 0.58:
                return C("#1e1a16")
            if t > 0.9 and 0.3 < v < 0.7:
                return C("#1e1a16")
            if v < 0.42:
                return C("#8a7a5a")
            return C("#ece6d4")
        return Look(body, clear("#d4ccb8"))
    if sp == "kuhli_loach":
        def body(t, v, x, z, y):
            k = math.sin(z * 0.95 + 0.4)
            if v > 0.82:
                return C("#f0d8b0")
            return C("#2e2018") if k > 0.15 and v < 0.85 else C("#f0a048")
        return Look(body, clear("#e8c090"))
    if sp == "clown_loach":
        def body(t, v, x, z, y):
            for c, half in ((0.14, 0.055), (0.44, 0.075), (0.8, 0.06)):
                lean = (v - 0.5) * 0.06
                if abs(t - c - lean) < half:
                    return C("#141010")
            return mix(C("#f08c20"), C("#ffc048"), v)

        def fins(part, a, b, t, v, z, y):
            if part == "tail" and abs(b) > 0.45:
                return C("#e02818")
            if part in ("pelvic", "pectoral", "anal"):
                return C("#e83020")
            return C("#f0a040")
        return Look(body, fins)
    if sp in ("guppy", "endler_guppy"):
        return livebearer_look(sp, var, female)
    if sp == "platy":
        return platy_look(var, female)
    if sp == "dwarf_gourami":
        return gourami_look(var, female)
    if sp == "ocellaris_clownfish":
        black = var == "black"

        def body(t, v, x, z, y):
            for c, half in ((0.2, 0.05), (0.5, 0.06), (0.88, 0.035)):
                d = abs(t - c - (v - 0.5) * 0.05)
                if d < half:
                    return C("#f8f8f4")
                if d < half + 0.025:
                    return C("#141414")
            if black:
                return C("#e8701e") if t < 0.17 else C("#1a1616")
            return mix(C("#f06a10"), C("#ff8a2a"), v * 0.6)

        def fins(part, a, b, t, v, z, y):
            edge = (part in ("tail", "dorsal", "dorsal2", "anal", "pelvic") and (abs(b) > 0.78 if part == "tail" else (b + 1) / 2 > 0.8 or a > 0.85))
            if edge:
                return C("#141414")
            return C("#1e1a1a") if black else C("#f07a1e")
        return Look(body, fins)
    if sp == "yellow_tang":
        def body(t, v, x, z, y):
            if t > 0.86 and 0.42 < v < 0.58:
                return C("#f8f8f0")  # the white scalpel at the tail base
            return mix(C("#f6d20e"), C("#ffe63a"), v * 0.5)
        return Look(body, lambda part, a, b, t, v, z, y: C("#f6d414") if part != "pectoral" else C("#f8e870"))
    if sp == "yellow_watchman_goby":
        rnd = np.random.default_rng(2005)
        spots = rnd.random((64, 64))

        def body(t, v, x, z, y):
            k = spots[int(abs(z) * 2.3 + abs(x) * 1.1) % 64, int((y + 20) * 2.9) % 64]
            if k > 0.86 and t < 0.92:
                return C("#5a8ee0")
            return mix(C("#e8c21e"), C("#f6dc5a"), v)

        def fins(part, a, b, t, v, z, y):
            k = spots[int(a * 31) % 64, int((b + 1) * 17) % 64]
            if part in ("dorsal", "dorsal2") and k > 0.8:
                return C("#5a8ee0")
            return C("#f0cc2a")
        return Look(body, fins)
    if sp == "green_chromis":
        def body(t, v, x, z, y):
            return mix(C("#5ec8a0"), C("#9ae8d0"), v)
        return Look(body, clear("#b8e8dc"))
    if sp == "yellowtail_damsel":
        def body(t, v, x, z, y):
            if t > 0.78:
                return C("#f4d018")
            return mix(C("#1c3ad8"), C("#3a6af0"), v)

        def fins(part, a, b, t, v, z, y):
            if part == "tail" or (part in ("dorsal", "anal") and a > 0.6):
                return C("#f4d018")
            return C("#2a4ae0") if part != "pectoral" else C("#c8d0e8")
        return Look(body, fins)
    return Look(lambda t, v, x, z, y: C("#a0a0a0"))


def livebearer_look(sp, var, female):
    rnd = np.random.default_rng(311 + len(var))
    spots = rnd.random((64, 64))
    grey = C("#9a9a80")
    if female:
        tailc = {"red": "#d84a3a", "blue_grass": "#6a8ac0", "green_cobra": "#a8b060", "tuxedo": "#c86040",
                 "tiger": "#b0a070", "black_bar": "#b0a070"}.get(var, "#c8c4b0")

        def body(t, v, x, z, y):
            if var == "tuxedo" and t > 0.5 and v < 0.8:
                return C("#3a3634")
            if 0.42 < t < 0.6 and v > 0.62:
                return C("#5a5040")  # the gravid spot
            return mix(C("#8a8a70"), C("#d8d8c8"), v)
        return Look(body, lambda part, a, b, t, v, z, y: C(tailc) if part == "tail" else C("#d0ccbc"))

    def tail_pattern(part, a, b, t, v, z, y):
        k = spots[int(z * 3) % 64, int((y + 30) * 3) % 64]
        if var == "red":
            return C("#e02a20") if k > 0.15 else C("#a01810")
        if var == "blue_grass":
            return C("#141830") if k > 0.82 else C("#3a70e0")
        if var == "green_cobra":
            return C("#203010") if math.sin(z * 1.4 + y * 1.1) > 0.3 else C("#c8d040")
        if var == "tuxedo":
            return C("#f04020") if k > 0.1 else C("#c02010")
        if sp == "endler_guppy":
            return C("#f07a1a") if abs(b) > 0.6 else (C("#202020") if k > 0.7 else C("#d8d0b0"))
        return C("#f0802a") if k > 0.75 else (C("#202020") if k < 0.15 else C("#c8b890"))

    def body(t, v, x, z, y):
        k = spots[int(abs(z) * 2.3) % 64, int((y + 30) * 2.9) % 64]
        if var == "tuxedo" and t > 0.45 and v < 0.85:
            return C("#141414")
        if var == "red":
            return mix(C("#e8c8b0"), C("#f0dcc8"), v) if t < 0.6 else C("#e04a30")
        if var == "blue_grass":
            return mix(C("#4a78c0"), C("#a8c0e0"), v)
        if var == "green_cobra":
            return C("#2a3418") if math.sin(z * 1.3 + y * 1.4) > 0.35 else C("#b8c048")
        if sp == "endler_guppy":
            if var == "tiger" and math.sin(z * 1.3) > 0.4 and v < 0.8:
                return C("#141414")
            if var == "black_bar" and 0.4 < t < 0.55 and v < 0.85:
                return C("#141414")
            if 0.3 < t < 0.6 and v < 0.6:
                return C("#40e060") if k > 0.5 else C("#f06a1a")
            return C("#f07020") if k > 0.8 else mix(grey, C("#d8d8c8"), v)
        if k > 0.85:
            return C("#f08030")
        if k < 0.1:
            return C("#202020")
        return mix(grey, C("#d8d8c8"), v)

    return Look(body, lambda part, a, b, t, v, z, y: tail_pattern(part, a, b, t, v, z, y) if part in ("tail", "dorsal") else C("#d8d0c0"))


def platy_look(var, female):
    def body(t, v, x, z, y):
        if var == "red":
            return mix(C("#d82a1a"), C("#f05a3a"), v)
        if var == "sunset":
            return mix(C("#f0c030"), C("#e83a1a"), smooth(0.2, 0.9, t))
        if var == "mickey_mouse":
            # the mickey-mouse spot at the tail base: one big, two small circles
            zz, yy = (t - 0.95) * 24, (v - 0.5) * 10
            if zz * zz + yy * yy < 4.5 or (zz + 1.5) ** 2 + (yy + 2.6) ** 2 < 1.3 or (zz + 1.5) ** 2 + (yy - 2.6) ** 2 < 1.3:
                return C("#141414")
            return mix(C("#f0a830"), C("#f8d070"), v)
        if var == "blue":
            return mix(C("#5878b8"), C("#b8c8e0"), v)
        k = math.sin(z * 2.1 + y * 1.7) * math.sin(z * 0.7 - y * 2.3)
        return C("#3a3a28") if k > 0.55 else mix(C("#7a7a58"), C("#c8c4a8"), v)

    def fins(part, a, b, t, v, z, y):
        if var == "red":
            return C("#e04030")
        if var == "sunset":
            return C("#e85020") if part == "tail" else C("#f0b040")
        if var == "mickey_mouse":
            return C("#f0c060")
        if var == "blue":
            return C("#a8b8d0")
        return C("#c8c4a8")
    return Look(body, fins)


def gourami_look(var, female):
    def body(t, v, x, z, y):
        if female:
            if math.sin(z * 1.0 + y * 0.6) > 0.7 and v < 0.85:
                return C("#8898a0")
            return mix(C("#a8b0b0"), C("#dcdcd4"), v)
        if var == "powder_blue":
            return C("#c83a28") if math.sin(z * 1.1 - y * 0.8) > 0.88 else mix(C("#2a78d0"), C("#60a8f0"), v)
        if var == "flame":
            return mix(C("#e83018"), C("#f87a30"), v) if v > 0.15 else C("#2a50a0")
        k = math.sin(z * 1.05 - y * 0.85)
        return C("#40b0e8") if k > 0.45 else mix(C("#d84020"), C("#f06a30"), v)

    def fins(part, a, b, t, v, z, y):
        if female:
            return C("#c8ccc8")
        if part == "filament":
            return C("#f08040")
        if var == "flame" and part == "dorsal":
            return C("#3060c0")
        if var == "powder_blue":
            return C("#4a90e0")
        return C("#e85030") if math.sin(z * 1.4 + y) > 0 else C("#3a98d8")
    return Look(body, fins)


# ---------------------------------------------------------------------------------------------------------- UV + painting

def footprint(b: Box):
    w, h, d = b.size
    return 2 * (d + w), d + h


def pack(groups):
    boxes = [b for g in groups for b in g.boxes]
    order = sorted(boxes, key=lambda b: (-footprint(b)[1], -footprint(b)[0]))
    x = y = row = 0
    for b in order:
        fw, fh = footprint(b)
        if x + fw > TEX_W:
            x, y, row = 0, y + row, 0
        if y + fh > TEX_H:
            raise ValueError("texture too small")
        b.u, b.v = x, y
        x += fw + 0
        row = max(row, fh)


def rot_matrix(rot):
    rx, ry, rz = (math.radians(r) for r in rot)
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


def faces(b: Box):
    """(face, u0, v0, width, height, local(a, b) -> point relative to the box centre) per the vanilla Cube mapping."""
    w, h, d = b.size
    x0, y0, z0 = -w / 2, -h / 2, -d / 2
    x1, y1, z1 = w / 2, h / 2, d / 2
    u, v = b.u, b.v
    return [
        ("top", u + d, v, w, d, lambda a, c: (x0 + a, y0, z1 - c)),
        ("bottom", u + d + w, v, w, d, lambda a, c: (x0 + a, y1, z1 - c)),
        ("west", u, v + d, d, h, lambda a, c: (x0, y0 + c, z1 - a)),
        ("north", u + d, v + d, w, h, lambda a, c: (x0 + a, y0 + c, z0)),
        ("east", u + d + w, v + d, d, h, lambda a, c: (x1, y0 + c, z0 + a)),
        ("south", u + 2 * d + w, v + d, w, h, lambda a, c: (x1 - a, y0 + c, z1)),
    ]


def fin_coords(b: Box, lp):
    """Local fin coordinates of a point: a along the fin (0..1 from its base), s across (-1..1 or base..tip)."""
    w, h, d = b.size
    lx, ly, lz = lp
    if b.fin == "pec_flat":
        return lx / max(1, w) + 0.5, 2 * lz / max(1, d)
    a = lz / max(1, d) + 0.5
    s = ly / max(1, h) * 2
    if b.part in ("dorsal",) or b.fin in ("tri", "sail", "flag", "long"):
        # base at the bottom of the plane (+y), tip at the top (-y)
        return a, -s
    if b.part == "anal" or b.fin in ("anal", "long_anal"):
        return a, s
    if b.part == "pectoral":
        return a, s
    return a, s


def paint(plan: Plan, groups, look: Look, seed: int) -> np.ndarray:
    rng = np.random.default_rng(seed)
    img = np.zeros((TEX_H, TEX_W, 4))
    for g in groups:
        for b in g.boxes:
            R = rot_matrix(b.rot)
            c = np.array(b.center)
            for face, u0, v0, fw, fh, fn in faces(b):
                for j in range(int(math.ceil(fh))):
                    for i in range(int(math.ceil(fw))):
                        uu, vv = int(u0 + i), int(v0 + j)
                        if not (0 <= uu < TEX_W and 0 <= vv < TEX_H):
                            continue
                        lp = fn(i + 0.5, j + 0.5)
                        p = c + R @ np.array(lp)
                        x, y, z = p
                        t = max(0.0, min(1.0, plan.t_of(z)))
                        vv_ = plan.v_of(y, z)
                        if b.fin is not None or b.part in ("tail", "dorsal", "anal", "pectoral", "pelvic", "adipose"):
                            a, s = fin_coords(b, lp)
                            if not fin_alpha(b.fin, max(0.0, min(1.0, a)), max(-1.0, min(1.0, s))):
                                continue
                            col = look.fin_fn(b.part, a, s, t, vv_, z, y)
                            # fin rays: faint darker lines
                            if b.part in ("tail", "dorsal", "anal") and int((s + 1) * 6) % 2 == 0:
                                col = col * 0.92
                        elif b.part == "eye":
                            # dark eye with one light glint at the top front
                            col = look.ring if (face in ("east", "west") and lp[1] < 0 and lp[2] < 0 and b.size[1] > 1) else look.eye
                        elif b.part in ("barbel", "mouth", "bristle"):
                            col = look.fin_fn(b.part, 0.5, 0.0, t, vv_, z, y) * 0.9
                        else:
                            col = look.body_fn(t, vv_, x, z, y)
                            if face == "bottom":
                                col = mix(col, C("#f0f0ea"), 0.35)
                        img[vv, uu, :3] = np.clip(col * (0.96 + 0.08 * rng.random()), 0, 1)
                        img[vv, uu, 3] = 1.0
    return img


# ---------------------------------------------------------------------------------------------------------- Java

def f(v):
    s = f"{v:.4f}".rstrip("0").rstrip(".")
    if s in ("-0", ""):
        s = "0"
    return s + "F"


def rad(deg):
    return f(math.radians(deg))


def world_pivot(groups, name):
    g = next(x for x in groups if x.name == name)
    return g.pivot


def java_layer(name, groups):
    lines = [f"    public static LayerDefinition create{name.capitalize()}Layer() {{",
             "        MeshDefinition mesh = new MeshDefinition();",
             "        PartDefinition root = mesh.getRoot();",
             "        PartDefinition fish = root.addOrReplaceChild(\"fish\", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));"]
    for g in groups:
        px, py, pz = g.pivot
        if g.parent:
            qx, qy, qz = world_pivot(groups, g.parent)
            parent = "g_" + g.parent
        else:
            qx = qy = qz = 0.0
            parent = "fish"
        lines.append(f"        PartDefinition g_{g.name} = {parent}.addOrReplaceChild(\"{g.name}\", CubeListBuilder.create(), "
                     f"PartPose.offset({f(px - qx)}, {f(py - qy)}, {f(pz - qz)}));")
        for b in g.boxes:
            w, h, d = b.size
            cx, cy, cz = b.center
            rx, ry, rz = b.rot
            lines.append(f"        g_{g.name}.addOrReplaceChild(\"{b.name}\", CubeListBuilder.create().texOffs({b.u}, {b.v})"
                         f".addBox({f(-w / 2)}, {f(-h / 2)}, {f(-d / 2)}, {f(w)}, {f(h)}, {f(d)}), "
                         f"PartPose.offsetAndRotation({f(cx - px)}, {f(cy - py)}, {f(cz - pz)}, {rad(rx)}, {rad(ry)}, {rad(rz)}));")
    lines.append(f"        return LayerDefinition.create(mesh, {TEX_W}, {TEX_H});")
    lines.append("    }")
    return "\n".join(lines)


def write_java(models):
    body = ["// GENERATED by tools/assetgen/tankmates.py - do not edit by hand, edit tankmates.py and rerun it",
            "package org.overengineer.bettacraft.client.model;",
            "",
            "import net.minecraft.client.model.geom.PartPose;",
            "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
            "import net.minecraft.client.model.geom.builders.LayerDefinition;",
            "import net.minecraft.client.model.geom.builders.MeshDefinition;",
            "import net.minecraft.client.model.geom.builders.PartDefinition;",
            "",
            "/**",
            " * M19: meshes of the tankmate fish, one per body plan (TankmateSpecies.Model): 32 px long (snout -16, tail +16),",
            " * belly at y = 24, a tree of named groups (body, tail, dorsal, pec_r/pec_l, male/female, filaments, seg1..).",
            " * Textures: textures/entity/tankmates/<species>[_variety][_male|_female].png (128x64 box UV).",
            " */",
            "public final class TankmateModelDefinitions {",
            "    /** Nominal length of every model in model pixels (the renderer scales it to the species' length). */",
            "    public static final float LENGTH = 32.0F;",
            "",
            "    private TankmateModelDefinitions() {}",
            "",
            "    public static LayerDefinition create(String model) {",
            "        return switch (model) {"]
    for m in models:
        body.append(f"            case \"{m}\" -> create{m.capitalize()}Layer();")
    body += ["            default -> throw new IllegalArgumentException(model);",
             "        };",
             "    }",
             ""]
    for m, groups in models.items():
        body.append(java_layer(m, groups))
        body.append("")
    body.append("}")
    path = JAVA_SRC / "client" / "model" / "TankmateModelDefinitions.java"
    path.write_text("\n".join(body) + "\n", encoding="utf-8", newline="\n")
    return path


# ---------------------------------------------------------------------------------------------------------- species table (mirrors TankmateSpecies)

SPECIES = [
    ("neon_tetra", "tetra", ["wild"], False), ("cardinal_tetra", "tetra", ["wild"], False), ("ember_tetra", "tetra", ["wild"], False),
    ("glowlight_tetra", "tetra", ["wild"], False), ("harlequin_rasbora", "rasbora", ["wild"], False),
    ("mosquito_rasbora", "rasbora", ["wild"], False), ("pygmy_cory", "cory", ["wild"], False), ("panda_cory", "cory", ["wild"], False),
    ("sterbai_cory", "cory", ["wild"], False), ("green_gold_cory", "cory", ["wild"], False),
    ("leopard_frog_pleco", "pleco", ["wild"], False), ("zebra_pleco", "pleco", ["wild"], False),
    ("bristlenose_pleco", "pleco", ["wild", "albino", "super_red"], False), ("otocinclus", "oto", ["wild"], False),
    ("kuhli_loach", "kuhli", ["wild"], False), ("clown_loach", "loach", ["wild"], False),
    ("guppy", "livebearer", ["wild", "red", "blue_grass", "green_cobra", "tuxedo"], True),
    ("endler_guppy", "livebearer", ["wild", "tiger", "black_bar"], True),
    ("platy", "platy", ["wild", "red", "sunset", "mickey_mouse", "blue"], True),
    ("dwarf_gourami", "gourami", ["wild", "powder_blue", "flame"], True),
    # M20 reef fish
    ("ocellaris_clownfish", "clown", ["wild", "black"], False), ("yellow_tang", "tang", ["wild"], False),
    ("yellow_watchman_goby", "goby", ["wild"], False), ("green_chromis", "damsel", ["wild"], False),
    ("yellowtail_damsel", "damsel", ["wild"], False),
    # M21.5 (at the end: the texture seeds of the others stay)
    ("green_neon_tetra", "tetra", ["wild", "blue"], False),
]


def texture_name(sp, varieties, var, dimorphic, female):
    n = sp
    if len(varieties) > 1:
        n += "_" + var
    if dimorphic:
        n += "_female" if female else "_male"
    return n


# ---------------------------------------------------------------------------------------------------------- blocks, eggs, icons

def cave_block():
    rng = np.random.default_rng(1901)
    img = np.zeros((16, 16, 4))
    base = C("#b0623a")
    for y in range(16):
        for x in range(16):
            n = rng.random()
            col = base * (0.82 + 0.25 * n)
            if (y % 5 == 0 and n < 0.6) or n > 0.97:
                col = col * 0.8
            img[y, x, :3] = np.clip(col, 0, 1)
            img[y, x, 3] = 1
    return img


def mop_block():
    rng = np.random.default_rng(1902)
    img = np.zeros((16, 16, 4))
    for i in range(7):
        x = 1 + i * 2 + int(rng.integers(0, 2))
        top = int(rng.integers(0, 4))
        sway = rng.random() * 2 - 1
        for y in range(top, 16):
            xx = int(round(x + sway * math.sin(y * 0.4)))
            if 0 <= xx < 16:
                g = 0.75 + 0.25 * rng.random()
                img[y, xx, :3] = C("#3e9a3a") * g
                img[y, xx, 3] = 1
    return img


def eggs_tex():
    """32x16: corydoras eggs (left, opaque cream), tetra eggs (middle, small, clear amber), pleco eggs (right, orange)."""
    img = np.zeros((16, 48, 4))
    yy, xx = np.mgrid[0:16, 0:16]
    r = np.hypot(xx - 7.5, yy - 7.5)
    for k, (col, rad) in enumerate((("#efe4c4", 7.5), ("#e8c878", 6.0), ("#f0a030", 7.5))):
        m = r <= rad
        c = C(col)
        shade = np.clip(1.05 - r / rad * 0.35, 0, 1)[..., None]
        img[:, k * 16:(k + 1) * 16, :3] = np.where(m[..., None], c * shade, 0)
        img[:, k * 16:(k + 1) * 16, 3] = m * 1.0
        # a dark embryo dot
        img[6:9, k * 16 + 6:k * 16 + 9, :3] = np.where(m[6:9, 6:9, None], c * 0.55, img[6:9, k * 16 + 6:k * 16 + 9, :3])
    return img


def breeding_box_tex():
    """16x16: see-through plastic with a frame and a slotted floor (drawn translucent)."""
    img = np.zeros((16, 16, 4))
    img[..., :3] = C("#dce8ee")
    img[..., 3] = 0.22
    for i in range(16):
        for j in (0, 15):
            img[j, i, :3] = C("#f4f8fa")
            img[j, i, 3] = 0.9
            img[i, j, :3] = C("#f4f8fa")
            img[i, j, 3] = 0.9
    for i in range(2, 14, 3):
        img[13, i, :3] = C("#90a0a8")
        img[13, i, 3] = 0.6
    return img


def grid_icon(rows, cmap):
    from icons import grid
    return grid(rows, cmap)


K = "#24232a"
BOX_ICON = [
    "................",
    "................",
    "..kkkkkkkkkkkk..",
    "..kgGGGGGGGGgk..",
    "..kG........Gk..",
    "..kG..wwww..Gk..",
    "..kG.wbbbbw.Gk..",
    "..kG.wbbbbw.Gk..",
    "..kGwwwwwwwwGk..",
    "..kGs.s.s.s.Gk..",
    "..kG.s.s.s.sGk..",
    "..kGs.s.s.s.Gk..",
    "..kgGGGGGGGGgk..",
    "..kkkkkkkkkkkk..",
    "................",
    "................",
]
BOX_P = {"k": K, "g": "#e8f0f4", "G": "#b8c8d0", "w": "#5aa8e0", "b": "#8ad0f8", "s": "#2c2c34"}
POWDER = [
    "................",
    "......kkkk......",
    ".....kHHHHk.....",
    ".....kkkkkk.....",
    "....kWWWWWWk....",
    "...kWyyyyyyWk...",
    "...kWyYyyYyWk...",
    "...kWyyyYyyWk...",
    "...kWyYyyyyWk...",
    "...kWyyyyYyWk...",
    "...kWyYyyyyWk...",
    "...kWyyyYyyWk...",
    "...kWWWWWWWWk...",
    "....kkkkkkkk....",
    "................",
    "................",
]
POWDER_P = {"k": K, "H": "#c03030", "W": "#e8eef2", "y": "#d8b870", "Y": "#a87a40"}
WAFER = [
    "................",
    "................",
    "................",
    "....kkkkkkk.....",
    "...kGGgGGGgk....",
    "..kGgGGGGgGGk...",
    "..kGGGdGGGGGk...",
    "..kgGGGGGdGgk...",
    "..kGGdGGGGGGk...",
    "..kGGGGGgGGGk...",
    "...kGgGGGGGk....",
    "....kDDDDDk.....",
    ".....kkkkk......",
    "................",
    "................",
    "................",
]
WAFER_P = {"k": K, "G": "#4e7a2c", "g": "#6e9a40", "d": "#2e4a1a", "D": "#34521e"}
CAVE_ICON = [
    "................",
    "................",
    "................",
    "................",
    "...kkkkkkkkk....",
    "..kHHLLLLLLMk...",
    ".kHLLLLLLLMMDk..",
    ".kLkkkkLLLMMDk..",
    "kLkwwwwkLLMDDk..",
    "kLkwwwwkLMMDDk..",
    "kLkwwwwkMMDDk...",
    "kMkkwwkkMDDk....",
    ".kMMkkMMDDk.....",
    "..kkkkkkkk......",
    "................",
    "................",
]
CAVE_ICON_P = {"k": K, "H": "#d88a5a", "L": "#b0623a", "M": "#8e4c2c", "D": "#6a361e", "w": "#1c1210"}
MOP_ICON = [
    "................",
    "....g..G..g.....",
    "...gG.gG.Gg.g...",
    "...G.gG.gG.gG...",
    "..gG.G.gG.G.Gg..",
    "..G.gG.G.gG.G...",
    "..GgG.gG.G.gGg..",
    "..G.G.G.gG.G.G..",
    "..GgGgG.G.gGgG..",
    "...GGGgGgGGGG...",
    "....GGGGGGGG....",
    ".....kkkkkk.....",
    "....kSSSSSSk....",
    "....kSssssSk....",
    ".....kkkkkk.....",
    "................",
]
MOP_ICON_P = {"k": K, "g": "#6ac060", "G": "#3e9a3a", "S": "#8e8e8e", "s": "#6e6e6e"}


def preview(images):
    from PIL import Image
    tiles = [Image.fromarray(np.clip(np.rint(im * 255), 0, 255).astype(np.uint8), "RGBA") for im in images]
    w = TEX_W * 2
    h = TEX_H * 2
    cols = 5
    rows = (len(tiles) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * w, rows * h), (40, 60, 80, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t.resize((w, h), Image.NEAREST), ((i % cols) * w, (i // cols) * h))
    PREVIEW.mkdir(parents=True, exist_ok=True)
    p = PREVIEW / "tankmates.png"
    sheet.save(p)
    return p


def write_all():
    written = []
    models = {}
    plans = {}
    for name, fn in PLANS.items():
        plan, groups = fn()
        pack(groups)
        models[name] = groups
        plans[name] = plan
    written.append(write_java(models))
    shots = []
    for i, (sp, model, varieties, dimorphic) in enumerate(SPECIES):
        for var in varieties:
            for female in ((False, True) if dimorphic else (False,)):
                name = texture_name(sp, varieties, var, dimorphic, female)
                img = paint(plans[model], models[model], looks(sp, var, female), 1900 + i)
                written.append(save_png(img, TM_TEX / f"{name}.png"))
                shots.append(img)
    written.append(preview(shots))
    written.append(save_png(cave_block(), BLOCK_TEX / "spawning_cave.png"))
    written.append(save_png(mop_block(), BLOCK_TEX / "spawning_mop.png"))
    written.append(save_png(eggs_tex(), TM_TEX / "eggs.png"))
    written.append(save_png(breeding_box_tex(), TM_TEX / "breeding_box.png"))
    written.append(save_png(grid_icon(BOX_ICON, BOX_P), ITEM_TEX / "breeding_box.png"))
    written.append(save_png(grid_icon(POWDER, POWDER_P), ITEM_TEX / "fry_powder.png"))
    written.append(save_png(grid_icon(WAFER, WAFER_P), ITEM_TEX / "algae_wafer.png"))
    written.append(save_png(grid_icon(CAVE_ICON, CAVE_ICON_P), ITEM_TEX / "decor_spawning_cave.png"))
    written.append(save_png(grid_icon(MOP_ICON, MOP_ICON_P), ITEM_TEX / "decor_spawning_mop.png"))
    return written


if __name__ == "__main__":
    for p in write_all():
        print(p)


# ---------------------------------------------------------------------------------------------------------- debug renders

def splats(groups, tex, sub=3):
    """Model points with their texel colour (sub x sub samples per texel), for the debug renders."""
    pts = []
    for g in groups:
        for b in g.boxes:
            R = rot_matrix(b.rot)
            c = np.array(b.center)
            for face, u0, v0, fw, fh, fn in faces(b):
                for j in range(int(math.ceil(fh * sub))):
                    for i in range(int(math.ceil(fw * sub))):
                        uu, vv = int(u0 + i / sub), int(v0 + j / sub)
                        if not (0 <= uu < TEX_W and 0 <= vv < TEX_H) or tex[vv, uu, 3] < 0.5:
                            continue
                        p = c + R @ np.array(fn((i + 0.5) / sub, (j + 0.5) / sub))
                        pts.append((p, tex[vv, uu, :3], g.name))
    return pts


def render_view(groups, tex, view="side", scale=8, hide=()):
    pts = [p for p in splats(groups, tex) if p[2] not in hide]
    W, H = 40 * scale, 24 * scale
    img = np.zeros((H, W, 4))
    img[..., :3] = C("#203040")
    img[..., 3] = 1
    depth = np.full((H, W), -1e9)
    for p, col, _ in pts:
        x, y, z = p
        if view == "side":  # seen from +x (the fish's right side), head to the left
            sx, sy, d = z + 18, y + 22, x
        elif view == "top":
            sx, sy, d = z + 18, x + 12, -y
        else:  # front
            sx, sy, d = x + 17, y + 20, -z
        px, py = int(sx * scale), int(sy * scale)
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                qx, qy = px + dx, py + dy
                if 0 <= qx < W and 0 <= qy < H and d > depth[qy, qx]:
                    depth[qy, qx] = d
                    img[qy, qx, :3] = col
    return img


def render_sheet(names=None):
    """Side and top views of every texture (debug: build/preview/tankmates_3d.png)."""
    from PIL import Image
    models = {}
    for name, fn in PLANS.items():
        plan, groups = fn()
        pack(groups)
        models[name] = (plan, groups)
    tiles = []
    for i, (sp, model, varieties, dimorphic) in enumerate(SPECIES):
        for var in varieties:
            for female in ((False, True) if dimorphic else (False,)):
                name = texture_name(sp, varieties, var, dimorphic, female)
                if names and name not in names:
                    continue
                plan, groups = models[model]
                tex = paint(plan, groups, looks(sp, var, female), 1900 + i)
                hide = ("female",) if not female else ("male",)
                side = render_view(groups, tex, "side", hide=hide)
                top = render_view(groups, tex, "top", hide=hide)
                tiles.append(np.concatenate([side, top], axis=0))
    cols = 4
    th, tw = tiles[0].shape[:2]
    rows = (len(tiles) + cols - 1) // cols
    sheet = np.zeros((rows * th, cols * tw, 4))
    for k, t in enumerate(tiles):
        r, c = divmod(k, cols)
        sheet[r * th:(r + 1) * th, c * tw:(c + 1) * tw] = t
    p = PREVIEW / "tankmates_3d.png"
    PREVIEW.mkdir(parents=True, exist_ok=True)
    Image.fromarray(np.clip(np.rint(sheet * 255), 0, 255).astype(np.uint8), "RGBA").save(p)
    return p
