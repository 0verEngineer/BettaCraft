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
CONTENT_FILL = (0x2E, 0x33, 0x39, 255)  # dark content area: light text stays readable (M2)

PANEL = (0, 0, 248, 230)  # x, y, w, h
CONTENT = (7, 17, 234, 123)  # inset box x 7..240, y 17..139
# M1-M3 heater slots: since M4 only drawn (from SLOT_FRAME) while an old heater is still in them
EQUIP_SLOTS = [(12, 148 + 18 * i) for i in range(4)]  # item top-left positions
INV_X, INV_Y, HOTBAR_Y = 44, 148, 206
ICON_HEATER = (0, 232)  # 16x16
ICON_LOCK = (16, 232)  # 10x10
ICON_UNLOCK = (26, 232)  # 10x10
SLOT_FRAME = (36, 232)  # 18x18 slot frame

# M4: container panels (vanilla layout: title y 6, inventory 82 px under the first row, hotbar 58 px under it)
# M66: the external filter (216 x 184) and the cabinet (216 x 188) are bigger: room for their texts, the front button and the stored energy;
# the inventory moves with the panel (x + 20, y + 18 / + 22): the Java menus mirror these numbers.
EXTERNAL = (0, 0, 216, 184)
CABINET = (0, 0, 216, 188)
FEEDER = (0, 0, 176, 190)  # M66: the feeders' own panel (the dispenser panel had no room for their hint); the slot frames are drawn by the screen
FEEDER_SLOT_FRAME = (180, 0)  # 18x18
FILTER_SLOTS = [(89, 35), (107, 35)]
CABINET_FILTER = (26, 18)
CABINET_HEATERS = [(17, 44), (35, 44)]  # frames drawn by the screen only while a filter is in the slot
CABINET_STORAGE = [(124 + 18 * c, 17 + 18 * r) for r in range(3) for c in range(3)]
SIDE_SLOT_FRAME = (220, 0)  # 18x18 (the sprites sit right of the panels)
SIDE_HEATER_GHOST = (220, 18)  # 16x16
SIDE_FILTER_GHOST = (220, 34)  # 16x16
CABINET_AIR_PUMP = (74, 44)  # M29.1: the air pump slot of the cabinet
SIDE_AIR_PUMP_GHOST = (220, 50)  # 16x16, cabinet only


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
    inset(img, *CONTENT, fill=CONTENT_FILL)
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
    x, y = SLOT_FRAME
    inset(img, x, y, 18, 18)
    return img


def filter_ghost() -> np.ndarray:
    """Semi-transparent canister silhouette (empty filter slot hint)."""
    out = np.zeros((16, 16, 4), np.uint8)
    out[4:15, 4:12] = (0x37, 0x37, 0x37, 0x70)
    out[2:4, 5:11] = (0x37, 0x37, 0x37, 0x90)
    out[0:2, 6:8] = (0x37, 0x37, 0x37, 0xA0)
    out[1, 8:12] = (0x37, 0x37, 0x37, 0xA0)
    return out


def air_pump_ghost() -> np.ndarray:
    """M29.1: semi-transparent air pump silhouette (a low box with a nozzle and a bit of hose; empty air pump slot hint)."""
    out = np.zeros((16, 16, 4), np.uint8)
    out[9:14, 2:12] = (0x37, 0x37, 0x37, 0x70)
    out[8, 3:11] = (0x37, 0x37, 0x37, 0x90)
    out[10:12, 12:14] = (0x37, 0x37, 0x37, 0xA0)
    out[3:11, 13:15] = (0x37, 0x37, 0x37, 0x80)
    out[2:4, 10:15] = (0x37, 0x37, 0x37, 0x80)
    out[14, 3:5] = out[14, 9:11] = (0x37, 0x37, 0x37, 0xA0)
    return out


def _small_panel(size) -> np.ndarray:
    img = np.zeros((H, W, 4), np.uint8)
    panel(img, *size)
    ix, iy = (size[2] - 162) // 2 + 1, size[3] - 82  # the player inventory: 162 px wide, 82 px from the bottom edge
    for r in range(3):
        for c in range(9):
            slot(img, ix + 18 * c, iy + 18 * r)
    for c in range(9):
        slot(img, ix + 18 * c, iy + 58)
    x, y = SIDE_SLOT_FRAME
    inset(img, x, y, 18, 18)
    x, y = SIDE_HEATER_GHOST
    img[y:y + 16, x:x + 16] = heater_ghost()
    x, y = SIDE_FILTER_GHOST
    img[y:y + 16, x:x + 16] = filter_ghost()
    return img


def external_filter_gui() -> np.ndarray:
    img = _small_panel(EXTERNAL)
    for ix, iy in FILTER_SLOTS:
        slot(img, ix, iy)
    return img


def feeder_gui() -> np.ndarray:
    img = _small_panel(FEEDER)
    x, y = FEEDER_SLOT_FRAME
    inset(img, x, y, 18, 18)
    return img


def cabinet_gui() -> np.ndarray:
    img = _small_panel(CABINET)
    slot(img, *CABINET_FILTER)
    for ix, iy in CABINET_STORAGE:
        slot(img, ix, iy)
    # a small arrow from the filter down to its heaters
    x0 = CABINET_FILTER[0] + 7
    img[36:41, x0:x0 + 2] = DARK
    # M29.1: the air pump slot and its hint
    slot(img, *CABINET_AIR_PUMP)
    x, y = SIDE_AIR_PUMP_GHOST
    img[y:y + 16, x:x + 16] = air_pump_ghost()
    return img


def write_all() -> list:
    return [save_png(tank_gui(), GUI_TEX / "tank.png"), save_png(external_filter_gui(), GUI_TEX / "external_filter.png"),
            save_png(cabinet_gui(), GUI_TEX / "cabinet.png"), save_png(feeder_gui(), GUI_TEX / "feeder.png")]


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
