"""Block textures for the aquarium (16x16, deterministic)."""
from __future__ import annotations

import json
import math

import numpy as np

from common import BLOCK_TEX, save_png

S = 16
WATER_FRAMES = 32


def _rng(seed):
    return np.random.default_rng(seed)


def tank_edge() -> np.ndarray:
    """Opaque dark grey with very subtle noise (renderer samples a 1px strip for edge lines)."""
    n = _rng(101).integers(-3, 4, (S, S))
    v = np.clip(0x3a + n, 0, 255).astype(np.uint8)
    out = np.zeros((S, S, 4), np.uint8)
    out[..., 0] = out[..., 1] = out[..., 2] = v
    out[..., 3] = 255
    return out


def tank_clear_icon() -> np.ndarray:
    """The top pane of the clear glass tank's item icon. M62: only a gloss and a sparkle: the pane itself is completely
    clear (no tint, no inner rim; the dark edge lines are the model's), with one short gloss stroke in one corner and a small
    white four-pointed sparkle (a star like U+2726) at the back corner, the top of the icon. The model draws it on the top
    face only (the brightest in the inventory; the placed block shows no glass at all). Before M62: a faint blue-green
    tint, a lighter rim and two highlights."""
    out = np.zeros((S, S, 4), np.uint8)
    # the gloss: a short diagonal stroke, brightest in its middle
    for i, (x, y) in enumerate(((2, 6), (3, 5), (4, 4), (5, 3), (6, 2))):
        out[y, x] = (0xff, 0xff, 0xff, (150, 215, 235, 215, 150)[i])
    # the sparkle: a white centre with four thin rays, the outer ends softer
    cx, cy = 2, 13
    out[cy, cx] = (0xff, 0xff, 0xff, 255)
    for k, a in ((1, 235), (2, 140)):
        for dx, dy in ((k, 0), (-k, 0), (0, k), (0, -k)):
            out[cy + dy, cx + dx] = (0xff, 0xff, 0xff, a)
    return out


def tank_frame() -> np.ndarray:
    """Item/inventory look: transparent centre, 1px dark grey border."""
    out = np.zeros((S, S, 4), np.uint8)
    border = np.zeros((S, S), bool)
    border[0, :] = border[-1, :] = border[:, 0] = border[:, -1] = True
    out[border] = (0x3a, 0x3a, 0x3a, 255)
    corners = [(0, 0), (0, S - 1), (S - 1, 0), (S - 1, S - 1)]
    for y, x in corners:
        out[y, x] = (0x2c, 0x2c, 0x2c, 255)
    return out


def _periodic_noise(rng, terms=6, max_f=2):
    """Tileable smooth field: sum of a few integer-frequency sines (seamless across tile edges)."""
    yy, xx = np.mgrid[0:S, 0:S] / S
    comps = []
    for _ in range(terms):
        fx, fy = rng.integers(-max_f, max_f + 1, 2)
        if fx == 0 and fy == 0:
            fx = 1
        ph = rng.uniform(0, 2 * math.pi)
        comps.append((fx, fy, ph, rng.uniform(0.5, 1.0)))
    return xx, yy, comps


def tank_water_still(alpha: int = 140) -> tuple[np.ndarray, dict]:
    """Calm greyscale water surface (tinted blue at runtime), 32-frame seamless loop.
    Slow broad swell + a few sparkle pixels fading in and out.
    The see-through amount is in the texture alpha (shader packs ignore the vertex alpha): 140 for the surface,
    90 for the side faces (`tank_water_side`, looked through at the fish)."""
    rng = _rng(202)
    xx, yy, comps = _periodic_noise(rng, terms=5)
    frames = []
    sparkles = [(int(rng.integers(0, S)), int(rng.integers(0, S)), float(rng.uniform(0, 1))) for _ in range(7)]
    for f in range(WATER_FRAMES):
        ph = 2 * math.pi * f / WATER_FRAMES
        field = np.zeros((S, S))
        norm = 0
        for i, (fx, fy, p0, amp) in enumerate(comps):
            # every component drifts by an integer number of cycles per loop -> seamless
            k = 1 if i % 2 == 0 else -1
            field += amp * np.sin(2 * math.pi * (fx * xx + fy * yy) + p0 + k * ph)
            norm += amp
        field /= norm  # -1..1
        v = 208 + 11 * field
        for sx, sy, sp in sparkles:
            # each sparkle is visible for ~1/4 of the loop
            phase = (f / WATER_FRAMES + sp) % 1.0
            b = max(0.0, math.sin(phase * 2 * math.pi)) ** 6
            v[sy, sx] += 30 * b
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                v[(sy + dy) % S, (sx + dx) % S] += 9 * b
        v = np.clip(np.rint(v), 190, 235).astype(np.uint8)
        fr = np.zeros((S, S, 4), np.uint8)
        fr[..., 0] = fr[..., 1] = fr[..., 2] = v
        fr[..., 3] = alpha
        frames.append(fr)
    strip = np.concatenate(frames, 0)
    mcmeta = {"animation": {"frametime": 3, "interpolate": True}}
    return strip, mcmeta


