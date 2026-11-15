---
navigation:
  title: Profile für Tiere anderer Mods
  icon: minecraft:tropical_fish_bucket
  parent: settings.md
  position: 20
feature: foreignAnimals.enabled
---

# Profile für Tiere anderer Mods

Ein **Profil** gibt einem Tier einer anderen Mod (oder aus Vanilla) im Bettacraft-Aquarium unsere Pflege und unsere Zucht.
Außerhalb des Aquariums ändert sich nichts: Dort lebt das Tier nach seiner eigenen Mod.

## Was ein Profil bewirkt

* **Pflege** (`features.foreignAnimals.care`): Das Tier bekommt Hunger wie ein Betta (es frisst das Futter des Beckens: von Hand,
  aus dem Futterautomaten, aus der Lebendfutter-Pumpe), achtet auf seinen Temperaturbereich, die Wasserqualität, pH und
  TDS, den Besatz und (mit `saltwater.foreignAnimals = REALISTIC`) den Salzgehalt. Tage mit Problemen machen es krank, ein krankes Tier, dem nicht geholfen wird,
  stirbt (Logbuch mit Ursache). Ein gefüttertes Tier heilt. Das Beckenmenü zeigt seine Gesundheit und seinen Hunger.
* **Zucht** (`features.foreignAnimals.breeding`): Zwei ausgewachsene, satte, gesunde Tiere derselben Art in Wasser ihrer
  Zuchttemperatur vermehren sich hin und wieder. Ein **Lebendgebärender** trägt die Jungen aus und bringt sie zur Welt,
  ein **Eierleger** laicht und die Eier schlüpfen (manche brauchen Pflanzen, Speisefische ein großes Aquarium). Die
  Jungen erben die Varianten ihrer Eltern (Farben, Muster: was ihre Mod über sie speichert), je Merkmal den Wert eines
  Elternteils.
* **Ihre eigene Zucht unterdrückt** (`features.foreignAnimals.suppressOwnBreeding`, für jedes Tier aus Vanilla und anderen Mods im
  Aquarium): keine Paarungsbereitschaft durch Futter, keine Jungen aus der Zucht ihrer Mod. Unsere eigene Zucht von
  Axolotln, Schildkröten und den Mobs im Glas bleibt davon unberührt.

## Die mitgelieferten Profile

| Mod | Tiere | Wie | Vermehrung |
|---|---|---|---|
| Fintastic | Guppy | unser [Guppy](../tankmates.md) | lebendgebärend, Flossen- und Schwanzfarben werden vererbt |
| Fintastic | Gurami | unser Zwergfadenfisch | Eierleger, Größe und Haut werden vererbt (der Riesengurami ist zu groß) |
| Fintastic | Pleco, kleiner Wels | Antennenwels, Panda-Panzerwels | Eierleger |
| Fintastic | Elritze | kühles Wasser (14 bis 26 °C) | Eier an Pflanzen |
| Fintastic | Moony | Brackwasser | nein |
| Fintastic, Aquaculture, Naturalist | Kabeljau, Lachs, Hering, Sonnenbarsch, Flussbarsch, Barsch, Forelle, Boulti, Wels, Piranha | Speisefische | nur in Aquarien ab 24 Wasserblöcken (Piranha 16) |
| Aquaculture | Synodontis | Wels | nein |
| Naturalist | [Landschildkröte](../terrarium/naturalist_tortoise.md) | trockenes Terrarium, 1 m, Grünzeug | Eier in Sand oder Erde |
| Naturalist | [Alligator](../terrarium/naturalist_alligator.md) | großes Paludarium, 2 m, rohes Fleisch, jagt kleine Fische | Eier in Sand oder Erde |
| Naturalist | [Echse, Schlange](../terrarium/foreign_land_animals.md) | trockenes Terrarium, Heimchen und Grünzeug / Beute | Eier in Sand oder Erde |
| Naturalist | [Wüsten- und Dschungelskorpion](../terrarium/foreign_land_animals.md) | trockenes Terrarium (trockene / feuchte Luft), Heimchen | lebende Junge |
| Naturalist | [Schnecke](../terrarium/foreign_land_animals.md) | feuchtes Terrarium, Grünzeug | Eier in Sand oder Erde |
| Naturalist | [Krabbe](../terrarium/foreign_land_animals.md) | Paludarium, Süß- oder Meerwasser, Pellets und Fisch | nein |
| Naturalist, Critters and Companions | [Libelle](../terrarium/foreign_land_animals.md) | Paludarium, fliegt, Fruchtfliegen | nein |
| Naturalist | [Ente, Capybara](../terrarium/foreign_land_animals.md) | großes Paludarium, 0,6 m / 1,2 m | Eier in Sand oder Erde / lebende Junge |
| Critters and Companions | [Wandelndes Blatt](../terrarium/foreign_land_animals.md) | feuchtes Terrarium, Laub | Eier in Sand oder Erde |
| Critters and Companions | [Otter](../terrarium/foreign_land_animals.md) | großes Paludarium, 1 m, Fisch, jagt kleine Fische | lebende Junge |
| Critters and Companions | Seehase | Meerwasseraquarium, Pellets | nein |
| Critters and Companions | Koi | Teichfisch (4 bis 28 °C) | Eier an Pflanzen, 16 Wasserblöcke |

