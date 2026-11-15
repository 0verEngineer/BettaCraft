"""M32 part d: writes the FTB Quests chapter "Bettacraft" for modpacks (modpack/config/ftbquests/quests/chapters/bettacraft.snbt).

The chapter is a progression: first tank -> betta -> water care -> breeding -> plants -> reef -> big tanks -> terrarium,
with advancement and item tasks. Titles and descriptions are JSON text components with translation keys
(quest.bettacraft.<quest>.title / .desc), so the texts come from the mod's own en_us / de_de lang files and every
language of the mod works. FTB Quests moves the inline texts into its lang/en_us.snbt on the first load (that is its
migration of older chapter files); they stay translation keys there.

The ids are fixed (from a hash of the quest name), so a new version of the file replaces the old quests instead of adding
copies. The file is written with commas between the entries: valid for FTB Quests and for Minecraft's SNBT parser (the
unit test QuestChapterTest reads it). Run: py tools/modpack/ftbquests_chapter.py
"""
import hashlib
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "modpack", "config", "ftbquests", "quests", "chapters", "bettacraft.snbt")


def qid(name):
    h = hashlib.sha1(("bettacraft:quests:" + name).encode("utf-8")).hexdigest()[:16].upper()
    return "%X" % (int(h[0], 16) & 0x7) + h[1:]  # positive as a signed long, like the ids FTB Quests makes


def adv(name):
    return {"type": "advancement", "advancement": "bettacraft:" + name}


def item(name, count=1):
    return {"type": "item", "item": "bettacraft:" + name, "count": count}


# name, tasks, dependencies, x, y, xp, shape
QUESTS = [
    # the first tank
    ("first_tank", [adv("build_tank")], [], 0.0, 0.0, 25, "hexagon"),
    ("fill_tank", [adv("fill_tank")], ["first_tank"], 2.0, 0.0, 25, None),
    ("equipment", [item("heater_small"), item("internal_filter"), item("tank_light")], ["fill_tank"], 4.0, 0.0, 50, None),
    # the betta
    ("catch_betta", [adv("catch_betta")], ["fill_tank"], 2.0, 2.0, 25, None),
    ("insert_betta", [adv("insert_betta")], ["catch_betta"], 4.0, 2.0, 25, None),
    ("pellets", [adv("pellets")], ["insert_betta"], 6.0, 2.0, 25, None),
    ("feeder", [adv("feeder")], ["pellets"], 8.0, 2.0, 50, None),
    # water care
    ("test_kits", [item("ph_test_kit"), item("tds_meter")], ["insert_betta"], 4.0, 4.0, 50, None),
    ("water_change", [adv("water_change")], ["test_kits"], 6.0, 4.0, 50, None),
    ("tropical", [adv("ideal_temperature")], ["equipment", "insert_betta"], 6.0, 0.0, 50, None),
    ("external_filter", [adv("filter")], ["water_change"], 8.0, 4.0, 75, None),
    ("reverse_osmosis", [item("ro_unit")], ["external_filter"], 10.0, 4.0, 100, None),
    # breeding
    ("nest", [adv("nest_built")], ["tropical"], 8.0, 0.0, 50, None),
    ("spawned", [adv("spawned")], ["nest"], 10.0, 0.0, 75, None),
    ("fry_food", [adv("fry_food")], ["spawned"], 12.0, 0.0, 75, None),
    ("fry_free", [adv("fry_free")], ["fry_food"], 14.0, 0.0, 100, None),
    ("grown_up", [adv("grown_up")], ["fry_free"], 16.0, 0.0, 100, None),
    ("variety", [adv("variety_bred")], ["grown_up"], 18.0, 0.0, 150, None),
    ("dynasty", [adv("dynasty")], ["variety"], 20.0, 0.0, 250, "hexagon"),
    # plants
    ("interior", [adv("enter_interior")], ["fill_tank"], 2.0, -2.0, 25, None),
    ("substrate", [adv("substrate")], ["interior"], 4.0, -2.0, 25, None),
    ("plants", [item("java_fern")], ["substrate"], 6.0, -2.0, 50, None),
    ("co2", [item("bio_co2")], ["plants"], 8.0, -2.0, 100, None),
    # the reef
    ("sea_water", [item("sea_salt"), item("sea_water_bucket")], ["reverse_osmosis"], 12.0, 4.0, 75, None),
    ("skimmer", [item("protein_skimmer"), item("live_rock")], ["sea_water"], 14.0, 4.0, 100, None),
    ("corals", [item("green_star_polyps")], ["skimmer"], 16.0, 4.0, 100, None),
    ("dosing", [item("dosing_pump")], ["corals"], 18.0, 4.0, 150, "hexagon"),
    # big tanks
    ("big_tank", [adv("big_tank")], ["equipment"], 4.0, 6.0, 100, None),
    ("food_maker", [item("food_maker")], ["big_tank"], 6.0, 6.0, 100, None),
    ("monitor", [item("monitor")], ["big_tank"], 6.0, 7.5, 100, "hexagon"),
    # the terrarium
    ("terrarium", [item("terrarium_box")], ["first_tank"], 0.0, -4.0, 50, None),
    ("terrarium_gear", [item("basking_lamp"), item("mister"), item("thermo_hygrometer")], ["terrarium"], 2.0, -4.0, 75, None),
    ("gecko", [item("leopard_gecko_box")], ["terrarium_gear"], 4.0, -4.0, 150, "hexagon"),
]


