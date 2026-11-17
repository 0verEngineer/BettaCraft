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


class SeaPlan:
    """M21.8: the upright seahorse: t runs from the top of the head (0) down to the tip of the tail (1), v from the back
    (0, +z) to the belly (1, -z), so the looks can paint rings down the body and a pale belly."""
    name = "seahorse"
    snout, ped = -16, 16

    def t_of(self, z):
        return 0.5  # the looks of the seahorse use y (height) directly

    def v_of(self, y, z):
        return max(0.0, min(1.0, 0.5 - z / 8.0))


def seahorse():
    """M21.8 seahorse: upright, 32 px from the crown (y = -32) down to the curled tail tip (y = 0), the snout pointing to
    -z. Groups: body (head, snout, crown, eyes, neck, trunk), male (brood pouch), dorsal (fin on the back), pec_r/pec_l
    and the tail chain curl1..curl6 (nested; the renderer curls it round the hitching post)."""
    p = SeaPlan()
    body = Group("body", (0, 0, 0))
    body.boxes.append(box("crown", "body", (0, -31.0, 1.2), (2, 2, 2)))
    body.boxes.append(box("head", "head", (0, -28.0, 0.6), (4, 5, 5)))
    body.boxes.append(box("snout", "head", (0, -27.2, -4.0), (2, 2, 5)))
    body.boxes.append(box("eye_r", "eye", (1.6, -28.6, -0.8), (2, 2, 2)))
    body.boxes.append(box("eye_l", "eye", (-1.6, -28.6, -0.8), (2, 2, 2)))
    body.boxes.append(box("neck", "body", (0, -24.0, 1.4), (3, 3, 4)))
    body.boxes.append(box("trunk0", "body", (0, -21.0, 0.2), (4, 3, 6)))
    body.boxes.append(box("trunk1", "body", (0, -18.0, -0.4), (5, 3, 7)))
    body.boxes.append(box("trunk2", "body", (0, -15.0, -0.2), (5, 3, 6)))
    body.boxes.append(box("trunk3", "body", (0, -12.5, 0.4), (4, 2, 5)))
    male = Group("male", (0, -14.2, -2.6))  # pivot at the back of the pouch: it swells forward when full
    male.boxes.append(box("pouch", "body", (0, -14.2, -3.6), (3, 4, 2)))
    dorsal = Group("dorsal", (0, -17.0, 3.0))
    dorsal.boxes.append(box("dorsal", "dorsal", (0, -17.0, 4.5), (0, 4, 3), fin="oval"))
    groups = [body, male, dorsal]
    for side, sx in (("r", 1), ("l", -1)):
        g = Group(f"pec_{side}", (sx * 1.6, -26.0, 2.0))
        g.boxes.append(box(f"pec_{side}", "pectoral", (sx * 1.9, -26.0, 3.0), (0, 2, 2), (0, sx * 35, 0), fin="oval"))
        groups.append(g)
    widths = [(3, 4), (3, 3), (3, 3), (2, 3), (2, 2), (1, 2)]
    parent = None
    for i, (wd, dp) in enumerate(widths):
        y0 = -11.5 + i * 2
        z = 0.6 + i * 0.25
        name = f"curl{i + 1}"
        g = Group(name, (0, y0, z), parent)
        g.boxes.append(box(name, "body", (0, y0 + 1.0, z), (wd, 2, dp)))
        groups.append(g)
        parent = name
    return p, groups


# M29 part a: cichlids

def cichlid():
    """M29 dwarf cichlid (rams): a deep, compressed body, a long dorsal fin with tall dark front rays, long pelvic fins,
    a round tail."""
    p = fusiform_plan("cichlid", depth=12.0, width=4.0, mid=7.5, peak=0.4, head=0.55, ped_frac=0.32, snout=-15, ped=9)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.15, up=0.3)
    body.boxes.append(anal_fin(p, 0.58, 8, 5, "anal"))
    body.boxes += pair("pelvic", "pelvic", (0.9, p.bottom(0.36) + 1.6, p.snout + 0.36 * 24 + 1), (0, 5, 3), (24, 16, 0), "anal")
    front = dorsal_fin(p, 0.2, 3, 7, "tri", name="dorsal2", lean=-4, part="dorsal2")
    return p, [body, caudal(p, 8, 12, "round"), dorsal_fin(p, 0.28, 13, 6, "long", lean=-4), front] + pectorals(p, 0.26, 4, 3)


def apisto():
    """M29 Apistogramma: an elongated dwarf cichlid with a tall, spiky "cockatoo" crest in front of a long dorsal fin and a
    lyre tail."""
    p = fusiform_plan("apisto", depth=9.0, width=4.0, mid=6.0, peak=0.38, head=0.6, ped_frac=0.36, snout=-15, ped=9)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.14, up=0.32)
    body.boxes.append(anal_fin(p, 0.6, 7, 4, "anal"))
    body.boxes += pair("pelvic", "pelvic", (0.9, p.bottom(0.38) + 1.2, p.snout + 0.38 * 24 + 1), (0, 4, 3), (22, 16, 0), "anal")
    return p, [body, caudal(p, 9, 12, "lyre"), dorsal_fin(p, 0.2, 15, 8, "crest", lean=-2)] + pectorals(p, 0.24, 4, 3)


def angel():
    """M29 angelfish: a tall, flat disc with huge swept-back dorsal and anal fins, long pelvic threads and a lyre tail.
    The body sits high so the anal fin ends at y = 0."""
    p = fusiform_plan("angel", depth=15.0, width=3.0, mid=19.5, peak=0.42, head=0.45, ped_frac=0.2, snout=-13, ped=6)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.2, up=0.33)
    body.boxes.append(box("anal", "anal", (0, p.bottom(0.5) + 5.5, p.snout + 0.42 * 19 + 5), (0, 12, 9), (28, 0, 0), fin="tri_down"))
    fil = Group("filaments", (0, p.bottom(0.3) + 0.5, p.snout + 0.3 * 19))
    fil.boxes += pair("filament", "filament", (0.6, p.bottom(0.3) + 5.0, p.snout + 0.3 * 19 + 1.5), (1, 11, 1), (-12, 0, 6), None)
    dorsal = Group("dorsal", (0, p.top(0.4), p.snout + 0.38 * 19))
    dorsal.boxes.append(box("dorsal", "dorsal", (0, p.top(0.45) - 5.5, p.snout + 0.42 * 19 + 5), (0, 12, 9), (-28, 0, 0), fin="tri_up"))
    return p, [body, fil, dorsal, caudal(p, 9, 13, "lyre")] + pectorals(p, 0.26, 4, 3)


def discus():
    """M29 discus: a round, flat disc with a low fringe of dorsal and anal fins all round its back and belly."""
    p = fusiform_plan("discus", depth=19.0, width=3.0, mid=13.5, peak=0.5, head=0.25, ped_frac=0.16, snout=-13, ped=7)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.16, up=0.36)
    body.boxes.append(anal_fin(p, 0.38, 11, 4, "long_anal"))
    body.boxes += pair("pelvic", "pelvic", (0.8, p.bottom(0.3) + 1.2, p.snout + 0.3 * 20 + 1), (0, 4, 2), (24, 14, 0), "anal")
    return p, [body, caudal(p, 6, 10, "round"), dorsal_fin(p, 0.24, 15, 4, "long", lean=0)] + pectorals(p, 0.28, 4, 3)


# M29 part b: puffers, barbs, the garra

def puffer():
    """M29 puffers: a short, round, wide body with a blunt head and big eyes high up, small dorsal and anal fins set far
    back opposite each other, a small round tail and fanning pectorals."""
    p = fusiform_plan("puffer", depth=11.0, width=8.0, mid=6.5, peak=0.35, head=0.72, ped_frac=0.3, snout=-13, ped=6)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.16, up=0.18, size=3, inset=0.6)
    body.boxes.append(anal_fin(p, 0.7, 4, 4, "round_small"))
    return p, [body, caudal(p, 6, 8, "round"), dorsal_fin(p, 0.7, 4, 4, "round_small", lean=-12)] + pectorals(p, 0.3, 3, 3, spread=55, up=0.55)


def barb():
    """M29 barbs: a deep, compressed body with a high back, a tall triangular dorsal fin and a forked tail."""
    p = fusiform_plan("barb", depth=11.0, width=4.0, mid=7.0, peak=0.42, head=0.5, ped_frac=0.3)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.13, up=0.32)
    body.boxes.append(anal_fin(p, 0.62, 5, 4, "anal"))
    body.boxes += pair("pelvic", "pelvic", (0.9, p.bottom(0.45) + 0.8, p.snout + 0.45 * 24), (0, 3, 3), (14, 18, 0), "oval")
    return p, [body, caudal(p, 8, 12, "fork"), dorsal_fin(p, 0.4, 6, 7, "tri", lean=-6)] + pectorals(p, 0.22, 4, 2)


def garra():
    """M29 garra: a long, flat-bellied bottom fish with a broad head, a sucker disc under the mouth, a forked tail and
    horizontal pectorals."""
    p = Plan("garra", -15, 9, 7.0, 6.0, 0.6, 0.4, 0.3, flat_belly=True, belly=0.0, nseg=9)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.12, up=0.25, size=2, inset=0.4)
    body.boxes.append(box("sucker", "barbel", (0, -0.5, -13.5), (3, 1, 2)))
    body.boxes += pair("barbel", "barbel", (1.2, -1.5, -15.2), (1, 1, 1), (20, 25, 0), None)
    body.boxes.append(anal_fin(p, 0.7, 4, 3, "anal"))
    body.boxes += pair("pelvic", "pelvic", (1.6, -0.6, p.snout + 0.5 * 24), (0, 3, 3), (30, 30, 0), "oval")
    return p, [body, caudal(p, 9, 11, "fork"), dorsal_fin(p, 0.36, 5, 5, "tri", lean=-10)] + pectorals(p, 0.2, 5, 3, horizontal=True)


# M29 part c: the rainbowfish, the swordtail

def rainbow():
    """M29 rainbowfish: a deep, compressed body with a high back, big eyes, two dorsal fins (a small first and a long
    second one), a long anal fin opposite the second dorsal and a forked tail."""
    p = fusiform_plan("rainbow", depth=11.0, width=4.0, mid=7.0, peak=0.45, head=0.45, ped_frac=0.3)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.12, up=0.3, size=3, inset=0.6)
    body.boxes.append(anal_fin(p, 0.45, 11, 4, "long_anal"))
    body.boxes += pair("pelvic", "pelvic", (0.9, p.bottom(0.4) + 0.8, p.snout + 0.4 * 24), (0, 3, 3), (14, 18, 0), "oval")
    return p, [body, caudal(p, 8, 12, "fork"), dorsal_fin(p, 0.36, 3, 4, "tri", lean=-6),
               dorsal_fin(p, 0.56, 8, 4, "long", name="dorsal2", lean=-4)] + pectorals(p, 0.22, 4, 2)


def swordtail():
    """M29 swordtail: a slimmer platy body; the male carries the long "sword" on the lower edge of the tail (it sits in the
    tail group so it swings with the tail; only males show it)."""
    p = fusiform_plan("swordtail", depth=9.0, width=4.5, mid=6.5, peak=0.4, head=0.5, ped_frac=0.38)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.12, up=0.3)
    body.boxes += pair("pelvic", "pelvic", (0.9, p.bottom(0.45) + 0.6, p.snout + 0.45 * 24), (0, 2, 3), (14, 18, 0), "oval")
    female = Group("female", (0, 0, 0))
    female.boxes.append(box("anal_f", "anal", (0, p.bottom(0.62) + 1.2, p.snout + 0.6 * 24 + 2), (0, 3, 4), (8, 0, 0), fin="anal"))
    tail = caudal(p, 7, 10, "round")
    cy = tail.pivot[1]
    male = Group("male", tail.pivot, "tail")
    male.boxes.append(box("sword", "sword", (0, cy + 10 / 2 - 1.0, p.ped - 1 + 3 + 5), (0, 2, 10), fin="rod"))
    return p, [body, female, tail, male, dorsal_fin(p, 0.42, 6, 5, "tri", lean=-6)] + pectorals(p, 0.24, 4, 3)


# M30 part a: the big animals

