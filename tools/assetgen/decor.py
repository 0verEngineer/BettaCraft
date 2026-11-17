"""M3 decoration: structure templates (.nbt) for the decor items, their item icons, the interior equipment block
textures (heater rod and internal filter segments) and the internal filter icon.

Decor structures are built only from vanilla blocks (PLAN 7.3, ASSETS-HOW option C), so they need no textures and
follow resource packs. Footprints range from 1x1 to 8x8 blocks (= tank pixels at interior scale 16). The icons are
rendered from the same voxels with a small isometric rasterizer and a flat colour per block (no vanilla pixels).
"""
from __future__ import annotations

import gzip
import io
import random
import struct

import numpy as np

from common import BLOCK_TEX, ITEM_TEX, PREVIEW, RES, save_png

STRUCT_DIR = RES / "data" / "bettacraft" / "structure" / "decor"
DATA_VERSION = 3955  # Minecraft 1.21.1


# ---------------------------------------------------------------- minimal NBT writer

def _s(b: io.BytesIO, text: str):
    raw = text.encode("utf-8")
    b.write(struct.pack(">H", len(raw)))
    b.write(raw)


def _payload(b: io.BytesIO, tag):
    kind, value = tag
    if kind == "int":
        b.write(struct.pack(">i", value))
    elif kind == "string":
        _s(b, value)
    elif kind == "list":
        elem_kind, items = value
        b.write(struct.pack(">b", _TYPE[elem_kind] if items else 0))
        b.write(struct.pack(">i", len(items)))
        for it in items:
            _payload(b, (elem_kind, it))
    elif kind == "compound":
        for name, child in value.items():
            b.write(struct.pack(">b", _TYPE[child[0]]))
            _s(b, name)
            _payload(b, child)
        b.write(b"\x00")
    else:
        raise ValueError(kind)


_TYPE = {"int": 3, "string": 8, "list": 9, "compound": 10}


def nbt_bytes(root: dict) -> bytes:
    b = io.BytesIO()
    b.write(b"\x0a")
    _s(b, "")
    _payload(b, ("compound", root))
    out = io.BytesIO()
    # mtime=0 keeps the file byte-identical between runs
    with gzip.GzipFile(fileobj=out, mode="wb", mtime=0) as g:
        g.write(b.getvalue())
    return out.getvalue()


# ---------------------------------------------------------------- voxel builder

class Voxels:
    def __init__(self):
        self.blocks: dict[tuple[int, int, int], str] = {}

    def set(self, x, y, z, state: str):
        self.blocks[(x, y, z)] = state

    def box(self, x0, y0, z0, x1, y1, z1, state):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    self.set(x, y, z, state)

    def remove(self, x, y, z):
        self.blocks.pop((x, y, z), None)

    def size(self):
        xs = [p[0] for p in self.blocks]
        ys = [p[1] for p in self.blocks]
        zs = [p[2] for p in self.blocks]
        return max(xs) - min(xs) + 1, max(ys) - min(ys) + 1, max(zs) - min(zs) + 1

    def normalized(self):
        xs = min(p[0] for p in self.blocks)
        ys = min(p[1] for p in self.blocks)
        zs = min(p[2] for p in self.blocks)
        out = Voxels()
        for (x, y, z), s in self.blocks.items():
            out.set(x - xs, y - ys, z - zs, s)
        return out


def parse_state(state: str):
    if "[" in state:
        name, props = state[:-1].split("[", 1)
        p = dict(kv.split("=") for kv in props.split(","))
    else:
        name, p = state, {}
    if ":" not in name:
        name = "minecraft:" + name
    return name, p


def structure_nbt(v: Voxels) -> bytes:
    v = v.normalized()
    palette: list[str] = []
    index: dict[str, int] = {}
    blocks = []
    for pos in sorted(v.blocks, key=lambda p: (p[1], p[2], p[0])):
        st = v.blocks[pos]
        if st not in index:
            index[st] = len(palette)
            palette.append(st)
        blocks.append({"pos": ("list", ("int", list(pos))), "state": ("int", index[st])})
    pal = []
    for st in palette:
        name, props = parse_state(st)
        c = {"Name": ("string", name)}
        if props:
            c["Properties"] = ("compound", {k: ("string", val) for k, val in sorted(props.items())})
        pal.append(c)
    sx, sy, sz = v.size()
    root = {
        "DataVersion": ("int", DATA_VERSION),
        "size": ("list", ("int", [sx, sy, sz])),
        "palette": ("list", ("compound", pal)),
        "blocks": ("list", ("compound", blocks)),
        "entities": ("list", ("compound", [])),
    }
    return nbt_bytes(root)


# ---------------------------------------------------------------- the decorations

def castle() -> Voxels:
    """Mini castle, 8x8 footprint, 12 high: curtain wall with gate, four towers with battlements, keep with a flag."""
    v = Voxels()
    rnd = random.Random(11)

    def brick():
        return "mossy_stone_bricks" if rnd.random() < 0.25 else "stone_bricks"

    for y in range(0, 5):
        for x in range(8):
            for z in range(8):
                if x in (0, 7) or z in (0, 7):
                    v.set(x, y, z, brick())
    # gate (front = north, z = 0)
    for y in range(0, 3):
        for x in (3, 4):
            v.remove(x, y, 0)
    v.set(3, 3, 0, "stone_brick_stairs[facing=east,half=top]")
    v.set(4, 3, 0, "stone_brick_stairs[facing=west,half=top]")
    v.set(3, 0, 0, "iron_bars")  # portcullis remains
    # towers in the corners, 2x2, up to y 9
    for tx, tz in ((0, 0), (6, 0), (0, 6), (6, 6)):
        for y in range(5, 9):
            for dx in (0, 1):
                for dz in (0, 1):
                    v.set(tx + dx, y, tz + dz, brick())
        for dx, dz in ((0, 0), (1, 1)):
            v.set(tx + dx, 9, tz + dz, "stone_brick_wall")
    # battlements on the curtain wall
    for x in range(2, 6):
        if x % 2 == 0:
            v.set(x, 5, 0, "stone_brick_wall")
            v.set(x, 5, 7, "stone_brick_wall")
    for z in range(2, 6):
        if z % 2 == 1:
            v.set(0, 5, z, "stone_brick_wall")
            v.set(7, 5, z, "stone_brick_wall")
    # keep 4x4 in the middle up to y 8, windows, flag pole + flag
    for y in range(0, 9):
        for x in range(2, 6):
            for z in range(2, 6):
                if x in (2, 5) or z in (2, 5) or y in (0, 8):
                    v.set(x, y, z, brick())
    for y in (4, 6):
        v.set(3, y, 2, "iron_bars")
        v.set(5, y, 4, "iron_bars")
    for x, z in ((2, 2), (5, 2), (2, 5), (5, 5)):
        v.set(x, 9, z, "stone_brick_wall")
    v.set(4, 9, 4, "oak_fence")
    v.set(4, 10, 4, "oak_fence")
    v.set(4, 11, 4, "oak_fence")
    v.set(3, 11, 4, "red_wool")
    v.set(3, 10, 4, "red_wool")
    return v


