"""Fin silhouettes and fin coordinate fields.

Every fin is a width-0 cuboid. Its texture is a d x h grid ("east orientation"): column 0 is the
front (minZ), row 0 is the top (minY). This module returns, per fin kind/style:

* ``mask``   bool (h, d)  - texel belongs to the fin (transparent elsewhere: spikes, notches),
* ``t``      float (h, d) - 0 at the fin base -> 1 at the outer edge, measured along the fin ray,
* ``ray``    bool (h, d)  - texel lies on a fin ray,
* ``edge``   int (h, d)   - chessboard distance (texels) to the outer edge (base edge excluded).
"""
from __future__ import annotations

import math
from functools import lru_cache

import numpy as np
from PIL import Image, ImageDraw

SS = 8  # supersampling for polygon coverage

# Normalised outlines (x: 0 = front/base side .. 1 = back, y: 0 = top .. 1 = bottom).
OUTLINES = {
    ("caudal", "plakat"): [(0, .30), (.30, .06), (.65, 0), (.92, .12), (1, .5), (.92, .88), (.65, 1), (.30, .94), (0, .70)],
    ("caudal", "veil"): [(0, .16), (.25, 0), (.55, .02), (.80, .22), (.96, .55), (1, .90), (.85, 1), (.6, .86), (.32, .68), (.1, .55), (0, .5)],
    ("caudal", "halfmoon"): [(0, 0), (.45, 0), (.80, .07), (1, .30), (1, .70), (.80, .93), (.45, 1), (0, 1)],
    ("caudal", "crowntail"): [(0, .06), (.45, 0), (.85, .12), (1, .5), (.85, .88), (.45, 1), (0, .94)],
    ("caudal", "doubletail"): [(0, .04), (.45, 0), (.85, .06), (1, .22), (.95, .40), (.38, .5), (.95, .60), (1, .78), (.85, .94), (.45, 1), (0, .96)],
    ("dorsal", "plakat"): [(0, 1), (.05, .5), (.3, .05), (.7, 0), (1, .35), (1, .8), (.9, 1)],
    ("dorsal", "veil"): [(0, 1), (.08, .55), (.3, .15), (.55, 0), (.85, .12), (1, .5), (1, .95), (.9, 1)],
    ("dorsal", "halfmoon"): [(0, 1), (.04, .45), (.2, .08), (.5, 0), (.85, .03), (1, .25), (1, 1)],
    ("dorsal", "crowntail"): [(0, 1), (.04, .45), (.2, .08), (.5, 0), (.85, .03), (1, .25), (1, 1)],
    ("dorsal", "doubletail"): [(0, 1), (.03, .35), (.2, .04), (.55, 0), (.9, .05), (1, .3), (1, 1)],
    ("anal", "plakat"): [(0, 0), (1, 0), (1, .55), (.85, 1), (.5, .85), (.15, .45), (0, .15)],
    ("anal", "veil"): [(0, 0), (1, 0), (1, .35), (.92, 1), (.62, .88), (.28, .55), (0, .22)],
    ("anal", "halfmoon"): [(0, 0), (1, 0), (1, .95), (.82, 1), (.5, .88), (.15, .5), (0, .2)],
    ("anal", "crowntail"): [(0, 0), (1, 0), (1, .95), (.82, 1), (.5, .88), (.15, .5), (0, .2)],
    ("anal", "doubletail"): [(0, 0), (1, 0), (1, .95), (.82, 1), (.5, .9), (.15, .55), (0, .2)],
    ("pectoral", "normal"): [(0, 0), (.6, 0), (1, .45), (.85, 1), (0, 1)],
    ("pectoral", "dumbo"): [(0, .12), (.35, 0), (.8, .1), (1, .5), (.85, .92), (.45, 1), (0, .8)],
    ("ventral", "normal"): [(0, 0), (1, 0), (.95, .45), (.75, 1), (.4, .75), (.1, .3)],
}

# Which side of the d x h grid is attached to the body.
BASE_SIDE = {"caudal": "left", "dorsal": "bottom", "anal": "top", "pectoral": "left", "ventral": "top"}

# Fin ray fan origin, normalised (may lie outside the grid, inside the body).
def _origin(kind: str, style: str, d: int, h: int, center_row: float | None) -> tuple[float, float]:
    if kind == "caudal":
        return -0.5, (center_row if center_row is not None else h / 2)
    if kind == "dorsal":
        return -0.3 * d, h + 0.35 * d
    if kind == "anal":
        return -0.4 * d, -0.3 * d
    if kind == "pectoral":
        return -0.5, h * 0.4
    return d * 0.3, -0.5  # ventral


def _chaikin(pts, it=2):
    pts = [tuple(p) for p in pts]
    for _ in range(it):
        out = []
        n = len(pts)
        for i in range(n):
            p, q = np.array(pts[i]), np.array(pts[(i + 1) % n])
            out += [tuple(.75 * p + .25 * q), tuple(.25 * p + .75 * q)]
        pts = out
    return pts


def _coverage(outline, d, h, smooth=2):
    img = Image.new("L", (d * SS, h * SS), 0)
    pts = _chaikin(outline, smooth) if smooth else outline
    # stretch slightly beyond [0,1] so edge columns/rows touching the border are full
    poly = [((x * (d + .02) - .01) * SS, (y * (h + .02) - .01) * SS) for x, y in pts]
    ImageDraw.Draw(img).polygon(poly, fill=255)
    a = np.asarray(img, dtype=np.float64).reshape(h, SS, d, SS).mean(axis=(1, 3)) / 255.0
    return a


