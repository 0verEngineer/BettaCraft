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


# M21.8 scene (DevClientSmokeM218, -Psmoketestm21_8): seahorses at their posts, the brood pouch. ``py guide_images.py m21_8``.
M21_8_WORLD = {"seahorses": ("bettacraft_m21_8_hitched", (0, 180, 1280, 660)),
               "seahorse_pouch": ("bettacraft_m21_8_pouch", (300, 180, 1000, 620))}


def m21_8(out_en: Path) -> None:
    for name, (shot, box) in M21_8_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            im = Image.open(src).convert("RGB").crop(box)
            w = 960
            save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")
        else:
            print("missing", src)


# M22.1 scene (DevClientSmokeM221, -Psmoketestm22_1): every plant and coral with its new look. ``py guide_images.py m22_1``.
M22_1_WORLD = {"plants_mesh": ("bettacraft_m22_1_inside_rooted_wide_new", (0, 60, 1280, 900)),
               "plants_mesh_all": ("bettacraft_m22_1_inside_epiphytes_wide_new", (0, 60, 1280, 900)),
               "plants_mesh_outside": ("bettacraft_m22_1_outside_rooted_new", (0, 40, 1280, 880)),
               "reef_corals_mesh": ("bettacraft_m22_1_inside_reef_wide_new", (0, 60, 1280, 900))}


def m22_1(out_en: Path) -> None:
    for name, (shot, box) in M22_1_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            im = Image.open(src).convert("RGB").crop(box)
            w = 960
            save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")
        else:
            print("missing", src)


# M22.2 gallery (DevClientSmokeM222, -Psmoketestm22_2): one small tank per species in front of a white wall, the same
# camera for every kind (reef fish and seahorses, snails and reef invertebrates, plants, corals, floating plants from
# above): ``bettacraft_m22_2_sp_<id>.png`` -> ``images/sp_<id>.png`` at 720x480. ``py guide_images.py m22_2``.
M22_2_BOX = {"fish": (160, 160, 1120, 800), "critter": (160, 200, 1120, 840), "floating": (160, 120, 1120, 760),
             "tall": (160, 100, 1120, 740), "low": (300, 360, 980, 813)}
M22_2_CRITTERS = {"zebra_nerite", "horned_nerite", "olive_nerite", "red_nerite", "yellow_nerite", "apple_snail", "ramshorn", "bladder_snail",
                  "trumpet_snail", "assassin_snail", "cleaner_shrimp", "hermit_crab", "turbo_snail"}
M22_2_FLOATING = {"duckweed", "amazon_frogbit", "salvinia", "red_root_floater", "water_lettuce", "hornwort"}
M22_2_FISH = {"ocellaris_clownfish", "yellow_tang", "yellow_watchman_goby", "green_chromis", "yellowtail_damsel", "lined_seahorse", "dwarf_seahorse"}
# plants and corals that reach up through the tank (the others sit low: a closer crop)
M22_2_TALL = {"java_fern", "amazon_sword", "rotala_rotundifolia", "ludwigia_repens", "alternanthera_reineckii", "vallisneria", "red_tiger_lotus",
              "acropora", "hammer_coral",
              # M28 part a: the new stem plants (Downoi stays low)
              "ludwigia_super_red", "rotala_hra", "rotala_wallichii", "hygrophila_pinnatifida", "limnophila_sessiliflora", "myriophyllum_red",
              "bacopa_caroliniana",
              # M28 part b: the tubers reach the surface
              "aponogeton_ulvaceus", "nymphaea_lotus_green",
              # M56: the tall terrarium plants
              "column_cactus", "papyrus", "umbrella_sedge", "pothos", "soft_rush", "swamp_sedge", "needlegrass", "sarracenia", "prickly_pear"}


def m22_2(out_en: Path) -> None:
    for src in sorted(SHOTS.glob("bettacraft_m22_2_sp_*.png")):
        sp = src.stem[len("bettacraft_m22_2_sp_"):]
        kind = ("critter" if sp in M22_2_CRITTERS else "floating" if sp in M22_2_FLOATING else "fish" if sp in M22_2_FISH
                else "tall" if sp in M22_2_TALL else "low")
        im = Image.open(src).convert("RGB").crop(M22_2_BOX[kind]).resize((720, 480), Image.LANCZOS)
        save(im, out_en / f"sp_{sp}.png")


# M22.4 scene (DevClientSmokeM224, -Psmoketestm22_4): the four outlet sides of the cabinet filter (back, left, right,
# front) as a 2x2 sheet of the close-ups. ``py guide_images.py m22_4``.
M22_4_SIDES = ["back", "left", "right", "front"]
M22_4_BOX = (370, 280, 910, 750)


