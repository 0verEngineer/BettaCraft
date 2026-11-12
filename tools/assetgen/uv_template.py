"""Per-texel metadata of the shared 64x64 betta UV layout, the debug UV template and the region masks.

``meta()`` answers, for every texel of the texture: which part/face it belongs to, where it sits on
the model (rest pose, model space), and for fin texels the fin coordinates (t along the ray,
ray flag, distance to the outer edge). Patterns and shading are computed from this, so they always
match the model definition.
"""
from __future__ import annotations

from functools import lru_cache

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import model_defs as md
from common import BUILD, ENTITY_TEX, PREVIEW, save_png, to_image
from fins import fin_fields

N = md.TEX_W


class Meta:
    def __init__(self):
        self.cover = np.zeros((N, N), bool)
        self.key = np.full((N, N), "", dtype=object)
        self.region = np.full((N, N), "", dtype=object)  # body | fin
        self.part = np.full((N, N), "", dtype=object)  # representative part name
        self.face = np.full((N, N), "", dtype=object)
        self.pos = np.zeros((N, N, 3))  # model space (rest pose)
        self.fs = np.zeros((N, N))  # 0..1 across the face (u direction)
        self.ft = np.zeros((N, N))  # 0..1 down the face (v direction)
        self.fin_kind = np.full((N, N), "", dtype=object)
        self.fin_t = np.zeros((N, N))
        self.fin_ray = np.zeros((N, N), bool)
        self.fin_edge = np.zeros((N, N), int)
        self.fin_row = np.zeros((N, N))  # 0..1 top->bottom inside the fin grid
        self.fin_col = np.zeros((N, N))  # 0..1 front->back inside the fin grid
        self.rects: list[tuple[str, str, tuple]] = []  # (key, face, rect)

    @property
    def body(self):
        return self.cover & (self.region == "body")

    @property
    def fin(self):
        return self.cover & (self.region == "fin")


def representative_parts():
    """uv key -> (shape, Part) for one user of every UV region."""
    out = {}
    for s in md.SHAPES:
        for p in md.parts(s):
            if p.uv and p.uv not in out:
                out[p.uv] = (s, p)
    return out


@lru_cache(maxsize=1)
def meta() -> Meta:
    m = Meta()
    mats_cache = {}
    for key, (shape, p) in representative_parts().items():
        if shape not in mats_cache:
            mats_cache[shape] = md.world_matrices(md.parts(shape))
        M = mats_cache[shape][p.name]
        u, v = md.tex_offs(p)
        w, h, d = md.box_dims(p.box)
        if p.region == "fin":
            center = -p.box[1] if p.fin_kind == "caudal" else None
            mask, t, ray, edge = fin_fields(p.fin_kind, p.fin_style, d, h, center)
        for face, (rect, origin, ds, dt) in md.box_faces(u, v, p.box).items():
            fu, fv, fw, fh = rect
            if fw == 0 or fh == 0:
                continue
            m.rects.append((key, face, rect))
            for j in range(fh):
                for i in range(fw):
                    U, V = fu + i, fv + j
                    local = origin + (i + .5) * ds + (j + .5) * dt
                    if p.region == "fin":
                        col = i if face == "east" else d - 1 - i
                        if not mask[j, col]:
                            continue
                        m.fin_kind[V, U] = p.fin_kind
                        m.fin_t[V, U] = t[j, col]
                        m.fin_ray[V, U] = ray[j, col]
                        m.fin_edge[V, U] = edge[j, col]
                        m.fin_row[V, U] = (j + .5) / h
                        m.fin_col[V, U] = (col + .5) / d
                    assert not m.cover[V, U], f"UV overlap at {U},{V} ({key})"
                    m.cover[V, U] = True
                    m.key[V, U] = key
                    m.region[V, U] = p.region
                    m.part[V, U] = p.name
                    m.face[V, U] = face
                    m.pos[V, U] = (M @ np.append(local, 1.0))[:3]
                    m.fs[V, U] = (i + .5) / fw
                    m.ft[V, U] = (j + .5) / fh
    return m


