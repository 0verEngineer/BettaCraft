"""Single source of truth for the betta entity model.

Everything here uses vanilla Minecraft entity-model conventions (Mojang mappings):

* units are model pixels (1/16 block), +Y points DOWN, the head points to -Z,
* every part is ``texOffs(u, v).addBox(x, y, z, w, h, d)`` with a ``PartPose`` (offset relative
  to the parent pivot, rotation in radians applied Z*Y*X like ``ModelPart.translateAndRotate``),
* the texture is one shared 64x64 box-UV layout. Shared parts (head, body, small fins) have one
  UV region used by every shape; parts whose *size* differs between shapes (caudal, dorsal,
  anal) get one UV region per shape, so a single texture works on every body shape.
* fins are thin cuboids with width 0 (a YZ plane); only their two large faces (west/east) carry
  texture. Both faces are coplanar, so the east face texture is always the mirror of the west
  face texture (then both render the same pixels in world space - no visible z-fight).

Run directly to (re)write ``build/generated/BettaModelDefinitions.java`` and the ``.bbmodel`` files.
"""
from __future__ import annotations

import base64
import json
import math
import uuid
from dataclasses import dataclass, replace
from pathlib import Path

import numpy as np

TEX_W = TEX_H = 64

SHAPES = ["plakat", "veil", "halfmoon", "crowntail", "doubletail"]
SHAPE_NAMES = {
    "plakat": ("Plakat", "Plakat"),
    "veil": ("Veiltail", "Schleierschwanz"),
    "halfmoon": ("Halfmoon", "Halbmond"),
    "crowntail": ("Crowntail", "Kronenschwanz"),
    "doubletail": ("Doubletail", "Doppelschwanz"),
}

# Pivot of the top-level "body" part (model space, y=24 is the bottom of the entity box).
BODY_OFFSET = (0.0, 20.0, 0.0)
# Suggested render scale for an adult (the model is ~21 px long at scale 1).
ADULT_RENDER_SCALE = 0.5

# ---------------------------------------------------------------- life stages / sex
# scale  = uniform model scale relative to adult
# alpha  = suggested vertex alpha (fry are see-through)
# saturation = multiply the tint colours' saturation (juveniles are pale)
# model  = "betta" (render the entity model) or "placeholder" (egg/larva are nest particles)
LIFE_STAGES = [
    {"id": "egg", "scale": 0.06, "alpha": 1.0, "saturation": 0.0, "model": "placeholder"},
    {"id": "larva", "scale": 0.12, "alpha": 0.8, "saturation": 0.1, "model": "placeholder"},
    {"id": "fry", "scale": 0.25, "alpha": 0.6, "saturation": 0.25, "model": "betta"},
    {"id": "juvenile", "scale": 0.5, "alpha": 1.0, "saturation": 0.55, "model": "betta"},
    {"id": "subadult", "scale": 0.75, "alpha": 1.0, "saturation": 0.85, "model": "betta"},
    {"id": "adult", "scale": 1.0, "alpha": 1.0, "saturation": 1.0, "model": "betta"},
]
# Females: same mesh, ModelPart.xScale/yScale/zScale of these parts set at render time.
FEMALE_FIN_SCALE = 0.8
FEMALE_TAIL_SCALE = 0.7
FEMALE_SATURATION = 0.85


@dataclass(frozen=True)
class Part:
    name: str
    parent: str | None
    offset: tuple  # PartPose offset relative to the parent pivot
    rot: tuple  # (xRot, yRot, zRot) radians
    box: tuple | None  # (x, y, z, w, h, d) relative to own pivot; None = empty bone
    uv: str | None  # key of the UV region
    region: str  # "body" | "fin" | "none"
    fin_kind: str | None = None  # caudal | dorsal | anal | pectoral | ventral
    fin_style: str | None = None  # silhouette style (shape name, "normal", "dumbo")
    female_scale: float = 1.0
    variant: str | None = None  # "normal"/"dumbo" for the two pectoral sets


# Per-shape fin sizes. caudal: (d, h, y0)  dorsal/anal: (z_pivot, d, h)
SHAPE_FINS = {
    "plakat": {"caudal": (5, 7, -3.5), "dorsal": (1.0, 5, 3), "anal": (-1.0, 7, 3)},
    "veil": {"caudal": (10, 9, -3.0), "dorsal": (-1.0, 7, 5), "anal": (-2.0, 8, 5)},
    "halfmoon": {"caudal": (8, 14, -7.0), "dorsal": (-1.0, 7, 6), "anal": (-2.0, 8, 5)},
    "crowntail": {"caudal": (9, 13, -6.5), "dorsal": (-1.0, 7, 6), "anal": (-2.0, 8, 6)},
    "doubletail": {"caudal": (8, 13, -6.5), "dorsal": (-2.0, 8, 6), "anal": (-2.0, 8, 5)},
}


