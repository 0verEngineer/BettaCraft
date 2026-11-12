"""Named betta varieties ("types") - single source of truth for the Java table BettaVarietyTable.

A variety is a rule on the *phenotype*: for each listed locus the expressed allele must be one of the given ids
(missing locus = any). The first matching variety (highest priority) names the colour part of the type; the
fin form part ("Halfmoon Plakat", "Dumbo Veiltail" ...) is always derived from finform / fin_length / pectoral.
Without a match the colour part is generic ("Solid Red", "Red/Blue Bicolor", "Red Butterfly", "Metallic ...").

Each variety also has an ``example`` genome: the type preview sheet renders it, the creative tab offers it as a
bucket and a unit test checks that every example is named by its own variety.

Real betta variety names (Mustard Gas, Black Orchid, Koi Galaxy, Nemo, Samurai, Cambodian ...) are common
hobby terms; the rules are simplified to the colours of our palette.
"""
from __future__ import annotations

from common import JAVA_SRC

DARK_COOL = ["navy", "royal_blue", "cobalt", "indigo", "steel", "teal", "emerald", "green", "black", "black_orchid"]
YELLOWS = ["yellow", "mustard", "gold", "pineapple", "orange", "tangerine"]
REDS = ["red", "crimson", "ruby", "oxblood", "wine", "coral"]
PALES = ["cream", "cellophane", "pastel", "salmon", "pink", "white", "platinum"]
BLUES = ["navy", "royal_blue", "cobalt", "steel", "sky", "indigo", "turquoise"]
BLACKS = ["black", "black_orchid"]
WILD_BODY = ["olive", "bronze", "chocolate", "smoke", "oxblood", "copper", "cinnamon"]


def _ex(body, fins, pattern="none", pc="black", irid="low", ic="turquoise", finform="halfmoon", length="long",
        pectoral="normal"):
    return {"finform": finform, "fin_length": length, "pectoral": pectoral, "body_color": body, "fin_color": fins,
            "pattern": pattern, "pattern_color": pc, "iridescence": irid, "iridescence_color": ic}


# (id, priority, include_form, conditions, en, de, example)
VARIETIES = [
    ("wild", 100, False,
     {"finform": ["round"], "fin_length": ["short"], "pectoral": ["normal"], "body_color": WILD_BODY,
      "pattern": ["none", "fade"]},
     "Wild Type", "Wildform", _ex("olive", "oxblood", irid="low", ic="teal", finform="round", length="short")),
    ("koi_galaxy", 80, True, {"pattern": ["koi"], "iridescence": ["high"]},
     "Koi Galaxy", "Koi Galaxy", _ex("white", "white", "koi", "red", "high", "royal_blue", "halfmoon", "short")),
    ("nemo", 75, True,
     {"pattern": ["koi", "marble"], "body_color": ["orange", "tangerine", "yellow", "gold", "red", "coral"],
      "pattern_color": ["black", "red", "crimson", "orange", "black_orchid"]},
     "Nemo", "Nemo", _ex("orange", "tangerine", "koi", "black", "low", "steel", "halfmoon", "short")),
    ("samurai", 75, True,
     {"pattern": ["dragon"], "body_color": BLACKS + ["navy", "indigo"], "pattern_color": ["silver", "platinum", "white"]},
     "Samurai", "Samurai", _ex("black", "black", "dragon", "platinum", "high", "steel", "halfmoon", "short")),
    ("black_orchid", 70, True,
     {"body_color": BLACKS, "fin_color": BLACKS + ["purple", "indigo", "navy", "violet"],
      "pattern": ["none", "butterfly", "tipped"], "iridescence": ["high"],
      "iridescence_color": ["royal_blue", "steel", "lavender", "sky", "turquoise"]},
     "Black Orchid", "Black Orchid", _ex("black_orchid", "black", "butterfly", "royal_blue", "high", "royal_blue")),
    ("mustard_gas", 65, True,
     {"body_color": DARK_COOL, "fin_color": YELLOWS, "pattern": ["none", "tipped", "butterfly", "fade"]},
     "Mustard Gas", "Mustard Gas", _ex("navy", "mustard", "none", "black", "high", "royal_blue")),
    ("thai_flag", 62, True,
     {"body_color": BLUES, "fin_color": REDS, "pattern": ["butterfly", "tipped"],
      "pattern_color": ["white", "cream", "platinum", "pastel"]},
     "Thai Flag", "Thai-Flagge", _ex("royal_blue", "red", "butterfly", "white", "low", "steel", "crowntail")),
    ("cambodian", 60, True, {"body_color": PALES, "fin_color": REDS, "pattern": ["none", "fade", "tipped"]},
     "Cambodian", "Cambodian", _ex("cream", "red", "none", irid="none", finform="veil")),
    ("chocolate", 58, True,
     {"body_color": ["chocolate", "cinnamon", "bronze", "copper"], "fin_color": YELLOWS, "pattern": ["none", "fade", "tipped"]},
     "Chocolate", "Chocolate", _ex("chocolate", "orange", irid="low", ic="steel", finform="delta")),
    ("pineapple", 56, True,
     {"body_color": ["yellow", "pineapple", "gold", "mustard"], "pattern": ["dragon", "grizzle"],
      "pattern_color": ["chocolate", "cinnamon", "black", "copper", "bronze"]},
     "Pineapple", "Pineapple", _ex("pineapple", "yellow", "dragon", "cinnamon", "none", finform="spade")),
    ("full_mask", 55, True, {"pattern": ["mask"], "iridescence": ["high"]},
     "Full Mask", "Full Mask", _ex("royal_blue", "royal_blue", "mask", "turquoise", "high", "turquoise", "rosetail")),
    ("hellboy", 52, True,
     {"body_color": REDS, "fin_color": REDS, "pattern": ["dragon"], "pattern_color": ["black", "oxblood", "black_orchid"]},
     "Hellboy", "Hellboy", _ex("red", "crimson", "dragon", "black", "high", "steel", "halfmoon", "short")),
    ("super_red", 50, True, {"body_color": ["red", "crimson", "ruby"], "fin_color": ["red", "crimson", "ruby"],
                             "pattern": ["none"]},
     "Super Red", "Super Red", _ex("red", "red", irid="none", finform="halfmoon")),
    ("platinum_white", 50, True,
     {"body_color": ["white", "platinum"], "fin_color": ["white", "platinum"], "pattern": ["none", "galaxy"]},
     "Platinum White", "Platinum White", _ex("white", "platinum", irid="low", ic="platinum", finform="crowntail")),
    ("melano", 48, True, {"body_color": ["black"], "fin_color": ["black"], "pattern": ["none"]},
     "Melano Black", "Melano", _ex("black", "black", irid="none", finform="doubletail")),
    ("avatar", 46, True,
     {"pattern": ["galaxy"], "iridescence": ["high"], "body_color": DARK_COOL + BLUES},
     "Avatar", "Avatar", _ex("teal", "turquoise", "galaxy", "mint", "high", "emerald", "halfmoon", "short")),
]