def shipwreck() -> Voxels:
    """Sunken ship, 8 long, 4 wide, broken mast; listing to one side."""
    v = Voxels()
    rnd = random.Random(23)
    for x in range(8):
        half = 1 if x in (0, 7) else 2
        for z in range(2 - half, 2 + half):
            v.set(x, 0, z, "spruce_planks")
        if x not in (0, 7):
            for z in (0, 3):
                for y in (1, 2):
                    if not (x in (3, 4) and z == 3 and y == 2):  # hole in the hull
                        v.set(x, y, z, "spruce_planks" if rnd.random() > 0.2 else "dark_oak_planks")
    v.set(0, 1, 1, "spruce_stairs[facing=east]")
    v.set(0, 1, 2, "spruce_stairs[facing=east]")
    v.set(7, 1, 1, "spruce_stairs[facing=west]")
    v.set(7, 1, 2, "spruce_stairs[facing=west]")
    for x in range(1, 7):
        if x != 4:
            v.set(x, 2, 1, "spruce_slab[type=bottom]")
    v.set(5, 2, 2, "barrel[facing=up]")
    for y in range(1, 5):
        v.set(3, y, 2, "stripped_spruce_log[axis=y]")
    v.set(2, 4, 2, "stripped_spruce_log[axis=x]")
    v.set(3, 4, 1, "white_wool")
    v.set(3, 3, 1, "white_wool")
    return v


def dino_skull() -> Voxels:
    """Dinosaur skull: 6 long, 4 wide, 4 high, eye sockets and teeth."""
    v = Voxels()
    v.box(0, 1, 0, 5, 3, 3, "bone_block[axis=x]")
    v.box(4, 2, 0, 5, 3, 3, "bone_block[axis=y]")
    for z in (0, 3):
        v.remove(4, 3, z)
        v.remove(3, 3, z)
    v.remove(4, 2, 0)
    v.remove(4, 2, 3)
    v.box(1, 1, 1, 5, 2, 2, "air")
    for p in [p for p, s in v.blocks.items() if s == "air"]:
        v.remove(*p)
    v.box(0, 0, 0, 4, 0, 3, "bone_block[axis=x]")
    for x in (1, 3):
        v.set(x, 1, 0, "polished_diorite_slab[type=bottom]")
        v.set(x, 1, 3, "polished_diorite_slab[type=bottom]")
    v.set(0, 1, 1, "polished_diorite_slab[type=bottom]")
    v.set(0, 1, 2, "polished_diorite_slab[type=bottom]")
    return v


def shipwreck_big() -> Voxels:
    """M21.5: the detailed shipwreck (about twice the old one): a hull of planks in two colours with a keel, a pointed
    bow with a bowsprit, a raised stern castle, a hole in the side facing the player, a broken leaning mast with its
    yard and rags of the sail, barrels on the deck; the whole ship lies tilted on its side."""
    v = Voxels()
    rnd = random.Random(2105)
    plank = lambda: "spruce_planks" if rnd.random() > 0.25 else "dark_oak_planks"
    for x in range(2, 13):
        v.set(x, 0, 3, "stripped_dark_oak_log[axis=x]")  # keel
        for z in (2, 3, 4):
            v.set(x, 1, z, "dark_oak_planks")
        for z in (1, 5):
            v.set(x, 2, z, "spruce_planks")
            v.set(x, 3, z, "dark_oak_planks" if x % 2 else "spruce_planks")  # planks: two colours in strakes
            if rnd.random() > 0.18:
                v.set(x, 4, z, "stripped_spruce_log[axis=x]")  # the rail, broken in places
    # bow: narrowing to the stem, a bowsprit forward
    for y in (1, 2, 3):
        v.set(1, y, 3, "spruce_planks")
        v.set(1, y, 2, "spruce_stairs[facing=east]" if y < 3 else "spruce_slab[type=bottom]")
        v.set(1, y, 4, "spruce_stairs[facing=east]" if y < 3 else "spruce_slab[type=bottom]")
    v.set(0, 2, 3, "spruce_stairs[facing=east]")
    v.set(1, 4, 3, "stripped_spruce_log[axis=x]")
    v.set(0, 4, 3, "stripped_spruce_log[axis=x]")
    # lower deck with an open hatch, barrels
    for x in range(3, 10):
        for z in (2, 3, 4):
            if not (x in (5, 6) and z == 3):
                v.set(x, 3, z, "spruce_slab[type=top]")
    v.set(4, 4, 3, "barrel[facing=up]")
    v.set(8, 4, 4, "barrel[facing=north]")
    # stern castle: walls and a deck one higher, a small window
    for x in (10, 11, 12):
        for z in (1, 5):
            v.set(x, 5, z, plank())
    for z in range(1, 6):
        v.set(12, 2, z, "spruce_planks")
        v.set(12, 3, z, "spruce_planks")
        v.set(12, 4, z, "dark_oak_planks")
        v.set(12, 5, z, "spruce_planks")
    v.remove(12, 4, 3)
    v.set(12, 4, 3, "spruce_trapdoor[facing=west,half=top,open=true]")
    for x in (10, 11):
        for z in range(2, 5):
            v.set(x, 5, z, "spruce_slab[type=bottom]")
    # the hole in the hull (side facing the player, z = 1), dark planks splintered around it
    for x in (6, 7, 8):
        for y in (2, 3):
            v.remove(x, y, 1)
    v.set(5, 3, 1, "spruce_slab[type=top]")
    v.set(9, 2, 1, "spruce_stairs[facing=west,half=top]")
    # broken mast, leaning back, with the yard and rags of the sail
    for y in range(4, 7):
        v.set(7, y, 3, "stripped_spruce_log[axis=y]")
    v.set(8, 7, 3, "stripped_spruce_log[axis=y]")
    v.set(8, 8, 3, "stripped_spruce_log[axis=y]")
    for z in range(1, 6):
        v.set(9, 8, z, "stripped_spruce_log[axis=z]")
    v.set(9, 7, 2, "white_wool")
    v.set(9, 6, 2, "white_wool")
    v.set(9, 7, 4, "white_wool")
    v.set(10, 6, 4, "white_carpet")
    # listing to one side: the far side (z = 5) lies one lower than the side facing the player
    tilted = Voxels()
    for (x, y, z), st in v.blocks.items():
        dy = 1 if z <= 2 else 0
        tilted.set(x, y + dy, z, st)
    # fill the steps under the near side, so the hull does not float
    for x in range(2, 13):
        if (x, 1, 2) in v.blocks:
            tilted.set(x, 1, 2, "dark_oak_planks")
        if (x, 2, 1) in v.blocks:
            tilted.set(x, 2, 1, "spruce_planks")
    return tilted


