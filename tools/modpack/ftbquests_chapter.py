"""M32 part d, M45: writes the FTB Quests chapter "Bettacraft" for modpacks (modpack/config/ftbquests/quests/chapters/bettacraft.snbt).

The chapter is a progression from the first tank in branches: bettas and their breeding, water care and technology
(filters, pipes, the multiblock filter), plants, food, the reef, tankmates and livebearer genetics, small animals and
frogs, terraria, paludaria and turtles, big animals, guests (axolotls, pets, mobs, animals of other mods) and the
optional Create quests, with advancement and item tasks. Titles and descriptions are JSON text components with
translation keys (quest.bettacraft.<quest>.title / .desc), so the texts come from the mod's own en_us / de_de lang files
and every language of the mod works. FTB Quests moves the inline texts into its lang/en_us.snbt on the first load (that
is its migration of older chapter files); they stay translation keys there.

The ids are fixed (from a hash of the quest name), so a new version of the file replaces the old quests instead of adding
copies, and players keep their progress. The 33 quests of the first chapter (1.25.0) keep their ids, tasks, dependencies
and xp rewards for good (src/test/resources/quests/chapter_1.25.0.txt, QuestChapterTest); new quests may depend on them,
never the other way round, and only rewards may be added to them.

M45: every quest carries FTB Quests tags for the feature switches it belongs to (bettacraft_feature:<path>, its own and
those of its dependencies, so a quest of a switch that is off never waits for a hidden quest), bettacraft_mod:<modid> for
quests of another mod (Create) and bettacraft_needs:power for quests that only work with a power option on. The example
script modpack/kubejs/server_scripts/bettacraft_quests.js hides the quests whose tags are off (BettacraftApi.questTagsOn).
Item rewards belong to the quest's features too (the test checks it).

The file is written with commas between the entries: valid for FTB Quests and for Minecraft's SNBT parser (the unit test
QuestChapterTest reads it). Run: py tools/modpack/ftbquests_chapter.py
"""
import hashlib
import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "modpack", "config", "ftbquests", "quests", "chapters", "bettacraft.snbt")
FEATURE_JAVA = os.path.join(ROOT, "common", "src", "main", "java", "org", "overengineer", "bettacraft", "config", "Feature.java")


def qid(name):
    h = hashlib.sha1(("bettacraft:quests:" + name).encode("utf-8")).hexdigest()[:16].upper()
    return "%X" % (int(h[0], 16) & 0x7) + h[1:]  # positive as a signed long, like the ids FTB Quests makes


def adv(name):
    return {"type": "advancement", "advancement": "bettacraft:" + name}


def item(name, count=1):
    return {"type": "item", "item": "bettacraft:" + name, "count": count}


# ---------------------------------------------------------------- the feature tree (read from Feature.java)

def read_features():
    src = open(FEATURE_JAVA, encoding="utf-8").read()
    out = {}
    for m in re.finditer(r'^    ([A-Z0-9_]+)\("([^"]+)", (null|[A-Z0-9_]+), List\.of\(([^)]*)\)', src, re.M):
        name, path, parent, req = m.group(1), m.group(2), m.group(3), m.group(4)
        out[name] = {"path": path, "parent": None if parent == "null" else parent,
                     "requires": [r.strip() for r in req.split(",") if r.strip()]}
    return out


FEATURES = read_features()


def implies(a, b):
    """a on means b on (b is a parent or requirement of a, at any depth); like FeatureContent.implies."""
    f = FEATURES[a]
    if f["parent"] and (f["parent"] == b or implies(f["parent"], b)):
        return True
    return any(r == b or implies(r, b) for r in f["requires"])


def minimal(names):
    s = list(dict.fromkeys(names))
    return [f for f in s if not any(g != f and implies(g, f) for g in s)]


# ---------------------------------------------------------------- the quests

