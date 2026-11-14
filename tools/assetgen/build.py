"""Regenerate every Bettacraft asset. Run from the repo root:  python tools/assetgen/build.py"""
from __future__ import annotations

import sys
import time

import blocks
import breeding
import critters
import tankmates
import cultures
import decor
import decor_icons
import equipment
import aquatic_plants
import reef
import aeration
import floating_plants
import gui
import icons
import buckets
import lang_de
import model_defs as md
import palette
import patterns
import preview
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
    written += aquatic_plants.write_all()
    written += reef.write_all()  # M20
    written += aeration.write_all()  # M23
    written += critters.write_all()
    written += tankmates.write_all()
    written += cultures.write_all()
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