def parts(shape: str) -> list[Part]:
    """All parts of one body shape, parents before children."""
    f = SHAPE_FINS[shape]
    cd, ch, cy0 = f["caudal"]
    dz, dd, dh = f["dorsal"]
    az, ad, ah = f["anal"]
    fs, ts = FEMALE_FIN_SCALE, FEMALE_TAIL_SCALE
    pec_rot = 0.55
    return [
        Part("body", None, BODY_OFFSET, (0, 0, 0), (-1.5, -2.0, -4.0, 3, 4, 8), "body", "body"),
        Part("head", "body", (0, 0, -4), (0, 0, 0), (-1.5, -2.0, -3.0, 3, 4, 3), "head", "body"),
        Part("snout", "head", (0, 0, -3), (0, 0, 0), (-1.0, -1.5, -1.0, 2, 2, 1), "snout", "body"),
        Part("peduncle", "body", (0, 0, 4), (0, 0, 0), (-1.0, -1.5, 0.0, 2, 3, 2), "peduncle", "body"),
        Part("caudal", "peduncle", (0, 0, 2), (0, 0, 0), (0.0, cy0, 0.0, 0, ch, cd),
             f"caudal_{shape}", "fin", "caudal", shape, ts),
        Part("dorsal", "body", (0, -2.0, dz), (0, 0, 0), (0.0, -dh + 1.0, 0.0, 0, dh, dd),
             f"dorsal_{shape}", "fin", "dorsal", shape, fs),
        Part("anal", "body", (0, 2.0, az), (0, 0, 0), (0.0, -1.0, 0.0, 0, ah, ad),
             f"anal_{shape}", "fin", "anal", shape, fs),
        Part("pectoral_left", "body", (1.5, 0.5, -3.5), (0.15, pec_rot, 0), (0.0, -1.0, 0.0, 0, 2, 3),
             "pectoral", "fin", "pectoral", "normal", fs, "normal"),
        Part("pectoral_right", "body", (-1.5, 0.5, -3.5), (0.15, -pec_rot, 0), (0.0, -1.0, 0.0, 0, 2, 3),
             "pectoral", "fin", "pectoral", "normal", fs, "normal"),
        Part("pectoral_dumbo_left", "body", (1.5, 0.5, -3.5), (0.25, pec_rot, 0), (0.0, -1.5, 0.0, 0, 4, 5),
             "pectoral_dumbo", "fin", "pectoral", "dumbo", fs, "dumbo"),
        Part("pectoral_dumbo_right", "body", (-1.5, 0.5, -3.5), (0.25, -pec_rot, 0), (0.0, -1.5, 0.0, 0, 4, 5),
             "pectoral_dumbo", "fin", "pectoral", "dumbo", fs, "dumbo"),
        Part("ventral_left", "body", (0.75, 2.0, -3.5), (0.55, 0.2, 0), (0.0, -0.5, 0.0, 0, 4, 2),
             "ventral", "fin", "ventral", "normal", fs),
        Part("ventral_right", "body", (-0.75, 2.0, -3.5), (0.55, -0.2, 0), (0.0, -0.5, 0.0, 0, 4, 2),
             "ventral", "fin", "ventral", "normal", fs),
    ]


# ---------------------------------------------------------------- UV layout

def box_dims(box) -> tuple[int, int, int]:
    w, h, d = box[3], box[4], box[5]
    assert float(w).is_integer() and float(h).is_integer() and float(d).is_integer()
    return int(w), int(h), int(d)


def _regions() -> dict[str, tuple[int, int, int]]:
    """uv key -> (w, h, d); asserts every user of a key has identical box dims."""
    regs: dict[str, tuple[int, int, int]] = {}
    for s in SHAPES:
        for p in parts(s):
            if p.uv is None:
                continue
            dims = box_dims(p.box)
            if p.uv in regs:
                assert regs[p.uv] == dims, f"uv region {p.uv} used with different sizes"
            regs[p.uv] = dims
    return regs


def _reserved_rect(w, h, d):
    """(dx, dy, W, H, min_y): area of the texture a box actually samples, relative to texOffs."""
    if w == 0:  # fin: only west+east faces exist (2d x h, below the empty top row)
        return 0, d, 2 * d, h
    return 0, 0, 2 * (w + d), d + h