def pangasius():
    """M30 pangasius: a long, torpedo-shaped catfish with a flat head, small eyes set low, two pairs of short barbels, a
    high, sickle-shaped dorsal fin, a tiny adipose fin, a long anal fin, low pelvics and a deeply forked tail."""
    p = Plan("pangasius", -16, 10, 7.0, 6.0, 0.62, 0.3, 0.32, mid=5.0, nseg=10)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.08, up=0.55, size=2, inset=0.5)
    body.boxes += pair("barbel", "barbel", (1.6, p.bottom(0.03) - 0.6, -15.4), (1, 1, 2), (10, 30, 0), None)
    body.boxes.append(anal_fin(p, 0.58, 9, 3, "long_anal"))
    body.boxes += pair("pelvic", "pelvic", (1.2, p.bottom(0.45) + 0.6, p.snout + 0.45 * 26), (0, 2, 3), (18, 20, 0), "oval")
    body.boxes.append(box("adipose", "dorsal", (0, p.top(0.78) + 0.2, p.snout + 0.78 * 26), (0, 1, 2), fin="round_small"))
    return p, [body, caudal(p, 7, 12, "fork"), dorsal_fin(p, 0.3, 4, 7, "tri", lean=-14)] + pectorals(p, 0.16, 5, 2, spread=50, droop=30)


# M30 part b: the other freshwater giants

def barbel_pair(name, origin, length, yaw, pitch=0.0):
    """A pair of thin barbels starting at ``origin`` (right side, mirrored to the left) and pointing back and out by ``yaw``
    degrees (and down by ``pitch``)."""
    ox, oy, oz = origin
    dx = math.sin(math.radians(yaw)) * math.cos(math.radians(pitch)) * length / 2
    dz = math.cos(math.radians(yaw)) * math.cos(math.radians(pitch)) * length / 2
    dy = math.sin(math.radians(pitch)) * length / 2
    return pair(name, "barbel", (ox + dx, oy + dy, oz + dz), (1, 1, length), (-pitch, yaw, 0), None)


def redtail():
    """M30 redtail catfish: a heavy catfish with a very broad, flat head and a wide mouth, long barbels at the corners of the
    mouth and two pairs of chin barbels, a tall rounded dorsal fin, a big adipose fin, a broad, slightly forked tail."""
    p = Plan("redtail", -16, 9, 8.0, 10.0, 0.75, 0.38, 0.3, flat_belly=True, belly=0.0, nseg=10)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.1, up=0.12, size=2, inset=1.2)
    body.boxes += barbel_pair("barbel", (3.8, -1.6, -15.2), 11, 42, 6)
    body.boxes += barbel_pair("chin", (1.4, -0.4, -15.0), 5, 18, 14)
    body.boxes += barbel_pair("chin2", (2.6, -0.4, -14.4), 5, 30, 12)
    body.boxes.append(anal_fin(p, 0.66, 5, 4, "oval"))
    body.boxes += pair("pelvic", "pelvic", (2.4, -0.5, p.snout + 0.52 * 25), (3, 0, 4), (0, 18, -8), "pec_flat")
    body.boxes.append(box("adipose", "adipose", (0, p.top(0.78) - 1.5, p.snout + 0.78 * 25), (0, 4, 5), fin="oval"))
    return p, [body, caudal(p, 8, 13, "fork"), dorsal_fin(p, 0.33, 5, 8, "sail", lean=-10)] + pectorals(p, 0.2, 7, 4, spread=30,
                                                                                                    droop=8, up=0.9, horizontal=True)


def wels():
    """M30 wels catfish: a long, scaleless body, broad and flat in front, compressed behind; a tiny dorsal fin, an endless
    anal fin from the middle of the belly to the small rounded tail, two very long barbels on the upper jaw and four short
    ones on the chin, small eyes."""
    p = Plan("wels", -16, 12, 7.0, 8.0, 0.85, 0.4, 0.2, mid=4.0, nseg=11)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.07, up=0.3, size=1, inset=0.6)
    body.boxes += barbel_pair("barbel", (3.2, -3.4, -15.4), 12, 55, 32)
    body.boxes += barbel_pair("chin", (1.2, p.bottom(0.04) - 0.4, -15.0), 4, 14, 20)
    body.boxes += barbel_pair("chin2", (2.4, p.bottom(0.06) - 0.4, -14.4), 4, 26, 20)
    body.boxes.append(anal_fin(p, 0.36, 18, 3, "long_anal"))
    body.boxes += pair("pelvic", "pelvic", (1.4, p.bottom(0.36) + 0.5, p.snout + 0.36 * 28), (0, 2, 3), (18, 20, 0), "oval")
    return p, [body, caudal(p, 6, 8, "round"), dorsal_fin(p, 0.26, 3, 3, "tri", lean=-8)] + pectorals(p, 0.14, 5, 3, spread=40, droop=25)


def arapaima():
    """M30 arapaima: a long, round, torpedo-like body with a small flat head and an upturned mouth; the dorsal and anal fins
    sit far back opposite each other and, with the rounded tail, make a paddle; big scales."""
    p = Plan("arapaima", -16, 10, 6.0, 6.0, 0.72, 0.6, 0.45, mid=4.0, nseg=11)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.07, up=0.3, size=1, inset=0.5)
    body.boxes.append(anal_fin(p, 0.72, 7, 4, "long_anal"))
    body.boxes += pair("pelvic", "pelvic", (1.0, p.bottom(0.48) + 0.5, p.snout + 0.48 * 26), (0, 2, 3), (18, 20, 0), "oval")
    return p, [body, caudal(p, 7, 9, "round"), dorsal_fin(p, 0.7, 7, 4, "long", lean=-2)] + pectorals(p, 0.16, 4, 2, spread=40, droop=25)


class RayPlan:
    """M30 part b: the flat ray: t runs from the snout (0) to the tip of the tail (1), v is 0 on the upper side and 1 on the
    underside (the disc is 1 px thick at the margin)."""

    def __init__(self, name, snout=-16, ped=16, under=-0.3):
        self.name, self.snout, self.ped, self.under = name, snout, ped, under

    def t_of(self, z):
        return (z - self.snout) / (self.ped - self.snout)

    def v_of(self, y, z):
        return 1.0 if y > self.under else 0.0


# disc of the stingray: z from -16 to +2 (round, 18 px), the margin cut into strips that wave
RAY_DISC = (-16, 2, 9.0)
RAY_CORE = 3


def ray_half_width(z):
    z0, z1, r = RAY_DISC
    c = (z0 + z1) / 2
    k = (z - c) / r
    return r * math.sqrt(max(0.0, 1 - k * k))


def stingray():
    """M30 freshwater stingray (Potamotrygon): a round, flat disc (a domed body in the middle, a thin margin cut into nine
    strips per side, groups wave_r0..8 / wave_l0..8 that the renderer ripples from front to back), small eyes on top, short
    pelvic lobes behind the disc and a thick tail with a spine."""
    p = RayPlan("stingray")
    z0, z1, _ = RAY_DISC
    body = Group("body", (0, 0, 0))
    groups = [body]
    strips = (z1 - z0) // 2
    for i in range(strips):
        za = z0 + 2 * i
        zc = za + 1
        hw = ray_half_width(zc)
        core_h = 3 if 2 <= i <= strips - 3 else 2
        body.boxes.append(box(f"core{i}", "head" if i < 3 else "body", (0, -core_h / 2, zc), (2 * RAY_CORE, core_h, 2)))
        wing = max(1, round(hw - RAY_CORE))
        for side, sx in (("r", 1), ("l", -1)):
            g = Group(f"wave_{side}{i}", (sx * RAY_CORE, -0.5, zc))
            g.boxes.append(box(f"wing_{side}{i}", "body", (sx * (RAY_CORE + wing / 2), -0.5, zc), (wing, 1, 2)))
            groups.append(g)
    body.boxes.append(box("eye_r", "eye", (1.6, -3.5, -10.5), (1, 1, 1)))
    body.boxes.append(box("eye_l", "eye", (-1.6, -3.5, -10.5), (1, 1, 1)))
    body.boxes += pair("pelvic", "body", (3.4, -0.5, 2.5), (3, 1, 3), (0, 15, 0), None)
    tail = Group("tail", (0, -1.0, z1))
    tail.boxes.append(box("tail0", "body", (0, -1.0, z1 + 2.5), (3, 2, 5)))
    tail.boxes.append(box("tail1", "body", (0, -0.5, z1 + 7.0), (2, 1, 4)))
    tail.boxes.append(box("tail2", "body", (0, -0.5, z1 + 11.5), (1, 1, 5)))
    tail.boxes.append(box("spine", "spine", (0, -1.5, z1 + 7.5), (1, 1, 3)))
    groups.insert(1, tail)
    return p, groups


# M30 part c: the marine big animals

def bamboo_shark():
    """M30 part c brownbanded bamboo shark (Chiloscyllium punctatum): a slender, flat-bellied bottom shark with a long thick
    tail, a short rounded snout with two barbels, small eyes high on the head, two equal dorsal fins far back, rounded
    paddle-like pectoral and pelvic fins and a long, low tail fin without a lower lobe."""
    p = Plan("bamboo_shark", -16, 10, 4.6, 5.0, 0.72, 0.42, 0.24, flat_belly=True, belly=0.0, nseg=11)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.07, up=0.05, size=1, inset=0.6)
    body.boxes += pair("barbel", "barbel", (0.9, -0.2, -15.6), (1, 1, 2), (20, 10, 0), None)
    body.boxes.append(anal_fin(p, 0.76, 4, 2, "falcate"))
    body.boxes += pair("pelvic", "pelvic", (2.6, -0.4, p.snout + 0.42 * 26), (4, 0, 3), (0, 25, -6), "pec_flat")
    return p, [body, caudal(p, 7, 5, "bamboo"), dorsal_fin(p, 0.46, 4, 4, "falcate", lean=-10),
               dorsal_fin(p, 0.63, 4, 4, "falcate", name="dorsal2", lean=-10)] + pectorals(p, 0.2, 5, 4, spread=40, droop=6, up=0.95,
                                                                                           horizontal=True)


def reef_shark():
    """M30 part c blacktip reef shark (Carcharhinus melanopterus): the classic torpedo-shaped requiem shark with a short,
    rounded snout, a tall falcate first dorsal fin, a small second dorsal, long sickle-shaped pectoral fins and a strongly
    heterocercal tail (the long upper lobe swept up and back); the black fin tips are in the texture."""
    p = Plan("reef_shark", -16, 7, 6.0, 5.6, 0.5, 0.26, 0.36, mid=4.0, nseg=10)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.1, up=0.4, size=1, inset=0.5)
    body.boxes.append(anal_fin(p, 0.8, 3, 2, "falcate"))
    body.boxes += pair("pelvic", "pelvic", (1.4, p.bottom(0.6) + 0.4, p.snout + 0.6 * 23), (0, 2, 3), (25, 25, 0), "oval")
    body.boxes.append(box("dorsal2", "dorsal", (0, p.top(0.8) - 0.8, p.snout + 0.81 * 23), (0, 2, 2), (-10, 0, 0), fin="falcate"))
    pecs = []
    for side, sx in (("r", 1), ("l", -1)):
        z = p.snout + 0.27 * 23
        y = p.bottom(0.27) - 1.0
        g = Group(f"pec_{side}", (sx * p.w(0.27) / 2, y, z))
        g.boxes.append(box(f"pec_{side}", "pectoral", (sx * (p.w(0.27) / 2 + 3.2), y + 0.6, z + 2.6), (8, 0, 4), (0, sx * 38, -sx * 14),
                           fin="pec_sickle"))
        pecs.append(g)
    tail = caudal(p, 9, 16, "hetero")
    # the upper lobe is long: the fin plane sits above the body axis and is tilted up
    tail.boxes[0] = box("caudal", "tail", (0, tail.pivot[1], p.ped - 1 + 4.5), (0, 16, 9), (12, 0, 0), fin="hetero")
    return p, [body, tail, dorsal_fin(p, 0.4, 5, 7, "falcate", lean=-12)] + pecs