def dino_skull_big() -> Voxels:
    """M21.5: the detailed dino skull (about twice the old one), seen from the side: a long snout with nostrils at its
    tip, deep eye sockets, a rounded cranium, a row of teeth in the upper jaw and a lower jaw that is a little open with
    its own teeth."""
    v = Voxels()
    bone, up = "bone_block[axis=x]", "bone_block[axis=y]"
    # lower jaw (a U: two branches joined at the chin)
    for x in range(2, 10):
        v.set(x, 0, 2, bone)
        v.set(x, 0, 5, bone)
    for z in range(2, 6):
        v.set(1, 0, z, bone)
        v.set(1, 1, z, "smooth_quartz_slab[type=bottom]")
    for x in range(1, 10):
        v.set(x, 1, 1, "smooth_quartz_slab[type=bottom]" if x > 1 else bone)
        v.set(x, 1, 6, "smooth_quartz_slab[type=bottom]" if x > 1 else bone)
    v.set(9, 1, 2, up)
    v.set(9, 1, 5, up)
    # teeth: lower ones point up, upper ones hang down (pointed dripstone), staggered
    # teeth: white pegs pointing up from the lower jaw, half blocks hanging from the upper one, staggered
    for x in (2, 4, 6, 8):
        for z in (1, 6):
            v.set(x, 2, z, "white_candle[candles=1,lit=false]")
    for x in (1, 3, 5, 7):
        for z in (1, 6):
            v.set(x, 2, z, "smooth_quartz_slab[type=top]")
    v.set(0, 2, 3, "smooth_quartz_slab[type=top]")
    v.set(0, 2, 4, "smooth_quartz_slab[type=top]")
    # upper jaw and snout
    for x in range(0, 11):
        for z in range(1, 7):
            v.set(x, 3, z, bone)
    for x in range(0, 12):
        for z in range(1, 7):
            v.set(x, 4, z, bone)
    # nostrils at the tip of the snout (seen from the front and from above)
    for z in (2, 5):
        v.remove(0, 4, z)
        v.set(0, 4, z, "black_concrete")
    v.set(1, 5, 2, "bone_block[axis=z]")
    v.set(1, 5, 5, "bone_block[axis=z]")
    # face and cranium
    for x in range(3, 12):
        for z in range(1, 7):
            v.set(x, 5, z, bone)
    for x in range(4, 12):
        for z in range(1, 7):
            v.set(x, 6, z, bone)
    for x in range(6, 12):
        for z in range(2, 6):
            v.set(x, 7, z, up)
    v.set(11, 7, 1, up)
    v.set(11, 7, 6, up)
    # eye sockets: open on both sides, dark deep inside
    for x in (6, 7):
        for y in (5, 6):
            for z in (1, 6):
                v.remove(x, y, z)
            v.set(x, y, 2, "black_concrete")
            v.set(x, y, 5, "black_concrete")
    # a ridge over the eyes, the back of the skull
    v.set(5, 7, 2, "smooth_quartz_slab[type=bottom]")
    v.set(5, 7, 5, "smooth_quartz_slab[type=bottom]")
    for z in range(2, 6):
        v.set(11, 3, z, up)
    return v


def volcano() -> Voxels:
    """Volcano cone 7x7, 6 high, glowing crater."""
    v = Voxels()
    rnd = random.Random(5)
    for y in range(6):
        r = 3.4 - y * 0.55
        for x in range(7):
            for z in range(7):
                d = ((x - 3) ** 2 + (z - 3) ** 2) ** 0.5
                if d <= r:
                    inner = d <= r - 1.2 and y >= 3
                    if inner:
                        continue
                    v.set(x, y, z, rnd.choice(["basalt[axis=y]", "blackstone", "basalt[axis=y]", "tuff"]))
    v.set(3, 3, 3, "magma_block")
    v.set(3, 2, 3, "magma_block")
    v.set(2, 3, 3, "magma_block")
    v.set(3, 4, 3, "magma_block")
    return v


def driftwood() -> Voxels:
    """Driftwood: two crossed logs and a root tangle, 6x3x3."""
    v = Voxels()
    for x in range(6):
        v.set(x, 0, 1, "stripped_oak_log[axis=x]")
    for z in range(3):
        v.set(2, 1, z, "stripped_dark_oak_log[axis=z]")
    v.set(4, 1, 1, "stripped_oak_wood[axis=y]")
    v.set(4, 2, 1, "mangrove_roots[waterlogged=false]")
    v.set(5, 1, 1, "mangrove_roots[waterlogged=false]")
    v.set(0, 1, 1, "mangrove_roots[waterlogged=false]")
    return v


def stones() -> Voxels:
    """A cluster of river stones, 3x3, 2 high."""
    v = Voxels()
    v.set(0, 0, 0, "cobblestone")
    v.set(1, 0, 0, "andesite")
    v.set(1, 0, 1, "andesite")
    v.set(0, 0, 1, "tuff")
    v.set(2, 0, 2, "cobblestone")
    v.set(1, 1, 0, "andesite")
    v.set(2, 0, 1, "mossy_cobblestone")
    return v


def moss_ball() -> Voxels:
    """Marimo moss ball: one tank pixel at scale 16."""
    v = Voxels()
    v.set(0, 0, 0, "moss_block")
    return v


def hammock() -> Voxels:
    """Betta hammock: a leaf on a post just below the surface, 3x2 leaf, 6 high."""
    v = Voxels()
    for y in range(5):
        v.set(1, y, 1, "oak_fence")
    for x in range(3):
        for z in range(2):
            v.set(x, 5, z + 1, "oak_leaves[persistent=true]")
    return v


def almond_leaf() -> Voxels:
    """Indian almond leaf lying on the ground (3x2)."""
    v = Voxels()
    v.set(0, 0, 0, "brown_carpet")
    v.set(1, 0, 0, "orange_carpet")
    v.set(2, 0, 0, "brown_carpet")
    v.set(1, 0, 1, "brown_carpet")
    return v


def cave() -> Voxels:
    """Rock cave 6x4, 5 high, with an entrance in front and a hole in the back."""
    v = Voxels()
    rnd = random.Random(31)
    for x in range(6):
        for y in range(5):
            for z in range(4):
                shell = x in (0, 5) or z in (0, 3) or y == 4
                if y == 4 and (x in (0, 5) or z in (0, 3)):
                    continue  # rounded top
                if shell:
                    v.set(x, y, z, rnd.choice(["stone", "tuff", "andesite", "mossy_cobblestone"]))
    for x in (2, 3):
        for y in (0, 1, 2):
            v.remove(x, y, 0)
    v.remove(3, 1, 3)
    v.set(1, 4, 1, "moss_block")
    v.set(4, 4, 2, "moss_carpet")
    return v


def spawning_cave() -> Voxels:
    """M19: fired clay tube for the plecos, 12 long, 6 wide, 6 high (inside 4 x 4), open at the front (x = 0), closed at the back."""
    v = Voxels()
    for x in range(12):
        for z in range(1, 5):
            v.set(x, 0, z, "bettacraft:spawning_cave")
            v.set(x, 5, z, "bettacraft:spawning_cave")
        for y in range(1, 5):
            v.set(x, y, 0, "bettacraft:spawning_cave")
            v.set(x, y, 5, "bettacraft:spawning_cave")
    for y in range(1, 5):
        for z in range(1, 5):
            v.set(11, y, z, "bettacraft:spawning_cave")
    return v


def spawning_mop() -> Voxels:
    """M19: a tuft of green wool strands tied to a stone, 5x5, 10 high."""
    v = Voxels()
    v.set(2, 0, 2, "cobblestone")
    rnd = random.Random(1919)
    for x in range(5):
        for z in range(5):
            d = abs(x - 2) + abs(z - 2)
            if d > 3:
                continue
            top = 9 - d - rnd.randint(0, 1)
            for y in range(1 + max(0, d - 1), top + 1):
                v.set(x, y, z, "bettacraft:spawning_mop")
    return v


import math

# ---------------------------------------------------------------- M28 part c: wrecks, hardscape, roots, caves, kitsch
# The front of a structure (the side that faces the player) is z = 0. Caves for plecos are tubes of
# bettacraft:spawning_cave (clusters of at least 20 blocks, TankmateSpots), with the inner 4 x 4 of the M19 tube so the
# fish swim in (M21 pathfinding).