def interior_floor() -> np.ndarray:
    """Neutral light grey floor with a faint 1px grid border ("substrate goes here")."""
    n = _rng(303).integers(-4, 5, (S, S))
    v = np.clip(0xc6 + n, 0, 255)
    v[0, :] = v[-1, :] = v[:, 0] = v[:, -1] = 0xb0
    out = np.zeros((S, S, 4), np.uint8)
    out[..., 0] = out[..., 1] = out[..., 2] = v.astype(np.uint8)
    out[..., 3] = 255
    return out


def interior_wall() -> np.ndarray:
    """Faint translucent diagonal-hatch pattern (break particles of the invisible wall)."""
    out = np.zeros((S, S, 4), np.uint8)
    yy, xx = np.mgrid[0:S, 0:S]
    out[..., :3] = (205, 225, 235)
    out[..., 3] = 34
    out[((xx + yy) % 4) == 0, 3] = 78
    out[0, :, 3] = out[-1, :, 3] = out[:, 0, 3] = out[:, -1, 3] = 60
    return out


# ---------------------------------------------------------------- feeder (box 3..13 x 0..8 x 3..13)

def _metal(seed, base, amp=4):
    n = _rng(seed).integers(-amp, amp + 1, (S, S))
    out = np.zeros((S, S, 4), np.uint8)
    v = np.clip(base + n, 0, 255)
    out[..., 0] = out[..., 1] = v.astype(np.uint8)
    out[..., 2] = np.clip(v + 4, 0, 255).astype(np.uint8)
    out[..., 3] = 255
    return out


def _put(img, x, y, rgb):
    img[y, x, :3] = rgb
    img[y, x, 3] = 255


def feeder_side() -> np.ndarray:
    """Metal box; rows 8..15 (= model height 0..8) carry the design, rows 0..7 repeat it."""
    half = _metal(401, 0x86)[:8].copy()
    half[0, :, :3] = 0xb4  # light top bevel
    half[6, :, :3] = 0x66
    half[7, :, :3] = 0x4a  # dark bottom
    for x in (3, 12):  # edges of the visible part (u 3..13)
        half[1:7, x, :3] = 0x5a
    pel = [(0xc0, 0x70, 0x3e), (0x9a, 0x4f, 0x28), (0xe2, 0xa0, 0x70), (0x6e, 0x34, 0x18)]
    rng = _rng(402)
    for y in range(1, 6):  # window with pellets
        for x in range(5, 11):
            if y in (1, 5) or x in (5, 10):
                _put(half, x, y, (0x2c, 0x2c, 0x31))
            elif y == 2:
                _put(half, x, y, (0xb8, 0xd8, 0xe4))
            else:
                _put(half, x, y, pel[int(rng.integers(0, 4))])
    return np.concatenate([half, half], 0)


def feeder_top() -> np.ndarray:
    img = _metal(403, 0x9a)
    img[3:13, 3, :3] = 0xc4
    img[3, 3:13, :3] = 0xc4
    img[3:13, 12, :3] = 0x58
    img[12, 3:13, :3] = 0x58
    for y in range(5, 11):  # hatch
        for x in range(5, 11):
            edge = y in (5, 10) or x in (5, 10)
            _put(img, x, y, (0x3a, 0x3a, 0x40) if edge else (0x70, 0x70, 0x78))
    for x in (7, 8):  # handle
        _put(img, x, 7, (0xd8, 0xd8, 0xdc))
        _put(img, x, 8, (0x4a, 0x4a, 0x50))
    return img


