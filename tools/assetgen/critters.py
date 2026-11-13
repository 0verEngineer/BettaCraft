"""M14 assets: the small tank animals (shrimp, dwarf crayfish, snails), the dwarf clawed frog, algae and the scraper.

Single source of truth for the critter models, like ``model_defs.py`` for the betta:

* units are model pixels, +Y points DOWN, the head points to -Z; the critters stand on y = 0 (the renderer scales a
  model of nominal length 16 to the species' length and flips it upright), the frog is an entity model (ground y = 24),
* every model is a set of named top-level groups (``body``, ``legs``, ``antennae``, ``eyes``, ``eggs``, ``claws``,
  ``shell``, ``foot``, ``tentacles``, ``horns``, frog: ``body``, ``head``, ``leg_*``) of cuboids; the renderer draws each
  group with its own tint (shell colour of the variety, pale legs, dark eyes, eggs) and animates the groups,
* textures are greyscale (tinted at runtime) with shading and a pattern per model (zebra stripes, rings, bands),
  binary alpha, one 64x64 box-UV texture per model (packed automatically). The frog texture is coloured.

Writes ``client/model/CritterModelDefinitions.java``, ``textures/entity/critters/*.png``, the algae overlays
``textures/entity/algae/algae_1..4.png`` and the item icons ``algae_scraper``, ``dwarf_frog_bucket``.

Run from the repo root:  py tools/assetgen/critters.py
"""
from __future__ import annotations

import math
from dataclasses import dataclass, field

import numpy as np

from common import ITEM_TEX, JAVA_SRC, TEX, save_png

CRIT_TEX = TEX / "entity" / "critters"
ALGAE_TEX = TEX / "entity" / "algae"


@dataclass
class Cube:
    name: str
    center: tuple[float, float, float]
    size: tuple[float, float, float]
    rot: tuple[float, float, float] = (0.0, 0.0, 0.0)
    pattern: str = "plain"
    u: int = 0
    v: int = 0


@dataclass
class Group:
    name: str
    pivot: tuple[float, float, float] = (0.0, 0.0, 0.0)
    cubes: list[Cube] = field(default_factory=list)


def c(name, center, size, rot=(0, 0, 0), pattern="plain"):
    return Cube(name, tuple(map(float, center)), tuple(map(float, size)), tuple(map(float, rot)), pattern)


def mirror(cube: Cube, name: str) -> Cube:
    cx, cy, cz = cube.center
    rx, ry, rz = cube.rot
    return Cube(name, (-cx, cy, cz), cube.size, (rx, -ry, -rz), cube.pattern)


def pairs(base: Cube, prefix: str) -> list[Cube]:
    return [Cube(prefix + "_r", base.center, base.size, base.rot, base.pattern), mirror(base, prefix + "_l")]


# ---------------------------------------------------------------------------------------------------------- models

def shrimp() -> list[Group]:
    body = Group("body", cubes=[
        c("carapace", (0, -3.2, -3.4), (3.2, 2.6, 5.6), (3, 0, 0), "speckle"),
        c("rostrum", (0, -3.7, -7.2), (0.6, 0.5, 3.0), (-10, 0, 0)),
        c("seg1", (0, -3.3, 0.3), (2.8, 2.4, 2.4), (8, 0, 0), "speckle"),
        c("seg2", (0, -3.0, 2.3), (2.5, 2.1, 2.2), (16, 0, 0), "speckle"),
        c("seg3", (0, -2.5, 4.2), (2.1, 1.8, 2.0), (26, 0, 0), "speckle"),
        c("seg4", (0, -1.9, 5.7), (1.6, 1.4, 1.8), (34, 0, 0)),
        c("tail", (0, -1.3, 7.1), (3.4, 0.4, 2.0), (22, 0, 0)),
    ])
    legs = Group("legs", cubes=[])
    for i, z in enumerate((-5.4, -4.4, -3.4, -2.4, -1.4)):
        legs.cubes += pairs(c(f"leg{i}", (1.6, -1.25, z), (0.35, 2.6, 0.35), (0, 0, 24 + i * 2)), f"leg{i}")
    for i, z in enumerate((0.6, 2.3, 3.9)):
        legs.cubes += pairs(c(f"swim{i}", (0.7, -1.9, z), (0.3, 1.0, 0.6), (-15, 0, 10)), f"swim{i}")
    ant = Group("antennae", pivot=(0, -3.8, -6.0), cubes=[])
    ant.cubes += pairs(Cube("long", (0.5, -3.8, -6.0), (0.2, 0.2, 10.0), (-14, 22, 0)), "long")
    ant.cubes += pairs(Cube("short", (0.4, -3.9, -6.2), (0.2, 0.2, 3.0), (-24, 40, 0)), "short")
    eyes = Group("eyes", cubes=pairs(c("eye", (0.95, -3.9, -6.0), (0.8, 0.8, 0.8)), "eye"))
    eggs = Group("eggs", cubes=[c("clutch", (0, -1.75, 1.6), (2.2, 1.1, 3.0), (10, 0, 0), "eggs"),
                                 c("clutch2", (0, -1.8, -0.3), (1.6, 0.9, 1.4), (0, 0, 0), "eggs")])
    return [body, legs, ant, eyes, eggs]


