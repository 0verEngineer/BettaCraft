"""Offline audit of the mod's textures against the models that use them (M53). Reads only; writes debug sheets.

Entity models
    The geometry is parsed from the generated Java layer definitions (``client/model/*ModelDefinitions.java``, what the
    game really bakes), the textures each layer is drawn with come from the generators (``tankmates.SPECIES``,
    ``terrarium_animals.TEXTURES``, ``critters.MODELS``, the betta and guppy mask lists) plus the hired aquarist on the
    vanilla player layout. Every cuboid face gets its box-UV rectangle exactly as ``ModelPart.Cube`` computes it (float
    sizes, no rounding: west/min x ``u..u+d``, north/min z ``u+d..u+d+w``, east/max x ``u+d+w..u+2d+w``, south/max z
    ``u+2d+w..u+2d+2w`` in the rows ``v+d..v+d+h``; top/min y ``u+d..u+d+w`` and bottom/max y ``u+d+w..u+d+2w`` in
    ``v..v+d``; the model's y points down). Checks per model and texture:

    * ``hole``: a face of a solid cuboid (all three sizes > 0) samples transparent texels (alpha < 0.1, discarded by the
      cutout render types: one sees through the animal). Edge slivers of fractional faces count by their area.
    * ``empty plane``: a width-0 plane (a fin) whose two faces show nothing at all.
    * ``bounds``: a face rectangle reaches outside the texture.
    * ``overlap``: two cuboids of different size or offset sample the same texels (a face shows another part's paint);
      identical rectangles (deliberately shared texOffs of left/right parts) are listed as ``shared``.
    * ``stretch``: never possible with plain box UV (the UV size is the cuboid size); reported if a layer used a texture
      size different from the PNG (the whole texture would be scaled).
    * ``seam``: neighbouring segments of a chain (the snake, the kuhli loach) whose touching side faces differ sharply
      in mean colour along the seam (a pattern that does not continue).
    * ``size``: the PNG size differs from the size the layer declares.

    Mask textures (betta patterns, guppy gene masks, critter overlays) only get the bounds and size checks; their union
    with the base layers must cover every solid face (checked for the betta and the guppy).

Block and item textures
    Every PNG under ``textures/block`` and ``textures/item``: fully transparent images, sizes that are not 16 x 16
    multiples, non-square images without an animation ``.mcmeta``, ``.mcmeta`` frame counts / frame indices that do not
    fit, block and item models (``common/src/main/resources`` and ``common/src/generated/resources``) that reference
    missing textures, and textures nothing references (models, Java, JSON, the generators' Java-side names).

Usage (repo root):
    py tools/assetgen/check_textures.py                    # report only
    py tools/assetgen/check_textures.py --sheets DIR       # also writes a contact sheet per model and texture to DIR
    py tools/assetgen/check_textures.py --only corn_snake  # only models / textures whose name contains the text
Exit code 1 when a defect (not an info line) is found.
"""
from __future__ import annotations

import argparse
import json
import math
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from common import JAVA_SRC, RES, ROOT, TEX  # noqa: E402

MODEL_JAVA = JAVA_SRC / "client" / "model"
GENERATED_RES = ROOT / "common" / "src" / "generated" / "resources"
ALPHA_CUT = 0.1  # the cutout render types discard alpha < 0.1


# ================================================================================================ Java layer parser

@dataclass
class Cuboid:
    part: str
    u: float
    v: float
    origin: tuple  # addBox x, y, z (local)
    size: tuple  # w, h, d
    matrix: np.ndarray  # 4x4 local -> model
    path: str = ""
    mirror: bool = False
    grow: tuple = (0.0, 0.0, 0.0)  # CubeDeformation: moves the vertices, not the UV

    @property
    def gmin(self) -> np.ndarray:
        return np.array(self.origin, float) - np.array(self.grow, float)

    @property
    def gmax(self) -> np.ndarray:
        return np.array(self.origin, float) + np.array(self.size, float) + np.array(self.grow, float)

    @property
    def solid(self) -> bool:
        return all(s > 1e-6 for s in self.size)

    @property
    def plane(self) -> bool:
        return sum(1 for s in self.size if s <= 1e-6) == 1


@dataclass
class Layer:
    name: str
    tex_w: int
    tex_h: int
    cuboids: list = field(default_factory=list)


def _split_args(s: str) -> list[str]:
    out, depth, cur = [], 0, []
    for ch in s:
        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            depth -= 1
        if ch == "," and depth == 0:
            out.append("".join(cur).strip())
            cur = []
        else:
            cur.append(ch)
    if cur:
        out.append("".join(cur).strip())
    return out


def _num(s: str, consts: dict) -> float:
    s = s.strip()
    if s in consts:
        return float(consts[s])
    s = re.sub(r"(?<=\d)[FfDd]\b", "", s)
    return float(eval(s, {"__builtins__": {}}, {"Math": math}))  # literal floats and simple expressions only


def _call_args(text: str, start: int) -> tuple[str, int]:
    """Text inside the parentheses that open at ``start``; returns (inside, index after the closing one)."""
    depth = 0
    for i in range(start, len(text)):
        if text[i] == "(":
            depth += 1
        elif text[i] == ")":
            depth -= 1
            if depth == 0:
                return text[start + 1:i], i + 1
    raise ValueError("unbalanced")


def _pose(expr: str, consts) -> np.ndarray:
    expr = expr.strip()
    off, rot = (0.0, 0.0, 0.0), (0.0, 0.0, 0.0)
    if expr.startswith("PartPose.offsetAndRotation"):
        a = [_num(x, consts) for x in _split_args(_call_args(expr, expr.index("("))[0])]
        off, rot = tuple(a[:3]), tuple(a[3:6])
    elif expr.startswith("PartPose.offset"):
        off = tuple(_num(x, consts) for x in _split_args(_call_args(expr, expr.index("("))[0]))
    elif expr.startswith("PartPose.rotation"):
        rot = tuple(_num(x, consts) for x in _split_args(_call_args(expr, expr.index("("))[0]))
    elif expr != "PartPose.ZERO":
        raise ValueError("pose " + expr)
    m = np.eye(4)
    m[:3, :3] = _rot_zyx(*rot)
    m[:3, 3] = off
    return m