# name: (tasks, dependencies, x, y, xp, shape)
# The first 33 (the chapter of 1.25.0) keep their ids, tasks and dependencies for good; only their place, tags and
# rewards may change.
QUESTS = [
    # the first tank
    ("first_tank", [adv("build_tank")], [], 0.00, 0.00, 25, "hexagon"),
    ("fill_tank", [adv("fill_tank")], ["first_tank"], 2.00, 0.00, 25, None),
    ("equipment", [item("heater_small"), item("internal_filter"), item("tank_light")], ["fill_tank"], 4.00, 0.00, 50, None),
    # the betta
    ("catch_betta", [adv("catch_betta")], ["fill_tank"], 2.00, 2.00, 25, None),
    ("insert_betta", [adv("insert_betta")], ["catch_betta"], 4.00, 2.00, 25, None),
    ("pellets", [adv("pellets")], ["insert_betta"], 6.00, 2.00, 25, None),
    ("feeder", [adv("feeder")], ["pellets"], 8.00, 2.00, 50, None),
    # water care
    ("test_kits", [item("ph_test_kit"), item("tds_meter")], ["insert_betta"], 4.00, 6.00, 50, None),
    ("water_change", [adv("water_change")], ["test_kits"], 6.00, 6.00, 50, None),
    ("tropical", [adv("ideal_temperature")], ["equipment", "insert_betta"], 6.00, 0.00, 50, None),
    ("external_filter", [adv("filter")], ["water_change"], 8.00, 6.00, 75, None),
    ("reverse_osmosis", [item("ro_unit")], ["external_filter"], 10.00, 6.00, 100, None),
    # breeding
    ("nest", [adv("nest_built")], ["tropical"], 8.00, 0.00, 50, None),
    ("spawned", [adv("spawned")], ["nest"], 10.00, 0.00, 75, None),
    ("fry_food", [adv("fry_food")], ["spawned"], 12.00, 0.00, 75, None),
    ("fry_free", [adv("fry_free")], ["fry_food"], 14.00, 0.00, 100, None),
    ("grown_up", [adv("grown_up")], ["fry_free"], 16.00, 0.00, 100, None),
    ("variety", [adv("variety_bred")], ["grown_up"], 18.00, 0.00, 150, None),
    ("dynasty", [adv("dynasty")], ["variety"], 20.00, 0.00, 250, "hexagon"),
    # plants
    ("interior", [adv("enter_interior")], ["fill_tank"], 1.00, -2.00, 25, None),
    ("substrate", [adv("substrate")], ["interior"], 1.00, -3.50, 25, None),
    ("plants", [item("java_fern")], ["substrate"], 1.00, -5.00, 50, None),
    ("co2", [item("bio_co2")], ["plants"], 1.00, -6.50, 100, None),
    # the reef
    ("sea_water", [item("sea_salt"), item("sea_water_bucket")], ["reverse_osmosis"], 12.00, 6.00, 75, None),
    ("skimmer", [item("protein_skimmer"), item("live_rock")], ["sea_water"], 14.00, 6.00, 100, None),
    ("corals", [item("green_star_polyps")], ["skimmer"], 16.00, 6.00, 100, None),
    ("dosing", [item("dosing_pump")], ["corals"], 18.00, 6.00, 150, "hexagon"),
    # big tanks
    ("big_tank", [adv("big_tank")], ["equipment"], 4.00, -2.00, 100, None),
    ("food_maker", [item("food_maker")], ["big_tank"], 5.60, -3.50, 100, None),
    ("monitor", [item("monitor")], ["big_tank"], 4.00, -3.75, 100, "hexagon"),
    # the terrarium
    ("terrarium", [item("terrarium_box")], ["first_tank"], -3.50, -3.00, 50, None),
    ("terrarium_gear", [item("basking_lamp"), item("mister"), item("thermo_hygrometer")], ["terrarium"], -3.50, -4.50, 75, None),
    ("gecko", [item("leopard_gecko_box")], ["terrarium_gear"], -5.25, -4.50, 150, "hexagon"),

    # ------------------------------------------------------------ M45
    # betta life
    ("varied_diet", [adv("varied_diet")], ["feeder"], 10.00, 2.00, 75, None),
    ("mirror", [adv("mirror")], ["insert_betta"], 3.00, 4.50, 25, None),
    ("recovered", [adv("recovered")], ["insert_betta"], 5.00, 4.50, 50, None),
    ("magnifier", [item("magnifier")], ["insert_betta"], 6.00, 3.50, 50, None),
    ("fight", [adv("fight")], ["insert_betta"], 2.00, 3.50, 10, None),
    ("fish_bag", [item("fish_bag")], ["fill_tank"], -0.50, 3.25, 25, None),
    # equipment and technology
    ("cabinet", [adv("cabinet")], ["equipment"], 7.00, -2.00, 25, None),
    ("cooler", [adv("cooler")], ["cabinet"], 9.00, -2.00, 25, None),
    ("aeration", [item("air_pump"), item("air_stone")], ["cooler"], 11.00, -1.40, 50, None),
    ("sponge_filter", [item("sponge_filter")], ["cooler"], 11.25, -2.90, 25, None),
    ("led_lights", [item("led_panel")], ["cooler"], 10.50, -4.25, 50, None),
    ("light_timer", [adv("light_timer")], ["led_lights"], 12.25, -4.75, 25, None),
    ("regulator", [adv("regulator")], ["sponge_filter"], 13.25, -2.90, 25, None),
    ("powered", [adv("powered")], ["regulator"], 15.25, -2.90, 50, None),
    ("wound_up", [adv("wound_up")], ["regulator"], 14.75, -4.40, 50, None),
    ("pipes", [item("water_pump"), item("glass_pipe")], ["cabinet"], 6.00, -4.75, 50, None),
    ("stainless_steel", [item("crucible"), item("stainless_steel_ingot")], ["pipes"], 4.50, -5.75, 75, None),
    ("interior_pipes", [item("bulkhead"), item("spray_bar")], ["pipes"], 4.00, -7.75, 50, None),
    ("auto_water_change", [adv("auto_water_change")], ["pipes"], 5.50, -6.75, 100, None),
    ("top_off", [item("level_sensor")], ["pipes"], 7.00, -8.25, 50, None),
    ("multiblock_filter", [item("filter_chamber"), item("filter_pump_head"), item("filter_inlet"), item("filter_outlet")],
     ["cabinet"], 7.25, -6.50, 100, None),
    ("filter_lines", [item("filter_inlet_pipe"), item("filter_outlet_pipe"), item("intake_guard"), item("outlet_guard")],
     ["multiblock_filter"], 8.75, -7.50, 75, None),
    ("shared_filter", [adv("shared_filter")], ["filter_lines"], 10.75, -7.75, 150, "hexagon"),
    ("filter_water", [item("filter_fresh_port"), item("filter_waste_port")], ["filter_lines"], 8.75, -9.00, 75, None),
    ("pipe_covers", [item("pipe_cover")], ["filter_lines"], 10.50, -9.50, 25, None),
    # plants and the interior
    ("decor", [adv("decor")], ["interior"], -1.00, -3.50, 25, None),
    ("floating_plants", [item("salvinia")], ["fill_tank"], -1.00, -2.00, 25, None),
    ("algae", [adv("algae_scraped")], ["fish_bag"], -1.75, 1.75, 25, None),
    ("fertilizer", [item("fertilizer_maker"), item("liquid_fertilizer")], ["plants"], -1.00, -6.50, 75, None),
    ("fertilizer_dosing", [item("fertilizer_canister")], ["dosing"], 20.00, 6.00, 100, None),
    # food
    ("swamp_water", [adv("swamp_water")], ["first_tank"], -2.00, 0.00, 25, None),
    ("mosquito_larvae", [adv("mosquito_larvae")], ["swamp_water"], -4.00, 0.25, 25, None),
    ("food_bred", [adv("food_bred")], ["mosquito_larvae"], -6.00, 0.25, 75, None),
    ("cultures", [item("tubifex_dish"), item("daphnia_barrel")], ["food_bred"], -8.00, 0.25, 75, None),
    ("live_food_pump", [item("live_food_pump")], ["cultures"], -10.00, 0.25, 100, None),
    # tankmates and livebearers
    ("tankmates", [adv("tankmate_inserted")], ["fish_bag"], -2.50, 3.25, 25, "hexagon"),
    ("livebearer_litter", [adv("livebearer_litter")], ["tankmates"], -4.50, 2.00, 75, None),
    ("guppy_strain", [adv("guppy_strain")], ["livebearer_litter"], -6.50, 2.00, 100, None),
    ("guppy_hybrid", [adv("guppy_hybrid")], ["guppy_strain"], -8.50, 1.75, 100, None),
    ("guppy_collection", [adv("guppy_collection")], ["guppy_strain"], -8.50, 3.50, 250, "hexagon"),
    ("community_fish", [item("zebra_danio_bucket")], ["tankmates"], -2.72, 5.74, 25, None),
    ("cichlids", [item("ram_cichlid_bucket")], ["tankmates"], -4.11, 5.17, 50, None),
    ("puffers", [item("dwarf_puffer_bucket")], ["tankmates"], -4.91, 3.90, 50, None),
    ("barbs_loaches", [item("tiger_barb_bucket")], ["tankmates"], -4.37, 7.89, 25, None),
    ("gouramis", [item("honey_gourami_bucket")], ["tankmates"], -6.74, 5.90, 25, None),
    # small animals and frogs
    ("critters", [item("critter_bucket")], ["fish_bag"], -1.00, 5.25, 25, None),
    ("critters_bred", [adv("critters_bred")], ["critters"], -1.50, 7.00, 75, None),
    ("frogs", [item("dwarf_frog_bucket")], ["fish_bag"], 1.25, 4.75, 25, None),
    # the reef, more of it
    ("reef_fish", [item("ocellaris_clownfish_bucket")], ["sea_water"], 12.00, 7.50, 75, None),
    ("seahorse", [item("lined_seahorse_bucket")], ["sea_water"], 14.00, 7.50, 75, None),
    ("seahorse_fry", [adv("seahorse_fry")], ["seahorse"], 16.00, 7.50, 150, None),
    # paludarium and turtles
    ("paludarium", [adv("paludarium")], ["first_tank"], -3.00, -1.25, 50, None),
    ("turtles", [item("musk_turtle_bucket")], ["paludarium"], -5.00, -1.25, 50, None),
    ("turtle_clutch", [adv("turtle_clutch")], ["turtles"], -7.00, -1.25, 150, None),
    ("sea_turtle", [adv("sea_turtle")], ["turtles"], -7.00, -2.75, 150, None),
    ("pets", [adv("pet_terrarium")], ["paludarium"], -5.00, -2.75, 50, None),
    ("herp_clutch", [adv("herp_clutch")], ["gecko"], -7.25, -4.50, 150, None),
    # big animals
    ("big_gear", [item("large_net"), item("transport_tub")], ["big_tank"], 2.40, -3.25, 75, None),
    ("big_animal", [adv("big_animal")], ["big_gear"], 2.75, -5.25, 150, "hexagon"),
    # guests
    ("foreign_animal", [adv("foreign_animal")], ["fish_bag"], 0.50, 7.25, 50, None),
    ("mob_in_glass", [adv("mob_in_glass")], ["fish_bag"], 2.25, 8.25, 50, None),
    # Create (optional)
    ("create_drive", [adv("create_drive")], ["cabinet"], 8.75, -5.25, 100, None),
    ("travelling_tank", [adv("travelling_tank")], ["create_drive"], 10.50, -6.00, 100, None),

    # ------------------------------------------------------------ M47
    ("aquarist", [adv("aquarist_trade")], ["insert_betta"], 7.50, 4.00, 50, None),
    ("frog_tadpole", [adv("frog_tadpole")], ["frogs"], 3.00, 6.00, 100, None),

    # ------------------------------------------------------------ M48 part a
    ("bottom_fish", [item("bronze_cory_bucket")], ["tankmates"], -1.20, 6.00, 25, None),
    ("tetra_cory_form", [adv("tetra_cory_form")], ["tankmates"], -2.60, 7.40, 100, None),
    # ------------------------------------------------------------ M48 part b
    ("mandarin", [adv("mandarin_copepods")], ["reef_fish"], 12.00, 9.00, 150, None),
    ("banggai", [adv("banggai_young")], ["reef_fish"], 13.50, 9.00, 100, None),
    ("clown_larvae", [adv("clown_larvae")], ["reef_fish"], 10.50, 9.00, 250, "hexagon"),
    # ------------------------------------------------------------ M49 part a: the sump, its refugium, the calcium reactor
    ("sump", [adv("sump_installed")], ["cabinet"], 5.60, -1.00, 75, None),
    ("refugium", [item("chaetomorpha")], ["sump"], 7.00, -0.90, 50, None),
    ("calcium_reactor", [adv("calcium_reactor")], ["dosing", "sump"], 18.00, 7.50, 150, None),
    # ------------------------------------------------------------ M49 part b: brackish water
    ("archerfish", [adv("archerfish_shot")], ["tankmates"], -6.40, 4.20, 75, None),
    ("mudskipper", [adv("mudskipper_paludarium")], ["paludarium"], -8.75, -1.60, 100, None),
    ("mangrove", [adv("mangrove_grown")], ["plants"], -1.00, -5.00, 100, None),
    ("amano", [adv("amano_young")], ["critters_bred"], -1.00, 8.60, 250, "hexagon"),
    # ------------------------------------------------------------ M50: the care on the road (Create, optional)
    ("powered_trip", [adv("powered_trip")], ["travelling_tank"], 12.25, -6.75, 150, None),
    # ------------------------------------------------------------ M51: the hired aquarist
    ("aquarist_hired", [adv("aquarist_hired")], ["spawned"], 11.00, 1.10, 150, None),
    # ------------------------------------------------------------ M53: the users of a tank
    ("shared_tank", [adv("shared_tank")], ["first_tank"], 0.25, 1.75, 25, None),
]

