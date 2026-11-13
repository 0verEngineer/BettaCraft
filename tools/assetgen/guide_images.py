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


# M16 scene (DevClientSmokeM16, -Psmoketestm16 -Paquatic -PnoTop -PnoWthit): a tank with water animals of other mods and
# a close-up. ``py guide_images.py m16``.
M16_WORLD = {"foreign_animals": ("bettacraft_m16_mixed_tank", (170, 215, 1110, 595)),
             "foreign_animals_closeup": ("bettacraft_m16_mixed_left", (0, 120, 1280, 760))}


def m16(out_en: Path) -> None:
    for name, (shot, box) in M16_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out_en / f"{name}.png")
        else:
            print("missing", src)


# M18 scene (DevClientSmokeM18, -Psmoketestm18 -PnoJade -PnoTop -PnoWthit): six aquariums under coloured lights at night,
# the 16 lights in the creative tab, the stocking tooltip of a tank with young bettas (en, de); with a copy of the row
# from a shader run (Complementary "Colored Lighting") as bettacraft_m18_shader_row_night.png also that.
# ``py guide_images.py m18``.
M18_WORLD = {"light_colors": ("bettacraft_m18_row_night", (60, 380, 1180, 540)),
             "light_items": ("bettacraft_m18_items", (270, 270, 1045, 640)),
             "light_shader": ("bettacraft_m18_shader_row_night", (60, 380, 1180, 560))}
M18_MENUS = {"menu_stocking": ("bettacraft_m18_menu_stocking", (150, 20, 1245, 740))}


def m18(out_en: Path, out_de: Path) -> None:
    for name, (shot, box) in M18_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out_en / f"{name}.png")
        else:
            print("missing", src)
    for name, (shot, box) in M18_MENUS.items():
        for suffix, out in (("", out_en), ("_de", out_de)):
            src = SHOTS / f"{shot}{suffix}.png"
            if src.exists():
                save(Image.open(src).convert("RGB").crop(box), out / f"{name}.png")
            else:
                print("missing", src)


# M19 scene (DevClientSmokeM19, -Psmoketestm19 -PnoJade -PnoTop -PnoWthit): a close-up of every tankmate species, the
# community tank, the breeding (eggs on the glass, a cave, the breeding box, a foam nest) and the menus (en, de).
# ``py guide_images.py m19``.
M19_SPECIES = ["neon_tetra", "cardinal_tetra", "ember_tetra", "glowlight_tetra", "harlequin_rasbora", "mosquito_rasbora", "pygmy_cory",
               "panda_cory", "sterbai_cory", "green_gold_cory", "leopard_frog_pleco", "zebra_pleco", "bristlenose_pleco", "otocinclus",
               "kuhli_loach", "clown_loach", "guppy", "endler_guppy", "platy", "dwarf_gourami"]
M19_BOTTOM = {"pygmy_cory", "panda_cory", "sterbai_cory", "green_gold_cory", "leopard_frog_pleco", "zebra_pleco", "bristlenose_pleco",
              "otocinclus", "kuhli_loach"}
M19_WORLD = {"tm_community": ("bettacraft_m19_community", (250, 150, 1040, 620)),
             "tm_care": ("bettacraft_m19_community_close", (0, 170, 1280, 830))}
M19_BREEDING = [("bettacraft_m19_eggs_glass", (300, 250, 980, 760)), ("bettacraft_m19_cave", (300, 250, 980, 760)),
                ("bettacraft_m19_breeding_box", (300, 120, 980, 630)), ("bettacraft_m19_foam_nest", (300, 450, 980, 960))]
M19_MENUS = {"menu_tankmates": ("bettacraft_m19_menu_animals", MENU_BOX), "menu_tankmate_breeding": ("bettacraft_m19_menu_breeding", MENU_BOX)}