## Ein Profil schreiben

Profile sind Daten: `data/<namespace>/bettacraft/foreign_animals/<name>.json` in einem Datapack (oder einem
KubeJS-Datenordner), neu gelesen mit `/reload`. Eine Datei für dasselbe Entity in einem späteren Pack ersetzt die frühere.
Entities, deren Mod nicht installiert ist, werden still übersprungen (außer mit `"required": true`). Beispiel:

```json
{
  "entity": ["somemod:neon", "somemod:cardinal"],
  "species": "neon_tetra",
  "group": "tetra",
  "care": {"temperature": [22, 27], "min_quality": 0.5, "salinity": "fresh", "ph": [5.5, 7.5], "tds": [0, 300], "stock": 0.35},
  "food": ["normal", "live", "fry", "#somemod:fish_food"],
  "breeding": {
    "type": "eggs", "temperature": [24, 26], "needs_plants": true, "min_water_blocks": 0,
    "days": 2, "young": [2, 5], "pause_days": 3, "rate": 0.5,
    "inherit": ["Variant"], "offspring_nbt": "{FromBucket:1b}", "baby": true
  },
  "suppress_own_breeding": true
}
```

| Schlüssel | Bedeutung |
|---|---|
| `entity` | eine Entity-ID oder eine Liste |
| `species` | optional: eine unserer Arten (`guppy`, `dwarf_gourami`, `neon_tetra` …), deren Werte es übernimmt; jeder Schlüssel der Datei geht vor |
| `group` | eine Bezeichnung (`livebearer`, `pond_fish`, `food_fish` …) |
| `care.temperature`, `min_quality`, `salinity` | Bereich in °C, niedrigste Wasserqualität (0 bis 1), `fresh`, `brackish`, `brackish_only`, `marine`, `hardy_marine`, `any` oder `[min, max]` in ppt |
| `care.ph`, `care.tds` | sein pH- und TDS-Bereich (weggelassen: pH und TDS sind egal, außer die Art hat einen) |
| `care.labyrinth`, `hunger_days`, `stock` | atmet Luft (wenig Sauerstoff schadet nicht), Tage bis zum Hunger (1 wie ein Betta), wie viel es für den Besatz zählt |
| `food` | `normal` (Pellets, Flocken …), `live` (Lebend- und Frostfutter), `fry` (Jungfischfutter; Jungtiere fressen es immer), `#item_tag` oder eine Item-ID |
| `breeding.type` | `livebearer`, `eggs` oder `none` |
| `breeding.temperature`, `needs_plants`, `min_water_blocks` | wann sich ein Paar vermehrt |
| `breeding.days`, `young`, `pause_days`, `rate` | Tage bis zu den Jungen, Junge pro Brut, Pause der Mutter, Bruten pro Paar und Tag |
| `breeding.offspring`, `offspring_nbt` | das Entity der Jungen (Standard: dasselbe) und NBT für jedes Junge |
| `breeding.inherit` | NBT-Schlüssel, die jedes Junge von einem seiner Eltern erbt (zu finden mit `/data get entity`) |
| `breeding.baby` | die Jungen kommen als Jungtiere ihrer Mod zur Welt (Standard true) |
| `terrarium` | statt eines Aquariums: `habitat` (`dry`, `paludarium`, `any`, `wet`), `air`, `humidity`, `land`, `water`, `uv`, `basking` wie bei den [Terrarientieren](../terrarium.md); dazu die Schlüssel unten |
| `terrarium.length` | seine Länge in Metern: ein Großtier nach der [Größenregel](../big_tanks.md) (12 / 6 / 3 Blöcke pro Meter, 100 Blöcke pro Kubikmeter); ohne: seine Hitbox entscheidet |
| `terrarium.volume` | was die Größenregel misst: `land` (das Land mit der Luft darüber), `water` (den Wasserteil) oder `all` (das ganze Becken); Standard nach dem Habitat (`dry` das Land, `wet` das Wasser, sonst alles) |
| `terrarium.movement` | wie es sich im Becken bewegt: `walk` (Standard), `slither`, `hop`, `climb`, `swim`, `fly` (durch die Luft, ruht an Land) oder `own` für die KI seiner Mod; oder ein Objekt mit `style` und `walk`, `swim` (Blöcke pro Tick), `bottom`, `max_step`, `water_time`, `bask_time`, `rest_time`, `height` |
| `terrarium.prey` | es frisst die kleinen Fische und Garnelen seines Beckens bis zu dieser Länge (Blöcke; ab 0,5 auch Bettas und Krallenfrösche) |
| `terrarium.box` | die Terrarienbox fängt es (Standard true; false: großer Kescher und Transportwanne) |
| `terrarium.min_blocks` | das kleinste Becken eines kleinen Tiers (Blöcke Wasser und Land; kleiner stresst es) |
| `breeding.nest`, `nest_temperature` | Eier in ein Nest auf dem Land, dessen Oberseite zu diesem Block-Tag gehört (`#bettacraft:turtle_nest_ground`: Sand und Erde); sie schlüpfen nach `days` bei dieser Nesttemperatur (°C; wärmer schneller, bei 18 °C und darunter warten sie, über 36 °C sterben sie) |
| `suppress_own_breeding` | `false` lässt die eigene Zucht des Tiers im Aquarium zusätzlich laufen |