# The feature switches of each quest (Feature constant names; the tags add those of the dependencies). None: the core of
# the mod (always there).
OWN_FEATURES = {
    "catch_betta": ["BETTAS"], "insert_betta": ["BETTAS"], "pellets": ["BETTAS"], "feeder": ["BETTAS"],
    "test_kits": ["WATER_CHEMISTRY"], "tropical": ["BETTAS"], "reverse_osmosis": ["OSMOSIS"],
    "nest": ["BREEDING"], "fry_food": ["LIVE_FOOD"],
    "interior": ["INTERIOR"], "plants": ["AQUATIC_PLANTS"], "co2": ["CO2"],
    "sea_water": ["SALTWATER"], "corals": ["CORALS"], "dosing": ["REEF_DOSING"],
    "big_tank": ["BIG_TANKS"], "food_maker": ["FOOD_MACHINE"], "monitor": ["MONITORS"],
    "terrarium": ["HERPS"], "terrarium_gear": ["TERRARIUM_DEVICES"],
    "varied_diet": ["FOOD_VARIETY"], "magnifier": ["MAGNIFIER"],
    "aeration": ["AERATION"], "sponge_filter": ["MORE_INTERNAL_FILTERS"], "led_lights": ["LED_TIERS"],
    "light_timer": ["LIGHT_TIMER"], "regulator": ["POWER_REGULATOR"], "powered": ["POWER"], "wound_up": ["WINDING"],
    "stainless_steel": ["STAINLESS_STEEL"], "pipes": ["PIPES"], "interior_pipes": ["INTERIOR_PIPES"],
    "auto_water_change": ["AUTO_WATER_CHANGE"], "top_off": ["TOP_OFF"], "multiblock_filter": ["MULTIBLOCK_FILTER"],
    "filter_lines": ["FILTER_LINES"], "filter_water": ["FILTER_WATER"], "pipe_covers": ["PIPE_COVERS"],
    "decor": ["DECOR"], "floating_plants": ["FLOATING_PLANTS"], "algae": ["ALGAE"], "fertilizer": ["FERTILIZER"],
    "fertilizer_dosing": ["FERTILIZER_DOSING"],
    "mosquito_larvae": ["LIVE_FOOD"], "food_bred": ["LIVE_FOOD"], "cultures": ["LIVE_FOOD_CULTURES"],
    "live_food_pump": ["LIVE_FOOD_PUMP"],
    "tankmates": ["TANKMATES"], "guppy_strain": ["LIVEBEARER_GENETICS"], "community_fish": ["COMMUNITY_FISH"],
    "cichlids": ["CICHLIDS"], "puffers": ["PUFFERS"], "barbs_loaches": ["BARBS_AND_LOACHES"], "gouramis": ["GOURAMIS"],
    "critters": ["INVERTEBRATES"], "frogs": ["FROGS"],
    "reef_fish": ["REEF_FISH"], "seahorse": ["SEAHORSES"],
    "paludarium": ["TERRARIUMS"], "turtles": ["TURTLES"], "turtle_clutch": ["TURTLE_BREEDING"], "sea_turtle": ["SEA_TURTLES"],
    "pets": ["VANILLA_PETS"], "herp_clutch": ["HERP_BREEDING"],
    "big_gear": ["BIG_ANIMALS"],
    "foreign_animal": ["FOREIGN_ANIMALS"], "mob_in_glass": ["MOBS_IN_GLASS"],
    "travelling_tank": ["CREATE_CONTRAPTIONS"], "create_drive": ["CREATE_DRIVE"],
    "aquarist": ["AQUARIST_VILLAGER"], "frog_tadpole": ["FROG_BREEDING"],
    "bottom_fish": ["MORE_BOTTOM_FISH"], "tetra_cory_form": ["TANKMATE_FORMS", "BREEDING"],
    "mandarin": ["REEF_ANIMALS"], "banggai": ["REEF_ANIMALS", "BREEDING"], "clown_larvae": ["LARVA_REARING"],
    "sump": ["SUMP"], "refugium": ["REFUGIUM"], "calcium_reactor": ["CALCIUM_REACTOR"], "archerfish": ["ARCHERFISH"],
    "mudskipper": ["MUDSKIPPER"], "mangrove": ["MANGROVES"], "amano": ["AMANO_SHRIMP", "BREEDING"],
    "powered_trip": ["CREATE_TRAVEL_CARE"],
    "aquarist_hired": ["AQUARIST_NPC"], "shared_tank": ["TANK_USERS"],
}

