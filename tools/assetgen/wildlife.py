"""M22.3 textures: the wild corals of the warm ocean reefs (one sprite per coral, drawn from its colours), the critter
bucket overlay (a shrimp in the bucket, body and legs tinted per animal) and the magnifier.

Run with the py launcher (PIL):  py tools/assetgen/wildlife.py
"""
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'common', 'src', 'main', 'resources', 'assets', 'bettacraft', 'textures')
BLOCK = os.path.join(ROOT, 'block')
ITEM = os.path.join(ROOT, 'item')

# tissue, tips, glow (PlantSpecies)
CORALS = {
    'zoanthus': (0x3E8A5A, 0xF07830, 0xFFB04A),
    'green_star_polyps': (0x6A2E6E, 0x5AE05A, 0x9AFF6A),
    'mushroom_coral': (0x9A2E52, 0xC85A7A, 0xFF5AA8),
    'leather_coral': (0xC8B888, 0xE0D8A8, 0xE8FFB0),
    'hammer_coral': (0x3E8A74, 0xA8F0C0, 0x5AFFD8),
    'lobophyllia': (0xA8402A, 0x4E9A4A, 0xFF6A3A),
    'acropora': (0x5A62C0, 0xA8C0FF, 0x7AA8FF),
    'bubble_tip_anemone': (0xC24A56, 0xF4A6B8, 0xFF7AA6),
}


def rgb(c, a=255):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255, a)


def mix(a, b, f):
    return tuple(int(round(a[i] * (1 - f) + b[i] * f)) for i in range(3)) + (255,)


def shade(c, f):
    if isinstance(c, int):
        c = rgb(c)
    return tuple(max(0, min(255, int(round(c[i] * f)))) for i in range(3)) + (c[3] if len(c) > 3 else 255,)


def new():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def put(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), c)


