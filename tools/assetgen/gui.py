"""GUI textures in the vanilla container style (256x256 sheets, transparent outside the panels).

The layout constants below are mirrored in the Java TankScreen; keep both in sync.
"""
from __future__ import annotations

import numpy as np

from common import GUI_TEX, save_png

W = H = 256
CLEAR = (0, 0, 0, 0)
FILL = (0xC6, 0xC6, 0xC6, 255)
WHITE = (0xFF, 0xFF, 0xFF, 255)
DARK = (0x55, 0x55, 0x55, 255)
BLACK = (0, 0, 0, 255)
SLOT_FILL = (0x8B, 0x8B, 0x8B, 255)
SLOT_DARK = (0x37, 0x37, 0x37, 255)

PANEL = (0, 0, 248, 230)  # x, y, w, h
CONTENT = (7, 17, 234, 123)  # inset box x 7..240, y 17..139
EQUIP_SLOTS = [(12, 148 + 18 * i) for i in range(4)]  # item top-left positions
INV_X, INV_Y, HOTBAR_Y = 44, 148, 206
ICON_HEATER = (0, 232)  # 16x16
ICON_LOCK = (16, 232)  # 10x10
ICON_UNLOCK = (26, 232)  # 10x10


def panel(img, x, y, w, h):
    """Vanilla inventory panel: 1px black outline with cut corners, 2px white top-left and
    dark grey bottom-right bevel, light grey fill."""
    x1, y1 = x + w - 1, y + h - 1
    img[y:y1 + 1, x:x1 + 1] = FILL
    # bevels
    img[y + 1:y + 3, x + 1:x1] = WHITE
    img[y + 1:y1, x + 1:x + 3] = WHITE
    img[y1 - 2:y1, x + 1:x1] = DARK
    img[y + 1:y1, x1 - 2:x1] = DARK
    # mixed corners (vanilla keeps fill colour where the two bevels meet)
    img[y + 1:y + 3, x1 - 2:x1] = FILL
    img[y1 - 2:y1, x + 1:x + 3] = FILL
    img[y + 1, x1 - 2] = WHITE
    img[y1 - 1, x + 2] = DARK
    # outline
    img[y, x + 2:x1 - 1] = BLACK
    img[y1, x + 2:x1 - 1] = BLACK
    img[y + 2:y1 - 1, x] = BLACK
    img[y + 2:y1 - 1, x1] = BLACK
    for cx, cy in ((x + 1, y + 1), (x1 - 1, y + 1), (x + 1, y1 - 1), (x1 - 1, y1 - 1)):
        img[cy, cx] = BLACK
    # transparent outside the rounded corners
    for cx, cy in ((x, y), (x + 1, y), (x, y + 1), (x1, y), (x1 - 1, y), (x1, y + 1),
                   (x, y1), (x + 1, y1), (x, y1 - 1), (x1, y1), (x1 - 1, y1), (x1, y1 - 1)):
        img[cy, cx] = CLEAR


def inset(img, x, y, w, h, fill=SLOT_FILL):
    """Slot-like inset: dark top-left edge, white bottom-right edge (vanilla slot style)."""
    x1, y1 = x + w - 1, y + h - 1
    img[y:y1 + 1, x:x1 + 1] = fill
    img[y, x:x1] = SLOT_DARK
    img[y:y1, x] = SLOT_DARK
    img[y1, x + 1:x1 + 1] = WHITE
    img[y + 1:y1 + 1, x1] = WHITE
    img[y, x1] = FILL
    img[y1, x] = FILL


def slot(img, ix, iy):
    inset(img, ix - 1, iy - 1, 18, 18)


def heater_ghost() -> np.ndarray:
    """Semi-transparent heater rod silhouette (empty equipment slot hint)."""
    out = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            a, k = x - y, x + y - 15
            if abs(k) <= 1 and -10 <= a <= 12:
                out[y, x] = (0x37, 0x37, 0x37, 0xA0 if a > 6 else 0x70)
    return out


def padlock(open_: bool) -> np.ndarray:
    out = np.zeros((10, 10, 4), np.uint8)
    k = (0x2A, 0x2A, 0x2E, 255)
    body = (0x6A, 0x6A, 0x72, 255) if not open_ else (0x8A, 0x86, 0x5A, 255)
    hi = (0xA8, 0xA8, 0xB0, 255) if not open_ else (0xC4, 0xBE, 0x84, 255)
    # shackle: closed = both legs into the body; open = lifted and swung right
    if not open_:
        out[0, 3:7] = k
        out[1:4, 2] = k
        out[1:4, 7] = k
    else:
        out[0, 5:9] = k
        out[1:4, 8] = k
        out[1:2, 4] = k
    out[4:10, 1:9] = k
    out[5:9, 2:8] = body
    out[5, 2:8] = hi
    out[6:8, 4:6] = k  # keyhole
    return out


def tank_gui() -> np.ndarray:
    img = np.zeros((H, W, 4), np.uint8)
    panel(img, *PANEL)
    inset(img, *CONTENT)
    for ix, iy in EQUIP_SLOTS:
        slot(img, ix, iy)
    for r in range(3):
        for c in range(9):
            slot(img, INV_X + 18 * c, INV_Y + 18 * r)
    for c in range(9):
        slot(img, INV_X + 18 * c, HOTBAR_Y)
    x, y = ICON_HEATER
    img[y:y + 16, x:x + 16] = heater_ghost()
    x, y = ICON_LOCK
    img[y:y + 10, x:x + 10] = padlock(False)
    x, y = ICON_UNLOCK
    img[y:y + 10, x:x + 10] = padlock(True)
    return img


def write_all() -> list:
    return [save_png(tank_gui(), GUI_TEX / "tank.png")]


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