def _rot_zyx(rx, ry, rz) -> np.ndarray:
    """ModelPart.translateAndRotate: Quaternionf.rotationZYX(zRot, yRot, xRot) = Rz * Ry * Rx."""
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


def _cubes(expr: str, consts) -> list[tuple]:
    """CubeListBuilder chain -> [(u, v, origin, size, mirror, grow)]."""
    out, u, v, mirror = [], 0.0, 0.0, False
    for m in re.finditer(r"\.(texOffs|addBox|mirror)\(", expr):
        args, _ = _call_args(expr, m.end() - 1)
        a = _split_args(args) if args.strip() else []
        if m.group(1) == "texOffs":
            u, v = _num(a[0], consts), _num(a[1], consts)
        elif m.group(1) == "mirror":
            mirror = (a[0].strip() == "true") if a else True
        else:
            if a and a[0].startswith('"'):
                a = a[1:]
            grow = (0.0, 0.0, 0.0)
            if len(a) > 6:
                dm = re.match(r"new CubeDeformation\((.*)\)$", a[6].strip())
                if not dm:
                    raise ValueError("addBox argument: " + args)
                g = [_num(x, consts) for x in _split_args(dm.group(1))]
                grow = tuple(g) if len(g) == 3 else (g[0],) * 3
            nums = [_num(x, consts) for x in a[:6]]
            out.append((u, v, tuple(nums[:3]), tuple(nums[3:6]), mirror, grow))
    return out


def parse_layers(path: Path) -> dict[str, Layer]:
    text = path.read_text(encoding="utf-8")
    consts = {m.group(1): m.group(2) for m in re.finditer(r"static final int (\w+) = (\d+);", text)}
    layers = {}
    for m in re.finditer(r"public static LayerDefinition (create\w*Layer)\(\)\s*\{", text):
        body_start = m.end()
        depth, i = 1, body_start
        while depth:
            depth += {"{": 1, "}": -1}.get(text[i], 0)
            i += 1
        body = text[body_start:i - 1]
        size = re.search(r"LayerDefinition\.create\(mesh,\s*(\w+),\s*(\w+)\)", body)
        if not size:
            continue
        layer = Layer(m.group(1), int(_num(size.group(1), consts)), int(_num(size.group(2), consts)))
        frames = {"root": np.eye(4)}
        paths = {"root": ""}
        for stmt in body.split(";"):
            stmt = " ".join(stmt.split())
            if "addOrReplaceChild" not in stmt:
                continue
            lhs = None
            am = re.match(r"(?:PartDefinition\s+)?(\w+)\s*=\s*(\w+)\.addOrReplaceChild", stmt)
            pm = re.search(r"(\w+)\.addOrReplaceChild\(", stmt)
            if am:
                lhs = am.group(1)
            parent = pm.group(1)
            args = _split_args(_call_args(stmt, pm.end() - 1)[0])
            name = args[0].strip('"')
            local = _pose(args[2], consts)
            world = frames[parent] @ local
            ppath = (paths[parent] + "/" + name).lstrip("/")
            for (u, v, origin, sz, mirror, grow) in _cubes(args[1], consts):
                layer.cuboids.append(Cuboid(name, u, v, origin, sz, world, ppath, mirror, grow))
            if lhs:
                frames[lhs] = world
                paths[lhs] = ppath
        layers[layer.name] = layer
    return layers


# ================================================================================================ box UV faces

FACES = ("top", "bottom", "west", "north", "east", "south")


def face_rects(cu: Cuboid) -> dict:
    """Face -> (u1, v1, u2, v2) exactly as ModelPart.Cube (float sizes)."""
    w, h, d = cu.size
    u, v = cu.u, cu.v
    return {
        "top": (u + d, v, u + d + w, v + d),          # Direction.DOWN polygon, min y (the model's y points down)
        "bottom": (u + d + w, v, u + d + 2 * w, v + d),
        "west": (u, v + d, u + d, v + d + h),          # min x
        "north": (u + d, v + d, u + d + w, v + d + h),  # min z
        "east": (u + d + w, v + d, u + 2 * d + w, v + d + h),  # max x
        "south": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }


def visible_faces(cu: Cuboid) -> list[str]:
    w, h, d = cu.size
    out = []
    for f in FACES:
        fw, fh = {"top": (w, d), "bottom": (w, d), "west": (d, h), "east": (d, h), "north": (w, h), "south": (w, h)}[f]
        if fw > 1e-6 and fh > 1e-6:
            out.append(f)
    return out


def texel_cover(rect):
    """[(tu, tv, area)] of the texels a rectangle covers."""
    u1, v1, u2, v2 = rect
    out = []
    for tv in range(math.floor(v1 + 1e-6), math.ceil(v2 - 1e-6)):
        for tu in range(math.floor(u1 + 1e-6), math.ceil(u2 - 1e-6)):
            a = (min(tu + 1, u2) - max(tu, u1)) * (min(tv + 1, v2) - max(tv, v1))
            if a > 1e-6:
                out.append((tu, tv, a))
    return out