# Other mods a quest needs (hidden without them) and quests that only work with a power option of the server config on.
MODS = {"travelling_tank": ["create"], "create_drive": ["create"], "powered_trip": ["create"]}
NEEDS_POWER = {"powered", "wound_up", "powered_trip"}
# Optional by design: they never block the chapter (Create, power, the fight).
OPTIONAL = {"travelling_tank", "create_drive", "powered", "wound_up", "fight", "powered_trip"}

# Item rewards besides the xp (C2): small and of the quest's own features (QuestChapterTest checks that).
REWARDS = {
    "fill_tank": [("swamp_water_bottle", 2)],
    "pellets": [("betta_pellets", 8)],
    "feeder": [("betta_pellets", 8)],
    "test_kits": [("almond_leaf", 2)],
    "water_change": [("ph_test_kit", 1)],
    "reverse_osmosis": [("ro_prefilter", 1)],
    "spawned": [("fry_powder", 2)],
    "fry_food": [("artemia_cysts", 4)],
    "plants": [("java_moss", 2), ("anubias_nana", 1)],
    "co2": [("hemianthus_callitrichoides", 2)],
    "sea_water": [("sea_salt", 4)],
    "corals": [("zoanthus", 1)],
    "dosing": [("calcium_canister", 1)],
    "cabinet": [("tank", 4)],
    "aeration": [("air_stone", 1)],
    "sponge_filter": [("intake_sponge", 1)],
    "stainless_steel": [("stainless_steel_ingot", 2)],
    "pipes": [("glass_pipe", 8)],
    "multiblock_filter": [("filter_foam", 2), ("activated_carbon", 1)],
    "filter_lines": [("intake_guard", 1)],
    "shared_filter": [("ceramic_rings", 2)],
    "pipe_covers": [("pipe_cover", 4)],
    "decor": [("decor_driftwood", 1)],
    "floating_plants": [("duckweed", 2)],
    "algae": [("algae_wafer", 4)],
    "fertilizer": [("ludwigia_repens", 2), ("rotala_rotundifolia", 2)],
    "fertilizer_dosing": [("liquid_fertilizer", 2)],
    "mosquito_larvae": [("artemia_cysts", 4)],
    "food_bred": [("mosquito_larvae", 8)],
    "cultures": [("daphnia", 4)],
    "varied_diet": [("betta_pellets", 8)],
    "mirror": [("betta_pellets", 4)],
    "tankmates": [("fish_bag", 2)],
    "critters": [("algae_wafer", 4)],
    "critters_bred": [("algae_wafer", 8)],
    "reef_fish": [("sea_salt", 4)],
    "seahorse": [("live_rock", 1)],
    "big_gear": [("steel_leader", 1)],
    "guppy_collection": [("fish_bag", 4)],
    "aquarist": [("breeding_basin", 1)],
    "tetra_cory_form": [("spawning_mop", 1)],
    "mandarin": [("live_rock", 4)],
    "clown_larvae": [("rotifer_culture", 2)],
    "calcium_reactor": [("coral_rubble", 16)],
    "amano": [("fish_bag", 4)],
}