# Fixed packing order keeps the layout stable and readable (shared parts first).
_PACK_ORDER = ["body", "head", "peduncle", "snout", "pectoral_dumbo", "pectoral", "ventral"]


def _pack() -> dict[str, tuple[int, int]]:
    regs = _regions()
    occ = np.zeros((TEX_H, TEX_W), dtype=bool)
    keys = [k for k in _PACK_ORDER if k in regs]
    fins = sorted((k for k in regs if k not in keys),
                  key=lambda k: (-regs[k][1], -regs[k][2], k))  # tallest fins first
    out = {}
    for k in keys + fins:
        w, h, d = regs[k]
        dx, dy, W, H = _reserved_rect(w, h, d)
        placed = False
        for y in range(dy, TEX_H - H + 1):
            for x in range(0, TEX_W - W + 1):
                if not occ[y:y + H, x:x + W].any():
                    occ[y:y + H, x:x + W] = True
                    out[k] = (x - dx, y - dy)
                    placed = True
                    break
            if placed:
                break
        if not placed:
            raise RuntimeError(f"UV region {k} does not fit into {TEX_W}x{TEX_H}")
    return out


UV = _pack()
REGIONS = _regions()


def tex_offs(part: Part) -> tuple[int, int]:
    return UV[part.uv]


# ---------------------------------------------------------------- box faces (vanilla ModelPart.Cube)
# Face names: top = model minY (vanilla Direction.DOWN), bottom = maxY (Direction.UP),
# west = minX, east = maxX, north = minZ (front, head side), south = maxZ (back).

FACE_ORDER = ["top", "bottom", "west", "north", "east", "south"]


def box_faces(u, v, box):
    """Return {face: (rect, origin, ds, dt)} where rect = (fu, fv, fw, fh) in texels and the
    3D point of texture coordinate (fu+s, fv+t) is origin + s*ds + t*dt (box-local space)."""
    x0, y0, z0, w, h, d = box
    x1, y1, z1 = x0 + w, y0 + h, z0 + d
    w, h, d = box_dims(box)
    X, Y, Z = np.eye(3)
    P = lambda x, y, z: np.array([x, y, z], dtype=np.float64)
    return {
        "top": ((u + d, v, w, d), P(x0, y0, z1), X, -Z),
        "bottom": ((u + d + w, v, w, d), P(x0, y1, z1), X, -Z),
        "west": ((u, v + d, d, h), P(x0, y0, z1), -Z, Y),
        "north": ((u + d, v + d, w, h), P(x0, y0, z0), X, Y),
        "east": ((u + d + w, v + d, d, h), P(x1, y0, z0), Z, Y),
        "south": ((u + 2 * d + w, v + d, w, h), P(x1, y0, z1), -X, Y),
    }


# ---------------------------------------------------------------- transforms

def _rot(rx, ry, rz) -> np.ndarray:
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


def world_matrices(plist: list[Part], scales: dict[str, float] | None = None,
                   extra_rot: dict[str, tuple] | None = None) -> dict[str, np.ndarray]:
    """4x4 part->model matrices (rest pose), like ModelPart.translateAndRotate + scale."""
    scales = scales or {}
    extra_rot = extra_rot or {}
    mats: dict[str, np.ndarray] = {}
    for p in plist:
        M = np.eye(4)
        M[:3, 3] = p.offset
        r = np.add(p.rot, extra_rot.get(p.name, (0, 0, 0)))
        R = np.eye(4)
        R[:3, :3] = _rot(*r)
        S = np.diag([scales.get(p.name, 1.0)] * 3 + [1.0])
        local = M @ R @ S
        mats[p.name] = (mats[p.parent] @ local) if p.parent else local
    return mats


def female_scales(plist: list[Part]) -> dict[str, float]:
    return {p.name: p.female_scale for p in plist if p.female_scale != 1.0}


# ---------------------------------------------------------------- Java export

def _f(x: float) -> str:
    s = f"{float(x):.4f}".rstrip("0")
    if s.endswith("."):
        s += "0"
    return s + "F"


