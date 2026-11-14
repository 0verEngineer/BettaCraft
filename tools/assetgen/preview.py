"""Preview sheets (build/preview/, gitignored), the combined review image builds/m0-preview.png and
the mod logo. Fish are rendered as flat orthographic side views from the real UV texture, composited
with the same layer order as the runtime renderer (see render.py)."""
from __future__ import annotations

import io
import shutil

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import blocks
import icons
import model_defs as md
import palette as pal
import patterns as pt
import varieties as vt
from common import LOGO, PREVIEW, ROOT, REVIEW_DIR, hex_f, save_png, to_image
from render import Fish, composite_texture, render_side

BG = (38, 62, 78, 255)
BG2 = (30, 50, 64, 255)
TEXT = (235, 240, 245, 255)
DIM = (160, 178, 190, 255)
PATTERN_CHOICES = [p for p in pt.PATTERNS if p != "bars"]


def font(size):
    try:
        return ImageFont.load_default(size)
    except TypeError:
        return ImageFont.load_default()


def fish_image(f: Fish, ppu=4, bounds=(-9.0, 17.0, 12.0, 28.0), scale=1.0) -> Image.Image:
    arr = render_side(f, ppu=ppu, bounds=bounds, scale=scale)
    stage = next(s for s in md.LIFE_STAGES if s["id"] == f.stage)
    arr[..., 3] *= stage["alpha"]
    return to_image(arr)


def form_name(shape: str, short: bool, dumbo: bool = False, lang: int = 0) -> str:
    """Same rule as BettaTypes#form (en = 0, de = 1)."""
    n = md.SHAPE_NAMES[shape][lang]
    if short:
        n = "Plakat" if shape == "round" else (f"{n} Plakat" if lang == 0 else f"{n}-Plakat")
    return f"{n} Dumbo" if dumbo else n


def fish_from_genome(g: dict, female=False) -> Fish:
    irid = {"none": 0.0, "low": 0.5, "high": 1.0}[g["iridescence"]]
    return Fish(shape=g["finform"], body=pal.hex_of(g["body_color"]), fins=pal.hex_of(g["fin_color"]),
                pattern=None if g["pattern"] == "none" else g["pattern"], pattern_colour=pal.hex_of(g["pattern_color"]),
                iridescence=irid, irid_colour=pal.hex_of(g["iridescence_color"]), dumbo=g["pectoral"] == "dumbo",
                short=g["fin_length"] == "short", female=female)


def random_fish(rng: np.random.Generator, shape: str) -> Fish:
    ids = [c[0] for c in pal.COLORS]
    body = ids[rng.integers(len(ids))]
    fins = body if rng.random() < 0.5 else ids[rng.integers(len(ids))]
    pattern = None if rng.random() < 0.22 else PATTERN_CHOICES[rng.integers(len(PATTERN_CHOICES))]
    pc = ids[rng.integers(len(ids))]
    while pc == body:
        pc = ids[rng.integers(len(ids))]
    irid = float(rng.choice([0.0, 0.5, 1.0], p=[0.45, 0.35, 0.2]))
    ic = pal.IRIDESCENT[rng.integers(len(pal.IRIDESCENT))]
    return Fish(shape=shape, body=pal.hex_of(body), fins=pal.hex_of(fins), pattern=pattern,
                pattern_colour=pal.hex_of(pc), iridescence=irid, irid_colour=pal.hex_of(ic),
                dumbo=bool(rng.random() < 0.15))


def _caption(f: Fish) -> tuple[str, str]:
    rev = {c[3]: c[0] for c in pal.COLORS}
    l1 = rev[f.body] if f.body == f.fins else f"{rev[f.body]}/{rev[f.fins]}"
    tags = [f.pattern or "solid"]
    if f.iridescence:
        tags.append("irid" + ("+" if f.iridescence > 0.5 else ""))
    if f.dumbo:
        tags.append("dumbo")
    if f.female:
        tags.append("female")
    return l1, " ".join(tags)


# ---------------------------------------------------------------- sheets