FIRST_CHAPTER = 33  # quests of the chapter of 1.25.0 (ids, tasks, dependencies frozen)


def text(key):
    return json.dumps({"translate": key}, separators=(",", ":"))


def s(v):
    return json.dumps(v, ensure_ascii=False)


def features_of(names):
    """Effective feature constants of each quest: its own and those of its dependencies (minimal)."""
    eff = {}
    for q in QUESTS:
        own = list(OWN_FEATURES.get(q[0], []))
        for d in q[2]:
            own += eff[d]
        eff[q[0]] = minimal(own)
    return eff


def main():
    names = [q[0] for q in QUESTS]
    assert len(set(names)) == len(names)
    for n in list(OWN_FEATURES) + list(REWARDS) + list(MODS) + list(NEEDS_POWER) + list(OPTIONAL):
        assert n in names, n
    for fs in OWN_FEATURES.values():
        for f in fs:
            assert f in FEATURES, f
    ids = [qid(n) for n in names] + [qid("chapter")]
    assert len(set(ids)) == len(ids)
    eff = features_of(names)
    places = set()
    for q in QUESTS:
        assert (q[3], q[4]) not in places, "two quests at %s" % ((q[3], q[4]),)
        places.add((q[3], q[4]))
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
            if qi < FIRST_CHAPTER:
                assert names.index(d) < FIRST_CHAPTER, "a quest of 1.25.0 never depends on a new one"
        L.append("\t\t{")
        if deps:
            L.append("\t\t\tdependencies: [%s]," % ", ".join(s(qid(d)) for d in deps))
        L.append("\t\t\tdescription: [%s]," % s(text("quest.bettacraft.%s.desc" % name)))
        L.append("\t\t\tid: %s," % s(qid(name)))
        if name in OPTIONAL:
            L.append("\t\t\toptional: true,")
        rewards = ["{ id: %s, type: \"xp\", xp: %d }" % (s(qid(name + ":xp")), xp)]
        for it, n in REWARDS.get(name, []):
            rewards.append("{ count: %d, id: %s, item: { count: 1, id: %s }, type: \"item\" }"
                           % (n, s(qid("%s:reward:%s" % (name, it))), s("bettacraft:" + it)))
        L.append("\t\t\trewards: [%s]," % ", ".join(rewards))
        if shape:
            L.append("\t\t\tshape: %s," % s(shape))
            L.append("\t\t\tsize: 1.5d,")
        tags = ["bettacraft_feature:" + FEATURES[f]["path"] for f in eff[name]]
        tags += ["bettacraft_mod:" + m for m in MODS.get(name, [])]
        if name in NEEDS_POWER:
            tags.append("bettacraft_needs:power")
        if name in OPTIONAL:
            tags.append("bettacraft_optional")
        if tags:
            L.append("\t\t\ttags: [%s]," % ", ".join(s(t) for t in tags))
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
    print("wrote %s (%d quests, %d new)" % (OUT, len(QUESTS), len(QUESTS) - FIRST_CHAPTER))
    readme_table(eff)
    if os.environ.get("PREVIEW"):
        preview(os.environ["PREVIEW"])