def m19(out_en: Path, out_de: Path) -> None:
    for sp in M19_SPECIES:
        src = SHOTS / f"bettacraft_m19_tm_{sp}.png"
        if src.exists():
            # bottom fish: the camera looks down at them from just above the sand (they sit higher in the picture)
            box = (160, 40, 1120, 680) if sp in M19_BOTTOM else (160, 160, 1120, 800)
            save(Image.open(src).convert("RGB").crop(box).resize((720, 480), Image.LANCZOS), out_en / f"tm_{sp}.png")
        else:
            print("missing", src)
    for name, (shot, box) in M19_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out_en / f"{name}.png")
        else:
            print("missing", src)
    parts = []
    for shot, box in M19_BREEDING:
        src = SHOTS / f"{shot}.png"
        if src.exists():
            parts.append(Image.open(src).convert("RGB").crop(box).resize((340, 255), Image.LANCZOS))
    if len(parts) == 4:
        strip = Image.new("RGB", (340 * 4, 255))
        for i, p in enumerate(parts):
            strip.paste(p, (340 * i, 0))
        save(strip, out_en / "tm_breeding.png")
    for name, (shot, box) in M19_MENUS.items():
        for suffix, out in (("", out_en), ("_de", out_de)):
            src = SHOTS / f"{shot}{suffix}.png"
            if src.exists():
                save(Image.open(src).convert("RGB").crop(box), out / f"{name}.png")
            else:
                print("missing", src)


# M20 scene (DevClientSmokeM20, -Psmoketestm20 -PnoJade -PnoTop -PnoWthit): the reef tank, its corals and clownfish, the
# skimmer and the lights, the water colours, the coral colours and the water tab of the menu. ``py guide_images.py m20``.
M20_WORLD = {"reef_tank": ("bettacraft_m20_reef_tank", (250, 150, 1040, 620)),
             "reef_clownfish": ("bettacraft_m20_reef_clownfish", (0, 120, 1280, 760)),
             "reef_corals": ("bettacraft_m20_reef_corals", (120, 60, 1280, 740)),
             "reef_technique": ("bettacraft_m20_reef_technique", (80, 40, 960, 660)),
             "reef_water_colors": ("bettacraft_m20_water_colors", (100, 160, 1180, 640)),
             "reef_coral_colors": ("bettacraft_m20_coral_colors", (40, 300, 1240, 560))}
M20_MENUS = {"menu_reef_water": ("bettacraft_m20_menu_water", MENU_BOX)}


def m20(out_en: Path, out_de: Path) -> None:
    for name, (shot, box) in M20_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out_en / f"{name}.png")
        else:
            print("missing", src)
    for name, (shot, box) in M20_MENUS.items():
        for suffix, out in (("", out_en), ("_de", out_de)):
            src = SHOTS / f"{shot}{suffix}.png"
            if src.exists():
                save(Image.open(src).convert("RGB").crop(box), out / f"{name}.png")
            else:
                print("missing", src)


# M21 scene (DevClientSmokeM21, -Psmoketestm21): the plant prototype inside the interior. ``py guide_images.py m21``.
M21_WORLD = {"plants_mesh": ("bettacraft_m21_plants_new_wide", (0, 60, 1280, 900))}


def m21(out_en: Path) -> None:
    for name, (shot, box) in M21_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box).resize((960, 630), Image.LANCZOS), out_en / f"{name}.png")
        else:
            print("missing", src)


# M21.1 scene (DevClientSmokeM211, -Psmoketestm21_1): the plant leaves in the miniature. ``py guide_images.py m21_1``.
M21_1_WORLD = {"plants_mesh_outside": ("bettacraft_m21_1_outside_rotala_new", (0, 40, 1280, 880))}


def m21_1(out_en: Path) -> None:
    for name, (shot, box) in M21_1_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box).resize((960, 630), Image.LANCZOS), out_en / f"{name}.png")
        else:
            print("missing", src)


# M21.5 scene (DevClientSmokeM215, -Psmoketestm21_5): green neon, racer nerites, clip-on feeder, skull and wreck.
# ``py guide_images.py m21_5``.
M21_5_WORLD = {"tm_green_neon_tetra": ("bettacraft_m21_5_green_neon", (180, 300, 1100, 620)),
               "critters_racer_nerites": ("bettacraft_m21_5_snails", (180, 330, 1100, 650)),
               "clip_feeder": ("bettacraft_m21_5_clip_feeder", (300, 220, 980, 660)),
               "decor_skull_wreck": ("bettacraft_m21_5_decor_inside", (200, 220, 1220, 840))}