def m22_4(out_en: Path) -> None:
    tiles = []
    for side in M22_4_SIDES:
        src = SHOTS / f"bettacraft_m22_4_outlet_{side}.png"
        if not src.exists():
            print("missing", src)
            return
        tiles.append(Image.open(src).convert("RGB").crop(M22_4_BOX))
    w, h = tiles[0].size
    sheet = Image.new("RGB", (w * 2, h * 2))
    for i, t in enumerate(tiles):
        sheet.paste(t, ((i % 2) * w, (i // 2) * h))
    width = 960
    save(sheet.resize((width, round(sheet.height * width / sheet.width)), Image.LANCZOS), out_en / "cabinet_outlets.png")


# M24 scene (DevClientSmokeM24, -Psmoketestm24): the multiblock filter, the tank on pipes, the interior pipes
M24_WORLD = {"multiblock_filter": ("bettacraft_m24_multiblock_filter", (0, 120, 1280, 900)),
             "pipes": ("bettacraft_m24_scene", (80, 300, 1240, 560)),
             "interior_pipes": ("bettacraft_m24_interior", (0, 0, 1280, 960))}


def m24(out_en: Path) -> None:
    for name, (shot, box) in M24_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if not src.exists():
            print("missing", src)
            continue
        im = Image.open(src).convert("RGB").crop(box)
        width = min(960, im.width)
        save(im.resize((width, round(im.height * width / im.width)), Image.LANCZOS), out_en / f"{name}.png")


# M25 part c (DevClientSmokeM25c, -Psmoketestm25c): the bee shrimp lines, the Sulawesi tank, the dosing pump window (en, de)
M25C_WORLD = {"critters_caridina": ("bettacraft_m25_caridina", (0, 400, 1280, 600)),
              "critters_sulawesi": ("bettacraft_m25_sulawesi", (240, 380, 1040, 620))}
M25C_MENU = ("bettacraft_m25_dosing_pump", "bettacraft_m25_dosing_pump_de", (464, 294, 816, 664))


def m25c(out_en: Path, out_de: Path) -> None:
    for name, (shot, box) in M25C_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out_en / f"{name}.png")
        else:
            print("missing", src)
    en, de, box = M25C_MENU
    for shot, out in ((en, out_en), (de, out_de)):
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out / "dosing_pump_menu.png")
        else:
            print("missing", src)


# M26 part a (DevClientSmokeM26, -Psmoketestm26): the live food cultures with the pump, the pump window (en, de)
M26_WORLD = {"live_food_cultures": ("bettacraft_m26_cultures", (150, 280, 1050, 540))}
M26_MENU = ("bettacraft_m26_pump", "bettacraft_m26_pump_de", (464, 368, 816, 592))


def m26(out_en: Path, out_de: Path) -> None:
    for name, (shot, box) in M26_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out_en / f"{name}.png")
        else:
            print("missing", src)
    en, de, box = M26_MENU
    for shot, out in ((en, out_en), (de, out_de)):
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out / "live_food_pump_menu.png")
        else:
            print("missing", src)


# M26 part b (same scene): the food maker at the tank, its window with flakes in the making (en, de)
M26B_WORLD = {"food_maker": ("bettacraft_m26_maker", (380, 70, 1100, 620))}
M26B_MENU = ("bettacraft_m26_maker_menu", "bettacraft_m26_maker_menu_de", (464, 314, 816, 646))


def m26b(out_en: Path, out_de: Path) -> None:
    for name, (shot, box) in M26B_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out_en / f"{name}.png")
        else:
            print("missing", src)
    en, de, box = M26B_MENU
    for shot, out in ((en, out_en), (de, out_de)):
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out / "food_maker_menu.png")
        else:
            print("missing", src)


# M27 part b (DevClientSmokeM27, -Psmoketestm27): the dashboard on the 3 x 2 wall (en, de) and the editor
M27_DASH = ("bettacraft_m27_dashboard", "bettacraft_m27_dashboard_de", (380, 240, 900, 580))
M27_EDITOR = ("bettacraft_m27_editor", (0, 0, 1280, 960))


def m27(out_en: Path, out_de: Path) -> None:
    en, de, box = M27_DASH
    for shot, out in ((en, out_en), (de, out_de)):
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out / "monitor_dashboard.png")
        else:
            print("missing", src)
    shot, box = M27_EDITOR
    src = SHOTS / f"{shot}.png"
    if src.exists():
        save(Image.open(src).convert("RGB").crop(box), out_en / "monitor_editor.png")
    else:
        print("missing", src)


# M28 part c scene (DevClientSmokeM28c, -Psmoketestm28c): the close-ups of the new decorations as one sheet per
# handbook page (three per row), the leaf litter from its wide shot. ``py guide_images.py m28c``.
M28C_GROUPS = {
    "decor_m28_wrecks": ["rowing_boat", "ship_bow"],
    "decor_m28_stones": ["holey_rock", "slate_cave", "seiryu_stone", "dragon_stone", "lava_rock"],
    "decor_m28_roots": ["mangrove_root", "spiderwood", "bogwood_bridge"],
    "decor_m28_caves": ["coconut_cave", "clay_tubes", "shrimp_tubes"],
    "decor_m28_kitsch": ["ruin_column", "diver_helmet", "treasure_chest"],
}
M28C_BOX = (200, 150, 1080, 810)  # 880 x 660 of the 1280 x 960 close-up
M28C_TILE = (360, 270)


