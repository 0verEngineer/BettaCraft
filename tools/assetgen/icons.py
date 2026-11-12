"""16x16 item icons in a vanilla-like pixel-art style (dark outline, 3-4 step ramps, light from top-left).

Icons are authored as small character grids + parametric shapes, so everything is generated from
this file (no external textures). Overlay layers (``*_fish.png``, ``*_fish_fins.png``) are
greyscale and get tinted at runtime through ItemColor (tint index 1 = body colour, 2 = fin colour).
"""
from __future__ import annotations

import numpy as np

from common import ITEM_TEX, hex_rgb, save_png

S = 16


def grid(rows: list[str], cmap: dict[str, str | tuple]) -> np.ndarray:
    """Character grid -> uint8 RGBA. cmap values: '#rrggbb', '#rrggbbaa' or (r,g,b,a)."""
    assert len(rows) == S and all(len(r) == S for r in rows), "icon grids must be 16x16"
    out = np.zeros((S, S, 4), np.uint8)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in ". ":
                continue
            c = cmap[ch]
            if isinstance(c, str):
                h = c.lstrip("#")
                rgba = (*hex_rgb(h[:6]), int(h[6:8], 16) if len(h) == 8 else 255)
            else:
                rgba = c
            out[y, x] = rgba
    return out


def grey(v: int, a: int = 255):
    return (v, v, v, a)


# ---------------------------------------------------------------- bucket

IRON = {"k": "#2c2c31", "H": "#f4f4f4", "L": "#d6d6d6", "M": "#a9a9ad", "D": "#76767c", "E": "#55555b",
        "W": "#5c8ff0", "w": "#3a68d6", "v": "#2b4fb0"}

BUCKET = [
    "................",
    "................",
    "....kkkkkkkk....",
    "...kDD....DDk...",
    "..kD........Dk..",
    ".kkkkkkkkkkkkkk.",
    ".kHWWWWWWWWWWEk.",
    ".kLwwwwwwwwwwvk.",
    ".kHHLLLLLLMMDEk.",
    "..kLHLMMMMMDEk..",
    "..kLLMMMMMMDEk..",
    "..kLLMMMMMMDEk..",
    "...kLMMMMMDEk...",
    "...kLMMMMMDEk...",
    "....kkkkkkkk....",
    "................",
]

# Fish overlays are authored as fill masks (b = body, e = eye, F = fin); outline and shading are
# derived in fish_layers(). Body layer = tint index 1, fin layer = tint index 2.
FISH_BUCKET = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".....FFF........",
    "....FFFFFF..FF..",
    "...bbbbbbFFFFFF.",
    "..bbebbbbbbFFFF.",
    "..bbbbbbbbbFFFF.",
    "...bbbbbbbFFFFF.",
    "....FFFFFFFFFFF.",
    ".....FFFFF..FF..",
    "......FF........",
    "................",
]
FISH_BAG = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "....FFF.........",
    "...FFFFF..FF....",
    "..bbbbbbFFFFF...",
    ".bbebbbbbbFFF...",
    "..bbbbbbbFFFF...",
    "...FFFFFFFFF....",
    "....FFF...FF....",
    "................",
    "................",
]
OUTLINE_GREY = 88

def bucket():
    return grid(BUCKET, IRON)


def _neighbours(mask):
    n = np.zeros_like(mask)
    n[1:, :] |= mask[:-1, :]
    n[:-1, :] |= mask[1:, :]
    n[:, 1:] |= mask[:, :-1]
    n[:, :-1] |= mask[:, 1:]
    return n