def crayfish() -> list[Group]:
    body = Group("body", cubes=[
        c("carapace", (0, -2.6, -2.8), (3.8, 2.6, 6.0), (2, 0, 0), "speckle"),
        c("head", (0, -2.5, -6.5), (2.8, 2.0, 2.2), (6, 0, 0), "speckle"),
        c("rostrum", (0, -2.8, -8.0), (0.7, 0.5, 1.6), (-6, 0, 0)),
        c("seg1", (0, -2.4, 1.2), (3.4, 1.9, 2.2), (4, 0, 0), "speckle"),
        c("seg2", (0, -2.3, 3.2), (3.1, 1.7, 2.0), (8, 0, 0), "speckle"),
        c("seg3", (0, -2.1, 5.0), (2.7, 1.5, 1.8), (12, 0, 0)),
        c("tail", (0, -1.8, 6.8), (4.4, 0.5, 2.4), (8, 0, 0)),
    ])
    claws = Group("claws", cubes=[])
    claws.cubes += pairs(c("arm", (2.4, -2.1, -6.6), (0.9, 0.9, 3.0), (0, 28, 0)), "arm")
    claws.cubes += pairs(c("hand", (3.4, -2.2, -9.2), (1.7, 1.0, 3.0), (0, 14, 0), "speckle"), "hand")
    claws.cubes += pairs(c("finger", (2.8, -2.2, -11.0), (0.5, 0.6, 2.0), (0, 30, 0)), "finger")
    legs = Group("legs", cubes=[])
    for i, z in enumerate((-4.6, -3.4, -2.2, -1.0)):
        legs.cubes += pairs(c(f"leg{i}", (2.3, -1.2, z), (0.45, 2.4, 0.45), (0, 0, 32)), f"leg{i}")
    ant = Group("antennae", pivot=(0, -2.9, -7.4), cubes=[])
    ant.cubes += pairs(Cube("long", (0.6, -2.9, -7.4), (0.25, 0.25, 11.0), (-8, 26, 0)), "long")
    eyes = Group("eyes", cubes=pairs(c("eye", (0.9, -3.4, -7.2), (0.7, 0.7, 0.7)), "eye"))
    eggs = Group("eggs", cubes=[c("clutch", (0, -1.4, 2.2), (2.8, 1.0, 3.6), (4, 0, 0), "eggs")])
    return [body, claws, legs, ant, eyes, eggs]


def snail_foot(length, width, head_z, tentacle_len, tentacle_z, thick=0.9):
    foot = Group("foot", cubes=[c("foot", (0, -thick / 2, 0), (width, thick, length), (0, 0, 0), "skin"),
                                c("head", (0, -thick - 0.3, head_z), (width * 0.45, 1.2, 2.2), (0, 0, 0), "skin")])
    tent = Group("tentacles", pivot=(0, -thick - 0.4, tentacle_z), cubes=[])
    tent.cubes += pairs(Cube("tentacle", (width * 0.12, -thick - 0.4, tentacle_z), (0.3, 0.3, tentacle_len), (-12, 28, 0), "skin"), "tentacle")
    return foot, tent


def nerite() -> list[Group]:
    shell = Group("shell", cubes=[
        c("base", (0, -2.0, 0.6), (9.0, 3.0, 11.0), (0, 0, 0), "zebra"),
        c("mid", (0, -4.1, 1.1), (8.0, 2.0, 9.5), (0, 0, 0), "zebra"),
        c("top", (0, -5.7, 1.7), (6.0, 1.6, 7.0), (0, 0, 0), "zebra"),
        c("apex", (0, -6.8, 2.8), (3.5, 1.0, 4.0), (0, 0, 0), "zebra"),
    ])
    foot, tent = snail_foot(11.0, 7.0, -6.2, 4.0, -7.0)
    horns = Group("horns", cubes=[])
    horns.cubes += pairs(c("horn_a", (2.4, -5.9, -0.6), (0.6, 2.6, 0.6), (0, 0, 22), "dark"), "horn_a")
    horns.cubes += pairs(c("horn_b", (2.2, -5.6, 3.4), (0.6, 2.4, 0.6), (18, 0, 22), "dark"), "horn_b")
    eyes = Group("eyes", cubes=pairs(c("eye", (0.9, -1.6, -7.2), (0.45, 0.45, 0.45)), "eye"))
    return [shell, foot, tent, horns, eyes]


