"""M47 part a: the aquarist villager and his workplace, the breeding basin.

* ``textures/entity/villager/profession/aquarist.png`` (and the same for the zombie villager): an overlay on the vanilla
  villager layout (64x64): a teal rubber apron with a bib and dark straps over the robe, an orange fish on the apron, blue
  rubber gloves, a teal cap.
* ``textures/block/breeding_basin_water.png`` (clear, a little green water with light ripples, drawn translucent),
  ``breeding_basin_plant.png`` (a cross of fine green stems for the plants in the basin), ``breeding_basin_wood.png``
  (the dark wooden stand) and ``breeding_basin_gravel.png`` (the bed of fine gravel).

Run: py tools/assetgen/aquarist.py (also from build.py).
"""
import numpy as np

from common import TEX, BLOCK_TEX, hex_f, save_png

VILLAGER = TEX / "entity" / "villager" / "profession" / "aquarist.png"
ZOMBIE = TEX / "entity" / "zombie_villager" / "profession" / "aquarist.png"


def _rect(img, x0, y0, x1, y1, col, rng=None, var=0.06):
    """Fills x0..x1-1, y0..y1-1 with a colour (a little noise per texel)."""
    c = hex_f(col)
    for y in range(y0, y1):
        for x in range(x0, x1):
            k = 1.0 if rng is None else 1 - var + 2 * var * rng.random()
            img[y, x, :3] = np.clip(c * k, 0, 1)
            img[y, x, 3] = 1


def villager() -> np.ndarray:
    rng = np.random.default_rng(4701)
    img = np.zeros((64, 64, 4))
    teal, dark, strap = "#2c8c86", "#1f6a66", "#1c2e2e"
    # the robe (jacket, 0,38: 8x20x6): the apron on the front from the waist down, its hem darker
    _rect(img, 6, 44, 14, 61, teal, rng)
    _rect(img, 6, 61, 14, 63, dark, rng)
    # the apron reaches a little round the sides
    _rect(img, 4, 44, 6, 62, dark, rng)
    _rect(img, 14, 44, 16, 62, dark, rng)
    # a belt over the robe front and back and the sides
    _rect(img, 0, 46, 28, 47, strap)
    # the fish on the apron (an orange betta, head right, its fins spread), below the crossed arms
    fish = [(9, 53), (10, 53), (7, 54), (8, 54), (9, 54), (10, 54), (11, 54), (8, 55), (9, 55), (10, 55), (11, 55), (12, 55),
            (7, 56), (9, 56), (10, 56), (6, 53), (6, 57)]
    for x, y in fish:
        img[y, x, :3] = hex_f("#e8701e")
        img[y, x, 3] = 1
    img[55, 11, :3] = hex_f("#1a1a1a")  # the eye
    # the body (16,20: 8x12x6): the bib on the chest, the straps over the shoulders and crossed on the back
    _rect(img, 23, 28, 29, 38, teal, rng)
    _rect(img, 23, 27, 29, 28, dark)
    for x in (23, 28):
        _rect(img, x, 20, x + 1, 26, strap)  # over the shoulders (top face)
        _rect(img, x, 26, x + 1, 28, strap)
    for i in range(10):  # crossed on the back (36,26: 8x12)
        for x in (37 + int(i * 0.6), 42 - int(i * 0.6)):
            img[26 + i, x, :3] = hex_f(strap)
            img[26 + i, x, 3] = 1
    # rubber gloves: the hands are the ends of the crossed forearms (40,38: 8x4x4): its two end faces and two texels of the
    # front, top and bottom at each end; rolled-up sleeves: a teal cuff at the lower end of the arms (44,22: 4x8x4)
    glove = "#3a6ad0"
    _rect(img, 40, 42, 44, 46, glove, rng)  # one end
    _rect(img, 52, 42, 56, 46, glove, rng)  # the other end
    for x0 in (44, 50):
        _rect(img, x0, 42, x0 + 2, 46, glove, rng)  # front
        _rect(img, x0, 38, x0 + 2, 42, glove, rng)  # top
        _rect(img, x0 + 8, 38, x0 + 10, 42, glove, rng)  # bottom
    for x0 in (56, 62):
        _rect(img, x0, 42, x0 + 2, 46, glove, rng)  # back
    _rect(img, 44, 32, 60, 34, dark, rng)
    # the cap (hat layer, 32,0: 8x10x8): crown and the top rows of the sides, a darker brim at the front
    _rect(img, 40, 0, 48, 8, teal, rng)
    _rect(img, 32, 8, 64, 10, teal, rng)
    _rect(img, 40, 10, 48, 11, dark)
    img[1, 43, :3] = hex_f("#e8701e")  # a little fish badge on top
    img[1, 44, :3] = hex_f("#e8701e")
    return img


