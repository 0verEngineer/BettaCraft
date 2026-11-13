"""M21.6 assets: the guppy and endler model with one part per tail shape and the per-gene mask textures.

The fixed-variety textures of ``tankmates.py`` stay for ``features.livebearerGenetics`` off. With the genetics the
renderer composites these greyscale masks on the CPU (``BettaSkins``, like the bettas): every mask is a 128x128 box-UV
texture of the guppy model; its grey value is the shading, its alpha the coverage of that gene. The first two layers
(``body``, ``fins``) give the coverage of the fish, the others are blended "atop" with the tint the Java side
(``client/render/GuppyLook``) picks from the genome.

* model: the livebearer body of ``tankmates.py`` with the tail group holding one plane per tail shape (``t_delta``,
  ``t_triangle``, ``t_double_sword``, ``t_top_sword``, ``t_bottom_sword``, ``t_lyre``, ``t_spade``, ``t_round``,
  ``t_flag``); the renderer shows the expressed one and scales it down for females,
* masks: body, fins, tail_color, tail_edge, belly, eyes, glint, gravid, spots_orange, spots_black (the wild Y),
  tail_spots, grass, mosaic, leopard (tail patterns), cobra, cobra_base, tuxedo, moscow, japan_blue, red_head, red_tail,
  tiger, black_bar, peacock_orange, peacock_green, peacock_spot.

Writes ``client/model/GuppyModelDefinitions.java`` and ``textures/entity/tankmates/guppy/*.png``; preview
``build/preview/guppies.png`` (side views of composited strains, same blending and colours as the game).

Run from the repo root:  py tools/assetgen/guppies.py
"""
from __future__ import annotations

import math

import numpy as np

import tankmates as tm
from common import JAVA_SRC, PREVIEW, save_png
from tankmates import Group, box, eyes, fusiform_plan, pair, pectorals, rot_matrix, faces, fin_coords, smooth

W, H = 128, 128
OUT = tm.TM_TEX / "guppy"

# tail shapes: (height, length) of the plane in model pixels; the delta is the old male tail (14 x 12)
SHAPES = {
    "delta": (14, 12), "triangle": (11, 11), "double_sword": (14, 16), "top_sword": (14, 15), "bottom_sword": (14, 15),
    "lyre": (13, 13), "spade": (9, 9), "round": (8, 7), "flag": (9, 15),
}


# ---------------------------------------------------------------------------------------------------------- model

def model():
    p = fusiform_plan("livebearer", depth=8.0, width=4.0, mid=6.0, peak=0.36, head=0.55, ped_frac=0.36)
    body = Group("body", (0, 0, 0))
    body.boxes += p.segments()
    body.boxes += eyes(p, 0.12, up=0.28)
    body.boxes += pair("pelvic", "pelvic", (0.8, p.bottom(0.45) + 0.6, p.snout + 0.45 * 24), (0, 2, 2), (14, 18, 0), "oval")
    male = Group("male", (0, 0, 0))
    male.boxes.append(box("gonopodium", "anal", (0, p.bottom(0.55) + 1.2, p.snout + 0.55 * 24 + 2), (0, 1, 5), (30, 0, 0), fin="rod"))
    male.boxes.append(box("dorsal_m", "dorsal", (0, p.top(0.55) - 2.5, p.snout + 0.5 * 24 + 4), (0, 5, 8), (-14, 0, 0), fin="flag"))
    female = Group("female", (0, 0, 0))
    female.boxes.append(box("anal_f", "anal", (0, p.bottom(0.62) + 1.0, p.snout + 0.6 * 24 + 2), (0, 3, 4), (8, 0, 0), fin="anal"))
    female.boxes.append(box("dorsal_f", "dorsal", (0, p.top(0.5) - 1.5, p.snout + 0.48 * 24 + 2), (0, 3, 4), (-10, 0, 0), fin="tri"))
    tail = Group("tail", (0, -6.0, p.ped - 1))
    for name, (h, d) in SHAPES.items():
        tail.boxes.append(box("t_" + name, "tail", (0, -6.0, p.ped - 1 + d / 2), (0, h, d), fin="g_" + name))
    return p, [body, male, female, tail] + pectorals(p, 0.22, 4, 2)