def face_uv(cu: Cuboid, face: str, p: np.ndarray) -> tuple:
    """Texture coordinates of a local point on a face (arrays ok), the vanilla mapping; mirror flips x; a grown box
    (CubeDeformation) keeps its UV size, so the point is first scaled back onto the box of the UV size."""
    x0, y0, z0 = cu.origin
    w, h, d = cu.size
    gmin, gmax = cu.gmin, cu.gmax
    q = []
    for k, (o, n) in enumerate(zip(cu.origin, cu.size)):
        span = gmax[k] - gmin[k]
        q.append(o + (p[..., k] - gmin[k]) * (n / span) if span > 1e-9 else p[..., k] * 0 + o)
    x, y, z = q
    if cu.mirror:
        x = 2 * x0 + w - x
        face = {"west": "east", "east": "west"}.get(face, face)
    x1, z1 = x0 + w, z0 + d
    u, v = cu.u, cu.v
    if face == "top":
        return u + d + (x - x0), v + (z1 - z)
    if face == "bottom":
        return u + d + w + (x - x0), v + (z1 - z)
    if face == "west":
        return u + (z1 - z), v + d + (y - y0)
    if face == "north":
        return u + d + (x - x0), v + d + (y - y0)
    if face == "east":
        return u + d + w + (z - z0), v + d + (y - y0)
    return u + 2 * d + w + (x1 - x), v + d + (y - y0)


# ================================================================================================ what uses what

@dataclass
class Target:
    group: str  # report heading
    layer: Layer
    textures: list  # [(Path, kind)] kind: "solid" (must cover every face) or "mask" (partial on purpose)
    union: list = field(default_factory=list)  # textures whose union must cover every face
    chain: str | None = None  # part name prefix of a segment chain for the seam check


def _png(rel: str) -> Path:
    return TEX / rel


def player_layer() -> Layer:
    """The vanilla player model with wide arms (PlayerModel.createMesh, 64x64), base parts and the second layer."""
    L = Layer("player_wide", 64, 64)

    def add(name, u, v, origin, size, off, grow=0.0):
        m = np.eye(4)
        m[:3, 3] = off
        # CubeDeformation grows the geometry but not the UV
        L.cuboids.append(Cuboid(name, u, v, origin, size, m, name, grow=(grow,) * 3))

    add("head", 0, 0, (-4, -8, -4), (8, 8, 8), (0, 0, 0))
    add("hat", 32, 0, (-4, -8, -4), (8, 8, 8), (0, 0, 0), 0.5)
    add("body", 16, 16, (-4, 0, -2), (8, 12, 4), (0, 0, 0))
    add("jacket", 16, 32, (-4, 0, -2), (8, 12, 4), (0, 0, 0), 0.25)
    add("right_arm", 40, 16, (-3, -2, -2), (4, 12, 4), (-5, 2, 0))
    add("right_sleeve", 40, 32, (-3, -2, -2), (4, 12, 4), (-5, 2, 0), 0.25)
    add("left_arm", 32, 48, (-1, -2, -2), (4, 12, 4), (5, 2, 0))
    add("left_sleeve", 48, 48, (-1, -2, -2), (4, 12, 4), (5, 2, 0), 0.25)
    add("right_leg", 0, 16, (-2, 0, -2), (4, 12, 4), (-1.9, 12, 0))
    add("right_pants", 0, 32, (-2, 0, -2), (4, 12, 4), (-1.9, 12, 0), 0.25)
    add("left_leg", 16, 48, (-2, 0, -2), (4, 12, 4), (1.9, 12, 0))
    add("left_pants", 0, 48, (-2, 0, -2), (4, 12, 4), (1.9, 12, 0), 0.25)
    return L


OUTER_PLAYER = {"hat", "jacket", "right_sleeve", "left_sleeve", "right_pants", "left_pants"}


def targets() -> list[Target]:
    import critters
    import tankmates
    import terrarium_animals

    out = []
    betta = parse_layers(MODEL_JAVA / "BettaModelDefinitions.java")
    base = [(_png("entity/betta/shade.png"), "solid")]
    masks = [(p, "mask") for p in sorted((TEX / "entity" / "betta").glob("*.png")) if p.stem != "shade"]
    for name, layer in betta.items():
        out.append(Target("betta", layer, base + masks,
                          union=[_png("entity/betta/region_body.png"), _png("entity/betta/region_fins.png")]))

    crit = parse_layers(MODEL_JAVA / "CritterModelDefinitions.java")
    overlays = {"nerite": ["nerite_lines_fine", "nerite_lines_wide"], "apple": ["apple_bands"],
                "shrimp": ["shrimp_bee_a", "shrimp_bee_s", "shrimp_bee_ss", "shrimp_bee_sss", "shrimp_tiger", "shrimp_dots"],
                "crayfish": ["crayfish_marble"]}
    for m in critters.MODELS:
        layer = crit[f"create{m.capitalize()}Layer"]
        tex = [(_png(f"entity/critters/{m}.png"), "solid")] + [(_png(f"entity/critters/{o}.png"), "mask") for o in overlays.get(m, [])]
        out.append(Target("critter " + m, layer, tex))

    terra = parse_layers(MODEL_JAVA / "TerrariumModelDefinitions.java")
    for m in terrarium_animals.MODELS:
        layer = terra[f"create{m.replace('_', '').capitalize()}Layer"]
        names = list(terrarium_animals.TEXTURES.get(m, {m: None}).keys())
        out.append(Target("terrarium " + m, layer, [(_png(f"entity/terrarium/{n}.png"), "solid") for n in names],
                          chain="seg" if m == "corn_snake" else None))

    tm = parse_layers(MODEL_JAVA / "TankmateModelDefinitions.java")
    by_model: dict[str, list] = {}
    for sp, model, varieties, dimorphic in tankmates.SPECIES:
        for var in varieties:
            for female in ((False, True) if dimorphic else (False,)):
                n = tankmates.texture_name(sp, varieties, var, dimorphic, female)
                by_model.setdefault(model, []).append((_png(f"entity/tankmates/{n}.png"), "solid"))
    for model, tex in by_model.items():
        layer = tm[f"create{model.capitalize()}Layer"]
        out.append(Target("tankmate " + model, layer, tex, chain="seg" if model == "kuhli" else None))

    guppy = parse_layers(MODEL_JAVA / "GuppyModelDefinitions.java")["createLayer"]
    gtex = [(p, "mask") for p in sorted((TEX / "entity" / "tankmates" / "guppy").glob("*.png"))]
    out.append(Target("guppy genetics", guppy, gtex,
                      union=[_png("entity/tankmates/guppy/body.png"), _png("entity/tankmates/guppy/fins.png")]))

    pl = player_layer()
    out.append(Target("hired aquarist (player model, wide arms)", pl,
                      [(p, "solid") for p in sorted((TEX / "entity" / "hired_aquarist").glob("*.png"))]))
    return out


