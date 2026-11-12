"""Procedural layer masks on the shared 64x64 betta UV layout.

Output (src/main/resources/assets/bettacraft/textures/entity/betta/):
  shade.png          RGB = volume shading (grey), A = whole-model coverage
  pattern_<name>.png RGB = the same shading, A = pattern coverage (partial alpha allowed)
  iridescence.png    RGB = sheen brightness, A = sheen strength
  eye.png            fixed-colour eyes (not tinted)

All random fields are functions of the texel's *model-space* position (|x|, y, z), never of the texel
itself. The two coplanar faces of a fin therefore always get identical pixels (no z-fighting) and
left/right body sides are mirror images.
"""
from __future__ import annotations

import numpy as np

from common import ENTITY_TEX, save_png
from uv_template import N, meta

SEED = 0xBE77A


# ---------------------------------------------------------------- noise helpers

def _hash(*ints, seed=SEED):
    h = np.uint64((seed * 0x9E3779B97F4A7C15) & 0xFFFFFFFFFFFFFFFF)
    h = np.full(np.shape(ints[0]), h, dtype=np.uint64)
    for k, a in enumerate(ints):
        a = np.asarray(a).astype(np.int64).astype(np.uint64)
        h ^= a * np.uint64([0x85EBCA6B, 0xC2B2AE35, 0x27D4EB2F, 0x165667B1][k % 4]) + np.uint64(k + 1)
        h ^= h >> np.uint64(29)
        h *= np.uint64(0xBF58476D1CE4E5B9)
        h ^= h >> np.uint64(32)
    return (h >> np.uint64(11)).astype(np.float64) / float(1 << 53)


def texel_random(seed: int) -> np.ndarray:
    """Per-texel random in [0,1), symmetric (depends on rounded |x|, y, z)."""
    P = meta().pos
    q = np.rint(np.stack([np.abs(P[..., 0]), P[..., 1], P[..., 2]], -1) * 4).astype(np.int64)
    return _hash(q[..., 0], q[..., 1], q[..., 2], seed=seed)


def value_noise(x, y, freq, seed):
    x, y = x * freq, y * freq
    x0, y0 = np.floor(x), np.floor(y)
    fx, fy = x - x0, y - y0
    sx, sy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    g = lambda i, j: _hash(x0 + i, y0 + j, seed=seed)
    a = g(0, 0) * (1 - sx) + g(1, 0) * sx
    b = g(0, 1) * (1 - sx) + g(1, 1) * sx
    return a * (1 - sy) + b * sy


def fbm(x, y, freq, seed, octaves=3):
    tot, amp, norm = 0.0, 1.0, 0.0
    for o in range(octaves):
        tot = tot + amp * value_noise(x, y, freq * 2 ** o, seed + o * 101)
        norm += amp
        amp *= 0.5
    return tot / norm


def _zy():
    P = meta().pos
    return P[..., 2], P[..., 1] - 20.0  # z (head -8 .. tail +), y centred on the body axis


def quantize(a, levels=4):
    return np.round(np.clip(a, 0, 1) * levels) / levels


# ---------------------------------------------------------------- shading

def shading() -> np.ndarray:
    m = meta()
    z, y = _zy()
    s = np.zeros((N, N))
    body, fin = m.body, m.fin
    side = np.isin(m.face, ["west", "east"])
    face_base = {"top": 1.0, "bottom": .62, "north": .82, "south": .74}
    for f, val in face_base.items():
        s[body & (m.face == f)] = val
    # side faces: light back, darker belly, lighter upper flank
    ft = m.ft
    sb = body & side
    s[sb] = (0.97 - 0.30 * ft[sb] ** 1.6)[...]
    top_row = sb & (ft < 0.2)
    s[top_row] *= 0.93
    # subtle scale texture
    r = texel_random(11)
    s[body] *= 0.97 + 0.06 * r[body]
    # gill cover line on the head sides
    gill = (m.key == "head") & side & (z > -5) & (ft > 0.2)
    s[gill] *= 0.74
    # mouth: dark lower row on the snout front, darker lips on the snout sides
    mouth = (m.key == "snout") & (m.face == "north") & (ft > 0.5)
    s[mouth] = 0.35
    lips = (m.key == "snout") & side & (ft > 0.5)
    s[lips] *= 0.85
    # fins: rays brighter than the membrane, lighter outer rim
    t = m.fin_t
    fv = 0.74 + 0.12 * t
    fv = np.where(m.fin_ray, fv + 0.16, fv)
    fv = np.where(t < 0.12, np.maximum(fv, 0.86), fv)
    fv = np.where(m.fin_edge == 1, fv + 0.05, fv)
    s[fin] = np.clip(fv[fin], 0, 1)
    s[~m.cover] = 0
    return s