README = os.path.join(ROOT, "README.md")
EN = os.path.join(ROOT, "common", "src", "generated", "resources", "assets", "bettacraft", "lang", "en_us.json")


NL = chr(10)


def readme_table(eff):
    """The README's table of the quests by feature switch (between the markers quests:start / quests:end)."""
    if not os.path.exists(EN):
        return
    en = json.load(open(EN, encoding="utf-8"))
    rows = {}
    for q in QUESTS:
        own = OWN_FEATURES.get(q[0])
        if own:
            key = "`features.%s`" % FEATURES[own[0]]["path"]
        elif eff[q[0]]:
            key = " + ".join("`features.%s`" % FEATURES[f]["path"] for f in eff[q[0]])
        else:
            key = "always (the core of the mod)"
        title = en.get("quest.bettacraft.%s.title" % q[0], q[0])
        extra = []
        if q[0] in MODS:
            extra.append("needs " + ", ".join(MODS[q[0]]))
        if q[0] in NEEDS_POWER:
            extra.append("needs a power option")
        if q[0] in OPTIONAL:
            extra.append("optional")
        rows.setdefault(key, []).append(title + (" (" + "; ".join(extra) + ")" if extra else ""))
    lines = ["A quest is shown while its switch is on and also those of the quests it depends on (the tags in the chapter).", "",
             "| Switch | Quests |", "|---|---|"]
    for k in sorted(rows, key=lambda k: (not k.startswith("always"), k)):
        lines.append("| %s | %s |" % (k, ", ".join(rows[k])))
    text = open(README, encoding="utf-8").read()
    a, b = "<!-- quests:start -->", "<!-- quests:end -->"
    if a not in text or b not in text:
        return
    i, j = text.index(a) + len(a), text.index(b)
    text = text[:i] + NL + NL.join(lines) + NL + text[j:]
    open(README, "w", encoding="utf-8", newline=NL).write(text)