def m28c(out_en: Path) -> None:
    for name, ids in M28C_GROUPS.items():
        tiles = []
        for i in ids:
            src = SHOTS / f"bettacraft_m28c_inside_{i}.png"
            if not src.exists():
                print("missing", src)
                return
            tiles.append(Image.open(src).convert("RGB").crop(M28C_BOX).resize(M28C_TILE, Image.LANCZOS))
        cols = min(3, len(tiles))
        rows = (len(tiles) + cols - 1) // cols
        sheet = Image.new("RGB", (cols * M28C_TILE[0] + (cols - 1) * 4, rows * M28C_TILE[1] + (rows - 1) * 4), (40, 40, 44))
        for k, t in enumerate(tiles):
            sheet.paste(t, ((k % cols) * (M28C_TILE[0] + 4), (k // cols) * (M28C_TILE[1] + 4)))
        save(sheet, out_en / f"{name}.png")
    src = SHOTS / "bettacraft_m28c_inside_litter_wide.png"
    if src.exists():
        save(Image.open(src).convert("RGB").crop((160, 240, 1120, 960)).resize((720, 540), Image.LANCZOS), out_en / "decor_m28_litter.png")
    else:
        print("missing", src)


# M29 part a scene (DevClientSmokeM29, -Psmoketestm29): a close-up of every cichlid species, the dwarf cichlid tank, the
# angelfish and discus tank, a ram pair at its brood. ``py guide_images.py m29``.
M29_DWARF_BOX = (280, 200, 1000, 740)
M29_WORLD = {
    "tm_ram_cichlid": ("sp_ram_cichlid", M29_DWARF_BOX),
    "tm_bolivian_ram": ("sp_bolivian_ram", M29_DWARF_BOX),
    "tm_cockatoo_cichlid": ("sp_cockatoo_cichlid", M29_DWARF_BOX),
    "tm_agassiz_dwarf_cichlid": ("sp_agassiz_dwarf_cichlid", M29_DWARF_BOX),
    "tm_angelfish": ("sp_angelfish", (200, 100, 1080, 760)),
    "tm_discus": ("sp_discus", (240, 60, 1040, 660)),
    "tm_cichlids": ("dwarfs_close", (0, 280, 1280, 640)),
    "tm_cichlid_brood": ("brood", (300, 200, 1000, 620)),
}


def m29(out_en: Path) -> None:
    for name, (shot, box) in M29_WORLD.items():
        src = SHOTS / f"bettacraft_m29_{shot}.png"
        if not src.exists():
            print("missing", src)
            continue
        im = Image.open(src).convert("RGB").crop(box)
        w = min(800, im.width)
        save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")


# M29 part b scene (DevClientSmokeM29 part b, -Psmoketestm29b): a close-up of every puffer, barb and loach, the puffer and
# barb tank, the loach tank. ``py guide_images.py m29b``.
M29B_BOX = (280, 200, 1000, 740)
M29B_WORLD = {f"tm_{s}": (f"sp_{s}", M29B_BOX) for s in (
    "dwarf_puffer", "red_eye_puffer", "green_spotted_puffer", "tiger_barb", "odessa_barb", "cherry_barb",
    "dwarf_chain_loach", "reticulated_loach", "garra_rufa")}
# the dwarf puffers are tiny: a tighter crop (the camera cannot get closer than the glass)
M29B_WORLD["tm_dwarf_puffer"] = ("sp_dwarf_puffer", (480, 370, 760, 560))
M29B_WORLD["tm_red_eye_puffer"] = ("sp_red_eye_puffer", (500, 360, 780, 550))
M29B_WORLD["tm_puffers"] = ("puffers_barbs", (0, 280, 1280, 700))
M29B_WORLD["tm_barbs"] = ("puffers_barbs", (0, 280, 1280, 700))


def m29b(out_en: Path) -> None:
    for name, (shot, box) in M29B_WORLD.items():
        src = SHOTS / f"bettacraft_m29b_{shot}.png"
        if not src.exists():
            print("missing", src)
            continue
        im = Image.open(src).convert("RGB").crop(box)
        w = min(800, im.width)
        save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")


# M29 part c scene (DevClientSmokeM29 part c, -Psmoketestm29c): a close-up of every new species and wild betta, the group
# tanks. ``py guide_images.py m29c``.
M29C_WORLD = {f"tm_{s}": (f"sp_{s}", M29B_BOX) for s in (
    "zebra_danio", "forktail_rainbow", "neon_dwarf_rainbow", "honey_gourami", "chocolate_gourami", "pearl_gourami",
    "emperor_tetra", "rummy_nose_tetra", "black_phantom_tetra", "black_molly", "swordtail")}
M29C_WORLD["tm_celestial_pearl_danio"] = ("sp_celestial_pearl_danio", (480, 370, 760, 560))
M29C_WORLD["tm_rainbowfish"] = ("community", (0, 280, 1280, 700))
M29C_WORLD["tm_gouramis"] = ("gouramis_livebearers", (0, 280, 1280, 700))
M29C_WORLD["betta_imbellis"] = ("betta_imbellis", M29B_BOX)
M29C_WORLD["betta_smaragdina"] = ("betta_smaragdina", M29B_BOX)


def m29c(out_en: Path) -> None:
    for name, (shot, box) in M29C_WORLD.items():
        src = SHOTS / f"bettacraft_m29c_{shot}.png"
        if not src.exists():
            print("missing", src)
            continue
        im = Image.open(src).convert("RGB").crop(box)
        w = min(800, im.width)
        save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")


# M29 part d scene (DevClientSmokeM29d, -Psmoketestm29d): the big crayfish and the axolotl tank. ``py guide_images.py m29d``.
M29D_WORLD = {
    "critters_big_crayfish": ("crayfish_close", (0, 180, 1280, 720)),
    "axolotl_tank": ("axolotl", (0, 120, 1280, 600)),
}


def m29d(out_en: Path) -> None:
    for name, (shot, box) in M29D_WORLD.items():
        src = SHOTS / f"bettacraft_m29d_{shot}.png"
        if not src.exists():
            print("missing", src)
            continue
        im = Image.open(src).convert("RGB").crop(box)
        w = min(800, im.width)
        save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")


# M30 part a scene (DevClientSmokeM30a, -Psmoketestm30a): the pangasius tank and the big sea water tank with two dolphins.
# ``py guide_images.py m30a``.
M30A_WORLD = {
    "tm_pangasius": ("pangasius_tank", (330, 250, 970, 730)),
    "big_tank_dolphins": ("dolphin_tank", (0, 200, 1280, 560)),
}


def m30a(out_en: Path) -> None:
    for name, (shot, box) in M30A_WORLD.items():
        src = SHOTS / f"bettacraft_m30a_{shot}.png"
        if not src.exists():
            print("missing", src)
            continue
        im = Image.open(src).convert("RGB").crop(box)
        w = min(800, im.width)
        save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")



# M30 part b scene (DevClientSmokeM30b, -Psmoketestm30b): the freshwater giants in a 24 x 6 x 12 tank, a stingray from above.
# ``py guide_images.py m30b``.
M30B_WORLD = {
    "tm_redtail_catfish": ("catfish_near", (200, 340, 560, 560)),
    "tm_wels_catfish": ("catfish_near", (620, 410, 880, 570)),
    "tm_arapaima": ("catfish_near", (700, 260, 1000, 420)),
    "tm_motoro_stingray": ("stingray_top", (500, 380, 800, 580)),
}


def m30b(out_en: Path) -> None:
    for name, (shot, box) in M30B_WORLD.items():
        src = SHOTS / f"bettacraft_m30b_{shot}.png"
        if not src.exists():
            print("missing", src)
            continue
        im = Image.open(src).convert("RGB").crop(box)
        w = min(800, im.width * 2)
        save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")


# M30 part c scene (DevClientSmokeM30c, -Psmoketestm30c): the sea giants in a 28 x 6 x 14 sea water tank, the egg cases.
# ``py guide_images.py m30c``.
M30C_WORLD = {
    "big_tank_sea": ("sea_tank", (130, 290, 1160, 530)),
    "tm_bamboo_shark": ("egg_case", (0, 180, 940, 470)),
    "big_tank_egg_case": ("egg_case", (620, 300, 960, 470)),
    "tm_blacktip_reef_shark": ("rays_side", (70, 140, 500, 330)),
    "tm_spotted_eagle_ray": ("rays_side", (700, 240, 1240, 410)),
    "tm_cownose_ray": ("rays_side", (200, 290, 680, 510)),
    "tm_honeycomb_moray": ("moray_cave", (0, 120, 760, 680)),
}


def m30c(out_en: Path) -> None:
    for name, (shot, box) in M30C_WORLD.items():
        src = SHOTS / f"bettacraft_m30c_{shot}.png"
        if not src.exists():
            print("missing", src)
            continue
        im = Image.open(src).convert("RGB").crop(box)
        w = min(800, im.width * 2)
        save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")


# M36 scene (DevClientSmokeM36, -Psmoketestm36, without -Pcompare): the same community tank at Normal and Large side by
# side -> images/betta_sizes.png. ``py guide_images.py m36`` also crops again every handbook image that shows our bettas
# (from fresh -Psmoketestworld, -Psmoketestm11, -Psmoketestm15 and -Psmoketestm29c runs: the bettas are smaller since M36).
M36_SIZES = (("bettacraft_m36_tank_normal", "bettacraft_m36_tank_large"), (240, 250, 1000, 600))
M36_WORLD = ("tank", "nest", "stages", "forms", "varieties")


def m36(out_en: Path) -> None:
    names, box = M36_SIZES
    srcs = [SHOTS / f"{n}.png" for n in names]
    if all(p.exists() for p in srcs):
        a, b = (Image.open(p).convert("RGB").crop(box) for p in srcs)
        gap = 8
        im = Image.new("RGB", (a.width + gap + b.width, a.height), (255, 255, 255))
        im.paste(a, (0, 0))
        im.paste(b, (a.width + gap, 0))
        w = 900
        save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / "betta_sizes.png")
    else:
        print("missing", srcs)
    for name in M36_WORLD:
        src = SHOTS / f"guide_{name}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(WORLD[name]), out_en / f"{name}.png")
        else:
            print("missing", src)
    for name in ("mirror_stick",):
        shot, box = M11_WORLD[name]
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out_en / f"{name}.png")
        else:
            print("missing", src)
    for name, (shot, box) in M15_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out_en / f"{name}.png")
        else:
            print("missing", src)
    for name in ("betta_imbellis", "betta_smaragdina"):
        shot, box = M29C_WORLD[name]
        src = SHOTS / f"bettacraft_m29c_{shot}.png"
        if not src.exists():
            print("missing", src)
            continue
        im = Image.open(src).convert("RGB").crop(box)
        w = min(800, im.width)
        save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")