def text(key):
    return json.dumps({"translate": key}, separators=(",", ":"))


def s(v):
    return json.dumps(v, ensure_ascii=False)


def main():
    names = [q[0] for q in QUESTS]
    assert len(set(names)) == len(names)
    ids = [qid(n) for n in names] + [qid("chapter")]
    assert len(set(ids)) == len(ids)
    L = []
    L.append("{")
    L.append("\tdefault_hide_dependency_lines: false,")
    L.append("\tdefault_quest_shape: \"\",")
    L.append("\tfilename: \"bettacraft\",")
    L.append("\tgroup: \"\",")
    L.append("\ticon: { count: 1, id: \"bettacraft:betta_bucket\" },")
    L.append("\tid: %s," % s(qid("chapter")))
    L.append("\timages: [ ],")
    L.append("\torder_index: 0,")
    L.append("\tquest_links: [ ],")
    L.append("\tquests: [")
    for qi, (name, tasks, deps, x, y, xp, shape) in enumerate(QUESTS):
        for d in deps:
            assert d in names[:qi], name + " needs " + d + " before it"
        L.append("\t\t{")
        if deps:
            L.append("\t\t\tdependencies: [%s]," % ", ".join(s(qid(d)) for d in deps))
        L.append("\t\t\tdescription: [%s]," % s(text("quest.bettacraft.%s.desc" % name)))
        L.append("\t\t\tid: %s," % s(qid(name)))
        L.append("\t\t\trewards: [{ id: %s, type: \"xp\", xp: %d }]," % (s(qid(name + ":xp")), xp))
        if shape:
            L.append("\t\t\tshape: %s," % s(shape))
            L.append("\t\t\tsize: 1.5d,")
        L.append("\t\t\ttasks: [")
        for ti, t in enumerate(tasks):
            tid = s(qid("%s:task%d" % (name, ti)))
            sep = "," if ti < len(tasks) - 1 else ""
            if t["type"] == "advancement":
                L.append("\t\t\t\t{ advancement: %s, criterion: \"\", id: %s, type: \"advancement\" }%s" % (s(t["advancement"]), tid, sep))
            else:
                L.append("\t\t\t\t{ id: %s, item: { count: %d, id: %s }, type: \"item\" }%s" % (tid, t["count"], s(t["item"]), sep))
        L.append("\t\t\t],")
        L.append("\t\t\ttitle: %s," % s(text("quest.bettacraft.%s.title" % name)))
        L.append("\t\t\tx: %.1fd," % x)
        L.append("\t\t\ty: %.1fd" % y)
        L.append("\t\t}" + ("," if qi < len(QUESTS) - 1 else ""))
    L.append("\t],")
    L.append("\tsubtitle: [%s]," % s(text("quest.bettacraft.chapter.subtitle")))
    L.append("\ttitle: %s" % s(text("quest.bettacraft.chapter.title")))
    L.append("}")
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(L) + "\n")
    print("wrote %s (%d quests)" % (OUT, len(QUESTS)))


if __name__ == "__main__":
    main()
