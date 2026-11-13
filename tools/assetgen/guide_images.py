"""M6: handbook images from the screenshots of the dev client smoke test.

Run ``gradlew runClient -Psmoketestworld`` first (window 1280x960, menus at GUI scale 4); it writes ``run/screenshots/
guide_*.png`` (world shots without HUD and the tank menu tabs, the menus also in German as ``*_de.png``). This script crops
them into ``assets/bettacraft/guides/bettacraft/guide/images/`` (German menus into ``_de_de/images/``). GuideME draws
images at a quarter of their pixel size and shrinks them to the page width, so the menu crops at GUI scale 4 show up
exactly at GUI size.

The interior and hologram images are kept as they are (from the M4/M5 screenshots). M9 adds the water care scene, the
live food breeders and the water tab from the ``bettacraft_m9_*`` screenshots of the same run, M11 the tank styles and the
mirror stick from ``bettacraft_m11_*`` (also made by ``-Psmoketestm11`` alone), M12 the floating plants from
``bettacraft_m12_*`` (``-Psmoketestm12``; the plants and water tabs in English and German at GUI scale 4, the smoke test
switches the language itself). ``py guide_images.py m12`` crops only the M12 images.
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
M11_WORLD = {"tank_styles": ("bettacraft_m11_styles", (60, 320, 1220, 570)), "mirror_stick": ("bettacraft_m11_mirror_side", (210, 260, 1070, 730))}

# M12 scene (DevClientSmokeM12): the main plant tank from above, the roots through the glass, the plants page
M12_WORLD = {"floating_plants": ("bettacraft_m12_overview", (230, 240, 1050, 660)),
             "floating_plants_roots": ("bettacraft_m12_roots", (0, 330, 1280, 640))}
# the plants tab and the water tab at GUI scale 4, English and German (DevClientSmokeM12 switches the language itself)
M12_MENUS = {"menu_plants": ("bettacraft_m12_menu_tabs_en_gui4", "bettacraft_m12_menu_tabs_de_gui4"),
             "menu_water": ("bettacraft_m12_menu_water_en_gui4", "bettacraft_m12_menu_water_de_gui4")}


def m12(out_en: Path, out_de: Path) -> None:
    for name, (shot, box) in M12_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out_en / f"{name}.png")
        else:
            print("missing", src)
    for name, shots in M12_MENUS.items():
        for shot, out in zip(shots, (out_en, out_de)):
            src = SHOTS / f"{shot}.png"
            if src.exists():
                save(Image.open(src).convert("RGB").crop(MENU_BOX), out / f"{name}.png")
            else:
                print("missing", src)


# M13 scene (DevClientSmokeM13, -Psmoketestm13 -PnoTop -PnoWthit): the planted tank, inside it, red plants in strong and
# weak light (side by side). ``py guide_images.py m13`` crops only these.
M13_WORLD = {"aquatic_plants": ("bettacraft_m13_overview", (240, 330, 1045, 690)),
             "aquatic_plants_interior": ("bettacraft_m13_interior", (0, 100, 1280, 900))}


def m13(out_en: Path) -> None:
    for name, (shot, box) in M13_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out_en / f"{name}.png")
        else:
            print("missing", src)
    strong, weak = SHOTS / "bettacraft_m13_stems.png", SHOTS / "bettacraft_m13_weak_light.png"
    if strong.exists() and weak.exists():
        a = Image.open(strong).convert("RGB").crop((370, 290, 1280, 800))
        b = Image.open(weak).convert("RGB").crop((230, 370, 1060, 640))
        h = 360
        a = a.resize((round(a.width * h / a.height), h), Image.LANCZOS)
        b = b.resize((round(b.width * h / b.height), h), Image.LANCZOS)
        im = Image.new("RGB", (a.width + b.width + 8, h), (24, 24, 24))
        im.paste(a, (0, 0))
        im.paste(b, (a.width + 8, 0))
        save(im, out_en / "aquatic_plants_red.png")
    else:
        print("missing", strong, weak)


# M14 scene (DevClientSmokeM14, -Psmoketestm14 -PnoTop -PnoWthit): the colony tank, close-ups, the frog, algae before
# and after (side by side) and the small animals page of the menu (English, German). ``py guide_images.py m14``.
M14_WORLD = {"critters_tank": ("bettacraft_m14_overview", (230, 300, 1050, 690)),
             "critters_closeup": ("bettacraft_m14_closeup", (0, 230, 1280, 740)),
             "critters_snails": ("bettacraft_m14_snails", (60, 80, 1280, 940)),
             "critters_frog": ("bettacraft_m14_frog", (0, 220, 1150, 900))}
M14_MENUS = {"menu_critters": ("bettacraft_m14_menu_critters", "bettacraft_m14_menu_critters_de")}


def m14(out_en: Path, out_de: Path) -> None:
    for name, (shot, box) in M14_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out_en / f"{name}.png")
        else:
            print("missing", src)
    before, after = SHOTS / "bettacraft_m14_algae_before.png", SHOTS / "bettacraft_m14_algae_after.png"
    if before.exists() and after.exists():
        a = Image.open(before).convert("RGB").crop((205, 300, 1075, 600))
        b = Image.open(after).convert("RGB").crop((205, 300, 1075, 600))
        im = Image.new("RGB", (a.width, a.height * 2 + 8), (24, 24, 24))
        im.paste(a, (0, 0))
        im.paste(b, (0, a.height + 8))
        save(im, out_en / "critters_algae.png")
    else:
        print("missing", before, after)
    for name, shots in M14_MENUS.items():
        for shot, out in zip(shots, (out_en, out_de)):
            src = SHOTS / f"{shot}.png"
            if src.exists():
                save(Image.open(src).convert("RGB").crop(MENU_BOX), out / f"{name}.png")
            else:
                print("missing", src)


# M15 scene (DevClientSmokeM15, -Psmoketestm15 -PnoTop -PnoWthit): the domed bubble nest from the side through the glass
# and the leave hint at the bottom left of the interior (English, German). ``py guide_images.py m15``.
M15_WORLD = {"nest_side": ("bettacraft_m15_nest_side", (240, 300, 1040, 560))}
M15_HINT = ("bettacraft_m15_interior_hint", "bettacraft_m15_interior_hint_de")
HINT_BOX = (0, 700, 760, 960)


def m15(out_en: Path, out_de: Path) -> None:
    for name, (shot, box) in M15_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out_en / f"{name}.png")
        else:
            print("missing", src)
    for shot, out in zip(M15_HINT, (out_en, out_de)):
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(HINT_BOX), out / "interior_leave_hint.png")
        else:
            print("missing", src)


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
    if sys.argv[1:] == ["m12"]:
        m12(out_en, out_de)
        return 0
    if sys.argv[1:] == ["m13"]:
        m13(out_en)
        return 0
    if sys.argv[1:] == ["m14"]:
        m14(out_en, out_de)
        return 0
    if sys.argv[1:] == ["m15"]:
        m15(out_en, out_de)
        return 0
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
    m12(out_en, out_de)
    m13(out_en)
    m14(out_en, out_de)
    m15(out_en, out_de)
    for kept in ("interior", "hologram"):
        p = out_en / f"{kept}.png"
        if p.exists() and Image.open(p).mode != "P":
            save(Image.open(p), p)
    return 0


if __name__ == "__main__":
    sys.exit(main())