# outline of the eagle ray's wings: (z, half width) from the head to the end of the disc (pointed tips sweeping back)
EAGLE_DISC = [(-13, 3.0), (-11, 7.0), (-9, 10.5), (-7, 13.5), (-5, 14.5), (-3, 11.0), (-1, 6.0)]
COWNOSE_DISC = [(-13, 4.0), (-11, 8.0), (-9, 11.0), (-7, 12.5), (-5, 12.0), (-3, 9.0), (-1, 5.0)]


def winged_ray(name, disc, head_len, tail_len, nose):
    """M30 part c open-water ray (eagle ray, cownose ray): a flat, wide disc of wing-like pectoral fins cut into strips
    (wave_r0.. / wave_l0..: the renderer beats them like wings), a domed body in the middle, a head that sticks out in
    front (a duck-bill snout or the notched cow nose), eyes on the sides of the head and a long, thin tail."""
    p = RayPlan(name, under=-1.0)  # the underside of the wings is at y = -0.9
    body = Group("body", (0, 0, 0))
    groups = [body]
    core = 3
    for i, (zc, hw) in enumerate(disc):
        zc = zc + 1  # strips of 2 px from z - 1 to z + 1
        core_h = 3 if 1 <= i <= len(disc) - 2 else 2
        body.boxes.append(box(f"core{i}", "head" if i < 1 else "body", (0, -core_h / 2, zc), (2 * core, core_h, 2)))
        wing = max(1, round(hw - core))
        for side, sx in (("r", 1), ("l", -1)):
            # the wings sit at mid height of the body (a far wing beating down still shows above it)
            g = Group(f"wave_{side}{i}", (sx * core, -1.4, zc))
            g.boxes.append(box(f"wing_{side}{i}", "body", (sx * (core + wing / 2), -1.4, zc), (wing, 1, 2)))
            groups.append(g)
    z_head = disc[0][0]
    if nose == "bill":
        body.boxes.append(box("head0", "head", (0, -1.5, z_head - head_len / 2), (4, 3, head_len)))
        body.boxes.append(box("bill", "head", (0, -0.8, z_head - head_len - 1), (3, 2, 2)))
    else:
        # the cow nose: a square head with a notch in front (two lobes) under it
        body.boxes.append(box("head0", "head", (0, -1.75, z_head - head_len / 2), (6, 3, head_len)))
        body.boxes += pair("lobe", "head", (1.6, -0.5, z_head - head_len - 0.5), (2, 1, 1), (0, 0, 0), None)
    zf = z_head - head_len * 0.55
    body.boxes.append(box("eye_r", "eye", (2.6 if nose != "bill" else 2.1, -2.0, zf), (1, 1, 1)))
    body.boxes.append(box("eye_l", "eye", (-2.6 if nose != "bill" else -2.1, -2.0, zf), (1, 1, 1)))
    z1 = disc[-1][0] + 2
    body.boxes += pair("pelvic", "body", (2.2, -0.5, z1 + 0.5), (2, 1, 3), (0, 20, 0), None)
    body.boxes.append(box("dorsal", "dorsal", (0, -2.2, z1 + 1.5), (0, 2, 2), (-10, 0, 0), fin="round_small"))
    tail = Group("tail", (0, -1.0, z1))
    seg = tail_len / 3
    tail.boxes.append(box("tail0", "body", (0, -1.0, z1 + seg / 2), (2, 2, round(seg))))
    tail.boxes.append(box("tail1", "body", (0, -1.0, z1 + seg * 1.5), (1, 1, round(seg))))
    tail.boxes.append(box("tail2", "body", (0, -1.0, z1 + seg * 2.5), (1, 1, round(seg))))
    groups.insert(1, tail)
    return p, groups


def eagle_ray():
    return winged_ray("eagle_ray", EAGLE_DISC, 3, 16, "bill")


def cownose_ray():
    return winged_ray("cownose_ray", COWNOSE_DISC, 2, 12, "cow")


def moray():
    """M30 part c honeycomb moray (Gymnothorax favagineus): a long, muscular, laterally flattened eel without pectoral fins
    in a chain of segments (seg1.. the renderer bends in a wave, like the kuhli loach), a tall head with a deep mouth whose
    lower jaw (group jaw) opens and closes as it breathes, small eyes far forward, and one fin seam along the back and the
    belly that runs round the tail tip."""
    p = Plan("moray", -16, 15, 5.0, 3.0, 0.9, 0.45, 0.25, flat_belly=True, belly=0.0, nseg=1)
    groups = []
    zs = [-16, -11, -6, -1, 4, 8, 12, 15]
    parent = None
    for i in range(7):
        z0, z1 = zs[i], zs[i + 1]
        tc = p.t_of((z0 + z1) / 2)
        depth = max(2, round(p.d(tc)))
        width = max(2, round(p.w(tc)))
        name = "body" if i == 0 else f"seg{i}"
        g = Group(name, (0, -depth / 2, z0), parent)
        if i == 0:
            # the head: the upper jaw and skull; the lower jaw is its own group (hinged at the corner of the mouth)
            g.boxes.append(box("seg0", "head", (0, -1.0 - (depth - 1) / 2, (z0 + z1) / 2), (width, depth - 1, z1 - z0)))
            g.boxes.append(box("eye_r", "eye", (width / 2 - 0.2, -depth + 0.6, z0 + 1.0), (1, 1, 1)))
            g.boxes.append(box("eye_l", "eye", (-width / 2 + 0.2, -depth + 0.6, z0 + 1.0), (1, 1, 1)))
            g.boxes += pair("nostril", "barbel", (0.7, -depth / 2 - 0.5, z0 - 0.4), (1, 1, 1), (0, 0, 0), None)
        else:
            g.boxes.append(box(f"seg{i}", "body", (0, -depth / 2, (z0 + z1) / 2), (width, depth, z1 - z0)))
            # the fin seam along the back (and along the belly behind the vent)
            g.boxes.append(box(f"dorsal{i}", "dorsal", (0, -depth - 0.5, (z0 + z1) / 2), (0, 1, z1 - z0), fin=None))
            if i >= 3:
                g.boxes.append(box(f"anal{i}", "anal", (0, 0.5, (z0 + z1) / 2), (0, 1, z1 - z0), fin=None))
        groups.append(g)
        if i == 0:
            jaw = Group("jaw", (0, -1.0, z1 - 1), "body")
            jaw.boxes.append(box("jaw", "body", (0, -0.5, (z0 + z1) / 2 - 0.5), (width - 1, 1, z1 - z0 - 1)))
            groups.append(jaw)
        parent = name
    tail = Group("tail", (0, -1.5, 15), "seg6")
    tail.boxes.append(box("caudal", "tail", (0, -1.2, 16), (0, 3, 2), fin="round"))
    groups.append(tail)
    return p, groups