def apple() -> list[Group]:
    shell = Group("shell", cubes=[
        c("w1", (0, -2.6, 1.0), (9.0, 3.4, 10.0), (0, 0, 0), "bands"),
        c("w2", (0, -5.6, 1.4), (10.0, 3.0, 10.5), (0, 0, 0), "bands"),
        c("w3", (0, -8.2, 1.8), (8.2, 2.6, 8.5), (0, 0, 0), "bands"),
        c("w4", (0, -10.1, 2.2), (5.6, 1.6, 6.0), (0, 0, 0), "bands"),
        c("w5", (0, -11.2, 2.6), (3.0, 1.0, 3.2), (0, 0, 0), "bands"),
        c("apex", (0, -11.9, 2.8), (1.4, 0.8, 1.4), (0, 0, 0), "bands"),
    ])
    foot, tent = snail_foot(13.0, 7.5, -7.0, 7.0, -8.0, thick=1.2)
    foot.cubes.append(c("siphon", (-1.9, -3.4, -5.4), (0.9, 0.9, 4.6), (-38, -8, 0), "skin"))
    eyes = Group("eyes", cubes=pairs(c("eye", (1.2, -2.0, -8.2), (0.6, 0.6, 0.6)), "eye"))
    return [shell, foot, tent, eyes]


def ramshorn() -> list[Group]:
    # a disc standing on its edge (axis along x); the inner whorls are a little thicker than the outer ones
    shell = Group("shell", cubes=[
        c("r1", (0, -7.6, 0.6), (3.6, 14.0, 6.0), (0, 0, 0), "rings"),
        c("r2", (0, -7.6, 0.6), (3.8, 12.0, 10.0), (0, 0, 0), "rings"),
        c("r3", (0, -7.6, 0.6), (4.0, 9.0, 13.0), (0, 0, 0), "rings"),
        c("r4", (0, -7.6, 0.6), (4.2, 6.0, 14.4), (0, 0, 0), "rings"),
        c("core", (0, -7.6, 0.6), (4.6, 3.0, 3.0), (0, 0, 0), "dark"),
    ])
    foot, tent = snail_foot(9.0, 2.8, -5.8, 4.5, -6.6, thick=0.8)
    eyes = Group("eyes", cubes=pairs(c("eye", (0.6, -1.4, -6.6), (0.4, 0.4, 0.4)), "eye"))
    return [shell, foot, tent, eyes]


def bladder() -> list[Group]:
    shell = Group("shell", cubes=[
        c("w1", (0, -2.3, 1.0), (6.0, 3.2, 8.0), (0, 0, 0), "speckle"),
        c("w2", (0, -4.6, 1.6), (5.0, 2.4, 6.5), (0, 0, 0), "speckle"),
        c("w3", (0, -6.2, 2.4), (3.4, 1.6, 4.0), (0, 0, 0), "speckle"),
        c("apex", (0, -7.1, 3.0), (1.6, 1.0, 2.0), (0, 0, 0), "speckle"),
    ])
    foot, tent = snail_foot(10.0, 4.5, -5.4, 3.2, -6.2, thick=0.8)
    eyes = Group("eyes", cubes=pairs(c("eye", (0.6, -1.4, -6.2), (0.4, 0.4, 0.4)), "eye"))
    return [shell, foot, tent, eyes]


def cone(prefix, sizes, start, step_y, step_z, tilt, pattern):
    out = []
    y, z = start
    for i, s in enumerate(sizes):
        out.append(c(f"{prefix}{i}", (0, y, z), (s, 2.4, s), (tilt, 0, 0), pattern))
        y -= step_y
        z += step_z
    return out


def trumpet() -> list[Group]:
    shell = Group("shell", cubes=cone("w", [5.0, 4.4, 3.8, 3.2, 2.6, 2.0, 1.4], (-2.2, 1.0), 1.8, 0.95, 28, "flecks"))
    foot, tent = snail_foot(7.0, 3.0, -3.6, 2.4, -4.4, thick=0.8)
    eyes = Group("eyes", cubes=pairs(c("eye", (0.5, -1.3, -4.4), (0.35, 0.35, 0.35)), "eye"))
    return [shell, foot, tent, eyes]