def _clay_tube(v: Voxels, x0: int, y0: int, z0: int, length: int, along: str = "z", open_high: bool = False,
               block: str = "bettacraft:spawning_cave"):
    """A tube of 6 x 6 (inside 4 x 4) and `length`, open at one end (the low one unless `open_high`), closed at the other."""
    closed = 0 if open_high else length - 1
    for a in range(length):
        for u in range(6):
            for w in range(6):
                ring = u in (0, 5) or w in (0, 5)
                if not ring and a != closed:
                    continue
                if along == "z":
                    v.set(x0 + u, y0 + w, z0 + a, block)
                else:
                    v.set(x0 + a, y0 + w, z0 + u, block)


def rowing_boat() -> Voxels:
    """M28: a sunken rowing boat, keel up on the bottom (11 x 8, 5 high): a low cave under the hull for catfish, open at
    the broken stern; a plank missing in the bottom, a broken oar beside it."""
    v = Voxels()
    rnd = random.Random(2801)
    plank = lambda: "oak_planks" if rnd.random() > 0.2 else "spruce_planks"
    for x in range(1, 10):
        taper = x in (1, 9)
        if not taper:
            for z in (0, 6):
                v.set(x, 0, z, "stripped_oak_log[axis=x]")  # the gunwale on the ground
                v.set(x, 1, z, plank())
            for z in (1, 5):
                v.set(x, 2, z, plank())
            for z in (2, 3, 4):
                v.set(x, 3, z, plank())
            v.set(x, 4, 3, "stripped_spruce_log[axis=x]")  # the keel on top
        else:
            for z in (1, 5):
                v.set(x, 1, z, plank())
            for z in (2, 4):
                v.set(x, 2, z, plank())
            v.set(x, 3, 3, plank())
            v.set(x, 4, 3, "stripped_spruce_log[axis=x]")
    for y in (1, 2, 3):
        v.set(0, y, 3, "stripped_spruce_log[axis=y]")  # the stem
    # the stern is broken off: the cave opens there
    for z in range(1, 6):
        for y in range(0, 4):
            v.remove(9, y, z)
    v.remove(9, 4, 3)
    v.set(9, 1, 0, "spruce_slab[type=top]")
    v.set(9, 1, 6, "spruce_slab[type=top]")
    v.remove(6, 3, 3)  # a missing plank in the bottom
    v.set(6, 3, 2, "oak_stairs[facing=north,half=top]")
    v.set(4, 4, 2, "moss_carpet")
    # the oar beside the boat
    for x in range(2, 8):
        v.set(x, 0, 7, "stripped_spruce_log[axis=x]")
    v.set(8, 0, 7, "spruce_slab[type=bottom]")
    v.set(9, 0, 7, "spruce_slab[type=bottom]")
    return v


def ship_bow() -> Voxels:
    """M28: the bow of a sailing ship, broken off amidships (14 x 10, 11 high): a planked hull with a rounded bilge that
    narrows to the stem, a bowsprit, a forecastle deck, the stump of the foremast with a rag of sail, a hole in the side,
    and in the open hold a cargo of clay pipe that plecos breed in (a spawning cave, its mouth at the broken end)."""
    v = Voxels()
    rnd = random.Random(2802)
    plank = lambda: "spruce_planks" if rnd.random() > 0.3 else "dark_oak_planks"
    L = 12  # hull x = 0 .. 11 (bow at x = 0), bowsprit in front of it
    for x in range(L):
        half = [1, 2, 3, 4][x] if x < 4 else 5  # half width at the rail: the bow narrows to the stem
        z0, z1 = 5 - half, 4 + half
        top = 8 + (1 if x < 3 else 0)  # the sheer rises to the bow
        v.set(x, 0, 4, "stripped_dark_oak_log[axis=x]")  # the keel
        v.set(x, 0, 5, "stripped_dark_oak_log[axis=x]")
        if half >= 4:
            # rounded bilge: bottom z0+2 .. z1-2 at y = 1, curving out to the full width at y = 3
            for z in range(z0 + 2, z1 - 1):
                v.set(x, 1, z, "dark_oak_planks")
            v.set(x, 1, z0 + 1, "spruce_stairs[facing=south,half=top]")
            v.set(x, 1, z1 - 1, "spruce_stairs[facing=north,half=top]")
            v.set(x, 2, z0 + 1, plank())
            v.set(x, 2, z1 - 1, plank())
            v.set(x, 2, z0, "spruce_stairs[facing=south,half=top]")
            v.set(x, 2, z1, "spruce_stairs[facing=north,half=top]")
            ys = range(3, top)
        else:
            for z in range(z0 + 1, z1):
                v.set(x, 1, z, "dark_oak_planks")
            ys = range(1, top)
        for y in ys:
            v.set(x, y, z0, plank())
            v.set(x, y, z1, plank())
        v.set(x, top, z0, "stripped_spruce_log[axis=x]")  # the rail
        v.set(x, top, z1, "stripped_spruce_log[axis=x]")
        if x < 6:
            for z in range(z0 + 1, z1):
                v.set(x, 8, z, "spruce_slab[type=top]")  # forecastle deck
    # stem and bowsprit
    for y in range(1, 10):
        v.set(0, y, 4, "stripped_dark_oak_log[axis=y]")
        v.set(0, y, 5, "stripped_dark_oak_log[axis=y]")
    v.set(-1, 9, 4, "stripped_spruce_log[axis=x]")
    v.set(-2, 10, 4, "stripped_spruce_log[axis=x]")
    v.set(-1, 10, 4, "stripped_spruce_log[axis=x]")
    # foremast stump on the forecastle, a rag of sail on its yard
    for y in range(9, 11):
        v.set(3, y, 4, "stripped_spruce_log[axis=y]")
    for z in range(2, 8):
        v.set(3, 10, z, "stripped_spruce_log[axis=z]")
    v.set(3, 9, 6, "white_wool")
    v.set(4, 9, 6, "white_carpet")
    # a hole in the side facing the player (z = 0): the clay pipe shows through
    for x in (7, 8):
        for y in (4, 5):
            v.remove(x, y, 0)
    v.set(6, 5, 0, "spruce_slab[type=top]")
    v.set(9, 4, 0, "spruce_stairs[facing=west,half=bottom]")
    # the broken end: splintered planks at x = 10, 11
    for x in (10, 11):
        for y in range(4, 9):
            for z in (0, 9):
                if rnd.random() < (0.45 if x == 11 else 0.2) * (y - 2) / 3:
                    v.remove(x, y, z)
    # cargo: a clay pipe along the hold, closed towards the bow, open at the broken end
    _clay_tube(v, 5, 2, 2, 7, along="x", open_high=True)
    v.set(6, 8, 7, "barrel[facing=up]")
    return v


def holey_rock() -> Voxels:
    """M28: holey rock (Lochgestein), 7 x 6, 5 high: a pale lump of limestone (calcite, a little dripstone) full of holes
    with two tunnels through it; it hardens the water like other limestone."""
    v = Voxels()
    rnd = random.Random(2803)
    for x in range(7):
        for y in range(5):
            for z in range(6):
                d = ((x - 3) / 3.6) ** 2 + (y / 5.2) ** 2 + ((z - 2.5) / 3.1) ** 2
                if d <= 1.0 + rnd.uniform(-0.12, 0.05):
                    v.set(x, y, z, "calcite" if rnd.random() > 0.15 else "dripstone_block")
    for z in range(6):  # tunnel front to back
        for y in (1, 2):
            v.remove(2, y, z)
            v.remove(3, y, z)
    for x in range(7):  # and across
        v.remove(x, 1, 3)
    for (x, y, z) in list(v.blocks):
        if rnd.random() < 0.18 and y > 0 and ((x + 1, y, z) not in v.blocks or (x, y + 1, z) not in v.blocks or (x, y, z - 1) not in v.blocks):
            v.remove(x, y, z)  # pits in the surface
    return v