def sheet_shapes(cols=12, ppu=4) -> Image.Image:
    cell_w, cell_h = int(26 * ppu), int(16 * ppu)
    label_w, cap_h, pad = 170, 26, 6
    title_h = 34
    W = label_w + cols * (cell_w + pad) + pad
    H = title_h + len(vt.PREVIEW_FORMS) * (cell_h + cap_h + pad) + pad
    img = Image.new("RGBA", (W, H), BG)
    dr = ImageDraw.Draw(img)
    dr.text((10, 8), f"Bettacraft - {len(md.SHAPES)} tail shapes long-finned + plakat (short-finned) forms x {cols} random fish",
            fill=TEXT, font=font(16))
    f_small = font(10)
    for r, (shape, length) in enumerate(vt.PREVIEW_FORMS):
        rng = np.random.default_rng(1000 + r)
        females = set(rng.choice(cols, size=2, replace=False).tolist())
        y = title_h + r * (cell_h + cap_h + pad)
        dr.rectangle([0, y - 2, W, y + cell_h + cap_h], fill=BG2 if r % 2 else BG)
        short = length == "short"
        dr.text((10, y + cell_h // 2 - 12), form_name(shape, short), fill=TEXT, font=font(16))
        dr.text((10, y + cell_h // 2 + 8), form_name(shape, short, lang=1), fill=DIM, font=font(11))
        for c in range(cols):
            f = random_fish(rng, shape)
            f.short = short
            f.female = c in females
            x = label_w + pad + c * (cell_w + pad)
            img.alpha_composite(fish_image(f, ppu=ppu), (x, y))
            l1, l2 = _caption(f)
            dr.text((x + 2, y + cell_h), l1[:22], fill=TEXT, font=f_small)
            dr.text((x + 2, y + cell_h + 12), l2[:24], fill=DIM, font=f_small)
    return img


def sheet_types(ppu=6) -> Image.Image:
    """M5 review sheet: every named variety (its example fish), the forms and the patterns."""
    cell_w, cell_h, pad, cap = int(26 * ppu), int(16 * ppu), 10, 40
    cols = 4
    title_h = 40
    rows_v = (len(vt.VARIETIES) + cols - 1) // cols
    forms = vt.PREVIEW_FORMS + [("halfmoon", "dumbo")]
    small = int(26 * 4), int(16 * 4)
    fcols = 7
    rows_f = (len(forms) + fcols - 1) // fcols
    pats = [p for p in pt.PATTERNS if p != "bars"] + ["bars"]
    rows_p = (len(pats) + 1 + fcols - 1) // fcols
    W = max(cols * (cell_w + pad), fcols * (small[0] + 14)) + pad
    H = title_h + rows_v * (cell_h + cap + pad) + 40 + rows_f * (small[1] + 34) + 40 + rows_p * (small[1] + 34) + 10
    img = Image.new("RGBA", (W, H), BG)
    dr = ImageDraw.Draw(img)
    dr.text((10, 10), f"Bettacraft M5 - {len(vt.VARIETIES)} named varieties (example fish, English / German name)",
            fill=TEXT, font=font(18))
    for i, v in enumerate(vt.VARIETIES):
        r, c = divmod(i, cols)
        x, y = pad + c * (cell_w + pad), title_h + r * (cell_h + cap + pad)
        dr.rectangle([x - 4, y - 4, x + cell_w + 4, y + cell_h + cap - 4], fill=BG2)
        g = v[6]
        img.alpha_composite(fish_image(fish_from_genome(g), ppu=ppu), (x, y))
        form = form_name(g["finform"], g["fin_length"] == "short", g["pectoral"] == "dumbo")
        form_de = form_name(g["finform"], g["fin_length"] == "short", g["pectoral"] == "dumbo", 1)
        en = f"{v[4]} {form}" if v[2] else v[4]
        de = f"{form_de} ({v[5]})" if v[2] else v[5]
        dr.text((x + 2, y + cell_h + 2), en, fill=TEXT, font=font(13))
        dr.text((x + 2, y + cell_h + 18), de, fill=DIM, font=font(11))
    y = title_h + rows_v * (cell_h + cap + pad) + 8
    dr.text((10, y), "Forms: finform (tail) x fin length (long / short = plakat), dumbo pectorals", fill=TEXT, font=font(16))
    y += 28
    base = dict(vt.VARIETIES[5][6])  # mustard gas colours
    for i, (shape, length) in enumerate(forms):
        r, c = divmod(i, fcols)
        x, yy = pad + c * (small[0] + 14), y + r * (small[1] + 34)
        g = dict(base, finform=shape, fin_length="short" if length == "short" else "long",
                 pectoral="dumbo" if length == "dumbo" else "normal")
        img.alpha_composite(fish_image(fish_from_genome(g), ppu=4), (x, yy))
        dr.text((x + 2, yy + small[1] + 2), form_name(shape, length == "short", length == "dumbo"), fill=TEXT, font=font(11))
        dr.text((x + 2, yy + small[1] + 15), form_name(shape, length == "short", length == "dumbo", 1), fill=DIM, font=font(10))
    y += rows_f * (small[1] + 34) + 8
    dr.text((10, y), "Patterns (red halfmoon, white pattern colour; bars = female breeding colours)", fill=TEXT, font=font(16))
    y += 28
    for i, p in enumerate([None] + pats):
        r, c = divmod(i, fcols)
        x, yy = pad + c * (small[0] + 14), y + r * (small[1] + 34)
        f = Fish(shape="halfmoon", body="#c8202a", fins="#c8202a", pattern=p, pattern_colour="#f4f3ee",
                 female=p == "bars")
        if p == "bars":
            f.pattern_colour = "#281e1a"
            f.body = "#ef6d12"
            f.fins = "#ef6d12"
        img.alpha_composite(fish_image(f, ppu=4), (x, yy))
        dr.text((x + 2, yy + small[1] + 2), p or "solid", fill=TEXT, font=font(11))
    return img


def sheet_patterns(ppu=5) -> Image.Image:
    names = [None] + list(pt.PATTERNS)
    cell_w, cell_h, pad, label_w = int(26 * ppu), int(16 * ppu), 8, 130
    rows = [("halfmoon", "#1d3f8f", "#1d3f8f", "#f4f3ee"), ("crowntail", "#c8202a", "#c8202a", "#1c1b20"),
            ("round", "#f4d03f", "#f4d03f", "#13817f")]
    W = label_w + len(names) * (cell_w + pad) + pad
    H = 34 + len(rows) * (cell_h + 22 + pad)
    img = Image.new("RGBA", (W, H), BG)
    dr = ImageDraw.Draw(img)
    dr.text((10, 8), "Patterns (pattern_<name>.png tinted with the pattern colour)", fill=TEXT, font=font(16))
    for r, (shape, b, fcol, pc) in enumerate(rows):
        y = 34 + r * (cell_h + 22 + pad)
        dr.text((10, y + cell_h // 2 - 8), md.SHAPE_NAMES[shape][0], fill=TEXT, font=font(14))
        for c, name in enumerate(names):
            x = label_w + pad + c * (cell_w + pad)
            f = Fish(shape=shape, body=b, fins=fcol, pattern=name, pattern_colour=pc)
            img.alpha_composite(fish_image(f, ppu=ppu), (x, y))
            dr.text((x + 4, y + cell_h + 2), name or "solid", fill=TEXT, font=font(12))
    return img


def _egg_or_larva(stage: str, ppu: int, bounds) -> Image.Image:
    zmin, zmax, ymin, ymax = bounds
    W, H = int((zmax - zmin) * ppu), int((ymax - ymin) * ppu)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    dr = ImageDraw.Draw(img)
    cx, cy = int((0 - zmin) * ppu), int((20 - ymin) * ppu)
    if stage == "egg":
        # a few eggs in a foam nest (placeholder)
        for dx, dy in [(-14, -4), (-4, -8), (6, -3), (-8, 4), (4, 6), (14, 2)]:
            dr.ellipse([cx + dx - 9, cy + dy - 9, cx + dx + 9, cy + dy + 9], fill=(235, 245, 250, 90))
        for dx, dy in [(-6, -2), (2, 2), (-1, -6), (8, 0)]:
            dr.rectangle([cx + dx - 2, cy + dy - 2, cx + dx + 1, cy + dy + 1], fill=(240, 228, 190, 255))
    else:
        L = int(0.12 * 21 * ppu)
        dr.rectangle([cx - L // 2, cy - 1, cx + L // 2, cy + 1], fill=(215, 205, 190, 220))
        dr.rectangle([cx - L // 2 - 1, cy - 3, cx - L // 2 + 5, cy + 3], fill=(240, 210, 130, 255))
        dr.rectangle([cx - L // 2, cy - 2, cx - L // 2 + 1, cy - 1], fill=(20, 20, 20, 255))
    return img


def sheet_stages(ppu=8) -> Image.Image:
    bounds = (-9.0, 17.0, 12.0, 28.0)
    cell_w, cell_h, pad = int(26 * ppu), int(16 * ppu), 10
    W = len(md.LIFE_STAGES) * (cell_w + pad) + pad
    H = 34 + cell_h + 40
    img = Image.new("RGBA", (W, H), BG)
    dr = ImageDraw.Draw(img)
    dr.text((10, 8), "Life stages at relative size (egg/larva are placeholders; fry translucent, juvenile pale)",
            fill=TEXT, font=font(16))
    for i, st in enumerate(md.LIFE_STAGES):
        x = pad + i * (cell_w + pad)
        if st["model"] == "placeholder":
            im = _egg_or_larva(st["id"], ppu, bounds)
        else:
            f = Fish(shape="halfmoon", body="#c8202a", fins="#1f4fb5", pattern="butterfly",
                     pattern_colour="#f4f3ee", iridescence=0.5, irid_colour="#2ec4c4", stage=st["id"])
            im = fish_image(f, ppu=ppu, bounds=bounds, scale=st["scale"])
        img.alpha_composite(im, (x, 34))
        dr.text((x + 6, 34 + cell_h + 4), f'{st["id"]}  x{st["scale"]}', fill=TEXT, font=font(14))
    return img


def _tint(layer: np.ndarray, hexc: str) -> Image.Image:
    o = layer.astype(np.float64).copy()
    o[..., :3] *= hex_f(hexc)
    return Image.fromarray(np.clip(np.rint(o), 0, 255).astype(np.uint8), "RGBA")


def _checker(size, n=8):
    a = np.zeros((size, size, 4), np.uint8)
    yy, xx = np.mgrid[0:size, 0:size] // n
    a[..., :3] = np.where(((xx + yy) % 2 == 0)[..., None], 150, 128)
    a[..., 3] = 255
    return Image.fromarray(a, "RGBA")


def sheet_icons(scale=8) -> Image.Image:
    ic = icons.all_icons()
    bl, _ = blocks.all_blocks()
    water = bl["tank_water_still"]
    tiles: list[tuple[str, Image.Image]] = []
    for k in ["betta_bucket", "betta_bucket_fish", "betta_bucket_fish_fins", "fish_bag", "fish_bag_fish",
              "fish_bag_fish_fins", "betta_pellets", "mosquito_larvae", "heater_small", "heater_medium", "heater_large"]:
        tiles.append((k, Image.fromarray(ic[k], "RGBA")))
    for k in ["tank_edge", "tank_frame", "interior_floor", "interior_wall"]:
        tiles.append((k, Image.fromarray(bl[k], "RGBA")))
    w0 = Image.fromarray(water[:16], "RGBA")
    tiles.append(("tank_water_still f0", w0))
    tiles.append(("water x tint", _tint(water[:16], "#3f76e4")))
    combos = [("#c8202a", "#1f4fb5"), ("#f4f3ee", "#c8202a"), ("#1f4fb5", "#1f4fb5"), ("#ef6d12", "#f4d03f"),
              ("#1c1b20", "#2ec4c4"), ("#ef8fb8", "#b49bd8")]
    tinted = []
    for base in ["betta_bucket", "fish_bag"]:
        for b, f in combos:
            im = Image.fromarray(ic[base], "RGBA").copy()
            im.alpha_composite(_tint(ic[base + "_fish_fins"], f))
            im.alpha_composite(_tint(ic[base + "_fish"], b))
            tinted.append(im)
    T = 16 * scale
    pad, label_h, title_h = 12, 30, 34
    per_row = 9
    rows1 = (len(tiles) + per_row - 1) // per_row
    W = pad + per_row * (T + pad)
    H = title_h + rows1 * (T + label_h + pad) + 30 + 2 * (T + pad) + 8 * (16 * 4) // 4 + pad
    img = Image.new("RGBA", (W, H), BG)
    dr = ImageDraw.Draw(img)
    dr.text((10, 8), "Item + block textures at 8x (checker = transparency)", fill=TEXT, font=font(16))
    for i, (name, t) in enumerate(tiles):
        r, c = divmod(i, per_row)
        x, y = pad + c * (T + pad), title_h + r * (T + label_h + pad)
        img.alpha_composite(_checker(T), (x, y))
        img.alpha_composite(t.resize((T, T), Image.NEAREST), (x, y))
        dr.text((x, y + T + 3), name, fill=TEXT, font=font(11))
    y0 = title_h + rows1 * (T + label_h + pad)
    dr.text((10, y0), "Bucket / bag with runtime-tinted fish overlays (tint 1 = body, tint 2 = fins)",
            fill=TEXT, font=font(14))
    y0 += 24
    for i, t in enumerate(tinted):
        r, c = divmod(i, 6)
        x, y = pad + c * (T + pad), y0 + r * (T + pad)
        img.alpha_composite(_checker(T), (x, y))
        img.alpha_composite(t.resize((T, T), Image.NEAREST), (x, y))
    # water animation frames (every 4th) at 4x, tinted
    y1 = y0 + 2 * (T + pad)
    for i in range(8):
        fr = _tint(water[16 * 4 * i:16 * 4 * i + 16], "#3f76e4").resize((64, 64), Image.NEAREST)
        img.alpha_composite(fr, (pad + 6 * (T + pad) + (i % 4) * 68, y0 + (i // 4) * 68))
    dr.text((pad + 6 * (T + pad), y0 + 2 * 68 + 2), "water frames 0,4,8..28 (tinted)", fill=DIM, font=font(11))
    return img.crop((0, 0, W, y1))


# ---------------------------------------------------------------- logo

FONT5 = {
    "B": ["1110", "1001", "1001", "1110", "1001", "1001", "1110"],
    "E": ["1111", "1000", "1000", "1110", "1000", "1000", "1111"],
    "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    "A": ["0110", "1001", "1001", "1111", "1001", "1001", "1001"],
    "C": ["0111", "1000", "1000", "1000", "1000", "1000", "0111"],
    "R": ["1110", "1001", "1001", "1110", "1010", "1001", "1001"],
    "F": ["1111", "1000", "1000", "1110", "1000", "1000", "1000"],
}


def pixel_text(text: str, px: int, colour, shadow) -> Image.Image:
    glyphs = [FONT5[ch] for ch in text]
    w = sum(len(g[0]) + 1 for g in glyphs) - 1
    img = Image.new("RGBA", ((w + 1) * px, 8 * px), (0, 0, 0, 0))
    dr = ImageDraw.Draw(img)
    for pass_, col, off in ((0, shadow, 1), (1, colour, 0)):
        x = 0
        for g in glyphs:
            for gy, row in enumerate(g):
                for gx, bit in enumerate(row):
                    if bit == "1":
                        X, Y = (x + gx + off) * px, (gy + off) * px
                        dr.rectangle([X, Y, X + px - 1, Y + px - 1], fill=col)
            x += len(g[0]) + 1
    return img


def make_logo(size=256) -> Image.Image:
    # background: water gradient, rounded, a few bubbles
    top, bot = np.array([62, 156, 196]), np.array([18, 52, 92])
    bg = np.zeros((size, size, 4))
    t = np.linspace(0, 1, size)[:, None]
    bg[..., :3] = ((top * (1 - t)[..., None] + bot * t[..., None]) / 255.0).repeat(size, 1).reshape(size, size, 3)
    bg[..., 3] = 1
    # quantise to bands for a pixel-art look
    bg[..., :3] = np.floor(bg[..., :3] * 24) / 24
    img = to_image(bg)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1], radius=size // 8, fill=255)
    img.putalpha(mask)
    dr = ImageDraw.Draw(img)
    rng = np.random.default_rng(77)
    for _ in range(9):
        r = int(rng.integers(3, 8))
        x, y = int(rng.integers(20, size - 20)), int(rng.integers(16, size // 2))
        dr.ellipse([x - r, y - r, x + r, y + r], outline=(200, 235, 250, 160), width=2)
        dr.rectangle([x - r // 2, y - r // 2, x - r // 2 + 1, y - r // 2 + 1], fill=(240, 250, 255, 200))
    # sand strip
    for x in range(0, size, 4):
        h = 10 + int(4 * np.sin(x / 19.0) + 3 * np.sin(x / 7.3))
        dr.rectangle([x, size - h, x + 3, size], fill=(212, 190, 140, 255))
    img.putalpha(Image.fromarray(np.minimum(np.asarray(img)[..., 3], np.asarray(mask)), "L"))
    f = Fish(shape="halfmoon", body="#1f4fb5", fins="#c8202a", pattern="butterfly", pattern_colour="#f4f3ee",
             iridescence=1.0, irid_colour="#2ec4c4")
    ppu = 9
    fish = fish_image(f, ppu=ppu, bounds=(-8.5, 14.5, 12.0, 28.0))
    img.alpha_composite(fish, ((size - fish.width) // 2, 18))
    title = pixel_text("BETTACRAFT", 4, (250, 250, 245, 255), (20, 34, 60, 255))
    img.alpha_composite(title, ((size - title.width) // 2, size - title.height - 22))
    return img


def make_banner(w=512, h=128) -> Image.Image:
    """The wide logo (4:1) for the NeoForge mod list (logoFile) and the top of the mod page: the water, bubbles, sand and
    halfmoon betta of the square icon, the betta on the left, the title on the right, a few plant stalks."""
    top, bot = np.array([62, 156, 196]), np.array([18, 52, 92])
    t = np.linspace(0, 1, h)[:, None, None]
    bg = np.ones((h, w, 4))
    bg[..., :3] = np.broadcast_to((top * (1 - t) + bot * t) / 255.0, (h, w, 3))
    bg[..., :3] = np.floor(bg[..., :3] * 24) / 24
    img = to_image(bg)
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, w - 1, h - 1], radius=h // 8, fill=255)
    over = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    dr = ImageDraw.Draw(over)
    # light shafts from the surface (pixel bands)
    for x0 in (150, 260, 400):
        for y in range(0, h - 20, 4):
            sx = x0 + y // 3
            dr.rectangle([sx, y, sx + 15, y + 3], fill=(150, 215, 240, 28))
    rng = np.random.default_rng(78)
    for _ in range(14):
        r = int(rng.integers(2, 6))
        x, y = int(rng.integers(12, w - 12)), int(rng.integers(8, h - 40))
        dr.ellipse([x - r, y - r, x + r, y + r], outline=(200, 235, 250, 150), width=2)
        dr.rectangle([x - r // 2, y - r // 2, x - r // 2 + 1, y - r // 2 + 1], fill=(240, 250, 255, 200))
    img.alpha_composite(over)
    dr = ImageDraw.Draw(img)
    # plant stalks (pixel columns with leaves), behind the sand
    for x, hh, col in ((22, 70, (46, 140, 64, 255)), (36, 52, (70, 168, 80, 255)), (486, 76, (46, 140, 64, 255)),
                       (470, 50, (70, 168, 80, 255)), (140, 40, (58, 150, 70, 255))):
        for y in range(h - 12 - hh, h - 8, 4):
            sway = int(2 * np.sin(y / 9.0 + x))
            dr.rectangle([x + sway, y, x + sway + 3, y + 3], fill=col)
            if (y // 4) % 3 == 0:
                side = 4 if (y // 12) % 2 == 0 else -8
                dr.rectangle([x + sway + side, y, x + sway + side + 7, y + 3], fill=col)
    for x in range(0, w, 4):
        sh = 9 + int(3 * np.sin(x / 19.0) + 2 * np.sin(x / 7.3))
        dr.rectangle([x, h - sh, x + 3, h], fill=(212, 190, 140, 255))
        if (x // 4) % 7 == 0:
            dr.rectangle([x, h - sh, x + 3, h - sh + 3], fill=(232, 214, 170, 255))
    f = Fish(shape="halfmoon", body="#1f4fb5", fins="#c8202a", pattern="butterfly", pattern_colour="#f4f3ee",
             iridescence=1.0, irid_colour="#2ec4c4")
    fish = fish_image(f, ppu=5, bounds=(-8.5, 14.5, 12.0, 28.0))
    fx = 40
    img.alpha_composite(fish, (fx, (h - fish.height) // 2 - 6))
    title = pixel_text("BETTACRAFT", 6, (250, 250, 245, 255), (20, 34, 60, 255))
    tx = fx + fish.width + (w - 24 - fx - fish.width - title.width) // 2
    img.alpha_composite(title, (tx, (h - title.height) // 2 - 8))
    img.putalpha(Image.fromarray(np.minimum(np.asarray(img)[..., 3], np.asarray(mask)), "L"))
    return img


# ---------------------------------------------------------------- bbmodel sample textures

def sample_textures() -> dict[str, bytes]:
    out = {}
    for s in md.SHAPES:
        tex = composite_texture(Fish(shape=s, body="#1f4fb5", fins="#c8202a", pattern="butterfly",
                                     pattern_colour="#f4f3ee", iridescence=0.5, irid_colour="#2ec4c4"))
        buf = io.BytesIO()
        to_image(tex).save(buf, "PNG")
        out[s] = buf.getvalue()
    return out


def combined(shapes, patterns, stages, icon_sheet) -> Image.Image:
    parts = [shapes, patterns, stages, icon_sheet]
    W = max(p.width for p in parts)
    H = sum(p.height for p in parts) + 6 * (len(parts) - 1)
    out = Image.new("RGBA", (W, H), (20, 32, 42, 255))
    y = 0
    # second row: patterns, stages and icons side by side if they fit, else stacked
    out.alpha_composite(shapes, (0, 0))
    y = shapes.height + 6
    if patterns.width + icon_sheet.width + 6 <= W:
        out.alpha_composite(patterns, (0, y))
        out.alpha_composite(stages, (0, y + patterns.height + 6))
        out.alpha_composite(icon_sheet, (patterns.width + 6 if stages.width <= patterns.width else max(patterns.width, stages.width) + 6, y))
        h = max(patterns.height + 6 + stages.height, icon_sheet.height)
        return out.crop((0, 0, W, y + h))
    for p in parts[1:]:
        out.alpha_composite(p, (0, y))
        y += p.height + 6
    return out.crop((0, 0, W, y - 6))


def write_banner() -> list:
    """The wide logo: src/main/resources/bettacraft_logo.png (logoFile of neoforge.mods.toml) and a 2x copy for the mod
    page (docs/images/bettacraft-banner.png)."""
    banner = make_banner()
    docs = ROOT / "docs" / "images" / "bettacraft-banner.png"
    return [save_png(banner, LOGO.with_name("bettacraft_logo.png")),
            save_png(banner.resize((banner.width * 2, banner.height * 2), Image.NEAREST), docs)]


def write_all() -> list:
    PREVIEW.mkdir(parents=True, exist_ok=True)
    shapes = sheet_shapes()
    patterns = sheet_patterns()
    stages = sheet_stages()
    icon_sheet = sheet_icons()
    out = [save_png(shapes, PREVIEW / "preview_shapes.png"), save_png(patterns, PREVIEW / "preview_patterns.png"),
           save_png(stages, PREVIEW / "preview_stages.png"), save_png(icon_sheet, PREVIEW / "preview_icons.png")]
    out.append(save_png(make_logo(), LOGO))
    out.append(save_png(make_logo(), PREVIEW / "logo.png"))
    out += write_banner()
    comb = combined(shapes, patterns, stages, icon_sheet)
    REVIEW_DIR.mkdir(parents=True, exist_ok=True)
    out.append(save_png(comb, PREVIEW / "preview.png"))
    types = sheet_types()
    out.append(save_png(types, PREVIEW / "types.png"))
    shutil.copyfile(PREVIEW / "types.png", REVIEW_DIR / "m5-types.png")
    out.append(REVIEW_DIR / "m5-types.png")
    return out


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