def preview(path):
    """A rough picture of the layout (nodes and dependency lines) for checking it without the game."""
    from PIL import Image, ImageDraw
    xs = [q[3] for q in QUESTS]
    ys = [q[4] for q in QUESTS]
    sc = 40
    ox, oy = -min(xs) + 1.5, -min(ys) + 1.5
    w, h = int((max(xs) - min(xs) + 3) * sc), int((max(ys) - min(ys) + 3) * sc)
    im = Image.new("RGB", (w, h), (30, 30, 40))
    d = ImageDraw.Draw(im)
    pos = {q[0]: ((q[3] + ox) * sc, (q[4] + oy) * sc) for q in QUESTS}
    for q in QUESTS:
        for dep in q[2]:
            d.line([pos[dep], pos[q[0]]], fill=(120, 120, 160), width=2)
    for q in QUESTS:
        x, y = pos[q[0]]
        r = sc * (0.75 if q[6] else 0.5) * 0.8
        col = (200, 150, 60) if q[0] in OPTIONAL else (90, 170, 90) if QUESTS.index(q) < FIRST_CHAPTER else (80, 140, 210)
        d.ellipse([x - r, y - r, x + r, y + r], fill=col)
        d.text((x - r, y + r), q[0][:12], fill=(230, 230, 230))
    im.save(path)


if __name__ == "__main__":
    main()