def assassin() -> list[Group]:
    shell = Group("shell", cubes=cone("w", [6.0, 5.2, 4.4, 3.6, 2.6, 1.6], (-2.6, 1.4), 1.9, 1.0, 26, "assassin"))
    foot, tent = snail_foot(9.0, 4.0, -4.8, 3.0, -5.6, thick=0.8)
    foot.cubes.append(c("siphon", (0, -3.0, -5.2), (0.8, 0.8, 4.6), (-30, 0, 0), "skin"))
    eyes = Group("eyes", cubes=pairs(c("eye", (0.6, -1.4, -5.6), (0.4, 0.4, 0.4)), "eye"))
    return [shell, foot, tent, eyes]


def frog() -> list[Group]:
    """Entity model of the dwarf clawed frog, ground at y = 0 (the Java code adds the entity offset)."""
    body = Group("body", cubes=[c("trunk", (0, -2.2, 0.6), (6.0, 3.0, 8.0), (-4, 0, 0), "frog")])
    head = Group("head", pivot=(0, -2.4, -3.2), cubes=[
        c("skull", (0, -2.4, -4.8), (5.0, 2.4, 3.6), (0, 0, 0), "frog"),
        c("snout", (0, -2.1, -6.9), (3.4, 1.6, 1.0), (0, 0, 0), "frog"),
        c("eye_r", (1.5, -3.8, -5.0), (1.0, 1.0, 1.0), (0, 0, 0), "eye"),
        c("eye_l", (-1.5, -3.8, -5.0), (1.0, 1.0, 1.0), (0, 0, 0), "eye"),
    ])
    groups = [body, head]
    for side, sx in (("right", 1), ("left", -1)):
        groups.append(Group(f"arm_{side}", pivot=(2.6 * sx, -1.8, -2.4), cubes=[
            c("upper", (3.4 * sx, -1.4, -3.4), (1.0, 1.0, 2.6), (20, 40 * sx, 0), "frog"),
            c("hand", (3.9 * sx, -0.4, -4.9), (1.6, 0.3, 1.6), (0, 30 * sx, 0), "frog")]))
        groups.append(Group(f"leg_{side}", pivot=(2.6 * sx, -1.8, 3.6), cubes=[
            c("thigh", (3.9 * sx, -1.8, 4.2), (3.4, 1.4, 1.4), (0, -28 * sx, 0), "frog"),
            c("shin", (4.6 * sx, -1.3, 6.4), (1.1, 1.1, 3.4), (0, 14 * sx, 0), "frog"),
            c("foot", (4.2 * sx, -0.6, 9.2), (2.6, 0.3, 3.2), (0, 6 * sx, 0), "web")]))
    return groups


def hermit() -> list[Group]:
    """M20 blue-leg hermit crab: a borrowed spiral shell at the back, the front of the crab with claws, legs and eye stalks."""
    shell = Group("shell", cubes=[
        c("w1", (0, -2.8, 2.6), (6.4, 4.6, 7.0), (0, 0, 0), "bands"),
        c("w2", (0, -5.6, 3.4), (5.2, 2.6, 5.6), (0, 0, 0), "bands"),
        c("w3", (0, -7.3, 4.2), (3.6, 1.6, 3.8), (0, 0, 0), "bands"),
        c("apex", (0, -8.4, 4.8), (1.8, 1.0, 2.0), (0, 0, 0), "bands"),
        c("mouth", (0, -2.2, -0.9), (5.0, 3.8, 1.0), (0, 0, 0), "dark"),
    ])
    body = Group("body", cubes=[c("front", (0, -2.0, -2.0), (3.6, 2.6, 2.4), (0, 0, 0), "speckle")])
    claws = Group("claws", cubes=[
        c("claw_big", (1.4, -1.6, -4.4), (2.0, 1.8, 2.8), (0, -10, 0), "speckle"),
        c("claw_small", (-1.4, -1.4, -4.0), (1.4, 1.3, 2.2), (0, 10, 0), "speckle"),
    ])
    legs = Group("legs", cubes=[])
    for i, z in enumerate((-3.0, -2.0, -1.0)):
        legs.cubes += pairs(c(f"leg{i}", (2.2, -1.0, z), (0.5, 2.2, 0.5), (0, 0, 36)), f"leg{i}")
    ant = Group("antennae", pivot=(0, -3.0, -3.4), cubes=[])
    ant.cubes += pairs(Cube("long", (0.5, -3.0, -3.4), (0.2, 0.2, 5.0), (-20, 20, 0)), "long")
    eyes = Group("eyes", cubes=pairs(c("eye", (0.7, -3.6, -3.4), (0.6, 1.4, 0.6)), "eye"))
    return [shell, body, claws, legs, ant, eyes]


