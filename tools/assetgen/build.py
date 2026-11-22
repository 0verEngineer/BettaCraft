"""Regenerate every Bettacraft asset. Run from the repo root:  python tools/assetgen/build.py"""
from __future__ import annotations

import sys
import time

import blocks
import breeding
import critters
import tankmates
import cultures
import create_drive
import decor
import decor_icons
import equipment
import aquatic_plants
import reef
import plant_icons
import aeration
import plumbing
import filter_lines
import chemistry
import co2_osmosis
import reef_dosing
import sump_brackish
import live_food
import food_maker
import monitor
import block_items
import big_animals
import terrarium
import terrarium_animals
import floating_plants
import wild_plants
import animal_icons
import gui
import icons
import buckets
import lang_de
import model_defs as md
import palette
import patterns
import preview
import aquarist
import hired_aquarist
import uv_template
import varieties
from common import GEN, ensure_dirs, rel


def main() -> int:
    t0 = time.time()
    ensure_dirs()
    written = []
    written += uv_template.write_template()
    written += uv_template.write_region_masks()
    written += patterns.write_all()
    written.append(palette.write_json())
    written.append(palette.write_java())
    written.append(varieties.write_java())
    lang_de.write()
    written.append(lang_de.DE)
    written += icons.write_all()
    written += buckets.write_all()  # M21
    written += breeding.write_all()
    written += decor.write_all()
    written += decor_icons.write_all()
    written += blocks.write_all()
    written += equipment.write_all()
    written += floating_plants.write_all()
    written += wild_plants.write_all()  # M28.1: after the floating plants (it composes their textures)
    written += aquatic_plants.write_all()
    written += reef.write_all()  # M20
    written += plant_icons.write_all()  # M62: an own icon per aquarium plant and coral
    written += aeration.write_all()  # M23
    written += plumbing.write_all()  # M24
    written += filter_lines.write_all()  # M40
    written += create_drive.write_all()  # M41
    written += chemistry.write_all()  # M25
    written += co2_osmosis.write_all()  # M25 part b
    written += reef_dosing.write_all()  # M25 part c
    written += sump_brackish.write_all()  # M49
    written += live_food.write_all()  # M26 part a
    written += food_maker.write_all()  # M26 part b
    written += monitor.write_all()  # M27
    written += block_items.write_all()  # M29.1
    written += big_animals.write_all()  # M30
    written += terrarium.write_all()  # M31
    written += terrarium_animals.write_all()
    written += critters.write_all()
    written += tankmates.write_all()
    written += animal_icons.write_all()  # M55: after the tankmates, the buckets and the tub
    written += cultures.write_all()
    written += aquarist.write_all()  # M47
    written += hired_aquarist.write_all()  # M51
    written += gui.write_all()
    written += md.write_generated(GEN, preview.sample_textures())
    written += preview.write_all()
    for p in written:
        print("  ", rel(p))
    (GEN / "uv_layout.txt").write_text(md.layout_summary() + "\n", encoding="utf-8", newline="\n")
    print(f"{len(written)} files written in {time.time() - t0:.1f}s")
    return 0


if __name__ == "__main__":
    sys.exit(main())