PLANS = {
    "tetra": tetra, "rasbora": rasbora, "cory": cory, "pleco": pleco, "oto": oto, "kuhli": kuhli, "loach": loach,
    "livebearer": livebearer, "platy": platy, "gourami": gourami,
    "clown": clown, "tang": tang, "goby": goby, "damsel": damsel,
    "seahorse": seahorse,
    "cichlid": cichlid, "apisto": apisto, "angel": angel, "discus": discus,
    "puffer": puffer, "barb": barb, "garra": garra,
    "rainbow": rainbow, "swordtail": swordtail,
    "pangasius": pangasius,
    "redtail": redtail, "wels": wels, "arapaima": arapaima, "stingray": stingray,
    "bamboo_shark": bamboo_shark, "reef_shark": reef_shark, "eagle_ray": eagle_ray, "cownose_ray": cownose_ray, "moray": moray,
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
    if shape == "hetero":
        # M30 part c: a shark's tail: the long upper lobe sweeps up to a point at the back, the short lower lobe points
        # down and back, the notch between them on the trailing edge
        if s <= 0:
            u = -s
            return u <= 0.08 + 0.92 * a ** 1.1 and a <= 0.72 + 0.28 * u
        if a < 0.35:
            return s <= 0.1 + 1.3 * a
        return s <= 0.555 - 1.0 * (a - 0.35)
    if shape == "bamboo":
        # M30 part c: the long, low tail of a bamboo shark (hardly an upper lobe, a long keel below, a notch near the tip)
        if s <= 0:
            return -s <= 0.45 * (1 - 0.4 * a)
        return s <= 0.9 - 0.75 * a and not (a > 0.78 and s > 0.1)
    if shape == "pec_sickle":
        # M30 part c: a long, sickle-shaped pectoral fin: the full chord at the root, a pointed tip swept back
        return -1 + 1.6 * a <= s <= 1 - 0.4 * a
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
    if shape == "falcate":
        # M30 part c: a shark's dorsal fin: the leading edge swept back to the apex, a concave trailing edge with a free rear tip
        return h <= a / 0.5 if a <= 0.5 else h <= 1.0 if a <= 0.62 else h <= 1.0 - 2.1 * (a - 0.62)
    if shape == "crest":
        # M29 Apistogramma: the spiky "cockatoo" crest, tallest in front, every other ray longer
        return h <= (1.0 - 0.45 * a) * (1.0 if int(a * 9) % 2 == 0 else 0.78)
    if shape in ("tri_up", "tri_down"):
        # M29 angelfish: a tall triangle that narrows to a point
        return h <= 1.0 - 0.75 * a ** 0.8 and a <= 1.0
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
        # M48: Diamond (shining blue scales over the back and head), Gold (pale gold body, a silvery stripe, faint red),
        # Albino (pink-white, the stripe pale silver-blue, the red faded to pink, red eyes)
        rnd = np.random.default_rng(4801)
        glint = rnd.random((64, 64))
        back = {"gold": ("#d8c890", "#ecdcaa"), "albino": ("#f0d8d0", "#f8e8e0")}.get(var, ("#6e6448", "#8a7c58"))
        stripe = {"gold": ("#e0e8e8", "#f8fcf0"), "albino": ("#a8d0e8", "#d0ecf8")}.get(var, ("#30a8ff", "#5ad0ff"))
        red = {"gold": ("#f0a888", "#f8c0a0"), "albino": ("#f08a90", "#f8a8a8")}.get(var, ("#e02a24", "#f04a3a"))

        def body(t, v, x, z, y):
            if var == "diamond" and v < 0.47 and t < 0.9:
                k = glint[int(abs(z) * 2.9 + abs(x) * 1.3) % 64, int((y + 20) * 3.1) % 64]
                if k > 0.45 or t < 0.2:
                    return mix(C("#58b8ff"), C("#c8f0ff"), k)
            if v < 0.32:
                return mix(C(back[0]), C(back[1]), v / 0.32)
            if v < 0.47 and 0.06 < t < 0.92:
                return mix(C(stripe[0]), C(stripe[1]), (v - 0.32) / 0.15)
            red_from = 0.05 if cardinal else 0.48
            if t > red_from and v < 0.92 and t < 0.98:
                return C(red[0]) if v < 0.85 else C(red[1])
            return C("#f4f0e8") if var in ("gold", "albino") else C("#e8e8e4")
        return Look(body, clear("#e8e0d8" if var in ("gold", "albino") else "#dcdcd4"), eye="#d02028" if var == "albino" else "#141414",
                    ring="#e8c8c0" if var == "albino" else "#c0c0c0")
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
        albino = var == "albino"  # M48: pink-white with the glowing line, red eyes

        def body(t, v, x, z, y):
            if 0.38 < v < 0.52 and t > 0.04:
                return mix(C("#ff6a2a"), C("#ffa040"), abs(v - 0.45) / 0.07)
            return mix(C("#f0dcd4"), C("#fbf0ea"), v) if albino else mix(C("#b8b4a4"), C("#e4e2d8"), v)

        def fins(part, a, b, t, v, z, y):
            if part == "dorsal" and a < 0.4:
                return C("#ff5030")
            if part in ("dorsal", "anal", "pelvic") and (b > 0.6 or (part == "pelvic" and a > 0.6)):
                return C("#f8f8f8")
            return C("#ecdcd4") if albino else C("#cfcabc")
        return Look(body, fins, eye="#d02028" if albino else "#141414", ring="#e8c8c0" if albino else "#c0c0c0")
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
        albino = var == "albino"  # M48: no black at all, pink-white, red eyes; the longfin has the wild colours

        def body(t, v, x, z, y):
            if albino:
                return mix(C("#f2d8cc"), C("#fcefe8"), v)
            if 0.08 < t < 0.24 and v < 0.75:
                return C("#141414")  # the eye mask
            if t > 0.86 and 0.15 < v < 0.75:
                return C("#141414")  # spot at the tail base
            return mix(C("#e8dcc8"), C("#f6ecdc"), v)

        def fins(part, a, b, t, v, z, y):
            if albino:
                return C("#f4e4dc")
            if part == "dorsal":
                return C("#181818")
            if var == "longfin" and part == "tail" and abs(b) > 0.7:
                return C("#c8c0b4")
            return C("#e8e0d4")
        return Look(body, fins, eye="#d02028" if albino else "#141414", ring="#e8c0b8" if albino else "#c0c0c0")
    if sp == "sterbai_cory" and var == "albino":
        # M48: the albino sterbai: cream-white with faint white dots, the orange pectorals stay, red eyes
        rnd = np.random.default_rng(301)
        spots = rnd.random((64, 64))

        def body(t, v, x, z, y):
            k = spots[int(abs(z) * 3.7) % 64, int((y + 20) * 3.3) % 64]
            return C("#fffaf0") if k > 0.72 else mix(C("#ecd8c4"), C("#f4e6d8"), v)

        def fins(part, a, b, t, v, z, y):
            if part == "pectoral" or part == "pelvic":
                return C("#f8a040")
            return C("#f4e8dc")
        return Look(body, fins, eye="#d02028", ring="#e8c0b8")
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
    if sp in ("lined_seahorse", "dwarf_seahorse"):
        return seahorse_look(sp, var)
    if sp in ("ram_cichlid", "bolivian_ram"):
        return ram_look(sp, var, female)
    if sp in ("cockatoo_cichlid", "agassiz_dwarf_cichlid"):
        return apisto_look(sp, var, female)
    if sp == "angelfish":
        return angel_look(var)
    if sp == "discus":
        return discus_look(var)
    if sp in ("dwarf_puffer", "red_eye_puffer", "green_spotted_puffer"):
        return puffer_look(sp, var, female)
    if sp in ("tiger_barb", "odessa_barb", "cherry_barb"):
        return barb_look(sp, var, female)
    if sp in ("dwarf_chain_loach", "reticulated_loach", "garra_rufa"):
        return loach29_look(sp)
    if sp == "bronze_cory":
        return bronze_cory_look(var)
    if sp == "mandarinfish":
        return mandarin_look()
    if sp == "banggai_cardinal":
        return banggai_look()
    if sp == "lyretail_anthias":
        return anthias_look(female)
    if sp == "panda_garra":
        return panda_garra_look()
    if sp in ("zebra_danio", "celestial_pearl_danio"):
        return danio_look(sp, var, female)
    if sp in ("forktail_rainbow", "neon_dwarf_rainbow"):
        return rainbow_look(sp, var, female)
    if sp in ("honey_gourami", "chocolate_gourami", "pearl_gourami"):
        return gourami29_look(sp, var, female)
    if sp in ("emperor_tetra", "rummy_nose_tetra", "black_phantom_tetra"):
        return tetra29_look(sp, var, female)
    if sp in ("black_molly", "swordtail"):
        return livebearer29_look(sp, var, female)
    if sp == "pangasius":
        return pangasius_look()
    if sp == "redtail_catfish":
        return redtail_look()
    if sp == "wels_catfish":
        return wels_look()
    if sp == "arapaima":
        return arapaima_look()
    if sp == "motoro_stingray":
        return stingray_look()
    if sp == "bamboo_shark":
        return bamboo_shark_look()
    if sp == "blacktip_reef_shark":
        return reef_shark_look()
    if sp == "spotted_eagle_ray":
        return eagle_ray_look()
    if sp == "cownose_ray":
        return cownose_ray_look()
    if sp == "honeycomb_moray":
        return moray_look()
    return Look(lambda t, v, x, z, y: C("#a0a0a0"))


# M30 part a: the big animals

def pangasius_look():
    """The adult pangasius: a dark blue-grey back, silvery sides with a faint iridescent sheen and a soft dark band along
    the flank, a white belly; grey fins, the tail lobes and the dorsal darker at the edge."""

    def body(t, v, x, z, y):
        sheen = 0.5 + 0.5 * math.sin(z * 0.9 + v * 6.0)
        if v < 0.22:
            return mix(C("#2c3440"), C("#46505e"), v / 0.22)
        if v > 0.8:
            return mix(C("#c8ccd0"), C("#eef0f0"), (v - 0.8) / 0.2)
        side = mix(C("#8a94a0"), C("#b8c0c8"), (v - 0.22) / 0.58)
        side = mix(side, C("#c8d4e0"), 0.18 * sheen)
        if abs(v - 0.42) < 0.05 and 0.12 < t < 0.95:
            side = mix(side, C("#4a5260"), 0.45)
        return side

    def fins(part, a, b, t, v, z, y):
        if part == "tail":
            return C("#3a4048") if abs(b) > 0.45 else C("#6a7078")
        if part == "dorsal":
            return C("#3a4048") if a > 0.55 else C("#5a6068")
        if part == "barbel":
            return C("#9a9ea4")
        return C("#7a8088")
    return Look(body, fins, eye="#1a1c20")


# M30 part b: the other freshwater giants

def _noise(seed, n=64):
    return np.random.default_rng(seed).random((n, n))


def _smooth_noise(seed, n=64, passes=3):
    a = _noise(seed, n)
    for _ in range(passes):
        a = (a + np.roll(a, 1, 0) + np.roll(a, -1, 0) + np.roll(a, 1, 1) + np.roll(a, -1, 1)) / 5
    return (a - a.min()) / max(1e-6, a.max() - a.min())


def redtail_look():
    """The redtail catfish: a dark slate-grey back and head with fine black spots, a sharp edge to a broad cream-white band
    along the lower flank from the mouth to the tail, a white belly; a bright red-orange tail, the dorsal and adipose fins dark
    with a red edge, light grey barbels."""
    spots = _noise(3011)

    def body(t, v, x, z, y):
        if v > 0.55:
            return mix(C("#ece6d8"), C("#f6f2ea"), (v - 0.55) / 0.45)
        k = spots[int(abs(z) * 3.1 + abs(x) * 1.7) % 64, int((y + 30) * 2.9) % 64]
        col = mix(C("#2e3034"), C("#4a4c52"), v / 0.55)
        if k > 0.84:
            col = col * 0.55
        if t > 0.9:
            col = mix(col, C("#b8402a"), 0.4)
        return col

    def fins(part, a, b, t, v, z, y):
        if part == "tail":
            return mix(C("#e8482a"), C("#f07a3a"), abs(b) * 0.5)
        if part == "dorsal":
            return C("#c8402a") if b > 0.55 else C("#34363c")
        if part == "adipose":
            return C("#c8402a") if a > 0.7 or abs(b) > 0.7 else C("#34363c")
        if part == "barbel":
            return C("#c8c4bc")
        if part == "anal":
            return C("#b85a3a")
        return C("#3a3c42")
    return Look(body, fins, eye="#141414", ring="#e0d070")


def wels_look():
    """The wels: marbled dark olive, brown and black on the back and the sides, lighter and greener towards the belly, a
    cream belly with grey blotches; dark brown-grey fins, the long anal fin with a lighter base, dark barbels."""
    marble = _smooth_noise(3021)
    fine = _noise(3022)

    def body(t, v, x, z, y):
        m = marble[int(abs(z) * 1.6 + abs(x) * 0.9) % 64, int((y + 30) * 1.7) % 64]
        k = fine[int(abs(z) * 3.3) % 64, int((y + 30) * 3.1) % 64]
        if v > 0.78:
            return mix(C("#e4dcc4"), C("#b8b098"), 0.6 * (m > 0.62))
        base = mix(C("#2a2a1e"), C("#5c5a3c"), v / 0.78)
        base = mix(base, C("#7a7450"), 0.45 * smooth(0.45, 0.7, m))
        if m < 0.32:
            base = base * 0.6
        return base * (0.94 + 0.12 * k)

    def fins(part, a, b, t, v, z, y):
        if part == "barbel":
            return C("#2a2820")
        if part == "anal":
            return mix(C("#6a6448"), C("#2e2c22"), max(0.0, min(1.0, (b + 1) / 2)))
        return C("#36342a") if part != "pectoral" else C("#4a4636")
    return Look(body, fins, eye="#1a1a14", ring="#a09a70")


def arapaima_look():
    """The arapaima: a dark grey-green head and back, olive-grey sides with large scales outlined darker; towards the rear the
    scales get red edges until the last part of the body is blotched red; a pale grey belly; dark fins with red margins on
    the dorsal, anal and tail fins."""

    def body(t, v, x, z, y):
        if v > 0.8:
            return mix(C("#b8bab0"), C("#d8d8cc"), (v - 0.8) / 0.2)
        base = mix(C("#2e382e"), C("#6a7460"), v / 0.8)
        # big scales: a darker outline on a 3 x 2.5 px grid (offset every other row), red edges towards the tail
        if t > 0.18:
            row = int((y + 30) / 2.5)
            col = (abs(z) + (1.5 if row % 2 else 0)) / 3.0
            edge = (col - int(col)) < 0.3 or ((y + 30) / 2.5 - row) < 0.25
            red = smooth(0.45, 0.85, t)
            if edge:
                base = mix(base * 0.72, C("#c83a28"), red * 0.9)
            else:
                base = mix(base, C("#8a5a44"), red * 0.35)
        return base

    def fins(part, a, b, t, v, z, y):
        red = C("#c8402c")
        dark = C("#2e322c")
        if part == "tail":
            return red if a > 0.65 else mix(dark, red, 0.35)
        if part in ("dorsal", "anal"):
            return red if b > 0.5 else mix(dark, red, 0.25)
        return C("#4a5044")
    return Look(body, fins, eye="#141414", ring="#c8b860")


def stingray_look():
    """The motoro stingray: a warm grey-brown disc covered with orange eye spots ringed in black, the tail brown with dark
    bands; the underside creamy white; dark eyes on top."""
    rnd = np.random.default_rng(3031)
    centres = []
    # one orange texel in a ring of black ones (texel centres lie on half numbers), on a staggered 4 px grid
    for i, gz in enumerate(range(-16, 3, 4)):
        for gx in range(-10, 11, 4):
            off = 2 if i % 2 else 0
            if rnd.random() < 0.85:
                centres.append((gx + off + 0.5 + int(rnd.integers(-1, 2)), gz + 0.5 + int(rnd.integers(-1, 2)) * (rnd.random() < 0.5), 1.0))
    fine = _noise(3032)

    def ocellus(x, z):
        best = 9.0
        for cx, cz, r in centres:
            d = math.hypot(x - cx, z - cz) / r
            if d < best:
                best = d
        return best

    def body(t, v, x, z, y):
        if v > 0.5:
            return mix(C("#f0e8dc"), C("#e8d0c0"), 0.25 * smooth(0.6, 1.0, abs(x) / 9.0))
        k = fine[int(abs(z) * 3.7 + 7) % 64, int(abs(x) * 3.3 + 3) % 64]
        base = mix(C("#6e5a44"), C("#8a7258"), k)
        if z > 2.5:  # the tail: bands of dark spots
            return C("#2a2018") if int((z - 2.5) / 1.6) % 2 == 0 and k > 0.35 else base * 0.9
        d = ocellus(x, z)
        if d < 0.6:
            return mix(C("#f08a2a"), C("#f8b048"), 0.5 * k)
        if d < 1.5:
            return C("#1c1610")
        return base

    def fins(part, a, b, t, v, z, y):
        return C("#6a5640")
    return Look(body, fins, eye="#141210", ring="#c8b890")


# M30 part c: the marine big animals

def bamboo_shark_look():
    """The brownbanded bamboo shark as an adult: warm grey-brown above, a cream belly, faint darker saddles across the back
    and the tail (the strong bands of the young fade), the fins like the body, pale barbels."""
    fine = _noise(3041)

    def saddle(z):
        return (z + 16) % 4.0 < 1.6 and z > -12

    def body(t, v, x, z, y):
        k = fine[int(abs(z) * 3.1 + 5) % 64, int((y + 30) * 2.7 + abs(x)) % 64]
        if v > 0.72:
            return mix(C("#e6dcc6"), C("#d8ccb2"), 0.4 * k)
        base = mix(C("#6e5a44"), C("#9a8262"), v / 0.72) * (0.95 + 0.1 * k)
        if saddle(z) and v < 0.6:
            base = base * 0.78
        return base

    def fins(part, a, b, t, v, z, y):
        if part == "barbel":
            return C("#d8c8a8")
        col = C("#7a6448") if not saddle(z) else C("#62503a")
        if part in ("pectoral", "pelvic"):
            return mix(col, C("#a89070"), 0.3)
        return col
    return Look(body, fins, eye="#141210", ring="#9a8a6a")


def reef_shark_look():
    """The blacktip reef shark: light grey-brown above, a white belly with a pale band reaching forward along the flank,
    five dark gill slits; jet black tips on the first dorsal, the pectorals and the lower tail lobe, a black margin on the
    upper tail lobe."""

    def body(t, v, x, z, y):
        if v > 0.62:
            return C("#f2f0ea")
        base = mix(C("#7e7c74"), C("#a8a498"), v / 0.62)
        # the pale flank band from the pelvics forward into the grey
        if 0.42 < t < 0.78 and 0.42 < v < 0.62 and v > 0.62 - 0.5 * (t - 0.42):
            base = mix(base, C("#e8e6de"), 0.75)
        # gill slits
        if 0.17 < t < 0.3 and 0.28 < v < 0.62 and int((z + 16) * 2.2) % 2 == 0:
            base = base * 0.72
        return base

    def fins(part, a, b, t, v, z, y):
        black = C("#16161a")
        grey = C("#8a8880")
        if part == "dorsal":
            return black if b > 0.35 else grey
        if part == "pectoral":
            return black if a > 0.72 else mix(grey, C("#d8d6ce"), 0.3)
        if part == "tail":
            if b > 0 and 0.22 < a < 0.55 and b > 0.32:
                return black  # the tip of the lower lobe
            if b < 0 and (-b > 0.72 or a > 0.62 + 0.28 * (-b)):
                return black  # the upper lobe's tip and trailing margin
            return grey
        if part == "anal":
            return black if b > 0.45 else grey
        return grey
    return Look(body, fins, eye="#18181a", ring="#b8b4a0")


def eagle_ray_look():
    """The spotted eagle ray: a dark blue-black back covered with small white spots, a white underside, the head and the
    long whip tail dark."""
    rnd = np.random.default_rng(3051)
    dots = set()
    for gz in range(-20, 20, 3):
        for gx in range(-16, 17, 3):
            off = 1 if (gz // 3) % 2 else 0
            if rnd.random() < 0.85:
                dots.add((gx + off, gz))

    def body(t, v, x, z, y):
        if v > 0.5:
            return C("#f2f0ea")
        base = mix(C("#1a2030"), C("#283044"), 0.5 + 0.5 * math.sin(x * 0.4 + z * 0.3))
        if z > 2:
            return base * 0.85
        if (math.floor(x), math.floor(z)) in dots and abs(x) > 1.2:
            return C("#e8ecf0")
        return base

    def fins(part, a, b, t, v, z, y):
        return C("#1e2434")
    return Look(body, fins, eye="#0e0e12", ring="#606870")


def cownose_ray_look():
    """The cownose ray: a plain olive-brown back that gets a little lighter towards the wing tips, a white underside, the
    tail brown."""
    fine = _smooth_noise(3061, passes=2)

    def body(t, v, x, z, y):
        if v > 0.5:
            return mix(C("#f2eee4"), C("#e8dccc"), 0.3 * smooth(6, 12, abs(x)))
        k = fine[int(abs(z) * 2.3 + 9) % 64, int(abs(x) * 2.1 + 3) % 64]
        base = mix(C("#5e4630"), C("#86684a"), 0.25 * k + 0.5 * smooth(4, 13, abs(x)))
        return base * (0.92 if z > 2 else 1.0)

    def fins(part, a, b, t, v, z, y):
        return C("#5a4430")
    return Look(body, fins, eye="#141008", ring="#a08868")


def moray_look():
    """The honeycomb (laced) moray: close-set dark brown-black spots separated by a narrow pale yellow network all over the
    head, the body and the fin seams, a little lighter on the belly."""
    rnd = np.random.default_rng(3071)
    jitter = rnd.random((32, 32))

    def pattern(z, y, x):
        row = math.floor((y + 30) / 2.2)
        zz = z + 2.8 * jitter[row % 32, 7] + 0.6 * x
        col = math.floor(zz / 2.8)
        fz = zz / 2.8 - col
        fy = (y + 30) / 2.2 - row
        line = fz < 0.26 + 0.12 * jitter[row % 32, col % 32] or fy < 0.3
        return line

    def body(t, v, x, z, y):
        pale = C("#e6d890") if v < 0.85 else C("#efe6b8")
        dark = C("#2a2016") if v < 0.85 else C("#4a3a26")
        return pale if pattern(z, y, x) else dark

    def fins(part, a, b, t, v, z, y):
        if part == "barbel":
            return C("#3a2c1c")
        return C("#e0d088") if pattern(z, y, 0) else C("#2e2418")
    return Look(body, fins, eye="#1a140a", ring="#d8c860")


# M29 part c: danio, rainbowfish, gourami, tetra and livebearer looks

def danio_look(sp, var, female):
    """Zebra danio: blue-black and silver-gold stripes along the body into the tail (Leopard: spots, Golden: pale gold with
    faint stripes); celestial pearl danio: blue-grey with golden pearl spots, the male with an orange belly and red-orange
    fins with black bars."""
    rnd = _spangles(2951 + len(var) + (5 if female else 0))

    def body(t, v, x, z, y):
        if sp == "celestial_pearl_danio":
            if v > 0.8:
                return C("#e8702a") if not female else C("#d8c8b0")
            k = rnd[int(abs(z) * 2.6 + abs(x)) % 64, int((y + 30) * 2.6) % 64]
            if k > 0.8 and 0.08 < t < 0.95:
                return C("#f0e0a0")
            return mix(C("#2a3a5a"), C("#4a6080"), v)
        if var == "leopard":
            k = rnd[int(abs(z) * 2.2 + abs(x)) % 64, int((y + 30) * 2.2) % 64]
            if k > 0.72 and v < 0.85:
                return C("#1a2440")
            return mix(C("#e0d090"), C("#f4ecd0"), v)
        dark, light = ("#a8a070", "#f0e0a0") if var == "golden" else ("#1e3090", "#e8d8a0")
        if v > 0.85:
            return C("#f0ece0")
        stripe = 0.2 < v < 0.8 and int((v - 0.2) / 0.12) % 2 == 0 and t > 0.12
        return C(dark) if stripe else C(light)

    def fins(part, a, b, t, v, z, y):
        if sp == "celestial_pearl_danio":
            if part in ("dorsal", "anal", "tail") and int(a * 4) % 2 == 1:
                return C("#141418")
            return C("#e85a2a") if not female else C("#d8a070")
        if part == "tail" and var != "leopard":
            dark = "#b0a070" if var == "golden" else "#2a3a90"
            return C(dark) if math.sin(b * math.pi * 2.5) > 0.2 else C("#e8dcb0")
        if part == "anal" and var != "golden":
            return C("#2a3a90") if int(b * 4) % 2 == 0 else C("#e8d8a0")
        return C("#e0dcc0")
    return Look(body, fins)


def rainbow_look(sp, var, female):
    """Forktail rainbow: pale blue-silver, the male with yellow fin edges and yellow-tipped tail lobes, a blue eye ring;
    neon dwarf rainbow: neon blue scales, the male with red fins, the female with yellowish fins."""
    def body(t, v, x, z, y):
        if sp == "forktail_rainbow":
            if v > 0.78:
                return C("#f0ecd8")
            return mix(C("#7aa8c8"), C("#c8dce0"), v)
        if v > 0.82:
            return C("#e8ecf0")
        shine = 0.5 + 0.5 * math.sin(z * 2.0 + y * 1.3)
        base = mix(C("#3a78d8"), C("#7ac0f8"), shine) if not female else mix(C("#7aa0c0"), C("#b0c8d8"), shine)
        return base

    def fins(part, a, b, t, v, z, y):
        if sp == "forktail_rainbow":
            if female:
                return C("#e0e0d8")
            if part == "tail" and abs(b) > 0.65:
                return C("#f8d820")
            if part in ("dorsal", "dorsal2", "anal") and (b > 0.5 or a > 0.8):
                return C("#f8d820")
            return C("#c8d0c0")
        if female:
            return C("#e8d8a0")
        if part in ("tail", "dorsal", "dorsal2", "anal", "pelvic"):
            return C("#e02a24") if not (part == "tail" and abs(b) < 0.25) else C("#c84a40")
        return C("#d8d8d0")
    return Look(body, fins, eye="#141414", ring="#4a90e0" if sp == "forktail_rainbow" else "#c0c0c0")


def gourami29_look(sp, var, female):
    """Honey gourami: honey-orange, the male with a dark blue-black throat and belly and a yellow dorsal, the female beige
    with a brown stripe (Gold: golden yellow, Red: deep orange-red); chocolate gourami: brown with cream bands and
    yellow-edged fins; pearl gourami: covered with pearl dots, a dark line from the snout to the tail, the male with an
    orange-red throat."""
    rnd = _spangles(2961 + len(sp) + len(var) + (3 if female else 0))

    def body(t, v, x, z, y):
        if sp == "honey_gourami":
            if female:
                if 0.42 < v < 0.55 and t > 0.1:
                    return C("#8a6038")
                return mix(C("#c8b088"), C("#e8dcc0"), v)
            if var == "gold":
                return mix(C("#f0b020"), C("#f8d860"), v)
            if var == "red":
                return mix(C("#e04018"), C("#f07030"), v)
            if v > 0.62 and t < 0.65:
                return C("#1e2440")
            return mix(C("#e08a28"), C("#f0b048"), v)
        if sp == "chocolate_gourami":
            bands = (0.22, 0.4, 0.58, 0.78)
            if any(abs(t - c + (v - 0.5) * 0.05) < 0.03 for c in bands) and v < 0.9:
                return C("#e8d0a0")
            return mix(C("#4a2a1a"), C("#7a4a30"), v)
        # pearl gourami
        if 0.48 < v < 0.56 and t > 0.05:
            return C("#2a2420")
        if not female and v > 0.68 and t < 0.6:
            return C("#e05a28")
        k = rnd[int(abs(z) * 3.0 + abs(x)) % 64, int((y + 30) * 3.0) % 64]
        if k > 0.55:
            return C("#f0ece0")
        return mix(C("#8a7a68"), C("#b8a890"), v)

    def fins(part, a, b, t, v, z, y):
        if sp == "honey_gourami":
            if female:
                return C("#d8ccb0")
            if part == "dorsal" and var == "wild":
                return C("#f8e040")
            if part == "filament":
                return C("#f0a040")
            return C("#f09030") if var != "red" else C("#e85020")
        if sp == "chocolate_gourami":
            if part in ("tail", "dorsal", "anal") and (a > 0.82 or abs(b) > 0.85):
                return C("#f0d040")
            return C("#6a4028")
        if part == "filament":
            return C("#e8a060") if not female else C("#d8c8a8")
        if part in ("anal", "tail", "dorsal"):
            k = rnd[int(abs(z) * 3.0) % 64, int((y + 30) * 3.0) % 64]
            if not female and part == "anal" and b > 0.5:
                return C("#e05a28")
            return C("#f0ece0") if k > 0.6 else C("#9a8a78")
        return C("#c8c0b0")
    return Look(body, fins)


def tetra29_look(sp, var, female):
    """Emperor tetra: a broad black band along the side, a blue-purple sheen above and gold below, yellow fins with black
    edges (the male with longer fins); rummy-nose tetra: silver with a red head and a tail striped black and white; black
    phantom tetra: smoky silver with a black shoulder patch, the male with black fins, the female with red ones."""
    def body(t, v, x, z, y):
        if sp == "emperor_tetra":
            if 0.4 < v < 0.6 and t > 0.08:
                return C("#141418")
            if v <= 0.4:
                return mix(C("#5a4a8a"), C("#7a8ac8"), v / 0.4)
            return mix(C("#b8a060"), C("#e8d8a8"), (v - 0.6) / 0.4)
        if sp == "rummy_nose_tetra":
            if t < 0.2 and v < 0.85:
                return C("#e0202a")
            return mix(C("#b8bcb8"), C("#ecece6"), v)
        # black phantom
        if 0.26 < t < 0.36 and 0.28 < v < 0.62:
            return C("#141414")
        return mix(C("#7a7a80"), C("#c8c8cc"), v)

    def fins(part, a, b, t, v, z, y):
        if sp == "emperor_tetra":
            if part in ("dorsal", "anal") and (b > 0.6 or a > 0.85):
                return C("#141418")
            if part == "tail" and abs(b) < 0.18:
                return C("#141418")
            return C("#e0c860") if not female else C("#d0c8a0")
        if sp == "rummy_nose_tetra":
            if part == "tail":
                bb = abs(b)
                return C("#141414") if 0.18 < bb < 0.42 or bb > 0.8 else C("#f4f4f0")
            return C("#e4e4dc")
        if part in ("dorsal", "anal") and not female:
            return C("#1a1a1e")
        if part in ("pelvic", "anal", "adipose") and female:
            return C("#e04030")
        return C("#c8c8c8")
    return Look(body, fins, ring="#5aa0f0" if sp == "emperor_tetra" and not female else "#c0c0c0")


def livebearer29_look(sp, var, female):
    """Black molly: velvet black (Dalmatian: white with black spots, Gold: yellow-gold); swordtail: olive-green with a red
    zigzag line along the side (Red, Red Wagtail: red with black fins), the male's sword yellow edged black."""
    rnd = _spangles(2971 + len(sp) + len(var))

    def body(t, v, x, z, y):
        if sp == "black_molly":
            if var == "dalmatian":
                k = rnd[int(abs(z) * 2.4 + abs(x)) % 64, int((y + 30) * 2.4) % 64]
                return C("#141414") if k > 0.84 else mix(C("#e8e8e4"), C("#f8f8f4"), v)
            if var == "gold":
                return mix(C("#e0a820"), C("#f8d860"), v)
            return mix(C("#141418"), C("#2a2a30"), v)
        if var in ("red", "red_wagtail"):
            return mix(C("#d82a1a"), C("#f05a30"), v)
        if abs(v - 0.5 - 0.04 * math.sin(z * 2.2)) < 0.05 and t > 0.08:
            return C("#d83a28")
        return mix(C("#6a8a48"), C("#d8dcb8"), v)

    def fins(part, a, b, t, v, z, y):
        if sp == "black_molly":
            if var == "dalmatian":
                k = rnd[int(abs(z) * 2.4) % 64, int((y + 30) * 2.4) % 64]
                return C("#141414") if k > 0.85 else C("#e8e8e4")
            if var == "gold":
                return C("#f0c040")
            return C("#202024")
        if part == "sword":
            if var == "red_wagtail":
                return C("#141414")
            return C("#141414") if b > 0.3 else C("#f0d030")
        if var == "red_wagtail":
            return C("#141414")
        if var == "red":
            return C("#e04030")
        return C("#c8c8a0")
    return Look(body, fins)


# M29 part b: puffer, barb and loach looks

def puffer_look(sp, var, female):
    """Puffers: the dwarf puffer yellow-green with dark blotches and a pale belly (the male with a dark line along the
    belly), the red-eye puffer brown (the male with a dark belly edged white and red fin edges, the female spotted), the
    green spotted puffer bright green with round black spots and a white belly."""
    rnd = _spangles(2931 + len(sp) + (3 if female else 0))

    def blot(z, y, x, sc):
        return rnd[int(abs(z) * sc + abs(x) * 0.9) % 64, int((y + 30) * sc * 0.9) % 64]

    def body(t, v, x, z, y):
        if sp == "green_spotted_puffer":
            if v > 0.62:
                return C("#f4f4ec")
            k = math.sin(z * 1.7 + 0.6 * math.sin(y)) * math.sin(y * 1.9 + 0.5 * math.sin(z * 0.7) + x * 0.8)
            if k > 0.55:
                return C("#141814")
            return mix(C("#a8d020"), C("#d8e840"), v / 0.62)
        if sp == "red_eye_puffer":
            if not female:
                if v > 0.72:
                    return C("#f0ece0") if v < 0.78 else C("#2a1c18")  # white edge, dark belly
                if v < 0.18 and 0.3 < t < 0.75:
                    return C("#c8402a")  # the red keel on the back
                return mix(C("#8a6040"), C("#b08a60"), v)
            if v > 0.78:
                return C("#e8e0cc")
            k = blot(z, y, x, 2.9)
            return C("#4a3020") if k > 0.75 else mix(C("#a08058"), C("#c8a878"), v)
        # dwarf puffer
        if v > 0.7:
            if not female and v > 0.8 and 0.3 < t < 0.85:
                return C("#3a3a20")  # the dark line along the male's belly
            return C("#f0ecc8")
        k = blot(z, y, x, 1.6)
        if k > 0.62:
            return C("#2e3a1e")
        return mix(C("#a8b030"), C("#d8d050"), v / 0.7)

    def fins(part, a, b, t, v, z, y):
        if sp == "red_eye_puffer" and not female and part in ("tail", "dorsal", "anal") and (abs(b) > 0.6 or a > 0.75):
            return C("#d82a20")
        if sp == "green_spotted_puffer":
            return C("#d8e8a0")
        return C("#e0dcc0") if part != "tail" else C("#d8d4b0")
    eye = "#e02818" if sp == "red_eye_puffer" else "#1a3a5a" if sp == "dwarf_puffer" else "#d0a020"
    return Look(body, fins, eye=eye, ring="#e8d070" if sp == "green_spotted_puffer" else "#c0c0c0")


def barb_look(sp, var, female):
    """Barbs: the tiger barb golden with four black bars (Green: dark moss green, Albino: pale pink with white bars),
    red snout and fin edges; the odessa barb silver-olive, the male with a broad scarlet band, the female with two dark
    spots; the cherry barb with a dark stripe, the male cherry red, the female beige."""
    rnd = _spangles(2941 + len(sp) + len(var) + (7 if female else 0))

    def body(t, v, x, z, y):
        k = rnd[int(abs(z) * 2.9 + abs(x)) % 64, int((y + 30) * 2.4) % 64]
        if sp == "tiger_barb":
            bars = ((0.12, 0.035), (0.34, 0.055), (0.6, 0.05), (0.9, 0.035))
            on_bar = any(abs(t - c - (v - 0.5) * 0.04) < half for c, half in bars) and v < 0.92
            if var == "green":
                base = mix(C("#1e3a1e"), C("#3a6a2e"), v)
                return mix(base, C("#5ac85a"), 0.5) if k > 0.8 else base
            if var == "albino":
                if on_bar:
                    return C("#f8f4f0")
                if t < 0.06:
                    return C("#f05a4a")
                return mix(C("#f0c8b0"), C("#f8e4d4"), v)
            if on_bar:
                return C("#141414")
            if t < 0.06:
                return C("#e04030")
            return mix(C("#d8a040"), C("#f4e0a0"), v)
        if sp == "odessa_barb":
            scale = k > 0.82 and v < 0.6
            if female:
                if (abs(t - 0.2) < 0.04 or abs(t - 0.86) < 0.04) and 0.35 < v < 0.6:
                    return C("#1e1a14")
                return mix(C("#b8b088"), C("#f0ecd8"), v)
            if 0.4 < v < 0.75 and 0.08 < t < 0.95:
                return C("#e02a1e") if not scale else C("#901810")
            if abs(t - 0.2) < 0.035 and 0.3 < v < 0.5:
                return C("#1e1a14")
            return mix(C("#8a8a60"), C("#d8d4b8"), v) if not scale else C("#3a3628")
        # cherry barb
        stripe = abs(v - 0.5) < 0.06 and 0.04 < t < 0.95
        if stripe:
            return C("#3a1410") if not female else C("#3a2a20")
        if female:
            return mix(C("#b89870"), C("#ecdcc0"), v)
        return mix(C("#c02018"), C("#f04a30"), v * 0.8)

    def fins(part, a, b, t, v, z, y):
        h = (b + 1) / 2
        if sp == "tiger_barb":
            if var == "green":
                return C("#e04030") if (part in ("dorsal", "anal") and h > 0.7) or (part == "tail" and abs(b) > 0.6) else C("#3a5a3a")
            if var == "albino":
                return C("#f07a5a") if part in ("dorsal", "tail", "pelvic") else C("#f0e0d4")
            if part == "dorsal":
                return C("#141414") if h < 0.75 else C("#e03a28")
            if part == "pelvic" or (part == "tail" and abs(b) > 0.55):
                return C("#e03a28")
            return C("#e8dcc0")
        if sp == "odessa_barb":
            if part in ("dorsal", "anal", "pelvic") and not female and (int(a * 6) % 2 == 0):
                return C("#1e1a14")
            return C("#e03a28") if not female and part == "tail" else C("#e8e4d0")
        if not female and part in ("dorsal", "tail", "anal", "pelvic"):
            return C("#d83020")
        if part == "dorsal" and h > 0.6:
            return C("#1e1410")
        return C("#e8d8c0")
    return Look(body, fins, eye="#d02828" if var == "albino" else "#141414")


def loach29_look(sp):
    """The dwarf chain loach pale with a chain of dark links along the side and back, the reticulated loach grey-cream
    with a dark net of Y-shaped bands, the garra grey-brown with a darker stripe and a dark spot at the tail base."""

    def body(t, v, x, z, y):
        if sp == "dwarf_chain_loach":
            if v > 0.82:
                return C("#f4f0e4")
            link = abs(v - 0.55) < 0.06 and t > 0.08
            ring = math.sin(z * 1.3) > 0.55 and v < 0.6 and t > 0.06
            if link or ring or (v < 0.12 and math.sin(z * 1.3 + 1.6) > 0.2):
                return C("#2a2420")
            return mix(C("#d8d0b0"), C("#f0ecd8"), v)
        if sp == "reticulated_loach":
            if v > 0.85:
                return C("#ece6d8")
            k = math.sin(z * 1.1 + 2.0 * math.sin(v * 3.1)) * math.sin(v * 7.5 + z * 0.4)
            if abs(k) < 0.28:
                return C("#2a2428")
            return mix(C("#b8b0a0"), C("#e0d8c4"), v)
        # garra
        if t > 0.9 and 0.35 < v < 0.65:
            return C("#201c18")
        if abs(v - 0.55) < 0.06 and t > 0.15:
            return C("#4a443a")
        if v > 0.85:
            return C("#d8d0c0")
        return mix(C("#6a6458"), C("#a8a090"), v)

    def fins(part, a, b, t, v, z, y):
        if sp == "dwarf_chain_loach":
            return C("#e8e0c8") if part != "tail" or abs(b) < 0.4 else C("#2a2420")
        if sp == "reticulated_loach":
            return C("#2a2428") if int(a * 5 + (b + 1) * 2) % 2 == 0 and part in ("tail", "dorsal") else C("#d8d0c0")
        return C("#9a9488") if part != "tail" else C("#8a8478")
    return Look(body, fins, eye="#2a2420")


# M48 part a: the bronze cory and the panda garra

def bronze_cory_look(var):
    """Bronze cory: metallic green-bronze flanks with a teal sheen, a darker back, a pale belly, clear fins with a dark spot in
    the dorsal; Albino pink-white with peach, red eyes; Gold Laser the wild colours with an orange-gold stripe along the back."""
    rnd = np.random.default_rng(4802)
    sheen = rnd.random((64, 64))

    def body(t, v, x, z, y):
        if var == "albino":
            return mix(C("#f0d0c0"), C("#fcece2"), v) if v < 0.85 else C("#fff4ec")
        if var == "gold_laser" and 0.22 < v < 0.4 and 0.12 < t < 0.9:
            return mix(C("#f09a20"), C("#ffc850"), abs(v - 0.31) / 0.09)
        if v > 0.82:
            return C("#e8dcc0")
        k = sheen[int(abs(z) * 2.3 + abs(x) * 1.9) % 64, int((y + 20) * 2.1) % 64]
        base = mix(C("#5a4a2a"), C("#8a7040"), v)
        if 0.3 < v < 0.8:
            base = mix(base, C("#3a8a72"), 0.45 + 0.25 * k)
        return base

    def fins(part, a, b, t, v, z, y):
        if var == "albino":
            return C("#f6e2d8")
        if part == "dorsal" and a < 0.4 and (b + 1) / 2 > 0.4:
            return C("#2a2418")
        if part in ("pectoral", "pelvic"):
            return C("#c8b898")
        return C("#b8ac90")
    return Look(body, fins, eye="#d02028" if var == "albino" else "#141414", ring="#e8c0b8" if var == "albino" else "#a0a0a0")


def panda_garra_look():
    """Panda garra: golden yellow with broad black bands (snout, behind the head, mid body, before the tail), a black stripe
    into the tail fork, yellow fins with dark rays."""

    def body(t, v, x, z, y):
        if v > 0.86:
            return C("#f4e8c0")
        for c, half in ((0.06, 0.05), (0.3, 0.07), (0.56, 0.08), (0.82, 0.06)):
            if abs(t - c - (v - 0.5) * 0.04) < half:
                return C("#141210")
        return mix(C("#e0a820"), C("#f8d050"), v)

    def fins(part, a, b, t, v, z, y):
        if part == "tail":
            return C("#141210") if abs(b) < 0.25 or abs(b) > 0.85 else C("#f0c040")
        if part in ("dorsal", "anal") and int(a * 6) % 2 == 0:
            return C("#2a2418")
        return C("#f0c848")
    return Look(body, fins, eye="#141210")


# M48 part b: the new reef fish

def mandarin_look():
    """Mandarin dragonet: a maze of orange wavy bands on bright blue, the head with orange stripes, blue-edged orange fins."""

    def body(t, v, x, z, y):
        k = math.sin(z * 1.7 + 1.3 * math.sin(y * 1.1 + z * 0.5)) + 0.6 * math.sin(y * 2.3 - z * 0.7)
        if v > 0.9:
            return C("#2a6ad0")
        if k > 0.55:
            return mix(C("#f07818"), C("#ffa040"), (k - 0.55) / 1.0)
        if k > 0.35:
            return C("#1a2a60")
        return mix(C("#1e78e8"), C("#3aa0ff"), v)

    def fins(part, a, b, t, v, z, y):
        k = math.sin(a * 9 + abs(b) * 4)
        if part in ("tail", "dorsal", "dorsal2", "anal") and (a > 0.75 or abs(b) > 0.8):
            return C("#2a7ae8")
        if part == "pectoral":
            return C("#e8a050")
        return C("#f07a20") if k > 0 else C("#2a6ad8")
    return Look(body, fins, eye="#c86a10", ring="#2a6ad0")


def banggai_look():
    """Banggai cardinalfish: silver with three black bars (through the eye, from the first dorsal down, from the second dorsal
    to the anal fin), the rear and the fins black-edged with white dots."""
    rnd = np.random.default_rng(4821)
    dots = rnd.random((64, 64))

    def body(t, v, x, z, y):
        for c, half in ((0.12, 0.035), (0.36, 0.04), (0.6, 0.045)):
            if abs(t - c - (v - 0.5) * 0.06) < half:
                return C("#121214")
        if t > 0.72:
            k = dots[int(abs(z) * 3.1 + abs(x) * 1.7) % 64, int((y + 20) * 2.9) % 64]
            return C("#f4f4f0") if k > 0.82 else mix(C("#b8bcc0"), C("#2a2a2e"), smooth(0.72, 0.95, t))
        return mix(C("#c8ccd0"), C("#eef0f0"), v)

    def fins(part, a, b, t, v, z, y):
        k = dots[int(a * 37) % 64, int((b + 1) * 21) % 64]
        if part == "pectoral":
            return C("#dcdcd8")
        if k > 0.78:
            return C("#f8f8f4")
        return C("#18181c") if part in ("dorsal", "dorsal2", "anal", "pelvic") or abs(b) > 0.6 else C("#3a3a40")
    return Look(body, fins, eye="#d8b030", ring="#202020")


def anthias_look(female):
    """Lyretail anthias: the female orange-gold with a violet streak from the eye, yellow-orange fins; the male magenta-pink
    with a red spot on the pectoral, orange-red dorsal tips and long magenta lyre tips on the tail."""

    def body(t, v, x, z, y):
        if 0.05 < t < 0.3 and abs(v - 0.55 - (t - 0.05) * 0.4) < 0.04:
            return C("#a060d0") if female else C("#f0a020")
        if female:
            return mix(C("#f08a1c"), C("#ffc050"), v)
        return mix(C("#c02a8a"), C("#f070b0"), v * 0.9)

    def fins(part, a, b, t, v, z, y):
        if female:
            if part == "tail" and abs(b) > 0.75:
                return C("#e05a10")
            return C("#f4b030") if part != "pectoral" else C("#f8d080")
        if part == "pectoral":
            return C("#e02030") if a < 0.4 else C("#f080b8")
        if part in ("dorsal", "dorsal2") and (b + 1) / 2 > 0.75:
            return C("#f04020")
        if part == "tail" and abs(b) > 0.7:
            return C("#a01870")
        return C("#d84098")
    return Look(body, fins, eye="#2a1830", ring="#f0b040" if female else "#e070b0")


# M29 part a: cichlid looks

def _spangles(seed):
    rnd = np.random.default_rng(seed)
    return rnd.random((64, 64))


def ram_look(sp, var, female):
    """Rams: golden head with a black bar through the eye, a black spot in the middle, blue spangles; the female with a
    pink belly. Gold, Electric Blue; the balloon has the wild colours (its short body is the renderer's). M47: Black (dark
    body and fins with blue spangles) and German Blue (a stronger blue than the wild form, a red belly and a red eye)."""
    sp_ = _spangles(2901 + len(var))
    bolivian = sp == "bolivian_ram"

    def base_belly(t, v):
        return mix(C("#2a6ee8"), C("#f0e0c8"), smooth(0.7, 1.0, v))

    def body(t, v, x, z, y):
        k = sp_[int(abs(z) * 2.7 + abs(x) * 1.3) % 64, int((y + 30) * 2.3) % 64]
        if bolivian:
            if 0.12 < t < 0.19:
                return C("#2a2420")
            if abs(t - 0.5) < 0.05 and 0.3 < v < 0.55:
                return C("#141210")
            if 0.42 < v < 0.52 and t > 0.25:
                return C("#5a5048")
            base = mix(C("#e8c870"), C("#b8b0a0"), smooth(0.2, 0.6, t))
            return mix(base, C("#f0ece0"), smooth(0.6, 1.0, v))
        if var == "black":
            # M47: velvet black front, blue-black flanks, bright blue spangles
            if 0.12 < t < 0.18 and v < 0.8:
                return C("#050508")
            base = mix(C("#101014"), C("#18202e"), smooth(0.2, 0.7, t))
            if female and 0.32 < t < 0.6 and v > 0.7:
                base = mix(base, C("#a0405a"), 0.55)
            return mix(base, C("#3a7ee0"), 0.65) if k > 0.86 else base
        if var == "german_blue":
            # M47: the eye bar and the spot like the wild form, a strong blue body, the head golden, the belly red
            if 0.12 < t < 0.18 and v < 0.8:
                return C("#100c0a")
            if abs(t - 0.45) < 0.05 and 0.3 < v < 0.5:
                return C("#100c0a")
            if 0.3 < t < 0.6 and v > 0.7:
                return mix(C("#e0303a") if female else C("#d84a30"), base_belly(t, v), 0.25)
            base = mix(C("#f0a028"), C("#2a6ee8"), smooth(0.1, 0.3, t))
            base = mix(base, C("#1e58d8"), smooth(0.5, 0.9, t) * 0.6)
            return mix(base, C("#7ad0ff"), 0.75) if k > 0.72 else base
        if var == "electric_blue":
            if 0.12 < t < 0.18 and v < 0.75:
                return C("#10204a")
            base = mix(C("#f08a30"), C("#1e7cf0"), smooth(0.05, 0.2, t))
            return mix(base, C("#7ac0ff"), 0.6) if k > 0.8 else base
        if var == "gold":
            if 0.12 < t < 0.18 and v < 0.75:
                return C("#e07020")
            base = mix(C("#f2a020"), C("#f6d040"), smooth(0.1, 0.5, t))
            return mix(base, C("#fff0a0"), 0.5) if k > 0.85 else base
        if 0.12 < t < 0.18 and v < 0.8:
            return C("#16120e")
        if abs(t - 0.45) < 0.055 and 0.28 < v < 0.52:
            return C("#16120e")
        if female and 0.32 < t < 0.6 and v > 0.66:
            return C("#e85a8a")
        base = mix(C("#e8a030"), C("#7a9ab0"), smooth(0.15, 0.6, t))
        base = mix(base, C("#f0e8d0"), smooth(0.7, 1.0, v) * 0.6)
        return mix(base, C("#3ab0f0"), 0.75) if k > 0.78 else base

    def fins(part, a, b, t, v, z, y):
        k = sp_[int(a * 37) % 64, int((b + 1) * 21) % 64]
        if bolivian:
            if part == "tail" and abs(b) > 0.7:
                return C("#e03a28")
            if part in ("dorsal", "dorsal2") and (b + 1) / 2 > 0.8:
                return C("#e05030")
            return C("#d8d0b8") if k < 0.8 else C("#6a8ab0")
        if var == "black":
            if part == "pelvic":
                return C("#c02a20") if a < 0.6 else C("#0a0a0e")
            return C("#121218") if k < 0.86 else C("#2e6ed0")
        if var == "german_blue":
            if part == "dorsal2":
                return C("#100c0a")
            if part == "pelvic":
                return C("#e83020") if a < 0.75 else C("#2a60e0")
            if part == "pectoral":
                return C("#d8dcf0")
            return C("#e0402c") if k < 0.45 else C("#2a78f0") if k < 0.8 else C("#90d8ff")
        if var == "electric_blue":
            return C("#2a80f0") if k < 0.75 else C("#a0d0ff")
        if var == "gold":
            return C("#f0b030") if k < 0.8 else C("#fff0a0")
        if part == "dorsal2":
            return C("#141010")
        if part == "pelvic":
            return C("#e04a30") if a < 0.7 else C("#141010")
        if part == "pectoral":
            return C("#e0dcc8")
        return C("#d04030") if k < 0.72 else C("#40b8f0")
    return Look(body, fins, eye="#e0201a" if var == "german_blue" else "#a02018")


def apisto_look(sp, var, female):
    """Apistogramma: a dark stripe along the side; the cockatoo male grey-yellow with red-tipped crest rays and a red
    marked lyre tail (Double Red: red tail and dorsal, Orange Flash: orange flashes), the Agassiz male bluish with an
    orange and blue framed spade tail (Fire Red: red fins); the females yellow."""
    sp_ = _spangles(2911 + len(var) + (5 if sp == "agassiz_dwarf_cichlid" else 0))
    agassiz = sp == "agassiz_dwarf_cichlid"

    def body(t, v, x, z, y):
        stripe = abs(v - 0.5) < 0.07 and 0.1 < t < 0.92
        if female:
            if stripe or (0.1 < t < 0.16 and v < 0.6):
                return C("#1a1610")
            return mix(C("#e8c030"), C("#f6e8a0"), v)
        k = sp_[int(abs(z) * 2.3 + abs(x)) % 64, int((y + 30) * 2.9) % 64]
        if stripe and var not in ("fire_red",):
            return C("#2a2a2e")
        if agassiz:
            if var == "fire_red":
                return mix(C("#e85a20"), C("#f0a050"), v) if k < 0.8 else C("#ffd080")
            base = mix(C("#8a9ab0"), C("#e0d090"), v)
            return mix(base, C("#5ab0e0"), 0.6) if k > 0.8 else base
        if t < 0.15 and v > 0.45:
            return C("#5aa0d0")  # blue cheeks
        if var == "orange_flash" and k > 0.7:
            return C("#f08a20")
        base = mix(C("#a8a080"), C("#f0e0a0"), v)
        return mix(base, C("#e04830"), 0.5) if var == "double_red" and k > 0.85 else base

    def fins(part, a, b, t, v, z, y):
        h = (b + 1) / 2
        if female:
            return C("#141210") if part in ("pelvic",) or (part == "dorsal" and a < 0.3) else C("#e8d070")
        k = sp_[int(a * 29) % 64, int((b + 1) * 19) % 64]
        if agassiz:
            if var == "fire_red":
                return C("#e83018") if k < 0.85 else C("#ffa040")
            if part == "tail":
                return C("#f07a20") if abs(b) > 0.55 or a > 0.85 else (C("#4a90d0") if k > 0.5 else C("#c8c0a0"))
            return C("#4a90d0") if part == "dorsal" and h > 0.7 else C("#d8c890")
        if var == "double_red":
            return C("#e02818") if part in ("tail", "dorsal", "anal") else C("#e8d8b0")
        if part == "dorsal":
            return C("#e83a20") if h > 0.75 and a < 0.5 else (C("#f08a20") if var == "orange_flash" and k > 0.6 else C("#e0dcc0"))
        if part == "tail":
            return C("#e03a28") if abs(b) > 0.6 or k > 0.82 else C("#d8d0b0")
        if part == "pelvic":
            return C("#f07a30")
        return C("#d8d0b0")
    return Look(body, fins, eye="#c03018" if agassiz else "#d0a020")


def angel_look(var):
    """Angelfish: silver with black bars through the eye, the body and the tail base (wild); Black, Marble (black and
    white marbled), Koi (white with orange and black blotches), Platinum (pure white)."""
    rnd = np.random.default_rng(2921 + len(var))
    sp_ = rnd.random((64, 64))

    def marble(z, y):
        return math.sin(z * 0.55 + math.sin(y * 0.35) * 2.2) + 0.6 * math.sin(y * 0.6 - z * 0.25)

    def body(t, v, x, z, y):
        if var == "black":
            return mix(C("#141414"), C("#2a2622"), v * 0.6)
        if var == "platinum":
            return mix(C("#e8eaee"), C("#ffffff"), v)
        if var == "marble":
            return C("#141414") if marble(z, y) > 0.2 else C("#e8e4dc")
        if var == "koi":
            k = sp_[int(abs(z) * 1.3) % 64, int((y + 40) * 1.1) % 64]
            if t < 0.3 and v < 0.55:
                return C("#f08a20")
            return C("#1a1a1a") if k > 0.8 else C("#f4f0e8")
        for c, half in ((0.15, 0.04), (0.45, 0.06), (0.86, 0.05)):
            if abs(t - c) < half:
                return C("#1a1a1a")
        return mix(C("#b8bcb8"), C("#ecece4"), v)

    def fins(part, a, b, t, v, z, y):
        if var == "black":
            return C("#1c1c1c")
        if var == "platinum":
            return C("#f0f0f0")
        if var == "marble":
            return C("#141414") if marble(z, y) > 0.2 else C("#dcd8d0")
        if var == "koi":
            return C("#e8e4d8") if part != "dorsal" else C("#f0a040")
        if part in ("dorsal", "anal"):
            return C("#1a1a1a") if a < 0.25 or (b + 1) / 2 > 0.9 else C("#d8d8d0")
        if part == "tail":
            return C("#1a1a1a") if abs(b) > 0.85 else C("#d0d0c8")
        return C("#e0e0d8")
    return Look(body, fins, eye="#c02018")


def discus_look(var):
    """Discus: the brown wild form with nine faint dark bars and blue lines on the head and fins; Red Turquoise (red
    with turquoise lines), Pigeon Blood (orange-white with black peppering), Blue Diamond (solid blue)."""
    rnd = np.random.default_rng(2931 + len(var))
    sp_ = rnd.random((64, 64))

    def lines(z, y):
        return math.sin(y * 1.1 + math.sin(z * 0.5) * 1.8)

    def body(t, v, x, z, y):
        bar = math.cos(t * math.pi * 9) > 0.86
        if var == "blue_diamond":
            return mix(C("#2a6ad8"), C("#4a9af0"), v)
        if var == "pigeon_blood":
            k = sp_[int(abs(z) * 2.9) % 64, int((y + 40) * 2.7) % 64]
            if k > 0.86:
                return C("#1a1210")
            return mix(C("#f07a30"), C("#f8e8d0"), smooth(0.2, 0.9, v))
        if var == "red_turquoise":
            return C("#30c8c0") if lines(z, y) > 0.35 else C("#c03020")
        if bar:
            return C("#4a2a18")
        if t < 0.35 and lines(z, y) > 0.6:
            return C("#3a98c8")
        return mix(C("#8a5428"), C("#c08a50"), v)

    def fins(part, a, b, t, v, z, y):
        h = (b + 1) / 2
        if part in ("dorsal", "anal") and h > 0.8:
            return C("#e04020") if var != "blue_diamond" else C("#80c0ff")
        if var == "blue_diamond":
            return C("#3a7ae0")
        if var == "pigeon_blood":
            return C("#f0a060")
        if var == "red_turquoise":
            return C("#30c8c0") if lines(z, y) > 0.2 else C("#b02818")
        return C("#3a98c8") if lines(z, y) > 0.5 else C("#8a5a30")
    return Look(body, fins, eye="#d02010")


def seahorse_look(sp, var):
    """M21.8: bony rings down the body, a paler belly and pouch, fine pale lines on the neck (the 'lined' seahorse) and
    small white dots; the dwarf seahorse tan with darker mottling."""
    rnd = np.random.default_rng(2108 + len(var) + (7 if sp == "dwarf_seahorse" else 0))
    spots = rnd.random((64, 64))
    base = {"wild": "#4e463e", "yellow": "#e2b828", "orange": "#e0702a"}.get(var, "#4e463e")
    if sp == "dwarf_seahorse":
        base = "#b09868"
    light = {"wild": "#a8987c", "yellow": "#f6dc70", "orange": "#f4a868"}.get(var, "#a8987c")
    if sp == "dwarf_seahorse":
        light = "#dccca0"

    def body(t, v, x, z, y):
        c = mix(C(base), C(light), 0.15 + 0.55 * v ** 1.6)
        ring = 0.5 + 0.5 * math.sin(y * 2.2)
        c = c * (0.86 + 0.14 * ring)
        k = spots[int(abs(y) * 2.1 + abs(x) * 3.3) % 64, int((z + 16) * 3.7) % 64]
        if sp == "dwarf_seahorse":
            if k > 0.8:
                c = mix(c, C("#5a4c30"), 0.6)
            elif k < 0.05:
                c = C("#f4f0e0")
        else:
            if -26 < y < -12 and int(abs(y) * 1.5) % 3 == 0 and v < 0.75:
                c = mix(c, C("#e8e4d8"), 0.45)  # the fine pale lines along the neck and trunk
            elif k > 0.93:
                c = C("#f0ece0")
        if y < -27 and z < -2:
            c = c * 0.9  # the snout a little darker
        return np.clip(c, 0, 1)

    def fins(part, a, b, t, v, z, y):
        return mix(C(light), C("#f0f0e8"), 0.5)
    return Look(body, fins, eye="#1a1410", ring="#d8d0b8")


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
    if b.fin == "pec_sickle":
        # M30 part c: a from the root (0) to the tip (1) on both sides
        a = lx / max(1, w) + 0.5
        return (1 - a if b.name.endswith("_l") else a), 2 * lz / max(1, d)
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
    ("neon_tetra", "tetra", ["wild", "diamond", "gold", "albino"], False), ("cardinal_tetra", "tetra", ["wild", "gold"], False), ("ember_tetra", "tetra", ["wild"], False),
    ("glowlight_tetra", "tetra", ["wild", "albino"], False), ("harlequin_rasbora", "rasbora", ["wild"], False),
    ("mosquito_rasbora", "rasbora", ["wild"], False), ("pygmy_cory", "cory", ["wild"], False), ("panda_cory", "cory", ["wild", "albino", "longfin"], False),
    ("sterbai_cory", "cory", ["wild", "albino"], False), ("green_gold_cory", "cory", ["wild"], False),
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
    # M21.8 seahorses
    ("lined_seahorse", "seahorse", ["wild", "yellow", "orange"], False),
    ("dwarf_seahorse", "seahorse", ["wild"], False),
    # M29 part a: cichlids
    ("ram_cichlid", "cichlid", ["wild", "gold", "electric_blue", "black", "german_blue", "balloon"], True),
    ("bolivian_ram", "cichlid", ["wild"], False),
    ("cockatoo_cichlid", "apisto", ["wild", "double_red", "orange_flash"], True),
    ("agassiz_dwarf_cichlid", "apisto", ["wild", "fire_red"], True),
    ("angelfish", "angel", ["wild", "black", "marble", "koi", "platinum"], False),
    ("discus", "discus", ["wild", "red_turquoise", "pigeon_blood", "blue_diamond"], False),
    # M29 part b: puffers, barbs, loaches
    ("dwarf_puffer", "puffer", ["wild"], True),
    ("red_eye_puffer", "puffer", ["wild"], True),
    ("green_spotted_puffer", "puffer", ["wild"], False),
    ("tiger_barb", "barb", ["wild", "green", "albino"], False),
    ("odessa_barb", "barb", ["wild"], True),
    ("cherry_barb", "barb", ["wild"], True),
    ("dwarf_chain_loach", "loach", ["wild"], False),
    ("reticulated_loach", "loach", ["wild"], False),
    ("garra_rufa", "garra", ["wild"], False),
    # M29 part c: danios, rainbowfish, gouramis, tetras, livebearers
    ("zebra_danio", "rasbora", ["wild", "leopard", "golden"], False),
    ("celestial_pearl_danio", "rasbora", ["wild"], True),
    ("forktail_rainbow", "rainbow", ["wild"], True),
    ("neon_dwarf_rainbow", "rainbow", ["wild"], True),
    ("honey_gourami", "gourami", ["wild", "gold", "red"], True),
    ("chocolate_gourami", "gourami", ["wild"], False),
    ("pearl_gourami", "gourami", ["wild"], True),
    ("emperor_tetra", "tetra", ["wild"], True),
    ("rummy_nose_tetra", "tetra", ["wild"], False),
    ("black_phantom_tetra", "tetra", ["wild"], True),
    ("black_molly", "platy", ["wild", "dalmatian", "gold"], True),
    ("swordtail", "swordtail", ["wild", "red", "red_wagtail"], True),
    # M30 part a: the big animals
    ("pangasius", "pangasius", ["wild"], False),
    # M30 part b: the other freshwater giants
    ("redtail_catfish", "redtail", ["wild"], False),
    ("wels_catfish", "wels", ["wild"], False),
    ("arapaima", "arapaima", ["wild"], False),
    ("motoro_stingray", "stingray", ["wild"], False),
    # M30 part c: the marine big animals
    ("bamboo_shark", "bamboo_shark", ["wild"], False),
    ("blacktip_reef_shark", "reef_shark", ["wild"], False),
    ("spotted_eagle_ray", "eagle_ray", ["wild"], False),
    ("cownose_ray", "cownose_ray", ["wild"], False),
    ("honeycomb_moray", "moray", ["wild"], False),
    # M48 part a
    ("bronze_cory", "cory", ["wild", "albino", "gold_laser"], False),
    ("panda_garra", "garra", ["wild"], False),
    # M48 part b
    ("mandarinfish", "goby", ["wild"], False),
    ("banggai_cardinal", "damsel", ["wild"], False),
    ("lyretail_anthias", "damsel", ["wild"], True),
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


def egg_case_tex():
    """M30 part c: the leathery egg case (mermaid's purse) of the bamboo shark: dark olive-brown horn with fine lengthwise
    ridges, a little lighter and amber-translucent in the middle where the yolk shows through."""
    rng = np.random.default_rng(3081)
    img = np.zeros((16, 16, 4))
    for y in range(16):
        for x in range(16):
            cx, cy = (x - 7.5) / 7.5, (y - 7.5) / 7.5
            col = mix(C("#3a3020"), C("#5a4a2c"), 0.5 + 0.5 * math.sin(y * 1.9))
            glow = max(0.0, 1 - (cx * cx * 1.4 + cy * cy * 2.2))
            col = mix(col, C("#a07a34"), 0.45 * glow)
            img[y, x, :3] = np.clip(col * (0.92 + 0.12 * rng.random()), 0, 1)
            img[y, x, 3] = 1.0
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
    written.append(save_png(egg_case_tex(), TM_TEX / "egg_case.png"))  # M30 part c
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