def turbo() -> list[Group]:
    """M20 turbo snail: a round, high shell with spiral ridges (Turbo fluctuosa)."""
    shell = Group("shell", cubes=[
        c("w1", (0, -2.6, 0.8), (10.0, 3.6, 10.5), (0, 0, 0), "rings"),
        c("w2", (0, -5.8, 1.2), (9.4, 3.2, 9.6), (0, 0, 0), "rings"),
        c("w3", (0, -8.6, 1.6), (7.2, 2.6, 7.4), (0, 0, 0), "rings"),
        c("w4", (0, -10.6, 2.0), (4.6, 1.8, 4.8), (0, 0, 0), "rings"),
        c("apex", (0, -11.9, 2.2), (2.0, 1.0, 2.2), (0, 0, 0), "rings"),
    ])
    foot, tent = snail_foot(12.0, 7.5, -6.6, 4.5, -7.4, thick=1.1)
    eyes = Group("eyes", cubes=pairs(c("eye", (1.1, -1.9, -7.6), (0.5, 0.5, 0.5)), "eye"))
    return [shell, foot, tent, eyes]


MODELS = {
    "shrimp": shrimp,
    "crayfish": crayfish,
    "nerite": nerite,
    "apple": apple,
    "ramshorn": ramshorn,
    "bladder": bladder,
    "trumpet": trumpet,
    "assassin": assassin,
    "frog": frog,
    "hermit": hermit,
    "turbo": turbo,
}


# ---------------------------------------------------------------------------------------------------------- UV + textures

def footprint(cube: Cube) -> tuple[int, int]:
    w, h, d = cube.size
    return int(math.ceil(2 * (d + w))) + 1, int(math.ceil(d + h)) + 1


def pack(groups: list[Group]) -> int:
    """Shelf-packs the box-UV footprints; returns the texture size (64, or 128 for the big shells)."""
    cubes = [cu for g in groups for cu in g.cubes]
    order = sorted(cubes, key=lambda cu: -footprint(cu)[1])
    for size in (64, 128):
        x = y = row = 0
        ok = True
        for cu in order:
            fw, fh = footprint(cu)
            if x + fw > size:
                x = 0
                y += row
                row = 0
            if y + fh > size:
                ok = False
                break
            cu.u, cu.v = x, y
            x += fw
            row = max(row, fh)
        if ok:
            return size
    raise ValueError("texture too small")


def _pattern(cu: Cube, face: str, px: float, py: float, pz: float, rng) -> float:
    """Brightness (0..1) of the greyscale texture at model point (px, py, pz)."""
    shade = {"top": 1.0, "bottom": 0.6, "north": 0.86, "south": 0.86, "east": 0.78, "west": 0.78}[face]
    base = 0.9
    p = cu.pattern
    if p == "eye":
        return 0.12
    if p == "dark":
        return 0.3 * shade
    if p == "zebra":
        band = math.sin((py * 1.1 + pz * 0.55 + px * 0.2) * 1.5)
        base = 0.3 if band > 0.55 else 0.95
    elif p == "bands":
        band = math.sin(-py * 1.6 + pz * 0.25)
        base = 0.78 if band > 0.75 else 0.95
    elif p == "rings":
        r = math.hypot(py + 7.6, pz - 0.6)
        base = 0.62 if (r % 2.0) < 0.45 else 0.94
    elif p == "assassin":
        base = 0.95 if int(math.floor((-py + pz * 0.3) / 1.2)) % 2 == 0 else 0.3
    elif p == "flecks":
        base = 0.55 if rng.random() < 0.18 else 0.85
    elif p == "speckle":
        base = 0.7 if rng.random() < 0.12 else 0.93
    elif p == "skin":
        base = 0.78 + 0.08 * rng.random()
    elif p == "eggs":
        base = 0.75 if (int(px * 2) + int(pz * 2)) % 2 else 0.98
    return max(0.0, min(1.0, base * shade + (rng.random() - 0.5) * 0.06))


FROG_BASE = np.array([0.42, 0.40, 0.31])
FROG_SPOT = np.array([0.20, 0.19, 0.15])
FROG_BELLY = np.array([0.72, 0.70, 0.60])