def polyp_mat(tissue, tip, glow, r):
    """Top of a mat of polyps: tissue ground with round heads (rim tissue, coloured disc, bright mouth)."""
    im = Image.new('RGBA', (16, 16), shade(tissue, 0.7))
    for _ in range(26):
        x, y = r.randrange(16), r.randrange(16)
        put(im, x, y, shade(tissue, 0.85 + r.random() * 0.3))
    for cy in range(1, 16, 4):
        for cx in range((cy // 4) % 2 * 2, 16, 4):
            x, y = cx + r.randrange(-1, 2), cy + r.randrange(-1, 2)
            for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
                put(im, x + dx, y + dy, rgb(tip))
            put(im, x, y, rgb(glow))
            for dx, dy in ((-1, 0), (2, 0), (0, -1), (0, 2), (-1, 1), (2, 1), (1, -1), (1, 2)):
                put(im, x + dx, y + dy, shade(rgb(tissue), 1.05))
    return im


def stalks(tissue, tip, glow, r, n=7, top=3):
    """Cross sprite: polyps on short stalks."""
    im = new()
    for i in range(n):
        x = 1 + i * 14 // max(1, n - 1) + r.randrange(-1, 1)
        h = top + r.randrange(0, 6)
        for y in range(15, h, -1):
            put(im, x, y, shade(rgb(tissue), 0.8 + 0.3 * (15 - y) / 12))
        for dx in (-1, 0, 1):
            put(im, x + dx, h, rgb(tip))
        put(im, x, h - 1, rgb(glow))
    return im


def disc_top(tissue, tip, glow, r):
    im = Image.new('RGBA', (16, 16), shade(rgb(tissue), 0.75))
    c = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - c, y - c)
            a = math.atan2(y - c, x - c)
            stripe = 0.9 + 0.12 * math.sin(a * 9)
            col = mix(rgb(tissue), rgb(tip), max(0, 1 - d / 8)) if d < 8 else rgb(tissue)
            im.putpixel((x, y), shade(col, stripe))
    for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
        put(im, 7 + dx, 7 + dy, rgb(glow))
    return im


def brain(tissue, tip, glow, r):
    im = Image.new('RGBA', (16, 16), rgb(tissue))
    for y in range(16):
        for x in range(16):
            v = math.sin(x * 0.9 + math.sin(y * 0.7) * 2.2) + math.sin(y * 0.8 + x * 0.3)
            if v > 0.7:
                im.putpixel((x, y), rgb(tip))
            elif v > 0.4:
                im.putpixel((x, y), mix(rgb(tip), rgb(glow), 0.5))
            elif v < -1.2:
                im.putpixel((x, y), shade(rgb(tissue), 0.7))
    return im


def branches(tissue, tip, glow, r, hammer=False):
    """Cross sprite: a branching colony (acropora) or branches with hammer-shaped polyps."""
    im = new()

    def branch(x, y, ang, length, depth):
        for i in range(length):
            x += math.sin(ang) * 1.0
            y -= math.cos(ang) * 1.0
            xi, yi = int(round(x)), int(round(y))
            put(im, xi, yi, shade(rgb(tissue), 0.85 + 0.25 * (15 - yi) / 15))
            if hammer and i % 2 == 0:
                put(im, xi - 1, yi, rgb(tip))
                put(im, xi + 1, yi, rgb(tip))
        xi, yi = int(round(x)), int(round(y))
        if hammer:
            for dx in (-2, -1, 0, 1, 2):
                put(im, xi + dx, yi - 1, rgb(tip))
            put(im, xi, yi - 2, rgb(glow))
        else:
            put(im, xi, yi - 1, rgb(glow))
            put(im, xi, yi, rgb(tip))
        if depth > 0:
            branch(x, y, ang - 0.5, length - 1, depth - 1)
            branch(x, y, ang + 0.5, length - 1, depth - 1)

    branch(7.5, 15.5, 0, 5, 2)
    branch(3.5, 15.5, -0.3, 4, 1)
    branch(11.5, 15.5, 0.3, 4, 1)
    return im


def anemone(tissue, tip, glow, r):
    """Cross sprite: tentacles with bubble tips on a foot."""
    im = new()
    for x in range(4, 12):
        put(im, x, 15, shade(rgb(0xD8A890), 0.9))
        put(im, x, 14, shade(rgb(0xD8A890), 1.0))
    for i in range(9):
        a = -1.1 + i * 2.2 / 8
        x, y = 7.5, 13.5
        n = 6 + r.randrange(0, 4)
        for k in range(n):
            x += math.sin(a) * 0.9
            y -= math.cos(a) * 0.9
            put(im, int(round(x)), int(round(y)), mix(rgb(tissue), rgb(tip), k / n))
        xi, yi = int(round(x)), int(round(y))
        put(im, xi, yi, rgb(tip))
        put(im, xi, yi - 1, rgb(glow))
        put(im, xi + 1, yi, rgb(tip))
    return im


def stem(tissue):
    im = Image.new('RGBA', (16, 16), shade(rgb(tissue), 0.85))
    r = random.Random(3)
    for _ in range(40):
        put(im, r.randrange(16), r.randrange(16), shade(rgb(tissue), 0.75 + r.random() * 0.3))
    return im


def corals():
    for name, (tissue, tip, glow) in CORALS.items():
        r = random.Random(name)
        if name in ('zoanthus', 'green_star_polyps'):
            polyp_mat(tissue, tip, glow, r).save(os.path.join(BLOCK, 'wild_%s.png' % name))
            stalks(tissue, tip, glow, r, n=8, top=6).save(os.path.join(BLOCK, 'wild_%s_cross.png' % name))
        elif name == 'mushroom_coral':
            disc_top(tissue, tip, glow, r).save(os.path.join(BLOCK, 'wild_%s.png' % name))
        elif name == 'lobophyllia':
            brain(tissue, tip, glow, r).save(os.path.join(BLOCK, 'wild_%s.png' % name))
        elif name == 'leather_coral':
            polyp_mat(tissue, tip, glow, r).save(os.path.join(BLOCK, 'wild_%s.png' % name))
            stem(tissue).save(os.path.join(BLOCK, 'wild_%s_stem.png' % name))
        elif name == 'bubble_tip_anemone':
            anemone(tissue, tip, glow, r).save(os.path.join(BLOCK, 'wild_%s.png' % name))
        else:
            branches(tissue, tip, glow, r, hammer=name == 'hammer_coral').save(os.path.join(BLOCK, 'wild_%s.png' % name))


def critter_bucket():
    """A shrimp in the water of the bucket (rows 9-13 like the betta), body (layer 1) and legs and feelers (layer 2), grey."""
    body, legs = new(), new()
    g = (235, 235, 235, 255)
    d = (200, 200, 200, 255)
    for x, y in ((5, 10), (6, 10), (7, 10), (8, 10), (4, 11), (5, 11), (6, 11), (7, 11), (8, 11), (9, 11), (9, 10), (10, 11), (10, 12), (11, 12)):
        put(body, x, y, g)
    for x, y in ((6, 11), (8, 11)):
        put(body, x, y, d)
    put(body, 4, 10, (40, 36, 36, 255))  # the eye
    for x, y in ((3, 9), (2, 9), (3, 10), (5, 12), (7, 12), (9, 12), (11, 13), (12, 12)):
        put(legs, x, y, g)
    body.save(os.path.join(ITEM, 'critter_bucket_animal.png'))
    legs.save(os.path.join(ITEM, 'critter_bucket_animal_legs.png'))


def magnifier():
    im = new()
    ring = [(5, 1), (6, 1), (7, 1), (8, 1), (4, 2), (9, 2), (3, 3), (10, 3), (3, 4), (10, 4), (3, 5), (10, 5), (3, 6), (10, 6), (4, 7), (9, 7),
            (5, 8), (6, 8), (7, 8), (8, 8)]
    copper = (196, 112, 70, 255)
    dark = (140, 74, 46, 255)
    for x, y in ring:
        put(im, x, y, copper)
    for x, y in ((4, 2), (3, 3), (3, 4)):
        put(im, x, y, (232, 156, 108, 255))
    for x, y in ((9, 7), (8, 8), (10, 6)):
        put(im, x, y, dark)
    for y in range(2, 8):
        for x in range(4, 10):
            if im.getpixel((x, y))[3] == 0:
                put(im, x, y, (170, 214, 236, 150))
    put(im, 5, 3, (240, 250, 255, 230))
    put(im, 5, 4, (240, 250, 255, 200))
    put(im, 6, 3, (240, 250, 255, 200))
    handle = (110, 76, 44, 255)
    for i, (x, y) in enumerate(((9, 9), (10, 10), (11, 11), (12, 12), (13, 13), (14, 14))):
        put(im, x, y, handle if i > 0 else dark)
        put(im, x + 1, y, shade(handle, 0.7) if i > 0 else dark)
    im.save(os.path.join(ITEM, 'magnifier.png'))


if __name__ == '__main__':
    corals()
    critter_bucket()
    magnifier()
    print('wildlife textures written')