Tiere mit eigener Pflege in Bettacraft (unsere Tiere, Axolotl, Delfin, Meeresschildkröte und die Mobs im Glas) können kein
Profil bekommen. Das Serverlog nennt die geladenen Profile und jeden Fehler in einer Datei.

## Terrarium-Profile

Ein Landtier mit Terrarium-Profil (Beispiel: die Landschildkröte):

```json
{
  "entity": "naturalist:tortoise",
  "care": {"hunger_days": 1.5},
  "food": ["#bettacraft:turtle_greens"],
  "breeding": {"type": "eggs", "nest": "#bettacraft:turtle_nest_ground", "nest_temperature": 30, "days": 4, "young": [2, 4], "inherit": ["Variant"]},
  "terrarium": {"habitat": "dry", "air": [22, 35], "humidity": [30, 70], "uv": 4, "basking": true,
                "length": 1.0, "volume": "land", "movement": {"style": "walk", "walk": 0.012}}
}
```

Die Serveroption `terrarium.boxAllow` nennt Landtiere ohne Profil, die die Terrarienbox trotzdem fängt (nur kleine; sie
leben ohne unsere Pflege in Terrarien und Paludarien).

## Profile in einer Welt prüfen

* **Umbenannte Entity:** Ist die Mod eines Profils installiert, die Entity-ID aber nicht registriert (eine neuere Version der
  Mod hat sie umbenannt), warnt das Server-Log mit der Profil-Datei, der fehlenden ID und den ähnlichen IDs dieser Mod,
  damit sich die Datei korrigieren lässt. Ein Profil für eine Mod, die nicht installiert ist, bleibt still.
* **`/bettacraft foreign list [Radius]`** (Operatoren): jedes Tier aus Vanilla oder einer anderen Mod im angeschauten Becken,
  sonst in allen Becken im Radius (Standard 32 Blöcke), mit seiner Entity-ID und der Profil-Datei, die es abbildet, oder
  „kein Profil“. Am Ende die Entity-Typen anderer Mods, die ohne Profil in diesen Becken leben: die, für die sich ein Profil
  lohnen würde.