# ================================================================================================ analysis

def load(path: Path) -> np.ndarray:
    return np.asarray(Image.open(path).convert("RGBA"), dtype=np.float64) / 255.0


@dataclass
class Finding:
    severity: str  # "defect" / "info"
    text: str


def analyse(t: Target, only: str | None) -> list[tuple[str, list[Finding]]]:
    results = []
    layer = t.layer
    cubes = layer.cuboids
    # overlaps (texture independent)
    lay_find: list[Finding] = []
    claims: dict[tuple, list] = {}
    for i, cu in enumerate(cubes):
        for f in visible_faces(cu):
            for tu, tv, a in texel_cover(face_rects(cu)[f]):
                claims.setdefault((tu, tv), []).append((i, f, a))
    pair_cnt: dict[tuple, int] = {}
    shared_cnt: dict[tuple, int] = {}
    for (tu, tv), lst in claims.items():
        ids = {}
        for i, f, a in lst:
            if a >= 0.2:
                ids.setdefault(i, f)
        keys = sorted(ids)
        for x in range(len(keys)):
            for y in range(x + 1, len(keys)):
                a, b = cubes[keys[x]], cubes[keys[y]]
                same = (a.u, a.v, a.size) == (b.u, b.v, b.size)
                k = (keys[x], keys[y])
                (shared_cnt if same else pair_cnt)[k] = (shared_cnt if same else pair_cnt).get(k, 0) + 1
    for (i, j), n in sorted(pair_cnt.items()):
        a, b = cubes[i], cubes[j]
        lay_find.append(Finding("defect", f"overlap: {a.path} (uv {a.u:g},{a.v:g} size {_s(a.size)}) and {b.path} "
                                          f"(uv {b.u:g},{b.v:g} size {_s(b.size)}) share {n} texels"))
    shared = sorted({(cubes[i].u, cubes[i].v) for (i, j) in shared_cnt})
    if shared:
        names = sorted({cubes[i].path.split('/')[-1] + "=" + cubes[j].path.split('/')[-1] for (i, j) in shared_cnt})
        lay_find.append(Finding("info", f"shared UV (identical rectangles, on purpose): {', '.join(names)}"))
    for cu in cubes:
        for f in visible_faces(cu):
            u1, v1, u2, v2 = face_rects(cu)[f]
            if u1 < -1e-6 or v1 < -1e-6 or u2 > layer.tex_w + 1e-6 or v2 > layer.tex_h + 1e-6:
                lay_find.append(Finding("defect", f"bounds: {cu.path} {f} rect {u1:g},{v1:g}..{u2:g},{v2:g} outside "
                                                  f"{layer.tex_w}x{layer.tex_h}"))
    for cu in cubes:
        geo = cu.gmax - cu.gmin
        for k, axis in enumerate("xyz"):
            if cu.size[k] > 1e-6 and geo[k] / cu.size[k] >= 1.6 and geo[k] - cu.size[k] > 0.6 and not (layer.name == "player_wide"):
                lay_find.append(Finding("defect", f"stretch: {cu.path} {axis}: {geo[k]:g} model px over {cu.size[k]:g} texels"))
    results.append((f"layer {layer.name} ({len(cubes)} cuboids, {layer.tex_w}x{layer.tex_h})", lay_find))

    for path, kind in t.textures:
        if only and only not in path.stem and only not in t.group and only not in layer.name.lower():
            continue
        fs: list[Finding] = []
        if not path.exists():
            results.append((path.name, [Finding("defect", "missing texture file")]))
            continue
        img = load(path)
        H, W = img.shape[:2]
        if (W, H) != (layer.tex_w, layer.tex_h):
            fs.append(Finding("defect", f"size/stretch: PNG {W}x{H}, layer declares {layer.tex_w}x{layer.tex_h} "
                                        f"(UVs are scaled by the declared size)"))
        if kind == "solid":
            fs += holes(cubes, img, outer=OUTER_PLAYER if layer.name == "player_wide" else set())
            fs += bleed(cubes, claims, img)
            if t.chain:
                fs += seams(cubes, img, t.chain)
        results.append((f"{path.relative_to(TEX).as_posix()} [{kind}]", fs))
    if t.union and (not only or only in t.group):
        imgs = [load(p) for p in t.union]
        a = np.max(np.stack([im[..., 3] for im in imgs]), axis=0)
        comb = np.zeros_like(imgs[0])
        comb[..., 3] = a
        comb[..., :3] = 1
        fs = holes(cubes, comb, planes_too=True)
        results.append(("union " + " + ".join(p.name for p in t.union), fs))
    return results