def fish_layers(rows):
    """Fill grid -> (body layer, fin layer) greyscale RGBA with auto outline and shading."""
    ch = np.array([list(r) for r in rows])
    body = np.isin(ch, ["b", "e"])
    fin = ch == "F"
    eye = ch == "e"
    filled = body | fin
    outline = _neighbours(filled) & ~filled
    out_body = outline & _neighbours(body)
    out_fin = outline & ~out_body
    B = np.zeros((S, S, 4), np.uint8)
    Fl = np.zeros((S, S, 4), np.uint8)
    # body: per column ramp, light from the top
    for x in range(S):
        ys = np.nonzero(body[:, x])[0]
        if len(ys) == 0:
            continue
        top, bot = ys.min(), ys.max()
        for y in ys:
            r = (y - top) / max(1, bot - top)
            v = 255 if y == top else 232 if r < 0.45 else 204 if r < 0.8 else 164
            B[y, x] = grey(v)
    B[eye] = grey(22)
    B[out_body] = grey(OUTLINE_GREY)
    # fins: lighter near the body, darker towards the rim
    dist = np.where(body, 0, 99)
    for k in range(1, 8):
        grow = _neighbours(dist == k - 1) & fin & (dist == 99)
        dist[grow] = k
    rim = fin & _neighbours(~filled)
    for y, x in zip(*np.nonzero(fin)):
        d = dist[y, x]
        v = 236 if d <= 2 else 206
        if rim[y, x]:
            v -= 30
        Fl[y, x] = grey(v)
    Fl[out_fin] = grey(OUTLINE_GREY)
    return B, Fl


# ---------------------------------------------------------------- fish bag

BAG = {"k": "#b9c7cfd8", "K": "#ffffffff", "n": "#dfe8ecff", "o": "#8fa3ad", "s": "#e9f6ffb0",
       "W": "#7fb2f2c8", "w": "#5a92e6c0", "v": "#4a7ccfc8", "h": "#ffffffd0", "e": "#ffffff70"}
BAG_ROWS = [
    "................",
    "......oKKo......",
    ".......nn.......",
    "......kssk......",
    ".....kseesk.....",
    "....khseeesk....",
    "...khseeeeesk...",
    "..khWWWWWWWWWk..",
    "..khwwwwwwwwwk..",
    ".khwwwwwwwwwwvk.",
    ".khwwwwwwwwwwvk.",
    ".kwwwwwwwwwwwvk.",
    ".kwwwwwwwwwwvvk.",
    "..kwwwwwwwwvvk..",
    "...kkkkkkkkkk...",
    "................",
]
def fish_bag():
    return grid(BAG_ROWS, BAG)


# ---------------------------------------------------------------- pellets

PELLET = {"k": "#3d1f12", "H": "#e2a070", "L": "#c0703e", "M": "#9a4f28", "D": "#6e3418"}
PELLETS = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "......kkk.......",
    ".....kHLMk......",
    ".....kLMDk.kkk..",
    "..kkk.kkk.kHLMk.",
    ".kHLMkkkk.kLMDk.",
    ".kLMDkHLMk.kkk..",
    "..kkkkLMDkkkk...",
    ".kkk..kkkHLMDk..",
    "kHLMk.kHLkMDk...",
    "kLMDk.kLMDkk....",
    ".kkk...kkkk.....",
]


def pellets():
    return grid(PELLETS, PELLET)


# ---------------------------------------------------------------- mosquito larvae jar

JAR = {"k": "#3b4a52", "c": "#6b4a2b", "C": "#a67a4c", "B": "#c99a62", "g": "#d8eef5e0", "G": "#ffffffe8",
       "W": "#7fb2f2c0", "w": "#5a92e6b8", "v": "#4a7ccfc0", "l": "#3a1f1a", "r": "#7a2a22"}
JAR_ROWS = [
    "................",
    ".....kkkkkk.....",
    ".....kBCCck.....",
    ".....kCCcck.....",
    "....kggggggk....",
    "....kG....gk....",
    "...kG......gk...",
    "...kGWWWWWWgk...",
    "...kGwwlwwwvk...",
    "...kGwlwwrwvk...",
    "...kGwwlwrwvk...",
    "...kGwrwwwlvk...",
    "...kGwwrwwlvk...",
    "...kGwwwwwwvk...",
    "....kkkkkkkk....",
    "................",
]


def mosquito_larvae():
    return grid(JAR_ROWS, JAR)


# ---------------------------------------------------------------- heaters (parametric)