def slate_cave() -> Voxels:
    """M28: a cave of stacked slate plates, 8 x 6, 5 high: two walls of layered plates under a wide roof plate, open to
    the front, for catfish and plecos to hide."""
    v = Voxels()
    rnd = random.Random(2804)
    plate = lambda: rnd.choice(["deepslate_tiles", "polished_deepslate", "cobbled_deepslate", "deepslate_bricks"])
    for z in range(6):
        for y in range(3):
            for x in (0, 1, 6, 7):
                if (x in (0, 7)) and y == 2 and z in (0, 5):
                    continue
                v.set(x, y, z, plate())
    for x in range(2, 6):
        v.set(x, 0, 5, plate())
        v.set(x, 1, 5, plate())
    for x in range(0, 8):
        for z in range(0, 6):
            if (x in (0, 7)) and z in (0, 5):
                continue
            v.set(x, 3, z, "deepslate_tile_slab[type=bottom]" if x in (0, 7) else plate())
    for x in range(1, 6):
        for z in range(1, 5):
            v.set(x, 4, z, "polished_deepslate_slab[type=bottom]")
    return v


def seiryu_stone() -> Voxels:
    """M28: Seiryu rocks, 8 x 6, 9 high: three grey, sharp-edged rocks (a tall one and two small ones) with white
    calcite veins."""
    v = Voxels()
    rnd = random.Random(2805)
    rocks = [(2.5, 2.5, 2.4, 9), (6.0, 1.5, 1.6, 5), (5.5, 4.5, 1.4, 3)]
    for cx, cz, r, h in rocks:
        lean = rnd.uniform(-0.15, 0.15)
        for y in range(h):
            rr = r * (1 - y / h) ** 0.7 + 0.35
            ox = cx + lean * y
            for x in range(int(ox - rr) - 1, int(ox + rr) + 2):
                for z in range(int(cz - rr) - 1, int(cz + rr) + 2):
                    # facetted: max-norm and diamond mixed give flat faces and sharp edges
                    dx, dz = abs(x + 0.5 - ox), abs(z + 0.5 - cz)
                    d = 0.55 * max(dx, dz) + 0.45 * (dx + dz) / 1.4
                    if d <= rr and 0 <= x < 8 and 0 <= z < 6:
                        vein = (x + 2 * y + z) % 6 == 0 and rnd.random() < 0.7
                        v.set(x, y, z, "calcite" if vein else rnd.choice(["stone", "stone", "andesite", "tuff"]))
    return v


def dragon_stone() -> Voxels:
    """M28: dragon stone (Ohko), 8 x 6, 6 high: clay-coloured, deeply wrinkled lumps with many holes, an arch to swim
    through."""
    v = Voxels()
    rnd = random.Random(2806)
    clay = lambda: rnd.choice(["terracotta", "terracotta", "brown_terracotta", "packed_mud", "light_gray_terracotta"])
    for x in range(8):
        for y in range(6):
            for z in range(6):
                d1 = ((x - 2) / 2.6) ** 2 + (y / 6.3) ** 2 + ((z - 2.5) / 2.9) ** 2
                d2 = ((x - 5.8) / 2.3) ** 2 + (y / 4.4) ** 2 + ((z - 3) / 2.4) ** 2
                if min(d1, d2) <= 1.0 + rnd.uniform(-0.15, 0.05):
                    v.set(x, y, z, clay())
    for z in range(6):  # the arch
        for y in (0, 1):
            v.remove(3, y, z)
            v.remove(4, y, z)
    for (x, y, z) in list(v.blocks):
        exposed = sum((x + dx, y + dy, z + dz) not in v.blocks for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, 0, 1), (0, 0, -1)))
        if exposed and rnd.random() < 0.28:
            v.remove(x, y, z)  # the holes and wrinkles
    return v


def lava_rock() -> Voxels:
    """M28: lava rock, 6 x 5, 5 high: porous red-brown and black rock with a small cave."""
    v = Voxels()
    rnd = random.Random(2807)
    for x in range(6):
        for y in range(5):
            for z in range(5):
                d = ((x - 2.5) / 3.1) ** 2 + (y / 5.0) ** 2 + ((z - 2) / 2.7) ** 2
                if d <= 1.0 + rnd.uniform(-0.15, 0.05):
                    v.set(x, y, z, rnd.choice(["blackstone", "red_terracotta", "red_terracotta", "brown_terracotta", "basalt[axis=y]"]))
    for z in range(0, 4):
        for x in (2, 3):
            v.remove(x, 0, z)
            v.remove(x, 1, z)
    for (x, y, z) in list(v.blocks):
        if y > 0 and rnd.random() < 0.15:
            v.remove(x, y, z)  # pores
    return v


def _line(v: Voxels, a, b, state: str):
    """Blocks along the straight line from a to b (fine steps, no gaps)."""
    n = int(max(abs(b[i] - a[i]) for i in range(3)) * 3) + 1
    for i in range(n + 1):
        t = i / n
        v.set(*(int(round(a[k] + (b[k] - a[k]) * t)) for k in range(3)), state)


def mangrove_root() -> Voxels:
    """M28: a mangrove root, 8 x 7, 7 high: a short trunk raised on arching prop roots, with room for fish underneath."""
    v = Voxels()
    top = (3.5, 4.0, 3.0)
    for y in range(4, 7):
        for x in (3, 4):
            v.set(x, y, 3, "mangrove_log[axis=y]")
    v.set(3, 6, 2, "mangrove_log[axis=y]")
    for i, ang in enumerate((0.3, 1.5, 2.7, 3.9, 5.1)):
        r = 3.6 if i % 2 else 3.2
        prev = top
        for s in range(1, 9):
            t = s / 8
            p = (3.5 + math.cos(ang) * r * t, 4.0 * (1 - t ** 1.6) + 0.2, 3.0 + math.sin(ang) * r * t * 0.9)
            _line(v, prev, p, "mangrove_roots")
            prev = p
    # keep it inside 8 x 7
    for (x, y, z) in list(v.blocks):
        if not (0 <= x < 8 and 0 <= z < 7):
            v.remove(x, y, z)
    return v


def mangrove_stilt_roots() -> Voxels:
    """M49: mangrove stilt roots, 14 x 14, 18 high: a trunk high up on six long arching stilt roots that reach the floor far
    out, a forked crown stump on top; it stands out of the water of a paludarium, and mudskippers climb on the roots."""
    v = Voxels()
    rnd = random.Random(4921)
    cx, cz = 6.5, 6.5
    foot = 9
    for y in range(foot, 16):
        for x in (6, 7):
            v.set(x, y, 6, "mangrove_log[axis=y]")
    for y in range(15, 18):
        v.set(5 if y < 17 else 4, y, 7, "mangrove_log[axis=y]")
        v.set(8 if y < 17 else 9, y, 5, "mangrove_log[axis=y]")
    for i in range(6):
        ang = i * math.pi / 3 + rnd.uniform(-0.25, 0.25)
        r = rnd.uniform(5.2, 6.4)
        start = (cx, foot + rnd.uniform(0.0, 2.5), cz)
        prev = start
        for s in range(1, 13):
            t = s / 12
            p = (cx + math.cos(ang) * r * t, start[1] * (1 - t ** 1.7), cz + math.sin(ang) * r * t)
            _line(v, prev, p, "mangrove_roots")
            prev = p
        v.set(int(round(prev[0])), 0, int(round(prev[2])), "muddy_mangrove_roots")
    for (x, y, z) in list(v.blocks):
        if not (0 <= x < 14 and 0 <= z < 14):
            v.remove(x, y, z)
    return v


