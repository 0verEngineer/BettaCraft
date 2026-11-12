"""M9: textures of the live food breeders (larva tub, artemia hatchery) and the tank lid (16x16, deterministic)."""
from __future__ import annotations

import numpy as np

from common import BLOCK_TEX, save_png

S = 16


def _rng(seed):
    return np.random.default_rng(seed)


def larva_tub_side() -> np.ndarray:
    """Wooden staves (vertical) with two dark iron hoops (rows 2..3 and 11..12)."""
    r = _rng(301)
    img = np.zeros((S, S, 4), np.uint8)
    base = np.array([138, 102, 60])
    for x in range(S):
        stave = x // 4
        shade = [0, 10, -6, 6][stave % 4]
        for y in range(S):
            v = base + shade + r.integers(-6, 7)
            if x % 4 == 0:
                v = v - 28  # gap between staves
            img[y, x, :3] = np.clip(v, 0, 255)
    for y0 in (2, 11):
        img[y0:y0 + 2, :, :3] = np.array([58, 58, 64]) + r.integers(-4, 5, (2, S, 1))
        img[y0, :, :3] = np.clip(img[y0, :, :3].astype(int) + 22, 0, 255)
    img[..., 3] = 255
    return img


def larva_tub_bottom() -> np.ndarray:
    """Planks of the tub floor."""
    r = _rng(302)
    img = np.zeros((S, S, 4), np.uint8)
    for y in range(S):
        for x in range(S):
            v = np.array([120, 88, 52]) + r.integers(-6, 7)
            if y % 5 == 0:
                v = v - 26
            img[y, x, :3] = np.clip(v, 0, 255)
    img[..., 3] = 255
    return img


def hatchery_base() -> np.ndarray:
    """Grey base of the hatchery with an air pump grille and a green LED."""
    r = _rng(303)
    img = np.zeros((S, S, 4), np.uint8)
    img[..., :3] = (np.array([96, 100, 106]) + r.integers(-5, 6, (S, S, 1))).clip(0, 255)
    for x in range(4, 12, 2):
        img[9:13, x, :3] = 52
    img[10:12, 13:15, :3] = np.array([60, 220, 90])
    img[0, :, :3] = 140
    img[S - 1, :, :3] = 60
    img[..., 3] = 255
    return img


def hatchery_glass() -> np.ndarray:
    """Translucent, slightly blue glass with a dark frame line and a highlight (the walls of the hatchery)."""
    img = np.zeros((S, S, 4), np.uint8)
    img[..., :3] = (190, 226, 238)
    img[..., 3] = 70
    img[0, :] = img[S - 1, :] = img[:, 0] = img[:, S - 1] = (70, 92, 104, 230)
    for i in range(3, 8):
        img[i, i - 1] = (255, 255, 255, 150)
    return img


def _specks(seed: int, colours: list[tuple[int, int, int]], count: int, wriggle: bool) -> np.ndarray:
    """Transparent overlay with small animals floating at the water surface."""
    r = _rng(seed)
    img = np.zeros((S, S, 4), np.uint8)
    for _ in range(count):
        x, y = int(r.integers(1, S - 2)), int(r.integers(1, S - 2))
        c = colours[int(r.integers(0, len(colours)))]
        img[y, x] = (*c, 255)
        if wriggle:
            dx, dy = [(1, 0), (0, 1), (1, 1), (-1, 1)][int(r.integers(0, 4))]
            img[y + dy, x + dx] = (*c, 235)
    return img


def culture_larvae() -> np.ndarray:
    return _specks(304, [(62, 40, 30), (88, 56, 38), (40, 30, 24)], 16, True)


def culture_nauplii() -> np.ndarray:
    return _specks(305, [(236, 128, 52), (250, 160, 70), (210, 96, 40)], 26, False)


TEXTURES = {
    "larva_tub_side": larva_tub_side,
    "larva_tub_bottom": larva_tub_bottom,
    "artemia_hatchery_base": hatchery_base,
    "artemia_hatchery_glass": hatchery_glass,
    "culture_larvae": culture_larvae,
    "culture_nauplii": culture_nauplii,
}


def write_all() -> list:
    return [save_png(f(), BLOCK_TEX / f"{name}.png") for name, f in TEXTURES.items()]


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
