"""M6: handbook images from the screenshots of the dev client smoke test.

Run ``gradlew runClient -Psmoketestworld`` first (window 1280x960, menus at GUI scale 4); it writes ``run/screenshots/
guide_*.png`` (world shots without HUD and the tank menu tabs, the menus also in German as ``*_de.png``). This script crops
them into ``assets/bettacraft/guides/bettacraft/guide/images/`` (German menus into ``_de_de/images/``). GuideME draws
images at a quarter of their pixel size and shrinks them to the page width, so the menu crops at GUI scale 4 show up
exactly at GUI size.

The interior and hologram images are kept as they are (from the M4/M5 screenshots). M9 adds the water care scene, the
live food breeders and the water tab from the ``bettacraft_m9_*`` screenshots of the same run, M11 the tank styles and the
mirror stick from ``bettacraft_m11_*`` (also made by ``-Psmoketestm11`` alone).
"""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

from common import RES, ROOT

SHOTS = ROOT / "run" / "screenshots"
GUIDE = RES / "assets" / "bettacraft" / "guides" / "bettacraft" / "guide"

# name -> crop box (left, top, right, bottom) in the 1280x960 screenshot
WORLD = {
    "tank": (120, 180, 1140, 800),
    "nest": (150, 110, 1230, 700),
    "stages": (0, 220, 1280, 680),
    "forms": (30, 270, 1250, 690),
    "varieties": (20, 300, 1260, 660),
    "cabinets": (0, 330, 1280, 780),
    "external_filter": (360, 350, 1220, 760),
}
# GUI scale 4: the 248x230 tank panel sits at (144, 20); tabs and the content area are its upper 146 rows
MENU_BOX = (144, 20, 144 + 248 * 4, 20 + 146 * 4)
MENUS = {"menu_overview": "guide_menu_overview", "menu_inhabitants": "guide_menu_inhabitants", "menu_breeding": "guide_menu_breeding"}
# M9 scene (DevClientSmokeM9): world shots without HUD and the water tab (English only; German pages fall back to it)
M9_WORLD = {"water_care": ("bettacraft_m9_scene", (0, 240, 1140, 620)), "live_food": ("bettacraft_m9_breeders", (0, 100, 1060, 520))}
M9_MENUS = {"menu_water": "bettacraft_m9_menu_water"}
# M11 scene (DevClientSmokeM11): the three tank styles and a male flaring at the mirror stick (world shots without HUD)
M11_WORLD = {"tank_styles": ("bettacraft_m11_styles", (60, 320, 1220, 570)), "mirror_stick": ("bettacraft_m11_mirror_side", (130, 250, 1060, 740))}


def save(im: Image.Image, path: Path) -> None:
    """256-colour PNG (screenshots of blocky scenes lose nothing visible, the jar stays small)."""
    im.convert("RGB").quantize(256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).save(path, optimize=True)
    print("wrote", path)


def main() -> int:
    if not SHOTS.is_dir():
        print("no screenshots in", SHOTS, "- run gradlew runClient -Psmoketestworld first")
        return 1
    out_en = GUIDE / "images"
    out_de = GUIDE / "_de_de" / "images"
    out_en.mkdir(parents=True, exist_ok=True)
    out_de.mkdir(parents=True, exist_ok=True)
    for name, box in WORLD.items():
        src = SHOTS / f"guide_{name}.png"
        if not src.exists():
            print("missing", src)
            continue
        save(Image.open(src).convert("RGB").crop(box), out_en / f"{name}.png")
    for name, shot in MENUS.items():
        for suffix, out in (("", out_en), ("_de", out_de)):
            src = SHOTS / f"{shot}{suffix}.png"
            if not src.exists():
                print("missing", src)
                continue
            save(Image.open(src).convert("RGB").crop(MENU_BOX), out / f"{name}.png")
    for name, (shot, box) in M9_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out_en / f"{name}.png")
        else:
            print("missing", src)
    for name, (shot, box) in M11_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out_en / f"{name}.png")
        else:
            print("missing", src)
    for name, shot in M9_MENUS.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(MENU_BOX), out_en / f"{name}.png")
        else:
            print("missing", src)
    for kept in ("interior", "hologram"):
        p = out_en / f"{kept}.png"
        if p.exists() and Image.open(p).mode != "P":
            save(Image.open(p), p)
    return 0


if __name__ == "__main__":
    sys.exit(main())