def pack(groups):
    boxes = [b for g in groups for b in g.boxes]
    order = sorted(boxes, key=lambda b: (-tm.footprint(b)[1], -tm.footprint(b)[0]))
    x = y = row = 0
    for b in order:
        fw, fh = tm.footprint(b)
        if x + fw > W:
            x, y, row = 0, y + row, 0
        if y + fh > H:
            raise ValueError("texture too small")
        b.u, b.v = x, y
        x += fw
        row = max(row, fh)


# ---------------------------------------------------------------------------------------------------------- outlines

def lobe(a, s, length=0.45, width=0.55):
    """The short central part of a sword tail (from the base)."""
    return (a / length) ** 2 + (s / width) ** 2 <= 1.0 or (a < length * 0.5 and abs(s) <= width)


def sword(a, s, sign):
    """A sword ray along the top (sign -1) or bottom (+1) edge, slightly diverging, to the end of the plane."""
    c = sign * (0.42 + 0.5 * a)
    w = 0.16 * (1 - 0.55 * a) + 0.05
    return abs(s - c) <= w and a <= 0.99


def outline(shape, a, s):
    if shape == "g_delta":
        return abs(s) <= 0.18 + 0.82 * a and a <= 1.0 - 0.08 * s * s
    if shape == "g_triangle":
        return abs(s) <= 0.22 + 0.62 * a and a <= 1.0 - 0.15 * s * s
    if shape == "g_double_sword":
        return lobe(a, s) or sword(a, s, -1) or sword(a, s, 1)
    if shape == "g_top_sword":
        return lobe(a, s) or sword(a, s, -1)
    if shape == "g_bottom_sword":
        return lobe(a, s) or sword(a, s, 1)
    if shape == "g_lyre":
        # a short fork with long, outward-curving edge rays
        if a < 0.55:
            return abs(s) <= 0.3 + 0.7 * a / 0.55
        return abs(s) >= 0.62 + 0.2 * (a - 0.55) and abs(s) <= 1.0 and a <= 0.98
    if shape == "g_spade":
        w = (0.3 + 0.7 * min(1.0, a / 0.45)) * (1.0 if a < 0.45 else max(0.0, (1 - a) / 0.55))
        return abs(s) <= w
    if shape == "g_round":
        return abs(s) <= 0.4 + 0.6 * a ** 0.5 and a <= 1.0 - 0.3 * s * s
    if shape == "g_flag":
        return abs(s) <= 0.62 + 0.18 * a and a <= 1.0 - 0.05 * s * s
    return tm.fin_alpha(shape, a, s)


# ---------------------------------------------------------------------------------------------------------- texel samples

class S:
    """One texel: where it is on the fish."""
    __slots__ = ("u", "v", "part", "box", "face", "lp", "x", "y", "z", "t", "vv", "a", "s")


def samples(plan, groups):
    out = []
    for g in groups:
        for b in g.boxes:
            R = rot_matrix(b.rot)
            c = np.array(b.center)
            for face, u0, v0, fw, fh, fn in faces(b):
                for j in range(int(math.ceil(fh))):
                    for i in range(int(math.ceil(fw))):
                        uu, vv = int(u0 + i), int(v0 + j)
                        if not (0 <= uu < W and 0 <= vv < H):
                            continue
                        lp = fn(i + 0.5, j + 0.5)
                        p = c + R @ np.array(lp)
                        x, y, z = p
                        fin = b.fin is not None or b.part in ("tail", "dorsal", "anal", "pectoral", "pelvic")
                        a = s = 0.0
                        if fin:
                            a, s = fin_coords(b, lp)
                            a, s = max(0.0, min(1.0, a)), max(-1.0, min(1.0, s))
                            if not outline(b.fin, a, s):
                                continue
                        k = S()
                        k.u, k.v, k.part, k.box, k.face, k.lp = uu, vv, b.part, b.name, face, lp
                        k.x, k.y, k.z = x, y, z
                        k.t = max(0.0, min(1.0, plan.t_of(z)))
                        k.vv = plan.v_of(y, z)
                        k.a, k.s = a, s
                        out.append(k)
    return out