def _bresenham(x0, y0, x1, y1):
    x0, y0, x1, y1 = int(round(x0)), int(round(y0)), int(round(x1)), int(round(y1))
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        yield x0, y0
        if x0 == x1 and y0 == y1:
            return
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def _angle_range(mask, O):
    ys, xs = np.nonzero(mask)
    ang = np.arctan2(ys + .5 - O[1], xs + .5 - O[0])
    return float(ang.min()) - 0.05, float(ang.max()) + 0.05


@lru_cache(maxsize=None)
def fin_fields(kind: str, style: str, d: int, h: int, center_row: float | None = None):
    O = _origin(kind, style, d, h, center_row)
    full = _coverage(OUTLINES[(kind, style)], d, h) >= 0.5
    spikes = None
    if style == "crowntail" and kind in ("caudal", "dorsal", "anal"):
        # reduced webbing: every texel line (rows for the caudal, columns for dorsal/anal) is cut
        # to ~60 % of its length, every second line continues to the outline as a 1-texel spike.
        lines = full if kind == "caudal" else full.T[:, ::-1] if kind == "dorsal" else full.T
        cut = np.zeros_like(lines)
        sp = np.zeros_like(lines)
        nl = lines.shape[0]
        centre = (nl - 1) / 2
        for k in range(nl):
            idx = np.nonzero(lines[k])[0]
            if len(idx) == 0:
                continue
            L = idx.max() + 1
            web = 0.62 if kind == "caudal" else 0.55
            keep = max(1, int(round(L * web)))
            spike = (abs(k - centre) % 2 < 0.5) if kind == "caudal" else (k % 2 == 1)
            cut[k, :keep] = lines[k, :keep]
            if spike:
                cut[k, :L] = lines[k, :L]
                sp[k, keep:L] = lines[k, keep:L]
        if kind == "caudal":
            mask, spikes = cut, sp
        elif kind == "dorsal":
            mask, spikes = cut[:, ::-1].T, sp[:, ::-1].T
        else:
            mask, spikes = cut.T, sp.T
    else:
        mask = full
    rays = None
    t, ray = _t_and_rays(mask, O, rays)
    if spikes is not None:
        ray = ray | spikes
    edge = _edge_distance(mask, BASE_SIDE[kind])
    return mask, t, ray, edge


def _march_exit(mask, O, a, step=0.05):
    h, d = mask.shape
    r, last = 0.0, 0.0
    rmax = math.hypot(d, h) + abs(O[0]) + abs(O[1]) + 2
    while r < rmax:
        x, y = O[0] + math.cos(a) * r, O[1] + math.sin(a) * r
        xi, yi = int(math.floor(x)), int(math.floor(y))
        if 0 <= xi < d and 0 <= yi < h and mask[yi, xi]:
            last = r
        r += step
    return last


def _entry_exit(mask, O, ang, step=0.05):
    """Per texel: distance along its ray from O where the mask starts (s0) and ends (s1)."""
    h, d = mask.shape
    rmax = math.hypot(d, h) + abs(O[0]) + abs(O[1]) + 2
    rs = np.arange(0, rmax, step)
    cx = O[0] + np.cos(ang)[..., None] * rs
    cy = O[1] + np.sin(ang)[..., None] * rs
    xi, yi = np.floor(cx).astype(int), np.floor(cy).astype(int)
    inside = (xi >= 0) & (xi < d) & (yi >= 0) & (yi < h)
    hit = np.zeros_like(inside)
    hit[inside] = mask[yi[inside], xi[inside]]
    any_hit = hit.any(-1)
    first = np.where(any_hit, np.argmax(hit, -1), 0)
    last = np.where(any_hit, len(rs) - 1 - np.argmax(hit[..., ::-1], -1), 0)
    return rs[first], rs[last] + step


def _t_and_rays(mask, O, rays=None):
    h, d = mask.shape
    jj, ii = np.mgrid[0:h, 0:d]
    px, py = ii + .5 - O[0], jj + .5 - O[1]
    dist = np.hypot(px, py)
    ang = np.arctan2(py, px)
    s0, s1 = _entry_exit(mask, O, ang)
    t = np.clip((dist - s0) / np.maximum(s1 - s0, 1e-6), 0, 1)
    # rays branch (double) with distance so their spacing stays ~3 texels
    a0, a1 = _angle_range(mask, O)
    arc = (a1 - a0) * np.maximum(dist, 1e-6)
    n = 2.0 ** np.floor(np.log2(np.maximum(arc / 3.0, 1.0)))
    phase = (ang - a0) / (a1 - a0) * n - 0.5
    off = np.abs(phase - np.round(phase)) * arc / n
    ray = (off <= 0.45) & mask & (t > 0.12)
    t = np.where(mask, t, 0)
    return t, ray


def _edge_distance(mask, base_side):
    """Chessboard distance to the nearest non-fin texel; the base side counts as fin."""
    h, d = mask.shape
    pad = np.zeros((h + 2, d + 2), dtype=bool)
    pad[1:-1, 1:-1] = mask
    if base_side == "left":
        pad[:, 0] = True
    elif base_side == "top":
        pad[0, :] = True
    elif base_side == "bottom":
        pad[-1, :] = True
    dist = np.where(pad, 99, 0)
    for k in range(1, 8):
        out = dist == k - 1
        grow = out.copy()
        grow[1:, :] |= out[:-1, :]
        grow[:-1, :] |= out[1:, :]
        grow[:, 1:] |= out[:, :-1]
        grow[:, :-1] |= out[:, 1:]
        grow[1:, 1:] |= out[:-1, :-1]
        grow[:-1, :-1] |= out[1:, 1:]
        grow[1:, :-1] |= out[:-1, 1:]
        grow[:-1, 1:] |= out[1:, :-1]
        dist = np.where((dist == 99) & grow, k, dist)
    return np.where(mask, dist[1:-1, 1:-1], 0)
