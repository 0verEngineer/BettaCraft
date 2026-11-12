"""Named betta colours (tint colours for the greyscale masks).

Written to build/generated/colors.json; the Java side turns these into datapack colour files in M1.
"""
from __future__ import annotations

import json

from common import GEN

# (id, english name, german name, hex)
COLORS = [
    ("red", "Red", "Rot", "#c8202a"),
    ("crimson", "Crimson", "Karmesin", "#a3132b"),
    ("oxblood", "Oxblood", "Ochsenblut", "#6e1018"),
    ("wine", "Wine", "Weinrot", "#7d1f3d"),
    ("ruby", "Ruby", "Rubin", "#b3123f"),
    ("coral", "Coral", "Koralle", "#f2665a"),
    ("salmon", "Salmon", "Lachs", "#f29175"),
    ("orange", "Orange", "Orange", "#ef6d12"),
    ("tangerine", "Tangerine", "Mandarine", "#f8942a"),
    ("copper", "Copper", "Kupfer", "#b8693b"),
    ("bronze", "Bronze", "Bronze", "#9c7a3c"),
    ("gold", "Gold", "Gold", "#e0b235"),
    ("mustard", "Mustard Gas", "Senfgas", "#c9b22c"),
    ("yellow", "Yellow", "Gelb", "#f4d03f"),
    ("pineapple", "Pineapple", "Ananas", "#f6e27a"),
    ("cream", "Cream", "Creme", "#f1e6c8"),
    ("white", "Opaque White", "Deckweiß", "#f4f3ee"),
    ("cellophane", "Cellophane", "Cellophan", "#e8cfc6"),
    ("pastel", "Pastel", "Pastell", "#dfe6ee"),
    ("platinum", "Platinum", "Platin", "#cfd6d8"),
    ("silver", "Silver", "Silber", "#a9b3b8"),
    ("smoke", "Smoke Grey", "Rauchgrau", "#7a7f86"),
    ("black", "Melano Black", "Melano-Schwarz", "#1c1b20"),
    ("black_orchid", "Black Orchid", "Schwarze Orchidee", "#2d1d33"),
    ("chocolate", "Chocolate", "Schokolade", "#5b3423"),
    ("cinnamon", "Cinnamon", "Zimt", "#8f4f2a"),
    ("olive", "Wild Olive", "Wildoliv", "#6b6a3a"),
    ("emerald", "Emerald", "Smaragd", "#1f9a5b"),
    ("green", "Green", "Grün", "#3aa04a"),
    ("mint", "Mint", "Minze", "#8fd8b8"),
    ("teal", "Teal", "Petrol", "#13817f"),
    ("turquoise", "Turquoise", "Türkis", "#2ec4c4"),
    ("sky", "Sky Blue", "Himmelblau", "#6cb7ec"),
    ("steel", "Steel Blue", "Stahlblau", "#5a7fa8"),
    ("royal_blue", "Royal Blue", "Königsblau", "#1f4fb5"),
    ("cobalt", "Cobalt", "Kobalt", "#1d3f8f"),
    ("navy", "Navy", "Marineblau", "#18275c"),
    ("indigo", "Indigo", "Indigo", "#3b2a86"),
    ("lavender", "Lavender", "Lavendel", "#b49bd8"),
    ("purple", "Purple", "Lila", "#7a36a8"),
    ("violet", "Violet", "Violett", "#9b3fc8"),
    ("magenta", "Magenta", "Magenta", "#c42f8e"),
    ("pink", "Pink", "Rosa", "#ef8fb8"),
]

# Subset that looks good as iridophore sheen colours.
IRIDESCENT = ["turquoise", "sky", "steel", "royal_blue", "emerald", "mint", "platinum", "teal", "lavender"]

BY_ID = {c[0]: c for c in COLORS}


def hex_of(cid: str) -> str:
    return BY_ID[cid][3]


def write_json():
    assert len({c[0] for c in COLORS}) == len(COLORS)
    data = {"colors": [{"id": i, "name": {"en_us": en, "de_de": de}, "hex": hx} for i, en, de, hx in COLORS],
            "iridescent": IRIDESCENT}
    GEN.mkdir(parents=True, exist_ok=True)
    p = GEN / "colors.json"
    p.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")
    return p


if __name__ == "__main__":
    print("wrote", write_json(), f"({len(COLORS)} colours)")