# M37 scene (DevClientSmokeM37, -Psmoketestm37 -PnoJei): the fertilizer maker piping into a tank on cabinets, the crank at
# the cabinet with the action bar, the window of the fertilizer maker (en, de)
M37_WORLD = {"fertilizer_maker": ("bettacraft_m37_machine", (250, 30, 1180, 560)), "crank": ("bettacraft_m37_crank", (160, 0, 1120, 960))}
M37_MENU = ("bettacraft_m37_maker_menu", "bettacraft_m37_maker_menu_de", (464, 314, 816, 646))


def m37(out_en: Path, out_de: Path) -> None:
    for name, (shot, box) in M37_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            im = Image.open(src).convert("RGB").crop(box)
            w = min(800, im.width)
            save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")
        else:
            print("missing", src)
    en, de, box = M37_MENU
    for shot, out in ((en, out_en), (de, out_de)):
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out / "fertilizer_maker_menu.png")
        else:
            print("missing", src)


# M38 scene (DevClientSmokeM38, -Psmoketestm38): a tank planted with kelp, seagrass and the water plants of other mods, the
# reef tank with Minecraft's corals and Fintastic's live rock
M38_WORLD = {"foreign_plants": ("bettacraft_m38_fresh_a", (110, 0, 1180, 700)), "foreign_corals": ("bettacraft_m38_reef", (0, 0, 1070, 700))}


