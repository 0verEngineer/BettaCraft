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
    """M19: fired clay tube for the plecos, 8 long, 5 wide, 4 high, open at the front (x = 0), closed at the back."""
    v = Voxels()
    for x in range(8):
        for z in range(5):
            v.set(x, 0, z, "bettacraft:spawning_cave")
        for y in (1, 2):
            v.set(x, y, 0, "bettacraft:spawning_cave")
            v.set(x, y, 4, "bettacraft:spawning_cave")
        for z in (1, 2, 3):
            v.set(x, 3, z, "bettacraft:spawning_cave")
    for y in (1, 2):
        for z in (1, 2, 3):
            v.set(7, y, z, "bettacraft:spawning_cave")
    return v


def spawning_mop() -> Voxels:
    """M19: a tuft of green wool strands tied to a stone, 3x3, 6 high."""
    v = Voxels()
    v.set(1, 0, 1, "cobblestone")
    for y in range(1, 6):
        v.set(1, y, 1, "bettacraft:spawning_mop")
    for (x, z) in ((0, 1), (2, 1), (1, 0), (1, 2)):
        for y in range(2, 6):
            v.set(x, y, z, "bettacraft:spawning_mop")
    for (x, z) in ((0, 0), (2, 2), (0, 2), (2, 0)):
        for y in range(3, 6):
            v.set(x, y, z, "bettacraft:spawning_mop")
    return v


DECOR = {
    "castle": castle,
    "shipwreck": shipwreck,
    "dino_skull": dino_skull,
    "volcano": volcano,
    "driftwood": driftwood,
    "stones": stones,
    "moss_ball": moss_ball,
    "hammock": hammock,
    "almond_leaf": almond_leaf,
    "cave": cave,
    "spawning_cave": spawning_cave,
    "spawning_mop": spawning_mop,
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
    "moss_carpet": "#5d7f30", "stone": "#7f7f7f", "spawning_cave": "#b0623a", "spawning_mop": "#3e9a3a",
}


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
        assert max(sx, sz) <= 8, f"{name}: footprint {sx}x{sz} exceeds 8x8"
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
