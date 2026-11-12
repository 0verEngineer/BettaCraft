"""M4 equipment textures: external filter canister, cabinet placeholder wood and trim, internal filter outlet,
filter hose and the animated-by-scrolling water jet ribbon. Deterministic (fixed seeds), no vanilla pixels."""
from __future__ import annotations

import math

import numpy as np

from common import BLOCK_TEX, TEX, save_png

S = 16
ENTITY_FILTER = TEX / "entity" / "filter"


def _rng(seed):
    return np.random.default_rng(seed)


def _rgba(rgb, a=255):
    return np.array([*rgb, a], np.uint8)


def _plastic(seed, base, amp=5):
    """Matte dark plastic with fine noise."""
    n = _rng(seed).integers(-amp, amp + 1, (S, S))
    out = np.zeros((S, S, 4), np.uint8)
    for c in range(3):
        out[..., c] = np.clip(base[c] + n, 0, 255)
    out[..., 3] = 255
    return out


# ---------------------------------------------------------------- external filter (canister 3..13 x 0..12, lid 4..12 x 12..14)

def external_filter_side() -> np.ndarray:
    """Canister: rows 4..15 = model height 0..12 (body), rows 2..3 = lid. Blue-grey body with a viewing strip."""
    img = _plastic(501, (0x3c, 0x44, 0x4e))
    img[0:4] = _plastic(502, (0x24, 0x28, 0x2e))[0:4]  # lid rows (v 2..4 used by the lid element)
    img[4, :, :3] = 0x5e  # rim under the lid
    img[15, :, :3] = 0x1e  # foot
    img[14, :, :3] = 0x2a
    # vertical ribs
    for x in (4, 11):
        img[5:14, x, :3] = (0x30, 0x36, 0x3e)
    # window showing filter media (sponge layers) and water
    for y in range(6, 13):
        for x in range(6, 10):
            if y in (6, 12) or x in (6, 9):
                img[y, x, :3] = (0x22, 0x26, 0x2c)
            else:
                layer = (y - 7) // 2
                img[y, x, :3] = [(0x3a, 0x7a, 0xa8), (0x2c, 0x4a, 0x7a), (0xb0, 0x8a, 0x3a)][min(layer, 2)]
    return img


def external_filter_top() -> np.ndarray:
    """Lid seen from above (4..12): two hose ports and a primer button."""
    img = _plastic(503, (0x26, 0x2a, 0x30))
    img[4:12, 4, :3] = 0x40
    img[4, 4:12, :3] = 0x40
    img[4:12, 11, :3] = 0x18
    img[11, 4:12, :3] = 0x18
    for cx, cy in ((6, 6), (9, 9)):  # hose ports
        img[cy - 1:cy + 1, cx - 1:cx + 1, :3] = (0x0c, 0x0e, 0x10)
    img[8:10, 5:7, :3] = (0x9a, 0x30, 0x2a)  # button
    return img


def external_filter_bottom() -> np.ndarray:
    return _plastic(504, (0x1c, 0x1f, 0x24), 3)


# ---------------------------------------------------------------- cabinet

def cabinet_wood() -> np.ndarray:
    """Placeholder planks (neutral light wood). Replaced at runtime by the planks the cabinet is made of; only shown
    if the planks have no usable texture."""
    rng = _rng(601)
    out = np.zeros((S, S, 4), np.uint8)
    base = np.array([0xb0, 0x8c, 0x5c], np.int32)
    for y in range(S):
        board = y // 4
        shade = [0, -10, 6, -4][board]
        grain = rng.integers(-6, 7, S)
        for x in range(S):
            v = base + shade + grain[x] + int(4 * math.sin((x + board * 5) * 0.7))
            out[y, x, :3] = np.clip(v, 0, 255)
        if y % 4 == 3:
            out[y, :, :3] = np.clip(base - 40, 0, 255)
    out[..., 3] = 255
    return out


def cabinet_trim() -> np.ndarray:
    """Dark metal for handles and the plinth."""
    img = _plastic(602, (0x2a, 0x2a, 0x2e), 3)
    img[0, :, :3] = 0x4a
    img[15, :, :3] = 0x18
    return img


# ---------------------------------------------------------------- internal filter outlet (face of the top segment)

def filter_segment_outlet() -> np.ndarray:
    n = _rng(611).random((S, S))
    img = np.zeros((S, S, 4), np.uint8)
    img[..., :3] = np.clip((0.22 + 0.05 * n[..., None]) * 255, 0, 255).astype(np.uint8)
    img[..., 3] = 255
    img[4:12, 3:13, :3] = (0x10, 0x12, 0x14)  # outlet opening
    img[5:11, 4:12, :3] = (0x2a, 0x50, 0x6c)
    for x in range(5, 12, 2):  # spray bar holes
        img[7:9, x, :3] = (0x80, 0xc0, 0xe0)
    img[:, 0, :3] = 0x4c
    img[:, 15, :3] = 0x24
    return img


# ---------------------------------------------------------------- hose and jet (entity textures, repeat-wrapped)

def hose() -> np.ndarray:
    """Ribbed black hose; u repeats along the hose (8 ribs per block)."""
    img = np.zeros((S, S, 4), np.uint8)
    for x in range(S):
        v = 0x30 if (x // 2) % 2 == 0 else 0x22
        img[:, x, :3] = v
    img[0:2, :, :3] += 0x10
    img[14:16, :, :3] = 0x16
    img[..., 3] = 255
    return img


def jet() -> np.ndarray:
    """Water ribbon, 16 x 64: bright core, soft edges and streaks along v (seamless vertically, so scrolling v
    animates the flow). Greyish white; the renderer tints it light blue."""
    w, h = 16, 64
    rng = _rng(621)
    out = np.zeros((h, w, 4), np.float64)
    xs = (np.arange(w) + 0.5) / w
    edge = np.clip(1 - np.abs(xs - 0.5) * 2, 0, 1) ** 0.6  # 1 in the middle, 0 at the edges
    streaks = []
    for _ in range(9):
        streaks.append((rng.uniform(0.1, 0.9), rng.integers(1, 4), rng.uniform(0, 2 * math.pi), rng.uniform(0.25, 0.6)))
    for y in range(h):
        t = y / h
        a = 0.35 + 0.15 * np.sin(2 * math.pi * (t * 2) + xs * 3)
        bright = np.full(w, 0.82)
        for cx, freq, ph, amp in streaks:
            band = np.exp(-((xs - cx) / 0.06) ** 2)
            pulse = 0.5 + 0.5 * math.sin(2 * math.pi * freq * t + ph)
            a = a + amp * band * pulse
            bright = bright + 0.18 * band * pulse
        out[y, :, 0] = np.clip(bright, 0, 1)
        out[y, :, 1] = np.clip(bright, 0, 1)
        out[y, :, 2] = np.clip(bright + 0.05, 0, 1)
        out[y, :, 3] = np.clip(a * edge, 0, 0.95)
    return out


def write_all() -> list:
    out = []
    for name, f in {"external_filter_side": external_filter_side, "external_filter_top": external_filter_top,
                    "external_filter_bottom": external_filter_bottom, "cabinet_wood": cabinet_wood,
                    "cabinet_trim": cabinet_trim, "filter_segment_outlet": filter_segment_outlet}.items():
        out.append(save_png(f(), BLOCK_TEX / f"{name}.png"))
    out.append(save_png(hose(), ENTITY_FILTER / "hose.png"))
    out.append(save_png(jet(), ENTITY_FILTER / "jet.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