def _frog_colour(cu: Cube, face: str, px, py, pz, rng) -> np.ndarray:
    if cu.pattern == "eye":
        return np.array([0.08, 0.08, 0.07])
    if face == "bottom":
        col = FROG_BELLY
    else:
        n = math.sin(px * 2.1 + pz * 1.3) * math.sin(pz * 1.7 - py * 2.3) + (rng.random() - 0.5) * 0.6
        col = FROG_SPOT if n > 0.45 else FROG_BASE
    if cu.pattern == "web":
        col = col * 0.8 + 0.12
    shade = {"top": 1.0, "bottom": 0.85, "north": 0.9, "south": 0.9, "east": 0.85, "west": 0.85}[face]
    return np.clip(col * shade + (rng.random() - 0.5) * 0.04, 0, 1)


def texture(groups: list[Group], name: str, seed: int, size: int) -> np.ndarray:
    rng = np.random.default_rng(seed)
    img = np.zeros((size, size, 4))
    for cu in (cu for g in groups for cu in g.cubes):
        w, h, d = cu.size
        cx, cy, cz = cu.center
        x0, y0, z0 = cx - w / 2, cy - h / 2, cz - d / 2
        W, D, H = int(math.ceil(w)), int(math.ceil(d)), int(math.ceil(h))
        rects = [  # face, u0, v0, width, height, mapping of (a, b) -> model point
            ("top", cu.u + D, cu.v, W, D, lambda a, b: (x0 + a, y0, z0 + b)),
            ("bottom", cu.u + D + W, cu.v, W, D, lambda a, b: (x0 + a, y0 + h, z0 + b)),
            ("east", cu.u, cu.v + D, D, H, lambda a, b: (x0 + w, y0 + b, z0 + a)),
            ("north", cu.u + D, cu.v + D, W, H, lambda a, b: (x0 + a, y0 + b, z0)),
            ("west", cu.u + D + W, cu.v + D, D, H, lambda a, b: (x0, y0 + b, z0 + d - a)),
            ("south", cu.u + 2 * D + W, cu.v + D, W, H, lambda a, b: (x0 + w - a, y0 + b, z0 + d)),
        ]
        for face, u0, v0, fw, fh, fn in rects:
            for j in range(max(1, fh)):
                for i in range(max(1, fw)):
                    uu, vv = u0 + i, v0 + j
                    if not (0 <= uu < size and 0 <= vv < size):
                        continue
                    a = (i + 0.5) * (w if face in ("top", "bottom", "north", "south") else d) / max(1, fw)
                    b = (j + 0.5) * (d if face in ("top", "bottom") else h) / max(1, fh)
                    px, py, pz = fn(a, b)
                    if name == "frog":
                        img[vv, uu, :3] = _frog_colour(cu, face, px, py, pz, rng)
                    else:
                        img[vv, uu, :3] = _pattern(cu, face, px, py, pz, rng)
                    img[vv, uu, 3] = 1.0
    return img


def overlay(groups: list[Group], size: int, kind: str, seed: int) -> np.ndarray:
    """M21.5: lines over the shell of a racer nerite (``fine`` / ``wide``) or the bands of an apple snail (``bands``):
    the same box-UV layout as the model texture, white with shading where a line is, transparent elsewhere (tinted with
    the colour of the lines at runtime and drawn over the shell)."""
    rng = np.random.default_rng(seed)
    img = np.zeros((size, size, 4))
    shell = [g for g in groups if g.name == "shell"]
    for cu in (cu for g in shell for cu in g.cubes):
        w, h, d = cu.size
        cx, cy, cz = cu.center
        x0, y0, z0 = cx - w / 2, cy - h / 2, cz - d / 2
        W, D, H = int(math.ceil(w)), int(math.ceil(d)), int(math.ceil(h))
        rects = [
            ("top", cu.u + D, cu.v, W, D, lambda a, b: (x0 + a, y0, z0 + b)),
            ("bottom", cu.u + D + W, cu.v, W, D, lambda a, b: (x0 + a, y0 + h, z0 + b)),
            ("east", cu.u, cu.v + D, D, H, lambda a, b: (x0 + w, y0 + b, z0 + a)),
            ("north", cu.u + D, cu.v + D, W, H, lambda a, b: (x0 + a, y0 + b, z0)),
            ("west", cu.u + D + W, cu.v + D, D, H, lambda a, b: (x0, y0 + b, z0 + d - a)),
            ("south", cu.u + 2 * D + W, cu.v + D, W, H, lambda a, b: (x0 + w - a, y0 + b, z0 + d)),
        ]
        for face, u0, v0, fw, fh, fn in rects:
            shade = {"top": 1.0, "bottom": 0.6, "north": 0.86, "south": 0.86, "east": 0.78, "west": 0.78}[face]
            for j in range(max(1, fh)):
                for i in range(max(1, fw)):
                    uu, vv = u0 + i, v0 + j
                    if not (0 <= uu < size and 0 <= vv < size):
                        continue
                    a = (i + 0.5) * (w if face in ("top", "bottom", "north", "south") else d) / max(1, fw)
                    b = (j + 0.5) * (d if face in ("top", "bottom") else h) / max(1, fh)
                    px, py, pz = fn(a, b)
                    if kind == "fine":
                        on = math.sin((py * 1.1 + pz * 0.55 + px * 0.2) * 3.4 + 0.7) > 0.72
                    elif kind == "wide":
                        on = math.sin((py * 1.1 + pz * 0.55 + px * 0.2) * 1.9 + 0.4) > 0.55
                    else:  # apple snail bands around the whorls
                        on = math.sin(-py * 0.95 + pz * 0.18) > 0.72
                    if not on or face == "bottom":
                        continue
                    img[vv, uu, :3] = max(0.0, min(1.0, 0.95 * shade + (rng.random() - 0.5) * 0.06))
                    img[vv, uu, 3] = 1.0
    return img


