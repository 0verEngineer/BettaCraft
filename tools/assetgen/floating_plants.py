"""M12 assets: floating plants (duckweed, Amazon frogbit, salvinia, red root floater; M28: water lettuce, hornwort, Riccia).

Item icons (16x16, vanilla-like pixel art with a dark outline) and the textures the tank renderer draws on the water
surface (``textures/entity/plants``): leaf quads and root strips. Everything is procedural and deterministic (fixed
seeds). Alpha is binary (the renderer uses a cutout render type, which shader packs handle like leaves); the red root
floater leaf and the roots are greyscale and get tinted at runtime (green -> red with the light).

Run from the repo root:  py tools/assetgen/floating_plants.py
"""
from __future__ import annotations

import math

import numpy as np

from common import ITEM_TEX, TEX, save_png

PLANT_TEX = TEX / "entity" / "plants"
S = 16


def _rng(seed):
    return np.random.default_rng(seed)


def _rgba(c, a=255):
    h = c.lstrip("#")
    return np.array([int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a], np.uint8)


def _shade(c, f):
    """Lighter (f > 1) or darker (f < 1) version of an RGB colour."""
    return np.clip(np.array(c[:3], np.float64) * f, 0, 255)


def _outline(img, colour):
    """Dark 1 px outline around every opaque pixel (4-neighbourhood), on transparent pixels only."""
    a = img[..., 3] > 0
    n = np.zeros_like(a)
    n[1:, :] |= a[:-1, :]
    n[:-1, :] |= a[1:, :]
    n[:, 1:] |= a[:, :-1]
    n[:, :-1] |= a[:, 1:]
    edge = n & ~a
    img[edge] = colour
    return img


def _ellipse(img, cx, cy, rx, ry, ang, col_fn):
    """Fills a rotated ellipse; col_fn(u, v) gives the RGB for local coordinates u, v in -1..1."""
    h, w = img.shape[:2]
    ca, sa = math.cos(ang), math.sin(ang)
    for y in range(h):
        for x in range(w):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            u = (dx * ca + dy * sa) / rx
            v = (-dx * sa + dy * ca) / ry
            if u * u + v * v <= 1.0:
                img[y, x, :3] = col_fn(u, v)
                img[y, x, 3] = 255


# ---------------------------------------------------------------- textures on the water

def duckweed_texture(n=32):
    """A small patch: ~16 tiny oval fronds of varying green, some in pairs, on a transparent disc."""
    img = np.zeros((n, n, 4), np.uint8)
    rng = _rng(1201)
    base = [(0x7c, 0xc0, 0x3a), (0x8e, 0xcf, 0x44), (0x6c, 0xb0, 0x34), (0x9a, 0xd4, 0x52)]
    placed = []
    tries = 0
    while len(placed) < 17 and tries < 600:
        tries += 1
        r = (n / 2 - 4) * math.sqrt(rng.random())
        a = rng.random() * math.tau
        cx, cy = n / 2 + r * math.cos(a), n / 2 + r * math.sin(a)
        if any(math.hypot(cx - px, cy - py) < 4.2 for px, py in placed):
            continue
        placed.append((cx, cy))
        col = base[int(rng.integers(len(base)))]
        ang = rng.random() * math.pi
        rx, ry = 2.6 + rng.random() * 0.8, 1.8 + rng.random() * 0.5

        def fn(u, v, col=col):
            lit = 1.15 - 0.25 * (u + v) * 0.5
            return _shade(col, lit if u * u + v * v < 0.5 else lit * 0.88)

        _ellipse(img, cx, cy, rx, ry, ang, fn)
    # tiny bright spot (the glossy top) on some fronds
    for cx, cy in placed[::2]:
        x, y = int(cx - 0.5), int(cy - 0.8)
        if 0 <= x < n and 0 <= y < n and img[y, x, 3]:
            img[y, x, :3] = (0xc8, 0xec, 0x86)
    return _outline(img, (0x2e, 0x55, 0x1c, 255))