def m21_5(out_en: Path) -> None:
    for name, (shot, box) in M21_5_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            im = Image.open(src).convert("RGB").crop(box)
            w = 960
            save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")
        else:
            print("missing", src)


# M21.6 scene (DevClientSmokeM216, -Psmoketestm21_6): 2x2 display tanks of guppies and endlers, combined into grids.
# ``py guide_images.py m21_6``.
M21_6_BOX = (300, 150, 980, 740)
M21_6_GRIDS = {
    "tm_guppy_strains": (["strains_guppy_1", "strains_guppy_2", "strains_guppy_4", "strains_endler_1"], 2),
    "tm_guppy_pairs": (["pairs_1", "females_1"], 2),
    "tm_guppy_tails": (["tails_1", "tails_2", "tails_3"], 3),
    "tm_guppy_hybrids": (["hybrids_1", "hybrids_2"], 2),
    "tm_guppy": (["strains_guppy_1"], 1),
    "tm_endler_guppy": (["strains_endler_1"], 1),
}


def m21_6(out_en: Path) -> None:
    for name, (shots, cols) in M21_6_GRIDS.items():
        tiles = []
        for shot in shots:
            src = SHOTS / f"bettacraft_m21_6_{shot}.png"
            if not src.exists():
                print("missing", src)
                return
            tiles.append(Image.open(src).convert("RGB").crop(M21_6_BOX))
        w, h = tiles[0].size
        rows = (len(tiles) + cols - 1) // cols
        sheet = Image.new("RGB", (w * cols, h * rows))
        for i, t in enumerate(tiles):
            sheet.paste(t, ((i % cols) * w, (i // cols) * h))
        width = 960
        save(sheet.resize((width, round(sheet.height * width / sheet.width)), Image.LANCZOS), out_en / f"{name}.png")


# M21.7 scene (DevClientSmokeM217, -Psmoketestm21_7): stone cabinets, strips of the row stacked into one picture.
# ``py guide_images.py m21_7``.
M21_7_STRIPS = {"cabinets_stone": [("counters", (60, 300, 1280, 560)), ("row_0", (0, 290, 1280, 580)), ("row_7", (0, 290, 1280, 580))]}


def m21_7(out_en: Path) -> None:
    for name, strips in M21_7_STRIPS.items():
        tiles = []
        for shot, box in strips:
            src = SHOTS / f"bettacraft_m21_7_{shot}.png"
            if not src.exists():
                print("missing", src)
                return
            t = Image.open(src).convert("RGB").crop(box)
            tiles.append(t.resize((1280, round(t.height * 1280 / t.width)), Image.LANCZOS))
        sheet = Image.new("RGB", (1280, sum(t.height for t in tiles)))
        y = 0
        for t in tiles:
            sheet.paste(t, (0, y))
            y += t.height
        width = 960
        save(sheet.resize((width, round(sheet.height * width / sheet.width)), Image.LANCZOS), out_en / f"{name}.png")


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
    if sys.argv[1:] == ["m21_7"]:
        m21_7(out_en)
        return 0
    if sys.argv[1:] == ["m21_6"]:
        m21_6(out_en)
        return 0
    if sys.argv[1:] == ["m21_5"]:
        m21_5(out_en)
        return 0
    if sys.argv[1:] == ["m21_1"]:
        m21_1(out_en)
        return 0
    if sys.argv[1:] == ["m21"]:
        m21(out_en)
        return 0
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
    if sys.argv[1:] == ["m16"]:
        m16(out_en)
        return 0
    if sys.argv[1:] == ["m18"]:
        m18(out_en, out_de)
        return 0
    if sys.argv[1:] == ["m19"]:
        m19(out_en, out_de)
        return 0
    if sys.argv[1:] == ["m20"]:
        m20(out_en, out_de)
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
    m16(out_en)
    m18(out_en, out_de)
    for kept in ("interior", "hologram"):
        p = out_en / f"{kept}.png"
        if p.exists() and Image.open(p).mode != "P":
            save(Image.open(p), p)
    return 0


if __name__ == "__main__":
    sys.exit(main())