# ---------------------------------------------------------------------------------------------------------- Java

def f(v: float) -> str:
    s = f"{v:.4f}".rstrip("0").rstrip(".")
    if s in ("-0", ""):
        s = "0"
    return s + "F"


def rad(deg: float) -> str:
    return f(math.radians(deg))


def java_layer(name: str, groups: list[Group], frog_offset: bool, size: int) -> str:
    lines = [f"    public static LayerDefinition create{name.capitalize()}Layer() {{",
             "        MeshDefinition mesh = new MeshDefinition();",
             "        PartDefinition root = mesh.getRoot();"]
    if frog_offset:
        lines.append("        root = root.addOrReplaceChild(\"frog\", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));")
    for g in groups:
        px, py, pz = g.pivot
        var = "g_" + g.name
        lines.append(f"        PartDefinition {var} = root.addOrReplaceChild(\"{g.name}\", CubeListBuilder.create(), "
                     f"PartPose.offset({f(px)}, {f(py)}, {f(pz)}));")
        for cu in g.cubes:
            w, h, d = cu.size
            cx, cy, cz = cu.center
            rx, ry, rz = cu.rot
            lines.append(f"        {var}.addOrReplaceChild(\"{cu.name}\", CubeListBuilder.create().texOffs({cu.u}, {cu.v})"
                         f".addBox({f(-w / 2)}, {f(-h / 2)}, {f(-d / 2)}, {f(w)}, {f(h)}, {f(d)}), "
                         f"PartPose.offsetAndRotation({f(cx - px)}, {f(cy - py)}, {f(cz - pz)}, {rad(rx)}, {rad(ry)}, {rad(rz)}));")
    lines.append(f"        return LayerDefinition.create(mesh, {size}, {size});")
    lines.append("    }")
    return "\n".join(lines)