def bleed(cubes, claims, img) -> list[Finding]:
    """Fractional cuboid sizes make neighbouring faces share texels (the rectangles meet inside a texel): the texel is
    painted for the face that covers more of it, the other face shows a sliver of foreign paint along its edge (e.g. the
    belly colour on the top edge of a flank). Reported where the foreign texels differ clearly from the face's own colour
    and cover a visible part of the face."""
    H, W = img.shape[:2]
    owner = {}
    for key, lst in claims.items():
        best = max(lst, key=lambda e: e[2])  # first wins on ties, like critters.box_texels
        owner[key] = (best[0], best[1])
    out = []
    worst = 0.0
    nfaces = 0
    for i, cu in enumerate(cubes):
        if not cu.solid:
            continue
        for f in visible_faces(cu):
            cover = texel_cover(face_rects(cu)[f])
            total = sum(a for *_, a in cover)
            own = [img[tv, tu, :3] for tu, tv, a in cover if 0 <= tu < W and 0 <= tv < H and owner.get((tu, tv)) == (i, f)]
            if not own or total <= 0:
                continue
            mean = np.mean(own, axis=0)
            foreign = 0.0
            for tu, tv, a in cover:
                if owner.get((tu, tv)) == (i, f) or not (0 <= tu < W and 0 <= tv < H):
                    continue
                if np.linalg.norm(img[tv, tu, :3] - mean) > 0.25:
                    foreign += a
            frac = foreign / total
            if frac > 0:
                nfaces += 1
                worst = max(worst, frac)
            if frac >= 0.08:
                o = owner
                others = sorted({f"{cubes[o[(tu, tv)][0]].path.split('/')[-1]}.{o[(tu, tv)][1]}" for tu, tv, a in cover
                                 if o.get((tu, tv)) not in (None, (i, f))})
                out.append(Finding("defect", f"bleed: {cu.path} {f} ({_s(cu.size)}): {frac * 100:.0f}% of the face shows "
                                             f"texels painted for {', '.join(others)}"))
    if nfaces:
        out.append(Finding("info", f"{nfaces} face(s) sample a sliver of a neighbouring face's paint (worst {worst * 100:.0f}%)"))
    return out


def _s(size):
    return "x".join(f"{s:g}" for s in size)


def holes(cubes, img, outer=frozenset(), planes_too=False) -> list[Finding]:
    H, W = img.shape[:2]
    out = []
    for cu in cubes:
        if cu.part in outer:
            continue
        if cu.solid:
            for f in visible_faces(cu):
                cover = texel_cover(face_rects(cu)[f])
                total = sum(a for *_, a in cover)
                if total <= 0:
                    continue
                clear = sum(a for tu, tv, a in cover if not (0 <= tu < W and 0 <= tv < H) or img[tv, tu, 3] < ALPHA_CUT)
                frac = clear / total
                if frac > 0.005:
                    what = "unpainted face" if frac > 0.9 else "hole"
                    out.append(Finding("defect", f"{what}: {cu.path} {f} {frac * 100:.0f}% transparent "
                                                 f"({clear:.2f} of {total:.2f} texel area)"))
        elif cu.plane:
            vis = visible_faces(cu)
            seen = 0.0
            for f in vis:
                seen += sum(a for tu, tv, a in texel_cover(face_rects(cu)[f]) if 0 <= tu < W and 0 <= tv < H and img[tv, tu, 3] >= ALPHA_CUT)
            if seen <= 1e-6:
                out.append(Finding("defect" if planes_too or True else "info", f"empty plane: {cu.path} ({_s(cu.size)}) shows nothing"))
    return out


def seams(cubes, img, prefix) -> list[Finding]:
    """Neighbouring chain segments: compare the mean colour of each segment's side and bottom faces (a pattern that
    stops at one segment shows as a jump)."""
    def seg_no(c):
        for part in c.path.split("/"):
            m = re.fullmatch(prefix + r"(\d+)", part)
            if m:
                return int(m.group(1))
        return None
    segs = sorted([c for c in cubes if seg_no(c) is not None], key=seg_no)
    out = []
    for f in ("west", "east", "bottom", "top"):
        means = []
        for cu in segs:
            cover = texel_cover(face_rects(cu)[f])
            tot = sum(a for *_, a in cover)
            if tot <= 0:
                continue
            means.append(sum(img[tv, tu, :3] * a for tu, tv, a in cover) / tot)
        if len(means) > 1:
            lum = [float(np.dot(m, [0.3, 0.59, 0.11])) for m in means]
            spread = max(lum) - min(lum)
            out.append(Finding("info", f"{f} faces mean luminance per segment: " + " ".join(f"{x:.2f}" for x in lum)
                               + f" (spread {spread:.2f})"))
    # left/right symmetry: west and east faces of the same segment should look alike (mirror images)
    for cu in segs:
        r = face_rects(cu)
        a, b = _face_pixels(img, r["west"]), _face_pixels(img, r["east"])
        if a is not None and b is not None and a.shape == b.shape:
            diff = float(np.abs(a[..., :3] - b[:, ::-1, :3]).mean())
            if diff > 0.25:
                out.append(Finding("defect", f"asymmetric sides: {cu.path} west and east differ by {diff:.2f} on average"))
    return out


def _face_pixels(img, rect):
    u1, v1, u2, v2 = rect
    a, b, c, d = math.floor(u1 + 1e-6), math.floor(v1 + 1e-6), math.ceil(u2 - 1e-6), math.ceil(v2 - 1e-6)
    if c <= a or d <= b:
        return None
    return img[b:d, a:c]


# ================================================================================================ contact sheets

VIEWS = {  # name: (ray direction, image-x axis, image-y axis), model space (y down, head to -z)
    "top (from -y)": ((0, 1, 0), (0, 0, 1), (1, 0, 0)),
    "bottom (from +y)": ((0, -1, 0), (0, 0, 1), (-1, 0, 0)),
    "side +x": ((-1, 0, 0), (0, 0, 1), (0, 1, 0)),
    "side -x": ((1, 0, 0), (0, 0, 1), (0, 1, 0)),
    "front (from -z)": ((0, 0, 1), (-1, 0, 0), (0, 1, 0)),
    "back (from +z)": ((0, 0, -1), (1, 0, 0), (0, 1, 0)),
}
SHADE = {"top": 1.0, "bottom": 0.55, "west": 0.75, "east": 0.75, "north": 0.88, "south": 0.88}