# Forms shown in the preview: (finform, fin_length)
PREVIEW_FORMS = [(s, "long") for s in ["round", "spade", "veil", "delta", "halfmoon", "rosetail", "crowntail", "doubletail"]] + \
                [("round", "short"), ("halfmoon", "short"), ("crowntail", "short"), ("doubletail", "short")]


def matches(v, genome: dict) -> bool:
    cond = v[3]
    return all(genome.get(k) in allowed for k, allowed in cond.items())


def name_of(genome: dict) -> str | None:
    for v in sorted(VARIETIES, key=lambda v: (-v[1], v[0])):
        if matches(v, genome):
            return v[0]
    return None


def _jstr(t: str) -> str:
    return '"' + t.replace("\\", "\\\\").replace('"', '\\"') + '"'


def _jmap(d: dict) -> str:
    items = ", ".join(f"{_jstr(k)}, java.util.List.of({', '.join(_jstr(a) for a in v)})" for k, v in d.items())
    if len(d) <= 10:
        return f"java.util.Map.of({items})"
    raise ValueError("too many conditions")


def _jgenome(d: dict) -> str:
    return "java.util.Map.of(" + ", ".join(f"{_jstr(k)}, {_jstr(v)}" for k, v in d.items()) + ")"


def write_java():
    for v in VARIETIES:
        assert matches(v, v[6]), f"example of {v[0]} does not match its own rule"
        assert name_of(v[6]) == v[0], f"example of {v[0]} is named {name_of(v[6])}"
    lines = [
        "// GENERATED by tools/assetgen/varieties.py - do not edit by hand, edit varieties.py and rerun build.py",
        "package org.overengineer.bettacraft.genetics;",
        "",
        "/** Built-in named betta varieties (defaults of data/bettacraft/bettacraft/varieties/*.json). */",
        "public final class BettaVarietyTable {",
        "    /**",
        "     * @param conditions locus id -> allowed expressed alleles",
        "     * @param example    a phenotype of this variety (locus id -> allele)",
        "     */",
        "    public record Entry(String id, int priority, boolean includeForm, java.util.Map<String, java.util.List<String>> conditions,",
        "                        String en, String de, java.util.Map<String, String> example) {}",
        "",
        "    public static final java.util.List<Entry> VARIETIES = java.util.List.of(",
    ]
    for k, (i, prio, form, cond, en, de, ex) in enumerate(VARIETIES):
        sep = "," if k < len(VARIETIES) - 1 else ");"
        lines.append(f"            new Entry({_jstr(i)}, {prio}, {str(form).lower()}, {_jmap(cond)},")
        lines.append(f"                    {_jstr(en)}, {_jstr(de)}, {_jgenome(ex)}){sep}")
    lines += ["", "    private BettaVarietyTable() {}", "}"]
    p = JAVA_SRC / "genetics" / "BettaVarietyTable.java"
    p.write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")
    return p


if __name__ == "__main__":
    print("wrote", write_java())