def feeder_bottom() -> np.ndarray:
    img = _metal(404, 0x4c, 3)
    for x in range(5, 11):
        _put(img, x, 6, (0x2a, 0x2a, 0x2e))
        _put(img, x, 7, (0x12, 0x12, 0x14))
        _put(img, x, 8, (0x12, 0x12, 0x14))
    return img


def clip_feeder_front() -> np.ndarray:
    """M21.5: front of the clip-on feeder (model u 5..11, v 0..9): brushed metal, a small window with pellets, a dial
    with three marks (1-3 feedings) and the clip at the top."""
    img = _metal(405, 0x8e)
    img[0, :, :3] = 0xc0  # the clip over the glass rim
    img[1, :, :3] = 0x64
    for x in (5, 10):
        img[2:9, x, :3] = 0x5a
    pel = [(0xc0, 0x70, 0x3e), (0x9a, 0x4f, 0x28), (0xe2, 0xa0, 0x70), (0x6e, 0x34, 0x18)]
    rng = _rng(406)
    for y in range(2, 6):
        for x in range(6, 10):
            if y == 2 or y == 5 or x in (6, 9):
                _put(img, x, y, (0x2c, 0x2c, 0x31))
            elif y == 3:
                _put(img, x, y, (0xb8, 0xd8, 0xe4))
            else:
                _put(img, x, y, pel[int(rng.integers(0, 4))])
    for x in (6, 7, 9):  # the dial: three marks, the knob
        _put(img, x, 7, (0xe0, 0xe0, 0xe4))
    _put(img, 8, 7, (0x30, 0x30, 0x34))
    img[8, 5:11, :3] = 0x48
    return img


# ---------------------------------------------------------------- tank light (bar along X, 3 px wide, 2 px high)

def tank_light() -> np.ndarray:
    """Anthracite housing, brushed along X, lighter top edge."""
    rng = _rng(501)
    img = np.zeros((S, S, 4), np.uint8)
    row = rng.integers(-3, 4, S)
    for y in range(S):
        v = np.clip(0x34 + row[y] + rng.integers(-1, 2, S), 0, 255)
        img[y, :, 0] = v
        img[y, :, 1] = v
        img[y, :, 2] = np.clip(v + 3, 0, 255)
    img[..., 3] = 255
    img[0, :, :3] = 0x5c
    img[S - 1, :, :3] = 0x22
    return img


def _panel(on: bool) -> np.ndarray:
    """LED panel (bottom face of the light): bright cool white when on, dark grey when off."""
    img = np.zeros((S, S, 4), np.uint8)
    yy, xx = np.mgrid[0:S, 0:S]
    if on:
        base, dot, dim = (0xf4, 0xf7, 0xff), (0xff, 0xff, 0xff), (0xe4, 0xea, 0xf4)
    else:
        base, dot, dim = (0x4a, 0x4c, 0x52), (0x5e, 0x60, 0x66), (0x40, 0x42, 0x48)
    img[..., :3] = base
    img[(xx % 2 == 0) & (yy % 2 == 0), :3] = dot
    img[(xx % 2 == 1) & (yy % 2 == 1), :3] = dim
    img[..., 3] = 255
    return img


def all_blocks():
    water, meta = tank_water_still(140)
    side, _ = tank_water_still(90)
    return {
        "tank_edge": tank_edge(),
        "tank_frame": tank_frame(),
        "tank_clear_icon": tank_clear_icon(),  # M21.8
        "tank_water_still": water,
        "tank_water_side": side,
        "interior_floor": interior_floor(),
        "interior_wall": interior_wall(),
        "feeder_side": feeder_side(),
        "feeder_top": feeder_top(),
        "feeder_bottom": feeder_bottom(),
        "clip_feeder_front": clip_feeder_front(),  # M21.5
        "tank_light": tank_light(),
        "tank_light_panel_on": _panel(True),
        "tank_light_panel_off": _panel(False),
    }, {"tank_water_still": meta, "tank_water_side": meta}


def write_all() -> list:
    imgs, metas = all_blocks()
    out = [save_png(img, BLOCK_TEX / f"{k}.png") for k, img in imgs.items()]
    for k, m in metas.items():
        p = BLOCK_TEX / f"{k}.png.mcmeta"
        p.write_text(json.dumps(m, indent=2) + "\n", encoding="utf-8", newline="\n")
        out.append(p)
    return out


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