def _bounds(cubes):
    pts = []
    for cu in cubes:
        lo, hi = cu.gmin, cu.gmax
        for x in (lo[0], hi[0]):
            for y in (lo[1], hi[1]):
                for z in (lo[2], hi[2]):
                    pts.append((cu.matrix @ np.array([x, y, z, 1]))[:3])
    pts = np.array(pts)
    return pts.min(0), pts.max(0)


def render_view(cubes, img, view, px_per_unit, lo, hi):
    """Orthographic ray cast through every cuboid (no culling, cutout at alpha 0.1). Holes of solid cuboids in front of
    the visible surface are drawn magenta."""
    dvec, ex, ey = (np.array(v, float) for v in VIEWS[view])
    corners = np.array([[a, b, c] for a in (lo[0], hi[0]) for b in (lo[1], hi[1]) for c in (lo[2], hi[2])])
    xs, ys = corners @ ex, corners @ ey
    W = max(1, int(math.ceil((xs.max() - xs.min()) * px_per_unit)) + 4)
    H = max(1, int(math.ceil((ys.max() - ys.min()) * px_per_unit)) + 4)
    jj, ii = np.mgrid[0:H, 0:W]
    sx = xs.min() + (ii + 0.5 - 2) / px_per_unit
    sy = ys.min() + (jj + 0.5 - 2) / px_per_unit
    centre = (lo + hi) / 2
    far = float(np.linalg.norm(hi - lo)) + 10
    base = centre - ex * (centre @ ex) - ey * (centre @ ey) - dvec * (centre @ dvec)
    orig = base[None, None, :] + sx[..., None] * ex + sy[..., None] * ey - dvec * far
    best_t = np.full((H, W), np.inf)
    col = np.zeros((H, W, 4))
    hole_t = np.full((H, W), np.inf)
    th, tw = img.shape[:2]
    for cu in cubes:
        inv = np.linalg.inv(cu.matrix)
        o = orig @ inv[:3, :3].T + inv[:3, 3]
        dl = inv[:3, :3] @ dvec
        bmin, bmax = cu.gmin, cu.gmax
        tmin = np.full((H, W), -np.inf)
        tmax = np.full((H, W), np.inf)
        ent = np.zeros((H, W), int)
        ext = np.zeros((H, W), int)
        ok = np.ones((H, W), bool)
        for k in range(3):
            if abs(dl[k]) < 1e-9:
                ok &= (o[..., k] >= bmin[k] - 1e-9) & (o[..., k] <= bmax[k] + 1e-9)
                continue
            t1 = (bmin[k] - o[..., k]) / dl[k]
            t2 = (bmax[k] - o[..., k]) / dl[k]
            near = np.minimum(t1, t2)
            farr = np.maximum(t1, t2)
            # face ids: 2k = min side, 2k+1 = max side
            near_id = np.where(t1 < t2, 2 * k, 2 * k + 1) if dl[k] != 0 else 0
            far_id = np.where(t1 < t2, 2 * k + 1, 2 * k)
            upd = near > tmin
            tmin = np.where(upd, near, tmin)
            ent = np.where(upd, near_id, ent)
            upd2 = farr < tmax
            tmax = np.where(upd2, farr, tmax)
            ext = np.where(upd2, far_id, ext)
        ok &= tmax >= tmin - 1e-9
        for which, tt, fid in (("in", tmin, ent), ("out", tmax, ext)):
            m = ok & np.isfinite(tt)
            if not m.any():
                continue
            p = o + dl * tt[..., None]
            for k, fname in enumerate(("west", "east", "top", "bottom", "north", "south")):
                mk = m & (fid == k)
                if not mk.any() or fname not in visible_faces(cu):
                    continue
                u, v = face_uv(cu, fname, p[mk])
                tu = np.clip(np.floor(u).astype(int), 0, tw - 1)
                tv = np.clip(np.floor(v).astype(int), 0, th - 1)
                texel = img[tv, tu]
                opaque = texel[:, 3] >= ALPHA_CUT
                tsel = tt[mk]
                idx = np.argwhere(mk)
                closer = opaque & (tsel < best_t[mk])
                ci = idx[closer]
                best_t[ci[:, 0], ci[:, 1]] = tsel[closer]
                c = texel[closer].copy()
                c[:, :3] *= SHADE[fname]
                c[:, 3] = 1
                col[ci[:, 0], ci[:, 1]] = c
                if cu.solid and which == "in":
                    hsel = ~opaque
                    hi_ = idx[hsel]
                    hole_t[hi_[:, 0], hi_[:, 1]] = np.minimum(hole_t[hi_[:, 0], hi_[:, 1]], tsel[hsel])
    holes_mask = hole_t < best_t
    col[holes_mask] = (1.0, 0.0, 1.0, 1.0)
    return col