def write_java(models: dict[str, list[Group]], sizes: dict[str, int]) -> object:
    body = ["// GENERATED by tools/assetgen/critters.py - do not edit by hand, edit critters.py and rerun it",
            "package org.overengineer.bettacraft.client.model;",
            "",
            "import net.minecraft.client.model.geom.PartPose;",
            "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
            "import net.minecraft.client.model.geom.builders.LayerDefinition;",
            "import net.minecraft.client.model.geom.builders.MeshDefinition;",
            "import net.minecraft.client.model.geom.builders.PartDefinition;",
            "",
            "/**",
            " * M14: meshes of the small tank animals (nominal length 16 px, standing on y = 0, head to -Z) and of the dwarf",
            " * clawed frog (entity model, ground y = 24). Each model is a set of named top-level groups drawn with their own tint.",
            " * Textures: textures/entity/critters/<model>.png (64x64 or 128x128 box UV).",
            " */",
            "public final class CritterModelDefinitions {",
            "    /** Model names (texture names); the critter models match CritterSpecies.Model. */",
            "    public static final String[] MODELS = {" + ", ".join(f'"{m}"' for m in models) + "};",
            "",
            "    private CritterModelDefinitions() {}",
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
        body.append(java_layer(m, groups, m == "frog", sizes[m]))
        body.append("")
    body.append("}")
    path = JAVA_SRC / "client" / "model" / "CritterModelDefinitions.java"
    path.write_text("\n".join(body) + "\n", encoding="utf-8", newline="\n")
    return path


# ---------------------------------------------------------------------------------------------------------- algae, items

def algae(stage: int) -> np.ndarray:
    """16x16 overlay: green film with spots, alpha grows with the stage (the glass dims)."""
    rng = np.random.default_rng(1400 + stage)
    s = 32
    img = np.zeros((s, s, 4))
    density = [0, 0.3, 0.55, 0.78, 0.95][stage]
    alpha = [0, 0.32, 0.5, 0.66, 0.8][stage]
    yy, xx = np.mgrid[0:s, 0:s]
    noise = (np.sin(xx * 0.7 + rng.random() * 6) * np.sin(yy * 0.55 + rng.random() * 6)
             + np.sin((xx + yy) * 0.31 + rng.random() * 6) * 0.7 + rng.random((s, s)) * 1.2)
    noise = (noise - noise.min()) / (noise.max() - noise.min())
    mask = noise > 1 - density
    green = np.array([0.30, 0.52, 0.16])
    dark = np.array([0.18, 0.34, 0.10])
    for y in range(s):
        for x in range(s):
            if mask[y, x]:
                col = dark if noise[y, x] > 1 - density * 0.35 else green
                img[y, x, :3] = col * (0.9 + 0.2 * rng.random())
                img[y, x, 3] = min(1.0, alpha + 0.15 * (noise[y, x] - (1 - density)))
    return img


def _px(img, x, y, col):
    img[y, x] = col


def scraper_icon() -> np.ndarray:
    img = np.zeros((16, 16, 4), np.uint8)
    handle = (110, 78, 46, 255)
    handle_d = (74, 50, 28, 255)
    blade = (205, 210, 214, 255)
    blade_d = (120, 126, 132, 255)
    edge = (240, 244, 246, 255)
    for i in range(9):  # handle diagonal from bottom left
        x, y = 2 + i, 13 - i
        _px(img, x, y, handle)
        _px(img, x, y + 1, handle_d)
    for y in range(1, 7):  # blade at the top right
        for x in range(9, 15):
            if x - 9 + (6 - y) < 9:
                _px(img, x, y, blade)
    for x in range(9, 15):
        _px(img, x, 1, edge)
    for y in range(1, 7):
        _px(img, 14, y, blade_d)
    _px(img, 10, 5, blade_d)
    _px(img, 11, 4, (80, 150, 60, 255))  # a little algae on the blade
    _px(img, 12, 5, (80, 150, 60, 255))
    return img


def frog_bucket_icon() -> np.ndarray:
    from PIL import Image
    base = np.array(Image.open(ITEM_TEX / "betta_bucket.png").convert("RGBA"))
    img = base.copy()
    # cover the fish with water and draw a small frog
    water = np.array([60, 90, 200, 255], np.uint8)
    for y in range(4, 9):
        for x in range(4, 12):
            if img[y, x, 3] > 0 and not (img[y, x, 0] > 150 and img[y, x, 1] > 150 and img[y, x, 2] > 150 and y < 5):
                img[y, x] = water
    body = np.array([112, 104, 80, 255], np.uint8)
    spot = np.array([60, 56, 42, 255], np.uint8)
    for (x, y) in [(7, 5), (8, 5), (6, 6), (7, 6), (8, 6), (9, 6), (7, 7), (8, 7), (5, 7), (10, 7), (6, 8), (9, 8)]:
        img[y, x] = body
    img[6, 7] = spot
    img[5, 7] = np.array([20, 20, 18, 255], np.uint8)
    img[5, 8] = np.array([20, 20, 18, 255], np.uint8)
    return img


def write_all() -> list:
    written = []
    models, sizes = {}, {}
    for i, (name, fn) in enumerate(MODELS.items()):
        groups = fn()
        sizes[name] = pack(groups)
        models[name] = groups
        written.append(save_png(texture(groups, name, 1400 + i, sizes[name]), CRIT_TEX / f"{name}.png"))
        # M21.5: overlays for the individual looks
        if name == "nerite":
            written.append(save_png(overlay(groups, sizes[name], "fine", 2150), CRIT_TEX / "nerite_lines_fine.png"))
            written.append(save_png(overlay(groups, sizes[name], "wide", 2151), CRIT_TEX / "nerite_lines_wide.png"))
        if name == "apple":
            written.append(save_png(overlay(groups, sizes[name], "bands", 2152), CRIT_TEX / "apple_bands.png"))
    written.append(write_java(models, sizes))
    for st in range(1, 5):
        written.append(save_png(algae(st), ALGAE_TEX / f"algae_{st}.png"))
    written.append(save_png(scraper_icon(), ITEM_TEX / "algae_scraper.png"))
    # the frog bucket: buckets.py (M21, in the style of the active bucket variant)
    return written


if __name__ == "__main__":
    for p in write_all():
        print(p)