def is_body(k):
    return k.part in ("body", "head")


def is_tail(k):
    return k.part == "tail" or (k.part == "dorsal" and k.box == "dorsal_m")


def is_fin(k):
    return k.part in ("tail", "dorsal", "anal", "pectoral", "pelvic")


# ---------------------------------------------------------------------------------------------------------- patterns

RNG = np.random.default_rng(2160)
NOISE = RNG.random((96, 96))


def _blur(a, r):
    k = np.exp(-np.arange(-2 * r, 2 * r + 1) ** 2 / (2.0 * r * r))
    k /= k.sum()
    a = np.apply_along_axis(lambda m: np.convolve(np.concatenate([m[-2 * r:], m, m[:2 * r]]), k, "valid"), 0, a)
    a = np.apply_along_axis(lambda m: np.convolve(np.concatenate([m[-2 * r:], m, m[:2 * r]]), k, "valid"), 1, a)
    return (a - a.min()) / (a.max() - a.min())


BLOBS = _blur(RNG.random((96, 96)), 1)


def blob(z, y, seed=0):
    """Smooth noise (irregular blobs about two pixels across)."""
    return BLOBS[int((z + 40) * 2 + seed * 13) % 96, int((y + 40) * 2 + seed * 31) % 96]


def noise(z, y, scale=1.0, seed=0):
    return NOISE[int((z + 40) * scale + seed * 17) % 96, int((y + 40) * scale + seed * 29) % 96]


def disc(k, spots):
    """Alpha of a set of round spots (z, y, r) in the side view."""
    for (z, y, r) in spots:
        d = math.hypot(k.z - z, k.y - y)
        if d <= r:
            return 1.0 if d <= r - 0.6 else 0.6
    return 0.0


def tail_ab(k):
    """Position on a tail plane in model pixels from the base (for spot grids that look the same on every shape)."""
    return k.z - 8.0, k.y + 6.0


def m_body(k):
    if is_body(k):
        g = 0.74 + 0.26 * k.vv
        if k.face == "top":
            g *= 0.9
        return g, 1.0
    if k.part == "eye":
        return 0.9, 1.0
    return 0, 0


def m_fins(k):
    if not is_fin(k):
        return 0, 0
    ray = 0.9 if int((k.s + 1) * 7) % 2 == 0 and k.part in ("tail", "dorsal") else 1.0
    edge = 1.0 if k.a > 0.9 else 0.97
    return 0.95 * ray * edge, 1.0


def m_tail_color(k):
    if not is_tail(k):
        return 0, 0
    ray = 0.86 if int((k.s + 1) * 7) % 2 == 0 else 1.0
    base = 0.85 + 0.15 * min(1.0, k.a * 2)
    return ray * base, 1.0


def m_tail_edge(k):
    if not is_tail(k):
        return 0, 0
    if k.part == "dorsal":
        return (1.0, 1.0) if k.s > 0.55 else (0, 0)
    e = abs(k.s)
    if e > 0.6:
        return 1.0, 1.0
    if e > 0.45:
        return 0.9, 0.55
    return 0, 0


def m_belly(k):
    if not is_body(k):
        return 0, 0
    return 1.0, 0.65 * smooth(0.5, 0.95, k.vv) + (0.25 if k.face == "bottom" else 0)


def m_eyes(k):
    return (1.0, 1.0) if k.part == "eye" else (0, 0)


def m_glint(k):
    if k.part != "eye":
        return 0, 0
    if k.face in ("east", "west") and k.lp[1] < 0 and k.lp[2] < 0:
        return 1.0, 1.0
    return 0, 0