def spiderwood() -> Voxels:
    """M28: spiderwood, 8 x 8, 10 high: a light root stock with fine branches that spread out and fork again and again."""
    v = Voxels()
    rnd = random.Random(2809)

    def branch(p, ang, tilt, length, depth):
        dx = math.cos(ang) * math.sin(tilt)
        dz = math.sin(ang) * math.sin(tilt)
        dy = math.cos(tilt)
        q = (p[0] + dx * length, p[1] + dy * length, p[2] + dz * length)
        _line(v, p, q, "stripped_oak_wood" if depth < 1 else "oak_wood" if depth == 1 else "stripped_oak_wood")
        if depth >= 3:
            return
        for k in range(2):
            branch(q, ang + (k - 0.5) * rnd.uniform(0.7, 1.3), min(1.25, tilt + rnd.uniform(-0.05, 0.3)), length * 0.7, depth + 1)

    for x in (3, 4):
        for z in (3, 4):
            v.set(x, 0, z, "stripped_oak_wood")
    for i in range(4):
        branch((3.5, 0.6, 3.5), i * 1.57 + rnd.uniform(-0.4, 0.4), rnd.uniform(0.45, 0.8), 3.2, 0)
    for (x, y, z) in list(v.blocks):
        if not (0 <= x < 8 and 0 <= z < 8 and y < 10):
            v.remove(x, y, z)
    return v