def water() -> np.ndarray:
    rng = np.random.default_rng(4702)
    img = np.zeros((16, 16, 4))
    yy, xx = np.mgrid[0:16, 0:16]
    ripple = 0.5 + 0.5 * np.sin(xx * 0.9 + np.sin(yy * 0.7) * 2.0)
    base, light = hex_f("#3f8fa8"), hex_f("#9ed8e0")
    for y in range(16):
        for x in range(16):
            t = ripple[y, x] * 0.35 + rng.random() * 0.08
            img[y, x, :3] = np.clip(base * (1 - t) + light * t, 0, 1)
            img[y, x, 3] = 0.55
    return img


def plant() -> np.ndarray:
    rng = np.random.default_rng(4703)
    img = np.zeros((16, 16, 4))
    for i in range(6):
        x = 1 + i * 2.5 + rng.random()
        top = int(rng.integers(1, 6))
        sway = rng.random() * 2 - 1
        g = hex_f(["#3c9a3a", "#5cb848", "#2e7a34"][i % 3])
        for y in range(top, 16):
            xx = int(round(x + sway * np.sin(y * 0.45)))
            if 0 <= xx < 16:
                img[y, xx, :3] = np.clip(g * (0.85 + 0.3 * rng.random()), 0, 1)
                img[y, xx, 3] = 1
                if y % 3 == 0 and 0 <= xx + 1 < 16:  # small leaves
                    img[y, xx + 1, :3] = np.clip(g * 1.1, 0, 1)
                    img[y, xx + 1, 3] = 1
    return img


def wood() -> np.ndarray:
    rng = np.random.default_rng(4704)
    img = np.zeros((16, 16, 4))
    base = hex_f("#5a3a22")
    for y in range(16):
        for x in range(16):
            grain = 0.85 + 0.1 * np.sin(y * 1.3 + x * 0.15) + 0.08 * rng.random()
            col = base * grain
            if y in (0, 15) or x in (0, 15):
                col = col * 0.75
            img[y, x, :3] = np.clip(col, 0, 1)
            img[y, x, 3] = 1
    return img


def gravel() -> np.ndarray:
    rng = np.random.default_rng(4705)
    img = np.zeros((16, 16, 4))
    cols = [hex_f(c) for c in ("#c8b48a", "#a8946c", "#8a7a60", "#d8ccb0")]
    for y in range(16):
        for x in range(16):
            img[y, x, :3] = cols[int(rng.integers(0, 4))] * (0.9 + 0.2 * rng.random())
            img[y, x, 3] = 1
    return img


def write_all() -> list:
    v = villager()
    return [save_png(v, VILLAGER), save_png(v, ZOMBIE), save_png(water(), BLOCK_TEX / "breeding_basin_water.png"),
            save_png(plant(), BLOCK_TEX / "breeding_basin_plant.png"), save_png(wood(), BLOCK_TEX / "breeding_basin_wood.png"),
            save_png(gravel(), BLOCK_TEX / "breeding_basin_gravel.png")]


if __name__ == "__main__":
    for p in write_all():
        print(p)
