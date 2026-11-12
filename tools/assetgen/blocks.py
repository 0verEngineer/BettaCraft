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


def tank_water_still() -> tuple[np.ndarray, dict]:
    """Calm greyscale water surface (tinted blue at runtime), 32-frame seamless loop.
    Slow broad swell + a few sparkle pixels fading in and out."""
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
        fr[..., 3] = 255
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


def all_blocks():
    water, meta = tank_water_still()
    return {
        "tank_edge": tank_edge(),
        "tank_frame": tank_frame(),
        "tank_water_still": water,
        "interior_floor": interior_floor(),
        "interior_wall": interior_wall(),
    }, {"tank_water_still": meta}


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
