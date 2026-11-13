"""M21: comparison sheet of the bucket variants A, B, C in a JEI-like grid (every fish bucket of the mod, tinted like in
the game). Writes ``builds/m21-buckets_A.png``, ``_B``, ``_C`` and ``builds/m21-buckets.png`` (all three side by side).
Run from the repo root:  py tools/assetgen/bucket_grid.py
"""
from __future__ import annotations

import re
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

import buckets

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "builds"
SPECIES = ROOT / "src/main/java/org/overengineer/bettacraft/tankmates/TankmateSpecies.java"
SCALE = 3
SLOT = 18 * SCALE
COLS = 9

# a few betta colours (body, fins) like the creative tab shows them
BETTAS = [(0xC81E1E, 0xE02828), (0x1E3CC8, 0x2850E8), (0x202024, 0x8A1010), (0xF0EEE6, 0xF4F0E8), (0xE8C020, 0xF0D040),
          (0x2A8C5A, 0x3AA070), (0xE87830, 0xF0A050)]


def tankmates() -> list[tuple[int, int]]:
    src = SPECIES.read_text(encoding="utf-8")
    return [(int(a, 16), int(b, 16)) for a, b in re.findall(r"\.egg\(0x([0-9A-Fa-f]{6}), 0x([0-9A-Fa-f]{6})\)", src)]


def tint(img: np.ndarray, rgb: int) -> np.ndarray:
    out = img.astype(np.float32)
    c = np.array([(rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255], np.float32) / 255
    out[..., :3] *= c
    return out.round().astype(np.uint8)


def icon(v: dict, body: int, fins: int) -> np.ndarray:
    img = v["betta_bucket"].copy()
    for layer in (tint(v["betta_bucket_fish"], body), tint(v["betta_bucket_fish_fins"], fins), v["betta_bucket_glass"]):
        img = buckets._over(img, layer)
    return img


def sheet(name: str) -> Image.Image:
    v = buckets.variant(name)
    icons = [icon(v, b, f) for b, f in BETTAS + tankmates()] + [v["dwarf_frog_bucket"]]
    rows = (len(icons) + COLS - 1) // COLS
    pad = 8
    head = 22
    w, h = COLS * SLOT + 2 * pad, rows * SLOT + 2 * pad + head
    im = Image.new("RGBA", (w, h), (198, 198, 198, 255))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, w - 1, h - 1], outline=(85, 85, 85, 255), width=2)
    d.text((pad, 6), f"Variante {name}" + (" (aktiv)" if name == buckets.ACTIVE else ""), fill=(64, 64, 64, 255))
    for i, ic in enumerate(icons):
        x, y = pad + (i % COLS) * SLOT, head + pad + (i // COLS) * SLOT
        # JEI slot: dark top-left, light bottom-right, grey inside
        d.rectangle([x, y, x + SLOT - 1, y + SLOT - 1], fill=(139, 139, 139, 255))
        d.rectangle([x, y, x + SLOT - 1, y + SCALE - 1], fill=(55, 55, 55, 255))
        d.rectangle([x, y, x + SCALE - 1, y + SLOT - 1], fill=(55, 55, 55, 255))
        d.rectangle([x, y + SLOT - SCALE, x + SLOT - 1, y + SLOT - 1], fill=(255, 255, 255, 255))
        d.rectangle([x + SLOT - SCALE, y, x + SLOT - 1, y + SLOT - 1], fill=(255, 255, 255, 255))
        big = Image.fromarray(ic, "RGBA").resize((16 * SCALE, 16 * SCALE), Image.NEAREST)
        im.alpha_composite(big, (x + SCALE, y + SCALE))
    return im


def main() -> None:
    OUT.mkdir(exist_ok=True)
    sheets = {n: sheet(n) for n in ("A", "B", "C")}
    for n, im in sheets.items():
        im.save(OUT / f"m21-buckets_{n}.png")
    gap = 12
    w = sum(im.width for im in sheets.values()) + gap * 2
    h = max(im.height for im in sheets.values())
    all3 = Image.new("RGBA", (w, h), (40, 40, 40, 255))
    x = 0
    for im in sheets.values():
        all3.alpha_composite(im, (x, 0))
        x += im.width + gap
    all3.save(OUT / "m21-buckets.png")
    print("wrote", OUT / "m21-buckets.png")


if __name__ == "__main__":
    main()
