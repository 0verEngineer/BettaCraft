"""Texture compositing (same layer order as the runtime renderer) and an orthographic side-view
software rasterizer for the box-UV model (nearest-neighbour sampling, z-buffer)."""
from __future__ import annotations

import colorsys
from dataclasses import dataclass, field
from functools import lru_cache

import numpy as np

import model_defs as md
import patterns as pt
from common import hex_f, over
from uv_template import meta


# ---------------------------------------------------------------- genes -> texture

@dataclass
class Fish:
    shape: str = "halfmoon"
    body: str = "#c0392b"
    fins: str = "#c0392b"
    pattern: str | None = None
    pattern_colour: str = "#f4f1ea"
    iridescence: float = 0.0  # 0 none, 0.5 low, 1 high
    irid_colour: str = "#4fc3d9"
    dumbo: bool = False
    female: bool = False
    stage: str = "adult"
    extra_rot: dict = field(default_factory=dict)


def _sat(rgb: np.ndarray, factor: float) -> np.ndarray:
    h, l, s = colorsys.rgb_to_hls(*rgb)
    return np.array(colorsys.hls_to_rgb(h, l, max(0.0, min(1.0, s * factor))))


@lru_cache(maxsize=None)
def _layers():
    from uv_template import N
    m = meta()
    sh = pt.shading()
    body = np.where(m.body, 1.0, 0.0)
    fins = np.where(m.fin, 1.0, 0.0)
    pats = {k: pt.PATTERNS[k]() for k in pt.PATTERNS}
    return sh, body, fins, pats, pt.iridescence_rgba(), pt.eye_rgba(), N


def composite_texture(f: Fish) -> np.ndarray:
    """RGBA float texture exactly as the layered runtime render would colour it:
    region_body x body colour, region_fins x fin colour, pattern x pattern colour, iridescence, eye."""
    sh, body, fins, pats, irid, eye, N = _layers()
    stage = next(s for s in md.LIFE_STAGES if s["id"] == f.stage)
    sat = stage["saturation"] * (md.FEMALE_SATURATION if f.female else 1.0)
    cb, cf, cp = (_sat(hex_f(c), sat) for c in (f.body, f.fins, f.pattern_colour))
    tex = np.zeros((N, N, 4))
    g = sh[..., None]
    over(tex, g * cb, body)
    over(tex, g * cf, fins)
    if f.pattern:
        over(tex, g * cp, pats[f.pattern])
    if f.iridescence > 0:
        ci = _sat(hex_f(f.irid_colour), sat)
        over(tex, irid[..., :3] * ci, irid[..., 3] * f.iridescence)
    over(tex, eye[..., :3], eye[..., 3])
    return tex


# ---------------------------------------------------------------- rasterizer

def visible_parts(f: Fish):
    plist = md.parts(f.shape)
    hide = "normal" if f.dumbo else "dumbo"
    return [p for p in plist if p.variant != hide]


def render_side(f: Fish, tex: np.ndarray | None = None, ppu: int = 4, scale: float = 1.0,
                bounds=(-9.0, 17.0, 11.0, 29.0)) -> np.ndarray:
    """Orthographic view from +X (head to the left). Returns float RGBA image.
    bounds = (zmin, zmax, ymin, ymax) in model units at scale 1, scaled around the body pivot."""
    if tex is None:
        tex = composite_texture(f)
    plist = md.parts(f.shape)
    scales = md.female_scales(plist) if f.female else {}
    mats = md.world_matrices(plist, scales, f.extra_rot)
    zmin, zmax, ymin, ymax = bounds
    W, H = int(round((zmax - zmin) * ppu)), int(round((ymax - ymin) * ppu))
    img = np.zeros((H, W, 4))
    depth = np.full((H, W), -1e9)
    piv = np.array(md.BODY_OFFSET)
    TW, TH = md.TEX_W, md.TEX_H
    for p in visible_parts(f):
        M = mats[p.name]
        u, v = md.tex_offs(p)
        for face, (rect, origin, ds, dt) in md.box_faces(u, v, p.box).items():
            fu, fv, fw, fh = rect
            if fw == 0 or fh == 0:
                continue
            corners = [origin, origin + fw * ds, origin + fw * ds + fh * dt]
            Q = [((M @ np.append(c, 1))[:3] - piv) * scale + piv for c in corners]
            P0, P1, P2 = (np.array([(q[2] - zmin) * ppu, (q[1] - ymin) * ppu, q[0]]) for q in Q)
            # affine map: screen = P0 + s*(P1-P0)/fw + t*(P2-P1)/fh
            A = np.array([[P1[0] - P0[0], P2[0] - P1[0]], [P1[1] - P0[1], P2[1] - P1[1]]])
            det = np.linalg.det(A)
            if abs(det) < 1e-6:
                continue  # face seen edge-on
            Ainv = np.linalg.inv(A)
            xs = [P0[0], P1[0], P2[0], P0[0] + P2[0] - P1[0]]
            ys = [P0[1], P1[1], P2[1], P0[1] + P2[1] - P1[1]]
            x0, x1 = max(int(np.floor(min(xs))), 0), min(int(np.ceil(max(xs))), W)
            y0, y1 = max(int(np.floor(min(ys))), 0), min(int(np.ceil(max(ys))), H)
            if x0 >= x1 or y0 >= y1:
                continue
            gy, gx = np.mgrid[y0:y1, x0:x1] + 0.5
            rx, ry = gx - P0[0], gy - P0[1]
            a = Ainv[0, 0] * rx + Ainv[0, 1] * ry
            b = Ainv[1, 0] * rx + Ainv[1, 1] * ry
            inside = (a >= 0) & (a < 1) & (b >= 0) & (b < 1)
            if not inside.any():
                continue
            ti = np.clip(np.floor(a * fw).astype(int), 0, fw - 1) + fu
            tj = np.clip(np.floor(b * fh).astype(int), 0, fh - 1) + fv
            ti, tj = np.clip(ti, 0, TW - 1), np.clip(tj, 0, TH - 1)
            col = tex[tj, ti]
            dz = P0[2] + a * (P1[2] - P0[2]) + b * (P2[2] - P1[2])
            # entity lighting approximation: faces turned away from the viewer are darker
            n = np.cross(Q[1] - Q[0], Q[2] - Q[1])
            nn = n / (np.linalg.norm(n) + 1e-9)
            light = 0.62 + 0.38 * abs(nn[0]) + (0.12 if nn[1] < -0.5 else 0.0)
            ok = inside & (col[..., 3] > 0.01) & (dz >= depth[y0:y1, x0:x1] - 1e-6)
            sub_img = img[y0:y1, x0:x1]
            sub_img[ok, :3] = np.clip(col[ok, :3] * min(light, 1.0), 0, 1)
            sub_img[ok, 3] = col[ok, 3]
            depth[y0:y1, x0:x1][ok] = dz[ok]
    return img