# ---------------------------------------------------------------- outputs

FACE_TINT = {"top": 1.15, "bottom": .65, "west": .85, "east": 1.0, "north": .75, "south": .55}


def _key_colour(key: str) -> np.ndarray:
    base = {
        "body": "#d9a441", "head": "#e06c4c", "snout": "#c0392b", "peduncle": "#b9770e",
        "pectoral": "#58d68d", "pectoral_dumbo": "#1e8449", "ventral": "#76d7c4",
    }
    if key in base:
        h = base[key]
    elif key.startswith("caudal"):
        h = "#5dade2"
    elif key.startswith("dorsal"):
        h = "#af7ac5"
    else:
        h = "#ec7063"
    return np.array([int(h[i:i + 2], 16) for i in (1, 3, 5)]) / 255.0


def uv_template_image(scale: int = 12) -> Image.Image:
    m = meta()
    img = np.zeros((N, N, 4))
    for key, face, (fu, fv, fw, fh) in m.rects:
        c = np.clip(_key_colour(key) * FACE_TINT[face], 0, 1)
        img[fv:fv + fh, fu:fu + fw, :3] = c * 0.35  # face bounds (dim)
        img[fv:fv + fh, fu:fu + fw, 3] = 1
    for V in range(N):
        for U in range(N):
            if m.cover[V, U]:
                c = np.clip(_key_colour(m.key[V, U]) * FACE_TINT[m.face[V, U]], 0, 1)
                if m.fin_ray[V, U]:
                    c = c * .8
                img[V, U, :3] = c
    big = to_image(img).resize((N * scale, N * scale), Image.NEAREST)
    bg = Image.new("RGBA", big.size, (24, 24, 28, 255))
    bg.alpha_composite(big)
    dr = ImageDraw.Draw(bg)
    for k in range(0, N + 1):
        col = (60, 60, 70, 255) if k % 8 else (110, 110, 125, 255)
        dr.line([(k * scale, 0), (k * scale, N * scale)], fill=col)
        dr.line([(0, k * scale), (N * scale, k * scale)], fill=col)
    font = _font(11)
    for key, face, (fu, fv, fw, fh) in m.rects:
        dr.rectangle([fu * scale, fv * scale, (fu + fw) * scale, (fv + fh) * scale], outline=(255, 255, 255, 200))
        if face in ("west", "north") and fw * scale >= 24:
            label = key.replace("caudal_", "C.").replace("dorsal_", "D.").replace("anal_", "A.")
            label = label.replace("pectoral_dumbo", "pec.dumbo").replace("pectoral", "pec")
            if face == "west" or key in ("body", "head", "snout", "peduncle"):
                dr.text((fu * scale + 2, fv * scale + 1), label if face == "west" else "N", fill=(255, 255, 255, 255),
                        font=font, stroke_width=2, stroke_fill=(0, 0, 0, 255))
    return bg


def _font(size: int):
    try:
        return ImageFont.load_default(size)
    except TypeError:  # very old Pillow
        return ImageFont.load_default()


def write_region_masks() -> list:
    from patterns import shading
    m = meta()
    shade = shading()
    out = []
    for name, sel in (("region_body", m.body), ("region_fins", m.fin)):
        arr = np.zeros((N, N, 4))
        arr[..., 0] = arr[..., 1] = arr[..., 2] = shade
        arr[..., 3] = sel
        arr[~sel, :3] = 0
        out.append(save_png(arr, ENTITY_TEX / f"{name}.png"))
    return out


def write_template() -> list:
    img = uv_template_image()
    return [save_png(img, BUILD / "uv_template.png"), save_png(img, PREVIEW / "uv_template.png")]


if __name__ == "__main__":
    for p in write_template() + write_region_masks():
        print("wrote", p)