def m_gravid(k):
    if not is_body(k):
        return 0, 0
    if 0.4 < k.t < 0.62 and k.vv > 0.58:
        return 0.8, 0.85 * smooth(0.58, 0.75, k.vv)
    return 0, 0


def m_spots_orange(k):
    if not is_body(k):
        return 0, 0
    a = disc(k, [(-2.0, -6.0, 1.8), (3.0, -4.5, 1.5), (6.5, -7.0, 1.2), (-6.0, -4.5, 1.1)])
    return 1.0, a


def m_spots_black(k):
    if not is_body(k):
        return 0, 0
    a = disc(k, [(-5.5, -7.2, 1.0), (1.0, -7.6, 1.1), (5.0, -5.0, 0.9), (8.0, -6.0, 0.8)])
    return 0.9, a


def m_tail_spots(k):
    if not is_tail(k):
        return 0, 0
    n = noise(k.z, k.y, 1.0, 1)
    return (0.9, 1.0) if n > 0.8 and k.a > 0.12 else (0, 0)


def m_grass(k):
    if not is_tail(k):
        return 0, 0
    n = noise(k.z, k.y, 1.0, 2)
    return (0.85, 1.0) if n > 0.6 and k.a > 0.1 else (0, 0)


def m_mosaic(k):
    if not is_tail(k):
        return 0, 0
    z, y = tail_ab(k)
    w = math.sin(z * 1.5 + 1.4 * math.sin(y * 0.9)) * math.cos(y * 1.3 + 1.2 * math.sin(z * 0.7))
    fade = 1 - smooth(0.55, 0.95, k.a)
    return (0.85, 1.0) if w > 0.15 and fade > 0.3 else (0, 0)


def m_leopard(k):
    if is_tail(k):
        return (0.9, 1.0) if blob(k.z, k.y, 3) > 0.55 and k.a > 0.1 else (0, 0)
    return 0, 0


def m_cobra(k):
    if not is_body(k) or k.t < 0.14 or k.vv > 0.86:
        return 0, 0
    w1 = math.sin(k.z * 1.25 + 1.8 * math.sin(k.y * 0.9))
    w2 = math.sin(k.y * 1.7 + 1.5 * math.sin(k.z * 0.8))
    return (0.9, 1.0) if w1 > 0.5 or w2 > 0.8 else (0, 0)


def m_cobra_base(k):
    if not is_body(k) or k.t < 0.1:
        return 0, 0
    return 1.0, 0.75 * (1 - 0.7 * smooth(0.6, 0.95, k.vv))


def m_tuxedo(k):
    if not is_body(k):
        return 0, 0
    a = smooth(0.44, 0.52, k.t) * (1 - smooth(0.82, 0.92, k.vv))
    return 0.9, a


def m_moscow(k):
    if not is_body(k):
        return 0, 0
    a = 0.95 * (1 - 0.6 * smooth(0.72, 0.95, k.vv)) * (0.55 + 0.45 * smooth(0.05, 0.25, k.t))
    return 0.85 + 0.15 * (1 - k.vv), a


def m_japan_blue(k):
    if not is_body(k):
        return 0, 0
    d = ((k.t - 0.4) / 0.17) ** 2 + ((k.vv - 0.42) / 0.3) ** 2
    d2 = ((k.t - 0.86) / 0.09) ** 2 + ((k.vv - 0.4) / 0.25) ** 2
    a = 1.0 if d <= 1 or d2 <= 1 else 0.5 if d <= 1.4 else 0.0
    return 1.0 if noise(k.z, k.y, 1, 5) > 0.3 else 0.85, a


def m_red_head(k):
    if not is_body(k):
        return 0, 0
    a = (1 - smooth(0.2, 0.34, k.t)) * (1 - smooth(0.4, 0.6, k.vv))
    return 1.0, a * 0.95


def m_red_tail(k):
    if not is_body(k):
        return 0, 0
    a = smooth(0.48, 0.72, k.t) * (1 - smooth(0.85, 0.98, k.vv))
    return 1.0, a * 0.95


