"""M51: the hired aquarist (a person of his own, not a villager), four skins in Minecraft's human layout (64x64, the
model of the player with wide arms, the second layer for the clothes).

* ``textures/entity/hired_aquarist/aquarist_<0..3>.png``: four skin tones and hair colours; a light blue work shirt
  with rolled-up sleeves, a teal rubber bib apron with dark straps and an orange betta on the bib, blue rubber gloves,
  dark work trousers, black rubber boots; variants 0 and 2 wear the teal cap of the aquarists (with a fish badge), 1 and
  3 show their hair.

Everything is drawn here from rectangles and a little noise (no third-party skin). Run: py tools/assetgen/hired_aquarist.py
(also from build.py).
"""
import numpy as np

from common import TEX, hex_f, save_png

OUT = TEX / "entity" / "hired_aquarist"

# (skin, skin shade, hair, eyes)
VARIANTS = [
    ("#e9b996", "#c99474", "#5b3a22", "#3d6fa8"),
    ("#c68a5e", "#a06a44", "#1e1a18", "#3a2a1a"),
    ("#f2cba8", "#d2a684", "#d9a441", "#3f8a52"),
    ("#7d5136", "#5e3a24", "#16120f", "#2a1d14"),
]
SHIRT, SHIRT_DARK = "#a9c6d8", "#86a6bb"
TEAL, TEAL_DARK, STRAP = "#2c8c86", "#1f6a66", "#1c2e2e"
TROUSERS, TROUSERS_DARK = "#3a3f4a", "#2c3038"
BOOT, BOOT_LIGHT = "#1b1d20", "#33373c"
GLOVE = "#3a6ad0"
FISH = "#e8701e"


def rect(img, x0, y0, x1, y1, col, rng=None, var=0.05):
    """Fills x0..x1-1, y0..y1-1 with a colour (with a little noise per texel)."""
    c = hex_f(col)
    for y in range(y0, y1):
        for x in range(x0, x1):
            k = 1.0 if rng is None else 1 - var + 2 * var * rng.random()
            img[y, x, :3] = np.clip(c * k, 0, 1)
            img[y, x, 3] = 1


def faces(u, v, w, h, d):
    """The faces of a box in Minecraft's UV layout: name -> (x0, y0, x1, y1)."""
    return {
        "top": (u + d, v, u + d + w, v + d),
        "bottom": (u + d + w, v, u + d + 2 * w, v + d),
        "right": (u, v + d, u + d, v + d + h),
        "front": (u + d, v + d, u + d + w, v + d + h),
        "left": (u + d + w, v + d, u + 2 * d + w, v + d + h),
        "back": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }


def fill_box(img, u, v, w, h, d, col, rng, only=None):
    for name, (x0, y0, x1, y1) in faces(u, v, w, h, d).items():
        if only is None or name in only:
            rect(img, x0, y0, x1, y1, col, rng)


def side_rows(img, u, v, w, h, d, y_from, y_to, col, rng):
    """Rows y_from..y_to-1 (counted from the top of the box sides) on the four sides of a box."""
    for name in ("right", "front", "left", "back"):
        x0, y0, x1, _ = faces(u, v, w, h, d)[name]
        rect(img, x0, y0 + y_from, x1, y0 + y_to, col, rng)