def java_source(package: str = "org.overengineer.bettacraft.client.model") -> str:
    lines = [
        "// GENERATED by tools/assetgen/model_defs.py - do not edit by hand, edit model_defs.py and rerun build.py",
        f"package {package};",
        "",
        "import net.minecraft.client.model.geom.PartPose;",
        "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
        "import net.minecraft.client.model.geom.builders.LayerDefinition;",
        "import net.minecraft.client.model.geom.builders.MeshDefinition;",
        "import net.minecraft.client.model.geom.builders.PartDefinition;",
        "",
        "/**",
        " * Betta entity meshes, one LayerDefinition per body shape. All shapes share one 64x64 box-UV",
        " * texture layout (shared parts use the same texOffs; caudal/dorsal/anal have a region per shape).",
        " * Every mesh contains both pectoral sets: hide either 'pectoral_*' or 'pectoral_dumbo_*'.",
        " * Females: set xScale/yScale/zScale of fin parts to FEMALE_FIN_SCALE, caudal to FEMALE_TAIL_SCALE.",
        " */",
        "public final class BettaModelDefinitions {",
        f"    public static final int TEXTURE_WIDTH = {TEX_W};",
        f"    public static final int TEXTURE_HEIGHT = {TEX_H};",
        f"    public static final float ADULT_RENDER_SCALE = {_f(ADULT_RENDER_SCALE)};",
        f"    public static final float FEMALE_FIN_SCALE = {_f(FEMALE_FIN_SCALE)};",
        f"    public static final float FEMALE_TAIL_SCALE = {_f(FEMALE_TAIL_SCALE)};",
        "    /** Parts scaled by FEMALE_FIN_SCALE (caudal uses FEMALE_TAIL_SCALE). */",
        "    public static final String[] FIN_PARTS = {" + ", ".join(
            f'"{p.name}"' for p in parts(SHAPES[0]) if p.region == "fin") + "};",
        "    /** Life stages in order: id, model scale, vertex alpha, colour saturation. */",
        "    public static final String[] STAGE_IDS = {" + ", ".join(f'"{s["id"]}"' for s in LIFE_STAGES) + "};",
        "    public static final float[] STAGE_SCALES = {" + ", ".join(_f(s["scale"]) for s in LIFE_STAGES) + "};",
        "    public static final float[] STAGE_ALPHAS = {" + ", ".join(_f(s["alpha"]) for s in LIFE_STAGES) + "};",
        "    public static final float[] STAGE_SATURATION = {" + ", ".join(_f(s["saturation"]) for s in LIFE_STAGES) + "};",
        "",
        "    public enum Shape { " + ", ".join(s.upper() for s in SHAPES) + " }",
        "",
        "    private BettaModelDefinitions() {}",
        "",
        "    public static LayerDefinition createBodyLayer(Shape shape) {",
        "        return switch (shape) {",
    ]
    for s in SHAPES:
        lines.append(f"            case {s.upper()} -> create{s.capitalize()}Layer();")
    lines += ["        };", "    }", ""]
    for s in SHAPES:
        lines.append(f"    public static LayerDefinition create{s.capitalize()}Layer() {{")
        lines.append("        MeshDefinition mesh = new MeshDefinition();")
        lines.append("        PartDefinition root = mesh.getRoot();")
        for p in parts(s):
            parent = "root" if p.parent is None else p.parent
            u, v = tex_offs(p)
            x, y, z, w, h, d = p.box
            cubes = f"CubeListBuilder.create().texOffs({u}, {v}).addBox({_f(x)}, {_f(y)}, {_f(z)}, {_f(w)}, {_f(h)}, {_f(d)})"
            ox, oy, oz = p.offset
            if any(p.rot):
                pose = f"PartPose.offsetAndRotation({_f(ox)}, {_f(oy)}, {_f(oz)}, {_f(p.rot[0])}, {_f(p.rot[1])}, {_f(p.rot[2])})"
            else:
                pose = f"PartPose.offset({_f(ox)}, {_f(oy)}, {_f(oz)})"
            lines.append(f'        PartDefinition {p.name} = {parent}.addOrReplaceChild("{p.name}",')
            lines.append(f"                {cubes},")
            lines.append(f"                {pose});")
        lines.append(f"        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);")
        lines.append("    }")
        lines.append("")
    lines.append("}")
    return "\n".join(lines) + "\n"


# ---------------------------------------------------------------- Blockbench export

def _uuid(*names) -> str:
    return str(uuid.uuid5(uuid.NAMESPACE_URL, "bettacraft:" + "/".join(names)))


