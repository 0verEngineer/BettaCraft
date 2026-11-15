"""M29.1 assets for the 3D block items: the light cone under the lamps (tinted in the light colour by the item colour).

The items of the aquarium lights show the lamp with a soft beam of light below it, so the icon reads as a light and
not as a grey slab. Deterministic, no vanilla pixels.

Run from the repo root:  py tools/assetgen/block_items.py
"""
from __future__ import annotations

import numpy as np

from common import ITEM_TEX, save_png

S = 16


def light_cone():
    """White, opaque-ish at the top (the lamp) and fading out downwards; the edges a little fainter than the middle."""
    img = np.zeros((S, S, 4), np.uint8)
    for y in range(S):
        fall = (1.0 - y / (S - 1)) ** 1.4
        for x in range(S):
            edge = 1.0 - 0.35 * (abs(x - 7.5) / 7.5) ** 2
            a = 235 * fall * edge
            img[y, x] = (255, 246, 214, int(round(a)))
    return img


def write_all():
    return [save_png(light_cone(), ITEM_TEX / "light_cone.png")]


if __name__ == "__main__":
    for p in write_all():
        print(p)