def bogwood_bridge() -> Voxels:
    """M28: a bogwood bridge, 8 x 4, 5 high: a dark, gnarled root arching over the bottom, fish swim through below."""
    v = Voxels()
    for i in range(41):
        x = i / 40 * 7
        y = 3.6 * math.sin(math.pi * i / 40) ** 0.8
        z = 1.5 + 0.6 * math.sin(i / 6)
        for dz in (0, 1):
            v.set(int(round(x)), int(round(y)), int(round(z)) + dz, "dark_oak_wood" if (i // 5) % 3 else "stripped_dark_oak_wood")
    for x in (0, 1, 6, 7):
        for z in (1, 2):
            v.set(x, 0, z, "dark_oak_wood")  # thick feet
    v.set(0, 1, 2, "dark_oak_wood")
    v.set(7, 1, 1, "dark_oak_wood")
    _line(v, (5, 3, 2), (6, 4, 0), "stripped_dark_oak_wood")  # a side twig
    _line(v, (2, 3, 2), (1, 4, 3), "stripped_dark_oak_wood")
    return v


def coconut_cave() -> Voxels:
    """M28: half a coconut shell lying on its rim, 6 x 6, 3 high, with an entrance at the front (a cave for small
    fish such as dwarf cichlids)."""
    v = Voxels()
    for x in range(6):
        for y in range(3):
            for z in range(6):
                d = ((x - 2.5) ** 2 + (y * 1.25) ** 2 + (z - 2.5) ** 2) ** 0.5
                if 1.9 <= d <= 3.0:
                    v.set(x, y, z, "brown_wool" if (x + y + z) % 4 == 0 else "brown_terracotta")
    for x in (2, 3):
        for y in (0, 1):
            v.remove(x, y, 0)
    v.set(1, 0, 0, "spruce_slab[type=bottom]")  # the broken rim
    return v


def clay_tubes() -> Voxels:
    """M28: two clay tubes side by side, 13 x 9, 6 high, open to the front: each is a spawning cave for plecos."""
    v = Voxels()
    _clay_tube(v, 0, 0, 0, 9)
    _clay_tube(v, 7, 0, 1, 7)
    return v


def shrimp_tubes() -> Voxels:
    """M28: a stack of thin terracotta tubes for shrimp, 6 x 5, 6 high: young shrimp hide in them."""
    v = Voxels()
    colours = ["terracotta", "orange_terracotta", "white_terracotta"]
    for i, (x0, y0) in enumerate(((0, 0), (3, 0), (1, 3))):
        for z in range(5 if i != 2 else 4):
            for u in range(3):
                for w in range(3):
                    if u == 1 and w == 1:
                        continue
                    v.set(x0 + u, y0 + w, z + (1 if i == 2 else 0), colours[i])
    return v


def ruin_column() -> Voxels:
    """M28: a broken antique column, 7 x 4, 9 high: a plinth, a fluted shaft broken off at the top, a fallen drum and
    its capital beside it, moss on the stones."""
    v = Voxels()
    for x in range(4):
        for z in range(4):
            v.set(x, 0, z, "polished_diorite")
    for y in range(1, 9):
        for x in (1, 2):
            for z in (1, 2):
                if y >= 7 and (x, z) != (1, 1) and not (y == 7 and (x, z) == (2, 1)):
                    continue
                v.set(x, y, z, "diorite" if (x + z + y) % 2 else "polished_diorite")
    for x in (4, 5, 6):
        for z in (1, 2):
            v.set(x, 0, z, "polished_diorite" if x != 5 else "diorite")
    v.set(5, 0, 3, "smooth_quartz_slab[type=bottom]")
    v.set(6, 0, 3, "smooth_quartz_slab[type=bottom]")
    v.set(0, 1, 0, "moss_carpet")
    v.set(3, 1, 3, "moss_carpet")
    v.set(5, 1, 1, "moss_carpet")
    return v


def diver_helmet() -> Voxels:
    """M28: an old diving helmet on its breastplate, 7 x 7, 7 high, brass-coloured, with round side windows and an open
    face window: small fish swim in and out."""
    v = Voxels()
    for x in range(7):
        for z in range(7):
            if ((x - 3) / 3.5) ** 2 + ((z - 3) / 3.0) ** 2 <= 1.0:
                v.set(x, 0, z, "orange_terracotta")
                if ((x - 3) / 3.5) ** 2 + ((z - 3) / 3.0) ** 2 > 0.45:
                    v.set(x, 1, z, "orange_terracotta")
    for x, z in ((1, 1), (5, 1), (1, 5), (5, 5)):
        v.set(x, 1, z, "yellow_terracotta")  # the bolts
    for x in range(7):
        for y in range(2, 7):
            for z in range(7):
                d = ((x - 3) ** 2 + (y - 3.8) ** 2 + (z - 3) ** 2) ** 0.5
                if 2.1 <= d <= 3.1:
                    v.set(x, y, z, "orange_terracotta")
    for x in (2, 3, 4):  # the face window: open
        for y in (3, 4, 5):
            v.remove(x, y, 0)
            v.remove(x, y, 1)
    for x in range(1, 6):  # the brass rim of the face window
        for y in range(2, 7):
            if x in (1, 5) or y in (2, 6):
                if (x, y) in ((1, 2), (5, 2), (1, 6), (5, 6)):
                    continue
                v.set(x, y, 1, "yellow_terracotta")
    for z in (3,):  # the side windows
        v.set(0, 4, z, "light_gray_stained_glass")
        v.set(6, 4, z, "light_gray_stained_glass")
    v.set(3, 7, 3, "yellow_terracotta")  # the valve on top
    v.set(3, 7, 4, "yellow_terracotta")
    return v


def treasure_chest() -> Voxels:
    """M28: a treasure chest with its lid open, 6 x 6, 5 high, full of gold coins that spill out at the front; an air
    stone hidden among the coins lets bubbles rise when an air pump drives it."""
    v = Voxels()
    for x in range(6):
        for z in range(1, 5):
            for y in range(3):
                shell = x in (0, 5) or z in (1, 4) or y == 0
                if shell:
                    band = x in (0, 5) or (x in (1, 4) and z in (1, 4))
                    v.set(x, y, z, "dark_oak_planks" if band else "spruce_planks")
            if 0 < x < 5 and 1 < z < 4:
                v.set(x, 1, z, "yellow_concrete")  # the coins inside
    v.set(2, 2, 2, "yellow_concrete")
    v.set(3, 2, 3, "yellow_concrete")
    v.set(2, 2, 3, "bettacraft:air_stone")  # bubbles: open above, the lid stands at the back
    v.set(3, 2, 2, "yellow_carpet")
    v.set(2, 2, 1, "yellow_concrete")  # the lock
    v.set(3, 2, 1, "yellow_concrete")
    for x in range(6):  # the open lid, upright behind the chest
        for y in (3, 4):
            v.set(x, y, 5, "dark_oak_planks" if x in (0, 5) else "spruce_planks")
    v.set(0, 3, 4, "spruce_slab[type=bottom]")
    v.set(5, 3, 4, "spruce_slab[type=bottom]")
    for x in range(2, 6):  # coins spilt in front of it
        v.set(x, 0, 0, "yellow_carpet")
    v.set(1, 0, 0, "yellow_concrete")
    return v


M28_DECOR = {
    "rowing_boat": rowing_boat,
    "ship_bow": ship_bow,
    "holey_rock": holey_rock,
    "slate_cave": slate_cave,
    "seiryu_stone": seiryu_stone,
    "dragon_stone": dragon_stone,
    "lava_rock": lava_rock,
    "mangrove_root": mangrove_root,
    "spiderwood": spiderwood,
    "bogwood_bridge": bogwood_bridge,
    "coconut_cave": coconut_cave,
    "clay_tubes": clay_tubes,
    "shrimp_tubes": shrimp_tubes,
    "ruin_column": ruin_column,
    "diver_helmet": diver_helmet,
    "treasure_chest": treasure_chest,
    "mangrove_stilt_roots": mangrove_stilt_roots,  # M49
}


DECOR = {
    "castle": castle,
    # M21.5: the detailed versions; the old small ones stay as *_small (features.detailedDecor = false)
    "shipwreck": shipwreck_big,
    "dino_skull": dino_skull_big,
    "shipwreck_small": shipwreck,
    "dino_skull_small": dino_skull,
    "volcano": volcano,
    "driftwood": driftwood,
    "stones": stones,
    "moss_ball": moss_ball,
    "hammock": hammock,
    "almond_leaf": almond_leaf,
    "cave": cave,
    "spawning_cave": spawning_cave,
    "spawning_mop": spawning_mop,
    **M28_DECOR,  # M28 part c
}

# ---------------------------------------------------------------- icons from the voxels

COLORS = {
    "stone_bricks": "#7d7d7f", "mossy_stone_bricks": "#6f7c5a", "stone_brick_wall": "#8a8a8c", "stone_brick_stairs": "#7d7d7f",
    "iron_bars": "#5c5f63", "oak_fence": "#9a7a48", "red_wool": "#b3312c", "spruce_planks": "#73522f",
    "dark_oak_planks": "#4a311a", "spruce_stairs": "#73522f", "spruce_slab": "#7c5a35", "barrel": "#8b6a3e",
    "stripped_spruce_log": "#8f6d45", "white_wool": "#e4e4e4", "bone_block": "#e6dfc6", "polished_diorite_slab": "#cfcfd2",
    "basalt": "#4e4e56", "blackstone": "#2e2a30", "tuff": "#6c6d66", "magma_block": "#e0641c",
    "stripped_oak_log": "#b29257", "stripped_dark_oak_log": "#6b5234", "stripped_oak_wood": "#b29257",
    "mangrove_roots": "#5a4630", "cobblestone": "#828282", "andesite": "#8e8f8f", "mossy_cobblestone": "#6f8a5c",
    "moss_block": "#5a7b2e", "oak_leaves": "#4f8a2a", "brown_carpet": "#7a4f2c", "orange_carpet": "#d0752a",
    "moss_carpet": "#5d7f30", "stone": "#7f7f7f", "pointed_dripstone": "#e8e2d0", "white_candle": "#f0f0e8", "black_concrete": "#101014",
    "smooth_quartz_slab": "#ece6dc", "stripped_dark_oak_log": "#6b5234", "spruce_trapdoor": "#6a4c2c", "white_carpet": "#e8e8e8", "spawning_cave": "#b0623a", "spawning_mop": "#3e9a3a",
}


# M28 part c
COLORS.update({
    "oak_planks": "#a8864f", "stripped_oak_log": "#b29257", "oak_stairs": "#a8864f", "dark_oak_planks": "#4a311a",
    "calcite": "#dcdcd6", "dripstone_block": "#866b5c", "deepslate_tiles": "#37373a", "polished_deepslate": "#48484b",
    "cobbled_deepslate": "#505055", "deepslate_bricks": "#46464a", "deepslate_tile_slab": "#37373a", "polished_deepslate_slab": "#48484b",
    "terracotta": "#985e43", "brown_terracotta": "#4d3323", "packed_mud": "#8e6b53", "light_gray_terracotta": "#876a61",
    "red_terracotta": "#8f3d2e", "mangrove_log": "#6b3c2a", "mangrove_roots": "#4a3b2a", "stripped_oak_wood": "#b29257",
    "oak_wood": "#6d5532", "dark_oak_wood": "#3e2e1a", "stripped_dark_oak_wood": "#6b5234", "brown_wool": "#724728",
    "orange_terracotta": "#a15325", "white_terracotta": "#d1b2a1", "yellow_terracotta": "#ba8523", "polished_diorite": "#c1c1c3",
    "diorite": "#bcbcbc", "light_gray_stained_glass": "#9a9a9a", "yellow_concrete": "#f0af15", "yellow_carpet": "#f8c527",
    "air_stone": "#3d6fa8", "white_wool": "#e4e4e4",
})


def _color(state: str) -> np.ndarray:
    name = parse_state(state)[0].split(":")[1]
    h = COLORS.get(name, "#ff00ff").lstrip("#")
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=np.float64)


def iso_icon(v: Voxels, size: int = 16, supersample: int = 4) -> np.ndarray:
    """Isometric flat-shaded rendering of the voxels, fitted into size x size, with a dark outline."""
    v = v.normalized()
    S = size * supersample
    sx, sy, sz = v.size()
    # projected extents of the bounding box: u = x - z, w = (x + z) / 2 - y
    corners = [(x, y, z) for x in (0, sx) for y in (0, sy) for z in (0, sz)]
    us = [x - z for x, y, z in corners]
    ws = [(x + z) * 0.5 - y for x, y, z in corners]
    span = max(max(us) - min(us), (max(ws) - min(ws)))
    scale = (S - 2 * supersample) / span
    ou = -min(us) * scale + (S - (max(us) - min(us)) * scale) / 2
    ow = -min(ws) * scale + (S - (max(ws) - min(ws)) * scale) / 2
    img = np.zeros((S, S, 4))
    depth = np.full((S, S), -1e9)
    faces = [((0, 1, 0), 1.0), ((0, 0, -1), 0.8), ((-1, 0, 0), 0.62)]  # top, north (left), west (right) as seen
    yy, xx = np.mgrid[0:S, 0:S]
    for (x, y, z), st in sorted(v.blocks.items(), key=lambda kv: kv[0][0] + kv[0][2] + kv[0][1]):
        col = _color(st)
        for normal, shade in ((("top"), 1.0), (("left"), 0.78), (("right"), 0.6)):
            if normal == "top" and (x, y + 1, z) in v.blocks:
                continue
            if normal == "left" and (x, y, z + 1) in v.blocks:
                continue
            if normal == "right" and (x + 1, y, z) in v.blocks:
                continue
            if normal == "top":
                quad = [(x, y + 1, z), (x + 1, y + 1, z), (x + 1, y + 1, z + 1), (x, y + 1, z + 1)]
            elif normal == "left":
                quad = [(x, y, z + 1), (x + 1, y, z + 1), (x + 1, y + 1, z + 1), (x, y + 1, z + 1)]
            else:
                quad = [(x + 1, y, z), (x + 1, y, z + 1), (x + 1, y + 1, z + 1), (x + 1, y + 1, z)]
            pts = np.array([((a - c) * scale + ou, ((a + c) * 0.5 - b) * scale + ow) for a, b, c in quad])
            # point-in-convex-polygon test
            inside = np.ones((S, S), bool)
            for i in range(4):
                (x0, y0), (x1, y1) = pts[i], pts[(i + 1) % 4]
                cross = (x1 - x0) * (yy + 0.5 - y0) - (y1 - y0) * (xx + 0.5 - x0)
                inside &= cross <= 0
            if not inside.any():
                inside = np.ones((S, S), bool)
                for i in range(4):
                    (x0, y0), (x1, y1) = pts[i], pts[(i + 1) % 4]
                    cross = (x1 - x0) * (yy + 0.5 - y0) - (y1 - y0) * (xx + 0.5 - x0)
                    inside &= cross >= 0
            d = x + z + y + (0.5 if normal == "top" else 0.3)
            m = inside & (d >= depth)
            depth[m] = d
            img[m, :3] = col / 255.0 * shade
            img[m, 3] = 1.0
    # downsample
    small = img.reshape(size, supersample, size, supersample, 4).mean(axis=(1, 3))
    a = small[..., 3]
    rgb = np.where(a[..., None] > 0, small[..., :3] / np.maximum(a[..., None], 1e-6), 0)
    out = np.zeros((size, size, 4))
    solid = a >= 0.45
    out[solid, :3] = rgb[solid]
    out[solid, 3] = 1.0
    # outline
    pad = np.pad(solid, 1)
    neigh = pad[:-2, 1:-1] | pad[2:, 1:-1] | pad[1:-1, :-2] | pad[1:-1, 2:]
    edge = neigh & ~solid
    out[edge, :3] = np.array([0.12, 0.12, 0.14])
    out[edge, 3] = 1.0
    return out


# ---------------------------------------------------------------- equipment textures

def _noise(seed, shape):
    return np.random.default_rng(seed).random(shape)


def heater_segment(part: str) -> np.ndarray:
    """Glass tube with an orange heating coil; the top segment is the black control cap."""
    img = np.zeros((16, 16, 4))
    n = _noise(7 if part != "top" else 8, (16, 16))
    if part == "top":
        img[..., :3] = 0.16 + 0.04 * n[..., None]
        img[2:5, 6:10, :3] = np.array([0.85, 0.2, 0.15])
        img[..., 3] = 1
        return img
    glass = np.array([0.78, 0.88, 0.9])
    img[..., :3] = glass * (0.92 + 0.06 * n[..., None])
    img[:, 0:2, :3] = glass * 1.05
    yy = np.arange(16)
    coil = (yy % 4) < 2
    img[coil, 5:11, :3] = np.array([0.91, 0.5, 0.16])
    img[~coil, 6:10, :3] = np.array([0.66, 0.27, 0.09])
    img[..., 3] = 1
    return np.clip(img, 0, 1)


def cooler_segment(part: str) -> np.ndarray:
    """M9: glass tube with ice-blue cooling fins; the top segment is the cap with a blue LED."""
    img = np.zeros((16, 16, 4))
    n = _noise(9 if part != "top" else 10, (16, 16))
    if part == "top":
        img[..., :3] = 0.16 + 0.04 * n[..., None]
        img[2:5, 6:10, :3] = np.array([0.25, 0.7, 1.0])
        img[..., 3] = 1
        return img
    glass = np.array([0.8, 0.9, 0.95])
    img[..., :3] = glass * (0.92 + 0.06 * n[..., None])
    img[:, 0:2, :3] = glass * 1.05
    yy = np.arange(16)
    fin = (yy % 3) == 0
    img[:, 6:10, :3] = np.array([0.25, 0.5, 0.75])
    img[fin, 4:12, :3] = np.array([0.55, 0.82, 0.96])
    img[..., 3] = 1
    return np.clip(img, 0, 1)


def filter_segment(part: str) -> np.ndarray:
    """Dark grey filter housing with intake slots; the top has the outlet."""
    img = np.zeros((16, 16, 4))
    n = _noise(11 if part != "top" else 12, (16, 16))
    img[..., :3] = 0.22 + 0.05 * n[..., None]
    if part == "top":
        img[5:11, 5:11, :3] = 0.08
        img[6:10, 6:10, :3] = np.array([0.25, 0.45, 0.6])
    else:
        for y in range(2, 15, 3):
            img[y, 2:14, :3] = 0.07
        img[:, 0, :3] = 0.3
        img[:, 15, :3] = 0.14
    img[..., 3] = 1
    return img


def internal_filter_icon() -> np.ndarray:
    v = Voxels()
    v.box(0, 0, 0, 1, 3, 1, "blackstone")
    return iso_icon(v)


# ---------------------------------------------------------------- output

def write_all() -> list:
    out = []
    STRUCT_DIR.mkdir(parents=True, exist_ok=True)
    sheet = []
    for name, f in DECOR.items():
        v = f()
        sx, sy, sz = v.size()
        assert max(sx, sz) <= (14 if name in ("spawning_cave", "shipwreck", "dino_skull", "rowing_boat", "ship_bow", "clay_tubes", "mangrove_stilt_roots") else 8), f"{name}: footprint {sx}x{sz} too big"
        p = STRUCT_DIR / f"{name}.nbt"
        p.write_bytes(structure_nbt(v))
        out.append(p)
        # M5: the item icons are hand-authored pixel art (decor_icons.py); the isometric render stays as a debug sheet
        sheet.append(iso_icon(v, 64, 2))
    for part in ("anchor", "middle", "top"):
        out.append(save_png(heater_segment(part), BLOCK_TEX / f"heater_segment_{part}.png"))
        out.append(save_png(cooler_segment(part), BLOCK_TEX / f"cooler_segment_{part}.png"))
        out.append(save_png(filter_segment(part), BLOCK_TEX / f"filter_segment_{part}.png"))
    # preview sheet (debug)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    row = np.concatenate(sheet, axis=1)
    out.append(save_png(row, PREVIEW / "decor_sheet.png"))
    return out


def sizes() -> dict[str, tuple[int, int, int]]:
    return {name: f().size() for name, f in DECOR.items()}


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
    print(sizes())