def m38(out_en: Path) -> None:
    for name, (shot, box) in M38_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            im = Image.open(src).convert("RGB").crop(box)
            w = min(800, im.width)
            save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")
        else:
            print("missing", src)


# M39 part a scene (DevClientSmokeM39a, -Psmoketestm39a -Paquatic): the sea animals of other mods, the Aquaculture turtles
M39A_WORLD = {"foreign_water_animals": ("bettacraft_m39a_sea", (180, 250, 1180, 580)),
              "foreign_turtles": ("bettacraft_m39a_turtles", (120, 300, 1160, 590))}


def m39a(out_en: Path) -> None:
    for name, (shot, box) in M39A_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            im = Image.open(src).convert("RGB").crop(box)
            w = min(800, im.width)
            save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")
        else:
            print("missing", src)


# M39 part b scene (DevClientSmokeM39b, -Psmoketestm39b -Paquatic): insects and crawlers, small mammals and birds of other mods
M39B_WORLD = {"foreign_insects": ("bettacraft_m39b_insects", (130, 250, 1170, 570)),
              "foreign_mammals": ("bettacraft_m39b_mammals", (90, 270, 1190, 550))}


def m39b(out_en: Path) -> None:
    for name, (shot, box) in M39B_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            im = Image.open(src).convert("RGB").crop(box)
            w = min(800, im.width)
            save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")
        else:
            print("missing", src)


# M39 part c scene (DevClientSmokeM39c, -Psmoketestm39c): the peaceful animals of Minecraft as terrarium pets
M39C_WORLD = {"minecraft_pets": ("bettacraft_m39c_pets", (460, 190, 1280, 560)),
              "minecraft_frogs": ("bettacraft_m39c_frogs", (170, 200, 1120, 565))}


def m39c(out_en: Path) -> None:
    for name, (shot, box) in M39C_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            im = Image.open(src).convert("RGB").crop(box)
            w = min(800, im.width)
            save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")
        else:
            print("missing", src)


# M40 scenes (DevClientSmokeM40a/b/c, -Psmoketestm40a, -Psmoketestm40b, -Psmoketestm40c -Ppower -Pstorage -Pfacades): one filter for
# three aquariums, the filter screen with its aquariums, the guard at a side dock, the pipe bundles, the water ports, the
# covers and the facades of other mods
M40_WORLD = {"filter_lines": ("bettacraft_m40a_filter", (40, 380, 1240, 730)),
             "pipe_guards": ("bettacraft_m40a_dock_side", (440, 330, 760, 500)),
             "pipe_bundle": ("bettacraft_m40b_bundle_end", (0, 40, 1280, 720)),
             "filter_water": ("bettacraft_m40b_filter_water", (150, 150, 900, 640)),
             "pipe_covers": ("bettacraft_m40c_covers_close", (0, 240, 1280, 700)),
             "pipe_facades": ("bettacraft_m40c_facades", (0, 150, 1280, 620))}
M40_SCREEN = ("bettacraft_m40a_screen", (362, 268, 918, 480))


def m40(out_en: Path) -> None:
    for name, (shot, box) in M40_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            im = Image.open(src).convert("RGB").crop(box)
            w = min(800, im.width)
            save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")
        else:
            print("missing", src)
    shot, box = M40_SCREEN
    src = SHOTS / f"{shot}.png"
    if src.exists():
        save(Image.open(src).convert("RGB").crop(box), out_en / "filter_screen_tanks.png")
    else:
        print("missing", src)


# M41 scene (DevClientSmokeM41, -Pcreate -Psmoketestm41): a tank on Create's rotation through a drive adapter on its wall and a
# shaft on the cabinet under it; the adapter close up with Create's goggles, the cabinet's flange
M41_WORLD = {"drive_adapter": ("bettacraft_m41_drive", (0, 120, 1280, 840)),
             "drive_adapter_goggles": ("bettacraft_m41_goggles", (0, 0, 1280, 960)),
             "cabinet_drive": ("bettacraft_m41_cabinet", (160, 120, 1120, 840))}


def m41(out_en: Path) -> None:
    for name, (shot, box) in M41_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            im = Image.open(src).convert("RGB").crop(box)
            w = min(800, im.width)
            save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")
        else:
            print("missing", src)


# M44 scene (DevClientSmokeM44, -Psmoketestm44): cabinet rows with their fronts (1 to 4, 5 and 6, 12 blocks; drawers at the
# ends, double doors, a single door in odd rows) stacked into one picture, and the cabinet menu with the buttons Front and
# joints (GUI scale 2 in that run: doubled to the size of the other menu pictures)
M44_STRIPS = [("bettacraft_m44_cab_rows_1to4", (0, 330, 1280, 640)), ("bettacraft_m44_cab_rows_5_6", (0, 330, 1280, 640)),
              ("bettacraft_m44_cab_row_12", (60, 230, 1220, 540))]
M44_MENU = ("bettacraft_m44_menu_auto", (464, 316, 816, 648))


