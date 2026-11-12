"""Keeps the generated parts of de_de.json (colour, variety and fin-form names) in sync with the Python tables.

de_de.json is maintained by hand; this script only replaces/inserts the keys it owns and keeps everything else
(order and content) as it is. Run from the repo root:  python tools/assetgen/lang_de.py
"""
from __future__ import annotations

import json

import model_defs as md
import palette
import varieties
from common import RES

DE = RES / "assets" / "bettacraft" / "lang" / "de_de.json"


def owned() -> dict:
    out = {}
    for cid, _en, de, _hx in palette.COLORS:
        out[f"color.bettacraft.{cid}"] = de
    for s in md.SHAPES:
        out[f"shape.bettacraft.{s}"] = md.SHAPE_NAMES[s][1]
    for v in varieties.VARIETIES:
        out[f"variety.bettacraft.{v[0]}"] = v[5]
    return out


def write(extra: dict | None = None, remove: tuple = ()) -> None:
    d = json.loads(DE.read_text(encoding="utf-8"))
    for k in remove:
        d.pop(k, None)
    for k, v in {**owned(), **(extra or {})}.items():
        d[k] = v
    DE.write_text(json.dumps(d, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")


if __name__ == "__main__":
    write()
    print("updated", DE)