def m_tiger(k):
    if not is_body(k) or k.t < 0.3 or k.vv > 0.84:
        return 0, 0
    w = math.sin((k.z + k.y * 0.35) * 1.45)
    return (0.9, 1.0) if w > 0.35 else (0, 0)


def m_black_bar(k):
    if not is_body(k):
        return 0, 0
    a = smooth(0.40, 0.44, k.t) * (1 - smooth(0.56, 0.6, k.t)) * (1 - smooth(0.84, 0.94, k.vv))
    return 0.9, a


def m_peacock_orange(k):
    if not is_body(k):
        return 0, 0
    return 1.0, disc(k, [(-1.0, -5.0, 2.3), (5.0, -7.0, 1.8), (7.6, -5.0, 1.4), (-6.0, -4.4, 1.3)])


def m_peacock_green(k):
    if not is_body(k):
        return 0, 0
    return 1.0, disc(k, [(-5.0, -7.4, 1.9), (2.4, -7.6, 1.7), (3.4, -4.4, 1.4), (-2.4, -8.0, 1.2)])


def m_peacock_spot(k):
    if not is_body(k):
        return 0, 0
    return 0.9, disc(k, [(1.0, -5.8, 1.4), (7.8, -6.4, 1.0)])


MASKS = {
    "body": m_body, "fins": m_fins, "tail_color": m_tail_color, "tail_edge": m_tail_edge, "belly": m_belly, "eyes": m_eyes,
    "glint": m_glint, "gravid": m_gravid, "spots_orange": m_spots_orange, "spots_black": m_spots_black, "tail_spots": m_tail_spots,
    "grass": m_grass, "mosaic": m_mosaic, "leopard": m_leopard, "cobra": m_cobra, "cobra_base": m_cobra_base, "tuxedo": m_tuxedo,
    "moscow": m_moscow, "japan_blue": m_japan_blue, "red_head": m_red_head, "red_tail": m_red_tail, "tiger": m_tiger,
    "black_bar": m_black_bar, "peacock_orange": m_peacock_orange, "peacock_green": m_peacock_green, "peacock_spot": m_peacock_spot,
}


def paint_masks(plan, groups):
    ks = samples(plan, groups)
    out = {}
    for name, fn in MASKS.items():
        img = np.zeros((H, W, 4))
        for k in ks:
            g, a = fn(k)
            if a <= 0:
                continue
            g = float(np.clip(g * (0.97 + 0.06 * NOISE[k.u % 96, k.v % 96]), 0, 1)) if name in ("body", "fins") else g
            img[k.v, k.u, :3] = g
            img[k.v, k.u, 3] = min(1.0, a)
        out[name] = img
    return out


# ---------------------------------------------------------------------------------------------------------- Java

def write_java(groups):
    old_w, old_h = tm.TEX_W, tm.TEX_H
    tm.TEX_W, tm.TEX_H = W, H
    try:
        layer = tm.java_layer("guppy", groups)
    finally:
        tm.TEX_W, tm.TEX_H = old_w, old_h
    lines = ["// GENERATED by tools/assetgen/guppies.py - do not edit by hand, edit guppies.py and rerun it",
             "package org.overengineer.bettacraft.client.model;",
             "",
             "import net.minecraft.client.model.geom.PartPose;",
             "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
             "import net.minecraft.client.model.geom.builders.LayerDefinition;",
             "import net.minecraft.client.model.geom.builders.MeshDefinition;",
             "import net.minecraft.client.model.geom.builders.PartDefinition;",
             "",
             "/**",
             " * M21.6: the guppy and endler model of the livebearer genetics: the livebearer body (TankmateModelDefinitions) with",
             " * one plane per tail shape in the tail group (t_delta, t_triangle, t_double_sword, t_top_sword, t_bottom_sword,",
             " * t_lyre, t_spade, t_round, t_flag). Masks: textures/entity/tankmates/guppy/*.png (" + f"{W}x{H}" + " box UV).",
             " */",
             "public final class GuppyModelDefinitions {",
             "    /** The tail shapes (part t_<shape>), as in the genome (LbLocus.TAIL). */",
             "    public static final String[] SHAPES = {" + ", ".join(f'"{s}"' for s in SHAPES) + "};",
             "",
             "    private GuppyModelDefinitions() {}",
             "",
             layer.replace("createGuppyLayer", "createLayer"),
             "}",
             ""]
    path = JAVA_SRC / "client" / "model" / "GuppyModelDefinitions.java"
    path.write_text("\n".join(lines), encoding="utf-8", newline="\n")
    return path