HEATER = {
    "outline": (58, 84, 96, 255), "glass": (214, 238, 245, 200), "glass_hi": (250, 254, 255, 230),
    "coil": (232, 128, 40, 255), "coil_dark": (168, 70, 22, 255),
    "cap_k": (24, 24, 28, 255), "cap": (64, 64, 70, 255), "cap_hi": (110, 110, 118, 255),
    "led": (240, 60, 50, 255), "cable": (30, 30, 34, 255),
}


# M9: coolers use the same rod with cooling fins in ice blue and a blue LED
COOLER = {**HEATER, "outline": (46, 74, 110, 255), "coil": (120, 196, 240, 255), "coil_dark": (52, 118, 186, 255),
          "led": (70, 190, 255, 255)}


def heater(length: int, pal: dict | None = None) -> np.ndarray:
    """Diagonal heater rod, bottom-left glass end -> top-right cap. length = rod length in
    diagonal steps (pixels along the diagonal). pal = colours (HEATER, or COOLER for M9 coolers)."""
    HEATER = pal or globals()["HEATER"]  # noqa: N806 - same names as the module palette
    img = np.zeros((S, S, 4), np.uint8)
    # the rod is centred on the anti-diagonal x + y = c; along-axis coordinate a = x - y
    c = 15
    a_end = 12  # cap end (top right)
    a_start = a_end - 2 * length
    cap_len = 6  # in 'a' units (=3 px)

    def put(x, y, col):
        if 0 <= x < S and 0 <= y < S:
            img[y, x] = col

    for y in range(S):
        for x in range(S):
            a, k = x - y, x + y - c
            if a < a_start - 1 or a > a_end + 1 or abs(k) > 2:
                continue
            inside = abs(k) <= 1 and a_start <= a <= a_end
            in_cap = a > a_end - cap_len
            if in_cap:
                if abs(k) <= 2 and a_end - cap_len < a <= a_end + 1:
                    if abs(k) == 2 or a >= a_end:
                        put(x, y, HEATER["cap_k"])
                    elif k == -1:
                        put(x, y, HEATER["cap_hi"])
                    else:
                        put(x, y, HEATER["cap"])
                continue
            if inside:
                if k == -1:
                    put(x, y, HEATER["glass_hi"])
                elif a > a_start + 1 and (a // 2) % 2 == 0:
                    put(x, y, HEATER["coil"] if k == 0 else HEATER["coil_dark"])
                else:
                    put(x, y, HEATER["glass"])
            elif abs(k) == 2 and a_start <= a <= a_end or (abs(k) <= 1 and a == a_start - 1):
                put(x, y, HEATER["outline"])
    # rounded glass end
    x0 = (a_start + c) // 2
    put(x0 - 1, c - x0 + 1 - 1 + 1, (0, 0, 0, 0))
    # indicator LED on the cap + cable leaving the cap towards the top-right corner
    lx = (a_end - 3 + c) // 2
    put(lx, c - lx, HEATER["led"])
    ex = (a_end + c) // 2 + 1
    for i, (dx, dy) in enumerate([(0, -1), (1, -1), (1, -2), (2, -2)]):
        put(ex + dx, c - ex + dy + 1, HEATER["cable"])
    return img


# ---------------------------------------------------------------- swamp water bottle (vanilla potion-like)

BOTTLE = {"k": "#2f3a2c", "c": "#6b4a2b", "C": "#a67a4c", "g": "#cfe3e8d8", "G": "#ffffffe8",
          "W": "#7c8a4a", "w": "#5f6e36", "v": "#46522a", "d": "#2e3618", "s": "#8e9a58"}
BOTTLE_ROWS = [
    "................",
    "......kkkk......",
    "......kCck......",
    "......kCck......",
    ".......gg.......",
    "......kGgk......",
    ".....kG..gk.....",
    "....kG....gk....",
    "...kGWWsWWWgk...",
    "...kGwwdwwwvk...",
    "...kGwwwwdwvk...",
    "...kGwdwwwwvk...",
    "...kGwwwdwvvk...",
    "....kwwvvvvk....",
    ".....kkkkkk.....",
    "................",
]


def swamp_water_bottle():
    return grid(BOTTLE_ROWS, BOTTLE)


# ---------------------------------------------------------------- mirror stick (M11)

def mirror_stick():
    """Hand mirror on a stick, held diagonally: round silvered glass in an iron frame (top right), wooden handle."""
    out = np.zeros((S, S, 4), np.uint8)
    cx, cy, r = 10.0, 5.5, 4.7
    yy, xx = np.mgrid[0:S, 0:S] + 0.5
    d = np.hypot(xx - cx, yy - cy)
    wood = {"k": (0x2c, 0x1e, 0x12, 255), "L": (0xb0, 0x83, 0x4c, 255), "D": (0x7a, 0x55, 0x2e, 255)}
    # handle: from (7, 9) down-left to (2, 14), 2 px wide with an outline
    for i in range(7):
        x, y = 7 - i, 9 + i
        if not (0 <= x < S and 0 <= y < S):
            continue
        for dx, dy in ((-1, 0), (0, 1), (-1, 1), (1, 0), (0, -1)):
            px, py = x + dx, y + dy
            if 0 <= px < S and 0 <= py < S and out[py, px, 3] == 0:
                out[py, px] = wood["k"]
    for i in range(6):
        x, y = 7 - i, 9 + i
        out[y, x] = wood["L"]
        if x - 1 >= 0 and y + 1 < S:
            out[y, x - 1] = wood["D"] if i < 5 else wood["k"]
    # iron collar where the handle meets the frame
    out[9, 7] = (0xa9, 0xa9, 0xad, 255)
    out[8, 8] = (0xd6, 0xd6, 0xd6, 255)
    # frame and glass
    for y in range(S):
        for x in range(S):
            dv = d[y, x]
            if dv > r + 0.55:
                continue
            if dv > r - 0.35:
                out[y, x] = (0x2c, 0x2c, 0x31, 255)
            elif dv > r - 1.35:
                # iron frame, lighter towards the top left
                lit = (cx - xx[y, x]) + (cy - yy[y, x])
                v = 0xd6 if lit > 1.5 else 0xa9 if lit > -1.5 else 0x76
                out[y, x] = (v, v, v + 4, 255)
            else:
                # silvered glass: pale blue with a gradient and a diagonal highlight
                t = ((xx[y, x] - cx) - (yy[y, x] - cy)) / (2 * r)
                base = np.array([0xb8, 0xdc, 0xf0]) * (1.0 - 0.25 * (0.5 + t)) + np.array([0x40, 0x60, 0x80]) * 0.25 * (0.5 + t)
                hl = abs((xx[y, x] - cx) + (yy[y, x] - cy) + 1.2) < 0.75 and (xx[y, x] - cx) < 1.2
                c = (0xf4, 0xfa, 0xff) if hl else tuple(int(v) for v in base)
                out[y, x] = (*c, 255)
    return out


ICONS = {
    "mirror_stick": mirror_stick,
    "swamp_water_bottle": swamp_water_bottle,
    "betta_bucket": bucket,
    "fish_bag": fish_bag,
    "betta_pellets": pellets,
    "mosquito_larvae": mosquito_larvae,
    "heater_small": lambda: heater(6),
    "heater_medium": lambda: heater(8),
    "heater_large": lambda: heater(10),
    "cooler_small": lambda: heater(6, COOLER),
    "cooler_medium": lambda: heater(8, COOLER),
    "cooler_large": lambda: heater(10, COOLER),
}


def all_icons() -> dict[str, np.ndarray]:
    out = {k: f() for k, f in ICONS.items()}
    out["betta_bucket_fish"], out["betta_bucket_fish_fins"] = fish_layers(FISH_BUCKET)
    out["fish_bag_fish"], out["fish_bag_fish_fins"] = fish_layers(FISH_BAG)
    return out


def write_all() -> list:
    return [save_png(img, ITEM_TEX / f"{name}.png") for name, img in all_icons().items()]


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