def m44(out_en: Path) -> None:
    strips = []
    for shot, box in M44_STRIPS:
        src = SHOTS / f"{shot}.png"
        if not src.exists():
            print("missing", src)
            return
        im = Image.open(src).convert("RGB").crop(box)
        strips.append(im.resize((1000, round(im.height * 1000 / im.width)), Image.LANCZOS))
    sheet = Image.new("RGB", (1000, sum(s.height for s in strips) + 4 * (len(strips) - 1)), (40, 40, 40))
    y = 0
    for s in strips:
        sheet.paste(s, (0, y))
        y += s.height + 4
    save(sheet, out_en / "cabinet_fronts.png")
    shot, box = M44_MENU
    src = SHOTS / f"{shot}.png"
    if src.exists():
        im = Image.open(src).convert("RGB").crop(box)
        save(im.resize((im.width * 2, im.height * 2), Image.NEAREST), out_en / "cabinet_front_menu.png")
    else:
        print("missing", src)


# M46 scene (DevClientSmokeM46, -Psmoketestm46): the wild carpet cushions in a swamp pool, the leopard gecko morphs (adults in
# front, the young behind them) from the side
M46_WORLD = {"wild_carpets": ("bettacraft_m46_carpet_swamp", (0, 200, 1280, 760)),
             "leopard_gecko_morphs": ("bettacraft_m46_gecko_morphs_2", (140, 280, 1140, 760))}


def m46(out_en: Path) -> None:
    for name, (shot, box) in M46_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out_en / f"{name}.png")
        else:
            print("missing", src)


M47_WORLD = {"aquarist_villager": ("bettacraft_m47_aquarist_village", (300, 120, 1180, 840)),
             "aquarist_trades": ("bettacraft_m47_trade_betta", (366, 314, 916, 646)),
             "ram_forms": ("bettacraft_m47_rams", (130, 300, 1150, 760)),
             "frog_breeding": ("bettacraft_m47_frog_spawn_top", (60, 100, 1220, 720)),
             "frog_amplexus": ("bettacraft_m47_amplexus", (240, 260, 1000, 640))}


def m47(out_en: Path) -> None:
    for name, (shot, box) in M47_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if src.exists():
            save(Image.open(src).convert("RGB").crop(box), out_en / f"{name}.png")
        else:
            print("missing", src)


# M48 scene (DevClientSmokeM48, -Psmoketestm48): the tetra and cory forms, the bronze cory and the panda garra, the new reef
# animals, the clownfish larvae. ``py guide_images.py m48``.
M48_WORLD = {"neon_forms": ("bettacraft_m48_neon_forms", (150, 300, 1130, 480)),
             "panda_cory_forms": ("bettacraft_m48_panda_cory_forms", (200, 290, 1120, 500)),
             "bronze_cory_forms": ("bettacraft_m48_bronze_cory_forms", (180, 280, 1100, 500)),
             "tm_bronze_cory": ("bettacraft_m48_bronze_cory_forms", (180, 270, 560, 500)),
             "tm_panda_garra": ("bettacraft_m48_sp_panda_garra", (180, 270, 1100, 500)),
             "sp_mandarinfish": ("bettacraft_m48_sp_mandarinfish", (380, 330, 1000, 640)),
             "sp_banggai_cardinal": ("bettacraft_m48_sp_banggai_cardinal", (440, 290, 840, 520)),
             "sp_lyretail_anthias": ("bettacraft_m48_sp_lyretail_anthias", (440, 280, 1280, 520)),
             "sp_fire_shrimp": ("bettacraft_m48_sp_fire_shrimp", (300, 330, 900, 730)),
             "reef_larvae": ("bettacraft_m48_larvae", (300, 150, 960, 620))}


def m48(out_en: Path) -> None:
    for name, (shot, box) in M48_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if not src.exists():
            print("missing", src)
            continue
        im = Image.open(src).convert("RGB").crop(box)
        w = min(800, im.width)
        save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")


# M49 scene (DevClientSmokeM49, -Psmoketestm49): the sump with the calcium reactor, the brackish paludarium, the archerfish,
# the mudskipper, the mangrove, the Amano shrimp; the windows of the sump and the reactor and the tab "Sump" (en, de).
# ``py guide_images.py m49``.
M49_WORLD = {"sump": ("bettacraft_m49_sump", (330, 140, 1000, 600)),
             "calcium_reactor": ("bettacraft_m49_calcium_reactor", (200, 180, 1280, 620)),
             "brackish": ("bettacraft_m49_brackish", (310, 190, 970, 590)),
             "archer_shot": ("bettacraft_m49_archer_shot", (150, 150, 1110, 880)),
             "sp_archerfish": ("bettacraft_m49_sp_archerfish", (140, 120, 1140, 760)),
             "sp_mudskipper": ("bettacraft_m49_sp_mudskipper", (200, 200, 1000, 700)),
             "mangrove": ("bettacraft_m49_mangrove", (150, 200, 1130, 900)),
             "sp_amano_shrimp": ("bettacraft_m49_sp_amano_shrimp", (150, 150, 1100, 600))}
M49_MENUS = {"sump_menu": ("bettacraft_m49_menu_sump", (464, 302, 816, 658)),
             "sump_tab": ("bettacraft_m49_menu_sump_tab", (394, 250, 886, 530)),
             "calcium_reactor_menu": ("bettacraft_m49_menu_reactor", (464, 314, 816, 480))}


def m49(out_en: Path, out_de: Path) -> None:
    for name, (shot, box) in M49_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if not src.exists():
            print("missing", src)
            continue
        im = Image.open(src).convert("RGB").crop(box)
        w = min(800, im.width)
        save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")
    for name, (shot, box) in M49_MENUS.items():
        for suffix, out in (("", out_en), ("_de", out_de)):
            src = SHOTS / f"{shot}{suffix}.png"
            if src.exists():
                save(Image.open(src).convert("RGB").crop(box), out / f"{name}.png")
            else:
                print("missing", src)