# ---------------------------------------------------------------------------------------------------------- preview (same blending as BettaSkins)

def composite(masks, layers):
    base = masks[layers[0][0]]
    out = np.zeros_like(base)
    for i, (name, argb) in enumerate(layers):
        m = masks[name]
        tint = np.array([((argb >> 16) & 255) / 255, ((argb >> 8) & 255) / 255, (argb & 255) / 255])
        ta = ((argb >> 24) & 255) / 255
        sa = m[..., 3] * ta
        src = m[..., :3] * tint
        if i >= 2:
            sel = (out[..., 3] > 0)[..., None]
            out[..., :3] = np.where(sel, out[..., :3] + (src - out[..., :3]) * sa[..., None], out[..., :3])
            continue
        da = out[..., 3]
        oa = sa + da * (1 - sa)
        safe = np.where(oa > 0, oa, 1)
        out[..., :3] = (src * sa[..., None] + out[..., :3] * (da * (1 - sa))[..., None]) / safe[..., None]
        out[..., 3] = oa
    return out


PREVIEW_FISH = [
    # (title, layers) with argb tints like GuppyLook.java
    ("wild", [("body", 0xFFA8A48A), ("fins", 0xFFD8D2C0), ("tail_color", 0x80D8CCA8), ("belly", 0xFFEDEBE0), ("spots_orange", 0xFFF07A28),
              ("spots_black", 0xFF1A1A1A), ("tail_spots", 0xFF2A2420), ("eyes", 0xFF141414), ("glint", 0xFFE0E0E0)], "round"),
    ("red cobra", [("body", 0xFFA8A48A), ("fins", 0xFFD8D2C0), ("tail_color", 0xFFE02A20), ("belly", 0xFFEDEBE0), ("cobra_base", 0xFFE04A30),
                   ("cobra", 0xFF1E1A16), ("leopard", 0xFF1E1A16), ("eyes", 0xFF141414), ("glint", 0xFFE0E0E0)], "delta"),
    ("blue moscow", [("body", 0xFF7890C0), ("fins", 0xFFD8D2C0), ("tail_color", 0xFF2A50C8), ("belly", 0xFFEDEBE0), ("moscow", 0xFF22305A),
                     ("eyes", 0xFF141414), ("glint", 0xFFE0E0E0)], "delta"),
    ("tuxedo koi", [("body", 0xFFF2F0EA), ("fins", 0xFFE8E4DA), ("tail_color", 0xFFE02A20), ("belly", 0xFFFFFFFF), ("tuxedo", 0xFF161414),
                    ("red_head", 0xFFE0281C), ("eyes", 0xFF141414), ("glint", 0xFFE0E0E0)], "delta"),
    ("endler tiger", [("body", 0xFFA8A48A), ("fins", 0xFFD8D2C0), ("tail_color", 0x60F07A1A), ("tail_edge", 0xFFF07A1A), ("belly", 0xFFEDEBE0),
                      ("peacock_orange", 0xFFF06A1A), ("tiger", 0xFF141414), ("eyes", 0xFF141414), ("glint", 0xFFE0E0E0)], "double_sword"),
    ("endler wild", [("body", 0xFFA8A48A), ("fins", 0xFFD8D2C0), ("tail_color", 0x60F07A1A), ("tail_edge", 0xFFF07A1A), ("belly", 0xFFEDEBE0),
                     ("peacock_orange", 0xFFF06A1A), ("peacock_green", 0xFF3AE070), ("peacock_spot", 0xFF141414), ("eyes", 0xFF141414),
                     ("glint", 0xFFE0E0E0)], "top_sword"),
    ("full gold f", [("body", 0xFFD8B860), ("fins", 0xFFD8D2C0), ("tail_color", 0x70E8C840), ("belly", 0xFFF0EAD0), ("gravid", 0xFF5A5040),
                     ("eyes", 0xFF141414), ("glint", 0xFFE0E0E0)], "round"),
    ("blue grass", [("body", 0xFF7890C0), ("fins", 0xFFD8D2C0), ("tail_color", 0xFF3A70E0), ("belly", 0xFFEDEBE0), ("grass", 0xFF141830),
                    ("eyes", 0xFF141414), ("glint", 0xFFE0E0E0)], "triangle"),
    ("red mosaic", [("body", 0xFFA8A48A), ("fins", 0xFFD8D2C0), ("tail_color", 0xFFE02A20), ("belly", 0xFFEDEBE0), ("mosaic", 0xFF2A1410),
                    ("eyes", 0xFF141414), ("glint", 0xFFE0E0E0)], "flag"),
    ("japan blue", [("body", 0xFFA8A48A), ("fins", 0xFFD8D2C0), ("tail_color", 0xFF3A70E0), ("belly", 0xFFEDEBE0), ("japan_blue", 0xFF50B0FF),
                    ("tail_spots", 0xFF202020), ("eyes", 0xFF141414), ("glint", 0xFFE0E0E0)], "lyre"),
    ("albino red", [("body", 0xFFF2DCD4), ("fins", 0xFFEEE0D8), ("tail_color", 0xFFF03028), ("belly", 0xFFFFFFFF), ("red_tail", 0xFFF03028),
                    ("eyes", 0xFFD02020), ("glint", 0xFFFFC0C0)], "spade"),
    ("black bar", [("body", 0xFFA8A48A), ("fins", 0xFFD8D2C0), ("tail_color", 0x60F07A1A), ("tail_edge", 0xFFF0B020), ("belly", 0xFFEDEBE0),
                   ("peacock_green", 0xFF3AE070), ("black_bar", 0xFF141414), ("eyes", 0xFF141414), ("glint", 0xFFE0E0E0)], "bottom_sword"),
]