def contact_sheet(t: Target, path: Path, out: Path):
    img = load(path)
    cubes = [c for c in t.layer.cuboids if c.part not in OUTER_PLAYER] if t.layer.name == "player_wide" else t.layer.cuboids
    lo, hi = _bounds(cubes)
    span = float(max(hi - lo))
    ppu = max(4.0, min(32.0, 560.0 / max(span, 1e-3)))
    tiles = [(name, render_view(cubes, img, name, ppu, lo, hi)) for name in VIEWS]
    # the texture, scaled, with every face rectangle outlined (top/bottom yellow, sides cyan, front/back green)
    s = max(4, min(12, 768 // max(img.shape[:2])))
    th, tw = img.shape[:2]
    tex = np.zeros((th * s, tw * s, 4))
    checker = ((np.indices((th * s, tw * s)).sum(0) // 4) % 2) * 0.12 + 0.78
    big = np.repeat(np.repeat(img, s, 0), s, 1)
    tex[..., :3] = big[..., :3] * big[..., 3:4] + checker[..., None] * (1 - big[..., 3:4])
    tex[..., 3] = 1
    pim = Image.fromarray((np.clip(tex, 0, 1) * 255).astype(np.uint8), "RGBA")
    from PIL import ImageDraw
    dr = ImageDraw.Draw(pim)
    colours = {"top": (230, 200, 0), "bottom": (230, 120, 0), "west": (0, 200, 230), "east": (0, 120, 230),
               "north": (0, 200, 0), "south": (0, 120, 0)}
    for cu in cubes:
        for f in visible_faces(cu):
            u1, v1, u2, v2 = face_rects(cu)[f]
            dr.rectangle([u1 * s, v1 * s, u2 * s - 1, v2 * s - 1], outline=colours[f])
    texarr = np.asarray(pim, dtype=np.float64) / 255
    # layout: views in a 3x2 grid on top, the texture below
    gap = 6
    cw = max(v.shape[1] for _, v in tiles)
    ch = max(v.shape[0] for _, v in tiles) + 12
    width = max(2 * (cw + gap), texarr.shape[1]) + gap
    height = 3 * (ch + gap) + texarr.shape[0] + 2 * gap
    sheet = np.ones((height, width, 4)) * np.array([0.2, 0.2, 0.22, 1])
    for k, (name, v) in enumerate(tiles):
        r, cidx = divmod(k, 2)
        y0, x0 = gap + r * (ch + gap) + 12, gap + cidx * (cw + gap)
        bg = sheet[y0:y0 + v.shape[0], x0:x0 + v.shape[1]]
        a = v[..., 3:4]
        bg[...] = v * a + bg * (1 - a)
    y0 = 3 * (ch + gap) + gap
    sheet[y0:y0 + texarr.shape[0], gap:gap + texarr.shape[1]] = texarr
    im = Image.fromarray((np.clip(sheet, 0, 1) * 255).astype(np.uint8), "RGBA")
    dr = ImageDraw.Draw(im)
    for k, (name, _) in enumerate(tiles):
        r, cidx = divmod(k, 2)
        dr.text((gap + cidx * (cw + gap), gap + r * (ch + gap)), name + "  (head/-z left, magenta = hole)" if k == 0 else name, fill=(255, 255, 255))
    out.parent.mkdir(parents=True, exist_ok=True)
    im.save(out)
    return out


# ================================================================================================ blocks and items

def block_item_checks() -> list[tuple[str, list[Finding]]]:
    results = []
    tex_root = TEX
    pngs = sorted(list((tex_root / "block").rglob("*.png")) + list((tex_root / "item").rglob("*.png")))
    fs: list[Finding] = []
    empty: list[tuple[str, int]] = []
    for p in pngs:
        rel = p.relative_to(tex_root).as_posix()
        im = Image.open(p)
        w, h = im.size
        arr = np.asarray(im.convert("RGBA"))
        meta = p.with_suffix(".png.mcmeta")
        if arr[..., 3].max() == 0:
            empty.append((rel, len(fs)))
            fs.append(Finding("defect", f"{rel}: fully transparent"))
        if w % 16 or h % 16:
            fs.append(Finding("info" if w <= 16 and h <= 16 else "defect", f"{rel}: size {w}x{h} is not a multiple of 16"))
        if meta.exists():
            try:
                anim = json.loads(meta.read_text(encoding="utf-8")).get("animation")
            except Exception as e:  # noqa: BLE001
                fs.append(Finding("defect", f"{rel}.mcmeta: unreadable ({e})"))
                continue
            if anim is not None:
                fw = anim.get("width", min(w, h))
                fh = anim.get("height", fw if "width" in anim else min(w, h))
                if w % fw or h % fh:
                    fs.append(Finding("defect", f"{rel}: {w}x{h} is not a multiple of the frame {fw}x{fh}"))
                n = (w // fw) * (h // fh)
                for fr in anim.get("frames", []):
                    idx = fr["index"] if isinstance(fr, dict) else fr
                    if idx >= n:
                        fs.append(Finding("defect", f"{rel}.mcmeta: frame index {idx} >= {n} frames"))
                if n <= 1:
                    fs.append(Finding("info", f"{rel}: animation with a single frame"))
        elif w != h and p.parent.name in ("block", "item"):
            fs.append(Finding("defect", f"{rel}: non-square {w}x{h} without an animation .mcmeta"))
    size_fs = fs
    results.append((f"{len(pngs)} block/item PNGs: size, alpha, animation", fs))

    # model references
    model_dirs = [RES / "assets" / "bettacraft" / "models", GENERATED_RES / "assets" / "bettacraft" / "models"]
    existing = {p.relative_to(tex_root).with_suffix("").as_posix() for p in tex_root.rglob("*.png")}
    referenced: set[str] = set()
    layered: set[str] = set()  # textures that are one of several layers of a model
    models_present = set()
    for d in model_dirs:
        if d.exists():
            models_present |= {m.relative_to(d).with_suffix("").as_posix() for m in d.rglob("*.json")}
    fs = []
    nmodels = 0
    for d in model_dirs:
        if not d.exists():
            continue
        for mp in sorted(d.rglob("*.json")):
            nmodels += 1
            try:
                data = json.loads(mp.read_text(encoding="utf-8"))
            except Exception as e:  # noqa: BLE001
                fs.append(Finding("defect", f"{mp.relative_to(ROOT).as_posix()}: unreadable JSON ({e})"))
                continue
            parent = data.get("parent", "")
            if parent.startswith("bettacraft:") and parent.partition(":")[2] not in models_present:
                fs.append(Finding("defect", f"{mp.relative_to(ROOT).as_posix()}: parent {parent} does not exist"))
            own = [v for v in (data.get("textures") or {}).values() if isinstance(v, str) and v.startswith("bettacraft:")]
            for key, val in (data.get("textures") or {}).items():
                if not isinstance(val, str) or val.startswith("#"):
                    continue
                ns, _, path = val.partition(":") if ":" in val else ("minecraft", "", val)
                if ns != "bettacraft":
                    continue
                referenced.add(path)
                if len(set(own)) > 1:
                    layered.add(path)
                if path not in existing:
                    fs.append(Finding("defect", f"{mp.relative_to(ROOT).as_posix()}: texture '{key}' -> {val} does not exist"))
    results.append((f"{nmodels} block/item models: texture and parent references", fs))
    # a fully transparent texture that is one tint layer of several (a plant form without stems, a stem without tips)
    # draws nothing on purpose
    for rel, idx in empty:
        rel = rel.removesuffix(".png")
        if rel in layered:
            size_fs[idx] = Finding("info", f"{rel}: fully transparent, an empty tint layer of a multi-layer model (draws nothing)")
        elif rel not in referenced:
            size_fs[idx] = Finding("defect", f"{rel}: fully transparent and not referenced by a model")

    # unreferenced block/item textures: not in a model, not named in Java or other JSON/resources
    corpus = []
    for base in (ROOT / "common" / "src", ROOT / "neoforge" / "src", ROOT / "fabric" / "src"):
        if base.exists():
            for p in base.rglob("*"):
                if p.suffix in (".java", ".json", ".mcmeta", ".md", ".snbt", ".js") and p.is_file():
                    try:
                        corpus.append(p.read_text(encoding="utf-8", errors="ignore"))
                    except OSError:
                        pass
    blob = "\n".join(corpus)
    fs = []
    for p in pngs:
        rel = p.relative_to(tex_root).with_suffix("").as_posix()
        if rel in referenced:
            continue
        stem = p.stem
        if re.search(r"[\"/:]" + re.escape(stem) + r"[\"'.]", blob) or re.search(re.escape(rel), blob):
            continue
        # names built in Java from a prefix ("plant_leaf_" + shape ...) or with a suffix (name + "_mini")
        parts = stem.split("_")
        if any((p.parent / ("_".join(parts[:k]) + ".png")).exists()
               and re.search(r"[\"/:]" + re.escape("_".join(parts[:k])) + r"[\"'.]", blob)
               and re.search(r"[+?:]\s*\"_" + re.escape("_".join(parts[k:])) + "\"", blob) for k in range(1, len(parts))):
            continue
        built = any(re.search(r"\"([a-z_/]*/)?" + re.escape("_".join(parts[:k]) + "_") + r"\"", blob) for k in range(1, len(parts)))
        fs.append(Finding("info", f"{rel}: not referenced by a model or by name" + (" (maybe built from a name prefix)" if built else "")))
    results.append(("unreferenced block/item textures", fs))
    return results


# ================================================================================================ main

def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--sheets", type=Path, help="write a contact sheet per model and texture into this folder")
    ap.add_argument("--only", help="only models / textures whose name contains this text")
    ap.add_argument("--no-blocks", action="store_true", help="skip the block and item section")
    ap.add_argument("--quiet", action="store_true", help="print only defects")
    a = ap.parse_args(argv)
    defects = 0
    print("== entity models ==")
    for t in targets():
        if a.only and a.only not in t.group and a.only not in t.layer.name.lower() and not any(a.only in p.stem for p, _ in t.textures):
            continue
        res = analyse(t, a.only)
        print(f"\n# {t.group} / {t.layer.name}")
        for head, fs in res:
            d = [f for f in fs if f.severity == "defect"]
            defects += len(d)
            if a.quiet and not d:
                continue
            print(f"  {head}: {'OK' if not d else str(len(d)) + ' defect(s)'}")
            for f in fs:
                if a.quiet and f.severity != "defect":
                    continue
                print(f"    [{f.severity}] {f.text}")
        if a.sheets:
            for p, kind in t.textures:
                if a.only and a.only not in p.stem and a.only not in t.group:
                    continue
                if p.exists() and (kind == "solid" or t.group == "betta" and p.stem == "region_body"):
                    contact_sheet(t, p, a.sheets / f"{t.layer.name}__{p.stem}.png")
            for p in t.union[:1]:
                if p.exists() and (not a.only or a.only in t.group):
                    contact_sheet(t, p, a.sheets / f"{t.layer.name}__{p.stem}.png")
    if not a.only:
        print("\n== other entity textures (custom renderers, no box-UV model) ==")
        covered = {p.resolve() for t in targets() for p, _ in t.textures} | {p.resolve() for t in targets() for p in t.union}
        for p in sorted((TEX / "entity").rglob("*.png")):
            if p.resolve() in covered:
                continue
            arr = np.asarray(Image.open(p).convert("RGBA"))
            note = "fully transparent" if arr[..., 3].max() == 0 else f"{arr.shape[1]}x{arr.shape[0]}"
            sev = "defect" if arr[..., 3].max() == 0 else "info"
            defects += sev == "defect"
            if not a.quiet or sev == "defect":
                print(f"    [{sev}] {p.relative_to(TEX).as_posix()}: {note}, not checked against a model")
    if not a.no_blocks and not a.only:
        print("\n== block and item textures ==")
        for head, fs in block_item_checks():
            d = [f for f in fs if f.severity == "defect"]
            defects += len(d)
            print(f"  {head}: {'OK' if not d else str(len(d)) + ' defect(s)'}" + (f", {len(fs) - len(d)} info" if len(fs) > len(d) else ""))
            for f in fs:
                if a.quiet and f.severity != "defect":
                    continue
                print(f"    [{f.severity}] {f.text}")
    print(f"\n{defects} defect(s)")
    return 1 if defects else 0


if __name__ == "__main__":
    sys.exit(main())