def skin(i: int) -> np.ndarray:
    tone, shade, hair, eye = VARIANTS[i]
    rng = np.random.default_rng(5100 + i)
    img = np.zeros((64, 64, 4))
    cap = i % 2 == 0
    # ---- head (0,0: 8x8x8)
    fill_box(img, 0, 0, 8, 8, 8, tone, rng)
    rect(img, 8, 0, 16, 8, hair, rng)  # top
    f = faces(0, 0, 8, 8, 8)
    for name in ("right", "left", "back"):
        x0, y0, x1, y1 = f[name]
        rect(img, x0, y0, x1, y0 + (3 if name != "back" else 6), hair, rng)
    # the face (front 8,8..16,16): hair line, brows, eyes, nose, mouth
    rect(img, 8, 8, 16, 10, hair, rng)
    rect(img, 9, 11, 11, 12, hair)  # brows
    rect(img, 13, 11, 15, 12, hair)
    rect(img, 9, 12, 11, 13, "#f4f4f4")  # eyes: white, iris inside
    rect(img, 13, 12, 15, 13, "#f4f4f4")
    img[12, 10, :3] = hex_f(eye)
    img[12, 13, :3] = hex_f(eye)
    rect(img, 11, 13, 13, 14, shade)  # nose
    rect(img, 10, 14, 14, 15, "#9c5a4a" if i != 3 else "#5a3428")  # mouth
    # ---- hat layer (32,0): the teal cap, or a fuller fringe
    if cap:
        rect(img, 40, 0, 48, 8, TEAL, rng)
        hf = faces(32, 0, 8, 8, 8)
        for name in ("right", "front", "left", "back"):
            x0, y0, x1, _ = hf[name]
            rect(img, x0, y0, x1, y0 + 2, TEAL, rng)
        rect(img, 40, 10, 48, 11, TEAL_DARK)  # the brim's shadow over the brow
        img[3, 43, :3] = hex_f(FISH)  # the fish badge on top
        img[3, 44, :3] = hex_f(FISH)
        img[3, 43, 3] = img[3, 44, 3] = 1
    else:
        hf = faces(32, 0, 8, 8, 8)
        x0, y0, x1, _ = hf["back"]
        rect(img, x0, y0 + 5, x1, y0 + 8, hair, rng)  # hair over the neck
        x0, y0, x1, _ = hf["top"]
        rect(img, x0 + 1, y0 + 1, x1 - 1, y0 + 7, hair, rng)  # a little volume on top
    # ---- body (16,16: 8x12x4): the shirt, the trouser waist at the bottom
    fill_box(img, 16, 16, 8, 12, 4, SHIRT, rng)
    side_rows(img, 16, 16, 8, 12, 4, 10, 12, TROUSERS, rng)
    rect(img, 28, 16, 36, 20, TROUSERS, rng)  # bottom face
    # collar
    rect(img, 22, 20, 26, 21, SHIRT_DARK)
    img[21, 23, :3] = img[21, 24, :3] = hex_f(tone)
    # ---- the apron in the jacket layer (16,32): bib on the front, straps over the shoulders, crossed on the back
    jf = faces(16, 32, 8, 12, 4)
    x0, y0, x1, y1 = jf["front"]
    rect(img, x0 + 1, y0 + 3, x1 - 1, y1, TEAL, rng)
    rect(img, x0, y0 + 7, x1, y1, TEAL, rng)  # wider below the waist band
    rect(img, x0, y0 + 7, x1, y0 + 8, STRAP)  # the waist band
    rect(img, x0 + 1, y0, x0 + 2, y0 + 3, STRAP)  # straps up over the shoulders
    rect(img, x1 - 2, y0, x1 - 1, y0 + 3, STRAP)
    tx0, ty0, _, _ = jf["top"]
    rect(img, tx0 + 1, ty0, tx0 + 2, ty0 + 4, STRAP)
    rect(img, tx0 + 6, ty0, tx0 + 7, ty0 + 4, STRAP)
    bx0, by0, bx1, by1 = jf["back"]
    for k in range(7):
        for x in (bx0 + 1 + k * 6 // 7, bx1 - 2 - k * 6 // 7):
            img[by0 + k, x, :3] = hex_f(STRAP)
            img[by0 + k, x, 3] = 1
    rect(img, bx0, by0 + 7, bx1, by0 + 8, STRAP)
    for name in ("right", "left"):
        sx0, sy0, sx1, _ = jf[name]
        rect(img, sx0, sy0 + 7, sx1, sy0 + 8, STRAP)
        rect(img, sx0, sy0 + 8, sx1, sy0 + 12, TEAL_DARK, rng)
    # the betta on the bib (orange, head right, its fins spread)
    fish = [(3, 4), (4, 4), (2, 5), (3, 5), (4, 5), (5, 5), (1, 6), (2, 6), (3, 6), (4, 6), (5, 6), (2, 7), (3, 7), (1, 4), (1, 8)]
    for fx, fy in fish:
        img[y0 + fy, x0 + fx, :3] = hex_f(FISH)
    img[y0 + 5, x0 + 5, :3] = hex_f("#1a1a1a")  # the eye
    # ---- arms (right 40,16; left 32,48: 4x12x4): shirt, rolled-up sleeves (skin below), blue gloves
    for (u, v), (su, sv) in (((40, 16), (40, 32)), ((32, 48), (48, 48))):
        fill_box(img, u, v, 4, 12, 4, SHIRT, rng)
        side_rows(img, u, v, 4, 12, 4, 5, 9, tone, rng)  # the forearm
        side_rows(img, u, v, 4, 12, 4, 9, 12, GLOVE, rng)  # the glove
        bf = faces(u, v, 4, 12, 4)["bottom"]
        rect(img, *bf, GLOVE, rng)
        # the rolled-up cuff in the sleeve layer
        side_rows(img, su, sv, 4, 12, 4, 4, 5, SHIRT_DARK, rng)
        side_rows(img, su, sv, 4, 12, 4, 8, 9, GLOVE, rng)  # the glove's rim
    # ---- legs (right 0,16; left 16,48: 4x12x4): trousers, boots up to the calf
    for (u, v), (pu, pv) in (((0, 16), (0, 32)), ((16, 48), (0, 48))):
        fill_box(img, u, v, 4, 12, 4, TROUSERS, rng)
        side_rows(img, u, v, 4, 12, 4, 0, 1, TROUSERS_DARK, rng)
        side_rows(img, u, v, 4, 12, 4, 6, 12, BOOT, rng)
        rect(img, *faces(u, v, 4, 12, 4)["bottom"], BOOT, rng)
        # the boot shaft stands out a little (pants layer): its rim
        side_rows(img, pu, pv, 4, 12, 4, 6, 7, BOOT_LIGHT, rng)
        side_rows(img, pu, pv, 4, 12, 4, 7, 12, BOOT, rng)
    return img


def write_all() -> list:
    return [save_png(skin(i), OUT / ("aquarist_%d.png" % i)) for i in range(len(VARIANTS))]


if __name__ == "__main__":
    for p in write_all():
        print(p)