def preview(masks, groups):
    from PIL import Image
    old = tm.TEX_W, tm.TEX_H
    tm.TEX_W, tm.TEX_H = W, H
    try:
        return _preview(masks, groups)
    finally:
        tm.TEX_W, tm.TEX_H = old


def _preview(masks, groups):
    from PIL import Image
    tiles = []
    for title, layers, shape in PREVIEW_FISH:
        tex = composite(masks, layers)
        female = title.endswith(" f")
        hide_tail = [g for g in groups if g.name == "tail"][0]
        keep = [b for b in hide_tail.boxes]
        hide_tail.boxes = [b for b in keep if b.name == "t_" + shape]
        hide = ("male",) if female else ("female",)
        side = tm.render_view(groups, tex, "side", hide=hide)
        hide_tail.boxes = keep
        tiles.append(side)
    cols = 4
    th, tw = tiles[0].shape[:2]
    rows = (len(tiles) + cols - 1) // cols
    sheet = np.zeros((rows * th, cols * tw, 4))
    for k, t in enumerate(tiles):
        r, c = divmod(k, cols)
        sheet[r * th:(r + 1) * th, c * tw:(c + 1) * tw] = t
    PREVIEW.mkdir(parents=True, exist_ok=True)
    p = PREVIEW / "guppies.png"
    Image.fromarray(np.clip(np.rint(sheet * 255), 0, 255).astype(np.uint8), "RGBA").save(p)
    return p


def write_all():
    plan, groups = model()
    pack(groups)
    written = [write_java(groups)]
    masks = paint_masks(plan, groups)
    for name, img in masks.items():
        written.append(save_png(img, OUT / f"{name}.png"))
    written.append(preview(masks, groups))
    return written


if __name__ == "__main__":
    for p in write_all():
        print(p)