# M50 scene (DevClientSmokeM50, -Pcreate -Psmoketestm50): the sloshing water of a tank on a minecart contraption in a curve
# and on a bearing that stopped at once, a tank with live bettas on a Sable airship. ``py guide_images.py m50``.
M50_WORLD = {"travel_curve": ("bettacraft_m50_cart_curve", (460, 180, 1040, 580)),
             "travel_stop": ("bettacraft_m50_bearing_stop", (520, 200, 1080, 620)),
             "travel_airship": ("bettacraft_m50_airship", (330, 230, 950, 720))}


def m50(out_en: Path) -> None:
    for name, (shot, box) in M50_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if not src.exists():
            print("missing", src)
            continue
        im = Image.open(src).convert("RGB").crop(box)
        w = min(800, im.width)
        save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")


# M51 scene (DevClientSmokeM51, -Psmoketestm51): the hired aquarist at his workplace, his offer, his menu and the tank menu
# with the aquarist button (en, de), his four looks. ``py guide_images.py m51``.
M51_WORLD = {"hired_aquarist": ("bettacraft_m51_working_1", (280, 270, 1200, 680)),
             "hired_aquarist_looks": ("bettacraft_m51_variants", (240, 320, 1040, 720))}
M51_MENUS = {"hired_aquarist_offer": ("bettacraft_m51_offer_menu", (340, 258, 940, 702)),
             "hired_aquarist_menu": ("bettacraft_m51_menu", (340, 258, 940, 702)),
             "hired_aquarist_tank_menu": ("bettacraft_m51_tank_menu", (394, 250, 886, 530))}


def m51(out_en: Path, out_de: Path) -> None:
    for name, (shot, box) in M51_WORLD.items():
        src = SHOTS / f"{shot}.png"
        if not src.exists():
            print("missing", src)
            continue
        im = Image.open(src).convert("RGB").crop(box)
        w = min(800, im.width)
        save(im.resize((w, round(im.height * w / im.width)), Image.LANCZOS), out_en / f"{name}.png")
    for name, (shot, box) in M51_MENUS.items():
        for suffix, out in (("", out_en), ("_de", out_de)):
            src = SHOTS / f"{shot}{suffix}.png"
            if src.exists():
                save(Image.open(src).convert("RGB").crop(box), out / f"{name}.png")
            else:
                print("missing", src)