def frogbit_leaf_texture(n=32):
    """Round, glossy leaf with a notch at the stem (bottom of the image, it points to the plant's centre)."""
    img = np.zeros((n, n, 4), np.uint8)
    c = n / 2
    rad = n / 2 - 1.5
    for y in range(n):
        for x in range(n):
            dx, dy = x + 0.5 - c, y + 0.5 - c
            d = math.hypot(dx, dy)
            if d > rad:
                continue
            # notch: a narrow wedge from the centre towards the bottom
            if dy > 2 and abs(dx) < (dy - 2) * 0.28:
                continue
            t = d / rad
            g = np.array([0x4a, 0x9a, 0x32], np.float64) * (1.12 - 0.3 * t)
            # spongy darker middle zone (frogbit leaves have an aerenchyma pad underneath that shows through)
            if t < 0.45:
                g = g * 0.92 + np.array([10, 14, 4])
            # veins from the stem
            ang = math.atan2(dx, dy)
            if t > 0.15 and abs(math.sin(ang * 4.0)) < 0.09:
                g = g * 0.8
            if abs(dx) < 0.6 and dy < 0:
                g = g * 0.78  # midrib
            # gloss
            if math.hypot(dx + rad * 0.38, dy + rad * 0.42) < rad * 0.16:
                g = g * 0.72 + np.array([0xc8, 0xe8, 0xa0]) * 0.28
            img[y, x, :3] = np.clip(g, 0, 255)
            img[y, x, 3] = 255
    # rim
    a = img[..., 3] > 0
    for y in range(n):
        for x in range(n):
            if not a[y, x]:
                continue
            if any(not (0 <= x + dx < n and 0 <= y + dy < n) or not a[y + dy, x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                img[y, x, :3] = (0x2c, 0x5c, 0x22)
    return img


def salvinia_texture(n=32):
    """Two pairs of oval, hairy leaves on a short stem (the stem runs along the image's vertical)."""
    img = np.zeros((n, n, 4), np.uint8)
    rng = _rng(1203)
    base = (0x6c, 0x98, 0x38)

    def leaf(u, v):
        g = _shade(base, 1.12 - 0.18 * (u + 1) * 0.5)
        # the leaves are folded a little: darker crease along the middle
        if abs(v) < 0.12:
            g = g * 0.82
        return g

    for cy, size in ((10.0, 1.0), (21.5, 0.85)):
        for side in (-1, 1):
            _ellipse(img, n / 2 + side * 6.6 * size, cy, 6.4 * size, 4.6 * size, 0.0, leaf)
    # stem
    for y in range(6, 27):
        img[y, n // 2, :] = (0x5a, 0x6e, 0x2c, 255)
    # white hairs (the water-repellent "egg beater" hairs show as light speckles)
    a = img[..., 3] > 0
    ys, xs = np.nonzero(a)
    for i in rng.choice(len(xs), size=min(70, len(xs)), replace=False):
        img[ys[i], xs[i], :3] = (0xd6, 0xe6, 0xb4)
    return _outline(img, (0x34, 0x4c, 0x1a, 255))


def red_root_floater_leaf_texture(n=16):
    """Greyscale round leaf (tinted green -> red at runtime), small notch at the stem, darker veins."""
    img = np.zeros((n, n, 4), np.uint8)
    c = n / 2
    rad = n / 2 - 0.8
    for y in range(n):
        for x in range(n):
            dx, dy = x + 0.5 - c, y + 0.5 - c
            d = math.hypot(dx, dy)
            if d > rad:
                continue
            if dy > 1.5 and abs(dx) < (dy - 1.5) * 0.25:
                continue
            t = d / rad
            v = 250 - 45 * t
            if abs(dx) < 0.55 and dy < 0:
                v -= 40
            if t > 0.88:
                v = 150
            if math.hypot(dx + rad * 0.35, dy + rad * 0.35) < rad * 0.2:
                v = 255
            img[y, x] = (v, v, v, 255)
    return img


def roots_texture(w=16, h=32):
    """Thin, slightly wavy strands hanging down from the top with fine side hairs (greyscale, tinted per species)."""
    img = np.zeros((h, w, 4), np.uint8)
    rng = _rng(1205)
    xs = [2.5, 5.2, 7.8, 10.3, 13.2]
    for i, x0 in enumerate(xs):
        length = int(h * (0.6 + 0.4 * rng.random()))
        phase = rng.random() * math.tau
        for y in range(length):
            x = int(round(x0 + 0.9 * math.sin(y * 0.35 + phase)))
            if 0 <= x < w:
                v = int(235 - 70 * y / h)
                img[y, x] = (v, v, v, 255)
                # fine side hairs
                if y > 3 and rng.random() < 0.18:
                    xx = x + (1 if rng.random() < 0.5 else -1)
                    if 0 <= xx < w:
                        img[y, xx] = (v - 30, v - 30, v - 30, 255)
    return img


# ---------------------------------------------------------------- M28: water lettuce, hornwort, floating Riccia

def water_lettuce_leaf_texture(n=16):
    """One spatulate, velvety water lettuce leaf (stem end at the bottom, v = 1): pale green with parallel ribs."""
    img = np.zeros((n, n, 4), np.uint8)
    rng = _rng(2831)
    for y in range(n):
        t = 1 - (y + 0.5) / n                # 0 at the stem end .. 1 at the tip
        half = (0.18 + 0.32 * min(1.0, t * 1.6) ** 0.7) * n * (1.0 if t < 0.85 else 1 - (t - 0.85) * 2.2)
        for x in range(n):
            dx = x + 0.5 - n / 2
            if abs(dx) > half:
                continue
            g = np.array([0x8c, 0xbc, 0x5a], np.float64) * (1.08 - 0.12 * abs(dx) / max(half, 1))
            if int(abs(dx) + 0.5) % 3 == 0 and t > 0.15:
                g = g * 0.86                 # the ribs running out to the tip
            g = g + rng.normal(0, 4)
            img[y, x, :3] = np.clip(g, 0, 255)
            img[y, x, 3] = 255
    return img


def hornwort_texture(n=16):
    """Hornwort seen from above: a few shoots with whorls of stiff, forked dark leaves (tinted at runtime: greyscale)."""
    img = np.zeros((n, n, 4), np.uint8)
    rng = _rng(2832)
    for k in range(3):
        ang = k * math.tau / 3 + rng.random() * 0.6
        for i in range(2, 8):
            cx, cy = n / 2 + math.cos(ang) * i, n / 2 + math.sin(ang) * i
            for j in range(6):
                a = j * math.tau / 6 + i
                for d in (1, 2):
                    x, y = int(cx + math.cos(a) * d), int(cy + math.sin(a) * d)
                    if 0 <= x < n and 0 <= y < n:
                        img[y, x] = (200 - 20 * d, 200 - 20 * d, 200 - 20 * d, 255)
            x, y = int(cx), int(cy)
            img[y, x] = (150, 150, 150, 255)
    return img


def hornwort_shoots_texture(w=16, h=64):
    """Hornwort shoots hanging down into the water: stems with whorls of needle leaves (greyscale, tinted).
    M64: two loose shoots per strip with whorls every 4 pixels and shorter needles (it looked like a dense curtain)."""
    img = np.zeros((h, w, 4), np.uint8)
    rng = _rng(2833)
    for x0 in (4.5, 11.5):
        length = int(h * (0.65 + 0.3 * rng.random()))
        phase = rng.random() * math.tau
        for y in range(length):
            x = x0 + 0.8 * math.sin(y * 0.12 + phase)
            ix = int(round(x))
            if 0 <= ix < w:
                img[y, ix] = (150, 150, 150, 255)
            if y % 4 == 0:
                for side in (-1, 1):
                    for d in range(1, 3 if (y // 4) % 2 else 4):
                        xx = int(round(x + side * d))
                        yy = y + d // 2
                        if 0 <= xx < w and yy < h:
                            v = 210 - 15 * d
                            img[yy, xx] = (v, v, v, 255)
    return img


def riccia_texture(n=16):
    """Floating Riccia: a dense, bright green cushion of forked thalli."""
    img = np.zeros((n, n, 4), np.uint8)
    rng = _rng(2834)
    base = (0x86, 0xc8, 0x46)

    def thallus(x, y, ang, ln, depth):
        for i in range(ln):
            ix, iy = int(round(x)), int(round(y))
            if 0 <= ix < n and 0 <= iy < n:
                img[iy, ix, :3] = np.clip(_shade(base, 0.9 + 0.25 * i / max(1, ln)), 0, 255)
                img[iy, ix, 3] = 255
            x += math.cos(ang)
            y += math.sin(ang)
        if depth < 2:
            thallus(x, y, ang + 0.55, max(2, ln - 1), depth + 1)
            thallus(x, y, ang - 0.55, max(2, ln - 1), depth + 1)

    for k in range(7):
        a = k * math.tau / 7 + rng.random() * 0.4
        thallus(n / 2 + math.cos(a), n / 2 + math.sin(a), a, 3, 0)
    return _outline(img, (0x4a, 0x7a, 0x26, 255))


# ---------------------------------------------------------------- item icons

def _icon_base():
    return np.zeros((S, S, 4), np.uint8)


def duckweed_icon():
    img = _icon_base()
    fronds = [(4.5, 6.0, 0.3), (8.0, 4.5, 1.2), (11.5, 6.5, 0.6), (6.0, 9.5, 2.0), (10.0, 10.0, 0.9), (13.0, 10.5, 1.6),
              (3.5, 11.5, 0.2)]
    cols = [(0x7c, 0xc0, 0x3a), (0x92, 0xd2, 0x48), (0x6a, 0xae, 0x32)]
    for i, (cx, cy, ang) in enumerate(fronds):
        col = cols[i % len(cols)]
        _ellipse(img, cx, cy, 2.3, 1.6, ang, lambda u, v, col=col: _shade(col, 1.2 if (u + v) < -0.6 else 1.0))
    # three short root threads
    for x, y0 in ((5, 11), (10, 12), (13, 12)):
        for y in range(y0 + 1, min(S - 1, y0 + 4)):
            if img[y, x, 3] == 0:
                img[y, x] = (0xd8, 0xd8, 0xc8, 255)
    return _outline(img, (0x2a, 0x4e, 0x1a, 255))


def frogbit_icon():
    img = _icon_base()
    c, rad = 8.0, 5.6
    for y in range(S):
        for x in range(S):
            dx, dy = x + 0.5 - c, y + 0.5 - 6.8
            d = math.hypot(dx, dy)
            if d > rad or (dy > 1 and abs(dx) < (dy - 1) * 0.35):
                continue
            g = np.array([0x4e, 0xa0, 0x34], np.float64) * (1.15 - 0.3 * d / rad)
            if math.hypot(dx + 2.2, dy + 2.2) < 1.4:
                g = np.array([0xc8, 0xec, 0xa0], np.float64)
            img[y, x, :3] = np.clip(g, 0, 255)
            img[y, x, 3] = 255
    img = _outline(img, (0x24, 0x48, 0x18, 255))
    # roots below the leaf (thin, no outline)
    for x0, length in ((6, 3), (8, 4), (10, 3)):
        for k in range(length):
            y = 13 + k
            if y < S and img[y, x0, 3] == 0:
                img[y, x0] = (0xe8, 0xe4, 0xd4, 255) if k < length - 1 else (0xb8, 0xb4, 0xa4, 255)
    return img


def salvinia_icon():
    img = _icon_base()
    base = (0x82, 0xac, 0x40)
    for cy, sz in ((5.5, 1.0), (10.5, 0.85)):
        for side in (-1, 1):
            _ellipse(img, 8 + side * 3.2 * sz, cy, 3.1 * sz, 2.2 * sz, 0.0,
                     lambda u, v: _shade(base, 1.18 if u + v < -0.5 else (0.85 if abs(v) < 0.18 else 1.0)))
    for y in range(3, 14):
        img[y, 7] = (0x5a, 0x6e, 0x2c, 255)
    # light hair speckles
    for x, y in ((4, 5), (6, 4), (10, 5), (12, 6), (5, 10), (11, 11), (9, 4)):
        if img[y, x, 3]:
            img[y, x, :3] = (0xe0, 0xec, 0xc0)
    # brown filaments below
    for x in (6, 9):
        for y in range(13, 16):
            if img[y, x, 3] == 0:
                img[y, x] = (0x7a, 0x61, 0x42, 255)
    return _outline(img, (0x30, 0x46, 0x18, 255))


def red_root_floater_icon():
    img = _icon_base()
    leaves = [(5.5, 6.0, (0xb8, 0x4a, 0x3a)), (10.5, 5.5, (0x9c, 0x6a, 0x38)), (8.0, 9.5, (0x6e, 0xa8, 0x44))]
    for cx, cy, col in leaves:
        _ellipse(img, cx, cy, 2.9, 2.9, 0.0, lambda u, v, col=col: _shade(col, 1.25 if u + v < -0.7 else 1.0))
    img = _outline(img, (0x3a, 0x22, 0x18, 255))
    for x0, length in ((6, 3), (8, 4), (10, 3)):
        for k in range(length):
            y = 13 + k
            if y < S and img[y, x0, 3] == 0:
                img[y, x0] = (0xc0, 0x48, 0x38, 255)
    return img


def water_lettuce_icon():
    img = _icon_base()
    col = (0x8c, 0xc2, 0x5c)
    for k, (ang, ln) in enumerate(((math.pi * 0.5, 5.8), (math.pi * 0.85, 5.2), (math.pi * 0.15, 5.2), (math.pi * 1.15, 4.4), (-math.pi * 0.15, 4.4))):
        cx, cy = 8 + math.cos(ang) * ln * 0.55, 8.5 - math.sin(ang) * ln * 0.55
        _ellipse(img, cx, cy, ln * 0.55, 2.2, -ang, lambda u, v, k=k: _shade(col, (1.15 if u > 0.3 else 1.0) * (0.92 if k % 2 else 1.0)))
    img = _outline(img, (0x34, 0x58, 0x1e, 255))
    for x0, length in ((6, 3), (8, 4), (10, 3)):
        for k in range(length):
            y = 12 + k
            if y < S and img[y, x0, 3] == 0:
                img[y, x0] = (0xe0, 0xe4, 0xd4, 255)
    return img


def hornwort_icon():
    img = _icon_base()
    stem = (0x3e, 0x6e, 0x2a)
    leaf = (0x56, 0x92, 0x38)
    for y in range(1, 15):
        x = 8 + int(round(math.sin(y * 0.45) * 1.2))
        img[y, x] = (*stem, 255)
        if y % 3 == 1:
            for side in (-1, 1):
                for d in range(1, 4):
                    xx, yy = x + side * d, y + (1 if d == 3 else 0)
                    if 0 <= xx < S and yy < S:
                        img[yy, xx] = (*leaf, 255)
    return _outline(img, (0x22, 0x3c, 0x16, 255))


TEXTURES = {
    "duckweed": duckweed_texture,
    "frogbit_leaf": frogbit_leaf_texture,
    "salvinia": salvinia_texture,
    "red_root_floater_leaf": red_root_floater_leaf_texture,
    "roots": roots_texture,
    "water_lettuce_leaf": water_lettuce_leaf_texture,
    "hornwort": hornwort_texture,
    "hornwort_shoots": hornwort_shoots_texture,
    "riccia": riccia_texture,
}

ICONS = {
    "duckweed": duckweed_icon,
    "amazon_frogbit": frogbit_icon,
    "salvinia": salvinia_icon,
    "red_root_floater": red_root_floater_icon,
    "water_lettuce": water_lettuce_icon,
    "hornwort": hornwort_icon,
}


def write_all() -> list:
    out = []
    for name, f in TEXTURES.items():
        out.append(save_png(f(), PLANT_TEX / f"{name}.png"))
    for name, f in ICONS.items():
        out.append(save_png(f(), ITEM_TEX / f"{name}.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