def bbmodel(shape: str, texture_png: bytes | None = None) -> dict:
    """Blockbench 'modded_entity' (Mojang mappings) project for one shape."""
    plist = parts(shape)
    # absolute pivot in java model space (Blockbench origins ignore parent rotation)
    absolute: dict[str, np.ndarray] = {}
    for p in plist:
        base = absolute[p.parent] if p.parent else np.zeros(3)
        absolute[p.name] = base + np.array(p.offset, dtype=float)

    def bb_origin(jp):
        return [-jp[0], 24.0 - jp[1], jp[2]]

    elements, groups = [], {}
    for p in plist:
        jp = absolute[p.name]
        o = bb_origin(jp)
        g = {
            "name": p.name, "origin": [round(c, 4) for c in o],
            "rotation": [round(-math.degrees(p.rot[0]), 4), round(-math.degrees(p.rot[1]), 4),
                         round(math.degrees(p.rot[2]), 4)],
            "color": 0, "uuid": _uuid(shape, "group", p.name), "export": True, "mirror_uv": False,
            "isOpen": True, "locked": False, "visibility": True, "autouv": 0, "children": [],
        }
        groups[p.name] = g
        if p.box is not None:
            x, y, z, w, h, d = p.box
            to = [o[0] - x, o[1] - y, o[2] + z + d]
            frm = [to[0] - w, to[1] - h, o[2] + z]
            to[2] = frm[2] + d
            u, v = tex_offs(p)
            wi, hi, di = box_dims(p.box)
            faces = {
                "north": {"uv": [u + di, v + di, u + di + wi, v + di + hi], "texture": 0},
                "east": {"uv": [u, v + di, u + di, v + di + hi], "texture": 0},
                "south": {"uv": [u + 2 * di + wi, v + di, u + 2 * di + 2 * wi, v + di + hi], "texture": 0},
                "west": {"uv": [u + di + wi, v + di, u + 2 * di + wi, v + di + hi], "texture": 0},
                "up": {"uv": [u + di + wi, v + di, u + di, v], "texture": 0},
                "down": {"uv": [u + di + 2 * wi, v, u + di + wi, v + di], "texture": 0},
            }
            cid = _uuid(shape, "cube", p.name)
            elements.append({
                "name": p.name, "box_uv": True, "rescale": False, "locked": False, "render_order": "default",
                "allow_mirror_modeling": True, "from": [round(c, 4) for c in frm], "to": [round(c, 4) for c in to],
                "autouv": 0, "color": 0, "origin": [round(c, 4) for c in o], "uv_offset": [u, v],
                "faces": faces, "type": "cube", "uuid": cid,
            })
            g["children"].append(cid)
    outliner = []
    for p in plist:
        if p.parent:
            groups[p.parent]["children"].append(groups[p.name])
        else:
            outliner.append(groups[p.name])
    textures = []
    if texture_png is not None:
        textures.append({
            "path": "", "name": f"betta_{shape}_sample.png", "folder": "", "namespace": "", "id": "0",
            "width": TEX_W, "height": TEX_H, "uv_width": TEX_W, "uv_height": TEX_H, "particle": False,
            "use_as_default": False, "layers_enabled": False, "render_mode": "default", "render_sides": "auto",
            "frame_time": 1, "frame_order_type": "loop", "frame_order": "", "frame_interpolate": False,
            "visible": True, "internal": True, "saved": True, "uuid": _uuid(shape, "texture"),
            "relative_path": "", "source": "data:image/png;base64," + base64.b64encode(texture_png).decode(),
        })
    return {
        "meta": {"format_version": "4.10", "model_format": "modded_entity", "box_uv": True},
        "name": f"betta_{shape}", "model_identifier": f"betta_{shape}", "modded_entity_version": "1.17_mojmaps",
        "modded_entity_flip_y": True, "visible_box": [1, 1, 0], "variable_placeholders": "",
        "variable_placeholder_buttons": [], "timeline_setups": [], "unhandled_root_fields": {},
        "resolution": {"width": TEX_W, "height": TEX_H},
        "elements": elements, "outliner": outliner, "textures": textures,
    }


def write_generated(out_dir: Path, sample_textures: dict[str, bytes] | None = None) -> list[Path]:
    out_dir.mkdir(parents=True, exist_ok=True)
    written = []
    jp = out_dir / "BettaModelDefinitions.java"
    jp.write_text(java_source(), encoding="utf-8", newline="\n")
    written.append(jp)
    for s in SHAPES:
        png = (sample_textures or {}).get(s)
        bp = out_dir / f"betta_{s}.bbmodel"
        bp.write_text(json.dumps(bbmodel(s, png), indent=1), encoding="utf-8", newline="\n")
        written.append(bp)
    return written


def layout_summary() -> str:
    rows = []
    for s in SHAPES:
        rows.append(f"[{s}]")
        for p in parts(s):
            u, v = tex_offs(p)
            rows.append(f"  {p.name:22s} uv={p.uv:18s} texOffs({u:2d},{v:2d}) addBox{tuple(p.box)}")
    return "\n".join(rows)


if __name__ == "__main__":
    from common import GEN
    for path in write_generated(GEN):
        print("wrote", path)
    print(layout_summary())