def _rgba(alpha: np.ndarray, shade: np.ndarray | None = None) -> np.ndarray:
    m = meta()
    sh = shading() if shade is None else shade
    a = np.where(m.cover, np.clip(alpha, 0, 1), 0)
    arr = np.zeros((N, N, 4))
    arr[..., 0] = arr[..., 1] = arr[..., 2] = np.where(a > 0, sh, 0)
    arr[..., 3] = a
    return arr


# ---------------------------------------------------------------- patterns (alpha masks)

def p_butterfly():
    m = meta()
    t = m.fin_t
    return (m.fin & (t >= 0.56) & (t <= 0.86)).astype(float)


def p_marble():
    m = meta()
    z, y = _zy()
    # jittered grid voronoi, every cell randomly on/off, borders wobbled by noise
    cell = 2.7
    wz = z + 1.1 * (fbm(z, y, 0.45, 301) - .5)
    wy = y + 1.1 * (fbm(z + 9, y, 0.45, 302) - .5)
    gz, gy = np.floor(wz / cell), np.floor(wy / cell)
    best = np.full((N, N), 1e9)
    on = np.zeros((N, N))
    for dz in (-1, 0, 1):
        for dy in (-1, 0, 1):
            cz, cy = gz + dz, gy + dy
            pz = (cz + _hash(cz, cy, seed=401)) * cell
            py = (cy + _hash(cz, cy, seed=402)) * cell
            dist = np.hypot(wz - pz, wy - py)
            closer = dist < best
            best = np.where(closer, dist, best)
            on = np.where(closer, (_hash(cz, cy, seed=403) < 0.48).astype(float), on)
    return on * m.cover


def p_dragon():
    m = meta()
    z, y = _zy()
    side = np.isin(m.face, ["west", "east"])
    body = m.body
    row = np.floor(y + 0.5)
    col = np.floor((z + 0.5 * (row % 2)))
    front = ((col % 2) == 0)
    a = np.where(front, 0.95, 0.62)
    a = np.where(side, a, np.where(m.face == "top", 0.9, np.where(m.face == "bottom", 0.4, 0.7)))
    a = np.where(m.key == "head", a * 0.8, a)
    a = np.where(m.key == "snout", 0.5, a)
    # dragon scaling creeps a little into the fin bases
    fin_base = m.fin & (m.fin_t < 0.18)
    return np.where(body, a, np.where(fin_base, 0.5, 0.0))


def p_koi():
    m = meta()
    z, y = _zy()
    n = fbm(z, y, 0.22, 501, octaves=3)
    a = (n > 0.52).astype(float)
    return a * m.cover


def p_mask():
    m = meta()
    z, _ = _zy()
    head = m.cover & np.isin(m.key, ["head", "snout"])
    a = head.astype(float)
    a = np.where(m.body & (m.key == "body") & (z < -2), 0.5, a)
    return a


def p_tipped():
    m = meta()
    return (m.fin & (m.fin_edge >= 1) & (m.fin_edge <= 2)).astype(float)


def p_fade():
    m = meta()
    z, _ = _zy()
    fin_a = quantize((m.fin_t - 0.2) / 0.7, 4)
    body_a = quantize((z - 0.0) / 6.0, 4) * 0.75
    return np.where(m.fin, fin_a, np.where(m.body, body_a, 0))