def m54(out_en: Path) -> None:
    """M54 (-Psmoketestm54): how the amphibians and reptiles move: the corn snake raised and coiled, the leopard gecko
    stretched, the snake coiled and the geckos in a terrarium with a rock and a ledge (a 2 x 2 picture)."""
    tiles = [("bettacraft_m54_actions_snake_head_up_side", (0, 280, 800, 680)),
             ("bettacraft_m54_actions_snake_coil_side", (240, 330, 1040, 730)),
             ("bettacraft_m54_actions_gecko_stretch_side", (240, 330, 1040, 730)),
             ("bettacraft_m54_tank_actions_coiled", (0, 120, 1280, 760))]
    out = Image.new("RGB", (800, 400))
    for i, (shot, box) in enumerate(tiles):
        src = SHOTS / f"{shot}.png"
        if not src.exists():
            print("missing", src)
            return
        out.paste(Image.open(src).convert("RGB").crop(box).resize((400, 200), Image.LANCZOS), ((i % 2) * 400, (i // 2) * 200))
    save(out, out_en / "herps_moves.png")


def m55(out_en: Path) -> None:
    """M55 (-Psmoketestm55 -Pm55tag=final): the wild carpets (the four looks, 2 x 2), the wild mosses and Marimo from above,
    the containers with their animals (two chest pages side by side)."""
    def tiles(names, box, size, cols, out_name):
        out = Image.new("RGB", (size[0] * cols, size[1] * ((len(names) + cols - 1) // cols)))
        for i, shot in enumerate(names):
            src = SHOTS / f"{shot}.png"
            if not src.exists():
                print("missing", src)
                return
            out.paste(Image.open(src).convert("RGB").crop(box).resize(size, Image.LANCZOS), ((i % cols) * size[0], (i // cols) * size[1]))
        save(out, out_en / out_name)

    tiles([f"bettacraft_m55_carpet_look_{k}_final" for k in ("mat", "clover", "fine", "needle")], (0, 160, 1280, 800), (480, 240), 2,
          "wild_carpets.png")
    tiles(["bettacraft_m55_plants_close_wild_moss_0", "bettacraft_m55_plants_close_wild_moss_1"], (0, 160, 1280, 800), (480, 240), 2,
          "wild_mosses.png")
    tiles(["bettacraft_m55_icons_0", "bettacraft_m55_icons_2"], (300, 100, 990, 540), (460, 293), 2, "animal_icons.png")


def m56(out_en: Path) -> None:
    """M56 (-Psmoketestm56): the three tanks of terrarium plants from inside (one picture per habitat page) and the rainforest
    terrarium from outside (the chapter page). The species pictures come from the M22.2 gallery (-Pgallery=<ids>, m22_2)."""
    for hab in ("tropical", "swamp", "dry"):
        src = SHOTS / f"bettacraft_m56_inside_{hab}.png"
        if not src.exists():
            print("missing", src)
            continue
        save(Image.open(src).convert("RGB").crop((0, 90, 1280, 810)).resize((960, 540), Image.LANCZOS), out_en / f"terra_plants_{hab}.png")
    src = SHOTS / "bettacraft_m56_tank_tropical.png"
    if src.exists():
        save(Image.open(src).convert("RGB").crop((140, 120, 1140, 745)).resize((960, 600), Image.LANCZOS), out_en / "terra_plants.png")
    else:
        print("missing", src)


def m62b(out_en: Path, out_de: Path) -> None:
    """M62 parts 2 and 3 (-Psmoketestm62b): the under-cabinet over the whole area (a 10 x 5 tank from the front and the back,
    a ring of cabinets around stone), the external filter one and two blocks below the tank and the chosen tank outlined,
    the choice of the tank (en and de)."""
    def tiles(names, size, cols, path):
        out = Image.new("RGB", (size[0] * cols, size[1] * ((len(names) + cols - 1) // cols)))
        for i, (shot, box) in enumerate(names):
            src = SHOTS / f"{shot}.png"
            if not src.exists():
                print("missing", src)
                return
            out.paste(Image.open(src).convert("RGB").crop(box).resize(size, Image.LANCZOS), ((i % cols) * size[0], (i // cols) * size[1]))
        save(out, path)

    tiles([("bettacraft_m62b_cabinet_area_10x5", (330, 330, 1010, 750)), ("bettacraft_m62b_cabinet_area_10x5_back", (440, 300, 1020, 658)),
           ("bettacraft_m62b_cabinet_ring", (380, 330, 1060, 750))], (400, 247), 3, out_en / "cabinet_area.png")
    tiles([("bettacraft_m62b_filter_one_below", (330, 180, 930, 720)), ("bettacraft_m62b_filter_two_below", (330, 160, 930, 700)),
           ("bettacraft_m62b_filter_chosen", (330, 400, 950, 958))], (360, 324), 3, out_en / "filter_below.png")
    for lang, out in (("en", out_en), ("de", out_de)):
        src = SHOTS / f"bettacraft_m62b_filter_choice_{lang}.png"
        if not src.exists():
            print("missing", src)
            continue
        save(Image.open(src).convert("RGB").crop((100, 60, 1180, 900)).resize((720, 560), Image.LANCZOS), out / "filter_choice.png")


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
    if sys.argv[1:] == ["m62b"]:
        m62b(out_en, out_de)
        return 0
    if sys.argv[1:] == ["m56"]:
        m56(out_en)
        return 0
    if sys.argv[1:] == ["m55"]:
        m55(out_en)
        return 0
    if sys.argv[1:] == ["m54"]:
        m54(out_en)
        return 0
    if sys.argv[1:] == ["m51"]:
        m51(out_en, out_de)
        return 0
    if sys.argv[1:] == ["m50"]:
        m50(out_en)
        return 0
    if sys.argv[1:] == ["m49"]:
        m49(out_en, out_de)
        return 0
    if sys.argv[1:] == ["m48"]:
        m48(out_en)
        return 0
    if sys.argv[1:] == ["m47"]:
        m47(out_en)
        return 0
    if sys.argv[1:] == ["m46"]:
        m46(out_en)
        return 0
    if sys.argv[1:] == ["m44"]:
        m44(out_en)
        return 0
    if sys.argv[1:] == ["m41"]:
        m41(out_en)
        return 0
    if sys.argv[1:] == ["m40"]:
        m40(out_en)
        return 0
    if sys.argv[1:] == ["m39c"]:
        m39c(out_en)
        return 0
    if sys.argv[1:] == ["m39b"]:
        m39b(out_en)
        return 0
    if sys.argv[1:] == ["m39a"]:
        m39a(out_en)
        return 0
    if sys.argv[1:] == ["m38"]:
        m38(out_en)
        return 0
    if sys.argv[1:] == ["m37"]:
        m37(out_en, out_de)
        return 0
    if sys.argv[1:] == ["m36"]:
        m36(out_en)
        return 0
    if sys.argv[1:] == ["m30c"]:
        m30c(out_en)
        return 0
    if sys.argv[1:] == ["m30b"]:
        m30b(out_en)
        return 0
    if sys.argv[1:] == ["m30a"]:
        m30a(out_en)
        return 0
    if sys.argv[1:] == ["m29d"]:
        m29d(out_en)
        return 0
    if sys.argv[1:] == ["m29c"]:
        m29c(out_en)
        return 0
    if sys.argv[1:] == ["m29b"]:
        m29b(out_en)
        return 0
    if sys.argv[1:] == ["m29"]:
        m29(out_en)
        return 0
    if sys.argv[1:] == ["m28c"]:
        m28c(out_en)
        return 0
    if sys.argv[1:] == ["m27"]:
        m27(out_en, out_de)
        return 0
    if sys.argv[1:] == ["m26"]:
        m26(out_en, out_de)
        return 0
    if sys.argv[1:] == ["m26b"]:
        m26b(out_en, out_de)
        return 0
    if sys.argv[1:] == ["m25c"]:
        m25c(out_en, out_de)
        return 0
    if sys.argv[1:] == ["m24"]:
        m24(out_en)
        return 0
    if sys.argv[1:] == ["m22_4"]:
        m22_4(out_en)
        return 0
    if sys.argv[1:] == ["m22_2"]:
        m22_2(out_en)
        return 0
    if sys.argv[1:] == ["m22_1"]:
        m22_1(out_en)
        return 0
    if sys.argv[1:] == ["m21_8"]:
        m21_8(out_en)
        return 0
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