def p_grizzle():
    m = meta()
    z, y = _zy()
    r = texel_random(601)
    streak = fbm(z * 0.5, y * 2.0, 0.9, 602, octaves=2)  # vertical-ish streaks on the body
    body_a = ((r < 0.22) | ((streak > 0.62) & (r < 0.7))).astype(float) * 0.85
    fin_ray = m.fin_ray & (value_noise(z, y, 0.9, 603) > 0.42)
    fin_a = np.where(fin_ray, 0.9, np.where(r < 0.12, 0.7, 0.0))
    return np.where(m.body, body_a, np.where(m.fin, fin_a, 0))


def p_bars():
    """Vertical breeding bars (females), body sides only."""
    m = meta()
    z, _ = _zy()
    side = np.isin(m.face, ["west", "east"]) & m.body & np.isin(m.key, ["body", "peduncle"])
    bar = (np.floor(z + 3) % 2 == 0)
    a = np.where(bar, np.where((m.ft > 0.15) & (m.ft < 0.9), 0.85, 0.5), 0.0)
    return np.where(side, a, 0)


PATTERNS = {
    "butterfly": p_butterfly,
    "marble": p_marble,
    "dragon": p_dragon,
    "koi": p_koi,
    "mask": p_mask,
    "tipped": p_tipped,
    "fade": p_fade,
    "grizzle": p_grizzle,
    "bars": p_bars,
}


def pattern_rgba(name: str) -> np.ndarray:
    return _rgba(PATTERNS[name]())


# ---------------------------------------------------------------- iridescence / eye / shade

def iridescence_rgba() -> np.ndarray:
    m = meta()
    z, y = _zy()
    side = np.isin(m.face, ["west", "east"])
    r = texel_random(701)
    glint = r > 0.62
    body_a = np.where(side, np.where(glint, 0.5, 0.22), 0.18)
    body_a = np.where(side & (m.ft < 0.3), body_a + 0.1, body_a)  # back shimmers most
    body_a = np.where(side & (m.ft > 0.75), body_a * 0.4, body_a)  # little on the belly
    body_a = np.where(m.key == "snout", 0.05, body_a)
    fin_a = np.where(m.fin_ray, 0.38, 0.1) * np.clip(1.1 - m.fin_t, 0.15, 1)
    a = np.where(m.body, body_a, np.where(m.fin, fin_a, 0))
    a = quantize(a, 8)
    arr = np.zeros((N, N, 4))
    arr[..., :3] = np.where(m.cover, 0.85 + 0.15 * r, 0)[..., None]
    arr[..., 3] = np.where(m.cover, a, 0)
    return arr


EYE_COLOURS = {
    (0, 0): "#e9edf2",  # front-top: highlight
    (1, 0): "#16141c",
    (0, 1): "#16141c",
    (1, 1): "#4a3b22",  # back-bottom: hint of a golden iris
}
EYE_Z = (-6.5, -5.5)  # model-space z of the two eye columns (front, back)
EYE_Y = (19.5, 20.5)  # model-space y of the two eye rows (top, bottom)


def eye_rgba() -> np.ndarray:
    m = meta()
    P = m.pos
    arr = np.zeros((N, N, 4))
    side = (m.key == "head") & np.isin(m.face, ["west", "east"])
    for (ci, ri), hexc in EYE_COLOURS.items():
        sel = side & (np.abs(P[..., 2] - EYE_Z[ci]) < 0.1) & (np.abs(P[..., 1] - EYE_Y[ri]) < 0.1)
        c = np.array([int(hexc[k:k + 2], 16) for k in (1, 3, 5)]) / 255.0
        arr[sel, :3] = c
        arr[sel, 3] = 1.0
    return arr


def shade_rgba() -> np.ndarray:
    m = meta()
    return _rgba(m.cover.astype(float))


def write_all() -> list:
    out = [save_png(shade_rgba(), ENTITY_TEX / "shade.png"),
           save_png(eye_rgba(), ENTITY_TEX / "eye.png"),
           save_png(iridescence_rgba(), ENTITY_TEX / "iridescence.png")]
    for name in PATTERNS:
        out.append(save_png(pattern_rgba(name), ENTITY_TEX / f"pattern_{name}.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
